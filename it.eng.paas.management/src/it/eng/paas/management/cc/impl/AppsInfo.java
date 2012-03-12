package it.eng.paas.management.cc.impl;

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
@Table(name = "apps")
@NamedQueries(value = {
		@NamedQuery(name = "getApps", query = "select a from AppsInfo a"),
		@NamedQuery(name = "getAppLocation", query = "select a.location from AppsInfo a where a.appID = ?"),
		@NamedQuery(name = "checkAppLocationAlreadyPresent", query = "select count(a) from AppsInfo a where a.appID = ? and a.location = ?"),
		@NamedQuery(name = "isDeadApp", query = "select count(a) from AppsInfo a"),
		@NamedQuery(name = "deleteAppsInfoID", query = "delete from AppsInfo a where a.appID = ?"),
		@NamedQuery(name = "deleteAppsInfoLocation", query = "delete from AppsInfo a where a.appID = ? and a.location = ?")
		})
public class AppsInfo implements Serializable{
	
	private static final long serialVersionUID = -4446570990612581526L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private long id;
	@Column(name = "appID")
	private String appID;
	@Column(name = "location")
	private String location;
	
	public long getId() {
		return id;
	}


	public void setId(long id) {
		this.id = id;
	}


	public String getAppID() {
		return appID;
	}


	public void setAppID(String appID) {
		this.appID = appID;
	}


	public String getLocation() {
		return location;
	}


	public void setLocation(String location) {
		this.location = location;
	}
	
	
	
	/*constructors*/
	
	//for hibernate
	public AppsInfo(){
		
	}
	
	
	public AppsInfo(String appID, String location){
		this.appID = appID;
		this.location = PaasUtilities.UNIXfyPath(location);
	}
	
	public Document toXML(String root){
		Document doc = PaasUtilities.createBaseXML(root);
		PaasUtilities.addXMLnode(doc, "appID", appID);
		PaasUtilities.addXMLnode(doc, "location", location);
		return doc;
	}
	
	public AppsInfo fromXML(Document doc){
		Hashtable<String, String> fields = PaasUtilities.XML2Hashtable(doc);
		try{
			String appID = fields.get("appID");
			String location = fields.get("location");
			if(appID==null || location == null)return null;
			return new AppsInfo(appID, location);
		}catch(Exception e){
			return null;
		}
	}

}

