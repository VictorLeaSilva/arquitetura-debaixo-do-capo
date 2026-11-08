package io.github.victorleasilva.archfixtures.catalog;

import io.github.victorleasilva.archfixtures.ordering.api.Facade;

/** Exemplo correto: o catálogo usa só a API pública de outro módulo (AC-RNF-004.3). */
public class UsesOrderingApi {
    public void call(Facade facade) {
        facade.run();
    }
}
