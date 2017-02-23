package oracle.apps.logs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;

import java.text.DateFormat;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.EJB;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;


public class LogAnalysis {
    private static int dlog_files = 0;
    private static int log_files = 0;

    static BufferedWriter outputFileWriter2; //pattern not matched - non-d logs
    static BufferedWriter outputFileWriter3; //pattern not matched - d logs
    
    static Workbook m_myBook;
    static Worksheet m_Sheet1;
    static Worksheet m_Sheet2;
    static Worksheet m_Sheet3;
    static int row1 = 2;
    static int row2 = 2;
    static int row3 = 2;
   Pattern pNonDiagnosticLog = Pattern.compile("<(.*?)>\\s*<(.*?)>\\s*(?:<.*?>\\s*)*<(.*?)>\\s*<(.*)>?");
    
    //(timestamp) (servername) (severity) (errCode) (module)([ ])* (message) 
    Pattern pDiagnosticLog = Pattern.compile("\\[(.*?)\\]\\s*(\\[.*?\\])\\s*(\\[.*?\\])\\s*(\\[.*?\\])\\s*(\\[.*?\\])\\s*(?:\\[.*\\]\\s*)*(.*)");
    
    //For diagnostic logs
    HashMap<String,HashSet<String>> errCodeMessages = new HashMap<String,HashSet<String>>();
    HashMap<String,Integer> errCodeCounts = new HashMap<String,Integer>();
    HashMap<String,Integer> messagesWithoutErrCodes = new HashMap<String,Integer>();
    
    //For non-diagnostic logs
    HashMap<String,HashSet<String>> errCodeMessages1 = new HashMap<String,HashSet<String>>();
    HashMap<String,Integer> errCodeCounts1 = new HashMap<String,Integer>();
    

    
    public static void main(String[] args) {
        
//        if(args.length < 1 || args[0] == null || args[0].equals("")) 
//                {
//                    System.out.println("Usage:  reviewLogWarnings.sh <path to your log files location> ");
//                    System.exit(1);
//                }
//        String sCrawlDir = args[0];
        //HCM B18 CDRM logs - adcdaq03, FIN B18 CDRM - adcdar12, FIN RUP1 CDRM - adcdai14, HCM RUP1 CDRM - adcdai12
        String sCrawlDir = "/net/adcdaq03/scratch/fusionapps/localdomain/domains/adcdaq03.us.oracle.com/HCMDomain/servers";
       // String sCrawlDir = "/scratch/sudgupta/Selenium_logs";
        System.out.println("Analyzing all files in directory:" + sCrawlDir);
        
        LogAnalysis logAnalyzer = new LogAnalysis();
        try {
            
//            outputFileWriter1 = new BufferedWriter(new FileWriter("log_warnings_scan1.csv") );
//            outputFileWriter1.write("ErrCode, Count, Statement \n"); 
            
            outputFileWriter2 = new BufferedWriter(new FileWriter("log_warnings_scan2.txt") );
            
            outputFileWriter3 = new BufferedWriter(new FileWriter("log_warnings_scan3.txt") );        
            
            m_myBook = new Workbook("Log Statements Scan");
            m_Sheet1 = new Worksheet("Diagnostic Logs");
            m_Sheet2 = new Worksheet("Log Stmts Without Error Codes");
            m_Sheet3 = new Worksheet("Non-diagnostic Logs");
            m_myBook.addWorksheet(m_Sheet1);
            m_myBook.addWorksheet(m_Sheet2);
            m_myBook.addWorksheet(m_Sheet3);
            
            m_Sheet1.addCell(new Cell(new CellLocation("A1"), new CellValue("errCode")));
            m_Sheet1.addCell(new Cell(new CellLocation("B1"), new CellValue("Category")));
            m_Sheet1.addCell(new Cell(new CellLocation("C1"), new CellValue("Count")));
            m_Sheet1.addCell(new Cell(new CellLocation("D1"), new CellValue("Sample message")));
           // m_Sheet1.addCell(new Cell(new CellLocation("D1"), new CellValue("Module")));
            
            m_Sheet2.addCell(new Cell(new CellLocation("A1"), new CellValue("message")));
            m_Sheet2.addCell(new Cell(new CellLocation("B1"), new CellValue("Category")));
            m_Sheet2.addCell(new Cell(new CellLocation("C1"), new CellValue("Count")));
            
            m_Sheet3.addCell(new Cell(new CellLocation("A1"), new CellValue("errCode")));
            m_Sheet3.addCell(new Cell(new CellLocation("B1"), new CellValue("Category")));
            m_Sheet3.addCell(new Cell(new CellLocation("C1"), new CellValue("Count")));
            m_Sheet3.addCell(new Cell(new CellLocation("D1"), new CellValue("Sample message")));
            
            logAnalyzer.crawlDirectory(sCrawlDir);       
            logAnalyzer.writeLogStatements();
            
            System.out.println("Number of Diagnostic Log files processed: "+ dlog_files); 
            System.out.println("Number of Non-Diagnostic Log files processed: "+ log_files);
            
          //  outputFileWriter1.close();
            outputFileWriter2.close();
            outputFileWriter3.close();
            
            FileOutputStream out = new FileOutputStream("LogWarnings_scan.xlsx");
            XLSXCreator.create(m_myBook, out);
            
        } catch(Exception e) {
            e.printStackTrace();       
        }
    }
    
