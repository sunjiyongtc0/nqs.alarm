package com.eystar.alarm.window;

import org.apache.flink.api.dag.Transformation;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class DynamicDataStream<T> extends DataStream<T> {

    public DynamicDataStream(StreamExecutionEnvironment environment, Transformation<T> transformation) {
        super(environment, transformation);
    }
}
