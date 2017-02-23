package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.serialization.SerializationDBWrapper;
import oracle.apps.utility.LRGUtil;

public class UnusedTaskFlowsDBWrapperLRG {

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

        UnusedTaskFlowsDBWrapperLRG dbWrapper =
            new UnusedTaskFlowsDBWrapperLRG();

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
            String fileName = pwd + "/UnusedTaskFlow_LRG.csv";

            BufferedReader fileReader =
                new BufferedReader(new FileReader(fileName));
            String line = fileReader.readLine();

            con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
            con.setAutoCommit(false);
            String tableName = "";

            CallableStatement cstmt =
                con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");

            System.out.println("First delete records from DB...");
            Statement stmt = con.createStatement();
            String sql =
                "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'Unused TaskFlow' and SERIES ='" +
                series + "'";

            stmt.executeUpdate(sql);
            // con.commit();
            stmt.close();

            if (line == null) {
                con.commit();
                return;
            }

            cstmt.registerOutParameter(1, Types.INTEGER);

            System.out.println("Now inserting values...");
            String[] labelSeries = null;
            String prevLabel = "";
            int count = 0;
            while ((line = fileReader.readLine()) != null) {

                String[] parts = line.split(",");
                if (parts.length < 14)
                    continue;

                String isUsed = parts[13].trim();
                if (isUsed.toLowerCase().equals("true"))
                    continue;

                if (LRGUtil.isDeferred(con, series, parts[3].trim()))
                    continue;

                if (prevLabel != parts[4].trim()) {
                    labelSeries =
                            FamilyModuleHelper.getLabelSeries(parts[4].trim());
                    prevLabel = parts[4].trim();
                }

                cstmt.setString(2, parts[0].trim()); //family
                cstmt.setString(3, parts[1].trim()); //module
                cstmt.setString(4, parts[2].trim()); //product
                cstmt.setString(5, parts[3].trim()); //filename
                cstmt.setString(6, "Unused TaskFlow"); //issue
                cstmt.setString(7,
                                parts[7].trim()); //subissue - TaskFlowID
                cstmt.setString(8,
                                "TaskFlow " + parts[7].trim() + " is not used in " +
                                parts[5].trim()); //description
                cstmt.setString(9, labelSeries[0]); //series
                cstmt.setString(10, labelSeries[1]); //label
                cstmt.execute();
                count++;
            }
            con.commit();
            cstmt.close();
            System.out.println("Total count for Unused TaskFlow is " + count +
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
