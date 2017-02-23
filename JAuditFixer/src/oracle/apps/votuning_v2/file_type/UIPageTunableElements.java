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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UIPageTunableElements
{
    public UIPageTunableElements(String sType, String theJarName, String thePageName,
                  String sTableId, String sFetchSize, String sAutoHeightRows,
                  String sRows, String sContentDelivery)
    {
        m_sType = sType;
        m_sJar = theJarName;
        m_sPage = thePageName;
        m_sTableId = sTableId;
        m_sFetchSize = sFetchSize;
        m_sAutoHeightRows = sAutoHeightRows;
        m_sRows = sRows;
        m_sContentDelivery = sContentDelivery;
    }

    String m_sJar;
    String m_sPage;
    String m_sType;
    String m_sTableId;
    String m_sFetchSize;
    String m_sAutoHeightRows;
    String m_sRows;
    String m_sContentDelivery;
    
    boolean potentialCorruption = false;


    public static HashMap<String, ArrayList<UIPageTunableElements>> m_tunablePageElements =
        new HashMap<String, ArrayList<UIPageTunableElements>>();

    public static void processPage(ZipFile zf, ZipEntry ze)
    {
        try
        {
            String theJarName = zf.getName();
            String thePageName = ze.getName();

            ArrayList<UIPageTunableElements> pageTunables = m_tunablePageElements.get(thePageName);
            if (pageTunables != null)
            {
                String description = "This page already was processed";

                if(pageTunables.size() > 0)
                {
                    UIPageTunableElements example = pageTunables.get(0);
                    for(int i = 0; i < pageTunables.size(); i++)
                    {
                        UIPageTunableElements x = pageTunables.get(i);
                        x.potentialCorruption = true;
                    }

                    PotentialCorruption.register(example.m_sJar, example.m_sPage, theJarName, thePageName, description);
                }
                else
                {
                    PotentialCorruption.register("no example", "no example", theJarName, thePageName, description);
                }
                
            }

            pageTunables = new ArrayList<UIPageTunableElements>();
            m_tunablePageElements.put(thePageName, pageTunables);

            XMLDocument doc =
                XMLParserHelper.getXMLDocument(zf.getInputStream(ze));

            NodeList nl1 = doc.getElementsByTagName("table");
            processTableList(theJarName, thePageName, pageTunables, nl1, false);

            NodeList nl2 = doc.getElementsByTagName("treeTable");
            processTableList(theJarName, thePageName, pageTunables, nl2, true);

            NodeList nl3 = doc.getElementsByTagName("selectOneChoice");
            processSelectOneChoiceList(theJarName, thePageName, pageTunables, nl3, "selectOneChoice");

            NodeList nl4 = doc.getElementsByTagName("inputComboboxListOfValues");
            processInputComboboxListOfValues(theJarName, thePageName, pageTunables, nl4, "inputComboboxListOfValues");
            

            //            record.setUiTagFetchSize(XMLParserHelper.getAttributeValue(child,
            //                                                                       "fetchSize"));
            //            record.setAutoHeightRows(XMLParserHelper.getAttributeValue(child,
            //                                                                       "autoHeightRows"));
            //            record.setUiTagRows(XMLParserHelper.getAttributeValue(child,
            //                                                                  "rows"));
            //            record.setContentDelivery(XMLParserHelper.getAttributeValue(child,
            //                                                                        "contentDelivery"));
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static private void processInputComboboxListOfValues(String theJarName, String thePageName,
                                         ArrayList<UIPageTunableElements> pageTunables,
                                         NodeList nl1, String sType)
    {
        for (int i = 0; i < nl1.getLength(); i++)
        {
            Node n = nl1.item(i);
            NamedNodeMap attrs = n.getAttributes();
            Node item = attrs.getNamedItem("fetchSize");
            String sFetchSize = null;
            if (item != null)
                sFetchSize = item.getNodeValue();

            item = attrs.getNamedItem("autoHeightRows");
            String sAutoHeightRows = null;
            if (item != null)
                sAutoHeightRows = item.getNodeValue();

            item = attrs.getNamedItem("rows");
            String sRows = null;
            if (item != null)
                sRows = item.getNodeValue();

            item = attrs.getNamedItem("contentDelivery");
            String sContentDelivery = null;
            if (item != null)
                sContentDelivery = item.getNodeValue();

            item = attrs.getNamedItem("id");
            String sTableId = item.getNodeValue();

            UIPageTunableElements uit =
                new UIPageTunableElements(sType, theJarName, thePageName, sTableId, sFetchSize,
                           sAutoHeightRows, sRows, sContentDelivery);
            pageTunables.add(uit);
        }
    }


    static private void processSelectOneChoiceList(String theJarName, String thePageName,
                                         ArrayList<UIPageTunableElements> pageTunables,
                                         NodeList nl1, String sType)
    {
        for (int i = 0; i < nl1.getLength(); i++)
        {
            Node n = nl1.item(i);
            NamedNodeMap attrs = n.getAttributes();
            Node item = attrs.getNamedItem("fetchSize");
            String sFetchSize = null;
            if (item != null)
                sFetchSize = item.getNodeValue();

            item = attrs.getNamedItem("autoHeightRows");
            String sAutoHeightRows = null;
            if (item != null)
                sAutoHeightRows = item.getNodeValue();

            item = attrs.getNamedItem("rows");
            String sRows = null;
            if (item != null)
                sRows = item.getNodeValue();

            item = attrs.getNamedItem("contentDelivery");
            String sContentDelivery = null;
            if (item != null)
                sContentDelivery = item.getNodeValue();

            item = attrs.getNamedItem("id");
            String sTableId = item.getNodeValue();

            UIPageTunableElements uit =
                new UIPageTunableElements(sType, theJarName, thePageName, sTableId, sFetchSize,
                           sAutoHeightRows, sRows, sContentDelivery);
            pageTunables.add(uit);
        }
    }


    static private void processTableList(String theJarName, String thePageName,
                                         ArrayList<UIPageTunableElements> pageTables,
                                         NodeList nl1, boolean isTreeTable)
    {
        for (int i = 0; i < nl1.getLength(); i++)
        {
            Node n = nl1.item(i);
            NamedNodeMap attrs = n.getAttributes();
            Node item = attrs.getNamedItem("fetchSize");
            String sFetchSize = null;
            if (item != null)
                sFetchSize = item.getNodeValue();

            item = attrs.getNamedItem("autoHeightRows");
            String sAutoHeightRows = null;
            if (item != null)
                sAutoHeightRows = item.getNodeValue();

            item = attrs.getNamedItem("rows");
            String sRows = null;
            if (item != null)
                sRows = item.getNodeValue();

            item = attrs.getNamedItem("contentDelivery");
            String sContentDelivery = null;
            if (item != null)
                sContentDelivery = item.getNodeValue();

            item = attrs.getNamedItem("id");
            String sTableId = item.getNodeValue();

            String sType = "table";
            if (isTreeTable)
                sType = "treeTable";

            UIPageTunableElements uit =
                new UIPageTunableElements(sType, theJarName, thePageName, sTableId, sFetchSize,
                           sAutoHeightRows, sRows, sContentDelivery);
            pageTables.add(uit);
        }
    }


    static public void writeExcelAnalysis(Workbook myBook)
    {
        int row = 1;

        // Create a worksheet
        Worksheet analysisSheet = new Worksheet("Page Tunable Info");
        myBook.addWorksheet(analysisSheet);

        analysisSheet.setColumnWidth(1, 25);
        analysisSheet.addCell(new Cell(new CellLocation("A1"),
                                       new CellValue("JAR")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("B1"),
                                       new CellValue("PAGE")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("C1"),
                                       new CellValue("TYPE")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("D1"),
                                       new CellValue("ID")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("E1"),
                                       new CellValue("FetchSize")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("F1"),
                                       new CellValue("AutoHeightRows")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("G1"),
                                       new CellValue("ContentDelivery")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("G1"),
                                       new CellValue("Rows")));


        Set<String> keys = m_tunablePageElements.keySet();
        Iterator<String> iter = keys.iterator();

        while (iter.hasNext())
        {
            String key = iter.next();
            ArrayList<UIPageTunableElements> list = m_tunablePageElements.get(key);

            for (int i = 0; i < list.size(); i++)
            {
                row++;

                UIPageTunableElements uit = list.get(i);

                String cellNumber = "A" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(uit.m_sJar)));

                cellNumber = "B" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(uit.m_sPage)));

                cellNumber = "C" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(uit.m_sType)));

                cellNumber = "D" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(uit.m_sTableId)));

                cellNumber = "E" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(uit.m_sFetchSize)));

                cellNumber = "F" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(uit.m_sAutoHeightRows)));

                cellNumber = "G" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(uit.m_sRows)));
            }
        }
        //        String m_sJar;
        //        String m_sPage;
        //        String m_sType;
        //        String m_sTableId;
        //        String m_sFetchSize;
        //        String m_sAutoHeightRows;
        //        String m_sRows;
        //        String m_sContentDelivery;

    }

}
