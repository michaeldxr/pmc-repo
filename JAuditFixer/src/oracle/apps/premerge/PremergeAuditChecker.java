package oracle.apps.premerge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.sql.DriverManager;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.ProcessExecutor;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.psr.GetAuditExemption;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PremergeAuditChecker {
    
    static BufferedWriter writer;
    static BufferedWriter writer_gf;
    static HashMap<String,String> jprjws = new HashMap<String,String>(); //store abs file paths
    static HashMap<String,HashSet<String>> jprFiles = new HashMap<String,HashSet<String>>(); //store abs paths
    static HashMap<String,String> globalFileWorkspace = new HashMap<String,String>(); //store abs paths
    //static HashSet<String> soaProjects = new HashSet<String>(); //store soa jprs
    static HashSet<String> transactionFiles = new HashSet<String>(); //store filenames starting with fusionapps
    
    static String ade_view_root = null;
    static String cmd = "";
    static String cmd1 = "";
    static String profile = "";
    static String label = "";
    static String outputFile = "";
    static ProcessExecutor pe = null;
    static String mandatoryRulesFile = "";
    static HashSet<String> mandatoryRules = new HashSet<String>();
    
    static Pattern pViolation = Pattern.compile("([^:]+):(\\d+):([^:]+):([^:]+):(.*)");
    static int num_violations = 0;
    static int num_gf_violations = 0;
    
    public static void main(String[] args) throws Exception{
        
        int exitStatus = 0;
        ade_view_root = args[1].trim() + "/";  
        String filelistpath=args[0].trim();
        String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root); 
        label = args[2].trim();
        outputFile = args[3].trim();
        pe = new ProcessExecutor();
        
        profile = "financialsmandatory_v2.xml";
        mandatoryRulesFile = "audit_rules_source_v2.txt";
        
        int releaseNum = 13;
        
        String codeline = findCodeLineForLabel();
        System.out.println("Codeline: " + codeline);
      
        if(codeline.equals("V1.2")){
            profile = "financialsmandatory_v12.xml"; 
            mandatoryRulesFile = "audit_rules_source_v12.txt";
        }
        else if(codeline.equals("Release 8")){
            profile = "financialsmandatory_rel8.xml"; 
            mandatoryRulesFile = "audit_rules_source_rel8.txt";
        }
        else if(codeline.equals("Release 7")){
            profile = "financialsmandatory_rel7.xml";
            mandatoryRulesFile = "audit_rules_source_rel7.txt";
        }
        else if(codeline.equals("Release 9")){
            profile = "financialsmandatory_rel9.xml"; 
            mandatoryRulesFile = "audit_rules_source_rel9.txt";
        }
        else if(codeline.equals("Release 10")) {
            profile = "financialsmandatory_rel10.xml"; 
            mandatoryRulesFile = "audit_rules_source_rel10.txt";
        }
        else if(codeline.equals("Release 11")){
            profile = "financialsmandatory_rel11.xml"; 
            mandatoryRulesFile = "audit_rules_source_rel11.txt";
        }
        else if(codeline.equals("Release 12")){
            profile = "financialsmandatory_rel12.xml"; 
            mandatoryRulesFile = "audit_rules_source_rel12.txt";
        }
        else if(codeline.equals("Release 13")){
            profile = "financialsmandatory_rel13.xml"; 
            mandatoryRulesFile = "audit_rules_source_rel13.txt";
        }
        else if(codeline.equals("Release 14")){
          profile = "financialsmandatory_rel14.xml"; 
          mandatoryRulesFile = "audit_rules_source_rel14.txt";
        }  
        
        String release = FamilyModuleHelper.getRelease(label);
        try{
            if(release==null||release.trim().equals("")){
                System.out.println("Release number cannot be obtained from" +
                    " http://fabs.oraclecorp.com/fabs/rest/ro/getSeriesDetails?series=$series and " +
                    " http://oracruise.oraclecorp.com/fusionapps/rest/projects/$series/withRelease. " +
                    "Use the release 13 as  the default release number.");    
            }else{
                releaseNum = Integer.parseInt(release);       
            }
            
        } catch(Exception e){
            e.printStackTrace();
            releaseNum=13;
        }
        
        String labelServer = FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX");
        
        BufferedReader reader = new BufferedReader(new FileReader(labelServer + "/fatools/opensource/jauditFixScripts/txt/" + mandatoryRulesFile));
        String line = "";
        while((line = reader.readLine()) != null) {
            mandatoryRules.add(line.trim());
        }
        
        String jdevHome = System.getenv("JDEV_HOME");    // bug 21752382
        if (jdevHome == null || jdevHome.isEmpty()) {
          jdevHome = ade_view_root + "fmwtools/BUILD_HOME/jdeveloper";
        }
        System.out.println("jdevHome="+jdevHome);
        String cmdCommPart = jdevHome + "/jdev/bin/ojaudit -quiet -J-Dide.extension.extra.search.path=" + ade_view_root + 
                           "fatools/extensions/ -J-Dide.extension.role.search.path=" + ade_view_root + 
                           "fatools/roles/ -profile " + labelServer + "/fatools/opensource/jauditFixScripts/xml/";
        String cmdRuleStyle = " -role oracle.apps.role -style " + labelServer + "/fatools/opensource/jauditFixScripts/txt/audit-text2.xsl";
        
        cmd  = cmdCommPart + profile + cmdRuleStyle;
        cmd1  = cmdCommPart + "financialsProjectRules.xml" + cmdRuleStyle;
        
        writer = new BufferedWriter(new FileWriter(outputFile));
        writer_gf = new BufferedWriter(new FileWriter("Grandfather_violations.txt"));
   
        BufferedWriter writer1 = new BufferedWriter(new FileWriter("raw_audit.txt"));
        writer1.close();
        
        Date d1 = new Date();
        
       for (int i =0; i < files.length; i++)
           System.out.println(files[i].trim());
        System.out.println("Profile: " + profile);
        
        for (int i =0; i < files.length; i++)
            findJprAndJws(files[i].trim());
        
//        System.out.println(jprjws);
//        System.out.println(jprFiles);
//        System.out.println(globalFileWorkspace);
        
        for (Iterator<String> it = jprFiles.keySet().iterator(); it.hasNext();) {
            String jpr = it.next();
            
            if(jpr.endsWith("Test.jpr")) //exclude test projects
                continue;
            
            String jws = jprjws.get(jpr);
            
            if(jws == null) {
                writer.write("\nCannot find workspace for jpr: " +  jpr + 
                             "\n. Please audit this project manually.\n\n");
                exitStatus = 1;
                continue;
            } 
            
            String auditCommand = cmd;
            
           /* if(isSoaProject(jpr)){
                auditCommand += " -workspace " + jws + " " + jpr;
                soaProjects.add(jpr);
            }
            else{         */   
                HashSet<String> filesinProject = jprFiles.get(jpr);
                String listOfFiles = "";
                for(Iterator<String> it1 = filesinProject.iterator(); it1.hasNext();)
                    listOfFiles += ade_view_root + it1.next() + " ";
                
                auditCommand += " -project " + jpr + " -workspace " + jws + " " + listOfFiles;
           // }
            System.out.println("\n\nAudit Command: \n" + auditCommand);
            ProcessBuilder pb = new ProcessBuilder("bash","-c",auditCommand.trim());
            //pb.redirectErrorStream(true);
            pe.runProcess(pb, "raw_audit.txt", true); 
            
            if(releaseNum >= 9){
                System.out.println("\n\nAudit Command1: \n" + cmd1 + " -workspace " + jws + " " + jpr);
                pb = new ProcessBuilder("bash","-c",cmd1 + " -workspace " + jws + " " + jpr);
                pe.runProcess(pb, "raw_audit.txt", true);
            }
            
        }
        
        if(!globalFileWorkspace.isEmpty()) {
            for(Iterator<String> it = globalFileWorkspace.keySet().iterator(); it.hasNext();) {
                String globalFile = it.next();
                String jws = globalFileWorkspace.get(globalFile);
                
                String auditCommand = cmd + " -workspace " + jws + " " + globalFile;
                
                System.out.println("\n\nAudit Command: \n" + auditCommand);
                ProcessBuilder pb = new ProcessBuilder("bash","-c",auditCommand.trim());
                pe.runProcess(pb, "raw_audit.txt", true);                        
            }
        }

        processRawAudit();
        
        Date d2 = new Date();
        
        System.out.println("Jaudit Processing started at:" + d1);
        System.out.println("Completed Jaudit processing :" + d2);
        writer.write("\n\nNumber of violations found: " + num_violations);
        writer.write("\n\nAudit of transaction is now complete.\n\nResults are saved in the file " + outputFile + "\n");       
        if(num_gf_violations > 0)
            writer.write("\n\nTransaction contains grandfatherd violations. These are not mandatory to fix." +
                "\nHowever,you my take a look at $ADE_VIEW_ROOT/premerge/Grandfather_violations.txt " +
                "file for the list of grandfathered violations.\n\n");
        
        writer.close();
        writer_gf.close();
        
        if(num_violations > 0)
            exitStatus = 1;
        System.exit(exitStatus);
    }
    
    /*private static boolean isSoaProject(String jpr) {
       File f = new File(jpr);
       if(!f.exists())
           return false;
       File folder = f.getParentFile();
       File[] listOfFiles = folder.listFiles();
       for(int i = 0; i < listOfFiles.length; i++) {
           if(listOfFiles[i].getName().contains("composite.xml"))
               return true;
       }
       return false;
    }*/
    
    private static String findCodeLineForLabel() {
        try {
            String line = "";
            
            File f = new File(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX")
                              +"/fatools/opensource/jauditFixScripts/txt/seriesCodelines.txt");
            
            BufferedReader reader = new BufferedReader(new FileReader(f));
            while((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if(parts.length<2)
                    continue;
                String series = parts[0].trim();
                String codeline = parts[1].trim();
                if(label.contains(series))
                    return codeline;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return "Release 9";           // default to REL9 if not specified
    }
    
    private static boolean isFileOfInterest(String filePath) {
        
        File f = new File(ade_view_root + filePath);
        filePath = filePath.toLowerCase();
        
        if(filePath.contains("test.jpr") || filePath.contains("cwallet.sso") || filePath.endsWith(".jws")
            || filePath.endsWith(".zip") || filePath.endsWith(".html") || filePath.endsWith(".adfc_diagram")
            || filePath.contains("wf_client_config.xml") 
            || filePath.contains("weblogic-application.xml") || filePath.contains("connections.xml")
            || filePath.contains("noship") 
            || filePath.contains("db/plsql") || filePath.contains("db/data"))    {  
               
               System.out.println("Skipping: " + filePath);
               return false;
           }
        
        if(!f.exists() || f.isDirectory()) {
            System.out.println("File does not exist: " + filePath);
            return false;
        }
        
        return true;
    }
    
    public static void findJprAndJws(String filePath) throws Exception {
        
        if(!isFileOfInterest(filePath))         
            return;    
        
      String absFilePath = ade_view_root + filePath;
      
      transactionFiles.add(filePath);
        
      String jpr = null;   
      String jws = null;
      HashSet<String> filesUnderJpr = new HashSet<String>();
      
      while(jws == null) {
          
        int index = absFilePath.lastIndexOf("/");
        if(index == -1)
            return;
       
        absFilePath = absFilePath.substring(0,index);
        File folder = new File(absFilePath);
        File[] listOfFiles = folder.listFiles();
        
        if(listOfFiles == null)
            return;
        
        for(int i = 0; i <listOfFiles.length; i++)
        {
          String sName = listOfFiles[i].getPath();
            if(jpr == null && sName.endsWith(".jpr")) {
                jpr = sName;
                if(jprFiles.containsKey(sName))
                    filesUnderJpr = jprFiles.get(sName);
 
                if(!filePath.endsWith(".jpr"))
                    filesUnderJpr.add(filePath);
                else
                    jpr = null;
                if(jpr != null)
                    jprFiles.put(jpr,filesUnderJpr);
                
                if(jprjws.containsKey(jpr))
                    jws = jprjws.get(jpr);
                
            }
            if(jws == null && sName.endsWith(".jws")){
                jws = sName;
                if(jpr != null) {
                    String jprInJws = checkJprJwsMembership(jpr,sName);
                    if(jprInJws.equals("jpr found in jws"))
                        jprjws.put(jpr,jws);
                    else{
                        System.out.println(jws + " is not the correct workspace for " + jpr);
                        jws = null;
                    }
                }
                else
                    globalFileWorkspace.put(ade_view_root + filePath,jws);
            }            
        } //for
      } //while
    }
    
    
    public static String checkJprJwsMembership(String jpr, String jws) throws Exception{
        
        File jprFile = new File(jpr);
        File jwsFile = new File(jws);
        
        if(!jprFile.exists() || !jwsFile.exists()) {
            return "Jpr or jws does not exist";
        }
        HashSet<String> listOfJprs = new HashSet<String>();
        XMLDocument doc = XMLParserHelper.getXMLDocument(jwsFile.getAbsolutePath());
        NodeList rootNodes = doc.getElementsByTagName("workspace");
        if(rootNodes == null || rootNodes.getLength() != 1) {
            System.out.println("Incorrect number of root nodes - jws:workspace..");
            System.exit(1);
        }
        Node root = rootNodes.item(0);
        Node n = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(root, "list", "n", "listOfChildren");
        
        NodeList children = n.getChildNodes();
        for(int i = 0; i < children.getLength(); i++) {
            Node hashNode = children.item(i);
            if(!hashNode.getNodeName().equals("hash"))
                continue;
            Node urlNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(hashNode, "url", "n", "URL");
            String jprValue = XMLParserHelper.getAttributeValue(urlNode, "path");
            if(!isEmpty(jprValue))
                listOfJprs.add(jprValue);
        }
        
        int jwsIndex = jws.lastIndexOf("/");
        if(jwsIndex != -1) {
            String jwsRoot = jws.substring(0, jwsIndex);
            int jprIndex = jpr.indexOf(jwsRoot);
            String jprValue = jpr.substring(jprIndex + jwsRoot.length() + 1);
            if(!listOfJprs.contains(jprValue)) 
               return("The jpr file " + jpr + " is not associated with the workspace " + jws + ".");
            else 
               return "jpr found in jws";
        }
        
        return "";
    }
    
    private static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("") || str.trim().equals("null")) {
            return true;
        }
        return false;
    }

    
    private static void processRawAudit() throws Exception{
        
        BufferedReader reader = new BufferedReader(new FileReader("raw_audit.txt"));
        
        System.out.println("Processing raw audit...");
        boolean apexConEstablished = false;
        
        try{ 
             GetAuditExemption.con = DriverManager.getConnection("jdbc:oracle:thin:@slcac706.us.oracle.com:1529:dbjaudit",
                                                            "dm_read_only", "read_only_connection"); 
            apexConEstablished= true;
        } catch(Exception e){
            e.printStackTrace();
            writer.write("\n\n Could not establish connection to Apex DB. The exemption framework will not work.\n" +
                "If the tool reports a violation for which you have an exemption registered, please ignore the premerge " +
                "failure and proceed with merge stating your argument.\n\n");
        }
        
        String line = "";
        while((line = reader.readLine()) != null) {
            Matcher m = pViolation.matcher(line);
            if(m.find()) {
                try{
                    System.out.println("Violation found: " + line.substring(0, 100) + " ...");
                } catch (Exception e) {
                 //   e.printStackTrace();
                }
                String fileName = m.group(1).trim();
                String fileNameAfterFusionapps = getPathAfterFusionapps(fileName);
                String lineNo = m.group(2).trim();
                String ruleId = m.group(3).trim();
                String rule = m.group(5).trim();
                
               /* if(ruleId.contains("internal.disabled-rules") || ruleId.contains("internal.traversal-exception")
                    || ruleId.contains("internal.visitor-exception") || ruleId.contains("internal.unauditable-file")
                    || ruleId.contains("oracle.jbo.view-validate-names") || ruleId.contains("ta-internal-error")
                    || rule.contains("INTERNAL:") || rule.contains("Rules disabled:") || ruleId.contains("internal.method-internal")
                    || ruleId.contains("dvt-category.dvthv-rule-compnotvalidsearchresultschild") || ruleId.contains("dvt-category.dvthv-rule-invalidTruncateAtInsideOutputText")
                    || ruleId.contains("internal.constructor-internal") || ruleId.contains("internal.field-internal")
                    || ruleId.contains("internal.type-internal") || ruleId.contains("adfm-internal")
                    || ruleId.contains("oracle.jbo.view.lov-entityviewaccessor")
                    || ruleId.contains("ta-internal-jbo-unable-to-find-referenced-object")
                    || rule.contains("criteria used in the include search region is also used by the underlying view accessor")
                    || (rule.contains("Derived attribute") && rule.contains("in this list binding has a list binding"))
                    || (rule.contains("Derived attribute") && rule.contains("in this list binding is readonly")) 
                    || (rule.contains("List Binding is bound to a Combobox") && rule.contains("LOV Control") && rule.contains("large Rowsets")))
                  continue;
                
                //These are Cat 3 but enabld by default in ojaudit after PS6..
                if(ruleId.contains("adfjs-function-or-name-conflict")
                    || ruleId.contains("dvtacc-category.dvtacc-rule-graphtimeselectornoadasupport")
                    || ruleId.contains("oracle.adfdt.controller.adfc.source.audit.DuplicateRegion")
                    || ruleId.contains("oracle.adfdt.controller.adfc.source.audit.TrainNotDefined")
                    || ruleId.contains("oracle.jbo.attribute-enumeration-classes-rule")
                    || ruleId.contains("oracle.jbo.attribute-order-rule")
                    || ruleId.contains("oracle.jbo.view-attribute-discriminator-rule")
                    || ruleId.contains("oracle.jbo.view-attributes-pkdiscr-rule")
                    || ruleId.contains("oracle.jbo.view.entityattr-sqlderived")
                    || ruleId.contains("private-adfjs-method-access")
                    || ruleId.contains("private-adfjsfunction-usage")
                    || ruleId.contains("protected-adfjs-method-access")
                    || ruleId.contains("oracle.jbo.eo-usage-association-rule")
                    || ruleId.contains("oracle.jbo.class-not-managed-rule")
                    || ruleId.contains("oracle.jbo.view.sdo-name-rule")
                    || ruleId.contains("oracle.jbo.eo-static-definition-rule")
                    || ruleId.contains("oracle.jbo.transient-expression-literal-rule"))
                    continue; */
                if(!mandatoryRules.contains(ruleId))
                    continue;
                
                if(apexConEstablished) {
//                    System.out.println("Apex connection established");
//                    System.out.println("Filename: " + fileNameAfterFusionapps);
//                    System.out.println("RuleId: " + ruleId);
//                    System.out.println("Rule: " + rule);
//                    System.out.println("lineNo: " + lineNo);
//                    System.out.println("label: " + label);
                    GetAuditExemption.getAuditExemption(fileNameAfterFusionapps,ruleId,rule,lineNo,label);
                    
                    if(GetAuditExemption.exempted.trim().equals("Y")) {
                        System.out.println("Exempted!");
                        continue;
                    }
                }
                
              //  if(transactionFiles.contains(fileNameAfterFusionapps)) {
                    
                    if(!isEmpty(GetAuditExemption.grandfather_category) && 
                       !GetAuditExemption.grandfather_category.equals("Category 1") &&
                       !GetAuditExemption.grandfather_category.equals("Category 2")) {
                    
                            writer_gf.write("FileName: " + fileNameAfterFusionapps + "\nLine #: " 
                                     + lineNo + "\nRuleId: " + ruleId + "\nError Text: " + rule);
                            writer_gf.write("\nGrandfather Category: " + GetAuditExemption.grandfather_category + "\n\n\n");
                            num_gf_violations++;
                    }
                    else {
                        writer.write("FileName: " + fileNameAfterFusionapps + "\nLine #: " 
                                     + lineNo + "\nRuleId: " + ruleId + "\nError Text: " + rule + "\n\n\n");
                            num_violations++;
                    }
              //  }                
            }
        }    //while      
        if(apexConEstablished)
            GetAuditExemption.con.close();
    }
    
    private static String getPathAfterFusionapps(String absPath) {
        
        int index = absPath.indexOf("fusionapps/");
        if(index == -1) 
            return absPath;
        else
            return absPath.substring(index);
    }
    
}
