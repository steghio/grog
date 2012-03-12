package it.eng.paas.management.cc.impl;

import it.eng.paas.utilities.PaasUtilities;

import java.util.Hashtable;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import org.w3c.dom.Document;

@Entity
@Table(name = "rr")
@NamedQueries(value = {
		@NamedQuery(name = "getRRs", query = "select r from RRInfo r"),
		@NamedQuery(name = "getRR", query = "select r from RRInfo r where r.ip = ?"),
		@NamedQuery(name = "checkRRAlreadyPresent", query = "select count(r) from RRInfo r where r.ip = ?"),
		@NamedQuery(name = "isDeadRR", query = "select count(r) from RRInfo r"),
		@NamedQuery(name = "deleteRRInfoID", query = "delete from RRInfo r where r.id = ?"),
		@NamedQuery(name = "deleteRRInfoIP", query = "delete from RRInfo r where r.ip = ?")
		})
public class RRInfo extends ManagementInfo{
	
	private static final long serialVersionUID = -4446570990612581526L;
	
	/*constructors*/
	
	//for hibernate
	public RRInfo(){
		
	}
	
	
	public RRInfo(String ip){
		super(ip);
	}
	
	public Document toXML(String root){
		Document doc = PaasUtilities.createBaseXML(root);
		PaasUtilities.addXMLnode(doc, "IP", ip);
		return doc;
	}
	
	public RRInfo fromXML(Document doc){
		Hashtable<String, String> fields = PaasUtilities.XML2Hashtable(doc);
		try{
			String ip = fields.get("IP");
			if(ip==null)return null;
			return new RRInfo(ip);
		}catch(Exception e){
			return null;
		}
	}

}

