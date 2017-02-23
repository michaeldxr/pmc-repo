package oracle.apps.extens;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.NullEntityResolver;
import oracle.apps.rowCountAnalyzer.SetRowCountThreshold;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class ImplicitPers extends JoesBaseClass
{
    static final String columnPersistSetting = "displayIndex visible width";
    static final String tablePersistSetting = "filterVisible";

    int m_page_reviewCount = 0;
    int m_fixCount = 0;
    private String m_sSummaryReport = "";

    public ImplicitPers() 
    {
        super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF);
    }

    protected String getSummaryReport() 
    {
        m_sSummaryReport = "For this run I reviewed a total of " +m_page_reviewCount +" pages.\n" + m_sSummaryReport;
        m_sSummaryReport = "For this run I fixed a total of    " +m_fixCount +" pages.\n" + m_sSummaryReport;
        
        return m_sSummaryReport;
    }


    private static final String idRegEx = "id=\"" +"(" + identifier +")\"";
    private static final Pattern idPattern = Pattern.compile(idRegEx, Pattern.MULTILINE);

    private String getId(String line)
    {
        Matcher m = idPattern.matcher(line);
        if(m.find())
        {
            String s = m.group(1);
            return s;
        }

        return null;
    }

    private void fixPage(File fName, boolean bDoADE, ArrayList<String> tableFixList, ArrayList<String> columnFixList) throws Exception
    {
        boolean fixed = false;
        
        FileReader reader = new FileReader( fName );
        BufferedReader input = new BufferedReader( reader );
        FileReader reader2 = new FileReader( fName );
        BufferedReader input2 = new BufferedReader( reader2 );

        String line;

        StringBuffer sb = new StringBuffer();

        int lineNo = 1;

        while( (line = input.readLine()) != null )
        {
            input2.skip(line.length());
            input2.mark(1024);
            int c1 = input2.read();
            int c2 = input2.read();
            input2.reset();

            String newline = "\n";
            if(c1 == -1)
            {
                newline = "";
            }
            else if(c1 == '\n')
            {
                input2.read();
            }
            else if(c1 == '\r' && c2 != '\n')
            {
                newline = "\r";
                input2.read();
            }
            else if(c1 == '\r' && c2 == '\n')
            {
                newline = "\r\n";
                input2.read();
                input2.read();
            }

            while(line.contains(">") == false && input.ready())
            {
                String extraLine = input.readLine();
                line += newline+extraLine;
                lineNo++;

                input2.skip(extraLine.length());
                input2.mark(1024);
                c1 = input2.read();
                c2 = input2.read();
                input2.reset();

                newline = "\n";
                if(c1 == -1)
                {
                    newline = "";
                }
                else if(c1 == '\n')
                {
                    input2.read();
                }
                else if(c1 == '\r' && c2 != '\n')
                {
                    newline = "\r";
                    input2.read();
                }
                else if(c1 == '\r' && c2 == '\n')
                {
                    newline = "\r\n";
                    input2.read();
                    input2.read();
                }
            }


            if(line.contains("<af:table"))
            {
                String id = getId(line);
                
                if(tableFixList.contains(id))
                {
                    int index = line.indexOf("persist=\"");
                    if(index == -1)
                    {
                        index = line.indexOf("<af:table ") +10;
                        line = line.substring(0, index) + "persist=\"" +tablePersistSetting +"\" " +line.substring(index);
                        fixed = true;
                    }
                    else
                    {
                        index = line.indexOf("persist=\"") +9;
                        int index2 = line.indexOf("\"", index);
                        
                        line = line.substring(0, index) + tablePersistSetting +line.substring(index2);
                        fixed = true;
                    }
                }
            }
            else if(line.contains("<af:column"))
            {
                String id = getId(line);
                
                if(columnFixList.contains(id))
                {
                    int index = line.indexOf("persist=\"");
                    if(index == -1)
                    {
                        index = line.indexOf("<af:column ") +11;
                        line = line.substring(0, index) + "persist=\"" +columnPersistSetting   +"\" " +line.substring(index);
                        fixed = true;
                    }
                    else
                    {
                        index = line.indexOf("persist=\"") +9;
                        int index2 = line.indexOf("\"", index);
                        
                        line = line.substring(0, index) + columnPersistSetting +line.substring(index2);
                        fixed = true;
                    }
                }
            }


            sb.append(line);
            
            sb.append(newline);

            lineNo++;
        }

        reader.close();

        if(fixed == false)
            return;

        m_fixCount++;
        
        if(bDoADE == false)
        {
            return;
        }

        m_sSummaryReport += "Applying fix to file: " +fName.getAbsolutePath() +"\n";

        checkoutAndDelete(fName);

        FileWriter fw = new FileWriter(fName.getAbsolutePath());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(sb.toString());
        bw.close();

        m_fixCount++;
        checkinAndDiff(fName);        
    }


    protected void processFile(File fName, boolean bDoADE) 
    {
        m_page_reviewCount++;
        System.out.println("Checking Page:  " +fName.toString());

        ArrayList<String> tableFixList = new ArrayList<String>();
        ArrayList<String> columnFixList = new ArrayList<String>();

        try {
            DOMParser parser = new DOMParser();
            parser.setDebugMode(true);
            parser.setErrorStream(System.out);
            parser.setEntityResolver(new NullEntityResolver());
            parser.showWarnings(true);
            parser.setValidationMode(DOMParser.NONVALIDATING);

            parser.parse("file:" + fName);

            XMLDocument doc = parser.getDocument();
            NodeList tableList = doc.getElementsByTagName("table");

            for (int i = 0; i < tableList.getLength(); i++) 
            {
                Node tableNode = tableList.item(i);
                NamedNodeMap attributes = tableNode.getAttributes();

                Node text = attributes.getNamedItem("id");
                if(text == null)
                    continue;
                String tableId = text.getNodeValue();
                if(tableId == null || "".equals(tableId))
                    continue;
                
                
                Node value = attributes.getNamedItem("value");
                if(value == null || "".equals(value))
                {
                    System.out.println("Weird, table does not seem to be bound to a collection");
                    System.exit(-1);
                    //skip it.
                    continue;
                }
                
                
                value = attributes.getNamedItem("persist");
                String valueText = null;
                if(value != null)
                valueText = value.getNodeValue();
                
                if(valueText == null || "filterVisible".equals(valueText) == false)
                {
                    // non-standard persist setting, need to apply fix
                    tableFixList.add(tableId);
                }
            }



            NodeList columnList = doc.getElementsByTagName("column");

            for (int i = 0; i < columnList.getLength(); i++) 
            {
                Node columnNode = columnList.item(i);
                NamedNodeMap attributes = columnNode.getAttributes();

                Node text = attributes.getNamedItem("id");
                if(text == null)
                    continue;
                String columnId = text.getNodeValue();
                if(columnId == null || "".equals(columnId))
                    continue;
                
                Node value = attributes.getNamedItem("headerText");
                if(value == null || "".equals(value))
                {
                    //skip it.
                    continue;
                }
                
                value = attributes.getNamedItem("persist");
                String valueText = null;
                if(value != null)
                valueText = value.getNodeValue();
                
                if(valueText == null || "width visible".equals(valueText) == false)
                {
                    // non-standard persist setting, need to apply fix
                    columnFixList.add(columnId);
                }
            }

        if(tableFixList.size() == 0 && columnFixList.size() == 0)
        {
            return;
        }
            
        fixPage(fName, bDoADE, tableFixList, columnFixList);

        } catch (Exception e) {
            e.printStackTrace();
            logException(fName.getAbsolutePath(), e);
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +fName.getAbsolutePath());
            return;
        }

    }

    protected String getSummaryReportSubject() 
    {
        return "SCRIPT:  Implicit Personalization";
    }

    public static void main(String[] args) 
    {
        if (args.length != 2 || args[0] == null || args[1] == null || args[0].equals("") || args[1].equals("")) {
            System.out.println("Usage:  enableImplicitPersonalization.sh <path to your code in ade> <do ade>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        
        Boolean bDoADE = new Boolean(args[1]);
        ImplicitPers x = new ImplicitPers();

        try {
            if (bDoADE.booleanValue())
                x.startDiffLog();

            if(sCrawlDir.endsWith("jsff") || sCrawlDir.endsWith("jspx"))
                x.processFile(new File(sCrawlDir), bDoADE);
            else
                x.crawlDirectory(sCrawlDir, bDoADE);
            
            if (bDoADE.booleanValue())
                x.closeDiffLog();
        } catch (Exception e) {
            e.printStackTrace();
            x.logException(sCrawlDir, e);
        }

        x.logUsageReport(sCrawlDir, !bDoADE);
    }
}
