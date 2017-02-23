package oracle.apps.helpers;

import java.io.InputStream;
import java.io.StringWriter;

import java.util.ArrayList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import javax.xml.transform.stream.StreamResult;

import oracle.apps.NullEntityResolver;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLParserHelper {
    boolean bDebug = false;

    public XMLParserHelper() {
        super();
    }


    public static XMLDocument getXMLDocument(String absFilePath) {
        DOMParser parser = null;
        try {
            parser = initializeDomParser();
            parser.parse("file:" + absFilePath);
        } catch (Exception ex) {
            System.out.println("[Error : Skipping] Error while initializing dom parser for file: " + absFilePath);

            //ex.printStackTrace();


            return null;
        }

        XMLDocument doc = parser.getDocument();
        return doc;
    }
    
    public static XMLDocument getXMLDocument(String absFilePath, boolean debug) {
        DOMParser parser = null;
        try {
            parser = initializeDomParser();
            parser.parse("file:" + absFilePath);
        } catch (Exception ex) {
            System.out.println("[Error : Skipping] Error while initializing dom parser for file: " + absFilePath);
            if(debug){
                ex.printStackTrace();
            }


            return null;
        }

        XMLDocument doc = parser.getDocument();
        return doc;
    }

   

    public static XMLDocument getXMLDocument(InputStream inputStream) {
        DOMParser parser = null;
        try {
            parser = initializeDomParser();
            parser.parse(inputStream);
        } catch (Exception ex) {
            System.out.println("[Error] Error while initializing dom parser for file: ");
            ex.printStackTrace();
            System.out.println("[Error] Skipping this file.. ");
            return null;
        }

        XMLDocument doc = parser.getDocument();
        return doc;
    }
    public static XMLDocument getXMLDocument(InputStream inputStream,boolean debug) {
        DOMParser parser = null;
        try {
            parser = initializeDomParser();
            parser.parse(inputStream);
        } catch (Exception ex) {
            System.out.println("[Error] Error while initializing dom parser .. Skipping ");
            if(debug)
                ex.printStackTrace();
            
            return null;
        }

        XMLDocument doc = parser.getDocument();
        return doc;
    }

    public static DOMParser initializeDomParser() throws Exception {

        DOMParser parser = new DOMParser();
        parser.setDebugMode(true);
        parser.setErrorStream(System.out);
        parser.setEntityResolver(new NullEntityResolver());
        parser.showWarnings(true);
        parser.setValidationMode(DOMParser.NONVALIDATING);

        return parser;
    }

    public static boolean hasOneNOnlyOneNode(NodeList nlist) {
        if (nlist == null) {
            return false;
        }
        if (nlist.getLength() == 1) {
            Node n = nlist.item(0);
            if (n == null) {
                return false;
            }
            return true;
        }
        return false;
    }

    public static Node getChildNodeWithMatchingAttribute(Node node, String attribute, String attrValue) {
        if (node == null || attrValue == null) {
            return null;
        }
        NodeList nlChildren = node.getChildNodes();
        int len = nlChildren.getLength();
        for (int i = 0; i < len; i++) {
            Node child = nlChildren.item(i);
            String idVal = getAttributeValue(child, attribute);
            if (idVal != null && idVal.trim().equals(attrValue)) {
                return child;
            }
        }
        return null;
    }

    

    public static Node getChildNodeWithMatchingAttributeAndName(Node node, String nodeName, String attribute, String attrValue) {
        if (node == null || attrValue == null) {
            return null;
        }
        NodeList nlChildren = node.getChildNodes();
        if (nlChildren == null || nlChildren.getLength() <= 0)
            return null;

        int len = nlChildren.getLength();
        for (int i = 0; i < len; i++) {
            Node child = nlChildren.item(i);
            String childNodeName = child.getNodeName();
            if (!isEmpty(childNodeName) && childNodeName.trim().equals(nodeName)) {
                String idVal = getAttributeValue(child, attribute);
                if (idVal != null && idVal.trim().equals(attrValue)) {
                    return child;
                }
            }

        }
        return null;
    }
    
    
    
    public static ArrayList<Node> getChildNodesWithMatchingAttributeAndName(Node node, String nodeName, String attribute, String attrValue) {
        
        ArrayList<Node> foundNodes = new ArrayList<Node>();
        
        if (node == null || attrValue == null) {
            return foundNodes;
        }
        NodeList nlChildren = node.getChildNodes();
        if (nlChildren == null || nlChildren.getLength() <= 0)
            return foundNodes; 

        int len = nlChildren.getLength();
        for (int i = 0; i < len; i++) {
            Node child = nlChildren.item(i);
            String childNodeName = child.getNodeName();
            if (!isEmpty(childNodeName) && childNodeName.trim().equals(nodeName)) {
                String idVal = getAttributeValue(child, attribute);
                if (idVal != null && idVal.trim().equals(attrValue))
                    foundNodes.add(child);                
            }
        }
        return foundNodes;
    }

    public static ArrayList<Node> getChildNodesWithNotMatchingAttributeAndMatchingName(Node node, String nodeName, String attribute, String attrValue, String defaultAttrValue) {
        
        //usage: return all child nodes whose attibute not equals to attrValue. if a child node doesn't have this attibute, it will also be considered a found node.
        //if the parent node doesn't exist or no child nodes found, an empty arrylist will be returned
        //if defaultAttrValue is not specified, node with missing attribute will be considered a found node
        ArrayList<Node> foundNodes = new ArrayList<Node>();
        
        if (node == null || attrValue == null) {
            return foundNodes;
        }
        NodeList nlChildren = node.getChildNodes();
        if (nlChildren == null || nlChildren.getLength() <= 0)
            return foundNodes; 

        int len = nlChildren.getLength();
        for (int i = 0; i < len; i++) {
            Node child = nlChildren.item(i);
            String childNodeName = child.getNodeName();
            if (!XMLParserHelper.isEmpty(childNodeName) && childNodeName.trim().equals(nodeName)) {
                String idVal = XMLParserHelper.getAttributeValue(child, attribute);
              if (defaultAttrValue != null && idVal == null) 
                idVal = defaultAttrValue; 
              if (defaultAttrValue == null && idVal == null || !idVal.trim().equals(attrValue))
                foundNodes.add(child);                
            }
        }
        return foundNodes;
    }

    public static ArrayList<Node> getChildNodesWithNotMatchingAttributeAndMatchingName(Node node, String nodeName, String attribute, String attrValue){
      //if defaultAttrValue is not specified
      return getChildNodesWithNotMatchingAttributeAndMatchingName(node, nodeName, attribute, attrValue, null);
    }

    public static Node getChildNodeWithName(Node node, String nodeName) {
        if (node == null) {
            return null;
        }
        NodeList nlChildren = node.getChildNodes();
        int len = nlChildren.getLength();
        for (int i = 0; i < len; i++) {
            Node child = nlChildren.item(i);
            String childNodeName = child.getNodeName();
            if (!isEmpty(childNodeName) && childNodeName.trim().equals(nodeName)) {
                return child;
            }

        }
        return null;
    }

    public static ArrayList<Node> getChildNodesWithName(Node node, String nodeName) {
        if (node == null) {
            return null;
        }
        ArrayList<Node> foundNodes = new ArrayList<Node>();

        NodeList nlChildren = node.getChildNodes();
        int len = nlChildren.getLength();
        for (int i = 0; i < len; i++) {
            Node child = nlChildren.item(i);
            String childNodeName = child.getNodeName();
            if (!isEmpty(childNodeName) && childNodeName.trim().equals(nodeName)) {
                foundNodes.add(child);
            }

        }
        return foundNodes;
    }

    public static Node getParentNodeWithName(Node node, String nodeName) {
        if (node == null) {
            return null;
        }

        Node parent = node.getParentNode();        
        while (parent != null && !parent.getNodeName().equals(nodeName))
            parent = parent.getParentNode();

        return parent;
    }

    public static Node getNodesInListWithMatchingAttribute(NodeList nodelist, String attribute, String attrValue) {
        if (nodelist == null || nodelist.getLength() <= 0) {
            return null;
        }

        int len = nodelist.getLength();
        for (int i = 0; i < len; i++) {
            Node child = nodelist.item(i);
            String idVal = getAttributeValue(child, attribute);
            if (idVal != null && idVal.trim().equals(attrValue)) {
                return child;
            }
        }
        return null;
    }

    

    public static String getAttributeValue(Node node, String attributeName) {

        if (node == null) {
            return null;
        }
        NamedNodeMap attributes = node.getAttributes();
        if (attributes == null) {
            return null;
        }
        Node attrNode = attributes.getNamedItem(attributeName);
        if (attrNode == null) {
            return null;
        }
        return attrNode.getNodeValue();
    }

    public static boolean isNodeName(Node node, String nodeNameToCheck) {
        if (node == null) {
            return false;
        }
        String nodeName = node.getNodeName();
        if (isEmpty(nodeName)) {
            return false;
        }
        if (nodeName.trim().equals(nodeNameToCheck)) {
            return true;
        }
        return false;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public static String getNodeAsString(Node node, String indent) {
        StringWriter sw = new StringWriter();
        switch (node.getNodeType()) {

        case Node.DOCUMENT_NODE:
            String str = "<xml version=\"1.0\">\n";
            sw.append(str);


            break;

        case Node.ELEMENT_NODE:
            String name = node.getNodeName();
            sw.append(indent + "<" + name);
            NamedNodeMap attributes = node.getAttributes();

            for (int i = 0; i < attributes.getLength(); i++) {
                Node current = attributes.item(i);
                sw.append(" " + current.getNodeName() + "=\"" + current.getNodeValue() + "\"");
            }
            sw.append(">");


            break;

        case Node.TEXT_NODE:
            sw.append(node.getNodeValue());
            break;
        }

        return sw.toString();

    }

    public static String getNodeAsStringWithCloseTag(Node node, String indent) {
        StringWriter sw = new StringWriter();
        switch (node.getNodeType()) {

        case Node.DOCUMENT_NODE:
            String str = "<xml version=\"1.0\">\n";
            sw.append(str);


            break;

        case Node.ELEMENT_NODE:
            String name = node.getNodeName();
            sw.append(indent + "<" + name);
            NamedNodeMap attributes = node.getAttributes();

            for (int i = 0; i < attributes.getLength(); i++) {
                Node current = attributes.item(i);
                sw.append(" " + current.getNodeName() + "=\"" + current.getNodeValue() + "\"");
            }
            sw.append("/>");


            break;

        case Node.TEXT_NODE:
            sw.append(node.getNodeValue());
            break;
        }

        return sw.toString();

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

    public static String getStringFromNode(Node node)    {
        if(node==null){
            return null;
        }        
        try{
                DOMSource domSource = new DOMSource(node);
                StringWriter writer = new StringWriter();
                StreamResult result = new StreamResult(writer);
                TransformerFactory tf = TransformerFactory.newInstance();        
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.transform(domSource, result);
                writer.flush();
                return writer.toString();
        }catch(TransformerException e){
                e.printStackTrace();
                return null;
        }
    } 

    public void setBDebug(boolean bDebug) {
        this.bDebug = bDebug;
    }

    public boolean isBDebug() {
        return bDebug;
    }
}
