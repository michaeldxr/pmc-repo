package oracle.apps.votuning_v4.file_type;

import java.util.ArrayList;
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

    String jar;
    String fname;
    String name;
    ArrayList<ViewAccessor> al_va = new ArrayList<ViewAccessor>();

    public EntityObject(ZipFile zf, ZipEntry ze) {
        super();

        try {
            jar = zf.getName();
            fname = ze.getName();


            XMLDocument doc =
                XMLParserHelper.getXMLDocument(zf.getInputStream(ze));
            Node root = doc.getDocumentElement();

            Node nName = root.getAttributes().getNamedItem("Name");
            name = nName.getNodeValue();

            NodeList nl1 = doc.getElementsByTagName("ViewAccessor");

            for (int i = 0; i < nl1.getLength(); i++) {
                Node va = nl1.item(i);
                NamedNodeMap attrs = va.getAttributes();
                al_va.add(new ViewAccessor(attrs.getNamedItem("Name").getNodeValue(),
                                           attrs.getNamedItem("ViewObjectName").getNodeValue(),
                                           attrs.getNamedItem("FetchSize") == null ? null: attrs.getNamedItem("FetchSize").getNodeValue()));
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
