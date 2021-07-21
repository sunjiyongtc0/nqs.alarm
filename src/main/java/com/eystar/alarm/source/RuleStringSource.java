package com.eystar.alarm.source;



import com.eystar.alarm.model.Rule;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class RuleStringSource extends RichParallelSourceFunction<String> {

    // 定义一个标识，表示数据源是否继续运行
    public boolean running = true;

    public void run(SourceContext<String> sourceContext) throws Exception {
        while (running) {
            File directory =new File("E:\\JAVACODE\\nqs\\console-idea\\console-alarm\\src\\main\\resources\\rules");
            if(directory.isDirectory()) {
                File[] files=directory.listFiles();
                for(int i=0;i<files.length;i++){
                    Rule rule = new Rule();
                    File file =files[i];
                    FileReader reader = new FileReader(file);
                    BufferedReader br = new BufferedReader(reader);
                    String line;
                    StringBuffer sb = new StringBuffer();
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    sourceContext.collect(sb.toString());
                }
            }
        }
    }

    public void cancel() {

    }




}
