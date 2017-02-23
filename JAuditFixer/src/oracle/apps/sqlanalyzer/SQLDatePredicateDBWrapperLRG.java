package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import java.util.HashSet;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;

public class SQLDatePredicateDBWrapperLRG {
    String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
    static String statsUname = "codescan";
    static String statsPwd = "codescan";
    static String pwd;
    static String series = "";
    HashSet<String> violations = new HashSet<String>();
    
    public static void main(String[] args) {             
     
        if (args.length < 2) {
            System.out.println("Need to specify both 1.directory and 2.series");
            System.exit(1);
        }

        pwd = args[0];
        series = args[1];

      SQLDatePredicateDBWrapperLRG dbWrapper = new SQLDatePredicateDBWrapperLRG();

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
          String fileName = pwd + "/SysdateNotTruncated_LRG.csv";

          BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
          String line = fileReader.readLine();
          if(line == null) return;
          
          con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
          con.setAutoCommit(false);
    
          CallableStatement cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
          
          System.out.println("First delete records from DB...");
          Statement stmt = con.createStatement();
          
          String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'SysdateNotTruncated' and SERIES ='" + series+"'";
          stmt.executeUpdate(sql);          
          stmt.close();
          
          if(line == null)  {
              con.commit(); 
              return;
          }
          
          PreparedStatement pstmt = con.prepareStatement("INSERT INTO SYSDATE_TRUNC_SCAN(SEQ_ID, TABLECOLUMN1, TIMESTAMPEDROWS1, NONTIMESTAMPEDROWS1, PERCENTTIMESTAMPED1," +
              "TABLECOLUMN2, TIMESTAMPEDROWS2, NONTIMESTAMPEDROWS2, PERCENETTIMESTAMPED2) " +
              "VALUES(?,?,?,?,?,?,?,?,?)");
          
          cstmt.registerOutParameter(1, Types.INTEGER);
          
          System.out.println("Now inserting values...");
          
          String[] labelSeries = null;
          String prevLabel = "";
          int count = 0;
          
          while((line = fileReader.readLine()) != null) {
              
             String[] parts = line.split(",");
              if(parts.length < 12 || parts.length > 18) 
                  continue;
              
              if(violations.contains(line))
                  continue; //avoid duplicates
              violations.add(line);
              
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
            cstmt.setString(6, "SysdateNotTruncated"); //issue        
            cstmt.setString(7, ""); //subissue       
            cstmt.setString(8,parts[5].trim().replaceAll(":COMMA:", ","));
              
            try{
                  cstmt.execute();    
                
                  int seq = cstmt.getInt(1);
                  pstmt.setInt(1,seq); 
                  pstmt.setString(2,parts[8]); //resolvedtablecol1
                  pstmt.setString(3,parts[9]); //timestampedrows1
                  pstmt.setString(4,parts[10]); //nontimestampedrows1
                  pstmt.setString(5,parts[11]); //percenttimestamped1

                if(parts.length > 14)
                    pstmt.setString(6,parts[14]);  //resolvedtablecol2
                else
                    pstmt.setString(6,""); 
                
                if(parts.length > 15) 
                    pstmt.setString(7,parts[15]); //timestampedrows2
                else 
                    pstmt.setString(7,"");
                if(parts.length > 16)
                    pstmt.setString(8,parts[16]); //timestampedrows2
                else
                    pstmt.setString(8,""); 

                if(parts.length > 17) 
                    pstmt.setString(9,parts[17]); //timestampedrows2
                else 
                    pstmt.setString(9,""); 
                    
                pstmt.execute();
                count ++;
                
           } catch(SQLException e){
                e.printStackTrace();
           }
          }
          con.commit();
          cstmt.close();
          pstmt.close();
          System.out.println("Total count for SysdateNotTruncated is "+count +" on series "+series);
          
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
