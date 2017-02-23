package oracle.apps.seed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;

import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import java.util.regex.Pattern;

import oracle.apps.helpers.AngryBirdsDumper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.ProcessExecutor;
import oracle.apps.helpers.RegExScanner;
import oracle.apps.scanresults.ABScanResult;

public class SeedReextractionPreMerge {
    static String filterListFilePath = "";
    static boolean bDebug = true;
    Pattern checksumPattern = null;
    ArrayList<ABScanResult> results = new ArrayList<ABScanResult>();
    static final String ISSUE="Seed-Extract";
    
    public SeedReextractionPreMerge() {
        super();
        checksumPattern = Pattern.compile("checksum=");
    }

    /**
     * Usage: seedReextractionPreMerge <crawlDirectory> [-DfilterByList=<transactionFiles_savedToFile>]
     * NOTE: THIS SCRIPT HAS TO BE RUN FROM WITHIN A VIEW
     * Standard:
     * At PreMerge time, every seed file that is being merged in , and contains changes to its core data, must have been reextracted.
     * We are trying to flag cases of manual edits to the file which at patch time might invalidate the checksum
     *
     * Scan:
     * For every seed file being merged in
     *  - Generate a diff between current file and previous version
     *  - If diff does not contain extract date (means no reextraction)
     *      - If file contains changes to core data (Filter out the header and adxml changes)
     *          - Violation
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String usage = "seedReextractionPreMerge <crawlDirectory> -DfilterByList=<transactionFiles_savedToFile>";
        SeedReextractionPreMerge seedReextractionPreMerge = new SeedReextractionPreMerge();
        HashSet<String> filesForScan = getFilterByList();
        if (isEmpty(filterListFilePath)) {
            System.out.println("Missing filterByList. Usage: " + usage);
            System.exit(1);
        }
        if (args == null || args.length < 1 || isEmpty(args[0])) {
            System.out.println("Missing scan directory. Usage:  " + usage);
            System.exit(1);
        }
        String crawlDir = args[0];
        seedReextractionPreMerge.runForListFilterByPath(filesForScan, crawlDir);
        System.out.println("Number of files that need reextract: " + seedReextractionPreMerge.results.size());
        seedReextractionPreMerge.writeResults();

    }

    private void writeResults() {
        String resultFile = "seedExtract.csv";



        try {
            ABScanResult.writeResults(results, resultFile);

        } catch (Exception ex) {
        }


   
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

        process(path);


    }

    public void process(String path) {
        String diffOut = "seedDiff.txt";
        String diffOutFiltered = "seedDiffFiltered.txt";
        String extractOut = "extractOut.txt";
        String adeDiffCommand = "ade diff -diff_args \"-I Header\" -label " + path ;
        String extractPresentCommand = "grep -i extract " + diffOut;
        String subIssue ="Seed file should not be modified without reextract";
        // Generate diff with label
        
        try {
            ProcessExecutor processExec1 = new ProcessExecutor();
            ProcessBuilder pb = new ProcessBuilder("ade","diff","-w","-label",path);
            int errorCode = processExec1.runProcess(pb, diffOut, false);
//            if (errorCode != 0) {
//                System.out.println("Exception during : " + adeDiffCommand);
//                return;
//            }
            if (!doesFileExist(diffOut)) {
                System.out.println("Error in diff... Diff file does not exist : " + diffOut);
                return;
            }
            filterRelevantChanges(diffOut, diffOutFiltered);

            // Check if extract present
            ProcessBuilder pb2 = new ProcessBuilder("grep","-i","extract",diffOutFiltered);
            ProcessExecutor processExec2 = new ProcessExecutor();
            errorCode = processExec2.runProcess(pb2, extractOut, false);
            if (!doesFileExist(extractOut)) {
                System.out.println("Error in diff... extractOut file does not exist : " + diffOut);
                return;
            }
            // If extract present -> Return
            if (doesFileHaveContents(extractOut)) {
                System.out.println("OK Good .. This file has been reextracted: " + path);
                return;
            }
            // file has a diff, has not been extracted
            // Only if the file has a checksum attribute, does it make sense to ask for it to be reextracted, otherwise it doesnt matter
            if(doesFileNeedReextract(path,diffOutFiltered))
            {
                // Violation: File needs extract and has not been re-extracted
                ABScanResult r = new ABScanResult(path);
                r.setIssue(ISSUE);
                r.setSubIssue(subIssue);
                r.setDescription("File Should not be modified without a reextract");
                results.add(r);
            }
        } catch (Exception ex) {
            System.out.println("Exception during : " + adeDiffCommand);
            ex.printStackTrace();
        }


        // else : (make sure file contains changes apart from header and adxml) -> Violation (File contains diff but not reextracted). File should atleast have a checksum

    }

    public void filterRelevantChanges(String diffFile, String newDiffFile) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(diffFile));
        BufferedWriter bw = new BufferedWriter(new FileWriter(newDiffFile));

        String readLine = "";
        while ((readLine = br.readLine()) != null) {
            readLine = readLine.trim();
            if (isEmpty(readLine))
                continue;
            if (readLine.startsWith("<") || readLine.startsWith(">")) {
                if (readLine.contains("$Header"))
                    continue;
//                if (readLine.contains("adxml:"))
//                    continue;

                bw.write(readLine + "\n");
            }
        }
        if (br != null)
            br.close();
        if (bw != null)
            bw.close();
    }


    public boolean doesFileNeedReextract(String path, String diffFile) throws Exception {

        RegExScanner scanner = new RegExScanner(path, "xml");
        // If no checksums present - then there is no need for a reextract
        if (!scanner.doesContainPattern(checksumPattern))
            return false;

        return true;
    }

    public boolean doesFileHaveContents(String path) throws Exception {
        BufferedReader br = null;
        try {
            File f = new File(path);
            if (f == null || !f.exists())
                return false;
            br = new BufferedReader(new FileReader(f));
            String line = "";
            while ((line = br.readLine()) != null) {
                if (!isEmpty(line))
                    return true;
            }
            return false;
        } catch (Exception ex) {
        } finally {
            if (br != null)
                br.close();
        }
        return false;
    }

    public boolean doesFileExist(String path) {
        try {
            File f = new File(path);
            if (f == null || !f.exists())
                return false;
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    public boolean isDirectoryOfInterest(String sName) {
        if (sName == null || sName.equals(""))
            return false;
        String lowerCaseName = sName.toLowerCase();
        if (lowerCaseName.contains("/noship/") || lowerCaseName.contains("/test/") || lowerCaseName.contains("servicetest") ||
            lowerCaseName.contains("datasecuritytest") || lowerCaseName.contains("securitypublictest") ||
            lowerCaseName.contains("/publicservicetest") || lowerCaseName.contains("/publicmodeltest") ||
            lowerCaseName.contains("/protectedmodeltest") || lowerCaseName.contains("/publicuitest") ||
            lowerCaseName.contains("/uimodeltest") || lowerCaseName.contains("/structuretest") || lowerCaseName.contains("/modeltest") ||
            lowerCaseName.contains("/uitest") || lowerCaseName.contains("/testui") || lowerCaseName.contains("/setest") ||
            lowerCaseName.contains("/protecteduimodeltest") || lowerCaseName.contains("/testviewcontroller") ||
            lowerCaseName.contains("/flextest") || lowerCaseName.contains("/uimodeler-launch-test") ||
            lowerCaseName.contains("/publicuibasetest") || lowerCaseName.contains("/uipickertest") || lowerCaseName.contains("/dist/"))
            return false;

        return true;
    }

    public boolean fileOfInterest(String sName) {

        if (sName == null || sName.equals(""))
            return false;
        if (!isDirectoryOfInterest(sName))
            return false;

        if (sName.endsWith("SD.xml"))
            return true;
        return false;
    }

    private static HashSet<String> getFilterByList() throws Exception {
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

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

}
