# ADR-001 · Monólito modular num único processo

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-01 em 03

## Contexto
O MVP é construído por uma pessoa e precisa ser simples de subir, de testar e de explicar. Ao mesmo tempo, o roteiro prevê, na fatia 5, extrair partes do sistema para serviços separados (o padrão *Strangler Fig*: substituir um sistema aos poucos, uma fatia de cada vez). Um **monólito** é uma aplicação que roda num processo só; um **monólito modular** é um monólito dividido em módulos com fronteiras explícitas, que só conversam por interfaces públicas.

## Decisão
Uma única aplicação Spring Boot, com os módulos `catalog` (catálogo), `inventory` (estoque), `ordering` (pedido) e `lab` (laboratório), cada um com fronteiras verificadas por teste de arquitetura (ADR-025).

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Microsserviços já no MVP | Traria rede entre serviços, vários deploys e dados distribuídos sem nenhum requisito do MVP que precise disso (P2) |
| Monólito sem módulos | Não prepararia a extração da fatia 5: as dependências cruzadas só apareceriam na hora de separar |

## Consequências
- Positivas: um processo, um deploy e uma transação local quando dois módulos colaboram (como pedido e estoque na flash sale); a extração futura parte de fronteiras que já existem.
- Negativas e mitigação: módulos no mesmo processo podem "vazar" uns para os outros; a mitigação é o teste de arquitetura (RNF-004).
- Como se prova: `ArchitectureTest` e `ArchitectureRulesTest` (RNF-004, T-002).
