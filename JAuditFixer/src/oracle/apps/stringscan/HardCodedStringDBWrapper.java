package oracle.apps.stringscan;


import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;


public class HardCodedStringDBWrapper {
  private static final Matcher FILENAME_MATCHER =
    Pattern.compile("fusionapps/(\\w+)/components/(\\w+)/(\\w+)/.+").matcher("");
  String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
  static String statsUname = "codescan";
  static String statsPwd = "codescan";
  static String outputFile;
  static String issuetype;
  static String series = "";

  public HardCodedStringDBWrapper() {
    super();
  }


  public static void main(String[] args) {


    if (args.length < 3) {
      System.out.println("Need to specify both 1.directory and 2.series");
      System.exit(1);
    }

    outputFile = args[0];
    series = args[1];
    issuetype = args[2];

    HardCodedStringDBWrapper dbWrapper = new HardCodedStringDBWrapper();

    try {
      System.out.println("Updating DB....");
      dbWrapper.updateDatabase();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public void updateDatabase() throws Exception {

    Connection con = null;
    try {
      String fileName = outputFile;

      BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
      String line = fileReader.readLine();

      con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
      con.setAutoCommit(false);
      String tableName = "CODESCAN_RESULTS";

      CallableStatement cstmt =
        con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");

      System.out.println("First delete records from DB...");
      Statement stmt = con.createStatement();
      String sql =
        "DELETE FROM " + tableName + " WHERE ISSUETYPE = '" + issuetype +
        "' and SERIES ='" + series +   "'";
      System.out.println("Delete SQL:" + sql);

      stmt.executeUpdate(sql);
      con.commit();
      stmt.close();

      cstmt.registerOutParameter(1, Types.INTEGER);

      System.out.println("Now inserting values...");
      
      int count = 0;
      while ((line = fileReader.readLine()) != null) {
        if ("".equals(line)||line.startsWith("#"))
          continue;

        String[] parts = line.split(",");

        if (parts.length < 10) {
          System.out.println("line:" + line);
          continue;
        }

        String family_csv = parts[0];
        String product_csv = parts[1];
        String componentType = parts[3];
        String property = parts[4];
        String id = parts[5];

        //Process the rest:
        String rest710 = this.concatArray(parts, 6, (parts.length - 1));
        String[] splitPattern = rest710.trim().split(",HARDCODED,", 2);
        if (splitPattern == null || splitPattern.length < 2) {
          continue;
        }

        String propertyValue = splitPattern[0].trim();
        String rest910 = splitPattern[1].trim();

        String[] splitFileName = rest910.split(",", 2);
        if (splitFileName == null || splitFileName.length < 2) {
          continue;
        }
        String fileFullPath = splitFileName[0];
        String hardcodedValue = splitFileName[1];

        String[] fileparts = fileFullPath.split("/fusionapps/", 2);
        if (fileparts.length < 2) {
          continue;
        }
        //should be fusionapps/...../xxx.jsff
        String filename = "fusionapps/" + fileparts[1];

        String[] labelparts = fileparts[0].split(series, 2);
        if (labelparts.length < 2) {
          continue;
        }

        //should be FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/131030.XXXX
        String label = labelparts[1].split("rdd/", 2)[1].replaceAll("/", "");

        FILENAME_MATCHER.reset(filename);
        if (!FILENAME_MATCHER.matches()) {
          continue;
        }
        String family = FILENAME_MATCHER.group(1);
        String module = FILENAME_MATCHER.group(2);
        String product = FILENAME_MATCHER.group(3);

        cstmt.setString(2, family_csv.toUpperCase()); //family
        cstmt.setString(3, module); //module - parse from file
        cstmt.setString(4,
                        product_csv.toUpperCase()); //product - parse from file
        cstmt.setString(5, filename); //filename
        cstmt.setString(6, issuetype); //issue
        cstmt.setString(7, "id=" + id.trim()); //subissue - id
        cstmt.setString(8,
                        "Hardcoded English String: " + hardcodedValue + " found in " +
                        componentType + ", with property=" + property +
                        " and id=" + id); //description
        cstmt.setString(9, series); //series
        cstmt.setString(10, label); //label
        cstmt.execute();
        count++;
      }
      System.out.println();
      con.commit();
      cstmt.close();
      System.out.println("Total count for Hardcoded Strings " + count +
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

  private String concatArray(String[] array, int i, int j) {
    int min = i;
    int max = j;
    if (i == j)
      return array[i];
    if (i > j) {
      min = j;
      max = i;
    }
    String results = "";
    for (int m = min; m <= max; m++) {
      if(m==max)
        results += array[m];
      else
        results += array[m] + ",";
    }
    return results;
  }

}
