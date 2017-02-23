package oracle.apps.serialization;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

public class SerializationDBWrapper {
    
  //String statsDB = "jdbc:oracle:thin:@//rws65057fwks.us.oracle.com:1521/Albowicz";
  String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:albowicz";
  static String statsUname = "findev";
  static String statsPwd = "findev";
  static String pwd;
  static String clearDBFamily = null;
  static String codeLine = "RUP";  
  
  public static void main(String[] args) {             
   
     pwd = args[0];
      //pwd = "/scratch/sudgupta/view_storage/sudgupta_fatools_label_view/fatools/opensource/jauditFixScripts/JAuditFixer";
     
     String env = args[1];
     // String env = "prod";
     if(env != null && env.equals("dev")){
       statsUname = "findev";
       statsPwd = "findev";
     }
     else {
       statsUname = "fintech";
       statsPwd = "fintech";
     }
     
     clearDBFamily = args[2]; //Family name used to delete results from DB
    //clearDBFamily = "FIN";
      
      if (pwd == null || pwd.trim().equals("")) {
          System.out.println("Present Working directory has to be specified");
          System.exit(1);
      }
      if(args.length > 3)
          codeLine = args[3].trim().toUpperCase();

    SerializationDBWrapper dbWrapper = new SerializationDBWrapper();

    try {         
        System.out.println("Updating DB....");
        dbWrapper.updateDatabase();          
    } catch (Exception e) {
        e.printStackTrace();        
    }
  }  
  
  
  public void updateDatabase() throws Exception {
    
    String fileName = pwd+ "/serialization_scan.csv";
    String userHomeDir = System.getProperty("user.home");
                                                     
    if(clearDBFamily != null && clearDBFamily.trim().equals("ALL"))
        fileName = userHomeDir + "/serialization_scan_final.csv";
    
    BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
    String line = fileReader.readLine();
   // if(line == null) return;
    
    //Class.forName("oracle.jdbc.driver.OracleDriver");
    Connection con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
    con.setAutoCommit(false);
    String tableName = "";
       
    if(clearDBFamily != null) {
      System.out.println("First delete records from DB...");
      Statement stmt = con.createStatement();
      String sql = "";
      
        if(codeLine.equals("RUP"))
            tableName = "CODESCAN_RESULTS_RUP2";
        else if(codeLine.equals("RUP4"))
            tableName = "CODESCAN_RESULTS_RUP4";
        else if(codeLine.equals("V2"))
            tableName = "CODESCAN_RESULTS_V2";
        else if(codeLine.equals("V2DEV"))
            tableName = "CODESCAN_RESULTS_V2DEV";
        else if(codeLine.equals("V12"))
            tableName = "CODESCAN_RESULTS_V12";
        
        if(clearDBFamily.equals("ATGPF"))
            tableName = "CODESCAN_RESULTS_ATGPF";
        
      if(clearDBFamily.trim().equals("ALL"))
          sql = "DELETE FROM " + tableName +" WHERE ISSUETYPE = 'Serialization'";
      else
          sql = "DELETE FROM " + tableName  + " WHERE ISSUETYPE = 'Serialization' and FAMILY = " + "'" + clearDBFamily.trim() + "'";
      //This should automatically delete the corresponding rows from the child table as FK has been specified with on delete cascade
     
      stmt.executeUpdate(sql);  
      con.commit();          
      stmt.close();
    }
    
      if(line == null) return;
    
      CallableStatement cstmt = null;
      String tableName2 = "";
      
      if(tableName.equals("CODESCAN_RESULTS_RUP2")) {
            cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT_RUP2(?,?,?,?,?,?,?,?) }");
            tableName2 = "SERIALIZATION_SCAN";
      }    
      else if(tableName.equals("CODESCAN_RESULTS_RUP4")) {
            cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT_RUP4(?,?,?,?,?,?,?,?) }");
            tableName2 = "SERIALIZATION_SCAN_RUP4";
      }    
      else if(tableName.equals("CODESCAN_RESULTS_V2")) {
            cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT_V2(?,?,?,?,?,?,?,?) }");
            tableName2 = "SERIALIZATION_SCAN_V2";
      }
      else if(tableName.equals("CODESCAN_RESULTS_V2DEV")) {
            cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT_V2DEV(?,?,?,?,?,?,?,?) }");
            tableName2 = "SERIALIZATION_SCAN_V2DEV";
      }
      else if(tableName.equals("CODESCAN_RESULTS_V12")) {
            cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT_V12(?,?,?,?,?,?,?,?) }");
            tableName2 = "SERIALIZATION_SCAN_V12";
      }
      else if(tableName.equals("CODESCAN_RESULTS_ATGPF")) {
            cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT_ATGPF(?,?,?,?,?,?,?,?) }");
            tableName2 = "SERIALIZATION_SCAN_ATGPF";
      }
        
      cstmt.registerOutParameter(1, Types.INTEGER);
    
    PreparedStatement pstmt = con.prepareStatement("INSERT INTO " + tableName2 + "(SEQ_ID,LINENUM,NODENAME,NODEID,COMMENT1,COMMENT2) " +
        "VALUES(?,?,?,?,?,?)");
    System.out.println("Now inserting values...");
    while((line = fileReader.readLine()) != null) {
        
      String[] parts = line.split(",");
      if(parts.length < 11) continue;
      cstmt.setString(2, parts[0].trim()); //family
      cstmt.setString(3, parts[1].trim()); //module
      cstmt.setString(4, parts[2].trim()); //product
      cstmt.setString(5, parts[3].trim()); //filename
      cstmt.setString(6, "Serialization"); //issue
      cstmt.setString(7, parts[9].trim()); //subissue
        
      cstmt.setString(8,""); //description
      cstmt.setString(9,parts[10].trim()); //label
  
      cstmt.execute();
      con.commit();
        
      int seq = cstmt.getInt(1);
      pstmt.setInt(1,seq);
      pstmt.setString(2,parts[4]);
      pstmt.setString(3,parts[5]);
      pstmt.setString(4,parts[6]);  
      pstmt.setString(5,parts[7]);  
      pstmt.setString(6,parts[8]);
      pstmt.execute();
      con.commit();
    }
    
    cstmt.close();
    pstmt.close();
    con.close();
  }
}
