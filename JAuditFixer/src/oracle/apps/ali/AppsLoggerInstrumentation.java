package oracle.apps.ali;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;

import java.util.regex.Pattern;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.JavaFileParserHelper;
import oracle.apps.helpers.Mail;

public class AppsLoggerInstrumentation {
    static boolean bInstrumentAm = true;

    static boolean bStartBreadCrumb = true;
    static boolean bEndBreadCrumb = false;
    static boolean bLogPrintStackTrace = true;
    static boolean bOverrideExistingBC = true;
    static boolean m_overrideExistingPsts = true;
    static boolean bDebug = false;

    static int m_numberOfFilesInstrumented = 0;
    static int m_numberOfMethodsInstrumented = 0;
    static int m_numberOfMethods = 0;
    static int m_numberOfPSEFound = 0;
    static int m_numberOfPSEInstrumented = 0;
    static String m_javaFiles = "all";
    static String m_diffFileName = "diff.txt";
    AdeDiffHelper diffHelper = new AdeDiffHelper(m_diffFileName);
    static String HAVE_BEEN_HERE_COMMENT = "[AppsLogStandardInstrumentation]";
    static String PST_HAVE_BEEN_HERE_COMMENT = "[PrintStackTraceALIStandard]";
    static String PST_HAVE_BEEN_HERE_REGEX = "//\\[PrintStackTraceALIStandard\\]";
    static String ALI_SUPRESS_LINE = "@ALI:Supress";
    static String m_pstLogLevel = "FINER";
    static ArrayList<String> exceptions = new ArrayList<String>();

    public AppsLoggerInstrumentation() {
        super();
    }

    /**
     * System properties support
     * -DstartBC=true/false
     * -DendBC=true/false
     * -DinstrumentPST=true/false
     * -Ddebug=true/false
     * -DoverrideExistingBC=true/false
     * -DoverrideExistingPstsIfReqd=true/false
     * -DjavaFiles=all/standard
     * -DpstLogLevel=SEVERE/FINE/FINER/FINEST
     */
    public static String checkSystemProperties() {
        StringBuffer sysPropsString = new StringBuffer();
        String startBc = System.getProperty("startBC");
        if (!isEmpty(startBc)) {
            if (startBc.equalsIgnoreCase("true"))
                bStartBreadCrumb = true;
            else
                bStartBreadCrumb = false;
        }
        sysPropsString.append(String.format(" startBC=%s ", bStartBreadCrumb));

        String endBc = System.getProperty("endBC");
        if (!isEmpty(endBc)) {
            if (endBc.equalsIgnoreCase("true"))
                bEndBreadCrumb = true;
            else
                bEndBreadCrumb = false;
        }
        sysPropsString.append(String.format(" endBC=%s ", bEndBreadCrumb));

        String instrumentPST = System.getProperty("instrumentPST");
        if (!isEmpty(instrumentPST)) {
            if (instrumentPST.equalsIgnoreCase("true"))
                bLogPrintStackTrace = true;
            else
                bLogPrintStackTrace = false;
        }
        sysPropsString.append(String.format(" instrumentPST=%s ", bLogPrintStackTrace));

        String debug = System.getProperty("debug");
        if (!isEmpty(debug)) {
            if (debug.equalsIgnoreCase("true"))
                bDebug = true;
            else
                bDebug = false;
        }
        sysPropsString.append(String.format(" debug=%s ", bDebug));

        String overrideBc = System.getProperty("overrideExistingBC");
        if (!isEmpty(overrideBc)) {
            if (overrideBc.equalsIgnoreCase("true"))
                bOverrideExistingBC = true;
            else
                bOverrideExistingBC = false;
        }
        sysPropsString.append(String.format(" overrideExistingBC=%s ", bOverrideExistingBC));

        String stroverrideExistingPsts = System.getProperty("overrideExistingPstsIfReqd");
        if (!isEmpty(stroverrideExistingPsts)) {
            if (stroverrideExistingPsts.equalsIgnoreCase("true"))
                m_overrideExistingPsts = true;
            else
                m_overrideExistingPsts = false;
        }
        sysPropsString.append(String.format(" overrideExistingPstsIfReqd=%s ", m_overrideExistingPsts));

        String strJavaFiles = System.getProperty("javaFiles");
        if (!isEmpty(strJavaFiles)) {
            if (strJavaFiles.equalsIgnoreCase("all"))
                m_javaFiles = "all";
            else
                m_javaFiles = "standard";
        }
        sysPropsString.append(String.format(" javaFiles=%s ", m_javaFiles));

        String strPstLogLevel = System.getProperty("pstLogLevel");
        if (!isEmpty(strPstLogLevel)) {
            if (strPstLogLevel.equalsIgnoreCase("FINE"))
                m_pstLogLevel = "FINE";
            else if (strPstLogLevel.equalsIgnoreCase("FINER"))
                m_pstLogLevel = "FINER";
            else if (strPstLogLevel.equalsIgnoreCase("FINEST"))
                m_pstLogLevel = "FINEST";
            else if (strPstLogLevel.equalsIgnoreCase("SEVERE"))
                m_pstLogLevel = "SEVERE";
            else {
                System.out.println("pst log level unrecognized: " + strPstLogLevel);
                System.out.println("Possible values - SEVERE/FINE/FINER/FINEST");
                System.exit(1);
            }
        }
        sysPropsString.append(String.format(" pstLogLevel=%s ", m_pstLogLevel));

        return sysPropsString.toString();
    }

