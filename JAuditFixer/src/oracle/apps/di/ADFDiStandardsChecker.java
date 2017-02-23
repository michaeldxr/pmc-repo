package oracle.apps.di;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import java.util.Iterator;

import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.di.checker.BindingsChecker;
import oracle.apps.di.checker.ButtonChecker;
import oracle.apps.di.checker.CheckerResult;
import oracle.apps.di.checker.ClientCompoentChecker;
import oracle.apps.di.checker.DIGenericStandardsChecker;
import oracle.apps.di.checker.FSMIntegrationChecker;
import oracle.apps.di.checker.LocalizationChecker;
import oracle.apps.di.checker.NamingStandardsChecker;
import oracle.apps.di.checker.StatusViewerChecker;
import oracle.apps.di.checker.TableChecker;
import oracle.apps.di.checker.WorkbookChecker;
import oracle.apps.helpers.AngryBirdsDumper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.helpers.ZOverallIntBase;

import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * TODOS:
 * 1. There seems to be an error with the metadata in the ADFDI_METADATA sheet and what is actually used.
 * One such example is for some tables the tableids are present in the metadata sheet against Componentxml but they are not really used in the spreadsheet
 * We should filter out all tables under ComponentXml (Column1). Only tables under Table(Column1) must be used.
 * For now we have registered exemptions for those cases, but ideally we should fix the script to handle those cases
 */
public class ADFDiStandardsChecker {
  static String standardRules =
    FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
    "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/di/config/Rules_R8_R9_R10.xml";
  static String usage =
    "Usage: standardsChecker.sh <PathToScan> [<rules.xml>]";
  ArrayList<DIGenericStandardsChecker> checkers = null;
  boolean bDebug = false ;  
  
  //HashSet<String> rules = new HashSet<String>();
  boolean spreadsheetDecodeReqd = false;

  HashMap<String, ArrayList<CheckerResult>> checkerResults =
    new HashMap<String, ArrayList<CheckerResult>>();
  BufferedWriter exceptionWriter = null;
  HashSet<String> filesWithErrors = new HashSet<String>();

  HashMap<String, HashSet<String>> rulesEnforced =
    new HashMap<String, HashSet<String>>();

  static boolean writeExceptions = false;
  static boolean writeFailedParsedComponents = true;
  static boolean printResultsAtEnd = true;
  static int numberOfExelsLookedAt = 0;

  private static String runmode;
  private static String series;
  private static int release=8; //default release version

  //private String filePath;
  
  static String overallOutFile = "diResults.csv";
  // static String ISSUE = "ADFDi Standards";
  static String filterListFilePath = "";
  static String EXEMPTIONS = FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") + "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/exemptions/ADFdi_Exemptions.txt";
  

  
  //    static String EXEMPTIONS =
  //        "/scratch/zcurrimb/view_storage/zcurrimb_fatoolsjardep/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/exemptions/ADFdi_Exemptions.txt";

  public ADFDiStandardsChecker(HashMap<String, HashSet<String>> codesEnforced) {
    super();

    ADFDiWorkbook.setBDebug(bDebug);
    SharedStringsParser.setBDebug(bDebug);

    rulesEnforced = codesEnforced;
    initializeCheckers(rulesEnforced);
    if (bDebug)
      printListOfCheckers();

    spreadsheetDecodeReqd = doesCodesNeedSpreadsheetDecode();
    try {
      exceptionWriter = new BufferedWriter(new FileWriter("Exceptions.log"));
    } catch (Exception ex) {
    }
  }

