/*
 *  This plugin is a falling behind sensor for each agent based on the imbalance 
 *  between task arrival duration and task allocation duration.
 *  This sensor deals with ProjectSupply/Supply and Transport tasks.
 */

package org.cougaar.tools.alf.sensor.plugin;

import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.*;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.util.UID;
import org.cougaar.planning.ldm.plan.*;
import org.cougaar.util.*;
//import org.cougaar.core.agent.ClusterIdentifier; Changed by Himanshu
import org.cougaar.core.agent.*; //Added by Himanshu
import org.cougaar.core.mts.MessageAddress;//Added by Himanshu
import java.util.Iterator;
import java.util.Collection;
import org.cougaar.core.service.community.CommunityService;
import org.cougaar.multicast.AttributeBasedAddress;
import org.cougaar.logistics.plugin.manager.LoadIndicator;
import org.cougaar.core.agent.service.alarm.PeriodicAlarm;
import java.io.*;

public class PSUFBSensor4Plugin extends ComponentPlugin
{

        class TriggerFlushAlarm implements PeriodicAlarm
    {
        public TriggerFlushAlarm(long expTime)
        {
            this.expTime = expTime;
        }

        public void reset(long currentTime)
        {
            expTime = currentTime + delay;
            expired = false;
        }

        public long getExpirationTime()
        {
            return expTime;
        }

        public void expire()
        {
            expired = true;
		getBlackboardService().openTransaction();
            setNormal();
		getBlackboardService().closeTransaction();

        }

        public boolean hasExpired()
        {
            return expired;
        }

        public boolean cancel()
        {
            boolean was = expired;
            expired = true;
            return was;
        }

        boolean expired = false;
        long expTime;
        long delay = 60000;
    }

