package oracle.apps.di.metadata;

import java.util.ArrayList;

import java.util.Iterator;

import oracle.apps.di.CheckerConstants;
import oracle.apps.di.checker.CheckerResult;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ActionSet extends ADFDiMetadata {
    String statusTitle = "";
    String statusMsg = "";
    Alert alert = null;
    ArrayList<Action> actions = null;

    public ArrayList<ActionSet.Action> getActions() {
        return actions;
    }

    public class Action {
        String methodType = "";
        String method = "";
        String componentId = "";
        String dialogPage = "";

        public Action(String type, String m) {
            this.methodType = type;
            this.method = m;
        }


        public void setComponentId(String componentId) {
            this.componentId = componentId;
        }

        public String getComponentId() {
            return componentId;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getMethod() {
            return method;
        }

        public void setMethodType(String methodType) {
            this.methodType = methodType;
        }

        public String getMethodType() {
            return methodType;
        }

        public void setDialogPage(String dialogPage) {
            this.dialogPage = dialogPage;
        }

        public String getDialogPage() {
            return dialogPage;
        }
    }

    public class Alert {
        String title = "";
        String successMessage = "";
        String okButtonLabel = "";
        String failureMessage = "";
        boolean enabled;

        public Alert() {
        }

    }

    public Alert getAlert(XMLDocument doc, Node actionSetNode) throws Exception {
        Alert alert = new Alert();
        
        alert.title = ADFDiMetadata.getTextContentOfXPathNode(actionSetNode, "Alert/Title/Value");
        alert.successMessage = ADFDiMetadata.getTextContentOfXPathNode(actionSetNode, "Alert/SuccessMessage/Value");
        alert.okButtonLabel = ADFDiMetadata.getTextContentOfXPathNode(actionSetNode, "Alert/OKButtonLabel/Value");
        alert.failureMessage = ADFDiMetadata.getTextContentOfXPathNode(actionSetNode, "Alert/FailureMessage/Value");
      

        return alert;
    }

    private ArrayList<Action> getActions(XMLDocument doc, ArrayList<Node> actionNodes) {
        ArrayList<Action> acts = new ArrayList<Action>();
        if (actionNodes == null || actionNodes.size() < 1)
            return acts;
        Iterator<Node> actionNodeIter = actionNodes.iterator();
        while (actionNodeIter.hasNext()) {
            Node a = actionNodeIter.next();

            if (a == null || !a.getNodeName().equals("Actions"))
                continue;
            String mtype = getAttributeOfType(a, "type");


            String method = XMLParserHelper.getAttributeValue(a, "Method");
            String compId = XMLParserHelper.getAttributeValue(a, "ComponentID");
            Action act = new Action(mtype, method);
            if (mtype != null && mtype.equals("ADFmAction")) {
                Node adfmAction = XMLParserHelper.getChildNodeWithName(a, "ActionID");
                if (adfmAction != null) {
                    method = XMLParserHelper.getAttributeValue(adfmAction, "ID");
                }
            } else if (mtype != null && mtype.equals("Dialog")) {
                Node pageNode = XMLParserHelper.getChildNodeWithName(a, "Page");
                if (pageNode != null) {

                    Node pageNode2 = XMLParserHelper.getChildNodeWithName(pageNode, "Page");
                    if (pageNode2 != null)
                        act.dialogPage = pageNode2.getTextContent();
                    if (act.dialogPage != null)
                        act.dialogPage = act.dialogPage.trim();
                }
            }

            if (compId != null && !compId.trim().equals(""))
                act.setComponentId(compId);
            act.method=method;
            acts.add(act);
        }


        return acts;
    }
//Overloaded getActions() method for TableChecker.T2_StatusCol
    private ArrayList<Action> getActions(ArrayList<Node> actionNodes) {
        ArrayList<Action> acts = new ArrayList<Action>();
        if (actionNodes == null || actionNodes.size() < 1)
            return acts;
        Iterator<Node> actionNodeIter = actionNodes.iterator();
        while (actionNodeIter.hasNext()) {
            Node a = actionNodeIter.next();

            if (a == null || !a.getNodeName().equals("Actions"))
                continue;
            String mtype = getAttributeOfType(a, "type");
            String method = XMLParserHelper.getAttributeValue(a, "Method");
            String compId =
                XMLParserHelper.getAttributeValue(a, "ComponentID");
            Action act = new Action(mtype, method);
            if (mtype != null && mtype.equals("ADFmAction")) {
                Node adfmAction =
                    XMLParserHelper.getChildNodeWithName(a, "ActionID");
                if (adfmAction != null) {
                    method =
                            XMLParserHelper.getAttributeValue(adfmAction, "ID");
                }
            } else if (mtype != null && mtype.equals("Dialog")) {
                Node pageNode =
                    XMLParserHelper.getChildNodeWithName(a, "Page");
                if (pageNode != null) {

                    Node pageNode2 =
                        XMLParserHelper.getChildNodeWithName(pageNode, "Page");
                    if (pageNode2 != null)
                        act.dialogPage = pageNode2.getTextContent();
                    if (act.dialogPage != null)
                        act.dialogPage = act.dialogPage.trim();
                }
            }

            if (compId != null && !compId.trim().equals(""))
                act.setComponentId(compId);
            act.method = method;
            acts.add(act);
        }


        return acts;
    }
    public static ActionSet getMetadata(XMLDocument xmlDoc, Element metadataNode) throws Exception {
        ActionSet actionSet = new ActionSet();
        actionSet.alert = actionSet.getAlert(xmlDoc, metadataNode);


        actionSet.statusTitle = ADFDiMetadata.getTextContentOfXPathNode(metadataNode, "Status/Title/Value");
        actionSet.statusMsg = ADFDiMetadata.getTextContentOfXPathNode(metadataNode, "Status/Message/Value");
        //actionSet.actions = actionSet.getActions(xmlDoc, xmlDoc.selectNodes("//Actions"));
        actionSet.actions = actionSet.getActions(xmlDoc, XMLParserHelper.getChildNodesWithName(metadataNode, "Actions"));
        return actionSet;
    }
    
    //Overloaded getMetadata() method for TableChecker.T2_StatusCol
    public static ActionSet getMetadata(Element metadataNode) throws Exception {
        if (metadataNode == null)
            return null;

        ActionSet actionSet = new ActionSet();
        String nodeName = metadataNode.getNodeName();
        ADFDiComponentMetadata compMetadata =
            new ADFDiComponentMetadata(nodeName);

        compMetadata.initializePositionAndId(metadataNode);

         Node mainCompNode =
            compMetadata.initializeMainComponentNode(metadataNode);
         ArrayList<Node> actionSetNodes =
            XMLParserHelper.getChildNodesWithName(mainCompNode,
                                                  "DoubleClickActionSet");
        if (actionSetNodes != null) {
            Iterator<Node> actionSetNodeIter = actionSetNodes.iterator();
            while (actionSetNodeIter.hasNext()) {
                Node actionNode = actionSetNodeIter.next();
                actionSet.actions =
                        actionSet.getActions(XMLParserHelper.getChildNodesWithName(actionNode,
                                                                                   "Actions"));

            }
        }

        return actionSet;
    }
    public ActionSet() {
        super(META_ACTIONSET);
    }

    public ArrayList<CheckerResult> runLocalizationCheck() {
        return runLocalizationCheck("Strings:ActionSet");
    }
    public ArrayList<CheckerResult> runLocalizationCheck(String subIssue) {
        ArrayList<CheckerResult> checkerResults = new ArrayList<CheckerResult>();
        checkString(statusTitle, subIssue,String.format("ActionSet StatusTitle :(%s) should be localized ", statusTitle), checkerResults);
        checkString(statusMsg, subIssue,String.format("ActionSet statusMessage :(%s) should be localized ", statusMsg), checkerResults);


        if (alert != null) {
            checkString(alert.failureMessage, subIssue,String.format("ActionSet failureMessage : (%s) should be localized ", alert.failureMessage),
                        checkerResults);
            checkString(alert.successMessage, subIssue,String.format("ActionSet successMessage : (%s) should be localized ", alert.successMessage),
                        checkerResults);
            checkString(alert.okButtonLabel, subIssue,String.format("ActionSet okButtonLabel : (%s) should be localized ", alert.okButtonLabel),
                        checkerResults);

        }
        return checkerResults;
    }

    public void checkString(String strToCheck, String subIssue,String desc, ArrayList<CheckerResult> results) {
        if (!isLocalizedString(strToCheck, false)) {
            results.add(new CheckerResult("R", CheckerConstants.ISSUE_RESOURCES,subIssue, desc, false));
        }
    }
}
