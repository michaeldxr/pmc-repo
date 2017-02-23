package oracle.apps.di;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;

import java.io.InputStreamReader;
import java.io.StringReader;

import java.io.StringWriter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.namespace.NamespaceContext;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import oracle.apps.di.metadata.ADFDiMetadata;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;


import oracle.xml.parser.v2.NSResolver;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;


public class SharedStringsParser {
    static boolean bDebug = false;

    public SharedStringsParser() {
        super();
    }

    private static String tagNameRegEx = "<(\\w+)?[>|\\s]+?";
    private static Pattern tagNamePattern = Pattern.compile(tagNameRegEx);

    public static void main(String[] args) throws Exception {
        //
        //        String xml1 = "<Workbook><Worksheet></Worksheet></Workbook>";
        //        String xml2 = "<Workbook count=\"1\"/>";
        //
        //        String regEx = "<(\\w+)?[>|\\s]+?";
        //        Pattern ptn = Pattern.compile(regEx);
        //        Matcher m = ptn.matcher(xml2);
        //        if (m.find()) {
        //            System.out.println("Found match");
        //            System.out.println("Group count: " + m.groupCount());
        //            System.out.println("Group 0: " + m.group(0));
        //            System.out.println("Group 1: " + m.group(1));
        //        } else {
        //            System.out.println("No Match");
        //        }
        //
        String p = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP3DEV_LINUX.X64") + 
            "/fusionapps/fin/components/payables/ap/invoices/quickInvoices/di/public_html/oracle/apps/financials/payables/invoices/quickInvoices/di/excel/CorrectImportErrors.xlsx";
        ArrayList<String> list = getListOfAllStrings(p, false);
        ArrayList<String> metaSt = getListOfAllADFDiMetaStrings_New(list, p);
        printStrings(metaSt);
    }


    public static ArrayList<String> getListOfAllStrings(String spreadsheetPath, boolean doConcatenation) throws Exception {
        ArrayList<String> strings = new ArrayList<String>();
        ZipFile spreadsheetZip = new ZipFile(spreadsheetPath);
        ZipEntry sharedStringsXmlEntry = spreadsheetZip.getEntry("xl/sharedStrings.xml");
        if (sharedStringsXmlEntry == null)
            throw new Exception("Could not find any shared strings in file: " + spreadsheetPath +
                                ". Either the file is not a DI spreadshseet, or the file does not contain any shared strings");

        InputStream sharedStringsIn = spreadsheetZip.getInputStream(sharedStringsXmlEntry);
        //        XMLDocument sharedStringsXml = XMLParserHelper.getXMLDocument(sharedStringsIn);
        //        if (sharedStringsXml == null)
        //            throw new Exception("Could not read shared strings xml");
        //        sharedStringsXml.setProperty("SelectionLanguage", "XPath");

        strings = getListOfAllStrings(sharedStringsIn, doConcatenation);
        //strings = getListOfAllStrings2(sharedStringsXml);
        //
        //        if (bDebug)
        //            printStrings(strings);
        return strings;
    }


    public static String getSharedStringsContent(String spreadsheetPath) throws Exception {

        ZipFile spreadsheetZip = new ZipFile(spreadsheetPath);
        ZipEntry sharedStringsXmlEntry = spreadsheetZip.getEntry("xl/sharedStrings.xml");
        if (sharedStringsXmlEntry == null)
            throw new Exception("Could not find any shared strings in file: " + spreadsheetPath +
                                ". Either the file is not a DI spreadshseet, or the file does not contain any shared strings");

        InputStream sharedStringsIn = spreadsheetZip.getInputStream(sharedStringsXmlEntry);
        StringWriter stringWriter = new StringWriter();
        BufferedReader reader = new BufferedReader(new InputStreamReader(sharedStringsIn));
        String line = "";
        while ((line = reader.readLine()) != null) {
            stringWriter.write(line);
        }
        return stringWriter.toString();


    }

    public static String getStringTagName(String str) {
        String tgName = "";
        if (str == null)
            return tgName;

        Matcher m = tagNamePattern.matcher(str);

        if (m.find()) {
            if (m.groupCount() < 1)
                return tgName;
            tgName = m.group(1);

        }
        return tgName;
    }

    public static boolean isStringADIComponent(String str) {
        String tgName = getStringTagName(str);
        if (tgName == null || tgName.trim().equals(""))
            return false;

        if (ADFDiMetadata.isStringDIMetadata(tgName))
            return true;
        return false;

    }

