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

import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PageDef
{
    static public class PageDefBinding
    {
        public String sJarName;
        public String sPageDefName;
        public String sBinds;
        public String sDataControl;
        public String sIterId;
        public String sRangeSize;
        public String sBindingID;
        public String sBindingType;
        public String sListBinding;
        
        boolean potentialCorruption = false;

        public String getSBinds() {
            return sBinds;
        }

        public String getSDataControl() {
            return sDataControl;
        }

        public String getSIterId() {
            return sIterId;
        }

        public String getSRangeSize() {
            return sRangeSize;
        }

        public String getSBindingID() {
            return sBindingID;
        }

        public void setSBindingID(String sBindingID) {
            this.sBindingID = sBindingID;
        }

        public String getSBindingType() {
            return sBindingType;
        }

        public void setSListBinding(String sUses) {
            this.sListBinding = sUses;
        }

        public String getSListBinding() {
            return sListBinding;
        }
    }
    
    static public class PageDefIter
    {
        public String sBinds;
        public String sDataControl;
        public String sId;
        public String sRangeSize;
    }
    
    static public HashMap<String, ArrayList<PageDefBinding>> m_pageDefBindings = 
        new HashMap<String, ArrayList<PageDefBinding>>();
    
    static public HashMap<String, PageDefIter> m_pageDefIterators = 
        new HashMap<String, PageDefIter>();
    
    private static String theJarName;
    public static String thePageDefName;
    
    public PageDef()
    {
        super();
    }

    public static void processPage(ZipFile zf, ZipEntry ze)
    {
        theJarName = zf.getName();
        thePageDefName = ze.getName();
        m_pageDefIterators.clear();

        try
        {   
            ArrayList<PageDefBinding> pageDefBindings = m_pageDefBindings.get(thePageDefName);
            
            if (pageDefBindings != null)
            {
                String type = "PageDef has been parsed";
                String description = "The Page Def appears to already have been processed";

                if(pageDefBindings.size() > 0)
                {
                    PageDefBinding example = pageDefBindings.get(0);
                    for(int i = 0; i < pageDefBindings.size(); i++)
                    {
                        PageDefBinding pdi = pageDefBindings.get(i);
                        pdi.potentialCorruption = true;
                    }

                    PotentialCorruption.register(example.sJarName, example.sPageDefName, theJarName, thePageDefName, type, description);
                }
                else
                    PotentialCorruption.register("was empty", "was empty", theJarName, thePageDefName, type, description);
            } 

            pageDefBindings = new ArrayList<PageDefBinding>();
            m_pageDefBindings.put(thePageDefName, pageDefBindings);           

            XMLDocument doc = XMLParserHelper.getXMLDocument(zf.getInputStream(ze));


            //Find, parse and initialize iterators
            NodeList nl1 = doc.getElementsByTagName("executables");
            int l = nl1.getLength();
            if(l != 1)
            {
                String type = "Multiple executable sections found";
                String description = "PageDef has " + l + " number of executable sections";
                description += "this pageDef appears to not have the expected executables section";

                PotentialCorruption.register("", "", theJarName, thePageDefName, type, description);
            }
            
            if(l == 0)
                return;
            
            PageDefIter iter;
            
            XMLElement e = (XMLElement)nl1.item(0);
            NodeList il = e.getElementsByTagName("iterator");
            
            for(int i = 0; i < il.getLength(); i++)
            {
                iter = new PageDefIter();
                Node pdi = il.item(i);
                
                NamedNodeMap attrs = pdi.getAttributes();
                
                Node attrNode = attrs.getNamedItem("Binds");
                if(attrNode == null)
                {
                    String type = "Iterator has no bindings associated";
                    String description = "PageDef has an iterator"   +iter.sId   +" that does not have a binding assigned.";
                    PotentialCorruption.register("", "", theJarName, thePageDefName, type, description);

                    continue;
                }
                
                iter.sBinds = attrs.getNamedItem("Binds").getNodeValue();
                
                iter.sId = attrs.getNamedItem("id").getNodeValue();
                
                attrNode = attrs.getNamedItem("RangeSize");
                if(attrNode != null)
                {
                    iter.sRangeSize = attrNode.getNodeValue();
                }

                attrNode = attrs.getNamedItem("DataControl");
                if(attrNode == null)
                {
                    String type = "Iterator has no Data Control assigned";
                    String description = "PageDef has an iterator"   +iter.sId   +" that does not have an assigned DataControl.";
                    PotentialCorruption.register("", "", theJarName, thePageDefName, type, description);

                    continue;
                }
                iter.sDataControl = attrNode.getNodeValue();
                
                m_pageDefIterators.put(iter.sId, iter);
            }
            
            
            //Bindings Section
            NodeList nl2 = doc.getElementsByTagName("bindings");
            int l2 = nl2.getLength();
            
            if(l2 != 1)
            {
                String type = "Multiple bindings sections found";
                String description = "PageDef has " +l + " number of bindings sections";
                description += "this pageDef appears to not have the expected bindings section";

                PotentialCorruption.register("", "", theJarName, thePageDefName, type, description);
            }
            
            if(l2 == 0)
                return;
            
            e = (XMLElement)nl2.item(0);
            populateBindingsData(e, "list", pageDefBindings);
            populateBindingsData(e, "tree", pageDefBindings);
            populateBindingsData(e, "listOfValues", pageDefBindings);
            populateBindingsData(e, "table", pageDefBindings);
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
    
    static void populateBindingsData(XMLElement bindings, String tag, ArrayList<PageDefBinding> pageDefBindings){
        PageDefBinding x;
        String id;
        String IterBindingID;
        String uses = null;
        Node pdi;       
        NamedNodeMap attrs;
        PageDefIter iterator = null;
        boolean isLOV = false;
                   
        NodeList il = bindings.getElementsByTagName(tag);
   
        if(tag.equals("list") || tag.equals("listOfValues")){
            isLOV = true;
        }
        
        for(int i = 0; i < il.getLength(); i++){
             id = null;
             IterBindingID = null;
             uses = null;        
          
            pdi = il.item(i);       
            attrs = pdi.getAttributes();
            id =attrs.getNamedItem("id").getNodeValue();
            IterBindingID = attrs.getNamedItem("IterBinding").getNodeValue();
            
            if(isLOV){
                if(attrs.getNamedItem("StaticList") != null && attrs.getNamedItem("StaticList").getNodeValue().equals("true")){
                    IterBindingID = null;
                    continue;
                }
                
                if(attrs.getNamedItem("Uses") != null)
                    uses = attrs.getNamedItem("Uses").getNodeValue();
                else if(attrs.getNamedItem("ListIter") != null)
                    IterBindingID = attrs.getNamedItem("ListIter").getNodeValue();
                else 
                    IterBindingID = null;
            }
            
            //Ignore static list
            if(IterBindingID != null)
                iterator = m_pageDefIterators.get(IterBindingID);
            
            if(iterator != null){
                x = new PageDefBinding();
                x.sBinds = iterator.sBinds;
                x.sDataControl = iterator.sDataControl;
                x.sRangeSize = iterator.sRangeSize;
                x.sIterId = iterator.sId;
                x.sJarName = theJarName;
                x.sPageDefName = thePageDefName;
                x.sBindingID = id;
                x.sBindingType = tag;
                x.sListBinding = uses;
                
                pageDefBindings.add(x);
            }else{
                String type = "Missing Iterator";
                String description = "Can't find corresponding iterator " + IterBindingID + " for binding " + id;

                PotentialCorruption.register("", "", theJarName, thePageDefName, type, description);
            }
            
            
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
                                       new CellValue("BINDING")));
        
        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("D1"),
                                       new CellValue("TYPE")));
        
        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("E1"),
                                       new CellValue("ITERATOR")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("F1"),
                                       new CellValue("DATACONTROL")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("G1"),
                                       new CellValue("BINDS")));

        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("H1"),
                                       new CellValue("LOV_VA")));
        
        analysisSheet.setColumnWidth(2, 25);
        analysisSheet.addCell(new Cell(new CellLocation("I1"),
                                       new CellValue("RANGESIZE")));

        Set<String> keys = m_pageDefBindings.keySet();
        Iterator<String> iter = keys.iterator();

        while (iter.hasNext())
        {
            String key = iter.next();
            ArrayList<PageDefBinding> list = m_pageDefBindings.get(key);

            for (int i = 0; i < list.size(); i++)
            {
                row++;

                PageDefBinding pdi = list.get(i);

                String cellNumber = "A" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sJarName)));

                cellNumber = "B" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sPageDefName)));

                cellNumber = "C" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sBindingID)));
                
                cellNumber = "D" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sBindingType)));
                
                cellNumber = "E" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sIterId)));

                cellNumber = "F" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sDataControl)));

                cellNumber = "G" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sBinds)));

                cellNumber = "H" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sListBinding)));
                
                cellNumber = "I" + row;
                analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                               new CellValue(pdi.sRangeSize)));

            }
        }
    }
}
