package it.eng.paas.networking.messenger;


import it.eng.paas.message.PaasMessage;
import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.ScribeMultiClient;
import rice.p2p.scribe.Topic;

public interface IP2PMessenger extends Application, ScribeMultiClient{
	void subscribe();
	public void deliver(Id id, Message message);
	void sendMulticast(String content, PaasMessage type);
	void deliver(Topic topic, ScribeContent content);
	void sendAnycast(String content, PaasMessage type);
	boolean anycast(Topic topic, ScribeContent content);
}