    /**
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length != 2 || isEmpty(args[0]) || isEmpty(args[1])) {
            String sysProps = " [ -DstartBC=true/false -DendBC=true/false -DinstrumentPST=true/false -Ddebug=true/false ]";
            System.out.println("Usage:  appsLoggerInstrumentation.sh <path to your code in ade> <doADE> " + sysProps);
            System.exit(1);
        }
        StringBuffer mailSummary = new StringBuffer();
        String systemProps = checkSystemProperties();

        System.out.println("-------------------------------------");
        System.out.println("INSTRUMENTATION:Running with properties ");
        System.out.println("--> " + systemProps);
        System.out.println("-------------------------------------");
        mailSummary.append("\n Instrumentation was run with properties: ");
        mailSummary.append(systemProps + "\n");
        String sCrawlDir = args[0];
        mailSummary.append("\n Script was run on Directory: " + sCrawlDir);
        Boolean bDoADE = new Boolean(args[1]);
        Date startDate = new Date();


        AppsLoggerInstrumentation appsLoggerInstrumentation = new AppsLoggerInstrumentation();
        // Remove any existing diff.txt. Create a new bufferred writer for diff.txt
        if (bDoADE) {
            appsLoggerInstrumentation.diffHelper.startDiffLog();
        }

        appsLoggerInstrumentation.crawlDirectory(sCrawlDir, bDoADE);

        if (exceptions != null && exceptions.size() > 0) {
            mailSummary.append("\n Exceptions occurred.. \n");
            Iterator<String> exceptionIter = exceptions.iterator();
            int cnt = 0;
            while (exceptionIter.hasNext()) {
                cnt++;
                String exc = exceptionIter.next();
                mailSummary.append(String.format("%d . %s", cnt, exc));
            }
        }
        String statistics = appsLoggerInstrumentation.printStatistics(startDate);
        mailSummary.append("\n Statistics: " + statistics);
        if (bDoADE) {
            appsLoggerInstrumentation.diffHelper.closeDiffLog();
        }
        Date runDate = new Date();
       // Mail.sendMail("zeesha.currimbhoy@oracle.com", mailSummary.toString(), "ALI Script Run At: " + runDate.toString());
    }

    public String printStatistics(Date startDate) {
        StringBuilder sb = new StringBuilder();
        // Print Statistics
        Date endDate = new Date();
        sb.append("\n====================================\n");
        sb.append("Run Statistics");
        sb.append("\n====================================\n");

        if (m_numberOfFilesInstrumented > 0) {
            sb.append("\nTotal Number of files instrumented: " + m_numberOfFilesInstrumented);
        }
        if (m_numberOfMethods > 0) {
            sb.append("\nTotal Number of methods Looked at: " + m_numberOfMethods);
        }
        if (m_numberOfMethodsInstrumented > 0) {
            sb.append("\nTotal Number of methods Instrumented at: " + m_numberOfMethodsInstrumented);
        }
        if (m_numberOfPSEFound > 0) {
            sb.append("\nTotal Number of calls to print stack trace found: " + m_numberOfPSEFound);
        }
        if (m_numberOfPSEInstrumented > 0) {
            sb.append("\nTotal Number of calls to print stack trace instrumented: " + m_numberOfPSEInstrumented);
        }
        System.out.println(sb.toString());
        return sb.toString();
    }


    public void crawlDirectory(String path, boolean bDoADE) {
        if (path.contains(".ade_path") || path.contains("/classes/"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String filePath = listOfFiles[i].getAbsolutePath();
                if (m_javaFiles.trim().equals("all")) {
                    if (isValidJavaFile(filePath)) {
                        try {
                            instrumentFileWithBreadCrumbs(listOfFiles[i], bDoADE);
                        } catch (Exception ex) {
                            System.out.println("Skipping file. Exception while trying to instrument file: " + filePath);
                            ex.printStackTrace();
                            addToExceptions(filePath, ex);
                            //exceptions.add(ex.getStackTrace().toString());
                        }
                    }
                } else if (isValidBCFile(filePath) && m_javaFiles.trim().equals("standard")) {
                    try {
                        instrumentFileWithBreadCrumbs(listOfFiles[i], bDoADE);
                    } catch (Exception ex) {
                        System.out.println("Skipping file. Exception while trying to instrument AM: " + filePath);
                        ex.printStackTrace();
                        addToExceptions(filePath, ex);
                        //exceptions.add(ex.getStackTrace().toString());
                    }
                }
                if (isValidJavaFile(filePath) && m_overrideExistingPsts) {
                    try {
                        cleanPreviousPstRun(listOfFiles[i], bDoADE);
                    } catch (Exception ex) {
                        System.out.println("Skipping file. Exception while trying to instrument for print stack trace: " +
                                           filePath);
                        ex.printStackTrace();
                        addToExceptions(filePath, ex);
                        //exceptions.add(ex.getStackTrace().toString());
                    }
                }
                if (isValidJavaFile(filePath) && bLogPrintStackTrace) {
                    try {
                        instrumentForPST(listOfFiles[i], bDoADE);
                    } catch (Exception ex) {
                        System.out.println("Skipping file. Exception while trying to instrument for print stack trace: " +
                                           filePath);
                        ex.printStackTrace();
                        addToExceptions(filePath, ex);
                        // exceptions.add(ex.getStackTrace().toString());
                    }
                }
            } else if (listOfFiles[i].isDirectory()) {
                if (isValidDirectory(listOfFiles[i].getAbsolutePath())) {
                    crawlDirectory(listOfFiles[i].getAbsolutePath(), bDoADE);
                }
            } else {
                if (bDebug) {
                    System.out.println("Neither node or directory");
                }
            }

        }

    }

    private void addToExceptions(String filePath, Exception ex) {
        StackTraceElement[] stackParts = ex.getStackTrace();
        if (stackParts == null)
            return;
        int stackPartsLen = stackParts.length;
        StringBuffer exStr = new StringBuffer();
        exStr.append(String.format("Exception occurred while processing file: %s \n", filePath));
        exStr.append(String.format("Exception Mesage: %s \n", ex.getMessage()));
        for (int i = 0; i < stackPartsLen; i++) {
            exStr.append(stackParts[i].toString());
            exStr.append("\n");
        }
        exceptions.add(exStr.toString());
    }

    public boolean modifyLineWithPstInstrumentat(int startIndex, int endIndex, JavaFileParserHelper helper) throws Exception {


        boolean importAdded = helper.addAppsLoggerImportIfRequred();
        if (!importAdded)
            throw new Exception("Could not add import statement");

        int lineNumber = helper.getLineNumberOfIndex(helper.getFileWithoutCommentsReadOnly(), startIndex);
        if (lineNumber == -1) {
            return false;
        }
        String appsLogLevelString = "AppsLogger." + m_pstLogLevel;

        String origLine = helper.getLineAtLineNumber(lineNumber);
        String startPadding = helper.getWhiteSpacePrefix(origLine);


        if (isEmpty(origLine)) {
            return false;
        }
        Matcher pstOnLineMatcher = helper.getPSTMatcher(origLine);
        StringBuffer newLine = new StringBuffer();
        if (pstOnLineMatcher.find()) {
            int start = pstOnLineMatcher.start();
            if (start < 0)
                return false;
            String exIdentifier = pstOnLineMatcher.group(1);
            if (isEmpty(exIdentifier))
                return false;

            newLine.append(origLine.substring(0, start));

            // add the apps log statement
            newLine.append("//" + PST_HAVE_BEEN_HERE_COMMENT + "\n");
            newLine.append(startPadding + String.format("if (AppsLogger.isEnabled(%s)) { \n", appsLogLevelString));
            newLine.append(startPadding + "\t" +
                           String.format("AppsLogger.write(this, %s ,%s);\n", exIdentifier, appsLogLevelString));
            newLine.append(startPadding + "}\n");

            if (bDebug) {
                System.out.println("Original Line: \n" +
                        origLine);
                System.out.println("New Line: \n" +
                        newLine.toString());
            }
            return helper.setLineAtLineNumber(newLine.toString(), lineNumber);
        } else {
            return false;
        }

    }

    public String getAppsLogWriteStatement(String methodName, ArrayList<String> methodParams, String startPadding,
                                           HashSet<String> supressParams) {
        // TO AVOID ANY NPES
        if (supressParams == null) {
            supressParams = new HashSet<String>();
        }
        StringBuilder methodStr = new StringBuilder();
        methodStr.append("\"");
        methodStr.append(methodName + ":Start");

        if (methodParams == null || methodParams.size() <= 0) {
            methodStr.append("()");
            methodStr.append("\"");
        } else {
            methodStr.append("(");

            Iterator<String> paramsIter = methodParams.iterator();
            int cnt = 0;
            while (paramsIter.hasNext()) {
                if (cnt != 0) {
                    // "Integer a="+a+","
                    methodStr.append("+\",");
                }
                String paramStr = paramsIter.next();
                String[] paramParts = paramStr.split("#", 2);
                if (paramParts == null || paramParts.length != 2)
                    continue;
                String paramType = paramParts[0];
                String paramName = paramParts[1];
                if (supressParams.contains(paramName)) {
                    methodStr.append(String.format(" %s %s = -----\" ", paramType, paramName));
                } else {
                    methodStr.append(String.format(" %s %s = \" + %s", paramType, paramName, paramName));
                }
                // "Integer a="+a

                cnt++;
            }
            // (Integer a = "+a+")"
            methodStr.append("+\")\"");
        }
        return String.format("AppsLogger.write(this, %s ,AppsLogger.FINER);", methodStr.toString());
        //return methodStr.toString();
    }

    public String createStartBreadCrumb(String methodName, ArrayList<String> methodParams, String startPadding,
                                        HashSet<String> supressParams) {
        // TO AVOID ANY NPES
        if (supressParams == null) {
            supressParams = new HashSet<String>();
        }
        String appsLogWriteStatement = getAppsLogWriteStatement(methodName, methodParams, startPadding, supressParams);

        final String startAppsLogStatement = "if (AppsLogger.isEnabled(AppsLogger.FINER)) { \n";
        StringBuilder startBreadcrumb = new StringBuilder();
        startBreadcrumb.append(startPadding + "//" + HAVE_BEEN_HERE_COMMENT + "\n");
        startBreadcrumb.append(startPadding + startAppsLogStatement);
        startBreadcrumb.append(startPadding + "\t" + String.format(" %s\n", appsLogWriteStatement));
        startBreadcrumb.append(startPadding + "}");

        return startBreadcrumb.toString();
    }

    public String createEndBreadCrumb(String methodName, String returnVal, String startPadding) {
        final String startAppsLogStatement = "if (AppsLogger.isEnabled(AppsLogger.FINER)) { \n";
        StringBuilder startBreadcrumb = new StringBuilder();
        startBreadcrumb.append(startPadding + startAppsLogStatement);
        startBreadcrumb.append(startPadding + "\t" +
                               String.format("AppsLogger.write(this, \"%s:End\",AppsLogger.FINER);\n", methodName));
        startBreadcrumb.append(startPadding + "}\n");
        return startBreadcrumb.toString();
    }

    /**
     *No getters or setters
     * No methods like print* debug*
     * @param methodName
     * @return
     */
    public boolean isValidMethodToInstrument(String methodName) {
        if (isEmpty(methodName))
            return false;
        if (methodName.trim().startsWith("get") || methodName.trim().startsWith("set"))
            return false;
        if (methodName.trim().startsWith("print") || methodName.trim().startsWith("debug"))
            return false;
        if (methodName.trim().equals("put"))
            return false;
        return true;
    }

