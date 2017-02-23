package oracle.apps.inlinestyle;

import java.io.File;

import java.util.Iterator;
import java.util.Set;

import oracle.apps.NullEntityResolver;
import oracle.apps.TransientAttributeAnalyzer;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class StyleAnalyzer {
    public StyleAnalyzer() {
        super();
    }

    int m_nullPanelHeader = 0;

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
                if(sName.endsWith("jsff") || sName.endsWith("jspx"))
                {
                    System.out.println("File:  " +sName);
                    process(listOfFiles[i]);
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
        if(args.length != 1 || args[0] == null || args[0].equals("")) 
        {
            System.out.println("Usage:  styleAnalyzer.sh <path to your code in ade>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        StyleAnalyzer sa = new StyleAnalyzer();
        sa.crawlDirectory(sCrawlDir);
        
        System.out.println("Null Panel Header cases:   " +sa.m_nullPanelHeader);
    }



    private void process(File f) 
    {
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
            


            NodeList nullPanelHeaders = doc.getElementsByTagName("panelHeader");

            for(int i = 0; i < nullPanelHeaders.getLength(); i++)
            {
                Node node = nullPanelHeaders.item(i);
                NamedNodeMap attributes = node.getAttributes();
                
                Node text = attributes.getNamedItem("text");
                
                String textValue = text.getNodeValue();
                if(textValue != null)
                    textValue = textValue.trim();
                
                if(textValue == null || "".equals(textValue)) 
                {
                    m_nullPanelHeader++;
                }
            }
            
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }


}
