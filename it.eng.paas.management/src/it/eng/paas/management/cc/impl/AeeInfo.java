package it.eng.paas.management.cc.impl;

import it.eng.paas.utilities.PaasUtilities;

import java.util.Hashtable;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.w3c.dom.Document;

@Entity
@Table(name = "aee")
@NamedQueries(value = {
		@NamedQuery(name = "getAees", query = "select a from AeeInfo a"),
		@NamedQuery(name = "getAee", query = "select a from AeeInfo a where a.ip = ?"),
		@NamedQuery(name = "checkAeeAlreadyPresent", query = "select count(a) from AeeInfo a where a.ip = ?"),
		@NamedQuery(name = "isDeadAee", query = "select count(a) from AeeInfo a"),
		@NamedQuery(name = "deleteAeeInfoID", query = "delete from AeeInfo a where a.id = ?"),
		@NamedQuery(name = "deleteAeeInfoIP", query = "delete from AeeInfo a where a.ip = ?")
		})
public class AeeInfo extends ManagementInfo{
	
	private static final long serialVersionUID = -4446570990612581526L;
	
	/*constructors*/
	
	//for hibernate
	public AeeInfo(){
		
	}
	
	
	public AeeInfo(String ip){
		super(ip);
	}
	
	public Document toXML(String root){
		Document doc = PaasUtilities.createBaseXML(root);
		PaasUtilities.addXMLnode(doc, "IP", ip);
		return doc;
	}
	
	public AeeInfo fromXML(Document doc){
		Hashtable<String, String> fields = PaasUtilities.XML2Hashtable(doc);
		try{
			String ip = fields.get("IP");
			if(ip==null)return null;
			return new AeeInfo(ip);
		}catch(Exception e){
			return null;
		}
	}

}

