package oracle.apps;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.sqlanalyzer.AutoExecuteVCWrapperLRG;
import oracle.apps.utility.LRGUtil;

public class WHOScanDBWRapper {
  
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
    
    //series="FUSIONAPPS_PT.V2MIB_LINUX.X64";
    //pwd = "/scratch/sudgupta";

    WHOScanDBWRapper dbWrapper = new WHOScanDBWRapper();

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
        String fileName = pwd + "/who.csv";
        
        BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
        String line = fileReader.readLine();

        con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
        con.setAutoCommit(false);
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,SERIES,LABEL) VALUES (?,?,?,?,?,?,?,?,?)");
       
        System.out.println("First delete records from DB...");
        Statement stmt = con.createStatement();
        String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'WHODataTypeMismatch' and SERIES='"+ series+"'";
         
        stmt.executeUpdate(sql);          
        stmt.close();
        
        if(line == null)  {
            con.commit();  
            return;
        }
            
        System.out.println("Now inserting values...");
        int count = 0;
        while((line = fileReader.readLine()) != null) {
            
          String[] parts = line.split(",");
          if(parts.length < 12) continue;
          
          String jsffFile = parts[1].trim();
         String fileNameInfo = FamilyModuleHelper.getFileNameInfo(jsffFile);
          String[] fileInfoParts = fileNameInfo.split(",");

        pstmt.setString(1, fileInfoParts[0].trim()); //family
          pstmt.setString(2, fileInfoParts[1].trim()); //module
          pstmt.setString(3, fileInfoParts[2].trim()); //product
          pstmt.setString(4, fileInfoParts[3].trim()); //filename
          pstmt.setString(5, "WHODataTypeMismatch"); //issue        
          pstmt.setString(6, "TableID: " + parts[3].trim()); //subissue        
          pstmt.setString(7,parts[8].trim() + ": " + parts[9].trim()
                            + " - " + parts[11].replace("Data type mismatch.","")); //description
          pstmt.setString(8, series); //series
          pstmt.setString(9, ""); //label
        //  cstmt.execute();
          pstmt.addBatch();
          count++;
        }
        pstmt.executeBatch();
        con.commit();
        pstmt.close();
        
        System.out.println("There are " + count + " violations for WHODataTypeMismatch Scan on " + series + "\n");
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