    public static ArrayList<String> getListOfAllADFDiMetaStrings_New(ArrayList<String> sharedStringsOrig,
                                                                     String spreadsheetPath) throws Exception {
        ArrayList<String> meta = new ArrayList<String>();
        // Get r:id
        String exceptionStatus = "";
        try {
            exceptionStatus = "Trying to get ADFDiMetadataSheet Path";
            String metadataWorksheetPath = getADFDiMetadataSheetPath(spreadsheetPath);
            exceptionStatus = "Trying to get ADFDiMetadataSheet Doc";
            ZipFile spreadsheetZip = new ZipFile(spreadsheetPath);
            ZipEntry worksheetZipE = spreadsheetZip.getEntry(metadataWorksheetPath);
            if (spreadsheetZip == null || worksheetZipE == null)
                throw new Exception("Cannot get either spreadsheetZip or worksheetZipE");
            XMLDocument worksheetDoc = XMLParserHelper.getXMLDocument(spreadsheetZip.getInputStream(worksheetZipE));
            if (worksheetDoc == null)
                throw new Exception("Cannot get worksheetDoc");

            meta = getListOfAllADFDiMetaStrings_New(spreadsheetPath, sharedStringsOrig, worksheetDoc);
        } catch (Exception ex) {
            System.out.println("Exception in: " + exceptionStatus);
//            throw ex;
        }

        return meta;
    }

    private static ArrayList<String> getListOfAllADFDiMetaStrings_New(String spreadsheetPath, ArrayList<String> sharedStringsOrig,
                                                                      XMLDocument worksheetDoc) throws Exception {
        ArrayList<String> meta = new ArrayList<String>();
        MyNSResolver namsespaceResolve = new MyNSResolver();

        worksheetDoc.setProperty("SelectionLanguage", "XPath");
        NodeList rowNodes = worksheetDoc.selectNodes("//me:sheetData/me:row", namsespaceResolve);
        if (rowNodes == null || rowNodes.getLength() < 1)
            return meta;
        int rowNodesLen = rowNodes.getLength();
        for (int i = 0; i < rowNodesLen; i++) {
            Node row = rowNodes.item(i);
            if (!XMLParserHelper.isNodeName(row, "row"))
                continue;
            NodeList colNodes = row.getChildNodes();
            if (colNodes == null || colNodes.getLength() < 1)
                continue;

            int numColumns = colNodes.getLength();
            String previous = "";
            for (int c = 0; c < numColumns; c++) {
                Node col = colNodes.item(c);
                if (!XMLParserHelper.isNodeName(col, "c"))
                    continue;
                String valOfT = XMLParserHelper.getAttributeValue(col, "t");
                if (valOfT == null || !valOfT.equals("s"))
                    continue;
                String indexOfSharedString = ADFDiMetadata.getTextContentOfXPathNode(col, "v");
                Integer indexSharedStr = getIntegerFromString(indexOfSharedString);
                if (indexSharedStr >= 0) {
                    if (indexSharedStr >= sharedStringsOrig.size()) {
                        System.out.println("Index out of bound error: " + spreadsheetPath);
                    }
                    String metaString = sharedStringsOrig.get(indexSharedStr);
                    // previous is not empty, then means there is a need for concatenation. so go ahead and concat to previous
                    int metaStringLen = metaString.length();

                    previous = previous + metaString;
                    if (metaStringLen < 32765) {
                        meta.add(previous);
                        previous = "";
                    } else {
                        // If greater than max, and does not end the tag, then continue to append
                        // However if the end tag is found, then also you can just close it out
                        String tagName = getStringTagName(previous);
                        if (previous.endsWith("</" + tagName + ">")) {
                            meta.add(previous);
                            previous = "";
                        }
                    }
                } // End of processing the columns shared string

            } // end of column
        } // end of row


        return meta;
    }

