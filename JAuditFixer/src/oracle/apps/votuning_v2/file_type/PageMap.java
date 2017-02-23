package oracle.apps.votuning_v2.file_type;

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

public class PageMap
{
    String jar;
    String cpx;
    String path;
    String usageId;
    
    public PageMap(String theJarName, String theCpxName, String thePath, String theUsageId)
    {
        super();
        
        jar = theJarName;
        cpx = theCpxName;
        path = thePath;
        usageId = theUsageId;
    }
    
    static public HashMap<String, ArrayList<PageMap>> m_pageMap = new HashMap<String, ArrayList<PageMap>>();
    
    public static void processCPXFile(ZipFile zf, ZipEntry ze)
    {
        try
        {
            String theJarName = zf.getName();
            String theCpxName = ze.getName();

            XMLDocument doc = XMLParserHelper.getXMLDocument(zf.getInputStream(ze));

            NodeList nl1 = doc.getElementsByTagName("pageMap");
            
            if(nl1 == null || nl1.getLength() == 0)
            {
                //System.out.println("No Page Maps found for file --> " +theJarName + "    " +theCpxName);
                return;
            }
            else if(nl1.getLength() > 1)
            {
                PotentialCorruption.register(theJarName, theCpxName, "", "", "Too many page maps found in this file.");
                return;
            }
            
            Node nPageMap = nl1.item(0);
            NodeList nl2 = nPageMap.getChildNodes();

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
                
                Node nUsageId = n.getAttributes().getNamedItem("usageId");
                String sUsageId = nUsageId.getNodeValue();

                PageMap newPm = new PageMap(theJarName, theCpxName, sPath, sUsageId);

                ArrayList<PageMap> pmList = m_pageMap.get(sUsageId);
                if(pmList != null)
                {
                    pmList.add(newPm);
                }
                else
                {
                    pmList = new ArrayList<PageMap>();
                    pmList.add(newPm);
                    m_pageMap.put(sUsageId, pmList);
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
        retVal += usageId +" maps to " + path + "\n";
        return retVal;
    }

    static public void writeExcelAnalysis(Workbook myBook)
    {
        int row = 1;

        // Create a worksheet
        Worksheet analysisSheet = new Worksheet("PageMap Info");

        analysisSheet.setColumnWidth(1, 25);
        analysisSheet.addCell(new Cell(new CellLocation("A1"),
                                       new CellValue("JAR")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("B1"),
                                       new CellValue("CPX")));

        analysisSheet.setColumnWidth(3, 25);
        analysisSheet.addCell(new Cell(new CellLocation("C1"),
                                       new CellValue("Usage")));

        analysisSheet.setColumnWidth(4, 25);
        analysisSheet.addCell(new Cell(new CellLocation("D1"),
                                       new CellValue("Path")));

        analysisSheet.setColumnWidth(5, 25);
        analysisSheet.addCell(new Cell(new CellLocation("E1"),
                                       new CellValue("Potential Corruption")));

        myBook.addWorksheet(analysisSheet);


        Set<String> keys = m_pageMap.keySet();
        Iterator<String> keyIter = keys.iterator();
        
//        static public HashMap<String, ArrayList<PageMap>> m_pageMap = new HashMap<String, ArrayList<PageMap>>();

        while(keyIter.hasNext())
        {
            String key = keyIter.next();
            ArrayList<PageMap> pmList = m_pageMap.get(key);

            boolean potentialCorruption = pmList.size() != 1;
            
            for(int i = 0; i < pmList.size(); i++)
            {
                row++;

                PageMap pm = pmList.get(i);
                
                String cellNumber = "A" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( pm.getJar() )));

                cellNumber = "B" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( pm.getCpx() )));

                cellNumber = "C" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( pm.getUsageId() )));

                cellNumber = "D" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( pm.getPath() )));

                cellNumber = "E" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( Boolean.toString(potentialCorruption) )));
            }

        }
        
    }

    public String getJar()
    {
        return jar;
    }

    public String getCpx()
    {
        return cpx;
    }

    public String getPath()
    {
        return path;
    }

    public String getUsageId()
    {
        return usageId;
    }

    public static HashMap<String, ArrayList<PageMap>> getPageMap()
    {
        return m_pageMap;
    }
}
