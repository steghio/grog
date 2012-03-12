package it.eng.paas.networking.rr.impl;

import it.eng.paas.networking.hibernate.HibernateServiceUtil;
import it.eng.paas.networking.rr.impl.RoutingInfo;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class RoutingInfoHibernateUtil {
	
	private static EntityManager entityManager;

	public static void initRoutingInfoHibernateUtil() {
		entityManager = HibernateServiceUtil.initEntityManager();
	}

	public static RoutingInfo saveRoutingInfo(String appID, String ip, String clientPort, String consolePort) {
		entityManager.getTransaction().begin();
		RoutingInfo r = new RoutingInfo();
		r.setAppID(appID);
		r.setIp(ip);
		r.setClientPort(clientPort);
		r.setConsolePort(consolePort);
		entityManager.persist(r);
		entityManager.getTransaction().commit();
		return r;
	}
	
	public static void saveRoutingInfo(RoutingInfo r) {
		entityManager.getTransaction().begin();
		entityManager.persist(r);
		entityManager.getTransaction().commit();
	}
	
	public static Boolean checkRoutingAlreadyPresent(RoutingInfo r){
		try{
			Query query = entityManager.createNamedQuery("checkRoutingAlreadyPresent");
			query.setParameter(1, r.getAppID());
			query.setParameter(2, r.getIp());
			query.setParameter(3, r.getConsolePort());
			long count = (long)query.getSingleResult();
			return count != 0;
		}catch(Exception e){
			System.err.println("unable to get routing from DB");
			e.printStackTrace();
			return false;
		}
	}
	
	public static Boolean checkRoutingAlreadyPresent(String appID, String ip, String port){
		try{
			Query query = entityManager.createNamedQuery("checkRoutingAlreadyPresent");
			query.setParameter(1, appID);
			query.setParameter(2, ip);
			query.setParameter(3, port);
			long count = (long)query.getSingleResult();
			return count != 0;
		}catch(Exception e){
			System.err.println("unable to get routing from DB");
			return null;
		}
	}
	
	public static Boolean checkRoutingAlreadyPresentNoPort(RoutingInfo r){
		try{
			Query query = entityManager.createNamedQuery("checkRoutingAlreadyPresentNoPort");
			query.setParameter(1, r.getAppID());
			query.setParameter(2, r.getIp());
			long count = (long)query.getSingleResult();
			return count != 0;
		}catch(Exception e){
			System.err.println("unable to get routing from DB");
			e.printStackTrace();
			return false;
		}
	}
	
	public static Boolean checkRoutingAlreadyPresentNoPort(String appID, String ip){
		try{
			Query query = entityManager.createNamedQuery("checkRoutingAlreadyPresentNoPort");
			query.setParameter(1, appID);
			query.setParameter(2, ip);
			long count = (long)query.getSingleResult();
			return count != 0;
		}catch(Exception e){
			System.err.println("unable to get routing from DB");
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<RoutingInfo> getRouting(RoutingInfo r){
		try{
			Query query = entityManager.createNamedQuery("getRouting");
			query.setParameter(1, r.getAppID());
			return query.getResultList();
		}catch(Exception e){
			System.err.println("unable to get routing from DB");
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<RoutingInfo> getRouting(String appID){
		try{
			Query query = entityManager.createNamedQuery("getRouting");
			query.setParameter(1, appID);
			return query.getResultList();
		}catch(Exception e){
			System.err.println("unable to get routing from DB");
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
	
	public static void deleteRoutingInfoID(String id){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteRoutingInfoID");
			query.setParameter(1, id);
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete routing "+id+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteRoutingInfoIP(String ip){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteRoutingInfoIP");
			query.setParameter(1, ip);
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete routing "+ip+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteRoutingInfoPort(String ip, String consolePort){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteRoutingInfoPort");
			query.setParameter(1, ip);
			query.setParameter(2, consolePort);
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete routing "+ip+":"+consolePort+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteRoutingInfoApp(String appID){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteRoutingInfoApp");
			query.setParameter(1, appID);
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete routing "+appID+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteRoutingInfoID(RoutingInfo r){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteRoutingInfoID");
			query.setParameter(1, r.getID());
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete routing "+r.getID()+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteRoutingInfoIP(RoutingInfo r){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteRoutingInfoIP");
			query.setParameter(1, r.getIp());
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete routing "+r.getIp()+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteRoutingInfoPort(RoutingInfo r){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteRoutingInfoPort");
			query.setParameter(1, r.getIp());
			query.setParameter(2, r.getConsolePort());
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete routing "+r.getIp()+":"+r.getConsolePort()+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteRoutingInfoApp(RoutingInfo r){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteRoutingInfoApp");
			query.setParameter(1, r.getAppID());
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete routing "+r.getAppID()+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void updateRoutingInfo(RoutingInfo r){
		try{
			if (r == null){
				System.err.println("cannot update routing because it is NULL");
				return;
			}
			entityManager.getTransaction().begin();
			entityManager.merge(r);
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to update routing "+r.getID()+" on DB");
		}
	}

	@SuppressWarnings("unchecked")
	public static List<RoutingInfo> getRoutingInfos(){
		try{
			Query query = entityManager.createNamedQuery("getRoutingInfos");
			return query.getResultList();
		}catch(Exception e){
			System.err.println("unable to get routingInfos from DB");
			return null;
		}
	}

}
