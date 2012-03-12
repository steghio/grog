package it.eng.paas.hostControl;

import it.eng.paas.message.PaasMessage;
import it.eng.paas.utilities.PaasUtilities;

import java.io.File;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

public class Runner implements Runnable{
	
	private Document request;
	private String appDir;
	private String javaDir;
	private static Logger logger=Logger.getLogger("Runner");
	
	//TODO
	////////////////////////////////////////////////////////////
	// usa p.exitValue() per vedere se processo è partito senza problemi
	////////////////////////////////////////////////////////////
	public Runner(Document request){
		this.request = request;
		if(PaasUtilities.isWindowsOS()){
			this.appDir = "C:/paas/";
			this.javaDir = "C:/Programmi/Java/jdk1.7.0/bin/java.exe";
		}
		else{
			this.appDir = "/paas/";
			this.javaDir = "/usr/lib/jvm/java-7-oracle/bin/java";
		}
		logger.info("using appDir: "+appDir+" and javaDir: "+javaDir+" to satisfy request: "+PaasUtilities.XML2String(request));
	}
	
	private void consumeRequest(){
		Hashtable<String, String> req = PaasUtilities.XML2Hashtable(request);
		//skip bad request
		if(req==null || req.isEmpty())return;
		PaasMessage type = PaasMessage.fromString(req.get("type"));
		if(type == null)return;
		switch(type){
			case REQUEST_START_AEE:{
				consumeStartAee(req);
				break;
			}
			case REQUEST_START_CC:{
				consumeStartCC(req);
				break;
			}
			case REQUEST_START_RR:{
				consumeStartRR(req);
				break;
			}
			case REQUEST_START_PO:{
				consumeStartPO(req);
				break;
			}
			default:{
				//should never get here
				System.out.println("Bad request type:"+type+" message "+PaasUtilities.XML2String(request));
				logger.error("Bad request type:"+type+" message "+PaasUtilities.XML2String(request));
			}
		}
	}
	
	@SuppressWarnings("unused")
	private void consumeStartAee(Hashtable<String, String> request){
		//read properties file from request and write it to aee folder if ok
		Hashtable<String, String> propfile = PaasUtilities.string2Hashtable(request.get("propfile"));
		if(propfile == null || propfile.equals("")){
			//TODO
		}
		String dir = appDir+"aee/";
		try(PrintWriter out = new PrintWriter(dir+"aee.properties");){
			for(String key : propfile.keySet()){
				out.println(key+": "+propfile.get(key));
			}
		}catch(Exception e){
			System.err.println("unable to create/write file "+dir+"aee.properties"+" error: "+e.getMessage());
			logger.fatal("unable to create/write file "+dir+"aee.properties"+" error: "+e.getMessage(), e);
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
			p = null;
			Runtime.getRuntime().gc();			
			//TODO controlla effettiva partenza
		} catch (Exception e) {
			logger.fatal("error while starting AEE with properties "+propfile+"\n error:"+e.getMessage(), e);
			System.err.println("error while starting AEE with properties "+propfile+"\n error:"+e.getMessage());
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	private void consumeStartCC(Hashtable<String, String> request){
		//read properties file from request and write it to aee folder if ok
		Hashtable<String, String> propfile = PaasUtilities.string2Hashtable(request.get("propfile"));
		if(propfile == null || propfile.equals("")){
			//TODO
		}
		String dir = appDir+"management/";
		try(PrintWriter out = new PrintWriter(dir+"cc.properties");){
			for(String key : propfile.keySet()){
				out.println(key+": "+propfile.get(key));
			}
		}catch(Exception e){
			System.err.println("unable to create/write file "+dir+"cc.properties"+" error: "+e.getMessage());
			logger.fatal("unable to create/write file "+dir+"cc.properties"+" error: "+e.getMessage(), e);
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
			p = null;
			Runtime.getRuntime().gc();
			//TODO controlla effettiva partenza
		} catch (Exception e) {
			logger.fatal("error while starting CC with properties "+propfile+"\n error:"+e.getMessage(), e);
			System.err.println("error while starting CC with properties "+propfile+"\n error:"+e.getMessage());
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private void consumeStartRR(Hashtable<String, String> request){
		//read properties file from request and write it to aee folder if ok
		Hashtable<String, String> propfile = PaasUtilities.string2Hashtable(request.get("propfile"));
		if(propfile == null || propfile.equals("")){
			//TODO
		}
		String dir = appDir+"networking/";
		try(PrintWriter out = new PrintWriter(dir+"rr.properties");){
			for(String key : propfile.keySet()){
				out.println(key+": "+propfile.get(key));
			}
		}catch(Exception e){
			System.err.println("unable to create/write file "+dir+"rr.properties"+" error: "+e.getMessage());
			logger.fatal("unable to create/write file "+dir+"rr.properties"+" error: "+e.getMessage(), e);
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
			p = null;
			Runtime.getRuntime().gc();
			//TODO controlla effettiva partenza
		} catch (Exception e) {
			logger.fatal("error while starting RR with properties "+propfile+"\n error:"+e.getMessage(), e);
			System.err.println("error while starting RR with properties "+propfile+"\n error:"+e.getMessage());
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private void consumeStartPO(Hashtable<String, String> request){
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
			p = null;
			Runtime.getRuntime().gc();
			//TODO controlla effettiva partenza
		} catch (Exception e) {
			logger.fatal("error while starting RabbitMQ server "+e.getMessage(), e);
			System.err.println("error while starting RabbitMQ server "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public void run(){
		consumeRequest();
	}
	
	public void startDaemon(){
		Thread t = new Thread(this);
		t.start();
	}

}
