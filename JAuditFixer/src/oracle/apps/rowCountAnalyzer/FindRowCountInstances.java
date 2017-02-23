package oracle.apps.rowCountAnalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JavaFileParserHelper;

public class FindRowCountInstances {
    
    /**
     * Statistics
     */
    static int m_javaCount = 0; //number of files processed
    static int num_occurances = 0; //number of instances of rowCount
    static int num_unused = 0; //number of instances where rowCount variable is never used
    static int num_existence_checks = 0; //num of instances where rowCount is used as an existence check (or in any conditional expr)
    static int num_logging = 0; //number of instances where rowCount variable is only used for logging    
    static int num_iterations = 0; //number of instances when rowCount is used in iterations (for or while loops)
    static int num_range = 0; //number of instances when rowCount is used for setting ranges
    static int num_object_creation = 0; //number of instances when rowCount is used for object creation (new)
    static int num_return_stmts = 0; //number of instances when rowCount is used to return a value
    
    static boolean bDebug = false;
    static boolean removeExemptions = true;
    static Writer outputFileWriter;
    BufferedReader fileReader;
    static String moduleName;
    
    static final int STATUS_UNUSED = 0;
    static final int STATUS_USAGE_UNKNOWN = 1;
    static final int STATUS_USED_IN_ITERATION = 2;
    static final int STATUS_USED_IN_OBJECT_CREATION = 3;
    static final int STATUS_USED_IN_EXISTANCE_CHECK = 4;
    static final int STATUS_USED_IN_RETURN = 5;
    static final int STATUS_USED_IN_LOGGING = 6;
    static final int STATUS_USED_IN_RANGE= 7;    
    static final int STATUS_USED_IN_EXISTANCE_CHECK_COMPLEX = 8;
    
    //(variable name)(optional white space)=(optional white space)(object name).(row count method)
    Pattern pRowCount = Pattern.compile("(\\w+)\\s*=\\s*.*(getEstimatedRowCount|estimatedRowCount|getCappedRowCount)");
    Pattern pIteration = Pattern.compile("(for|while)\\s*\\(");
    Pattern pObjectCreation = Pattern.compile("new\\s+");
    Pattern pExistenceCheck = Pattern.compile("(==|!=|<=|>=|<|>)");
   
    Pattern pReturnStmt = Pattern.compile("(return\\s+)|(return\\()");
    Pattern pLogging = Pattern.compile("(print\\(|println\\(|write\\(|logMessage\\()");
    Pattern pRange = Pattern.compile("setRangeSize");    
    
    HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();
    HashMap<String,ArrayList> exemptionLines = new HashMap<String,ArrayList>();
    
    
    public static void main(String[] args) {

      Date startDate = new Date();      

      String sCrawlDir = args[0];

     if (isEmpty(sCrawlDir)) {
          System.out.println("Crawl directory has to be specified");
          System.exit(1);
      }
      
      File f = new File(sCrawlDir.trim());
      if(!f.exists()){
          System.out.println("Crawl directory does not exist - " + sCrawlDir);
          System.exit(1);
      }
      
     /* if(sCrawlDir.contains("/LATEST")) {
          int ind = sCrawlDir.indexOf("/LATEST");
          String pathBeforeLatest = sCrawlDir.substring(0,ind);
          File series = new File(pathBeforeLatest);
          File[] labels = series.listFiles();
          
          long lastModified = 0;
          String latestLabel = "";
          for(int i = 0; i < labels.length; i++) {
              if(labels[i].lastModified() > lastModified) {
                  lastModified = labels[i].lastModified();
                  latestLabel = labels[i].getName();
              }
          }
          if(!latestLabel.equals(""))
              sCrawlDir = sCrawlDir.replace("LATEST", latestLabel);
      }*/
      
      System.out.println("Analyzing all files in directory:" + sCrawlDir);
      
      boolean append = false;
      if(args.length > 1)
        append = new Boolean(args[1].trim());
      
      try {
        outputFileWriter = new BufferedWriter(new FileWriter("SelectCountScan.csv",append) );
        if(!append)
            outputFileWriter.write("Family,Module,Product,Filename,Label, Line, Line#, Type of RowCount, Usage Type\n");
      } catch(Exception ex) {
        System.out.println("Cannot write to file " + moduleName + ".csv");
        System.exit(1);
      }

      FindRowCountInstances rowCountAnalyzer = new FindRowCountInstances();
                                                                                                                                                                                                            

     try {
          rowCountAnalyzer.crawlDirectory(sCrawlDir);
          outputFileWriter.close();
      } catch (Exception ex) {
          System.out.println("Exception occurred: " + ex.getMessage());
      }
      
      if(removeExemptions) {
        System.out.println("Removing exemption cases...");
        try{
          rowCountAnalyzer.removeExceptionCases();
        }catch(Exception e) {
          System.out.println("Exception when removing exemption cases: " + e.getMessage());
          e.printStackTrace();
        }
      }

      rowCountAnalyzer.printStatistics(startDate);
  }
  