    UnaryPredicate allocationPredicate = new UnaryPredicate()
    {
        public boolean execute(Object o)
        {
            if (o instanceof Allocation) {
                Allocation allocation = (Allocation) o;
                if (same(allocation.getAsset().getUID().getOwner(), cluster)) {
                    AllocationResult ar = allocation.getEstimatedResult();
                    Task task = allocation.getTask();
                    if (ar != null) {
                        if (ar.isSuccess()) {
                            String verb = task.getVerb().toString();
                            if (same(verb, "ProjectWithdraw") || same(verb, "Withdraw") || same(verb, "ProjectSupply") || same(verb, "Supply") || same(verb, "Transport")) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }
    };
    
    UnaryPredicate sensorPredicate = new UnaryPredicate()
    {
        public boolean execute(Object o)
        {
            if (o instanceof LoadIndicator) {
                LoadIndicator loadIndicator = (LoadIndicator) o;
                if (loadIndicator.getReportingSensorClassName().endsWith(sensorname) && same(loadIndicator.getAgentName(), cluster)) {
                    return true;
                }
            }
            return false;
        }
    };

    
    public void setupSubscriptions()
    {

        myTimestampService = (BlackboardTimestampService) getBindingSite().getServiceBroker().getService(this, BlackboardTimestampService.class, null) ;
        myBlackboardService = getBlackboardService();
        myUIDService = (UIDService) getBindingSite().getServiceBroker().getService(this, UIDService.class, null); 
        myLoggingService = (LoggingService) getBindingSite().getServiceBroker().getService(this, LoggingService.class, null); 
		cluster = ((AgentIdentificationService) getBindingSite().getServiceBroker().getService(this, AgentIdentificationService.class, null)).getName();
        allocationSubscription = (IncrementalSubscription) myBlackboardService.subscribe(allocationPredicate);
        sensorSubscription = (IncrementalSubscription) myBlackboardService.subscribe(sensorPredicate);

        if (myBlackboardService.didRehydrate()==false)
        {
            CommunityService communityService = (CommunityService) getBindingSite().getServiceBroker().getService(this, CommunityService.class, null);
            if (communityService == null) {
                myLoggingService.error("CommunityService not available.");
                return;
            }
            Collection alCommunities = communityService.listParentCommunities(cluster, "(CommunityType=AdaptiveLogistics)");
            if (alCommunities.size() == 0) {
                myLoggingService.warn(cluster + " does not belong to an AdaptiveLogistics community.");
            }
            for (Iterator iterator = alCommunities.iterator(); iterator.hasNext();) {
                String community = (String) iterator.next();
                LoadIndicator loadIndicator = new LoadIndicator(this, 
                                    cluster,
                                    myUIDService.nextUID(),
                                    LoadIndicator.NORMAL_LOAD);
                loadIndicator.addTarget(new AttributeBasedAddress().getAttributeBasedAddress(community, "Role", "AdaptiveLogisticsManager"));
                myBlackboardService.publishAdd(loadIndicator);
            }
		}
		myBlackboardService.setShouldBePersisted(false);
		
		if (myTimestampService == null) {
            System.out.println("\n"+cluster+" ["+sensorname+"]: TimestampService is NOT available.\n");
        }
	    as = getAlarmService() ;
	    if(as == null){
	        System.out.println("\n"+cluster+" ["+sensorname+"]: AlarmService is NOT available.\n"); 
	    }
	
    }


    public void execute()
    {

        if (alarm != null) alarm.cancel();
		if (myTimestampService == null) {
            myTimestampService = (BlackboardTimestampService) getBindingSite().getServiceBroker().getService(this, BlackboardTimestampService.class, null);
            if (myTimestampService == null) {
                return;
            }
            else {
                System.out.println("\n"+cluster+" ["+sensorname+"]: TimestampService is available.\n");
            }
        }
        
        if (as == null) {
            as = (AlarmService) getBindingSite().getServiceBroker().getService(this, AlarmService.class, null);
            if (as == null) {
                return;
            }
            else {
                System.out.println("\n"+cluster+" ["+sensorname+"]: AlarmService is available.\n");
            }
        }

        Iterator iter;
        Task task;
        Allocation allocation;

        int valid = 0;
        String status = LoadIndicator.NORMAL_LOAD;
        long tevent_time, pevent_time;

        if (allocationSubscription.size()<3) return;
	    if (allocationSubscription.size() == size && access == 1) return;
	    size = allocationSubscription.size();
        
        queue = new long[2][2*allocationSubscription.size()];
        long min_task_time = Long.MAX_VALUE, max_task_time = Long.MIN_VALUE;
        
        for (iter = allocationSubscription.getCollection().iterator() ; iter.hasNext() ;)
        {
            allocation = (Allocation)iter.next();
            task = allocation.getTask();
            tevent_time = myTimestampService.getModificationTime(task.getUID());
            pevent_time = myTimestampService.getModificationTime(allocation.getUID());
            if (tevent_time > 0 && pevent_time > 0) {
				queue[0][valid*2] = tevent_time;
				queue[0][valid*2+1] = pevent_time;
				queue[1][valid*2] = 1;
				queue[1][valid*2+1] = -1;
                if (tevent_time > max_task_time) max_task_time = tevent_time;
                if (tevent_time < min_task_time) min_task_time = tevent_time;
                valid = valid + 1;
            }
        }
        
        if (valid < 3) {
            return;
        }
        
        long g_duration = max_task_time - min_task_time;
        
        sort(valid*2);
        
		int i, start = 0;
		long load = 0;
		
		for (i=0; i<2*valid; i++) {
			if (queue[1][i] == 0) {
				load = load + (queue[0][i] - queue[0][start]);
				start = i + 1;
			}
		}

        double loadIndex;
        double sigma2 = 1.25, sigma3 = 1.66;
         
        if (g_duration > 0) loadIndex = (double)load / (double)g_duration;
        else loadIndex = -1;
        
        for (iter = sensorSubscription.getCollection().iterator(); iter.hasNext();) {
            LoadIndicator loadIndicator = (LoadIndicator) iter.next();
            if (loadIndex > sigma3) status = LoadIndicator.SEVERE_LOAD;
            else if (loadIndex <= sigma2) status = LoadIndicator.NORMAL_LOAD;
            else status = LoadIndicator.MODERATE_LOAD;
            if (!same(loadIndicator.getLoadStatus(), status)) {
                loadIndicator.setLoadStatus(status);
                myBlackboardService.publishChange(loadIndicator);
            }
        }
        
        System.out.println("\n"+cluster+" ["+sensorname+"]: Load Index (LI) = "+((int)(loadIndex*100))/100.0+" ["+status+"]");
	    alarm = new TriggerFlushAlarm( currentTimeMillis() + 60000 );
        as.addAlarm(alarm) ;	
	    access = 0;


    }
 
    
    boolean same(String s1, String s2)
    {
        if (s1.equalsIgnoreCase(s2)) return true;
        else return false;
    }

    void sort (int n) {
    
        int i, j, minindex = 0;
        long min;
        long[][] sdata = new long[2][n];
        
        for (i=0; i<n; i++) {
            
            min = Long.MAX_VALUE;
            
            for(j=0; j<n; j++) {
                
                if (min > queue[0][j] && queue[1][j] != 0) {
                    
                    min = queue[0][j];
                    minindex = j;
                    
                }
            }
            
            sdata[0][i] = queue[0][minindex];
            sdata[1][i] = queue[1][minindex];
  	        queue[1][minindex] = 0;
  	        
        }
            
        for (i=1; i<n; i++) {
            
  	        sdata[1][i] = sdata[1][i-1] + sdata[1][i];

        }
        
        queue = sdata;
        
    }  

    void setNormal () {
        Iterator iter;
        String status = LoadIndicator.NORMAL_LOAD;
        for (iter = sensorSubscription.getCollection().iterator(); iter.hasNext();) {
            LoadIndicator loadIndicator = (LoadIndicator) iter.next();
            status = LoadIndicator.NORMAL_LOAD;
            if (!same(loadIndicator.getLoadStatus(), status)) {
                loadIndicator.setLoadStatus(status);
                myBlackboardService.publishChange(loadIndicator);
            }
        }
        System.out.println("\n"+cluster+" ["+sensorname+"]: Load Index (LI) = -1"+" ["+status+"]");
	  alarm.cancel();
	  access = 1;
    }

    IncrementalSubscription allocationSubscription;   
    IncrementalSubscription sensorSubscription; 
    String cluster, sensorname = "PSUFBSensor4Plugin";
    long[][] queue;
    private BlackboardTimestampService myTimestampService; 
    private BlackboardService myBlackboardService;
    private LoggingService myLoggingService;
    private UIDService myUIDService;
    AlarmService as;
    TriggerFlushAlarm alarm = null;	
    int size = 0;
    int access = 0;
    
}
