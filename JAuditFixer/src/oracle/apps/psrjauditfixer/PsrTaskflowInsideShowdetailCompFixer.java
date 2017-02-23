package oracle.apps.psrjauditfixer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import java.util.ArrayList;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class PsrTaskflowInsideShowdetailCompFixer {
   
    static final String classUsage = "Usage of this class: $JAVA_HOME/bin/java -classpath $CLASSPATH oracle.apps.psrjauditfixer.PsrTaskflowInsideShowdetailCompFixer $ADE_VIEW_ROOT <path to csv containing violations>";
    static final String csvFormat = "Expect each line is filepath,id,rendered property,taskflow,activation,active,pagedef\n";
    static final String Tag_taskFlow = "taskFlow";
    static final String Tag_activation = "activation";
    static final String Tag_active = "active";
    static final String Tag_id = "id";
    static final String String_conditional = "conditional";
    static final String String_never = "never";
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
            PsrTaskflowInsideShowdetailCompFixer fixer = new PsrTaskflowInsideShowdetailCompFixer(); 
            diffHelper.startDiffLog();
            fixer.makeFixes(viewRoot,diffHelper,reader);//fixer function
            reader.close();
            diffHelper.closeDiffLog(); 
        }
         
    
  
    
    private void makeFixes(String viewRoot,AdeDiffHelper diffHelper,BufferedReader reader) throws Exception{
        
        String line = reader.readLine();  //first line is the header - read & ignore 
        String prevPageDefFilePath = null;
        XMLDocument defDoc = null;
        XmlFixer xmlFixer = null;
        String pageDefFileContentsAsString = null;
        BufferedWriter writer1 = new BufferedWriter(new FileWriter("Error.txt")); 
        Boolean bMissingInfo = false;
        try{
            //outerwhileloop:
            while((line = reader.readLine()) != null) {                              
              String[] parts = line.split(",");
              bMissingInfo = false;
              if(parts.length < 7) {
                  writer1.write("Cannot fix due to some missing info:" + line + "\n");
                  writer1.write(csvFormat);
                  continue;
              }
              
             
              for (int i = 0; i < 7 ; i++){
                    if(parts[i]==null||parts[i].trim().isEmpty()){
                        writer1.write("Cannot fix due to missing info in line:" + line + "\n");
                        writer1.write(csvFormat);
                        bMissingInfo = true;
                        break;
                       // continue outerwhileloop;                        
                    }
              }
                if(bMissingInfo == true){
                    continue;
                }
            // CSV format: filepath,id,rendered property,taskflow,activation,active,pagedef   
            String filePath = viewRoot + "/" + parts[0].trim();
            String violationId = parts[1].trim();
            String renderedProperty = parts[2].trim();
            String taskflowId = parts[3].trim();             
            String activation = parts[4].trim();
            String active = parts[5].trim();
            String pageDefFilepath = viewRoot + "/" + parts[6].trim();

            if(!pageDefFilepath.equals(prevPageDefFilePath)) {
              
                if(prevPageDefFilePath != null) {
                  pageDefFileContentsAsString = xmlFixer.getFileContentsAsString(false);    //this will get the changed file contents  
                  diffHelper.applyFix(prevPageDefFilePath,pageDefFileContentsAsString); 
                }
                prevPageDefFilePath = pageDefFilepath;
                defDoc = XMLParserHelper.getXMLDocument(pageDefFilepath);  
                if(defDoc == null) {
                    writer1.write("Failed to load the page definition file in line:" + line + "\n");
                    writer1.write("Please make sure the page definition filepath is correct." + pageDefFilepath + "\n");
                    continue;
                }
                xmlFixer = new XmlFixer(pageDefFilepath);
            }
            
            //if filePath==prevFilePath, defDoc won't change, jump to next loop
            if(defDoc == null) {
                 writer1.write("Failed to load the page definition file in line:" + line + "\n");
                 writer1.write("Please make sure the page definition filepath is correct." + pageDefFilepath + "\n");
                 continue;
            }
                
            
            //Get all taskFlow components in page definition file          
            NodeList taskflowComp = defDoc.getElementsByTagName(Tag_taskFlow);
            
            if(taskflowComp == null || taskflowComp.getLength() < 1){
                writer1.write("No taskFlow Nodes found in page definition file:" + pageDefFilepath +"\n");
                continue; 
            }
            
            Node resultTaskflowComp = XMLParserHelper.getNodesInListWithMatchingAttribute(taskflowComp,Tag_id,taskflowId);
            
            if(resultTaskflowComp == null){
                writer1.write("taskFlow Node not found with taskflowId:" + taskflowId + " ,in page definition file" + pageDefFilepath + "\n");  
                continue;                        
            }
            
            String tfActivation = XMLParserHelper.getAttributeValue(resultTaskflowComp, Tag_activation);
            String tfActive = XMLParserHelper.getAttributeValue(resultTaskflowComp, Tag_active);

            // if activation property is conditional or never, no matter what acive property is(except null), do not fix
            
            if(String_conditional.equals(tfActivation) && (tfActive != null)){
                writer1.write("Node does not need fix in line:" + line +"\n");
                continue;                    
            }
            if(String_never.equals(tfActivation)){
                 writer1.write("Node does not need fix in line:" + line +"\n");
                 continue;                    
            }
            
            
            //if single&double quote both exists in rendered property, skip this violation
            //in current scan, only 1 violation is skipped
            if(renderedProperty.contains("\"") &&  renderedProperty.contains("\'")){
                writer1.write("Violation skipped due to both single&double quotes were contained in rendered property in line:" + line + " Please fix this violation manually\n");
                continue;
            }
                        
            // check when activation is not conditional
            if(tfActivation == null){
                xmlFixer.addAttribute(resultTaskflowComp, Tag_activation, String_conditional);
                if(tfActive == null){
                    if(renderedProperty.contains("\"")){
                        xmlFixer.addAttribute(resultTaskflowComp, Tag_active, renderedProperty, "\'");
                    }else{
                        xmlFixer.addAttribute(resultTaskflowComp, Tag_active, renderedProperty);
                    }
                }                
            }else{                
                //if activation is conditional, leave active property alone(if active ==null, assign rendered property); 
                //if activation is not conditional, and active is null, change active to corresponding rendered property value in jsff
                //if active is null, assign rendered property to it
                if(tfActive == null){
                    if(renderedProperty.contains("\"")){
                        xmlFixer.addAttribute(resultTaskflowComp, Tag_active, renderedProperty, "\'");
                    }else{
                        xmlFixer.addAttribute(resultTaskflowComp, Tag_active, renderedProperty);
                    }
                }
                
                //Modify must be after add, I guess it is a bug in modifyAttributeIrrespectiveOldValue. 
                //It seems XMLParserHelper did not handle the new line correctly if modify node value then add new node
                xmlFixer.modifyAttributeIrrespectiveOldValue(resultTaskflowComp, Tag_activation, String_conditional);                
            }                
          } //while
          writer1.flush();
                          
        if(xmlFixer != null) { //to take care of the last set of violations in the last file
            pageDefFileContentsAsString = xmlFixer.getFileContentsAsString(false);    
            diffHelper.applyFix(prevPageDefFilePath,pageDefFileContentsAsString); 
        }
        
      }catch (Exception e) {
           e.printStackTrace();
      }finally{
           writer1.close();
      }
    }

}