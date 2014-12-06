package org.ding;

import java.util.function.Supplier;

class DingMetadata<BeanType> {
    private final DingName name;
    private final Supplier<BeanType> supplier;
    private final Class<? extends BeanType> beanClass;
    private final DingScope scope;

    public DingMetadata(DingName name, Supplier<BeanType> supplier, Class<? extends BeanType> beanClass, DingScope scope) {
        this.name = name;
        this.supplier = supplier;
        this.beanClass = beanClass;
        this.scope = scope;
    }

    public DingName getName() {
        return name;
    }

    public Supplier<BeanType> getSupplier() {
        return supplier;
    }

    public Class<? extends BeanType> getBeanClass() {
        return beanClass;
    }

    public DingScope getScope() {
        return scope;
    }
}
