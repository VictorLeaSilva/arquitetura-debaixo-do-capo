# ADR-011 · Aplicação e front-end em containers

- **Status:** aceito
- **Data:** 2026-11-07
- **Origem:** D-11 em 03

## Contexto
Quem chega ao repositório quer rodar o laboratório com poucos comandos (meta de até 3, RNF-001), sem instalar o Java 25 e o Node na própria máquina.

## Decisão
A aplicação e o front-end também rodam em containers, no `compose.yaml`, no perfil `mvp`. Rodar pelo `mvnw` e pelo `npm` continua disponível como modo de desenvolvimento.

## Alternativas rejeitadas
| Alternativa | Por que não |
|---|---|
| Exigir o Java 25 e o Node instalados como caminho principal | Mais pré-requisitos para o leitor e mais chances de diferença de versão entre as máquinas |

## Consequências
- Positivas: o leitor só precisa do Docker; a versão do Java e do Node é a da imagem.
- Negativas e mitigação: a imagem da aplicação precisa ser construída e varrida (Trivy); isso entra na T-018.
- Como se prova: AC-RNF-001.1, nos dois shells (T-018).
