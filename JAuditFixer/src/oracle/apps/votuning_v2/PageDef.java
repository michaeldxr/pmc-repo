package oracle.apps.votuning_v2;

import java.util.HashMap;

import java.util.Iterator;
import java.util.Set;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PageDef {
    String absPath = "";
    static boolean  bDebug = false;

    public PageDef() {
        super();
    }
    HashMap<String, PageDefIteratorBinding> mapIterators = new HashMap<String, PageDefIteratorBinding>();

    HashMap<String, pageDefTreeBindings> mapTreeBindings = new HashMap<String, pageDefTreeBindings>();

    HashMap<String, pageDefListBindings> mapListBindings = new HashMap<String, pageDefListBindings>();

    public PageDefIteratorBinding getIterBindingForTreeId(String id) {
        if (mapTreeBindings == null || isEmpty(id) || mapIterators == null) {
            return null;
        }
        if (mapTreeBindings.containsKey(id)) {
            pageDefTreeBindings treeBinding = mapTreeBindings.get(id);
            String iteratorId = treeBinding.getIteratorName();
            return mapIterators.get(iteratorId);
        }

        return null;
    }

    public PageDefIteratorBinding getIterBindingForListId(String id) {
        if (mapListBindings == null || isEmpty(id) || mapIterators == null) {
            return null;
        }
        if (mapListBindings.containsKey(id)) {
            pageDefListBindings listBinding = mapListBindings.get(id);
            String iteratorId = listBinding.getIteratorBindingName();
            return mapIterators.get(iteratorId);
        }

        return null;
    }
    public PageDefIteratorBinding getIteratorBindingWithId(String iteratorId) {
       
      
            return mapIterators.get(iteratorId);

       
    }

    public pageDefListBindings getListBindingForId(String id) {
        if (mapListBindings == null || isEmpty(id) || mapIterators == null) {
            return null;
        }
        if (mapListBindings.containsKey(id)) {
            pageDefListBindings listBinding = mapListBindings.get(id);
            return listBinding;
        }
        return null;
    }

    /**
     *Assumptions
     * 1. page def will have onlyone executable node
     * @param pageDefPath
     * @return
     */
    public static PageDef parsePageDef(String pageDefPath) {
        if (isEmpty(pageDefPath)) {
            return null;
        }
        PageDef pagedefinst = new PageDef();
        pagedefinst.absPath = pageDefPath;

        XMLDocument doc = XMLParserHelper.getXMLDocument(pageDefPath);
        NodeList executableNodes = doc.getElementsByTagName("executables");
        if (!XMLParserHelper.hasOneNOnlyOneNode(executableNodes)) {
            System.out.println("[ERROR] Assumption: There should be one and only one executables node in the page def.." +
                               pageDefPath);

        } else {
            Node executableNode = executableNodes.item(0);
            NodeList executableChildList = executableNode.getChildNodes();
            if (executableChildList == null || executableChildList.getLength() <= 0) {
                if(bDebug){
                System.out.println("did not find any executables in the page def" + pageDefPath);
                }
            } else {
                int childLen = executableChildList.getLength();
                for (int i = 0; i < childLen; i++) {
                    Node executableChild = executableChildList.item(i);
                    String nodeName = executableChild.getNodeName();
                    if (!isEmpty(nodeName) && nodeName.trim().toLowerCase().endsWith("iterator")) {
                        String bindName = XMLParserHelper.getAttributeValue(executableChild, "Binds");
                        String rangeSize = XMLParserHelper.getAttributeValue(executableChild, "RangeSize");
                        Integer iRangeSize = -37;
                        if (!isEmpty(rangeSize)) {
                            try {
                                iRangeSize = Integer.parseInt(rangeSize);
                            } catch (Exception ex) {
                                System.out.println("Could not convert range size to number: " + rangeSize);
                            }
                        }
                        String datacontrol = XMLParserHelper.getAttributeValue(executableChild, "DataControl");
                        String id = XMLParserHelper.getAttributeValue(executableChild, "id");
                        pagedefinst.createIterBinding(id, bindName, iRangeSize, datacontrol);
                    }
                }
            }
        }

        NodeList bindingsNodes = doc.getElementsByTagName("bindings");
        if (!XMLParserHelper.hasOneNOnlyOneNode(bindingsNodes)) {
            System.out.println("[ERROR] Assumption: There should be one and only one bindings node in the page def.." +
                               pageDefPath);

        } else {
            Node bindingNode = bindingsNodes.item(0);
            NodeList bindingsChildList = bindingNode.getChildNodes();
            if (bindingsChildList == null || bindingsChildList.getLength() <= 0) {
            if(bDebug)
            {
                System.out.println("did not find any bindings in the page def" + pageDefPath);
            }
            } else {
                int childLen = bindingsChildList.getLength();
                for (int i = 0; i < childLen; i++) {
                    Node bindingChild = bindingsChildList.item(i);
                    if (XMLParserHelper.isNodeName(bindingChild, "tree")) {
                        String iterBindingName = XMLParserHelper.getAttributeValue(bindingChild, "IterBinding");
                        String id = XMLParserHelper.getAttributeValue(bindingChild, "id");
                        String vodefName = "";
                        NodeList treeBindingChildren = bindingChild.getChildNodes();
                        if (treeBindingChildren != null && treeBindingChildren.getLength() >= 0) {
                            int treeChildLen = treeBindingChildren.getLength();
                            for (int j = 0; j < treeChildLen; j++) {
                                Node treeChild = treeBindingChildren.item(j);
                                if (XMLParserHelper.isNodeName(treeChild, "nodeDefinition")) {
                                    vodefName = XMLParserHelper.getAttributeValue(treeChild, "DefName");
                                }
                            }
                        }
                        pagedefinst.createTreeBinding(id, iterBindingName, vodefName);
                    } else if (XMLParserHelper.isNodeName(bindingChild, "listOfValues") ||
                               XMLParserHelper.isNodeName(bindingChild, "list")) {
                        String iterBindingName = XMLParserHelper.getAttributeValue(bindingChild, "IterBinding");
                        String id = XMLParserHelper.getAttributeValue(bindingChild, "id");
                        String usesLov = XMLParserHelper.getAttributeValue(bindingChild, "Uses");
                        String listOperMode = XMLParserHelper.getAttributeValue(bindingChild, "ListOperMode");
                        String listIter = XMLParserHelper.getAttributeValue(bindingChild, "ListIter");
                        String lovAttr = "";
                        if (isEmpty(usesLov)) {
                            Node attrNameNode = XMLParserHelper.getChildNodeWithName(bindingChild, "AttrNames");
                            if (attrNameNode != null) {
                                Node attrItemNode = XMLParserHelper.getChildNodeWithName(attrNameNode, "Item");
                                if (attrItemNode != null) {
                                    lovAttr = XMLParserHelper.getAttributeValue(attrItemNode, "Value");

                                }
                            }
                        }
                        pagedefinst.createListBinding(id, iterBindingName, usesLov, listOperMode, listIter, lovAttr);
                    } else if (XMLParserHelper.isNodeName(bindingChild, "navigationlist")) {
                        String iterBindingName = XMLParserHelper.getAttributeValue(bindingChild, "IterBinding");
                        String id = XMLParserHelper.getAttributeValue(bindingChild, "id");
                        String listOperMode = XMLParserHelper.getAttributeValue(bindingChild, "ListOperMode");
                        String listIter = XMLParserHelper.getAttributeValue(bindingChild, "ListIter");
                        pagedefinst.createNavListBinding(id, iterBindingName, listOperMode, listIter);
                    }
                }
            }
        }


        return pagedefinst;
    }

    public void createIterBinding(String id, String bindName, Integer rangeSize, String datacontrol) {
        PageDefIteratorBinding binding = new PageDefIteratorBinding();
        binding.setBindName(bindName);
        binding.setId(id);
        binding.setRangeSize(rangeSize);
        binding.setDataControlName(datacontrol);
        mapIterators.put(id, binding);
    }

    public void createTreeBinding(String id, String iterName, String voDefName) {
        pageDefTreeBindings treeBinding = new pageDefTreeBindings();
        treeBinding.id = id;
        treeBinding.IteratorName = iterName;
        treeBinding.nodeDefName = voDefName;
        mapTreeBindings.put(id, treeBinding);
    }

    public void createListBinding(String id, String iterName, String usesLov) {
        pageDefListBindings listBindings = new pageDefListBindings();
        listBindings.id = id;
        listBindings.iteratorBindingName = iterName;
        listBindings.lovUses = usesLov;
        if (isEmpty(usesLov)) {
            listBindings.setLovType(TuningConstants.LOV_TYPE_DYNAMICLIST);
        } else {
            listBindings.setLovType(TuningConstants.LOV_TYPE_MODEL_DRIVEN);
        }
        mapListBindings.put(id, listBindings);
    }

    public void createListBinding(String id, String iterName, String usesLov, String listOperMode, String listIter,
                                  String attrName) {
        pageDefListBindings listBindings = new pageDefListBindings();
        listBindings.id = id;
        listBindings.iteratorBindingName = iterName;
        listBindings.lovUses = usesLov;
        listBindings.listOpermode = listOperMode;
        listBindings.listIter = listIter;
        listBindings.listAttrName = attrName;
        if (isEmpty(usesLov)) {
            listBindings.setLovType(TuningConstants.LOV_TYPE_DYNAMICLIST);
        } else {
            listBindings.setLovType(TuningConstants.LOV_TYPE_MODEL_DRIVEN);
        }
        mapListBindings.put(id, listBindings);
    }

    public void createNavListBinding(String id, String iterName, String listOperMode, String listIter) {
        pageDefListBindings listBindings = new pageDefListBindings();
        listBindings.id = id;
        listBindings.iteratorBindingName = iterName;

        listBindings.listOpermode = listOperMode;
        listBindings.listIter = listIter;
        listBindings.setLovType(TuningConstants.LOV_TYPE_NAVIGATION);
        mapListBindings.put(id, listBindings);
    }

    public void printPageDef() {
        System.out.println("==========Printing page def===========");
        System.out.println("Page def path: " + absPath);
        System.out.println("Iterator bindings");
        Set<String> iterKeys = mapIterators.keySet();
        Iterator<String> keyIter = iterKeys.iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            PageDefIteratorBinding iterbinding = mapIterators.get(key);
            System.out.println("----------------------------------------------------");
            System.out.println("Iterator Id: " + iterbinding.getId());
            System.out.println("Bind Name: " + iterbinding.getBindName());
            System.out.println("Data control name: " + iterbinding.getDataControlName());
            System.out.println("Range Size: " + iterbinding.getRangeSize());
            System.out.println("----------------------------------------------------");
        }
        System.out.println(".");
        System.out.println("Tree bindings");
        Set<String> setTreeKeys = mapTreeBindings.keySet();
        Iterator<String> treeKeyIter = setTreeKeys.iterator();
        while (treeKeyIter.hasNext()) {
            String treeKey = treeKeyIter.next();
            pageDefTreeBindings treeBinding = mapTreeBindings.get(treeKey);
            System.out.println("----------------------------------------------------");
            System.out.println("Tree Binding Id: " + treeBinding.getId());
            System.out.println("Iterator Name: " + treeBinding.getIteratorName());
            System.out.println("Node Def name: " + treeBinding.getNodeDefName());

            System.out.println("----------------------------------------------------");
        }
        System.out.println(".");
        System.out.println("===================================");
    }


    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }
  public HashMap<String, PageDef.PageDefIteratorBinding> getMapIterators() {
      return mapIterators;
  }

  public String getAbsPath() {
      return absPath;
  }
  
    public class pageDefTreeBindings {
        String id = "";
        String IteratorName = "";
        String nodeDefName = "";

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setIteratorName(String IteratorName) {
            this.IteratorName = IteratorName;
        }

        public String getIteratorName() {
            return IteratorName;
        }

        public void setNodeDefName(String nodeDefName) {
            this.nodeDefName = nodeDefName;
        }

        public String getNodeDefName() {
            return nodeDefName;
        }
    }

    public class pageDefListBindings {
        String id = "";
        String iteratorBindingName = "";
        String lovUses = "";
        String listOpermode = "";
        String listIter = "";
        String listAttrName = "";
        String lovType = "";

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }


        public void setIteratorBindingName(String iteratorBindingName) {
            this.iteratorBindingName = iteratorBindingName;
        }

        public String getIteratorBindingName() {
            return iteratorBindingName;
        }

        public void setLovUses(String lovUses) {
            this.lovUses = lovUses;
        }

        public String getLovUses() {
            return lovUses;
        }

        public void setListOpermode(String listOpermode) {
            this.listOpermode = listOpermode;
        }

        public String getListOpermode() {
            return listOpermode;
        }

        public void setListIter(String listIter) {
            this.listIter = listIter;
        }

        public String getListIter() {
            return listIter;
        }

        public void setListAttrName(String listAttrName) {
            this.listAttrName = listAttrName;
        }

        public String getListAttrName() {
            return listAttrName;
        }

        public void setLovType(String lovType) {
            this.lovType = lovType;
        }

        public String getLovType() {
            return lovType;
        }
    }

    public class PageDefIteratorBinding {
        String id = "";
        String DataControlName = "";
        Integer RangeSize = -362;
        String BindName = "";

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public void setDataControlName(String DataControlName) {
            this.DataControlName = DataControlName;
        }

        public String getDataControlName() {
            return DataControlName;
        }

        public void setRangeSize(Integer RangeSize) {
            this.RangeSize = RangeSize;
        }

        public Integer getRangeSize() {
            return RangeSize;
        }

        public void setBindName(String BindName) {
            this.BindName = BindName;
        }

        public String getBindName() {
            return BindName;
        }
    }

}

