package com.eystar.alarm.startup;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.eystar.alarm.cfg.BaseConfig;
import com.eystar.alarm.flatMap.FullWindowAggregationCycle;
import com.eystar.alarm.flatMap.FullWindowAggregationStandard;
import com.eystar.alarm.flatMap.FullWindowAggregationToALL;
import com.eystar.alarm.model.AlarmMeta;
import com.eystar.alarm.model.Rule;
import com.eystar.alarm.sinkData.TestSink;
import com.eystar.alarm.source.RuleSource;
import com.eystar.alarm.source.TestKafkaSource;
import com.eystar.alarm.window.DynamicSlidingEventTimeWindows;
import com.google.common.collect.Lists;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows;
import org.apache.flink.streaming.api.windowing.evictors.CountEvictor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.triggers.CountTrigger;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.util.*;


public class AlarmStartAllDynamicMain {

    //TODO
    /**
     * 1.????????????????????????  (??????????????????sink??????)
     * 2.?????????????????????????????????job??????????????? ???lock???
     * 3.???????????????????????????????????? ??????????????? 5.10.30 min   ???????????????????????? 1.3.5???
     * 4.????????????????????????/??????  (??????????????????????????????????????????)
     * 5.??????????????????????????????????????? AlarmMeta
     * 6.AlarmMeta?????????????????????????????????????????????????????????   ????????????  ???kafka?????????mysql?????????
     * 7.??????????????????alarm_id????????????????????????????????????task_id??????
     * */




    public static void main(String[] args)  throws Exception {

        // ???????????????
        BaseConfig.init();

        //??????flink??????
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        //????????????
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        //?????????
        DataStream<JSONObject> dataMessageStream = env.addSource(new TestKafkaSource());
        DataStream<Rule> ruleStream = env.addSource(new RuleSource());

        //???????????????
        MapStateDescriptor<String, KieSession> stateDescriptor = new MapStateDescriptor<String, KieSession>("ruleState", String.class, KieSession.class);
        BroadcastStream<Rule> broadcastStream = ruleStream.broadcast(stateDescriptor);

        //?????????????????????
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

        //???????????????????????????
        DataStream<JSONObject> daAlarm = da.filter(new FilterFunction<JSONObject>() {
            public boolean filter(JSONObject message) throws Exception {
                return message.containsKey("alarm_info_detail");
            }
        });

        //??????????????????,??????????????????
        DataStream<AlarmMeta> AlarmList = daAlarm.flatMap(new FlatMapFunction<JSONObject, AlarmMeta>() {
                public void flatMap(JSONObject msg, Collector<AlarmMeta> out) throws Exception {
                    AlarmMeta am=new AlarmMeta();
                    am.setAlarm_id(msg.getString("alarm_id"));
                    am.setAlarm_info_detail(msg.getString("alarm_info_detail"));
                    am.setTask_id(msg.getString("id"));
                    am.setTest_time(msg.getLong("test_time"));
                    am.setInterval(10);
                    if(msg.getLong("id")%2==0){
                        am.setType("time");
                    }else{
                        am.setType("count");
                    }

                    am.setSize(5l);
                    am.setSlide(3l);
                    Random rm = new Random();
                    am.setIndex(rm.nextDouble());
                    out.collect(am);
                }
            }
        ).assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<AlarmMeta>(Time.milliseconds(1)) {
                    @Override
                    public long extractTimestamp(AlarmMeta element) {
                        return element.getTest_time();
                    }
                }
        );

        AlarmList.print();
        //??????????????????tag
        OutputTag<AlarmMeta> timeOutputTag = new OutputTag<AlarmMeta>("time") {};
        OutputTag<AlarmMeta> countOutputTag = new OutputTag<AlarmMeta>("count") {};