  /**
   * ADFDi Standards Checker
   * This class is the main entry point for enforcing all ADFDi standards check.
   * It operates out of a Rules.xml file (Default location is in ade, but can be overriden via the command line).
   * The Rules.xml file contains a list of all the standards to enforce for ADFDi.
   *
   * Usage: ADFDiStandardsChecker.sh <path to code> [<path to Rules.xml>]
   *
   * For AB Dump:
   * -DdumpToAB=true -DabFamily=ALL -DabTable=CODESCAN_RESULTS2
   *
   * Output:
   * diResults_<issue>.csv -> contains the list of results by issue type
   * Exceptions.log -> Exceptions during parsing the spreadsheet
   * FilesWithErrors.txt -> Filepaths that resulted in parsing errors including the metadata name that failed parsing
   * checkerExceptions.txt -> Exceptions when running the checker
   * Author: Zeesha Currimbhoy
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {

    scan(args);

  }

  public static void scan(String[] args) throws Exception {
    
    String sCrawlDir = "";
    String mode = "all";
    runmode = System.getProperty("mode");
    
    // To support premerge option. List of explicit files to run the checkers on
    HashSet<String> filterByList = getFilterByList();
    
    if (runmode == null){
      sCrawlDir = args[0];
      
      //it will be invoked by LGR model
      if(args.length >= 5 && !isEmpty(args[1]) && args[2]!=null && args[2].trim().length()>0){
        series = args[1];
        overallOutFile = args[2] +"/"+overallOutFile;  
        release = Integer.valueOf(args[3]);
        standardRules = args[4];
      }
      
      //it will be invoked by the Local model
      if(args.length==2){
        String label = args[1];
        int pos = label.lastIndexOf("_");
        setReleaseInfo(label.substring(0, pos));
      }
    } else if (runmode.contains("LRG")){
      setReleaseInfo(args[0].trim());
      String label = LRGUtil.getLatestLabel(series);
      sCrawlDir = LRGUtil.getCrawlDir(series, null, label, "ADFdi");
      
    } else if (runmode.equals("premerge") && !isEmpty(filterListFilePath)){
        mode = "scanList";
        String label = args[1];
        int pos = label.lastIndexOf("_");
        setReleaseInfo(label.substring(0, pos));
    }
    
    HashMap<String, HashSet<String>> codesEnforced = getRuleCoesEnforced();

//    if (args.length > 1 && !isEmpty(args[1]))
//      standardRules = args[1];
    
    if (!doesFileExist(standardRules))
      System.out.println("Rules file does not exist: " + standardRules);
    
    System.out.println("Rules: " + standardRules);
    System.out.println("==================================================");

    ADFDiStandardsChecker adfdiStandardsChecker =
      new ADFDiStandardsChecker(codesEnforced);
    Date startRun = new Date();
    if (mode.equals("all")) {
      System.out.println("Running DI Standards Check on path: " + sCrawlDir);
      adfdiStandardsChecker.runStandardsChecker(sCrawlDir);
    } else if (mode.equals("scanList")) {
      System.out.println("Running for specified list of files, will filter by crawlDir:" +
                         sCrawlDir);
      adfdiStandardsChecker.runForListFilterByPath(filterByList, sCrawlDir);
    }
    
    adfdiStandardsChecker.doAdditionalCheckerProcessing();
    HashMap<String, Integer> issueStats = adfdiStandardsChecker.writeResults();
    Date endRunTime = new Date();

    System.out.println("\n========================================");
    System.out.println("Number of excels looked at: " + numberOfExelsLookedAt);
    System.out.println(adfdiStandardsChecker.getAdditionalStats(issueStats));
    // System.out.println("Number of files with some form of parsing error: " + adfdiStandardsChecker.filesWithErrors.size());
    long timeTakenseconds =
      (endRunTime.getTime() - startRun.getTime()) / (1000);
    if (timeTakenseconds > 60) {
      System.out.println("Time Taken (min): " + (timeTakenseconds / 60));
    } else {
      System.out.println("Time Taken (s): " + timeTakenseconds);
    }
    System.out.println("========================================\n");
  }
  
  public static void setReleaseInfo(String seriesArg){
    series = seriesArg;
    String ret = FamilyModuleHelper.getRelease(series);
      try {
          release = Integer.parseInt(ret);
      } catch (NumberFormatException e) {
          e.printStackTrace();
      }
    if(release==11){
        standardRules =
                FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
                "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/di/config/Rules_R11.xml";
    }
    else if(release==12){
        standardRules =
                FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
                "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/di/config/Rules_R12.xml";
    }
    else if(release>=13){
        standardRules =
                FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
                "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/di/config/Rules_R13.xml";
    }  
    
  }

  public String getAdditionalStats(HashMap<String, Integer> issueStats) {
    StringBuffer sb = new StringBuffer();
    sb.append("\nTotal number of files having one or more errors: " +
              filesWithErrors.size() + "\n");
    if (issueStats != null && issueStats.size() > 0) {
      Iterator<String> issueIter = issueStats.keySet().iterator();
      while (issueIter.hasNext()) {
        String issue = issueIter.next();
        if (isEmpty(issue))
          continue;
        Integer cnt = issueStats.get(issue);
        sb.append(String.format("\nTotal number of violations for issue: %s detected as: %d",
                                issue, cnt));
      }
    }
    return sb.toString();
  }

  private void runForListFilterByPath(HashSet<String> listOffilesToRunAgainst,
                                      String crawlPathFilter) throws Exception {

    String adeViewRoot =
      FamilyModuleHelper.getViewRootFromPath(crawlPathFilter);
    Iterator<String> lifeOfFilesIter = listOffilesToRunAgainst.iterator();
    String relativeCrawlDirectoryPath =
      FamilyModuleHelper.getRelativeFileName(crawlPathFilter);
    if (isEmpty(relativeCrawlDirectoryPath)) {
      System.out.println("Relative crawl directory path is empty.. This is not allowed");
      return;
    }

    while (relativeCrawlDirectoryPath.startsWith("/"))
      relativeCrawlDirectoryPath = relativeCrawlDirectoryPath.substring(1);

    while (lifeOfFilesIter.hasNext()) {
      String relativePath = lifeOfFilesIter.next();
      String absPath = adeViewRoot + "/" + relativePath;
      // filter by crawl directory relative
      if (relativePath.contains(relativeCrawlDirectoryPath)) {
        processFileExplicitly(absPath);
      }
    }
  }

  protected void processFileExplicitly(String path) {
    int tmp = path.indexOf("[");
    if(tmp !=-1){
        path = path.substring(0, tmp);
    }

    if (path.contains(".ade_path") || path.contains("/classes/"))
      return;

    File f = new File(path);
    if (f == null || !f.exists()) {
      System.out.println("File does not exist: " + path);
      return;
    }
    if (bDebug)
      System.out.println("Processing: " + path);

    runStandardsCheckerOnFile(f);


  }

  public static HashSet<String> getFilterByList() throws Exception {
    HashSet<String> filterByList = new HashSet<String>();
    String strFilterFile = System.getProperty("filterByList");
    if (isEmpty(strFilterFile))
      return filterByList;
    File f = new File(strFilterFile);
    if (f == null || !f.exists())
      throw new Exception("FilterFile does not exist: " + strFilterFile);

    try {
      filterListFilePath = f.getAbsolutePath();
      BufferedReader br = new BufferedReader(new FileReader(f));
      String line = "";
      while ((line = br.readLine()) != null) {
        if (!isEmpty(line))
          filterByList.add(line.trim());
      }
    } catch (Exception ex) {
    }

    return filterByList;

  }

  public HashMap<String, Integer> writeResults() throws Exception {
    HashMap<String, Integer> issuestats = writeResults("diResults");
    if (exceptionWriter != null)
      exceptionWriter.close();
    writeFilesWithErrors("FilesWithErrors.txt");
    writeCheckerExceptions("checkerExceptions.txt");

    if (runmode != null && runmode.trim().equals("LRG"))
      dumpToAB(getIssueFiles(), series, false);
    if (runmode != null && runmode.trim().equals("LRG_prototype"))
      dumpToAB(getIssueFiles(), series, true);
    return issuestats;

  }

  public void dumpToAB(HashMap<String, String> issueFiles, String series,
                       boolean prototype) throws Exception {
    if (issueFiles == null || issueFiles.size() < 1)
      return;

    Iterator<String> issueIter = issueFiles.keySet().iterator();
    while (issueIter.hasNext()) {
      String issue = issueIter.next();
      if (isEmpty(issue))
        continue;
      String fileName = issueFiles.get(issue);
      AngryBirdsDumper dumper = new AngryBirdsDumper(fileName, true);
      dumper.dumpCsvToAngryBirdsDB(series, issue, prototype);
    }
  }

  public void writeCheckerExceptions(String path) throws Exception {
    BufferedWriter bw = new BufferedWriter(new FileWriter(path));
    Iterator<DIGenericStandardsChecker> checkerIter = checkers.iterator();
    while (checkerIter.hasNext()) {
      DIGenericStandardsChecker c = checkerIter.next();
      HashMap<String, ArrayList<String>> checkerExceptions = c.getExceptions();
      Set<Map.Entry<String, ArrayList<String>>> entrySet =
        checkerExceptions.entrySet();
      Iterator<Map.Entry<String, ArrayList<String>>> entryIter =
        entrySet.iterator();
      while (entryIter.hasNext()) {
        Map.Entry<String, ArrayList<String>> entry = entryIter.next();
        String file = entry.getKey();
        ArrayList<String> exceptios = entry.getValue();
        Iterator<String> excIter = exceptios.iterator();
        while (excIter.hasNext()) {
          String exc = excIter.next();
          bw.write(String.format("File: %s \n Exception: %s \n---------------------------\n",
                                 file, exc));
        }
      }


    }
    if (bw != null)
      bw.close();
  }

  public void writeFilesWithErrors(String path) throws Exception {
    BufferedWriter bw = new BufferedWriter(new FileWriter(path));
    Iterator<String> strIter = filesWithErrors.iterator();
    while (strIter.hasNext()) {
      String s = strIter.next();
      bw.write("\n" +
          s);
    }
    if (bw != null)
      bw.close();
  }

  public void writeExceptions(String path, ArrayList<String> exceptions) {
    if (exceptions == null || exceptions.size() < 1)
      return;
    Iterator<String> exIter = exceptions.iterator();
    try {
      exceptionWriter.write("\nSpreadsheet Path: " + path);
      while (exIter.hasNext()) {
        String e = exIter.next();
        exceptionWriter.write("\n\n======================================================");

        exceptionWriter.write("\nException: \n" +
            e);
        exceptionWriter.write("\n======================================================");
      }
    } catch (Exception ex2) {
    }
  }


  public void runCheckersOnFile(File f) {
    ArrayList<String> exceptions = new ArrayList<String>();
    try {
      String absolutePath = f.getAbsolutePath();
      if (bDebug)
        System.out.println("Running checkers on file: " + absolutePath);

      numberOfExelsLookedAt++;
      ArrayList<CheckerResult> resultsForFile = new ArrayList<CheckerResult>();
      ADFDiWorkbook workbook = null;
      if (spreadsheetDecodeReqd) {
        workbook = ADFDiWorkbook.getDIWorkbook(absolutePath, writeExceptions);
        if (workbook != null &&
            ((workbook.parseExceptions != null && workbook.parseExceptions.size() >
              0) ||
             (workbook.listOfFailedParsedComponents != null && workbook.listOfFailedParsedComponents.size() >
              0))) {
          //No need to keep track of all parse exception stacks they are very long and not very useful
          //exceptions.addAll(workbook.parseExceptions);
          filesWithErrors.add(absolutePath);

        }
        if (writeFailedParsedComponents) {
          writeFailedComponents(absolutePath,
                                workbook.getListOfFailedParsedComponents());
        }
      }

      if (workbook.getSharedStrings() == null ||
          workbook.getSharedStrings().size() < 1) {
        // No point running checkers if there are no shared stirngs.. means something is wrong in parsing this workbook
        filesWithErrors.add(absolutePath);
        return;
      }

              
      Iterator<DIGenericStandardsChecker> checkerIter = checkers.iterator();
      while (checkerIter.hasNext()) {
        DIGenericStandardsChecker checkr = checkerIter.next();
          
          //It's only used for the Fin and Project  in R13.      
          if( (checkr instanceof ClientCompoentChecker )){        
            if( !( release >= 13 && ( absolutePath.contains("fusionapps/fin/" )==true  ||  absolutePath.contains("fusionapps/prj/")==true ) ) ){
                continue;
            }
          }
          
        //It's only used for the Fin and Project in R12 and all fusion family in R13.
        if(checkr instanceof ButtonChecker || checkr instanceof StatusViewerChecker){
            if(release ==12 && absolutePath.contains("fusionapps/fin/")==false && absolutePath.contains("fusionapps/prj/")==false){
                  continue;
            }
                         
        }
        ArrayList<CheckerResult> resultsForRule =
          checkr.check(absolutePath, workbook, true);
        if (resultsForRule != null && resultsForRule.size() > 0)
          resultsForFile.addAll(resultsForRule);
      }


//                  if (bDebug) {
//                      if (resultsForFile != null && resultsForFile.size() > 0) {
//                          System.out.println("For file: " + absolutePath + " there are " + resultsForFile.size() + " number of violations");
//                          Iterator<CheckerResult> resultIter = resultsForFile.iterator();
//                          while (resultIter.hasNext()) {
//                              CheckerResult r = resultIter.next();
//                              System.out.println(String.format("===> Code: %s , Description: %s", r.getCheckerCode(), r.getDescription()));
//                          }
//                      }
//                  }
      addToGlobalResults(absolutePath, resultsForFile);

    } catch (Exception ex) {
      System.out.println("Exception while running checker on file: " +
                         f.getAbsolutePath());
    }
  }

  public void doAdditionalCheckerProcessing() {
    if (checkers == null || checkers.size() < 1)
      return;

    Iterator<DIGenericStandardsChecker> checkerIter = checkers.iterator();
    while (checkerIter.hasNext()) {
      DIGenericStandardsChecker checker = checkerIter.next();
      if (checker instanceof WorkbookChecker) {
        WorkbookChecker wcheck = (WorkbookChecker)checker;
        if (wcheck.isWorkbookIdCheck()) {
          HashMap<String, ArrayList<CheckerResult>> violations =
            wcheck.getWorkbookIdViolationsByFile();
          if (violations != null && violations.size() > 0) {
            //addToGlobalResults(fileName, results);
            Iterator<String> fileIter = violations.keySet().iterator();
            while (fileIter.hasNext()) {
              String filePath = fileIter.next();
              ArrayList<CheckerResult> fileResults = violations.get(filePath);
              addToGlobalResults(filePath, fileResults);
            }
          }
        }
        break;
      }
    }
  }

  public void runIntegrationCheckOnFile(File f) {

    if (!rulesEnforced.containsKey("I"))
      return;
    //        if (bDebug)
    //            System.out.println("Running integration checker on file: " + f.getAbsolutePath());
    String absPath = f.getAbsolutePath();
    FSMIntegrationChecker checker = new FSMIntegrationChecker();
    ArrayList<CheckerResult> results =
      checker.checkJsffForFSMIntegrationIssue(absPath);
    if (results == null || results.size() < 1)
      return;
    addToGlobalResults(absPath, results);
  }

  public void writeFailedComponents(String absPath,
                                    ArrayList<String> failedComponents) {
    if (failedComponents == null || failedComponents.size() < 1)
      return;
    StringBuffer sb = new StringBuffer();
    Iterator<String> compIter = failedComponents.iterator();
    while (compIter.hasNext()) {
      String comp = compIter.next();
      sb.append(comp + "#");
    }
    try {
      exceptionWriter.write(absPath + "," + sb.toString() + "\n");
    } catch (Exception e) {
    }
  }

  /**
   * Do any of the rules need to decode and parse the spreadsheet.
   * Doing a check will help us avoid doing unnecessary decoding.
   * @return
   * true/false
   */
  private boolean doesCodesNeedSpreadsheetDecode() {
    Set<String> ruleCodeCategorys = rulesEnforced.keySet();

    Iterator<String> ruleCode = ruleCodeCategorys.iterator();
    while (ruleCode.hasNext()) {
      String cd = ruleCode.next();
      if (cd.equals("R") || cd.equals("T") || cd.equals("W") || cd.equals("B")|| cd.equals("C") || cd.equals("V"))
        return true;
    }
    return false;
  }

