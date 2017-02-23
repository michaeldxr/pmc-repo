package oracle.apps.UICodeScanner;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.HashMap;
import java.util.HashSet;

import oracle.apps.utility.LRGUtil;

public class PartialTriggerPrototypeDBUpdater {
   
    static String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
    static String statsUname = "codescan";
    static String statsPwd = "codescan";
    static String pwd;
    static String series = "";
    
    public static void main(String[] args) {

        if(args.length != 1 || args[0] == null ||args[0].isEmpty()){
            System.out.println("USAGE: runUnusedPartialTrigger <series>");
            System.exit(1);
        }
        series = args[0];
      
        PartialTriggerPrototypeDBUpdater dbWrapper = new PartialTriggerPrototypeDBUpdater();

        try {
           System.out.println("Updating DB....");
           dbWrapper.updateDatabase1();   
           dbWrapper.updateDatabase2();  
            
        } catch (Exception e) {
           e.printStackTrace();        
        }
    }
    
    
    public void updateDatabase1() throws Exception {
      
      Connection con = null;
      try{
          String fileName = "UnusedAutoSubmit.csv";
         
          BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
          String line = fileReader.readLine();
               
          //Class.forName("oracle.jdbc.driver.OracleDriver");
          con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
          con.setAutoCommit(false);
          
          PreparedStatement cstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS_P(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,LABEL,SERIES) " +
                  "VALUES(?,?,?,?,?,?,?,?,?)");
             
            System.out.println("First delete records from DB...");
            Statement stmt = con.createStatement();
            String sql = "DELETE FROM CODESCAN_RESULTS_P WHERE ISSUETYPE = 'UnusedAutoSubmit' and SERIES = '" + series + "'" ;
            
            stmt.executeUpdate(sql);           
            stmt.close();    
         
          System.out.println("Now inserting values...");
          int count = 0;
          while((line = fileReader.readLine()) != null) {
              
            String[] parts = line.split(",");
              if(parts.length < 8) {
                  System.out.println("Skipping: " + line);
                  continue;
              }
              
            cstmt.setString(1, parts[0].trim()); //family
            cstmt.setString(2, parts[1].trim()); //module
            cstmt.setString(3, parts[2].trim()); //product
            cstmt.setString(4, parts[3].trim()); //filename 
            cstmt.setString(8, parts[5].trim()); //label
            cstmt.setString(9, parts[4].trim()); //series
            cstmt.setString(5, "UnusedAutoSubmit"); //issue     
                
            cstmt.setString(6, parts[7].trim() + " (" + parts[6].trim() + ")"); //subissue              
            cstmt.setString(7,"Id does not appear in partialTriggers list for any other node. " +
                "autoSubmit should be set to false."); //Description
            cstmt.addBatch();     
            count++;
          }
          cstmt.executeBatch();
          con.commit();
          cstmt.close();
          System.out.println("Total count for UnusedAutoSubmit is "+count +" on series "+series);
      }catch(Exception e){
          e.printStackTrace();
          if(con != null)
             con.rollback();
      }finally {
         if(con != null)
             con.close();
      }
    }  
    
    public void updateDatabase2() throws Exception {
      
      Connection con = null;
      try{
          String fileName = "NonExistantPartialTriggers.csv";
         
          BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
          String line = fileReader.readLine();
               
          //Class.forName("oracle.jdbc.driver.OracleDriver");
          con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
          con.setAutoCommit(false);
          
          PreparedStatement cstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS_P(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,LABEL,SERIES) " +
                  "VALUES(?,?,?,?,?,?,?,?,?)");
             
            System.out.println("First delete records from DB...");
            Statement stmt = con.createStatement();
            String sql = "DELETE FROM CODESCAN_RESULTS_P WHERE ISSUETYPE = 'NonExistentPartialTriggers' and SERIES = '" + series + "'" ;
            
            stmt.executeUpdate(sql);           
            stmt.close();    
         
          System.out.println("Now inserting values...");
          int count = 0;
          while((line = fileReader.readLine()) != null) {
              
            String[] parts = line.split(",");
              if(parts.length < 8) {
                  System.out.println("Skipping: " + line);
                  continue;
              }
              
            cstmt.setString(1, parts[0].trim()); //family
            cstmt.setString(2, parts[1].trim()); //module
            cstmt.setString(3, parts[2].trim()); //product
            cstmt.setString(4, parts[3].trim()); //filename 
            cstmt.setString(8, parts[5].trim()); //label
            cstmt.setString(9, parts[4].trim()); //series
            cstmt.setString(5, "NonExistentPartialTriggers"); //issue     
                
            cstmt.setString(6, parts[6].trim() + " (" + parts[7].trim() + ")"); //subissue              
            cstmt.setString(7,"Partial Trigger Id not found on the page."); //Description
            cstmt.addBatch();     
            count++;
          }
          cstmt.executeBatch();
          con.commit();
          cstmt.close();
          System.out.println("Total count for NonExistentPartialTriggers is "+count +" on series "+series);
      }catch(Exception e){
          e.printStackTrace();
          if(con != null)
             con.rollback();
      }finally {
         if(con != null)
             con.close();
      }
    }  
    
    public static void getExemptionList(String series, HashMap<String,HashSet<String>> exemptions) throws SQLException {
      Connection con = null;
      
      con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
      
      PreparedStatement cstmt = con.prepareStatement("SELECT PAGE, ID FROM EXEMPTIONS WHERE SERIES = ?");
      cstmt.setString(1, series);
      
      ResultSet rs = cstmt.executeQuery();
      String page = null;
      String id = null;
        
      while(rs.next()){
        page = rs.getString("PAGE");
        id = rs.getString("ID");
        
        HashSet<String> value = exemptions.get(page);
        if(value == null){
          value = new HashSet<String>();
          value.add(id);
          exemptions.put(page, value);
        }else{
          value.add(id);
        }
      }    
    }
}
