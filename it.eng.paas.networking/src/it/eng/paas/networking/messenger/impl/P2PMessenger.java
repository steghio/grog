package it.eng.paas.networking.messenger.impl;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.scribe.Scribe;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.ScribeImpl;
import rice.p2p.scribe.Topic;
import rice.pastry.commonapi.PastryIdFactory;
import it.eng.paas.message.PaasMessage;
import it.eng.paas.networking.messenger.IP2PMessenger;
import it.eng.paas.networking.rr.impl.RR;
import it.eng.paas.networking.rr.impl.RoutingInfo;
import it.eng.paas.networking.rr.impl.RoutingInfoHibernateUtil;
import it.eng.paas.utilities.PaasUtilities;

public class P2PMessenger implements IP2PMessenger{

	/**
	   * The message sequence number.  Will be incremented after each send.
	   */
	  int seqNum = 0;
	  
	  /** 
	   * My handle to a scribe impl.
	   */
	  Scribe scribe;
	  
	  /**
	   * The only topic this appl is subscribing to.
	   */
	  Topic topic;
	
	  /**
	   * The Endpoint represents the underlieing node.  By making calls on the 
	   * Endpoint, it assures that the message will be delivered to a MyApp on whichever
	   * node the message is intended for.
	   */
	  protected Endpoint endpoint;
	  
	  private static final Logger logger = Logger.getLogger("P2P");
	
	  /**
	   * The constructor for this scribe client.  It will construct the ScribeApplication.
	   * 
	   * @param node the PastryNode
	   */
	  public P2PMessenger(Node node) {
	    this.endpoint = node.buildEndpoint(this, "endpoint");
	    // construct Scribe
	    scribe = new ScribeImpl(node,"scribe");
	
	    // construct the topic, use one and the same as it serves only for broadcasting purposes
	    topic = new Topic(new PastryIdFactory(node.getEnvironment()), "rr");
	    
	    // now we can receive messages
	    endpoint.register();
	  }
	  
	  /**
	   * Subscribes to Topic.
	   */
	  public void subscribe() {
		  //subscribe(Topic, ScribeMultiClient, ScribeContent, NodeHandle hint)
		  /*
		   * topic - The topic to subscribe to
		   * client - The client to give messages to
		   * content - The content to include in the subscribe
		   * hint - The first hop of the message ( Helpful to implement a centralized solution)
		   */
	    scribe.subscribe(topic, this, null, null); 
	  }	  
	  
	  /**
	   * Part of the Application interface.  Will receive PublishContent every so often.
	   */
	  public void deliver(Id id, Message message) {
		  //useless in our context
		  System.out.println("LA DELIVER STRANA :"+message);
		  logger.info("LA DELIVER STRANA :"+message);
	  }
	  
	  /**
	   * Sends the multicast message.
	   */
	  public void sendMulticast(String content, PaasMessage type) {
	    RRMessage msg = new RRMessage(endpoint.getLocalNodeHandle(), null, seqNum, content, type);
	    scribe.publish(topic, msg); 
	    seqNum++;
	  }
	
	  /**
	   * Called whenever we receive a multicast message.
	   */
	  public void deliver(Topic topic, ScribeContent content) {
	    RRMessage msg = (RRMessage)content;
	    //if I sent it, discard
	    if(!msg.getFrom().equals(endpoint.getLocalNodeHandle())){
		    PaasMessage type = msg.getType();
		    switch(type){
			    case TOPIC_START_APP:{
			    	RR.getInstance().addStartAppMessage(msg.getXMLContent());
			    	break;
			    }
			    case TOPIC_STOP_APP:{
			    	RR.getInstance().addStopAppMessage(msg.getXMLContent());
			    	break;
			    }
			    default:{
			    	System.err.println("received unknown message type:"+type.getText()+" message: "+msg.getContent());
			    	
			    }
		    }
		    System.out.println("received message from: "+msg.getFrom()+" msg: "+msg.getContent());
		    logger.info("received message from: "+msg.getFrom()+" msg: "+msg.getContent());
	    }
	    
	  }
	
