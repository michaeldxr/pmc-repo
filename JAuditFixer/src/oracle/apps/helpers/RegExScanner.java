package oracle.apps.helpers;

import java.io.File;

import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;

public class RegExScanner {
    File _file = null;
    // type will govern how we handle comments etc
    String type = "";

    public RegExScanner(String filePath, String type) throws Exception {
        super();
        if (!doesFileExist(filePath))
            throw new Exception("File does not exist : " + filePath);
        _file = new File(filePath);
        this.type = type;
        if (!isValidType(type))
            throw new Exception("Invalid Type .. " + type);
    }

    public boolean isValidType(String type) {
        if (isEmpty(type))
            return false;
        if (type.equals("java") || type.equals("xml"))
            return true;
        return false;
    }

    public RegExScanner(File f, String type) throws Exception {
        super();
        if (f == null || !f.exists())
            throw new Exception("File does not exist ");
        _file = f;
        this.type = type;
        if (!isValidType(type))
            throw new Exception("Invalid Type .. " + type);
    }

    public ArrayList<RegExScanResult> scanFor(Pattern regExPattern) throws Exception {
        ArrayList<RegExScanResult> regexResults = new ArrayList<RegExScanResult>();
        if (regExPattern == null)
            throw new Exception("Missing reg ex.. ");

        String absPath = _file.getAbsolutePath();
        FileParserHelper parserHelper = new FileParserHelper(absPath);
        parserHelper.removeAllComments(type);
        String fileWithoutComments = parserHelper.getFileWithoutCommentsReadOnly();
        Matcher m = regExPattern.matcher(fileWithoutComments);
        while (m.find()) {
            // record the entire line,
            int start = m.start();
            int end = m.end();
            int lineNumber = parserHelper.getLineNumberOfIndex(fileWithoutComments, start);
            String line = parserHelper.getLineAtLineNumber(lineNumber);
            RegExScanResult result = new RegExScanResult(absPath);
            result.setLineNumber(lineNumber);
            result.setMatchingLine(line);
            result.setFileName(_file.getName());
            regexResults.add(result);
        }
        return regexResults;
    }

    public boolean doesContainPattern(Pattern regExPattern) throws Exception {
        if (regExPattern == null)
            throw new Exception("Missing reg ex.. ");

        String absPath = _file.getAbsolutePath();
        FileParserHelper parserHelper = new FileParserHelper(absPath);
        parserHelper.removeAllComments(type);
        String fileWithoutComments = parserHelper.getFileWithoutCommentsReadOnly();
        Matcher m = regExPattern.matcher(fileWithoutComments);
        if (m.find()) {
            return true;
        }
        return false;
    }
    public boolean doesFileExist(String path) {
        if (isEmpty(path))
            return true;
        File f = new File(path);
        if (f != null && f.exists())
            return true;
        return false;
    }

    public boolean isEmpty(String str) {
        if (str == null || "".equals(str.trim()))
            return true;
        return false;
    }


    public static class RegExScanResult {
        private int lineNumber = 0;
        private String matchingLine = "";
        private String fileName = "";
        private String filePath = "";
        private String product = "";
        private String family = "";
        private String additional="";
        public RegExScanResult(String filePath) {
            setFileVals(filePath);
        }

        public void setLineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public void setMatchingLine(String matchingLine) {
            this.matchingLine = matchingLine;
        }

        public String getMatchingLine() {
            return matchingLine;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileVals(String filePath) {
            this.filePath = filePath;
            this.product = FamilyModuleHelper.getProduct(filePath);
            this.family = FamilyModuleHelper.getFamily(filePath);
        }

        public String getFilePath() {
            return filePath;
        }


        public String getProduct() {
            return product;
        }


        public String getFamily() {
            return family;
        }

        public void setAdditional(String additional) {
            this.additional = additional;
        }

        public String getAdditional() {
            return additional;
        }
    }
    public static void writeResultsToExcel(ArrayList<RegExScanner.RegExScanResult> results, String fileName)  throws Exception{
       
        Workbook myBook = new Workbook("output");
        Worksheet sheet = new Worksheet("Results");
        sheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Family")));
        sheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Product")));
        sheet.addCell(new Cell(new CellLocation("C1"), new CellValue("File Name")));
        sheet.addCell(new Cell(new CellLocation("D1"), new CellValue("File Path")));
        sheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Line Number")));
        sheet.addCell(new Cell(new CellLocation("F1"), new CellValue("Line")));
        sheet.addCell(new Cell(new CellLocation("G1"), new CellValue("Additional Info")));
        
        int rowNum = 2;
        if (results != null) {
            Iterator<RegExScanner.RegExScanResult> resultIter = results.iterator();
            while (resultIter.hasNext()) {
                RegExScanner.RegExScanResult result = resultIter.next();
                sheet.addCell(new Cell(new CellLocation("A" + rowNum), new CellValue(result.getFamily())));
                sheet.addCell(new Cell(new CellLocation("B" + rowNum), new CellValue(result.getProduct())));
                sheet.addCell(new Cell(new CellLocation("C" + rowNum), new CellValue(result.getFileName())));
                sheet.addCell(new Cell(new CellLocation("D" + rowNum), new CellValue(result.getFilePath())));
                sheet.addCell(new Cell(new CellLocation("E" + rowNum), new CellValue(result.getLineNumber())));
                sheet.addCell(new Cell(new CellLocation("F" + rowNum), new CellValue(result.getMatchingLine())));
                String additionalInfo = result.getAdditional();
                if(additionalInfo!=null && additionalInfo.length()>0)
                    sheet.addCell(new Cell(new CellLocation("G" + rowNum), new CellValue(additionalInfo)));
               
                rowNum++;
            }
        }
        myBook.addWorksheet(sheet);
        writeSpreadsheet(fileName,myBook);
    }

    public static void writeSpreadsheet(String outputFileName, Workbook book) throws Exception {

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFileName);
            XLSXCreator.create(book, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

   
}
