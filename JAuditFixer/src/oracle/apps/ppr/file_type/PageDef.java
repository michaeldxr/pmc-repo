package oracle.apps.ppr.file_type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PageDef
{
    static public class PageDefBinding
    {
        public String sJarName;
        public String sPageDefName;
        public String sBinds;
        public String sDataControl;
        public String sIterId;
        public String sBindingID;
        public String sBindingType;
        public String sListBinding;
        public String attrName;

        public String getSBinds() {
            return sBinds;
        }

        public String getSDataControl() {
            return sDataControl;
        }

        public String getSIterId() {
            return sIterId;
        }

        public String getSBindingID() {
            return sBindingID;
        }

        public void setSBindingID(String sBindingID) {
            this.sBindingID = sBindingID;
        }

        public String getSBindingType() {
            return sBindingType;
        }

        public void setSListBinding(String sUses) {
            this.sListBinding = sUses;
        }

        public String getSListBinding() {
            return sListBinding;
        }

        public String getAttrName() {
            return attrName;
        }
    }
    
    static public class PageDefIter
    {
        public String sBinds;
        public String sDataControl;
        public String sId;
    }
    
    static public HashMap<String, ArrayList<PageDefBinding>> m_pageDefBindings = 
        new HashMap<String, ArrayList<PageDefBinding>>();
    
    static public HashMap<String, PageDefIter> m_pageDefIterators = 
        new HashMap<String, PageDefIter>();
    
    private static String theJarName;
    public static String thePageDefName;
    
    public PageDef()
    {
        super();
    }

    public static void processPage(ZipFile zf, ZipEntry ze)
    {
        theJarName = zf.getName();
        thePageDefName = ze.getName();
        m_pageDefIterators.clear();

        try
        {   
            ArrayList<PageDefBinding> pageDefBindings = m_pageDefBindings.get(thePageDefName);
            
            if (pageDefBindings != null)
            {
                String type = "PageDef has been parsed";
                String description = "The Page Def appears to already have been processed";
            } 

            pageDefBindings = new ArrayList<PageDefBinding>();
            m_pageDefBindings.put(thePageDefName, pageDefBindings);           

            XMLDocument doc = XMLParserHelper.getXMLDocument(zf.getInputStream(ze),false);


            //Find, parse and initialize iterators
            NodeList nl1 = doc.getElementsByTagName("executables");
            int l = nl1.getLength();
            if(l != 1)
            {
                String type = "Multiple executable sections found";
                String description = "PageDef has " + l + " number of executable sections";
                description += "this pageDef appears to not have the expected executables section";
            }
            
            if(l == 0)
                return;
            
            PageDefIter iter;
            
            XMLElement e = (XMLElement)nl1.item(0);
            NodeList il = e.getElementsByTagName("iterator");
            
            for(int i = 0; i < il.getLength(); i++)
            {
                iter = new PageDefIter();
                Node pdi = il.item(i);
                
                NamedNodeMap attrs = pdi.getAttributes();
                
                Node attrNode = attrs.getNamedItem("Binds");
                if(attrNode == null)
                {
                    String type = "Iterator has no bindings associated";
                    String description = "PageDef has an iterator"   +iter.sId   +" that does not have a binding assigned.";

                    continue;
                }
                
                iter.sBinds = attrs.getNamedItem("Binds").getNodeValue();
                
                iter.sId = attrs.getNamedItem("id").getNodeValue();

                attrNode = attrs.getNamedItem("DataControl");
                if(attrNode == null)
                {
                    String type = "Iterator has no Data Control assigned";
                    String description = "PageDef has an iterator"   +iter.sId   +" that does not have an assigned DataControl.";

                    continue;
                }
                iter.sDataControl = attrNode.getNodeValue();
                
                m_pageDefIterators.put(iter.sId, iter);
            }
            
            
            //Bindings Section
            NodeList nl2 = doc.getElementsByTagName("bindings");
            int l2 = nl2.getLength();
            
            if(l2 != 1)
            {
                String type = "Multiple bindings sections found";
                String description = "PageDef has " +l + " number of bindings sections";
                description += "this pageDef appears to not have the expected bindings section";
            }
            
            if(l2 == 0)
                return;
            
            e = (XMLElement)nl2.item(0);
            populateBindingsData(e, "list", pageDefBindings); //af:selectOneChoice
            populateBindingsData(e, "button", pageDefBindings); //af:selectBooleanCheckbox
            populateBindingsData(e, "attributeValues", pageDefBindings); //af:inputText and af:inputDate
            populateBindingsData(e, "tree", pageDefBindings);
            populateBindingsData(e, "table", pageDefBindings);
        } 
        catch (Exception e)
        {
            System.out.println("*********   ERROR   **************************************");
            System.out.println(theJarName);
            System.out.println(thePageDefName);
            e.printStackTrace();
            System.out.println("*********   ERROR   **************************************");
        }
    }
    
    static void populateBindingsData(XMLElement bindings, String tag, ArrayList<PageDefBinding> pageDefBindings){
        PageDefBinding x;
        String id = null;
        String IterBindingID = null;
        String uses = null;
        String attrName = null;
        Node pdi;       
        NamedNodeMap attrs;
        PageDefIter iterator = null;
        boolean isLOV = false;
                   
        NodeList il = bindings.getElementsByTagName(tag);
   
        if(tag.equals("list") || tag.equals("button")){
            isLOV = true;
        }
        
        for(int i = 0; i < il.getLength(); i++){
             id = null;
             IterBindingID = null;
             uses = null;
             attrName = null;
          
            pdi = il.item(i);
            attrs = pdi.getAttributes();
            Node n = attrs.getNamedItem("id");
            if(n != null)
                id = n.getNodeValue();
            
            n = attrs.getNamedItem("IterBinding");
            if(n != null)
                IterBindingID = n.getNodeValue();


                Node child =
                    XMLParserHelper.getChildNodeWithName(pdi, "AttrNames");
                if (child != null) {
                    Node item = XMLParserHelper.getChildNodeWithName(child, "Item");
                    if (item != null)
                        attrName =
                                item.getAttributes().getNamedItem("Value").getNodeValue();
    
                }

            if (isLOV) {
                if (attrs.getNamedItem("Uses") != null)
                    uses =
                            attrs.getNamedItem("Uses").getNodeValue();
            }
            
            //Ignore static list
            if(IterBindingID != null)
                iterator = m_pageDefIterators.get(IterBindingID);
            
            if(iterator != null){
                x = new PageDefBinding();
                x.sBinds = iterator.sBinds;
                x.sDataControl = iterator.sDataControl;
                x.sIterId = iterator.sId;
                x.sJarName = theJarName;
                x.sPageDefName = thePageDefName;
                x.sBindingID = id;
                x.sBindingType = tag;
                x.sListBinding = uses;
                x.attrName = attrName;
                
                pageDefBindings.add(x);
            }else{
                String type = "Missing Iterator";
                String description = "Can't find corresponding iterator " + IterBindingID + " for binding " + id;
            }
            
            
        }
    }
}
