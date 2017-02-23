package oracle.apps.votuning_v3.file_type;

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
      String usageName = null, sPath = null,sVO = null,fs = null;
      Node n = null, nName = null, nVO = null, nPath = null;
      
      Long defaultFetchSize = null;
        try
        {
            String theJarName = zf.getName();
            String theAMName = ze.getName();

            XMLDocument doc = XMLParserHelper.getXMLDocument(zf.getInputStream(ze));
            NodeList nl = doc.getElementsByTagName("ViewUsage");

            for(int i = 0; i < nl.getLength(); i++)
            {
                n = nl.item(i);

                nName = n.getAttributes().getNamedItem("Name");
                usageName = nName.getNodeValue();
                
                nVO = n.getAttributes().getNamedItem("ViewObjectName");
                sVO = nVO.getNodeValue();

                Node nFS = n.getAttributes().getNamedItem("FetchSize");
                if (nFS != null)
                {
                    fs = nFS.getNodeValue();
                    if (fs != null && "".equals(fs) == false)
                    {
                        defaultFetchSize = new Long(fs);
                    }
                }
                else{
                  defaultFetchSize = null;
                }

                viewUsages.add(new ViewUsage(theJarName, theAMName, usageName, sVO, defaultFetchSize));
            }

      //Parsing nested AM
      nl = doc.getElementsByTagName("AppModuleUsage");
      ArrayList<AppModuleUsage> al_subAM = new ArrayList<AppModuleUsage>();
      AppModuleUsage.m_subAM.put(theJarName+theAMName, al_subAM);
        
      for (int i = 0; i < nl.getLength(); i++) {
        n = nl.item(i);

        nName = n.getAttributes().getNamedItem("Name");
        usageName = nName.getNodeValue();

        nPath = n.getAttributes().getNamedItem("FullName");
        sPath = nPath.getNodeValue();
        
        al_subAM.add(new AppModuleUsage(theJarName, theAMName, usageName, sPath));      
      }


    } catch (Exception e) {
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
