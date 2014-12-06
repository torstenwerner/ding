package org.ding;

import java.util.function.BiConsumer;

public class DingDependency<TargetType, DependencyType> {
    private final DingName name;
    private final BiConsumer<TargetType, DependencyType> consumer;
    private final Class<? extends DependencyType> beanClass;

    public DingDependency(DingName name, BiConsumer<TargetType, DependencyType> consumer, Class<? extends DependencyType> beanClass) {
        this.name = name;
        this.consumer = consumer;
        this.beanClass = beanClass;
    }

    public DingDependency(String name, BiConsumer<TargetType, DependencyType> consumer, Class<? extends DependencyType> beanClass) {
        this(DingName.dingName(name), consumer, beanClass);
    }

    public DingName getName() {
        return name;
    }

    public BiConsumer<TargetType, DependencyType> getConsumer() {
        return consumer;
    }

    public Class<? extends DependencyType> getBeanClass() {
        return beanClass;
    }
}
