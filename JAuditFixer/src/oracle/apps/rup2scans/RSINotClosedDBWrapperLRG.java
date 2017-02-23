package oracle.apps.rup2scans;

import java.io.BufferedReader;

import java.io.FileReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;

public class RSINotClosedDBWrapperLRG {
    String statsDB =
        "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
    static String statsUname = "codescan";
    static String statsPwd = "codescan";
    static String pwd;
    static String series = "";

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Need to specify both 1.directory and 2.series");
            System.exit(1);
        }

        pwd = args[0];
        series = args[1];

        RSINotClosedDBWrapperLRG dbWrapper = new RSINotClosedDBWrapperLRG();

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
            String fileName1 = pwd + "/RowSetIteratorNotClosed_LRG.csv";

            BufferedReader fileReader =
                new BufferedReader(new FileReader(fileName1));
            String line = "";

            con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
            con.setAutoCommit(false);


            System.out.println("First delete records from DB...");
            Statement stmt = con.createStatement();
            String sql =
                "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'RowSetIteratorNotClosed' and SERIES='" +
                series + "'";

            stmt.executeUpdate(sql);
            stmt.close();


            PreparedStatement pstmt =
                con.prepareStatement("INSERT INTO CODESCAN_RESULTS(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,SERIES,LABEL) " +
                                     "VALUES(?,?,?,?,?,?,?,?,?)");

            System.out.println("Now inserting values...");
            String[] labelSeries = null;
            String prevLabel = "";
            int count = 0;

            while ((line = fileReader.readLine()) != null) {

                //"Family,Module,Product,FileName,Label,MethodName,MethodStartLineNo, RSI Variable Not Closed,Iterator Name\n");
                String[] parts = line.split(",");
                if (parts.length < 9) {
                    System.out.println("Skipping: " + line);
                    continue;
                }
                String iteratorName = parts[8].trim();
                if (!iteratorName.equals("null"))
                    continue;

                if (LRGUtil.isDeferred(con, series, parts[3].trim()))
                    continue;
                if (prevLabel != parts[4].trim()) {
                    labelSeries =
                            FamilyModuleHelper.getLabelSeries(parts[4].trim());
                    prevLabel = parts[4].trim();

                }
                pstmt.setString(1, parts[0].trim()); //family
                pstmt.setString(2, parts[1].trim()); //module
                pstmt.setString(3, parts[2].trim()); //product
                pstmt.setString(4, parts[3].trim()); //filename
                pstmt.setString(5, "RowSetIteratorNotClosed"); //issue
                pstmt.setString(6,
                                "Method: " + parts[5].trim()); //subissue   -> method Name
                pstmt.setString(7,
                                parts[7].trim() + " is not closed"); //Description  -> RSI variable
                pstmt.setString(8, labelSeries[0]); //series
                pstmt.setString(9, labelSeries[1]); //label
                pstmt.execute();
                count++;
            }
            con.commit();
            pstmt.close();
            System.out.println("Total count for RowSetIteratorNotClosed is " +
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

}
