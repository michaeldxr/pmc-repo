package oracle.apps.accessibilityfixers;

import java.io.File;

import java.util.HashMap;
import java.util.Iterator;

import java.util.regex.Pattern;

import oracle.apps.NullEntityResolver;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class UIBundleFinder extends JoesBaseClass {
    private static final String ACR_ATTR_BUNDLE_FQN = "oracle.apps.common.acr.resource.ResourcesAttrBundle";
    private static final String ACR_GEN_BUNDLE_FQN = "oracle.apps.common.acr.resource.ResourcesGenBundle";
    private static final String ACR_BUNDLE_PATH = "/ade_autofs/ade_fusion_linux/FUSIONAPPS_MAIN_LINUX.rdd/LATEST/fusionapps/jlib/AdfResourcesPublicResource.jar";

    DOMParser parser;
    HashMap<String, HashMap<String, String>> jarMappings;
    
    boolean verbose = true;

    public UIBundleFinder() {
        super(CRAWL_TYPE.JPR);

        parser = new DOMParser();
        jarMappings = new HashMap<String, HashMap<String, String>>();
    }

    public void start(String crawlDir) {
	System.out.println("====================================================");
	System.out.println("Processing JPRs looking for XLF bundle references...");
	System.out.println("====================================================");
        crawlDirectory(crawlDir, false);
    }
    
    public void start(String crawlDir, boolean verbose) {
        this.verbose = verbose;
        this.start(crawlDir);
        System.out.println();
    }

    public void addJPR(File jprFile) {
        processFile(jprFile, false);
    }

    protected void processFile(File jprFile, boolean doADE) {
        if (verbose) {
            System.out.println("INFO: Processing jpr: " + jprFile.getAbsolutePath());
        }
        else {
            System.out.print(".");
        }
        if (isTestJpr(jprFile.getAbsolutePath())) {
            if (verbose) {
                System.out.println("INFO: this is a test jpr. Skipping...");
            }
            return;
        }

        HashMap<String, String> xlfMappings = new HashMap<String, String>();
        xlfMappings.put(this.ACR_ATTR_BUNDLE_FQN, ACR_BUNDLE_PATH);
        xlfMappings.put(this.ACR_GEN_BUNDLE_FQN, ACR_BUNDLE_PATH);

        parser.reset();
        parser.setEntityResolver(new NullEntityResolver());
        parser.showWarnings(true);
        parser.setValidationMode(DOMParser.NONVALIDATING);
        try {
            parser.parse("file:" + jprFile.getAbsolutePath());
        }
        catch (Exception e) {
            System.out.println("ERROR: Could not parse jpr. Skipping...");
            return;
        }

        XMLDocument doc = parser.getDocument();
        NodeList allHashes = doc.getElementsByTagName("hash");
        Node resourceBundleHash = null;
        for (int i = 0; i < allHashes.getLength(); i++) {
            Node node = allHashes.item(i);
            NamedNodeMap attrs = node.getAttributes();
            Node n = attrs.getNamedItem("n");
            if (n != null && n.getNodeValue()
                .equals("ResourceBundleAliases")) {
                resourceBundleHash = node;
                break;
            }
        }

        if (resourceBundleHash == null) {
            if (verbose) {
                System.out.println("WARNING: Couldn't find resource bundle mappings in file: " + jprFile.getAbsolutePath());
            }
            jarMappings.put(jprFile.getName(), xlfMappings);
            return;
        }

        NodeList mappings = resourceBundleHash.getChildNodes();
        for (int i = 0; i < mappings.getLength(); i++) {
            Node mapping = mappings.item(i);
            if (!mapping.getNodeName().equals("url"))
                continue;
            NamedNodeMap attrs = mapping.getAttributes();
            Node n = attrs.getNamedItem("n");
            String nString = n.getNodeValue();
            Node path = attrs.getNamedItem("path");
            String pathString = path.getNodeValue();
            xlfMappings.put(nString, jprFile.getParent() + "/" + pathString);
        }


        jarMappings.put(jprFile.getName(), xlfMappings);
    }

    public HashMap<String, HashMap<String, String>> getMappings() {
        return jarMappings;
    }

    public void printMappings() {
        System.out.println("=================== XLF Mappings ===================");
        Iterator<String> iter1 = jarMappings.keySet().iterator();
        while (iter1.hasNext()) {
            String jpr = iter1.next();
            System.out.println("------ JPR: " + jpr);
            HashMap<String, String> xlfMappings = jarMappings.get(jpr);
            Iterator<String> iter2 = xlfMappings.keySet().iterator();
            while (iter2.hasNext()) {
                String n = iter2.next();
                System.out.println("----------------------------------------------------");
                System.out.println("name = " + n);
                System.out.println("path = " + xlfMappings.get(n));
            }
            System.out.println();
        }
    }

    private boolean isTestJpr(String jprPath) {
        if (jprPath.indexOf("/uiTest/") > -1 || jprPath.indexOf("/modelTest/") > -1)
            return true;
        else
            return false;
    }

    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return null;
    }

    public static void main(String[] args) {
        String path = "/ade_autofs/ade_fusion_linux/FUSIONAPPS_PT.FINM6B_LINUX.rdd/LATEST/fusionapps/fin/components/payables/ap/invoices";
        UIBundleFinder x = new UIBundleFinder();
        x.crawlDirectory(path, false);

        x.printMappings();
    }
}
