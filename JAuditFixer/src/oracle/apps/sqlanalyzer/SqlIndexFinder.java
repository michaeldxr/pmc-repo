package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;

public class SqlIndexFinder {
    
 // static String m_devDb = "jdbc:oracle:thin:@//adc6017fems.us.oracle.com:1540/ffintech";
  //static String m_devDb = "jdbc:oracle:thin:@//rws65181fwks.us.oracle.com:1522/rws65181";
  static String m_devDb = "jdbc:oracle:thin:@//slcak359.us.oracle.com:1532/ems4211";
  static String m_devUName = "fusion";
  static String m_devPwd = "fusion";
  static String labelServer = "";
  static {
     labelServer = FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX");
     //labelServer = "/ade_autofs/ud21_fa/FATOOLS_MAIN_LINUX.rdd/161124.0513";
      //labelServer = "/scratch/sudgupta/view_storage/sudgupta_fatools_aug5_view/";
  }
  
  static HashMap<String,String> defaultIndexPositions = new HashMap<String,String>();
  static HashMap<String,String> functionIndexPositions = new HashMap<String,String>();
  static HashMap<String,String> tableRows = new HashMap<String,String>();
  static HashMap<String,String> tableBlocks = new HashMap<String,String>();
  static HashMap<String,String> viewNameText = new HashMap<String,String>();
  static HashMap<String,String> synonyms = new HashMap<String,String>();
  static HashMap<String,String> viewRows = new HashMap<String,String>();
  static HashMap<String,ArrayList<String>> viewDependencies = new HashMap<String,ArrayList<String>>();
  public static HashMap<String,String> tableColDataType = new HashMap<String,String>();
  
  public static void main(String[] args) throws Exception{
      //TODO: Generate metadata..and copy it locally..try using local metadata for the VC scans
      //scripts to comapre the new meta data file with one in labelsever...
//      Pattern pFunctionIndex =  Pattern.compile("UPPER\\(\"(\\w+)\"\\)");
//      SqlIndexFinder.findFunctionIndexes(pFunctionIndex);
      
      HashMap<String,HashSet> default_indexColumns = findDefaultIndexes();      
      Pattern pFunctionIndex = Pattern.compile("\\w+\\(\"(\\w+)\".*?\\)");
      HashMap<String,HashSet> function_indexColumns = findFunctionIndexes(pFunctionIndex);
      
      populateTableData();
      populateViewData();
      populateDateTypes();
      
    String homeDir = System.getProperty("user.home");
    System.out.println("Processing case insensitive col indexes...");
    populateColData(homeDir + "/functionColIndexes.txt",false);
    System.out.println("Processing case sensitive col indexes...");
    populateColData(homeDir + "/defaultColIndexes.txt",true);
    System.out.println("Done!");      
      
      
      //TODO: populateColData for function and feualt indexes ..used by VC casemismatch perf script
      
     
  }
  
