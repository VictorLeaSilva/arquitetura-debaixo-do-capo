package io.github.victorleasilva.capo.lab.run;

import java.util.Map;

/**
 * O ponto de extensão de cada experimento: um bean por experimento (T-013, T-014, T-021), achado pelo id do descritor.
 * O motor chama {@link #run} uma vez por versão e por tamanho, na ordem do pedido, já com a trava de execução segura.
 */
public interface ExperimentRunner {

    /** O id do descritor que esta implementação executa. */
    String experimentId();

    /**
     * Executa uma versão num tamanho.
     *
     * @param size o valor do parâmetro com {@code isSize}, ou {@code null} se o experimento não tem esse parâmetro
     * @param parameters os parâmetros efetivos da execução (os do pedido, completados com os padrões do descritor)
     */
    VariantOutcome run(String variant, Integer size, Map<String, Object> parameters);
}
