package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class GenerateProfileSourceXml {
   
    public static void main(String[] args) throws Exception {
        
        BufferedReader reader = new BufferedReader(new FileReader("/scratch/sudgupta/Dependency_Rules.txt"));
        BufferedWriter writer = new BufferedWriter(new FileWriter("/scratch/sudgupta/auditRulesSource.xml"));
        writer.write("<?xml version = '1.0' encoding = 'UTF-8' ?>\n");
        writer.write("<audit-rules>\n");
        String line = "";
        while((line = reader.readLine()) != null){
            if(line.trim().equals(""))
                continue;
            writer.write("<audit-rule ruleId=\"" + line.trim() + "\" enabled=\"true\" style=\"error\" severity=\"error\"/>\n");            
        }
        reader.close();
        writer.write("</audit-rules>\n");
        writer.close();
    }
}
