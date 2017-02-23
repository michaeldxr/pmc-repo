package oracle.apps.psr;

import java.io.BufferedReader;

import java.io.FileNotFoundException;
import java.io.FileReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.HashMap;
import java.util.HashSet;

import java.util.Iterator;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.Mail;

public class AuditProfileUpdate {


  static String m_psrDb =
    "jdbc:oracle:thin:@slcac706.us.oracle.com:1529:dbjaudit";
  static String m_psrUName = "dm_read_only";
  static String m_psrPwd = "read_only_connection";
  static String emailAddress = "mike.deng@oracle.com,fusion_premerge_support_ww_grp@oracle.com";
  static String avt = null;

  public static void main(String[] args) throws Exception {
    avt = FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX");
    if (args.length > 0) {
      if (args[0].endsWith("@oracle.com")) {
        emailAddress = args[0];
      } else {
        emailAddress = args[0] + "@oracle.com";
      }
      System.out.println("Email address is set to " + emailAddress);
      avt = System.getenv("ADE_VIEW_ROOT"); // read local file
      System.out.println("ADE View Root: " + avt);
    }
    /*
        System.out.println("Updating profile V2..");
        updateProfile("MAIN", "audit_rules_source_v2.txt");
        System.out.println("Updating profile V1.2..");
        updateProfile("V1.2", "audit_rules_source_v12.txt");
        System.out.println("Updating profile Release 8..");
        updateProfile("Release 8", "audit_rules_source_rel8.txt");
        System.out.println("Updating profile Release 9..");
        updateProfile("Release 9", "audit_rules_source_rel9.txt");
        System.out.println("Updating profile Release 10..");
        updateProfile("Release 10", "audit_rules_source_rel10.txt");
        System.out.println("Updating profile Release 11..");
        updateProfile("Release 11", "audit_rules_source_rel11.txt");
        */
    System.out.println("Updating profile Release 12..");
    updateProfile("Release 12", "audit_rules_source_rel12.txt");
    System.out.println("Updating profile Release 13..");
    updateProfile("Release 13", "audit_rules_source_rel13.txt");
    System.out.println("Updating profile Release 14..");
    updateProfile("Release 14", "audit_rules_source_rel14.txt");
    System.out.println("Updating series codeline mapping...");
    updateseriesCodeline();
  }

  private static void updateseriesCodeline() {

    try {
      Connection con = null;
      Statement stmt = null;
      ResultSet rs = null;

      BufferedReader reader =
        new BufferedReader(new FileReader(avt + "/fatools/opensource/jauditFixScripts/txt/seriesCodelines.txt"));
      String line = "";
      HashSet<String> existingSeriesCodelines = new HashSet<String>();

      while ((line = reader.readLine()) != null) {
        String key = line.trim();
        if (!key.startsWith("#"))
          existingSeriesCodelines.add(key);
      }

      HashSet<String> newSeriesCodelines = new HashSet<String>();

      con = DriverManager.getConnection(m_psrDb, m_psrUName, m_psrPwd);
      stmt = con.createStatement();

      String sql =
        "SELECT ade_series,codeline from dm_audit_series where codeline not like 'RUP%'" +
        " and ade_series not like 'FATOOLS%' and ade_series not like '%PLATFORMS%'";

      rs = stmt.executeQuery(sql);

      while (rs.next()) {
        String series =
          rs.getString("ade_series") + ":" + rs.getString("codeline");
        // older codeline needs no more update
        // if (codeline.equals("Release 12") == false && codeline.equals("Release 13") == false)  continue;

        if (existingSeriesCodelines.contains(series))
          continue;
        newSeriesCodelines.add(series);
      }

      rs.close();
      stmt.close();

      if (newSeriesCodelines.size() > 0) {
        String new_mappings = "";
        //for(Iterator<String> it = newSeriesCodelines.keySet().iterator();it.hasNext();) {
        //    String series = it.next();
        for (String series : newSeriesCodelines) {
          new_mappings += series + "\n";
        }
        System.out.println("New series codelines mappings added.");
        System.out.println(new_mappings);
        Mail.sendMail(emailAddress, new_mappings,
                      "New series codelines mappings added.");
      }
    } catch (Exception e) {
      Mail.sendMail(emailAddress, "Script Failed:",
                    "Audit Series Codeline update script failed");
    }
  }

