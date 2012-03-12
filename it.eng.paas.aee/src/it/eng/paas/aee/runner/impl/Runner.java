package it.eng.paas.aee.runner.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import it.eng.paas.aee.impl.Aee;
import it.eng.paas.message.PaasMessage;
import it.eng.paas.aee.messenger.impl.Messenger;
import it.eng.paas.aee.runner.IRunner;
import it.eng.paas.utilities.PaasUtilities;

public class Runner implements IRunner{
	
	private static Runner runnerInstance = null;
	private static final Logger logger = Logger.getLogger("Runner");
	
	private Hashtable<String, Sandbox> sandboxes;//sID, Sandbox
	private Hashtable<String, List<String>> apps;//appID, List<sID>
	private List<String> usedPorts;//ports assigned to the processes
	private List<String> deployedApps;
	private Hashtable<String, List<String>> serviceAddresses; //list of known services and their locations
	
	/*config params*/
	private String aee_current_IP;
	private String aee_deployed_apps_dir;
	//private String aee_stats_dir;
	
	public static IRunner getInstance(String propertiesFile){
		if(runnerInstance == null)runnerInstance = new Runner(propertiesFile);
		return runnerInstance;
	}
	
	public static IRunner getInstance(){
		if(runnerInstance == null)runnerInstance = new Runner(Aee.getPropertiesFile());
		return runnerInstance;
	}
	
	private Runner(String propertiesFile){
		/*initialize config params*/
		Properties props = new Properties();
		try(FileInputStream fis = new FileInputStream(propertiesFile);){
			props.load(fis);
//			fis.close();
			aee_current_IP = InetAddress.getLocalHost().getHostAddress();//props.getProperty("aee_current_IP");
			aee_deployed_apps_dir = PaasUtilities.path4OS(props.getProperty("aee_deployed_apps_dir"));
			//aee_stats_dir = props.getProperty("aee_stats_dir");
			
		}catch(Exception e){
			logger.error("loadProperties fail: "+e.getMessage(), e);
			System.err.println("loadProperties fail: "+e.getMessage());
		}
		sandboxes = new Hashtable<>();
		apps = new Hashtable<>();
		usedPorts = new LinkedList<>();
		deployedApps = new LinkedList<>();
		serviceAddresses = new Hashtable<>();
		SandboxHibernateUtil.initSandboxHibernateUtil();
		//check if this instance is replacing a dead one, if so, recover all lost info
		theGoodShepherd();
		logger.info("started on: "+aee_current_IP);
	}
	
	/*getters & setters*/
	public Collection<Sandbox> getSandboxes(){
		return sandboxes.values();
	}
	
	public Sandbox getSandbox(String sID){
		try{
			return sandboxes.get(sID);
		}catch(NullPointerException e){
			return null;
		}
	}
	
	public List<String> getAppSandboxes(String appID){
		try{
			return apps.get(appID);
		}catch(NullPointerException e){
			return null;
		}
	}
	
	/**
	 * Check if there are running sanboxes. Information may not be 100% reliable as some sandboxes
	 * may still be in the list but have to be cleared yet since last scan
	 */
	public Boolean isRunningSandboxes(){
		return !sandboxes.isEmpty();
	}
	
	public Boolean isRunningApp(String appID){
		return apps.containsKey(appID);
	}
	
	public Boolean isRunningSandbox(String sID){
		return sandboxes.containsKey(sID);
	}
	
	/**
	 * Check how many instances are running for appID.
	 * @param appID
	 * @return number of running instances, 0 if none running
	 */
	public int howManyInstancesRunning(String appID){
		if(getAppSandboxes(appID) == null)return 0;
		else return getAppSandboxes(appID).size();
	}
	
	private Boolean addSandbox(Sandbox s){
		try{
			sandboxes.put(s.getSID(), s);
			return true;
		}catch(NullPointerException e){
			return false;
		}
	}
	
	private Boolean addAppSandbox(String appID, String sID){
		try{
			//app already has at least one configured sandbox
			if(apps.containsKey(appID)){
				apps.get(appID).add(sID);
				return true;
			}
			else{
				//first time
				List<String> list = new LinkedList<>();
				list.add(sID);
				apps.put(appID, list);
				return true;
			}
		}catch(Exception e){
			return false;
		}
	}
	
