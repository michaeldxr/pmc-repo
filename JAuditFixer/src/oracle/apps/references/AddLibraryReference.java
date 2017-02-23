package oracle.apps.references;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;

import java.io.Writer;

import java.util.ArrayList;
import java.util.Date;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AddLibraryReference {

    /**
     * Number of jprs seen
     */
    static int m_jprSeen = 0;

    /**
     * Number of jprs fixed
     */
    static int m_jprFixed = 0;

     /**
     * Name of the diff file
     */
    static String m_diffFileName = "diff.txt";
     
    static Writer outputFileWriter; 
    
    static ArrayList<String> jarsToAdd = new ArrayList<String>();

    static String library = null; //tag/adf
    static String projects = null; //all/ui/model
    
    //Needed only for tag lib
    private ArrayList<JarEntry> jes = null;
    private JarFile jarFile = null;
    private static boolean addTagLibs = false;
    private ArrayList<HashMap> tlds = new ArrayList<HashMap>();
    
    /**
     * Diff helper to write diff output to the file
     */
    AdeDiffHelper diffHelper = new AdeDiffHelper(m_diffFileName);

    /**
     * debug flag - used to print sop statements when in debug mode
     */
    boolean bDebug = false;

    public AddLibraryReference() {
        super();
    }
 

    public static void main(String[] args) {

        Date startDate = new Date();
        
        if (args.length < 5) {
            System.out.println("Usage:  AddAdfLibraryReference.sh <tag|adf> <all|ui|model> <name of jar to be added>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        Boolean bDoADE = new Boolean(args[1]);   
        
        // Validate input arguments ...
        library = args[2];
        if(!library.equals("tag") && !library.equals("adf")) {
          System.out.println("Library option can only be tag or adf");
          System.exit(1);
        }
        projects = args[3];        
        if(!projects.equals("ui") && !projects.equals("all") && !projects.equals("model")) {
          System.out.println("You can only give all | ui | model as the projects option");
          System.exit(1);
        }             

        for(int i=4; i < args.length; i++){
          if(!args[i].startsWith("fusionapps/")) {
            System.out.println(args[i] + " - Please specify the name of jar starting with fusionapps/ . e.g. fusionapps/jlib/AdfAtkHelpPortalUAPPublicUi.jar");
            System.exit(1);
          }
          jarsToAdd.add(args[i]);
        }
        
        if(library.equals("tag") && jarsToAdd.size() > 1) {
          System.out.println("You can only specify one jar file with the tag option");
          System.exit(1);
        }
        
      AddLibraryReference addLibraryRef =
          new AddLibraryReference();
      
      for(int i = 0; i < jarsToAdd.size(); i++) {
        String jarName = addLibraryRef.getJarName(jarsToAdd.get(i));
        if(!addLibraryRef.isValidAdfJar(jarName)) {
          System.out.println("Invalid Adf jar name - " + jarsToAdd.get(i));
          System.exit(1);
        }
      }
      
      for(int i=0; i< jarsToAdd.size(); i++) {
        String jarPath = sCrawlDir.substring(0, sCrawlDir.indexOf("fusionapps/")) + jarsToAdd.get(i);
        if(!(new File(jarPath)).exists()) {
          System.out.println(jarsToAdd.get(i) + " - Specified jar file does not exist. Please ensure that the jar name is correct");
          System.exit(1);
        }
      }
        
      System.out.println("============= Analyzing My Directory " + sCrawlDir);
      
        try {
            outputFileWriter = new BufferedWriter(new FileWriter("edit_manually.txt") );
            if (bDoADE) {
                // Remove any existing diff.txt. Create a new bufferred writer for diff.txt
                addLibraryRef.diffHelper.startDiffLog();
            }
            if(library.equals("tag")) {
              addTagLibs = addLibraryRef.jarFileContainsTagLib(sCrawlDir, jarsToAdd.get(0));
            }
            addLibraryRef.crawlDirectory(sCrawlDir, bDoADE);
            
            outputFileWriter.write("\n\n\nPlease edit the above files (if any) manually.");
            outputFileWriter.close();

            if (bDoADE.booleanValue()) {
                addLibraryRef.diffHelper.closeDiffLog();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        printStatistics(startDate);
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
    private void crawlDirectory(String path, Boolean bDoADE) {
        if (path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (isValidJpr(absFilePath)) {
                    System.out.println("Analyzing jpr: " + absFilePath);
                    processJpr(absFilePath, bDoADE);                    
                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidDirectory(dir)) {
                    crawlDirectory(dir, bDoADE);
                }
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    public void processJpr(String absPath, boolean bDoADE) {

        m_jprSeen++;

        XMLDocument jprXml = XMLParserHelper.getXMLDocument(absPath);
        XmlFixer fixer = null;
        boolean bFixMade1 = false;
        boolean bFixMade2 = false;

        try {
            fixer = new XmlFixer(absPath);
        } catch (Exception ex) {
            System.out.println("Skipping file.. could not initialze fixer: " +
                               absPath);
            return;
        }

        if (jprXml == null) {
            System.out.println("Skipping file .. Could not parse jpr as xml " +
                               absPath);
            return;
        }
        if(addTagLibs)
          bFixMade1 = addTagLibrary(jprXml,fixer,absPath); //This will also add the jar in Adf lib references if needed
        else 
          bFixMade2 = addAdfLibrary(jprXml,fixer,absPath);
        
        if (bFixMade1 || bFixMade2) {           
            try {
                if (bDoADE) {
                    String newFileContents = fixer.getFileContentsAsString(false);
                    applyFix(absPath, newFileContents);
                }
            } catch (Exception ex) {
                System.out.println("Exception occurred while apply fix for file: " +
                                   absPath);
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }

            m_jprFixed++;
        }
    }
    
    private boolean addTagLibrary(XMLDocument xmlJpr,XmlFixer fixer,String jprAbsPath) {
        
      boolean tagLibAdded = false;
      if(bDebug)
        System.out.println("jpr: " + jprAbsPath);
      if (isEmpty(jprAbsPath)) {
          System.out.println("addTagLibrary: Required Absolute jpr path (jprAbsPath) ");
          return false;
      }   

      if (xmlJpr == null) {
          System.out.println("addTagLibrary: Could not parse jpr as xml " + jprAbsPath);
          return false;
      }
            
      xmlJpr.setProperty("SelectionLanguage", "XPath");

      try {            
          tagLibAdded = addTagLibraryToJpr(xmlJpr,fixer,jprAbsPath);                    
      } catch (Exception ex) {
          System.out.println("Exception: " + ex.getMessage());
      }
      return tagLibAdded;
    }
    
    private String getRelativePathToJar(String jprAbsPath, String jarToadd) {
        
      String jarToaddRelativePath = "";
      String[] jarToAddParts = jarToadd.split("/");
      
      int fusionAppsIndex = jprAbsPath.indexOf("fusionapps/");
      String jprPathFromfusionApps = jprAbsPath.substring(fusionAppsIndex);
      String[] parts = jprPathFromfusionApps.split("/");
      
      String commonPath = "";
      for(int i =0; i< jarToAddParts.length-2 ; i++) {
          if(jprPathFromfusionApps.contains(commonPath + jarToAddParts[i] +"/" ))
              commonPath += jarToAddParts[i] + "/";
          else break;        
      }
      
      if(bDebug)
        System.out.println("commonPath: " + commonPath);
      String remainingPath = jarToadd.substring(commonPath.length());
      if(bDebug)
        System.out.print("remainingPath: " + remainingPath);                                                
      String[] commonPathParts = commonPath.split("/");    
      
      for(int i= 0; i < parts.length-commonPathParts.length-1; i++)
        jarToaddRelativePath += "../";
      
      jarToaddRelativePath += remainingPath;
      if(bDebug)
        System.out.println("Relative path to jar: " + jarToaddRelativePath);
      
      return jarToaddRelativePath;
    }
    
    private String getJarName(String jarToadd) {
      String[] parts = jarToadd.split("/");
      String jarName = parts[parts.length-1];
      return jarName;
    }
    
    private boolean jprContainsTld(XMLDocument xmlJpr, String tldName) throws Exception {
      NodeList tlds = xmlJpr.selectNodes("//hash[@n='oracle.jdevimpl.webapp.jsp.libraries.model.ProjectTagLibraries']/list[@n='tag-libraries']/hash/value[@n='tldURL']");
      for(int i =0; i < tlds.getLength(); i++) {
        if(XMLParserHelper.getAttributeValue(tlds.item(i),"v").contains(tldName)) return true;
      }
      return false;
    }
    
    private boolean addTagLibraryToJpr(XMLDocument xmlJpr,XmlFixer fixer,String jprAbsPath) throws Exception {
      
      NodeList projectTagLibraries = xmlJpr.selectNodes("//hash[@n='oracle.jdevimpl.webapp.jsp.libraries.model.ProjectTagLibraries']/list[@n='tag-libraries']");
      String jarToaddRelativePath = getRelativePathToJar(jprAbsPath, jarsToAdd.get(0));
      
      String jarName = getJarName(jarsToAdd.get(0));
    
      if(projectTagLibraries.getLength() != 1) {
        System.out.println("Could not find ProjectTagLibraries hash section with tag-libraries list in: " + jprAbsPath + ". Please edit manually");
        outputFileWriter.write("\nCould not find ProjectTagLibraries hash section with tag-libraries list in: " + jprAbsPath);
        return false;
      }    
      
      ArrayList<HashMap> tldsToAdd = new ArrayList<HashMap>();
      
      for(int i =0; i < tlds.size(); i++) {
          
        HashMap tldValues = tlds.get(i);
        if(jprContainsTld(xmlJpr,(String)tldValues.get("tldName"))) continue;
        tldsToAdd.add(tldValues);
      }
          
      if(tldsToAdd.size() == 0) {        
          System.out.println("Project already contains all the tlds in the specified jar.");
        return false;
      }
      
      String childNode = "";
      
      for(int i =0; i < tldsToAdd.size(); i++) {
          
        HashMap tldValues = tldsToAdd.get(i);
      
        childNode += "<hash>\n" + 
        "            <value n=\"jspVersion\" v=\"" + tldValues.get("jspVersion") + "\"/>\n" + 
        "            <value n=\"libraries\" v=\""+ jarName + "\"/>\n" + 
        "            <value n=\"name\" v=\"" + tldValues.get("displayName") + "\"/>\n" + 
        "            <value n=\"prefix\" v=\"" + tldValues.get("prefix") + "\"/>\n" + 
        "            <value n=\"runInEditor\" v=\"false\"/>\n" + 
        "            <value n=\"showInPalette\" v=\"true\"/>\n" + 
        "            <value n=\"tldURL\" v=\"@project@" + jarToaddRelativePath + "!/" + tldValues.get("tldName") + "\"/>\n" + 
        "            <value n=\"URI\" v=\"" + tldValues.get("uri") + "\"/>\n" + 
        "            <value n=\"version\" v=\"" + tldValues.get("tldVersion") + "\"/>\n" + 
        "         </hash>";
          
          if(i != tldsToAdd.size()-1) childNode += "\n" + fixer.getIndentation(projectTagLibraries.item(0)) + "   ";
      
      }
      
      fixer.addAsLastChildToNode(projectTagLibraries.item(0), childNode, "   ");
      
      if(checkIfLibRefExistsInClassPath(xmlJpr, jarsToAdd.get(0))) return true;
      
      return addAdfLibrary(xmlJpr,fixer,jprAbsPath);
    
    }

    public void applyFix(String absPath,
                         String newFileContents) throws Exception {
        File file = new File(absPath);
        if (!file.exists()) {
            throw new Exception("while making fix, file not found: " +
                                absPath);
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
    
    public boolean isValidDirectory(String absDirPath) {
        String absPathLowerCase = absDirPath.toLowerCase();
        if (!absPathLowerCase.contains("/dbschema/") &&
            !absPathLowerCase.contains("/.ade_path/") &&
            !absPathLowerCase.contains("/test/")&&!absPathLowerCase.contains("/classes/")) {
            return true;
        }
        return false;
    }

   private boolean isValidJpr(String absDirPath) {
        String absPathLowerCase = absDirPath.toLowerCase();
        boolean validJpr = false;
        if (!absPathLowerCase.contains("/dbschema/") &&
            !absPathLowerCase.contains("/.ade_path/") &&            
            !absPathLowerCase.contains("test.") && !absPathLowerCase.contains("/test/")
            && !absPathLowerCase.contains("/modeltest/")&&!absPathLowerCase.contains("/uitest/")
            && absPathLowerCase.endsWith(".jpr")) {

            if(projects.equals("all")) validJpr = true;
            else if(projects.equals("ui")) validJpr = absPathLowerCase.endsWith("ui.jpr");
            else if(projects.equals("model")) validJpr = absPathLowerCase.endsWith("model.jpr");
        }
        return validJpr;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }


    private static void printStatistics(Date startDate) {
        // Print Statistics
        Date endDate = new Date();
        System.out.println("=================================================");
        System.out.println("Run Statistics");
        System.out.println("Start Time: " + startDate.toString());
        System.out.println("End Time: " + endDate.toString());
        long elapsedTime = endDate.getTime() - startDate.getTime();
        System.out.println("Elapsed Time (sec): " + elapsedTime / 1000);
        System.out.println("=================================================");

        System.out.println("=================================================");
        System.out.println("Total Number of jprs reviewed: " + m_jprSeen);
        System.out.println("Total Number of jprs fixed : " + m_jprFixed);
        System.out.println("=================================================");

    }
    
    private boolean jarFileContainsTagLib(String crawlDir, String jarToadd) throws Exception{
        
      String jarPath = crawlDir.substring(0, crawlDir.indexOf("fusionapps/")) + jarToadd;
      jarFile = new JarFile(jarPath);
      Enumeration e = jarFile.entries();
      jes = new ArrayList<JarEntry>();      
      while(e.hasMoreElements()) {
        JarEntry tmp = (JarEntry)e.nextElement();
        if(tmp.getName().endsWith(".tld") && tmp.getName().contains("META-INF")) {            
            jes.add(tmp);        
        }
      }     
      if(jes.size() == 0 ) 
        return false;
      
      tlds = readTlds();
            
      return true;
    }
    
    private ArrayList<HashMap> readTlds() throws Exception{
        
        ArrayList<HashMap> result = new ArrayList<HashMap>();
        
        for(int i = 0; i < jes.size(); i++) {
            
          JarEntry je = jes.get(i);
          XMLDocument doc = XMLParserHelper.getXMLDocument(jarFile.getInputStream(je));   
          HashMap tldValues = new HashMap();
              
          tldValues.put("tldName",je.getName());
          
          NodeList list = doc.getElementsByTagName("display-name");
          tldValues.put("displayName",list.item(0).getTextContent());
          
          list = doc.getElementsByTagName("tlib-version");
          tldValues.put("tldVersion",list.item(0).getTextContent());
          
          list = doc.getElementsByTagName("short-name");
          tldValues.put("prefix",list.item(0).getTextContent());
          
          list = doc.getElementsByTagName("uri");
          tldValues.put("uri",list.item(0).getTextContent());
          
          list = doc.getElementsByTagName("taglib");
          tldValues.put("jspVersion",XMLParserHelper.getAttributeValue(list.item(0), "version"));     
            
          result.add(tldValues);          
        }
        return result;      
    }
    
    public boolean addAdfLibrary(XMLDocument jprXml,XmlFixer fixer,String absPath) {
        
      try {
        Node adf_libHashNode = JprHelper.getADFLibraryNode(jprXml);
        
        if(adf_libHashNode == null) {
          System.out.println("Could not find ADF library references section in: " + absPath);
          outputFileWriter.write("\nCould not find ADF library references section in: " + absPath);
          return false;
        }
        Node adf_libListNode = XMLParserHelper.getChildNodeWithMatchingAttribute(adf_libHashNode, "n", "classPath");
          
        if(adf_libListNode == null) {
          System.out.println("Could not find ADF library references section in: " + absPath);
          outputFileWriter.write("\nCould not find ADF library references section in: " + absPath);
          return false;
        }                    
          
        ArrayList<String> jarsToActuallyAdd = new ArrayList<String>();
        for(int i = 0; i < jarsToAdd.size(); i++) {
          if(checkIfLibRefExistsInClassPath(jprXml, jarsToAdd.get(i))) continue;
          jarsToActuallyAdd.add(jarsToAdd.get(i));
        }
        if(jarsToActuallyAdd.size() ==0) {
          System.out.println("jpr contains the specified jar reference(s)");
          return false;
        }
          
        String child = "";
        String indent = fixer.getIndentation(adf_libListNode);
          
        for(int i = 0; i < jarsToActuallyAdd.size(); i++) {          
          String relativePathToJar = getRelativePathToJar(absPath, jarsToActuallyAdd.get(i));
          child += "<url path=\"" + relativePathToJar + "\" jar-entry=\"\"/>";
          if(i != jarsToActuallyAdd.size() -1) child += "\n" + indent + "   ";
        }
       
        fixer.addAsLastChildToNode(adf_libListNode, child, "   ");
          
        return true;
      }
        catch(Exception e) {
          System.out.println("Exception: " + e.getMessage());
          return false;
        }
    }
    
    public boolean checkIfLibRefExistsInClassPath(XMLDocument jprXml, String jarToadd) throws Exception{
        
      NodeList libraryDefinitions = jprXml.selectNodes("//hash[@n='internalDefinitions']/list[@n='libraryDefinitions']");
      
      if(libraryDefinitions == null || libraryDefinitions.getLength() != 1) 
       return false;
        
      Node internalDefinition = libraryDefinitions.item(0);
      NodeList children = internalDefinition.getChildNodes();
      
      String[] parts = jarToadd.split("/");
      String jarName = parts[parts.length-1];
      
      for(int i = 0; i < children.getLength(); i++) {        
        Node node = children.item(i);
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
                            String path = XMLParserHelper.getAttributeValue(possibleUrlNode, "path");        
                            if(path != null && path.contains(jarName)) return true;
                        }
                    }
                }
            }
        }
      }
      return false;
    }
    
  /**
   * Valid - if it is a jar and it starts with adf
   * @param jarName
   * @return
   */
  public boolean isValidAdfJar(String jarName) {   

      if (isEmpty(jarName)) 
          return false;      
      if (jarName.endsWith(".jar") &&
          jarName.trim().toLowerCase().startsWith("adf")) 
          return true;
      
      return false;
  }
  
 /* public boolean addLibInClassPath(XMLDocument xmlJpr, String jprAbsPath, XmlFixer fixer) throws Exception{
      
    NodeList libraryDefinitions = xmlJpr.selectNodes("//hash[@n='internalDefinitions']/list[@n='libraryDefinitions']");
    String jarToaddRelativePath = getRelativePathToJar(jprAbsPath);
    String jarName = getJarName();
    
    String childNode1 = "<hash>\n" + 
    "               <list n=\"classPath\">\n" + 
    "                  <url path=\"" + jarToaddRelativePath + "\" jar-entry=\"\"/>\n" + 
    "               </list>\n" + 
    "               <value n=\"deployedByDefault\" v=\"true\"/>\n" + 
    "               <value n=\"description\" v=\"" + jarName + "\"/>\n" + 
    "               <value n=\"id\" v=\"" + jarName + "\"/>\n" + 
    "            </hash>";
    
    NodeList exportedReferences = xmlJpr.selectNodes("//hash[@n='oracle.jdevimpl.config.JProjectLibraries']/list[@n='exportedReferences']");
    
    if(exportedReferences == null || exportedReferences.getLength() != 1) {
      System.out.println("addLibInClassPath: Could not find exportedReferences section in: " + jprAbsPath);
      outputFileWriter.write("\nCould not add the library reference in class path: " + jprAbsPath);
      return false;
    }
    
    if(libraryDefinitions == null || libraryDefinitions.getLength() != 1) {
      System.out.println("addLibInClassPath: Could not find libraryDefinitions section in: " + jprAbsPath);        
      
      fixer.addSiblingAfterNode(exportedReferences.item(0), "<hash n=\"internalDefinitions\">\n" + 
      "         <list n=\"libraryDefinitions\">\n" + fixer.getIndentation(exportedReferences.item(0))
                + "      " + childNode1 + "\n" + fixer.getIndentation(exportedReferences.item(0)) + "   </list>\n" + 
      "      </hash>");
    }
    else {
      fixer.addAsLastChildToNode(libraryDefinitions.item(0), childNode1, "   ");
    }
    
    String childNode = "<hash>\n" + 
    "            <value n=\"id\" v=\"" + jarName + "\"/>\n" + 
    "            <value n=\"isJDK\" v=\"false\"/>\n" + 
    "         </hash>";
    
    fixer.addAsLastChildToNode(exportedReferences.item(0), childNode, "   ");
    
    NodeList libraryReferences = xmlJpr.selectNodes("//list[@n='libraryReferences']");
    if(libraryReferences == null || libraryReferences.getLength() != 1) {
      System.out.println("Could not find libraryReferences section in: " + jprAbsPath);
      outputFileWriter.write("\nCould not find libraryReferences section in: " + jprAbsPath);
      return false;
    }
    fixer.addAsLastChildToNode(libraryReferences.item(0), childNode, "   ");
    return true;
  }*/
}
