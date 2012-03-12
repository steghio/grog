package it.eng.paas.management.messenger.impl;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import it.eng.paas.message.PaasMessage;
import it.eng.paas.module.PaasModule;
import it.eng.paas.management.cc.impl.CC;
import it.eng.paas.management.messenger.IMessenger;
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
	private String cc_current_IP;
	private static String cc_local_po_address;
	
	public static String getLocalPOAddress(){
		return cc_local_po_address;
	}
	
	public static IMessenger getInstance(String propertiesFile){
		if(messengerInstance == null)messengerInstance = new Messenger(propertiesFile);
		return messengerInstance;
	}
	
	public static IMessenger getInstance(){
		if(messengerInstance == null)messengerInstance = new Messenger(CC.getPropertiesFile());
		return messengerInstance;
	}
	
	private Messenger(String propertiesFile){
		/*initialize config params*/
		Properties props = new Properties();
		try(FileInputStream fis = new FileInputStream(propertiesFile);){
			props.load(fis);
			//fis.close();
			cc_current_IP = InetAddress.getLocalHost().getHostAddress();//CC.getInstance().getCurrentIP();
			cc_local_po_address = props.getProperty("cc_local_po_address");			
		}catch(Exception e){
			System.err.println("loadProperties fail: "+e.getMessage());
			logger.error("loadProperties fail: "+e.getMessage(), e);
		}
		registerWithPO();
		System.out.println("messenger started on: "+cc_current_IP);
		logger.info("started on: "+cc_current_IP);
		//start polling queue for incoming messages
		startDaemon();
	}
	
	private Boolean registerWithPO() {
		//RabbitMQ EXCHANGE name and PO IP are the same vale, not the same thing!
		EXCHANGE_NAME = cc_local_po_address;
		Connection connection = null;
	    Channel channel = null;
	    //contact PO and create my inbox queue then listen on it
	    ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(cc_local_po_address);
		try{
			connection = factory.newConnection();
		    channel = connection.createChannel();
		    //TOPIC routes based on routing keys in the form x.y.z, *.y.# with * = one word, # = two or more words
		    channel.exchangeDeclare(EXCHANGE_NAME, "topic");
		    //declare queue to receive incoming messages as <entity>_IP without dots
		    queueName = "cc_"+PaasUtilities.sdotIP(cc_current_IP);
		    //declare queue(name, destroy if server shuts down, not exclusive, do not autodelete, no extra arguments)
		    channel.queueDeclare(queueName, false, false, false, null);
		    //bind queue to routing keys <entity>.<queueName>.<messageTopic>
		    //receive all messages addressed to ccs
		    channel.queueBind(queueName, EXCHANGE_NAME, "cc.*.*");
		    //receive all messages addressed only to me
		    channel.queueBind(queueName, EXCHANGE_NAME, "cc."+queueName+".*");
		    consumer = new QueueingConsumer(channel);
		    channel.basicConsume(queueName, true, consumer);
		    //do not close connection as we will keep polling queue to check for messages as long as I'm running
		    //connection.close();
		}catch(Exception e){
			System.err.println("error while contacting PO at IP "+cc_local_po_address);
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
		//TODO metti HmMessage.TOPIC_HEARTBEAT
		String routingKey = "";//"hm.*."+AeeMessage.TOPIC_HEARTBEAT.getText();
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
	 * acceptApp - ask the AEE if it can run the app
	 * startApp
	 * stopApp - stop all app instances
	 * scaleApp
	 * Message parameters:
	 * acceptApp manifest - the URL where to find the MANIFEST for the app to be deployed
	 * startApp URI - the URL where to find the app to be started
	 * stopApp appID
	 * scaleApp appID instances
	 */
	public static void getMessages(){
		//contact PO to retrieve messages
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
			Document message = null;
			//get replyTo info from messag envelope
			String replyTo = delivery.getProperties().getReplyTo();
			if(delivery.getBody()!=null){
				message = PaasUtilities.string2XML(new String(delivery.getBody()));
				//TODO riga sotto solo per test
				System.out.println("received message, topic: "+topic.getText()+" replyTo: "+replyTo+" message "+new String(delivery.getBody()));
				logger.info("received message, topic: "+topic.getText()+" replyTo: "+replyTo+" message "+new String(delivery.getBody()));
			}
			else{
				//TODO riga sotto solo per test
				System.out.println("received message, topic: "+topic.getText()+" replyTo: "+replyTo+" empty message ");
				logger.info("received message, topic: "+topic.getText()+" replyTo: "+replyTo+" empty message ");
			}
			//add replyTo field to message if necessary
			if(replyTo != null)PaasUtilities.addXMLnode(message, "replyTo", replyTo);
			
			switch(topic){
				case REPLY_APP_ALREADY_PRESENT:{
					CC.getInstance().addAppAlreadyPresentReply(message);
					break;
				}
				case REPLY_APP_ACCEPTABLE:{
					CC.getInstance().addAppAcceptableReply(message);
					break;
				}
				case REPLY_APP_DEPLOY_FAIL:{
					//TODO
					System.out.println("DEPLOY FAIL");
					break;
				}
				case REPLY_APP_START_OK:{
					//TODO
					System.out.println("START OK");
					break;
				}
				case REPLY_APP_START_FAIL:{
					//TODO
					System.out.println("START FAIL");
					break;
				}
				case REPLY_APP_STOP_OK:{
					//TODO
					System.out.println("STOP OK");
					break;
				}
				case REPLY_APP_STOP_FAIL:{
					//TODO
					System.out.println("STOP FAIL");
					break;
				}
				case REPLY_APP_SCALE_OK:{
					//TODO
					System.out.println("SCALE OK");
					break;
				}
				case REPLY_APP_SCALE_FAIL:{
					//TODO
					System.out.println("SCALE FAIL");
					break;
				}
				case COMPONENT_ALIVE_AEE:{
					CC.getInstance().addAeeStartMessage(message);
					CC.getModulesToStart().remove(PaasModule.AEE);
					break;
				}
				case COMPONENT_ALIVE_RR:{
					CC.getInstance().addRRStartMessage(message);
					CC.getModulesToStart().remove(PaasModule.RR);
					break;
				}
				case COMPONENT_ALIVE_CC:{
					CC.getInstance().addCCStartMessage(message);
					CC.getModulesToStart().remove(PaasModule.CC);
					break;
				}
				default:{
					//you should never get here
					System.err.println("Bad request from: "+replyTo+" message: "+message);
				}
			}
		}	
	}
	
	/*Multiple recipients*/
	private void send(List<String> routingKeys, BasicProperties props, String message){
		Connection connection = null;
		Channel channel = null;
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(cc_local_po_address);
	    try{
		    connection = factory.newConnection();
		    channel = connection.createChannel();
		    channel.exchangeDeclare(EXCHANGE_NAME, "topic");
		    //exchange, routing, props, message
		    for(String routingKey : routingKeys){
		    	if(message == null) message = "";
		    	channel.basicPublish(EXCHANGE_NAME, routingKey, props, message.getBytes());
		    	System.out.println("sent message: "+message+" with routing: "+routingKey);
			    logger.info("sent message: "+message+" with routing: "+routingKey);
		    }
	    }catch(Exception e){
	    	System.err.println("error while trying to contact po on "+cc_local_po_address+" to send message");
	    	logger.error("error while trying to contact po on "+cc_local_po_address+" to send message");
	    }finally {
	        if (connection != null) {
	            try {
	              connection.close();
	            }catch (Exception ignore) {}
	        }
	    }		
	}
	
	/*Single recipient*/
	private void send(String routingKey, BasicProperties props, String message){
		Connection connection = null;
		Channel channel = null;
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(cc_local_po_address);
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
	    	System.err.println("error while trying to contact po on "+cc_local_po_address+" to send message");
	    	logger.error("error while trying to contact po on "+cc_local_po_address+" to send message");
	    }finally {
	        if (connection != null) {
	            try {
	              connection.close();
	            }catch (Exception ignore) {}
	        }
	    }		
	}
	
	@SuppressWarnings("deprecation")
	/* SINGLE message - aee
	 * MANDATORY additionalInfo, replyTo
	 */
	private void replyStartAppMessage(PaasMessage reply, String replyTo, Document additionalInfo){
		String routingKey = null;
		BasicProperties props = null;
		routingKey = "aee."+replyTo+"."+reply.getText();
		props = new BasicProperties();
		props.setReplyTo(queueName);
		send(routingKey, props, PaasUtilities.XML2String(additionalInfo));
		System.out.println("reply start routing: "+routingKey+" message :"+PaasUtilities.XML2String(additionalInfo));
		logger.info("reply start routing: "+routingKey+" message :"+PaasUtilities.XML2String(additionalInfo));
	}
	
	@SuppressWarnings("deprecation")
	/* SINGLE message - aee
	 * MANDATORY additionalInfo
	 * NO replyTo as we do not know who will answer yet
	 */
	private void replyAcceptAppMessage(PaasMessage reply, String replyTo, Document additionalInfo){
		String routingKey = null;
		BasicProperties props = null;
		routingKey = "aee.*."+reply.getText();
		props = new BasicProperties();
		props.setReplyTo(queueName);
		send(routingKey, props, PaasUtilities.XML2String(additionalInfo));
		System.out.println("reply accept routing: "+routingKey+" message :"+PaasUtilities.XML2String(additionalInfo));
		logger.info("reply accept routing: "+routingKey+" message :"+PaasUtilities.XML2String(additionalInfo));
	}
	
	@SuppressWarnings("deprecation")
	/* BROADCAST message - every component
	 * NO additionalInfo
	 * MANDATORY replyTo
	 */
	private void sendWhoAliveMessage(PaasMessage reply, String replyTo, Document additionalInfo){
		List<String> routingKeys = new LinkedList<>();
		BasicProperties props = null;
		routingKeys.add("aee.*."+reply.getText());
		routingKeys.add("rr.*."+reply.getText());
		routingKeys.add("cc.*."+reply.getText());
		props = new BasicProperties();
		props.setReplyTo(queueName);
		if(additionalInfo == null){
			additionalInfo = PaasUtilities.createBaseXML("alive");
		}
		send(routingKeys, props, PaasUtilities.XML2String(additionalInfo));
		System.out.println("sent whoalive");
		logger.info("sent whoalive");
	}
	
	public void sendMessage(PaasMessage topic, PaasMessage reply, String replyTo, Document additionalInfo){
		switch(topic){
			case TOPIC_START_APP:{
				replyStartAppMessage(reply, replyTo, additionalInfo);
				break;
			}
			case TOPIC_ACCEPT_APP:{
				replyAcceptAppMessage(reply, replyTo, additionalInfo);
				break;
			}
			case COMPONENT_WHO_ALIVE:{
				sendWhoAliveMessage(reply, replyTo, additionalInfo);
				break;
			}
			//TODO il resto
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
	
	public String getQueueName(){
		return queueName;
	}
	
	@SuppressWarnings("deprecation")
	public void notifyCCStart(){
		String routingKey = "cc.*."+PaasMessage.COMPONENT_ALIVE_CC.getText();
		BasicProperties props = new BasicProperties();
		props.setReplyTo(queueName);
		Document additionalInfo = PaasUtilities.createBaseXML("aee_start");
		PaasUtilities.addXMLnode(additionalInfo, "IP", cc_current_IP);
		send(routingKey, props, PaasUtilities.XML2String(additionalInfo));
	}

}
