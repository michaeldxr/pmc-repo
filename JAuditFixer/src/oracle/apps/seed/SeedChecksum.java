package oracle.apps.seed;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.helpers.AngryBirdsDumper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.scanresults.ABScanResult;
import oracle.apps.utility.JoesBaseClass;

import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SeedChecksum extends JoesBaseClass {
    int numSeedFilesLookedAt = 0;
    static boolean bDebug = false;


    int numberOfMissingChecksum = 0;
    ArrayList<ABScanResult> checksumResults = new ArrayList<ABScanResult>();
    HashSet<String> filterByList = new HashSet<String>();
    HashSet<String> exemptions = new HashSet<String>();

    static final String CSUM_ISSUE = "Seed-CheckSum";
    static String filterListFilePath = "";


    public SeedChecksum() throws Exception {
        super(JoesBaseClass.CRAWL_TYPE.SEED);
    }

    private static String runmode;
    private static String series;
    private static String sCrawlDir;

    /**
     * Usages:
     * 1. seedIncrementalUpload $ADE_VIEW_ROOT/fusionapps [-filterByList=$filepath_containinglistOfFiles]
     *
     * When invoked from preMerge it should be invoked as:
     * seedIncrementalUpload $ADE_VIEW_ROOT/fusionapps  [-filterByList=$filepath_containinglistOfFiles_inTxn]
     * // This way it will only processing the list of files passed in
     *
     * When invoked for an ABScan, it should be invoked as
     * seedIncrementalUpload $ADE_VIEW_ROOT/fusionapps  [-filterByList=$filepath_containinglistOfFiles_inPatch]
     *
     * When invoked for dev work from within a view, it should be invoked as
     * seedIncrementalUpload $ADE_VIEW_ROOT/fusionapps
     * // It will crawl all files in crawlDirectory and report as violations
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        System.out.println(" ====================================== \n seedIncrementalUpload: START \n ====================================== \n");
        if (args == null || args.length < 1 || isEmpty(args[0])) {
            System.out.println("Usage: seedIncrementalUpload.sh <path to scan> ");
            System.exit(1);
        }

        // System Property -DfilterByList=FilterListPath
        SeedChecksum x = new SeedChecksum();
        x.filterByList = getFilterByList();
        x.exemptions = getExemptions();

        String mode = "all";
        if (!isEmpty(filterListFilePath))
            mode = "scanList";
        
        runmode = System.getProperty("mode");
        
        if(runmode == null)
            sCrawlDir = args[0];
        if(runmode != null && runmode.equals("LRG")) {
            series = args[0].trim();
            String label = LRGUtil.getLatestLabel(series);
            sCrawlDir = LRGUtil.getCrawlDir(series, null, label,"ADFdi");
        }
       
        if (mode.equals("all")) {
            System.out.println("Scanning directory: " + sCrawlDir);
            x.runForDevMode(sCrawlDir);
        } else {
            System.out.println("Running for specified list of files, will filter by crawlDir:" + sCrawlDir);
            x.runForListFilterByPath(x.filterByList, sCrawlDir);
        }

        System.out.println(" [DONE] seedIncrementalUpload: ");
        x.printSummary();
        x.writeResults();


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
                line=line.trim();
                if (!isEmpty(line) && !line.startsWith("#"))
                    filterByList.add(line.trim());
            }
        } catch (Exception ex) {
        }

        return filterByList;

    }
    
    private static HashSet<String> getExemptions() {
        
        HashSet<String> exemptions = new HashSet<String>();
        File f = new File(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
            "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/exemptions/SeedChecksum_exceptions.txt");
        if (f == null || !f.exists())
            return exemptions;

        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line = "";
            while ((line = br.readLine()) != null) {
                line=line.trim();
                if (!isEmpty(line))
                    exemptions.add(line.trim());
            }
        } catch (Exception ex) {
        }

        return exemptions;

    }


    private static HashSet<String> getFilterByList(String fileName) throws Exception {
        HashSet<String> filterByList = new HashSet<String>();
        String strFilterFile = fileName;
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

        doChecksumPresentCheck(f);


    }

    @Override
    protected void processFile(File fName, boolean bDoADE) {

        String absPath = fName.getAbsolutePath();
        try {
            String relativePath = FamilyModuleHelper.getRelativeFileName(absPath);
            if (filterByList != null && filterByList.size() > 0)
                if (!filterByList.contains(relativePath))
                    return;
        } catch (Exception ex) {
        }

        if (bDebug)
            System.out.println("Processing: " + absPath);


        doChecksumPresentCheck(fName);


    }

    private void printSummary() {

        System.out.println("Number of files containing checksum Violations: " + checksumResults.size());

    }

    private void writeResults() throws Exception{
        String checksumFile = "checksumErrors.csv";
        String incrLoadingEnbFile = "incrLoadingEnabledErrors.csv";

        ABScanResult.writeResults(checksumResults, checksumFile);

        if(runmode != null && runmode.equals("LRG")) {
            AngryBirdsDumper dumper = new AngryBirdsDumper(checksumFile, true);
            dumper.dumpCsvToAngryBirdsDB(series, CSUM_ISSUE,false);
        }
    }


    private void doChecksumPresentCheck(File file) {

        String subIssue = "Seed files must have checksum property";
        String absPath = file.getAbsolutePath();
        XMLDocument seedXml = XMLParserHelper.getXMLDocument(absPath);
        if (seedXml == null)
            return;
        NodeList seedDataNodes = seedXml.getElementsByTagName("SEEDDATA");
        if (!XMLParserHelper.hasOneNOnlyOneNode(seedDataNodes)) {

            System.out.println("[ERROR] Expecting one and only  one node SEEDDATA! Error in file: " + absPath);
            return;
        }
        Node seedDataNode = seedDataNodes.item(0);
        NodeList seedDataChildren = seedDataNode.getChildNodes();
        if (seedDataChildren == null || seedDataChildren.getLength() < 1)
            return;
        int numSeedChildren = seedDataChildren.getLength();
        for (int i = 0; i < numSeedChildren; i++) {
            Node seedChild = seedDataChildren.item(i);
            if (seedChild == null)
                continue;
            if (XMLParserHelper.isNodeName(seedChild, "#text") || XMLParserHelper.isNodeName(seedChild, "#comment"))
                continue;
            String csum = XMLParserHelper.getAttributeValue(seedChild, "checksum");
            if (isEmpty(csum)) {
                
                // Violation - Only report once per file
                String nodeName = seedChild.getNodeName();
                int lineNum = XMLParserHelper.getLineNumberInSourceFile(seedChild);
                String issueDesc = String.format("Line: %d Node: %s missing checksum attribute", lineNum, nodeName);
                ABScanResult r = new ABScanResult(absPath);
                if(exemptions.contains(r.getFileRelativePath()))
                    continue;
                r.setIssue(CSUM_ISSUE);
                r.setSubIssue(subIssue);
                r.setDescription(issueDesc);
                checksumResults.add(r);
                break;
            }

        }


    }

    @Override
    protected String getSummaryReportSubject() {
        return "SeedIncrementalUpload";
    }

    @Override
    protected String getSummaryReport() {
        return "SeedIncrementalUpload";
    }

    @Override
    public boolean isDirOfInterest(String sName) {
        boolean superResult = super.isDirOfInterest(sName);
        if (superResult == false)
            return false;
        if (sName.contains("/model/") || sName.contains("/publicModel/") || sName.contains("/protectedModel/") ||
            sName.contains("/service/"))
            return false;
        if (sName.contains("/ui/") || sName.contains("/publicUi/") || sName.contains("/protectedUi/"))
            return false;

        return true;

    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }


}
