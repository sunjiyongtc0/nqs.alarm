package com.eystar.alarm.startup;

import com.alibaba.fastjson.JSONObject;
import com.eystar.alarm.cfg.BaseConfig;
import com.eystar.alarm.flatMap.RuleMessageFunction;
import com.eystar.alarm.model.Rule;
import com.eystar.alarm.source.KafkaSource;
import com.eystar.alarm.source.RuleSource;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;


public class AlarmStartMain {

    //TODO 存在Provided byte array can not be empty怀疑JOB更新文件与扫描文件起冲突

    public static void main(String[] args)  throws Exception {
        // 初始化插件
        BaseConfig.init();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        DataStream<JSONObject> dataMessageStream= KafkaSource.getStream(env);
        DataStream<Rule> ruleStream = env.addSource(new RuleSource());

        DataStream<JSONObject> da = dataMessageStream.connect(ruleStream).flatMap(new RuleMessageFunction());


        DataStream<JSONObject> daAlarm = da.filter(new FilterFunction<JSONObject>() {
            public boolean filter(JSONObject message) throws Exception {
                return message.containsKey("alarm_info_detail");
            }
        });

        daAlarm.print();

        env.execute("FlinkConsoleDataMain");
    }

}
