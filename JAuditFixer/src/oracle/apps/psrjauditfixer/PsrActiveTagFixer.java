package oracle.apps.psrjauditfixer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class PsrActiveTagFixer {
        
    static final String classUsage = "Usage of this class: $JAVA_HOME/bin/java -classpath $CLASSPATH oracle.apps.psrjauditfixer.PsrActiveTagCountFixer $ADE_VIEW_ROOT <path to csv containing violations>";
    static final String csvFormat = "Expect each line is filepath followed by id then binding value, separated by comma. If no binding value, put \"null\"\n";
    static final String TAG_af_active = "af:active";
    static final String STRING_active = "active";
    static final String STRING_af = "af";
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
        PsrActiveTagFixer fixer = new PsrActiveTagFixer();
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
        BufferedWriter writer1 = new BufferedWriter(new FileWriter("Error.txt"));        
        try{ 
            while((line = reader.readLine()) != null) {                              
              String[] parts = line.split(",");
                
              //CSV file for this fixer has this format: <path to page to be fixed>,<v iolation id>,<binding value>
             if(parts.length < 3) {
                  writer1.write("Cannot fix due to some missing info in line:" + line + "\n");
                  writer1.write(csvFormat);
                  continue;
              }
                  
              if(parts[0]==null||parts[0].trim().isEmpty()||parts[1]==null||parts[1].trim().isEmpty()||parts[2]==null||parts[2].trim().isEmpty()){
                  writer1.write("Cannot fix due to missing info in line:" + line + "\n");
                  writer1.write(csvFormat);
                  continue;
              }
                                   
              if(!parts[2].trim().equals("not set")){
                  writer1.write("Do not fix due to binding attribute being set in line:" + line + "\n"); 
                  continue;                  
              }   
                           
            String filePath = viewRoot + "/" + parts[0].trim();
            String violationId = parts[1].trim();            
          
            if(!filePath.equals(prevFilePath)) {
              
                if(prevFilePath != null) { 
                    fileContentsAsString = xmlFixer.getFileContentsAsString(false);    //this will get the changed file contents  
                    diffHelper.applyFix(prevFilePath,fileContentsAsString); 
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
            
            
            NodeList components = doc.getElementsByTagName("*");           
            Node resultNode = XMLParserHelper.getNodesInListWithMatchingAttribute(components,"id",violationId);
              
            if(resultNode==null){
                writer1.write("Node not found with id:" + violationId + " ,in line" + line + "\n");  
                continue;
            }         

            Element elem = (Element)resultNode;              
            if (!elem.getTagName().startsWith(TAG_af_active)){
                writer1.write("Node does not need fix in line:" + line +"\n");
                continue;
            }
            String nodeName = resultNode.getNodeName();
            String nodeString = XMLParserHelper.getStringFromNode(resultNode); 
            
            //Modify nodeString here
            nodeString = nodeString.replaceFirst(STRING_active, "");
            nodeString = ModifyPrefixedNodeStringStartingLetterToLowerCase(nodeString, resultNode.getPrefix());
            
            // nodeString will contain namespace info. Will get rid of it later.
            // It is possible that end of node, say, /> or ><nodeName/> will be replaced as well. So check below
            String namespaceRegex = "xmlns:\\S+";
            String namespaceEndofNodeRegex1 = "xmlns:\\S+/>";
            String namespaceEndofNodeRegex2 = "xmlns:\\S+>";
            
            //Replace all namespace attributes at the end of node with />(<af:nodename  xmlns:\S+/>) or >(<af:nodename  xmlns:\S+> </nodename>)
            //or xmlns:\S+\> and xmln:\S+> will be replaced as "". We will miss the closing tags  
            nodeString = nodeString.replaceAll(namespaceEndofNodeRegex1, " />");   
            nodeString = nodeString.replaceAll(namespaceEndofNodeRegex2, " >");
            //Get rid of all namespace info
            nodeString = nodeString.replaceAll(namespaceRegex, ""); 
            
            xmlFixer.replaceNode(resultNode, nodeString);
          } //while
          writer1.flush();
                          
        if(doc != null) { //to take care of the last set of violations in the last file
           fileContentsAsString = xmlFixer.getFileContentsAsString(false);    //this will get the changed file contents  
           diffHelper.applyFix(prevFilePath,fileContentsAsString); 
        }
        
      }catch (Exception e) {
           e.printStackTrace();
      }finally{
           writer1.close();
      }
    }
   
    
    // Modify prefixed node name starting letter to lower case e.g. af:Output to af:output
    private static String ModifyPrefixedNodeStringStartingLetterToLowerCase(String NodeString, String Prefix){
        if(NodeString==null){
            return null;
        }
        //prefix can be null
        String regex = null;
        StringBuffer sb = new StringBuffer();
        if(Prefix != null){
            regex = "<(" + Prefix + ":)" + "([A-Z])(.*)";
        }else{
            regex = "<()" + "([A-Z])(.*)";        
        }
        Matcher m = Pattern.compile(regex).matcher(NodeString);
        while (m.find()) {
                m.appendReplacement(sb, "<" + m.group(1) + m.group(2).toLowerCase() + m.group(3));
        }
        return m.appendTail(sb).toString();
    } 
    
}
