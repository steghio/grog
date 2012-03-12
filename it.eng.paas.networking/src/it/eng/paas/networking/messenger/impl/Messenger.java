package it.eng.paas.networking.messenger.impl;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import it.eng.paas.networking.messenger.IMessenger;
import it.eng.paas.message.PaasMessage;
import it.eng.paas.networking.rr.impl.RR;
import it.eng.paas.utilities.PaasUtilities;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;

public class Messenger implements IMessenger, Runnable{

	private static Messenger messengerInstance = null;
	private static final Logger logger = Logger.getLogger("Messenger");
	private static String EXCHANGE_NAME = null;
	private static String queueName = null; //replyTo ID
	private static QueueingConsumer consumer = null;
	
	/*config params*/
	private String rr_current_IP;
	private static String rr_local_po_address;
	
	public static IMessenger getInstance(String propertiesFile){
		if(messengerInstance == null)messengerInstance = new Messenger(propertiesFile);
		return messengerInstance;
	}
	
	public static IMessenger getInstance(){
		if(messengerInstance == null)messengerInstance = new Messenger(RR.getPropertiesFile());
		return messengerInstance;
	}

	private Messenger(String propertiesFile){
		/*initialize config params*/
		Properties props = new Properties();
		try(FileInputStream fis = new FileInputStream(propertiesFile);){
			props.load(fis);
			//fis.close();
			rr_current_IP = InetAddress.getLocalHost().getHostAddress();//RR.getInstance().getCurrentIP();//props.getProperty("rr_current_IP");
			rr_local_po_address = props.getProperty("rr_local_po_address");			
		}catch(Exception e){
			System.err.println("loadProperties fail: "+e.getMessage());
			logger.error("loadProperties fail: "+e.getMessage(), e);
		}
		registerWithPO();
		System.out.println("messenger started on: "+rr_current_IP);
		logger.info("started on: "+rr_current_IP);
		//start polling queue for incoming messages
		startDaemon();
	}
	
