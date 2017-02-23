package oracle.apps.references;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Date;

import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CleanInvalidJarReferences {

    static boolean ignoreHangingRefs = false;
    private static int releaseNum=0;

    /**
     * Number of jprs seen
     */
    static int m_jprSeen = 0;

    /**
     * Number of jprs fixed
     */
    static int m_jprFixed = 0;

    /**
     * Number of jprs fixed
     */
    static int m_hangingRefsJprFixed = 0;

    /**
     * Number of jprs fixed
     */
    static int m_nonExistentRefsJprFixed = 0;

    /**
     * Number of library files fixed
     */
    static int m_libraryFixed = 0;

    /**
     * Number of library files fixed
     */
    static int m_librarySeen = 0;


    /**
     * Name of the diff file
     */
    static String m_diffFileName = "diff.txt";

    /**
     * Hash set of all the invalid jars that are to be used as a comparison point for the jprs
     */
    HashSet<String> invalidJars = new HashSet<String>();

    HashSet<String> crmInvalidJars = new HashSet<String>();

    /**
     * Hash set of all the invalid jars that are to be used as a comparison point for the jprs
     */
    HashSet<String> nonExistentJarsRemoved = new HashSet<String>();

    /**
     * Diff helper to write diff output to the file
     */
    AdeDiffHelper diffHelper = new AdeDiffHelper(m_diffFileName);

    /**
     * debug flag - used to print sop statements when in debug mode
     */
    boolean bDebug = false;

    /**
     * Set of invalid libraries removed in the entire scan
     */
    HashSet<String> libraryRefsRemoved = new HashSet<String>();

    /**
     * Set of libraries moved to the adf library due to bad placement
     */
    HashSet<String> librariesMovedToADF = new HashSet<String>();
    boolean removeNonExistentJarRefs = true;

    public static BufferedWriter writer;
    
    static ArrayList<String> listOfErrorMessages = new ArrayList<String>();

    public CleanInvalidJarReferences() {
        super();
    }

    public static void setSystemProperties() {
        String ignoreIsolatedRefs = System.getProperty("ignoreHangingRefs");
        if (!isEmpty(ignoreIsolatedRefs) && "true".equals(ignoreIsolatedRefs))
            ignoreHangingRefs = true;
        else if (!isEmpty(ignoreIsolatedRefs) && "false".equals(ignoreIsolatedRefs))
            ignoreHangingRefs = false;
    }

    public void initializeCrmInvalidJars() {
        // CRM Jars that are invalid - added on: 17-Oct-10. Kix : https://kix.oraclecorp.com/KIX/display.php?labelId=3353&articleId=269477
        crmInvalidJars.add("AdfCustomerCtrApplication360PublicUi.jar");
        //Resource jars were removed as per discussion with Anand kalidindi on 15-apr-11
        //crmInvalidJars.add("AdfCustomerCtrApplication360PublicResource.jar");
        crmInvalidJars.add("AdfCustomerCtrInfra360PublicModel.jar");
        crmInvalidJars.add("AdfCustomerCtrInfra360Publicui.jar");
        crmInvalidJars.add("AdfCustomerCtrApplication360PublicUi.jar");
        crmInvalidJars.add("AdfCustomerCtrInfra360PublicViewEcsf.jar");
      //Resource jars were removed as per discussion with Anand kalidindi on 15-apr-11
      //  crmInvalidJars.add("AdfCustomerCtrInfra360PublicResource.jar");
        crmInvalidJars.add("AdfCrmCommonSearchPublicModel.jar");
        crmInvalidJars.add("AdfCrmCommonSearchPublicUi.jar");
        // This jar was removed on Apr-12-11 as per mail discussion by Anand Kalidindi & Ravi Chandanala
        //crmInvalidJars.add("AdfCrmCommonSalesPartiesPublicEntity.jar");
        crmInvalidJars.add("AdfCrmCommonSalesPartiesPublicModel.jar");
        crmInvalidJars.add("AdfCrmCommonSalesPartiesPublicUi.jar");
    }

    /**
     * set of all the invalid jars..
     * If jpr contains anyjar that is present in this list, it will be removed from the jpr
     */
    public void initializeInvalidJars() {
        invalidJars.add("Saxpath.jar");
        invalidJars.add("Jdom-1.0.jar");
        invalidJars.add("Jaxen-1.1.1.jar");
        invalidJars.add("Jaxen-core.jar");
        invalidJars.add("Jaxen-jdom.jar");
        invalidJars.add("Jtestcase_4.0.0.jar");
        invalidJars.add("Org.jicengine-2.0.0.jar");
        invalidJars.add("JarAdfbcUnitTestHelper.jar");
        invalidJars.add("Bipclientapi.jar");
        invalidJars.add("Ess.jar");
        invalidJars.add("MonitorProcesses-Model.jar");
        invalidJars.add("MonitorProcesses-View.jar");
        invalidJars.add("SRS-Model.jar");
        invalidJars.add("SRS-View.jar");
        invalidJars.add("Oc4j.jar");
        invalidJars.add("Xdocore.jar");
        invalidJars.add("AdfAppsTracePublicUi.jar");
        invalidJars.add("AdfAppsTraceProtectedModel.jar");

    }

    /**
     * Usage:  CleanInvalidJarReferences.sh <path to your code in ade>  <doADE>
     * @param args
     */
    public static void main(String[] args) throws Exception{

        // Print Statistics
        Date startDate = new Date();

        if (args.length < 1 || isEmpty(args[0])) {
            System.out.println("Usage:  CleanInvalidJarReferences.sh <path to your code in ade>  <doADE>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        
            if(args.length > 1){
                String series = args[1].trim();
                String release= FamilyModuleHelper.getRelease(series);
                try{              
                    releaseNum = Integer.parseInt(release);
                }catch(NumberFormatException e){
                   e.printStackTrace();
                   releaseNum=0;
                }
            }
            
        FamilyModuleHelper.generateDeferedPaths();
        
        Boolean bDoADE = true;
        if(args.length > 2)
            bDoADE = new Boolean(args[2]);

        setSystemProperties();


        System.out.println("============= Analyzing My Directory " + sCrawlDir);

        CleanInvalidJarReferences invalidJarCleaner = new CleanInvalidJarReferences();
        invalidJarCleaner.initializeInvalidJars();
        invalidJarCleaner.initializeCrmInvalidJars();
        try {
            writer = new BufferedWriter(new FileWriter("invalidJarReferences.csv"));
            writer.write("family,module,product,filename,label,issue,subissue,description\n");
            if (bDoADE) 
                invalidJarCleaner.diffHelper.startDiffLog();
            invalidJarCleaner.crawlDirectory(sCrawlDir, bDoADE);

            if (bDoADE.booleanValue()) {
                invalidJarCleaner.diffHelper.closeDiffLog();
            }            

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally{
                writer.close();
        }

        String statistics = printStatistics(startDate);
        System.out.println(statistics);

        if (invalidJarCleaner.libraryRefsRemoved != null) {
            System.out.println("Following libraries were removed (" + invalidJarCleaner.libraryRefsRemoved.size() + ")");
            printMap(invalidJarCleaner.libraryRefsRemoved);
        }
        if (invalidJarCleaner.librariesMovedToADF != null) {
            System.out.println("Following libraries were moved to ADF Librarys (" +
                               invalidJarCleaner.librariesMovedToADF.size() + ")");
            printMap(invalidJarCleaner.librariesMovedToADF);
        }
        if (invalidJarCleaner.nonExistentJarsRemoved != null) {
            System.out.println("Number of non existent jars found: " + invalidJarCleaner.nonExistentJarsRemoved.size());
            printMap(invalidJarCleaner.nonExistentJarsRemoved);
        }
        if (listOfErrorMessages != null && listOfErrorMessages.size() > 0) {
            System.out.println("===================");
            System.out.println("ERRORS");
            System.out.println("===================");
            Iterator<String> errorIter = listOfErrorMessages.iterator();
            int cnt = 1;
            while (errorIter.hasNext()) {
                System.out.println(String.format("%d. %s", cnt, errorIter.next()));
                cnt++;
            }
        }
        String mailSummary = getMailSummary(sCrawlDir, statistics);

//        Mail.sendMail("zeesha.currimbhoy@oracle.com", mailSummary,
//                      "SCRIPT: CleanInvalidJarReferences:  run at: " + (new Date()).toString());

    }

    public static String getMailSummary(String crawlDir, String stats) {
        StringBuffer mailSummary = new StringBuffer();
        mailSummary.append("\n Script was run on: " + crawlDir);
        mailSummary.append("\n Stats: \n");
        mailSummary.append(stats);
        return mailSummary.toString();
    }

    /**
     * Crawls the directory and for every jpr or adf library found
     * - removes the invalid jar references
     * - for every invalid library name
     *      - move any valid adf libraries that it contains to the ADF Library
     *      - if there are no other libraries present - remove the entire library name
     * @param path
     * @param bDoADE
     */
    private void crawlDirectory(String path, Boolean bDoADE) throws Exception {
        if (path.contains(".ade_path"))
            return;
        
        if(FamilyModuleHelper.isDeferredPath(releaseNum, FamilyModuleHelper.getPathAfterViewRoot(path)))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (isValidJpr(absFilePath)) {
                    if (bDebug)
                        System.out.println("Analyzing jpr: " + absFilePath);
                    processJpr(absFilePath, bDoADE);
                    if (!ignoreHangingRefs)
                        removeHangingInvalidLibraryRefs(absFilePath, bDoADE);

                    if (removeNonExistentJarRefs) {
                        removeNonExistentLibraryRefs(absFilePath, bDoADE);
                    }
                } else if (isValidLibraryFile(absFilePath)) {
                    if (bDebug)
                        System.out.println("Analyzing library: " + absFilePath);
                    processADFLibraryFile(absFilePath, bDoADE);
                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidDirectory(dir)) {
                    crawlDirectory(dir, bDoADE);
                }
            } else {
                System.out.println("ERROR:  node is neither file or directory: " + listOfFiles[i].getAbsolutePath());
            }
        }
    }

    private void writeRefsToBeMovedToScannerOutput(String jprPath, ArrayList<Node> validAdfLibsToBemoved) throws Exception {
        if (validAdfLibsToBemoved == null || validAdfLibsToBemoved.size() < 1)
            return;

        Iterator<Node> validLibIter = validAdfLibsToBemoved.iterator();
        while (validLibIter.hasNext()) {
            Node ref = validLibIter.next();
            String urlPath = XMLParserHelper.getAttributeValue(ref, "path");
            File f = new File(jprPath);
            String canonicalPath = f.getCanonicalPath();
            if(!urlPath.contains("AdfFinUiUtilPublicUi.jar")) //SU: Remove FinUiUtil from being reported as violation due to uptake of helper library 
                writer.write(FamilyModuleHelper.getFileNameInfo(canonicalPath) + "InvalidRef,Move Library ref to ADF Library section," + urlPath + "\n");
        }
    }

    public void processJpr(String absPath, boolean bDoADE) {

        m_jprSeen++;

        XMLDocument jprXml = XMLParserHelper.getXMLDocument(absPath);
        XmlFixer fixer = null;
        boolean bFixMade = false;

        try {
            fixer = new XmlFixer(absPath);
        } catch (Exception ex) {
            System.out.println("Skipping file.. could not initialze fixer: " + absPath);
            return;
        }

        if (jprXml == null) {
            System.out.println("Skipping file .. Could not parse jpr as xml " + absPath);
            return;
        }

        HashSet<String> allAdfLibUrls = JprHelper.getAllAdfLibrarysInJpr(absPath, false);
        HashSet<String> alladfbcImportUrls = JprHelper.getAllBcImportsInJpr(absPath, false);
        HashSet<String> invalidLibraryNamesRemoved = new HashSet<String>();

        jprXml.setProperty("SelectionLanguage", "XPath");
        try {

            File f = new File(absPath);
            String canonicalPath = f.getCanonicalPath();
            // get the libraryDefinitions node
            NodeList libraryDefsNodeList = jprXml.selectNodes("//hash[@n='internalDefinitions']/list[@n='libraryDefinitions']");
            if (libraryDefsNodeList == null || libraryDefsNodeList.getLength() == 0) {
                return;
            }
            if (!XMLParserHelper.hasOneNOnlyOneNode(libraryDefsNodeList)) {
                System.out.println("Skipping.. Expecting only one libraryDefnitions node: " + absPath);
                return;
            }
            Node libDefn = libraryDefsNodeList.item(0);

            // process every child of this library definition node
            NodeList libDefnsChildren = libDefn.getChildNodes();
            if (libDefnsChildren == null)
                return;

            int libDefnsChildCnt = libDefnsChildren.getLength();
            for (int i = 0; i < libDefnsChildCnt; i++) {
                Node childHash = libDefnsChildren.item(i);

                if (XMLParserHelper.isNodeName(childHash, "hash")) {

                    // clean up all invalid class paths
                    ArrayList<Node> invalidUrlNodes = getListOfInvalidUrlNodes(absPath, childHash);

                    if (invalidUrlNodes != null && invalidUrlNodes.size() > 0) {
                        for (int invalidCnt = 0; invalidCnt < invalidUrlNodes.size(); invalidCnt++) {
                            Node invalidUrl = invalidUrlNodes.get(invalidCnt);
                            String jarRef = XMLParserHelper.getAttributeValue(invalidUrl, "path");
                            try {
                                if (bDebug) {
                                    String msg = String.format("Removing invalid Url: %s in Jpr %s", jarRef, absPath);

                                    System.out.println(msg);
                                }
                                // Remove this line
                                fixer.removeNode(invalidUrl);
                                bFixMade = true;
                                libraryRefsRemoved.add(jarRef);
                                writer.write(FamilyModuleHelper.getFileNameInfo(canonicalPath) + "InvalidRef,Invalid Library Reference," + jarRef + "\n");
                            } catch (Exception ex1) {
                                String msg =
                                    String.format("Skipping file  %s, Exception occurred while trying to remove node with jar ref:  %s",
                                                  jarRef, absPath);
                                System.out.println(msg);
                                throw new Exception(ex1);
                            }

                        }
                    }
                    if (!ignoreHangingRefs) {
                        // Process all invalid libraries - if there is any node
                        Node libraryNameNode =
                            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(childHash, "value", "n", "id");
                        if (libraryNameNode != null) {
                            String libraryName = XMLParserHelper.getAttributeValue(libraryNameNode, "v");
                            if (isInvalidLibraryName(libraryName)) {


                                if (bDebug) {
                                    System.out.println("Invalid library name found: " + libraryName + " jpr: " + absPath);
                                }
                                ArrayList<Node> validAdfLibs = getListOfValidADFLibUrlNodes(absPath, childHash);

                                try {
                                    if (validAdfLibs != null && validAdfLibs.size() > 0) {
                                        // Sudipti - Scanner requirement
                                        writeRefsToBeMovedToScannerOutput(absPath, validAdfLibs);

                                        moveValidRefsToADFLibraryNode(validAdfLibs, allAdfLibUrls, alladfbcImportUrls, jprXml,
                                                                      fixer, absPath);
                                        bFixMade = true;


                                    }

                                } catch (Exception validLibMoveEx) {
                                    String msg =
                                        " Exception while moving valid adf library nodes to adf library..:  " + validLibMoveEx.getMessage();
                                    System.out.println(msg);
                                    throw new Exception(msg, validLibMoveEx);
                                }
                                boolean bCanRemoveParentHash = false;
                                // If there is a node that is in the parent, but is not in valid adf list, or in the invalid url list - then you cannot remove the has node
                                if (isAnlyUrlRemaining(validAdfLibs, invalidUrlNodes, childHash)) {
                                    bCanRemoveParentHash = false;
                                } else {
                                    bCanRemoveParentHash = true;
                                }

                                if (bCanRemoveParentHash) {
                                    if (bDebug) {
                                        // all child url nodes were removed - safe to remove this parent hash node
                                        System.out.println("removing parent hash Node");
                                    }
                                    try {
                                        // Remove this line
                                        fixer.removeNode(childHash);
                                        invalidLibraryNamesRemoved.add(libraryName);


                                        bFixMade = true;
                                    } catch (Exception ex1) {
                                        String msg =
                                            String.format("Skipping file  %s, Exception occurred while trying to remove node with hash node ref:  %s",
                                                          absPath, ex1.getMessage());
                                        System.out.println(msg);
                                        throw new Exception(msg, ex1);
                                    }
                                }


                            }


                        } //
                    }
                }


            } // end of processing every lib

            // If any iinvalid library names were removed - remove their references as well..
            if (invalidLibraryNamesRemoved != null && invalidLibraryNamesRemoved.size() > 0) {
                if (bDebug) {
                    System.out.println("Invalid library names were removed.. will remove any references to them as well");
                }
                Iterator<String> libNamesRemovedIter = invalidLibraryNamesRemoved.iterator();
                while (libNamesRemovedIter.hasNext()) {

                    String libNameRemoved = libNamesRemovedIter.next();
                    if (bDebug) {
                        System.out.println("lib name removed: " + libNameRemoved);
                    }
                    if (isEmpty(libNameRemoved)) {
                        continue;
                    }
                    ArrayList<Node> list_libraryHashNodeRefrenced =
                        getLibraryReferencedNodes(jprXml, libNameRemoved.trim(), absPath);
                    boolean libraryReferencenodeRemoved = removeAllNodesInList(list_libraryHashNodeRefrenced, fixer);
                    if (libraryReferencenodeRemoved) {
                        bFixMade = true;
                    }

                    ArrayList<Node> list_exportedReferences = getExportedLibraryRefsNode(jprXml, libNameRemoved.trim(), absPath);
                    boolean exportedNodesRemoved = removeAllNodesInList(list_exportedReferences, fixer);
                    if (exportedNodesRemoved) {
                        bFixMade = true;
                    }
                    /*if (list_exportedReferences == null) {
                        if (bDebug) {
                            System.out.println("Library reference node not found");
                        }
                    }
                    if (bDebug) {
                        System.out.println("Found " +
                                           list_exportedReferences.size() +
                                           " references to library name: " +
                                           libNameRemoved);
                    }
                    for (int exportedCnt = 0;
                         exportedCnt < list_exportedReferences.size();
                         exportedCnt++) {
                        try {
                            Node libraryRefNode =
                                list_exportedReferences.get(exportedCnt);
                            fixer.removeNode(libraryRefNode);
                        } catch (Exception libRemovalEx) {
                            System.out.println("Exception while trying to remove referenced library name: " +
                                               libNameRemoved);
                            throw new Exception(libRemovalEx);
                        }
                    }*/
                }


            }

            //TODO: FIX FOR LEDGER ALREADY MERGED IN TXN.. Please remove.. this is only a fix for ledger
            /* ArrayList<Node> list_libraryHashNodeRefrenced =
                getInvalidLibraryReferencedNodes(jprXml, absPath);
            boolean bRemovedAnyNodes =
                removeAllNodesInList(list_libraryHashNodeRefrenced, fixer);
            if (bRemovedAnyNodes) {
                bFixMade = true;
            }*/
            //TODO: [END] FIX FOR LEDGER ALREADY MERGED IN TXN.. Please remove.. this is only a fix for ledger


        } catch (Exception ex) {
            String msg = String.format("Skipping %s.. Exception %s", absPath, ex.getMessage());
            System.out.println(msg);
            return;
        }
        if (bFixMade) {
            try {
                if (bDoADE) {
                    String newFileContents = fixer.getFileContentsAsString(true);
                    applyFix(absPath, newFileContents);
                }
            } catch (Exception ex) {
                System.out.println("Exception occurred while apply fix for file: " + absPath);
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }

            m_jprFixed++;
        }
    }

    public Node getLibraryDefintionNodeWithId(XMLDocument jprXml, String libraryName) {
        try {
            // get the libraryDefinitions node
            NodeList libraryDefsNodeList = jprXml.selectNodes("//hash[@n='internalDefinitions']/list[@n='libraryDefinitions']");
            if (libraryDefsNodeList == null || libraryDefsNodeList.getLength() == 0) {
                return null;
            }
            if (!XMLParserHelper.hasOneNOnlyOneNode(libraryDefsNodeList)) {
                throw new Exception("one and only one node expected");
            }

            Node libDefn = libraryDefsNodeList.item(0);
            // process every child of this library definition node
            NodeList libDefnsChildren = libDefn.getChildNodes();
            if (libDefnsChildren == null)
                return null;

            int libDefnsChildCnt = libDefnsChildren.getLength();
            for (int i = 0; i < libDefnsChildCnt; i++) {
                Node childHash = libDefnsChildren.item(i);
                if (XMLParserHelper.isNodeName(childHash, "hash")) {
                    Node libraryIdNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(childHash, "value", "n", "id");
                    if (libraryIdNode == null) {
                        continue;
                    }
                    String libName = XMLParserHelper.getAttributeValue(libraryIdNode, "v");
                    if (!isEmpty(libName) && libName.trim().equals(libraryName))
                        return childHash;
                }
            }
        } catch (Exception ex) {
            System.out.println("Exception occurred while processing getLibraryDefintionNodeWithId. " + ex.getMessage());
        }
        return null;
    }

    public void removeHangingInvalidLibraryRefs(String jprFullPath, boolean bDoADE) {
        if (bDebug)
            System.out.println("Processing jpr: " + jprFullPath);
        boolean bFixMade = false;
        try {
            XMLDocument jprXml = XMLParserHelper.getXMLDocument(jprFullPath);
            jprXml.setProperty("SelectionLanguage", "XPath");
            if (jprXml == null) {
                return;
            }

            XmlFixer fixer = new XmlFixer(jprFullPath);
            if (fixer == null) {
                return;
            }

            ArrayList<Node> list_libraryHashNodeRefrenced = getInvalidLibraryReferencedNodes(jprXml, jprFullPath);
            if (list_libraryHashNodeRefrenced == null || list_libraryHashNodeRefrenced.size() <= 0)
                return;

            Iterator<Node> libraryHashNodeRefIter = list_libraryHashNodeRefrenced.iterator();
            while (libraryHashNodeRefIter.hasNext()) {
                Node libraryHashNodeRef = libraryHashNodeRefIter.next();
                Node idNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(libraryHashNodeRef, "value", "n", "id");
                if (idNode == null) {
                    continue;
                }
                String libraryName = XMLParserHelper.getAttributeValue(idNode, "v");
                if (isEmpty(libraryName)) {
                    continue;
                }
                // check if the library has a definition. if it has a definition do not remove it - continue
                Node definitionNode = getLibraryDefintionNodeWithId(jprXml, libraryName);
                if (definitionNode != null) {
                    continue;
                }

                // if there is no definition, then remove the ref and exported refs
                ArrayList<Node> list_exportedReferences = getExportedLibraryRefsNode(jprXml, libraryName, jprFullPath);
                boolean exportedNodesRemoved = removeAllNodesInList(list_exportedReferences, fixer);
                if (exportedNodesRemoved) {
                    bFixMade = true;
                }

                fixer.removeNode(libraryHashNodeRef);
                bFixMade = true;
            }

            if (bFixMade) {
                String newFileContents = fixer.getFileContentsAsString(true);
                try {
                    if (bDoADE) {
                        applyFix(jprFullPath, newFileContents);
                    }
                } catch (Exception ex) {
                    System.out.println("Exception occurred while apply fix for file: " + jprFullPath);
                    System.out.println(ex.getMessage());
                    ex.printStackTrace();
                }

                m_hangingRefsJprFixed++;
            }


        } catch (Exception ex) {
            System.out.println("Exception while processing: " + jprFullPath + " Exception: " + ex.getMessage());
        }
    }


    public void removeNonExistentLibraryRefs(String jprFullPath, boolean bDoADE) {
        if (bDebug)
            System.out.println("[Non existent library check]Processing jpr: " + jprFullPath);
        boolean bFixMade = false;
        try {
            File f = new File(jprFullPath);
            String canonicalPath = f.getCanonicalPath();
            XMLDocument jprXml = XMLParserHelper.getXMLDocument(jprFullPath);
            jprXml.setProperty("SelectionLanguage", "XPath");
            if (jprXml == null) {
                return;
            }

            XmlFixer fixer = new XmlFixer(jprFullPath);
            if (fixer == null) {
                return;
            }


            // get the libraryDefinitions classpath node
            NodeList classPathNodes =
                jprXml.selectNodes("//hash[@n='internalDefinitions']/list[@n='libraryDefinitions']/hash/list[@n='classPath']");
            if (classPathNodes == null || classPathNodes.getLength() == 0) {
                return;
            }
            // for each ref - if non existent - remove
            int classPathLen = classPathNodes.getLength();
            for (int i = 0; i < classPathLen; i++) {
                Node classPathNode = classPathNodes.item(i);
                if (!classPathNode.hasChildNodes()) {
                    continue;
                }
                NodeList urlNodes = classPathNode.getChildNodes();
                int urlNodeLen = urlNodes.getLength();
                for (int j = 0; j < urlNodeLen; j++) {
                    Node urlNode = urlNodes.item(j);
                    String urlNodeName = urlNode.getNodeName();
                    if (isEmpty(urlNodeName) || !urlNodeName.trim().equals("url")) {
                        continue;
                    }
                    String urlPath = XMLParserHelper.getAttributeValue(urlNode, "path");
                    boolean jarExists = doesJarExist(jprFullPath, urlPath);
                    if (!jarExists) {
                        // remove node
                        fixer.removeNode(urlNode);
                        bFixMade = true;
                        nonExistentJarsRemoved.add(urlPath);
                        if(!urlPath.contains("/opmn") && !urlPath.contains("AdfExtnContentPublicModelCommon.jar")) //SU: opmn folder does not exist when script is run from ade label server
                            writer.write(FamilyModuleHelper.getFileNameInfo(canonicalPath) + "InvalidRef,Non-Existent Jar Ref," + urlPath + "\n");
                    }
                }

            }
            if (bFixMade) {
                String newFileContents = fixer.getFileContentsAsString(true);
                try {
                    if (bDoADE) {
                        applyFix(jprFullPath, newFileContents);
                    }
                } catch (Exception ex) {
                    System.out.println("Exception occurred while apply fix for file: " + jprFullPath);
                    System.out.println(ex.getMessage());
                    ex.printStackTrace();
                }

                m_nonExistentRefsJprFixed++;
            }


        } catch (Exception ex) {
            System.out.println("Exception while processing: " + jprFullPath + " Exception: " + ex.getMessage());
        }
    }


    private static String getPrevDirectory(String absPath) {

        int indexLastSlash = absPath.lastIndexOf("/");
        if (indexLastSlash != -1) {
            return absPath.substring(0, indexLastSlash);
        }


        return null;
    }

    public boolean doesJarExist(String jprPath, String jarPath) {
        if (isEmpty(jprPath)) {
            return false;
        }
        // [Zeesha] Modified 28-Jul-10. This is because sometimes the url path contains ! at the end
        // e.g <url path="../../../jlib/AdfFinGlJrnlEntriesModel.jar!/>"
        // though this jar exists if there is a ! at the end, the logic will mark it as non existent
        // Though at some level it is correct that we should not have these characters, want to follow a more conservative
        // approach and not remove these guys.. hence check that the url path always ends with .jar before marking as non existent
        if (!jarPath.trim().endsWith(".jar"))
            return true;
        String baseDir = getPrevDirectory(jprPath);
        String absPath = baseDir + "/" + jarPath;
        File f = new File(absPath);
        if (f.exists())
            return true;

        return false;


    }

    public boolean removeAllNodesInList(ArrayList<Node> listNodesToRemove, XmlFixer fixer) throws Exception {
        boolean bFixMade = false;
        if (listNodesToRemove == null) {
            if (bDebug) {
                System.out.println("No nodes to remove");
            }
            //  no fix made
            return false;
        }
        if (bDebug) {
            System.out.println("Need to remove " + listNodesToRemove.size() + " reference nodes ");
        }
        for (int libCnt = 0; libCnt < listNodesToRemove.size(); libCnt++) {
            try {
                Node libraryRefNode = listNodesToRemove.get(libCnt);
                fixer.removeNode(libraryRefNode);
                bFixMade = true;
            } catch (Exception libRemovalEx) {
                System.out.println("Exception while trying to remove referenced library name: ");
                throw new Exception(libRemovalEx);
            }

        }
        return bFixMade;
    }

    public void processADFLibraryFile(String libraryFilePath, boolean bDoADE) {
        m_librarySeen++;
        boolean fixMade = false;
        boolean bremoveException = false;
        XmlFixer fixer = null;
        HashSet<String> localLibsRemoved = new HashSet<String>();
        try {
            fixer = new XmlFixer(libraryFilePath);
        } catch (Exception ex) {
            System.out.println("Unable to initialize fixer for path: " + libraryFilePath);
        }
        if (isEmpty(libraryFilePath)) {
            return;
        }

        XMLDocument libraryXml = XMLParserHelper.getXMLDocument(libraryFilePath);
        if(libraryXml == null)
            return;

        NodeList libraryRootNode = libraryXml.getElementsByTagName("JLibraryNode");
        if (!XMLParserHelper.hasOneNOnlyOneNode(libraryRootNode)) {
            System.out.println("Library reference exepected one and only one JLibrary node");
        }
        Node libraryNode = libraryRootNode.item(0);
        Node classPathNode = XMLParserHelper.getChildNodeWithName(libraryNode, "classPath");
        if (classPathNode == null) {
            return;
        }
        Node entryNode = XMLParserHelper.getChildNodeWithName(classPathNode, "entries");
        if (entryNode == null) {
            return;
        }

        NodeList itemNodes = entryNode.getChildNodes();
        if (itemNodes == null) {
            return;
        }
        int len = itemNodes.getLength();
        for (int i = 0; i < len; i++) {
            Node itemNode = itemNodes.item(i);
            String nodeName = itemNode.getNodeName();
            if (!isEmpty(nodeName) && nodeName.trim().equals("Item")) {
                String jarPath = XMLParserHelper.getAttributeValue(itemNode, "path");
                if(isJarPathExempted(jarPath))
                    continue;
                if (!isEmpty(jarPath) && isInvalidJarRef(libraryFilePath, jarPath)) {
                    try {
                        // have to remove this node
                        fixer.removeNode(itemNode);
                        localLibsRemoved.add(jarPath);
                        File f = new File(libraryFilePath);
                        String canonicalPath = f.getCanonicalPath();
                        writer.write(FamilyModuleHelper.getFileNameInfo(canonicalPath) + "InvalidRef,Invalid Library Reference," + jarPath + "\n"); //SU: LocalLibs was not reported earlier
                    } catch (Exception ex1) {
                        bremoveException = true;
                        System.out.println("Exception occurred while processing library: (Skipping) " + libraryFilePath);
                        return;
                    }
                    fixMade = true;
                } else if (!isEmpty(jarPath) && !doesJarExist(libraryFilePath, jarPath) && removeNonExistentJarRefs) {
                    try {
                        // have to remove this node
                        fixer.removeNode(itemNode);
                        nonExistentJarsRemoved.add(jarPath);
                        File f = new File(libraryFilePath);
                        String canonicalPath = f.getCanonicalPath();
                        if(!jarPath.contains("AdfExtnContentPublicModelCommon.jar"))
                            writer.write(FamilyModuleHelper.getFileNameInfo(canonicalPath) + "InvalidRef,Non-Existent Jar Ref," + jarPath + "\n");
                    } catch (Exception ex1) {
                        bremoveException = true;
                        System.out.println("Exception occurred while processing library: (Skipping) " + libraryFilePath);
                        return;
                    }
                    fixMade = true;
                }
            }
        }

        if (fixMade && !bremoveException) {
          
            try {
                if (bDoADE) {
                    String newFileContents = fixer.getFileContentsAsString(true);
                    applyFix(libraryFilePath, newFileContents);
                    m_libraryFixed++;                    
                }
              libraryRefsRemoved.addAll(localLibsRemoved);
            } catch (Exception ex) {
                System.out.println("Exception occurred while apply fix for file: " + libraryFilePath);
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    public ArrayList<Node> getLibraryReferencedNodes(XMLDocument jprXml, String libraryName, String absPath) throws Exception {
        if (jprXml == null) {
            return null;
        }

        // require an array list because sometimes there are duplicate library reference nodes that reference the same library.
        // all these nodes will have to be removed.
        ArrayList<Node> list_libraryRefNodes = new ArrayList<Node>();

        NodeList libraryRefsNodeList = jprXml.selectNodes("//list[@n='libraryReferences']");
        // No libraryDefinitions found
        if (libraryRefsNodeList == null || libraryRefsNodeList.getLength() == 0) {
            throw new Exception("Could not find library references in jpr");
        }
        if (!XMLParserHelper.hasOneNOnlyOneNode(libraryRefsNodeList)) {
            System.out.println("Skipping.. Expecting only one library reference node: " + absPath);
            return null;
        }
        Node libraryRefNode = libraryRefsNodeList.item(0);
        NodeList librayRefsChildren = libraryRefNode.getChildNodes();
        for (int i = 0; i < librayRefsChildren.getLength(); i++) {
            Node childHash = librayRefsChildren.item(i);
            String nodeName = childHash.getNodeName();
            if (!isEmpty(nodeName) && nodeName.trim().equals("hash")) {
                Node idNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(childHash, "value", "n", "id");
                if (idNode != null) {
                    String idValue = XMLParserHelper.getAttributeValue(idNode, "v");
                    if (!isEmpty(idValue) && idValue.trim().equals(libraryName)) {
                        list_libraryRefNodes.add(childHash);
                    }
                }
            }
        }
        return list_libraryRefNodes;


    }
    //TODO: Please remove.. this is only for ledger

    public ArrayList<Node> getInvalidLibraryReferencedNodes(XMLDocument jprXml, String absPath) throws Exception {
        if (jprXml == null) {
            return null;
        }

        // require an array list because sometimes there are duplicate library reference nodes that reference the same library.
        // all these nodes will have to be removed.
        ArrayList<Node> list_libraryRefNodes = new ArrayList<Node>();

        NodeList libraryRefsNodeList = jprXml.selectNodes("//list[@n='libraryReferences']");
        // No libraryDefinitions found
        if (libraryRefsNodeList == null || libraryRefsNodeList.getLength() == 0) {
            if (bDebug)
                System.out.println("file: " + absPath + " no library references.. returning");
            return null;
        }
        if (!XMLParserHelper.hasOneNOnlyOneNode(libraryRefsNodeList)) {
            System.out.println("Skipping.. Expecting only one library reference node: " + absPath);
            return null;
        }
        Node libraryRefNode = libraryRefsNodeList.item(0);
        NodeList librayRefsChildren = libraryRefNode.getChildNodes();
        for (int i = 0; i < librayRefsChildren.getLength(); i++) {
            Node childHash = librayRefsChildren.item(i);
            String nodeName = childHash.getNodeName();
            if (!isEmpty(nodeName) && nodeName.trim().equals("hash")) {
                Node idNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(childHash, "value", "n", "id");
                if (idNode != null) {
                    String idValue = XMLParserHelper.getAttributeValue(idNode, "v");
                    if (!isEmpty(idValue) && (isInvalidLibraryName(idValue.trim()))) {
                        list_libraryRefNodes.add(childHash);
                    }
                }
            }
        }
        return list_libraryRefNodes;


    }

    public ArrayList<Node> getExportedLibraryRefsNode(XMLDocument jprXml, String libraryName, String absPath) throws Exception {
        if (jprXml == null) {
            return null;
        }

        // require an array list because sometimes there are duplicate library reference nodes that reference the same library.
        // all these nodes will have to be removed.
        ArrayList<Node> list_libraryRefNodes = new ArrayList<Node>();

        NodeList libraryRefsNodeList =
            jprXml.selectNodes("//hash[@n='oracle.jdevimpl.config.JProjectLibraries']/list[@n='exportedReferences']");
        // No libraryDefinitions found
        if (libraryRefsNodeList == null || libraryRefsNodeList.getLength() == 0) {
            throw new Exception("Could not find library references in jpr");
        }
        if (!XMLParserHelper.hasOneNOnlyOneNode(libraryRefsNodeList)) {
            System.out.println("Skipping.. Expecting only one library reference node: " + absPath);
            return null;
        }
        Node libraryRefNode = libraryRefsNodeList.item(0);
        NodeList librayRefsChildren = libraryRefNode.getChildNodes();
        for (int i = 0; i < librayRefsChildren.getLength(); i++) {
            Node childHash = librayRefsChildren.item(i);
            String nodeName = childHash.getNodeName();
            if (!isEmpty(nodeName) && nodeName.trim().equals("hash")) {
                Node idNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(childHash, "value", "n", "id");
                if (idNode != null) {
                    String idValue = XMLParserHelper.getAttributeValue(idNode, "v");
                    if (!isEmpty(idValue) && idValue.trim().equals(libraryName)) {
                        list_libraryRefNodes.add(childHash);
                    }
                }
            }
        }
        return list_libraryRefNodes;


    }

    public boolean isAnlyUrlRemaining(ArrayList<Node> adflibNodesRemoved, ArrayList<Node> invalidLibsRemoved,
                                      Node parentHashNode) {
        if (parentHashNode == null) {
            return false;
        }


        Node listClasspathNode =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(parentHashNode, "list", "n", "classPath");
        if (listClasspathNode == null) {
            return false;
        }

        NodeList listUrlNodes = listClasspathNode.getChildNodes();
        // there are no remaining url nodes
        if (listUrlNodes == null || listUrlNodes.getLength() == 0) {
            return false;
        }
        for (int j = 0; j < listUrlNodes.getLength(); j++) {
            Node urlNode = listUrlNodes.item(j);
            if (XMLParserHelper.isNodeName(urlNode, "url")) {
                if (invalidLibsRemoved == null && adflibNodesRemoved == null) {
                    return true;
                }
                if ((invalidLibsRemoved != null && !invalidLibsRemoved.contains(urlNode)) &&
                    (adflibNodesRemoved != null && !adflibNodesRemoved.contains(urlNode))) {

                    return true;
                }

            }
        }
        return false;
    }

    public ArrayList<Node> getListOfInvalidUrlNodes(String jprOrLibPath, Node parentHashNode) {
        ArrayList<Node> invalidNodes = new ArrayList<Node>();

        if (parentHashNode == null) {
            return null;
        }


        Node listClasspathNode =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(parentHashNode, "list", "n", "classPath");
        if (listClasspathNode == null) {
            return null;
        }

        NodeList listUrlNodes = listClasspathNode.getChildNodes();
        for (int j = 0; j < listUrlNodes.getLength(); j++) {
            Node urlNode = listUrlNodes.item(j);
            if (XMLParserHelper.isNodeName(urlNode, "url")) {
                String jarRef = XMLParserHelper.getAttributeValue(urlNode, "path");
                if (!isEmpty(jarRef)) {
                    if (isInvalidJarRef(jprOrLibPath, jarRef)) {
                        invalidNodes.add(urlNode);
                    }
                } else {
                    System.out.println("[STRANGE] classpath entry thats not a jar: " + jarRef);
                }
            }
        }
        return invalidNodes;

    }

   
    public HashSet<String> getListOfAllUrlNodes(Node parentHashNode) {

        HashSet<String> urlNames = new HashSet<String>();
        if (parentHashNode == null) {
            return null;
        }


        Node listClasspathNode =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(parentHashNode, "list", "n", "classPath");
        if (listClasspathNode == null) {
            return null;
        }

        NodeList listUrlNodes = listClasspathNode.getChildNodes();
        for (int j = 0; j < listUrlNodes.getLength(); j++) {
            Node urlNode = listUrlNodes.item(j);
            if (XMLParserHelper.isNodeName(urlNode, "url")) {
                String jarRef = XMLParserHelper.getAttributeValue(urlNode, "path");
                if (!isEmpty(jarRef)) {
                    urlNames.add(jarRef);

                } else {
                    System.out.println("[STRANGE] classpath entry thats not a jar: " + jarRef);
                }
            }
        }
        return urlNames;

    }

    public static void printMap(HashSet<String> libs) {
        if (libs == null)
            return;
        Iterator<String> libsIter = libs.iterator();
        while (libsIter.hasNext()) {
            String lib = libsIter.next();
            System.out.println("Reference: " + lib);
        }
    }

    public ArrayList<Node> getListOfValidADFLibUrlNodes(String jprOrLibPath, Node parentHashNode) {
        ArrayList<Node> validAdfLibNodes = new ArrayList<Node>();
        HashSet<String> urlNames = new HashSet<String>();
        if (parentHashNode == null) {
            return null;
        }


        Node listClasspathNode =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(parentHashNode, "list", "n", "classPath");
        if (listClasspathNode == null) {
            return null;
        }

        NodeList listUrlNodes = listClasspathNode.getChildNodes();
        for (int j = 0; j < listUrlNodes.getLength(); j++) {
            Node urlNode = listUrlNodes.item(j);
            if (XMLParserHelper.isNodeName(urlNode, "url")) {
                String jarRef = XMLParserHelper.getAttributeValue(urlNode, "path");
                if (!isEmpty(jarRef)) {

                    if (isValidAdfJar(jprOrLibPath, jarRef)) {
                        /* if (urlNames.contains(jarRef.trim())) {
                            continue;
                        }*/
                        urlNames.add(jarRef.trim());
                        validAdfLibNodes.add(urlNode);
                    }
                } else {
                    System.out.println("[STRANGE] classpath entry thats not a jar: " + jarRef);
                }
            }
        }
        return validAdfLibNodes;

    }

    public void writeErrorForNodesCannotBeMoved_NoADF(String jprPath, ArrayList<Node> listValidLibsToBeMoved) {
        Iterator<Node> validRefsIter = listValidLibsToBeMoved.iterator();
        while (validRefsIter.hasNext()) {
            Node ref = validRefsIter.next();
            String urlPath = XMLParserHelper.getAttributeValue(ref, "path");
            listOfErrorMessages.add(String.format("jpr: %s : lib %s , cannot be moved due to no existing ADF Lib Node", jprPath,
                                                  urlPath));
        }
    }

    public void moveValidRefsToADFLibraryNode(ArrayList<Node> validRefs, HashSet<String> alladfLibRefs,
                                              HashSet<String> alladfbcImports, XMLDocument jprXml, XmlFixer fixer,
                                              String jprAbsPath) throws Exception {
        // dont need to move any references
        if (validRefs == null || validRefs.size() <= 0) {
            return;
        }

        if (jprXml == null)
            return;

        // get the adf library node
        Node adfLibHashNode = JprHelper.getADFLibraryNode(jprXml);
        if (adfLibHashNode == null) {
            // add error message that all the libs that should be removed cannot be
            writeErrorForNodesCannotBeMoved_NoADF(jprAbsPath, validRefs);
            throw new Exception("No existing adf library node");
        }

        Node listClasspathNode =
            XMLParserHelper.getChildNodeWithMatchingAttributeAndName(adfLibHashNode, "list", "n", "classPath");
        if (listClasspathNode == null) {
            writeErrorForNodesCannotBeMoved_NoADF(jprAbsPath, validRefs);
            throw new Exception("No existing adf library node");
        }
        Iterator<Node> validRefsIter = validRefs.iterator();
        try {
            while (validRefsIter.hasNext()) {
                Node ref = validRefsIter.next();
                String urlPath = XMLParserHelper.getAttributeValue(ref, "path");
                if (!isEmpty(urlPath) &&
                    ((alladfLibRefs != null && alladfLibRefs.contains(urlPath)) || (alladfbcImports != null &&
                                                                                    alladfbcImports.contains(urlPath)))) {
                    // remove from hash node - since the ref is already present under adf library or adfbcimport node

                    if (bDebug) {
                        String alreadypresentMsg =
                            String.format("Removing library: %s from has node since it is already present in ADF Library Node",
                                          urlPath);
                        System.out.println(alreadypresentMsg);
                    }
                    fixer.removeNode(ref);
                    continue;
                }
                // add as child to the listClassPathNode

                if (bDebug) {
                    String addMsg = String.format("Adding library: %s to ADF Library Node and removing from has node", urlPath);
                    System.out.println(addMsg);
                }
                fixer.addAsLastChildToNode(listClasspathNode, ref, "   ");
                alladfLibRefs.add(urlPath);
                librariesMovedToADF.add(urlPath);
                File f = new File(jprAbsPath);
                String canonicalPath = f.getCanonicalPath();

                // remove from has node
                fixer.removeNode(ref);


            }
        } catch (Exception ex) {
            System.out.println("Exception while trying to move reference");
            throw new Exception(ex);
        }


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

    /**
     * Invalid if
     * 1. In directory /fatools/talibs/
     * 2. In directory /fmwtools/BUILD_HOME/
     * 3. contains "/JTestCase/"
     * 4. starts with file:
     * 5. starts with /scratch/
     * @param jarPath
     * @return
     */
    public boolean isInvalidJarRef(String jprOrLibPath, String jarPath) {
        if (isEmpty(jarPath)) {
            return false;
        }
        // Temporary exemption added for padepu_attachments, as atg is adding invalid jars. 
        // Tracked by bug: 13927283
     
        if(isJarPathExempted(jarPath))
            return false;
        //  not invalid if it is a directory
        // (assumption - if it has .jar means it is a jar file. any other file will be a directory that should not be scanned for validity)
        if (jarPath.indexOf(".jar") < 0) {
            return false;
        }
        if (jarPath.indexOf("/fatools/talibs/") > 0) {
            return true;
        }
        String jarName = "";
        int indexSlash = jarPath.lastIndexOf("/");
        if (indexSlash > 0) {
            jarName = jarPath.substring(indexSlash + 1);
        }
        if (!isEmpty(jarName) && invalidJars.contains(jarName)) {
            return true;
        }
        if (jarPath.indexOf("/JTestCase/") > 0) {
            return true;
        }
        /*  if (jarPath.indexOf("fmwtools/BUILD_HOME/") > 0) {
            return true;
        }*/
        if (jarPath.trim().startsWith("file:")) {
            return true;
        }
        if (jarPath.trim().startsWith("/scratch/")) {
            return true;
        }
        // check for crm jars that are invalid.
        // As Per: Matt Kettle, these jars are invalid only for non crm applications.
        // e.g. Non CRM Projects should not reference these jars.
        // If it is not a CRM App and it contains one of the crm invalid jars - INVALID
        // Added on 1-Mar-11
        if (!isCrmApp(jprOrLibPath) && crmInvalidJars.contains(jarName)) {
            return true;
        }
        return false;
    }
    public boolean isJarPathExempted(String path)
    {
            if(path.contains("padepu_attachments") && path.contains("AuthorizationService-Client"))
                return true;
            return false;
        }
    public boolean isCrmApp(String jprOrLibraryPath) {
        if (isEmpty(jprOrLibraryPath))
            return false;
        if (jprOrLibraryPath.contains("/fusionapps/crm"))
            return true;
        return false;
    }

    public String getJarNameFromPath(String jarPath) {
        if (isEmpty(jarPath)) {
            return "";
        }
        int indexSlash = jarPath.lastIndexOf("/");
        if (indexSlash > 0) {
            return jarPath.substring(indexSlash + 1);
        }
        return "";
    }

    /**
     * Valid - if it is a jar and it starts with adf
     * @param jarPath
     * @return
     */
    public boolean isValidAdfJar(String jprOrLibPath, String jarPath) {

        if (isEmpty(jarPath)) {
            return true;
        }


        String jarName = "";
        int indexSlash = jarPath.lastIndexOf("/");
        if (indexSlash > 0) {
            jarName = jarPath.substring(indexSlash + 1);
        }


        if (!isEmpty(jarName) && invalidJars.contains(jarName)) {
            return false;
        }

        // Invalid if: non crm app contains one of the invalid crm jars
        if (!isCrmApp(jprOrLibPath) && crmInvalidJars.contains(jarName))
            return false;

        if (jarName.contains(".jar") && jarName.trim().toLowerCase().startsWith("adf")) {
            return true;
        }


        return false;
    }

    /**
     * Invalid if
     * 1. In directory /fatools/talibs/
     * @param jarPath
     * @return
     */
    public boolean isInvalidLibraryName(String libraryName) {
        if (isEmpty(libraryName)) {
            return false;
        }

        if (libraryName.endsWith(".jar")) {
            return true;
        }
        return false;

    }

    public boolean isValidDirectory(String absDirPath) {
        String absPathLowerCase = absDirPath.toLowerCase();

//        if (absPathLowerCase.contains("/qoc"))
//            return false;

        if (absPathLowerCase.contains("/dbschema/") || absPathLowerCase.contains("/.ade_path/") ||
            absPathLowerCase.contains("/test/") || absPathLowerCase.contains("/classes/") ||
            absPathLowerCase.contains("/noship/")) 
            return false;
        
        return true;
    }

    public boolean isValidJpr(String absDirPath) {
        String absPathLowerCase = absDirPath.toLowerCase();

        if (absPathLowerCase.contains("servicetest") || absPathLowerCase.contains("modeltest") ||
            absPathLowerCase.contains("structuretest") ||  absPathLowerCase.contains("securitypublictest")
            ||  absPathLowerCase.contains("datasecuritytest") || absPathLowerCase.contains("uitest")
            || absPathLowerCase.contains("analyticstest") || absPathLowerCase.contains("integrationtest")
            || absPathLowerCase.contains("ecsftest") || absPathLowerCase.contains("publiclinktest")
            || absPathLowerCase.contains("/setest") || absPathLowerCase.contains("fwktest"))
            return false;
        
        if( absDirPath.contains("/FinFunWfSetupProtectedUiModel.jpr"))
            return false;
        
        if (!absPathLowerCase.contains("/dbschema/") && !absPathLowerCase.contains("/.ade_path/") &&
            absPathLowerCase.endsWith(".jpr") && !absPathLowerCase.contains("test.") && !absPathLowerCase.contains("/test/") &&
            !absPathLowerCase.contains("/testui/") && !absPathLowerCase.contains("/noship/")) {

            return true;

        }
        return false;
    }

    public boolean isValidLibraryFile(String absDirPath) {
        String absPathLowerCase = absDirPath.toLowerCase();

        if (absPathLowerCase.contains("servicetest") || absPathLowerCase.contains("publicmodeltest") ||
            absPathLowerCase.contains("publicuitest") || absPathLowerCase.contains("modeltest") ||
            absPathLowerCase.contains("structuretest") ||  absPathLowerCase.contains("securitypublictest")
            ||  absPathLowerCase.contains("datasecuritytest") || absPathLowerCase.contains("uitest")
            || absPathLowerCase.contains("analyticstest") || absPathLowerCase.contains("integrationtest")
            || absPathLowerCase.contains("ecsftest") || absPathLowerCase.contains("publiclinktest")
            || absPathLowerCase.contains("/setest") || absPathLowerCase.contains("fwktest"))
            return false;
        
        if(absDirPath.contains("fusionapps/fin/components/financialCommon/fun/workflow/setup/protectedUiModel/ADF_Library_Dependencies.library"))
            return false;
        
        /*if(absDirPath.contains("/prj/components/projectsManagement") ||
            absDirPath.contains("/prj/components/projectsFinancialsSoa/projectManagement") ||
            absDirPath.contains("/prj/components/projectsFinancialsSoa/projectManagementControl") ||
            absDirPath.contains("/prj/components/projectsFinancialsSoa/resourceManagement") ||
            absDirPath.contains("/prj/components/projectsFinancialsEss/projectManagement") ||
            absDirPath.contains("/prj/components/projectsFinancialsEss/resourceManagement"))
            return false;*/

        if (!absPathLowerCase.contains("/dbschema/") && !absPathLowerCase.contains("/.ade_path/") &&
            absDirPath.endsWith("ADF_Library_Dependencies.library") && !absPathLowerCase.contains("test.") &&
            !absPathLowerCase.contains("/test/") && !absPathLowerCase.contains("/testui/") &&
            !absPathLowerCase.contains("/noship/")) {
            return true;
        }
        return false;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public static String printStatistics(Date startDate) {
        StringBuffer summary = new StringBuffer();

        // Print Statistics
        Date endDate = new Date();

        summary.append("=================================================");
        summary.append("Run Statistics");
        summary.append("Start Time: " + startDate.toString());
        summary.append("End Time: " + endDate.toString());
        long elapsedTime = endDate.getTime() - startDate.getTime();
        summary.append("Elapsed Time (sec): " + elapsedTime / 1000);
        summary.append("=================================================");

        summary.append("=================================================");
        summary.append("\nTotal Number of jprs reviewed: " + m_jprSeen);
        summary.append("\nTotal Number of jprs fixed : " + m_jprFixed);
        summary.append("\nTotal Number of ADF_Library_Dependencies.library files reviewed: " + m_librarySeen);
        summary.append("\nTotal Number of ADF_Library_Dependencies.library fixed : " + m_libraryFixed);
        summary.append("\nTotal Number of jprs fixed containing hanging library refs : " + m_hangingRefsJprFixed);
        summary.append("\nTotal Number of jprs fixed containing non existent jars refs : " + m_nonExistentRefsJprFixed);
        summary.append("=================================================");

        return summary.toString();

    }
}
