package oracle.apps.utility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import oracle.apps.helpers.FamilyModuleHelper;

public class UpdateLabelServers {
   
    private  final static String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
    private final static String statsUname = "codescan";
    private final static String statsPwd = "codescan"; 
    
    public static void main(String[] args) throws Exception{
        
        Connection con= DriverManager.getConnection(statsDB,statsUname,statsPwd);
        Statement stmt = con.createStatement();
        String sql = "SELECT series from BRANCH_FAMILY_CODELINE";
        ResultSet rs = stmt.executeQuery(sql);
        String series="";
        String labelServer="";
        String releaseNum="";
        PreparedStatement ps1 = con.prepareStatement("UPDATE BRANCH_FAMILY_CODELINE set labelserver=? where series =?");
        PreparedStatement ps2 = con.prepareStatement("UPDATE BRANCH_FAMILY_CODELINE set release=? where series =?");
        
        while(rs.next()) {
            series = rs.getString("SERIES");
            System.out.println(series);
            labelServer = FamilyModuleHelper.getLabelServerTop(series);
            releaseNum = FamilyModuleHelper.getRelease(series);
            System.out.println("Series: " + releaseNum);
            
            if(!FamilyModuleHelper.isEmpty(labelServer) && labelServer.startsWith("/ade")) {
                ps1.setString(1, labelServer);
                ps1.setString(2,series);
                ps1.addBatch();
            }
            if(!FamilyModuleHelper.isEmpty(releaseNum)) {
                ps2.setString(1, "R" + releaseNum.trim());
                ps2.setString(2,series);
                ps2.addBatch();
            }
        }
        ps1.executeBatch();
        ps2.executeBatch();
        con.commit();
        rs.close();
        stmt.close();
        ps1.close();
        ps2.close();
        con.close();
    }
}
