#!/usr/bin/env bash
# Testes do build do site (AC-010.1): o build passa, e um link interno quebrado o derruba.
#   cd site && npm ci && cd .. && bash scripts/test/test-site-build.sh
# Cada página de teste é criada em site/docs/ com o prefixo zz-teste- e apagada no fim, mesmo se o teste falhar.
set -u
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SITE="$(cd "$AQUI/../.." && pwd)/site"
PASSOU=0; FALHOU=0

[ -d "$SITE/node_modules" ] || { echo "rode antes: cd site && npm ci"; exit 2; }

limpar() { rm -f "$SITE"/docs/zz-teste-*.md; }
trap limpar EXIT

# build <nome> <espera sucesso: sim|não> <texto esperado na saída, ou vazio>
build() {
  local nome="$1" sucesso="$2" texto="$3" saida rc
  saida="$(cd "$SITE" && npm run build 2>&1)"; rc=$?
  limpar
  if { [ "$sucesso" = sim ] && [ "$rc" -eq 0 ]; } \
     || { [ "$sucesso" = não ] && [ "$rc" -ne 0 ] && printf '%s' "$saida" | grep -qF -- "$texto"; }; then
    PASSOU=$((PASSOU + 1)); echo "ok   $nome"
  else
    FALHOU=$((FALHOU + 1)); echo "FAIL $nome (exit $rc)"; printf '%s\n' "$saida" | tail -20 | sed 's/^/     /'
  fi
}

# covers: RF-010
build "1. o site compila" sim ""

# covers: RF-010
printf '# Teste\n\n[quebrado](./nao-existe.md)\n' > "$SITE/docs/zz-teste-markdown.md"
build "2. link Markdown para arquivo inexistente derruba o build" não "nao-existe"

# covers: RF-010
printf '# Teste\n\n[quebrado](/pagina-que-nao-existe)\n' > "$SITE/docs/zz-teste-caminho.md"
build "3. link para caminho inexistente derruba o build" não "pagina-que-nao-existe"

echo "---"; echo "$PASSOU passaram, $FALHOU falharam"
[ "$FALHOU" -eq 0 ]