	private Boolean registerWithPO() {
		//RabbitMQ EXCHANGE name and PO IP are the same value, not the same thing!
		EXCHANGE_NAME = rr_local_po_address;
		Connection connection = null;
	    Channel channel = null;
	    //contact PO and create my inbox queue then listen on it
	    ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(rr_local_po_address);
		try{
			connection = factory.newConnection();
		    channel = connection.createChannel();
		    //TOPIC routes based on routing keys in the form x.y.z, *.y.# with * = one word, # = two or more words
		    channel.exchangeDeclare(EXCHANGE_NAME, "topic");
		    //declare queue to receive incoming messages as <entity>_IP without dots
		    queueName = "rr_"+PaasUtilities.sdotIP(rr_current_IP);
		    //declare queue(name, destroy if server shuts down, not exclusive, do not autodelete, no extra arguments)
		    channel.queueDeclare(queueName, false, false, false, null);
		    //bind queue to routing keys <entity>.<queueName>.<messageTopic>
		    //receive all messages addressed to rrs
		    channel.queueBind(queueName, EXCHANGE_NAME, "rr.*.*");
		    //receive all messages addressed only to me
		    channel.queueBind(queueName, EXCHANGE_NAME, "rr."+queueName+".*");
		    consumer = new QueueingConsumer(channel);
		    channel.basicConsume(queueName, true, consumer);
		    //do not close connection as we will keep polling queue to check for messages as long as I'm running
		    //connection.close();
		}catch(Exception e){
			System.err.println("error while contacting PO at IP "+rr_local_po_address);
			e.printStackTrace();
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	/* ONE recipient - health manager
	 * MANDATORY replyTo
	 * NO message
	 */
	public void sendHeartBeat() {
		//TODO metti HmMessage.TOPIC_...
		String routingKey = "";//"hm.*."+RrMessage.TOPIC_HEARTBEAT.getText();
		BasicProperties props = null;
		props = new BasicProperties();
		props.setReplyTo(queueName);
		/*Props are:
		 * java.lang.String contentType, java.lang.String contentEncoding, 
		 * java.util.Map<java.lang.String,java.lang.Object> headers, java.lang.Integer deliveryMode, 
		 * java.lang.Integer priority, java.lang.String correlationId, java.lang.String replyTo, 
		 * java.lang.String expiration, java.lang.String messageId, java.util.Date timestamp, 
		 * java.lang.String type, java.lang.String userId, java.lang.String appId, 
		 * java.lang.String clusterId
		 */
		send(routingKey, props, null);
	}
	
	//check if messenger is still alive
	public Boolean isAlive(){
		return !(messengerInstance == null);
	}
	
	/**
	 * Message topics:
	 * 
	 */
	public static void getMessages(){
		QueueingConsumer.Delivery delivery = null;
		//try{
		//read all messages with timeout
		//TODO metti timeout decente
		while (true) {
			try{
				delivery = consumer.nextDelivery(5000);
			}catch(Exception e){
				System.err.println("error while waiting for messages");
			}
			if(delivery == null){
				continue;
			}
			//get topic from routing key
			PaasMessage topic = PaasMessage.getTopic(delivery.getEnvelope().getRoutingKey());
			//get message, body is byte[] so ALWAYS new String!!!
			Document message = PaasUtilities.string2XML(new String(delivery.getBody()));
			//get replyTo info from message envelope
			String replyTo = delivery.getProperties().getReplyTo();
			//add replyTo field to message if necessary
			if(replyTo != null)PaasUtilities.addXMLnode(message, "replyTo", replyTo);
			//TODO riga sotto solo per test
			System.out.println("received message, topic: "+topic.getText()+" replyTo: "+replyTo+" message "+new String(delivery.getBody()));
			logger.info("received message, topic: "+topic.getText()+" replyTo: "+replyTo+" message "+new String(delivery.getBody()));
			switch(topic){
				case TOPIC_SERVICE_ADDRESS:{
					//reply to service address request from aees
					RR.getInstance().addServiceAddressMessage(message);
					break;
				}
				case REPLY_APP_START_OK:{
					//update routing table with info from aee
					RR.getInstance().addStartAppMessage(message);
					break;
				}
				case REPLY_APP_STOP_OK:{
					//update routing table with info from aee
					RR.getInstance().addStopAppMessage(message);
					break;
				}
				//TODO
				default:{
					//you should never get here
					System.err.println("Bad request from: "+replyTo+" message: "+message);
				}
			}
		}		
	}
	
	/*Single recipient*/
	private void send(String routingKey, BasicProperties props, String message){
		Connection connection = null;
		Channel channel = null;
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(rr_local_po_address);
	    try{
		    connection = factory.newConnection();
		    channel = connection.createChannel();
		    channel.exchangeDeclare(EXCHANGE_NAME, "topic");
		    //exchange, routing, props, message
		    if(message == null) message = "";
	    	channel.basicPublish(EXCHANGE_NAME, routingKey, props, message.getBytes());
	    	System.out.println("sent message: "+message+" with routing: "+routingKey);
		    logger.info("sent message: "+message+" with routing: "+routingKey);
	    }catch(Exception e){
	    	System.err.println("error while trying to contact po on "+rr_local_po_address+" to send message");
	    	logger.error("error while trying to contact po on "+rr_local_po_address+" to send message");
	    }finally {
	        if (connection != null) {
	            try {
	              connection.close();
	            }catch (Exception ignore) {}
	        }
	    }		
	}
	
	@SuppressWarnings("deprecation")
	public void sendMessage(PaasMessage topic, PaasMessage reply, String replyTo, Document additionalInfo){
		switch(topic){
			case TOPIC_SERVICE_ADDRESS:{
				String routingKey = null;
				BasicProperties props = null;
				//should never be null, only one recipient!
				if(replyTo!=null){
					routingKey = "aee."+replyTo+"."+reply.getText();
					props = new BasicProperties();
					props.setReplyTo(replyTo);
				}
				//if for some reason replyTo is null, notify every AEE
				else routingKey = "aee.*."+reply.getText();
				/*Props are:
				 * java.lang.String contentType, java.lang.String contentEncoding, 
				 * java.util.Map<java.lang.String,java.lang.Object> headers, java.lang.Integer deliveryMode, 
				 * java.lang.Integer priority, java.lang.String correlationId, java.lang.String replyTo, 
				 * java.lang.String expiration, java.lang.String messageId, java.util.Date timestamp, 
				 * java.lang.String type, java.lang.String userId, java.lang.String appId, 
				 * java.lang.String clusterId
				 */
				//if no additional info send message as ACK - should never be null!
				if(additionalInfo == null)send(routingKey, props, "");
				else send(routingKey, props, PaasUtilities.XML2String(additionalInfo));
			}
			default:{
				//you should never get here
				System.err.println("something very wrong happened in sendMessage. Topic: "+topic.getText()+" reply: "+reply.getText()+" replyTo: "+replyTo+" message: "+PaasUtilities.XML2String(additionalInfo));
				logger.fatal("something very wrong happened in sendMessage. Topic: "+topic.getText()+" reply: "+reply.getText()+" replyTo: "+replyTo+" message: "+PaasUtilities.XML2String(additionalInfo));
			}
		}
	}

	public void run() {
		try {
			Thread.sleep(5000);
		} catch (Exception e) {
			System.err.println("messenger thread error while sleeping "+e.getMessage());
			logger.error("messenger thread error while sleeping "+e.getMessage(), e);
			e.printStackTrace();
		}
		getMessages();
		//TODO close connection to Rabbit server if crash, shutdown, ...
	}
	
	private void startDaemon(){
		Thread thread = new Thread(this);
		thread.start();
	}
	
	@SuppressWarnings("deprecation")
	public void notifyRRStart(){
		String routingKey = "cc.*."+PaasMessage.COMPONENT_ALIVE_RR.getText();
		BasicProperties props = new BasicProperties();
		props.setReplyTo(queueName);
		Document additionalInfo = PaasUtilities.createBaseXML("rr_start");
		PaasUtilities.addXMLnode(additionalInfo, "IP", rr_current_IP);
		send(routingKey, props, PaasUtilities.XML2String(additionalInfo));
	}

}
