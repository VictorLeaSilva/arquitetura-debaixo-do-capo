package io.github.victorleasilva.capo.lab.listintersection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** As três versões encontram os mesmos SKUs no mesmo lote (AC-005.1; P3), e o lote segue A3, passo 1. */
@Tag("RF-005")
@Tag("RNF-002")
class IntersectionTest {

    private static final List<String> CATALOG = IntStream.rangeClosed(1, 10_000)
            .mapToObj(i -> String.format("SKU-%07d", i))
            .toList();

    @Test
    void allVersionsFindTheSameSkus() {
        List<String> batch = IntersectionBatch.of(CATALOG);

        List<String> bad = new LinearSearchIntersection().found(CATALOG, batch);
        List<String> medium = new BinarySearchIntersection().found(CATALOG, batch);
        List<String> good = new HashSetIntersection().found(CATALOG, batch);

        assertThat(bad).hasSize(5000);
        assertThat(medium).hasSize(5000);
        assertThat(good).hasSize(5000);
        String checksum = IntersectionBatch.checksum(bad);
        assertThat(checksum).matches("[0-9a-f]{64}");
        assertThat(IntersectionBatch.checksum(medium)).isEqualTo(checksum);
        assertThat(IntersectionBatch.checksum(good)).isEqualTo(checksum);
    }

    @Test
    void versionsReturnTheFoundSkusInBatchOrder() {
        List<String> batch = IntersectionBatch.of(CATALOG);
        List<String> expected =
                batch.stream().filter(new HashSet<>(CATALOG)::contains).toList();

        assertThat(new LinearSearchIntersection().found(CATALOG, batch)).isEqualTo(expected);
        assertThat(new BinarySearchIntersection().found(CATALOG, batch)).isEqualTo(expected);
        assertThat(new HashSetIntersection().found(CATALOG, batch)).isEqualTo(expected);
    }

    /**
     * Um catálogo fora de ordem e um SKU que fica na posição 0 da cópia ordenada: pega a {@code medium} sem o
     * {@code Collections.sort} e com {@code > 0} no lugar de {@code >= 0} (o catálogo dos outros testes já vem em ordem).
     */
    @Test
    void versionsHandleAnUnsortedCatalogAndTheFirstSortedSku() {
        List<String> catalog = List.of("SKU-c", "SKU-a", "SKU-b");
        List<String> batch = List.of("SKU-a", "MISS-1", "SKU-c", "SKU-b");

        for (Intersection version : ListIntersectionRunner.VERSIONS.values()) {
            assertThat(version.found(catalog, batch))
                    .as(version.getClass().getSimpleName())
                    .containsExactly("SKU-a", "SKU-c", "SKU-b");
        }
    }

    @Test
    void batchHasHalfExistingWithoutRepetitionAndHalfMissing() {
        List<String> batch = IntersectionBatch.of(CATALOG);

        assertThat(batch).hasSize(10_000).doesNotHaveDuplicates();
        assertThat(batch.stream().filter(CATALOG::contains)).hasSize(5000);
        List<String> missing = new ArrayList<>();
        IntStream.rangeClosed(1, 5000).forEach(i -> missing.add("MISS-" + i));
        assertThat(batch.stream().filter(sku -> sku.startsWith("MISS-"))).containsExactlyInAnyOrderElementsOf(missing);
        assertThat(batch).isNotEqualTo(batch.stream().sorted().toList());
    }

    @Test
    void theSameCatalogGivesTheSameBatch() {
        assertThat(IntersectionBatch.of(CATALOG)).isEqualTo(IntersectionBatch.of(new ArrayList<>(CATALOG)));
    }

    /** A3, passo 5: SHA-256 dos encontrados em ordem alfabética, unidos por \n, sem \n no fim. */
    @Test
    void checksumIsTheSha256OfTheSortedSkus() {
        // printf 'a\nb' | sha256sum
        assertThat(IntersectionBatch.checksum(List.of("b", "a")))
                .isEqualTo("7e18f737311b2dc3b2f269dd78396b0351f14fb66efa879f768cb23181883c78");
        // printf '' | sha256sum
        assertThat(IntersectionBatch.checksum(List.of()))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }
}
