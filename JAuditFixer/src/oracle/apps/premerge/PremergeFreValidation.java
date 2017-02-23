package oracle.apps.premerge;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.FileParserHelper;
import oracle.apps.helpers.ProcessExecutor;

public class PremergeFreValidation {
    
    static BufferedWriter writer;
    static String viewlabel = "";
    static String fredb = "";
    static Pattern pErrorPattern = Pattern.compile("Number of validation errors : (\\d+)");
    static Pattern pUrl = Pattern.compile("Report URL - (.*)");
    static Pattern pSystem = Pattern.compile("systempass\\s*:\\s*(\\w+)\\s*/");
    static Pattern pFusion = Pattern.compile("fusionpass\\s*:\\s*(\\w+)\\s*/");
    
    public static void main(String[] args) throws Exception{
        
        writer = new BufferedWriter(new FileWriter("bugOutputForFreValidation.txt",true));
        viewlabel = args[0].trim();
        if(args.length > 1){
            fredb=args[1].trim();
            System.out.println(fredb);
        }
        
        //parseEnvSid(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX")+"/fatools/opensource/jauditFixScripts/FinPreMerge/fredbStrings.txt");
        
        /*if(fredb.equals("")){
            writer.write("\nNo database found to perform Fre Validations. Skipping ...");
            writer.write("\nYou can manually perform fre validations by invoking freValidate from the command line as follows: ");
            writer.write("\nfreValidate -source currenttransction -database <host:port:sid> -systempass manager -fusionpass fusion");
            writer.close();
            System.exit(2);
        }*/
        
        String databaseString="";
        String fusionpass = "fusion";
        String systempass = "manager";
        
         String cmd = "freValidate -source currenttransaction -category C_PREMERGE";
         
        if(!fredb.equals("")){
            String[] parts = fredb.split("/");
            if(parts.length < 1){
                writer.write("\n FRE database string not specified in the correct format.");
                writer.write("\nPlease specify -r dbhost:port:sid/systempass:<ststem password>/fusionpass:<fusion_password>");
                writer.write("\n e.g. -r abc.us.oracle.com:1521:abc/systempass:manager/fusionpass:fusion");
                writer.close();
                System.exit(5);
            }
            databaseString = parts[0].trim();
            
            Matcher m = pSystem.matcher(fredb);
            if(m.find())
                systempass = m.group(1);
            m= pFusion.matcher(fredb);
             if(m.find())
                 fusionpass = m.group(1);
             
             cmd = "freValidate -source currenttransaction -database " + databaseString + " -systempass " + systempass + " -fusionpass " +  fusionpass;
        }
 
         ProcessExecutor pe = new ProcessExecutor();   
         ProcessBuilder pb = new ProcessBuilder("bash","-c",cmd);
         pe.runProcess(pb, "raw_freValidation.txt", false); 
         parseOutput();
        
     }
    
    public static void parseOutput() throws Exception{
        
        String fileContestsAsString = FileParserHelper.getFileContentsAsString("raw_freValidation.txt");
        if(fileContestsAsString.contains("DB conection error") || fileContestsAsString.contains("Invalid DBString specified")) {
            writer.write("\nCould not connect to database. Skipping ...");
            writer.write("\n\nPlease see $ADE_VIEW_ROOT/fusionapps/premerge/raw_freValidation.txt file for more details on the error.");
            writer.write("\n\nYou can manually perform fre validations by invoking freValidate from the command line as follows: ");
            writer.write("\n\nfreValidate -source currenttransction -database <host:port:sid> -systempass manager -fusionpass fusion");        
            writer.close();
            System.exit(5);
        }
        if(fileContestsAsString.contains("[ERROR]") && fileContestsAsString.contains("Aborted.")) {
            writer.write("\nCould not perform freValidation. Skipping ...");
            writer.write("\n\nPlease see $ADE_VIEW_ROOT/fusionapps/premerge/raw_freValidation.txt file for more details on the error.");
            writer.write("\n\nYou can manually perform fre validations by invoking freValidate from the command line as follows: ");
            writer.write("\n\nfreValidate -source currenttransction -database <host:port:sid> -systempass manager -fusionpass fusion");        
            writer.close();
            System.exit(5);
        }
        Matcher m = pErrorPattern.matcher(fileContestsAsString);
        Matcher m1 = pUrl.matcher(fileContestsAsString);
        if(m.find()){
            
            int numViolations = Integer.parseInt(m.group(1));
            writer.write(m.group() + "\n\n");
            if(m1.find())
                writer.write(m1.group() + "\n\n");
            
            writer.write("\n\n You may ignore violations that do not belong to your transaction.\n\n");
            writer.close();
            
            if(numViolations == 0){  
                if(!fredb.equals(""))
                    System.exit(0);
                else
                    System.exit(1);
            }
            else {
                if(!fredb.equals(""))
                    System.exit(2);
                else
                    System.exit(3);
            }
        }
    }
}
