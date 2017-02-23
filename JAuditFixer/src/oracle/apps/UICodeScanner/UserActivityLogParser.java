package oracle.apps.UICodeScanner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.sql.Statement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* This will be done in FED */

public class UserActivityLogParser {
    
    
    public static void main(String[] args) throws Exception{
        
        HashSet<String> ids = new HashSet<String>();
        HashMap<String,Integer> componentTypes = new HashMap<String,Integer>();
        
        Pattern pViewId = Pattern.compile("\\[VIEW_ID:(.*?)\\]");
        Pattern pRegionViewId = Pattern.compile("\\[REGION_VIEW_ID: \\[(.*?)\\]\\]");
        Pattern pClientId = Pattern.compile("\\[CLIENT_ID:(.*?)\\]");
        Pattern pComponentType = Pattern.compile("\\[COMPONENT_TYPE:(.*?)\\]");
        Pattern pDisplayName = Pattern.compile("\\[DISPLAY_NAME:(.*?)\\]");
        
        BufferedWriter writer = new BufferedWriter(new FileWriter("logComponents.csv"));
        writer.write("SeqId,ClientId, ViewId,RegionViewId,ComponentType,DisplayName\n");
        
        BufferedReader reader = new BufferedReader(new FileReader("/scratch/sudgupta/Desktop/PGALogs/PayableServer_1.out"));
        String line = "";
        
        String viewId="";
        String regionId="";
        String clientId = "";
        //String lastClientId="";
        String componentType="";
        String displayName="";
        
        while((line = reader.readLine()) != null) {
            
            viewId="";
            regionId="";
            clientId = "";
            componentType="";
            displayName="";
            
            if(!line.contains("FIN_USER_ACTIVITY_MONITORING: ENABLED"))
                continue;
            
            Matcher m = pClientId.matcher(line);
            if(m.find())
                clientId=m.group(1).trim();
            else
                continue;
            clientId = clientId.replaceAll(":\\d+:", ":");
//            String[] parts = clientId.split(":");
//            lastClientId=parts[parts.length-1];
            
     /*       if(lastClientId.equals("acff_fnd_kffhoverPopup")
               || lastClientId.equals("addAttachmentPopup") || lastClientId.equals("attachmentListPopup")
                || lastClientId.equals("clLAdds") || lastClientId.equals("clLDel")
                 || lastClientId.equals("confirm") || lastClientId.equals("dc_cb1") 
               || lastClientId.equals("dc_cb2") || lastClientId.equals("dConfDelPop") 
               || lastClientId.equals("APb") || lastClientId.equals("APc"))
                
                lastClientId=parts[parts.length-2]+":"+parts[parts.length-1];
            
            if(clientId.endsWith(":popApplicationsTable:_ATp:delete")
                || clientId.endsWith(":popApplicationsTable:_ATp:create")
                || clientId.endsWith(":popApplicationsTable:_ATp:popAttachmentTable"))                
                lastClientId= parts[parts.length-4]+":"+parts[parts.length-3]+":"+parts[parts.length-2]+":"+parts[parts.length-1];
            
            if(clientId.endsWith(":iLstAtt:lastAttachedPopup")
                || clientId.endsWith(":_ATp:edit") || clientId.endsWith(":_ATp:createMenuItem")
                || clientId.endsWith(":_ATp:create") || clientId.endsWith(":_ATp:editMenuItem")
                || clientId.endsWith(":_ATp:delete") || clientId.endsWith(":_ATp:deleteMenuItem")
                || clientId.endsWith(":_ATp:ATex") || clientId.endsWith(":_ATp:ATexMenuItem"))
                
                lastClientId= parts[parts.length-3]+":"+parts[parts.length-2]+":"+parts[parts.length-1];
                
            if(clientId.endsWith(":popApplicationsTable:_ATp:popAttachmentTable:ifPopup"))
                lastClientId= parts[parts.length-5]+":"+parts[parts.length-4]+":"+parts[parts.length-3]+":"+parts[parts.length-2]+":"+parts[parts.length-1];
              
            if(clientId.endsWith("::_afrLovInternalQueryId"))    
                lastClientId=parts[parts.length-3]+"::_afrLovInternalQueryId";
            
            if(clientId.endsWith("::pop::cd"))    
                lastClientId=parts[parts.length-5]+"::pop::cd";*/
            
            m = pViewId.matcher(line);
            if(m.find())
                viewId=m.group(1).trim();            
            
            m = pRegionViewId.matcher(line);
            if(m.find())
                regionId=m.group(1).trim();
            
           String[] parts = regionId.split(",");
            regionId=parts[0].trim();
            
            m = pComponentType.matcher(line);
            if(m.find())
                componentType=m.group(1).trim();
            //componentType= componentType.replace("oracle.adf.","");
            
            m = pDisplayName.matcher(line);
            if(m.find())
                displayName=m.group(1).trim();
            
            ids.add(clientId + ", " + viewId+", "+regionId+", "+componentType+", "+displayName);  
            
            if(componentTypes.containsKey(componentType)){
                int i = componentTypes.get(componentType);
                componentTypes.put(componentType,i+1);
            }else
                componentTypes.put(componentType,1);
        }
        
        Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","findev","findev");
        conn.setAutoCommit(false);
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO LOG_COMPONENTS(CLIENT_ID,VIEW_ID,REGION_VIEW_ID,COMPONENT_TYPE,DISPLAY_NAME) " +
                "VALUES(?,?,?,?,?)");
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("DELETE FROM LOG_COMPONENTS");
        
        int count = 0;
        System.out.println(ids.size());
        for(Iterator<String> it = ids.iterator(); it.hasNext(); ){
            
            String details = it.next();
            writer.write(++count + "," + details + "\n");
            String[] parts = details.split(",");
            pstmt.setString(1,parts[0].trim());
            pstmt.setString(2,parts[1].trim());
            pstmt.setString(3,parts[2].trim());
            pstmt.setString(4,parts[3].trim());
            pstmt.setString(5,parts[4].trim());
            pstmt.addBatch();
            
            if(count%1000==0)
                pstmt.executeBatch();  
        }
        pstmt.executeBatch(); 
        System.out.println(componentTypes);
        
        reader.close();
        writer.close();
        conn.commit();
        pstmt.close();
        stmt.close();
        conn.commit();
        conn.close();
        
    }
    
}