    public String getStartAppsLogStatement() throws Exception {

        return "if (AppsLogger.isEnabled(AppsLogger.FINER)) { \n";
    }

    public String getEndAppsLogStatement() {
        return "}\n";
    }

    public String getApsLogWriteStatement(int lineNumber, JavaFileParserHelper javaHelper) throws Exception {
        String methodBody = getUncommentedBodyOfAppsLogStatement(lineNumber, javaHelper);
        if (isEmpty(methodBody)) {
            return null;
        }
        Matcher apLogWriteStartMatcher = javaHelper.getAppsLogWriteMatcher(methodBody);
        if (apLogWriteStartMatcher.find()) {
            int start = apLogWriteStartMatcher.start();
            int end = apLogWriteStartMatcher.end();
            return methodBody.substring(start, end);
        }
        return null;
    }

    public boolean containsStandardizedAppsLogStartStatement(String line, int lineNumber,
                                                             JavaFileParserHelper javaHelper) throws Exception {
        String commentBeforeLine = javaHelper.getSingleLineCommentJustBeforeLine(lineNumber);
        if (!isEmpty(commentBeforeLine)) {
            commentBeforeLine = commentBeforeLine.trim();
            if (commentBeforeLine.contains(HAVE_BEEN_HERE_COMMENT))
                return true;
        }

        //Matcher appsLogStartMatcher = javaHelper.getAppsLogStartMatcher(line);
        //if (!appsLogStartMatcher.find()) {
        //return false;
        //}
        return false;
    }

