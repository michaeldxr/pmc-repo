package oracle.apps.rowCountAnalyzer;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Types;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;


public class RCT_DBWrapperLRG {
  //String statsDB = "jdbc:oracle:thin:@//rws65057fwks.us.oracle.com:1521/Albowicz";
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
      
    RCT_DBWrapperLRG dbWrapper = new RCT_DBWrapperLRG();

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
        String fileName = pwd + "/RowCountThreshold_LRG.csv";
        
        BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
        String line = fileReader.readLine();
            
        con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
        con.setAutoCommit(false);
        CallableStatement cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
           
          System.out.println("First delete records from DB...");
          Statement stmt = con.createStatement();
          String sql ="DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'RowCountThreshold' and SERIES='" + series+"'";
         
          stmt.executeUpdate(sql);         
          stmt.close();
        
        
        if(line == null) {
            con.commit();  
            return;
        }

        cstmt.registerOutParameter(1, Types.INTEGER);
        
        String[] labelSeries = null;
        String prevLabel = "";
        
        System.out.println("Now inserting values...");
        int count=0;
        while((line = fileReader.readLine()) != null) {
            
          String[] parts = line.split(",");
          if(parts.length < 8) continue;
            
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
          cstmt.setString(9, labelSeries[0]); //series
          cstmt.setString(10, labelSeries[1]); //label
          cstmt.setString(6, "RowCountThreshold"); //issue
            
          cstmt.setString(7, ""); //subissue
            
          cstmt.setString(8,parts[6].trim() + " - RCT:" + parts[7].trim());
          cstmt.execute();
          count++;
        }    
        con.commit();
        cstmt.close();
        System.out.println("Total count for RowCountThreshold is "+count +" on series "+series);
    } catch(Exception e){
        e.printStackTrace();
        if(con != null)
            con.rollback();
    }finally {
        if(con != null)
           con.close();
    }                     
  }
}
