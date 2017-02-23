package oracle.apps.fixer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.sql.Connection;

import java.sql.DriverManager;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.FileParserHelper;
import oracle.apps.helpers.JavaFileParserHelper;
import oracle.apps.helpers.Mail;
import oracle.apps.helpers.ProcessExecutor;
import oracle.apps.utility.JoesBaseClass;

public class EnumTransformer2 extends JoesBaseClass
{
    static int m_reviewCount = 0;
    //fully qualfied classname -> set of non-custom getter/setter methods that are not internally referenced.
    static HashMap<String,HashSet<String>> classMethods = new HashMap<String,HashSet<String>>();     
    static HashMap<String,String> classFile = new HashMap<String,String>(); //full classname - > abs file path    
    static HashMap<String,HashSet<String>> externalReferences = new HashMap<String,HashSet<String>>(); //classname,methodName -> potentially referencing class
    HashSet<String> exemptions = new HashSet<String>();
    static BufferedWriter bufwriter;

    static boolean followsGoodPattern = false;
    
    public EnumTransformer2(JoesBaseClass.CRAWL_TYPE type) {
        super(type);
    }
    
    public EnumTransformer2() {
        super(JoesBaseClass.CRAWL_TYPE.JAVA);
    }

    public static void main(String[] args)  throws Exception{
        if (args.length < 1 || args[0] == null || args[0].equals("")) {
            System.out.println("Usage:  fixBloatedEnums2.sh <path to your code in ade> <true for fixer mode/false for scanning>");
            System.exit(1);
        }

        String sCrawlDir = args[0];        
        Boolean bDoADE = true;  
        String codeline = "PREARU";

        if(args.length > 1)
            bDoADE = new Boolean(args[1]);

        if(args.length > 2)
            codeline = args[2].trim();
        
        Date startDate = new Date();
      
//        String sCrawlDir = "/ade../FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64.rdd/LATEST/fusionapps/" +
//            "fin/components/payables/ap/invoices/transactions/";
//        Boolean bDoADE = false;
        

        try {
            EnumTransformer2 enumTransformer =
                new EnumTransformer2(JoesBaseClass.CRAWL_TYPE.JAVA);
            
            bufwriter = new BufferedWriter(new FileWriter("cannot_fix.txt"));

            if (bDoADE)
                enumTransformer.startDiffLog();
            
            enumTransformer.readExemptionsFromFile();            
            enumTransformer.crawlDirectory(sCrawlDir, bDoADE);              
            enumTransformer.writeMap(); 
            enumTransformer.readExternalReferencesFromDB("FIN",codeline); 
            enumTransformer.pruneCandidates(); 
            
            enumTransformer.readExternalReferencesFromDB("HCM",codeline); 
            enumTransformer.pruneCandidates(); 
            
            enumTransformer.readExternalReferencesFromDB("SCM",codeline); 
            enumTransformer.pruneCandidates(); 
            
            enumTransformer.readExternalReferencesFromDB("CRM",codeline); 
            enumTransformer.pruneCandidates(); 
            
            enumTransformer.readExternalReferencesFromDB("ALL",codeline); 
            enumTransformer.pruneCandidates(); 
            
            enumTransformer.readExternalReferencesFromDB("PRC",codeline); 
            enumTransformer.pruneCandidates(); 
            enumTransformer.readExternalReferencesFromDB("PRJ",codeline); 
            enumTransformer.pruneCandidates(); 
            enumTransformer.readExternalReferencesFromDB("ATF",codeline); 
            enumTransformer.pruneCandidates(); 
            enumTransformer.readExternalReferencesFromDB("IC",codeline); 
            enumTransformer.pruneCandidates(); 
           
            enumTransformer.writeMap();
            
            enumTransformer.removeUnreferencedAccessors(bDoADE.booleanValue());
            
            if (bDoADE)
                enumTransformer.closeDiffLog();
            
            
            enumTransformer.logUsageReport(sCrawlDir);  

            Date endDate = new Date();
            System.out.println("Started at: " + startDate);
            System.out.println("Finished at: " + endDate);
            
            bufwriter.close();

            
        } catch (Exception e) {
            e.printStackTrace();
            String s = e.toString() + "\n\n\n";
            StackTraceElement[] element = e.getStackTrace();
            for (int i = 0; i < element.length; i++) 
                s += element[i].toString() + "\n";
            
            BufferedWriter bw = new BufferedWriter(new FileWriter("finalError.txt"));
            bw.write(s);
            bw.close();
        }
    }
    