        //flink???????????????
        SingleOutputStreamOperator<AlarmMeta> dataStreamSide = AlarmList.process(new ProcessFunction<AlarmMeta, AlarmMeta>() {

            @Override
            public void processElement(AlarmMeta alarmMeta, Context ctx, Collector<AlarmMeta> out) throws Exception {
                if(StrUtil.equals(alarmMeta.getType(),"time")) {
                    ctx.output(timeOutputTag,alarmMeta);
                }
                if(StrUtil.equals(alarmMeta.getType(),"count")) {
                    ctx.output(countOutputTag,alarmMeta);
                }else {
                    alarmMeta.setType("unknow");
                }
                out.collect(alarmMeta);
            }
        });

        //???????????????
        DataStream<AlarmMeta> timeStream= dataStreamSide.getSideOutput(timeOutputTag);
        DataStream<AlarmMeta> countStream = dataStreamSide.getSideOutput(countOutputTag);

        DataStream<String> twoTime = timeStream.keyBy("alarm_id").timeWindow(Time.seconds(30), Time.seconds(1)).apply(new FullWindowAggregationCycle());
//        twoTime.print("two");
//        timeStream.keyBy("alarm_id").timeWindow(Time.minutes(5),Time.minutes(1));
//
//        timeStream.keyBy("alarm_id").timeWindow(Time.minutes(10),Time.minutes(1));




        //????????????????????????????????????????????????
        DataStream<String> oneCount =countStream.keyBy("alarm_id").countWindow(1).apply(new FullWindowAggregationStandard(1));
//        oneCount.print("one");

        DataStream<String> threeCount =countStream.keyBy("alarm_id").countWindow(5).apply(new FullWindowAggregationStandard(3));
//        threeCount.print("three");
        //??????????????????????????????(???????????????).????????????????????????????????????
        DataStream<String> fiveCount =countStream.keyBy("alarm_id").countWindow(5,1).apply(new FullWindowAggregationStandard(5));
//        fiveCount.print("five");



        // TODO ????????????????????????
        //?????????????????? ??????????????????????????????  A1-----------------------------------------------------------------------------------
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
//                        return "????????????"+accumulator.f1+"??????????????????"+accumulator.f0/accumulator.f1;
//                    }
//
//                    @Override
//                    public Tuple2<Double, Long> merge(Tuple2<Double, Long> a, Tuple2<Double, Long> b) {
//                        return new Tuple2<Double, Long>(a.f0+b.f0,a.f1+b.f1);
//                    }
//                });
//        countWindow.print();

        //?????????????????? ?????????5s ????????????  A2------------------------------------------------------------------------------------
        AlarmMeta am=new AlarmMeta();
        am.setSize(5l);am.setSlide(3l);
        DataStream<String> timeWindow=  AlarmList
                .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<AlarmMeta>(Time.milliseconds(1)) {
                    @Override
                    public long extractTimestamp(AlarmMeta element) {
                        return element.getTest_time();
                    }
                }).keyBy("alarm_id")
                .window(DynamicSlidingEventTimeWindows.of(am))
//                .timeWindow(Time.seconds(5), Time.seconds(3))
                .aggregate(new AggregateFunction<AlarmMeta, String, String>() {
                    //???????????????
                    public String createAccumulator() {
                        return "???????????????";
                    }
                    //????????????
                    public String add(AlarmMeta value, String accumulator) {
                        return accumulator+";"+value.getAlarm_info_detail();
                    }
                    //????????????
                    public String getResult(String accumulator) {
                        return accumulator;
                    }
                    //????????????
                    public String merge(String a, String b) {
                        return a+";"+b;
                    }
                });
        timeWindow.addSink(new TestSink());
        // TODO ???????????????????????????????????????

//        //???????????????   A3----------------------------------------------------------------------------------------------------
//        DataStream<String> countWindow=  AlarmList.keyBy("alarm_id")
//                . window(GlobalWindows.create())
//                        .evictor(CountEvictor.of(3))
//                        .trigger(CountTrigger.of(1))
//                .apply(new FullWindowAggregationToALL());
//
//        countWindow.addSink(new TestSink());


//---------------------------????????????------------------------------------------------


        //????????????flink??????
        env.execute("FlinkConsoleDataMain");
    }

}
