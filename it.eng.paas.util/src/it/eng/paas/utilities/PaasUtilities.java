package it.eng.paas.utilities;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import java.net.ServerSocket;
import java.net.URI;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;

public class PaasUtilities {

	/**
	 * Unzips file to destination
	 * @param file the file to unzip
	 * @param destination the path where to unzip
	 * @return true if successful
	 */
	public static Boolean unZip(String zipFile, String destination){
		try {
			int BUFFER = 2048;
		    File file = new File(zipFile);
		    ZipFile zip = new ZipFile(file);
		    Enumeration<? extends ZipEntry> zipFileEntries = zip.entries();
		    // Process each entry
		    while (zipFileEntries.hasMoreElements()){
		        // grab a zip file entry
		        ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
		        String currentEntry = entry.getName();
		        File destFile = new File(destination, currentEntry);
		        //destFile = new File(destination, destFile.getName());
		        File destinationParent = destFile.getParentFile();
		        // create the parent directory structure if needed
		        destinationParent.mkdirs();
		        if (!entry.isDirectory()){
		            BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
		            int currentByte;
		            // establish buffer for writing file
		            byte data[] = new byte[BUFFER];
		            // write the current file to disk
		            FileOutputStream fos = new FileOutputStream(destFile);
		            BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
		            // read and write until last byte is encountered
		            while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
		                dest.write(data, 0, currentByte);
		            }
		            dest.flush();
		            dest.close();
		            is.close();
		        }
		    }
		} catch (Exception e) {
			//logger.fatal("error while unzipping "+zipFile+" to "+destination+" message "+e.getMessage(), e);
			System.err.println("error while unzipping "+zipFile+" to "+destination+" message "+e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Finds a free port and returns it. 
	 * The time between the check and the actual binding may not ensure that the port remains free
	 * @return port
	 */
	public static String findFreePort(){
		int port = 0;
		try(ServerSocket server = new ServerSocket(0);){
			port = server.getLocalPort();
//			server.close();
		}catch(Exception e){
			System.err.println("unable to find a free port");
			return null;
		}
		return String.valueOf(port);
	}

	/**
	 * Checks if the current JVM instance runs on Windows
	 * @return true if windows
	 */
	public static Boolean isWindowsOS(){
		String s = System.getProperty("os.name").toLowerCase();
		return s.contains("windows");
	}
	
	/**
	 * Converts path from UNIX to Windows / is replaced with \
	 * @param path
	 * @return the converted path
	 */
	public static String path4OS(String path){
		if(isWindowsOS()){
			return path.replace("/", File.separator);
		}
		return path;
	}
	
	/**
	 * Converts path into UNIX path, \ is replaced with /
	 * @param path
	 * @return the converted path
	 */
	public static String UNIXfyPath(String path){
		return path.replace("\\", "/");
	}

	/**
	 * Deletes all files and subdirectories under dir.
	 * @param dir the directory to be deleted
	 * @return true if all deletions were successful, if one fails it stops and returns false
	 */
	public static boolean deleteDir(File directory) {
		if (directory == null){
			System.err.println("dir is null");
			return false;
		}
		if (!directory.exists()){
			System.err.println("dir does not exist "+directory.getPath());
			return false;
		}
	  	if (!directory.isDirectory()){
	  		System.err.println("dir is not dir "+directory.getPath());
	  		return false;
	  	}
	  	String[] list = directory.list();
	  	// Some JVMs return null for File.list() when the
	  	// directory is empty.
	  	if (list != null) {
	  		for (int i = 0; i < list.length; i++) {
	  			File entry = new File(directory, list[i]);
	  			if (entry.isDirectory()){
	  				if (!deleteDir(entry)){
	  					System.err.println("error while removing dir "+entry.getPath());
	  					return false;
	  				}
	  			}
	  			else{
	  				if (!entry.delete()){
	  					System.err.println("error while removing dir "+entry.getPath());
	  					return false;
	  				}
	  			}
	  			System.out.println("deleted "+entry.getPath());
	  		}
	  	}
	  	return directory.delete();
	}
	
	/**
	 * Returns the java process PID running the sandbox sID
	 * Uses jps and filters on that string
	 * @param sID
	 * @return the process PID as a String or null if error
	 */
	public static String getSandboxPID(String sID){
		String [] cmd = new String[7];
		if(isWindowsOS()){
			cmd[0] = "cmd";
			cmd[1] = "/C";
			cmd[2] = "jps";
			cmd[3] = "|";
			cmd[4] = "find";
			cmd[5] = "/I";
			cmd[6] = "\""+sID+"\"";
		}
		else{
			cmd[0] = "/bin/bash";
			cmd[1] = "-c";
			cmd[2] = "jps";
			cmd[3] = "|";
			cmd[4] = "grep";
			cmd[5] = "-i";
			cmd[6] = sID;
		}
		ProcessBuilder pb = new ProcessBuilder(cmd);
		//start the process and store it
		Process p;
		try {
			p = pb.start();
		} catch (Exception e) {
			System.err.println("error when executing jps | find "+sID);
			return null;
		}
	 	String line;
		try{
			InputStream is = p.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			//only one line will be returned as the sID is unique inside the host
			line = br.readLine();
			br.close();
			isr.close();
			is.close();
		} catch (Exception e) {
			System.err.println("error when reading output from jps | find "+sID);
			return null;
		}
	 	String [] splitted = line.split(" ");
	 	//0 is PID, 1 is sandboxName.jar
	 	return splitted[0];
	}
	
	public static void killPID(String PID){
		if(isWindowsOS()){
			String [] cmd = {"taskkill", "/F", "/PID", PID };
			ProcessBuilder pb = new ProcessBuilder(cmd);
			try {
				pb.start();
			} catch (Exception e) {
				System.err.println("failed to kill process "+PID);
			}
		}
		else{
			String [] cmd = {"kill", "-9", PID };
			ProcessBuilder pb = new ProcessBuilder(cmd);
			try {
				pb.start();
			} catch (Exception e) {
				System.err.println("failed to kill process "+PID);
			}
		}
	}
	
	public static <T> T lastArrayElement(T[] array) {
	    return array[array.length - 1];
	}
	
	public static URI string2URI(String string){
		return new File(string).toURI();
	}
	
	public static boolean copyFile(String source, String destination){
		try{
			Path sourcePath = Paths.get(string2URI(source));
			Path destPath = Paths.get(string2URI(destination));
			Files.copy(sourcePath, destPath, LinkOption.NOFOLLOW_LINKS);
			return true;
		}catch(Exception e){
			System.err.println("Cannot move file "+source+" to "+destination);
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean copyFile(URI source, String destination){
		try{
			Path sourcePath = Paths.get(source);
			Path destPath = Paths.get(string2URI(destination));
			Files.copy(sourcePath, destPath, LinkOption.NOFOLLOW_LINKS);
			return true;
		}catch(Exception e){
			System.err.println("Cannot move file "+source.toString()+" to "+destination);
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean copyFileFromURL(String URL, String destination, String user, String pwd){
		Sardine sardine = SardineFactory.begin(user, pwd);
		try {
			if(!sardine.exists(URL)){
				System.out.println("File "+URL+" does not exist");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		try(InputStream is = sardine.get(URL);){
			Path destPath = Paths.get(string2URI(destination));
			Files.copy(is, destPath, StandardCopyOption.REPLACE_EXISTING);
			return true;
		}catch(Exception e){
			System.err.println("Cannot move file "+URL+" to "+destination+" user: "+user+" pwd: "+pwd);
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Gathers system info on os, memory and processors. Values in KB
	 * @return Hashtable with info inside, keys are:
	 * -Memory-
	 * RAM - total RAM available
	 * swap - total swap available
	 * -CPU-
	 * MHz
     * cores (logical)
     * socket -physical CPUs
     * socketCores - cores per socket (only if socket>1)
     * cache (if available)
     * -OS-
     * fqdn - fully qualified domain name hostname.acme.com
     * hostname (only if !=fqdn)
     * -System.getProperty(...)-
     * arch 
     * os
	 */
	public static Hashtable<String, String> getSystemInfo(){
		MemoryInfo mem = new MemoryInfo();
		CpuInfo cpu = new CpuInfo();
		//if initialization went bad
		if(!mem.isOk() || !cpu.isOk())return null;
		Hashtable<String, String> sysInfo = new Hashtable<>();
		double ram = Integer.parseInt(mem.getTotal())/1024;
		sysInfo.put("RAM", Double.toString(ram));
		sysInfo.put("swap", mem.getSwapTotal());
		sysInfo.putAll(cpu.getCPUInfo());
		sysInfo.putAll(OsInfo.getOsInfo());
		return sysInfo;
	}
	
	/**
	 * Gathers system memory info, values in KB
	 * @return Hashtable with info inside, keys are
	 * usedRAM
	 * freeRAM
	 * actualUsedRAM - including buffers and cache
	 * actualFreeRAM - including buffers and cache
	 * swapUsed
	 * swapFree
	 */
	public static Hashtable<String, String> getRAMInfo(){
		MemoryInfo mem = new MemoryInfo();
		CpuInfo cpu = new CpuInfo();
		//if initialization went bad
		if(!mem.isOk() || !cpu.isOk())return null;
		Hashtable<String, String> memInfo = new Hashtable<>();
		memInfo.put("usedRAM", mem.getUsed());
		memInfo.put("freeRAM", mem.getFree());
		memInfo.put("actualUsedRAM", mem.getActualUsed());
		memInfo.put("actualFreeRAM", mem.getActualFree());
		memInfo.put("swapUsed", mem.getSwapUsed());
		memInfo.put("swapFree", mem.getSwapFree());
		return memInfo; 
	}
	
	/**
	 * Show current system CPU usage as sys+user+nice+wait %
	 * @return -1 if error or int percentage usage value (no rounding)
	 */
	public static int getCPUUsage(){
		CpuInfo cpu = new CpuInfo();
		//if initialization went bad
		if(!cpu.isOk())return -1;
		return (int)cpu.getCPUUsage();
	}
	
	/**
	 * Show current RAM usage as actualUsedRAM (cache and buffer included) / totalRAM %
	 * @return -1 if error or int percentage usage value (no rounding)
	 */
	public static int getRAMUsage(){
		MemoryInfo mem = new MemoryInfo();
		//if initialization went bad
		if(!mem.isOk())return -1;
		return (int)(Double.parseDouble(mem.getActualUsed())/Double.parseDouble(mem.getTotal())*100);
	}
	
	public static double getCurrentlyAvailableRAM(){
		MemoryInfo mem = new MemoryInfo();
		//if initialization went bad
		if(!mem.isOk())return -1;
		return Double.parseDouble(mem.getActualUsed())/1024;
	}
	
	/**
	 * Create XML from Hashtable<String, String> considering every element as depth 0 <key>value</key>
	 * @param root - name of the root element
	 * @param body - Hashtable<String, String> key, value. No nesting allowed
	 * @return null if failure otherwise the XML document populated
	 */
	public static Document hashtable2XML(String root, Hashtable<String, String> body){
		Document document = createBaseXML(root);
		if(document==null){
			System.err.println("Error while creating XML file");
			return null;
		}
		Element rootElement = document.getDocumentElement();
		for (String key : body.keySet()){
			Element em = document.createElement(key);
			em.appendChild(document.createTextNode(body.get(key)));
			rootElement.appendChild(em);
		}
		return document;
	}
	
	/**
	 * Create Hashtable<String,String> from given XML <root><key1>value1</key1>...<keyN>valueN</keyN></root>
	 * values are text only
	 * @param xml document to parse
	 * @return null if empty document otherwise Hashtable<String, String> populated from XML discarding root
	 */
	public static Hashtable<String, String> XML2Hashtable(Document xml){
        xml.getDocumentElement().normalize();
        //get children of root <message>
        NodeList nodes = xml.getDocumentElement().getChildNodes();
        //if empty
        if(nodes.getLength()==0)return null;
        Hashtable<String, String> hash = new Hashtable<>();
        for(int i=0; i<nodes.getLength(); i++){
        	//every node is depth 0 <key>string_value</key>
        	hash.put(nodes.item(i).getNodeName(), nodes.item(i).getTextContent());
        }
        return hash;
	}
	
	/**
	 * Create empty XML document with given root
	 * @param root
	 * @return null if failure, Document otherwise
	 */
	public static Document createBaseXML(String root){
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder;
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (Exception e) {
			System.err.println("Error while creating XML file "+e.getMessage());
			e.printStackTrace();
			return null;
		}
		Document document = documentBuilder.newDocument();
		Element rootElement = document.createElement(root);
		document.appendChild(rootElement);
		return document;
	}
	
	/**
	 * Adds a node to a given XML document
	 * @param doc - the document to modify
	 * @param name - the node name
	 * @param value - the node value as an Object, it will be saved as String
	 */
	public static void addXMLnode(Document doc, String name, Object value){
		Element rootElement = doc.getDocumentElement();
		Element em = doc.createElement(name);
		em.appendChild(doc.createTextNode(value.toString()));
		rootElement.appendChild(em);
	}
	
	/**
	 * Serializes the given document to String
	 * @param doc
	 * @return null if error
	 */
	public static String XML2String(Document doc){
		StringWriter stw = new StringWriter();
		try{
		    Transformer serializer = TransformerFactory.newInstance().newTransformer();
		    serializer.transform(new DOMSource(doc), new StreamResult(stw));
		}catch(Exception e){
			System.err.println("error while transforming document to string "+e.getMessage());
			e.printStackTrace();
			return null;
		}
	    return stw.toString(); 
	}
	
	/**
	 * Deserializes given String into XML Document
	 * @param str
	 * @return null if error
	 */
	public static Document string2XML(String str){
		DOMParser parser = new DOMParser();
		try {
			parser.parse(new InputSource(new java.io.StringReader(str)));
		} catch (Exception e) {
			System.err.println("error while transforming string "+str+" to document "+e.getMessage());
			e.printStackTrace();
			return null;
		}
		return parser.getDocument();
	}
	
	/**
	 * Generate a random UUID in the form IDUUID
	 * @param ID not mandatory, if not wanted set null or ""
	 * NEVER pass ID as a String containing dots, it will create problems with RabbitMQ routing policy
	 * @return UUID.toString
	 */
	public static String generateUUID(String ID){
		if(ID == null || ID.equals(""))return UUID.randomUUID().toString();
		return ID+UUID.randomUUID().toString();
	}
	
	public static String sdotIP(String IP){
		return IP.replace(".", "");
	}
	
	/**
	 * Merge two lists together with no duplicates
	 * @param list1
	 * @param list2
	 * @return merged list
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<String> mergeNoDuplicates(List<String> list1, List<String> list2){
		if(list1 == null || list1.isEmpty())return list2;
		if(list2 == null || list2.isEmpty())return list1;
		Set setboth = new HashSet(list1);
		setboth.addAll(list2);
		list1.clear();
		list1.addAll(setboth);
		return list1;
	}
	
	public static String getXMLroot(Document doc){
		return doc.getDocumentElement().getNodeName();
	}
	
	public static Document file2XML(File f, String XMLroot){
		Properties props = new Properties();
		Document doc = null;
		try(FileInputStream fis = new FileInputStream(f); ){
			props.load(fis);
			doc = createBaseXML(XMLroot);
			for(Object key : props.keySet()){
				PaasUtilities.addXMLnode(doc, (String)key, props.getProperty((String)key));
			}
		}catch(Exception e){
			System.out.println("error while reading file"+f.getAbsolutePath());
			return null;
		}
		return doc;
	}
	
	public static Hashtable<String, String> string2Hashtable(String string){
		return XML2Hashtable(string2XML(string));
	}
	
	public static Hashtable<String, String> badString2Hashtable(String string){
		Properties props = new Properties();
		Hashtable<String, String> hash = new Hashtable<>();
		try(FileInputStream fis = new FileInputStream(string); ){
			props.load(fis);
			for(Object key : props.keySet()){
				hash.put((String)key, (String)props.get(key));
			}
		}catch(Exception e){
			System.out.println("error while reading file"+string);
			return null;
		}
		return hash;
	}
}
