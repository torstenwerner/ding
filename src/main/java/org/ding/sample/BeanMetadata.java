package org.ding.sample;

public class BeanMetadata<BeanType> {
    private final int index;
    private final Class<? extends BeanType> beanClass;

    public BeanMetadata(int index, Class<? extends BeanType> beanClass) {
        this.index = index;
        this.beanClass = beanClass;
    }

    public int getIndex() {
        return index;
    }

    public Class<? extends BeanType> getBeanClass() {
        return beanClass;
    }
}
