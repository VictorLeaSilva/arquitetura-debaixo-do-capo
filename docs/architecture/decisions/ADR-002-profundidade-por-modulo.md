# ADR-002 · Profundidade da arquitetura por módulo

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-02 em 03

## Contexto
Nem todo módulo tem a mesma complexidade. A **arquitetura hexagonal** (também chamada de *portas e adaptadores*) isola o domínio — as regras de negócio — de frameworks e do banco, por meio de interfaces (as portas). Ela compensa onde há regras e concorrência, mas custa mapeamentos e camadas a mais. O princípio P2 pede que cada camada justifique o próprio custo.

## Decisão
- `catalog` e `lab`: camadas simples (controller → serviço → repositório), com JPA direto.
- `inventory` e `ordering`: hexagonal enxuta, com o domínio puro (sem Spring nem JPA), casos de uso e adaptadores.
- Cada módulo expõe um pacote `api`; as versões "ruins" dos experimentos ficam num pacote `experiment`, uma API restrita, usada só pelo `lab` e por outros pacotes `experiment` (`01 §3`).
- Chave estrangeira entre tabelas de módulos diferentes só para `product(id)`, como acoplamento aceito.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Hexagonal em todos os módulos | O catálogo é praticamente leitura; teria mapeamentos duplicados sem nenhuma regra a proteger |
| Camadas em todos os módulos | A regra do estoque (nunca vender mais do que existe) ficaria acoplada ao JPA e difícil de testar sem banco |

## Consequências
- Positivas: o custo da arquitetura aparece só onde existe regra; o projeto mostra, na prática, quando a hexagonal vale e quando não vale.
- Negativas e mitigação: dois estilos no mesmo código pedem explicação; ela fica nas páginas de estudo e neste ADR.
- Como se prova: as regras `DOMAIN_IS_FRAMEWORK_FREE`, `EXPERIMENT_ONLY_FROM_LAB_OR_EXPERIMENT` e `MODULES_TALK_ONLY_THROUGH_API` (RNF-004).
