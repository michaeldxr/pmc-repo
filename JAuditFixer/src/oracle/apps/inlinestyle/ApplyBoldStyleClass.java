package oracle.apps.inlinestyle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;

import java.io.FileReader;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.NullEntityResolver;
import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLNode;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class ApplyBoldStyleClass extends JoesBaseClass 
{
    Workbook m_myBook;
    Worksheet m_inlineSytlesSheet;
    
    int m_styleCount = 2;

    public ApplyBoldStyleClass(JoesBaseClass.CRAWL_TYPE type, boolean bDoADE) 
    {
        super(type);
        
        setDoADE(bDoADE);

        m_myBook = new Workbook("styles");

        m_inlineSytlesSheet = new Worksheet("styles");
        m_myBook.addWorksheet(m_inlineSytlesSheet);

        m_inlineSytlesSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("File Name")));
        m_inlineSytlesSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Path")));
        m_inlineSytlesSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Line Number")));
        m_inlineSytlesSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("Orig Style")));
        m_inlineSytlesSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Individual Style")));
    }

    public static void main(String[] args) 
    {
        if(args.length != 3 || args[0] == null || args[1] == null || args[2] == null || args[0].equals("") || args[1].equals("") || args[2].equals("")) 
        {
            System.out.println("Usage:  applyAppsBoldFont.sh <path to your code in ade> <output file name, including path> <ApplyFix (true/false)>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        String sOutpFilename = args[1];
        Boolean bDoADE = new Boolean(args[2]);

        ApplyBoldStyleClass style = new ApplyBoldStyleClass(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF, bDoADE);
        
        try
        {
    
            if(bDoADE.booleanValue())
                style.startDiffLog();
            
            style.crawlDirectory(sCrawlDir, bDoADE);
    

            if(bDoADE.booleanValue())
                style.closeDiffLog();
    
            FileOutputStream out = null;

            out = new FileOutputStream(sOutpFilename);
            XLSXCreator.create(style.m_myBook, out);

        }
        catch(Exception e) 
        {
            e.printStackTrace();
            style.logException("Exception Occured at top level", e);
        }


        style.logUsageReport(sCrawlDir);
    }
    
    private ArrayList<Integer> m_fixList = new ArrayList<Integer>();

    private void resetFixList() 
    {
        m_fixList.clear();
    }
    
    
    protected void processFile(File fName, boolean bDoADE) 
    {
        resetFixList();

        try
        {
            String msg = "---------------------------------------------------\nReviewing File:\n" +fName.getAbsolutePath();
            System.out.println(msg);
    
            DOMParser parser = new DOMParser();
            parser.setErrorStream(System.out);
            parser.setEntityResolver(new NullEntityResolver());
            parser.showWarnings(true);
            parser.setValidationMode(DOMParser.NONVALIDATING);
            parser.setDebugMode(true);
            
            parser.parse("file:" +fName.getCanonicalPath());
            
            XMLDocument doc = parser.getDocument();
            XMLNode n = (XMLNode)doc;
            
            boolean needsFix = reviewStyles(fName, n);
            
            if(needsFix) {
//                System.out.println(m_fixList.toString());
                applyFix(fName, bDoADE);
            }
            
            
            
        }
        catch(Exception e) 
        {
            e.printStackTrace();
            logException("Occurred while processing file ---> " +fName.getAbsolutePath() +"\n", e);
        }
    }

    private final static String styleClassRegex = "(styleClass=\"(?:"+whiteSpace+identifier +whiteSpace+")+)\"";
    private final static Pattern styleClassPattern =  Pattern.compile(styleClassRegex);

    private final static String missingNewlineRegex = ">" +whiteSpace +"<";
    private final static Pattern missingNewlinePatter = Pattern.compile(missingNewlineRegex);
    
     //TODO
     // newlines in this file are messed up:
     // /ade/albowicz_stretch_scratch/fusionapps/fin/components/receivables/ar/receipts/desktopLockboxes/di/public_html/oracle/apps/financials/receivables/receipts/desktopLockboxes/di/page/LockboxDIBusinessUnitSearch.jspx
    void applyFix(File f, boolean bDoADE)
    {
//        if(f.getAbsolutePath().contains("JournalDashboard.jsff")) {
//            System.out.println("here");
//        }
        
        try
        {
            boolean fixed = false;
            int seen = 0;
            int lineNo = 1;
            
            File fp = new File(f.getAbsolutePath());
            FileReader reader = new FileReader( fp );
            BufferedReader input = new BufferedReader( reader );
    
            FileReader reader2 = new FileReader( fp );
            BufferedReader input2 = new BufferedReader( reader2 );
    
    
            String line;
            StringBuffer sb = new StringBuffer();
    
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

                int len = line.length() +newline.length();
                seen += line.length() +newline.length();


                if(m_fixList.contains(lineNo)) 
                {
                    //to avoid problems, put some newlines that are missing.
                    Matcher n = missingNewlinePatter.matcher(line);
                    while(n.find()) 
                    {
                        String prev = line.substring(0, n.start()+1);
                        
                        if(prev.contains("styleClass") || prev.contains("inlineStyle")) 
                        {
                            throw new Exception("Unexpected xml, mutliple tags on same ling ");
                        }
                        
                        sb.append(prev);
                        sb.append("\n");
                        
                        line = line.substring(n.start()+1);
                        n = missingNewlinePatter.matcher(line);
                    }


                    // Now go into a tight loop to fix this tag

                    boolean removedBoldStyle = false;
                    boolean hasAppsBoldFont = false;
                    boolean tagOpen = true;
                    
                    while(tagOpen) 
                    {
                        if(line.contains("inlineStyle")) 
                        {
                            String modifiedLine = removeBoldFromLine(line);
                            if(line.equals(modifiedLine) == false)
                            {
                                removedBoldStyle = true;
                                line = modifiedLine;
                            }
                        }
                        
                        if(line.contains("styleClass") && line.contains("AppsBoldFont"))
                            hasAppsBoldFont = true;
                        else if(line.contains("styleClass")) 
                        {
                            Matcher m = styleClassPattern.matcher(line);
                            if(m.find()) 
                            {
                                String sStyleClass = m.group(1);
                                String smatch = line.substring(m.start(), m.end());
                                line = line.substring(0, m.start()) +sStyleClass + " AppsBoldFont" +"\"" +line.substring(m.end());
                            }
                            else 
                            {
                                throw new Exception("need to handle existing styleClass case\n"+line);
                            }
                            hasAppsBoldFont = true;
                        }

                        // last line in tag will be appended outside the loop.
                        if(locationOfTagEnd(line) != -1)
                            break;
                        
                        if("".equals(line.trim()) == false)
                        {
                            sb.append(line);
                            sb.append("\n");
                        }

                        lineNo++;

                        line = input.readLine();
                        if(line == null)
                            throw new Exception("unexpected to read a null line");
                    }

                    
                    // current line terminates the tag with ">"
                    if(!hasAppsBoldFont) 
                    {
                        int index = locationOfTagEnd(line);
                        line = line.substring(0, index) + " styleClass=\"AppsBoldFont\"" +line.substring(index);
                    }

                    fixed = true;
                }

                if(bDoADE == false)
                    sb.append(String.valueOf(lineNo));
                sb.append(line);
                sb.append(newline);

                lineNo++;
            }





            reader.close();

            if(fixed)
            {
                System.out.println("+++ Fix Applied ++++++++++++++++++++++++++++++++++++++++++++++++++++");
                doADE(isDoADE(), f, sb.toString());
            }
        }
        catch(Exception e) 
        {
            e.printStackTrace();
            logException("Occurred while processing file ---> " +f.getAbsolutePath() +"\n", e);
        }
    }

    private final static String boldExtractRegex = whiteSpace +"font-weight:" +whiteSpace +"bold(er)?;?" +whiteSpace;
    private final static Pattern boldExtractPattern =  Pattern.compile(boldExtractRegex, Pattern.CASE_INSENSITIVE);
    
    private final static String emptyInlineStyleExtractRegex = "inlineStyle=\"\"";
    private final static Pattern emptyInlineStyleExtractPattern =  Pattern.compile(emptyInlineStyleExtractRegex, Pattern.CASE_INSENSITIVE);
    
    private final static String whiteSpacePrefixRegex = "(" +whiteSpace +")";
    private final static Pattern whiteSpacePrefixPattern = Pattern.compile(whiteSpacePrefixRegex);
    
    private String removeBoldFromLine(String line) throws Exception
    {
        boolean hasStartBracket = false;
        boolean hasCloseBracket = false;
        
        if(line.contains("<"))
            hasStartBracket = true;
        
        if(line.contains(">"))
            hasCloseBracket = true;
        
        String whiteSpacePrefixOrig = "";
        Matcher wsMatcher = whiteSpacePrefixPattern.matcher(line);
        if(wsMatcher.find()) 
        {
            whiteSpacePrefixOrig = wsMatcher.group(1);
        }
        
        
        Matcher m = boldExtractPattern.matcher(line);
        if(m.find()) 
        {
            if(line.contains("bolder"))
            {
                System.out.println("WARNING:  Replacing hard coded BOLDER style with AppsBoldFont; please review this case with your PM to make sure the appropriate style choices are applied.");
            }

            String s = line.substring(0, m.start()) + line.substring(m.end());
            
            Matcher m2 = emptyInlineStyleExtractPattern.matcher(s);
            if(m2.find()) 
            {
                s = s.substring(0, m2.start()) +s.substring(m2.end());
                if(s.trim().equals(""))
                    s = "";
                //System.out.println(s);
            }
            
            line = s;
        }
        else 
        {
            throw new Exception ("Could not match the width in this string!!  ---> " +line);
        }
        
        
        // restore leading whitespace
        Matcher m2 = whiteSpacePrefixPattern.matcher(line);
        if(m2.find()) 
        {
            line = whiteSpacePrefixOrig +line.substring(m2.end());
        }
        
        return line;
    }


    protected String getSummaryReport() 
    {
        return "just finished";
    }


    private boolean reviewStyles(File fName, XMLNode n) throws Exception
    {
        if(n == null || (n.getNodeType() != Node.ELEMENT_NODE && n.getNodeType() != Node.DOCUMENT_NODE))
            return false;
        
        boolean needsFix = false;
        NamedNodeMap attrs = n.getAttributes();
        
        
        if(attrs != null)
        {
            XMLNode inlineStyle = (XMLNode)attrs.getNamedItem("inlineStyle");
            
            if(inlineStyle != null)
            {
                if(ReviewAndLogStyles(fName, inlineStyle))
                    needsFix = true;
            }
        }
        
        NodeList children = n.getChildNodes();
        if(children==null)
            return needsFix;
        
        for(int i = 0; i < children.getLength(); i++)
            if(reviewStyles(fName, (XMLNode)children.item(i)))
                needsFix = true;
        
        return needsFix;
    }


//    private static final String notSemiColon = "[^;]+";
//    private static final String notSemiColon2 = whiteSpace +"([^;\\#\\{\\}]*" +"(:?#\\{[^\\}]+\\}" + "[^;]*)?);";
    private static final String notSemiColon2 = whiteSpace +"([^;\\#\\{\\}\\\"]+|(:?#\\{[^\\}]+\\}));?";

//    private static final String stylesRegex2 = whiteSpace +"(" +notSemiColon +"|" +");";
    private static final String stylesRegex = whiteSpace +"([^;]+);";
    private static final Pattern stylesPattern = Pattern.compile(notSemiColon2, Pattern.MULTILINE);
    
    private static final String boldStyleRegex = "font-weight:" +whiteSpace  +"bold(er)?";
    private static final Pattern boldPattern = Pattern.compile(boldStyleRegex, Pattern.CASE_INSENSITIVE);

    private boolean ReviewAndLogStyles(File fName, XMLNode inlineStyle) throws Exception
    {
        ArrayList<String> fileAsStrings = new ArrayList<String>();

        File fp = new File(fName.getAbsolutePath());
        FileReader reader = new FileReader( fp );
        BufferedReader input = new BufferedReader( reader );
        String line;
        
        while( (line = input.readLine()) != null )
        {
            fileAsStrings.add(line);
        }

        reader.close();




        boolean needsAppsBoldFont = false;
        
        int lineNo = inlineStyle.getLineNumber();
        
        String styles = inlineStyle.getNodeValue();

        Matcher m = stylesPattern.matcher(styles);
        while(m.find()) 
        {
            String style = m.group(1);
            //System.out.println(style);

            Matcher wm = boldPattern.matcher(style);
            if(wm.find()) 
            {
                needsAppsBoldFont = true;

                int realLineNo = lineNo;
                for(int i = realLineNo; i > 0; i--) 
                {
                    String realLine = fileAsStrings.get(i-1);
                //                            if(realLine.contains("width:") && realLine.contains("inlineStyle"))
                    if(realLine.contains("<")) 
                    {
                        m_fixList.add(new Integer( i ));
                        break;
                    }
                    if(realLine.contains("<")) 
                    {
                        // this should neverl happen!!
                        
                        throw new Exception("Unexpected case!");
                    }
                }

            }
            
            String l1 = getCellLocation(0, m_styleCount);
            String l2 = getCellLocation(1, m_styleCount);
            String l3 = getCellLocation(2, m_styleCount);
            String l4 = getCellLocation(3, m_styleCount);
            String l5 = getCellLocation(4, m_styleCount);
            String l6 = getCellLocation(5, m_styleCount);

            m_inlineSytlesSheet.addCell(new Cell(new CellLocation(l1), new CellValue(fName.getName())));
            m_inlineSytlesSheet.addCell(new Cell(new CellLocation(l2), new CellValue(fName.getAbsolutePath())));
            m_inlineSytlesSheet.addCell(new Cell(new CellLocation(l3), new CellValue(String.valueOf(lineNo))));
            m_inlineSytlesSheet.addCell(new Cell(new CellLocation(l4), new CellValue(styles)));
            m_inlineSytlesSheet.addCell(new Cell(new CellLocation(l5), new CellValue(style)));

            m_styleCount++;
        }
        
        return needsAppsBoldFont;
    }

    protected String getSummaryReportSubject() {
        return "Analyze Inline Styles";
    }
    
    // need to escape any '>' occurring within #{  }
    private static final String escapeELRegex = "#\\{" +"[^}]*" +"\\}";
    private static final Pattern escapeELPattern = Pattern.compile(escapeELRegex);
    int locationOfTagEnd(String line) 
    {
        if(line.contains("#{") && line.contains(">")) 
        {
            // need to potentially escape
            Matcher m = escapeELPattern.matcher(line);
            while(m.find()) 
            {
                line = line.substring(0, m.start()) + line.substring(m.start(), m.end()).replace('>', '_').replace('<', '_') +line.substring(m.end());
            }
        }
        
        int index = line.lastIndexOf("/>");
        if(index != -1)
            return index;
        
        return line.indexOf(">");
    }
}
