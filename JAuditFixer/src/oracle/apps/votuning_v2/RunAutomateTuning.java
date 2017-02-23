package oracle.apps.votuning_v2;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileOutputStream;

import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import java.util.logging.ConsoleHandler;
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

import oracle.apps.bc4jAnalyzer;
import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;


import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Cases not taken care of
 * 1. If one pagedef is used by more than one page
 * Reason: based on analysis and code scan of entire financials code we do not have this case where one page def is used by more than one page.
 * If this case was to be handled in the future - then for each of those pages that use the same page def, pagedef.iteratorRangeSize = max of autoheights on pages
 *
 *
 * 2. Case where one iterator on a pagedef is used in multiple tree components
 * Reason: based on analysis and code scan of entire financials code we do not have this case where one page def is used by more than one page.
 * If this case was to be handled in the future - then for each of those pages that use same iterator pagedef, pagedef.iteratorRangeSize = max of autoheights on pages
 */
public class RunAutomateTuning {
    public RunAutomateTuning() {
        super();
    }

    private static boolean writeAsXml = true;
    private static boolean bDebug = true;
    HashMap<String, Integer> defaultVOFetchSizesFixed =
        new HashMap<String, Integer>();

    /**
     * Map of all the binding components (AM - VO) used by each record
     */
    HashMap<String, ArrayList<TuningAnalysisRecord>> m_map_bcs_records =
        new HashMap<String, ArrayList<TuningAnalysisRecord>>();


    /**
     * Map of only those binding components (AM-VO) that are used by multiple records)
     */
    HashMap<String, ArrayList<TuningAnalysisRecord>> m_map_violatingBcs_records =
        new HashMap<String, ArrayList<TuningAnalysisRecord>>();


    /**
     * Map of the changed record for the violating bc.
     * If a BC is used by  more than two components whose autoheight and iterator range size is different then the bc fetch size
     * will be configured as the max of the iteratorRangeSize
     */
    HashMap<String, TuningAnalysisRecord> m_map_violatingBcs_changedRecord =
        new HashMap<String, TuningAnalysisRecord>();



    /**
     * Map of multiple page def usages.
     * Used to identify if one page def is being used by more than one page.
     * Indexed by the page def location
     * Result - so far in financials - we do not have this case that there is one page def being used by more than one page.
     * Hence this script does not take care of that corner case
     */
    // HashMap<String, ArrayList<String>> m_multiplepagedefUsages =
    //    new HashMap<String, ArrayList<String>>();

    /**
     * indexed by pagedef#iteratorName
     * This helps identify if an iterator on one page is being used by  multiple components.
     * Based on analysis, so far in fianncials - we do not have this case that there is one pagedef iterator being used across multiple tree components
     */
    HashMap<String, ArrayList<TuningAnalysisRecord>> m_mapOfMultipleIteratorUsages =
        new HashMap<String, ArrayList<TuningAnalysisRecord>>();


    /**
     * Map of all the anlaysis records for all of financials
     */
    private static HashMap<String, ArrayList<TuningAnalysisRecord>> allfin_analysisRecords =
        new HashMap<String, ArrayList<TuningAnalysisRecord>>();

    /**
     * Map of all the anlaysis records for all of financials
     */
    private static HashMap<String, ArrayList<TuningAnalysisRecord>> onlyreqiured_analysisRecords =
        new HashMap<String, ArrayList<TuningAnalysisRecord>>();

    /**
     * List of error records..
     */
    private static HashMap<String, ArrayList<TuningAnalysisRecord>> allfin_errorRecords =
        new HashMap<String, ArrayList<TuningAnalysisRecord>>();


    private static HashMap<String, ArrayList<TuningAnalysisRecord>> onlyrequired_errorRecords =
        new HashMap<String, ArrayList<TuningAnalysisRecord>>();

    HashMap<String, String> map_multipleBcs_fetchSizes =
        new HashMap<String, String>();

    /**
     * Map of all the anlaysis records for all of financials
     */
    private static HashMap<String, ArrayList<TuningAnalysisRecord>> onlyreq_multipleBcs_fixedrecords =
        new HashMap<String, ArrayList<TuningAnalysisRecord>>();

    /**
     * Map of all the anlaysis records for all of financials
     */
    private static HashMap<String, ArrayList<TuningAnalysisRecord>> multipleBcsFinalMap =
        new HashMap<String, ArrayList<TuningAnalysisRecord>>();

    /**
     * Excel workbook to write to
     */
    static Workbook m_myBook;

    /**
     * Worksheet to write all the violations to
     */
    static Worksheet m_violationsSheet;

    /**
     * worksheet to write all the multiple bc usages to
     */
    static Worksheet m_multipleBcSheet;

    /**
     * Number of jsff files processed
     */
    static int m_jsffCount = 0;

    /**
     * number of jspx files processed
     */
    static int m_jspxCount = 0;

    static int m_pageDefsFixed = 0;
    static int m_amsFixed = 0;

    /**
     * Name of the log file for errors
     */
    static String ERROR_LOG_FILENAME = "error.log";

    static String MULTIPLEBC_SHEET_NAME = "Multple BC Usages";
    static String ANALAYSIS_OUTPUT_SHEET_NAME = "Analysis Output";
    static String m_diffFileName = "diff.txt";
    AdeDiffHelper diffHelper = new AdeDiffHelper(m_diffFileName);

    /**
     * Map of all the databindings indexed by the adfmsrc directory.
     * This is used to speed up lookup time. Instead of trying to find, parse the cpx file for each ui file, this  map holds the cpx object indexed in the map by the adfmsrc directory.
     * if there is an adfmsrc directory as the key in this map, the bindings object is retrieved from here. Else for the ui file, attempt to find the cpx file in thea dfmsrc directory. once found it is put in the map.
     */
    private static HashMap<String, DataBindings> bindingsMap =
        new HashMap<String, DataBindings>();

    private static Logger errorLogger = null;

    /* toggle these values to keep recording multiple pagedef and iterator usages.. will typically be
    used while debugging.. waste of memory otherwise */
    private static boolean recordMultipleIteratorUsages = true;


    public static void main(String[] args) {
        // Make sure it is run with correct number of arguments
        // Usage:  RunAutomatedTuningFixer.sh <path to your code in an open ade view> <fix violations in path> <Do ADE checkin/checkout>
        if (args.length != 3 || args[0] == null || args[0].equals("") ||
            args[1] == null || args[1].equals("") || args[2] == null ||
            args[2].equals("")) {
            System.out.println("Usage:  RunAutomatedTuningFixer.sh <path to your code in an open ade view> <fixviolations(true/false)> <Do ADE checkin/checkout>");
            System.exit(1);
        }

        Date startDate = new Date();

        // get arguments
        String sCrawlDir = args[0];
        Boolean bfixViolations = new Boolean(args[1]);
        Boolean bDoADE = new Boolean(args[2]);

        if (isEmpty(sCrawlDir)) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }

        System.out.println("Final output will be an Analyses of all files in directory:" +
                           sCrawlDir + "\n");
        errorLogger = initLogger();
        RunAutomateTuning runAutomatedTuningFixer = new RunAutomateTuning();

        /* At the output of pass 1 we have:
         * 1. For all of financials - all the violtions in: allfin_analysisRecords
         * 2. Multiple bc uages in - m_map_violatingBcs_records */
        try {
            runAutomatedTuningFixer.runPass1(sCrawlDir);
        } catch (Exception ex) {
            errorLogger.log(Level.SEVERE,
                            "Exception occurred in pass 1: " + ex.getMessage());
            System.out.println("Exception occurred in pass 1: " +
                               ex.getMessage());
            System.exit(1);
        }

       
        // Debugging for pass 1
        /* try{
            writeToSpreadsheet(allfin_analysisRecords);
            writeSpreadsheet("output.xlsx");
        }catch(Exception ex)
        {
            System.out.println("Exception occurred: " + ex.getMessage());
            ex.printStackTrace();
            }*/


        if (bfixViolations) {
            try {
                runAutomatedTuningFixer.scanAndMakeFixes(sCrawlDir, bDoADE);
            } catch (Exception ex) {
                System.out.println("Exception occurred: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            // crawl directory just to filter all the analysis results
            runAutomatedTuningFixer.crawlDirToFilterAnalysisOnly(sCrawlDir);
        }

        // Write results: analysis, error records and multiple bc usages
        try {
            if (writeAsXml) {
                runAutomatedTuningFixer.writeOutputXml(onlyreqiured_analysisRecords,
                                                       multipleBcsFinalMap,
                                                       onlyrequired_errorRecords,
                                                       "results.xml");

            } else {
                writeToSpreadsheet(onlyreqiured_analysisRecords,
                                   ANALAYSIS_OUTPUT_SHEET_NAME);
                // Write all the bc violations to the spreadsheet
                writeBCViolationsToSpreadsheet(runAutomatedTuningFixer.m_map_violatingBcs_records);
                writeToSpreadsheet(onlyrequired_errorRecords, "Error Records");
                writeSpreadsheet("output_required.xlsx");
            }
        } catch (Exception ex) {
            System.out.println("Exception occurred while writing spreadsheet: " +
                               ex.getMessage());
            ex.printStackTrace();
        }
        runAutomatedTuningFixer.printStatistics(startDate);
    }

    public static Integer countTotalRecords(HashMap<String, ArrayList<TuningAnalysisRecord>> mapRecordsToCount) {
        Integer cnt = 0;
        Set<String> setofkeys = mapRecordsToCount.keySet();
        Iterator<String> keyIter = setofkeys.iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            ArrayList<TuningAnalysisRecord> recordlist =
                mapRecordsToCount.get(key);
            cnt = cnt + recordlist.size();
        }
        return cnt;
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

        String pass1AnalysisPath = getPass1AnalysisPath(sCrawlDir);
        System.out.println(String.format("============Pass 1 : Analyse all files in directory %s (to find multiple bc usages) ============",
                                         pass1AnalysisPath));
        if (pass1AnalysisPath == null) {
            System.out.println("Could not get the pass 1 analysis path from analysis directory: " +
                               sCrawlDir);
            System.exit(1);
        }

        /* for each valid input jspx/jsff file, get a list of all records. Indexed by ui file path.
         * Records updated in static member variable: m_map_bcs_records
         * Records contain every ui file in fusionapps/fin
         * */
        crawlDirectoryForPass1(pass1AnalysisPath);

        if (m_map_bcs_records != null && m_map_bcs_records.size() > 0) {
            m_map_violatingBcs_records =
                    UiFileHelper.getOnlyMultipleUsages(m_map_bcs_records);
        }
        System.out.println(String.format("Number of files analyzed in pass 1: (jsff): %s, (jspx):%s",
                                         m_jsffCount, m_jspxCount));
        if (m_map_violatingBcs_records != null &&
            m_map_violatingBcs_records.size() > 0) {
            System.out.println(String.format("Number of multiple bcs found in pass 2: %s",
                                             m_map_violatingBcs_records.size()));
        }
        System.out.println("-----------------end of pass 1------------------------------------");
        // reset counters
        m_jsffCount = 0;
        m_jspxCount = 0;
    }


