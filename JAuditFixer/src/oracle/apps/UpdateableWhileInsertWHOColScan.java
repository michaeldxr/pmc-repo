package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashSet;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;
import oracle.apps.sqlanalyzer.ViewCriteriaHelper;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UpdateableWhileInsertWHOColScan extends JoesBaseClass{
  static BufferedWriter writer;
  private static String mode="view"; //LRG/view/premerge mode
  static String sCrawlDir = "";
  HashSet<String> exemptions = new HashSet<String>();
  public static Boolean bDoADE = false;
  static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
  static int violations = 0;

  public UpdateableWhileInsertWHOColScan() {
      super(JoesBaseClass.CRAWL_TYPE.EO_XML);
  }
  
  public static void main(String[] args) throws Exception{
      
     java.util.Date start = new java.util.Date();
      UpdateableWhileInsertWHOColScan x = new UpdateableWhileInsertWHOColScan();
      //x.generateExemptions();
      
      mode = System.getProperty("mode");
      if(mode == null)
          mode = "view";
      //mode = "LRG";
      
      writer = new BufferedWriter(new FileWriter("eo_historyCol_updateable.csv"));
      writer.write("Family,Module,Product,Filename,Series, Label,WHO Attribute, " +
        "Updateable, Description\n");
      
      if(mode.equals("view")) {
            
         if (args.length < 1 || args[0] == null || args[0].equals("")) {
           System.out.println("USAGE: eoHistoryColScan.sh <crawlDirectory>");
           System.exit(1);
         }
         sCrawlDir = args[0];
         
        if(args.length > 1)
           bDoADE = new Boolean(args[1].trim());         
            
         System.out.println("Analyzing files in directory for EO History Columns: " + sCrawlDir);
      
         x.crawlDirectory(sCrawlDir, false);
         writer.close();
         System.out.println("Done. Please review eo_historyCol_updateable.csv file for results of the scan.");
         
         if(bDoADE.booleanValue()){
            diffHelper.startDiffLog();     
            x.makeFixes(sCrawlDir);
            diffHelper.closeDiffLog();  
         }
      }
      else if(mode.equals("LRG")){
          //String series = "FUSIONAPPS_PT.V2MIB_LINUX.X64";
          String series = args[0]; //only needs series name as the argument    
          String label = LRGUtil.getLatestLabel(series);
          System.out.println("Running UpdateableWhileInsertWHOColScan Scan on series: " + series + ".......");
          System.out.println("Latest label is: " + label);
          
          ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
          String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
          String viewRoot = serverTop + "/"+series+".rdd/"+label + "/";
        
          for (int j = 0; j < families.size(); j++) {
              String family = families.get(j);
              String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, "UpdateableWhileInsertWHOColScan");
              System.out.println("Crawling dir '" + viewRoot + sCrawlDir + "' for family: " + family);
              x.crawlDirectoryNew(viewRoot,sCrawlDir,false);      
              System.out.println("Done crawling for family "+family+".");
          }        
          writer.close();
          x.dumpResultsToAB(series);
          
      } else if(mode.equals("premerge")) {
          //premerge integration
      }
      FamilyModuleHelper.printScanDuration(start,"UpdateableWhileInsertWHOColScan");
      System.out.println("Number of violations found: " + violations);
   
  }
  
  public void makeFixes(String crawlDir) throws Exception {
    
    System.out.println("Fixing Updateable While Insert EO History Columns...");
      
    BufferedReader fileReader = new BufferedReader(new FileReader("eo_historyCol_updateable.csv"));
    String line = fileReader.readLine(); //first line is the header - read & ignore
    String prevFilePath = null;
    XMLDocument doc = null;
    XmlFixer xmlFixer = null;
    String[] fileContents = null;
    String fileContentsAsString = null;
    
    String viewRoot = ViewCriteriaHelper.getViewRoot(crawlDir);
    
    while((line = fileReader.readLine()) != null) {
        
      String[] parts = line.split(",");
      if(parts.length <9)
        continue;
      String filePath = viewRoot + "/" + parts[3].trim();
        
      if(!filePath.equals(prevFilePath)) {
        
          if(prevFilePath != null) {
            fileContents = xmlFixer.getFileContents(); // test
            fileContentsAsString = xmlFixer.getFileContentsAsString(fileContents, false);        
            applyFix(prevFilePath,fileContentsAsString);
          }
        
        doc = XMLParserHelper.getXMLDocument(filePath);        
        xmlFixer = new XmlFixer(filePath);
        fileContents = xmlFixer.convertFileToStringArray();
        xmlFixer.setFileContents(fileContents); //test
      }
      prevFilePath = filePath;
        
      Node n = findElement(doc,parts[6].trim());
      String updateableColValue = parts[8].trim();
    //add attribute value if not present
     xmlFixer.modifyAttribute(n, "IsUpdateable", null, "while_insert", true);
      
    } // end of csv file
    
    //apply fix to the last file 
    if(xmlFixer != null) {
      fileContentsAsString = xmlFixer.getFileContentsAsString(fileContents, false);        
      applyFix(prevFilePath,fileContentsAsString);
    }
    
  }
  
  private Node findElement(XMLDocument doc, String name){
    NodeList list = doc.getElementsByTagName("Attribute");
    for(int i = 0; i< list.getLength(); i++) {
        Node n = list.item(i);
        if(XMLParserHelper.getAttributeValue(n, "Name").equals(name)) 
          return n;
    }
    return null;
  }
  
  public void applyFix(String absPath,
                      String newFileContents) throws Exception {
     File file = new File(absPath);
     if (!file.exists()) {
         throw new Exception("while making fix, file not found: " +
                             absPath);
     }
     diffHelper.checkoutAndDelete(file);

     FileWriter fw = new FileWriter(file.getAbsolutePath());
     BufferedWriter bw = new BufferedWriter(fw);
     bw.write(newFileContents);
     if (bw != null)
         bw.close();
     if (fw != null)
         fw.close();
     diffHelper.checkinAndDiff(file);
  }
  
  private void dumpResultsToAB(String series) throws Exception {
      
      System.out.println("Dumping results to Angrybirds DB...");
      //BufferedWriter w = new BufferedWriter(new FileWriter("filtered.csv"));
      //w.write("Family,Module,Product,Filename,Series, Label,IssueType, Description\n");
      Connection con =
        DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
      PreparedStatement insert =
        con.prepareStatement("INSERT INTO codescan_results_p (SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME," +
          "ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)");     
      
      
    Statement delete = con.createStatement();
    delete.executeUpdate("DELETE from codescan_results_p where issuetype='UpdateableWhileInsertWHOColumns' and series = '" + series + "'");
    delete.close();
  
    BufferedReader reader = new BufferedReader(new FileReader("eo_historyCol_updateable.csv"));
    String line = reader.readLine();
    
    int count = 0;
    while((line = reader.readLine()) != null) {
        
        String[] parts = line.split(",");
        if(parts.length < 9)
            continue;
      count++;
      insert.setString(1, parts[4].trim());
      insert.setString(2, parts[5].trim());
      insert.setString(3, parts[0].trim());
      insert.setString(4, parts[1].trim());
      insert.setString(5, parts[2].trim());
      insert.setString(6, parts[3].trim());
      insert.setString(7, "UpdateableWhileInsertWHOColumns");
      insert.setString(8, parts[6].trim() + ": " + parts[7].trim());
      insert.setString(9,parts[8].trim());
      insert.addBatch();
      if(count%1000==0)
        insert.executeBatch();
        
    }
    reader.close();
    insert.executeBatch();
    insert.close();
    con.commit();
      con.close();  
      System.out.println("Done");
  }
  
  protected void processFile(File fName, boolean bDoADE) {
      
      String f = fName.getAbsolutePath();
      if(f.contains("/flex/") || f.contains("/publicFlex/") || f.contains("/viewLink/") || f.contains("/noship"))
          return;
      
      String absPathLowerCase = f.toLowerCase();
      if (absPathLowerCase.contains("test.") || absPathLowerCase.contains("/test/") || 
          absPathLowerCase.contains("/modeltest") || absPathLowerCase.contains("/uitest/")
          || absPathLowerCase.contains("/testui") || absPathLowerCase.contains("servicetest") 
          || absPathLowerCase.contains("/publicservicetest") || absPathLowerCase.contains("/publicuitest")
          || absPathLowerCase.contains("/publicmodeltest") || absPathLowerCase.contains("/structuretest"))
          return;
      
      try{
          
          XMLDocument doc = XMLParserHelper.getXMLDocument(f);

          if(doc == null) {
            System.out.println("Could not parse XMLDocument..Skipping");
            return;
          }
          
            NodeList entityObjects = doc.getElementsByTagName("Entity");
            if(entityObjects == null || entityObjects.getLength() != 1) 
              return;
           Node entity = entityObjects.item(0);
           
           String updatable = XMLParserHelper.getAttributeValue(entity, "IsUpdatable");
          if(updatable != null && updatable.equals("false"))
            return;

          ArrayList<Node> attributes = XMLParserHelper.getChildNodesWithName(entity, "Attribute");
          if(attributes == null || attributes.size() ==0 )
              return;
        
        for(int i = 0; i < attributes.size(); i++) {
          
          Node attribute = attributes.get(i);
          String name = XMLParserHelper.getAttributeValue(attribute, "Name");
          String columnName = XMLParserHelper.getAttributeValue(attribute, "ColumnName");
          String updateable = XMLParserHelper.getAttributeValue(attribute, "IsUpdateable");
          
          if(columnName == null)
            continue;
            
          if((columnName.equals("CREATION_DATE") || columnName.equals("CREATED_BY")) && updateable == null){
            writer.write(FamilyModuleHelper.getFileNameInfo1(f)+ name + "," + updateable + "," 
                          + "CREATION_DATE and CREATED_BY should be updateable only while insert\n");
            violations++;
          }
    
        }
          
      } catch(Exception e){
          e.printStackTrace();
      }
  }

  protected String getSummaryReport() {
      return null;
  }

  protected String getSummaryReportSubject() {
      return null;
  }

}
