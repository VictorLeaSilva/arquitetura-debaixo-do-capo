#!/usr/bin/env bash
# Testes do verificador do compose.yaml (scripts/compose-lint.sh), um arquivo temporário por cenário.
#   bash scripts/test/test-compose-lint.sh
set -u
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LINT="$(cd "$AQUI/.." && pwd)/compose-lint.sh"
RAIZ="$(cd "$AQUI/../.." && pwd)"
PASSOU=0; FALHOU=0

# caso <nome> <exit esperado> <texto esperado na saída ou ""> <quantas linhas de ERRO> <conteúdo do compose>
caso() {
  local nome="$1" esperado="$2" texto="$3" erros="$4" conteudo="$5" f rc saida n
  f="$(mktemp)"; printf '%s\n' "$conteudo" > "$f"
  saida="$(bash "$LINT" "$f" 2>&1)"; rc=$?
  n="$(printf '%s\n' "$saida" | grep -c '^ERRO')"
  if [ "$rc" -eq "$esperado" ] && [ "$n" -eq "$erros" ] && { [ -z "$texto" ] || printf '%s' "$saida" | grep -qF -- "$texto"; }; then
    PASSOU=$((PASSOU + 1)); echo "ok   $nome"
  else
    FALHOU=$((FALHOU + 1)); echo "FAIL $nome (exit $rc, esperado $esperado; $n erro(s), esperado $erros)"; printf '%s\n' "$saida" | sed 's/^/     /'
  fi
  rm -f "$f"
}

# covers: RNF-009
caso "compose válido: tags fixas, digest e portas em 127.0.0.1" 0 ": ok" 0 'services:
  db:
    image: postgres:18.6-trixie
    ports:
      - "127.0.0.1:5432:5432"
  obs:
    image: grafana/otel-lgtm@sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef
    ports:
      - 127.0.0.1:3000:3000'

# covers: RNF-001
caso "imagem com a tag latest" 1 "imagem sem versão fixa" 1 'services:
  db:
    image: postgres:latest'

# covers: RNF-001
caso "imagem sem tag, inclusive com porta no endereço do registro" 1 "imagem sem versão fixa" 2 'services:
  db:
    image: postgres
  app:
    image: registry.example.com:5000/app'

# covers: RNF-009
caso "porta sem endereço e porta em 0.0.0.0" 1 "porta publicada fora de 127.0.0.1" 2 'services:
  app:
    image: app:1.0
    ports:
      - "8080:8080"
      - "0.0.0.0:9090:9090"'

# covers: RNF-009
caso "comentário no fim da linha ports: não desliga a verificação" 1 "porta publicada fora de 127.0.0.1" 1 'services:
  app:
    image: app:1.0
    ports: # web
      - "8080:8080"'

# covers: RNF-009
caso "porta na forma de fluxo (numa linha só) é recusada" 1 "use a lista em várias linhas" 1 'services:
  app:
    image: app:1.0
    ports: ["8080:8080"]'

# covers: RNF-009
caso "porta na forma longa é recusada" 1 "use a forma curta" 1 'services:
  app:
    image: app:1.0
    ports:
      - target: 8080
        published: 8080'

# covers: RNF-001
real() {
  local saida rc
  saida="$(bash "$LINT" "$RAIZ/compose.yaml" 2>&1)"; rc=$?
  if [ "$rc" -eq 0 ]; then PASSOU=$((PASSOU + 1)); echo "ok   o compose.yaml real do projeto passa"
  else FALHOU=$((FALHOU + 1)); echo "FAIL o compose.yaml real do projeto passa (exit $rc)"; printf '%s\n' "$saida" | sed 's/^/     /'; fi
}
real

echo "---"; echo "$PASSOU passaram, $FALHOU falharam"
[ "$FALHOU" -eq 0 ]
