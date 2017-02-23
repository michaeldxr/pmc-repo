package oracle.apps.seed;

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
import java.util.regex.Pattern;

import oracle.apps.di.MyNSResolver;
import oracle.apps.helpers.AngryBirdsDumper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.RegExScanner;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.scanresults.ABScanResult;
import oracle.apps.scanresults.GenericScanResult;
import oracle.apps.utility.JoesBaseClass;

import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SeedIncrementalUpload {
    int numSeedFilesLookedAt = 0;
    static boolean bDebug = true;


    int numberOfIncrLoadingMissing = 0;

    ArrayList<ABScanResult> incrLoadingResults = new ArrayList<ABScanResult>();
    HashSet<String> filterByList = new HashSet<String>();

    static final String ENABLE_UPLOAD_ISSUE = "Seed-IncrLoadEnabled";
    static String filterListFilePath = "";

    private static String runmode;
    private static String series;
    private static String sCrawlDir;
    
    static final String PROP_DRIVER = "SD_DRIVER";
    static final String PROP_INCR = "SD_INCR_MIDTIER";
    static HashSet<String> distinctAms = new HashSet<String>();
    static final String EXEMPTION_FNAME = "SeedIncrementalUpload_Exemptions";

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
        SeedIncrementalUpload x = new SeedIncrementalUpload();
        x.filterByList = getFilterByList();
        
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
        
        Date start = new Date();
        HashSet<String> exemptions = x.getExemptions();
        if (mode.equals("all")) {
            System.out.println("Scanning directory: " + sCrawlDir);
            x.runForDevMode(sCrawlDir, exemptions);
        } else {
            System.out.println("Running for specified list of files, will filter by crawlDir:" + sCrawlDir);
            x.runForListFilterByPath(x.filterByList, sCrawlDir, exemptions);
        }

        System.out.println(" [DONE] seedIncrementalUpload: ");
        Date end = new Date();
        long time_in_mins=(end.getTime()-start.getTime())/(1000*60);
        System.out.println("==> Time taken (mins) : " + time_in_mins);
        x.printSummary();
        x.writeResults();


    }

    private void runForDevMode(String crawlDirectory, HashSet<String> exemptions) {
        if (filterByList != null && filterByList.size() > 0)
            System.out.println("Will filter list with file: " + filterListFilePath);
        crawlDirectory(crawlDirectory, exemptions);
    }

    private void runForListFilterByPath(HashSet<String> listOffilesToRunAgainst, String crawlPathFilter,
                                        HashSet<String> exemptions) throws Exception {

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
                processFileExplicitly(absPath, exemptions);
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
                if (!isEmpty(line))
                    filterByList.add(line.trim());
            }
        } catch (Exception ex) {
        }

        return filterByList;

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


    protected void processFileExplicitly(String path, HashSet<String> exemptions) {

        if (!fileOfInterest(path))
            return;

        File f = new File(path);
        if (f == null || !f.exists()) {
            System.out.println("File does not exist: " + path);
            return;
        }
        if (bDebug)
            System.out.println("Processing: " + path);


        doIncrUploadEnabledCheck(f, exemptions);

    }


    protected void processFile(File fName, HashSet<String> exemptions) {

        String absPath = fName.getAbsolutePath();
        try {
            String relativePath = FamilyModuleHelper.getRelativeFileName(absPath);
            if (filterByList != null && filterByList.size() > 0)
                if (!filterByList.contains(relativePath))
                    return;
        } catch (Exception ex) {
        }

        //        if (bDebug)
        //            System.out.println("Processing: " + absPath);


        doIncrUploadEnabledCheck(fName, exemptions);

    }

    private void printSummary() {
        System.out.println("Number of Seed AMs looked at: "+ numSeedFilesLookedAt);
        System.out.println("Number of files containing incremental Upload Enabled Violations: " + incrLoadingResults.size());
    }

    private void writeResults() throws Exception{
        String checksumFile = "checksumErrors.csv";
        String incrLoadingEnbFile = "incrLoadingEnabledErrors.csv";

        ABScanResult.writeResults(incrLoadingResults, incrLoadingEnbFile);
        if(runmode != null && runmode.equals("LRG")) {
            AngryBirdsDumper dumper2 = new AngryBirdsDumper(incrLoadingEnbFile, true);
            dumper2.dumpCsvToAngryBirdsDB(series, ENABLE_UPLOAD_ISSUE,false);
        }
       
    }

    private HashMap<String, String> getSeedPropertiesOfInterest(XMLDocument seedAmXml) {
        HashMap<String, String> seedProperties = new HashMap<String, String>();
        String xpath = "//am:AppModule/am:Properties/am:CustomProperties/am:Property";
        //String xpath="//am:AppModule";
        seedAmXml.setProperty("SelectionLanguage", "XPath");
        try {
            NodeList nodes = seedAmXml.selectNodes(xpath, new MyNSResolver());
            if (nodes == null || nodes.getLength() < 1)
                return seedProperties;

            int nodeLen = nodes.getLength();
            for (int i = 0; i < nodeLen; i++) {
                Node node = nodes.item(i);
                if (node == null)
                    continue;
                if (!XMLParserHelper.isNodeName(node, "Property"))
                    continue;
                String propertyName = XMLParserHelper.getAttributeValue(node, "Name");
                String propertyValue = XMLParserHelper.getAttributeValue(node, "Value");
                if (isEmpty(propertyName))
                    continue;
                if (propertyName.contains(PROP_DRIVER) || propertyName.contains(PROP_INCR))
                    seedProperties.put(propertyName, propertyValue);

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return seedProperties;
    }

    private void doIncrUploadEnabledCheck(File file, HashSet<String> exemptions) {

        String subIssue1 = "Missing Incremental Loading Property";
        String subIssue2 = "Incremental Loading set to False";

        String absPath = file.getAbsolutePath();
        XMLDocument seedAmXml = XMLParserHelper.getXMLDocument(absPath);
        if (seedAmXml == null)
            return;
        HashMap<String, String> seedProperties = getSeedPropertiesOfInterest(seedAmXml);
        HashMap<String, String> seedDrivers = getSeedDrivers(seedProperties);
        // Not a seed AM - No drivers
        if (seedDrivers == null || seedDrivers.size() < 1)
            return;
        numSeedFilesLookedAt++;
        String amPackage = getPackageFromPath(absPath);
        if (isEmpty(amPackage)) {
            System.out.println("Could not obtain package from file path: " + absPath);
            return;
        }
        if (bDebug)
            System.out.println("Obtained Seed AM: " + file.getName() + " Package:" + amPackage);

        Iterator<String> seedDriverIter = seedDrivers.keySet().iterator();
        while (seedDriverIter.hasNext()) {
            String seedDriverProperty = seedDriverIter.next();
            String seedDriverVO = seedDrivers.get(seedDriverProperty);
            if (isEmpty(seedDriverVO))
                continue;
            // Exemption
            String exKey = getExemptionKey(amPackage, seedDriverVO);
            if (exemptions.contains(exKey))
                continue;
            String incrUploadProperty = PROP_INCR + "_" + seedDriverVO;
            incrUploadProperty = incrUploadProperty.trim();
            if (!seedProperties.containsKey(incrUploadProperty)) {
                //  Violation
                String description = String.format("Missing: %s for driver VO: %s", incrUploadProperty, seedDriverVO);
                ABScanResult r = new ABScanResult(absPath);
                r.setIssue(ENABLE_UPLOAD_ISSUE);
                r.setSubIssue(subIssue1);
                r.setDescription(description);
                incrLoadingResults.add(r);
            } else {
                String incrUploadPropValue = seedProperties.get(incrUploadProperty);
                if (!isEmpty(incrUploadPropValue) && incrUploadPropValue.equalsIgnoreCase("false")) {
                    //  Violation - should be registered exemption
                    String description = String.format("Property Value %s=False for driver VO: %s", incrUploadProperty, seedDriverVO);
                    ABScanResult r = new ABScanResult(absPath);
                    r.setIssue(ENABLE_UPLOAD_ISSUE);
                    r.setSubIssue(subIssue2);
                    r.setDescription(description);
                    incrLoadingResults.add(r);
                } else {
                    System.out.println("Wow !! - Property exists .. !! " + incrUploadProperty);
                }
            }

        }
    }


    private String getPackageFromPath(String path) {

        if (isEmpty(path))
            return null;
        int srcIndex = path.indexOf("/src/");
        if (srcIndex == -1)
            return null;
        String afterSrcPath = path.substring(srcIndex + "/src/".length());
        if (isEmpty(afterSrcPath))
            return null;
        if (afterSrcPath.startsWith("/"))
            afterSrcPath = afterSrcPath.substring(1);
        // Remove Extension
        int indexExtension = afterSrcPath.lastIndexOf(".");
        if (indexExtension >= 0)
            afterSrcPath = afterSrcPath.substring(0, indexExtension);
        // convert to package
        return afterSrcPath.replaceAll("/", ".");
    }

    private HashMap<String, String> getSeedDrivers(HashMap<String, String> seedProperties) {
        HashMap<String, String> seedDrivers = new HashMap<String, String>();
        if (seedProperties == null || seedProperties.size() < 1)
            return seedDrivers;

        Iterator<String> keyIter = seedProperties.keySet().iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            if (!isEmpty(key) && key.contains(PROP_DRIVER))
                seedDrivers.put(key, seedProperties.get(key));
        }
        return seedDrivers;
    }

    public boolean isValidDirectoryForScan(String sName) {

        if (sName == null || sName.equals(""))
            return false;
        String lowerCaseName=sName.toLowerCase();
        if (lowerCaseName.contains("/noship/") || lowerCaseName.contains("/test/") ||
            lowerCaseName.contains("servicetest") || lowerCaseName.contains("datasecuritytest") ||
            lowerCaseName.contains("securitypublictest") || lowerCaseName.contains("/publicservicetest") ||
            lowerCaseName.contains("/publicmodeltest") || lowerCaseName.contains("/protectedmodeltest") ||
            lowerCaseName.contains("/publicuitest") || lowerCaseName.contains("/uimodeltest") ||
            lowerCaseName.contains("/structuretest") || lowerCaseName.contains("/modeltest") ||
            lowerCaseName.contains("/uitest") || lowerCaseName.contains("/testui") ||
            lowerCaseName.contains("/setest") || lowerCaseName.contains("/protecteduimodeltest") ||
            lowerCaseName.contains("/testviewcontroller") || lowerCaseName.contains("/flextest") ||
            lowerCaseName.contains("/uimodeler-launch-test") || lowerCaseName.contains("/publicuibasetest") ||
            lowerCaseName.contains("/uipickertest") || lowerCaseName.contains("/dist/"))
            return false;

        return true;
    }


    /**
     * skip dbSchema and ui directories
     * @param sName
     * @return
     */
    public boolean isDirOfInterest(String sName) {
        if (!isValidDirectoryForScan(sName))
            return false;

        if (sName.contains("/dbSchema/"))
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

    public boolean fileOfInterest(String sName) {
        // Directory is not fo interest, so skip it
        if (!isDirOfInterest(sName))
            return false;

        // Process only AM's
        if (sName.endsWith("AM.xml"))
            return true;
        return false;
    }

    protected void crawlDirectory(String path, HashSet<String> exemptions) {
        if (path.contains(".ade_path") || path.contains("/classes/"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String sName = listOfFiles[i].getAbsolutePath();
                if (fileOfInterest(sName)) {
                    processFile(listOfFiles[i], exemptions);
                }
            } else if (listOfFiles[i].isDirectory()) {
                // Added by zeesha - no point going into the directory if we are not interested in it. Better performance
                if (isDirOfInterest(listOfFiles[i].getAbsolutePath()))
                    crawlDirectory(listOfFiles[i].getAbsolutePath(), exemptions);
            } else {
                System.out.println("ERROR:  node is neither file or directory: " + listOfFiles[i]);
            }
        }

    }

    private HashSet<String> getExemptions() {
        HashSet<String> exemptionLines = new HashSet<String>();
        String exemptionFilePath = AngryBirdsDumper.getExemptionFlie(EXEMPTION_FNAME);
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
                String[] parts = line.split("#");
                if (parts == null || parts.length < 2)
                    continue;
                String am = parts[0];
                String vo = parts[1];
                String exemptionKey = getExemptionKey(am, vo);
                exemptionLines.add(exemptionKey);
            }
            br.close();
        } catch (Exception ex) {
        }
        return exemptionLines;
    }

    private String getExemptionKey(String amPackage, String voName) {
        return amPackage + "#" + voName;
    }


}