	//location IP:PORT
	public void addServiceAddress(String service, String location){
		try{
			//service already has at least one location registered
			if(serviceAddresses.containsKey(service)){
				serviceAddresses.get(service).add(location);
			}
			else{
				//first time
				List<String> list = new LinkedList<>();
				list.add(location);
				serviceAddresses.put(service, list);
			}
		}catch(Exception e){
			System.out.println("failed to add service address "+service+" "+location);
		}
	}
	
	private Boolean addAppSandbox(String appID, Sandbox s){
		try{
			//app already has at least one configured sandbox
			if(apps.containsKey(appID)){
				apps.get(appID).add(s.getSID());
				return true;
			}
			else{
				//first time
				List<String> list = new LinkedList<>();
				list.add(s.getSID());
				apps.put(appID, list);
				return true;
			}
		}catch(Exception e){
			return false;
		}
	}
	
	private Boolean removeSandbox(Sandbox s){
		try{
			if(new File(s.getConfigDir()).exists() && !PaasUtilities.deleteDir(new File(s.getConfigDir()))){
				logger.fatal("removeSandbox "+s.getSID()+" failed");
				System.err.println("removeSandbox "+s.getSID()+" failed");
				return false;
			}
		}catch(Exception e){
			logger.fatal("removeSandbox "+s.getSID()+" failed: "+e.getMessage(), e);
			System.err.println("removeSandbox "+s.getSID()+" failed: "+e.getMessage());
			return false;
		}
		if(s.getConsolePort()!=null)usedPorts.remove(s.getConsolePort());
		if(s.getClientPort()!=null)usedPorts.remove(s.getClientPort());
		SandboxHibernateUtil.deleteSandbox(s);
		//sandbox is really removed in clearSandboxes with iter.remove to avoid ConcurrentModificationException
		return true;
	}

	private Boolean removeAppSandbox(String appID, String sID){
		try{
			apps.get(appID).remove(sID);
			return true;
		}catch(NullPointerException e){
			return false;
		}
	}
	
	/*logic*/

	private Sandbox configureApp(String appID){
		//create new sandbox for app, if error abort
		Sandbox s = new Sandbox(appID);
		//create app dirs
		if(!createAppDirs(appID, s))return null;
		//unpack app files into dir
		if(!unpackApp(s))return null;
		//create sandbox.ini
		if(!generateSandboxIni(s))return null;
		//edit config.ini properly
		if(!updateConfigIni(s))return null;
		//create logfile
		if(!createAppLogFiles(s))return null;
		//add sandbox to app sandboxes list
		if (!addAppSandbox(appID, s.getSID())){
			logger.error("failed to store configured sandbox for app "+appID);
			return null;
		}
		//add sandbox to sandbox list
		if(!addSandbox(s)){
			logger.error("failed to store configured sandbox for app "+appID);
			return null;
		}
		logger.info("created sandbox "+s.getSID()+" for app "+appID);
		return s;
	}
	
	

	
	/**
	 * Create Sandbox and start the app.
	 * @param appID
	 * @return Sandbox if started successfully, null otherwise
	 */
	public Sandbox startApp(String appID){
		//run the new app
		Sandbox s = configureApp(appID);
		//if sandbox couldn't be created
		if(s == null){
			logger.fatal("failed to start app "+appID+" - no sandbox");
			System.err.println("failed to start app "+appID+" - no sandbox");
			return null;
		}
		else{
			Process p=null;
			List<String> tmpList = new ArrayList<>();
			String dir = s.getConfigDir();
			try(FileInputStream fstream = new FileInputStream(dir+"sandbox.ini");
					DataInputStream in = new DataInputStream(fstream);
					BufferedReader br = new BufferedReader(new InputStreamReader(in));){
				//open sandbox.ini
				String strLine;
				//read line by line
				while ((strLine = br.readLine()) != null)   {
					//store parameters in tmpList to be later transformed in String []
					tmpList.add(strLine);
				}
//				br.close();
//				in.close();
//				fstream.close();
				//minimum 5: java -jar osgi -configuration dir
				//if less, bad sandbox.ini
				if(tmpList.size()<5){
					logger.fatal("bad sandbox.ini in "+dir+" for "+appID);
					System.err.println("bad sandbox.ini in "+dir+" for "+appID);
					return null;
				}
			}catch (Exception e){
				logger.fatal("unable to read JVM configuration from "+dir+"sandbox.ini to launch "+appID);
				System.err.println("unable to read JVM configuration from "+dir+"sandbox.ini to launch "+appID);
				return null;
			}
			//prepare exec cmd argument
			String [] cmd = tmpList.toArray(new String [tmpList.size()]);
			try {
				ProcessBuilder pb = new ProcessBuilder(cmd);
				//set process working directory
				pb.directory(new File(s.getConfigDir()));
				//redirect stderr to errorfile
				pb.redirectError(s.getFErrFile());
				//redirect stdout to logfile
				pb.redirectOutput(s.getFLogFile());
				//start the process and store it
				p = pb.start();
				s.setProcess(p);
				//get PID and store it for (maybe) later, thanks Java for no getPID..
				s.setPID(PaasUtilities.getSandboxPID(s.getSID()));
				//store sandbox in DB for error recovery
				SandboxHibernateUtil.saveSandbox(s);
			} catch (Exception e) {
				logger.fatal("startApp "+appID+" failed: "+e.getMessage(), e);
				System.err.println("startApp "+appID+" failed: "+e.getMessage());
				e.printStackTrace();
				return null;
			}
			logger.info("startApp "+appID+" ok in sandbox "+s.getSID());
			//send app_start message, done here because sending that message when scaling is a challenge
			//default info is appID and IP:PORT
			/*
			 * <app_start>
			 * <appID>VALUE</appID>
			 * <IP>IP</IP>
			 * <clientPort>PORT</clientPort>
			 * <consolePort>PORT</consolePort>
			 * </app_start>
			 */
			Document additionalInfo = PaasUtilities.createBaseXML("app_start");
			PaasUtilities.addXMLnode(additionalInfo, "appID", appID);
			PaasUtilities.addXMLnode(additionalInfo, "IP", aee_current_IP);
			if(s.getClientPort()!=null){
				PaasUtilities.addXMLnode(additionalInfo, "clientPort", s.getClientPort());
				addServiceAddress(appID, aee_current_IP+":"+s.getClientPort());
			}
			PaasUtilities.addXMLnode(additionalInfo, "consolePort", s.getConsolePort());
			//reply has to be broadcasted, replyTo = null
			Messenger.getInstance().sendMessage(PaasMessage.TOPIC_START_APP, PaasMessage.REPLY_APP_START_OK, null, additionalInfo);
			return s;
		}
	}

