#!/usr/bin/env bash
# Testes do harness SDD: um repositório Git temporário por cenário.
#   scripts/test/run-tests.sh
set -u
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS="$(cd "$AQUI/.." && pwd)"
PASSOU=0; FALHOU=0

novo_repo() {
  local d; d="$(mktemp -d)"
  (
    cd "$d" || exit 1
    git init -q -b main && git config user.email t@t && git config user.name t
    mkdir -p scripts/lib docs/specs/tasks src/main src/test
    cp "$SCRIPTS"/*.sh scripts/
    cp "$SCRIPTS/lib/sdd-common.sh" scripts/lib/
    cat > scripts/sdd.conf <<'EOF'
SDD_BUILD_CMD='true'
SDD_DEPS='npm'
SDD_TEST_FILES='(^|/)src/test/'
SDD_TAG_LINE='req:'
SDD_SRC_FILES='\.js$'
SDD_FORBIDDEN='TODO|console\.log'
SDD_FORBIDDEN_TEST='setTimeout\('
SDD_MANDATORY='RF-[0-9]{3}'
EOF
    printf '### RF-001 · Registrar\n### RF-002 · Estornar\n### RNF-001 · Disponibilidade\n' > docs/specs/02-requirements.md
    echo '{ "dependencies": { "a": "1" } }' > package.json
    cat > docs/specs/tasks/T-010.md <<'EOF'
---
id: T-010
covers: [RF-001]
evidence_only: []
allowed_paths:
  - src/**   # comentário ignorado
  - docs/specs/tasks/T-010.md
allowed_new_dependencies: []
---
## Objetivo
Registrar.
## Entradas
- `02-requirements`: RF-001.
## Decisões da tarefa
Nenhuma além da spec.
## Saídas
- `src/main/registrar.js`
## Critérios de aceite
- **AC-001.1** Dado X, quando Y, então Z.
## Testes exigidos
| AC | Teste | Tag |
|---|---|---|
| AC-001.1 | registrar.test.js | `RF-001` |
## Não-objetivos
- Estorno.
## Checklist de pronto
- [ ] verify verde
EOF
    bash scripts/check-traceability.sh --write > /dev/null
    git add -A && git commit -qm base && git checkout -qb task/T-010
  ) > /dev/null 2>&1
  echo "$d"
}

# caso <nome> <exit esperado> <texto esperado no log ou ""> <comandos de preparo>
caso() {
  local nome="$1" esperado="$2" texto="$3" preparo="$4" opc="${5:-}" d rc saida
  d="$(novo_repo)"
  saida="$(cd "$d" && eval "$preparo" > /dev/null 2>&1; git add -A > /dev/null 2>&1; git commit -qm x > /dev/null 2>&1
           if [ "$opc" = rapido ]; then bash scripts/verify-task.sh --rapido T-010 2>&1; else bash scripts/verify-task.sh T-010 2>&1; fi)"; rc=$?
  if [ "$rc" -eq "$esperado" ] && { [ -z "$texto" ] || printf '%s' "$saida" | grep -qF -- "$texto"; }; then
    PASSOU=$((PASSOU + 1)); echo "ok   $nome"
  else
    FALHOU=$((FALHOU + 1)); echo "FAIL $nome (exit $rc, esperado $esperado)"; printf '%s\n' "$saida" | sed 's/^/     /'
  fi
  rm -rf "$d"
}

FELIZ='echo "x" > src/main/registrar.js; echo "// req: RF-001" > src/test/registrar.test.js; bash scripts/check-traceability.sh --write'

caso "verde no caminho feliz"            0 "RESULTADO: VERDE" "$FELIZ"
caso "arquivo fora de allowed_paths"     1 "FALHA item 2"     "$FELIZ; echo x > fora.txt"
caso "dependência nova não autorizada"   1 "    b"            "$FELIZ; echo '{ \"dependencies\": { \"a\": \"1\", \"b\": \"2\" } }' > package.json"
caso "covers sem marcação em teste"      1 "RF-001 sem marcação" 'echo x > src/main/registrar.js; echo "// nada" > src/test/registrar.test.js; bash scripts/check-traceability.sh --write'
caso "traceability.md desatualizado"     1 "traceability.md desatualizado" 'echo x > src/main/registrar.js; echo "// req: RF-001" > src/test/registrar.test.js'
caso "padrão proibido em produção"       1 "FALHA item 5"     "$FELIZ; echo 'console.log(1)' >> src/main/registrar.js"
caso "sleep proibido só em teste"        1 "setTimeout"       "$FELIZ; echo 'setTimeout(f, 10)' >> src/test/registrar.test.js"
caso "setTimeout em produção é permitido" 0 "RESULTADO: VERDE" "$FELIZ; echo 'setTimeout(f, 10)' >> src/main/registrar.js"
caso "saída declarada ausente"           1 "FALHA item 6"     'echo "// req: RF-001" > src/test/registrar.test.js; bash scripts/check-traceability.sh --write'
caso "build falhando"                    1 "FALHA item 1"     "$FELIZ; sed -i \"s/SDD_BUILD_CMD='true'/SDD_BUILD_CMD='false'/\" scripts/sdd.conf; echo 'scripts/**' >> /dev/null; sed -i 's|  - src/\*\*.*|  - src/**\n  - scripts/**|' docs/specs/tasks/T-010.md"

caso "--rapido pula o build com exit 3"   3 "VERDE PARCIAL"    "$FELIZ; sed -i 's|bash scripts/verify-task.sh T-010|&|' /dev/null" rapido
caso "contrato: AC sem teste exigido"     1 "AC sem linha em Testes exigidos: AC-001.2" "$FELIZ; sed -i 's|^- \*\*AC-001.1\*\*.*|&\n- **AC-001.2** outro.|' docs/specs/tasks/T-010.md"
caso "contrato: Q citada sem decisão"     1 "Q-001 ainda sem decisão" "$FELIZ; printf '| ID | T | P | O | D | Data |\n|---|---|---|---|---|---|\n| Q-001 | T-010 | p | a / b | **Proposta:** a | 2026-01-01 |\n' > docs/specs/open-questions.md; echo 'Aplica Q-001.' >> docs/specs/tasks/T-010.md"
caso "contrato: placeholder pendente"     1 "placeholder não preenchido" "$FELIZ; echo 'Ver <decidir depois>.' >> docs/specs/tasks/T-010.md"

echo "---"; echo "$PASSOU passaram, $FALHOU falharam"
[ "$FALHOU" -eq 0 ]
