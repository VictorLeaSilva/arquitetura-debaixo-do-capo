package io.github.victorleasilva.capo.lab.listintersection;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

/**
 * As entradas e o checksum do list-intersection (A3, passos 1 e 5). O lote é o mesmo para as três versões, montado com
 * semente fixa, e o checksum prova que elas encontraram os mesmos SKUs (P3).
 */
final class IntersectionBatch {

    private static final long SEED = 42;

    private IntersectionBatch() {}

    /** n/2 SKUs do catálogo, sorteados sem reposição, e n/2 inexistentes ({@code MISS-1}..), embaralhados. */
    static List<String> of(List<String> catalog) {
        int half = catalog.size() / 2;
        List<String> copy = new ArrayList<>(catalog);
        Collections.shuffle(copy, new Random(SEED));
        List<String> batch = new ArrayList<>(copy.subList(0, half));
        for (int i = 1; i <= half; i++) {
            batch.add("MISS-" + i);
        }
        Collections.shuffle(batch, new Random(SEED));
        return batch;
    }

    /** SHA-256, em hexadecimal minúsculo, dos SKUs em ordem alfabética, unidos por \n (sem \n no fim), em UTF-8. */
    static String checksum(List<String> found) {
        List<String> sorted = new ArrayList<>(found);
        Collections.sort(sorted);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.join("\n", sorted).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