	private List<Sandbox> killAppInstances(String appID){
		if(!sandboxes.isEmpty()){
			List<Sandbox> toStop = new LinkedList<>();
			for(Sandbox s : getSandboxes()){
				if(s.getAppID().equalsIgnoreCase(appID)) toStop.add(s);
			}
			for(Sandbox s : toStop){
				if(!killWithTelnet(s)){
					if(s.getProcess()!=null)s.getProcess().destroy();
					else PaasUtilities.killPID(s.getPID());
					s.setProcess(null);
					s.setPID(null);
					SandboxHibernateUtil.updateSandbox(s);
				}
			}
			return toStop;
		}
		return null;
	}
	
	/**
	 * Stops all running instances of @param appID. Tries with graceful Telnet firstly, then
	 * uses process.destroy or killPID
	 * @return true if stopped, false if nothing to be stopped
	 */
	public Boolean stopAppAll(String appID){
		List<Sandbox> toRemove = killAppInstances(appID);
		if(toRemove == null)return false;
		for(Sandbox s : toRemove){
			if(!removeAppSandbox(appID, s.getSID())){
				logger.error("failed to remove "+s.getSID()+" from "+appID+" running list");
				System.err.println("failed to remove "+s.getSID()+" from "+appID+" running list");
			}
			logger.info("stopped app "+appID+" instance in "+s.getSID());
		}
		//send stop_all message
		/*
		 * default info is
		 * <app_stop_all>
		 * <appID>VALUE</appID>
		 * <IP>IP</IP> no PORT as every appID on IP has been stopped so purge routing tables
		 * </app_stop_all>
		 */
		Document additionalInfo = PaasUtilities.createBaseXML("app_stop_all");
		PaasUtilities.addXMLnode(additionalInfo, "appID", appID);
		PaasUtilities.addXMLnode(additionalInfo, "IP", aee_current_IP);
		//reply has to be broadcasted, replyTo = null
		Messenger.getInstance().sendMessage(PaasMessage.TOPIC_STOP_APP, PaasMessage.REPLY_APP_STOP_OK, null, additionalInfo);
		return true;
	}
	
