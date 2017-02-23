package oracle.apps;

import java.io.File;

import java.io.FileOutputStream;

import java.util.HashMap;

import java.util.Iterator;
import java.util.Set;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.EntityResolver;

public class TransientAttributeAnalyzer 
{
    int m_currentRow = 2;

    int m_numViewLinks = 0;
    int m_numVOs = 0;
    int m_numFlexVOs = 0;

    int m_numReadOnlyEOBasedVOs = 0;
    int m_numUpdateableEOBasedVOs = 0;
    int m_numTransientVOs = 0;
    int m_numMaxFetchSizeZero = 0;

    int m_numReadOnlyEOBasedVOs_uptransattrs = 0;
    int m_numUpdateableEOBasedVOs_uptransattrs = 0;
    int m_numTransientVOs_uptransattrs = 0;
    int m_numMaxFetchSizeZero_uptransattrs = 0;

    HashMap<String, Integer> m_transVO_maxFetchSizesMap = new HashMap<String, Integer>();
    int m_numReadOnlyEOBasedVOs_with_uptransattrs = 0;
    int m_numUpdateableEOBasedVOs_with_uptransattrs = 0;
    int m_numTransientVOs_with_uptransattrs = 0;

    int m_numViewAttributes = 0;
    int m_numViewAttributesTransientVOs = 0;
    int m_numViewAttributesUpdateableEOBasedVOs = 0;
    int m_numViewAttributesReadOnlyEOBasedVOs = 0;
    int m_numViewAttributesMaxFetchSizeZero = 0;

    int m_numTransAttributes = 0;
    int m_numTransAttributesReadOnly = 0;

    

    public TransientAttributeAnalyzer() {
        super();

    }

    public void buildSpreadsheet(String sCrawlLocation, String sOutputFileName) 
    {
        crawlDirectory(sCrawlLocation);

    }

