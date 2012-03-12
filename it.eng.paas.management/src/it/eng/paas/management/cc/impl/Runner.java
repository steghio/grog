package it.eng.paas.management.cc.impl;

import it.eng.paas.module.PaasModule;
import it.eng.paas.utilities.PaasUtilities;

import java.io.File;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.Hashtable;

import org.apache.log4j.Logger;

public class Runner{
	
	private Hashtable<String, String> properties;
	private String appDir;
	private String javaDir;
	private static Logger logger=Logger.getLogger("Runner");
	
	public Runner(Hashtable<String, String> properties){
		this.properties = properties;
		if(PaasUtilities.isWindowsOS()){
			this.appDir = "C:/paas/";
			this.javaDir = "C:/Programmi/Java/jdk1.7.0/bin/java.exe";
		}
		else{
			this.appDir = "/paas/";
			this.javaDir = "/usr/lib/jvm/java-7-oracle/bin/java";
		}
		logger.info("using appDir: "+appDir+" and javaDir: "+javaDir);
	}
	
	public boolean startModule(PaasModule module){
		switch(module){
			case AEE:{
				return startAee(properties);
			}
			case CC:{
				return startCC(properties);
			}
			case RR:{
				return startRR(properties);
			}
			case PO:{
				return startPO(properties);
			}
			default:{
				//should never get here
				System.out.println("Bad request module:"+module.getText());
				logger.error("Bad request module:"+module.getText());
				return false;
			}
		}
	}
	
