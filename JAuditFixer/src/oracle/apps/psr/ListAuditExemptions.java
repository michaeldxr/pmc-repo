package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import java.sql.Statement;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;


public class ListAuditExemptions {
    
    static String m_psrDb = "jdbc:oracle:thin:@slcac706.us.oracle.com:1529:dbjaudit";
    static String m_psrUName = "dm_read_only";
    static String m_psrPwd = "read_only_connection";
    static BufferedWriter outputFileWriter;
    static BufferedWriter errorFileWriter;
    boolean bDebug = false;
    private static HashMap<String,String> auditRuleIdMessage = new HashMap<String,String>();
    Pattern p_rule = Pattern.compile("rule\\.(.*)\\.message");
    
  public static void main(String[] args) throws Exception {
      
      ListAuditExemptions auditExemptions = new ListAuditExemptions();
      try {
        
        auditExemptions.populateAuditRuleMessages();
        //System.out.println(auditRuleIdMessage);
        outputFileWriter = new BufferedWriter(new FileWriter("financialsexemptions.txt") );
        errorFileWriter = new BufferedWriter(new FileWriter("errors.txt") );
        errorFileWriter.write("Could not find messages for the following exemptions: \n");
          
        System.out.println("Generating exemption list...");
          
        auditExemptions.getAuditExemptions();
          
        outputFileWriter.close();
        errorFileWriter.close();        
          
      } catch(Exception e) {
        System.out.println("EXCEPTION OCCURED: " + e.getMessage());
        e.printStackTrace();
      }
    }
  
  private void getAuditExemptions() throws Exception {
   
    Connection con = null;
    Statement stmt = null;
    ResultSet rs = null;
    
    con = DriverManager.getConnection(m_psrDb, m_psrUName, m_psrPwd);
    stmt = con.createStatement();
    
    String sql = "SELECT exempted_file_path,exempted_attribute,gscc_standard,audit_rule_name FROM " +
        "dm_audit_exempted_attributes, dm_audit_rules WHERE " +
        "DM_AUDIT_RULES.Audit_Rule_ID LIKE '%' || dm_audit_exempted_attributes.jaudit_rule_name";
    
    rs = stmt.executeQuery(sql);  
    
    while(rs.next()) {
      String filePath =rs.getString("exempted_file_path");  
      String attrName =rs.getString("exempted_attribute");  
      String gscc_standard = rs.getString("gscc_standard");
      String audit_rule_name = rs.getString("audit_rule_name");
      
      String message = null;
      if(gscc_standard != null)
        message = auditRuleIdMessage.get(gscc_standard);
        
        if(message == null) {
           // errorFileWriter.write(filePath + "," + attrName + "," + audit_rule + "," + gscc_standard + "\n");
           // continue;
            message = audit_rule_name;
        }
      message = message.replaceFirst("\\{.*\\}",attrName);
        
      outputFileWriter.write(filePath + "\t-\t" + message + "\n");
    }
    
    sql = "SELECT exempted_file_path, gscc_standard, audit_rule_name FROM " +
        "dm_audit_exempted_files, dm_audit_rules WHERE " +
          "DM_AUDIT_RULES.Audit_Rule_ID LIKE '%' || dm_audit_exempted_files.jaudit_rule_name";
    
    rs = stmt.executeQuery(sql);  
    
    while(rs.next()) {
      String filePath =rs.getString("exempted_file_path");   
      String gscc_standard = rs.getString("gscc_standard");
      String audit_rule_name = rs.getString("audit_rule_name");
        
      String message = null;
      if(gscc_standard != null)
        message = auditRuleIdMessage.get(gscc_standard);
        
        if(message == null) {
           // errorFileWriter.write(filePath + "," + audit_rule + "," + gscc_standard + "\n");
           // continue;
            message = audit_rule_name;
        }
              
        outputFileWriter.write(filePath + "\t-\t" + message + "\n");
    }
    
    sql = "SELECT exempted_file_path,exempted_line_number,gscc_standard,audit_rule_name FROM " +
        "dm_audit_exempted_lines, dm_audit_rules WHERE " +
          "DM_AUDIT_RULES.Audit_Rule_ID LIKE '%' || dm_audit_exempted_lines.jaudit_rule_name";
    
    rs = stmt.executeQuery(sql);  
    
    while(rs.next()) {
      String filePath =rs.getString("exempted_file_path");  
      String lineNo = rs.getString("exempted_line_number");
      String gscc_standard = rs.getString("gscc_standard");
      String audit_rule_name = rs.getString("audit_rule_name");
        
      String message = null;
      if(gscc_standard != null)
        message = auditRuleIdMessage.get(gscc_standard);
        
        if(message == null) {
           // errorFileWriter.write(filePath + "," + audit_rule + "," + gscc_standard + "\n");
           // continue;
            message = audit_rule_name;
        }
              
       outputFileWriter.write(filePath + "\t" + lineNo + "\t" + message + "\n");
    }
    
    rs.close();
    stmt.close();
    con.close();
  }
  
  private void populateAuditRuleMessages() throws Exception {
    
    BufferedReader reader1 = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
        "/fatools/jaudit/JAuditForADF/JAuditExtensionForADF/src/oracle/apps/ta/jaudit/resources/StandardsAuditResource.properties"));
    
    String line = "";
    String ruleId = "";
    String message = "";
    while((line = reader1.readLine()) != null){
        if(line.startsWith("rule") && line.contains(".message")) {
          String[] parts = line.split("=");
          if(parts.length==2) {
              //writer1.write(parts[1].trim() + "\n");
              Matcher m = p_rule.matcher(parts[0].trim());
              if(m.find()){
                  ruleId = m.group(1);
                  message = parts[1].trim();
                  auditRuleIdMessage.put(ruleId, message);
              }
          }
        }
    }
    
    
    /*BufferedWriter writer2 = new BufferedWriter(new FileWriter("audit_rules_db.txt"));
    
    Connection con = null;
    Statement stmt = null;
    ResultSet rs = null;
    
    con = DriverManager.getConnection(m_psrDb, m_psrUName, m_psrPwd);
    stmt = con.createStatement();
    
    String sql = "SELECT distinct(audit_rule_name) FROM dm_audit_rules";
    
    rs = stmt.executeQuery(sql);  
    while(rs.next()) {
      String audit_rule =rs.getString("audit_rule_name");  
      writer2.write(audit_rule + "\n");
    }
    
    writer2.close();
    rs.close();
    stmt.close();
    con.close();*/
  }
}
