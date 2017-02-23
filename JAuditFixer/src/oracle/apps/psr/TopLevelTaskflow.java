package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileOutputStream;

import java.io.FileReader;
import java.io.FileWriter;

import oracle.apps.NullEntityResolver;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TopLevelTaskflow extends JoesBaseClass
{
    private String m_sSummaryReport = "";
    int m_menuFileReviewCount = 0;
    int m_taskFlowDefReviewCount = 0;
    int m_fixCount = 0;

    public TopLevelTaskflow(JoesBaseClass.CRAWL_TYPE crawL_TYPE)
    {
        super(crawL_TYPE);
    }

    protected void processFile(File fName, boolean bDoADE)
    {
        m_menuFileReviewCount++;
        
        int i = fName.getAbsolutePath().indexOf("/WEB-INF");
        String base_path = fName.getAbsolutePath().substring(0, i);
        
        try {
            DOMParser parser = new DOMParser();
            parser.setDebugMode(true);
            parser.setErrorStream(System.out);
            parser.setEntityResolver(new NullEntityResolver());
            parser.showWarnings(true);
            parser.setValidationMode(DOMParser.NONVALIDATING);

            parser.parse("file:" + fName.getCanonicalPath());

            XMLDocument doc = parser.getDocument();
            NodeList menu = doc.getElementsByTagName("menu");
            
            if(menu == null || menu.getLength() != 1)
            {
                System.out.println("ERROR: Error while processing file.  Either zero or more than 1 menu tags were found.  review carefully if the script updated it. " +fName.getAbsolutePath());
                return;
            }
            
            System.out.println("Checking Menu file -->  " +fName.getAbsolutePath());
            
            NodeList itemNodeList = doc.getElementsByTagName("itemNode");
            if(itemNodeList == null || itemNodeList.getLength() <= 0)
            {
                System.out.println("ERROR: Error while processing file.  No itemNode tags were found in the menu file.  review carefully if the script updated it. " +fName.getAbsolutePath());
                return;
            }
            
            boolean needsFix = false;
            String parentId = null;
            
            for(i = 0; needsFix == false && i < itemNodeList.getLength(); i++)            
            {
                m_taskFlowDefReviewCount++;
                
                Node itemNode = itemNodeList.item(i);
                Node parentNode = itemNode.getParentNode();
                NamedNodeMap attributes = itemNode.getAttributes();
                Node id = attributes.getNamedItem("id");
                
                if(parentNode != null && parentNode.getNodeName().equals("menu"))
                {
                    // then I am top level itemNode.
                    Node dcs = attributes.getNamedItem("dataControlScope");
                    if(dcs != null && dcs.getNodeValue().equals("isolated"))
                    {
                        System.out.println("Top Level Task Menu already isolated, skipping.");
                        return;
                    }
                    
                    Node isDyna = attributes.getNamedItem("isDynamicTabNavigation");
                    if(isDyna != null && isDyna.getNodeValue().equals("false"))
                    {
                        System.out.println("isDynamicTabNavigation is set to false, skipping.");
                        return;
                    }
                    
                    
                    parentId = id.getNodeValue();
                }
                else
                {
                    Node taskFlowId = attributes.getNamedItem("taskFlowId");
                    Node taskType = attributes.getNamedItem("taskType");

                    if(taskType == null || taskFlowId == null)
                        continue;
                    
                    String sTaskType = taskType.getNodeValue();
                    String sTaskFlowId = taskFlowId.getNodeValue();
                    
                    int index = sTaskFlowId.lastIndexOf("#");
                    if(index == -1)
                        index = sTaskFlowId.length();
                    
                    String sTaskFlowFile = base_path + sTaskFlowId.substring(0, index);
                    
                    if("defaultMain".equals(sTaskType) || "dynamicMain".equals(sTaskType) || "defaultRegional".equals(sTaskType))
                    {
                        System.out.println("need to check --> " +sTaskFlowFile);
                        if(checkTaskFlowForProblem(sTaskFlowFile, bDoADE))
                            needsFix = true;
                    }
                }
            }
            
            if(needsFix)
            {
                System.out.println("need to fix the menu file.");
                fixDCS(fName, bDoADE, parentId);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            logException(fName.getAbsolutePath(), e);
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated any of the taskflows related to this menu. " +fName.getAbsolutePath());
            return;
        }
    }


    private boolean checkTaskFlowForProblem(String f, boolean bDoADE) throws Exception
    {
        System.out.println("Checking Taskflow:  " +f.toString());
        File file = new File(f);
        if(file.exists() == false)
        {
            System.out.println("Task flow does not exist in this WEB-INF --> " +f);
            System.out.println("therefore, skipping file");
            return false;
        }

        DOMParser parser = new DOMParser();
        parser.setDebugMode(true);
        parser.setErrorStream(System.out);
        parser.setEntityResolver(new NullEntityResolver());
        parser.showWarnings(true);
        parser.setValidationMode(DOMParser.NONVALIDATING);

        parser.parse("file:" + f);

        XMLDocument doc = parser.getDocument();
        NodeList dcs = doc.getElementsByTagName("data-control-scope");
        
        if(dcs == null || dcs.getLength() == 0)
        {
            System.out.println("DCS not set!");
            return true;
            // set after
            // default-activity
        }
        else if(dcs.getLength() > 1)
        {
            System.out.println("ERROR:  multiple data-control-scope tags found!!!  skipping ---> " +f);
            return false;
        }
        else
        {
            System.out.println("DCS is set!");
            NodeList children = dcs.item(0).getChildNodes();
            Node setting = null;
            
            for(int j = 0; j < children.getLength(); j++)
            {
                setting = children.item(j);
                if(setting.getNodeType() != Node.TEXT_NODE)
                    break;
            }
            

            if(setting == null)            
            {
                System.out.println("ERROR:  unexpected state.  we have a dcs block, without a proper setting");
            }
            
            String sCurrentSetting = setting.getNodeName();
            
            if("shared".equals(sCurrentSetting))
            {
                // Need to rewrite to be isolated
                System.out.println("need to change to isolated");
                return true;
            }
            else if("isolated".equals(sCurrentSetting))
            {
                System.out.println("Taskflow is isolated.");
                return false;
            }
        }
        
        return false;
    }


    private void fixDCS(File f, boolean bDoADE, String id) throws Exception
    {
        boolean appliedFix = false;
        
        String key = "id=\""+id +"\"";

        File fp = new File(f.getAbsolutePath());
        FileReader reader = new FileReader( fp );
        BufferedReader input = new BufferedReader( reader );
        FileReader reader2 = new FileReader( fp );
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

            if(appliedFix == false)
            {
                if(line.contains(key))
                {
                    int index = line.indexOf(">");
                    if(index == -1)
                    {
                        System.out.println("ERROR:  could not process the fix for this line.");
                        System.out.println(line);
                        System.out.println(f.getAbsolutePath());
                        System.exit(-1);
                    }

                    line = line.substring(0, index) +" dataControlScope=\"isolated\"" +line.substring(index);
                    appliedFix = true;
                }
            }

            sb.append(line);
            sb.append(newline);

            lineNo++;
        }

        reader.close();

        if(appliedFix == false)
        {
            System.out.println("I wasn't able to figure out how to fix this one --->   " + f.getAbsolutePath());
            return;
        }

        if(bDoADE == false)
        {
            System.out.println("************************\n");
            System.out.println(sb.toString());
            return;
        }

        m_sSummaryReport += "Applying fix to file: " +f.getAbsolutePath() +"\n";

        checkoutAndDelete(f);

        FileWriter fw = new FileWriter(f.getAbsolutePath());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(sb.toString());
        bw.close();

        m_fixCount++;
        checkinAndDiff(f);        
    }



//    private void fixDCS(File f, boolean bDoADE, boolean inject) throws Exception
//    {
//        boolean appliedFix = false;
//
//        File fp = new File(f.getAbsolutePath());
//        FileReader reader = new FileReader( fp );
//        BufferedReader input = new BufferedReader( reader );
//        FileReader reader2 = new FileReader( fp );
//        BufferedReader input2 = new BufferedReader( reader2 );
//
//        String line;
//
//        StringBuffer sb = new StringBuffer();
//
//        int lineNo = 1;
//
//        while( (line = input.readLine()) != null )
//        {
//            input2.skip(line.length());
//            input2.mark(1024);
//            int c1 = input2.read();
//            int c2 = input2.read();
//            input2.reset();
//
//            String newline = "\n";
//            if(c1 == -1)
//            {
//                newline = "";
//            }
//            else if(c1 == '\n')
//            {
//                input2.read();
//            }
//            else if(c1 == '\r' && c2 != '\n')
//            {
//                newline = "\r";
//                input2.read();
//            }
//            else if(c1 == '\r' && c2 == '\n')
//            {
//                newline = "\r\n";
//                input2.read();
//                input2.read();
//            }
//
//            while(line.contains(">") == false && input.ready())
//            {
//                String extraLine = input.readLine();
//                line += newline+extraLine;
//                lineNo++;
//
//                input2.skip(extraLine.length());
//                input2.mark(1024);
//                c1 = input2.read();
//                c2 = input2.read();
//                input2.reset();
//
//                newline = "\n";
//                if(c1 == -1)
//                {
//                    newline = "";
//                }
//                else if(c1 == '\n')
//                {
//                    input2.read();
//                }
//                else if(c1 == '\r' && c2 != '\n')
//                {
//                    newline = "\r";
//                    input2.read();
//                }
//                else if(c1 == '\r' && c2 == '\n')
//                {
//                    newline = "\r\n";
//                    input2.read();
//                    input2.read();
//                }
//            }
//
//            if(appliedFix == false)
//            {
//                if(inject == true)
//                {
//                    if(line.contains("</default-activity>"))
//                    {
//                        String pad = "    ";
//                        int index = line.indexOf("<");
//                        if(index != -1)
//                        {
//                            String newPad = line.substring(0, index);
//                            if(newPad != null)
//                            {
//                                if(newPad.trim().equals(""))
//                                {
//                                    pad = newPad;
//                                }
//                            }
//                        }
//                        
//                        line = line +newline +pad +"<data-control-scope>";
//                        line = line +newline +pad +"  <isolated/>";
//                        line = line +newline +pad +"</data-control-scope>";
//                        appliedFix = true;
//                    }
//                }
//                else
//                {
//                    if(line.contains("<shared/>"))
//                    {
//                        line = line.replaceFirst("<shared/>", "<isolated/>");
//                        appliedFix = true;
//                    }
//                }
//            }
//
//            sb.append(line);
//            sb.append(newline);
//
//            lineNo++;
//        }
//
//        reader.close();
//
//        if(appliedFix == false)
//        {
//            System.out.println("I wasn't able to figure out how to fix this one --->   " + f.getAbsolutePath());
//            return;
//        }
//
//        if(bDoADE == false)
//        {
//            System.out.println("************************\n");
//            System.out.println(sb.toString());
//            return;
//        }
//
//        m_sSummaryReport += "Applying fix to file: " +f.getAbsolutePath() +"\n";
//
//        checkoutAndDelete(f);
//
//        FileWriter fw = new FileWriter(f.getAbsolutePath());
//        BufferedWriter bw = new BufferedWriter(fw);
//        bw.write(sb.toString());
//        bw.close();
//
//        m_fixCount++;
//        checkinAndDiff(f);        
//    }


    protected String getSummaryReport()
    {
        m_sSummaryReport = m_sSummaryReport + "For this run I reviewed a total of " +m_menuFileReviewCount +" menu files.\n";
        m_sSummaryReport = m_sSummaryReport + "For this run I reviewed a total of " +m_taskFlowDefReviewCount +" task flow definitions.\n";
        m_sSummaryReport = m_sSummaryReport + "For this run I fixed a total of    " +m_fixCount +" task flow definitions.\n";
        
        return m_sSummaryReport;
    }

    protected String getSummaryReportSubject()
    {
        return "Make Top Level Taskflows Isolated";
    }


    public static void main(String[] args) 
    {
        if (args.length != 2 || args[0] == null || args[1] == null || args[0].equals("") || args[1].equals("")) {
            System.out.println("Usage:  makeTopLevelTaskflowsIsolated.sh <path to your code in ade> <do ade>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        Boolean bDoADE = new Boolean(args[1]);

        TopLevelTaskflow x =
            new TopLevelTaskflow(JoesBaseClass.CRAWL_TYPE.TASKMENU);

        try {

            if (bDoADE.booleanValue())
                x.startDiffLog();

            x.crawlDirectory(sCrawlDir, bDoADE);

            if (bDoADE.booleanValue())
                x.closeDiffLog();

            FileOutputStream out = null;

        } catch (Exception e) {
            e.printStackTrace();
            x.logException(sCrawlDir, e);
        }

        x.logUsageReport(sCrawlDir);
    }
}
