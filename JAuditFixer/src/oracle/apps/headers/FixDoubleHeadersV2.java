package oracle.apps.headers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JavaFileParserHelper;

public class FixDoubleHeadersV2{    
    
  static Writer outputFileWriter;
  static Writer fileWriter;
  static Writer writer;
  static Writer multiPackagewriter;
  
  static Writer outputFileWriter1;
      
  static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
  
  static int sql_files = 0;
  static int pkb_files = 0;
  static int pkh_files = 0;
  static int java_files = 0;
  static int xml_files = 0;
  static int xlf_files = 0;
  
  static String sCrawlDir = null;
  static String ade_view_root = null;
  static String label = null;
  static boolean makeFix = true;
  static boolean filterNoShip = true;
  
  static boolean bDebug = false;
  
  static String summary = "";
  
  HashSet<String> exemptions = new HashSet<String>(); 
  
  Pattern p_header = Pattern.compile("\\$Header.*\\$",Pattern.DOTALL);
  Pattern p_split_header = Pattern.compile("^(\\s*/\\*\\s*\\$Header)"); //If header starts with /* it might be split accross lines.
  Pattern p_correct_header_syntax = Pattern.compile("^(\\s*(rem|/\\*|--|//|<!--)\\s*\\$Header.*\\$)",Pattern.CASE_INSENSITIVE);
  Pattern p_package = Pattern.compile("^(\\s*create\\s+or\\s+replace\\s+package(\\s+body)?\\s+(.*)\\s*(as|is))",Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
  static Pattern p_labelPattern = Pattern.compile(".*/(.*?\\.rdd/.*)");
  
  public static void main(String[] args) throws Exception{             
   
       if (args.length <2 || isEmpty(args[0]) || isEmpty(args[1])) {
           System.out.println("Usage:  FixBadHeaders.sh <path to your code in ade> <ade_view_root> <makeFix=true/false>");
           System.exit(1);
       }
      sCrawlDir = args[0];
      //sCrawlDir = "/ade.../FSM_MAIN_GENERIC.rdd/LATEST/fsm";
      
     ade_view_root = args[1];
     //ade_view_root = "/ade/sudgupta_finv2a_view";
     
     File f1 = new File(ade_view_root);
     String viewlabel = f1.getCanonicalPath();
     System.out.println("ViewLabel: " + viewlabel);
     Matcher m = p_labelPattern.matcher(viewlabel);
     if(m.find()){
         label = m.group(1);
         label = label.replace("/","_");
         label = label.replace(".rdd","");
     }
     
     System.out.println("Crawl dir: " + sCrawlDir);
     System.out.println("ade_view_root: " + ade_view_root);
       System.out.println("Label: " + label);
     
      if(args.length > 2)
        makeFix = new Boolean(args[2]);
      
     boolean append = false;
     if(args.length > 3)
        append = new Boolean(args[3].trim());
     
      diffHelper.startDiffLog();
      
      if (sCrawlDir == null || sCrawlDir.trim().equals("")) {
          System.out.println("Crawl directory has to be specified");
          System.exit(1);
      } 
      
      File f = new File(sCrawlDir);      
      
      sCrawlDir = f.getCanonicalPath();
     
    System.out.println("Analyzing all files in directory:" + sCrawlDir);      

    FixDoubleHeadersV2 fixer = new FixDoubleHeadersV2();
    fixer.initializeExemptions();

    try {         
        outputFileWriter = new BufferedWriter(new FileWriter("headers_scan.csv", append));        
        if(!append)
           outputFileWriter.write("Label,Family,Product,Filename,Extension,Header1,Header2,Headers Syncronized?,Header With bad syntax,# Headers, # Packages, Issue\n");    
       
        multiPackagewriter = new BufferedWriter(new FileWriter("files_with_multiple_packages.csv") ); //write only when num_headers=num_packages - so script wont detect violation - review these files manually to ensure headers are in correct location
        multiPackagewriter.write("Label,Family,Product,Filename,Extension,# Headers, # Packages,Issue\n"); 
        
        outputFileWriter1 = new BufferedWriter(new FileWriter("headers_scan_bad_location.csv",append) );
        if(!append)
          outputFileWriter1.write("Label,Family,Product,Filename,Extension,Header1,Header2,Headers Syncronized?,Header With bad syntax,# Headers, # Packages, Issue\n");   
        
        fixer.crawlDirectory(sCrawlDir);       
        
        outputFileWriter.close();
        
        fileWriter = new BufferedWriter(new FileWriter("errors.csv"));
        fileWriter.write("Label,Family,Product,Filename,Extension,Header1,Header2,Headers Syncronized?,Header With bad syntax,# Headers, # Packages,Issue\n");
        writer = new BufferedWriter(new FileWriter("FilesWhereSecondHeaderWasNeutered.txt"));
        
        if(makeFix)
          fixer.fixBadHeaders();
        
        fixer.crawlDirectoryAgain(sCrawlDir);
        
        outputFileWriter1.close();
        
        if(makeFix)
          fixer.fixBadHeaderLocation();
        
        diffHelper.closeDiffLog(); 
        fileWriter.close();
        writer.close();
        multiPackagewriter. close();
        
        summary += "\nCrawl Directory: " + sCrawlDir;
        summary += "\nNumber of sql files processed: "+ sql_files;
        summary += "\nNumber of pkb files processed: "+ pkb_files;
        summary += "\nNumber of pkh files processed: "+ pkh_files;
     
        System.out.println(summary);       
      
    } catch (Exception e) {
        e.printStackTrace();      
        String summary = "CrawlDirectory: " + sCrawlDir + "\nException: " + e.getMessage(); 
        //Mail.sendMail("sudipti.gupta@oracle.com", summary, "SCRIPT: to fix bad SQL headers failed ");
    }
  }  
  
  public void initializeExemptions() {
    exemptions.add("fusionapps/fin/fa/db/plsql/fa_xla_cmp_extract_pkg.pkb");
    exemptions.add("fusionapps/fin/xla/db/plsql/xla_cmp_tab_pkg.pkb");
    exemptions.add("fusionapps/hcm/ff/db/plsql/ff_wrapper_sup_pkg.pkb");
    exemptions.add("fusionapps/hcm/ff/db/sql/ff_wrapper_dyd_pre_pkgdyn.sql");
    exemptions.add("fusionapps/scm/doo/db/plsql/doo_pc_conc_requests.pkb");
  }
    
  private void crawlDirectory(String path) throws Exception
  {
      if(path.contains(".ade_path") || path.contains("/classes/"))
          return;
      if(path.contains("/noship") && filterNoShip)
          return;

      File folder = new File(path);
      File[] listOfFiles = folder.listFiles();
          
      if(listOfFiles == null)
          return;
      
      for(int i = 0; i <listOfFiles.length; i++)
      {
          if(listOfFiles[i].isFile()) 
          {
              String sName = listOfFiles[i].getAbsolutePath();              
              if(fileOfInterest(sName))               
                  processFile(listOfFiles[i]);                
          }
          else if(listOfFiles[i].isDirectory()) 
              crawlDirectory(listOfFiles[i].getAbsolutePath());          
          else 
              System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);          
      }      
  }
  
