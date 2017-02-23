package oracle.apps.rup2scans;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JavaFileParserHelper;

public class RowSetIteratorsNotClosed {
    
    static BufferedWriter csvWriter;
    Pattern pRowSetIterator = Pattern.compile("(\\w+)\\s*=\\s*.*createRowSetIterator\\((.*?)\\)");
    Pattern pRSIClose = Pattern.compile("(\\w+)\\.closeRowSetIterator");
    Pattern pMethodName = Pattern.compile("(?:(?:public|private|protected|static)\\s+)*\\s*\\w+(?:\\[\\])?(?:<\\w+>)?\\s+(\\w+)\\s*\\(.*\\).*?\\{",Pattern.DOTALL);
    HashMap<String,Boolean> localVariables = new HashMap<String,Boolean>();
    HashMap<String,String> iteratorVariableNames = new HashMap<String,String>();
    
    public static void main(String[] args) throws Exception{
        
        if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }
        
        String sCrawlDir = args[0].trim();       
        
       if (sCrawlDir == null || sCrawlDir.equals("")) {
           System.out.println("Crawl directory has to be specified");
           System.exit(1);
       }
       
       File f = new File(sCrawlDir);
       if(!f.exists()){
           System.out.println("Crawl directory does not exist - " + sCrawlDir);
           System.exit(1);
       }
       
       boolean append = false;
       
       try{
       if(args.length > 1)
           append = new Boolean(args[1].trim());
       } catch(Exception e) {
           System.out.println("2nd argument is ot a boolean...");
       }
       
       System.out.println("Scanning folder: " + sCrawlDir);
       
        csvWriter = new BufferedWriter(new FileWriter("RSI_issues.csv",append));
             
        if(!append)
         csvWriter.write("Family,Module,Product,FileName,Label,MethodName,MethodStartLineNo, RSI Variable Not Closed,Iterator Name\n");
        
        RowSetIteratorsNotClosed scanner = new RowSetIteratorsNotClosed();
        scanner.crawlDirectory(sCrawlDir);
        
        csvWriter.close();
          
   }
    
    private void crawlDirectory(String path) throws Exception{
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

    public boolean fileOfInterest(String sName) 
    {     
        if(!FamilyModuleHelper.isPathOfInterest(sName))
            return false;  
        
        if (sName.endsWith(".java")) {
         // java_files++;
          return true;
        }
      return false;
    }
    
    public void processFile(File fName) throws Exception{
        
        int open_braces = 0;
        int i = 0;
        int lineNo = 0;
        String methodName = "";
        String prevMethodName = "";
        int prevMethodStart = -1;
        int methodStartLineNo = -1;
        int methodEndLineNo = -1;
        String line = "";
        
        JavaFileParserHelper javaHelper = new JavaFileParserHelper(fName.getAbsolutePath());
        javaHelper.removeAllComments();
        String fileContents = javaHelper.getFileWithoutCommentsReadOnly();
        String[] lines = javaHelper.getFileContentsArrayWithoutComments(fileContents);
        
        while(i< lines.length) {
           
           line = lines[i];
           if(line.trim().equals("")){ //ignore blank lines
               i++;
               continue;
           }
           lineNo = i; //record line number of the begining of the line
           while(!(line.trim().endsWith(";") || line.trim().endsWith("{") || line.trim().endsWith("}") || (i == lines.length-1)))
               line += lines[++i];
           
           line = line.trim();   
           
          int num_braces = getNumBraces(line);
           open_braces += num_braces;
          
           if(open_braces == 2) { //method starts
               Matcher m = pMethodName.matcher(line);
               if(m.find()) {
                   methodName = m.group(1);
                   methodStartLineNo = lineNo;
                   if(prevMethodName.equals("")) {
                       prevMethodName = methodName;
                       prevMethodStart = methodStartLineNo;
                   }
                   
                   if(!methodName.equals(prevMethodName)){
                       localVariables = new HashMap<String,Boolean>();
                       processMethod(fName.getCanonicalPath(),prevMethodName,lines,prevMethodStart,methodStartLineNo-1);
                       prevMethodName = methodName;
                       prevMethodStart = methodStartLineNo;
                   }
                   //localVariables = new HashMap<String,String>();
               }
           }
            i++;
        }//end of while
        if(!methodName.equals("")){
            localVariables = new HashMap<String,Boolean>();
            processMethod(fName.getCanonicalPath(),methodName,lines,methodStartLineNo,lines.length-1); //process last method
        }
    }
    
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
    
    private void processMethod(String filePath, String methodName, String[] lines, int methodStartLineNo, int methodEndLineNo) throws Exception{
        
        String line = "";
        int lineNo = -1;
    
        int violationLineNo = -1;
        
        for(int i = methodStartLineNo; i < methodEndLineNo; i++) {
            line = lines[i];
            if(line.trim().equals(""))
                continue;
            lineNo = i; //record line number of the begining of the line
            while(!(line.trim().endsWith(";") || line.trim().endsWith("{") || line.trim().endsWith("}") || (i >= methodEndLineNo)))
                line += lines[++i];
            
            line = line.trim(); 
            
            if(line.contains("createRowSetIterator")) {
   
                violationLineNo = lineNo;
                Matcher m = pRowSetIterator.matcher(line);
                if(m.find()){
                    localVariables.put(m.group(1).trim(),false);
                    iteratorVariableNames.put(m.group(1).trim(),m.group(2).trim());
                }else {
                    System.out.println("RSI Pattern not matched:" + line);
                }
            } 
            if(line.contains("closeRowSetIterator")){
                Matcher m = pRSIClose.matcher(line);
                if(m.find()){
                    String var = m.group(1).trim();
                    if(localVariables.containsKey(var))
                        localVariables.put(var,true);
                }                
            }
        } //end of for - tranvered the method code
        
        for(Iterator<String> it = localVariables.keySet().iterator();it.hasNext();) {
            String var = it.next();
            boolean closed = localVariables.get(var);
            if(filePath.contains("/Printing.java") && methodName.equals("getTermsXMLData") && var.equals("logMessage"))
                continue;
            if(filePath.contains("/ChangeVersionViewImpl.java") && methodName.equals("createPendingDummyRows") && var.equals("iter"))
                continue;
            if(!closed)
                csvWriter.write(FamilyModuleHelper.getFileNameInfo(filePath) + methodName + "," + (methodStartLineNo+1) + 
                                "," + var + "," + iteratorVariableNames.get(var) + "\n" );
        }
            //
    }
}
