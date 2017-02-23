package oracle.apps.idAnalyzer;

import java.io.File;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;

public class NamingCntSpreadsheetGenerator {
    /**
     * Number of jsff files processed
     */
    static int m_jsffCount = 0;

    /**
     * number of jspx files processed
     */
    static int m_jspxCount = 0;

    /**
     * Number of naming container id violations
     */
    static int number_of_violations = 0;

    /**
     * Excel workbook to write to
     */
    Workbook m_myBook;

    /**
     * Excel worksheet to write to
     */
    Worksheet m_worksheet;

    /**
     * Current worksheet row
     */
    int worksheet_currentRow = 2;

    public NamingCntSpreadsheetGenerator() {
        super();

        intializeSpreadsheet();
    }

    public static void main(String[] args) {

        // Print Statistics
        Date startDate = new Date();

        if (args.length != 2 || args[0] == null || args[1] == null ||
            args[0].equals("") || args[1].equals("")) {
            System.out.println("Usage:  generateNamingCntSpreadsheet.sh <path to your code in ade> <output file name, including path>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        String sOutpFilename = args[1];
        if (isEmpty(sCrawlDir) || isEmpty(sOutpFilename)) {
            System.out.println("Either crawl directory or output file name is empty");
            System.exit(1);
        }

        System.out.println("Analyzing directory: " + sCrawlDir);
        System.out.println("Will generate file: " + sOutpFilename);

        NamingCntSpreadsheetGenerator spreadsheetGenerator =
            new NamingCntSpreadsheetGenerator();

        /**
         * Recursevely scan the directory. For any valid UI file, process all the faces components
         * For each naming container, determine if the length of the component Id is within standards (7char)
         * populate the list of resulting naming containers not within standards, indexed by their file name
         */
        spreadsheetGenerator.crawlDirectory(sCrawlDir, false);

        try {
            /**
         * Write spreadsheet to disk
         */
            spreadsheetGenerator.writeSpreadsheet(sOutpFilename);
        } catch (Exception ex) {
            System.out.println("Error while writing spreadsheet to file: " +
                               sOutpFilename);
        }

        printStatistics(startDate);
    }

    /**
     *Write spreadsheet to disk
     * @param outputFileName
     * Output file name to write to
     * @throws Exception
     * Exception if operation is unsuccessful
     */
    public void writeSpreadsheet(String outputFileName) throws Exception {

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFileName);
            XLSXCreator.create(m_myBook, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

    /**
     * Write the trigger serialized xml as a comment in the componentId cell
     * @param rowNumber
     * Row number to write to
     * @param serializedRefs
     * Trigger serialized tag
     */
    public void writeTriggerInSheetRow(int rowNumber, String serializedRefs) {
        String triggerCellNumber = "G" + rowNumber;
        String compIdCellNumber = "E" + rowNumber;
        /*  m_worksheet.addCell(new Cell(new CellLocation(triggerCellNumber),
                                     new CellValue(serializedRefs)));*/
        m_worksheet.createComment(serializedRefs, compIdCellNumber);

    }

    /**
     * Write the component details in the spreadsheet row
     * @param rowNumber
     * row number to write to
     * @param component
     * Component details
     */
    public void writeComponentInSheetRow(int rowNumber,
                                         ComponentDetails component) {
        String productName = component.getProduct();
        String compfilePath = component.getFilePath();
        String compfileName = component.getFileName();
        String componentName = component.getComponentName();
        String compId = component.getComponentId();
        String suggestedId = component.getSuggestedId();

        String productCellNumber = "A" + rowNumber;
        String fileNameCellNumber = "B" + rowNumber;
        String filePathCellNumber = "C" + rowNumber;
        String compNameCellNumber = "D" + rowNumber;
        String compIdCellNumber = "E" + rowNumber;
        String suggestedIdCellNumber = "F" + rowNumber;


        m_worksheet.addCell(new Cell(new CellLocation(productCellNumber),
                                     new CellValue(productName)));

        m_worksheet.addCell(new Cell(new CellLocation(fileNameCellNumber),
                                     new CellValue(compfileName)));

        m_worksheet.addCell(new Cell(new CellLocation(filePathCellNumber),
                                     new CellValue(compfilePath)));

        m_worksheet.addCell(new Cell(new CellLocation(compNameCellNumber),
                                     new CellValue(componentName)));

        m_worksheet.addCell(new Cell(new CellLocation(compIdCellNumber),
                                     new CellValue(compId)));

        m_worksheet.addCell(new Cell(new CellLocation(suggestedIdCellNumber),
                                     new CellValue(suggestedId)));
        m_worksheet.createComment(component.getSerializedTag(),
                                  compNameCellNumber);

        // Secondary References


    }

    /**
     *Print Run statistics
     * @param startDate
     */
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
        System.out.println("Total Number of jspx files reviewed: " +
                           m_jspxCount);
        System.out.println("Total Number of jsff files reviewed: " +
                           m_jsffCount);
        System.out.println("Total Number of violations found: " +
                           number_of_violations);

        System.out.println("=================================================");

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
    private void crawlDirectory(String path, Boolean bDoADE) {
        if (path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (isValidUIFile(absFilePath)) {
                    if (absFilePath.trim().endsWith(".jsff")) {
                        m_jsffCount++;
                    } else if (absFilePath.trim().endsWith(".jspx")) {
                        m_jspxCount++;
                    }
                    //                    System.out.print("..");
                    processUiFile(listOfFiles[i], bDoADE);
                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidDirectory(dir)) {
                    crawlDirectory(dir, bDoADE);
                }
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }


    /**
     * Process each valid ui file. If the file has any found violations, fix them and increment the violation count
     * @param file
     * @param bDoADE
     */
    public void processUiFile(File file, boolean bDoADE) {
        //       System.out.print("..");

        NamingCntIdFileProcessor namingContainerProc =
            new NamingCntIdFileProcessor();

        // call analyze namingContainers
        namingContainerProc.analyzeNamingCntsForSpreadsheet(file, bDoADE);

        // get all violating components
        ArrayList<ComponentDetails> list_violations =
            namingContainerProc.getAllViolatingComponents();
        if (list_violations.size() > 0) {
            HashMap<String, ArrayList<PartialTriggers>> secondaryRefs =
                namingContainerProc.getSecondaryRefs_byCompId();

            // keep track of the total number of violations
            number_of_violations =
                    number_of_violations + list_violations.size();

            writeFileOutputToSheet(list_violations, secondaryRefs);
        }

    }

    /**
     * Write the output of the analysis to a spreadsheet
     * @param list_components
     * List of violating components
     * @param secondaryRefs
     * List of secondary references by referenced componentId
     */
    public void writeFileOutputToSheet(ArrayList<ComponentDetails> list_components,
                                       HashMap<String, ArrayList<PartialTriggers>> secondaryRefs) {
        Iterator<ComponentDetails> component_iter = list_components.iterator();
        while (component_iter.hasNext()) {
            ComponentDetails comp = component_iter.next();
            writeComponentInSheetRow(worksheet_currentRow, comp);
            String compId = comp.getComponentId();
            if (secondaryRefs.containsKey(compId)) {
                ArrayList<PartialTriggers> list_triggers =
                    secondaryRefs.get(compId);
                Iterator<PartialTriggers> trigger_iter =
                    list_triggers.iterator();
                String triggerContents = "";
                while (trigger_iter.hasNext()) {
                    PartialTriggers trigger = trigger_iter.next();
                    String lineNumPrefix =
                        "Line " + trigger.getLineNumber() + " : ";
                    triggerContents =
                            triggerContents + lineNumPrefix + trigger.getSerializedTag() +
                            "\n \n";
                }
                writeTriggerInSheetRow(worksheet_currentRow, triggerContents);
            }
            worksheet_currentRow++;
        }
    }

    public boolean isValidDirectory(String absDirPath) {
        if (!absDirPath.contains("/model/") &&
            !absDirPath.contains("/dbSchema/") &&
            !absDirPath.contains("/publicModel/") &&
            !absDirPath.contains("/classes/") &&
            !absDirPath.contains("/test/")) {
            return true;
        }
        return false;
    }

    public boolean isValidUIFile(String absFilePath) {
        if (!isEmpty(absFilePath)) {
            if (absFilePath.contains("/test/") ||
                absFilePath.contains("/modelTest/") ||
                absFilePath.contains("/classes/") ||
                absFilePath.contains("/model/") ||
                absFilePath.contains("/publicModel/")) {
                return false;
            }
            if (absFilePath.endsWith(".jspx") ||
                absFilePath.endsWith(".jsff")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    private void intializeSpreadsheet() {
        // Create the workbook
        m_myBook = new Workbook("output");
        // Create a worksheet
        m_worksheet = new Worksheet("NamingContainers");
        // column 1 - product
        m_worksheet.setColumnWidth(1, 10);
        // column 2 - filename
        m_worksheet.setColumnWidth(2, 30);
        // Column 3 - file path
        m_worksheet.setColumnWidth(3, 40);
        // Column 4 - component Name
        m_worksheet.setColumnWidth(4, 30);
        // Column 5 - component Id
        m_worksheet.setColumnWidth(5, 30);
        // Column 6 - New component Id
        m_worksheet.setColumnWidth(6, 30);
        // Column 6 - Secondary References
        m_worksheet.setColumnWidth(7, 30);

        m_worksheet.addCell(new Cell(new CellLocation("A1"),
                                     new CellValue("Product")));
        m_worksheet.addCell(new Cell(new CellLocation("B1"),
                                     new CellValue("File Name")));
        m_worksheet.addCell(new Cell(new CellLocation("C1"),
                                     new CellValue("File Path")));
        m_worksheet.addCell(new Cell(new CellLocation("D1"),
                                     new CellValue("Component Name")));
        m_worksheet.addCell(new Cell(new CellLocation("E1"),
                                     new CellValue("Component Id")));
        m_worksheet.addCell(new Cell(new CellLocation("F1"),
                                     new CellValue("New Id")));

        m_myBook.addWorksheet(m_worksheet);
    }
}
