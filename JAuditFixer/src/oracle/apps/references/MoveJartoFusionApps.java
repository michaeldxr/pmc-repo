package oracle.apps.references;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;

import java.io.Writer;

import java.util.Date;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MoveJartoFusionApps {

    /**
     * Number of jprs seen
     */
    static int m_jprSeen = 0;

    /**
     * Number of jprs fixed
     */
    static int m_jprFixed = 0;
    
    /**
     * Number of libs seen
     */
    static int m_libSeen = 0;

    /**
     * Number of libs fixed
     */
    static int m_libFixed = 0;

     /**
     * Name of the diff file
     */
    static String m_diffFileName = "diff.txt";
     
    static Writer writer;
    
    static String jarName = "fusionapps/jlib/AdfFinFunSharedPublicModel.jar";
    
    /**
     * Diff helper to write diff output to the file
     */
    AdeDiffHelper diffHelper = new AdeDiffHelper(m_diffFileName);

    /**
     * debug flag - used to print sop statements when in debug mode
     */
    boolean bDebug = false;


    public static void main(String[] args) {

      Date startDate = new Date();

      String sCrawlDir = args[0];
      Boolean bDoADE = new Boolean(args[1]);       
        
      MoveJartoFusionApps moveJar = new MoveJartoFusionApps();      
           
      System.out.println("============= Analyzing My Directory " + sCrawlDir);
      
      String jarPath = sCrawlDir.substring(0, sCrawlDir.indexOf("fusionapps/")) + jarName;
      if(!(new File(jarPath)).exists()) {
        System.out.println(jarName + " - Specified jar file does not exist. Please ensure that the jar name is correct");
        System.exit(1);
      }
      
      try {
          writer = new BufferedWriter(new FileWriter("not_modified.txt") );
          if (bDoADE) {
              // Remove any existing diff.txt. Create a new bufferred writer for diff.txt
              moveJar.diffHelper.startDiffLog();
          }         
          writer.write("The following jprs were not modified.\n\n");          
          moveJar.crawlDirectory(sCrawlDir, bDoADE);          
          writer.close();

          if (bDoADE.booleanValue()) {
              moveJar.diffHelper.closeDiffLog();
          }

      } catch (Exception ex) {
          ex.printStackTrace();
      }

      printStatistics(startDate);
    }

    /**
     * Crawls the directory and for every jpr found
     * - Moves jar reference to AdfFinFunSharedPublicModel.jar
     * @param path
     * @param bDoADE
     */
    private void crawlDirectory(String path, Boolean bDoADE) throws Exception {
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
                    if(bDebug)
                      System.out.println("Analyzing jpr: " + absFilePath);
                    processJpr(absFilePath, bDoADE);                    
                }
                else if (isValidLibrary(absFilePath)) {
                    if(bDebug)
                      System.out.println("Analyzing library: " + absFilePath);
                    processLib(absFilePath, bDoADE);                    
                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();                
                crawlDirectory(dir, bDoADE);                
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    public void processJpr(String absPath, boolean bDoADE) throws Exception {

        m_jprSeen++;

        XMLDocument jprXml = XMLParserHelper.getXMLDocument(absPath);
        XmlFixer fixer = null;
        boolean bFixMade = false;

        try {
            fixer = new XmlFixer(absPath);
        } catch (Exception ex) {
            writer.write("Skipping file.. could not initialze fixer: " +
                               absPath + "\n");
            System.out.println("Skipping file.. could not initialze fixer: " +
                               absPath);
            return;
        }

        if (jprXml == null) {
            writer.write("Skipping file .. Could not parse jpr as xml " +
                               absPath + "\n");
            System.out.println("Skipping file .. Could not parse jpr as xml " +
                               absPath);
            return;
        }
          bFixMade = moveJarToFusionap(jprXml,fixer,absPath);
        
        if (bFixMade) {
            String newFileContents = fixer.getFileContentsAsString(false);
            try {
                if (bDoADE) {
                    applyFix(absPath, newFileContents);
                }
            } catch (Exception ex) {
                writer.write("Exception occurred while apply fix for file: " +
                                   absPath + "\n");
                System.out.println("Exception occurred while apply fix for file: " +
                                   absPath);
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }

            m_jprFixed++;
        }
    }
    
    public void processLib(String absPath, boolean bDoADE) throws Exception {

        m_libSeen++;

        XMLDocument libXml = XMLParserHelper.getXMLDocument(absPath);
        XmlFixer fixer = null;
        boolean bFixMade = false;

        try {
            fixer = new XmlFixer(absPath);
        } catch (Exception ex) {
            writer.write("Skipping file.. could not initialze fixer: " +
                               absPath + "\n");
            System.out.println("Skipping file.. could not initialze fixer: " +
                               absPath);
            return;
        }

        if (libXml == null) {
            writer.write("Skipping file .. Could not parse lib as xml " +
                               absPath + "\n");
            System.out.println("Skipping file .. Could not parse lib as xml " +
                               absPath);
            return;
        }
          bFixMade = moveLibToFusionap(libXml,fixer,absPath);
        
        if (bFixMade) {
            String newFileContents = fixer.getFileContentsAsString(false);
            try {
                if (bDoADE) {
                    applyFix(absPath, newFileContents);
                }
            } catch (Exception ex) {
                writer.write("Exception occurred while apply fix for file: " +
                                   absPath + "\n");
                System.out.println("Exception occurred while apply fix for file: " +
                                   absPath);
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }

            m_libFixed++;
        }
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

   private boolean isValidJpr(String absDirPath) {
        String absPathLowerCase = absDirPath.toLowerCase();
        if (absPathLowerCase.endsWith(".jpr")) 
            return true;        
        return false;
    }
   
   private boolean isValidLibrary(String absDirPath) {
       if(absDirPath.contains("ADF_Library_Dependencies.library")) return true;
       return false;
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
        System.out.println("Total Number of libraries reviewed: " + m_libSeen);
        System.out.println("Total Number of libraries fixed : " + m_libFixed);
        System.out.println("=================================================");

    }
    
   
    
    public boolean moveJarToFusionap(XMLDocument jprXml,XmlFixer fixer,String absPath) throws Exception {
      
      NodeList libraryDefinitions = jprXml.selectNodes("//hash[@n='internalDefinitions']/list[@n='libraryDefinitions']");
      
      if(libraryDefinitions == null || libraryDefinitions.getLength() != 1)  
        return false;      
        
      Node internalDefinition = libraryDefinitions.item(0);
      NodeList children = internalDefinition.getChildNodes();
      
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
                            if(path != null && path.contains("AdfFinFunSharedPublicModel.jar")) { 
                               String newPath = getRelativePathToJar(absPath, jarName);
                               if(newPath.equals(path)) return false;
                               fixer.modifyAttribute(possibleUrlNode, "path", path, newPath, false);
                               return true;
                            }
                        }
                    }
                }
            }
        }
      }      
      return false;
  }  
    
    public boolean moveLibToFusionap(XMLDocument libXml,XmlFixer fixer,String absPath) throws Exception {
      
      Node libNode = XMLParserHelper.getChildNodeWithName(libXml, "JLibraryNode");
      Node classPathNode = XMLParserHelper.getChildNodeWithName(libNode, "classPath");
      Node entries = XMLParserHelper.getChildNodeWithName(classPathNode, "entries");
      
      NodeList items = entries.getChildNodes();
      
      for(int i = 0; i < items.getLength(); i++) {        
        Node node = items.item(i);
        String nodeName = node.getNodeName();
        if (!isEmpty(nodeName) && nodeName.trim().equals("Item")) {
            
            String path = XMLParserHelper.getAttributeValue(node, "path");        
            if(path != null && path.contains("AdfFinFunSharedPublicModel.jar")) { 
               String newPath = getRelativePathToJar(absPath, jarName);
               if(newPath.equals(path)) return false;
               fixer.modifyAttribute(node, "path", path, newPath, false);
               return true;  
            }
        }
      }      
      return false;
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
}
