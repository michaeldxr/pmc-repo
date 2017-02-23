package oracle.apps.helpers;

import java.io.File;


import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import oracle.apps.di.MyNSResolver;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JprHelper {
    
    
    public static void main(String[] args) {
        HashSet<String> taglibs = getAllTagLibUrlsinJpr("/ade_autofs/ade_fusion_linux/FUSIONAPPS_PT.R8INT_LINUX.X64.rdd/" +
            "LATEST/fusionapps/fin/components/financialsEss/iby/fundsCapture/transactions/ess/FinPmtFCTransactionEss.jpr");
        System.out.println(taglibs);
    }

    /**
     * for the given jpr absolute path, get the base directory where the jpr exists
     * e.g. absPath: $ADE_VIEW_ROOT/fusionapps/fin/components/Ledger/LedgerSuperWeb/LedgerSuperWeb.jpr
     * base jpr location - $ADE_VIEW_ROOT/fusionapps/fin/components/Ledger/LedgerSuperWeb
     * @param absPath
     * @return
     */
    public static String getBaseJprLocation(String absPath) {
        if (absPath.endsWith(".jpr")) {
            int indexLastSlash = absPath.lastIndexOf("/");
            if (indexLastSlash != -1) {
                return absPath.substring(0, indexLastSlash);
            }

        }
        return null;
    }

    public static HashSet<String> getAllSecondaryDependencies(String jprPath) {
        HashSet<String> secondaryDependencies = new HashSet<String>();
        String basePath = getBaseJprLocation(jprPath);
        if (isEmpty(basePath))
            return null;
        String adfLibraryFileLocation = basePath + "/ADF_Library_Dependencies.library";
        File adfLibFile = new File(adfLibraryFileLocation);
        if (!adfLibFile.exists())
            return null;

        XMLDocument adfLibXml = XMLParserHelper.getXMLDocument(adfLibraryFileLocation);

        NodeList libraryRootNode = adfLibXml.getElementsByTagName("JLibraryNode");
        if (!XMLParserHelper.hasOneNOnlyOneNode(libraryRootNode)) {
            System.out.println("Library reference exepected one and only one JLibrary node");
            return null;
        }
        Node libraryNode = libraryRootNode.item(0);
        Node classPathNode = XMLParserHelper.getChildNodeWithName(libraryNode, "classPath");
        if (classPathNode == null) {
            return null;
        }
        Node entryNode = XMLParserHelper.getChildNodeWithName(classPathNode, "entries");
        if (entryNode == null) {
            return null;
        }

        NodeList itemNodes = entryNode.getChildNodes();
        if (itemNodes == null) {
            return null;
        }
        int len = itemNodes.getLength();
        for (int i = 0; i < len; i++) {
            Node itemNode = itemNodes.item(i);
            String nodeName = itemNode.getNodeName();
            if (!isEmpty(nodeName) && nodeName.trim().equals("Item")) {
                String jarPath = XMLParserHelper.getAttributeValue(itemNode, "path");
                if (!isEmpty(jarPath)) {


                    File projectfile = new File(basePath, jarPath);
                    if (projectfile.exists()) {
                        jarPath = projectfile.getAbsolutePath();
                        secondaryDependencies.add(jarPath);
                    } else {
                        secondaryDependencies.add(jarPath);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Gets the ADF Library Node
     * @param jprxml
     * @return
     * @throws Exception
     */
    public static Node getADFLibraryNode(XMLDocument jprxml) throws Exception {
        try {
            NodeList selectedHashNodes =
                jprxml.selectNodes("//hash[@n='internalDefinitions']/list[@n='libraryDefinitions']/hash/value[@v='ADF Library' and @n='id']/parent::");
            if (selectedHashNodes == null || selectedHashNodes.getLength() <= 0)
                return null;
            if (XMLParserHelper.hasOneNOnlyOneNode(selectedHashNodes)) {
                return selectedHashNodes.item(0);
            } else {
                System.out.println("Expecting only one adf library tag.. ");
                return null;
            }
        } catch (Exception ex) {
            System.out.println("Error while trying to get adf library node");
            throw new Exception(ex);
        }
    }

    /**
     * Gets the ADF BC Imports Library Node
     * @param jprxml
     * @return
     * @throws Exception
     */
    public static Node getADFBcImportsNode(XMLDocument jprxml) throws Exception {
        try {
            NodeList selectedHashNodes =
                jprxml.selectNodes("//hash[@n='internalDefinitions']/list[@n='libraryDefinitions']/hash/value[@v='ADFBC Imports' and @n='id']/parent::");
            if (selectedHashNodes == null || selectedHashNodes.getLength() <= 0)
                return null;
            if (XMLParserHelper.hasOneNOnlyOneNode(selectedHashNodes)) {
                return selectedHashNodes.item(0);
            } else {
                System.out.println("Expecting only one adf bc import library tag.. ");
                return null;
            }
        } catch (Exception ex) {
            System.out.println("Error while trying to get adf bc imports library node");
            throw new Exception(ex);
        }
    }

    public static HashSet<String> getAllBcImportsInJpr(String jprAbsPath, boolean convertToAbsolutePath) {

        String baseDir = getBaseJprLocation(jprAbsPath);
        HashSet<String> set_alladflibs = new HashSet<String>();
        if (isEmpty(jprAbsPath)) {
            System.out.println("JprHelper->getAllAdfLibrarysInJpr: Required Absolute jpr path (jprAbsPath) ");
            return null;
        }
        XMLDocument xmlJpr = XMLParserHelper.getXMLDocument(jprAbsPath);
        xmlJpr.setProperty("SelectionLanguage", "XPath");

        if (xmlJpr == null) {
            System.out.println("JprHelper->getAllAdfLibrarysInJpr: Could not parse jpr as xml " + jprAbsPath);
            return null;
        }

        try {

            Node bcImportHashNode = getADFBcImportsNode(xmlJpr);
            if (bcImportHashNode == null) {
                return set_alladflibs;
            }

            String nodeName = bcImportHashNode.getNodeName();
            if (!isEmpty(nodeName) && nodeName.trim().equals("hash")) {
                NodeList childrenOfHash = bcImportHashNode.getChildNodes();
                for (int j = 0; j < childrenOfHash.getLength(); j++) {
                    Node listNode = childrenOfHash.item(j);
                    String nValue = XMLParserHelper.getAttributeValue(listNode, "n");
                    if (!isEmpty(nValue) && nValue.trim().equals("classPath")) {
                        NodeList childrenOfList = listNode.getChildNodes();
                        int listLen = childrenOfList.getLength();
                        for (int k = 0; k < listLen; k++) {
                            Node possibleUrlNode = childrenOfList.item(k);
                            String urlTagName = possibleUrlNode.getNodeName();
                            if (!isEmpty(urlTagName) && urlTagName.trim().equals("url")) {
                                String jarValue = XMLParserHelper.getAttributeValue(possibleUrlNode, "path");
                                if (convertToAbsolutePath) {
                                    File f = new File(baseDir, jarValue);
                                    if (f.exists()) {
                                        jarValue = f.getAbsolutePath();
                                    }
                                }

                                set_alladflibs.add(jarValue);
                            }
                        }
                    }
                }
            }


        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }


        return set_alladflibs;
    }
    
    public static HashSet<String> getAllTagLibUrlsinJpr(String jprAbsPath) {
        
        HashSet<String> tagLibUrls = new HashSet<String>();
        XMLDocument xmlJpr = XMLParserHelper.getXMLDocument(jprAbsPath);
        if(xmlJpr == null)
            return tagLibUrls;
        xmlJpr.setProperty("SelectionLanguage", "XPath");
        try{
            NodeList selectedNodes =
                xmlJpr.selectNodes("//hash[@n='oracle.jdevimpl.webapp.jsp.libraries.model.ProjectTagLibraries']/list[@n='tag-libraries']/hash");
            if (selectedNodes != null) {
                int hashNodesLength = selectedNodes.getLength();
                for(int i=0;i < hashNodesLength; i++) {
                    
                    Node node = selectedNodes.item(i);
                    Node child = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(node, "value", "n", "tldURL");
                    if(child !=null) {
                        String taglib= XMLParserHelper.getAttributeValue(child, "v");
                        if(!FamilyModuleHelper.isEmpty(taglib))
                            tagLibUrls.add(taglib);
                    }
                }
                
            }
        }catch(Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
        
        return tagLibUrls;
    }

    public static HashSet<String> getAllAdfLibrarysInJpr(String jprAbsPath, boolean convertToAbsolutePath) {

        String baseDir = getBaseJprLocation(jprAbsPath);
        HashSet<String> set_alladflibs = new HashSet<String>();
        if (isEmpty(jprAbsPath)) {
            System.out.println("JprHelper->getAllAdfLibrarysInJpr: Required Absolute jpr path (jprAbsPath) ");
            return null;
        }
        XMLDocument xmlJpr = XMLParserHelper.getXMLDocument(jprAbsPath);
        xmlJpr.setProperty("SelectionLanguage", "XPath");

        if (xmlJpr == null) {
            System.out.println("JprHelper->getAllAdfLibrarysInJpr: Could not parse jpr as xml " + jprAbsPath);
            return null;
        }

        try {
            NodeList selectedNodes =
                xmlJpr.selectNodes("//hash[@n='internalDefinitions']/list[@n='libraryDefinitions']/hash/value[@v='ADF Library' and @n='id']/parent::");
            if (selectedNodes != null) {
                int selectedNodesCnt = selectedNodes.getLength();
                for (int i = 0; i < selectedNodesCnt; i++) {
                    Node node = selectedNodes.item(i);
                    String nodeName = node.getNodeName();
                    if (!isEmpty(nodeName) && nodeName.trim().equals("hash")) {
                        NodeList childrenOfHash = node.getChildNodes();
                        for (int j = 0; j < childrenOfHash.getLength(); j++) {
                            Node listNode = childrenOfHash.item(j);
                            String nValue = XMLParserHelper.getAttributeValue(listNode, "n");
                            if (!isEmpty(nValue) && nValue.trim().equals("classPath")) {
                                NodeList childrenOfList = listNode.getChildNodes();
                                int listLen = childrenOfList.getLength();
                                for (int k = 0; k < listLen; k++) {
                                    Node possibleUrlNode = childrenOfList.item(k);
                                    String urlTagName = possibleUrlNode.getNodeName();
                                    if (!isEmpty(urlTagName) && urlTagName.trim().equals("url")) {
                                        String jarValue = XMLParserHelper.getAttributeValue(possibleUrlNode, "path");
                                        if (convertToAbsolutePath) {
                                            File f = new File(baseDir, jarValue);
                                            if (f.exists()) {
                                                jarValue = f.getAbsolutePath();
                                            }
                                        }

                                        set_alladflibs.add(jarValue);
                                    }
                                }
                            }
                        }
                    }

                }
            } else {
                System.out.println("Could not obtain any library references in : " + jprAbsPath);
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }


        return set_alladflibs;
    }

    public static XMLDocument getResourceAsXMlFromJprLibraryRefs(String jprFullPath, String resourceToFind) {
        HashSet<String> adfLibrarys = getAllAdfLibrarysInJpr(jprFullPath, true);

        if (adfLibrarys != null && adfLibrarys.size() > 0) {
            Iterator<String> libraryIter = adfLibrarys.iterator();
            while (libraryIter.hasNext()) {
                String lib = libraryIter.next();
                JarFile jar = null;
                try {
                    jar = new JarFile(lib);
                    if (jar != null) {
                        JarEntry resourceEntry = jar.getJarEntry(resourceToFind);
                        if (resourceEntry != null) {
                            System.out.println("Found the resource in: " + jar.getName());
                            InputStream contents = jar.getInputStream(resourceEntry);
                            return XMLParserHelper.getXMLDocument(contents);
                            //break;
                        }
                    } else {
                        System.out.println("Jar file instance was null: " + lib);
                        return null;
                    }
                } catch (IOException ex) {
                    System.out.println("IOExcpetion: " + ex.getMessage());
                    continue;
                }
            }
        } else {
            System.out.println("Cannot find any referenced adf librarys in jpr: " + jprFullPath);
            return null;
        }
        return null;
    }

    public static NodeList getProjectDependencyHashNodeList(String jprFullPath) {
        if (isEmpty(jprFullPath)) {
            System.out.println("jprFullPath: Empty");
            return null;
        }
        File f = new File(jprFullPath);
        if (!f.exists()) {
            String msg = String.format("[ERROR] Jpr does not exist. Jpr location: %s", jprFullPath);
            System.out.println(msg);
            return null;
        }

        String baseDir = getBaseJprLocation(jprFullPath);


        XMLDocument xmlJpr = XMLParserHelper.getXMLDocument(jprFullPath);
        try {
            NodeList hashNodesList =
                xmlJpr.selectNodes("//hash[@n='oracle.ide.model.DependencyConfiguration']/list[@n='dependencyList']/hash/value[@v='oracle.jdeveloper.library.ProjectLibrary' and @n='class']/parent::");
            return hashNodesList;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;

    }
public static String  getDeploymentProfileUrlWithName(String dependentJpr, String profileName, boolean getAbsPath)
{
    String baseJpr = getBaseJprLocation(dependentJpr);
    String deploymentUrl = getDeploymentProfileUrlWithName(dependentJpr, profileName);
    if(isEmpty(deploymentUrl))
        return deploymentUrl;
    if(!getAbsPath)
        return deploymentUrl;
    try{
        File f = new File(baseJpr, deploymentUrl);
        if (f.exists()) {
            return f.getCanonicalPath();
        }
    }catch(Exception ex)
    {}
    return "";
    
    
}
    public static HashSet<String> getProjectDependencysInJar(String jprFullPath) {

        //        if (isEmpty(jprFullPath)) {
        //            System.out.println("jprFullPath: Empty");
        //            return null;
        //        }
        //        File f = new File(jprFullPath);
        //        if (!f.exists()) {
        //            String msg =
        //                String.format("[ERROR] Jpr does not exist. Jpr location: %s",
        //                              jprFullPath);
        //            System.out.println(msg);
        //            return null;
        //        }

        String baseDir = getBaseJprLocation(jprFullPath);

        HashSet<String> set_projDependencies = new HashSet<String>();
        XMLDocument xmlJpr = XMLParserHelper.getXMLDocument(jprFullPath);
        try {
            NodeList hashNodesList = getProjectDependencyHashNodeList(jprFullPath);
            //            NodeList hashNodesList =
            //                xmlJpr.selectNodes("//hash[@n='oracle.ide.model.DependencyConfiguration']/list[@n='dependencyList']/hash/value[@v='oracle.jdeveloper.library.ProjectLibrary' and @n='class']/parent::");
            if (hashNodesList != null) {
                int hashNodesCnt = hashNodesList.getLength();
                for (int i = 0; i < hashNodesCnt; i++) {
                    Node hasNode = hashNodesList.item(i);
                    String nodeName = hasNode.getNodeName();
                    if (!isEmpty(nodeName) && nodeName.trim().equals("hash")) {
                        NodeList childrenOfHash = hasNode.getChildNodes();
                        for (int j = 0; j < childrenOfHash.getLength(); j++) {
                            Node hashChild = childrenOfHash.item(j);
                            String hashChildNodeName = hashChild.getNodeName();
                            if (!isEmpty(hashChildNodeName) && hashChildNodeName.trim().equalsIgnoreCase("url")) {
                                String nValue = XMLParserHelper.getAttributeValue(hashChild, "n");
                                if (!isEmpty(nValue) && nValue.trim().equals("sourceURL")) {

                                    String dependentProjectPath = XMLParserHelper.getAttributeValue(hashChild, "path");
                                    File projectfile = new File(baseDir, dependentProjectPath);
                                    if (projectfile.exists()) {
                                        dependentProjectPath = projectfile.getAbsolutePath();
                                        set_projDependencies.add(dependentProjectPath);
                                    }
                                }
                            }

                        }
                    }

                }
            } else {
                System.out.println("Could not obtain any project dependencies in : " + jprFullPath);
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return set_projDependencies;
    }


    public static NodeList getDeploymentProfilesList(String jprPath) {
        if (isEmpty(jprPath)) {
            System.out.println("jprFullPath: Empty");
            return null;
        }
        File f = new File(jprPath);
        if (!f.exists()) {
            String msg = String.format("[ERROR] Jpr does not exist. Jpr location: %s", jprPath);
            System.out.println(msg);
            return null;
        }

        XMLDocument xmlJpr = XMLParserHelper.getXMLDocument(jprPath);
        try {

            return xmlJpr.selectNodes("//hash[@n='oracle.jdeveloper.deploy.dt.DeploymentProfiles']/list[@n='profileList']/string");


        } catch (Exception ex) {
            System.out.println("exception while trying to retrive deployment profiles for jpr: " + jprPath);
            ex.printStackTrace();
        }
        return null;
    }

    public static NodeList getDeploymentProfiles(String jprPath) {

        if (isEmpty(jprPath)) {
            System.out.println("jprFullPath: Empty");
            return null;
        }
        File f = new File(jprPath);
        if (!f.exists()) {
            String msg = String.format("[ERROR] Jpr does not exist. Jpr location: %s", jprPath);
            System.out.println(msg);
            return null;
        }


        XMLDocument xmlJpr = XMLParserHelper.getXMLDocument(jprPath);
        if(xmlJpr == null)
            return null;
        try {
            return xmlJpr.selectNodes("//hash[@n='oracle.jdeveloper.deploy.dt.DeploymentProfiles']/hash[@n='profileDefinitions']/hash");

        } catch (Exception ex) {
            System.out.println("exception while trying to retrive deployment profiles for jpr: " + jprPath);
            ex.printStackTrace();
        }
        return null;
    }

    public static String getDeploymentProfileUrlWithName(String jprPath, String profileName) {

        try {
            NodeList depProfiles = getDeploymentProfiles(jprPath);
            int depprofileLen = depProfiles.getLength();
            for (int i = 0; i < depprofileLen; i++) {
                Node profile = depProfiles.item(i);
                if (profile == null)
                    continue;
                Node profileNameNode =
                    XMLParserHelper.getChildNodeWithMatchingAttributeAndName(profile, "value", "n", "profileName");
                if (profileNameNode == null)
                    continue;
                String obtProfileName = XMLParserHelper.getAttributeValue(profileNameNode, "v");
                if (!isEmpty(obtProfileName) && obtProfileName.trim().equals(profileName)) {
                    Node urlNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(profile, "url", "n", "jarURL");
                    if (urlNode == null)
                        continue;
                    return XMLParserHelper.getAttributeValue(urlNode, "path");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    public static ArrayList<String> getAllDeploymentProfileUrls(String jprPath) {
        
        ArrayList<String> jarNames = new ArrayList<String>();
        NodeList depProfiles = getDeploymentProfiles(jprPath);
        if(depProfiles == null)
            return jarNames;
        
        int depprofileLen = depProfiles.getLength();
        
        for (int i = 0; i < depprofileLen; i++) {
            
            Node profile = depProfiles.item(i);
            if (profile == null)
                continue;
            Node profileNameNode =
                XMLParserHelper.getChildNodeWithMatchingAttributeAndName(profile, "value", "n", "profileName");
            if (profileNameNode == null)
                continue;
            String obtProfileName = XMLParserHelper.getAttributeValue(profileNameNode, "v");
            if (!isEmpty(obtProfileName)){
                Node urlNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(profile, "url", "n", "jarURL");
                if (urlNode == null)
                    continue;
                String jarUrl = XMLParserHelper.getAttributeValue(urlNode, "path");
                String[] parts = jarUrl.split("/");
                jarNames.add(parts[parts.length-1]);
            }
        }
        
        return jarNames;
    }
    

public static NodeList getAllProfileDependencyNodes(String jprAbsPath)
{
        String baseDir = getBaseJprLocation(jprAbsPath);
        
        if (isEmpty(jprAbsPath)) {
            System.out.println("JprHelper->getAllAdfLibrarysInJpr: Required Absolute jpr path (jprAbsPath) ");
            return null;
        }
        XMLDocument xmlJpr = XMLParserHelper.getXMLDocument(jprAbsPath);
        xmlJpr.setProperty("SelectionLanguage", "XPath");

        if (xmlJpr == null) {
            System.out.println("JprHelper->getAllProfileDependencyNodes: Could not parse jpr as xml " + jprAbsPath);
            return null;
        }
        try{
        NodeList hashNodesList =xmlJpr.selectNodes("//hash[@n='oracle.ide.model.DependencyConfiguration']/list[@n='dependencyList']/hash/value[@v='oracle.jdevimpl.deploy.common.DefaultProfileDependable' and @n='adapterClass']/parent::");
            return hashNodesList;
        }catch(Exception ex)
        {
            
            }
        return null;
    }
    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public static boolean isModelProject(String jprPath) {
        if (isEmpty(jprPath))
            return false;
        if (jprPath.endsWith("Model.jpr"))
            return true;
        return false;
    }

    public HashMap<String, String> getListOfAllPropertySetsInJpr(String jprPath) {
        String baseJpr = getBaseJprLocation(jprPath);
        if (isEmpty(baseJpr))
            return null;
        HashMap<String, String> voFilePaths = new HashMap<String, String>();
        buildPSsInPath(baseJpr, voFilePaths);
        return voFilePaths;

    }

    public HashMap<String, String> getListOfAllViewObjectsInJpr(String jprPath) {
        String baseJpr = getBaseJprLocation(jprPath);
        if (isEmpty(baseJpr))
            return null;
        HashMap<String, String> voFilePaths = new HashMap<String, String>();
        buildVOsInPath(baseJpr, voFilePaths);
        return voFilePaths;

    }

    public HashMap<String, String> getListOfAllAMsInJpr(String jprPath) {
        String baseJpr = getBaseJprLocation(jprPath);
        if (isEmpty(baseJpr))
            return null;
        HashMap<String, String> amfilePaths = new HashMap<String, String>();
        buildAMsInPath(baseJpr, amfilePaths);
        return amfilePaths;

    }

    public static HashSet<String> getListOfAllUIFilesInJpr(String jprPath) {
        String baseJpr = getBaseJprLocation(jprPath);
        if (isEmpty(baseJpr))
            return null;
        HashSet<String> uiFilePaths = new HashSet<String>();
        buildUIFilesInPath(baseJpr, uiFilePaths);
        return uiFilePaths;

    }


    public HashMap<String, String> getListOfAllEntityObjectsInJpr(String jprPath) {
        String baseJpr = getBaseJprLocation(jprPath);
        if (isEmpty(baseJpr))
            return null;
        HashMap<String, String> eoFilePaths = new HashMap<String, String>();
        buildEOsInPath(baseJpr, eoFilePaths);
        return eoFilePaths;

    }

    private static void buildUIFilesInPath(String baseLocation, HashSet<String> uiFilePaths) {
        if (baseLocation.contains(".ade_path"))
            return;

        File folder = new File(baseLocation);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (absFilePath.trim().endsWith(".jsff") || absFilePath.endsWith(".jspx")) {
                    uiFilePaths.add(absFilePath);
                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (!dir.contains("/classes/")) {
                    buildUIFilesInPath(dir, uiFilePaths);
                }
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    private void buildAMsInPath(String baseLocation, HashMap<String, String> amFiles) {
        if (baseLocation.contains(".ade_path"))
            return;

        File folder = new File(baseLocation);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();

                if (absFilePath.trim().endsWith(".xml")) {
                    if (JprHelper.isAMXml(absFilePath)) {
                        String packagePath = getPackage(listOfFiles[i]);
                        amFiles.put(packagePath, absFilePath);
                    }
                }


            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (!dir.contains("/classes/")) {
                    buildAMsInPath(dir, amFiles);
                }

            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    public static boolean isViewObjectXml(String absFilePath) {
        if (isEmpty(absFilePath))
            return false;
        if (!absFilePath.endsWith(".xml"))
            return false;
        XMLDocument fileXml = XMLParserHelper.getXMLDocument(absFilePath);
        if (fileXml == null)
            return false;
        NodeList listViewObj = fileXml.getElementsByTagName("ViewObject");
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
            return false;
        } else
            return true;
    }

    public static boolean isPropertySetXml(String absFilePath) {
        if (isEmpty(absFilePath))
            return false;
        if (!absFilePath.endsWith(".xml"))
            return false;
        XMLDocument fileXml = XMLParserHelper.getXMLDocument(absFilePath);
        if (fileXml == null)
            return false;
        NodeList listViewObj = fileXml.getElementsByTagName("Domain");
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
            return false;
        } else
            return true;
    }

    public static boolean isAMXml(String absFilePath) {
        if (isEmpty(absFilePath))
            return false;
        if (!absFilePath.endsWith(".xml"))
            return false;
        XMLDocument fileXml = XMLParserHelper.getXMLDocument(absFilePath);
        if (fileXml == null)
            return false;
        NodeList amObjs = fileXml.getElementsByTagName("AppModule");
        if (amObjs != null && amObjs.getLength() > 0)
            return true;
        return false;
    }

    private void buildVOsInPath(String baseLocation, HashMap<String, String> voFiles) {
        if (baseLocation.contains(".ade_path"))
            return;

        File folder = new File(baseLocation);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();

                if (absFilePath.trim().endsWith(".xml")) {
                    if (JprHelper.isViewObjectXml(absFilePath)) {
                        String packagePath = getPackage(listOfFiles[i]);
                        voFiles.put(packagePath, absFilePath);
                    }
                }


            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (!dir.contains("/classes/")) {
                    buildVOsInPath(dir, voFiles);
                }

            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    private void buildPSsInPath(String baseLocation, HashMap<String, String> voFiles) {
        if (baseLocation.contains(".ade_path"))
            return;

        File folder = new File(baseLocation);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();

                if (absFilePath.trim().endsWith(".xml")) {
                    if (JprHelper.isPropertySetXml(absFilePath)) {
                        String packagePath = getPackage(listOfFiles[i]);
                        voFiles.put(packagePath, absFilePath);
                    }
                }


            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (!dir.contains("/classes/")) {
                    buildPSsInPath(dir, voFiles);
                }

            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    private void buildEOsInPath(String baseLocation, HashMap<String, String> eoFiles) {
        if (baseLocation.contains(".ade_path"))
            return;

        File folder = new File(baseLocation);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();

                if (absFilePath.trim().endsWith(".xml")) {
                    if (JprHelper.isEntityObjectXml(absFilePath)) {
                        String packagePath = getPackage(listOfFiles[i]);
                        eoFiles.put(packagePath, absFilePath);
                    }
                }


            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (!dir.contains("/classes/")) {
                    buildEOsInPath(dir, eoFiles);
                }

            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    public static boolean isEntityObjectXml(String absFilePath) {
        if (isEmpty(absFilePath))
            return false;
        if (!absFilePath.endsWith(".xml"))
            return false;
        XMLDocument fileXml = XMLParserHelper.getXMLDocument(absFilePath);
        if (fileXml == null)
            return false;
        NodeList listViewObj = fileXml.getElementsByTagName("Entity");
        if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
            return false;
        } else
            return true;
    }

    public String getPackage(File f) {
        String path = f.getAbsolutePath();
        int extIndex = path.lastIndexOf(".");
        if (extIndex != -1) {
            path = path.substring(0, extIndex);
        }
        int indexOfSrc = path.indexOf("/src/");
        if (indexOfSrc == -1)
            return path;
        String noSrcPath = path.substring(indexOfSrc + "/src/".length());
        String packagePath = noSrcPath.replaceAll("/", ".");


        return packagePath;


    }

}
