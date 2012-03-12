package it.eng.paas.management.cc.impl;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public class ManagementInfo implements Serializable{
	
	private static final long serialVersionUID = -4446570990612581526L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	protected Long id;
	@Column(name = "ip")
	protected String ip;
	
	/*getters & setters*/
	public Long getID(){
		return id;
	}
	
	public String getIp(){
		return ip;
	}
	
	public void setIp(String ip){
		this.ip = ip;
	}
	
	public void setId(Long id){
		this.id = id;
	}
	/*constructors*/
	
	//for hibernate
	public ManagementInfo(){
		
	}
	
	public ManagementInfo(String ip){
		this.ip = ip;
	}

}

