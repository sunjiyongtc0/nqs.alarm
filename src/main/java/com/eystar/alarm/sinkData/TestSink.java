package com.eystar.alarm.sinkData;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;


public class TestSink extends RichSinkFunction<String>  {

    public void open(Configuration parameters) throws Exception {
    }

    public void invoke(String message, Context context) throws Exception {

        System.out.println("这是sink ==>暂不保存只打印=>message"+message);

    }


    @Override
    public void close() throws Exception {

    }

}
