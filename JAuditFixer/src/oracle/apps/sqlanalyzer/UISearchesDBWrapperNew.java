package oracle.apps.sqlanalyzer;

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

public class UISearchesDBWrapperNew {

    String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
    static String statsUname = "codescan";
    static String statsPwd = "codescan";

    static String pwd;
    static String series = "";
    static int releaseNum = 0;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Need to specify both 1.directory and 2.series");
            System.exit(1);
        }

        pwd = args[0];
        series = args[1];
        
        String release= FamilyModuleHelper.getRelease(series);
        
        
        try{              
            releaseNum = Integer.parseInt(release);         
        }catch(NumberFormatException e){
            System.out.println("Could not determine release. Defaulting to 0.");
            releaseNum=0;
        }
        
        System.out.println("Release#: " + releaseNum);

        UISearchesDBWrapperNew dbWrapper = new UISearchesDBWrapperNew();

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
            String fileName = pwd + "/UI_Searches1.csv";

            BufferedReader fileReader =
                new BufferedReader(new FileReader(fileName));
            String line = fileReader.readLine();

            con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
            con.setAutoCommit(false);
            CallableStatement cstmt1 = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");

            System.out.println("First delete records from DB...");
            Statement stmt = con.createStatement();
            String sql1 = "";

            sql1 = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE like 'UISearches%' and series='" + series+"'";

            stmt.executeUpdate(sql1);
            stmt.close();

            if (line == null) {
                con.commit();
                return;
            }

            cstmt1.registerOutParameter(1, Types.INTEGER);

            PreparedStatement pstmt1 =
                con.prepareStatement("INSERT INTO UI_SEARCHES_CASE(SEQ_ID, COMPONENT, " +
                                     "MODEL_VALUE, VIEW_FILE, VIEW_CRITERIA, VIEWATTRIBUTE,TABLE_NAME," +
                                     "COLUMN_NAME, RESOLVED_TABLE_COLUMN, COL_TYPE, COLUMN_INDEX, VC_REQUIRED," +
                                     "COLUMN_POSITION_IN_INDEX, CASE_SENSITIVE, CATEGORY, ORIG_VO_LOVUses) " +
                                     "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            System.out.println("Now inserting values...");

            String prevLabel = "";
            String[] labelSeries = null;

            String issuetype = "";
            String subissue = "";
            int count = 0;
            while ((line = fileReader.readLine()) != null) {

                String[] parts = line.split(",");
                if (parts.length < 21)
                    continue;

                if (parts[12].length() >= 100)
                    continue;

                if(LRGUtil.isDeferred(con, series, parts[3].trim()))
                    continue;
                
//                if (parts[11].trim().contains("Could not determine"))
//                    continue;

                if (!prevLabel.equals(parts[4].trim())) {
                    labelSeries =
                            FamilyModuleHelper.getLabelSeries(parts[4].trim());
                    prevLabel = parts[4].trim();
                }

                if (parts[11].trim().contains("Could not determine")) {
                    issuetype = "UISearches_new";
                    subissue = "ManualReview";
                }
                else if (parts[19].trim().startsWith("Simply")) {
                    if(parts[5].trim().equals("query panel LOV"))
                        issuetype = "UISearches1_QP";
                    else issuetype = "UISearches1";
                    subissue = "SimpleFix";
                } 
                else {
                    if(parts[5].trim().equals("query panel LOV"))
                        issuetype = "UISearches2_QP";
                    else
                        issuetype="UISearches2";
                        subissue = "ManualReview";
                }
//                if(parts[20].trim().equals("exempted")){
//                    issuetype = "UISearches3";
//                    subissue = "Placeholder Text";
//                }
//                
                String product = parts[2].trim();
                String family = parts[0].trim();
                if (parts[7].trim().contains("/financials/tax/")) {
                    product = "ZX";
                    family = "FIN";
                }
    
                cstmt1.setString(2, family); //family
                cstmt1.setString(3, parts[1].trim()); //module
                cstmt1.setString(4, product); //product
                cstmt1.setString(5, parts[3].trim()); //filename
                cstmt1.setString(9, labelSeries[0]); //series
                cstmt1.setString(10, labelSeries[1]); //label
                cstmt1.setString(7, subissue); //subissue
                cstmt1.setString(6, issuetype); //issue
                cstmt1.setString(8, parts[9].trim()); //description
                cstmt1.execute();

                int seq = cstmt1.getInt(1);

                pstmt1.setInt(1, seq);
                pstmt1.setString(2, parts[5]); //component
                pstmt1.setString(3, parts[6]); //model
                pstmt1.setString(4, parts[7]); //view file
                pstmt1.setString(5, parts[8]); //view criteria
                pstmt1.setString(6, parts[10]); //view attribute
                pstmt1.setString(7, parts[11]); //table name

                if(parts.length > 19){
                    pstmt1.setString(8, parts[12]); //col name
                    pstmt1.setString(9, parts[13]); //resolved table col
                    pstmt1.setString(10, parts[14]); //col type
                    pstmt1.setString(11, parts[15]); //col index
                    pstmt1.setString(12, parts[16]); //vc_required
                    pstmt1.setString(13, parts[17]); //col position in index
                    pstmt1.setString(14, parts[18]); //case sensitive
                    pstmt1.setString(15, parts[19]); //category
                }
                if(parts.length > 20)
                    pstmt1.setString(16, parts[20]); //OrigLOVVO

                pstmt1.execute();

                count++;
            }
            con.commit();
            cstmt1.close();
            pstmt1.close();
           
            System.out.println("There are " + count + " violations for UI Seaches Scan on " + series + "\n");
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
