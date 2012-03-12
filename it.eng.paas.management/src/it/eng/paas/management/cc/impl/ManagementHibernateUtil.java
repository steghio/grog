package it.eng.paas.management.cc.impl;

import it.eng.paas.management.hibernate.HibernateServiceUtil;
import it.eng.paas.management.cc.impl.AeeInfo;
import it.eng.paas.management.cc.impl.RRInfo;
import it.eng.paas.management.cc.impl.CCInfo;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

public class ManagementHibernateUtil {
	
	private static EntityManager entityManager;

	public static void initManagementHibernateUtil() {
		entityManager = HibernateServiceUtil.initEntityManager();
	}

	/*AEE*/
	
	public static AeeInfo saveAeeInfo(String ip) {
		entityManager.getTransaction().begin();
		AeeInfo a = new AeeInfo();
		a.setIp(ip);
		entityManager.persist(a);
		entityManager.getTransaction().commit();
		return a;
	}
	
	public static void saveAeeInfo(AeeInfo a) {
		entityManager.getTransaction().begin();
		entityManager.persist(a);
		entityManager.getTransaction().commit();
	}
	
	public static Boolean checkAeeAlreadyPresent(AeeInfo a){
		try{
			Query query = entityManager.createNamedQuery("checkAeeAlreadyPresent");
			query.setParameter(1, a.getIp());
			long count = (long)query.getSingleResult();
			return count != 0;
		}catch(Exception e){
			System.err.println("unable to get aee from DB");
			e.printStackTrace();
			return false;
		}
	}
	
