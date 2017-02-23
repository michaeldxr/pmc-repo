package oracle.apps.psr;


import com.sun.org.apache.xerces.internal.util.URI;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.Mail;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ELHelpProviderScanner {
    private static HashMap<String, HashSet<String>> _jarMap = new HashMap<String, HashSet<String>>();
    public static int m_numberJarFiles = 0;
    public static int m_numberJprFiles = 0;
    static boolean bDebug = true;

    private static Workbook m_myBook;
    static Worksheet m_elPrefixesSheet;
    static BufferedWriter outputFileWriter;
    int numberOfExceptions = 0;
    static String label;
    
    StringBuffer exceptions = new StringBuffer();
    
    public ELHelpProviderScanner() {
        super();
    }

    private static void initializePrefixesSheet(String worksheetName) {
        if (m_myBook == null) {
            m_myBook = new Workbook("output");
        }
        // Create a worksheet
        m_elPrefixesSheet = new Worksheet(worksheetName);
        // column 1 - Product
        m_elPrefixesSheet.setColumnWidth(1, 10);
        // column 2 - Prefix Name
        m_elPrefixesSheet.setColumnWidth(2, 30);
        // Column 3 - jars
        m_elPrefixesSheet.setColumnWidth(3, 40);


        // column 1 - product
        m_elPrefixesSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Product")));
        // column 2 - filename
        m_elPrefixesSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Prefix")));
        // Column 3 - file path
        m_elPrefixesSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Jars Present In")));


        m_myBook.addWorksheet(m_elPrefixesSheet);
    }

    public static void main(String[] args) throws Exception{
       ELHelpProviderScanner eLHelpProviderFixer = new ELHelpProviderScanner();
       
       String usage = "findDuplicateHelpProviders.sh <scanPath> (fusionapps/jlib and fusionapps/fin/jlib already included) ";
        if (args == null || args.length < 1 || isEmpty(args[0])) {
            System.out.println("Usage: " + usage);
            return;
        }

        String crawlDir = args[0];
        boolean append = false;
        if(args.length > 1)
            append = new Boolean(args[1].trim());
        
      /*  if(crawlDir.contains("/LATEST")) {
            int ind = crawlDir.indexOf("/LATEST");
            String pathBeforeLatest = crawlDir.substring(0,ind);
            File series = new File(pathBeforeLatest);
            File[] labels = series.listFiles();
            
            long lastModified = 0;
            String latestLabel = "";
            for(int i = 0; i < labels.length; i++) {
                if(labels[i].lastModified() > lastModified) {
                    lastModified = labels[i].lastModified();
                    latestLabel = labels[i].getName();
                }
            }
            if(!latestLabel.equals(""))
                crawlDir = crawlDir.replace("LATEST", latestLabel);
        }*/

        System.out.println("Crawling Directory: " + crawlDir);
        File folder = new File(crawlDir);
        label = getLabel(folder.getCanonicalPath());
        
        outputFileWriter = new BufferedWriter(new FileWriter("ELHelpProviderScan.csv", append));
       // outputFileWriter.write(label + "\n");
        if(!append)
          outputFileWriter.write("Label,Prefix,Jar1,Jar2,\n");
        
        eLHelpProviderFixer.findPrefixInMultipleJars(crawlDir);
        String results = eLHelpProviderFixer.getResults();
        System.out.println("Results ==> ");
        System.out.println(results);
        StringBuffer mailSummary = new StringBuffer();
        mailSummary.append(String.format("Script was run on: %s \n", crawlDir));
        mailSummary.append(results);
        eLHelpProviderFixer.writeResultsToSheet();
        outputFileWriter.close();
        
      //String afterExSummary = eLHelpProviderFixer.getmailSummaryAfterExceptions(mailSummary,eLHelpProviderFixer.exceptions.toString());
      
//        Mail.sendMail("zeesha.currimbhoy@oracle.com", afterExSummary,
//                      "SCRIPT: ELHelpProviderPrefix: Prefix detected in multiple Jars run at: " + (new Date()).toString());
    }

public String getmailSummaryAfterExceptions(StringBuffer mailSummary, String exStr)
{
    if(mailSummary==null || isEmpty(mailSummary.toString()))
        return "";
    if(numberOfExceptions==0)
        mailSummary.append(" \n No exceptions to report..\n");
    else
        mailSummary.append("\n Total  number of exceptions = " + numberOfExceptions);
    if(isEmpty(exStr))
        return mailSummary.toString();
    else
        return mailSummary.toString()+" \n " +exStr;
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

    public void writeResultsToSheet() throws Exception{
        if (_jarMap == null || _jarMap.size() <= 0)
            return;

        initializePrefixesSheet("PrefixInMultipleJars");
        Set<String> keySet = _jarMap.keySet();
        Iterator<String> keyIter = keySet.iterator();
        int sheetRow = 2;
        while (keyIter.hasNext()) {
            String prefix = keyIter.next();
            HashSet<String> jars = _jarMap.get(prefix);
            String product = "";
            String formattedJars = "";
            String jarsForcsv = "";
            if (jars != null && jars.size() > 1) {

                Iterator<String> jarsIter = jars.iterator();
                while (jarsIter.hasNext()) {
                    String nextjar = jarsIter.next();
                    formattedJars = formattedJars + " \n " + nextjar;
                    jarsForcsv =  getFileName(nextjar) + "," + jarsForcsv;
                }

                m_elPrefixesSheet.addCell(new Cell(new CellLocation("A" + sheetRow), new CellValue(product)));
                m_elPrefixesSheet.addCell(new Cell(new CellLocation("B" + sheetRow), new CellValue(prefix)));
                m_elPrefixesSheet.addCell(new Cell(new CellLocation("C" + sheetRow), new CellValue(formattedJars)));
                sheetRow++;
                
                outputFileWriter.write(label + "," + prefix + "," + jarsForcsv + "\n");

                //sbResults.append(String.format("\n File: %s present in following jars %s ", entryName, jars.toString()));
            }
        }
        try {
            writeSpreadsheet("output.xlsx");
        } catch (Exception ex) {
            System.out.println("Exception in writing excel file");
            ex.printStackTrace();

        }

    }

    public void findPrefixInMultipleJars(String crawlDir) {
        String viewRoot = getADERootDirectory(crawlDir);
      
        if(crawlDir.contains("/fusionapps")) {
          String fusionappsLibs = viewRoot + "/fusionapps/jlib";
          System.out.println("Crawling dir: " + fusionappsLibs);
          crawlDirectory(fusionappsLibs);
        }        
        
        if(crawlDir.contains("/fusionapps")) {
          String fusionappsFinLibs = viewRoot + "/fusionapps/fin/jlib";      
          System.out.println("Crawling dir: " + fusionappsFinLibs);
          crawlDirectory(fusionappsFinLibs);
        }
        
        System.out.println("Crawling dir: " + crawlDir);
        crawlDirectory(crawlDir);
    }

    /**
     *Get the ADE_VIEW_ROOT directory.
     * This utility has to be run from within an ADE_VIEW_ROOT. If the environment variable is not set or the user
     * is not within the ade view root, the utility terminates
     * @return
     * Returns the ade view root directory
     */
    public String getADERootDirectory(String filePath) {

        String adeRootStr = "";
        // Check 2: ade_view_root is always the path before fusionapps
        if (filePath.contains("/fusionapps")) {
            int fappsIndex = filePath.indexOf("/fusionapps");
            adeRootStr = filePath.substring(0, fappsIndex);
        }
        if (filePath.contains("/fsm")) {
            int fappsIndex = filePath.indexOf("/fsm");
            adeRootStr = filePath.substring(0, fappsIndex);
        }
        if (filePath.contains("/atgpf")) {
            int fappsIndex = filePath.indexOf("/atgpf");
            adeRootStr = filePath.substring(0, fappsIndex);
        }

        if (isEmpty(adeRootStr)) {
            System.out.println("Cannot find ade_view_root: " + filePath);
        }

        return adeRootStr;
    }

    public String getResults() {
        StringBuffer sbResults = new StringBuffer();
        sbResults.append(String.format("1. Number of Jar Files scanned: %d \n \n", m_numberJarFiles));
        if (_jarMap != null && _jarMap.size() > 0) {

            Set<String> keySet = _jarMap.keySet();
            Iterator<String> keyIter = keySet.iterator();
            while (keyIter.hasNext()) {
                String entryName = keyIter.next();
                HashSet<String> jars = _jarMap.get(entryName);

                if (jars != null && jars.size() > 1) {

                    sbResults.append(String.format("\n File: %s present in following jars %s ", entryName, jars.toString()));
                }
            }
        }
        return sbResults.toString();
    }


    private void crawlDirectory(String crawlDir) {
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
                    processJarForClassInMultipleJars(listOfFiles[i]);
                }

            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                crawlDirectory(dir);
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }


    /**
     * For jar file -> look for adf-settings.xml
     * find prefix tag in adf-settings.xml and log it in the jar map
     * jarMap: Key-Prefix, Value -> HashSet<Jar path>
     * @param f Jar file that is to be processed
     */
    private void processJarForClassInMultipleJars(File f) {
        String jarPath = f.getAbsolutePath();

        try {
            JarInputStream jarStream = new JarInputStream(new FileInputStream(f));
            JarFile jar = new JarFile(f);
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

                if (!jarEntry.isDirectory()) {
                    if (strEntryName.contains("adf-settings.xml")) {


                        // check if adfsettings has a prefix
                        InputStream contents = jar.getInputStream(jarEntry);
                        XMLDocument adfsettingsXml = XMLParserHelper.getXMLDocument(contents);
                        if (adfsettingsXml == null)
                            continue;
                        NodeList helpProviderNodes = adfsettingsXml.getElementsByTagName("help-provider");
                        if (helpProviderNodes == null)
                            continue;
                        int helpProvLen = helpProviderNodes.getLength();
                        for (int i = 0; i < helpProvLen; i++) {
                            Node helpProvNode = helpProviderNodes.item(i);
                            if (helpProvNode == null)
                                continue;
                            if (XMLParserHelper.isNodeName(helpProvNode, "help-provider")) {
                                String prefix = XMLParserHelper.getAttributeValue(helpProvNode, "prefix");
                                if (isEmpty(prefix))
                                    continue;
                                System.out.println("Found adf-settings with prefix " + prefix + " in file:" + f.getName());
                                addToJarMap(prefix, jarPath);
                            }
                        }
                        if(contents!=null)
                          contents.close();
                    }
                }
                
                
            }
            if (jar != null)
                jar.close();
            if (jarStream != null)
                jarStream.close();
        } catch (IOException ex) {
            System.err.println("Error occurred while opening jar file: " + f.getName());
          logException(jarPath,ex);
            return;
        }
    }

    public boolean isValidJarFile(String path, String fileName) {
        if (isEmpty(path))
            return false;

        if (path.contains("/classes/") || path.contains("/.ade_path/") || path.contains(".ear/"))
            return false;
        if (path.endsWith("Test.jar"))
            return false;
        if (path.endsWith(".jar") && path.contains("/jlib/"))
            return true;

        return false;
    }

    public void crawlDirForFix(String crawlDir, boolean bDoADE) {
        if (crawlDir.contains(".ade_path") || crawlDir.contains("/classes/"))
            return;

        File folder = new File(crawlDir);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();

                if (isValidJpr(absFilePath)) {
                    m_numberJprFiles++;
                    fixJpr(listOfFiles[i], bDoADE);
                }

            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                crawlDirForFix(dir, bDoADE);
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    public void fixJpr(File jprFile, boolean boADE) {
        String jprPath = jprFile.getAbsolutePath();

        if (bDebug) {
            System.out.println("Processing jpr: " + jprPath);
        }

        // get resource project bo dependencies
        HashSet<Node> resourceBONode = getResourceProjectBODependencies(jprPath);
        if (resourceBONode != null && resourceBONode.size() > 0) {
            System.out.println(String.format("Jpr: %s has %d number of Resource Prj BO Dep", jprPath, resourceBONode.size()));
            try {
                XmlFixer fixer = new XmlFixer(jprPath);
                fixDependencyNodes(jprPath, fixer, resourceBONode);
            } catch (Exception ex) {
                ex.printStackTrace();
                logException(jprPath,ex);
            }


        }

    }

    public HashSet<String> fixDependencyNodes(String jprPath, XmlFixer jprFixer,
                                              HashSet<Node> buildOutputHashNodes) throws Exception {
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
            String classVal = XMLParserHelper.getAttributeValue(classNode, "v");
            if (isEmpty(classVal) || !classVal.trim().equals("oracle.jdeveloper.library.ProjectLibrary")) {
                System.out.println("Invalid value for class node: " + classVal);
                continue;
            }
            Node ownerUrlNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(hashNode, "url", "n", "sourceOwnerURL");
            if (ownerUrlNode == null) {
                ownerUrlNode =
                        XMLParserHelper.getChildNodeWithMatchingAttributeAndName(hashNode, "value", "n", "sourceOwnerURL");
            }
            Node sourceUrlNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(hashNode, "url", "n", "sourceURL");
            if (sourceUrlNode == null) {
                sourceUrlNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(hashNode, "value", "n", "sourceURL");
            }
            if (classNode == null || ownerUrlNode == null || sourceUrlNode == null) {
                System.out.println("[ERROR] [MakeFix] Could not obtain class or sourceOwnerURL or sourceURL Nodes");
                continue;
            }
            String currentNode = "";
            String oldBOPath = "";
            String newDepProfile = "";
            try {


                String sourceUrlVal = XMLParserHelper.getAttributeValue(sourceUrlNode, "path");


                File jprFile = new File(jprPath);
                File dependentJpr = new File(jprFile.getParent(), sourceUrlVal);
                if (!dependentJpr.exists())
                    throw new Exception("Dependent jpr file does not exist: " + dependentJpr.getAbsolutePath());

                if (dependentJpr.getName().equals(jprName)) {
                    throw new Exception("[ERROR] [MakeFix] Will not modify any self dependent dependencies");
                }
                File deploymentProfilePath = getDependentDeploymentProfileFile(jprPath, dependentJpr);

                if (deploymentProfilePath == null) {
                    throw new Exception("Will not fix could not obtain deployment profile name..");
                }

                File relativeFile = new File(deploymentProfilePath.toURI().relativize(jprFile.getParentFile().toURI()));
                if (relativeFile != null && relativeFile.exists()) {
                    // here is the todo
                    System.out.println("YAYAYAYAYA!!! ");
                }
                System.out.println("base Jpr: " + jprPath);
                System.out.println("Relative Url: " + relativeFile.getPath());

                System.out.println("\n\n\n ");


            } catch (Exception ex) {

                System.out.println(String.format("[ERROR] [MakeFix] Exception while trying to make fix for jpr: %s for %s",
                                                 jprPath, classNode));
              logException(jprPath,ex);
                throw new Exception(ex);
            }
            buildOutputUrls.add(String.format("OldValue: %s , NewValue %s", oldBOPath, newDepProfile));
        }

        return buildOutputUrls;
    }

    public File getDependentDeploymentProfileFile(String baseJpr, File dependentJpr) throws Exception {
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
                //addToExceptionsDueToMultiple(profilesNotFixedDueToExceptions, String.format("%s # %s", baseJpr, baseProfileName), msg);
                throw new Exception(msg);
            } else {
                if (!isEmpty(foundProfileName))
                    return null;
            }
        }
        Node profileNode = profileList.item(0);
        if (!XMLParserHelper.isNodeName(profileNode, "string"))
            throw new Exception("Could not obtain valid profile for dependen jpr: " + dependentJprPath);
        String profileName = XMLParserHelper.getAttributeValue(profileNode, "v");
        if (isEmpty(profileName)) {
            throw new Exception("Could not obtain valid profile for dependent jpr: " + dependentJprPath);
        }
        String dependentJarLoc = JprHelper.getDeploymentProfileUrlWithName(dependentJprPath, profileName);
        if (dependentJarLoc == null) {
            System.out.println("could not get profile name: " + profileName + " in dependent jar: " + dependentJprPath);
            return null;
        }
        File dependentJarFile = new File(new File(dependentJprPath).getParent(), dependentJarLoc);
        if (dependentJarFile != null && dependentJarFile.exists())
            return dependentJarFile;
        return null;
    }

    public String getJprNameFromPath(String path) {
        if (isEmpty(path))
            return null;
        if (path.endsWith(".jpr")) {
            int lastSlash = path.lastIndexOf("/");
            int lastDot = path.lastIndexOf(".");
            if (lastSlash != -1 && lastDot != -1) {
                return path.substring(lastSlash + 1, lastDot);
            }

        }
        return null;
    }

    public HashSet<Node> getResourceProjectBODependencies(String jprFilePath) {
        HashSet<Node> resourceBODep = new HashSet<Node>();
        NodeList prjDependencyList = JprHelper.getProjectDependencyHashNodeList(jprFilePath);
        if (prjDependencyList == null || prjDependencyList.getLength() < 1)
            return null;
        int depLen = prjDependencyList.getLength();
        for (int i = 0; i < depLen; i++) {
            Node prjDep = prjDependencyList.item(i);
            Node urlNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(prjDep, "url", "n", "sourceURL");
            if (urlNode == null)
                continue;
            String pathVal = XMLParserHelper.getAttributeValue(urlNode, "path");
            if (isEmpty(pathVal))
                continue;

            if (pathVal.endsWith("Resource.jpr")) {
                resourceBODep.add(prjDep);
            }
        }
        return resourceBODep;


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

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }
    
  private String getFileName(String filePath) {
      
    int index = 0;
    if (filePath.contains("fusionapps/")) {
            index = filePath.indexOf("fusionapps/");            
    }
    if (filePath.contains("fsm/")) {
            index = filePath.indexOf("fsm/");
    }
      if (filePath.contains("atgpf/")) {
              index = filePath.indexOf("atgpf/");
      }
      if(index > 0) {
        return filePath.substring(index);
      }
      return filePath;
  }

    public void addToJarMap(String prefix, String jarPath) {
        if (_jarMap.containsKey(prefix)) {
            HashSet<String> setJars = _jarMap.get(prefix);
            if (setJars.contains(jarPath))
                return;
            setJars.add(jarPath);
            _jarMap.put(prefix, setJars);
        } else {
            HashSet<String> setJars = new HashSet<String>();
            setJars.add(jarPath);
            _jarMap.put(prefix, setJars);
        }
    }
    
    /**
     *This function was introduced by sudipti.gupta@oracle.com
     * 
     * Zeesha - am modifying it because there are no index checks. This method throws
     * array index out of bounds exceptions.
     * @param filePath
     * @return
     */
  private static String getLabel(String filePath) {
      if(filePath==null || filePath.trim().equals(""))
          return "";
      if (filePath.startsWith("/ade/"))
          return "";
      
      String label="";
      int index = 0;
      
    if (filePath.contains("/fusionapps")) {
            index = filePath.indexOf("/fusionapps");            
    }
    if (filePath.contains("/fsm")) {
            index = filePath.indexOf("/fsm");
    }
    if (filePath.contains("/atgpf")) {
            index = filePath.indexOf("/atgpf");
    }
      
      String path_before_fusionapps =
          filePath.substring(0, index);
      String[] parts = path_before_fusionapps.split("/");
      if(parts==null || parts.length<2)
          return "";
      
      label = parts[parts.length -2] + "_" + parts[parts.length -1];            
      label = label.replace(".rdd","");            
      return label;
  }
    
  public void logException(String info, Exception e) 
  {
      numberOfExceptions++;
      
      exceptions.append("\n@@ Exception # " +numberOfExceptions +"@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n");
      exceptions.append(info.toString() +"\n");
      exceptions.append(e.toString() +"\n");
      StackTraceElement[] element = e.getStackTrace();
      for(int i = 0; i < element.length; i++) {
          exceptions.append(element[i].toString() +"\n");
      }
  }
}
