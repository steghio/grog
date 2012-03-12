/*
 * Copyright (c) 2006-2009 Hyperic, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.eng.paas.utilities;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import org.hyperic.sigar.cmd.SigarCommandBase;

/**
 * Display Sigar, java and system version information.
 */
public class OsInfo extends SigarCommandBase {

    public OsInfo() {
        super();
    }

    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    public static Hashtable<String,String> getOsInfo() {
    	Hashtable<String,String> info = new Hashtable<>();
        String host = getHostName();
        String fqdn;//fully qualified domain name hostname.acme.com
        Sigar sigar = new Sigar(); 
        try {
            fqdn = sigar.getFQDN();
        } catch (SigarException e) {
            fqdn = "unknown";
        } finally {
            sigar.close();
        }
        info.put("fqdn", fqdn);
        if (!fqdn.equals(host))info.put("hostname", host);
        info.put("arch", System.getProperty("os.arch"));
        info.put("os", System.getProperty("os.name"));
        return info;
    }
    
	public void output(String[] arg0) throws SigarException {
		//useless
	}
}
