package oracle.apps.rup2scans;

//import com.sun.org.apache.xpath.internal.axes.IteratorPool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;

import java.io.InputStream;

import java.net.URL;

import java.util.ArrayList;

import java.util.HashSet;
import java.util.Iterator;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.AngryBirdsDumper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.ZOverallIntBase;
import oracle.apps.helpers.parsers.PlsqlFileParser;
import oracle.apps.scanresults.ABScanResult;
import oracle.apps.utility.JoesBaseClass;

/**
 * Scans for all plsql blocks that use autonomous transactions and checks to see if there is a commit or delete
 * in every execution path
 *
 * Usage: [-DdumpToAB=true/false -DabTable=CODESCAN_RESULTS2 -DabFamily=ALL] plsqlAutonomous.sh <crawlDir>
 */
public class PlsqlAutonomousTxnScanner extends ZOverallIntBase{
    static int numberOfFilesProcessed = 0;
    static int numberOfFilesHavingAtleastOneAuto = 0;
    static int numberOfAutonomousTxns = 0;

    
    static final String ISSUE = "PlSql_AutonomousTransaction";

    static final String RESULTSFILE="PlSqlAuto.csv";
    public PlsqlAutonomousTxnScanner() {
        super(ISSUE,JoesBaseClass.CRAWL_TYPE.PLSQL);
        setDoADE(false);
        setResultsFileName(RESULTSFILE);
        setExemptionFileName("PlSqlAutonomousTxn_Exemptions.txt");
        setBDebug(false);
    }


    public static void main(String[] args) throws Exception {
        if (args == null || args.length < 1 || isEmpty(args[0])) {
            System.out.println("Usage: plsqlAutonomous.sh <path to scan> ");
            System.exit(1);
        }

        String crawlDir = args[0];
        PlsqlAutonomousTxnScanner x = new PlsqlAutonomousTxnScanner();
        x.startScan(crawlDir);
    }

//    public static void printStats() {
//        System.out.println("==================================================");
//        System.out.println(" STATISTICS");
//        System.out.println("1. Total number of files processed: " + numberOfFilesProcessed);
//        System.out.println("2. Total number of files that have atleast one autonmous txn procedure: " + numberOfFilesHavingAtleastOneAuto);
//        System.out.println("3. Total number of procedures having autonmous txn : " + numberOfAutonomousTxns);
//        System.out.println("4. Number of Autonomous Txn Violations: " + scanResults.size());
//        System.out.println("==================================================");
//    }

  

