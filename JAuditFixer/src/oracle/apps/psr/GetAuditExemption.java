package oracle.apps.psr;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import java.sql.Statement;

import java.sql.Types;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GetAuditExemption {
    
    static String m_psrDb = "jdbc:oracle:thin:@slcac706.us.oracle.com:1529:dbjaudit";
    static String m_psrUName = "dm_read_only";
    static String m_psrPwd = "read_only_connection";
    public static String exempted = "";
    public static String grandfather_category = null;
    public static Connection con= null;
                  
    
  public static void main(String[] args)  throws Exception{
      
      try {
        
        con = DriverManager.getConnection(m_psrDb, m_psrUName, m_psrPwd); 
        String filePath = args[0].trim();
        String ruleId = args[1].trim();
        String violationText = args[2].trim();
        String lineNo = args[3].trim();
        String label = args[4].trim();
          
          //System.out.println(violationText);
          //System.out.println(lineNo);
          //System.out.println(label);
          
        filePath = filePath.substring(filePath.indexOf("/fusionapps/") + 1);
          
        getAuditExemption(filePath,ruleId,violationText,lineNo,label);
         
        if(exempted.trim().equals("Y"))
            System.out.println("Exempted!");
        if(grandfather_category != null && !grandfather_category.trim().equals("null"))
            System.out.println("Grandfather Category: " + grandfather_category);

          
      } catch(Exception e) {
        System.out.println("EXCEPTION OCCURED: " + e.getMessage());
        e.printStackTrace();
      }
      finally {
          if(con != null)
              con.close();
      }
    }
  
  public static void getAuditExemption(String filePath,String ruleId,String violationText,String lineNo,String label) throws Exception {
   
   // Statement stmt = null;
   // ResultSet rs = null;
    CallableStatement cstmt = null;
      violationText = violationText.replaceAll("'", "\'");

    //stmt = con.createStatement();
      cstmt = con.prepareCall("{ call JAUDIT_MANAGER.GET_VIOLATION_INFORMATION(?,?,?,?,?,?,?) }");
      cstmt.registerOutParameter(6, Types.VARCHAR);
      cstmt.registerOutParameter(7, Types.VARCHAR);
      cstmt.setString(1, ruleId);
      cstmt.setString(2, filePath);
      cstmt.setString(3, lineNo);
      cstmt.setString(4, violationText);
      cstmt.setString(5, label);
      
      cstmt.execute();
      
      exempted = cstmt.getString(6);
      grandfather_category = cstmt.getString(7);
      cstmt.close();
      
      //System.out.println(ruleId + "," + filePath + "," + lineNo + "," + violationText + ":" + exempted + "," + grandfather_category);
      
     /* String sql = "SELECT codeline, regexp_for_error_text " +
        "FROM dm_audit_exempted_files, dm_audit_rules WHERE " +
        "DM_AUDIT_RULES.Audit_Rule_ID LIKE '%' || dm_audit_exempted_files.jaudit_rule_name "  +
          "and exempted_file_path='" + filePath + "'" + " and audit_rule_id = '" + ruleId +"'";
      
      rs = stmt.executeQuery(sql);  
      
      while(rs.next()) {
       
        String codeline = rs.getString("codeline");
        String regex = rs.getString("regexp_for_error_text");  
        Pattern pRegex = Pattern.compile(regex);
          
        if(labelCodelineMatch(label,codeline)){
              Matcher m = pRegex.matcher(violationText);
              if(m.find())
                  return true;
        }     
      }
    
        sql = "SELECT exempted_attribute,codeline " +
        "FROM dm_audit_exempted_attributes, dm_audit_rules WHERE " +
        "DM_AUDIT_RULES.Audit_Rule_ID LIKE '%' || dm_audit_exempted_attributes.jaudit_rule_name " +
        "and exempted_file_path='" + filePath + "'" + " and audit_rule_id = '" + ruleId + "'";
    
    rs = stmt.executeQuery(sql);  
    
    while(rs.next()) {
      
      String attrName =rs.getString("exempted_attribute");  
      String codeline = rs.getString("codeline");      
        
        if(labelCodelineMatch(label,codeline)){
            if(violationText.startsWith(attrName))
                return true;
        }
    }    
    
    
    sql = "SELECT codeline,exempted_line_number FROM " +
        "dm_audit_exempted_lines, dm_audit_rules WHERE " +
          "DM_AUDIT_RULES.Audit_Rule_ID LIKE '%' || dm_audit_exempted_lines.jaudit_rule_name "  +
        "and exempted_file_path='" + filePath + "'" + " and audit_rule_id = '" + ruleId + "'";
    
    rs = stmt.executeQuery(sql);  
    
    while(rs.next()) {

      String codeline = rs.getString("codeline");
        String exemptedLine = rs.getString("exempted_line_number");
       
        if(labelCodelineMatch(label,codeline)){
            if(exemptedLine.equals(lineNo))
                return true;
        }     
    }
    
    rs.close();
    stmt.close(); */
   // return false;
  }
  
  private static boolean labelCodelineMatch(String label, String codeline) {
      if(codeline.equals("ALL"))
          return true;
      if(label.contains("FUSIONAPPS_11.1.1.5.1_LINUX.X64") && codeline.equals("RUP2"))
          return true;
      if(label.contains("FUSIONAPPS_11.1.1.5.1") && !label.contains("FUSIONAPPS_11.1.1.5.1_LINUX") && codeline.equals("V1.0"))
          return true;
      if(label.contains("FUSIONAPPS_11.1.2.0.0") && codeline.equals("RUP1"))
          return true;
      if(label.contains("FUSIONAPPS_11.1_LINUX") && codeline.equals("RUP1"))
          return true;
      if(label.contains("FUSIONAPPS_PT.11.1.1_LINUX") && codeline.equals("RUP1"))
          return true;
      if(label.contains("B18") && codeline.equals("B18"))
          return true;
      if(label.contains("FUSIONAPPS_MAIN_LINUX") && codeline.equals("MAIN"))
          return true;
      if(label.contains("V2A_LINUX") && codeline.equals("MAIN"))
          return true;
      
      if(label.contains("RUP2") && codeline.equals("RUP2"))
          return true;
      if(label.contains("RUP1") && codeline.equals("RUP1"))
          return true;
      if(label.contains("V11") && codeline.equals("V1.1"))
          return true;
      if(label.contains("V12") && codeline.equals("V1.2"))
          return true;
      if(label.contains("V13") && codeline.equals("V1.3"))
          return true;
      if(label.contains("RUP4") && codeline.equals("RUP4"))
          return true;
      
        return false;
  }
}

