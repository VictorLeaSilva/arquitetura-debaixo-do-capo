# Visão do produto

> Versão pública e resumida da descoberta de produto (DP-22). Os IDs `DP-nn` e `H-nn` são os citados nas specs.

## Problema
Quem estuda arquitetura de software costuma aprender os conceitos (Big O, concorrência, consistência eventual, padrões de projeto, resiliência) por textos e trechos de código soltos. Falta ver o efeito em execução, medir a diferença entre uma solução ruim e uma boa, e ler explicações que não pressuponham conhecimento prévio nem valham só para uma versão antiga da ferramenta.

## Público
Desenvolvedores que sabem Java básico e querem entender arquitetura de software (DP-09). O primeiro usuário é o próprio autor, que constrói o laboratório para estudar (DP-08).

## Proposta de valor
Para desenvolvedores que estudam arquitetura de software, que leem conceitos sem conseguir vê-los funcionando nem medir suas diferenças, o *Arquitetura Debaixo do Capô* é um laboratório aberto em português, que roda na máquina do leitor e mostra cada conceito num e-commerce com flash sale, em versões ruim, média e boa medidas lado a lado. Diferente de cursos, livros e tutoriais, ele explica o que acontece por baixo, sem pressupor nada além de Java básico, inclusive quando **não** usar um padrão. [DP-12]

## O que o produto não é
- Não é uma loja para vender de verdade nem um modelo para copiar em produção sem crítica (DP-07).
- Não é uma vitrine de loja: a interface é um laboratório de experimentos (DP-03).
- Não exige conta em nuvem paga: tudo roda localmente (DP-11).

## Roteiro
Dez fatias, cada uma com experimentos medidos, testes, páginas de estudo e um post (DP-13):

| Fatia | Tema |
|---|---|
| 0 | Fundação e observabilidade básica |
| 1 | Catálogo e Big O |
| 2 | Padrões de projeto |
| 3 | Flash sale e concorrência |
| 4 | Escala horizontal e balanceamento de carga |
| 5 | Checkout distribuído (mensageria, outbox, saga) |
| 6 | Dados distribuídos (réplicas, PACELC, NoSQL) |
| 7 | Observabilidade avançada |
| 8 | Segurança |
| 9 | Cloud local |

O MVP está descrito em `mvp.md`.
