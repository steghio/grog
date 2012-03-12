package it.eng.paas.aee.impl;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
//import java.net.URL;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.log4j.Logger;

import it.eng.paas.aee.controller.IController;
import it.eng.paas.aee.controller.impl.Controller;
import it.eng.paas.aee.messenger.IMessenger;
import it.eng.paas.aee.messenger.impl.Messenger;
import it.eng.paas.aee.runner.IRunner;
import it.eng.paas.aee.runner.impl.Runner;
import it.eng.paas.utilities.PaasUtilities;

/**
 * 
 * @author GHIO
 *
 */
public class Aee implements Runnable{
	
	//propertiesFile is args[0]
	private static String propertiesFile;//="C:\\temp\\junit\\aee.properties";
	private static Logger logger = null;
	private static String hostInfoFile;
	
	/*submodules*/
	private static IController controller;
	private static IMessenger messenger;
	private static IRunner runner;

	/*config params*/
	private static String aee_logs_dir;
	private static Boolean aee_tenant_setup;
	private static String aee_current_IP;
	private static String aee_storage_user;
	private static String aee_storage_pwd;
	private static String aee_DB_user;
	private static String aee_DB_pwd;
	private static String aee_DB_URL;
	
	/*params*/
	private static Aee aeeInstance = null;
	
	public static Aee getInstance() {
		if(aeeInstance == null)aeeInstance = new Aee(propertiesFile);
		return aeeInstance;
	}
	
	public static Aee getInstance(String propertiesFile) {
		if(aeeInstance == null)aeeInstance = new Aee(propertiesFile);
		return aeeInstance;
	}
	
	private Aee(String propertiesFile){
		//Aee.propertiesFile = propertiesFile+"aee.properties";
		/*initialize config params*/
		loadProperties();
		/*initialize Logger*/
		System.setProperty("rootPath", aee_logs_dir);
		logger = Logger.getLogger("Aee");
		/*initialize modules*/
		loadModules();
		/*gather and store system info*/
		loadSysInfo();
		logger.info("started on "+aee_current_IP);
		/*inform CCs I spawned*/
		messenger.notifyAeeStart();
		startDaemon();
	}
//	
//	private Aee(){
//		/*initialize config params*/
//		loadProperties();
//		/*initialize Logger*/
//		System.setProperty("rootPath", aee_logs_dir);
//		logger = Logger.getLogger("Aee");
//		/*initialize modules*/
//		loadModules();
//		/*gather and store system info*/
//		
//		logger.info("started on "+aee_current_IP);
//		
//	}
	
	private static void sendHeartbeat(){
		messenger.sendHeartBeat();
	}
	
	private static void loadModules(){
		/*initialize submodules*/
		controller = Controller.getInstance(propertiesFile);
		//deployer = Deployer.getInstance();
		messenger = Messenger.getInstance(propertiesFile);
		runner = Runner.getInstance(propertiesFile);
	}
	
	
	private static void loadProperties(){
		Properties props = new Properties();
		try(FileInputStream fis = new FileInputStream(propertiesFile);){
			props.load(fis);
			//fis.close();
			aee_logs_dir = props.getProperty("aee_logs_dir");
			aee_tenant_setup = new Boolean(props.getProperty("aee_tenant_setup"));
			aee_current_IP = InetAddress.getLocalHost().getHostAddress();//props.getProperty("aee_current_IP");
			aee_storage_user = props.getProperty("aee_storage_user");
			aee_storage_pwd = props.getProperty("aee_storage_pwd");
			aee_DB_user = props.getProperty("aee_DB_user");
			aee_DB_pwd = props.getProperty("aee_DB_pwd");
			aee_DB_URL = props.getProperty("aee_DB_URL");
			
		}catch(IOException e){
			System.out.println("unable to load properties file "+propertiesFile);
		}
	}
	
	private static void loadSysInfo(){
		Properties props = new Properties();
		hostInfoFile = aee_logs_dir+"aee.info";
		try(FileOutputStream fis = new FileOutputStream(hostInfoFile);){
			//get info
			Hashtable<String,String> sysinfo = PaasUtilities.getSystemInfo();
			//store in aee.info under aee_log_dir
			for(String key : sysinfo.keySet())props.setProperty(key, sysinfo.get(key));
			props.store(fis, null);
		}catch(IOException e){
			System.err.println("unable to create/write info file "+aee_logs_dir+"aee.info");
			logger.fatal("unable to create/write info file "+aee_logs_dir+"aee.info");
		}
	}

	public void run() {
		controller.run();
		while(true){
			try{
				Aee.sendHeartbeat();
				if(!runner.isAlive())runner = Runner.getInstance(propertiesFile);
				if(!messenger.isAlive())messenger = Messenger.getInstance(propertiesFile);
				if(!controller.isAlive())controller = Controller.getInstance(propertiesFile);
				// TODO se c'e' altro
				Thread.sleep(5000);
			}catch(Exception e){
				logger.fatal("thread interrupted unexpectedly "+e.getMessage(), e);
				System.err.println("error "+e.getMessage());
			}
		}
	}
	
	private void startDaemon(){
		Thread thread = new Thread(this);
		thread.start();
	}
	
	/*getters & setters*/
	public IRunner getRunner(){
		return runner;
	}
	
	public IMessenger getMessenger(){
		return messenger;
	}
	
	public IController getController(){
		return controller;
	}
	
	public static String getPropertiesFile(){
		return propertiesFile;
	}
	
	public static String getHostInfoFile(){
		return hostInfoFile;
	}
	
	public static void main(String[] args){
		if(args.length != 1){
			System.out.println("Usage aee propertiesFile");
			System.exit(1);
		}
		Aee.propertiesFile = args[0];
		aeeInstance = Aee.getInstance();
	}
	
	//check tenant setup
	public Boolean tenantSetup(){
		return aee_tenant_setup;
	}
	
	public Boolean isAlive(){
		return !(aeeInstance == null);
	}
	
	public String getStorageUser(){
		return aee_storage_user;
	}
	
	public String getStoragePwd(){
		return aee_storage_pwd;
	}
	
	public String getCurrentIP(){
		return aee_current_IP;
	}

	public static String getAee_DB_user() {
		return aee_DB_user;
	}

	public static String getAee_DB_pwd() {
		return aee_DB_pwd;
	}

	public static String getAee_DB_URL() {
		return aee_DB_URL;
	}

}
