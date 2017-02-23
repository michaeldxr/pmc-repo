package oracle.apps.logs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;

import java.io.FileReader;
import java.io.FileWriter;

import java.io.Writer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;

import java.util.regex.Pattern;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.JavaFileParserHelper;
import oracle.apps.helpers.Mail;

public class ScanAppsLogger {
    
   // static Workbook m_myBook;
   // static Worksheet m_attributesSheet;
    static int java_files = 0;
  //  static int row = 2;
    static Writer errorWriter;
    static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
    static String summary = "";
    static int num_module_fixes;
    static int num_module_errors;
 
    static String viewRoot;
    static String fixMode = "none";
    
    static BufferedWriter csvWriter1;
   // static BufferedWriter csvWriter2;
    
    Pattern pMethodName = Pattern.compile("(?:(?:public|private|protected|static)\\s+)*\\s*\\w+(?:\\[\\])?(?:<.*>)?\\s+(\\w+)\\s*\\(.*\\).*?\\{",Pattern.DOTALL);
    Pattern pAppsLogger = Pattern.compile("AppsLogger.write\\s*\\((.*?),(.*),(.*?)\\)",Pattern.DOTALL);
    
    Pattern pClassName = Pattern.compile("(?:class|enum)\\s+(\\w+).*?\\{",Pattern.DOTALL);
    
    Pattern pPackageName = Pattern.compile("package\\s+(.*?);");
    
    Pattern pMemberVariableDeclaration = Pattern.compile("(?:(?:public|private|protected|transient|static|final)\\s+)*(\\w+)\\s+(\\w+)\\s*=\\s*(.*?);"); 
    Pattern pLocalStringVariableDeclaration = Pattern.compile("String\\s+(\\w+)\\s*=\\s*(.*?);"); 
    
    //String[] logLevels = {"AppsLogger.FINER","AppsLogger.FINE","AppsLogger.FINEST","AppsLogger.CONFIG","FINER","FINE","FINEST","CONFIG"};    
   // HashSet allowedLogLevels = new HashSet(Arrays.asList(logLevels));
    private HashMap<String,String> memberVariables = new HashMap<String,String>();
    private HashMap<String,String> localVariables = new HashMap<String,String>();
    
    static boolean append = false; 
    
    public static void main(String[] args) throws Exception {        
        
        if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }
        
        String sCrawlDir = args[0].trim();       
       
//        String sCrawlDir = "/ade../FUSIONAPPS_MAIN_LINUX.X64.rdd/LATEST/fusionapps/scm/components/orderOrchestration/doo/" +
//            "orchestration/processPlanning/uiModel/src/oracle/apps/scm/doo/orchestration/processPlanning/uiModel/model/applicationModule";
        viewRoot = getViewRoot(sCrawlDir);
               
       if(args.length > 1)
           fixMode = args[1].trim();
       if(!fixMode.equals("none") && !fixMode.equals("module")){
           System.out.println("1st Argument of the script is the FixMode which can only be 'none' or 'module'");
           System.exit(1);
       }       
       
       if(args.length > 2)
           append = new Boolean(args[2].trim());
        
        /*m_myBook = new Workbook("AppsLogger Scan");

        m_attributesSheet = new Worksheet("AppsLogger Statements");
        Worksheet validationSheet = new Worksheet("Validation");
        
        m_myBook.addWorksheet(m_attributesSheet);
        m_myBook.addWorksheet(validationSheet);
        
        validationSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("AppsLogger.FINE")));
        validationSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("AppsLogger.FINER")));
        validationSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("AppsLogger.FINEST")));
        validationSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("AppsLogger.CONFIG")));

        m_attributesSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Family")));
        m_attributesSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Module")));
        m_attributesSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Product")));
        m_attributesSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("FileName")));
        m_attributesSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Label")));
        m_attributesSheet.addCell(new Cell(new CellLocation("F1"), new CellValue("Line No")));
        m_attributesSheet.addCell(new Cell(new CellLocation("G1"), new CellValue("Logging Level")));
        m_attributesSheet.addCell(new Cell(new CellLocation("H1"), new CellValue("New Logging Level")));
        m_attributesSheet.addCell(new Cell(new CellLocation("I1"), new CellValue("Issue")));    
        m_attributesSheet.addCell(new Cell(new CellLocation("J1"), new CellValue("Method Start LineNo"))); 
        
        
        m_myBook.defineName("AllowedLogLevels", "Validation", "$A$1", "$D$1");*/
       
        System.out.println("Analyzing all files in directory:" + sCrawlDir);

        ScanAppsLogger scanner = new ScanAppsLogger();

