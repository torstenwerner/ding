package org.ding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Collections.synchronizedList;

public enum DingManager {
    dingManager;

    // might be concurrently accessed
    private List<Object> beanList = synchronizedList(new ArrayList<>());

    // both collections must always be protected by the lock
    private List<Supplier<?>> supplierList = new ArrayList<>();
    private Map<String, DingMetadata<?>> singletonMap = new HashMap<>();

    private Lock lock = new ReentrantLock();

    public <BeanType> void addBean(String beanName, Supplier<BeanType> supplier, Class<? extends BeanType> beanClass) {
        lock.lock();
        try {
            int index;
            if (singletonMap.containsKey(beanName)) {
                if (!singletonMap.get(beanName).getBeanClass().isAssignableFrom(beanClass)) {
                    final String message = format("incompatible classes, old: %s, new: %s",
                            singletonMap.get(beanName).getBeanClass(), beanClass);
                    throw new RuntimeException(message);
                }
                index = singletonMap.get(beanName).getIndex();
                beanList.set(index, null);
                supplierList.set(index, supplier);
            } else {
                index = beanList.size();
                beanList.add(null);
                supplierList.add(supplier);
            }
            singletonMap.put(beanName, new DingMetadata<>(index, beanClass));
        } finally {
            lock.unlock();
        }
    }

    // The default execution path is not protected by the lock for performance reasons.
    private <BeanType> BeanType getBean(int index, Class<? extends BeanType> beanClass) {
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
                        final String message = format("incompatible class, bean class is %s but got %s", bean.getClass(),
                                beanClass);
                        throw new RuntimeException(message);
                    }
                    beanList.set(index, bean);
                }
            } finally {
                lock.unlock();
            }
        }
        return bean;
    }

    public <BeanType> Supplier<BeanType> getBean(String beanName, Class<? extends BeanType> beanClass) {
        lock.lock();
        try {
            if (!singletonMap.containsKey(beanName)) {
                throw new RuntimeException("bean does not exist");
            }
            final DingMetadata<?> metadata = singletonMap.get(beanName);
            if (!beanClass.isAssignableFrom(metadata.getBeanClass())) {
                final String message = format("incompatible class, bean class is %s but got %s", metadata.getBeanClass(),
                        beanClass);
                throw new RuntimeException(message);
            }
            return () -> getBean(metadata.getIndex(), beanClass);
        } finally {
            lock.unlock();
        }
    }
}