  protected void runStandardsChecker(String path) {
    if (path.contains(".ade_path") || path.contains("/classes/"))
      return;

    File folder = new File(path);
    File[] listOfFiles = folder.listFiles();

    if (listOfFiles == null)
      return;

    for (int i = 0; i < listOfFiles.length; i++) {
      String sName = listOfFiles[i].getAbsolutePath();
      if (sName.contains(".ade_path"))
        continue;
      if (listOfFiles[i].isFile()) {
        runStandardsCheckerOnFile(listOfFiles[i]);
      } else if (listOfFiles[i].isDirectory()) {
        if (isValidUiDirectory(sName)) {
          runStandardsChecker(listOfFiles[i].getAbsolutePath());
        }
      } else {
        System.out.println("ERROR:  node is neither file or directory: " +
                           listOfFiles[i]);
      }
    }

  }

  protected void runStandardsCheckerOnFile(File file) {
    String absPath = file.getAbsolutePath();
    if (excelfileOfInterest(absPath)) {
      runCheckersOnFile(file);
    } else if (rulesEnforced.containsKey("I") && pagefileOfInterest(absPath)) {
      runIntegrationCheckOnFile(file);
    }
  }

  public static HashMap<String, HashSet<String>> getRuleCoesEnforced() throws Exception {
    HashMap<String, HashSet<String>> ruleCodes =
      new HashMap<String, HashSet<String>>();
    
    XMLDocument rulesDoc = XMLParserHelper.getXMLDocument(standardRules);
    NodeList ruleNodes = rulesDoc.getElementsByTagName("Rule");
    int numRules = ruleNodes.getLength();

    for (int i = 0; i < numRules; i++) {
      Node ruleNode = ruleNodes.item(i);
      if (!XMLParserHelper.isNodeName(ruleNode, "Rule"))
        continue;
      String enforced =
        XMLParserHelper.getAttributeValue(ruleNode, "Enforced");
      if (enforced == null || !enforced.trim().equals("true"))
        continue;

      String cd = XMLParserHelper.getAttributeValue(ruleNode, "id");
      if (cd == null || cd.trim().equals(""))
        continue;
      String category = cd.substring(0, 1);
      addToMap(ruleCodes, category, cd);
    }
    return ruleCodes;
  }

