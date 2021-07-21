package com.eystar.alarm.startup;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eystar.alarm.cfg.BaseConfig;
import com.eystar.alarm.flatMap.FullWindowAggregationToALL;
import com.eystar.alarm.model.AlarmMeta;
import com.eystar.alarm.model.Rule;
import com.eystar.alarm.sinkData.TestSink;
import com.eystar.alarm.source.KafkaSource;
import com.eystar.alarm.source.RuleSource;
import com.eystar.alarm.source.TestKafkaSource;
import com.eystar.alarm.window.DynamicSlidingEventTimeWindows;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import kafka.coordinator.transaction.TxnIdAndMarkerEntry;
import org.apache.commons.collections.IteratorUtils;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows;
import org.apache.flink.streaming.api.windowing.evictors.CountEvictor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.triggers.CountTrigger;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.IterableUtils;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.util.*;
import java.util.function.Consumer;


public class AlarmStartAllMain {

    public static void main(String[] args)  throws Exception {
        // 初始化插件
        BaseConfig.init();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        //通过kafka获取数据集
//        DataStream<JSONObject> dataMessageStream= KafkaSource.getStream(env);
        DataStream<JSONObject> dataMessageStream = env.addSource(new TestKafkaSource());
       //获取
        DataStream<Rule> ruleStream = env.addSource(new RuleSource());


        MapStateDescriptor<String, KieSession> stateDescriptor = new MapStateDescriptor<String, KieSession>("ruleState", String.class, KieSession.class);

        //广播信息流
        BroadcastStream<Rule> broadcastStream = ruleStream.broadcast(stateDescriptor);

        DataStream<JSONObject> da = dataMessageStream.connect(broadcastStream).process(new BroadcastProcessFunction<JSONObject, Rule, JSONObject>(){
            private BroadcastState<String, KieSession> broadcastState;

            public void processElement(JSONObject value, ReadOnlyContext ctx, Collector<JSONObject> out) throws Exception {
                Iterator<Map.Entry<String, KieSession>> rulesIterator = broadcastState.iterator();
                ArrayList<Map.Entry<String, KieSession>> rules = Lists.newArrayList(rulesIterator);
                for(Map.Entry<String, KieSession> rule :rules){
                    KieSession kieSession = rule.getValue();
                    kieSession.insert(value);
                    kieSession.fireAllRules();
                    if(value.containsKey("alarm_info_detail")){
                        out.collect(value);
                        break;
                    }
                }
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

        DataStream<AlarmMeta> AlarmList = daAlarm.flatMap(new FlatMapFunction<JSONObject, AlarmMeta>() {

            public void flatMap(JSONObject msg, Collector<AlarmMeta> out) throws Exception {
                AlarmMeta am=new AlarmMeta();
                am.setAlarm_id(msg.getString("alarm_id"));
                am.setAlarm_info_detail(msg.getString("alarm_info_detail"));
                am.setTask_id(msg.getString("id"));
                am.setType("time");
                am.setSize(5l);
                am.setSlide(3l);
                am.setTest_time((new Date().getTime()));
                Random rm = new Random();
                am.setIndex(rm.nextDouble());
                out.collect(am);
            }
        });

        // TODO 增量来个处理一个
        //增量个数聚合 样例取五条的平均得分  A1-----------------------------------------------------------------------------------
//        DataStream<String> countWindow=  AlarmList.keyBy("alarm_id")
//                .countWindow(5,1)
//                .aggregate(new AggregateFunction<AlarmMeta, Tuple2<Double,Long>, String>() {
//
//                    @Override
//                    public Tuple2<Double, Long> createAccumulator() {
//                        return new Tuple2<Double, Long>(0.0d,0l);
//                    }
//
//                    @Override
//                    public Tuple2<Double, Long> add(AlarmMeta value, Tuple2<Double, Long> accumulator) {
//                        return new Tuple2<Double, Long>(accumulator.f0+value.getIndex(),accumulator.f1+1);
//                    }
//
//                    @Override
//                    public String getResult(Tuple2<Double, Long> accumulator) {
//                        return "连续测试"+accumulator.f1+"条数据得分为"+accumulator.f0/accumulator.f1;
//                    }
//
//                    @Override
//                    public Tuple2<Double, Long> merge(Tuple2<Double, Long> a, Tuple2<Double, Long> b) {
//                        return new Tuple2<Double, Long>(a.f0+b.f0,a.f1+b.f1);
//                    }
//                });
//        countWindow.print();

        //增量时间聚合 样例取5s 数据汇总  A2------------------------------------------------------------------------------------
//        AlarmMeta am=new AlarmMeta();
//        am.setSize(5l);am.setSlide(3l);
//        DataStream<String> timeWindow=  AlarmList
//                .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<AlarmMeta>(Time.milliseconds(1)) {
//                    @Override
//                    public long extractTimestamp(AlarmMeta element) {
//                        return element.getTest_time();
//                    }
//                }).keyBy("alarm_id")
//                .window(DynamicSlidingEventTimeWindows.of(am))
////                .timeWindow(Time.seconds(5),Time.seconds(5))
//                .aggregate(new AggregateFunction<AlarmMeta, String, String>() {
//                    //初始化参数
//                    public String createAccumulator() {
//                        return "信息统计：";
//                    }
//                    //添加方式
//                    public String add(AlarmMeta value, String accumulator) {
//                        return accumulator+";"+value.getAlarm_info_detail();
//                    }
//                    //输出结果
//                    public String getResult(String accumulator) {
//                        return accumulator;
//                    }
//                    //合并方式
//                    public String merge(String a, String b) {
//                        return a+";"+b;
//                    }
//                });
//        timeWindow.addSink(new TestSink());
        // TODO 全窗口聚合集齐数据统一处理

//        //全窗口聚合   A3----------------------------------------------------------------------------------------------------
        DataStream<String> countWindow=  AlarmList.keyBy("alarm_id")
//                .countWindow(3,1)
                . window(GlobalWindows.create())
                        .evictor(CountEvictor.of(3))
                        .trigger(CountTrigger.of(1))
                .apply(new FullWindowAggregationToALL(3));

        countWindow.addSink(new TestSink());


//---------------------------标记分流------------------------------------------------



        env.execute("FlinkConsoleDataMain");
    }

}
