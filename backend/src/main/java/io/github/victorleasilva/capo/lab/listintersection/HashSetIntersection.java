package io.github.victorleasilva.capo.lab.listintersection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A versão boa (A3, passo 4): coloca o catálogo num {@code HashSet} uma vez e testa cada SKU com {@code contains}, que
 * em média encontra o item sem percorrer os outros: O(n).
 */
final class HashSetIntersection implements Intersection {

    @Override
    public List<String> found(List<String> catalog, List<String> batch) {
        Set<String> set = new HashSet<>(catalog);
        List<String> found = new ArrayList<>();
        for (String sku : batch) {
            if (set.contains(sku)) {
                found.add(sku);
            }
        }
        return found;
    }
}
