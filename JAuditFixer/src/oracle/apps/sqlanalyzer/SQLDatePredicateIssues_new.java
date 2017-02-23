package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.sql.Types;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.FileParserHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.psr.VOAnalyzer;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SQLDatePredicateIssues_new extends JoesBaseClass{
    HashMap<String,String> tableAliasNames = new HashMap<String,String>();
    
    Pattern psysdate1 = Pattern.compile("SYSDATE\\s+BETWEEN\\s+(.+?)\\s+AND\\s+(.+?)\\s+(?:AND|OR|UNION|;)", Pattern.DOTALL);
    Pattern psysdate2 = Pattern.compile("(SYSDATE\\s*<=(.*?))(?:AND|OR|UNION|;)",Pattern.DOTALL);
    Pattern psysdate3 = Pattern.compile("WHERE.*(?:AND|OR)\\s+((.*?)>=\\s*SYSDATE)", Pattern.DOTALL);
    
    Pattern psysdate4 = Pattern.compile(":\\w+\\s+BETWEEN\\s+(.+?)\\s+AND\\s+(.+?)(?:AND|OR|UNION|;)",Pattern.DOTALL);
    
    Pattern pTableCol = Pattern.compile("(\\w+)\\.\\s*(\\w+)");
    Pattern pColumnOnly = Pattern.compile("(\\w+)");
    Pattern pnvl = Pattern.compile("NVL\\s*\\((\\w+),");
    Pattern pTablesColumn = Pattern.compile("(.*?)\\((.*?)\\)");
    
    Pattern multiLineComment = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    Pattern singleLineComment = Pattern.compile("--.*?(\\r)?\\n");
    
    Pattern selectQueryPattern = Pattern.compile("SELECT\\s+(.*?)\\s+FROM\\s+.*?WHERE.*?;",Pattern.DOTALL);
    
    HashMap<String,String> tableColTimeStamp = new HashMap<String,String>();
    HashMap<String,ArrayList<String>> exemptions = new HashMap<String,ArrayList<String>>();
    
    static BufferedWriter writer;
    static String sCrawlDir;
    static Connection con = null;
    private static String mode="view"; //can be view/LRG/premerge
    private static final String issuetype = "SysdateNotTruncated_new";
    static int releaseNo = 13;
    
    public SQLDatePredicateIssues_new() {
        super(JoesBaseClass.CRAWL_TYPE.SQL_FILES);
    }
    
    public static void main(String[] args) throws Exception
    {
        
        mode = System.getProperty("mode");
        if(mode == null)
            mode="view";
        //mode = "LRG";
        //System.out.println("Operation mode is: " + mode);
        
        writer = new BufferedWriter(new FileWriter("SysdateNotTruncated_new.csv"));
        writer.write("Family,Module,Product,Filename,Label,Sysdate Pattern," +
            "Table Name1, Column Name1, ResolvedTableCol1, TimeStamped Rows1, NonTimestamped Rows 1, " +
            "Percent TimeStamped1, Table Name2, Column Name2, ResolvedTableCol2, TimeStamped Rows2, " +
            "NonTimeStamped Rows 2, Percent Timestamped2\n");
        
        Date startDate = new Date();
        SQLDatePredicateIssues_new scanner = new SQLDatePredicateIssues_new();
        System.out.println("Starting SQLDatePredicateIssues_new scan at: " + startDate);
        
        try{
            con = DriverManager.getConnection("jdbc:oracle:thin:@//slcao715.us.oracle.com:1534/cer11box", "fusion", "fusion");
        } catch(Exception e){
            e.printStackTrace();
        }         
        
        SqlIndexFinder.readViewDataFromFile();
        //    SqlIndexFinder.populateDateTypes();
        scanner.readTableColTimeStampsFromFile();        
        scanner.generateExemptionList();
        
    
        if(mode.equals("view")){        
        
           if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
                System.out.println("Usage:  reviewViewCriteria.sh");
                System.exit(1);
            }
          
           sCrawlDir = args[0].trim();
            //        /sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/fusionapps";
          
            File f = new File(sCrawlDir);
            if(!f.exists()) {
                System.out.println("Crawl directory does not exist - " + sCrawlDir);
                System.exit(1);
            }    
            
            System.out.println("Analyzing all files in directory: " + sCrawlDir);
            scanner.crawlDirectory(sCrawlDir,false);
            writer.close();
        } else if(mode.equals("LRG")) {
            
            System.out.println("Running LRG version of the scan...");
            String series = args[0]; //only needs series name as the argument
            //String series = "FSM_MAIN_GENERIC";
            
            String release = FamilyModuleHelper.getRelease(series);
            releaseNo = 13;
            if(!ViewCriteriaHelper.isEmpty(release)) {
                try{
                     releaseNo= Integer.parseInt(release);
                     System.out.println("Release #" + releaseNo);
                }catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
                
            //String series = "FUSIONAPPS_PT.R9INT_LINUX.X64";
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);
            
            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
            String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
            String viewRoot = serverTop + "/"+series+".rdd/"+label + "/";
            
            for (int j = 0; j < families.size(); j++) {
              
                String family = families.get(j);
                String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, issuetype);
                System.out.println("Crawling dir '" + viewRoot + sCrawlDir + "' for family: " + family);           
                scanner.crawlDirectoryNew(viewRoot,sCrawlDir,false);               
                System.out.println("Done crawling for family "+family+".");
            }    
            LRGUtil.addLabelInfoToAB(issuetype, series, label);
            writer.close();
            scanner.updateDatabase(series);
            //scanner.writeTableColTimeStampToFile();
        }     
  
        if(con != null)
            con.close();
        
        FamilyModuleHelper.printScanDuration(startDate,"SQLDatePredicateIssues_new");
    }
    
    public void updateDatabase(String series) throws Exception {
        
        Connection con = null;
        HashSet<String> violations = new HashSet<String>();
        try{
            String fileName = "SysdateNotTruncated_new.csv";

            BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
            String line = fileReader.readLine();
            if(line == null) return;
            
            con = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan", "codescan", "codescan");
            con.setAutoCommit(false);
        
            CallableStatement cstmt1 = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
            CallableStatement cstmt2 = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT_P(?,?,?,?,?,?,?,?,?) }");
            
            System.out.println("First delete records from DB...");
            Statement stmt = con.createStatement();
            
            String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'SysdateNotTruncated_new' and SERIES ='" + series+"'";
            String sql1 = "DELETE FROM CODESCAN_RESULTS_P WHERE ISSUETYPE = 'SysdateNotTruncated_new' and SERIES ='" + series+"'";
          
            if(series.contains("FSM"))
              stmt.executeUpdate(sql1);   
            else
              stmt.executeUpdate(sql);          
            stmt.close();
            
            if(line == null)  {
                con.commit(); 
                return;
            }
            
            PreparedStatement pstmt1 = con.prepareStatement("INSERT INTO SYSDATE_TRUNC_SCAN(SEQ_ID, TABLECOLUMN1, TIMESTAMPEDROWS1, NONTIMESTAMPEDROWS1, PERCENTTIMESTAMPED1," +
                "TABLECOLUMN2, TIMESTAMPEDROWS2, NONTIMESTAMPEDROWS2, PERCENETTIMESTAMPED2) " +
                "VALUES(?,?,?,?,?,?,?,?,?)");
            PreparedStatement pstmt2 = con.prepareStatement("INSERT INTO SYSDATE_TRUNC_SCAN_P(SEQ_ID, TABLECOLUMN1, TIMESTAMPEDROWS1, NONTIMESTAMPEDROWS1, PERCENTTIMESTAMPED1," +
              "TABLECOLUMN2, TIMESTAMPEDROWS2, NONTIMESTAMPEDROWS2, PERCENETTIMESTAMPED2) " +
              "VALUES(?,?,?,?,?,?,?,?,?)");
            
            cstmt1.registerOutParameter(1, Types.INTEGER);
            cstmt2.registerOutParameter(1, Types.INTEGER);
            
            System.out.println("Now inserting values...");
            
            String[] labelSeries = null;
            String prevLabel = "";
            int count = 0;
          
            PreparedStatement pstmt = pstmt1;
            CallableStatement cstmt = cstmt1;
            if(series.contains("FSM")){
              pstmt = pstmt2;
              cstmt = cstmt2;
            }
            
            while((line = fileReader.readLine()) != null) {
                
               String[] parts = line.split(",");
                if(parts.length < 12 || parts.length > 18) 
                    continue;
                
                if(violations.contains(line))
                    continue; //avoid duplicates
                violations.add(line);
                
                if (LRGUtil.isDeferred(con, series, parts[3].trim()))
                    continue;
                
                if(prevLabel != parts[4].trim()) {
                    labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
                    prevLabel = parts[4].trim();
                }
              
              cstmt.setString(2, parts[0].trim()); //family
              cstmt.setString(3, parts[1].trim()); //module
              cstmt.setString(4, parts[2].trim()); //product
              cstmt.setString(5, parts[3].trim()); //filename
              cstmt.setString(9, labelSeries[0]); //series
              cstmt.setString(10, labelSeries[1]); //label
              cstmt.setString(6, "SysdateNotTruncated_new"); //issue        
              cstmt.setString(7, ""); //subissue       
              cstmt.setString(8,parts[5].trim().replaceAll(":COMMA:", ","));
                
              try{
                    cstmt.execute();    
                  
                    int seq = cstmt.getInt(1);
                    pstmt.setInt(1,seq); 
                    pstmt.setString(2,parts[8]); //resolvedtablecol1
                    pstmt.setString(3,parts[9]); //timestampedrows1
                    pstmt.setString(4,parts[10]); //nontimestampedrows1
                    pstmt.setString(5,parts[11]); //percenttimestamped1

                  if(parts.length > 14)
                      pstmt.setString(6,parts[14]);  //resolvedtablecol2
                  else
                      pstmt.setString(6,""); 
                  
                  if(parts.length > 15) 
                      pstmt.setString(7,parts[15]); //timestampedrows2
                  else 
                      pstmt.setString(7,"");
                  if(parts.length > 16)
                      pstmt.setString(8,parts[16]); //timestampedrows2
                  else
                      pstmt.setString(8,""); 

                  if(parts.length > 17) 
                      pstmt.setString(9,parts[17]); //timestampedrows2
                  else 
                      pstmt.setString(9,""); 
                      
                  pstmt.execute();
                  count ++;
                  
             } catch(SQLException e){
                  e.printStackTrace();
             }
            }
            con.commit();
            cstmt2.close();
            pstmt2.close();
            cstmt1.close();
            pstmt1.close();
            System.out.println("Total count for SysdateNotTruncated_new is "+count +" on series "+series);
            
        }catch(Exception e) {
            e.printStackTrace();
            if(con != null)
                con.rollback();
        } finally{
            if(con != null)
                con.close();
        }        
    }
    
    public void generateExemptionList() {
        
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
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
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
    
  protected void processFile(File fName, boolean bDoADE) {
  
  
      String sName = fName.getAbsolutePath();
      if(!FamilyModuleHelper.isPathOfInterest(sName))
          return;
      
      //System.out.println(sName);
        
      try{
          if (sName.endsWith("VO.xml"))
             processVO(sName);
          if (sName.endsWith(".view"))
             processView(sName);
          if(sName.endsWith(".pkb"))
              processPkb(sName);
      }catch(Exception e){
        e.printStackTrace();
      }
          
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
//          System.out.println("WARNING:  Unexpected number of view objects found in view:" + f);
//          System.out.println("Skipping...");
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
            if(where != null && !where.toUpperCase().contains("SYSDATE"))
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
        ///System.out.println("Could not parse XMLDocument..Skipping");
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
                if(f.endsWith("oracle/apps/hed/studentFinancials/customerAccounts/shared/uiModel/view/AggregateAccountSummaryPVO.xml")
                   && releaseNo <= 12)
                    return;
                if(f.endsWith("oracle/apps/financials/revenueManagement/contractObligSetup/obligRules/uiModel/view/RepresentationTypeVVO.xml")
                   && releaseNo <= 11)
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
                 if(f.endsWith("oracle/apps/hed/studentFinancials/customerAccounts/shared/uiModel/view/AggregateAccountSummaryPVO.xml")
                    && releaseNo <= 12)
                     continue;
                 if(f.endsWith("oracle/apps/financials/revenueManagement/contractObligSetup/obligRules/uiModel/view/RepresentationTypeVVO.xml")
                    && releaseNo <= 11)
                     return;
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

  protected String getSummaryReport() {
    return null;
  }

  protected String getSummaryReportSubject() {
    return null;
  }
}
