package oracle.apps.rup2scans;

import java.io.BufferedWriter;

import java.io.File;
import java.io.FileWriter;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class SerializationTFScanner {
    
    static BufferedWriter csvWriter;
    
    public static void main(String[] args) throws Exception {             
     
        if(args == null || args.length < 1){
          System.out.println("Crawl directory has to be specified");
          System.exit(1);
        }
        String sCrawlDir = args[0];
       
        if (sCrawlDir == null || sCrawlDir.trim().equals("")) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }
        
        csvWriter = new BufferedWriter(new FileWriter("Serialization_issues.csv"));
        csvWriter.write("Family,Module,Product,FileName,Label,inputParamName,inputParamValue,inputParamClass\n");
        
        
        boolean append = false;
        if(args.length > 1)
           append = new Boolean(args[1].trim());
        
        System.out.println("Analyzing all files in directory:" + sCrawlDir);

        SerializationTFScanner scanner = new SerializationTFScanner();
        scanner.crawlDirectory(sCrawlDir); 
        csvWriter.close();
    }
    
    private void crawlDirectory(String path) throws Exception {
        
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
        
      String absPathLowerCase = sName.toLowerCase();
      if ( absPathLowerCase.contains("/test/")  
          || absPathLowerCase.contains("modeltest") || absPathLowerCase.contains("/uitest") || absPathLowerCase.contains("/noship/")
          || absPathLowerCase.contains("/publicuitest")  
          || absPathLowerCase.contains("servicetest") || absPathLowerCase.contains("/testui")
          || absPathLowerCase.contains("/structuretest"))
          return false;
      
      if (sName.endsWith(".xml"))
        return true;
      
        return false;
    }
    
    private void processFile(File f) throws Exception {
        
        XMLDocument doc = null;
        try{
            doc = XMLParserHelper.getXMLDocument(f.getAbsolutePath()); 
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }
        
        if(doc == null)
            return;
      
      //Scan for setAction Listener
      NodeList tfnodes = doc.getElementsByTagName("task-flow-definition");  
      if(tfnodes.getLength() == 0)
          return;
      
      NodeList nodes = doc.getElementsByTagName("input-parameter-definition");  
      
      for(int i = 0; i < nodes.getLength(); i++) {
          
        Node n = nodes.item(i);
        Node name = XMLParserHelper.getChildNodeWithName(n, "name");
        Node value = XMLParserHelper.getChildNodeWithName(n, "value");
        Node type = XMLParserHelper.getChildNodeWithName(n, "class");
          
        String paramName = " ";
        String paramValue = " ";
        String paramClass = " ";
          
        if(name != null)
            paramName = name.getTextContent().trim();
        if(value != null)
            paramValue = value.getTextContent().trim();
        if(type != null)
            paramClass = type.getTextContent().trim();
          
        csvWriter.write(getFileNameInfo(f.getCanonicalPath()) + paramName + "," + paramValue + "," + paramClass + "\n");    
       
      }
    }
    
    private String getFileNameInfo(String absPath) {
            
          int i = absPath.indexOf("fusionapps/");
          String pathAfterFusionApps = absPath.substring(i);
          String[] parts = pathAfterFusionApps.split("/");
          String family = parts[1].toUpperCase();
          String module = parts[3];
          String product = parts[4].toUpperCase();
          
          String path_before_fusionapps = absPath.substring(0, i-1);
          parts = path_before_fusionapps.split("/");
          String label = parts[parts.length -2] + "_" + parts[parts.length -1];
         
          label = label.replace(".rdd","");

          return family+","+module+","+product+","+pathAfterFusionApps+","+label+",";
        }
}
