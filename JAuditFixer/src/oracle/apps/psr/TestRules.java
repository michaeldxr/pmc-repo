package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.FileReader;

import java.util.HashSet;
import java.util.Iterator;

public class TestRules {
    public static void main(String[] args) throws Exception{
        
        BufferedReader reader = new BufferedReader(new FileReader("/home/sudgupta/Profile-Report-for-financialsMandatory_v12.txt"));
        String line = "";
        HashSet<String> profileRules = new HashSet<String>();
        
        while((line = reader.readLine()) != null) {
            if(line.trim().startsWith("rule-id:"))
                profileRules.add(line.trim().replace("rule-id:",""));
        }
        
        reader = new BufferedReader(new FileReader("/ade.../FATOOLS_MAIN_LINUX.rdd/LATEST/fatools/" +
                "opensource/jauditFixScripts/txt/audit_rules_source_v12.txt"));
        
        HashSet<String> v2rules = new HashSet<String>();
        while((line = reader.readLine()) != null) {
            v2rules.add(line.trim());
        }
        
        for(Iterator<String> it = profileRules.iterator(); it.hasNext();) {
            String rule = it.next();
            if(!v2rules.contains(rule))
                System.out.println("Missing rule: " + rule);
        }
    }
}
