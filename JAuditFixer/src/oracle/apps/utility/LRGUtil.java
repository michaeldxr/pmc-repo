package oracle.apps.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.Iterator;

import oracle.apps.helpers.FamilyModuleHelper;

public class LRGUtil {

    private  final static String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
    private final static String statsUname = "codescan";
    private final static String statsPwd = "codescan"; 
    private static HashMap<String,Integer> deferredPaths = new HashMap<String,Integer>();
    private static HashMap<String,Integer> seriesRelease = new HashMap<String,Integer>();
    
    static{
        
        try{
            BufferedReader reader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") + 
                                         "/fatools/opensource/jauditFixScripts/txt/Deferred_paths_dbWrappers.txt"));
        //            BufferedReader reader = new BufferedReader(new FileReader("/scratch/sudgupta/view_storage/sudgupta_fatools_may16_view/" +
        //                                         "/fatools/opensource/jauditFixScripts/txt/Deferred_paths.txt"));
            String line = "";
            while((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if(parts.length < 2)
                    continue;
                deferredPaths.put(parts[1].trim(), Integer.parseInt(parts[0].trim()));
            }
            reader.close();
        }catch(Exception e) {
                e.printStackTrace();
        }
        
    }
    
  /*  public static ArrayList<String> getSeriesFromCodeline(String codeline){
        ArrayList<String> seriesList = new ArrayList<String>();
        Connection conn = null;
        Statement stmt;
        
        try {
            conn = DriverManager.getConnection(statsDB,statsUname,statsPwd);
            stmt = conn.createStatement();
            
            String sql = 
                "SELECT distinct series from branch_family_codeline where codeline='"+codeline+"'";
            ResultSet rs = stmt.executeQuery(sql);
            
            while(rs.next()){
                String series = rs.getString("series");
                seriesList.add(series);
            }
            stmt.close();
            conn.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if(conn!=null)
                    conn.close();
            } catch (SQLException exception) {
                System.out.println("Connection cannot be closed.");
            }
        }
        return seriesList;
    }
    
    public static String getCodelineFromSeries(String series){
        String codeline = "";
        Connection conn = null;
        Statement stmt;
        
        try {
            conn = DriverManager.getConnection(statsDB,statsUname,statsPwd);
            stmt = conn.createStatement();
            
            String sql = 
                "SELECT codeline from branch_family_codeline where series='"+series+"'";
            ResultSet rs = stmt.executeQuery(sql);
            
            if(rs.next()){
                codeline = rs.getString("codeline");
            }
            stmt.close();
            conn.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if(conn!=null)
                    conn.close();
            } catch (SQLException exception) {
                System.out.println("Connection cannot be closed.");
            }
        }
        return codeline;
    }*/
            
            

    public static ArrayList<String> getFamiliesFromSeries(String series) {
        ArrayList<String> families = new ArrayList<String>();
        Connection conn = null;
        Statement stmt;

        try {
            conn = DriverManager.getConnection(statsDB, statsUname, statsPwd);
            stmt = conn.createStatement();

            String sql =
                "SELECT family from branch_family_codeline where series='" +
                series + "'";
            ResultSet rs = stmt.executeQuery(sql);

            while (rs.next()) {
                String family = rs.getString("family").toLowerCase();
                if (!family.equals("all"))
                    families.add(family);
                else {
                    families.add("atf");
                    families.add("com");
                    families.add("crm");
                    families.add("fin");
                    families.add("hcm");
                    families.add("hed");
                    families.add("scm");
                    families.add("ic");
                    families.add("prc");
                    families.add("prj");
                    families.add("grc");
                    families.add("fscm");
                }
            }
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException exception) {
                System.out.println("Connection cannot be closed.");
            }
        }
        return families;
    }
    
    
    public static String getCrawlDirNew(String series, String family,String issuetype){
        String crawlDir = "";        if("ELHelpProvider".equals(issuetype) || issuetype.contains("ADFdi") 
           || "ProfileDepdencyErrors".equals(issuetype) || "PlSql_AutonomousTransaction".equals(issuetype)
           || "Seed-Reextract".equals(issuetype)  || "Seed-CheckSum".equals(issuetype) 
           || "MethodActionWithVCParam".equals(issuetype) || "Seed-IncrLoadEnabled".equals(issuetype) 
           || "i18n VONullValueID".equals(issuetype) || "DuplicateDeploymentDescriptorEntries".equals(issuetype)){
          if(series.contains("FSM"))
              crawlDir ="fsm/";
          else
              crawlDir ="fusionapps/";
        }          
        else if ("BIExtenderAutomation".equals(issuetype) || 
                 "SysdateNotTruncated".equals(issuetype) || 
                 "SysdateNotTruncated_new".equals(issuetype) ||
                 "Bad Headers".equals(issuetype) || 
                 "SeedEndDateIncorrect".equals(issuetype) || 
                 "SeedIdCheck".equals(issuetype)) {
            if(series.contains("FSM"))
                crawlDir ="fsm/";
            else
                crawlDir ="fusionapps/"+family.toLowerCase()+"/";
        }        
        else {
            if(series.contains("FSM"))
                crawlDir ="fsm/components/";
            else
                crawlDir ="fusionapps/"+family.toLowerCase()+"/components/";
        }
        
        return crawlDir;
    }
    
    public static String getCrawlDir(String series, String family, String label, String issuetype){
        String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
        String crawlDir = "";
        
        if("ELHelpProvider".equals(issuetype) || issuetype.contains("ADFdi") 
           || "ProfileDepdencyErrors".equals(issuetype) || "PlSql_AutonomousTransaction".equals(issuetype)
           || "Seed-Reextract".equals(issuetype)  || "Seed-CheckSum".equals(issuetype) 
           || "MethodActionWithVCParam".equals(issuetype) || "Seed-IncrLoadEnabled".equals(issuetype) 
           || "i18n VONullValueID".equals(issuetype) || "DuplicateDeploymentDescriptorEntries".equals(issuetype)){
          if(series.contains("FSM"))
              crawlDir =serverTop + "/"+series+".rdd/"+label+"/fsm/";
          else
              crawlDir =serverTop + "/"+series+".rdd/"+label+"/fusionapps/";
        }          
        else if ("BIExtenderAutomation".equals(issuetype) || 
                 "SysdateNotTruncated".equals(issuetype) || 
                 "SysdateNotTruncated_new".equals(issuetype) ||
                 "Bad Headers".equals(issuetype) || 
                 "SeedEndDateIncorrect".equals(issuetype) || 
                 "SeedIdCheck".equals(issuetype)) {
            if(series.contains("FSM"))
                crawlDir =serverTop + "/"+series+".rdd/"+label+"/fsm/";
            else
                crawlDir =serverTop + "/"+series+".rdd/"+label+"/fusionapps/"+family.toLowerCase()+"/";
        }        
        else {
            if(series.contains("FSM"))
                crawlDir =serverTop + "/"+series+".rdd/"+label+"/fsm/components/";
            else
                crawlDir =serverTop + "/"+series+".rdd/"+label+"/fusionapps/"+family.toLowerCase()+"/components/";
        }
        
        return crawlDir;
    }
    
    public static String getLabelServerFromDB(String series) {
        Connection conn = null;
        Statement stmt;
        System.out.println("Getting labelserver from DB...");

        try {
            
            conn = DriverManager.getConnection(statsDB, statsUname, statsPwd);
            stmt = conn.createStatement();

            String sql =
                "SELECT labelserver from branch_family_codeline where series='" + series + "'";
            ResultSet rs = stmt.executeQuery(sql);
            if(rs.next()) 
                return rs.getString("labelserver").trim();
            
        } catch (Exception e) {
            System.out.println("Failed connection to "+statsDB);
            System.out.println("       "+e.getMessage());
        } finally {
            try{
              if (conn != null) conn.close();  
            } catch (Exception ee){
                System.out.println("Failed to close connection: "+statsDB);;
            }
        }
        return "";
    }
    
    public static String getLatestLabel(String series){
       String labelPath = FamilyModuleHelper.getLabelServer(series);
       String label = labelPath.split(".rdd/", 2)[1];
       return label;
    }
    
    public static void addLabelInfoToAB(String issuetype, String series, String label){
        try{
            Connection con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
            con.setAutoCommit(false);
            
            if(!existLabelInfo(con,issuetype, series)){
                System.out.println("Label does not exist for "+issuetype+" on "+series+", now inserting the label.");
                PreparedStatement pstmt =
                    con.prepareStatement("INSERT INTO SCAN_LABELS(ISSUETYPE,SERIES,LATEST_LABEL) " +
                                         "VALUES(?,?,?)");
                    
                pstmt.setString(1, issuetype);
                pstmt.setString(2, series);
                pstmt.setString(3, label);
                pstmt.execute();
                    
                con.commit();
                System.out.println("Label "+label+" inserted successfully.");
                
                pstmt.close();
                con.close();
            }
            
            else{
                System.out.println("Label exists for "+issuetype+" on "+series+", now updating the label.");
                PreparedStatement pstmt =
                    con.prepareStatement("Update SCAN_LABELS Set latest_label='" +label+ "' where issuetype='"+issuetype+"' and series='"+series+"'");
                pstmt.executeUpdate();
                
                con.commit();
                System.out.println("Label updated to "+label+" successfully.");
                
                pstmt.close();
                con.close();
            }
        }catch(SQLException sqle){
            sqle.printStackTrace();
        }
    }
         
    public static boolean existLabelInfo(Connection con, String issuetype, String series) {
        try {
            Statement stmt = con.createStatement();

            String sql =
                "SELECT latest_label from SCAN_LABELS where issuetype='" +
                issuetype + "' and series='" + series + "'";
            ResultSet rs = stmt.executeQuery(sql);
            
            if(rs.next()) {
                System.out.println("current label is "+rs.getString("latest_label"));
                stmt.close();
                //con.close();
                return true;
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return false;         
    }
    
    public static boolean isDeferred(Connection con, String series, String filePath){
        try {
                      
            int release = 8; //current release
            
            if(seriesRelease.containsKey(series))
                release = seriesRelease.get(series);
            else{
                Statement stmt = con.createStatement();

                String sql =
                    "SELECT release from branch_family_codeline where series='" + series + "'";
                ResultSet rs = stmt.executeQuery(sql);
                if(rs.next()) {
                    System.out.println("current release for "+ series +" is "+rs.getString("release"));
                    String releaseOfSeries = rs.getString("release");
                    if(releaseOfSeries.startsWith("R")){
                        release = Integer.parseInt(releaseOfSeries.split("R", 2)[1]); //current release for the given series
                        stmt.close();
                        seriesRelease.put(series,release);
                    }
                }
            }                 
            
            for(Iterator<String> it = deferredPaths.keySet().iterator(); it.hasNext(); ){
                String path = it.next();
                int deferUntilRelease = deferredPaths.get(path);
                if(filePath.contains(path) && release < deferUntilRelease)
                    return true;
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return false;         
    }
    
    public static boolean isDeferred(int releaseNum, String filePath){
                        
        for(Iterator<String> it = deferredPaths.keySet().iterator(); it.hasNext(); ){
                String path = it.next();
                int deferUntilRelease = deferredPaths.get(path);
                if(filePath.contains(path) && releaseNum < deferUntilRelease)
                    return true;
        }
        return false;         
    }
}
