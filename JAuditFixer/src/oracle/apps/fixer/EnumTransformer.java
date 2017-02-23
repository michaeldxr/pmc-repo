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
import oracle.apps.utility.JoesBaseClass;

public class EnumTransformer extends JoesBaseClass
{
    static int m_reviewCount = 0;
    static BufferedWriter writer;

    public EnumTransformer(JoesBaseClass.CRAWL_TYPE type) {
        super(type);
    }

    public static void main(String[] args)  throws Exception{
        if (args.length <1 || args[0] == null || args[0].equals("")) {
            System.out.println("Usage: fixBloatedEnums.sh <path to your code in ade> <true for fixer mode/false for scanning>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        Boolean bDoADE = true;
        
        if(args.length >1)
            bDoADE  = new Boolean(args[1]);
        
        writer = new BufferedWriter(new FileWriter("enum_violations.txt"));
        writer.write("Family,Module,Product,FileName,Label,Issue\n");
//        String sCrawlDir = "/ade.../FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64.rdd/LATEST/" +
//            "fusionapps/fin/components/financialCommon/zx/configuration/party/publicModel" +
//            "/src/oracle/apps/financials/tax/configuration/party/publicModel/entity";
//        Boolean bDoADE = false;

        try {
            EnumTransformer enumTransformer =
                new EnumTransformer(JoesBaseClass.CRAWL_TYPE.JAVA);

            if (bDoADE.booleanValue())
                enumTransformer.startDiffLog();

            enumTransformer.crawlDirectory(sCrawlDir, bDoADE);

            if (bDoADE.booleanValue())
                enumTransformer.closeDiffLog();

            enumTransformer.logUsageReport(sCrawlDir);
            
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    protected void processFile(File fName, boolean bDoADE) 
    {
        if(fName.getPath().endsWith("RowImpl.java") || fName.getPath().endsWith("EOImpl.java")) 
        {
            System.out.println("Checking this file:" + fName);
            
            try{
                
                JavaFileParserHelper javaFileHelper = new JavaFileParserHelper(fName.getAbsolutePath());
                String fileContentsWoComments = javaFileHelper.preserveComments();
                ArrayList<String> code = javaFileHelper.getFileContentsArrayList(fileContentsWoComments);
                
              //  ArrayList<String> code = getCodeAsStrings(fName);

                findIssue(fName, bDoADE, code, javaFileHelper.comments);
            }
            catch (Exception e) 
            {
                e.printStackTrace();
                logException(e);                
            }

            m_reviewCount++;
        }
        
        return;
        
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
        
    private void findIssue(File fFile, boolean doADE, ArrayList<String> code, HashMap<String,String> comments) 
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
        String new_block2 = "";
        String new_block3 = "\n";
        
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
            return;
        }
        
        try{
          writer.write(FamilyModuleHelper.getFileNameInfo(fFile.getAbsolutePath()) + "Bad AttributesEnum class pattern\n");
        } catch(Exception e) {
            e.printStackTrace();
        }
        m = pattern_part1.matcher(block);

        if(m.find() == false) {
            System.out.println("UNEXPECTED");
            return;
        }
        
        String g0 = m.group(0);
        new_block1 = g0;
        
        int end = m.end();
        
        String left = block.substring(end);
        String className = "UNEXPECTED_CLASS_NAME";
        
        Matcher m2 = p2.matcher(left);
        
        int numAttrs = 0;
        
        while(m2.find())
        {
            numAttrs++;
            g0 = m2.group(0);
            String g1 = m2.group(1);
            String g2 = m2.group(2);
            String g3 = m2.group(3);
            String g4 = m2.group(4);
            String g5 = m2.group(5);
            String g6 = m2.group(6);
           // String g7 = m2.group(7);
            
            new_block1 += g1 + ",";
            
            new_block2 += "            case " + g1 +":\n";
            new_block2 += "                return object.get" + g3 +"();\n\n";

            new_block3 += "            case " + g1 +":\n";
            if("index".equals(g6))
            {
                new_block3 += "                object.setAttributeInternal(index(), value);\n";
                new_block3 += "                break;\n\n";
            }
            else
            {
                new_block3 += "                object.set" + g4 +"((" +g5 +")value);\n";
                new_block3 += "                break;\n\n";
            }

            className = g2;
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
        
        new_block1 += "        public Object get(" + className +" object) {\n";
        new_block1 += "            switch (this) {\n";
        
        new_block1 += new_block2;
        
        new_block1 += "            }\n\n";
        new_block1 += "            return null;\n";
        new_block1 += "        }";


        new_block1 += "\n";
        new_block1 += "\n";
        new_block1 += "        public void put(" + className +" object, Object value) {\n";
        new_block1 += "            switch (this) {";

        new_block1 += new_block3;

        new_block1 += "            }\n";
        new_block1 += "        }\n\n";

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

//    if (doADE == false)
//        dumpFixedCode(code);

     try{
        doADE(doADE, fFile, code, comments);
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

}
