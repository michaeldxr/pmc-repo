package oracle.apps.logs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.AngryBirdsDumper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.RegExScanner;
import oracle.apps.rup2scans.getUserGuidScanner;
import oracle.apps.utility.JoesBaseClass;

public class NativeLoggersScan extends JoesBaseClass {
    boolean bDebug = false;
    // static boolean dumpToAngryBirds = false;
    //String adfLoggerRegEx = "ADFLogger\\.create";
    String adfLoggerRegEx = "ADFLogger\\.create";
    Pattern adfLoggerPattern = Pattern.compile(adfLoggerRegEx, Pattern.DOTALL);

    String javaLoggerRegEx = "\\bLogger\\.getLogger";
    public Pattern javaLoggerPattern =
        Pattern.compile(javaLoggerRegEx, Pattern.DOTALL);
    ArrayList<RegExScanner.RegExScanResult> scanResults =
        new ArrayList<RegExScanner.RegExScanResult>();
    static int numFilesLookedAt = 0;
    static final String PROP_AB_DUMP = "dumpToAB";
    static final String PROP_AB_DUMP_TABLE = "abTable";
    static final String PROP_AB_DUMP_FAMILY = "abFamily";

    public NativeLoggersScan() {
        super(CRAWL_TYPE.JAVA);
    }

    public static void main(String[] args) throws Exception {

        //        String s = "Logger.getLogger(meis";
        //        NativeLoggersScan scanner1= new NativeLoggersScan();
        //        Matcher m = scanner1.javaLoggerPattern.matcher(s);
        //        if(m.find())
        //        {
        //            System.out.println("Found match: " + m.group());
        //            }
        Date startTime = new Date();
        CRAWL_TYPE scanFor = CRAWL_TYPE.JAVA;
        if (args == null || args.length < 1 || isEmpty(args[0])) {
            System.out.println("Usage: loggerScanner.sh <path to scan> ");
            System.exit(1);
        }

        String crawlDir = args[0];
        System.out.println(" ====================================== \n NativeLoggerScanner: START \n ====================================== \n");
        System.out.println("Scanning directory: " + crawlDir);
        NativeLoggersScan scanner = new NativeLoggersScan();
        scanner.crawlDirectory(crawlDir, false);
        System.out.println(" [DONE] NativeLoggerScanner: ");

        scanner.printResults();
        RegExScanner.writeResultsToExcel(scanner.scanResults,
                                         "loggerScan.xlsx");
        System.out.println("Total files looked at: " + numFilesLookedAt);
        Date endTime = new Date();
        String dumpToAbirds = System.getProperty(PROP_AB_DUMP);
        String dumpFam = System.getProperty(PROP_AB_DUMP);
        String dumpTag = System.getProperty(PROP_AB_DUMP_TABLE);
        if (dumpToAbirds != null &&
            dumpToAbirds.trim().equalsIgnoreCase("true")) {
            System.out.println("Dumping to AB.. YOU BETTER BE AUTHORIZED TO DO THIS!!");
            // csvfile
            String family = "ALL";
            String dumpTable = "CODESCAN_RESULTS2";
            if (!isEmpty(dumpFam))
                family = dumpFam.trim();
            if (!isEmpty(dumpTag))
                dumpTable = dumpTag.trim();

            scanner.writeResultsToCsv(scanner.scanResults, "loggerScan.csv");
            /*AngryBirdsDumper abDumper =
                new AngryBirdsDumper("loggerScan.csv", true);
            abDumper.dumpCsvToAngryBirdsDB(false, family, "Logger", dumpTable,"Prototype");*/
        }
        System.out.println(" Time taken (s): " +
                           ((endTime.getTime() - startTime.getTime()) / 1000));
    }

    public void printResults() {
        if (scanResults == null || scanResults.size() < 1) {
            System.out.println("No results");
            return;
        }
        Iterator<RegExScanner.RegExScanResult> resultIter =
            scanResults.iterator();
        while (resultIter.hasNext()) {
            RegExScanner.RegExScanResult r = resultIter.next();
            System.out.println("\n ==> File: " + r.getFileName());
            System.out.println(" Line Number: " + r.getLineNumber() + " : " +
                               r.getMatchingLine());

        }
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    @Override
    protected String getSummaryReport() {
        return null;
    }

    @Override
    protected String getSummaryReportSubject() {
        return null;
    }

    @Override
    protected void processFile(File fName, boolean bDoADE) {
        String absPath = fName.getAbsolutePath();
        if (absPath.contains("/dist/"))
            return;
        if (absPath.contains("/proxy/"))
            return;
        if (absPath.contains("Service/"))
            return;
        if (absPath.contains("/jaxWsProxy/")|| absPath.contains("/webProxy/") || absPath.contains("/client/") )
            return;
        // Exemption - the connector server files should not participate in this logging scan since they are files that are downloaded to the client and run on the client. 
        // Their logging levels are controlled by a seperate logging.xml file
        if(absPath.contains("fusionapps/crm/components/crmLibrary/zcc/connector/server/"))
            return;
        if(absPath.contains("fusionapps/crm/components/crmLibrary/zcc/connector/core/"))
            return;
        // Exemption - internal fsm code
        if(absPath.contains("fsm/dev/"))
            return;
        
        String name = fName.getName();
        if (name.endsWith("Service.java"))
            return;
        if (bDebug)
            System.out.println("Processing File: " + fName);

        numFilesLookedAt++;
        try {
            RegExScanner regExScanner = new RegExScanner(fName, "java");
            ArrayList<RegExScanner.RegExScanResult> adfLoggerResults =
                regExScanner.scanFor(adfLoggerPattern);
            if (adfLoggerResults != null)
                scanResults.addAll(adfLoggerResults);
            ArrayList<RegExScanner.RegExScanResult> javaloggerResults =
                regExScanner.scanFor(javaLoggerPattern);
            if (javaloggerResults != null)
                scanResults.addAll(javaloggerResults);

        } catch (Exception ex) {
            logException("Exception in RegExScanner ", ex);
        }

    }

    public void writeResultsToCsv(ArrayList<RegExScanner.RegExScanResult> results,
                                  String newCsvName) throws Exception {

        FileWriter fw = new FileWriter(newCsvName);
        BufferedWriter bw = new BufferedWriter(fw);


        int cnt = 0;
        bw.write("FAMILY,MODULE,PRODUCT,FILENAME,LABEL,ISSUETYPE,SUB_ISSUE,DESCRIPTION \n");
        // HashSet<String> exemptions = getExemptionHash("PlSqlAutonomousTxn_Exemptions.txt");
        Iterator<RegExScanner.RegExScanResult> resultIter = results.iterator();
        while (resultIter.hasNext()) {

            RegExScanner.RegExScanResult r = resultIter.next();
            String filePath = r.getFilePath();


            StringBuffer linebuf = new StringBuffer();
            linebuf.append(r.getFamily()).append(",");
            String mod = FamilyModuleHelper.getModule(r.getFilePath());
            linebuf.append(mod).append(",");
            linebuf.append(r.getProduct()).append(",");
            linebuf.append(FamilyModuleHelper.getRelativeFileName(r.getFilePath())).append(",");
            linebuf.append(FamilyModuleHelper.getLabelInfo(r.getFilePath())).append(",");
            linebuf.append("LOGGER,");
            linebuf.append("Java code should not create Loggers,");
            linebuf.append(String.format("Line:%s %s", r.getLineNumber(),
                                         r.getMatchingLine()));
            bw.write(linebuf.toString() + "\n");

        }
        if (bw != null)
            bw.close();
    }

}