	public static Boolean checkAeeAlreadyPresent(String ip){
		try{
			Query query = entityManager.createNamedQuery("checkAeeAlreadyPresent");
			query.setParameter(1, ip);
			long count = (long)query.getSingleResult();
			return count != 0;
		}catch(Exception e){
			System.err.println("unable to get aee from DB");
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<AeeInfo> getAees(){
		try{
			Query query = entityManager.createNamedQuery("getAees");
			return query.getResultList();
		}catch(Exception e){
			System.err.println("unable to get aees from DB");
			return null;
		}
	}
	
	public static AeeInfo getAee(String ip){
		try{
			Query query = entityManager.createNamedQuery("getAee");
			query.setParameter(1, ip);
			return new AeeInfo(query.getSingleResult().toString());
		}catch(Exception e){
			System.err.println("unable to get aee "+ip+" from DB");
			return null;
		}
	}
	
	public static Boolean isDeadAee(){
		try{
			Query query = entityManager.createNamedQuery("isDeadAee");
			Long result = (Long) query.getSingleResult();
			return result > 0;
		}catch(Exception e){
			System.err.println("Unable to query isDeadAee on DB "+e.getMessage());
			return false;
		}
	}
	
	public static void deleteAeeInfoID(String id){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteAeeInfoID");
			query.setParameter(1, id);
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete aee "+id+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteAeeInfoIP(String ip){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteAeeInfoIP");
			query.setParameter(1, ip);
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete aee "+ip+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	public static void deleteAeeInfoID(AeeInfo a){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteAeeInfoID");
			query.setParameter(1, a.getID());
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete aee "+a.getID()+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteAeeInfoIP(AeeInfo a){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteAeeInfoIP");
			query.setParameter(1, a.getIp());
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete aee "+a.getIp()+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void updateAeeInfo(AeeInfo a){
		try{
			if (a == null){
				System.err.println("cannot update aee because it is NULL");
				return;
			}
			entityManager.getTransaction().begin();
			entityManager.merge(a);
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to update aee "+a.getID()+" on DB");
		}
	}
	
	/////////////////////////////////////////////////
	/*RR*/
	////////////////////////////////////////////////
	
	public static RRInfo saveRRInfo(String ip) {
		entityManager.getTransaction().begin();
		RRInfo r = new RRInfo();
		r.setIp(ip);
		entityManager.persist(r);
		entityManager.getTransaction().commit();
		return r;
	}
	
	public static void saveRRInfo(RRInfo r) {
		entityManager.getTransaction().begin();
		entityManager.persist(r);
		entityManager.getTransaction().commit();
	}
	
	public static Boolean checkRRAlreadyPresent(RRInfo r){
		try{
			Query query = entityManager.createNamedQuery("checkRRAlreadyPresent");
			query.setParameter(1, r.getIp());
			long count = (long)query.getSingleResult();
			return count != 0;
		}catch(Exception e){
			System.err.println("unable to get RR from DB");
			e.printStackTrace();
			return false;
		}
	}
	
	public static Boolean checkRRAlreadyPresent(String ip){
		try{
			Query query = entityManager.createNamedQuery("checkRRAlreadyPresent");
			query.setParameter(1, ip);
			long count = (long)query.getSingleResult();
			return count != 0;
		}catch(Exception e){
			System.err.println("unable to get RR from DB");
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<RRInfo> getRRs(){
		try{
			Query query = entityManager.createNamedQuery("getRRs");
			return query.getResultList();
		}catch(Exception e){
			System.err.println("unable to get RRs from DB");
			return null;
		}
	}
	
	public static RRInfo getRR(String ip){
		try{
			Query query = entityManager.createNamedQuery("getRR");
			query.setParameter(1, ip);
			return new RRInfo(query.getSingleResult().toString());
		}catch(Exception e){
			System.err.println("unable to get RR "+ip+" from DB");
			return null;
		}
	}
	
	public static Boolean isDeadRR(){
		try{
			Query query = entityManager.createNamedQuery("isDeadRR");
			Long result = (Long) query.getSingleResult();
			return result > 0;
		}catch(Exception e){
			System.err.println("Unable to query isDeadRR on DB "+e.getMessage());
			return false;
		}
	}
	
	public static void deleteRRInfoID(String id){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteRRInfoID");
			query.setParameter(1, id);
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete RR "+id+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteRRInfoIP(String ip){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteRRInfoIP");
			query.setParameter(1, ip);
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete RR "+ip+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	public static void deleteRRInfoID(RRInfo r){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteRRInfoID");
			query.setParameter(1, r.getID());
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete RR "+r.getID()+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteRRInfoIP(RRInfo r){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteRRInfoIP");
			query.setParameter(1, r.getIp());
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete RR "+r.getIp()+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void updateRRInfo(RRInfo r){
		try{
			if (r == null){
				System.err.println("cannot update RR because it is NULL");
				return;
			}
			entityManager.getTransaction().begin();
			entityManager.merge(r);
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to update RR "+r.getID()+" on DB");
		}
	}
	
	/////////////////////////////////////////////////
	/*APPS*/
	////////////////////////////////////////////////
	
	public static AppsInfo saveAppsInfo(String appID, String location) {
		entityManager.getTransaction().begin();
		AppsInfo a = new AppsInfo();
		a.setAppID(appID);
		a.setLocation(location);
		entityManager.persist(a);
		entityManager.getTransaction().commit();
		return a;
	}
	
	public static void saveAppsInfo(AppsInfo a) {
		entityManager.getTransaction().begin();
		entityManager.persist(a);
		entityManager.getTransaction().commit();
	}
	
	public static Boolean checkAppLocationAlreadyPresent(AppsInfo a){
		try{
			Query query = entityManager.createNamedQuery("checkAppLocationAlreadyPresent");
			query.setParameter(1, a.getAppID());
			query.setParameter(2, a.getLocation());
			long count = (long)query.getSingleResult();
			return count != 0;
		}catch(Exception e){
			System.err.println("unable to get App "+a.getAppID()+" from DB");
			e.printStackTrace();
			return false;
		}
	}
	
	public static Boolean checkAppLocationAlreadyPresent(String appID, String location){
		try{
			Query query = entityManager.createNamedQuery("checkAppLocationAlreadyPresent");
			query.setParameter(1, appID);
			query.setParameter(2, location);
			long count = (long)query.getSingleResult();
			return count != 0;
		}catch(Exception e){
			System.err.println("unable to get App "+appID+" from DB");
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<AppsInfo> getApps(){
		try{
			Query query = entityManager.createNamedQuery("getApps");
			return query.getResultList();
		}catch(Exception e){
			System.err.println("unable to get Apps from DB");
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<String> getAppLocation(String appID){
		try{
			Query query = entityManager.createNamedQuery("getAppLocation");
			query.setParameter(1, appID);
			return query.getResultList();
		}catch(Exception e){
			System.err.println("unable to get app "+appID+" from DB");
			e.printStackTrace();
			return null;
		}
	}
	
	public static Boolean isDeadApp(){
		try{
			Query query = entityManager.createNamedQuery("isDeadApp");
			Long result = (Long) query.getSingleResult();
			return result > 0;
		}catch(Exception e){
			System.err.println("Unable to query isDeadApp on DB "+e.getMessage());
			return false;
		}
	}
	
	public static void deleteAppsInfoID(String appID){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteAppsInfoID");
			query.setParameter(1, appID);
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete app "+appID+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteAppsInfoLocation(String appID, String location){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteAppsInfoLocation");
			query.setParameter(1, appID);
			query.setParameter(2, location);
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete app "+appID+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	public static void deleteAppsInfoID(AppsInfo a){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteAppsInfoID");
			query.setParameter(1, a.getAppID());
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete app "+a.getAppID()+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteAppsInfoLocation(AppsInfo a){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteAppsInfoLocation");
			query.setParameter(1, a.getAppID());
			query.setParameter(2, a.getLocation());
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete app "+a.getAppID()+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void updateAppsInfo(AppsInfo a){
		try{
			if (a == null){
				System.err.println("cannot update app because it is NULL");
				return;
			}
			entityManager.getTransaction().begin();
			entityManager.merge(a);
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to update app "+a.getAppID()+" on DB");
		}
	}
	
	/////////////////////////////////////////////////
	/*CC*/
	////////////////////////////////////////////////
	
	public static CCInfo saveCCInfo(String ip) {
		entityManager.getTransaction().begin();
		CCInfo c = new CCInfo();
		c.setIp(ip);
		entityManager.persist(c);
		entityManager.getTransaction().commit();
		return c;
	}
	
	public static void saveCCInfo(CCInfo c) {
		entityManager.getTransaction().begin();
		entityManager.persist(c);
		entityManager.getTransaction().commit();
	}
	
	public static Boolean checkCCAlreadyPresent(CCInfo c){
		try{
			Query query = entityManager.createNamedQuery("checkCCAlreadyPresent");
			query.setParameter(1, c.getIp());
			long count = (long)query.getSingleResult();
			return count != 0;
		}catch(Exception e){
			System.err.println("unable to get CC from DB");
			e.printStackTrace();
			return false;
		}
	}
	
	public static Boolean checkCCAlreadyPresent(String ip){
		try{
			Query query = entityManager.createNamedQuery("checkCCAlreadyPresent");
			query.setParameter(1, ip);
			long count = (long)query.getSingleResult();
			return count != 0;
		}catch(Exception e){
			System.err.println("unable to get CC from DB");
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<CCInfo> getCCs(){
		try{
			Query query = entityManager.createNamedQuery("getCCs");
			return query.getResultList();
		}catch(Exception e){
			System.err.println("unable to get CCs from DB");
			return null;
		}
	}
	
	public static CCInfo getCC(String ip){
		try{
			Query query = entityManager.createNamedQuery("getCC");
			query.setParameter(1, ip);
			return new CCInfo(query.getSingleResult().toString());
		}catch(Exception e){
			System.err.println("unable to get CC "+ip+" from DB");
			return null;
		}
	}
	
	public static Boolean isDeadCC(){
		try{
			Query query = entityManager.createNamedQuery("isDeadCC");
			Long result = (Long) query.getSingleResult();
			return result > 0;
		}catch(Exception e){
			System.err.println("Unable to query isDeadCC on DB "+e.getMessage());
			return false;
		}
	}
	
	public static void deleteCCInfoID(String id){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteCCInfoID");
			query.setParameter(1, id);
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete CC "+id+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteCCInfoIP(String ip){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteCCInfoIP");
			query.setParameter(1, ip);
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete CC "+ip+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	public static void deleteCCInfoID(CCInfo c){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteCCInfoID");
			query.setParameter(1, c.getID());
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete CC "+c.getID()+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void deleteCCInfoIP(CCInfo c){
		try{
			entityManager.getTransaction().begin();
			Query query = entityManager.createNamedQuery("deleteCCInfoIP");
			query.setParameter(1, c.getIp());
			query.executeUpdate();
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to delete CC "+c.getIp()+" from DB "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static void updateCCInfo(CCInfo c){
		try{
			if (c == null){
				System.err.println("cannot update CC because it is NULL");
				return;
			}
			entityManager.getTransaction().begin();
			entityManager.merge(c);
			entityManager.getTransaction().commit();
		}catch(Exception e){
			System.err.println("unable to update CC "+c.getID()+" on DB");
		}
	}	

}
