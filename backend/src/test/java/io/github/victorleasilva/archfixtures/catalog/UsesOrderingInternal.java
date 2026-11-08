package io.github.victorleasilva.archfixtures.catalog;

import io.github.victorleasilva.archfixtures.ordering.internal.Hidden;

/** Violação proposital: o catálogo usa uma classe interna de outro módulo (AC-RNF-004.3). */
public class UsesOrderingInternal {
    public void call(Hidden hidden) {
        hidden.run();
    }
}