  private static void updateProfile(String codeline, String fileName) {

    try {
      Connection con = null;
      Statement stmt = null;
      ResultSet rs = null;
      FileReader fr = null;
      try {
        if (avt != null) {
          System.out.println("Using local file: " + fileName);
          fr =
              new FileReader(avt + "/fatools/opensource/jauditFixScripts/txt/" +
                             fileName);
        } else {
          fr =
              new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
                             "/fatools/opensource/jauditFixScripts/txt/" +
                             fileName);
        }
      } catch (FileNotFoundException fnf) {
        System.out.println("Cannot load " + fileName);
        System.exit(-1);
      }

      BufferedReader reader = new BufferedReader(fr);
      //  BufferedReader reader = new BufferedReader(new FileReader("/scratch/sudgupta/view_storage/sudgupta_fatools_jan23_view/fatools/opensource/jauditFixScripts/txt/" + fileName));

      String line = "";
      HashSet<String> existing_rules = new HashSet<String>();
      while ((line = reader.readLine()) != null) {
        existing_rules.add(line.trim());
      }

      HashSet<String> new_rules = new HashSet<String>();


      con = DriverManager.getConnection(m_psrDb, m_psrUName, m_psrPwd);
      stmt = con.createStatement();

      // String sql = "SELECT audit_rule_id from dm_audit_rules_in_codelines where codeline = '" + codeline + "' and (rule_category = 'Category 1' or rule_category='Category 2') ";
      String sql =
        "SELECT ac.audit_rule_id, ar.audit_rule_name from dm_audit_rules_in_codelines ac, dm_audit_rules ar where ac.codeline = '" +
        codeline +
        "' and (ac.rule_category = 'Category 1' or ac.rule_category='Category 2') and ac.audit_rule_id=ar.audit_rule_id";

      rs = stmt.executeQuery(sql);

      while (rs.next()) {
        String ruleId = rs.getString("audit_rule_id").trim();
        if ("fre-test-2".equals(ruleId) || "fre-dm-3".equals(ruleId)) continue;  // skip these two test rules
        if (!existing_rules.contains(ruleId))
          new_rules.add(ruleId + " : " +
                        rs.getString("audit_rule_name").trim());
      }

      rs.close();
      stmt.close();

      if (new_rules.size() > 0) {
        String new_rules_added = "";
        //for(Iterator<String> it = new_rules.iterator();it.hasNext();) {
        //    String rule = it.next();
        for (String rule : new_rules)
          new_rules_added += rule + "\n";
        //}
        System.out.println("New Audit rules added to " + codeline);
        System.out.println(new_rules_added);
        Mail.sendMail(emailAddress, new_rules_added,
                      "New Audit rules added to " + codeline +
                      ". Please review and update profile.");
      }

      /* Category 3 are not added to tules txt file
            // sql = "SELECT audit_rule_id from dm_audit_rules_in_codelines where codeline = '" + codeline + "' and rule_category = 'Category 3'";
            sql = "SELECT ac.audit_rule_id, ar.audit_rule_name from dm_audit_rules_in_codelines ac, dm_audit_rules ar where ac.codeline = '"+codeline+"' and ac.rule_category = 'Category 3' and ac.audit_rule_id=ar.audit_rule_id";
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            //new_rules = new HashSet<String>();
            new_rules.clear();

            while(rs.next()) {
                String ruleId = rs.getString("audit_rule_id").trim();
                if(!existing_rules.contains(ruleId))  new_rules.add(ruleId+", "+rs.getString("audit_rule_name").trim());

                // if(existing_rules.contains(ruleId.trim()))  new_rules.add(ruleId);
            }

            rs.close();
            stmt.close();
            con.close();

            if(new_rules.size() > 0) {
                String new_rules_added = "";
                // for(Iterator<String> it = new_rules.iterator();it.hasNext();) {
                //    String rule = it.next();
                for (String rule : new_rules)
                    new_rules_added += rule + "\n";
                // }
                Mail.sendMail(emailAddress, new_rules_added,
                              "Following Cat3 rules present in " + codeline + ". Please review and update profile.");
            }
            */
    } catch (Exception e) {
      e.printStackTrace();
      // Mail.sendMail(emailAddress, "Script Failed:",  "Audit profile update script failed for codeline: " + codeline);
    }
  }
}
