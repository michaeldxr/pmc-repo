package oracle.apps.rup2scans;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class FndIncidentMessages {
   
    static String angrybirdsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:albowicz";
    static String angrybirdsUname = "fintech";
    static String angrybirdsPwd = "fintech";
    
    static String fintechDB = "jdbc:oracle:thin:@adc60171fems.us.oracle.com:1540:ffintech";
    static String fintechUname = "fusion";
    static String fintechPwd = "fusion";
    
    public static void main(String[] args) throws Exception {
        
        Connection con1 = null;
        Connection con2 = null;
        Statement stmt1 = null;
        Statement stmt2 = null;
        PreparedStatement pstmt1 = null;
        PreparedStatement pstmt2 = null;
        ResultSet rs1 = null;
        ResultSet rs2 = null;
        
        try {       
            
          con1 = DriverManager.getConnection(fintechDB, fintechUname, fintechPwd);
          stmt1 = con1.createStatement(); 
          rs1 = stmt1.executeQuery("select APPLICATION_SHORT_NAME,MESSAGE_NAME,MESSAGE_TEXT from " +
              "FND_MESSAGES, FND_APPLICATION where FND_MESSAGES.APPLICATION_ID = FND_APPLICATION.APPLICATION_ID " +
              "and loggable_alertable = 'Y' and type = 'ERROR' and MESSAGE_SEVERITY = 'HIGH' and MESSAGE_CATEGORY IS NOT NULL");
           
          con2 =  DriverManager.getConnection(angrybirdsDB, angrybirdsUname, angrybirdsPwd);
          con2.setAutoCommit(false);
          stmt2 = con2.createStatement();
          stmt2.executeUpdate("DELETE FROM codescan_results2 WHERE ISSUETYPE = 'FndIncidentMessage'");
            
          pstmt1 = con2.prepareStatement("INSERT INTO CODESCAN_RESULTS2(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,LABEL) " +
                               "VALUES(?,?,?,?,?,?,?,?)");
          pstmt2 = con2.prepareStatement("Select f.code family_name FROM products p, families f WHERE p.familyid=f.familyid and p.code = ?");
            
          while(rs1.next()) {
            
            String product =rs1.getString("APPLICATION_SHORT_NAME");     
            String messageName = rs1.getString("MESSAGE_NAME");
            String messageText = rs1.getString("MESSAGE_TEXT");
            String family = " ";
              
            pstmt2.setString(1, product);
            rs2 = pstmt2.executeQuery();
              
            if(rs2.next())
              family = rs2.getString(1);
              
              pstmt1.setString(1, family); //family
              pstmt1.setString(2, ""); //module
              pstmt1.setString(3, product); //product
              pstmt1.setString(4, ""); //filename 
              pstmt1.setString(5, "FndIncidentMessage"); //issue     
              pstmt1.setString(6, messageName); //subissue   -> method Name          
              pstmt1.setString(7, messageText); //Description  -> RSI variable 
              
              pstmt1.setString(8, ""); //label
              pstmt1.execute();               
          }
            
        } catch(Exception ex){
          ex.printStackTrace();
          con2.rollback();
        } finally {
            con1.commit();
            con2.commit();
            rs1.close();
            rs2.close();
            stmt1.close();
            stmt2.close();
            pstmt1.close();
            pstmt2.close();
            con1.close();
            con2.close();
        }
        
    }
    
    
}
