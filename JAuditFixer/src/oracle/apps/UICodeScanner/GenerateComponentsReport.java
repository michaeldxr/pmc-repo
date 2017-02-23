package oracle.apps.UICodeScanner;

import java.io.BufferedWriter;

import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class GenerateComponentsReport {
    
    public static void main(String[] args) throws Exception {
        
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","findev","findev");
        Statement stmt = conn.createStatement();
        BufferedWriter writer = new BufferedWriter(new FileWriter("componentDetailsReport.csv"));
        writer.write("CLIENT_ID,VIEW_ID,REGION_ID,COMPONENT_TYPE,DISPLAY_NAME,UI_FILE,PARENT_COMPONENT_ID,PARENT_COMPONENT_TYPE," +
            "PARENT_COMPONENT_DESCRIPTION, FAMILY,MODULE,PRODUCT,COMMENT\n");
        
        ResultSet rs = stmt.executeQuery("SELECT lcd.client_id as CLIENT_ID, lcd.view_ID as VIEW_ID, lcd.region_ID as REGION_VIEW_ID," +
            "lcd.component_type as component_type, lcd.display_name as DISPLAY_NAME, " +
            "acm.ui_file as UI_FILE, acm.component_id as PARENT_COMPONENT_ID, acm.component_type as PARENT_COMPONENT_TYPE, " +
            "acm.resolved_display_text as PARENT_COMPONENT_DESCRIPTION, upd.family as FAMILY, upd.module as MODULE," +
            "upd.PRODUCT as PRODUCT, lcd.DESCRIPTION as description " +
                                         
            "FROM LOG_COMPONENT_DETAILS lcd, ADF_COMPONENTS_MAP acm ,UI_PAGE_DETAILS upd " +
                                         
            "WHERE lcd.parent_component_id = acm.SEQ_ID(+)" +
            " and acm.ui_file = upd.ui_page(+)");
        
        String clientId="";
        String viewId="";
        String regionId="";
        String compType="";
        String displayName="";
        String uiFile="";
        String parentCompId="";
        String parentCompType="";
        String parentCompDesc="";
        String family="";
        String module="";
        String product="";
        String comment="";
        
        
        while(rs.next()){
            
            clientId=rs.getString("CLIENT_ID");
            viewId=rs.getString("VIEW_ID");
            regionId=rs.getString("REGION_VIEW_ID");
            compType=rs.getString("COMPONENT_TYPE");
            displayName=rs.getString("DISPLAY_NAME");
            uiFile=rs.getString("UI_FILE");
            if(uiFile == null)
                uiFile="";
            parentCompId=rs.getString("PARENT_COMPONENT_ID");
            if(parentCompId==null)
                parentCompId="";
            parentCompType=rs.getString("PARENT_COMPONENT_TYPE");
            if(parentCompType==null)
                parentCompType="";
            parentCompDesc=rs.getString("PARENT_COMPONENT_DESCRIPTION");
            if(parentCompDesc==null)
                parentCompDesc="";
            family=rs.getString("FAMILY");
            if(family==null)
                family="";
            module=rs.getString("MODULE");
            if(module==null)
                module="";
            product=rs.getString("PRODUCT");
            if(product==null)
                product="";
            comment=rs.getString("DESCRIPTION");
            if(comment==null)
                comment="";
            
            writer.write(clientId + "," + viewId + "," + regionId + "," + compType + "," + displayName + 
                         "," + uiFile + "," + parentCompId + "," + parentCompType + "," + parentCompDesc + "," +
                         family + "," + module + "," + product + "," + comment + "\n");
        }
        writer.close();
        rs.close();
        stmt.close();
        conn.close();
    }
}