  public static void test() throws Exception{
      
      
      HashMap<String,HashSet> default_indexColumns = SqlIndexFinder.findDefaultIndexes();
      //      SqlIndexFinder.populateTableData();
      //      SqlIndexFinder.populateViewData();
      
      Pattern pFunctionIndex = Pattern.compile("\\w+\\(\"(\\w+)\".*?\\)");
      HashMap<String,HashSet> function_indexColumns = SqlIndexFinder.findFunctionIndexes(pFunctionIndex);
      
      //TODO: populateColData for function and feualt indexes ..used by VC casemismatch perf script
      
      System.out.println("Comparing function indexes:\n");
      BufferedReader reader = new BufferedReader(new FileReader("/ade_autofs/ud21_fa/FATOOLS_MAIN_LINUX.rdd/LATEST/fatools/opensource/jauditFixScripts/txt/functionColIndexes.txt"));
      String line = "";
      while((line = reader.readLine()) != null) {
          String[] parts = line.split("-->");
          String tableName = parts[0].trim();
          String columns = parts[1].trim();
          
          if(!function_indexColumns.containsKey(tableName)){
              System.out.println("No indexes on table: " + tableName);
              continue;
          }          
          HashSet indexedCols = function_indexColumns.get(tableName);
          parts = columns.split(",");
          for(int i = 0; i < parts.length; i++) {
              String col = parts[i].trim();
              if(!indexedCols.contains(col))
                  System.out.println("Index not found:" + col + ","+ tableName);
          }
      }
      reader.close();
      
      System.out.println("\nComparing function index positions:\n");
      reader = new BufferedReader(new FileReader("/ade_autofs/ud21_fa/FATOOLS_MAIN_LINUX.rdd/LATEST/fatools/opensource/jauditFixScripts" +
                                                                "/txt/functionIndexPositions.txt"));
      line = "";
      while((line =reader.readLine()) != null) {
          String[] parts = line.split("-->");
          String tableCol = parts[0].trim();
          String position = parts[1].trim();
          if(!functionIndexPositions.containsKey(tableCol)){
              System.out.println("No index found: " + tableCol);
              continue;
          }
          String newPosition = functionIndexPositions.get(tableCol);
          if((!newPosition.contains("1") && position.contains("1")) || (newPosition.contains("1") && !position.contains("1")))
              System.out.println("Index positions do not match:" + line);
      }
      
      
      System.out.println("\n\nComparing default indexes: ");
      reader = new BufferedReader(new FileReader("/ade_autofs/ud21_fa/FATOOLS_MAIN_LINUX.rdd/LATEST/fatools/opensource/jauditFixScripts/txt/defaultColIndexes.txt"));
      line = "";
      while((line = reader.readLine()) != null) {
          String[] parts = line.split("-->");
          String tableName = parts[0].trim();
          String columns = parts[1].trim();
          
          if(!default_indexColumns.containsKey(tableName)){
              System.out.println("No indexes on table: " + tableName);
              continue;
          }          
          HashSet indexedCols = default_indexColumns.get(tableName);
          parts = columns.split(",");
          for(int i = 0; i < parts.length; i++) {
              String col = parts[i].trim();
              if(!indexedCols.contains(col))
                  System.out.println("Index not found:" + col + ","+ tableName);
          }
      }
      
      System.out.println("\nComparing default index positions:\n");
      reader = new BufferedReader(new FileReader("/ade_autofs/ud21_fa/FATOOLS_MAIN_LINUX.rdd/LATEST/fatools/opensource/jauditFixScripts" +
                                                                "/txt/defaultIndexPositions.txt"));
      line = "";
      while((line =reader.readLine()) != null) {
          String[] parts = line.split("-->");
          String tableCol = parts[0].trim();
          String position = parts[1].trim();
          if(!defaultIndexPositions.containsKey(tableCol)){
              System.out.println("No index found: " + tableCol);
              continue;
          }
          String newPosition = defaultIndexPositions.get(tableCol);
            if((!newPosition.contains("1") && position.contains("1")) || (newPosition.contains("1") && !position.contains("1")))
              System.out.println("Index positions do not match:" + line);
      }
  }
  //Find the default case-sensitive indexes from dba_ind_columns
  public static HashMap<String,HashSet> findDefaultIndexes() throws Exception{
    
    Connection con = null;
    Statement stmt = null;
    ResultSet rs = null;
    HashMap<String,HashSet> caseSensitive_indexColumns = new HashMap<String,HashSet>();
    try {       
        
      con = DriverManager.getConnection(m_devDb, m_devUName, m_devPwd);
      stmt = con.createStatement(); 
      rs = stmt.executeQuery("select TABLE_NAME,COLUMN_NAME,COLUMN_POSITION from DBA_IND_COLUMNS where TABLE_OWNER in ('FUSION','FUSION_SETUP')");
        
      while(rs.next()) {
        
        String tableName =rs.getString("TABLE_NAME");     
        String colName = rs.getString("COLUMN_NAME");
        String position = rs.getString("COLUMN_POSITION");
          
        String existingPosition = defaultIndexPositions.get(tableName+","+colName);
        if(existingPosition != null)
            position = existingPosition + ";" + position;
        
        defaultIndexPositions.put(tableName+","+colName, position);
          
        HashSet indexColumns = new HashSet();
        if(caseSensitive_indexColumns.containsKey(tableName))
            indexColumns = caseSensitive_indexColumns.get(tableName);
        
        indexColumns.add(colName);
        caseSensitive_indexColumns.put(tableName, indexColumns);
      }
        
    } catch(Exception ex){
      ex.printStackTrace();
    } finally {
        con.commit();
        rs.close();
        stmt.close();
        con.close();
    }
      String homeDir = System.getProperty("user.home");
      BufferedWriter writer1 = new BufferedWriter(new FileWriter(homeDir + "/defaultIndexPositions.txt"));
      
      Iterator it = defaultIndexPositions.keySet().iterator();
      while(it.hasNext()){
        String tableCol = (String)it.next();
        String data = defaultIndexPositions.get(tableCol);
        writer1.write(tableCol + " --> " + data + "\n");          
      }
      writer1.close();
      
      writer1 = new BufferedWriter(new FileWriter(homeDir + "/defaultColIndexes.txt"));
            
      it = caseSensitive_indexColumns.keySet().iterator();
      while(it.hasNext()){
        String table = (String)it.next();
        HashSet<String> cols = caseSensitive_indexColumns.get(table);
          
        String tableCols = "";
        
        for(Iterator<String> it1 = cols.iterator(); it1.hasNext();)
            tableCols += it1.next() + ",";
        writer1.write(table + " --> " + tableCols + "\n");          
      }
      writer1.close();
      
    return caseSensitive_indexColumns;
  }
  
