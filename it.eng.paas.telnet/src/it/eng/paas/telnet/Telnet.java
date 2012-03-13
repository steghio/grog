package it.eng.paas.telnet;

import it.eng.paas.utilities.PaasUtilities;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import org.w3c.dom.Document;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Telnet {
	
	static Socket client;
	static PrintWriter out;
	
	public static void sendScale(String appid, String instances){
		Connection connection = null;
	    Channel channel = null;
	    try {
	    	ConnectionFactory factory = new ConnectionFactory();
		      factory.setHost("192.168.23.28");
		  
		      connection = factory.newConnection();
		      channel = connection.createChannel();

		      channel.exchangeDeclare("192.168.23.28", "topic");

		      String routingKey = "aee.*.scaleApp";//getRouting(argv);
		      Document xml = PaasUtilities.createBaseXML("scale_app");
		      PaasUtilities.addXMLnode(xml, "appID", appid);
		      PaasUtilities.addXMLnode(xml, "instances", instances);
		      PaasUtilities.addXMLnode(xml, "location", "http://192.168.23.94/mnt/storage/nas1/apps/"+appid+".zip");
		      String message = PaasUtilities.XML2String(xml);

		      channel.basicPublish("192.168.23.28", routingKey, null, message.getBytes());
		      System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");

	    }
	    catch (Exception e) {
	      e.printStackTrace();
	    }
	    finally {
	      if (connection != null) {
	        try {
	          connection.close();
	        }
	        catch (Exception ignore) {}
	      }
	    }
	}
	
	public static void sendStop(String appid){
		Connection connection = null;
	    Channel channel = null;
	    try {
	    	ConnectionFactory factory = new ConnectionFactory();
		      factory.setHost("192.168.23.28");
		  
		      connection = factory.newConnection();
		      channel = connection.createChannel();

		      channel.exchangeDeclare("192.168.23.28", "topic");

		      String routingKey = "aee.*.stopApp";//getRouting(argv);
		      Document xml = PaasUtilities.createBaseXML("stop_app");
		      PaasUtilities.addXMLnode(xml, "appID", appid);
		      String message = PaasUtilities.XML2String(xml);

		      channel.basicPublish("192.168.23.28", routingKey, null, message.getBytes());
		      System.out.println(" [x] Sent '" + routingKey + "':'" + message + "'");

	    }
	    catch (Exception e) {
	      e.printStackTrace();
	    }
	    finally {
	      if (connection != null) {
	        try {
	          connection.close();
	        }
	        catch (Exception ignore) {}
	      }
	    }
	  }

	
	public static void action(String url, String appid, String type, String manifest, String instances){
		try{
			switch(type){
			case "start":{
				client = new Socket(url, 6666);
				OutputStream a = client.getOutputStream();
				out = new PrintWriter(a, true);
				Document doc = PaasUtilities.createBaseXML("acceptApp");
				PaasUtilities.addXMLnode(doc, "appID", appid);
				Document asd = PaasUtilities.file2XML(new File(manifest), "manifest");
				PaasUtilities.addXMLnode(doc, "manifest", PaasUtilities.XML2String(asd));
				System.out.println(PaasUtilities.XML2String(doc));
				out.println(PaasUtilities.XML2String(doc));
				out.close();
				a.close();
				client.close();
				break;
			}
			case "stop":{
				sendStop(appid);
				break;
			}
			case "scale":{
				sendScale(appid, instances);
				break;
			}
			default:{
				
			}
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		
		String url = args[0];
		String appid = args[1];
		String type = args[2];
		String manifest = args[3];
		String instances = args[4];
		
		action(url, appid, type, manifest, instances);
		
	}
}
