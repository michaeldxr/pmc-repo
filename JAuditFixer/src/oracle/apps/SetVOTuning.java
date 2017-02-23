package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.File;
import java.io.FileReader;

import java.io.FileWriter;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import java.util.Iterator;
import java.util.Set;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SetVOTuning {

    /**
     * Writer for diff.txt
     */
    private BufferedWriter diff;

    /**
     * Number of VO files looked at
     */
    static int m_voFilesLookedAt = 0;

    /**
     * Number of VO files fixed
     */
    static int m_voFilesFixed = 0;

    /**
     * Number of files skipped due to fetch size being already set by the developer
     */
    static int m_numberFetchSizeExpSet = 0;

    /**
     * DataStructure for csv data. Contains a map of all the vo details
     */
    static HashMap<String, VODetails> map_VoDetails =
        new HashMap<String, VODetails>();

    /**
     * StringBuilder for any info messages that need to be shown after the run
     */
    private StringBuilder sbInfoMessages = new StringBuilder();

    /**
     * Name of diff file
     */
    final static String diffFileName = "diff.txt";


    public SetVOTuning() {
        super();
    }

    /**
     * Usage: <path to the code> <path to csv file> <do ade operations:boolean (e.g. true/false)>
     * @param args
     * args[0] = path to the code
     * args[1] = path to csv file
     * args[2] = do ade operations. If in debug mode and do not want to do checkin/checkout set to false. else true.
     */
    public static void main(String[] args) {
        /* Usage: SetVoTuning <path to code> <path to csv file> <do ade operations>*/
        Date startDate = new Date();

        if (args.length != 3 || isEmpty(args[0]) || isEmpty(args[1]) ||
            isEmpty(args[2])) {
            System.out.println("Usage:  setVoTuning.sh <path to your code in /ade_autofs> <csv file name, including path> <doADE>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        String sCsvFile = args[1];
        Boolean bDoADE = new Boolean(args[2]);
        System.out.println("----------Input Parameters --------------");
        System.out.println("Analyzing Directory " + sCrawlDir);
        System.out.println("Csv file location " + sCsvFile);
        System.out.println("------------------------------------------");

        SetVOTuning setVOTuning = new SetVOTuning();

        /* Parse csv file*/
        try {
            map_VoDetails = setVOTuning.parseCsvFile(sCsvFile);
            if (map_VoDetails == null) {
                System.out.println("Error in parsing csv file");
                System.exit(1);
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            System.exit(1);
        }
        System.out.println("[INFO] Based on CSV File total number of files to be processed: " +
                           map_VoDetails.size());


        try {
            if (bDoADE.booleanValue()) {
                // Remove any existing diff.txt. Create a new bufferred writer for diff.txt
                setVOTuning.startDiffLog();
            }

            /**
             * Recursevely scan the directory - Fix any vo file that needs fixing (file that needs fixing obtained after from csv parse)
             */
            System.out.println("====================Analyzing and fixing VO files  ===================== ");
            setVOTuning.crawlDirectory(sCrawlDir, bDoADE, map_VoDetails);
            System.out.println("======================================================================= ");

            if (bDoADE.booleanValue()) {
                setVOTuning.closeDiffLog();
            }


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
        System.out.println("Total Number of vo files looked at: " +
                           m_voFilesLookedAt);
        System.out.println("Total Number of vo files fixed fixed: " +
                           m_voFilesFixed);
        System.out.println("Total of vo files that had fetch size explictly set: (files were skipped) " +
                           m_numberFetchSizeExpSet);
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
     *Parses the csv file and generates appropriate datastructure for vo details.
     * Also performs validation of datastructure.
     * If there are any validation errors - returns null..
     * @param csvFilePath
     * @return
     * @throws Exception
     */
    public HashMap<String, VODetails> parseCsvFile(String csvFilePath) throws Exception {

        int lineNum = 0;
        StringBuffer sbErrors = new StringBuffer();
        HashMap<String, VODetails> map_components =
            new HashMap<String, VODetails>(100);
        File file = new File(csvFilePath);
        FileReader reader = new FileReader(file);
        BufferedReader input = new BufferedReader(reader);
        String firstLine = input.readLine();
        lineNum++;
        if (firstLine == null) {
            System.out.println("File is empty!!");
            return null;
        }
        String line;
        while ((line = input.readLine()) != null) {
            lineNum++;
            if (isEmpty(line)) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length < 4) {
                continue;
            }
            String filePath = parts[0].trim();
            String strMaxFetchSize = parts[1].trim();
            String strFetchSize = parts[2].trim();
            String strFetchMode = parts[3].trim();
            if (isEmpty(filePath) || isEmpty(strMaxFetchSize) ||
                isEmpty(strFetchSize) || isEmpty(strFetchMode)) {
                continue;
            }
            if (!filePath.startsWith("fin/")) {
                System.out.println("[Error while parsing csv] file path[" +
                                   filePath +
                                   "] should start with 'fin/'.. skipping this entry");
                sbErrors.append("Error line: " + lineNum);
                sbErrors.append("filePath should start with 'fin/'.. skipping this line" +
                                " \n");
                continue;
            }
            Integer maxFetchSize = -1;
            if (!strMaxFetchSize.contains("-1:")) {
                try {
                    maxFetchSize = Integer.valueOf(strMaxFetchSize);
                } catch (NumberFormatException ex) {

                    sbErrors.append("Error line: " + lineNum);
                    sbErrors.append("invalid max fetch size (could not convert to number) :" +
                                    strMaxFetchSize + " \n");
                }
            }
            Integer fetchSize = 1;
            try {
                fetchSize = Integer.valueOf(strFetchSize);
            } catch (NumberFormatException ex) {
                sbErrors.append("Error line: " + lineNum);
                sbErrors.append("invalid fetch size (could not convert to number): " +
                                strFetchSize + " \n");
            }

            if (!strFetchMode.trim().equals("FETCH_ALL") &&
                !strFetchMode.trim().equals("FETCH_AS_NEEDED")) {
                System.out.println("Invalid fetch  mode");
                sbErrors.append("Error line: " + lineNum +
                                " Invalid fetch  mode: " + strFetchMode +
                                " \n");
            }
            VODetails voDetails = new VODetails();
            voDetails.setFetchMode(strFetchMode);
            voDetails.setFetchSize(fetchSize);
            voDetails.setMaxFetchSize(maxFetchSize);
            voDetails.setVoFile(filePath);
            if (map_components.containsKey(filePath)) {
                System.out.println("Error.. duplicate entry: " + filePath);
                continue;
            }
            map_components.put(filePath, voDetails);
        }
        if (input != null) {
            input.close();
        }
        if (reader != null) {
            reader.close();
        }

        String strErrors = sbErrors.toString();
        if (!isEmpty(strErrors)) {
            System.out.println("Error while validating CSV");
            System.out.println("Errors are: " + strErrors);
            return null;
        }
        return map_components;
    }

    /**
     * recurse over the crawl directory and for each valid ui file - process it.
     * Valid directories -
     * - should not contain, /dbSchema/ or /model/ or /publicModel/ or /test/ or /classes/
     * Valid UI files
     * - jspx or jsff extensions
     * - and not in folder with /classes/ or /test/ or /model/ or /publicModel/
     * @param path
     * @param bDoADE
     */
    private void crawlDirectory(String path, Boolean bDoADE,
                                HashMap<String, VODetails> csvParsedComponents) {
        if (path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();

                String formattedPath = getFusionappsIndexedPath(absFilePath);
                if (csvParsedComponents.containsKey(formattedPath)) {
                    System.out.println("Processing file " +
                                       listOfFiles[i].getName());

                    VODetails voDetails =
                        csvParsedComponents.get(formattedPath);
                    m_voFilesLookedAt++;
                    processVOFile(listOfFiles[i], bDoADE, voDetails);


                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                crawlDirectory(dir, bDoADE, csvParsedComponents);
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    public void processVOFile(File file, Boolean bDoADE, VODetails voDetails) {
        DOMParser parser = null;
        String absFilePath = file.getAbsolutePath();
        int endTagLineNumber = 0;
        boolean fixMade = false;

        final String STR_MAX_FETCHSIZE = "MaxFetchSize=\"";
        final String STR_FETCHSIZE = "FetchSize=\"";
        final String STR_FETCHMODE = "FetchMode=\"";


        try {
            parser = initializeDomParser();
            parser.parse("file:" + absFilePath);
        } catch (Exception ex) {
            System.out.println("Error while initializing dom parser for file.. Skipping " +
                               absFilePath);
            return;
        }

        XMLDocument doc = parser.getDocument();
        NodeList voTag = doc.getElementsByTagName("ViewObject");
        if (voTag == null || voTag.getLength() != 1) {
            System.out.println("Error processing file:  " +
                               file.getAbsolutePath());
            System.out.println("Was expecting to find exactely 1 ViewObject tag in the file.");
            System.out.println("Skipping....");
            sbInfoMessages.append("File: " + absFilePath +
                                  " not processed because only 1 ViewObject tag expected in the file");
            return;
        }
        Node voNode = voTag.item(0);
        endTagLineNumber = getLineNumberInSourceFile(voNode);
        Integer columnNum = getColumnNumberInSourceFile(voNode);
        // set the mode to FETCH_AS_NEEDED if maxFetchSize==1 or 0.
        if (voDetails.maxFetchSize == 0 || voDetails.maxFetchSize == 1) {
            voDetails.fetchMode = "FETCH_AS_NEEDED";
        }
        String[] fileContents;
        try {
            fileContents = convertFileToStringArray(file);
            if (fileContents == null) {
                System.out.println("file contents null when trying to convert to array..  " +
                                   absFilePath);
                return;
            }
        } catch (Exception ex) {
            System.out.println("Exception while trying to convert file to array: " +
                               absFilePath);
            sbInfoMessages.append("Exception while trying to convert file to array: " +
                                  absFilePath);
            return;
        }

        /**
         * Read the file contents line by line and find if it already contains the tags maxFetchSize, FetchSize and FetchMode.
         * if it does - make note of the line numbers on which the tags are present
         */
        int maxFetchSizeLineNumber = -1;
        int fetchSizeLineNumber = -1;
        int fetchModeLineNumber = -1;
        for (int i = 0; i < endTagLineNumber; i++) {

            String line = fileContents[i];
            if (line.contains(STR_MAX_FETCHSIZE)) {
                maxFetchSizeLineNumber = i;
            }
            if (line.contains(STR_FETCHSIZE)) {
                boolean fetchSizeFound = false;
                int fetchSizeIndex = line.indexOf(STR_FETCHSIZE);
                if (fetchSizeIndex == 0) {
                    fetchSizeLineNumber = i;
                    fetchSizeFound = true;
                } else if (fetchSizeIndex > 0) {
                    Character charbefore = line.charAt(fetchSizeIndex - 1);
                    if (Character.isWhitespace(charbefore)) {
                        fetchSizeFound = true;
                        fetchSizeLineNumber = i;
                    }
                }

            }
            if (line.contains(STR_FETCHMODE)) {
                fetchModeLineNumber = i;
            }

        }

        /**
         * Make fixes.
         * if maxFetchSize to be set ==1, (default is -1)
         *   - if file contains a maxFetchSize tag - remove it
         * else if maxFetchSize > 0
         *   - if file contains a maxFetchSize tag - modify the existing tag to reflect the new maxFetchSize to be set
         *   - else add the maxFetchSize tag to the end of the ViewObject tag
         *
         * if FetchSize to be set >0
         *  - if file contains the FetchSize tag - skip the file.. we do not want to modify files that already have the tag set. Count the number of such files
         *  - if file does not contain the tag - add the tag at the end of the ViewObject tag
         *
         * if FetchMode !=N/A && is either FETCH_ALL or FETCH_AS_NEEDED
         *  - if file contains tag - modify the tag
         *  - if it does not contain tag
         *      - if FetchMode != FETCH_AS_NEEDED - add tag at the end of the ViewObject tag (do not want to be adding unnecessary tags so we add the tag only if it is not the default option)
         *
         */
        if (voDetails.maxFetchSize == -1) {
            if (maxFetchSizeLineNumber > 0) {
                String line = fileContents[maxFetchSizeLineNumber];
                String modifiedLine = removeAttribute(line, STR_MAX_FETCHSIZE);
                if (!line.equals(modifiedLine)) {
                    System.out.println("** Fixing line.. ");
                    fixMade = true;
                    fileContents[maxFetchSizeLineNumber] = modifiedLine;
                    if (!bDoADE) {
                        System.out.println("Original Line: " + line);
                        System.out.println("Modified Line: " + modifiedLine);
                    }
                }
            }
        } else if (voDetails.maxFetchSize >= 0) {
            if (maxFetchSizeLineNumber >= 0) {
                String line = fileContents[maxFetchSizeLineNumber];
                String modifiedLine =
                    modifyAttribute(line, STR_MAX_FETCHSIZE, voDetails.maxFetchSize.toString());
                if (!line.equals(modifiedLine)) {
                    System.out.println("** Fixing line.. ");
                    fixMade = true;
                    fileContents[maxFetchSizeLineNumber] = modifiedLine;
                    if (!bDoADE) {
                        System.out.println("Original Line: " + line);
                        System.out.println("Modified Line: " + modifiedLine);
                    }
                }

            } else {
                // needs to add line MaxFetchSize="<value>"
                String lineToAdd =
                    " " + STR_MAX_FETCHSIZE + voDetails.maxFetchSize.toString() +
                    "\"";
                String originalLine = fileContents[endTagLineNumber - 1];
                String modifiedLine = originalLine;
                if (originalLine.length() >= columnNum) {
                    String beforeEndTag =
                        originalLine.substring(0, columnNum - 2);
                    String after = originalLine.substring(columnNum - 2);
                    modifiedLine = beforeEndTag + lineToAdd + after;
                } else {
                    System.out.println("[ERROR] cannot fix line: " +
                                       originalLine + " in file: " +
                                       absFilePath);
                }
                if (!originalLine.equals(modifiedLine)) {
                    System.out.println("** Fixing line.. ");
                    fixMade = true;
                    fileContents[endTagLineNumber - 1] = modifiedLine;
                    if (!bDoADE) {
                        System.out.println("Original Line: " + originalLine);
                        System.out.println("Modified Line: " + modifiedLine);
                    }
                }
            }
        }

        if (voDetails.fetchSize >= 0) {
            if (fetchSizeLineNumber >= 0) {
                m_numberFetchSizeExpSet++;
                sbInfoMessages.append("FileName: " + absFilePath +
                                      " has fetch size explicitly set.. ");
                sbInfoMessages.append("Line is: " +
                                      fileContents[fetchSizeLineNumber] +
                                      " \n");

                /*String line = fileContents[fetchSizeLineNumber];
                String modifiedLine =
                    modifyAttribute(line, STR_FETCHSIZE, voDetails.fetchSize.toString());
                if (!line.equals(modifiedLine)) {
                    System.out.println("** Fixing line.. ");
                    fixMade = true;
                    fileContents[fetchSizeLineNumber] = modifiedLine;
                    if (!bDoADE) {
                        System.out.println("Original Line: " + line);
                        System.out.println("Modified Line: " + modifiedLine);
                    }
                }*/
            } else {
                // add only if it is not default
                if (voDetails.fetchSize != 1) {
                    // needs to add line MaxFetchSize="<value>" before the end tag
                    String lineToAdd =
                        " " + STR_FETCHSIZE + voDetails.fetchSize.toString() +
                        "\"";
                    String originalLine = fileContents[endTagLineNumber - 1];
                    String modifiedLine = originalLine;
                    if (originalLine.length() >= columnNum) {
                        String beforeEndTag =
                            originalLine.substring(0, columnNum - 2);
                        String after = originalLine.substring(columnNum - 2);
                        modifiedLine = beforeEndTag + lineToAdd + after;
                    } else {
                        System.out.println("[ERROR] cannot fix line: " +
                                           originalLine + " in file: " +
                                           absFilePath);
                    }


                    if (!originalLine.equals(modifiedLine)) {
                        System.out.println("** Fixing line.. ");
                        fixMade = true;
                        fileContents[endTagLineNumber - 1] = modifiedLine;
                        if (!bDoADE) {
                            System.out.println("Original Line: " +
                                               originalLine);
                            System.out.println("Modified Line: " +
                                               modifiedLine);


                        }
                    }
                }
            }
        } else {
            System.out.println("Invalid fetch size entered: " +
                               voDetails.fetchSize);
        }

        if (voDetails.fetchMode != "N/A") {
            if (voDetails.fetchMode.equals("FETCH_AS_NEEDED") ||
                voDetails.fetchMode.equals("FETCH_ALL")) {
                if (fetchModeLineNumber >= 0) {
                    String line = fileContents[fetchModeLineNumber];
                    String modifiedLine =
                        modifyAttribute(line, STR_FETCHMODE, voDetails.fetchMode);
                    if (!line.equals(modifiedLine)) {
                        System.out.println("** Fixing line.. ");
                        fixMade = true;
                        fileContents[fetchModeLineNumber] = modifiedLine;
                        if (!bDoADE) {
                            System.out.println("Original Line: " + line);
                            System.out.println("Modified Line: " +
                                               modifiedLine);
                        }
                    }
                } else {
                    // add only if it is not the default. Does not make sense adding in the value that is default
                    if (!voDetails.fetchMode.equals("FETCH_AS_NEEDED")) {
                        // needs to add line MaxFetchSize="<value>"
                        String lineToAdd =
                            " " + STR_FETCHMODE + voDetails.fetchMode.toString() +
                            "\"";
                        String originalLine =
                            fileContents[endTagLineNumber - 1];
                        String modifiedLine = originalLine;
                        if (originalLine.length() >= columnNum) {
                            String beforeEndTag =
                                originalLine.substring(0, columnNum - 2);
                            String after =
                                originalLine.substring(columnNum - 2);
                            modifiedLine = beforeEndTag + lineToAdd + after;
                        } else {
                            System.out.println("[ERROR] cannot fix line: " +
                                               originalLine + " in file: " +
                                               absFilePath);
                        }
                        if (!originalLine.equals(modifiedLine)) {
                            System.out.println("** Fixing line.. ");
                            fixMade = true;
                            fileContents[endTagLineNumber - 1] = modifiedLine;
                            if (!bDoADE) {
                                System.out.println("Original Line: " +
                                                   originalLine);
                                System.out.println("Modified Line: " +
                                                   modifiedLine);
                            }
                        }
                    }
                }
            } else {
                System.out.println("Invalid fetch Mode specified");
            }
        }
        String newFileContents = convertStringArrayToString(fileContents);
        try {
            if (bDoADE) {
                applyFix(file, newFileContents);
            }
        } catch (Exception ex) {
            System.out.println("Exception occurred while apply fix for file: " +
                               file.getAbsolutePath());
        }
        if (fixMade) {
            m_voFilesFixed++;
        }

    }

    public void applyFix(File file, String newFileContents) throws Exception {
        checkoutAndDelete(file);

        FileWriter fw = new FileWriter(file.getAbsolutePath());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(newFileContents);
        bw.close();

        checkinAndDiff(file);
    }

    private String convertStringArrayToString(String[] fileContents) {
        StringBuffer sb = new StringBuffer();
        int len = fileContents.length;
        for (int i = 0; i < len; i++) {
            sb.append(fileContents[i]);
        }
        return sb.toString();
    }

    private String removeAttribute(String originalLine,
                                   String attributeToRemove) {
        String newLine = "";
        if (originalLine.contains(attributeToRemove)) {
            int startIndex = originalLine.indexOf(attributeToRemove);
            int endIndex = originalLine.indexOf("\"");
            newLine = originalLine.substring(0, startIndex);
            newLine = newLine + " ";
            newLine = newLine + originalLine.substring(endIndex);
        }
        return newLine;
    }

    private String modifyAttribute(String originalLine,
                                   String attributeToModify, String newValue) {
        String newLine = "";
        if (originalLine.contains(attributeToModify)) {
            int startIndex = originalLine.indexOf(attributeToModify);
            int endIndex =
                originalLine.indexOf("\"", startIndex + attributeToModify.length());
            newLine = originalLine.substring(0, startIndex);
            newLine = newLine + attributeToModify;
            newLine = newLine + newValue;
            newLine = newLine + originalLine.substring(endIndex);
        }
        return newLine;
    }

    /**
     * Gets the column number of the end of the start tag in the soruce file
     * @param componentNode
     * ComponentNode whose column number is to be found
     * @return
     * Returns the column number in the source file
     */
    public Integer getColumnNumberInSourceFile(Node componentNode) {
        if (componentNode instanceof XMLElement) {
            XMLElement xmlComponentNode = (XMLElement)componentNode;
            Integer columnNumber = xmlComponentNode.getColumnNumber();
            return columnNumber;
        }

        return 0;
    }

    /**
     * Gets the line number of the end of the start tag in the source file
     * @param componentNode
     * componentNode whose line number is to be found
     * @return
     * returns the line number in the source file
     */
    public Integer getLineNumberInSourceFile(Node componentNode) {
        if (componentNode instanceof XMLElement) {
            XMLElement xmlComponentNode = (XMLElement)componentNode;
            Integer lineNum = xmlComponentNode.getLineNumber();
            return lineNum;
        }

        return 0;
    }

    /**
     * Reads the entire file, line by line, and converts the contents of the file into  a String Array.
     * @param file
     * file to be read
     * @return
     * String[] representation of the file
     * @throws Exception
     */
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
            fileContents.add(line + newline);
        }
        if (input != null) {
            input.close();
        }
        if (input2 != null) {
            input2.close();
        }
        if (reader != null) {
            reader.close();
        }
        if (reader2 != null) {
            reader2.close();
        }
        return fileContents.toArray(new String[fileContents.size()]);

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

    /* ===========================================================
    *  ADE Related Functions Below
    * ===========================================================
    * */

    private void checkinAndDiff(File f) throws Exception {

        Process ciProcess =
            Runtime.getRuntime().exec("ade ci " + f.getAbsolutePath());
        checkProcess(ciProcess, f, "checkin", false);

        String diffCmd = "ade diff -label " + f.getAbsolutePath();
        Process diffProcess = Runtime.getRuntime().exec(diffCmd);
        checkProcess(diffProcess, f, "diff file with previous in label", true);
    }

    private void checkoutAndDelete(File f) throws Exception {

        Process coProcess =
            Runtime.getRuntime().exec("ade co -nc " + f.getAbsolutePath());
        checkProcess(coProcess, f, "checkout", false);

        Process rmProcess =
            Runtime.getRuntime().exec("rm -f " + f.getAbsolutePath());
        checkProcess(rmProcess, f, "remove old file", false);


    }

    /**
     * Start the diff log. Remove any previously existing diff.txt file
     * @throws Exception
     */
    private void startDiffLog() throws Exception {
        // Remove any existing diff.txt
        Process p = Runtime.getRuntime().exec("rm -f " + diffFileName);
        checkProcess(p, null, "remove any existing diff log", false);

        diff = new BufferedWriter(new FileWriter(diffFileName));
    }

    private void checkProcess(Process p, File f, String description,
                              boolean isDiff) throws Exception {
        int exitCode = p.waitFor();
        if (!isDiff && exitCode != 0) {
            if (f != null)
                System.out.println("There was an error when trying to " +
                                   description + " for file : " +
                                   f.getAbsolutePath());
            else
                System.out.println("There was an error when trying to " +
                                   description);
        }

        dumpInputStream(p.getInputStream(), isDiff);
        dumpInputStream(p.getErrorStream(), isDiff);
    }

    private void dumpInputStream(InputStream in,
                                 boolean isDiff) throws Exception {
        int n = in.available();
        byte b[] = new byte[n];
        in.read(b, 0, n);
        String str = new String(b);
        if (str.contains("ade ERROR")) {
            System.out.println(str);
            System.out.println("There was an error interacting with ADE, exiting script.");
            System.exit(-1);
        }

        if (isDiff == false)
            System.out.println(str.trim());
        else
            diff.write(str);
    }


    private void closeDiffLog() throws Exception {
        diff.close();
    }


    /**
     * Class to store all vo details
     */
    public class VODetails {
        /**
         * VO file full path
         */
        String voFile = "";

        /**
         * Max fetch size
         */
        Integer maxFetchSize;

        /**
         * Fetch Mode
         */
        String fetchMode = "";

        /**
         * Fetch Size
         *
         */
        Integer fetchSize;


        public void setMaxFetchSize(Integer maxFetchSize) {
            this.maxFetchSize = maxFetchSize;
        }

        public Integer getMaxFetchSize() {
            return maxFetchSize;
        }

        public void setFetchMode(String fetchMode) {
            this.fetchMode = fetchMode;
        }

        public String getFetchMode() {
            return fetchMode;
        }

        public void setFetchSize(Integer fetchSize) {
            this.fetchSize = fetchSize;
        }

        public Integer getFetchSize() {
            return fetchSize;
        }

        public void setVoFile(String voFullPath) {
            this.voFile = voFullPath;
        }

        public String getVoFullPath() {
            return voFile;
        }
    }

}
