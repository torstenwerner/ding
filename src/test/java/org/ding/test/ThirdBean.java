package org.ding.test;

public class ThirdBean {
    private FourthBean fourthBean;

    public FourthBean getFourthBean() {
        return fourthBean;
    }

    public void setFourthBean(FourthBean fourthBean) {
        this.fourthBean = fourthBean;
    }

    public String whoami() {
        return getClass().getName();
    }
}
