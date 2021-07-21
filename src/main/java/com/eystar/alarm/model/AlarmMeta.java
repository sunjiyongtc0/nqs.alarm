package com.eystar.alarm.model;


public class AlarmMeta {


    //任务id
    private String task_id;
    //告警id
    private  String alarm_id;
    //输出告警详情
    private String  alarm_info_detail;
    //指标额度？暂定
    private Double  index;
    //告警策略(时间/数量)
    private String   type;
    //告警起始指标（时间段/额定次数）
    private Long  size;
    //告警跨度指标(时间段内告警数量/无)
    private Long  slide;
    //测试时间(事件依据时间)
    private Long  test_time;
    //任务间隔(无/数据上传时间间隔)
    private long interval;


    @Override
    public String toString() {
        return "AlarmMeta{" +
                "任务id：'" + task_id + '\'' +
                ", 告警id：'" + alarm_id + '\'' +
                ", 输出告警详情：'" + alarm_info_detail + '\'' +
                ", 指标额度：" + index +
                ", 告警策略：'" + type + '\'' +
                ", 告警起始指标：" + size +
                ", 告警跨度指标：" + slide +
                ", 测试时间：" + test_time +
                ", 任务间隔：" + interval +
                '}';
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public Long getTest_time() {
        return test_time;
    }

    public void setTest_time(Long test_time) {
        this.test_time = test_time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getSlide() {
        return slide;
    }

    public void setSlide(Long slide) {
        this.slide = slide;
    }


    public Double getIndex() {
        return index;
    }

    public void setIndex(Double index) {
        this.index = index;
    }





    public String getAlarm_id() {
        return alarm_id;
    }

    public void setAlarm_id(String alarm_id) {
        this.alarm_id = alarm_id;
    }

    public String getAlarm_info_detail() {
        return alarm_info_detail;
    }

    public void setAlarm_info_detail(String alarm_info_detail) {
        this.alarm_info_detail = alarm_info_detail;
    }

    public String getTask_id() {
        return task_id;
    }

    public void setTask_id(String task_id) {
        this.task_id = task_id;
    }



}
