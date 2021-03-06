package org.hydra.plugin;
import java.util.* ;
import java.net.* ;
import org.hydra.pdu.* ;

import org.cougaar.util.*;
import org.cougaar.core.cluster.* ;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.asset.*;
import org.cougaar.domain.glm.ldm.asset.Organization;
import org.cougaar.core.society.*;
import org.cougaar.domain.glm.ldm.asset.OrganizationPG;
import java.io.* ;
import org.hydra.util.* ;

/**
 *  PlanMonitorPlugIn sends "out-of-band" messages to a central data collector.
 * <p> Only one of these plugins should be created for each cluster.
 */
public class PlanMonitorPlugIn extends org.cougaar.core.plugin.SimplePlugIn {
    static class ARTuple {
        AllocationResult received ;
        AllocationResult reported ;
        AllocationResult observed ;
        AllocationResult estimated ;
    }
    
    public static final int AR_LOG_LEVEL_NONE = 0 ;
    public static final int AR_LOG_LEVEL_SUCCESS = 1 ;
    public static final int AR_LOG_LEVEL_FULL = 2 ;
    public static final int AR_LOG_LEVEL_FAST = 3 ;
    
    /** Subscribe to all tasks, assets, and allocations. */
    UnaryPredicate allPredicate = new UnaryPredicate() {
        public boolean execute( Object o ) {
            return ( o instanceof PlanElement || o instanceof Task || o instanceof Asset ) ;
        }
    } ;
    
    Collection queryLocal( UnaryPredicate up ) {
        return query( up ) ;
    }
    
    IncrementalSubscription allElements ;
    
    /** The level of detail to which allocation results will be logged.
     */
    public int logAllocationResultsLevel() {
        return logAllocationResultsLevel ;
    }
    
    public void setupSubscriptions() {
        clientMessageTransport = new SocketClientMTImpl(getConfigFinder(),getClusterIdentifier(),this) ;
        clientMessageTransport.connect() ;
        
        if ( clientMessageTransport.isConnected() ) {
            subscribe() ;
        }

        //uiFrame = new PlanMonitorPlugInUIFrame(this) ;
        //uiFrame.setVisible( true ) ;
    }

    void subscribe( ) {
         allElements = ( IncrementalSubscription ) subscribe( allPredicate ) ;
    }

    synchronized void unsubscribe() {
        if ( allElements != null ) {
            unsubscribe( allElements ) ;
            allElements = null ;
        }
    }

    public void log( String s ) {
        //        uiFrame.log( s ) ;
    }

    protected AllocationResult replicate( AllocationResult ar ) {
        if ( ar == null ) return null ;
        return ( AllocationResult ) ar.clone() ;
    }

    /**
     * @param message  Any messages generated by this
     */
    protected AllocationResult compareAndReplicate( UID planElement, short arType, AllocationResult old, AllocationResult newResult,
    ArrayList message  ) {
        if ( old == null && newResult == null ) {
            return null ;
        }
        if ( old == null && newResult != null ) {
            message.add( PlanToPDUTranslator.makeAllocationResultMessage( this, planElement, newResult, arType, EventPDU.ACTION_ADD ) ) ;
            return ( AllocationResult ) newResult.clone() ;

        }
        if ( old != null && newResult == null ) {
            message.add( PlanToPDUTranslator.makeAllocationResultMessage( this, planElement, old, arType, EventPDU.ACTION_REMOVE ) ) ;
            return null ;  // Should publish an AllocationResultPDU with a REMOVE flag
        }
        if ( !old.isEqual( newResult ) ) {
            AllocationResult result = ( AllocationResult ) newResult.clone() ;
            message.add( PlanToPDUTranslator.makeAllocationResultMessage( this, planElement, newResult, arType, EventPDU.ACTION_CHANGE ) ) ;
            return result ;
        }
        return null ;
    }

