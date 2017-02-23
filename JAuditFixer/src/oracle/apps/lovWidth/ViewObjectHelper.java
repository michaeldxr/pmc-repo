package oracle.apps.lovWidth;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * this class is used by the combobox width scripts. 
 * It helps perform all viewObject operations like getting the ViewObjectXml node, getting a VO Attribute,
 * getting the VODisplayWidth etc.
 */
public class ViewObjectHelper {
    String m_voFilePath = "";
    XMLDocument voXml = null;
    Node m_viewObjectNode = null;
    private static boolean m_bDebug = false;

    public ViewObjectHelper(String voFilePath) {
        super();
        m_voFilePath = voFilePath;
        voXml = XMLParserHelper.getXMLDocument(m_voFilePath);
    }

    public Node getViewObjectNode() {
        if (m_viewObjectNode == null) {
            NodeList listViewObj = voXml.getElementsByTagName("ViewObject");
            if (listViewObj == null)
                return null;
            m_viewObjectNode = listViewObj.item(0);
        }
        return m_viewObjectNode;
    }

    public static Node getPSRootNode(XMLDocument psXml) {

        NodeList listViewObj = psXml.getElementsByTagName("Domain");
        if (listViewObj == null)
            return null;
        return listViewObj.item(0);


    }

    public static Node getViewObjectNodeFromxml(XMLDocument voXml) {

        NodeList listViewObj = voXml.getElementsByTagName("ViewObject");
        if (listViewObj == null)
            return null;
        return listViewObj.item(0);

    }


    public static String getPropertySetForAttr(Node attrNode) {
        String ps = "";
        if (attrNode == null)
            return ps;

        ps = XMLParserHelper.getAttributeValue(attrNode, "Domain");
        if (isEmpty(ps))
            return "";
        return ps;
    }

    public static boolean isViewObjectXml(File f) {
        String absPath = f.getAbsolutePath();
        return isViewObjectXml(absPath);
    }


    public static ArrayList<Node> getAllAttributesInVOXMl(XMLDocument voXml) {
        ArrayList<Node> voAttributes = new ArrayList<Node>();
        NodeList viewAtrNodes = voXml.getElementsByTagName("ViewAttribute");
        if (viewAtrNodes == null)
            return voAttributes;
        int len = viewAtrNodes.getLength();
        for (int i = 0; i < len; i++) {
            Node n = viewAtrNodes.item(i);
            if (XMLParserHelper.isNodeName(n, "ViewAttribute")) {
                voAttributes.add(n);
            }
        }
        return voAttributes;
    }
    public  ArrayList<Node> getAllAttributesInVOXMl() {
        return getAllAttributesInVOXMl(voXml);
    }
    public static String getVONameFromPackage(String packageName) {
        if (isEmpty(packageName))
            return "";
        int lastDot = packageName.lastIndexOf(".");
        if (lastDot != -1)
            return packageName.substring(lastDot + 1);
        return "";
    }

    public static String getVONameFromFilePath(String filePath) {
        if (isEmpty(filePath))
            return "";
        int lastDot = filePath.lastIndexOf("/");
        if (lastDot != -1)
            return filePath.substring(lastDot + 1);
        return "";
    }

    public static Node getAttributeByName(Node viewObjectNode, String attributename) {
        if (viewObjectNode == null)
            return null;
        Node attributeNode =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(viewObjectNode, "ViewAttribute", "Name", attributename);
        return attributeNode;
    }

