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

public class TableMetadata extends ADFDiComponentMetadata {

    String rowLimitWarning = "";
    String tableUniqueAttr = "";
    // Have to set the defaults, because if not changed the values will not be present in xml
    String insertRowEnabled = "false";
    String updateRowEnabled = "true";
    String deleteRowEnabled = "false";
    String commitBatchEnabled="false";
    String limitBatchSize="true";
    String insertRowsAfterUploadEnabled = "";
    String insertBeforeRowAction = "true";
    String insertAfterRowAction = "";
    String batchsize="";
    ArrayList<Column> columns;
    ArrayList<String> cachedAttrs = new ArrayList<String>();

    public TableMetadata() {
        super(META_TABLE);
    }

    public String getRowLimitWarning() {
        return rowLimitWarning;
    }
    public String getcommitBatchEnabled() {
        return commitBatchEnabled;
    }
    public String getlimitBatchSize() {
        return limitBatchSize;
    }
    public String getTableUniqueAttr() {
        return tableUniqueAttr;
    }

    public String getInsertRowEnabled() {
        return insertRowEnabled;
    }

    public String getInsertRowsAfterUploadEnabled() {
        return insertRowsAfterUploadEnabled;
    }

    public String getInsertBeforeRowAction() {
        return insertBeforeRowAction;
    }

    public String getInsertAfterRowAction() {
        return insertAfterRowAction;
    }

    public void setColumns(ArrayList<TableMetadata.Column> columns) {
        this.columns = columns;
    }

    public String getUpdateRowEnabled() {
        return updateRowEnabled;
    }

    public String getDeleteRowEnabled() {
        return deleteRowEnabled;
    }

    public ArrayList<String> getCachedAttrs() {
        return cachedAttrs;
    }

    public String getBatchsize() {
        return batchsize;
    }

    public class Column {
        String id = "";
        String headerStyle = "";
        String headerLabel = "";
        String cellStyle = "";
        String dynamicColumn = "false";
        String visible = "";
        ADFDiComponentMetadata updateComponent;
        ADFDiComponentMetadata insertComponent;
        Node unode; 
        Node inode;
        String insertUsesUpdate = "False";

        public Column() {
        }

        public void runLocalizationCheck(ArrayList<CheckerResult> results) {
            String subIssue = "Strings:Table";
            if (!isLocalizedString(headerLabel, false)) {
                String desc = String.format("Column: %s Header Label should not contain hardcoded text : %s", id, headerLabel);
                results.add(new CheckerResult("R", CheckerConstants.ISSUE_RESOURCES,subIssue, desc, false));
            }
            if (updateComponent != null) {
                addResults(results, updateComponent.runLocalizationCheck("Strings:Table.Column"));
            }
            if (insertComponent != null) {
                addResults(results, insertComponent.runLocalizationCheck("Strings:Table.Column"));
            }
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getHeaderStyle() {
            return headerStyle;
        }

        public String getHeaderLabel() {
            return headerLabel;
        }

        public String getCellStyle() {
            return cellStyle;
        }

        public String getDynamicColumn() {
            return dynamicColumn;
        }

        public String getVisible() {
            return visible;
        }

        public ADFDiComponentMetadata getUpdateComponent() {
            return updateComponent;
        }

        public ADFDiComponentMetadata getInsertComponent() {
            return insertComponent;
        }


        public Node getUnode() {
            return unode;
        }

        public Node getInode() {
            return inode;
        }

        public String getInsertUsesUpdate() {
            return insertUsesUpdate;
        }
    }

    public ArrayList<Column> getListOfColumns(XMLDocument xmlDoc, ArrayList<Node> columnNodes) throws Exception {
        
        if (columnNodes == null || columnNodes.size() < 1)
            return null;
        ArrayList<Column> columns = new ArrayList<Column>();
        Iterator<Node> nodeIter = columnNodes.iterator();
        while (nodeIter.hasNext()) {

            Column column = new Column();
            Node c = nodeIter.next();
            if (!c.getNodeName().equals("Columns"))
                continue;
            column.id = XMLParserHelper.getAttributeValue(c, "ID");
           
            column.headerStyle =
                    getTextContentOfSelectList(xmlDoc.selectNodes(String.format("//Columns[@ID='%s']/HeaderStyleName/Value", column.id)));
            column.headerLabel = getTextContentOfSelectList(xmlDoc.selectNodes(String.format("//Columns[@ID='%s']/HeaderLabel/Value", column.id)));
            column.cellStyle = getTextContentOfSelectList(xmlDoc.selectNodes(String.format("//Columns[@ID='%s']/CellStyleName/Value", column.id)));
            column.visible = getTextContentOfSelectList(xmlDoc.selectNodes(String.format("//Columns[@ID='%s']/Visible/Value", column.id)));
            column.insertUsesUpdate = XMLParserHelper.getAttributeValue(c, "InsertUsesUpdate");
            Node updateComponentNode = XMLParserHelper.getChildNodeWithName(c, "UpdateComponent");
            column.unode=updateComponentNode;
            if (updateComponentNode != null) {
               
                column.updateComponent = ADFDiComponentMetadata.getMetadata(xmlDoc, (Element)updateComponentNode);
                column.updateComponent.componentType = getAttributeOfType(updateComponentNode, "type");
                if(column.updateComponent.componentType!=null && column.updateComponent.componentType.equals("TreeNodeList"))
                {
                    column.updateComponent.setValue(getTextContentOfXPathNode(updateComponentNode, "List/Value"));
                    }
            }
            Node insertComponentNode = XMLParserHelper.getChildNodeWithName(c, "InsertComponent");
            column.inode=insertComponentNode;
            if (insertComponentNode != null) {
               
                column.insertComponent = ADFDiComponentMetadata.getMetadata(xmlDoc, (Element)insertComponentNode);
                column.insertComponent.componentType = getAttributeOfType(insertComponentNode, "type");
                if(column.insertComponent.componentType!=null && column.insertComponent.componentType.equals("TreeNodeList"))
                {
                    column.insertComponent.setValue(getTextContentOfXPathNode(insertComponentNode, "List/Value"));
                    }
            }
            columns.add(column);
        }
        return columns;
    }

