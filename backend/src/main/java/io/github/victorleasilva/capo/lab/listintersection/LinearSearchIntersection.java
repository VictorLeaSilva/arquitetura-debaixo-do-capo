package io.github.victorleasilva.capo.lab.listintersection;

import java.util.ArrayList;
import java.util.List;

/**
 * A versão ruim (A3, passo 2): para cada SKU do lote, {@code List.contains} percorre o catálogo do começo. Com n itens
 * no lote e n no catálogo, são até n × n comparações: O(n²).
 */
final class LinearSearchIntersection implements Intersection {

    @Override
    public List<String> found(List<String> catalog, List<String> batch) {
        List<String> found = new ArrayList<>();
        for (String sku : batch) {
            if (catalog.contains(sku)) {
                found.add(sku);
            }
        }
        return found;
    }
}
