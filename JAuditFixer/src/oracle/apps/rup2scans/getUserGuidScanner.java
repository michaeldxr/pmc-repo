package oracle.apps.rup2scans;

import java.io.File;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import oracle.apps.OverlappingJarContentReporter;
import oracle.apps.helpers.RegExScanner;
import oracle.apps.utility.JoesBaseClass;

public class getUserGuidScanner extends JoesBaseClass {
    boolean bDebug = true;
    String regEx = "ADFContext\\.getCurrent\\(\\)\\.getSecurityContext\\(\\)\\.getUserProfile\\(\\).getGUID\\(\\)";
    Pattern regExPattern = Pattern.compile(regEx, Pattern.DOTALL);
    ArrayList<RegExScanner.RegExScanResult> scanResults = new ArrayList<RegExScanner.RegExScanResult>();

    public getUserGuidScanner() {
        super(CRAWL_TYPE.JAVA);
    }

    public static void main(String[] args) throws Exception {
        CRAWL_TYPE scanFor = CRAWL_TYPE.JAVA;
        if (args == null || args.length != 1 || isEmpty(args[0])) {
            System.out.println("Usage: userGuidScanner.sh <path to scan> ");
            System.exit(1);
        }

        String crawlDir = args[0];
        System.out.println(" ====================================== \n UserGuidScanner: START \n ====================================== \n");
        System.out.println("Scanning directory: " + crawlDir);
        getUserGuidScanner x = new getUserGuidScanner();
        x.crawlDirectory(crawlDir, false);
        System.out.println(" [DONE] UserGuidScanner: ");
        System.out.println(x.getSummaryReport());
        x.printResults();
        RegExScanner.writeResultsToExcel(x.scanResults,"userGuidResults.xlsx");
    }

    public void printResults() {
        if (scanResults == null || scanResults.size() < 1) {
            System.out.println("No results");
            return;
        }
        Iterator<RegExScanner.RegExScanResult> resultIter = scanResults.iterator();
        while (resultIter.hasNext()) {
            RegExScanner.RegExScanResult r = resultIter.next();
            System.out.println("\n ==> File: " + r.getFileName());
            System.out.println(" Line Number: " + r.getLineNumber() + " : " + r.getMatchingLine());

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
        if (bDebug)
            System.out.println("Processing File: " + fName);

        try {
            RegExScanner regExScanner = new RegExScanner(fName, "java");
            ArrayList<RegExScanner.RegExScanResult> reslts = regExScanner.scanFor(regExPattern);
            if (reslts != null)
                scanResults.addAll(reslts);
        } catch (Exception ex) {
            logException("Exception in RegExScanner ", ex);
        }

    }
}