    private static Integer getIntegerFromString(String s) {
        if (isEmpty(s))
            return -1;
        try {
            return Integer.parseInt(s);
        } catch (Exception ex) {
            return -1;
        }
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    public static String getADFDiMetadataSheetPath(String spreadsheetPath) throws Exception {
        String worksheetPath = "";
        String WORKBOOK_XML = "xl/workbook.xml";
        String WORKBOOK_RES_XML = "xl/_rels/workbook.xml.rels";

        String wbk_sheet_meta_name = "_ADFDI_Metadata";
        String wbkSheetMetaXPath = String.format("w:workbook/w:sheets/w:sheet[@name='%s']", wbk_sheet_meta_name);
        try {
            ZipFile spreadsheetZip = new ZipFile(spreadsheetPath);
            ZipEntry workbookXmlZip = spreadsheetZip.getEntry(WORKBOOK_XML);
            ZipEntry workbookResXmlZip = spreadsheetZip.getEntry(WORKBOOK_RES_XML);
            if (spreadsheetZip == null || workbookXmlZip == null || workbookResXmlZip == null)
                throw new Exception("Cannot get either spreadsheetZip or workbookXmlZip or workbookResXmlZip");

            XMLDocument workbookXmlDoc = XMLParserHelper.getXMLDocument(spreadsheetZip.getInputStream(workbookXmlZip));
            XMLDocument workbookResXmlDoc = XMLParserHelper.getXMLDocument(spreadsheetZip.getInputStream(workbookResXmlZip));
            if (workbookXmlDoc == null || workbookResXmlDoc == null)
                throw new Exception("Cannot get either workbookXmlDoc or workbookResXmlDoc");

            MyNSResolver namsespaceResolve = new MyNSResolver();

            workbookXmlDoc.setProperty("SelectionLanguage", "XPath");
            workbookResXmlDoc.setProperty("SelectionLanguage", "XPath");

            NodeList worksheetNodes = workbookXmlDoc.selectNodes(wbkSheetMetaXPath, namsespaceResolve);
//            if (!XMLParserHelper.hasOneNOnlyOneNode(worksheetNodes))
//                throw new Exception("Could not get worksheet node : " + wbk_sheet_meta_name);
            if (!XMLParserHelper.hasOneNOnlyOneNode(worksheetNodes)){
                return null;
            }
            Node worksheetNd = worksheetNodes.item(0);
            String relationId = XMLParserHelper.getAttributeValue(worksheetNd, "r:id");
            if (relationId == null || relationId.trim().equals(""))
                throw new Exception("Could not get relationshipId attribute value");

            //String relationshipXPath = String.format("r:Relationships/r:Relationship[@Id='%s']", relationId); // get attr:Target
            NodeList relationshipsNode = workbookResXmlDoc.getElementsByTagName("Relationships");

            if (!XMLParserHelper.hasOneNOnlyOneNode(relationshipsNode))
                throw new Exception("Could not get relationships node ");
            Node relationshipNode = relationshipsNode.item(0);
            Node worksheetRelNode =
                XMLParserHelper.getChildNodeWithMatchingAttributeAndName(relationshipNode, "Relationship", "Id", relationId);
            if (worksheetRelNode == null)
                throw new Exception("Could not find relationshipNode for id: " + relationId);
            worksheetPath = XMLParserHelper.getAttributeValue(worksheetRelNode, "Target");
            if (worksheetPath == null || worksheetPath.trim().equals(""))
                throw new Exception("Could not get worksheet target path for id: " + relationId);
            worksheetPath = "xl/" + worksheetPath;

        } catch (Exception ex) {
            System.out.println("Exception trying to get the ADFDi_Metadata sheet");
//            throw ex;
        }
        return worksheetPath;
    }

    public static ArrayList<String> getListOfAllADFDiMetaStrings(String spreadsheetPath, boolean doConcat) throws Exception {
        ArrayList<String> strings = getListOfAllStrings(spreadsheetPath, doConcat);
        if (strings == null || strings.size() < 1)
            return strings;
        ArrayList<String> allMetaStrings = new ArrayList<String>();
        Iterator<String> allStringsIter = strings.iterator();


        while (allStringsIter.hasNext()) {
            String s = allStringsIter.next();
            if (s == null)
                continue;
            s = s.trim();
            if (isStringADIComponent(s))
                allMetaStrings.add(s);
            //            Matcher m = p.matcher(s);
            //            String tagName = "";
            //            if (m.find()) {
            //                if (m.groupCount() < 1)
            //                    continue;
            //                tagName = m.group(1);
            //
            //            } else {
            //                continue;
            //            }
            //
            //            if (ADFDiMetadata.isStringDIMetadata(tagName))
            //                allMetaStrings.add(s);
        }


        return allMetaStrings;
    }

    public static ArrayList<String> getListOfAllADFDiMetaStrings(String spreadsheetPath, String metadataName,
                                                                 boolean doConcat) throws Exception {
        ArrayList<String> strings = getListOfAllStrings(spreadsheetPath, doConcat);
        if (strings == null || strings.size() < 1)
            return strings;
        ArrayList<String> allMetaStrings = new ArrayList<String>();
        Iterator<String> allStringsIter = strings.iterator();

        //        String tagNameRegEx = "<(\\w+)?[>|\\s]+?";
        //        Pattern p = Pattern.compile(tagNameRegEx);


        while (allStringsIter.hasNext()) {
            String s = allStringsIter.next();
            if (s == null)
                continue;
            s = s.trim();
            if (isStringADIComponent(s))
                allMetaStrings.add(s);


        }


        return allMetaStrings;
    }

    public static void printStrings(ArrayList<String> strs) {
        if (strs == null || strs.size() < 1)
            System.out.println("No strings to print");
        Iterator<String> strIter = strs.iterator();
        int cnt = 0;
        while (strIter.hasNext()) {
            System.out.println(String.format("%d: %s", ++cnt, strIter.next()));
        }
    }

    public static void tryXPath(XMLDocument doc) throws Exception {
        NamespaceContext nsctx = new NamespaceContext() {
            public String getNamespaceURI(String prefix) {
                return "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
            }

            public Iterator getPrefixes(String va) {
                return null;
            }

            public String getPrefix(String url) {
                return null;
            }
        };
        String xpathStr = "//me:sst";
        XPathFactory xpathFact = XPathFactory.newInstance();
        XPath xpathnew = xpathFact.newXPath();
        xpathnew.setNamespaceContext(nsctx);
        String result = xpathnew.evaluate(xpathStr, doc);
        System.out.println("Result is: " + result);


    }

    //    public static ArrayList<String> getListOfAllStrings(XMLDocument doc) throws Exception {
    //        ArrayList<String> strings = null;
    //
    //        NodeList rootNodes = doc.getElementsByTagName("sst");
    //        if (!XMLParserHelper.hasOneNOnlyOneNode(rootNodes))
    //            throw new Exception("Invalid shared strings file.. expecting only one root node sst");
    //        Node rootNode = rootNodes.item(0);
    //        String cntStr = XMLParserHelper.getAttributeValue(rootNode, "count");
    //        int numNodes = getIntFromString(cntStr);
    //        strings = new ArrayList<String>(numNodes + 10);
    //
    //        doc.setProperty("SelectionLanguage", "XPath");
    //
    //        try {
    //            // XPath is not gonna work here , because of xmlns attribute. We should do it manually.
    //            MyNSResolver resolve = new MyNSResolver();
    //            //NodeList nds = doc.selectNodes("//me:sst/me:si/me:t", resolve);
    //
    //            NodeList allSiNodes = rootNode.getChildNodes();
    //            if (allSiNodes == null || allSiNodes.getLength() < 1)
    //                return strings;
    //            int siNodesLen = allSiNodes.getLength();
    //            String strText = "";
    //            int numberOfStringsConcatenated=0;
    //            for (int i = 0; i < siNodesLen; i++) {
    //                Node siNode = allSiNodes.item(i);
    //                if (!XMLParserHelper.isNodeName(siNode, "si"))
    //                    continue;
    //                NodeList allTNodes = siNode.getChildNodes();
    //                if (allTNodes == null || allTNodes.getLength() < 1)
    //                    continue;
    //                int tNodesLen = allTNodes.getLength();
    //                for (int j = 0; j < tNodesLen; j++) {
    //                    Node tNode = allTNodes.item(j);
    //                    if (!XMLParserHelper.isNodeName(tNode, "t"))
    //                        continue;
    //                    String nodeText = tNode.getTextContent();
    //                    if (nodeText == null || nodeText.trim().equals(""))
    //                        continue;
    //                    nodeText = nodeText.trim();
    //                    // Excel cell text limitation of 32767. so you need  to concatenate the next one if it is over the max.
    //                    strText = strText + nodeText;
    //                    // Dont filter out di metadata here because it will affect the indexed count
    //                    // If length is less than max, go ahead and add it
    //                    if (nodeText.length() < 32766) {
    //                        strings.add(strText);
    //                        // Adding empty strings into the array to preserve the indexes of the strings in the final list
    //                        if (numberOfStringsConcatenated > 0) {
    //                            while (numberOfStringsConcatenated > 0) {
    //                                strings.add("");
    //                                numberOfStringsConcatenated--;
    //                            }
    //                        }
    //                        strText = "";
    //                        numberOfStringsConcatenated=0;
    //                    } else {
    //                        numberOfStringsConcatenated++;
    //                    }
    //
    //
    //                }
    //            }
    //
    //        } catch (Exception ex) {
    //            ex.printStackTrace();
    //        }
    //        return strings;
    //    }

    public static ArrayList<String> getListOfAllStrings(InputStream in, boolean doConcatenation) throws Exception {
        ArrayList<String> strings = new ArrayList<String>();
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        SAXParser saxparser = saxFactory.newSAXParser();
        XMLReader xmlreader = saxparser.getXMLReader();
        SharedStringsHandler handler = new SharedStringsHandler(doConcatenation);
        xmlreader.setContentHandler(handler);

        xmlreader.parse(new InputSource(in));
        strings = handler.getStrings();
        return strings;
    }


    public static Integer getIntFromString(String s) {
        if (s == null || s.trim().equals(""))
            return 0;
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ex) {
            return 0;
        }
    }

    public static void setBDebug(boolean bDebug) {
        SharedStringsParser.bDebug = bDebug;
    }

    public static boolean isBDebug() {
        return bDebug;
    }
}
