/*
 * Copyright (c) 2006-2008 Hyperic, Inc.
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

import java.util.Hashtable;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.cmd.SigarCommandBase;

/**
 * Display cpu information for each cpu found on the system.
 */
public class CpuInfo extends SigarCommandBase {

    public boolean displayTimes = true;
    private org.hyperic.sigar.CpuInfo c;

    public CpuInfo() {
        super();
        try{
        	c = this.sigar.getCpuInfoList()[0];
        }catch(Exception e){
        	System.err.println("cannot instantiate cpuinfo "+e.getMessage());
        	e.printStackTrace();
        	c = null;
        }
    }
    
    public Boolean isOk(){
    	return c!=null;
    }
    
    /**
     * Gather CPU info, keys are
     * Hz
     * cores (logical)
     * socket -physical CPUs
     * socketCores - cores per socket (only if socket>1)
     * cache (if available)
     * @return
     */
    public Hashtable<String, String> getCPUInfo(){
    	Hashtable<String,String> info = new Hashtable<>();
		info.put("Hz", String.valueOf(c.getMhz()));
		//logical
		info.put("cores", String.valueOf(c.getTotalCores()));
		//physical
		info.put("socket", String.valueOf(c.getTotalSockets()));
		//if more than one CPU present
		if ((c.getTotalCores() != c.getTotalSockets()) || c.getCoresPerSocket() > c.getTotalCores())info.put("socketCores", String.valueOf(c.getCoresPerSocket()));
		long cacheSize = c.getCacheSize();
    	if (cacheSize != Sigar.FIELD_NOTIMPL)info.put("cache", String.valueOf(cacheSize));
    	return info;
    }
    
    public double getCPUUsage(){
    	//usage = sys+user+wait+nice %
    	try {
			return 100*this.sigar.getCpuPerc().getCombined();
		} catch (Exception e) {
			System.err.println("cannot get cpu usage "+e.getMessage());
        	e.printStackTrace();
        	return -1;
		}
    }

	public void output(String[] arg0) throws SigarException {
		//useless
	}
}
