package oracle.apps.serialization;


import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;

import java.util.ArrayList;

import java.util.Iterator;

import oracle.apps.helpers.AngryBirdsDumper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;
import oracle.apps.helpers.ZOverallIntBase;
import oracle.apps.scanresults.ABScanResult;
import oracle.apps.scanresults.GenericScanResult;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * ViewCriteriaSerializationScanner
 * Reference: https://stbeehive.oracle.com/teamcollab/wiki/Fintech+Standards:HA+Scan+for+ViewCriteria+usage+in+method+actions
 * Reference Bugs: FIN: AR: 13467595 and SCM:9007464
 *
 * Scan: For each PageDef: if methodAction->parameterType=oracle.jbo.ViewCriteria ==> Flag
 * How to fix: Follow the Regional Search Implementation Guide - http://aseng-wiki.us.oracle.com/asengwiki/display/ATG/Regional+Search+Pattern+Implementation
 *                  OR
 *             Chnage this such that you pass in a serialized XML VC to the AM and the AM should activate the VC before proceeding
 *
 * @Author Zeesha Currimbhoy (zeesha.currimbhoy@oracle.com)
 */
public class ViewCriteriaSerializationScanner extends ZOverallIntBase {
    static String usage = "viewCriteriaSerializationScanner.sh <pathToScan>";
    static String ISSUE = "MethodActionWithVCParam";
    static String SUBISSUE = "METHOD ACTION WITH NON SERIALIZABLE VC PARAM";


    public ViewCriteriaSerializationScanner() {
        super(ISSUE, JoesBaseClass.CRAWL_TYPE.PAGE_DEF);
        setDoADE(false);
        setResultsFileName("vcResults.csv");
    }

    /**
     * Usage: viewCriteriaSerializationScanner <pathToScan> [-DfilterByList=<List_Of_files_to_run_scan_on>]
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        if (args.length != 1 || isEmpty(args[0])) {
            System.out.println("Usage: " + usage);
            System.exit(1);
        }

        String crawlDirectory = args[0].trim();
    
        ViewCriteriaSerializationScanner viewCriteriaSerializationScanner = new ViewCriteriaSerializationScanner();
        viewCriteriaSerializationScanner.startScan(crawlDirectory);

    }


    protected void doProcessFile(File fName) {

        String absPath = fName.getAbsolutePath();
        //System.out.println("Processing file: " + absPath);
        XMLDocument xmlDoc = XMLParserHelper.getXMLDocument(absPath);
        NodeList methodActionNodes = xmlDoc.getElementsByTagName("methodAction");
        if (methodActionNodes == null || methodActionNodes.getLength() < 1)
            return;
        int numberOfMethodActions = methodActionNodes.getLength();
        for (int i = 0; i < numberOfMethodActions; i++) {
            Node methodActionNode = methodActionNodes.item(i);
            String id = XMLParserHelper.getAttributeValue(methodActionNode, "id");
            NodeList namedDataNodes = methodActionNode.getChildNodes();
            if (namedDataNodes == null || namedDataNodes.getLength() < 1)
                continue;
            int numberOfNamedDataNodes = namedDataNodes.getLength();
            for (int j = 0; j < numberOfNamedDataNodes; j++) {
                Node namedDataNode = namedDataNodes.item(j);
                if (!XMLParserHelper.isNodeName(namedDataNode, "NamedData"))
                    continue;
                String parameterName = XMLParserHelper.getAttributeValue(namedDataNode, "NDName");
                String parameterType = XMLParserHelper.getAttributeValue(namedDataNode, "NDType");
                String parameterValue = XMLParserHelper.getAttributeValue(namedDataNode, "NDValue");
                if (!isEmpty(parameterType) && parameterType.trim().equals("oracle.jbo.ViewCriteria")) {
                    // This is a violation:
                    ABScanResult r = new ABScanResult(absPath);
                    r.setIssue(ISSUE);
                    r.setSubIssue(SUBISSUE);
                    String lineNum = XMLParserHelper.getLineNumberInSourceFile(namedDataNode).toString();


                    // Issue description should be: method action with id =<> has named data with name : <> and value <> of type oracle.jbo.ViewCriteria
                    String desc =
                        String.format("Line Number: %s Method Action with id: %s has NamedData with Name : %s and Value %s of type oracle.jbo.ViewCriteria",
                                      lineNum, id, parameterName, parameterValue);
                    r.setDescription(desc);
                    if(!isExemption(getExemptionKey(fName,parameterName)))
                        results.add(r);
                }
            }
        }
    }
private String getExemptionKey(File f, String parameterName)
{
    String fname = f.getName();
    String key = fname+"##"+parameterName;
    return key.trim();
    }

}
