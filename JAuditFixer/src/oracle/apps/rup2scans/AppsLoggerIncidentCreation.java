package oracle.apps.rup2scans;

import java.io.File;

import java.io.FileOutputStream;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.JavaFileParserHelper;


public class AppsLoggerIncidentCreation {
    
    static Workbook m_myBook;
    static Worksheet m_attributesSheet;
    static int row = 2;
    
    public static void main(String[] args)  throws Exception{
      
        String sCrawlDir = args[0].trim();
        
        m_myBook = new Workbook("AppsLogger Scan");

        m_attributesSheet = new Worksheet("AppsLogger Incident Statements");
        
        m_myBook.addWorksheet(m_attributesSheet);
        m_attributesSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Family")));
        m_attributesSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Module")));
        m_attributesSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Product")));
        m_attributesSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("FileName")));
        m_attributesSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Label")));
        m_attributesSheet.addCell(new Cell(new CellLocation("F1"), new CellValue("Line No")));
        m_attributesSheet.addCell(new Cell(new CellLocation("G1"), new CellValue("AppsLoggerStatement")));
        
        System.out.println("Analyzing all files in directory:" + sCrawlDir);

        AppsLoggerIncidentCreation scanner = new AppsLoggerIncidentCreation();
        scanner.crawlDirectory(sCrawlDir);     
        
        FileOutputStream out = new FileOutputStream("AppsLogger_scan.xlsx");
        XLSXCreator.create(m_myBook, out);
    }
    
    private void crawlDirectory(String path) throws Exception
      {
          if(path.contains(".ade_path") || path.contains("/classes/"))
              return;

          File folder = new File(path);
          File[] listOfFiles = folder.listFiles();
              
          if(listOfFiles == null)
              return;
          
          for(int i = 0; i <listOfFiles.length; i++)
          {
              if(listOfFiles[i].isFile()) 
              {
                  String sName = listOfFiles[i].getAbsolutePath();              
                  if(fileOfInterest(sName))               
                      processFile(listOfFiles[i]);                           
              }
              else if(listOfFiles[i].isDirectory()) 
                  crawlDirectory(listOfFiles[i].getAbsolutePath());          
              else 
                  System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);          
          }      
      }

    private boolean fileOfInterest(String sName) 
    {
        String absPathLowerCase = sName.toLowerCase();
        
        if(!absPathLowerCase.contains("/components"))
            return false;
        
        if (absPathLowerCase.contains("modeltest") ||
            absPathLowerCase.contains("uitest") || absPathLowerCase.contains("testui") ||
            absPathLowerCase.contains("/test/") || absPathLowerCase.contains("noship") ||
            absPathLowerCase.contains("servicetest") || absPathLowerCase.contains("structuretest"))
            return false;
        
        if (sName.endsWith(".java")) {
          return true;
        }
      return false;
    }
    
    private void processFile(File fName) throws Exception{
        
        JavaFileParserHelper javaHelper = new JavaFileParserHelper(fName.getAbsolutePath());
        javaHelper.removeAllComments();
        String fileContents = javaHelper.getFileWithoutCommentsReadOnly();
        String[] lines = javaHelper.getFileContentsArrayWithoutComments(fileContents);
        
        int i = 0;
        int lineNo = 0;
        String line = "";
        
        while(i< lines.length) {
            
            line = lines[i];
            if(line.trim().equals("")){ //ignore blank lines
                i++;
                continue;
            }
            lineNo = i+1; //record line number of the begining of the line
            while(!(line.trim().endsWith(";") || line.trim().endsWith("{") || line.trim().endsWith("}") || (i == lines.length-1)))
                line += lines[++i];
            
            line = line.trim();  
            
            if(line.contains("AppsLogger.createIncident")){
                writeFileNameInfo(fName.getCanonicalPath());
                
                String c6 = getCellLocation(5, row);
                String c7 = getCellLocation(6, row);       
                
                row++;
                
                m_attributesSheet.addCell(new Cell(new CellLocation(c6), new CellValue(lineNo)));
                m_attributesSheet.addCell(new Cell(new CellLocation(c7), new CellValue(line)));
            }
            i++;
        }
    }
    
    private void writeFileNameInfo(String absPath) {
        
      int i = absPath.indexOf("fusionapps/");
      String pathAfterFusionApps = absPath.substring(i);
      String[] parts = pathAfterFusionApps.split("/");
      String family = parts[1].toUpperCase();
      String module = parts[3];
      String product = parts[4].toUpperCase();
      
      String path_before_fusionapps = absPath.substring(0, i-1);
      parts = path_before_fusionapps.split("/");
      String label = parts[parts.length -2] + "_" + parts[parts.length -1];
     
      label = label.replace(".rdd","");
        
          String c1 = getCellLocation(0, row);
          String c2 = getCellLocation(1, row);
          String c3 = getCellLocation(2, row);
          String c4 = getCellLocation(3, row);
          String c5 = getCellLocation(4, row);
          
          m_attributesSheet.addCell(new Cell(new CellLocation(c1), new CellValue(family)));
          m_attributesSheet.addCell(new Cell(new CellLocation(c2), new CellValue(module)));
          m_attributesSheet.addCell(new Cell(new CellLocation(c3), new CellValue(product)));
          m_attributesSheet.addCell(new Cell(new CellLocation(c4), new CellValue(pathAfterFusionApps)));
          m_attributesSheet.addCell(new Cell(new CellLocation(c5), new CellValue(label)));
     
    }
    
    private String getCellLocation(int col, int row) 
    {
        return getColumn(col) + row;
    }
    
    private String getColumn(int col) 
    {
        int r = col % 26;
        int x = col / 26;
        
        String retVal = "";
        
        if(col >= 26)
        {
            retVal = getColumn(x-1);
        }
        
        return retVal +getSingleColumnDigit(r);
    }
    
    private String getSingleColumnDigit(int col) 
    {
        assert(col >= 0 && col < 26);
        
        int valueOfA  = 65;
        char c = (char)(valueOfA+col);
        return String.valueOf(c);
    }
}