    /**
     *Supress string //@ALI:Supress Params=
     * @param javaHelper
     * @param methodStartIndex
     * @param methodName
     * @return
     * @throws Exception
     */
    public HashSet<String> getSupressParams(JavaFileParserHelper javaHelper, int methodStartIndex,
                                            String methodName) throws Exception {
        // get the comment block before the method
        HashSet<String> supressedParams = new HashSet<String>();
        String fileWithoutCmmts = javaHelper.getFileWithoutCommentsReadOnly();
        int methodStartLineNumber = javaHelper.getLineNumberOfIndex(fileWithoutCmmts, methodStartIndex);
        String singleLineComment = javaHelper.getSingleLineCommentJustBeforeLine(methodStartLineNumber);
        if (singleLineComment != null && !singleLineComment.trim().equals("")) {
            if (singleLineComment.trim().startsWith("//")) {
                int indexOfSupress = singleLineComment.indexOf(ALI_SUPRESS_LINE);
                if (indexOfSupress == -1)
                    return null;
                String paramsToSupress = singleLineComment.substring(indexOfSupress + ALI_SUPRESS_LINE.length());
                if (paramsToSupress == null)
                    return null;
                paramsToSupress = paramsToSupress.trim();

                if (paramsToSupress.startsWith("Params=")) {
                    String paramsStr = paramsToSupress.substring("Params=".length());
                    if (paramsStr == null)
                        return null;
                    String[] paramParts = paramsStr.split(",");
                    for (int i = 0; i < paramParts.length; i++) {
                        supressedParams.add(paramParts[i].trim());
                    }
                }
            }
        }
        return supressedParams;
    }

    public boolean isMethodBodyEmpty(String methodName, int methodStartIndex, int methodEndIndex,
                                     JavaFileParserHelper javaHelper) throws Exception {
        String fileWithoutCmmts = javaHelper.getFileWithoutCommentsReadOnly();

        int methodStartLineNumber = javaHelper.getLineNumberOfIndex(fileWithoutCmmts, methodStartIndex);
        String methodBody = javaHelper.getMethodBodyFromUncommentedFile(methodStartLineNumber);
        String bodyWithoutBraces = methodBody.trim();
        if (bodyWithoutBraces.startsWith("{"))
            bodyWithoutBraces = bodyWithoutBraces.substring(1);
        if (bodyWithoutBraces.endsWith("}"))
            bodyWithoutBraces = bodyWithoutBraces.substring(0, bodyWithoutBraces.length() - 1);
        if (isEmpty(bodyWithoutBraces)) {
            return true;
        }
        return false;
    }
    // @ALI:Supress Params=