    /** 
     */
    long compareAR( UID planElement, short arType, long value, AllocationResult newResult, ArrayList message ) {
        long newValue = PlanToPDUTranslator.computeCRC32( newResult ) ;
        if ( newValue == value ) {
            return value ;
        }
        if ( newValue != 0 && value == 0 ) {
            message.add( PlanToPDUTranslator.makeAllocationResultMessage( this, planElement, newResult, arType, EventPDU.ACTION_ADD ) ) ;
            return newValue ;
        }
        if ( newValue == 0 && value != 0 ) {
            // make a remove message
            //message.add( PlanToPDUTranslator.makeAllocationResultMessage( this, planElement, old, arType, EventPDU.ACTION_REMOVE ) ) ;
            return newValue ;  // Should publish an AllocationResultPDU with a REMOVE flag
        }
        AllocationResult result = ( AllocationResult ) newResult.clone() ;
        message.add( PlanToPDUTranslator.makeAllocationResultMessage( this, planElement, newResult, arType, EventPDU.ACTION_CHANGE ) ) ;

        return newValue ;
    }

    /** Changed to use CRC32 to compare.
     */
    protected void checkARChanged( Allocation a ) {
        long[] tuple = ( long[] ) allocationToARMap.get( a.getUID() ) ;
        if ( tuple == null ) {
            if ( a.getEstimatedResult() == null && a.getReceivedResult() == null &&
            a.getReportedResult() == null && a.getObservedResult() == null ) {
                return ;
            }
            tuple = new long[ 4 ] ;
            AllocationResult estimated = a.getEstimatedResult(), received = a.getReceivedResult(),
               reported = a.getReportedResult(), observed = a.getObservedResult() ;
            tuple[0] = PlanToPDUTranslator.computeCRC32( estimated ) ;
            tuple[1] = PlanToPDUTranslator.computeCRC32( received ) ;
            tuple[2] = PlanToPDUTranslator.computeCRC32( reported ) ;
            tuple[3] = PlanToPDUTranslator.computeCRC32( observed ) ;
            allocationToARMap.put( a.getUID(), tuple ) ;
            if ( tuple[0] != 0 ) {
                AllocationResultPDU pdu = PlanToPDUTranslator.makeAllocationResultMessage( this, a.getUID(), estimated,
                AllocationResultPDU.ESTIMATED, EventPDU.ACTION_ADD ) ;
                sendMessage( pdu ) ;
            }
            if ( tuple[1] != 0 ) {
                AllocationResultPDU pdu = PlanToPDUTranslator.makeAllocationResultMessage( this, a.getUID(), received,
                AllocationResultPDU.RECEIVED, EventPDU.ACTION_ADD ) ;
                sendMessage( pdu ) ;
            }
            if ( tuple[2] != 0 ) {
                AllocationResultPDU pdu = PlanToPDUTranslator.makeAllocationResultMessage( this, a.getUID(), reported,
                AllocationResultPDU.REPORTED, EventPDU.ACTION_ADD ) ;
                sendMessage( pdu ) ;
            }
            if ( tuple[3] != 0 ) {
                AllocationResultPDU pdu = PlanToPDUTranslator.makeAllocationResultMessage( this, a.getUID(), observed,
                AllocationResultPDU.OBSERVED, EventPDU.ACTION_ADD ) ;
                sendMessage( pdu ) ;
            }
        }
        else {
            messages.clear() ;
            tuple[0] = compareAR( a.getUID(), AllocationResultPDU.ESTIMATED, tuple[0], a.getEstimatedResult(), messages ) ;
            tuple[1] = compareAR( a.getUID(), AllocationResultPDU.RECEIVED, tuple[1], a.getReceivedResult(), messages ) ;
            tuple[2] = compareAR( a.getUID(), AllocationResultPDU.REPORTED, tuple[2], a.getReportedResult(), messages ) ;
            tuple[3] = compareAR( a.getUID(), AllocationResultPDU.OBSERVED, tuple[3], a.getObservedResult(), messages ) ;
            for (int i=0;i<messages.size();i++) {
                PDU pdu = ( PDU ) messages.get(i) ;
                sendMessage( pdu ) ;
            }
        }
    }


