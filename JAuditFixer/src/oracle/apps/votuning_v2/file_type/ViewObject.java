package oracle.apps.votuning_v2.file_type;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;

public class ViewObject
{
    Long defaultFetchSize;
    String jar;
    String fname;
    String name;
    
    public ViewObject(ZipFile zf, ZipEntry ze)
    {
        super();

        try
        {
            jar = zf.getName();
            fname = ze.getName();
            
            
            XMLDocument doc = XMLParserHelper.getXMLDocument(zf.getInputStream(ze));
            Node root = doc.getDocumentElement();

            Node nName = root.getAttributes().getNamedItem("Name");
            name = nName.getNodeValue();

            Node nFS = root.getAttributes().getNamedItem("FetchSize");
            
            if(nFS != null)
            {
                String fs = nFS.getNodeValue();
                if(fs != null && "".equals(fs) == false)
                {
                    defaultFetchSize = new Long(fs);
            }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }


    public Long getDefaultFetchSize()
    {
        return defaultFetchSize;
    }

    public String getJar()
    {
        return jar;
    }

    public String getFname()
    {
        return fname;
    }

    public String getName()
    {
        return name;
    }
}
