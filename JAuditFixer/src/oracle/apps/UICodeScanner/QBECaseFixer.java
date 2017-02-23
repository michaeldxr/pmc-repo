package oracle.apps.UICodeScanner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class QBECaseFixer extends JoesBaseClass {
    static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
    static Writer outputFileWriter; 
    static boolean bDebug = false;
    int m_page_reviewCount = 0;
    int m_qbe_violations = 0;
    float time_taken = 0;
    String m_sSummaryReport = "";
    static String viewRoot = "";
    
    public QBECaseFixer() {
      super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF);
    }
     
    public static void main(String[] args) throws Exception {
        QBECaseFixer qbeFixer = new QBECaseFixer();

        if (args.length <1 || args[0] == null || args[0].equals("")) {
            System.out.println("Usage: fixBloatedEnums.sh <path to your code in ade> <true for fixer mode/false for scanning>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        //String sCrawlDir = "/ade_autofs/ade_fusion_linux/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/fusionapps/fin/components";
        viewRoot = FamilyModuleHelper.getViewRootFromPath(sCrawlDir);
        
        boolean makeFix = true;
        if(args.length > 1)
            makeFix = new Boolean(args[1].trim());
       // makeFix = true;
      try {         
          
          outputFileWriter = new BufferedWriter(new FileWriter("qbe_case_sensitive.csv") );
          outputFileWriter.write("Family,Module,Product,FileName,Series,Label,Node Id,Description\n");          
          System.out.println("Finding case-sensitive QBE searches in crawl directory: " + sCrawlDir);
          qbeFixer.crawlDirectory(sCrawlDir,false);
          outputFileWriter.close();
          
          if(makeFix) {
              System.out.println("Fixing case sensitive QBE...");
              qbeFixer.startDiffLog();     
              qbeFixer.fixQbeCases();
              qbeFixer.closeDiffLog();    
              System.out.println("Done!");
             
          }            
        
      } catch (Exception e) {
          e.printStackTrace();
          qbeFixer.logException(sCrawlDir, e);
      }
    }
    
    public void fixQbeCases() throws Exception {
      
      System.out.println("Fixing Case sensitive QBE searches...");
        
      BufferedReader fileReader = new BufferedReader(new FileReader("qbe_case_sensitive.csv"));
      String line = fileReader.readLine(); //first line is the header - read & ignore
      String prevFilePath = null;
      XMLDocument doc = null;
      XmlFixer xmlFixer = null;
      String fileContentsAsString = null;
      
      while((line = fileReader.readLine()) != null) {
        
        if(bDebug)
          System.out.println("line from csv File:" + line);
          
        String[] parts = line.split(",");
          if(parts.length < 8)
              continue;
        String filePath = viewRoot + parts[3].trim();
        
        if(bDebug)
          System.out.println("filePath:" + filePath);
          
        if(!filePath.equals(prevFilePath)) {
          
            if(prevFilePath != null) {
              fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
              doADE(true, new File(prevFilePath),fileContentsAsString);
            }
          
          doc = XMLParserHelper.getXMLDocument(filePath);        
          xmlFixer = new XmlFixer(filePath);
           
        }
        prevFilePath = filePath;
          
        Node n = findElement(doc,parts[6].trim());
        //Node n = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(adfc.item(0), "column", "id", parts[6].trim());
        String attrOldValue = XMLParserHelper.getAttributeValue(n,"filterFeatures");
        String attrNewValue = "caseInsensitive";
        if(attrOldValue != null)
            attrNewValue = (attrOldValue + " " + attrNewValue).trim();

        xmlFixer.modifyAttribute(n, "filterFeatures", attrOldValue, attrNewValue, true);    
        
      } // end of csv file
      
      //apply fix to the last file 
      if(xmlFixer != null) {
        fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
        doADE(true, new File(prevFilePath),fileContentsAsString);
      }
      
    }
    
    private Node findElement(XMLDocument doc, String id){
      NodeList list = doc.getElementsByTagName("column");
      for(int i = 0; i< list.getLength(); i++) {
          Node n = list.item(i);
          if(XMLParserHelper.getAttributeValue(n, "id").equals(id)) return n;
      }
      return null;
    }
    

     
     protected String getSummaryReport() {
    
       m_sSummaryReport += "For this run I reviewed a total of " +m_page_reviewCount +" pages.\n";
       m_sSummaryReport += "Total # of button type mismatches found:" + m_qbe_violations + "\n";
       
       return m_sSummaryReport;
     }
     
     protected String getSummaryReportSubject() {
       return "QBE Case-sensitivity Scan";
     }
     
     protected void processFile(File f, boolean bDoADE) 
     {
       m_page_reviewCount++;

         try {          

             XMLDocument doc = XMLParserHelper.getXMLDocument(f.getAbsolutePath());
             NodeList adfc = doc.getElementsByTagName("jsp:root");
             if (adfc == null) 
             {
                 if(bDebug) System.out.println("REVIEW:  Not a page file ---> " + f);
                 return;
             }
            findCaseSensitiveQbeCases(doc, f);          
        } 
        catch (Exception e) {
            if(bDebug)
            e.printStackTrace();
            logException(f.getName(), e);
            System.out.println("ERROR:  Error while processing file.  " +
                "review carefully if the script updated it. " +f);
            return;
        }
     }

    private void findCaseSensitiveQbeCases(XMLDocument doc, File f) throws IOException {
      NodeList columns = doc.getElementsByTagName("column");
     
      for(int i = 0; i< columns.getLength(); i++) {
          
          Node n = columns.item(i);
          
          String filterable = XMLParserHelper.getAttributeValue(n, "filterable");
          if(filterable == null || filterable.equalsIgnoreCase("false"))
              continue;
          
          NodeList numbers = ((Element)n).getElementsByTagName("convertNumber");
          NodeList dates = ((Element)n).getElementsByTagName("convertDateTime");
         
          if(numbers != null && numbers.getLength() > 0)
              continue;
          if(dates != null && dates.getLength() > 0)
              continue;
          
          String filterFeatures = XMLParserHelper.getAttributeValue(n, "filterFeatures");
          
          if(filterFeatures != null && filterFeatures.contains("caseInsensitive"))
              continue;
                
          outputFileWriter.write(FamilyModuleHelper.getFileNameInfo1(f.getAbsolutePath()) + 
                                XMLParserHelper.getAttributeValue(n, "id") + "," + 
                                "QBE search should be case-insensitive\n");
              
      }
    }

   
}
