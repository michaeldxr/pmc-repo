package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.StringReader;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashMap;

import java.util.Iterator;
import java.util.Set;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CheckSQLGSCCErrors {
    public CheckSQLGSCCErrors() {
        super();
    }
    /**
     * Number of SQL type  files
     */
    static int m_serverOutPut = 0;
    static int m_ShowErrors = 0;
    StringBuffer nSb=new StringBuffer(0);
    private BufferedWriter diff;
    public static void main(String[] args) {

        // Make sure it is run with correct number of arguments
        if(args.length != 3 || args[0] == null || args[0].equals("") || args[1] == null || args[1].equals("")|| args[2] == null || args[2].equals("")){
            System.out.println("Usage:  CheckSqlGSCCErrors.sh <path to your code in an open ade view> <Do ADE checkin/checkout> <Error Type(SHOWERROR OR SERVEROUTPUT)>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        Boolean bDoADE = new Boolean(args[1]);
        String errorTypeTofix = args[2];
        
        if (isEmpty(sCrawlDir)) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }
        System.out.println("Analyzing all files in directory:" + sCrawlDir);
        
        CheckSQLGSCCErrors CheckSqlGSCCErrors = new CheckSQLGSCCErrors();
        try {
            if(bDoADE.booleanValue()) 
             CheckSqlGSCCErrors.startDiffLog();


            CheckSqlGSCCErrors.crawlDirectory(sCrawlDir,bDoADE,errorTypeTofix);
            if(bDoADE.booleanValue())
             CheckSqlGSCCErrors.closeDiffLog();

        } catch (Exception ex) {
            System.out.println("Exception occurred: " + ex.getMessage());
        }
        
        System.out.println("Total files with set serveroutput on " +m_serverOutPut);
        System.out.println("Total files with show errors " + m_ShowErrors);
        if(bDoADE.booleanValue())
         System.out.println("*** REVIEW diff.txt FOR CORRECTNESS *** " );           
    }
    private void crawlDirectory(String crawlDir,Boolean bDoADE,String errorTypeTofix) {
            if (crawlDir.contains(".ade_path"))
                return;

            File folder = new File(crawlDir);
            File[] listOfFiles = folder.listFiles();

            if (listOfFiles == null)
                return;

            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {

                    String absFilePath = listOfFiles[i].getAbsolutePath();
                    if (!isEmpty(absFilePath) &&( absFilePath.endsWith(".pkb")||absFilePath.endsWith(".pkh") )) {
                       
                     
                        processSQLFile(listOfFiles[i],bDoADE,errorTypeTofix);
                    }

                } else if (listOfFiles[i].isDirectory()) {
                    String dir = listOfFiles[i].getAbsolutePath();
                    if (isValidDirectory(dir))
                    crawlDirectory(dir,bDoADE,errorTypeTofix);
                   

                } else {
                    System.out.println("ERROR:  node is neither file or directory");
                }
            }
    }
    public void processSQLFile(File file, Boolean bDoADE,String errorTypeTofix) {
        String absPath = file.getAbsolutePath();
       // System.out.println("Processing file: " + absPath);

        XmlFixer fileFixer = null;
        String fileContents = "";
        try {
            fileFixer = new XmlFixer(absPath);
            String[] fileContentsArray = fileFixer.convertFileToStringArray();
            fileContents =
                    fileFixer.getFileContentsAsString(fileContentsArray, false);
    //            fileContents = removeSingleLineComments(fileContents);
    //            fileContents = removeMultilineComments(fileContents);
            if (!isEmpty(fileContents)) {

                findViolationsInSQLFile(file, fileContents,bDoADE,errorTypeTofix);
            } else {
                
                    System.out.println("File contents were empty.. returning.. "+absPath);
               
            }
        } catch (Exception ex) {
            System.out.println("Exception in file: Skipping..  " +
                               absPath + " Exception: " + ex.getMessage());
            return;
        }


    }
    
    public void findViolationsInSQLFile(File file,
                                        String fileContentsWithoutComments,Boolean bDoADE,String errorTypeTofix) throws Exception {
        String fileName = file.getName();
        String filePath = file.getAbsolutePath();
        StringBuffer sb = new StringBuffer();
        boolean ruleViolated = false;
        int lineNum = 0;
        BufferedReader fileReader =
            new BufferedReader(new StringReader(fileContentsWithoutComments));
        if (fileReader == null)
            return;
        String line = null;
       
        BufferedReader fileReader2 = new BufferedReader(new StringReader(fileContentsWithoutComments) );
        fileReader.mark(1024);
        try {
            while ((line = fileReader.readLine()) != null) {
              lineNum++;
                fileReader2.skip(line.length());
                fileReader2.mark(1024);
                int c1 = fileReader2.read();
                int c2 = fileReader2.read();
                fileReader2.reset();

                String newLine = "\n";
                
                if(c1 == -1)
                {
                    newLine = "";
                }
                else if(c1 == '\n')
                {
                    fileReader2.read();
                }
                else if(c1 == '\r' && c2 != '\n')
                {
                    newLine = "\r";
                    fileReader2.read();
                }
                else if(c1 == '\r' && c2 == '\n')
                {
                    newLine = "\r\n";
                    fileReader2.read();
                    fileReader2.read();
                }
                // dont waste time on blank lines or commented line
                if (isEmpty(line)) {
                    sb.append(line+newLine);
                    continue;
                }
               
                if (isCommented(line)) {
                    sb.append(line+newLine);
                    continue;
                }
                if (errorTypeTofix.equals("SERVEROUTPUT")) {
                    if (containsSetServerOutputOn(line)) {
                        String fixedServerOutLine = fixError(line);
                        sb.append(fixedServerOutLine + newLine);
                        m_serverOutPut++;
                        ruleViolated = true;
                        System.out.println("Server Output On at Line " +
                                           lineNum + " in file " + filePath);
                        continue;
                    }
                }
                if (errorTypeTofix.equals("SHOWERROR")) {
                    if (containsShowErrors(line)) {
                        String fixedShowErrorLine = fixError(line);
                        sb.append(fixedShowErrorLine + newLine);
                        m_ShowErrors++;
                        ruleViolated = true;
                        System.out.println("Show errors at Line " + lineNum +
                                           " in file " + filePath);
                        continue;
                    }
                }
                sb.append(line+newLine);
            }

        } catch(Exception e){}
        fileReader.close();
        
        if (ruleViolated && bDoADE) {
            checkoutAndDelete(file);

            FileWriter fw = new FileWriter(file.getAbsolutePath());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(sb.toString());
            bw.close();

            checkinAndDiff(file);
        }
    }
    public boolean containsSetServerOutputOn(String str) {
        try {
            if (!isEmpty(str) &&
                ((str.toUpperCase().contains("SET SERVEROUTPUT ON") &&str.toUpperCase().trim().substring(0,19).equals("SET SERVEROUTPUT ON")) ||
                 (str.toUpperCase().contains("SET SERVEROUT ON") &&str.toUpperCase().trim().substring(0,16).equals("SET SERVEROUT ON")))) {
                return true;
            }
            return false;
        }catch (Exception e) {
                return false;
            }
        }

    public boolean containsShowErrors(String str) {
        try {
      
        if (!isEmpty(str) &&
            ((str.toUpperCase().contains("SHOW ERRORS")&&str.toUpperCase().trim().substring(0,11).equals("SHOW ERRORS")) ||
            (str.toUpperCase().contains("SHO ERRORS")&&str.toUpperCase().trim().substring(0,10).equals("SHO ERRORS")) ||
             (str.toUpperCase().contains("SHO ERR")&&str.toUpperCase().trim().substring(0,7).equals("SHO ERR")) ||
             (str.toUpperCase().contains("SHOW ERR")&&str.toUpperCase().trim().substring(0,8).equals("SHOW ERR")))){

            return true;
             }
        return false;
        } catch(Exception e)
        {
            return false;
            }
      
    }
    private String fixError(String linetofix){
        String comment = "REM ";
        return comment+linetofix;
    }
    
    private void checkoutAndDelete(File f) throws Exception
    {
        Process coProcess = Runtime.getRuntime().exec("ade co -nc " +f.getAbsolutePath());
        checkProcess(coProcess, f, "checkout", false);
        
        Process rmProcess = Runtime.getRuntime().exec("rm -f " +f.getAbsolutePath());
        checkProcess(rmProcess, f, "remove old file", false);
    }
    private void checkProcess(Process p, File f, String description, boolean isDiff) throws Exception
    {
        int exitCode = p.waitFor();
        if(!isDiff && exitCode != 0)
        {
            if(f != null)
                System.out.println("There was an error when trying to " +description+ " for file : " +f.getAbsolutePath());
            else
                System.out.println("There was an error when trying to " +description);
        }

        dumpInputStream(p.getInputStream(), isDiff);
        dumpInputStream(p.getErrorStream(), isDiff);
    }
    
    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }
    private void checkinAndDiff(File f) throws Exception
    {
        Process ciProcess = Runtime.getRuntime().exec("ade ci " +f.getAbsolutePath());
        checkProcess(ciProcess, f, "checkin", false);
        
        String diffCmd = "ade diff -label " +f.getAbsolutePath();
        Process diffProcess = Runtime.getRuntime().exec(diffCmd);
        checkProcess(diffProcess, f, "diff file with previous in label", true);
    }
    
        public boolean isValidDirectory(String absDirPath) {
            if (!absDirPath.contains("/dbSchema/") &&
                !absDirPath.contains("/classes/") &&
                !absDirPath.contains("/noship/") &&
                !absDirPath.contains("/test/")) {
                return true;
            }
            return false;
        }
        
   
    private void dumpInputStream(InputStream in, boolean isDiff) throws Exception
    {
        int n = in.available();
        byte b[] = new byte[n];
        in.read(b, 0, n);
        String str = new String(b);
        if(str.contains("ade ERROR"))
        {
            System.out.println(str);
            System.out.println("There was an error interacting with ADE, exiting script.");
            System.exit(-1);
        }

        if(isDiff == false)
            System.out.println(str.trim());
        else
            diff.write(str);
    }
    private void startDiffLog() throws Exception
    {
        Process p = Runtime.getRuntime().exec("rm -f diff.txt");
        checkProcess(p, null, "remove diff log", false);

        diff = new BufferedWriter(new FileWriter("diff.txt"));
    }
    private void closeDiffLog() throws Exception
    {
        diff.close();
    }
    private boolean isCommented(String line){
        if (line.equals("/"))
            return false;
       
            
        int counter=0;
        char c='/';
        StringBuffer sb2 = new StringBuffer();
        StringBuffer sb3 = new StringBuffer();
        for (int i=0;i<=line.length();i++) {
            if (counter ==3)
                break;
            try {
            
            c = line.charAt(i);
            }catch(Exception e){
               
            }
          
            if (c == ' '){
                continue;
            } else {
                 if (counter < 2){
                
                    sb2.append(c);
                    sb3.append(c);
                } else {
                  
                    sb3.append(c); 
                }
                counter++;
                
            }
        }
        counter=0;
        String s2 = null;
        String s3=null;
        if (sb2 != null)
            s2 = sb2.toString();
        if (sb3 != null)
            s3 = sb3.toString();
       
        Pattern p  = Pattern.compile("REM");
        Pattern p1 =  Pattern.compile("--");
        Matcher m = p.matcher(s3);
        Matcher m1 = p1.matcher(s2);
        
        if (m.matches() || m1.matches())
            return true;
        return false;
                    
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
            //System.out.println("start: "+ start);
            // System.out.println("end: "+ end);
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
}
