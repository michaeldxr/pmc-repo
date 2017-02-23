package oracle.apps.helpers.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public  class GenericFileParserHelper {
    public static final String identifier = "[0-9a-zA-Z_]+";
    public static final String requiredWhiteSpace = "[\\s]+";
    public static final String whiteSpace = "[\\s]*";
    
    protected String[] fileContents = null;
    protected String fileWithoutCommentsReadOnly = null;
    protected File m_file = null;

    public GenericFileParserHelper(String filePath) throws Exception {
        super();
        File f = new File(filePath);
        if (!f.exists()) {
            throw new Exception("File does not exist:" + filePath);
        }
        m_file = f;
        removeAllComments();
    }

    public String getFileWithoutCommentsReadOnly() throws Exception {
        if (isEmpty(fileWithoutCommentsReadOnly)) {
            removeAllComments();
        }
        return fileWithoutCommentsReadOnly;
    }
    
    /**
     * Gets the file contents as an array of strings
     * @return
     */
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
    

    public static GenericFileParserHelper createAppropriateFileParser(String filePath) throws Exception {
        File f = new File(filePath);
        if (!f.exists()) {
            throw new Exception("File does not exist:" + filePath);
        }
        String extension = getExtension(filePath);
        if (isEmpty(extension))
            throw new Exception("could not obtain the extension for file: " + filePath);
        extension = extension.trim();
        if (extension.equals("java")) {
            throw new Exception("TODO: Not supported");
        } else if (extension.equals("xml")) {
            throw new Exception("TODO: Not supported");
        } else if (extension.equals("pkb")) {
            throw new Exception("TODO: Not supported");
        }
        return null;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    public static String getExtension(String path) {
        if (path == null)
            return "";
        int extIndex = path.lastIndexOf(".");
        if (extIndex == -1)
            return "";
        return path.substring(extIndex + 1);
    }

    public void removeAllComments() throws Exception {
    }


/*
 * =============================================================
 * PRIVATE HELPER FUNCTIONS
 * =============================================================
*/
    private String[] convertFileToStringArray(File file) throws Exception {

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
            // FILE_NEW_LINE = newline;
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
    /**
     * remove all single line comments. Maintain the original file line positions
     * @param fileContents
     * @return
     */
    public String removeSingleLineComments(String fileContents, String regEx) {
        //String singleLineCommentsRE = "//.*$";
        Pattern pattern = Pattern.compile(regEx, Pattern.MULTILINE);
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
    public String removeMultilineComments(String fileContents, String regEx) {
        StringBuffer newFileContents = new StringBuffer();


        /*anything*/
        // reluctant quantifier because we want to match the first occurrence of the closing tag as opposed to the last
        String multiLineCommentsRE = "/\\*.*?\\*/";
        Pattern pattern = Pattern.compile(regEx, Pattern.DOTALL);
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

    
}
