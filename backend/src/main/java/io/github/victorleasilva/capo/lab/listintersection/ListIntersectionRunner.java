package io.github.victorleasilva.capo.lab.listintersection;

import io.github.victorleasilva.capo.catalog.api.CatalogSkus;
import io.github.victorleasilva.capo.lab.run.ExperimentRunner;
import io.github.victorleasilva.capo.lab.run.Measurement;
import io.github.victorleasilva.capo.lab.run.Stopwatch;
import io.github.victorleasilva.capo.lab.run.VariantOutcome;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * O experimento list-intersection (RF-005; A3), medido pelo método D-06. O catálogo e o lote são preparados fora da
 * medição (P3): o intervalo medido só tem o trabalho da versão, inclusive a preparação dela (a cópia ordenada da média,
 * o {@code HashSet} da boa), que faz parte da complexidade comparada.
 */
@Component
public class ListIntersectionRunner implements ExperimentRunner {

    static final Map<String, Intersection> VERSIONS = Map.of(
            "bad", new LinearSearchIntersection(),
            "medium", new BinarySearchIntersection(),
            "good", new HashSetIntersection());

    private final CatalogSkus catalogSkus;
    private final Stopwatch stopwatch;

    public ListIntersectionRunner(CatalogSkus catalogSkus, Stopwatch stopwatch) {
        this.catalogSkus = catalogSkus;
        this.stopwatch = stopwatch;
    }

    @Override
    public String experimentId() {
        return "list-intersection";
    }

    @Override
    public VariantOutcome run(String variant, Integer size, Map<String, Object> parameters) {
        List<String> catalog = catalogSkus.firstSkus(size);
        if (catalog.size() < size) {
            throw new IllegalStateException(
                    "o catálogo tem " + catalog.size() + " produtos; o tamanho pedido é " + size);
        }
        List<String> batch = IntersectionBatch.of(catalog);
        Intersection version = VERSIONS.get(variant);

        Measurement.Measured<List<String>> measured = Measurement.repeat(
                stopwatch, (Integer) parameters.get("repetitions"), () -> version.found(catalog, batch));

        Map<String, Double> measurements = new LinkedHashMap<>();
        measurements.put("durationMsMedian", measured.medianMs());
        measurements.put("durationMsMin", measured.minMs());
        measurements.put("durationMsMax", measured.maxMs());
        measurements.put("matches", (double) measured.result().size());
        return new VariantOutcome(measurements, IntersectionBatch.checksum(measured.result()));
    }
}
