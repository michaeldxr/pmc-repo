package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.FileParserHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SQLDatePredicateIssuesLRG {
    
    private HashMap<String,String> tableColTimeStamp = new HashMap<String,String>();
    private HashMap<String,String> tableAliasNames = new HashMap<String,String>();
    
    private static final Pattern psysdate1 = Pattern.compile("SYSDATE\\s+BETWEEN\\s+(.+?)\\s+AND\\s+(.+?)\\s+(?:AND|OR|UNION|;)", Pattern.DOTALL);
    private static final Pattern psysdate2 = Pattern.compile("(SYSDATE\\s*<=(.*?))(?:AND|OR|UNION|;)",Pattern.DOTALL);
    private static final Pattern psysdate3 = Pattern.compile("WHERE.*(?:AND|OR)\\s+((.*?)>=\\s*SYSDATE)", Pattern.DOTALL);
    private static final Pattern psysdate4 = Pattern.compile(":\\w+\\s+BETWEEN\\s+(.+?)\\s+AND\\s+(.+?)(?:AND|OR|UNION|;)",Pattern.DOTALL);
    
    private static final Pattern pTableCol = Pattern.compile("(\\w+)\\.\\s*(\\w+)");
    private static final Pattern pColumnOnly = Pattern.compile("(\\w+)");
    private static final Pattern pnvl = Pattern.compile("NVL\\s*\\((\\w+),");
    private static final Pattern pTablesColumn = Pattern.compile("(.*?)\\((.*?)\\)");
    
    private static final Pattern multiLineComment = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern singleLineComment = Pattern.compile("--.*?(\\r)?\\n");
    private static final Pattern selectQueryPattern = Pattern.compile("SELECT\\s+(.*?)\\s+FROM\\s+.*?WHERE.*?;",Pattern.DOTALL);
    
    private static HashMap<String,ArrayList<String>> exemptions = new HashMap<String,ArrayList<String>>();
    private static BufferedWriter writer;
    private static Connection con = null;
    private static final String issuetype = "SysdateNotTruncated";
    
    public static void main(String[] args) throws Exception {
        
        try {
            if (args.length != 1 || args[0] == null || args[0].equals("")) {
                System.out.println("Series has to be specified for " +
                                   issuetype);
                System.exit(1);
            }

            String series = args[0];
         
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);

            writer = new BufferedWriter(new FileWriter("SysdateNotTruncated_LRG.csv"));
            writer.write("Family,Module,Product,Filename,Label,Sysdate Pattern," +
               "Table Name1, Column Name1, ResolvedTableCol1, TimeStamped Rows1, NonTimestamped Rows 1, " +
               "Percent TimeStamped1, Table Name2, Column Name2, ResolvedTableCol2, TimeStamped Rows2, " +
               "NonTimeStamped Rows 2, Percent Timestamped2\n");
            
            generateExemptionList();

            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
            for (int j = 0; j < families.size(); j++) {
                String family = families.get(j);
                String sCrawlDir = LRGUtil.getCrawlDir(series, family, label, issuetype);
                System.out.println("Crawling dir '" + sCrawlDir +"' for family: " + family);
                
                SQLDatePredicateIssuesLRG scanner = new SQLDatePredicateIssuesLRG();
                
                try{
                    con = DriverManager.getConnection("jdbc:oracle:thin:@//slc03zob.us.oracle.com:1522/slc03zob", "fusion", "fusion");
                } catch(Exception e){
                    System.out.println("Connection is attempted for slc03zob.us.oracle.com:1522/slc03zob but failed!");
                    e.printStackTrace();
                }  
                
                SqlIndexFinder.readViewDataFromFile();
                scanner.readTableColTimeStampsFromFile();
                scanner.crawlDirectory(sCrawlDir);
            }
            writer.close();

            LRGUtil.addLabelInfoToAB(issuetype, series, label);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void generateExemptionList() {
        
    try{
        BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
              "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_new.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(";");
        if(parts.length < 3)
            continue;
        String issue = parts[1].trim();
        if(!issue.equals("SysdateNotTruncated"))
            continue;
        String fileName = parts[0].trim();
        String regex = parts[2].trim();
        
        ArrayList<String> patterns = new ArrayList<String>();
          if(exemptions.containsKey(fileName))
              patterns = exemptions.get(fileName);
          
          patterns.add(regex);
          
        exemptions.put(fileName,patterns);
      }
    }catch(Exception e) {
        e.printStackTrace();
    }
    }
    
    private void writeTableColTimeStampToFile() throws Exception{
        
        BufferedWriter writer1 = new BufferedWriter(new FileWriter("tableCol_timestamps.txt"));
        
        for(Iterator<String> it = tableColTimeStamp.keySet().iterator(); it.hasNext(); ) {
            String tableCol = it.next();
            String timestamp = tableColTimeStamp.get(tableCol);
            
            writer1.write(tableCol + " --> " + timestamp + "\n");            
        }
        writer1.close();
    }
    
    public void readTableColTimeStampsFromFile() throws Exception {
        
        BufferedReader reader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
            "/fatools/opensource/jauditFixScripts/txt/tableCol_timestamps.txt")); 
        
        String line = "";
        while((line=reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            String timestamp = parts[1].trim();
            if(timestamp.equals(", ,"))
                timestamp = " , , ";
            tableColTimeStamp.put(parts[0].trim(),timestamp);
        }
    }
    
    private void crawlDirectory(String path) throws Exception
    {
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
                String fileofInterest = getFileOfInterest(sName);
                if(fileofInterest.equals("VO"))               
                    processVO(sName);   
                else if(fileofInterest.equals("View"))               
                    processView(sName);   
                else if(fileofInterest.equals("PLSQL"))               
                    processPkb(sName);   
                
                if(i%10==0) writer.flush();
            }
            else if(listOfFiles[i].isDirectory()) 
                crawlDirectory(listOfFiles[i].getAbsolutePath());          
            else 
                System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);          
        }      
    }
    
  public boolean fileOfInterest(String sName) 
  {
      if(!FamilyModuleHelper.isPathOfInterest(sName))
          return false;

    if (sName.endsWith("VO.xml"))
        return true;
    return false;
  }
    
    public String getFileOfInterest(String sName) 
    {
      if(!FamilyModuleHelper.isPathOfInterest(sName))
          return "";
                
          if (sName.endsWith("VO.xml"))
              return "VO";
          if (sName.endsWith(".view"))
              return "View";
          if(sName.endsWith(".pkb"))
              return "PLSQL";
          
        return "";
    }
    
    public void processVO(String f) {    
    
      XMLDocument doc = XMLParserHelper.getXMLDocument(f);

      if(doc == null) {
        //System.out.println("Could not parse XMLDocument..Skipping");
        return;
      }
            
        NodeList viewObjects = doc.getElementsByTagName("ViewObject");
        if(viewObjects == null || viewObjects.getLength() != 1) 
        {
          //System.out.println("WARNING:  Unexpected number of view objects found in view:" + f);
          //System.out.println("Skipping...");
          return;
        }
        Node viewObject = viewObjects.item(0);         
   
        String where = XMLParserHelper.getAttributeValue(viewObject, "Where");
        Node sqlQuery = XMLParserHelper.getChildNodeWithName(viewObject, "SQLQuery");
        
        String sqlQueryText ="";
            if(sqlQuery != null) {           
               
               sqlQueryText = sqlQuery.getTextContent().toUpperCase();
               if(!sqlQueryText.contains("SYSDATE"))
                  return;
            }
            if(where != null && !where.contains("SYSDATE"))
                return;

        if(where == null && sqlQuery == null)
            return; 
        
        tableAliasNames = new HashMap<String,String>();
        SQLParserHelper.parseFromSql(viewObject,tableAliasNames);
        
        if(sqlQuery != null) {              
                                  
            sqlQueryText = sqlQueryText.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
            sqlQueryText = sqlQueryText.replaceAll("--.*(\r)?\n",""); //remove single line comments
            
            processWhere(sqlQueryText,f);          
            processWhere1(sqlQueryText,f);
        }
        
        if(where != null) {        
            where = where.toUpperCase();
            processWhere(where,f);   
            processWhere1(where,f);

        }
        
    }
    
    public void processView(String f) throws Exception
    {    
    
      XMLDocument doc = XMLParserHelper.getXMLDocument(f);

      if(doc == null) {
        //System.out.println("Could not parse XMLDocument..Skipping");
        return;
      }
            
        NodeList viewObjects = doc.getElementsByTagName("VIEW");
        if(viewObjects == null || viewObjects.getLength() != 1) 
        {
//          System.out.println("WARNING:  Unexpected number of view objects found in view:" + f);
//          System.out.println("Skipping...");
          return;
        }
        Node viewObject = viewObjects.item(0);         
    
        Node sqlQuery = XMLParserHelper.getChildNodeWithName(viewObject, "SUBQUERY");

        if(sqlQuery == null)
            return; 
        
        String sqlQueryText = "";
        if(sqlQuery != null) {           
           
           sqlQueryText = sqlQuery.getTextContent().toUpperCase();
           if(!sqlQueryText.contains("SYSDATE"))
              return;
        }
        
        tableAliasNames = new HashMap<String,String>();
        SQLParserHelper.parseFromSql(viewObject,tableAliasNames);
        
        if(sqlQuery != null) {           
            
            sqlQueryText = sqlQueryText.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
            sqlQueryText = sqlQueryText.replaceAll("--.*(\r)?\n",""); //remove single line comments
            
            processWhere(sqlQueryText,f);    
            processWhere1(sqlQueryText,f);
        }        
    }
    
    public void processPkb(String f) throws Exception
    {    
    
      FileParserHelper fpHelper = new FileParserHelper(f);
        String[] lines = fpHelper.getFileContents();
        String fileText = fpHelper.getFileContentsAsString(lines,false).toUpperCase();
       
        Matcher matcher = multiLineComment.matcher(fileText);
        fileText = matcher.replaceAll("");
        
        Matcher m = selectQueryPattern.matcher(fileText);
       
       while(m.find()) {

           String sqlQuery = m.group(0);   
           
           if(!sqlQuery.contains("SYSDATE"))
               continue;
           
           matcher = singleLineComment.matcher(sqlQuery);
           sqlQuery = matcher.replaceAll("");             
           
           tableAliasNames = new HashMap<String,String>();
           SQLParserHelper.parseFromSql(sqlQuery,tableAliasNames); 
           
           processWhere(sqlQuery,f);  

           processWhere1(sqlQuery,f);
       }
    }
    
    private void processWhere1(String sql, String f) {
        
        Matcher m = null;
        String tableCol= "";
        ArrayList<String> result = null;
        
        try {
            m = psysdate2.matcher(sql);         
            if(!m.find()) 
                m = psysdate3.matcher(sql);
            else 
                m.reset();
            
            if(m.find()) {
                tableCol = m.group(2);
                result = processSysdateCol(tableCol);
                
                String timestampedRows  = getTimeStampedRows(result.get(2));  
                String sysdatePattern = m.group(1).replaceAll("\\s+"," ").replaceAll(",",":COMMA:");
                    
                if(!sysdatePattern.contains("EFFECTIVE")){
                    if(timestampedRows.equals(" , , ")) //cannot resolve timestamp
                        return;
                    
                    String[] parts = timestampedRows.split(",");
                    if(parts[0].equals("Y = 0") && parts[1].equals("N = 0") ) //if there are no rows in the table, do not flag as violation
                        return;
                    try{
                            int percent = Integer.parseInt(parts[2].trim()); 
                            if(percent > 50) //more that 50% rows are timestamped.
                                return;
                    } catch (Exception e) {}
                }
                
                ArrayList<String> exemptPatterns = exemptions.get(ViewCriteriaHelper.getFileName(f));
                if(exemptPatterns != null && exemptPatternContains(sysdatePattern, exemptPatterns))
                    return;
                    writer.write(ViewCriteriaHelper.getFileNameInfo(f,"")+ "\"" + sysdatePattern + "\"" + "," + 
                            "\"" + result.get(0) + "\"" + "," + "\"" + result.get(1) + "\"" + ","+ result.get(2) + "," + timestampedRows + "\n");  
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }                  
    }
    
    private boolean exemptPatternContains(String sysdatePattern, ArrayList<String>exemptPatterns) {
        
        sysdatePattern = sysdatePattern.trim().replaceAll(":COMMA:", ",");
        for(int i = 0; i < exemptPatterns.size(); i++) {
            if(sysdatePattern.contains(exemptPatterns.get(i).trim()))
                return true;
        }
        
        return false;
    }
    
    private ArrayList<String> processSysdateCol(String tableCol) throws Exception{
        
        ArrayList<String> result = new ArrayList<String>();
        String table1 = "";
        String col1 = "";
        String resolvedTableCol1 ="";
                   
        boolean processTableCol = true;           
        
        Matcher m1 = pTableCol.matcher(tableCol);
        if(m1.find()){
            table1 = m1.group(1);
            col1 = m1.group(2);
            if(tableAliasNames.containsKey(table1))
                table1 = tableAliasNames.get(table1);
            
        } else {
            Matcher m2 = pnvl.matcher(tableCol);
            if(m2.find()) {
            
                col1 = m2.group(1);
                Iterator<String> it = tableAliasNames.keySet().iterator();
                if(it.hasNext())
                    table1 = tableAliasNames.get(it.next());
                else table1 = "";
                
            } else {
                Matcher m3 = pColumnOnly.matcher(tableCol);
                if(m3.find()) {
                
                    col1 = m3.group(1);
                    Iterator<String> it = tableAliasNames.keySet().iterator();
                    if(it.hasNext())
                        table1 = tableAliasNames.get(it.next());
                    else table1 = "";
                    
                } else {
                    table1 = tableCol;
                    col1 = tableCol;
                    processTableCol = false;
                }
            }
        }
        
        if(processTableCol) {
            ViewCriteriaHelper.isView= false;
            ViewCriteriaHelper.resolvedTableColumn = "";
            String table = ViewCriteriaHelper.convertViewNamesIntoTableNames(table1, col1);
            resolvedTableCol1 = ViewCriteriaHelper.resolvedTableColumn;
            if(ViewCriteriaHelper.isEmpty(resolvedTableCol1))
                resolvedTableCol1 = table + " (" + col1 + ")";
            
            resolvedTableCol1 = resolvedTableCol1.replace("EXMLOOKUPPEO","EXM_LOOKUP_VALUES");
            resolvedTableCol1 = resolvedTableCol1.replace("HPUA","HZ_PARTY_USG_ASSIGNMENTS");
        }
        result.add(table1);
        result.add(col1);
        result.add(resolvedTableCol1);
        return result;
    }
    
    private void processWhere(String sql, String f) {
        
        Matcher m = psysdate1.matcher(sql);
        String colTable1 = "";
        String colTable2 = "";
        ArrayList<String> result1 = null;
        ArrayList<String> result2 = null;
        if(!m.find())
            m = psysdate4.matcher(sql);
        else
            m.reset();
             
            while(m.find()) {
                
                colTable1 = m.group(1);
                colTable2 = m.group(2);
                
              try{
    
                result1 = processSysdateCol(colTable1);
                result2 = processSysdateCol(colTable2);
                
                String timestampedRows  = getTimeStampedRows(result1.get(2));  
                String sysdatePattern = m.group(0).replaceAll("\\s+"," ").replaceAll(",",":COMMA:");
                
                if(!sysdatePattern.contains("EFFECTIVE")) {
                    if(timestampedRows.equals(" , , ")) //timestamp could not be resolved
                        return;
                    
                    String[] parts = timestampedRows.split(",");
                    if(parts[0].equals("Y = 0") && parts[1].equals("N = 0") ) //table has no rows
                        return;
                    try{
                            int percent = Integer.parseInt(parts[2].trim());
                            if(percent > 50) //More than 50% rows are timestamped
                                return;
                    } catch (Exception e) {}
                 }
                  
                 ArrayList<String> exemptPatterns = exemptions.get(ViewCriteriaHelper.getFileName(f));
                 if(exemptPatterns != null && exemptPatternContains(sysdatePattern, exemptPatterns))
                     continue;
                
                 writer.write(ViewCriteriaHelper.getFileNameInfo(f,"")+ "\"" + sysdatePattern + "\"" + "," + 
                           "\"" + result1.get(0) + "\"" + "," + "\"" + result1.get(1) + "\"" + ","+ result1.get(2) + "," + getTimeStampedRows(result1.get(2)) + "," + 
                            "\"" + result2.get(0) + "\"" + "," + "\"" + result2.get(1) + "\"" + "," + result2.get(2) + "," + getTimeStampedRows(result2.get(2)) + "\n");
            
             } catch(Exception e) {
                    e.printStackTrace();
             }
           } //while
        
    }
    
   private String getTimeStampedRows(String resolvedTableColumn) {        
        
        if(tableColTimeStamp.containsKey(resolvedTableColumn))
            return tableColTimeStamp.get(resolvedTableColumn);
    
        Matcher m =  pTablesColumn.matcher(resolvedTableColumn);
        String tableName = "";
        String colName = "";
        String timestamp = " , , ";
        int timeStampedRows = 0;
        int nonTimeStampedRows = 0;
        int percentTimeStamped = 0;
        
        if(con == null)
            return timestamp;
        
        try { 
            Statement stmt = null;
            ResultSet rs = null;
          
            stmt = con.createStatement();
            
            if(m.find()) {
                  tableName = m.group(1).trim();
                  colName = m.group(2).trim();
                  
                String sql = "SELECT timestamped, count(timestamped) AS C FROM (SELECT (CASE WHEN " + colName + " = TRUNC(" + colName + ") " +
                    "then 'N' else 'Y' END) AS timestamped FROM " + tableName + ") GROUP BY timestamped" ;
                rs = stmt.executeQuery(sql);
                
                while(rs.next()) {
                    String timestamptedCol = rs.getString("TIMESTAMPED");
                    int count = rs.getInt("C");
                    if(timestamptedCol.equals("Y"))
                        timeStampedRows = count;
                    else if(timestamptedCol.equals("N"))
                        nonTimeStampedRows = count;
                }
                timestamp = "Y = " + timeStampedRows + ",N = " + nonTimeStampedRows; 
                if(timeStampedRows > 0 && nonTimeStampedRows > 0)
                    percentTimeStamped = (timeStampedRows*100)/(nonTimeStampedRows+timeStampedRows);
                else if(timeStampedRows == 0)
                    percentTimeStamped = 0;
                else if(nonTimeStampedRows == 0)
                    percentTimeStamped = 100;
                
                timestamp += "," + percentTimeStamped;
                
                rs.close();
                stmt.close();
                tableColTimeStamp.put(resolvedTableColumn, timestamp);
            }
        } catch(Exception e) {
            e.printStackTrace();
            tableColTimeStamp.put(resolvedTableColumn, timestamp);
        }
 
        return timestamp;
    }
}
