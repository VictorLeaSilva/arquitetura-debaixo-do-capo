#!/usr/bin/env bash
# Verificador dos ADRs (T-005, RNF-014).
#   bash scripts/adr-lint.sh [raiz]      (padrão: a raiz do repositório)
# Confere:
#   1. cada decisão D-nn da tabela de 03 §1 aparece na "Origem" de exatamente um ADR;
#   2. cada ADR tem Status (proposto | aceito | substituído por ADR-nnn), Data, Origem,
#      as seções Contexto, Decisão, Alternativas rejeitadas (com ao menos uma linha) e Consequências;
#   3. nenhum número de ADR se repete;
#   4. o índice (decisions/README.md) cita cada ADR e nenhum arquivo inexistente;
#   5. cada RNF de 02 tem uma linha na tabela docs/architecture/rnf-mecanismos.md.
# Saída: as linhas de ERRO e exit 1; ou "adr-lint: ok (...)" e exit 0. Só usa grep, awk e sed (POSIX).
set -u

RAIZ="${1:-$(git rev-parse --show-toplevel 2> /dev/null || pwd)}"
DEC="$RAIZ/docs/architecture/decisions"
D03="$RAIZ/docs/specs/03-design-contracts.md"
R02="$RAIZ/docs/specs/02-requirements.md"
TAB="$RAIZ/docs/architecture/rnf-mecanismos.md"
IDX="$DEC/README.md"
for f in "$D03" "$R02" "$TAB" "$IDX"; do [ -f "$f" ] || { echo "ERRO arquivo não encontrado: $f"; exit 1; }; done

ERROS=0
erro() { echo "ERRO $*"; ERROS=$((ERROS + 1)); }

# Os ADRs (o modelo ADR-000 não conta).
ADRS=()
for f in "$DEC"/ADR-[0-9][0-9][0-9]-*.md; do
  [ -f "$f" ] || continue
  case "$(basename "$f")" in ADR-000-*) continue ;; esac
  ADRS+=("$f")
done

# 2. Estrutura de cada ADR.
for f in "${ADRS[@]}"; do
  n="$(basename "$f")"
  tr -d '\r' < "$f" | grep -qE '^- \*\*Status:\*\* (proposto|aceito|substituído por ADR-[0-9]{3})[[:space:]]*$' \
    || erro "$n: status inválido ou ausente (use proposto, aceito ou substituído por ADR-nnn)"
  tr -d '\r' < "$f" | grep -qE '^- \*\*Data:\*\* [0-9]{4}-[0-9]{2}-[0-9]{2}' || erro "$n: sem a linha Data (AAAA-MM-DD)"
  tr -d '\r' < "$f" | grep -qE '^- \*\*Origem:\*\* ' || erro "$n: sem a linha Origem"
  for s in "Contexto" "Decisão" "Alternativas rejeitadas" "Consequências"; do
    tr -d '\r' < "$f" | grep -qE "^## $s[[:space:]]*\$" || erro "$n: sem a seção $s"
  done
  # Linhas de conteúdo da tabela de alternativas: as linhas "|" da seção, menos o cabeçalho e o separador.
  linhas="$(tr -d '\r' < "$f" | awk '
    /^## / { dentro = ($0 ~ /^## Alternativas rejeitadas[[:space:]]*$/); next }
    dentro && /^\|/ { n++; if (n > 2) c++ }
    END { print c + 0 }')"
  [ "$linhas" -ge 1 ] || erro "$n: sem alternativa rejeitada na tabela"
done

# 3. Números repetidos.
for num in $(for f in "${ADRS[@]}"; do basename "$f" | cut -c1-7; done | sort | uniq -d); do
  erro "número repetido: $num"
done

# 1. Correspondência decisão ↔ ADR.
DECISOES="$(tr -d '\r' < "$D03" | grep -oE '^\| D-[0-9]+ \|' | grep -oE 'D-[0-9]+' | sort -u)"
for d in $DECISOES; do
  cita=()
  for f in "${ADRS[@]}"; do
    tr -d '\r' < "$f" | grep -E '^- \*\*Origem:\*\* ' | grep -qE "(^|[^0-9])$d([^0-9]|\$)" && cita+=("$(basename "$f")")
  done
  if [ "${#cita[@]}" -eq 0 ]; then erro "$d: sem ADR"
  elif [ "${#cita[@]}" -gt 1 ]; then erro "$d: em mais de um ADR (${cita[*]})"; fi
done

# 4. Índice.
for f in "${ADRS[@]}"; do
  n="$(basename "$f")"
  grep -qF -- "$n" "$IDX" || erro "fora do índice: $n"
done
for n in $(tr -d '\r' < "$IDX" | grep -oE 'ADR-[0-9]{3}-[A-Za-z0-9-]+\.md' | sort -u); do
  [ -f "$DEC/$n" ] || erro "índice cita arquivo inexistente: $n"
done

# 5. RNF na tabela de mecanismos.
RNFS="$(tr -d '\r' < "$R02" | grep -oE '^### RNF-[0-9]+' | grep -oE 'RNF-[0-9]+')"
for r in $RNFS; do
  tr -d '\r' < "$TAB" | grep -qE "^\| *$r *\|" || erro "$r: sem linha em docs/architecture/rnf-mecanismos.md"
done

if [ "$ERROS" -gt 0 ]; then exit 1; fi
echo "adr-lint: ok (${#ADRS[@]} ADRs, $(printf '%s\n' $DECISOES | grep -c .) decisões, $(printf '%s\n' $RNFS | grep -c .) RNF)"
