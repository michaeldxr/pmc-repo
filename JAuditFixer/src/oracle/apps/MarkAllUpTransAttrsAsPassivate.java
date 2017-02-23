package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileOutputStream;
import java.io.FileReader;

import java.io.FileWriter;
import java.io.InputStream;
import java.io.ObjectOutputStream;

import java.io.Writer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLAttr;
import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MarkAllUpTransAttrsAsPassivate
{
    private BufferedWriter diff;
    
    public MarkAllUpTransAttrsAsPassivate()
    {
        super();
    }

    int m_numViewLinks = 0;
    int m_numVOs = 0;
    int m_numFlexVOs = 0;
    
    int m_numVOsProcessed = 0;
    int m_numAttrsProcessed = 0;

    private boolean inList(ArrayList<Integer> lineNumberList, int line)
    {
        for(int i = 0; i < lineNumberList.size(); i++)
        {
            if(lineNumberList.get(i).intValue() == line)
                return true;
        }
        
        return false;
    }

    private boolean appearsMoreThanOnce(String str, String pattern)
    {
        int index = str.indexOf(pattern);
        if(index == -1)
            return false;
        
        String str2 = str.substring(index +pattern.length());
        index = str2.indexOf(pattern);
        if(index == -1)
            return false;
        
        return true;
    }

    private void applyFix(File f, ArrayList<Integer> lineNumberList) throws Exception
    {
        File fp = new File(f.getAbsolutePath());
        FileReader reader = new FileReader( fp );
        BufferedReader input = new BufferedReader( reader );

        FileReader reader2 = new FileReader( fp );
        BufferedReader input2 = new BufferedReader( reader2 );


        String line;
        String line2;

        StringBuffer sb = new StringBuffer();

        int lineNo = 1;
        input.mark(1024);

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
            

            if(inList(lineNumberList, lineNo))
            {
                line = fixLine(line, newline);
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

        checkoutAndDelete(f);

        FileWriter fw = new FileWriter(f.getAbsolutePath());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(sb.toString());
        bw.close();

        checkinAndDiff(f);
    }

    private String fixLine(String line, String newline)
    {
        // First do some sanity tests to make sure we are dealing with good input.
        if(appearsMoreThanOnce(line, "<") ||
           appearsMoreThanOnce(line, ">"))
        {
            System.out.println("ERROR:  skipping this line which is formatted poorly");
            System.out.println(line);
            return line;
        }

        String retVal = null;

        boolean singleLine = line.indexOf("<ViewAttribute") != -1;
        
        int i = line.indexOf("/>");

        if(singleLine)
        {
            if(i != -1)
            {
                retVal = line.substring(0, i);
                retVal += " Passivate=\"true\"/>";
                return retVal;
            }
    
            i = line.indexOf(">");
            retVal = line.substring(0, i);
            retVal += " Passivate=\"true\">";
            return retVal;
        }

        String leadingWhitespace = newline +line.substring(0, line.indexOf(line.trim()));

        if(i != -1)
        {
            retVal = line.substring(0, i);
            retVal += leadingWhitespace;
            retVal += "Passivate=\"true\"/>";
            return retVal;
        }
        
        i = line.indexOf(">");
        retVal = line.substring(0, i);
        retVal += leadingWhitespace;
        retVal += "Passivate=\"true\">";
        return retVal;
    }


    private void checkoutAndDelete(File f) throws Exception
    {
        Process coProcess = Runtime.getRuntime().exec("ade co -nc " +f.getAbsolutePath());
        checkProcess(coProcess, f, "checkout", false);
        
        Process rmProcess = Runtime.getRuntime().exec("rm -f " +f.getAbsolutePath());
        checkProcess(rmProcess, f, "remove old file", false);
    }

    private void checkProcess(Process p, File f, String description, boolean isDiff) throws Exception
    {
        int exitCode = p.waitFor();
        if(!isDiff && exitCode != 0)
        {
            if(f != null)
                System.out.println("There was an error when trying to " +description+ " for file : " +f.getAbsolutePath());
            else
                System.out.println("There was an error when trying to " +description);
        }

        dumpInputStream(p.getInputStream(), isDiff);
        dumpInputStream(p.getErrorStream(), isDiff);
    }

    private void dumpInputStream(InputStream in, boolean isDiff) throws Exception
    {
        int n = in.available();
        byte b[] = new byte[n];
        in.read(b, 0, n);
        String str = new String(b);
        if(str.contains("ade ERROR"))
        {
            System.out.println(str);
            System.out.println("There was an error interacting with ADE, exiting script.");
            System.exit(-1);
        }

        if(isDiff == false)
            System.out.println(str.trim());
        else
            diff.write(str);
    }

    private void checkinAndDiff(File f) throws Exception
    {
        Process ciProcess = Runtime.getRuntime().exec("ade ci " +f.getAbsolutePath());
        checkProcess(ciProcess, f, "checkin", false);
        
        String diffCmd = "ade diff -label " +f.getAbsolutePath();
        Process diffProcess = Runtime.getRuntime().exec(diffCmd);
        checkProcess(diffProcess, f, "diff file with previous in label", true);
    }

    private void closeDiffLog() throws Exception
    {
        diff.close();
    }
    private void startDiffLog() throws Exception
    {
        Process p = Runtime.getRuntime().exec("rm -f diff.txt");
        checkProcess(p, null, "remove diff log", false);

        diff = new BufferedWriter(new FileWriter("diff.txt"));
    }


    private void processVO(File f, Boolean bDoADE) 
    {
        try{
            ArrayList<Integer> lineNumberList = new ArrayList<Integer>();
            
            boolean needsFix = false;
            
            DOMParser parser = new DOMParser();
            parser.setDebugMode(true);
            parser.setErrorStream(System.out);
            parser.setEntityResolver(new NullEntityResolver());
            parser.showWarnings(true);
            parser.setValidationMode(DOMParser.NONVALIDATING);
            
            parser.parse("file:" +f.getAbsolutePath());
            
            XMLDocument doc = parser.getDocument();
            NodeList voTag = doc.getElementsByTagName("ViewObject");
            
            if(voTag == null || voTag.getLength() != 1)
            {
                System.out.println("Error processing file:  " +f.getAbsolutePath());
                System.out.println("Was expecting to find exactely 1 ViewObject tag in the file.");
                System.out.println("Skipping....");
            }

            NamedNodeMap VOAttrs = voTag.item(0).getAttributes();
            Node passivateAllNode = VOAttrs.getNamedItem("Passivate");

            if(passivateAllNode != null && (passivateAllNode.getNodeValue().equals("All") || passivateAllNode.getNodeValue().equals("None")))
            {
                return;
            }

            NodeList viewAttributes = doc.getElementsByTagName("ViewAttribute");

            for(int i = 0; i < viewAttributes.getLength(); i++)
            {
                Node node = viewAttributes.item(i);
                
                NamedNodeMap attributes = node.getAttributes();

                Node entityAttr = attributes.getNamedItem("EntityAttrName");
                // Skip EO based Attributes
                if(entityAttr != null)
                    continue;

                // Skip Transient Atrributes already marked to be passivated.
                Node passivateAttr = attributes.getNamedItem("Passivate");
                if(passivateAttr != null && passivateAttr.getNodeValue().equals("true"))
                    continue;
                
                // Skip Read Only attributes
                Node updateAttr = attributes.getNamedItem("IsUpdateable");
                if(updateAttr != null && updateAttr.getNodeValue().equals("false"))
                    continue;

               boolean hasTransientExpression = false;
               boolean alwaysRecalcCondition = true;

                NodeList viewAttrChildren = node.getChildNodes();
                for(int j = 0; j < viewAttrChildren.getLength(); j++)
                {
                    Node viewAttrChild = viewAttrChildren.item(j);
                    if(viewAttrChild.getNodeName().equals("TransientExpression"))
                    {
                        hasTransientExpression = true;
                    }
                    if(viewAttrChild.getNodeName().equals("RecalcCondition"))
                    {
                        String sRecalc = viewAttrChild.getTextContent().trim();
                        
                        if("true".equals(sRecalc) == false)
                            alwaysRecalcCondition = false;
                    }
                }

            if(hasTransientExpression && alwaysRecalcCondition)
                continue;

               Node nameAttr = attributes.getNamedItem("Name");

               XMLElement elem = null;
               if(node instanceof XMLElement)
                   elem = (XMLElement)node;
               XMLAttr attr = null;
               if(nameAttr instanceof XMLAttr)
                   attr = (XMLAttr)nameAttr;

                
                if(attr != null)
                {
                    lineNumberList.add(attr.getLineNumber());
                }
                else
                    System.out.println("Unexpected ERROR:  cannot determine line number of view attribute");
                m_numAttrsProcessed++;
                needsFix = true;
           }
            
//            <AttrArray Name="KeyAttributes">
//              <Item Value="CardId" />
//              <Item Value="PersonNameId" />
//              <Item Value="PersonId1" />
//              <Item Value="EffectiveStartDate" />
//              <Item Value="EffectiveEndDate" />
//            </AttrArray> 

            
                  
            // TODO:  MAKE SURE YOU FLAG VOS THAT YOU FIX, BUT DO NOT HAVE PRIMARY KEY!!!!
            if(needsFix && bDoADE.booleanValue())
            {
                m_numVOsProcessed++;
                System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
                System.out.println("Fixing file:  " +f.getAbsolutePath());

                boolean hasKeys = false;
                NodeList AttrArrayList = doc.getElementsByTagName("AttrArray");
                for(int a = 0; a < AttrArrayList.getLength(); a++)
                {
                    NamedNodeMap attributes = AttrArrayList.item(a).getAttributes();
                    Node nameNode = attributes.getNamedItem("Name");
                    
                    if(nameNode != null && "KeyAttributes".equals(nameNode.getNodeValue()))
                    {
                        hasKeys = true;
                        break;
                    }
                }
                
                if(hasKeys != true)
                {
                    System.out.println("REVIEW:  This VO does not have KeyAttributes configured, but needs KeyAttributes.");
                }
                
                applyFix(f, lineNumberList);
            }
        }
        catch(Exception e)
        {
            System.out.println("Error processing file:  " +f.toString());
            System.out.println("Skipping....   but here is the error.");
            e.printStackTrace();
        }
    }

    private void crawlDirectory(String path, Boolean bDoADE) 
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
//                System.out.println("crawl: " +sName);
                if(sName.endsWith("VO.xml"))
                {
                    if(sName.contains("adfmsrc") || sName.contains("/test/") || sName.contains("/modelTest/") || sName.contains("/classes/") || sName.contains("/association/"))
                    {
                        System.out.println("REVIEW:  Skipping --> " +sName);
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
                        processVO(listOfFiles[i], bDoADE);
                    }
                }
            }
            else if(listOfFiles[i].isDirectory()) 
            {
                String dir = listOfFiles[i].getAbsolutePath();

                if(dir.contains("/ui/") == false && dir.contains("/dbSchema/") == false)
                    crawlDirectory(dir, bDoADE);
            }
            else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }


    public static void main(String[] args) 
    {
        if(args.length != 2 || args[0] == null || args[0].equals("") || args[1] == null || args[1].equals(""))
        {
            System.out.println("Usage:  markAllUpTransAttrsAsPassivate.sh <path to your code in an open ade view> <Do ADE checkin/checkout>");
            System.exit(1);
        }


        String sCrawlDir = args[0];
        Boolean bDoADE = new Boolean(args[1]);
        
        MarkAllUpTransAttrsAsPassivate x = new MarkAllUpTransAttrsAsPassivate();
        
        try
        {
            if(bDoADE.booleanValue())
                x.startDiffLog();
            
            x.crawlDirectory(sCrawlDir, bDoADE);

            if(bDoADE.booleanValue())
                x.closeDiffLog();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("Number of ViewObjects reviewed    :   " +x.m_numVOs);
        System.out.println("Number of ViewObjects fixed       :   " +x.m_numVOsProcessed);
        System.out.println("Number of ViewAttributes fixed    :   " +x.m_numAttrsProcessed);
    }
}


