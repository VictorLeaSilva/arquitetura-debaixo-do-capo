# Arquitetura Debaixo do Capô

![ci](https://github.com/VictorLeaSilva/arquitetura-debaixo-do-capo/actions/workflows/ci.yml/badge.svg)

Um laboratório aberto, em português, que roda na sua máquina e mostra conceitos de arquitetura de software num e-commerce com flash sale, em versões ruim, média e boa medidas lado a lado, explicando o que acontece por baixo.

**Status:** em construção — Fase 0 (fundação).

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

Durante os testes pode aparecer a linha `Java HotSpot(TM) 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended`. Ela também não é erro: o Mockito e o JaCoCo entram na JVM como *agentes Java*, e isso desliga parte de uma otimização de inicialização da JVM (o *class data sharing*). O único efeito é os testes subirem um pouco mais devagar.

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

## Licenças

- **Código** (`backend/`, `frontend/`, `scripts/` e arquivos de configuração): MIT, em [`LICENSE`](LICENSE).
- **Documentação** (`docs/`, `site/` e os arquivos Markdown): Creative Commons Atribuição-NãoComercial-CompartilhaIgual 4.0 Internacional (CC BY-NC-SA 4.0), em [`LICENSE-docs`](LICENSE-docs).

## Especificação

O projeto é construído com *Spec Driven Development*. A especificação começa pela [constituição do projeto](docs/specs/00-constitution.md); as versões usadas estão na [matriz de versões](docs/versoes.md).
