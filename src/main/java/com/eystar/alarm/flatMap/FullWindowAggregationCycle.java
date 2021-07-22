package com.eystar.alarm.flatMap;

import com.eystar.alarm.model.AlarmMeta;
import org.apache.commons.collections.IteratorUtils;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.util.Arrays;
import java.util.List;

/**
 * 动态时间窗口周期
 * 根据AlarmMeta中配置的
 * */
public class FullWindowAggregationCycle implements WindowFunction<AlarmMeta, String, Tuple, TimeWindow> {


    @Override
    public void apply(Tuple tuple, TimeWindow window, Iterable<AlarmMeta> input, Collector<String> out) throws Exception {
        List<AlarmMeta> l= IteratorUtils.toList(input.iterator());
        if(l.size()>0){
            String alarmId=l.get(0).getAlarm_id();
            long   slide  =l.get(0).getSlide();
            int index=Integer.parseInt(slide+"");
            String[] s=new String[index];
            if(l.size()>=index){
                int row=index;
                for(int i=l.size();row>0;i--){
                    row-=1;
                    s[row]=l.get(i-1).getTask_id();
                }
                //设置展示数据为这段时间最后几条数据
                out.collect("总数为：" + s.length + "task_id为==》" + Arrays.toString(s) + "alarm_id为==》" + alarmId);
            }
        }

    }
}
