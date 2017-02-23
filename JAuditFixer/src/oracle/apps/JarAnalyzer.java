package oracle.apps;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.Mail;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.apps.votuning.UiFileHelper;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JarAnalyzer {
    public JarAnalyzer() {
        super();
    }

    // Parameters configuring script
    public static boolean findClassInMultipleJars = true;
    public static boolean findModelJarsInUiJar = false;
    public static boolean findJprDeploymentProfViolations = false;

    public static int m_numberJarFiles = 0;
    public static int m_numberJprFiles = 0;
    public static int m_numberJprFixed = 0;
    public static boolean bDebug = false;

    private static Workbook m_myBook;
    static Worksheet m_classMultipleJarsSheet;
    static Worksheet m_ModelInUiJarsSheet;
    static Worksheet m_violtingJprSheet;
    static int m_modelClassesInUISheetRow = 2;
    static int m_jprViolationsSheetRow = 2;
    static String label ="";
    static String family ="";

    static BufferedWriter writer;
    static int CUTOFF_UIJAR_MODELCLASS_MAX = 1;
    HashMap<String, ArrayList<String>> profilesNotFixedDueToExceptions = new HashMap<String, ArrayList<String>>();

    /**
     * HashMap to store the jar and corresponding class files. This will be used when indexing to associate a class with its jar.
     */
    private static HashMap<String, HashSet<String>> _jarMap = new HashMap<String, HashSet<String>>();

    static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");

    public static void main(String[] args) throws Exception {


        // Make sure it is run with correct number of arguments
        if (args.length <3 || isEmpty(args[0]) || isEmpty(args[1]) || isEmpty(args[2])) {
            System.out.println("Usage:  jarAnalyzer.sh <path to your code in an open ade view>  <fixViolations> <bDoADE>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        Boolean bfixViolations = new Boolean(args[1]);
        Boolean bDoADE = new Boolean(args[2]);
        if(args.length > 3) family = args[3].trim().toUpperCase();
        
        boolean append = false;
        if(args.length > 4) 
            append = new Boolean(args[4].trim());

//        String sCrawlDir = "/ade_autofs/ade_fusion_linux/FUSIONAPPS_PT.FINM6B_LINUX.rdd/LATEST/fusionapps";
//        Boolean bfixViolations = false;
//        Boolean bDoADE = false;
//        family = "FIN";        
        
        File folder = new File(sCrawlDir);

        writer = new BufferedWriter(new FileWriter("duplicateContentScan.csv", append));
      //  writer.write(label + "\n");
        if(!append)
          writer.write("Product,Label,Family,Entry,Jar1,Jar2\n");

        System.out.println("Analyzing  directory:" + sCrawlDir);
        JarAnalyzer jarAnalyzer = new JarAnalyzer();
        
        label = jarAnalyzer.getLabel(folder.getCanonicalPath());
      
      
        if(family.equals(""))
           jarAnalyzer.getFamilyFromLabel();
         
        String families[] = {"fin", "hcm", "crm", "prj", "prc", "scm", "ic", "fsm", "fscm", "atf"};
        HashSet acceptedFamilies = new HashSet(Arrays.asList(families));
        
        if(!acceptedFamilies.contains(family.trim().toLowerCase())) {
           System.out.println("Invalid Family Name. Please specify one of - fin/hcm/crm/prj/prc/scm/ic/fsm/fscm/atf");
           System.exit(1);
        }

        // Remove any existing diff.txt. Create a new bufferred writer for diff.txt
        if (bDoADE) {
            diffHelper.startDiffLog();
        }

        //jarAnalyzer.processJprForDeplProfileViolation(new File("/home/zcurrimb/view_storage/zcurrimb_com_cs_index/fusionapps/fin/components/financialsEss/gl/journals/allocations/publicEssUi/FinGlJrnlAllocationsPublicEssUi.jpr"));
        if (findModelJarsInUiJar) {
            initializeModelInUISheet("modelClassesInUIJars");
        }
        if (findJprDeploymentProfViolations) {
            initializeViolatingJprs("jprViolations");
        }


        try {
            jarAnalyzer.crawlDirectory(sCrawlDir, bfixViolations, bDoADE);
        } catch (Exception ex) {
            System.out.println("Exception occurred: " + ex.getMessage());
            Date runDate = new Date();
            String summary = "CrawlDirectory: " + sCrawlDir + "\nException: " + ex.getMessage();
            Mail.sendMail("zeesha.currimbhoy@oracle.com", summary,
                          "SCRIPT: to detect deployment profile dependencies failed: " + runDate.toString());
            System.exit(1);
        }
        jarAnalyzer.printResults(bfixViolations);
        if (m_myBook == null) {
            System.out.println("There were no results to write");
            System.exit(0);
        }

        if (bDoADE) {
            diffHelper.closeDiffLog();
        }

        String summary = "Number of Jprs looked at: " + m_numberJprFiles + " \n";
        summary = summary + "Number of Jprs fixed: " + m_numberJprFixed;

        try {
            System.out.println("[START]Writing spreadsheet");
            writeSpreadsheet("output.xlsx");
            System.out.println("[END]Writing spreadsheet");
        } catch (Exception ex) {
            System.out.println("Exception while writing spreadsheet..");
            ex.printStackTrace();
            summary += "\nException: " + ex.getMessage();
        }
        Date runDate = new Date();
        if(bfixViolations)
            Mail.sendMail("zeesha.currimbhoy@oracle.com", summary,
                      "SCRIPT: to detect deployment profile dependencies run at: " + runDate.toString());
        writer.close();
    }

    private String printResults(boolean fixViolations) {
        StringBuffer sbResults = new StringBuffer();
        sbResults.append("\n =================================");
        sbResults.append("\n ============ RESULTS ============");
        sbResults.append("\n =================================\n");
        sbResults.append("Number of Jprs looked at: " + m_numberJprFiles);
        sbResults.append("Number of Jprs fixed: " + m_numberJprFixed);
        if (findClassInMultipleJars) {
            initializeMultipleClassesSheet("ClassInMultipleJars");
            int sheetRowNum = 2;
            if (_jarMap != null && _jarMap.size() > 0) {

                Set<String> keySet = _jarMap.keySet();
                Iterator<String> keyIter = keySet.iterator();
                while (keyIter.hasNext()) {
                    String entryName = keyIter.next();
                    HashSet<String> jars = _jarMap.get(entryName);

                    if (jars != null && jars.size() > 1) {
                        writeInMultipleClassSheet(sheetRowNum, entryName, jars);
                        sheetRowNum++;
                        sbResults.append(String.format("\n File: %s present in following jars %s ", entryName, jars.toString()));
                    }
                }
            } else {
                sbResults.append("\n No classes in multiple jars");
            }

        }
        if (findJprDeploymentProfViolations && fixViolations) {
            if (profilesNotFixedDueToExceptions != null && profilesNotFixedDueToExceptions.size() > 0) {
                sbResults.append("\n **** Following Files need manual fixes");

                Set<String> keySet = profilesNotFixedDueToExceptions.keySet();
                Iterator<String> keyIter = keySet.iterator();
                int num = 1;
                while (keyIter.hasNext()) {
                    String key = keyIter.next();
                    ArrayList<String> exc = profilesNotFixedDueToExceptions.get(key);

                    String msg = String.format("\n --> %d Jpr#ProfileName : %s \n Msg: %s \n", num, key, exc.toString());
                    sbResults.append(msg);
                    num++;
                }
            }
        }
        System.out.println(sbResults.toString());
        return sbResults.toString();
    }

    private void crawlDirectory(String crawlDir, boolean makeFix, boolean bDoADE) {
        if (crawlDir.contains(".ade_path") || crawlDir.contains("/classes/") || crawlDir.contains("/noship"))
            return;

        File folder = new File(crawlDir);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (isValidJarFile(absFilePath, listOfFiles[i].getName())) {
                    m_numberJarFiles++;
                    if (findClassInMultipleJars) {
                        processJarForClassInMultipleJars(listOfFiles[i]);
                    }
                    if (findModelJarsInUiJar) {
                        processJarForModelClassInUiJar(listOfFiles[i]);
                    }

                }
                if (isValidJpr(absFilePath)) {
                    m_numberJprFiles++;
                    if (findJprDeploymentProfViolations) {
                        processJprForDeplProfileViolation(listOfFiles[i], makeFix, bDoADE);
                    }
                }

            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                crawlDirectory(dir, makeFix, bDoADE);
            } else {
                System.out.println("ERROR:  node is neither file or directory: " + listOfFiles[i].getAbsolutePath());
            }
        }
    }


    /**
     * Process the jar file. It reads all the allowed classes (see method allowedEntryName to know which classes are allowed)
     * that this file contains and stores the class and the corresponding jar in a hash map.
     * For each permissible file within the jar, it stores the class file as the key and the corresponding jar entry as its value.
     * @param f Jar file that is to be processed
     */
    private void processJarForClassInMultipleJars(File f) {
        String jarPath = f.getAbsolutePath();
        if (bDebug) {
            System.out.println("Processing jar: " + jarPath);
        }

        //   final String JAR_PATH = getFormattedJarPath(f.getPath());

        try {
            JarInputStream jarStream = new JarInputStream(new FileInputStream(f));

            if (jarStream == null) {
                System.err.println("[ERROR] Could not open jar stream");
                return;
            }

            while (true) {

                JarEntry jarEntry = jarStream.getNextJarEntry();
                if (jarEntry == null) {
                    break;
                }
                String strEntryName = jarEntry.getName().trim();

                if (!jarEntry.isDirectory() && allowedEntryName(strEntryName)) {
                    String strFormattedEntryName = formatEntryName(strEntryName);
                    addToJarMap(strFormattedEntryName, jarPath);
                }
            }
        } catch (IOException ex) {
            System.err.println("Error occurred while opening jar file: " + f.getName());
            return;
        }
    }

    public boolean isUIJar(String absPath) {
        if (isEmpty(absPath))
            return false;
        if (absPath.contains("Ui.jar") || absPath.contains("UI.jar") || absPath.contains("Di.jar") || absPath.contains("DI.jar"))
            return true;

        return false;
    }

    /**
     * Process the jar file. It reads all the allowed classes (see method allowedEntryName to know which classes are allowed)
     * that this file contains and stores the class and the corresponding jar in a hash map.
     * For each permissible file within the jar, it stores the class file as the key and the corresponding jar entry as its value.
     * @param f Jar file that is to be processed
     */
    private void processJarForModelClassInUiJar(File f) {
        String jarPath = f.getAbsolutePath();
        if (!isUIJar(jarPath)) {
            return;
        }
        if (bDebug) {
            System.out.println("Processing jar: " + jarPath);
        }

        HashSet<String> modelClassesInJar = new HashSet<String>();
        try {
            JarInputStream jarStream = new JarInputStream(new FileInputStream(f));

            if (jarStream == null) {
                System.err.println("[ERROR] Could not open jar stream");
                return;
            }

            while (true) {

                JarEntry jarEntry = jarStream.getNextJarEntry();
                if (jarEntry == null) {
                    break;
                }
                String strEntryName = jarEntry.getName().trim();

                if (!jarEntry.isDirectory() && isModelEntry(strEntryName)) {
                    String strFormattedEntryName = formatEntryName(strEntryName);
                    if (modelClassesInJar.size() > CUTOFF_UIJAR_MODELCLASS_MAX)
                        break;
                    modelClassesInJar.add(strFormattedEntryName);

                    //addToModelInUiMap(strFormattedEntryName, jarPath);
                }
            }
        } catch (IOException ex) {
            System.err.println("Error occurred while opening jar file: " + f.getName());
            return;
        } catch (Exception ex) {
            System.out.println("Exception while processing file: " + f.getAbsolutePath());
            ex.printStackTrace();
        }

        if (modelClassesInJar.size() > 0) {
            writeInModelClassInUIJarSheet(m_modelClassesInUISheetRow, jarPath, modelClassesInJar);
            m_modelClassesInUISheetRow++;
        }
    }


    /**
     * Process the jar file. It reads all the allowed classes (see method allowedEntryName to know which classes are allowed)
     * that this file contains and stores the class and the corresponding jar in a hash map.
     * For each permissible file within the jar, it stores the class file as the key and the corresponding jar entry as its value.
     * @param f Jar file that is to be processed
     */
    private void processJprForDeplProfileViolation(File f, boolean makeFix, boolean bDoADE) {
        String jprPath = f.getAbsolutePath();

        if (bDebug) {
            System.out.println("Processing jpr: " + jprPath);
        }

        NodeList deploymentProfiles = JprHelper.getDeploymentProfiles(jprPath);
        if (deploymentProfiles == null) {
            if (bDebug)
                System.out.println("No deployment profiles for jpr: " + jprPath);

            return;
        }
        HashMap<String, HashSet<String>> profile_buildOutputDependencies = new HashMap<String, HashSet<String>>();
        int numberOfProfiles = deploymentProfiles.getLength();
        for (int i = 0; i < numberOfProfiles; i++) {
            Node deploymentProfileHash = deploymentProfiles.item(i);
            if (!XMLParserHelper.isNodeName(deploymentProfileHash, "hash")) {
                System.out.println("Node name is not hash: name is: " + deploymentProfileHash.getNodeName());
                continue;
            }
            String profileName = XMLParserHelper.getAttributeValue(deploymentProfileHash, "n");
            if (isEmpty(profileName))
                continue;
            if (!deploymentProfileHash.hasChildNodes())
                continue;

            Node deploymentProfileUrlNode =
                XMLParserHelper.getChildNodeWithMatchingAttributeAndName(deploymentProfileHash, "url", "n", "jarURL");
            if (deploymentProfileUrlNode == null)
                continue;
            String deploymentProfilePath = XMLParserHelper.getAttributeValue(deploymentProfileUrlNode, "path");

            Node libraryDependencyNode =
                XMLParserHelper.getChildNodeWithMatchingAttributeAndName(deploymentProfileHash, "hash", "n",
                                                                         "LibraryDependencies");
            if (libraryDependencyNode == null)
                continue;
            HashSet<Node> buildOpNodes = getBuildOutputLibraryDependencies(libraryDependencyNode);
            if (buildOpNodes != null && buildOpNodes.size() > 0) {
                if (makeFix) {
                    try {
                        XmlFixer jprFixer = new XmlFixer(jprPath);
                        HashSet<String> fixedUrls = fixBuildOutputDependencyNodes(jprPath, jprFixer, buildOpNodes, profileName);
                        profile_buildOutputDependencies.put(deploymentProfilePath, fixedUrls);
                        if (bDoADE) {
                            String jprNewContents = jprFixer.getFileContentsAsString(false);
                            applyFix(jprPath, jprNewContents);
                        }
                    } catch (Exception ex) {

                        System.out.println(String.format("[ERROR][MakeFix] Exception while trying to fix jpr: %s, exception %s",
                                                         jprPath, ex.getMessage()));
                        ex.printStackTrace();
                    }


                } else {
                    HashSet<String> buildOpUrls = getBuildOutputDependencyUrls(buildOpNodes);
                    profile_buildOutputDependencies.put(deploymentProfilePath, buildOpUrls);
                }

            }

        }
        if (profile_buildOutputDependencies != null && profile_buildOutputDependencies.size() > 0) {
            m_numberJprFixed++;
            writeInJprViolationSheet(new File(jprPath), profile_buildOutputDependencies);
        }

    }

    public HashSet<String> fixBuildOutputDependencyNodes(String jprPath, XmlFixer jprFixer, HashSet<Node> buildOutputHashNodes,
                                                         String proflileName) throws Exception {
        String jprName = jprPath.substring(jprPath.lastIndexOf("/") + 1);
        HashSet<String> buildOutputUrls = new HashSet<String>();

        if (buildOutputHashNodes == null || buildOutputHashNodes.size() <= 0)
            return null;
        ArrayList<String> couldNotFixMultipleProfiles = new ArrayList<String>();

        Iterator<Node> iterNode = buildOutputHashNodes.iterator();
        while (iterNode.hasNext()) {
            Node hashNode = iterNode.next();
            if (!XMLParserHelper.isNodeName(hashNode, "hash"))
                continue;

            Node classNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(hashNode, "value", "n", "class");
            Node ownerUrlNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(hashNode, "url", "n", "sourceOwnerURL");
            Node sourceUrlNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(hashNode, "url", "n", "sourceURL");
            if (classNode == null || ownerUrlNode == null || sourceUrlNode == null) {
                System.out.println("[ERROR] [MakeFix] Could not obtain class or sourceOwnerURL or sourceURL Nodes");
                continue;
            }
            String currentNode = "";
            String oldBOPath = "";
            String newDepProfile = "";
            try {
                currentNode = "class Node";
                jprFixer.modifyAttribute(classNode, "n", "class", "adapterClass", false);
                jprFixer.modifyAttribute(classNode, "v", "oracle.jdeveloper.library.ProjectLibrary",
                                         "oracle.jdevimpl.deploy.common.DefaultProfileDependable", false);

                currentNode = "sourceOwnerUrl Node";
                jprFixer.modifyAttribute(ownerUrlNode, "n", "sourceOwnerURL", "ownerURL", false);
                String pathOldValue = XMLParserHelper.getAttributeValue(ownerUrlNode, "path");
                String pathNewValue = XMLParserHelper.getAttributeValue(sourceUrlNode, "path");
                oldBOPath = pathNewValue;
                jprFixer.modifyAttribute(ownerUrlNode, "path", pathOldValue, pathNewValue, false);

                currentNode = "Source URL  Node";
                File jprFile = new File(jprPath);
                File dependentJpr = new File(jprFile.getParent(), pathNewValue);
                if (!dependentJpr.exists())
                    throw new Exception("Dependent jpr file does not exist: " + dependentJpr.getAbsolutePath());

                if (dependentJpr.getName().equals(jprName)) {
                    throw new Exception("[ERROR] [MakeFix] Will not modify any self dependent dependencies");
                }
                String deploymentProfile = getDependentDeploymentProfile(jprPath, dependentJpr, proflileName);
                newDepProfile = deploymentProfile;
                String newprofileNode = String.format("<value n=\"profileName\" v=\"%s\" />", deploymentProfile);
                jprFixer.replaceNode(sourceUrlNode, newprofileNode);

            } catch (Exception ex) {

                System.out.println(String.format("[ERROR] [MakeFix] Exception while trying to make fix for jpr: %s for %s",
                                                 jprPath, classNode));
                addToExceptionsDueToMultiple(profilesNotFixedDueToExceptions, String.format("%s # %s", jprPath, proflileName),
                                             ex.getMessage());
                throw new Exception(ex);
            }
            buildOutputUrls.add(String.format("OldValue: %s , NewValue %s", oldBOPath, newDepProfile));
        }

        return buildOutputUrls;
    }

    public void addToExceptionsDueToMultiple(HashMap<String, ArrayList<String>> exceptionMap, String profilePath,
                                             String excToAdd) {
        if (exceptionMap.containsKey(profilePath)) {
            ArrayList<String> exceptions = exceptionMap.get(profilePath);
            exceptions.add(excToAdd);
            exceptionMap.put(profilePath, exceptions);
        } else {
            ArrayList<String> exceptions = new ArrayList<String>();
            exceptions.add(excToAdd);
            exceptionMap.put(profilePath, exceptions);
        }
    }

    public String getDependentDeploymentProfile(String baseJpr, File dependentJpr, String baseProfileName) throws Exception {
        String dependentJprPath = dependentJpr.getAbsolutePath();
        NodeList profileList = JprHelper.getDeploymentProfilesList(dependentJprPath);
        if (profileList == null || profileList.getLength() <= 0) {
            throw new Exception("Could not get list of profiles for dependent jpr: " + dependentJprPath);
        }

        if (profileList.getLength() != 1) {
            String foundProfileName = "";
            int adfProfilesCnt = 0;
            int profileLen = profileList.getLength();
            String availableProfiles = "";
            for (int i = 0; i < profileLen; i++) {
                Node prof = profileList.item(i);
                if (!XMLParserHelper.isNodeName(prof, "string"))
                    continue;
                String profName = XMLParserHelper.getAttributeValue(prof, "v");
                if (profName.startsWith("Adf")) {
                    adfProfilesCnt++;
                    foundProfileName = profName;
                }
                availableProfiles = availableProfiles + "  " + profName;

            }
            if (adfProfilesCnt > 1) {
                String msg =
                    String.format("Could not fix jpr: %s since dependent jpr %s has more than one defined profiles.\n Available Profiles: %s \n Cannot choose without user input.",
                                  baseJpr, dependentJpr.getAbsolutePath(), availableProfiles);
                addToExceptionsDueToMultiple(profilesNotFixedDueToExceptions, String.format("%s # %s", baseJpr, baseProfileName),
                                             msg);
                throw new Exception(msg);
            } else {
                if (!isEmpty(foundProfileName))
                    return foundProfileName;
            }
        }
        Node profileNode = profileList.item(0);
        if (!XMLParserHelper.isNodeName(profileNode, "string"))
            throw new Exception("Could not obtain valid profile for dependen jpr: " + dependentJprPath);
        String profileName = XMLParserHelper.getAttributeValue(profileNode, "v");
        if (isEmpty(profileName)) {
            throw new Exception("Could not obtain valid profile for dependent jpr: " + dependentJprPath);
        }
        return profileName;
    }

    public HashSet<String> getBuildOutputDependencyUrls(HashSet<Node> buildOuputNodes) {
        HashSet<String> buildOutputUrls = new HashSet<String>();

        if (buildOuputNodes == null || buildOuputNodes.size() <= 0)
            return null;


        Iterator<Node> iterNode = buildOuputNodes.iterator();
        while (iterNode.hasNext()) {
            Node hashNode = iterNode.next();
            if (!XMLParserHelper.isNodeName(hashNode, "hash"))
                continue;
            Node urlNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(hashNode, "url", "n", "sourceURL");
            if (urlNode == null)
                continue;
            String url = XMLParserHelper.getAttributeValue(urlNode, "path");
            buildOutputUrls.add(url);
        }
        return buildOutputUrls;
    }

    public HashSet<Node> getBuildOutputLibraryDependencies(Node libraryDependencyNode) {

        HashSet<Node> buildOutputDependencies = new HashSet<Node>();
        if (libraryDependencyNode == null)
            return null;

        Node dependencyList =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(libraryDependencyNode, "list", "n", "dependencyList");
        if (dependencyList == null)
            return null;
        if (!dependencyList.hasChildNodes())
            return null;

        NodeList dependencyListChildren = dependencyList.getChildNodes();

        int dependencyListLen = dependencyListChildren.getLength();
        for (int i = 0; i < dependencyListLen; i++) {

            Node dependencyHash = dependencyListChildren.item(i);
            if (!XMLParserHelper.isNodeName(dependencyHash, "hash")) {
                continue;
            }
            Node valueClassNode =
                XMLParserHelper.getChildNodeWithMatchingAttributeAndName(dependencyHash, "value", "n", "class");
            if (valueClassNode == null)
                continue;
            Node sourceOwnerUrl =
                XMLParserHelper.getChildNodeWithMatchingAttributeAndName(dependencyHash, "url", "n", "sourceOwnerURL");
            if (sourceOwnerUrl == null)
                continue;

            Node classNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(dependencyHash, "value", "n", "class");
            if (classNode == null)
                continue;

            String classValue = XMLParserHelper.getAttributeValue(classNode, "v");
            if (isEmpty(classValue) || !classValue.trim().equals("oracle.jdeveloper.library.ProjectLibrary")) {
                continue;
            }


            Node sourceUrlNode =
                XMLParserHelper.getChildNodeWithMatchingAttributeAndName(dependencyHash, "url", "n", "sourceURL");
            if (sourceUrlNode == null)
                continue;

            buildOutputDependencies.add(dependencyHash);
            /*String sourceUrl = XMLParserHelper.getAttributeValue(sourceUrlNode, "path");


            File jprFile = new File(jprPath);
            String jprParent = jprFile.getParent();
            File sourceFile = new File(jprParent, sourceUrl);
            if (sourceFile.exists()) {
                buildOutputDependencies.add(sourceFile.getAbsolutePath());
            } else {
                buildOutputDependencies.add(sourceUrl);
            }*/

        }
        return buildOutputDependencies;
    }

    public boolean isModelEntry(String strEntryName) {

        if (isEmpty(strEntryName))
            return false;
        if (strEntryName.contains("/model/") || strEntryName.contains("/uiModel/") ||
            strEntryName.contains("/protectedModel/") || strEntryName.contains("/publicModel/") ||
            strEntryName.contains("/publicEssModel/"))
            return true;
        if (strEntryName.contains("/entity/") || strEntryName.contains("/publicView/") ||
            strEntryName.contains("/publicEntity/") || strEntryName.contains("/view/") ||
            strEntryName.contains("/applicationModule/"))
            return true;
        return false;
    }

    public void addToJarMap(String entryName, String jarPath) {
        if (_jarMap.containsKey(entryName)) {
            HashSet<String> setJars = _jarMap.get(entryName);
            setJars.add(jarPath);
            _jarMap.put(entryName, setJars);
        } else {
            HashSet<String> setJars = new HashSet<String>();
            setJars.add(jarPath);
            _jarMap.put(entryName, setJars);
        }
    }

    /*public void addToModelInUiMap(String entryName, String jarPath) {

        if (_modelInUiJarMap.containsKey(jarPath)) {
            HashSet<String> setEntries = _modelInUiJarMap.get(jarPath);
            if(setEntries.size()>=CUTOFF_UIJAR_MODELCLASS_MAX)
            {return;}
            setEntries.add(entryName);
            _modelInUiJarMap.put(jarPath, setEntries);
        } else {
            HashSet<String> setEntries = new HashSet<String>();
            setEntries.add(entryName);
            _modelInUiJarMap.put(jarPath, setEntries);
        }
    }*/

    /**
     * Formats the jar entry name to a form that can be added to the map
     * @param strEntryName The jar entry name to be formatted
     * @return The formatted entry name
     */
    private String formatEntryName(String strEntryName) {
        String ext = getFileExtension(strEntryName);

        if (ext.equals("class")) {
            return strEntryName.substring(0, strEntryName.indexOf(ext) - 1);
        } else {
            if (strEntryName.contains("oracle/apps")) {
                // always format the name so as to remove everything before oracle/apps. this is so that we format
                // files that are of format WEB-INF/oracle/apps to remove the WEB-INF
                String strFormatted = strEntryName.substring(strEntryName.indexOf("oracle/apps"));
                return strFormatted;
            }
            /* if it does not contain oracle/apps but it passed the valid entry name test, it should be added to the map
             as is*/
            return strEntryName;
        }

    }


    /**
     *  Rules for classes to be included:
     *  1. A file with the extensions: xcfg, class, jsff, jspx, xsd, wsdl always included
     *  2. A file that contains "oracle/apps" always included
     *  3. Files that have META-INF in their path not allowed - these are the common files that are present in every jar
     *  3. File that does not contain oracle/apps but is a text file is not allowed (this rejects adfLibReadMe.txt)
     * @param strEntryName
     * @return true/false to indicate if the entry is allowed
     */
    private boolean allowedEntryName(String strEntryName) {
        String[] allowedJarExt = null;
        String ext = getFileExtension(strEntryName);
        allowedJarExt = new String[] { "class", "jpx", "xcfg", "jsff","jspx" };

        if (strEntryName.contains(".ade_path"))
            return false;

        if (strEntryName.startsWith("persdef/") || strEntryName.contains("/persdef/"))
            return false;

        if(strEntryName.contains("FlexfieldPkgInf.xml"))
            return false;
        
        // always accept these extensions
        for (int i = 0; i < allowedJarExt.length; i++) {
            if (ext.equals(allowedJarExt[i].trim())) {
                return true;
            }
        }

        // reject all entries that start with META-INF - these are the common files in every jar. we dont want these
        if (strEntryName.startsWith("META-INF"))
            return false;

        // if there is any entry that has oracle/apps accept it
        if (strEntryName.contains("oracle/apps"))
            return true;
        else {
            /* if it is not in the oracle apps dir, and has txt reject it (this is to reject adflibREADME.txt
             there can be files that are not in the oracle apps dir but have jpx or jspx ext. We should accept those
             incase they are found in source while indexing */
            if (ext.equals("txt"))
                return false;
        }

        // for debugging: System.err.println("Rejected entry: " + strEntryName);
        return false;
    }


    public boolean isValidJarFile(String path, String fileName) {
        if (isEmpty(path))
            return false;

        if (path.contains("/classes/") || path.contains("/.ade_path/"))
            return false;
        if (path.endsWith("Test.jar"))
            return false;
        if (fileName.startsWith("Adf") && (path.endsWith(".jar") && !path.endsWith("_ko.jar")))
            return true;

        return false;
    }

    public boolean isValidJpr(String path) {
        if (isEmpty(path))
            return false;

        if (path.contains("/classes/") || path.contains("/.ade_path/") || path.contains("/dbSchema/"))
            return false;
        if (path.endsWith("Test.jpr"))
            return false;
        if (path.endsWith(".jpr"))
            return true;

        return false;
    }


    /**
     * Gets the extension of the file
     * @param f File whose extension is to be found
     * @return the extension. If an extension cannot be found, UNKNOWN is returned
     */
    private static String getFileExtension(File f) {
        String name = f.getName();
        String extension = "UNKNOWN";
        if ((name != null) && (name.indexOf(".") != -1)) {
            extension = name.substring(name.indexOf(".") + 1);
        }
        return extension;
    }

    private static String getFileExtension(String strFileName) {
        String extension = "UNKNOWN";
        if ((strFileName != null) && (strFileName.indexOf(".") != -1)) {
            extension = strFileName.substring(strFileName.indexOf(".") + 1);
        }
        return extension;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }


    private static void initializeMultipleClassesSheet(String worksheetName) {
        if (m_myBook == null) {
            m_myBook = new Workbook("output");
        }
        // Create a worksheet
        m_classMultipleJarsSheet = new Worksheet(worksheetName);
        // column 1 - Product
        m_classMultipleJarsSheet.setColumnWidth(1, 10);
        // column 2 - entryName
        m_classMultipleJarsSheet.setColumnWidth(2, 30);
        // Column 3 - jars
        m_classMultipleJarsSheet.setColumnWidth(3, 40);


        // column 1 - product
        m_classMultipleJarsSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Product")));
        // column 2 - filename
        m_classMultipleJarsSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Entry Name")));
        // Column 3 - file path
        m_classMultipleJarsSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Jars Present In")));


        m_myBook.addWorksheet(m_classMultipleJarsSheet);
    }

    private static void initializeModelInUISheet(String worksheetName) {
        if (m_myBook == null) {
            m_myBook = new Workbook("output");
        }
        // Create a worksheet
        m_ModelInUiJarsSheet = new Worksheet(worksheetName);
        // column 1 - Product
        m_ModelInUiJarsSheet.setColumnWidth(1, 10);
        // column 2 - UI Jar
        m_ModelInUiJarsSheet.setColumnWidth(2, 30);
        // Column 3 - Model Entries
        m_ModelInUiJarsSheet.setColumnWidth(3, 40);


        // column 1 - product
        m_ModelInUiJarsSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Product")));
        // column 2 - filename
        m_ModelInUiJarsSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("UI Jar")));
        // Column 3 - file path
        m_ModelInUiJarsSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Model Entries")));


        m_myBook.addWorksheet(m_ModelInUiJarsSheet);
    }

    private static void initializeViolatingJprs(String worksheetName) {
        if (m_myBook == null) {
            m_myBook = new Workbook("output");
        }
        // Create a worksheet
        m_violtingJprSheet = new Worksheet(worksheetName);
        // column 1 - Product
        m_violtingJprSheet.setColumnWidth(1, 10);
        // column 2 - jprName
        m_violtingJprSheet.setColumnWidth(2, 30);
        // column 3 - jpr Path
        m_violtingJprSheet.setColumnWidth(3, 30);
        // Column 4 - profileName
        m_violtingJprSheet.setColumnWidth(4, 40);
        // Column 5 - build output dependencies
        m_violtingJprSheet.setColumnWidth(5, 40);

        // column 1 - product
        m_violtingJprSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Product")));
        // column 2 - filename
        m_violtingJprSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Jpr Name")));
        // column 3 - filePath
        m_violtingJprSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Jpr Path")));
        // Column 4 - profile name
        m_violtingJprSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("Profile Name")));
        // Column 5 - deployment dependencies
        m_violtingJprSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Buld output dependencies")));


        m_myBook.addWorksheet(m_violtingJprSheet);
    }


    /**
     *Write spreadsheet to disk
     * @param outputFileName
     * Output file name to write to
     * @throws Exception
     * Exception if operation is unsuccessful
     */
    public static void writeSpreadsheet(String outputFileName) throws Exception {

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFileName);
            XLSXCreator.create(m_myBook, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    /**
     * Write the component details in the spreadsheet row
     * @param rowNumber
     * row number to write to
     * @param component
     * Component details
     */
    public static void writeInMultipleClassSheet(int rowNumber, String entryName, HashSet<String> jars) {


        String productCellNumber = "A" + rowNumber;
        String entryNameCellNumber = "B" + rowNumber;
        String jarsCellNumber = "C" + rowNumber;

        String[] entryParts = entryName.split("/");
        String product = "";
        if (entryParts.length >= 4) {
            product = entryParts[3];
        }
        String formattedJars = "";
        String jarsForCsv = "";
        if (jars != null) {
            Iterator<String> jarsIter = jars.iterator();
            while (jarsIter.hasNext()) {
                String nextjar = jarsIter.next();
                formattedJars = formattedJars + " \n " + nextjar;
                jarsForCsv += getFileName(nextjar) + ",";
            }
        }

        m_classMultipleJarsSheet.addCell(new Cell(new CellLocation(productCellNumber), new CellValue(product)));
        m_classMultipleJarsSheet.addCell(new Cell(new CellLocation(entryNameCellNumber), new CellValue(entryName)));
        m_classMultipleJarsSheet.addCell(new Cell(new CellLocation(jarsCellNumber), new CellValue(formattedJars)));

        try {
            String jarFamily = getFamilyFromJar(jarsForCsv);
            if(jarFamily.equals(family))
              writer.write(product + "," + label + "," + family + "," + entryName + "," + jarsForCsv + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getFileName(String filePath) {

        if (filePath.contains("fusionapps/")) {
            Integer fusionappsIndex = filePath.indexOf("fusionapps/");
            String path_after_fusionapps = filePath.substring(fusionappsIndex);
            return path_after_fusionapps;
        }
        if (filePath.contains("fsm/")) {
            Integer fusionappsIndex = filePath.indexOf("fsm/");
            String path_after_fusionapps = filePath.substring(fusionappsIndex);
            return path_after_fusionapps;
        }
        return filePath;
    }

    /**
     * Write the component details in the spreadsheet row
     * @param rowNumber
     * row number to write to
     * @param component
     * Component details
     */
    public static void writeInJprViolationSheet(File jprFile, HashMap<String, HashSet<String>> profileDependencies) {


        String productCellNumber = "A" + m_jprViolationsSheetRow;
        String jprFileNameCellNumber = "B" + m_jprViolationsSheetRow;
        String jprFilePathCellNumber = "C" + m_jprViolationsSheetRow;
        String profileCellNumber = "D" + m_jprViolationsSheetRow;
        String dependentJarCellNumber = "E" + m_jprViolationsSheetRow;
        String jprPath = jprFile.getAbsolutePath();
        String jprName = jprFile.getName();
        String product = UiFileHelper.getProductName(jprPath);


        Set<String> keyset = profileDependencies.keySet();
        Iterator<String> keyiter = keyset.iterator();


        while (keyiter.hasNext()) {
            String profilePath = keyiter.next();
            HashSet<String> dependencies = profileDependencies.get(profilePath);
            String depend = "";
            if (dependencies != null) {
                depend = dependencies.toString();
            }

            m_violtingJprSheet.addCell(new Cell(new CellLocation(productCellNumber), new CellValue(product)));

            m_violtingJprSheet.addCell(new Cell(new CellLocation(jprFileNameCellNumber), new CellValue(jprName)));

            m_violtingJprSheet.addCell(new Cell(new CellLocation(jprFilePathCellNumber), new CellValue(jprPath)));

            m_violtingJprSheet.addCell(new Cell(new CellLocation(jprFilePathCellNumber), new CellValue(jprPath)));

            m_violtingJprSheet.addCell(new Cell(new CellLocation(profileCellNumber), new CellValue(profilePath)));

            m_violtingJprSheet.addCell(new Cell(new CellLocation(dependentJarCellNumber), new CellValue(depend)));
            m_jprViolationsSheetRow++;
        }
    }

    /**
     * Gets the product name from the file path
     *
     * @param uiFilePath
     * @return
     */
    public static String getProductName(String absPath) {
        int i = 0;
        if (absPath.contains("fusionapps/")) {
            i = absPath.indexOf("fusionapps/");
        }
        if (absPath.contains("fsm/")) {
            i = absPath.indexOf("fsm/");
        }

        String pathAfterFusionApps = absPath.substring(i);
        String[] parts = pathAfterFusionApps.split("/");

        String product = "";

        if (absPath.contains("fusionapps/")) {
            if (parts.length < 5)
                return "";
            product = parts[4].trim().toUpperCase();
        }
        if (absPath.contains("fsm/")) {
            if (parts.length < 4)
                return "";
            product = parts[3].trim().toUpperCase();
        }

        return product;
    }

    public static void writeInModelClassInUIJarSheet(int rowNumber, String jarName, HashSet<String> entrys) {


        String productCellNumber = "A" + rowNumber;
        String uiJarCellNumber = "B" + rowNumber;
        String modelClassesCellNumber = "C" + rowNumber;

        String product = getProductName(jarName);

        String formattedEntries = "";
        if (entrys != null) {
            Iterator<String> entrysIter = entrys.iterator();
            while (entrysIter.hasNext()) {
                formattedEntries = formattedEntries + " \n " + entrysIter.next();
            }
        }
        m_ModelInUiJarsSheet.addCell(new Cell(new CellLocation(productCellNumber), new CellValue(product)));
        m_ModelInUiJarsSheet.addCell(new Cell(new CellLocation(uiJarCellNumber), new CellValue(jarName)));
        m_ModelInUiJarsSheet.addCell(new Cell(new CellLocation(modelClassesCellNumber), new CellValue(formattedEntries)));
    }


    public void applyFix(String absPath, String newFileContents) throws Exception {
        File file = new File(absPath);
        if (!file.exists()) {
            throw new Exception("while making fix, file not found: " + absPath);
        }
        diffHelper.checkoutAndDelete(file);

        FileWriter fw = new FileWriter(file.getAbsolutePath());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(newFileContents);
        if (bw != null)
            bw.close();
        if (fw != null)
            fw.close();
        diffHelper.checkinAndDiff(file);
    }

    private String getLabel(String filePath) {
        if (filePath.contains("/fusionapps")) {
            int fusionappsIndex = filePath.indexOf("/fusionapps");
            String path_before_fusionapps = filePath.substring(0, fusionappsIndex);
            String[] parts = path_before_fusionapps.split("/");
            if (parts == null || parts.length < 2)
                return "";
            String label = parts[parts.length - 1];
            String[] series = parts[parts.length - 2].split("\\.");
            if (series == null || series.length < 2)
                return label;
            return series[1] + "_" + label;
        }
        if (filePath.contains("/fsm")) {
            int fusionappsIndex = filePath.indexOf("/fsm");
            String path_before_fusionapps = filePath.substring(0, fusionappsIndex);
            String[] parts = path_before_fusionapps.split("/");
            if (parts == null || parts.length < 2)
                return "";
            String label = parts[parts.length - 1];
            String[] series = parts[parts.length - 2].split("\\.");
            if (series == null || series.length < 1)
                return label;
            return series[0] + "_" + label;
        }
        return "";
    }
    
    private void getFamilyFromLabel(){
      if(label.equals("")) return;
      if(label.contains("FINM6B"))
          family = "FIN";
      else if(label.contains("HCMM6C"))
          family = "HCM";
      else if(label.contains("SCMM6B"))
          family = "SCM";
      else if(label.contains("CRMPRMM6B"))
          family = "CRM";
      else if(label.contains("PRJM6B"))
          family = "PRJ";
      else if(label.contains("PRCM6C"))
          family = "PRC";
      else if(label.contains("ATKM6B"))
          family = "ATF";
      else if(label.contains("FORDM6B"))
          family = "FSCM";
      else if(label.contains("FSM_MAIN"))
          family = "FSM";
      else if(label.contains("ICM6B"))
          family = "IC";
    }
    
  private static String getFamilyFromJar(String jars){
    String jarName = jars.split(",")[0].trim();
    String family = "";
    if(jarName.contains("AdfFin"))
        family = "FIN";
    else if(jarName.contains("AdfHcm"))
        family = "HCM";
    else if(jarName.contains("AdfIc"))
        family = "IC";
    else if(jarName.contains("AdfContracts") || jarName.contains("AdfCrm") || 
            jarName.contains("AdfSales") || jarName.contains("AdfMkt") ||
            jarName.contains("AdfMkl") || jarName.contains("AdfZpm") ||
            jarName.contains("AdfBase") || jarName.contains("AdfOppty") ||
            jarName.contains("AdfPartner") || jarName.contains("AdfApp") ||
            jarName.contains("AdfFoundation") || jarName.contains("AdfCustomer") ||
            jarName.contains("AdfOrder") || jarName.contains("AdfOc") ||
            jarName.contains("AdfExtn") || jarName.contains("AdfCc"))
        family = " CRM";
    else if(jarName.contains("AdfScm") || jarName.contains("AdfEgp") ||
            jarName.contains("AdfEgo") || jarName.contains("AdfCmr") ||
            jarName.contains("AdfInv") || jarName.contains("AdfRcv") ||
            jarName.contains("AdfWsh") || jarName.contains("AdfCst") ||
            jarName.contains("AdfMsc") || jarName.contains("AdfDoo") ||
            jarName.contains("AdfJmf"))
        family = "SCM";
    else if(jarName.contains("AdfPjo") || jarName.contains("AdfPjs") ||
            jarName.contains("AdfPjc") || jarName.contains("AdfPjf"))
        family = "PRJ";
    else if(jarName.contains("AdfPrc"))
        family = "PRC";
    else if(jarName.contains("AdfFunctional") || jarName.contains("AdfSetup") || 
            jarName.contains("AdfFeature") || jarName.contains("AdfResources") || 
            jarName.contains("AdfTopology") || jarName.contains("AdfAsk") || jarName.contains("AdfAsm"))
        family = "FSM";
    
    return family;
  }
}
