package oracle.apps.votuning_v4.file_type;

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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PageDefUsages
{
    String jar;
    String cpx;
    String path;
    String id;
    
    public PageDefUsages(String theJarName, String theCpxName, String thePath, String theId)
    {
        super();
        
        jar = theJarName;
        cpx = theCpxName;
        path = thePath;
        id = theId;
    }
    
    static public HashMap<String, PageDefUsages> m_pageDefUsageMap = new HashMap<String, PageDefUsages>();

    public static void processCPXFile(ZipFile zf, ZipEntry ze)
    {
        try
        {
            String theJarName = zf.getName();
            String theCpxName = ze.getName();

            XMLDocument doc = XMLParserHelper.getXMLDocument(zf.getInputStream(ze));

            NodeList nl1 = doc.getElementsByTagName("pageDefinitionUsages");
            
            if(nl1 == null || nl1.getLength() == 0)
            {
                return;
            }
            else if(nl1.getLength() > 1)
            {
                
                System.out.println("CORRUPTION:  Too many Page Def Usages found for file --> " +theJarName + "    " +theCpxName);
                String type = "Multiple PageDefinitionUsages found";
                PotentialCorruption.register(theJarName, theCpxName, "", "", type, "More than one pageDefinitionUsages sections found");
                return;
            }
            
            Node nPageDefUsages = nl1.item(0);
            NodeList nl2 = nPageDefUsages.getChildNodes();

            for(int i = 0; i < nl2.getLength(); i++)
            {
                Node n = nl2.item(i);
                String s1 = n.getNodeName();
                short s2 = n.getNodeType();
                String s3 = n.getNodeValue();

                if("page".equals(s1) == false)
                    continue;
                
                Node nPath = n.getAttributes().getNamedItem("path");
                String sPath = nPath.getNodeValue();
                
                Node nId = n.getAttributes().getNamedItem("id");
                String sId = nId.getNodeValue();

                PageDefUsages newPdu = new PageDefUsages(theJarName, theCpxName, sPath, sId);

                PageDefUsages pdu = m_pageDefUsageMap.get(sId);
                if(pdu == null)
                {
                    m_pageDefUsageMap.put(sId, newPdu);
                }
                else
                {
                    String type = "Duplicate Page Def Usages found";
                    String desc = "CORRUPTION:  Duplicate Page Def Usages found";
                    desc += "\n";
                    desc += newPdu.id;
                    desc += "\n";
                    desc += pdu.id;

                    PotentialCorruption.register(pdu.jar, pdu.id, theJarName, theCpxName, type, desc);
                }
            }


        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public String toString()
    {
        String retVal = jar + " has " + cpx +"\n";
        retVal += id +" maps to " + path + "\n";
        return retVal;
    }


    static public void writeExcelAnalysis(Workbook myBook)
    {
        int row = 1;

        // Create a worksheet
        Worksheet analysisSheet = new Worksheet("PageDefUsages Info");
        myBook.addWorksheet(analysisSheet);

        analysisSheet.setColumnWidth(1, 25);
        analysisSheet.addCell(new Cell(new CellLocation("A1"),
                                       new CellValue("JAR")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("B1"),
                                       new CellValue("CPX")));
        
        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("C1"),
                                       new CellValue("PATH")));
        
        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("D1"),
                                       new CellValue("ID")));
            
        
        Set <String> keys = m_pageDefUsageMap.keySet();
        Iterator <String> iter = keys.iterator();
        
        while(iter.hasNext())
        {
            row++;
            
            String key = iter.next();
            PageDefUsages pdu = m_pageDefUsageMap.get(key);

            String cellNumber = "A" + row;
            analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( pdu.jar )));

            cellNumber = "B" + row;
            analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( pdu.cpx )));

            cellNumber = "C" + row;
            analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( pdu.path )));

            cellNumber = "D" + row;
            analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( pdu.id )));
        }
        }


    public String getJar() {
        return jar;
    }

    public String getCpx() {
        return cpx;
    }

    public String getPath() {
        return path;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
