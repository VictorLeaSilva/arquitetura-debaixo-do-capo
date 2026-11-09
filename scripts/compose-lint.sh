#!/usr/bin/env bash
# Verificador do compose.yaml (T-004, AC-RNF-001.2 e AC-RNF-009.1).
#   bash scripts/compose-lint.sh [arquivo]      (padrão: compose.yaml na raiz do repositório)
# Regras:
#   1. toda imagem tem versão fixa: uma tag diferente de "latest" ou um digest (@sha256:...);
#   2. toda porta publicada está ligada a 127.0.0.1, na forma curta "127.0.0.1:<host>:<container>".
# Saída: as linhas de ERRO e exit 1; ou "<arquivo>: ok" e exit 0; arquivo inexistente, exit 2.
set -u

ARQ="${1:-}"
if [ -z "$ARQ" ]; then
  RAIZ="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
  ARQ="$RAIZ/compose.yaml"
fi
[ -f "$ARQ" ] || { echo "arquivo não encontrado: $ARQ" >&2; exit 2; }

tr -d '\r' < "$ARQ" | awk -v arq="$ARQ" '
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
    if (img ~ /@sha256:/) next
    if (index(ultimo, ":") == 0) { print "ERRO linha " NR ": imagem sem versão fixa: " img; erros++; next }
    tag = ultimo; sub(/^[^:]*:/, "", tag)
    if (tag == "" || tag == "latest") { print "ERRO linha " NR ": imagem sem versão fixa: " img; erros++ }
    next
  }

  # Início do bloco de portas.
  linha ~ /^[ \t]*ports:[ \t]*$/ { emports = 1; nivel_ports = indent(linha); next }

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
    print arq ": ok"
  }'
