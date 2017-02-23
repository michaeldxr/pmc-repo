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

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UIPageTunableElements
{
    public UIPageTunableElements(String sType, String theJarName, String thePageName,
                  String sTableId, String sFetchSize, String sAutoHeightRows,
                  String sRows, String sContentDelivery, String sIsWrappedByPanelStretchLayout, String sValue)
    {
        m_sType = sType;
        m_sJar = theJarName;
        m_sPage = thePageName;
        m_sTableId = sTableId;
        m_sFetchSize = sFetchSize;
        m_sAutoHeightRows = sAutoHeightRows;
        m_sRows = sRows;
        m_sContentDelivery = sContentDelivery;       
        m_isWrappedByPanelStretchLayout = sIsWrappedByPanelStretchLayout;
        m_sValue = sValue;
    }

    String m_sJar;
    String m_sPage;
    String m_sType;
    String m_sTableId;
    String m_sFetchSize;
    String m_sAutoHeightRows;
    String m_sRows;
    String m_sContentDelivery;
    String m_isWrappedByPanelStretchLayout;
    String m_sValue;
    
    boolean potentialCorruption = false;


    public static HashMap<String, ArrayList<UIPageTunableElements>> m_tunablePageElements =
        new HashMap<String, ArrayList<UIPageTunableElements>>();

    public static void processPage(ZipFile zf, ZipEntry ze)
    {        
        String theJarName = null;
        String thePageName = null;
        try
        {
            theJarName = zf.getName();
            thePageName = ze.getName();

            ArrayList<UIPageTunableElements> pageTunables = m_tunablePageElements.get(thePageName);
            if (pageTunables != null)
            {
                String type = "Page has been parsed";
                String description = "This page already was processed";

                if(pageTunables.size() > 0)
                {
                    UIPageTunableElements example = pageTunables.get(0);
                    for(int i = 0; i < pageTunables.size(); i++)
                    {
                        UIPageTunableElements x = pageTunables.get(i);
                        x.potentialCorruption = true;
                    }

                    PotentialCorruption.register(example.m_sJar, example.m_sPage, theJarName, thePageName,  type, description);
                }
                else
                {
                    PotentialCorruption.register("no example", "no example", theJarName, thePageName, type, description);
                }
                
            }

            pageTunables = new ArrayList<UIPageTunableElements>();
            m_tunablePageElements.put(thePageName, pageTunables);

            XMLDocument doc =
                XMLParserHelper.getXMLDocument(zf.getInputStream(ze),false);

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
            String type = "XML corruption";
            String description = "Unable to get XML Documents for this page";
            PotentialCorruption.register("", "", theJarName, thePageName, type, description);
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

            item = attrs.getNamedItem("value");
            String sValue = null;
            if(item != null){
                sValue = item.getNodeValue();
            }
            else{
                String type = "Failed to get the binding associated";
                String description = "InputComboboxListOfValues " + sTableId + " doesn't have a value attribute";
                PotentialCorruption.register(theJarName, thePageName,"","", type, description);
                continue;
            }
            
            UIPageTunableElements uit =
                new UIPageTunableElements(sType, theJarName, thePageName, sTableId, sFetchSize,
                           sAutoHeightRows, sRows, sContentDelivery, isDirectChildOfPanelStretchLayout(n),sValue);
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

            item = attrs.getNamedItem("value");
            String sValue = null;
            if(item != null){
                sValue = item.getNodeValue();
            }
            else{
                String type = "Failed to get the binding associated";
                String description = "SelectOneChoice " + sTableId + " doesn't have a value attribute";
                PotentialCorruption.register(theJarName, thePageName,"","", type, description);
                continue;
            }

            UIPageTunableElements uit =
                new UIPageTunableElements(sType, theJarName, thePageName, sTableId, sFetchSize,
                           sAutoHeightRows, sRows, sContentDelivery, isDirectChildOfPanelStretchLayout(n),sValue);
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
            if (isTreeTable){
                sType = "treeTable";                                
            }
            
            item = attrs.getNamedItem("value");
            String sValue = null;
            if(item != null){
                sValue = item.getNodeValue();
            }
            else{
                String type = "Failed to get the binding associated";
                String description = sType + " " + sTableId + " doesn't have a value attribute";
                PotentialCorruption.register(theJarName, thePageName,"","", type, description);
                continue;
            }  
            
            UIPageTunableElements uit =
                new UIPageTunableElements(sType, theJarName, thePageName, sTableId, sFetchSize,
                           sAutoHeightRows, sRows, sContentDelivery, isDirectChildOfPanelStretchLayout(n), sValue);
            pageTables.add(uit);
        }
    }
    
    private static String isDirectChildOfPanelStretchLayout(Node x)
    {
        Node p = x.getParentNode();
        
        if(p == null)
            return "No";
        
        String parentName = p.getNodeName();

        if("af:panelStretchLayout".equals(parentName))
        {
            // disqualify panelStretchLayou cases that are dimensionsFrom = children
            NamedNodeMap nnm = p.getAttributes();
            Node df = nnm.getNamedItem("dimensionsFrom");
            if(df != null && "children".equals(df.getNodeValue()))
                return "No";
            
            return "Yes";
        }

        if("fnd:applicationsTable".equals(parentName) ||
           "fnd:applicationsTreeTable".equals(parentName) || 
            "f:facet".equals(parentName))
            return isDirectChildOfPanelStretchLayout(p);

        return "No";
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
                                       new CellValue("Rows")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("H1"),
                                       new CellValue("WRAPPED BY PANEL STRETCH LAYOUT")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("I1"),
                                       new CellValue("Value")));
        
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
                
                cellNumber = "H" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(uit.m_isWrappedByPanelStretchLayout)));
                cellNumber = "I" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(uit.m_sValue)));
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

    public String getSJar() {
        return m_sJar;
    }

    public String getSPage() {
        return m_sPage;
    }

    public String getSType() {
        return m_sType;
    }

    public String getSTableId() {
        return m_sTableId;
    }

    public String getSFetchSize() {
        return m_sFetchSize;
    }

    public String getSAutoHeightRows() {
        return m_sAutoHeightRows;
    }

    public String getSRows() {
        return m_sRows;
    }

    public String getSContentDelivery() {
        return m_sContentDelivery;
    }

    public String getIsWrappedByPanelStretchLayout() {
        return m_isWrappedByPanelStretchLayout;
    }

    public void setSTableId(String m_sTableId) {
        this.m_sTableId = m_sTableId;
    }

    public String getSValue() {
        return m_sValue;
    }
}
