package oracle.apps.ppr.file_type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ViewObject {
    public ArrayList<ViewObject.ViewAccessor> getAl_va() {
        return al_va;
    }

    public ArrayList<ViewObject.ListBinding> getAl_listBinding() {
        return al_listBinding;
    }

    public void setAl_eo(ArrayList<ViewObject.EntityUsage> al_eo) {
        this.al_eo = al_eo;
    }

    public ArrayList<ViewObject.EntityUsage> getAl_eo() {
        return al_eo;
    }

    public Set<String> getAttrsWithValidation() {
        return s_attrsWithValidation;
    }

    public Map<String, String> getM_attrsWithEO() {
        return m_attrsWithEO;
    }

    static public class ListBinding {
        public String name;
        public String listVOName;
        public HashSet<String> attrs;

        public ListBinding(String name, String listVOName, HashSet<String> s_attrs) {
            this.name = name;
            this.listVOName = listVOName;
            this.attrs = s_attrs;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setListVOName(String ListVOName) {
            this.listVOName = ListVOName;
        }

        public String getListVOName() {
            return listVOName;
        }

        public void setAttrs(HashSet<String> attrs) {
          this.attrs = attrs;
        }
    
        public HashSet<String> getAttrs() {
          return attrs;
        }
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
    
    static public class EntityUsage {
        public String name;
        public String entity;

        public EntityUsage(String name, String entity) {
            this.name = name;
            this.entity = entity;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }


        public void setEntity(String entity) {
            this.entity = entity;
        }

        public String getEntity() {
            return entity;
        }
    }

    Long defaultFetchSize;
    String jar;
    String fname;
    String name;
    ArrayList<ViewAccessor> al_va = new ArrayList<ViewAccessor>();
    ArrayList<ListBinding> al_listBinding = new ArrayList<ListBinding>();
    ArrayList<EntityUsage> al_eo = new ArrayList<EntityUsage>();
    Set<String> s_attrsWithValidation = new HashSet<String>();
    Map<String,String> m_attrsWithEO = new HashMap<String,String>();

    public ViewObject(ZipFile zf, ZipEntry ze) {
        super();

        try {
            jar = zf.getName();
            fname = ze.getName();


            XMLDocument doc =
                XMLParserHelper.getXMLDocument(zf.getInputStream(ze), false);
            Node root = doc.getDocumentElement();

            Node nName = root.getAttributes().getNamedItem("Name");
            name = nName.getNodeValue();

            NodeList nl1 = doc.getElementsByTagName("ViewAccessor");

            for (int i = 0; i < nl1.getLength(); i++) {
                Node va = nl1.item(i);
                NamedNodeMap attrs = va.getAttributes();
                al_va.add(new ViewAccessor(attrs.getNamedItem("Name").getNodeValue(),
                                           attrs.getNamedItem("ViewObjectName").getNodeValue()));
            }
            
            nl1 = doc.getElementsByTagName("ListBinding");

            for (int i = 0; i < nl1.getLength(); i++) {
                Node va = nl1.item(i);
                NamedNodeMap attrs = va.getAttributes();
                HashSet<String> s_attrs = new HashSet<String>();
                for(Node attr : XMLParserHelper.getChildNodesWithMatchingAttributeAndName(va, "AttrArray", "Name", "AttrNames")){
                  for(Node item : XMLParserHelper.getChildNodesWithName(attr, "Item")){
                    s_attrs.add(XMLParserHelper.getAttributeValue(item, "Value"));
                  }
                }
                for(Node attr : XMLParserHelper.getChildNodesWithMatchingAttributeAndName(va, "AttrArray", "Name", "DerivedAttrNames")){
                  for(Node item : XMLParserHelper.getChildNodesWithName(attr, "Item")){
                    s_attrs.add(XMLParserHelper.getAttributeValue(item, "Value"));
                  }
                }
                al_listBinding.add(new ListBinding(attrs.getNamedItem("Name").getNodeValue(),
                                           attrs.getNamedItem("ListVOName").getNodeValue(),s_attrs));
            }
            
            nl1 = doc.getElementsByTagName("EntityUsage");

            for (int i = 0; i < nl1.getLength(); i++) {
                Node va = nl1.item(i);
                NamedNodeMap attrs = va.getAttributes();
                al_eo.add(new EntityUsage(attrs.getNamedItem("Name").getNodeValue(),
                                           attrs.getNamedItem("Entity") == null ? null : attrs.getNamedItem("Entity").getNodeValue()));
            }
            
            nl1 = doc.getElementsByTagName("ViewAttribute");
            for (int i = 0; i < nl1.getLength(); i++) {
                Node vAttrs = nl1.item(i);
                NamedNodeMap attrs = vAttrs.getAttributes();
                String name = attrs.getNamedItem("Name").getNodeValue().toLowerCase();
                NodeList children = vAttrs.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    if(children.item(j).getNodeName().toLowerCase().contains("validation")){
                        attrs = vAttrs.getAttributes();
                        s_attrsWithValidation.add(name.toLowerCase());
                        continue;
                    }
                }
                
                Node eo = attrs.getNamedItem("EntityUsage");
                if(eo != null){                 
                    Node eoAttr = attrs.getNamedItem("EntityAttrName");
                    if(eoAttr != null)                   
                        m_attrsWithEO.put(name, eo.getNodeValue() + "#" + eoAttr.getNodeValue() );
                }
            }
          
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
            
            
        } catch (Exception e) {
            System.out.println(jar);
            System.out.println(fname);
            e.printStackTrace();
        }

    }


    public Long getDefaultFetchSize() {
        return defaultFetchSize;
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
