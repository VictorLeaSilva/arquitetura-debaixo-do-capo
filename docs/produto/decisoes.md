# Decisões de produto

Versão pública das decisões de produto citadas nas specs (DP-22). Os IDs são estáveis; os números que faltam são decisões de processo que não afetam o que é construído.

| ID | Pergunta | Decisão |
|---|---|---|
| DP-01 | Qual domínio serve de case? | E-commerce com flash sale: o cenário de "últimas unidades disputadas por muitos compradores" força concorrência e idempotência de verdade, e o catálogo dá volume para os experimentos de listas e Big O. Descartados: venda de ingressos (pouco catálogo) e e-commerce clássico (sem o problema de concorrência). |
| DP-03 | Como é a interface? | Um laboratório visual que executa versões ruim, média e boa lado a lado e mostra as medições; não é uma vitrine de loja. |
| DP-04 | Idioma | Código em inglês; documentação em português. |
| DP-05 | Profundidade da explicação | Detalhada, sem pressupor nada além de Java básico. Toda explicação cobre: termos definidos na primeira vez; o que é; o que cada comando ou configuração faz; por que importa; alternativas; por que escolhemos e por que testamos assim; como verificar; armadilhas; diferenças entre Windows e Linux; particularidades de versão, com link para as notas oficiais. Comandos copiáveis em bash e PowerShell. |
| DP-06 | Visibilidade | Repositório público no GitHub. |
| DP-07 | Princípio de desenho | Pragmatismo acima de cerimônia: camada, interface ou padrão só entra com benefício visível, e o projeto mostra também quando **não** usar. |
| DP-08 | Perfil primário | O próprio autor, como estudante de arquitetura; os leitores se beneficiam em seguida. |
| DP-09 | Conhecimento mínimo do leitor | Lógica de programação e Java básico; todo o resto é explicado. |
| DP-11 | Nuvem | Só local e custo zero; qualquer leitor reproduz. |
| DP-12 | Proposta de valor | A frase de `visao.md`. |
| DP-13 | Roteiro | Dez fatias, de 0 a 9 (`visao.md`), cada uma com especificação própria. |
| DP-16 | Escopo do MVP | Fundação + fatia 1 reduzida (busca em lista e N+1) + 1 experimento da flash sale, para medir o custo de uma fatia barata e de uma cara. |
| DP-17 | Régua de custo | Média de até 5 semanas por fatia, contando especificação, planejamento, código, testes, revisão, documentação e post; 1 post por fatia. |
| DP-18 | Rigor dos documentos | Todo número tem rótulo (medido, meta ou estimativa); tecnologia só entra depois da descoberta de produto. |
| DP-20 | Testar a H-02? | Aceita como premissa, sem teste agora. |
| DP-21 | Formato dos experimentos | Cada experimento é um módulo autodescrito (título, versões, medições, perguntas de verificação), e cada página de estudo termina com perguntas de verificação. Login, progresso por pessoa e execução isolada por pessoa ficam fora do projeto. |
