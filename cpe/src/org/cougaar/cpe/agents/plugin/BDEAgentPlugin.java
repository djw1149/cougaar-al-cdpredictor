package org.cougaar.cpe.agents.plugin;

import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.adaptivity.OperatingModeCondition;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.UIDService;
import org.cougaar.cpe.agents.Constants;
import org.cougaar.cpe.agents.messages.*;
import org.cougaar.cpe.model.*;
import org.cougaar.cpe.planning.zplan.*;
import org.cougaar.cpe.relay.GenericRelayMessageTransport;
import org.cougaar.cpe.relay.MessageSink;
import org.cougaar.cpe.relay.SourceBufferRelay;
import org.cougaar.cpe.relay.TimerMessage;
import org.cougaar.cpe.util.CPUConsumer;
import org.cougaar.cpe.util.ConfigParserUtils;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.tools.techspecs.events.MessageEvent;
import org.cougaar.tools.techspecs.events.TimerEvent;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.UnaryPredicate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

public class BDEAgentPlugin extends ComponentPlugin implements MessageSink {
    private SourceBufferRelay worldStateRelay;
    private IncrementalSubscription findOrgsSubscription ;
    private HashMap subordinateCombatOrganizations = new HashMap();

    private OMCRangeList replanPeriodList = new OMCRangeList( new int[] { 2, 3, 4 } );

    /**
     * The number of zone phases per replan.
     */
    private OperatingModeCondition replanPeriodCondition ;

    private OMCRangeList planningDepthList = new OMCRangeList( new int[] { 4, 5, 6, 7, 8, 9, 10, 11 } ) ;

    /**
     * The depth of the replan in phases.
     */
    private OperatingModeCondition planningDepthCondition ;

    private OperatingModeCondition planningBreadthCondition ;

    private OMCRangeList planningBreadthList = new OMCRangeList( new int[] { 30, 50, 70, 90, 100, 120 } ) ;

    private OperatingModeCondition planningDelayCondition ;

    private OMCRangeList planningDelayList = new OMCRangeList( new int[] { 1, 2, 3 } ) ;
    /**
     * The number of deltaT increments per planning phase. This is a multiple of the number of delta per task element.
     */
    protected int deltaTPerPlanningPhase = 12 ;

    protected ZonePlanner zonePlanner;

    /**
     * Configuration data saved at startup.
     */
    private byte[] configBytes;
    private long updateStatusNIU = 0 ;
    private long baseTime;

    protected void setupSubscriptions() {
        logger = (LoggingService) getServiceBroker().getService( this, LoggingService.class, null ) ;

        logger.shout( "BDEAgentPlugin:: STARTING agent " + getAgentIdentifier() );
        gmrt = new GenericRelayMessageTransport( this, getServiceBroker(), getAgentIdentifier(),
                this, getBlackboardService() ) ;

        // Always sign in to the world state.
        worldStateRelay = gmrt.addRelay( MessageAddress.getMessageAddress( Constants.WORLD_STATE_AGENT ) ) ;
        // Find the WorldStateAgent
        UIDService service = (UIDService) getServiceBroker().getService( this, UIDService.class, null ) ;

        class FindOrgsPredicate implements UnaryPredicate {
            public boolean execute(Object o) {
                return o instanceof Organization ;
            }
        }
        findOrgsSubscription = (IncrementalSubscription) getBlackboardService().subscribe(new FindOrgsPredicate());

        publishOperatingModes() ;

        getConfigInfo();
    }

    private void publishOperatingModes() {

        replanPeriodCondition = new OperatingModeCondition( "ReplanPeriod", replanPeriodList ) ;
        getBlackboardService().publishAdd( replanPeriodCondition );

        planningDepthCondition = new OperatingModeCondition( "PlanningDepth", planningDepthList ) ;
        getBlackboardService().publishAdd( planningDepthCondition );

        planningBreadthCondition = new OperatingModeCondition( "PlanningBreadth", planningBreadthList ) ;
        getBlackboardService().publishAdd( planningBreadthCondition );

        planningDelayCondition = new OperatingModeCondition( "PlanningDelay", planningDelayList ) ;
        getBlackboardService().publishAdd( planningDelayCondition );
    }

