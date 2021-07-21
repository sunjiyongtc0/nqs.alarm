package com.eystar.alarm.window;

import org.apache.flink.annotation.Public;
import org.apache.flink.api.dag.Transformation;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

@Public
public class DynamicKeyedStream<T, KEY> extends DataStream<T> {
    /**
     * Create a new {@link DataStream} in the given execution environment with
     * partitioning set to forward by default.
     *
     * @param environment    The StreamExecutionEnvironment
     * @param transformation
     */
    public DynamicKeyedStream(StreamExecutionEnvironment environment, Transformation<T> transformation) {
        super(environment, transformation);
    }
}
