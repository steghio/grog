package it.eng.paas.aee.messenger.impl;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import it.eng.paas.aee.controller.impl.Controller;
import it.eng.paas.aee.impl.Aee;
import it.eng.paas.message.PaasMessage;
import it.eng.paas.aee.messenger.IMessenger;
import it.eng.paas.aee.runner.impl.Runner;
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
	private String aee_current_IP;
	private static String aee_local_po_address;
	
	public static IMessenger getInstance(String propertiesFile){
		if(messengerInstance == null)messengerInstance = new Messenger(propertiesFile);
		return messengerInstance;
	}
	
	public static IMessenger getInstance(){
		if(messengerInstance == null)messengerInstance = new Messenger(Aee.getPropertiesFile());
		return messengerInstance;
	}
	
	private Messenger(String propertiesFile){
		/*initialize config params*/
		Properties props = new Properties();
		try(FileInputStream fis = new FileInputStream(propertiesFile);){
			props.load(fis);
			//fis.close();
			aee_current_IP = InetAddress.getLocalHost().getHostAddress();//Aee.getInstance().getCurrentIP();//props.getProperty("aee_current_IP");
			aee_local_po_address = props.getProperty("aee_local_po_address");			
		}catch(Exception e){
			System.err.println("loadProperties fail: "+e.getMessage());
			logger.error("loadProperties fail: "+e.getMessage(), e);
		}
		registerWithPO();
		System.out.println("messenger started on: "+aee_current_IP);
		logger.info("started on: "+aee_current_IP);
		//start polling queue for incoming messages
		startDaemon();
	}
	
	private Boolean registerWithPO() {
		//RabbitMQ EXCHANGE name and PO IP are the same vale, not the same thing!
		EXCHANGE_NAME = aee_local_po_address;
		Connection connection = null;
	    Channel channel = null;
	    //contact PO and create my inbox queue then listen on it
	    ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(aee_local_po_address);
		try{
			connection = factory.newConnection();
		    channel = connection.createChannel();
		    //TOPIC routes based on routing keys in the form x.y.z, *.y.# with * = one word, # = two or more words
		    channel.exchangeDeclare(EXCHANGE_NAME, "topic");
		    //declare queue to receive incoming messages as <entity>_IP without dots
		    queueName = "aee_"+PaasUtilities.sdotIP(aee_current_IP);
		    //declare queue(name, destroy if server shuts down, not exclusive, do not autodelete, no extra arguments)
		    channel.queueDeclare(queueName, false, false, false, null);
		    //bind queue to routing keys <entity>.<queueName>.<messageTopic>
		    //receive all messages addressed to aees
		    channel.queueBind(queueName, EXCHANGE_NAME, "aee.*.*");
		    //receive all messages addressed only to me
		    channel.queueBind(queueName, EXCHANGE_NAME, "aee."+queueName+".*");
		    consumer = new QueueingConsumer(channel);
		    channel.basicConsume(queueName, true, consumer);
		    //do not close connection as we will keep polling queue to check for messages as long as I'm running
		    //connection.close();
		}catch(Exception e){
			System.err.println("error while contacting PO at IP "+aee_local_po_address);
			e.printStackTrace();
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	public void requestServiceAddress(String service) {
		String routingKey = "rr.*."+PaasMessage.TOPIC_SERVICE_ADDRESS.getText();
		BasicProperties props = new BasicProperties();
		props.setReplyTo(queueName);
		Document message = PaasUtilities.createBaseXML("service_address_request");
		PaasUtilities.addXMLnode(message, "service", service);
		send(routingKey, props, PaasUtilities.XML2String(message));
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
				//TODO solo per debug
				//System.out.println("delivery null");//break;
				continue;
			}
			//get topic from routing key
			String routing = delivery.getEnvelope().getRoutingKey();
			//check if message is broadcast or reply to someone else
			String [] splitted = routing.split("\\.");
			//if it is broadcast
			if(splitted[1]==null || splitted[1].equalsIgnoreCase("") || splitted[1].equalsIgnoreCase("*")){
				PaasMessage topic = PaasMessage.getTopic(routing);
				//get message, body is byte[] so ALWAYS new String!!!
				Document message = PaasUtilities.string2XML(new String(delivery.getBody()));
				//get replyTo info from messag envelope
				String replyTo = delivery.getProperties().getReplyTo();
				//add replyTo field to message if necessary
				if(replyTo != null)PaasUtilities.addXMLnode(message, "replyTo", replyTo);
				//TODO riga sotto solo per test
				System.out.println("received message, topic: "+topic.getText()+" replyTo: "+replyTo+" message "+new String(delivery.getBody()));
				logger.info("received message, topic: "+topic.getText()+" replyTo: "+replyTo+" message "+new String(delivery.getBody()));
				switch(topic){
					case TOPIC_ACCEPT_APP:{
						/*message is:
						 * <appID>appID</appID>
						 * <replyTo>WHO</replyTo>
						 * <manifest>manifest</manifest> 
						 */
						Controller.getInstance().addAcceptAppMessage(message);
						break;
					}
					case REPLY_ADDRESS_FOUND:{
						/*
						 * message is:
						 * <service>NAME</service>
						 * <IP>IP</IP>
						 * <port>PORT</port>
						 */
						Runner.getInstance().addServiceAddress(message);
						break;
					}
					case TOPIC_STOP_APP:{
						/*
						 * message is:
						 * <appID>appID</appID>
						 */
						Controller.getInstance().addStopAppMessage(message);
						break;
					}
					case TOPIC_SCALE_APP:{
						/*
						 * message is:
						 * <appID>appID</appID>
						 * <instances>NUMBER</instances>
						 * <location>http://IP:PORT/FOLDER</location>
						 */
						Controller.getInstance().addScaleAppMessage(message);
						break;
					}
					default:{
						//you should never get here
						System.err.println("Bad request from: "+replyTo+" message: "+message);
					}
				}
			}
			//else it is for someone
			else{
				//check if it is for me
				if(splitted[1].contains(queueName)){
					PaasMessage topic = PaasMessage.getTopic(routing);
					//get message, body is byte[] so ALWAYS new String!!!
					Document message = PaasUtilities.string2XML(new String(delivery.getBody()));
					//get replyTo info from messag envelope
					String replyTo = delivery.getProperties().getReplyTo();
					//add replyTo field to message if necessary
					if(replyTo != null)PaasUtilities.addXMLnode(message, "replyTo", replyTo);
					//TODO riga sotto solo per test
					System.out.println("received message, topic: "+topic.getText()+" replyTo: "+replyTo+" message "+new String(delivery.getBody()));
					logger.info("received message, topic: "+topic.getText()+" replyTo: "+replyTo+" message "+new String(delivery.getBody()));
					switch(topic){
						case TOPIC_START_APP:{
							/*
							 * message is:
							 * <appID>appID</appID>
							 * <location>http://IP:PORT/FOLDER</location>
							 */
							Controller.getInstance().addStartAppMessage(message);
							break;
						}
						case TOPIC_STOP_APP:{
							/*
							 * message is:
							 * <appID>appID</appID>
							 */
							Controller.getInstance().addStopAppMessage(message);
							break;
						}
						case TOPIC_SCALE_APP:{
							/*
							 * message is:
							 * <appID>appID</appID>
							 * <instances>NUMBER</instances>
							 * <location>http://IP:PORT/FOLDER</location>
							 */
							Controller.getInstance().addScaleAppMessage(message);
							break;
						}
						case REPLY_ADDRESS_FOUND:{
							/*
							 * message is:
							 * <service>NAME</service>
							 * <IP>IP</IP>
							 * <port>PORT</port>
							 */
							Runner.getInstance().addServiceAddress(message);
							break;
						}
						default:{
							//you should never get here
							System.err.println("Bad request from: "+replyTo+" message: "+message);
						}
					}
				}
			}
		}
//		}catch(Exception e){
//			System.err.println("error reading aee messages from po");
//			e.printStackTrace();
//		}//finally {
//	        if (connection != null) {
//	            try {
//	              connection.close();
//	            }catch (Exception ignore) {}
//	        }
//	    }		
	}
	
	/*Multiple recipients*/
	private void send(List<String> routingKeys, BasicProperties props, String message){
		Connection connection = null;
		Channel channel = null;
		ConnectionFactory factory = new ConnectionFactory();
	    factory.setHost(aee_local_po_address);
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
	    	System.err.println("error while trying to contact po on "+aee_local_po_address+" to send message");
	    	logger.error("error while trying to contact po on "+aee_local_po_address+" to send message");
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
	    factory.setHost(aee_local_po_address);
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
	    	System.err.println("error while trying to contact po on "+aee_local_po_address+" to send message");
	    	logger.error("error while trying to contact po on "+aee_local_po_address+" to send message");
	    }finally {
	        if (connection != null) {
	            try {
	              connection.close();
	            }catch (Exception ignore) {}
	        }
	    }		
	}
	
	@SuppressWarnings("deprecation")
	/* ONE recipient - cloud controller which sent request
	 * MANDATORY additionalInfo
	 * MANDATORY replyTo
	 */
	private void replyAcceptAppMessage(PaasMessage reply, String replyTo, Document additionalInfo){
		String routingKey = null;
		BasicProperties props = null;
		//should never be null, only one recipient!
		if(replyTo!=null){
			routingKey = "cc."+replyTo+"."+reply.getText();
			props = new BasicProperties();
			props.setReplyTo(queueName);
		}
		//if for some reason replyTo is null, notify every CC
		else routingKey = "cc.*."+reply.getText();
		/*Props are:
		 * java.lang.String contentType, java.lang.String contentEncoding, 
		 * java.util.Map<java.lang.String,java.lang.Object> headers, java.lang.Integer deliveryMode, 
		 * java.lang.Integer priority, java.lang.String correlationId, java.lang.String replyTo, 
		 * java.lang.String expiration, java.lang.String messageId, java.util.Date timestamp, 
		 * java.lang.String type, java.lang.String userId, java.lang.String appId, 
		 * java.lang.String clusterId
		 */
		//if no additional info send message as ACK - SHOULD NEVER DO IT (useless)
		if(additionalInfo == null)send(routingKey, props, "");
		else send(routingKey, props, PaasUtilities.XML2String(additionalInfo));
	}
	
	@SuppressWarnings("deprecation")
	/* BROADCAST message - cloud controller, request router
	 * MANDATORY additionalInfo
	 * OPTIONAL replyTo
	 */
	private void replyStartAppMessage(PaasMessage reply, String replyTo, Document additionalInfo){
		List<String> routingKeys = new LinkedList<>();
		BasicProperties props = null;
		//should never be set!
		if(replyTo!=null){
			routingKeys.add("cc."+replyTo+"."+reply.getText());
			props = new BasicProperties();
			props.setReplyTo(queueName);
		}
		//broadcast to CCs, RRs
		else {
			routingKeys.add("cc.*."+reply.getText());
			routingKeys.add("rr.*."+reply.getText());
		}
		//should never be null!
		if(additionalInfo == null)send(routingKeys, null, "");
		else send(routingKeys, null, PaasUtilities.XML2String(additionalInfo));
	}
	
	@SuppressWarnings("deprecation")
	/* BROADCAST message - cloud controller, request router
	 * MANDATORY additionalInfo
	 * OPTIONAL replyTo
	 */
	private void replyStopAppMessage(PaasMessage reply, String replyTo, Document additionalInfo){
		List<String> routingKeys = new LinkedList<>();
		BasicProperties props = null;
		//should never be set!
		if(replyTo!=null){
			routingKeys.add("cc."+replyTo+"."+reply.getText());
			props = new BasicProperties();
			props.setReplyTo(queueName);
		}
		//broadcast to CCs, RRs if stop OK
		else {
			if(!reply.equals(PaasMessage.REPLY_APP_STOP_FAIL)){
				routingKeys.add("cc.*."+reply.getText());
				routingKeys.add("rr.*."+reply.getText());
			}
			else routingKeys.add("cc.*."+reply.getText());
		}
		//should never be null!
		if(additionalInfo == null)send(routingKeys, null, "");
		else send(routingKeys, null, PaasUtilities.XML2String(additionalInfo));
	}
	
	@SuppressWarnings("deprecation")
	/* BROADCAST message - cloud controller, request router
	 * MANDATORY additionalInfo
	 * OPTIONAL replyTo
	 */
	private void replyScaleAppMessage(PaasMessage reply, String replyTo, Document additionalInfo){
		List<String> routingKeys = new LinkedList<>();
		BasicProperties props = null;
		//should never be set!
		if(replyTo!=null){
			routingKeys.add("cc."+replyTo+"."+reply.getText());
			props = new BasicProperties();
			props.setReplyTo(queueName);
		}
		//broadcast to CCs, RRs if scale OK
		else {
			if(!reply.equals(PaasMessage.REPLY_APP_SCALE_FAIL)){
				routingKeys.add("cc.*."+reply.getText());
				routingKeys.add("rr.*."+reply.getText());
			}
			else routingKeys.add("cc.*."+reply.getText());
		}
		//should never be null!
		if(additionalInfo == null)send(routingKeys, null, "");
		else send(routingKeys, null, PaasUtilities.XML2String(additionalInfo));
	}
	
	public void sendMessage(PaasMessage topic, PaasMessage reply, String replyTo, Document additionalInfo){
		switch(topic){
			case TOPIC_ACCEPT_APP:{
				replyAcceptAppMessage(reply, replyTo, additionalInfo);
				break;
			}
			case TOPIC_START_APP:{
				replyStartAppMessage(reply, replyTo, additionalInfo);
				break;
			}
			case TOPIC_STOP_APP:{
				replyStopAppMessage(reply, replyTo, additionalInfo);
				break;
			}
			case TOPIC_SCALE_APP:{
				replyScaleAppMessage(reply, replyTo, additionalInfo);
				break;
			}
			default:{
				//you should never get here
				System.err.println("something very wrong happened in sendMessage. Topic: "+topic.getText()+" reply: "+reply.getText()+" replyTo: "+replyTo+" message: "+PaasUtilities.XML2String(additionalInfo));
				logger.fatal("something very wrong happened in sendMessage. Topic: "+topic.getText()+" reply: "+reply.getText()+" replyTo: "+replyTo+" message: "+PaasUtilities.XML2String(additionalInfo));
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	public void notifyAeeStart(){
		String routingKey = "cc.*."+PaasMessage.COMPONENT_ALIVE_AEE.getText();
		BasicProperties props = new BasicProperties();
		props.setReplyTo(queueName);
		Document additionalInfo = PaasUtilities.createBaseXML("aee_start");
		PaasUtilities.addXMLnode(additionalInfo, "IP", aee_current_IP);
		send(routingKey, props, PaasUtilities.XML2String(additionalInfo));
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

}
