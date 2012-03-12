package it.eng.paas.message;

import it.eng.paas.utilities.PaasUtilities;

public enum PaasMessage {
	TOPIC_ACCEPT_APP("acceptApp"),
	TOPIC_START_APP("startApp"),
	TOPIC_STOP_APP("stopApp"),
	TOPIC_SCALE_APP("scaleApp"),
	TOPIC_SERVICE_ADDRESS("serviceAddress"),
	TOPIC_HEARTBEAT("heartBeat"),
	REPLY_APP_ALREADY_PRESENT("appAlreadyPresent"),
	REPLY_APP_ACCEPTABLE("appAcceptable"),
	REPLY_APP_DEPLOY_FAIL("appDeployFAIL"),
	REPLY_APP_START_OK("appStartOK"),
	REPLY_APP_START_FAIL("appStartFAIL"),
	REPLY_APP_STOP_OK("appStopOK"),
	REPLY_APP_STOP_FAIL("appStopFAIL"),
	REPLY_APP_SCALE_OK("appScaleOK"),
	REPLY_APP_SCALE_FAIL("appScaleFAIL"),
	REPLY_ADDRESS_FOUND("addressFound"),
	REPLY_ADDRESS_NOT_FOUND("addressNotFound"),
	COMPONENT_ALIVE_AEE("aliveAee"),
	COMPONENT_ALIVE_RR("aliveRR"),
	COMPONENT_ALIVE_CC("aliveCC"),
	COMPONENT_WHO_ALIVE("whoAlive"),
	REQUEST_START_AEE("startAee"),
	REQUEST_START_CC("startCC"),
	REQUEST_START_RR("startRR"),
	REQUEST_START_PO("startPO");
	
	private String text;

	  PaasMessage(String text) {
	    this.text = text;
	  }

	  public String getText() {
	    return this.text;
	  }
	  
	  public static PaasMessage fromString(String text) {
		  if (text != null) {
			  for (PaasMessage m : PaasMessage.values()) {
				  if (text.equalsIgnoreCase(m.text)) {
					  return m;
				  }
		      }
		  }
		  return null;
	  }
	  
	  public static PaasMessage getTopic(String routingKey){
		  //<entity>.<id>.<paas_message>
		  return fromString(PaasUtilities.lastArrayElement(routingKey.split("\\.")));
	  }

}
