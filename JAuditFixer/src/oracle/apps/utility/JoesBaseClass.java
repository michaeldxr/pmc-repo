package oracle.apps.utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.io.Reader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import oracle.apps.helpers.Mail;
import oracle.apps.helpers.ProcessExecutor;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

abstract public class JoesBaseClass
{
    protected static final String requiredWhiteSpace = "\\s+";
    protected static final String whiteSpace = "\\s*";
    protected static final String identifier = "\\w+";
    protected static final String javaIdentifier = "[A-Za-z0-9_.]+";

    abstract protected void processFile(File fName, boolean bDoADE);

    protected void crawlPeek(File f)
    {
        // override this method to get the chance to peek at each file going by
    }

    abstract protected String getSummaryReport();

    abstract protected String getSummaryReportSubject();

    CRAWL_TYPE m_crawlType;

    protected JoesBaseClass(CRAWL_TYPE type)
    {
        m_crawlType = type;
    }

    protected void setDoADE(boolean m_doADE)
    {
        this.m_doADE = m_doADE;
    }

    protected boolean isDoADE()
    {
        return m_doADE;
    }

    public enum CRAWL_TYPE
    {
        JSPX_AND_JSFF,
        JSPX_AND_JSFF_AND_JAVA,
        JSPX_AND_JSFF_AND_PAGE_DEF_AND_CPX,
        JSPX_AND_JSFF_AND_PAGE_DEF_AND_CPX_AND_VO,
        JAVA,
        ADFC_CONFIG,
        JPR_TASKFLOW,
        TASKMENU,
        PAGE_DEF,
        VO_XML,
        EO_XML,
        ALL_MODEL_AND_VIEW,
        JPR,
        XLF,
        TABLE,
        SEED,
        MESSAGE_SCANNER,
        PLSQL,
        VO_EO_XML,
        JAVA_JPR,
        JAR,
        XLS,
        JWS,
        SQL_FILES,
        ALL_FILES;
    }

    private class StreamEater extends Thread
    {

        InputStream is;
        boolean isDiff;

        public StreamEater(InputStream is)
        {
            this(is, false);
        }

        public StreamEater(InputStream is, boolean isDiff)
        {
            this.is = is;
            this.isDiff = isDiff;
        }

        @Override
        public void run()
        {
            try
            {
                PrintWriter pw = null;

                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                if (isDiff)
                {
                    pw =
  new PrintWriter(new FileOutputStream(new File("diff.txt"), true));
                }
                while ((line = br.readLine()) != null)
                {
                    if (isDiff)
                    {
                        pw.println(line);
                    } else
                        System.out.println(line);
                    if (line.contains("ade ERROR"))
                    {
                        System.exit(-1);
                    }
                }
                if (pw != null)
                {
                    pw.flush();
                    pw.close();
                }
            } catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
    }

    protected void checkoutAndDelete(File f) throws Exception
    {
        ProcessBuilder pb1 =
            new ProcessBuilder("ade", "co", "-nc", f.getAbsolutePath());
        Process process1 = pb1.start();
        System.out.println("ade co -nc " + f.getName());
        StreamEater input1Eater = new StreamEater(process1.getInputStream());
        StreamEater error1Eater = new StreamEater(process1.getErrorStream());
        input1Eater.start();
        error1Eater.start();
        process1.waitFor();

        ProcessBuilder pb2 =
            new ProcessBuilder("rm", "-f", f.getAbsolutePath());
        Process process2 = pb2.start();
        System.out.println("rm -f " + f.getName());
        StreamEater input2Eater = new StreamEater(process2.getInputStream());
        StreamEater error2Eater = new StreamEater(process2.getErrorStream());
        input2Eater.start();
        error2Eater.start();
        process2.waitFor();
    }

    protected void checkinAndDiff(File f) throws Exception
    {
        checkin(f);

        ProcessBuilder pb2 =
            new ProcessBuilder("ade", "diff", "-pred", f.getAbsolutePath());
        Process process2 = pb2.start();
        System.out.println("ade diff -pred " + f.getName() + " >> diff.txt");
        StreamEater input2Eater =
            new StreamEater(process2.getInputStream(), true);
        StreamEater error2Eater =
            new StreamEater(process2.getErrorStream(), true);
        input2Eater.start();
        error2Eater.start();
        process2.waitFor();
    }

