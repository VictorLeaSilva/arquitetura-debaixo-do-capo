# Arquitetura Debaixo do Capô

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

Hoje aparecem também algumas linhas `WARNING: A Java agent has been loaded dynamically`. Elas são um aviso do JDK sobre a forma como a biblioteca de testes Mockito se instala, e não um erro. A configuração que elimina esse aviso está prevista na tarefa de gates de qualidade (T-002).

## Licenças

- **Código** (`backend/`, `frontend/`, `scripts/` e arquivos de configuração): MIT, em [`LICENSE`](LICENSE).
- **Documentação** (`docs/`, `site/` e os arquivos Markdown): Creative Commons Atribuição-NãoComercial-CompartilhaIgual 4.0 Internacional (CC BY-NC-SA 4.0), em [`LICENSE-docs`](LICENSE-docs).

## Especificação

O projeto é construído com *Spec Driven Development*. A especificação começa pela [constituição do projeto](docs/specs/00-constitution.md); as versões usadas estão na [matriz de versões](docs/versoes.md).
