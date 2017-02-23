package oracle.apps.seed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;

import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.ProcessExecutor;
import oracle.apps.helpers.RegExScanner;
import oracle.apps.helpers.ZOverallIntBase;
import oracle.apps.scanresults.ABScanResult;
import oracle.apps.utility.JoesBaseClass;

public class SeedReExtract extends ZOverallIntBase {
    static final String ISSUE = "Seed-Reextract";
    static String usage = "seedReExtract.sh <crawlDirectory> [-DfilterByList=<listOfFilesToRunOn_asFile>]";
    Pattern checksumPattern = null;

    public SeedReExtract() {
        super(ISSUE, JoesBaseClass.CRAWL_TYPE.SEED);
        bDebug = true;
        checksumPattern = Pattern.compile("checksum=");
    }

    public static void main(String[] args) throws Exception {
        SeedReExtract seedReExtract = new SeedReExtract();

        if (args == null || args.length < 1 || isEmpty(args[0])) {
            System.out.println("Missing scan directory. Usage:  " + usage);
            System.exit(1);
        }
        String crawlDir = args[0];
        seedReExtract.startScan(crawlDir);
    }

    @Override
    protected void doProcessFile(File fName) {
        String absPath = fName.getAbsolutePath();
        if (bDebug)
            System.out.println("Processing file: " + absPath);


        String diffOut = "seedDiff.txt";
        String diffOutFiltered = "seedDiffFiltered.txt";
        String extractOut = "extractOut.txt";
        //String adeDiffCommand = "ade diff -diff_args \"-I Header\" -label " + path ;
        // String extractPresentCommand = "grep -i extract " + diffOut;
        String subIssue = "Seed file should not be modified without reextract";
        
        try {
            String relativeFilePath = FamilyModuleHelper.getRelativeFileName(absPath);
            String adeViewRootPath = "$ADE_VIEW_ROOT/" + relativeFilePath;

            ProcessBuilder pb = new ProcessBuilder("ade", "diff", "-w","-pred", absPath);
            ProcessExecutor processExecutor1 = new ProcessExecutor();
            processExecutor1.runProcess(pb, diffOut, false);
            if (!doesFileExist(diffOut)) {
                System.out.println("Error in diff... Diff file does not exist : " + diffOut);
                return;
            }
            filterRelevantChanges(diffOut, diffOutFiltered);

            // Check if extract present
            ProcessBuilder pb2 = new ProcessBuilder("grep", "-i", "extract", diffOut);
            ProcessExecutor processExecutor2 = new ProcessExecutor();
            processExecutor2.runProcess(pb2, extractOut, false);
            if (!doesFileExist(extractOut)) {
                System.out.println("Error in diff... extractOut file does not exist : " + diffOut);
                return;
            }
            // If extract present -> Return
            if (doesFileHaveContents(extractOut)) {
                System.out.println("OK Good .. This file has been reextracted: " + absPath);
                return;
            }
            // file has a diff, has not been extracted
            // Only if the file has a checksum attribute, does it make sense to ask for it to be reextracted, otherwise it doesnt matter
            if (doesFileNeedReextract(absPath, diffOutFiltered)) {
                System.out.println("Found violation: " + absPath);
                // Violation: File needs extract and has not been re-extracted
                ABScanResult r = new ABScanResult(absPath);
                r.setIssue(ISSUE);
                r.setSubIssue(subIssue);
                r.setDescription("File Should not be modified without a reextract");
                results.add(r);
            }
        } catch (Exception ex) {
            System.out.println("Exception occurred");
            ex.printStackTrace();
        }

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
                bw.flush();
            }
        }
        if (br != null)
            br.close();
        if (bw != null) {
            bw.flush();
            bw.close();
        }
    }


    public boolean doesFileNeedReextract(String path, String diffFile) throws Exception {

        boolean needsExtract = false;
        RegExScanner scanner = new RegExScanner(path, "xml");
        // If no checksums present - then there is no need for a reextract
        if (!scanner.doesContainPattern(checksumPattern))
            return false;

        BufferedReader br = new BufferedReader(new FileReader(diffFile));
        String line = "";

        while ((line = br.readLine()) != null) {
            if (!isEmpty(line)) {
                needsExtract = true;
                break;
            }
        }
        if (br != null)
            br.close();

        return needsExtract;
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
}
