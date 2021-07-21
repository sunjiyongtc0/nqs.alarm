package com.eystar.alarm.source;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xxl.conf.core.XxlConfClient;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.AssignerWithPunctuatedWatermarks;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011;
import org.apache.flink.util.Collector;

import javax.annotation.Nullable;
import java.util.Properties;

public class KafkaSource {

    public static Properties init() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", XxlConfClient.get("gw-common.kafka.servers"));
        properties.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"" + XxlConfClient.get("gw-common.kafka.username") + "\" password=\"" + XxlConfClient.get("gw-common.kafka.password") + "\";");
        properties.put("group.id", "alarm2");
        properties.put("enable.auto.commit", "true");
        properties.put("auto.commit.interval.ms", "1000");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("security.protocol", "SASL_PLAINTEXT");
        properties.put("sasl.mechanism", "SCRAM-SHA-256");
        return properties;
    }




    public static DataStream<JSONObject> getStream(StreamExecutionEnvironment env) {
        // 定义相关的配置
        Properties props = init();
        FlinkKafkaConsumer011<String> consumer = new FlinkKafkaConsumer011<String>("data_upload", new SimpleStringSchema(), props);
        DataStreamSource<String> stream = env.addSource(consumer);

        //读取kfka中数据,将其转换成JSONObject
        DataStream<JSONObject> dataMessageStream = stream.flatMap(new FlatMapFunction<String, JSONObject>() {
            private static final long serialVersionUID = -6986783354031993339L;

            public void flatMap(String msg, Collector<JSONObject> out) throws Exception {
                try {
                    if (StrUtil.isBlank(msg))
                        return;
                    JSONArray array = JSONArray.parseArray(msg);
                    for (int i = 0; i < array.size(); i++) {
                        JSONObject object = array.getJSONObject(i);
                        out.collect(object);
                    }
                } catch (Exception e) {
                }
            }
        });

        return dataMessageStream;
    }

}
