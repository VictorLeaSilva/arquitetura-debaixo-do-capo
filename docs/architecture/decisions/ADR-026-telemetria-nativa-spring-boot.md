# ADR-026 · Telemetria pelo suporte nativo do Spring Boot

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** Q-008

## Contexto
Há duas formas comuns de gerar telemetria numa aplicação Java: o **agente Java do OpenTelemetry**, carregado junto da JVM, que instrumenta bibliotecas sem nenhum código; e o **suporte nativo do Spring Boot** (Micrometer e a exportação OTLP), em que os spans e as métricas próprios são escritos no código.

## Decisão
O suporte nativo do Spring Boot (o starter de OpenTelemetry, com exportação OTLP), com os spans e as métricas do laboratório escritos no código (`03 §8`).

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| O agente Java do OpenTelemetry | É o melhor exemplo de "o que a ferramenta faz sozinha"; entra na fatia 7, comparado lado a lado com o suporte nativo |
| Os dois desde o MVP | Mistura os dois antes da hora e confunde a explicação |

## Consequências
- Positivas: o MVP precisa de spans e métricas próprios de qualquer jeito, e o suporte nativo não acrescenta um processo à parte.
- Negativas e mitigação: as consultas SQL não viram spans sozinhas; isso é resolvido pelo ADR-015.
- Como se prova: AC-009.1 a AC-009.3 (T-015).
