package test;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import org.w3c.dom.Document;

import it.eng.paas.utilities.PaasUtilities;





class SendStopAppMessage{
	
	public static void send(){
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
		      PaasUtilities.addXMLnode(xml, "appID", "it.eng.test.remote.ds.hello");
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
