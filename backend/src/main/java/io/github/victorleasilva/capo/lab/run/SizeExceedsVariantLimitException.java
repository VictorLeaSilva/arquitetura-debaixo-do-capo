package io.github.victorleasilva.capo.lab.run;

/** Um tamanho pedido passa do {@code maxSize} de uma versão; vira um 422 {@code /problems/size-exceeds-variant-limit}. */
public class SizeExceedsVariantLimitException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String variant;
    private final int maxSize;
    private final int size;

    public SizeExceedsVariantLimitException(String variant, int maxSize, int size) {
        super("tamanho acima do limite da versão " + variant);
        this.variant = variant;
        this.maxSize = maxSize;
        this.size = size;
    }

    public String variant() {
        return variant;
    }

    public int maxSize() {
        return maxSize;
    }

    public int size() {
        return size;
    }
}
