package org.cougaar.tools.techspecs.results;

import org.cougaar.tools.techspecs.ActionMessageSpec;

/**
 * Represents a deterministic action result. The target role is the role of
 * the agent to which the action message is being delivered
 */
public class ActionMessageResult extends ActionResult
{
    private ActionMessageSpec spec;
    private String targetRole ;

    public ActionMessageResult( ActionMessageSpec spec, String targetRole )
    {
        super( ActionResult.OUTPUT_MESSAGE ) ;
        this.targetRole = targetRole ;
        this.spec = spec ;
    }

    /**
     * The type of message generated by thes result.
     * @return
     */
    public ActionMessageSpec getSpec()
    {
        return spec;
    }

    /**
     * The role which this action message is associated with.
     * @return
     */
    public String getTargetRole()
    {
        return targetRole;
    }
}