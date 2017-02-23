package oracle.apps.warnings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.Writer;


import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JavaFileParserHelper;
import oracle.apps.helpers.Mail;
import oracle.apps.helpers.XMLParserHelper;
import oracle.xml.parser.v2.XMLDocument;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NonSerializableSdoInterfaces {
    public static Writer outputFileWriter;
    static int vo_files = 0;
    static int num_violations = 0;
    static int num_fixes = 0;
    static BufferedWriter errorWriter;
    
    private Pattern pInterface = Pattern.compile("public\\s+interface\\s+(\\w+)(\\s+extends\\s+.+?)?\\s*\\{");
    private Pattern pExtendSerializable = Pattern.compile("interface.+?extends.*?Serializable",Pattern.DOTALL);
    
    static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
    HashSet<String> exemptions = new HashSet<String>();
    
    public static void main(String[] args) throws Exception {
        
        if(args == null || args.length < 1){
          System.out.println("Crawl directory has to be specified");
          System.exit(1);
        }
        String sCrawlDir = args[0].trim();
    
        if (sCrawlDir == null || sCrawlDir.equals("")) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }
        
        File f = new File(sCrawlDir);
        if(!f.exists()){
            System.out.println("Crawl directory does not exist - " + sCrawlDir);
            System.exit(1);
        }
        
        boolean bDoAde = false; 
        if(args.length > 1)
           bDoAde = new Boolean(args[1].trim());
        
        boolean append = false;
        if(args.length > 2)
           append = new Boolean(args[2].trim());
        
        System.out.println("Analyzing all files in directory:" + sCrawlDir);
        
        NonSerializableSdoInterfaces scanner = new NonSerializableSdoInterfaces();
        try {         
            outputFileWriter = new BufferedWriter(new FileWriter("sdo_interfaces_scan.csv", append) );
            errorWriter = new BufferedWriter(new FileWriter("errors.txt"));
            if(!append)
                outputFileWriter.write("Family, Module, SubModule, Filename, Label, SdoGen, SdoName, SdoFilePath\n");     
            scanner.generateExemptionList();
            scanner.crawlDirectory(sCrawlDir, false); 
            outputFileWriter.close();
            
            if(bDoAde) {
                System.out.println("Making the fixes ... ");
                diffHelper.startDiffLog();
                scanner.makeFixes();
                diffHelper.closeDiffLog(); 
            }
            String summary = "";
            summary += "Number of VOs processed: "+ vo_files + "\n";
            summary += "Number of Violations: "+ num_violations + "\n";
            summary += "Number of Fixes made: "+ num_fixes + "\n";
            
            System.out.println(summary);
            errorWriter.close();
            
            if(bDoAde) {
                Date runDate = new Date();       
               // Mail.sendMail("sudipti.gupta@oracle.com", summary, "SCRIPT: to fix AppsLogger violations run at: " + runDate.toString());
            }
          
        } catch (Exception e) {
            e.printStackTrace();     
            //Mail.sendMail("sudipti.gupta@oracle.com", e.getMessage(), "SCRIPT: SDO Interface Scan run on " + sCrawlDir + " failed!!!");
        }
    }
    
    private void makeFixes() throws Exception{
        
        BufferedReader reader = new BufferedReader(new FileReader("sdo_interfaces_scan.csv"));
        String line = reader.readLine();
        while((line = reader.readLine()) != null) {
            
            String[] parts = line.split(",");
            if(parts.length < 8) {
                errorWriter.write("Could not fix " + line + "\n");
                continue;     
            }
            
            String sdoFile = parts[7].trim();
            try{
               // System.out.println("Fixing: " + viewRoot + sdoFile);
                JavaFileParserHelper javaHelper = new JavaFileParserHelper(sdoFile);
                
                String fileContents = javaHelper.getFileContentsAsString(false);
                
                boolean addImport = false;
                
                if(!fileContents.contains("import java.io.Serializable")){
                    javaHelper.removeAllComments();
                    addImport = javaHelper.addImport("import java.io.Serializable;");        
                }
                
                fileContents = javaHelper.getFileContentsAsString(false);
                
                Matcher m = pInterface.matcher(fileContents);
                if(m.find()){
                    String interfaceName = m.group(1);
                    String extendString = m.group(2);

                    if(extendString == null || extendString.equals(""))
                        fileContents = fileContents.replaceAll("public\\s+interface\\s+\\w+\\s*\\{", "public interface " + interfaceName + " extends Serializable {");
                    else{
                        if(!extendString.contains("Serializable"))
                            fileContents = fileContents.replaceAll("public\\s+interface\\s+(\\w+)(\\s+extends\\s+.+?)?\\s*\\{",
                                                                   "public interface " + interfaceName + extendString +",Serializable {");
                    }
                        
                    if(!fileContents.contains("import java.io.Serializable"))
                        errorWriter.write("Could not add import " + line + "\n");
                    else{
                        applyFix(sdoFile, fileContents);
                        num_fixes++;       
                    }                   
                } 
                else
                  errorWriter.write("Could not fix " + line + "\n");
            }catch(Exception e) {
                errorWriter.write("Could not fix " + line + "\n");
            }
        }
    }
    
    private void crawlDirectory(String path, boolean bDoADE) 
    {
        if(path.contains(".ade_path") || path.contains("/classes/"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
            
        if(listOfFiles == null)
            return;
        
        for(int i = 0; i <listOfFiles.length; i++)
        {
            if(listOfFiles[i].isFile()) 
            {
                String sName = listOfFiles[i].getAbsolutePath();                
                if(fileOfInterest(sName))                 
                    processFile(listOfFiles[i], bDoADE);              
            }
            else if(listOfFiles[i].isDirectory()) {
                crawlDirectory(listOfFiles[i].getAbsolutePath(), bDoADE);
            }
            else 
                System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);            
        }
        
    }
    
    public boolean fileOfInterest(String sName) 
    {
        if(!FamilyModuleHelper.isPathOfInterest(sName))
                return false;   
        
        if (sName.endsWith("VO.xml") && sName.contains("/link/") == false 
            && sName.contains("/association/") == false && sName.contains("/test/") == false)
            return true;
        
        return false;
    }
    
    public void processFile(File fName, boolean bDoADE) 
    {
      String f = fName.getAbsolutePath();
      if(f.contains("/flex/") || f.contains("/publicFlex/") || f.contains("/viewLink/") || f.contains("/noship"))
          return;
      
      String absPathLowerCase = f.toLowerCase();
      if (absPathLowerCase.contains("test.") || absPathLowerCase.contains("/test/") || 
          absPathLowerCase.contains("/modeltest") || absPathLowerCase.contains("/uitest/")
          || absPathLowerCase.contains("/testui") || absPathLowerCase.contains("/servicetest") 
          || absPathLowerCase.contains("/publicservicetest") || absPathLowerCase.contains("/publicuitest")
          || absPathLowerCase.contains("/publicmodeltest") || absPathLowerCase.contains("/structuretest"))
          return;

      vo_files++;

      try {
          
          XMLDocument doc = XMLParserHelper.getXMLDocument(f);
          
          NodeList viewObjects = doc.getElementsByTagName("ViewObject");
          if(viewObjects == null || viewObjects.getLength() != 1) 
          {
            System.out.println("WARNING:  Unexpected number of view objects found for file:  " +f);
            System.out.println("Skipping...");
            return;
          }
         
          Node viewObject = viewObjects.item(0);  
          String sdoGen = XMLParserHelper.getAttributeValue(viewObject, "SdoGen");
          String sdoName = XMLParserHelper.getAttributeValue(viewObject, "SdoName");          
          
          if(sdoName != null) {
              String sdoFilePath = findSdoFilePath(sdoName,f);
              if(!sdoFilePath.equals("File Not Found")){
                  JavaFileParserHelper helper= new JavaFileParserHelper(sdoFilePath);
                  String fileContents = helper.getFileContentsAsString(false);
                  Matcher m = pExtendSerializable.matcher(fileContents);
                  if(!m.find() && !isExempt(sdoFilePath)) {
                    outputFileWriter.write(FamilyModuleHelper.getFileNameInfo(fName.getCanonicalPath()) + sdoGen + "," + sdoName + "," + sdoFilePath + "\n");
                    num_violations++;
                  }
              }
          }

      } catch (Exception e) {
          e.printStackTrace();
          System.err.println("ERROR:  Error while processing file.  Please review manually: " +fName.getAbsolutePath());
          return;
      }
    }
    
    
    private String findSdoFilePath(String sdoName, String absPath) {
        
        String sdoFilePath = "File Not Found";
        int slashIndex = absPath.lastIndexOf("/");
        String prospectiveSdoPath = absPath.substring(0,slashIndex);
        
        slashIndex = prospectiveSdoPath.lastIndexOf("/");
        prospectiveSdoPath = prospectiveSdoPath.substring(0,slashIndex) + "/" + sdoName + ".java";
        
        File f = new File(prospectiveSdoPath);
        if(f.exists())            
            return f.getAbsolutePath();    
        else        
            return sdoFilePath;
    }
    
    private void applyFix(String absPath,
                        String newFileContents) throws Exception {
       
       File file = new File(absPath);
       if (!file.exists()) {
           throw new Exception("while making fix, file not found: " +
                               absPath);
       }
       diffHelper.checkoutAndDelete(file);
        
       System.out.println("Writing new file...");
       FileWriter fw = new FileWriter(file.getAbsolutePath());
       BufferedWriter bw = new BufferedWriter(fw);
       bw.write(newFileContents);
       if (bw != null)
           bw.close();
       if (fw != null)
           fw.close();
       diffHelper.checkinAndDiff(file);
    }
    
    public void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 2)
            continue;
        String issue = parts[1].trim();
        if(!issue.equals("SDOInterfaceNotSerializable"))
            continue;
        String fileName = parts[0].trim();
          
        exemptions.add(fileName);
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }
    
    private boolean isExempt(String fileName){
        
        if(exemptions.contains(fileName))
            return true;
        
        return false;
    }
    

}

