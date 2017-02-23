package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.FileReader;
import java.io.FileWriter;

import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.HashSet;

import oracle.apps.helpers.FamilyModuleHelper;

public class GenerateAuditProfile {
    
    static String m_psrDb = "jdbc:oracle:thin:@slcac706.us.oracle.com:1529:dbjaudit";
    static String m_psrUName = "dm_read_only";
    static String m_psrPwd = "read_only_connection";
    
    static String prjRuleFileDir = FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") + 
                                   "/fatools/opensource/jauditFixScripts/txt/audit_rules_source_prj.txt";
    
    public static void main(String[] args) throws Exception {
        
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        String home = System.getProperty("user.home");
        
        String rel = "12";
        
        if(args != null && args.length == 1){
            rel=args[0];
        }
        
        HashSet<String> prjRules = getPrjRules(prjRuleFileDir);
        
        BufferedWriter writer = new BufferedWriter(new FileWriter(home+"/jaudit/audit_rules_source_r" + rel + ".txt"));
        writer.write("<?xml version = '1.0' encoding = 'UTF-8'?>\n<audit-rules>\n");
        
        System.out.println("Generating source audit profile for Release " + rel + "...");
        
        con = DriverManager.getConnection(m_psrDb, m_psrUName, m_psrPwd);
        stmt = con.createStatement();
        
          String sql = "SELECT ac.audit_rule_id from dm_audit_rules_in_codelines ac, dm_audit_rules ar" + 
          " where (ac.rule_category = 'Category 1' or ac.rule_category='Category 2') and ac.codeline = 'Release " + rel + "'" + 
          " and ac.audit_rule_id=ar.audit_rule_id order by ac.audit_rule_id";
//          String sql = "SELECT audit_rule_id from dm_audit_rules_in_codelines where codeline = 'Release 12' and (rule_category = 'Category 1' or rule_category='Category 2') order by audit_rule_id";
          
          rs = stmt.executeQuery(sql);  
          
          boolean resultsFound = false;
          
          while(rs.next()) {
              resultsFound = true;
              String ruleId = rs.getString("audit_rule_id");
              if(!prjRules.contains(ruleId)){
                  writer.write("<audit-rule ruleId=\""+ruleId+"\" enabled=\"true\" style=\"error\" severity=\"error\"/>\n");
                  //              writer.write(ruleId+"\n");
              }
          }
          
          if(!resultsFound)
              System.exit(1);
           
        writer.write("</audit-rules>\n");
        writer.close();
           
        rs.close();
        stmt.close();
        con.close();
        
        System.out.println("Finished Generating source audit profile.");
    }
    
    public static HashSet<String> getPrjRules(String file) throws IOException{
		    HashSet<String> prjRules = new HashSet<String>();
		    BufferedReader br = new BufferedReader(new FileReader(file));
		    String line;
	    
	       while ((line = br.readLine()) != null) {
	    	     prjRules.add(line);
	       }
		
		     return prjRules;
	  }
    
}
