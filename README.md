# Arquitetura Debaixo do Capô

![ci](https://github.com/VictorLeaSilva/arquitetura-debaixo-do-capo/actions/workflows/ci.yml/badge.svg)

Um laboratório aberto, em português, que roda na sua máquina e mostra conceitos de arquitetura de software num e-commerce com flash sale, em versões ruim, média e boa medidas lado a lado, explicando o que acontece por baixo.

**Status:** em construção — Fase 0 (fundação).

## Como subir a infraestrutura local

Pré-requisito: **Docker** com o Compose v2 (no Windows, o Docker Desktop; no Linux, o Docker Engine com o plugin `docker compose`).

O arquivo [`compose.yaml`](compose.yaml) descreve dois serviços, os dois no perfil `mvp`:

| Serviço | O que é | Porta na sua máquina |
|---|---|---|
| `postgres` | O banco de dados do laboratório (PostgreSQL 18.6) | `5432` |
| `observability` | A pilha de observabilidade: recebe traces, métricas e logs por OpenTelemetry e mostra tudo no Grafana | `3000` (Grafana) e `4318` (entrada da telemetria) |

Todas as portas ficam presas a `127.0.0.1`: só a sua própria máquina consegue acessá-las, nunca a rede.

**1. Criar o arquivo `.env`** a partir do modelo. Ele guarda o usuário e a senha do banco e **nunca** vai para o git; os valores do modelo são só para uso local.

```bash
cp .env.example .env
```

```powershell
Copy-Item .env.example .env
```

**2. Subir os serviços.** O `--profile mvp` escolhe os serviços do MVP, o `-d` deixa os containers rodando em segundo plano, e o `--wait` só devolve o terminal quando os dois estiverem **prontos** (os *health checks* passaram), não apenas iniciados. Na primeira vez, o Docker baixa as imagens, o que leva alguns minutos.

```bash
docker compose --profile mvp up -d --wait
```

```powershell
docker compose --profile mvp up -d --wait
```

**3. Conferir.** Os dois serviços devem aparecer como `healthy`:

```bash
docker compose ps
```

```powershell
docker compose ps
```

O Grafana abre em `http://localhost:3000`, com usuário e senha `admin` / `admin` (o padrão desta imagem, que é feita só para desenvolvimento).

**Para parar** os serviços, mantendo os dados do banco:

```bash
docker compose --profile mvp down
```

```powershell
docker compose --profile mvp down
```

**Para parar e apagar também os dados** do banco: o `-v` remove o volume `postgres-data`, onde o PostgreSQL guarda as tabelas. Na próxima subida, o banco começa vazio.

```bash
docker compose --profile mvp down -v
```

```powershell
docker compose --profile mvp down -v
```

## Como compilar e testar o backend

Pré-requisito: **JDK 25** instalado (qualquer distribuição). Não é preciso instalar o Maven: o *Maven Wrapper* (`mvnw`) baixa a versão certa na primeira execução.

No bash (Linux, macOS ou Git Bash no Windows):

```bash
cd backend
./mvnw -q verify
```

No PowerShell (Windows):

```powershell
cd backend
.\mvnw.cmd -q verify
```

O `-q` (*quiet*) esconde o log normal do Maven, mas não o da aplicação: durante os testes, o Spring Boot sobe e imprime algumas linhas de log próprias. O comando deu certo quando termina com código de saída `0` (confira com `echo $?` no bash ou `$LASTEXITCODE` no PowerShell) e sem nenhuma linha `[ERROR]`.

O `verify` não só compila e testa: ele também **reprova o build** quando encontra:

- código fora da formatação padrão (Spotless, com o estilo Palantir);
- bugs comuns no código compilado (SpotBugs);
- cobertura de testes abaixo do piso (JaCoCo: 70% das linhas);
- violação das regras de arquitetura (ArchUnit).

Durante os testes pode aparecer a linha `... 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended` (o começo muda conforme o JDK: `Java HotSpot(TM)` no JDK da Oracle, `OpenJDK` no Temurin usado pelo CI). Ela também não é erro: o Mockito e o JaCoCo entram na JVM como *agentes Java*, e isso desliga parte de uma otimização de inicialização da JVM (o *class data sharing*). O único efeito é os testes subirem um pouco mais devagar. Também pode aparecer `WARNING: sun.misc.Unsafe::staticFieldBase has been called by com.diffplug.spotless...`: é o JDK avisando que o próprio Spotless usa uma API antiga do Java, que vai ser removida; não diz respeito ao código do projeto.

O relatório de cobertura fica em `backend/target/site/jacoco/index.html`. Se o build reprovar só pela formatação, o próprio Spotless corrige:

```bash
cd backend
./mvnw spotless:apply
```

```powershell
cd backend
.\mvnw.cmd spotless:apply
```

O mesmo `verify` roda no GitHub a cada pull request, junto com um detector de segredos e a revisão das dependências novas (`.github/workflows/ci.yml`).

## Site de documentação

As páginas de estudo ficam num site feito com Docusaurus, na pasta `site/`. Pré-requisito: **Node.js 26** (a versão fixada em `site/.nvmrc`; confira com `node -v`). O npm vem junto com o Node.js.

No bash (Linux, macOS ou Git Bash no Windows):

```bash
cd site
npm ci
npm start
```

No PowerShell (Windows):

```powershell
cd site
npm ci
npm start
```

O `npm ci` instala exatamente as versões do `package-lock.json` (na primeira vez, leva alguns minutos). O `npm start` sobe o site em `http://127.0.0.1:3001/arquitetura-debaixo-do-capo/`, só para a sua máquina, e atualiza a página no navegador a cada arquivo alterado; para parar, `Ctrl+C`. A porta é a 3001 porque a 3000 é do Grafana.

Para conferir tudo o que o CI confere no site (tipos, lint, formatação e o build, que reprova link interno quebrado):

```bash
cd site
npm run verify
```

```powershell
cd site
npm run verify
```

Durante o build pode aparecer `ExperimentalWarning: localStorage is not available because --localstorage-file was not provided.`: é um aviso do Node 26 sobre um recurso que o build não usa, não um erro. As páginas de estudo também são conferidas pelo `scripts/doc-lint.sh` (as 13 seções do padrão de explicação e os comandos em abas bash e PowerShell); a página-modelo é `site/docs/como-estudar/modelo-de-pagina.mdx`.

## Licenças

- **Código** (`backend/`, `frontend/`, `scripts/` e arquivos de configuração): MIT, em [`LICENSE`](LICENSE).
- **Documentação** (`docs/`, `site/` e os arquivos Markdown): Creative Commons Atribuição-NãoComercial-CompartilhaIgual 4.0 Internacional (CC BY-NC-SA 4.0), em [`LICENSE-docs`](LICENSE-docs).

## Especificação

O projeto é construído com *Spec Driven Development*. A especificação começa pela [constituição do projeto](docs/specs/00-constitution.md); as versões usadas estão na [matriz de versões](docs/versoes.md).
