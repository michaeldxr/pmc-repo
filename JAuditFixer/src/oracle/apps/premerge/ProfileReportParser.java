package oracle.apps.premerge;

import java.io.File;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FileParserHelper;

import oracle.as.management.translation.Category;

public class ProfileReportParser {
  static String[] obsoletedRules = {
  "ta-service-connection-owsm-policy-external-secure-File_FmwCommon_54",
  "ta-service-connection-owsm-policy-internal-secure-File_FmwCommon_54",
  "ta-service-connection-owsm-policy-internal-other-File_FmwCommon_54", 
  "ta-service-connection-owsm-policy-internal-no-auth-File_FmwCommon_54", 
  "ta-connection-service-policy-synchronization-File_AdfModel_110", 
  "ta-connection-username-client-policy-valid-csf-key-File_AdfModel_111",
  "ta-connection-owsm-policy-supported_File_AdfModel_128",
  "fre-dm-3",
  "fre-test-2",
  };
  
    static Pattern pRuleNameId=Pattern.compile("<div>(.*?)&nbsp;/&nbsp;(.*?)</div>");
    static HashSet<String> ruleIds = new HashSet<String>();
    static HashSet<String> mandatoryRules = new HashSet<String>();
    static String projectHtml = "Profile-Report-for-Financials-Project-Rules.html";
    
    static String num = "13";
    static String reportHtml = null;
    static String release = null;
    
    public static void main(String[] args) throws Exception {
        String homeDir = System.getenv("HOME");      
        System.out.println("home directory is "+homeDir);

        if (args.length > 0) {
            num = args[0];
            Integer.parseInt(num);
            System.out.println("Set release to "+args[0]+"\n");
        }
        reportHtml = "Profile-Report-for-financialsMandatory_rel"+num+".html";
        release = "Release "+num;
        System.out.println(" "+release);
      
        //Step1. First construct the list of rules that are enabled in the profile (by scanning the profile report for current release profile + project level profile)
        
        String fileName = homeDir + File.separator + reportHtml;
        String fileContents = FileParserHelper.getFileContentsAsString(fileName);
        Matcher m = pRuleNameId.matcher(fileContents);
        
        while(m.find())
             ruleIds.add(m.group(2));
        
        //Su: Also add the project level rules to this list becasue project level rules are always run rel9 onwards
        fileName = homeDir + File.separator + projectHtml;;
        fileContents = FileParserHelper.getFileContentsAsString(fileName);
        m = pRuleNameId.matcher(fileContents);
                
        while(m.find())
             ruleIds.add(m.group(2));
        
        System.out.println("Total rules in the two files are "+ruleIds.size());
        //System.out.println(ruleIds);
        
        //Step 2. Construct the list of mandatory i.e. cat 1 & 2 rules for the given codeline/release from the audit database
        Connection con = DriverManager.getConnection("jdbc:oracle:thin:@slcac706.us.oracle.com:1529:dbjaudit", "dm_read_only", "read_only_connection");
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT audit_rule_id from dm_audit_rules_in_codelines where codeline = '"+release+"' and (rule_category = 'Category 1' or rule_category='Category 2')");
        
        List<String> obsolete = Arrays.asList(obsoletedRules);
        while(rs.next()) {
            String ruleId = rs.getString("audit_rule_id");
            if (obsolete.contains(ruleId)) {
                System.out.println("    ------ obsoleting "+ ruleId);
                continue;
            }
            mandatoryRules.add(ruleId);
        }
    
        rs.close();
        stmt.close();
        con.close();
        
        //Step3:       Find mandatory rules are missing from profile  (these should be added to the profile)
        System.out.println("\n\nFollowing mandatory rules are missing from profile:");
        // for(Iterator<String> it = mandatoryRules.iterator(); it.hasNext();){
        //    String rule = it.next();
        for (String rule : mandatoryRules)
            if(!ruleIds.contains(rule) && !rule.contains("crm-portlet"))
                System.out.println(rule);
        //}
        
        //Step 4: Find rules present in the profile are not mandatory (these should be removed from the profile)
        //Make sure you remove these from both the current release profile + project level profile
        System.out.println("\n\nFollowing rules present in the profile are not mandatory:");
        for(Iterator<String> it = ruleIds.iterator(); it.hasNext();){
            String rule = it.next();
            if(!mandatoryRules.contains(rule))
                System.out.println(rule);
        }
        System.out.println("Done");
    }
}
