#!/usr/bin/env bash
# Testes do verificador dos ADRs (scripts/adr-lint.sh), um diretório temporário por cenário.
#   bash scripts/test/test-adr-lint.sh
set -u
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LINT="$(cd "$AQUI/.." && pwd)/adr-lint.sh"
RAIZ="$(cd "$AQUI/../.." && pwd)"
PASSOU=0; FALHOU=0

# Um ADR válido: adr <arquivo> <origem>
adr() {
  cat > "$1" <<EOF
# ${2} · Título de teste

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** $2 em 03

## Contexto
Contexto.

## Decisão
Decisão.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Outra | Motivo |

## Consequências
- Positivas: x.
EOF
}

# Repositório mínimo e válido: D-01 e D-02, dois ADRs, índice e tabela com RNF-001.
novo() {
  local d; d="$(mktemp -d)"
  mkdir -p "$d/docs/specs" "$d/docs/architecture/decisions"
  printf '## 1. Decisões\n| ID | Decisão |\n|---|---|\n| D-01 | Uma |\n| D-02 | Outra |\n' > "$d/docs/specs/03-design-contracts.md"
  printf '### RNF-001 · Algo\n' > "$d/docs/specs/02-requirements.md"
  adr "$d/docs/architecture/decisions/ADR-001-uma.md" D-01
  adr "$d/docs/architecture/decisions/ADR-002-outra.md" D-02
  printf '# Modelo\n' > "$d/docs/architecture/decisions/ADR-000-modelo.md"
  printf '| ADR | Título |\n|---|---|\n| [ADR-001](ADR-001-uma.md) | Uma |\n| [ADR-002](ADR-002-outra.md) | Outra |\n' > "$d/docs/architecture/decisions/README.md"
  printf '| RNF | Mecanismo |\n|---|---|\n| RNF-001 | x |\n' > "$d/docs/architecture/rnf-mecanismos.md"
  echo "$d"
}

# caso <nome> <exit esperado> <texto esperado> <preparo (roda dentro do diretório)>
caso() {
  local nome="$1" esperado="$2" texto="$3" preparo="$4" d rc saida
  d="$(novo)"
  (cd "$d" && eval "$preparo") > /dev/null 2>&1
  saida="$(bash "$LINT" "$d" 2>&1)"; rc=$?
  if [ "$rc" -eq "$esperado" ] && printf '%s' "$saida" | grep -qF -- "$texto"; then
    PASSOU=$((PASSOU + 1)); echo "ok   $nome"
  else
    FALHOU=$((FALHOU + 1)); echo "FAIL $nome (exit $rc, esperado $esperado; texto: $texto)"; printf '%s\n' "$saida" | sed 's/^/     /'
  fi
  rm -rf "$d"
}

DEC=docs/architecture/decisions

# covers: RNF-014
caso "tudo certo" 0 "adr-lint: ok (2 ADRs, 2 decisões, 1 RNF)" ":"
# covers: RNF-014
caso "decisão sem ADR" 1 "ERRO D-02: sem ADR" "rm $DEC/ADR-002-outra.md; sed -i '/ADR-002/d' $DEC/README.md"
# covers: RNF-014
caso "decisão em dois ADRs" 1 "ERRO D-01: em mais de um ADR" "sed -i 's/D-02 em 03/D-01 em 03/' $DEC/ADR-002-outra.md"
# covers: RNF-014
caso "ADR sem alternativas rejeitadas" 1 "sem alternativa rejeitada" "sed -i '/^| Outra/d' $DEC/ADR-001-uma.md"
# covers: RNF-014
caso "ADR sem a seção Consequências" 1 "sem a seção Consequências" "sed -i 's/^## Consequências/## Outra/' $DEC/ADR-001-uma.md"
# covers: RNF-014
caso "ADR com status inválido" 1 "status inválido" "sed -i 's/^- \*\*Status:\*\* aceito/- **Status:** talvez/' $DEC/ADR-001-uma.md"
# covers: RNF-014
caso "dois ADRs com o mesmo número" 1 "número repetido: ADR-002" "cp $DEC/ADR-002-outra.md $DEC/ADR-002-copia.md; sed -i 's/D-02 em 03/sem origem/' $DEC/ADR-002-copia.md; echo '| [ADR-002](ADR-002-copia.md) | Cópia |' >> $DEC/README.md"
# covers: RNF-014
caso "ADR fora do índice" 1 "fora do índice: ADR-002-outra.md" "sed -i '/ADR-002/d' $DEC/README.md"
# covers: RNF-014
caso "item do índice sem arquivo" 1 "índice cita arquivo inexistente: ADR-009-fantasma.md" "echo '| [ADR-009](ADR-009-fantasma.md) | Fantasma |' >> $DEC/README.md"
# covers: RNF-014
caso "RNF sem linha na tabela de mecanismos" 1 "ERRO RNF-002: sem linha" "printf '### RNF-002 · Outro\n' >> docs/specs/02-requirements.md"

# covers: RNF-014
real() {
  local saida rc
  saida="$(bash "$LINT" "$RAIZ" 2>&1)"; rc=$?
  if [ "$rc" -eq 0 ]; then PASSOU=$((PASSOU + 1)); echo "ok   os arquivos reais do projeto passam ($saida)"
  else FALHOU=$((FALHOU + 1)); echo "FAIL os arquivos reais do projeto passam (exit $rc)"; printf '%s\n' "$saida" | sed 's/^/     /'; fi
}
real

echo "---"; echo "$PASSOU passaram, $FALHOU falharam"
[ "$FALHOU" -eq 0 ]
