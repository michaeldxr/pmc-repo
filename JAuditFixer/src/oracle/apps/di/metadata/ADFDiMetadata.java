package oracle.apps.di.metadata;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.HashSet;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


import oracle.apps.di.checker.CheckerResult;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Document;

import org.w3c.dom.Element;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;

public abstract class ADFDiMetadata {
    // ======= STATIC MEMBERS ========================================
    public static String META_WORKBOOK = "Workbook";
    public static String META_WORKSHEET = "Worksheet";
    public static String META_CELLINPUTTEXT = "CellInputText";
    public static String META_BUTTON = "Button";
    public static String META_CELLLISTOFVALUES = "CellListOfValues";
    public static String META_CELLLABEL = "CellLabel";
    public static String META_CELLOUTPUTTEXT = "CellOutputText";
    public static String META_TABLE = "Table";
    public static String META_RONTREETABLE = "ReadOnlyTreeTable";
    public static String META_ACTIONSET = "ActionSet";
    static Pattern atleastOneCharacterPtrn = Pattern.compile("[a-zA-Z]");
    public static HashSet<String> metadataSet = new HashSet<String>(10);
    static {
        metadataSet.add(META_WORKBOOK);
        metadataSet.add(META_WORKSHEET);
        metadataSet.add(META_CELLINPUTTEXT);
        metadataSet.add(META_BUTTON);
        metadataSet.add(META_CELLLISTOFVALUES);
        metadataSet.add(META_CELLLABEL);
        metadataSet.add(META_CELLOUTPUTTEXT);
        metadataSet.add(META_TABLE);
        //metadataSet.add(META_RONTREETABLE);
    }
    // ===========================================================================

    protected String metadataType = "";

    public ADFDiMetadata(String type) {
        super();
        metadataType = type;
    }

    public static ADFDiMetadata getMetadata(String metadataXml) throws Exception {
        String metadataProcXml = "<?xml version = '1.0' encoding = 'UTF-8'?>\n" +
            metadataXml;
        XMLDocument xmlDoc = getDomForString(metadataProcXml);
        if (xmlDoc == null)
            throw new Exception("could not parse metadata string: " + metadataXml);
        xmlDoc.setProperty("SelectionLanguage", "XPath");
        Element elem = xmlDoc.getDocumentElement();
        String tagName = elem.getNodeName();
        if (tagName == null || tagName.trim().equals(""))
            return null;
        tagName = tagName.trim();
        if (tagName.equals(META_WORKBOOK)) {
            return WorkbookMetadata.getMetadata(xmlDoc, elem);
        } else if (tagName.equals(META_WORKSHEET)) {
            return Worksheet.getMetadata(xmlDoc, elem);
        } else if (tagName.equals(META_TABLE)) {
            return TableMetadata.getMetadata(xmlDoc, elem);
        } else if (tagName.equals(META_RONTREETABLE)) {
            return null;
            //            return TableMetadata.getMetadata(xmlDoc, elem);
        } else if (tagName.equals(META_BUTTON)) {
            return ButtonMetadata.getMetadata(xmlDoc, elem);
        } else {
            return ADFDiComponentMetadata.getMetadata(xmlDoc, elem);
        }


    }

    public static String getAttributeOfType(Node node, String attributeLike) {
        if (node == null)
            return "";

        NamedNodeMap attrs = node.getAttributes();

        if (attrs != null && attrs.getLength() > 0) {
            int attrLen = attrs.getLength();
            for (int j = 0; j < attrLen; j++) {
                Node attrNode = attrs.item(j);
                String xmlAttrName = attrNode.getNodeName();
                if (xmlAttrName != null && !xmlAttrName.trim().equals("")) {
                    if (xmlAttrName.contains(attributeLike)) {
                        return attrNode.getNodeValue();
                    }
                }
            }
        }
        return "";
    }

    public abstract ArrayList<CheckerResult> runLocalizationCheck();

