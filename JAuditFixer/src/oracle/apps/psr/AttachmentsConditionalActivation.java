package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;

import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;

import oracle.apps.NullEntityResolver;
import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.managedBeanFixer.ApplyComponentWrapper;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLAttr;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AttachmentsConditionalActivation extends JoesBaseClass 
{
    int m_reviewCount = 0;
    int m_fixCount = 0;
//    int m_numExceptions = 0;
    
    private String m_sExceptions = "";
    private String m_sSummaryReport = "";
    
    public AttachmentsConditionalActivation(JoesBaseClass.CRAWL_TYPE type) {
        super(type);
    }
    
    public static void main(String[] args) 
    {
        if (args.length != 2 || args[0] == null || args[1] == null || args[0].equals("") || args[1].equals("")) {
            System.out.println("Usage:  attachmentsConditionalActivation.sh <path to your code in ade> <do ade>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        Boolean bDoADE = new Boolean(args[1]);

        AttachmentsConditionalActivation x =
            new AttachmentsConditionalActivation(JoesBaseClass.CRAWL_TYPE.PAGE_DEF);

        try {

            if (bDoADE.booleanValue())
                x.startDiffLog();

            x.crawlDirectory(sCrawlDir, bDoADE);
//            x.analyzeBeans(bDoADE.booleanValue());

            if (bDoADE.booleanValue())
                x.closeDiffLog();

            FileOutputStream out = null;

        } catch (Exception e) {
            e.printStackTrace();
            x.logException(sCrawlDir, e);
        }

        x.logUsageReport(sCrawlDir);
    }

    protected String getSummaryReportSubject() 
    {
        return "Make Attachments Conditionally Activated";
    }

    protected String getSummaryReport() 
    {
        m_sSummaryReport = "For this run I reviewed a total of " +m_reviewCount +" pageDefs.\n" + m_sSummaryReport;
        m_sSummaryReport = "For this run I fixed a total of    " +m_fixCount +" pageDefs.\n" + m_sSummaryReport;
        
        return m_sSummaryReport;
    }

    protected void processFile(File fName, boolean bDoADE) 
    {
        m_reviewCount++;
        processPageDef(fName, bDoADE);

    }


    private void processPageDef(File f, boolean bDoADE) 
    {
        System.out.println("Checking PageDef:  " +f.toString());

        ArrayList<Integer> lineNumberList = new ArrayList<Integer>();
        
        try {
            DOMParser parser = new DOMParser();
            parser.setDebugMode(true);
            parser.setErrorStream(System.out);
            parser.setEntityResolver(new NullEntityResolver());
            parser.showWarnings(true);
            parser.setValidationMode(DOMParser.NONVALIDATING);

            parser.parse("file:" + f.getCanonicalPath());

            XMLDocument doc = parser.getDocument();
            NodeList adfc = doc.getElementsByTagName("pageDefinition");
            if (adfc == null) 
            {
                System.out.println("REVIEW:  Not a pageDefinition file ---> " + f.getCanonicalPath());
                return;
            }

            NodeList taskList = doc.getElementsByTagName("taskFlow");

            for (int i = 0; i < taskList.getLength(); i++) 
            {
                Node task = taskList.item(i);
                NamedNodeMap attributes = task.getAttributes();
                Node text = attributes.getNamedItem("id");
                if(text == null)
                    continue;
                
                String textValue = text.getNodeValue();
                if(textValue != null && textValue.contains("attachment"))
                {
                    System.out.println("Found attachments reference --->  " +textValue);
                    if(textValue.startsWith("attachmentRepositoryBrowseTaskFlow") == false)
                    {
                        System.out.println("ERROR:  unexpected attachments reference --> " +textValue);
                        continue;
                    }
                    
                boolean bAlreadyConditional = false;
                
                Node activation = attributes.getNamedItem("activation");
                if(activation != null)
                {
                    String actText = activation.getNodeValue();
                    if(actText != null && actText.equals("conditional"))
                        bAlreadyConditional = true;
                }

                boolean bAlreadyHasActive = false;
                
                Node active = attributes.getNamedItem("active");
//                    ="#{pageFlowScope.FND_ATTACHMENTS_LOAD_TF==true}">
                if(active != null)
                {
                    String actText = active.getNodeValue();
                    if(actText != null && actText.equals("#{pageFlowScope.FND_ATTACHMENTS_LOAD_TF==true}"))
                        bAlreadyHasActive = true;
                }

                if(bAlreadyConditional && bAlreadyHasActive)
                {
                    System.out.println("Already has fix, skipping...");
                    continue;
                }
                else
                {
                    System.out.println("Applying automated fix...");
                }
                    
                    XMLAttr attr = null;
                    if(text instanceof XMLAttr)
                    {
                        attr = (XMLAttr)text;
                        System.out.println("At line:  " +attr.getLineNumber());
                        lineNumberList.add(attr.getLineNumber());
                    }
                }
            }


            if(lineNumberList.isEmpty() == false)
                applyFix(f, lineNumberList, bDoADE);

        } catch (Exception e) {
            e.printStackTrace();
            logException(f.getAbsolutePath(), e);
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +f.getAbsolutePath());
            return;
        }

    }

//    private void logException(Exception e) 
//    {
//        m_numExceptions++;
//
//        m_sExceptions +=
//                "\n@@ Exception # " + m_numExceptions + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n";
//        m_sExceptions += e.toString() + "\n";
//        StackTraceElement[] element = e.getStackTrace();
//        for (int i = 0; i < element.length; i++) {
//            m_sExceptions += element[i].toString() + "\n";
//        }
//    }


    private void applyFix(File f, ArrayList<Integer> lineNumberList, boolean bDoADE) throws Exception
    {
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

            if(inList(lineNumberList, lineNo))
            {
//                line = fixLine(line, newline);
                System.out.println(lineNo +":" +line);
                
                if(line.contains("<") && line.contains(">"))
                {
                    if(line.contains("activation=\"conditional\"") == false)
                    {
                        int l = line.indexOf("activation");
                        if(l == -1)
                        {
                            // need to add 'activation="conditional"'
                            l = line.indexOf("/>");
                            if(l == -1)
                                l = line.indexOf(">");
                            line = line.substring(0, l) +" activation=\"conditional\"" +line.substring(l);
                        }
                        else
                        {
                            // Need to update activation 
                            String a = line.substring(0, l+12);
                            String b = line.substring(l+12);
                            l = b.indexOf("\"");
                            line = a + "conditional" +b.substring(l);
                        }
                    }

                    String newActive = "active=\"#{pageFlowScope.FND_ATTACHMENTS_LOAD_TF==true}\"";
                    if(line.contains(newActive) == false)
                    {
                        int l = line.indexOf("active");
                        if(l == -1)
                        {
                            // need to add 
                            l = line.indexOf("/>");
                            if(l == -1)
                                l = line.indexOf(">");
                            
                            line = line.substring(0, l) +" " +newActive +line.substring(l);
                        }
                        else
                        {
                            System.out.println("ERROR:  case not handled.  existing active condition");
                        }
                    }
                    
                    System.out.println(lineNo +":" + line);
                    
                }
                else
                {
                    System.out.println("ERROR:  attachment not found as expected.  apply fix manually");
                    return;
                }

                sb.append(line +newline);
            }
            else
            {
                sb.append(line);
                
                sb.append(newline);
            }

            lineNo++;
        }

        reader.close();

        if(bDoADE == false)
            return;

        m_sSummaryReport += "Applying fix to file: " +f.getAbsolutePath() +"\n";

        checkoutAndDelete(f);

        FileWriter fw = new FileWriter(f.getAbsolutePath());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(sb.toString());
        bw.close();

        m_fixCount++;
        checkinAndDiff(f);
    }

    private boolean inList(ArrayList<Integer> lineNumberList, int line)
    {
        for(int i = 0; i < lineNumberList.size(); i++)
        {
            if(lineNumberList.get(i).intValue() == line)
                return true;
        }
        
        return false;
    }
}