	private Boolean startAee(Hashtable<String, String> properties){
		String dir = appDir+"aee/";
		try(PrintWriter out = new PrintWriter(dir+"aee.properties");){
			for(String key : properties.keySet()){
				out.println(key+": "+properties.get(key));
			}
		}catch(Exception e){
			System.err.println("unable to create/write file "+dir+"aee.properties"+" error: "+e.getMessage());
			logger.fatal("unable to create/write file "+dir+"aee.properties"+" error: "+e.getMessage(), e);
			return false;
		}
		//start aee
		String [] cmd = {javaDir, "-jar", dir+"it.eng.paas.aee.jar", dir+"aee.properties"};
		try {
			System.out.println("starting aee from dir "+dir);
			logger.info("starting aee from dir "+dir);
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.directory(new File(dir));
			//redirect stdout and stderr to father's so that Java does not hang while waiting for someone to consume those poor lonely streams
			pb.redirectError(Redirect.INHERIT);
			pb.redirectOutput(Redirect.INHERIT);
			//for some reason, if the object Process p is not destroyed, the new process takes up a lot of time
			//to start. Destroying it and calling the garbage collector right away fixes the issue
			Process p = pb.start();
			//give it some time to start
			Thread.sleep(5000);
			try{
				p.exitValue();
				//something went bad
				return false;
			}catch(Exception e){
				//it's ok, we do not want it to exit
			}
			p = null;
			Runtime.getRuntime().gc();			
			//TODO controlla effettiva partenza
		} catch (Exception e) {
			logger.fatal("error while starting AEE :"+e.getMessage(), e);
			System.err.println("error while starting AEE :"+e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private boolean startCC(Hashtable<String, String> properties){
		String dir = appDir+"management/";
		try(PrintWriter out = new PrintWriter(dir+"cc.properties");){
			for(String key : properties.keySet()){
				out.println(key+": "+properties.get(key));
			}
		}catch(Exception e){
			System.err.println("unable to create/write file "+dir+"cc.properties"+" error: "+e.getMessage());
			logger.fatal("unable to create/write file "+dir+"cc.properties"+" error: "+e.getMessage(), e);
			return false;
		}
		//start cc
		String [] cmd = {javaDir, "-jar", dir+"it.eng.paas.management.jar", dir+"cc.properties"};
		try {
			System.out.println("starting cc from dir "+dir);
			logger.info("starting cc from dir "+dir);
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.directory(new File(dir));
			//redirect stdout and stderr to father's so that Java does not hang while waiting for someone to consume those poor lonely streams
			pb.redirectError(Redirect.INHERIT);
			pb.redirectOutput(Redirect.INHERIT);
			//for some reason, if the object Process p is not destroyed, the new process takes up a lot of time
			//to start. Destroying it and calling the garbage collector right away fixes the issue
			Process p = pb.start();
			//give it some time to start
			Thread.sleep(5000);
			try{
				p.exitValue();
				//something went bad
				return false;
			}catch(Exception e){
				//it's ok, we do not want it to exit
			}
			p = null;
			Runtime.getRuntime().gc();
			//TODO controlla effettiva partenza
		} catch (Exception e) {
			logger.fatal("error while starting CC :"+e.getMessage(), e);
			System.err.println("error while starting CC :"+e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private boolean startRR(Hashtable<String, String> properties){
		String dir = appDir+"networking/";
		try(PrintWriter out = new PrintWriter(dir+"rr.properties");){
			for(String key : properties.keySet()){
				out.println(key+": "+properties.get(key));
			}
		}catch(Exception e){
			System.err.println("unable to create/write file "+dir+"rr.properties"+" error: "+e.getMessage());
			logger.fatal("unable to create/write file "+dir+"rr.properties"+" error: "+e.getMessage(), e);
			return false;
		}
		//start rr
		String [] cmd = {javaDir, "-jar", dir+"it.eng.paas.networking.jar", dir+"rr.properties"};
		try {
			System.out.println("starting rr from dir "+dir);
			logger.info("starting rr from dir "+dir);
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.directory(new File(dir));
			//redirect stdout and stderr to father's so that Java does not hang while waiting for someone to consume those poor lonely streams
			pb.redirectError(Redirect.INHERIT);
			pb.redirectOutput(Redirect.INHERIT);
			//for some reason, if the object Process p is not destroyed, the new process takes up a lot of time
			//to start. Destroying it and calling the garbage collector right away fixes the issue
			Process p = pb.start();
			//give it some time to start
			Thread.sleep(5000);
			try{
				p.exitValue();
				//something went bad
				return false;
			}catch(Exception e){
				//it's ok, we do not want it to exit
			}
			p = null;
			Runtime.getRuntime().gc();
			//TODO controlla effettiva partenza
		} catch (Exception e) {
			logger.fatal("error while starting RR :"+e.getMessage(), e);
			System.err.println("error while starting RR :"+e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private boolean startPO(Hashtable<String, String> request){
		String [] cmd = new String[3];
		if(PaasUtilities.isWindowsOS()){
			cmd[0] = "rabbitmq-server";
			cmd[1] = "start";
		}
		else{
			cmd[0] = "invoke-rc.d";
			cmd[1] = "rabbitmq-server";
			cmd[2] = "start";
		}
		try {
			System.out.println("starting PO");
			logger.info("starting PO");
			ProcessBuilder pb = new ProcessBuilder(cmd);
			//redirect stdout and stderr to father's so that Java does not hang while waiting for someone to consume those poor lonely streams
			pb.redirectError(Redirect.INHERIT);
			pb.redirectOutput(Redirect.INHERIT);
			//for some reason, if the object Process p is not destroyed, the new process takes up a lot of time
			//to start. Destroying it and calling the garbage collector right away fixes the issue
			Process p = pb.start();
			//give it some time to start
			Thread.sleep(5000);
			try{
				p.exitValue();
				//something went bad
				return false;
			}catch(Exception e){
				//it's ok, we do not want it to exit
			}
			p = null;
			Runtime.getRuntime().gc();
			//TODO controlla effettiva partenza
		} catch (Exception e) {
			logger.fatal("error while starting RabbitMQ server "+e.getMessage(), e);
			System.err.println("error while starting RabbitMQ server "+e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
