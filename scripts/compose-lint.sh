#!/usr/bin/env bash
# Verificador do compose.yaml (T-004, AC-RNF-001.2 e AC-RNF-009.1).
#   bash scripts/compose-lint.sh [arquivo]      (padrão: compose.yaml na raiz do repositório)
# Regras:
#   1. toda imagem tem versão fixa: uma tag diferente de "latest" ou um digest (@sha256:...);
#   2. toda porta publicada está ligada a 127.0.0.1, na forma curta "127.0.0.1:<host>:<container>".
#      A forma longa (- target: ...) e a de fluxo (ports: [...]) são recusadas.
# Saída: as linhas de ERRO e exit 1; ou "<arquivo>: ok" e exit 0; arquivo inexistente, exit 2.
set -u

ARQ="${1:-}"
if [ -z "$ARQ" ]; then
  RAIZ="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
  ARQ="$RAIZ/compose.yaml"
fi
[ -f "$ARQ" ] || { echo "arquivo não encontrado: $ARQ" >&2; exit 2; }

tr -d '\r' < "$ARQ" | ARQ_NOME="$ARQ" awk '
  # Indentação de uma linha (número de espaços no começo).
  function indent(s) { match(s, /^ */); return RLENGTH }
  # Tira comentário do fim, espaços e aspas (simples ou duplas). O apóstrofo vem de sprintf, que é POSIX.
  function limpa(s) { sub(/[ \t]+#.*$/, "", s); gsub(/["]/, "", s); gsub(sprintf("%c", 39), "", s); gsub(/^[ \t]+|[ \t]+$/, "", s); return s }

  { linha = $0 }
  # Comentários e linhas vazias não contam.
  linha ~ /^[ \t]*(#|$)/ { next }

  # Saiu do bloco "ports:" quando a indentação volta ao nível dele ou menos (e a linha não é item da lista).
  emports && indent(linha) <= nivel_ports && linha !~ /^[ \t]*-/ { emports = 0 }

  # Regra 1: imagem com versão fixa.
  linha ~ /^[ \t]*image:/ {
    img = linha; sub(/^[ \t]*image:[ \t]*/, "", img); img = limpa(img)
    ultimo = img; sub(/^.*\//, "", ultimo)          # o trecho depois do último "/" (o ":" do registro não conta)
    # Um digest (@sha256:...) sempre traz ":" e passa pela mesma regra da tag, sem caso especial.
    if (index(ultimo, ":") == 0) { print "ERRO linha " NR ": imagem sem versão fixa: " img; erros++; next }
    tag = ultimo; sub(/^[^:]*:/, "", tag)
    if (tag == "" || tag == "latest") { print "ERRO linha " NR ": imagem sem versão fixa: " img; erros++ }
    next
  }

  # Início do bloco de portas.
  # Forma de fluxo numa linha só (ports: ["8080:8080"]): recusada, para nenhuma forma de escrever porta escapar (Q-020).
  linha ~ /^[ \t]*ports:[ \t]*\[/ { print "ERRO linha " NR ": use a lista em várias linhas, um item \"127.0.0.1:<host>:<container>\" por linha"; erros++; next }

  # Aceita comentário no fim ("ports: # web"): sem isso, um comentário desligaria a verificação do bloco.
  linha ~ /^[ \t]*ports:[ \t]*(#.*)?$/ { emports = 1; nivel_ports = indent(linha); next }

  # Regra 2: cada item da lista de portas.
  emports && linha ~ /^[ \t]*-/ {
    item = linha; sub(/^[ \t]*-[ \t]*/, "", item); item = limpa(item)
    if (item ~ /^[a-z_]+:[ \t]/ || item ~ /^[a-z_]+:$/) {
      print "ERRO linha " NR ": use a forma curta \"127.0.0.1:<host>:<container>\""; erros++; next
    }
    if (item !~ /^127\.0\.0\.1:/) { print "ERRO linha " NR ": porta publicada fora de 127.0.0.1: " item; erros++ }
    next
  }

  END {
    if (erros > 0) exit 1
    print ENVIRON["ARQ_NOME"] ": ok"   # por ENVIRON, e não por -v: o -v trataria as barras de um caminho do Windows como escapes
  }'
