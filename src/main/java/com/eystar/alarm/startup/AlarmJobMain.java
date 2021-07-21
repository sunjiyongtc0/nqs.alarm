package com.eystar.alarm.startup;

import com.eystar.alarm.cfg.BaseConfig;

public class AlarmJobMain {

    public static void main(String[] args) {
        BaseConfig.init();
        BaseConfig.initJob();
    }
}
