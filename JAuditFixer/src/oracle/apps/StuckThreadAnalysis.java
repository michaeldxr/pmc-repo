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


public class StuckThreadAnalysis {
    private Workbook m_myBook;
    Worksheet m_beansSheet;
    Worksheet m_membersSheet;

    int m_currentBeanRow = 2;
    int m_currentMemberRow = 2;
    int rowNumber=0;
     
    public StuckThreadAnalysis() {
        
        m_myBook = new Workbook("StuckThreads");
        m_beansSheet = new Worksheet("Stuck Threads");
        m_myBook.addWorksheet(m_beansSheet);

        m_beansSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Row Number")));
        //m_beansSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Application")));
        m_beansSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("File Name")));
        m_beansSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Line Number")));
        m_beansSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("Error Type")));
        m_beansSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Thread Number")));
        m_beansSheet.addCell(new Cell(new CellLocation("F1"), new CellValue("Execute Thread")));
        m_beansSheet.addCell(new Cell(new CellLocation("G1"), new CellValue("Time")));
        m_beansSheet.addCell(new Cell(new CellLocation("H1"), new CellValue("Referrer")));
        m_beansSheet.addCell(new Cell(new CellLocation("I1"), new CellValue("User")));
        m_beansSheet.addCell(new Cell(new CellLocation("J1"), new CellValue("JSESSIONID")));
      //  m_beansSheet.addCell(new Cell(new CellLocation("K1"), new CellValue("AWR")));
         

    
    }
    public static void main(String[] args) 
    {
     
                                                                                                            
//        if(args.length !=2 || args[0] == null || args[1] == null || args[0].equals("") || args[1].equals("")) 
//        {
//            System.out.println("Usage:  stuckThreadAnalysis.sh <path to your log files> <output excel file  name eg: gllog.xlsx> ");
//            System.exit(1);
//        }

        String sCrawlDir = args[0];
        String sOutpFilename = args[1];
       
           String family=null;
           String env=null;
       if (args.length > 2)
         family=args[2];
        if (args.length > 3)
          env=args[3];
//        Integer currentdate = new Integer(args[2]);
//        Integer numberofdays = new Integer(args[3]);
        StuckThreadAnalysis stuckThreadAnalysis = new StuckThreadAnalysis();
        Connection conn=null; 
        Statement stmt=null;
        try {
        DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
        conn = DriverManager.getConnection("jdbc:oracle:thin:@den65024fwks.us.oracle.com:1522:den65024","fusion","fusion");
        stmt = conn.createStatement();
        } catch (Exception e){System.out.println(e);}
                                   
                         
        stuckThreadAnalysis.crawlDirectory(sCrawlDir,family,env,conn,stmt,sOutpFilename);
        try {
            conn.commit();
       conn.close();
            stmt.close();
        } catch(Exception e){System.out.println(e);}
        FileOutputStream out = null;
        try{
            out = new FileOutputStream(sOutpFilename);
            XLSXCreator.create(stuckThreadAnalysis.m_myBook, out);
        }
        catch(Exception e) {
            e.printStackTrace();            
        }



    }
    
    public void crawlDirectory(String path,String OutputFileName,String env,Connection conn, Statement stmt,String sOutpFilename) 
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
                if(sName.contains("log") )
               {
                    processLogFile(listOfFiles[i],OutputFileName,env,conn,stmt,sOutpFilename);
               }
              
            }
            else if(listOfFiles[i].isDirectory()) {
                crawlDirectory(listOfFiles[i].getAbsolutePath(),OutputFileName,env,conn, stmt,sOutpFilename);
            }
            else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
        
    } //End CrawlDirectory
    
    public void processLogFile(File file,String OutputFileName,String env,Connection conn, Statement stmt,String sOutpFilename) {
        String absPath = file.getAbsolutePath();
       // System.out.println("Processing file: " + absPath);

        XmlFixer fileFixer = null;
        String fileContents = "";
        try {

                findStuckThreads(file, fileContents,OutputFileName,env,conn, stmt,sOutpFilename);
           
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
                                        String fileContentsWithoutComments,String family,String env,Connection conn, Statement stmt,String sOutpFilename) throws Exception {
        String fileName = file.getName();
        StringBuffer sb = new StringBuffer();
        Long lineNum=0L;
      
        FileReader reader = new FileReader(file);
        BufferedReader fileReader = new BufferedReader(reader);
        if (fileReader == null)
            return;
        String line=null ;
        String stime=null;
        String referrer=null;
        String user=null;
        String threadid=null;
       // boolean parked=false;
        StringBuffer stack=new StringBuffer();
        boolean foundStuck=false;
        boolean startStack=false;
        boolean isser=false;
      boolean isserexcep=false;
        String product=null;
        String threadInfo=null;
        String errorType= "Other";
        String jsessionid=null;
     
        try {

         
             int t = 0;
            while ((line = fileReader.readLine()) != null) {
                lineNum++;
               
              if (line.length() > 30) {
                  if (line.substring(1, 29).contains("PST")||line.substring(1, 29).contains("PDT")||line.substring(1, 29).contains("UTC")){
                    stime = line.substring(1, 29);
                  if (stime.contains("PST"))
                      t = stime.indexOf("PST");
                  if (stime.contains("PDT"))
                      t = stime.indexOf("PDT");
                      if (stime.contains("UTC"))
                          t = stime.indexOf("UTC");
                  if (t != 0) {

                       stime = stime.substring(0, t + 3);
                  }
                  }
                  if (!(line.substring(1, 29).contains("PDT") ||
                        line.substring(1, 29).contains("PST") ||line.substring(1, 29).contains("UTC") )) {
                      if (line.contains("DATE =")) {
                          stime =
              line.substring(line.indexOf("DATE =") + 7, line.indexOf("DATE =") + 35);

                      }
                  }
                  if (stime==null){
                    if (line.contains("PST")||line.contains("PDT")||line.contains("UTC")){
                     stime = line;
                    if (stime.contains("PST"))
                        t = stime.indexOf("PST");
                    if (stime.contains("PDT"))
                        t = stime.indexOf("PDT");
                        if (stime.contains("UTC"))
                            t = stime.indexOf("UTC");
                    if (t != 0) {

                         stime = stime.substring(5, t + 3);
                    }
                  }
                  }
                  
              }
                
            if (stackstart(line))
                    startStack=true;
              if (containsSerializable(line)) {
                startStack=true;
                foundStuck=true;
                isserexcep=true;
                  errorType = "Not Serializable Exception";
              }
              if (containsCouldNotDeserialize(line)) {
                startStack=true;
                foundStuck=true;
                isser=true;
                  errorType = "Could not deserialize";
              }
              
                if (containsOOM(line) && startStack){
                  errorType = "Out of Memory";
                }
                if (containsStuck(line)) {
                 
                    foundStuck=true;
                    if (containsStuckAndParked(line))
                        errorType = "Parked";

                    try {
                       t = 0;


                        if (stime == null) {
                            if (line.length() > 30) {
                                if (line.substring(1, 29).contains("PST")||line.substring(1, 29).contains("PDT")||line.substring(1, 29).contains("UTC")){
                                 stime = line.substring(1, 29);
                                 
                                if (stime.contains("PST"))
                                    t = stime.indexOf("PST");
                                if (stime.contains("PDT"))
                                    t = stime.indexOf("PDT");
                                    if (stime.contains("UTC"))
                                        t = stime.indexOf("UTC");
                                if (t != 0) {

                                     stime = stime.substring(0, t + 3);
                                }
                                }
                                if (!(line.substring(1, 29).contains("PDT") ||
                                      line.substring(1, 29).contains("PST") ||line.substring(1, 29).contains("UTC") )) {
                                    if (line.contains("DATE =")) {
                                        stime =
line.substring(line.indexOf("DATE =") + 7, line.indexOf("DATE =") + 35);

                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("time * " + e);
                    }
                    if (containsExecuteThread(line)) {
                        int pos = line.indexOf("ExecuteThread:");

                        threadid = line.substring(pos + 16, pos + 20);
                        if (threadid.contains("'")) {
                            int apospos = threadid.indexOf("'");
                            threadid = threadid.substring(0, apospos);
                        }
                    }
                    if (containsThread(line))
                        threadInfo = line.substring(0, line.indexOf(' '));
                    continue;
                   
                }

                if (startStack && foundStuck) {
                    stack.append(line);
                    stack.append("\n");
                }
              if (containsOOM(line) && startStack)
                  errorType = "Out of Memory";
                if (containsStuckAndParked(line))
                    errorType = "Parked";

                if (containsSlowSql(line) && startStack) {
                    errorType = "Slow SQL";
                }
              if (containsViewPortLock(line) && startStack) {
                  errorType = "Viewport Lock (known issue)";
              }
              if (containsSerializable(line)) {
                startStack=true;
                  errorType = "Not Serializable Exception";
              }
              if (containsCouldNotDeserialize(line)) {
                startStack=true;
                foundStuck=true;
                  errorType = "Could not deserialize";
              }
                if (containsBlocked(line) && startStack)
                    errorType = "Blocked";
                if (containsXdo(line) && startStack)
                    errorType = "XDO";
              if (containsOOM(line) && startStack)
                  errorType = "Out of Memory";
                if (containsLDAP(line) && startStack)
                    errorType = "LDAP";
                if (containsBundle(line) && startStack)
                    errorType = "Resource Bundle";
                if (containsApplCp(line) && startStack)
                    errorType = "ESS";
                if (containsMds(line) && startStack)
                    errorType = "MDS";
                if (containsJms(line) && startStack)
                    errorType = "JMS";
                if (containsLogging(line) && startStack)
                    errorType = "Logging";
                if (containsExecuteThread(line)) {
                    int pos = line.indexOf("ExecuteThread:");

                    threadid = line.substring(pos + 16, pos + 20);
                    if (threadid.contains("'")) {
                        int apospos = threadid.indexOf("'");
                        threadid = threadid.substring(0, apospos);
                    }
                }
                if (errorType.equals("Parked")) {
                    if (containsConcurrentLock(line) && startStack)
                        errorType = "Parked Waiting on Lock";
                }

                if (containsAsScheduler(line) && startStack)
                    errorType = "Scheduler";
                if (containsGroovy(line) && startStack)
                    errorType = "Groovy";
                if (stackstart(line) && foundStuck) {

                  if (containsOOM(line) && startStack)
                      errorType = "Out of Memory";
                    if (containsStuckAndParked(line))
                        errorType = "Parked";
                    if (errorType.equals("Parked")) {
                        if (containsConcurrentLock(line) && startStack)
                            errorType = "Parked Waiting on Lock";
                    }
                    if (containsSlowSql(line) && startStack) {

                        errorType = "Slow SQL";
                    }
                    if (containsBlocked(line) && startStack)
                        errorType = "Blocked";
                    if (containsXdo(line) && startStack)
                        errorType = "XDO";
                  if (containsViewPortLock(line) && startStack) {
                      errorType = "Viewport Lock (known issue)";
                  }
                  if (containsSerializable(line)) {
                    startStack=true;
                      errorType = "Not Serializable Exception";
                  }
                  if (containsCouldNotDeserialize(line)) {
                    startStack=true;
                    foundStuck=true;
                      errorType = "Could not deserialize";
                  }
                  if (containsOOM(line) && startStack)
                      errorType = "Out of Memory";
                    if (containsLDAP(line) && startStack)
                        errorType = "LDAP";
                    if (containsBundle(line) && startStack)
                        errorType = "Resource Bundle";
                    if (containsApplCp(line) && startStack)
                        errorType = "ESS";
                    if (containsMds(line) && startStack)
                        errorType = "MDS";
                    if (containsJms(line) && startStack)
                        errorType = "JMS";
                    if (containsLogging(line) && startStack)
                        errorType = "Logging";

                    if (containsAsScheduler(line) && startStack)
                        errorType = "Scheduler";
                    if (containsGroovy(line) && startStack)
                        errorType = "Groovy";
                    if (containsThread(line))
                        threadInfo = line.substring(0, line.indexOf(' '));
                    if (containsExecuteThread(line)) {
                        int pos = line.indexOf("ExecuteThread:");
                        threadid = line.substring(pos + 16, pos + 20);
                        if (threadid.contains("'")) {
                            int apospos = threadid.indexOf("'");
                            threadid = threadid.substring(0, apospos);
                        }
                    }

                    startStack = true;
                    stack.append(line);
                    stack.append("\n");
                    continue;
                }
                


                if (containsReferrer(line) && foundStuck) {

                    referrer = line.substring(9);

                    continue;

                }
                if (containsUserName(line) && foundStuck) {

                    int i = line.lastIndexOf("oracle.com");
                    int j =
                        line.substring(0, line.lastIndexOf("oracle.com")).lastIndexOf("~");
                    user = line.substring(j + 1, i + 10);

                    continue;
                }

                if (containsJSessionId(line) && foundStuck) {
                    try {
                        int i = line.indexOf("JSESSIONID");
                        String s = line.substring(i);
                        int j = s.indexOf(";");
                        jsessionid = s.substring(11);
                        if (j != -1)
                            jsessionid = s.substring(11, j);
                    } catch (Exception e) {
                        System.out.println(e);
                    }

                    continue;
                }


                if (foundStuck &&
                    stackend(line)) { //Stack ended and stuck thread info collection ended
                    if (null == user)
                        user = "No User Information";
                    rowNumber++;
                 
                  if(isserexcep)
                  errorType = "Not Serializable Exception";
                 
                 
                    if(isser)
                   errorType = "Could not deserialize";
                  
                    recordStuckInXlsx(rowNumber, jsessionid, fileName,
                                      lineNum.toString(), stime, referrer,
                                      user, stack.toString(), threadInfo,
                                      product, errorType, family, env, conn,
                                      stmt, threadid, sOutpFilename);
                    foundStuck = false;
                    startStack = false;
                  isser=false;
                  isserexcep=false;
                    stime = null;
                    threadInfo = null;
                    product = null;
                    referrer = null;
                    user = null;
                    stack = null;
                    jsessionid = null;
                    threadid = null;
                    errorType = "Other";

                    stack = new StringBuffer();
                    continue;
                }

                line = null;
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
      public boolean containsViewPortLock(String str) {
        try {
            if (!isEmpty(str) &&
                ((str.contains("lockViewPortRequestLock")))) {
                return true;
            }
            return false;
        }catch (Exception e) {
                return false;
            }
      }
      
      public boolean containsSerializable(String str) {
        try {
            if (!isEmpty(str) &&
                ((str.contains("NotSerializableException")))) {
                return true;
            }
            return false;
        }catch (Exception e) {
                return false;
            }
      }
      public boolean containsCouldNotDeserialize(String str) {
        try {
            if (!isEmpty(str) &&
                ((str.contains("Could not deserialize")))) {
                return true;
            }
            return false;
        }catch (Exception e) {
                return false;
            }
      }
        public boolean containsJSessionId(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("JSESSIONID")))) {
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
                    str.contains("ExecuteThread")&&str.contains("STUCK")) {
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
        public boolean containsUserName(String str) {
            try {
                if (!isEmpty(str) &&
                    ((str.contains("oracle.com")))) {
                    return true;
                }
                return false;
            }catch (Exception e) {
                    return false;
                }
            }
        public void recordStuckInXlsx(int RowNumber, String JSESSIONID,String filename, String linenum,String time, String referrer, String username, String stack,String info,String product,String errorType,String family,String env,Connection conn,Statement stmt,String threadid,String sOutpFilename)
        {
            String l1 = getCellLocation(0, m_currentBeanRow);
            String l2 = getCellLocation(1, m_currentBeanRow);
            String l3 = getCellLocation(2, m_currentBeanRow);
            String l4 = getCellLocation(3, m_currentBeanRow);
            String l5 = getCellLocation(4, m_currentBeanRow);
            String l6 = getCellLocation(5, m_currentBeanRow);
            String l7= getCellLocation(6, m_currentBeanRow);
            String l8= getCellLocation(7, m_currentBeanRow);
            String l9= getCellLocation(8, m_currentBeanRow);
            String l10= getCellLocation(9, m_currentBeanRow);
           
            
            m_beansSheet.addCell(new Cell(new CellLocation(l1), new CellValue(new Integer(RowNumber).toString())));

            m_beansSheet.addCell(new Cell(new CellLocation(l2), new CellValue(filename)));
            m_beansSheet.addCell(new Cell(new CellLocation(l3), new CellValue(linenum)));
            m_beansSheet.addCell(new Cell(new CellLocation(l4),
                                          new CellValue(errorType)));
       
            m_beansSheet.addCell(new Cell(new CellLocation(l5), new CellValue(info)));
            m_beansSheet.addCell(new Cell(new CellLocation(l6), new CellValue(threadid)));
            m_beansSheet.addCell(new Cell(new CellLocation(l7), new CellValue(time)));
            m_beansSheet.addCell(new Cell(new CellLocation(l8), new CellValue(referrer)));
            if (null==referrer)
                m_beansSheet.addCell(new Cell(new CellLocation(l8), new CellValue("No Referrer")));
            m_beansSheet.addCell(new Cell(new CellLocation(l9), new CellValue(username)));
            
            m_beansSheet.addCell(new Cell(new CellLocation(l10), new CellValue(JSESSIONID)));
            if (null==JSESSIONID)
            m_beansSheet.addCell(new Cell(new CellLocation(l10),
                                          new CellValue("No JSESSIONID")));
        m_beansSheet.createComment(stack, l2,12,40);
        m_currentBeanRow++;
         
        boolean rowexist=false;
            PreparedStatement pst=null;
            ResultSet rs=null;
            int rc=0;
        try {
             pst =
                conn.prepareStatement("select count(*) as rowcount from stuckthreads where threadtime=? and product=? and env=? and linenum=?");
            pst.setString(1, time);
            pst.setString(2, sOutpFilename);
            pst.setString(3, env);
            pst.setString(4,linenum);
            rs = pst.executeQuery();
            rs.next();
            rc=rs.getInt(1);
       
        }

        catch (Exception e) {
            System.out.println("Error in SQL"+e);
        }
        finally{
            try{
            pst.close();
            rs.close();
            }catch(Exception e){System.out.println(e);}
        }

        if (rc < 1) {

    
            String s = null;
            if (time!=null){
            String timeformatted =
                time.substring(time.indexOf(' ') + 1, time.indexOf(',')) +
                "-" + time.substring(0, 3) + "-" +
                time.substring(time.indexOf(',') + 2, time.indexOf(',') + 6);
            if (env != null && family != null && !errorType.equals("Not Serializable Exception") && !errorType.equals("Could not deserialize")) {
                
                    try {
                        s =
   "insert into stuckthreads(datetime,env,family,filename,threadid,product,threadtime,linenum,datetimestring) values (to_date('" +
       timeformatted + "','DD-MON-RRRR'),'" + env + "','" + family + "','" +
       filename + "','" + info + "','" + sOutpFilename + "','" + time + "','"+ linenum+"','"+ timeformatted +"')";
                        int count = stmt.executeUpdate(s);
                    } catch (Exception e) {
                        System.out.println("Issue with insert "+e);
                    }
                
            }
            }
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