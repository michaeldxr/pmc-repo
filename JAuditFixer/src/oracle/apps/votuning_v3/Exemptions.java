package oracle.apps.votuning_v3;

import java.io.BufferedReader;
import java.io.FileReader;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import java.util.Iterator;

import oracle.apps.helpers.FamilyModuleHelper;

public class Exemptions {
    public Exemptions() {
        super();
    }
    
    static HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();
    
    public static ArrayList<ScanResult> filterExemptions(ArrayList<ScanResult> al_violations){
        generateExemptionList();
        
        ScanResult r = null; 
        String fileNameInfo;
            
        Iterator<ScanResult> i = al_violations.iterator();
        while(i.hasNext()){
          r = i.next();
            try {
                fileNameInfo= FamilyModuleHelper.getFileNameInfo1(r.getPage());
            } catch (IOException e) {
                continue;
            }
            
            if(isExempt(r.getTableId(),fileNameInfo.split(",")[3].trim())){
                i.remove();
            }
        }
        
        return al_violations;
    }
    
    
    public static void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_Yao.txt"));
        
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 3)
            continue;
        String issue = parts[0].trim();
        if(!issue.equals("TableAndLOVTuning_v2"))
            continue;
        String fileName = parts[1].trim();
        String id = parts[2].trim();
          
        HashSet<String> exemptLines = null;
        if(!exemptions.containsKey(fileName)) 
          exemptLines = new HashSet<String>();
        else 
          exemptLines = exemptions.get(fileName);
        
        exemptLines.add(id);
        exemptions.put(fileName, exemptLines);
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }
    
    private static boolean isExempt(String id, String fileName){
        
        if(!exemptions.containsKey(fileName)) 
            return false;
        
        HashSet<String> componentIds = (HashSet<String>)exemptions.get(fileName);
        if(componentIds.contains(id))
            return true;
        
        return false;
    }
}