  /**
   * recurse over the crawl directory and for each valid file - process it.
   * Valid directories -
   * - should not contain, /dbSchema/ or /model/ or /publicModel/ or /test/ or /classes/
   * Valid UI files
   * - jspx or jsff extensions
   * - and not in folder with /classes/ or /test/ or /model/ or /publicModel/
   */
  private void crawlDirectory(String crawlDir) {
      if (crawlDir.contains(".ade_path"))
          return;

      File folder = new File(crawlDir);
      File[] listOfFiles = folder.listFiles();

      if (listOfFiles == null)
          return;

      for (int i = 0; i < listOfFiles.length; i++) {
          if (listOfFiles[i].isFile()) {
              String absFilePath = listOfFiles[i].getAbsolutePath();
              if (isFileOfInterest(absFilePath)) {
                  m_javaCount++;

                  processFile(listOfFiles[i]);
              }

          } else if (listOfFiles[i].isDirectory()) {
              String dir = listOfFiles[i].getAbsolutePath();  
              if (isValidDirectory(dir))
                crawlDirectory(dir);             

          } else {
              System.out.println("ERROR:  node is neither file or directory");
          }
      }            
  }
  
  public void processFile(File file){
      
    String absPath = file.getAbsolutePath();
   
    try {        
        
      JavaFileParserHelper javaFileHelper = new JavaFileParserHelper(absPath);
      String fileContents = javaFileHelper.getFileContentsAsString(false);
      
        if(absPath.trim().endsWith("java")) {
            fileContents = removeSingleLineComments(fileContents);
            fileContents = removeMultilineComments(fileContents);
        }else if(absPath.trim().endsWith("jsff")) {
            fileContents = removeCommentsFromJsff(fileContents);
        }
      if (!isEmpty(fileContents)) {

          findViolationsInFile(file, fileContents);
      } else {
          if (bDebug) {
              System.out.println("File contents were empty.. returning.. " +
                                 absPath);
          }
      }
    } catch (Exception ex) {
                System.out.println("Exception in file: Skipping..  " + absPath +
                                   " Exception: " + ex.getMessage());
                return;
      }
  }
  