  /**
   * Returns a map of the rule code and the associated short name for all the rules enforced
   * @return
   * @throws Exception
   */
  public static HashMap<String, String> getRuleCodeNameMap() throws Exception {
    HashMap<String, String> codeNames = new HashMap<String, String>();

    XMLDocument rulesDoc = XMLParserHelper.getXMLDocument(standardRules);
    NodeList ruleNodes = rulesDoc.getElementsByTagName("Rule");
    int numRules = ruleNodes.getLength();

    for (int i = 0; i < numRules; i++) {
      Node ruleNode = ruleNodes.item(i);
      if (!XMLParserHelper.isNodeName(ruleNode, "Rule"))
        continue;
      String enforced =
        XMLParserHelper.getAttributeValue(ruleNode, "Enforced");
      if (enforced == null || !enforced.trim().equals("true"))
        continue;

      String cd = XMLParserHelper.getAttributeValue(ruleNode, "id");
      if (cd == null || cd.trim().equals(""))
        continue;

      String shortName =
        XMLParserHelper.getAttributeValue(ruleNode, "shortCode");
      if (shortName == null || shortName.trim().equals(""))
        continue;

      codeNames.put(cd, shortName);
    }
    return codeNames;
  }

  public static void addToMap(HashMap<String, HashSet<String>> map, String key,
                              String valueToAdd) {
    if (map == null || key == null || valueToAdd == null)
      return;
    if (map.containsKey(key)) {
      HashSet<String> existingValues = map.get(key);
      existingValues.add(valueToAdd);
      map.put(key, existingValues);
    } else {
      HashSet<String> existingValues = new HashSet<String>();
      existingValues.add(valueToAdd);
      map.put(key, existingValues);
    }
  }


