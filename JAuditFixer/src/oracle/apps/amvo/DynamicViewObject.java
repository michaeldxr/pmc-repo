package oracle.apps.amvo;

import java.io.File;
import java.io.FileOutputStream;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JavaFileParserHelper;
import oracle.apps.utility.JoesBaseClass;

public class DynamicViewObject extends JoesBaseClass{
    
    private static Workbook m_myBook;
    private static Worksheet m_Sheet1;
    private static int row = 2;
    
    public DynamicViewObject(){
        super(JoesBaseClass.CRAWL_TYPE.JAVA);
    }
        
    public static void main(String[] args) throws Exception {
        
        String sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_PT.R9ERP_LINUX.X64.rdd/LATEST/fusionapps/fin/components";
        System.out.println("Analyzing all files in directory:" + sCrawlDir);
        
        DynamicViewObject analyzer = new DynamicViewObject();
        
        m_myBook = new Workbook("Dynamic VO Scan");
        m_Sheet1 = new Worksheet("Dynamically added VOs");
        m_myBook.addWorksheet(m_Sheet1);
        
        m_Sheet1.addCell(new Cell(new CellLocation("A1"), new CellValue("Family")));
        m_Sheet1.addCell(new Cell(new CellLocation("B1"), new CellValue("Module")));
        m_Sheet1.addCell(new Cell(new CellLocation("C1"), new CellValue("Product")));
        m_Sheet1.addCell(new Cell(new CellLocation("D1"), new CellValue("FileName")));
        m_Sheet1.addCell(new Cell(new CellLocation("E1"), new CellValue("Series")));
        m_Sheet1.addCell(new Cell(new CellLocation("F1"), new CellValue("Label")));
        m_Sheet1.addCell(new Cell(new CellLocation("G1"), new CellValue("Violation text")));
        
        analyzer.crawlDirectory(sCrawlDir,false);    
        FileOutputStream out = new FileOutputStream("DynamicVOCreation_scan.xlsx");
        XLSXCreator.create(m_myBook, out);
    }

    protected void processFile(File fName, boolean bDoADE) {
        
        try{
            
            JavaFileParserHelper javaFileHelper = new JavaFileParserHelper(fName.getAbsolutePath());
            javaFileHelper.removeAllComments();
            String fileContentsWithoutComments = javaFileHelper.getFileWithoutCommentsReadOnly();
            String[] fileContents = javaFileHelper.getFileContentsArrayWithoutComments(fileContentsWithoutComments);
            String fileNameInfo = "";
            
            for(int i = 0; i < fileContents.length; i++) {
                if(!fileContents[i].contains("createViewObject"))
                    continue;
                
                String comment = fileContents[i-1] + fileContents[i] + fileContents[i+1];
                if(i-2 > 0)
                    comment = fileContents[i-2] + comment;
                if(i+2 < fileContents.length)
                    comment = comment + fileContents[i+2];
                
                if(fileNameInfo.equals(""))
                    fileNameInfo = FamilyModuleHelper.getFileNameInfo1(fName.getAbsolutePath());
                
                String[] parts = fileNameInfo.split(",");
                if(parts.length < 6){
                    System.out.println("File name info could not be obtained correctly");
                    continue;
                }
                
                String c1 = getCellLocation(0, row);
                String c2 = getCellLocation(1, row);
                String c3 = getCellLocation(2, row);
                String c4 = getCellLocation(3, row);
                String c5 = getCellLocation(4, row);
                String c6 = getCellLocation(5, row);
                String c7 = getCellLocation(6, row);
                
                m_Sheet1.addCell(new Cell(new CellLocation(c1), new CellValue(parts[0].trim())));
                m_Sheet1.addCell(new Cell(new CellLocation(c2), new CellValue(parts[1].trim())));
                m_Sheet1.addCell(new Cell(new CellLocation(c3), new CellValue(parts[2].trim())));
                m_Sheet1.addCell(new Cell(new CellLocation(c4), new CellValue(parts[3].trim())));
                m_Sheet1.addCell(new Cell(new CellLocation(c5), new CellValue(parts[4].trim())));
                m_Sheet1.addCell(new Cell(new CellLocation(c6), new CellValue(parts[5].trim())));
                m_Sheet1.addCell(new Cell(new CellLocation(c7), new CellValue(fileContents[i])));
                m_Sheet1.createComment(comment, c7, 10, 10);
                row++;
            }
            
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return null;
    }
}
