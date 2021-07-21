package com.eystar.alarm.flatMap;


import com.alibaba.fastjson.JSONObject;
import com.eystar.alarm.model.Rule;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction;
import org.apache.flink.util.Collector;
import org.drools.core.impl.KnowledgeBaseImpl;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RuleMessageFunction extends RichCoFlatMapFunction<JSONObject, Rule, JSONObject> {
    private  KieHelper kieHelper;
    private  KnowledgeBaseImpl kieBase;
    private  KieSession kieSession;
    private Lock lock = new ReentrantLock();

    @Override
    public void open(Configuration parameters) throws Exception {
        if(kieSession == null ){
            kieHelper = new KieHelper();
            KieBaseConfiguration config = KieServices.Factory.get().newKieBaseConfiguration();
            config.setOption( EventProcessingOption.STREAM );
            try{
                kieBase =(KnowledgeBaseImpl) kieHelper.build();
                kieSession = kieBase.newStatefulSession();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void close() throws Exception {
        Thread.sleep(1000);
        kieSession.destroy();
    }


    public void flatMap1(JSONObject message, Collector collector) throws Exception {
        lock.lock();
        try{
            kieSession.insert(message);
            kieSession.fireAllRules();
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        collector.collect(message);
    }

    public void flatMap2(Rule rule, Collector collector) throws Exception {

            kieHelper = new KieHelper();
            kieBase = (KnowledgeBaseImpl) kieHelper.build();
            KnowledgeBuilder kb = KnowledgeBuilderFactory.newKnowledgeBuilder();
            kb.add(ResourceFactory.newByteArrayResource(rule.getDrlStr().getBytes("utf-8")), ResourceType.DRL);
            kieBase.addKnowledgePackages(kb.getKnowledgePackages());
            kieSession = kieBase.newKieSession();
    }
}

