package oracle.apps.psrjauditfixer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import oracle.apps.psrjauditfixer.util.psrJauditFixerConstants;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class psrShowDetailFrameFixer {
    static final String classUsage = "Usage of this class: $JAVA_HOME/bin/java -classpath $CLASSPATH oracle.apps.psrjauditfixer.psrShowDetailFrameFixer $ADE_VIEW_ROOT <path to csv containing violations>";
    static final String csvFormat = "Expect each line is 'filepath,id,taskflowId,activation,pagedef'\n";
 
    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception{
        
            
            
            if(args[0]==null){
                System.out.println("Environment Variable $ADE_VIEW_ROOT is not specified.");
                System.out.println("Please make sure you are using a valid view.");
                System.out.println(classUsage);
                System.exit(1);
            }
        
            String viewRoot = args[0].trim();
            
            if(args.length<2 || args[1]==null || args[1].equals("")) {
                System.out.println("Path to the csv file containing content violations must be specified.");
                System.out.println(classUsage);
                System.exit(1);
            }
            
            if(!args[1].trim().endsWith(".csv")) {
                System.out.println("Wrong input file format. Please provide a csv file as an argument to the script.");
                System.out.println(classUsage);
                System.exit(1);
            }
                 
            
            File f = new File(args[1].trim());
            if(!f.exists()) {
                System.out.println("csv file does not exist.");
                System.out.println(classUsage);
                System.exit(1);
            }
           
            
            AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
            BufferedReader reader=new BufferedReader(new FileReader(f));
            psrShowDetailFrameFixer fixer = new psrShowDetailFrameFixer(); 
            diffHelper.startDiffLog();
            fixer.makeFixes(viewRoot,diffHelper,reader);//fixer function
            reader.close();
            diffHelper.closeDiffLog(); 
        }
         
    
    
    
    private void makeFixes(String viewRoot,AdeDiffHelper diffHelper,BufferedReader reader) throws Exception{
        
        String line = reader.readLine();  //first line is the header - read & ignore 
        String prevFilePath = null;
        String prevPageDefFilePath = null;
        XMLDocument doc = null;
        XMLDocument defDoc = null;
        XmlFixer xmlFixer = null;
        XmlFixer defxmlFixer = null;
        String FileContentsAsString = null;
        String PageDefFileContentsAsString = null;
        String pageDefFilePath = null;
        BufferedWriter writer1 = new BufferedWriter(new FileWriter("Error.txt"));    
        Boolean bMissingInfo = false;
        try{
            //outerwhileloop:
            while((line = reader.readLine()) != null) {                              
              String[] parts = line.split(",");
              bMissingInfo = false;
              if(parts.length < 5) {
                  writer1.write("Cannot fix due to some missing info in line:" + line + "\n");
                  writer1.write(csvFormat);
                  continue;
              }
              
             
              for (int i = 0; i < 5 ; i++){
                    if(parts[i]==null||parts[i].trim().isEmpty()){
                        writer1.write("Cannot fix due to missing info in line:" + line + "\n");
                        writer1.write(csvFormat);
                        bMissingInfo = true;
                        //continue outerwhileloop;   
                        break;
                    }
              }
              
                if(bMissingInfo == true){
                    continue;
                }
            // CSV format: filepath,id,taskflowId,activation,pagedef
            String filePath = viewRoot + "/" + parts[0].trim();
            String violationId = parts[1].trim();
            String taskflowId = parts[2].trim();
            String activation = parts[3].trim();
            if(!parts[4].trim().equals(psrJauditFixerConstants.String_notset)){
                pageDefFilePath = viewRoot + "/" + parts[4].trim();
            }else{
                pageDefFilePath = psrJauditFixerConstants.String_notset;
            }
           
            
            // When filepath not changing, pageDefFilePath should not be changing neither and vice versa
            if(!filePath.equals(prevFilePath)) {
              
                if(prevFilePath != null) {
                  FileContentsAsString = xmlFixer.getFileContentsAsString(false);    //this will get the changed file contents  
                  diffHelper.applyFix(prevFilePath,FileContentsAsString); 
                  
                    if(defxmlFixer != null){
                        PageDefFileContentsAsString = defxmlFixer.getFileContentsAsString(false);    //this will get the changed file contents  
                        diffHelper.applyFix(prevPageDefFilePath,PageDefFileContentsAsString); 
                    }
                }
                prevFilePath = filePath;
                prevPageDefFilePath = pageDefFilePath; 
                doc = XMLParserHelper.getXMLDocument(filePath);
                if(!pageDefFilePath.equals(psrJauditFixerConstants.String_notset)){
                    defDoc = XMLParserHelper.getXMLDocument(pageDefFilePath);
                    if(defDoc == null) {
                        writer1.write("Failed to load the page def file in line:" + line + "\n");
                        writer1.write("Please make sure the pagedef filepath is correct." + "\n");
                        continue;
                    }
                }else{
                    defDoc = null;
                    //def file path is not set, not changing def file, just checking doc
                }
                if(doc == null) {
                    writer1.write("Failed to load the file in line:" + line + "\n");
                    writer1.write("Please make sure the filepath is correct." + "\n");
                    continue;
                }
                
                xmlFixer = new XmlFixer(filePath);
                //this check here is needed because if defDoc or doc is null, I should not assign anything to defxmlFixer/xmlFixer, or it would affect next iteration
                if(defDoc == null){
                    defxmlFixer = null;
                }else{
                    defxmlFixer = new XmlFixer(pageDefFilePath);
                }
            }
            
            //if filePath==prevFilePath, doc and defDoc won't change, jump to next loop
            if(doc == null) {
                writer1.write("Failed to load the file in line:" + line + "\n");
                writer1.write("Please make sure the filepath is correct." + "\n");
                continue;
            }
            if(!pageDefFilePath.equals(psrJauditFixerConstants.String_notset) && defDoc == null) {
                writer1.write("Failed to load the page def file in line:" + line + "\n");
                writer1.write("Please make sure the pagedef filepath is correct." + "\n");
                continue;
            }
      
            NodeList components = doc.getElementsByTagName("*");  
            Node resultNode = XMLParserHelper.getNodesInListWithMatchingAttribute(components,psrJauditFixerConstants.Tag_id,violationId);
              
            if(resultNode==null){
                writer1.write("Node not found with id:" + violationId + " ,in line" + line + "\n");  
                continue;
            }
            
            String childCreationVal = XMLParserHelper.getAttributeValue(resultNode, psrJauditFixerConstants.Tag_childCreation);
                           
            if (childCreationVal == null){
            //add childCreation="lazy"
                    xmlFixer.addAttribute(resultNode, psrJauditFixerConstants.Tag_childCreation, psrJauditFixerConstants.String_lazy); 
            }else if(psrJauditFixerConstants.String_immediate.equals(childCreationVal)){
            //modify childCreation attr to lazy only when childCreation == immediate
                    xmlFixer.modifyAttributeIrrespectiveOldValue(resultNode, psrJauditFixerConstants.Tag_childCreation, psrJauditFixerConstants.String_lazy);
            }
            
            //check whether it associates with a taskflow    
            // if defDoc is not null, pageDefFilePath is for sure not "not set"
            if(defDoc != null){
                
                    NodeList defComponents = defDoc.getElementsByTagName(psrJauditFixerConstants.Tag_taskFlow);
                    Node defResultNode = XMLParserHelper.getNodesInListWithMatchingAttribute(defComponents,psrJauditFixerConstants.Tag_id,taskflowId);
            
                    if(defResultNode==null){
                        writer1.write("Node not found in page def file with taskflow id:" + taskflowId + " ,in line" + line + "\n");  
                        continue;
                    }
               
                    if(psrJauditFixerConstants.String_notset.equals(activation)){
                        defxmlFixer.addAttribute(defResultNode, psrJauditFixerConstants.Tag_activation, psrJauditFixerConstants.String_deferred);
                    }else if(psrJauditFixerConstants.String_immediate.equals(activation)){
                        if(!defxmlFixer.modifyAttributeIrrespectiveOldValue(defResultNode, psrJauditFixerConstants.Tag_activation, psrJauditFixerConstants.String_deferred)){
                            defxmlFixer.addAttribute(defResultNode, psrJauditFixerConstants.Tag_activation, psrJauditFixerConstants.String_deferred); 
                        }
                    }
            }
        } //while
          writer1.flush();
                          
        if(xmlFixer != null) { //to take care of the last set of violations in the last file
            FileContentsAsString = xmlFixer.getFileContentsAsString(false);    
            diffHelper.applyFix(prevFilePath,FileContentsAsString); 
            
        }
        if(defxmlFixer != null){    
            PageDefFileContentsAsString = defxmlFixer.getFileContentsAsString(false);    //this will get the changed file contents  
            diffHelper.applyFix(prevPageDefFilePath,PageDefFileContentsAsString); 
        }
        
      }catch (Exception e) {
           e.printStackTrace();
      }finally{
           writer1.close();
      }
    }
}
