# ADR-021 · Maven com wrapper

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** Q-004

## Contexto
A ferramenta de build compila, testa e empacota o Java. O **wrapper** (`mvnw`) é um script versionado no repositório que baixa a versão certa da ferramenta na primeira execução, e por isso ninguém precisa instalá-la.

## Decisão
Maven, sempre pelo wrapper (`mvnw` e `mvnw.cmd`), no modo `only-script` (sem `.jar` no repositório).

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Gradle (Kotlin DSL) | Mais flexível e rápido, mas a configuração é código e pede mais explicação para quem está começando |

## Consequências
- Positivas: o mais comum em empresas; um clone limpo compila sem instalar nada além do JDK.
- Negativas e mitigação: o `pom.xml` é verboso; os comentários dele explicam cada bloco.
- Como se prova: AC-T001.1 e AC-T001.2 (build num clone limpo, nos dois shells).
