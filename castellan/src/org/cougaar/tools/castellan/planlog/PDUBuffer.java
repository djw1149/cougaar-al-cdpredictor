/*
  * <copyright>
  *  Copyright 2002 (Intelligent Automation, Inc.)
  *  under sponsorship of the Defense Advanced Research Projects
  *  Agency (DARPA).
  *
  *  This program is free software; you can redistribute it and/or modify
  *  it under the terms of the Cougaar Open Source License as published by
  *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
  *
  *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
  *  PROVIDED "AS IS" WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
  *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
  *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
  *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
  *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
  *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
  *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
  *  PERFORMANCE OF THE COUGAAR SOFTWARE.
  *
  * </copyright>
  *
  */

package org.cougaar.tools.castellan.planlog;

import org.cougaar.tools.castellan.pdu.PDU;
import org.cougaar.tools.castellan.ldm.LogMessage;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A Cougaar blackboard object representing both incoming and outgoing PDUs.  The PlanEventLogLP
 * receives incoming PDUs for any server agents and places them onto the BB in a PDUBuffer.
 * Any interested plug-ins should subscribe to and clear the incoming
 *
 * <p> Currently only PDUs incoming to an agent designated as a PlanLogAgent are placed in the
 * PDUBuffer.
 */
public class PDUBuffer implements java.io.Serializable
{
    public int getSize() {
        return incoming.size() ;
    }

    /**
     * Incoming messages are characterized by log messages.  The LogServerPlugin will
     * decompress these into PDUs and deliver them appropriately.
     */
    public void addIncoming( PDU pdu ) {
        incoming.add( pdu ) ;
    }

    /**
     * PDUs are generated by LPs. The LogClientPlugin will translate these into LogMessages
     * for subsequent delivery using a Relay approach.
     */
    public void addOutgoing( PDU pdu ) {
        outgoing.add( pdu ) ;
    }

    public Iterator getOutgoing() {
        return outgoing.iterator() ;
    }

    public Iterator getIncoming() {
        return incoming.iterator() ;
    }

    public void clearIncoming() {
        incoming.clear();
    }

    public void clearOutgoing() {
        outgoing.clear() ;
    }

    ArrayList outgoing = new ArrayList( 500 ) ;
    ArrayList incoming = new ArrayList( 800 ) ;
}