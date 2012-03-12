package it.eng.paas.management.messenger.impl;

import org.w3c.dom.Document;

import it.eng.paas.message.PaasMessage;
import it.eng.paas.utilities.PaasUtilities;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.scribe.ScribeContent;

public class CCMessage implements ScribeContent{
	private static final long serialVersionUID = 6022471673433134650L;
	
	private NodeHandle from;
	private NodeHandle to;
	private int seq;
	private String content;
	private PaasMessage type;
	
	public CCMessage(NodeHandle from, NodeHandle to, int seq, String content, PaasMessage type) {
	  this.from = from;
	  this.to = to;
	  this.seq = seq;
	  this.content = content;
	  this.type = type;
	}
	
	public PaasMessage getType(){
		return type;
	}
	
	public String getContent(){
		return content;
	}
	
	public NodeHandle getFrom(){
		return from;
	}
	
	public NodeHandle getTo(){
		return to;
	}
	
	public Document getXMLContent(){
		return PaasUtilities.string2XML(content);
	}
	
	public void setContent(String content){
		this.content=content;
	}
	
	public void setXMLContent(Document content){
		this.content=PaasUtilities.XML2String(content);
	}
	
	public void setType(PaasMessage type){
		this.type = type;
	}
	
	public String toString() {
	 //TODO usa roba piu' utile
	  return "MyScribeContent #"+seq+" from "+from+" content "+content;
	}  
	
}
