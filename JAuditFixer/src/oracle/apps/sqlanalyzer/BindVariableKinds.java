package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.io.ObjectInputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.Mail;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;
import oracle.apps.utility.JoesBaseClass;

import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BindVariableKinds extends JoesBaseClass {
    
    //HashMap<String,String> bindVarKinds = new HashMap<String,String>();
    HashSet<String> bindsReferencedinVCIs = new HashSet<String>();
    HashSet<String> bindsReferencedinWhere = new HashSet<String>();
    HashSet<String> bindsReferencedinDeclWhereClause = new HashSet<String>();
    HashMap<String,String> bindsReferencedinOptionalVCI = new HashMap<String,String>();
   // HashSet<String> bindsReferencedinWhere = new HashSet<String>();
    static BufferedWriter writer;
    HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();  
    HashMap<String,HashSet> exemptions1 = new HashMap<String,HashSet>();  
    
    static String viewRoot = "";
    static boolean makeFix = false;
    boolean bDebug = false;
    static String  sCrawlDir;
    private String m_sSummaryReport = "";
    private static String mode="view"; //can be view/LRG/premerge
    private static final String issuetype = "BindVarKindMismatch";
    private static int releaseNum=0;
    
    static HashMap<String,HashSet<String>> baseSubVos = new HashMap<String,HashSet<String>>();
    Pattern p = Pattern.compile(":\\s*(\\w+)",Pattern.DOTALL);
   
    public static void main(String[] args) {
        
        try{
        mode = System.getProperty("mode");
        if(mode == null)
            mode="view";
       // mode = "LRG";
            
        java.util.Date start = new java.util.Date();            
        System.out.println("Starting BindVariableKinds scan at: " + start);
            
        BindVariableKinds x = new BindVariableKinds();    
        x.readExternalReferencesFromDB();   //Build Base -> SubVO relationship map
        x.generateExemptionList();
        FamilyModuleHelper.generateDeferedPaths();
        
        writer = new BufferedWriter(new FileWriter("BindVarKindMismatch.csv"));         
        writer.write("Family,Module,Product,Filename,Series,Label," +
          "BindVariableName, BindVarKind,Referenced in VCI/Where, Extended VO?, Ignore Null Values?\n");   
        
        if(mode.equals("view")) {
            if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
                 System.out.println("Crawl directory must be specified");
                 System.exit(1);
             }
            sCrawlDir = args[0].trim();
          // sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_PT.R9ERP_LINUX.X64.rdd/LATEST/fusionapps/hcm/components/hcmPayroll/payrolls"; 
            
            File f = new File(sCrawlDir);
            if(!f.exists()) {
                System.out.println("Crawl directory does not exist - " + sCrawlDir);
                System.exit(1);
            }
            if(args.length > 1){
              String series = args[1].trim();
              String release= FamilyModuleHelper.getRelease(series);
              try{              
                  releaseNum = Integer.parseInt(release);
              }catch(NumberFormatException e){
                 e.printStackTrace();
                 releaseNum=0;
              }
            }
            
            if(args.length > 2)
                makeFix = new Boolean(args[2].trim());
            
            viewRoot = ViewCriteriaHelper.getViewRoot(sCrawlDir);
            
            System.out.println("Analyzing all files in directory: " + sCrawlDir);
            
       /*     if(makeFix) { //needed only when running the script in fixer mode
              System.out.println("Building baseVO subVo relationships for the family...");
              BaseSubVOAnalyzer basesubVoAnalyzer = new BaseSubVOAnalyzer();       
              int compIndex = sCrawlDir.indexOf("/components");
              if(compIndex != -1)
                basesubVoAnalyzer.crawlDirectory(sCrawlDir.substring(0, compIndex+11));
              else
                basesubVoAnalyzer.crawlDirectory(sCrawlDir);
              for(Iterator<String> it = BaseSubVOAnalyzer.base_subVos.keySet().iterator(); it.hasNext(); ) {
                  String baseVo = it.next();
                  HashSet<String> subVos = new HashSet<String>();
                  if(baseSubVos.containsKey(baseVo))
                      subVos = baseSubVos.get(baseVo);
                  subVos.addAll(BaseSubVOAnalyzer.base_subVos.get(baseVo));
                  baseSubVos.put(baseVo, subVos);
              }
             System.out.println("Done building baseVO subVo relationships for the family!");    
            }         */
            
            x.crawlDirectory(sCrawlDir,makeFix);            
            writer.close();
            
            if(makeFix){
                System.out.println("Fixing bind variable mismatches");
                x.startDiffLog();     
                x.makeFixes();
                x.closeDiffLog();    
                System.out.println("Done!");
            }   
        } //local mode
        else if(mode.equals("LRG")) {
            System.out.println("Running LRG version of the scan...");
            String series = args[0]; //only needs series name as the argument
            //String series = "FSM_MAIN_GENERIC";
            
            String release= FamilyModuleHelper.getRelease(series);
            try{              
                releaseNum = Integer.parseInt(release);
            }catch(NumberFormatException e){
               e.printStackTrace();
               releaseNum=0;
            }
         
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);
            
            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
          String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
          String viewRoot = serverTop + "/"+series+".rdd/"+label + "/";
            
            for (int j = 0; j < families.size(); j++) {
              
                String family = families.get(j);
                String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, issuetype);     
                System.out.println("Crawling dir '" + viewRoot + sCrawlDir + "' for family: " + family);
                x.crawlDirectoryNew(viewRoot,sCrawlDir,false);           
                System.out.println("Done crawling for family "+family+".");
            }
            writer.close();            
            LRGUtil.addLabelInfoToAB(issuetype, series, label);
            FamilyModuleHelper.printScanDuration(start,"BindVariableKinds");
        }
        else if(mode.equals("premerge")) {
            
            //String[] files = args[0].trim().split("\n"); //supply the list of files as first arg when running in premerge mode
            String ade_view_root = args[1].trim();
            String filelistpath=args[0].trim();
            String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root); 
            
            String label = args[2].trim();
            
            String release= FamilyModuleHelper.getRelease(label);
            System.out.println("Release: " + release);
            
            try{              
                releaseNum = Integer.parseInt(release);
                if (releaseNum < 9) //This check was mandated post release 9 only, hence this check is necessary to avoid the current rel7 and rel8 branches from failing
                    return;              
            }catch(NumberFormatException e){
                System.out.println("Could not determine release. Skipping BindVarKindMismatch check...");
                return;
            }
            for(int i = 0; i < files.length; i++) {
                
                String filePath = ade_view_root + "/" + files[i].trim();
                File f = new File(filePath);
                if(!f.exists())
                    continue;
                if(!x.fileOfInterest(filePath))
                    continue;
                x.processFile(f,false);
            }
            writer.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/BindVariableMismatch " +
                "for description of the issue and resolution.\n\n");
            writer.close();   
            
        }else {
            System.out.println("Unrecongixed mode of operation: " + mode);
            System.out.println("Mode must be local/LRG/premerge");
            System.exit(1);
        }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    public void makeFixes() throws Exception {
        
        BufferedReader fileReader = new BufferedReader(new FileReader("BindVarKindMismatch.csv"));
        String line = fileReader.readLine(); //first line is the header - read & ignore
        
        String prevFilePath = null;
        XMLDocument doc = null;
        XmlFixer xmlFixer = null;
        String fileContentsAsString = null;
        BufferedWriter writer1 = new BufferedWriter(new FileWriter("CouldNotFix.txt"));
        
        while((line = fileReader.readLine()) != null) {
          
          try{
            
            String[] parts = line.split(",");
            if(parts.length < 9)
                continue;
            
            if(line.contains("baseVO extended by other VOs")) {
                writer1.write("Cannot fix " + parts[3].trim() + " as the " + parts[9]);
                continue;
            }
            
            String filePath = viewRoot + parts[3].trim();
            String reference = parts[8].trim(); //where is the bind variable referenced
            String binVarName = parts[6].trim();
            String bindKind = parts[7].trim();
              
            if(parts.length >=12 && parts[11].contains("oracle.apps.") && !parts[11].contains("OAViewObjectImpl") 
               && bindKind.equals("where") && reference.equals("none")){
                   writer1.write("Cannot fix " + parts[3].trim() + " as there is a custom VOImpl class: (which might be accessing the bind variables) " + parts[11]);
                   continue;
               }
            
            String vcName = "";
            String vcrName = "";
            String vciName = "";
            boolean fixVCI = false;
          
            if(parts.length > 10) {
                vcName = parts[10].replace("Uncheck Ignore Null Values checkbox for the VC Item referencing this bind var:","").trim();
                fixVCI = true;
                
                String[] vcName_parts = vcName.split(":");
                if(vcName_parts.length != 2)
                    fixVCI = false;
                else{
                  vcrName = vcName_parts[0].trim();
                  vciName = vcName_parts[1].trim();
                }
            }
            
            String fixValue = "";
            if(reference.equals("where") || reference.equals("both"))
                fixValue = "where";
            else 
                fixValue = "viewcriteria";
            
        
            if(!filePath.equals(prevFilePath)) {
              
                if(prevFilePath != null) {
                  fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
                  doADE(true,new File(prevFilePath),fileContentsAsString); 
                }
              
              doc = XMLParserHelper.getXMLDocument(filePath);     
                if(doc == null) 
                    continue;
              xmlFixer = new XmlFixer(filePath);
            }
            prevFilePath = filePath;
            
            if(doc == null) 
                continue;
            
            NodeList bindVariables = doc.getElementsByTagName("Variable");            
                       
                for(int i =0; i < bindVariables.getLength(); i++){
                    Node var = bindVariables.item(i);
                    String name = XMLParserHelper.getAttributeValue(var, "Name");
                    if(name.equals(binVarName)){
                        xmlFixer.modifyAttribute(var, "Kind", bindKind, fixValue, true);
                        break;
                    }                
                }
              
              if(fixVCI){
               
                NodeList vcRows = doc.getElementsByTagName("ViewCriteriaRow");
                for(int j = 0; j < vcRows.getLength(); j++) {
                  Node vcr = vcRows.item(j);
                  String vcr_name = XMLParserHelper.getAttributeValue(vcr, "Name");
                 
                  if(vcr_name != null && vcr_name.equals(vcrName)) {
                    
                    Node vci = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(vcr, "ViewCriteriaItem", "Name", vciName);
                    if(vci != null) 
                      xmlFixer.modifyAttribute(vci, "GenerateIsNullClauseForBindVars", "true", "false", true);
                    
                    break;                    
                  }
                }
            
                
              }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if(xmlFixer != null) {
          fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
          doADE(true,new File(prevFilePath),fileContentsAsString); 
        }
        writer1.close();
    }
    
    public BindVariableKinds() 
    {
        super(JoesBaseClass.CRAWL_TYPE.VO_XML);
    }

    protected String getSummaryReportSubject() 
    {
        return "SCRIPT: Find Kinds of Bind variables";
    }

    protected String getSummaryReport() 
    {
        return m_sSummaryReport;
    }
    
    public void processFile(File fName, boolean bDoADE) {
        
        try {
            XMLDocument doc = XMLParserHelper.getXMLDocument(fName.getAbsolutePath());
            
            NodeList viewObjects = doc.getElementsByTagName("ViewObject");
            if(viewObjects == null || viewObjects.getLength() != 1) 
                return;
            
          
            NodeList viewCriteriaItems = doc.getElementsByTagName("ViewCriteriaItem");
            Node viewObject = viewObjects.item(0);
            
            //if its a programmatic VO, skip it
            Node designTime = XMLParserHelper.getChildNodeWithName(viewObject, "DesignTime");
            
            if(designTime != null) {
                Node expertMode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(designTime, "Attr", "Name", "_isExpertMode");
                if(expertMode != null){
                    String value = XMLParserHelper.getAttributeValue(expertMode, "Value");
                    if(value != null && value.equals("true")) {
                        Node sqlQuery = XMLParserHelper.getChildNodeWithName(viewObject, "SQLQuery");
                        if(sqlQuery == null)
                            //System.out.println("Skipping Programmatic VO: " + fName.getName());
                            return;
                    }
                }
            }
            
            String componentClass = XMLParserHelper.getAttributeValue(viewObject, "ComponentClass");
            
            
            NodeList variables = doc.getElementsByTagName("Variable");
            bindsReferencedinVCIs = new HashSet<String>();
            bindsReferencedinWhere = new HashSet<String>();
            bindsReferencedinDeclWhereClause = new HashSet<String>();
            bindsReferencedinOptionalVCI = new HashMap<String,String>(); //only store Optional VCI that have generateIsNullClauseForBindVars = true or not defined
            
            for(int i =0; i < viewCriteriaItems.getLength(); i++) {
                Node vci = viewCriteriaItems.item(i);
                String value = XMLParserHelper.getAttributeValue(vci, "Value");
                String vcRequired = XMLParserHelper.getAttributeValue(vci, "Required");
                String vciName = XMLParserHelper.getAttributeValue(vci, "Name");
                Node parentVcRow = XMLParserHelper.getParentNodeWithName(vci, "ViewCriteriaRow");
                vciName = XMLParserHelper.getAttributeValue(parentVcRow, "Name") + ":" + vciName;
                
                if(value == null) {
                    Node vciValue = XMLParserHelper.getChildNodeWithName(vci, "ViewCriteriaItemValue");
                    value = XMLParserHelper.getAttributeValue(vciValue, "Value");
                }
                if(value !=null && value.contains(":")) //i.e. value is a bind variable
                {
                    Node parentDeclWhereClause = XMLParserHelper.getParentNodeWithName(vci,"DeclarativeWhereClause");
                    if(parentDeclWhereClause == null)
                        bindsReferencedinVCIs.add(value);
                    else
                        bindsReferencedinDeclWhereClause.add(value);
                    
                    if(vcRequired ==null || vcRequired.equals("Optional")) {
                        String generateNullClause = XMLParserHelper.getAttributeValue(vci, "GenerateIsNullClauseForBindVars");
                        if(generateNullClause == null || generateNullClause.equals("true")) 
                            bindsReferencedinOptionalVCI.put(value, vciName);
                    }
                       
                }
            }
            
            String where = XMLParserHelper.getAttributeValue(viewObject, "Where");
            Node sqlQuery = XMLParserHelper.getChildNodeWithName(viewObject, "SQLQuery");
            String selectList = XMLParserHelper.getAttributeValue(viewObject, "SelectList");
            String sql = null;
          if(sqlQuery != null) {
                sql = sqlQuery.getTextContent();
                if(!FamilyModuleHelper.isEmpty(sql)) {
                  sql = sql.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
                  sql = sql.replaceAll("'---'","'NA'");
                  sql = sql.replaceAll("'--'","'NA'");
                  sql = sql.replaceAll("--.*$",""); //remove single line comments
                  Matcher m = p.matcher(sql);
                  while(m.find())
                      bindsReferencedinWhere.add(m.group(1));
                }
          }
            if(selectList != null) {
                selectList = selectList.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
                selectList = selectList.replaceAll("'---'","'NA'");
                selectList = selectList.replaceAll("'--'","'NA'");
                selectList = selectList.replaceAll("--.*$",""); //remove single line comments
                //selectList = SQLParserHelper.removeSingleLineSQLComments(selectList);
                Matcher m = p.matcher(selectList);
                while(m.find())
                    bindsReferencedinWhere.add(m.group(1));
            }
            if(where != null) {
                where = where.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
                where = where.replaceAll("'---'","'NA'");
                where = where.replaceAll("'--'","'NA'");
                where = where.replaceAll("--.*$",""); //remove single line comments
                //where = SQLParserHelper.removeSingleLineSQLComments(selectList);
                Matcher m = p.matcher(where);
                while(m.find())
                    bindsReferencedinWhere.add(m.group(1));
            }
            
            String fileNameInfo = ViewCriteriaHelper.getFileNameInfo1(fName.getAbsolutePath());
            
            String fileNameFromOracle = fName.getAbsolutePath();
            int index = fileNameFromOracle.indexOf("oracle/");
            if(index != -1)
                fileNameFromOracle = fileNameFromOracle.substring(index);
            fileNameFromOracle = fileNameFromOracle.replace(".xml","").replace("/",".");
            
            for(int i = 0; i < variables.getLength(); i++) {
              
  
              boolean vciReference = false;
              boolean whereReference = false;
              boolean declWhereClauseRef = false;
              String bindReference = "";
              boolean isViolation1 = true;
              boolean isViolation2 = false;
              
                Node var = variables.item(i);
                String bindName = XMLParserHelper.getAttributeValue(var, "Name");
               // bindVarKinds.put(XMLParserHelper.getAttributeValue(var, "Name"),XMLParserHelper.getAttributeValue(var, "Kind"));
                if(bindsReferencedinVCIs.contains(":"+bindName))
                    vciReference = true;
                if(bindsReferencedinDeclWhereClause.contains(":"+bindName)) {
                    //whereReference = true;
                    declWhereClauseRef = true;
                }
                
                if(bindsReferencedinWhere.contains(bindName))
                    whereReference = true;
                
                if(vciReference && whereReference)
                    bindReference = "both";
                else if(vciReference)
                    bindReference = "vci";
                else if(whereReference)
                    bindReference = "where";
                else
                    bindReference = "none";      
                
                String bindKind = XMLParserHelper.getAttributeValue(var, "Kind");
                if(bindKind == null)
                    bindKind="not defined";
                
                boolean bindReferencedinOptionalVC = false;
                if(bindsReferencedinOptionalVCI.containsKey(":" + bindName))
                    bindReferencedinOptionalVC = true;
                
                if((bindReference.equals("where") || bindReference.equals("both")) && bindKind.equals("where")) 
                    isViolation1 = false;                    
                if(bindReference.equals("vci") && bindKind.equals("viewcriteria"))
                    isViolation1 = false;
                if(bindReference.equals("none") && bindKind.equals("viewcriteria"))
                isViolation1 = false;

                if(bindReference.equals("none") && bindName.equals("BIConnectionName"))
                    isViolation1 = false;
              
               //Ignore kind if bind var is ONLY referenced in Decl Where Clause
              if(bindReference.equals("none") && declWhereClauseRef)
                 isViolation1 = false;
                
                if(bindReference.equals("both") && bindReferencedinOptionalVC){
                    if(!isExemptIgnoreNull(FamilyModuleHelper.getPathAfterViewRoot(fName.getAbsolutePath()), bindName))
                        isViolation2=true;
                }
                
                if(isViolation1 || isViolation2) { 
                    
                    if(isExempt(FamilyModuleHelper.getPathAfterViewRoot(fName.getAbsolutePath()), bindName))
                        continue;
                   
                    writer.write(fileNameInfo + bindName + "," + bindKind + "," + bindReference);             
                    if(baseSubVos.containsKey(fileNameFromOracle))
                        writer.write(",baseVO extended by other VOs (" + getSubVos(fileNameFromOracle) + ")");
                    else
                        writer.write(", ");
                    
                    if(isViolation2)
                        writer.write(",Uncheck Ignore Null Values checkbox for the VC Item referencing this bind var: " + bindsReferencedinOptionalVCI.get(":" + bindName));
                    else
                        writer.write(", ");
                    
                    writer.write(", " + componentClass + "\n");
                }
               
            }           
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    
    }
    
    private String getSubVos(String baseVo) {
        HashSet<String> subVos = baseSubVos.get(baseVo);
        String subVos_list = "";
        for(Iterator<String> i = subVos.iterator(); i.hasNext(); ){
            String subVo = i.next();
            subVos_list += subVo + "; ";
        }
        return subVos_list;
    }
    
    private void readExternalReferencesFromDB() {
        try{
            System.out.println("Reading Serialized HashMap from database...");
            Connection con = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","codescan","codescan");
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT SERIALIZED_OBJECT FROM SERIALIZEDREFERENCES WHERE NAME = 'BaseSubVos'");
       
            while(rs.next()) {
                byte[] buf = rs.getBytes(1);
                if(buf != null) {
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buf));
                    baseSubVos = (HashMap<String,HashSet<String>>)ois.readObject();
                    ois.close();
                }
            }            
            rs.close();
            st.close();
            con.close();
            System.out.println("Done reading Serialized HashMap from database !");
            
        } catch (Exception e) {
            e.printStackTrace();
//            Mail.sendMail("sudipti.gupta@oracle.com", e.getMessage() + ",ViewRoot: " + System.getenv("ADE_VIEW_ROOT") + ",Mode: " + mode,
//                          "Could not read serialized baseVOSubVO References from DB.Please review and try again.");
            if(!mode.equals("premerge"))
                System.exit(1);
        }
    }
    
    public void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_new.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 3)
            continue;
        String issue = parts[0].trim();
          
          if(issue.equals("BindVarKindMismatch")){
                String fileName = parts[1].trim();
                String varName = parts[2].trim();
                  
                HashSet<String> exemptLines = null;
                if(!exemptions.containsKey(fileName)) 
                  exemptLines = new HashSet<String>();
                else 
                  exemptLines = exemptions.get(fileName);
                
                exemptLines.add(varName);                
                exemptions.put(fileName, exemptLines);
        
          }else if(issue.equals("BindVarKindMismatch_IgnoreNull")) {
              
              String fileName = parts[1].trim();
              String varName = parts[2].trim();
                
              HashSet<String> exemptLines = null;
              if(!exemptions1.containsKey(fileName)) 
                exemptLines = new HashSet<String>();
              else 
                exemptLines = exemptions1.get(fileName);
              
              exemptLines.add(varName);
              exemptions1.put(fileName, exemptLines);
          }
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }
    
    private boolean isExempt(String fileName, String varName){
        
        if(releaseNum <= 12 && fileName.contains("fusionapps/scm/components/costManagement/cml"))
          return true;
        
        if(!exemptions.containsKey(fileName)) 
            return false;
        
        HashSet<String> variables = (HashSet<String>)exemptions.get(fileName);
        if(variables.contains(varName) || variables.contains("*"))
            return true;
        
        return false;
    }
    
    private boolean isExemptIgnoreNull(String fileName, String varName){
        
        if(!exemptions1.containsKey(fileName)) 
            return false;
        
        HashSet<String> variables = (HashSet<String>)exemptions1.get(fileName);
        if(variables.contains(varName) || variables.contains("*"))
            return true;
        
        return false;
    }
}
