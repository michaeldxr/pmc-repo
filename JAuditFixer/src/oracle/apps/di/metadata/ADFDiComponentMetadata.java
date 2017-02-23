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

public class ADFDiComponentMetadata extends ADFDiMetadata {
    String componentId = "";
    String styleName = "";
    String componentType = "";
    ActionSet dblclickActionSet = null;
    String readOnly = "false";
    String value = "";
    String positionRow = "";
    String positionCol = "";

    // Only applicable for ListOfValues, but just putting it in this class instead of creating a seperate listOfValues class
    String listId = "";
    String dependsOnListId = "";

    //private String clickActionName = "DoubleClickActionSet";
    //String mainCompNode = "Value";

    public ADFDiComponentMetadata(String type) {
        super(type);
        componentType = type;
    }

    public static ADFDiComponentMetadata getMetadata(XMLDocument xmlDoc, Element metadataNode) throws Exception {
        //        ADFDiComponentMetadata metadata = new ADFDiComponentMetadata(META_WORKBOOK);
        //
        if (xmlDoc == null || metadataNode == null)
            return null;


        //String mtype = ADFDiComponentMetadata.getAttributeOfType(metadataNode, "type");
        String nodeName = metadataNode.getNodeName();
        ADFDiComponentMetadata compMetadata = new ADFDiComponentMetadata(nodeName);
        
        compMetadata.initializePositionAndId(metadataNode);
        
        // Initialize componentType, and the main node where the value is. Also initializes the clickAction name
        Node mainCompNode = compMetadata.initializeMainComponentNode(metadataNode);
        //The below is needed, because in the casel of label, input and output text and generic components
        // the double click action set, value are under <InputText> or <OutputText>
        // In the case of Table.UdpateComponent it is under the same node. Table.updateComponent is different in that it has an attribute of 'type'
        //        if (isEmpty(mtype) && !isEmpty(nodeName)) {
        //            if (nodeName.equals("CellLabel"))
        //                mtype = "Label";
        //            else if (nodeName.equals("CellInputText"))
        //                mtype = "InputText";
        //            else if (nodeName.equals("CellOutputText"))
        //                mtype = "OutputText";
        //            else if (nodeName.equals("CellListOfValues"))
        //                mtype = "ListOfValues";
        //            if (!isEmpty(mtype)) {
        //
        //                Node t = XMLParserHelper.getChildNodeWithName(metadataNode, mtype);
        //                if (t != null)
        //                    mainCompNode = t;
        //            }
        //        }
        //        compMetadata.componentType = mtype;

        // Cannot use xpaths on the whole doc here, have to get specific to that metadata node.
       
        String style = getTextContentOfXPathNode(metadataNode, "StyleName/Value");
        if (style != null)
            compMetadata.styleName = style.trim();

        // Note - we select DoubleClickActionSet on the mainCompNode that we determined earlier
        ArrayList<Node> actionSetNodes = XMLParserHelper.getChildNodesWithName(mainCompNode, "DoubleClickActionSet");
        if (actionSetNodes != null) {
            Iterator<Node> actionSetNodeIter = actionSetNodes.iterator();
            while (actionSetNodeIter.hasNext()) {
                Node actionNode = actionSetNodeIter.next();
                ActionSet act = ActionSet.getMetadata(xmlDoc, (Element)actionNode);
                if (act != null)
                    compMetadata.dblclickActionSet = act;
            }
        }
        // In table-updateComponent value is under /Value/Value. But if it is just CellLabel, then value is under Value
        String val = getTextContentOfXPathNode(metadataNode, "Value/Value");
        //String val = getTextContentOfSelectList(xmlDoc.selectNodes("/Value/Value"));
        if (isEmpty(val)) {
            val = getTextContentOfXPathNode(mainCompNode, "Value");
        }
        if (!isEmpty(val))
            compMetadata.value = val;

        String ro = getTextContentOfXPathNode(mainCompNode, "ReadOnly/Value");
        if (ro != null)
            compMetadata.readOnly = ro.trim();

        if (compMetadata.componentType != null && compMetadata.componentType.equals("TreeNodeList")) {
            compMetadata.initializeLOVVals(mainCompNode);
        }

        return compMetadata;
    }

