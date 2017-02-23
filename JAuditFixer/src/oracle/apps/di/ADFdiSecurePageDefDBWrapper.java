package oracle.apps.di;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

public class ADFdiSecurePageDefDBWrapper {
    
    String statsDB = "jdbc:oracle:thin:@angrybirds.oracle.com:1521:codescan";
    static String statsUname = "codescan";
    static String statsPwd = "codescan";
    static String pwd;
    static String series = "";
    
    public static void main(String[] args) {
        update(args);
    }
    
    public static void update(String[] args){
      pwd = args[0].trim();
      series = args[1].trim();

      ADFdiSecurePageDefDBWrapper dbWrapper = new ADFdiSecurePageDefDBWrapper();

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
          String fileName = pwd + "/AdfDiSecurePageDef.csv";
          BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
          String line = fileReader.readLine();
               
          con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
          con.setAutoCommit(false);
          
          PreparedStatement cstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,LABEL,SERIES) " +
                  "VALUES(?,?,?,?,?,?,?,?,?)");
             
            System.out.println("First delete records from DB...");
            Statement stmt = con.createStatement();
            String sql = "DELETE FROM CODESCAN_RESULTS_P WHERE ISSUETYPE = 'AdfDiSecurePageDef' and SERIES='" + series +"'";
            stmt.executeUpdate(sql);           
            stmt.close();

          System.out.println("Now inserting values...");
          int count=0;
          String description = "ADFdi worksheet pagedef must be included in application's jazn-data.xml as a <resource> and <member-resource>";
          while((line = fileReader.readLine()) != null) {
              
            String[] parts = line.split(",");
              if(parts.length < 9) {
                  System.out.println("Skipping: " + line);
                  continue;
              }          
                            
            cstmt.setString(1, parts[0].trim()); //family
            cstmt.setString(2, parts[1].trim()); //module
            cstmt.setString(3, parts[2].trim()); //product
            cstmt.setString(4, parts[3].trim()); //filename (SDO Interface)
            cstmt.setString(8, parts[5].trim()); //label
            cstmt.setString(9, parts[4].trim()); //series
            cstmt.setString(5, "AdfDiSecurePageDef"); //issue          
            cstmt.setString(6, parts[6].trim()); //subissue  - pagedef file name            
            cstmt.setString(7,description); //Description
            cstmt.addBatch();     
            count++;
          }
          cstmt.executeBatch();
          con.commit();
          cstmt.close();
          System.out.println("Total count for AdfDiSecurePageDef is "+count +" on series "+series);
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