    public void scanAndMakeFixes(String sCrawlDir,
                                 boolean bDoADE) throws Exception {
        System.out.println(String.format("============== Pass 2 (Scanning and making fixes: directory: %s =========== ",
                                         sCrawlDir));

        if (allfin_analysisRecords == null) {
            return;
        }
        // calculate for all the multiple page def iterator, the new iterator range size
        HashMap<String, TuningAnalysisRecord> multipleIteratorFetchSizes =
            new HashMap<String, TuningAnalysisRecord>();
        multipleIteratorFetchSizes =
                Rules.calculateRangeSizeForMultipleIteratorUsages(m_mapOfMultipleIteratorUsages);

        // Remove any existing diff.txt. Create a new bufferred writer for diff.txt
        if (bDoADE) {
            diffHelper.startDiffLog();
        }

        crawlDirForFixes(sCrawlDir, bDoADE, multipleIteratorFetchSizes);
        multipleBcsFinalMap = fixMultipleBcUsages(bDoADE);

        if (bDoADE) {
            diffHelper.closeDiffLog();
        }

    }

    public HashMap<String, ArrayList<TuningAnalysisRecord>> fixMultipleBcUsages(boolean bDoADE) throws Exception {
        System.out.println("--------------Fixing multiple bc iterator Usages------------------");
        // onlyreq_multipleBcs_fixedrecords contains all the multipleBCKeys to be fixed as their index
        // also contains the fixed iterators sizes records against them.
        Set<String> keySet = onlyreq_multipleBcs_fixedrecords.keySet();
        Iterator<String> keyIter = keySet.iterator();
        HashSet<String> pageDefusages = new HashSet<String>();
        HashMap<String, ArrayList<TuningAnalysisRecord>> unifiedBcUsagesMap =
            new HashMap<String, ArrayList<TuningAnalysisRecord>>();
        HashMap<String, ArrayList<TuningAnalysisRecord>> finalFixedBcMap =
            new HashMap<String, ArrayList<TuningAnalysisRecord>>();

        while (keyIter.hasNext()) {
            String bcKeyToFix = keyIter.next();
            ArrayList<TuningAnalysisRecord> listOfbcRecords =
                new ArrayList<TuningAnalysisRecord>();
            ArrayList<TuningAnalysisRecord> fixedIteratorRecords =onlyreq_multipleBcs_fixedrecords.get(bcKeyToFix);
            ArrayList<TuningAnalysisRecord> allRecords =m_map_violatingBcs_records.get(bcKeyToFix);
            Iterator<TuningAnalysisRecord> fixedIteratorRecordsIter =
                fixedIteratorRecords.iterator();
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
            Iterator<TuningAnalysisRecord> allRecordsIter =
                allRecords.iterator();
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

        System.out.println("Number of multiple bcUsages to be fixed: " +
                           unifiedBcUsagesMap.size());
        // [END] got the unified map of all multiple bc usages records
        // HashMap<String,String> fetchSizesForMultipleBcs = Rules.calculateVOFetchSizeForMultipleBCUages(unifiedBcUsagesMap);


        // for each bc key - calculate the max of the iterator range size, and set vo fetch size accd
        HashMap<String, TuningAnalysisRecord> bcsFixStatusMap =new HashMap<String, TuningAnalysisRecord>();
        Set<String> unifiedKeySet = unifiedBcUsagesMap.keySet();
        Iterator<String> unifiedKeyIter = unifiedKeySet.iterator();
        while (unifiedKeyIter.hasNext()) {
            String unifiedKey = unifiedKeyIter.next();

            if (isEmpty(unifiedKey)) {
                continue;
            }

            ArrayList<TuningAnalysisRecord> unifiedRecords =
                unifiedBcUsagesMap.get(unifiedKey);
            if (unifiedRecords == null || unifiedRecords.size() <= 0) {
                continue;
            }
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
            boolean defaultFetchSizeFixNeededFailed = false;
            try {
                Integer newVoUsageSize =
                    Rules.calculateVOFetchSizeForMultipleBCUages(unifiedRecords);
                if (newVoUsageSize == null || newVoUsageSize == -37) {
                    throw new Exception("could not obtain new vo usage fetch size");
                }

                bcRecord.setVoUsageFetchSizeNewValue(newVoUsageSize.toString());
                if (!bcRecord.voUsageFetchSizeInAm.equals(bcRecord.voUsageFetchSizeNewValue)) {

                    // before fixing am do defautl vo fixing step
                    //TODO: fix default vo usage
                    boolean defaultVONeedsFix =
                        doesDefaultVONeedFix(bcRecord, defaultVOFetchSizesFixed);
                    if (defaultVONeedsFix) {
                        if (bDebug) {
                            System.out.println("Default vo needs fix");
                        }
                        // if vo needs fix and fix cannot be made then skip the record - because we cannot be setting the vo usage to > default fetch size. runtime error
                        if (!canFixVO(bcRecord)) {
                            String msg =
                                "[ERROR] Default VO Needs fix (usageSize < defaultFetchSize).. but vo cannot be fixed because it cannot be obtained";
                            msg =
 msg + "\n Skipping this record and not fixing am vo usage since it will throw a runtime error";
                            System.out.println(msg);
                            errorLogger.log(Level.SEVERE, msg);
                            bcRecord.setFixStatus(TuningAnalysisRecord.STATUS_FIX_MULTIPLEBC_FAILURE);
                            bcRecord.setFixStatusComment(msg);
                            bcsFixStatusMap.put(unifiedKey, bcRecord);
                            continue;

                        } else {
                            // fix vo
                            String voPath = bcRecord.getVoPath();
                            XmlFixer voFixer = new XmlFixer(voPath);

                            boolean voFixMade =
                                fixDefaultFetchSizeInVO(bcRecord, voFixer);
                            if (voFixMade) {
                                if (bDoADE) {
                                    String vofileContents =
                                        voFixer.getFileContentsAsString(false);

                                    applyFix(voPath, vofileContents);
                                }
                                bcRecord.setFixStatus(TuningAnalysisRecord.STATUS_DEFAULTVO_FIX_SUCCESS);
                                String newDefaultFetchSize =
                                    bcRecord.getVoDefaultFetchSizeNewValue();
                                Integer defaultnewInteger = null;
                                try {
                                    defaultnewInteger =
                                            Integer.parseInt(newDefaultFetchSize);
                                    defaultVOFetchSizesFixed.put(voPath,
                                                                 defaultnewInteger);
                                } catch (Exception ex) {
                                    System.out.println("Exception while trying to conver to integer while putting map: " +
                                                       newDefaultFetchSize);
                                }
                            } else {

                                // fix was needed but fix was not made.. this is an error because it will throw a runtime exception. do not fix the am usage
                                bcRecord.setFixStatus(TuningAnalysisRecord.STATUS_DEFAULTVO_FIX_FAILED);
                                bcRecord.setFixStatusComment("Default vo fix was needed but it failed.. skipping record.");
                                bcsFixStatusMap.put(unifiedKey, bcRecord);
                                continue;
                            }
                        }
                    }


                    XmlFixer amFixer = new XmlFixer(bcRecord.amPath);
                    boolean fixMade = fixFetchSizeInAm(bcRecord, amFixer);
                    if (fixMade) {
                        if (bDoADE) {
                            String amFileContents =
                                amFixer.getFileContentsAsString(false);

                            applyFix(bcRecord.amPath, amFileContents);
                        }

                        bcRecord.setFixStatus(bcRecord.getFixStatus() + "," +
                                              TuningAnalysisRecord.STATUS_FIX_MULTIPLEBC_SUCCESS);

                    }
                } else {
                    // already fixed
                    bcRecord.setFixStatus(bcRecord.getFixStatus() + "," +
                                          TuningAnalysisRecord.STATUS_FIX_MULTIPLEBC_SUCCESS);
                }

            } catch (Exception ex) {
                System.out.println(" Exception while fixing bckey: " +
                                   unifiedKey);
                bcRecord.setFixStatus(bcRecord.getFixStatus() + "," +
                                      TuningAnalysisRecord.STATUS_FIX_MULTIPLEBC_FAILURE);
                bcRecord.setFixStatusComment("Exception: " + ex.getMessage());
            }
            // update all bc records in map with fix status..
            bcsFixStatusMap.put(unifiedKey, bcRecord);


        }


        // update whatever records are required
        Set<String> reqKeys = onlyreq_multipleBcs_fixedrecords.keySet();
        Iterator<String> reqKeysiter = reqKeys.iterator();
        while (reqKeysiter.hasNext()) {

            String reqBcKey = reqKeysiter.next();
            TuningAnalysisRecord recordObt = bcsFixStatusMap.get(reqBcKey);
            String bcRecordFixStatus = recordObt.getFixStatus();
            String bcRecordFixComment = recordObt.getFixStatusComment();

            /*String reqValue = bcsFixStatusMap.get(reqBcKey);
            String bcRecordFixStatus = "";
            String newVoFetchSize = "";
            if (!isEmpty(reqValue)) {
                String[] valueParts = reqValue.split("#", 2);
                if (valueParts != null && valueParts.length > 1) {
                    bcRecordFixStatus = valueParts[0];
                    newVoFetchSize = valueParts[1];
                }
            }*/

            ArrayList<TuningAnalysisRecord> listupdateRecords =
                onlyreq_multipleBcs_fixedrecords.get(reqBcKey);
            Iterator<TuningAnalysisRecord> updateRecordsIter =
                listupdateRecords.iterator();
            while (updateRecordsIter.hasNext()) {

                TuningAnalysisRecord rec1 = updateRecordsIter.next();

                String origFixStatus = rec1.getFixStatus();
                String origFixComment = rec1.getFixStatusComment();
                if (!origFixStatus.contains(bcRecordFixStatus)) {
                    rec1.setFixStatus(origFixStatus + "," + bcRecordFixStatus);
                }
                if (!origFixComment.contains(bcRecordFixComment)) {
                    rec1.setFixStatusComment(origFixComment + " \n " +
                                             bcRecordFixComment);
                }

                /* if (origFixStatus.contains(TuningAnalysisRecord.STATUS_MULTIPLE_BC) &&
                    !origFixStatus.contains(bcRecordFixStatus)) {
                    if (origFixStatus.contains(TuningAnalysisRecord.STATUS_FIX_ITERATOR_SUCCESS)) {
                        rec1.setFixStatus(TuningAnalysisRecord.STATUS_FIX_ITERATOR_SUCCESS +
                                          "," + bcRecordFixStatus);
                    } else if (origFixStatus.contains(TuningAnalysisRecord.STATUS_FIX_MULTITERATOR_SUCCESS)) {
                        rec1.setFixStatus(TuningAnalysisRecord.STATUS_FIX_MULTITERATOR_SUCCESS +
                                          "," + bcRecordFixStatus);
                    } else {
                        rec1.setFixStatus(origFixStatus + "," +
                                          bcRecordFixStatus);
                    }
                } else if (!origFixStatus.contains(bcRecordFixStatus)) {

                    rec1.setFixStatus(rec1.getFixStatus() + "," +
                                      bcRecordFixStatus);
                }*/

                rec1.setVoUsageFetchSizeNewValue(recordObt.getVoUsageFetchSizeNewValue());
                rec1.setVoDefaultFetchSizeNewValue(recordObt.getVoDefaultFetchSizeNewValue());

                onlyreqiured_analysisRecords =
                        UiFileHelper.addToMap(onlyreqiured_analysisRecords,
                                              rec1.getUifilePath(), rec1);
            }

            ArrayList<TuningAnalysisRecord> unifiedRecordsmodList = unifiedBcUsagesMap.get(reqBcKey);
            Iterator<TuningAnalysisRecord> unifiedModIter = unifiedRecordsmodList.iterator();
            while (unifiedModIter.hasNext()) {
                TuningAnalysisRecord rec2 = unifiedModIter.next();
                String origFixStatus = rec2.getFixStatus();

                String origFixComment = rec2.getFixStatusComment();
                if (!origFixStatus.contains(bcRecordFixStatus)) {
                    rec2.setFixStatus(origFixStatus + "," + bcRecordFixStatus);
                }
                if (!origFixComment.contains(bcRecordFixComment)) {
                    rec2.setFixStatusComment(origFixComment + " \n " +
                                             bcRecordFixComment);
                }


                rec2.setVoUsageFetchSizeNewValue(recordObt.getVoUsageFetchSizeNewValue());
                rec2.setVoDefaultFetchSizeNewValue(recordObt.getVoDefaultFetchSizeNewValue());

                finalFixedBcMap =
                        UiFileHelper.addToMap(finalFixedBcMap, reqBcKey, rec2);
            }
        }

        return finalFixedBcMap;

    }

    /**
     *return true/false if any fix was made
     * @param record
     * @param pagedefFixer
     * @return
     * @throws Exception
     */
    private boolean fixIteratorRangeSizeInPageDef(TuningAnalysisRecord record,
                                                  XmlFixer pagedefFixer) throws Exception {

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
        if (!isEmpty(newIteratorFetchSize) &&
            !newIteratorFetchSize.trim().equals("-37") &&
            newIteratorFetchSize.compareTo(oldIteratorFetchSize) != 0) {
            try {
                iteratorFetchSizeInt = Integer.parseInt(newIteratorFetchSize);
                if (iteratorFetchSizeInt == -37) {
                    return false;
                }
                validNewFetchSize = true;
            } catch (NumberFormatException nex) {
                record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
                record.setFixStatusComment("Cannot convert iterator fetch size to integer: " +
                                           newIteratorFetchSize);
                System.out.println("could not convert iterator fetch size to integer: " +
                                   newIteratorFetchSize);
                throw new Exception(nex);
            }


        }else if(!isEmpty(newIteratorFetchSize) &&
            !newIteratorFetchSize.trim().equals("-37")&&newIteratorFetchSize.compareTo(oldIteratorFetchSize) == 0){
            // treated as a fixed record
            record.setFixStatus(TuningAnalysisRecord.STATUS_FIX_ITERATOR_NOCHANGE);
            return false;
        } else {

            record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
            record.setFixStatusComment("newIteratorRangeSize is empty or -37");
            return false;
        }
        if (!validNewFetchSize) {
            return false;
        }
        String pageDefIteratorName = record.getPageDefIteratorName();
        XMLDocument pageDefXml = XMLParserHelper.getXMLDocument(pageDefPath);
        NodeList executableNodes =
            pageDefXml.getElementsByTagName("executables");
        if (!XMLParserHelper.hasOneNOnlyOneNode(executableNodes)) {
            System.out.println("[ERROR] Assumption: There should be one and only one executables node in the page def.." +
                               pageDefPath);
            throw new Exception("Could not fix page def because there should be only one executables node in page def");

        } else {
            Node executableNode = executableNodes.item(0);
            NodeList executableChildren = executableNode.getChildNodes();
            if (executableChildren != null &&
                executableChildren.getLength() > 0) {
                int childLen = executableChildren.getLength();
                for (int i = 0; i < childLen; i++) {
                    Node executableChild = executableChildren.item(i);
                    String nodeName = executableChild.getNodeName();
                    if (!isEmpty(nodeName) &&
                        nodeName.trim().toLowerCase().endsWith("iterator")) {


                        String id =
                            XMLParserHelper.getAttributeValue(executableChild,
                                                              "id");
                        if (!isEmpty(id) &&
                            id.trim().equals(pageDefIteratorName)) {
                            String rangeSize =
                                XMLParserHelper.getAttributeValue(executableChild,
                                                                  "RangeSize");
                            if (rangeSize != null && !isEmpty(rangeSize)) {
                                // modify this attribute..
                                pagedefFixer.modifyAttribute(executableChild,
                                                             "RangeSize",
                                                             rangeSize,
                                                             iteratorFetchSizeInt.toString(),
                                                             false);
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
    private boolean fixFetchSizeInAm(TuningAnalysisRecord record,
                                     XmlFixer amFixer) throws Exception {

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
        if (!isEmpty(voNewUsageFetchSize) &&
            !voNewUsageFetchSize.trim().equals("-37")) {
            try {
                voNewUsageFetchSizeInt = Integer.parseInt(voNewUsageFetchSize);
                if (voNewUsageFetchSizeInt == -37) {
                    return false;
                }
                validNewFetchSize = true;
            } catch (NumberFormatException nex) {
                System.out.println("could not convert iterator fetch size to integer: " +
                                   voNewUsageFetchSize);
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
        Node viewUsageNode =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(amNode,
                                                                     "ViewUsage",
                                                                     "Name",
                                                                     voInstanceName);
        if (viewUsageNode == null) {
            throw new Exception("Could not find view usage tag");
        }

        try {
            amFixer.modifyAttribute(viewUsageNode, "FetchSize",
                                    vousageFetchSize,
                                    voNewUsageFetchSizeInt.toString(), true);
            return true;
        } catch (Exception ex) {
            throw new Exception(ex);
        }

    }

    /**
     *return true/false if any fix was made
     * @param record
     * @param pagedefFixer
     * @return
     * @throws Exception
     */
    private boolean fixDefaultFetchSizeInVO(TuningAnalysisRecord record,
                                            XmlFixer amFixer) throws Exception {
        if (bDebug) {
            System.out.println("Fixing Default Fetch Size in VO");
        }
        if (record == null) {
            return false;
        }
        String voPath = record.getVoPath();
        if (isEmpty(voPath)) {
            return false;
        }
        File voFile = new File(voPath);
        if (!voFile.exists()) {
            throw new Exception("VO does not exist: " + voPath);
        }
        boolean validNewFetchSize = true;
        String defaultFetchSizeNewValue =
            record.getVoDefaultFetchSizeNewValue();
        if (isEmpty(defaultFetchSizeNewValue)) {
            if (bDebug) {
                System.out.println("Defaut fetch size new value is null.. nothing to fix");
            }
            return false;
        }
        //DONT DO VALIDATIONS HERE.. ASSUME ITS DONE N WE KNOW WHAT WE ARE DOING
        // validations to make sure we are setting the correct fetch size
        /* try {
            Integer newFetchSizeInt =
                Integer.parseInt(defaultFetchSizeNewValue);
            if (newFetchSizeInt == null || newFetchSizeInt == -37) {
                System.out.println("New fetch size was not set");
                return false;
            }
            String oldDefaultFetchSize = record.getVoDefaultFetchSize();
            if (isEmpty(oldDefaultFetchSize)) {
                System.out.println("Old fetch size was null.. cannot fix default fetch size now.. ");
                return false;
            }
            Integer oldDefaultFetchInteger =
                Integer.parseInt(oldDefaultFetchSize);
            if (oldDefaultFetchSize == null) {
                return false;
            }
            if (newFetchSizeInt > oldDefaultFetchInteger) {
                System.out.println("setting a new fetch size greater than the old one is not allowed");
                return false;
            }
            validNewFetchSize = true;
        } catch (NumberFormatException nex) {
            System.out.println("could not convert fetch size to integer: ");
            return false;
        }*/


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
            String defaultFetchSize = record.getVoDefaultFetchSize();

            if (isEmpty(defaultFetchSize) ||
                defaultFetchSize.trim().equalsIgnoreCase("not set")) {
                System.out.println("Default fetch size was empty.. not fixing default fetch size");
                return false;
            } else {
                return amFixer.modifyAttributeIrrespectiveOldValue(voNode,
                                                                   "FetchSize",
                                                                   defaultFetchSizeNewValue);
            }

        } catch (Exception ex) {
            String msgex =
                "Exception occurred while trying to fix default fetch size: " +
                ex.getMessage();
            System.out.println(msgex);
            errorLogger.log(Level.SEVERE, msgex);
            throw new Exception(ex);
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
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
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
     *
     * 1. Keep track of all the error records in this crawl director
     * 2. all the analyzed records (irrespective if they are fixed or not) in the crawl directory
     * @param path
     * @param bDoADE
     */
    private void crawlDirForFixes(String path, boolean bDoADE,
                                  HashMap<String, TuningAnalysisRecord> multipleIteratorFetchSizes) {
        if (path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();

                if (isValidUIFile(absFilePath)) {
                    if (absFilePath.trim().endsWith(".jsff")) {
                        m_jsffCount++;
                    } else if (absFilePath.trim().endsWith(".jspx")) {
                        m_jspxCount++;
                    }
                    if (allfin_errorRecords.containsKey(absFilePath)) {
                        ArrayList<TuningAnalysisRecord> list_errorrecords =
                            allfin_errorRecords.get(absFilePath);
                        onlyrequired_errorRecords.put(absFilePath,
                                                      list_errorrecords);
                    }
                    if (allfin_analysisRecords.containsKey(absFilePath)) {
                        try {
                            makeFix(absFilePath, bDoADE,
                                    multipleIteratorFetchSizes);
                        } catch (Exception ex) {
                            String msg =
                                String.format("Skipping file: %s.. Error while trying to make fix for file. Exception: %s",
                                              absFilePath, ex.getMessage());
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
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (isValidUIFile(absFilePath)) {
                    if (allfin_errorRecords.containsKey(absFilePath)) {
                        ArrayList<TuningAnalysisRecord> list_errorrecords =
                            allfin_errorRecords.get(absFilePath);
                        onlyrequired_errorRecords.put(absFilePath,
                                                      list_errorrecords);
                    }
                    if (allfin_analysisRecords.containsKey(absFilePath)) {
                        ArrayList<TuningAnalysisRecord> list_analysisInFile =
                            allfin_analysisRecords.get(absFilePath);
                        onlyreqiured_analysisRecords.put(absFilePath,
                                                         list_analysisInFile);
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

    public ArrayList<TuningAnalysisRecord> fixPageDefIterators(String uiFilePath,
                                                               boolean bDoADE,
                                                               HashMap<String, TuningAnalysisRecord> multipleIteratorFetchSizes) {
        ArrayList<TuningAnalysisRecord> list_iteratorsFixed =
            new ArrayList<TuningAnalysisRecord>();

        ArrayList<TuningAnalysisRecord> records =
            allfin_analysisRecords.get(uiFilePath);
        if (records == null) {
            return null;
        }
        // to avoid any npes when accessing this map
        if (multipleIteratorFetchSizes == null) {
            multipleIteratorFetchSizes =
                    new HashMap<String, TuningAnalysisRecord>();
        }
        Iterator<TuningAnalysisRecord> recordIter = records.iterator();
        while (recordIter.hasNext()) {
            TuningAnalysisRecord record = recordIter.next();
            if (record == null) {
                continue;
            }
            // get the page def
            String pageDefPath = record.getPageDefFilePath();
            String pageDefIteratorName = record.getPageDefIteratorName();
            String pageDefKey = pageDefPath + "#" + pageDefIteratorName;

            if (bDebug) {
                System.out.println("Processing page def: : " + pageDefPath);
            }


            try {
                TuningAnalysisRecord newrecord = null;
                if (multipleIteratorFetchSizes.containsKey(pageDefKey)) {
                    System.out.println("UIFilePath: " + uiFilePath);
                    System.out.println("Found multiple iterator fetch size usage: " +
                                       pageDefIteratorName);
                    System.out.println("pagedef key " + pageDefKey);
                    newrecord = multipleIteratorFetchSizes.get(pageDefKey);
                    String newRecordFixStatus = newrecord.getFixStatus();
                    record.setFixStatus(newRecordFixStatus);
                    record.setIteratorRangeSizeNewValue(newrecord.getIteratorRangeSizeNewValue());

                } else {
                    newrecord =
                            Rules.calculateAutoHeightNIteratorRange(record);
                }
                if (newrecord == null) {
                    continue;
                }
                String iteratorFixStatus = newrecord.getFixStatus();
                if (!isEmpty(iteratorFixStatus) &&
                    iteratorFixStatus.equals(TuningAnalysisRecord.STATUS_WILLFIX_ITERATOR)) {

                    XmlFixer pageDefFixer = new XmlFixer(pageDefPath);
                    boolean fixMade =
                        fixIteratorRangeSizeInPageDef(record, pageDefFixer);
                    if (fixMade) {
                        if (bDoADE) {
                            String pageDefFileContents =
                                pageDefFixer.getFileContentsAsString(false);
                            applyFix(pageDefFixer.getFilePath(),
                                     pageDefFileContents);
                        }
                        m_pageDefsFixed++;
                        record.setFixStatus(TuningAnalysisRecord.STATUS_FIX_ITERATOR_SUCCESS);
                        if (multipleIteratorFetchSizes.containsKey(pageDefKey)) {
                            newrecord.setFixStatus(TuningAnalysisRecord.STATUS_FIX_MULTITERATOR_SUCCESS);
                            multipleIteratorFetchSizes.put(pageDefKey,
                                                           newrecord);
                        }
                    }
                } else if (!isEmpty(iteratorFixStatus) &&
                           iteratorFixStatus.equals(TuningAnalysisRecord.STATUS_FIX_ITERATOR_NOCHANGE)) {

                }

            } catch (Exception ex) {
                record.setFixStatus(TuningAnalysisRecord.STATUS_FIX_ITERATOR_FAILED);

                System.out.println("[ERROR] Ecxeption while trying to fix record with pagedef: " +
                                   pageDefPath);
                System.out.println("Skipping this record");
                if (multipleIteratorFetchSizes.containsKey(pageDefKey)) {
                    TuningAnalysisRecord recordAgain =
                        multipleIteratorFetchSizes.get(pageDefKey);
                    recordAgain.setFixStatus(TuningAnalysisRecord.STATUS_FIX_MULTITERATOR_FAILED);
                    multipleIteratorFetchSizes.put(pageDefKey, recordAgain);
                }
            }
            list_iteratorsFixed.add(record);
        }
        return list_iteratorsFixed;
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
    public boolean doesDefaultVONeedFix(TuningAnalysisRecord record,
                                        HashMap<String, Integer> alreadyFixedVos) {
        /* If the am is going to be fixed - need to make sure that :
         * 1. VO Usage fetch size is not less than the DefaultFetchSize.
         * 2. If voUsageFetchSize is less than the default fetch size : fix the default fetch size to be the voUsageFetchSize
         */
        // preliminary validations
        if (record == null) {
            return false;
        }

        boolean voNeedsFix = false;
        String voPath = record.getVoPath();
        if (isEmpty(voPath)) {
            return false;
        }

        Integer voUsageFetchSizeNewValInteger = null;
        Integer defaultFetchSizeInteger = null;
        if (alreadyFixedVos.containsKey(voPath)) {
            defaultFetchSizeInteger = alreadyFixedVos.get(voPath);
        } else {
            String defaultFetchSize = record.getVoDefaultFetchSize();
            if (isEmpty(defaultFetchSize) ||
                defaultFetchSize.trim().equalsIgnoreCase("not set")) {
                return false;
            }
            try {
                defaultFetchSizeInteger = Integer.parseInt(defaultFetchSize);
            } catch (Exception ex) {
                String msg =
                    String.format("ERROR: [doesDefaultVONeedFix]: VOPath %s: cannot convert defaultFetchSize %s to Integer",
                                  voPath, defaultFetchSize);
                errorLogger.log(Level.SEVERE, msg);
                System.out.println(msg);
                return false;
            }
        }
        if (defaultFetchSizeInteger == null || defaultFetchSizeInteger <= 0) {
            return false;
        }

        String voUsageFetchSizeNewVal = record.getVoUsageFetchSizeNewValue();
        if (isEmpty(voUsageFetchSizeNewVal)) {
            return false;
        }
        try {
            voUsageFetchSizeNewValInteger =
                    Integer.parseInt(voUsageFetchSizeNewVal);

        } catch (Exception ex) {
            String msg =
                String.format("ERROR: [doesDefaultVONeedFix]: VOPath %s: cannot convert voUsageNewFetchSize %s to Integer",
                              voPath, voUsageFetchSizeNewVal);
            errorLogger.log(Level.SEVERE, msg);
            System.out.println(msg);
            return false;
        }

        if (voUsageFetchSizeNewValInteger == null ||
            voUsageFetchSizeNewValInteger <= 0) {
            return false;
        }

        if (defaultFetchSizeInteger > voUsageFetchSizeNewValInteger) {
            voNeedsFix = true;
            record.setVoDefaultFetchSizeNewValue(voUsageFetchSizeNewValInteger.toString());
        }

        return voNeedsFix;

    }

    public boolean canFixVO(TuningAnalysisRecord record) {
        if (record == null) {
            return false;
        }

        String voPath = record.getVoPath();
        if (isEmpty(voPath) || voPath.trim().endsWith(".jar")) {
            return false;
        }
        if (voPath.trim().contains("JAR:")) {
            return false;
        }

        File f = new File(voPath);
        if (f.exists()) {
            return true;
        }

        System.out.println("Cannot fix vo path because absoltue vo cannot be obtained");
        return false;
    }

    public ArrayList<TuningAnalysisRecord> fixAmsForUIFiles(ArrayList<TuningAnalysisRecord> records,
                                                            boolean bDoADE) {
        ArrayList<TuningAnalysisRecord> list_records =
            new ArrayList<TuningAnalysisRecord>();

        if (records == null) {
            return null;
        }

        Iterator<TuningAnalysisRecord> recordIter = records.iterator();
        while (recordIter.hasNext()) {
            TuningAnalysisRecord record = recordIter.next();
            if (record == null) {
                continue;
            }
            // -------------Fix AM
            String amPath = record.getAmPath();
            String iteratorFixStatus = record.getFixStatus();
            String voDefaultFetchSizeFixStatus = "";
            try {

                if (isEmpty(amPath)) {
                    record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
                    record.setFixStatusComment("Am Path is null");
                    list_records.add(record);
                    continue;
                }
                if (bDebug) {
                    System.out.println("Processing am: : " + amPath);
                }

                if (!isEmpty(iteratorFixStatus) &&
                    (iteratorFixStatus.trim().contains(TuningAnalysisRecord.STATUS_FIX_ITERATOR_SUCCESS) ||
                     iteratorFixStatus.trim().contains(TuningAnalysisRecord.STATUS_FIX_ITERATOR_NOCHANGE) ||
                     iteratorFixStatus.trim().contains(TuningAnalysisRecord.STATUS_FIX_MULTITERATOR_SUCCESS))) {

                    TuningAnalysisRecord newrecord =
                        Rules.calculateVOFetchSize(record,
                                                   m_map_violatingBcs_records);
                    String amFixStatus = newrecord.getFixStatus();

                    if (!isEmpty(amFixStatus) &&
                        amFixStatus.contains(TuningAnalysisRecord.STATUS_WILLFIX_AM)) {
                        boolean defaultVONeedsFix =
                            doesDefaultVONeedFix(record,
                                                 defaultVOFetchSizesFixed);
                        if (defaultVONeedsFix) {
                            if (bDebug) {
                                System.out.println("Default vo needs fix");
                            }
                            // if vo needs fix and fix cannot be made then skip the record - because we cannot be setting the vo usage to > default fetch size. runtime error
                            if (!canFixVO(record)) {
                                String msg =
                                    "[ERROR] Default VO Needs fix (usageSize < defaultFetchSize).. but vo cannot be fixed because it cannot be obtained";
                                msg =
 msg + "\n Skipping this record and not fixing am vo usage since it will throw a runtime error";
                                System.out.println(msg);
                                errorLogger.log(Level.SEVERE, msg);
                                record.setFixStatus(iteratorFixStatus + "," +
                                                    TuningAnalysisRecord.STATUS_FIX_AM_FAILED);
                                record.setFixStatusComment(msg);
                                list_records.add(record);
                                continue;
                            } else {
                                // fix vo
                                String voPath = record.getVoPath();
                                XmlFixer voFixer = new XmlFixer(voPath);

                                boolean voFixMade =
                                    fixDefaultFetchSizeInVO(record, voFixer);
                                if (voFixMade) {
                                    if (bDoADE) {
                                        String vofileContents =
                                            voFixer.getFileContentsAsString(false);

                                        applyFix(voPath, vofileContents);
                                    }

                                    record.setFixStatus(iteratorFixStatus +
                                                        " , " +
                                                        TuningAnalysisRecord.STATUS_DEFAULTVO_FIX_SUCCESS);
                                    voDefaultFetchSizeFixStatus =
                                            TuningAnalysisRecord.STATUS_DEFAULTVO_FIX_SUCCESS;
                                    String newDefaultFetchSize =
                                        record.getVoDefaultFetchSizeNewValue();
                                    Integer defaultnewInteger = null;
                                    try {
                                        defaultnewInteger =
                                                Integer.parseInt(newDefaultFetchSize);
                                        defaultVOFetchSizesFixed.put(voPath,
                                                                     defaultnewInteger);
                                    } catch (Exception ex) {
                                        System.out.println("Exception while trying to conver to integer while putting map: " +
                                                           newDefaultFetchSize);
                                    }
                                } else {

                                    // fix was needed but fix was not made.. this is an error because it will throw a runtime exception. do not fix the am usage
                                    record.setFixStatus(iteratorFixStatus +
                                                        " , " +
                                                        TuningAnalysisRecord.STATUS_DEFAULTVO_FIX_FAILED);
                                    record.setFixStatusComment("Default vo fix was needed but it failed.. skipping record.");
                                    list_records.add(record);
                                    continue;
                                }
                            }
                        }
                    }

                    if (!isEmpty(amFixStatus) &&
                        amFixStatus.contains(TuningAnalysisRecord.STATUS_WILLFIX_AM)) {
                        XmlFixer amFixer = new XmlFixer(amPath);
                        boolean fixMade = fixFetchSizeInAm(record, amFixer);
                        if (fixMade) {
                            if (bDoADE) {
                                String amFileContents =
                                    amFixer.getFileContentsAsString(false);

                                applyFix(amPath, amFileContents);
                            }
                            m_amsFixed++;
                            record.setFixStatus(iteratorFixStatus + " , " +
                                                voDefaultFetchSizeFixStatus +
                                                "," +
                                                TuningAnalysisRecord.STATUS_FIX_AM_SUCCESS);
                            list_records.add(record);

                        }
                    } else if (!isEmpty(amFixStatus) &&
                               amFixStatus.contains(TuningAnalysisRecord.STATUS_MULTIPLE_BC)) {
                        String bcKey =
                            record.getAmFullPackage() + "#" + record.getVoInstanceName();
                        UiFileHelper.addToMap(onlyreq_multipleBcs_fixedrecords,
                                              bcKey, record);
                    } else if(amFixStatus.contains(TuningAnalysisRecord.STATUS_AMFETCH_UNCHANGED))
                    {
                        record.setFixStatus(iteratorFixStatus+" " + amFixStatus);
                        list_records.add(record);
                    }else {
                        list_records.add(record);
                    }

                } else {
                    list_records.add(record);
                }
            } catch (Exception ex) {
                record.setFixStatus(iteratorFixStatus + " , " +
                                    voDefaultFetchSizeFixStatus + "," +
                                    TuningAnalysisRecord.STATUS_FIX_AM_FAILED);

                System.out.println("Ecxeption while trying to fix record am: " +
                                   amPath);
                System.out.println("Skipping this record");
                list_records.add(record);
                continue;
            }

        }
        return list_records;
    }

    public void makeFix(String uiFilePath, boolean bDoADE,
                        HashMap<String, TuningAnalysisRecord> multipleIteratorFetchSizes) {
        if (bDebug) {
            System.out.println("Processing file: " + uiFilePath);
        }
        ArrayList<TuningAnalysisRecord> records =
            fixPageDefIterators(uiFilePath, bDoADE,
                                multipleIteratorFetchSizes);
        if (records == null) {
            System.out.println("No page def iterators fixed.. returning");
        }
        records = fixAmsForUIFiles(records, bDoADE);
        onlyreqiured_analysisRecords.put(uiFilePath, records);
    }


    /**
     * Process each valid ui file. If the file has any found violations, fix them and increment the violation count
     * @param file
     * @param bDoADE
     */
    public void analyseUiFile(File file) {
        String uiFilePath = file.getAbsolutePath();
        System.out.println("Processing file: " + uiFilePath);

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
                System.out.println("[ERROR] Bindings was null.. skipping file");
                return;
            }
        }
        if (bindings != null) {
            // debug
            // bindings.printBindings();

            VOTuningUIProcessor tuningProcessor = new VOTuningUIProcessor();
            tuningProcessor.setRecordMultipleIteratorUsages(recordMultipleIteratorUsages);
            ArrayList<TuningAnalysisRecord> records =
                tuningProcessor.analyzeUIFile(file, bindings, errorLogger);
            if (records != null && records.size() > 0) {
                allfin_analysisRecords.put(uiFilePath, records);
            }

            ArrayList<TuningAnalysisRecord> errorRecordsInUiFile =
                tuningProcessor.getList_errorRecords();
            if (errorRecordsInUiFile != null &&
                errorRecordsInUiFile.size() > 0) {
                allfin_errorRecords.put(uiFilePath, errorRecordsInUiFile);
            }

           

            HashMap<String, ArrayList<TuningAnalysisRecord>> bcUsagesInPage =
                tuningProcessor.getBcUsages();
            if (bcUsagesInPage != null && bcUsagesInPage.size() > 0) {
                m_map_bcs_records =
                        addtoMap(m_map_bcs_records, bcUsagesInPage);
            }

            if (recordMultipleIteratorUsages) {
                HashMap<String, ArrayList<TuningAnalysisRecord>> iteratorUsagesInPage =
                    tuningProcessor.getMapOfMultipleIteratorUsages();
                if (iteratorUsagesInPage != null &&
                    iteratorUsagesInPage.size() > 0) {
                    m_mapOfMultipleIteratorUsages =
                            addtoMap(m_mapOfMultipleIteratorUsages,
                                     iteratorUsagesInPage);
                }
            }


        }
        System.out.println("\n");
    }

    //========================HELPER FUNCTIONS================================


    public static String getPass1AnalysisPath(String analysisDirectory) {
        if (!isEmpty(analysisDirectory)) {
            String searchStr = "/fusionapps/fin/components";
            int index = analysisDirectory.indexOf(searchStr);
            if (index == -1) {
                // so that it works for all families and not just financials
                searchStr = "/components";
                index = analysisDirectory.indexOf(searchStr);
                if (index == -1) {
                    return null;
                }
            }
            return analysisDirectory.substring(0, index + searchStr.length());
        } else {
            return null;
        }
    }


    /**
     * Valid directories are those that do not contain
     * - model and dbschema and publicModel and test and classes
     * @param absDirPath
     * @return
     */
    public boolean isValidDirectory(String absDirPath) {
        if (!absDirPath.contains("/model/") &&
            !absDirPath.contains("/dbSchema/") &&
            !absDirPath.contains("/publicModel/") &&
            !absDirPath.contains("/classes/") &&
            !absDirPath.contains("/test/")) {
            return true;
        }
        return false;
    }

    /**
     * Valid ui files are those that
     * - end with jspx or jsff
     * - and are not in a directory containing test or modeltest or classes or model or publicmodel
     * @param absFilePath
     * @return
     */
    public boolean isValidUIFile(String absFilePath) {
        if (!isEmpty(absFilePath)) {
            if (absFilePath.contains("/test/") ||
                absFilePath.contains("/modelTest/") ||
                absFilePath.contains("/classes/") ||
                absFilePath.contains("/model/") ||
                absFilePath.contains("/publicModel/")) {
                return false;
            }
            if (absFilePath.endsWith(".jspx") ||
                absFilePath.endsWith(".jsff")) {
                return true;
            }
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

    //========================[END] HELPER FUNCTIONS================================

    //=======================WORKSHEET RELATED FUNCTIONS===================================

    private static void writeToSpreadsheet(HashMap<String, ArrayList<TuningAnalysisRecord>> records,
                                           String worksheetName) {

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

    private static void writeBCViolationsToSpreadsheet(HashMap<String, ArrayList<TuningAnalysisRecord>> records) {

        if (records == null) {
            return;
        }
        int rowNumber = 2;
        initializeBCUsagesSheet();
        Set<String> setofkeys = records.keySet();
        Iterator<String> keyIter = setofkeys.iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            ArrayList<TuningAnalysisRecord> recordlist = records.get(key);
            if (recordlist != null && recordlist.size() > 1) {
                Iterator<TuningAnalysisRecord> iterRecord =
                    recordlist.iterator();
                while (iterRecord.hasNext()) {
                    TuningAnalysisRecord recordToWrite = iterRecord.next();
                    writeComponentInSheetRow(rowNumber, key, recordToWrite);
                    rowNumber++;
                }
            }

        }

    }

    private static void intializeSpreadsheet(String worksheetName) {
        if (m_myBook == null) {
            m_myBook = new Workbook("output");
        }
        // Create a worksheet
        m_violationsSheet = new Worksheet(worksheetName);
        // column 1 - product
        m_violationsSheet.setColumnWidth(1, 10);
        // column 2 - filename
        m_violationsSheet.setColumnWidth(2, 30);
        // Column 3 - file path
        m_violationsSheet.setColumnWidth(3, 40);
        // Column 4 - component Name
        m_violationsSheet.setColumnWidth(4, 30);
        // Column 5 - autoHeightRows attr
        m_violationsSheet.setColumnWidth(5, 30);
        // Column 6 -  new autoHeightRows attr
        m_violationsSheet.setColumnWidth(6, 30);
        // Column 7 - Iterator fetch size
        m_violationsSheet.setColumnWidth(7, 30);
        // Column 8 - New Iterator fetch size
        m_violationsSheet.setColumnWidth(8, 30);
        // Column 9 - vousage fetch size
        m_violationsSheet.setColumnWidth(9, 30);
        // Column 10 - vo default fetch size
        m_violationsSheet.setColumnWidth(10, 30);
        // Column 11 - vnew vo usage fetch sizeo default fetch size
        m_violationsSheet.setColumnWidth(11, 30);
        // Column 12 - am name
        m_violationsSheet.setColumnWidth(12, 30);
        // Column 13 - vo name
        m_violationsSheet.setColumnWidth(13, 30);

        // Column 14 - content delivery
        m_violationsSheet.setColumnWidth(14, 30);
        // Column 15 - rows attr
        m_violationsSheet.setColumnWidth(15, 30);
        // Column 16 - fetchSize attr
        m_violationsSheet.setColumnWidth(16, 30);
        // Column 17 - diagnostics
        m_violationsSheet.setColumnWidth(17, 30);
        // Column 18 - fix status
        m_violationsSheet.setColumnWidth(18, 30);

        // column 1 - product
        m_violationsSheet.addCell(new Cell(new CellLocation("A1"),
                                           new CellValue("Product")));
        // column 2 - filename
        m_violationsSheet.addCell(new Cell(new CellLocation("B1"),
                                           new CellValue("File Name")));
        // Column 3 - file path
        m_violationsSheet.addCell(new Cell(new CellLocation("C1"),
                                           new CellValue("File Path")));
        // Column 4 - component Name
        m_violationsSheet.addCell(new Cell(new CellLocation("D1"),
                                           new CellValue("Component Name")));
        // Column 5 - autoHeightRows attr
        m_violationsSheet.addCell(new Cell(new CellLocation("E1"),
                                           new CellValue("Attr:autoHeightRows")));
        // Column 6 -  new autoHeightRows attr
        m_violationsSheet.addCell(new Cell(new CellLocation("F1"),
                                           new CellValue("calculated (newAutoHeight)")));
        // Column 7 - Iterator fetch size
        m_violationsSheet.addCell(new Cell(new CellLocation("G1"),
                                           new CellValue("Attr:Iterator.FetchSize")));
        // Column 8 - New: Iterator fetch size
        m_violationsSheet.addCell(new Cell(new CellLocation("H1"),
                                           new CellValue("calculated (New IteratorFetchSize)")));
        // Column 9 - vousage fetch size
        m_violationsSheet.addCell(new Cell(new CellLocation("I1"),
                                           new CellValue("vo usage fetch size")));
        // Column 10 - vo default fetch size
        m_violationsSheet.addCell(new Cell(new CellLocation("J1"),
                                           new CellValue("vo default fetch size")));
        // Column 11 - vnew vo usage fetch sizeo default fetch size
        m_violationsSheet.addCell(new Cell(new CellLocation("K1"),
                                           new CellValue("calculated: New vo usage fetch size")));
        // Column 12 - am name
        m_violationsSheet.addCell(new Cell(new CellLocation("L1"),
                                           new CellValue("AM Name")));
        // Column 13 - vo name
        m_violationsSheet.addCell(new Cell(new CellLocation("M1"),
                                           new CellValue("VO Name")));
        // Column 14 - content delivery
        m_violationsSheet.addCell(new Cell(new CellLocation("N1"),
                                           new CellValue("content delivery")));
        // Column 15 - rows attr
        m_violationsSheet.addCell(new Cell(new CellLocation("O1"),
                                           new CellValue("Attr:rows")));
        // Column 16 - fetchSize attr
        m_violationsSheet.addCell(new Cell(new CellLocation("P1"),
                                           new CellValue("Attr:fetch size")));
        // Column 17 - diagnostics
        m_violationsSheet.addCell(new Cell(new CellLocation("Q1"),
                                           new CellValue("Diagnostics")));
        // Column 17 - diagnostics
        m_violationsSheet.addCell(new Cell(new CellLocation("R1"),
                                           new CellValue("Fix Status")));

        m_myBook.addWorksheet(m_violationsSheet);
    }

    private static void initializeBCUsagesSheet() {
        if (m_myBook == null) {
            m_myBook = new Workbook("output");
        }
        m_multipleBcSheet = new Worksheet(MULTIPLEBC_SHEET_NAME);
        // column 1 - bc key
        m_multipleBcSheet.setColumnWidth(1, 10);
        // column 2 - product
        m_multipleBcSheet.setColumnWidth(2, 10);
        // column 3 - filename
        m_multipleBcSheet.setColumnWidth(3, 30);
        // Column 4 - file path
        m_multipleBcSheet.setColumnWidth(4, 40);
        // Column 5 - autoHeightRows attr
        m_multipleBcSheet.setColumnWidth(5, 30);
        // Column 6 - Iterator range size
        m_multipleBcSheet.setColumnWidth(6, 30);
        // Column 7 - vousage fetch size
        m_multipleBcSheet.setColumnWidth(7, 30);
        // Column 8 - vo default fetch size
        m_multipleBcSheet.setColumnWidth(8, 30);


        m_multipleBcSheet.addCell(new Cell(new CellLocation("A1"),
                                           new CellValue("AM # VO")));
        m_multipleBcSheet.addCell(new Cell(new CellLocation("B1"),
                                           new CellValue("Product")));
        m_multipleBcSheet.addCell(new Cell(new CellLocation("C1"),
                                           new CellValue("File Name")));
        m_multipleBcSheet.addCell(new Cell(new CellLocation("D1"),
                                           new CellValue("File Path")));
        m_multipleBcSheet.addCell(new Cell(new CellLocation("E1"),
                                           new CellValue("Attr:autoHeightRows")));
        m_multipleBcSheet.addCell(new Cell(new CellLocation("F1"),
                                           new CellValue("Iterator:rangeSize")));
        m_multipleBcSheet.addCell(new Cell(new CellLocation("G1"),
                                           new CellValue("VO usage fetch size")));
        m_multipleBcSheet.addCell(new Cell(new CellLocation("H1"),
                                           new CellValue("VO default fetch size")));


        m_myBook.addWorksheet(m_multipleBcSheet);
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
    public static void writeComponentInSheetRow(int rowNumber,
                                                TuningAnalysisRecord record) {


        String productCellNumber = "A" + rowNumber;
        String fileNameCellNumber = "B" + rowNumber;
        String filePathCellNumber = "C" + rowNumber;
        String compNameCellNumber = "D" + rowNumber;
        String autoHeightRowsCellNumber = "E" + rowNumber;
        String New_autoHeightRowsCellNumber = "F" + rowNumber;
        String iteratorFetchSizeCellNumber = "G" + rowNumber;
        String New_iteratorFetchSizeCellNumber = "H" + rowNumber;
        String voUsageFetchSizeCellNumber = "I" + rowNumber;
        String voDefaultFetchSizeCellNumber = "J" + rowNumber;
        String New_voUsageFetchSizeCellNumber = "K" + rowNumber;

        String amPathCellNumber = "L" + rowNumber;
        String voPathCellNumber = "M" + rowNumber;
        String contentDeliveryCellNumber = "N" + rowNumber;
        String rowsCellNumber = "O" + rowNumber;
        String fetchSizeAttrCellNumber = "P" + rowNumber;
        String diagnostics = "Q" + rowNumber;
        String fixstatus = "R" + rowNumber;

        m_violationsSheet.addCell(new Cell(new CellLocation(productCellNumber),
                                           new CellValue(record.getProduct())));

        m_violationsSheet.addCell(new Cell(new CellLocation(fileNameCellNumber),
                                           new CellValue(record.getUifileName())));

        m_violationsSheet.addCell(new Cell(new CellLocation(filePathCellNumber),
                                           new CellValue(record.getUifilePath())));


        m_violationsSheet.addCell(new Cell(new CellLocation(compNameCellNumber),
                                           new CellValue(record.getComponentType())));


        m_violationsSheet.addCell(new Cell(new CellLocation(autoHeightRowsCellNumber),
                                           new CellValue(record.getAutoHeightRows())));
        m_violationsSheet.addCell(new Cell(new CellLocation(New_autoHeightRowsCellNumber),
                                           new CellValue(record.getAutoHeightNewValue())));


        m_violationsSheet.addCell(new Cell(new CellLocation(rowsCellNumber),
                                           new CellValue(record.getUiTagRows())));

        m_violationsSheet.addCell(new Cell(new CellLocation(fetchSizeAttrCellNumber),
                                           new CellValue(record.getUiTagFetchSize())));

        m_violationsSheet.addCell(new Cell(new CellLocation(iteratorFetchSizeCellNumber),
                                           new CellValue(record.getIteratorRangeSize())));
        m_violationsSheet.addCell(new Cell(new CellLocation(New_iteratorFetchSizeCellNumber),
                                           new CellValue(record.getIteratorRangeSizeNewValue())));

        m_violationsSheet.addCell(new Cell(new CellLocation(amPathCellNumber),
                                           new CellValue(record.getAmPath())));

        m_violationsSheet.addCell(new Cell(new CellLocation(voPathCellNumber),
                                           new CellValue(record.getVoFullPackage())));

        m_violationsSheet.addCell(new Cell(new CellLocation(voUsageFetchSizeCellNumber),
                                           new CellValue(record.getVoUsageFetchSizeInAm())));
        m_violationsSheet.addCell(new Cell(new CellLocation(New_voUsageFetchSizeCellNumber),
                                           new CellValue(record.getVoUsageFetchSizeNewValue())));

        m_violationsSheet.addCell(new Cell(new CellLocation(voDefaultFetchSizeCellNumber),
                                           new CellValue(record.getVoDefaultFetchSize())));
        m_violationsSheet.addCell(new Cell(new CellLocation(contentDeliveryCellNumber),
                                           new CellValue(record.getContentDelivery())));
        m_violationsSheet.addCell(new Cell(new CellLocation(diagnostics),
                                           new CellValue(record.getDiagnostic())));
        m_violationsSheet.addCell(new Cell(new CellLocation(fixstatus),
                                           new CellValue(record.getFixStatus())));


    }

    /**
     * Write the component details in the spreadsheet row
     * @param rowNumber
     * row number to write to
     * @param component
     * Component details
     */
    public static void writeComponentInSheetRow(int rowNumber, String bcKey,
                                                TuningAnalysisRecord record) {

        String bcKeyCellNumber = "A" + rowNumber;
        String productCellNumber = "B" + rowNumber;
        String fileNameCellNumber = "C" + rowNumber;
        String filePathCellNumber = "D" + rowNumber;
        String autoHeightRowsCellNumber = "E" + rowNumber;
        String iteratorFetchSizeCellNumber = "F" + rowNumber;
        String voUsageFetchSizeCellNumber = "G" + rowNumber;
        String voDefaultFetchSizeCellNumber = "H" + rowNumber;

        m_multipleBcSheet.addCell(new Cell(new CellLocation(bcKeyCellNumber),
                                           new CellValue(bcKey)));
        m_multipleBcSheet.addCell(new Cell(new CellLocation(productCellNumber),
                                           new CellValue(record.getProduct())));

        m_multipleBcSheet.addCell(new Cell(new CellLocation(fileNameCellNumber),
                                           new CellValue(record.getUifileName())));

        m_multipleBcSheet.addCell(new Cell(new CellLocation(filePathCellNumber),
                                           new CellValue(record.getUifilePath())));


        m_multipleBcSheet.addCell(new Cell(new CellLocation(autoHeightRowsCellNumber),
                                           new CellValue(record.getAutoHeightRows())));


        m_multipleBcSheet.addCell(new Cell(new CellLocation(iteratorFetchSizeCellNumber),
                                           new CellValue(record.getIteratorRangeSize())));


        m_multipleBcSheet.addCell(new Cell(new CellLocation(voUsageFetchSizeCellNumber),
                                           new CellValue(record.getVoUsageFetchSizeInAm())));

        m_multipleBcSheet.addCell(new Cell(new CellLocation(voDefaultFetchSizeCellNumber),
                                           new CellValue(record.getVoDefaultFetchSize())));


    }

    //=======================[END] WORKSHEET RELATED FUNCTIONS===================================

    public HashMap<String, ArrayList<TuningAnalysisRecord>> addtoMap(HashMap<String, ArrayList<TuningAnalysisRecord>> mapToAddTo,
                                                                     TuningAnalysisRecord record,
                                                                     String uiKey) {
        if (mapToAddTo == null) {
            mapToAddTo =
                    new HashMap<String, ArrayList<TuningAnalysisRecord>>();
        }

        if (mapToAddTo.containsKey(uiKey)) {
            ArrayList<TuningAnalysisRecord> existingRecords =
                mapToAddTo.get(uiKey);
            existingRecords.add(record);
            mapToAddTo.put(uiKey, existingRecords);
        } else {
            ArrayList<TuningAnalysisRecord> existingRecords =
                new ArrayList<TuningAnalysisRecord>();
            existingRecords.add(record);
            mapToAddTo.put(uiKey, existingRecords);
        }

        return mapToAddTo;

    }

    public HashMap<String, ArrayList<TuningAnalysisRecord>> addtoMap(HashMap<String, ArrayList<TuningAnalysisRecord>> mapToAddTo,
                                                                     HashMap<String, ArrayList<TuningAnalysisRecord>> mapToAdd) {
        if (mapToAddTo == null) {
            mapToAddTo =
                    new HashMap<String, ArrayList<TuningAnalysisRecord>>();
        }
        if (mapToAdd == null || mapToAdd.size() <= 0) {
            return mapToAddTo;
        }
        Set<String> keySet = mapToAdd.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            ArrayList<TuningAnalysisRecord> recordListToAdd =
                mapToAdd.get(key);
            if (mapToAddTo.containsKey(key)) {
                ArrayList<TuningAnalysisRecord> existingRecords =
                    mapToAddTo.get(key);
                existingRecords.addAll(recordListToAdd);
                mapToAddTo.put(key, existingRecords);
            } else {
                mapToAddTo.put(key, recordListToAdd);
            }
        }
        return mapToAddTo;

    }


    //====================DEBUGGING RELATED PRINT FUNCTIONS=====================================

    public void printViolatingBCs(HashMap<String, ArrayList<String>> mapbcs) {
        if (mapbcs == null) {
            return;
        }
        Set<String> bcset = mapbcs.keySet();
        Iterator<String> bcIter = bcset.iterator();
        while (bcIter.hasNext()) {
            String bc = bcIter.next();
            ArrayList<String> pages = mapbcs.get(bc);
            if (pages != null && pages.size() > 1) {
                System.out.println("----------------------------");
                System.out.println("violation found");
                System.out.println("BC: " + bc);
                System.out.println("Pages: " + pages.toString());
            }
        }

    }

    public void printIteratorUsagesViolations(HashMap<String, ArrayList<TuningAnalysisRecord>> mapUsages) {
        if (mapUsages == null) {
            return;
        }
        Set<String> bcset = mapUsages.keySet();
        Iterator<String> bcIter = bcset.iterator();
        while (bcIter.hasNext()) {
            String bc = bcIter.next();
            ArrayList<TuningAnalysisRecord> iteratorList = mapUsages.get(bc);
            if (iteratorList != null && iteratorList.size() > 1) {
                System.out.println("----------------------------");
                System.out.println("violation found");
                System.out.println("pagedefusage: " + bc);
                Iterator<TuningAnalysisRecord> recordIter =
                    iteratorList.iterator();
                while (recordIter.hasNext()) {
                    TuningAnalysisRecord rec = recordIter.next();
                    System.out.println("page name: " + rec.getUifileName());
                    System.out.println("auto height: " +
                                       rec.getAutoHeightRows());
                }
                System.out.println("********");
            }
        }

    }


    public void printStatistics(Date startDate) {
        // Print Statistics
        Date endDate = new Date();
        System.out.println("=================================================");
        System.out.println("Run Statistics");
        System.out.println("Total Number of jspx files reviewed: " +
                           m_jspxCount);
        System.out.println("Total Number of jsff files reviewed: " +
                           m_jsffCount + "\n");
        System.out.println("total number of pagedefs fixed: " +
                           m_pageDefsFixed);
        System.out.println("total number of ams fixed: " + m_amsFixed);

        System.out.println("Start Time: " + startDate.toString());
        System.out.println("End Time: " + endDate.toString());
        long elapsedTime = endDate.getTime() - startDate.getTime();
        System.out.println("Elapsed Time (sec): " + elapsedTime / 1000);
        System.out.println("=================================================");

        System.out.println("=== Default VO Fixes Statistics ===");
        if (defaultVOFetchSizesFixed != null) {
            System.out.println("Number of default VOs updated with new default fetch sizes: " +
                               defaultVOFetchSizesFixed.size());
            Set<String> keys = defaultVOFetchSizesFixed.keySet();
            Iterator<String> keyIter = keys.iterator();
            while (keyIter.hasNext()) {
                String key = keyIter.next();
                Integer val = defaultVOFetchSizesFixed.get(key);
                String msg =
                    String.format("Default FetchSizeSet: %s in VO: %s", val,
                                  key);
                System.out.println(msg);

            }


        }

    }

    //====================[END] DEBUGGING RELATED PRINT FUNCTIONS=====================================

    //=================LOGGER RELATED FUNCTIONS===============================

    public static Logger initLogger() {
        Logger errorLogger = null;
        try {
            // Initialize logger
            errorLogger = Logger.getLogger("RunAutomatedTuningFixer");

            errorLogger.setLevel(Level.ALL);
            FileHandler filehandler = new FileHandler(ERROR_LOG_FILENAME);
            Formatter fileformatter = new XMLFormatter();
            filehandler.setFormatter(fileformatter);
            errorLogger.addHandler(filehandler);
            ConsoleHandler consolehandler = new ConsoleHandler();
            errorLogger.addHandler(consolehandler);
        } catch (Exception ex) {
            System.out.println("Exception while initializing logger");
        }
        return errorLogger;
    }
    //=================[END] LOGGER RELATED FUNCTIONS===============================


    public void applyFix(String absPath,
                         String newFileContents) throws Exception {
        File file = new File(absPath);
        if (!file.exists()) {
            throw new Exception("while making fix, file not found: " +
                                absPath);
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

    //[START]===============XML Writing functions=====================

    private Element writeErrorRecords(Document doc,
                                      HashMap<String, ArrayList<TuningAnalysisRecord>> errorRecords) {
        if (errorRecords == null || errorRecords.size() <= 0) {
            return null;
        }
        Element errorRecordsNode = doc.createElement("ErrorRecords");
        Set<String> keySet = errorRecords.keySet();
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

            ArrayList<TuningAnalysisRecord> errorrecordsList =
                errorRecords.get(filePath);
            if (errorrecordsList != null && errorrecordsList.size() > 0) {
                Iterator<TuningAnalysisRecord> recordIter =
                    errorrecordsList.iterator();
                while (recordIter.hasNext()) {
                    Element recordNode = doc.createElement("component");
                    TuningAnalysisRecord rec = recordIter.next();
                    recordNode.setAttribute("dataBindingsPath",
                                            rec.getCpxFilePath());
                    recordNode.setAttribute("pageDefPath",
                                            rec.getPageDefFilePath());
                    recordNode.setAttribute("type", "table");
                    recordNode.setAttribute("autoHeightRows",
                                            rec.getAutoHeightRows());
                    recordNode.setAttribute("diagnostics",
                                            rec.getDiagnostic());

                    Element iteratorNode = doc.createElement("iterator");
                    iteratorNode.setAttribute("name",
                                              rec.getPageDefIteratorName());
                    iteratorNode.setAttribute("rangeSize",
                                              rec.getIteratorRangeSize());
                    recordNode.appendChild(iteratorNode);

                    Element bcObjNode = doc.createElement("BCObject");
                    bcObjNode.setAttribute("amPath", rec.getAmPath());
                    bcObjNode.setAttribute("voFullPackage",
                                           rec.getVoFullPackage());
                    bcObjNode.setAttribute("voInstanceName",
                                           rec.getVoInstanceName());
                    bcObjNode.setAttribute("voUsageFetchSize",
                                           rec.getVoUsageFetchSizeInAm());
                    bcObjNode.setAttribute("voDefaultFetchSize",
                                           rec.getVoDefaultFetchSize());


                    recordNode.appendChild(bcObjNode);
                    componentsNode.appendChild(recordNode);
                }
            }

            fileNode.appendChild(componentsNode);
            errorRecordsNode.appendChild(fileNode);

        }
        return errorRecordsNode;
    }

    private Element writeRecordsUnderRoot(Document doc,String rootNodeName,
                                      HashMap<String, ArrayList<TuningAnalysisRecord>> recordMap) {
        if (recordMap == null || recordMap.size() <= 0) {
            return null;
        }
        Element recordsRootNode = doc.createElement(rootNodeName);
        Set<String> keySet = recordMap.keySet();
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

            ArrayList<TuningAnalysisRecord> errorrecordsList = recordMap.get(filePath);
            if (errorrecordsList != null && errorrecordsList.size() > 0) {
                Iterator<TuningAnalysisRecord> recordIter =
                    errorrecordsList.iterator();
                while (recordIter.hasNext()) {
                    Element recordNode = doc.createElement("component");
                    TuningAnalysisRecord rec = recordIter.next();
                    recordNode.setAttribute("dataBindingsPath",
                                            rec.getCpxFilePath());
                    recordNode.setAttribute("pageDefPath",
                                            rec.getPageDefFilePath());
                    recordNode.setAttribute("type", "table");
                    recordNode.setAttribute("autoHeightRows",
                                            rec.getAutoHeightRows());
                    recordNode.setAttribute("diagnostics",
                                            rec.getDiagnostic());

                    Element iteratorNode = doc.createElement("iterator");
                    iteratorNode.setAttribute("name",
                                              rec.getPageDefIteratorName());
                    iteratorNode.setAttribute("rangeSize",
                                              rec.getIteratorRangeSize());
                    recordNode.appendChild(iteratorNode);

                    Element bcObjNode = doc.createElement("BCObject");
                    bcObjNode.setAttribute("amPath", rec.getAmPath());
                    bcObjNode.setAttribute("voFullPackage",
                                           rec.getVoFullPackage());
                    bcObjNode.setAttribute("voInstanceName",
                                           rec.getVoInstanceName());
                    bcObjNode.setAttribute("voUsageFetchSize",
                                           rec.getVoUsageFetchSizeInAm());
                    bcObjNode.setAttribute("voDefaultFetchSize",
                                           rec.getVoDefaultFetchSize());


                    recordNode.appendChild(bcObjNode);
                    componentsNode.appendChild(recordNode);
                }
            }

            fileNode.appendChild(componentsNode);
            recordsRootNode.appendChild(fileNode);

        }
        return recordsRootNode;
    }

    private void writeOutputXml(HashMap<String, ArrayList<TuningAnalysisRecord>> mapToWrite,
                                HashMap<String, ArrayList<TuningAnalysisRecord>> multipleBcUsages,
                                HashMap<String, ArrayList<TuningAnalysisRecord>> errorRecords,
                                String xmlFileName) {
        String ROOT_NODE = "voTuningAnalysis";

        Document doc = createXmlDocument();
        Element rootNode = doc.createElement(ROOT_NODE);

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

            ArrayList<TuningAnalysisRecord> recordsList =
                mapToWrite.get(filePath);
            if (recordsList != null && recordsList.size() > 0) {
                Iterator<TuningAnalysisRecord> recordIter =
                    recordsList.iterator();
                while (recordIter.hasNext()) {
                    Element recordNode = doc.createElement("component");
                    TuningAnalysisRecord rec = recordIter.next();
                    recordNode.setAttribute("type", "table");
                    recordNode.setAttribute("autoHeightRows",
                                            rec.getAutoHeightRows());
                    recordNode.setAttribute("tableFetchSize",
                                            rec.getUiTagFetchSize());
                    recordNode.setAttribute("fixStatus", rec.getFixStatus());
                    recordNode.setAttribute("fixComment",
                                            rec.getFixStatusComment());

                    Element iteratorNode = doc.createElement("iterator");
                    iteratorNode.setAttribute("name",
                                              rec.getPageDefIteratorName());
                    iteratorNode.setAttribute("rangeSize",
                                              rec.getIteratorRangeSize());
                    String newRangeSize = rec.getIteratorRangeSizeNewValue();
                    if (!isEmpty(newRangeSize)) {
                        iteratorNode.setAttribute("newRangeSize",
                                                  newRangeSize);
                    }
                    recordNode.appendChild(iteratorNode);

                    Element bcObjNode = doc.createElement("BCObject");
                    bcObjNode.setAttribute("amName", rec.getAmPath());
                    bcObjNode.setAttribute("voPath", rec.getVoPath());
                    bcObjNode.setAttribute("voInstanceName",
                                           rec.getVoInstanceName());
                    bcObjNode.setAttribute("voUsageFetchSize",
                                           rec.getVoUsageFetchSizeInAm());
                    bcObjNode.setAttribute("voDefaultFetchSize",
                                           rec.getVoDefaultFetchSize());

                    String newVoFetchSize = rec.getVoUsageFetchSizeNewValue();
                    if (!isEmpty(newVoFetchSize)) {
                        bcObjNode.setAttribute("voUsageNewFetchSize",
                                               newVoFetchSize);
                    }
                    String defaultFetchSizeNewVal =
                        rec.getVoDefaultFetchSizeNewValue();
                    if (!isEmpty(defaultFetchSizeNewVal)) {
                        bcObjNode.setAttribute("vodefaultFetchSizeNewValue",
                                               defaultFetchSizeNewVal);
                    }

                    recordNode.appendChild(bcObjNode);
                    componentsNode.appendChild(recordNode);
                }
            }

            fileNode.appendChild(componentsNode);
            rootNode.appendChild(fileNode);

        }


        Set<String> multiplebcKeys = multipleBcUsages.keySet();
        Iterator<String> multiplebcKeyIter = multiplebcKeys.iterator();
        Element bcsnode = doc.createElement("multipleBCs");
        while (multiplebcKeyIter.hasNext()) {
            // create the ui file node
            String bcKey = multiplebcKeyIter.next();
            ArrayList<TuningAnalysisRecord> bcRecords =
                multipleBcUsages.get(bcKey);
            if (bcRecords != null && bcRecords.size() > 0) {
                Iterator<TuningAnalysisRecord> bcRecordIter =
                    bcRecords.iterator();
                while (bcRecordIter.hasNext()) {
                    Element bcRecord = doc.createElement("BC");
                    TuningAnalysisRecord rec = bcRecordIter.next();
                    bcRecord.setAttribute("bcKey", bcKey);
                    bcRecord.setAttribute("fixStatus", rec.getFixStatus());
                    bcRecord.setAttribute("fixComment",
                                          rec.getFixStatusComment());
                    Element iteratorNode = doc.createElement("iterator");
                    iteratorNode.setAttribute("name",
                                              rec.getPageDefIteratorName());
                    iteratorNode.setAttribute("rangeSize",
                                              rec.getIteratorRangeSize());
                    String newRangeSize = rec.getIteratorRangeSizeNewValue();
                    if (!isEmpty(newRangeSize)) {
                        iteratorNode.setAttribute("newRangeSize",
                                                  newRangeSize);
                    }
                    bcRecord.appendChild(iteratorNode);

                    Element bcObjNode = doc.createElement("BCObject");
                    bcObjNode.setAttribute("amName", rec.getAmPath());
                    bcObjNode.setAttribute("voInstanceName",
                                           rec.getVoInstanceName());
                    bcObjNode.setAttribute("voUsageFetchSize",
                                           rec.getVoUsageFetchSizeInAm());
                    bcObjNode.setAttribute("voDefaultFetchSize",
                                           rec.getVoDefaultFetchSize());

                    String newVoFetchSize = rec.getVoUsageFetchSizeNewValue();
                    if (!isEmpty(newVoFetchSize)) {
                        bcObjNode.setAttribute("voUsageNewFetchSize",
                                               newVoFetchSize);
                    }
                    String defaultFetchSizeNewVal =
                        rec.getVoDefaultFetchSizeNewValue();
                    if (!isEmpty(defaultFetchSizeNewVal)) {
                        bcObjNode.setAttribute("vodefaultFetchSizeNewValue",
                                               defaultFetchSizeNewVal);
                    }
                    bcRecord.appendChild(bcObjNode);
                    bcsnode.appendChild(bcRecord);
                }
            }
        }
        rootNode.appendChild(bcsnode);

        // write the error records
        Element errorRecordsNode = writeErrorRecords(doc, errorRecords);
        rootNode.appendChild(errorRecordsNode);

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
            Transformer xformer =
                TransformerFactory.newInstance().newTransformer();
            xformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            System.out.println("Exception while writing xml output");
        } catch (TransformerException e) {
            System.out.println("Exception while writing xml output");
        }
    }
}
