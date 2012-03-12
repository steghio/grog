package it.eng.paas.networking.rr.impl;

import it.eng.paas.utilities.PaasUtilities;

import java.io.Serializable;
import java.util.Hashtable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.w3c.dom.Document;

@Entity
@Table(name = "routing")
@NamedQueries(value = {
		@NamedQuery(name = "getRouting", query = "select r from RoutingInfo r where r.appID = ?"),
		@NamedQuery(name = "checkRoutingAlreadyPresent", query = "select count(r) from RoutingInfo r where r.appID = ? and r.ip = ? and r.consolePort = ?"),
		@NamedQuery(name = "checkRoutingAlreadyPresentNoPort", query = "select count(r) from RoutingInfo r where r.appID = ? and r.ip = ?"),
		@NamedQuery(name = "isDead", query = "select count(r) from RoutingInfo r"),
		@NamedQuery(name = "getRoutingInfos", query = "select r from RoutingInfo r"),
		@NamedQuery(name = "deleteRoutingInfoID", query = "delete from RoutingInfo r where r.id = ?"),
		@NamedQuery(name = "deleteRoutingInfoIP", query = "delete from RoutingInfo r where r.ip = ?"),
		@NamedQuery(name = "deleteRoutingInfoPort", query = "delete from RoutingInfo r where r.ip = ? and r.consolePort = ?"),
		@NamedQuery(name = "deleteRoutingInfoApp", query = "delete from RoutingInfo r where r.appID = ?")
		})
public class RoutingInfo implements Serializable{
	
	private static final long serialVersionUID = -4446570990612581526L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private Long id;
	@Column(name = "appID")
	private String appID;
	@Column(name = "ip")
	private String ip;
	@Column(name = "clientPort")
	private String clientPort;
	@Column(name = "consolePort")
	private String consolePort;
	
	/*getters & setters*/
	public Long getID(){
		return id;
	}
	
	public String getAppID(){
		return appID;
	}
	
	public String getIp(){
		return ip;
	}
	
	public String getClientPort(){
		return clientPort;
	}
	
	public String getConsolePort(){
		return consolePort;
	}
	
	public void setAppID(String appID){
		this.appID = appID;
	}
	
	public void setClientPort(String clientPort){
		this.clientPort = clientPort;
	}
	
	public void setConsolePort(String consolePort){
		this.consolePort = consolePort;
	}
	
	public void setIp(String ip){
		this.ip = ip;
	}
	
	public void setId(Long id){
		this.id = id;
	}
	/*constructors*/
	
	//for hibernate
	public RoutingInfo(){
		
	}
	
	
	public RoutingInfo(String appID, String ip, String clientPort, String consolePort){
		this.appID = appID;
		this.ip = ip;
		//use 0 as default port, we use it only when STOP_APP_ALL, else they're never null
		this.clientPort = (clientPort==null) ? "0" : clientPort;
		this.consolePort = (consolePort==null) ? "0" : consolePort;
	}
	
	public Document toXML(String root){
		Document doc = PaasUtilities.createBaseXML(root);
		PaasUtilities.addXMLnode(doc, "appID", appID);
		PaasUtilities.addXMLnode(doc, "IP", ip);
		PaasUtilities.addXMLnode(doc, "clientPort", clientPort);
		PaasUtilities.addXMLnode(doc, "consolePort", consolePort);
		return doc;
	}
	
	public RoutingInfo fromXML(Document doc){
		Hashtable<String, String> fields = PaasUtilities.XML2Hashtable(doc);
		try{
			String appID = fields.get("appID");
			String ip = fields.get("IP");
			String clientPort = fields.get("clientPort");
			String consolePort = fields.get("consolePort");
			if(appID==null || ip==null || clientPort==null || consolePort==null)return null;
			return new RoutingInfo(appID, ip, clientPort, consolePort);
		}catch(Exception e){
			return null;
		}
	}

}

