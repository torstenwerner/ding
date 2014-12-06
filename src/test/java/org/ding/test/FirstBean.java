package org.ding.test;

import java.util.function.Supplier;

import static org.ding.DingManager.dingManager;

public class FirstBean {
    private final String name;

    private final Supplier<SecondBean> secondBean = dingManager.getBean("secondBean", SecondBean.class);

    public FirstBean(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public SecondBean getSecondBean() {
        return secondBean.get();
    }
}
