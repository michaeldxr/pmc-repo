package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import oracle.apps.helpers.AngryBirdsDumper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.Mail;
import oracle.apps.helpers.XmlFixer;
import oracle.apps.idAnalyzer.ComponentDetails;
import oracle.apps.idAnalyzer.NoIdAnalyzer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FindClearedClauses {
    public static String ISSUE = "ViewCriteriaBlinds";

    public class ClearedClauseViolation {
        public String fileName = "";
        public String product = "";
        public String filePath = "";
        public String violationType = "";
        public String lineNumber = "";
        public String violatingLine = "";
        public String violationSnippet = "";
    }
    static boolean bDebug = false;
    boolean logOnlyNullLine = false;

    /**
     * Number of java files
     */
    static int m_javaCount = 0;
    static int m_numerExempted=0;
    public FindClearedClauses() {
        super();
    }

    private HashMap<String, ArrayList<ClearedClauseViolation>> map_whereAfterExecuteViolations =
        new HashMap<String, ArrayList<ClearedClauseViolation>>();
    private HashMap<String, ArrayList<ClearedClauseViolation>> map_possibleBlindQviolations =
        new HashMap<String, ArrayList<ClearedClauseViolation>>();


    public static void main(String[] args) throws Exception {

        // Make sure it is run with correct number of arguments
        if (args.length < 1 || args[0] == null) {
            System.out.println("Usage:  FindClearedWhereClause.sh <path to your code in an open ade view>");
            System.exit(1);
        }


        Date startDate = new Date();

        // get arguments
        String sCrawlDir = args[0];
        if (isEmpty(sCrawlDir)) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }

        boolean dumpToAngryBirds = false;
        // If using the angry birds dumper, user is advanced user, should supply all args
        String familyToDelete="";
        if (args.length > 2) {
            String strdump = args[1].trim();
            familyToDelete = args[2].trim().toUpperCase();
            // to encrypt it , so no one else will be able to trigger the dump... setting has to be truez :-)
            if (strdump != null && strdump.equals("truez"))
                dumpToAngryBirds = true;
        }
        

        System.out.println("Analyzing all files in directory:" + sCrawlDir);

        FindClearedClauses whereClauseAnalyzer = new FindClearedClauses();


        try {
            whereClauseAnalyzer.crawlDirectory(sCrawlDir);
        } catch (Exception ex) {
            System.out.println("Exception occurred: " + ex.getMessage());
        }
        if (bDebug) {
            System.out.println("=======Activation state error violations==============");
            whereClauseAnalyzer.printMap(whereClauseAnalyzer.map_whereAfterExecuteViolations);
            System.out.println("=======Possible Blind query violations==============");
            whereClauseAnalyzer.printMap(whereClauseAnalyzer.map_possibleBlindQviolations);
        }
        try {
            System.out.println("Writing output xml.. (File output.xml will be located in current directory)");
            whereClauseAnalyzer.writeOutputXml(whereClauseAnalyzer.map_possibleBlindQviolations,
                                               whereClauseAnalyzer.map_whereAfterExecuteViolations, "output.xml");

        } catch (Exception ex) {
            System.out.println("Exception in writing xml");
        }

        String mailSummary = whereClauseAnalyzer.printStatistics(startDate);

      /*  if (dumpToAngryBirds) {
            System.out.println("===> Dumping results of viewCriteriaBlinds to angry birds.. \n YOU BETTER BE AUTHORIZED TO DO THIS.. (COZ WE COME TO KNOW WHEN SOMEONE DUMPS THE RESULTS) !!! \n");
            String angryBirdsCsv = "viewCriteriaBlinds_AngryBirds";
            whereClauseAnalyzer.createAngryBirdsCsv(angryBirdsCsv);
            AngryBirdsDumper dumper = new AngryBirdsDumper(angryBirdsCsv, true);
            dumper.dumpCsvToAngryBirdsDB(false, familyToDelete, ISSUE, "CODESCAN_RESULTS_RUP2","V2");
            System.out.println("Total Number of files exempted: " + m_numerExempted);
            
            mailSummary += "\n Total Number of files exempted: " + m_numerExempted;
            //Mail.sendMail("zeesha.currimbhoy@oracle.com", mailSummary, "SCRIPT: ViewCriteriaBlinds - AB Scan Run");
            
        }*/

    }

    public void printMap(HashMap<String, ArrayList<ClearedClauseViolation>> mapToPrint) {
        if (mapToPrint == null) {
            return;
        }
        Set<String> keySet = mapToPrint.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            System.out.println("File: " + key);
            ArrayList<ClearedClauseViolation> list_values = mapToPrint.get(key);
            Iterator<ClearedClauseViolation> iterValues = list_values.iterator();
            while (iterValues.hasNext()) {
                ClearedClauseViolation violation = iterValues.next();
                String msg =
                    String.format("lineNum: %s, violatingLine: %s, violatingSnippet %s", violation.lineNumber, violation.violatingLine,
                                  violation.violationSnippet);
                System.out.println(msg);
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
    private void crawlDirectory(String crawlDir) {
        if (crawlDir.contains(".ade_path"))
            return;

        File folder = new File(crawlDir);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (isValidJavaFile(absFilePath)) {
                    m_javaCount++;

                    processJavaFile(listOfFiles[i]);
                }

            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidDirectory(dir)) {
                    crawlDirectory(dir);
                }

            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    public void processJavaFile(File file) {
        String absPath = file.getAbsolutePath();
        if(bDebug)
         System.out.println("Processing file: " + absPath);

        XmlFixer fileFixer = null;
        String fileContents = "";
        try {
            fileFixer = new XmlFixer(absPath);
            String[] fileContentsArray = fileFixer.convertFileToStringArray();
            fileContents = fileFixer.getFileContentsAsString(fileContentsArray, false);
            fileContents = removeSingleLineComments(fileContents);
            fileContents = removeMultilineComments(fileContents);
            if (!isEmpty(fileContents)) {

                findViolationsInJavaFile_modified(file, fileContents);
            } else {
                if (bDebug) {
                    System.out.println("File contents were empty.. returning.. " + absPath);
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception in file: Skipping..  " + absPath + " Exception: " + ex.getMessage());
            return;
        }


    }

    // might need to handle advance cases like brace within string

    public boolean hasMethodOpeningBraces(String line) {
        if (!isEmpty(line) && line.contains("{"))
            return true;
        return false;
    }

    public boolean hasEndiingBraces(String line) {
        if (!isEmpty(line) && line.contains("}"))
            return true;
        return false;
    }

    public boolean containsNakedWhereClause(String str) {
        if (!isEmpty(str) && str.contains("setWhereClause(null)"))
            return true;
        return false;
    }

    public boolean containsExecuteQuery(String str) {
        if (!isEmpty(str) && str.contains("executeQuery()"))
            return true;
        return false;
    }

    public boolean containsAnyTypeSetWhereClause(String str) {
        if (!isEmpty(str) && str.contains("setWhereClause("))
            return true;
        return false;
    }

    public String getTypeofClearedClause(String line) {
        if (containsAnyTypeSetWhereClause(line))
            return "setWhereClause";
        if (containsAnyTypeSetWhereParams(line))
            return "setWhereClauseParams";
        if (containsAnyTypeSetVC(line))
            return "setApplyViewCriteriaName";
        return "";
    }

    public boolean containsSetWhereClauseWithBindVar(String str) {
        Pattern p = Pattern.compile("setWhereClause\\(.*:.*\\)");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }


        return false;
    }

    public boolean containsAnyTypeSetVC(String str) {
        if (!isEmpty(str) && (str.contains("setApplyViewCriteriaName(") || str.contains("setApplyViewCriteriaNames(")))
            return true;
        return false;
    }

    public boolean containsAnyTypeSetWhereParams(String str) {
        if (!isEmpty(str) && str.contains("setWhereClauseParams("))
            return true;
        return false;
    }

    public boolean containsSetWhereClause(String str) {
        if (!isEmpty(str) &&
            (str.contains("setWhereClause(") || str.contains("applyViewCriteria(") || str.contains("setApplyViewCriteriaName(") ||
             str.contains("setApplyViewCriteriaNames(") || str.contains("addWhereClause(")) &&
            !str.contains("setWhereClause(null)") && !str.contains("applyViewCriteria(null)") &&
            !str.contains("setApplyViewCriteriaName(null)") && !str.contains("setApplyViewCriteriaNames(null)"))
            return true;
        return false;
    }
    /*
    public boolean containsSetWhereClause1(String str) {
        if (!isEmpty(str) &&
            (str.contains("setWhereClause(") ) &&
            !str.contains("setWhereClause(null)") )
            return true;
        return false;
    }*/

    public boolean containsNakedWhereParamsClause(String str) {
        if (!isEmpty(str) && str.contains("setWhereClauseParams(null)"))
            return true;
        return false;
    }

    public boolean containsSetWhereClauseParams(String str) {
        if (!isEmpty(str) &&
            (str.contains("setWhereClauseParams(") || str.contains("setNamedWhereClauseParams(") || str.contains("setNamedWhereClauseParam(") ||
             str.contains("setWhereClauseParam(")) && !str.contains("setWhereClauseParams(null)") &&
            !str.contains("setNamedWhereClauseParams(null)") && !str.contains("setWhereClauseParam(null)"))
            return true;
        if (!isEmpty(str) && str.contains("setbind"))
            return true;
        return false;
    }

    public boolean containsNakedViewCriteria(String str) {
        if (!isEmpty(str) && str.contains("setApplyViewCriteriaNames(null)"))
            return true;
        return false;
    }

    public boolean containsSetviewCriteria(String str) {
        if (!isEmpty(str) &&
            (str.contains("setApplyViewCriteriaNames(") || str.contains("setApplyViewCriteriaName(") || str.contains("setWhereClause(")) &&
            !str.contains("setApplyViewCriteriaNames(null)") && !str.contains("setApplyViewCriteriaName(null)") &&
            !str.contains("setWhereClause(null)"))
            return true;
        return false;
    }

    public boolean isClassDeclaration(String str) {
        if (!isEmpty(str) && str.contains(" class "))
            return true;
        return false;
    }

    public int countNumberOfOpeningBracesInStr(String str) {

        if (isEmpty(str)) {
            return 0;
        }
        if (!str.contains("{"))
            return 0;

        Pattern p = Pattern.compile("\\{");
        Matcher m = p.matcher(str);
        int cnt = 0;
        while (m.find()) {
            cnt++;
        }
        return cnt;
    }

    public int countNumberOfNewLines(String str) {

        if (isEmpty(str)) {
            return 0;
        }
        if (!str.contains("\n"))
            return 0;

        Pattern p = Pattern.compile("\\n");
        Matcher m = p.matcher(str);
        int cnt = 0;
        while (m.find()) {
            cnt++;
        }
        return cnt;
    }

    public int countNumberOfClosingBracesInStr(String str) {

        if (isEmpty(str)) {
            return 0;
        }
        if (!str.contains("}"))
            return 0;

        Pattern p = Pattern.compile("\\}");
        Matcher m = p.matcher(str);
        int cnt = 0;
        while (m.find()) {
            cnt++;
        }
        return cnt;
    }

    private void recordAfterTextForExecuteQueryVOs(HashMap<String, StringBuffer> vosDoneExecuteQuery, String line) {
        if (vosDoneExecuteQuery.size() < 1)
            return;
        Set<Map.Entry<String, StringBuffer>> entrySet = vosDoneExecuteQuery.entrySet();
        Iterator<Map.Entry<String, StringBuffer>> entrySetIter = entrySet.iterator();
        while (entrySetIter.hasNext()) {
            entrySetIter.next().getValue().append(line);
        }

    }

    public void findViolationsInJavaFile_modified(File file, String fileContentsWithoutComments) throws Exception {
        String fileName = file.getName();
        String filePath = file.getAbsolutePath();

        // possible activation issues, keyed by the voName on which the issue is observed
        HashMap<String, ArrayList<ClearedClauseViolation>> possibleActivationIssues = new HashMap<String, ArrayList<ClearedClauseViolation>>();
        // Keep trace of the vos that have done an execute query and all the text that came after that execute query. Will be useful to record a snippet
        HashMap<String, StringBuffer> vosDoneExecuteQuery = new HashMap<String, StringBuffer>();

        //HashMap<String, String> tempPossibleActivationIssues = new HashMap<String, String>();
        BufferedReader fileReader = new BufferedReader(new StringReader(fileContentsWithoutComments));
        Integer lineNum = 0;
        if (fileReader == null)
            return;
        String line = null;
        boolean isInMethod = false;

      //  boolean executeQueryOccurred = false;
        // boolean possibleActivationIssue = false;
        //StringBuffer afterExecuteQuery = new StringBuffer();
        Integer whereAfterExecuteLineNumber = 0;


        boolean possibleWhereClauseViolation = false;
        StringBuffer violationString = new StringBuffer();
        Integer whereClauseViolationLineNumber = 0;
        String violatingLine_lonelyWhere = "";

        boolean possibleWhereClauseParamsViolation = false;
        StringBuffer violationParamsString = new StringBuffer();
        Integer whereClauseParamsLineNumber = 0;

        boolean possibleNullVC = false;
        StringBuffer violationNullVC = new StringBuffer();
       // Integer VCLineNumber = 0;

        int cntUnmatchedOpeningBrace = 0;
        boolean foundClassOpenBrace = false;
        boolean isClass = false;

        try {
            while ((line = fileReader.readLine()) != null) {
                lineNum++;
                // dont waste time on blank lines
                if (isEmpty(line)) {
                    continue;
                }

                if (isClassDeclaration(line)) {
                    isClass = true;
                }
                if (hasMethodOpeningBraces(line)) {
                    int numberOfOpeningBraces = countNumberOfOpeningBracesInStr(line);
                    // do not want to count the class opening brace
                    if (isClass && !foundClassOpenBrace) {
                        foundClassOpenBrace = true;
                    } else if (isInMethod) {
                        cntUnmatchedOpeningBrace = cntUnmatchedOpeningBrace + numberOfOpeningBraces;
                    } else {
                        isInMethod = true;
                        if (numberOfOpeningBraces > 1) {
                            cntUnmatchedOpeningBrace = numberOfOpeningBraces - 1;
                        }
                    }
                }

                if (containsExecuteQuery(line)) {
                    String voName = getVoInstancOnMethodCall(line, "executeQuery");
                    if (!isEmpty(voName)) {
                        // record the vo that has done an execute query (if it was already previously recorded, its ok, re initialize it. since its better if we record the smallest snippet)
                        vosDoneExecuteQuery.put(voName, new StringBuffer());
                        // If you see an execute query, its no longer an activation issue
                        if (possibleActivationIssues.containsKey(voName))
                            possibleActivationIssues.remove(voName);
                        //executeQueryOccurred = true;
                        //possibleActivationIssue = false;
                        //TODO: record after execute query for the individual vo
                        //afterExecuteQuery = new StringBuffer();
                        //tempPossibleActivationIssues = new HashMap<String, String>();
                    }
                }
                // If there is some vo that has done an execute query, we need to record the after query text
                recordAfterTextForExecuteQueryVOs(vosDoneExecuteQuery, line);

                //                if (executeQueryOccurred) {
                //                    afterExecuteQuery.append(line);
                //                }
                if ((containsAnyTypeSetWhereClause(line) || containsAnyTypeSetVC(line) || containsAnyTypeSetWhereParams(line)) &&
                    vosDoneExecuteQuery.size()>0) {
                    String typeOfClause = getTypeofClearedClause(line);
                    if (!isEmpty(typeOfClause)) {
                        String voInstanceName = getVoInstancOnMethodCall(line, typeOfClause);
                        if (vosDoneExecuteQuery.containsKey(voInstanceName)) {
                            // This is a possible violation: executeQuery -> clearedClause
                            ClearedClauseViolation objViolation = new ClearedClauseViolation();
                            objViolation.fileName = fileName;
                            objViolation.filePath = filePath;
                            objViolation.product = getProductName(filePath);
                            objViolation.violationType = "ActivationError";
                            objViolation.lineNumber = lineNum.toString();
                            StringBuffer afterQuery = vosDoneExecuteQuery.get(voInstanceName);
                            objViolation.violationSnippet = afterQuery.toString();
                            objViolation.violatingLine = line;
                            addPossibleActivationIssue(voInstanceName,objViolation,possibleActivationIssues);
                        }
                    }
                    //possibleActivationIssue = true;
                    //tempPossibleActivationIssues.put(lineNum.toString() + "#" + line, afterExecuteQuery.toString());
                    // set where clause violation
                    //whereAfterExecuteLineNumber=lineNum;

                    //  addToMap(map_whereAfterExecuteViolations, fileName, "Line Number "+lineNum+""+afterExecuteQuery.toString());
                }

                // setWhereClause(null) && no executeQuery before this line - possible blind query violation (if it is not followed by an execute query)
//                if (containsNakedWhereClause(line) && !executeQueryOccurred) {
//                    possibleWhereClauseViolation = true;
//                    whereClauseViolationLineNumber = lineNum;
//                    violatingLine_lonelyWhere = line;
//                }

//                if (possibleWhereClauseViolation && isInMethod) {
//                    if (containsExecuteQuery(line) || containsSetWhereClause(line)) {
//                        possibleWhereClauseViolation = false;
//                        violationString = new StringBuffer();
//                        violatingLine_lonelyWhere = "";
//                    } else {
//
//                        violationString.append(line);
//                    }
//                }

                if (hasEndiingBraces(line)) {
                    int cntEndBraces = countNumberOfClosingBracesInStr(line);
                    if (cntUnmatchedOpeningBrace > 0) {
                        cntUnmatchedOpeningBrace = cntUnmatchedOpeningBrace - cntEndBraces;
                    } else {
                        // if there is a violation - record it
//                        if (possibleWhereClauseViolation) {
//                            if (bDebug) {
//                                System.out.println("Found violation: " + violationString.toString());
//                            }
//                            ClearedClauseViolation objViolation = new ClearedClauseViolation();
//                            objViolation.fileName = fileName;
//                            objViolation.filePath = filePath;
//                            objViolation.product = getProductName(filePath);
//                            objViolation.violationType = "PossibleBlind";
//                            objViolation.lineNumber = whereClauseViolationLineNumber.toString();
//                            objViolation.violationSnippet = violationString.toString();
//                            objViolation.violatingLine = violatingLine_lonelyWhere;
//
//                            map_possibleBlindQviolations =
//                                    addToViolationMap(map_possibleBlindQviolations, filePath, objViolation);
//                            whereClauseViolationLineNumber = 0;
//                        }


    
  
                        if(possibleActivationIssues.size()>0){
                            int numberOfIssues = 0;
                            if (bDebug) {
                                System.out.println("Found possible activation violation issue : ");
                            }
                            Set<Map.Entry<String,ArrayList<ClearedClauseViolation>>> activationErrorsEntry =  possibleActivationIssues.entrySet();
                            Iterator<Map.Entry<String,ArrayList<ClearedClauseViolation>>> activationErrorIter = activationErrorsEntry.iterator();
                            while(activationErrorIter.hasNext())
                            {
                                Map.Entry<String,ArrayList<ClearedClauseViolation>> activationErrorMap = activationErrorIter.next();
                                ArrayList<ClearedClauseViolation> activationErrorList = activationErrorMap.getValue();
                                if(bDebug)
                                {
                                    System.out.println("For vo: " + activationErrorMap.getKey()+" Number of violations: " + activationErrorList.size() );
                                    numberOfIssues=numberOfIssues+activationErrorList.size();
                                }
                                Iterator<ClearedClauseViolation> activationErrorListIter =activationErrorList.iterator();
                                while(activationErrorListIter.hasNext())
                                {
                                    ClearedClauseViolation viol = activationErrorListIter.next();
                                    addToViolationMap(map_whereAfterExecuteViolations, filePath, viol);
                                    }
                            }
//                            Set<String> keys = tempPossibleActivationIssues.keySet();
//                            Iterator<String> keyIter = keys.iterator();
//                            while (keyIter.hasNext()) {
//                                String key = keyIter.next();
//                                String[] keyParams = key.split("#", 2);
//                                String lineNumObt = "";
//                                String violatingActivLine = "";
//                                if (keyParams != null && keyParams.length > 1) {
//                                    lineNumObt = keyParams[0];
//                                    violatingActivLine = keyParams[1];
//                                }
//                                String tempViolStrObt = tempPossibleActivationIssues.get(key);
//                                ClearedClauseViolation tempViolationObt = new ClearedClauseViolation();
//                                tempViolationObt.fileName = fileName;
//                                tempViolationObt.filePath = filePath;
//                                tempViolationObt.product = getProductName(filePath);
//                                tempViolationObt.violationType = "ActivationError";
//                                tempViolationObt.lineNumber = lineNumObt;
//                                tempViolationObt.violationSnippet = tempViolStrObt;
//                                tempViolationObt.violatingLine = violatingActivLine;
//                                map_whereAfterExecuteViolations =
//                                        addToViolationMap(map_whereAfterExecuteViolations, filePath, tempViolationObt);
//                            }

                           // VCLineNumber = 0;
                        }
                        // end of the method
                        isInMethod = false;
                        cntUnmatchedOpeningBrace = 0;
                        
                        // Clear sets
                        vosDoneExecuteQuery.clear();
                        possibleActivationIssues.clear();
                        
                       // executeQueryOccurred = false;

//                        violationString = new StringBuffer();
//                        violatingLine_lonelyWhere = "";
//                        violationParamsString = new StringBuffer();
//                        violationNullVC = new StringBuffer();
//                        afterExecuteQuery = new StringBuffer();
//                        tempPossibleActivationIssues = new HashMap<String, String>();

//                        possibleWhereClauseViolation = false;
//                        possibleWhereClauseParamsViolation = false;
//                        possibleNullVC = false;
//                        possibleActivationIssue = false;

//                        whereClauseParamsLineNumber = 0;
//                        whereClauseViolationLineNumber = 0;
//                        VCLineNumber = 0;
//                        whereAfterExecuteLineNumber = 0;

                    }
                }

            }
        } catch (Exception ex) {
            System.out.println("Excepion in find");
            throw new Exception("Exception while trying to find violatiosn in file", ex);
        }

    }

    public void addPossibleActivationIssue(String voName, ClearedClauseViolation violation,
                                           HashMap<String, ArrayList<ClearedClauseViolation>> listOfIssues) {
        if (listOfIssues.containsKey(voName)) {
            ArrayList<ClearedClauseViolation> existingList = listOfIssues.get(voName);
            existingList.add(violation);
            listOfIssues.put(voName, existingList);
        } else {
            ArrayList<ClearedClauseViolation> newList = new ArrayList<ClearedClauseViolation>();
            newList.add(violation);
            listOfIssues.put(voName, newList);
        }
    }

    private String getVoInstancOnMethodCall(String line, String methodCall) {
        if (isEmpty(line) || isEmpty(methodCall))
            return "";
        if (!line.contains(methodCall))
            return "";
        String identifier = "[0-9a-zA-Z_]+";
        String regEx = String.format("\\s+(%s)\\.%s", identifier, methodCall);
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(line);
        if (m.find()) {
            String voName = m.group(1);
            return voName;
        }
        return "";
    }

    public void findViolationsInJavaFile(File file, String fileContentsWithoutComments) throws Exception {
        String fileName = file.getName();
        String filePath = file.getAbsolutePath();
        HashMap<String, String> tempPossibleActivationIssues = new HashMap<String, String>();
        BufferedReader fileReader = new BufferedReader(new StringReader(fileContentsWithoutComments));
        Integer lineNum = 0;
        if (fileReader == null)
            return;
        String line = null;
        boolean isInMethod = false;

        boolean executeQueryOccurred = false;
        boolean possibleActivationIssue = false;
        StringBuffer afterExecuteQuery = new StringBuffer();
        Integer whereAfterExecuteLineNumber = 0;


        boolean possibleWhereClauseViolation = false;
        StringBuffer violationString = new StringBuffer();
        Integer whereClauseViolationLineNumber = 0;
        String violatingLine_lonelyWhere = "";

        boolean possibleWhereClauseParamsViolation = false;
        StringBuffer violationParamsString = new StringBuffer();
        Integer whereClauseParamsLineNumber = 0;

        boolean possibleNullVC = false;
        StringBuffer violationNullVC = new StringBuffer();
        Integer VCLineNumber = 0;

        int cntUnmatchedOpeningBrace = 0;
        boolean foundClassOpenBrace = false;
        boolean isClass = false;

        try {
            while ((line = fileReader.readLine()) != null) {
                lineNum++;
                // dont waste time on blank lines
                if (isEmpty(line)) {
                    continue;
                }

                if (isClassDeclaration(line)) {
                    isClass = true;
                }
                if (hasMethodOpeningBraces(line)) {
                    int numberOfOpeningBraces = countNumberOfOpeningBracesInStr(line);
                    // do not want to count the class opening brace
                    if (isClass && !foundClassOpenBrace) {
                        foundClassOpenBrace = true;
                    } else if (isInMethod) {
                        cntUnmatchedOpeningBrace = cntUnmatchedOpeningBrace + numberOfOpeningBraces;
                    } else {
                        isInMethod = true;
                        if (numberOfOpeningBraces > 1) {
                            cntUnmatchedOpeningBrace = numberOfOpeningBraces - 1;
                        }
                    }
                }

                if (containsExecuteQuery(line)) {
                    executeQueryOccurred = true;
                    possibleActivationIssue = false;
                    afterExecuteQuery = new StringBuffer();
                    tempPossibleActivationIssues = new HashMap<String, String>();
                }
                if (executeQueryOccurred) {
                    afterExecuteQuery.append(line);
                }
                if ((containsAnyTypeSetWhereClause(line) || containsAnyTypeSetVC(line) || containsAnyTypeSetWhereParams(line)) &&
                    executeQueryOccurred) {
                    possibleActivationIssue = true;
                    tempPossibleActivationIssues.put(lineNum.toString() + "#" + line, afterExecuteQuery.toString());
                    // set where clause violation
                    //whereAfterExecuteLineNumber=lineNum;

                    //  addToMap(map_whereAfterExecuteViolations, fileName, "Line Number "+lineNum+""+afterExecuteQuery.toString());
                }

                // setWhereClause(null) && no executeQuery before this line - possible blind query violation (if it is not followed by an execute query)
                if (containsNakedWhereClause(line) && !executeQueryOccurred) {
                    possibleWhereClauseViolation = true;
                    whereClauseViolationLineNumber = lineNum;
                    violatingLine_lonelyWhere = line;
                }

                if (possibleWhereClauseViolation && isInMethod) {
                    if (containsExecuteQuery(line) || containsSetWhereClause(line)) {
                        possibleWhereClauseViolation = false;
                        violationString = new StringBuffer();
                        violatingLine_lonelyWhere = "";
                    } else {

                        violationString.append(line);
                    }
                }

                if (hasEndiingBraces(line)) {
                    int cntEndBraces = countNumberOfClosingBracesInStr(line);
                    if (cntUnmatchedOpeningBrace > 0) {
                        cntUnmatchedOpeningBrace = cntUnmatchedOpeningBrace - cntEndBraces;
                    } else {
                        // if there is a violation - record it
                        if (possibleWhereClauseViolation) {
                            if (bDebug) {
                                System.out.println("Found violation: " + violationString.toString());
                            }
                            ClearedClauseViolation objViolation = new ClearedClauseViolation();
                            objViolation.fileName = fileName;
                            objViolation.filePath = filePath;
                            objViolation.product = getProductName(filePath);
                            objViolation.violationType = "PossibleBlind";
                            objViolation.lineNumber = whereClauseViolationLineNumber.toString();
                            objViolation.violationSnippet = violationString.toString();
                            objViolation.violatingLine = violatingLine_lonelyWhere;

                            map_possibleBlindQviolations =
                                    addToViolationMap(map_possibleBlindQviolations, filePath, objViolation);
                            whereClauseViolationLineNumber = 0;
                        }


                        if (possibleActivationIssue) {
                            if (bDebug) {
                                System.out.println("Found possible activation violation issue : (number of issues) " +
                                                   tempPossibleActivationIssues.size());
                            }
                            Set<String> keys = tempPossibleActivationIssues.keySet();
                            Iterator<String> keyIter = keys.iterator();
                            while (keyIter.hasNext()) {
                                String key = keyIter.next();
                                String[] keyParams = key.split("#", 2);
                                String lineNumObt = "";
                                String violatingActivLine = "";
                                if (keyParams != null && keyParams.length > 1) {
                                    lineNumObt = keyParams[0];
                                    violatingActivLine = keyParams[1];
                                }
                                String tempViolStrObt = tempPossibleActivationIssues.get(key);
                                ClearedClauseViolation tempViolationObt = new ClearedClauseViolation();
                                tempViolationObt.fileName = fileName;
                                tempViolationObt.filePath = filePath;
                                tempViolationObt.product = getProductName(filePath);
                                tempViolationObt.violationType = "ActivationError";
                                tempViolationObt.lineNumber = lineNumObt;
                                tempViolationObt.violationSnippet = tempViolStrObt;
                                tempViolationObt.violatingLine = violatingActivLine;
                                map_whereAfterExecuteViolations =
                                        addToViolationMap(map_whereAfterExecuteViolations, filePath, tempViolationObt);
                            }

                            VCLineNumber = 0;
                        }
                        // end of the method
                        isInMethod = false;
                        cntUnmatchedOpeningBrace = 0;
                        executeQueryOccurred = false;

                        violationString = new StringBuffer();
                        violatingLine_lonelyWhere = "";
                        violationParamsString = new StringBuffer();
                        violationNullVC = new StringBuffer();
                        afterExecuteQuery = new StringBuffer();
                        tempPossibleActivationIssues = new HashMap<String, String>();

                        possibleWhereClauseViolation = false;
                        possibleWhereClauseParamsViolation = false;
                        possibleNullVC = false;
                        possibleActivationIssue = false;

                        whereClauseParamsLineNumber = 0;
                        whereClauseViolationLineNumber = 0;
                        VCLineNumber = 0;
                        whereAfterExecuteLineNumber = 0;

                    }
                }

            }
        } catch (Exception ex) {
            System.out.println("Excepion in find");
            throw new Exception("Exception while trying to find violatiosn in file", ex);
        }

    }

    private ArrayList<String> getListFromMap(HashMap<String, String> mapOfValues) {
        if (mapOfValues == null) {
            return new ArrayList<String>();
        }
        ArrayList<String> listOfValues = new ArrayList<String>();
        Set<String> setKeys = mapOfValues.keySet();
        Iterator<String> keyIter = setKeys.iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            String val = mapOfValues.get(key);
            listOfValues.add("Line Number " + key + " " + val);
        }

        return listOfValues;
    }

    private HashMap<String, ArrayList<String>> addToMap(HashMap<String, ArrayList<String>> maptoAddTo, String key,
                                                        String value) {
        if (maptoAddTo == null) {
            maptoAddTo = new HashMap<String, ArrayList<String>>();
        }
        if (maptoAddTo.containsKey(key)) {
            ArrayList<String> list_values = maptoAddTo.get(key);
            list_values.add(value);
            maptoAddTo.put(key, list_values);
        } else {
            ArrayList<String> list_values = new ArrayList<String>();
            list_values.add(value);
            maptoAddTo.put(key, list_values);
        }
        return maptoAddTo;
    }

    private HashMap<String, ArrayList<ClearedClauseViolation>> addToViolationMap(HashMap<String, ArrayList<ClearedClauseViolation>> maptoAddTo,
                                                                                 String key, ClearedClauseViolation value) {
        if (maptoAddTo == null) {
            maptoAddTo = new HashMap<String, ArrayList<ClearedClauseViolation>>();
        }
        if (maptoAddTo.containsKey(key)) {
            ArrayList<ClearedClauseViolation> list_values = maptoAddTo.get(key);
            list_values.add(value);
            maptoAddTo.put(key, list_values);
        } else {
            ArrayList<ClearedClauseViolation> list_values = new ArrayList<ClearedClauseViolation>();
            list_values.add(value);
            maptoAddTo.put(key, list_values);
        }
        return maptoAddTo;
    }

    private HashMap<String, ArrayList<String>> addToMap(HashMap<String, ArrayList<String>> maptoAddTo, String key,
                                                        ArrayList<String> value) {
        if (maptoAddTo == null) {
            maptoAddTo = new HashMap<String, ArrayList<String>>();
        }
        if (maptoAddTo.containsKey(key)) {
            ArrayList<String> list_values = maptoAddTo.get(key);
            list_values.addAll(value);
            maptoAddTo.put(key, list_values);
        } else {
            ArrayList<String> list_values = new ArrayList<String>();
            list_values.addAll(value);
            maptoAddTo.put(key, list_values);
        }
        return maptoAddTo;
    }

    private String removeSingleLineComments(String fileContents) {
        String singleLineCommentsRE = "//.*$";
        Pattern pattern = Pattern.compile(singleLineCommentsRE, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(fileContents);

        return matcher.replaceAll("");
    }

    private String removeMultilineComments(String fileContents) {
        StringBuffer newFileContents = new StringBuffer();


        /*anything*/
        // reluctant quantifier because we want to match the first occurrence of the closing tag as opposed to the last
        String multiLineCommentsRE = "/\\*.*?\\*/";
        Pattern pattern = Pattern.compile(multiLineCommentsRE, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(fileContents);
        int startPos = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            //System.out.println("start: "+ start);
            // System.out.println("end: "+ end);
            String str = fileContents.substring(start, end);
            int numb = countNumberOfNewLines(str);
            int indexTrailingnewLine = fileContents.indexOf("\n", end);
            if (indexTrailingnewLine != -1) {
                String endAfterComment = fileContents.substring(end, indexTrailingnewLine);
                if (isEmpty(endAfterComment)) {
                    //    numb++;
                }
            }
            String replacement = "";
            for (int i = 0; i < numb; i++) {
                replacement = replacement + " \n ";
            }
            newFileContents.append(fileContents.substring(startPos, start));
            newFileContents.append(replacement);
            startPos = end;
            ///System.out.println(str);
            //newFileContents=newFileContents.replace(start, end, replacement);
        }
        newFileContents.append(fileContents.substring(startPos));
        return newFileContents.toString();
    }

    /**
     * Exclude all classes, no ship and test folders
     * @param absDirPath
     * @return
     */
    public boolean isValidDirectory(String absDirPath) {
        if (absDirPath.contains("/dbSchema/") || absDirPath.contains("/classes/")) {
            return false;
        }
        String lowerCasePath = absDirPath.toLowerCase();
        if (lowerCasePath.contains("/noship/") || lowerCasePath.contains("/test/") || lowerCasePath.contains("servicetest") ||
            lowerCasePath.contains("datasecuritytest") || lowerCasePath.contains("securitypublictest") ||
            lowerCasePath.contains("/publicservicetest") || lowerCasePath.contains("/publicmodeltest") ||
            lowerCasePath.contains("/protectedmodeltest") || lowerCasePath.contains("/publicuitest") ||
            lowerCasePath.contains("/uimodeltest") || lowerCasePath.contains("/structuretest") ||
            lowerCasePath.contains("/modeltest") || lowerCasePath.contains("/uitest") || lowerCasePath.contains("/testui") ||
            lowerCasePath.contains("/setest") || lowerCasePath.contains("/protecteduimodeltest") ||
            lowerCasePath.contains("/testviewcontroller") || lowerCasePath.contains("/flextest") ||
            lowerCasePath.contains("/uimodeler-launch-test") || lowerCasePath.contains("/publicuibasetest") ||
            lowerCasePath.contains("/uipickertest"))
            return false;

        return true;
    }

    public boolean isValidJavaFile(String absFilePath) {
        if (!isEmpty(absFilePath)) {
            if (absFilePath.contains("/test/") || absFilePath.contains("/modelTest/") || absFilePath.contains("/classes/")) {
                return false;
            }
            if (absFilePath.endsWith(".java")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public String printStatistics(Date startDate) {
        StringBuffer sb = new StringBuffer();
        // Print Statistics
        Date endDate = new Date();
        sb.append("=================================================\n");
        
        sb.append("Run Statistics\n");
        sb.append("Start Time: " + startDate.toString()+"\n");
        sb.append("End Time: " + endDate.toString()+"\n");
        long elapsedTime = endDate.getTime() - startDate.getTime();
        sb.append("Elapsed Time (sec): " + elapsedTime / 1000+"\n");
        sb.append("=================================================\n");

        sb.append("=================================================\n");
        sb.append("Total Number of java files analyzed: " + m_javaCount+"\n");
        
        
        if (map_whereAfterExecuteViolations != null) {
            sb.append("Total Number of files with activation state errors: " + map_whereAfterExecuteViolations.size()+"\n");
        }
        if (map_possibleBlindQviolations != null) {
            sb.append("Total Number of files with possible blind query: " + map_possibleBlindQviolations.size()+"\n");
        }
        sb.append("=================================================\n");
        String str = sb.toString();
        System.out.println(str);
        return str;

    }

    /**
     * Gets the product name from the file path
     *
     * @param uiFilePath
     * @return
     */
    public String getProductName(String uiFilePath) {
        String productName = "";
        Integer productIndex = 5;
        // file path: /ade/<view_name>/fusionapps/fin/components/ledger/gl
        if (uiFilePath.contains("/fusionapps/")) {
            Integer fusionappsIndex = uiFilePath.indexOf("/fusionapps/");
            String path_after_fusionapps = uiFilePath.substring(fusionappsIndex);
            if (!isEmpty(path_after_fusionapps)) {
                String[] parts = path_after_fusionapps.split("/");
                if (parts != null & parts.length > productIndex) {
                    return parts[productIndex];
                }
            }
        }
        return "";
    }

    private Element addMapToRootNode(Document doc, Element rootNode,
                                     HashMap<String, ArrayList<ClearedClauseViolation>> results) {

        String FILE_NODE = "file";
        String FILE_NAME_ATTR = "fileName";
        String FILE_PATH_ATTR = "filePath";
        String PER_FILE_VIOLATIONS = "NumberOfViolations";
        String VIOLATIONSSTRING = "Violations";
        String VIOLATIONNODE_STRING = "Violation";
        String VIOLATION_STRING = "ViolatingLine";
        String VIOLATION_LINENUM = "LineNum";
        String VIOLATION_SNIPPET = "Snippet";
        String VIOLATION_TYPE = "type";
        String PRODUCT = "product";

        String FAMILY = "family";
        Set<String> keySet = results.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {

            String filePath = keyIter.next();
            String fileName = "";
            int lastSlashIndex = filePath.lastIndexOf("/");
            if (lastSlashIndex != -1) {
                fileName = filePath.substring(lastSlashIndex + 1);
            }

            Element fileNode = doc.createElement(FILE_NODE);
            String family = FamilyModuleHelper.getFamily(filePath);
            String product = FamilyModuleHelper.getProduct(filePath);

            //String prod = getProductName(filePath);
            fileNode.setAttribute(FAMILY, family);
            fileNode.setAttribute(PRODUCT, product);
            fileNode.setAttribute(FILE_NAME_ATTR, fileName);
            fileNode.setAttribute(FILE_PATH_ATTR, filePath);

            Element violationsNode = doc.createElement(VIOLATIONSSTRING);

            ArrayList<ClearedClauseViolation> violationsList = results.get(filePath);
            if (violationsList != null && violationsList.size() > 0) {
                Iterator<ClearedClauseViolation> violationsIter = violationsList.iterator();
                while (violationsIter.hasNext()) {
                    ClearedClauseViolation violation = violationsIter.next();
                    Element violNode = doc.createElement(VIOLATIONNODE_STRING);

                    violNode.setAttribute(VIOLATION_TYPE, violation.violationType);
                    violNode.setAttribute(VIOLATION_LINENUM, violation.lineNumber);
                    // violNode.setAttribute(VIOLATION_STRING, violation.getViolatingLine());
                    Element snippetNode = doc.createElement(VIOLATION_SNIPPET);
                    snippetNode.setTextContent(violation.violationSnippet);
                    violNode.appendChild(snippetNode);
                    Element violatingLineNode = doc.createElement(VIOLATION_STRING);
                    violatingLineNode.setTextContent(violation.violatingLine);
                    violNode.appendChild(violatingLineNode);
                    violationsNode.appendChild(violNode);
                }
            }
            fileNode.appendChild(violationsNode);
            rootNode.appendChild(fileNode);

        }
        return rootNode;
    }

    private void writeOutputXml(HashMap<String, ArrayList<ClearedClauseViolation>> possibleBlindMap,
                                HashMap<String, ArrayList<ClearedClauseViolation>> whereAfterQueryMap, String xmlFileName) {
        String ROOT_NODE = "WhereClauseViolations";
        String FILE_NODE = "file";
        String FILE_NAME_ATTR = "fileName";
        String FILE_PATH_ATTR = "filePath";
        String PER_FILE_VIOLATIONS = "NumberOfViolations";
        String VIOLATIONSSTRING = "Violations";
        String VIOLATIONSTRING = "Violation";
        String VIOLATION_TYPE = "type";
        String PRODUCT = "product";
        Document doc = createXmlDocument();
        Element rootNode = doc.createElement(ROOT_NODE);

        rootNode = addMapToRootNode(doc, rootNode, possibleBlindMap);
        rootNode = addMapToRootNode(doc, rootNode, whereAfterQueryMap);

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

    // This method writes a DOM document to a file

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

    public void createAngryBirdsCsv(String fileName) throws Exception {
        // Exception format should be File##SubIssue##LineNumber
        FileWriter fw = new FileWriter(fileName);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("FAMILY,MODULE,PRODUCT,FILENAME,LABEL,ISSUETYPE,SUB_ISSUE,DESCRIPTION \n");
        HashSet<String> exemptions = getExemptionHash("ViewCriteriaBlinds_Exemptions.txt");

        HashMap<String, ArrayList<ClearedClauseViolation>> activationViolations = map_whereAfterExecuteViolations;
       // HashMap<String, ArrayList<ClearedClauseViolation>> blindViolations = map_possibleBlindQviolations;
        writeMapToAngryBirdsCsv(bw, activationViolations, exemptions);
       // writeMapToAngryBirdsCsv(bw, blindViolations, exemptions);
        if (bw != null)
            bw.close();
    }

    private void writeMapToAngryBirdsCsv(BufferedWriter bw, HashMap<String, ArrayList<ClearedClauseViolation>> mapToWrite,
                                         HashSet<String> exemptions) throws Exception {
        if (mapToWrite == null || mapToWrite.size() < 1)
            return;

        Set<String> keys = mapToWrite.keySet();
        Iterator<String> keyIter = keys.iterator();
        while (keyIter.hasNext()) {
            String path = keyIter.next();
            String fileNameRelative = FamilyModuleHelper.getRelativeFileName(path);
            String fileNameInfo = FamilyModuleHelper.getFileNameInfo(path);
            ArrayList<ClearedClauseViolation> listviolations = mapToWrite.get(path);
            Iterator<ClearedClauseViolation> listviolationsIter = listviolations.iterator();
            while (listviolationsIter.hasNext()) {
                ClearedClauseViolation v = listviolationsIter.next();
                String desc = "Line: " + v.lineNumber + " " + v.violatingLine;
                String lineToWrite = fileNameInfo + ISSUE + "," + v.violationType + "," + desc + "\n";
                String exemptionKey = fileNameRelative + "##" + v.violationType + "##" + v.violatingLine.trim() ;
                if (exemptions.contains(exemptionKey))
                {
                    m_numerExempted++;
                    continue;
                }
                bw.write(lineToWrite);
            }
        }

    }

    public HashSet<String> getExemptionHash(String fileName) {
       
        HashSet<String> exemptions = new HashSet<String>();
       
        try{        
        String filePath = AngryBirdsDumper.getExemptionFlie(fileName);
        if (!isEmpty(filePath)) {
            File f = new File(filePath);
            if (f == null || !f.exists())
                return exemptions;
            BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
            String line = "";
            while ((line = br.readLine()) != null) {
                if(isEmpty(line))
                    continue;
                
                if (line.trim().startsWith("#"))
                    continue;
                
                // Dont want to add the line number as a unique constraint since it is so volatile
                String[] parts = line.split("##");
                if(parts.length>=4)
                {
                    String exemptionLine=parts[0]+"##"+parts[1]+"##"+parts[3];
                    exemptions.add(exemptionLine);
                }
            }
            br.close();
            return exemptions;
        }
        return exemptions;
        }catch(Exception e){
            e.printStackTrace();
            return exemptions;
        }
    }
}
