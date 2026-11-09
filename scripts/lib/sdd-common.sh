#!/usr/bin/env bash
# Funções comuns do harness SDD (frontmatter, configuração, dependências por ecossistema).

# Carrega scripts/sdd.conf com padrões razoáveis.
sdd_carregar_conf() {
  local raiz="$1"
  SDD_PRINCIPAL="main"
  SDD_BUILD_CMD="true"
  SDD_TEST_FILES='(^|/)(src/test/|tests?/|__tests__/)|(\.test|\.spec|_test)\.[A-Za-z]+$'
  SDD_TAG_LINE='@Tag\(|@pytest\.mark\.req|req:|\[R(N)?F-'
  SDD_REQ_ID='RN?F-[0-9]{3}'
  SDD_DEPS="none"
  SDD_SRC_FILES='\.(java|kt|ts|tsx|js|py|go|cs|rb)$'
  SDD_FORBIDDEN='TODO|FIXME|System\.out|printStackTrace|console\.log|@Disabled|\.skip\(|xit\('
  SDD_FORBIDDEN_TEST='Thread\.sleep|time\.sleep|setTimeout\('
  SDD_EVIDENCE_IDS=''
  SDD_MANDATORY='RF-[0-9]{3}'
  if [ -f "$raiz/scripts/sdd.conf" ]; then
    # shellcheck disable=SC1091
    . "$raiz/scripts/sdd.conf"
  fi
}

# fm_lista <arquivo> <chave>: itens de uma lista do frontmatter, um por linha.
# Aceita `chave: [a, b]` e a forma em bloco (`chave:` seguido de `  - a`). Ignora comentários `#`.
fm_lista() {
  awk -v chave="$2" '
    NR == 1 && $0 == "---" { dentro = 1; next }
    dentro && $0 == "---" { exit }
    !dentro { next }
    { sub(/[ \t]+#.*$/, "") }
    bloco && /^[ \t]*-[ \t]*/ { s = $0; sub(/^[ \t]*-[ \t]*/, "", s); gsub(/^["\x27]|["\x27]$/, "", s); if (s != "") print s; next }
    bloco && /^[^ \t]/ { bloco = 0 }
    $0 ~ "^" chave ":" {
      v = $0; sub("^" chave ":[ \t]*", "", v)
      if (v == "") { bloco = 1; next }
      gsub(/^\[|\][ \t]*$/, "", v)
      n = split(v, partes, ",")
      for (i = 1; i <= n; i++) { s = partes[i]; gsub(/^[ \t"\x27]+|[ \t"\x27]+$/, "", s); if (s != "") print s }
    }
  ' "$1"
}

# sdd_requisitos [arquivo]: "ID<TAB>nome" de cada requisito de 02, declarado como título
# (`### RF-001 · Nome`) ou como linha de tabela (`| **RNF-004** | Nome | ...`).
sdd_requisitos() {
  tr -d '\r' < "${1:-docs/specs/02-requirements.md}" | R_ID="$SDD_REQ_ID" awk -F'|' '
    BEGIN { re = "^(" ENVIRON["R_ID"] ")$" }
    /^### / { s = $0; sub(/^### +/, "", s); id = s; sub(/[ \t].*$/, "", id)
              if (id ~ re) { n = s; sub(/^[^ \t]+[ \t]*(·|-|—)?[ \t]*/, "", n); if (!(id in v)) { v[id]; print id "\t" n } } next }
    /^\|/ { id = $2; gsub(/[ \t*`]/, "", id)
            if (id ~ re) { n = $3; gsub(/^[ \t*]+|[ \t*]+$/, "", n); if (!(id in v)) { v[id]; print id "\t" n } } }'
}

# deps_em <rev>: coordenadas de dependências diretas declaradas na revisão, uma por linha, ordenadas.
deps_em() {
  local rev="$1" f
  case "$SDD_DEPS" in
    maven)
      git ls-tree -r --name-only "$rev" | grep -E '(^|/)pom\.xml$' | while read -r f; do
        git show "$rev:$f" | tr -d '\r' | awk '
          /<dependency>/ { d = 1; g = ""; a = ""; next }
          d && /<groupId>/ { g = $0; gsub(/.*<groupId>|<\/groupId>.*/, "", g) }
          d && /<artifactId>/ { a = $0; gsub(/.*<artifactId>|<\/artifactId>.*/, "", a) }
          /<\/dependency>/ { if (g != "" && a != "") print g ":" a; d = 0 }'
      done ;;
    npm)
      git ls-tree -r --name-only "$rev" | grep -E '(^|/)package\.json$' | grep -v node_modules | while read -r f; do
        # O arquivo vira uma linha só, para aceitar JSON formatado ou compacto.
        git show "$rev:$f" | tr -d '\r\n' | awk '{
          resto = $0
          while (match(resto, /"(dependencies|devDependencies|peerDependencies|optionalDependencies)"[ \t]*:[ \t]*\{[^}]*\}/)) {
            bloco = substr(resto, RSTART, RLENGTH); resto = substr(resto, RSTART + RLENGTH)
            sub(/^[^{]*\{/, "", bloco)
            while (match(bloco, /"[^"]+"[ \t]*:/)) {
              s = substr(bloco, RSTART + 1, RLENGTH); sub(/"[ \t]*:$/, "", s); print s
              bloco = substr(bloco, RSTART + RLENGTH)
              sub(/^[ \t]*"[^"]*"/, "", bloco)
            }
          }
        }'
      done ;;
    pip)
      git ls-tree -r --name-only "$rev" | grep -E '(^|/)requirements[^/]*\.txt$' | while read -r f; do
        git show "$rev:$f" | tr -d '\r' | grep -Eo '^[A-Za-z0-9][A-Za-z0-9._-]*' | tr '[:upper:]' '[:lower:]'
      done ;;
    custom) sdd_deps_custom "$rev" ;;   # defina sdd_deps_custom no sdd.conf (pyproject, go.mod, csproj...)
    *) : ;;
  esac | LC_ALL=C sort -u
}
