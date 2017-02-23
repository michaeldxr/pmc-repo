package oracle.apps.warnings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JavaFileParserHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NonSerializableSdoInterfacesLRG {
    private static Writer writer;
    private static int vo_files = 0;
    private static int num_violations = 0;
    private static HashSet<String> exemptions = new HashSet<String>();

    private static final Pattern pExtendSerializable = Pattern.compile("interface.+?extends.*?Serializable",Pattern.DOTALL);
    private static final String issuetype = "SDOInterfaceNotSerializable";
    
    public static void main(String[] args) throws Exception {
        try {
            if (args.length != 1 || args[0] == null || args[0].equals("")) {
                System.out.println("Series has to be specified for " +
                                   issuetype);
                System.exit(1);
            }

            String series = args[0];
        
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running " + issuetype + " Scan on series: " +
                               series + ".......");
            System.out.println("Latest label is: " + label);

            writer = new BufferedWriter(new FileWriter("SDOInterfaceNotSerializable_LRG.csv") );
            writer.write("Family, Module, SubModule, Filename, Label, SdoGen, SdoName, SdoFilePath\n");
            
            generateExemptionList();
            
            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
            for (int j = 0; j < families.size(); j++) {
                String family = families.get(j);
                String sCrawlDir = LRGUtil.getCrawlDir(series, family, label, issuetype);
                System.out.println("Crawling dir '" + sCrawlDir +"' for family: " + family);
                
                NonSerializableSdoInterfacesLRG scanner = new NonSerializableSdoInterfacesLRG();
                scanner.crawlDirectory(sCrawlDir, false); 
                
                System.out.println("Crawling done for family "+family+".");
            }
            writer.close();

            LRGUtil.addLabelInfoToAB(issuetype, series, label);
        } catch (Exception e) {
            e.printStackTrace();
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
                    processFile(listOfFiles[i]);              
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
    
    public void processFile(File fName) 
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
                    writer.write(FamilyModuleHelper.getFileNameInfo(fName.getCanonicalPath()) + sdoGen + "," + sdoName + "," + sdoFilePath + "\n");
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
    
    
    private static void generateExemptionList() {
        
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
        e.printStackTrace();
    }
    }
    
    private boolean isExempt(String fileName){
        
        if(exemptions.contains(fileName))
            return true;
        
        return false;
    }
    

}