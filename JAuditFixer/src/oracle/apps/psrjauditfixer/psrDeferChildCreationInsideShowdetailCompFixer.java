package oracle.apps.psrjauditfixer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import oracle.apps.psrjauditfixer.util.psrJauditFixerConstants;
import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class psrDeferChildCreationInsideShowdetailCompFixer{
   
    static final String classUsage = "Usage of this class: $JAVA_HOME/bin/java -classpath $CLASSPATH oracle.apps.psrjauditfixer.psrDeferChildCreationInsideShowdetailCompFixer $ADE_VIEW_ROOT <path to csv containing violations>";
    static final String csvFormat = "Expect each line is 'filepath,id,panelAccordionId,taskflowId,activation,pagedef'\n";
    static final String Tag_id = "id";
    static final String Tag_childCreation = "childCreation";
    static final String Tag_taskFlow = "taskFlow";
    static final String Tag_activation = "activation";
    static final String String_lazy = "lazy";
    static final String String_immediate = "immediate";
    static final String String_deferred = "deferred";
    static final String String_notset = "not set";
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
            psrDeferChildCreationInsideShowdetailCompFixer fixer = new psrDeferChildCreationInsideShowdetailCompFixer(); 
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
        Boolean bFileChanged = false;//use this flag to keep track of whether xmlFixer has been changed. if not, do not applyFix to avoid unexpected program exits
        Boolean bDefFileChanged = false;//similarly keep track of whether pagedef has been changed
        //use Hash to store panelAccordion information 
        HashMap <String,ArrayList<String>> panelAccordionVal = new HashMap<String, ArrayList<String>>();
        ArrayList<String> panelAccordionList = null;
        try{
            //outerwhileloop:
            while((line = reader.readLine()) != null) {                              
              String[] parts = line.split(",");
              bMissingInfo = false;
              if(parts.length < 6) {
                  writer1.write("Cannot fix due to some missing info in line:" + line + "\n");
                  writer1.write(csvFormat);
                  continue;
              }
              
             
              for (int i = 0; i < 6 ; i++){
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
            // CSV format: filepath,id,panelAccordionId,taskflowId,activation,pagedef
            String filePath = viewRoot + "/" + parts[0].trim();
            String violationId = parts[1].trim();
            String panelAccordionId = parts[2].trim();
            String taskflowId = parts[3].trim();
            //do not use activation value from input file at this time 
            //String activation = parts[4].trim();
            if(!parts[5].trim().equals(String_notset)){
                pageDefFilePath = viewRoot + "/" + parts[5].trim();
            }else{
                pageDefFilePath = String_notset;
            }
            
            // When filepath not changing, pageDefFilePath should not be changing neither and vice versa
            if(!filePath.equals(prevFilePath)) {
                //ONLY create new list for each filepath, or just add panelAccordionId to the current list
                panelAccordionList = new ArrayList<String>();
                panelAccordionVal.put(filePath, panelAccordionList);
               
                if(prevFilePath != null) {
                    //NOTICE: if the xmlFixer here is unchanged, the program will stop! this behavior is from diffHelper
                    if(xmlFixer != null && bFileChanged){
                        FileContentsAsString = xmlFixer.getFileContentsAsString(false);    //this will get the changed file contents  
                        diffHelper.applyFix(prevFilePath,FileContentsAsString);
                        bFileChanged = false;
                    }
                    if(defxmlFixer != null && bDefFileChanged){
                        PageDefFileContentsAsString = defxmlFixer.getFileContentsAsString(false);    //this will get the changed file contents  
                        diffHelper.applyFix(prevPageDefFilePath,PageDefFileContentsAsString); 
                        bDefFileChanged = false;
                    }
                }
                prevFilePath = filePath;
                prevPageDefFilePath = pageDefFilePath; 
                doc = XMLParserHelper.getXMLDocument(filePath);
                if(!pageDefFilePath.trim().equals(String_notset)){
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
            
            /*
             *  handle the case where:
             *  Line1: filepath, ... , not set pagedef
             *  Line2: same filepath as above, ... , pagedef
             *  after the first loop, defxmlFixer is null while in the second loop we need defxmlFixer
            */
            if(!pageDefFilePath.trim().equals(String_notset) && defxmlFixer == null){//Need to update defxmlFixer, defDoc and prevPageDefFilePath
                defDoc = XMLParserHelper.getXMLDocument(pageDefFilePath);
                if(defDoc == null) {
                    writer1.write("Failed to load the page def file in line:" + line + "\n");
                    writer1.write("Please make sure the pagedef filepath is correct." + "\n");
                    continue;
                }
                prevPageDefFilePath = pageDefFilePath;
                defxmlFixer = new XmlFixer(pageDefFilePath);
            }
            
            //if filePath==prevFilePath, doc and defDoc won't change, jump to next loop
            if(doc == null) {
                writer1.write("Failed to load the file in line:" + line + "\n");
                writer1.write("Please make sure the filepath is correct." + "\n");
                continue;
            }
            if(!pageDefFilePath.equals(String_notset) && defDoc == null) {
                writer1.write("Failed to load the page def file in line:" + line + "\n");
                writer1.write("Please make sure the pagedef filepath is correct." + "\n");
                continue;
            }
            
            //check whether it associates with a taskflow    
            //if defDoc is not null, pageDefFilePath is for sure not "not set"
            //check pagedef first
            if(defDoc != null && !pageDefFilePath.equals(String_notset)){   
                bDefFileChanged = fixPageDefDoc(defDoc, taskflowId, line, writer1, defxmlFixer);
            }else{
                //Fix jsff in this else block
                //check panelAccordionId
                //if panelAccordionId == N/A, do nothing; continue to normal steps
                if(!"N/A".equals(panelAccordionId)){           
                    //check the HashMap
                    //if panelAccordionId already seen for certain filePath, skip it 
                    if(!panelAccordionVal.get(filePath).contains(panelAccordionId)){
                        //store panelAccordionId in HashMap
                        panelAccordionList.add(panelAccordionId);
                        panelAccordionVal.put(filePath,panelAccordionList);
                        
                        //fix the panelAccordion
                        NodeList panelAccordions = doc.getElementsByTagName(psrJauditFixerConstants.Tag_panelAccordion);
                        Node resultNode = XMLParserHelper.getNodesInListWithMatchingAttribute(panelAccordions,Tag_id,panelAccordionId);
                        
                        if(resultNode==null){
                            writer1.write("panelAccordion Node not found with id:" + panelAccordionId + " ,in line" + line + "\n");  
                            continue;
                        }
                        
                        String childCreationVal = XMLParserHelper.getAttributeValue(resultNode, Tag_childCreation);
                                       
                        if (childCreationVal == null){
                        //add childCreation="lazy"
                                xmlFixer.addAttribute(resultNode, Tag_childCreation, String_lazy); 
                                bFileChanged = true;
                        }else if(String_immediate.equals(childCreationVal)){
                        //modify childCreation attr to deferred only when childCreation == immediate
                                xmlFixer.modifyAttributeIrrespectiveOldValue(resultNode, Tag_childCreation, String_lazy);
                                bFileChanged = true;
                        }
                    }       
                }else{
                //Normal case: no panelAccordionId                    
                    NodeList components = doc.getElementsByTagName("*");  
                    Node resultNode = XMLParserHelper.getNodesInListWithMatchingAttribute(components,Tag_id,violationId);
                  
                    if(resultNode==null){
                        writer1.write("Node not found with id:" + violationId + " ,in line" + line + "\n");  
                        continue;
                    }
                
                    String childCreationVal = XMLParserHelper.getAttributeValue(resultNode, Tag_childCreation);
                               
                    if (childCreationVal == null){
                        //add childCreation="lazy"
                        xmlFixer.addAttribute(resultNode, Tag_childCreation, String_lazy); 
                        bFileChanged = true;
                    }else if(String_immediate.equals(childCreationVal)){
                        //modify childCreation attr to deferred only when childCreation == immediate
                        xmlFixer.modifyAttributeIrrespectiveOldValue(resultNode, Tag_childCreation, String_lazy);
                        bFileChanged = true;
                    }
                }
                
            }
            
        } //while
          writer1.flush();
                          
        if(xmlFixer != null && bFileChanged) { //to take care of the last set of violations in the last file
            FileContentsAsString = xmlFixer.getFileContentsAsString(false);    
            diffHelper.applyFix(prevFilePath,FileContentsAsString); 
            
        }
        if(defxmlFixer != null && bDefFileChanged){    
            PageDefFileContentsAsString = defxmlFixer.getFileContentsAsString(false);    //this will get the changed file contents  
            diffHelper.applyFix(prevPageDefFilePath,PageDefFileContentsAsString); 
        }
        
      }catch (Exception e) {
           e.printStackTrace();
      }finally{
           writer1.close();
      }
    }
    
    private Boolean fixPageDefDoc(XMLDocument defDoc, String taskflowId, String line, BufferedWriter writer1, XmlFixer defxmlFixer) throws Exception{
            NodeList defComponents = defDoc.getElementsByTagName(Tag_taskFlow);
            Node defResultNode = XMLParserHelper.getNodesInListWithMatchingAttribute(defComponents,Tag_id,taskflowId);
        
            if(defResultNode==null){
                writer1.write("Node not found in page def file with taskflow id:" + taskflowId + " ,in line" + line + "\n");  
            }else{
            
                String activationVal = XMLParserHelper.getAttributeValue(defResultNode, Tag_activation);
        
                if(activationVal==null){
                    defxmlFixer.addAttribute(defResultNode, Tag_activation, String_deferred);
                    return true;
                }else if(String_immediate.equals(activationVal)){
                    defxmlFixer.modifyAttributeIrrespectiveOldValue(defResultNode, psrJauditFixerConstants.Tag_activation, psrJauditFixerConstants.String_deferred);
                    return true;
                }
               
            }
            return false;
    }

}