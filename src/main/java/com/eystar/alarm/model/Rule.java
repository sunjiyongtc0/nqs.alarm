package com.eystar.alarm.model;

import java.io.Serializable;

public class Rule implements Serializable {
    private String drlStr;
    private int operate;
    private String name;

    public String toString() {
        return "Rule{" +
                "drlStr='" + drlStr + '\'' +
                ", operate=" + operate +
                ", name='" + name + '\'' +
                '}';
    }

    public String getDrlStr() {
        return drlStr;
    }

    public void setDrlStr(String drlStr) {
        this.drlStr = drlStr;
    }

    public int getOperate() {
        return operate;
    }

    public void setOperate(int operate) {
        this.operate = operate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
