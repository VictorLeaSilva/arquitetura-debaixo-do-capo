package io.github.victorleasilva.archfixtures.ordering.experiment;

import io.github.victorleasilva.archfixtures.inventory.experiment.RawThing;

/** Exemplo correto: um pacote experiment usa o experiment de outro módulo (AC-RNF-004.2, AC-RNF-004.3). */
public class UsesInventoryExperiment {
    public void call(RawThing thing) {
        thing.run();
    }
}
