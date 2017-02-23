package oracle.apps.lovWidth;

import java.io.File;

import java.util.ArrayList;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is used by the ComboboxWidth Scripts. 
 */
public class EntityObjectHelper {
    String m_eoFilePath = "";
    XMLDocument eoXml = null;
    Node m_eoNode = null;
    private static boolean m_bDebug = false;
    
    public EntityObjectHelper() {
        super();
    }
    public EntityObjectHelper(String eoFilePath) {
        super();
        m_eoFilePath = eoFilePath;
        eoXml = XMLParserHelper.getXMLDocument(eoFilePath);
    }
    
  
    
    public static Node getEntityObjectNode(XMLDocument eoXml) {
        NodeList listViewObj = eoXml.getElementsByTagName("Entity");
        if (listViewObj == null)
            return null;
        return listViewObj.item(0);
    }

    public static String getAttrColumnWidth(Node attrNode) {
        return ViewObjectHelper.getAttrColumnWidth(attrNode);

    }

    public static Node getViewAccessorWithName(Node eoNode, String name) {
        Node vaNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(eoNode, "ViewAccessor", "Name", name);
        return vaNode;
    }

    public static boolean isEntityObjectXml(File f) {
        String absPath = f.getAbsolutePath();
        return isEntityObjectXml(absPath);
    }

    public static boolean isEntityObjectXml(String absFilePath) {
        if (isEmpty(absFilePath))
            return false;
        if (!absFilePath.endsWith(".xml"))
            return false;
        XMLDocument fileXml = XMLParserHelper.getXMLDocument(absFilePath);
        if (fileXml == null)
            return false;
        NodeList listViewObj = fileXml.getElementsByTagName("Entity");
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
            return false;
        } else
            return true;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    public static String getDisplayWidthFromAttrNode(Node viewAttr) {
        return ViewObjectHelper.getDisplayWidthFromAttrNode(viewAttr);

    }

    public static Node getAttributeByName(Node eoNode, String attributename) {
        if (eoNode == null)
            return null;
        Node attributeNode =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(eoNode, "Attribute", "Name", attributename);
        return attributeNode;
    }

    public static String getEONameFromPackage(String packageName) {
        if (isEmpty(packageName))
            return "";
        int lastDot = packageName.lastIndexOf(".");
        if (lastDot != -1)
            return packageName.substring(lastDot + 1);
        return "";
    }
    
    public static ArrayList<Node> getAllAttributesInEOXMl(XMLDocument voXml) {
        ArrayList<Node> voAttributes = new ArrayList<Node>();
        NodeList viewAtrNodes = voXml.getElementsByTagName("Attribute");
        if (viewAtrNodes == null)
            return voAttributes;
        int len = viewAtrNodes.getLength();
        for (int i = 0; i < len; i++) {
            Node n = viewAtrNodes.item(i);
            if (XMLParserHelper.isNodeName(n, "Attribute")) {
                voAttributes.add(n);
            }
        }
        return voAttributes;
    }
    public  ArrayList<Node> getAllAttributesInEOXMl() {
        return getAllAttributesInEOXMl(eoXml);
    }
    
}