    protected void checkAllocationResultChanged( Allocation a ) {
        ARTuple tuple = ( ARTuple ) allocationToARMap.get( a.getUID() ) ;
        if ( tuple == null ) {
            if ( a.getEstimatedResult() == null && a.getReceivedResult() == null &&
            a.getReportedResult() == null && a.getObservedResult() == null ) {
                return ;
            }

            tuple = new ARTuple() ;
            tuple.estimated = replicate( a.getEstimatedResult() ) ;
            tuple.received = replicate( a.getReceivedResult() ) ;
            tuple.reported = replicate( a.getReportedResult() ) ;
            tuple.observed = replicate( a.getObservedResult() ) ;
            allocationToARMap.put( a.getUID(), tuple ) ;
            if ( tuple.estimated != null ) {
                AllocationResultPDU pdu = PlanToPDUTranslator.makeAllocationResultMessage( this, a.getUID(), tuple.estimated,
                AllocationResultPDU.ESTIMATED, EventPDU.ACTION_ADD ) ;
                sendMessage( pdu ) ;
            }
            if ( tuple.received != null ) {
                AllocationResultPDU pdu = PlanToPDUTranslator.makeAllocationResultMessage( this, a.getUID(), tuple.received,
                AllocationResultPDU.RECEIVED, EventPDU.ACTION_ADD ) ;
                sendMessage( pdu ) ;
            }
            if ( tuple.reported != null ) {
                AllocationResultPDU pdu = PlanToPDUTranslator.makeAllocationResultMessage( this, a.getUID(), tuple.reported,
                AllocationResultPDU.REPORTED, EventPDU.ACTION_ADD ) ;
                sendMessage( pdu ) ;
            }
            if ( tuple.observed != null ) {
                AllocationResultPDU pdu = PlanToPDUTranslator.makeAllocationResultMessage( this, a.getUID(), tuple.observed,
                AllocationResultPDU.OBSERVED, EventPDU.ACTION_ADD ) ;
                sendMessage( pdu ) ;
            }
        }
        else { // Compare the existing against the old
            messages.clear() ;
            tuple.estimated = compareAndReplicate( a.getUID(), AllocationResultPDU.ESTIMATED, tuple.estimated, a.getEstimatedResult(), messages ) ;
            tuple.received = compareAndReplicate( a.getUID(), AllocationResultPDU.RECEIVED, tuple.received, a.getReceivedResult(), messages ) ;
            tuple.reported = compareAndReplicate( a.getUID(), AllocationResultPDU.REPORTED, tuple.estimated, a.getReportedResult(), messages ) ;
            tuple.observed = compareAndReplicate( a.getUID(), AllocationResultPDU.OBSERVED, tuple.estimated, a.getObservedResult(), messages ) ;
            for (int i=0;i<messages.size();i++) {
                PDU pdu = ( PDU ) messages.get(i) ;
                sendMessage( pdu ) ;
            }
        }
    }
    
    /** Notifies the plugin that its message transport is no longer delivering messages.
     */
    void notifyDisconnected( ClientMessageTransport cmt ) {
        unsubscribe() ;
    }
    
    ArrayList messages = new ArrayList(4) ;
    
    protected void processChangedObject( Object o ) {
        int action = EventPDU.ACTION_CHANGE ;
        // System.out.println( "Processing " + o ) ;
        PDU pdu = null ;
        if ( o instanceof MPTask ) {
            pdu = PlanToPDUTranslator.makeMPTaskPDU(this,(MPTask)o, action ) ;
        }
        else if ( o instanceof Task ) {
            pdu = PlanToPDUTranslator.makeTaskMessage(this,(Task)o,action) ;
        }
        else if ( o instanceof Asset ) {
            pdu = PlanToPDUTranslator.makeAssetMessage(this,(Asset)o,action ) ;
        }
        else if ( o instanceof Allocation ) {
            // Check to (specifically) see if the AR has changed.  If so, make
            // an AllocationResultPDU
            Allocation al = ( Allocation ) o ;
            PDU temp = PlanToPDUTranslator.makeAllocationMessage( this, al, action ) ;
            sendMessage( temp ) ;
            checkARChanged( al ) ;
            //checkAllocationResultChanged( al ) ;
        }
        else if ( o instanceof Aggregation ) {
            pdu = PlanToPDUTranslator.makeAggregationPDU( this, (AggregationImpl) o, action ) ;
        }
        else if ( o instanceof Expansion ) {
            pdu = PlanToPDUTranslator.makeExpansionPDU( this, (Expansion) o, action ) ;
        }
        sendMessage( pdu ) ;
    }
    
