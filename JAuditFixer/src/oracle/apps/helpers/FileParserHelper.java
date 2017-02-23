package oracle.apps.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileParserHelper {

    private String[] fileContents = null;
    private String fileWithoutCommentsReadOnly = null;
    private File m_file = null;

    public FileParserHelper(String absFilePath) throws Exception {
        super();
        File f = new File(absFilePath);
        if (!f.exists()) {
            throw new Exception("File does not exist:" + absFilePath);
        }
        m_file = f;

    }

    public String getFileWithoutCommentsReadOnly() throws Exception {
        if (isEmpty(fileWithoutCommentsReadOnly)) {
            removeAllComments();
        }
        return fileWithoutCommentsReadOnly;
    }

    public static String getExtension(String path) {
        if (path==null)
            return "";
        int extIndex = path.lastIndexOf(".");
        if (extIndex == -1)
            return "";
        return path.substring(extIndex + 1);
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


    // Converts the file to a string array

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

    public void removeAllComments() throws Exception {
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            throw new Exception("Exception while trying to convert file to string array " + m_file.getAbsolutePath(), ex);
        }

        String origFileContents = getFileContentsAsString(fileContents, false);
        String extension = getExtension(m_file.getAbsolutePath());
        if (isEmpty(extension))
            throw new Exception("Invalid type: " + extension);


        try {

            if (extension.equals("java")) {
                String multiLineCommentsRE = "/\\*.*?\\*/";
                fileWithoutCommentsReadOnly = removeSingleLineComments(origFileContents);
                fileWithoutCommentsReadOnly = removeMultilineComments(fileWithoutCommentsReadOnly, multiLineCommentsRE);

            } else if (extension.equals("xml")) {
                String multiLineCommentsRE = "<!--.*?-->";
                fileWithoutCommentsReadOnly = removeSingleLineComments(origFileContents);
                fileWithoutCommentsReadOnly = removeMultilineComments(fileWithoutCommentsReadOnly, multiLineCommentsRE);
            }


        } catch (Exception ex) {
            throw new Exception("Exception while trying to remove comments from file: " + m_file.getAbsolutePath(), ex);
        }

    }

    public void removeAllComments(String extensionType) throws Exception {
        try {
            if (fileContents == null) {
                fileContents = convertFileToStringArray(m_file);
            }
        } catch (Exception ex) {
            throw new Exception("Exception while trying to convert file to string array " + m_file.getAbsolutePath(), ex);
        }

        String origFileContents = getFileContentsAsString(fileContents, false);
      


        try {

            if (extensionType.equals("java")) {
                String multiLineCommentsRE = "/\\*.*?\\*/";
                fileWithoutCommentsReadOnly = removeSingleLineComments(origFileContents);
                fileWithoutCommentsReadOnly = removeMultilineComments(fileWithoutCommentsReadOnly, multiLineCommentsRE);

            } else if (extensionType.equals("xml")) {
                String multiLineCommentsRE = "<!--.*?-->";
                fileWithoutCommentsReadOnly = removeSingleLineComments(origFileContents);
                fileWithoutCommentsReadOnly = removeMultilineComments(fileWithoutCommentsReadOnly, multiLineCommentsRE);
            }
            else{
                throw new Exception("Unsupported type: " + extensionType);
                }


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

    public static String getFileContentsAsString(String f) throws Exception {
        StringBuffer fileContents = new StringBuffer();

        BufferedReader input = new BufferedReader(new FileReader(f));
        String line;

        while ((line = input.readLine()) != null)       
            fileContents.append(line + "\n");        
        
        if (input != null) 
            input.close();
        
        return fileContents.toString();
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

    public boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

}
