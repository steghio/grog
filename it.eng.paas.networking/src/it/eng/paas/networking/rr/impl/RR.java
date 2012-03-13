package it.eng.paas.networking.rr.impl;

import it.eng.paas.message.PaasMessage;
import it.eng.paas.networking.messenger.IMessenger;
import it.eng.paas.networking.messenger.IP2PMessenger;
import it.eng.paas.networking.messenger.impl.Messenger;
import it.eng.paas.networking.messenger.impl.P2PMessenger;
import it.eng.paas.utilities.PaasUtilities;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
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

public class RR implements Runnable{
	private static RR rrInstance = null;
	private static Logger logger = null; 
	private static IP2PMessenger p2p;
	private static IMessenger messenger;
	private static String rr_logs_dir;
	private List<Document> serviceAddressMessages;
	private List<Document> startAppMessages;
	private List<Document> stopAppMessages;
	
	//args[0] is propertiesFile
	private static String propertiesFile;//="C:\\temp\\junit\\rr.properties";
	
	/*config params*/
	private String rr_current_IP;
	private static String rr_DB_user;
	private static String rr_DB_pwd;
	private static String rr_DB_URL;
	
	public static RR getInstance(String propertiesFile){
		if(rrInstance == null)rrInstance = new RR(propertiesFile);
		return rrInstance;
	}
	
	public static RR getInstance(){
		if(rrInstance == null)rrInstance = new RR(getPropertiesFile());
		return rrInstance;
	}
	
	private RR(String propertiesFile){
		Properties props = new Properties();
		try(FileInputStream fis = new FileInputStream(propertiesFile);){
			props.load(fis);
			//fis.close();
			rr_current_IP = InetAddress.getLocalHost().getHostAddress();//props.getProperty("rr_current_IP");	
			rr_logs_dir = props.getProperty("rr_logs_dir");
			rr_DB_user = props.getProperty("rr_DB_user");
			rr_DB_pwd = props.getProperty("rr_DB_pwd");
			rr_DB_URL = props.getProperty("rr_DB_URL");
			System.setProperty("rootPath", rr_logs_dir);
			logger = Logger.getLogger("RR");
			p2p=null;
			//rr_known_rrs are IP1:PORT1;...;IPN:PORTN
			joinRRs(props.getProperty("rr_known_rrs"));
		}catch(Exception e){
			System.err.println("loadProperties fail: "+e.getMessage());
			logger.error("loadProperties fail: "+e.getMessage(), e);
			e.printStackTrace();
		}
		serviceAddressMessages = new LinkedList<Document>();
		startAppMessages = new LinkedList<Document>();
		stopAppMessages = new LinkedList<Document>();
		messenger = Messenger.getInstance(propertiesFile);
		RoutingInfoHibernateUtil.initRoutingInfoHibernateUtil();
		//check if this instance is replacing a dead one, if so, check all previous info
		theGoodShepherd();
		System.out.println("rrstarted on: "+rr_current_IP);
		logger.info("started on: "+rr_current_IP);
		//inform CCs I spawned
		messenger.notifyRRStart();
		//start polling queue for incoming messages
		startDaemon();
		
	}
	
	
	private void joinRRs(String known_rrs) {
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
		if(known_rrs == null || known_rrs.equals("")){
	    	//launch is localPort, bootstrapNode, bootstrapNodePort
	    	//if I'm creating network localPort = bootstrapNodePort
	    	try {
	    		System.out.println("First rr P2P node starting up on: "+rr_current_IP+":"+bindport);
	    		logger.info("First rr P2P node starting up on: "+rr_current_IP+":"+bindport);
	    		//TODO usa getByAddress(byte [] ip) NON usare hosts!! prendi IP diretto
	    		bootaddr = InetAddress.getByName(rr_current_IP);
	    		bootaddress = new InetSocketAddress(bootaddr,bindport);
	    		// launch our node!
	    		joinOrCreateNetwork(bindport, bootaddress, env);
	    	} catch (Exception e) {
	    		System.out.println("rr error resolving addresses, bootaddr: "+bootaddr+" bootport: "+bindport+" error: "+e.getMessage());
	    		logger.fatal("rr error while resolving addresses, bootaddr: "+bootaddr+" bootport: "+bindport+" error: "+e.getMessage(), e);
	    		e.printStackTrace();
	    	}
		}
		//else join the existing network by contacting one node in it
		else{
			//format is IP1:PORT1;...;IPN:PORTN
			String [] rrs = known_rrs.split(";");
			try{
				int i=0;
				String [] rr;
				//if fail, try with another known node
				while(true){
					rr = rrs[i].split(":");
					bootaddr = InetAddress.getByName(rr[0]);
					bootaddress = new InetSocketAddress(bootaddr, Integer.parseInt(rr[1]));
					System.out.println("Trying to joing network of "+bootaddress);
					logger.info("Trying to joing network of "+bootaddress);
					if(joinOrCreateNetwork(bindport, bootaddress, env))break;
					i++;
				}
			}catch(Exception e){
				System.out.println("rr error resolving addresses, bootaddr: "+bootaddr+" bootport: "+bindport+" error: "+e.getMessage());
	    		logger.fatal("rr error while resolving addresses, bootaddr: "+bootaddr+" bootport: "+bindport+" error: "+e.getMessage(), e);
	    		e.printStackTrace();
			}
		}
	}
	
