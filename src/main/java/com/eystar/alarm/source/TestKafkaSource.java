package com.eystar.alarm.source;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class TestKafkaSource implements SourceFunction<JSONObject> {
    private Boolean running = true;
    private static AtomicInteger counter = new AtomicInteger(0);

    String [] ss={
            "{\"jitter\":1.524,\"task_type_name\":\"PING\"}",
            "{\"time_cost\":1.524,\"task_type_name\":\"DNS\"}",
            "{\"dns_cost\":1.524,\"task_type_name\":\"HTTP\"}",
            "{\"jitter\":1.524,\"task_type_name\":\"TRACE\"}"
    };
    String alarmPINGId[]={"7D1L63i9","EVqmOOan","Fh2nlrYo","ycyLEZhX"};
    String alarmTRACEId[]={"LxASeYvA"};
    String alarmDNSId[]={"qOKBQ23U"};
    String alarmHTTPId[]={"YsSDAojb"};
    public void run(SourceContext<JSONObject> sourceContext) throws Exception {
        while (running){
            String s= ss[counter.getAndIncrement()%4];
            JSONObject message = JSONObject.parseObject(s);
            message.put("id",counter+"");
            if(StrUtil.equals("TRACE",message.getString("task_type_name"))){
                message.put("alarm_id",alarmTRACEId[0]);
            }
            if(StrUtil.equals("DNS",message.getString("task_type_name"))){
                message.put("alarm_id",alarmDNSId[0]);
            }
            if(StrUtil.equals("HTTP",message.getString("task_type_name"))){
                message.put("alarm_id",alarmHTTPId[0]);
            }
            if(StrUtil.equals("PING",message.getString("task_type_name"))){
                message.put("alarm_id",alarmPINGId[counter.get()%3]);
            }
            Thread.sleep(300);
            message.put("test_time",(new Date()).getTime());
            sourceContext.collect(message);
        }
    }

    public void cancel() {
        running = false;
    }
}
