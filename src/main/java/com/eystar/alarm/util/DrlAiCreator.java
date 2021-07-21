package com.eystar.alarm.util;

import cn.hutool.core.util.StrUtil;
import com.jfinal.plugin.activerecord.Record;
import org.drools.compiler.lang.DrlDumper;
import org.drools.compiler.lang.api.DescrFactory;
import org.drools.compiler.lang.api.PackageDescrBuilder;
import org.drools.compiler.lang.descr.PackageDescr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class DrlAiCreator {

    public static String creatFileString(Record record){

        String index="jitter";
        if(StrUtil.equals(record.getStr("task_type_name"),"HTTP"))
            index="dns_cost";
        if(StrUtil.equals(record.getStr("task_type_name"),"DNS"))
            index="time_cost";

        PackageDescrBuilder pdb=   DescrFactory.newPackage()
                .name("rules")
//                .name(record.getStr("id"))
                .newImport().target("com.alibaba.fastjson.JSONObject").end()
                .newImport().target("com.eystar.alarm.model.RuleTreeOrbit").end()
                ;
        for(int i=0;i<3;i++){
            if(i==0) {
                pdb.newRule().name(record.getStr("id") + "_" + i)
                        .attribute("salience", (100-i)+"")//执行顺序
                        .attribute("no-loop", "false")//数据只执行一次
                        .lhs()
                        .and()
                        .pattern("JSONObject").id("$j", false).constraint(" alarm_id !=\""+record.getStr("id")+"\"").end()
                        .end()
                        .end()
                        .rhs("  drools.halt(); ").end();//不满足条件清除该数据
            }else if(i==2){
                pdb.newRule().name(record.getStr("id") + "_" + i)
                        .attribute("salience", (100-i)+"")
                        .attribute("no-loop", "false")
                        .lhs()
                        .and()
                        .pattern("JSONObject").id("$j", false).constraint("$j.containsKey(\""+index+"\") && "+index+">1 && alarm_id ==\""+record.getStr("id")+"\"  && (!$j.containsKey(\"alarm_info_detail\") ) " ).end()
                        .end()
                        .end()
                        .rhs("$j.put(\"alarm_info_detail\",\""+record.getStr("task_type_name")+"告警模板"+record.getStr("id")+"数据存在告警\");").end();

            }
        }
        PackageDescr   pkg = pdb.getDescr();
        DrlDumper dumper=new DrlDumper();
        String drl=dumper.dump(pkg);
        return drl;
    }

    public  static void CreatDrl(Record record,String context){
        try{
            String fileName=record.getStr("id");
            String ParentPath="E:\\JAVACODE\\nqs\\console-idea\\console-alarm\\src\\main\\resources\\rules";
            String path=ParentPath+ File.separator+fileName+".drl";
            File file = new File(path);
            file.delete();
            if(record.getInt("delete_flag") !=1){
                file.createNewFile();
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(context);
                bw.close();
                System.out.println("File Created Successfully");
            }else{
                System.out.println("File delete!!!!!");
            }
        }catch(Exception e){
            System.out.println(e);
        }



    }




}
