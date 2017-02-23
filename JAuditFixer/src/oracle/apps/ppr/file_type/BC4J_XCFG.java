package oracle.apps.ppr.file_type;

import java.io.Serializable;

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

public class BC4J_XCFG implements Serializable
{
    static final long serialVersionUID = 1;
    private String sJarName;
    private String sXcfg;
    private String sName;
    private String sAppName;
    private String sJboProject;
    
    public boolean involvedInPotentialCorruption = false;

    public static HashMap<String, BC4J_XCFG> m_appModules = new HashMap<String, BC4J_XCFG>();


    public BC4J_XCFG()
    {
        super();
    }

    public static void processXcfgFile(ZipFile zf, ZipEntry ze)
    {
        try
        {
            String theJarName = zf.getName();
            String theXcfgName = ze.getName();
            
            XMLDocument doc =
                XMLParserHelper.getXMLDocument(zf.getInputStream(ze),false);

            NodeList nl1 = doc.getElementsByTagName("AppModuleConfig");

            if (nl1 == null || nl1.getLength() == 0)
            {  
                return;
            }

            for (int i = 0; i < nl1.getLength(); i++)
            {
                Node n = nl1.item(i);

                Node nName = n.getAttributes().getNamedItem("name");
                String sName = nName.getNodeValue();

                Node nAppName = n.getAttributes().getNamedItem("ApplicationName");
                String sAppName = nAppName.getNodeValue();
                
                Node nJboProject = n.getAttributes().getNamedItem("jbo.project");
                if(nJboProject == null)
                {
                    String type = "No jbo.project field";
                    String description = "The jbo.project field is not set for " + sAppName;
                    continue;
                }

                String sJboProject = nJboProject.getNodeValue();
                
                String k = sJboProject +"." +sName;
                
                BC4J_XCFG tmp = m_appModules.get(k);
                
                if(tmp != null)
                {
                    tmp.involvedInPotentialCorruption = true;
                    
                    String type = "Duplicate App Module found";
                    String description = "The App Module " +k +" is defined twice";

//                    System.out.println("******BC4J.XCFG****************************************************");
//                    System.out.println("Potential corruption, the application is duplicated definition");
//                    System.out.println(theJarName);
//                    System.out.println(theXcfgName);
//                    System.out.println(sName);
//                    System.out.println(sAppName);
//                    System.out.println(sJboProject);
//                    System.out.println("******BC4J.XCFG****************************************************");
                    continue;
                }
                
                BC4J_XCFG x = new BC4J_XCFG();
                x.sJarName = theJarName;
                x.sXcfg = theXcfgName;
                x.sName  = sName;
                x.sAppName = sAppName;
                x.sJboProject = sJboProject;
                
                m_appModules.put(k, x);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    static public void writeExcelAnalysis(Workbook myBook)
    {
        int row = 1;

        // Create a worksheet
        Worksheet analysisSheet = new Worksheet("AppModules");
        myBook.addWorksheet(analysisSheet);

        analysisSheet.setColumnWidth(1, 25);
        analysisSheet.addCell(new Cell(new CellLocation("A1"),
                                       new CellValue("JAR")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("B1"),
                                       new CellValue("XCFG")));
        
        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("C1"),
                                       new CellValue("NAME")));
        
        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("D1"),
                                       new CellValue("APPNAME")));
            
        
        Set <String> keys = m_appModules.keySet();
        Iterator <String> iter = keys.iterator();
        
        while(iter.hasNext())
        {
            row++;
            
            String key = iter.next();
            BC4J_XCFG x = m_appModules.get(key);

            String cellNumber = "A" + row;
            analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( x.sJarName)));

            cellNumber = "B" + row;
            analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( x.sXcfg )));

            cellNumber = "C" + row;
            analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( x.sName )));

            cellNumber = "D" + row;
            analysisSheet.addCell(new Cell(new CellLocation(cellNumber), new CellValue( x.sAppName )));

        }
        }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public void setSJarName(String sJarName) {
        this.sJarName = sJarName;
    }

    public String getSJarName() {
        return sJarName;
    }

    public void setSXcfg(String sXcfg) {
        this.sXcfg = sXcfg;
    }

    public String getSXcfg() {
        return sXcfg;
    }

    public void setSName(String sName) {
        this.sName = sName;
    }

    public String getSName() {
        return sName;
    }

    public void setSAppName(String sAppName) {
        this.sAppName = sAppName;
    }

    public String getSAppName() {
        return sAppName;
    }

    public void setSJboProject(String sJboProject) {
        this.sJboProject = sJboProject;
    }

    public String getSJboProject() {
        return sJboProject;
    }

    public void setInvolvedInPotentialCorruption(boolean involvedInPotentialCorruption) {
        this.involvedInPotentialCorruption = involvedInPotentialCorruption;
    }

    public boolean isInvolvedInPotentialCorruption() {
        return involvedInPotentialCorruption;
    }

    public static void setAppModules(HashMap<String, BC4J_XCFG> m_appModules) {
        BC4J_XCFG.m_appModules = m_appModules;
    }

    public static HashMap<String, BC4J_XCFG> getAppModules() {
        return m_appModules;
    }
}
