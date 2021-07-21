package com.eystar.alarm.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录规则执行轨迹的对象
 *
 */
public class RuleTreeOrbit {

    public List<String> ruleOrbit = new ArrayList<String>();

    public List<String> getRuleOrbit() {
        return ruleOrbit;
    }

    public void setRuleOrbit(List<String> ruleOrbit) {
        this.ruleOrbit = ruleOrbit;
    }


}