	private Boolean joinOrCreateNetwork(int bindport, InetSocketAddress bootaddress, Environment env){
	    // Generate the NodeIds from my current IP
	    NodeIdFactory nidFactory;
		try {
			nidFactory = new IPNodeIdFactory(InetAddress.getByName(rr_current_IP), bindport, env);
		} catch (Exception e) {
			System.out.println("rr error while creating node ID: "+e.getMessage());
    		logger.fatal("rr error while creating node ID: "+e.getMessage(), e);
			e.printStackTrace();
			return false;
		}
	    
	    // construct the PastryNodeFactory, this is how we use rice.pastry.socket
	    PastryNodeFactory factory = null;
		try {
			factory = new SocketPastryNodeFactory(nidFactory, bindport, env);
		} catch (Exception e) {
			System.out.println("rr error while creating factory: "+e.getMessage());
    		logger.fatal("rr error while creating factory: "+e.getMessage(), e);
    		e.printStackTrace();
    		return false;
		}

	    // construct a node
	    PastryNode node = null;
		try {
			node = factory.newNode();
		} catch (Exception e) {
			System.out.println("rr error while creating node: "+e.getMessage());
    		logger.fatal("rr error while creating node: "+e.getMessage(), e);
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
				System.out.println("rr error while joining network: "+e.getMessage());
	    		logger.fatal("rr error while joining network: "+e.getMessage(), e);
	    		e.printStackTrace();
	    		return false;
			}
	        
	        // abort if can't join
	        if (node.joinFailed()) {
				System.out.println("rr failed to join network: "+node.joinFailedReason());
	    		logger.fatal("rr failed to join network: "+node.joinFailedReason());
	    		return false;
	        }
	      }       
	    }
	    //subscribe for broadcasting
	    p2p.subscribe();
	    System.out.println("STARTED RR P2P on "+rr_current_IP);
	    logger.info("STARTED RR P2P on "+rr_current_IP);
	    return true;
	  }

	public static Boolean isAlive(){
		return !(rrInstance == null);
	}
	
	public static String getPropertiesFile(){
		return propertiesFile;
	}
	
	public void run() {
		while(true){
			try {
				Thread.sleep(10000);
			} catch (Exception e) {
				System.err.println("rr thread error while sleeping "+e.getMessage());
				logger.error("rr thread error while sleeping "+e.getMessage(), e);
				e.printStackTrace();
			}
			//TODO aggiungi altro se serve
			if(!messenger.isAlive())messenger = Messenger.getInstance(propertiesFile);
			try {
				Thread.sleep(5000);
			} catch (Exception e) {
				System.err.println("rr thread error while sleeping "+e.getMessage());
				logger.error("rr thread error while sleeping "+e.getMessage(), e);
				e.printStackTrace();
			}
			readMessages();
		}
	}
	
	private void startDaemon(){
		Thread thread = new Thread(this);
		thread.start();
	}
	
	public static IP2PMessenger getP2PMessenger(){
		return p2p;
	}
	
	public static void main(String[] args){
		if(args.length != 1){
			System.out.println("Usage rr propertiesFile");
			System.exit(1);
		}
		RR.propertiesFile = args[0];
		RR.rrInstance = RR.getInstance();
	}
	
	public void addServiceAddressMessage(Document message){
		serviceAddressMessages.add(message);
	}
	
	public Boolean hasServiceAddressMessages(){
		return !serviceAddressMessages.isEmpty();
	}
	
	public void addStartAppMessage(Document message){
		startAppMessages.add(message);
	}
	
	public Boolean hasStartAppMessages(){
		return !startAppMessages.isEmpty();
	}
	
	public void addStopAppMessage(Document message){
		stopAppMessages.add(message);
	}
	
	public Boolean hasStopAppMessages(){
		return !stopAppMessages.isEmpty();
	}
	
	private void readMessages(){
		readServiceAddressMessages();
		readStartAppMessages();
		readStopAppMessages();
	}

	private void readStopAppMessages() {
		if(hasStopAppMessages()){
			Iterator<Document> iter = stopAppMessages.iterator();
			while(iter.hasNext()){
				Document message = iter.next();
				Hashtable<String, String> read = PaasUtilities.XML2Hashtable(message);
				//if STOP ALL: appID, IP
				if(PaasUtilities.getXMLroot(message).contains("app_stop_all")){
					String appID = read.get("appID");
					String IP = read.get("IP");
					//delete info from routing table if present
					if(RoutingInfoHibernateUtil.checkRoutingAlreadyPresentNoPort(appID, IP)){
						RoutingInfoHibernateUtil.deleteRoutingInfoIP(IP);
						//propagate info via P2P
						RoutingInfo r = new RoutingInfo(appID, IP, null, null);
						p2p.sendMulticast(PaasUtilities.XML2String(r.toXML("app_stop_all")), PaasMessage.TOPIC_STOP_APP);
					}
				}
				else{
					//appID, IP, port
					String appID = read.get("appID");
					String IP = read.get("IP");
					String consolePort = read.get("consolePort");
					//delete info from routing table if present
					RoutingInfo r = new RoutingInfo(appID, IP, null, consolePort);
					if(RoutingInfoHibernateUtil.checkRoutingAlreadyPresent(r)){
						RoutingInfoHibernateUtil.deleteRoutingInfoPort(r);
						//propagate info via P2P
						p2p.sendMulticast(PaasUtilities.XML2String(r.toXML("app_stop")), PaasMessage.TOPIC_STOP_APP);
					}
				}
				//else I did not have the info, skip it
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
				String IP = read.get("IP");
				String clientPort = read.get("clientPort");
				String consolePort = read.get("consolePort");
				//add info to routing table if not already present
				RoutingInfo r = new RoutingInfo(appID, IP, clientPort, consolePort);
				if(!RoutingInfoHibernateUtil.checkRoutingAlreadyPresent(r)){
					RoutingInfoHibernateUtil.saveRoutingInfo(r);
					//propagate info via P2P
					p2p.sendMulticast(PaasUtilities.XML2String(r.toXML("app_start")), PaasMessage.TOPIC_START_APP);
					//notify AEEs of new service address
					/*
					 * message is:
					 * <service>NAME</service>
					 * <addresses>IP1:clientPORT1:consolePORT1;...;IPN:clientPORTN:consolePORTN</addresses>
					 */
					Document additionalInfo = PaasUtilities.createBaseXML("service_addresses");
					PaasUtilities.addXMLnode(additionalInfo, "service", appID);
					PaasUtilities.addXMLnode(additionalInfo, "addresses", IP+":"+clientPort);
					Messenger.getInstance().sendMessage(PaasMessage.TOPIC_SERVICE_ADDRESS, PaasMessage.REPLY_ADDRESS_FOUND, null, additionalInfo);
				}
				//else duplicate message, skip it
				iter.remove();
			}
		}
	}

	private void readServiceAddressMessages() {
		//reply is TOPIC, REPLY, replyTo, INFO - if sending no info or reply broadcast use null
		if(hasServiceAddressMessages()){
			Iterator<Document> iter = serviceAddressMessages.iterator();
			while(iter.hasNext()){
				Document message = iter.next();
				Hashtable<String, String> read = PaasUtilities.XML2Hashtable(message);
				//get appID
				String service = read.get("service");
				String replyTo = read.get("replyTo");
				//standard info is known addresses appID, IP1:PORT1;...;IPN:PORTN
				/*
				 * <hosts>
				 * <appID>ID</appID>
				 * <addresses>IP1:PORT1;...;IPN:PORTN</addresses>
				 * </hosts>
				 */
				//check if info is in routing table
				List<RoutingInfo> hosts = RoutingInfoHibernateUtil.getRouting(service);
				if(hosts!=null){
					String reply = "";
					for(RoutingInfo r : hosts){
						reply+=r.getIp()+":"+r.getClientPort()+";";
					}
					Document additionalInfo = PaasUtilities.createBaseXML("service_addresses");
					PaasUtilities.addXMLnode(additionalInfo, "service", service);
					PaasUtilities.addXMLnode(additionalInfo, "addresses", reply);
					Messenger.getInstance().sendMessage(PaasMessage.TOPIC_SERVICE_ADDRESS, PaasMessage.REPLY_ADDRESS_FOUND, replyTo, additionalInfo);
				}
				//if no host is known
				else{
					//tell I don't have it
					Messenger.getInstance().sendMessage(PaasMessage.TOPIC_SERVICE_ADDRESS, PaasMessage.REPLY_ADDRESS_NOT_FOUND, replyTo, null);
					//ask other rrs for info
					Document content = PaasUtilities.createBaseXML("service_address_request");
					PaasUtilities.addXMLnode(content, "service", service);
					p2p.sendAnycast(PaasUtilities.XML2String(content), PaasMessage.TOPIC_SERVICE_ADDRESS);
				}
				iter.remove();
			}
		}
	}
	
	private void theGoodShepherd(){
		//check if I'm replacing a dead Runner instance
		if(RoutingInfoHibernateUtil.isDead()){
			List<RoutingInfo> lostSheeps = RoutingInfoHibernateUtil.getRoutingInfos();
			Iterator<RoutingInfo> iter = lostSheeps.iterator();
			while (iter.hasNext()) {
				RoutingInfo current = iter.next();
				//if so, check all previous info
				//telnet app and check if is still good, if not, delete bad info
				if(!isAppAlive(current.getAppID(), current.getIp(), current.getConsolePort()))RoutingInfoHibernateUtil.deleteRoutingInfoPort(current);
			}
		}
	}
	
	public Boolean isAppAlive(String appID, String IP, String consolePort){
		try(Socket telnetSocket = new Socket(IP, Integer.parseInt(consolePort));
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
		        System.err.println("cannot connect to host "+IP+":"+consolePort);
		        return false;
		    }
	}
	
	public String getCurrentIP(){
		return rr_current_IP;
	}

	public static String getRr_DB_user() {
		return rr_DB_user;
	}

	public static String getRr_DB_pwd() {
		return rr_DB_pwd;
	}

	public static String getRr_DB_URL() {
		return rr_DB_URL;
	}

}
