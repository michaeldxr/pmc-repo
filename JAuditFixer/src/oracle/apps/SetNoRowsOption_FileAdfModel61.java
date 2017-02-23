package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.File;
import java.io.FileReader;

import java.io.FileWriter;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Date;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLAttr;
import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SetNoRowsOption_FileAdfModel61 
{
    /**
     * Writer for diffWriter.txt
     */
    private BufferedWriter diffWriter;

    /**
     * Number of VO files looked at
     */
    static int m_voFilesLookedAt = 0;
    static int m_numParameterVOs = 0;
    static int m_numParameterVOsWithMaxFetchZero = 0;
    static int m_numParameterVOsWithCustomQueryFalse = 0;

    static int m_numFixed = 0;

    /**
     * StringBuilder for any info messages that need to be shown after the run
     */
    private StringBuilder sbInfoMessages = new StringBuilder();

    /**
     * Name of diffWriter file
     */
    final static String diffFileName = "diff.txt";


    public SetNoRowsOption_FileAdfModel61() {
        super();
    }

    /**
     * Usage: <path to the code>
     * @param args
     * args[0] = path to the code
     */
    public static void main(String[] args) {
        /* Usage: setNoRowsOption_FileAdfModel61 <path to code>*/
        Date startDate = new Date();

        if (args.length != 1 || isEmpty(args[0]))
        {
            System.out.println("Usage:  setNoRowsOption_FileAdfModel61.sh <path to your code in $ADE_VIEWROOT/fusionapps/etc etc/>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        Boolean bDoADE = Boolean.TRUE;
        System.out.println("----------Input Parameters --------------");
        System.out.println("Analyzing Directory " + sCrawlDir);
        System.out.println("------------------------------------------");

        SetNoRowsOption_FileAdfModel61 setVOTuning = new SetNoRowsOption_FileAdfModel61();


        try 
        {
            if(bDoADE.booleanValue())
                setVOTuning.startDiffLog();
            
            /**
             * Recursevely scan the directory - Fix any vo file that needs fixing
             */
            System.out.println("====================Analyzing and fixing VO files  ===================== ");
            setVOTuning.crawlDirectory(sCrawlDir, bDoADE);
            System.out.println("======================================================================= ");

            if(bDoADE.booleanValue())
                setVOTuning.closeDiffLog();


        } catch (Exception ex) {
            System.out.println("[Error] Exception while analyzing VO Files");
            ex.printStackTrace();
        }
        String strInfoMessages = setVOTuning.sbInfoMessages.toString();
        if (!isEmpty(strInfoMessages)) {
            System.out.println("[ATTENTION] Please review the below.. ");
            System.out.println(strInfoMessages);
        }
        printStatistics(startDate);

    }


    public static void printStatistics(Date startDate) {
        // Print Statistics
        Date endDate = new Date();
        System.out.println("=================================================");
        System.out.println("Run Statistics");
        System.out.println("Start Time: " + startDate.toString());
        System.out.println("End Time: " + endDate.toString());
        long elapsedTime = endDate.getTime() - startDate.getTime();
        System.out.println("Elapsed Time (sec): " + elapsedTime / 1000);
        System.out.println("=================================================");

        System.out.println("=================================================");
        System.out.println("Total Number of vo files looked at: " +m_voFilesLookedAt);
        System.out.println("Number of Parameter VOs = " +m_numParameterVOs);
        System.out.println("Number of Parameter VOs fixed = " +m_numFixed);
        System.out.println("Number of Parameter VOs with maxFechSize=0 = " +m_numParameterVOsWithMaxFetchZero);
//        System.out.println("Number of Parameter VOs with custom query = false = " +m_numParameterVOsWithCustomQueryFalse);

        System.out.println("=================================================");

    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    /**
     * Format the absolute file path by the occurrence of /fusionapps/
     * e.g. /scratch/zcurrimb/view_storage/zcurrimb_m6b_latest/fusionapps/fin/components/abc.txt
     * will become
     * /fusionapps/fin/components/abc.txt
     * @param absFilePath
     * Absolute file path
     * @return
     * formatted file path
     */
    private String getFusionappsIndexedPath(String absFilePath) {
        if (isEmpty(absFilePath)) {
            return absFilePath;
        }
        if (absFilePath.contains("/fin/")) {
            int finIndex = absFilePath.indexOf("/fin/");
            return absFilePath.substring(finIndex + 1);
        }
        return absFilePath;
    }


    /**
     * recurse over the crawl directory and for each valid VO file - process it.
     * Valid directories -
     * - should not contain, /dbSchema/ or /test/ or /classes/
     * Valid VO files
     * Must be named with "VO.xml".  
     * - and not in folder with /dbSchema/ or /test/ or /classes/
     * @param path
     * @param bDoADE
     */
    private void crawlDirectory(String path, Boolean bDoADE) 
    {
        if (path.contains(".ade_path") || 
            path.contains("/link/") || 
            path.endsWith("/link") || 
            path.contains("/test/") || 
            path.contains("/adfmsrc/") || 
            path.contains("/modelTest/") || 
            path.endsWith("/test") || 
            path.endsWith("/entity/association") || 
            path.endsWith("/viewLink") || 
            path.contains("/dbSchema/") || 
            path.contains("/classes/"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) 
        {
            if (listOfFiles[i].isFile()) 
            {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                String formattedPath = getFusionappsIndexedPath(absFilePath);
                
                if(formattedPath.endsWith("VO.xml"))
                {
                    m_voFilesLookedAt++;
                    processVOFile(listOfFiles[i], bDoADE);
                }
            } 
            else if (listOfFiles[i].isDirectory()) 
            {
                String dir = listOfFiles[i].getAbsolutePath();
                crawlDirectory(dir, bDoADE);
            } 
            else 
            {
                sbInfoMessages.append("***************************************************************************\n");
                sbInfoMessages.append("ERROR:  node is neither file or directory\n");
                sbInfoMessages.append(path +"\n");
                sbInfoMessages.append("***************************************************************************\n");
            }
        }
    }

    private static final String whiteSpace = "[\\s]*";
    private static final String identifier = "[\\-'0-9a-zA-Z_]*";
    private static final String asClause = "[\\s]*(as)?";
    private static final String optParams = "(" +whiteSpace +"\\(" +whiteSpace +identifier +whiteSpace +"\\)" +whiteSpace +")?";
    
    private static final String sStandardDualQuery = whiteSpace + "select" + "("+whiteSpace +identifier +optParams +asClause +whiteSpace +identifier +whiteSpace+ ",)*" + whiteSpace +identifier +optParams +asClause +whiteSpace +identifier +whiteSpace+"from" +whiteSpace +"dual"+whiteSpace +"(" +identifier +")?" +whiteSpace;

    Pattern standardDualQuery = Pattern.compile(sStandardDualQuery, Pattern.CASE_INSENSITIVE);

    Pattern fromPattern         = Pattern.compile("\\sfrom[\\s]*[a-zA-Z0-9_]*", Pattern.CASE_INSENSITIVE);
    Pattern fromWithDualPattern = Pattern.compile("\\sfrom[\\s]*dual", Pattern.CASE_INSENSITIVE);
    
    public void processVOFile(File file, Boolean bDoADE) 
    {
        try{
        DOMParser parser = null;
        String absFilePath = file.getAbsolutePath();

        int fixLocation = -1;
        boolean isEssParameterVO = false;

        String lower = absFilePath.toLowerCase();
        if(lower.contains("paramsvo")  || lower.contains("paramvo")  || lower.contains("parametersvo")  || lower.contains("parametervo") ||
           lower.contains("paramspvo") || lower.contains("parampvo") || lower.contains("parameterspvo") || lower.contains("parameterpvo"))
        {
            isEssParameterVO = true;
        }

        if(isEssParameterVO == false)
            return;

        m_numParameterVOs++;

        try 
        {
            parser = initializeDomParser();
            parser.parse("file:" + absFilePath);
        } 
        catch (Exception ex) 
        {
            sbInfoMessages.append("***************************************************************************\n");
            sbInfoMessages.append("Error with File: " + file.getAbsolutePath() +"\n");
            sbInfoMessages.append(ex.getMessage());
            sbInfoMessages.append("***************************************************************************\n");
            return;
        }

        XMLDocument doc = parser.getDocument();
        NodeList voTag = doc.getElementsByTagName("ViewObject");

        if (voTag == null || voTag.getLength() != 1) 
        {
            sbInfoMessages.append("***************************************************************************\n");
            sbInfoMessages.append("Error with File: " + absFilePath +"\n");
            sbInfoMessages.append("File not processed because exactely 1 ViewObject tag is expected in a file defining a View Object\n");
            sbInfoMessages.append("***************************************************************************\n");
            return;
        }
        
        Node voNode = voTag.item(0);
        NamedNodeMap voAttr = voNode.getAttributes();

        Node nameAttr = voAttr.getNamedItem("Name");
        fixLocation = ((XMLAttr)nameAttr).getLineNumber();

        Node customQueryNode = voAttr.getNamedItem("CustomQuery");
        boolean isCustomQuery = false;
        if(customQueryNode != null && "true".equals(customQueryNode.getNodeValue()))
            isCustomQuery = true;

        boolean maxFetchSizeZero = false;
        boolean hasMaxFetchSize = false;
        
        Node maxFetchSizeClassNode = voAttr.getNamedItem("MaxFetchSize");
        if(maxFetchSizeClassNode != null && "0".equals(maxFetchSizeClassNode.getNodeValue()) == true)
        {
            maxFetchSizeZero = true;
        }

        if(maxFetchSizeZero)
        {
            m_numParameterVOsWithMaxFetchZero++;
            return;
        }
        
        if(isCustomQuery == false)
        {
            m_numParameterVOsWithCustomQueryFalse++;
            return;
        }

        boolean hasSqlQuery = false;
        boolean hasDummyQuery = false;
        boolean isDual = false;
        boolean emptyCustomQuery = false;


        String sqlText = "";

        NodeList sqlNodeList = doc.getElementsByTagName("SQLQuery");
        if(sqlNodeList != null && sqlNodeList.getLength() > 0)
        {
            Node sqlNode = sqlNodeList.item(0);
            
            if(sqlNode != null)
            {
                hasSqlQuery = true;

                sqlText = sqlNode.getTextContent();
                
                if(sqlText.toLowerCase().contains("union") == false)
                {
             
                    Matcher fromMatcher = fromPattern.matcher(sqlText);
                    Matcher fromDualMatcher = fromWithDualPattern.matcher(sqlText);
                    
                    int numFroms = 0;
                    int numFromsWithDual = 0;
                    
                    while(fromMatcher.find())
                        numFroms++;
                    while(fromDualMatcher.find())
                        numFromsWithDual++;

                    if(numFroms == numFromsWithDual)    
                    {
                        hasDummyQuery = true;
                    }

                    if(hasDummyQuery)
                    {
                        isDual = false;
                        Matcher stdDualQuery = standardDualQuery.matcher(sqlText);
                        if(stdDualQuery.find())
                        {
                            int start = stdDualQuery.start();
                            int end = stdDualQuery.end();
                            int length = sqlText.length();
                            int l2 = end -start;
                            String str = sqlText.substring(start, end);
                            
                            if(l2 == length)
                            {
                                //System.out.println("have a full match");
                                isDual = true;
                            }
                        }
                    }
                }
            }
        }

        if(sqlText == null || "".equals(sqlText))
            emptyCustomQuery = true;

        if(hasDummyQuery != isDual)
        {
            sbInfoMessages.append("********************************************************************\n");
            sbInfoMessages.append("Error:  VO has an select <something> from dual that cannot be processed.  Please fix manually.\n");
            sbInfoMessages.append("File:  " +file.getAbsolutePath() +"\n");
            sbInfoMessages.append("SQLText is:  " + sqlText +"\n");
            sbInfoMessages.append("********************************************************************\n");
            return;
        }
        else if(isDual && (sqlText.contains("(") || sqlText.contains("'")) )
        {
            sbInfoMessages.append("********************************************************************\n");
            sbInfoMessages.append("Warning:  VO has a simple select <something> from dual.  The VO has been processed and fix is applied.\n");
            sbInfoMessages.append("The SQL Text may have default value logic that needs to be moved to the attributes.  Please fix the default value logic manually.\n");
            sbInfoMessages.append("File:  " +file.getAbsolutePath() +"\n");
            sbInfoMessages.append("SQLText is:  " + sqlText +"\n");
            sbInfoMessages.append("********************************************************************\n");
        }


        if(isDual && isCustomQuery)
        {
            if(maxFetchSizeClassNode != null && "".equals(maxFetchSizeClassNode.getNodeValue()) == false)
            {
                hasMaxFetchSize = true;
                sbInfoMessages.append("********************************************************************\n");
                sbInfoMessages.append("Warning:  VO has a MaxFetchSize already set.  The MaxFetchSize has been changed to 1.  Please review.\n");
                sbInfoMessages.append("File:  " +file.getAbsolutePath() +"\n");
                sbInfoMessages.append("MaxFetchSize is --> " + maxFetchSizeClassNode.getNodeValue() +"\n");
                sbInfoMessages.append("********************************************************************\n");
            }
            

            m_numFixed++;
            System.out.println("Applying fix to file:" +file.getAbsolutePath());
            applyFix(file, hasMaxFetchSize, fixLocation);
        }
        else if(emptyCustomQuery && isCustomQuery)
        {
//            m_numFixed++;
//            System.out.println("Need to fix:" +file.getAbsolutePath() +":" +file.getName() +":" +isEssParameterVO);
        }
        else
        {
//            System.out.println("********************************");
//            System.out.println("isCustomQuery ---> " +isCustomQuery);
        }
        }
        catch (Exception ex) 
        {
            ex.printStackTrace();
            System.exit(-1);
            
            sbInfoMessages.append("***************************************************************************\n");
            sbInfoMessages.append("Error with File: " + file.getAbsolutePath() +"\n");
            sbInfoMessages.append(ex.getMessage());
            sbInfoMessages.append("***************************************************************************\n");
            return;
        }
    }



    /**
     * Initializes the Dom parser to parse jspx and jsff files
     * @return
     * Dom parser
     * @throws Exception
     */
    private DOMParser initializeDomParser() throws Exception {

        DOMParser parser = new DOMParser();
        parser.setDebugMode(true);
        parser.setErrorStream(System.out);
        parser.setEntityResolver(new NullEntityResolver());
        parser.showWarnings(true);
        parser.setValidationMode(DOMParser.NONVALIDATING);

        return parser;
    }


    private void applyFix(File f, boolean hasMaxFetchSize, int location) throws Exception
    {
        String mfs = "MaxFetchSize=\"";

        File fp = new File(f.getAbsolutePath());
        FileReader reader = new FileReader( fp );
        BufferedReader input = new BufferedReader( reader );

        FileReader reader2 = new FileReader( fp );
        BufferedReader input2 = new BufferedReader( reader2 );

        String line;
        String line2;

        StringBuffer sb = new StringBuffer();

        int lineNo = 1;
        input.mark(1024);

        while( (line = input.readLine()) != null )
        {
            input2.skip(line.length());
            input2.mark(1024);
            int c1 = input2.read();
            int c2 = input2.read();
            input2.reset();

            String newline = "\n";
            if(c1 == -1)
            {
                newline = "";
            }
            else if(c1 == '\n')
            {
                input2.read();
            }
            else if(c1 == '\r' && c2 != '\n')
            {
                newline = "\r";
                input2.read();
            }
            else if(c1 == '\r' && c2 == '\n')
            {
                newline = "\r\n";
                input2.read();
                input2.read();
            }
            
            if(hasMaxFetchSize && line.contains(mfs))
            {
                location = lineNo;
            }

            if(hasMaxFetchSize && lineNo == location)
            {
                // need to update an existing one.
                int index = line.indexOf(mfs) +mfs.length();
                int end = line.indexOf('"', index);
                
                String s = line.substring(0, index) +"0" +line.substring(end);
                System.out.println("substituting ---> " +s);
                line = s;
                sb.append(line +newline);
            }
            else if(lineNo == location)
            {
                line = fixLine(line, newline);
                sb.append(line +newline);
                // insert MaxFetchSize = 0
            }
//            if(inList(lineNumberList, lineNo))
//            {
//                line = fixLine(line, newline);
//                sb.append(line +newline);
//            }
            else
            {
                sb.append(line);
                sb.append(newline);
            }

            lineNo++;
        }

        reader.close();

        checkoutAndDelete(f);

        FileWriter fw = new FileWriter(f.getAbsolutePath());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(sb.toString());
        bw.close();

        checkinAndDiff(f);
    }


    private void checkinAndDiff(File f) throws Exception
    {
        Process ciProcess = Runtime.getRuntime().exec("ade ci " +f.getAbsolutePath());
        checkProcess(ciProcess, f, "checkin", false);
        
        String diffCmd = "ade diff -label " +f.getAbsolutePath();
        Process diffProcess = Runtime.getRuntime().exec(diffCmd);
        checkProcess(diffProcess, f, "diff file with previous in label", true);
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
            diffWriter.write(str);
    }

    private boolean appearsMoreThanOnce(String str, String pattern)
    {
        int index = str.indexOf(pattern);
        if(index == -1)
            return false;
        
        String str2 = str.substring(index +pattern.length());
        index = str2.indexOf(pattern);
        if(index == -1)
            return false;
        
        return true;
    }

    private String fixLine(String line, String newline)
    {
        // First do some sanity tests to make sure we are dealing with good input.
        if(appearsMoreThanOnce(line, "<") ||
           appearsMoreThanOnce(line, ">"))
        {
            System.out.println("ERROR:  skipping this line which is formatted poorly");
            System.out.println(line);
            return line;
        }

        String retVal = null;

        boolean singleLine = line.indexOf("<ViewObject") != -1;
        
        int i = line.indexOf("/>");

        if(singleLine)
        {
            if(i != -1)
            {
                retVal = line.substring(0, i);
                retVal += " MaxFetchSize=\"0\"/>";
                return retVal;
            }
    
            i = line.indexOf(">");
            retVal = line.substring(0, i);
            retVal += " MaxFetchSize=\"0\">";
            return retVal;
        }

        String leadingWhitespace = newline +line.substring(0, line.indexOf(line.trim()));

        if(i != -1)
        {
            retVal = line.substring(0, i);
            retVal += leadingWhitespace;
            retVal += "MaxFetchSize=\"0\"/>";
            return retVal;
        }
        
        i = line.indexOf(">");
        retVal = line.substring(0, i);
        retVal += leadingWhitespace;
        retVal += "MaxFetchSize=\"0\">";
        return retVal;
    }


    private void closeDiffLog() throws Exception
    {
        diffWriter.close();
    }
    private void startDiffLog() throws Exception
    {
        Process p = Runtime.getRuntime().exec("rm -f diff.txt");
        checkProcess(p, null, "remove diff log", false);

        diffWriter = new BufferedWriter(new FileWriter("diff.txt"));
    }

}