    public boolean instrumentForStartBreadCrumb(String methodName, ArrayList<String> methodParams,
                                                JavaFileParserHelper javaHelper, int methodDeclStartIndex,
                                                int methodDeclEndIndex) throws Exception {
        if (isEmpty(methodName))
            return false;


        if (bDebug)
            System.out.println("Instrumenting method For start breadcrumb: " + methodName);

        boolean existingStandardizedALI = false;

        String fileWithoutCmmts = javaHelper.getFileWithoutCommentsReadOnly();

        // [START] Check for existing bcs
        int methodStartLineNumber = javaHelper.getLineNumberOfIndex(fileWithoutCmmts, methodDeclStartIndex);
        if (methodStartLineNumber < 0) {
            if (bDebug) {
                System.out.println("[ERROR] could not get line number of method: " + methodName);
            }
            return false;
        }

        // no need to do any instrumentation for empty methods
        if (isMethodBodyEmpty(methodName, methodDeclStartIndex, methodDeclEndIndex, javaHelper)) {
            return false;
        }

        int nextNonCommentedLineNumber = javaHelper.getNextNonCommentedLineNumberAfterIndex(methodDeclEndIndex + 1);
        String nextUncommentedNonBlankLine = javaHelper.getLineAtLineNumber(nextNonCommentedLineNumber);

        if (nextUncommentedNonBlankLine.contains("AppsLogger.isEnabled")) {
            if (bDebug) {
                System.out.println("**** BC Already present ****");
            }
            if (!bOverrideExistingBC) {
                if (bDebug) {
                    System.out.println("[NO BC INJECTION] BC Present and User does not want to override.. ");
                }
                return false;
            }

            // check if bc is standardized
            if (!containsStandardizedAppsLogStartStatement(nextUncommentedNonBlankLine, nextNonCommentedLineNumber,
                                                           javaHelper)) {
                if (bDebug) {
                    System.out.println("Present BC was not inserted by script.. cannot override non standard BC");
                }
                return false;
            } else {
                // START BC PRESENT AND HAS BEEN INSERTED BY SCRIPT - NEED TO CHECK IF IT IS STILL IN RIGHT FORMAT
                existingStandardizedALI = true;

            }

        }

        String existingLine = nextUncommentedNonBlankLine;
        if (isEmpty(existingLine)) {
            return false;
        }
        if (existingLine.trim().startsWith("return ") || existingLine.trim().startsWith("return;") ||
            existingLine.trim().startsWith("}")) {
            System.out.println("Cannot instrument for start breadcrumb, since first line of method has return. First Line is: " +
                               existingLine);
            return false;
        }

        String startPadding = javaHelper.getWhiteSpacePrefix(existingLine);
        HashSet<String> supressParams = getSupressParams(javaHelper, methodDeclStartIndex, methodName);
        if (existingStandardizedALI) {
            String appslogwriteStatement = getApsLogWriteStatement(nextNonCommentedLineNumber, javaHelper);
            appslogwriteStatement = appslogwriteStatement.trim();

            String appslogWriteToInject = getAppsLogWriteStatement(methodName, methodParams, startPadding, supressParams);
            appslogWriteToInject = appslogWriteToInject.trim();
            // check existing apps log write statement with one to be injected
            if (appslogWriteToInject != null && appslogWriteToInject.equals(appslogwriteStatement)) {
                if (bDebug) {
                    System.out.println("Existing apps log statement is up to date.. ");
                }
                return false;
            } else {
                // if does not match - remove existing apps log statement
                boolean fixMade = removeStandardizedAppsLogBlock(nextNonCommentedLineNumber, javaHelper);
                if (!fixMade) {
                    System.out.println("Could not remove existing apps log statement when required.. will exit this fix");
                    return false;
                }
            }

        }
        String startBreadcrumb = createStartBreadCrumb(methodName, methodParams, startPadding, supressParams);


        boolean importAdded = javaHelper.addAppsLoggerImportIfRequred();
        if (!importAdded)
            throw new Exception("Could not add import statement");
        boolean startBcAdded = javaHelper.addBeforeExistingLineAtNumber(startBreadcrumb, nextNonCommentedLineNumber, false);
        if (startBcAdded)
            return true;

        return false;
    }

    public boolean removeEntireMethodDecl(int methodStartLineNumber, JavaFileParserHelper javaHelper) throws Exception {
        int endOfMethodLineNumber = javaHelper.getLineNumberOfEndMethod(methodStartLineNumber);
        int endOfMethodBlockIndex = javaHelper.getIndexAtEndLineNumberOfEndMethod(methodStartLineNumber);
        StringBuffer origContents = new StringBuffer();
        StringBuffer modContents = new StringBuffer();


        for (int i = methodStartLineNumber; i < endOfMethodLineNumber; i++) {
            String origLine = javaHelper.getLineAtLineNumber(i);
            javaHelper.setLineAtLineNumber("", i);

            origContents.append(origLine);
            modContents.append("");
        }
        String endLine = javaHelper.getLineAtLineNumber(endOfMethodLineNumber);
        String endLineSubstring = endLine.substring(endOfMethodBlockIndex + 1);
        String orig = javaHelper.getLineAtLineNumber(endOfMethodLineNumber);
        javaHelper.setLineAtLineNumber(endLineSubstring, endOfMethodLineNumber);
        origContents.append(orig);
        modContents.append(endLineSubstring);

        if (bDebug) {
            System.out.println("OrigContents: \n" +
                    origContents.toString());
            System.out.println("modifiedContents: \n" +
                    modContents.toString());
        }

        return true;
    }

