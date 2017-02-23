package oracle.apps.rowCountAnalyzer;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;

public class SelectCountDBWrapperLRG {

  String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
  static String statsUname = "codescan";
  static String statsPwd = "codescan";
  static String pwd;
  static Connection con;
  static String series = "";  
  
  public static void main(String[] args) throws Exception{             
   
      if (args.length < 2) {
          System.out.println("Need to specify both 1.directory and 2.series");
          System.exit(1);
      }

      pwd = args[0];
      series = args[1];
      
    SelectCountDBWrapperLRG dbWrapper = new SelectCountDBWrapperLRG();

    try {         
        System.out.println("Updating DB....");
        dbWrapper.updateDatabase();          
    } catch (Exception e) {
        e.printStackTrace();        
        if(con != null)
            con.rollback();
      } finally {
            con.close();
        }
  }  
  
  
  public void updateDatabase() throws Exception {
    
    String fileName = pwd + "/SelectCountQuery_LRG.csv";
    
    BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
    String line = "";
        
    con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
    con.setAutoCommit(false);
    CallableStatement cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
       
    System.out.println("First delete records from DB...");
    Statement stmt = con.createStatement();
    String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'SelectCountQuery' and SERIES='" + series+"'";
     
      stmt.executeUpdate(sql);         
      stmt.close();
    
    if(line == null) {
        con.commit();  
        return;
    }
      
    cstmt.registerOutParameter(1, Types.INTEGER);    
    PreparedStatement pstmt = con.prepareStatement("INSERT INTO SELECT_COUNT_SCAN(SEQ_ID,FILE_TYPE, USAGE_TYPE," +
          "ROWCOUNT_USAGE,LINE) VALUES(?,?,?,?,?)");
    
    System.out.println("Now inserting values...");
    String[] labelSeries = null;
    String prevLabel = "";
    int count = 0;
    while((line = fileReader.readLine()) != null) {
        
      String[] parts = line.split(",");
      if(parts.length < 9) continue;
        
      String usageType = parts[8].trim();
      if(!usageType.equals("iteration") && !usageType.equals("var unused") && !usageType.equals("existence check"))
          continue;
        
      if (LRGUtil.isDeferred(con, series, parts[3].trim()))
            continue;
        
      String rowCountType = parts[7];
      String filePath = parts[3].trim();
        
        if(rowCountType != null && rowCountType.trim().equals("rowCount")){
            if(usageType.equals("iteration"))
                continue;
            if(usageType.equals("existence check") && filePath.contains("ui/"))
                continue;
        }
        
        if(prevLabel != parts[4].trim()) {
            labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
            prevLabel = parts[4].trim();
        }
        
      cstmt.setString(2, parts[0].trim()); //family
      cstmt.setString(3, parts[1].trim()); //module
      cstmt.setString(4, parts[2].trim()); //product
      cstmt.setString(5, filePath); //filename
      cstmt.setString(9, labelSeries[0]); //series
      cstmt.setString(10, labelSeries[1]); //label
      cstmt.setString(6, "SelectCountQuery"); //issue
        
      cstmt.setString(7, ""); //subissue        
      cstmt.setString(8,parts[6].trim() + ": " + parts[5].trim()); //Description - line #: line
      cstmt.execute();
        
      int seq = cstmt.getInt(1);
      pstmt.setInt(1,seq); 
      pstmt.setString(2,getFileType(parts[3].trim())); //view_attr
      pstmt.setString(3,parts[8]); //usage_type
      pstmt.setString(4,rowCountType);  //rowcount usage
      pstmt.setString(5,parts[6].trim() + ": " + parts[5].trim()); //Description
      pstmt.execute();    
       count++; 
    }    
    con.commit();
    cstmt.close();
    pstmt.close();
    System.out.println("Total count for SelectCountQuery is "+count +" on series "+series);
  }
  
  private String getFileType(String fileName) {
      String[] parts = fileName.trim().split("/");
      String f = parts[parts.length-1];
      
      parts = f.split("\\.");
      if(parts.length < 2)
         return "";
      else
         return parts[1];
  }
}