	/**
	 * Stop a particular @param sID instance of @param appID. Tries with graceful Telnet firstly, then
	 * uses process.destroy or killPID
	 * @return true if something has stopped, false is no instance could be found
	 */
	public Boolean stopApp(String appID, String sID){
		String clientPort = null;
		String consolePort = null;
		if(!sandboxes.isEmpty()){
			if(!killWithTelnet(getSandbox(sID))){
				if(getSandbox(sID).getProcess()!=null)getSandbox(sID).getProcess().destroy();
				else PaasUtilities.killPID(getSandbox(sID).getPID());
				clientPort = getSandbox(sID).getClientPort();
				consolePort = getSandbox(sID).getConsolePort();
				getSandbox(sID).setProcess(null);
				getSandbox(sID).setPID(null);
				SandboxHibernateUtil.updateSandbox(getSandbox(sID));
			}
			if(!removeAppSandbox(appID, sID))logger.error("failed to remove "+sID+" from "+appID+" running list");
			logger.info("stopped app "+appID+" instance in "+sID);
			//send stop_app message
			/*
			 * default info is
			 * <app_stop>
			 * <appID>VALUE</appID>
			 * <IP>IP</IP>
			 * <clientPort>PORT</clientPort>
			 * <consolePort>PORT</consolePort>
			 * </app_stop>
			 */
			Document additionalInfo = PaasUtilities.createBaseXML("app_stop");
			PaasUtilities.addXMLnode(additionalInfo, "appID", appID);
			PaasUtilities.addXMLnode(additionalInfo, "IP", aee_current_IP);
			if(clientPort!=null)PaasUtilities.addXMLnode(additionalInfo, "clientPort", clientPort);
			PaasUtilities.addXMLnode(additionalInfo, "consolePort", consolePort);
			//reply has to be broadcasted, replyTo = null
			Messenger.getInstance().sendMessage(PaasMessage.TOPIC_STOP_APP, PaasMessage.REPLY_APP_STOP_OK, null, additionalInfo);
			return true;
		}
		return false;
	}

	/**
	 * Scales app appID up or down to instances
	 * @param appID
	 * @param instances the new instances number
	 * @return true if successfully scaled, false is something went wrong
	 */
	public Boolean scaleApp(String appID, int instances){
		//minimum instances is 0 (stop all)
		if(instances<0){
			logger.warn("scaleApp "+appID+" bad instance value "+instances);
			return false;
		}
		//if 0 stop all
		else if(instances==0){
			if(isRunningApp(appID)){
				stopAppAll(appID);
				return true;
			}
			return false;
		}
		//else scale it
		else{
			int n = howManyInstancesRunning(appID);
			//scale down
			if(instances == n)return false;
			else if(instances < n){
				for(int i=0; i<=instances; i++){
					//TODO ferma istanze con un criterio decente
					stopApp(appID, getAppSandboxes(appID).get(0));
				}
				logger.info("scaleApp "+appID+" to "+instances+" instances");
				return true;
			}
			//scale up
			else{
				//scale up to instances total
				int number = instances-n;
				for(int i=0; i<number; i++){
					startApp(appID);
				}
				logger.info("scaleApp "+appID+" to "+instances+" instances");
				return true;
			}
		}
	}
	
	//check if runner is still alive
	public Boolean isAlive(){
		return !(runnerInstance == null);
	}
	
	private boolean createAppDirs(String appID, Sandbox s){
		String dir = aee_deployed_apps_dir+appID+File.separator+s.getSID()+File.separator;
		s.setConfigDir(dir);
		Path path = Paths.get(dir, "");
		if(!Files.exists(path, LinkOption.NOFOLLOW_LINKS)){
			if(!new File(dir).mkdirs()){
				System.err.println("failed to create dir "+dir+" for sandbox configuration");
				logger.fatal("failed to create dir "+dir+" for sandbox configuration");
				return false;
			}
		}
		String logDir = aee_deployed_apps_dir+appID+File.separator+"logs"+File.separator;
		path = Paths.get(logDir, "");
		if(!Files.exists(path, LinkOption.NOFOLLOW_LINKS)){
			if(!new File(logDir).mkdirs()){
				System.err.println("failed to create log dir "+logDir+" for sandbox configuration");
				logger.fatal("failed to create log dir "+logDir+" for sandbox configuration");
				return false;
			}
		}
		return true;
	}
	
