package com.eystar.alarm.flatMap;


import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public class RuleMessageProcessFunction extends KeyedBroadcastProcessFunction<String, JSONObject, String, JSONObject> {

    BroadcastState<String, KieSession> broadcastState;
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
    }

    @Override
    public void processElement(JSONObject value, ReadOnlyContext ctx, Collector<JSONObject> out) throws Exception {
        Iterator<Map.Entry<String, KieSession>> rulesIterator = broadcastState.iterator();
        ArrayList<Map.Entry<String, KieSession>> rules = Lists.newArrayList(rulesIterator);
        rules.forEach(new Consumer<Map.Entry<String, KieSession>>() {
            public void accept(Map.Entry<String, KieSession> entry) {

                KieSession kieSession = entry.getValue();
                kieSession.insert(value);
                kieSession.fireAllRules();
                if (value.containsKey("alarm_info_detail")) {
                    out.collect(value);
                }
            }
        });
    }

    public void processBroadcastElement(String value, Context ctx, Collector<JSONObject> out) throws Exception {
//        broadcastState = ctx.getBroadcastState(stateDescriptor);
        String ruleName = "rule1";
        String intent =value;
        broadcastState.put(ruleName, new KieHelper().addContent(intent, ResourceType.DRL).build().newKieSession());
    }
}

