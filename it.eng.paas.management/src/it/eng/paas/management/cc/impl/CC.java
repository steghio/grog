package it.eng.paas.management.cc.impl;

import it.eng.paas.management.messenger.IControlSurfaceListener;
import it.eng.paas.management.messenger.IMessenger;
import it.eng.paas.management.messenger.impl.ControlSurfaceListener;
import it.eng.paas.management.messenger.impl.Messenger;
import it.eng.paas.management.messenger.IP2PMessenger;
import it.eng.paas.management.messenger.impl.P2PMessenger;
import it.eng.paas.message.PaasMessage;
import it.eng.paas.module.PaasModule;
import it.eng.paas.utilities.PaasUtilities;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import rice.environment.Environment;
import rice.pastry.NodeIdFactory;
import rice.pastry.PastryNode;
import rice.pastry.PastryNodeFactory;
import rice.pastry.socket.SocketPastryNodeFactory;
import rice.pastry.standard.IPNodeIdFactory;

public class CC implements Runnable{

	private static String propertiesFile;// = "C:\\temp\\junit\\cc.properties";
	private static Logger logger = null;
	
	private static int cc_app_deploy_retries;
	private static int cc_cpu_deploy_percentage;
	private static int cc_ram_deploy_percentage;
	private static int cc_listener_port;
	private static String cc_DB_user;
	private static String cc_DB_pwd;
	private static String cc_DB_URL;
	
	private static CC ccInstance = null;
	private List<Document> aeeStartMessages;
	private List<Document> rrStartMessages;
	private List<Document> ccStartMessages;
	private static List<PaasModule> modulesToStart;
	private Hashtable<String, Integer> appToDeploy;
	private Hashtable<String, List<Document>> appAlreadyPresentReplies;
	private Hashtable<String, List<Document>> appAcceptableReplies;
	
	/*submodules*/
	private static IMessenger messenger;
	private static IP2PMessenger p2p;
	private static IControlSurfaceListener csl;
	
	/*config params*/
	private static String cc_logs_dir;
	private static String cc_current_IP;
	
	public static CC getInstance(String propertiesFile){
		if(ccInstance == null)ccInstance = new CC(propertiesFile);
		return ccInstance;
	}
	
	public static CC getInstance(){
		if(ccInstance == null)ccInstance = new CC(getPropertiesFile());
		return ccInstance;
	}
	
	public static String getPropertiesFile(){
		return propertiesFile;
	}
	
	public IMessenger getMessenger(){
		return messenger;
	}
	
	private CC(String propertiesFile){
		modulesToStart = new LinkedList<>();
		//I need all these modules
		modulesToStart.add(PaasModule.AEE);
		modulesToStart.add(PaasModule.RR);
		modulesToStart.add(PaasModule.PO);
		/*initialize config params*/
		loadProperties();
		/*initialize modules*/
		loadModules();
		logger.info("started on "+cc_current_IP);
		aeeStartMessages = new LinkedList<>();
		rrStartMessages = new LinkedList<>();
		ccStartMessages = new LinkedList<>();
		appToDeploy = new Hashtable<>();
		appAlreadyPresentReplies = new Hashtable<>();
		appAcceptableReplies = new Hashtable<>();
		ManagementHibernateUtil.initManagementHibernateUtil();
	}
	
	private static void loadModules(){
		messenger = Messenger.getInstance(propertiesFile);
	}
	
