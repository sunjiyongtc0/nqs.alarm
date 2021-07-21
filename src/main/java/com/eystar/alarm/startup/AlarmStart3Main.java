package com.eystar.alarm.startup;

import com.alibaba.fastjson.JSONObject;
import com.eystar.alarm.cfg.BaseConfig;
import com.eystar.alarm.model.Rule;
import com.eystar.alarm.source.KafkaSource;
import com.eystar.alarm.source.RuleSource;
import com.google.common.collect.Lists;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;


public class AlarmStart3Main {

    //TODO 存在Provided byte array can not be empty怀疑JOB更新文件与扫描文件起冲突

    public static void main(String[] args)  throws Exception {
        // 初始化插件
        BaseConfig.init();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        DataStream<JSONObject> dataMessageStream= KafkaSource.getStream(env);
        DataStream<Rule> ruleStream = env.addSource(new RuleSource());

        MapStateDescriptor<String, KieSession> stateDescriptor = new MapStateDescriptor<String, KieSession>("ruleState", String.class, KieSession.class);

        BroadcastStream<Rule> broadcastStream = ruleStream.broadcast(stateDescriptor);

        DataStream<JSONObject> da = dataMessageStream.connect(broadcastStream).process(new BroadcastProcessFunction<JSONObject, Rule, JSONObject>(){
            private BroadcastState<String, KieSession> broadcastState;

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

            public void processBroadcastElement(Rule value, Context ctx, Collector<JSONObject> out) throws Exception {
                broadcastState = ctx.getBroadcastState(stateDescriptor);
                broadcastState.put(value.getName(), new KieHelper().addContent(value.getDrlStr(), ResourceType.DRL).build().newKieSession());
            }
        });

        DataStream<JSONObject> daAlarm = da.filter(new FilterFunction<JSONObject>() {
            public boolean filter(JSONObject message) throws Exception {
                return message.containsKey("alarm_info_detail");
            }
        });

        daAlarm.print();

        env.execute("FlinkConsoleDataMain");
    }

}
