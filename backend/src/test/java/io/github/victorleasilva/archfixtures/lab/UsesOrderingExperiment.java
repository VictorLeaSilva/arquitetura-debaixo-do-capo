package io.github.victorleasilva.archfixtures.lab;

import io.github.victorleasilva.archfixtures.ordering.experiment.NaiveThing;

/** Exemplo correto: o laboratório pode usar as versões ruins (AC-RNF-004.2, AC-RNF-004.3). */
public class UsesOrderingExperiment {
    public void call(NaiveThing thing) {
        thing.run();
    }
}