	private static void loadProperties(){
		Properties props = new Properties();
		try(FileInputStream fis = new FileInputStream(propertiesFile);){
			props.load(fis);
			//fis.close();
			cc_logs_dir = props.getProperty("cc_logs_dir");
			System.setProperty("rootPath", cc_logs_dir);
			cc_current_IP = InetAddress.getLocalHost().getHostAddress();//props.getProperty("cc_current_IP");
			cc_app_deploy_retries = Integer.parseInt(props.getProperty("cc_app_deploy_retries"));
			cc_cpu_deploy_percentage = Integer.parseInt(props.getProperty("cc_cpu_deploy_percentage"));
			cc_ram_deploy_percentage = Integer.parseInt(props.getProperty("cc_ram_deploy_percentage"));
			cc_listener_port = Integer.parseInt(props.getProperty("cc_listener_port"));
			cc_DB_user = props.getProperty("cc_DB_user");
			cc_DB_pwd = props.getProperty("cc_DB_pwd");
			cc_DB_URL = props.getProperty("cc_DB_URL");
			//if no PO exist, start a new one
			String cc_local_po_address = props.getProperty("cc_local_po_address");
			if(cc_local_po_address == null || cc_local_po_address.equals("")){
				//no properties file needed to start PO
				Runner r  = new Runner(null);
				if(!r.startModule(PaasModule.PO)){
					//TODO qualcosa e' andato storto
				}
				else{
					//TODO scrivi indirizzo PO in propertiesFile
				}
			}
			//remove PO from toStart list
			else modulesToStart.remove(PaasModule.PO);
			p2p=null;
			logger = Logger.getLogger("CC");
			//cc_known_ccs are IP1:PORT1;...;IPN:PORTN
			joinCCs(props.getProperty("cc_known_ccs"));
		}catch(Exception e){
			System.err.println("unable to load properties file "+propertiesFile);
		}
	}
	
	private static void joinCCs(String known_ccs) {
		Environment env = new Environment();
		//TODO controlla se serve o meno sotto
	    // disable the UPnP setting (in case you are testing this on a NATted LAN)
	    env.getParameters().setString("nat_search_policy","never");
	    ///////////////////
	    int bindport = 0;
	    InetAddress bootaddr = null;
	    InetSocketAddress bootaddress = null;
	    // the port to use locally
    	bindport = Integer.parseInt(PaasUtilities.findFreePort());  
		//if I'm the first node, start a new network
		if(known_ccs == null || known_ccs.equals("")){
	    	//launch is localPort, bootstrapNode, bootstrapNodePort
	    	//if I'm creating network localPort = bootstrapNodePort
	    	try {
	    		System.out.println("First cc P2P node starting up on: "+cc_current_IP+":"+bindport);
	    		logger.info("First cc P2P node starting up on: "+cc_current_IP+":"+bindport);
	    		//TODO usa getByAddress(byte [] ip) NON usare hosts!! prendi IP diretto
	    		bootaddr = InetAddress.getByName(cc_current_IP);
	    		bootaddress = new InetSocketAddress(bootaddr,bindport);
	    		// launch our node!
	    		joinOrCreateNetwork(bindport, bootaddress, env);
	    	} catch (Exception e) {
	    		System.out.println("cc error resolving addresses, bootaddr: "+bootaddr+" bootport: "+bindport+" error: "+e.getMessage());
	    		logger.fatal("cc error while resolving addresses, bootaddr: "+bootaddr+" bootport: "+bindport+" error: "+e.getMessage(), e);
	    		e.printStackTrace();
	    	}
		}
		//else join the existing network by contacting one node in it
		else{
			//format is IP1:PORT1;...;IPN:PORTN
			String [] ccs = known_ccs.split(";");
			try{
				int i=0;
				String [] cc;
				//if fail, try with another known node
				while(true){
					cc = ccs[i].split(":");
					bootaddr = InetAddress.getByName(cc[0]);
					bootaddress = new InetSocketAddress(bootaddr, Integer.parseInt(cc[1]));
					System.out.println("Trying to joing network of "+bootaddress);
					logger.info("Trying to joing network of "+bootaddress);
					if(joinOrCreateNetwork(bindport, bootaddress, env))break;
					i++;
				}
			}catch(Exception e){
				System.out.println("cc error resolving addresses, bootaddr: "+bootaddr+" bootport: "+bindport+" error: "+e.getMessage());
	    		logger.fatal("cc error while resolving addresses, bootaddr: "+bootaddr+" bootport: "+bindport+" error: "+e.getMessage(), e);
	    		e.printStackTrace();
			}
		}
	}
	