    protected void processOrganizations() {
        //  Find subordinates signing in.
        Collection addedCollection = findOrgsSubscription.getAddedCollection() ;
        ArrayList self = new ArrayList(),
                newSubordinates = new ArrayList(),
                newSuperiors = new ArrayList() ;

        OrganizationHelper.processSuperiorAndSubordinateOrganizations( getAgentIdentifier(),
                addedCollection, newSuperiors, newSubordinates, self );

        // Make relays to all new subordinates organizations.

        boolean subordinatesAdded = false ;
        for (int i = 0; i < newSubordinates.size(); i++) {
            Organization subOrg = (Organization) newSubordinates.get(i) ;
            if ( gmrt.getRelay( subOrg.getMessageAddress() ) == null ) {
                gmrt.addRelay( subOrg.getMessageAddress() ) ;
                System.out.println(getAgentIdentifier() + " adding relay to subordinate "
                        + subOrg.getMessageAddress() );
            }

            if ( subOrg.getOrganizationPG().inRoles( Role.getRole("Combat") ) ) {
                System.out.println( getAgentIdentifier() + ":: ADDING subordinate combat organization " + subOrg);
                subordinateCombatOrganizations.put( subOrg.getMessageAddress().getAddress(), subOrg ) ;
                subordinatesAdded = true ;
            }
        }

    }