  //find function-based indexes UPPER(attrName) from dba_ind_expressions
  public static HashMap<String,HashSet>  findFunctionIndexes(Pattern pFunctionIndex) throws Exception{
      
    Connection con = null;
    Statement stmt = null;
    ResultSet rs = null;
    HashMap<String,HashSet> function_indexColumns = new HashMap<String,HashSet>();
    try {       
        
      con = DriverManager.getConnection(m_devDb, m_devUName, m_devPwd);
      stmt = con.createStatement(); 
      rs = stmt.executeQuery("select TABLE_NAME,COLUMN_EXPRESSION,COLUMN_POSITION from DBA_IND_EXPRESSIONS where TABLE_OWNER in ('FUSION','FUSION_SETUP')");
        
      while(rs.next()) {
        
        String tableName =rs.getString("TABLE_NAME");     
        String colExpr = rs.getString("COLUMN_EXPRESSION");
        String position = rs.getString("COLUMN_POSITION");
          
        String colName = null;
          
        Matcher m = pFunctionIndex.matcher(colExpr);
        if(m.find())
            colName = m.group(1);
        
        if(colName != null) {
            
            String existingPosition = functionIndexPositions.get(tableName+","+colName);
            if(existingPosition != null)
                position = existingPosition + ";" + position;
        
          functionIndexPositions.put(tableName+","+colName, position);
          HashSet indexColumns = new HashSet();
          if(function_indexColumns.containsKey(tableName))
              indexColumns = function_indexColumns.get(tableName);
          
          indexColumns.add(colName);
          function_indexColumns.put(tableName, indexColumns);
        }
      }        
    } catch(Exception ex){
      ex.printStackTrace();
    } finally {
        con.commit();
        rs.close();
        stmt.close();
        con.close();
    }
    
      String homeDir = System.getProperty("user.home");
      BufferedWriter writer1 = new BufferedWriter(new FileWriter(homeDir + "/functionIndexPositions.txt")); //ci as functionIndexPositions.txt or upperIndexPositions.txt depending on the pattern
      
      Iterator it = functionIndexPositions.keySet().iterator();
      while(it.hasNext()){
        String tableCol = (String)it.next();
        String data = functionIndexPositions.get(tableCol);
        writer1.write(tableCol + " --> " + data + "\n");          
      }
      writer1.close();
      
      writer1 = new BufferedWriter(new FileWriter(homeDir + "/functionColIndexes.txt"));
            
      it = function_indexColumns.keySet().iterator();
      while(it.hasNext()){
        String table = (String)it.next();
        HashSet<String> cols = function_indexColumns.get(table);
          
        String tableCols = "";
        
        for(Iterator<String> it1 = cols.iterator(); it1.hasNext();)
            tableCols += it1.next() + ",";
        writer1.write(table + " --> " + tableCols + "\n");          
      }
      writer1.close();
      
    return function_indexColumns;
  }
  
    public static void readColPositionsFromFile() throws Exception{ 
        
        String fileName = labelServer+ "/fatools/opensource/jauditFixScripts/txt/defaultIndexPositions.txt";
        
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = "";
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            defaultIndexPositions.put(parts[0].trim(),parts[1].trim());          
        }
        
         fileName = labelServer + "/fatools/opensource/jauditFixScripts/txt/functionIndexPositions.txt";
         //if(indexExpr.equals("function"))
            // fileName ="/scratch/sudgupta/functionIndexPositions.txt";
//         else if(indexExpr.equals("upper"))
//             fileName ="/scratch/sudgupta/upperIndexPositions.txt";
        
