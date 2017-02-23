package oracle.apps.fixer;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JavaFileParserHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EnumTransformer3 extends JoesBaseClass
{
    static int m_reviewCount = 0;
    static BufferedWriter writer;
    static BufferedWriter writer1;
    static boolean rowClassChanged = false;

    public EnumTransformer3(JoesBaseClass.CRAWL_TYPE type) {
        super(type);
    }

    public static void main(String[] args) {
        if (args.length != 2 || args[0] == null || args[1] == null || args[0].equals("") ||
            args[1].equals("")) {
            System.out.println("Usage:  fixBloatedEnums3.sh <path to your code in ade> <true for fixer mode/false for scanning>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        Boolean bDoADE = new Boolean(args[1]);
        
//        String sCrawlDir = "/ade.../FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64.rdd/LATEST/" +
//            "fusionapps/fin/components/financialCommon/zx/configuration/party/publicModel" +
//            "/src/oracle/apps/financials/tax/configuration/party/publicModel/entity";
//        Boolean bDoADE = false;

        try {
            EnumTransformer3 enumTransformer =
                new EnumTransformer3(JoesBaseClass.CRAWL_TYPE.VO_EO_XML);
            
            writer = new BufferedWriter(new FileWriter("enum_violations.txt"));
            writer1 = new BufferedWriter(new FileWriter("errors.txt"));
            writer.write("Family,Module,Product,FileName,Label,Issue\n");

            if (bDoADE.booleanValue())
                enumTransformer.startDiffLog();

            enumTransformer.crawlDirectory(sCrawlDir, bDoADE);
            
            writer.close();
            writer1.close();

            if (bDoADE.booleanValue())
                enumTransformer.closeDiffLog();

            enumTransformer.logUsageReport(sCrawlDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    protected void processFile(File f, boolean bDoADE)
    {
        String fName = f.getAbsolutePath();
        System.out.println("Checking this file:" + fName);
        XmlFixer fixer = null;
        
        try{
            
            XMLDocument doc = XMLParserHelper.getXMLDocument(fName); 
            if(doc == null) {
                writer1.write("Skipping: Could not get xml document: " + fName + "\n");
                return;
            }
            
            NodeList viewObjects = null;
            if(fName.endsWith("VO.xml"))
                viewObjects = doc.getElementsByTagName("ViewObject");
            else if(fName.endsWith("EO.xml"))
                viewObjects = doc.getElementsByTagName("Entity");
            if(viewObjects == null || viewObjects.getLength() != 1) 
            {
              writer1.write("Skipping:  Unexpected number of view/entity objects found for file:  " +fName + "\n");
              return;
            }
            Node viewObject = viewObjects.item(0);
            
            String glueCodeFlag = XMLParserHelper.getAttributeValue(viewObject, "UseGlueCode"); 
            String rowClass = XMLParserHelper.getAttributeValue(viewObject, "RowClass");
            if(FamilyModuleHelper.isEmpty(rowClass)) {
                writer1.write("Skipping:  VO does not have a row class, no change necessary:  " +fName + "\n");
                return;
            }
            
            String rowClassFile  = FamilyModuleHelper.getRowClassImpl(fName,rowClass);
            
            if(rowClassFile == null) {
                writer1.write("Skipping:  Could not get Row class:  " +fName + "\n");
                return;
            }
            
            rowClassChanged = false;
            
            JavaFileParserHelper javaFileHelper = new JavaFileParserHelper(rowClassFile);
            String fileContentsWoComments = javaFileHelper.preserveComments();
            ArrayList<String> code = javaFileHelper.getFileContentsArrayList(fileContentsWoComments);
            
            boolean customized = checkForCustomAccessors(code);
            if(customized) {
                writer1.write("Skipping:  RowClass has customer get/setAttrInvokeAccessor:  " +rowClass + "\n");
                return;
            }

            code = javaFileHelper.removeMethodWithName("getAttrInvokeAccessor", code, false, "");
            code = javaFileHelper.removeMethodWithName("setAttrInvokeAccessor", code, false, "");

            findIssue(new File(rowClassFile), bDoADE, code, javaFileHelper.comments);
            
            if(rowClassChanged){      
                fixer = new XmlFixer(fName);
                if(glueCodeFlag == null || (glueCodeFlag != null && glueCodeFlag.equals("false"))){
                    writer.write(FamilyModuleHelper.getFileNameInfo(fName) + "UseGlueCode is not true\n");
                    if(fixer != null)
                        fixer.modifyAttribute(viewObject, "UseGlueCode", "false", "true", true);
                    else
                        writer1.write("WARNING:  Row class was changed, but VO could not be changed:  " +fName + "\n");
                }
                
                doADE(bDoADE, f, fixer.getFileContentsAsString(false));
            }
        }
        catch (Exception e) 
        {
            e.printStackTrace();    
            logException(e);
            try{
                writer1.write("Exception occured when processing file: " + fName + ". Please review both VO and rowclass carefully.\n");
                writer1.write(e.getMessage() + "\n\n");
            }catch(Exception e1) { e1.printStackTrace(); }
        }
        m_reviewCount++;        
    }


    private static final String part1 =
        whiteSpace + "public" +whiteSpace + "enum" +whiteSpace +"AttributesEnum" +whiteSpace +"\\{" + whiteSpace;
    private static final Pattern pattern_part1 = Pattern.compile(part1, Pattern.CASE_INSENSITIVE);

    private static final String good_part1 =
        whiteSpace + "public" +whiteSpace + "enum" +whiteSpace +"AttributesEnum" +whiteSpace +"\\{" + whiteSpace
        +"[\\w\\s,]*;" ;
    private static final Pattern good_pattern1 = Pattern.compile(good_part1, Pattern.CASE_INSENSITIVE);

    private static final String part2 =
        "("+ identifier +")"
        +whiteSpace + "\\{" +whiteSpace +"public Object get\\(" +"("+identifier +")" +whiteSpace +"obj\\)\\s*\\{" + whiteSpace
        +"return obj.get(" +identifier +")\\(\\s*\\)\\s*;" + whiteSpace +"\\}" + whiteSpace 
        +"public void put\\(" + identifier + " obj," +whiteSpace +"Object value\\)\\s*\\{" + whiteSpace
        +"obj.set(" +identifier +")\\(" 

        // either this is a typecast
        +"(?:(?:"
        +"\\("  +"("+identifier  +")" +"\\)"
        +")"
        // or this is VA 
        +"|(?:"
        +"(index)\\(\\)," + whiteSpace
        +"))"
    
        +"value\\);" +whiteSpace
        +"\\}" +whiteSpace +"\\}" + whiteSpace +"(,|;)";
    
    private static Pattern p2 = Pattern.compile(part2, Pattern.CASE_INSENSITIVE);
    
    private static Pattern commentPattern = Pattern.compile("<COMMENT_\\d+>");
    
    private static String getCase = "(\\s*case\\s+\\w+\\s*:\\s*return\\s+\\w+\\(\\s*\\)\\s*;\\s*)*";
    private static String getReturn = "\\s*return super.getAttrInvokeAccessor\\s*\\(\\s*index\\s*,\\s*attrDef\\s*\\)\\s*;\\s*";
    private static String getSwitch = "\\s*switch\\s*\\(\\s*index\\s*\\)\\s*\\{\\s*" + getCase + "default:\\s*" + getReturn + "\\}";
    private static String getAccessorDecl = "protected Object getAttrInvokeAccessor\\s*\\(\\s*int index\\s*,\\s*AttributeDefImpl attrDef\\s*\\) throws Exception\\s*\\{\\s*";
   
    private static String ifPattern = "if\\s*\\(\\(index >= AttributesEnum.firstIndex\\(\\)\\)\\s*&\\s*\\(index < AttributesEnum.count\\(\\)\\)\\)\\s*\\{\\s*";
    private static String enumAccess = "AttributesEnum.staticValues\\(\\)\\[index\\s*-\\s*AttributesEnum.firstIndex\\(\\)\\]\\.";
    private static String optionalTypeCast = "(\\(\\s*\\w+\\s*\\))?";
    
    private static Pattern getAccessorPattern1 = Pattern.compile(getAccessorDecl + getSwitch + "\\s*\\}");
    private static Pattern getAccessorPattern2 = Pattern.compile(getAccessorDecl + getReturn + "\\s*\\}");
    private static Pattern getAccessorPattern3 = Pattern.compile(getAccessorDecl + ifPattern + "return\\s+" + enumAccess + "get\\(this\\)\\s*;\\s*\\}" + getReturn + "\\}");
      
    private static String setCase = "(\\s*case\\s+\\w+\\s*:\\s*\\w+\\(\\s*" + optionalTypeCast + "value\\s*\\)\\s*;\\s*return\\s*;\\s*)*";
    private static String setReturn = "\\s*super.setAttrInvokeAccessor\\s*\\(\\s*index\\s*,\\s*value\\s*,\\s*attrDef\\s*\\)\\s*;(\\s*return\\s*;)?\\s*";
    private static String setSwitch = "\\s*switch\\s*\\(\\s*index\\s*\\)\\s*\\{\\s*" + setCase + "default:\\s*" + setReturn + "\\}";
    private static String setAccessorDecl = "protected void setAttrInvokeAccessor\\s*\\(\\s*int index\\s*,\\s*Object value\\s*,\\s*AttributeDefImpl attrDef\\s*\\) throws Exception\\s*\\{\\s*";
    
    private static Pattern setAccessorPattern1 = Pattern.compile(setAccessorDecl + setSwitch + "\\s*\\}");
    private static Pattern setAccessorPattern2 = Pattern.compile(setAccessorDecl + setReturn + "\\s*\\}");
    private static Pattern setAccessorPattern3 = Pattern.compile(setAccessorDecl + ifPattern + enumAccess + "put\\(this\\s*,\\s*value\\s*\\)\\s*;\\s*return\\s*;\\s*\\}" + setReturn + "\\}");
      
    private void findIssue(File fFile, boolean doADE, ArrayList<String> code, HashMap<String,String> comments) throws Exception
    {
        int i; 
        for(i = 0; i < code.size(); i++) 
        {
            String line = code.get(i);

            if(line.contains("public enum AttributesEnum"))
                break;
        }
        
        if(i >= code.size())
            return;


        int start = i;

        String line = code.get(i);
        
        while(line.contains("{") == false)
        {
            i++;
            line += code.get(i);
        }
        
        int curly_cnt = 1;
        

        String new_block1;
        
        String block = line + "\n";
        
        
        while(curly_cnt > 0)
        {
            i++;
            line = code.get(i);
            if(line.contains("{"))
                curly_cnt++;
            else if(line.contains("}"))
                curly_cnt--;
            
            block += line + "\n";
        }
        
        int remove_end = i;
        
        Matcher m1 = commentPattern.matcher(block);
        block = m1.replaceAll(" ");
        
        Matcher m = good_pattern1.matcher(block);

        if(m.find() == true)
        {
            System.out.println("This file follows the good pattern");
            rowClassChanged = true; //there is no need to change the row class, so mark the status to true, so that xml change can be made if needed
            return;
        }
        
        writer.write(FamilyModuleHelper.getFileNameInfo(fFile.getAbsolutePath()) + "Bad AttributesEnum class pattern\n");
        

        m = pattern_part1.matcher(block);

        if(m.find() == false) {
            System.out.println("UNEXPECTED");
            return;
        }
        
        String g0 = m.group(0);
        new_block1 = g0;
        
        int end = m.end();
        
        String left = block.substring(end);
        
        Matcher m2 = p2.matcher(left);
        
        int numAttrs = 0;
        
        while(m2.find())
        {
            numAttrs++;
            g0 = m2.group(0);
            String g1 = m2.group(1);            
            new_block1 += g1 + ",";
        }

        if(numAttrs == 0)
        {
            System.out.println("UNEXPECTED");
            return;
        }

        int lastComma = new_block1.lastIndexOf(",");
        new_block1 = new_block1.substring(0, lastComma) + ";";
        new_block1 += "\n";
        new_block1 += "\n";
        new_block1 += "        private static AttributesEnum[] vals = null;\n";
        new_block1 += "        private static int firstIndex = 0;\n\n";

        new_block1 +="        public int index() {\n";
        new_block1 +="            return AttributesEnum.firstIndex() + ordinal();\n";
        new_block1 +="        }\n\n";
        new_block1 +="        public static int firstIndex() {\n";
        new_block1 +="            return firstIndex;\n";
        new_block1 +="        }\n\n";
        new_block1 +="        public static int count() {\n";
        new_block1 +="            return AttributesEnum.firstIndex() + AttributesEnum.staticValues().length;\n";
        new_block1 +="        }\n\n";
        new_block1 +="        public static AttributesEnum[] staticValues() {\n";
        new_block1 +="            if (vals == null) {\n";
        new_block1 +="                vals = AttributesEnum.values();\n";
        new_block1 +="            }\n";
        new_block1 +="            return vals;\n";
        new_block1 +="        }\n";
        new_block1 +="    }\n";

        code.set(start, new_block1);
    
    for(i = start+1; i <= remove_end; i++)
        code.remove(start +1);

    if (doADE == false)
        dumpFixedCode(code);

     try{
         doADE(doADE, fFile, code, comments);
         rowClassChanged = true;
     } catch (Exception e) {
        e.printStackTrace();
        logException(e);
     }
    }

    protected String getSummaryReportSubject() 
    {
        return "Script: Enum Transformer";
    }

    protected String getSummaryReport() 
    {
        return "just finished";
    }

    private void logException(Exception e) {
        m_numExceptions++;

        sExceptions +=
                "\n@@ Exception # " + m_numExceptions + "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n";
        sExceptions += e.toString() + "\n";
        StackTraceElement[] element = e.getStackTrace();
        for (int i = 0; i < element.length; i++) {
            sExceptions += element[i].toString() + "\n";
        }
    }
    
    public boolean checkForCustomAccessors(ArrayList<String> code){
        
        boolean gettercustomized = true;
        boolean settercustomized = true;
        
        String getAccessor = JavaFileParserHelper.getMethodWithName("getAttrInvokeAccessor", code);
        
        if(getAccessor == null)
            gettercustomized = false;
        else {
            Matcher m1 = getAccessorPattern1.matcher(getAccessor);
            Matcher m2 = getAccessorPattern2.matcher(getAccessor);
            Matcher m3 = getAccessorPattern3.matcher(getAccessor);
            
            if(m1.find() || m2.find() || m3.find())
                gettercustomized = false;
        }   
        
        String setAccessor = JavaFileParserHelper.getMethodWithName("setAttrInvokeAccessor", code);
        
        if(setAccessor == null)
            settercustomized = false;
        else {
            
            Matcher m1 = setAccessorPattern1.matcher(setAccessor);
            Matcher m2 = setAccessorPattern2.matcher(setAccessor);
            Matcher m3 = setAccessorPattern3.matcher(setAccessor);
            
            if(m1.find() || m2.find() || m3.find())
                settercustomized = false;
        }        
        
        return gettercustomized && settercustomized;
    }

}
