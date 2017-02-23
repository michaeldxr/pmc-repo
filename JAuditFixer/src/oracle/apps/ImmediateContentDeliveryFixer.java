package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.ProcessExecutor;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ImmediateContentDeliveryFixer {
   
    static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
    static BufferedReader reader;
    static BufferedWriter errorWriter;
    static String viewRoot;
    
    public static void main(String[] args) throws Exception{
        
        viewRoot = args[0].trim();
        if(args.length<2 || args[1]==null || args[1].equals("")) {
            System.out.println("Path to the csv file containing content violations must be specified.");
            System.out.println("Usage: contentDeliveryFinder.sh <path to csv containing violations>");
            System.exit(1);
        }
        
        if(args[1].trim().endsWith(".xlsx") || args[1].trim().endsWith(".xls")) {
            System.out.println("Please save the excel spreadsheet as a csv file and provide that as arg to the script.");
            System.out.println("Usage: contentDeliveryFinder.sh <path to csv containing violations>");
            System.exit(1);
        }
        
        File f = new File(args[1].trim());
        if(!f.exists()) {
            System.out.println("csv file does not exist.");
            System.out.println("Usage: contentDeliveryFinder.sh <path to csv containing violations>");
            System.exit(1);
        }
        
        reader=new BufferedReader(new FileReader(f));
        ImmediateContentDeliveryFixer fixer = new ImmediateContentDeliveryFixer();
        errorWriter = new BufferedWriter(new FileWriter("errors.txt"));
        diffHelper.startDiffLog();
        fixer.makeFixes();
        diffHelper.closeDiffLog(); 
        errorWriter.close();
    }
    
    private void makeFixes() throws Exception{
        
        String line = reader.readLine(); //first line is the header - read & ignore
        String prevFilePath = null;
        XMLDocument doc = null;
        XmlFixer xmlFixer = null;
        String fileContentsAsString = null;
        BufferedWriter writer1 = new BufferedWriter(new FileWriter("CouldNotFix.txt"));
        
        while((line = reader.readLine()) != null) {
          
          try{            
            String[] parts = line.split(",");
              if(parts.length < 11) {
                writer1.write("Cannot fix due to some missing info:" + line);
                continue;
              }
            
            String filePath = viewRoot + "/" + parts[5].trim();
            String componentId = parts[6].trim(); 
            String oldContentDelivery = parts[8].trim();
            String newContentDelivery = parts[10].trim();
            String nodeType = parts[7].trim();
            
            if(!newContentDelivery.equals("default") && !newContentDelivery.equals("lazy") 
               && !newContentDelivery.equals("immediate")) {
                   writer1.write("Script cannot fix...unrecognized contentDelivery for node with id: " + componentId + " in file: " + parts[5]);
                   continue;
            }                
          
            if(!filePath.equals(prevFilePath)) {
              
                if(prevFilePath != null) {
                  fileContentsAsString = xmlFixer.getFileContentsAsString(false);    //this will get the changed file contents  
                  diffHelper.applyFix(prevFilePath,fileContentsAsString); 
                }
              
              doc = XMLParserHelper.getXMLDocument(filePath);     
                if(doc == null) 
                    continue;
              xmlFixer = new XmlFixer(filePath);
            }
            prevFilePath = filePath;
            
            if(doc == null) 
                continue;
              
            NodeList components = doc.getElementsByTagName(nodeType);
            if(components == null || components.getLength() < 1)
                continue;
            Node n = null;
              
            for(int i = 0; i < components.getLength(); i++) {
              n = components.item(i);
              String id = XMLParserHelper.getAttributeValue(n, "id");
              if(id !=null && id.equals(componentId))
                  break;
              else
                  n=null;
            }
              
            if(n !=null) {
                  String existingContentDelivery = XMLParserHelper.getAttributeValue(n, "contentDelivery");
                  if(existingContentDelivery == null )
                      existingContentDelivery="default";
                  
                  if(!existingContentDelivery.equals(oldContentDelivery)) {
                      System.out.println("contentDelivery for node with id " + componentId + " in file " + 
                        parts[5].trim() + " does not match with the csv file. Script will not fix it.");
                      continue;
                  }
                  if(newContentDelivery.equals("default") && !oldContentDelivery.equals("default"))
                    xmlFixer.removeAttribute(n, "contentDelivery", oldContentDelivery);
                  else if(!newContentDelivery.equals("default") && oldContentDelivery.equals("default"))
                      xmlFixer.addAttribute(n,"contentDelivery",newContentDelivery);
                  else if(!newContentDelivery.equals(oldContentDelivery))
                      xmlFixer.modifyAttribute(n,"contentDelivery",oldContentDelivery,newContentDelivery,false);
            }         

            } catch (Exception e) {
                e.printStackTrace();
            }
        } //while
        
        if(xmlFixer != null) { //to take care of the last set of violations in the last file
          fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
          diffHelper.applyFix(prevFilePath,fileContentsAsString);
        }
        writer1.close();
        reader.close();
    }
    
}
