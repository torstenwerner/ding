package org.ding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.util.Collections.synchronizedList;

public enum DingManager {
    dingManager;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // might be concurrently accessed
    private List<Object> beanList = synchronizedList(new ArrayList<>());

    // both collections must always be protected by the lock
    private List<Supplier<?>> supplierList = new ArrayList<>();
    private Map<DingName, DingMetadata<?>> singletonMap = new HashMap<>();

    private Lock lock = new ReentrantLock();

    /**
     * deletes all beans which is done during testing
     */
    public void deleteAllBeans() {
        lock.lock();
        try {
            beanList.clear();
            supplierList.clear();
            singletonMap.clear();
            logger.info(() -> "delete all beans");
        } finally {
            lock.unlock();
        }
    }

    /**
     * adds or replaces a bean
     *
     * @param dingName  unique name of the bean including namespace
     * @param supplier  supplier that creates a new object of type BeanType
     * @param beanClass type or some base type of the bean object which is used for error checking
     */
    public <BeanType> void addBean(DingName dingName, Supplier<BeanType> supplier, Class<? extends BeanType> beanClass) {
        lock.lock();
        try {
            int index;
            if (singletonMap.containsKey(dingName)) {
                final Class<?> oldBeanClass = singletonMap.get(dingName).getBeanClass();
                if (!oldBeanClass.isAssignableFrom(beanClass)) {
                    final String message = format("incompatible classes for bean %s, old: %s, new: %s", dingName,
                            oldBeanClass, beanClass);
                    throw new RuntimeException(message);
                }
                index = singletonMap.get(dingName).getIndex();
                beanList.set(index, null);
                supplierList.set(index, supplier);
                logger.info(() -> format("replace bean %s of type %s with type %s", dingName, oldBeanClass, beanClass));
            } else {
                index = beanList.size();
                beanList.add(null);
                supplierList.add(supplier);
                logger.fine(() -> format("add bean %s of type %s", dingName, beanClass));
            }
            singletonMap.put(dingName, new DingMetadata<>(index, beanClass));
        } finally {
            lock.unlock();
        }
    }

    /**
     * Same as @addBean but without namespace.
     *
     * @param beanName bean name without namespace
     */
    public <BeanType> void addBean(String beanName, Supplier<BeanType> supplier, Class<? extends BeanType> beanClass) {
        addBean(DingName.dingName(beanName), supplier, beanClass);
    }

    // The default execution path is not protected by the lock for performance reasons. This method should be really
    // fast for the default execution path.
    private <BeanType> BeanType getBean(int index, DingName dingName, Class<? extends BeanType> beanClass) {
        // This line is potentially accessed concurrently. It is protected by the synchronizedList.
        BeanType bean = (BeanType) beanList.get(index);

        if (bean == null) {
            lock.lock();
            try {
                bean = (BeanType) beanList.get(index);
                if (bean == null) {
                    final Supplier<BeanType> supplier = (Supplier<BeanType>) supplierList.get(index);
                    bean = supplier.get();
                    if (!beanClass.isAssignableFrom(bean.getClass())) {
                        final String message = format("incompatible class for bean %s, bean class is %s but got %s",
                                dingName, bean.getClass(), beanClass);
                        throw new RuntimeException(message);
                    }
                    beanList.set(index, bean);
                    logger.finer(() -> format("created new bean %s of type %s", dingName, beanClass));
                }
            } finally {
                lock.unlock();
            }
        } else {
            logger.finest(() -> format("found existing bean %s of type %s", dingName, beanClass));
        }
        return bean;
    }

    /**
     * fetches a supplier for the bean
     *
     * @param dingName  unique name of the bean
     * @param beanClass type or a subtype of the registered bean
     * @return a supplier that can be used to fetch the actual bean
     */
    public <BeanType> Supplier<BeanType> getBean(DingName dingName, Class<? extends BeanType> beanClass) {
        lock.lock();
        try {
            if (!singletonMap.containsKey(dingName)) {
                throw new RuntimeException("bean does not exist");
            }
            final DingMetadata<?> metadata = singletonMap.get(dingName);
            if (!beanClass.isAssignableFrom(metadata.getBeanClass())) {
                final String message = format("incompatible class for bean %s, bean class is %s but got %s", dingName,
                        metadata.getBeanClass(), beanClass);
                throw new RuntimeException(message);
            }
            logger.finer(() -> format("created wrapper for bean %s of type %s", dingName, beanClass));
            return () -> getBean(metadata.getIndex(), dingName, beanClass);
        } finally {
            lock.unlock();
        }
    }

    /**
     * same as @getBean but without namespace
     *
     * @param beanName bean name without namespace
     */
    public <BeanType> Supplier<BeanType> getBean(String beanName, Class<? extends BeanType> beanClass) {
        return getBean(DingName.dingName(beanName), beanClass);
    }
}
