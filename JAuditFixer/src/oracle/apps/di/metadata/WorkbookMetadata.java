package oracle.apps.di.metadata;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.Iterator;
import java.util.Map;

import java.util.Set;

import oracle.apps.di.CheckerConstants;
import oracle.apps.di.checker.CheckerResult;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class WorkbookMetadata extends ADFDiMetadata {
    HashMap<String, String> resources = new HashMap<String, String>();
    HashMap<String, String> brandingItems = new HashMap<String, String>();
    // Workbook commands : Key = methodName, Value = Label Value
    HashMap<String, String> workbookCommands = new HashMap<String, String>();
    ArrayList<String> menuItems = null;

    String workbookId = "";
    String runtimeRibbonTitle = "";
    String applicationHomeFolder = "";
    String webPagesFolder = "";
    String projectFolder = "";
    //For ADFdi customization
    boolean customizationEnabled;
    
    String autoDisplayStatusViewerEnabled=""; 
    String autoDisplayStatusViewerMethod="";

    public WorkbookMetadata(String type) {
        super(type);
    }

    public static WorkbookMetadata getMetadata(String xmlMetadata) throws Exception {

        return null;
    }

    public static WorkbookMetadata getMetadata(XMLDocument xmlDoc, Element metadataNode) throws Exception {
        WorkbookMetadata metadata = new WorkbookMetadata(META_WORKBOOK);

        if (xmlDoc == null || metadataNode == null)
            return null;
        metadata.initializeResources(xmlDoc.selectNodes("//Resources"));
        metadata.initializeBrandingItems(xmlDoc.selectNodes("//BrandingItems"));
        metadata.workbookId = metadata.getTextContentOfSelectList(xmlDoc.selectNodes("//WorkbookID"));
        metadata.autoDisplayStatusViewerEnabled=metadata.getTextContentOfSelectList(xmlDoc.selectNodes("//AutoDisplayStatusViewerEnabled"));
        //For ADFdi customization
        metadata.customizationEnabled = Boolean.valueOf(metadata.getTextContentOfSelectList(xmlDoc.selectNodes("//CustomizationEnabled")));
        metadata.runtimeRibbonTitle = metadata.getTextContentOfSelectList(xmlDoc.selectNodes("//ToolbarMenu/Title/Value"));
        metadata.initializeWorkbookMenuItems(xmlDoc.selectNodes("//ToolbarMenu/WorkbookMenuItems"));
        metadata.applicationHomeFolder = getTextContentOfSelectList(xmlDoc.selectNodes("//Workbook/ApplicationHomeFolder"));
        metadata.webPagesFolder = getTextContentOfSelectList(xmlDoc.selectNodes("//Workbook/WebPagesFolder"));
        metadata.menuItems = metadata.getMenuItems(xmlDoc.selectNodes("//WorkbookMenuItems/Label/Value"));
        //metadata.setProjectFolder();
        return metadata;

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

    public void setProjectFolder() {
        if (isEmpty(applicationHomeFolder) || isEmpty(webPagesFolder))
            return;
        // Since it is on windows it will have a different slash
        int indexOfFusionapps = applicationHomeFolder.indexOf("fusionapps\\");
        if (indexOfFusionapps < 0)
            return;
        // should give us fusionapps\fin\components\ledger
        String relativeApplicationFolder = applicationHomeFolder.substring(indexOfFusionapps + 1);

        if (isEmpty(relativeApplicationFolder))
            return;
        relativeApplicationFolder = relativeApplicationFolder.replaceAll("\\", "/");
        int publichtmlIndex = webPagesFolder.indexOf("public_html");
        if (publichtmlIndex < 0)
            return;
        // this will give us base dir
        String basePrjDir = webPagesFolder.substring(0, publichtmlIndex);
        basePrjDir = basePrjDir.replaceAll("\\", "/");
        if (!relativeApplicationFolder.endsWith("/"))
            relativeApplicationFolder = relativeApplicationFolder + "/";
        if (basePrjDir.startsWith("/"))
            basePrjDir = basePrjDir.substring(1);
        String prjPath = applicationHomeFolder + basePrjDir;
        projectFolder = prjPath;


    }

    public void initializeWorkbookId(NodeList workbookIdNode) {
        if (workbookIdNode == null || workbookIdNode.getLength() < 1)
            return;
        Node wrkidNode = workbookIdNode.item(0);
        if (wrkidNode == null)
            return;
        workbookId = wrkidNode.getTextContent();
        if (workbookId == null)
            workbookId = "";
        else
            workbookId = workbookId.trim();
    }

    public void initializeRuntimeRibTitle(NodeList runtimeTitleValNode) {
        if (runtimeTitleValNode == null || runtimeTitleValNode.getLength() < 1)
            return;
        Node valNode = runtimeTitleValNode.item(0);
        if (valNode == null)
            return;
        runtimeRibbonTitle = valNode.getTextContent();
        if (runtimeRibbonTitle == null)
            runtimeRibbonTitle = "";
        else
            runtimeRibbonTitle = runtimeRibbonTitle.trim();
    }

    public void initializeWorkbookMenuItems(NodeList menuNodes) {
        if (menuNodes == null)
            return;
        int resourcesLen = menuNodes.getLength();
        for (int i = 0; i < resourcesLen; i++) {
            Node menuNode = menuNodes.item(i);
            if (!XMLParserHelper.isNodeName(menuNode, "WorkbookMenuItems"))
                return;
            String methodValue = XMLParserHelper.getAttributeValue(menuNode, "Method");
            if (isEmpty(methodValue))
                continue;
            if(methodValue.equalsIgnoreCase("ToggleStatusViewer")){
               this.autoDisplayStatusViewerMethod="true"; 
            }
            Node labelNode = XMLParserHelper.getChildNodeWithName(menuNode, "Label");
            if (labelNode == null)
                continue;

            Node valNode = XMLParserHelper.getChildNodeWithName(labelNode, "Value");

            String labelValue = valNode.getTextContent();
            if (isEmpty(labelValue))
                continue;
            if (labelValue == null)
                labelValue = "";
            workbookCommands.put(methodValue.trim(), labelValue.trim());
        }
    }

    public void initializeResources(NodeList resourcesNode) {
        if (resourcesNode == null)
            return;
        int resourcesLen = resourcesNode.getLength();
        for (int i = 0; i < resourcesLen; i++) {
            Node resNode = resourcesNode.item(i);
            if (!XMLParserHelper.isNodeName(resNode, "Resources"))
                return;
            Node aliasNode = XMLParserHelper.getChildNodeWithName(resNode, "Alias");
            Node classNode = XMLParserHelper.getChildNodeWithName(resNode, "Class");
            if (aliasNode == null || classNode == null)
                continue;
            String alias = aliasNode.getTextContent();
            String klass = classNode.getTextContent();
            if (isEmpty(alias))
                continue;
            if (klass == null)
                klass = "";
            resources.put(alias.trim(), klass.trim());
        }
    }

    public void initializeBrandingItems(NodeList brandingItemsNode) {
        if (brandingItemsNode == null)
            return;
        int itemsLen = brandingItemsNode.getLength();
        for (int i = 0; i < itemsLen; i++) {
            Node bNode = brandingItemsNode.item(i);
            if (!XMLParserHelper.isNodeName(bNode, "BrandingItems"))
                return;

            Node bNameNode = XMLParserHelper.getChildNodeWithName(bNode, "Name");
            Node bValueNode = XMLParserHelper.getChildNodeWithName(bNode, "Value");
            if (bNameNode == null || bValueNode == null)
                continue;

            Node bNameValueNode = XMLParserHelper.getChildNodeWithName(bNameNode, "Value");
            Node bValueValueNode = XMLParserHelper.getChildNodeWithName(bValueNode, "Value");
            if (bNameValueNode == null || bValueValueNode == null)
                continue;
            String bName = bNameValueNode.getTextContent();
            String bVal = bValueValueNode.getTextContent();
            if (bVal == null)
                bVal = "";
            if (isEmpty(bName))
                continue;
            brandingItems.put(bName.trim(), bVal.trim());
        }
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    /**
     * Checks for:
     * RuntimeRibbon.Title
     * WorkbookCommands.item.Label
     * BrandingItems.item.Name
     * BrandingItems.item.Value
     * @return
     */
    @Override
    public ArrayList<CheckerResult> runLocalizationCheck() {
        String errorMsg = "Strings: Workbook";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        addResults(results, runBrandingItemLocalizationChecker());
        addResults(results, runRibbonTitleLocalizationChk(errorMsg));
        addResults(results, runWbComdsLocalizationChk(errorMsg));

        return results;
    }


    private ArrayList<CheckerResult> runBrandingItemLocalizationChecker() {
        String issue = CheckerConstants.ISSUE_RESOURCES;
        String resultCode = "R";
        String subIssue = "Strings:BrandingItems";

        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        if (brandingItems == null || brandingItems.size() < 1)
            return null;
        Set<Map.Entry<String, String>> entrySet = brandingItems.entrySet();
        Iterator<Map.Entry<String, String>> entryIter = entrySet.iterator();
        while (entryIter.hasNext()) {
            Map.Entry<String, String> entry = entryIter.next();
            String name = entry.getKey();
            String val = entry.getValue();
            if (isEmpty(name) || isEmpty(val))
                continue;
            if (!isLocalizedString(name, false)) {
                CheckerResult res = new CheckerResult(resultCode, issue, subIssue, false);
                String desc = String.format("Branding Item Name: (%s) must be localized", name);
                res.setDescription(desc);
                results.add(res);
            }
            if (!isLocalizedString(val, false)) {
                CheckerResult res = new CheckerResult(resultCode, issue, subIssue, false);
                String desc = String.format("Branding Item Value: (%s) must be localized", val);
                res.setDescription(desc);
                results.add(res);
            }
        }
        return results;
    }

    private ArrayList<CheckerResult> runWbComdsLocalizationChk(String errorMsg) {
        String resultCode = "R";
        String issue = CheckerConstants.ISSUE_RESOURCES;
        String subIssue = "String:WorkbookCommands";
        //String resultMsg = "Workbook Commands must be localized";

        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        if (workbookCommands == null || workbookCommands.size() < 1)
            return null;

        Set<Map.Entry<String, String>> entrySet = workbookCommands.entrySet();
        Iterator<Map.Entry<String, String>> entryIter = entrySet.iterator();
        while (entryIter.hasNext()) {
            Map.Entry<String, String> entry = entryIter.next();
            String name = entry.getKey();
            String val = entry.getValue();
            if (isEmpty(name) || isEmpty(val))
                continue;

            if (!isLocalizedString(val, false)) {
                CheckerResult res = new CheckerResult(resultCode, issue, subIssue, false);
                String desc = String.format("Workbook Command for: %s  Value: (%s) must be localized", name, val);
                res.setDescription(desc);
                results.add(res);
            }
        }
        return results;
    }

    private ArrayList<CheckerResult> runRibbonTitleLocalizationChk(String errorMsg) {
        String resultCode = "R";
        String issue = CheckerConstants.ISSUE_RESOURCES;
        String subIssue = "String:RibbonTitle";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        if (isEmpty(runtimeRibbonTitle))
            return results;
        if (!isLocalizedString(runtimeRibbonTitle, false)) {
            CheckerResult res = new CheckerResult(resultCode, issue, subIssue, false);
            String desc = String.format("Runtime Ribbon Title: (%s) must be localized", runtimeRibbonTitle);
            res.setDescription(desc);
            results.add(res);
        }
        return results;


    }

    public void print() {
        StringBuffer sb = new StringBuffer();
        sb.append("\n=================Workbook==================\n");
        sb.append(String.format("\n Workbook Id: %s", workbookId));
        sb.append(String.format("\n Runtime Ribbon Title : %s", runtimeRibbonTitle));
        sb.append("\n----> Resources");
        Set<Map.Entry<String, String>> entrySet = resources.entrySet();
        Iterator<Map.Entry<String, String>> entryIter = entrySet.iterator();
        int cnt = 0;
        while (entryIter.hasNext()) {
            cnt++;
            Map.Entry<String, String> e = entryIter.next();
            sb.append(String.format("\n\t %d. Alias: %s  Class: %s", cnt, e.getKey(), e.getValue()));
        }

        sb.append("\n\n----> Branding Items: ");
        Set<Map.Entry<String, String>> bientrySet = resources.entrySet();
        Iterator<Map.Entry<String, String>> bientryIter = bientrySet.iterator();
        cnt = 0;
        while (bientryIter.hasNext()) {
            cnt++;
            Map.Entry<String, String> e = bientryIter.next();
            sb.append(String.format("\n\t %d. Branding Item Name: %s  Value: %s", cnt, e.getKey(), e.getValue()));
        }


        sb.append("\n\n----> Workbook Commands");
        Set<Map.Entry<String, String>> rentrySet = workbookCommands.entrySet();
        Iterator<Map.Entry<String, String>> rentryIter = rentrySet.iterator();
        cnt = 0;
        while (rentryIter.hasNext()) {
            cnt++;
            Map.Entry<String, String> e = rentryIter.next();
            sb.append(String.format("\n\t %d. Method: %s  Label: %s", cnt, e.getKey(), e.getValue()));
        }

        sb.append("\n===========================================\n");
        System.out.println(sb.toString());
    }


    public String getWorkbookId() {
        return workbookId;
    }

    public ArrayList<String> getMenuItems() {
        return menuItems;
    }

    public HashMap<String, String> getResources() {
        return resources;
    }
    
    public boolean getCustomizationEnabled(){
        return customizationEnabled;
    }
    
    public boolean getAutoDisplayStatusViewerEnabled(){
        if(this.autoDisplayStatusViewerEnabled!="" && this.autoDisplayStatusViewerEnabled.equalsIgnoreCase("true")){
            return true;
        }
        else{
            return false;
        }
    }
    
    public boolean getAutoDisplayStatusViewerMehtod(){
        if(this.autoDisplayStatusViewerMethod!="" && this.autoDisplayStatusViewerMethod.equalsIgnoreCase("true")){
            return true;
        }
        else{
            return false;
        }
    }
}
