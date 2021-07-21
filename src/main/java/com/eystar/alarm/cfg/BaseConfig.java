package com.eystar.alarm.cfg;

import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.druid.DruidPlugin;
import com.xxl.conf.core.XxlConfClient;
import com.xxl.conf.core.factory.XxlConfBaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jfinal.kit.PropKit;



/**
 * API引导式配置
 */
public class BaseConfig  {

	public final static Logger logger = LoggerFactory.getLogger(BaseConfig.class);

	/**
	 * 初始化方法
	 */
	public static void init() {
		initXxlConf();
		initJDBC();
	}

	/**
	 * 
	 */
	private static void initXxlConf() {
		PropKit.use("little_config.txt");
		// 初始化xxl-conf
		XxlConfBaseFactory.init(PropKit.get("xxl.conf.admin.address"), PropKit.get("xxl.conf.env"), PropKit.get("xxl.conf.access.token"), PropKit.get("xxl.conf.mirrorfile"));
	}

	public static void initJob(){
		QuartzPlugin quartzPlugin = new QuartzPlugin("job.properties");
		quartzPlugin.start();
	}
	private static  void initJDBC(){
		DruidPlugin dsMysql = new DruidPlugin(XxlConfClient.get("nqs-db.mysql.jdbc"), XxlConfClient.get("nqs-db.mysql.user"), XxlConfClient.get("nqs-db.mysql.password"), XxlConfClient.get("nqs-db.mysql.driver"));

		ActiveRecordPlugin arp= new ActiveRecordPlugin("mysql",dsMysql);
		dsMysql.start();
		arp.start();
	}
}
