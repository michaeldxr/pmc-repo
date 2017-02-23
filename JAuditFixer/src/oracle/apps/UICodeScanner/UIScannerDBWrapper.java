package oracle.apps.UICodeScanner;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

import oracle.apps.helpers.FamilyModuleHelper;

public class UIScannerDBWrapper {
  String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
  static String statsUname = "codescan";
  static String statsPwd = "codescan";
  static String pwd;
  static String clearDBFamily = null;
  static Connection con;  
  static String codeLine = "V12";
  static String series = "";
  
  public static void main(String[] args) throws Exception {             
   
     pwd = args[0];
     //pwd = "/scratch/sudgupta";     
     String env = args[1];     
     clearDBFamily = args[2]; //Family name used to delete results from DB
    // clearDBFamily = "ALL";
      
      if (pwd == null || pwd.trim().equals("")) {
          System.out.println("Present Working directory has to be specified");
          System.exit(1);
      }
      
      if(args.length > 3)
          codeLine = args[3].trim().toUpperCase();  
      series = FamilyModuleHelper.getSeries(codeLine);
      
    UIScannerDBWrapper dbWrapper = new UIScannerDBWrapper();

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
    
    String fileName = pwd + "/ui_scan.csv";
    String userHomeDir = System.getProperty("user.home");
    if(clearDBFamily != null && clearDBFamily.trim().equals("ALL"))
        fileName = userHomeDir + "/ui_scan_final.csv";
    BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
    String line = fileReader.readLine();
   // if(line == null) return;
    
    //Class.forName("oracle.jdbc.driver.OracleDriver");
    con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
    con.setAutoCommit(false);
    
    String tableName = "CODESCAN_RESULTS_P";
       
    if(clearDBFamily != null) {
      System.out.println("First delete records from DB...");
      Statement stmt = con.createStatement();
      String sql = "";     
        
      if(clearDBFamily.trim().equals("ALL"))
          sql = "DELETE FROM " + tableName + " WHERE SUB_ISSUE = 'UI Issue' and series in " + series;
      else 
          sql = "DELETE FROM " + tableName + " WHERE SUB_ISSUE = 'UI Issue' and FAMILY = " + "'" + clearDBFamily.trim() 
                + "' and SERIES IN " + series;
      //This should automatically delete the corresponding rows from the child table as FK has been specified with on delete cascade
     
      stmt.executeUpdate(sql);   
      stmt.close();
    }
    
      if(line == null)  {
          con.commit();  
          return;
      }      
    
    PreparedStatement pstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS_P(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,SERIES,LABEL) " +
          "VALUES(?,?,?,?,?,?,?,?,?)");
    
    System.out.println("Now inserting values...");
    while((line = fileReader.readLine()) != null) {
        
      String[] parts = line.split(",");
      if(parts.length < 9) continue;
        
      String issue = parts[5].trim();
      String description = parts[6].trim() + "(" + parts[7].trim() + "): " + parts[8].trim();
      String[] labelSeries = null;
        String prevLabel = "";
        
      if(issue.equals("Padding Issue")){
          
          if(prevLabel != parts[4].trim()) {
              labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
              prevLabel = parts[4].trim();
//              Statement stmt = con.createStatement();
//              stmt.executeUpdate("UPDATE SCAN_LABELS set latest_label = '" + labelSeries[1] + "'" +
//                  " WHERE issuetype = 'Padding Issue' AND series = '" + labelSeries[0] + "'");
//              stmt.close();
          }
          
          pstmt.setString(1,parts[0]); //family
          pstmt.setString(2,parts[1]); //module
          pstmt.setString(3,parts[2]);  //product
          pstmt.setString(4,parts[3]); //filename
          pstmt.setString(5,issue); //issue
          pstmt.setString(6,"UI Issue"); //sub issue
          pstmt.setString(7,description); //Description
          pstmt.setString(8,labelSeries[0]); //series
          pstmt.setString(9,labelSeries[1]); //label
          
          pstmt.execute();
      }
    }
    con.commit();
    pstmt.close();
  }
  }
