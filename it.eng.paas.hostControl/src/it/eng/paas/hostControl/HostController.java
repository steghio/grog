package it.eng.paas.hostControl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import it.eng.paas.utilities.PaasUtilities;

public class HostController{
	
	private static String ip;
	private static final int port = 9999;
	private static Logger logger;
	private static ServerSocket socket;
	private static Socket client;
	private static HostController instance = null;
	//private List<Document> requests;
	
	public static HostController getInstance(){
		if(instance==null)return new HostController();
		return instance;
	}
	
	private HostController(){
		System.setProperty("rootPath", "../hostControl_logs/");
		logger = Logger.getLogger("HostController");
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			System.out.println("error while getting local IP: "+e.getMessage());
			logger.fatal("error while getting local IP: "+e.getMessage(), e);
			e.printStackTrace();
		}
		if(!openServerSocket()){
			//TODO
			//magari porta occupata cambiala ed informa chi deve sapere
		}
		System.out.println("listening on: "+ip+":"+port);
		logger.info("listening on: "+ip+":"+port);
		//requests = new LinkedList<>();
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

	private Boolean openServerSocket(){
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
		} catch (Exception e) {
			System.out.println("Error while reading from socket "+e.getMessage());
			logger.error("Error while reading from socket "+e.getMessage(), e);
			e.printStackTrace();
		}
		if(read!=null && read!=""){
			Document request = PaasUtilities.string2XML(read);
			//start new thread to satisfy request
			Runner r = new Runner(request);
			r.startDaemon();
			//requests.add(request);
			//TODO TEST
			System.out.println("received request: "+PaasUtilities.XML2String(request));
			logger.info("received request: "+PaasUtilities.XML2String(request));
		}
	}
	
	
	
	public static void main(String [] args){
		instance = HostController.getInstance();
	}
	
}
