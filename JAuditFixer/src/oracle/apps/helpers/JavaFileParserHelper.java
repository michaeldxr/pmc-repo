package oracle.apps.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.io.StringReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JavaFileParserHelper {

    private String[] fileContents = null;
    private String fileWithoutCommentsReadOnly = null;
    private File m_file = null;
    // if you turng this out will print sop statements to console.. useful for debugging
    private boolean bDebug = false;
    private String FILE_NEW_LINE = "";
    public static boolean implementsInterface= false;

    private boolean m_appslogImportPresent = false;
    
    public HashMap<String,String> comments = new HashMap<String,String>();

    public String getFileWithoutCommentsReadOnly() {
        return fileWithoutCommentsReadOnly;
    }

    // for test purposes

    public static void main(String[] args) {
        // test methods for this class
        String fileName = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.FINM6B_LINUX") + 
            "/fusionapps/fin/components/ledger/gl/journals/desktopEntry/model/src/oracle/apps/financials/generalLedger/journals/desktopEntry/model/applicationModule/FinGlDesktopEntryAMImpl.java";


        try {

            JavaFileParserHelper helper = new JavaFileParserHelper(fileName);
            helper.removeAllComments();
            helper.getClassName(helper.getFileWithoutCommentsReadOnly());
            System.exit(1);
            String line1 = "if(AppsLogger.isEnabled(AppsLogger.FINER)){" + "AppsLogger.write(this,ex,AppsLogger.FINER);" + "}";
            String line2 = "if (AppsLogger.isEnabled(AppsLogger.FINER)) { \n" +
                "                                	AppsLogger.write(this, e ,AppsLogger.FINER);\n" +
                "                                }";

            Matcher m1 = helper.getAppsLogBlockMatcher(line2);
            if (m1.find()) {
                int s = m1.start();
                int e = m1.end();
                // String r = line1.substring(s, e);
                int grpCnt = m1.groupCount();
                for (int i = 0; i <= grpCnt; i++) {
                    System.out.println("Grp " + i + " is: " + m1.group(i));
                }
                // System.out.println("Result is: " +r);

                System.out.println("Yay");
                System.exit(1);
            } else {
                System.exit(1);
            }


            String PST_HAVE_BEEN_HERE_REGEX = "//\\[PrintStackTraceALIStandard\\]";
            String line = "  //[PrintStackTraceALIStandard]\n" +
                "            if (AppsLogger.isEnabled(AppsLogger.FINER)) { ";
            Pattern pstBeenHereMatcher = Pattern.compile(PST_HAVE_BEEN_HERE_REGEX, Pattern.MULTILINE);
            Matcher m = pstBeenHereMatcher.matcher(line);
            if (m.find()) {
                int s = m.start();
                int e = m.end();
                System.out.println(line.substring(s, e));
                System.out.println("Found");
            }

            //Test 1 - File conversion to string
            helper.fileContents = helper.convertFileToStringArray(helper.m_file);
            String[] localFileContents = helper.getFileContents();
            if (localFileContents == null)
                throw new Exception("Could not convert file contents to string");
            int numberOfLinesinOrigFile = helper.getFileContents().length;
            System.out.println("Number of lines in orig file: " + numberOfLinesinOrigFile);

            // Test 2 - Remove Comments
            helper.removeAllComments();
            String noComments = helper.getFileWithoutCommentsReadOnly();
            int numLines = helper.countNumberOfLinesInFile(noComments);
            System.out.println("Number of lines in file without comments: " + numLines);
            if (numberOfLinesinOrigFile != numLines) {
                throw new Exception("remove comments number of lines not equal to original number of lines");
            }

            String pst = " int i =0; " + " ex.printStackTrace();" + "me = 0;";
            helper.findAllPSTS(pst);


            // Test 3 - methods regex
            // helper.findAllMethods(helper.fileWithoutCommentsReadOnly);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void setFileContents(String[] fileContents) {
        this.fileContents = fileContents;
    }

    public String[] getFileContents() {
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return fileContents;
        }
        return fileContents;
    }

    public static class JavaParsingRegEx {
        public static final String requiredWhiteSpace = "[\\s]+";
        public static final String whiteSpace = "[\\s]*";
        public static final String identifier = "[0-9a-zA-Z_]+";

        public static final String classDeclRegEx =
            "(public|private|protected|static|final|native|synchronized|abstract|threadsafe|transient)*\\s*class" +
            requiredWhiteSpace + "(" + identifier + ")" + whiteSpace + "[^{]*" + whiteSpace + "\\{";
        
        //SU: added this to catch patterns like public static class Classname ....
        public static final String classDeclRegEx_new =
            "(?:(?:public|private|protected|static|final|native|synchronized|abstract|threadsafe|transient)\\s+)*class" +
            requiredWhiteSpace + "(" + identifier + ")" + whiteSpace + "([^{]*)" + whiteSpace + "\\{";

        public static final String methodParamsRegEx =
            "\\(" + whiteSpace + "[" + identifier + requiredWhiteSpace + identifier + whiteSpace + "[,{0,1}]" + whiteSpace +
            "]*" + "\\)";

        public static final String methodReturnTypeRegEx = "(" + identifier + requiredWhiteSpace + ")";
        public static final String methodDeclRegEx =
            "(public|private|protected|static|final|native|synchronized|abstract|threadsafe|transient)+\\s*" +
            methodReturnTypeRegEx + "(" + identifier + ")" + whiteSpace + "(" + methodParamsRegEx + ")" + "[^{]*" + "\\{";
        
        
        public static final String methodDeclRegEx_new =
            "(?:(?:public|private|protected|static|final|native|synchronized|abstract|threadsafe|transient)\\s+)*" +
            methodReturnTypeRegEx + "(" + identifier + ")" + whiteSpace + "(" + methodParamsRegEx + ")" + "[^{]*" + "\\{";        

        private static final String importRegEx = "import" + whiteSpace + "[^;]*" + whiteSpace + ";";
        private static final String packageRegEx = "package" + whiteSpace + "([^;]*)" + whiteSpace + ";";
        private static final String importAppsLoggerRegEx =
            "import" + whiteSpace + "oracle\\.apps\\.fnd\\.applcore\\.log\\.AppsLogger";
       
        private static final Pattern importPattern = Pattern.compile(importRegEx, Pattern.MULTILINE);
        private static final Pattern importAppsLoggerPattern = Pattern.compile(importAppsLoggerRegEx, Pattern.MULTILINE);
        //private static final String pstRegEx = requiredWhiteSpace+identifier+"\\.printStackTrace\\(\\)"+whiteSpace+";";
        private static final String pstRegEx = "(" + identifier + ")" + "\\.printStackTrace\\(\\)" + whiteSpace + ";";
        public static final String appsLogFINERStartRegEx =
            "if" + whiteSpace + "\\(" + whiteSpace + "AppsLogger.isEnabled" + whiteSpace + "\\(" + whiteSpace +
            "AppsLogger.FINER" + whiteSpace + "\\)" + whiteSpace + "\\)" + whiteSpace + "\\{";
        public static final String appslogWriteRegEx =
            whiteSpace + "AppsLogger.write" + whiteSpace + "\\(" + whiteSpace + "this" + whiteSpace + "," + "[^;]*" +
            whiteSpace + ";";
        public static final Pattern appsLogWritePattern = Pattern.compile(appslogWriteRegEx);
        public static final Pattern appsLogFinerPattern = Pattern.compile(appsLogFINERStartRegEx);
        public static final Pattern whiteSpacePrefixPattern = Pattern.compile(whiteSpace);
        public static final Pattern classDeclPattern = Pattern.compile(classDeclRegEx, Pattern.MULTILINE);
        public static final Pattern classDeclPattern_new = Pattern.compile(classDeclRegEx_new, Pattern.MULTILINE);
        public static final Pattern methodDeclPattern = Pattern.compile(methodDeclRegEx, Pattern.MULTILINE);
        public static final Pattern methodDeclPattern_new = Pattern.compile(methodDeclRegEx_new, Pattern.MULTILINE);
        public static final Pattern pstPatter = Pattern.compile(pstRegEx, Pattern.MULTILINE);
        public static final Pattern packagePattern = Pattern.compile(packageRegEx, Pattern.MULTILINE);

    }

    public String getClassName(String fileWithoutComments) {
        Matcher classMatcher = JavaParsingRegEx.classDeclPattern.matcher(fileWithoutComments);

        if (classMatcher.find()) {
            return classMatcher.group(2);
        }
        return "";
    }
    
    public String getClassNameNew(String fileWithoutComments) {
        
        Matcher classMatcher = JavaParsingRegEx.classDeclPattern_new.matcher(fileWithoutComments);
        implementsInterface=false;
        if (classMatcher.find()) {
            if(classMatcher.group(2).contains("implements"))
                implementsInterface=true;
            return classMatcher.group(1);
        }
        return "";
    }
    
    public String getPackageName(String fileWithoutComments) {
        Matcher packageMatcher = JavaParsingRegEx.packagePattern.matcher(fileWithoutComments);

        if (packageMatcher.find()) {
            return packageMatcher.group(1);
        }
        return "";
    }

    public void findAllMethods(String fileWithoutComments) {
        Matcher methodMatcher = JavaParsingRegEx.methodDeclPattern.matcher(fileWithoutComments);
        int numberOfMethods = 0;
        while (methodMatcher.find()) {
            numberOfMethods++;
            int start = methodMatcher.start();
            int end = methodMatcher.end();
            int grpCnt = methodMatcher.groupCount();
            for (int i = 0; i <= grpCnt; i++) {
                String grp = methodMatcher.group(i);
                System.out.println("Group: " + i + ": " + grp);
                String methodName = methodMatcher.group(3);
                int endMethod = methodMatcher.end();
                int lineNum = getLineNumberOfIndex(fileWithoutComments, endMethod);
            }

            System.out.println("Group: " + methodMatcher.group(0));
        }
        System.out.println("Number of methods: " + numberOfMethods);
    }

    public void findAllPSTS(String fileWithoutComments) {
        Matcher pstMatcher = JavaParsingRegEx.pstPatter.matcher(fileWithoutComments);
        int numberOfMethods = 0;
        while (pstMatcher.find()) {
            numberOfMethods++;
            int start = pstMatcher.start();
            int end = pstMatcher.end();
            int grpCnt = pstMatcher.groupCount();
            for (int i = 0; i <= grpCnt; i++) {
                String grp = pstMatcher.group(i);
                System.out.println("Group: " + i + ": " + grp);
                int endMethod = pstMatcher.end();
                int lineNum = getLineNumberOfIndex(fileWithoutComments, endMethod);
            }
            System.out.println("Group: " + pstMatcher.group(0));


        }
        System.out.println("Number of methods: " + numberOfMethods);
    }


    public Matcher getAppsLogWriteMatcher(String contents) {
        return JavaParsingRegEx.appsLogWritePattern.matcher(contents);
    }

    public Matcher getAppsLogStartMatcher(String contents) {
        return JavaParsingRegEx.appsLogFinerPattern.matcher(contents);
    }

    public Matcher getAppsLogEnabledAtLevelMatcher(String contents, String appsLogLevel) {
        String appsLogEnabledAtLevelRegEx =
            "if" + JavaParsingRegEx.whiteSpace + "\\(" + JavaParsingRegEx.whiteSpace + "AppsLogger.isEnabled" +
            JavaParsingRegEx.whiteSpace + "\\(" + JavaParsingRegEx.whiteSpace + "AppsLogger." + appsLogLevel +
            JavaParsingRegEx.whiteSpace + "\\)" + JavaParsingRegEx.whiteSpace + "\\)" + JavaParsingRegEx.whiteSpace + "\\{";
        Pattern apsLogEnabledPatt = Pattern.compile(appsLogEnabledAtLevelRegEx);
        return apsLogEnabledPatt.matcher(contents);
    }

    public Matcher getAppsLogBlockMatcher(String contents) {

        String appsLogEnabledAtLevelRegEx =
            "if" + JavaParsingRegEx.whiteSpace + "\\(" + JavaParsingRegEx.whiteSpace + "AppsLogger.isEnabled" +
            JavaParsingRegEx.whiteSpace + "\\(" + JavaParsingRegEx.whiteSpace + "AppsLogger\\.([^\\)]*)" +
            JavaParsingRegEx.whiteSpace + "\\)" + JavaParsingRegEx.whiteSpace + "\\)" + JavaParsingRegEx.whiteSpace + "\\{";


        String appsLogWriteRegEx =
            JavaParsingRegEx.whiteSpace + "AppsLogger\\.write" + JavaParsingRegEx.whiteSpace + "\\(" + JavaParsingRegEx.whiteSpace +
            "this" + JavaParsingRegEx.whiteSpace + "," + "[^,]*" + "," + JavaParsingRegEx.whiteSpace + "AppsLogger\\.([^\\)]*)" +
            JavaParsingRegEx.whiteSpace + "\\)" + JavaParsingRegEx.whiteSpace + ";";
        StringBuffer appsLoggerBlockRegEx = new StringBuffer();
        appsLoggerBlockRegEx.append(appsLogEnabledAtLevelRegEx);
        appsLoggerBlockRegEx.append(appsLogWriteRegEx);

        appsLoggerBlockRegEx.append(JavaParsingRegEx.whiteSpace + "\\}");
        Pattern apsLogEnabledPatt = Pattern.compile(appsLoggerBlockRegEx.toString());
        return apsLogEnabledPatt.matcher(contents);
    }

    public Matcher getMethodMatcher(String contents) {
        return JavaParsingRegEx.methodDeclPattern.matcher(contents);
    }
    
    public Matcher getMethodMatcherNew(String contents) {
        return JavaParsingRegEx.methodDeclPattern_new.matcher(contents);
    }
    
    public boolean containsMethodDeclaration(String method, String fileContents) {
        Pattern p = Pattern.compile("(?:(?:public|private|protected|static|final|native|synchronized|abstract|threadsafe|transient)" +
            "\\s+)*\\w+\\s+" + method);
        Matcher m = p.matcher(fileContents);
        if(m.find())
            return true;
        else
            return false;
    }
    
    public ArrayList<String> updateSetAttrInvokeAccessorMethod(String attrName, ArrayList<String> code) throws Exception {
        
        Pattern p = Pattern.compile("(?:(?:public|private|protected|static|final|native|synchronized|abstract|threadsafe|transient)" +
            "\\s+)*\\w+\\s+setAttrInvokeAccessor\\s*\\(");
        Matcher m;
        
        int i; 
        for(i = 0; i < code.size(); i++) 
        {
            String line = code.get(i);

            if(line.contains("setAttrInvokeAccessor")) {
                m = p.matcher(line);
                if(m.find())
                    break;
            }
        }
        
        if(i >= code.size())
            return code;

        int start = i;
        
        String line = code.get(i);
        
        while(line.contains("{") == false)
        {
            i++;
            line += code.get(i);
        }
        
        int curly_cnt = 1;
        
        String block = line + "\n";
        
        while(curly_cnt > 0)
        {
            i++;
            line = code.get(i);
            if(line.contains("{"))
                curly_cnt++;
            else if(line.contains("}"))
                curly_cnt--;
            
            block += line + "\n";
        }
        
        int method_end = i;
        
        int remove_start = -1;
        int remove_end = -1;
        
        boolean attrFound = false;
        
        for(i = start; i <= method_end; i++) {
            
            String codeLine = code.get(i).trim();
            if(codeLine.contains(attrName))
                attrFound = true;
            
            if(codeLine.equals("case " + attrName + ":") 
               || codeLine.equals("case " + attrName.toUpperCase() + ":"))
                remove_start=i;
            
            if(remove_start != -1 && codeLine.contains("return;")){
                if(!codeLine.equals("return;"))
                    throw new Exception("EOImpl class could not be fixed. Please review and fix manually");
                remove_end = i;
                break;
            }
        }
        
        if(attrFound && (remove_start == -1 || remove_end == -1))
            throw new Exception("EOImpl class could not be fixed. Please review and fix manually");
        
        if(remove_start != -1 && remove_end != -1){
            for(i = remove_start; i <= remove_end; i++)
                code.remove(remove_start);      
        }
       
        return code;
        
    }
    
    public ArrayList<String> removeMethodWithName(String method, ArrayList<String> code, boolean replaceMethod, String methodCode) {
        
       Pattern p = Pattern.compile("(?:(?:public|private|protected|static|final|native|synchronized|abstract|threadsafe|transient)" +
           "\\s+)*\\w+\\s+" + method+"\\s*\\(");
       Matcher m;
    
        int i; 
        for(i = 0; i < code.size(); i++) 
        {
            String line = code.get(i);

            if(line.contains(method)) {
                m = p.matcher(line);
                if(m.find())
                    break;
            }
        }
        
        if(i >= code.size())
            return code;

        int start = i;

        String line = code.get(i);
        
        while(line.contains("{") == false)
        {
            i++;
            line += code.get(i);
        }
        
        int curly_cnt = 1;
        
        String block = line + "\n";
        
        while(curly_cnt > 0)
        {
            i++;
            line = code.get(i);
            if(line.contains("{"))
                curly_cnt++;
            else if(line.contains("}"))
                curly_cnt--;
            
            block += line + "\n";
        }
        
        int remove_end = i;
        
            for(i = start; i <= remove_end; i++)
                code.remove(start);
        
    
        line = code.get(start-1);        
        
        if(line.trim().equals("@Override")){
            code.remove(--start);
            line = code.get(start-1);
        }
        
        if(line.contains("<COMMENT_"))
            code.remove(start-1);
        else if(line.trim().endsWith("*/")) {
            while(start > 1){
                code.remove(--start);
                if(line.trim().contains("/*"))
                    break;
                line = code.get(start-1);
            }
        }
        
        line = code.get(start-1);        
        if(line.trim().equals("")){
            code.remove(start-1);
            line = code.get(start-1);
        }
        
        if(replaceMethod)
            code.add(start-1, methodCode);
        
        return code;
    }
    
    public static String getMethodWithName(String method, ArrayList<String> code) {
        
       Pattern p = Pattern.compile("(?:(?:public|private|protected|static|final|native|synchronized|abstract|threadsafe|transient)" +
           "\\s+)*\\w+\\s+" + method+"\\s*\\(");
       Matcher m;
    
        int i; 
        for(i = 0; i < code.size(); i++) 
        {
            String line = code.get(i);

            if(line.contains(method)) {
                m = p.matcher(line);
                if(m.find())
                    break;
            }
        }
        
        if(i >= code.size())
            return null;

        String line = code.get(i);
        
        while(line.contains("{") == false)
        {
            i++;
            line += code.get(i);
        }
        
        int curly_cnt = 1;        
        String block = line + "\n";
        
        while(curly_cnt > 0)
        {
            i++;
            line = code.get(i);
            if(line.contains("{"))
                curly_cnt++;
            else if(line.contains("}"))
                curly_cnt--;
            
            block += line + "\n";
        }        
       return block;
    }


    public Matcher getPSTMatcher(String contents) {
        return JavaParsingRegEx.pstPatter.matcher(contents);
    }

    public JavaFileParserHelper(String absFilePath) throws Exception {
        super();
        File f = new File(absFilePath);
        if (!f.exists()) {
            throw new Exception("File does not exist:" + absFilePath);
        }
        m_file = f;
    }

    public String[] getFileContentsArrayWithoutComments(String fileContentsWithoutComments) throws Exception {
        
        ArrayList<String> fileContents = new ArrayList<String>(500);
        
        BufferedReader input = new BufferedReader(new StringReader(fileContentsWithoutComments));
        BufferedReader input2 = new BufferedReader(new StringReader(fileContentsWithoutComments));
        
        String line;
        input.mark(1024);

        while ((line = input.readLine()) != null) {
            input2.skip(line.length());
            input2.mark(1024);
            int c1 = input2.read();
            int c2 = input2.read();
            input2.reset();

            /* Determine the new line character being used in the file.. there were some errors in
             * processing due to carriage return characters.. this is required to handle new line characters
             * in the file formatting*/
            String newline = "\n";
            if (c1 == -1) {
                newline = "";
            } else if (c1 == '\n') {
                input2.read();
            } else if (c1 == '\r' && c2 != '\n') {
                newline = "\r";
                input2.read();
            } else if (c1 == '\r' && c2 == '\n') {
                newline = "\r\n";
                input2.read();
                input2.read();
            }
            FILE_NEW_LINE = newline;
            fileContents.add(line + "\n");
        }
        if (input2 != null) {
            input2.close();
        }
     
        if (input != null) {
            input.close();
        }
       
        return fileContents.toArray(new String[fileContents.size()]);
    }
    
    public ArrayList<String> getFileContentsArrayList(String fileContentsString) throws Exception {
        
        ArrayList<String> fileContents = new ArrayList<String>();
        
        BufferedReader input = new BufferedReader(new StringReader(fileContentsString));
        String line = "";

        while ((line = input.readLine()) != null)        
            fileContents.add(line);
        
        if (input != null) {
            input.close();
        }
       
        return fileContents;
    }

    public String[] convertFileToStringArray(File file) throws Exception {

        ArrayList<String> fileContents = new ArrayList<String>(500);

        File fp = new File(file.getAbsolutePath());
        FileReader reader = new FileReader(fp);
        BufferedReader input = new BufferedReader(reader);

        FileReader reader2 = new FileReader(fp);
        BufferedReader input2 = new BufferedReader(reader2);

        String line;
        input.mark(1024);

        while ((line = input.readLine()) != null) {
            input2.skip(line.length());
            input2.mark(1024);
            int c1 = input2.read();
            int c2 = input2.read();
            input2.reset();

            /* Determine the new line character being used in the file.. there were some errors in
             * processing due to carriage return characters.. this is required to handle new line characters
             * in the file formatting*/
            String newline = "\n";
            if (c1 == -1) {
                newline = "";
            } else if (c1 == '\n') {
                input2.read();
            } else if (c1 == '\r' && c2 != '\n') {
                newline = "\r";
                input2.read();
            } else if (c1 == '\r' && c2 == '\n') {
                newline = "\r\n";
                input2.read();
                input2.read();
            }
            FILE_NEW_LINE = newline;
            fileContents.add(line + "\n");
        }
        if (input2 != null) {
            input2.close();
        }
        if (reader2 != null) {
            reader2.close();
        }
        if (input != null) {
            input.close();
        }
        if (reader != null) {
            reader.close();
        }
        return fileContents.toArray(new String[fileContents.size()]);

    }

    public void removeAllComments() throws Exception {
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            throw new Exception("Exception while trying to convert file to string array " + m_file.getAbsolutePath(), ex);
        }

        String origFileContents = getFileContentsAsString(fileContents, false);


        try {
            fileWithoutCommentsReadOnly = removeSingleLineComments(origFileContents);
            fileWithoutCommentsReadOnly = removeMultilineComments(fileWithoutCommentsReadOnly);
        } catch (Exception ex) {
            throw new Exception("Exception while trying to remove comments from file: " + m_file.getAbsolutePath(), ex);
        }

    }

    /**
     * remove all single line comments. Maintain the original file line positions
     * @param fileContents
     * @return
     */
    public String removeSingleLineComments(String fileContents) {
        String singleLineCommentsRE = "//.*$";
        Pattern pattern = Pattern.compile(singleLineCommentsRE, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(fileContents);

        return matcher.replaceAll("");
    }

    /**
     * Remove  multiline comments preserving line numbering. We do not want to lose the line
     * numbering since it is becomes harder to fix the file. Hence we strip all the comments
     * and keep the line numbering as well.
     * @param fileContents
     * @return
     */
    public String removeMultilineComments(String fileContents) {
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
                String endAfterComment = fileContents.substring(end, indexTrailingnewLine);
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
    
    public String preserveComments() throws Exception{
        
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
            
        comments = new HashMap<String,String>();

        String origFileContents = getFileContentsAsString(fileContents, false);
        StringBuffer newFileContents = new StringBuffer();
        int commentNo = 0;
        
        //first remove comments like //*.. else it will be incorrectly interpretted as multiline comment
//        Pattern specialComment = Pattern.compile("//\\*.*$",Pattern.MULTILINE);
//        Matcher m1 = specialComment.matcher(origFileContents);
//        origFileContents = m1.replaceAll("");
        
        String singleLineCommentsRE = "//+\\*.*$";
        Pattern pattern = Pattern.compile(singleLineCommentsRE, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(origFileContents);
        int startPos = 0;
        
        while (matcher.find()) {
            commentNo++;
            String comment = matcher.group(0);
            int start = matcher.start();
            int end = matcher.end();

            String replacement = "<COMMENT_" + commentNo + ">";
            newFileContents.append(origFileContents.substring(startPos, start));
            newFileContents.append(replacement);
            startPos = end;
            comments.put(replacement,comment);
        }
        newFileContents.append(origFileContents.substring(startPos));
        
        String fileWithoutSingleComments = newFileContents.toString();
        
    //   return newFileContents.toString();

        /*anything*/
        // reluctant quantifier because we want to match the first occurrence of the closing tag as opposed to the last
        String multiLineCommentsRE = "/\\*.*?\\*/";
        pattern = Pattern.compile(multiLineCommentsRE, Pattern.DOTALL);
        matcher = pattern.matcher(fileWithoutSingleComments);    
        newFileContents = new StringBuffer();
        startPos = 0;
        while (matcher.find()) {
            commentNo++;
            String comment = matcher.group(0);
            int start = matcher.start();
            int end = matcher.end();

            String replacement = "<COMMENT_" + commentNo + ">";
            newFileContents.append(fileWithoutSingleComments.substring(startPos, start));
            newFileContents.append(replacement);
            startPos = end;
            comments.put(replacement,comment);
        }
        newFileContents.append(fileWithoutSingleComments.substring(startPos));
        
        return newFileContents.toString();        
       
    }

    private int countNumberOfNewLines(String str) {

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

    public boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    public String getFileContentsAsString(boolean removeBlankLines) throws Exception {
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            throw new Exception("Exception while trying to convert file to string array " + m_file.getAbsolutePath(), ex);
        }

        StringBuffer sb = new StringBuffer();
        int len = fileContents.length;
        for (int i = 0; i < len; i++) {
            String line = fileContents[i];
            if (removeBlankLines) {
                if (line.trim().equals("")) {
                    continue;
                }
            }
            sb.append(line);

        }
        return sb.toString();
    }
    
    public String getFileContentsAsString(ArrayList<String> fileContentsArray, boolean removeBlankLines) {
        StringBuffer sb = new StringBuffer();
        int len = fileContentsArray.size();
        for (int i = 0; i < len; i++) {
            String line = fileContentsArray.get(i);
            if (removeBlankLines) {
                if (line.trim().equals("")) {
                    continue;
                }
            }
            sb.append(line+"\n");

        }
        return sb.toString();
    }

    public String getFileContentsAsString(String[] fileContentsArray, boolean removeBlankLines) {
        StringBuffer sb = new StringBuffer();
        int len = fileContentsArray.length;
        for (int i = 0; i < len; i++) {
            String line = fileContentsArray[i];
            if (removeBlankLines) {
                if (line.trim().equals("")) {
                    continue;
                }
            }
            sb.append(line);

        }
        return sb.toString();
    }


    public int countNumberOfLinesInFile(String strFileContents) {

        if (isEmpty(strFileContents)) {
            return 0;
        }
        if (!strFileContents.contains("\n"))
            return 0;

        Pattern p = Pattern.compile("\\n");
        Matcher m = p.matcher(strFileContents);
        int cnt = 0;
        while (m.find()) {
            cnt++;
        }
        return cnt;
    }

    public int countNumberOfOccurences(String line, String regEx) {

        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(line);
        int cnt = 0;
        while (m.find()) {
            cnt++;
        }
        return cnt;

    }

    public int getLineNumberOfIndex(String contentsToSearch, int index) {
        int fileLen = contentsToSearch.length();
        if (index > fileLen - 1)
            return -1;

        String strBeforeIndex = contentsToSearch.substring(0, index);
        int lineNumber = countNumberOfNewLines(strBeforeIndex);

        // lineNumber gives the number of lines before this line. Hence this line is present on lineNumber +1
        return lineNumber + 1;
    }

    public String getLineAtLineNumber(int lineNumber) throws Exception {
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            throw new Exception("Exception while trying to convert file to string array " + m_file.getAbsolutePath(), ex);
        }
        if (lineNumber < 0)
            throw new Exception("[ERROR: getLineAtLineNumber] cannot retrive a negative line number");
        if (fileContents.length < lineNumber)
            throw new Exception("lineNumber is not present in file");
        return fileContents[lineNumber - 1];
    }


    public boolean setLineAtLineNumber(String line, int lineNumber) throws Exception {
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            throw new Exception("Exception while trying to convert file to string array " + m_file.getAbsolutePath(), ex);
        }

        if (fileContents.length < lineNumber)
            throw new Exception("lineNumber is not present in file");
        fileContents[lineNumber - 1] = line;
        return true;
    }

    public boolean addBeforeExistingLineAtNumber(String lineToAddBefore, int lineNumber,
                                                 boolean addPaddingOfExistingLine) throws Exception {
        String newLine = "";
        if (lineNumber < 0) {
            throw new Exception("[ERROR:addBeforeExistingLineAtNumber] Line number cannot be negative");
        }
        String existingLine = getLineAtLineNumber(lineNumber);
        String padding = "";
        if (addPaddingOfExistingLine) {
            padding = getWhiteSpacePrefix(existingLine);
        }

        newLine = padding + lineToAddBefore + FILE_NEW_LINE + existingLine;
        if (bDebug) {
            System.out.println("Old line: \n" +
                    existingLine);
            System.out.println("New line: \n" +
                    newLine);
        }
        setLineAtLineNumber(newLine, lineNumber);

        return true;
    }


    public String getWhiteSpacePrefix(String line) {
        Matcher m = JavaParsingRegEx.whiteSpacePrefixPattern.matcher(line);

        if (m.find())
            return line.substring(m.start(), m.end());

        return "";
    }

    public int getLineNumberOfNextNonBlankLine(int fromLineNumber) throws Exception {

        int nextBlankLineIndex = -1;
        int nextLineNumber = fromLineNumber + 1;
        while (true) {
            String line = getLineAtLineNumber(nextLineNumber);
            // method was empty
            if (!isEmpty(line) && line.trim().equals("}"))
                return -1;
            if (!isEmpty(line)) {
                break;
            }
            nextLineNumber++;
        }
        // incase of any errors. the nextLineNumber should never be equal to the fromLineNumber
        if (nextLineNumber == fromLineNumber)
            return -1;
        return nextLineNumber;


    }

    public int getLineNumberOfNextUncommentedNonBlankLine(int fromLineNumber) throws Exception {

        int nextBlankLineIndex = -1;
        int nextLineNumber = fromLineNumber + 1;
        while (true) {
            String line = getLineAtLineNumber(nextLineNumber);
            // method was empty
            if (!isEmpty(line) && line.trim().equals("}"))
                return -1;
            if (!isEmpty(line)) {
                break;
            }
            nextLineNumber++;
        }
        // incase of any errors. the nextLineNumber should never be equal to the fromLineNumber
        if (nextLineNumber == fromLineNumber)
            return -1;
        return nextLineNumber;


    }

    public String getNextNonCommentedLineAfterIndex(int index) throws Exception {
        String afterindex = fileWithoutCommentsReadOnly.substring(index);
        StringReader sreader = new StringReader(afterindex);
        BufferedReader breader = new BufferedReader(sreader);

        String line = breader.readLine();
        while (line != null) {
            if (!isEmpty(line))
                break;
            line = breader.readLine();
        }
        if (breader != null)
            breader.close();
        if (sreader != null)
            sreader.close();
        return line;
    }

    public Integer getNextNonCommentedLineNumberAfterIndex(int index) throws Exception {
        String beforeIndex = fileWithoutCommentsReadOnly.substring(0, index);
        int numberOfLinesBeforeIndex = countNumberOfOccurences(beforeIndex, "\\n");
        String afterindex = fileWithoutCommentsReadOnly.substring(index);
        StringReader sreader = new StringReader(afterindex);
        BufferedReader breader = new BufferedReader(sreader);
        int lineNumber = 0;
        String line = breader.readLine();
        while (line != null) {
            lineNumber++;
            if (!isEmpty(line))
                return numberOfLinesBeforeIndex + lineNumber;
            line = breader.readLine();
        }
        if (breader != null)
            breader.close();
        if (sreader != null)
            sreader.close();
        return -1;
    }

    public boolean addAppsLoggerImportIfRequred() throws Exception {
        if (m_appslogImportPresent)
            return true;
        if (doesOrigFileHaveAppsLoggerImport()) {
            m_appslogImportPresent = true;
            return true;
        }
        boolean addedImport = addImport("import oracle.apps.fnd.applcore.log.AppsLogger;");
        m_appslogImportPresent = addedImport;
        return addedImport;
    }

    public boolean addImport(String importToAdd) throws Exception {
        boolean added = false;
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            throw new Exception("Exception while trying to convert file to string array " + m_file.getAbsolutePath(), ex);
        }

        int lineNumberFirstImport = getLineNumberOfFirstImport();
        if (lineNumberFirstImport < 0) {
            // get the package declaration, and add the import declaration after that..
            int packageDeclLineNumber = getLineNumberOfPackageDecl();
            if (packageDeclLineNumber < 0) {
                throw new Exception("could not obtain line number of first import");
            }
            String origLine = getLineAtLineNumber(packageDeclLineNumber);
            if (isEmpty(origLine)) {
                throw new Exception("could not obtain line number of first import");

            } else {
                String modifiedLine = origLine + "\n" +
                    importToAdd;
                setLineAtLineNumber(modifiedLine, packageDeclLineNumber);
                added = true;
            }
        } else {

            return addBeforeExistingLineAtNumber(importToAdd, lineNumberFirstImport, true);
        }
        return added;
    }
    
    public String getAllImports( String fileContents) {
        
        Matcher importMatcher = JavaParsingRegEx.importPattern.matcher(fileContents);
        StringBuffer allImports = new StringBuffer();
        while(importMatcher.find())
            allImports.append(importMatcher.group(0) + "\n");
        
        return allImports.toString();
    }

    private int getLineNumberOfFirstImport() throws Exception {
        if (isEmpty(fileWithoutCommentsReadOnly)) {
            throw new Exception("Could not obtain file without comments");
        }
        Matcher importMatcher = JavaParsingRegEx.importPattern.matcher(fileWithoutCommentsReadOnly);
        if (importMatcher.find()) {
            int start = importMatcher.start();
            int lineNumber = getLineNumberOfIndex(fileWithoutCommentsReadOnly, start);

            return lineNumber;
        } else {
            return -1;
            //throw new Exception("[ERROR:getLineNumberOfFirstImport] No imports present in file: " + m_file.getAbsolutePath());
        }
    }

    private int getLineNumberOfPackageDecl() throws Exception {
        if (isEmpty(fileWithoutCommentsReadOnly)) {
            throw new Exception("Could not obtain file without comments");
        }
        Matcher packageMatcher = JavaParsingRegEx.packagePattern.matcher(fileWithoutCommentsReadOnly);
        if (packageMatcher.find()) {
            int start = packageMatcher.start();
            int lineNumber = getLineNumberOfIndex(fileWithoutCommentsReadOnly, start);

            return lineNumber;
        } else {
            return -1;
            //throw new Exception("[ERROR:getLineNumberOfPackageDecl] No imports present in file: " + m_file.getAbsolutePath());
        }

    }

    private boolean doesOrigFileHaveAppsLoggerImport() throws Exception {
        if (isEmpty(fileWithoutCommentsReadOnly)) {
            throw new Exception("Could not obtain file without comments");
        }
        Matcher appsLogMatcher = JavaParsingRegEx.importAppsLoggerPattern.matcher(fileWithoutCommentsReadOnly);
        if (appsLogMatcher.find())
            return true;

        return false;
    }

    public int findLineNumberOfEndMethod(int methodStartIndex) throws Exception {
        if (isEmpty(fileWithoutCommentsReadOnly)) {
            throw new Exception("Could not obtain file without comments");
        }

        int len = fileWithoutCommentsReadOnly.length();


        int numberOfOpenBr = 0;
        int numberOfCloseBr = 0;
        int i = 0;
        for (i = methodStartIndex + 2; i < len; i++) {
            if (fileWithoutCommentsReadOnly.charAt(i) == '{')
                numberOfOpenBr++;
            if (fileWithoutCommentsReadOnly.charAt(i) == '}') {
                numberOfCloseBr++;
                if (numberOfCloseBr > numberOfOpenBr)
                    break;
            }

        }

        int linenumber = getLineNumberOfIndex(fileWithoutCommentsReadOnly, i);
        String line = getLineAtLineNumber(linenumber);
        String methodDecl = fileWithoutCommentsReadOnly.substring(methodStartIndex, i);
        return linenumber;

    }

    public String getSingleLineCommentJustBeforeLine(int lineNumber) throws Exception {
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            throw new Exception("Exception while trying to convert file to string array " + m_file.getAbsolutePath(), ex);
        }

        for (int i = lineNumber - 2; i > 0; i--) {
            if (fileContents.length <= i - 1)
                return null;

            if (fileContents[i].trim().startsWith("//"))
                return fileContents[i];
            else if (!fileContents[i].trim().equals("")) {
                return null;
            }
        }
        return null;
    }

    public Integer getLineNumberOfSingleLineCommentJustBeforeLine(int lineNumber) throws Exception {
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            throw new Exception("Exception while trying to convert file to string array " + m_file.getAbsolutePath(), ex);
        }

        for (int i = lineNumber - 2; i > 0; i--) {
            if (fileContents.length <= i - 1)
                return null;

            if (fileContents[i].trim().startsWith("//"))
                return i + 1; // line number is always one greater than index
            else if (!fileContents[i].trim().equals("")) {
                return null;
            }
        }
        return null;
    }

    public String getMethodBodyFromUncommentedFile(int startIndexOfMethodDeclaration) throws Exception {
        StringBuffer sb = new StringBuffer();
        int cntOpenBrackets = 0;
        int cntClosedBrackets = 0;
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            throw new Exception("Exception while trying to convert file to string array " + m_file.getAbsolutePath(), ex);
        }
        int fileLength = fileContents.length;
        boolean startOfMethodFound = false;
        for (int i = startIndexOfMethodDeclaration; i < fileLength; i++) {
            String line = fileContents[i - 1];
            int numberOfOpenBrackets = countNumberOfOccurences(line, "\\{");
            if (!startOfMethodFound) {
                if (numberOfOpenBrackets > 0) {
                    startOfMethodFound = true;
                    numberOfOpenBrackets = numberOfOpenBrackets - 1;
                    int indexOpBr = line.indexOf("{");
                    line = line.substring(indexOpBr);
                } else {
                    continue;
                }
            }
            int numberOfClosedBrackets = countNumberOfOccurences(line, "}");
            cntOpenBrackets = cntOpenBrackets + numberOfOpenBrackets;
            cntClosedBrackets = cntClosedBrackets + numberOfClosedBrackets;
            int numberOfUnmatchedClosedBrackets = cntClosedBrackets - cntOpenBrackets;
            if (numberOfUnmatchedClosedBrackets <= 0) {
                sb.append(line);
            } else if (numberOfUnmatchedClosedBrackets == 1) {
                sb.append(line);
                return sb.toString();
            } else if (numberOfUnmatchedClosedBrackets > 1) {
                int indexOfCloseBrkNeeded = 0;
                while (numberOfUnmatchedClosedBrackets != 1) {
                    indexOfCloseBrkNeeded = line.lastIndexOf("}");
                    numberOfUnmatchedClosedBrackets--;
                }
                sb.append(line.substring(0, indexOfCloseBrkNeeded));
                return sb.toString();
            }
        }
        return sb.toString();
    }

    public String getContentsBetweenLineNumbers(Integer startLineNumber, Integer endLineNumber) throws Exception {
        StringBuffer contents = new StringBuffer();
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            throw new Exception("Exception while trying to convert file to string array " + m_file.getAbsolutePath(), ex);
        }
        for (int i = startLineNumber; i <= endLineNumber; i++) {
            contents.append(getLineAtLineNumber(i));
        }
        return contents.toString();
    }

    public Integer getLineNumberOfEndMethod(int lineNumberStartMethod) throws Exception {

        int cntOpenBrackets = 0;
        int cntClosedBrackets = 0;
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            throw new Exception("Exception while trying to convert file to string array " + m_file.getAbsolutePath(), ex);
        }
        int fileLength = fileContents.length;
        boolean startOfMethodFound = false;
        int endLineNumber = -1;
        for (int i = lineNumberStartMethod; i < fileLength; i++) {
            String line = fileContents[i - 1];
            int numberOfOpenBrackets = countNumberOfOccurences(line, "\\{");
            if (!startOfMethodFound) {
                if (numberOfOpenBrackets > 0) {
                    startOfMethodFound = true;
                    cntOpenBrackets = numberOfOpenBrackets - 1;
                    int indexOpBr = line.indexOf("{");
                    line = line.substring(indexOpBr);
                } else {
                    continue;
                }
            }
            int numberOfClosedBrackets = countNumberOfOccurences(line, "}");
            cntOpenBrackets = cntOpenBrackets + numberOfOpenBrackets;
            cntClosedBrackets = cntClosedBrackets + numberOfClosedBrackets;
            int numberOfUnmatchedClosedBrackets = numberOfClosedBrackets - numberOfOpenBrackets;
            if (numberOfUnmatchedClosedBrackets <= 0) {

            } else if (numberOfUnmatchedClosedBrackets == 1) {

                endLineNumber = i;
                break;
            } else if (numberOfUnmatchedClosedBrackets > 1) {
                endLineNumber = i;
                break;
            }
        }
        return endLineNumber;
    }

    public Integer getIndexAtEndLineNumberOfEndMethod(int lineNumberStartMethod) throws Exception {

        int cntOpenBrackets = 0;
        int cntClosedBrackets = 0;
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            throw new Exception("Exception while trying to convert file to string array " + m_file.getAbsolutePath(), ex);
        }
        int fileLength = fileContents.length;
        boolean startOfMethodFound = false;
        int endLineNumberIndexOfCloseBracket = -1;
        for (int i = lineNumberStartMethod; i < fileLength; i++) {
            String line = fileContents[i - 1];
            int numberOfOpenBrackets = countNumberOfOccurences(line, "\\{");
            if (!startOfMethodFound) {
                if (numberOfOpenBrackets > 0) {
                    startOfMethodFound = true;
                    cntOpenBrackets = numberOfOpenBrackets - 1;
                    int indexOpBr = line.indexOf("{");
                    line = line.substring(indexOpBr);
                } else {
                    continue;
                }
            }
            int numberOfClosedBrackets = countNumberOfOccurences(line, "}");
            cntOpenBrackets = cntOpenBrackets + numberOfOpenBrackets;
            cntClosedBrackets = cntClosedBrackets + numberOfClosedBrackets;
            int numberOfUnmatchedClosedBrackets = numberOfClosedBrackets - numberOfOpenBrackets;
            if (numberOfUnmatchedClosedBrackets <= 0) {

            } else if (numberOfUnmatchedClosedBrackets == 1) {

                endLineNumberIndexOfCloseBracket = line.lastIndexOf("}");
                break;
            } else if (numberOfUnmatchedClosedBrackets > 1) {
                int indexOfCloseBrkNeeded = 0;
                while (numberOfUnmatchedClosedBrackets != 1) {
                    indexOfCloseBrkNeeded = line.lastIndexOf("}");
                    numberOfUnmatchedClosedBrackets--;
                }
                endLineNumberIndexOfCloseBracket = indexOfCloseBrkNeeded;

                break;
            }
        }
        return endLineNumberIndexOfCloseBracket;
    }
}