  public boolean excelfileOfInterest(String sName) {

    if (sName == null || sName.equals("") || sName.contains(".ade_path") ||
        sName.contains("/classes/"))
      return false;

    if (!isValidUiDirectory(sName))
      return false;

    // Only scan file under components
    if (!sName.contains("/components/"))
      return false;

    // Only scan excels under public_html, for published workbooks
    if (!sName.contains("/public_html/"))
      return false;

    if (sName.endsWith(".xlsx") || sName.endsWith(".xlsm"))
      return true;

    return false;

  }

  public boolean pagefileOfInterest(String sName) {

    if (sName == null || sName.equals(""))
      return false;
    if (!isValidUiDirectory(sName))
      return false;

    if (sName.endsWith(".jsff") || sName.endsWith(".jspx"))
      return true;
    return false;

  }

  public boolean isValidUiDirectory(String sName) {

    if (sName == null || sName.equals("") || sName.contains(".ade_path") ||
        sName.contains("/classes/"))
      return false;

    if (!FamilyModuleHelper.isPathOfInterest(sName))
      return false;

    if (sName.toLowerCase().contains("/model/") ||
        sName.toLowerCase().contains("/publicmodel/") ||
        sName.toLowerCase().contains("/protectedmodel/") ||
        sName.toLowerCase().contains("/dist/"))
      return false;

    return true;

  }

