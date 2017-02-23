package oracle.apps.logs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.PreparedStatement;
import java.sql.Statement;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;

public class AuditResultHtmlParser {
    
  static BufferedWriter writer;
  static BufferedReader reader;
  static int row = 2;
  static HashMap<String,String> javaFilePathToFullPath = new HashMap<String,String>();
  static boolean insertIntoDB = false;
  
  static String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
  static String statsUname = "codescan";
  static String statsPwd = "codescan";
  static String module = "";
  static String family = "";
  static String codeLine = "V2";
  static String series = "";
    
  /*  <td>Rule 'contrib1-AppsLogger-LogLevel-Violation' violated: , 
   * Message="Review Log Level, should be AppsLogger.FINE/FINER/FINEST/CONFIG, but is AppsLogger.INFO.", Method="execute"</td> */
  
  static Pattern ruleMessage = Pattern.compile("<td>AppsLogger Violation: (.*?)</td>",Pattern.CASE_INSENSITIVE);
  
    public static void main(String[] args) throws Exception {
      
        if(args.length < 1) {
            System.out.println("Usage: <ade_label_server>/FATOOLS_MAIN_LINUX.rdd/LATEST/fatools/opensource/jauditFixScripts/bin/" +
                "parseHtmlAppsLoggerAuditResults.sh <path of input html file>");
            System.exit(1);
        }
        
       String inputFile = args[0].trim();
//       String inputFile = "/scratch/sudgupta/view_storage/sudgupta_ctb1_jaudit_view/fatools/output_ledger.html";
       String outputFile = "appslogger_jaudit_output.txt";
      
      if(args.length > 1)
          insertIntoDB = new Boolean(args[1].trim());
      
      if(args.length > 2)
          module = args[2].trim();

      if(args.length > 3)
          family = args[3].trim();
      
        if(args.length > 4)
            codeLine = args[4].trim();
      
      series = FamilyModuleHelper.getSeries(codeLine);
        
      writer = new BufferedWriter(new FileWriter(outputFile));
      writer.write("Family,Module,Product,Filename,Label,LineNum,Issue,Comment1,Comment2\n");
      
      System.out.println("Converting output...");
      
      File f = new File(inputFile);
      if(!f.exists()){
          System.out.println("File Not found: " + inputFile);
          System.exit(1);
      }
      
      if(f.isDirectory()) {
          File[] listOfFiles = f.listFiles();
          
          for(int i = 0; i < listOfFiles.length; i++) {              
              File f1 = listOfFiles[i];                          
              processFile(f1);                       
          }
      } else if(f.isFile()) {                 
          processFile(f);                
      } else {
          System.out.println(f.getName() + " is neither a file nor a directory !!");
          System.exit(1);
      }
      System.out.println("Done!");
      writer.close(); 
      
      if(insertIntoDB) {
          System.out.println("Inserting into DB...");
          Connection con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
          con.setAutoCommit(false);
          
          Statement stmt = con.createStatement();
          String sql = "DELETE FROM codescan_results_p WHERE SUB_ISSUE='AppsLoggerIssue' and MODULE = " + "'" + module + "'";
          if(!family.equals(""))
             sql += " and FAMILY = " + "'" + family + "'";
          sql += " and SERIES IN " + series;
          stmt.executeUpdate(sql);          
          stmt.close();
          PreparedStatement pstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS_p(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,SERIES,LABEL) " +
                "VALUES(?,?,?,?,?,?,?,?,?)");
          
          System.out.println("Now inserting values...");
          
          BufferedReader fileReader = new BufferedReader(new FileReader("appslogger_jaudit_output.txt"));
          String line = fileReader.readLine();
          
          while((line = fileReader.readLine()) != null) {
              String[] parts = line.split(",");
                if(parts.length < 9) {
                    System.out.println("Skipping: " + line);
                    continue;
                }
              String issue = "";
              String description = "";
              if(parts[6].trim().contains("does not match")) {
                  issue = "Log Level If Mismatch";
                  description = parts[5].trim() + ":" + parts[7].trim() + "," + parts[8].trim();
              }
              if(parts[6].trim().equals("No 'AppsLogger.isEnabled()' check.")) {
                  issue = "No AppsLogger.isEnabled() check";
                  description = parts[5].trim();
              }
              if(parts[6].trim().contains("Incorrect AppsLogger module param")) {
                  issue = "Incorrect Module Parameter";
                  description = parts[5].trim() + ":" + parts[7].trim() + "," + parts[8].trim();
              }
              if(parts[6].trim().contains("Review Log Level")) {
                  issue = "SEVERE Log Level";
                  description = parts[5].trim() + ":Loglevel " + parts[7].trim() + " " + parts[8].trim();
              }
              
              if(issue.equals(""))
                  continue;
              
              String[] labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
              pstmt.setString(1, parts[0].trim()); //family
              pstmt.setString(2, parts[1].trim()); //module
              pstmt.setString(3, parts[2].trim()); //product
              pstmt.setString(4, parts[3].trim()); //filename 
              pstmt.setString(5, issue); //issue     
              pstmt.setString(6, "AppsLoggerIssue"); //subissue              
              pstmt.setString(7, description); //Description        
              
              pstmt.setString(8, labelSeries[0]); //series
              pstmt.setString(9, labelSeries[1]); //label
              pstmt.execute();            
          }
          con.commit();
          con.close();
          pstmt.close();
      }
    }
    
    private static void processFile( File inputFile) {
        
        if(!inputFile.getName().endsWith(".html"))
            return;         
        
        try{
        
        reader = new BufferedReader(new FileReader(inputFile));     
          
        String line= "";
        int lineNo = 0;
        String javaFileFullPath ="";
        String javaFileName = "";
        String family = "";
        String module = "";
        String product = "";
        Pattern p = null;
        String label = "";
        
        while((line = reader.readLine()) != null) {
          
           //step 1, parse java files and get the URL of each java file
           if (line.contains(".java")) {
               //if the violation from previous list still exists, report them and clear the list
               //parse java file name and location
               p = Pattern.compile("</*h2>");
               String[] javaFile = p.split(line);
               if (javaFile.length == 2) {
                   javaFileFullPath = javaFile[1];
               }
               if(label.equals(""))
                   label = getLabel(javaFileFullPath);
               
               if(javaFileFullPath.contains("/fusionapps/")){
                 javaFileName = "fusionapps/" + javaFileFullPath.split("/fusionapps/",2)[1];
                 String[] parts = javaFileName.split("/", 6);
                 family = parts[1].toUpperCase();
                 module = parts[3];
                 product = parts[4].toUpperCase();
               }             
               else if(javaFileFullPath.contains("/fsm/")){
                 javaFileName = "fsm/" + javaFileFullPath.split("/fsm/",2)[1];
                 String[] parts = javaFileName.split("/", 6);
                 family = parts[0].toUpperCase();
                 module = parts[2];
                 product = parts[3].toUpperCase();
               }  
               else if(javaFileFullPath.contains("/atgpf/")){
                     javaFileName = "atgpf/" + javaFileFullPath.split("/atgpf/",2)[1];
                     String[] parts = javaFileName.split("/", 6);
                     family = "ATGPF";
                     module = parts[0];
                     product = parts[2].toUpperCase();
               }  
           }
            
            javaFilePathToFullPath.put(javaFileName,javaFileFullPath);

           //find line number of the violations in the html file
           if (line.contains("td class=\"line\"")) {
               p = Pattern.compile("</*td[\\sclass=\"line\"]*>");
               String sLine = p.split(line)[1];
               lineNo = Integer.parseInt(sLine);
           }
           //find member variable info in the html file
        
          Matcher m = ruleMessage.matcher(line);
          if (m.find()) {
               String issue = m.group(1);
           
               String[] parts = issue.split(", ");
               if(parts.length == 1)
                   writer.write(family + "," + module + "," + product + "," + javaFileName + "," + label + "," 
                         + lineNo + "," + parts[0] + ", , \n");           
               else if(parts.length == 3) {
                   writer.write(family + "," + module + "," + product + "," + javaFileName + "," + label + "," 
                            + lineNo + "," + parts[0] + "," + parts[1] + "," + parts[2] + "\n");      
               }
          }
        }
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Skipping File: " + inputFile.getAbsolutePath());
        }
        
    }
    
    private static String getLabel(String javaFileFullPath) throws Exception {
        
        String label = "";
        File f = new File(javaFileFullPath);
        String absPath = f.getCanonicalPath();
        int i = 0;
        
        if(absPath.contains("fusionapps/")) {
         i = absPath.indexOf("fusionapps/");
        }
        if(absPath.contains("fsm/")){
         i = absPath.indexOf("fsm/");
        }
        if (absPath.contains("atgpf/")) {
          i = absPath.indexOf("atgpf/");
        }
        
        String path_before_fusionapps = absPath.substring(0, i-1);
        String[] parts = path_before_fusionapps.split("/");
        if(parts!=null && parts.length>=2) {
          label = parts[parts.length -2] + "_" + parts[parts.length -1];      
          label = label.replace(".rdd","");   
        }    
        
        return label;
    }
    
    private static boolean isEmpty(String s) {
        if(s == null || s.equals(""))
            return true;
        return false;
    }
    
}
