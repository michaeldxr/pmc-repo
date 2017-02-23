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

public class PassivationDBWrapperLRG {
    
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

      PassivationDBWrapperLRG dbWrapper = new PassivationDBWrapperLRG();

    try {         
        System.out.println("Updating DB....");
        dbWrapper.updateDatabase();          
    } catch (Exception e) {
        e.printStackTrace();        
    }
  }  
  
  
  public void updateDatabase() throws Exception {
    
    String fileName = pwd + "/Passivation_LRG.csv";
    
    BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
    String line = fileReader.readLine();

    Connection con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
    con.setAutoCommit(false);
    CallableStatement cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
      
    System.out.println("First delete records from DB...");
    Statement stmt = con.createStatement();
    String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'Passivation' and SERIES ='" + series+"'";
     
    stmt.executeUpdate(sql);
    con.commit();          
    stmt.close();

    if(line == null) return;
    
    cstmt.registerOutParameter(1, Types.INTEGER);
    
    PreparedStatement pstmt = con.prepareStatement("INSERT INTO PASSIVATION_SCAN(SEQ_ID, LINENUM, ATTRNAME, ATTRTYPE, GETTER_ASSESSMENT," +
          "UPDATEABLE, PASSIVATION, VO_PASSIVATE_ALL, ATTR_PASSIVATE_SETTING, ENTITY_BASED, HAS_EXECUTE, HAS_GET_ATTRIBUTE, TRANS_EXPRESSION, RECALC_CONDITION) " +
          "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
    
    System.out.println("Now inserting values...");
    String[] labelSeries = null;
    String prevLabel = "";
    int count=0;
    
    while((line = fileReader.readLine()) != null) {
        
      String[] parts = line.split(",");
      if(parts.length < 18) continue;
        
        if (LRGUtil.isDeferred(con, series, parts[3].trim()))
            continue;
        
        if(prevLabel != parts[4].trim()) {
            labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
            prevLabel = parts[4].trim();
          //  Statement stmt = con.createStatement();
          //  stmt.executeUpdate("UPDATE SCAN_LABELS set latest_label = '" + labelSeries[1] + "'" +
           //     " WHERE issuetype = 'Passivation' AND series = '" + labelSeries[0] + "'");
            //stmt.close();
        }
        
      cstmt.setString(2, parts[0].trim()); //family
      cstmt.setString(3, parts[1].trim()); //module
      cstmt.setString(4, parts[2].trim()); //product
      cstmt.setString(5, parts[3].trim()); //filename
      cstmt.setString(9, labelSeries[0]); //series
      cstmt.setString(10, labelSeries[1]); //label
      cstmt.setString(6, "Passivation"); //issue
        
      String getterAssessment = parts[8].trim();
      
      String transExpr = parts[16].trim();
     
      String description = "";
      String subissue = "";
        
      if(getterAssessment.equals("Custom Getter")) {
        description = "Attr with Custom Getter marked to Passivate"; 
        subissue = "Custom Getter";
      }
      else if(transExpr.equals("present")) {
        description = "Attr has transient expression that will always be recalculated";
        subissue = "Groovy Expr";
      } else {
        description = "Read only Attr marked to Passivate"; 
        subissue = "Read only attr";
      }
        
        description = parts[6] + ": " + description;
        
      if(!subissue.equals("Custom Getter")) //other 2 are taken care of by jaudit
          continue;
        
      cstmt.setString(7, subissue); //subissue
        
      cstmt.setString(8,description);
      cstmt.execute();
        
      int seq = cstmt.getInt(1);
      pstmt.setInt(1,seq); 
      pstmt.setString(2,parts[5]); //linenum
      pstmt.setString(3,parts[6]); //attrName
      pstmt.setString(4,parts[7]);  //attrType
      pstmt.setString(5,parts[8]); //Getter Assessment
      pstmt.setString(6,parts[9]); //Updateable
      pstmt.setString(7,parts[10]); //Passivation
      pstmt.setString(8,parts[11]); //VO Passivate All
      pstmt.setString(9,parts[12]); //Attr Passivate Setting
      pstmt.setString(10,parts[13]); //Entity Based
      pstmt.setString(11,parts[14]); //Has Execute
      pstmt.setString(12,parts[15]); // Has getAttribute
      pstmt.setString(13,parts[16]); // Trans expression
      pstmt.setString(14,parts[17]); // Recalc Condition
        
      pstmt.execute();
      count++;
    }
    con.commit();
    cstmt.close();
    pstmt.close();
    con.close();
    System.out.println("Total count for Passivation is "+count +" on series "+series);
  }
}
