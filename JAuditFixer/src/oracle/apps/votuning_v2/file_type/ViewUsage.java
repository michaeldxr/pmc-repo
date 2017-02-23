package oracle.apps.votuning_v2.file_type;

import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ViewUsage
{
    String jar;
    String applicationModuleName;
    String usageName;
    String viewObjectName;

    Long fetchSize;

    public ViewUsage(String sJar, String sAppModule, String sUsageName, String sVO, Long fs)
    {
        super();
        
        jar = sJar;
        applicationModuleName = sAppModule;
        usageName = sUsageName;
        viewObjectName = sVO;
        fetchSize = fs;
    }
    
    static public void processViewUsages(ZipFile zf, ZipEntry ze, ArrayList<ViewUsage> viewUsages)
    {
        try
        {
            String theJarName = zf.getName();
            String theAMName = ze.getName();

            XMLDocument doc = XMLParserHelper.getXMLDocument(zf.getInputStream(ze));
            NodeList nl = doc.getElementsByTagName("ViewUsage");

            for(int i = 0; i < nl.getLength(); i++)
            {
                Node n = nl.item(i);

                Node nName = n.getAttributes().getNamedItem("Name");
                String usageName = nName.getNodeValue();
                
                Node nVO = n.getAttributes().getNamedItem("ViewObjectName");
                String sVO = nVO.getNodeValue();

                Long defaultFetchSize = null;
                Node nFS = n.getAttributes().getNamedItem("FetchSize");
                if (nFS != null)
                {
                    String fs = nFS.getNodeValue();
                    if (fs != null && "".equals(fs) == false)
                    {
                        defaultFetchSize = new Long(fs);
                    }
                }
                
                ViewUsage vu = new ViewUsage(theJarName, theAMName, usageName, sVO, defaultFetchSize);
                viewUsages.add(vu);
            }


        } catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    public String getJar()
    {
        return jar;
    }

    public String getApplicationModuleName()
    {
        return applicationModuleName;
    }

    public String getUsageName()
    {
        return usageName;
    }

    public String getViewObjectName()
    {
        return viewObjectName;
    }

    public Long getFetchSize()
    {
        return fetchSize;
    }
}
