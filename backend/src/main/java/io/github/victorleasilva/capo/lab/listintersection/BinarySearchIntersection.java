package io.github.victorleasilva.capo.lab.listintersection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A versão média (A3, passo 3): ordena uma cópia do catálogo uma vez, O(n log n), e procura cada SKU com
 * {@code Collections.binarySearch}, O(log n) por SKU.
 */
final class BinarySearchIntersection implements Intersection {

    @Override
    public List<String> found(List<String> catalog, List<String> batch) {
        List<String> sorted = new ArrayList<>(catalog);
        Collections.sort(sorted);
        List<String> found = new ArrayList<>();
        for (String sku : batch) {
            if (Collections.binarySearch(sorted, sku) >= 0) {
                found.add(sku);
            }
        }
        return found;
    }
}
