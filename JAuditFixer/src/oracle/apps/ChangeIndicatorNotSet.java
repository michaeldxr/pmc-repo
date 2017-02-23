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

public class ChangeIndicatorNotSet extends JoesBaseClass{
  static BufferedWriter writer;
  private static String mode="view"; //LRG/view/premerge mode
  static String sCrawlDir = "";
  HashSet<String> exemptions = new HashSet<String>();
  public static Boolean bDoADE = false;
  static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
  static int violations = 0;
  
  public ChangeIndicatorNotSet() {
      super(JoesBaseClass.CRAWL_TYPE.EO_XML);
  }
  
  public static void main(String[] args) throws Exception{
      
     java.util.Date start = new java.util.Date();
      ChangeIndicatorNotSet x = new ChangeIndicatorNotSet();
      x.generateExemptions();
      
      mode = System.getProperty("mode");
      if(mode == null)
          mode = "view";
      //mode = "LRG";
      
      writer = new BufferedWriter(new FileWriter("changeIndicatorNotSet_scan.csv"));
      writer.write("Family,Module,Product,Filename,Series, Label,AttributeName, ChangeIndicator, Description\n");
      
      if(mode.equals("view")) {
            
         sCrawlDir = args[0];
         
        if(args.length > 1)
           bDoADE = new Boolean(args[1].trim());         
            
         System.out.println("Analyzing EOs in directory for ChangeIndicator: " + sCrawlDir);
      
         x.crawlDirectory(sCrawlDir, false);
         writer.close();
  
         System.out.println("Done. Please review changeIndicatorNotSet_scan.csv file for results of the scan.");
         
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
          System.out.println("Running ChangeIndicatorNotSet Scan on series: " + series + ".......");
          System.out.println("Latest label is: " + label);
          
          ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
          String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
          String viewRoot = serverTop + "/"+series+".rdd/"+label + "/";
        
          for (int j = 0; j < families.size(); j++) {
              String family = families.get(j);
              String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, "ChangeIndicatorNotSet");
              System.out.println("Crawling dir '" + viewRoot + sCrawlDir + "' for family: " + family);
              x.crawlDirectoryNew(viewRoot,sCrawlDir,false);      
              System.out.println("Done crawling for family "+family+".");
          }        
          writer.close();
          x.dumpResultsToAB(series);
          
      } else if(mode.equals("premerge")) {
         
          String ade_view_root = args[1].trim();
          String filelistpath=args[0].trim();
          String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);
          
          for(int i = 0; i < files.length; i++) {
              
              String filePath = ade_view_root + "/" + files[i].trim();
              File f = new File(filePath);
              if(!f.exists())
                  continue;
              if(!x.fileOfInterest(filePath))
                  continue;
              x.processFile(f,false);
          }
          writer.write("\n\nPlease see https://stbeehive.oracle.com/teamcollab/wiki/Fintech+Standards:ChangeIndicatorNotSet " +
              "for description of the issue and resolution.\n\n");
          writer.close();   
      }
      FamilyModuleHelper.printScanDuration(start,"ChangeIndicatorNotSet");
      System.out.println("Number of violations found: " + violations);
  }
  
  public void makeFixes(String crawlDir) throws Exception {
    
    System.out.println("Fixing ObjectVersionNumber attributes..");
      
    BufferedReader fileReader = new BufferedReader(new FileReader("changeIndicatorNotSet_scan.csv"));
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
      String changeIndicator = parts[7].trim();
    //add attribute value if not present
     xmlFixer.modifyAttribute(n, "ChangeIndicator", changeIndicator, "true", true);      
      
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
        con.prepareStatement("INSERT INTO codescan_results(SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME," +
          "ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)");     
      
      Statement delete = con.createStatement();
      delete.executeUpdate("DELETE from codescan_results where issuetype='ChangeIndicatorNotSet' and series = '" + series + "'");
      delete.close();
      

      BufferedReader reader = new BufferedReader(new FileReader("changeIndicatorNotSet_scan.csv"));
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
        insert.setString(7, "ChangeIndicatorNotSet");
        insert.setString(8, parts[6].trim());
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
      if(f.endsWith("PEO.xml"))
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
        
        boolean changeIndicatorFound=false;
        boolean objectVersionNumberFound = false;
        String name = "";
        String changeIndicator = "";
        
        for(int i = 0; i < attributes.size(); i++) {
          
          Node attribute = attributes.get(i);
         
          String columnName = XMLParserHelper.getAttributeValue(attribute, "ColumnName");
          changeIndicator = XMLParserHelper.getAttributeValue(attribute, "ChangeIndicator");
          if(changeIndicator !=null && changeIndicator.equals("true"))
            changeIndicatorFound = true;
          
          if(columnName!= null && columnName.equals("OBJECT_VERSION_NUMBER")){
            objectVersionNumberFound = true;
            name = XMLParserHelper.getAttributeValue(attribute, "Name");
          }
        }
        
        if(objectVersionNumberFound && !changeIndicatorFound && !isExempt(FamilyModuleHelper.getPathAfterViewRoot(f))){
          writer.write(FamilyModuleHelper.getFileNameInfo1(f) + name + "," + changeIndicator+ 
          ", ChangeIndicator should be set to true on ObjectVersionNumber attribute of the EO\n");
          violations++;
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
  
    public void generateExemptions() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_Su.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 2)
            continue;
        String issue = parts[0].trim();
        if(!issue.equals("ChangeIndicatorNotSet"))
            continue;
        String fileName = parts[1].trim();
          
        exemptions.add(fileName);
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }
    
    private boolean isExempt(String filepath){
        
        if(exemptions.contains(filepath)) 
            return true;
        
        return false;
    }
}
