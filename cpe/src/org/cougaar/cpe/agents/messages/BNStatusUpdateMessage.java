/*
 * <copyright>
 *  Copyright 2004 Intelligent Automation, Inc.
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

package org.cougaar.cpe.agents.messages;

import org.cougaar.tools.techspecs.events.MessageEvent;
import org.cougaar.cpe.model.WorldStateModel;

public class BNStatusUpdateMessage  extends MessageEvent {

    public BNStatusUpdateMessage(String bnUnitId, WorldStateModel wsm) {
        this.bnUnitId = bnUnitId;
        this.wsm = wsm;
    }

    public WorldStateModel getWorldStateModel() {
        return wsm;
    }

    public String getBnUnitId() {
        return bnUnitId;
    }

    protected WorldStateModel wsm ;

    protected String bnUnitId ;
}

