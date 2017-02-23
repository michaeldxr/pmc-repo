package oracle.apps.dm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.NullEntityResolver;
import oracle.apps.helpers.Mail;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class DMFixer extends JoesBaseClass {


    public enum Key {
        PATH,
        OBJECT_TYPE,
        NAME,
        NEW_DESCRIPTION;
    }

    private static final int ROW_SIZE = 11;

    private static final String indent = "   ";

    private class InvalidPathException extends Exception {
    }

    private DOMParser parser;

    private int numRowsProcessed = 0;
    private int numRowsIgnoredBlank = 0;
    private int numRowsIgnoredBadPath = 0;
    private int numRowsIgnoredBadColName = 0;
    private int numRowsAlreadyChanged = 0;
    private int numUnexpectedError = 0;

    public DMFixer() {
        super(null);
    }

    protected void processFile(File fName, boolean bDoADE) {
        // Not used in this class.
    }

    private void start(String viewRoot, String csvPath, Boolean doADE) {
        parser = new DOMParser();
        System.out.println("====================================================");
        System.out.println("Processing CSV and making changes to .table files...");
        System.out.println("====================================================");

        Collection<String> csvRows;
        try {
            csvRows = parse(csvPath);

            Iterator<String> csvRowsIterator = csvRows.iterator();
            while (csvRowsIterator.hasNext()) {
                String csvRow = csvRowsIterator.next();
                System.out.println("INFO: Processing CSV row: " + csvRow);
                try {
                    HashMap<Key, String> row = parseRow(csvRow);
                    if (row.get(Key.NEW_DESCRIPTION).isEmpty()) {
                        numRowsIgnoredBlank++;
                        continue;
                    }
                    String path = replaceADERootWithViewRoot(row.get(Key.PATH), viewRoot);
                    File file = new File(path);
                    if (!file.exists()) {
                        System.out.println("ERROR: File doesn't exist: " + file.getAbsolutePath());
                        System.out.println("ERROR: ... Skipping row");
                        numRowsIgnoredBadPath++;
                        continue;
                    }

                    parser.reset();
                    try {
                        parser.reset();
                        parser.setDebugMode(true);
                        parser.setErrorStream(System.out);
                        parser.setEntityResolver(new NullEntityResolver());
                        parser.showWarnings(true);
                        parser.setValidationMode(DOMParser.NONVALIDATING);
                        parser.parse("file:" + file.getAbsolutePath());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("ERROR: XML Parse error while parsing file:  " + file.getAbsolutePath());
                        System.out.println("ERROR: ... Skipping");
                        this.logException("Error parsing file: " + file.getAbsolutePath(), e);
                        return;
                    }
                    XMLDocument doc = parser.getDocument();

                    // Grab the appropriate comment element.
                    Node n = XMLParserHelper.getChildNodeWithName(doc, "COMPOSITE");
                    n = XMLParserHelper.getChildNodeWithName(n, "BASE_OBJECT");
                    n = XMLParserHelper.getChildNodeWithName(n, "TABLE");

                    if (row.get(Key.OBJECT_TYPE).equals("TABLE")) {

                        n = XMLParserHelper.getChildNodeWithName(n, "CUSTOMER_AREA");
                        Node n_tmp = XMLParserHelper.getChildNodeWithName(n, "ODB_PROPERTY_LIST");
                        if (n_tmp != null) {
                            n = n_tmp;
                            n_tmp = XMLParserHelper.getChildNodeWithName(n, "COMMENT");
                            if (n_tmp != null) {
                                n = n_tmp;
                                XMLElement e = (XMLElement)n;
                                int lineNum = e.getLineNumber();
                                applyNewDescription(file, lineNum, row.get(Key.NEW_DESCRIPTION), doADE);
                            }
                            else {
                                System.out.println("ERROR: Could not update row. Expected XML tags not found.");
                                numUnexpectedError++;
                            }
                        }
                        else {
                            XMLElement e = (XMLElement)n;
                            int linenum = e.getLineNumber();
                            applyNewDescriptionWithODBPropList(file, linenum, row.get(Key.NEW_DESCRIPTION), doADE);
                        }
                    }
                    else {
                        n = XMLParserHelper.getChildNodeWithName(n, "RELATIONAL_TABLE");
                        n = XMLParserHelper.getChildNodeWithName(n, "COL_LIST");
                        NodeList col_list_items = n.getChildNodes();
                        int length = col_list_items.getLength();
                        boolean found = false;
                        for (int i = 0; i < length; i++) {
                            n = col_list_items.item(i);
                            if (n.getNodeName().equals("COL_LIST_ITEM")) {
                                Node name = XMLParserHelper.getChildNodeWithName(n, "NAME");
                                if (name.getTextContent().equals(row.get(Key.NAME))) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            System.out.println("ERROR: Column not found: " + row);
                            System.out.println("ERROR: ... SKipping");
                            numRowsIgnoredBadColName++;
                            continue;
                        }
                        // We found the column. Now drilling down to the comment.
                        Node n_tmp = XMLParserHelper.getChildNodeWithName(n, "CUSTOMER_AREA");
                        if (n_tmp != null) {
                            n = n_tmp;
                            n_tmp = XMLParserHelper.getChildNodeWithName(n, "ODB_PROPERTY_LIST");
                            if (n_tmp != null) {
                                n = n_tmp;
                                n_tmp = XMLParserHelper.getChildNodeWithName(n, "COMMENT");
                                if (n_tmp != null) {
                                    n = n_tmp;
                                    XMLElement e = (XMLElement)n;
                                    int lineNum = e.getLineNumber();
                                    applyNewDescription(file, lineNum, row.get(Key.NEW_DESCRIPTION), doADE);
                                }
                                else {
                                    XMLElement e = (XMLElement)n;
                                    int linenum = e.getLineNumber();
                                    applyNewDescriptionWithODBPropList(file, linenum, row.get(Key.NEW_DESCRIPTION), doADE);
                                }
                            }
                            else {
                                XMLElement e = (XMLElement)n;
                                int linenum = e.getLineNumber();
                                applyNewDescriptionWithODBPropList(file, linenum, row.get(Key.NEW_DESCRIPTION), doADE);
                            }
                        }
                        else {
                            XMLElement e = (XMLElement)n;
                            int linenum = e.getLineNumber();
                            applyNewDescriptionWithODBPropListAndCustomerArea(file, linenum, row.get(Key.NEW_DESCRIPTION), doADE);
                        }
                    }

                    numRowsProcessed++;
                }
                catch (Exception e) {
                    if (!csvRow.equals("")) {
                        System.out.println("ERROR: Unexpected error parsing CSV row: " + csvRow);
                        this.logException("Error parsing CSV row: ", e);
                        numUnexpectedError++;
                    }
                }
            }
        }
        catch (FileNotFoundException e) {
            System.err.println("FATAL ERROR: Could not find CSV file: " + csvPath);
            this.logException("ERROR: Count not find CSV file: ", e);
        }
        catch (IOException e) {
            System.err.println("FATAL ERROR: Could not find CSV file: " + csvPath);
            this.logException("ERROR: Count not find CSV file: ", e);
        }
        catch (Exception e) {
            System.err.println("FATAL ERROR: Unexpected error parsing csv: " + csvPath);
            this.logException("Unexpected error parsing csv.", e);
        }
    }


    private void applyNewDescription(File f, int lineNum,
                                     String newDescription,
                                     Boolean doADE) throws Exception {
        boolean modified = false;

        File fp = new File(f.getAbsolutePath());
        FileReader reader = new FileReader(fp);
        BufferedReader input = new BufferedReader(reader);

        FileReader reader2 = new FileReader(fp);
        BufferedReader input2 = new BufferedReader(reader2);

        String line;

        StringBuffer sb = new StringBuffer();

        int myLineNum = 1;
        input.mark(1024);

        while ((line = input.readLine()) != null) {
            input2.skip(line.length());
            input2.mark(1024);
            int c1 = input2.read();
            int c2 = input2.read();
            input2.reset();

            String newline = "\n";
            if (c1 == -1) {
                newline = "";
            }
            else if (c1 == '\n') {
                input2.read();
            }
            else if (c1 == '\r' && c2 != '\n') {
                newline = "\r";
                input2.read();
            }
            else if (c1 == '\r' && c2 == '\n') {
                newline = "\r\n";
                input2.read();
                input2.read();
            }

            if (myLineNum == lineNum) {
                String line2 = line.replaceAll("(<COMMENT>).*?(</COMMENT>)", "$1" + newDescription + "$2");
                if (!line2.equals(line)) {
                    modified = true;
                }

                sb.append(line2 + newline);
            }
            else {
                sb.append(line);
                sb.append(newline);
            }

            myLineNum++;
        }

        reader.close();

        if (modified) {
            if (doADE) {
                checkoutAndDelete(f);

                FileWriter fw = new FileWriter(f.getAbsolutePath());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(sb.toString());
                bw.close();

                checkinAndDiff(f);
            }
            else {
                System.out.println(sb.toString());
            }
        }
        else {
            numRowsAlreadyChanged++;
            numRowsProcessed--;
        }
    }

    private void applyNewDescriptionWithODBPropList(File f, int lineNum,
                                                    String newDescription,
                                                    Boolean doADE) throws Exception {
        boolean modified = false;

        File fp = new File(f.getAbsolutePath());
        FileReader reader = new FileReader(fp);
        BufferedReader input = new BufferedReader(reader);

        FileReader reader2 = new FileReader(fp);
        BufferedReader input2 = new BufferedReader(reader2);

        String line;

        StringBuffer sb = new StringBuffer();

        int myLineNum = 1;
        input.mark(1024);

        while ((line = input.readLine()) != null) {
            input2.skip(line.length());
            input2.mark(1024);
            int c1 = input2.read();
            int c2 = input2.read();
            input2.reset();

            String newline = "\n";
            if (c1 == -1) {
                newline = "";
            }
            else if (c1 == '\n') {
                input2.read();
            }
            else if (c1 == '\r' && c2 != '\n') {
                newline = "\r";
                input2.read();
            }
            else if (c1 == '\r' && c2 == '\n') {
                newline = "\r\n";
                input2.read();
                input2.read();
            }

            if (myLineNum == lineNum) {
                // capture the leading spaces in the <customer_area> component
                Matcher m = Pattern.compile("\\A(\\s*?)<").matcher(line);
                m.find();
                String ls = m.group(1);
                sb.append(line + newline);
                sb.append(ls + indent + "<ODB_PROPERTY_LIST>" + newline);
                sb.append(ls + indent + indent + "<COMMENT>" + newDescription + "</COMMENT>" + newline);
                sb.append(ls + indent + "</ODB_PROPERTY_LIST>" + newline);
                modified = true;
            }
            else {
                sb.append(line);
                sb.append(newline);
            }

            myLineNum++;
        }

        reader.close();

        if (modified) {
            if (doADE) {
                checkoutAndDelete(f);

                FileWriter fw = new FileWriter(f.getAbsolutePath());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(sb.toString());
                bw.close();

                checkinAndDiff(f);
            }
            else {
                System.out.println(sb.toString());
            }
        }
        else {
            numRowsAlreadyChanged++;
            numRowsProcessed--;
        }
    }
    
  private void applyNewDescriptionWithODBPropListAndCustomerArea(File f, int lineNum,
                                                  String newDescription,
                                                  Boolean doADE) throws Exception {
      boolean modified = false;
      boolean startTagFound = false;

      File fp = new File(f.getAbsolutePath());
      FileReader reader = new FileReader(fp);
      BufferedReader input = new BufferedReader(reader);

      FileReader reader2 = new FileReader(fp);
      BufferedReader input2 = new BufferedReader(reader2);

      String line;

      StringBuffer sb = new StringBuffer();

      int myLineNum = 1;
      input.mark(1024);

      while ((line = input.readLine()) != null) {
          input2.skip(line.length());
          input2.mark(1024);
          int c1 = input2.read();
          int c2 = input2.read();
          input2.reset();

          String newline = "\n";
          if (c1 == -1) {
              newline = "";
          }
          else if (c1 == '\n') {
              input2.read();
          }
          else if (c1 == '\r' && c2 != '\n') {
              newline = "\r";
              input2.read();
          }
          else if (c1 == '\r' && c2 == '\n') {
              newline = "\r\n";
              input2.read();
              input2.read();
          }

          if (myLineNum == lineNum) {
                startTagFound = true;
          }
          if (startTagFound && line.contains("</COL_LIST_ITEM>")) {
              // capture the leading spaces in the <customer_area> component
              Matcher m = Pattern.compile("\\A(\\s*?)<").matcher(line);
              m.find();
              String ls = m.group(1);
              sb.append(ls + indent + "<CUSTOMER_AREA>" + newline);
              sb.append(ls + indent + indent + "<ODB_PROPERTY_LIST>" + newline);
              sb.append(ls + indent + indent + indent + "<COMMENT>" + newDescription + "</COMMENT>" + newline);
              sb.append(ls + indent + indent + "</ODB_PROPERTY_LIST>" + newline);
              sb.append(ls + indent + "</CUSTOMER_AREA>" + newline);
              sb.append(line + newline);
              modified = true;
	      startTagFound = false;
          }
          else {
              sb.append(line);
              sb.append(newline);
          }

          myLineNum++;
      }

      reader.close();

      if (modified) {
          if (doADE) {
              checkoutAndDelete(f);

              FileWriter fw = new FileWriter(f.getAbsolutePath());
              BufferedWriter bw = new BufferedWriter(fw);
              bw.write(sb.toString());
              bw.close();

              checkinAndDiff(f);
          }
          else {
              System.out.println(sb.toString());
          }
      }
      else {
          numRowsAlreadyChanged++;
          numRowsProcessed--;
      }
    }

    private Collection<String> parse(String csvPath) throws FileNotFoundException,
                                                            IOException {
        boolean firstLine = true;
        File csvFile = new File(csvPath);
        FileReader reader = new FileReader(csvFile);
        StringBuilder lineBuilder = new StringBuilder();
        Collection<String> csv = new ArrayList<String>();

        boolean inQuotes = false;
        int i = reader.read();
        while (i != -1) {
            char c = (char)i;
            if (c == '\"') {
                inQuotes = !inQuotes;
            }

            if (!inQuotes && c == '\n') {
                if (!firstLine) {
                    csv.add(lineBuilder.toString());
                }
                else {
                    firstLine = false;
                }
                lineBuilder = new StringBuilder();
            }
            else {
                lineBuilder.append(c);
            }

            i = reader.read();
        }
        
        return csv;
    }


    private HashMap<DMFixer.Key, String> parseRow(String csvRow) {
        List<String> elements = splitRow(csvRow);

        int length = elements.size();
        if (length != ROW_SIZE)
            throw new RuntimeException("This CSV does not contain the correct number of rows.");

        HashMap<Key, String> row = new HashMap<Key, String>();
        row.put(Key.OBJECT_TYPE, elements.get(4));
        int nameColumnNum = 5;
        if (elements.get(4).equals("COLUMN")) {
            nameColumnNum = 6;
        }
        row.put(Key.PATH, elements.get(3));
        row.put(Key.NAME, elements.get(nameColumnNum));
        String newDesc = elements.get(10);
        if (newDesc.contains("\n")) {
            row.put(Key.NEW_DESCRIPTION, elements.get(10).replaceAll("\n", ""));
            System.out.println("WARNING: Your new description contains newlines. Will remove them before updating the .table file.");
        }
        else {
          row.put(Key.NEW_DESCRIPTION, elements.get(10));
        }

        return row;
    }

    private List<String> splitRow(String row) {
        List<String> elements = new ArrayList<String>();
        boolean inQuotes = false;
        char chars[] = row.toCharArray();
        int strlen = row.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strlen; i++) {
            char c = chars[i];
            if (c == '\"') {
                inQuotes = !inQuotes;
            }
            else if (c != ',' || (c == ',' && inQuotes)) {
                sb.append(c);
            }
            else {
                elements.add(sb.toString().trim());
                sb = new StringBuilder();
            }
        }
        elements.add(sb.toString().trim());
        return elements;
    }

    private String replaceADERootWithViewRoot(String adePath,
                                              String viewRoot) throws InvalidPathException {
        if (adePath == null || adePath.isEmpty()) {
            throw new InvalidPathException();
        }
        if (adePath.contains("/fusionapps/")) {
            int i = adePath.indexOf("/fusionapps/");
            String relativePath = adePath.substring(i);
            return viewRoot + relativePath;
        }
        else {
            throw new InvalidPathException();
        }
    }

    protected String getSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================================================\n");
        sb.append("Number of CSV rows processed successfully:                            " + numRowsProcessed + "\n");
        if (numRowsAlreadyChanged > 0)
            sb.append("Number of CSV rows skipped because the change has already been made:  " + numRowsAlreadyChanged + "\n");
        if (numRowsIgnoredBadPath > 0)
            sb.append("Number of CSV rows skipped because they had unresolvable file paths:  " + numRowsIgnoredBadPath + "\n");
        if (numRowsIgnoredBadColName > 0)
            sb.append("Number of CSV rows ignored, as their column names could not be found: " + numRowsIgnoredBlank + "\n");
        if (numRowsIgnoredBlank > 0)
            sb.append("Number of CSV rows ignored because they didn't have new descriptions: " + numRowsIgnoredBlank + "\n");
        if (numUnexpectedError > 0)
            sb.append("Number of CSV rows ignored due to an unexpected error:                " + numUnexpectedError + "\n");
        sb.append("============================================================================\n");

        return sb.toString();
    }


    protected String getSummaryReportSubject() {
        return "SCRIPT: Data Model Fixer";
    }

    protected void logUsageReport(String csvPath, String viewRoot) {
        String s = "";

        s += "user.dir set to  --->  " + System.getProperty("user.dir") + "\n";
        s += "CSV path set to  --->  " + csvPath + "\n";
        s += "View root set to --->  " + viewRoot + "\n";

        s += getSummaryReport();

        System.out.println(s);

        s += "\n\n**EXCEPTION REPORT ************************************************\n";
        if (m_numExceptions == 0) {
            s += "No Exceptions to report.\n";
        }
        else {
            s += "A total of " + m_numExceptions + " exception(s) were collected.\n\n";
            s += sExceptions;
        }
        //Mail.sendMail("angel.irizarry@oracle.com", s, getSummaryReportSubject());
    }

    private static boolean isEmpty(String arg) {
        return (arg == null || arg.isEmpty());
    }

    public static void main(String[] args) {
        if (args.length != 3 || isEmpty(args[0]) || isEmpty(args[1]) || isEmpty(args[2])) {
            System.out.println("Usage: dmFix.sh <path to your .csv file>");
            System.exit(1);
        }

        String viewRoot = args[0];
        Boolean doADE = new Boolean(args[1]);
        String csvPath = args[2];

        if (!csvPath.endsWith(".csv")) {
            System.err.println("Usage: findAccessibilityViolations.sh <path to your .csv file>");
            System.err.println(csvPath + " may not be a valid CSV file.");
            System.exit(1);
        }

        DMFixer x = new DMFixer();

        if (doADE.booleanValue()) {
            try {
                x.startDiffLog();
            }
            catch (Exception e) {
                x.logException("diff log exception", e);
            }
        }

        x.start(viewRoot, csvPath, doADE);

        x.logUsageReport(csvPath, viewRoot);
    }
}