    protected void checkin(File f) throws Exception
    {
        ProcessBuilder pb1 =
            new ProcessBuilder("ade", "ci", f.getAbsolutePath());
        Process process1 = pb1.start();
        System.out.println("ade ci " + f.getName());
        StreamEater input1Eater = new StreamEater(process1.getInputStream());
        StreamEater error1Eater = new StreamEater(process1.getErrorStream());
        input1Eater.start();
        error1Eater.start();
        process1.waitFor();
    }

    protected void removeFile(File f) throws Exception
    {
        ProcessBuilder pb =
            new ProcessBuilder("ade", "rmname", f.getAbsolutePath());
        Process process = pb.start();
        System.out.println("ade rmname " + f.getName());
        StreamEater inputEater = new StreamEater(process.getInputStream());
        StreamEater errorEater = new StreamEater(process.getErrorStream());
        inputEater.start();
        errorEater.start();
        process.waitFor();

        checkin(f.getParentFile());
    }

    protected void closeDiffLog() throws Exception
    {
        // No longer needed. Left here only for compatibility.
    }

    protected void startDiffLog() throws Exception
    {
        Process p = Runtime.getRuntime().exec("rm -f diff.txt");
        p.waitFor();
    }
    
    //SU: This method can be muc faster for crawling labels when running in LRG mode
    //Su: viewRoot is actually the labelserver path e.g. /ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/
    //path - e.g. fusionapps, fusionapps/fin, fusionapps/fin/components ..
    protected void crawlDirectoryNew(String viewRoot, String path, boolean bDoADE) {
        
        try{
            
            ProcessExecutor pe = new ProcessExecutor();   
            
            ProcessBuilder pb = new ProcessBuilder("bash","-c","zcat " + viewRoot + 
                ".labellog.emd.gz | grep 'FILE " + path + "' | egrep '" + getExtension() + "'" +
                "| grep -vi '/noship/' | cut -f2 -d ' '");
            pe.runProcess(pb, "/tmp/fileList.txt", false); 
            
            BufferedReader reader = new BufferedReader(new FileReader("/tmp/fileList.txt"));
            String line = "";
            
            while((line = reader.readLine()) != null) {
                if (fileOfInterest(line.trim()))
                    processFile(new File(viewRoot + line.trim()),bDoADE);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        
    }


    protected void crawlDirectory(String path, boolean bDoADE)
    {
        if (path.contains(".ade_path") || path.contains("/classes/"))
            return;

        if(optionallySkipDirectory(path))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++)
        {
            if (listOfFiles[i].isFile())
            {
                String sName = listOfFiles[i].getAbsolutePath();
                // Zeesha: Added, because for files modified recently there is an .ade_path file created in that directory, that is neither node, nor dir. Skip this without raising error
                if (sName.contains(".ade_path"))
                    continue;

                if (fileOfInterest(sName))
                {
                    processFile(listOfFiles[i], bDoADE);
                }

                crawlPeek(listOfFiles[i]);
            } else if (listOfFiles[i].isDirectory())
            {
                // Added by zeesha - no point going into the directory if we are not interested in it. Better performance
                if (isDirOfInterest(listOfFiles[i].getAbsolutePath()))
                    crawlDirectory(listOfFiles[i].getAbsolutePath(), bDoADE);
            } else
            {
                System.out.println("ERROR:  node is neither file or directory: " +
                                   listOfFiles[i]);
            }
        }

    }

  //Added by Yao. This method is used to find jws files more efficiently. No need to go very deep to find jws.
  protected void crawlDirectory(String path, int maxDepth, boolean bDoADE)
  {
      if(maxDepth == 0)
        return;
      
      maxDepth--;
      
      if (path.contains(".ade_path") || path.contains("/classes/"))
          return;

    if(optionallySkipDirectory(path))
        return;

      File folder = new File(path);
//      if(path.contains("/db/data") == false)
//          System.out.println("Analyzing -->  " + path);
      File[] listOfFiles = folder.listFiles();

      if (listOfFiles == null)
          return;

      for (int i = 0; i < listOfFiles.length; i++)
      {
          if (listOfFiles[i].isFile())
          {
              String sName = listOfFiles[i].getAbsolutePath();
              // Zeesha: Added, because for files modified recently there is an .ade_path file created in that directory, that is neither node, nor dir. Skip this without raising error
              if (sName.contains(".ade_path"))
                  continue;

              if (fileOfInterest(sName))
              {
                  processFile(listOfFiles[i], bDoADE);
              }

              crawlPeek(listOfFiles[i]);
          } else if (listOfFiles[i].isDirectory())
          {
              // Added by zeesha - no point going into the directory if we are not interested in it. Better performance
              if (isDirOfInterest(listOfFiles[i].getAbsolutePath()))
                  crawlDirectory(listOfFiles[i].getAbsolutePath(), maxDepth, bDoADE);
          } else
          {
              System.out.println("ERROR:  node is neither file or directory: " +
                                 listOfFiles[i]);
          }
      }

  }
  
  
  public boolean optionallySkipDirectory(String sDirName)
  {
      return false;
  }

    public boolean isDirOfInterest(String sName)
    {
        if (sName.contains(".ade_path") || sName.contains("/classes/"))
            return false;

        if (sName == null || sName.equals(""))
            return false;

        String lowerCaseName = sName.toLowerCase();
        if (lowerCaseName.contains("/noship/") ||
            lowerCaseName.contains("/test/") ||
            lowerCaseName.contains("servicetest") ||
            lowerCaseName.contains("datasecuritytest") ||
            lowerCaseName.contains("securitypublictest") ||
            lowerCaseName.contains("publicuitest") ||
            lowerCaseName.contains("structuretest") ||
            lowerCaseName.contains("modeltest") ||
            lowerCaseName.contains("uitest") ||
            lowerCaseName.contains("testui") ||
            lowerCaseName.contains("setest") ||
            lowerCaseName.contains("testviewcontroller") ||
            lowerCaseName.contains("flextest") ||
            lowerCaseName.contains("uimodeler-launch-test") ||
            lowerCaseName.contains("publicuibasetest") ||
            lowerCaseName.contains("uipickertest") ||
            lowerCaseName.contains("/dist/") ||
            lowerCaseName.contains("/fsm/dev"))
            return false;

        return true;
    }
    
    public String getExtension(){
        switch (m_crawlType){
            case JAVA: 
                return "java";
            case TASKMENU:
                return "taskmenu.xml";
            case JSPX_AND_JSFF_AND_PAGE_DEF_AND_CPX:
                return "cpx|PageDef|jspx|jsff";
            case JSPX_AND_JSFF_AND_PAGE_DEF_AND_CPX_AND_VO:
                return "cpx|PageDef|jspx|jsff|VO.xml";
            case JSPX_AND_JSFF:
                return "jsff|jspx";
            case JSPX_AND_JSFF_AND_JAVA:
                return "jsff|jspx|java";
            case ADFC_CONFIG:
                return "adfc-config.xml|Flow.xml|flow";
            case JPR_TASKFLOW:
                return "jpr|Flow.xml|flow";
            case PAGE_DEF:
                return "PageDef.xml";
            case VO_XML:
                return "VO.xml";
            case EO_XML:
              return "EO.xml";
            case ALL_MODEL_AND_VIEW:
                return "VO.xml|EO.xml|PS.xml|jspx|jsff|taskmenu.xml";
            case JPR:
                return "jpr";
            case XLF:
                return "xlf";
            case TABLE:
                return "table";
            case ALL_FILES:
                return "";
            case MESSAGE_SCANNER:
                return "";
            case SEED:
                return "SD.xml";
            case PLSQL:
                return "pkb";
            case VO_EO_XML:
                return "VO.xml|EO.xml";
            case JAVA_JPR:
                return "java|jpr";
            case JAR:
                return "jar";    
            case JWS:
                return "jws";
            case XLS:
                return "xlsx|xlsm";
            case SQL_FILES:
                return "pkb|view|VO.xml";
            default:
                return "";
        }
    }

    public boolean fileOfInterest(String sName)
    {

        if (sName == null || sName.equals(""))
            return false;
        if (!isDirOfInterest(sName))
            return false;

        switch (m_crawlType)
        {
        case JAVA:
            if (sName.endsWith(".java"))
                return true;
            break;

        case TASKMENU:
            if (sName.endsWith("taskmenu.xml"))
                return true;
            break;

        case JSPX_AND_JSFF_AND_PAGE_DEF_AND_CPX:
            if (sName.endsWith(".cpx"))
                return true;
            if (sName.endsWith("PageDef.xml"))
                return true;
            if (sName.endsWith(".jspx"))
                return true;
            if (sName.endsWith(".jsff"))
                return true;
            if (sName.contains("PageDef"))
                if (sName.matches(".*PageDef[0-9]*\\.xml"))
                    return true;
            break;
        
        case JSPX_AND_JSFF_AND_PAGE_DEF_AND_CPX_AND_VO:
                if (sName.endsWith(".cpx"))
                    return true;
                if (sName.endsWith("PageDef.xml"))
                    return true;
                if (sName.endsWith(".jspx"))
                    return true;
                if (sName.endsWith(".jsff"))
                    return true;
                if (sName.contains("PageDef"))
                    if (sName.matches(".*PageDef[0-9]*\\.xml"))
                        return true;
                if (sName.endsWith("VO.xml") && !sName.contains("/link/") &&
                    !sName.contains("/association/"))
                    return true;
                break;

        case JSPX_AND_JSFF:
            if (sName.endsWith(".jspx"))
                return true;
            if (sName.endsWith(".jsff"))
                return true;
            break;
        case JSPX_AND_JSFF_AND_JAVA:
            if (sName.endsWith(".jspx"))
                return true;
            if (sName.endsWith(".jsff"))
                return true;
            if (sName.endsWith(".java"))
                return true;
            break;

        case ADFC_CONFIG:
            if (sName.endsWith("adfc-config.xml"))
                return true;
            if (sName.endsWith("Flow.xml"))
                return true;
            if (sName.contains("/flow/") && sName.endsWith(".xml"))
                return true;
            break;
        
        case JPR_TASKFLOW:
            if (sName.endsWith(".jpr"))
                return true;
            if (sName.endsWith("Flow.xml"))
                return true;
            if (sName.contains("/flow/") && sName.endsWith(".xml"))
                return true;
            break;

        case PAGE_DEF:
            if (sName.endsWith("PageDef.xml"))
                return true;
            break;

        case VO_XML:
            if (sName.endsWith("VO.xml") && !sName.contains("/link/") &&
                !sName.contains("/association/"))
                return true;
            break;
        case EO_XML:
              if (sName.endsWith("EO.xml"))
                  return true;
              break;
        case ALL_MODEL_AND_VIEW:
            if (sName.endsWith("VO.xml"))
                return true;
            if (sName.endsWith("EO.xml"))
                return true;
            if (sName.endsWith("PS.xml"))
                return true;
            if (sName.endsWith(".jspx"))
                return true;
            if (sName.endsWith(".jsff"))
                return true;
            if (sName.endsWith("taskmenu.xml"))
                return true;
            break;
        case JPR:
            if (sName.endsWith(".jpr"))
                return true;
            break;
        case XLF:
            if (sName.endsWith(".xlf"))
                return true;
            break;
        case TABLE:
            if (sName.endsWith(".table"))
                return true;
            break;
        case ALL_FILES:
            return true;
        case MESSAGE_SCANNER:
            if (sName.endsWith("MessageSD.xml"))
                return true;
            if (sName.endsWith(".pkb") || sName.endsWith(".pkh"))
                return true;
            if (sName.endsWith(".java"))
                return true;
            if(sName.endsWith(".cs"))
                return true;
            if (sName.endsWith(".oc") || sName.endsWith(".lc") || sName.endsWith(".lpc") || sName.endsWith(".opc") || sName.endsWith(".h"))
                return true;
            if (sName.endsWith("PageDef.xml") || sName.endsWith("AM.xml") || sName.endsWith("VO.xml") || sName.endsWith("EO.xml") || sName.endsWith(".jspx") ||sName.endsWith(".jsff") || sName.endsWith("Flow.xml"))
                return true;
            if (sName.endsWith("SD.xml"))
                return true;
            if (sName.endsWith("Bundle.xlf"))
              return true;
            if(sName.contains("/publicFlow/") && sName.endsWith(".xml"))
                return true;
            if(sName.contains("/entity/") && sName.contains("EO") && sName.endsWith(".xml"))
                return true;
            if(sName.contains("/odi/") && sName.endsWith(".xml"))
                return true;
            if (sName.endsWith(".sql") || sName.endsWith(".bpel") || sName.endsWith(".xdm")  || sName.endsWith(".xsl"))
                return true;
            break;
        case SEED:
            if (sName.endsWith("SD.xml"))
                return true;
            break;
        case PLSQL:
            if (sName.endsWith(".pkb"))
                return true;
            break;
        case VO_EO_XML:
            if (sName.endsWith("VO.xml") || sName.endsWith("EO.xml"))
                return true;
            break;
        case JAVA_JPR:
            if (sName.endsWith(".jpr") || sName.endsWith(".java"))
                return true;
            break;
        case JAR:
            if (sName.endsWith(".jar"))
                return true;
            break;
        case JWS:
            if (sName.endsWith(".jws"))
                return true;
            break;
        case XLS:
            if (sName.endsWith(".xlsx") || sName.endsWith(".xlsm")) {
                if(!sName.contains("/src/") && !sName.contains("/adfmsrc/") 
                   && !sName.endsWith("DT.xlsx") && !sName.endsWith("DT.xlsm"))
                return true;
            }
            break;
        case SQL_FILES:
          if(sName.endsWith(".pkb") || sName.endsWith("VO.xml") || sName.endsWith(".view"))
            return true;
          break;
        }
        return false;
    }


    protected String sExceptions = "";
    protected int m_numExceptions = 0;


    protected void logUsageReport(String sCrawlDir)
    {
        logUsageReport(sCrawlDir, false);
    }

    protected void logUsageReport(String sCrawlDir, boolean debugMode)
    {
        String s = "";

        s += "user.dir set to  --->  " + System.getProperty("user.dir") + "\n";
        s += "Crawl Dir set to --->  " + sCrawlDir + "\n";

        s += getSummaryReport();

        System.out.flush();
        System.out.println(s);

        s +=
  "\n\n**EXCEPTION REPORT ************************************************\n";
        if (m_numExceptions == 0)
        {
            s += "No Exceptions to report.\n";
        } else
        {
            s +=
  "A total of " + m_numExceptions + " exception(s) were collected.\n\n";
            s += sExceptions;
        }

        try
        {
            BufferedWriter writer =
                new BufferedWriter(new FileWriter("errors.log"));
            writer.write(s);
            writer.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        //
        //        if(!debugMode)
        //          Mail.sendMail("joseph.albowicz@oracle.com", s, getSummaryReportSubject());
        //        else
        //            System.out.println(s);
    }

    public void logException(String info, Exception e)
    {
        m_numExceptions++;

        sExceptions +=
                "\n@@ Exception # " + m_numExceptions + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n";
        sExceptions += info.toString() + "\n";
        sExceptions += e.toString() + "\n";
        StackTraceElement[] element = e.getStackTrace();
        for (int i = 0; i < element.length; i++)
        {
            sExceptions += element[i].toString() + "\n";
        }
    }

    private String getSingleColumnDigit(int col)
    {
        assert (col >= 0 && col < 26);

        int valueOfA = 65;
        char c = (char)(valueOfA + col);
        return String.valueOf(c);
    }


    private String getColumn(int col)
    {
        int r = col % 26;
        int x = col / 26;

        String retVal = "";

        if (col >= 26)
        {
            retVal = getColumn(x - 1);
        }

        return retVal + getSingleColumnDigit(r);
    }

    protected String getCellLocation(int col, int row)
    {
        return getColumn(col) + row;
    }

    protected String getExactCellLocation(int col, int row)
    {
        return "$" + getColumn(col) + "$" + row;
    }

    private boolean m_doADE = false;

    public static final String newline = "\n";

    protected void doADE(boolean doADE, File f,
                         ArrayList<String> fixedCode) throws Exception
    {
        if (doADE == false)
            return;

        StringBuffer sb = new StringBuffer();
        Iterator<String> iter = fixedCode.iterator();
        while (iter.hasNext())
        {
            sb.append(iter.next());
            sb.append(newline);
        }

        doADE(doADE, f, sb.toString());
    }

    protected void doADE(boolean doADE, File f, ArrayList<String> fixedCode,
                         HashMap<String, String> comments) throws Exception
    {
        if (doADE == false)
            return;

        StringBuffer sb = new StringBuffer();
        Iterator<String> iter = fixedCode.iterator();
        while (iter.hasNext())
        {
            sb.append(iter.next());
            sb.append(newline);
        }

        String s = sb.toString();
        for (Iterator<String> it = comments.keySet().iterator(); it.hasNext();
        )
        {
            String comment1 = it.next();
            s = s.replace(comment1, comments.get(comment1));
        }
        doADE(doADE, f, s);
    }

    protected void doADE(boolean doADE, File f, String s) throws Exception
    {
        if (doADE)
        {
            checkoutAndDelete(f);

            FileWriter fw = new FileWriter(f.getAbsolutePath());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(s);
            bw.close();

            checkinAndDiff(f);
        }
    }

    protected ArrayList<String> getCodeAsStrings(File f) throws Exception
    {
        File fp = new File(f.getAbsolutePath());
        FileReader reader = new FileReader(fp);
        BufferedReader input = new BufferedReader(reader);

        String line;
        ArrayList<String> retVal = new ArrayList<String>();


        while ((line = input.readLine()) != null)
            retVal.add(line);

        return retVal;
    }

    protected void dumpFixedCode(ArrayList<String> fixedCode)
    {
        System.out.println("**** Fixed ************************************************************************************");
        Iterator iter = fixedCode.iterator();
        // int l = 1;
        while (iter.hasNext())
        {
            System.out.println(iter.next());
            //            System.out.println(l++ + ":  " + iter.next());
        }
    }

    protected String removeComment(String str, int start, int stop)
    {
        String r = "";

        for (int i = start; i < stop && i < str.length(); i++)
            r = r + "x";

        str = str.substring(0, start) + r + str.substring(stop);
        return str;
    }

    protected String removeJavaComments(String str)
    {
        // Remove all line comments
        Matcher m1 =
            Pattern.compile("\\s*?//.*$[\r\n]*", Pattern.MULTILINE).matcher(str);
        str = m1.replaceAll("");
        // Remove all other comments
        Matcher m2 =
            Pattern.compile("/\\*.*?\\*/\\s*?[\r\n]*", Pattern.DOTALL).matcher(str);
        str = m2.replaceAll("");

        return str;
    }

    protected String removePlSQLComments(String str)
    {
        // Remove all line comments
        Matcher m1 = Pattern.compile("^\\s*?REM.*$", Pattern.MULTILINE).matcher(str);
        str = m1.replaceAll("");
        
        Matcher m3 =
            Pattern.compile("/\\*.*?\\*/\\s*?[\r\n]*", Pattern.DOTALL).matcher(str);
        str = m3.replaceAll("");
      
        // Remove all line comments
        Matcher m2 = Pattern.compile("--.*$", Pattern.MULTILINE).matcher(str);
        str = m2.replaceAll("");
        // Remove all other comments
        
        return str;
    }

    protected String removeLpcComments(String str)
    {
        // Remove all other comments
        Matcher m2 =
            Pattern.compile("/\\*.*?\\*/\\s*?[\r\n]*", Pattern.DOTALL).matcher(str);
        str = m2.replaceAll("");

        return str;
    }

    protected String removeHTMLComments(String str)
    {
        Matcher m1 =
            Pattern.compile("<!\\-\\-.*?\\-\\->", Pattern.DOTALL).matcher(str);
        str = m1.replaceAll("");
        return str;
    }

    protected String replaceComments(String str)
    {
        //    Matcher m = multiLineCommentPattern.matcher(str);
        //    while(m.find())
        //    {
        //        int s = m.start();
        //        int e = m.end();
        //
        //        String r = "";
        //
        //        for(int i = m.start(); i < m.end() && i < str.length(); i++)
        //            r = r + "x";
        //
        //        str = str.substring(0, s) + r + str.substring(e);
        //    }

        boolean inComment = false;
        int start = -1;

        for (int i = 0; i < str.length() - 1; i++)
        {
            if (inComment == false)
            {
                if (str.charAt(i) == '/' && str.charAt(i + 1) == '*')
                {
                    inComment = true;
                    start = i;
                }
            } else
            {
                if (str.charAt(i) == '*' && str.charAt(i + 1) == '/')
                {
                    inComment = false;
                    str = removeComment(str, start, i + 2);
                }
            }
        }


        Matcher m = lineCommentPattern.matcher(str);

        while (m.find())
        {
            int s = m.start();
            int e = m.end();

            String r = "";

            for (int i = m.start(); i < m.end() && i < str.length(); i++)
                r = r + "x";

            str = str.substring(0, s) + r + str.substring(e);
        }

        return str;
    }

    protected static final String lineCommentRegex =
        whiteSpace + "//[^\n\r]*[\\n]";
    protected static final Pattern lineCommentPattern =
        Pattern.compile(lineCommentRegex, Pattern.MULTILINE);
    protected static final String multiLineCommentRegex =
        "/\\*(?:.|[\n\r])*?\\*/";
    protected static final Pattern multiLineCommentPattern =
        Pattern.compile(multiLineCommentRegex, Pattern.MULTILINE);

    private long start;
    private long end;
    
    public void markStartTime()
    {
        start = System.currentTimeMillis();
    }
    
    public void markEndTime()
    {
        end = System.currentTimeMillis();
        long e = (end - start) / 1000;
        
        System.out.println("Elasped Time is " + e + " seconds.");
    }

    public enum FILE_TYPE
    {
        ENTITY,
        VIEW_OBJECT,
        VIEWLINK,
        APPLICATION_MODULE,
        ASSOCIATION,
        CLASS,
        PAGE_DEF,
        UI_FILE,
        CPX_FILE,
        BC4JXCFG,
        UNKNOWN
    }
    
    public FILE_TYPE getFileType(ZipFile zf, ZipEntry ze)
    {
        try
        {
            if (zf == null || ze == null)
                return FILE_TYPE.UNKNOWN;

            String name = ze.getName();

            if (name.endsWith(".class"))
                return FILE_TYPE.CLASS;

            if (name.endsWith(".jspx") || name.endsWith(".jsff"))
                return FILE_TYPE.UI_FILE;
            
            if(name.endsWith(".cpx"))
                return FILE_TYPE.CPX_FILE;

            if(name.endsWith("bc4j.xcfg"))
                return FILE_TYPE.BC4JXCFG;

            if (name.endsWith("xml"))
            {
                XMLDocument doc = XMLParserHelper.getXMLDocument(zf.getInputStream(ze));
                Node root = doc.getDocumentElement();
                String rname = root.getNodeName();
                
                
                if("pageDefinition".equals(rname))
                    return FILE_TYPE.PAGE_DEF;
                if("ViewObject".equals(rname))
                    return FILE_TYPE.VIEW_OBJECT;
                if("Entity".equals(rname))
                    return FILE_TYPE.ENTITY;
                if("AppModule".equals(rname))
                    return FILE_TYPE.APPLICATION_MODULE;
                if("ViewLink".equals(rname))
                    return FILE_TYPE.VIEWLINK;
                if("Association".equals(rname))
                    return FILE_TYPE.VIEWLINK;

                return FILE_TYPE.UNKNOWN;
            }
            
            else
            {
//                System.out.println("****************************************");
//                System.out.println("File is " + name);
//                System.out.println("****************************************");
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return FILE_TYPE.UNKNOWN;
    }
    
    public String readFileToStringAlternate(String sPath)
    {
        try
        {
            FileInputStream fis = new FileInputStream(sPath);
            Reader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            char[]buffer = new char[8192];
            int read;
            while((read = reader.read(buffer, 0, buffer.length)) > 0)
            {
                sb.append(buffer, 0, read);
            }
            
            return sb.toString();
        }
        catch(Exception e)
        {
            return null;
        }

    }
    
    public String readFileToString(String sPath)
    {
        String retval = null;
        Scanner scanner = null;
        
        try
        {
            scanner = new Scanner(new File(sPath));
            scanner.useDelimiter("\\A");
            retval = scanner.next();
            scanner.close();
            scanner = null;
        }
        catch (java.util.NoSuchElementException e)
        {
            retval = readFileToStringAlternate(sPath);
            
            if(retval == null)
            {
                System.out.println("ERROR: readFileToString returned null for file:  " + sPath);
            }

            return retval;
//            return retval;
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if(scanner != null)
            {
                scanner.close();
                scanner = null;
            }
        }
        
        return retval;
    }
}
