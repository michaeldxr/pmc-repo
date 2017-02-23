package oracle.apps.ppr.file_type;

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
                  String sTableId, String sValue)
    {
        m_sType = sType;
        m_sJar = theJarName;
        m_sPage = thePageName;
        m_sId = sTableId;
        m_sValue = sValue;
    }

    String m_sJar;
    String m_sPage;
    String m_sType;
    String m_sId;
    String m_sValue;

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
                //page has been parsed.
                return;
            }

            pageTunables = new ArrayList<UIPageTunableElements>();
            m_tunablePageElements.put(thePageName, pageTunables);

            XMLDocument doc =
                XMLParserHelper.getXMLDocument(zf.getInputStream(ze),false);
            
            if(doc == null)
                return;
            
            String type = "inputText";
            NodeList nl1 = doc.getElementsByTagName(type);
            process(theJarName, thePageName, pageTunables, nl1, type);

            type = "inputDate";
            NodeList nl2 = doc.getElementsByTagName(type);
            process(theJarName, thePageName, pageTunables, nl2,type);

            type = "selectOneChoice";
            NodeList nl3 = doc.getElementsByTagName(type);
            process(theJarName, thePageName, pageTunables, nl3, type);

            type = "selectBooleanCheckbox";
            NodeList nl4 = doc.getElementsByTagName(type);
            process(theJarName, thePageName, pageTunables, nl4,type);
            
            

        } catch (Exception e)
        {
            System.out.println("Unable to get XML Documents for this page: "+ze.getName());
            e.printStackTrace();
        }
    }

    static private void process(String theJarName, String thePageName,
                                         ArrayList<UIPageTunableElements> pageTunables,
                                         NodeList nl1,String sType)
    {
        for (int i = 0; i < nl1.getLength(); i++)
        {
            Node n = nl1.item(i);
            NamedNodeMap attrs = n.getAttributes();
            Node item = null;
            
            item = attrs.getNamedItem("autoSubmit");
            
            //filter out ui components with autoSubmit = false, in order to reduce the size of m_tunablePageElements map
            if(item ==null)
                continue;
            
            String sAutoSubmit = item.getNodeValue();

            if(sAutoSubmit == null || !sAutoSubmit.equals("true"))
                continue;
            
            item = attrs.getNamedItem("valueChangeListener");
            if(item !=null)
                continue;
            
            item = attrs.getNamedItem("id");
            String sId = item.getNodeValue();

            item = attrs.getNamedItem("value");
            String sValue = null;
            if(item != null){
                sValue = item.getNodeValue();
                
                if(sType.equals("inputDate") || sType.equals("inputText")){
                  if(sValue.startsWith("#{row.bindings")){
                      item = attrs.getNamedItem("label");
                      if(item != null)
                          sValue = item.getNodeValue();
                      else{
                        item = attrs.getNamedItem("shortDesc");
                        if(item != null)
                            sValue = item.getNodeValue();
                      }
                  }
                }else if(sType.equals("selectOneChoice") || sType.equals("selectBooleanCheckbox")){
                  if(sValue.startsWith("#{row.bindings")){
                      item = attrs.getNamedItem("shortDesc");
                      if(item != null)
                          sValue = item.getNodeValue();
                  }
                }
            }
            else{
                //"UI Component " + sId + " doesn't have a value attribute"
                continue;
            }
            
            UIPageTunableElements uit =
                new UIPageTunableElements(sType, theJarName, thePageName, sId, sValue);
            pageTunables.add(uit);
        }
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
        return m_sId;
    }

    public void setSId(String m_sId) {
        this.m_sId = m_sId;
    }

    public String getSValue() {
        return m_sValue;
    }
}
