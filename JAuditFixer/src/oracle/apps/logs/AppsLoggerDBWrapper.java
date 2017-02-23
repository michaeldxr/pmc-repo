package oracle.apps.logs;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import oracle.apps.helpers.FamilyModuleHelper;


public class AppsLoggerDBWrapper {
    String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
    static String statsUname = "codescan";
    static String statsPwd = "codescan";
    static String pwd;
    static String clearDBFamily = null;
    static String codeLine = "V2";
    static String series = "";
    
    public static void main(String[] args) {             
     
       pwd = args[0];
     //  pwd = "/scratch/sudgupta";
       
       String env = args[1];       
       clearDBFamily = args[2]; //Family name used to delete results from DB
      // clearDBFamily = "ALL";
        
        if (pwd == null || pwd.trim().equals("")) {
            System.out.println("Present Working directory has to be specified");
            System.exit(1);
        }
        
        if(args.length > 3)
            codeLine = args[3].trim();
        series = FamilyModuleHelper.getSeries(codeLine);
        
      AppsLoggerDBWrapper dbWrapper = new AppsLoggerDBWrapper();

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
          String fileName1 = pwd + "/appslogger_module_issues.csv";
          String fileName2 = pwd + "/appslogger_loglevel_issues.csv";
          String userHomeDir = System.getProperty("user.home");
          if(clearDBFamily != null && clearDBFamily.trim().equals("ALL")) {
              fileName1 = userHomeDir + "/appslogger_module_issues_final.csv";
              fileName2 = userHomeDir + "/appslogger_loglevel_issues_final.csv";
          }
          BufferedReader fileReader = new BufferedReader(new FileReader(fileName1));
          String line = fileReader.readLine();
          if(line == null) return;
          
          //Class.forName("oracle.jdbc.driver.OracleDriver");
          con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
          con.setAutoCommit(false);
             
          if(clearDBFamily != null) {
            System.out.println("First delete records from DB...");
            Statement stmt = con.createStatement();
            String sql = "";
            if(clearDBFamily.trim().equals("ALL"))
                sql = "DELETE FROM codescan_results_p WHERE ISSUETYPE = 'Review Log Level' " +
                    "or ISSUETYPE = 'Incorrect Module Parameter' and SERIES IN " + series;
            else
                sql = "DELETE FROM codescan_results_p WHERE ISSUETYPE = 'Review Log Level' " +
                    "or ISSUETYPE = 'Incorrect Module Parameter' and FAMILY = " + "'" + clearDBFamily.trim() + 
                      "' and SERIES IN " + series;
            //This should automatically delete the corresponding rows from the child table as FK has been specified with on delete cascade
           
            stmt.executeUpdate(sql);  
            //con.commit();          
            stmt.close();
          }
          
          PreparedStatement pstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS_P(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,SERIES,LABEL) " +
                "VALUES(?,?,?,?,?,?,?,?,?)");
          
          System.out.println("Now inserting values...");
          
          while((line = fileReader.readLine()) != null) {
              
            String[] parts = line.split(",");
              if(parts.length < 11) {
                  System.out.println("Skipping: " + line);
                  continue;
              }
              if(line.startsWith("Family"))
                  continue;
              
              String[] labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
            pstmt.setString(1, parts[0].trim()); //family
            pstmt.setString(2, parts[1].trim()); //module
            pstmt.setString(3, parts[2].trim()); //product
            pstmt.setString(4, parts[3].trim()); //filename 
            pstmt.setString(5, parts[10].trim()); //issue     
            pstmt.setString(6, "AppsLoggerIssue"); //subissue              
            pstmt.setString(7, parts[8].trim() + ":" + parts[9].trim()); //Description        
            
            pstmt.setString(8, labelSeries[0]); //series
            pstmt.setString(9, labelSeries[1]); //label
            pstmt.execute();            
          }
          fileReader.close();
          fileReader = new BufferedReader(new FileReader(fileName2));
          line = fileReader.readLine();
          if(line != null) {
          
              while((line = fileReader.readLine()) != null) {
                  
                String[] parts = line.split(",");
                  if(parts.length < 8) {
                      System.out.println("Skipping: " + line);
                      continue;
                  }
                  if(line.startsWith("Family"))
                      continue;
                  
                  String[] labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
                pstmt.setString(1, parts[0].trim()); //family
                pstmt.setString(2, parts[1].trim()); //module
                pstmt.setString(3, parts[2].trim()); //product
                pstmt.setString(4, parts[3].trim()); //filename 
                pstmt.setString(5, parts[7].trim()); //issue     
                pstmt.setString(6, "AppsLoggerIssue"); //subissue              
                pstmt.setString(7, parts[5].trim() + ":" + parts[6].trim()); //Description        
                
                pstmt.setString(8, labelSeries[0]); //series
                pstmt.setString(9, labelSeries[1]); //label
                pstmt.execute();            
              }
          }
          con.commit();
          pstmt.close();
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
