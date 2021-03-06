/*
 * <copyright>
 *  Copyright 2003-2004 Intelligent Automation, Inc.
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 *
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.cpe.agents.plugin;

import org.cougaar.core.agent.service.alarm.Alarm;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.blackboard.Subscription;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.adaptivity.OperatingModeCondition;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.glm.ldm.asset.Organization;
import org.cougaar.util.UnaryPredicate;
import org.cougaar.util.ConfigFinder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.cougaar.cpe.agents.messages.*;
import org.cougaar.cpe.agents.qos.QoSConstants;
import org.cougaar.cpe.model.Plan;
import org.cougaar.cpe.model.UnitEntity;
import org.cougaar.cpe.model.VGWorldConstants;
import org.cougaar.cpe.model.WorldStateModel;
import org.cougaar.cpe.relay.GenericRelayMessageTransport;
import org.cougaar.cpe.relay.MessageSink;
import org.cougaar.cpe.relay.SourceBufferRelay;
import org.cougaar.cpe.relay.TargetBufferRelay;
import org.cougaar.tools.techspecs.qos.*;
import org.cougaar.cpe.util.CPUConsumer;
import org.cougaar.cpe.util.OMCMPManager;
import org.cougaar.tools.techspecs.events.MessageEvent;
import org.cougaar.tools.techspecs.events.TimerEvent;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.io.*;

public class UnitAgentPlugin extends ComponentPlugin implements MessageSink {

	public static final String MODE_HIGH_FIDELITY = "HighFidelity";

	public static final String MODE_MEDIUM_FIDELITY = "MediumFidelity";

	private IncrementalSubscription myOrgsSubscription;

	/**
	 * Relay for sending messages to the superior.
	 */
	private TargetBufferRelay relayFromSuperior;

	/**
	 * Relay for receiving messages from the world state agent.
	 */
	private SourceBufferRelay worldStateRelay;

	private Organization superior;
	private ArrayList suppliers = new ArrayList();
	private IncrementalSubscription relaySubscription;

	/**
	 * Timebase for simulation.
	 */
	private long baseTime;
	GenericRelayMessageTransport mt;
	private DelayMeasurementPoint manueverPlanFreshnessMP;
	private OMCRangeList worldStateUpdateList =
		new OMCRangeList(
			new int[] { 5000, 10000, 15000, 20000, 25000, 30000, 35000, 40000 });
	private long updateStatusNIU;
	private long updateManueverNIU;
	private boolean started;

	/**
	 * Measure the time necessary to plan.
	 */
	private EventDurationMeasurementPoint updateUnitStatusTimeMP;

	private byte[] configBytes;
	private LoggingService ls;
	private String UNIT_STATUS_UPDATE_TIMER = "UnitStatusUpdateTimer";

	/**
	* Measurement Points for the 2 classes of traffic dealt with in CPY
	*/
	private DelayMeasurementPoint ProcessPlanMP;
	private	DelayMeasurementPoint ProcessUpdateMP;
    private OMCMPManager omcMPManager;
    private IncrementalSubscription mpSubscription;

    protected void execute() {
		System.out.print("E(" + getAgentIdentifier() + ")");
		Collection c = myOrgsSubscription.getAddedCollection();
		if (!c.isEmpty()) {
			processNewOrganizations(c);
		}

		// Search for Target relays from superior.
		c = relaySubscription.getAddedCollection();
		Iterator iter = c.iterator();
		while (iter.hasNext()) {
			Object o = iter.next();
			if (o instanceof TargetBufferRelay) {
				TargetBufferRelay relay = (TargetBufferRelay) o;
				if (superior != null
					&& superior.getMessageAddress().equals(relay.getSource())) {
					System.out.println(
						getAgentIdentifier()
							+ " found relay from superior "
							+ relay.getSource());
					relayFromSuperior = relay;
				} else {
					System.err.println(
						"ERROR:: Could not connect incoming relay with null superior organization.");
					break;
				}
			}
		}

        omcMPManager.execute( perceivedWorldState == null ? 0 : perceivedWorldState.getTime() );

		//
		// Always execute the mt.
		//
		mt.execute(getBlackboardService());
	}

	public long getWorldStateUpdatePeriodInMillis() {
		Integer value = (Integer) statusUpdatePeriodOMC.getValue();
		return (long) (value.intValue());
	}

	private void processNewOrganizations(Collection c) {
		ArrayList self = new ArrayList(),
			newSubordinates = new ArrayList(),
			newSuperiors = new ArrayList(),
			newCustomers = new ArrayList(),
			newProviders = new ArrayList();
		OrganizationHelper.processSuperiorAndSubordinateOrganizations(
			getAgentIdentifier(),
			c,
			newSuperiors,
			newSubordinates,
			self);
		OrganizationHelper.processCustomerAndProviderOrganizations(
			getAgentIdentifier(),
			c,
			newCustomers,
			newProviders);

		// Find superior orgs.
		for (int i = 0; i < newSuperiors.size(); i++) {
			Organization organization = (Organization) newSuperiors.get(i);
			if (superior == null) {
				superior = organization;
			} else if (
				!superior.getMessageAddress().getAddress().equals(
					organization.getMessageAddress().getAddress())) {
				System.err.println(
					"Warning: More than one superior "
						+ superior
						+ " and "
						+ organization
						+ " found for "
						+ getAgentIdentifier());
			}
		}

		// Find new provider organizations.
		for (int i = 0; i < newProviders.size(); i++) {
			Organization organization = (Organization) newProviders.get(i);
			suppliers.add(organization);
			System.out.println(
				getAgentIdentifier() + " found supplier " + organization);
			mt.addRelay(organization.getMessageAddress());
		}
	}

	public void processMessage(Object o) {
		// System.out.println( getAgentIdentifier() + ":: RECEIVED MESSAGE " + o );
		if (o instanceof MessageEvent) {
			MessageEvent cpem = (MessageEvent) o;
			if (cpem
				.getSource()
				.getAddress()
				.equals(superior.getMessageAddress().getAddress())) {
				//TODO NG: Need RT (plan)
				processMessageFromSuperior(cpem);
			} else if (cpem.getSource().getAddress().equals("WorldState")) {
				processMessageFromWorldState(cpem);
			}
		}
	}

	protected void processMessageFromSuperior(MessageEvent msg) {
		if (msg instanceof ManueverPlanMessage) {
			long startTime = System.currentTimeMillis();
			boolean wasOpen = true;
			if (!getBlackboardService().isTransactionOpen()) {
				wasOpen = false;
				getBlackboardService().openTransaction();
			}


			ManueverPlanMessage mpm = (ManueverPlanMessage) msg;
			if (!mpm
				.getEntityName()
				.equals(getAgentIdentifier().getAddress())) {
				ls.error(
					"Manuever plan "
						+ mpm
						+ " has unexpected entity name "
						+ mpm.getEntityName()
						+ ", expected="
						+ getAgentIdentifier());
				return;
			}

			// DEBUG
			//TODO NG: Need PBT (plan)
			ls.shout(getAgentIdentifier() + ":: Received plan from superior.");

			// This is the actual processing to resolve the new plan against the local plan
			// Set the local manuever plan, overwriting any existing plan.  We need to confirm
			// if this is correct.

			entity.updateManeuverPlan(mpm.getPlan());

			//
			//
			// Consume CPU, simulating the local reconciliation of two plans.
			//
			if (updateManueverNIU > 0) {
				CPUConsumer.consumeCPUMemoryIntensive(updateManueverNIU);
			}

			// Measure freshness QoS.
			MeasurementChain chain = mpm.getMeasurements();

			// System.out.println("\n" + getAgentIdentifier() + " DEBUG:: processMessageFromSuperior():  Found measurement chain from superior " + chain);
			long currentTime = System.currentTimeMillis();
			if (chain.getNumMeasurements() > 0) {
				// Look for the vector measurement of freshness.

				VectorMeasurement pm = null;

				// For now, just find the first TimestampMeasurement and signal this as the freshness
				// of the manuever plan.
				for (int i = 0; i < chain.getNumMeasurements(); i++) {
					Measurement m = chain.getMeasurement(i);
					// System.out.println( getAgentIdentifier() + ":: DEBUG Looking at message " + m );
					// Find the timer that triggered this action.
					if (m instanceof VectorMeasurement
						&& m.getAction().equals(
							QoSConstants.PLAN_MANUEVER_ACTION)) {
						//&& m.getAction() != null
						//&& m.getAction().equals(QoSConstants.TIMER_ACTION) ) {
						pm = (VectorMeasurement) m;
						break;
					}
				}

				DelayMeasurement dm = null;
				// System.out.println( getAgentIdentifier() + " CALCULATING FRESHNESS FROM " + pm );

				// Now, find the freshness of the ManueverPlanMessage
				if (pm != null) {
					// Measure and retain the delay between mplan generation and mplan
					//

					// This is the timestamp associated with my generation of the world state.
					DelayMeasurement unitStatusTimestamp =
						(DelayMeasurement) pm.getMeasurement(
							getAgentIdentifier().toString()
								+ ".WorldStateTimestamp");
					// System.out.println("FOUND " + unitStatusTimestamp );
					if (unitStatusTimestamp.getTimestamp() > currentTime) {
						System.out.println(
							"Warning: UnitStatusTimestamp "
								+ unitStatusTimestamp.getTimestamp()
								+ " is after local timestamp "
								+ currentTime);
					}

					if (unitStatusTimestamp != null) {
						dm =
							new DelayMeasurement(
								QoSConstants.UPDATE_MANUEVER_PLAN_ACTION,
								QoSConstants.RECEIVE_ACTION,
								getAgentIdentifier(),
								unitStatusTimestamp.getTimestamp(),
								currentTime);
					}
				}
				//                else {
				//                    dm = new DelayMeasurement( QoSConstants.PLAN_MANUEVER_ACTION,
				//                            QoSConstants.RECEIVE_ACTION, getAgentIdentifier(),
				//                            TimestampMeasurement.UNKNOWN_TIMESTAMP, currentTime ) ;
				//                }
				//System.out.println( getAgentIdentifier() + "::DEBUG PM=" + pm );
				//System.out.println( getAgentIdentifier() + "::DEBUG New ManueverPlanFreshnessMP " + dm );
				if (dm != null) {
					manueverPlanFreshnessMP.addMeasurement(dm);
				} else {
					System.err.println(
						"Unable to measure freshness for " + chain);
				}
			}

			// Chain a measurement here to the mpm before forwarding it.
			mpm.getMeasurements().addMeasurement(
				new TimestampMeasurementImpl(
					QoSConstants.RECEIVE_ACTION,
					QoSConstants.UPDATE_MANUEVER_PLAN_ACTION,
					getAgentIdentifier(),
					currentTime));

			// Make a new manuever plan message.
			ManueverPlanMessage newMpm =
				new ManueverPlanMessage(
					getAgentIdentifier().getAddress(),
					entity.getManueverPlan());
			newMpm.setMeasurements(mpm.getMeasurements());

			// Deliver the new (resolved) plan
			// straight to the WorldStateAgent for execution.
			mt.sendMessage(worldStateRelay.getTarget(), newMpm);

			// Just find the zeroth supplier and forward it.
			if (suppliers.size() != 0) {
				Organization sorg = (Organization) suppliers.get(0);
				ManueverPlanMessage mpmToSupplier =
					new ManueverPlanMessage(
						getAgentIdentifier().getAddress(),
						entity.getManueverPlan());
				newMpm.setMeasurements(mpm.getMeasurements());
				// System.out.println( getAgentIdentifier() + " UNIT SENDING MANUEVER PLAN TO SUPPLIER " + sorg );
				mt.sendMessage(sorg.getMessageAddress(), mpmToSupplier);
				//mt.sendMessage( su);
			}
			//TODO NG: Need ET (plan)
			ProcessPlanMP.addMeasurement(
				new DelayMeasurement(
					"ProcessPlan",
					"ProcessPlan",
					getAgentIdentifier(),
					startTime,
					System.currentTimeMillis()));

			if (getBlackboardService().isTransactionOpen() && !wasOpen) {
				getBlackboardService().closeTransaction();
			}
		}
	}

	protected void processMessageFromWorldState(MessageEvent msg) {
		if (msg instanceof StartMessage) {
			startTimeAdvance((StartMessage) msg);
		} else if (msg instanceof StopMessage) {
			started = false;
		} else if (msg instanceof UnitStatusUpdateMessage) {
			// Update the world state with no delay.
			//System.out.println( getAgentIdentifier() + "::RECEIVED WorldStateModel " + perceivedWorldState );
			UnitStatusUpdateMessage wsum = (UnitStatusUpdateMessage) msg;
			perceivedWorldState = wsum.getWorldState();

			// Copy the old manuever plan to the new entity.
			Plan manueverPlan = null;
			if (entity != null) {
				manueverPlan = entity.getManueverPlan();
			}
			refToWorldState.setState(perceivedWorldState);
			getBlackboardService().publishChange(refToWorldState);

			// Copy the manuever plan over to the new entity
			entity = wsum.getEntity();
			if (entity != null && manueverPlan != null) {
				entity.setManueverPlan(manueverPlan);
			}
		} else if (msg instanceof PublishMPMessage) {
			BundledMPMessage bmm = new BundledMPMessage();
			for (Iterator iterator = mpSubscription.iterator(); iterator.hasNext();) {
                MeasurementPoint mp = (MeasurementPoint) iterator.next();
                bmm.addData( mp ) ;
            }

			// Write the configuration data file.
			Collection params = getParameters();
			Vector paramVector = new Vector(params);
			String fileName = null;
			if (paramVector.size() > 0 && configBytes != null) {
				fileName = (String) paramVector.elementAt(0);
				bmm.addData(fileName, configBytes);
			}

			mt.sendMessage(msg.getSource(), bmm);

		}
        else if ( msg instanceof ConfigureMessage ) {
            ConfigureMessage cm = (ConfigureMessage) msg ;
            byte[] paramDoc = cm.getParamConfigurationDocument() ;
            if ( paramDoc != null ) {
                ls.shout( "CONFIGURING FROM DOCUMENT ") ;
                ByteArrayInputStream bis = new ByteArrayInputStream( paramDoc ) ;
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance() ;
                try {
                    DocumentBuilder builder = factory.newDocumentBuilder() ;
                    Document doc =builder.parse( bis ) ;
                    VGWorldConstants.setParameterValues( doc );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }

            refToWorldState.setState( cm.getWorldStateModel() );
        }
	}

	protected void processUnitStatusUpdateTimer() {
		//System.out.println( "\n\n UnitAgent " + getAgentIdentifier() + ":: EXECUTING WorldStateUpdateTimer...");
		//TODO NG: Need RT (update)
		long startTime = System.currentTimeMillis();
		BlackboardService bs = getBlackboardService();
		boolean wasOpen = false;
		try {
			if (!bs.isTransactionOpen()) {
				bs.openTransaction();
			} else {
				wasOpen = true;
			}

			// Now, broadcast a WorldStateUpdate message.
			org.cougaar.cpe.model.EntityInfo info =
				perceivedWorldState.getEntityInfo(
					getAgentIdentifier().getAddress());
			// Make an initial measurement of how much off the timer we were actually delayed?

			TimestampMeasurementImpl timerTM =
				new TimestampMeasurementImpl(
					null,
					QoSConstants.TIMER_ACTION,
					getAgentIdentifier(),
					startTime);

			//--------------------------------------------------------------------------------------
			// Consume some CPU time, based on a local statistical model and the current fidelity.  This is the
			// update status CPU information.
			//
			CPUConsumer.consumeCPUMemoryIntensive(updateStatusNIU);

			long endTime = System.currentTimeMillis();

			updateUnitStatusTimeMP.addMeasurement(
				new DelayMeasurement(
					"updateUnitStatusTimeMP",
					"updateUnitStatusTimeMP",
					getAgentIdentifier(),
					startTime,
					endTime));

			// Now, broadcast my state information.
			//TODO NG: Need PBT (update)
			if (info != null) {
				// Broadcast only my state information.
				WorldStateModel newModel =
					new WorldStateModel(
						perceivedWorldState,
						true,
						false,
						false);
				UnitEntity ue = (UnitEntity) entity.clone();
				newModel.addEntity(ue);
				UnitStatusUpdateMessage wsm =
					new UnitStatusUpdateMessage(
						(UnitEntity) entity.clone(),
						newModel);

				wsm.getMeasurements().addMeasurement(timerTM);
				// This is the timer action.
				wsm.getMeasurements().addMeasurement(
					new TimestampMeasurementImpl(
						QoSConstants.UPDATE_STATUS_ACTION,
						QoSConstants.SEND_ACTION,
						getAgentIdentifier(),
						endTime));
				// This is the send action.
				mt.sendMessage(superior.getMessageAddress(), wsm);
			} else {
				System.out.println(
					"WARNING:: perceivedWorldState=" + perceivedWorldState);
				System.out.println(
					"UnitAgentPlugin::executeWorldStateUpdateTimer::No entity info found for "
						+ getAgentIdentifier());
			}
			// Now, execute again.
			execute();

			ProcessUpdateMP.addMeasurement(
				new DelayMeasurement(
					"StatusUpdateProcessTimer",
					"StatusUpdateProcessTimer",
					getAgentIdentifier(),
					startTime,
					System.currentTimeMillis()));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Now, reset the alarm service to the update period.
			if (started) {
				long nextTime = getWorldStateUpdatePeriodInMillis();
				// System.out.println( getAgentIdentifier() + ":  SCHEDULING NEXT WORLD STATE UPDATE " + nextTime / 1000 + " secs.");
				getAlarmService().addRealTimeAlarm(
					new UpdateStateAlarm(nextTime));
			}

			//			TODO NG: Need ET(update)

			if (!wasOpen && bs.isTransactionOpen()) {
				bs.closeTransaction();
			}
		}

	}

	/**
	 * Update the world state, depending on the
	 */

	public void ProcessUnitStatusUpdateTimer(TimerEvent e) {
		// DEBUG
		ls.shout(
			"\n\n UnitAgent "
				+ getAgentIdentifier()
				+ ":: EXECUTING WorldStateUpdateTimer...");
		//        BlackboardService bs = getBlackboardService() ;
		//        try {
		//            bs.openTransaction();
		// Now, broadcast a WorldStateUpdate message.
		org.cougaar.cpe.model.EntityInfo info =
			perceivedWorldState.getEntityInfo(
				getAgentIdentifier().getAddress());
		// Make an initial measurement of how much off the timer we were actually delayed?
		long startTime = System.currentTimeMillis();
		TimestampMeasurementImpl timerTM =
			new TimestampMeasurementImpl(
				null,
				QoSConstants.TIMER_ACTION,
				getAgentIdentifier(),
				startTime);

		//--------------------------------------------------------------------------------------
		// Consume some CPU time, based on a local statistical model and the current fidelity.  This is the
		// update status CPU information.
		//
		CPUConsumer.consumeCPUMemoryIntensive(updateStatusNIU);
		long endTime = System.currentTimeMillis();

		updateUnitStatusTimeMP.addMeasurement(
			new DelayMeasurement(
				null,
				null,
				getAgentIdentifier(),
				startTime,
				endTime));

		// Now, broadcast my state information.
		if (info != null) {
			WorldStateModel newModel =
				new WorldStateModel(perceivedWorldState, true, false, false);
			UnitEntity ue = (UnitEntity) entity.clone();
			newModel.addEntity(ue);
			UnitStatusUpdateMessage wsm =
				new UnitStatusUpdateMessage(
					(UnitEntity) entity.clone(),
					newModel);

			wsm.getMeasurements().addMeasurement(timerTM);
			// This is the timer action.
			wsm.getMeasurements().addMeasurement(
				new TimestampMeasurementImpl(
					QoSConstants.UPDATE_STATUS_ACTION,
					QoSConstants.SEND_ACTION,
					getAgentIdentifier(),
					endTime));
			// This is the send action.
			mt.sendMessage(superior.getMessageAddress(), wsm);
		} else {
			System.out.println(
				"WARNING:: perceivedWorldState=" + perceivedWorldState);
			System.out.println(
				"UnitAgentPlugin::executeWorldStateUpdateTimer::No entity info found for "
					+ getAgentIdentifier());
		}
		// Now, execute again.
		//            execute() ;
		//        }
		//        catch ( Exception e ) {
		//            e.printStackTrace();
		//        }
		//        finally {
		//            // Now, reset the alarm service to the update period.
		//            if ( started ) {
		//                long nextTime = getWorldStateUpdatePeriodInMillis() ;
		//                // System.out.println( getAgentIdentifier() + ":  SCHEDULING NEXT WORLD STATE UPDATE " + nextTime / 1000 + " secs.");
		//                getAlarmService().addRealTimeAlarm( new UpdateStateAlarm( nextTime ) ) ;
		//            }
		//            bs.closeTransaction() ;
		//        }
	}

	protected void startTimeAdvance(StartMessage msg) {
		started = true;
		System.out.println(
			getAgentIdentifier() + " UNIT AGENT Starting time advance... ");
		this.baseTime = msg.getBaseTime();
		// Set up UpdateState timer.  The timer is actually controlled by the adaptivity engine.
		//mt.setAlarm( "ProcessUnitStatusUpdateTimer", UNIT_STATUS_UPDATE_TIMER, getWorldStateUpdatePeriodInMillis(), true );
		getAlarmService().addRealTimeAlarm(
			new UpdateStateAlarm(getWorldStateUpdatePeriodInMillis()));
	}

	protected void setupSubscriptions() {
		ls =
			(LoggingService) getServiceBroker().getService(
				this,
				LoggingService.class,
				null);

		ls.info("Starting UnitAgent " + getAgentIdentifier());

		//System.out.println("Starting agent " + getAgentIdentifier() );
		mt =
			new GenericRelayMessageTransport(
				this,
				getServiceBroker(),
				getAgentIdentifier(),
				this,
				getBlackboardService());

		/**
		 * A predicate that matches all organizations that can
		 * fulfill the SoftwareDevelopment role
		 */
		class MyOrganizationsPredicate implements UnaryPredicate {
			public boolean execute(Object o) {
				return o instanceof Organization;
			}
		}
		myOrgsSubscription =
			(IncrementalSubscription) getBlackboardService().subscribe(
				new MyOrganizationsPredicate());

		// Use this to trigger any incoming/outgoing relays.
		relaySubscription =
			(IncrementalSubscription) getBlackboardService()
				.subscribe(new UnaryPredicate() {
			public boolean execute(Object o) {
				return (o instanceof TargetBufferRelay);
			}
		});

		// Hook up with the world state agent by publishing a relay.
		System.out.println(
			getAgentIdentifier() + ":: Connecting to WorldState agent.");
		worldStateRelay =
			mt.addRelay(MessageAddress.getMessageAddress("WorldState"));

		createMeasurementPoints();

		// Create the OperatingModeCondition for the update period in seconds
        createOperatingModes();

        refToWorldState = new WorldStateReference("PerceivedWorldState", null);
		getBlackboardService().publishAdd(refToWorldState);

		getConfigInfo();

		System.out.println("\n-----------------------------------------");
		System.out.println(
			"CPY Agent \"" + getAgentIdentifier() + "\" Configuration");
		System.out.println(
			"StatusUpdatePeriod(ms)=" + getWorldStateUpdatePeriodInMillis());
		System.out.println("UpdateStatusNIU=" + updateStatusNIU);
		System.out.println("UpdateManueverPlanNIU=" + updateManueverNIU);
		System.out.println("--------------------------------------------");
	}

    private void createOperatingModes() {
        statusUpdatePeriodOMC =
			new OperatingModeCondition(
				"WorldStateUpdatePeriod",
				worldStateUpdateList);
        statusUpdatePeriodOMC.setValue(new Integer(15000));
        getBlackboardService().publishAdd(statusUpdatePeriodOMC);
    }

    private void createMeasurementPoints() {
        omcMPManager = new OMCMPManager( getBlackboardService() ) ;
        mpSubscription = (IncrementalSubscription) getBlackboardService().subscribe( new UnaryPredicate() {
            public boolean execute(Object o) {
                return o instanceof MeasurementPoint ;
            }
        }) ;

		// Create a DelayMeasurementPoint associated with the NewManueverPlanMessage
		// This is the minimum time between when the CP agent takes the world
		// state and sends it to the BN agent and
		// when it receives a specific ManueverPlan from the BN agent and processes it.

		manueverPlanFreshnessMP =
			new DelayMeasurementPoint("ManueverPlanFreshness");
		getBlackboardService().publishAdd(manueverPlanFreshnessMP);

		// manueverPlanDelayMP = new DelayMeasurementPoint( "" ) ;
		// This is the time to process a status update timer.  This is CPU processing time only.
		updateUnitStatusTimeMP =
			new EventDurationMeasurementPoint("StatusUpdateProcessTime");
		getBlackboardService().publishAdd(updateUnitStatusTimeMP);

		ProcessPlanMP = new EventDurationMeasurementPoint("ProcessPlan");
		getBlackboardService().publishAdd(ProcessPlanMP);

		ProcessUpdateMP =
			new EventDurationMeasurementPoint("StatusUpdateProcessTimer");
		getBlackboardService().publishAdd(ProcessUpdateMP);

	}

	private String getNodeValueForTag(
		Document doc,
		String tagName,
		String namedItem) {
		NodeList nodes = doc.getElementsByTagName(tagName);

		String value = null;
		// Get target plan log
		for (int i = 0; i < nodes.getLength(); i++) {
			Node n = nodes.item(i);
			value = n.getAttributes().getNamedItem(namedItem).getNodeValue();
		}
		return value;
	}

	protected void getConfigInfo() {
		Collection params = getParameters();
		Vector paramVector = new Vector(params);
		LoggingService log =
			(LoggingService) getServiceBroker().getService(
				this,
				LoggingService.class,
				null);

		String fileName = null;
		if (paramVector.size() > 0) {
			fileName = (String) paramVector.elementAt(0);
		}

		try {
			ConfigFinder finder = getConfigFinder();

			if (fileName != null && finder != null) {

				File f = finder.locateFile(fileName);

				// DEBUG -- Replace by call to log4j
				System.out.println(
					"Unit Agent "
						+ getAgentIdentifier()
						+ " :: Configuring from "
						+ f);

				if (f != null && f.exists()) {

					try {
						FileInputStream fis = new FileInputStream(f);
						configBytes = new byte[fis.available()];
						fis.read(configBytes);
						fis.close();
					} catch (Exception e) {
						e.printStackTrace();
					}

					Document doc = null;
					try {
						doc = finder.parseXMLConfigFile(fileName);
					} catch (Exception e) {
						System.out.println(e);
					}

					if (doc != null) {
						try {
							Node root = doc.getDocumentElement();
							if (root.getNodeName().equals("CPConfig")) {
								String updateStatusNIUString =
									getNodeValueForTag(
										doc,
										"CPYUpdateStatusNIU",
										"value");
								if (updateStatusNIUString != null) {
									updateStatusNIU =
										Long.parseLong(updateStatusNIUString);
								}

								String updateManueverPlanNIUString =
									getNodeValueForTag(
										doc,
										"CPYUpdateManueverPlanNIU",
										"value");
								if (updateManueverPlanNIUString != null) {
									System.out.println(
										"CPYUpdateManueverPlanNIU="
											+ updateManueverPlanNIUString
											+ " found.");
									updateManueverNIU =
										Long.parseLong(
											updateManueverPlanNIUString);
								}

								String updateStatusPeriodString =
									getNodeValueForTag(
										doc,
										"CPYUpdateTimerPeriod",
										"value");
								if (updateStatusPeriodString != null) {
									System.out.println(
										"Tag CPYUpdateTimerPeriod="
											+ updateStatusPeriodString
											+ " found.");
									int periodInMillis =
										Integer.parseInt(
											updateStatusPeriodString);
									statusUpdatePeriodOMC.setValue(
										new Integer(periodInMillis));
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public class UpdateStateAlarm implements Alarm {
		private boolean expired;

		public UpdateStateAlarm(long period) {
			this.period = period;
			this.expirationTime = period + System.currentTimeMillis();
		}

		public boolean cancel() {
			return false;
		}

		public void reset(long currentTime) {
			expired = false;
			expirationTime = currentTime + period;
		}

		public void expire() {
			expired = true;
			// TODO
			// Move this out of the plugin and back into an event!
			processUnitStatusUpdateTimer();
			// mt.sendMessage( getAgentIdentifier(), new UpdateStateMessage());
		}

		public long getExpirationTime() {
			return expirationTime;
		}
		public boolean hasExpired() {
			return expired;
		}

		public long getPeriod() {
			return period;
		}

		public void setPeriod(long period) {
			this.period = period;
		}

		protected long expirationTime;
		protected long period;
	}

	/**
	 * Local perceived world state.
	 */
	String orgName;

	/**
	 * The state of the entity represented by this UnitAgentPlugin.
	 */
	UnitEntity entity;

	/**
	 * The state of the world represented by this UnitAgentPlugin.
	 */
	org.cougaar.cpe.model.WorldStateModel perceivedWorldState;

	WorldStateReference refToWorldState;

	/**
	 * The plan for this unit.
	 */
	Plan manueverPlan;

	String operatingModeFidelity = MODE_HIGH_FIDELITY;

	OperatingModeCondition statusUpdatePeriodOMC;

}