  public static boolean doesFileExist(String path) {
    if (isEmpty(path))
      return false;
    try {
      File f = new File(path);
      if (f != null && f.exists())
        return true;
    } catch (Exception ex) {
    }
    return false;
  }

  public static boolean isEmpty(String str) {
    if (str == null || str.trim().equals(""))
      return true;
    return false;
  }

  public void addToGlobalResults(String fileName,
                                 ArrayList<CheckerResult> results) {
    if (results == null || results.size() < 1)
      return;

    if (checkerResults.containsKey(fileName)) {
      ArrayList<CheckerResult> existingResults = checkerResults.get(fileName);
      existingResults.addAll(results);
      checkerResults.put(fileName, existingResults);
    } else {
      checkerResults.put(fileName, results);
    }
  }

  private String getFileNameForIssue(String issue, String prefix) {
    return prefix + "_" + issue + ".csv";
  }

  private BufferedWriter initializeWriterForIssue(String issue,
                                                  String fname) throws Exception {
    BufferedWriter rw = new BufferedWriter(new FileWriter(fname));
    rw.write("FAMILY,MODULE,PRODUCT,FILENAME,Label,ISSUETYPE,SUB_ISSUE,DESCRIPTION\n");
    return rw;
  }

  public HashMap<String, BufferedWriter> initializeWriters() {
    HashMap<String, BufferedWriter> issueWriters =
      new HashMap<String, BufferedWriter>();
    try {
      HashMap<String, String> codeNames = getRuleCodeNameMap();
      if (codeNames == null || codeNames.size() < 1)
        return issueWriters;
      Iterator<Map.Entry<String, String>> mapEntryIter =
        codeNames.entrySet().iterator();
      while (mapEntryIter.hasNext()) {
        Map.Entry<String, String> entry = mapEntryIter.next();
        String code = entry.getKey();
        String shortName = entry.getValue();
        String fName = shortName + ".csv";
        BufferedWriter rw = new BufferedWriter(new FileWriter(fName));
        rw.write("FAMILY,MODULE,PRODUCT,FILENAME,Label,ISSUETYPE,SUB_ISSUE,DESCRIPTION\n");
        issueWriters.put(code, rw);
      }
    } catch (Exception ex) {
    }
    return issueWriters;
  }

  public HashMap<String, String> getIssueFiles() {
    HashMap<String, String> issueFiles = new HashMap<String, String>();
    try {
      HashMap<String, String> codeNames = getRuleCodeNameMap();
      if (codeNames == null || codeNames.size() < 1)
        return issueFiles;
      Iterator<Map.Entry<String, String>> mapEntryIter =
        codeNames.entrySet().iterator();
      while (mapEntryIter.hasNext()) {
        Map.Entry<String, String> entry = mapEntryIter.next();
        // String code = entry.getKey();
        String shortName = entry.getValue();
        String fName = shortName + ".csv";
        issueFiles.put(shortName, fName);
      }
    } catch (Exception ex) {
    }
    return issueFiles;
  }

  public HashMap<String, ArrayList<String>> getExemptionList() {
    HashMap<String, ArrayList<String>> exemptions =
      new HashMap<String, ArrayList<String>>();
    try {
      File f = new File(EXEMPTIONS);
      if (f == null || !f.exists())
        return exemptions;
      BufferedReader br = new BufferedReader(new FileReader(f));
      String line = "";
      while ((line = br.readLine()) != null) {
        if (isEmpty(line))
          continue;
        if (line.startsWith("#"))
          continue;
        String[] parts = line.split("#");
        if (parts == null || parts.length < 4)
          continue;
        String code = parts[0];
        String filePath = parts[1];
        String descriptionRegEx = parts[2];
        ArrayList<String> list;
        if (exemptions.containsKey(filePath))
          list = exemptions.get(filePath);
        else
          list = new ArrayList<String>();


        list.add(code + "#" + descriptionRegEx);
        exemptions.put(filePath, list);


      }
      if (br != null)
        br.close();
    } catch (Exception ex) {
    }
    return exemptions;
  }

