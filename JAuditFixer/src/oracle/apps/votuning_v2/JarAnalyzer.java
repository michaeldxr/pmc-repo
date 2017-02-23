package oracle.apps.votuning_v2;

import java.io.File;

import java.io.FileOutputStream;

import java.util.ArrayList;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.votuning_v2.file_type.BC4J_XCFG;
import oracle.apps.votuning_v2.file_type.DataControl;
import oracle.apps.votuning_v2.file_type.FusionFileInfo;
import oracle.apps.votuning_v2.file_type.PageDef;
import oracle.apps.votuning_v2.file_type.PageDefUsages;
import oracle.apps.votuning_v2.file_type.PageMap;
import oracle.apps.votuning_v2.file_type.UIPageTunableElements;
import oracle.apps.votuning_v2.file_type.PotentialCorruption;
import oracle.apps.votuning_v2.file_type.ViewObject;
import oracle.apps.votuning_v2.file_type.ViewUsage;

public class JarAnalyzer extends JoesBaseClass
{
    HashMap<String, FusionFileInfo> m_duplicatedFusionContent = new HashMap<String, FusionFileInfo>();
    HashMap<String, FusionFileInfo> m_fusionContent = new HashMap<String, FusionFileInfo>();
    
    ArrayList<String> jar_list = new ArrayList<String>();
    
    HashMap<String, ViewObject> viewObject_map = new HashMap<String, ViewObject>();
    
    ArrayList<ViewObject> viewObject_list = new ArrayList<ViewObject>();
    ArrayList<ViewUsage> viewUsage_list = new ArrayList<ViewUsage>();
    
    
    public JarAnalyzer()
    {
        super(JoesBaseClass.CRAWL_TYPE.JAR);
    }

    public static void main(String[] args)
    {
        JarAnalyzer ja = new JarAnalyzer();
        ja.markStartTime();

        if (args.length != 2 || args[0] == null || args[1] == null || args[0].equals("") || args[1].equals("")) {
            System.out.println("USAGE: jarAnalyzerForADFTuning.sh <crawlDirector> <doADE>");
            System.exit(1);
        }
        
        String crawlDirectory = args[0];
        Boolean doADE = new Boolean(args[1]);

        ja.start(crawlDirectory, doADE);
        
        ja.writeExcel();
        
        ja.markEndTime();
    }

    public void start(String crawlDirectory, Boolean doADE)
    {
        crawlDirectory(crawlDirectory, doADE);
    }