    public Node initializeMainComponentNode(Node rootNode) {
        Node mainCompNode = rootNode;
        String mtype = ADFDiComponentMetadata.getAttributeOfType(rootNode, "type");
        String nodeName = rootNode.getNodeName();
        // If the rootComponent has a type attribute, means the component is coming from table. In such a case there wont be a child InputText node etc
        // Only if there is no type attribute, means the comp is <CellLabel><Label> etc ..
        //String strCompType="";
        //  String tagRootWhereValueIs="";
        if (isEmpty(mtype) && !isEmpty(nodeName)) {
            if (nodeName.equals("CellLabel"))
                mtype = "Label";
            else if (nodeName.equals("CellInputText"))
                mtype = "InputText";
            else if (nodeName.equals("CellOutputText"))
                mtype = "OutputText";
            else if (nodeName.equals("CellListOfValues"))
                mtype = "ListOfValues";
            //            else if (nodeName.equals("Button"))
            //            {
            //                mtype = "Button";
            //                tagRootWhereValueIs="Label";
            //                clickActionName="ClickActionSet";
            //            }
            //            if(isEmpty(tagRootWhereValueIs))
            //                tagRootWhereValueIs=mtype;

            if (!isEmpty(mtype)) {
                Node t = XMLParserHelper.getChildNodeWithName(rootNode, mtype);
                if (t != null)
                    mainCompNode = t;
            }

        }
        componentType = mtype;
        return mainCompNode;

    }

    public void initializeLOVVals(Node lovNode) {
        if (lovNode == null)
            return;
        Node listIdNode = XMLParserHelper.getChildNodeWithName(lovNode, "List");
        Node valueNode = XMLParserHelper.getChildNodeWithName(listIdNode, "Value");
       
        if (valueNode != null) {
            
            listId = valueNode.getTextContent();
        }
        Node dependsOnlistIdNode = XMLParserHelper.getChildNodeWithName(lovNode, "DependsOnList");
        valueNode = XMLParserHelper.getChildNodeWithName(dependsOnlistIdNode, "Value");
        if (valueNode != null) {
             dependsOnListId =  valueNode.getTextContent();
          }
    }

    public void initializePositionAndId(Node metadataNode) {

        Node positionNode=XMLParserHelper.getChildNodeWithName(metadataNode, "Position");
        String id = XMLParserHelper.getAttributeValue(metadataNode, "ComponentID");
        if (!isEmpty(id))
            componentId = id;
        
        positionRow = XMLParserHelper.getAttributeValue(positionNode, "Row");
        positionCol = XMLParserHelper.getAttributeValue(positionNode, "Column");

    }

    @Override
    public ArrayList<CheckerResult> runLocalizationCheck() {
        String msg = "Strings:"+componentType;
        return runLocalizationCheck(msg);
    }
    public ArrayList<CheckerResult> runLocalizationCheck(String errorMsg) {
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();

        checkString(value, errorMsg,String.format("Component ID=%s of type:%s . Value (%s) should be localized ", componentId, componentType, value),
                    results);

        if (dblclickActionSet != null) {
            ArrayList<CheckerResult> actionSetResults = dblclickActionSet.runLocalizationCheck(errorMsg+".Action");
            addResults(results, actionSetResults);
        }
        return results;
    }
    public void checkString(String strToCheck, String subIssue,String desc, ArrayList<CheckerResult> results) {
        if (!isLocalizedString(strToCheck, false)) {
            results.add(new CheckerResult("R",CheckerConstants.ISSUE_RESOURCES, subIssue, desc, false));
        }
    }

    public String getComponentId() {
        return componentId;
    }

    public String getStyleName() {
        return styleName;
    }

    public String getComponentType() {
        return componentType;
    }

    public ActionSet getDblclickActionSet() {
        return dblclickActionSet;
    }

    public String getReadOnly() {
        return readOnly;
    }

    public String getValue() {
        return value;
    }

    public String getPositionRow() {
        return positionRow;
    }

    public String getPositionCol() {
        return positionCol;
    }

    public String getListId() {
        return listId;
    }

    public String getDependsOnListId() {
        return dependsOnListId;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
