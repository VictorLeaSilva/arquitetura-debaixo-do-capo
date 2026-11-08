package io.github.victorleasilva.archfixtures.catalog;

import io.github.victorleasilva.archfixtures.ordering.experiment.NaiveThing;

/** Violação proposital: o catálogo usa uma versão ruim de outro módulo (AC-RNF-004.2). */
public class UsesOrderingExperiment {
    public void call(NaiveThing thing) {
        thing.run();
    }
}