	private static Boolean joinOrCreateNetwork(int bindport, InetSocketAddress bootaddress, Environment env){
	    // Generate the NodeIds from my current IP
	    NodeIdFactory nidFactory;
		try {
			nidFactory = new IPNodeIdFactory(InetAddress.getByName(cc_current_IP), bindport, env);
		} catch (Exception e) {
			System.out.println("cc error while creating node ID: "+e.getMessage());
    		logger.fatal("cc error while creating node ID: "+e.getMessage(), e);
			e.printStackTrace();
			return false;
		}
	    
	    // construct the PastryNodeFactory, this is how we use rice.pastry.socket
	    PastryNodeFactory factory = null;
		try {
			factory = new SocketPastryNodeFactory(nidFactory, bindport, env);
		} catch (Exception e) {
			System.out.println("cc error while creating factory: "+e.getMessage());
    		logger.fatal("cc error while creating factory: "+e.getMessage(), e);
    		e.printStackTrace();
    		return false;
		}

	    // construct a node
	    PastryNode node = null;
		try {
			node = factory.newNode();
		} catch (Exception e) {
			System.out.println("cc error while creating node: "+e.getMessage());
    		logger.fatal("cc error while creating node: "+e.getMessage(), e);
    		e.printStackTrace();
    		return false;
		}
	      
	    // construct a new P2PMessenger app
	    p2p  = new P2PMessenger(node);    
	    
	    node.boot(bootaddress);
	    
	    // the node may require sending several messages to fully boot into the ring
	    synchronized(node) {
	      while(!node.isReady() && !node.joinFailed()) {
	        // delay so we don't busy-wait
	        try {
				node.wait(500);
	        } catch (Exception e) {
				System.out.println("cc error while joining network: "+e.getMessage());
	    		logger.fatal("cc error while joining network: "+e.getMessage(), e);
	    		e.printStackTrace();
	    		return false;
			}
	        
	        // abort if can't join
	        if (node.joinFailed()) {
				System.out.println("cc failed to join network: "+node.joinFailedReason());
	    		logger.fatal("cc failed to join network: "+node.joinFailedReason());
	    		return false;
	        }
	      }       
	    }
	    //subscribe for broadcasting
	    p2p.subscribe();
	    System.out.println("STARTED CC P2P on "+cc_current_IP);
	    logger.info("STARTED CC P2P on "+cc_current_IP);
	    return true;
	  }
	
	public Boolean isAlive(){
		return !(ccInstance==null);
	}
	
//	private static void sendHeartbeat(){
//		messenger.sendHeartBeat();
//	}
	
	public Boolean hasAeeStartMessages(){
		return !aeeStartMessages.isEmpty();
	}
	
	public void addAeeStartMessage(Document message){
		aeeStartMessages.add(message);
	}
	
	public Boolean hasCCStartMessages(){
		return !ccStartMessages.isEmpty();
	}
	
	public void addCCStartMessage(Document message){
		ccStartMessages.add(message);
	}
	
	public Boolean hasRRStartMessages(){
		return !rrStartMessages.isEmpty();
	}
	
	public void addRRStartMessage(Document message){
		rrStartMessages.add(message);
	}
	
	public Boolean hasDeployAppReplies(){
		return !appAlreadyPresentReplies.isEmpty() || !appAcceptableReplies.isEmpty();
	}
	
	public void addAppAlreadyPresentReply(Document message){
		String appID = PaasUtilities.XML2Hashtable(message).get("appID");
		//if already there, add message
		if(appAlreadyPresentReplies.containsKey(appID)){
			appAlreadyPresentReplies.get(appID).add(message);
		}
		//else put first message
		else{
			List<Document> messages = new LinkedList<>();
			messages.add(message);
			appAlreadyPresentReplies.put(appID, messages);
		}
	}
	
	public void addAppAcceptableReply(Document message){
		String appID = PaasUtilities.XML2Hashtable(message).get("appID");
		if(appAcceptableReplies.containsKey(appID)){
			appAcceptableReplies.get(appID).add(message);
		}
		//else put first message
		else{
			List<Document> messages = new LinkedList<>();
			messages.add(message);
			appAcceptableReplies.put(appID, messages);
		}
	}
	
