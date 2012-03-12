package it.eng.paas.aee.runner;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.w3c.dom.Document;

import it.eng.paas.aee.runner.impl.Sandbox;

public interface IRunner{

	Collection<Sandbox> getSandboxes();
	Sandbox getSandbox(String sID);
	List<String> getAppSandboxes(String appID);
	Hashtable<String, List<String>> getServiceAddresses();
	void addServiceAddress(String service, String location);
	void addServiceAddress(Document message);
	void removeServiceAddress(String service, String location);
	void removeService(String service);
	Boolean hasServiceAddress(String service);
	List<String> getServiceAddress(String service);
	int howManyInstancesRunning(String appID);
 	Sandbox startApp(String appID);
	Boolean stopAppAll(String appID);
	Boolean stopApp(String appID, String sID);
	Boolean scaleApp(String appID, int instances);
	Boolean isRunningApp(String appID);
	Boolean isRunningSandboxes();
	Boolean isRunningSandbox(String sID);
	Boolean isAlive();
	Boolean isAppAlive(String consolePort, String appID);
	void clearSandboxes();
	Boolean deployApp(String location, String appID);
	Boolean isAppDeployed(String appID);
	void requestServiceAddresses(String manifest);
}
