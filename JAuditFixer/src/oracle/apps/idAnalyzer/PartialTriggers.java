package oracle.apps.idAnalyzer;

import java.util.HashSet;

public class PartialTriggers {
    /**
     * Name of the trigger
     * This is the tagName of the attribute that could be a secondary container
     * e.g. partialTriggers, editPartialTriggers
     */
    String triggerName = "";

    /**
     * value of the trigger tag
     * Its the entire getValueOf on the node
     */
    String rawTriggerOn = "";

    /**
     * The componentIds that are referenced in the value string
     */
    HashSet<String> triggerOnSplitIds = new HashSet<String>(30);

    /**
     * Line number of this tag.
     * The dom parser returns the line number of the end of the start tag
     */
    Integer lineNumber = 0;

    /**
     * Only the violating component ids that are referenced in the value tag
     */
    HashSet<String> violatingCompId = new HashSet<String>(30);

    /**
     * Replacement string of the raw trigger
     * all the violating component ids in the raw trigger string are replaced with the new ids
     */
    String triggerReplacement = "";

    /**
     * Serialized string version of the entire tag where this attribute is found
     */
    String serializedTag = "";

    public PartialTriggers() {
        super();
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public void setRawTriggerOn(String rawTriggerOn) {
        this.rawTriggerOn = rawTriggerOn;
    }

    public String getRawTriggerOn() {
        return rawTriggerOn;
    }

    public void setTriggerOnSplitIds(HashSet<String> triggerOnSplitIds) {
        this.triggerOnSplitIds = triggerOnSplitIds;
    }

    public HashSet<String> getTriggerOnSplitIds() {
        return triggerOnSplitIds;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setViolatingCompId(HashSet<String> violatingCompId) {
        this.violatingCompId = violatingCompId;
    }

    public HashSet<String> getViolatingCompId() {
        return violatingCompId;
    }

    public void setTriggerReplacement(String triggerReplacement) {
        this.triggerReplacement = triggerReplacement;
    }

    public String getTriggerReplacement() {
        return triggerReplacement;
    }


    public void setSerializedTag(String serializedTag) {
        this.serializedTag = serializedTag;
    }

    public String getSerializedTag() {
        return serializedTag;
    }
}
