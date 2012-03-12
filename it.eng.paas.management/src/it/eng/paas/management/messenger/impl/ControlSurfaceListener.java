package it.eng.paas.management.messenger.impl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import it.eng.paas.management.cc.impl.CC;
import it.eng.paas.management.messenger.IControlSurfaceListener;
import it.eng.paas.message.PaasMessage;
import it.eng.paas.utilities.PaasUtilities;

public class ControlSurfaceListener implements IControlSurfaceListener{
	
	private static String ip;
	private static int port;
	private static Logger logger = Logger.getLogger("ControlSurfaceListener");
	private static ControlSurfaceListener instance = null;
	private static ServerSocket socket;
	private static Socket client;
	
	public static ControlSurfaceListener getInstance(String propertiesFile){
		if(instance==null)return new ControlSurfaceListener(propertiesFile);
		return instance;
	}
	
	public static ControlSurfaceListener getInstance(){
		if(instance==null)return new ControlSurfaceListener(CC.getPropertiesFile());
		return instance;
	}
	
	private ControlSurfaceListener(String propertiesFile){
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			System.out.println("error while getting local IP: "+e.getMessage());
			logger.fatal("error while getting local IP: "+e.getMessage(), e);
			e.printStackTrace();
		}//CC.getInstance().getCurrentIP();
		Properties props = new Properties();
		try(FileInputStream fis = new FileInputStream(propertiesFile);){
			props.load(fis);
			//fis.close();
			port = Integer.parseInt(props.getProperty("cc_listener_port"));			
		}catch(Exception e){
			System.err.println("loadProperties fail: "+e.getMessage());
			logger.error("loadProperties fail: "+e.getMessage(), e);
		}
		if(!openServerSocket()){
			//TODO
			//magari porta occupata cambiala ed informa chi deve sapere
		}
		run();
	}
	
	public void run() {
		while(true){
			listen();
//			try {
//				Thread.sleep(5000);
//			} catch (Exception e) {
//				System.out.println("ControlSurfaceListener.listen interrupted while sleeping "+e.getMessage());
//				logger.fatal("ControlSurfaceListener.listen interrupted while sleeping "+e.getMessage(), e);
//				e.printStackTrace();
//			}
		}
	}

	public Boolean openServerSocket(){
		try{
			socket = new ServerSocket(port);
		} catch (Exception e) {
		        System.err.println("cannot open socket on "+ip+":"+port);
		        logger.info("cannot open socket on "+ip+":"+port);
		        return false;
		}
		return true;
	}
	
	private void listen(){
		String read = null;
		try{
			client = socket.accept();
		}catch(Exception e){
			System.out.println("Error while waiting for client connection "+e.getMessage());
			logger.error("Error while waiting for client connection "+e.getMessage(), e);
			e.printStackTrace();
		}
		try (InputStreamReader is = new InputStreamReader(client.getInputStream());
				BufferedReader in = new BufferedReader(is);) {
			//read XML request, connection is closed afterwards by client
			read = in.readLine();
			System.out.println("AAAAAAAA "+read);
		} catch (Exception e) {
			System.out.println("Error while reading from socket "+e.getMessage());
			logger.error("Error while reading from socket "+e.getMessage(), e);
			e.printStackTrace();
		}
		if(read!=null && read!=""){
			//analyze and satisfy request if possible
			Document request = PaasUtilities.string2XML(read);
			parseRequest(request);
			System.out.println("REQ: "+PaasUtilities.XML2String(request));
		}
	}
	
	private void parseRequest(Document request){
		/*request type is in document root*/
		PaasMessage topic = PaasMessage.fromString(PaasUtilities.getXMLroot(request));
		Hashtable<String, String> message = PaasUtilities.XML2Hashtable(request);
		
		System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA "+message.get("manifest"));
		
		switch(topic){
			case TOPIC_ACCEPT_APP:{
				String appID = message.get("appID");
				CC.getInstance().addAppToDeploy(appID);
				Document doc = PaasUtilities.createBaseXML("deployApp");
				PaasUtilities.addXMLnode(doc, "appID", appID);
				PaasUtilities.addXMLnode(doc, "replyTo", Messenger.getInstance().getQueueName());
				PaasUtilities.addXMLnode(doc, "manifest", message.get("manifest"));
				Messenger.getInstance().sendMessage(PaasMessage.TOPIC_ACCEPT_APP, PaasMessage.TOPIC_ACCEPT_APP, null, doc);
				break;
			}
			default:{
				System.out.println("Parse request: "+PaasUtilities.XML2String(request));
			}
		}
	}	
}