  public void findViolationsInFile(File file, 
                                   String fileContentsWithoutComments) throws Exception {
      
      int numOfOpenBraces = 0;
      //Map of variableName (which holds rowCount) -> usageType(0-7)
      HashMap rowCountVariables = new HashMap();
      //List of strings to be output
      ArrayList outputLines = new ArrayList();
      
      String fileName = file.getName();
      String filePath = file.getAbsolutePath();
      
      File f = new File(filePath);
      String canonicalPath = f.getCanonicalPath();
     
      fileReader =
          new BufferedReader(new StringReader(fileContentsWithoutComments));
      
      Integer lineNum = 0;
      if (fileReader == null)
          return;
      
      String line = null;
      
      try {
        do {
            
            Object[] lineAndNumLines = (Object[])readLine(fileName);
            if(lineAndNumLines == null) break;
            line = (String)lineAndNumLines[0];
           //while((line = fileReader.readLine()) != null) {
           lineNum += (Integer)lineAndNumLines[1];
          
          //If numberOfOpenBraces > 1 => we are in a method.
          numOfOpenBraces += countNumberOfOpeningBracesInStr(line) - countNumberOfClosingBracesInStr(line);
          Matcher m = pRowCount.matcher(line);
          
          String variableName = null;
            
          if(m.find())
              variableName = m.group(1);
          
          Object[] values = getOccuranceType(line);
          String usageType = (String)values[1];
          
          int index = (Integer)values[0];              
            
          // test if a variable name from the hashMap of rowcount variables is used in this line, and how it is used
          //(Usage test is perfomed before we put the variable name in the map, 
          //so that the first time the variable name is encountered, it won't be considered as used
          testVariablesUsage(line, rowCountVariables, outputLines);
          
          //if variableName is not null and we are in a method, put the variable name in the hashmap
          if(variableName != null && numOfOpenBraces > 1)
              insertRowCountVariable(rowCountVariables, variableName);                   
          
          // when out of the method - write output to file, clear the hashmap & clear out outputLines list.
          if(numOfOpenBraces <= 1) {              
              writeOutputTofile(outputLines, rowCountVariables);
              rowCountVariables.clear();
              outputLines.clear();
          }
          
          if(index != -1){        
              outputLines.add(new Object[] {variableName, FamilyModuleHelper.getFileNameInfo(canonicalPath) + line.trim().replace(",", "<comma>") + "," + lineNum + "," 
                                    + usageType + testExpressionUsage(line,file.getName())});
              num_occurances++;
          } else
              continue;          
      } while (line != null);
          
  } catch (Exception ex) {
            System.out.println("Excepion in reading file");
            ex.printStackTrace();
            throw new Exception("Exception while trying to find violatiosn in file",
                                ex);
    }
}


  public boolean isFileOfInterest(String absFilePath) {
      
        if(!FamilyModuleHelper.isPathOfInterest(absFilePath))
              return false;  
        
        if ((absFilePath.endsWith(".java") 
              || absFilePath.endsWith(".jspx")
              || absFilePath.endsWith(".jsff")) 
              && !absFilePath.toLowerCase().endsWith("test.java")) 
              return true;
          
       return false;
  }

  
  public static boolean isEmpty(String str) {
      if (str == null || str.trim().equals("")) {
          return true;
      }
      return false;
  }
  
  public void printStatistics(Date startDate) {
      // Print Statistics
      Date endDate = new Date();
      
      try {
      BufferedWriter outStats = new BufferedWriter(new FileWriter(moduleName + ".txt") );
      
      outStats.write("=================================================\n\n");
      outStats.write("Run Statistics\n");
      outStats.write("Start Time: " + startDate.toString() + "\n");
      outStats.write("End Time: " + endDate.toString() + "\n");
      long elapsedTime = endDate.getTime() - startDate.getTime();
      outStats.write("Elapsed Time (sec): " + elapsedTime / 1000 + "\n");
      outStats.write("=================================================\n\n");

      outStats.write("=================================================\n\n");
      outStats.write("Total Number of java files analyzed: " +
                      m_javaCount + "\n");
      outStats.write("Total Number of row count instances found: " +
                      num_occurances + "\n");
      
      outStats.write("===================Violations====================\n\n");
      outStats.write("Total Number of row count instances unused: " +                         
                     num_unused + "\n");
      outStats.write("Total Number of row count instances used in logging only: " +
                     num_logging + "\n");   
      outStats.write("Total Number of row count instances used in existence checks (in any conditional expr): " +
                     num_existence_checks + "\n");
      
      int violations = num_unused + num_logging + num_existence_checks;
      outStats.write("\nTotal number of probable violations: " + violations + "\n\n");
      
      outStats.write("================Non - Violations=================\n\n");
      outStats.write("Total Number of instances when rowCount is used in iterations (for or while loops): " +
                     num_iterations + "\n");
      outStats.write("Total Number of instances when rowCount is used for setting ranges: " +
                     num_range + "\n");
      outStats.write("Total Number of instances when rowCount is used for object creation (new): " +
                     num_object_creation + "\n");
      outStats.write("Total Number of instances when rowCount is used to return a value: " +
                    num_return_stmts + "\n");      
      
      int non_violations = num_iterations + num_range + num_object_creation + num_return_stmts;
      
      outStats.write("\nTotal number of probable non-violations: " + non_violations +  "\n\n");
      
      outStats.write("\nNumber of unknown usages: " + (num_occurances - non_violations - violations) + "\n\n");
  
      outStats.write("=================================================\n");
      
      outStats.flush();
      outStats.close();
          
      System.out.println("Done in " + elapsedTime / 1000 + " secs");
      } catch(IOException e) {
        System.out.println("Some exception is writing output statistics");
      }     
  }
  