        reader = new BufferedReader(new FileReader(fileName));
        line = "";
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            functionIndexPositions.put(parts[0].trim(),parts[1].trim());          
        }
        
    }
    
    public static void readColPositionsFromFile_rel11() throws Exception{ 
        
        String fileName = labelServer+ "/fatools/opensource/jauditFixScripts/txt/defaultIndexPositions_rel11.txt";
        
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = "";
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            defaultIndexPositions.put(parts[0].trim(),parts[1].trim());          
        }
        
         fileName = labelServer + "/fatools/opensource/jauditFixScripts/txt/functionIndexPositions_rel11.txt";
         //if(indexExpr.equals("function"))
            // fileName ="/scratch/sudgupta/functionIndexPositions.txt";
    //         else if(indexExpr.equals("upper"))
    //             fileName ="/scratch/sudgupta/upperIndexPositions.txt";
        
        reader = new BufferedReader(new FileReader(fileName));
        line = "";
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            functionIndexPositions.put(parts[0].trim(),parts[1].trim());          
        }
        
    }
    
    public static String getResolvedTablesColumn(String viewName, String aliasName) throws Exception {
        
        String resolvedTablesColumn = "";
        
        if(ViewCriteriaHelper.isEmpty(viewName))
            return resolvedTablesColumn;
        
        if(!viewNameText.containsKey(viewName)) //it is not a view
            return resolvedTablesColumn;
        
        String viewText = viewNameText.get(viewName);
        
        HashMap<String,HashSet> colNameTableNames = new HashMap<String,HashSet>();
        HashMap<String,String> colAliasName = new HashMap<String,String>();
        HashMap<String,String> tableAliasNames = new HashMap<String,String>();   
        
        SQLParserHelper.nestedQueries = new HashMap<String,String>();
        SQLParserHelper.num_nestedQueriesFound = 0;
        
        viewText = SQLParserHelper.removeNestedQueries(viewText); 
        SQLParserHelper.parseQuery(viewText,tableAliasNames,colAliasName,colNameTableNames);
        
        for(Iterator<String> it = SQLParserHelper.nestedQueries.values().iterator(); it.hasNext();) {
            String nestedQuery = it.next();
            SQLParserHelper.parseQuery(nestedQuery,tableAliasNames,colAliasName,colNameTableNames);
        }
        
        String colName = colAliasName.get(aliasName);
        String tableNames = "";
        if(colName == null){
//            tableNames = getReferencedTables(viewName);
//            return tableNames + "(" + aliasName + ")";
              return "";
            
        } else {
            HashSet possibleTables = colNameTableNames.get(aliasName);
            for(Iterator it = possibleTables.iterator(); it.hasNext();){
              String tableName = (String)it.next();
                
                if(synonyms.containsKey(tableName))
                    tableName = synonyms.get(tableName);
                
                tableNames+=tableName + " ";
            }
            
            return tableNames + "(" + colName + ")";
        }
        
    }
    
    public static String getReferencedTabledFromFile(String viewName) {
        
        String tableNames = "";
        
        ArrayList<String> viewDependents = viewDependencies.get(viewName);
        if(viewDependents == null)
            return tableNames;
        
        for(int i = 0; i < viewDependents.size(); i++){
            
            String[] parts = viewDependents.get(i).split(",");
            if(parts.length < 2)
                continue;
            String referencedName = parts[0].trim();
            String referencedType = parts[1].trim();
            
            if(referencedType.equals("TABLE"))
              tableNames += referencedName + " ";                
            else if(referencedType.equals("SYNONYM")) {
                if(SqlIndexFinder.synonyms.containsKey(referencedName)){
                    referencedName = SqlIndexFinder.synonyms.get(referencedName);
                    tableNames += referencedName + " ";           
                }
            }                
            else if(referencedType.equals("VIEW")) {
                
                ArrayList<String> viewDependents1 = viewDependencies.get(referencedName);
                for(int j = 0; j < viewDependents1.size(); j++) {
                    
                    String[] parts1 = viewDependents1.get(j).split(",");
                    if(parts1.length < 2)
                        continue;
                    if(parts1[1].trim().equals("TABLE"))
                        tableNames += parts1[0].trim() + " ";
                }
            }
        }
        
        return tableNames;
    }
    
    public static String getReferencedTables(String viewName) throws Exception {
        
        if(ViewCriteriaHelper.isEmpty(viewName))
            return viewName;
        
        if(!viewNameText.containsKey(viewName)) //it is not a view
            return viewName;
        
         return getReferencedTabledFromFile(viewName);
        
        
        /*
        String tableNames = "";
       
        try {       
            
            Connection con = DriverManager.getConnection(m_devDb, m_devUName, m_devPwd);
            Statement stmt = con.createStatement(); 
            ResultSet rs = stmt.executeQuery("select REFERENCED_NAME,REFERENCED_TYPE from DBA_DEPENDENCIES " +
                "where OWNER='FUSION' and REFERENCED_NAME <> NAME and NAME = '" + viewName + "'");
            
            PreparedStatement pstmt = con.prepareStatement("select REFERENCED_NAME " +
                "from DBA_DEPENDENCIES WHERE REFERENCED_TYPE = 'TABLE' AND NAME = ?");
            ResultSet rs2 = null;
            
          while(rs.next()) {
            
            String referencedName =rs.getString("REFERENCED_NAME");     
            String referencedType = rs.getString("REFERENCED_TYPE"); 
              if(referencedType.equals("TABLE"))
                tableNames += referencedName + " ";
              else if(referencedType.equals("VIEW")) {
                  pstmt.setString(1, referencedName);
                  rs2 = pstmt.executeQuery();
                  while(rs2.next())
                      tableNames += rs2.getString("REFERENCED_NAME") + " ";
              }
          }
            con.commit();
            rs.close();
            stmt.close();
            con.close();
            
        } catch(Exception ex){
          ex.printStackTrace();
        } 
        return tableNames; */
    }
    
    public static void populateDateTypes() throws Exception {
        
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        con = DriverManager.getConnection(m_devDb, m_devUName, m_devPwd);
        stmt = con.createStatement(); 
        rs = stmt.executeQuery("select TABLE_NAME,COLUMN_NAME,DATA_TYPE from ALL_TAB_COLUMNS where OWNER in ('FUSION','FUSION_SETUP') " +
            "and (DATA_TYPE = 'DATE' OR DATA_TYPE like 'TIMESTAMP%')");
        
        while(rs.next()) {
          
          String tableName =rs.getString("TABLE_NAME");     
          String colName = rs.getString("COLUMN_NAME");
          String dataType = rs.getString("DATA_TYPE");
            
          tableColDataType.put(tableName+" ("+colName+")", dataType);
        }
        
        String homeDir = System.getProperty("user.home");
        BufferedWriter writer1 = new BufferedWriter(new FileWriter(homeDir + "/tableDateTypes.txt"));
        
        Iterator it = tableColDataType.keySet().iterator();
        while(it.hasNext()){
          String tableCol = (String)it.next();
          writer1.write(tableCol + " --> " + tableColDataType.get(tableCol) + "\n");          
        }
        writer1.close();
    }
    
    public static void populateTableData() throws Exception{
           
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {       
            
          con = DriverManager.getConnection(m_devDb, m_devUName, m_devPwd);
          stmt = con.createStatement(); 
          rs = stmt.executeQuery("select TABLE_NAME,NUM_ROWS,BLOCKS from DBA_TABLES where OWNER in ('FUSION','FUSION_SETUP')");
            
          while(rs.next()) {
            
            String tableName =rs.getString("TABLE_NAME");     
            String numRows = rs.getString("NUM_ROWS");
            String blocks = rs.getString("BLOCKS");
              
            tableRows.put(tableName, numRows);
            tableBlocks.put(tableName, blocks);
          }
            
        } catch(Exception ex){
          ex.printStackTrace();
        } finally {
            con.commit();
            rs.close();
            stmt.close();
            con.close();
        }
          String homeDir = System.getProperty("user.home");
          BufferedWriter writer1 = new BufferedWriter(new FileWriter(homeDir + "/tableRowsBlocks.txt"));
          
          Iterator it = tableRows.keySet().iterator();
          while(it.hasNext()){
            String tableName = (String)it.next();
            String numRows = tableRows.get(tableName);
            String numBlocks = tableBlocks.get(tableName);
            writer1.write(tableName + " --> " + numRows + "," + numBlocks + "\n");          
          }
          writer1.close();
    }
    
    public static void populateViewData() throws Exception{
           
        Connection con = null;
        Statement stmt1 = null;
        ResultSet rs1 = null;
        Statement stmt2 = null;
        ResultSet rs2 = null;
        Statement stmt3 = null;
        ResultSet rs3 = null;

        try {       
            
          con = DriverManager.getConnection(m_devDb, m_devUName, m_devPwd);
          stmt1 = con.createStatement(); 
          rs1 = stmt1.executeQuery("select VIEW_NAME,TEXT from DBA_VIEWS where OWNER in ('FUSION','FUSION_SETUP')");
         
            
          while(rs1.next()) {
            
            String viewName =rs1.getString("VIEW_NAME");     
            String viewText = rs1.getString("TEXT");
              
            viewText = viewText.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
            viewText = viewText.replaceAll("--.*$",""); //remove single line comments
            viewNameText.put(viewName, viewText);
          }
            
            stmt2 = con.createStatement(); 
            rs2 = stmt2.executeQuery("select SYNONYM_NAME,TABLE_NAME from DBA_SYNONYMS where TABLE_OWNER in ('FUSION', 'FUSION_SETUP') " +
                  "and SYNONYM_NAME <> TABLE_NAME and TABLE_NAME <> 'S_ROW_ID_SEQ'");
            
            while(rs2.next()) {
              
              String syn =rs2.getString("SYNONYM_NAME");     
              String tableName = rs2.getString("TABLE_NAME");
                
              synonyms.put(syn, tableName);
            }
            
            stmt3 = con.createStatement(); 
            rs3 = stmt3.executeQuery("select NAME,REFERENCED_NAME,REFERENCED_TYPE from DBA_DEPENDENCIES where OWNER in ('FUSION_SETUP','FUSION') " +
                  "and REFERENCED_OWNER='FUSION_SETUP' and TYPE='VIEW' and referenced_name <> name " +
                "and (referenced_type ='TABLE' or referenced_type='VIEW' or referenced_type='SYNONYM')");
            
            while(rs3.next()) {
              
              String viewName =rs3.getString("NAME");     
              String tableName = rs3.getString("REFERENCED_NAME");
              String referencedType = rs3.getString("REFERENCED_TYPE");
                
              ArrayList<String> dependencies = new ArrayList<String>();
                if(viewDependencies.containsKey(viewName))
                    dependencies = viewDependencies.get(viewName);

               dependencies.add(tableName+","+referencedType);

                viewDependencies.put(viewName, dependencies);
            }
            
        } catch(Exception ex){
            ex.printStackTrace();
        } finally {
            con.commit();
            rs1.close();
            stmt1.close();
            rs2.close();
            stmt2.close();
            rs3.close();
            stmt3.close();
            con.close();
        }
          String homeDir = System.getProperty("user.home");
          BufferedWriter writer1 = new BufferedWriter(new FileWriter(homeDir + "/viewNameText.txt"));
          
          Iterator it = viewNameText.keySet().iterator();
          while(it.hasNext()){
            String viewName = (String)it.next();
            String viewText = viewNameText.get(viewName).replace("\n", " ");
            writer1.write(viewName + " --> " + viewText + "\n");          
          }
          writer1.close();
          
        writer1 = new BufferedWriter(new FileWriter(homeDir + "/synonyms.txt"));
        
        it = synonyms.keySet().iterator();
        while(it.hasNext()){
          String synName = (String)it.next();
          String tabName = synonyms.get(synName);
          writer1.write(synName + " --> " + tabName + "\n");          
        }
        writer1.close();
        
        writer1 = new BufferedWriter(new FileWriter(homeDir + "/viewDependencies.txt"));
        
        it = viewDependencies.keySet().iterator();
        while(it.hasNext()){
          String viewName = (String)it.next();
          ArrayList<String> dependencies = viewDependencies.get(viewName);
          
          String viewDependents = "";
          for(int i = 0; i< dependencies.size();i++)
              viewDependents += dependencies.get(i)+";";
          
          writer1.write(viewName + " --> " + viewDependents + "\n");          
        }
        writer1.close();
    }
    
    public static void readTableDataFromFile() {
        
        try{
        String fileName = labelServer +     "/fatools/opensource/jauditFixScripts/txt/tableRowsBlocks.txt";
        
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = "";
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            String tableName = parts[0].trim();
            String[] rowBlocks = parts[1].split(",");
            if(rowBlocks.length < 2)
                continue;
            String rows = rowBlocks[0].trim();
            String blocks = rowBlocks[1].trim();
            
            tableRows.put(tableName,rows);   
            tableBlocks.put(tableName,blocks);
        }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public static void readTableDataFromFile_rel11() {
        
        try{
        String fileName = labelServer +     "/fatools/opensource/jauditFixScripts/txt/tableRowsBlocks_rel11.txt";
        
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = "";
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            String tableName = parts[0].trim();
            String[] rowBlocks = parts[1].split(",");
            if(rowBlocks.length < 2)
                continue;
            String rows = rowBlocks[0].trim();
            String blocks = rowBlocks[1].trim();
            
            tableRows.put(tableName,rows);   
            tableBlocks.put(tableName,blocks);
        }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public static void readTableDataFromFile_customer(){
        
        try{
        String fileName = labelServer +     "/fatools/opensource/jauditFixScripts/txt/tableRowsBlocks.txt";
        
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = "";
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            String tableName = parts[0].trim();
            String[] rowBlocks = parts[1].split(",");
            if(rowBlocks.length < 2)
                continue;
            String rows = rowBlocks[0].trim();
            String blocks = rowBlocks[1].trim();
            
            tableRows.put(tableName,rows);   
            tableBlocks.put(tableName,blocks);
        }
            
       reader.close();        
       fileName = labelServer + "/fatools/opensource/jauditFixScripts/txt/crmap_tabrowsblocks.txt";
        
        reader = new BufferedReader(new FileReader(fileName));
        line = "";
        
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            String tableName = parts[0].trim();
            String[] rowBlocks = parts[1].trim().split(",");
            if(rowBlocks.length < 2)
                continue;
            String rows = rowBlocks[0].trim();
            String blocks = rowBlocks[1].trim();
            
            if(tableRows.containsKey(tableName) && !isEmpty(rows)) {
                try{
                String count = tableRows.get(tableName);
                int oldrows=0;
                if(!isEmpty(count))                        
                     oldrows = Integer.parseInt(count);
                int newrows = Integer.parseInt(rows);
                tableRows.put(tableName,max(oldrows,newrows).toString());
                }catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            
            if(tableBlocks.containsKey(tableName) && !isEmpty(blocks)) {
                try{
                    String count = tableBlocks.get(tableName);
                    int oldblocks=0;
                    if(!isEmpty(count))                        
                         oldblocks = Integer.parseInt(count);                
                int newblocks = Integer.parseInt(blocks);
                tableBlocks.put(tableName, max(oldblocks,newblocks).toString());
                }catch(NumberFormatException e) {
                    e.printStackTrace();
                }
            }               
        }
        
        fileName = labelServer + "/fatools/opensource/jauditFixScripts/txt/schneider_tabrowsblocks.txt";
        
        reader = new BufferedReader(new FileReader(fileName));
        line = "";
        
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            String tableName = parts[0].trim();
            String[] rowBlocks = parts[1].trim().split(",");
            if(rowBlocks.length < 2)
                continue;
            String rows = rowBlocks[0].trim();
            String blocks = rowBlocks[1].trim();
            
            if(tableRows.containsKey(tableName) && !isEmpty(rows)) {
                try{
                    String count = tableRows.get(tableName);
                    int oldrows=0;
                    if(!isEmpty(count))                        
                         oldrows = Integer.parseInt(count);
                int newrows = Integer.parseInt(rows);
                tableRows.put(tableName,max(oldrows,newrows).toString());
                }catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            
            if(tableBlocks.containsKey(tableName) && !isEmpty(blocks)) {
                try{
                    String count = tableBlocks.get(tableName);
                    int oldblocks=0;
                    if(!isEmpty(count))                        
                         oldblocks = Integer.parseInt(count);  
                int newblocks = Integer.parseInt(blocks);
                tableBlocks.put(tableName, max(oldblocks,newblocks).toString());
                }catch(NumberFormatException e) {
                    e.printStackTrace();
                }
            }               
        }
        } catch(Exception e){
            e.printStackTrace();
        }
        
    }
    
    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("") || str.trim().equals("null"))
            return true;
        return false;
    }
    
    private static Integer max(int c1, int c2) {
        if(c1 <= c2) return c2;
        else return c1;
    }
    
    public static void readViewDataFromFile() throws Exception{
        
        String fileName = labelServer + "/fatools/opensource/jauditFixScripts/txt/viewNameText.txt";
        
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = "";
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            String viewName = parts[0].trim();
            String viewText = parts[1].trim();
            viewNameText.put(viewName,viewText);   
        }        
        
        fileName = labelServer + "/fatools/opensource/jauditFixScripts/txt/synonyms.txt";
        
        reader = new BufferedReader(new FileReader(fileName));
        line = "";
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            String synName = parts[0].trim();
            String tabName = parts[1].trim();
            synonyms.put(synName,tabName);   
        }        
   
        fileName = labelServer + "/fatools/opensource/jauditFixScripts/txt/viewDependencies.txt";
       //fileName = "/scratch/sudgupta/viewDependencies.txt";
        
        reader = new BufferedReader(new FileReader(fileName));
        line = "";
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            String viewName = parts[0].trim();
            String dependencies = parts[1].trim();
            
            ArrayList<String> viewDependents = new ArrayList<String>();
            
            String[] dependentParts = dependencies.split(";");
            for(int j = 0; j < dependentParts.length; j++)
                if(!dependentParts[j].trim().equals(""))
                    viewDependents.add(dependentParts[j].trim());
                
            viewDependencies.put(viewName,viewDependents);   
        }        
    }
    
    
    public static void readViewDataFromFile_rel11() throws Exception{
        
        String fileName = labelServer + "/fatools/opensource/jauditFixScripts/txt/viewNameText_rel11.txt";
        
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line = "";
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            String viewName = parts[0].trim();
            String viewText = parts[1].trim();
            viewNameText.put(viewName,viewText);   
        }        
        
        fileName = labelServer + "/fatools/opensource/jauditFixScripts/txt/synonyms_rel11.txt";
        
        reader = new BufferedReader(new FileReader(fileName));
        line = "";
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            String synName = parts[0].trim();
            String tabName = parts[1].trim();
            synonyms.put(synName,tabName);   
        }        
    
        fileName = labelServer + "/fatools/opensource/jauditFixScripts/txt/viewDependencies_rel11.txt";
       //fileName = "/scratch/sudgupta/viewDependencies.txt";
        
        reader = new BufferedReader(new FileReader(fileName));
        line = "";
        while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            if(parts.length < 2)
                continue;
            String viewName = parts[0].trim();
            String dependencies = parts[1].trim();
            
            ArrayList<String> viewDependents = new ArrayList<String>();
            
            String[] dependentParts = dependencies.split(";");
            for(int j = 0; j < dependentParts.length; j++)
                if(!dependentParts[j].trim().equals(""))
                    viewDependents.add(dependentParts[j].trim());
                
            viewDependencies.put(viewName,viewDependents);   
        }        
    }
    
    
    public static String getViewRows(String viewName) throws Exception{
       
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {       
            
          if(viewRows.containsKey(viewName))
              return viewRows.get(viewName);
            
          con = DriverManager.getConnection(m_devDb, m_devUName, m_devPwd);
          stmt = con.createStatement(); 
          rs = stmt.executeQuery("select count(*) AS COUNT_ROWS from " + viewName);
            
          while(rs.next()) {            
            String count =rs.getString("COUNT_ROWS");  
              
            viewRows.put(viewName, count);
            return count;

          }
            
        } catch(Exception ex){
          ex.printStackTrace();
        } finally {
            con.commit();
            rs.close();
            stmt.close();
            con.close();
        }
        return null;
    }
    
    public static boolean LeadColumn(String colPosition){
          if(colPosition == null || colPosition.trim().equals(""))
              return false;
          
          String[] parts = colPosition.split(";");
          for(int i = 0; i < parts.length; i++)
              if(parts[i].trim().equals("1"))
                  return true;
          
          return false;
          
    }
    
    public static HashMap<String,HashSet> readIndexesFromFile(String caseflag) throws Exception {
          
          BufferedReader reader = null;
          HashMap<String,HashSet> result = new HashMap<String,HashSet>();
          
          System.out.println("labelserver: " + labelServer);
       
          if(caseflag.equals("function"))
              reader = new BufferedReader(new FileReader(labelServer +
                                                         "/fatools/opensource/jauditFixScripts/txt/functionColIndexes.txt"));
          else if(caseflag.equals("default"))
              reader = new BufferedReader(new FileReader(labelServer +
                                                         "/fatools/opensource/jauditFixScripts/txt/defaultColIndexes.txt"));          
          else if(caseflag.equals("caseInsensitive"))
              reader = new BufferedReader(new FileReader(labelServer +
                                                         "/fatools/opensource/jauditFixScripts/txt/caseInsensitiveColIndexes.txt"));
          else if(caseflag.equals("caseSensitive"))
              reader = new BufferedReader(new FileReader(labelServer +
                                                         "/fatools/opensource/jauditFixScripts/txt/caseSensitiveColIndexes.txt"));
          
          String line = null;
          while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            String tableName = parts[0].trim();
            String colIndexes = parts[1].trim();
              
            HashSet indexes = new HashSet();
            String[] columns = colIndexes.split(",");
            
            for(int i = 0; i < columns.length; i++)
                indexes.add(columns[i]);
              
            result.put(tableName,indexes);
          }
          reader.close();
          return result;  
      }
      
    public static HashMap<String,HashSet> readIndexesFromFile_rel11(String caseflag) throws Exception {
          
          BufferedReader reader = null;
          HashMap<String,HashSet> result = new HashMap<String,HashSet>();
          
          System.out.println("labelserver: " + labelServer);
       
          if(caseflag.equals("function"))
              reader = new BufferedReader(new FileReader(labelServer +
                                                         "/fatools/opensource/jauditFixScripts/txt/functionColIndexes_rel11.txt"));
          else if(caseflag.equals("default"))
              reader = new BufferedReader(new FileReader(labelServer +
                                                         "/fatools/opensource/jauditFixScripts/txt/defaultColIndexes_rel11.txt"));          
          else if(caseflag.equals("caseInsensitive"))
              reader = new BufferedReader(new FileReader(labelServer +
                                                         "/fatools/opensource/jauditFixScripts/txt/caseInsensitiveColIndexes.txt"));
          else if(caseflag.equals("caseSensitive"))
              reader = new BufferedReader(new FileReader(labelServer +
                                                         "/fatools/opensource/jauditFixScripts/txt/caseSensitiveColIndexes.txt"));
          
          String line = null;
          while((line = reader.readLine()) != null) {
            String[] parts = line.split("-->");
            String tableName = parts[0].trim();
            String colIndexes = parts[1].trim();
              
            HashSet indexes = new HashSet();
            String[] columns = colIndexes.split(",");
            
            for(int i = 0; i < columns.length; i++)
                indexes.add(columns[i]);
              
            result.put(tableName,indexes);
          }
          reader.close();
          return result;  
      }
    
    public static HashMap<String,String> findDataAllCaps(HashSet<String> tableColumns) throws Exception{
          
        HashMap<String,String> tableColumnsData = new HashMap<String,String>();
        
        if(CaseInsensitiveViewCriteria.refreshFromDB) {
        
            String homeDir = System.getProperty("user.home");
            System.out.println("Processing case insensitive col indexes...");
            populateColData(homeDir + "/caseInsensitiveColIndexes.txt",false);
            System.out.println("Processing case sensitive col indexes...");
            populateColData(homeDir + "/caseSensitiveColIndexes.txt",true);
            System.out.println("Done!");      
        }
       
        tableColumnsData = readTableColDataFromFile(tableColumns);
        
        return tableColumnsData;
      }
    
    private static HashMap<String,String> readTableColDataFromFile(HashSet<String> tableColumns) throws Exception {
           
           BufferedReader reader = null;
           HashMap<String,String> result = new HashMap<String,String>();
        
           reader = new BufferedReader(new FileReader(labelServer +  "/fatools/opensource/jauditFixScripts/txt/tableColumnData.txt"));
           
           String line = null;
           while((line = reader.readLine()) != null) {
             String[] parts = line.split("-->");
             String tableCol = parts[0].trim();
               if(!tableColumns.contains(tableCol)) {
                 continue;
               }
             String data = parts[1].trim();
               
             result.put(tableCol,data);
           }
           reader.close();
           if(result.size() != tableColumns.size()){
               
               for(Iterator<String> it = tableColumns.iterator(); it.hasNext();){
                   String tableCol = it.next();
                   if(!result.containsKey(tableCol))
                       System.out.println("Could not resolve table column data for: " + tableCol);
               }
           }
           return result;  
       }
       
    private static void populateColData(String fileName, boolean append) throws Exception{
               
           Connection con = DriverManager.getConnection(m_devDb, m_devUName, m_devPwd);
           
           BufferedReader reader = new BufferedReader(new FileReader(fileName));
           String homeDir = System.getProperty("user.home");
           BufferedWriter writer = new BufferedWriter(new FileWriter(homeDir + "/tableColumnData.txt",append));
           
           String line = "";
           Statement stmt1 = con.createStatement();;
           Statement stmt2 = con.createStatement();;
           PreparedStatement pstmt = con.prepareStatement("Select data_type from all_tab_cols where table_name = ? and column_name = ?");
           ResultSet rs1;
           ResultSet rs2;
           ResultSet rs3;
           
           while((line = reader.readLine()) != null) {
               
               String[] parts = line.split("-->");
               if(parts.length <2)
                   continue;
               String tableName = parts[0].trim();
               String columnsString = parts[1].trim();
               String[] columns = columnsString.split(",");
               
               for(int i = 0; i < columns.length; i++) {
                   
                   try{               
                       String colType = "";                
                       String columnName = columns[i].trim();
                       if(columnName.endsWith("$"))
                           continue;
                       String query3 = "Select " + columnName + " from " + tableName + " where upper(" + columnName + ") <> " + columnName;
                       String query2 = "Select distinct(" + columnName + ") from " + tableName;
                       pstmt.setString(1, tableName);
                       pstmt.setString(2, columnName);
                       rs1 = pstmt.executeQuery();
                       
                       if(rs1.next()) {
                           
                          String dataType = rs1.getString("DATA_TYPE");
                          if(!dataType.contains("VARCHAR"))
                              colType = "Not varchar";
                          else{
                              rs2 = stmt1.executeQuery(query2);
                              if(rs2.next()){ //means that column is not empty
                                  rs3 = stmt2.executeQuery(query3);
                                  if(rs3.next())
                                    colType = "Mixed";
                                  else
                                    colType = "All Caps";
                              } else {
                                  colType = "No data";
                              }
                          }
                       }
                       else
                           colType = "Column not found";
                       
                       writer.write(tableName + "," + columnName + "-->" + colType + "\n");
                       
                   } catch (Exception e) {
                       e.printStackTrace();
                   }
               } //for
           }//while
           writer.close();
           pstmt.close();
           stmt1.close();
           stmt2.close();
           con.commit();
        }
}
