/*
 * Copyright (c) 2006 Hyperic, Inc.
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

import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Swap;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.cmd.SigarCommandBase;

/**
 * Display amount of free and used memory in the system.
 */
public class MemoryInfo extends SigarCommandBase {
	
	private Mem mem;
	private Swap swap;

    public MemoryInfo() {
        super();
        try{
        	mem = this.sigar.getMem();
        	swap = this.sigar.getSwap();
        }catch(Exception e){
        	System.err.println("cannot instantiate mem and swap "+e.getMessage());
        	e.printStackTrace();
        	mem = null;
        	swap = null;
        }
    }
    
    public Boolean isOk(){
    	return mem!=null && swap!=null;
    }

    private static String format(long value) {
        return Long.toString(value / 1024);
    }

    public String getTotal(){
    	return format(mem.getTotal());
    }
    
    public String getUsed(){
    	return format(mem.getUsed());
    }
    
    public String getFree(){
    	return format(mem.getFree());
    }
    
    public String getActualUsed(){
    	return format(mem.getActualUsed());
    }
    
    public String getActualFree(){
    	return format(mem.getActualFree());
    }
    
    public String getSwapTotal(){
    	return format(swap.getTotal());
    }
    
    public String getSwapUsed(){
    	return format(swap.getUsed());
    }
    
    public String getSwapFree(){
    	return format(swap.getFree());
    }
    
	public void output(String[] arg0) throws SigarException {
		// useless
	}
}
