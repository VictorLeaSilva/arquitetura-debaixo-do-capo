#!/usr/bin/env bash
# Gate automático da tarefa SDD (05-harness §4).
#   BASE=<rev> scripts/verify-task.sh T-xxx      (BASE padrão: SDD_PRINCIPAL do sdd.conf)
# Roda todos os itens, imprime o resultado de cada um e grava docs/specs/reviews/T-xxx.verify.log.
# Exit 0 só se todos passarem. Configuração em scripts/sdd.conf.
set -u

if [ "${BASH_VERSINFO[0]}" -lt 4 ]; then
  echo "bash 4 ou superior é necessário; no macOS: brew install bash" >&2
  exit 2
fi

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# A raiz é a do repositório em que o comando roda (os scripts podem ficar fora dele, na skill).
RAIZ="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "rode dentro do repositório" >&2; exit 2; }
# shellcheck source=lib/sdd-common.sh
. "$AQUI/lib/sdd-common.sh"
sdd_carregar_conf "$RAIZ"

# --rapido: pula o build (item 1) para iterar barato; o gate final sempre roda sem a opção.
RAPIDO=0
[ "${1:-}" = "--rapido" ] && { RAPIDO=1; shift; }
TAREFA="${1:-}"
if ! [[ "$TAREFA" =~ ^T-[0-9]{3}$ ]]; then
  echo "uso: BASE=<rev> scripts/verify-task.sh [--rapido] T-xxx" >&2
  exit 2
fi
cd "$RAIZ" || exit 2

ARQ_TAREFA="docs/specs/tasks/$TAREFA.md"
[ -f "$ARQ_TAREFA" ] || { echo "tarefa não encontrada: $ARQ_TAREFA" >&2; exit 2; }
BASE="${BASE:-$SDD_PRINCIPAL}"
git rev-parse --verify -q "$BASE^{commit}" > /dev/null \
  || { echo "BASE inválida: $BASE (use BASE=<branch base da tarefa>)" >&2; exit 2; }

LOG="docs/specs/reviews/$TAREFA.verify.log"
RESULTADOS=()
FALHOU=0
ok() { RESULTADOS+=("OK item $1 ($2): $3"); }
falha() {
  local n="$1" nome="$2"; shift 2
  FALHOU=1
  RESULTADOS+=("FALHA item $n ($nome): $1"); shift
  local d; for d in "$@"; do RESULTADOS+=("    $d"); done
}

NOMES="$(git -c core.quotepath=false diff --no-renames --name-only "$BASE"...HEAD)" \
  || { echo "sem base de merge em comum com $BASE" >&2; exit 2; }
ALTERADOS=()
[ -n "$NOMES" ] && mapfile -t ALTERADOS <<< "$NOMES"
mapfile -t COVERS < <(fm_lista "$ARQ_TAREFA" covers)
mapfile -t EVID < <(fm_lista "$ARQ_TAREFA" evidence_only)
mapfile -t PERMITIDOS < <(fm_lista "$ARQ_TAREFA" allowed_paths)
mapfile -t DEPS_OK < <(fm_lista "$ARQ_TAREFA" allowed_new_dependencies)

item1() {
  local tmp; tmp="$(mktemp)"
  if bash -c "$SDD_BUILD_CMD" > "$tmp" 2>&1; then
    ok 1 build "$SDD_BUILD_CMD verde"
  else
    local fim; mapfile -t fim < <(tail -n 20 "$tmp")
    falha 1 build "$SDD_BUILD_CMD falhou (últimas linhas abaixo)" "${fim[@]}"
  fi
  rm -f "$tmp"
}

glob_para_regex() { printf '%s' "$1" | sed -e 's/[][$^+?(){}|.]/\\&/g' -e 's/\*\+/.*/g'; }

item2() {
  local regex="docs/specs/reviews/.*|docs/specs/traceability\.md" p fora=()
  for p in "${PERMITIDOS[@]}"; do [ -n "$p" ] && regex="$regex|$(glob_para_regex "$p")"; done
  [ "${#ALTERADOS[@]}" -gt 0 ] \
    && mapfile -t fora < <(printf '%s\n' "${ALTERADOS[@]}" | LC_ALL=C grep -Ev "^($regex)\$")
  if [ "${#fora[@]}" -eq 0 ]; then
    ok 2 escopo "${#ALTERADOS[@]} arquivo(s) do diff dentro de allowed_paths"
  else
    falha 2 escopo "arquivo(s) fora de allowed_paths:" "${fora[@]}"
  fi
}

item3() {
  local novas=() c ruins=()
  mapfile -t novas < <(LC_ALL=C comm -13 <(deps_em "$BASE") <(deps_em HEAD))
  for c in "${novas[@]}"; do
    [ -z "$c" ] && continue
    printf '%s\n' "${DEPS_OK[@]}" | grep -qxF -- "$c" || ruins+=("$c")
  done
  if [ "${#ruins[@]}" -eq 0 ]; then
    ok 3 dependencias "nenhuma dependência nova fora de allowed_new_dependencies (SDD_DEPS=$SDD_DEPS)"
  else
    falha 3 dependencias "dependência(s) nova(s) fora de allowed_new_dependencies:" "${ruins[@]}"
  fi
}