	private Boolean createAppLogFiles(Sandbox s){
		File logFile = new File(s.getConfigDir()+".."+File.separator+"logs"+File.separator+s.getAppID()+"_"+s.getSID()+".log");
		try{
			logFile.createNewFile();
		}catch(Exception e){
			System.err.println("failed to create logfile "+logFile);
			logger.fatal("failed to create logfile "+logFile+" error "+e.getMessage(), e);
			return false;
		}
		File errFile = new File(s.getConfigDir()+".."+File.separator+"logs"+File.separator+s.getAppID()+"_"+s.getSID()+".err");
		try{
			errFile.createNewFile();
		}catch(Exception e){
			System.err.println("failed to create logfile "+errFile);
			logger.fatal("failed to create logfile "+errFile+" error "+e.getMessage(), e);
			return false;
		}
		s.setFLogFile(logFile);
		s.setFErrFile(errFile);
		return true;
	}

	private boolean generateSandboxIni(Sandbox s){
		String dir = s.getConfigDir();
		String file = dir+"sandbox.ini";
		String manifest = dir+"META-INF"+File.separator+"MANIFEST.MF";
		try(FileOutputStream out = new FileOutputStream(file);
	        PrintStream p = new PrintStream(out);){ 
	        p.println ("java");
	        //if I'm at least a provider
	        if(isProvider(manifest)){
	        	p.println("-Dzoodiscovery.dataDir=.");
	        	String clientPort = findFreePort();
	        	if(clientPort == null){
//	        		p.close();
//	       	        out.close();
	       	        return false;
	        	}
	        	p.println("-Dzoodiscovery.clientPort="+clientPort);
	        	s.setClientPort(clientPort);
	        }
	        //else I'm at least a consumer
	        if(isConsumer(manifest)){
	        	p.println("-Dzoodiscovery.autoStart=true");
	        	p.print("-Dzoodiscovery.flavor=zoodiscovery.flavor.standalone=");
	        	//pattern is IP1:PORT1, ..., IPN:PORTN\n
	        	//check local knowledge
	        	List<String> services = lookupServiceAddresses(manifest);
	        	if(services == null)return false;
	        	int size = services.size();
	        	for(int i=0; i<size; i++){
	        		p.print(services.get(i));
	        		if(i == size-1)p.println();
	        		else p.print(", ");
	        	}
	        }
	        //Anyway I need this...
	        p.println("-jar");
	        p.println("."+File.separator+"plugins"+File.separator+s.getSID()+".jar");
        	p.println("-consoleLog");
        	//p.println("-clean");
        	p.println("-configuration");
        	p.println("."+File.separator+"configuration");
//	        p.close();
//	        out.close();
	    }catch (Exception e){
            logger.fatal("failed to write to "+file);
            return false;
	    }
		return true;
	}
	
	private boolean updateConfigIni(Sandbox s){
		String dir = s.getConfigDir();
		String file = dir+"configuration"+File.separator+"config.ini";
		//open config.ini
		try(FileWriter fstream = new FileWriter(file,true);
			BufferedWriter out = new BufferedWriter(fstream);){
			//store osgi.console=port
			out.newLine();
			String consolePort = findFreePort();
			if(consolePort == null){
				out.close();
				fstream.close();
				return false;
			}
			out.write("osgi.console="+consolePort);
//			out.close();
//			fstream.close();
			//save port info in sandbox
			s.setConsolePort(consolePort);
		}catch (Exception e){
			logger.fatal("unable to update "+file);
			return false;
		}
		return true;
	}
	
	private boolean unpackApp(Sandbox s){
		if(!PaasUtilities.unZip(aee_deployed_apps_dir+s.getAppID()+File.separator+s.getAppID()+".zip", s.getConfigDir())){
			return false;
		}
		//rename osgi executable to sandboxname to be later found in processes with jps to get damn PID
		File file = new File(s.getConfigDir()+"plugins"+File.separator+"org.eclipse.osgi.jar");
		File file2 = new File(s.getConfigDir()+"plugins"+File.separator+s.getSID()+".jar");
		if (!file.renameTo(file2)) {
			System.err.println("unable to rename "+file.getPath()+" to "+file.getPath());
		    return false;
		}
		return true;
	}
	
