package oracle.apps.UICodeScanner;

import java.io.BufferedReader;

import java.io.FileReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;

public class TagLogComponents {
   
    public static void main(String[] args) throws Exception{
        
        ArrayList<String> patterns = new ArrayList<String>();
        String mode = "full"; //can be full/incremental
        if(args.length < 2) {
            System.out.println("You must specify the regEx file and DB connection details. Invoke the script as:\n " +
                "tagLogComponents.sh <path to parentIdRegex.txt> <dbhost>:port:sid/username/pwd");
            System.exit(1);
        }
        
        String fileName = args[0].trim();
        String dbConnDetails = args[1].trim();
        String[] parts = dbConnDetails.split("/");
        if(parts.length < 3) {
            System.out.println("DB Connection details must be speficied in the format: dbhost:port:sid/username/pwd");
            System.exit(1);
        }        
        
        if(args.length > 2) mode = args[2].trim();
        if(!mode.equals("full") && !mode.equals("incremental")){
            System.out.println("Mode can only be 'full' or 'incremental'");
            System.exit(1);            
        }            
        
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        String line="";
        
        while((line=reader.readLine()) != null) {
            
            if(line.trim().equals("") || line.trim().startsWith("#"))
                continue;
            patterns.add(line.trim());
        }        
        reader.close();
        
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@"+parts[0].trim(),parts[1].trim(),parts[2].trim());
        conn.setAutoCommit(false);
        Statement stmt = conn.createStatement();
        Statement stmt1 = conn.createStatement();
        
        ResultSet rs = stmt.executeQuery("SELECT LAST_SEQ_ID from LASTSEQ");
        if(!rs.next())
            stmt1.executeUpdate("INSERT INTO LASTSEQ values (0)");
                
        if(mode.equals("full")){
            stmt.executeUpdate("DELETE FROM LOG_COMPONENT_DETAILS");
            stmt.executeUpdate("UPDATE LASTSEQ set LAST_SEQ_ID=0");
        }
        
        ResultSet rs1 = stmt1.executeQuery("SELECT max(CLICKID) from CLICKSTREAM");
        int maxid = 0;
        if(rs1.next())
            maxid=rs1.getInt(1);
        rs1.close();
        
        String nestedQuery = "(SELECT DISTINCT client_id,view_id,region_view_id_first as region_id,component_type,display_name FROM CLICKSTREAM " +
            "WHERE clickid > (select LAST_SEQ_ID from LASTSEQ)) lc"; 
       
        rs = stmt.executeQuery("SELECT client_id,view_id,region_id,component_type,display_name," +
            "nvl(rpm.ui_page,vpm.ui_page) as ui_page FROM " +
            "region_page_map rpm, view_page_Map vpm," + nestedQuery + 
            " WHERE lc.region_id=rpm.regionid(+) and lc.view_id = vpm.viewid(+)");     
        
        stmt1.executeUpdate("UPDATE LASTSEQ set LAST_SEQ_ID="+maxid);
        
        PreparedStatement ps1 = conn.prepareCall("INSERT INTO LOG_COMPONENT_DETAILS(CLIENT_ID,VIEW_ID,REGION_ID,DISPLAY_NAME,COMPONENT_TYPE,PARENT_COMPONENT_ID,DESCRIPTION) VALUES (?,?,?,?,?,?,?)");
        PreparedStatement ps2 = conn.prepareCall("SELECT SEQ_ID from ADF_COMPONENTS_MAP WHERE UI_FILE=? and component_id=?");
        
        String clientId=null;
        String region=null;
        String view = null;
        String displayName=null;
        String componentType=null;
        String definingCompId = null;
        String origClientId = null;
        
        String parentComponentId = "";
        String description = "";
        String ui_page = "";
        int count = 0;
        
        while(rs.next()){
            
            count++;
            parentComponentId = "";
            description = "";
            definingCompId = "";
    
            
            clientId = rs.getString("CLIENT_ID");
            clientId = clientId.replaceAll(":\\d+:", ":");
            origClientId = clientId;
            
            region = rs.getString("REGION_ID"); 
            view = rs.getString("VIEW_ID");
            displayName = rs.getString("DISPLAY_NAME");
            componentType = rs.getString("COMPONENT_TYPE");
            ui_page=rs.getString("UI_PAGE");
            
           /* if(region == null)
                region = " ";
            if(view == null)
                view = " ";
            if(displayName == null)
                displayName = " ";
            if(componentType == null)
                componentType = " ";
            if(ui_page == null)
                ui_page= "";*/
            
       /*     String[] regionParts = region.split(",");
            ps2.setString(1, regionParts[0]);
            rs1 = ps2.executeQuery();
            if(rs1.next())
                ui_page = rs1.getString("UI_PAGE");
            
            if(ui_page==null || ui_page.equals("")){
                ps3.setString(1, view);
                rs1 = ps3.executeQuery();
                if(rs1.next())
                    ui_page = rs1.getString("UI_PAGE");
            }*/
            
            //special handling for query panels
            if(clientId.contains(":operator")) {
                clientId = clientId.replaceAll(":operator\\d+", "");  
                description = "Operator (shown when Advanced Search button is clicked) ";                    
            }
            if(clientId.contains(":value")) {
                clientId = clientId.replaceAll(":value\\d+", "");  
                description = "Search field ";
            }
            //special handling for inputListOfValues selections
            if(clientId.contains(":addFieldsIter")) {
                clientId = clientId.replaceAll(":addFieldsIter.*$", "");  
                description = "Search field ";
            }
            
            for(int i =0; i < patterns.size(); i++ ){
                
                String patternLine = patterns.get(i);  
                parts = patternLine.split(",");
                String pattern = parts[0].trim();
                
                if(clientId.endsWith(pattern)){
                    clientId = clientId.replace(pattern, "");
                    if(parts.length > 1) description += parts[1].trim();
                    break;
                } 
            }
            
            String[] compIdParts = clientId.split(":");
            definingCompId = compIdParts[compIdParts.length - 1];
            ps2.setString(1,ui_page);
            ps2.setString(2,definingCompId);
            rs1=ps2.executeQuery();
            if(rs1.next())
                parentComponentId=rs1.getString("SEQ_ID");
            else{
                // Special handling for descriptive flex fields                
                if(definingCompId.contains("Iterator")){
                    String[] lastCompIdParts = definingCompId.split("_");
                    String dffId = lastCompIdParts[0];
                    ps2.setString(1,ui_page);
                    ps2.setString(2,dffId);
                    rs1=ps2.executeQuery();
                    if(rs1.next())
                        parentComponentId=rs1.getString("SEQ_ID");
                }
            }
            
//LOG_COMPONENT_DETAILS(CLIENT_ID,VIEW_ID,REGION_VIEW_ID,DISPLAY_NAME,COMPONENT_TYPE,PARENT_COMPONENT_ID,DESCRIPTION) 
            ps1.setString(1,origClientId);
            ps1.setString(2,view);
            ps1.setString(3,region);
//            if(displayName == null || view == null || region == null || componentType == null || parentComponentId == null)
//                System.out.println("break here");
            ps1.setString(4,displayName);
            ps1.setString(5,componentType);
            ps1.setString(6,parentComponentId);
            ps1.setString(7,description);
            
//            if(!description.equals(""))
//                ps1.setString(7,description);
//            else
//                ps1.setString(7,"");
            ps1.addBatch();
            
            if(count%1000==0)
                ps1.executeBatch();            
        }
        ps1.executeBatch();     
        if(ps1 != null) ps1.close();
        if(ps2 != null) ps2.close();
        if(rs !=null) rs.close();
        if(rs1 !=null) rs1.close();
        stmt.close();
        conn.commit();
        conn.close();
    }
}
