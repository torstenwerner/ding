package org.ding.test;

import java.util.function.Supplier;

import static org.ding.DingManager.dingManager;

public class SecondBean {
    private final Supplier<FirstBean> firstBean = dingManager.getBean("firstBean", FirstBean.class);

    public String getName() {
        return "Ernie";
    }

    public FirstBean getFirstBean() {
        return firstBean.get();
    }
}
