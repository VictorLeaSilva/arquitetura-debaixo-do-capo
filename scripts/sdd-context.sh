#!/usr/bin/env bash
# Pacote de contexto de uma tarefa SDD: só o que ela referencia, num único texto.
#   sdd-context.sh T-xxx            (rode da raiz do repositório)
# Inclui: constituição, a tarefa, os blocos de 02 dos requisitos em covers, as seções §n.n e
# decisões D-nn de 03/04 citadas em "Entradas", as Q-nnn citadas e as armadilhas do 05.
# Substitui a leitura de arquivos inteiros pelo agente (economia de contexto e mesma entrada sempre).
set -u
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "rode dentro do repositório" >&2; exit 2; }
# shellcheck source=lib/sdd-common.sh
. "$AQUI/lib/sdd-common.sh"
sdd_carregar_conf "$RAIZ"
cd "$RAIZ" || exit 2

T="${1:-}"; ARQ="docs/specs/tasks/$T.md"
[[ "$T" =~ ^T-[0-9]{3}$ ]] && [ -f "$ARQ" ] || { echo "uso: sdd-context.sh T-xxx (tarefa detalhada)" >&2; exit 2; }
S="docs/specs"

# secao <arquivo> <n.n>: do título numerado n.n até o próximo título que não seja subseção dele.
secao() {
  awk -v n="$2" '
    function num(l,  s) { s = l; sub(/^#+[ \t]+/, "", s); if (match(s, /^[0-9]+(\.[0-9]+)*/)) return substr(s, 1, RLENGTH); return "" }
    /^#+[ \t]/ {
      k = num($0)
      if (dentro && !(k != "" && (k == n || index(k, n ".") == 1))) exit
      if (k == n) dentro = 1
    }
    dentro' "$1"
}

# linha_de <arquivo> <campo>: a linha de "Entradas" que cita o arquivo.
linha_de() { awk '/^## Entradas/ { d = 1; next } d && /^## / { exit } d' "$ARQ" | grep -F -- "$1" || true; }

bloco_req() { awk -v id="$1" '$0 ~ "^### " id "([^0-9]|$)" { d = 1; print; next } d && /^#{1,3} / { exit } d' "$S/02-requirements.md"; }

echo "# Contexto da $T (gerado por sdd-context.sh; não edite)"
echo; echo "## 00-constitution.md"; cat "$S/00-constitution.md"
echo; echo "## tasks/$T.md"; cat "$ARQ"

echo; echo "## 02-requirements.md (requisitos de covers)"
fm_lista "$ARQ" covers | while read -r id; do bloco_req "$id"; echo; done

for f in 03-design-contracts 04-quality-standards; do
  [ -f "$S/$f.md" ] || continue
  l="$(linha_de "$f")"; [ -z "$l" ] && continue
  echo; echo "## $f.md (seções citadas)"
  # Sem repetição: uma seção já coberta por uma ancestral citada (§3.1 cobre §3.1.1) é pulada.
  printf '%s\n' "$l" | grep -oE '§[0-9]+(\.[0-9]+)*' | tr -d '§' | sort -u -V \
    | awk '{ for (i = 1; i <= k; i++) if (index($0, a[i] ".") == 1) next; a[++k] = $0; print }' \
    | while read -r n; do secao "$S/$f.md" "$n"; echo; done
  printf '%s\n' "$l" | grep -oE '\bD-[0-9]{2}\b' | sort -u | while read -r d; do grep -E "^\| *$d *\|" "$S/$f.md"; done
done

qs="$(grep -oE '\bQ-[0-9]{3}\b' "$ARQ" | sort -u)"
if [ -n "$qs" ] && [ -f "$S/open-questions.md" ]; then
  echo; echo "## open-questions.md (citadas)"
  for q in $qs; do grep -E "^\| *$q *\|" "$S/open-questions.md"; done
fi

if [ -f "$S/05-harness.md" ]; then
  echo; echo "## 05-harness.md (armadilhas conhecidas)"
  awk '/^## [0-9.]* *Armadilhas/ { d = 1 } d && /^## / && !/Armadilhas/ { exit } d' "$S/05-harness.md"
fi