    public String getOnlyLogicBlock(String procedureText) throws Exception {
        if (procedureText == null || procedureText.length() < 1)
            return "";
        String exceptionRegEx = "begin?.*exception\\s+";
        Pattern p = Pattern.compile(exceptionRegEx, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        Matcher m = p.matcher(procedureText);
        if (m.find())
            return m.group();
        return procedureText;
    }

    private boolean doesBlockCommitBeforeReturn(String block, PlsqlFileParser parser) throws Exception {
        if (isEmpty(block))
            return true;

        String[] blockCode = getContentsFromString(block);
        if (blockCode == null)
            return true;

        int len = blockCode.length;
        // Keep a stack of the commits found at every level.
        boolean foundCommit = false;
        String currentConditonalStmt = "";
        for (int i = 0; i < len; i++) {

            String s = blockCode[i];
            if (parser.willStatementReturn(s)) {
                // If there is a return, without a seen commit, then it is an error
                if (!foundCommit)
                    return false;
            } else if (parser.hasCommitOrRollback(s)) {
                foundCommit = true;
            }

        }

        return foundCommit;
    }

    private String[] getContentsFromString(String str) {
        if (str == null)
            return null;
        String[] contents = str.split("\n");
        return contents;
    }

  
   

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    @Override
    protected void doProcessFile(File fName) {
        String absPath = fName.getAbsolutePath();
        String subIssue_Logic="LogicBlock";
        String subIssue_Exception="ExceptionBlock";
        if (bDebug)
            System.out.println("Processing: " + absPath);

        try{
        PlsqlFileParser plsqlParser = new PlsqlFileParser(absPath);
        ArrayList<String> procedures = plsqlParser.getAllAutonomousProceduresAndFunctions();
        if (procedures == null || procedures.size() < 1)
            return;
        numberOfFilesHavingAtleastOneAuto++;
        Iterator<String> procedureIter = procedures.iterator();
        while (procedureIter.hasNext()) {
            numberOfAutonomousTxns++;
            String procedure = procedureIter.next();
            String procedureName = plsqlParser.getNameFromBlock(procedure);
            if (bDebug)
                System.out.println("Processing: " + fName.getName() + " Procedure: " + procedureName);
            // Check if main block has no commit (keeping scan simple now without checking for multiple loops)
            String logicBlock = getOnlyLogicBlock(procedure);
            if (!doesBlockCommitBeforeReturn(logicBlock, plsqlParser)) {
                if (bDebug) {
                    System.out.println("===> Found Violation In Logic: \n --> File: " + fName.getName() + " Procedure: " + procedureName);
                }
                ABScanResult r = new ABScanResult(absPath);
                r.setIssue(ISSUE);
                r.setSubIssue(subIssue_Logic);
                r.setDescription(String.format("Procedure/Function : %s does not commit/rollback in all possible execution paths",procedureName));
                r.setProduct(FamilyModuleHelper.getPlSqlProduct(absPath));
                r.setModule("plsql");
                if(!isExemption(getExemptionKey(fName, procedureName, subIssue_Logic)))
                    results.add(r);

            }
            // check if exception block has no rollback. Keeping scan simple for now
            String exceptionBlock = plsqlParser.getExceptionblockFromProcedure(procedure);
            if (!doesBlockCommitBeforeReturn(exceptionBlock, plsqlParser)) {
                if (bDebug) {
                    System.out.println("===> Found Violation In Exception: \n --> File: " + fName.getName() + " Procedure: " +
                                       procedureName);
                }
                ABScanResult r = new ABScanResult(absPath);
                r.setIssue(ISSUE);
                r.setSubIssue(subIssue_Exception);
                r.setDescription(String.format("Procedure/Function : %s does not commit/rollback in all possible execution paths",procedureName));
                r.setProduct(FamilyModuleHelper.getPlSqlProduct(absPath));
                r.setModule("plsql");
                if(!isExemption(getExemptionKey(fName, procedureName, subIssue_Exception)))
                    results.add(r);
                
            }
        }
        }catch(Exception ex){
            ex.printStackTrace();
            }

    }

private String getExemptionKey(File f, String procedureName, String subIssue)
{
    String fname = f.getName();
    String key=fname+"##"+subIssue+"##"+procedureName;
    return key.trim();
    }

//   
//
//    public static void writeResultsCsv(String outFile) throws Exception {
//        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
//        writer.write("Family, Product, FileName, FilePath, Procedure Name, Type \n");
//        if (scanResults != null) {
//            Iterator<ScanResult> resultIter = scanResults.iterator();
//            while (resultIter.hasNext()) {
//                ScanResult r = resultIter.next();
//                writer.write(r.getFamily() + "," + r.getProduct() + "," + r.getFileName() + "," + r.getFilePath() + "," +
//                             r.getProcedureName() + "," + r.getViolationType() + "\n");
//
//            }
//        }
//        writer.close();
//    }


    public HashSet<String> getExemptionHash(String fileName)  {
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
                if (line.trim().startsWith("#"))
                    continue;
                exemptions.add(line);
            }
            br.close();
            return exemptions;
        }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return exemptions;
    }

    public void convertToAngryBirdsCsv(String currentCsvName, String newCsvName) throws Exception {

        FileWriter fw = new FileWriter(newCsvName);
        BufferedWriter bw = new BufferedWriter(fw);

        BufferedReader originalCsvReader = new BufferedReader(new FileReader(new File(currentCsvName)));
        String line = "";
        int cnt = 0;

        HashSet<String> exemptions = getExemptionHash("PlSqlAutonomousTxn_Exemptions.txt");
        while ((line = originalCsvReader.readLine()) != null) {
            cnt++;
            // Skip header
            if (cnt == 1) {
                bw.write("FAMILY,MODULE,PRODUCT,FILENAME,LABEL,ISSUETYPE,SUB_ISSUE,DESCRIPTION \n");
                continue;
            }
            String[] parts = line.split(",");
            if (parts == null || parts.length < 6) {
                System.out.println("Invalid format: " + line);
                continue;
            }
            String filePath = parts[3];
            String procedureName = parts[4];
            String violationType = parts[5];
            // family,module,proudct,filename,label,
            String fileNameInfo = FamilyModuleHelper.getFileNameInfo(filePath);

            String description = "Procedure/Function: " + procedureName + " does not commit/rollback in all its possible execution paths";

            String relativeFileName = FamilyModuleHelper.getRelativeFileName(filePath);
            if (!isEmpty(relativeFileName)) {
                String key = relativeFileName + "##" + violationType + "##" + procedureName;
                if (exemptions.contains(key))
                    continue;
            }
            String lineToWrite = fileNameInfo + ISSUE + "," + violationType + "," + description + "\n";
            bw.write(lineToWrite);

        }

        if (bw != null)
            bw.close();
        if (originalCsvReader != null)
            originalCsvReader.close();
    }


}
