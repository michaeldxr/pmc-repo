package oracle.apps.helpers.parsers;

import java.util.ArrayList;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.NSResolver;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UIPageDef {

    ArrayList<InvokeAction> invokeActions = new ArrayList<InvokeAction>();
    ArrayList<PageDefIteratorBinding> iteratorBindings = new ArrayList<PageDefIteratorBinding>();
    ArrayList<ActionBinding> actionBindings = new ArrayList<ActionBinding>();
    ArrayList<MethodActionBinding> methodActionBindings = new ArrayList<MethodActionBinding>();
    ArrayList<PageDefTreeBindings> pageDefTreeBindings = new ArrayList<PageDefTreeBindings>();
    ArrayList<AttributeBindings> attributeBindings = new ArrayList<AttributeBindings>();
    ArrayList<PageDefListBindings> listBindings = new ArrayList<PageDefListBindings>();
String pageDefPath="";
    public static void main(String[] args) throws Exception {
        UIPageDef def =
            UIPageDef.getPageDef(FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP3DEV_LINUX.X64") + 
            "/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/adfmsrc/oracle/apps/financials/generalLedger/journals/desktopEntry/di/page/FinGlDesktopEntryPageDef.xml");
        int i =0;
    }

    public UIPageDef(String pageDefPath) {
        super();
        this.pageDefPath=pageDefPath;
    }

    public static UIPageDef getPageDef(String path) {
        UIPageDef uipgdef = new UIPageDef(path);
        try {
            XMLDocument pageDefDoc = XMLParserHelper.getXMLDocument(path);
            if (pageDefDoc == null)
                return null;

            pageDefDoc.setProperty("SelectionLanguage", "XPath");
            uipgdef.create(pageDefDoc);
        } catch (Exception ex) {
            System.out.println("Exception parsing page def: " + path);
            ex.printStackTrace();
        }
        return uipgdef;
    }

    public void create(XMLDocument doc) throws Exception {

        PageDefNSResolver resolver = new PageDefNSResolver();
        int i = 0;
        int len=0;
        NodeList invokeActionNodeList = doc.selectNodes("//ns:executables/ns:invokeAction", resolver);
        if (invokeActionNodeList != null) {
            len = invokeActionNodeList.getLength();
            for (i = 0; i < len; i++) {
                InvokeAction b = InvokeAction.getInvokeAction(invokeActionNodeList.item(i));
                if (b != null)
                    invokeActions.add(b);
            }
        }
        NodeList iteratorNodeList = doc.selectNodes("//ns:executables/ns:iterator", resolver);
        if (iteratorNodeList != null) {
            len = iteratorNodeList.getLength();
            for (i = 0; i < len; i++) {
                PageDefIteratorBinding b = PageDefIteratorBinding.getPageDefIterator(iteratorNodeList.item(i));
                if (b != null)
                    iteratorBindings.add(b);
            }
        }
        
        NodeList attrList = doc.selectNodes("//ns:bindings/ns:attributeValues", resolver);
        if (attrList != null) {
            len = attrList.getLength();
            for (i = 0; i < len; i++) {
                AttributeBindings b = AttributeBindings.getAttributeBinding(attrList.item(i));
                if (b != null)
                    attributeBindings.add(b);
            }
        }


        NodeList methodActionList = doc.selectNodes("//ns:bindings/ns:methodAction", resolver);
        if (methodActionList != null) {
            len = methodActionList.getLength();
            for (i = 0; i < len; i++) {
                MethodActionBinding b = MethodActionBinding.getMethodAction(methodActionList.item(i));
                if (b != null)
                    methodActionBindings.add(b);
            }
        }
        
        NodeList actionList = doc.selectNodes("//ns:bindings/ns:action", resolver);
        if (actionList != null) {
            len = actionList.getLength();
            for (i = 0; i < len; i++) {
                ActionBinding b = ActionBinding.getAction(actionList.item(i));
                if (b != null)
                    actionBindings.add(b);
            }
        }
        
        NodeList treeList = doc.selectNodes("//ns:bindings/ns:tree", resolver);
        if (treeList != null) {
            len = treeList.getLength();
            for (i = 0; i < len; i++) {
                 PageDefTreeBindings b = PageDefTreeBindings.getTreeBinding(treeList.item(i));
                if (b != null)
                    pageDefTreeBindings.add(b);
            }
        }
        
        NodeList listList = doc.selectNodes("//ns:bindings/ns:list", resolver);
        if (listList != null) {
            len = listList.getLength();
            for (i = 0; i < len; i++) {
                 PageDefListBindings b = PageDefListBindings.getListBinding(listList.item(i));
                if (b != null)
                    listBindings.add(b);
            }
        }
        
    }

    public ArrayList<UIPageDef.PageDefIteratorBinding> getIteratorBindings() {
        return iteratorBindings;
    }

    public ArrayList<UIPageDef.ActionBinding> getActionBindings() {
        return actionBindings;
    }

    public ArrayList<UIPageDef.MethodActionBinding> getMethodActionBindings() {
        return methodActionBindings;
    }

    public ArrayList<UIPageDef.PageDefTreeBindings> getPageDefTreeBindings() {
        return pageDefTreeBindings;
    }

    public ArrayList<UIPageDef.AttributeBindings> getAttributeBindings() {
        return attributeBindings;
    }

    public ArrayList<UIPageDef.PageDefListBindings> getListBindings() {
        return listBindings;
    }

    public ArrayList<UIPageDef.InvokeAction> getInvokeActions() {
        return invokeActions;
    }

    public class PageDefNSResolver implements NSResolver {
        public PageDefNSResolver() {
            super();
        }

        public String resolveNamespacePrefix(String prefix) {
            return "http://xmlns.oracle.com/adfm/uimodel";
        }
    }


    public static class ActionBinding {
        String id = "";
        String instanceName = "";
        String dcName = "";
        String requiresUpdateModel = "";
        String action = "";
        String iteratorBinding="";

        public static ActionBinding getAction(Node methodActionNode) {
            if (methodActionNode == null)
                return null;
            if (!XMLParserHelper.isNodeName(methodActionNode, "action"))
                return null;
            ActionBinding bndg = new ActionBinding();
            bndg.id = XMLParserHelper.getAttributeValue(methodActionNode, "id");
            bndg.instanceName = XMLParserHelper.getAttributeValue(methodActionNode, "InstanceName");
            bndg.dcName = XMLParserHelper.getAttributeValue(methodActionNode, "DataControl");
            bndg.requiresUpdateModel = XMLParserHelper.getAttributeValue(methodActionNode, "RequiresUpdateModel");
            bndg.action = XMLParserHelper.getAttributeValue(methodActionNode, "Action");
            bndg.iteratorBinding = XMLParserHelper.getAttributeValue(methodActionNode, "IterBinding");
            return bndg;
        }

        public String getId() {
            return id;
        }

        public String getInstanceName() {
            return instanceName;
        }

        public String getDcName() {
            return dcName;
        }

        public String getRequiresUpdateModel() {
            return requiresUpdateModel;
        }

        public String getAction() {
            return action;
        }

        public String getIteratorBinding() {
            return iteratorBinding;
        }
    }

    public static class MethodActionBinding {
        String id = "";
        String instanceName = "";
        String dcName = "";
        String requiresUpdateModel = "";
        String action = "";
        String methodName = "";
        String isVOMethod = "";
       
        

        public static MethodActionBinding getMethodAction(Node methodActionNode) {
            if (methodActionNode == null)
                return null;
            if (!XMLParserHelper.isNodeName(methodActionNode, "methodAction"))
                return null;
            MethodActionBinding mbindings = new MethodActionBinding();
            mbindings.id = XMLParserHelper.getAttributeValue(methodActionNode, "id");
            mbindings.instanceName = XMLParserHelper.getAttributeValue(methodActionNode, "InstanceName");
            mbindings.dcName = XMLParserHelper.getAttributeValue(methodActionNode, "DataControl");
            mbindings.requiresUpdateModel = XMLParserHelper.getAttributeValue(methodActionNode, "RequiresUpdateModel");
            mbindings.action = XMLParserHelper.getAttributeValue(methodActionNode, "Action");
            mbindings.methodName = XMLParserHelper.getAttributeValue(methodActionNode, "MethodName");
            mbindings.isVOMethod = XMLParserHelper.getAttributeValue(methodActionNode, "IsViewObjectMethod");
           

            return mbindings;
        }

        public String getId() {
            return id;
        }

        public String getInstanceName() {
            return instanceName;
        }

        public String getDcName() {
            return dcName;
        }

        public String getAction() {
            return action;
        }

        public String getMethodName() {
            return methodName;
        }

      

     
    }

    public static class AttributeBindings {
        String iterBindingId = "";
        String id = "";
        ArrayList<String> attributeNames = new ArrayList<String>();

        public static AttributeBindings getAttributeBinding(Node attributeValuesNode) {
            if (attributeValuesNode == null)
                return null;
            if (!XMLParserHelper.isNodeName(attributeValuesNode, "attributeValues"))
                return null;
            AttributeBindings attrBinding = new AttributeBindings();
            attrBinding.iterBindingId = XMLParserHelper.getAttributeValue(attributeValuesNode, "IterBinding");
            attrBinding.id = XMLParserHelper.getAttributeValue(attributeValuesNode, "id");
            Node attrNamesNode = XMLParserHelper.getChildNodeWithName(attributeValuesNode, "AttrNames");
            if (attrNamesNode != null) {
                NodeList itemValues = attrNamesNode.getChildNodes();
                if (itemValues != null) {
                    int itemCnt = itemValues.getLength();
                    for (int i = 0; i < itemCnt; i++) {
                        Node itemNode = itemValues.item(i);
                        if (XMLParserHelper.isNodeName(itemNode, "Item")) {
                            String v = XMLParserHelper.getAttributeValue(itemNode, "Value");
                            if (v != null && !v.trim().equals(""))
                                attrBinding.attributeNames.add(v);
                        }
                    }
                }
            }
            return attrBinding;
        }

        public String getIterBindingId() {
            return iterBindingId;
        }

        public String getId() {
            return id;
        }

        public ArrayList<String> getAttributeNames() {
            return attributeNames;
        }
    }

    public static class InvokeAction {
        String binds = "";
        String id = "";
        String refresh = "";

        public static InvokeAction getInvokeAction(Node invokeActionNode) {
            if (invokeActionNode == null)
                return null;
            if (!XMLParserHelper.isNodeName(invokeActionNode, "invokeAction"))
                return null;
            InvokeAction invAct = new InvokeAction();
            invAct.binds = XMLParserHelper.getAttributeValue(invokeActionNode, "Binds");
            invAct.id = XMLParserHelper.getAttributeValue(invokeActionNode, "id");
            invAct.refresh = XMLParserHelper.getAttributeValue(invokeActionNode, "Refresh");
            return invAct;
        }

        public String getBinds() {
            return binds;
        }

        public String getId() {
            return id;
        }

        public String getRefresh() {
            return refresh;
        }
    }


    public static class PageDefTreeBindings {
        String id = "";
        String iteratorName = "";
        String nodeDef = "";
        String nodeDefName = "";

        public static PageDefTreeBindings getTreeBinding(Node treeBinding) {
            if (treeBinding == null)
                return null;
            if (!XMLParserHelper.isNodeName(treeBinding, "tree"))
                return null;
            PageDefTreeBindings bndg = new PageDefTreeBindings();
            bndg.iteratorName = XMLParserHelper.getAttributeValue(treeBinding, "IterBinding");
            bndg.id = XMLParserHelper.getAttributeValue(treeBinding, "id");
            Node defNode = XMLParserHelper.getChildNodeWithName(treeBinding, "nodeDefinition");
            if (defNode != null) {
                bndg.nodeDef = XMLParserHelper.getAttributeValue(defNode, "DefName");
                bndg.nodeDefName = XMLParserHelper.getAttributeValue(defNode, "Name");
            }
            return bndg;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getIteratorName() {
            return iteratorName;
        }

        public String getNodeDef() {
            return nodeDef;
        }

        public String getNodeDefName() {
            return nodeDefName;
        }
    }

    public static class PageDefListBindings {
        String id = "";
        String iteratorBindingName = "";
        String staticList="";
        String listIter="";
        String nullValueId="";
        String nullValueFlag="";
        
        String lovUses = "";
        String listOpermode = "";
        String listAttrName = "";
        String lovType = "";
        public static PageDefListBindings getListBinding(Node listBindingNode) {
            if (listBindingNode == null)
                return null;
            if (!XMLParserHelper.isNodeName(listBindingNode, "list"))
                return null;
            PageDefListBindings bndg = new PageDefListBindings();
            
            bndg.id = XMLParserHelper.getAttributeValue(listBindingNode, "id");
            bndg.iteratorBindingName = XMLParserHelper.getAttributeValue(listBindingNode, "IterBinding");
            bndg.staticList = XMLParserHelper.getAttributeValue(listBindingNode, "StaticList");
            bndg.listIter = XMLParserHelper.getAttributeValue(listBindingNode, "ListIter");
            bndg.nullValueFlag = XMLParserHelper.getAttributeValue(listBindingNode, "NullValueFlag");
            bndg.nullValueId = XMLParserHelper.getAttributeValue(listBindingNode, "NullValueId");
            
            return bndg;
        }

        public String getId() {
            return id;
        }

        public String getIteratorBindingName() {
            return iteratorBindingName;
        }

        public String getStaticList() {
            return staticList;
        }

        public String getListIter() {
            return listIter;
        }

        public String getNullValueId() {
            return nullValueId;
        }

        public String getNullValueFlag() {
            return nullValueFlag;
        }

        public String getLovUses() {
            return lovUses;
        }

        public String getListOpermode() {
            return listOpermode;
        }

        public String getListAttrName() {
            return listAttrName;
        }

        public String getLovType() {
            return lovType;
        }
    }

    public static class PageDefIteratorBinding {
        String id = "";
        String dataControlName = "";
        String rangeSize = "";
        String bindName = "";

        public static PageDefIteratorBinding getPageDefIterator(Node iteratorNode) {
            if (iteratorNode == null)
                return null;
            if (!XMLParserHelper.isNodeName(iteratorNode, "iterator"))
                return null;
            PageDefIteratorBinding bndg = new PageDefIteratorBinding();
            bndg.bindName = XMLParserHelper.getAttributeValue(iteratorNode, "Binds");
            bndg.id = XMLParserHelper.getAttributeValue(iteratorNode, "id");
            bndg.dataControlName = XMLParserHelper.getAttributeValue(iteratorNode, "DataControl");
            bndg.rangeSize = XMLParserHelper.getAttributeValue(iteratorNode, "RangeSize");
            return bndg;
        }

        public String getId() {
            return id;
        }

        public String getDataControlName() {
            return dataControlName;
        }

        public String getRangeSize() {
            return rangeSize;
        }

        public String getBindName() {
            return bindName;
        }
    }
}
