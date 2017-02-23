package oracle.apps.psrjauditfixer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.io.PrintWriter;

import java.util.ArrayList;

import oracle.apps.psrjauditfixer.util.psrJauditFixerConstants;
import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class psrViewObjectViewAttributeIsSelected {
    static final String classUsage = "Usage of this class: $JAVA_HOME/bin/java -classpath $CLASSPATH oracle.apps.psrjauditfixer.psrViewObjectViewAttributeIsSelected $ADE_VIEW_ROOT <path to csv containing violations>";
    static final String csvFormat = "Expect each line is 'filepath,id'\n";
    
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
        psrViewObjectViewAttributeIsSelected fixer = new psrViewObjectViewAttributeIsSelected(); 
        diffHelper.startDiffLog();
        fixer.makeFixes(viewRoot,diffHelper,reader);//fixer function
        reader.close();
        diffHelper.closeDiffLog(); 
    }
    
    private void makeFixes(String viewRoot,AdeDiffHelper diffHelper,BufferedReader reader) throws Exception{
        String line = reader.readLine();  //first line is the header - read & ignore
        String prevFilePath = null;
        XMLDocument doc = null;
        XmlFixer xmlFixer = null;
        String fileContentsAsString = null;
        PrintWriter writer1 = new PrintWriter(new BufferedWriter(new FileWriter("Error.txt")));        
        try{ 
            while((line = reader.readLine()) != null) {                              
              String[] parts = line.split(",");
              if(parts.length < 2) {
                  writer1.write("Cannot fix due to some missing info:" + line + "\n");
                  writer1.write(csvFormat);
                  continue;
              }
            
              if(parts[0]==null||parts[0].equals("")){
                  writer1.write("Cannot fix due to missing filepath in line:" + line + "\n");
                  writer1.write(csvFormat);
                  continue;
              }
            
              if(parts[1]==null||parts[1].equals("")){
                  writer1.write("Cannot fix due to missing violation viewAttribute id in line:" + line + "\n");
                  writer1.write(csvFormat);
                  continue;                  
              }
                                      
            String filePath = viewRoot + "/" + parts[0].trim();
            String violationId = parts[1].trim();
                               
            if(!filePath.equals(prevFilePath)) {
              
                if(prevFilePath != null) {
                    if(xmlFixer!=null){
                        fileContentsAsString = xmlFixer.getFileContentsAsString(false);    //this will get the changed file contents  
                        diffHelper.applyFix(prevFilePath,fileContentsAsString);
                    }else{
                        writer1.write("Cannot fix due to null xmlFixer for file in line:" + line + "\n");
                    }
                }
                prevFilePath = filePath;
                doc = XMLParserHelper.getXMLDocument(filePath);     
                if(doc == null) {
                    writer1.write("Failed to load the file in line:" + line + "\n");
                    writer1.write("Please make sure the filepath is correct." + "\n");
                    continue;
                }
                xmlFixer = new XmlFixer(filePath);
            }
            
            //if filePath==prevFilePath, doc won't change, jump to next loop
            if(doc == null) {
                writer1.write("Failed to load the file in line:" + line + "\n");
                writer1.write("Please make sure the filepath is correct." + "\n");
                continue;
            }
            
            if(xmlFixer==null){
                writer1.write("Cannot fix due to null xmlFixer for file in line:" + line + "\n");
                continue;
            }
            
            NodeList components = doc.getElementsByTagName("ViewAttribute");  
            Node resultNode = XMLParserHelper.getNodesInListWithMatchingAttribute(components,psrJauditFixerConstants.String_Name,violationId);
              
            if(resultNode==null){
                writer1.write("Node not found with name:" + violationId + " ,in line" + line + "\n");  
                continue;
            }    
            
            String isSelectedVal = XMLParserHelper.getAttributeValue(resultNode, psrJauditFixerConstants.Tag_IsSelected);
                try{
                    if(isSelectedVal==null){ //IsSelected attr is not specified in this ViewAttribute
                        xmlFixer.addAttribute(resultNode, psrJauditFixerConstants.Tag_IsSelected, psrJauditFixerConstants.String_false);    
                    }else if(psrJauditFixerConstants.String_true.equalsIgnoreCase(isSelectedVal)){
                        xmlFixer.modifyAttributeIrrespectiveOldValue(resultNode, psrJauditFixerConstants.Tag_IsSelected, psrJauditFixerConstants.String_false);
                    }
                } catch (Throwable th){
                    //set xmlFixer to null to avoid diffHelper.applyFix call, which will terminate program if xmlFixer remains unchanged
                    //by setting xmlFxier to null, we will just skip all violations in that file.
                    //other files will throw NPE exception, print to Error.txt
                    
                    //NOTE: Cannot fix this file due to a potential error in API
                    //Failed to get correct xmlNode line number due to probably 100+ commented out nodes in this file
                    //fusionapps/hcm/components/hcmAnalytics/assignment/publicModelAnalytics/src/oracle/apps/hcm/assignment/publicView/analytics/AsgEFFSearchVO.xml
                    xmlFixer = null;
                    writer1.write("Cannot fix due to null xmlFixer for file in line:" + line + "\n");
                    th.printStackTrace(writer1);
                }
            
          } //while
          writer1.flush();
                          
        if(xmlFixer != null) { //to take care of the last set of violations in the last file
              fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
              diffHelper.applyFix(prevFilePath,fileContentsAsString);
        }
        
        }catch (Exception e) {
           e.printStackTrace(writer1);
        }finally{
           writer1.close();
        }
    }
}
