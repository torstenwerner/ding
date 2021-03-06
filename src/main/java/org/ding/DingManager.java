package org.ding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static org.ding.DingName.dingName;
import static org.ding.DingScope.SCOPE_SINGLETON;
import static org.ding.DingScope.SCOPE_THREAD;

public enum DingManager {
    dingManager;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // might be concurrently accessed but should be optimized for get()
    private List<Object> singletonBeanList = new CopyOnWriteArrayList<>();

    // must always be protected by the lock
    private Map<DingName, DingMetadata<?>> metadataMap = new HashMap<>();

    // is accessed by the current thread only and need no protection
    private ThreadLocal<ArrayList<Object>> threadBeanList = ThreadLocal.withInitial(() -> new ArrayList<>());

    private Lock lock = new ReentrantLock();

    /**
     * deletes all beans which is done during testing
     */
    public void deleteAllBeans() {
        lock.lock();
        try {
            singletonBeanList.clear();
            metadataMap.clear();
            logger.info(() -> "delete all beans");
        } finally {
            lock.unlock();
        }
    }

    public void initializeSingletons() {
        lock.lock();
        try {
            metadataMap.entrySet().stream()
                    .map(Map.Entry::getKey)
                    .forEach(key -> getSingletonBean(metadataMap.get(key).getIndex(), key));
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
    public <BeanType> void addSingletonBean(DingName dingName, Supplier<BeanType> supplier,
                                            Class<? extends BeanType> beanClass,
                                            DingDependency<BeanType, ? extends Object>... dependencies) {
        lock.lock();
        try {
            if (metadataMap.containsKey(dingName)) {
                final Class<?> oldBeanClass = metadataMap.get(dingName).getBeanClass();
                if (!oldBeanClass.isAssignableFrom(beanClass)) {
                    final String message = format("incompatible classes for bean %s, old: %s, new: %s", dingName,
                            oldBeanClass, beanClass);
                    throw new RuntimeException(message);
                }
                final int index = metadataMap.get(dingName).getIndex();
                singletonBeanList.set(index, null);
                logger.info(() -> format("replace bean %s of type %s with type %s", dingName, oldBeanClass, beanClass));

                resetParentsByDependencyName(dingName);
                metadataMap.put(dingName, new DingMetadata<>(dingName, index, supplier, beanClass, SCOPE_SINGLETON, dependencies));

            } else {
                final int index = singletonBeanList.size();
                singletonBeanList.add(null);
                logger.fine(() -> format("add bean %s of type %s", dingName, beanClass));
                metadataMap.put(dingName, new DingMetadata<>(dingName, index, supplier, beanClass, SCOPE_SINGLETON, dependencies));
            }
        } finally {
            lock.unlock();
        }
    }

    private void resetParentsByDependencyName(DingName dependencyName) {
        metadataMap.entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(parent -> parent.getScope().equals(SCOPE_SINGLETON))
                .forEach(parent -> resetParent(dependencyName, parent));
    }

    private void resetParent(DingName dependencyName, DingMetadata<?> parent) {
        parent.getDependencies().stream()
                .filter(dependency -> dependency.getName().equals(dependencyName))
                .findAny()
                .ifPresent(dependency -> {
                    singletonBeanList.set(parent.getIndex(), null);
                });
    }

    public <BeanType> void addThreadBean(DingName dingName, Supplier<BeanType> supplier,
                                         Class<? extends BeanType> beanClass,
                                         DingDependency<BeanType, ? extends Object>... dependencies) {
        lock.lock();
        try {
            if (metadataMap.containsKey(dingName)) {
                final Class<?> oldBeanClass = metadataMap.get(dingName).getBeanClass();
                if (!oldBeanClass.isAssignableFrom(beanClass)) {
                    final String message = format("incompatible classes for bean %s, old: %s, new: %s", dingName,
                            oldBeanClass, beanClass);
                    throw new RuntimeException(message);
                }
                final int index = metadataMap.get(dingName).getIndex();
                singletonBeanList.set(index, null);
                logger.info(() -> format("replace bean %s of type %s with type %s", dingName, oldBeanClass, beanClass));
                metadataMap.put(dingName, new DingMetadata<>(dingName, index, supplier, beanClass, SCOPE_THREAD, dependencies));
            } else {
                final int index = singletonBeanList.size();
                metadataMap.put(dingName, new DingMetadata<>(dingName, index, supplier, beanClass, SCOPE_THREAD, dependencies));
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Same as @addBean but without namespace.
     *
     * @param beanName bean name without namespace
     */
    public <BeanType> void addSingletonBean(String beanName, Supplier<BeanType> supplier,
                                            Class<? extends BeanType> beanClass,
                                            DingDependency<BeanType, ? extends Object>... dependencies) {
        addSingletonBean(dingName(beanName), supplier, beanClass, dependencies);
    }

    public <BeanType> void addThreadBean(String beanName, Supplier<BeanType> supplier,
                                         Class<? extends BeanType> beanClass,
                                         DingDependency<BeanType, ? extends Object>... dependencies) {
        addThreadBean(dingName(beanName), supplier, beanClass, dependencies);
    }

    // The default execution path is not protected by the lock for performance reasons. This method should be really
    // fast for the default execution path.
    private <BeanType> BeanType getSingletonBean(int index, DingName dingName) {
        // This line is potentially accessed concurrently. It is protected by the synchronizedList.
        BeanType bean = (BeanType) singletonBeanList.get(index);

        if (bean == null) {
            lock.lock();
            try {
                bean = (BeanType) singletonBeanList.get(index);
                if (bean == null) {
                    final DingMetadata<BeanType> metadata = (DingMetadata<BeanType>) metadataMap.get(dingName);
                    final BeanType newBean = metadata.getSupplier().get();
                    bean = newBean;
                    if (!metadata.getBeanClass().isAssignableFrom(bean.getClass())) {
                        final String message = format("incompatible class for bean %s, bean class is %s but got %s",
                                metadata.getName(), bean.getClass(), metadata.getBeanClass());
                        throw new RuntimeException(message);
                    }
                    singletonBeanList.set(index, bean);
                    logger.finer(() -> format("created new bean %s of type %s", metadata.getName(),
                            metadata.getBeanClass()));
                    metadata.getDependencies().stream()
                            .forEach(dependency -> {
                                final DingMetadata depMetadata = metadataMap.get(dependency.getName());
                                if (depMetadata.getScope().equals(SCOPE_THREAD)) {
                                    final String message = format("singleton bean %s depends on thread bean %s",
                                            metadata.getName(), dependency.getName());
                                    throw new RuntimeException(message);
                                }
                                final Object dependencyBean = getBean(depMetadata);
                                dependency.getConsumer().accept(newBean, dependencyBean);
                            });
                }
            } finally {
                lock.unlock();
            }
        }
        return bean;
    }

    private <BeanType> BeanType getThreadBean(int index, DingName dingName) {
        final ArrayList<Object> beanList = threadBeanList.get();
        IntStream.range(beanList.size(), index + 1).forEach(i -> beanList.add(null));
        BeanType bean = (BeanType) beanList.get(index);
        if (bean == null) {
            lock.lock();
            try {
                final DingMetadata<BeanType> metadata = (DingMetadata<BeanType>) metadataMap.get(dingName);
                final BeanType newBean = metadata.getSupplier().get();
                bean = newBean;
                if (!metadata.getBeanClass().isAssignableFrom(bean.getClass())) {
                    final String message = format("incompatible class for bean %s, bean class is %s but got %s",
                            metadata.getName(), bean.getClass(), metadata.getBeanClass());
                    throw new RuntimeException(message);
                }
                beanList.set(index, bean);
                logger.finer(() -> format("created new bean %s of type %s", metadata.getName(), metadata.getBeanClass()));
                metadata.getDependencies().stream()
                        .forEach(dependency -> {
                            final Object dependencyBean = getBean(metadataMap.get(dependency.getName()));
                            dependency.getConsumer().accept(newBean, dependencyBean);
                        });
            } finally {
                lock.unlock();
            }
        }
        return bean;
    }

    private <BeanType> BeanType getBean(DingMetadata<BeanType> metadata) {
        switch (metadata.getScope()) {
            case SCOPE_SINGLETON:
                return getSingletonBean(metadata.getIndex(), metadata.getName());
            case SCOPE_THREAD:
                return getThreadBean(metadata.getIndex(), metadata.getName());
            default:
                throw new RuntimeException(format("scope %s not supported", metadata.getScope()));
        }
    }

    private <BeanType> Supplier<BeanType> getBeanSupplier(DingMetadata<BeanType> metadata) {
        lock.lock();
        try {
            logger.finer(() -> format("created wrapper for bean %s of type %s", metadata.getName(),
                    metadata.getBeanClass()));
            switch (metadata.getScope()) {
                case SCOPE_SINGLETON:
                    return () -> getSingletonBean(metadata.getIndex(), metadata.getName());
                case SCOPE_THREAD:
                    return () -> getThreadBean(metadata.getIndex(), metadata.getName());
                default:
                    throw new RuntimeException(format("scope %s not supported", metadata.getScope()));
            }
        } finally {
            lock.unlock();
        }
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
            if (!metadataMap.containsKey(dingName)) {
                throw new RuntimeException(format("bean %s does not exist", dingName));
            }
            final DingMetadata<BeanType> metadata = (DingMetadata<BeanType>) metadataMap.get(dingName);
            if (!beanClass.isAssignableFrom(metadata.getBeanClass())) {
                final String message = format("incompatible class for bean %s, bean class is %s but got %s", dingName,
                        metadata.getBeanClass(), beanClass);
                throw new RuntimeException(message);
            }
            return getBeanSupplier(metadata);
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
        return getBean(dingName(beanName), beanClass);
    }
}
