package oracle.apps.votuning_v2.file_type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;

public class PotentialCorruption
{
    String m_firstJarName = "--";
    String m_firstFileName = "--";
    String m_secondJarName = "--";
    String m_secondFileName = "--";
    String m_sDescription = "--";

    public PotentialCorruption(String firstJarName, String firstFileName, String secondJarName, String secondFileName, String description)
    {
        m_firstJarName = firstJarName;
        m_firstFileName = firstFileName;
        m_secondJarName = secondJarName;
        m_secondFileName = secondFileName;
        m_sDescription = description;
    }

    
    static ArrayList<PotentialCorruption> list = new ArrayList<PotentialCorruption>();
    
    public static void register(String firstJarName, String firstFileName, String secondJarName, String secondFileName, String description)
    {
       PotentialCorruption pc = new PotentialCorruption(firstJarName, firstFileName, secondJarName, secondFileName,description);
       list.add(pc);
   }
    
    public static void dumpSheet(Workbook myBook)
    {
        // Create a worksheet
        Worksheet dupSheet = new Worksheet("Corruption Report");
        dupSheet.setColumnWidth(1, 25);
        dupSheet.addCell(new Cell(new CellLocation("A1"),
                                       new CellValue("JAR")));

        dupSheet.setColumnWidth(2, 25);
        dupSheet.addCell(new Cell(new CellLocation("B1"),
                                       new CellValue("File")));

        dupSheet.setColumnWidth(4, 25);
        dupSheet.addCell(new Cell(new CellLocation("C1"),
                                       new CellValue("First - JAR")));

        dupSheet.setColumnWidth(5, 25);
        dupSheet.addCell(new Cell(new CellLocation("D1"),
                                       new CellValue("First - File")));

        dupSheet.setColumnWidth(5, 25);
        dupSheet.addCell(new Cell(new CellLocation("E1"),
                                       new CellValue("Description")));


        myBook.addWorksheet(dupSheet);

        int dupRow = 1;
        
        Iterator<PotentialCorruption> iter = list.iterator();
        
        while(iter.hasNext())
        {
            dupRow++;
            PotentialCorruption pc = iter.next();
            
            String cellNumber = "A" + dupRow;
            dupSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( pc.m_firstJarName )));

            cellNumber = "B" + dupRow;
            dupSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( pc.m_firstFileName )));

            cellNumber = "C" + dupRow;
            dupSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( pc.m_secondJarName )));

            cellNumber = "D" + dupRow;
            dupSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( pc.m_secondFileName )));

            cellNumber = "E" + dupRow;
            dupSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( pc.m_sDescription )));
        }
    }
}
