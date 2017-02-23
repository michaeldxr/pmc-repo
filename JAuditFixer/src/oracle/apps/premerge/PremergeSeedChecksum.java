package oracle.apps.premerge;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.FileParserHelper;
import oracle.apps.helpers.Mail;
import oracle.apps.helpers.ProcessExecutor;

public class PremergeSeedChecksum {
    
    static String txn = null;
    static String viewlabel = "";
    static String env = "";
    static String adeViewRoot = "";
    static Pattern pPatchId = Pattern.compile("Patch ID allocated as \\[(\\d+)\\]");
    static Pattern pPatchStatus = Pattern.compile("Patch Status\\s*\\:(.*)"); 
    static BufferedWriter writer;
    static boolean comExists = true;
    static boolean acrExists = true;
    static boolean dbExists = true;
    static boolean sqlExists = true;
    
    public static void main(String[] args) throws Exception{
        viewlabel = args[0].trim();
        txn = args[1].trim();
        if(args.length > 2 && !args[2].trim().equals("")){
            env=args[2].trim();
            System.out.println("FA_PREMERGE_DB Branch property set to: " + env);
        }

        writer = new BufferedWriter(new FileWriter("bugOutputForSeedValidation.java.txt"));
	writer.write("Java code started at " + (new Date()).toString());

        
        adeViewRoot = System.getenv("ADE_VIEW_ROOT");
        System.out.println("Java code started at "+ (new Date()).toString());
        System.out.println("adeViewRoot:" + adeViewRoot);
       
        System.out.println("Transaction: " + txn); 
        System.out.println("View label: " + viewlabel); 
        //Determine the env/db sid 
      
      // Need this only for compatibility with rel7
        if(env.equals(""))
            parseEnvSid(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX")+"/fatools/opensource/jauditFixScripts/FinPreMerge/dbEnvSids.txt");
        
        if(env.equals("")) {
            writer.write("FA_PREMERGE_DB branch property not set for this env. Skipping seed validation...");
            return;
        }
  
       
        
//        System.out.println("Checking queue status");
//        checkQueueStatus(null);
        
        System.out.println("Creating Patching manifest at :" + (new Date()).toString() );
        
        ProcessExecutor pe = new ProcessExecutor();   
        
        //First delete the temporary manifest
        ProcessBuilder pb = new ProcessBuilder("bash","-c","rm -f /tmp/patchingManifest.txt");
        pe.runProcess(pb, "raw_seedchecksum.txt", false);
        
        //Create manifest
        String cmd_invoke = "fpr -e " + env + " -s " + txn + " -m /tmp/patchingManifest.txt -r createmanifest -f F_PREMERGE";
        System.out.println("Invoking fpr at " + (new Date()).toString() + " " + cmd_invoke);       
        
        pb = new ProcessBuilder("bash","-c",cmd_invoke);
        pe.runProcess(pb, "raw_seedchecksum.txt", true);
        
        //exit if manifest is not created
        File f = new File("/tmp/patchingManifest.txt");
        if(!f.exists()) {
            System.out.println("Transaction has no files to patch. " + (new Date()).toString());
            writer.write("\n\nTransaction has no files to patch. " + (new Date()).toString());
            exitSystem(3);
        }
        
        //Create the directories if they don't exist
        File comDir = new File(adeViewRoot + "/fusionapps/com");
        if(!comDir.exists()) {
            comExists=false;
            System.out.println("fusionapps/com path does not exist. It will be created");
            comDir.mkdirs();            
        }
        
        comDir = new File(adeViewRoot + "/fusionapps/com/acr");
        if(!comDir.exists()) {
            acrExists = false;
            System.out.println("fusionapps/com/acr path does not exist. It will be created");
            comDir.mkdirs();            
        }
        
        comDir = new File(adeViewRoot + "/fusionapps/com/acr/db");
        if(!comDir.exists()) {
            dbExists = false;
            System.out.println("fusionapps/com/acr/db path does not exist. It will be created");
            comDir.mkdirs();            
        }
        
        comDir = new File(adeViewRoot + "/fusionapps/com/acr/db/sql");
        if(!comDir.exists()) {
            sqlExists = false;
            System.out.println("fusionapps/com/acr/db/sql path does not exist. It will be created");
            comDir.mkdirs();            
        }
        
        
        //////////////////////////////////////////////////////////////////////////
        
        String comPath = comDir.getAbsolutePath();
        System.out.println("\n" + (new Date()).toString() + " ComPath: " + comPath);
        System.out.println("Creating symlink...");

        pb = new ProcessBuilder("bash","-c","ln -s " + 
                               adeViewRoot + "/fatools/opensource/jauditFixScripts/FinPreMerge/apps_flex_check.sql "
                                + comPath + "/apps_flex_check.sql"); 
        pe.runProcess(pb,"raw_seedchecksum.txt", true);
            
        BufferedWriter writer1 = new BufferedWriter(new FileWriter("/tmp/patchingManifest.txt",true));
        writer1.write("fusionapps/com/acr/db/sql/apps_flex_check.sql\n");
        writer1.close();
        
        System.out.println("\n" + (new Date()).toString() +  " Invoking seed data patching framework:");
        
        cmd_invoke = "fpr -e " + env + " -s " + txn + " -p V_CONT_EQUIV=NO -o O_PREMERGE -f F_PREMERGE -m /tmp/patchingManifest.txt";
        System.out.println("\n" + (new Date()).toString() + " " + cmd_invoke);
        pb = new ProcessBuilder("bash","-c",cmd_invoke);
        pe.runProcess(pb, "raw_seedchecksum.txt", true);
        
        String jobid = parseJobId();
        System.out.println("\n" + (new Date()).toString() + " " + "Patch job id is: " + jobid);
        
        if(jobid == null) {
            System.out.println("\n" + (new Date()).toString() + " " + "No patch id was allocated by fpr scheduler. This should not happen. Please validate your transaction manually."); 
            writer.write("\n\nNo patch id was allocated by fpr scheduler. This should not happen. Please validate your txn manually.");
            exitSystem(1); 
        }
        if(jobid.equals("0")) {
            System.out.println("\n" + (new Date()).toString() + " " + "Transaction has no files to patch. " + (new Date()).toString());
            writer.write("\n\nTransaction has no files to patch. " + (new Date()).toString());
            exitSystem(3);
        }
        if(jobid.equals("-1")) {
            System.out.println("Source Transaction not found.");
            writer.write("\n\nSource Transaction not found.");
            exitSystem(4);
        }
        if(jobid.equals("-2")) {
            System.out.println("User is not allowed to patch the environment. Please contact premerge DB administrators to get permission to patch.");
            writer.write("\n\nUser is not allowed to patch the environment. Please contact premerge DB administrators to get permission to patch.");
            exitSystem(1);
        }
        if(jobid.equals("-3")) {
            System.out.println("Env does not exist. Premerge cannot be used to perform Seed validation.");
            writer.write("\n\nEnv does not exist. Premerge cannot be used to perform Seed validation.");
            exitSystem(1);
        }
        
        printUrl(jobid);
        String jobStatus = pollPatchStatus(jobid);
        System.out.println("\n" + (new Date()).toString() + " " + "Final Job status is: " + jobStatus);
        if(jobStatus.contains("PASSED (WITH WARNINGS")) {
            System.out.println("\n" + (new Date()).toString() + " " + "\nPatching completed successfuly. But, some requested files did not load on thier first attempt " +
                "but were successful on subsequent tries. This may indicate an unspecified file dependency relationship in " +
                "your transaction files which should be investigated and resolved. See the detailed status in job_status.txt");
            writer.write("\n\nPatching completed successfuly. Some requested files did not load on thier first attempt " +
                "but were successful on subsequent tries. This may indicate an unspecified file dependency relationship in " +
                "your transaction files which should be investigated and resolved. See the detailed status in $ADE_VIEW_ROOT/fusionapps/premerge/job_status.txt " + (new Date()).toString());
            exitSystem(0); //exit with SUCCESS
        }
        else if(jobStatus.contains("PASSED")) {
            System.out.println("\n" + (new Date()).toString() + " " + "\nPatching completed successfuly. See the detailed status in job_status.txt");
            writer.write((new Date()).toString() + "\n\nPatching completed successfuly. See the detailed status in $ADE_VIEW_ROOT/fusionapps/premerge/job_status.txt");
            exitSystem(0); //exit with SUCCESS
        }
        else if(jobStatus.contains("FAILED")) {
            System.out.println("\n" + (new Date()).toString() + " " + "\nPatching had some failures. See the detailed status in job_status.txt");
            writer.write( "\n" + (new Date()).toString() + "\n\nPatching had some failures. See the detailed status in $ADE_VIEW_ROOT/fusionapps/premerge/job_status.txt");
            exitSystem(1); //exit with FAILURE
        }
        else if(jobStatus.contains("UNKNOWN")) {
            System.out.println("\n" + (new Date()).toString() + " " + "\nPatching status is UNKNOWN. Premerge is timing out as it could not get completed patching status in " +
                "30 mins. Seed data patching could not be tested. Developers are advised to complete the patch testing before merging any seed data changes.");
            writer.write("\n" + (new Date()).toString() + "\n\nPatching status is UNKNOWN. Premerge is timing out as it could not get completed patching status in " +
                "30 mins. Seed data patching could not be tested. Developers are advised to complete the patch testing before merging any seed data changes.");
            exitSystem(2); //exit with WARNING
        }
        else if(jobStatus.contains("Logfile missing")) {
            System.out.println("\n" + (new Date()).toString() + " " + "\nPatching status cannot be ascertained as Logfile is missing. Please review the status of the patch manually before merging code.");
            writer.write("\n" + (new Date()).toString() + "\n\nPatching status cannot be ascertained as Logfile is missing. Please review the status of the patch manually before merging code.");
            exitSystem(2); //exit with WARNING
        }
        writer.close();
    }
    
    static void printUrl(String jobid) throws Exception{
        if(jobid !=null){
            System.out.println("\n" + (new Date()).toString() + " " + "\nYou can go to http://fre.us.oracle.com:7777/cgi-bin/fprScheduler?scope=env&req=job&sid="+env+"&jobid="+jobid
                              + " to view the full patch details.");
            writer.write("\n" + (new Date()).toString() + "\n\nYou can go to http://fre.us.oracle.com:7777/cgi-bin/fprScheduler?scope=env&req=job&sid="+env+"&jobid="+jobid
                          + " to view the full patch details.");
        }
        
            System.out.println("\n" + (new Date()).toString() + "\nYou can also go to http://fre.us.oracle.com:7777/cgi-bin/fprScheduler?scope=env&req=queue&sid="+env
                              + " to view the environment details.");
            writer.write("\n" + (new Date()).toString() + "\n\nYou can also go to http://fre.us.oracle.com:7777/cgi-bin/fprScheduler?scope=env&req=queue&sid="+env
                          + " to view the environment details.");
            System.out.println("Review the KIX for more details about seed framework integration with premerge:" +
                "http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/SeedframeworkInPremerge");
    }
    
    static void exitSystem(int code) {
        
        try{
            writer.close();
            
            ProcessExecutor pe = new ProcessExecutor();
            
            String cmd = "rm -f " + adeViewRoot+"/fusionapps/com/acr/db/sql/apps_flex_check.sql"; 
            ProcessBuilder pb = new ProcessBuilder("bash","-c",cmd);
            pe.runProcess(pb, "raw_seedchecksum.txt", true);
            
            if(!comExists){
                System.out.println("\n" + (new Date()).toString() + " " + "deleting com...");
                cmd = "rm -rf " + adeViewRoot+"/fusionapps/com";
                pb = new ProcessBuilder("bash","-c",cmd);
                pe.runProcess(pb, "raw_seedchecksum.txt", true);
            } else if(!acrExists) {
                System.out.println("deleting com/acr...");
                cmd = "rm -rf " + adeViewRoot+"/fusionapps/com/acr";
                pb = new ProcessBuilder("bash","-c",cmd);
                pe.runProcess(pb, "raw_seedchecksum.txt", true);
            } else if(!dbExists) {
                System.out.println("deleting com/acr/db...");
                cmd = "rm -rf " + adeViewRoot+"/fusionapps/com/acr/db";
                pb = new ProcessBuilder("bash","-c",cmd);
                pe.runProcess(pb, "raw_seedchecksum.txt", true);
            } else if(!sqlExists) {
                System.out.println("deleting com/acr/db/sql...");
                cmd = "rm -rf " + adeViewRoot+"/fusionapps/com/acr/db/sql";
                pb = new ProcessBuilder("bash","-c",cmd);
                pe.runProcess(pb, "raw_seedchecksum.txt", true);
            } 
           
//            if(code != 0 && code !=3)
//                Mail.sendMail("sudipti.gupta@oracle.com", "Exit Code: " + code,
//                          "Seed validation run on txn " + txn);
            System.exit(code);  
        }catch (Exception e) { e.printStackTrace();}
    }
    
    public static String pollPatchStatus(String patchId) throws Exception{
                
        ProcessExecutor pe = new ProcessExecutor();
        String cmd_job_status = "fpr -r jobstatus -j " + patchId;
        ProcessBuilder pb = new ProcessBuilder("bash","-c",cmd_job_status);
        String status = "UNKNOWN";        
        for(int i=0; i<15; i++) {
            
            Thread.sleep(120000); //sleep for 2 mins
            pe.runProcess(pb, "job_status.txt", false); //never append here
            String fileContents = FileParserHelper.getFileContentsAsString("job_status.txt");
            //System.out.println(fileContents);
            Matcher m = pPatchStatus.matcher(fileContents);
          
            if(m.find())
                status = m.group(1);
            if(status.contains("FAILED") || status.contains("PASSED")) {
                pb = new ProcessBuilder("bash","-c","fpr -r showjobfailures -j " + patchId);
                pe.runProcess(pb, "job_status.txt", true); //append job failures
                
                pb = new ProcessBuilder("bash","-c","fpr -r getpatchinglogs -j " + patchId);
                pe.runProcess(pb, "job_status.txt", true); //append patch logs
                return status;
            }
            if(status.contains("Logfile missing"))
                return status;
            
            System.out.println("\n" + (new Date()).toString() + " " + "Job status is: " + status);
            if(i%3 ==0) {
                checkQueueStatus();
            }
        }   
        return status;
    }
    
    public static String parseJobId() throws Exception{
        
        String fileContents = FileParserHelper.getFileContentsAsString("raw_seedchecksum.txt");
        if(fileContents.contains("ZERO_PATCH_FILES"))
            return "0";
        if(fileContents.contains("SOURCE_TRANS_NOT_FOUND"))
            return "-1";
        if(fileContents.contains("USER_NOT_ALLOWED"))
            return "-2";
        if(fileContents.contains("ENV_DOES_NOT_EXIST"))
            return "-3";
        Matcher m = pPatchId.matcher(fileContents);
        if(m.find())
            return m.group(1);
        else return null;       
    }
    
   /* public static void queueUp() throws Exception{
        ProcessExecutor pe = new ProcessExecutor();
        String cmd_queue = "fpr -e " + env + " -r queueup";
        ProcessBuilder pb = new ProcessBuilder("bash","-c",cmd_queue);
        pe.runProcess(pb, "queue_status.txt", false);
        
        String fileContents = FileParserHelper.getFileContentsAsString("queue_status.txt");
        if(fileContents.contains("QUEUE_UP_FAILURE")) {
            System.out.println("Queue is down and cannot be brought up.");
            System.exit(2);
        }
        if(fileContents.contains("USER_NOT_ALLOWED")) {
            System.out.println("You don't have admin rights to bring the queue up.");
            System.exit(2);
        }
    }*/
    
    public static void checkQueueStatus() throws Exception{
        ProcessExecutor pe = new ProcessExecutor();
        String cmd_queue = "fpr -e " + env + " -r queuestatus";
        ProcessBuilder pb = new ProcessBuilder("bash","-c",cmd_queue);
        pe.runProcess(pb, "queue_status.txt", true);
        
        String fileContents = FileParserHelper.getFileContentsAsString("queue_status.txt");
        if(fileContents.contains("Environment Queue Status : InActive")) {
            System.out.println("\n" + (new Date()).toString() + " " + "Queue is currently down. Cannot patch. It is advisable that you submit a patch " +
                "request later when queue comes up before merging seed data changes.");
            writer.write("\n" + (new Date()).toString() + "\nQueue is currently down. Cannot patch. It is advisable that you submit a patch " +
                "request later when queue comes up before merging seed data changes.");
            exitSystem(2);
        }
    }
    private static void parseEnvSid(String fileName) {
        System.out.println("\n" + (new Date()).toString() + " " + "Reading dbSid from config file...");
        try{
            String line = "";
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
           
            while((line = reader.readLine()) != null) {
                
                if(line.trim().startsWith("#"))
                    continue; //ignore comments
                if(line.trim().equals(""))
                    continue;
                String[] parts = line.split(":");
                if(parts.length < 2){
                    System.out.println("Bad file format");
                    continue;
                }
                 String series = parts[0].trim();
                 String envSid = parts[1].trim();
                if(viewlabel.contains(series)){
                    env = envSid;
                    return;
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }                                
    }
}
