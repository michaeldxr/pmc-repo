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
import java.util.HashMap;
import java.util.HashSet;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JavaFileParserHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;
import oracle.apps.psr.VOAnalyzer;
import oracle.apps.sqlanalyzer.ViewCriteriaHelper;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ScanEOHistoryColumns extends JoesBaseClass{
  
  static BufferedWriter writer;
  static BufferedWriter writer1;
  private static String mode="view"; //LRG/view/premerge mode
  static String sCrawlDir = "";
  HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();
  public static Boolean bDoADE = false;
  static AdeDiffHelper diffHelper = new AdeDiffHelper("diff1.txt");
  static int violations = 0;
  
  public ScanEOHistoryColumns() {
      super(JoesBaseClass.CRAWL_TYPE.EO_XML);
  }
  
  public static void main(String[] args) throws Exception{
      
     java.util.Date start = new java.util.Date();
      ScanEOHistoryColumns x = new ScanEOHistoryColumns();
      x.generateExemptions();
      
      mode = System.getProperty("mode");
      if(mode == null)
          mode = "view";
      
      writer = new BufferedWriter(new FileWriter("eo_historyCol_scan.csv"));
      writer1 = new BufferedWriter(new FileWriter("eo_historyCol_scan1.csv"));
      writer.write("Family,Module,Product,Filename,Series, Label,WHO Attribute, " +
        "Description, HistoryColumn Actual Value, History Column Desired Value, UpdateableWhileInsert\n");
      writer1.write("CustomRowClass, WHO Attribute\n");
      if(mode.equals("view")) {
            
         sCrawlDir = args[0];
          //sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_PT.V2MIBFIND_LINUX.X64.rdd/LATEST/fusionapps/fin/components/receivables/vrm";
         //bDoADE= true;
         
        if(args.length > 1)
           bDoADE = new Boolean(args[1].trim());         
            
         System.out.println("Analyzing files in directory for EO History Columns: " + sCrawlDir);
      
         x.crawlDirectory(sCrawlDir, false);
         writer.close();
         writer1.close();
  
         System.out.println("Done. Please review eo_historyCol_scan.csv file for results of the scan.");
         
         if(bDoADE.booleanValue()){
            diffHelper = new AdeDiffHelper("diff1.txt");
            diffHelper.startDiffLog();      
            x.makeFixes(sCrawlDir);
            diffHelper.closeDiffLog();  
            
            diffHelper = new AdeDiffHelper("diff2.txt");
            diffHelper.startDiffLog(); 
            x.makeJavaFixes(sCrawlDir);
            diffHelper.closeDiffLog();  
         }
      }
      else if(mode.equals("LRG")){
          //String series = "FUSIONAPPS_PT.V2MIB_LINUX.X64";
          String series = args[0]; //only needs series name as the argument    
          String label = LRGUtil.getLatestLabel(series);
          System.out.println("Running ScanEOHistoryColumns Scan on series: " + series + ".......");
          System.out.println("Latest label is: " + label);
          
          ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
          String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
          String viewRoot = serverTop + "/"+series+".rdd/"+label + "/";
        
          for (int j = 0; j < families.size(); j++) {
              String family = families.get(j);
              String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, "ScanEOHistoryColumns");
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
          writer.write("\n\nPlease see https://stbeehive.oracle.com/teamcollab/wiki/Fintech+Standards:ScanEOHistoryColumns " +
              "for description of the issue and resolution.\n\n");
          writer.close();   
      }
      FamilyModuleHelper.printScanDuration(start,"ScanEOHistoryColumns");
      System.out.println("Number of violations found: " + violations);
  }
  
  public void makeJavaFixes(String crawlDir) throws Exception {
      System.out.println("Fixing EOImpl classes...");
        
      BufferedReader fileReader = new BufferedReader(new FileReader("eo_historyCol_scan1.csv"));
      String line = fileReader.readLine(); //first line is the header - read & ignore
      String prevFilePath = null;
      
      String fileContentsAsString = null;
      JavaFileParserHelper javaFileHelper = null;
      ArrayList<String> code = null;
      
      while((line = fileReader.readLine()) != null) {
          
        String[] parts = line.split(",");
        if(parts.length <2)
          continue;
          
        String filePath = parts[0].trim();
          
        if(!filePath.equals(prevFilePath)) {
          
            if(prevFilePath != null) {                
              fileContentsAsString = javaFileHelper.getFileContentsAsString(code, false);        
              applyFix(prevFilePath,fileContentsAsString);
            }

              javaFileHelper = new JavaFileParserHelper(filePath);
              String fileContentsString = javaFileHelper.getFileContentsAsString(false);
              code = javaFileHelper.getFileContentsArrayList(fileContentsString);
          
        }
        prevFilePath = filePath;
          
        String attrName = parts[1].trim();
        code = updateEOImplClass(javaFileHelper,code, attrName, filePath);
        
      } // end of csv file
      
      //apply fix to the last file 
      if(javaFileHelper != null) {
        fileContentsAsString = javaFileHelper.getFileContentsAsString(code, false);        
        applyFix(prevFilePath,fileContentsAsString);
      }
      
  }
  
  public void makeFixes(String crawlDir) throws Exception {
    
    System.out.println("Fixing EO History Columns...");
      
    BufferedReader fileReader = new BufferedReader(new FileReader("eo_historyCol_scan.csv"));
    String line = fileReader.readLine(); //first line is the header - read & ignore
    String prevFilePath = null;
    XMLDocument doc = null;
    XmlFixer xmlFixer = null;
    String[] fileContents = null;
    String fileContentsAsString = null;
    
    String viewRoot = ViewCriteriaHelper.getViewRoot(crawlDir);
    
    while((line = fileReader.readLine()) != null) {
        
      String[] parts = line.split(",");
      if(parts.length <10)
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
        xmlFixer.setFileContents(fileContents); 
        
      }
      prevFilePath = filePath;
        
      String attrName = parts[6].trim();
      Node n = findElement(doc,attrName);
      String oldHistoryColValue = parts[8].trim();
      String newHistoryColValue = parts[9].trim();
    
    if(newHistoryColValue == null || newHistoryColValue.equals("null"))
      continue;
    //add attribute value if not present
     xmlFixer.modifyAttribute(n, "HistoryColumn", oldHistoryColValue, newHistoryColValue, true);      
        
    String updateable = "";
    if(parts.length > 10)
        updateable = parts[10].trim();
    
    if((newHistoryColValue.equals("DateHistory") && attrName.equals("CreationDate")) 
       || (newHistoryColValue.equals("UserHistory") && attrName.equals("CreatedBy")))
        xmlFixer.modifyAttribute(n, "IsUpdateable", updateable, "while_insert", true);       
      
    } // end of csv file
    
    //apply fix to the last file 
    if(xmlFixer != null) {
      fileContentsAsString = xmlFixer.getFileContentsAsString(fileContents, false);        
      applyFix(prevFilePath,fileContentsAsString);
    }
    
  }
  
  private ArrayList<String> updateEOImplClass(JavaFileParserHelper javaFileHelper, 
                                              ArrayList<String> code, String attrName, String customEOImplClass){
    
      
      try{
          code = javaFileHelper.updateSetAttrInvokeAccessorMethod(attrName, code);   
          code = javaFileHelper.removeMethodWithName("set"+attrName, code, false, "");
        
      } catch(Exception e) {
          e.printStackTrace();
          System.out.println("Please fix manually: " + customEOImplClass);
      }
      
      return code;
  }
  
  private boolean isEmpty(String f) {
      if(f == null || f.trim().equals(""))
          return true;
      return false;
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
      delete.executeUpdate("DELETE from codescan_results where issuetype" +
        " in ('ScanEOHistoryColumns_missingHistory','ScanEOHistoryColumns_incorrectHistory') and series = '" + series + "'");
      delete.close();
      

      BufferedReader reader = new BufferedReader(new FileReader("eo_historyCol_scan.csv"));
      String line = reader.readLine();
      String issuetype = "";
      
      int count = 0;
      while((line = reader.readLine()) != null) {
          
          String[] parts = line.split(",");
          if(parts.length < 8)
              continue;
        count++;
        if(line.contains("This can result in Incorrect Data size SQL Errors"))
          issuetype = "ScanEOHistoryColumns_incorrectHistory";
        else issuetype = "ScanEOHistoryColumns_missingHistory";
        insert.setString(1, parts[4].trim());
        insert.setString(2, parts[5].trim());
        insert.setString(3, parts[0].trim());
        insert.setString(4, parts[1].trim());
        insert.setString(5, parts[2].trim());
        insert.setString(6, parts[3].trim());
        insert.setString(7, issuetype);
        insert.setString(8, parts[6].trim());
        insert.setString(9,parts[7].trim());
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
          
          String rowClass = XMLParserHelper.getAttributeValue(entity, "RowClass");
          String customRowClass="";
          if(rowClass != null && !rowClass.startsWith("oracle.apps.fnd")){
             
              int index = rowClass.lastIndexOf('.') +1;
              String className = rowClass.substring(index);
              
              index = f.lastIndexOf('/') +1;
              String path = f.substring(0, index);
              
              path = path + className +".java";
              File f1 = new File(path);
              if(f1 != null && f1.exists())
                  customRowClass = path;              
          }
        
        for(int i = 0; i < attributes.size(); i++) {
          
          Node attribute = attributes.get(i);
          String name = XMLParserHelper.getAttributeValue(attribute, "Name");
          String historyColumn = XMLParserHelper.getAttributeValue(attribute, "HistoryColumn");
          String columnName = XMLParserHelper.getAttributeValue(attribute, "ColumnName");
          String updateable = XMLParserHelper.getAttributeValue(attribute, "IsUpdateable");
        
          String description = "";
          String desiredValue = "";
          if(columnName == null)
            continue;
          
          if(columnName.equals("OBJECT_VERSION_NUMBER")){
            
            if(historyColumn == null || historyColumn.equals(""))
              description = "HistoryColumn should be set to VersionHistory " +
                "(select 'version number' from Entity Attribute Wizard in Jdev)";
            else if(historyColumn.equals("VersionHistory"))
              continue;
            else description = "HistoryColumn Value is " + historyColumn +
            " where it should be VersionHistory : This can result in Incorrect Data size SQL Errors";
            desiredValue = "VersionHistory";
          }
          else if(columnName.equals("LAST_UPDATED_BY")){
            
            if(historyColumn == null || historyColumn.equals(""))
              description = "HistoryColumn should be set to UserHistory " +
                "(select 'modified by' from Entity Attribute Wizard in Jdev)";
            else if(historyColumn.equals("UserHistory"))
              continue;
            else description = "HistoryColumn Value is " + historyColumn +
            " where it should be UserHistory : This can result in Incorrect Data size SQL Errors";
            desiredValue = "UserHistory";
          }
          else if(columnName.equals("CREATION_DATE")){
            
            if(historyColumn == null || historyColumn.equals(""))
              description = "HistoryColumn should be set to DateHistory " +
                "(select 'created on' from Entity Attribute Wizard in Jdev)";
            else if(historyColumn.equals("DateHistory"))
              continue;
            else description = "HistoryColumn Value is " + historyColumn +
            " where it should be DateHistory : This can result in Incorrect Data size SQL Errors";
            desiredValue = "DateHistory";
          }
          else if(columnName.equals("LAST_UPDATE_DATE")){
            
            if(historyColumn == null || historyColumn.equals(""))
              description = "HistoryColumn should be set to DateHistory " +
                "(select 'modified on' from Entity Attribute Wizard in Jdev)";
            else if(historyColumn.equals("DateHistory"))
              continue;
            else description = "HistoryColumn Value is " + historyColumn +
            " where it should be DateHistory : This can result in Incorrect Data size SQL Errors";
            desiredValue = "DateHistory";
          }
          else if(columnName.equals("CREATED_BY")){
         
            if(historyColumn == null || historyColumn.equals(""))
              description = "HistoryColumn should be set to UserHistory " +
                "(select 'created by' from Entity Attribute Wizard in Jdev)";
            else if(historyColumn.equals("UserHistory"))
              continue;
            else description = "HistoryColumn Value is " + historyColumn +
            " where it should be UserHistory : This can result in Incorrect Data size SQL Errors";
            desiredValue = "UserHistory";
          }
          else if(columnName.equals("LAST_UPDATE_LOGIN")){
            
            if(historyColumn == null || historyColumn.equals(""))
              description = "HistoryColumn should be set to 11 " +
                "(select 'last update login' from Entity Attribute Wizard in Jdev)";
            else if(historyColumn.equals("11"))
              continue;
            else description = "HistoryColumn Value is " + historyColumn +
            " where it should be 11 : This can result in Incorrect Data size SQL Errors";
            desiredValue = "11";
          } 
          else //if(historyColumn == null || historyColumn.equals(""))
            continue;
         /* else {
            
            description = "Only ObjectVersionNumber LastUpdatedBy CreationDate LastUpdateDate CreatedBy and LastUpdateLogin" +
              " can be designated as History/WHO columns";
            desiredValue = null;
          }*/
         
         if(isExempt(FamilyModuleHelper.getPathAfterViewRoot(f),name)) 
                continue;
            
          writer.write(FamilyModuleHelper.getFileNameInfo1(f)+ name + "," 
                     + description+ "," + historyColumn + "," + desiredValue + "," + updateable + "\n");
            
          if(!isEmpty(customRowClass) && isNull(historyColumn) && !isNull(desiredValue))
              writer1.write(customRowClass + "," + name + "\n");
            
          violations++;
          
        }
          
      } catch(Exception e){
          e.printStackTrace();
      }
  }
  
  private boolean isNull(String s) {
      if(s==null || s.trim().equals("null") || s.trim().equals(""))
          return true;
      return false;
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
        if(parts.length < 3)
            continue;
        String issue = parts[0].trim();
        if(!issue.equals("EoHistoryColumn"))
            continue;
        String fileName = parts[1].trim();
        String attrName = parts[2].trim();
          
        HashSet<String> exemptLines = null;
        if(!exemptions.containsKey(fileName)) 
          exemptLines = new HashSet<String>();
        else 
          exemptLines = exemptions.get(fileName);
        
        exemptLines.add(attrName);
        exemptions.put(fileName, exemptLines);
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }
    
    private boolean isExempt(String filepath, String attrName){
        
        if(!exemptions.containsKey(filepath)) 
            return false;
        
        HashSet<String> lines = (HashSet<String>)exemptions.get(filepath);
        if(lines.contains(attrName) || lines.contains("all"))
            return true;
        
        return false;
    }
}