  private void crawlDirectoryAgain(String path) throws Exception
  {
      if(path.contains(".ade_path") || path.contains("/classes/"))
          return;
      if(path.contains("/noship") && filterNoShip)
          return;

      File folder = new File(path);
      File[] listOfFiles = folder.listFiles();
   
      if(listOfFiles == null)
          return;
      
      for(int i = 0; i <listOfFiles.length; i++)
      {
          if(listOfFiles[i].isFile()) 
          {
              String sName = listOfFiles[i].getAbsolutePath();              
              if(fileOfInterest(sName))               
                  processPkg(listOfFiles[i]);                
          }
          else if(listOfFiles[i].isDirectory()) 
              crawlDirectoryAgain(listOfFiles[i].getAbsolutePath());          
          else 
              System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);          
      }      
  }

  public boolean fileOfInterest(String sName) 
  {
      String pathAfterViewRoot = FamilyModuleHelper.getPathAfterViewRoot(sName);
      if(exemptions.contains(pathAfterViewRoot))
          return false;
      String absPathLowerCase = sName.toLowerCase();
      if (absPathLowerCase.contains("test.") || absPathLowerCase.contains("/test/") 
          || absPathLowerCase.contains("modeltest") || absPathLowerCase.contains("/uitest")
          || absPathLowerCase.contains("publicuitest") || absPathLowerCase.contains("publicmodeltest")
          || absPathLowerCase.contains("/testui")  || absPathLowerCase.contains("servicetest") 
          || absPathLowerCase.contains("/publicservicetest")
          || absPathLowerCase.contains("/noship"))
        return false;

      if (sName.endsWith(".pkb")) {
          pkb_files++;
          return true;          
      }
      if (sName.endsWith(".pkh")) {
          pkh_files++;
          return true;
      }          
      if (sName.endsWith(".sql")){
          sql_files++;
          return true;
      }
      return false;
  }
  
  public boolean processFile(File f) throws Exception {
    
    String absPath = f.getCanonicalPath();
    BufferedReader reader = new BufferedReader(new FileReader(absPath));
    String line = "";
    ArrayList<String> headers = new ArrayList<String>(); 
    int num_headers = 0;
    Matcher m;
    Matcher m2;
    String headerWithBadSyntax = "";   
    
    boolean result = false;
    
    while((line = reader.readLine()) != null) {    
        
      boolean multiline = false;
        
      Matcher m_multiline_header = p_split_header.matcher(line);
      if(m_multiline_header.find()){
          while(!line.contains("*/")) {
            line += reader.readLine();
            multiline = true;
          }
      }
      
      m = p_header.matcher(line);
        
      if(m.find()) {
        String headerline = m.group();
        headerline = headerline.replace(",", "#COMMA");
        headers.add(headerline);
        num_headers++;
        m2 = p_correct_header_syntax.matcher(line);
        if(!m2.find() && !line.trim().startsWith("$Header"))  {
            headerWithBadSyntax += num_headers + " ";
        } else if(multiline)
            headerWithBadSyntax += num_headers + " ";
      }      
     // if(num_headers >= 2) break;
    }
    headerWithBadSyntax = headerWithBadSyntax.trim();
    int num_packages = getNumPackagesInFile(f);
    
    if(num_headers >= 2 && num_headers > num_packages) {
        outputFileWriter.write(getFileNameInfo(absPath) + headers.get(0) + "," + headers.get(1) +","
                               + headers.get(0).equals(headers.get(1)) + "," + headerWithBadSyntax + ","
                               + num_headers + "," + num_packages + ",Bad Headers\n");     
        result = true;
    } else if(!headerWithBadSyntax.equals("")) {
        outputFileWriter.write(getFileNameInfo(absPath) + headers.get(0) + ", , ," 
                             + headerWithBadSyntax + "," + num_headers + "," + num_packages + ",Bad Headers\n");   
        result = true;
    } else if(num_headers < num_packages) {
        outputFileWriter.write(getFileNameInfo(absPath) + " , , ," 
                         + headerWithBadSyntax + "," + num_headers + "," + num_packages + ",Bad Headers\n");
        result = true;
    } else if(num_headers == 0) {
        outputFileWriter.write(getFileNameInfo(absPath)  + " , , ," 
                         + headerWithBadSyntax + "," + num_headers + "," + num_packages + ",Bad Headers\n");  
        result = true;
    } else if(num_packages > 1) {
        multiPackagewriter.write(getFileNameInfo(absPath) + num_headers + "," + num_packages + ",Bad Headers\n"); 
        result = true;
    }
    return result;
  }
  
  public void processPkg(File f) throws Exception {
       
       String absPath = f.getCanonicalPath();
       
       if(absPath.endsWith(".sql")) return;
        
       JavaFileParserHelper fileHelper = new JavaFileParserHelper(f.getCanonicalPath());
       String fileContentsAsString = fileHelper.getFileContentsAsString(false);
       
       int index = -1;
       String header="";
       
       Matcher m1 = p_package.matcher(fileContentsAsString);
       if(m1.find())
           index = m1.end();     
         
       if(index == -1) return;
      
       Matcher m2 = p_header.matcher(fileContentsAsString.substring(index));
           
       if(!m2.find()) {                  
         m2 = p_header.matcher(fileContentsAsString.substring(0,index));
         if(m2.find())
             header = m2.group();
         
         outputFileWriter1.write(getFileNameInfo(absPath) + header.replace(",", "#COMMA") + ", , , ," +
                                 getNumHeadersInFile(f) + "," + getNumPackagesInFile(f) + ",Bad Header Location\n");        
       }     
    }
  
  private int getNumPackagesInFile(File f) throws Exception{
      
    String absPath = f.getCanonicalPath();
    if(absPath.endsWith(".sql")) return 0;
     
    JavaFileParserHelper fileHelper = new JavaFileParserHelper(absPath);
    String fileContentsAsString = fileHelper.getFileContentsAsString(false);
    
    int index = -1;
    int num_packages = 0;
    
    Matcher m1 = p_package.matcher(fileContentsAsString);
    while (m1.find()) {
        
        index = m1.end();     
        num_packages++;
        if(index == -1) return num_packages;
        fileContentsAsString = fileContentsAsString.substring(index);
        m1 = p_package.matcher(fileContentsAsString);
    }
    return num_packages;
  }
  
  private int getNumHeadersInFile(File f) throws Exception{
      
    String absPath = f.getCanonicalPath();
     
    JavaFileParserHelper fileHelper = new JavaFileParserHelper(absPath);
    String fileContentsAsString = fileHelper.getFileContentsAsString(false);
    
    int index = -1;
    int num_headers = 0;
    
    Matcher m1 = p_header.matcher(fileContentsAsString);
    while (m1.find()) {
        
        index = m1.end();     
        num_headers++;
        if(index == -1) return num_headers;
        fileContentsAsString = fileContentsAsString.substring(index);
        m1 = p_header.matcher(fileContentsAsString);
    }
    return num_headers;
  }    
  
  private String getFileNameInfo(String absPath) {
      
   //   System.out.println("AbsPath: " + absPath);
      
    int i = 0;
    String family = "";
    String product = "";
    
    String blankString =  "NA,NA,NA,NA,NA,";
    if(isEmpty(absPath))
        return blankString;
    
    if(absPath.contains("/fusionapps")) {
     i = absPath.indexOf("/fusionapps");
    }
    if(absPath.contains("/fsm")){
     i = absPath.indexOf("/fsm");
     family = "FSM";
    }
      if (absPath.contains("/atgpf")) {
          i = absPath.indexOf("/atgpf");
          family = "ATGPF";
      }
      
      if(absPath.contains("/emcore")) {
          i = absPath.indexOf("/emcore");
          family = "EMGC";   
          product = "EMCORE";
      }
      
    String pathAfterFusionApps = absPath.substring(i+1);
    String[] parts = pathAfterFusionApps.split("/");

    
    if(parts == null) return blankString;
    
    if(absPath.contains("/fusionapps")){
        if(parts.length > 2)
          product = parts[2].trim().toUpperCase();
        if(parts.length > 1)
          family = parts[1].trim().toUpperCase();
    }
    if(absPath.contains("/fsm") && parts.length > 1)
        product = parts[1].trim().toUpperCase();
    
      if(absPath.contains("/atgpf") && parts.length > 1)
          product = parts[1].trim().toUpperCase();
      
    String filename = parts[parts.length -1];
    String[] filename_parts = filename.split("\\.");
    String extension = " ";
    if(filename_parts.length == 2) extension = filename_parts[1];    
    
    return label + "," + family + "," + product + "," + pathAfterFusionApps + "," + extension + ",";
  }
  
  private void fixBadHeaders() throws Exception {
      
    System.out.println("Fixing Double Headers...");
    
    String filename = "headers_scan.csv";
    if(bDebug)
      System.out.println(filename);
  
    BufferedReader fileReader = new BufferedReader(new FileReader(filename));
    
    String line = fileReader.readLine(); //first line is the header - read & ignore
    
    String fileContentsAsString = null;
    
    String pathPrefix = ade_view_root + "/";
    
    JavaFileParserHelper fileHelper = null;
    
    while((line = fileReader.readLine()) != null) {
      
      if(bDebug)
        System.out.println("line from csv File:" + line);
        
      String[] parts = line.split(",");
      if(parts.length < 11) {
        fileWriter.write(line + "\n");
        continue;
      }
      String filePath = pathPrefix + parts[3].trim();
      String extension = parts[4].trim();
      String header2 = parts[6].replace("#COMMA",",").trim();
      String header1 = parts[5].replace("#COMMA",",").trim();
      int num_headers = Integer.parseInt(parts[9].trim());
      String headersWithBadSyntax = parts[8].trim();
      int num_packages = Integer.parseInt(parts[10].trim());
        
      if(num_headers == 0) { 
        fileWriter.write(line + "\n");
        continue;
      }
        
      if(num_headers < num_packages) { 
        fileWriter.write(line + "\n");
        continue;
      }
        
      if(bDebug){
        System.out.println("filePath:" + filePath);
        System.out.println("Header2:" + header2);
      }
        
      try {
        fileHelper = new JavaFileParserHelper(filePath);
        fileContentsAsString = fileHelper.getFileContentsAsString(false);          
        int index = -1;
          
        if(num_headers == 2) {            
                
          String[] badHeaders = headersWithBadSyntax.split(" ");
          
          if(badHeaders.length > 1) { //More than one header has bad syntax
            fileWriter.write(line+ "\n");
            continue;
          }         
          
          if(badHeaders[0].equals("2")) { //neuter the 2nd header only if it has bad syntax (this will probably never happen)
              index = fileContentsAsString.lastIndexOf(header2);
              if(index == -1) {
                fileWriter.write(line + "\n");
                continue;
              }
              fileContentsAsString = fileContentsAsString.substring(0, index) + fileContentsAsString.substring(index).replace(header2, " ");
              applyFix(filePath, fileContentsAsString);
              writer.write(line);
          }
          else { //always neuter the first header unless it is the 2nd one with the bad syntax
              index = fileContentsAsString.indexOf(header1);    
              if(index == -1) {
                fileWriter.write(line + "\n");
                continue;
              }
              index += header1.length();
              fileContentsAsString = fileContentsAsString.substring(0,index+1).replace(header1, " ") + fileContentsAsString.substring(index+1) ;
              applyFix(filePath, fileContentsAsString);
          }
        }        
        else if(num_headers==1) { //there is one header  => it has bad syntax -> make sure fix always writes it to correct location
            if(extension.equals("sql")) {
              fileContentsAsString = "/*" + header1 + "*/\n" + fileContentsAsString.replace(header1, " ");
              applyFix(filePath, fileContentsAsString);
            }
            else {
              Matcher m = p_package.matcher(fileContentsAsString);
              if(m.find())
                  index = m.end();     
              
              if(index == -1) {
                fileWriter.write(line + "\n");
                continue;
              }                
              fileContentsAsString = fileContentsAsString.substring(0,index).replace(header1, " ") + 
                                     "\n/*" + header1 + "*/\n" + fileContentsAsString.substring(index).replace(header1, " ");
              applyFix(filePath, fileContentsAsString);
            }
        }        
        else if(num_headers > 2 && headersWithBadSyntax.equals("")) {
            int n = 1;
            if(num_packages > 1) n = num_packages;
            for(int i = n; i < num_headers; i++) {
              index = fileContentsAsString.indexOf(header1);   
              if(index == -1) {
                fileWriter.write(line + "\n");
                continue;
              }
              index += header1.length();
              fileContentsAsString = fileContentsAsString.substring(0,index+1).replace(header1, " ") + fileContentsAsString.substring(index+1) ;
            }
            applyFix(filePath, fileContentsAsString);
        }        
        else { 
          fileWriter.write(line + "\n");
        }
        
      } catch (Exception e) {
            System.out.println("Some exception occured while processing file: " + filePath);
            e.printStackTrace();
            System.out.println("Skipping to next file...");
            fileWriter.write(line + "\n");
      }
    }
    if(fileReader!=null)
        fileReader.close();    
  }
  
  private void fixBadHeaderLocation() throws Exception {
      
    System.out.println("Fixing Double Headers...");
    
    String filename = "headers_scan_bad_location.csv";
    if(bDebug)
      System.out.println(filename);
  
    BufferedReader fileReader = new BufferedReader(new FileReader(filename));
    
    String line = fileReader.readLine(); //first line is the header - read & ignore
    
    String fileContentsAsString = null;
    
    String pathPrefix = ade_view_root + "/";
    
    JavaFileParserHelper fileHelper = null;
    
    while((line = fileReader.readLine()) != null) {
      
      if(bDebug)
        System.out.println("line from csv File:" + line);
        
      String[] parts = line.split(",");
        //outputFileWriter1.write("Label,Family,Product,Filename,Extension,Header,# Headers, # Packages\n"); 
      if(parts.length < 11) {
        fileWriter.write(line + "\n");
        continue;
      }
      String filePath = pathPrefix + parts[3].trim();
      String header = parts[5].replace("#COMMA",",").trim();
      int num_headers = Integer.parseInt(parts[9].trim());
      int num_packages = Integer.parseInt(parts[10].trim());
        
      if(num_headers != 1) { 
        fileWriter.write(line + "\n");
        continue;
      }
        
      if(num_packages > 1) { 
        fileWriter.write(line + "\n");
        continue;
      }
        
      //We are here => file has one header and one package and header is in bad location
      if(bDebug){
        System.out.println("filePath:" + filePath);
        System.out.println("Header:" + header);
      }
        
      try {
        fileHelper = new JavaFileParserHelper(filePath);
        fileContentsAsString = fileHelper.getFileContentsAsString(false);          
        int index = -1;             
       
        Matcher m = p_package.matcher(fileContentsAsString);
        if(m.find())
            index = m.end();     
        
        if(index == -1) {
          fileWriter.write(line + "\n");
          continue;
        }                
        fileContentsAsString = fileContentsAsString.substring(0,index).replace(header, " ") + 
                               "\n/*" + header + "*/\n" + fileContentsAsString.substring(index).replace(header, " ");
        applyFix(filePath, fileContentsAsString);    
            
      } catch (Exception e) {
            System.out.println("Some exception occured while processing file: " + filePath);
            e.printStackTrace();
            System.out.println("Skipping to next file...");
            fileWriter.write(line + "\n");
      }
    }
    if(fileReader!=null)
        fileReader.close();    
  }
  
  
  private void applyFix(String absPath,
                      String newFileContents) throws Exception {
     if(bDebug)
         System.out.println("applying Fix to file: "+ absPath);
     File file = new File(absPath);
     if (!file.exists()) {
         throw new Exception("while making fix, file not found: " +
                             absPath);
     }
     diffHelper.checkoutAndDelete(file);
      
     System.out.println("Writing new file...");
     FileWriter fw = new FileWriter(file.getAbsolutePath());
     BufferedWriter bw = new BufferedWriter(fw);
     bw.write(newFileContents);
     if (bw != null)
         bw.close();
     if (fw != null)
         fw.close();
     diffHelper.checkinAndDiff(file);
     //diffHelper.diff(file);
  }
  
  public static boolean isEmpty(String str) {
      if (str == null || str.trim().equals(""))
          return true;
      return false;
  }

}