    public boolean removeStandardizedAppsLogBlock(int appsLogBlockStartLineNumber,
                                                  JavaFileParserHelper javaHelper) throws Exception {
        int endOfAppslogBlockLineNumber = javaHelper.getLineNumberOfEndMethod(appsLogBlockStartLineNumber);
        int endOfAppsLogBlockIndex = javaHelper.getIndexAtEndLineNumberOfEndMethod(appsLogBlockStartLineNumber);
        int lineNumberOfStandardizedComment =
            javaHelper.getLineNumberOfSingleLineCommentJustBeforeLine(appsLogBlockStartLineNumber);
        String singleLineComment = javaHelper.getLineAtLineNumber(lineNumberOfStandardizedComment);
        StringBuffer origContents = new StringBuffer();
        StringBuffer modContents = new StringBuffer();
        boolean fixMade = false;

        if (singleLineComment.trim().equals("//" + HAVE_BEEN_HERE_COMMENT)) {

            // remove comment
            String origCommentLine = javaHelper.getLineAtLineNumber(lineNumberOfStandardizedComment);
            javaHelper.setLineAtLineNumber("", lineNumberOfStandardizedComment);
            origContents.append(origCommentLine);
            modContents.append("");
            fixMade = true;

            String startLine = javaHelper.getLineAtLineNumber(appsLogBlockStartLineNumber);
            for (int i = appsLogBlockStartLineNumber; i < endOfAppslogBlockLineNumber; i++) {
                String origLine = javaHelper.getLineAtLineNumber(i);
                javaHelper.setLineAtLineNumber("", i);

                origContents.append(origLine);
                modContents.append("");
            }

            String endLine = javaHelper.getLineAtLineNumber(endOfAppslogBlockLineNumber);
            // ERROR - check if index is after the length
            String endLineSubstring = endLine.substring(endOfAppsLogBlockIndex + 1);
            String orig = javaHelper.getLineAtLineNumber(endOfAppslogBlockLineNumber);
            javaHelper.setLineAtLineNumber(endLineSubstring, endOfAppslogBlockLineNumber);
            origContents.append(orig);
            modContents.append(endLineSubstring);

            if (bDebug) {
                System.out.println("OrigContents: \n" +
                        origContents.toString());
                System.out.println("modifiedContents: \n" +
                        modContents.toString());
            }

            return fixMade;
        } else {
            return false;
        }

    }

    public String getUncommentedBodyOfAppsLogStatement(Integer startOfAppsLogStatementLineNum,
                                                       JavaFileParserHelper javaHelper) throws Exception {
        String startOfAppsLogStatement = javaHelper.getLineAtLineNumber(startOfAppsLogStatementLineNum);
        if (isEmpty(startOfAppsLogStatement))
            return "";
        String methodBody = javaHelper.getMethodBodyFromUncommentedFile(startOfAppsLogStatementLineNum);
        String methodBodyWithoutComments = "";
        try {
            methodBodyWithoutComments = javaHelper.removeSingleLineComments(methodBody);
            methodBodyWithoutComments = javaHelper.removeMultilineComments(methodBodyWithoutComments);
        } catch (Exception ex) {
            throw new Exception("Exception while trying to remove comments from apps log body", ex);
        }

        return methodBodyWithoutComments;

    }

    public boolean instrumentForEndBreadCrumb(String methodName, String returnType, JavaFileParserHelper javaHelper,
                                              int methodStartIndex) throws Exception {
        if (isEmpty(methodName))
            return false;


        if (bDebug)
            System.out.println("Instrumenting method For end breadcrumb: " + methodName);

        //TODO: find if the method has annotations to avoid instrumentation - return false

        // get method body

        // find all return statements

        // for each return statement :
        // if previous uncommented line already has apps log statements - continue
        // else instrument return statement


        boolean importAdded = javaHelper.addAppsLoggerImportIfRequred();
        if (!importAdded)
            throw new Exception("Could not add import statement");


        return false;
    }

    public boolean instrumentMethod(String methodName, ArrayList<String> methodParams, String returnType,
                                    JavaFileParserHelper javaHelper, int startIndexOfMethod, int endIndexOfMethod,
                                    boolean bDoADE) throws Exception {
        if (isEmpty(methodName))
            return false;
        boolean fixMade = false;

        if (!bStartBreadCrumb && !bEndBreadCrumb) {
            return false;
        }


        if (bStartBreadCrumb) {
            boolean starBcAdded =
                instrumentForStartBreadCrumb(methodName, methodParams, javaHelper, startIndexOfMethod, endIndexOfMethod);
            if (starBcAdded)
                fixMade = true;
        }

        /* TODO:EndBC
        * if (bEndBreadCrumb) {
            boolean endBcAdded = instrumentForEndBreadCrumb(methodName, returnType, javaHelper, endIndexOfMethod);
            if (endBcAdded)
                fixMade = true;
        }*/


        return fixMade;
    }

