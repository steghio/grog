package it.eng.paas.aee.controller.impl;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import it.eng.paas.aee.controller.IController;
import it.eng.paas.aee.impl.Aee;
import it.eng.paas.aee.messenger.impl.Messenger;
import it.eng.paas.aee.runner.impl.Runner;
import it.eng.paas.aee.runner.impl.Sandbox;
import it.eng.paas.utilities.PaasUtilities;
import it.eng.paas.message.PaasMessage;

public class Controller implements IController, Runnable{
	
	private static Controller controllerInstance = null;
	private static final Logger logger = Logger.getLogger("Controller");
	private String aee_current_IP;
	private int aee_controller_recovery_retries;
	private List<Document> acceptAppMessages; //check if AEE can run app
	private List<Document> startAppMessages;
	private List<Document> stopAppMessages; //stop all app instances
	private List<Document> scaleAppMessages;
	
	private Hashtable<String, Integer> appsToRecover;//appID, recoveryTries
	
	public static IController getInstance(String propertiesFile){
		if(controllerInstance == null)controllerInstance = new Controller(propertiesFile);
		return controllerInstance;
	}
	
	public static IController getInstance(){
		if(controllerInstance == null)controllerInstance = new Controller(Aee.getPropertiesFile());
		return controllerInstance;
	}
	
	private Controller(String propertiesFile){
		Properties props = new Properties();
		try(FileInputStream fis = new FileInputStream(propertiesFile);){
			props.load(fis);
			//fis.close();
			aee_current_IP = InetAddress.getLocalHost().getHostAddress();//props.getProperty("aee_current_IP");
			aee_controller_recovery_retries = Integer.parseInt(props.getProperty("aee_controller_recovery_retries"));
		}catch(Exception e){
			logger.error("loadProperties fail: "+e.getMessage(), e);
			System.err.println("loadProperties fail: "+e.getMessage());
		}
		appsToRecover = new Hashtable<>();
		acceptAppMessages = new LinkedList<>();
		startAppMessages = new LinkedList<>();
		stopAppMessages = new LinkedList<>();
		scaleAppMessages = new LinkedList<>();
		logger.info("started on "+aee_current_IP);
	}

	public Boolean isAlive(){
		return !(controllerInstance == null);
	}
	
