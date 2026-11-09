#!/usr/bin/env bash
# Matriz de rastreabilidade SDD: requisito -> testes (marcação) -> tarefas (covers).
#   scripts/check-traceability.sh              lista obrigatórios sem teste (exit 1 se houver)
#   scripts/check-traceability.sh --write      regenera docs/specs/traceability.md
#   scripts/check-traceability.sh --check-file exit 1 se o arquivo versionado difere do gerado
set -u

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "rode dentro do repositório" >&2; exit 2; }
# shellcheck source=lib/sdd-common.sh
. "$AQUI/lib/sdd-common.sh"
sdd_carregar_conf "$RAIZ"
cd "$RAIZ" || exit 2

REQS="docs/specs/02-requirements.md"
MATRIZ="docs/specs/traceability.md"
[ -f "$REQS" ] || { echo "não encontrado: $REQS" >&2; exit 2; }

# Linhas "ID<TAB>arquivo" para cada marcação de requisito em arquivo de teste.
marcacoes() {
  local f
  git -c core.quotepath=false ls-files --cached --others --exclude-standard \
    | LC_ALL=C grep -E "$SDD_TEST_FILES" | LC_ALL=C sort -u | while read -r f; do
      [ -f "$f" ] || continue
      grep -E "$SDD_TAG_LINE" "$f" | grep -oE "$SDD_REQ_ID" | LC_ALL=C sort -u | sed "s|\$|	$f|"
    done
}

# Linhas "ID<TAB>T-xxx" a partir do covers de cada tarefa.
coberturas() {
  local t id
  for t in docs/specs/tasks/T-*.md; do
    [ -f "$t" ] || continue
    fm_lista "$t" covers | while read -r id; do printf '%s\t%s\n' "$id" "$(basename "$t" .md)"; done
  done
}

obrigatorio() {
  printf '%s\n' "$1" | grep -qxE "$SDD_MANDATORY" || return 1
  printf '%s\n' $SDD_EVIDENCE_IDS | grep -qxF -- "$1" && return 1
  return 0
}

gerar() {
  local m c id nome tst trf obr
  m="$(marcacoes)"; c="$(coberturas)"
  echo "# Matriz de rastreabilidade"
  echo
  echo "Gerado por \`scripts/check-traceability.sh --write\`. Não edite à mão."
  echo
  echo "| Requisito | Obrigatório com teste | Testes | Tarefas |"
  echo "|---|---|---|---|"
  sdd_requisitos "$REQS" | while IFS=$'\t' read -r id nome; do
    tst="$(printf '%s\n' "$m" | awk -F'\t' -v i="$id" '$1 == i { print "`" $2 "`" }' | paste -sd ' ' -)"
    trf="$(printf '%s\n' "$c" | awk -F'\t' -v i="$id" '$1 == i { print $2 }' | LC_ALL=C sort -u | paste -sd ' ' -)"
    if obrigatorio "$id"; then obr="sim"; else obr="evidência"; fi
    echo "| $id $nome | $obr | ${tst:-—} | ${trf:-—} |"
  done
}

case "${1:-}" in
  "")
    m="$(marcacoes)"; faltam=0
    while read -r id; do
      obrigatorio "$id" || continue
      printf '%s\n' "$m" | cut -f1 | grep -qxF -- "$id" || { echo "sem teste: $id"; faltam=1; }
    done < <(sdd_requisitos "$REQS" | cut -f1)
    [ "$faltam" -eq 0 ] && echo "todos os requisitos obrigatórios têm teste"
    exit "$faltam" ;;
  --write)
    gerar > "$MATRIZ.tmp" && mv "$MATRIZ.tmp" "$MATRIZ" && echo "gerado: $MATRIZ" ;;
  --check-file)
    if [ -f "$MATRIZ" ] && cmp -s <(gerar) "$MATRIZ"; then
      echo "$MATRIZ está em dia"
    else
      echo "$MATRIZ desatualizado: rode scripts/check-traceability.sh --write e commite" >&2
      exit 1
    fi ;;
  *) echo "uso: scripts/check-traceability.sh [--write | --check-file]" >&2; exit 2 ;;
esac
