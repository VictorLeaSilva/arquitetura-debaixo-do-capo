package io.github.victorleasilva.capo.lab.run;

/** Um tamanho pedido passa dos produtos carregados; vira um 422 {@code /problems/insufficient-data}. */
public class InsufficientDataException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int size;
    private final int seedProducts;

    public InsufficientDataException(int size, int seedProducts) {
        super("tamanho maior que os produtos carregados");
        this.size = size;
        this.seedProducts = seedProducts;
    }

    public int size() {
        return size;
    }

    public int seedProducts() {
        return seedProducts;
    }
}
