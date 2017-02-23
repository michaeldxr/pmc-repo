package oracle.apps.seed;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.sqlanalyzer.BindVariableKindsDBWrapper;

public class SeedEndDatesDBWrapper {
    
    String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
    static String statsUname = "codescan";
    static String statsPwd = "codescan";
    static String pwd; 
    static String series = "";
    
    public static void main(String[] args) {             
     
       pwd = args[0];
       series = args[1];

      SeedEndDatesDBWrapper dbWrapper = new SeedEndDatesDBWrapper();

      try {         
          System.out.println("Updating DB....");
          dbWrapper.updateDatabase();          
      } catch (Exception e) {
          e.printStackTrace();        
      }
    }  
    
    
    public void updateDatabase() throws Exception {
      
      Connection con = null;
        String[] labelSeries = null;
        String prevLabel = "";
        String[] parts = null;
      try{
          String fileName = pwd + "/SeedEndDateIncorrect.csv";
          BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
          String line = fileReader.readLine();
               
          //Class.forName("oracle.jdbc.driver.OracleDriver");
          con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
          con.setAutoCommit(false);
          
          PreparedStatement cstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,LABEL,SERIES) " +
                  "VALUES(?,?,?,?,?,?,?,?,?)");
             
            System.out.println("First delete records from DB...");
            Statement stmt = con.createStatement();
            String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'SeedEndDateIncorrect' and SERIES = '" + series + "'" ;
            stmt.executeUpdate(sql);           
            stmt.close();
         
          
          System.out.println("Now inserting values...");
          int count=0;
          while((line = fileReader.readLine()) != null) {
              
            parts = line.split(",");
              if(parts.length < 7) {
                  System.out.println("Skipping: " + line);
                  continue;
              }
              
              if(prevLabel != parts[4].trim()) {
                  labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
                  prevLabel = parts[4].trim();
              }                       
              
            String description = parts[6].trim();
              if(description.length() > 999)
                  description = description.substring(0, 999);
            cstmt.setString(1, parts[0].trim()); //family
            cstmt.setString(2, parts[1].trim()); //module
            cstmt.setString(3, parts[2].trim()); //product
            cstmt.setString(4, parts[3].trim()); //filename 
            cstmt.setString(9, labelSeries[0]); //series
            cstmt.setString(8, labelSeries[1]); //label
            cstmt.setString(5, "SeedEndDateIncorrect"); //issue             
            cstmt.setString(6, parts[5].trim()); //subissue 
            
            cstmt.setString(7,description); //Description
            cstmt.addBatch();     
            count++;
          }
          cstmt.executeBatch();
          con.commit();
          cstmt.close();
          System.out.println("Total count for SeedEndDateIncorrect is "+count +" on series "+series);
      }catch(Exception e){
          e.printStackTrace();
          if(con != null)
             con.rollback();
      }finally {
         if(con != null)
             con.close();
      }
    }  
}