    public ArrayList<String> getParamsSetFromString(String paramsStr) {
        ArrayList<String> paramsSet = new ArrayList<String>();
        if (isEmpty(paramsStr))
            return paramsSet;
        paramsStr = paramsStr.trim();
        if (paramsStr.startsWith("("))
            paramsStr = paramsStr.substring(paramsStr.indexOf("(") + 1);
        if (paramsStr.endsWith(")"))
            paramsStr = paramsStr.substring(0, paramsStr.lastIndexOf(")"));

        /**
     * params :
     * (int i, int a)
     * (OAViewobjectImpl vo)
     * (String a, Integer b)
     * (HashSet<String, String>  // TODO CASE
     */
        String[] mainParams = paramsStr.split(",");
        int mainparamsLen = mainParams.length;
        for (int i = 0; i < mainparamsLen; i++) {
            String param = mainParams[i].trim();
            if (isEmpty(param))
                continue;
            String[] paramParts = param.split(" ");
            if (paramParts.length != 2) {
                System.out.println("Could not get param parts");
                return null;
            }

            paramsSet.add(paramParts[0] + "#" + paramParts[1]);
        }
        return paramsSet;
    }

    public void instrumentForPST(File file, boolean bDoADE) throws Exception {
        String filePath = file.getAbsolutePath();
        boolean fixMade = false;

        JavaFileParserHelper javaparser = new JavaFileParserHelper(filePath);
        javaparser.removeAllComments();
        String fileWithoutComments = javaparser.getFileWithoutCommentsReadOnly();

        if (bLogPrintStackTrace) {
            Matcher pstMatcher = javaparser.getPSTMatcher(fileWithoutComments);
            while (pstMatcher.find()) {
                m_numberOfPSEFound++;
                int start = pstMatcher.start();
                int end = pstMatcher.end();
                boolean pstInstrumented = modifyLineWithPstInstrumentat(start, end, javaparser);
                if (pstInstrumented) {
                    m_numberOfPSEInstrumented++;
                    fixMade = true;
                }

            }
        }
        if (fixMade && bDoADE) {
            try {
                String fileContentsStr = javaparser.getFileContentsAsString(false);
                applyFix(filePath, fileContentsStr);
            } catch (Exception ex) {
                System.out.println("Exception occurred while applying fix for file: " + filePath);
                ex.printStackTrace();
            }
        }

    }


    public boolean cleanPreviousPstRun(File file, boolean bDoADE) throws Exception {

        String filePath = file.getAbsolutePath();
        if (bDebug) {
            System.out.println("checking file: " + file.getName());
        }
        boolean fixMade = false;

        JavaFileParserHelper javaHelper = new JavaFileParserHelper(filePath);
        String originalFileContents = javaHelper.getFileContentsAsString(false);
        Pattern pstBeenHereMatcher = Pattern.compile(PST_HAVE_BEEN_HERE_REGEX, Pattern.MULTILINE);
        Matcher m = pstBeenHereMatcher.matcher(originalFileContents);
        while (m.find()) {
            int pstStart = m.start();
            int pstEnd = m.end();
            int pstLineNumber = javaHelper.getLineNumberOfIndex(originalFileContents, pstEnd);
            int pstAppsLogStartLineNumber = javaHelper.getLineNumberOfNextNonBlankLine(pstLineNumber);
            String existingPstappsLogStartLine = javaHelper.getLineAtLineNumber(pstAppsLogStartLineNumber);

            if (existingPstappsLogStartLine.trim().contains("AppsLogger.isEnabled")) {

                Integer endOfMethodLineNumber = javaHelper.getLineNumberOfEndMethod(pstAppsLogStartLineNumber);
                String methodBody = javaHelper.getContentsBetweenLineNumbers(pstAppsLogStartLineNumber, endOfMethodLineNumber);
                if (bDebug) {
                    System.out.println("Existing PST Method body is:\n" +
                            methodBody);
                }
                Matcher appsLogLevelMatcher = javaHelper.getAppsLogBlockMatcher(methodBody);
                if (!appsLogLevelMatcher.find()) {
                    return false;
                } else {

                    int groupCnt = appsLogLevelMatcher.groupCount();
                    if (groupCnt != 2)
                        return false;
                    String enabledLevel = appsLogLevelMatcher.group(1);
                    String writeLevel = appsLogLevelMatcher.group(2);
                    if (isEmpty(enabledLevel) || isEmpty(writeLevel))
                        return false;
                    if (enabledLevel.trim().equals(writeLevel.trim()) && enabledLevel.trim().equals(m_pstLogLevel)) {
                        //  no clean up required.. already set at the required level
                        return false;
                    } else {
                        StringBuffer newPstContents = new StringBuffer();
                        // clean up required
                        int startofEnabledLogLevel = appsLogLevelMatcher.start(1);
                        int endOfEnabledLogLevel = appsLogLevelMatcher.end(1);
                        int startOfWriteLogLevel = appsLogLevelMatcher.start(2);
                        int endOfWriteLogLevel = appsLogLevelMatcher.end(2);
                        if (startofEnabledLogLevel == -1 || endOfEnabledLogLevel == -1 || startOfWriteLogLevel == -1 ||
                            endOfWriteLogLevel == -1)
                            return false;
                        newPstContents.append(methodBody.substring(0, startofEnabledLogLevel));
                        newPstContents.append(m_pstLogLevel);
                        newPstContents.append(methodBody.substring(endOfEnabledLogLevel, startOfWriteLogLevel));
                        newPstContents.append(m_pstLogLevel);
                        newPstContents.append(methodBody.substring(endOfWriteLogLevel));

                        if (bDebug) {
                            System.out.println("New PstContents to be set: \n " + newPstContents.toString());
                        }

                        boolean origPstRemoved = removeEntireMethodDecl(pstAppsLogStartLineNumber, javaHelper);
                        if (origPstRemoved) {
                            javaHelper.setLineAtLineNumber(newPstContents.toString(), pstAppsLogStartLineNumber);
                            if (bDebug) {
                                System.out.println("New PST Set:\n " + newPstContents.toString());
                            }
                        }
                        return true;
                    }

                }
            }

        }

        return false;
    }

