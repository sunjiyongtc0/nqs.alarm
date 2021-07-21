package com.eystar.alarm.startup;

import com.alibaba.fastjson.JSONObject;
import com.eystar.alarm.cfg.BaseConfig;
import com.eystar.alarm.flatMap.RuleMessageFunction;
import com.eystar.alarm.flatMap.RuleMessageProcessFunction;
import com.eystar.alarm.model.Rule;
import com.eystar.alarm.source.KafkaSource;
import com.eystar.alarm.source.RuleSource;
import com.eystar.alarm.source.RuleStringSource;
import com.google.common.collect.Lists;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.*;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;


public class AlarmStart2Main {

    //TODO 存在Provided byte array can not be empty怀疑JOB更新文件与扫描文件起冲突

    public static void main(String[] args)  throws Exception {
        // 初始化插件
        BaseConfig.init();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        DataStream<JSONObject> dataMessageStream= KafkaSource.getStream(env);
        DataStream<String> ruleStream = env.addSource(new RuleStringSource());


        KeyedStream<JSONObject, String> keyedStream = dataMessageStream.keyBy(new KeySelector<JSONObject, String>() {
            public String getKey(JSONObject value) throws Exception {
                return value.getString("task_type_name");
            }
        });

        MapStateDescriptor<String, KieSession> stateDescriptor = new MapStateDescriptor<String, KieSession>("ruleState", String.class, KieSession.class);

        BroadcastStream<String> broadcastStream = ruleStream.broadcast(stateDescriptor);

        BroadcastConnectedStream<JSONObject, String> connectedStream = keyedStream.connect(broadcastStream);

        SingleOutputStreamOperator<JSONObject> resultStream = connectedStream.process(new KeyedBroadcastProcessFunction<String, JSONObject, String, JSONObject>() {

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
                broadcastState = ctx.getBroadcastState(stateDescriptor);
                String ruleName = "rule1";
                String intent =value;
                broadcastState.put(ruleName, new KieHelper().addContent(intent, ResourceType.DRL).build().newKieSession());
            }
        });


        resultStream.print("resultStream====>");
        env.execute();
    }

}
