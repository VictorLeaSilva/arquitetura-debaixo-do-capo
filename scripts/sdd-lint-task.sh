#!/usr/bin/env bash
# Definição de Pronto-para-executar (DoR) de uma tarefa SDD. Determinístico; exit 1 se houver ERRO.
#   sdd-lint-task.sh T-xxx            (rode da raiz do repositório)
# Confere: frontmatter, covers existentes em 02, seções obrigatórias, placeholders <...>,
# todo AC com linha em "Testes exigidos", todo covers com teste ou evidence_only,
# Q-nnn citadas decididas, depends_on concluídas e latitude do implementador declarada.
set -u
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ="$(git rev-parse --show-toplevel 2>/dev/null)" || { echo "rode dentro do repositório" >&2; exit 2; }
# shellcheck source=lib/sdd-common.sh
. "$AQUI/lib/sdd-common.sh"
sdd_carregar_conf "$RAIZ"
cd "$RAIZ" || exit 2

T="${1:-}"; ARQ="docs/specs/tasks/$T.md"
[[ "$T" =~ ^T-[0-9]{3}$ ]] && [ -f "$ARQ" ] || { echo "uso: sdd-lint-task.sh T-xxx" >&2; exit 2; }
ERROS=0; AVISOS=0
erro() { echo "ERRO  $*"; ERROS=$((ERROS + 1)); }
aviso() { echo "AVISO $*"; AVISOS=$((AVISOS + 1)); }
secao() { awk -v t="$1" '$0 ~ "^## " t { d = 1; next } d && /^## / { exit } d' "$ARQ"; }

# 1. frontmatter
[ "$(fm_lista "$ARQ" id)" = "$T" ] || erro "frontmatter: id diferente de $T"
mapfile -t COVERS < <(fm_lista "$ARQ" covers)
mapfile -t EVID < <(fm_lista "$ARQ" evidence_only)
[ "${#COVERS[@]}" -gt 0 ] || erro "frontmatter: covers vazio"
[ -n "$(fm_lista "$ARQ" allowed_paths)" ] || erro "frontmatter: allowed_paths vazio"
grep -q '^allowed_new_dependencies:' "$ARQ" || erro "frontmatter: falta allowed_new_dependencies (use [] se nenhuma)"

# 2. covers existem em 02; evidence_only ⊆ covers
REQS="$(sdd_requisitos | cut -f1)"
for id in "${COVERS[@]}"; do
  printf '%s\n' "$REQS" | grep -qxF -- "$id" || erro "covers: $id não existe em 02-requirements.md"
done
for id in "${EVID[@]}"; do
  printf '%s\n' "${COVERS[@]}" | grep -qxF -- "$id" || erro "evidence_only: $id não está em covers"
done

# 3. seções obrigatórias
for s in Objetivo Entradas Saídas "Critérios de aceite" "Testes exigidos" "Não-objetivos" "Checklist de pronto"; do
  grep -q "^## $s" "$ARQ" || erro "seção ausente: ## $s"
done
grep -q '^## Decisões da tarefa' "$ARQ" || aviso "sem '## Decisões da tarefa': o implementador terá de decidir o que a spec não detalha"
grep -q '^## Latitude do implementador' "$ARQ" || aviso "sem '## Latitude do implementador': o que ele pode escolher sozinho fica implícito"

# 4. placeholders do modelo (fora de trechos entre crases)
PH="$(sed -e 's/`[^`]*`//g' "$ARQ" | grep -nE '<[a-zà-ú][^<>]{2,}>')"
[ -n "$PH" ] && while read -r l; do erro "placeholder não preenchido: linha $l"; done <<< "$PH"

# 5. AC → teste
AC_CRIT="$(secao 'Critérios de aceite' | grep -oE 'AC-(T[0-9]{3}|[0-9]{3})\.[0-9]+' | sort -u)"
TESTES="$(secao 'Testes exigidos')"
[ -n "$AC_CRIT" ] || erro "nenhum AC com ID (AC-nnn.n ou AC-Txxx.n) em Critérios de aceite"
for ac in $AC_CRIT; do
  printf '%s\n' "$TESTES" | grep -qF -- "$ac" || erro "AC sem linha em Testes exigidos: $ac"
done
for ac in $(printf '%s\n' "$TESTES" | grep -oE 'AC-(T[0-9]{3}|[0-9]{3})\.[0-9]+' | sort -u); do
  printf '%s\n' "$AC_CRIT" | grep -qxF -- "$ac" || aviso "Testes exigidos cita $ac, ausente de Critérios de aceite"
done

# 6. covers (fora de evidence_only) com tag prevista
for id in "${COVERS[@]}"; do
  printf '%s\n' "${EVID[@]}" | grep -qxF -- "$id" && continue
  printf '%s\n' "$TESTES" | grep -qE -- "$id([^0-9]|$)" || erro "covers $id sem tag prevista em Testes exigidos (ou mova para evidence_only)"
done

# 7. Q citadas decididas (aberta = célula de decisão vazia ou começando por "Proposta")
if [ -f docs/specs/open-questions.md ]; then
  for q in $(grep -oE '\bQ-[0-9]{3}\b' "$ARQ" | sort -u); do
    linha="$(grep -E "^\| *$q *\|" docs/specs/open-questions.md | head -1)"
    if [ -z "$linha" ]; then erro "$q citada e inexistente em open-questions.md"; continue; fi
    dec="$(printf '%s' "$linha" | awk -F'|' '{ gsub(/^[ \t*]+|[ \t]+$/, "", $6); print $6 }')"
    case "$dec" in ""|Proposta*|proposta*) erro "$q ainda sem decisão";; esac
  done
fi

# 8. dependências concluídas = relatório de revisão presente na branch principal
for d in $(fm_lista "$ARQ" depends_on | grep -oE 'T-[0-9]{3}'); do
  git cat-file -e "$SDD_PRINCIPAL:docs/specs/reviews/$d.md" 2>/dev/null \
    || erro "depends_on $d sem revisão em $SDD_PRINCIPAL (não concluída)"
done

echo "---"; echo "$T: $ERROS erro(s), $AVISOS aviso(s)"
[ "$ERROS" -eq 0 ]