    public static TableMetadata getMetadata(XMLDocument xmlDoc, Element metadataNode) throws Exception {

        if (xmlDoc == null || metadataNode == null)
            return null;

        TableMetadata metadata = new TableMetadata();
        ADFDiComponentMetadata tableComp = ADFDiComponentMetadata.getMetadata(xmlDoc, metadataNode);
        metadata.componentId = tableComp.componentId;
        metadata.componentType = metadataNode.getNodeName();
        metadata.positionRow = tableComp.positionRow;
        metadata.positionCol = tableComp.positionCol;

        metadata.rowLimitWarning = ADFDiMetadata.getTextContentOfXPathNode(metadataNode, "RowLimit/WarningMessage/Value");
        metadata.tableUniqueAttr = ADFDiMetadata.getTextContentOfXPathNode(metadataNode, "UniqueAttribute/Value");
        metadata.columns = metadata.getListOfColumns(xmlDoc, XMLParserHelper.getChildNodesWithName(metadataNode, "Columns"));
        metadata.setRowActions((Element)metadataNode);
        metadata.setCachedAttrs((Element)metadataNode);
        metadata.setBatchOptions((Element)metadataNode);
        
        return metadata;
    }
    public void setBatchOptions(Element tableMetadataNode) {
        Node BatchOptionsNode = XMLParserHelper.getChildNodeWithName(tableMetadataNode, "BatchOptions");
        Node CommitBatchActionNode  = null;
        String commitAction=null;
        String limitBatchSizeValue=null;
        String batchsizeValue=null;
        if (BatchOptionsNode !=  null){
             CommitBatchActionNode = XMLParserHelper.getChildNodeWithName(BatchOptionsNode, "CommitBatchActionID"); 
        }
        if (null != CommitBatchActionNode)
            commitAction = XMLParserHelper.getAttributeValue(CommitBatchActionNode, "ID");
        if (commitAction == null)
            commitBatchEnabled="false";
        else
             commitBatchEnabled = "true";
        if (BatchOptionsNode !=  null){
             limitBatchSizeValue = XMLParserHelper.getAttributeValue(BatchOptionsNode, "LimitBatchSize");
             batchsizeValue = XMLParserHelper.getAttributeValue(BatchOptionsNode, "BatchSize");
            
             
        }
        if (null != limitBatchSizeValue)
            limitBatchSize = limitBatchSizeValue;
        if (null != batchsizeValue)      
            batchsize = batchsizeValue;
        else
            batchsize="100";
        

    }
    public void setRowActions(Element tableMetadataNode) {
        Node rowActionsNode = XMLParserHelper.getChildNodeWithName(tableMetadataNode, "RowActions");
        if (rowActionsNode == null)
            return;
        String insEnbld = XMLParserHelper.getAttributeValue(rowActionsNode, "InsertRowEnabled");
        if (insEnbld != null)
            insertRowEnabled = insEnbld.trim();

        String updEnbld = XMLParserHelper.getAttributeValue(rowActionsNode, "UpdateRowEnabled");
        if (updEnbld != null)
            updateRowEnabled = updEnbld.trim();

        String dltEnbld = XMLParserHelper.getAttributeValue(rowActionsNode, "DeleteRowEnabled");
        if (dltEnbld != null)
            deleteRowEnabled = dltEnbld.trim();
        Node insertBeforeRowActionsNode =  XMLParserHelper.getChildNodeWithName(rowActionsNode, "InsertBeforeRowActionID");
        String insertBeforeRowActionEnbld = null;
         insertBeforeRowActionEnbld = XMLParserHelper.getAttributeValue(insertBeforeRowActionsNode, "ID");
       if (null != insertBeforeRowActionEnbld &&insertBeforeRowActionEnbld.length()>1 ){
         insertBeforeRowAction = "true";
       }
       else{
            insertBeforeRowAction = "false";
       }
    
    }

    public void setCachedAttrs(Element tableMetadataNode) {
        Node rowDataNode = XMLParserHelper.getChildNodeWithName(tableMetadataNode, "RowData");
        if (rowDataNode == null)
            return;
        ArrayList<Node> cachedAttrsNodes = XMLParserHelper.getChildNodesWithName(rowDataNode, "CachedAttributes");
        if (cachedAttrsNodes == null || cachedAttrsNodes.size() < 1)
            return;
        Iterator<Node> cachedAttrIter = cachedAttrsNodes.iterator();
        while (cachedAttrIter.hasNext()) {
            Node cachedAttr = cachedAttrIter.next();
            String cachedAttrVal = getTextContentOfXPathNode(cachedAttr, "Value/Value");
            if (!isEmpty(cachedAttrVal))
                cachedAttrs.add(cachedAttrVal);
        }
    }

    @Override
    public ArrayList<CheckerResult> runLocalizationCheck() {
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        checkString(rowLimitWarning, "Strings:Table",
                    String.format("Table : %s  RowLimitWarning (%s) should be localized", componentId, rowLimitWarning), results);
        if (columns != null) {
            Iterator<Column> colIter = columns.iterator();
            while (colIter.hasNext()) {
                Column c = colIter.next();
                c.runLocalizationCheck(results);
            }
        }
        return results;
    }

    public ArrayList<Column> getColumns() {
        return columns;
    }
}
