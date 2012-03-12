package it.eng.paas.management.cc.impl;

import it.eng.paas.utilities.PaasUtilities;

import java.util.Hashtable;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.w3c.dom.Document;

@Entity
@Table(name = "cc")
@NamedQueries(value = {
		@NamedQuery(name = "getCCs", query = "select c from CCInfo c"),
		@NamedQuery(name = "getCC", query = "select c from CCInfo c where c.ip = ?"),
		@NamedQuery(name = "checkCCAlreadyPresent", query = "select count(c) from CCInfo c where c.ip = ?"),
		@NamedQuery(name = "isDeadCC", query = "select count(c) from CCInfo c"),
		@NamedQuery(name = "deleteCCInfoID", query = "delete from CCInfo c where c.id = ?"),
		@NamedQuery(name = "deleteCCInfoIP", query = "delete from CCInfo c where c.ip = ?")
		})
public class CCInfo extends ManagementInfo{
	
	private static final long serialVersionUID = -4446570990612581526L;	
	
	/*constructors*/
	
	//for hibernate
	public CCInfo(){
		
	}
	
	
	public CCInfo(String ip){
		super(ip);
	}
	
	public Document toXML(String root){
		Document doc = PaasUtilities.createBaseXML(root);
		PaasUtilities.addXMLnode(doc, "IP", ip);
		return doc;
	}
	
	public CCInfo fromXML(Document doc){
		Hashtable<String, String> fields = PaasUtilities.XML2Hashtable(doc);
		try{
			String ip = fields.get("IP");
			if(ip==null)return null;
			return new CCInfo(ip);
		}catch(Exception e){
			return null;
		}
	}

}

