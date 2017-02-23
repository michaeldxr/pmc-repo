package oracle.apps.di.metadata;

import java.util.ArrayList;

import java.util.Iterator;

import oracle.apps.di.CheckerConstants;
import oracle.apps.di.checker.CheckerResult;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Worksheet extends ADFDiMetadata {
    String worksheetTitle = "";
    String protectionMode = "";
    ArrayList<WorksheetEvent> worksheetEvents = null;
    ArrayList<String> menuItems = null;
    String pageDefName="";

    public void setPageDefName(String pageDefName) {
        this.pageDefName = pageDefName;
    }

    public String getPageDefName() {
        return pageDefName;
    }

    public ArrayList<Worksheet.WorksheetEvent> getWorksheetEvents() {
        return worksheetEvents;
    }

    public String getWorksheetTitle() {
        return worksheetTitle;
    }

    public ArrayList<String> getMenuItems() {
        return menuItems;
    }
    //ActionSet worksheetActions=null;

    public class WorksheetEvent {
        String eventName = "";
        ActionSet eventActions = null;
        String invokeOnce = "";

        public WorksheetEvent(String name, ActionSet actions) {
            this.eventName = name;
            this.eventActions = actions;
        }

        public void setInvokeOnce(String invokeOnce) {
            this.invokeOnce = invokeOnce;
        }

        public String getInvokeOnce() {
            return invokeOnce;
        }

        public void setEventName(String eventName) {
            this.eventName = eventName;
        }

        public String getEventName() {
            return eventName;
        }

        public void setEventActions(ActionSet eventActions) {
            this.eventActions = eventActions;
        }

        public ActionSet getEventActions() {
            return eventActions;
        }
    }

    public Worksheet(String type) {
        super(type);
    }

    public static Worksheet getMetadata(XMLDocument xmlDoc, Element metadataNode) throws Exception {
        Worksheet metadata = new Worksheet(META_WORKSHEET);

        if (xmlDoc == null || metadataNode == null)
            return null;
        // It is ok to use selectNodes here because we are within the worksheet xml only
        metadata.initializeWorksheetTitle(xmlDoc.selectNodes("//Worksheet/Title/Value"));
        metadata.worksheetEvents = metadata.getWorksheetEvents(xmlDoc, xmlDoc.selectNodes("//Worksheet/Events"));
        metadata.pageDefName=getTextContentOfSelectList(xmlDoc.selectNodes("//Worksheet/PageDef/PageDef"));
        metadata.menuItems = metadata.getMenuItems(xmlDoc.selectNodes("//Worksheet/MenuItems/Label/Value"));
        return metadata;
    }

    public ArrayList<WorksheetEvent> getWorksheetEvents(XMLDocument xmlDoc, NodeList eventsNode) throws Exception {
        ArrayList<WorksheetEvent> events = new ArrayList<WorksheetEvent>();
        if (eventsNode == null || eventsNode.getLength() < 1)
            return events;
        int eventsLen = eventsNode.getLength();
        for (int i = 0; i < eventsLen; i++) {

            Node event = eventsNode.item(i);
            if (!XMLParserHelper.isNodeName(event, "Events"))
                continue;
            String eventType = XMLParserHelper.getAttributeValue(event, "Event");
            String invokeOnce = XMLParserHelper.getAttributeValue(event, "InvokeOnlyOnce");
            Node actionSetNode = XMLParserHelper.getChildNodeWithName(event, "ActionSet");
            ActionSet aset = null;
            if (actionSetNode != null) {
                aset = ActionSet.getMetadata(xmlDoc, (Element)actionSetNode);

            }
            WorksheetEvent wrksheetEvent = new WorksheetEvent(eventType, aset);
            wrksheetEvent.setInvokeOnce(invokeOnce);
            events.add(wrksheetEvent);

        }
        return events;
    }
    
    public ArrayList<String> getMenuItems(NodeList menuItems) throws Exception {
        
        ArrayList<String> mi = new ArrayList<String>();
        if (menuItems == null || menuItems.getLength() < 1)
            return mi;
        
        for(int i=0; i < menuItems.getLength(); i++){
            Node item = menuItems.item(i);
            mi.add(item.getTextContent());            
        }
        
        return mi;
    }

    /**
     * Checks:
     * Worksheet.Title
     * WorksheetEvent.item.Action.item
     * @return
     */
    @Override
    public ArrayList<CheckerResult> runLocalizationCheck() {
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        String issue=CheckerConstants.ISSUE_RESOURCES;
        String subIssue="Strings:Worksheet";
        if (!isLocalizedString(worksheetTitle, false))
            results.add(new CheckerResult("R", issue,subIssue,String.format("Worksheet Title: %s must be localized", worksheetTitle), false));
        if (worksheetEvents != null) {
            Iterator<WorksheetEvent> eventIter = worksheetEvents.iterator();
            while (eventIter.hasNext()) {
                WorksheetEvent e = eventIter.next();
                if (e.eventActions != null)
                    addResults(results, e.eventActions.runLocalizationCheck("Strings:Worksheet.Action"));
            }
        }
        return results;
    }

    public void initializeWorksheetTitle(NodeList node) {
        if (node == null || node.getLength() < 1)
            return;
        Node n = node.item(0);
        if (n == null)
            return;
        worksheetTitle = n.getTextContent();
        if (worksheetTitle == null)
            worksheetTitle = "";
        else
            worksheetTitle = worksheetTitle.trim();
    }
    
    
}
