package com.eystar.alarm.flatMap;

import com.eystar.alarm.model.AlarmMeta;
import org.apache.commons.collections.IteratorUtils;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;

import java.util.Arrays;
import java.util.List;

public class FullWindowAggregationToALL implements WindowFunction<AlarmMeta, String, Tuple, GlobalWindow> {

    private  int index=1;

    public FullWindowAggregationToALL(int i) {
        this.index=i;
    }

    @Override
    public void apply(Tuple tuple, GlobalWindow window, Iterable<AlarmMeta> input, Collector<String> out) throws Exception {
            List<AlarmMeta> l= IteratorUtils.toList(input.iterator());
            String[] s=new String[index];
            String alarmId="";
            if(l.size()>=index){
                alarmId=l.get(0).getAlarm_id();
                for(int i=0;i<index;i++){
                    s[i]=l.get(i).getTask_id();
                }
                out.collect("总数为："+s.length+"task_id为==》"+ Arrays.toString(s)+"alarm_id为==》"+alarmId);
            }else{
                out.collect("未达到告警数据不报警~~~~");
            }


            }
}