	private Boolean isSandboxOK(Sandbox s){
		if(isSandboxAlive(s)){
			Boolean ok=true;
			try(FileInputStream fstream = new FileInputStream(s.getErrFile());
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));){
				String strLine;
				while ((strLine = br.readLine()) != null)   {
					if(strLine.toLowerCase().contains("exception") || strLine.toLowerCase().contains("!stack")){
						ok=false;
						break;
					}
				}
				//if started ok as a recovery attempt, take no further actions
				if(ok && appsToRecover.containsKey(s.getAppID()))appsToRecover.remove(s.getAppID());
	//			br.close();
	//			in.close();
	//			fstream.close();
			}catch (Exception e){
					logger.error("unable to read "+s.getErrFile());
					System.err.println("unable to read "+s.getErrFile());
					return false;
			}
			return ok;
		}
		return false;
	}
	
	private Boolean isSandboxAlive(Sandbox s){
		return Runner.getInstance().isAppAlive(s.getConsolePort(), s.getAppID());
	}
	
	private void addAppToRecover(String appID){
		try{
			//if already existing, I have already tried to recover it, try again until limit
			if(appsToRecover.containsKey(appID)){
				//over the limit, fail
				if(appsToRecover.get(appID).intValue() > aee_controller_recovery_retries){
					logger.fatal("unable to recover "+appID+" from failure, retry attempts done: "+aee_controller_recovery_retries);
					System.err.println("unable to recover "+appID+" from failure, retry attempts done: "+aee_controller_recovery_retries);
					appsToRecover.remove(appID);
				}
				else appsToRecover.put(appID, appsToRecover.get(appID).intValue()+1);
			}
			else appsToRecover.put(appID, 1);
		}catch(Exception e){
			logger.error("failed to update appsToRecover "+e.getMessage(), e);
			System.err.println("failed to update appsToRecover "+e.getMessage());
		}
	}
	
	private void checkSandboxes(){
		if(Runner.getInstance().isRunningSandboxes()){
			for(Sandbox s : Runner.getInstance().getSandboxes()){
				if(!isSandboxOK(s)){
					logger.info("detected sandbox "+s.getSID()+" failure, trying to stop and restart");
					System.out.println("detected sandbox "+s.getSID()+" failure, trying to stop it");
					Runner.getInstance().stopApp(s.getAppID(), s.getSID());
					addAppToRecover(s.getAppID());
				}
			}
			Runner.getInstance().clearSandboxes();
		}
	}
	
	private void checkApps(){
		if(hasAppsToRecover()){
			for(String s : getAppsToRecover()){
				logger.info("attempting to recover "+s+" from failure");
				System.out.println("attempting to recover "+s+" from failure");
				Runner.getInstance().startApp(s);
			}
		}
	}
	
	public Set<String> getAppsToRecover(){
		return appsToRecover.keySet();
	}
	
	public Boolean hasAppsToRecover(){
		return !appsToRecover.isEmpty();
	}
	
	public void addAcceptAppMessage(Document message){
		acceptAppMessages.add(message);
	}
	
	public void addStartAppMessage(Document message){
		//TODO riga sotto solo per test
		if(message == null)System.out.println("messaggio null "+PaasUtilities.XML2String(message));
		startAppMessages.add(message);
	}
	
	public void addStopAppMessage(Document message){
		stopAppMessages.add(message);
	}
	
	public void addScaleAppMessage(Document message){
		scaleAppMessages.add(message);
	}
	
	public Boolean hasAcceptAppMessages(){
		return !acceptAppMessages.isEmpty();
	}
	
	public Boolean hasStartAppMessages(){
		return !startAppMessages.isEmpty();
	}
	
	public Boolean hasStopAppMessages(){
		return !stopAppMessages.isEmpty();
	}
	
	public Boolean hasScaleAppMessages(){
		return !scaleAppMessages.isEmpty();
	}
	
	/*
	 * Message topics:
	 * acceptApp - ask the AEE if it can run the app
	 * startApp
	 * stopApp - stop all app instances
	 * scaleApp
	 * Message parameters:
	 * acceptApp manifest - the URL where to find the MANIFEST for the app to be deployed
	 * startApp string_URI - the URI as a string where to find the app to be started
	 * stopApp appID
	 * scaleApp appID instances
	 */
	private void readMessages(){
		readAcceptAppMessages();
		readStartAppMessages();
		readStopAppMessages();
		readScaleAppMessages();
	}

	private void readScaleAppMessages() {
		if(hasScaleAppMessages()){
			Iterator<Document> iter = scaleAppMessages.iterator();
			while(iter.hasNext()){
				Document message = iter.next();
				Hashtable<String, String> read = PaasUtilities.XML2Hashtable(message);
				String appID = read.get("appID");
				int instances = Integer.parseInt(read.get("instances"));
				String replyTo = read.get("replyTo");
				//check if app is already deployed start it
				if(Runner.getInstance().isAppDeployed(appID))Runner.getInstance().scaleApp(appID, instances);
				else{
					//get location and deploy then start
					if(!Runner.getInstance().deployApp(read.get("location"), appID)){
						System.err.println("Deploy app fail, location: "+read.get("location"));
						Messenger.getInstance().sendMessage(PaasMessage.TOPIC_SCALE_APP, PaasMessage.REPLY_APP_DEPLOY_FAIL, replyTo, null);
						continue;
					}
					if(!Runner.getInstance().scaleApp(appID, instances))Messenger.getInstance().sendMessage(PaasMessage.TOPIC_SCALE_APP, PaasMessage.REPLY_APP_SCALE_FAIL, replyTo, null);
					//OK messages sent by start, stop and scale in Runner
					//no replyTo if OK as it has to be broadcasted
				}
				iter.remove();
			}
		}
	}

	private void readStopAppMessages() {
		if(hasStopAppMessages()){
			Iterator<Document> iter = stopAppMessages.iterator();
			while(iter.hasNext()){
				Document message = iter.next();
				Hashtable<String, String> read = PaasUtilities.XML2Hashtable(message);
				String replyTo = read.get("replyTo");
				if(!Runner.getInstance().stopAppAll(read.get("appID")))Messenger.getInstance().sendMessage(PaasMessage.TOPIC_STOP_APP, PaasMessage.REPLY_APP_STOP_FAIL, replyTo, null);
				//stop_app_all message sent by Runner.stopAppAll
				//no replyTo if OK as it has to be broadcasted
				iter.remove();
			}
		}
	}

	private void readStartAppMessages() {
		if(hasStartAppMessages()){
			Iterator<Document> iter = startAppMessages.iterator();
			while(iter.hasNext()){
				Document message = iter.next();
				Hashtable<String, String> read = PaasUtilities.XML2Hashtable(message);
				//get appID
				String appID = read.get("appID");
				String replyTo = read.get("replyTo");
				//check if app is already deployed start it
				if(Runner.getInstance().isAppDeployed(appID))Runner.getInstance().startApp(appID);
				else{
					System.out.println("LOC: "+read.get("location")+" LOC");
					//get location and deploy then start
					if(!Runner.getInstance().deployApp(read.get("location"), appID)){
						Messenger.getInstance().sendMessage(PaasMessage.TOPIC_START_APP, PaasMessage.REPLY_APP_DEPLOY_FAIL, replyTo, null);
						continue;
					}
					Sandbox s = Runner.getInstance().startApp(appID);
					if(!Runner.getInstance().isRunningSandbox(s.getSID()))Messenger.getInstance().sendMessage(PaasMessage.TOPIC_START_APP, PaasMessage.REPLY_APP_START_FAIL, replyTo, null);
					//start_app_OK is sent by Runner.startApp since sending that message when scaling is a challenge
					//no replyTo if OK as it has to be broadcasted
				}
				iter.remove();
			}
		}
	}

	private void readAcceptAppMessages() {
		//reply is TOPIC, REPLY, replyTo, INFO - if sending no info or reply broadcast use null
		if(hasAcceptAppMessages()){
			Iterator<Document> iter = acceptAppMessages.iterator();
			while(iter.hasNext()){
				Document message = iter.next();
				Hashtable<String, String> read = PaasUtilities.XML2Hashtable(message);
				//get appID
				String appID = read.get("appID");
				String replyTo = read.get("replyTo");
				//standard info is current CPU and RAM usage
				/*
				 * <current_status>
				 * <CPU_usage>VALUE</CPU_usage>
				 * <RAM_usage>VALUE</RAM_usage>
				 * </current_status>
				 */
				//if app is already deployed
				if(Runner.getInstance().isAppDeployed(appID)){
					Document additionalInfo = PaasUtilities.createBaseXML("current_status");
					PaasUtilities.addXMLnode(additionalInfo, "appID", appID);
					PaasUtilities.addXMLnode(additionalInfo, "aee", Messenger.getInstance().getQueueName());
					PaasUtilities.addXMLnode(additionalInfo, "CPU_usage", PaasUtilities.getCPUUsage());
					PaasUtilities.addXMLnode(additionalInfo, "RAM_usage", PaasUtilities.getRAMUsage());
					Messenger.getInstance().sendMessage(PaasMessage.TOPIC_ACCEPT_APP, PaasMessage.REPLY_APP_ALREADY_PRESENT, replyTo, additionalInfo);
				}
				//else if it is not deployed AND I'm not reserved to another app
				else{
					if(checkTenantSetupForApp(appID)){
						//check if I can run it
						if(checkAppRequirements(read.get("manifest"))){
							Document additionalInfo = PaasUtilities.createBaseXML("current_status");
							PaasUtilities.addXMLnode(additionalInfo, "appID", appID);
							PaasUtilities.addXMLnode(additionalInfo, "aee", Messenger.getInstance().getQueueName());
							PaasUtilities.addXMLnode(additionalInfo, "CPU_usage", PaasUtilities.getCPUUsage());
							PaasUtilities.addXMLnode(additionalInfo, "RAM_usage", PaasUtilities.getRAMUsage());
							Messenger.getInstance().sendMessage(PaasMessage.TOPIC_ACCEPT_APP, PaasMessage.REPLY_APP_ACCEPTABLE, replyTo, additionalInfo);
						}
					}
				}
				//if cannot accept, do not reply
				iter.remove();
			}
		}
	}
	
	//true if Aee can run appID, false otherwise
	public Boolean checkTenantSetupForApp(String appID){
		//if is single (true) and is already running an app
		if(Aee.getInstance().tenantSetup() && !Runner.getInstance().isRunningApp(appID))return false;
		return true;
	}
	
	//true if can accept app after reading MANIFEST
	private Boolean checkAppRequirements(String manifest){
		//Properties props = new Properties();
		Properties sys = new Properties();
		try(/*FileInputStream fis = new FileInputStream(manifest);*/
			FileInputStream fisSys = new FileInputStream(Aee.getHostInfoFile());){
			//props.load(fis);
			Hashtable<String, String> mf = PaasUtilities.string2Hashtable(manifest);//PaasUtilities.XML2Hashtable(PaasUtilities.string2XML(manifest));
			sys.load(fisSys);
			//fis.close();
			//check system requirements os,arch,RAM,Hz,Cores
			//OS
			String ros = mf.get("Require-OS");//props.getProperty("Require-OS").toLowerCase();
			if(ros != null && !ros.equalsIgnoreCase(sys.getProperty("os"))){
				System.out.println("cannot accept app, wrong OS");
				logger.info("cannot accept app, wrong OS");
				return false;
			}
			//ARCH
			String rosArch = mf.get("Require-Arch");//props.getProperty("Require-Arch").toLowerCase();
			if(rosArch!=null && !rosArch .equalsIgnoreCase(sys.getProperty("arch"))){
				System.out.println("cannot accept app, wrong architecture");
				logger.info("cannot accept app, wrong architecture");
				return false;
			}
			//RAM
			//if required > available
			String rram = mf.get("Require-RAM");//props.getProperty("Require-RAM").toLowerCase();
			if(rram!=null && Double.parseDouble(rram) > PaasUtilities.getCurrentlyAvailableRAM()){
				System.out.println("cannot accept app, not enough free ram");
				logger.info("cannot accept app, not enough free ram");
				return false;
			}
			//HZ
			//if required > total
			String rHz = mf.get("Require-Hz");//props.getProperty("Require-Hz").toLowerCase();
			if(rHz!=null && rHz!="" && Double.parseDouble(rHz) > Double.parseDouble(sys.getProperty("Hz"))){
				System.out.println("cannot accept app, CPU too slow");
				logger.info("cannot accept app, CPU too slow");
				return false;
			}
			//CORES
			//if required > total
			String rcores = mf.get("Require-Cores");//props.getProperty("Require-Cores").toLowerCase();
			if(rcores!=null && rcores!="" && Integer.parseInt(rcores) > Integer.parseInt(sys.getProperty("cores"))){
				System.out.println("cannot accept app, too few cores");
				logger.info("cannot accept app, too few cores");
				return false;
			}
			//if ok, update service list
			Runner.getInstance().requestServiceAddresses(manifest);
			return true;
		}catch(Exception e){
			System.out.println("error while reading MANIFEST "+manifest+" or hostInfoFile "+Aee.getHostInfoFile()+" "+e.getMessage());
			logger.fatal("error while reading MANIFEST "+manifest+" or hostInfoFile "+Aee.getHostInfoFile()+" "+e.getMessage());
			e.printStackTrace();
			return false;
		}
	}
	
	public void run() {
		while(true){
			try{
				Thread.sleep(5000);
				Runner.getInstance().clearSandboxes();
				Thread.sleep(10000);
				checkSandboxes();
				Thread.sleep(5000);
				checkApps();
				Thread.sleep(5000);
				//Messenger.getMessages();
				//Thread.sleep(5000);
				readMessages();
				//TODO fai aggiornare serviceAddresses di Runner ogni tot
			}catch(Exception e){
				logger.fatal("thread interrupted unexpectedly "+e.getMessage(), e);
				System.err.println("error "+e.getMessage());
			}
		}
	}

}