    private void processVO(File f) {
        try
        {
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
            System.out.println("Reviewing file:  " +f.getCanonicalPath());
            DOMParser parser = new DOMParser();
            parser.setErrorStream(System.out);
            parser.setEntityResolver(new NullEntityResolver());
            parser.showWarnings(true);
            parser.setValidationMode(DOMParser.NONVALIDATING);
            
            parser.parse("file:" +f.getCanonicalPath());
            
            XMLDocument doc = parser.getDocument();


            // If VO has maxFetchSize = 0 or not
            boolean isInsertOnly  = false;
            
            // If VO is marked to passivate all or not
            boolean isPassivateAll = false;
            
            NodeList byTagName = doc.getElementsByTagName("ViewObject");
            NamedNodeMap VOAttrs = byTagName.item(0).getAttributes();
            
            Node passivateAllNode = VOAttrs.getNamedItem("Passivate");
            Node maxFetchSizeNode = VOAttrs.getNamedItem("MaxFetchSize");
            
            if(passivateAllNode != null && passivateAllNode.getNodeValue().equals("All"))
                isPassivateAll = true;
            
            if(maxFetchSizeNode != null && maxFetchSizeNode.getNodeValue().equals("0"))
                isInsertOnly = true;

            NodeList entityUsages = doc.getElementsByTagName("EntityUsage");

            // If VO has EOs or not            
            boolean isTransientVO = true;
            
            // If VO has updateable EOs or not
            boolean isReadOnly    = true;
            

            for(int i = 0; i < entityUsages.getLength(); i++)
            {
                isTransientVO = false;
                
                Node node = entityUsages.item(i);
                NamedNodeMap attributes = node.getAttributes();
                
                Node readOnlyFlag = attributes.getNamedItem("ReadOnly");
                
                if(readOnlyFlag == null || readOnlyFlag.getNodeValue().equals("false"))
                {
                    isReadOnly = false;
                }
            }


            NodeList viewAttributes = doc.getElementsByTagName("ViewAttribute");
            int count = 0;
            int numTransAttr = 0;
            int numTransAttrRO = 0;
            
            for(int i = 0; i < viewAttributes.getLength(); i++)
            {
                count++;

                Node node = viewAttributes.item(i);
                NamedNodeMap attributes = node.getAttributes();
                Node entryAttr = attributes.getNamedItem("EntityAttrName");
                Node updateAttr = attributes.getNamedItem("IsUpdateable");
                
                if(entryAttr == null)
                {
                    numTransAttr++;
                    if(updateAttr != null && updateAttr.getNodeValue().equals("false"))
                    {
                        numTransAttrRO++;
                    }
                }
            }



            m_numViewAttributes += count;
            m_numTransAttributes += numTransAttr;
            m_numTransAttributesReadOnly += numTransAttrRO;
            
            int numUpTransAttr = numTransAttr - numTransAttrRO;

            if(isTransientVO == true)
            {
                m_numTransientVOs++;
                m_numViewAttributesTransientVOs += count;

                m_numTransientVOs_uptransattrs += numUpTransAttr;

                //m_transVO_maxFetchSizesMap
//                if(maxFetchSizeNode != null && maxFetchSizeNode.getNodeValue().equals("0"))
//                    isInsertOnly = true;
                
                if(numUpTransAttr > 0)
                {
                    m_numTransientVOs_with_uptransattrs++;
                    
                    String key = "null";
                    if(maxFetchSizeNode != null)
                        key = maxFetchSizeNode.getNodeValue();
                    Integer value = m_transVO_maxFetchSizesMap.get(key);
                    if(value == null)
                        value = new Integer(1);
                    else
                        value = value+1;
                    
                    m_transVO_maxFetchSizesMap.put(key, value);
                }
            }
            else
            {
                if(isReadOnly)
                {
                    m_numReadOnlyEOBasedVOs++;
                    
                    if(numUpTransAttr > 0)
                        m_numReadOnlyEOBasedVOs_with_uptransattrs++;

                    
                    m_numViewAttributesReadOnlyEOBasedVOs += count;

                    m_numReadOnlyEOBasedVOs_uptransattrs += numUpTransAttr;
                }
                else
                {
                    m_numUpdateableEOBasedVOs++;
                    
                    if(numUpTransAttr > 0)
                        m_numUpdateableEOBasedVOs_with_uptransattrs++;

                    m_numViewAttributesUpdateableEOBasedVOs += count;

                    m_numUpdateableEOBasedVOs_uptransattrs += numUpTransAttr;
                }
            }

            if(isInsertOnly)
            {
                m_numMaxFetchSizeZero++;
                m_numViewAttributesMaxFetchSizeZero += count;

                m_numMaxFetchSizeZero_uptransattrs += numUpTransAttr;
            }
            



        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }


    private void crawlDirectory(String path) 
    {
        if(path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
            
        if(listOfFiles == null)
            return;
        
        for(int i = 0; i <listOfFiles.length; i++)
        {
            if(listOfFiles[i].isFile()) 
            {
                String sName = listOfFiles[i].getAbsolutePath();
                if(sName.endsWith("VO.xml"))
                {
                    if(sName.contains("adfmsrc") || sName.contains("/test/") || sName.contains("/modelTest/") || sName.contains("/association/") ||
                       sName.contains("view/CreditMemoTaxLineAveragePctVO.xml") ||
                       sName.contains("view/LineOrderingRuleManageUIVOToLineOrderByUIVO.xml") ||
                       sName.contains("view/AutoAccountingManageUIVOToAccountingDefaultSegmentUIVO.xml"))
                    {
                        System.out.println("Warning:  Skipping --> " +sName);
                        continue;
                    }
                    if(sName.contains("/link/") || sName.contains("/viewLink/")) 
                    {
                        m_numViewLinks++;
                        continue;
                    }
                    
                    m_numVOs++;
                    if(sName.contains("/flex/") || sName.contains("/publicFlex/"))
                    {
                        m_numFlexVOs++;                        
                    }
                    else
                    {
                        processVO(listOfFiles[i]);
                    }
//                    m_jsffCount++;
//                    processUI(listOfFiles[i]);
                }
            }
            else if(listOfFiles[i].isDirectory()) {
                crawlDirectory(listOfFiles[i].getAbsolutePath());
            }
            else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    public static void main(String[] args) 
    {
        if(args.length != 2 || args[0] == null || args[1] == null || args[0].equals("") || args[1].equals("")) 
        {
            System.out.println("Usage:  createTransientAttributeSpreadsheet.sh <path to your code in /ade_autofs> <output file name, including path>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        String sOutpFilename = args[1];
        
        TransientAttributeAnalyzer x = new TransientAttributeAnalyzer();
    //        x.buildSpreadsheet("/ade_autofs/ade_fusion_linux/FUSIONAPPS_PT.FINCOMM6A_LINUX.rdd/LATEST/fusionapps/fin/components/payables/exm/", "/scratch/albowicz/access_xlsx/exm.xlsx");
        x.buildSpreadsheet(sCrawlDir, sOutpFilename);

        System.out.println("Number of view links:     " +x.m_numViewLinks);
        System.out.println("Number of view objects:   " +x.m_numVOs);
        System.out.println("Number of flex VOs:       " +x.m_numFlexVOs);

        System.out.println("\nNumber of RO EO based VOs:          " +x.m_numReadOnlyEOBasedVOs);
        System.out.println("Number of RO EO based VOs with updateable Transient Attrs: " +x.m_numReadOnlyEOBasedVOs_with_uptransattrs);
        
        System.out.println("\nNumber of Updateable EO based VOs:  " +x.m_numUpdateableEOBasedVOs);
        System.out.println("Number of Updateable EO based VOs with updateable Transient Attrs: " +x.m_numUpdateableEOBasedVOs_with_uptransattrs);

        System.out.println("\nNumber of Transient VOs:            " +x.m_numTransientVOs);
        System.out.println("Number of Transient VOs with updateable Transient Attrs: " +x.m_numTransientVOs_with_uptransattrs);

        System.out.println("\nNumber of MaxFetchSize=0 VOs:       " +x.m_numMaxFetchSizeZero);

        System.out.println("Number of UpTransAttrs for RO EO Based VOs:                     " +x.m_numReadOnlyEOBasedVOs_uptransattrs);
        System.out.println("Number of UpTransAttrs for Up EO Based VOs:                     " +x.m_numUpdateableEOBasedVOs_uptransattrs);
        System.out.println("Number of UpTransAttrs for Trans VOs      :                     " +x.m_numTransientVOs_uptransattrs);
        System.out.println("\nNumber of UpTransAttrs for MaxFetch=0 VOs :                     " +x.m_numMaxFetchSizeZero_uptransattrs +"\n");

        
        System.out.println("Number of View Attributes:                    " +x.m_numViewAttributes);
        System.out.println("Number of View Attributes on Trans VO:        " +x.m_numViewAttributesTransientVOs);
        System.out.println("Number of View Attributes on Updateable VO:   " +x.m_numViewAttributesUpdateableEOBasedVOs);
        System.out.println("Number of View Attributes on Read Only VO:    " +x.m_numViewAttributesReadOnlyEOBasedVOs);
        System.out.println("Number of View Attributes on MaxFetchSize = 0:" +x.m_numViewAttributesMaxFetchSizeZero);

        System.out.println("Number of Transient Attributes:               " +x.m_numTransAttributes);
        System.out.println("Number of Transient Attributes Read Only:     " +x.m_numTransAttributesReadOnly);

        System.out.println("\n\nThe distribution of maxFetchSize for Transient VOs is:");

        Set<String> keySet = x.m_transVO_maxFetchSizesMap.keySet();
        Iterator<String> iter = keySet.iterator();
        
        while(iter.hasNext())
        {
            String key = iter.next();
            Integer value = x.m_transVO_maxFetchSizesMap.get(key);
            
            System.out.println("For Trans VOs, maxFetchSize=" +key +" occurred " +value.toString());
        }
    }

}
