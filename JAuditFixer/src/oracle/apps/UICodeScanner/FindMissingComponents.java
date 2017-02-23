package oracle.apps.UICodeScanner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;

import java.io.FileWriter;

import java.util.HashMap;

public class FindMissingComponents {
   
    public static void main(String[] args) throws Exception {
        
        HashMap<String,String> componentMap = new HashMap<String,String>();
        BufferedWriter writer = new BufferedWriter(new FileWriter("missingcomponents.csv"));
        
        //1. Build component Map
        BufferedReader reader = new BufferedReader(new FileReader("adfComponentDetails.csv"));
        String line = "";
        while((line = reader.readLine()) != null){
            
            String[] parts = line.split(",");
            String nodeId = parts[7].trim();
            String regionId="";
            if(parts.length>10)
              regionId = parts[10].trim();
            String viewId="";
            if(parts.length>11)
                viewId = parts[11].trim();
            
            if(!componentMap.containsKey(nodeId))
                componentMap.put(nodeId, regionId+","+viewId);
            else{
                String existingId = componentMap.get(nodeId);
                existingId += "::" + regionId+","+viewId;
                componentMap.put(nodeId, existingId);
            }
       }
        reader.close();
        
        //2. Print Missing components
        reader = new BufferedReader(new FileReader("logComponents.csv"));
        int count = 0;
        while((line = reader.readLine()) != null){
            
            String[] parts = line.split(",");
            String lastClientId= parts[2].trim();
            String viewId=parts[3].trim();
            String regionId=parts[4].trim();
            
            if(!componentMap.containsKey(lastClientId)){
                writer.write(line+"\n");
                count++;
                continue;
            }
            String region_view_ids = componentMap.get(lastClientId);
            parts = region_view_ids.split("::");
            boolean found = false;
            for(int i=0; i< parts.length; i++){
                
                String[] regionView = parts[i].trim().split(",");
                String mappedregionId = regionView[0].trim();
                String mappedviewId="";
                if(regionView.length >1)
                  mappedviewId=regionView[1].trim();
                if(!mappedregionId.equals("") && mappedregionId.contains(regionId)){
                    found = true;
                    break;
                }
                if(mappedregionId.equals("") && !mappedviewId.equals("") && mappedviewId.contains(viewId)){
                    found = true;
                    break;
                }                
            }
            if(!found){
                writer.write(line+"\n");
                count++;
            }
      }  //while
        System.out.println(count);
        reader.close();
        writer.close();
   }
}