      try {         
         
           csvWriter1 = new BufferedWriter(new FileWriter("appslogger_module_issues.csv",append));
         
           if(!append)
           csvWriter1.write("Family,Module,Product,FileName,Label,ClassName,MethodName,MethodStatic,Lineno,AppsLoggerModule,Issue\n");
          
          /* csvWriter2 = new BufferedWriter(new FileWriter("appslogger_loglevel_issues.csv",append));
          
           if(!append)
           csvWriter2.write("Family,Module,Product,FileName,Label,Lineno,Logging Level,Issue\n");*/

           scanner.crawlDirectory(sCrawlDir);       
           
         //  m_attributesSheet.createDataValidation("AllowedLogLevels", "H1", "H"+row, false);
           System.out.println("Number of Java files processed: "+ java_files); 

         //  FileOutputStream out = new FileOutputStream("AppsLogger_scan.xlsx");
         //  XLSXCreator.create(m_myBook, out);
          
           csvWriter1.close();
          // csvWriter2.close();
          
           errorWriter = new BufferedWriter(new FileWriter("CouldNotFix.txt"));
          
           diffHelper.startDiffLog();
          
          if(!fixMode.equals("none")) {
             System.out.println("Making fixes...");
             scanner.makeFixes();
          }
          
           diffHelper.closeDiffLog(); 
           errorWriter.close();
          
          summary += "\nNumber of module fixes made: "+ num_module_fixes;          
          summary += "\nNumber of modules that could not be fixed: "+ num_module_errors;
          System.out.println(summary); 
          
//          Date runDate = new Date();       
//          Mail.sendMail("sudipti.gupta@oracle.com", summary, "SCRIPT: to fix AppsLogger violations run at: " + runDate.toString());        
        
      } catch (Exception e) {
          e.printStackTrace();               
      }
    }  
    
    private void crawlDirectory(String path) 
      {
          if(path.contains(".ade_path") || path.contains("/classes/"))
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

    private boolean fileOfInterest(String sName) 
    {
        String absPathLowerCase = sName.toLowerCase();
        
        if (absPathLowerCase.contains("modeltest") ||
            absPathLowerCase.contains("uitest") || absPathLowerCase.contains("testui") ||
            absPathLowerCase.contains("/test/") || absPathLowerCase.contains("noship") ||
            absPathLowerCase.contains("servicetest") || absPathLowerCase.contains("structuretest"))
            return false;
        if (sName.endsWith(".java")) {
          java_files++;
          return true;
        }
      return false;
    }
    
    private void processFile(File fName){
        
        try{

            memberVariables = new HashMap<String,String>();
            JavaFileParserHelper javaHelper = new JavaFileParserHelper(fName.getAbsolutePath());
            javaHelper.removeAllComments();
            String fileContents = javaHelper.getFileWithoutCommentsReadOnly();
            String[] lines = javaHelper.getFileContentsArrayWithoutComments(fileContents);
            
            int open_braces = 0;
            int i = 0;
            int lineNo = 0;
            String className = "";
            String methodName = "";
            String methodStatic = "";
            String packageName = "";
            String appsLoggerModule = "";
           // String logLevel = "";
            String line = "";            
            int methodStartLineNo = -1;
           // boolean isLogViolation = true;

            while(i< lines.length) {
                
                line = lines[i];
                if(line.trim().equals("")){ //ignore blank lines
                    i++;
                    continue;
                }
                lineNo = i+1; //record line number of the begining of the line
                while(!(line.trim().endsWith(";") || line.trim().endsWith("{") || line.trim().endsWith("}") || (i == lines.length-1)))
                    line += lines[++i];
                
                line = line.trim();   
            //    isLogViolation = true;
                if(packageName.equals("")){
                    Matcher m = pPackageName.matcher(line);
                    if(m.find())
                        packageName = m.group(1).trim();
                }
                
               int num_braces = getNumBraces(line);
                open_braces += num_braces;
                
                if(open_braces == 1) { //class starts
                    if(className.equals("")){ //look for className
                        Matcher m = pClassName.matcher(line);
                        if(m.find())
                            className = m.group(1);
                    }
                    else if(!line.trim().equals("}")){ //outside of any methods look for member variable declarations
                        Matcher m = pMemberVariableDeclaration.matcher(line);
                        if(m.find()){
                           String memberVar = m.group(2);
                           String memberValue = m.group(3).trim();
                           memberVariables.put(memberVar,memberValue);
                        }                            
                    }
                }
               
                if(open_braces == 2) { //method starts
                    Matcher m = pMethodName.matcher(line);
                    if(m.find()) {
                        methodName = m.group(1);
                        methodStartLineNo = lineNo;
                        localVariables = new HashMap<String,String>();
                        if(line.contains("static"))
                            methodStatic = "static";
                        else
                            methodStatic = "nonStatic";
                    }
                    
                    Matcher m1= pLocalStringVariableDeclaration.matcher(line);
                    if(m1.find()){
                       String localVar = m1.group(1);
                       String localValue = m1.group(2).trim();
                       localVariables.put(localVar,localValue);
                    }    
                }
                
                if(open_braces >= 2) { //means we are inside a method
                
                    String appsLoggerStmt = "";                
                    String issue="";
                    
                    Matcher m = pAppsLogger.matcher(line);
                    if(m.find()) { //appsLogger.write detected
                        
                        appsLoggerModule = m.group(1).trim();
                       // logLevel = m.group(3).trim();
                        appsLoggerStmt = line;
                        
                      /*  if(!allowedLogLevels.contains(logLevel)) {
                            
                            if(logLevel.contains("WARNING") || logLevel.contains("SEVERE") || logLevel.contains("INFO") || logLevel.equals("null") || logLevel.equals(""))
                                 writeToSpreadsheet(fName, lineNo, logLevel, "Review Log Level",methodStartLineNo,lines);
                            else{
                                if(methodStartLineNo != -1) {
                                    int j = methodStartLineNo-1;
                                    String methodLine = lines[j];
                                    while(!(methodLine.trim().endsWith(";") || methodLine.trim().endsWith("{") || methodLine.trim().endsWith("}") || (j == lines.length-1)))
                                        methodLine += lines[++j];
                                    Matcher methodMatcher = javaHelper.getMethodMatcherNew(methodLine);
                                    if(methodMatcher.find()) {
                                        String methodParams = methodMatcher.group(3);
                                        if(!methodParams.contains(logLevel))
                                           isLogViolation = false;
                                    }
                                }
                                if(isLogViolation) {
                                    String resolvedLogLevel = getResolvedLogLevel(logLevel);
                                    if(!resolvedLogLevel.equals("") && !allowedLogLevels.contains(resolvedLogLevel))
                                        writeToSpreadsheet(fName, lineNo, resolvedLogLevel, "Review Log Level",methodStartLineNo,lines);
                                }
                            }
                        }       */                
                        
                          if (!appsLoggerModule.contains("getClass()") &&
                              !appsLoggerModule.contains("this") &&
                              !appsLoggerModule.contains(className+".class") &&
                              !appsLoggerModule.contains(packageName + "." + className)) {     
                             
                              if(appsLoggerModule.contains("\"") || appsLoggerModule.contains(".class") 
                                 || appsLoggerModule.trim().equals("") || appsLoggerModule.trim().equals("null")
                                 || appsLoggerModule.contains("getClass()"))
                                    issue = "Incorrect Module Parameter";  
                              else{
                                  if(testResolvedModuleValue(appsLoggerModule,className, packageName)) {
                                      i++;
                                      continue;
                                  }
                                
                                  issue = "Incorrect Module Parameter";
                              }
                              
                             // appsLoggerStmt  = appsLoggerStmt.replace("\n", " ");
                             // appsLoggerStmt  = appsLoggerStmt.replace(",", "#");
                              
                              csvWriter1.write(writeFileNameInfo(fName.getCanonicalPath(),false) + className + "," 
                                              + methodName + "," + methodStatic + "," + lineNo + "," + appsLoggerModule.replace("\n", " ") + "," + issue + "\n");
                          }                     
                      }
                }                        
                i++;
            } //end of While
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
  /*  private String getResolvedLogLevel(String logLevel) {
        String resolvedLogLevel = "";

        for(Iterator<String> it = memberVariables.keySet().iterator(); it.hasNext();){
            String memberVar = it.next();
            if(memberVar.equals(logLevel)){
                resolvedLogLevel = memberVariables.get(memberVar);
                break;
            }
        }
        return resolvedLogLevel;
    }*/
    
    private boolean testResolvedModuleValue(String appsLoggerModule,String className, String packageName) {
        
        String resolvedModuleValue = "";
        if(appsLoggerModule.contains("LOG_CLASS") && appsLoggerModule.contains("LOG_METHOD"))
            return true;

        for(Iterator<String> it = memberVariables.keySet().iterator(); it.hasNext();){
            String memberVar = it.next();
            if(memberVar.equals(appsLoggerModule)){
                resolvedModuleValue = memberVariables.get(memberVar);
                break;
            }
        }
        
        if(resolvedModuleValue.equals("")){
            for(Iterator<String> it = localVariables.keySet().iterator(); it.hasNext();){
                String localVar = it.next();
                if(localVar.equals(appsLoggerModule)){
                    resolvedModuleValue = localVariables.get(localVar);
                    break;
                }
            }
        }

        if (resolvedModuleValue.contains("this") ||
            resolvedModuleValue.contains("getClass()") ||
            resolvedModuleValue.contains(className+".class") ||
            resolvedModuleValue.contains(packageName + "." + className))
            return true;
             
        return false;
    }
    
  /*  private void writeToSpreadsheet(File f, int lineNo, String logLevel, String issue, int methodStartLineNo, String[] lines) throws Exception{
        
        String fileNameInfo = writeFileNameInfo(f.getCanonicalPath(), true);
        
        String c6 = getCellLocation(5, row);
        String c7 = getCellLocation(6, row);
        String c8 = getCellLocation(7, row);
        String c9 = getCellLocation(8, row);
        String c10 = getCellLocation(9, row);         
        
        row++;
        
        m_attributesSheet.addCell(new Cell(new CellLocation(c6), new CellValue(lineNo)));
        m_attributesSheet.addCell(new Cell(new CellLocation(c7), new CellValue(logLevel)));
        m_attributesSheet.addCell(new Cell(new CellLocation(c8), new CellValue(" ")));
        m_attributesSheet.addCell(new Cell(new CellLocation(c9), new CellValue(issue)));
        m_attributesSheet.addCell(new Cell(new CellLocation(c10), new CellValue(methodStartLineNo)));
        
        int startIndex = lineNo - 5;
        if(startIndex < 0)
            startIndex = 0;
        int endIndex = lineNo + 5;
        if(endIndex >= lines.length)
            endIndex = lines.length -1;
        
        String comment = "";
        for(int j = startIndex; j <= endIndex; j++)
            comment += j + ":" +  lines[j];
        
        m_attributesSheet.createComment(comment, c7, 10, 40);
        
        csvWriter2.write(fileNameInfo + lineNo + "," + logLevel + "," + issue + "\n");
        
    } */
    
    private int getNumBraces(String line){
        int num_braces = 0;
        while(line.contains("{")) {
            num_braces++;
            line = line.replaceFirst("\\{","");
        }
        while(line.contains("}")){
            num_braces--;
            line = line.replaceFirst("\\}","");
        }
        
        return num_braces;
    }
    
    private String writeFileNameInfo(String absPath, boolean write) {
        
      int i = absPath.indexOf("fusionapps/");
      String pathAfterFusionApps = absPath.substring(i);
      String[] parts = pathAfterFusionApps.split("/");
      String family = parts[1].toUpperCase();
      String module = parts[3];
      String product = parts[4].toUpperCase();
      
      String path_before_fusionapps = absPath.substring(0, i-1);
      parts = path_before_fusionapps.split("/");
      String label = parts[parts.length -2] + "_" + parts[parts.length -1];
     
      label = label.replace(".rdd","");
      
    /*  if(write) {      
          String c1 = getCellLocation(0, row);
          String c2 = getCellLocation(1, row);
          String c3 = getCellLocation(2, row);
          String c4 = getCellLocation(3, row);
          String c5 = getCellLocation(4, row);
          
          m_attributesSheet.addCell(new Cell(new CellLocation(c1), new CellValue(family)));
          m_attributesSheet.addCell(new Cell(new CellLocation(c2), new CellValue(module)));
          m_attributesSheet.addCell(new Cell(new CellLocation(c3), new CellValue(product)));
          m_attributesSheet.addCell(new Cell(new CellLocation(c4), new CellValue(pathAfterFusionApps)));
          m_attributesSheet.addCell(new Cell(new CellLocation(c5), new CellValue(label)));
      }*/
      
      return family+","+module+","+product+","+pathAfterFusionApps+","+label+",";
    }
    
    
    
    /*private String getCellLocation(int col, int row) 
    {
        return getColumn(col) + row;
    }
    
    private String getColumn(int col) 
    {
        int r = col % 26;
        int x = col / 26;
        
        String retVal = "";
        
        if(col >= 26)
        {
            retVal = getColumn(x-1);
        }
        
        return retVal +getSingleColumnDigit(r);
    }
    
    private String getSingleColumnDigit(int col) 
    {
        assert(col >= 0 && col < 26);
        
        int valueOfA  = 65;
        char c = (char)(valueOfA+col);
        return String.valueOf(c);
    } */
    
    public void makeFixes() throws Exception{
        
        //Make module fixes
        BufferedReader csvReader1 = new BufferedReader(new FileReader("appslogger_module_issues.csv"));
        String line = csvReader1.readLine();
        ArrayList<String> lines = new ArrayList<String>();
        String fileAbsPath = null;
        JavaFileParserHelper javaHelper = null;
        String[] fileContents = null;
        String newFileContents = null;
        String prevFileName = null;
        String currentFileName = null;

            while((line = csvReader1.readLine()) != null) {
                String[] parts = line.split(",");
                if(parts.length < 11) 
                    continue;            
                String issue = parts[10].trim();
                currentFileName = parts[3].trim();
                if(prevFileName == null)
                    prevFileName = currentFileName;
    
                if(issue.equals("Incorrect Module Parameter")) { 
                    if(currentFileName.equals(prevFileName))
                        lines.add(line);
                    else {
                        fileAbsPath = viewRoot + prevFileName;
                        javaHelper = new JavaFileParserHelper(fileAbsPath);
                        fileContents = javaHelper.getFileContents();
                        fileContents = fixModule(lines, fileContents);
                        newFileContents = javaHelper.getFileContentsAsString(fileContents, false);
                        applyFix(fileAbsPath, newFileContents);
                        
                        lines = new ArrayList<String>();
                        lines.add(line);
                    }
                    prevFileName = currentFileName;
                }
            }
            //process the last group
            
            if(prevFileName != null) {
                fileAbsPath = viewRoot + prevFileName;
                javaHelper = new JavaFileParserHelper(fileAbsPath);
                fileContents = javaHelper.getFileContents();
                fileContents = fixModule(lines, fileContents);
                newFileContents = javaHelper.getFileContentsAsString(fileContents, false);
                applyFix(fileAbsPath, newFileContents);
                prevFileName = null;
                currentFileName = null;
            }
            
            csvReader1.close();
            lines = new ArrayList<String>(); 
    }
    
    private String[] fixModule(ArrayList<String> lines, String[] fileContents) throws Exception {
        //csvWriter1.write("Family,Module,Product,FileName,Label,ClassName,MethodName,MethodStatic,Lineno,AppsLoggerModule,Issue\n");
        for(Iterator<String> it = lines.iterator(); it.hasNext();) {
            String line = it.next();
            String[] parts = line.split(",");
            
           int lineNo = Integer.parseInt(parts[8].trim())-1;
           int endStmtLineNo = lineNo; //initialize end of statement line number to begining of stmt line number
           String appsLoggerStmt = fileContents[lineNo];
            
            while(!appsLoggerStmt.trim().endsWith(";"))
               appsLoggerStmt += fileContents[++endStmtLineNo];
            
            
            if(!appsLoggerStmt.contains("AppsLogger.write")) {
                errorWriter.write("Line No is messed up..Cannot fix module: " + parts[3].trim() + ": " + (lineNo+1) + "\n");
                num_module_errors++;
                continue;
            }
            
            String className = parts[5].trim();   
            String methodType= parts[7].trim();
            String newModule = "this";
            
            if(methodType.equals("static")) {
                if(!className.equals(""))
                    newModule = className + ".class";
                else{
                    errorWriter.write("Cannot determine className. Cannot fix:" + line + "\n");
                    num_module_errors++;
                    continue;
                }
            }
                    
            Matcher m = pAppsLogger.matcher(appsLoggerStmt);
            if(m.find()) {
                String oldModule = m.group(1);
                int startMatch = m.start(1);
                //appsLoggerStmt = appsLoggerStmt.replace(oldModule, newModule);
                appsLoggerStmt = appsLoggerStmt.substring(0, startMatch) + newModule + appsLoggerStmt.substring(startMatch + oldModule.length());
                fileContents[lineNo] = appsLoggerStmt;
                for(int i = lineNo+1; i <= endStmtLineNo; i++)
                    fileContents[i] = "";
                
                num_module_fixes++;
            }
            else{
                errorWriter.write("AppsLogger write pattern not found. Cannot fix:" + line + "\n");
                num_module_errors++;
            }      
            
        } //for
        return fileContents;
    }
    
  
    
    private static String getViewRoot(String curDir) {
        
        String path_before_fusionapps = "";
        if (curDir.contains("/fusionapps")) {
                int i = curDir.indexOf("/fusionapps") + 1;
                path_before_fusionapps = curDir.substring(0,i);                
        }
        if (curDir.contains("/fsm")) {
                int i = curDir.indexOf("/fsm") + 1;
                path_before_fusionapps = curDir.substring(0,i);                
        }
        if (curDir.contains("/atgpf")) {
                int i = curDir.indexOf("/atgpf") + 1;
                path_before_fusionapps = curDir.substring(0,i);                
        }
        return path_before_fusionapps;
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
