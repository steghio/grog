package test;

import it.eng.paas.message.PaasMessage;
import it.eng.paas.utilities.PaasUtilities;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import org.w3c.dom.Document;

public class Telnet {
	
	
	static Socket client;
	static PrintWriter out;
	
	public static void startModule(){
		try{
			//CAMBIA CLIENT!
			//75 ubuntu
			client = new Socket("192.168.23.75", 9999);
			OutputStream a = client.getOutputStream();
			out = new PrintWriter(a, true);
			Document doc = PaasUtilities.createBaseXML("request");
			PaasUtilities.addXMLnode(doc, "type", PaasMessage.REQUEST_START_AEE.getText());
			Document asd = PaasUtilities.file2XML(new File("C:/temp/junit/aee.properties"), "test");
//			PaasUtilities.addXMLnode(doc, "type", PaasMessage.REQUEST_START_RR.getText());
//			Document asd = PaasUtilities.file2XML(new File("C:/paas/networking/rr.properties"), "test");
			PaasUtilities.addXMLnode(doc, "propfile", PaasUtilities.XML2String(asd));
			System.out.println(PaasUtilities.XML2String(doc));
			out.println(PaasUtilities.XML2String(doc));
			out.close();
			a.close();
			client.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void startApp(){
		try{
			//CAMBIA CLIENT!
			//75 ubuntu
			client = new Socket("192.168.23.28", 6666);
			OutputStream a = client.getOutputStream();
			out = new PrintWriter(a, true);
			Document doc = PaasUtilities.createBaseXML("acceptApp");
			PaasUtilities.addXMLnode(doc, "appID", "it.eng.test.remote.ds.helloconsumer");
			Document asd = PaasUtilities.file2XML(new File("C:/temp/it.eng.test.remote.ds.helloconsumer.MF"), "manifest");
			PaasUtilities.addXMLnode(doc, "manifest", PaasUtilities.XML2String(asd));
			System.out.println(PaasUtilities.XML2String(doc));
			out.println(PaasUtilities.XML2String(doc));
			out.close();
			a.close();
			client.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		
		
		startApp();
		
	}
}
