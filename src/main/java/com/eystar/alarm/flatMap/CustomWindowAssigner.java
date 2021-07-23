package com.eystar.alarm.flatMap;

import com.eystar.alarm.model.AlarmMeta;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.WindowAssigner;
import org.apache.flink.streaming.api.windowing.triggers.EventTimeTrigger;
import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.api.windowing.windows.Window;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

public class CustomWindowAssigner  extends WindowAssigner<AlarmMeta, TimeWindow> {


    @Override
    public Collection<TimeWindow> assignWindows(AlarmMeta element, long timestamp, WindowAssignerContext context) {
        Long[] offset=new Long[2];
        offset=getTimestampFromFiveMinute(timestamp);
        //分配窗口
        return  Collections.singletonList(new TimeWindow(offset[0], offset[1]));
    }

    @Override
    public Trigger getDefaultTrigger(StreamExecutionEnvironment env) {
        return EventTimeTrigger.create();
    }

    @Override
    public TypeSerializer<TimeWindow> getWindowSerializer(ExecutionConfig executionConfig) {
        return  new TimeWindow.Serializer();
    }

    @Override
    public boolean isEventTime() {
        return true;
    }



    /**
     * 获取指定时间戳五分钟范围
     *
     * @param timestamp 时间戳
     * @return
     */

    private static Long[] getTimestampFromFiveMinute(Long timestamp){
        String[] timeString= getByinterMinute(timestamp+"");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        Date start_date=new Date() ;
        Date end_date =new Date() ;
        try {
             start_date = dateFormat.parse(timeString[0]);
             end_date = dateFormat.parse(timeString[1]);
        }catch (Exception e){

        }
        return  new Long[]{start_date.getTime(),end_date.getTime()};
    }

    private static String[] getByinterMinute(String timeinfo){
        Long timeMillons = Long.parseLong(timeinfo);
        Date date = new Date(timeMillons) ;
        SimpleDateFormat dateFormatMinute = new SimpleDateFormat("mm");
        SimpleDateFormat dateFormatHour = new SimpleDateFormat("yyyyMMddHH");
        String minute = dateFormatMinute.format(date);
        String hour = dateFormatHour.format(date);
        Long minuteLong =  Long.parseLong(minute);
        String endMinute = "";
        String startMinute = "";
        if(minuteLong >= 0 && minuteLong <5){//0-5
            startMinute = "00";
            endMinute = "05";
        }else if (minuteLong >= 5 && minuteLong <10){
            startMinute = "05";
            endMinute = "10";
        }else if (minuteLong >= 10 && minuteLong <15){
            startMinute = "10";
            endMinute = "15";
        }else if (minuteLong >= 15 && minuteLong <20){
            startMinute = "15";
            endMinute = "20";
        }else if (minuteLong >= 20 && minuteLong <25){
            startMinute = "20";
            endMinute = "25";
        }else if (minuteLong >= 25 && minuteLong <30){
            startMinute = "25";
            endMinute = "30";
        }else if (minuteLong >= 30 && minuteLong <35){
            startMinute = "30";
            endMinute = "35";
        }else if (minuteLong >= 35 && minuteLong <40){
            startMinute = "35";
            endMinute = "40";
        }else if (minuteLong >= 40 && minuteLong <45){
            startMinute = "40";
            endMinute = "45";
        }else if (minuteLong >= 45 && minuteLong <50){
            startMinute = "45";
            endMinute = "50";
        }else if (minuteLong >= 50 && minuteLong <55){
            startMinute = "50";
            endMinute = "55";
        }else if (minuteLong >= 55 && minuteLong <60){
            startMinute = "55";
            endMinute = "60";
        }
        String endTime = hour+endMinute ;// 窗口结束时间
        String startTime = hour+startMinute ;//窗口开始时间
        return  new String[]{startTime,endTime};
    }


    public static void main(String[] args) {
        System.out.println(Arrays.toString(getTimestampFromFiveMinute(1536268066000L)));
        Long[] offset=new Long[2];
        if(offset==null){
            System.out.println("aaanull");
        }
    }

}
