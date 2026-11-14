package io.github.victorleasilva.capo.lab.seed;

/** Leitura do tamanho da carga, a variável {@code LAB_SEED_PRODUCTS} (A1, passo 1). */
public final class SeedSettings {

    static final String KEY = "LAB_SEED_PRODUCTS";
    static final int DEFAULT_PRODUCTS = 1_000_000;
    private static final int MIN_PRODUCTS = 1;
    private static final int MAX_PRODUCTS = 1_000_000;

    private SeedSettings() {}

    /**
     * Ausente → 1.000.000. Senão, um inteiro de 1 a 1.000.000; qualquer outro valor impede a aplicação de subir, com
     * uma mensagem que cita a chave e o valor recebido (AC-001.4).
     */
    public static int parse(String raw) {
        if (raw == null) {
            return DEFAULT_PRODUCTS;
        }
        try {
            int products = Integer.parseInt(raw);
            if (products >= MIN_PRODUCTS && products <= MAX_PRODUCTS) {
                return products;
            }
        } catch (NumberFormatException e) {
            // cai na mensagem abaixo
        }
        throw new IllegalStateException(
                KEY + " inválido: \"" + raw + "\" (use um inteiro de " + MIN_PRODUCTS + " a " + MAX_PRODUCTS + ")");
    }
}
