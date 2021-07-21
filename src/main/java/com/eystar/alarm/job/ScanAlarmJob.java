package com.eystar.alarm.job;

import com.eystar.alarm.util.DrlAiCreator;
import com.eystar.alarm.util.DrlCreator;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Record;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.List;

public class ScanAlarmJob   implements Job {

    private static Log logger= Log.getLog(ScanAlarmJob.class);

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.info("开始执行扫描告警模板");
        try{
            run() ;
            logger.info("执行扫描告警模板成功");
        }catch (Exception e) {
            logger.error("执行扫描告警模板任务失败",e);
        }

    }

    public  static void   run(){
        List<Record> lr= Db.find("select * from t_m_alarm_template ");
        if(lr.size()>0) {
            for (Record r : lr) {
                creatRule(r);
            }
        }
    }

    public static void  creatRule(Record record){
        String drl="";
        if(record.getInt("delete_flag")==0){
//            drl=DrlCreator.creatFileString(record);
            drl= DrlAiCreator.creatFileString(record);
        }
        DrlCreator.CreatDrl(record,drl);

    }
}
