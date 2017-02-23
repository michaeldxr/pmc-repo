package oracle.apps.votuning;

import java.io.File;

import java.io.IOException;

import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.HashSet;

import java.util.Iterator;

import java.util.jar.JarEntry;

import java.util.jar.JarFile;

import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class VOTuningUIProcessor {
    public VOTuningUIProcessor() {
        super();
       
    }

    /**
     * Map of the bc uages in this page
     */
    HashMap<String, ArrayList<TuningAnalysisRecord>> m_bcUsages =
        new HashMap<String, ArrayList<TuningAnalysisRecord>>();

    HashMap<String, ArrayList<TuningAnalysisRecord>> m_mapOfMultipleIteratorUsages =
        new HashMap<String, ArrayList<TuningAnalysisRecord>>();


    /**
     * Databindings.cpx file object for this ui project (jpr).
     * This is passed in from the caller
     */
    DataBindings m_dataBindingsCpx = null;

    /**
     * Page def for this ui file
     */
    PageDef m_pagedef = null;

    /**
     * Absolute ui path. Since this is used by all the methods, decided to
     * make it a private class variable.
     */
    private String m_uiAbsPath = "";

    /**
     * List of the output records used for analysis purposes
     */
    ArrayList<TuningAnalysisRecord> list_analysisRecords =
        new ArrayList<TuningAnalysisRecord>();


    /**
     * List of the output records used for analysis purposes
     */
    ArrayList<TuningAnalysisRecord> list_errorRecords =
        new ArrayList<TuningAnalysisRecord>();


    /**
     * Base lba path.
     * e.g. if the ui file path is: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/public_html/oracle/apps/financials/Test.jspx
     * then the absolute base path is derived from this ui path as:
     * $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/
     */
    private String m_baseLbaPath = "";

    /**
     * the error logger to log all the errors to.
     * The object is passed in from the caller
     */
    private Logger m_errorLogger = null;

    /**
     * Whether you want to record multiple iterator usages
     */
    private boolean recordMultipleIteratorUsages = true;

    private static boolean bDebug = false;


    /**
     *
     * @param uiFile
     * @param bindings
     * @param errorLogger
     * @return
     * null if:
     * 1. [Prerequisite] Databindings is nulll
     * 2. page def is null
     * 3. Cannot obtain baseLba path from ui file - this means there is something seriously wrong with the ui path.
     * 4. [XML Parsing Error] Cannot parse the input ui file with the xml parser
     */
    public ArrayList<TuningAnalysisRecord> analyzeUIFile(File uiFile,
                                                         DataBindings bindings,
                                                         Logger errorLogger) {

        // validate input parameters
        m_uiAbsPath = uiFile.getAbsolutePath();
        String fileName = uiFile.getName();
        m_errorLogger = errorLogger;
        if (bindings == null) {
            String err =
                String.format("[ERROR] Databindings was null.. Skipping: %s",
                              m_uiAbsPath);
            System.out.println(err);
            m_errorLogger.log(Level.SEVERE, err);
            return null;
        }

        m_dataBindingsCpx = bindings;

        //---------------- get page definition for this file--------------
        File pageDef = getPageDef(uiFile, bindings);
        if (pageDef == null) {
            if (bDebug) {
                String msg =
                    String.format("[No pagedef] Cannot get pageDef for file.. Skipping: %s",
                                  m_uiAbsPath);
                System.out.println(msg);
            }
            return null;
        }

        String pageDefAbsPath = pageDef.getAbsolutePath();
        m_pagedef = PageDef.parsePageDef(pageDefAbsPath);
        if (m_pagedef == null) {
            String msg =
                String.format("[No pagedef] Cannot parse page def: %s for file.. Skipping: %s",
                              pageDefAbsPath, m_uiAbsPath);
            System.out.println(msg);
            m_errorLogger.log(Level.INFO, msg);
            return null;
        }

        m_baseLbaPath = UiFileHelper.getBaseLBAPath(m_uiAbsPath);
        if (isEmpty(m_baseLbaPath)) {
            String msg =
                "[ERROR] Could not get the base lba path for file: " + m_uiAbsPath;
            System.out.println(msg);
            m_errorLogger.log(Level.SEVERE, msg);

            return null;
        }
        //objPageDef.printPageDef();
        //---------------- obtained page def--------------


        // traverse ui file
        XMLDocument uiXml = XMLParserHelper.getXMLDocument(m_uiAbsPath);
        if (uiXml == null) {
            String msg =
                String.format("[ERROR] analyzeUIFile: could not parse xml doc for ui file..Skipping file.. %s",
                              m_uiAbsPath);
            System.out.println(msg);
            m_errorLogger.log(Level.SEVERE, msg);
            return null;

        }

        Element rootNode = uiXml.getDocumentElement();
        if (rootNode != null && rootNode.hasChildNodes()) {
            traverseNodes(rootNode.getChildNodes(), uiFile);
        }

        return list_analysisRecords;
    }

   /*NOT Needed anymore private void analyzeResultsToRemoveMultipleIterators(ArrayList<TuningAnalysisRecord> analysisRecords,
                                                         ArrayList<TuningAnalysisRecord> listOfErrorRecords) {
        if (analysisRecords == null || analysisRecords.size() < 1)
            return;

        Iterator<TuningAnalysisRecord> analysisRecordIter =
            analysisRecords.iterator();
        while (analysisRecordIter.hasNext()) {
            TuningAnalysisRecord record = analysisRecordIter.next();
            String iterName = record.getPageDefIteratorName();
            if (mapOtherIteratorUsages.containsKey(iterName)) {
                ArrayList<String> otherComponents =
                    mapOtherIteratorUsages.get(iterName);
                String otherComponentsString = otherComponents.toString();
                analysisRecordIter.remove();
                record.setDiagnostic(otherComponentsString);
                listOfErrorRecords.add(record);
            }
        }

    }*/

   public boolean isDirectChildOfPanelStretchLayout(Node x)
   {
       Node p = x.getParentNode();
       
       if(p == null)
           return false;
       
       String parentName = p.getNodeName();

       if("af:panelStretchLayout".equals(parentName))
       {
           // disqualify panelStretchLayou cases that are dimensionsFrom = children
           NamedNodeMap nnm = p.getAttributes();
           Node df = nnm.getNamedItem("dimensionsFrom");
           if(df != null && "children".equals(df.getNodeValue()))
               return false;
           
           return true;
       }

       if("fnd:applicationsTable".equals(parentName) ||
          "fnd:applicationsTreeTable".equals(parentName) || 
           "f:facet".equals(parentName))
           return isDirectChildOfPanelStretchLayout(p);

       return false;
   }


    /**
     * Recursively traverses all entire ui file nodes.
     * for each component to be analyzed (e.g. af:table), records the required properties.
     * Gets the tree iterator and obtains the iterator binding from the page def.
     * Also gets the am package name from page def and cpx.
     * For the vo usage it tries to find the am and gets the vo fetch size.
     * @param childNodes
     * @param uiFile
     */
    public void traverseNodes(NodeList childNodes, File uiFile) {

        if (childNodes == null) {
            return;
        }
        String uiFilePath = uiFile.getPath();

        int childNodeLength = childNodes.getLength();
        for (int i = 0; i < childNodeLength; i++) 
        {
            Node child = childNodes.item(i);
            String nodeName = child.getNodeName();
            
            boolean exempted = false;
            if("af:table".equals(nodeName) || "af:treeTable".equals(nodeName))
            {
                exempted = isDirectChildOfPanelStretchLayout(child);
            }
            

            if (!exempted && XMLParserHelper.isNodeName(child, "af:table")) 
            {
                TuningAnalysisRecord record = new TuningAnalysisRecord();
                record.setUifilePath(uiFilePath);
                record.setUifileName(uiFile.getName());
                record.setPageDefFilePath(m_pagedef.absPath);
                record.setComponentType(child.getNodeName());
                String productName = UiFileHelper.getProductName(uiFilePath);
                record.setProduct(productName);

                String valueNode =
                    XMLParserHelper.getAttributeValue(child, "value");

                record.setUiTagFetchSize(XMLParserHelper.getAttributeValue(child,
                                                                           "fetchSize"));
                record.setAutoHeightRows(XMLParserHelper.getAttributeValue(child,
                                                                           "autoHeightRows"));
                record.setUiTagRows(XMLParserHelper.getAttributeValue(child,
                                                                      "rows"));
                record.setContentDelivery(XMLParserHelper.getAttributeValue(child,
                                                                            "contentDelivery"));
                record.setSerializedNode(XMLParserHelper.getNodeAsString(child,
                                                                         ""));

                // get page def iterator name and iterator range size based on Iterator configured in value node
                if (!isPossibleELExpression(valueNode)) {
                    String msg =
                        String.format("[ERROR] : traverseNodes: Skipping file: %s : Cannot recognize format of value node: %s.. AFAIK value node should always map to EL Exp and only then can we recover the itetrator binding.. ",
                                      uiFilePath, valueNode);
                    System.out.println(msg);
                    m_errorLogger.log(Level.SEVERE, msg);
                    if (record != null) {
                        record.setDiagnostic(record.getDiagnostic() +
                                             "[ERROR] Cannot recognize format of value node: " +
                                             valueNode + "\n");
                        // this is an error record
                        list_errorRecords.add(record);
                    }
                    continue;
                } else {
                    // gets the iterator name from the EL. e.g. #{bindings.IteratorName.<whatever>}
                    String treeIteratorId =
                        extractIteratorNameFromEL(valueNode);
                    if (isEmpty(treeIteratorId)) {
                        String msg =
                            String.format("[ERROR]: traverseNodes: Skipping file %s.. Could not extract iteratorId from EL %s",
                                          uiFilePath, valueNode);
                        System.out.println(msg);
                        m_errorLogger.log(Level.SEVERE, msg);
                        if (record != null) {
                            record.setDiagnostic(record.getDiagnostic() +
                                                 "[ERROR] Could not extract iteratorId from EL: " +
                                                 valueNode + "\n");
                            // this is an error record
                            list_errorRecords.add(record);
                        }
                        // dont need to do any more processing on this node..
                        continue;
                    }
                    // gets the page def iterator binding
                    PageDef.PageDefIteratorBinding pagedefIterBinding =
                        m_pagedef.getIterBindingForTreeId(treeIteratorId);
                    if (pagedefIterBinding == null) {

                        String msg =
                            String.format("[ERROR] :traverseNodes: skipping file: %s : could not obtain iterator binding for the tree iterator id: %s",
                                          uiFilePath, treeIteratorId);
                        System.out.println(msg);
                        m_errorLogger.log(Level.SEVERE, msg);
                        if (record != null) {
                            record.setDiagnostic(record.getDiagnostic() +
                                                 "[ERROR] could not obtain iterator binding for the tree iterator id: " +
                                                 treeIteratorId + "\n");
                            // this is an error record
                            list_errorRecords.add(record);
                        }
                        continue;
                    }
                    record.setPageDefIteratorName(pagedefIterBinding.getId());
                    record.setIteratorRangeSize(pagedefIterBinding.getRangeSize().toString());
                }

                try {
                    record =
                            updateVOPropertiesInRecord(record.getPageDefIteratorName(),
                                                       record);
                } catch (Exception ex) {
                    System.out.println("Exception: " + ex.getMessage());
                    if (record != null) {
                        list_errorRecords.add(record);
                    }
                    continue;
                }
                if (record != null) {
                    list_analysisRecords.add(record);
                    updateMultipleBcUsages(record);
                    updateMultipleIteratorUsages(record);
                }

            }/*NO NEEDED ANYMORE - this was there first to find if there were lovs that were using the same iterator as the table.
              * This is not a concern anymore - because it would be possible that an lov uses the same table iterator siince it refers to the current row.
              * Dont need this so commenting it out
              * else {
                // if node name matches the ones in the list - whose iterators also we have to check for
                String nodeName = child.getNodeName();

                String id = XMLParserHelper.getAttributeValue(child, "id");

                if (map_lovBindingAttrs.containsKey(nodeName)) {
                    String iteratorBindingAttr =
                        map_lovBindingAttrs.get(nodeName);
                    if (!isEmpty(iteratorBindingAttr)) {
                        String iteratorBindingValue =
                            XMLParserHelper.getAttributeValue(child,
                                                              iteratorBindingAttr);
                        if (isPossibleELExpression(iteratorBindingValue)) {
                            String listBindingId =
                                extractIteratorNameFromEL(iteratorBindingValue);
                            if (!isEmpty(listBindingId)) {
                                // get the listBindingName
                                String iteratorName =
                                    getIteratorNameFromListIteratorId(listBindingId);
                                String formattedName =
                                    String.format(" %s id=%s %s=%s ", nodeName,
                                                  id, iteratorBindingAttr,
                                                  iteratorBindingValue);
                                UiFileHelper.addToMap(mapOtherIteratorUsages,
                                                      iteratorName,
                                                      formattedName);
                            }
                        }

                    }
                }
            }*/
            if (child.hasChildNodes()) {
                traverseNodes(child.getChildNodes(), uiFile);
            }
        }
    }


    private void updateMultipleBcUsages(TuningAnalysisRecord record) {

        String amFullPackage = record.getAmFullPackage();
        String voInstanceName = record.getVoInstanceName();
        if (!isEmpty(amFullPackage) && !isEmpty(voInstanceName)) {
            String bcKey = amFullPackage + "#" + voInstanceName;
            m_bcUsages = UiFileHelper.addToMap(m_bcUsages, bcKey, record);
        }

    }

    private void updateMultipleIteratorUsages(TuningAnalysisRecord record) {
        String pagedefPath = record.getPageDefFilePath();
        String pageDefIteratorName = record.getPageDefIteratorName();

        if (!isEmpty(pagedefPath) && !isEmpty(pageDefIteratorName)) {
            String pagedefKey = pagedefPath + "#" + pageDefIteratorName;
            m_mapOfMultipleIteratorUsages =
                    UiFileHelper.addToMap(m_mapOfMultipleIteratorUsages,
                                          pagedefKey, record);
        }
    }

    /**
     *Is a possible EL Expression if it starts with '#{'
     * @param possibleEL
     * @return
     */
    private boolean isPossibleELExpression(String possibleEL) {
        if (!isEmpty(possibleEL) && possibleEL.trim().startsWith("#{")) {
            return true;
        }
        return false;
    }

    /**
     * purpose: find the vo fetch size - either in the am usage or the vo itself and update relevant properties in the record
     * 1. Get the pageDef iterator binding based on the iterator Id
     * 2. based on the iteratorbinding get
     *      a. the bindname - which is the same as vo usage name
     *      b. the datacontrolname
     * 3. Based on the datacontrol name - get the datacontrol package from databindings.cpx
     * 4. based on the datacontrol name - get the am name
     *      am name is obtained by removing 'DataControl' suffix
     * 5. Find the am
     * 6. Find the vo usage in am
     * 7. if vousagefetchsize !=null
     *      set vo usage fetch size property in record
     * 8. If vo usage fetchsize == null
     *      8a. Get the actual vo
     *      8b. Find the vo fetch size and update in record
     * @param iteratorId
     * @param record
     * Exception thrown if any one of the following
     * 1. page def is null
     * 2. IteratorId is null
     * 3. record is null
     * 4. Cannot get iterator id from page def
     * 5. Cannot get datacontrol name from iterator binding in page def
     * 6. Could not obtain am name from data control name
     * 7. If datacontrol package is null -obtained from the databindings.cpx file
     * 8. If am Absolute file cannot be obtained
     * 9. if amxml not obtained
     * @return
     * null if:
     * 1. input parameters are empty
     * 2. page def is null
     */
    private TuningAnalysisRecord updateVOPropertiesInRecord(String iteratorId,
                                                            TuningAnalysisRecord record) throws Exception {

        if (m_pagedef == null || isEmpty(iteratorId) || record == null) {
            String msg = "page def is null or iterator Id is null";
            System.out.println(msg);
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        PageDef.PageDefIteratorBinding iterBinding =
            m_pagedef.mapIterators.get(iteratorId);
        if (iterBinding == null) {
            String msg =
                String.format("[ERROR] :updateVOPropertiesInRecord: Could not find iterator Id: %s in page def %s Skipping this record in the UI File %s",
                              iteratorId, m_pagedef.absPath, m_uiAbsPath);
            System.out.println(msg);
            m_errorLogger.log(Level.SEVERE, msg);
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }
        String voInstanceName = iterBinding.getBindName();
        String datacontrolName = iterBinding.getDataControlName();
        if (datacontrolName == null) {
            record.setDiagnostic(record.getDiagnostic() +
                                 "could not obtain data control name from iterator binding: " +
                                 iteratorId + "\n");

            String msg =
                String.format("[ERROR] :updateVOPropertiesInRecord: page def entry for iterator Binding %s does not have an entry for 'DataControlName'. uiFile: %s.. pagedef path: %s cpx path: %s.. skipping",
                              iteratorId, m_uiAbsPath,
                              m_dataBindingsCpx.getCpxFullPath(),
                              m_pagedef.absPath);
            System.out.println(msg);
            m_errorLogger.log(Level.SEVERE, msg);
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }
        String amName = getAMNameFromDataControlName(datacontrolName);
        if (amName == null) {
            record.setDiagnostic(record.getDiagnostic() +
                                 "could not obtain am name from data control: " +
                                 datacontrolName + "\n");

            String msg =
                String.format("[ERROR] :updateVOPropertiesInRecord:  could not obtain am name from data control: %s uiFile: %s..",
                              datacontrolName, m_uiAbsPath);
            System.out.println(msg);
            m_errorLogger.log(Level.SEVERE, msg);
            throw new Exception(msg);
        }
        String dcPackage =
            m_dataBindingsCpx.getDataControlPackage(datacontrolName);
        if (isEmpty(dcPackage)) {

            record.setDiagnostic(record.getDiagnostic() +
                                 "could not obtain datacontorl package name for: " +
                                 datacontrolName + "\n");

            String msg =
                String.format("[ERROR] :updateVOPropertiesInRecord:  could not obtain data control package name for datacontrol name: %s, Iterator Id: %s, databindings.cpx file path: %s uiFile: %s..",
                              datacontrolName, iteratorId,
                              m_dataBindingsCpx.cpxFullPath, m_uiAbsPath);
            System.out.println(msg);
            m_errorLogger.log(Level.SEVERE, msg);
            throw new Exception(msg);
        }
        record.setAmFullPackage(dcPackage + "." + amName);
        record.setVoInstanceName(voInstanceName);

        /**
         * Typically the ams are in the same project space.
         * e.g. if the ui is in $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/allocations/ui/public_html
         * then the model am is typically under $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/allocations/<model> or <publicmodel> or <protectedmodel>
         * Instead of going into the imported jars.. typically you should first look in this directory structure.. it will save time.
         */


        //System.out.println("Looking for am: " + amPathToFind);
        boolean bFoundAmAsFile = false;
        String uiJprFullPath =
            UiFileHelper.getJprLocationForUiFile(m_uiAbsPath);
        XMLDocument amXml = null;

        StringBuffer amPath = new StringBuffer();
        String amFullPackagePath = dcPackage + "." + amName;
        amXml = getAmFile(amFullPackagePath, amPath);
        // If am is not file  - then this will be empty
        String amAbsPath = amPath.toString();
        record.setAmPath(amAbsPath);
        // even though we can proceed to analyze with the amxml we should not -because such files cannot be fixed.. We need the absolute am file to do any fixes. hence throw exception here
        if (isEmpty(amAbsPath)) {
            String msg = "AM file could not be obtained";
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        if (amXml == null) {
            record.setDiagnostic(record.getDiagnostic() +
                                 " Could not find am : " + amFullPackagePath +
                                 " \n");
            String msg =
                String.format("[ERROR]: updateVOPropertiesInRecord:could not obtain am: %s: UiFile: %s.. ",
                              amFullPackagePath, m_uiAbsPath);
            m_errorLogger.log(Level.SEVERE, msg);
            System.out.println(msg);
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        // Need to handled nested am case
        if (!isEmpty(voInstanceName) && voInstanceName.startsWith("Root.")) {
            int beginIndex = voInstanceName.indexOf("Root.");
            beginIndex = beginIndex + "Root.".length();
            int endIndex = voInstanceName.indexOf(".", beginIndex);
            if (beginIndex != -1 && endIndex != -1) {
                String nestedAmName =
                    voInstanceName.substring(beginIndex, endIndex);
                String nesterdAmPackage =
                    getNestedAmPackage(amXml, nestedAmName);
                voInstanceName = voInstanceName.substring(endIndex + 1);
                StringBuffer nestedAmFilePath = new StringBuffer();
                amXml = getAmFile(nesterdAmPackage, nestedAmFilePath);
                if (amXml == null) {
                    String msg =
                        String.format("[ERROR] Could not find nested am file %s, full nested am package %s. UI File: %s ",
                                      nestedAmName, nesterdAmPackage,
                                      m_uiAbsPath);
                    System.out.println(msg);
                    m_errorLogger.log(Level.SEVERE, msg);
                    record.setDiagnostic(msg);
                    throw new Exception(msg);
                }
                amAbsPath = nestedAmFilePath.toString();
                record.setAmPath(amAbsPath);
            }
        }

        return updateVoFetchSize(amAbsPath, amXml, voInstanceName, record,
                                 uiJprFullPath);

    }

    private String getNestedAmPackage(XMLDocument amXml, String nestedAmName) {
        if (amXml == null)
            return null;
        NodeList nestedAmList = amXml.getElementsByTagName("AppModuleUsage");
        if (nestedAmList != null) {
            Node nestedAmNode =
                XMLParserHelper.getNodesInListWithMatchingAttribute(nestedAmList,
                                                                    "Name",
                                                                    nestedAmName);
            if (nestedAmNode != null) {
                return XMLParserHelper.getAttributeValue(nestedAmNode,
                                                         "FullName");
            }
        }

        return null;
    }

    /**
     * amFile = null if the am was found in a jar and not in an absolute path
     *
     * @param amFile
     * @param amXml
     * @param voUsageName
     * @param record
     * @param uiJprFile
     * @return
     *
     */
    private TuningAnalysisRecord updateVoFetchSize(String amAbsPath,
                                                   XMLDocument amXml,
                                                   String voUsageName,
                                                   TuningAnalysisRecord record,
                                                   String uiJprFile) throws Exception {
        boolean bFoundVoUsage = false;
        String voUsagePackage = "";
        String voUsageFetchSize = "";


        if (amXml == null) {
            String msg =
                String.format("updateVoFetchSize: amXml was null.. returning.. ui File: %s",
                              m_uiAbsPath);
            System.out.println(msg);
            m_errorLogger.log(Level.SEVERE, msg);
            record.setDiagnostic("amXml was null");
            throw new Exception(msg);
        }

        NodeList appmoduleNodeList = amXml.getElementsByTagName("AppModule");
        if (!XMLParserHelper.hasOneNOnlyOneNode(appmoduleNodeList)) {
            String msg =
                String.format("[ERROR] : updateVoFetchSize : Expecting one and only one node tag AppModule.. Skipping file: %s",
                              m_uiAbsPath);
            System.out.println(msg);
            m_errorLogger.log(Level.SEVERE, msg);
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }
        Node appModuleNode = appmoduleNodeList.item(0);
        NodeList appModuleChildren = appModuleNode.getChildNodes();
        if (appModuleChildren == null) {
            String msg =
                "VO usage not found in am.. no child nodes in this appmodule tag";
            System.out.println(msg);
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }
        int appModuleChildLen = appModuleChildren.getLength();
        for (int i = 0; i < appModuleChildLen; i++) {
            Node appModuleChild = appModuleChildren.item(i);
            String tagName = appModuleChild.getNodeName();
            if (!isEmpty(tagName) && tagName.trim().equals("ViewUsage")) {
                String obtainedVoUsageName =
                    XMLParserHelper.getAttributeValue(appModuleChild, "Name");

                if (!isEmpty(obtainedVoUsageName) &&
                    obtainedVoUsageName.trim().equals(voUsageName)) {
                    if (bFoundVoUsage == true) {
                        String msg =
                            String.format("[WARNING]: updateVoFetchSize: file might be corrupt.. duplicate vo usage: %s in am file: %s",
                                          obtainedVoUsageName, amAbsPath);
                        System.out.println(msg);
                        m_errorLogger.log(Level.WARNING, msg);
                        record.setDiagnostic(record.getDiagnostic() +
                                             " * duplicate vo usage detected in am: " +
                                             amAbsPath);
                        // At the time of fixing this will be a problem.. hence throw an exception.. we only want to collect the good or can be fixed records
                        throw new Exception(msg);
                    }
                    bFoundVoUsage = true;
                    voUsagePackage =
                            XMLParserHelper.getAttributeValue(appModuleChild,
                                                              "ViewObjectName");
                    voUsageFetchSize =
                            XMLParserHelper.getAttributeValue(appModuleChild,
                                                              "FetchSize");
                }
            }
        }

        // at this point you should have the voUsageName, ViewObjectName and FetchSize
        if (isEmpty(voUsagePackage)) {

            String msg =
                String.format("[ERROR] VO Package for VO: %s could not be obtained from am file: %s .. UI File: %s",
                              voUsageName, amAbsPath, m_uiAbsPath);
            System.out.println(msg);
            m_errorLogger.log(Level.SEVERE, msg);
            record.setDiagnostic(msg);
            throw new Exception(msg);
        } else {
            record.setVoFullPackage(voUsagePackage);
        }
        // If a vo usage is found - return the record. no need to look for the default vo fetch size
        // CORRECTION- even if usage is found - still find the dfault fetch size.
        // while fixing we need to check if new fetch size is < default fetch size.
        if (voUsageFetchSize != null) {
            record.setVoUsageFetchSizeInAm(voUsageFetchSize);
            //return record;
        }
        //NOTE: Even if the vo usage is not found - at this point while fixing we can fix the am.. so dont throw an exception of the vo is not found

        XMLDocument viewObjectFile = null;
        if (isEmpty(amAbsPath)) {
            //TODO: Dont know how to get to this point because we dont have the model jpr to look for the secondary dependencies..
            System.out.println("AM was not obtained from a file.. need to look for voxml in the ui jpr dependencies.. this will be a case of error when we are actually modifying the files.. ");

            String bcPathToFind = voUsagePackage.replaceAll("\\.", "/");
            bcPathToFind = bcPathToFind + ".xml";
            viewObjectFile =
                    JprHelper.getResourceAsXMlFromJprLibraryRefs(uiJprFile,
                                                                 bcPathToFind);
        } else {
            viewObjectFile = getViewObject(amAbsPath, voUsagePackage, record);
        }
        if (viewObjectFile == null) {
            System.out.println("[ERROR] Could not find view object: " +
                               voUsagePackage);
            String msg =
                String.format("[ERROR] : updateVoFetchSize: could not find view object %s",
                              voUsagePackage);
            System.out.println(msg);
            m_errorLogger.log(Level.SEVERE, msg);
            record.setDiagnostic(record.getDiagnostic() +
                                 " * couldnot find vo " + voUsagePackage +
                                 " \n");
            return record;
        }
        // System.out.println("View object found");
        String fetchSize = "ERROR";
        NodeList listViewObj =
            viewObjectFile.getElementsByTagName("ViewObject");
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
            System.out.println("Expecting one and only one view object node");
        } else {
            Node voNode = listViewObj.item(0);
            String obtFetchSize =
                XMLParserHelper.getAttributeValue(voNode, "FetchSize");
            if (obtFetchSize == null) {
                fetchSize = "Not Set";
            } else {
                fetchSize = obtFetchSize;
            }
            record.setVoDefaultFetchSize(fetchSize);
        }
        return record;

    }

    private XMLDocument getViewObject(String amAbsFilePath,
                                      String voFullPackage,
                                      TuningAnalysisRecord record) {
        if (isEmpty(voFullPackage)) {
            System.out.println("[ERROR] getViewObject: VO package empty");
            return null;
        }
        String bcPathToFind = voFullPackage.replaceAll("\\.", "/");
        bcPathToFind = bcPathToFind + ".xml";
        XMLDocument voXml = null;

        //Search location 1 - in the same base directory that the am is in
        String modelJprLocation =
            UiFileHelper.getJprLocationForModelFile(amAbsFilePath);
        if (!isEmpty(modelJprLocation)) {
            String modelBaseDir = getPreviousDirectory(modelJprLocation);
            File voFile = findBCInDirectory(modelBaseDir, bcPathToFind);
            if (voFile != null) {
                record.setVoPath(voFile.getAbsolutePath());
                return XMLParserHelper.getXMLDocument(voFile.getAbsolutePath());
            }

        }
        //Search location 2 - in all valid model directories in the base lba path
        ArrayList<String> modelDirslist =
            getListOfModelDirectoriesInPath(m_baseLbaPath);
        if (modelDirslist == null || modelDirslist.size() <= 0) {
            System.out.println("[INFO] Could not obtain any valid model dirs");
        } else {
            for (int i = 0; i < modelDirslist.size(); i++) {
                String modelDir = modelDirslist.get(i);
                File bcFile = findBCInDirectory(modelDir, bcPathToFind);
                if (bcFile != null) {
                    record.setVoPath(bcFile.getAbsolutePath());
                    return XMLParserHelper.getXMLDocument(bcFile.getAbsolutePath());
                }
            }
        }

        // Search location 3 - in all valid model directories within the previous directory
        ArrayList<String> modelDirsInPrevDir =
            getListOfAllModelDirsInPrevDir(m_baseLbaPath);
        if (modelDirsInPrevDir != null && modelDirsInPrevDir.size() > 0) {
            int modeldirsinDirSize = modelDirsInPrevDir.size();
            for (int i = 0; i < modeldirsinDirSize; i++) {
                String modelDirInDir = modelDirsInPrevDir.get(i);
                File voFile = findBCInDirectory(modelDirInDir, bcPathToFind);
                if (voFile != null && voFile.exists()) {
                    record.setVoPath(voFile.getAbsolutePath());
                    return XMLParserHelper.getXMLDocument(voFile.getAbsolutePath());
                }
            }
        }
        // Search location 4 - in all dependent model jars
        /* If it reaches this stage means it did not find the view object in any of the most probable directories..
        now have to scan all the referenced jars in the jpr*/

        if (isEmpty(modelJprLocation)) {
            System.out.println("Could not obtain jpr");
            return null;
        }
        HashSet<String> adfLibrarys =
            JprHelper.getAllAdfLibrarysInJpr(modelJprLocation, true);
        if (adfLibrarys != null && adfLibrarys.size() > 0) {
            Iterator<String> libraryIter = adfLibrarys.iterator();
            while (libraryIter.hasNext()) {
                String lib = libraryIter.next();
                JarFile jar = null;
                try {
                    jar = new JarFile(lib);

                    if (jar != null) {

                        JarEntry voEntry = jar.getJarEntry(bcPathToFind);

                        if (voEntry != null) {
                            System.out.println("Found the vo in: " +
                                               jar.getName());
                            record.setVoPath("JAR: " + jar.getName());
                            InputStream contents = jar.getInputStream(voEntry);
                            voXml = XMLParserHelper.getXMLDocument(contents);
                            if (contents != null) {
                                contents.close();
                            }
                            break;
                        }
                    }
                } catch (IOException ex) {
                    String msg =
                        "[ERROR]:getViewObject: IOException: " + ex.getMessage();
                    if (bDebug){
                        System.out.println(msg);
                        m_errorLogger.log(Level.SEVERE, msg);
                    }
                    continue;
                }
            }
        } else {
            System.out.println("Could not obtain any librarys from jpr: " +
                               modelJprLocation);
            return null;
        }


        return voXml;
    }

    private String getPreviousDirectory(String path) {
        if (!isEmpty(path)) {
            int indexLastSlash = path.lastIndexOf("/");
            if (indexLastSlash != -1) {
                return path.substring(0, indexLastSlash);
            }
        }
        return null;
    }

    /**
     * Tries to find the am file : amPathToFind
     * 1.  From the ui file path - gets the uiJpr full path
     *      e.g. if the ui file name is : $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/public_html/oracle/apps/financials/Test.jspx
     *      UI Jpr will be: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/<something.jpr>
     * 2. if the UI Jpr is not null: get the dependent model project dependencies e.g. model.jpr
     *      2a. For each dependent model project jpr - get the base model directory
     *          e.g. if the model jpr is: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/model/Model.jpr
     *          base model directory is: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/model
     *      2b - try to find the am in this base model directory
     * [NOTE; It is most likely that the am will be in the dependent model projects and hence this is the first search location]
     * 3. if the am is not found in any of the dependent model jprs or if the ui jpr was not recoverable then :
     *      3a. Search for all probable model directories in the base lba location
     *          e.g. for ui path:$ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/public_html/oracle/apps/financials/Test.jspx
     *          base lba location is: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry
     *      3b. Find all probably model directories in this path
     *      3c. for each model directory found - try to locate the am.
     *
     * Return - amFile if found
     * Retrun - null if not found
     * @param amPathToFind
     * path of the am artifact to find
     * @return
     * Return - amFile if found
     * Retrun - null if not found
     */
    private XMLDocument getAmFile(String amFullPackage,
                                  StringBuffer returnAmFilePath) {
        File amFile = null;

        String amPathToFind = amFullPackage.replaceAll("\\.", "/");
        amPathToFind = amPathToFind + ".xml";

        String uiJprFullPath =
            UiFileHelper.getJprLocationForUiFile(m_uiAbsPath);
        if (uiJprFullPath != null) {
            HashSet<String> modelDependencyJars =
                JprHelper.getProjectDependencysInJar(uiJprFullPath);

            if (modelDependencyJars != null) {
                Iterator<String> modelJprIterator =
                    modelDependencyJars.iterator();
                while (modelJprIterator.hasNext()) {
                    String modelJprLocation = modelJprIterator.next();
                    File modelFile = new File(modelJprLocation);
                    if (modelFile.exists()) {
                        String modelDir =
                            getPreviousDirectory(modelJprLocation);
                        amFile = findBCInDirectory(modelDir, amPathToFind);
                        if (amFile != null) {
                            returnAmFilePath.append(amFile.getAbsolutePath());
                            return XMLParserHelper.getXMLDocument(amFile.getAbsolutePath());
                        }
                    }
                }

            }
        } else {
            return null;
        }

        if (isEmpty(m_baseLbaPath)) {
            System.out.println("ERROR: Base UI Path is empty.. skipping file:");
            return null;
        }
        ArrayList<String> modelDirs =
            getListOfModelDirectoriesInPath(m_baseLbaPath);

        for (int i = 0; i < modelDirs.size(); i++) {
            String modelDir = modelDirs.get(i);
            amFile = findBCInDirectory(modelDir, amPathToFind);
            if (amFile != null && amFile.exists()) {
                returnAmFilePath.append(amFile.getAbsolutePath());
                return XMLParserHelper.getXMLDocument(amFile.getAbsolutePath());
            }
        }

        ArrayList<String> modelDirsInPrevDir =
            getListOfAllModelDirsInPrevDir(m_baseLbaPath);
        if (modelDirsInPrevDir != null && modelDirsInPrevDir.size() > 0) {
            int modeldirsinDirSize = modelDirsInPrevDir.size();
            for (int i = 0; i < modeldirsinDirSize; i++) {
                String modelDirInDir = modelDirsInPrevDir.get(i);
                amFile = findBCInDirectory(modelDirInDir, amPathToFind);
                if (amFile != null && amFile.exists()) {
                    returnAmFilePath.append(amFile.getAbsolutePath());
                    return XMLParserHelper.getXMLDocument(amFile.getAbsolutePath());
                }
            }
        }


        String msg =
            String.format("[ERROR] Could not find am in the probable directories.. AM: %s, UIFile: %s.. will look at dependencies in ui jpr now.. however during modifications will not be possible to modify this am...  ",
                          amPathToFind, m_uiAbsPath);
        System.out.println(msg);
        m_errorLogger.log(Level.SEVERE, msg);
        if (isEmpty(uiJprFullPath)) {
            System.out.println("Could not find jpr for ui file: " +
                               m_uiAbsPath);
            return null;
        }

        return JprHelper.getResourceAsXMlFromJprLibraryRefs(uiJprFullPath,
                                                            amPathToFind);

    }

    private ArrayList<String> getListOfAllModelDirsInPrevDir(String baseDir) {
        ArrayList<String> modelDirs = new ArrayList<String>();
        if (isEmpty(baseDir)) {
            return null;
        }

        // am still not found.. then go one directory higher
        String prevDir = getPreviousDirectory(baseDir);
        if (isEmpty(prevDir)) {
            return null;
        }

        ArrayList<String> alldirs =
            getListOFAllValidDirectoriesInPath(prevDir);
        if (alldirs != null) {
            Iterator<String> allDirsIter = alldirs.iterator();
            while (allDirsIter.hasNext()) {
                String dir = allDirsIter.next();
                ArrayList<String> modeDirsInDir =
                    getListOfModelDirectoriesInPath(dir);
                modelDirs.addAll(modeDirsInDir);
            }
        } else {
            return null;
        }
        return modelDirs;
    }

    private File findBCInDirectory(String modelDir, String amPath) {
        String amFullPAth = modelDir + "/src/" + amPath;
        File f = new File(amFullPAth);
        if (f.exists()) {
            return f;
        }
        return null;
    }

    private ArrayList<String> getListOfModelDirectoriesInPath(String baseDir) {
        if (isEmpty(baseDir)) {
            return null;
        }
        File f = new File(baseDir);
        ArrayList<String> list_modeDir = new ArrayList<String>();
        if (f.exists()) {
            File[] files = f.listFiles();
            int fileLen = files.length;
            for (int i = 0; i < fileLen; i++) {
                File file = files[i];
                String absPath = file.getAbsolutePath();

                if (file.isDirectory() && isValidModelDir(absPath))
                    list_modeDir.add(absPath);
            }

        }
        return list_modeDir;
    }

    private ArrayList<String> getListOFAllValidDirectoriesInPath(String baseDir) {
        if (isEmpty(baseDir)) {
            return null;
        }
        File f = new File(baseDir);
        ArrayList<String> list_allDirs = new ArrayList<String>();
        if (f.exists()) {
            File[] files = f.listFiles();
            int fileLen = files.length;
            for (int i = 0; i < fileLen; i++) {
                File file = files[i];
                String absPath = file.getAbsolutePath();

                if (file.isDirectory() && !absPath.contains(".ade_path"))
                    list_allDirs.add(absPath);
            }

        }
        return list_allDirs;
    }

    private boolean isValidModelDir(String dirPath) {
        if (isEmpty(dirPath)) {
            return false;
        }

        int lastSlash = dirPath.lastIndexOf("/");
        String lastDir = "nothing";
        if (lastSlash != -1) {
            lastDir = dirPath.substring(lastSlash);
            lastDir = lastDir.toLowerCase();
        }

        if (dirPath.contains(".ade_path") || lastDir.contains("dbschema") ||
            lastDir.endsWith("test")) {
            return false;
        }

        if ((lastDir.contains("ui") || lastDir.contains("publicui")) &&
            !lastDir.contains("model")) {
            return false;
        }
        if ((lastDir.contains("di") || lastDir.contains("publicdi")) &&
            !lastDir.contains("model")) {
            return false;
        }


        return true;
    }


    private boolean isValidUIDirectory(String path) {
        if (!isEmpty(path)) {
            if (path.endsWith("ui") || path.endsWith("di")) {
                return true;
            }
        }
        return false;
    }


    private String getAMNameFromDataControlName(String datacontrolName) {
        if (isEmpty(datacontrolName)) {
            return null;
        }
        int lastindexDc = datacontrolName.lastIndexOf("DataControl");
        if (lastindexDc != -1) {
            return datacontrolName.substring(0, lastindexDc);
        }
        return null;
    }

    private String extractIteratorNameFromEL(String elExp) {
        String extractedValue = "";
        if (isEmpty(elExp)) {
            return extractedValue;
        }
        if (elExp.startsWith("#{bindings.")) {
            int indexbindings = elExp.indexOf("#{bindings.");
            int startPos = indexbindings + "#{bindings.".length();
            int indexNextDot = elExp.indexOf(".", startPos);
            if (startPos != -1 && indexNextDot != -1) {
                String iteratorId = elExp.substring(startPos, indexNextDot);
                //System.out.println("IterName is : " + iteratorId);
                return iteratorId;
            } else {
                System.out.println("[ERROR] Could not find iter name from bindings");
            }
        } else if (elExp.contains(".bindings.")) {
            int indexbindings = elExp.indexOf(".bindings.");
            int startPos = indexbindings + ".bindings.".length();
            int indexNextDot = elExp.indexOf(".", startPos);
            if (startPos != -1 && indexNextDot != -1) {
                String iteratorId = elExp.substring(startPos, indexNextDot);
                //System.out.println("IterName is : " + iteratorId);
                return iteratorId;
            } else {
                System.out.println("[ERROR] Could not find iter name from bindings");
            }
        } else if (elExp.startsWith("#{")) {
            int indexbindings = elExp.indexOf("#{");
            int startPos = indexbindings + "#{".length();
            int indexfirstDot = elExp.indexOf(".", startPos);
            startPos = indexfirstDot + ".".length();
            int endPos = elExp.indexOf(".", startPos);
            if (startPos != -1 && endPos != -1) {
                String iteratorId = elExp.substring(startPos, endPos);
                System.out.println("IterName is : " + iteratorId +
                                   ".. elexp is: " + elExp);
                return iteratorId;
            }
        } else {
            System.out.println("Not a valid EL Exp.. should start with #{");

        }
        return extractedValue;
    }

    public String extractAttrValueIfPossibleOrInt(String attrValue,
                                                  PageDef pageDef) {
        String extractedValue = "";
        if (isEmpty(attrValue)) {
            return extractedValue;
        }
        if (attrValue.startsWith("#{")) {
            String iteratorId = extractIteratorNameFromEL(attrValue);
            String searchStr = iteratorId + ".rangeSize";
            int indexSearch = attrValue.indexOf(searchStr);
            if (indexSearch != -1) {
                PageDef.PageDefIteratorBinding iterBinding =
                    pageDef.getIterBindingForTreeId(iteratorId);
                return iterBinding.getRangeSize().toString();
            } else {
                System.out.println("Review this line.. does not map to range size: " +
                                   attrValue);
                return attrValue;
            }

        } else {
            try {
                Integer intValue = Integer.parseInt(attrValue);
                return intValue.toString();
            } catch (NumberFormatException ex) {
                System.out.println("Is not a number: " + ex.getMessage());
                System.out.println("[ERRRRRROR] could not extract attribute value from string: " +
                                   attrValue);
                return attrValue;
            }
        }
    }


    /**
     *checks whether the given string is empty
     * @param str
     * @return
     */
    public boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public File getPageDef(File uiFile, DataBindings bindings) {
        if (uiFile == null) {
            System.out.println("[ERROR] Input file was null");
            return null;
        }
        String absPath = uiFile.getAbsolutePath();
        HashMap<String, String> mapPagesPageDefs = bindings.getPagesPageDef();
        if (mapPagesPageDefs == null) {
            System.out.println("databindings do not have any page refs");
            return null;
        }
        String abstractPath = UiFileHelper.getUIFileAbstractPath(absPath);
        if (UiFileHelper.isEmpty(abstractPath)) {
            System.out.println("ui file abstract path is null");
            return null;
        }
        String pageDefDotPath = mapPagesPageDefs.get(abstractPath);
        if (UiFileHelper.isEmpty(pageDefDotPath)) {
            System.out.println("Could not obtain page def path for file: " +
                               absPath);
            return null;
        }
        String pageDefAbsPath = pageDefDotPath.replaceAll("\\.", "/");
        String adfmsrcPath = UiFileHelper.getAdfmsrcPath(absPath);
        pageDefAbsPath = adfmsrcPath + pageDefAbsPath + ".xml";
        File pageDef = new File(pageDefAbsPath);
        if (pageDef.exists()) {
            return pageDef;
        }
        return null;
    }

    public ArrayList<TuningAnalysisRecord> getList_analysisRecords() {
        return list_analysisRecords;
    }

    public void setBcUsages(HashMap<String, ArrayList<TuningAnalysisRecord>> m_bcUsages) {
        this.m_bcUsages = m_bcUsages;
    }

    public HashMap<String, ArrayList<TuningAnalysisRecord>> getBcUsages() {
        return m_bcUsages;
    }

    public String getPageDefPath() {
        if (m_pagedef != null) {
            return m_pagedef.absPath;
        }

        return null;
    }

    // returns only violating records

    public HashMap<String, ArrayList<TuningAnalysisRecord>> getMapOfMultipleIteratorUsages() {
        if (m_mapOfMultipleIteratorUsages == null) {
            return null;
        }

        return UiFileHelper.getOnlyMultipleUsages(m_mapOfMultipleIteratorUsages);
    }

    public void setRecordMultipleIteratorUsages(boolean recordMultipleIteratorUsages) {
        this.recordMultipleIteratorUsages = recordMultipleIteratorUsages;
    }

    public boolean isRecordMultipleIteratorUsages() {
        return recordMultipleIteratorUsages;
    }

    public ArrayList<TuningAnalysisRecord> getList_errorRecords() {
        return list_errorRecords;
    }


}

