#!/usr/bin/env bash
# Testes da configuração do harness deste projeto (scripts/sdd.conf), um repositório git temporário por cenário.
#   bash scripts/test/test-sdd-conf.sh
# Usa o sdd.conf REAL do projeto (RNF-012, Q-016): prova a configuração de verdade, não uma cópia simplificada.
set -u
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPTS="$(cd "$AQUI/.." && pwd)"
PASSOU=0; FALHOU=0

ok()   { PASSOU=$((PASSOU + 1)); echo "ok   $1"; }
fail() { FALHOU=$((FALHOU + 1)); echo "FAIL $1"; [ -n "${2:-}" ] && printf '%s\n' "$2" | sed 's/^/     /'; }

# Repositório temporário com os scripts do projeto, o sdd.conf real e uma spec mínima.
novo_repo() {
  local d; d="$(mktemp -d)"
  (
    cd "$d" || exit 1
    git init -q -b main && git config user.email t@t && git config user.name t
    mkdir -p scripts/lib scripts/test docs/specs/tasks src/main src/test
    cp "$SCRIPTS"/*.sh scripts/
    cp "$SCRIPTS/lib/sdd-common.sh" scripts/lib/
    cp "$SCRIPTS/sdd.conf" scripts/sdd.conf
    printf '### RF-001 · Registrar\n### RNF-012 · Gate automático em cada tarefa\n' > docs/specs/02-requirements.md
  ) > /dev/null 2>&1
  echo "$d"
}

# Coordenadas impressas por deps_em HEAD, com o sdd.conf real.
coordenadas() {
  (cd "$1" && . scripts/lib/sdd-common.sh && sdd_carregar_conf "$PWD" && deps_em HEAD)
}

# Compara a saída obtida com a esperada (uma coordenada por linha, já ordenada).
confere() {
  local nome="$1" esperado="$2" obtido="$3"
  if [ "$obtido" = "$esperado" ]; then ok "$nome"; else fail "$nome" "esperado:
$esperado
obtido:
$obtido"; fi
}

# --- Cenário 1: parent, dependências com e sem versão, exclusão e plugins ---
# covers: RNF-012
cenario_pom() {
  local d; d="$(novo_repo)"
  cat > "$d/pom.xml" <<'EOF'
<project>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.2</version>
        <relativePath/>
    </parent>
    <groupId>io.example</groupId>
    <artifactId>app</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <dependencies>
        <dependency>
            <groupId>g.a</groupId>
            <artifactId>with-version</artifactId>
            <version>1.0</version>
        </dependency>
        <dependency>
            <groupId>g.a</groupId>
            <artifactId>no-version</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>g.a</groupId>
            <artifactId>with-exclusion</artifactId>
            <version>2.0</version>
            <exclusions>
                <exclusion>
                    <groupId>bad.group</groupId>
                    <artifactId>excluded</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>g.p</groupId>
                <artifactId>plugin-with-version</artifactId>
                <version>3.0</version>
                <configuration>
                    <artifactId>not-a-coordinate</artifactId>
                </configuration>
            </plugin>
            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <argLine>-Dx=1</argLine>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
EOF
  (cd "$d" && git add -A && git commit -qm x) > /dev/null 2>&1
  confere "pom: parent, dependências, exclusão ignorada e plugins" \
"g.a:no-version
g.a:with-exclusion:2.0
g.a:with-version:1.0
g.p:plugin-with-version:3.0
org.apache.maven.plugins:maven-surefire-plugin
org.springframework.boot:spring-boot-starter-parent:4.1.2" "$(coordenadas "$d")"
  rm -rf "$d"
}

# --- Cenário 2: plugin com dependências aninhadas ---
# covers: RNF-012
cenario_plugin_aninhado() {
  local d; d="$(novo_repo)"
  mkdir -p "$d/backend"
  cat > "$d/backend/pom.xml" <<'EOF'
<project>
    <build>
        <plugins>
            <plugin>
                <groupId>g.p</groupId>
                <artifactId>outer</artifactId>
                <dependencies>
                    <dependency>
                        <groupId>g.n</groupId>
                        <artifactId>nested</artifactId>
                        <version>9</version>
                    </dependency>
                </dependencies>
                <version>1.5</version>
            </plugin>
        </plugins>
    </build>
</project>
EOF
  (cd "$d" && git add -A && git commit -qm x) > /dev/null 2>&1
  confere "pom: plugin com dependência aninhada (cada um com as próprias coordenadas)" \
"g.n:nested:9
g.p:outer:1.5" "$(coordenadas "$d")"
  rm -rf "$d"
}

# --- Cenário 3: GitHub Actions ---
# covers: RNF-012
cenario_actions() {
  local d; d="$(novo_repo)"
  mkdir -p "$d/.github/workflows"
  cat > "$d/.github/workflows/ci.yml" <<'EOF'
jobs:
  build:
    steps:
      - name: Baixar
        uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
      - uses: dono/repo@abc123
EOF
  (cd "$d" && git add -A && git commit -qm x) > /dev/null 2>&1
  confere "workflow: tag lida do comentário, ou a ref quando não há comentário" \
"actions/checkout@v7.0.1
dono/repo@abc123" "$(coordenadas "$d")"
  rm -rf "$d"
}

# Repositório com uma tarefa T-010 (covers RF-001) e um pom base, já no branch da tarefa.
repo_com_tarefa() {
  local d; d="$(novo_repo)"
  (
    cd "$d" || exit 1
    printf '<project>\n    <dependencies>\n    </dependencies>\n</project>\n' > pom.xml
    cat > docs/specs/tasks/T-010.md <<'EOF'
---
id: T-010
covers: [RF-001]
evidence_only: []
allowed_paths:
  - src/**
  - pom.xml
  - docs/specs/tasks/T-010.md
allowed_new_dependencies:
  - g.a:allowed:1.0   # a única permitida
---
## Objetivo
Registrar.
## Entradas
- `02-requirements`: RF-001.
## Decisões da tarefa
Nenhuma além da spec.
## Saídas
- `src/main/Registrar.java`
## Critérios de aceite
- **AC-001.1** Dado X, quando Y, então Z.
## Testes exigidos
| AC | Teste | Tag |
|---|---|---|
| AC-001.1 | RegistrarTest | `RF-001` |
## Não-objetivos
- Estorno.
## Checklist de pronto
- [ ] verify verde
EOF
    bash scripts/check-traceability.sh --write
    git add -A && git commit -qm base && git checkout -qb task/T-010
    echo 'class Registrar {}' > src/main/Registrar.java
    printf '@Tag("RF-001")\nclass RegistrarTest {}\n' > src/test/RegistrarTest.java
    bash scripts/check-traceability.sh --write
  ) > /dev/null 2>&1
  echo "$d"
}

# Roda o gate (--rapido: os itens 0 e 2 a 6, sem o build) depois do preparo; confere exit e texto.
gate() {
  local nome="$1" esperado="$2" texto="$3" preparo="$4" d rc saida
  d="$(repo_com_tarefa)"
  saida="$(cd "$d" && eval "$preparo" > /dev/null 2>&1; bash scripts/check-traceability.sh --write > /dev/null 2>&1
           git add -A > /dev/null 2>&1; git commit -qm x > /dev/null 2>&1; bash scripts/verify-task.sh --rapido T-010 2>&1)"; rc=$?
  if [ "$rc" -eq "$esperado" ] && printf '%s' "$saida" | grep -qF -- "$texto"; then
    ok "$nome"
  else
    fail "$nome (exit $rc, esperado $esperado; texto esperado: $texto)" "$saida"
  fi
  rm -rf "$d"
}

DEP_OK='<dependency>\n            <groupId>g.a</groupId>\n            <artifactId>allowed</artifactId>\n            <version>1.0</version>\n        </dependency>'
PLUGIN='<build>\n        <plugins>\n            <plugin>\n                <groupId>g.p</groupId>\n                <artifactId>sneaky</artifactId>\n                <version>2.0</version>\n            </plugin>\n        </plugins>\n    </build>'

# --- Cenário 4: dependência permitida passa; plugin fora do contrato reprova o item 3 ---
# covers: RNF-012
gate "gate: dependência permitida passa (verde parcial com --rapido)" 3 "VERDE PARCIAL" \
  "printf '<project>\n    <dependencies>\n        $DEP_OK\n    </dependencies>\n</project>\n' > pom.xml"
# covers: RNF-012
gate "gate: plugin novo fora do contrato reprova o item 3" 1 "g.p:sneaky:2.0" \
  "printf '<project>\n    <dependencies>\n        $DEP_OK\n    </dependencies>\n    $PLUGIN\n</project>\n' > pom.xml"

# --- Cenário 5: arquivo fora do escopo (item 2) e teste sem a marcação (item 4) ---
# covers: RNF-012
gate "gate: arquivo fora de allowed_paths reprova o item 2" 1 "FALHA item 2" "echo x > fora.txt"
# covers: RNF-012
gate "gate: teste sem @Tag do requisito reprova o item 4" 1 "RF-001 sem marcação" \
  "printf 'class RegistrarTest {}\n' > src/test/RegistrarTest.java"

# --- Cenário 6: '# covers:' num teste de script é reconhecido como marcação ---
# covers: RNF-012
cenario_covers_em_script() {
  local d linha; d="$(novo_repo)"
  (cd "$d" && printf '#!/usr/bin/env bash\n# covers: RNF-012\ntrue\n' > scripts/test/test-x.sh \
     && bash scripts/check-traceability.sh --write) > /dev/null 2>&1
  linha="$(grep -F 'RNF-012' "$d/docs/specs/traceability.md")"
  if printf '%s' "$linha" | grep -qF 'scripts/test/test-x.sh'; then
    ok "matriz: '# covers:' em scripts/test/ conta como marcação"
  else
    fail "matriz: '# covers:' em scripts/test/ conta como marcação" "$linha"
  fi
  rm -rf "$d"
}

cenario_pom
cenario_plugin_aninhado
cenario_actions
cenario_covers_em_script

echo "---"; echo "$PASSOU passaram, $FALHOU falharam"
[ "$FALHOU" -eq 0 ]
