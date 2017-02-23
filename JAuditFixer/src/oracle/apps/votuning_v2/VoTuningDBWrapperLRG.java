package oracle.apps.votuning_v2;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;

public class VoTuningDBWrapperLRG {
    String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
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

        VoTuningDBWrapperLRG dbWrapper = new VoTuningDBWrapperLRG();

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
            String fileName = pwd + "/TableFetchSizePSL_LRG.csv";

            BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
            String line = fileReader.readLine();

            con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
            con.setAutoCommit(false);
            String tableName = "CODESCAN_RESULTS";

            CallableStatement cstmt =
                con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
            String tableName2 = "Table_FetchSize";

            System.out.println("First delete records from DB...");
            Statement stmt = con.createStatement();
            String sql = "";

            sql = "DELETE FROM " + tableName + " WHERE ISSUETYPE = 'TableFetchSizePSL' and series='" + series + "'";

            stmt.executeUpdate(sql);
            stmt.close();

            if (line == null) {
                con.commit();
                return;
            }

            cstmt.registerOutParameter(1, Types.INTEGER);

            PreparedStatement pstmt =
                con.prepareStatement("INSERT INTO " + tableName2 +
                                     "(SEQ_ID, COMPONENT, COMPONENT_ID, AUTOHEIGHT_ROWS," +
                                     "ITERATOR_RANGE_SIZE, IRS_NEW, ITERATOR_NAME, LIST_RANGE_SIZE, LRS_NEW, VO_USAGE_FETCH_SIZE, VO_USAGE_FETCH_SIZE_NEW, " +
                                     "VO_DEFAULT_FETCH_SIZE, VO_DEFAULT_FETCH_SIZE_NEW, AMPACKAGE, VOPACKAGE, VOINSTANCE_NAME) " +
                                     "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            System.out.println("Now inserting values...");
            int count = 0;
            while ((line = fileReader.readLine()) != null) {

                String[] parts = line.split(",");
                if (parts.length < 24) {
                    System.out.println("Skipping: " + line);
                    continue;
                }

                if (LRGUtil.isDeferred(con, series, parts[3].trim()))
                    continue;
                
                cstmt.setString(2, parts[0].trim()); //family
                cstmt.setString(3, parts[1].trim()); //module
                cstmt.setString(4, parts[2].trim()); //product
                cstmt.setString(5, parts[3].trim()); //filename
                cstmt.setString(9, parts[4].trim()); //series
                cstmt.setString(10, parts[5].trim()); //label
                cstmt.setString(6, "TableFetchSizePSL"); //issue
                cstmt.setString(7, "Table Wrapped in Panel Stretch Layout"); //subissue
                String description = getDescription(parts[7], parts[21]); //based on fix statuses
                cstmt.setString(8, description);
                cstmt.execute();

                int seq = cstmt.getInt(1);
                pstmt.setInt(1, seq); //SEQ_ID
                pstmt.setString(2, parts[6]); //component name
                pstmt.setString(3, parts[7]); //component id
                pstmt.setString(4, parts[8]); //autoheight rows
                pstmt.setString(5, parts[9]); //IRS
                pstmt.setString(6, parts[10]); //IRS new
                pstmt.setString(7, parts[11]); //Iterator Name
                pstmt.setString(8, parts[12]); //LRS
                pstmt.setString(9, parts[13]); //LRS New
                pstmt.setString(10, parts[14]); //VO Usage Fetch Size
                pstmt.setString(11, parts[15]); //VO Usage Fetch Size new
                pstmt.setString(12, parts[16]); //Vo Default Fetch Size
                pstmt.setString(13, parts[17]); //Vo Default Fetch Size New
                pstmt.setString(14, parts[18]); //am package
                pstmt.setString(15, parts[19]); //vo package
                pstmt.setString(16, parts[20]); //vo instance

                pstmt.execute();
                count++;
            }
            con.commit();
            cstmt.close();
            pstmt.close();
            System.out.println("There are " + count + " violations for VO tuning v2 Scans on " + series + "\n\n");
        } catch (Exception e) {
            e.printStackTrace();
            if (con != null)
                con.rollback();
        } finally {
            if (con != null)
                con.close();
        }
    }

    public static String getDescription(String compId, String fixStatuses) {

        String result = "";
        String[] parts = fixStatuses.trim().split(" ");

        for (int i = 0; i < parts.length; i++) {
            if (parts[i].contains("LRS_FIX"))
                result += ", Fix List RangeSize";
            else if (parts[i].contains("ITERATOR_FIX"))
                result += ", Fix Iterator RangeSize";
            else if (parts[i].contains("VA_FETCHSIZE_FIX"))
                result += ", Fix ViewAccessor ";
            else if (parts[i].contains("AM_VO_FETCH"))
                result += ", Fix AM VO Usage Fetch Size";
            else if (parts[i].contains("VOFETCH_DEFAULT"))
                result += ", Fix Default VO Fetch Size";
        }
        result = result.replaceFirst(", ", "");

        return compId + ":" + result;
    }
}
