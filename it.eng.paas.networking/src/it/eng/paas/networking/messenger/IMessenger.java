package it.eng.paas.networking.messenger;

import org.w3c.dom.Document;
import it.eng.paas.message.PaasMessage;

public interface IMessenger{

	void sendHeartBeat();
	Boolean isAlive();
	void sendMessage(PaasMessage topic, PaasMessage reply, String replyTo, Document additionalInfo);
	void notifyRRStart();
}