item4() {
  local det=() testes=() f id precisa=() presentes=""
  for f in "${ALTERADOS[@]}"; do
    [ -f "$f" ] && printf '%s\n' "$f" | grep -qE "$SDD_TEST_FILES" && testes+=("$f")
  done
  for id in "${COVERS[@]}"; do
    [ -z "$id" ] && continue
    printf '%s\n' "${EVID[@]}" | grep -qxF -- "$id" || precisa+=("$id")
  done
  [ "${#testes[@]}" -gt 0 ] && presentes="$(grep -hE "$SDD_TAG_LINE" "${testes[@]}" | grep -oE "$SDD_REQ_ID" | sort -u)"
  for id in "${precisa[@]}"; do
    printf '%s\n' "$presentes" | grep -qxF -- "$id" || det+=("$id sem marcação em teste criado ou alterado no diff")
  done
  bash "$AQUI/check-traceability.sh" --check-file > /dev/null 2>&1 \
    || det+=("docs/specs/traceability.md desatualizado: rode scripts/check-traceability.sh --write")
  if [ "${#det[@]}" -eq 0 ]; then
    ok 4 rastreabilidade "covers marcados em teste do diff (ou evidence_only) e traceability.md em dia"
  else
    falha 4 rastreabilidade "${det[0]}" "${det[@]:1}"
  fi
}

item5() {
  local achados=()
  mapfile -t achados < <(
    # As regex vão por ENVIRON: com -v o awk reprocessaria as barras invertidas.
    git -c core.quotepath=false diff --no-renames -U0 "$BASE"...HEAD \
      | R_SRC="$SDD_SRC_FILES" R_PROIB="$SDD_FORBIDDEN" R_PROIBT="$SDD_FORBIDDEN_TEST" R_TESTES="$SDD_TEST_FILES" awk '
      BEGIN { src = ENVIRON["R_SRC"]; proib = ENVIRON["R_PROIB"]; proibt = ENVIRON["R_PROIBT"]; testes = ENVIRON["R_TESTES"] }
      /^\+\+\+ / { arq = substr($0, 7); next }
      /^@@/ { s = $3; sub(/^\+/, "", s); split(s, p, ","); linha = p[1]; next }
      /^\+/ {
        if (arq ~ src) {
          txt = substr($0, 2)
          if (txt ~ proib) print arq ":" linha ": " txt
          else if (arq ~ testes && txt ~ proibt) print arq ":" linha ": " txt
        }
        linha++
      }')
  if [ "${#achados[@]}" -eq 0 ]; then
    ok 5 padroes "nenhum padrão proibido nas linhas adicionadas"
  else
    falha 5 padroes "padrão(ões) proibido(s) nas linhas adicionadas:" "${achados[@]}"
  fi
}

item6() {
  local faltam=() c
  while read -r c; do
    [ -n "$c" ] && [ ! -e "$c" ] && faltam+=("$c")
  done < <(awk '/^## Saídas/ { d = 1; next } d && /^## / { exit } d' "$ARQ_TAREFA" \
    | grep -oE '`[^`]+`' | tr -d '`' | grep '/' | grep -E '\.[A-Za-z0-9]+$' | LC_ALL=C grep -Ev '[*?{}<> ]')
  if [ "${#faltam[@]}" -eq 0 ]; then
    ok 6 artefatos "caminhos de \"Saídas\" existem"
  else
    falha 6 artefatos "caminho(s) de \"Saídas\" ausente(s):" "${faltam[@]}"
  fi
}

item0() {
  local tmp; tmp="$(mktemp)"
  if bash "$AQUI/sdd-lint-task.sh" "$TAREFA" > "$tmp" 2>&1; then
    ok 0 contrato "tarefa pronta para executar (sdd-lint-task)"
  else
    local e; mapfile -t e < <(grep '^ERRO' "$tmp")
    falha 0 contrato "contrato da tarefa com erro(s):" "${e[@]}"
  fi
  rm -f "$tmp"
}

# Barato primeiro: as checagens estáticas falham em segundos; o build só roda por último.
item0; item2; item3; item4; item5; item6
if [ "$RAPIDO" -eq 1 ]; then
  RESULTADOS+=("PULADO item 1 (build): --rapido (não vale como gate final)")
  [ "$FALHOU" -eq 0 ] && FALHOU=3
else
  item1
fi

mkdir -p "$(dirname "$LOG")"
{
  echo "verify-task $TAREFA · base $BASE ($(git rev-parse --short "$BASE")) · head $(git rev-parse --short HEAD) · $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf '%s\n' "${RESULTADOS[@]}"
  case "$FALHOU" in
    0) echo "RESULTADO: VERDE" ;;
    3) echo "RESULTADO: VERDE PARCIAL (--rapido, exit 3; rode sem a opção antes da revisão)" ;;
    *) echo "RESULTADO: VERMELHO" ;;
  esac
} | tee "$LOG"
exit "$FALHOU"
