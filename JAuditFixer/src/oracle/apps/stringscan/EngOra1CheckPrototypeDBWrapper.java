package oracle.apps.stringscan;

import java.io.File;
import java.io.FileNotFoundException;

import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;


public class EngOra1CheckPrototypeDBWrapper {

  public EngOra1CheckPrototypeDBWrapper() {
    super();
  }


  public static Connection con = null;
  public static PreparedStatement insert = null;
  public static PreparedStatement delete = null;
  public static String prefix = null;
  public static final  Pattern p = Pattern.compile("(\\d+),(fusionapps.+?),(\".+?\")");
  
  public static void main(String[] args) {  
    if(args.length != 2){
      System.out.println("USAGE: runEngOra1chk.sh <series> <abs_output_dir>");
      System.exit(1);
    }
    
    if(args[0] == null || args[0].isEmpty()){
      System.out.println("USAGE: runEngOra1chk.sh <series> <abs_output_dir>");
      System.out.println("Please specify the series you want to scan on.");
      System.exit(1);
    }
    String series = args[0]; 
    
    if(args[1] == null || args[1].isEmpty()){
      System.out.println("USAGE: runEngOra1chk.sh <series> <abs_output_dir>");
      System.out.println("Please specify the output directory.");
      System.exit(1);
    }
    String outputDir = args[1];       
        
    prefix = FamilyModuleHelper.getLabelServerTop1(series) + "/"+series+".rdd/LATEST/";
    try {
      con =
          DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
      con.setAutoCommit(false);

      insert = con.prepareStatement("INSERT INTO CODESCAN_RESULTS_P (FAMILY, MODULE, PRODUCT, FILENAME, ISSUETYPE, SUB_ISSUE, DESCRIPTION, SERIES,LABEL) VALUES (?,?,?,?,?,?,?,?,?)");
      delete = con.prepareStatement("DELETE FROM CODESCAN_RESULTS_P WHERE ISSUETYPE = ? AND SERIES = ?");
      
      delete.setString(1, "English Duplicate ORA-1 Strings");
      delete.setString(2, series);
      
      delete.executeUpdate();
    }catch(Exception e){
      e.printStackTrace();
      System.out.println("Failed to create JDBC c");
      System.exit(1);
    }
    
   
    try {
      System.out.println("Parsing report csv: " + outputDir +"/clean_rptOra1_"+series+".csv...");
      HashMap<String, ArrayList<String>> issueMap = scanCSV(outputDir+"/clean_rptOra1_"+series+".csv");
      int count = insertIssues(issueMap);
      System.out.println("inserting the data...");
      insert.executeBatch();
      con.commit();
      System.out.println("Inserted " + count + " violations for " + series + ".");
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("Failed to commit");
      try {
        con.rollback();
      } catch (SQLException f) {
        System.out.println("Failed to rollback");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public static HashMap<String, ArrayList<String>> scanCSV(String csvPath) throws Exception {
    // declare a hashmap that will store mappings of issue number to a list
    // of issues that have that issue number
    HashMap<String, ArrayList<String>> issueMap = new HashMap<String, ArrayList<String>>();
    Scanner s;
    
    try {
      s = new Scanner(new File(csvPath));
      if(s.hasNextLine())
      //skip the csv header
        s.nextLine();
      else{
        System.out.println("Report clean_rptOra1.csv is empty");
        System.exit(1);
      }
      
      while(s.hasNextLine()){
        String line = s.nextLine();
        // get the issue number from the current line
        String issueNumber = line.substring(0, line.indexOf(","));
        // check if this issue number already exists in the hash map
        if (issueMap.containsKey(issueNumber)) {
            // if it is already in the hash map, then just append this row to the key
            issueMap.get(issueNumber).add(line);
        } else {
            // instantiate a new arraylist and associate it with this key
            ArrayList<String> lines = new ArrayList<String>();
            lines.add(line);
            issueMap.put(issueNumber, lines);
        }
      }
        
    } catch (FileNotFoundException e) {
      System.out.println("Report clean_rptOra1.csv not found");
    }
    
    return issueMap;
  }
  
  private static int insertIssues(HashMap<String, ArrayList<String>> issueMap) throws Exception {
    // declare a variable to keep track of the number of rows inserted
    int count = 0;
    
    // go through each list in the issues hash map
    for (ArrayList<String> issueList : issueMap.values()) {
      // concatenate the individual descriptions to build a description 
      // string from this list
      StringBuilder descriptionSB = new StringBuilder();
      for (String line : issueList) {
        descriptionSB.append("\n");
        descriptionSB.append(line);
      }
      String description = descriptionSB.toString();
      
      // then insert each of these lines with the description created above
      for (String line : issueList) {
        insert(line, description);
        count++;
      }
    }
    
    return count;
  }
  
  private static void insert(String line, String description) throws Exception {

    Matcher m = p.matcher(line);
    if(m.find()){
        String fileNameInfo = getFileNameInfo1(prefix + m.group(2));
        String[] parts = fileNameInfo.split(",");
        if(isQOC(parts[3].trim()))
            return;
        
        insert.setString(1, parts[0].trim());//family
        insert.setString(2, parts[1].trim());//module
        insert.setString(3, parts[2].trim());//product
        insert.setString(4, parts[3].trim());//filename
        insert.setString(5, "English Duplicate ORA-1 Strings");//issue
        insert.setString(6, "Issue Number: "+ m.group(1));//sub-issue
        insert.setString(7, "Found duplicate string: " + description);//description
        insert.setString(8, parts[4].trim()); //series
        insert.setString(9, parts[5].trim());//label
        insert.addBatch();
    }
    
  }
  
    static private boolean isQOC(String filename){
        return filename.contains("fusionapps/crm/components/orderCapture/qoc/") || filename.contains("fusionapps/crm/qoc/components/");
    }
  
    public static String getFileNameInfo1(String absPath) throws IOException {
        
      File f = new File(absPath);           
      absPath = f.getCanonicalPath();
      int i = 0;
      String family = "";
      String module = "";
      String product = "";
      String blankString =  "NA,NA,NA,NA,NA,NA,";
      if(isEmpty(absPath))
          return blankString;
      
      if(absPath.contains("fusionapps/")) {
       i = absPath.indexOf("fusionapps/");
      }
      if(absPath.contains("fsm/")){
       i = absPath.indexOf("fsm/");
       family = "FSM";
      }
        if (absPath.contains("atgpf/")) {
            i = absPath.indexOf("atgpf/");
            family = "ATGPF";
        }
        if (absPath.contains("emcore/")) {
            i = absPath.indexOf("emcore/");
            family = "EMGC";
            module = "EMCORE";
        }
        
      String pathAfterFusionApps = absPath.substring(i);
      String[] parts = pathAfterFusionApps.split("/");
      
      if(parts == null) return blankString;
      
      if(absPath.contains("fusionapps/")) {
          int partsLength = parts.length;
          if(partsLength>1)
            family = parts[1].trim().toUpperCase();  
          if(partsLength>3)
            module = parts[3].trim();
          if(partsLength>4)
            product = parts[2].trim().toUpperCase();
          
          if(product.equals(".ADF")){
              if(module.equals("payables"))
                  product="AP";
              else if(module.equals("ledger"))
                  product="GL";
          }          
      }
      if(absPath.contains("fsm/")) {
        int partsLength = parts.length;
        if(partsLength>2)
          module = parts[2].trim();
        if(partsLength>3)
          product = parts[3].trim().toUpperCase();
      }
      
        if (absPath.contains("atgpf/")) {
            int partsLength = parts.length;
            if (partsLength > 1)
                module = parts[1].trim();
            if (partsLength > 3)
                product = parts[3].trim().toUpperCase();
        }
        if (absPath.contains("emgc/")) {
            int partsLength = parts.length;
            if (partsLength > 1)
                product = parts[1].trim();
        }
         
      if(i<1)
         return family + "," + module + "," + product + "," + pathAfterFusionApps + ", , ," ;
      
      String path_before_fusionapps = absPath.substring(0, i-1);
      parts = path_before_fusionapps.split("/");
      if(parts==null || parts.length<2)
        return blankString;
     
      String series = parts[parts.length -2];
      series = series.replace(".rdd","");   
      String label = parts[parts.length -1]; 
      
      return family + "," + module + "," + product + "," + pathAfterFusionApps + "," + series + "," + label + ",";
    }
    
    public static boolean isEmpty(String path) {
        if (path == null || path.trim().equals(""))
            return true;
        return false;
    }
}
