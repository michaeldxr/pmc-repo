package oracle.apps.helpers;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileReader;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashSet;

import java.util.Iterator;

import oracle.apps.scanresults.ABScanResult;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.utility.LRGUtil;

/**
 * This extends on JoesBaseClass
 * Provides the following extensions:
 * 1. startScan
 *  a. checks systemProperty filterByList (PreMerge integration) to only run the scan on a select number of files
 *      if filterByList -> run scan only on file list
 *      else -> run on the entire crawl directory
 *  b. Writes the results to issue_Errors.csv
 *  c. If the ABDump properties are specified, dumps the csv to AngryBirds as well
 */
abstract public class ZOverallIntBase extends JoesBaseClass {
    
    public static String overallUsage = "seedReExtract.sh <crawlDirectory> [-DfilterByList=<listOfFilesToRunOn_asFile>]";
    
    int numberOfFilesLookedAt = 0;
    static String filterListFilePath = "";
    public ArrayList<ABScanResult> results = new ArrayList<ABScanResult>();
    HashSet<String> filterByList = new HashSet<String>();
   
    private static String runmode;
    private static String series;
    private static String sCrawlDir;
    
    String scanIssue = "";
    public boolean bDebug = false;

    String resultsFileName="";
    String exemptionFileName="";
    HashSet<String> exemptions = new HashSet<String>();
    public ZOverallIntBase(String issue, JoesBaseClass.CRAWL_TYPE crawlType) {
        super(crawlType);
        this.scanIssue = issue;

    }

    protected String printSummary() {
        StringBuffer sb = new StringBuffer();
        sb.append("==================== Summary ====================== ");
        sb.append("\n---> Number of files looked at: " + numberOfFilesLookedAt);
        if(results!=null)
            sb.append("\n---> Number of violations: " + results.size());
        System.out.println(sb.toString());
        return sb.toString();
    }

    public void startScan(String firstarg) throws Exception {
        
        filterByList = getFilterByList();
        Date startTime = new Date();
        SimpleDateFormat dateFmt = new SimpleDateFormat("dd-MMM-yy kk:mm:ss");


        System.out.println("============================================");
        System.out.println("       START: " + scanIssue + " at: " + dateFmt.format(startTime));
        System.out.println("============================================");
        
        String mode = "all";
        if (!isEmpty(filterListFilePath))
            mode = "scanList";
        
        runmode = System.getProperty("mode");        
        if(runmode == null)
            sCrawlDir = firstarg;

        // get all the exemption keys by processing the exemptions file
        exemptions = getExemptions();
        
        if(runmode != null && runmode.equals("LRG")) {
            series = firstarg.trim();
            String label = LRGUtil.getLatestLabel(series);
            sCrawlDir = LRGUtil.getCrawlDir(series, null, label,scanIssue);
        }
        
        // In dev mode - scan the current working directory and report all files in pwd as violations
        if (mode.equals("all")) {
            System.out.println("Scanning directory: " + sCrawlDir);
            runForDevMode(sCrawlDir);
        } else {
            System.out.println("Running for specified list of files, will filter by crawlDir:" + sCrawlDir);
            runForListFilterByPath(filterByList, sCrawlDir);
        }

        Date endTime = new Date();
        long elapsedMins = (endTime.getTime() - startTime.getTime()) / (1000 * 60);
        System.out.println("END: " + scanIssue + " at: " + dateFmt.format(endTime));
        System.out.println("Time taken (mins): " + elapsedMins);
        System.out.println("====================================================");
        printSummary();
        writeResults();

    }

    public boolean isExemption(String exemptionKey)
    {
        if(exemptions.contains(exemptionKey))
            return true;
        return false;
        }
    private void runForDevMode(String crawlDirectory) {
        if (filterByList != null && filterByList.size() > 0)
            System.out.println("Will filter list with file: " + filterListFilePath);
        crawlDirectory(crawlDirectory, false);
    }

    private void runForListFilterByPath(HashSet<String> listOffilesToRunAgainst, String crawlPathFilter) throws Exception {

        String adeViewRoot = FamilyModuleHelper.getViewRootFromPath(crawlPathFilter);
        Iterator<String> lifeOfFilesIter = listOffilesToRunAgainst.iterator();
        String relativeCrawlDirectoryPath = FamilyModuleHelper.getRelativeFileName(crawlPathFilter);
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

        if (!fileOfInterest(path))
            return;

        File f = new File(path);
        if (f == null || !f.exists()) {
            System.out.println("File does not exist: " + path);
            return;
        }
        if (bDebug)
            System.out.println("Processing: " + path);

        processFile(f, false);


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

    public boolean doesFileExist(String path) {
        try {
            File f = new File(path);
            if (f != null && f.exists())
                return true;
        } catch (Exception ex) {
        }
        return false;
    }

    private void writeResults() throws Exception {
        if(isEmpty(resultsFileName))
            resultsFileName = scanIssue + "Errors.csv";

        ABScanResult.writeResults(results, resultsFileName);    
        
        if (runmode != null && runmode.trim().equals("LRG")) {
            AngryBirdsDumper dumper = new AngryBirdsDumper(resultsFileName, true);
            dumper.dumpCsvToAngryBirdsDB(series, scanIssue, false);
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
        numberOfFilesLookedAt++;
        doProcessFile(fName);
    }

    abstract protected void doProcessFile(File fName);

    public void setResultsFileName(String resultsFileName) {
        this.resultsFileName = resultsFileName;
    }

    public String getResultsFileName() {
        return resultsFileName;
    }
    private HashSet<String> getExemptions() {
        HashSet<String> exemptionLines = new HashSet<String>();
        if(isEmpty(exemptionFileName))
            return exemptionLines;
        String exemptionFilePath = AngryBirdsDumper.getExemptionFlie(exemptionFileName);
        if (isEmpty(exemptionFilePath))
            return exemptionLines;
        try {
            File f = new File(exemptionFilePath);
            if (f == null || !f.exists())
                return exemptionLines;
            BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
            String line = "";
            while ((line = br.readLine()) != null) {
                if (line.trim().startsWith("#"))
                    continue;
               
                exemptionLines.add(getExemptionsKeyFromLine(line));
            }
            br.close();
        } catch (Exception ex) {
        }
        return exemptionLines;
    }
    
    /**
     * Last doubleslashhes marks begining of reason. This is the most common exemption format file
     * e.g. exemptionLine = AM##VO##Reason
     * Key will be AM##VO
     * @param lineFromExemptionFile
     * @return
     */
    protected String getExemptionsKeyFromLine(String lineFromExemptionFile) 
    {
        if(isEmpty(lineFromExemptionFile))
            return lineFromExemptionFile;
        int lastSlash=lineFromExemptionFile.lastIndexOf("##");
        if(lastSlash!=-1)
            return lineFromExemptionFile.substring(0, lastSlash);
        return lineFromExemptionFile;
        }
    

    public void setExemptionFileName(String exemptionFileName) {
        this.exemptionFileName = exemptionFileName;
    }

    public String getExemptionFileName() {
        return exemptionFileName;
    }

    public void setBDebug(boolean bDebug) {
        this.bDebug = bDebug;
    }

    public boolean isBDebug() {
        return bDebug;
    }

    public String getScanIssue() {
        return scanIssue;
    }

    public void setScanIssue(String scanIssue) {
        this.scanIssue = scanIssue;
    }
}