	public void run() {
		while(true){
			//if(csl==null)ControlSurfaceListener.getInstance();
			try{
				Thread.sleep(5000);
			}catch(Exception e){
				logger.fatal("thread interrupted unexpectedly "+e.getMessage(), e);
				System.err.println("error "+e.getMessage());
			}
			if(!messenger.isAlive())messenger = Messenger.getInstance(propertiesFile);
			//CC.sendHeartbeat();
			readMessages();
			try{
				Thread.sleep(5000);
			}catch(Exception e){
				logger.fatal("thread interrupted unexpectedly "+e.getMessage(), e);
				System.err.println("error "+e.getMessage());
			}
			decideDeploy();
			try{
				Thread.sleep(5000);
			}catch(Exception e){
				logger.fatal("thread interrupted unexpectedly "+e.getMessage(), e);
				System.err.println("error "+e.getMessage());
			}
			checkModulesToStart();
		}
	}

	private void checkModulesToStart() {
		if(CC.hasModulesToStart()){
			Iterator<PaasModule> iter = modulesToStart.iterator();
			while(iter.hasNext()){
				PaasModule m = (PaasModule)iter.next();
				switch(m){
				//false start local, true start remote
					case AEE:{
						//start local
						if(!decideModuleStart(m)){
							//populate properties file
							Hashtable<String, String> properties = new Hashtable<>();
							Properties props = new Properties();
							try(FileInputStream fis = new FileInputStream(propertiesFile);){
								props.load(fis);
								//fis.close();
								properties.put("aee_controller_recovery_retries", props.getProperty("aee_controller_recovery_retries"));
								properties.put("aee_deployed_apps_dir", props.getProperty("aee_deployed_apps_dir"));
								properties.put("aee_logs_dir", props.getProperty("aee_logs_dir"));
								properties.put("aee_local_po_address", Messenger.getLocalPOAddress());
								properties.put("aee_storage_pwd", props.getProperty("aee_storage_pwd"));
								properties.put("aee_storage_user", props.getProperty("aee_storage_user"));
								properties.put("aee_tenant_setup", props.getProperty("aee_tenant_setup"));
								properties.put("aee_DB_user", props.getProperty("aee_DB_user"));
								properties.put("aee_DB_pwd", props.getProperty("aee_DB_pwd"));
								properties.put("aee_DB_URL", props.getProperty("aee_DB_URL"));
							}catch(Exception e){
								System.err.println("unable to load properties file "+propertiesFile);
								break;
							}
							Runner r = new Runner(properties);
							if(!r.startModule(m)){
								//TODO qualcosa è andato storto
							}
							else iter.remove();
						}
						//start remote
						else{
							//TODO fai partire macchina e poi contattala al suo IP:9999 per dare comando avvia modulo
						}
						break;
					}
					case CC:{
						//start local
						if(!decideModuleStart(m)){
							//populate properties file
							Hashtable<String, String> properties = new Hashtable<>();
							Properties props = new Properties();
							try(FileInputStream fis = new FileInputStream(propertiesFile);){
								props.load(fis);
								//fis.close();
								properties.put("cc_app_deploy_retries", String.valueOf(cc_app_deploy_retries));
								properties.put("cc_logs_dir", cc_logs_dir);
								properties.put("cc_cpu_deploy_percentage", String.valueOf(cc_cpu_deploy_percentage));
								properties.put("cc_ram_deploy_percentage", String.valueOf(cc_ram_deploy_percentage));
								properties.put("cc_local_po_address", Messenger.getLocalPOAddress());
								properties.put("cc_listener_port", String.valueOf(cc_listener_port));
								properties.put("cc_DB_user", cc_DB_user);
								properties.put("cc_DB_pwd", cc_DB_pwd);
								properties.put("cc_DB_URL", cc_DB_URL);
								//get all known ccs and pass them to new CC to join P2P network
								List<CCInfo> ccs = ManagementHibernateUtil.getCCs();
								String cc_known_ccs = null;
								for(CCInfo c : ccs){
									cc_known_ccs+=c.getIp()+";";
								}
								properties.put("cc_known_ccs", cc_known_ccs);
							}catch(Exception e){
								System.err.println("unable to load properties file "+propertiesFile);
								break;
							}
							Runner r = new Runner(properties);
							if(!r.startModule(m)){
								//TODO qualcosa è andato storto
							}
							else iter.remove();
						}
						//start remote
						else{
							//TODO fai partire macchina e poi contattala al suo IP:9999 per dare comando avvia modulo
						}
						break;
					}
					case RR:{
						//start local
						if(!decideModuleStart(m)){
							//populate properties file
							Hashtable<String, String> properties = new Hashtable<>();
							Properties props = new Properties();
							try(FileInputStream fis = new FileInputStream(propertiesFile);){
								props.load(fis);
								//fis.close();
								properties.put("rr_logs_dir", props.getProperty("rr_logs_dir"));
								properties.put("rr_local_po_address", Messenger.getLocalPOAddress());
								properties.put("rr_DB_user", props.getProperty("rr_DB_user"));
								properties.put("rr_DB_pwd", props.getProperty("rr_DB_pwd"));
								properties.put("rr_DB_URL", props.getProperty("rr_DB_URL"));
							}catch(Exception e){
								System.err.println("unable to load properties file "+propertiesFile);
								break;
							}
							Runner r = new Runner(properties);
							if(!r.startModule(m)){
								//TODO qualcosa è andato storto
							}
							else iter.remove();
						}
						//start remote
						else{
							//TODO fai partire macchina e poi contattala al suo IP:9999 per dare comando avvia modulo
						}
						break;
					}
					case PO:{
						//start local
						if(!decideModuleStart(m)){
							Runner r = new Runner(null);
							if(!r.startModule(m)){
								//TODO qualcosa è andato storto
							}
							else iter.remove();
						}
						//start remote
						else{
							//TODO fai partire macchina e poi contattala al suo IP:9999 per dare comando avvia modulo
						}
						break;
					}
					default:{
						//should never get here
						System.out.println("Bad request module:"+m.getText());
						logger.error("Bad request module:"+m.getText());
						//remove bad module
						iter.remove();
					}
				}
			}
		}
	}
	
