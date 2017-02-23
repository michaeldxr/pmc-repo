package oracle.apps.bi;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;

public class BIExtenderDBWrapperLRG {
    
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

    BIExtenderDBWrapperLRG dbWrapper = new BIExtenderDBWrapperLRG();

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
        String fileName = pwd + "/BIExtenderAutomation_LRG.csv";
        
        BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
        String line = fileReader.readLine();
             
        con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
        con.setAutoCommit(false);
        
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,LABEL,SERIES) " +
                "VALUES(?,?,?,?,?,?,?,?,?)");
           
          System.out.println("First delete records from DB...");
          Statement stmt = con.createStatement();
          String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'BIExtenderAutomation' and SERIES ='" + series+"'";

          stmt.executeUpdate(sql);          
          stmt.close();
        
        if(line == null)  {
            con.commit(); 
            return;
        }
                     
        
        System.out.println("Now inserting values...");
        String[] labelSeries = null;
        String prevLabel = "";
        
        int count =0;
        while((line = fileReader.readLine()) != null) {
            
            String[] parts = line.split(",");
            if(parts.length < 10) {
                System.out.println("Skipping: " + line);
                continue;
            }
            
            if (LRGUtil.isDeferred(con, series, parts[3].trim()))
                continue;
            
            if(prevLabel != parts[4].trim()) {
                labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
                prevLabel = parts[4].trim();
            }
            
            pstmt.setString(1, parts[0].trim()); //family
            pstmt.setString(2, parts[1].trim()); //module
            pstmt.setString(3, parts[2].trim()); //product
            pstmt.setString(4, parts[3].trim()); //filename (SDO Interface)
            pstmt.setString(5, "BIExtenderAutomation"); //issue     
            
            pstmt.setString(6, parts[6].trim()); //subissue
            pstmt.setString(7,parts[7].trim() + "(" + parts[5].trim() + ") -" + parts[8].trim()); //Description
            pstmt.setString(8,labelSeries[1]); //label
            pstmt.setString(9,labelSeries[0]); //series
            pstmt.execute();     
            count++; 
        }
        con.commit();
        pstmt.close();
        System.out.println("Total count for BIExtenderAutomation is "+count +" on series "+series);
        
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