	/* open MANIFEST.MF and search for Require-Bundle
	 * pattern: name1,...,nameN
	 * name;bundle-version="x.y.z" optional
	 * the Exception is catched but should never be thrown
	 * */
	private List<String> findRequiredBundles(String manifest){
		List<String> required = null;
		Properties props = new Properties();
		try(FileInputStream fis = new FileInputStream(manifest); ){
			props.load(fis);
			//fis.close();
			System.out.println("Opening manifest: "+manifest);
			String bundles = props.getProperty("Require-Bundle");//PaasUtilities.badString2Hashtable(manifest).get("Require-Bundle");//PaasUtilities.XML2Hashtable(PaasUtilities.string2XML(manifest)).get("Require-Bundle");//props.getProperty("Require-Bundle");
			//if no bundle is required or key is not found, I'm a provider
			if(bundles==null)return null;
			//else I'm at least a consumer
			String [] list = bundles.split("\\,");
			required = new LinkedList<>();
			for(int i=0; i<list.length; i++){
				required.add(list[i]);
			}
		}catch(Exception e){
			logger.fatal("error while reading "+manifest+": the impossible happened");
		}
		return required;
	}
	
	private Boolean isProvider(String manifest){
		Properties props = new Properties();
		try(FileInputStream fis  = new FileInputStream(manifest);){
			props.load(fis);
			//fis.close();
			String bundles = props.getProperty("Bundle-Description");
			//if key is not found, I'm a consumer
			if(bundles==null)return false;
		}catch(Exception e){
			logger.fatal("error while reading "+manifest+": the impossible happened");
		}
		//else I'm at least a provider
		return true;
	}
	
	private Boolean isConsumer(String manifest){
		Properties props = new Properties();
		try(FileInputStream fis = new FileInputStream(manifest);){
			props.load(fis);
			//fis.close();
			String bundles = props.getProperty("Require-Bundle");
			//if key is not found, I'm a provider
			if(bundles==null)return false;
		}catch(Exception e){
			logger.fatal("erro while reading "+manifest+": the impossible happened");
		}
		//else I'm at least a consumer
		return true;
	}
	
	//try to retrieve missing service addresses
	public void requestServiceAddresses(String manifest){
		List<String> required = findRequiredBundles(manifest);
		if(required!=null){
			for(String str : required){
				//if I'm missing the service address, request it
				if(!hasServiceAddress(str))Aee.getInstance().getMessenger().requestServiceAddress(str);
			}
		}
	}
	
	private List<String> lookupServiceAddresses(String manifest){
		List<String> required = findRequiredBundles(manifest);
		if(required!=null){
			List<String> URLs = new LinkedList<>();
			for(String str : required){
				System.out.println("SERVICEADDRESS SIZE "+serviceAddresses.size());
				System.out.println("SERVICE CONTENT per "+str+" ha "+serviceAddresses.get(str).size());
				URLs = PaasUtilities.mergeNoDuplicates(URLs, serviceAddresses.get(str));
			}
			return URLs;
		}
		return null;
	}
	
	private Boolean killWithTelnet(Sandbox s){
	    String port = s.getConsolePort();
	    try(Socket telnetSocket = new Socket(aee_current_IP, Integer.parseInt(port));
	        PrintWriter out = new PrintWriter(telnetSocket.getOutputStream(), true);) {
	    	//shutdown gracefully via Telnet
	        out.println("close");
//	    	out.close();
//	    	telnetSocket.close();
	    	try{
	    		s.getProcess().exitValue();
	    		s.setProcess(null);
	    		s.setPID(null);
	    		SandboxHibernateUtil.updateSandbox(s);
	    		return true;
	    	}catch(Exception e){
	    		//failed to kill with telnet
	    		return false;
	    	}
	    } catch (Exception e) {
	        System.err.println("cannot connect to host "+aee_current_IP+":"+port);
	        return false;
	    }
	}
	
	private void theGoodShepherd(){
		//check if I'm replacing a dead Runner instance
		if(SandboxHibernateUtil.isDead()){
			List<Sandbox> lostSheeps = SandboxHibernateUtil.getSandboxes();
			Iterator<Sandbox> iter = lostSheeps.iterator();
			while (iter.hasNext()) {
				Sandbox current = iter.next();
				//if so, recover all lost info
				if(current.getPID()!=null && current.getConsolePort()!=null){
					if(isAppAlive(current.getConsolePort(), current.getAppID())){
						addSandbox(current);
						addAppSandbox(current.getAppID(), current);
						usedPorts.add(current.getConsolePort());
						deployedApps.add(current.getAppID());
					}
					//else I don't need it anyway
					//inform rrs that app is dead
					else{
						Document additionalInfo = PaasUtilities.createBaseXML("app_stop");
						PaasUtilities.addXMLnode(additionalInfo, "appID", current.getAppID());
						PaasUtilities.addXMLnode(additionalInfo, "IP", aee_current_IP);
						if(current.getClientPort()!=null)PaasUtilities.addXMLnode(additionalInfo, "port", current.getConsolePort());
						Messenger.getInstance().sendMessage(PaasMessage.TOPIC_STOP_APP, PaasMessage.REPLY_APP_STOP_OK, null, additionalInfo);
						removeSandbox(current);
					}
				}
			}
		}
	}
	