	//false start local, true start remote
	private boolean decideModuleStart(PaasModule module){
		int cpu = PaasUtilities.getCPUUsage();
		double ram = PaasUtilities.getRAMUsage();
		//if current CPU OR RAM usage > deploy values, start a new host else deploy locally
		switch(module){
		//TODO usa valori personalizzati a seconda del modulo
			case AEE:{
				if(cpu>cc_cpu_deploy_percentage || ram > cc_ram_deploy_percentage){
					return true;
				}
				return false;
			}
			case CC:{
				if(cpu>cc_cpu_deploy_percentage || ram > cc_ram_deploy_percentage){
					return true;
				}
				return false;
			}
			case RR:{
				if(cpu>cc_cpu_deploy_percentage || ram > cc_ram_deploy_percentage){
					return true;
				}
				return false;
			}
			case PO:{
				if(cpu>cc_cpu_deploy_percentage || ram > cc_ram_deploy_percentage){
					return true;
				}
				return false;
			}
			default:{
				//should never get here
				System.out.println("Bad request module:"+module.getText());
				logger.error("Bad request module:"+module.getText());
			}
		}
		return false;
	}
	
	public Boolean hasAppToDeploy(){
		return !appToDeploy.isEmpty();
	}
	
	//TODO rimetti private!
	public void addAppToDeploy(String appID){
		try{
			//if already existing, I have already tried to deploy it, try again until limit
			if(appToDeploy.containsKey(appID)){
				//over the limit, fail
				if(appToDeploy.get(appID).intValue() > cc_app_deploy_retries){
					logger.fatal("unable to deploy "+appID+", retry attempts done: "+cc_app_deploy_retries);
					System.err.println("unable to deploy "+appID+", retry attempts done: "+cc_app_deploy_retries);
					appToDeploy.remove(appID);
				}
				else appToDeploy.put(appID, appToDeploy.get(appID).intValue()+1);
			}
			else appToDeploy.put(appID, 1);				
		}catch(Exception e){
			logger.error("failed to update appsToDeploy "+e.getMessage(), e);
			System.err.println("failed to update appsToDeploy "+e.getMessage());
		}
	}
	
