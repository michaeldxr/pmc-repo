package oracle.apps.votuning_v2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;

import java.io.IOException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.XMLFormatter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.AdeDiffHelper;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.apps.lovWidth.ViewObjectHelper;

import oracle.apps.sqlanalyzer.ViewCriteriaHelper;

import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RunLOVAnalysisLRG implements Serializable
{
    private static final long serialVersionUID = 1;
    
    private HashMap<String, DataBindings> bindingsMap = new HashMap<String, DataBindings>();

    transient static String ERROR_LOG_FILENAME = "error.log";
    transient public static Logger errorLogger = null;
    transient static int m_jsffCount = 0;
    transient static int m_jspxCount = 0;
    transient static int m_currentrRow = 2;
    transient static String m_diffFileName = "diff.txt";
    transient static BufferedWriter csvWriter;
    transient AdeDiffHelper diffHelper = new AdeDiffHelper(m_diffFileName);
    private transient static Workbook m_myBook;
    private transient static Worksheet m_analysisSheet;
    
    private transient static HashSet<String> exemptions = new HashSet<String>();
    /**
     * List of error records..
     */
    HashMap<String, ArrayList<TuningAnalysisRecord>> allfin_errorRecords = new HashMap<String, ArrayList<TuningAnalysisRecord>>();


    HashMap<String, ArrayList<TuningAnalysisRecord>> onlyrequired_errorRecords = new HashMap<String, ArrayList<TuningAnalysisRecord>>();


    /**
     * Map of all the anlaysis records for all of financials
     */
    HashMap<String, ArrayList<TuningAnalysisRecord>> allfin_analysisRecords = new HashMap<String, ArrayList<TuningAnalysisRecord>>();

    /**
     * Map of all the anlaysis records for all of financials
     */
    HashMap<String, ArrayList<TuningAnalysisRecord>> onlyreqiured_analysisRecords =
        new HashMap<String, ArrayList<TuningAnalysisRecord>>();

    /**
     * Map of all the binding components (AM - VO) used by each record
     */
    HashMap<String, ArrayList<TuningAnalysisRecord>> m_map_bcs_records = new HashMap<String, ArrayList<TuningAnalysisRecord>>();


    /**
     * Map of only those binding components (AM-VO) that are used by multiple records)
     */
    HashMap<String, ArrayList<TuningAnalysisRecord>> m_map_violatingBcs_records = new HashMap<String, ArrayList<TuningAnalysisRecord>>();


    /**
     * Map of all the anlaysis records for all of financials
     */
    private HashMap<String, ArrayList<TuningAnalysisRecord>> onlyreq_multipleBcs_fixedrecords =
        new HashMap<String, ArrayList<TuningAnalysisRecord>>();

    /**
     * Map of the final multiple bc analysis
     */
    private HashMap<String, ArrayList<TuningAnalysisRecord>> multipleBcsFinalMap = new HashMap<String, ArrayList<TuningAnalysisRecord>>();

    /**
     * indexed by pagedef#iteratorName
     * This helps identify if an iterator on one page is being used by  multiple components.
     * Based on analysis, so far in fianncials - we do not have this case that there is one pagedef iterator being used across multiple tree components
     */
    HashMap<String, ArrayList<TuningAnalysisRecord>> m_mapOfMultipleIteratorUsages = new HashMap<String, ArrayList<TuningAnalysisRecord>>();

    HashMap<String, Integer> defaultVOFetchSizesFixed = new HashMap<String, Integer>();


    private HashMap<String, Integer> m_map_componentCnt = new HashMap<String, Integer>();
    private static boolean bDebug = false;

    static boolean fix_MultipleBcUsages = true;
    // As per Sam To (PSR) we do nnot want to fix LRS by default. 
    // Default should be false.
    static boolean fix_selectOneChoice_listRangeSize = false;
    static boolean fix_combobox_listRangeSize = false;
    static boolean fix_tune_tables = true;
    static boolean fix_tune_combobx = true;
    static boolean fix_tune_selectoneChoice = true;
    static boolean writeToSpreadsheet = true;

    static boolean m_fixlov_only_if_max_exceeded = false;
    static Integer m_fixlov_Max = 250;
    static boolean m_fixViolations = true; 
    static boolean bDoADE = false; //Su: Making it false, and it cannot be overriden to true by developers. Fix will be manual in this case
    static boolean append = false;
    
    private static final String issuetype= "TableFetchSizePSL";
    
    public RunLOVAnalysisLRG() {
        super();
    }


    public void printMap(HashMap<String, Integer> map) {
        Set<String> keys = map.keySet();
        Iterator<String> keyiter = keys.iterator();
        while (keyiter.hasNext()) {
            String key = keyiter.next();
            Integer cnt = map.get(key);
            String msg = String.format("Component Name: %s, Count %s", key, cnt);
            System.out.println(msg);
        }
    }

    public static void main(String[] args) {
       
        try {
            // Usage:  RunAutomatedTuningFixer.sh <path to your code in an open ade view> <fix violations in path> <Do ADE checkin/checkout>
            if (args.length != 1 || args[0] == null || args[0].equals("")) {
                System.out.println("Series has to be specified");
                System.exit(1);
            }

            String series = args[0];
           
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);

            
            errorLogger = initLogger();
            UiFileHelper.bDebug = bDebug;
            
            csvWriter = new BufferedWriter(new FileWriter("TableFetchSizePSL_LRG.csv"));
            csvWriter.write("family,module,product,filename,series,label,Component Name,Component Id, AutoHeightRows,IteratorRangeSize," +
                "IteratorRangeSize_New, Iterator Name, List Range Size,List Range Size New, VOUsageFetchSize,VOUsageFetchSize_New, VODEfaultFetchSize," +
                "VODEfaultFetchSize_New,AM Package, VO Package, VO Instance, Fix Status, isTableWrappedByPanelStretchLayout, Panel Stretch Ancestory\n");

            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
            for (int j = 0; j < families.size(); j++) {
                String family = families.get(j);
              
                //This steps need to be tweaked to add component, because this specific scan checkes "component" keyword
                String sCrawlDir = LRGUtil.getCrawlDir(series, family, label, issuetype);
                System.out.println("Crawling dir '" + sCrawlDir + "' for family: " + family);

                RunLOVAnalysisLRG lovAnalyzer = new RunLOVAnalysisLRG();
                lovAnalyzer.runPass1(sCrawlDir);
                
                String pass1AnalysisPath = getPass1AnalysisPath(sCrawlDir);
                if (pass1AnalysisPath.equals(sCrawlDir)) {
                    lovAnalyzer.onlyreqiured_analysisRecords = lovAnalyzer.allfin_analysisRecords;
                    lovAnalyzer.onlyrequired_errorRecords = lovAnalyzer.allfin_errorRecords;
                } else {
                    lovAnalyzer.crawlDirToFilterAnalysisOnly(sCrawlDir);
                }
                
                lovAnalyzer.writeOutputXml(lovAnalyzer.onlyreqiured_analysisRecords, lovAnalyzer.onlyrequired_errorRecords, "lovresults.xml");
                
                System.out.println("======== WRITING RESULTS TO SPREADSHEET ============");
                writeToSpreadsheet(lovAnalyzer.onlyreqiured_analysisRecords, "Results");
                writeSpreadsheet("results.xlsx");
                System.out.println("======== RESULTS WRITTEN TO SPREADSHEET results.xlsx ============");
            }
            csvWriter.close();
            
            LRGUtil.addLabelInfoToAB(issuetype, series, label);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Pass 1
     * 1. Analyze all of financials (fusionapps/fin/components) - to find all bcs used in multiple pages. These cases will require special handling.
     * This process will slow down the entire script.. but it is required.
     * @param sCrawlDir
     * Main directory to be analyzed.
     * @throws Exception
     */
    public void runPass1(String sCrawlDir) throws Exception {
        if(sCrawlDir==null || sCrawlDir.equals(""))
            return;
        String pass1AnalysisPath = getPass1AnalysisPath(sCrawlDir);
        //String pass1AnalysisPath = sCrawlDir;
        if (pass1AnalysisPath == null) {
            throw new Exception("[ERROR]Could not get analysis path for pass 1");
        }

        System.out.println(String.format("============Pass 1 : Analyse all files in directory %s ============", pass1AnalysisPath));

        /* for each valid input jspx/jsff file, get a list of all records. Indexed by ui file path.
         * Records updated in static member variable: m_map_bcs_records
         * Records contain every ui file in fusionapps/fin
         * */
        crawlDirectoryForPass1(pass1AnalysisPath);

        if (m_map_bcs_records != null && m_map_bcs_records.size() > 0) {
            m_map_violatingBcs_records = UiFileHelper.getOnlyMultipleUsages(m_map_bcs_records);
        }
        System.out.println("************** Pass 1 Statistics ***************");
        System.out.println(String.format("Number of files analyzed in pass 1: (jsff): %s, (jspx):%s", m_jsffCount, m_jspxCount));
        if (m_map_violatingBcs_records != null && m_map_violatingBcs_records.size() > 0) {
            System.out.println(String.format("Number of multiple bcs found in pass 1: %s \n", m_map_violatingBcs_records.size()));
        }
        if (m_mapOfMultipleIteratorUsages != null && m_mapOfMultipleIteratorUsages.size() > 0) {
            System.out.println(String.format("Number of pagedefs having multiple iterator usages: %s \n", m_mapOfMultipleIteratorUsages.size()));
        }
        if (m_map_componentCnt != null && m_map_componentCnt.size() > 0) {
            System.out.println("Statistics of components analyzed in pass 1: ");
            printMap(m_map_componentCnt);
            System.out.println("\n");
        }
        // reset counters
        m_jsffCount = 0;
        m_jspxCount = 0;
        
        System.out.println("===================END OF PASS 1 ==========================================");
    }


    public void scanAndMakeFixes(String sCrawlDir, boolean bDoADE) throws Exception {
        System.out.println(String.format("============== Pass 2 (Scanning and making fixes) : directory: %s =========== ", sCrawlDir));

        if (allfin_analysisRecords == null) {
            System.out.println("No fixes required");
            return;
        }

        System.out.println("****** [START] Calculating range sizes for multiple iterator usages ******* ");
        HashMap<String, TuningAnalysisRecord> multipleIteratorFetchSizes = new HashMap<String, TuningAnalysisRecord>();
        // only need multiple iterator usages if we are making table fixes
        if (fix_tune_tables) {
            // calculate for all the multiple page def iterator, the new iterator range size
            multipleIteratorFetchSizes = Rules.calculateRangeSizeForMultipleIteratorUsages(m_mapOfMultipleIteratorUsages);
            if (multipleIteratorFetchSizes != null) {
                System.out.println(String.format("****** [END] Calculating range sizes for multiple iterator usages (Number of mutliple Iterators %s) ******* ",
                                                 multipleIteratorFetchSizes.size()));
            } else {
                System.out.println("*********[END- Multiple iterator usages] No multiple Iteartor Usages");
            }
        }
        // Remove any existing diff.txt. Create a new bufferred writer for diff.txt
        if (bDoADE) {
            diffHelper.startDiffLog();
        }

        crawlDirForFixes(sCrawlDir, bDoADE, multipleIteratorFetchSizes);
        if (fix_MultipleBcUsages) {
            System.out.println("******* [START] Fixing multiple bc usages***************");
            multipleBcsFinalMap = fixMultipleBcUsages(bDoADE);
            //multipleBcsFinalMap = fixMultipleBcUsages(bDoADE);
            System.out.println("******* [END] Fixing multiple bc usages***************");
        } else {
            System.out.println("[WARNING] Not fixing multiple bc usages since user elected not to do so");
        }

        if (bDoADE) {
            diffHelper.closeDiffLog();
        }

    }


    /**
     * recurse over the crawl directory and for each valid ui file - process it.
     * Valid directories -
     * - should not contain, /dbSchema/ or /model/ or /publicModel/ or /test/ or /classes/
     * Valid UI files
     * - jspx or jsff extensions
     * - and not in folder with /classes/ or /test/ or /model/ or /publicModel/
     *
     * 1. Keep track of all the error records in this crawl director
     * 2. all the analyzed records (irrespective if they are fixed or not) in the crawl directory
     * @param path
     * @param bDoADE
     */
    private void crawlDirForFixes(String path, boolean bDoADE, HashMap<String, TuningAnalysisRecord> multipleIteratorFetchSizes) {
        if (path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            String absFilePath = listOfFiles[i].getAbsolutePath();
            if(absFilePath.contains(".ade_path"))
                continue;
            if (listOfFiles[i].isFile()) {

                if (isValidUIFile(absFilePath)) {
                    if (absFilePath.trim().endsWith(".jsff")) {
                        m_jsffCount++;
                    } else if (absFilePath.trim().endsWith(".jspx")) {
                        m_jspxCount++;
                    }
                    if (allfin_errorRecords.containsKey(absFilePath)) {
                        ArrayList<TuningAnalysisRecord> list_errorrecords = allfin_errorRecords.get(absFilePath);
                        onlyrequired_errorRecords.put(absFilePath, list_errorrecords);
                    }
                    if (allfin_analysisRecords.containsKey(absFilePath)) {
                        try {
                            makeFix(absFilePath, bDoADE, multipleIteratorFetchSizes);
                        } catch (Exception ex) {
                            String msg =
                                String.format("Skipping file: %s.. Error while trying to make fix for file. Exception: %s", absFilePath, ex.getMessage());
                            if(bDebug)
                               System.out.println(msg); 
                            errorLogger.log(Level.SEVERE, msg); 
                            continue;
                        }
                    }

                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidDirectory(dir)) {
                    crawlDirForFixes(dir, bDoADE, multipleIteratorFetchSizes);
                }
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }


    public void fixPageDefIterators(TuningAnalysisRecord record, boolean bDoADE, HashMap<String, TuningAnalysisRecord> multipleIteratorFetchSizes) {

        if (record == null) {
            return;
        }
        
        // JOE_TODO:  Fix 1, process just stuff for this scanner
        if(!record.isTableWrappedByPanelStretchLayout())
        {
            return;
        }

        // to avoid any npes when accessing this map
        if (multipleIteratorFetchSizes == null) {
            multipleIteratorFetchSizes = new HashMap<String, TuningAnalysisRecord>();
        }


        // get the page def
        String pageDefPath = record.getPageDefFilePath();
        String pageDefIteratorName = record.getPageDefIteratorName();
        String pageDefKey = pageDefPath + "#" + pageDefIteratorName;

        if (bDebug) {
            System.out.println("Processing page def: : " + pageDefPath);
        }


        try {
            //TuningAnalysisRecord newrecord = null;
            if (multipleIteratorFetchSizes.containsKey(pageDefKey)) {
                if (bDebug) {
                    System.out.println("Found multiple iterator fetch size usage: " + pageDefIteratorName);
                    System.out.println("pagedef key " + pageDefKey);
                }
                TuningAnalysisRecord multipleIterRecord = multipleIteratorFetchSizes.get(pageDefKey);

                HashSet<String> newRecordFixStatuses = multipleIterRecord.getSetfixStatus();
                // add all the new record fix statuses..only add the ones with multiple iterator
                record.addFixStatus(newRecordFixStatuses);
                //String newRecordFixStatus = newrecord.getFixStatus();
                //record.setFixStatus(newRecordFixStatus);
                record.setIteratorRangeSizeNewValue(multipleIterRecord.getIteratorRangeSizeNewValue());

            } 
            else 
            {
                Rules.calculateAutoHeightNIteratorRange(record);
            }

            record.addRuleApplied(TuningConstants.Rules.FIX_TUNE_TABLES_PDEF_ITERATOR);
            //            if (newrecord == null) {
            //                return;
            //            }

            // String newIteratorRangeSize = newrecord.getIteratorRangeSizeNewValue();
            String newIteratorRangeSize = record.getIteratorRangeSizeNewValue();
            if (isEmpty(newIteratorRangeSize)) {
                record.addFixStatus(TuningConstants.FixStatus.ITEARTOR_RANGESIZE_FIX_CANNOT);
                record.setFixStatusComment(record.getFixStatusComment() + " \n new iterator range size empty.");
                return;
            } else if (newIteratorRangeSize.equals(TuningConstants.NO_CHANGE)) {
                record.addFixStatus(TuningConstants.FixStatus.ITEARTOR_RANGESIZE_FIX_NOCHANGE);
                return;
            }

            //String iteratorFixStatus = newrecord.getFixStatus();
            //HashSet<String> setFixStatus = newrecord.getSetfixStatus();
            //if (setFixStatus != null && setFixStatus.contains(TuningAnalysisRecord.STATUS_WILLFIX_ITERATOR)) {

            XmlFixer pageDefFixer = new XmlFixer(pageDefPath);
            boolean fixMade = fixIteratorRangeSizeInPageDef(record, pageDefFixer);
            if (fixMade) {
                if (bDoADE) {
                    String pageDefFileContents = pageDefFixer.getFileContentsAsString(false);
                    applyFix(pageDefFixer.getFilePath(), pageDefFileContents);
                }

                //record.setFixStatus(TuningAnalysisRecord.STATUS_FIX_ITERATOR_SUCCESS);
                if (multipleIteratorFetchSizes.containsKey(pageDefKey)) {
                    //newrecord.setFixStatus(TuningAnalysisRecord.STATUS_FIX_MULTITERATOR_SUCCESS);
                    record.addFixStatus(TuningConstants.FixStatus.FIX_MULTITERATOR_SUCCESS);
                    TuningAnalysisRecord multiRecord = multipleIteratorFetchSizes.get(pageDefKey);
                    multiRecord.addFixStatus(TuningConstants.FixStatus.FIX_MULTITERATOR_SUCCESS);
                    multipleIteratorFetchSizes.put(pageDefKey, multiRecord);
                    //multipleIteratorFetchSizes.put(pageDefKey, newrecord);
                } else {
                    record.addFixStatus(TuningConstants.FixStatus.ITEARTOR_RANGESIZE_FIX_SUCCESS);
                }
            } else {
                //record.addFixStatus("ITER_FIX_falSE");
            }
            //} else if (setFixStatus != null &&
            //         setFixStatus.contains(TuningAnalysisRecord.STATUS_FIX_ITERATOR_NOCHANGE)) {

            //}

        } catch (Exception ex) {
            if (multipleIteratorFetchSizes.containsKey(pageDefKey)) {
                record.addFixStatus(TuningConstants.FixStatus.FIX_MULTITERATOR_FAILED);
                TuningAnalysisRecord multiRecord = multipleIteratorFetchSizes.get(pageDefKey);
                multiRecord.addFixStatus(TuningConstants.FixStatus.FIX_MULTITERATOR_FAILED);
                multipleIteratorFetchSizes.put(pageDefKey, multiRecord);
            } else {
                record.addFixStatus(TuningConstants.FixStatus.ITEARTOR_RANGESIZE_FIX_FAILED);
            }

            System.out.println("[ERROR] Ecxeption while trying to fix record with pagedef: " + pageDefPath);
            System.out.println("Skipping this record");

        }


    }

    /**
     * Main method for making fixes
     * @param uiFilePath
     * @param bDoADE
     * @param multipleIteratorFetchSizes
     */
    public void makeFix(String uiFilePath, boolean bDoADE, HashMap<String, TuningAnalysisRecord> multipleIteratorFetchSizes) {
        if (bDebug) {
            System.out.println("[makeFix] Processing file: " + uiFilePath);
        }

        ArrayList<TuningAnalysisRecord> records = allfin_analysisRecords.get(uiFilePath);
        ArrayList<TuningAnalysisRecord> fixedRecords = new ArrayList<TuningAnalysisRecord>();
        if (records == null || records.size() == 0) {
            return;
        }

        Iterator<TuningAnalysisRecord> recordsIter = records.iterator();
        while (recordsIter.hasNext()) {
            TuningAnalysisRecord record = recordsIter.next();

            if (fix_tune_tables) 
            {
                analyzeForPanelStretchWrappedTables(record, bDoADE);
                fixPageDefIterators(record, bDoADE, multipleIteratorFetchSizes);
                fixAmsForTables(record, bDoADE);
                /*ArrayList<TuningAnalysisRecord> records = fixPageDefIterators(uiFilePath, bDoADE, multipleIteratorFetchSizes);
            if (records == null) {
                System.out.println("No page def iterators fixed.. returning");
            }
            records = fixAmsForUIFiles(records, bDoADE);
            onlyreqiured_analysisRecords.put(uiFilePath, records);*/
            }
//            if (fix_selectOneChoice_listRangeSize) {
//                fixSOCListRangeSize(record, bDoADE);
//            }
//
//            if (fix_combobox_listRangeSize) {
//                fixCombobBoxListRangeSize(record, bDoADE);
//            }
//
//            if (fix_tune_selectoneChoice) {
//                fixTuneSOC(record, bDoADE);
//            }
//
//            if (fix_tune_combobx) {
//                fixTuneCombobox(record, bDoADE);
//            }

            fixedRecords.add(record);
        }
        onlyreqiured_analysisRecords.put(uiFilePath, fixedRecords);


    }

    public TuningAnalysisRecord analyzeForPanelStretchWrappedTables(TuningAnalysisRecord record, boolean bDoADE) 
    {
        if (record == null || record.isTableWrappedByPanelStretchLayout() == false)
            return record;

        String componentName = record.getComponentName();
        if (!componentName.equals("af:table") && !componentName.equals("af:treeTable"))
            return record;
        
        String amPath = record.getAmPath();
        HashSet<String> fixStatuses = record.getSetfixStatus();
        
        String oldVoUsageFetchSize = record.getVoUsageFetchSizeInAm();
        
        if(!isEmpty(oldVoUsageFetchSize) && !oldVoUsageFetchSize.trim().equals("-37"))
        {
            Integer intoldfetchSize = Integer.parseInt(oldVoUsageFetchSize);
            if(intoldfetchSize < 15 || intoldfetchSize == -1)
            {
                System.out.println("violation of new standard");
            }
        }
        else
        {
            // definately need to fix this one
            System.out.println("violation of new standard");
        }

        return record;
    }

    public TuningAnalysisRecord fixAmsForTables(TuningAnalysisRecord record, boolean bDoADE) {
        if (record == null) {
            return null;
        }

        if (!record.isTableWrappedByPanelStretchLayout())
            return record;
        
        // -------------Fix AM
        String amPath = record.getAmPath();
        // String iteratorFixStatus = record.getFixStatus();
        HashSet<String> fixStatuses = record.getSetfixStatus();
        String voDefaultFetchSizeFixStatus = "";
        try {

            if (isEmpty(amPath)) {
                record.addFixStatus(TuningConstants.FixStatus.WILL_NOT_FIX);
                record.setFixStatusComment(record.getFixStatusComment() + " \n Am Path is null");
                return record;
            }
            if (bDebug) {
                System.out.println("Processing am: : " + amPath);
            }

            if (fixStatuses != null &&
                (fixStatuses.contains(TuningConstants.FixStatus.ITEARTOR_RANGESIZE_FIX_SUCCESS) || fixStatuses.contains(TuningConstants.FixStatus.ITEARTOR_RANGESIZE_FIX_NOCHANGE) ||
                 fixStatuses.contains(TuningConstants.FixStatus.FIX_MULTITERATOR_SUCCESS))) {

                TuningAnalysisRecord newrecord = Rules.calculateVOFetchSize(record, m_map_violatingBcs_records);
                record.addRuleApplied(TuningConstants.Rules.FIX_TUNE_TABLES_VOFETCHSIZE);
                String amFixStatus = newrecord.getFixStatus();
               
                
                if (!isEmpty(amFixStatus) && amFixStatus.contains(TuningAnalysisRecord.STATUS_WILLFIX_AM)) {
                    boolean defaultVONeedsFix = doesDefaultVONeedFix(record, record.getVoPath(), defaultVOFetchSizesFixed);
                    if (defaultVONeedsFix) {
                        if (bDebug) {
                            System.out.println("Default vo needs fix");
                        }
                        // if vo needs fix and fix cannot be made then skip the record - because we cannot be setting the vo usage to > default fetch size. runtime error
                        if (!canFixVO(record, record.getVoPath())) {
                            String msg =
                                "[ERROR] Default VO Needs fix (usageSize < defaultFetchSize).. but vo cannot be fixed because it cannot be obtained";
                            msg = msg + "\n Skipping this record and not fixing am vo usage since it will throw a runtime error";
                            if(bDebug)
                               System.out.println(msg);
                            errorLogger.log(Level.SEVERE, msg); 
                            record.addFixStatus(TuningConstants.FixStatus.AM_VO_FETCH_FAILED);
                            //record.setFixStatus(iteratorFixStatus + "," +
                            //                    TuningAnalysisRecord.STATUS_FIX_AM_FAILED);
                            record.setFixStatusComment(msg);
                            return record;

                        } else {
                            // fix vo
                            String voPath = record.getVoPath();
                            XmlFixer voFixer = new XmlFixer(voPath);

                            boolean voFixMade = fixDefaultFetchSizeInVO(record, voFixer, voPath);
                            if (voFixMade) {
                                if (bDoADE) {
                                    String vofileContents = voFixer.getFileContentsAsString(false);

                                    applyFix(voPath, vofileContents);
                                }

                                //record.setFixStatus(iteratorFixStatus +
                                //                   " , " +
                                //                   TuningAnalysisRecord.STATUS_DEFAULTVO_FIX_SUCCESS);
                                record.addFixStatus(TuningConstants.FixStatus.DEFAULTVO_FIX_SUCCESS);
                                //voDefaultFetchSizeFixStatus =
                                //       TuningAnalysisRecord.STATUS_DEFAULTVO_FIX_SUCCESS;
                                String newDefaultFetchSize = record.getVoDefaultFetchSizeNewValue();
                                Integer defaultnewInteger = null;
                                try {
                                    defaultnewInteger = Integer.parseInt(newDefaultFetchSize);
                                    defaultVOFetchSizesFixed.put(voPath, defaultnewInteger);
                                } catch (Exception ex) {
                                    System.out.println("Exception while trying to conver to integer while putting map: " + newDefaultFetchSize);
                                }
                            } else {

                                // fix was needed but fix was not made.. this is an error because it will throw a runtime exception. do not fix the am usage
                                //   record.setFixStatus(iteratorFixStatus +
                                //                      " , " +
                                //                     TuningAnalysisRecord.STATUS_DEFAULTVO_FIX_FAILED);
                                record.addFixStatus(TuningConstants.FixStatus.DEFAULTVO_FIX_FAILED);
                                record.addFixStatus(TuningConstants.FixStatus.AM_VO_FETCH_FAILED);
                                record.setFixStatusComment("Default vo fix was needed but it failed.. skipping record.");

                                return record;
                            }
                        }
                    }
                }

                if (!isEmpty(amFixStatus) && amFixStatus.contains(TuningAnalysisRecord.STATUS_WILLFIX_AM)) {
                    XmlFixer amFixer = new XmlFixer(amPath);
                    boolean fixMade = fixFetchSizeInAm(record, amFixer);
                    if (fixMade) {
                        if (bDoADE) {
                            String amFileContents = amFixer.getFileContentsAsString(false);

                            applyFix(amPath, amFileContents);
                        }

                        record.addFixStatus(TuningConstants.FixStatus.AM_VO_FETCH_SUCESS);

                    }
                } else if (!isEmpty(amFixStatus) && amFixStatus.contains(TuningAnalysisRecord.STATUS_MULTIPLE_BC)) {
                    String bcKey = record.getAmFullPackage() + "#" + record.getVoInstanceName();
                    // These records should set status to multiple bc. they will be fixed later on.
                    record.addFixStatus(TuningConstants.FixStatus.STATUS_MULTIPLE_BC);
                    UiFileHelper.addToMap(onlyreq_multipleBcs_fixedrecords, bcKey, record);
                } else if (amFixStatus.contains(TuningAnalysisRecord.STATUS_AMFETCH_UNCHANGED)) {
                    //    record.setFixStatus(iteratorFixStatus+" " + amFixStatus);
                    record.addFixStatus(TuningConstants.FixStatus.AM_VO_FETCH_UNCHANGED);
                } else {
                }

            } else {
                //list_records.add(record);
                // Iterator was not fixed successfully, however still might be a case ofmultiple bc so check required
                //String bcKey = record.getAmFullPackage() + "#" + record.getVoInstanceName();
                // These records should set status to multiple bc. they will be fixed later on.
                //record.addFixStatus(TuningConstants.FixStatus.STATUS_MULTIPLE_BC);
                //UiFileHelper.addToMap(onlyreq_multipleBcs_fixedrecords, bcKey, record);
            }
        } catch (Exception ex) {
            String msg = String.format("Exception occurred while trying to fix am: %s. Exception: %s", amPath, ex.getMessage());
            record.addFixStatus(TuningConstants.FixStatus.AM_VO_FETCH_FAILED);
            System.out.println(msg);
            record.setFixStatusComment(record.getFixStatusComment() + " \n " + msg);
            return record;
        }


        return record;
    }


    public void fixCombobBoxListRangeSize(TuningAnalysisRecord record, boolean bDoADE) {
        if (!fix_combobox_listRangeSize) {
            return;
        }


        String componentName = record.getComponentName();
        if (!componentName.equals("af:inputComboboxListOfValues")) {
            return;
        }

        String lovType = record.getCustomAttrWithKey(TuningConstants.LOV_TYPE);
        if (isEmpty(lovType) || !lovType.equals(TuningConstants.LOV_TYPE_MODEL_DRIVEN)) {
            return;
        }

        Rules.calculateListRangeSize(record,m_fixlov_only_if_max_exceeded,m_fixlov_Max);
        if(record.hasFixStatus(TuningConstants.FixStatus.LISTRANGESIZE_WILL_NOT_FIX))
        {
            return;
        }
        
        String newListRangeSize = record.getCustomAttrWithKey(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE_NEW);
        if (isEmpty(newListRangeSize) || newListRangeSize.equals(TuningConstants.NO_CHANGE)) {
            record.addFixStatus(TuningConstants.FixStatus.LISTRANGESIZE_NOCHANGE);
            return;
        }
        record.addRuleApplied(TuningConstants.Rules.FIX_COMBOBOX_LRS);
        // When it reaches this point need to apply the nex fix status
        String bcToFix = record.getVoPath();
        if (isEmpty(bcToFix)) {
            record.addFixStatus(TuningConstants.FixStatus.LISTRANGESIZE_FIX_FAILED);
            record.setFixStatusComment(record.getFixStatusComment() + " \n " + " View object not found ");
        }
        String listBindingName = record.getLovUsesName();
        try {
            XmlFixer listRangeSizeFixer = new XmlFixer(bcToFix);
            boolean fixMade = makeListRangeSizeFix(record, bcToFix, listBindingName, newListRangeSize, listRangeSizeFixer);
            if (fixMade) {
                if (bDoADE) {
                    String bcContents = listRangeSizeFixer.getFileContentsAsString(false);
                    applyFix(listRangeSizeFixer.getFilePath(), bcContents);
                }
                record.addFixStatus(TuningConstants.FixStatus.LISTRANGESIZE_FIX_SUCESS);
            }

        } catch (Exception ex) {

            record.addFixStatus(TuningConstants.FixStatus.LISTRANGESIZE_FIX_FAILED);
            String msg = "[ERROR] Ecxeption while trying to fix list range size for combobox: " + ex.getMessage();
            System.out.println(msg);
            record.setFixStatusComment(record.getFixStatusComment() + " \n" +
                    msg);
            System.out.println("Skipping this record");
        }

    }

    public void fixSOCListRangeSize(TuningAnalysisRecord record, boolean bDoADE) {
        if (!fix_combobox_listRangeSize) {
            return;
        }


        String componentName = record.getComponentName();
        if (!componentName.equals("af:selectOneChoice")) {
            return;
        }

        String lovType = record.getCustomAttrWithKey(TuningConstants.LOV_TYPE);
        if (isEmpty(lovType) || !lovType.equals(TuningConstants.LOV_TYPE_MODEL_DRIVEN)) {
            return;
        }

        Rules.calculateListRangeSize(record,m_fixlov_only_if_max_exceeded,m_fixlov_Max);
        String newListRangeSize = record.getCustomAttrWithKey(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE_NEW);
        if (isEmpty(newListRangeSize) || newListRangeSize.equals(TuningConstants.NO_CHANGE)) {
            record.addFixStatus(TuningConstants.FixStatus.LISTRANGESIZE_NOCHANGE);
            return;
        }
        record.addRuleApplied(TuningConstants.Rules.FIX_SELECTONECHOICE_LRS);
        // When it reaches this point need to apply the nex fix status
        String bcToFix = record.getVoPath();
        if (isEmpty(bcToFix)) {
            record.addFixStatus(TuningConstants.FixStatus.LISTRANGESIZE_FIX_FAILED);
            record.setFixStatusComment("Could not find List View Object");
            return;
        }
        String listBindingName = record.getLovUsesName();
        try {
            XmlFixer listRangeSizeFixer = new XmlFixer(bcToFix);
            boolean fixMade = makeListRangeSizeFix(record, bcToFix, listBindingName, newListRangeSize, listRangeSizeFixer);
            if (fixMade) {
                if (bDoADE) {
                    String bcContents = listRangeSizeFixer.getFileContentsAsString(false);
                    applyFix(listRangeSizeFixer.getFilePath(), bcContents);
                }
                record.addFixStatus(TuningConstants.FixStatus.LISTRANGESIZE_FIX_SUCESS);
            }

        } catch (Exception ex) {
            record.addFixStatus(TuningConstants.FixStatus.LISTRANGESIZE_FIX_FAILED);
            String msg = "[ERROR] Ecxeption while trying to fix list range size for combobox: " + ex.getMessage();
            System.out.println(msg);
            record.setFixStatusComment(record.getFixStatusComment() + " \n" +
                    msg);
            System.out.println("Skipping this record");
        }


    }

    public boolean canFixVO(TuningAnalysisRecord record, String bcPath) {
        if (record == null) {
            return false;
        }


        if (isEmpty(bcPath) || bcPath.trim().endsWith(".jar")) {
            return false;
        }
        if (bcPath.trim().contains("JAR:")) {
            return false;
        }

        File f = new File(bcPath);
        if (f.exists()) {
            return true;
        }

        System.out.println("Cannot fix vo path because absoltue vo cannot be obtained");
        return false;
    }


    /**
     *
     * VO Does not need fix if:
     * 1. VO Path is null
     * 1. If defaultFetchSize ==1 or not set
     * 2. If defaultFetchSize is not > 0
     * 3. If newFetchSize is is not > 0
     * VO Needs fix if
     * 1. Default Fetch size > newFetchSize
     * @param record
     * @return
     */
    public boolean doesDefaultVONeedFix(TuningAnalysisRecord record, String bcPath, HashMap<String, Integer> alreadyFixedVos) {
        /* If the am is going to be fixed - need to make sure that :
         * 1. VO Usage fetch size is not less than the DefaultFetchSize.
         * 2. If voUsageFetchSize is less than the default fetch size : fix the default fetch size to be the voUsageFetchSize
         */
        // preliminary validations
        if (record == null) {
            return false;
        }

        String defaultFetchSize = record.getVoDefaultFetchSize();
        if (isEmpty(defaultFetchSize) || defaultFetchSize.trim().equalsIgnoreCase(TuningConstants.VO_FETCHSIZE_NOTSET)) {
            return false;
        }
        if (!isEmpty(defaultFetchSize) && defaultFetchSize.trim().equalsIgnoreCase(TuningConstants.VO_BASEVO_NOT_FOUND)) {
            record.setFixStatusComment(record.getFixStatusComment() + " \n Base vo not found.. cannot make fix.");
            return true;
        }
        
        boolean voNeedsFix = false;

        Integer voUsageFetchSizeNewValInteger = null;
        Integer defaultFetchSizeInteger = null;
        
        
        // Get the default vo fetch size from disk. No point in relying on a map because relative paths screw up the logic
        try{
            // Only if the path is not empty, check the default fetch size  on disk. Because it is possible that the 
            // bcPath is empty if the vo is a list of know vos. In that case the default fetch size is whtever is in the reocrd
            if(!isEmpty(bcPath) && bcPath.trim().endsWith(".xml"))               
            {
                defaultFetchSize  = ViewObjectHelper.getFetchSize(bcPath);
                if(isEmpty(defaultFetchSize))
                    return false;
            }
            
            
            // If there is a mismatch between the vo default fetchsize and the one we are now seeing on disk, means that there was a change somewhere in the middle
            // update the record if this is the case. on second thoughts dont set this. it will be confusing in the spreadsheet to see so many differring values.
//            if(baseVoFetchSizeOnDisk.equals(record.getVoDefaultFetchSize()))
//            {
//                record.setVoDefaultFetchSizeNewValue(baseVoFetchSizeOnDisk);
//            }
        }catch(Exception ex)
        {
            
            record.setDiagnostic(record.getDiagnostic()+"Error while trying to get base vo fetch size from disk: " + ex.getMessage());
            record.setFixStatusComment(record.getFixStatusComment()+"\n Error while trying to get base vo fetch size from disk: " + ex.getMessage());
            return true;
        }
        // --End of get default vo fetch size from disk
        
        defaultFetchSizeInteger=Integer.parseInt(defaultFetchSize);
//        if (alreadyFixedVos.containsKey(bcPath)) {
//            defaultFetchSizeInteger = alreadyFixedVos.get(bcPath);
//        } else {
//            String defaultFetchSize = record.getVoDefaultFetchSize();
//            if (isEmpty(defaultFetchSize) || defaultFetchSize.trim().equalsIgnoreCase(TuningConstants.VO_FETCHSIZE_NOTSET)) {
//                return false;
//            }
//            // Added here because if the base vo is not found - DO  NOT MAKE ANY FIX.
//            // TO BE CONSERVATIVE - return the fact that the base vo does need fix.
//            if (!isEmpty(defaultFetchSize) && defaultFetchSize.trim().equalsIgnoreCase(TuningConstants.VO_BASEVO_NOT_FOUND)) {
//                record.setFixStatusComment(record.getFixStatusComment() + " \n Base vo not found.. cannot make fix.");
//                return true;
//            }
//            try {
//                defaultFetchSizeInteger = Integer.parseInt(defaultFetchSize);
//            } catch (Exception ex) {
//                String msg =
//                    String.format("ERROR: [doesDefaultVONeedFix]: VOPath %s: cannot convert defaultFetchSize %s to Integer", bcPath, defaultFetchSize);
//                errorLogger.log(Level.SEVERE, msg);
//                if(bDebug)
//                   System.out.println(msg); 
//                return false;
//            }
//        }
        if (defaultFetchSizeInteger == null || defaultFetchSizeInteger <= 0) {
            return false;
        }

        String voUsageFetchSizeNewVal = record.getVoUsageFetchSizeNewValue();
        if (isEmpty(voUsageFetchSizeNewVal)) {
            return false;
        }
        try {
            voUsageFetchSizeNewValInteger = Integer.parseInt(voUsageFetchSizeNewVal);

        } catch (Exception ex) {
            String msg =
                String.format("ERROR: [doesDefaultVONeedFix]: VOPath %s: cannot convert voUsageNewFetchSize %s to Integer", bcPath, voUsageFetchSizeNewVal);
            errorLogger.log(Level.SEVERE, msg); 
            if(bDebug)
               System.out.println(msg); 
            return false;
        }

        if (voUsageFetchSizeNewValInteger == null || voUsageFetchSizeNewValInteger <= 0) {
            return false;
        }

        if (defaultFetchSizeInteger > voUsageFetchSizeNewValInteger) {
            voNeedsFix = true;
            record.setVoDefaultFetchSizeNewValue(voUsageFetchSizeNewValInteger.toString());
        } else {
            // default vo does not need fix, but if the default fetch size was updated in already fixed vos then update the new val
            if (alreadyFixedVos.containsKey(bcPath)) {
                record.setVoDefaultFetchSizeNewValue(defaultFetchSizeInteger.toString());
            }
        }

        return voNeedsFix;

    }


    public void fixTuneCombobox(TuningAnalysisRecord record, boolean bDoADE) {
        if (!fix_tune_combobx) {
            return;
        }

        String componentName = record.getComponentName();
        if (!componentName.equals("af:inputComboboxListOfValues")) {
            return;
        }

        String lovType = record.getCustomAttrWithKey(TuningConstants.LOV_TYPE);
        if (isEmpty(lovType) || !lovType.equals(TuningConstants.LOV_TYPE_MODEL_DRIVEN)) {
            return;
        }
        Rules.calculateLOVModelDrivenFetchSize(record);
        String newFetchSize = record.getVoUsageFetchSizeNewValue();
        if (isEmpty(newFetchSize) || newFetchSize.equals(TuningConstants.NO_CHANGE)) {
            record.addFixStatus(TuningConstants.FixStatus.VA_FETCHSIZE_FIX_NOCHANGE);
            return;
        }
        record.addRuleApplied(TuningConstants.Rules.FIX_COMBOBOX_VA_FETCHSIZE);
        // When it reaches this point need to apply the nex fix status


        try {
            String defaultVoPath = record.getBaseVoPath();
            checkAndMakeDefaultVOFix(record, defaultVoPath, bDoADE);
        } catch (Exception ex) {
            record.addFixStatus(TuningConstants.FixStatus.VA_FETCHSIZE_FIX_FAILED);
            record.addFixStatus(TuningConstants.FixStatus.DEFAULTVO_FIX_FAILED);
            record.setFixStatusComment(record.getFixStatusComment() + " \n " + ex.getMessage());
            return;
        }


        String bcKey = record.getCustomAttrWithKey(TuningConstants.CUSTOMATTR_LOV_VA_BCPATH_VANAME);
        String[] bcKeyParts = bcKey.split("#");
        if (bcKeyParts.length != 2) {
            record.addFixStatus(TuningConstants.FixStatus.VA_FETCHSIZE_FIX_FAILED);
            record.setFixStatusComment(record.getFixStatusComment() + " \n " + " bcKeyFormatting issue");
        }
        String bcPathTofix = bcKeyParts[0];
        String vaName = bcKeyParts[1];


        try {
            XmlFixer bcFixer = new XmlFixer(bcPathTofix);
            boolean fixMade = makeVAFetchSizeFix(record, bcPathTofix, vaName, newFetchSize, bcFixer);
            if (fixMade) {
                if (bDoADE) {
                    String bcContents = bcFixer.getFileContentsAsString(false);
                    applyFix(bcFixer.getFilePath(), bcContents);
                }
                record.addFixStatus(TuningConstants.FixStatus.VA_FETCHSIZE_FIX_SUCCESS);
            }

        } catch (Exception ex) {
            record.addFixStatus(TuningConstants.FixStatus.VA_FETCHSIZE_FIX_FAILED);
            String msg = "[ERROR] Ecxeption while trying to fix VA Fetch size for bc:" + bcPathTofix + " Exception: " + ex.getMessage();
            System.out.println(msg);
            record.setFixStatusComment(record.getFixStatusComment() + " \n" +
                    msg);
            System.out.println("Skipping this record");
        }
    }

    public void fixTuneSOC(TuningAnalysisRecord record, boolean bDoADE) {
        if (!fix_tune_selectoneChoice) {
            return;
        }

        String componentName = record.getComponentName();
        if (!componentName.equals("af:selectOneChoice")) {
            return;
        }

        String lovType = record.getCustomAttrWithKey(TuningConstants.LOV_TYPE);
        if (isEmpty(lovType) || !lovType.equals(TuningConstants.LOV_TYPE_MODEL_DRIVEN)) {
            return;
        }

        Rules.calculateLOVModelDrivenFetchSize(record);
        String newFetchSize = record.getVoUsageFetchSizeNewValue();
        if (isEmpty(newFetchSize) || newFetchSize.equals(TuningConstants.NO_CHANGE)) {
            record.addFixStatus(TuningConstants.FixStatus.VA_FETCHSIZE_FIX_NOCHANGE);
            return;
        }
        record.addRuleApplied(TuningConstants.Rules.FIX_SELECTONECHOICE_VA_FETCHSIZE);

        try {
            String defaultVoPath = record.getBaseVoPath();
           
            checkAndMakeDefaultVOFix(record, defaultVoPath, bDoADE);
        } catch (Exception ex) {
            record.addFixStatus(TuningConstants.FixStatus.VA_FETCHSIZE_FIX_FAILED);
            record.addFixStatus(TuningConstants.FixStatus.DEFAULTVO_FIX_FAILED);
            record.setFixStatusComment(record.getFixStatusComment() + " \n " + ex.getMessage());
            return;
        }


        String bcKey = record.getCustomAttrWithKey(TuningConstants.CUSTOMATTR_LOV_VA_BCPATH_VANAME);
        String[] bcKeyParts = bcKey.split("#");
        if (bcKeyParts.length != 2) {
            record.addFixStatus(TuningConstants.FixStatus.VA_FETCHSIZE_FIX_FAILED);
            record.setFixStatusComment(record.getFixStatusComment() + " \n " + " bcKeyFormatting issue");
        }
        String bcPathTofix = bcKeyParts[0];
        String vaName = bcKeyParts[1];


        try {
            XmlFixer bcFixer = new XmlFixer(bcPathTofix);
            boolean fixMade = makeVAFetchSizeFix(record, bcPathTofix, vaName, newFetchSize, bcFixer);
            if (fixMade) {
                if (bDoADE) {
                    String bcContents = bcFixer.getFileContentsAsString(false);
                    applyFix(bcFixer.getFilePath(), bcContents);
                }
                record.addFixStatus(TuningConstants.FixStatus.VA_FETCHSIZE_FIX_SUCCESS);
            }

        } catch (Exception ex) {
            record.addFixStatus(TuningConstants.FixStatus.VA_FETCHSIZE_FIX_FAILED);
            String msg = "[ERROR] Ecxeption while trying to fix VA Fetch size for bc:" + bcPathTofix + " Exception: " + ex.getMessage();
            System.out.println(msg);
            record.setFixStatusComment(record.getFixStatusComment() + " \n" +
                    msg);
            System.out.println("Skipping this record");
        }
    }

    public boolean checkAndMakeDefaultVOFix(TuningAnalysisRecord record, String defaultVoPath, boolean bDoADE) throws Exception {

        // Determine if default vo needs fix
        boolean defaultVONeedsFix = doesDefaultVONeedFix(record, defaultVoPath, defaultVOFetchSizesFixed);
        if (defaultVONeedsFix) {
            if (bDebug) {
                System.out.println("Default vo needs fix " + defaultVoPath);
            }

            // if vo needs fix and fix cannot be made then skip the record - because we cannot be setting the vo usage to > default fetch size. runtime error
            if (!canFixVO(record, defaultVoPath)) {
                String msg = "[ERROR] Default VO Needs fix (usageSize < defaultFetchSize).. but vo cannot be fixed because it cannot be obtained";
                msg = msg + "\n Skipping this record and not fixing am vo usage since it will throw a runtime error";
                if(bDebug)
                   System.out.println(msg); 
                errorLogger.log(Level.SEVERE, msg);
                throw new Exception(msg);
            }

            XmlFixer voFixer = new XmlFixer(defaultVoPath);

            try {
                boolean voFixMade = fixDefaultFetchSizeInVO(record, voFixer, defaultVoPath);
                if (voFixMade) {

                    if (bDoADE) {
                        String vofileContents = voFixer.getFileContentsAsString(false);
                        applyFix(defaultVoPath, vofileContents);
                    }
                    record.addFixStatus(TuningConstants.FixStatus.DEFAULTVO_FIX_SUCCESS);
                    String newDefaultFetchSize = record.getVoDefaultFetchSizeNewValue();
                    Integer defaultnewInteger = null;
                    try {
                        defaultnewInteger = Integer.parseInt(newDefaultFetchSize);
                        defaultVOFetchSizesFixed.put(defaultVoPath, defaultnewInteger);
                    } catch (Exception ex) {
                        System.out.println("Exception while trying to conver to integer while putting map: " + newDefaultFetchSize);
                    }
                }
            } catch (Exception ex) {
                String msg =
                    String.format("Exception occurred while making default vo fix in vo %s . Exception %s ", defaultVoPath, ex.getMessage());
                throw new Exception(msg);

            }


        }
        return true;
    }

    /**
     *return true/false if any fix was made
     * @param record
     * @param pagedefFixer
     * @return
     * @throws Exception
     */
    private boolean fixDefaultFetchSizeInVO(TuningAnalysisRecord record, XmlFixer fixer, String voPath) throws Exception {
        if (bDebug) {
            System.out.println("Fixing Default Fetch Size in VO " + voPath);
        }
        if (record == null) {
            return false;
        }

        if (isEmpty(voPath)) {
            return false;
        }

        File voFile = new File(voPath);
        if (!voFile.exists()) {
            throw new Exception("VO does not exist: " + voPath);
        }

        boolean validNewFetchSize = true;
        String defaultFetchSizeNewValue = record.getVoDefaultFetchSizeNewValue();
        if (isEmpty(defaultFetchSizeNewValue)) {
            if (bDebug) {
                System.out.println("Defaut fetch size new value is null.. nothing to fix");
            }
            return false;
        }

        if (!validNewFetchSize) {
            System.out.println("New VO Fetch size was not valid");
            return false;
        }

        XMLDocument voXml = XMLParserHelper.getXMLDocument(voPath);
        NodeList viewObjectNodes = voXml.getElementsByTagName("ViewObject");

        if (!XMLParserHelper.hasOneNOnlyOneNode(viewObjectNodes)) {
            throw new Exception("cannot modify VO.. one and only one node of ViewObject expected");
        }

        Node voNode = viewObjectNodes.item(0);

        try {
            String defaultFetchSize = XMLParserHelper.getAttributeValue(voNode, "FetchSize");

            if (isEmpty(defaultFetchSize) || defaultFetchSize.trim().equalsIgnoreCase(TuningConstants.VO_FETCHSIZE_NOTSET)) {
                System.out.println("Default fetch size was empty.. not fixing default fetch size");
                return false;
            } else {
                return fixer.modifyAttributeIrrespectiveOldValue(voNode, "FetchSize", defaultFetchSizeNewValue);
            }

        } catch (Exception ex) {
            String msgex = "Exception occurred while trying to fix default fetch size: " + ex.getMessage();
            if(bDebug)
              System.out.println(msgex); 
            errorLogger.log(Level.SEVERE, msgex); 
            throw new Exception(ex);
        }

    }

    public boolean makeVAFetchSizeFix(TuningAnalysisRecord record, String bcToFix, String viewAccessorName, String newVAFetchSize,
                                      XmlFixer listRangeSizeFixer) throws Exception {

        XMLDocument bcXml = XMLParserHelper.getXMLDocument(bcToFix);

        NodeList vaNodes = bcXml.getElementsByTagName("ViewAccessor");
        if (vaNodes == null || vaNodes.getLength() == 0) {
            String msg = String.format("Could not find VA with name %s in bc %s", viewAccessorName, bcToFix);
            throw new Exception(msg);
        }
        int len = vaNodes.getLength();
        Node vaNode = null;
        for (int i = 0; i < len; i++) {
            Node vaNodeFound = vaNodes.item(i);
            String nodeName = vaNodeFound.getNodeName();
            if (!isEmpty(nodeName) && nodeName.equals("ViewAccessor")) {
                String vaName = XMLParserHelper.getAttributeValue(vaNodeFound, "Name");
                if (!isEmpty(vaName) && vaName.equals(viewAccessorName)) {
                    vaNode = vaNodeFound;
                    break;
                }
            }
        }
        if (vaNode == null) {
            String msg = String.format("Could not find VA with name %s in bc %s", viewAccessorName, bcToFix);
            throw new Exception(msg);
        }


        String origFetchSize = XMLParserHelper.getAttributeValue(vaNode, "FetchSize");
        if (origFetchSize != null && origFetchSize.trim().equals(newVAFetchSize)) {
            // might be the case when multiple records are fixing the same file - it might have already been set to 250.
            // no need to re apply the fix
            record.addFixStatus(TuningConstants.FixStatus.VA_FETCHSIZE_FIX_SUCCESS);
            return false;
        }

        if (isEmpty(origFetchSize)) {
            listRangeSizeFixer.modifyAttribute(vaNode, "FetchSize", origFetchSize, newVAFetchSize, true);
        } else {
            listRangeSizeFixer.modifyAttribute(vaNode, "FetchSize", origFetchSize, newVAFetchSize, false);
        }

        return true;
    }

    public boolean makeListRangeSizeFix(TuningAnalysisRecord record, String bcToFix, String listBindingName, String newListRangeSize,
                                        XmlFixer listRangeSizeFixer) throws Exception {

        XMLDocument bcXml = XMLParserHelper.getXMLDocument(bcToFix);
        NodeList listViewObj = bcXml.getElementsByTagName("ViewObject");
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
            System.out.println("Expecting one and only one view object node");
            throw new Exception("Expecting one and only one view object node in vo file: " + bcToFix);
        }
        Node voNode = listViewObj.item(0);
        Node listBindingNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(voNode, "ListBinding", "Name", listBindingName);
        if (listBindingNode == null) {
            throw new Exception("Could not find listbinding with name: " + listBindingName + " in file: " + bcToFix);
        }
        String listRangeSize = XMLParserHelper.getAttributeValue(listBindingNode, "ListRangeSize");
        if (listRangeSize != null && listRangeSize.trim().equals(newListRangeSize)) {
            // might be the case when multiple records are fixing the same file - it might have already been set to 250.
            // no need to re apply the fix
            return false;
        }
        if (isEmpty(listRangeSize)) {
            listRangeSizeFixer.modifyAttribute(listBindingNode, "ListRangeSize", listRangeSize, newListRangeSize, true);
        } else {
            listRangeSizeFixer.modifyAttribute(listBindingNode, "ListRangeSize", listRangeSize, newListRangeSize, false);
        }

        return true;
    }

    public void applyFix(String absPath, String newFileContents) throws Exception {
        File file = new File(absPath);
        if (!file.exists()) {
            throw new Exception("while making fix, file not found: " + absPath);
        }
        diffHelper.checkoutAndDelete(file);

        FileWriter fw = new FileWriter(file.getAbsolutePath());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(newFileContents);
        if (bw != null)
            bw.close();
        if (fw != null)
            fw.close();
        diffHelper.checkinAndDiff(file);
    }

    public String printStatistics(Date startDate) {
        StringBuilder summary = new StringBuilder();
        // Print Statistics
        Date endDate = new Date();
        System.out.println("=================================================");
        System.out.println("Run Statistics");
        if (m_jspxCount > 0) {
            summary.append("\n Total Number of jspx files reviewed: " + m_jspxCount);
            System.out.println("Total Number of jspx files reviewed: " + m_jspxCount);
        }
        if (m_jsffCount > 0) {
            summary.append("\n Total Number of jsff files reviewed: " + m_jsffCount);
            System.out.println("Total Number of jsff files reviewed: " + m_jsffCount + "\n");
        }
        summary.append("\n Start Time: " + startDate.toString());
        System.out.println("Start Time: " + startDate.toString());
        summary.append("\n End Time: " + endDate.toString());
        System.out.println("End Time: " + endDate.toString());
        long elapsedTime = endDate.getTime() - startDate.getTime();
        summary.append("\n Elapsed Time (sec): " + elapsedTime / 1000);
        System.out.println("Elapsed Time (sec): " + elapsedTime / 1000);
        summary.append("\n =================================================");
        System.out.println("=================================================");

        System.out.println("=== Default VO Fixes Statistics ===");
        if (defaultVOFetchSizesFixed != null) {
            System.out.println("Number of default VOs updated with new default fetch sizes: " + defaultVOFetchSizesFixed.size());
            Set<String> keys = defaultVOFetchSizesFixed.keySet();
            Iterator<String> keyIter = keys.iterator();
            while (keyIter.hasNext()) {
                String key = keyIter.next();
                Integer val = defaultVOFetchSizesFixed.get(key);
                String msg = String.format("Default FetchSizeSet: %s in VO: %s", val, key);
                System.out.println(msg);

            }
        }

        return summary.toString();
    }


    public static String getPass1AnalysisPath(String analysisDirectory) {
        if(analysisDirectory==null || analysisDirectory.equals(""))
            return analysisDirectory;
        
        if (fix_MultipleBcUsages) {
            String searchStr = "/fusionapps/fin/components";
            int index = analysisDirectory.indexOf(searchStr);
            if (index == -1) {
                // so that it works for all families and not just financials
                searchStr = "/components";
                index = analysisDirectory.indexOf(searchStr);
                if (index == -1) {
                   searchStr = "/atgpf/applcore/applications";
                   index = analysisDirectory.indexOf(searchStr);
                }
                if (index == -1) {
                   searchStr = "/emcore";
                   index = analysisDirectory.indexOf(searchStr);
                }
                if(index == -1)
                   return null;
            }
            return analysisDirectory.substring(0, index + searchStr.length());
        } else {
            return analysisDirectory;
        }

    }

    /**
     * recurse over the crawl directory and for each valid ui file - process it.
     * Valid directories -
     * - should not contain, /dbSchema/ or /model/ or /publicModel/ or /test/ or /classes/
     * Valid UI files
     * - jspx or jsff extensions
     * - and not in folder with /classes/ or /test/ or /model/ or /publicModel/
     * @param path
     * @param bDoADE
     */
    private void crawlDirectoryForPass1(String path) {
        if (path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            String absFilePath = listOfFiles[i].getAbsolutePath();
            if(absFilePath.contains(".ade_path"))
                continue;
            
            if (listOfFiles[i].isFile()) {
                if (isValidUIFile(absFilePath)) {
                    if (absFilePath.trim().endsWith(".jsff")) {
                        m_jsffCount++;
                    } else if (absFilePath.trim().endsWith(".jspx")) {
                        m_jspxCount++;
                    }
                    analyseUiFile(listOfFiles[i]);
                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidDirectory(dir)) {
                    crawlDirectoryForPass1(dir);
                }
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    /**
     * recurse over the crawl directory and for each valid ui file - process it.
     * Valid directories -
     * - should not contain, /dbSchema/ or /model/ or /publicModel/ or /test/ or /classes/
     * Valid UI files
     * - jspx or jsff extensions
     * - and not in folder with /classes/ or /test/ or /model/ or /publicModel/
     * @param path
     * @param bDoADE
     */
    private void crawlDirToFilterAnalysisOnly(String path) {
        if (path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if(listOfFiles[i]==null)
                continue;
            String absFilePath = listOfFiles[i].getAbsolutePath();
            if(isEmpty(absFilePath))
                continue;
            if(absFilePath.contains(".ade_path"))
                continue;
            if (listOfFiles[i].isFile()) {


                if (isValidUIFile(absFilePath)) {
                    if (absFilePath.trim().endsWith(".jsff")) {
                        m_jsffCount++;
                    } else if (absFilePath.trim().endsWith(".jspx")) {
                        m_jspxCount++;
                    }
                    if (allfin_errorRecords.containsKey(absFilePath)) {
                        ArrayList<TuningAnalysisRecord> list_errorrecords = allfin_errorRecords.get(absFilePath);
                        onlyrequired_errorRecords.put(absFilePath, list_errorrecords);
                    }
                    if (allfin_analysisRecords.containsKey(absFilePath)) {
                        ArrayList<TuningAnalysisRecord> list_analysisInFile = allfin_analysisRecords.get(absFilePath);
                        onlyreqiured_analysisRecords.put(absFilePath, list_analysisInFile);
                    }

                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidDirectory(dir)) {
                    crawlDirToFilterAnalysisOnly(dir);
                }
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    /**
     * Process each valid ui file. If the file has any found violations, fix them and increment the violation count
     * @param file
     * @param bDoADE
     */
    public void analyseUiFile(File file) {
        String uiFilePath = file.getAbsolutePath();
        if (bDebug) {
            System.out.println("Processing file: " + uiFilePath);
        }

        String baseDir = UiFileHelper.getAdfmsrcPath(uiFilePath);
        if (isEmpty(baseDir)) {
            System.out.println("[ERROR] base directory could not be obtained.. ");
            System.out.println("Skipping file: " + uiFilePath);
            return;
        }
        DataBindings bindings = null;
        if (bindingsMap.containsKey(baseDir)) {
            bindings = bindingsMap.get(baseDir);
        } else {
            bindings = UiFileHelper.getDataBindingsFileForUiFile(uiFilePath);
            if (bindings != null) {
                bindingsMap.put(baseDir, bindings);
            } else {
                // System.out.println("[ERROR] Bindings was null.. skipping file");
                return;
            }
        }
        if (bindings != null) {            
          
            LOVAnalysis lovanalyzer = new LOVAnalysis();
            LOVAnalysis.bDebug = bDebug;
            ArrayList<TuningAnalysisRecord> records = lovanalyzer.analyzeUIFile(file, bindings, errorLogger);

            if (records != null && records.size() > 0) {
                allfin_analysisRecords.put(uiFilePath, records);
            }

            ArrayList<TuningAnalysisRecord> errorRecordsInUiFile = lovanalyzer.getList_errorRecords();
            if (errorRecordsInUiFile != null && errorRecordsInUiFile.size() > 0) {
                allfin_errorRecords.put(uiFilePath, errorRecordsInUiFile);
            }


            HashMap<String, ArrayList<TuningAnalysisRecord>> bcUsagesInPage = lovanalyzer.getBcUsages();
            if (bcUsagesInPage != null && bcUsagesInPage.size() > 0) {
                m_map_bcs_records = addtoMap(m_map_bcs_records, bcUsagesInPage);
            }


            HashMap<String, ArrayList<TuningAnalysisRecord>> iteratorUsagesInPage = lovanalyzer.getMapOfMultipleIteratorUsages();
            if (iteratorUsagesInPage != null && iteratorUsagesInPage.size() > 0) {
                m_mapOfMultipleIteratorUsages = addtoMap(m_mapOfMultipleIteratorUsages, iteratorUsagesInPage);
            }


            HashMap<String, Integer> map_numberOfComponents = lovanalyzer.getMap_numberOfComponentsByName();
            if (map_numberOfComponents != null && map_numberOfComponents.size() > 0) {
                m_map_componentCnt = UiFileHelper.addToMap(m_map_componentCnt, map_numberOfComponents);
            }


        }
        //System.out.println("\n");
    }

    /**
     * Valid directories are those that do not contain
     * - model and dbschema and publicModel and test and classes
     * @param absDirPath
     * @return
     */
    public boolean isValidDirectory(String absDirPath) {
        if(isEmpty(absDirPath))
            return false;
        if(absDirPath.contains(".ade_path"))
            return false;
        String absDirPathLowerCase=absDirPath.toLowerCase();
        if(absDirPathLowerCase.contains("/noship/") 
           || absDirPathLowerCase.contains("/dist/") 
           || absDirPathLowerCase.contains("/test/")
           || absDirPathLowerCase.contains("servicetest")
            || absDirPathLowerCase.contains("/publicservicetest")
            || absDirPathLowerCase.contains("/publicmodeltest")
            || absDirPathLowerCase.contains("/protectedmodeltest")
            || absDirPathLowerCase.contains("/publicuitest")
            || absDirPathLowerCase.contains("/uimodeltest")
            || absDirPathLowerCase.contains("/structuretest")
            || absDirPathLowerCase.contains("/modeltest")
            || absDirPathLowerCase.contains("/uitest")
            || absDirPathLowerCase.contains("/testui")
            || absDirPathLowerCase.contains("/uimodeltest")
            || absDirPathLowerCase.contains("/testviewcontroller")
            || absDirPathLowerCase.contains("/flextest")
            || absDirPathLowerCase.contains("/uimodeler-launch-test")
            || absDirPathLowerCase.contains("/publicuibasetest")
            || absDirPathLowerCase.contains("/uipickertest"))
            return false;
         
//        if(absDirPathLowerCase.contains("/qoc"))
//            return false;
        
      /*  if(absDirPath.contains("/prj/components/projectsManagement") ||
            absDirPath.contains("/prj/components/projectsFinancialsSoa/projectManagement") ||
            absDirPath.contains("/prj/components/projectsFinancialsSoa/projectManagementControl") ||
            absDirPath.contains("/prj/components/projectsFinancialsSoa/resourceManagement") ||
            absDirPath.contains("/prj/components/projectsFinancialsEss/projectManagement") ||
            absDirPath.contains("/prj/components/projectsFinancialsEss/resourceManagement"))
            return false;*/
        
        if (absDirPathLowerCase.contains("/model/") || absDirPathLowerCase.contains("/dbSchema/") || absDirPathLowerCase.contains("/publicModel/") ||
            absDirPathLowerCase.contains("/classes/") ) {
            return false;
        }
        return true;
    }

    /**
     * Valid ui files are those that
     * - end with jspx or jsff
     * - and are not in a directory containing test or modeltest or classes or model or publicmodel
     * @param absFilePath
     * @return
     */
    public boolean isValidUIFile(String absFilePath) {
        if(isEmpty(absFilePath))
            return false;
        
        if(absFilePath.contains(".ade_path"))
            return false;
        String lowerCaseFile = absFilePath.toLowerCase();

                if(lowerCaseFile.contains("/noship/") 
                   || lowerCaseFile.contains("/dist/") 
                   || lowerCaseFile.contains("/test/")
                   || lowerCaseFile.contains("servicetest")
                    || lowerCaseFile.contains("/publicservicetest")
                    || lowerCaseFile.contains("/publicmodeltest")
                    || lowerCaseFile.contains("/protectedmodeltest")
                    || lowerCaseFile.contains("/publicuitest")
                    || lowerCaseFile.contains("/uimodeltest")
                    || lowerCaseFile.contains("/structuretest")
                    || lowerCaseFile.contains("/modeltest")
                    || lowerCaseFile.contains("/uitest")
                    || lowerCaseFile.contains("/testui")
                    || lowerCaseFile.contains("/uimodeltest")
                    || lowerCaseFile.contains("/testviewcontroller")
                    || lowerCaseFile.contains("/flextest")
                    || lowerCaseFile.contains("/uimodeler-launch-test")
                    || lowerCaseFile.contains("/publicuibasetest")
                    || lowerCaseFile.contains("/uipickertest"))
                    return false;
                
            //Filter out QOC from V1
//            if(lowerCaseFile.contains("/qoc"))
//                return false;
            
            if (absFilePath.endsWith(".jspx") || absFilePath.endsWith(".jsff")) {
                return true;
            }

        return false;
    }

    /**
     *checks whether the given string is empty
     * @param str
     * @return
     */
    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public static Logger initLogger() {
        Logger errorLogger = null;
        try {
            // Initialize logger
            errorLogger = Logger.getLogger("RunAutomatedTuningFixer");

            errorLogger.setLevel(Level.ALL);
            if(!bDebug)
                errorLogger.setLevel(Level.OFF);
            FileHandler filehandler = new FileHandler(ERROR_LOG_FILENAME);
            Formatter fileformatter = new XMLFormatter();
            filehandler.setFormatter(fileformatter);
            errorLogger.addHandler(filehandler);
            //ConsoleHandler consolehandler = new ConsoleHandler();
            //consolehandler.setLevel(Level.SEVERE);
            //errorLogger.addHandler(consolehandler);
        } catch (Exception ex) {
            System.out.println("Exception while initializing logger");
        }
        return errorLogger;
    }

    public void writeOutputXml(HashMap<String, ArrayList<TuningAnalysisRecord>> goodMap,
                                HashMap<String, ArrayList<TuningAnalysisRecord>> errorRecordsMap, String xmlFileName) {
        String ROOT_NODE = "voTuningAnalysis";

        Document doc = createXmlDocument();
        Element rootNode = doc.createElement(ROOT_NODE);
        Element goodRecords = writeMap(doc, goodMap, "AnalysisRecords");
        if (goodRecords != null)
            rootNode.appendChild(goodRecords);

        if (bDebug) {
            System.out.println("Wrote analysis records");
        }

        // write the error records
        Element errorRecordsNode = writeMap(doc, errorRecordsMap, "ErrorRecords");
        if (errorRecordsNode != null)
            rootNode.appendChild(errorRecordsNode);

        if (bDebug) {
            System.out.println("Wrote error records");
        }

        if (fix_MultipleBcUsages && multipleBcsFinalMap != null) {
            // write the error records
            Element multipleBcNode = writeMap(doc, multipleBcsFinalMap, "MultipleBcs");
            if (multipleBcNode != null)
                rootNode.appendChild(multipleBcNode);
            if (bDebug) {
                System.out.println("Wrote multiple records");
            }

        }
        //Element recordsOmitted = writeRecordsUnderRoot(doc,"OmittedDueToMultipleIter",omittedResultsMultipleIter);
        //rootNode.appendChild(recordsOmitted);
        doc.appendChild(rootNode);
        writeXmlFile(doc, xmlFileName);
    }

    private Document createXmlDocument() {
        try {
            DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
            DocumentBuilder bd = fact.newDocumentBuilder();
            return bd.newDocument();
        } catch (Exception ex) {
            System.out.println("Exception while trying to create xml document");
        }
        return null;
    }

    private void writeXmlFile(Document doc, String filename) {
        try {
            // Prepare the DOM document for writing
            Source source = new DOMSource(doc);

            // Prepare the output file
            File file = new File(filename);
            Result result = new StreamResult(file);

            // Write the DOM document to the file
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            System.out.println("Exception while writing xml output");
        } catch (TransformerException e) {
            System.out.println("Exception while writing xml output");
        }
    }

    private Element writeMap(Document doc, HashMap<String, ArrayList<TuningAnalysisRecord>> mapToWrite, String rootText) {
        if (mapToWrite == null || mapToWrite.size() <= 0) {
            return null;
        }
        Element errorRecordsNode = doc.createElement(rootText);
        Set<String> keySet = mapToWrite.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            // create the ui file node
            String filePath = keyIter.next();
            Element fileNode = doc.createElement("UIFile");
            String fileName = "";
            int lastSlashIndex = filePath.lastIndexOf("/");
            if (lastSlashIndex != -1) {
                fileName = filePath.substring(lastSlashIndex + 1);
            }
            String prod = UiFileHelper.getProductName(filePath);
            fileNode.setAttribute("product", prod);
            fileNode.setAttribute("fileName", fileName);
            fileNode.setAttribute("filePath", filePath);

            Element componentsNode = doc.createElement("components");

            ArrayList<TuningAnalysisRecord> errorrecordsList = mapToWrite.get(filePath);
            if (errorrecordsList != null && errorrecordsList.size() > 0) {
                Iterator<TuningAnalysisRecord> recordIter = errorrecordsList.iterator();
                while (recordIter.hasNext()) {
                    Element recordNode = doc.createElement("component");
                    TuningAnalysisRecord rec = recordIter.next();
                    recordNode.setAttribute("componentName", rec.getComponentName());
                    recordNode.setAttribute("dataBindingsPath", rec.getCpxFilePath());
                    recordNode.setAttribute("pageDefPath", rec.getPageDefFilePath());
                    recordNode.setAttribute("diagnostics", rec.getDiagnostic());

                    HashMap<String, String> customAttrs = rec.getLovCustomAttrs();
                    if (customAttrs != null) {
                        Set<String> customKeySet = customAttrs.keySet();
                        Iterator<String> customKeyIter = customKeySet.iterator();
                        while (customKeyIter.hasNext()) {
                            String key = customKeyIter.next();
                            String val = customAttrs.get(key);
                            recordNode.setAttribute(key, val);
                        }
                    }
                    String compName = rec.getComponentName();
                    if (compName.equals("af:table") || compName.equals("af:treeTable")) {
                        recordNode.setAttribute("autoHeightRows", rec.getAutoHeightRows());
                    }
                    recordNode.setAttribute("type", rec.getComponentType());
                    Element iteratorNode = doc.createElement("iterator");
                    iteratorNode.setAttribute("IteratorName", rec.getPageDefIteratorName());
                    iteratorNode.setAttribute("IteratorRangeSize", rec.getIteratorRangeSize());
                    iteratorNode.setAttribute("IteratorRangeSizeNewVal", rec.getIteratorRangeSizeNewValue());
                    recordNode.appendChild(iteratorNode);

                    Element bcObjNode = doc.createElement("BCObject");
                    bcObjNode.setAttribute("amPackage", rec.getAmFullPackage());
                    bcObjNode.setAttribute("amPath", rec.getAmPath());
                    bcObjNode.setAttribute("voPath", rec.getVoPath());
                    bcObjNode.setAttribute("voFullPackage", rec.getVoFullPackage());
                    bcObjNode.setAttribute("voInstanceName", rec.getVoInstanceName());
                    bcObjNode.setAttribute("voUsageFetchSize", rec.getVoUsageFetchSizeInAm());
                    bcObjNode.setAttribute("voDefaultFetchSize", rec.getVoDefaultFetchSize());
                    bcObjNode.setAttribute("voUsageFetchSizeNewVal", rec.getVoUsageFetchSizeNewValue());
                    bcObjNode.setAttribute("voDefaultFetchSizeNewVal", rec.getVoDefaultFetchSizeNewValue());

                    HashSet<String> rulesApplied = rec.getRulesApplied();
                    Element rulesAppliedNode = doc.createElement("RulesApplied");
                    if (rulesAppliedNode != null) {
                        rulesAppliedNode.setTextContent(rulesApplied.toString());
                        recordNode.appendChild(rulesAppliedNode);
                    }
                    HashSet<String> fixStatuses = rec.getSetfixStatus();
                    Element fixStatusesNode = doc.createElement("FixStatuses");
                    if (fixStatusesNode != null) {
                        fixStatusesNode.setTextContent(fixStatuses.toString());
                        recordNode.appendChild(fixStatusesNode);
                    }
                    String fixComment = rec.getFixStatusComment();
                    if (!isEmpty(fixComment)) {
                        recordNode.setAttribute("fixComment", fixComment);
                    }
                    recordNode.appendChild(bcObjNode);
                    componentsNode.appendChild(recordNode);
                }
            }

            fileNode.appendChild(componentsNode);
            errorRecordsNode.appendChild(fileNode);

        }
        return errorRecordsNode;
    }


    public HashMap<String, ArrayList<TuningAnalysisRecord>> addtoMap(HashMap<String, ArrayList<TuningAnalysisRecord>> mapToAddTo,
                                                                     HashMap<String, ArrayList<TuningAnalysisRecord>> mapToAdd) {
        if (mapToAddTo == null) {
            mapToAddTo = new HashMap<String, ArrayList<TuningAnalysisRecord>>();
        }
        if (mapToAdd == null || mapToAdd.size() <= 0) {
            return mapToAddTo;
        }
        Set<String> keySet = mapToAdd.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            ArrayList<TuningAnalysisRecord> recordListToAdd = mapToAdd.get(key);
            if (mapToAddTo.containsKey(key)) {
                ArrayList<TuningAnalysisRecord> existingRecords = mapToAddTo.get(key);
                existingRecords.addAll(recordListToAdd);
                mapToAddTo.put(key, existingRecords);
            } else {
                mapToAddTo.put(key, recordListToAdd);
            }
        }
        return mapToAddTo;

    }

    /**
     *return true/false if any fix was made
     * @param record
     * @param pagedefFixer
     * @return
     * @throws Exception
     */
    private boolean fixIteratorRangeSizeInPageDef(TuningAnalysisRecord record, XmlFixer pagedefFixer) throws Exception {

        if (record == null) {
            return false;
        }
        String pageDefPath = record.getPageDefFilePath();
        File pagedef = new File(pageDefPath);
        if (!pagedef.exists()) {
            throw new Exception("pageDef file does not exist: " + pageDefPath);
        }
        boolean validNewFetchSize = false;

        String newIteratorFetchSize = record.getIteratorRangeSizeNewValue();
        String oldIteratorFetchSize = record.getIteratorRangeSize();
        Integer iteratorFetchSizeInt = null;
        if (!isEmpty(newIteratorFetchSize) && !newIteratorFetchSize.trim().equals("-37") &&
            newIteratorFetchSize.compareTo(oldIteratorFetchSize) != 0) {
            try {
                iteratorFetchSizeInt = Integer.parseInt(newIteratorFetchSize);
                if (iteratorFetchSizeInt == -37) {
                    return false;
                }
                validNewFetchSize = true;
            } catch (NumberFormatException nex) {
                record.addFixStatus(TuningConstants.FixStatus.WILL_NOT_FIX);
                //record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
                record.setFixStatusComment(record.getFixStatusComment() + " \n Cannot convert iterator fetch size to integer: " +
                                           newIteratorFetchSize);
                System.out.println("could not convert iterator fetch size to integer: " + newIteratorFetchSize);
                throw new Exception(nex);
            }


        } else if (!isEmpty(newIteratorFetchSize) && !newIteratorFetchSize.trim().equals("-37") &&
                   newIteratorFetchSize.compareTo(oldIteratorFetchSize) == 0) {
            // treated as a fixed record
            //record.setFixStatus(TuningAnalysisRecord.STATUS_FIX_ITERATOR_NOCHANGE);
            record.addFixStatus(TuningConstants.FixStatus.ITEARTOR_RANGESIZE_FIX_NOCHANGE);
            return false;
        } else {
            record.addFixStatus(TuningConstants.FixStatus.WILL_NOT_FIX);
            //record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
            record.setFixStatusComment(record.getFixStatusComment() + " \n newIteratorRangeSize is empty or -37");
            return false;
        }
        if (!validNewFetchSize) {
            return false;
        }
        String pageDefIteratorName = record.getPageDefIteratorName();
        XMLDocument pageDefXml = XMLParserHelper.getXMLDocument(pageDefPath);
        NodeList executableNodes = pageDefXml.getElementsByTagName("executables");
        if (!XMLParserHelper.hasOneNOnlyOneNode(executableNodes)) {
            System.out.println("[ERROR] Assumption: There should be one and only one executables node in the page def.." + pageDefPath);
            throw new Exception("Could not fix page def because there should be only one executables node in page def");

        } else {
            Node executableNode = executableNodes.item(0);
            NodeList executableChildren = executableNode.getChildNodes();
            if (executableChildren != null && executableChildren.getLength() > 0) {
                int childLen = executableChildren.getLength();
                for (int i = 0; i < childLen; i++) {
                    Node executableChild = executableChildren.item(i);
                    String nodeName = executableChild.getNodeName();
                    if (!isEmpty(nodeName) && nodeName.trim().toLowerCase().endsWith("iterator")) {


                        String id = XMLParserHelper.getAttributeValue(executableChild, "id");
                        if (!isEmpty(id) && id.trim().equals(pageDefIteratorName)) {
                            String rangeSize = XMLParserHelper.getAttributeValue(executableChild, "RangeSize");
                            if (!isEmpty(rangeSize) && rangeSize.equals(newIteratorFetchSize))
                                return true;

                            if (!isEmpty(rangeSize)) {
                                // modify this attribute..
                                pagedefFixer.modifyAttribute(executableChild, "RangeSize", rangeSize, iteratorFetchSizeInt.toString(), false);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     *return true/false if any fix was made
     * @param record
     * @param pagedefFixer
     * @return
     * @throws Exception
     */
    private boolean fixFetchSizeInAm(TuningAnalysisRecord record, XmlFixer amFixer) throws Exception {

        if (record == null) {
            return false;
        }
        String amPath = record.getAmPath();
        if (isEmpty(amPath)) {
            return false;
        }
        File amFile = new File(amPath);
        if (!amFile.exists()) {
            throw new Exception("am does not exist: " + amPath);
        }
        boolean validNewFetchSize = false;

        String voInstanceName = record.getVoInstanceName();
        String vousageFetchSize = record.getVoUsageFetchSizeInAm();
        String voNewUsageFetchSize = record.getVoUsageFetchSizeNewValue();

        
        Integer voNewUsageFetchSizeInt = null;
        if (!isEmpty(voNewUsageFetchSize) && !voNewUsageFetchSize.trim().equals("-37")) {
            try {
                voNewUsageFetchSizeInt = Integer.parseInt(voNewUsageFetchSize);
                if (voNewUsageFetchSizeInt == -37) {
                    return false;
                }
                validNewFetchSize = true;
            } catch (NumberFormatException nex) {
                System.out.println("could not convert iterator fetch size to integer: " + voNewUsageFetchSize);
            }


        }
        if (!validNewFetchSize) {
            return false;
        }

        XMLDocument amXml = XMLParserHelper.getXMLDocument(amPath);
        NodeList appmoduleNodes = amXml.getElementsByTagName("AppModule");

        if (!XMLParserHelper.hasOneNOnlyOneNode(appmoduleNodes)) {
            throw new Exception("cannot modify am.. one and only one node of app  module expected");
        }

        Node amNode = appmoduleNodes.item(0);
        Node viewUsageNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(amNode, "ViewUsage", "Name", voInstanceName);
        if (viewUsageNode == null) {
            throw new Exception("Could not find view usage tag");
        }

        try {
            amFixer.modifyAttribute(viewUsageNode, "FetchSize", vousageFetchSize, voNewUsageFetchSizeInt.toString(), true);
            return true;
        } catch (Exception ex) {
            throw new Exception(ex);
        }

    }

    public HashMap<String, ArrayList<TuningAnalysisRecord>> fixMultipleBcUsages(boolean bDoADE) throws Exception {
        System.out.println("--------------Fixing multiple bc iterator Usages------------------");

        // onlyreq_multipleBcs_fixedrecords contains all the multipleBCKeys to be fixed as their index
        // also contains the fixed iterators sizes records against them.
        Set<String> keySet = onlyreq_multipleBcs_fixedrecords.keySet();
        Iterator<String> keyIter = keySet.iterator();
        HashSet<String> pageDefusages = new HashSet<String>();
        HashMap<String, ArrayList<TuningAnalysisRecord>> unifiedBcUsagesMap = new HashMap<String, ArrayList<TuningAnalysisRecord>>();

        while (keyIter.hasNext()) {
            String bcKeyToFix = keyIter.next();
            ArrayList<TuningAnalysisRecord> listOfbcRecords = new ArrayList<TuningAnalysisRecord>();
            ArrayList<TuningAnalysisRecord> fixedIteratorRecords = onlyreq_multipleBcs_fixedrecords.get(bcKeyToFix);
            ArrayList<TuningAnalysisRecord> allRecords = m_map_violatingBcs_records.get(bcKeyToFix);
            Iterator<TuningAnalysisRecord> fixedIteratorRecordsIter = fixedIteratorRecords.iterator();
            // For all the multiple bcs in path to be fixed, form a map of their iterators
            while (fixedIteratorRecordsIter.hasNext()) {
                TuningAnalysisRecord rec = fixedIteratorRecordsIter.next();
                String pageDef = rec.getPageDefFilePath();
                String pageDefIter = rec.getPageDefIteratorName();
                String pageDefKey = pageDef + "#" + pageDefIter;
                pageDefusages.add(pageDefKey);
                listOfbcRecords.add(rec);
            }
            // If there are any multiple usages using the same iterator but not in the path, form a unified map
            Iterator<TuningAnalysisRecord> allRecordsIter = allRecords.iterator();
            while (allRecordsIter.hasNext()) {
                TuningAnalysisRecord rec = allRecordsIter.next();
                String pageDef = rec.getPageDefFilePath();
                String pageDefIter = rec.getPageDefIteratorName();
                String pageDefKey = pageDef + "#" + pageDefIter;
                if (!pageDefusages.contains(pageDefKey)) {
                    listOfbcRecords.add(rec);
                }
            }
            unifiedBcUsagesMap.put(bcKeyToFix, listOfbcRecords);
        }

        System.out.println("Number of multiple bcUsages to be fixed: " + unifiedBcUsagesMap.size());
        // [END] got the unified map of all multiple bc usages records
        // HashMap<String,String> fetchSizesForMultipleBcs = Rules.calculateVOFetchSizeForMultipleBCUages(unifiedBcUsagesMap);


        // for each bc key - calculate the max of the iterator range size, and set vo fetch size accd
        HashMap<String, TuningAnalysisRecord> bcsFixStatusMap = new HashMap<String, TuningAnalysisRecord>();
        Set<String> unifiedKeySet = unifiedBcUsagesMap.keySet();
        Iterator<String> unifiedKeyIter = unifiedKeySet.iterator();
        while (unifiedKeyIter.hasNext()) {
            String unifiedKey = unifiedKeyIter.next();

            if (isEmpty(unifiedKey)) {
                continue;
            }

            ArrayList<TuningAnalysisRecord> unifiedRecords = unifiedBcUsagesMap.get(unifiedKey);
            if (unifiedRecords == null || unifiedRecords.size() <= 0) {
                continue;
            }
            // To keep track if the multiple bc fetch size was changed atleast once. else no change reqd.
            boolean wasAtleastOneChangeMade  =false;
            
            TuningAnalysisRecord anyoneRecord = unifiedRecords.get(0);
            String voUsageFetchSize = anyoneRecord.getVoUsageFetchSizeInAm();
            String voDefaultFetchSize = anyoneRecord.getVoDefaultFetchSize();

            TuningAnalysisRecord bcRecord = new TuningAnalysisRecord();
            bcRecord.amPath = anyoneRecord.getAmPath();
            bcRecord.amFullPackage = anyoneRecord.getAmFullPackage();
            bcRecord.voInstanceName = anyoneRecord.getVoInstanceName();
            bcRecord.voFullPackage = anyoneRecord.getVoFullPackage();
            bcRecord.voUsageFetchSizeInAm = voUsageFetchSize;
            bcRecord.voDefaultFetchSize = voDefaultFetchSize;
            bcRecord.voPath = anyoneRecord.getVoPath();
            try {
                Integer newVoUsageSize = Rules.calculateVOFetchSizeForMultipleBCUages(unifiedRecords);
                if (newVoUsageSize == null || newVoUsageSize == -37) {
                    throw new Exception("could not obtain new vo usage fetch size");
                }

                bcRecord.setVoUsageFetchSizeNewValue(newVoUsageSize.toString());
                if (!bcRecord.voUsageFetchSizeInAm.equals(bcRecord.voUsageFetchSizeNewValue)) {

                    // before fixing am do defautl vo fixing step

                    boolean defaultVONeedsFix = doesDefaultVONeedFix(bcRecord, bcRecord.voPath, defaultVOFetchSizesFixed);
                    if (defaultVONeedsFix) {
                        if (bDebug) {
                            System.out.println("Default vo needs fix: " + bcRecord.voPath);
                        }
                        // if vo needs fix and fix cannot be made then skip the record - because we cannot be setting the vo usage to > default fetch size. runtime error
                        if (!canFixVO(bcRecord, bcRecord.voPath)) {
                            String msg =
                                "[ERROR] Default VO Needs fix (usageSize < defaultFetchSize).. but vo cannot be fixed because it cannot be obtained";
                            msg = msg + "\n Skipping this record and not fixing am vo usage since it will throw a runtime error";
                            if(bDebug)
                              System.out.println(msg); 
                            errorLogger.log(Level.SEVERE, msg);
                            bcRecord.setFixStatus(TuningAnalysisRecord.STATUS_FIX_MULTIPLEBC_FAILURE);
                            bcRecord.addFixStatus(TuningConstants.FixStatus.STATUS_FIX_MULTIPLEBC_FAILURE);
                            bcRecord.setFixStatusComment(msg);
                            bcsFixStatusMap.put(unifiedKey, bcRecord);
                            continue;

                        } else {
                            // fix vo
                            String voPath = bcRecord.getVoPath();
                            XmlFixer voFixer = new XmlFixer(voPath);

                            boolean voFixMade = fixDefaultFetchSizeInVO(bcRecord, voFixer, voPath);
                            if (voFixMade) {
                                if (bDoADE) {
                                    String vofileContents = voFixer.getFileContentsAsString(false);

                                    applyFix(voPath, vofileContents);
                                }
                                //bcRecord.setFixStatus(TuningAnalysisRecord.STATUS_DEFAULTVO_FIX_SUCCESS);
                                bcRecord.addFixStatus(TuningConstants.FixStatus.DEFAULTVO_FIX_SUCCESS);
                                String newDefaultFetchSize = bcRecord.getVoDefaultFetchSizeNewValue();
                                Integer defaultnewInteger = null;
                                try {
                                    defaultnewInteger = Integer.parseInt(newDefaultFetchSize);
                                    defaultVOFetchSizesFixed.put(voPath, defaultnewInteger);
                                } catch (Exception ex) {
                                    System.out.println("Exception while trying to conver to integer while putting map: " + newDefaultFetchSize);
                                }
                                wasAtleastOneChangeMade=true;
                            } else {

                                // fix was needed but fix was not made.. this is an error because it will throw a runtime exception. do not fix the am usage
                                //bcRecord.setFixStatus(TuningAnalysisRecord.STATUS_DEFAULTVO_FIX_FAILED);
                                bcRecord.addFixStatus(TuningConstants.FixStatus.DEFAULTVO_FIX_FAILED);
                                bcRecord.addFixStatus(TuningConstants.FixStatus.STATUS_FIX_MULTIPLEBC_FAILURE);
                                bcRecord.setFixStatusComment(bcRecord.getFixStatusComment() +
                                                             " \n Default vo fix was needed but it failed.. skipping record.");
                                bcsFixStatusMap.put(unifiedKey, bcRecord);
                                continue;
                            }
                        }
                    }


                    XmlFixer amFixer = new XmlFixer(bcRecord.amPath);
                    boolean fixMade = fixFetchSizeInAm(bcRecord, amFixer);
                    if (fixMade) {
                        if (bDoADE) {
                            String amFileContents = amFixer.getFileContentsAsString(false);

                            applyFix(bcRecord.amPath, amFileContents);
                        }

                        //bcRecord.setFixStatus(bcRecord.getFixStatus() + "," +
                        //                      TuningAnalysisRecord.STATUS_FIX_MULTIPLEBC_SUCCESS);
                        bcRecord.addFixStatus(TuningConstants.FixStatus.STATUS_FIX_MULTIPLEBC_SUCCESS);

                    }
                } else {
                    // already fixed
                    //bcRecord.setFixStatus(bcRecord.getFixStatus() + "," +
                    //                      TuningAnalysisRecord.STATUS_FIX_MULTIPLEBC_SUCCESS);
                    if(wasAtleastOneChangeMade)
                        bcRecord.addFixStatus(TuningConstants.FixStatus.STATUS_FIX_MULTIPLEBC_SUCCESS);
                    else 
                        bcRecord.addFixStatus(TuningConstants.FixStatus.STATUS_FIX_MULTIPLEBC_NOCHANGE);
                }

            } catch (Exception ex) {
                System.out.println(" Exception while fixing bckey: " + unifiedKey);
                //bcRecord.setFixStatus(bcRecord.getFixStatus() + "," +
                //                      TuningAnalysisRecord.STATUS_FIX_MULTIPLEBC_FAILURE);
                bcRecord.addFixStatus(TuningConstants.FixStatus.STATUS_FIX_MULTIPLEBC_FAILURE);
                bcRecord.setFixStatusComment(bcRecord.getFixStatusComment() + " \n Exception: " + ex.getMessage());
            }
            // update all bc records in map with fix status..
            bcsFixStatusMap.put(unifiedKey, bcRecord);
        }


        updateAllMultipleBcRecords(bcsFixStatusMap);
        return createResultMultipleBcMap(bcsFixStatusMap, unifiedBcUsagesMap);
        //return finalFixedBcMap;

    }

    public HashMap<String, ArrayList<TuningAnalysisRecord>> createResultMultipleBcMap(HashMap<String, TuningAnalysisRecord> bcFixedRecords,
                                                                                      HashMap<String, ArrayList<TuningAnalysisRecord>> unifiedBcUsagesMap) {
        HashMap<String, ArrayList<TuningAnalysisRecord>> finalBcRecords = new HashMap<String, ArrayList<TuningAnalysisRecord>>();

        Set<String> reqKeys = onlyreq_multipleBcs_fixedrecords.keySet();
        Iterator<String> reqKeysiter = reqKeys.iterator();
        while (reqKeysiter.hasNext()) {
            String reqBcKey = reqKeysiter.next();
            ArrayList<TuningAnalysisRecord> unifiedRecordsmodList = unifiedBcUsagesMap.get(reqBcKey);
            Iterator<TuningAnalysisRecord> unifiedModIter = unifiedRecordsmodList.iterator();
            TuningAnalysisRecord fixedRecord = bcFixedRecords.get(reqBcKey);
            if (fixedRecord == null)
                continue;
            while (unifiedModIter.hasNext()) {
                TuningAnalysisRecord rec2 = unifiedModIter.next();
                rec2.addFixStatus(fixedRecord.getSetfixStatus());
                rec2.setFixStatusComment(rec2.getFixStatusComment() + " \n " + fixedRecord.getFixStatusComment());
                rec2.setVoUsageFetchSizeNewValue(fixedRecord.getVoUsageFetchSizeNewValue());
                rec2.setVoDefaultFetchSizeNewValue(fixedRecord.getVoDefaultFetchSizeNewValue());
                finalBcRecords = UiFileHelper.addToMap(finalBcRecords, reqBcKey, rec2);
            }
        }

        return finalBcRecords;

    }

    public void updateAllMultipleBcRecords(HashMap<String, TuningAnalysisRecord> multipleBcsFixed) {
        Set<String> keySet = onlyreqiured_analysisRecords.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String uiPath = keyIter.next();
            ArrayList<TuningAnalysisRecord> listRecords = onlyreqiured_analysisRecords.get(uiPath);
            Iterator<TuningAnalysisRecord> listRecordsIter = listRecords.iterator();
            while (listRecordsIter.hasNext()) {
                TuningAnalysisRecord recordToUpdate = listRecordsIter.next();
                HashSet<String> fixStatuses = recordToUpdate.getSetfixStatus();
                if (fixStatuses.contains(TuningConstants.FixStatus.STATUS_MULTIPLE_BC)) {
                    String bcKey = recordToUpdate.getAmFullPackage() + "#" + recordToUpdate.getVoInstanceName();
                    if (multipleBcsFixed.containsKey(bcKey)) {
                        TuningAnalysisRecord bcFixedRecord = multipleBcsFixed.get(bcKey);
                        HashSet<String> bcFixedStatus = bcFixedRecord.getSetfixStatus();
                        fixStatuses.addAll(bcFixedStatus);
                        fixStatuses.remove(TuningConstants.FixStatus.STATUS_MULTIPLE_BC);

                        recordToUpdate.setVoUsageFetchSizeNewValue(bcFixedRecord.getVoUsageFetchSizeNewValue());
                        recordToUpdate.setVoDefaultFetchSizeNewValue(bcFixedRecord.getVoDefaultFetchSizeNewValue());
                    } else {
                        recordToUpdate.setFixStatusComment(recordToUpdate.getFixStatusComment() +
                                                           " \n Could not find bc key in modified map to update.");
                        fixStatuses.add(TuningConstants.FixStatus.STATUS_FIX_MULTIPLEBC_FAILURE);
                    }
                }
                recordToUpdate.setSetfixStatus(fixStatuses);
            }

        }
    }

    //=======================WORKSHEET RELATED FUNCTIONS===================================

    public static void writeToSpreadsheet(HashMap<String, ArrayList<TuningAnalysisRecord>> records, String worksheetName) {

        if (records == null) {
            return;
        }
        int rowNumber = 2;
        intializeSpreadsheet(worksheetName);
        Set<String> setofkeys = records.keySet();
        Iterator<String> keyIter = setofkeys.iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            ArrayList<TuningAnalysisRecord> recordlist = records.get(key);
            Iterator<TuningAnalysisRecord> iterRecord = recordlist.iterator();
            while (iterRecord.hasNext()) {
                TuningAnalysisRecord recordToWrite = iterRecord.next();
                writeComponentInSheetRow(rowNumber, recordToWrite);
                rowNumber++;
            }
        }

    }


    private static void intializeSpreadsheet(String worksheetName) {
        if (m_myBook == null) {
            m_myBook = new Workbook("output");
        }
        // Create a worksheet
        m_analysisSheet = new Worksheet(worksheetName);
        // column 1 - product
        m_analysisSheet.setColumnWidth(1, 10);
        // column 2 - filename
        m_analysisSheet.setColumnWidth(2, 30);
        // Column 3 - file path
        m_analysisSheet.setColumnWidth(3, 40);
        // Column 4 - component Name
        m_analysisSheet.setColumnWidth(4, 20);
        // Column 4 - component Id
        m_analysisSheet.setColumnWidth(5, 20);
        // Column 5 - lov type
        m_analysisSheet.setColumnWidth(6, 10);
        // Column 6 - autoHeightRows attr
        m_analysisSheet.setColumnWidth(7, 10);
        // Column 7 - Iterator range size
        m_analysisSheet.setColumnWidth(8, 15);
        // Column 8 - New Iterator fetch size
        m_analysisSheet.setColumnWidth(9, 15);
        // Column 9 - ListRange size
        m_analysisSheet.setColumnWidth(10, 10);
        // Column 10 - New ListRangeSize
        m_analysisSheet.setColumnWidth(11, 10);
        // Column 11 - vousage fetch size
        m_analysisSheet.setColumnWidth(12, 15);
        // Column 12 - vo usage fetch size new val
        m_analysisSheet.setColumnWidth(13, 15);
        // Column 13 - default fetch size
        m_analysisSheet.setColumnWidth(14, 15);
        // Column 14 - default fetch size new val
        m_analysisSheet.setColumnWidth(15, 15);
        // Column 15 - rules applied
        m_analysisSheet.setColumnWidth(16, 30);
        // Column 16 - fix status
        m_analysisSheet.setColumnWidth(17, 30);
        // Column 16 - fix status comment
        m_analysisSheet.setColumnWidth(18, 30);

        // Column 17 - If this table component is direct child of Panel Stretch Layout
        m_analysisSheet.setColumnWidth(19, 30);

        // Column 18 - PanelStretchAncestry
        m_analysisSheet.setColumnWidth(20, 50);


        // column 1 - product
        m_analysisSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Product")));
        // column 2 - filename
        m_analysisSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("File Name")));
        // Column 3 - file path
        m_analysisSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("File Path")));
        // Column 4 - component Name
        m_analysisSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("Component Name")));
        // Column 5 - component Id
        m_analysisSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Component Id")));
        // Column 5 - autoHeightRows attr
        m_analysisSheet.addCell(new Cell(new CellLocation("F1"), new CellValue("LovType")));
        // Column 6 -  autoHeightRows
        m_analysisSheet.addCell(new Cell(new CellLocation("G1"), new CellValue("AutoHeightRows")));
        // Column 7 - Iterator fetch size
        m_analysisSheet.addCell(new Cell(new CellLocation("H1"), new CellValue("IteratorRangeSize")));
        // Column 8 - New: Iterator fetch size
        m_analysisSheet.addCell(new Cell(new CellLocation("I1"), new CellValue("IteratorRangeSize.NewVal)")));
        // Column 9 - Iterator fetch size
        m_analysisSheet.addCell(new Cell(new CellLocation("J1"), new CellValue("ListRangeSize")));
        // Column 10 - New: Iterator fetch size
        m_analysisSheet.addCell(new Cell(new CellLocation("K1"), new CellValue("ListRangeSize.NewVal)")));
        // Column 11 - vousage fetch size
        m_analysisSheet.addCell(new Cell(new CellLocation("L1"), new CellValue("VOUsageFetchSize")));
        // Column 12 - vo default fetch size
        m_analysisSheet.addCell(new Cell(new CellLocation("M1"), new CellValue("VOusageFetchSize.NewVal")));
        // Column 13 - vnew vo usage fetch sizeo default fetch size
        m_analysisSheet.addCell(new Cell(new CellLocation("N1"), new CellValue("VODefaultFetchSize")));
        // Column 14 - vnew vo usage fetch sizeo default fetch size
        m_analysisSheet.addCell(new Cell(new CellLocation("O1"), new CellValue("VODefaultFetchSize.NewVal")));
        // Column 15 - vnew vo usage fetch sizeo default fetch size
        m_analysisSheet.addCell(new Cell(new CellLocation("P1"), new CellValue("RulesApplied")));
        // Column 16 - vnew vo usage fetch sizeo default fetch size
        m_analysisSheet.addCell(new Cell(new CellLocation("Q1"), new CellValue("FixStatus")));
        // Column 17 - vnew vo usage fetch sizeo default fetch size
        m_analysisSheet.addCell(new Cell(new CellLocation("R1"), new CellValue("fixStatusComment")));
        // Column 18 - If this table component is direct child of Panel Stretch Layout
        m_analysisSheet.addCell(new Cell(new CellLocation("S1"), new CellValue("isTableWrappedByPanelStretchLayout")));

        // Column 19 - Panel Stretch Ancestry
        m_analysisSheet.addCell(new Cell(new CellLocation("T1"), new CellValue("Panel Stretch Ancestry")));

        m_myBook.addWorksheet(m_analysisSheet);
    }


    /**
     *Write spreadsheet to disk
     * @param outputFileName
     * Output file name to write to
     * @throws Exception
     * Exception if operation is unsuccessful
     */
    public static void writeSpreadsheet(String outputFileName) throws Exception {

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFileName);
            XLSXCreator.create(m_myBook, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    /**
     * Write the component details in the spreadsheet row
     * @param rowNumber
     * row number to write to
     * @param component
     * Component details
     */
    public static void writeComponentInSheetRow(int rowNumber, TuningAnalysisRecord record) 
    {
        String productCellNumber = "A" + rowNumber;
        String fileNameCellNumber = "B" + rowNumber;
        String filePathCellNumber = "C" + rowNumber;
        String compNameCellNumber = "D" + rowNumber;
        String compIdCellNumber = "E" + rowNumber;
        String lovTypeCellNumber = "F" + rowNumber;
        String autoHeightRowsCellNumber = "G" + rowNumber;
        String iteratorFetchSizeCellNumber = "H" + rowNumber;
        String New_iteratorFetchSizeCellNumber = "I" + rowNumber;
        String listRangeSizeCellNumber = "J" + rowNumber;
        String New_listRangeSizeCellNumber = "K" + rowNumber;
        String voUsageFetchSizeCellNumber = "L" + rowNumber;
        String New_voUsageFetchSizeCellNumber = "M" + rowNumber;
        String voDefaultFetchSizeCellNumber = "N" + rowNumber;
        String New_voDefaultFetchSizeCellNumber = "O" + rowNumber;
        String rulesAppliedCellNumber = "P" + rowNumber;
        String fixStatusesCellNumber = "Q" + rowNumber;
        String fixCommentCellNumber = "R" + rowNumber;
        String isPanelStretchCellNumber = "S" + rowNumber;
        String panelStretchAncestorCellNumber = "T" + rowNumber;

        m_analysisSheet.addCell(new Cell(new CellLocation(productCellNumber), new CellValue(record.getProduct())));

        m_analysisSheet.addCell(new Cell(new CellLocation(fileNameCellNumber), new CellValue(record.getUifileName())));

        m_analysisSheet.addCell(new Cell(new CellLocation(filePathCellNumber), new CellValue(record.getUifilePath())));


        m_analysisSheet.addCell(new Cell(new CellLocation(compNameCellNumber), new CellValue(record.getComponentName())));

        m_analysisSheet.addCell(new Cell(new CellLocation(compIdCellNumber), new CellValue(record.getComponentId())));


        m_analysisSheet.addCell(new Cell(new CellLocation(autoHeightRowsCellNumber), new CellValue(record.getAutoHeightRows())));

        m_analysisSheet.addCell(new Cell(new CellLocation(listRangeSizeCellNumber),
                                         new CellValue(record.getCustomAttrWithKey(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE))));

        m_analysisSheet.addCell(new Cell(new CellLocation(New_listRangeSizeCellNumber),
                                         new CellValue(record.getCustomAttrWithKey(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE_NEW))));

        m_analysisSheet.addCell(new Cell(new CellLocation(iteratorFetchSizeCellNumber), new CellValue(record.getIteratorRangeSize())));

        m_analysisSheet.addCell(new Cell(new CellLocation(New_iteratorFetchSizeCellNumber), new CellValue(record.getIteratorRangeSizeNewValue())));


        m_analysisSheet.addCell(new Cell(new CellLocation(voUsageFetchSizeCellNumber), new CellValue(record.getVoUsageFetchSizeInAm())));
        m_analysisSheet.addCell(new Cell(new CellLocation(New_voUsageFetchSizeCellNumber), new CellValue(record.getVoUsageFetchSizeNewValue())));

        m_analysisSheet.addCell(new Cell(new CellLocation(voDefaultFetchSizeCellNumber), new CellValue(record.getVoDefaultFetchSize())));
        m_analysisSheet.addCell(new Cell(new CellLocation(New_voDefaultFetchSizeCellNumber), new CellValue(record.getVoDefaultFetchSizeNewValue())));

        HashSet<String> rulesApplied = record.getRulesApplied();
        if (rulesApplied != null) {
            m_analysisSheet.addCell(new Cell(new CellLocation(rulesAppliedCellNumber), new CellValue(rulesApplied.toString())));
        }
        HashSet<String> fixStatuses = record.getSetfixStatus();
        if (rulesApplied != null) {
            m_analysisSheet.addCell(new Cell(new CellLocation(fixStatusesCellNumber), new CellValue(fixStatuses.toString())));
        }


        m_analysisSheet.addCell(new Cell(new CellLocation(fixCommentCellNumber), new CellValue(record.getFixStatusComment())));

        String sFlag = String.valueOf(record.isTableWrappedByPanelStretchLayout());
        m_analysisSheet.addCell(new Cell(new CellLocation(isPanelStretchCellNumber), new CellValue( sFlag )));

        String psa = record.getPanelStretchAncestry();
        m_analysisSheet.addCell(new Cell(new CellLocation(panelStretchAncestorCellNumber), new CellValue( psa )));
     
        File f = new File(record.getUifilePath());
        try{
            
           if(record.getAmFullPackage().equals("oracle.apps.customerCenter.infrastructure360.uiModel.manageContacts.applicationModule.ZcmManageContactsAM") 
              && (record.getVoFullPackage().equals("oracle.apps.customerCenter.infrastructure360.uiModel.manageContacts.view.ZcmUserContactSubVO")
              || record.getVoFullPackage().equals("oracle.apps.customerCenter.infrastructure360.uiModel.manageContacts.view.ZcmAllContactSubVO")) )
               return;
            
            if(record.getVoFullPackage().equals("oracle.apps.projects.foundation.projectDefinition.uiModel.view.DummyTaskDateVO"))
                return;
            
            if(f.getCanonicalPath().contains("fusionapps/crm/components/sales/moo/" +
                "opportunities/ui/public_html/oracle/apps/sales/opptyMgmt/opportunities/ui/page/OpportunitySummary.jsff"))
                return;
            if(record.getVoFullPackage().equals("oracle.apps.crmCommon.activities.activitiesService.view.TemplateTaskVO") &&
                f.getCanonicalPath().contains("fusionapps/crm/components/crmCommon/zmm/activities/publicUi/public_html/" +
                "oracle/apps/crmCommon/activities/publicUi/page/activitiesTaskTemplateListViewPageLevel.jsff"))
                return;
            
            // Zeesha : Bug: 13920652
            if(record.getVoFullPackage().equals("oracle.apps.atk.essMeta.uiModel.view.AtkEssUserPropertyVO")) 
                return;
            if(record.getVoFullPackage().equals("oracle.apps.atk.essMeta.uiModel.view.AtkEssDependentLOVVO")) 
                return;
            if(record.getVoFullPackage().equals("oracle.apps.atk.essMeta.uiModel.view.AtkEssParameterVO")) 
                return;
            
            String fixStatuses1 = getFixStatuses(fixStatuses);
            if(!fixStatuses1.replace("MULTIPLE_BC_SUCCESS","").contains("SUCCESS"))
                return;
            if(sFlag.equals("false"))
                return;
            
            //Only report violations in csv
            if(!exemptions.contains("TableFetchSizePSL," + ViewCriteriaHelper.getFileName(f.getAbsolutePath()) + "," + record.getComponentId()))
                csvWriter.write(FamilyModuleHelper.getFileNameInfo1(f.getCanonicalPath()) + record.getComponentName() + "," + record.getComponentId() 
                           + "," + record.getAutoHeightRows() + "," + record.getIteratorRangeSize() 
                           + "," + record.getIteratorRangeSizeNewValue() + "," + record.getPageDefIteratorName() 
                           + "," + record.getCustomAttrWithKey(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE) 
                           + "," + record.getCustomAttrWithKey(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE_NEW) 
                           + "," + record.getVoUsageFetchSizeInAm() + "," + record.getVoUsageFetchSizeNewValue() 
                           + "," + record.getVoDefaultFetchSize() + "," + record.getVoDefaultFetchSizeNewValue()
                           + "," + record.getAmFullPackage() + "," + record.getVoFullPackage() + "," + record.getVoInstanceName()
                           + "," + fixStatuses1 + "," + sFlag + "," + psa + "\n");
        } catch(IOException ex){
           ex.printStackTrace();
        }
    }


    //=======================[END] WORKSHEET RELATED FUNCTIONS===================================
    
    
    private static String getFixStatuses(HashSet<String> fixStatuses) {
      String result = "";
      for(Iterator it = fixStatuses.iterator(); it.hasNext();)
          result += " " + (String)it.next();
      
      return result;
    }
    
    public HashMap<String, ArrayList<TuningAnalysisRecord>> removeTransientVOsFromList(HashMap<String, ArrayList<TuningAnalysisRecord>> records)
    {
        return null; //TODO:
    }
    
    public void writeSer()
    {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        
        try
        {
            fos = new FileOutputStream(getSerPath());
            out = new ObjectOutputStream(fos);
            out.writeObject(this);
            out.close();
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public boolean hasSer()
    {
        File f = new File(getSerPath());
        return f.exists();
    }

    
    public static RunLOVAnalysis readSer()
    {
        RunLOVAnalysis x = null;
        
        FileInputStream fis = null;
        ObjectInputStream in = null;
        try
        {
            fis = new FileInputStream(getSerPath());
            in = new ObjectInputStream(fis);
            x = (RunLOVAnalysis)in.readObject();
            in.close();
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
        catch(ClassNotFoundException ex)
        {
            ex.printStackTrace();
        }

        return x;           
    }
    
    public static String getSerPath()
    {
        return "/tmp/" + RunLOVAnalysis.class.getName() +".ser";
    }

    public void generateExemptionList() {
         
     try{
       BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
             "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_new.txt"));
       String line = null;
       
       while((line = fileReader.readLine()) != null) {
         String[] parts = line.split(",");
         if(parts.length < 3)
             continue;
         String issue = parts[0].trim();
         if(!issue.equals("TableFetchSizePSL"))
             continue;
         exemptions.add(issue + "," + parts[1].trim() + "," + parts[2].trim());
           //jsff file, VO file, description
       }
        fileReader.close();       
     }catch(Exception e) {
         e.printStackTrace();
     }    
    }
}