	public Boolean isAppAlive(String consolePort, String appID){
		try(Socket telnetSocket = new Socket(aee_current_IP, Integer.parseInt(consolePort));
		        PrintWriter out = new PrintWriter(telnetSocket.getOutputStream(), true);
				InputStreamReader isr = new InputStreamReader(telnetSocket.getInputStream());
				BufferedReader br = new BufferedReader(isr);) {
		    	//check if app is running ok
		        out.println("activeApps");
//		    	out.close();
//		    	telnetSocket.close();
		        //skip first line since it's empty
		        br.readLine();
		        //read real response
		        String response = br.readLine();
		        //app_package.app_name.number [status]
		        //TODO metti la riga sotto che ora ovviamente non va perche' nome app non e' lo stesso!!!
		    	//if(response.toLowerCase().contains(appID) && response.toLowerCase().contains("running"))return true;
		        if(response.toLowerCase().contains("running"))return true;
		    	return false;
		    } catch (Exception e) {
		        System.err.println("cannot connect to host "+aee_current_IP+":"+consolePort);
		        return false;
		    }
	}
	
	public void clearSandboxes(){
		if(!sandboxes.isEmpty()){
			//avoid ConcurrentModificationException
			Iterator<Sandbox> iter = getSandboxes().iterator();
			while (iter.hasNext()) {
				Sandbox current = iter.next();
			    if ((current.getPID() == null) && (current.getProcess() == null)){
			    	removeSandbox(current);
			    	iter.remove();
			    }
			}
		}
	}
	
	private String findFreePort(){
		String port= PaasUtilities.findFreePort();
		if(port == null)return null;
		if(usedPorts.contains(Integer.parseInt(port)))return findFreePort();
		//if free, reserve it
		usedPorts.add(port);
		return port;
	}
	
	public Boolean deployApp(String location, String appID){
		//String appID = PaasUtilities.lastArrayElement(location.split("/"));
		String appDir = aee_deployed_apps_dir+appID+"/";
		//create dir to deploy app into
		Path path = Paths.get(appDir, "");
		if(!Files.exists(path, LinkOption.NOFOLLOW_LINKS)){
			if(!new File(appDir).mkdirs()){
				System.err.println("failed to create dir "+appDir+" to deploy app into");
				logger.fatal("failed to create dir "+appDir+" to deploy app into");
				return false;
			}
			if(!PaasUtilities.copyFileFromURL(location, appDir+appID+".zip", Aee.getInstance().getStorageUser(), Aee.getInstance().getStoragePwd()))return false;
			deployedApps.add(appID);
		}
		return true;
	}
	
	public Boolean isAppDeployed(String appID){
		return deployedApps.contains(appID);
	}

	public Hashtable<String, List<String>> getServiceAddresses() {
		return serviceAddresses;
	}

	public void removeServiceAddress(String service, String location) {
		if(serviceAddresses.containsKey(service))serviceAddresses.get(service).remove(location);
	}

	public void removeService(String service) {
		serviceAddresses.remove(service);
	}

	public List<String> getServiceAddress(String service) {
		return serviceAddresses.get(service);
	}

	public void addServiceAddress(Document message) {
		Hashtable<String, String> table = PaasUtilities.XML2Hashtable(message);
		//IP1:clientPORT1:consolePORT1;...;IPN:clientPORTN:consolePORTN
		String [] splitted = table.get("addresses").split(";");
		for(int i=0; i< splitted.length; i++){
			String [] address = splitted[i].split(":");
			addServiceAddress(table.get("service"), address[0]+":"+address[1]);
		}
	}
	
	public Boolean hasServiceAddress(String service){
		return serviceAddresses.containsKey(service);
	}
	
}
