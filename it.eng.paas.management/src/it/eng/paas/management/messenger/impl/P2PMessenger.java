package it.eng.paas.management.messenger.impl;

import java.util.Collection;

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
import it.eng.paas.management.cc.impl.CC;
import it.eng.paas.management.messenger.IP2PMessenger;

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
	    topic = new Topic(new PastryIdFactory(node.getEnvironment()), "cc");
	    
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
	    CCMessage msg = new CCMessage(endpoint.getLocalNodeHandle(), null, seqNum, content, type);
	    scribe.publish(topic, msg); 
	    seqNum++;
	  }
	
	  /**
	   * Called whenever we receive a multicast message.
	   */
	  public void deliver(Topic topic, ScribeContent content) {
	    CCMessage msg = (CCMessage)content;
	    //if I sent it, discard
	    if(!msg.getFrom().equals(endpoint.getLocalNodeHandle())){
		    PaasMessage type = msg.getType();
		    Document message = msg.getXMLContent();
		    switch(type){
			    case COMPONENT_ALIVE_AEE:{
					CC.getInstance().addAeeStartMessage(message);
					break;
				}
				case COMPONENT_ALIVE_RR:{
					CC.getInstance().addRRStartMessage(message);
					break;
				}
				case COMPONENT_ALIVE_CC:{
					CC.getInstance().addCCStartMessage(message);
					break;
				}
			    default:{
			    	System.err.println("received unknown message type:"+type.getText()+" message: "+msg.getContent());
			    	logger.error("received unknown message type:"+type.getText()+" message: "+msg.getContent());
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
	    CCMessage myMessage = new CCMessage(endpoint.getLocalNodeHandle(), null, seqNum, content, type);
	    scribe.anycast(topic, myMessage); 
	    seqNum++;
	  }
	  
	  public void sendAnycast(String content, NodeHandle to, PaasMessage type){
		  CCMessage myMessage = new CCMessage(endpoint.getLocalNodeHandle(), to, seqNum, content, type);
		  scribe.anycast(topic, myMessage); 
		  seqNum++;
	  }
	  
	  /**
	   * Called when we receive an anycast.  If we return
	   * false, it will be delivered elsewhere.  Returning true
	   * stops the message here.
	   */
	  public boolean anycast(Topic topic, ScribeContent content) {
		  CCMessage msg = (CCMessage)content;
		  //if message is for me (I did not send it)
		  if(!msg.getFrom().equals(endpoint.getLocalNodeHandle())){
			  PaasMessage type = msg.getType();
			  switch(type){
			    //TODO non so nemmeno se serve anycast tra i cc
			    default:{
			    	System.err.println("received unknown message type:"+type.getText()+" message: "+msg.getContent());
			    	logger.error("received unknown message type:"+type.getText()+" message: "+msg.getContent());
			    	return false;
			    }
			  }
		  }
		  else return false;
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
