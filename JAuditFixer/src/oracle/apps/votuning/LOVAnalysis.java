package oracle.apps.votuning;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.lovWidth.ViewObjectHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class LOVAnalysis {
    public LOVAnalysis() {

        super();
        initializeComponentIterators();
        initializeKnownVos();
        initializeCommonVOBasePaths();
    }


    private static final String TYPE_TREE = "tree";
    private static final String TYPE_LIST = "list";

    /**
     * Map of the bc uages in this page
     */
    HashMap<String, ArrayList<TuningAnalysisRecord>> m_bcUsages = new HashMap<String, ArrayList<TuningAnalysisRecord>>();

    HashMap<String, ArrayList<TuningAnalysisRecord>> m_mapOfMultipleIteratorUsages = new HashMap<String, ArrayList<TuningAnalysisRecord>>();

    HashMap<String, Integer> map_numberOfComponentsByName = new HashMap<String, Integer>();

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
     * the error logger to log all the errors to.
     * The object is passed in from the caller
     */
    private Logger m_errorLogger = null;


    /**
     * Base lba path.
     * e.g. if the ui file path is: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/public_html/oracle/apps/financials/Test.jspx
     * then the absolute base path is derived from this ui path as:
     * $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/
     */
    private String m_baseLbaPath = "";

    public static boolean bDebug = false;

    private HashMap<String, String> map_component_iterators = new HashMap<String, String>();
    private HashMap<String, String> map_component_type = new HashMap<String, String>();


    /**
     * List of the output records used for analysis purposes
     */
    ArrayList<TuningAnalysisRecord> list_analysisRecords = new ArrayList<TuningAnalysisRecord>();


    /**
     * List of the output records used for analysis purposes
     */
    ArrayList<TuningAnalysisRecord> list_errorRecords = new ArrayList<TuningAnalysisRecord>();
    public int cnt_cmbo_rowLevel = 0;
    public int cnt_cmbo_total = 0;
    public int nestedAmCounter = 0;

    HashMap<String, String> map_knownVoFetchSizes = new HashMap<String, String>();
    HashMap<String, String> commonVOPaths = new HashMap<String, String>();

    public void initializeKnownVos() {
        map_knownVoFetchSizes.put("oracle.apps.fnd.applcore.lookups.model.publicView.LookupPVO", TuningConstants.VO_FETCHSIZE_NOTSET);
        map_knownVoFetchSizes.put("oracle.apps.fnd.applcore.lookups.lookupService.view.LookupVO", TuningConstants.VO_FETCHSIZE_NOTSET);
        map_knownVoFetchSizes.put("oracle.apps.fnd.applcore.lookups.model.publicView.SetIdLookupPVO", TuningConstants.VO_FETCHSIZE_NOTSET);
        map_knownVoFetchSizes.put("oracle.apps.fnd.applcore.nls.model.publicView.TerritoryPVO", TuningConstants.VO_FETCHSIZE_NOTSET);
        map_knownVoFetchSizes.put("oracle.apps.fnd.applcore.nls.territoryService.view.TerritoryVO", TuningConstants.VO_FETCHSIZE_NOTSET);
        map_knownVoFetchSizes.put("oracle.apps.fnd.applcore.nls.model.publicView.CurrencyPVO", TuningConstants.VO_FETCHSIZE_NOTSET);
        map_knownVoFetchSizes.put("oracle.apps.fnd.applcore.applications.model.publicView.ApplicationPVO", TuningConstants.VO_FETCHSIZE_NOTSET);
        map_knownVoFetchSizes.put("oracle.apps.fnd.applcore.setid.model.publicView.SetIdSetPVO", TuningConstants.VO_FETCHSIZE_NOTSET);
        map_knownVoFetchSizes.put("oracle.apps.fnd.applcore.nls.model.publicView.LanguagePVO", TuningConstants.VO_FETCHSIZE_NOTSET);
        map_knownVoFetchSizes.put("oracle.apps.fnd.applcore.flex.kff.model.publicView.KeyFlexFieldStructureInstancePVO",
                                  TuningConstants.VO_FETCHSIZE_NOTSET);
        map_knownVoFetchSizes.put("oracle.apps.fnd.applcore.flex.vst.model.publicView.ValueSetValuePVO", TuningConstants.VO_FETCHSIZE_NOTSET);
        map_knownVoFetchSizes.put("oracle.apps.fnd.applcore.nls.currencyService.view.CurrencyVO", TuningConstants.VO_FETCHSIZE_NOTSET);
    }

    public void initializeCommonVOBasePaths() {
        commonVOPaths.put("oracle.apps.financials.generalLedger.currencies.manageRate.publicView",
                          "/fusionapps/fin/components/ledger/gl/currencies/manageRate/publicView/");

    }

    public boolean isApplcoreVO(String voPackage) {
        if (voPackage.startsWith("oracle.apps.fnd.applcore"))
            return true;
        return false;

    }

    public boolean isComonVOPath(String bcPackage) {
        Set<String> keySet = commonVOPaths.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String commonPackage = keyIter.next();
            if (bcPackage.contains(commonPackage)) {
                return true;
            }
        }
        return false;
    }

    public String getADEViewRoot() {
        int fusionappsIndex = m_uiAbsPath.indexOf("/fusionapps/");
        if(fusionappsIndex == -1)
            fusionappsIndex = m_uiAbsPath.indexOf("/fsm/");
        if(fusionappsIndex == -1)
            fusionappsIndex = m_uiAbsPath.indexOf("/emcore/");
        if(fusionappsIndex == -1)
            fusionappsIndex = m_uiAbsPath.indexOf("/atgpf/");
        return m_uiAbsPath.substring(0, fusionappsIndex);
    }

    public String getCommonVOLocation(String bcPackage) {
        Set<String> keySet = commonVOPaths.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String commonPackage = keyIter.next();
            if (bcPackage.contains(commonPackage)) {
                String packagePath = commonVOPaths.get(commonPackage);
                String adeViewRoot = getADEViewRoot();
                String voBaseLocation = adeViewRoot + packagePath;
                return voBaseLocation;
            }
        }
        return "";
    }

    public void incrementComponentCounter(String componentName) {
        if (map_numberOfComponentsByName.containsKey(componentName)) {
            Integer cnt = map_numberOfComponentsByName.get(componentName);
            cnt++;
            map_numberOfComponentsByName.put(componentName, cnt);
        } else {
            map_numberOfComponentsByName.put(componentName, 1);
        }
    }

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
    public ArrayList<TuningAnalysisRecord> analyzeUIFile(File uiFile, DataBindings bindings, Logger errorLogger) {


        m_uiAbsPath = uiFile.getAbsolutePath();
        String fileName = uiFile.getName();
        if (bDebug) {
            System.out.println("[LOVAnalysis] Analyzing file: " + fileName);
        }
        m_errorLogger = errorLogger;
        if (bindings == null) {
            String err = String.format("[ERROR] Databindings was null.. Skipping: %s", m_uiAbsPath);
            if (bDebug) {
              System.out.println(err);
              m_errorLogger.log(Level.SEVERE, err);
            }
            return null;
        }

        m_dataBindingsCpx = bindings;

        //---------------- get page definition for this file--------------
        File pageDef = getPageDef(uiFile, bindings);
        if (pageDef == null) {
            if (bDebug) {
                String msg = String.format("[No pagedef] Cannot get pageDef for file.. Skipping: %s", m_uiAbsPath);
                System.out.println(msg);
            }
            return null;
        }

        String pageDefAbsPath = pageDef.getAbsolutePath();
        m_pagedef = PageDef.parsePageDef(pageDefAbsPath);
        if (m_pagedef == null) {
          if (bDebug) {
            String msg = String.format("[No pagedef] Cannot parse page def: %s for file.. Skipping: %s", pageDefAbsPath, m_uiAbsPath);
            System.out.println(msg); 
            m_errorLogger.log(Level.INFO, msg); 
          }            
            return null;
        }

        m_baseLbaPath = UiFileHelper.getBaseLBAPath(m_uiAbsPath);
        if (isEmpty(m_baseLbaPath)) {
          if (bDebug) {
            String msg = "[ERROR] Could not get the base lba path for file: " + m_uiAbsPath;
            System.out.println(msg);
            m_errorLogger.log(Level.SEVERE, msg);
          }
            return null;
        }
        //objPageDef.printPageDef();
        //---------------- obtained page def--------------


        // traverse ui file
        XMLDocument uiXml = XMLParserHelper.getXMLDocument(m_uiAbsPath);
        if (uiXml == null) {
          if (bDebug) {
            String msg = String.format("[ERROR] analyzeUIFile: could not parse xml doc for ui file..Skipping file.. %s", m_uiAbsPath);
            System.out.println(msg); 
            m_errorLogger.log(Level.SEVERE, msg);            
          }
          return null;
        }

        Element rootNode = uiXml.getDocumentElement();
        if (rootNode != null && rootNode.hasChildNodes()) {
            traverseNodes(rootNode.getChildNodes(), uiFile);
        }

        return list_analysisRecords;
    }

    private void initializeComponentIterators() {
        map_component_iterators.put("af:treeTable", "value");
        map_component_iterators.put("af:table", "value");
        map_component_iterators.put("af:inputComboboxListOfValues", "model");
        map_component_iterators.put("af:selectOneChoice", "child#f:selectItems#model#value");


        map_component_type.put("af:table", TYPE_TREE);
        map_component_type.put("af:treeTable", TYPE_TREE);
        map_component_type.put("af:inputComboboxListOfValues", TYPE_LIST);
        map_component_type.put("af:selectOneChoice", TYPE_LIST);
    }

    private String getModelAttributeValue(Node componentNode, TuningAnalysisRecord record) {
        String nodeName = componentNode.getNodeName();
        if (isEmpty(nodeName)) {
            return null;
        }

        if (nodeName.equals("af:table") || nodeName.equals("af:treeTable")) {
            String modelValue = XMLParserHelper.getAttributeValue(componentNode, "value");
            record.addCustomAttr(TuningConstants.CUSTOMATTR_MODEL_VALUE, modelValue);
            return modelValue;
        }
        if (nodeName.equals("af:inputComboboxListOfValues")) {
            String modelValue = XMLParserHelper.getAttributeValue(componentNode, "model");
            record.addCustomAttr(TuningConstants.CUSTOMATTR_MODEL_VALUE, modelValue);
            return modelValue;
        }
        if (nodeName.equals("af:selectOneChoice")) {
            Node childModelNode = XMLParserHelper.getChildNodeWithName(componentNode, "f:selectItem");
            if (childModelNode == null) {
                childModelNode = XMLParserHelper.getChildNodeWithName(componentNode, "f:selectItems");
            }
            if (childModelNode == null) {
                childModelNode = XMLParserHelper.getChildNodeWithName(componentNode, "af:selectItems");
            }
            if (childModelNode == null) {
                childModelNode = XMLParserHelper.getChildNodeWithName(componentNode, "af:selectItem");
            }
            if (childModelNode == null) {
                String msg =
                    String.format("[NoModelAttribute] [Possible Nesting] could not retrieve model node. ComponentName %s , looking for child %s .",
                                  nodeName, "model");
                record.setDiagnostic(msg);
                return null;
            }
            String modelValue = XMLParserHelper.getAttributeValue(childModelNode, "value");
            record.addCustomAttr(TuningConstants.CUSTOMATTR_MODEL_VALUE, modelValue);
            return modelValue;
        }

        return null;
    }


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
            
            if (!exempted && !isEmpty(nodeName) && map_component_iterators.containsKey(nodeName)) {
                TuningAnalysisRecord record = new TuningAnalysisRecord();

                record.setComponentName(nodeName);
                String type = map_component_type.get(record.getComponentName());
                record.setComponentType(type);
                incrementComponentCounter(nodeName);
                record.setUifilePath(uiFilePath);
                record.setUifileName(uiFile.getName());
                record.setPageDefFilePath(m_pagedef.absPath);
                String productName = UiFileHelper.getProductName(uiFilePath);
                record.setProduct(productName);
                String compId=XMLParserHelper.getAttributeValue(child, "id");
                if(!isEmpty(compId))
                    record.setComponentId(compId);
                // update properties from the jspx/jsff component e.g. autoheight for tables
                updateComponentPageAttributes(child, record);

                String modelValue = getModelAttributeValue(child, record);
                if (isEmpty(modelValue)) {
                    list_errorRecords.add(record);
                    if (bDebug) 
                       m_errorLogger.log(Level.SEVERE, "Could not retrive model value for node.. if list - > maybe case of nesting"); 
                    continue;
                }

                if (!isPossibleELExpression(modelValue)) {
                    // not an error, because the select items can be a static list
                    if (nodeName.equals("af:selectOneChoice")) {
                        continue;
                    }

                    String msg =
                        String.format("[ERROR] : traverseNodes: Skipping file: %s : Cannot recognize format of value node: %s.. AFAIK value node should always map to EL Exp and only then can we recover the itetrator binding.. ",
                                      uiFilePath, modelValue);
                    if (bDebug) 
                       m_errorLogger.log(Level.SEVERE, msg); 
                    record.setDiagnostic(record.getDiagnostic() + msg + "\n");
                    list_errorRecords.add(record);
                    continue;
                }

                try {
                    updateIteratorProperties(child, modelValue, record);
                } catch (Exception ex) {
                    //String msg = "[ERROR:UpdateIteratorProperties]: " + ex.getMessage();
                     if (bDebug) 
                         m_errorLogger.log(Level.SEVERE, record.getDiagnostic()); 
                    // record.setDiagnostic(record.getDiagnostic() + msg + "\n");
                    list_errorRecords.add(record);
                    continue;
                }

                //if (type.equals(TYPE_TREE)) {
                //    System.out.println("**** dont care bout tree for now");
                //} else if (type.equals(TYPE_LIST)) {
                try {
                    doModelLayerProcessing(record);
                } catch (Exception ex) {
                    String msg = "****[LOV]Exception: " + ex.getMessage();
                    if (bDebug) 
                       m_errorLogger.log(Level.SEVERE, msg);
                    record.setDiagnostic(record.getDiagnostic() + msg + "\n");
                    list_errorRecords.add(record);
                    continue;
                }
                //}
                // common processing for tree and lov : from iterator binding get the am,vo
                if (record != null) {

                    updateMultipleBcUsages(record);
                    updateMultipleIteratorUsages(record);
                    list_analysisRecords.add(record);
                }

            }

            if (child.hasChildNodes()) {
                traverseNodes(child.getChildNodes(), uiFile);
            }
        }
    }

    public void updateComponentPageAttributes(Node componentNode, TuningAnalysisRecord record) {


        String componentNodeName = componentNode.getNodeName();
        if (componentNodeName.equals("af:table") || componentNodeName.equals("af:treeTable")) {
            record.setAutoHeightRows(XMLParserHelper.getAttributeValue(componentNode, "autoHeightRows"));
            String contentDelivery = XMLParserHelper.getAttributeValue(componentNode, "contentDelivery");
            if (!isEmpty(contentDelivery)) {
                record.addCustomAttr(TuningConstants.CUSTOMATTR_TREE_CONTENTDEL, contentDelivery);
            }
            String rows = XMLParserHelper.getAttributeValue(componentNode, "rows");
            if (!isEmpty(rows)) {
                record.addCustomAttr(TuningConstants.CUSTOMATTR_TREE_ROWS, rows);
            }
            String tableFetchSize = XMLParserHelper.getAttributeValue(componentNode, "fetchSize");
            if (!isEmpty(tableFetchSize)) {
                record.addCustomAttr(TuningConstants.CUSTOMATTR_TREE_TABLE_FETCHSIZE, tableFetchSize);
            }
        }
    }

    public String getTableIteratorIdForRow(Node componentNode) {
        String iteratorId = "";
        Node traversalNode = componentNode;
        while (true) {
            Node parentNode = traversalNode.getParentNode();
            String parentNodeName = parentNode.getNodeName();
            if (isEmpty(parentNodeName)) {
                break;
            }
            if (parentNodeName.equals("af:table") || parentNodeName.equals("af:treeTable")) {
                String modelValue = XMLParserHelper.getAttributeValue(parentNode, "value");
                iteratorId = extractIteratorNameFromEL(modelValue);
                break;
            }
            if (parentNodeName.equals("jsp:root")) {
                break;
            }
            traversalNode = parentNode;
        }

        return iteratorId;
    }

    public String updateIteratorProperties(Node componentNode, String modelValue, TuningAnalysisRecord record) throws Exception {

        String componentName = record.getComponentName();
        String type = map_component_type.get(componentName);
        String modelIteratorId = extractIteratorNameFromEL(modelValue);
        String iteratorBindingId = "";
        if (type.equals(TYPE_TREE)) {
            // System.out.println("**** dont care bout tree for now");
            // gets the page def iterator binding
            PageDef.PageDefIteratorBinding pagedefIterBinding = m_pagedef.getIterBindingForTreeId(modelIteratorId);
            if (pagedefIterBinding == null) {
                String msg =
                    String.format("[IteratorBinding] Could not obatin iterator binding for tree id: %s, ui file %s", modelIteratorId, m_uiAbsPath);
                record.setDiagnostic(msg);
                throw new Exception(msg);
            }
            record.setPageDefIteratorName(pagedefIterBinding.getId());
            if (pagedefIterBinding.getRangeSize() != null) {
                record.setIteratorRangeSize(pagedefIterBinding.getRangeSize().toString());
            } else {
                record.setIteratorRangeSize(null);
            }

        }
        if (type.equals(TYPE_LIST)) {
            cnt_cmbo_total++;
            if (isRowLevelBinding(modelValue)) {
                cnt_cmbo_rowLevel++;
                record.addCustomAttr(TuningConstants.LOV_ROW_LEVEL, "true");
                // all row level are model driven
                record.addCustomAttr(TuningConstants.LOV_TYPE, TuningConstants.LOV_TYPE_MODEL_DRIVEN);
                String tableIteratorId = getTableIteratorIdForRow(componentNode);
                if (isEmpty(tableIteratorId)) {
                    String msg =
                        String.format("[IterBindingProcessing]could not obtain table iterator id while processing row level lov.. modelValue: %s",
                                      modelValue);
                    record.setDiagnostic(msg);
                    m_errorLogger.log(Level.SEVERE, msg); 
                    throw new Exception(msg);
                }
                PageDef.PageDefIteratorBinding iterBinding = m_pagedef.getIterBindingForTreeId(tableIteratorId);
                if (iterBinding == null) {
                    String msg =
                        String.format("[IterBindingProcessing] [RowBinding] Could not obtain the iterator binding for component: %s, in ui file: %s.. Looking for iteratorId: %s",
                                      componentName, m_uiAbsPath, tableIteratorId);
                    record.setDiagnostic(msg + " maybe row level lov issue");
                    m_errorLogger.log(Level.SEVERE, msg); 
                    throw new Exception(msg);
                }
                record.setPageDefIteratorName(iterBinding.getId());
                if (iterBinding.getRangeSize() != null) {
                    record.setIteratorRangeSize(iterBinding.getRangeSize().toString());
                } else {
                    record.setIteratorRangeSize(null);
                }
                iteratorBindingId = iterBinding.getId();
                record.addCustomAttr(TuningConstants.LOV_ROW_LEVEL_ATTR, modelIteratorId);
                // increment row level counter
            } else {
                // gets the page def iterator binding
                PageDef.pageDefListBindings listBinding = m_pagedef.getListBindingForId(modelIteratorId);
                if (listBinding == null) {
                    String msg =
                        String.format("[ListBindingNotObtained] [TODO: Possible null ListIter] Could not obtain list binding for iteratorId %s, ui file: %s",
                                      modelIteratorId, m_uiAbsPath);
                    record.setDiagnostic(msg);
                    throw new Exception(msg);
                }
                record.addCustomAttr(TuningConstants.LOV_TYPE, listBinding.getLovType());
                record.setLovUsesName(listBinding.getLovUses());
                PageDef.PageDefIteratorBinding iterBinding = null;
                if (listBinding.getLovType().equals(TuningConstants.LOV_TYPE_DYNAMICLIST)) {
                    record.addCustomAttr(TuningConstants.LOV_DYNAMICLIST_LISTITER, listBinding.getListIter());
                    iterBinding = m_pagedef.getIteratorBindingWithId(listBinding.getListIter());
                    //record.setDiagnostic("[TODO] Dynamic list ");
                    //throw new Exception("[TODO] Dynamic list ");

                } else if (listBinding.getLovType().equals(TuningConstants.LOV_TYPE_NAVIGATION)) {
                    record.addCustomAttr(TuningConstants.LOV_DYNAMICLIST_LISTITER, listBinding.getListIter());
                    iterBinding = m_pagedef.getIteratorBindingWithId(listBinding.getListIter());
                    //record.setDiagnostic("[TODO] Navigation list ");
                    //throw new Exception("[TODO] Navigation list ");
                } else {
                    iterBinding = m_pagedef.getIterBindingForListId(modelIteratorId);
                }

                if (iterBinding == null) {
                    String msg =
                        String.format("Could not obtain the iterator binding for component: %s, in ui file: %s.. modelIteratorId %s", componentName,
                                      m_uiAbsPath, modelIteratorId);
                    record.setDiagnostic(msg);
                    throw new Exception(msg);
                }
                record.setPageDefIteratorName(iterBinding.getId());
                if (iterBinding.getRangeSize() != null) {
                    record.setIteratorRangeSize(iterBinding.getRangeSize().toString());
                } else {
                    record.setIteratorRangeSize(null);
                }


                iteratorBindingId = iterBinding.getId();
            }
        }
        return iteratorBindingId;
    }


    public void setAmLevelPropertiesInRecord(String iteratorId, TuningAnalysisRecord record) throws Exception {
        PageDef.PageDefIteratorBinding iterBinding = m_pagedef.mapIterators.get(iteratorId);
        if (iterBinding == null) {
            String msg =
                String.format("[ERROR] :updateVOPropertiesInRecord: Could not find iterator Id: %s in page def %s Skipping this record in the UI File %s",
                              iteratorId, m_pagedef.absPath, m_uiAbsPath);
            if (bDebug) {
                System.out.println(msg);
                m_errorLogger.log(Level.SEVERE, msg);
            }
            
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        String voInstanceName = iterBinding.getBindName();
        String datacontrolName = iterBinding.getDataControlName();
        if (datacontrolName == null) {
            record.setDiagnostic(record.getDiagnostic() + "could not obtain data control name from iterator binding: " + iteratorId + "\n");

            String msg =
                String.format("[ERROR] :updateVOPropertiesInRecord: page def entry for iterator Binding %s does not have an entry for 'DataControlName'. uiFile: %s.. pagedef path: %s cpx path: %s.. skipping",
                              iteratorId, m_uiAbsPath, m_dataBindingsCpx.getCpxFullPath(), m_pagedef.absPath);
            if (bDebug) {
                System.out.println(msg);
                m_errorLogger.log(Level.SEVERE, msg); 
            }            
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        String amName = getAMNameFromDataControlName(datacontrolName);
        if (amName == null) {
            record.setDiagnostic(record.getDiagnostic() + "could not obtain am name from data control: " + datacontrolName + "\n");

            String msg =
                String.format("[ERROR] :updateVOPropertiesInRecord:  could not obtain am name from data control: %s uiFile: %s..", datacontrolName,
                              m_uiAbsPath);
            if (bDebug) {
                System.out.println(msg);
                m_errorLogger.log(Level.SEVERE, msg); 
            }            
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }
        String dcPackage = m_dataBindingsCpx.getDataControlPackage(datacontrolName);
        if (isEmpty(dcPackage)) {

            record.setDiagnostic(record.getDiagnostic() + "could not obtain datacontorl package name for: " + datacontrolName + "\n");

            String msg =
                String.format("[ERROR] :updateVOPropertiesInRecord:  could not obtain data control package name for datacontrol name: %s, Iterator Id: %s, databindings.cpx file path: %s uiFile: %s..",
                              datacontrolName, iteratorId, m_dataBindingsCpx.cpxFullPath, m_uiAbsPath);
            if (bDebug) {
                System.out.println(msg);
                m_errorLogger.log(Level.SEVERE, msg);
            }            
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }
        record.setAmFullPackage(dcPackage + "." + amName);
        record.setVoInstanceName(voInstanceName);

    }
    // main link between ui and model is the iterator id

    private boolean isRowLevelLOV(TuningAnalysisRecord record) {

        String isRowLevel = record.getCustomAttrWithKey(TuningConstants.LOV_ROW_LEVEL);
        if (!isEmpty(isRowLevel) && isRowLevel.equals("true"))
            return true;

        return false;
    }

    public void doModelLayerProcessing(TuningAnalysisRecord record) throws Exception {
        String iteratorId = record.getPageDefIteratorName();
        if (m_pagedef == null || isEmpty(iteratorId) || record == null) {
            String msg = "page def is null or iterator Id is null";
            if (bDebug) {
                System.out.println(msg);
            }
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        setAmLevelPropertiesInRecord(iteratorId, record);

        //------------------------GET AM-----------------------------------
        /**
         * Typically the ams are in the same project space.
         * e.g. if the ui is in $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/allocations/ui/public_html
         * then the model am is typically under $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/allocations/<model> or <publicmodel> or <protectedmodel>
         * Instead of going into the imported jars.. typically you should first look in this directory structure.. it will save time.
         */
        //System.out.println("Looking for am: " + amPathToFind);
        String amFullPackagePath = record.getAmFullPackage();
        String amAbsPath = getAmFile(amFullPackagePath);
        record.setAmPath(amAbsPath);
        // even though we can proceed to analyze with the amxml we should not -because such files cannot be fixed.. We need the absolute am file to do any fixes. hence throw exception here
        if (isEmpty(amAbsPath)) {
            String msg = "[AM Not Obtained] AM file could not be obtained: " + amFullPackagePath;
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        if (bDebug) {
            System.out.println("Found am in: " + amAbsPath);
        }
        //------------------------[END]GET AM-----------------------------------

        String voInstanceName = record.getVoInstanceName();
        if (!isEmpty(voInstanceName) && voInstanceName.startsWith("Root.")) {
            // for now lets not worry about nested AMs
            nestedAmCounter++;
            String msg = String.format("TODO: NestedAM: %s , am absolute Path %s, UI File %s", voInstanceName, amAbsPath, m_uiAbsPath);
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        // update the vo full package
        getViewUsagePackageFromAm(amAbsPath, voInstanceName, record, false);
        // if needs tree processing: update amvousage fetch size
        // else if needs list processing - get lov usages

        if (needsTreeProcessing(record)) {
            record.addCustomAttr("treeTypeProcessing", "true");
            processVOForTree(record);
        } else {
            // needs list type processing
            processVOForList(record);
        }


    }

    public boolean needsTreeProcessing(TuningAnalysisRecord record) {
        String compName = record.getComponentName();
        if (compName.equals("af:table") || compName.equals("af:treeTable")) {
            return true;
        }

        if (compName.equals("af:inputComboboxListOfValues") || compName.equals("af:selectOneChoice")) {
            String lovType = record.getCustomAttrWithKey(TuningConstants.LOV_TYPE);
            if (!isEmpty(lovType) && lovType.trim().equals(TuningConstants.LOV_TYPE_NAVIGATION)) {
                return true;
            }
            if (!isEmpty(lovType) && lovType.trim().equals(TuningConstants.LOV_TYPE_DYNAMICLIST)) {
                return true;
            }
            if (!isEmpty(lovType) && lovType.trim().equals(TuningConstants.LOV_TYPE_MODEL_DRIVEN)) {
                return false;
            }
            if (!isEmpty(lovType) && lovType.trim().equals(TuningConstants.LOV_TYPE_DYNAMICLIST)) {
                return false;
            }

        }

        return false;
    }

    public void updateVOUsageFetchSize(TuningAnalysisRecord record) throws Exception {
        String amAbsPath = record.getAmPath();
        String voUsageName = record.getVoInstanceName();
        if (isEmpty(amAbsPath)) {
            String msg = String.format("Am path not found.. ui file: %s", m_uiAbsPath);
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        XMLDocument amXml = XMLParserHelper.getXMLDocument(amAbsPath);

        NodeList appmoduleNodeList = amXml.getElementsByTagName("AppModule");
        if (!XMLParserHelper.hasOneNOnlyOneNode(appmoduleNodeList)) {
            String msg =
                String.format("[updateVOUsageFetchSize] : updateVoFetchSize : Expecting one and only one node tag AppModule.. Skipping file: %s",
                              m_uiAbsPath);
            if (bDebug) {
               System.out.println(msg); 
               m_errorLogger.log(Level.SEVERE, msg);
            }
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        Node appModuleNode = appmoduleNodeList.item(0);
        NodeList appModuleChildren = appModuleNode.getChildNodes();
        if (appModuleChildren == null) {
            String msg = "[VOUsageNotFound] VO usage not found in am.. no child nodes in this appmodule tag";
            if (bDebug) {
               System.out.println(msg);
            }
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        int appModuleChildLen = appModuleChildren.getLength();
        boolean bFoundVoUsage = false;
        String voUsagePackage = "";
        String voUsageFetchSize = "";
        for (int i = 0; i < appModuleChildLen; i++) {
            Node appModuleChild = appModuleChildren.item(i);
            String tagName = appModuleChild.getNodeName();
            if (!isEmpty(tagName) && tagName.trim().equals("ViewUsage")) {
                String obtainedVoUsageName = XMLParserHelper.getAttributeValue(appModuleChild, "Name");

                if (!isEmpty(obtainedVoUsageName) && obtainedVoUsageName.trim().equals(voUsageName)) {
                    if (bFoundVoUsage == true) {
                        String msg =
                            String.format("[WARNING]: updateVoFetchSize: file might be corrupt.. duplicate vo usage: %s in am file: %s", obtainedVoUsageName,
                                          amAbsPath);
                        if (bDebug) {
                           System.out.println(msg);
                           m_errorLogger.log(Level.WARNING, msg); 
                        }
                        record.setDiagnostic(record.getDiagnostic() + " * duplicate vo usage detected in am: " + amAbsPath);
                        // At the time of fixing this will be a problem.. hence throw an exception.. we only want to collect the good or can be fixed records
                        throw new Exception(msg);
                    }
                    bFoundVoUsage = true;
                    voUsagePackage = XMLParserHelper.getAttributeValue(appModuleChild, "ViewObjectName");
                    voUsageFetchSize = XMLParserHelper.getAttributeValue(appModuleChild, "FetchSize");
                }
            }
        }

        // at this point you should have the voUsageName, ViewObjectName and FetchSize
        if (isEmpty(voUsagePackage)) {

            String msg =
                String.format("[ERROR] VO Package for VO: %s could not be obtained from am file: %s .. UI File: %s", voUsageName, amAbsPath, m_uiAbsPath);
            if (bDebug) {
               System.out.println(msg);
               m_errorLogger.log(Level.SEVERE, msg);
            }
            record.setDiagnostic(msg);
            throw new Exception(msg);
        } else {
            record.setVoFullPackage(voUsagePackage);
        }
        if (!bFoundVoUsage) {
            String msg = String.format("did not find view usage %s in am: %s", voUsageName, amAbsPath);
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        if (voUsageFetchSize != null) {
            record.setVoUsageFetchSizeInAm(voUsageFetchSize);
        }

    }

    public void processVOForTree(TuningAnalysisRecord record) throws Exception {

        if (record == null) {
            record.setDiagnostic("Record was null");
            throw new Exception("record was null");
        }

        // udate the AM:VOUsage fetch size
        updateVOUsageFetchSize(record);

        // update the base fetch size
        XMLDocument voXml = getViewObject(record.getAmPath(), record.getVoFullPackage(), record, true, null);

        String voPath = record.getVoPath();
        boolean voFileNotObtained = false;
        if (!isEmpty(voPath) && voPath.startsWith("JAR:")) {
            voFileNotObtained = true;
        }
        if (voFileNotObtained) {
            String msg = "[BaseVONotFound] Could not obtain vo file for package: " + record.getVoFullPackage();
            if(bDebug)
               m_errorLogger.log(Level.SEVERE, msg); 
            record.setDiagnostic(msg);
            // no need for exception throw new Exception("cannot obtain vo for record: " + voPackage);
        }
        if (voXml == null) {
            String baseVoPackage = record.getVoFullPackage();
            if (isEmpty(baseVoPackage)) {
                String msg1 =
                    String.format("[UpdateBaseVOFetch] could not find vo package when tryin to update fetch size from known vos %s", m_uiAbsPath);
                record.setDiagnostic(msg1);
                throw new Exception(msg1);
            }
            boolean couldupdateDefaultFetch = updateDefaultFetchSizeIfKnownVO(baseVoPackage, record);
            if (!couldupdateDefaultFetch) {
                String msg = "[processVOForTree] voxml was null: " + record.getVoFullPackage();
                record.setDiagnostic(msg);
                throw new Exception(msg);
            }
            return;
        }
        record.setBaseVoPath(voPath);
        updateDefaultVOFetchSize(voPath, voXml, record);
        //If the VO FetchSize is not set, check to see if this VO extends another VO. 
        // If the VO extends another VO, the default fetchsize will come from the vo that is extended from. 
        checkDefaultVOForExtendsCaseForTable(voXml,record);
    }


    public void processVOForList(TuningAnalysisRecord record) throws Exception {

        if (record == null) {
            record.setDiagnostic("Record was null");
            throw new Exception("record was null");
        }
        String amAbsPath = record.getAmPath();
        String voPackage = record.getVoFullPackage();
        // get the vo
        String voPath = getBCObjectPath(amAbsPath, voPackage, record);
        //XMLDocument voXml = getViewObject(amAbsPath, voPackage, record, true);

        if (isEmpty(voPath)) {
            if(bDebug)
               m_errorLogger.log(Level.SEVERE, "Could not obtain vo file for package:" + voPackage); 
            record.setDiagnostic("**** Could not obtain vo file for package: " + voPackage);
            throw new Exception("cannot obtain vo for record: " + voPackage);
        }
        record.setVoPath(voPath);
        XMLDocument voXml = XMLParserHelper.getXMLDocument(voPath);
        // if row level lov, get the lov uses
        if (isRowLevelLOV(record)) {
            updateLOVUsesForAttribute(voXml, record);
        }
        // updates mrucount, list range size, vo fetch size
        updateVOLOVProperties(voXml, record, record.getLovUsesName());

    }

    public boolean updateLOVUsesForAttribute(XMLDocument voXml, TuningAnalysisRecord record) throws Exception {
        String voPath = record.getVoPath();
        if (voXml == null) {
            throw new Exception("Could not obtain vo xml");
        }
        NodeList listViewObj = voXml.getElementsByTagName("ViewObject");
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
            String msg = "Expecting one and only one view object node in vo file: " + voPath;
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }
        String lovAttr = record.getCustomAttrWithKey(TuningConstants.LOV_ROW_LEVEL_ATTR);
        Node voNode = listViewObj.item(0);
        Node attrNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(voNode, "ViewAttribute", "Name", lovAttr);
        if (attrNode == null) {
            String msg = String.format("Could not obtain row level lov attribute in vo: %s. LOV Attribute %s", voPath, lovAttr);
            // System.out.println(msg);
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }
        String lovUses = XMLParserHelper.getAttributeValue(attrNode, "LOVName");
        if (isEmpty(lovUses)) {
            String msg = String.format("could not obtain lov name for attribute %s, in VO %s", lovAttr, voPath);
            record.setDiagnostic(msg);
            // System.out.println(msg);
            throw new Exception(msg);
        }
        record.setLovUsesName(lovUses);
        return true;
    }

    public boolean updateVOLOVProperties(XMLDocument voXml, TuningAnalysisRecord record, String lovUses) throws Exception {
        String voPath = record.getVoPath();
        if (voXml == null) {
            throw new Exception("Could not obtain vo xml");
        }

        NodeList listViewObj = voXml.getElementsByTagName("ViewObject");
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
            //    System.out.println("Expecting one and only one view object node");
            throw new Exception("Expecting one and only one view object node in vo file: " + voPath);
        }
        Node voNode = listViewObj.item(0);
        Node listBindingNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(voNode, "ListBinding", "Name", lovUses);
        if (listBindingNode == null) {
            throw new Exception("Could not find listbinding with name: " + lovUses + " in file: " + voPath);
        }
        String listRangeSize = XMLParserHelper.getAttributeValue(listBindingNode, "ListRangeSize");
        String mruCount = XMLParserHelper.getAttributeValue(listBindingNode, "MRUCount");
        record.addCustomAttr(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE, listRangeSize);
        record.addCustomAttr("MRUCount", mruCount);
        String listDataSource = XMLParserHelper.getAttributeValue(listBindingNode, "ListVOName");
        if (isEmpty(listDataSource)) {
            throw new Exception("Could not obtain the List data source for the lov name: " + lovUses + " vo file: " + voPath);
        }
        // get view accessor matching the list data source
        String listVOName = updateViewAccessorProperties(voXml, listDataSource, record);
        if (isEmpty(listVOName)) {
            String msg = "List VO could not be botained " + listDataSource;
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }
        String refPath = record.getVoPath();
        StringBuffer voPathObtained = new StringBuffer();
        XMLDocument listVOXml = getViewObject(refPath, listVOName, record, false, voPathObtained);
        // if we dont find the list vo as file its ok.. we only need to change the view usage fetch size
        // however this maybe an issue if we are settign the usageFetchSize <default fetch size
        // TODO: decide how to handle this case at the time of fixing
        String listvoPath = voPathObtained.toString();
        if (isEmpty(listvoPath)) {
            record.setVoDefaultFetchSize(TuningConstants.VO_BASEVO_NOT_FOUND);
            record.setDiagnostic("Could not find list vo as file");
        } else {
            record.addCustomAttr(TuningConstants.CUSTOMATTR_BASELISTVOPATH, listvoPath);
        }

        if (listVOXml == null) {
            String listVO = record.getCustomAttrWithKey(TuningConstants.CUSTOMATTR_LISTVONAME);
            if (isEmpty(listVO)) {
                String msg1 =
                    String.format("[updateDefaultFetch] listVO was null when trying to find default fetch size for known vos: %s", m_uiAbsPath);
                record.setDiagnostic(msg1);
                throw new Exception(msg1);
            }
            boolean couldUpdateDefaultFetch = updateDefaultFetchSizeIfKnownVO(listVO, record);
            if (!couldUpdateDefaultFetch) {
                String msg = "[BaseVONotFound] Could not find vo: " + listVOName;
                record.setDiagnostic(msg);
                record.setVoDefaultFetchSize(TuningConstants.VO_BASEVO_NOT_FOUND);
                m_errorLogger.log(Level.FINER, msg); 
                // dont throw exception coz its still ok if we dont find the base VO
                throw new Exception(msg);
            }
            return true;
        }
        record.setBaseVoPath(listvoPath);
        updateDefaultVOFetchSize(listvoPath, listVOXml, record);
        checkDefaultVOForExtendsCaseForList(listVOXml,record);
        // get the vo path
        // get the vo usage fetch size on the list data source
        // get the vo file and the default fetch size
        return true;
    }

    public boolean updateDefaultFetchSizeIfKnownVO(String voPackage, TuningAnalysisRecord record) throws Exception {

        if (isEmpty(voPackage)) {
            String msg = "vo package null";
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }
        if (map_knownVoFetchSizes.containsKey(voPackage)) {
            record.setVoDefaultFetchSize(map_knownVoFetchSizes.get(voPackage));
            record.setDiagnostic("ApplcoreVO");
            return true;
        }
        if (isApplcoreVO(voPackage)) {
            record.setVoDefaultFetchSize("[ApplcoreVO] Cant obtain vo");
        }
        return false;
    }

    private boolean updateDefaultVOFetchSize(String voPath, XMLDocument voXml, TuningAnalysisRecord record) throws Exception {
        if (voXml == null) {
            throw new Exception("[updateDefaultVOFetchSize] voXml was null");
        }
        String fetchSize = "";
        NodeList listViewObj = voXml.getElementsByTagName("ViewObject");
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
            // System.out.println("Expecting one and only one view object node");
            throw new Exception("[updateDefaultVOFetchSize] expecting one and only one vo node");
        }

        Node voNode = listViewObj.item(0);
        String obtFetchSize = XMLParserHelper.getAttributeValue(voNode, "FetchSize");
        if (obtFetchSize == null) {
            fetchSize = TuningConstants.VO_FETCHSIZE_NOTSET;
        } else {
            fetchSize = obtFetchSize;
        }
        
        
       
        
        record.setVoDefaultFetchSize(fetchSize);
        return true;
    }

    private void checkDefaultVOForExtendsCaseForTable(XMLDocument voXml, TuningAnalysisRecord record) throws Exception
    {
        int maxNestingLevel = 5;
        int nestingCount = 0;
        
        if(record ==null)
            return;
       
        
        if(voXml==null)
            return;
        
        while(nestingCount<maxNestingLevel)
        {
            nestingCount++;
            
            String defaultFetchSize = record.getVoDefaultFetchSize();
            // default fetch size has been set to some value
            if(!isEmpty(defaultFetchSize) && !defaultFetchSize.equals(TuningConstants.VO_FETCHSIZE_NOTSET))
                return;
            
            String extendsVo = ViewObjectHelper.getExtendedVOPackage(voXml);
            if(isEmpty(extendsVo))
                return;
           
            // ==> this vo does not have a fetchsize set and it extends another vo. The default fetch size will now come from the extended vo.
            
            // clear base vo attrs coz they are now wrong since it extends another vo
            record.setVoFullPackage(extendsVo);
            record.setBaseVoPath("");
            record.setVoDefaultFetchSize("");
            voXml = getViewObject(record.getAmPath(), extendsVo, record, true, null);
            if(voXml==null)
            {
                // check if it is a know vo
                boolean couldupdateDefaultFetch = updateDefaultFetchSizeIfKnownVO(extendsVo, record);
                if (!couldupdateDefaultFetch) {
                    String msg = "[checkDefaultVOForExtendsCaseForTable] voxml was null: " + record.getVoFullPackage();
                    record.setDiagnostic(msg);
                    throw new Exception(msg);
                }
                
            }else{
                String fetchSize = ViewObjectHelper.getFetchSize(voXml);
                if(fetchSize==null || isEmpty(fetchSize))
                    record.setVoDefaultFetchSize(TuningConstants.VO_FETCHSIZE_NOTSET);
                else
                    record.setVoDefaultFetchSize(fetchSize);
            }
            
        }
    }
    
    private void checkDefaultVOForExtendsCaseForList(XMLDocument voXml, TuningAnalysisRecord record) throws Exception
    {
        int maxNestingLevel = 5;
        int nestingCount = 0;
        
        if(record ==null)
            return;
       
        
        if(voXml==null)
            return;
        
        if(record.getUifileName().contains("DeductionSearch") && record.pageDefIteratorName.equals("DeductionTypeSearch1Iterator"))
        {
            int me = 0;
            int ke = 0;
            }
        while(nestingCount<maxNestingLevel)
        {
            nestingCount++;
            
            String defaultFetchSize = record.getVoDefaultFetchSize();
            // default fetch size has been set to some value
            if(!isEmpty(defaultFetchSize) && !defaultFetchSize.equals(TuningConstants.VO_FETCHSIZE_NOTSET))
                return;
            
            String extendsVo = ViewObjectHelper.getExtendedVOPackage(voXml);
            if(isEmpty(extendsVo))
                return;
           
            // ==> this vo does not have a fetchsize set and it extends another vo. The default fetch size will now come from the extended vo.
            
            // clear base vo attrs coz they are now wrong since it extends another vo
           // record.setVoFullPackage(extendsVo);
            record.addCustomAttr(TuningConstants.CUSTOMATTR_LISTVONAME, extendsVo);
            record.setBaseVoPath("");
            record.setVoDefaultFetchSize("");
            StringBuffer voPathObtained = new StringBuffer();
            voXml = getViewObject(record.getVoPath(), extendsVo, record, false, voPathObtained);
            if(voXml==null)
            {
                // check if it is a know vo
                boolean couldupdateDefaultFetch = updateDefaultFetchSizeIfKnownVO(extendsVo, record);
                if (!couldupdateDefaultFetch) {
                    String msg = "[checkDefaultVOForExtendsCaseForTable] Could not obtain base VO (either as xml or file) for package: " + extendsVo;
                    record.setDiagnostic(msg);
                    throw new Exception(msg);
                }
                
            }else{
                // update the path that was found for this base vo
                String newBaseVoPath = voPathObtained.toString();
                if(!isEmpty(newBaseVoPath))
                {
                    record.setBaseVoPath(voPathObtained.toString());
                    record.addCustomAttr(TuningConstants.CUSTOMATTR_BASELISTVOPATH, newBaseVoPath);
                }
                else{
                        record.setVoDefaultFetchSize(TuningConstants.VO_BASEVO_NOT_FOUND);
                        record.setDiagnostic("Could not find list vo as file");
                }
                               
                String fetchSize = ViewObjectHelper.getFetchSize(voXml);
                if(fetchSize==null || isEmpty(fetchSize))
                    record.setVoDefaultFetchSize(TuningConstants.VO_FETCHSIZE_NOTSET);
                else
                    record.setVoDefaultFetchSize(fetchSize);
            }
            
        }
    }
    
    private boolean isEOBasedVA(String dataSourceName) {
        if (!isEmpty(dataSourceName) && dataSourceName.contains(".")) {
            return true;
        }
        return false;
    }

    private String getEOUsageFromVOXml(XMLDocument voXml, String EOName) throws Exception {
        if (voXml == null) {
            throw new Exception("Could not obtain vo xml");
        }
        NodeList listViewObj = voXml.getElementsByTagName("ViewObject");
        // should never happen
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
            // System.out.println("Expecting one and only one view object node");
            throw new Exception("Expecting one and only one view object node in vo file: ");
        }
        Node voNode = listViewObj.item(0);
        Node eoUsageNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(voNode, "EntityUsage", "Name", EOName);
        if (eoUsageNode == null) {
            return null;
        }
        return XMLParserHelper.getAttributeValue(eoUsageNode, "Entity");

    }

    private String updateViewAccessorProperties(XMLDocument voXml, String dataSourceName, TuningAnalysisRecord record) throws Exception {
        String voPath = record.getVoPath();
        if (voXml == null) {
            throw new Exception("Could not obtain vo xml");
        }
        XMLDocument vaXml = voXml;
        boolean isEoBasedVA = false;
        String viewAccessorName = dataSourceName;
        String bcPath = voPath;
        if (isEOBasedVA(dataSourceName)) {
            if (bDebug) {
                System.out.println("EOBAsed VA !!");
            }
            isEoBasedVA = true;
            String[] dataSourceParts = dataSourceName.split("\\.");
            if (dataSourceParts == null || dataSourceParts.length < 2) {
                record.setDiagnostic("Not an EOBased VO.. strange.. dont know what this is: " + dataSourceName + " vo name: " + voPath);
                throw new Exception("Not even an eo based va");
            }
            String eoName = dataSourceParts[0];
            viewAccessorName = dataSourceParts[1];
            String eoPackage = getEOUsageFromVOXml(voXml, eoName);
            //String amPath = record.getAmPath();
            String modelJprLocation = UiFileHelper.getJprLocationForModelFile(record.getVoPath());
            String eoPath = getBCObjectPath(modelJprLocation, eoPackage, record);
            //vaXml = getViewObject(amPath, eoPackage, record, false);
            if (isEmpty(eoPath)) {
                throw new Exception("could not get entity for view accessor: " + eoName + ", va" + viewAccessorName);
            }
            record.addCustomAttr("isEOBasedVA", "true");
            record.addCustomAttr("EOBasedVAPackage", eoPackage);
            bcPath = eoPath;
            vaXml = XMLParserHelper.getXMLDocument(bcPath);
            if (vaXml == null) {
                String msg =
                    String.format("Could not obtain eo xml. eo Path %s, vo Path that contains listBinding %s, ui file %s ", eoPath, voPath, m_uiAbsPath);
                record.setDiagnostic(msg);
                throw new Exception(msg);
            }

        }

        Node vaParentNode = null;
        if (isEoBasedVA) {
            NodeList listViewObj = vaXml.getElementsByTagName("Entity");
            if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
                //  System.out.println("Expecting one and only one entity object node");
                record.setDiagnostic("Expecting one and only one entity node in eo file");
                throw new Exception("Expecting one and only one view object node in vo file: " + voPath);
            }
            vaParentNode = listViewObj.item(0);
        } else {
            NodeList listViewObj = vaXml.getElementsByTagName("ViewObject");
            if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
                //   System.out.println("Expecting one and only one view object node");
                throw new Exception("Expecting one and only one view object node in vo file: " + voPath);
            }
            vaParentNode = listViewObj.item(0);
        }


        Node viewAccessorNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(vaParentNode, "ViewAccessor", "Name", viewAccessorName);
        if (viewAccessorNode == null) {
            String msg = String.format("Could not obtain view accessor with name %s in vo: %s", viewAccessorName, bcPath);
            record.setDiagnostic(msg);
            m_errorLogger.log(Level.SEVERE, msg);
            throw new Exception(msg);
        }
        String listFetchSize = XMLParserHelper.getAttributeValue(viewAccessorNode, "FetchSize");
        record.setVoUsageFetchSizeInAm(listFetchSize);
        String viewObjectName = XMLParserHelper.getAttributeValue(viewAccessorNode, "ViewObjectName");
        record.addCustomAttr(TuningConstants.CUSTOMATTR_LISTVONAME, viewObjectName);
        record.addCustomAttr(TuningConstants.CUSTOMATTR_LOV_VA_BCPATH_VANAME, bcPath + "#" + viewAccessorName);
        return viewObjectName;

    }

    private String getBCObjectPath(String baseJprLocation, String bcFullPackage, TuningAnalysisRecord record) throws Exception {
        if (isEmpty(bcFullPackage)) {
            String msg = "[getBCObjectPath] bcFullPackage was empty";
            // System.out.println(msg);
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        String bcPathToFind = bcFullPackage.replaceAll("\\.", "/");
        bcPathToFind = bcPathToFind + ".xml";


        //Search location 1 - in the same base directory that the am is in
        if (!isEmpty(baseJprLocation)) {
            String modelBaseDir = getPreviousDirectory(baseJprLocation);
            File voFile = findBCInDirectory(modelBaseDir, bcPathToFind);
            if (voFile != null) {
                return voFile.getAbsolutePath();
            }
        }

        //Search location 2 - in all valid model directories in the base lba path
        ArrayList<String> modelDirslist = getListOfModelDirectoriesInPath(m_baseLbaPath);
        if (modelDirslist == null || modelDirslist.size() <= 0) {
            // System.out.println("[INFO] Could not obtain any valid model dirs");
        } else {
            for (int i = 0; i < modelDirslist.size(); i++) {
                String modelDir = modelDirslist.get(i);
                File bcFile = findBCInDirectory(modelDir, bcPathToFind);
                if (bcFile != null) {
                    return bcFile.getAbsolutePath();
                }
            }
        }

        /**
 * TODO: Modify logic so taht you lookup the name after oracle.apps.financials.<generalLedger>
 * have a lookup of mapping e.g. generalLedger -> ledger
 * go into the folder adeviewroot/fusionapps/fin/components/mappedname(ledger)
 * take the next value after generalledger e.g. journalSetup
 * create page adeviewroot/fusionapps/fin/coponents/mappedname(ledger)/journalSetup
 * go on doing that till the maximum level of nesting or model directory found and look in that directory
 *
 */

        // Search Location 2b - if it is a common bc (e.g. currencyVO etc), get the lookup location that we have hardcoded
        // Look in that directory as well as the previous directory in the same location
        if (isComonVOPath(bcFullPackage)) {
            String bcBaseLocation = getCommonVOLocation(bcFullPackage);
            File voFile = findBCInDirectory(bcBaseLocation, bcPathToFind);
            if (voFile != null) {
                return voFile.getAbsolutePath();
            }

            String prevBaseDir = getPreviousDirectory(bcBaseLocation);
            File voFile2 = findBCInDirectory(prevBaseDir, bcPathToFind);
            if (voFile2 != null) {
                return voFile2.getAbsolutePath();
            }
        }

        // Search location 3 - in all valid model directories within the previous directory
        ArrayList<String> modelDirsInPrevDir = getListOfAllModelDirsInPrevDir(m_baseLbaPath);
        if (modelDirsInPrevDir != null && modelDirsInPrevDir.size() > 0) {
            int modeldirsinDirSize = modelDirsInPrevDir.size();
            for (int i = 0; i < modeldirsinDirSize; i++) {
                String modelDirInDir = modelDirsInPrevDir.get(i);
                File voFile = findBCInDirectory(modelDirInDir, bcPathToFind);
                if (voFile != null && voFile.exists()) {
                    return voFile.getAbsolutePath();
                }
            }
        }

  //TODO: This will only work for fusionapps/fin/cpmponents it seems - should be fixed and analyzed for impact on # violations for other faimilies
        /* Search location 4 - try to look for shared vos etc
         * Drawback only single workspace lookups
         * From the BC package get the part between oracle.apps.financials && (view || publicView) ; This should give an idea of the location
         * e.g. oracle.apps.financials.receivables.customers.uiModel.view.CustomerProfileUIVO
         * So here we would extract customers/uiModel
         * From the ui file path get the product e.g. receivables/ar
         * put the two together e.g fusionapps/fin/components/receivables/ar/customers/uiModel
         * 
         * if directory does not exist go one level up and find all model directorys in there.
         * else if directory exists look for bc in that directory
         * TODO: cross workspace*/
        String beforeintelligentDir = "";
        if (m_uiAbsPath.contains("fusionapps/fin/components/")) {
            String baseProdPath = "fusionapps/fin/components/";
            int index = m_uiAbsPath.indexOf(baseProdPath);
            beforeintelligentDir = m_uiAbsPath.substring(0, index + baseProdPath.length());
            String afterPath = m_uiAbsPath.substring(index + baseProdPath.length());
            String[] parts = afterPath.split("/");

            if (parts != null && parts.length >= 2) {
                beforeintelligentDir = beforeintelligentDir + parts[0] + "/" + parts[1];
            } else {
                return "";
            }
        }
        String intelligentBcPath = "";
        if (bcFullPackage.contains("oracle.apps.financials.")) {
            intelligentBcPath = bcFullPackage.replaceFirst("oracle.apps.financials.", "");
            int firstPackageSepInd = intelligentBcPath.indexOf(".");
            if (firstPackageSepInd == -1)
                return "";
            // strip the
            String productCode = intelligentBcPath.substring(0, firstPackageSepInd);
            if (intelligentBcPath.length() < firstPackageSepInd + 2)
                return "";
            intelligentBcPath = intelligentBcPath.substring(firstPackageSepInd + 1);
            // means we are lookig for a vo
            if (intelligentBcPath.contains(".view.")) {
                int viewIndex = intelligentBcPath.indexOf(".view");
                intelligentBcPath = intelligentBcPath.substring(0, viewIndex);
                intelligentBcPath = intelligentBcPath.replaceAll("\\.", "/");

                String intelligentBcFilePath = beforeintelligentDir + "/" + intelligentBcPath;
                File intelligentBcFile = new File(intelligentBcFilePath);
                if (!intelligentBcFile.exists()) {
                    // if it does not exist, go one directory up and find all model classes in this directory
                    String prevDirintelligentDir = getPreviousDirectory(intelligentBcFilePath);
                    intelligentBcFile = new File(prevDirintelligentDir);
                    if (!intelligentBcFile.exists())
                        return "";
                    ArrayList<String> trialModelDirs = getListOFAllValidDirectoriesInPath(prevDirintelligentDir);
                    if (trialModelDirs != null && trialModelDirs.size() > 0) {
                        int modeldirsinDirSize = trialModelDirs.size();
                        for (int i = 0; i < modeldirsinDirSize; i++) {
                            String modelDirInDir = trialModelDirs.get(i);
                            File voFile = findBCInDirectory(modelDirInDir, bcPathToFind);
                            if (voFile != null && voFile.exists()) {
                                return voFile.getAbsolutePath();
                            }
                        }
                    }
                    return "";
                }
                if (isValidModelDir(intelligentBcFile.getAbsolutePath())) {
                    File voFile = findBCInDirectory(intelligentBcFile.getAbsolutePath(), bcPathToFind);
                    if (voFile != null && voFile.exists()) {
                        return voFile.getAbsolutePath();
                    }
                }
            }
        }


        return "";
    }


    private XMLDocument getViewObject(String amAbsFilePath, String voFullPackage, TuningAnalysisRecord record, boolean updateVOPath,
                                      StringBuffer voFilePathObtained) throws Exception {
        if (isEmpty(voFullPackage)) {
            if (bDebug) 
                System.out.println("[ERROR] getViewObject: VO package empty");
            return null;
        }
        String bcPathToFind = voFullPackage.replaceAll("\\.", "/");
        bcPathToFind = bcPathToFind + ".xml";
        XMLDocument voXml = null;

        String modelJprLocation = UiFileHelper.getJprLocationForModelFile(amAbsFilePath);
        
        if(isEmpty(modelJprLocation))
        {
            System.out.println("==> Error : getViewObject: cannot find model jpr. looking for ampath:  " + amAbsFilePath);
            return null;
        }

        String bcPath = getBCObjectPath(modelJprLocation, voFullPackage, record);

        if (!isEmpty(bcPath) && updateVOPath) {
            record.setVoPath(bcPath);
        }
        if (voFilePathObtained != null && !isEmpty(bcPath)) {
            voFilePathObtained.append(bcPath);
        }
        if (!isEmpty(bcPath)) {
            return XMLParserHelper.getXMLDocument(bcPath);
        }


        // Search location 4 - in all dependent model jars
        /* If it reaches this stage means it did not find the view object in any of the most probable directories..
        now have to scan all the referenced jars in the jpr*/

        if (isEmpty(modelJprLocation)) {
            if (bDebug) 
               System.out.println("Could not obtain jpr");
            return null;
        }
        HashSet<String> adfLibrarys = JprHelper.getAllAdfLibrarysInJpr(modelJprLocation, true);
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
                            if (bDebug) {
                                System.out.println("Found the vo in: " + jar.getName());
                            }
                            if (updateVOPath) {
                                record.setVoPath("JAR: " + jar.getName());
                            }
                            InputStream contents = jar.getInputStream(voEntry);
                            voXml = XMLParserHelper.getXMLDocument(contents);
                            if (contents != null) {
                                contents.close();
                            }
                            break;
                        }
                    }
                } catch (IOException ex) {
                    String msg = "[ERROR]:getViewObject: IOException: " + ex.getMessage()+" lib file: " + lib;
                    if (bDebug) {
                      System.out.println(msg); 
                      m_errorLogger.log(Level.SEVERE, msg); 
                    }
                    continue;
                }
            }
        }

        HashSet<String> adfbcImports = JprHelper.getAllBcImportsInJpr(modelJprLocation, true);
        if (adfbcImports != null && adfbcImports.size() > 0) {
            Iterator<String> libraryIter = adfbcImports.iterator();
            while (libraryIter.hasNext()) {
                String lib = libraryIter.next();
                JarFile jar = null;
                try {
                    jar = new JarFile(lib);

                    if (jar != null) {

                        JarEntry voEntry = jar.getJarEntry(bcPathToFind);

                        if (voEntry != null) {
                            if (bDebug) {
                                System.out.println("Found the vo in: " + jar.getName());
                            }
                            if (updateVOPath) {
                                record.setVoPath("JAR: " + jar.getName());
                            }
                            InputStream contents = jar.getInputStream(voEntry);
                            voXml = XMLParserHelper.getXMLDocument(contents);
                            if (contents != null) {
                                contents.close();
                            }
                            break;
                        }
                    }
                } catch (IOException ex) {
                    String msg = "[ERROR]:getViewObject: IOException: " + ex.getMessage();
                    if (bDebug) {
                       System.out.println(msg); 
                       m_errorLogger.log(Level.SEVERE, msg); 
                    }
                    continue;
                }
            }
        }
        return voXml;
    }

    public String getViewUsagePackageFromAm(String amAbsPath, String viewUsageName, TuningAnalysisRecord record,
                                            boolean recordAMVOFetchSize) throws Exception {
        if (isEmpty(amAbsPath)) {
            throw new Exception("Am abs path was null");
        }
        XMLDocument amXml = XMLParserHelper.getXMLDocument(amAbsPath);
        boolean bFoundViewUsage = false;
        NodeList appmoduleNodeList = amXml.getElementsByTagName("AppModule");
        if (!XMLParserHelper.hasOneNOnlyOneNode(appmoduleNodeList)) {
            String msg = "Expecting one and only one AppModule node in the amxml";
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }

        Node appModuleNode = appmoduleNodeList.item(0);
        NodeList appModuleChildren = appModuleNode.getChildNodes();
        if (appModuleChildren == null) {
            String msg = String.format("View usage tag not found in am %s", amAbsPath);
            throw new Exception(msg);
        }
        int appModuleChildLen = appModuleChildren.getLength();
        for (int i = 0; i < appModuleChildLen; i++) {
            Node appModuleChild = appModuleChildren.item(i);
            String tagName = appModuleChild.getNodeName();
            if (!isEmpty(tagName) && tagName.trim().equals("ViewUsage")) {
                String obtainedVoUsageName = XMLParserHelper.getAttributeValue(appModuleChild, "Name");

                if (!isEmpty(obtainedVoUsageName) && obtainedVoUsageName.trim().equals(viewUsageName)) {
                    if (bFoundViewUsage == true) {
                        String msg =
                            String.format("[WARNING]: updateVoFetchSize: file might be corrupt.. duplicate vo usage: %s in am file: %s", obtainedVoUsageName,
                                          amAbsPath);
                        if (bDebug) {
                           System.out.println(msg);
                           m_errorLogger.log(Level.WARNING, msg);
                        }
                        record.setDiagnostic(record.getDiagnostic() + " * duplicate vo usage detected in am: " + amAbsPath);
                        // At the time of fixing this will be a problem.. hence throw an exception.. we only want to collect the good or can be fixed records
                        throw new Exception(msg);
                    }
                    bFoundViewUsage = true;
                    String voUsagePackage = XMLParserHelper.getAttributeValue(appModuleChild, "ViewObjectName");
                    record.setVoFullPackage(voUsagePackage);
                    if (recordAMVOFetchSize) {
                        String voUsageFetchSize = XMLParserHelper.getAttributeValue(appModuleChild, "FetchSize");
                        record.setVoUsageFetchSizeInAm(voUsageFetchSize);
                    }
                    break;
                }
            }
        }

        if (isEmpty(record.getVoFullPackage())) {
            String msg = String.format("Could not get vo package from am: %s, looking for voUsage: %s", amAbsPath, viewUsageName);
            record.setDiagnostic(msg);
            throw new Exception(msg);
        }
        return record.getVoFullPackage();
    }

    public File getPageDef(File uiFile, DataBindings bindings) {
        if (uiFile == null) {
           // System.out.println("[ERROR] Input file was null");
            return null;
        }
        String absPath = uiFile.getAbsolutePath();
        HashMap<String, String> mapPagesPageDefs = bindings.getPagesPageDef();
        if (mapPagesPageDefs == null) {
            //System.out.println("databindings do not have any page refs");
            return null;
        }
        String abstractPath = UiFileHelper.getUIFileAbstractPath(absPath);
        if (UiFileHelper.isEmpty(abstractPath)) {
            //System.out.println("ui file abstract path is null");
            return null;
        }
        String pageDefDotPath = mapPagesPageDefs.get(abstractPath);
        if (UiFileHelper.isEmpty(pageDefDotPath)) {
           // System.out.println("Could not obtain page def path for file: " + absPath);
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
                  if (bDebug)
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
                if (bDebug)
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
                if (bDebug) {
                    System.out.println("IterName is : " + iteratorId + ".. elexp is: " + elExp);
                }
                return iteratorId;
            }
        } else {
            if (bDebug) 
              System.out.println("Not a valid EL Exp.. should start with #{");
        }
        return extractedValue;
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
     *Is a possible EL Expression if it starts with '#{'
     * @param possibleEL
     * @return
     */
    private boolean isRowLevelBinding(String possibleEL) {
        if (!isEmpty(possibleEL) && possibleEL.trim().startsWith("#{row.")) {
            return true;
        }
        return false;
    }

    public void setList_errorRecords(ArrayList<TuningAnalysisRecord> list_errorRecords) {
        this.list_errorRecords = list_errorRecords;
    }

    public ArrayList<TuningAnalysisRecord> getList_errorRecords() {
        return list_errorRecords;
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
    private String getAmFile(String amFullPackage) {
        File amFile = null;

        String amPathToFind = amFullPackage.replaceAll("\\.", "/");
        amPathToFind = amPathToFind + ".xml";

        String uiJprFullPath = UiFileHelper.getJprLocationForUiFile(m_uiAbsPath);
        if (uiJprFullPath != null) {
            HashSet<String> modelDependencyJars = JprHelper.getProjectDependencysInJar(uiJprFullPath);

            if (modelDependencyJars != null) {
                Iterator<String> modelJprIterator = modelDependencyJars.iterator();
                while (modelJprIterator.hasNext()) {
                    String modelJprLocation = modelJprIterator.next();
                    File modelFile = new File(modelJprLocation);
                    if (modelFile.exists()) {
                        String modelDir = getPreviousDirectory(modelJprLocation);
                        amFile = findBCInDirectory(modelDir, amPathToFind);
                        if (amFile != null) {
                            return amFile.getAbsolutePath();
                        }
                    }
                }

            }
        } else {
            return null;
        }

        if (isEmpty(m_baseLbaPath)) {
          if (bDebug)
            System.out.println("ERROR: Base UI Path is empty.. skipping file:");
            return null;
        }
        ArrayList<String> modelDirs = getListOfModelDirectoriesInPath(m_baseLbaPath);

        for (int i = 0; i < modelDirs.size(); i++) {
            String modelDir = modelDirs.get(i);
            amFile = findBCInDirectory(modelDir, amPathToFind);
            if (amFile != null && amFile.exists()) {
                return amFile.getAbsolutePath();

            }
        }

        ArrayList<String> modelDirsInPrevDir = getListOfAllModelDirsInPrevDir(m_baseLbaPath);
        if (modelDirsInPrevDir != null && modelDirsInPrevDir.size() > 0) {
            int modeldirsinDirSize = modelDirsInPrevDir.size();
            for (int i = 0; i < modeldirsinDirSize; i++) {
                String modelDirInDir = modelDirsInPrevDir.get(i);
                amFile = findBCInDirectory(modelDirInDir, amPathToFind);
                if (amFile != null && amFile.exists()) {
                    return amFile.getAbsolutePath();

                }
            }
        }


        String msg =
            String.format("[ERROR] Could not find am in the probable directories.. AM: %s, UIFile: %s.. will look at dependencies in ui jpr now.. however during modifications will not be possible to modify this am...  ",
                          amPathToFind, m_uiAbsPath);
        if (bDebug) {
            System.out.println(msg); 
            m_errorLogger.log(Level.SEVERE, msg); 
        }
        if (isEmpty(uiJprFullPath)) {
            System.out.println("Could not find jpr for ui file: " + m_uiAbsPath);
            return null;
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

        if (dirPath.contains(".ade_path") || lastDir.contains("dbschema") || lastDir.endsWith("test")) {
            return false;
        }

        if ((lastDir.contains("ui") || lastDir.contains("publicui")) && !lastDir.contains("model")) {
            return false;
        }
        if ((lastDir.contains("di") || lastDir.contains("publicdi")) && !lastDir.contains("model")) {
            return false;
        }


        return true;
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

        ArrayList<String> alldirs = getListOFAllValidDirectoriesInPath(prevDir);
        if (alldirs != null) {
            Iterator<String> allDirsIter = alldirs.iterator();
            while (allDirsIter.hasNext()) {
                String dir = allDirsIter.next();
                ArrayList<String> modeDirsInDir = getListOfModelDirectoriesInPath(dir);
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

    private String getPreviousDirectory(String path) {
        if (!isEmpty(path)) {
            int indexLastSlash = path.lastIndexOf("/");
            if (indexLastSlash != -1) {
                return path.substring(0, indexLastSlash);
            }
        }
        return null;
    }

    private void updateMultipleBcUsages(TuningAnalysisRecord record) {
        String amVoUsageRequired = record.getCustomAttrWithKey("treeTypeProcessing");
        if (amVoUsageRequired == null || !amVoUsageRequired.trim().equals("true")) {
            return;
        }
        //This check is added here only temporarily since we want to be conservative in our approach
        // for now will only track multiple bc uages for tables. TODO: handle the case of dynamic lists and navigation lists
        String componentName = record.getComponentName();
        if (!componentName.equals("af:table")) {
            return;
        }

        String amFullPackage = record.getAmFullPackage();
        String voInstanceName = record.getVoInstanceName();
        if (!isEmpty(amFullPackage) && !isEmpty(voInstanceName)) {
            String bcKey = amFullPackage + "#" + voInstanceName;
            m_bcUsages = UiFileHelper.addToMap(m_bcUsages, bcKey, record);
        }

    }

    private void updateMultipleIteratorUsages(TuningAnalysisRecord record) {
        String componentName = record.getComponentName();
        //if (componentName.trim().equals("af:table") || componentName.trim().equals("af:treeTable")) {
        //TODO: handle the case of tree tables as well..
        if (componentName.trim().equals("af:table")) {

            String pagedefPath = record.getPageDefFilePath();
            String pageDefIteratorName = record.getPageDefIteratorName();

            if (!isEmpty(pagedefPath) && !isEmpty(pageDefIteratorName)) {
                String pagedefKey = pagedefPath + "#" + pageDefIteratorName;
                m_mapOfMultipleIteratorUsages = UiFileHelper.addToMap(m_mapOfMultipleIteratorUsages, pagedefKey, record);
            }
        }
    }


    public void setBcUsages(HashMap<String, ArrayList<TuningAnalysisRecord>> m_bcUsages) {
        this.m_bcUsages = m_bcUsages;
    }

    public HashMap<String, ArrayList<TuningAnalysisRecord>> getBcUsages() {
        return m_bcUsages;
    }

    public void setMapOfMultipleIteratorUsages(HashMap<String, ArrayList<TuningAnalysisRecord>> m_mapOfMultipleIteratorUsages) {
        this.m_mapOfMultipleIteratorUsages = m_mapOfMultipleIteratorUsages;
    }
    // returns only violating records

    public HashMap<String, ArrayList<TuningAnalysisRecord>> getMapOfMultipleIteratorUsages() {
        if (m_mapOfMultipleIteratorUsages == null) {
            return null;
        }

        return UiFileHelper.getOnlyMultipleUsages(m_mapOfMultipleIteratorUsages);
    }

    public void setMap_numberOfComponentsByName(HashMap<String, Integer> map_numberOfComponentsByName) {
        this.map_numberOfComponentsByName = map_numberOfComponentsByName;
    }

    public HashMap<String, Integer> getMap_numberOfComponentsByName() {
        return map_numberOfComponentsByName;
    }
}