    private void crawlDirectory(String path) throws Exception
      {
          if(path.contains(".ade_path"))
              return;
          if(path.contains("/metrics") || path.contains("/diagnostic_images") || path.contains("/metrics") 
             || path.contains("/owsm") || path.contains("/css") || path.contains("/registry"))
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
                  if(isDiagnosticLog(sName))               
                      processDiagnosticLog(listOfFiles[i]);      
                  if(isNonDiagnosticLog(sName))               
                      processNonDiagnosticLog(listOfFiles[i]);      
              }
              else if(listOfFiles[i].isDirectory()) 
                  crawlDirectory(listOfFiles[i].getAbsolutePath());          
              else 
                  System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);          
          }      
      }

    private boolean isDiagnosticLog(String sName) 
    {
        if (sName.contains(".log") && sName.contains("diagnostic")) {
          dlog_files++;
          return true;
        }
      return false;
    }
    
    private boolean isNonDiagnosticLog(String sName) 
    {
        if (sName.contains(".log") && !sName.contains("diagnostic") && !sName.contains("access.")) {
          log_files++;
          return true;
        }
      return false;
    }
    
    private void processDiagnosticLog(File f) {
        try{
        
        Date dateLastModified =  new Date(f.lastModified());
        Date lastweek = new Date(System.currentTimeMillis() - (10*1000*60*60*24));
       
        //ignore files older than last week
        if(dateLastModified.compareTo(lastweek) < 0)
            return;
            
        BufferedReader logReader = new BufferedReader(new FileReader(f));
        String line = "";
        Matcher m;
        
        while((line = logReader.readLine()) != null) {
            m = pDiagnosticLog.matcher(line);
            if(m.find()) {
              
              String datetime = m.group(1).trim().substring(0,19);
              datetime = datetime.replace('-', '/');
              datetime = datetime.replace('T', ':');
              DateFormat df = new SimpleDateFormat("yyyy/MM/dd:hh:mm:ss");
              Date d = null;
              try{
                  d = df.parse(datetime);
              } catch (Exception e) {
                  e.printStackTrace();
              }
              
              if(d != null && d.compareTo(lastweek) < 0)
                  continue;
              
              String serverName = m.group(2).trim();
              String category = m.group(3).trim();
              String errCode = m.group(4).trim();
              String module = m.group(5).trim();
              String message = m.group(6).trim();
              
              if(errCode.equals("[]")){
                  int count = 0;
                  if(messagesWithoutErrCodes.containsKey(category + "::" + message))
                      count = messagesWithoutErrCodes.get(category + "::" + message);
                  
                  messagesWithoutErrCodes.put(category + "::" + message,count+1);
                  continue;
              }
              HashSet<String> messages = new HashSet<String>();
              if(errCodeMessages.containsKey(errCode))
                  messages = errCodeMessages.get(errCode);
              
              messages.add(serverName + "::" + category + "::" + module + "::" + message);
              errCodeMessages.put(errCode, messages);
              
              //errCodeCounts
              int count = 0;
              if(errCodeCounts.containsKey(errCode))
                  count = errCodeCounts.get(errCode);
              
              errCodeCounts.put(errCode,count+1);
              
                
            }else {
               // outputFileWriter3.write(line + "\n");
            }
        }
        }catch(Exception e) {
            System.out.println("Exception in processing file: " + f.getAbsolutePath());
            System.out.println(e.getMessage());
            System.out.println("Skipping ...");
        }
    }
    
    private void processNonDiagnosticLog(File f) {
        try{
            
        Date dateLastModified =  new Date(f.lastModified());
        Date lastweek = new Date(System.currentTimeMillis() - (10*1000*60*60*24));
        
        //ignore files older than last week
        if(dateLastModified.compareTo(lastweek) < 0)
            return;
        BufferedReader logReader = new BufferedReader(new FileReader(f));
        String line = "";
        Matcher m;
        
        while((line = logReader.readLine()) != null) {
            m = pNonDiagnosticLog.matcher(line);
            if(m.find()) {            
            
                String datetime = m.group(1).trim();
                DateFormat df = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a z");
                Date d = null;
                try{
                    d = df.parse(datetime);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                if(d != null && d.compareTo(lastweek) < 0)
                    continue;
                
              String category = m.group(2).trim();
              String errCode = m.group(3).trim();
              String message = m.group(4).trim();
              
              HashSet<String> messages = new HashSet<String>();
              if(errCodeMessages1.containsKey(errCode))
                  messages = errCodeMessages1.get(errCode);
              
              messages.add(category + "::" + message);
              errCodeMessages1.put(errCode, messages);
              
              //errCodeCounts
              int count = 0;
              if(errCodeCounts1.containsKey(errCode))
                  count = errCodeCounts1.get(errCode);
              
              errCodeCounts1.put(errCode,count+1);              
                
            }else {
                //outputFileWriter2.write(line + "\n");
            }
        }
        }catch(Exception e) {
            System.out.println("Exception in processing file: " + f.getAbsolutePath());
            System.out.println(e.getMessage());
            System.out.println("Skipping ...");
        }
    }
    
    private void writeLogStatements() {
        
        for(Iterator<String> it1 = errCodeMessages.keySet().iterator(); it1.hasNext();) {
            
            String errCode = it1.next();
            HashSet<String> messages = errCodeMessages.get(errCode);
           // HashSet<String> rows = new HashSet<String>();
            String comment = "";
            String samplemessage = "";
            String category = "";
            int i = 1;
            for(Iterator<String> it2 = messages.iterator(); it2.hasNext();) {
                String msg = it2.next();
                if(i == 1) {
                    String[] parts = msg.split("::");
                    samplemessage = parts[parts.length-1];
                    category = parts[1];
                }
                if(i > 5)
                    break;
               // String[] parts = msg.split(":");
               // rows.add(parts[0]+":"+parts[1]+":"+parts[2]);
                comment += msg + "\n";
                i++;
            }
            
            String c1 = getCellLocation(0, row1);
            String c2 = getCellLocation(1, row1);
            String c3 = getCellLocation(2, row1);
            String c4 = getCellLocation(3, row1);
            
            m_Sheet1.addCell(new Cell(new CellLocation(c1), new CellValue(errCode)));
            m_Sheet1.addCell(new Cell(new CellLocation(c2), new CellValue(category)));
            m_Sheet1.addCell(new Cell(new CellLocation(c3), new CellValue(errCodeCounts.get(errCode))));
            m_Sheet1.addCell(new Cell(new CellLocation(c4), new CellValue(samplemessage)));
            m_Sheet1.createComment(comment, c4, 10, 40);
            row1++;
        }
        
        for(Iterator<String> it = messagesWithoutErrCodes.keySet().iterator(); it.hasNext();) {
            String msg = it.next();
            int count = messagesWithoutErrCodes.get(msg);
            String[] parts = msg.split("::");
            
            String message = "";
            String category = parts[0];
            if(parts.length > 1)
                message = parts[1];
            
            
            String c1 = getCellLocation(0, row2);
            String c2 = getCellLocation(1, row2);
            String c3 = getCellLocation(2, row2);
            
            m_Sheet2.addCell(new Cell(new CellLocation(c1), new CellValue(message)));
            m_Sheet2.addCell(new Cell(new CellLocation(c2), new CellValue(category)));
            m_Sheet2.addCell(new Cell(new CellLocation(c3), new CellValue(count)));       
            row2++;
        }
        
        for(Iterator<String> it1 = errCodeMessages1.keySet().iterator(); it1.hasNext();) {
            
            String errCode = it1.next();
            HashSet<String> messages = errCodeMessages1.get(errCode);
           // HashSet<String> rows = new HashSet<String>();
            String comment = "";
            String samplemessage = "";
            String category = "";
            int i = 1;
            for(Iterator<String> it2 = messages.iterator(); it2.hasNext();) {
                String msg = it2.next();
                if(i == 1) {
                    String[] parts = msg.split("::");
                    samplemessage = parts[parts.length-1];
                    category = parts[0];
                 
                }
                if(i > 5)
                    break;
               // String[] parts = msg.split(":");
               // rows.add(parts[0]+":"+parts[1]+":"+parts[2]);
                comment += msg + "\n";
                i++;
            }
            
            String c1 = getCellLocation(0, row3);
            String c2 = getCellLocation(1, row3);
            String c3 = getCellLocation(2, row3);
            String c4 = getCellLocation(3, row3);
            
            m_Sheet3.addCell(new Cell(new CellLocation(c1), new CellValue(errCode)));
            m_Sheet3.addCell(new Cell(new CellLocation(c2), new CellValue(category)));
            m_Sheet3.addCell(new Cell(new CellLocation(c3), new CellValue(errCodeCounts1.get(errCode))));
            m_Sheet3.addCell(new Cell(new CellLocation(c4), new CellValue(samplemessage)));
            m_Sheet3.createComment(comment, c4, 10, 40);
            row3++;
        }
        
    }
            
    private String getCellLocation(int col, int row) 
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
    }
    
}
