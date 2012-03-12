package it.eng.paas.management.hibernate;

import it.eng.paas.management.cc.impl.CC;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class HibernateServiceUtil {

	private static EntityManagerFactory emf;
	
    private static EntityManager em;
	
	private static final String PERSISTENCE_UNIT = "MANAGEMENT"; 
	
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static EntityManager initEntityManager() {
    	BasicConfigurator.configure();
        Logger.getLogger(HibernateServiceUtil.class).setLevel(Level.ERROR);
        if (emf == null) {
        	Map addedOrOverridenProperties = new HashMap();

        	// Let's suppose we are using Hibernate as JPA provider
        	addedOrOverridenProperties.put("hibernate.connection.password", CC.getCc_DB_pwd());
        	addedOrOverridenProperties.put("hibernate.connection.url", CC.getCc_DB_URL());
        	addedOrOverridenProperties.put("hibernate.connection.username", CC.getCc_DB_user());
        	emf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, addedOrOverridenProperties);
            em = emf.createEntityManager();
        }
        return em;
    }
	
    public void cleanup() {
        em.clear();
    	emf.close();
    }
    
}