  public String getExemptionKey(String code, String fileName) {
    return code + "#" + fileName;
  }

  public boolean isExempted(HashMap<String, ArrayList<String>> exemptions,
                            String filePath, CheckerResult r) {
    if (exemptions == null || exemptions.isEmpty())
      return false;


    if (!exemptions.containsKey(filePath))
      return false;
    ArrayList<String> exemptionsForFile = exemptions.get(filePath);
    if (exemptionsForFile == null || exemptionsForFile.size() < 1)
      return false;
    Iterator<String> exemptionsPerFileIter = exemptionsForFile.iterator();
    while (exemptionsPerFileIter.hasNext()) {
      String e = exemptionsPerFileIter.next();
      if (isEmpty(e))
        continue;
      String[] parts = e.split("#", 2);
      if (parts == null || parts.length < 2)
        continue;
      String issueCode = parts[0];
      String descRegEx = parts[1];
      if (!issueCode.equals(r.getCheckerCode()))
        continue;

      String desc = r.getDescription();
      if (isEmpty(desc) || isEmpty(descRegEx))
        continue;
      try {
        Pattern p = Pattern.compile(descRegEx);
        Matcher m = p.matcher(desc);
        if (m.find())
          return true;
      } catch (Exception ex) {
      }
    }

    return false;
  }

  public HashMap<String, Integer> writeResults(String prefix) throws Exception {

    HashMap<String, ArrayList<String>> exemptions = getExemptionList();
    HashMap<String, BufferedWriter> issueWriters = initializeWriters();
    BufferedWriter overallWriter =
      new BufferedWriter(new FileWriter(overallOutFile));
    //FAMILY,MODULE,PRODUCT,FILENAME,Label,ISSUETYPE,SUB_ISSUE,DESCRIPTION
    overallWriter.write("FAMILY,MODULE,PRODUCT,FILENAME,Label,ISSUETYPE,SUB_ISSUE,DESCRIPTION\n");
    Set<Map.Entry<String, ArrayList<CheckerResult>>> entrySet =
      checkerResults.entrySet();
    Iterator<Map.Entry<String, ArrayList<CheckerResult>>> entrySetIter =
      entrySet.iterator();
    HashMap<String, Integer> issueStats = new HashMap<String, Integer>();
    while (entrySetIter.hasNext()) {
      Map.Entry<String, ArrayList<CheckerResult>> entry = entrySetIter.next();
      String filePath = entry.getKey();
      String fileNameInfo = FamilyModuleHelper.getFileNameInfo(filePath);


      ArrayList<CheckerResult> fileResults = entry.getValue();
      Iterator<CheckerResult> fileResultsIter = fileResults.iterator();
      while (fileResultsIter.hasNext()) {
        CheckerResult fresult = fileResultsIter.next();

        String checkerCode = fresult.getCheckerCode();
        if (isEmpty(checkerCode)) {
          System.out.println("Error : CheckerResult does not contain checkerCode: " +
                             checkerCode);
          continue;
        }
        if (!issueWriters.containsKey(checkerCode)) {
          System.out.println("Error : No writer fo code defined: " +
                             checkerCode);
          continue;
        }

        // Check if it is a registered exemption: issue#file->description contains regex
        String relativePath = FamilyModuleHelper.getRelativeFileName(filePath);

        if (!isEmpty(relativePath) && exemptions.containsKey(relativePath))
          if (isExempted(exemptions, relativePath, fresult))
            continue;

        BufferedWriter w = issueWriters.get(checkerCode);

        String resultStr =
          fileNameInfo + fresult.getIssue() + "," + fresult.getSubIssue() +
          "," + fresult.getDescription() + "\n";
        w.write(resultStr);
        overallWriter.write(resultStr);
        String issue = fresult.getIssue();
        if (issueStats.containsKey(issue))
          issueStats.put(issue, (issueStats.get(issue) + 1));
        else
          issueStats.put(issue, 1);
      }
    }

    if (issueWriters != null && issueWriters.size() > 0) {
      Iterator<String> issueIter = issueWriters.keySet().iterator();
      while (issueIter.hasNext()) {
        BufferedWriter w = issueWriters.get(issueIter.next());
        if (w != null)
          w.close();
      }
    }
    if (overallWriter != null)
      overallWriter.close();
    return issueStats;
  }