    public void instrumentFileWithBreadCrumbs(File file, boolean bDoADE) throws Exception {
        if (!bStartBreadCrumb && !bEndBreadCrumb) {
            return;
        }
        String filePath = file.getAbsolutePath();
        boolean fixMade = false;
        if (bDebug)
            System.out.println("instrumenting file: " + filePath);

        int origMethodsInstrumented = m_numberOfMethodsInstrumented;
        JavaFileParserHelper javaparser = new JavaFileParserHelper(filePath);
        javaparser.removeAllComments();
        String fileWithoutComments = javaparser.getFileWithoutCommentsReadOnly();
        if (bStartBreadCrumb || bEndBreadCrumb) {

            Matcher methodMatcher = javaparser.getMethodMatcher(fileWithoutComments);
            // will be used to filter out static methods
            final int methodAccessType = 1;
            final int methodNameGroup = 3;
            final int methodParamsGroup = 4;
            int numMethods = 0;

            while (methodMatcher.find()) {
                numMethods++;
                int groupCnt = methodMatcher.groupCount();
                // avoid indexout of bounds
                if (groupCnt < methodNameGroup)
                    continue;
                if (groupCnt < methodParamsGroup)
                    continue;

                String methodName = methodMatcher.group(methodNameGroup);
                if (!isValidMethodToInstrument(methodName)) {
                    continue;
                }
                String accesType = methodMatcher.group(methodAccessType);
                if (!isEmpty(accesType) && accesType.trim().equals("static")) {
                    System.out.println("[ATTENTON] Will not instrument static method - " + methodName);
                    continue;
                }
                String params = methodMatcher.group(methodParamsGroup);
                ArrayList<String> methodParams = getParamsSetFromString(params);
                boolean isInstrumented =
                    instrumentMethod(methodName, methodParams, "", javaparser, methodMatcher.start(), methodMatcher.end(),
                                     bDoADE);
                if (isInstrumented) {
                    m_numberOfMethodsInstrumented++;
                    fixMade = true;
                }
            }
            m_numberOfMethods = m_numberOfMethods + numMethods;
            if (m_numberOfMethodsInstrumented > origMethodsInstrumented)
                m_numberOfFilesInstrumented++;
        }


        if (fixMade && bDoADE) {
            try {
                String fileContentsStr = javaparser.getFileContentsAsString(false);
                applyFix(filePath, fileContentsStr);
            } catch (Exception ex) {
                System.out.println("Exception occurred while applying fix for file: " + filePath);
                ex.printStackTrace();
            }
        }


    }

    public boolean isValidBCFile(String absFilePath) {
        if (!isEmpty(absFilePath)) {
            if (!isValidJavaFile(absFilePath))
                return false;

            if (absFilePath.contains("/test/") || absFilePath.contains("/modelTest/") || absFilePath.contains("/classes/") ||
                absFilePath.contains("Ui/") || absFilePath.contains("/dbSchema/") || absFilePath.contains("/.ade_path/")) {
                return false;
            }
            if (absFilePath.endsWith("AMImpl.java") || absFilePath.endsWith("Bean.java") ||
                absFilePath.endsWith("VOImpl.java")) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidJavaFile(String absFilePath) {
        if (!isEmpty(absFilePath)) {
            if (absFilePath.contains("/test/") || absFilePath.contains("/modelTest/") || absFilePath.contains("/classes/") ||
                absFilePath.contains("/dbSchema/") || absFilePath.contains("/.ade_path/")) {
                return false;
            }
            if (absFilePath.endsWith("AMClient.java") || absFilePath.endsWith("VOClient.java") ||
                absFilePath.endsWith("VORowClient.java") || absFilePath.endsWith("ServiceImpl.java")) {
                return false;
            }
            if (absFilePath.endsWith(".java")) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidDirectory(String absFilePath) {
        if (!isEmpty(absFilePath)) {
            if (absFilePath.contains("/test/") || absFilePath.contains("/modelTest/") || absFilePath.contains("/classes/") ||
                absFilePath.contains("/dbSchema/") || absFilePath.contains("/.ade_path/")) {
                return false;
            }

        }
        return true;
    }


    public void applyFix(String absPath, String newFileContents) throws Exception {
        File file = new File(absPath);
        if (!file.exists()) {
            throw new Exception("while making fix, file not found: " + absPath);
        }
        diffHelper.checkoutAndDelete(file);

        FileWriter fw = new FileWriter(file.getAbsolutePath());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(newFileContents);
        if (bw != null)
            bw.close();
        if (fw != null)
            fw.close();
        diffHelper.checkinAndDiff(file);
    }

    /**
     *checks whether the given string is empty
     * @param str
     * @return
     */
    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }
}
