package io.github.victorleasilva.archfixtures.ordering.experiment;

import io.github.victorleasilva.archfixtures.inventory.internal.Internal;

/** Violação proposital: mesmo um pacote experiment não pode usar classes internas de outro módulo (AC-RNF-004.3). */
public class UsesInventoryInternal {
    public void call(Internal internal) {
        internal.run();
    }
}
