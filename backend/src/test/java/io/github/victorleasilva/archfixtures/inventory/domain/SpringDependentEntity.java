package io.github.victorleasilva.archfixtures.inventory.domain;

import org.springframework.stereotype.Component;

/** Violação proposital: classe de domínio que depende do Spring (AC-RNF-004.1). */
@Component
public class SpringDependentEntity {}
