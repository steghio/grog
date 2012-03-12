package it.eng.paas.aee.runner.impl;

import it.eng.paas.utilities.PaasUtilities;

import java.io.File;
import java.io.Serializable;
import javax.persistence.*;

@Entity
@Table(name = "sandboxes")
@NamedQueries(value = {
		@NamedQuery(name = "getSandbox", query = "select s from Sandbox s where s.sID = ?"),
		@NamedQuery(name = "isDead", query = "select count(s) from Sandbox s"),
		@NamedQuery(name = "getSandboxes", query = "select s from Sandbox s"),
		@NamedQuery(name = "deleteSandbox", query = "delete from Sandbox s where s.sID = ?")
		})
public class Sandbox implements Serializable{
	
	private static final long serialVersionUID = -2853745324178701257L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id")
	private Long id;
	@Column(name = "sID")
	private String sID; //this sandbox ID
	@Column(name = "appID")
	private String appID; //ID of the app running her
	@Transient
	private Process p; //external process in which this sandbox runs
	
	@Column(name = "consolePort")
	private String consolePort; //port to telnet osgi console
	@Column(name = "configDir")
	private String configDir; //configuration directory
	@Column(name = "logFile")
	private String logFile;
	@Column(name = "errFile")
	private String errFile;
	@Column(name = "PID")
	private String PID;
	@Column(name = "clientPort")
	private String clientPort;
	
	/*getters & setters*/
	public Long getID(){
		return id;
	}
	
	public String getPID(){
		return PID;
	}
	
	public String getLogFile(){
		return logFile;
	}
	
	public File getFLogFile(){
		return new File(logFile);
	}
	
	public String getErrFile(){
		return errFile;
	}
	
	public File getFErrFile(){
		return new File(errFile);
	}
	
	public String getAppID(){
		return appID;
	}
	
	public String getConfigDir(){
		return configDir;
	}
	
	public String getSID(){
		return sID;
	}
	
	public Process getProcess(){
		return p;
	}
	
	public String getConsolePort(){
		return consolePort;
	}
	
	public String getClientPort(){
		return clientPort;
	}
	
	public void setAppID(String appID){
		this.appID = appID;
	}
	
	public void setSID(String sID){
		this.sID = sID;
	}
	
	public void setProcess(Process p){
		this.p = p;
	}
	
	public void setConsolePort(String consolePort){
		this.consolePort=consolePort;
	}
	
	public void setClientPort(String clientPort){
		this.clientPort=clientPort;
	}
	
	public void setConfigDir(String configDir){
		this.configDir = configDir;
	}
	
	public void setLogFile(String logFile){
		this.logFile = logFile;
	}
	
	public void setErrFile(String errFile){
		this.errFile = errFile;
	}
	
	public void setFLogFile(File logFile){
		this.logFile = logFile.getPath();
	}
	
	public void setFErrFile(File errFile){
		this.errFile = errFile.getPath();
	}
	
	public void setPID(String PID){
		this.PID = PID;
	}
	
	public void setId(Long id){
		this.id = id;
	}
	/*constructors*/
	
	//for hibernate
	public Sandbox(){
		
	}
	
	
	public Sandbox(String appID){
		this.sID = PaasUtilities.generateUUID(null);
		this.appID = appID;
		this.p = null;
		this.consolePort=null;
		this.configDir=null;
		this.PID = null;
		this.clientPort=null;
	}

}
