package oracle.apps.ppr.file_type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class EntityObject {


    public void setAl_va(ArrayList<EntityObject.ViewAccessor> al_va) {
        this.al_va = al_va;
    }

    public ArrayList<EntityObject.ViewAccessor> getAl_va() {
        return al_va;
    }

    public Set<String> getAttrsWithValidation() {
        return s_attrsWithValidation;
    }

    static public class ViewAccessor {
        public String name;
        public String VOName;

        public ViewAccessor(String name, String VOName) {
            this.name = name;
            this.VOName = VOName;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setVOName(String VOName) {
            this.VOName = VOName;
        }

        public String getVOName() {
            return VOName;
        }
    }

    String jar;
    String fname;
    String name;
    ArrayList<ViewAccessor> al_va = new ArrayList<ViewAccessor>();
    Set<String> s_attrsWithValidation = new HashSet<String>();
    
    public EntityObject(ZipFile zf, ZipEntry ze) {
        super();

        try {
            jar = zf.getName();
            fname = ze.getName();


            XMLDocument doc =
                XMLParserHelper.getXMLDocument(zf.getInputStream(ze),false);
            Node root = doc.getDocumentElement();

            Node nName = root.getAttributes().getNamedItem("Name");
            name = nName.getNodeValue();
            
            NodeList nl1 = doc.getElementsByTagName("Attribute");
            for (int i = 0; i < nl1.getLength(); i++) {
                Node vAttrs = nl1.item(i);
                NodeList children = vAttrs.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    if(children.item(j).getNodeName().toLowerCase().contains("validation")){
                        NamedNodeMap attrs = vAttrs.getAttributes();
                        s_attrsWithValidation.add(attrs.getNamedItem("Name").getNodeValue().toLowerCase());
                    }
                }
            }
/*
            <UniqueKeyValidationBean xmlns="http://xmlns.oracle.com/adfm/validation" Name="CostBaseEO_Rule_0" ResId="PJF:::PJF_CB_COST_BASE_NOT_UNIQUE"
                KeyName="BurdenCostBaseAltKey" ResBundleId="oracle.apps.fnd.applcore.messages.FndMessagesResourceBundle">
                <OnAttributes>
                    <Item Value="CostBase"/>
                    <Item Value="CostBaseType"/>
            <   /OnAttributes>
            </UniqueKeyValidationBean>
*/
      nl1 = doc.getElementsByTagName("OnAttributes");
      for (int i = 0; i < nl1.getLength(); i++) {
        Node item = nl1.item(i);
        if (item.getNamespaceURI().equals("http://xmlns.oracle.com/adfm/validation")) {
          NodeList items = item.getChildNodes();
          for (int k = 0; k < items.getLength(); k++) {
            if (items.item(k).getNodeName().equals("Item") || items.item(k).getNodeName().equals("validation:Item")) {
              NamedNodeMap attrs = items.item(k).getAttributes();
              s_attrsWithValidation.add(attrs.getNamedItem("Value").getNodeValue().toLowerCase());
            }
          }
        }

      }

            nl1 = doc.getElementsByTagName("ViewAccessor");

            for (int i = 0; i < nl1.getLength(); i++) {
                Node va = nl1.item(i);
                NamedNodeMap attrs = va.getAttributes();
                al_va.add(new ViewAccessor(attrs.getNamedItem("Name").getNodeValue(),
                                           attrs.getNamedItem("ViewObjectName").getNodeValue()));
            }
        
            
        } catch (Exception e) {
            System.out.println(jar);
            System.out.println(fname);
            e.printStackTrace();
        }
    }

    public String getJar() {
        return jar;
    }

    public String getFname() {
        return fname;
    }

    public String getName() {
        return name;
    }
}
