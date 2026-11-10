# ADR-025 · Só ArchUnit para as fronteiras no MVP

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** Q-007

## Contexto
Um monólito modular (ADR-001) só se mantém modular se alguém conferir as fronteiras. **ArchUnit** é uma biblioteca que escreve regras de arquitetura como testes (por exemplo, "o domínio não depende do Spring"). **Spring Modulith** é um projeto do Spring que verifica módulos e gera a documentação deles.

## Decisão
Só ArchUnit no MVP, com as regras escritas no próprio projeto e provadas por violações propositais (T-002).

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Spring Modulith, com ArchUnit para as regras extras | Um conceito a mais agora; ele é reavaliado na fatia 5, onde o registro de publicação de eventos dele conversa com a outbox |
| Um módulo Maven separado por módulo de negócio | O custo de vários `pom.xml` sem necessidade agora |

## Consequências
- Positivas: as regras ficam explícitas no código, o que é didático.
- Negativas e mitigação: uma regra pode passar sem verificar nada quando não há classes; cada regra tem uma fixture de violação, e o teste de produção exige que a importação encontre classes.
- Como se prova: `ArchitectureRulesTest` e `ArchitectureTest` (RNF-004, T-002).
