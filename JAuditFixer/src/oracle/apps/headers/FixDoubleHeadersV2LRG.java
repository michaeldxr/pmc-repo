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

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JavaFileParserHelper;
import oracle.apps.utility.LRGUtil;

public class FixDoubleHeadersV2LRG{    
    
  private static Writer outputFileWriter;
  private static Writer multiPackagewriter;
  private static Writer outputFileWriter1;

  private static int sql_files = 0;
  private static int pkb_files = 0;
  private static int pkh_files = 0;
  
  private static String label = null;
  private static boolean filterNoShip = true;

  private static HashSet<String> exemptions = new HashSet<String>(); 
  
  private static final Pattern p_header = Pattern.compile("\\$Header.*\\$",Pattern.DOTALL);
  private static final Pattern p_split_header = Pattern.compile("^(\\s*/\\*\\s*\\$Header)"); //If header starts with /* it might be split accross lines.
  private static final Pattern p_correct_header_syntax = Pattern.compile("^(\\s*(rem|/\\*|--|//|<!--)\\s*\\$Header.*\\$)",Pattern.CASE_INSENSITIVE);
  private static final Pattern p_package = Pattern.compile("^(\\s*create\\s+or\\s+replace\\s+package(\\s+body)?\\s+(.*)\\s*(as|is))",Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
  
  private static final String issuetype = "Bad Headers";
  
  public static void main(String[] args) throws Exception{             
   
      try {
          if (args.length != 1 || args[0] == null || args[0].equals("")) {
              System.out.println("Series has to be specified for " +
                                 issuetype);
              System.exit(1);
          }

          String series = args[0];
    
          String label = LRGUtil.getLatestLabel(series);
          System.out.println("Running " + issuetype + " Scan on series: " +
                             series + ".......");
          System.out.println("Latest label is: " + label);

          outputFileWriter = new BufferedWriter(new FileWriter("BadHeaders_LRG.csv"));        
          outputFileWriter.write("Label,Family,Product,Filename,Extension,Header1,Header2,Headers Syncronized?,Header With bad syntax,# Headers, # Packages, Issue\n");    
          
          multiPackagewriter = new BufferedWriter(new FileWriter("files_with_multiple_packages.csv") ); //write only when num_headers=num_packages - so script wont detect violation - review these files manually to ensure headers are in correct location
          multiPackagewriter.write("Label,Family,Product,Filename,Extension,# Headers, # Packages,Issue\n"); 
        
          outputFileWriter1 = new BufferedWriter(new FileWriter("BadHeadersAgain_LRG.csv"));
          outputFileWriter1.write("Label,Family,Product,Filename,Extension,Header1,Header2,Headers Syncronized?,Header With bad syntax,# Headers, # Packages, Issue\n");   
          
          initializeExemptions();

          ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
          for (int j = 0; j < families.size(); j++) {
              String family = families.get(j);
              String sCrawlDir = LRGUtil.getCrawlDir(series, family, label,issuetype);
              System.out.println("Crawling dir '" + sCrawlDir + "' for family: " + family);
              
              FixDoubleHeadersV2LRG fixer = new FixDoubleHeadersV2LRG();
              fixer.crawlDirectory(sCrawlDir);
              fixer.crawlDirectoryAgain(sCrawlDir);
          }
          outputFileWriter1.close();
          outputFileWriter.close();              
          multiPackagewriter. close();

          LRGUtil.addLabelInfoToAB(issuetype, series, label);
      } catch (Exception e) {
          e.printStackTrace();
      }
  }  
  
  private static void initializeExemptions() {
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

  
  public static boolean isEmpty(String str) {
      if (str == null || str.trim().equals(""))
          return true;
      return false;
  }

}
