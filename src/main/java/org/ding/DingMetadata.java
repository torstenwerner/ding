package org.ding;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

class DingMetadata<BeanType> {
    private final DingName name;
    private final Supplier<BeanType> supplier;
    private final Class<? extends BeanType> beanClass;
    private final DingScope scope;
    private final List<DingDependency> dependencies;

    public DingMetadata(DingName name, Supplier<BeanType> supplier, Class<? extends BeanType> beanClass, DingScope scope,
                        DingDependency... dependencies) {
        this.name = name;
        this.supplier = supplier;
        this.beanClass = beanClass;
        this.scope = scope;
        this.dependencies = Arrays.asList(dependencies);
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

    public List<DingDependency> getDependencies() {
        return dependencies;
    }
}
