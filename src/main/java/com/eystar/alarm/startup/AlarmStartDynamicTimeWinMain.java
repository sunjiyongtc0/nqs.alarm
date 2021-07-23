package com.eystar.alarm.startup;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.eystar.alarm.cfg.BaseConfig;
import com.eystar.alarm.flatMap.CustomWindowAssigner;
import com.eystar.alarm.flatMap.FullWindowAggregationCycle;
import com.eystar.alarm.flatMap.FullWindowAggregationStandard;
import com.eystar.alarm.model.AlarmMeta;
import com.eystar.alarm.model.Rule;
import com.eystar.alarm.sinkData.TestSink;
import com.eystar.alarm.source.RuleSource;
import com.eystar.alarm.source.TestKafkaSource;
import com.google.common.collect.Lists;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.utils.KieHelper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;


public class AlarmStartDynamicTimeWinMain {

    //TODO
    /**
     * 1.输出结果为字符串  (改为特定格式sink入库)
     * 2.广播循环读取数据时文件job被修改问题 （lock）
     * 3.时间滚动及次数滚动有限制 （时间暂定 5.10.30 min   次数暂定连续出现 1.3.5）
     * 4.设置窗口方式滚动/滑动  (滑动显示最先还是最后几条数据)
     * 5.规划页面存储数据格式。匹配 AlarmMeta
     * 6.AlarmMeta字段间隔问题及根据业务相关指标范围判断   读取问题  （kafka存储还mysql读取）
     * 7.测试用暂时用alarm_id作为窗口分组。实际应该用task_id分组
     * */

    public static void main(String[] args)  throws Exception {

        // 初始化插件
        BaseConfig.init();

        //配置flink环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(2);

        //事件时间
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        //引入源
        DataStream<JSONObject> dataMessageStream = env.addSource(new TestKafkaSource());
        DataStream<Rule> ruleStream = env.addSource(new RuleSource());

        //广播信息流
        MapStateDescriptor<String, KieSession> stateDescriptor = new MapStateDescriptor<String, KieSession>("ruleState", String.class, KieSession.class);
        BroadcastStream<Rule> broadcastStream = ruleStream.broadcast(stateDescriptor);

        //合并进程流处理
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

        //去除非告警内容数据
        DataStream<JSONObject> daAlarm = da.filter(new FilterFunction<JSONObject>() {
            public boolean filter(JSONObject message) throws Exception {
                return message.containsKey("alarm_info_detail");
            }
        });

        //修改告警规格,配置时间参数
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

        //数据分流定义tag
        OutputTag<AlarmMeta> timeOutputTag = new OutputTag<AlarmMeta>("time") {};

        //flink数据流拆分
        SingleOutputStreamOperator<AlarmMeta> dataStreamSide = AlarmList.process(new ProcessFunction<AlarmMeta, AlarmMeta>() {

            @Override
            public void processElement(AlarmMeta alarmMeta, Context ctx, Collector<AlarmMeta> out) throws Exception {
                if(StrUtil.equals(alarmMeta.getType(),"time")) {
                    ctx.output(timeOutputTag,alarmMeta);
                }
                out.collect(alarmMeta);
            }
        });

        //拆分好的流
        DataStream<AlarmMeta> timeStream= dataStreamSide.getSideOutput(timeOutputTag);
        DataStream<String> timeWindow = timeStream.keyBy("alarm_id").window(new CustomWindowAssigner())
                .apply(new FullWindowAggregationCycle());
        timeWindow.addSink(new TestSink());


        //开始执行flink任务
        env.execute("FlinkConsoleDataMain");
    }

}