	private void decideDeploy(){
		if(hasAppToDeploy()){
			Iterator<String> iter = appToDeploy.keySet().iterator();
			while(iter.hasNext()){
				String appID = iter.next();
				List<Document> alreadyPresent = appAlreadyPresentReplies.get(appID);
				List<Document> acceptable = appAcceptableReplies.get(appID);
				//if no aee can take app, delete deploy request after cc_app_deploy_retries
				if(alreadyPresent == null && acceptable == null)addAppToDeploy(appID);
				//else decide who will host it
				else{
					String [] aeeHas = searchList(alreadyPresent);
					String [] aeeAccept = searchList(acceptable);
					//TODO scegli location in modo decente
					String location = ManagementHibernateUtil.getAppLocation(appID).get(0);
					//finally check if it is better to deploy a new one or start another instance
					//if I only had accept app replies
					if(aeeHas[0] == "")sendStartReply(appID, location, aeeHas[0]);
					else{
						//if I only had alreadyPresent replies
						if(aeeAccept[0] == "")sendStartReply(appID, location, aeeAccept[0]);
						//else if I have both
						else{
							//based on deploy percentages, decide whether to deploy or start instance
							//if aeeHas*percentage < aeeAccept, go with Has
							if((Double.parseDouble(aeeHas[1])/100*cc_cpu_deploy_percentage) < Double.parseDouble(aeeAccept[1]))sendStartReply(appID, location, aeeHas[0]);
							//if same, check RAM
							else if((Double.parseDouble(aeeHas[1])/100*cc_cpu_deploy_percentage) == Double.parseDouble(aeeAccept[1])){
								if((Double.parseDouble(aeeHas[2])/100*cc_ram_deploy_percentage) < Double.parseDouble(aeeAccept[2]))sendStartReply(appID, location, aeeHas[0]);
								//if aeeHas with percentage modifiers is worse than redeploy, ask to depoly again
								else sendStartReply(appID, location, aeeAccept[0]);
							}
							//else it is anyway better to redeploy
							else sendStartReply(appID, location, aeeAccept[0]);
						}
					}
					iter.remove();
				}
			}
		}
	}
	
	private String [] searchList(List<Document> list){
		//priority is already has > can accept
		String [] aee = {"", "", ""};
		//if someone already has it
		if(list != null){
			//get first then compare with all others to decide
			//priority is CPU > RAM
			Document tmpdoc = list.get(0);
			Hashtable<String, String> tmp = PaasUtilities.XML2Hashtable(tmpdoc);
			//aee, CPU_usage, RAM_usage
			aee[0] = tmp.get("aee");
			aee[1] = tmp.get("CPU_usage");
			aee[2] = tmp.get("RAM_usage");
			list.remove(tmpdoc);
			for(Document doc : list){
				Hashtable<String, String> data = PaasUtilities.XML2Hashtable(doc);
				//if another aee is using less CPU, give it to her
				if(Integer.parseInt(data.get("CPU_usage")) < Integer.parseInt(aee[1])){
					aee[0] = data.get("aee");
					aee[1] = data.get("CPU_usage");
					aee[2] = data.get("RAM_usage");
				}
				//else if using same CPU, check who is using less RAM
				else if(Integer.parseInt(data.get("CPU_usage")) == Integer.parseInt(aee[1])){
					if(Integer.parseInt(data.get("RAM_usage")) < Integer.parseInt(aee[2])){
						aee[0] = data.get("aee");
						aee[1] = data.get("CPU_usage");
						aee[2] = data.get("RAM_usage");
					}
				}
			}
		}
		return aee;
	}
	
