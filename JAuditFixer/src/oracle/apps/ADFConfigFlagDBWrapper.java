package oracle.apps;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

import oracle.apps.helpers.FamilyModuleHelper;

public class ADFConfigFlagDBWrapper {
    
    String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
    static String statsUname = "codescan";
    static String statsPwd = "codescan";
    static String pwd;
    static String series = "";
    
    public static void main(String[] args) {
        pwd = args[0].trim();
        //pwd="/ade/jenchen_fusionapps_rel7_v1main_vw/fusionapps";
       series = args[1].trim();
        //series = "FUSIONAPPS_11.1.1.5.1_LINUX.X64";

        ADFConfigFlagDBWrapper dbWrapper = new ADFConfigFlagDBWrapper();

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
          String fileName = pwd + "/ADFConfigFlagCheckMissing.csv";
          BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
          String line = fileReader.readLine();
               
          con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
          con.setAutoCommit(false);
          
          PreparedStatement cstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,LABEL,SERIES) " +
                  "VALUES(?,?,?,?,?,?,?,?,?)");
             
            System.out.println("First delete records from DB...");
            Statement stmt = con.createStatement();
            String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'AvoidLoadingTranslatorNotes' and SERIES='" + series +"'";
            stmt.executeUpdate(sql);           
            stmt.close();

          System.out.println("Now inserting values...");
          int count=0;
          while((line = fileReader.readLine()) != null) {
              
            String[] parts = line.split(",");
              if(parts.length < 7) {
                  System.out.println("Skipping: " + line);
                  continue;
              }
              
            String product=parts[2].trim();
            if(product.equals(".ADF"))
                product="";
                            
            cstmt.setString(1, parts[0].trim()); //family
            cstmt.setString(2, parts[1].trim()); //module
            cstmt.setString(3, product); //product
            cstmt.setString(4, parts[3].trim()); //filename 
            cstmt.setString(8, parts[5].trim()); //label
            cstmt.setString(9, parts[4].trim()); //series
            cstmt.setString(5, "AvoidLoadingTranslatorNotes"); //issue          
            cstmt.setString(6, ""); //subissue              
            cstmt.setString(7, parts[6].trim()); //Description
            cstmt.addBatch();     
            count++;
          }
          cstmt.executeBatch();
          con.commit();
          cstmt.close();
          System.out.println("Total count for AvoidLoadingTranslatorNotes is "+count +" on series "+series);
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
