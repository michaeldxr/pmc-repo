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

public class BindVariableKindsDBWrapper {
    String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
    static String statsUname = "codescan";
    static String statsPwd = "codescan";
    static String pwd;
    static String series = "";
    
    public static void main(String[] args) {             
     
       pwd = args[0];
       series = args[1];

      BindVariableKindsDBWrapper dbWrapper = new BindVariableKindsDBWrapper();

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
          String fileName = pwd + "/BindVarKindMismatch.csv";
         
          BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
          String line = fileReader.readLine();
               
          //Class.forName("oracle.jdbc.driver.OracleDriver");
          con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
          con.setAutoCommit(false);
          
          PreparedStatement cstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,LABEL,SERIES) " +
                  "VALUES(?,?,?,?,?,?,?,?,?)");
             
            System.out.println("First delete records from DB...");
            Statement stmt = con.createStatement();
            String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'BindVarKindMismatch' and SERIES = '" + series + "'" ;
            
            stmt.executeUpdate(sql);           
            stmt.close();    
         
          System.out.println("Now inserting values...");
          int count = 0;
          while((line = fileReader.readLine()) != null) {
              
            String[] parts = line.split(",");
              if(parts.length < 9) {
                  System.out.println("Skipping: " + line);
                  continue;
              }
              
              String description = "";
              String reference = parts[8].trim();
              String kind = parts[7].trim();
              
              if(reference.equals("none") && kind.equals("where"))
                  description = "BindVariable is not referenced in the VO. Safest to change its kind to viewcriteria. ";
              if(reference.equals("vci") && !kind.equals("viewcriteria"))
                  description = "BindVariable is only referenced in VC. Change the kind to viewcriteria. ";
              if((reference.equals("where") || reference.equals("both")) && !kind.equals("where"))
                  description = "Make the \"kind\" of BindVariable \"where\" as it is referenced " +
                      "in the SQL Query. ";
              
              if(parts.length > 10)
                  description += parts[10].trim();      
              
              if (LRGUtil.isDeferred(con, series, parts[3].trim()))
                continue;
              
            cstmt.setString(1, parts[0].trim()); //family
            cstmt.setString(2, parts[1].trim()); //module
            cstmt.setString(3, parts[2].trim()); //product
            cstmt.setString(4, parts[3].trim()); //filename 
            cstmt.setString(8, parts[5].trim()); //label
            cstmt.setString(9, parts[4].trim()); //series
            cstmt.setString(5, "BindVarKindMismatch"); //issue     
                
            cstmt.setString(6, parts[6].trim() + ": " + kind); //subissue              
            cstmt.setString(7,description); //Description
            cstmt.addBatch();     
            count++;
          }
          cstmt.executeBatch();
          con.commit();
          cstmt.close();
          System.out.println("Total count for BindVarKindMismatch is "+count +" on series "+series);
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
