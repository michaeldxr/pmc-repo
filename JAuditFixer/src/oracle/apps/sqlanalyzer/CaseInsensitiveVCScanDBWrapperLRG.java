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


public class CaseInsensitiveVCScanDBWrapperLRG {
  
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

    CaseInsensitiveVCScanDBWrapperLRG dbWrapper = new CaseInsensitiveVCScanDBWrapperLRG();

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
        String fileName = pwd + "/ViewCriteriaCaseMismatch_LRG.csv";
       
        BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
        String line = fileReader.readLine();
        //if(line == null) return;
        
        //Class.forName("oracle.jdbc.driver.OracleDriver");
        con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
        con.setAutoCommit(false);
        CallableStatement cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
           
          System.out.println("First delete records from DB...");
          Statement stmt = con.createStatement();
          String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'ViewCriteriaCaseMismatch' and SERIES ='" + series+"'";
          
          stmt.executeUpdate(sql);          
          stmt.close();
        
        
        if(line == null)  {
            con.commit();  
            return;
        }
        
        cstmt.registerOutParameter(1, Types.INTEGER);
        
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO VIEW_CRITERIA_SCAN(SEQ_ID, VIEW_ATTR, ENTITY_ATTR," +
            "ENTITY_USAGE, COL_NAME, TABLE_NAME, COL_TYPE, COL_INDEX, VC_REQUIRED,VC_ISUPPER, COLUMN_DATA_CASE) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?)");
        System.out.println("Now inserting values...");
        String[] labelSeries = null;
        String prevLabel = "";
        int count=0;
        while((line = fileReader.readLine()) != null) {
            
          String[] parts = line.split(",");
          if(parts.length < 16) continue;
            
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
          cstmt.setString(6, "ViewCriteriaCaseMismatch"); //issue        
          cstmt.setString(7, ""); //subissue        
          cstmt.setString(8,parts[15].trim() + ":" + parts[14].trim() + ": VC missing key index");
          cstmt.execute();
            
          int seq = cstmt.getInt(1);
          pstmt.setInt(1,seq); 
          pstmt.setString(2,parts[5]); //view_attr
          pstmt.setString(3,parts[6]); //entity_attr
          pstmt.setString(4,parts[7]);  //entity_usage
          pstmt.setString(5,parts[8]); //column name
          pstmt.setString(6,parts[9]); //table name
          pstmt.setString(7,parts[10]); //column type
          pstmt.setString(8,parts[11]); //column index
          pstmt.setString(9,parts[12]); //vc_required
          pstmt.setString(10,parts[13]); //vc_isupper
          
          String col_data = "";
          if(parts.length > 16)
              col_data = parts[16].trim();
          pstmt.setString(11,col_data); //column_data_case
    
          pstmt.execute();    
          count++;
        }
        con.commit();
        cstmt.close();
        pstmt.close();
        System.out.println("Total count for ViewCriteriaCaseMismatch is "+count +" on series "+series);
    }catch(Exception e) {
        e.printStackTrace();
        if(con != null)
            con.rollback();
    } finally{
        if(con != null)
            con.close();
    }
  }
  }