  public void initializeCheckers(HashMap<String, HashSet<String>> ruleCodesToEnforce) {
    checkers = new ArrayList<DIGenericStandardsChecker>();
    if (ruleCodesToEnforce == null || ruleCodesToEnforce.size() < 1)
      return;
    Set<String> categoryKeys = ruleCodesToEnforce.keySet();
    Iterator<String> categoryIter = categoryKeys.iterator();
    while (categoryIter.hasNext()) {
      String category = categoryIter.next();
      HashSet<String> codes = ruleCodesToEnforce.get(category);
      if (codes == null || codes.size() < 1)
        continue;
      if (category.trim().equals("R")) {
        checkers.add(new LocalizationChecker());

      } else if (category.trim().equals("N")) {
        NamingStandardsChecker chk = new NamingStandardsChecker();
        if (codes.contains("N_1"))
          chk.setRunN1(true);
        if (codes.contains("N_2"))
          chk.setRunN2(true);
        if (codes.contains("N_3"))
          chk.setRunN3(true);
        if (codes.contains("N_4"))
          chk.setRunN4(true);
        if (codes.contains("N_6"))
          chk.setRunN6(true);

        checkers.add(chk);
      } else if (category.trim().equals("T")) {
        TableChecker chk = new TableChecker();
        if (codes.contains("T_1"))
          chk.setRunT1(true);
        if (codes.contains("T_2"))
          chk.setRunT2(true);
        if (codes.contains("T_3"))
          chk.setRunT3(true);
        if (codes.contains("T_4"))
          chk.setRunT4(true);
        if (codes.contains("T_5"))
          chk.setRunT5(true);
        if (codes.contains("T_6"))
          chk.setRunT6(true);
        if (codes.contains("T_7"))
          chk.setRunT7(true);
        if (codes.contains("T_8")) {
          chk.setRunT8(true);
        }
        checkers.add(chk);

      } else if (category.trim().equals("W")) {
        WorkbookChecker chk = new WorkbookChecker();
        if (codes.contains("W_1"))
          chk.setWorkbookIdCheck(true);
        if (codes.contains("W_2"))
          chk.setWorkbookErrorsCheck(true);
        if (codes.contains("W_3"))
          chk.setWorkbookCustomizationCheck(true);
        checkers.add(chk);
      } else if (category.trim().equals("B")) {
        BindingsChecker chk = new BindingsChecker();

        checkers.add(chk);
      }else if (category.trim().equals("C")) {
          if (codes.contains("C_1")){
              ButtonChecker bt = new ButtonChecker();
              checkers.add(bt);              
          }
          // Work only after R13
          if( release >= 13 ){
              if (codes.contains("C_2")){
                  ClientCompoentChecker bt = new ClientCompoentChecker();
                  checkers.add(bt);
              }      
              
          }
      } else if (category.trim().equals("V")) {
         StatusViewerChecker sv = new StatusViewerChecker();
         checkers.add(sv);
      }
    }


  }

  public void printListOfCheckers() {
    System.out.println("==================List of Checker==================");
    Iterator<DIGenericStandardsChecker> checkerIter = checkers.iterator();
    while (checkerIter.hasNext()) {
      StringBuffer line = new StringBuffer();
      DIGenericStandardsChecker c = checkerIter.next();
      if (c instanceof LocalizationChecker) {
        line.append("LocalizationChecker");
      } else if (c instanceof NamingStandardsChecker) {
        line.append("NamingStandardsChecker");
        if (((NamingStandardsChecker)c).isRunN1())
          line.append(" RunN1= true ");
        if (((NamingStandardsChecker)c).isRunN2())
          line.append(" RunN2=true ");
        if (((NamingStandardsChecker)c).isRunN3())
          line.append(" RunN3=true ");
        if (((NamingStandardsChecker)c).isRunN4())
          line.append(" RunN4=true ");
        if (((NamingStandardsChecker)c).isRunN6())
          line.append(" RunN6=true ");
      } else if (c instanceof TableChecker) {
        line.append("TableChecker");
        if (((TableChecker)c).isRunT1()) {
          line.append(" RunT1=true ");
        }
        if (((TableChecker)c).isRunT2()) {
          line.append(" RunT2=true ");
        }
        if (((TableChecker)c).isRunT3()) {
          line.append(" RunT3=true ");
        }

      } else if (c instanceof WorkbookChecker) {
        line.append("WorkbookChecker");
        if (((WorkbookChecker)c).isWorkbookIdCheck()) {
          line.append(" runW1=true ");
        }
        if (((WorkbookChecker)c).isWorkbookErrorsCheck()) {
          line.append(" runW2=true ");
        }
        if (((WorkbookChecker)c).isWorkbookCustomizationCheck()) {
          line.append(" runW3=true ");
        }
      } else if (c instanceof BindingsChecker) {
        line.append("BindingsChecker");
      }
      System.out.println(line.toString());
    }
    if (rulesEnforced.containsKey("I"))
      System.out.println("FSM Integration checker");
    System.out.println("==================================\n");
  }
}
