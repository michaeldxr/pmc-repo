package oracle.apps.headers;

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

public class DoubleHeaderDBWrapperLRG {
    
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
   
      DoubleHeaderDBWrapperLRG dbWrapper = new DoubleHeaderDBWrapperLRG();

    try {         
        System.out.println("Updating DB....");
        dbWrapper.updateDatabase(); 
        dbWrapper.updateDatabase1();
    } catch (Exception e) {
        e.printStackTrace();        
    }
  }  
  
  
  public void updateDatabase() throws Exception {
      
    String fileName = pwd+ "/BadHeaders_LRG.csv";
   
    BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
    String line = fileReader.readLine();
    //if(line == null) return;
    
    Connection con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
    con.setAutoCommit(false);
      
    CallableStatement cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
    
    System.out.println("First delete records from DB...");
    Statement stmt = con.createStatement();
    String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'Bad Headers' and SERIES='" + series+"'";
      
    stmt.executeUpdate(sql);
    con.commit();          
    stmt.close();
    
    if(line == null) return;
    
    cstmt.registerOutParameter(1, Types.INTEGER);
    
    PreparedStatement pstmt = con.prepareStatement("INSERT INTO DOUBLE_HEADER_SCAN(SEQ_ID,EXTENSION,HEADER1,HEADER2,SYNCHRONIZED,BAD_SYNTAX_HEADER,NUM_HEADERS,NUM_PACKAGES) " +
          "VALUES(?,?,?,?,?,?,?,?)");
    System.out.println("Now inserting values...");
    
    String[] labelSeries = null;
    String prevLabel = "";
    int count = 0;
    
    while((line = fileReader.readLine()) != null) {
        
      String[] parts = line.split(",");
      if(parts.length < 11) continue;
        
        if (LRGUtil.isDeferred(con, series, parts[3].trim()))
            continue;

      cstmt.setString(2, parts[1].trim()); //family
      cstmt.setString(3, ""); //module
      cstmt.setString(4, parts[2].trim()); //product
      cstmt.setString(5, parts[3].trim()); //filename
      cstmt.setString(6, "Bad Headers"); //issue
        
      String subissue = "";
      int num_headers = Integer.parseInt(parts[9].trim());
      int num_packages = Integer.parseInt(parts[10].trim());
      if(num_headers >= 2 && num_headers > num_packages)
          subissue = "More Headers than packages";
      else if(num_headers == 0)
          subissue = "File has no headers";
      else if(num_headers < num_packages)
          subissue = "File has fewer headers than packages";
      else
          subissue = "Bad header syntax";
        
      cstmt.setString(7, subissue); //subissue

      if(prevLabel != parts[0].trim()) {
            labelSeries = FamilyModuleHelper.getLabelSeries(parts[0].trim());
            prevLabel = parts[0].trim();
      }
        
      cstmt.setString(8,"Header1: " + parts[5] + ",Header2: " + parts[6] + ";"); //description
      cstmt.setString(9,labelSeries[0]); //series   
      cstmt.setString(10,labelSeries[1]); //label   
  
      cstmt.execute();    
        
      int seq = cstmt.getInt(1);
      pstmt.setInt(1,seq);
      pstmt.setString(2,parts[4]); //extension
      pstmt.setString(3,parts[5]); //header1
      pstmt.setString(4,parts[6]); // header2
      pstmt.setString(5,parts[7]); // synchronized
      pstmt.setString(6,parts[8]); //Header with bad syntax
      pstmt.setString(7,parts[9]); //# headers
      pstmt.setString(8,parts[10]); //# packages
      pstmt.execute();
      
      count ++;
    }
      con.commit();
      fileReader.close();
      cstmt.close();
      pstmt.close();
      con.close();
      System.out.println("Total count for Bad Header is "+count +" on series "+series);
  }
  
  public void updateDatabase1() throws Exception {
      
    String fileName = pwd+ "/BadHeadersAgain_LRG.csv";
   
    BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
      
    String line = fileReader.readLine();
    if(line == null) return;
    
    Connection con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
    con.setAutoCommit(false);

    CallableStatement cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
    
    cstmt.registerOutParameter(1, Types.INTEGER);
    
    PreparedStatement pstmt = con.prepareStatement("INSERT INTO DOUBLE_HEADER_SCAN(SEQ_ID,EXTENSION,HEADER1,HEADER2,SYNCHRONIZED,BAD_SYNTAX_HEADER,NUM_HEADERS,NUM_PACKAGES) " +
        "VALUES(?,?,?,?,?,?,?,?)");
    System.out.println("Now inserting values...");
      String[] labelSeries = null;
    String prevLabel = "";
    int count = 0;
    
    //outputFileWriter.write("Label,Family,Product,Filename,Extension,Header1,Header2,Headers Syncronized?,Header With bad syntax,# Headers\n");    
    while((line = fileReader.readLine()) != null) {
        
      String[] parts = line.split(",");
      if(parts.length < 11) continue;

        if (LRGUtil.isDeferred(con, series, parts[3].trim()))
           continue;

      if(prevLabel != parts[0].trim()) {
            labelSeries = FamilyModuleHelper.getLabelSeries(parts[0].trim());
            prevLabel = parts[0].trim();
      }
      cstmt.setString(2, parts[1].trim()); //family
      cstmt.setString(3, ""); //module
      cstmt.setString(4, parts[2].trim()); //product
      cstmt.setString(5, parts[3].trim()); //filename
      cstmt.setString(6, "Bad Headers"); //issue
      cstmt.setString(7, "Bad header location"); //subissue
      cstmt.setString(8,""); //description
      cstmt.setString(9,labelSeries[0]); //series
      cstmt.setString(10,labelSeries[1]); //label
  
      cstmt.execute();

      int seq = cstmt.getInt(1);
      pstmt.setInt(1,seq);
      pstmt.setString(2,parts[4]); //extension
      pstmt.setString(3,parts[5]); //header1
      pstmt.setString(4,""); // header2
      pstmt.setString(5,""); // synchronized
      pstmt.setString(6,""); //Header with bad syntax
      pstmt.setString(7,parts[9]); //# headers
      pstmt.setString(8,parts[10]); //# packages
      pstmt.execute();      
      count++;
        
    }
    con.commit();
    fileReader.close();
    cstmt.close();
    pstmt.close();
    con.close();
    System.out.println("Total count for Bad Header - bad location is "+count +" on series "+series);
  }
}