	  /**
	   * Sends an anycast message.
	   */
	  public void sendAnycast(String content, PaasMessage type) {
	    RRMessage myMessage = new RRMessage(endpoint.getLocalNodeHandle(), null, seqNum, content, type);
	    scribe.anycast(topic, myMessage); 
	    seqNum++;
	  }
	  
	  public void sendAnycast(String content, NodeHandle to, PaasMessage type){
		  RRMessage myMessage = new RRMessage(endpoint.getLocalNodeHandle(), to, seqNum, content, type);
		  scribe.anycast(topic, myMessage); 
		  seqNum++;
	  }
	  
	  /**
	   * Called when we receive an anycast.  If we return
	   * false, it will be delivered elsewhere.  Returning true
	   * stops the message here.
	   */
	  public boolean anycast(Topic topic, ScribeContent content) {
		  RRMessage msg = (RRMessage)content;
		  //if message is for me (I did not send it)
		  if(!msg.getFrom().equals(endpoint.getLocalNodeHandle())){
			  PaasMessage type = msg.getType();
			  switch(type){
			    case TOPIC_SERVICE_ADDRESS:{
			    	//reply to service address request from other rrs
			    	return serviceAddressAnycastReply(msg.getXMLContent(), msg.getFrom());
			    }
			    case REPLY_ADDRESS_FOUND:{
			    	//update routing table
			    	addServiceAddress(msg.getXMLContent());
			    	return true;
			    }
			    default:{
			    	System.err.println("received unknown message type:"+type.getText()+" message: "+msg.getContent());
			    	logger.error("received unknown message type:"+type.getText()+" message: "+msg.getContent());
			    	return false;
			    }
			  }
		  }
		  else return false;
	  }
	  
	  public void addServiceAddress(Document message) {
			Hashtable<String, String> table = PaasUtilities.XML2Hashtable(message);
			//IP1:clientPORT1:consolePORT1;...;IPN:clientPORTN:consolePORTN
			String [] splitted = table.get("addresses").split(";");
			for(int i=0; i< splitted.length; i++){
				addServiceAddress(table.get("service"), splitted[i]);
			}
		}
	  
	  public void addServiceAddress(String service, String location){
		  	String [] splitted = location.split(":");
		  	//IP - clientPORT - consolePORT
			RoutingInfo r = new RoutingInfo(service, splitted[0], splitted[1], splitted[2]);
			if (!RoutingInfoHibernateUtil.checkRoutingAlreadyPresent(r))RoutingInfoHibernateUtil.saveRoutingInfo(r);
		}
	  
	  private boolean serviceAddressAnycastReply(Document message, NodeHandle from){
		  Hashtable<String, String> read = PaasUtilities.XML2Hashtable(message);
			//get service name
			String service = read.get("service");
			List<RoutingInfo> hosts = RoutingInfoHibernateUtil.getRouting(service);
			if(hosts!=null){
				String reply = "";
				for(RoutingInfo r : hosts){
					reply+=r.getIp()+":"+r.getClientPort()+":"+r.getConsolePort()+";";
				}
				Document additionalInfo = PaasUtilities.createBaseXML("service_addresses");
				PaasUtilities.addXMLnode(additionalInfo, "service", service);
				PaasUtilities.addXMLnode(additionalInfo, "addresses", reply);
				sendAnycast(PaasUtilities.XML2String(additionalInfo), from, PaasMessage.REPLY_ADDRESS_FOUND);
				return true;
			}
			return false;
	  }
	  
	  //Useless
	@Override
	public boolean forward(RouteMessage arg0) {
		return false;
	}

	@Override
	public void update(NodeHandle arg0, boolean arg1) {		
	}

	@Override
	public void childAdded(Topic arg0, NodeHandle arg1) {
	}

	@Override
	public void childRemoved(Topic arg0, NodeHandle arg1) {
	}

	@Override
	public void subscribeFailed(Topic arg0) {
	}

	@Override
	public void subscribeFailed(Collection<Topic> arg0) {
	}

	@Override
	public void subscribeSuccess(Collection<Topic> arg0) {
	}	
}
