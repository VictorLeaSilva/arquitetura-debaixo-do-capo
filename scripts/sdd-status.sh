#!/usr/bin/env bash
# Estado de cada tarefa do backlog SDD, calculado de forma determinística (sem leitura pelo agente).
#   sdd-status.sh            (rode da raiz do repositório)
# Saída: tabela TSV "ID  estado  checklist  revisão  título", depois Q abertas e próximas elegíveis.
# Estados: pronta | pronta-com-pendencia | em-andamento | em-revisao | revisao-fail | sem-revisao
#          | elegivel | bloqueada | pendente | cortada
# Dependência satisfeita: pronta, pronta-com-pendencia, sem-revisao ou cortada.
set -u
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "rode dentro do repositório" >&2; exit 2; }
# shellcheck source=lib/sdd-common.sh
. "$AQUI/lib/sdd-common.sh"
sdd_carregar_conf "$RAIZ"
cd "$RAIZ" || exit 2
P="$SDD_PRINCIPAL"; S="docs/specs"
[ -f "$S/06-tasks.md" ] || { echo "não encontrado: $S/06-tasks.md" >&2; exit 2; }

# Q abertas: célula de decisão vazia ou começando por "Proposta".
QABERTAS="$( [ -f "$S/open-questions.md" ] && awk -F'|' '/^\| *Q-[0-9]{3} *\|/ {
  q = $2; d = $6; gsub(/^[ \t]+|[ \t]+$/, "", q); gsub(/^[ \t*]+|[ \t]+$/, "", d)
  if (d == "" || d ~ /^[Pp]roposta/) print q "\t" $3 }' "$S/open-questions.md")"

BRANCHES="$(git branch -a --format='%(refname:short)' | sed 's|^origin/||' | sort -u)"
MERGEADAS="$(git branch -a --merged "$P" --format='%(refname:short)' | sed 's|^origin/||' | sort -u)"

declare -A ESTADO DEPS TITULO
ORDEM=()
while IFS=$'\t' read -r id titulo deps; do
  ORDEM+=("$id"); TITULO[$id]="$titulo"; DEPS[$id]="$deps"
done < <(awk -F'|' '/^\| *T-[0-9]{3} *\|/ { id = $2; gsub(/[ \t*]/, "", id); t = $3; gsub(/^[ \t]+|[ \t]+$/, "", t)
          d = $4; s = ""; while (match(d, /T-[0-9]{3}/)) { s = s " " substr(d, RSTART, RLENGTH); d = substr(d, RSTART + RLENGTH) }
          print id "\t" t "\t" s }' "$S/06-tasks.md")

checklist() { # feitas/total na seção "Checklist de pronto", lida da branch principal
  git show "$P:$S/tasks/$1.md" 2>/dev/null | awk '/^## Checklist de pronto/ { d = 1; next } d && /^## / { exit }
    d { f += gsub(/- \[[xX]\]/, ""); a += gsub(/- \[ \]/, "") } END { printf "%d/%d", f, f + a }'
}
# Veredito vigente = o da última rodada (rodadas novas vão no fim do relatório).
veredito() {
  git show "$P:$S/reviews/$1.md" 2>/dev/null | tr -d '\r' | sed 's/^\xEF\xBB\xBF//' \
    | grep -oE '^\**(Veredito|Status)\**:[ *]*(PASS_WITH_NOTES|PASS|FAIL)' \
    | tail -1 | grep -oE 'PASS_WITH_NOTES|PASS|FAIL'
}

for id in "${ORDEM[@]}"; do
  if printf '%s' "${TITULO[$id]}" | grep -qiE 'cortad'; then ESTADO[$id]=cortada; continue; fi
  v="$(veredito "$id")"; c="$(checklist "$id")"
  if [ -n "$v" ] && [ "$v" != FAIL ]; then
    if [ -n "$c" ] && [ "${c%/*}" = "${c#*/}" ] && [ "${c#*/}" != 0 ]; then ESTADO[$id]=pronta; else ESTADO[$id]=pronta-com-pendencia; fi
  elif printf '%s\n' "$BRANCHES" | grep -qE "^task/$id(-|$)" && ! printf '%s\n' "$MERGEADAS" | grep -qE "^task/$id(-|$)"; then
    ref="$(git for-each-ref --format='%(refname:short)' "refs/heads/task/$id*" "refs/remotes/*/task/$id*" | head -1)"
    if git show "$ref:$S/reviews/$id.verify.log" 2>/dev/null | grep -q 'VERDE'; then ESTADO[$id]=em-revisao; else ESTADO[$id]=em-andamento; fi
  elif [ "$v" = FAIL ]; then
    ESTADO[$id]=revisao-fail          # última rodada reprovou: corrigir ou registrar a aprovação humana
  elif git cat-file -e "$P:$S/reviews/$id.evidence.md" 2>/dev/null; then
    ESTADO[$id]=sem-revisao           # entrou no principal com evidência e sem relatório do revisor
  else
    ESTADO[$id]=pendente
  fi
done

ELEGIVEIS=()
for id in "${ORDEM[@]}"; do
  [ "${ESTADO[$id]}" = pendente ] || continue
  ok=1
  for d in ${DEPS[$id]}; do case "${ESTADO[$d]:-pendente}" in pronta|pronta-com-pendencia|sem-revisao|cortada) ;; *) ok=0 ;; esac; done
  if printf '%s\n' "$QABERTAS" | cut -f2 | grep -qE "\b$id\b"; then ok=0; ESTADO[$id]=bloqueada; fi
  [ "$ok" -eq 1 ] && ESTADO[$id]=elegivel && ELEGIVEIS+=("$id")
done

printf 'ID\testado\tchecklist\trevisao\ttitulo\n'
for id in "${ORDEM[@]}"; do
  printf '%s\t%s\t%s\t%s\t%s\n' "$id" "${ESTADO[$id]}" "$(checklist "$id")" "$(veredito "$id")" "${TITULO[$id]:0:70}"
done
echo
echo "resumo: $(for id in "${ORDEM[@]}"; do echo "${ESTADO[$id]}"; done | sort | uniq -c | awk '{ printf "%s=%s ", $2, $1 }')"
echo "q-abertas: $(printf '%s\n' "$QABERTAS" | cut -f1 | paste -sd' ' -)"
echo "elegiveis: ${ELEGIVEIS[*]:-nenhuma}"
for id in "${ELEGIVEIS[@]}"; do [ -f "$S/tasks/$id.md" ] || echo "  $id ainda não detalhada (sdd-planejar $id)"; done
