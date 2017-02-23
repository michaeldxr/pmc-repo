package oracle.apps.votuning_v3.file_type;

import java.util.ArrayList;
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

    public boolean isIsTransientVO() {
        return isTransientVO;
    }

    static public class ListBinding {
        public String name;
        public String listVOName;
        public String listRangeSize;

        public ListBinding(String name, String listVOName,
                           String listRangeSize) {
            this.name = name;
            this.listRangeSize = listRangeSize;
            this.listVOName = listVOName;
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

        public void setListRangeSize(String listRangeSize) {
            this.listRangeSize = listRangeSize;
        }

        public String getListRangeSize() {
            return listRangeSize;
        }
    }

    static public class ViewAccessor {
        public String name;
        public String VOName;
        public String fetchSize;

        public ViewAccessor(String name, String VOName, String fetchSize) {
            this.name = name;
            this.VOName = VOName;
            this.fetchSize = fetchSize;
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

        public void setFetchSize(String fetchSize) {
            this.fetchSize = fetchSize;
        }

        public String getFetchSize() {
            return fetchSize;
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
    int numAttr;
    boolean isTransientVO = false;
    ArrayList<ViewAccessor> al_va = new ArrayList<ViewAccessor>();
    ArrayList<ListBinding> al_listBinding = new ArrayList<ListBinding>();
    ArrayList<EntityUsage> al_eo = new ArrayList<EntityUsage>();

    public ViewObject(ZipFile zf, ZipEntry ze) {
        super();

        try {
            jar = zf.getName();
            fname = ze.getName();


            XMLDocument doc =
                XMLParserHelper.getXMLDocument(zf.getInputStream(ze));
            Node root = doc.getDocumentElement();

            Node nName = root.getAttributes().getNamedItem("Name");
            name = nName.getNodeValue();

            Node nFS = root.getAttributes().getNamedItem("FetchSize");

            if (nFS != null) {
                String fs = nFS.getNodeValue();
                if (fs != null && "".equals(fs) == false) {
                    defaultFetchSize = new Long(fs);
                }
            }

            numAttr = doc.getElementsByTagName("ViewAttribute").getLength();

            NodeList nl1 = doc.getElementsByTagName("ViewAccessor");

            for (int i = 0; i < nl1.getLength(); i++) {
                Node va = nl1.item(i);
                NamedNodeMap attrs = va.getAttributes();
                al_va.add(new ViewAccessor(attrs.getNamedItem("Name").getNodeValue(),
                                           attrs.getNamedItem("ViewObjectName").getNodeValue(),
                                           attrs.getNamedItem("FetchSize") == null ? null: attrs.getNamedItem("FetchSize").getNodeValue()));
            }
            
            nl1 = doc.getElementsByTagName("ListBinding");

            for (int i = 0; i < nl1.getLength(); i++) {
                Node va = nl1.item(i);
                NamedNodeMap attrs = va.getAttributes();
                al_listBinding.add(new ListBinding(attrs.getNamedItem("Name").getNodeValue(),
                                           attrs.getNamedItem("ListVOName").getNodeValue(),
                                           attrs.getNamedItem("ListRangeSize") == null ? null : attrs.getNamedItem("ListRangeSize").getNodeValue()));
            }
            
            nl1 = doc.getElementsByTagName("EntityUsage");

            for (int i = 0; i < nl1.getLength(); i++) {
                Node va = nl1.item(i);
                NamedNodeMap attrs = va.getAttributes();
                al_eo.add(new EntityUsage(attrs.getNamedItem("Name").getNodeValue(),
                                           attrs.getNamedItem("Entity") == null ? null : attrs.getNamedItem("Entity").getNodeValue()));
            }
            
            if(doc.getElementsByTagName("SQLQuery").getLength() == 0){
                isTransientVO = true;
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

    public int getNumAttr() {
        return numAttr;
    }
}