    public static boolean isViewObjectXml(String absFilePath) {
        if (isEmpty(absFilePath))
            return false;
        if (!absFilePath.endsWith(".xml"))
            return false;
        XMLDocument fileXml = XMLParserHelper.getXMLDocument(absFilePath);
        if (fileXml == null)
            return false;
        NodeList listViewObj = fileXml.getElementsByTagName("ViewObject");
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
            return false;
        } else
            return true;
    }

    public static boolean isPropertySetXml(String absFilePath) {
        if (isEmpty(absFilePath))
            return false;
        if (!absFilePath.endsWith(".xml"))
            return false;
        XMLDocument fileXml = XMLParserHelper.getXMLDocument(absFilePath);
        if (fileXml == null)
            return false;
        NodeList listViewObj = fileXml.getElementsByTagName("Domain");
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
            return false;
        } else
            return true;
    }

    public static boolean isAMXml(String absFilePath) {
        if (isEmpty(absFilePath))
            return false;
        if (!absFilePath.endsWith(".xml"))
            return false;
        XMLDocument fileXml = XMLParserHelper.getXMLDocument(absFilePath);
        if (fileXml == null)
            return false;
        NodeList amObjs = fileXml.getElementsByTagName("AppModule");
        if (amObjs != null && amObjs.getLength() > 0)
            return true;
        return false;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    public boolean doesViewAttrHaveLOV(Node viewAttr) {
        if (!XMLParserHelper.isNodeName(viewAttr, "ViewAttribute")) {
            return false;
        }
        String lovName = XMLParserHelper.getAttributeValue(viewAttr, "LOVName");
        if (!isEmpty(lovName))
            return true;
        return false;
    }

    public boolean doesViewAttrHaveComboboxLOV(Node viewAttr) {
        if (doesViewAttrHaveLOV(viewAttr)) {
            Node attrProperties = getPropertiesNode(viewAttr);
            if (attrProperties == null)
                return false;
            Node schemaBasedProperties = XMLParserHelper.getChildNodeWithName(attrProperties, "SchemaBasedProperties");
            Node controlType = XMLParserHelper.getChildNodeWithName(schemaBasedProperties, "CONTROLTYPE");
            if (controlType == null)
                return false;
            String control = XMLParserHelper.getAttributeValue(controlType, "Value");
            if (!isEmpty(control) && control.trim().equals("combo_lov"))
                return true;
        }
        return false;
    }

    public boolean isExpertModeVO() {
        Node viewObjectNode = getViewObjectNode();
        return isExpertModeVO(viewObjectNode);

    }

    public static String getAttributePrecision(Node attrNode) {
        if (attrNode == null)
            return "";

        String displaySize = XMLParserHelper.getAttributeValue(attrNode, "Precision");
        if (isEmpty(displaySize))
            return "";
        return displaySize;

    }

    public static String getAttrColumnWidth(Node attrNode) {
        if (attrNode == null)
            return "";
        Node designTimeNode = XMLParserHelper.getChildNodeWithName(attrNode, "DesignTime");
        if (designTimeNode == null)
            return "";
        Node displaySizeAttrNode =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(designTimeNode, "Attr", "Name", "_DisplaySize");
        if (displaySizeAttrNode == null)
            return "";
        String displaySize = XMLParserHelper.getAttributeValue(displaySizeAttrNode, "Value");
        if (isEmpty(displaySize))
            return "";
        return displaySize;

    }

    public static boolean isExpertModeVO(Node viewObjectNode) {

        if (viewObjectNode == null)
            return false;
        Node designTimenode = XMLParserHelper.getChildNodeWithName(viewObjectNode, "DesignTime");
        if (designTimenode == null)
            return false;
        Node isExpertNode =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(designTimenode, "Attr", "Name", "_isExpertMode");
        if (isExpertNode == null)
            return false;
        String val = XMLParserHelper.getAttributeValue(isExpertNode, "Value");
        if (!isEmpty(val) && val.trim().equals("true"))
            return true;
        return false;

    }

    public static String getDisplayWidthFromAttrNode(Node viewAttr) {
        Node attrProperties = getPropertiesNode(viewAttr);
        if (attrProperties == null)
            return "";
        Node schemaBasedProperties = XMLParserHelper.getChildNodeWithName(attrProperties, "SchemaBasedProperties");
        if (schemaBasedProperties == null)
            return "";
        Node widthNode = XMLParserHelper.getChildNodeWithName(schemaBasedProperties, "DISPLAYWIDTH");
        if (widthNode == null)
            return "";
        String widthValue = XMLParserHelper.getAttributeValue(widthNode, "Value");
        return widthValue;
    }
    
  public static String getControlTypeFromAttrNode(Node viewAttr) {
        Node attrProperties = getPropertiesNode(viewAttr);
        if (attrProperties == null)
            return "";
        Node schemaBasedProperties = XMLParserHelper.getChildNodeWithName(attrProperties, "SchemaBasedProperties");
        if (schemaBasedProperties == null)
            return "";
        Node widthNode = XMLParserHelper.getChildNodeWithName(schemaBasedProperties, "CONTROLTYPE");
        if (widthNode == null)
            return "";
        String propertyValue = XMLParserHelper.getAttributeValue(widthNode, "Value");
        return propertyValue;
    }
      

    public static Node getPropertiesNode(Node n) {
        if (n == null)
            return null;
        return XMLParserHelper.getChildNodeWithName(n, "Properties");
    }

    public Node getListBindingWithName(String name) {
        Node viewObjectNode = getViewObjectNode();
        if (viewObjectNode == null)
            return null;
        Node listBindingNodeObt =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(viewObjectNode, "ListBinding", "Name", name);
        return listBindingNodeObt;
    }

    public static Node getListBindingWithName(XMLDocument voXml, String name) {
        Node viewObjectNode = getViewObjectNodeFromxml(voXml);
        if (viewObjectNode == null)
            return null;
        Node listBindingNodeObt =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(viewObjectNode, "ListBinding", "Name", name);
        return listBindingNodeObt;
    }

    public Node getViewAccessorWithName(String name) {
        Node viewObjectNode = getViewObjectNode();
        if (viewObjectNode == null)
            return null;
        Node vaNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(viewObjectNode, "ViewAccessor", "Name", name);
        return vaNode;
    }

    public static Node getViewAccessorWithName(Node voNode, String name) {

        Node vaNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(voNode, "ViewAccessor", "Name", name);
        return vaNode;
    }

    public static Long getLongFromString(String str) {

        if (isEmpty(str))
            return 0L;
        try {
            return Long.parseLong(str);
        } catch (Exception ex) {
            ex.printStackTrace();
            ;
        }


        return 0L;
    }

    public static ArrayList<String> getListBindingDisplayAttrNames(Node listBindingNode) {
        ArrayList<String> displayAttrs = new ArrayList<String>();
        String displayAttrCnt = XMLParserHelper.getAttributeValue(listBindingNode, "ComboAttributeCount");
        Long longAttrCnt = getLongFromString(displayAttrCnt);

        Node displayAttrNode =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(listBindingNode, "AttrArray", "Name", "ListDisplayAttrNames");
        if (displayAttrNode == null)
            return displayAttrs;
        NodeList displayAttrChildren = displayAttrNode.getChildNodes();
        int len = displayAttrChildren.getLength();
        int numAttrsAdded = 0;
        for (int i = 0; i < len; i++) {
            if (longAttrCnt > 0) {
                if (numAttrsAdded >= longAttrCnt)
                    break;
            }
            Node child = displayAttrChildren.item(i);
            if (XMLParserHelper.isNodeName(child, "Item")) {
                String displayAttr = XMLParserHelper.getAttributeValue(child, "Value");
                if (!isEmpty(displayAttr)) {
                    displayAttrs.add(displayAttr);
                    numAttrsAdded++;
                }
            }
        }
        return displayAttrs;

    }


    public static XMLDocument findBCFromSetOfJarFiles(HashSet<String> jarFiles, String packageToFind) {
        XMLDocument bcXml = null;
        if (jarFiles == null || jarFiles.size() <= 0)
            return bcXml;

        String bcPathToFind = packageToFind.replaceAll("\\.", "/");
        bcPathToFind = bcPathToFind + ".xml";
        Iterator<String> libraryIter = jarFiles.iterator();
        while (libraryIter.hasNext()) {
            String lib = libraryIter.next();
            JarFile jar = null;
            try {
                jar = new JarFile(lib);

                if (jar != null) {

                    JarEntry voEntry = jar.getJarEntry(bcPathToFind);

                    if (voEntry != null) {
                        if (m_bDebug) {
                            System.out.println("Found the vo in: " + jar.getName());
                        }

                        InputStream contents = jar.getInputStream(voEntry);
                        bcXml = XMLParserHelper.getXMLDocument(contents);
                        if (contents != null) {
                            contents.close();
                        }
                        break;
                    }
                }
            } catch (IOException ex) {
                String msg = "[ERROR]:getViewObject: IOException: " + ex.getMessage() + " lib file: " + lib;
                if (m_bDebug)
                    System.out.println(msg);

                continue;
            }
        }
        return bcXml;

    }

    public static String getPackage(String absPath) {

        int extIndex = absPath.lastIndexOf(".");
        if (extIndex != -1) {
            absPath = absPath.substring(0, extIndex);
        }
        int indexOfSrc = absPath.indexOf("/src/");
        if (indexOfSrc == -1)
            return absPath;
        String noSrcPath = absPath.substring(indexOfSrc + "/src/".length());
        String packagePath = noSrcPath.replaceAll("/", ".");


        return packagePath;


    }
  public static Node getDisplayWidthNodeForAttribute(Node attrNode) {
      Node attrProperties = getPropertiesNode(attrNode);
      if (attrProperties == null)
          return null;
      Node schemaBasedProperties = XMLParserHelper.getChildNodeWithName(attrProperties, "SchemaBasedProperties");
      if (schemaBasedProperties == null)
          return null;
      Node widthNode = XMLParserHelper.getChildNodeWithName(schemaBasedProperties, "DISPLAYWIDTH");
      return widthNode;
  }
    public static String getEntityAttrNameForVOAttribute(Node attributeNode) {
        return XMLParserHelper.getAttributeValue(attributeNode, "EntityAttrName");
    }

    public static Node getEntityUsageNodeWithName(Node viewObjectNode, String usageName) {
        Node entityUsageNode =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(viewObjectNode, "EntityUsage", "Name", usageName);
        return entityUsageNode;
    }

    public static Node getEntityUsageNodeForVOAttributeNode(Node viewObjectNode, Node attributeNode) {
        String entityUsage = XMLParserHelper.getAttributeValue(attributeNode, "EntityUsage");
        if (isEmpty(entityUsage))
            return null;
        return getEntityUsageNodeWithName(viewObjectNode, entityUsage);
    }

    public static boolean isExpertModeAttribute(Node attributeNode) {
        if (attributeNode == null)
            return false;
        String entityAttrName = getEntityAttrNameForVOAttribute(attributeNode);
        if (isEmpty(entityAttrName))
            return true;
        return false;
    }
    
    public static String getFetchSize(String path) throws Exception
    {
        if(isEmpty(path))
            throw new Exception("viewObjectHelper:getFetchSize . Path cannot be empty");
        XMLDocument viewObjectxml =  XMLParserHelper.getXMLDocument(path);
        if(viewObjectxml==null)
            throw new Exception("viewObjectHelper:getFetchSize: could not get view object xml: "+ path);
        return getFetchSize(viewObjectxml);
    }
    public static String getFetchSize(XMLDocument xml) throws Exception
    {
            if(xml==null)
                throw new Exception("vo xml was null");
            
            Node viewObjectNode = getViewObjectNodeFromxml(xml);
            if(viewObjectNode==null)
            {
                    throw new Exception("viewObjectHelper:getFetchSize: Not a valid view object: " );
            }
            return XMLParserHelper.getAttributeValue(viewObjectNode, "FetchSize");
        }
    public static boolean doesVOExtendAnother(XMLDocument xml)
    {
        String extendedVoDef = getExtendedVOPackage(xml);
        if(isEmpty(extendedVoDef))
            return false;
        return true;
    }
    public static String getExtendedVOPackage(XMLDocument xml)
    {
        if(xml==null)    
            return "";
        Node voNode = getViewObjectNodeFromxml(xml);
        if(voNode==null)
            return "";
        return XMLParserHelper.getAttributeValue(voNode, "Extends");
        
    }
    public static String getTransientExpressionForAttribute(Node attributeNode)
    {
        String transientExpression = "";
        if(attributeNode==null)
            return transientExpression;
        return getCDataStringFromNode(attributeNode,"TransientExpression");
       
        }
    
    
    public static String getCDataStringFromNode(Node parentNode, String childTagName) {
        Node childCDataNode = XMLParserHelper.getChildNodeWithName(parentNode, childTagName);
        if (childCDataNode == null)
            return "";
        NodeList nl = childCDataNode.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n instanceof CharacterData) {
                return ((CharacterData)n).getData();
            }
        }
        return "";
    }
    
    /**
     * Gets the line number of the end of the start tag in the source file
     * @param componentNode
     * componentNode whose line number is to be found
     * @return
     * returns the line number in the source file
     */
    public static Integer getLineNumberInSourceFile(Node componentNode) {
        if (componentNode instanceof XMLElement) {
            XMLElement xmlComponentNode = (XMLElement)componentNode;
            Integer lineNum = xmlComponentNode.getLineNumber();
            return lineNum;
        }

        return 0;
    }
    
}