  private String removeSingleLineComments(String fileContents) {
      String singleLineCommentsRE = "//.*$";
      Pattern pattern =
          Pattern.compile(singleLineCommentsRE, Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(fileContents);

      return matcher.replaceAll("");
  }

  private String removeMultilineComments(String fileContents) {
      StringBuffer newFileContents = new StringBuffer();


      /*anything*/
      // reluctant quantifier because we want to match the first occurrence of the closing tag as opposed to the last
      String multiLineCommentsRE = "/\\*.*?\\*/";
      Pattern pattern = Pattern.compile(multiLineCommentsRE, Pattern.DOTALL);
      Matcher matcher = pattern.matcher(fileContents);
      int startPos = 0;
      while (matcher.find()) {
          int start = matcher.start();
          int end = matcher.end();
          String str = fileContents.substring(start, end);
          int numb = countNumberOfNewLines(str);
          int indexTrailingnewLine = fileContents.indexOf("\n", end);
          if (indexTrailingnewLine != -1) {
              String endAfterComment =
                  fileContents.substring(end, indexTrailingnewLine);
              if (isEmpty(endAfterComment)) {
                  //    numb++;
              }
          }
          String replacement = "";
          for (int i = 0; i < numb; i++) {
              replacement = replacement + " \n ";
          }
          newFileContents.append(fileContents.substring(startPos, start));
          newFileContents.append(replacement);
          startPos = end;
          ///System.out.println(str);
          //newFileContents=newFileContents.replace(start, end, replacement);
      }
      newFileContents.append(fileContents.substring(startPos));
      return newFileContents.toString();
  }
  
  private String removeCommentsFromJsff(String fileContents) {
      StringBuffer newFileContents = new StringBuffer();


      /*anything*/
      // reluctant quantifier because we want to match the first occurrence of the closing tag as opposed to the last
      Pattern pattern = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
      Matcher matcher = pattern.matcher(fileContents);
      int startPos = 0;
      while (matcher.find()) {
          int start = matcher.start();
          int end = matcher.end();
          String str = fileContents.substring(start, end);
          int numb = countNumberOfNewLines(str);
          int indexTrailingnewLine = fileContents.indexOf("\n", end);
          if (indexTrailingnewLine != -1) {
              String endAfterComment =
                  fileContents.substring(end, indexTrailingnewLine);
              if (isEmpty(endAfterComment)) {
                  //    numb++;
              }
          }
          String replacement = "";
          for (int i = 0; i < numb; i++) {
              replacement = replacement + " \n ";
          }
          newFileContents.append(fileContents.substring(startPos, start));
          newFileContents.append(replacement);
          startPos = end;
          ///System.out.println(str);
          //newFileContents=newFileContents.replace(start, end, replacement);
      }
      newFileContents.append(fileContents.substring(startPos));
      return newFileContents.toString();
  }
  
  public int countNumberOfNewLines(String str) {

      if (isEmpty(str)) {
          return 0;
      }
      if (!str.contains("\n"))
          return 0;

      Pattern p = Pattern.compile("\\n");
      Matcher m = p.matcher(str);
      int cnt = 0;
      while (m.find()) {
          cnt++;
      }
      return cnt;
  }
  
  private boolean isValidDirectory(String absDirPath) {
      if (absDirPath.contains("/dbSchema") ||
          absDirPath.contains("/classes") ||
          absDirPath.contains("/test") ||
          absDirPath.contains("/modelTest")) {
          return false;
      }
      
      if(absDirPath.contains("/prj/components/projectsManagement") ||
          absDirPath.contains("/prj/components/projectsFinancialsSoa/projectManagement") ||
          absDirPath.contains("/prj/components/projectsFinancialsSoa/projectManagementControl") ||
          absDirPath.contains("/prj/components/projectsFinancialsSoa/resourceManagement") ||
          absDirPath.contains("/prj/components/projectsFinancialsEss/projectManagement") ||
          absDirPath.contains("/prj/components/projectsFinancialsEss/resourceManagement"))
          return false;
      
      return true;
  }
  
  private int countNumberOfOpeningBracesInStr(String str) {

      if (isEmpty(str)) {
          return 0;
      }
      if (!str.contains("{"))
          return 0;

      Pattern p = Pattern.compile("\\{");
      Matcher m = p.matcher(str);
      int cnt = 0;
      while (m.find()) {
          cnt++;
      }
      return cnt;
  }
  
  private int countNumberOfClosingBracesInStr(String str) {

      if (isEmpty(str)) {
          return 0;
      }
      if (!str.contains("}"))
          return 0;

      Pattern p = Pattern.compile("\\}");
      Matcher m = p.matcher(str);
      int cnt = 0;
      while (m.find()) {
          cnt++;
      }
      return cnt;
  }
    
    /**
     * If rowCount occurance is saved in a variable, this method determines how the variable is used
     * @param line
     * @param rowCountVariables
     * @param outputLines
     * @throws IOException
     */
  private void testVariablesUsage(String line, HashMap rowCountVariables, ArrayList outputLines) throws IOException{
    
      Matcher mIteration = pIteration.matcher(line);
      Matcher mObjectCreation = pObjectCreation.matcher(line);
      Matcher mExistenceCheck = pExistenceCheck.matcher(line);
      Matcher mReturnStmt = pReturnStmt.matcher(line);
      Matcher mLogging = pLogging.matcher(line);
      Matcher mRange = pRange.matcher(line);
      
      for(Iterator i = rowCountVariables.keySet().iterator(); i.hasNext(); ){
          String variableName = (String)i.next();
          if(lineContainsVariable(line, variableName)) { 
              
             //As soon as variables status is determined write that outputline to file 
             //and remove that variable from rowCountVariables map and line from outPutLine array
              // Variable is removed only in case of a non-violation (e.g. if a variable
              // is used for existence check first, and then in an iteration, then its not really a violation)
              if(mIteration.find()) {
                rowCountVariables.put(variableName, STATUS_USED_IN_ITERATION);
                writeToFile(variableName, outputLines, rowCountVariables);
                i.remove();
              }
              else if(mObjectCreation.find() && !line.contains("Exception")) { //new xyzException(..) should not be confused with object creation
                rowCountVariables.put(variableName, STATUS_USED_IN_OBJECT_CREATION);
                //writeToFile(variableName, outputLines, rowCountVariables);
                //i.remove();
              }              
              else if(mReturnStmt.find()) {
                rowCountVariables.put(variableName, STATUS_USED_IN_RETURN);
                writeToFile(variableName, outputLines, rowCountVariables);
                i.remove();
              }              
              else if(mRange.find()) {
                rowCountVariables.put(variableName, STATUS_USED_IN_RANGE);
                writeToFile(variableName, outputLines, rowCountVariables);
                i.remove();
              }
             else if(mLogging.find()) {
              rowCountVariables.put(variableName, STATUS_USED_IN_LOGGING);
              //writeToFile(variableName, outputLines, rowCountVariables);
              //i.remove();
             }
             else if(mExistenceCheck.find()) {
              Pattern pExistenceCheck_strong = Pattern.compile(variableName + "\\s*(==|!=|<=|>=|<|>)\\s*(\\d*)\\W+");
              Matcher m = pExistenceCheck_strong.matcher(line);
              if(m.find()){
                  String checkNumber = m.group(2);
                  if(checkNumber.equals("0") || checkNumber.equals("1"))
                      rowCountVariables.put(variableName, STATUS_USED_IN_EXISTANCE_CHECK); 
                  else
                      rowCountVariables.put(variableName, STATUS_USED_IN_EXISTANCE_CHECK_COMPLEX); 
              } else                  
                    rowCountVariables.put(variableName, STATUS_USED_IN_EXISTANCE_CHECK_COMPLEX); //test for existence check after iteration & object creation becasue if any of the prev two checks succeed, then its a valid use of rowCount
              //writeToFile(variableName, outputLines, rowCountVariables);
              //i.remove();
             }
             else {
                //if((Integer)rowCountVariables.get(variableName) < 2) //only is status is not determined till now
                    rowCountVariables.put(variableName, STATUS_USAGE_UNKNOWN);
             }
          }
          
      }    
  }
  
  /**
     * If rowCount occurance is directly used, this method determines how the ocurance is used
     * @param line
     * @return
     */
  private String testExpressionUsage(String line, String filename){
      
      if(filename.endsWith(".jsff") || filename.endsWith(".jspx")) {
        if(line.trim().startsWith("<"))
            line = line.trim().substring(1);
        if(line.trim().endsWith(">"))
              line = line.trim().substring(0,line.trim().length()-1);
      }
      
    Matcher mIteration = pIteration.matcher(line);
    Matcher mObjectCreation = pObjectCreation.matcher(line);
    Matcher mExistenceCheck = pExistenceCheck.matcher(line);
    Matcher mReturnStmt = pReturnStmt.matcher(line);
    Matcher mLogging = pLogging.matcher(line);
    Matcher mRange = pRange.matcher(line);
    
    if(mIteration.find()) {
      num_iterations++;
      return ",iteration"; 
    }
    else if(mObjectCreation.find() && !line.contains("Exception")) {
      num_object_creation++;
      return ",object creation";  
    }
    else if(mExistenceCheck.find()) {
        num_existence_checks++;
        Pattern pExistenceCheck_strong = Pattern.compile("(?:getEstimatedRowCount|estimatedRowCount|getCappedRowCount)(?:\\(\\))?" + "\\s*(==|!=|<=|>=|<|>)\\s*(\\d*)\\W+");
        Matcher m = pExistenceCheck_strong.matcher(line);
        if(m.find()){
            String checkNumber = m.group(2);
            if(checkNumber.equals("0") || checkNumber.equals("1"))
                return ",existence check";   
            else
                return ",existence check complex";  
        } else                  
              return ",existence check complex";      
    }
    else if(mReturnStmt.find()) {
      num_return_stmts++;
      return ",return stmt";  
    }
    else if(mLogging.find()) {
      num_logging++;
      return ",logging";  
    }
    else if(mRange.find()) {   
      num_range++;
      return ",range";  
    }
    else {
      return "";  
    }
    
  }
  
  /**
     * Writes all outputLines in bulk.
     * @param outputLines
     * @param rowCountVariables
     * @throws IOException
     */
  private void writeOutputTofile(ArrayList outputLines, HashMap rowCountVariables) throws IOException {
      
      for(Iterator i = outputLines.iterator(); i.hasNext(); ){
          Object pair = i.next();
          String variableName = (String)((Object[])pair)[0];
          String outputLine = (String)((Object[])pair)[1];
          if(variableName != null) {
              outputLine = appendUsageType(variableName, rowCountVariables, outputLine);                  
          }
          else {
              outputLine += "\n";
          }
          outputFileWriter.write(outputLine);
      }    
  }
  
  /**
     * Writes one output line to file, then removes it from list of OutputLines
     * @param variableName
     * @param outputLines
     * @param rowCountVariables
     * @throws IOException
     */
  private void writeToFile(String variableName, ArrayList outputLines, HashMap rowCountVariables) throws IOException {
      
      for(Iterator i = outputLines.iterator(); i.hasNext(); ) {
          Object[] values = (Object[])i.next();
          String varName = (String)values[0];
          String line = (String)values[1];
          if(varName != null && varName.equals(variableName)){
              outputFileWriter.write(appendUsageType(variableName, rowCountVariables, line));
              i.remove();
              return;
          }
      }
    
  }
  
  /**
     * Append usage type to outputLine
     * @param variableName
     * @param rowCountVariables
     * @param outputLine
     * @return
     */
  private String appendUsageType(String variableName, HashMap rowCountVariables, String outputLine) {
      
    if(rowCountVariables.get(variableName) == null) return outputLine+ " \n";
    if(rowCountVariables.get(variableName).equals(STATUS_UNUSED)) {
      outputLine += ",var unused\n";
      num_unused++;
    } else if(rowCountVariables.get(variableName).equals(STATUS_USED_IN_ITERATION)){
      outputLine += ",iteration\n";
      num_iterations++;
    } else if(rowCountVariables.get(variableName).equals(STATUS_USED_IN_OBJECT_CREATION)){
      outputLine += ",object creation\n";
      num_object_creation++;
    } else if(rowCountVariables.get(variableName).equals(STATUS_USED_IN_EXISTANCE_CHECK)){
      outputLine += ",existence check\n";
      num_existence_checks++;
    } else if(rowCountVariables.get(variableName).equals(STATUS_USED_IN_EXISTANCE_CHECK_COMPLEX)){
      outputLine += ",existence check complex\n";
      num_existence_checks++;
    } else if(rowCountVariables.get(variableName).equals(STATUS_USED_IN_RETURN)){
      outputLine += ",return stmt\n";
      num_return_stmts++;
    } else if(rowCountVariables.get(variableName).equals(STATUS_USED_IN_LOGGING)){
      outputLine += ",logging\n";
      num_logging++;
    } else if(rowCountVariables.get(variableName).equals(STATUS_USED_IN_RANGE)){
      outputLine += ",range\n";
      num_range++;
    } else {
      outputLine += ",???\n";
    }
    return outputLine;
  }
  
  private void insertRowCountVariable(HashMap rowCountVariables, String variableName) {
      if(!rowCountVariables.containsKey(variableName))
        rowCountVariables.put(variableName, 0);     
  }
  
  private Object[]  getOccuranceType(String line) {
      
    String usageType = "";
    
      int index = line.indexOf("getEstimatedRowCount()");
      
        if(index != -1) {
            usageType = "estimated";
        }
        else {
          index = line.indexOf("estimatedRowCount"); //for jsff & jspX files
          
            if(index != -1) {
                usageType = "estimated";
            }
            else {
              index = line.indexOf("getCappedRowCount");
              if(index != -1)
              usageType = "capped";
            }
    }
    
    return new Object[]{index, usageType};
    
  }
  
  /**
     * This method reads one line of a Java file i.e. a line which is terminated by semi-colon or opening or closing curly braces
     * @param fileName
     * @return
     * @throws IOException
     */
  private Object[] readLine(String fileName) throws IOException{
      
      int numLines = 0;
      StringBuffer javaLine = new StringBuffer();
      String line = "";
      while((line = fileReader.readLine()) != null) {          
          numLines++;          
          javaLine.append(line);
          if(!fileName.endsWith(".java")) break;
          line = line.replaceAll("\\s+$",""); //remove trailing whitespaces
          if(line.endsWith(";") || line.endsWith("{") || line.endsWith("}")) break;
      }
      
      if(line == null) 
          return null;      
      return new Object[]{javaLine.toString(), numLines};
  } 
  
  private boolean lineContainsVariable(String line, String varName){
    Pattern p = Pattern.compile("\\W+" + varName + "\\W+");
    Matcher m = p.matcher(line);
    return m.find();
  }
  
  
  public void removeExceptionCases() throws Exception{
      
    generateExemptionList();
    BufferedReader fileReader = new BufferedReader(new FileReader("SelectCountScan.csv"));
    String line = null;
    BufferedWriter writer = new BufferedWriter(new FileWriter("temp.csv") );
    while((line = fileReader.readLine()) != null) {   
      String[] parts = line.split(",");
        if(parts.length < 9)
            continue;
        
      String fileName = parts[3].trim();
      String lineNo = parts[6].trim();
        
      if(parts[5].trim().contains("assert"))
          continue;
        
      if(exemptionsContainsLine(fileName, lineNo, parts[5].trim()))
          continue;
        
     String usageType = parts[8].trim();
     String rowCountType = parts[7].trim();
    
     if(!usageType.equals("iteration") && !usageType.equals("var unused") 
        && !usageType.equals("existence check"))
        continue;
        
     if(rowCountType.equals("rowCount")){
        if(usageType.equals("iteration"))
            continue;
        if(usageType.equals("existence check") && (fileName.contains("ui/") || fileName.contains("publicUi/")))
            continue;
     }
      writer.write(parts[0] + "," + parts[1] + "," + parts[2] + "," + parts[3] + "," + parts[4] + "," +
                   parts[5] + "," + parts[6] + "," + parts[7] + "," + parts[8] +"\n");
    }    
    writer.close();
    
    fileReader = new BufferedReader(new FileReader("temp.csv"));
    writer = new BufferedWriter(new FileWriter("SelectCountScan.csv") );
    while((line = fileReader.readLine()) != null) {   
        writer.write(line+"\n");
      }
      writer.close();
      fileReader.close();
  }
  
  private void generateExemptionList() {
      
      try{
    BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
        "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions.txt"));
    
    String line = null;
    
    while((line = fileReader.readLine()) != null) {
      String[] parts = line.split(",");
      if(parts.length < 3)
          continue;
      String issue = parts[2].trim();
      if(!issue.equals("SelectCountQuery"))
          continue;
      String fileName = parts[0].trim();
      String lineNo = parts[1].trim();
      String exemptionSignature = "";
        
      if(parts.length > 4)
          exemptionSignature = parts[3].trim();
        
      HashSet<String> exemptLines = null;
      if(!exemptions.containsKey(fileName)) 
        exemptLines = new HashSet<String>();
      else 
        exemptLines = exemptions.get(fileName);
        
      exemptLines.add(lineNo);
      exemptions.put(fileName, exemptLines);
        
      if(!exemptionSignature.equals("")) {
         ArrayList<String> exemptLineSignatures = null;
         if(!exemptionLines.containsKey(fileName)) 
           exemptLineSignatures = new ArrayList<String>();
         else 
           exemptLineSignatures = exemptionLines.get(fileName);
         
          exemptLineSignatures.add(exemptionSignature);
          exemptionLines.put(fileName, exemptLineSignatures);
      }
    }
      }catch (Exception e) {
          System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
      }
  }
  
  private boolean exemptionsContainsLine(String fileName, String lineNo, String line){
    
    if(!exemptions.containsKey(fileName) && !exemptionLines.containsKey(fileName)) 
        return false;
    
    HashSet<String> lines = exemptions.get(fileName);
    if(lines != null && lines.contains(lineNo))
        return true;
    
    ArrayList<String> exemptionSignatures = exemptionLines.get(fileName);
    if(exemptionSignatures != null) {
        for(int i = 0; i < exemptionSignatures.size(); i++) {
            String exemptionSignature = exemptionSignatures.get(i);
            if(line.contains(exemptionSignature))
                return true;        
        }    
    }
    return false;
  }
}
