package oracle.apps;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;


import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.XmlFixer;
import java.sql.*;


public class SerializationAnalysis {
    private Workbook m_myBook;
    Worksheet m_beansSheet;
    Worksheet m_membersSheet;

    int m_currentBeanRow = 2;
    int m_currentMemberRow = 2;
    int rowNumber=0;
     
    public SerializationAnalysis() {
        
        m_myBook = new Workbook("Serialization");
        m_beansSheet = new Worksheet("Serialization Issues");
        m_myBook.addWorksheet(m_beansSheet);

        m_beansSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Row Number")));
       m_beansSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("File Name")));
        m_beansSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Line Number")));
       m_beansSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("Execute Thread")));
        m_beansSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Time")));
        m_beansSheet.addCell(new Cell(new CellLocation("F1"), new CellValue("Page Flow Scope")));
        m_beansSheet.addCell(new Cell(new CellLocation("G1"), new CellValue("Object Key")));
       

    
    }
    public static void main(String[] args) 
    {
     
                                                                                                            

        String sCrawlDir = args[0];
        String sOutpFilename = args[1];

        String family = null;
        String env = null;
        if (args.length > 2)
            family = args[2];
        if (args.length > 3)
            env = args[3];
        SerializationAnalysis SerializationAnalysis =
            new SerializationAnalysis();


        SerializationAnalysis.crawlDirectory(sCrawlDir, family, env,
                                             sOutpFilename);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(sOutpFilename);
            XLSXCreator.create(SerializationAnalysis.m_myBook, out);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    
    public void crawlDirectory(String path,String OutputFileName,String env,String sOutpFilename) 
    {
      

        File folder = new File(path);
         File[] listOfFiles = folder.listFiles();
        if(listOfFiles == null)
            return;
        
        
        for(int i = 0; i <listOfFiles.length; i++)
        {
            if(listOfFiles[i].isFile()) 
            {
                String sName = listOfFiles[i].getAbsolutePath();
                if(sName.contains("diagnostic") )
               {
                    processLogFile(listOfFiles[i],OutputFileName,env,sOutpFilename);
               }
              
            }
            else if(listOfFiles[i].isDirectory()) {
                crawlDirectory(listOfFiles[i].getAbsolutePath(),OutputFileName,env,sOutpFilename);
            }
            else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
        
    } //End CrawlDirectory
    
    public void processLogFile(File file,String OutputFileName,String env,String sOutpFilename) {
        String absPath = file.getAbsolutePath();
       // System.out.println("Processing file: " + absPath);

        XmlFixer fileFixer = null;
        String fileContents = "";
        try {

                findStuckThreads(file, fileContents,OutputFileName,env,sOutpFilename);
           
        } catch (Exception ex) {
            System.out.println("Exception in file: Skipping..  " +
                               absPath + " Exception: " + ex.getMessage());
            return;
        }


    }
    
    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }
    
    public void findStuckThreads(File file,
                                        String fileContentsWithoutComments,String family,String env,String sOutpFilename) throws Exception {
        String fileName = file.getName();
        StringBuffer sb = new StringBuffer();
        Long lineNum=0L;
      
        FileReader reader = new FileReader(file);
        BufferedReader fileReader = new BufferedReader(reader);
        if (fileReader == null)
            return;
        String line=null ;
        String stime=null;
        String threadid=null;
        String pageFlowScope=null;
        String objectKey=null;
        try {

         
             int t = 0;
            while ((line = fileReader.readLine()) != null) {
                lineNum++;
                if (line.contains("Scope object serialization failed")){
                    
                    //get time part
                  stime= line.substring(1,31);
                  //get Execute Thread
                  if (containsExecuteThread(line)) {
                      int pos = line.indexOf("ExecuteThread:");

                      threadid = line.substring(pos + 16, pos + 20);
                      if (threadid.contains("'")) {
                          int apospos = threadid.indexOf("'");
                          threadid = threadid.substring(0, apospos);
                      }
                  }
                  if (containsPageFlowScope(line)) {
                      int pfpos = line.indexOf("pageFlowScope[");
                      
                       pageFlowScope = line.substring(pfpos + 14);
                      if (pageFlowScope.contains("]]")) {
                          int apospos = pageFlowScope.indexOf("]]");
                          pageFlowScope = pageFlowScope.substring(0, apospos);
                      }
                  }
                  if (containsObjectKey(line)) {
                      int obpos = line.indexOf("objectKey=");
                      
                       objectKey = line.substring(obpos + 11);
                       int apospos = objectKey.indexOf("',");
                          objectKey = objectKey.substring(0, apospos);
                      
                  }
                  rowNumber++;
                  //jsessionid, referrer,user,threadiinfo,prodct,errortype,family,env
                  recordStuckInXlsx(rowNumber,  fileName,
                                    lineNum.toString(), stime, 
                                    line.toString(),  threadid, sOutpFilename,pageFlowScope,objectKey);
                }
              line = null;
              stime=null;
              threadid=null;
              pageFlowScope=null;
              objectKey=null;
                   continue;
                
            } //End read line
        } //End try

        catch (Exception e) {
            System.out.println("Exception from processing " + e + line + lineNum);
        }
        fileReader.close();
        

    }
    
    public boolean containsStuck(String str) {
        try {
            if (!isEmpty(str) &&
                ((str.contains("STUCK"))||(str.contains("OutOfMemory")))) {
                return true;
            }
            return false;
        }catch (Exception e) {
                return false;
            }
    }
        public boolean containsXdo(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("xdo")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
      public boolean containsOOM(String str) {
          try {
              if (!isEmpty(str) &&
                  ((str.contains("OutOfMemory")))) {
                  return true;
              }
              return false;
          }catch (Exception e) {
                  return false;
              }
      }
        public boolean containsEL(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("javax.el")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
        public boolean containsJms(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("weblogic.jms")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
        public boolean containsGroovy(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("org.codehaus.groovy")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
        public boolean containsSecurity(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("oracle.security.jps")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
        public boolean containsLogging(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("logging")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }   
        public boolean containsConcurrentLock(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("concurrent.lock")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
        public boolean containsBundle(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("resourcebundle")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
        public boolean containsAsScheduler(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("as.scheduler")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
        public boolean containsApplCp(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("applcp")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
        public boolean containsLDAP(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("LDAP")||(str.contains("ldap"))))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
        public boolean containsPageFlowScope(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("pageFlowScope")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
        public boolean containsStuckAndParked(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("STUCK"))&&str.contains("parked"))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
        public boolean containsThread(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.substring(0,25).contains("Thread")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
        public boolean containsExecuteThread(String str) {
            try {
                if (!isEmpty(str) &&
                    str.contains("ExecuteThread")) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
        }
        public boolean stackstart(String str) {
            
            try {
                if (!isEmpty(str) &&
                    ((str.contains("Stack trace"))||(str.contains("DAEMON"))||(str.contains("OutOfMemory")))) {
                     return true;
                }
                return false;
            }catch (Exception e) {
                System.out.println(e);
                    return false;
                }
            }
        public boolean containsBlocked(String str) {
            
            try {
                if (!isEmpty(str) &&
                    ((str.contains("Blocked")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                System.out.println(e);
                    return false;
                }
            }
        public boolean containsSlowSql(String str) {
            
            try {
                if (!isEmpty(str) &&
                    ((str.contains("jdbc.driver")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                System.out.println(e);
                    return false;
                }
            }
        public boolean containsMds(String str) {
            
            try {
                if (!isEmpty(str) &&
                    ((str.contains("oracle.mds")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                System.out.println(e);
                    return false;
                }
            }
        public boolean stackend(String str) {
            try {
                if (!isEmpty(str) && str.trim().length()==1 &&
                    ((str.contains(">")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
            }
        public boolean containsReferrer(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("Referer")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
            }
        public boolean containsObjectKey(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("objectKey")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
            }
     public void recordStuckInXlsx(int RowNumber,String filename, String linenum,String time, String stack,
                                   String threadid,String sOutpFilename,String pageFlowScope, String objectKey)
        {
            String l1 = getCellLocation(0, m_currentBeanRow);
        String l2 = getCellLocation(1, m_currentBeanRow);
        String l3 = getCellLocation(2, m_currentBeanRow);
        String l4 = getCellLocation(3, m_currentBeanRow);
        String l5 = getCellLocation(4, m_currentBeanRow);
        String l6 = getCellLocation(5, m_currentBeanRow);
        String l7 = getCellLocation(6, m_currentBeanRow);


        m_beansSheet.addCell(new Cell(new CellLocation(l1),
                                      new CellValue(new Integer(RowNumber).toString())));

        m_beansSheet.addCell(new Cell(new CellLocation(l2),
                                      new CellValue(filename)));
        m_beansSheet.addCell(new Cell(new CellLocation(l3),
                                      new CellValue(linenum)));
        m_beansSheet.addCell(new Cell(new CellLocation(l4),
                                      new CellValue(threadid)));
        m_beansSheet.addCell(new Cell(new CellLocation(l5),
                                      new CellValue(time)));
        m_beansSheet.addCell(new Cell(new CellLocation(l6),
                                      new CellValue(pageFlowScope)));
        m_beansSheet.addCell(new Cell(new CellLocation(l7),
                                      new CellValue(objectKey)));

        m_beansSheet.createComment(stack, l2, 12, 40);
        m_currentBeanRow++;

            
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