    public void getConfigInfo() {
        Collection params = getParameters() ;
        Vector paramVector = new Vector( params ) ;
        LoggingService log = (LoggingService)
                getServiceBroker().getService( this, LoggingService.class, null ) ;

        String fileName = null ;
        if ( paramVector.size() > 0 ) {
            fileName = ( String ) paramVector.elementAt(0) ;
        }

        try {
            ConfigFinder finder = getConfigFinder() ;
            if ( fileName != null && finder != null ) {
                File f = finder.locateFile( fileName ) ;

                // Save the configruation data
                try {
                    FileInputStream fis = new FileInputStream( f ) ;
                    configBytes = new byte[ fis.available() ] ;
                    fis.read( configBytes ) ;
                    fis.close();
                }
                catch ( Exception e ) {
                    e.printStackTrace();
                }

                // DEBUG -- Replace by call to log4j
                log.info( "BDEAgentPlugin configuring from " + f ) ;

                if ( f != null && f.exists() ) {
                    Document doc = null ;
                    try {
                        doc = finder.parseXMLConfigFile(fileName);
                    }
                    catch ( Exception e ) {
                        System.out.println( e ) ;
                    }

                    if ( doc != null ) {
                        try {
                            Node root = doc.getDocumentElement() ;
                            if( root.getNodeName().equals( "CPConfig" ) ) {
                                Integer val = ConfigParserUtils.parseIntegerValue( doc, "BDEReplanPeriod", "value" ) ;
                                if ( val != null ) {
                                    replanPeriodCondition.setValue( val );
                                }

                                val = ConfigParserUtils.parseIntegerValue( doc, "BDEPlanningDepth", "value" ) ;
                                if ( val != null ) {
                                    planningDepthCondition.setValue( val );
                                }

                                val = ConfigParserUtils.parseIntegerValue( doc, "BDEPlanningBreadth", "value" ) ;
                                if ( val != null ) {
                                    planningBreadthCondition.setValue( val );
                                }
                                val = ConfigParserUtils.parseIntegerValue( doc, "BDEPlanningDelay", "value" ) ;
                                if ( val != null ) {
                                    planningDelayCondition.setValue( val );
                                }

                                val = ConfigParserUtils.parseIntegerValue( doc, "BDEUpdateStatusNIU", "value" ) ;
                                if ( val != null ) {
                                    updateStatusNIU = val.intValue() ;
                                }
                            }
                        }
                        catch ( Exception e ) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        catch ( Exception e ) {
            e.printStackTrace();
        }

    }

    public void processMessage(Object o) {
        if ( o instanceof MessageEvent ) {
            MessageEvent event = (MessageEvent) o ;
            if ( event.getSource().getAddress().equals( Constants.WORLD_STATE_AGENT ) ) {
                processMessageFromWorldState( event ) ;
            }
            else {
                Organization org = (Organization) subordinateCombatOrganizations.get( event.getSource().getAddress() ) ;
                if ( org != null ) {
                    processMessageFromSubordinate( event ) ;
                }
            }
        }
    }

    protected void processMessageFromSubordinate(MessageEvent message) {

        if (message instanceof BNStatusUpdateMessage) {
            BNStatusUpdateMessage wsum = (BNStatusUpdateMessage) message;

            WorldStateModel statusReport = wsum.getWorldStateModel() ;
            // QoS measurement.
            //measureUpdateUnitStatusDelays(wsum);
            //
            BNAggregate agg = (BNAggregate) referenceZoneWorld.getAggUnitEntity( wsum.getBnUnitId() ) ;
            for (int i=0;i<agg.getNumSubEntities();i++) {
                UnitEntity newEntity = (UnitEntity) statusReport.getEntity( agg.getSubEntityName(i) ) ;
                org.cougaar.cpe.model.EntityInfo info = referenceZoneWorld.getEntityInfo(newEntity.getId());
                org.cougaar.cpe.model.UnitEntity pue;

                // Fill in the perceived message state.
                if (info == null) {
                    referenceZoneWorld.addEntity((UnitEntity) newEntity.clone(),
                            new BinaryEngageByFireModel(0));
                    info = referenceZoneWorld.getEntityInfo(newEntity.getId());
                } else {
                    pue = (org.cougaar.cpe.model.UnitEntity) info.getEntity();
                    pue.setX(newEntity.getX());
                    pue.setY(newEntity.getY());
                    pue.setAmmoQuantity(newEntity.getAmmoQuantity());
                    pue.setFuelQuantity(newEntity.getFuelQuantity());
                }
            }

            // TODO merge the target locations based on fidelity/error.
            mergeSensorValues( statusReport ) ;

            long startTime = System.currentTimeMillis();

            if (updateStatusNIU > 0) {
                CPUConsumer.consumeCPUMemoryIntensive(updateStatusNIU);
            }

            long endTime = System.currentTimeMillis();
            // updateUnitStatusDelayMP.addMeasurement(new DelayMeasurement(null, null, null, startTime, endTime));
        }
    }

    private void mergeSensorValues(WorldStateModel statusReport)
    {
        Iterator newTargets = statusReport.getTargets() ;
        while (newTargets.hasNext())
        {
            TargetEntity newSensedTarget = (TargetEntity) newTargets.next();
            TargetContact currentContact = (TargetContact) referenceZoneWorld.getEntity( newSensedTarget.getId() );
            if ( currentContact != null ) {
                if ( newSensedTarget instanceof TargetContact ) {
                    TargetContact newContact = (TargetContact) newSensedTarget ;
                    if ( newContact.getXError() < currentContact.getXError() && newContact.getTimeStamp() >= ( currentContact.getTimeStamp() - 10000) )
                    {
                        currentContact.setPosition( newContact.getX(), newContact.getY() );
                        currentContact.setError( newContact.getXError(), newContact.getYError() );
                    }
                }
                else {
                    currentContact.setPosition( newSensedTarget.getX(), newSensedTarget.getY() );
                    currentContact.setError( 0, 0 );
                }
            }
            else {
                currentContact = (TargetContact) newSensedTarget.clone() ;
                referenceZoneWorld.addEntity( currentContact );
            }
        }
    }

    public int getPlanningDelay() {
        if ( planningDelayCondition != null ) {
            Integer intValue = (Integer) planningDelayCondition.getValue() ;
            return intValue.intValue() ;
        }
        throw new RuntimeException( "Planning delay operating mode is not initialized.") ;

    }

    public int getReplanPeriod() {
        if ( replanPeriodCondition != null ) {
            Integer intValue = (Integer) replanPeriodCondition.getValue() ;
            return intValue.intValue() ;
        }
        throw new RuntimeException( "ReplanPeriod operating mode is not initialized.") ;
    }

    public int getPlanningDepth() {
        if ( planningDepthCondition != null ) {
            Integer intValue = (Integer) planningDepthCondition.getValue() ;
            return intValue.intValue() ;
        }
        throw new RuntimeException( planningDepthCondition.getName() + " operating mode is not initialized.") ;
    }


    protected void processStartMessage( StartMessage msg ) {
        this.baseTime = msg.getBaseTime() ;
        long replanTime = getReplanPeriod() * deltaTPerPlanningPhase * referenceZoneWorld.getDeltaTInMS() ;
        logger.shout( "Starting time for " + getAgentIdentifier() + " with replan period " + replanTime / 1000 + " secs.");

        ProcessReplanTimer( null );

        gmrt.setAlarm( "ProcessReplanTimer", "ReplanTimer", replanTime, true );

        // Send initial zone assigments to all subordinates.
    }

    protected void doConfigure( ConfigureMessage cm ) {
        logger.shout( " Configuring " + getAgentIdentifier() );
        referenceZoneWorld = ( ZoneWorld ) cm.getWorldStateModel() ;

        worldStateRef = new WorldStateReference( "ZoneWorld", referenceZoneWorld ) ;
        getBlackboardService().publishAdd( worldStateRef );

        ArrayList aggEntities = new ArrayList() ;
        for (int i=0;i<referenceZoneWorld.getNumAggUnitEntities();i++) {
            BNAggregate agg = (BNAggregate) referenceZoneWorld.getAggUnitEntity( i ) ;
            System.out.println("Initialize aggregrate with " + agg.getCurrentZone() + " zone.");
            aggEntities.add( agg.getId() ) ;
            IndexedZone currentZone = (IndexedZone) agg.getCurrentZone() ;

            // Make configuration message and send it to the subordinate.
            ZoneTask t = new ZoneTask( referenceZoneWorld.getTime(), deltaTPerPlanningPhase * referenceZoneWorld.getDeltaTInMS(),
                                       referenceZoneWorld.getIntervalForZone( currentZone ), referenceZoneWorld.getIntervalForZone( currentZone ) ) ;
            Plan p = new Plan(t) ;
            ZoneScheduleMessage msg = new ZoneScheduleMessage( p ) ;
            if ( subordinateCombatOrganizations.get( agg.getId() ) == null ) {
                logger.warn( "Subordinate unit with id=" + agg.getId() + " not found as subordinate." );
            }
            else {
                gmrt.sendMessage( MessageAddress.getMessageAddress( agg.getId() ),
                        msg );
            }
        }

        zonePlanner = new ZonePlanner( aggEntities, referenceZoneWorld, deltaTPerPlanningPhase ) ;
        zonePlanner.setMaxBranchFactor( getPlanningBreadth() );
        zonePlanner.setMaxDepth( getPlanningDepth() );

        System.out.println("\n-------------------------------------------------------");
        System.out.println("Agent \"" + getAgentIdentifier() + "\" INITIAL CONFIGURATION" );
        System.out.println();
        System.out.println("Deltas Per Phase=" + deltaTPerPlanningPhase + ",time=" + referenceZoneWorld.getDeltaT() * deltaTPerPlanningPhase + " .sec" );
        System.out.println("PlanningDepth=" + getPlanningDepth() + " phases, horizon=" + referenceZoneWorld.getDeltaT() * getPlanningDepth() + " .sec");
        System.out.println("PlanningBreadth=" + getPlanningBreadth() + " branches per ply.");
        System.out.println("ReplanPeriod=" + getReplanPeriod() + " planning phases (" + referenceZoneWorld.getDeltaT() * deltaTPerPlanningPhase * getReplanPeriod() + " sec.)" );
        System.out.println("Planning Delay=" + getPlanningDelay() + " planning phases (" + referenceZoneWorld.getDeltaT() * deltaTPerPlanningPhase * getPlanningDelay() + " sec.)" );
        //System.out.println("UpdateStatusNIU=" + updateStatusNIU );
        System.out.println("-------------------------------------------------------\n");

        // Send a dummy zone schedule to all subordinates with the

    }

    public Zone getCurrentZone( Plan zoneSchedule, long time ) {
        Zone zone = null;

        if ( zoneSchedule != null ) {
            for (int i=0;i<zoneSchedule.getNumTasks();i++) {
                ZoneTask t = (ZoneTask) zoneSchedule.getTask( i ) ;
                if ( t.getStartTime() <= time && t.getEndTime() >= time ) {
                     zone = t.getEndZone() ;
                }
            }
        }

        // Always return the last task if we are after the time, always return the first task
        // if we are before the time.
        if ( zoneSchedule.getNumTasks() > 0 ) {
            if ( time > zoneSchedule.getTask( zoneSchedule.getNumTasks() - 1).getStartTime() ) {
                zone = ( ( ZoneTask) zoneSchedule.getTask( zoneSchedule.getNumTasks() - 1 ) ).getStartZone() ;
            }
            else if ( time < zoneSchedule.getTask( 0).getStartTime() ) {
                zone  = ( (ZoneTask) zoneSchedule.getTask( 0 ) ).getEndZone() ;
            }
        }

        return zone ;
    }

    /**
     * Does zone based planning and distributes the results to any subordinate organizations.
     *
     * @param event
     */
    public void ProcessReplanTimer( TimerEvent event ) {
        // Now, find out what the current plans are and what the scheduled zones ought to be based on the current plans.
        // Run the reference zone forward until matches the current base time.
        // Update the location of the aggregate units.
        ArrayList units = zonePlanner.getSubordinateUnits() ;
        for (int i = 0; i < units.size(); i++) {
            String id = (String)units.get(i);
            BNAggregate agg = (BNAggregate) referenceZoneWorld.getAggUnitEntity( id ) ;
            Plan p = agg.getZonePlan() ;
            if ( p != null ) {
                Zone z = getCurrentZone( p, referenceZoneWorld.getTime() ) ;
                if ( z != null ) {
                    agg.setCurrentZone( z );
                }
            }
        }

        zonePlanner.plan( referenceZoneWorld, getPlanningDelay() * deltaTPerPlanningPhase * referenceZoneWorld.getDeltaTInMS() );
        Object[][] plans = zonePlanner.getPlans( false ) ;

        for (int i = 0; i < plans.length; i++) {
            Object[] plan = plans[i];
            String unitId = (String) plan[0] ;
            Plan zonePlan = (Plan) plan[1] ;
            BNAggregate agg = (BNAggregate) referenceZoneWorld.getAggUnitEntity( unitId ) ;
            agg.setZonePlan( zonePlan );
        }

        plans = zonePlanner.getPlans( true ) ;

        for (int i = 0; i < plans.length; i++)
        {
            Object[] plan = plans[i];
            String unitId = (String) plan[0];
            Plan zonePlan = (Plan) plan[1];
            Organization org = (Organization) subordinateCombatOrganizations.get(unitId);
            if (org != null)
            {
                ZoneScheduleMessage zsm = new ZoneScheduleMessage(zonePlan);
                gmrt.sendMessage(org.getMessageAddress(), zsm);
            }
        }
    }

    protected int getPlanningBreadth() {
        if ( planningBreadthCondition!= null ) {
            Integer intValue = (Integer) planningBreadthCondition.getValue() ;
            return intValue.intValue() ;
        }
        throw new RuntimeException( "ReplanPeriod operating mode is not initialized.") ;
    }

    protected void processMessageFromWorldState( MessageEvent event ) {
        if ( event instanceof StartMessage ) {
            processStartMessage( ( StartMessage ) event );
        }
        else if ( event instanceof ConfigureMessage ) {
            doConfigure( ( ConfigureMessage ) event ) ;
        }
        else if ( event instanceof UnitStatusUpdateMessage ) {
            processWorldStateUpdateMessage( ( UnitStatusUpdateMessage ) event ) ;
        }
    }

    /**
     * Process change to the world state as reported by the simulator.
     *
     * @param event
     */
    protected void processWorldStateUpdateMessage( UnitStatusUpdateMessage event ) {
        WorldStateModel sensedWorldState = event.getWorldState() ;
        Iterator newTargets = sensedWorldState.getTargets() ;

        // DEBUG
        if ( event.getSource().getAddress().equals(Constants.WORLD_STATE_AGENT) ) {
            // Update the official time associated with the zone world.
            referenceZoneWorld.setTime( sensedWorldState );
            logger.shout( "Agent " + getAgentIdentifier() + ":: Updating world state to time " + referenceZoneWorld.getTime() +
                    ",measured elapsed time=" + ( System.currentTimeMillis() - baseTime ) );

        }
        // Plan with the coarsest target contact.  Do not assume sensor updates from the
        // clients.

        // Copy the newTargets.
        while (newTargets.hasNext()) {
            TargetContact targetContact = (TargetContact) newTargets.next();
            //System.out.println("Reference zone world=" + referenceZoneWorld );
            TargetContact currentContact = (TargetContact) referenceZoneWorld.getEntity( targetContact.getId() ) ;
            if ( currentContact == null ) {
                referenceZoneWorld.addEntity( ( Entity ) targetContact.clone() );
            }
            else {
                currentContact.setPosition( targetContact.getX(), targetContact.getY() );
                currentContact.setStrength( targetContact.getStrength() );
                currentContact.setSuppressed( targetContact.isSuppressed(), sensedWorldState.getTime() );
            }
//            else {
//                // Minimize error
//                if ( targetContact.getXError() < currentContact.getXError() ) {
//
//                }
//            }
        }

        ArrayList targetsToBeRemoved = new ArrayList();
        Iterator oldTargets = referenceZoneWorld.getTargets() ;
        while (oldTargets.hasNext()) {
            TargetContact targetContact = (TargetContact) oldTargets.next();
            TargetContact newTargetcontact = (TargetContact) sensedWorldState.getEntity( targetContact.getId() ) ;
            if ( newTargetcontact == null ) {
               targetsToBeRemoved.add( targetContact.getId() ) ;
            }
        }

        // Remove the non-visible targets
        for (int i=0;i<targetsToBeRemoved.size();i++) {
            String id = (String) targetsToBeRemoved.get(i) ;
            referenceZoneWorld.deleteTarget( id ) ;
        }
//        System.out.println("Modified zone world=" + referenceZoneWorld );
        referenceZoneWorld.setBaseTime( sensedWorldState.getTime() );

        getBlackboardService().publishChange( worldStateRef );

    }

    public void execute() {
        processOrganizations();

        gmrt.execute( getBlackboardService() );
    }

    LoggingService logger ;
    WorldStateReference worldStateRef ;
    ZoneWorld referenceZoneWorld ;
    boolean isConfigured = false ;
    GenericRelayMessageTransport gmrt ;
}