    public static XMLDocument getDomForString(String xmlString) throws Exception {

        InputStream inStream = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
        return XMLParserHelper.getXMLDocument(inStream, false);
        //         DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        //
        //         DocumentBuilder builder = builderFactory.newDocumentBuilder();
        //         InputSource inStream = new InputSource();
        //         inStream.setCharacterStream(new StringReader(xmlString));

        //return builder.parse(inStream);
    }

    public static boolean isStringDIMetadata(String str) {
        if (metadataSet.contains(str))
            return true;
        return false;
    }

    public static boolean isLocalizedString(String str, boolean validateExistence) {
        boolean localized = isStringBasicLocalized(str);
        //        if (str == null || str.trim().equals(""))
        //            return true;
        //        str = str.trim();
        //        if (isStringDIMetadata(str))
        //            return true;
        //        if(str.equals("ComponentXml"))
        //            return true;
        //        while (str.startsWith("*"))
        //            str = str.substring(1).trim();
        //
        //        if (str.startsWith("#"))
        //            localized = true;
        //        if (str.startsWith("=") || str.startsWith("["))
        //            localized = true;
        if (!localized) {
            str = removeDigitsInString(str);
            localized = isStringBasicLocalized(str);
        }


        // Last Chance to check: If it does not have even one character, it is localized
        if (!localized) {
            Matcher m = atleastOneCharacterPtrn.matcher(str);
            if (m.find()) {
            } else {
                localized = true;
            }
        }

        if (localized && validateExistence) {
            System.out.println("TODO: Validate existence.. Not a supported option");
        }
        return localized;
    }

    private static boolean isStringBasicLocalized(String str) {
        boolean localized = false;
        if (str == null || str.trim().equals(""))
            return true;
        str = str.trim();
        if (isStringDIMetadata(str))
            return true;
        if (str.equals("ComponentXml"))
            return true;
        while (str.startsWith("*"))
            str = str.substring(1).trim();

        if (str.startsWith("#"))
            localized = true;
        if (str.startsWith("=") || str.startsWith("["))
            localized = true;

        // If it does not contain any alphabet, it is localized (e.g .only digits, numbers , . , _ or special characters)
       

        return localized;
    }

    public static String removeDigitsInString(String s) {
        String result = s;
        try {
            //        String digitsRegEx="[\\.\\d+]";
            //        Pattern p = Pattern.compile(digitsRegEx);
            //        Matcher m = p.matcher(s);
            result = s.replaceAll("[\\.\\d+]", "");
            if (result == null)
                return "";
            return result.trim();
        } catch (Exception e) {
            return s;
        }
    }

    public static String getTextContentOfSelectList(NodeList nodeList) {
        if (nodeList == null || nodeList.getLength() < 1)
            return "";
        Node n = nodeList.item(0);
        if (n == null)
            return "";
        String t = n.getTextContent();
        if (t == null)
            t = "";
        else
            t = t.trim();
        return t;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    public void addResults(ArrayList<CheckerResult> results, ArrayList<CheckerResult> resultToAdd) {
        if (resultToAdd == null || resultToAdd.size() < 1)
            return;
        results.addAll(resultToAdd);
    }

    public static Node getChildNodeOrder(Node baseNode, String pathToChild) {
        Node childNode = null;
        if (isEmpty(pathToChild))
            return childNode;
        String[] childElems = pathToChild.split("/");
        if (childElems == null || childElems.length < 1)
            return childNode;
        Node startSearchNode = baseNode;
        for (int i = 0; i < childElems.length; i++) {
            String childNodeName = childElems[i];
            if (isEmpty(childNodeName))
                continue;
            childNode = XMLParserHelper.getChildNodeWithName(startSearchNode, childNodeName);
            startSearchNode = childNode;
        }
        return childNode;
    }

    public static String getTextContentOfXPathNode(Node baseNode, String pathToChild) {
        Node childNode = getChildNodeOrder(baseNode, pathToChild);
        if (childNode == null)
            return "";
        return childNode.getTextContent();
    }

    public String getMetadataType() {
        return metadataType;
    }
}
