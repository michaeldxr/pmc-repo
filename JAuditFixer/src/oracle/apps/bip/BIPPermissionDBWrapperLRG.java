package oracle.apps.bip;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;

public class BIPPermissionDBWrapperLRG {
    
  String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
  static String statsUname = "codescan";
  static String statsPwd = "codescan";
  static String fileLoc;
  static String series = "";
  
  public static void main(String[] args) {             
   
    if (args.length < 2) {
        System.out.println("Need to specify both 1.directory and 2.series");
        System.exit(1);
    }

    fileLoc = args[0];
    series = args[1];

    BIPPermissionDBWrapperLRG dbWrapper = new BIPPermissionDBWrapperLRG();

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
        String fileName = fileLoc;
        
        BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
        String line = "";
             
        con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
        con.setAutoCommit(false);
        
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS(SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION) " +
                "VALUES(?,?,?,?,?,?,?,?,?)");
           
          System.out.println("First delete records from DB...");
          Statement stmt = con.createStatement();
          String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'BIP Permissions' and SERIES ='" + series+"'";

          stmt.executeUpdate(sql);          
          stmt.close();                     
        
        System.out.println("Now inserting values...");

        
        int count =0;
        while((line = fileReader.readLine()) != null) {
            
            String[] parts = line.split(",");
            if(parts.length < 9) {
                System.out.println("Skipping: " + line);
                continue;
            }
            
            pstmt.setString(1, parts[0].trim()); //series
            pstmt.setString(2, parts[1].trim()); //label
            pstmt.setString(3, parts[2].trim()); //family
            pstmt.setString(4, parts[3].trim()); //modile
            pstmt.setString(5, parts[4].trim()); //product
            
            pstmt.setString(6, parts[5].trim()); //issuetype
            pstmt.setString(7,parts[6].trim()); //sub-issue
            pstmt.setString(8,parts[7].trim()); //filename
            pstmt.setString(9,parts[8].trim()); //error text
            pstmt.execute();     
            count++; 
        }
        con.commit();
        pstmt.close();
        System.out.println("Total count for BIP Permission is "+count +" on series "+series);
        
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