    public void writeExcel()
    {
        try
        {
            Workbook myBook = new Workbook("output");

            UIPageTunableElements.writeExcelAnalysis(myBook);
            PageMap.writeExcelAnalysis(myBook);
            PageDefUsages.writeExcelAnalysis(myBook);
            PageDef.writeExcelAnalysis(myBook);
            DataControl.writeExcelAnalysis(myBook);
            BC4J_XCFG.writeExcelAnalysis(myBook);

            PotentialCorruption.dumpSheet(myBook);

            // Create a worksheet
            Worksheet dupSheet = new Worksheet("Duplicate Content");
            dupSheet.setColumnWidth(1, 25);
            dupSheet.addCell(new Cell(new CellLocation("A1"),
                                           new CellValue("JAR")));

            dupSheet.setColumnWidth(2, 25);
            dupSheet.addCell(new Cell(new CellLocation("B1"),
                                           new CellValue("File")));

            dupSheet.setColumnWidth(3, 25);
            dupSheet.addCell(new Cell(new CellLocation("C1"),
                                           new CellValue("MD5")));

            dupSheet.setColumnWidth(4, 25);
            dupSheet.addCell(new Cell(new CellLocation("D1"),
                                           new CellValue("First - JAR")));

            dupSheet.setColumnWidth(5, 25);
            dupSheet.addCell(new Cell(new CellLocation("E1"),
                                           new CellValue("First - File")));



            myBook.addWorksheet(dupSheet);

            int dupRow = 1;
            
            Set<String> keySet = m_duplicatedFusionContent.keySet();
            Iterator<String> iter = keySet.iterator();
            
            while(iter.hasNext())
            {
                dupRow++;
                String key = iter.next();
                
                FusionFileInfo ffi = m_duplicatedFusionContent.get(key);
                FusionFileInfo first_ffi = m_fusionContent.get(key);

                String cellNumber = "A" + dupRow;
                dupSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( ffi.getJar() )));

                cellNumber = "B" + dupRow;
                dupSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( ffi.getPath() )));

                cellNumber = "C" + dupRow;
                dupSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( ffi.getDigest() )));

                cellNumber = "D" + dupRow;
                dupSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( first_ffi.getJar() )));

                cellNumber = "E" + dupRow;
                dupSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( first_ffi.getPath() )));
            }



            // Create a worksheet
            Worksheet analysisSheet = new Worksheet("VO Information");
            analysisSheet.setColumnWidth(1, 25);
            analysisSheet.addCell(new Cell(new CellLocation("A1"),
                                           new CellValue("JAR")));

            analysisSheet.setColumnWidth(2, 25);
            analysisSheet.addCell(new Cell(new CellLocation("B1"),
                                           new CellValue("File")));

            analysisSheet.setColumnWidth(3, 25);
            analysisSheet.addCell(new Cell(new CellLocation("C1"),
                                           new CellValue("Name")));

            analysisSheet.setColumnWidth(4, 25);
            analysisSheet.addCell(new Cell(new CellLocation("D1"),
                                           new CellValue("FetchSize")));

            myBook.addWorksheet(analysisSheet);

            
            for(int i = 0; i < viewObject_list.size(); i++)
            {
                int row = i+2;
                ViewObject vo = viewObject_list.get(i);

                String cellNumber = "A" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( vo.getJar() )));

                cellNumber = "B" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( vo.getFname() )));

                cellNumber = "C" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( vo.getName() )));

                cellNumber = "D" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( vo.getDefaultFetchSize() )));
            }



            // Create a worksheet
            analysisSheet = new Worksheet("ViewUsage Info");
            analysisSheet.setColumnWidth(1, 25);
            analysisSheet.addCell(new Cell(new CellLocation("A1"),
                                           new CellValue("JAR")));

            analysisSheet.setColumnWidth(2, 25);
            analysisSheet.addCell(new Cell(new CellLocation("B1"),
                                           new CellValue("AM")));

            analysisSheet.setColumnWidth(3, 25);
            analysisSheet.addCell(new Cell(new CellLocation("C1"),
                                           new CellValue("Usage")));

            analysisSheet.setColumnWidth(3, 25);
            analysisSheet.addCell(new Cell(new CellLocation("D1"),
                                           new CellValue("VO")));

            analysisSheet.setColumnWidth(4, 25);
            analysisSheet.addCell(new Cell(new CellLocation("E1"),
                                           new CellValue("FetchSize")));

            myBook.addWorksheet(analysisSheet);

            for(int i = 0; i < viewUsage_list.size(); i++)
            {
                int row = i+2;
                ViewUsage vu = viewUsage_list.get(i);

                String cellNumber = "A" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( vu.getJar() )));

                cellNumber = "B" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( vu.getApplicationModuleName() )));

                cellNumber = "C" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( vu.getUsageName() )));

                cellNumber = "D" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( vu.getViewObjectName() )));

                cellNumber = "E" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( vu.getFetchSize() )));
            }



            FileOutputStream out = new FileOutputStream("tuning.xlsx");
            XLSXCreator.create(myBook, out);
            out.close();
            
        } 
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void processFile(File fName, boolean bDoADE)
    {
        if(fName.getAbsolutePath().endsWith("Test.jar"))
            return;
        
        jar_list.add(fName.getAbsolutePath());
        
        try
        {
            ZipFile jar = new ZipFile(fName);
            Enumeration entries = jar.entries();
            
            while(entries.hasMoreElements())
            {
                ZipEntry ze = (ZipEntry) entries.nextElement();
                String name = ze.getName();
                
                FusionFileInfo newFFI = new FusionFileInfo(jar, ze);
                FusionFileInfo ffi = m_fusionContent.get(newFFI.getDigest());
                
                if(ffi == null)
                {
                    m_fusionContent.put(newFFI.getDigest(), newFFI);
                }
                else
                {
                    m_duplicatedFusionContent.put(newFFI.getDigest(), newFFI);
//                    System.out.println("****************************************************");
//                    System.out.println("Skipping file as it was already processed");
//                    System.out.println(newFFI.toString());
//                    System.out.println(ffi.toString());
//                    System.out.println("****************************************************");
                    continue;
                }
                
                FILE_TYPE t = getFileType(jar, ze);

                switch (t)
                {
                case BC4JXCFG:
                    BC4J_XCFG.processXcfgFile(jar, ze);
                break;

                case VIEW_OBJECT:
                    ViewObject vo = new ViewObject(jar, ze);
                    viewObject_list.add(vo);
                    
                    ViewObject tmp = viewObject_map.get(name);
                    if(tmp != null)
                    {
                        Long l1 = vo.getDefaultFetchSize();
                        Long l2 = tmp.getDefaultFetchSize();

                        if(l1 == null && l2 == null)
                        {
                            // ok, weird dups that have consistent tuning will calculate out same
                        }
                        else if(l1 != null && l2 != null && l1.longValue() == l2.longValue())
                        {
                            // ok, weird dups that have consistent tuning will calculate out same
                        }
                        else
                        {
//                            System.out.println("Wow, this view object is duplicated, and has different fetch sizes!!");
//                            System.out.println(name);
//                            System.out.println(tmp.getJar());
//                            System.out.println(vo.getJar());
//                            System.out.println("values are " +l1 +" and " + l2);

                            // These are View Objects that have same name and path.  
                            // but they have different fetchSize.  
                            // these present an obsticle for the program to calculate the optimal values correctly
                            
                            String desc = "There are two View Objects that have the same name and path, but they have different fetchSizes defined.  This will present an obstacle for the scanner to calculate optimal tuning values.  --> " +l1 +" and " +l2;
                            PotentialCorruption.register(tmp.getJar(), name, vo.getJar(), name, desc);
                        }
                    }
                    else
                    {
                        viewObject_map.put(name, vo);
                    }
                    break;
                
                case APPLICATION_MODULE:
                    ViewUsage.processViewUsages(jar, ze, viewUsage_list);
                    break;
                
                case UI_FILE:
                    UIPageTunableElements.processPage(jar, ze);
                    break;
                case PAGE_DEF:
                    PageDef.processPage(jar, ze);
                    break;
                case CPX_FILE:
                    PageMap.processCPXFile(jar, ze);
                    PageDefUsages.processCPXFile(jar, ze);
                    DataControl.processCPXFile(jar, ze);
                    break;
                }
//                System.out.println("Type is --> " + t);
            }
            
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.out.println(e.toString());
        }
        
    }

    @Override
    protected String getSummaryReportSubject()
    {
        return "Jar Analyzer for ADF Tuning";
    }

    @Override
    protected String getSummaryReport()
    {
        return "Not Implemented";
    }
}
