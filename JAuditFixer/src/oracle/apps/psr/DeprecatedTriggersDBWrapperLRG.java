package oracle.apps.psr;

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

public class DeprecatedTriggersDBWrapperLRG {
    
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

    DeprecatedTriggersDBWrapperLRG dbWrapper = new DeprecatedTriggersDBWrapperLRG();

    try {         
        System.out.println("Updating DB....");
        dbWrapper.updateDatabase();          
    } catch (Exception e) {
        e.printStackTrace();        
    }
  }  
  
  
  public void updateDatabase() throws Exception {
    
    String fileName = pwd+ "/DeprecatedPartialTriggers_LRG.csv";
   
    BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
    String line = fileReader.readLine();
    
    Connection con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
    con.setAutoCommit(false);
    CallableStatement cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");;
       
    System.out.println("First delete records from DB...");
    Statement stmt = con.createStatement();
    String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'DeprecatedPartialTriggers' and SERIES ='" + series+"'";
     
    stmt.executeUpdate(sql);  
    con.commit();          
    stmt.close();
    
    cstmt.registerOutParameter(1, Types.INTEGER);
    
    System.out.println("Now inserting values...");
    String[] labelSeries = null;
    String prevLabel = "";
    
    int count=0;
    while((line = fileReader.readLine()) != null) {
        
      String[] parts = line.split(",");
      if(parts.length < 6) continue;
        
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
      cstmt.setString(6, "DeprecatedPartialTriggers"); //issue
      cstmt.setString(7, ""); //subissue
      cstmt.setString(8,parts[5].trim()); //description 
      cstmt.setString(9,labelSeries[0]); //series
      cstmt.setString(10,labelSeries[1]); //label
     
      cstmt.execute();
        count++;
    }
    con.commit();
    cstmt.close();
    con.close();
    System.out.println("Total count for DeprecatedPartialTriggers is "+count +" on series "+series);
  }
}
