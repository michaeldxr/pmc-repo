package oracle.apps.votuning_v2;

import java.io.File;

import java.io.Serializable;

import java.util.HashMap;

import java.util.Iterator;
import java.util.Set;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DataBindings implements Serializable {
    /**
     * full path of the cpx file
     */
    String cpxFullPath = "";
    /**
     * Map of the pages and their corresponding page defs
     */
    HashMap<String, String> pagesPageDef = new HashMap<String, String>();
    /**
     * Map of the data control usages indexed by the usageId
     */
    HashMap<String, String> dataControlUsages = new HashMap<String, String>();

    /**
     * wheter or not to sop all the messages
     */
    static boolean bDebug=false;

    public DataBindings() {
        super();
    }

    /**
     * Creates a databindings object from the cpxFile passed in
     * 
     * @param cpxFile
     * @return
     * returns null if:
     * 1. Zero or More than one 'Application' nodes -  indicates an invalid format
     * 2. Zero or More than one 'pageMap' nodes -  no pageMaps
     * 3. Zero or More than one 'pageDefinitionUsages' nodes -  no page definition usages
     */
    public static DataBindings createBindingsFile(File cpxFile) {
        DataBindings bindings = new DataBindings();
        bindings.setCpxFullPath(cpxFile.getAbsolutePath());
        String absPath = cpxFile.getAbsolutePath();
        XMLDocument doc = XMLParserHelper.getXMLDocument(absPath);
        
        if(doc == null)
            return null;
        NodeList applicationNodes = doc.getElementsByTagName("Application");
        if (!XMLParserHelper.hasOneNOnlyOneNode(applicationNodes)) {
            System.out.println("Error while parsing databindings file.. Expecting one and only one Application Node.." +
                               absPath);
            return null;
        }


        NodeList pageMapNodes = doc.getElementsByTagName("pageMap");
        if (!XMLParserHelper.hasOneNOnlyOneNode(pageMapNodes)) {
            System.out.println("Could not find any pageMaps or more than one pageMap tag in this databindings.cpx file" +
                               absPath);
            return null;
        }

        NodeList pageDefUsagesList =
            doc.getElementsByTagName("pageDefinitionUsages");
        if (!XMLParserHelper.hasOneNOnlyOneNode(pageDefUsagesList)) {
            System.out.println("Could not find any pageDefinitionUsages or more than one pageDefinitionUsages tag in this databindings.cpx file" +
                               absPath);
            return null;
        }


        Node pagemapNode = pageMapNodes.item(0);
        Node pgdefUsagesNodes = pageDefUsagesList.item(0);
        NodeList pageMapChildren = pagemapNode.getChildNodes();
        if (pageMapChildren == null) {
            if(bDebug)
            {
                System.out.println("Could not find any pageMap children this databindings.cpx file" +
                               absPath);
            }
            return null;
        }
        int pmapLen = pageMapChildren.getLength();
        for (int i = 0; i < pmapLen; i++) {
            Node pageNode = pageMapChildren.item(i);

            String pagePath = XMLParserHelper.getAttributeValue(pageNode, "path");
            String usageId = XMLParserHelper.getAttributeValue(pageNode, "usageId");
            if (pagePath == null || usageId == null) {
                continue;
            }
            Node pageDefUsage = XMLParserHelper.getChildNodeWithMatchingAttribute(pgdefUsagesNodes,
                                                                  "id",
                                                                  usageId);
            if (pageDefUsage == null) {
                String msg = String.format("DataBindings file name: %s, could not find any pageDefinitionUsages with usageId: %s", absPath,usageId);
                System.out.println(msg);
                continue;
            }
            String pageDefPath = XMLParserHelper.getAttributeValue(pageDefUsage, "path");
            if (!isEmpty(pageDefPath) && !isEmpty(pagePath)) {
                bindings.pagesPageDef.put(pagePath, pageDefPath);
            } else {
                if(bDebug)
                {
                    System.out.println("[DataBindings] Not adding to map since page path or pagedef path is empty");
                }
            }
        }
        NodeList datacontrolNodes =
            doc.getElementsByTagName("dataControlUsages");
        if (!XMLParserHelper.hasOneNOnlyOneNode(datacontrolNodes)) {
            if(bDebug)
            {
                System.out.println("Could not find any dataControlUsages tag in databindings file: " +
                               absPath);
            }
            return bindings;
        }
        Node dcUsagesNode = datacontrolNodes.item(0);
        NodeList dcChildren = dcUsagesNode.getChildNodes();
        int dcChildLen = dcChildren.getLength();

        for (int j = 0; j < dcChildLen; j++) {
            Node bc4jControl = dcChildren.item(j);
            String nodeName = bc4jControl.getNodeName();
            if (nodeName != null &&
                nodeName.trim().equals("BC4JDataControl")) {
                String dcUsageId = XMLParserHelper.getAttributeValue(bc4jControl, "id");
                String dcUsagePackage = XMLParserHelper.getAttributeValue(bc4jControl, "Package");
                if (!isEmpty(dcUsageId) && !isEmpty(dcUsagePackage)) {
                    bindings.dataControlUsages.put(dcUsageId, dcUsagePackage);
                } else {
                    System.out.println("[DataBindings] Not adding to map since id or package string is empty");
                }
            }
        }
        return bindings;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public void printBindings() {
        System.out.println("Printing description of bindings file: " +
                           getCpxFullPath());
        Set<String> pageKeys = pagesPageDef.keySet();
        Iterator<String> pageIter = pageKeys.iterator();
        System.out.println("Printing pages and pagedef mappings");
        while (pageIter.hasNext()) {
            String strPage = pageIter.next();
            String strPageDef = pagesPageDef.get(strPage);
            System.out.println("---------------------------------------------");
            System.out.println("page: " + strPage);
            System.out.println("pageDef: " + strPageDef);
            System.out.println("---------------------------------------------");
        }

        Set<String> dcKeys = dataControlUsages.keySet();
        Iterator<String> dcIter = dcKeys.iterator();
        System.out.println("Printing DataControl Usages");
        while (dcIter.hasNext()) {
            String strDcId = dcIter.next();
            String strDcPkg = dataControlUsages.get(strDcId);
            System.out.println("---------------------------------------------");
            System.out.println("page: " + strDcId);
            System.out.println("pageDef: " + strDcPkg);
            System.out.println("---------------------------------------------");
        }
    }

    public String getDataControlPackage(String datacontrolId) {
        return dataControlUsages.get(datacontrolId);
    }

    public void setCpxFullPath(String cpxFullPath) {
        this.cpxFullPath = cpxFullPath;
    }

    public String getCpxFullPath() {
        return cpxFullPath;
    }

    public HashMap<String, String> getPagesPageDef() {
        return pagesPageDef;
    }

    public HashMap<String, String> getDataControlUsages() {
        return dataControlUsages;
    }
}

