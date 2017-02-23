package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.xml.parser.v2.XMLDocument;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class ViewCriteriaEffectiveDates {
    
    static BufferedWriter writer;
    static String sCrawlDir;
    
    public static void main(String[] args) throws Exception
    {
       if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
            System.out.println("Usage:  reviewVCEffectiveDates.sh");
            System.exit(1);
        }
       sCrawlDir = args[0].trim();

        File f = new File(sCrawlDir);
        if(!f.exists()) {
            System.out.println("Crawl directory does not exist - " + sCrawlDir);
            System.exit(1);
        }
        
        System.out.println("Analyzing all files in directory: " + sCrawlDir);
        
        ViewCriteriaEffectiveDates civc = new ViewCriteriaEffectiveDates();
        
        writer = new BufferedWriter(new FileWriter("vc_effective_date_scan.csv"));
        
        writer.write("Family,Module,Product,Filename,Label,ViewCriteriaItem,ViewAttribute, Value,Required,Operator\n");
        
        civc.crawlDirectory(sCrawlDir);
        writer.close();
    }
    
    private void crawlDirectory(String path) throws Exception
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
            else if(listOfFiles[i].isDirectory()) 
                crawlDirectory(listOfFiles[i].getAbsolutePath());          
            else 
                System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);          
        }      
    }
    
    public boolean fileOfInterest(String sName) 
    {
        if(!FamilyModuleHelper.isPathOfInterest(sName))
            return false;  
        
      if (sName.endsWith("VO.xml"))
          return true;
      return false;
    }
    
    public void processFile(File fName) {
        
        try{
            String f = fName.getCanonicalPath();
            XMLDocument doc = XMLParserHelper.getXMLDocument(f);
    
            if(doc == null) {
              System.out.println("Could not parse XMLDocument..Skipping");
              return;
            }
            
              NodeList viewObjects = doc.getElementsByTagName("ViewObject");
              if(viewObjects == null || viewObjects.getLength() != 1) 
              {
                System.out.println("WARNING:  Unexpected number of view objects found");
                System.out.println("Skipping...");
                return;
              }
              NodeList vc_items = doc.getElementsByTagName("ViewCriteriaItem"); 
            if(vc_items == null)
                return;
            
            for(int i = 0; i < vc_items.getLength(); i++) {
                Node vc_item = vc_items.item(i);
                
                String va = XMLParserHelper.getAttributeValue(vc_item,"ViewAttribute");
                if(va.toLowerCase().contains("effective") && va.toLowerCase().contains("date")) {
                    writer.write(ViewCriteriaHelper.getFileNameInfo(f, "") 
                                 + XMLParserHelper.getAttributeValue(vc_item,"Name") + ","
                                 + va + "," + XMLParserHelper.getAttributeValue(vc_item,"Required") + "," 
                                 + XMLParserHelper.getAttributeValue(vc_item,"Value") + "," 
                                 + XMLParserHelper.getAttributeValue(vc_item,"Operator") + "\n");
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    
    
}