    protected void processObject( Object o, int action ) {
        // System.out.println( "Processing " + o ) ;
        try {
            PDU pdu = null ;
            if ( o instanceof MPTask ) {
                pdu = PlanToPDUTranslator.makeMPTaskPDU(this,(MPTask)o, action ) ;
            }
            else if ( o instanceof Task ) {
                pdu = PlanToPDUTranslator.makeTaskMessage(this,(Task)o,action) ;
            }
            else if ( o instanceof Asset ) {
                pdu = PlanToPDUTranslator.makeAssetMessage(this,(Asset)o,action ) ;
            }
            else if ( o instanceof Allocation ) {
                // Check to (specifically) see if the AR has changed.  If so, make
                // an AllocationResultPDU
                Allocation al = ( Allocation ) o ;
                PDU temp = PlanToPDUTranslator.makeAllocationMessage( this, al, action ) ;
                sendMessage( temp ) ;
                if ( logAllocationResultsLevel == AR_LOG_LEVEL_FULL ) {
                    checkAllocationResultChanged( al ) ;
                }
                else if ( logAllocationResultsLevel == AR_LOG_LEVEL_FAST ) {
                    checkARChanged( al ) ;
                }
            }
            else if ( o instanceof Aggregation ) {
                pdu = PlanToPDUTranslator.makeAggregationPDU( this, (AggregationImpl) o, action ) ;
            }
            else if ( o instanceof Expansion ) {
                pdu = PlanToPDUTranslator.makeExpansionPDU( this, (Expansion) o, action ) ;
            }
            sendMessage( pdu ) ;
        }
        catch ( Exception e ) {
            e.printStackTrace() ;
        }
        
    }
    
    void sendMessage( PDU pdu ) {
        // System.out.println( "Sending : " + pdu ) ;
        if ( pdu != null && clientMessageTransport != null ) {
            clientMessageTransport.sendMessage( pdu ) ;
        }
    }
    
    public synchronized void execute() {

        if ( allElements == null ) {
            return ;
        }
        
        long startTime = System.currentTimeMillis() ;
        String cluster = getClusterIdentifier().toString() ;
        // Process added/removed/changed
        for ( Enumeration e=allElements.getAddedList();e.hasMoreElements();) {
            Object o = e.nextElement() ;
            processObject( o, EventPDU.ACTION_ADD );
        }
        
        for ( Enumeration e=allElements.getRemovedList();e.hasMoreElements();) {
            Object o = e.nextElement() ;
            processObject( o, EventPDU.ACTION_REMOVE ) ;
        }
        
        for ( Enumeration e=allElements.getChangedList();e.hasMoreElements();) {
            Object o = e.nextElement() ;
            processChangedObject( o ) ;
        }
        
        totalTime += System.currentTimeMillis() - startTime ;
        lastExecuted = System.currentTimeMillis() ;
    }
    
    protected int logAllocationResultsLevel = AR_LOG_LEVEL_FAST ;
    // Parse parameters.  Where do I send my results?
    protected long lastExecuted ;
    protected long totalTime ;
    protected InetAddress target ;
    protected ObjectOutputStream sout ;
    protected ClientMessageTransport clientMessageTransport ;
    // protected PlanMonitorPlugInUIFrame uiFrame ;
    protected SymbolTable symTable = new SymbolTable() ;
    protected HashMap allocationToARMap = new HashMap() ;
}