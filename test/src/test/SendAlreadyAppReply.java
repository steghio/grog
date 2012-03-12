package test;

import it.eng.paas.utilities.PaasUtilities;

import org.w3c.dom.Document;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class SendAlreadyAppReply{

	public static void send1(){
		Connection connection = null;
	    Channel channel = null;
	    try {
	    	ConnectionFactory factory = new ConnectionFactory();
		      factory.setHost("192.168.23.28");
		  
		      connection = factory.newConnection();
		      channel = connection.createChannel();

		      channel.exchangeDeclare("192.168.23.28", "topic");

		      String routingKey = "cc.*.appAcceptable";//getRouting(argv);
		      Document xml = PaasUtilities.createBaseXML("accept_app");
		      PaasUtilities.addXMLnode(xml, "appID", "pippo");
		      PaasUtilities.addXMLnode(xml, "replyTo", "000");
				PaasUtilities.addXMLnode(xml, "aee", "aee_000");
				PaasUtilities.addXMLnode(xml, "CPU_usage", "4");
				PaasUtilities.addXMLnode(xml, "RAM_usage", "4");
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
	
	public static void send(){
		Connection connection = null;
	    Channel channel = null;
	    try {
	    	ConnectionFactory factory = new ConnectionFactory();
		      factory.setHost("192.168.23.28");
		  
		      connection = factory.newConnection();
		      channel = connection.createChannel();

		      channel.exchangeDeclare("192.168.23.28", "topic");

		      String routingKey = "cc.*.appAlreadyPresent";//getRouting(argv);
		      Document xml = PaasUtilities.createBaseXML("accept_app");
		      PaasUtilities.addXMLnode(xml, "appID", "pippo");
		      PaasUtilities.addXMLnode(xml, "replyTo", "123456");
				PaasUtilities.addXMLnode(xml, "aee", "aee_1234");
				PaasUtilities.addXMLnode(xml, "CPU_usage", "5");
				PaasUtilities.addXMLnode(xml, "RAM_usage", "5");
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

	  public static void main(String[] argv) {
	    send();
	    //send1();
	  }
}
