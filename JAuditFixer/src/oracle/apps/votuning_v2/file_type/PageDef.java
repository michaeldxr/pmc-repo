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

import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PageDef
{    
    static public class PageDefIterator
    {
        public String sJarName;
        public String sPageDefName;
        public String sBinds;
        public String sDataControl;
        public String sId;
        public String sRangeSize;
        
        boolean potentialCorruption = false;
    }
    
    static public HashMap<String, ArrayList<PageDefIterator>> m_pageDefIterators = 
        new HashMap<String, ArrayList<PageDefIterator>>();


    public PageDef()
    {
        super();
    }

    public static void processPage(ZipFile zf, ZipEntry ze)
    {
        String theJarName = zf.getName();
        String thePageDefName = ze.getName();
        

        try
        {
            ArrayList<PageDefIterator> pageDefIters = m_pageDefIterators.get(thePageDefName);
            
            if (pageDefIters != null)
            {
                String description = "The Page Def appears to already have been processed";

                if(pageDefIters.size() > 0)
                {
                    PageDefIterator example = pageDefIters.get(0);
                    for(int i = 0; i < pageDefIters.size(); i++)
                    {
                        PageDefIterator pdi = pageDefIters.get(i);
                        pdi.potentialCorruption = true;
                    }

                    PotentialCorruption.register(example.sJarName, example.sPageDefName, theJarName, thePageDefName, description);
                }
                else
                    PotentialCorruption.register("was empty", "was empty", theJarName, thePageDefName, description);
            }

            pageDefIters = new ArrayList<PageDefIterator>();
            m_pageDefIterators.put(thePageDefName, pageDefIters);

            XMLDocument doc = XMLParserHelper.getXMLDocument(zf.getInputStream(ze));

            NodeList nl1 = doc.getElementsByTagName("executables");
            int l = nl1.getLength();
            if(l != 1)
            {
                String description = "PageDef has " +l + " number of executable sections";
                description += "this pageDef appears to not have the expected executables section";

                PotentialCorruption.register("", "", theJarName, thePageDefName, description);
            }
            
            if(l == 0)
                return;
            
            XMLElement e = (XMLElement)nl1.item(0);
            NodeList il = e.getElementsByTagName("iterator");
            
            for(int i = 0; i < il.getLength(); i++)
            {
                Node pdi = il.item(i);
                
                NamedNodeMap attrs = pdi.getAttributes();
                
                PageDefIterator x = new PageDefIterator();

                x.sJarName = theJarName;
                x.sPageDefName = thePageDefName;
                x.sBinds = attrs.getNamedItem("Binds").getNodeValue();
                x.sId = attrs.getNamedItem("id").getNodeValue();
                
                Node attrNode = attrs.getNamedItem("RangeSize");
                if(attrNode != null)
                {
                    x.sRangeSize = attrNode.getNodeValue();
                }

                attrNode = attrs.getNamedItem("DataControl");
                if(attrNode == null)
                {
                    String description = "PageDef has an iterator"   +x.sId   +" that does not have an assigned DataControl.";
                    PotentialCorruption.register("", "", theJarName, thePageDefName, description);

                    continue;
                }
                x.sDataControl = attrNode.getNodeValue();
                
                pageDefIters.add(x);
            }
        } 
        catch (Exception e)
        {
            System.out.println("*********   ERROR   **************************************");
            System.out.println(theJarName);
            System.out.println(thePageDefName);
            e.printStackTrace();
            System.out.println("*********   ERROR   **************************************");
        }
    }



    static public void writeExcelAnalysis(Workbook myBook)
    {
        int row = 1;

        // Create a worksheet
        Worksheet analysisSheet = new Worksheet("PageDef Iterators");
        myBook.addWorksheet(analysisSheet);

        analysisSheet.setColumnWidth(1, 25);
        analysisSheet.addCell(new Cell(new CellLocation("A1"),
                                       new CellValue("JAR")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("B1"),
                                       new CellValue("PAGEDEF")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("C1"),
                                       new CellValue("ID")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("D1"),
                                       new CellValue("DATACONTROL")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("E1"),
                                       new CellValue("BINDS")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("F1"),
                                       new CellValue("RANGESIZE")));

        Set<String> keys = m_pageDefIterators.keySet();
        Iterator<String> iter = keys.iterator();

        while (iter.hasNext())
        {
            String key = iter.next();
            ArrayList<PageDefIterator> list = m_pageDefIterators.get(key);

            for (int i = 0; i < list.size(); i++)
            {
                row++;

                PageDefIterator pdi = list.get(i);

                String cellNumber = "A" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sJarName)));

                cellNumber = "B" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sPageDefName)));

                cellNumber = "C" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sId)));

                cellNumber = "D" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sDataControl)));

                cellNumber = "E" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sBinds)));

                cellNumber = "F" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sRangeSize)));

            }
        }
    }
}
