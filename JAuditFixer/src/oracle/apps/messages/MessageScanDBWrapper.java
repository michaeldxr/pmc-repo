package oracle.apps.messages;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.Map;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;


public class MessageScanDBWrapper {
  static String statsDB =
    "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
  static String statsUname = "codescan";
  static String statsPwd = "codescan";
  static String series;
  static String sLabel;
  static int default_release = 13;

  public static void main(String[] args) throws Exception {
    if (args.length != 1 || args[0] == null || args[0].isEmpty()) {
      System.out.println("USAGE: runUnusedPartialTrigger <series>");
      System.exit(1);
    }

    series = args[0];
    sLabel = LRGUtil.getLatestLabel(series);
    String release = FamilyModuleHelper.getRelease(series);
    try {
      default_release = Integer.parseInt(release);
    } catch (NumberFormatException e) {
      default_release = 13;
    }
    MessageScanDBWrapper dbWrapper = new MessageScanDBWrapper();

    try {
      System.out.println("Updating DB....");
      dbWrapper.updateDatabase();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public Map<String, ArrayList<String>> getExemptions(String issue) throws Exception {
    Connection con = null;
    Map<String, ArrayList<String>> exemptions =
      new HashMap<String, ArrayList<String>>();
    try {
      con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
      con.setAutoCommit(false);
      String sql =
        "select * from msg_scan_exemptions where issue ='" + issue +
        "' and (SERIES = 'ALL' or SERIES ='" + series + "')" + " and (release = 'ALL' or release ='" + default_release + "')";
      PreparedStatement ps = con.prepareStatement(sql);
      ResultSet rs = ps.executeQuery();
      ArrayList<String> data;
      while (rs.next()) {
        data = new ArrayList<String>();
        String sSeries = rs.getString("SERIES");
        String filename = rs.getString("FILENAME");
        String message = rs.getString("MESSAGE");
        String bug = rs.getString("BUG");
        String key = message + ":" + filename;
        data.add(sSeries);
        data.add(issue);
        data.add(bug);
        exemptions.put(key, data);
      }
    } catch (Exception e) {
      e.printStackTrace();
      if (con != null)
        con.rollback();
    } finally {
      if (con != null)
        con.close();
    }
    con.close();
    return exemptions;
  }

  public void updateDatabase() throws Exception {
    updateMessageNotInMRTScanResults();
    updateMRTMessageReviewScanResults();
    updateUnusedMessageScanResults();
    updateUndefinedMessageUseageScanResults();
  }

  public void updateMessageNotInMRTScanResults() throws Exception {

    Connection con = null;
    Map<String, ArrayList<String>> exemptions =
      getExemptions("Message not valid in MRT");
    ArrayList<String> data;
    try {
      String fileName = "message_not_in_mrt.csv";

      BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
      String line = fileReader.readLine();
      con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
      con.setAutoCommit(false);
      CallableStatement cstmt =
        con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");

      System.out.println("First delete records from DB...");
      Statement stmt = con.createStatement();
      String sql =
        "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'Message not valid in MRT' and SERIES ='" +
        series + "'";

      stmt.executeUpdate(sql);
      stmt.close();


      if (line == null) {
        con.commit();
        return;
      }

      cstmt.registerOutParameter(1, Types.INTEGER);

      PreparedStatement pstmt =
        con.prepareStatement("INSERT INTO MSG_NOT_IN_MRT_SCAN(SEQ_ID, MESSAGE_NAME,MSG_STATUS, IS_OBSOLETE," +
                             "V_FROM, V_TO, DEV_MGR, PRODUCT_MGR, INFODEV_MGR) " +
                             "VALUES(?,?,?,?,?,?,?,?,?)");
      System.out.println("Now inserting message not in mrt scan results...");
      String[] labelSeries = null;
      String prevLabel = "";
      int count = 0;
      while ((line = fileReader.readLine()) != null) {

        String[] parts = line.split(",");
        if (parts.length < 15)
          continue;
        String message = parts[4];
        String sSeries = parts[0].trim();
        String filename = parts[5].trim();
        if (filename.equals("-")) {
          filename = null;
        } else {
          filename = filename.substring(filename.indexOf("fusionapps"));
        }
        
          String key = message + ":" + filename;
          if (exemptions.containsKey(key)) {
            data = exemptions.get(key);
            if ((data.get(0).equals("ALL") || data.get(0).equals(sSeries)) && key.contains(filename)) {
              continue;
            }
          }

        cstmt.setString(2, parts[2].trim()); //family
        cstmt.setString(3, ""); //module
        cstmt.setString(4, parts[3].trim()); //product
        cstmt.setString(5, filename); //filename
        cstmt.setString(9, sSeries); //series
        cstmt.setString(10, parts[1].trim()); //label
        cstmt.setString(6, "Message not valid in MRT"); //issue
        cstmt.setString(7, ""); //subissue
        cstmt.setString(8, parts[7].trim());
        cstmt.execute();

        int seq = cstmt.getInt(1);
        pstmt.setInt(1, seq);
        pstmt.setString(2, parts[4]); //message_name
        pstmt.setString(3, parts[8]); //message_status
        pstmt.setString(4, parts[9]); //is_obsolete
        pstmt.setString(5, parts[10]); //version from
        pstmt.setString(6, parts[11]); //version to
        pstmt.setString(7, parts[12]); //dev manager
        pstmt.setString(8, parts[13]); //product manager
        pstmt.setString(9, parts[14]); //infodev manager

        pstmt.execute();
        count++;
      }
      con.commit();
      cstmt.close();
      pstmt.close();
      System.out.println("Total count for Message not in MRT scan is " +
                         count + " on series " + series);
    } catch (Exception e) {
      e.printStackTrace();
      if (con != null)
        con.rollback();
    } finally {
      if (con != null)
        con.close();
    }
  }

  public void updateUndefinedMessageUseageScanResults() throws Exception {

    Connection con = null;
    Map<String, ArrayList<String>> exemptions =
          getExemptions("Undefined Messages");
    ArrayList<String> data;
    try {
      String fileName = "undefined_message_usages.csv";
      
      BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
      String line = fileReader.readLine();
      con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
      con.setAutoCommit(false);
      CallableStatement cstmt =
        con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");

      System.out.println("First delete records from DB...");
      Statement stmt = con.createStatement();
      String sql =
        "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'Undefined Messages' and SERIES ='" +
        series + "'";

      stmt.executeUpdate(sql);
      stmt.close();


      if (line == null) {
        con.commit();
        return;
      }

      cstmt.registerOutParameter(1, Types.INTEGER);

      PreparedStatement pstmt =
        con.prepareStatement("INSERT INTO UNDEFINED_MSG_USAGE_SCAN(SEQ_ID, MESSAGE_NAME,PRESENT_IN_XLF, PRESENT_IN_MRT," +
                             "MSG_STATUS,IS_OBSOLETE,V_FROM, V_TO, DEV_MGR, PRODUCT_MGR, INFODEV_MGR, CREATION_DATE, LAST_UPDATE_DATE, LAST_EXPORT_DATE, PREVIOUSLY_APPROVED) " +
                             "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
      System.out.println("Now inserting undefined message usage scan results...");
      int count = 0;
      while ((line = fileReader.readLine()) != null) {

        String[] parts = line.split(",");
        if (parts.length < 21)
          continue;
        String message = parts[4];
        String sSeries = parts[0].trim();
        String filename = parts[5].trim();
        if (filename.equals("-")) {
          filename = null;
        } else {
          filename = filename.substring(filename.indexOf("fusionapps"));
        }
        
          String key = message + ":" + filename;
          if (exemptions.containsKey(key)) {
            data = exemptions.get(key);
            if (data.get(0).equals(sSeries) && key.contains(filename)) {
              continue;
            }
          }
        
        cstmt.setString(2, parts[2].trim()); //family
        cstmt.setString(3, ""); //module
        cstmt.setString(4, parts[3].trim()); //product
        cstmt.setString(5, filename); //filename
        cstmt.setString(9, parts[0].trim()); //series
        cstmt.setString(10, parts[1].trim()); //label
        cstmt.setString(6, "Undefined Messages"); //issue
        cstmt.setString(7, ""); //subissue
        cstmt.setString(8, parts[7].trim());
        cstmt.execute();

        int seq = cstmt.getInt(1);
        pstmt.setInt(1, seq);
        pstmt.setString(2, parts[4]); //message_name
        pstmt.setString(3, parts[8]); //present in xlf
        pstmt.setString(4, parts[9]); //present in mrt
        pstmt.setString(5, parts[10]); //msg status
        pstmt.setString(6, parts[11]); //is obsolete
        pstmt.setString(7, parts[12]); //v from
        pstmt.setString(8, parts[13]); //v to
        pstmt.setString(9, parts[14]); //dev manager
        pstmt.setString(10, parts[15]); //product manager
        pstmt.setString(11, parts[16]); //infodev manager
        pstmt.setString(12, parts[17]); //creation date
        pstmt.setString(13, parts[18]); //last update date
        pstmt.setString(14, parts[19]); //last export date
        pstmt.setString(15, parts[20]); //previously approved

        pstmt.execute();
        count++;
      }
      con.commit();
      cstmt.close();
      pstmt.close();
      System.out.println("Total count for Undefined scan is " + count +
                         " on series " + series);
    } catch (Exception e) {
      e.printStackTrace();
      if (con != null)
        con.rollback();
    } finally {
      if (con != null)
        con.close();
    }
  }

//Not yet mandated. Publish to prototype area only.
  public void updateMRTMessageReviewScanResults() throws Exception {

    Connection con = null;
    try {
      String fileName = "mrt_message_review.csv";

      BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
      String line = fileReader.readLine();
      con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
      con.setAutoCommit(false);
      CallableStatement cstmt =
        con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT_P(?,?,?,?,?,?,?,?,?) }");

      System.out.println("First delete records from DB...");
      Statement stmt = con.createStatement();
      String sql =
        "DELETE FROM CODESCAN_RESULTS_P WHERE ISSUETYPE ='MRT/ADE Mismatch' and SERIES ='" +
        series + "'";

      stmt.executeUpdate(sql);
      stmt.close();


      if (line == null) {
        con.commit();
        return;
      }

      cstmt.registerOutParameter(1, Types.INTEGER);

      PreparedStatement pstmt =
        con.prepareStatement("INSERT INTO MRT_MSG_REVIEW_SCAN_P(SEQ_ID, MESSAGE_NAME,MSG_IN_SD_OR_XLF, IS_MSG_TXT_SAME," +
                             "IS_MSG_UD_SAME,IS_MSG_CAUSE_SAME,IS_MSG_ACTION_SAME, IS_MSG_ADMIN_DETAILS_SAME,IS_MSG_ADMIN_ACTION_SAME,IS_MSG_CONTEXT_SAME,V_FROM, V_TO,IS_OBSOLETE, DEV_MGR, PRODUCT_MGR, INFODEV_MGR, CREATION_DATE, LAST_UPDATE_DATE, LAST_EXPORT_DATE) " +
                             "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
      System.out.println("Now inserting mrt message review scan results...");
      String[] labelSeries = null;
      String prevLabel = "";
      int count = 0;
      while ((line = fileReader.readLine()) != null) {

        String[] parts = line.split(",");
        if (parts.length < 26)
          continue;
        cstmt.setString(2, parts[2].trim()); //family
        cstmt.setString(3, ""); //module
        cstmt.setString(4, parts[3].trim()); //product
        String filename = parts[5].trim();
        if (filename.equals("-")) {
          filename = null;
        } else {
          filename = filename.substring(filename.indexOf("fusionapps"));
        }
        cstmt.setString(5, filename); //filename
        cstmt.setString(9, parts[0].trim()); //series
        cstmt.setString(10, parts[1].trim()); //label
        cstmt.setString(6, "MRT/ADE Mismatch"); //issue
        cstmt.setString(7, parts[7].trim()); //subissue
        cstmt.setString(8, parts[8].trim());
        cstmt.execute();

        int seq = cstmt.getInt(1);
        pstmt.setInt(1, seq);
        pstmt.setString(2, parts[4]); //message_name
        pstmt.setString(3, parts[9]); //present in xlf/mrt
        pstmt.setString(4, parts[10]); //message text same
        pstmt.setString(5, parts[11]); //msg user details same
        pstmt.setString(6, parts[12]); //msg cause same
        pstmt.setString(7, parts[13]); //msg action same
        pstmt.setString(8, parts[14]); //msg admin details same
        pstmt.setString(9, parts[15]); //msg admin action same
        pstmt.setString(10, parts[16]); //msg context same
        pstmt.setString(11, parts[17]); //v from
        pstmt.setString(12, parts[18]); //v to
        pstmt.setString(13, parts[19]); //isobsolete
        pstmt.setString(14, parts[20]); //dev mgr
        pstmt.setString(15, parts[21]); //product mgr
        pstmt.setString(16, parts[22]); //infodev manager
        pstmt.setString(17, parts[23]); //creation date
        pstmt.setString(18, parts[24]); //last update date
        pstmt.setString(19, parts[25]); //last export date

        pstmt.execute();
        count++;
      }
      con.commit();
      cstmt.close();
      pstmt.close();
      System.out.println("Total count for MRT Message Review scan is " +
                         count + " on series " + series);
    } catch (Exception e) {
      e.printStackTrace();
      if (con != null)
        con.rollback();
    } finally {
      if (con != null)
        con.close();
    }
  }

//Not yet mandated. Publish to prototype area only.
  
  private boolean isUnusedMessageScanExempted(String[] parts){
    String product = parts[3].trim();
    /*bug 24426996: PJG is an "Integration Gateway" and the only supported integration is with Primavera P6. Messages in this producr are triggered from Primavera code and 
     * reported on output reports on the Fusion side as part of the integration processing. Not possible to scan Primavera code, so granting exemptions */
    if(product.equalsIgnoreCase("PJG")){
      return true;
    }
    /* bug 24467868.*/
    String message = parts[4].toUpperCase();
    if(message.contains("IBY_AMT_IN_WORD_")){
      return true;
    }
    
    return false;
  }
  public void updateUnusedMessageScanResults() throws Exception {

    Connection con = null;
    Map<String, ArrayList<String>> exemptions =
          getExemptions("Unused Messages");
           ArrayList<String> data; 
    try {
      String fileName = "unused_messages.csv";
      
      BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
      String line = fileReader.readLine();
      con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
      con.setAutoCommit(false);
      CallableStatement cstmt =
        con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT_P(?,?,?,?,?,?,?,?,?) }");

      System.out.println("First delete records from DB...");
      Statement stmt = con.createStatement();
      String sql =
        "DELETE FROM CODESCAN_RESULTS_P WHERE ISSUETYPE = 'Unused Messages' and SERIES ='" +
        series + "'";

      stmt.executeUpdate(sql);
      stmt.close();


      if (line == null) {
        con.commit();
        return;
      }

      cstmt.registerOutParameter(1, Types.INTEGER);

      PreparedStatement pstmt =
        con.prepareStatement("INSERT INTO UNUSED_MSG_SCAN_P(SEQ_ID, MESSAGE_NAME,PRESENT_IN_MRT,MSG_STATUS,IS_OBSOLETE,V_FROM,V_TO,DEV_MGR,PRODUCT_MGR,INFODEV_MGR,CREATION_DATE,LAST_UPDATE_DATE,LAST_EXPORT_DATE,PREVIOUSLY_APPROVED)" +
                             "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
      System.out.println("Now inserting unused message scan results...");
      String[] labelSeries = null;
      String prevLabel = "";
      int count = 0;
      while ((line = fileReader.readLine()) != null) {

        String[] parts = line.split(",");
        if (parts.length < 10)
          continue;
        String message = parts[4];
        String sSeries = parts[0].trim();
        String filename = parts[5].trim();
        if (filename.equals("-")) {
          filename = null;
        } else {
          filename = filename.substring(filename.indexOf("fusionapps"));
        }
        
          String key = message + ":" + filename;
          if (exemptions.containsKey(key)) {
            data = exemptions.get(key);
            if (data.get(0).equals(sSeries) && key.contains(filename)) {
              continue;
            }
          }
        
        if(!isUnusedMessageScanExempted(parts)){ 
        cstmt.setString(2, parts[2].trim()); //family
        cstmt.setString(3, ""); //module
        cstmt.setString(4, parts[3].trim()); //product
        cstmt.setString(5, filename); //filename
        cstmt.setString(9, parts[0].trim()); //series
        cstmt.setString(10, parts[1].trim()); //label
        cstmt.setString(6, "Unused Messages"); //issue
        cstmt.setString(7, ""); //subissue
        cstmt.setString(8, parts[7].trim());
        cstmt.execute();

        int seq = cstmt.getInt(1);
        pstmt.setInt(1, seq);
        pstmt.setString(2, parts[4]); //message_name
        pstmt.setString(3, parts[8]); //present in mrt
        pstmt.setString(4, parts[9]); //msg status
        pstmt.setString(5, parts[10]); //is obsolete
        pstmt.setString(6, parts[11]); //v from
        pstmt.setString(7, parts[12]); //v to
        pstmt.setString(8, parts[13]); //dev manager
        pstmt.setString(9, parts[14]); //product manager
        pstmt.setString(10, parts[15]); //infodev manager
        pstmt.setString(11, parts[16]); //creation date
        pstmt.setString(12, parts[17]); //last update date
        pstmt.setString(13, parts[18]); //last export date
        pstmt.setString(14, parts[19]); //previously approved
        pstmt.execute();
        count++;
         }
      }
      con.commit();
      cstmt.close();
      pstmt.close();
      System.out.println("Total count for Unused message scan is " + count +
                         " on series " + series);
    } catch (Exception e) {
      e.printStackTrace();
      if (con != null)
        con.rollback();
    } finally {
      if (con != null)
        con.close();
    }
  }
}
