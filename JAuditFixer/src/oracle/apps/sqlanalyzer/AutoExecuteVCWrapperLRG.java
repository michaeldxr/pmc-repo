package oracle.apps.sqlanalyzer;

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


public class AutoExecuteVCWrapperLRG {
  
  String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
  static String statsUname = "codescan";
  static String statsPwd = "codescan";
  
  static String pwd;
  static String series = ""; 
  
  public static void main(String[] args) {             
   
      if (args.length < 2) {
          System.out.println("Need to specify both 1.directory and 2.series");
          System.exit(1);
      }

      pwd = args[0];
      series = args[1];

      AutoExecuteVCWrapperLRG dbWrapper = new AutoExecuteVCWrapperLRG();

    try {         
        System.out.println("Updating DB....");
        dbWrapper.updateDatabase();          
    } catch (Exception e) {
        e.printStackTrace();        
    }
  }  
  
  
  public void updateDatabase() throws Exception {
    
    Connection con = null;
    try{
        String fileName = pwd + "/AutoExecuteVC_LRG.csv";
        
        BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
        String line = fileReader.readLine();

        con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
        con.setAutoCommit(false);
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,SERIES,LABEL) VALUES (?,?,?,?,?,?,?,?,?)");
       
        System.out.println("First delete records from DB...");
        Statement stmt = con.createStatement();
        String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'AutoExecuteVC' and SERIES='"+ series+"'";
         
        stmt.executeUpdate(sql);          
        stmt.close();
        
        if(line == null)  {
            con.commit();  
            return;
        }
            
        System.out.println("Now inserting values...");
        int count = 0;
        while((line = fileReader.readLine()) != null) {
            
          String[] parts = line.split(",");
          if(parts.length < 8) continue;
            
          if (LRGUtil.isDeferred(con, series, parts[3].trim()))
            continue;
            
          pstmt.setString(1, parts[0].trim()); //family
          pstmt.setString(2, parts[1].trim()); //module
          pstmt.setString(3, parts[2].trim()); //product
          pstmt.setString(4, parts[3].trim()); //filename
          pstmt.setString(5, "AutoExecuteVC"); //issue        
          pstmt.setString(6, parts[6].trim()); //subissue        
          pstmt.setString(7,parts[7].trim()); //description
          pstmt.setString(8, parts[4].trim()); //series
          pstmt.setString(9, parts[5].trim()); //label
        //  cstmt.execute();
          pstmt.addBatch();
          count++;
        }
        pstmt.executeBatch();
        con.commit();
        pstmt.close();
        
        System.out.println("There are " + count + " violations for AutoExecuteVC Scan on " + series + "\n");
    }catch(Exception e) {
        e.printStackTrace();
        if(con != null)
            con.rollback();
    } finally{
        if(con != null)
            con.close();
    }
  }
  }
