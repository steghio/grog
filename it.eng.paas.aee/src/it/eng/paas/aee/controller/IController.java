package it.eng.paas.aee.controller;

import org.w3c.dom.Document;

public interface IController{

	void run();
	Boolean isAlive();
	void addAcceptAppMessage(Document message);
	void addStartAppMessage(Document message);
	void addStopAppMessage(Document message);
	void addScaleAppMessage(Document message);
}
