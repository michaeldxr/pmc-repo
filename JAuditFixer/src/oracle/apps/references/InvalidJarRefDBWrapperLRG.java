package oracle.apps.references;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Types;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;

public class InvalidJarRefDBWrapperLRG {
    
  String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
  static String statsUname = "codescan";
  static String statsPwd = "codescan";
  static String pwd;
  static String series = "";
  static int releaseNum = 8;
  
  public static void main(String[] args) {             
   
    if (args.length < 2) {
        System.out.println("Need to specify both 1.directory and 2.series");
        System.exit(1);
    }

    pwd = args[0];
    series = args[1];
//    pwd = "/scratch/sudgupta/view_storage/sudgupta_fatools_aug5_view/fatools/opensource/jauditFixScripts/JAuditFixer";
//    series = "FSM_MAIN_GENERIC";
   
      String release= FamilyModuleHelper.getRelease(series);
      try{              
          releaseNum = Integer.parseInt(release);
      }catch(NumberFormatException e){
         e.printStackTrace();
         releaseNum=0;
      }
      
    InvalidJarRefDBWrapperLRG dbWrapper = new InvalidJarRefDBWrapperLRG();

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
        String fileName = pwd+ "/InvalidJarReference_LRG.csv";
        
        BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
        String line = fileReader.readLine();
        
        //Class.forName("oracle.jdbc.driver.OracleDriver");
        con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
        con.setAutoCommit(false);
        CallableStatement cstmt1 = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
        CallableStatement cstmt2 = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT_P(?,?,?,?,?,?,?,?,?) }");
        
        System.out.println("First delete records from DB...");
        Statement stmt = con.createStatement();
        String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'InvalidJarReference' and series ='" + series + "'";
        String sql1 = "DELETE FROM CODESCAN_RESULTS_P WHERE ISSUETYPE = 'InvalidJarReference' and series ='" + series + "'";
        
        if(series.contains("FSM"))
          stmt.executeUpdate(sql1);
        else
          stmt.executeUpdate(sql);
        stmt.close();
      
        cstmt1.registerOutParameter(1, Types.INTEGER);
        cstmt2.registerOutParameter(1, Types.INTEGER);

        System.out.println("Now inserting values...");
        String[] labelSeries = null;
        String prevLabel = "";
        int count=0;
      
      CallableStatement cstmt = cstmt1;
      if(series.contains("FSM"))
        cstmt = cstmt2;
        
        while((line = fileReader.readLine()) != null) {
            
          String[] parts = line.split(",");
          if(parts.length < 8) continue;
            
          String filename = parts[3].trim();
          if(filename.equals("fsm/components/setup/SetupInstaller/SetupInstaller.jpr") ||
             filename.equals("fsm/components/setup/SuperWeb/SuperWeb.jpr")) 
              continue;
            
            if(releaseNum <=8 && parts[7].trim().contains("/ide_link/MW_HOME"))
                continue;
            
            if(parts[7].trim().contains("fmwtools/BUILD_HOME/jdeveloper/bifoundation/javahost/lib/obisintegration/biAdminEssbaseRuleGenerator.jar"))
                continue;
            
            if (LRGUtil.isDeferred(con, series, parts[3].trim()))
                continue;
            
            if(prevLabel != parts[4].trim()) {
                labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
                prevLabel = parts[4].trim();
            }
          cstmt.setString(2, parts[0].trim()); //family
          cstmt.setString(3, parts[1].trim()); //module
          cstmt.setString(4, parts[2].trim()); //product
          cstmt.setString(5, parts[3].trim()); //filename
          cstmt.setString(6, "InvalidJarReference"); //issue
          cstmt.setString(7, parts[6].trim()); //subissue        
          cstmt.setString(8,parts[7].trim()); //description
          cstmt.setString(9,labelSeries[0]); //series
          cstmt.setString(10,labelSeries[1]); //label
    
          cstmt.execute();     
            count++;
        }
        con.commit();
        cstmt1.close();
        cstmt2.close();
        System.out.println("Total count for InvalidJarReference is "+count +" on series "+series);
        
    } catch(Exception e) {
        e.printStackTrace();
        if(con != null)
            con.rollback();
    } finally{
        if(con != null)
            con.close();
    }
  }
}