    private void writeMap() throws Exception {
        
        //HashMap<String,HashSet<String>> classMethods
        
        Date d = new Date();
        String fileName = "classMethods" + d.toString() + ".txt";
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        
        for(Iterator<String> it1 = classMethods.keySet().iterator(); it1.hasNext();) {
            String className = it1.next();
            HashSet<String> methods = classMethods.get(className);
            String methodList = "";
            for(Iterator<String> it2 = methods.iterator(); it2.hasNext();)
                methodList += it2.next()+",";
            writer.write("\n" + className + " --> " + methodList);
        }
        
        writer.close();
    }

    protected void processFile(File fName, boolean bDoADE) 
    {
        if(fName.getPath().endsWith("RowImpl.java") || fName.getPath().endsWith("EOImpl.java")) 
        {
            System.out.println("Checking this file:" + fName);
            
            try{
                
                JavaFileParserHelper javaFileHelper = new JavaFileParserHelper(fName.getAbsolutePath());
                String fileContentsWoComments = javaFileHelper.preserveComments();
                
                String className = javaFileHelper.getClassNameNew(fileContentsWoComments);
                String packageName = javaFileHelper.getPackageName(fileContentsWoComments);
                HashSet<String> methods = new HashSet<String>();
                
                ArrayList<String> code = javaFileHelper.getFileContentsArrayList(fileContentsWoComments);
                code = javaFileHelper.removeMethodWithName("getAttrInvokeAccessor", code, false, "");
                code = javaFileHelper.removeMethodWithName("setAttrInvokeAccessor", code, false, "");
                
                followsGoodPattern = false;
                code = findIssue(code);
                
                if(!followsGoodPattern) {
                    try{
                       doADE(bDoADE, fName, code, javaFileHelper.comments);
                    } catch (Exception e) {
                       e.printStackTrace();
                       logException(e);
                    }
                }
                
                if(javaFileHelper.implementsInterface){
                    bufwriter.write("\nCannot remove unused accessors from: " + fName.getAbsolutePath() + " as it implements an interface.");
                    return;
                }
                
                fileContentsWoComments = javaFileHelper.getFileContentsAsString(code,false); // this code now has all get and setAttrInvokeAccessors, enum switch and private enum classes removed
                
                Matcher m = genericGetterMethod.matcher(fileContentsWoComments);
                String tempCode = m.replaceAll("");
                m.reset();
                while(m.find()) {
                    String methodName = m.group(1);
                    if(!tempCode.contains(methodName))
                        methods.add(methodName);
                }
                
                m = genericSetterMethod.matcher(fileContentsWoComments);
                tempCode = m.replaceAll("");
                m.reset();
                while(m.find()) {
                    String methodName = m.group(1);
                    if(!tempCode.contains(methodName))
                        methods.add(methodName);
                }
                if(methods.size() > 0) {
                    
                    classMethods.put(packageName + "." + className, methods);
                    classFile.put(packageName + "." + className, fName.getAbsolutePath());
                }
                
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
    
    public static Pattern commentPattern = Pattern.compile("<COMMENT_\\d+>");
    
    private static Pattern genericGetterMethod = Pattern.compile("public\\s+\\w+\\s+(get\\w+)\\(\\)\\s*\\{\\s*return\\s+\\(\\w+\\)\\s*getAttributeInternal\\(\\w+\\)\\s*;\\s*\\}", Pattern.DOTALL);
    private static Pattern genericSetterMethod = Pattern.compile("public\\s+void\\s+(set\\w+)\\(\\w+\\s+\\w+\\)\\s*\\{\\s*setAttributeInternal\\(\\w+,\\s*value\\)\\s*;\\s*\\}", Pattern.DOTALL);
     
    private ArrayList<String> findIssue(ArrayList<String> code) 
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
            followsGoodPattern = true;
            return code;
        }
        

        m = pattern_part1.matcher(block);

        if(m.find() == false) {
            System.out.println("UNEXPECTED");
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
            new_block1 += g1 + ",";
        }

        if(numAttrs == 0)
        {
            System.out.println("UNEXPECTED");
            return code;
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

     return code;
    }

    public String getSummaryReportSubject() 
    {
        return "Script: Enum Transformer";
    }

    protected String getSummaryReport() 
    {
        return "";
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
    
    
    private void pruneCandidates() {
        
        for(Iterator<String> it1 = classMethods.keySet().iterator(); it1.hasNext();) {
            
            String className = it1.next();         
            
            HashSet<String> methods = classMethods.get(className);
            
            for(Iterator<String> it2 = methods.iterator(); it2.hasNext();) {
                String methodName = it2.next();
                if(externalReferences.keySet().contains(className + "," + methodName))
                    it2.remove();
            }     
            //classMethods.put(className,methods);
        }
    } 
    
    private void removeUnreferencedAccessors(boolean bDoAde) { 
        
        for(Iterator<String> it1 = classMethods.keySet().iterator(); it1.hasNext();) {
            
            String className = it1.next();            
            String filePath = classFile.get(className);
            int index = filePath.lastIndexOf("/");
            String fileName = filePath.substring(index+1);
            
            try{
            
            JavaFileParserHelper javaFileHelper = new JavaFileParserHelper(filePath);
            String fileContents = javaFileHelper.getFileContentsAsString(false);
            ArrayList<String> code = javaFileHelper.getFileContentsArrayList(fileContents);
            
            HashSet<String> methods = classMethods.get(className);
            
            for(Iterator<String> it2 = methods.iterator(); it2.hasNext();) {
                String methodName = it2.next();
                if(exemptions.contains(fileName + "," + methodName))
                    continue;
                code = javaFileHelper.removeMethodWithName(methodName, code, false, "");
            }            
            
               doADE(bDoAde, new File(filePath), code);
            } catch (Exception e) {
               e.printStackTrace();
               logException(e);
            }            
        }
    }
    
    private void readExternalReferencesFromDB(String family, String codeline) {
        try{
            System.out.println("Reading Serialized HashMap from database...");
            Connection con = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","fintech","fintech");
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT REFERENCESMAP FROM SERIALIZEDEXTERNALREFERENCES WHERE CODELINE ='"+codeline+"' AND FAMILY ='" + family + "'");
            while(rs.next()) {
                byte[] buf = rs.getBytes(1);
                if(buf != null) {
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buf));
                    externalReferences = (HashMap<String,HashSet<String>>)ois.readObject();
                    ois.close();
                }
            }
            rs.close();
            st.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            Mail.sendMail("sudipti.gupta@oracle.com", e.getMessage(),
                          "Could not read external Enum References Serialized Map for family: " + family + " and codeline: " + codeline + ". Please review and try again.");
            System.exit(1);
        }
    }
    
    private void readExemptionsFromFile() {
        try{
            System.out.println("Reading Exceptions from file...");
            BufferedReader reader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") 
                                    + "/fatools/opensource/jauditFixScripts/txt/EnumException.txt")); 
            String line = "";
            while((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if(parts.length < 2)
                    continue;
                exemptions.add(parts[0].trim() + "," + parts[1].trim());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
  
    /*private void readExternalReferencesFromFile()  {
        
        try {
        BufferedReader reader = new BufferedReader(new FileReader("classMethods.txt"));
        String line = "";
    
        System.out.println("Reading Extrenal References from file...");
        
        while((line = reader.readLine()) != null) {
            if(line.contains("External References:"))
                break;
        }
        
        while((line = reader.readLine()) != null) {
            
            if(!line.trim().equals("")) {
                String[] parts = line.split("-->");
                if(parts.length < 2)
                    continue;
                String classString = parts[1].trim();
                classString = classString.replace("[","");
                classString = classString.replace("]","");
                String[] classes = classString.split(",");
                HashSet<String> refClasses = new HashSet<String>();
                for(int i=0;i<classes.length;i++)
                    refClasses.add(classes[i]);
                externalReferences.put(parts[0].trim(),refClasses);
            }
        } 
        }catch(Exception e){
            logException(e);
            e.printStackTrace();
        }
    }   */

}
