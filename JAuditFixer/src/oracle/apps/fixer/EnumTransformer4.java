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

public class EnumTransformer4 extends JoesBaseClass
{
    static int m_reviewCount = 0;
    static BufferedWriter writer;
    static BufferedWriter writer1;
    static String getAttrInvokeAccessor = "";
    static String setAttrInvokeAccessor = "";
    static boolean rowClassChanged = false;
    static boolean voOnlyHasROAttrs = true; 
    static Boolean bDoADE = true;
    static Boolean append = false;

    public EnumTransformer4(JoesBaseClass.CRAWL_TYPE type) {
        super(type);
    }

    public static void main(String[] args)  throws Exception{
        if (args.length <1 || args[0] == null || args[0].equals("")) {
            System.out.println("Usage: fixBloatedEnums.sh <path to your code in ade> <true for fixer mode/false for scanning>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        
        if(args.length >1)
            bDoADE  = new Boolean(args[1]);
        
        if(args.length >2)
            append  = new Boolean(args[2]);
        
        writer = new BufferedWriter(new FileWriter("enum_violations.csv",append));
        if(!append)
            writer.write("Family,Module,Product,FileName,Series,Label,Issue\n");
        
        writer1 = new BufferedWriter(new FileWriter("enum_errors.txt",append));
        
        System.out.println("Running Enum scan on: " + sCrawlDir);

        try {
            EnumTransformer4 enumTransformer =
                new EnumTransformer4(JoesBaseClass.CRAWL_TYPE.JAVA_JPR);

            if (bDoADE.booleanValue())
                enumTransformer.startDiffLog();

            enumTransformer.crawlDirectory(sCrawlDir, bDoADE);

            if (bDoADE.booleanValue())
                enumTransformer.closeDiffLog();

            enumTransformer.logUsageReport(sCrawlDir);
            
            writer.close();
            writer1.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    public void processFile(File fName, boolean bDoADE) 
    {
       

        if(fName.getPath().endsWith("RowImpl.java") || fName.getPath().endsWith("EOImpl.java")) 
        {       
            
            String fName1 = "fusionapps/crm/components/crmPerformance/mot/territories/model/src/oracle"          
            +"/apps/sales/territoryMgmt/territories/metricService/view/TerritoryMetricSubjectAreaVORowImpl.java";

            String fName2 = "fusionapps/scm/components/productManagement/productCatalogManagement/changeManagement"
            +"/changeObjects/protectedModel/src/oracle/apps/scm/productCatalogManagement/changeManagement"
            +"/changeObjects/protectedUiModel/view/ChangeObjectVORowImpl.java";

            if(fName.getAbsolutePath().contains(fName1) || fName.getAbsolutePath().contains(fName2))
                 return;    
            
            try{
                JavaFileParserHelper javaFileHelper = new JavaFileParserHelper(fName.getAbsolutePath());
                String fileContentsWoComments = javaFileHelper.preserveComments();
                ArrayList<String> code = javaFileHelper.getFileContentsArrayList(fileContentsWoComments); 
                
                getAttrInvokeAccessor="";
                setAttrInvokeAccessor="";
                rowClassChanged= false;
                voOnlyHasROAttrs = true; //VO only has RO attrs

                code = findIssue(fName, bDoADE, code);
                
                if(rowClassChanged) {
                      code = javaFileHelper.removeMethodWithName("getAttrInvokeAccessor", code, true, getAttrInvokeAccessor);
                      if(voOnlyHasROAttrs) //IF VO only has RO attributes, do not generate setAttrInvokeAccessor method
                          code = javaFileHelper.removeMethodWithName("setAttrInvokeAccessor", code, false, "");
                      else
                          code = javaFileHelper.removeMethodWithName("setAttrInvokeAccessor", code, true, setAttrInvokeAccessor);
                      doADE(bDoADE, fName, code, javaFileHelper.comments);
                }
              
            }
            catch (Exception e) 
            {
                e.printStackTrace();
                logException(e);                
            }

            m_reviewCount++;
        }
        
        try {
            if(fName.getPath().endsWith(".jpr")) {
                processJpr(fName,bDoADE);
            }
        }catch(Exception e) {
            e.printStackTrace();
            try{
                writer1.write("Could not fix jpr: " + fName.getPath());
            } catch(Exception ex) {}
            logException(e); 
        }
        
    }
    
    public void processJpr(File f, boolean doADE) throws Exception {
        
        String absPath = f.getAbsolutePath();
        XMLDocument jprXml = XMLParserHelper.getXMLDocument(absPath);
        XmlFixer fixer = null;
        try{
            fixer = new XmlFixer(absPath);
        } catch(Exception e) {
            e.printStackTrace(); 
            writer1.write("\nCould not instantiate XmlFixer for jpr: " + absPath);
            return;
        }
        
        if (jprXml == null) {
            writer1.write("Skipping file .. Could not parse jpr as xml: " +
                               absPath); 
            return;
        }
        
        Node projectNode = XMLParserHelper.getChildNodeWithName(jprXml, "jpr:project");
        Node jboNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(projectNode, "value", "n", "JboProject");
        Node jpxNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(projectNode, "value", "n", "jbo.JpxName");
        if(jboNode == null || jpxNode == null || !XMLParserHelper.getAttributeValue(jboNode,"v").equals("true"))
            return;
        
        Node attrSwitchNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(projectNode, "value", "n", "jbo.AttrSwitchGeneration");
        Node attrEnumNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(projectNode, "value", "n", "jbo.NoAttrEnumClasses");
        
        if(attrSwitchNode == null || attrEnumNode == null || !XMLParserHelper.getAttributeValue(attrSwitchNode,"v").equals("true") 
           || !XMLParserHelper.getAttributeValue(attrEnumNode,"v").equals("true"))
            writer.write(FamilyModuleHelper.getFileNameInfo1(f.getAbsolutePath()) + "Jpr does not contain the right enum flags\n");
        
        boolean fixMade = false;
        if(attrSwitchNode == null) {
            fixer.addSiblingAfterNode(jpxNode, "<value n=\"jbo.AttrSwitchGeneration\" v=\"true\"/>");
            fixMade = true;
        }
        if(attrEnumNode == null) {
            fixer.addSiblingAfterNode(jpxNode, "<value n=\"jbo.NoAttrEnumClasses\" v=\"true\"/>");
            fixMade = true;
        }
        
        //Su: If flag is already present, check to see if there are multiple copies of the flag and fix if needed.
        if(attrSwitchNode != null) {
            ArrayList<Node> attrSwitchNodes = XMLParserHelper.getChildNodesWithMatchingAttributeAndName(projectNode, "value", "n", "jbo.AttrSwitchGeneration");
            for(int i = 1; i < attrSwitchNodes.size(); i++){
                fixer.removeNode(attrSwitchNodes.get(i));
                fixMade = true;
            }
        }
        if(attrEnumNode != null) {
            ArrayList<Node> attrEnumNodes = XMLParserHelper.getChildNodesWithMatchingAttributeAndName(projectNode, "value", "n", "jbo.NoAttrEnumClasses");
            for(int i = 1; i < attrEnumNodes.size(); i++) {
                fixer.removeNode(attrEnumNodes.get(i));
                fixMade = true;
            }
        }
        
        if (doADE && fixMade) {
            String newFileContents = fixer.getFileContentsAsString(true);
            doADE(doADE, f, newFileContents);
        }
        
    }


    private static final String part1 =
        whiteSpace + "public" +whiteSpace + "enum" +whiteSpace +"AttributesEnum" +whiteSpace +"\\{" + whiteSpace;
    private static final Pattern pattern_part1 = Pattern.compile(part1, Pattern.CASE_INSENSITIVE);


    //    private static final String good_part1 =
    //        whiteSpace + "public" +whiteSpace + "enum" +whiteSpace +"AttributesEnum" +whiteSpace +"\\{" + whiteSpace
    //        +"(" +whiteSpace +identifier + whiteSpace +",)*" +whiteSpace +identifier +";"; //resulted in catastrophic backtracking unless ?> atomic grouping"(?>" is used
    
    private static final String good_part1 =
        whiteSpace + "public" +whiteSpace + "enum" +whiteSpace +"AttributesEnum" +whiteSpace +"\\{" + whiteSpace
        +"[\\w\\s,]*;" ;
    private static final Pattern good_pattern1 = Pattern.compile(good_part1, Pattern.CASE_INSENSITIVE);

    private static final String part2 =
        "("+ identifier +")"
        +whiteSpace + "\\{" +whiteSpace +"public (?:java\\.lang\\.)?Object get\\(" +"("+javaIdentifier +")" +whiteSpace +"obj\\)\\s*\\{" + whiteSpace
        +"return obj.get(" +identifier +")\\(\\s*\\)\\s*;" + whiteSpace +"\\}" + whiteSpace 
        +"public void put\\(" + javaIdentifier + " obj," +whiteSpace +"(?:java\\.lang\\.)?Object value\\)\\s*\\{" + whiteSpace
        +"obj.set(" +identifier +")\\(" 

        // either this is a typecast
        +"(?:(?:"
        +"\\("  +"("+javaIdentifier  +")" +"\\)"
        +")"
        // or this is VA 
        +"|(?:"
        +"(index)\\(\\)," + whiteSpace
        +"))"
    
        +"\\s*value(?:\\.\\w+\\(\\s*\\))?\\);" +whiteSpace //Accomodate value.toString()
        +"\\}" +whiteSpace +"\\}";
    
    private static Pattern p2 = Pattern.compile(part2, Pattern.CASE_INSENSITIVE);
    
    private static Pattern commentPattern1 = Pattern.compile("<COMMENT_\\d+>");
    private static Pattern commentPattern2 = Pattern.compile("//.*\\n");
    private static Pattern firstIndexPattern = Pattern.compile("private (?:final )?static (?:final )?int firstIndex =(.*?);",Pattern.DOTALL);
  
        
    private ArrayList<String> findIssue(File fFile, boolean doADE, ArrayList<String> code) throws Exception
    {
        int i; 
        for(i = 0; i < code.size(); i++) 
        {
            String line = code.get(i);

            if(line.contains("public enum AttributesEnum"))
                break;
        }
        
        if(i >= code.size())
            return code;


        int start = i;

        String line = code.get(i);
        
        while(line.contains("{") == false)
        {
            i++;
            line += code.get(i);
        }
        
        int curly_cnt = 1;
        

        String new_block1;
        String new_block2 = "";
        String new_block3 = "\n";
        
        String block = line + "\n";
        
        
        while(curly_cnt > 0)
        {
            i++;
            line = code.get(i);
            if(line.trim().startsWith("//")) {
                block += line + "\n";
                continue;
            }
            if(line.contains("{"))
                curly_cnt++;
            else if(line.contains("}"))
                curly_cnt--;
            
            block += line + "\n";
        }
        
        int remove_end = i;
        Matcher mFirstIndex = firstIndexPattern.matcher(block);
        String firstIndex = "private static int firstIndex = ";
        if(mFirstIndex.find()) {
            firstIndex += mFirstIndex.group(1) + ";\n\n";
        }
        else{
            writer1.write("\nfirstindex not found (review) :" + fFile.getName());
           // firstIndex += "0;\n\n";
            return code;
        }
        
        Matcher m1 = commentPattern1.matcher(block);
        block = m1.replaceAll(" ");
        m1 = commentPattern2.matcher(block);
        block = m1.replaceAll("\n");
        
        Matcher m = good_pattern1.matcher(block);

        if(m.find() == true)
        {
            return code;
        }
        
        try{
          writer.write(FamilyModuleHelper.getFileNameInfo1(fFile.getAbsolutePath()) + "Bad AttributesEnum class pattern\n");
        } catch(Exception e) {
            e.printStackTrace();
        }
        m = pattern_part1.matcher(block);

        if(m.find() == false) {
            System.out.println("UNEXPECTED");
            try{
                writer1.write("\nUnexpected pattern1: " + fFile.getAbsolutePath());
            } catch(Exception e) {}
            return code;
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
            //String g2 = m2.group(2);
            String g3 = m2.group(3);
            String g4 = m2.group(4);
            String g5 = m2.group(5);
            String g6 = m2.group(6);

            new_block1 += g1 + ",\n        ";
            
            new_block2 += "            case " + g1 +":\n";
            new_block2 += "                return get" + g3 +"();\n";

            if(!"index".equals(g6))
            {
                new_block3 += "            case " + g1 +":\n";
                new_block3 += "                set" + g4 +"((" +g5 +")value);\n";
                new_block3 += "                return;\n";
                voOnlyHasROAttrs = false;
            }
        }
        
        new_block2 += "            }\n"; //end switch block        
        new_block2 = "            switch (vals[enumIndex]) {\n" + new_block2 ; //prepend swiitch 
        new_block3 += "            }\n";
        new_block3 = "            switch (vals[enumIndex]) {\n" + new_block3 ; //prepend swiitch 
        
        getAttrInvokeAccessor = "    /**\n" + 
        "     * getAttrInvokeAccessor: generated method. Do not modify.\n" + 
        "     * @param index the index identifying the attribute\n" + 
        "     * @param attrDef the attribute\n" + 
        "\n" + 
        "     * @return the attribute value\n" + 
        "     * @throws Exception\n" + 
        "     */\n";
        getAttrInvokeAccessor +=  "    protected Object getAttrInvokeAccessor(int index, AttributeDefImpl attrDef) throws Exception {\n";        
        getAttrInvokeAccessor += "        final AttributesEnum[] vals = AttributesEnum.staticValues();\n";
        getAttrInvokeAccessor += "        int enumIndex = index - AttributesEnum.firstIndex();\n";
        getAttrInvokeAccessor += "        if ((enumIndex >= 0) && (enumIndex < vals.length)) { \n";
        getAttrInvokeAccessor += new_block2;
        getAttrInvokeAccessor += "        }\n";
        getAttrInvokeAccessor += "        return super.getAttrInvokeAccessor(index, attrDef);\n";
        getAttrInvokeAccessor += "    }\n";
        
        setAttrInvokeAccessor = "    /**\n" + 
        "     * setAttrInvokeAccessor: generated method. Do not modify.\n" + 
        "     * @param index the index identifying the attribute\n" + 
        "     * @param value the value to assign to the attribute\n" + 
        "     * @param attrDef the attribute\n" + 
        "\n" + 
        "     * @throws Exception\n" + 
        "     */\n";
        setAttrInvokeAccessor += "    protected void setAttrInvokeAccessor(int index, Object value, \n" +
                                "                                        AttributeDefImpl attrDef) throws Exception {\n";
        setAttrInvokeAccessor += "        final AttributesEnum[] vals = AttributesEnum.staticValues();\n";
        setAttrInvokeAccessor += "        int enumIndex = index - AttributesEnum.firstIndex();\n";
        setAttrInvokeAccessor += "        if ((enumIndex >= 0) && (enumIndex < vals.length)) { \n";
        setAttrInvokeAccessor += new_block3;
        setAttrInvokeAccessor += "        }\n";
        setAttrInvokeAccessor += "        super.setAttrInvokeAccessor(index, value, attrDef);\n";
        setAttrInvokeAccessor += "    }\n";        

        if(numAttrs == 0)
        {
            System.out.println("UNEXPECTED");
            try{
                writer1.write("\nUnexpected pattern2: " + fFile.getAbsolutePath());
            } catch(Exception e) {}
            return code;
        }

        int lastComma = new_block1.lastIndexOf(",");
        new_block1 = new_block1.substring(0, lastComma) + ";";
        new_block1 += "\n";
        new_block1 += "\n";
        new_block1 += "        private static AttributesEnum[] vals = null;\n";
        new_block1 += "        " + firstIndex;

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
        new_block1 +="    }";

    code.set(start, new_block1);
    
    for(i = start+1; i <= remove_end; i++)
        code.remove(start +1);
    
    rowClassChanged= true;

    return code;
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

}
