package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateSTS {
  
  String m_centralDb = "jdbc:oracle:thin:@//rws60046rems.us.oracle.com:1573/az1tl999";
  String m_centralUName = "findev";
  String m_centralPwd = "findev";
  
  boolean bDebug = false;
  
  HashMap<String,String> sqlIdText;
  HashMap<String,ArrayList> sqlIdBinds;
  
  static BufferedWriter outputFileWriter;
  BufferedWriter outputFileWriter1;
  BufferedWriter outputFileWriter2;
  
  Pattern selectPattern = Pattern.compile("SELECT\\s+.*?\\s+FROM\\s+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
  Pattern functionPattern = Pattern.compile("\\w+\\.\\w+\\s*\\(.*?\\)", Pattern.DOTALL);
  
  public static void main(String[] args) {
    
    CreateSTS createSTS = new CreateSTS();
    try {
      
      outputFileWriter = new BufferedWriter(new FileWriter("query_errors.txt") );
      outputFileWriter.write("Follwoing queries cannot be run due to errors in resolving bind variables: \n\n");
        
      createSTS.selectQueriesWithAvgOver100();
    //  createSTS.select20QueriesWithMaxExecutions();
      createSTS.selectQueriesWithAvg50To100();
      createSTS.selectQueriesWithAvg10To50();
        
      createSTS.generateOverviewReport();
        
      outputFileWriter.close();
        
    } catch(Exception e) {
      System.out.println("EXCEPTION OCCURED: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  // Select top 20 queries with max number of executions
  public void select20QueriesWithMaxExecutions() throws Exception {    
    
    sqlIdText = new HashMap<String,String>();
    sqlIdBinds = new HashMap<String,ArrayList>();
    
    String overviewQuery = "SELECT * FROM (SELECT sql_id, SUM(executions) AS total_executions FROM overview_stats " +
          "WHERE source = 'ST3B15' AND family = 'fin' GROUP BY sql_id ORDER BY sum(executions) desc) WHERE ROWNUM <= 20";    
    
    outputFileWriter1 = new BufferedWriter(new FileWriter("STS_maxExecutions.sql") );
    outputFileWriter1.write("set define off; \n\n");
    
    outputFileWriter.write("=====================Top 20 Queries with max executions==============================\n\n\n");
    
    outputFileWriter2 = new BufferedWriter(new FileWriter("Queries_maxExecutions.csv") );
    outputFileWriter2.write("SQL_ID, NUM_EXECUTIONS\n");
    
    createSQLSet(overviewQuery);
    createTuningSet();
    outputFileWriter1.close();
    outputFileWriter2.close();
  }
  
  // Select queries with avg_time > 100 seconds
  public void selectQueriesWithAvgOver100() throws Exception {
    
    sqlIdText = new HashMap<String,String>();
    sqlIdBinds = new HashMap<String,ArrayList>();
    
    String overviewQuery = "SELECT sql_id, average_time FROM overview_stats " +
        "WHERE source='ST3B15' AND family='fin' AND average_time > 100";    
    
    outputFileWriter1 = new BufferedWriter(new FileWriter("STS_avgTime100.sql") );
    outputFileWriter1.write("set define off; \n\n");
    
    outputFileWriter.write("=====================Queries taking avg time > 100 sec==============================\n\n\n");
    
    outputFileWriter2 = new BufferedWriter(new FileWriter("Queries_avgTimeOver100.csv") );
    outputFileWriter2.write("SQL_ID, AVG_EXECUTION_TIME\n");
    
    createSQLSet(overviewQuery);
    createTuningSet();
    outputFileWriter1.close();
    outputFileWriter2.close();
    findFunctionCallsInSelect(false);
  }
  
  // Select queries with avg_time > 50 seconds and <= 100 secs
  public void selectQueriesWithAvg50To100() throws Exception {    
    
    sqlIdText = new HashMap<String,String>();
    sqlIdBinds = new HashMap<String,ArrayList>();
    
    String overviewQuery = "SELECT sql_id, average_time FROM overview_stats " +
        "WHERE source='ST3B15' AND family='fin' AND average_time > 50 AND average_time <= 100";     
    
    outputFileWriter1 = new BufferedWriter(new FileWriter("STS_avgTime50To100.sql") );
    outputFileWriter1.write("set define off; \n\n");
    
    outputFileWriter.write("=================Queries taking avg time between 50 and 100 sec=====================\n\n\n");
    
    outputFileWriter2 = new BufferedWriter(new FileWriter("Queries_avgTime50To100.csv") );
    outputFileWriter2.write("SQL_ID, AVG_EXECUTION_TIME\n");
    
    createSQLSet(overviewQuery);
    createTuningSet();
    outputFileWriter1.close();
    outputFileWriter2.close();
    findFunctionCallsInSelect(true);
  }
  
  // Select queries with avg_time >= 10 seconds and <= 50 secs
  public void selectQueriesWithAvg10To50() throws Exception {  
      
    sqlIdText = new HashMap<String,String>();
    sqlIdBinds = new HashMap<String,ArrayList>();
    
    String overviewQuery = "SELECT sql_id, average_time FROM overview_stats " +
        "WHERE source='ST3B15' AND family='fin' AND average_time >= 10 AND average_time <= 50"; 
    
    outputFileWriter1 = new BufferedWriter(new FileWriter("STS_avgTime10To50.sql") );
    outputFileWriter1.write("set define off; \n\n");
    
    outputFileWriter.write("=================Queries taking avg time between 10 and 50 sec=====================\n\n\n");
    
    outputFileWriter2 = new BufferedWriter(new FileWriter("Queries_avgTime10To50.csv") );
    outputFileWriter2.write("SQL_ID, AVG_EXECUTION_TIME\n");
    
    createSQLSet(overviewQuery);
    createTuningSet();
    
    outputFileWriter1.close();
    outputFileWriter2.close(); 
    findFunctionCallsInSelect(true);
  }
  
  public void generateOverviewReport() throws Exception {
    
    BufferedWriter csvWriter = new BufferedWriter(new FileWriter("query_overview_report.csv") );
    csvWriter.write("DATE_START,DATE_END,# DISTINCT SQLS, # SQLS >= 1 sec, # SQLS >= 10 sec, #SQLS >= 100 sec\n");
    
    Connection con = DriverManager.getConnection(m_centralDb, m_centralUName, m_centralPwd);
    Statement stmt = con.createStatement();
    ResultSet rs = stmt.executeQuery("SELECT week_start, week_end, COUNT(DISTINCT sql_id) FROM overview_stats " +
        "WHERE source = 'ST3B15' AND family = 'fin' GROUP BY week_start, week_end");
    
    HashMap<String, ArrayList> queryOverview = new HashMap<String, ArrayList>();
    
    while(rs.next()) {
      
      ArrayList<String> row = new ArrayList<String>();
      row.add(rs.getDate(2).toString());
      row.add(rs.getString(3));
        
      queryOverview.put(rs.getDate(1).toString(), row);
    }
    
    rs = stmt.executeQuery("SELECT week_start, week_end, COUNT(DISTINCT sql_id) FROM overview_stats " +
        "WHERE source = 'ST3B15' AND family = 'fin' AND average_time >= 1 GROUP BY week_start, week_end");
    
    while(rs.next()) {
      String week_start = rs.getDate(1).toString();
      ArrayList row = queryOverview.get(week_start);
      row.add(rs.getString(3));
    }
    
    rs = stmt.executeQuery("SELECT week_start, week_end, COUNT(DISTINCT sql_id) FROM overview_stats " +
        "WHERE source = 'ST3B15' AND family = 'fin' AND average_time >= 10 GROUP BY week_start, week_end");
    
    while(rs.next()) {
      String week_start = rs.getDate(1).toString();
      ArrayList row = queryOverview.get(week_start);
      row.add(rs.getString(3));
    }
    
    rs = stmt.executeQuery("SELECT week_start, week_end, COUNT(DISTINCT sql_id) FROM overview_stats " +
        "WHERE source = 'ST3B15' AND family = 'fin' AND average_time >= 100 GROUP BY week_start, week_end");
    
    while(rs.next()) {
      String week_start = rs.getDate(1).toString();
      ArrayList row = queryOverview.get(week_start);
      row.add(rs.getString(3));
    }
    
    stmt.close();
    con.close();
    
    for(Iterator i = queryOverview.keySet().iterator(); i.hasNext();){
      String week_start = (String)i.next();
      ArrayList row = queryOverview.get(week_start);
        
      csvWriter.write(week_start + "," + row.get(0) + "," + row.get(1) + "," + row.get(2) + "," + row.get(3) + "," + row.get(4) + "\n");
    }
    
    csvWriter.close();
  }
  
  public void createSQLSet(String overviewQuery) throws Exception {
      
    Connection con = null;
    Statement stmt1 = null;
    PreparedStatement stmt2 = null;
    PreparedStatement stmt3 = null;
    Statement stmt4 = null;
    ResultSet rs1 = null;
    ResultSet rs2 = null;
    ResultSet rs3 = null;
    ResultSet rs4 = null;    
    
    try {        
      con = DriverManager.getConnection(m_centralDb, m_centralUName, m_centralPwd);
      stmt1 = con.createStatement(); 
      stmt2 = con.prepareStatement("SELECT sql_text FROM local_sqlid_text where sql_id = ?"); 
      stmt3 = con.prepareStatement("SELECT BIND_DATA FROM local_report_sqls WHERE sql_id =?"); 
      stmt4 = con.createStatement();
      rs1 = stmt1.executeQuery(overviewQuery);  
      
      String bindData = null;      
    
      while(rs1.next()) {
        
        String sql_id =rs1.getString("SQL_ID");     
        String col2 = rs1.getString(2);
          
        if(sqlIdText.containsKey(sql_id)) continue;
          
        outputFileWriter2.write(sql_id + "," + col2 + "\n");
          
        stmt2.setString(1,sql_id);          
        rs2 = stmt2.executeQuery();
          
        if(rs2.next()) {
            
          sqlIdText.put(sql_id, rs2.getString("SQL_TEXT")); 
          stmt3.setString(1,sql_id);     
          rs3=stmt3.executeQuery();
          
          if(rs3.next()) {
              
            bindData = rs3.getString("BIND_DATA");  
            
            if(bindData != null){
                
              String bindQuery = getQuery_resolveBinds(bindData);
              rs4=stmt4.executeQuery(bindQuery);
              ArrayList<String> bindVars = new ArrayList<String>();
              
              while(rs4.next())
                bindVars.add(rs4.getString("value_string"));  
              
              sqlIdBinds.put(sql_id,bindVars);              
            }
          }           
        }
      }
    } catch(Exception e) {
      System.out.println("EXCEPTION OCCURED: " + e.getMessage());
      e.printStackTrace();
    }
    finally {
      if(stmt1 != null) stmt1.close();
      if(stmt2 != null) stmt2.close();
      if(stmt3 != null) stmt3.close();
      if(stmt4 != null) stmt4.close();
      if(con != null) con.close();
    }
    System.out.println("\n# of distinct SQLs: " + sqlIdText.size() + "\n");
  }
  
  
  public void createTuningSet() throws Exception {    
        
    Pattern bindVarPattern = Pattern.compile("(:[A-Za-z0-9_]+)");
    Matcher m;
    
    Set<String> sqlIds = sqlIdText.keySet();
    Iterator i = sqlIds.iterator();
    boolean error = false;
    
    while(i.hasNext()) {

      error = false;
      String sqlId = (String)i.next();
      String sqlText = sqlIdText.get(sqlId);           
     
      ArrayList<String> bindValues = sqlIdBinds.get(sqlId);      
      if(bDebug) 
          System.out.println(bindValues);
        
      m = bindVarPattern.matcher(sqlText);
      ArrayList<String> bindVars = new ArrayList<String>();
      
      while(m.find()){
          String bindVar = m.group();
          if(!bindVar.equals(":MI") && !bindVar.equals(":SS")) bindVars.add(bindVar);
          sqlText.replaceFirst(bindVar, "?");
      }
        
      if(bDebug) 
          System.out.println(bindVars);     
        
      HashMap<String,String> bindVarValues = new HashMap<String,String>();
        
      for(int j =0; j < bindVars.size(); j++) {
          
          if(bindVars == null || bindValues ==null) {
              error = true;
              System.out.println(sqlId + " --> BindVars or BindValues is null\n");
              break;
          }
          if(bindVars.size() != bindValues.size()) {
            System.out.println(sqlId +  "--> +BindVars and BindValues do not match up\n");
            error = true;
            break;
          }
          
          String bindVar = bindVars.get(j);
          String bindValueNew = bindValues.get(j);
          
          if(!bindVarValues.containsKey(bindVar))
            bindVarValues.put(bindVar,bindValueNew);
          else {
            String bindValueOld = bindVarValues.get(bindVar);
            if(bindValueOld == null || bindValueOld.trim().equals("")) 
              bindVarValues.put(bindVar,bindValueNew);
          }
      }
      
      if(bDebug) {
          System.out.println(bindVarValues);
          System.out.println("--------------------------------------------------\n");
      }
        
      if(error) {
        outputFileWriter.write("\n\n" + sqlId + " ==> " + sqlText + "\n\n");
        if(bindVars != null) outputFileWriter.write(bindVars.toString() + "\n");
        if(bindValues != null) outputFileWriter.write(bindValues.toString() + "\n");
      } else {
        
          sqlText = sqlIdText.get(sqlId); 
          for(Iterator it = bindVarValues.keySet().iterator(); it.hasNext();) {
            String bindVar = (String)it.next();
            String bindValue = bindVarValues.get(bindVar);
            if(bindValue != null)
                sqlText = sqlText.replace(bindVar, "'" + bindValue + "'");
              else sqlText = sqlText.replace(bindVar,"null");
          }
          outputFileWriter1.write(sqlText + ";\n\n");
      }      
    }
  }
  
  public String getQuery_resolveBinds(String bindData) {
    StringBuffer sb = new StringBuffer();
    sb.append("SELECT position, value_string FROM ");
    sb.append(String.format("table(dbms_sqltune.extract_binds('%s'))",bindData));
    sb.append(" ORDER BY POSITION");
    return sb.toString();
  }
  
  private void findFunctionCallsInSelect(boolean append) throws Exception {
    BufferedWriter writer = new BufferedWriter(new FileWriter("ScalarSubqueryPattern.txt",append));
    
    for(Iterator it = sqlIdText.keySet().iterator(); it.hasNext();) {
      
      boolean write = false;
      String sqlId = (String)it.next();
      String sqlText = sqlIdText.get(sqlId);
     
      //writer.write("Function calls in Select clause:\n");
      Matcher m = selectPattern.matcher(sqlText);
      
        while(m.find()){
          String selectPart = m.group();
          //System.out.println("Select clase:" + selectPart);
          Matcher m1 = functionPattern.matcher(selectPart);
            while(m1.find()){
                String function = m1.group().toUpperCase(); //&& !m1.group().contains("FIRST_ROWS") && !m1.group().contains("COUNT(")
                /*if(function.contains("FIRST_ROWS") || function.contains("COUNT(") 
                   || function.contains("DECODE") || function.contains("TO_") 
                   || function.contains("NVL") || function.contains("SUBSTR")
                    || function.contains("TRUNC"))
                    continue;*/
                write = true;
                writer.write(m1.group() + "\n");
            }
        } 
        if(write){
          writer.write("\n" +sqlId + " --> " + sqlText + "...\n\n");
          writer.write("-------------------------------------------------------------------------------------\n\n");
        }
    }
    writer.close();
  }
}