	private void sendStartReply(String appID, String location, String to){
		String replyTo = Messenger.getInstance().getQueueName();
		Document message = PaasUtilities.createBaseXML("start_app");
		PaasUtilities.addXMLnode(message, "appID", appID);
		PaasUtilities.addXMLnode(message, "location", location);
		PaasUtilities.addXMLnode(message, "replyTo", replyTo);
		Messenger.getInstance().sendMessage(PaasMessage.TOPIC_START_APP, PaasMessage.TOPIC_START_APP, to, message);
	}
	
	private void readMessages(){
		readAeeStartMessage();
		readCCStartMessage();
		readRRStartMessage();
	}

	private void readAeeStartMessage() {
		if(hasAeeStartMessages()){
			Iterator<Document> iter = aeeStartMessages.iterator();
			while(iter.hasNext()){
				Document message = iter.next();
				Hashtable<String, String> read = PaasUtilities.XML2Hashtable(message);
				String ip = read.get("IP");
				/*message is
				 * <aee_start>
				 * <IP>IP</IP>
				 * </aee_start>
				 */
				//insert in DB if not already there
				if(ManagementHibernateUtil.getAee(ip)==null){
					ManagementHibernateUtil.saveAeeInfo(new AeeInfo(ip));
				}
				iter.remove();
			}
		}
	}
	
	private void readCCStartMessage() {
		if(hasCCStartMessages()){
			Iterator<Document> iter = ccStartMessages.iterator();
			while(iter.hasNext()){
				Document message = iter.next();
				Hashtable<String, String> read = PaasUtilities.XML2Hashtable(message);
				String ip = read.get("IP");
				/*message is
				 * <cc_start>
				 * <IP>IP</IP>
				 * </cc_start>
				 */
				//insert in DB if I'm not it and it is not already there
				if(!ip.equalsIgnoreCase(cc_current_IP) && ManagementHibernateUtil.getCC(ip)==null){
					ManagementHibernateUtil.saveCCInfo(new CCInfo(ip));
				}
				iter.remove();
			}
		}
	}
	
	private void readRRStartMessage() {
		if(hasRRStartMessages()){
			Iterator<Document> iter = rrStartMessages.iterator();
			while(iter.hasNext()){
				Document message = iter.next();
				Hashtable<String, String> read = PaasUtilities.XML2Hashtable(message);
				String ip = read.get("IP");
				/*message is
				 * <rr_start>
				 * <IP>IP</IP>
				 * </rr_start>
				 */
				//insert in DB if not already there
				if(ManagementHibernateUtil.getRR(ip)==null){
					ManagementHibernateUtil.saveRRInfo(new RRInfo(ip));
				}
				iter.remove();
			}
		}
	}
	
	private void startDaemon(){
		Thread thread = new Thread(this);
		thread.start();
	}
	
	public static void main(String [] args){
		if(args.length != 1){
			System.out.println("Usage cc propertiesFile");
			System.exit(1);
		}
		CC.propertiesFile = args[0];
		CC.ccInstance = CC.getInstance();
		//ask around to know who's there
		Messenger.getInstance().sendMessage(PaasMessage.COMPONENT_WHO_ALIVE, PaasMessage.COMPONENT_WHO_ALIVE, null, null);
		ccInstance.startDaemon();
		csl = ControlSurfaceListener.getInstance(propertiesFile);
	}
	
	public static IP2PMessenger getP2PMessenger(){
		return p2p;
	}
	
	public String getCurrentIP(){
		return cc_current_IP;
	}
	
	public int getListenerPort(){
		return cc_listener_port;
	}

	public static String getCc_DB_user() {
		return cc_DB_user;
	}

	public static String getCc_DB_pwd() {
		return cc_DB_pwd;
	}

	public static String getCc_DB_URL() {
		return cc_DB_URL;
	}
	
	public static List<PaasModule> getModulesToStart(){
		return modulesToStart;
	}
	
	public static boolean hasModulesToStart(){
		return !modulesToStart.isEmpty();
	}

}
