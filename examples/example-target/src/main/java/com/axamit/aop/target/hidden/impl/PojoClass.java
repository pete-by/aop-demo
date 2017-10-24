package com.axamit.aop.target.hidden.impl;

public class PojoClass {

    private String title;

    private int order;

    public PojoClass(String title, int order) {
        this.title = title;
        this.order = order;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
