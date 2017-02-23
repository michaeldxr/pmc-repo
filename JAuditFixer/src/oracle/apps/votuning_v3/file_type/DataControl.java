package oracle.apps.votuning_v3.file_type;

import java.util.ArrayList;
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
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DataControl
{
    private String sJarName;
    private String sCpxPath;
    private String sId;
    private String sPackage;
    private String sConfiguration;
    
    boolean potentialCorruption = false;
    
    public static HashMap<String, DataControl> m_dataControls = new HashMap<String, DataControl>();
    
    public DataControl()
    {
        super();
    }

    public static void processCPXFile(ZipFile zf, ZipEntry ze)
    {
        try
        {
            String theJarName = zf.getName();
            String theCpxName = ze.getName();
            
            XMLDocument doc = XMLParserHelper.getXMLDocument(zf.getInputStream(ze));

            NodeList nl1 = doc.getElementsByTagName("dataControlUsages");
            
            if(nl1 == null || nl1.getLength() == 0)
            {
                return;
            }
            else if(nl1.getLength() > 1)
            {
                System.out.println("CORRUPTION:  Too many dataControlUsages --> " +theJarName + "    " +theCpxName);
                return;
            }
            
            XMLElement eDataControlUsages = (XMLElement)nl1.item(0);
            NodeList nl2 = eDataControlUsages.getElementsByTagName("BC4JDataControl");

            for(int i = 0; i < nl2.getLength(); i++)
            {
                Node n = nl2.item(i);
                
                Node nId = n.getAttributes().getNamedItem("id");
                String sId = nId.getNodeValue();

                Node nPackage = n.getAttributes().getNamedItem("Package");
                String sPackage = nPackage.getNodeValue();

                Node nConfiguration = n.getAttributes().getNamedItem("Configuration");
                String sConfiguration = nConfiguration.getNodeValue();
                
                DataControl dc = new DataControl();
                dc.sJarName = theJarName;
                dc.sCpxPath = theCpxName;
                dc.sId = sId;
                dc.sPackage = sPackage;
                dc.sConfiguration = sConfiguration;
                
                String key = theCpxName + sPackage + "." +sId;
                
                DataControl tmp = m_dataControls.get(key);
                if(tmp != null)
                {
//                    if(dc.sPackage.equals(tmp.sPackage) && dc.sConfiguration.equals(tmp.sConfiguration))
//                    {
//                        // really this is just use of the same AM, so ok
//                        continue;
//                    }

                    String type = "Duplicate Data Control ID found";
                    String description = "The data control ID is defined twice, but differently.";
                    PotentialCorruption.register(dc.sJarName, dc.sCpxPath, tmp.sJarName, tmp.sCpxPath, type, description);
                    tmp.potentialCorruption = true;
                    continue;
                }
                
                m_dataControls.put(key, dc);
            }
        }
            catch (Exception e)
            {
                e.printStackTrace();
            }
    }


    static public void writeExcelAnalysis(Workbook myBook)
    {
        int row = 1;

        // Create a worksheet
        Worksheet analysisSheet = new Worksheet("Data Controls");
        myBook.addWorksheet(analysisSheet);

        analysisSheet.setColumnWidth(1, 25);
        analysisSheet.addCell(new Cell(new CellLocation("A1"),
                                       new CellValue("JAR")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("B1"),
                                       new CellValue("CPX")));
        
        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("C1"),
                                       new CellValue("ID")));
        
        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("D1"),
                                       new CellValue("PACKAGE")));
            
        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("E1"),
                                       new CellValue("CONFIGURATION")));
                
        
        Set <String> keys = m_dataControls.keySet();
        Iterator <String> iter = keys.iterator();
        
        while(iter.hasNext())
        {
            row++;
            
            String key = iter.next();
            DataControl dc = m_dataControls.get(key);

            String cellNumber = "A" + row;
            analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( dc.sJarName)));

            cellNumber = "B" + row;
            analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( dc.sCpxPath )));

            cellNumber = "C" + row;
            analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( dc.sId )));

            cellNumber = "D" + row;
            analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( dc.sPackage )));

            cellNumber = "E" + row;
            analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( dc.sConfiguration )));
        }
        }

    public void setSJarName(String sJarName) {
        this.sJarName = sJarName;
    }

    public String getSJarName() {
        return sJarName;
    }

    public void setSCpxPath(String sCpxPath) {
        this.sCpxPath = sCpxPath;
    }

    public String getSCpxPath() {
        return sCpxPath;
    }

    public void setSId(String sId) {
        this.sId = sId;
    }

    public String getSId() {
        return sId;
    }

    public void setSPackage(String sPackage) {
        this.sPackage = sPackage;
    }

    public String getSPackage() {
        return sPackage;
    }

    public void setSConfiguration(String sConfiguration) {
        this.sConfiguration = sConfiguration;
    }

    public String getSConfiguration() {
        return sConfiguration;
    }

    public void setPotentialCorruption(boolean potentialCorruption) {
        this.potentialCorruption = potentialCorruption;
    }

    public boolean isPotentialCorruption() {
        return potentialCorruption;
    }

    public static void setDataControls(HashMap<String, DataControl> m_dataControls) {
        DataControl.m_dataControls = m_dataControls;
    }

    public static HashMap<String, DataControl> getDataControls() {
        return m_dataControls;
    }
}
