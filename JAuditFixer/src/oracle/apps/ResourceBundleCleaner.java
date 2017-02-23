package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.accessibilityfixers.UIBundleFinder;
import oracle.apps.helpers.Mail;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class ResourceBundleCleaner extends JoesBaseClass {

    public class InvalidXLFReferenceException extends Exception {
        public InvalidXLFReferenceException(String message) {
            super(message);
        }
    }

    BufferedWriter bundleTypesBW;
    StringBuilder bundleTypesSB;
    BufferedWriter oldUsesBW;
    StringBuilder oldUsesSB;

    // Handle stats
    private int numBundleRemoved = 0;
    private int numOldBundleReferences = 0;
    private int numOldBundleReferences_noBundle = 0;
    private int numReview = 0;

    // Bundle stats
    private int numCase1Bundles = 0;
    private int numCase2Bundles = 0;
    private int numUnreadableBundles = 0;
    private int numBundlesNotThere = 0;

    private static final Pattern UI_BUNDLE_DEF_REGEX = Pattern.compile("^#\\{adfBundle\\['([\\w\\.]+)'\\]\\}$");
    private static final Pattern MODEL_BUNDLE_DEF_REGEX = Pattern.compile("^\\s*?<ResourceBundle>\\s*?<XliffBundle\\s+?id=\"(.+?)\"\\s*?(?:(?:/>)|(?:>\\s*?</XliffBundle>))\\s*?</ResourceBundle>\\s*$\n",
                                                                          Pattern.MULTILINE);
    private static final Pattern MODEL_BUNDLE_DEF_REGEX2 = Pattern.compile("<ResourceBundle.*?>\\s*?<XliffBundle\\s+?id=\"(.+?)\"\\s*?(?:(?:/>)|(?:>\\s*?</XliffBundle>))\\s*?</ResourceBundle>",
                                                                        Pattern.MULTILINE);
    private static final Pattern BAD_TASK_MENU_REGEX = Pattern.compile("#\\{adfBundle\\['([\\w\\.]+?)'\\]\\.\\w+?\\}");

    private HashMap<String, HashMap<String, String>> jprMappings;

    private DOMParser parser = new DOMParser();

    private boolean testMode = false;
    private boolean handlesOnly = false;
    private boolean bundlesOnly = false;
    private boolean reporting = false;

    public ResourceBundleCleaner(CRAWL_TYPE crawlType, String crawlDirectory) {
        super(crawlType);
        if (crawlType.equals(CRAWL_TYPE.ALL_MODEL_AND_VIEW)) {
            UIBundleFinder uiBundleFinder = new UIBundleFinder();
            uiBundleFinder.start(crawlDirectory, false);
            jprMappings = uiBundleFinder.getMappings();
        }
    }

    public void startBundlesCSV() {
        try {
            Process p = Runtime.getRuntime().exec("rm -f removed_bundles.csv");
            p.waitFor();

            bundleTypesBW = new BufferedWriter(new FileWriter("removed_bundles.csv"));
            bundleTypesSB = new StringBuilder();
            bundleTypesSB.append("XLF PATH, CONTAINED TRANSLATABLE STRINGS?\n");
        }
        catch (Exception e) {
            this.logException("removed_bundles.csv exception", e);
        }
    }

    public void closeBundlesCSV() {
        try {
            bundleTypesBW.write(bundleTypesSB.toString());
            bundleTypesBW.close();
        }
        catch (IOException e) {
            this.logException("removed_bundles.csv exception", e);
        }
    }

  public void startOldUsesCSV() {
      try {
          Process p = Runtime.getRuntime().exec("rm -f old_uses.csv");
          p.waitFor();

          oldUsesBW = new BufferedWriter(new FileWriter("old_uses.csv"));
          oldUsesSB = new StringBuilder();
          oldUsesSB.append("FILE NAME, FILE PATH, OLD HANDLE USE\n");
      }
      catch (Exception e) {
          this.logException("old_uses.csv exception", e);
      }
  }

  public void closeOldUsesCSV() {
      try {
          oldUsesBW.write(oldUsesSB.toString());
          oldUsesBW.close();
      }
      catch (IOException e) {
          this.logException("old_uses.csv exception", e);
      }
  }


    protected void processFile(File f, boolean doADE) {
        try {
            System.out.println("INFO: Reviewing file:  " + f.getAbsolutePath());
            XMLDocument doc = null;

            if (f.getName().endsWith("VO.xml") || f.getName().endsWith("EO.xml") || f.getName().endsWith("PS.xml")) {
                parser.reset();
                try {
                    parser.reset();
                    parser.setDebugMode(true);
                    parser.setErrorStream(System.out);
                    parser.setEntityResolver(new NullEntityResolver());
                    parser.showWarnings(true);
                    parser.setValidationMode(DOMParser.NONVALIDATING);
                    parser.parse("file:" + f.getAbsolutePath());
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("ERROR:  XML Parse error while parsing file:  " + f.getAbsolutePath());
                    System.err.println("Skipping...");
                    this.logException("Error parsing file: " + f.getAbsolutePath(),
                                      e);
                    return;
                }
                doc = parser.getDocument();

                // Grab all ResId attributes.

                List<String> resIds = getAllResIds(doc,
                                                   new ArrayList<String>());

                Iterator<String> resIdsIterator = resIds.iterator();

                boolean hasBundleReferences = false;
                while (resIdsIterator.hasNext()) {
                    String resId = resIdsIterator.next();
                    if (resId.endsWith("LABEL")) {
                        hasBundleReferences = true;
                        oldUsesSB.append(f.getName() + ", " + f.getAbsolutePath() + ", ResId=\"" + resId + "\"\n");
                    }
                }
                boolean hasBundle = hasBundle(doc);

                if (hasBundle) {
                    if (hasBundleReferences) {
                        this.numOldBundleReferences++;
                        System.out.println("REVIEW: File references an old (or incorrectly addded) bundle: " + f.getAbsolutePath());
                    }
                    else {
                        String fString = convertFileToString(f);
                        this.numBundleRemoved++;
                        if (testMode) {
                            System.out.println("INFO: File flagged for bundle reference removal: " + f.getAbsolutePath());
                        }
                        else {
                            removeBundleFromModel(f, fString, doADE);
                            System.out.println("INFO: Removed bundle reference from file: " + f.getAbsolutePath());

                        }

                        if (!handlesOnly) {
                            Matcher m = MODEL_BUNDLE_DEF_REGEX.matcher(fString);
                            boolean match_found = m.find();
                            if (!match_found) {
                                m = MODEL_BUNDLE_DEF_REGEX2.matcher(fString);
                                match_found = m.find();
                            }
                            if (!match_found) {
                                System.err.println("ERROR: Unexpected <Xliffbundle> format. Cannot remove. Skipping...");
                                this.logException("Unexpected <XliffBundle> format: " + f.getAbsolutePath(), new RuntimeException());
                                return;
                            }
                            if (!m.group(1).contains(".resource."))
                                removeBundle(m.group(1), f.getAbsolutePath());
                        }
                    }
                }
                else {
                    if (hasBundleReferences) {
                        this.numOldBundleReferences_noBundle++;
                        this.numOldBundleReferences++;
                        System.out.println("REVIEW: File has old bundle references, but no defined bundle: " + f.getAbsolutePath());
                    }
                }
            }
            else if (f.getName().endsWith(".jsff") || f.getName().endsWith(".jspx")) {
                parser.reset();
                try {
                    parser.reset();
                    parser.setDebugMode(true);
                    parser.setErrorStream(System.out);
                    parser.setEntityResolver(new NullEntityResolver());
                    parser.showWarnings(true);
                    parser.setValidationMode(DOMParser.NONVALIDATING);
                    parser.parse("file:" + f.getAbsolutePath());
                }
                catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("ERROR:  XML Parse error while parsing file:  " + f.getAbsolutePath());
                    System.err.println("Skipping...");
                    this.logException("Error parsing file: " + f.getAbsolutePath(),
                                      e);
                    return;
                }
                doc = parser.getDocument();
                HashMap<String, String> oldBundles = null;
                try {
                    HashMap<String, String> xlfMappings = jprMappings.get(identifyJPR(f).getName());
                    oldBundles = getOldBundles(doc, xlfMappings.keySet());
                }
                catch (NullPointerException e) {
                    System.out.println("WARNING: File is not part of a JPR: " + f.getAbsolutePath());
                    System.out.println("Skipping...");
                }
                boolean atLeastOneBundleRemoved = false;
                if (oldBundles != null) {
                    String fString = convertFileToString(f);
                    Set<String> oldBundleKeySet = oldBundles.keySet();
                    for (String oldBundle : oldBundleKeySet) {
                        Pattern badRef = Pattern.compile(oldBundle + "\\['.+?'\\]");
                        
                        boolean hasBundleReferences = false;
                        Matcher m = badRef.matcher(fString);
                        StringBuilder tempSB = new StringBuilder();
                        while (m.find()) {
                            tempSB.append(f.getName() + ", " + f.getAbsolutePath() + ", " + m.group() + "\n");
                            hasBundleReferences = true;
                        }
                        if (!hasBundleReferences) {
                            atLeastOneBundleRemoved = true;
                            if (testMode) {
                                System.out.println("INFO: File flagged for bundle removal: " + f.getAbsoluteFile());
                            }
                            else {
                                removeBundleFromUI(f, fString, oldBundle,
                                                   doADE);
                                System.out.println("INFO: Removed '" + oldBundle + "' bundle reference from file: " + f.getAbsolutePath());
                            }
                            if (!handlesOnly) {
                                if (!oldBundles.get(oldBundle).contains(".resource."))
                                    removeBundle(oldBundles.get(oldBundle),
                                                 f.getAbsolutePath());
                            }
                        }
                        else if (!isSRBundle(oldBundles.get(oldBundle), f.getAbsolutePath())) {
                            this.numOldBundleReferences++;
                            oldUsesSB.append(tempSB.toString());
                            System.out.println("REVIEW: File uses keys from old (or incorrectly added) bundle '" + oldBundle + "': " + f.getAbsolutePath());
                        }

                    }

                }
                if (atLeastOneBundleRemoved)
                    this.numBundleRemoved++;
            }
            else if (f.getName().endsWith("taskmenu.xml")) {
                // GOOD EXAMPLE:    #{adfBundle['oracle.apps.prc.pos.commonPos.resource.PrcPosCommonPosGenBundle']['Header.Orders']}
                //  BAD EXAMPLE:    #{adfBundle['oracle.apps.financials.receivables.receiptSetup.ui.ReceiptSourceDashboard_taskmenuBundle'].ASSIGNMENT_WORK_BENCH}
                String fString = convertFileToString(f);
                fString = removeHTMLComments(fString);
                Matcher m = BAD_TASK_MENU_REGEX.matcher(fString);
                if (m.find()) {
                    if (!m.group(1).endsWith("GenBundle") && !m.group(1).endsWith("AttrBundle")) {
                        System.out.println("REVIEW: File contains references to old bundles: " + f.getAbsolutePath());
                        this.numOldBundleReferences++;
                    }
                }
            }
            else {
              String fString = convertFileToString(f);
              if (!fString.contains("<!-- Extracted from SR:"))
                removeBundle(f);
              }
        }
        catch (Exception e) {
            System.err.println("ERROR: Unexpected error in file: " + f.getAbsolutePath());
            System.err.println("Skipping file. The developer has been notified.");
            e.printStackTrace();
            this.logException("Unhandled error in file: " + f.getAbsolutePath(),
                              e);
        }
    }

    private boolean isSRBundle(String fqn, String artifactPath) throws Exception {
        if (fqn.equals("oracle.apps.fnd.applcore.messages.FndMessagesResourceBundle"))
            return true;
        
        String relativePath = fqn.replace('.', '/');
        relativePath += ".xlf";

        String bundlePath = null;
        if (artifactPath.contains("/src/")) {
            int s = artifactPath.indexOf("/src/");
            bundlePath = artifactPath.substring(0, s + 5) + relativePath;
        }
        else {
            int p = artifactPath.indexOf("/public_html/");
            bundlePath = artifactPath.substring(0, p) + "/src/" + relativePath;
        }
        
        File xlf = new File(bundlePath);
        
        if (!xlf.isFile())
            return false;
        String fString = convertFileToString(xlf);
        if (fString.contains("<!-- Extracted from SR:"))
            return true;
        return false;
    }

    private void removeBundleFromUI(File f, String fString, String bundle,
                                    boolean doADE) throws Exception {

        try {
            Pattern p = Pattern.compile("^\\s*?<c:set\\s+?var=\"" + bundle + "\"\\s+?value=\"#\\{adfBundle\\['.+?'\\]\\}\"\\s*?/>\\s*$\n",
                                        Pattern.MULTILINE);
            removeRegexAndSave(f, fString, p, doADE);
        }
        catch (RuntimeException e) {
            try {
                Pattern p = Pattern.compile("^\\s*?<c:set\\s+?value=\"#\\{adfBundle\\['.+?'\\]\\}\"\\s+?var=\"" + bundle + "\"\\s*?/>\\s*$\n",
                                            Pattern.MULTILINE);
                removeRegexAndSave(f, fString, p, doADE);
            }
            catch (RuntimeException e2) {
                System.err.println("REVIEW: Could not remove bundle reference. Please do so manually: " + f.getAbsolutePath());
                this.numReview++;
                return;
            }
        }
    }

    private void removeBundleFromModel(File f, String fString,
                                       boolean doADE) throws Exception {
        try {
            removeRegexAndSave(f, fString, MODEL_BUNDLE_DEF_REGEX, doADE);
        }
        catch (Exception e) {
            removeRegexAndSave(f, fString, MODEL_BUNDLE_DEF_REGEX2, doADE);
        }
    }

    private void removeRegexAndSave(File f, String fString, Pattern regex,
                                    boolean doADE) throws Exception {
        Matcher m = regex.matcher(fString);
        if (m.find()) {
            fString = m.replaceFirst("");
        }
        else {
            throw new RuntimeException("No match found.");
        }

        if (doADE) {
            checkoutAndDelete(f);
            FileWriter fw = new FileWriter(f.getAbsolutePath());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(fString);
            bw.close();
            checkinAndDiff(f);
        }
        else {
            System.out.println(fString);
        }
    }

    private List<String> getAllResIds(Node node, List<String> resIds) {
        NodeList children = node.getChildNodes();
        if (children != null && children.getLength() > 0) {
            for (int i = 0; i < children.getLength(); i++) {
                getAllResIds(children.item(i), resIds);
            }
        }

        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            Node resId = attributes.getNamedItem("ResId");
            if (resId != null) {
                resIds.add(resId.getNodeValue());
            }
        }

        return resIds;
    }

    private boolean hasBundle(XMLDocument doc) {
        NodeList xliffBundles = doc.getElementsByTagName("XliffBundle");
        if (xliffBundles != null && xliffBundles.getLength() > 0)
            return true;
        return false;
    }

    private HashMap<String, String> getOldBundles(XMLDocument doc,
                                                  Set<String> newBundles) throws CloneNotSupportedException {
        // Special bundle that Satish wants me to unclude in list of "good" bundles
        newBundles = new HashSet(newBundles);
        newBundles.add("oracle.apps.fnd.applcore.messages.FndMessagesResourceBundle");
        HashMap<String, String> oldBundles = new HashMap<String, String>();
        NodeList bundleNodes = doc.getElementsByTagName("set");
        for (int i = 0; i < bundleNodes.getLength(); i++) {
            Node valueNode = bundleNodes.item(i).getAttributes().getNamedItem("value");
            if (valueNode != null) {
                try {
                    String value = pullFQNFromElExpression(valueNode.getNodeValue());
                    if (!newBundles.contains(value)) {
                        oldBundles.put(bundleNodes.item(i).getAttributes().getNamedItem("var").getNodeValue(),
                                       value);
                    }
                }
                catch (Exception e) {
                    continue;
                }
            }
        }

        return oldBundles;
    }

    private File identifyJPR(File path) throws NullPointerException {
        File parent = path.getParentFile();
        File[] listOfFiles = parent.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            String fileName = listOfFiles[i].getAbsolutePath();
            if (fileName.endsWith(".jpr"))
                return listOfFiles[i];
        }
        return identifyJPR(parent);
    }

    private String pullFQNFromElExpression(String expression) {
        Matcher matcher = UI_BUNDLE_DEF_REGEX.matcher(expression);
        if (!matcher.matches())
            throw new RuntimeException("EL Expression looks nothing like what I expected: " + expression);
        return matcher.group(1);
    }

    protected String convertFileToString(File f) throws Exception {
        File fp = new File(f.getAbsolutePath());
        FileReader reader = new FileReader(fp);
        BufferedReader input = new BufferedReader(reader);

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = input.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }

        return sb.toString();
    }

    private void removeBundle(String fqn,
                              String artifactPath) throws Exception {
        String relativePath = fqn.replace('.', '/');
        relativePath += ".xlf";

        String bundlePath = null;
        if (artifactPath.contains("/src/")) {
            int s = artifactPath.indexOf("/src/");
            bundlePath = artifactPath.substring(0, s + 5) + relativePath;
        }
        else if (artifactPath.contains("/public_html/")) {
            int p = artifactPath.indexOf("/public_html/");
            bundlePath = artifactPath.substring(0, p) + "/src/" + relativePath;
        }
        else {
            numBundlesNotThere++;
            System.out.println("WARNING: Could not remove bundle. Your directory structure is not a type I recognize. Please remove manually if bundle exists: " + fqn);
            return;
        }

        File xlf = new File(bundlePath);

        if (xlf.isFile())
            removeBundle(xlf);
        else {
            numBundlesNotThere++;
            System.out.println("WARNING: Could not remove bundle. Possibly already removed: " + xlf.getAbsolutePath());
        }
    }

    private void removeBundle(File xlf) throws Exception {
        parser.reset();
        parser.setDebugMode(true);
        try {
            parser.setErrorStream(System.out);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        try {
            parser.setEntityResolver(new NullEntityResolver());
            parser.showWarnings(true);
            parser.setValidationMode(DOMParser.NONVALIDATING);

            parser.parse("file:" + xlf.getAbsolutePath());

            XMLDocument doc = parser.getDocument();

            NodeList transUnits = doc.getElementsByTagName("trans-unit");
            boolean hasTranslatableStrings = false;
            // Search through all of the <trans-unit> tags
            for (int i = 0; i < transUnits.getLength(); i++) {
                Node transUnit = transUnits.item(i);
                NodeList children = transUnit.getChildNodes();
                // Find the <source> tag in each trans-unit
                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if (child.getNodeName().equals("source")) {
                        if (!child.getTextContent().trim().isEmpty())
                            hasTranslatableStrings = true;
                    }
                }
            }

            if (hasTranslatableStrings) {
                numCase1Bundles++;
                bundleTypesSB.append(xlf.getAbsolutePath() + ", YES\n");
            }
            else {
                numCase2Bundles++;
                bundleTypesSB.append(xlf.getAbsolutePath() + ", NO\n");
            }
        }
        catch (Exception e) {
            numUnreadableBundles++;
            bundleTypesSB.append(xlf.getAbsolutePath() + ", UNREADABLE\n");
        }

        if (!testMode && !reporting)
            removeFile(xlf);
    }

    protected String getSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("-------------- Summary Report --------------");
	if (reporting) {
	    sb.append("\nNumber of local resource bundles found:   " + (this.numCase1Bundles + this.numCase2Bundles + this.numUnreadableBundles));
            sb.append("\n...those with translatable strings:       " + this.numCase1Bundles);
            sb.append("\n...those with no translatable strings:    " + this.numCase2Bundles);
            sb.append("\n...those unreadable (malformed):          " + this.numUnreadableBundles);
        }
        else {
            if (!bundlesOnly) {
		sb.append("\nNumber of files marked for bundle removal:                " + this.numBundleRemoved);
		sb.append("\nNumber of files that still reference and use old bundles: " + this.numOldBundleReferences);
		if (numReview > 0)
		    sb.append("\nNumber of files that require manual fixes:                " + this.numReview);
		sb.append("\n");
	    }
	    if (!handlesOnly) {
		sb.append("\nNumber of .xlf files marked for deletion: " + (this.numCase1Bundles + this.numCase2Bundles + this.numUnreadableBundles));
                sb.append("\n...those with translatable strings:       " + this.numCase1Bundles);
                sb.append("\n...those with no translatable strings:    " + this.numCase2Bundles);
                sb.append("\n...those unreadable (malformed):          " + this.numUnreadableBundles);
                sb.append("\n");
                sb.append("\nNumber of .xlf files not found:           " + this.numBundlesNotThere);
            }
	}
        return sb.toString();
    }

    protected String getSummaryReportSubject() {
        return "SCRIPT: Resource Bundle Cleaner";
    }

    protected void logUsageReport(String sCrawlDir, String mode) {
        String s = "";

        s += "user.dir set to  --->  " + System.getProperty("user.dir") + "\n";
        s += "Crawl Dir set to --->  " + sCrawlDir + "\n";
        s += "Run mode set to  --->  " + mode + "\n";

        s += getSummaryReport();

        System.out.println(s);

        s += "\n\n**EXCEPTION REPORT ************************************************\n";
        if (m_numExceptions == 0) {
            s += "No Exceptions to report.\n";
        }
        else {
            s += "A total of " + m_numExceptions + " exception(s) were collected.\n\n";
            s += sExceptions;
        }
        //Mail.sendMail("angel.irizarry@oracle.com", s, getSummaryReportSubject());
    }

    public static void main(String[] args) {
        if (args.length != 3 || args[0] == null || args[1] == null || args[0].equals("") || args[1].equals("")) {
            System.out.println("USAGE: cleanResourceBundleReferences.sh [ --all | --dryrun | --handlesonly | --bundlesonly | --reporting ]");
            System.exit(1);
        }
        
        if (!args[0].contains("/components") || !args[0].contains("/fusionapps/")) {
            System.out.println("This script is intended to be run from anywhere inside fusionapps/..../components, all other directories are disabled now for your protection.");
            System.exit(1);
        }

        String crawlDirectory = args[0];
        Boolean doADE = new Boolean(args[1]);

        boolean testMode = false;
        boolean handlesOnly = false;
        boolean bundlesOnly = false;
	boolean reporting = false;
        if (args[2].equals("--dryrun")) {
            testMode = true;
        }
        else if (args[2].equals("--handlesonly")) {
            handlesOnly = true;
        }
        else if (args[2].equals("--bundlesonly")) {
            bundlesOnly = true;
        }
	else if (args[2].equals("--reporting")) {
	    reporting = true;
	}
        else if (args[2].isEmpty() || !args[2].equals("--all")) {
            System.out.println("USAGE: cleanResourceBundleReferences.sh [ --all | --dryrun | --handlesonly | --bundlesonly | --reporting ]");
            System.exit(1);
        }

        ResourceBundleCleaner x = null;
        if (bundlesOnly || reporting) {
            x = new ResourceBundleCleaner(CRAWL_TYPE.XLF, crawlDirectory);
        }
        else {
            x = new ResourceBundleCleaner(CRAWL_TYPE.ALL_MODEL_AND_VIEW,
                                          crawlDirectory);
        }

        x.testMode = testMode;
        x.bundlesOnly = bundlesOnly;
        x.handlesOnly = handlesOnly;
	x.reporting = reporting;

        x.startBundlesCSV();
        if (!bundlesOnly && !reporting)
            x.startOldUsesCSV();
        if (doADE.booleanValue()) {
            try {
                x.startDiffLog();
            }
            catch (Exception e) {
                x.logException("diff log exception", e);
            }
        }
        x.crawlDirectory(crawlDirectory, doADE.booleanValue());
        x.closeBundlesCSV();
        if (!bundlesOnly && !reporting)
            x.closeOldUsesCSV();

        x.logUsageReport(crawlDirectory, args[2]);
    }
}
