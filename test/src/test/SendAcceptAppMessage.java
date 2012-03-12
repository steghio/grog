package test;

import it.eng.paas.utilities.PaasUtilities;

import org.w3c.dom.Document;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class SendAcceptAppMessage {

	public static void send(){
		Connection connection = null;
	    Channel channel = null;
	    try {
	    	ConnectionFactory factory = new ConnectionFactory();
		      factory.setHost("192.168.23.28");
		  
		      connection = factory.newConnection();
		      channel = connection.createChannel();

		      channel.exchangeDeclare("192.168.23.28", "topic");

		      String routingKey = "aee.*.acceptApp";//getRouting(argv);
		      Document xml = PaasUtilities.createBaseXML("accept_app");
		      PaasUtilities.addXMLnode(xml, "appID", "pippo");
		      PaasUtilities.addXMLnode(xml, "replyTo", "123456");
		      Document asd = PaasUtilities.createBaseXML("manifest");
		      PaasUtilities.addXMLnode(asd, "Require-OS", "Windows XP");
		      PaasUtilities.addXMLnode(asd, "Require-Arch", "x86");
		      PaasUtilities.addXMLnode(asd, "Require-RAM", "1024");
		      PaasUtilities.addXMLnode(asd, "Require-Hz", "1000");
		      PaasUtilities.addXMLnode(asd, "Require-Cores", "1");
		      PaasUtilities.addXMLnode(xml, "manifest", PaasUtilities.XML2String(asd));
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
	  }
}
