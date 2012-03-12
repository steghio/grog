package it.eng.paas.aee.runner.impl;

import java.io.File;
import java.util.List;

import it.eng.paas.aee.hibernate.HibernateServiceUtil;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class SandboxHibernateUtil {

	private static EntityManager entityManager;

	public static void initSandboxHibernateUtil() {
		entityManager = HibernateServiceUtil.initEntityManager();
	}

	public static Sandbox saveSandbox(String sID, String appID, String consolePort, String configDir, File logFile, File errFile, String PID, String clientPort) {
		entityManager.getTransaction().begin();
		Sandbox s = new Sandbox();
		s.setSID(sID);
		s.setAppID(appID);
		s.setConsolePort(consolePort);
		s.setConfigDir(configDir);
		s.setFLogFile(logFile);
		s.setFErrFile(errFile);
		s.setPID(PID);
		s.setClientPort(clientPort);
		entityManager.persist(s);
		entityManager.getTransaction().commit();
		return s;
	}
	
	public static void saveSandbox(Sandbox s) {
		entityManager.getTransaction().begin();
		entityManager.persist(s);
		entityManager.getTransaction().commit();
	}

	public static Sandbox getSandbox(String sID){
		try{
			Query query = entityManager.createNamedQuery("getSandbox");
			query.setParameter(1, sID);
			Sandbox result = (Sandbox) query.getSingleResult();
			return result;
		}catch(Exception e){
			System.err.println("unable to get sandbox "+sID+" from DB");
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<Sandbox> getSandboxes(){
		try{
			Query query = entityManager.createNamedQuery("getSandboxes");
			return query.getResultList();
		}catch(Exception e){
			System.err.println("unable to get sandboxes from DB");
			return null;
		}
	}
	
	public static Boolean isDead(){
		try{
			Query query = entityManager.createNamedQuery("isDead");
			Long result = (Long) query.getSingleResult();
			return result > 0;
		}catch(Exception e){
			System.err.println("Unable to query isDead on DB "+e.getMessage());
			return false;
		}
	}
	
	public static void deleteSandbox(String sID){
		try{
			Query query = entityManager.createNamedQuery("deleteSandbox");
			query.setParameter(1, sID);
			query.executeUpdate();
		}catch(Exception e){
			System.err.println("unable to delete sandbox "+sID+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteSandbox(Sandbox s){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteSandbox");
			query.setParameter(1, s.getSID());
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete sandbox "+s.getSID()+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void updateSandbox(Sandbox s){
		try{
			if (s == null){
				System.err.println("cannot update sandbox because it is NULL");
				return;
			}
			entityManager.getTransaction().begin();
			entityManager.merge(s);
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to update sandbox "+s.getSID()+" on DB");
		}
	}

}
