package oracle.apps.rup2scans;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JavaFileParserHelper;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.utility.LRGUtil;

public class RowSetIteratorsNotClosedLRG extends JoesBaseClass{
    
    private HashMap<String,Boolean> localVariables = new HashMap<String,Boolean>();
    private HashMap<String,String> iteratorVariableNames = new HashMap<String,String>();
    
    private static BufferedWriter writer;
    private static final Pattern pRowSetIterator = Pattern.compile("(\\w+)\\s*=\\s*.*createRowSetIterator\\((.*?)\\)");
    private static final Pattern pRSIClose = Pattern.compile("(\\w+)\\.closeRowSetIterator");
    private static final Pattern pMethodName = Pattern.compile("(?:(?:public|private|protected|static)\\s+)*\\s*\\w+(?:\\[\\])?(?:<\\w+>)?\\s+(\\w+)\\s*\\(.*\\).*?\\{",Pattern.DOTALL);
    
    private static final String issuetype = "RowSetIteratorsNotClosed";
    
    public RowSetIteratorsNotClosedLRG(){
      super(JoesBaseClass.CRAWL_TYPE.JAVA);
    }
    
    public static void main(String[] args){
        try {
            if (args.length != 1 || args[0] == null || args[0].equals("")) {
                System.out.println("Series has to be specified");
                System.exit(1);
            }

            String series = args[0];
          //String series = "FSM_MAIN_GENERIC";
          
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);

            writer = new BufferedWriter(new FileWriter("RowSetIteratorNotClosed_LRG.csv"));
            writer.write("Family,Module,Product,FileName,Label,MethodName,MethodStartLineNo, RSI Variable Not Closed,Iterator Name\n");

            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
          String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
          String viewRoot = serverTop + "/"+series+".rdd/"+label + "/";
          RowSetIteratorsNotClosedLRG scanner = new RowSetIteratorsNotClosedLRG();
          
            for (int j = 0; j < families.size(); j++) {
              
                String family = families.get(j);
              String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, issuetype);                    
              System.out.println("Crawling dir '" + viewRoot + sCrawlDir + "' for family: " + family);              
              scanner.crawlDirectoryNew(viewRoot,sCrawlDir,false);      
                
                System.out.println("Crawling done for family "+family+".");
            }
            writer.close();

            LRGUtil.addLabelInfoToAB(issuetype, series, label);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public void processFile(File fName, boolean doADE) {
        
      try{
        int open_braces = 0;
        int i = 0;
        int lineNo = 0;
        String methodName = "";
        String prevMethodName = "";
        int prevMethodStart = -1;
        int methodStartLineNo = -1;
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
               }
           }
            i++;
        }//end of while
        if(!methodName.equals("")){
            localVariables = new HashMap<String,Boolean>();
            processMethod(fName.getCanonicalPath(),methodName,lines,methodStartLineNo,lines.length-1); //process last method
        }
      }catch(Exception e){
        e.printStackTrace();
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
                writer.write(FamilyModuleHelper.getFileNameInfo(filePath) + methodName + "," + (methodStartLineNo+1) + 
                                "," + var + "," + iteratorVariableNames.get(var) + "\n" );
        }
    }

  protected String getSummaryReport() {
    return null;
  }

  protected String getSummaryReportSubject() {
    return null;
  }
}

