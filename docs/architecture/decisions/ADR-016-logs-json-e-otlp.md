# ADR-016 · Logs em JSON e por OTLP

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-16 em 03

## Contexto
Logs em JSON têm campos (`trace_id`, `run_id`) que as ferramentas leem sem adivinhar o formato. Para ligar um log ao trace no Grafana, o log precisa chegar ao Loki. O Spring Boot 4.1 exporta logs por OTLP (`management.opentelemetry.logging.export.otlp.endpoint`), mas o *appender* do Logback que faz a ponte não faz parte dele.

## Decisão
Logs em JSON na saída padrão **e** enviados por OTLP com o appender Logback do OpenTelemetry (Q-010), configurado no `logback-spring.xml` e instalado na inicialização; a versão é confirmada na T-015.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Só na saída padrão | Sem Loki no MVP, sem a ligação log ↔ trace no Grafana |
| Um coletor dos logs do Docker (como o Grafana Alloy) | Mais memória e mais um componente para explicar |

## Consequências
- Positivas: clicar num trace mostra os logs dele; a saída padrão continua legível no terminal.
- Negativas e mitigação: uma dependência e uma configuração de Logback a mais; a página de estudo da observabilidade explica as duas.
- Como se prova: AC-009.2 e AC-009.4 (T-015).
