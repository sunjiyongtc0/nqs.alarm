package com.eystar.alarm.flatMap;

import com.eystar.alarm.model.AlarmMeta;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

public class DataNoStandardProcessFunction  extends KeyedProcessFunction<Tuple, AlarmMeta, String> {

    // 最近一次定时器的触发时间(报警时间)
    private ValueState<Long> TimerTimeStamp;
    // 定时器的统计次数
    private ValueState<Long> TimerTimeCount;
    //定时器统计任务信息
    private ValueState<String> TimerAlarmId;
    //定时器统计次数阈值
    private ValueState<Long> TimerAlarmCount;

    //负责初始化
    @Override
    public void open(Configuration parameters) throws Exception {
        TimerTimeStamp = getRuntimeContext().getState(new ValueStateDescriptor<Long>("TimerTimeStamp", Long.class));
        TimerTimeCount = getRuntimeContext().getState(new ValueStateDescriptor<Long>("TimerTimeCount", Long.class));
        TimerAlarmId = getRuntimeContext().getState(new ValueStateDescriptor<String>("TimerAlarmId", String.class));
        TimerAlarmCount = getRuntimeContext().getState(new ValueStateDescriptor<Long>("TimerAlarmCount", Long.class));
    }

    //负责结束数据
    @Override
    public void close() throws Exception {
        TimerTimeStamp.clear();
        TimerTimeCount.clear();
        TimerAlarmId.clear();
        TimerAlarmCount.clear();
    }

    //进程逻辑
    @Override
    public void processElement(AlarmMeta am, Context ctx, Collector<String> out) throws Exception {
        // 计时器状态值(时间戳)
        Long timerTimestamp = TimerTimeStamp.value();
        Long  curTimestamp=am.getInterval()*am.getSize()*100;
        if(null == timerTimestamp||timerTimestamp.longValue()!=curTimestamp.longValue()){
            long endTimestamp = ctx.timerService().currentProcessingTime() + curTimestamp;
            System.out.println("设置告警器===》结束时间："+endTimestamp);
            ctx.timerService().registerProcessingTimeTimer(endTimestamp);
            TimerTimeStamp.update(curTimestamp);
            TimerTimeCount.update(1l);
            TimerAlarmId.update(am.getAlarm_id());
            TimerAlarmCount.update(am.getSlide());
        }else{
            System.out.println(TimerAlarmId.value()+"已经有任务了测试数+1");
            TimerTimeCount.update(TimerTimeCount.value()+1);
        }
    }

    // 到点执行操作
    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception {
        long alarmCount=TimerAlarmCount.value();
        long timeCount=TimerTimeCount.value();
        if(timeCount>=alarmCount){
            System.out.println("定时任务结束后，timeCount>=alarmCount"+timeCount+"======="+alarmCount);
            // 触发报警，并且清除 定时器状态值
            out.collect("传感器" + ctx.getCurrentKey() + "温度值连续ms上升");
        }
        TimerTimeStamp.clear();
        TimerTimeCount.clear();
        TimerAlarmId.clear();
        TimerAlarmCount.clear();
    }

}
