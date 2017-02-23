package oracle.apps.warnings;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;

import java.util.HashMap;
import java.util.HashSet;

import java.util.Iterator;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DuplicateManagedBeanDefinition {
   
   private static int adfc_files = 0;
   HashMap<String,HashSet<String>> managedBeanConfigfiles = new HashMap<String,HashSet<String>>();
   
   public static void main(String[] args) {
       
       if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
           System.out.println("Crawl directory has to be specified");
           System.exit(1);
       }       
       String sCrawlDir = args[0];
       DuplicateManagedBeanDefinition scanner = new DuplicateManagedBeanDefinition();
       
       try {         
            //csvWriter = new BufferedWriter(new FileWriter("AppsLoggerMismatches.csv"));

            scanner.crawlDirectory(sCrawlDir);       
            System.out.println("Number of adfc_config files processed: "+ adfc_files); 
           
            scanner.writeHashMapToFile();

         
       } catch (Exception e) {
           e.printStackTrace();               
       }
   }
   
   private void writeHashMapToFile() throws Exception
   {
       BufferedWriter writer = new BufferedWriter(new FileWriter("managedBeansAdfcConfigs.txt"));
       
       for(Iterator i = managedBeanConfigfiles.keySet().iterator(); i.hasNext(); ){
           
           String managedBeanName = (String)i.next();
           HashSet<String> files = managedBeanConfigfiles.get(managedBeanName);
           if(files.size() <= 1) continue;
           
           writer.write(managedBeanName + " -->\n");
           for(Iterator i1 = files.iterator(); i1.hasNext();)
               writer.write("\t" + (String)i1.next() + "\n");
           
           writer.write("\n\n\n");
       }
       
       writer.close();
   }
   
    private void crawlDirectory(String path) 
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

    private boolean fileOfInterest(String sName) 
    {
        String absPathLowerCase = sName.toLowerCase();
        
        if (absPathLowerCase.contains("/publicservicetest/") || absPathLowerCase.contains("/modeltest") ||
            absPathLowerCase.contains("/uitest/") || absPathLowerCase.contains("/testui") ||
            absPathLowerCase.contains("/test/") || absPathLowerCase.contains("/noship") ||
            absPathLowerCase.contains("/servicetest") || absPathLowerCase.contains("/publicmodeltest") ||
            absPathLowerCase.contains("/publicuitest") || absPathLowerCase.contains("/uimodeltest") ||
            absPathLowerCase.contains("/structuretest"))
            return false;
        if (sName.contains("adfc-config.xml")) {
          adfc_files++;
          return true;
        }
      return false;
    }
    
    private void processFile(File f) {
        
        try{
            XMLDocument doc = XMLParserHelper.getXMLDocument(f.getAbsolutePath());  
            NodeList managedbeans = doc.getElementsByTagName("managed-bean-name");
            
            for(int i = 0; i < managedbeans.getLength(); i++) {
                Node n = managedbeans.item(i);
                String managedBeanName = n.getTextContent().trim();
                HashSet<String> adfcfiles;
                if(managedBeanConfigfiles.containsKey(managedBeanName))
                    adfcfiles = managedBeanConfigfiles.get(managedBeanName);
                else
                    adfcfiles = new HashSet<String>();
                
                adfcfiles.add(getPathAfterFusionApps(f.getAbsolutePath()));
                managedBeanConfigfiles.put(managedBeanName,adfcfiles);
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }    
    }
    
    private String getPathAfterFusionApps(String absPath) {
        
        int i = absPath.indexOf("fusionapps/");
        String pathAfterFusionApps = absPath.substring(i);
        return pathAfterFusionApps;
    }
                                   
}
