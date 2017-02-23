package oracle.apps.votuning;

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

public class VoTuningDBWrapperLRG {
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

    VoTuningDBWrapperLRG dbWrapper = new VoTuningDBWrapperLRG();

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
        String fileName = pwd + "/TableAndLOVTuning_LRG.csv";
        
        BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
        String line = fileReader.readLine();

        con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
        con.setAutoCommit(false);
        
        CallableStatement cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
           
          System.out.println("First delete records from DB...");
          Statement stmt = con.createStatement();
          String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'TableAndLOVTuning' and SERIES='" + series+"'";          
        
          stmt.executeUpdate(sql);       
          stmt.close();
        
        
        if(line == null)  {
            con.commit();  
            return;
        }
          
        cstmt.registerOutParameter(1, Types.INTEGER);
        
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO VOTUNING(SEQ_ID, COMPONENT, COMPONENT_ID, AUTOHEIGHT_ROWS," +
            "ITERATOR_RANGE_SIZE, IRS_NEW, ITERATOR_NAME, LIST_RANGE_SIZE, LRS_NEW, VO_USAGE_FETCH_SIZE, VO_USAGE_FETCH_SIZE_NEW, " +
            "VO_DEFAULT_FETCH_SIZE, VO_DEFAULT_FETCH_SIZE_NEW, AMPACKAGE, VOPACKAGE, VOINSTANCE_NAME) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        System.out.println("Now inserting values...");
        String[] labelSeries = null;
        
        String prevLabel = "";
        
        int count = 0;
        while((line = fileReader.readLine()) != null) {
            
          String[] parts = line.split(",");
          if(parts.length < 21) continue;
          
          if(!insertInDB(parts[20]))
              continue;
            
            if (LRGUtil.isDeferred(con, series, parts[3].trim()))
                continue;
                    
            if(prevLabel != parts[4].trim()) {
                labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
                prevLabel = parts[4].trim();
            }
            String description =  getDescription(parts[6],parts[20]);
            
            cstmt.setString(2, parts[0].trim()); //family
            cstmt.setString(3, parts[1].trim()); //module
            cstmt.setString(4, parts[2].trim()); //product
            cstmt.setString(5, parts[3].trim()); //filename
            cstmt.setString(9, labelSeries[0]); //series
            cstmt.setString(10, labelSeries[1]); //label
            cstmt.setString(6, "TableAndLOVTuning"); //issue        
            cstmt.setString(7, parts[6]); //subissue   - component id - for uniqueness   
            cstmt.setString(8,description);
            cstmt.execute();
            
            int seq1 = cstmt.getInt(1);
            pstmt.setInt(1,seq1); //SEQ_ID
            pstmt.setString(2,parts[5]); //component name
            pstmt.setString(3,parts[6]); //component id
            pstmt.setString(4,parts[7]); //autoheight rows
            pstmt.setString(5,parts[8]); //IRS
            pstmt.setString(6,parts[9]); //IRS new
            pstmt.setString(7,parts[10]); //Iterator Name
            pstmt.setString(8,parts[11]); //LRS
            pstmt.setString(9,parts[12]); //LRS New
            pstmt.setString(10,parts[13]); //VO Usage Fetch Size
            pstmt.setString(11,parts[14]); //VO Usage Fetch Size new
            pstmt.setString(12,parts[15]); //Vo Default Fetch Size
            pstmt.setString(13,parts[16]); //Vo Default Fetch Size New
            pstmt.setString(14,parts[17]);  //am package
            pstmt.setString(15,parts[18]); //vo package
            pstmt.setString(16,parts[19]); //vo instance
            
            pstmt.execute();
            
            count ++;
        }
        con.commit();
        cstmt.close();
        pstmt.close();
        
        System.out.println("Total count for TableAndLOVTuning is "+count +" on series "+series);
       
    } catch(Exception e) {
        e.printStackTrace();
        if(con != null)
            con.rollback();
    } finally {
        if(con != null)
            con.close();
    }    
  }
  
  public static boolean insertInDB(String fixStatuses) {
      
    fixStatuses = fixStatuses.replace("MULTIPLE_BC_SUCCESS","");
    
    if(fixStatuses.contains("SUCCESS"))
        return true;
            
    return false;
  }
  
  public static String getDescription(String compId, String fixStatuses){
      
    String result = "";
    
    String[] parts = fixStatuses.trim().split(" ");

    for(int i = 0; i < parts.length; i++) {
      if(parts[i].contains("LRS_FIX"))
        result += ", Fix List RangeSize";
      else if(parts[i].contains("ITERATOR_FIX"))
        result += ", Fix Iterator RangeSize";
      else if(parts[i].contains("VA_FETCHSIZE_FIX"))
        result += ", Fix ViewAccessor ";
      else if(parts[i].contains("AM_VO_FETCH"))
        result += ", Fix AM VO Usage Fetch Size";
      else if(parts[i].contains("VOFETCH_DEFAULT"))
        result += ", Fix Default VO Fetch Size";
    }
    result = result.replaceFirst(", ", "");
        
    return compId + ":" + result;
  }
}
