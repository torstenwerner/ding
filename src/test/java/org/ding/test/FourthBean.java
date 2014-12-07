package org.ding.test;

public class FourthBean {
    private ThirdBean thirdBean;

    public ThirdBean getThirdBean() {
        return thirdBean;
    }

    public void setThirdBean(ThirdBean thirdBean) {
        this.thirdBean = thirdBean;
    }

    public String whoami() {
        return getClass().getName();
    }
}
