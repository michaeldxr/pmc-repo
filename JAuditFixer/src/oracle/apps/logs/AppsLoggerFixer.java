package oracle.apps.logs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.JavaFileParserHelper;
import oracle.apps.helpers.Mail;

public class AppsLoggerFixer {
    
    static String logLevelViolationsFile = null;
    static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
    static Writer errorWriter;
    static int num_logLevel_fixes;
    static int num_logLevel_errors;
    static String viewRoot;
    
    Pattern pAppsLogger = Pattern.compile("AppsLogger.write\\s*\\((.*?),(.*),(.*?)\\)",Pattern.DOTALL);
    Pattern pAppsLoggerIfEnabled = Pattern.compile("if\\s*\\(\\s*AppsLogger.isEnabled\\s*\\((.*?)\\)\\s*\\)");
    
    public static void main(String[] args) throws Exception{
        
        viewRoot = args[0].trim() + "/";
      //  viewRoot = "/scratch/sudgupta/view_storage/sudgupta_fusionapps_rup1_sep14_view/";
        
        if(args.length < 2) {
            System.out.println("path to csv file containing the loglevel violations must be specified.");
            System.exit(1);
        }
        logLevelViolationsFile = args[1].trim();
        //logLevelViolationsFile = "/scratch/sudgupta/AppsLogger_scan.csv";
        File f = new File(logLevelViolationsFile);
        if(!f.exists()) {
            System.out.println("csv file containing the loglevel violations does not exist. Please specify valid filepath.");
            System.exit(1);
        }
        
        errorWriter = new BufferedWriter(new FileWriter("ReviewManually.txt"));
        
        diffHelper.startDiffLog();
        
        AppsLoggerFixer fixer = new AppsLoggerFixer();
        
        System.out.println("Making fixes...");
        fixer.makeFixes();
        
        diffHelper.closeDiffLog(); 
        errorWriter.close();
        
        String summary = "";
        summary += "\nNumber of logLevel fixes: "+ num_logLevel_fixes;
        summary += "\nNumber of logLevels that could not be fixed: "+ num_logLevel_errors;
        
        System.out.println(summary); 
        
    }
    
    private void makeFixes() throws Exception{
        
        
            BufferedReader csvReader2 = new BufferedReader(new FileReader(logLevelViolationsFile));
            String line = csvReader2.readLine();
            String currentFileName = "";
            String prevFileName = "";
            ArrayList<String> lines = new ArrayList<String>();
            String fileAbsPath = "";
            JavaFileParserHelper javaHelper = null;
            String[] fileContents = null;
            String newFileContents = null;                        
      
            while((line = csvReader2.readLine()) != null) {
                
                String[] parts = line.split(",");                   
                if(parts.length < 10)
                    continue;
                currentFileName = parts[3].trim();
                if(prevFileName.equals(""))
                    prevFileName = currentFileName;
                
                if(currentFileName.equals(prevFileName))
                    lines.add(line);
                else {
                    fileAbsPath = viewRoot + prevFileName;
                    javaHelper = new JavaFileParserHelper(fileAbsPath);
                    fileContents = javaHelper.getFileContents();
                    fileContents = fixLogLevel(lines, fileContents);
                    newFileContents = javaHelper.getFileContentsAsString(fileContents, false);
                    applyFix(fileAbsPath, newFileContents);
                    
                    lines = new ArrayList<String>();
                    lines.add(line);
                }
                prevFileName = currentFileName;                  
            } //while
            
            //process the last group
            if(prevFileName != null) {
                fileAbsPath = viewRoot + prevFileName;
                javaHelper = new JavaFileParserHelper(fileAbsPath);
                fileContents = javaHelper.getFileContents();
                fileContents = fixLogLevel(lines, fileContents);
                newFileContents = javaHelper.getFileContentsAsString(fileContents, false);
                applyFix(fileAbsPath, newFileContents);
            }
          
          csvReader2.close();                
        }
    
    private String[] fixLogLevel(ArrayList<String> lines, String[] fileContents) throws Exception {    
                  
        for(Iterator<String> it = lines.iterator(); it.hasNext();) {
            String line = it.next();
            String[] parts = line.split(",");
            String newLogLevel = parts[7].trim();
            if(newLogLevel.equals(""))
                continue;
            
            int lineNo = Integer.parseInt(parts[5].trim())-1;
            int methodStartLineNo = Integer.parseInt(parts[9].trim())-1;
            int endStmtLineNo = lineNo;
            String appsLoggerStmt = fileContents[lineNo];
            
             if(!appsLoggerStmt.contains("AppsLogger.write")) {
                errorWriter.write("Line No is messed up..Cannot fix LogLevel: " + line + "\n");
                num_logLevel_errors++;
                continue;
             }
            
             while(!appsLoggerStmt.trim().endsWith(";"))
               appsLoggerStmt += fileContents[++endStmtLineNo];
            
             Matcher m = pAppsLogger.matcher(appsLoggerStmt);
            
             if(m.find()) {
                String oldLogLevel = m.group(3);
                int startMatch = m.start(3);
                appsLoggerStmt = appsLoggerStmt.substring(0, startMatch) + newLogLevel + appsLoggerStmt.substring(startMatch + oldLogLevel.length());
                //appsLoggerStmt = appsLoggerStmt.replace(oldLogLevel, " " + newLogLevel);
                 fileContents[lineNo] = appsLoggerStmt;
                 for(int i = lineNo+1; i <= endStmtLineNo; i++)
                    fileContents[i] = "";
                 
                    //Fix the isEnabledcheck
                     boolean isEnabledCheckFixed = false;
                     for(int i = lineNo; i >= methodStartLineNo; i--){
                         Matcher m1 = pAppsLoggerIfEnabled.matcher(fileContents[i]);
                         if(m1.find()) {                        
                             fileContents[i] = fileContents[i].replace(m1.group(1), newLogLevel);
                             isEnabledCheckFixed = true;
                             break;
                         }
                     }                             
                     if(!isEnabledCheckFixed) {
                         errorWriter.write("Corresponding isEnabled check could not be fixed..Please review and fix manually if needed" + line + "\n");
                         num_logLevel_errors++;
                         continue;
                     }
                 num_logLevel_fixes++;
             }
             else{
                 errorWriter.write("AppsLogger write pattern not found. Cannot fix:" + line + "\n");
                 num_logLevel_errors++;
                 continue;
             }                     
        }// for
            return fileContents;          
    }
    
    private  void applyFix(String absPath,
                        String newFileContents) throws Exception {
       
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
    }
}
