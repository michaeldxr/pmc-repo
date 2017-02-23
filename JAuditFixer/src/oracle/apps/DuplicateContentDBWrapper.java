package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;

import java.io.FileWriter;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Types;

import java.util.HashMap;
import java.util.Iterator;

public class DuplicateContentDBWrapper {
    
  String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:albowicz";
  static String statsUname = "findev";
  static String statsPwd = "findev";
  static String pwd;
  static String clearDBFamily = null;
  
  public static void main(String[] args) {             
   
     pwd = args[0];
        //pwd = "/scratch/sudgupta/view_storage/sudgupta_fatools_mar15_view/fatools/opensource/jauditFixScripts/JAuditFixer";
     
     String env = args[1];
    //String env = "dev";
     if(env != null && env.equals("dev")){
       statsUname = "findev";
       statsPwd = "findev";
     }
     else {
       statsUname = "fintech";
       statsPwd = "fintech";
     }
     
     clearDBFamily = args[2]; //Family name used to delete results from DB
    //clearDBFamily = "FIN";
      
      if (pwd == null || pwd.trim().equals("")) {
          System.out.println("Present Working directory has to be specified");
          System.exit(1);
      }

    DuplicateContentDBWrapper dbWrapper = new DuplicateContentDBWrapper();
  

    try {         
        dbWrapper.removeDuplicates();
        System.out.println("Updating DB....");
        dbWrapper.updateDatabase();          
    } catch (Exception e) {
        e.printStackTrace();        
    }
  }  
  
  
  public void updateDatabase() throws Exception {
      
    String fileName = pwd+ "/duplicateContentScan.csv";
    if(clearDBFamily != null && clearDBFamily.trim().equals("ALL"))
        fileName = pwd+ "/duplicateContentScan_final.csv";
    BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
    String line = fileReader.readLine();
    
    if(line == null) return;
    
    //Class.forName("oracle.jdbc.driver.OracleDriver");
    Connection con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
    con.setAutoCommit(false);
       
    if(clearDBFamily != null) {
      System.out.println("First delete records from DB...");
      Statement stmt = con.createStatement();
      String sql ="";
      if(clearDBFamily.trim().equals("ALL"))
         sql = "DELETE FROM codescan_results WHERE ISSUETYPE = 'DuplicatedJarContent'";
      else
         sql = "DELETE FROM codescan_results WHERE ISSUETYPE = 'DuplicatedJarContent' and FAMILY = " + "'" + clearDBFamily.trim() + "'";
      //This should automatically delete the corresponding rows from the child table as FK has been specified with on delete cascade
     
      stmt.executeUpdate(sql);  
      con.commit();          
      stmt.close();
    }
    
    CallableStatement cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?) }");
    cstmt.registerOutParameter(1, Types.INTEGER);
    
    System.out.println("Now inserting values...");
    while((line = fileReader.readLine()) != null) {
        
      String[] parts = line.split(",");
      if(parts.length < 5) continue;
      cstmt.setString(2, parts[2].trim().toUpperCase()); //family
      cstmt.setString(3, ""); //module
      cstmt.setString(4, ""); //product
      cstmt.setString(5, parts[3].trim()); //filename
      cstmt.setString(6, "DuplicatedJarContent"); //issue
      cstmt.setString(7, ""); //subissue = entryname
      cstmt.setString(8,parts[4].trim()); //description = jar2
      cstmt.setString(9,parts[1].trim()); //label
     
      cstmt.execute();
      con.commit();
    }
    cstmt.close();
    con.close();
  }
  
  private void removeDuplicates() throws Exception {
    HashMap<String,String> jars = new HashMap<String,String>();
    //String label = "";
    
    String fileName = pwd+ "/duplicateContentScan.csv";
    if(clearDBFamily != null && clearDBFamily.trim().equals("ALL"))
        fileName = pwd+ "/duplicateContentScan_final.csv";
    
    BufferedReader reader = new BufferedReader(new FileReader(fileName));
    String line = reader.readLine();
    if(line == null) return;
    
    //label = line.trim();
    //line = reader.readLine();
    
    while((line = reader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 6) continue;
        String jarString = parts[4] + "," + parts[5];
        String productLabelFamily = parts[0] + "," + parts[1] + "," + parts[2];
        jars.put(jarString,productLabelFamily);
    }
    reader.close();
    
    BufferedWriter writer = new BufferedWriter(new FileWriter(pwd + "/duplicateContentScan.csv"));
    //writer.write(label + "\n");
    writer.write("Product,Label,Family,Jar1,Jar2\n");
    for(Iterator i = jars.keySet().iterator(); i.hasNext();) {
        
       String jarString = (String)i.next();
       String productLabelFamily = jars.get(jarString);
       String[] parts = productLabelFamily.split(",");
       if(parts.length < 3) continue;
       writer.write(parts[0].trim() + "," + parts[1].trim() + "," + parts[2].trim().toUpperCase() + "," + jarString + "\n");
    }
    writer.close();
  }

}
