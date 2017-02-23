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
import oracle.apps.helpers.ProcessExecutor;

public class PsuedoTranslation {
    
    static String txn = null;
    static String env = "";
    static Pattern pTranslatedFiles = Pattern.compile("Download translated files.*?:\\s*(.*?\\.zip)");
    static Pattern pPatchId = Pattern.compile("Patch ID allocated as \\[(\\d+)\\]");
    static Pattern pPatchStatus = Pattern.compile("Patch Status\\s*\\:(.*)"); 
    static BufferedWriter writer;
       
    public static void main(String[] args) throws Exception{
        writer = new BufferedWriter(new FileWriter("bugOutputForPseudoTranslation.java.txt"));     
        txn = args[0].trim();
        System.out.println((new Date()).toString() + " " + "Transaction: " + txn); 
        //String[] files = args[1].trim().split("\n"); 
        String ade_view_root = args[2].trim();          
        if(args.length>3)
           env= args[3].trim();
        String filelistpath=args[1].trim();
        String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);  
       
        
        String userHome = System.getProperty("user.home");
        
           
        writer.write("\n" + (new Date()).toString() + " " + "Starting java code..");
        
        
        boolean seedXlfFound = false;
        for(int i = 0; i < files.length; i++) {
            if(files[i].endsWith(".xlf") && files[i].contains("db/data")){
                seedXlfFound = true;
                break;
            }
        }
        if(!seedXlfFound){
            writer.write("\n" + (new Date()).toString() + " " + "Trx does not contain any seed xlf files. There is no need to perform psuedo translation.");
            exitSystem(2);
        }
        
        if(env.equals("")) {
            writer.write("FA_PREMERGE_DB branch property not set for this env. Skipping pseudo translation...");
            exitSystem(2);
        }
        
        ProcessExecutor pe = new ProcessExecutor();   
        
        System.out.println((new Date()).toString() + " " + "Invoking ARU API to psuedotranslate files:");
        String cmd_invoke = "/usr/local/packages/isd/bin/nlstranscli pseudotrans -type transaction -name " + txn;
        
        ProcessBuilder pb = new ProcessBuilder("bash","-c",cmd_invoke);
        pe.runProcess(pb, "pseudotranslation.txt", false);
        
        String fileContents = FileParserHelper.getFileContentsAsString("pseudotranslation.txt");
        if(fileContents.contains("No translatable files found")){
            writer.write("\n" + (new Date()).toString() + " " + "\n\nTransaction has no translatable files.");
            exitSystem(2);
        }  
        if(fileContents.contains("Pseudo translation failed for some files")){
            writer.write("\n" + (new Date()).toString() + " " + "\n\nPseudo translation failed for some files. Please see pseudotranslation.txt for more details.");
            exitSystem(1);
        }        
        if(fileContents.contains("Pseudo translation failed")){
            writer.write("\n" + (new Date()).toString() + " " + "\n\nPseudo translation failed. Please make sure your transaction is saved before you try to invoke premerge." +
                "Please review pseudotranslation.txt for more details.");
            exitSystem(1);
        }
        //Create the manifest
        
        Matcher m = pTranslatedFiles.matcher(fileContents);
        if(m.find()){
            String zipfileloc = m.group(1);
            String[] parts = zipfileloc.trim().split("/");
            String zipfilename = parts[parts.length-1].trim();
            
            pb = new ProcessBuilder("bash","-c","rm -rf " + userHome + "/FPR_SEED_LANG/");
            pe.runProcess(pb, "pseudotranslation.txt", true);
            
            pb = new ProcessBuilder("bash","-c","cp " + zipfileloc +  " " + userHome + "/");
            pe.runProcess(pb, "pseudotranslation.txt", true);
            
            pb = new ProcessBuilder("bash","-c","unzip " + userHome + "/" + zipfilename + " -d " + userHome + "/FPR_SEED_LANG/");
            pe.runProcess(pb, "pseudotranslation.txt", true);
            
            pb = new ProcessBuilder("bash","-c","mv " + userHome + "/FPR_SEED_LANG/translation/ko " + userHome + "/FPR_SEED_LANG/translation/fusionapps");
            pe.runProcess(pb, "pseudotranslation.txt", true);
            
            pb = new ProcessBuilder("bash","-c","find " + userHome + "/FPR_SEED_LANG/translation/fusionapps -name \"*.xlf\" -print | grep db/data > fprmanifest.txt");
            pe.runProcess(pb, "pseudotranslation.txt", true);
            
        }else{
            writer.write("\n" + (new Date()).toString() + " " + "\n\nCould not find translated files. Please see pseudotranslation.txt for more details.");
            exitSystem(1);
        }
        
        File f = new File("fprmanifest.txt");
        if(!f.exists()){
            writer.write("\n" + (new Date()).toString() + " " + "\n\nCould not create patch manifest.");
            exitSystem(1);
        }
        else {
            BufferedWriter writer1 = new BufferedWriter(new FileWriter("fprmanifest1.txt"));
            BufferedReader reader = new BufferedReader(new FileReader("fprmanifest.txt"));
            String line = "";
            while((line = reader.readLine()) != null){
                int index = line.indexOf("fusionapps/");
                if(index != -1)
                    writer1.write(line.substring(index)+"\n");
            }                
            writer1.close();
            reader.close();
        }
        
        String seedjobid = PremergeSeedChecksum.parseJobId();
        System.out.println((new Date()).toString() + " " + "Seed Job ID:" + seedjobid);
        int cnt = 0;
        //wait for a amx 10 mins to ensure US file upload was kicked off...
        while(seedjobid == null){
            Thread.sleep(120000); //sleep for 2 mins
            cnt++;
            if(cnt >= 5)
                break;
            seedjobid = PremergeSeedChecksum.parseJobId();
            System.out.println((new Date()).toString() + " " + "Seed Job ID:" + seedjobid);
        }
        
        
        System.out.println((new Date()).toString() + " " + "Invoking fpr scheduler:");
        
        cmd_invoke = "fpr -e " + env + " -s " + userHome+"/FPR_SEED_LANG/translation/fusionapps/ -p V_XLF_LANGUAGE=NO V_FILETYPES=xlf V_CONT_EQUIV=NO -f F_EVERYTHING -m fprmanifest1.txt";
        System.out.println(cmd_invoke);
        pb = new ProcessBuilder("bash","-c",cmd_invoke);
        pe.runProcess(pb, "pseudotranslation.txt", true);
        
        String jobid = parseJobId();
        System.out.println((new Date()).toString() + " " + "Patch job id is: " + jobid);
        
        if(jobid == null) { 
            writer.write("\n" + (new Date()).toString() + " " + "\n\nNo patch id was allocated by fpr scheduler. This should not happen. Please validate your txn manually.");
            exitSystem(1); 
        }
        if(jobid.equals("0")) {
            writer.write("\n" + (new Date()).toString() + " " + "\n\nTransaction has no files to patch.");
            exitSystem(2);
        }
        if(jobid.equals("-1")) {
            writer.write("\n" + (new Date()).toString() + " " + "\n\nSource Transaction not found.");
            exitSystem(1);
        }
        if(jobid.equals("-2")) {
            writer.write("\n" + (new Date()).toString() + " " + "\n\nUser is not allowed to patch the environment. Please contact premerge DB administrators to get permission to patch.");
            exitSystem(1);
        }
        if(jobid.equals("-3")) {
            writer.write("\n" + (new Date()).toString() + " " + "\n\nEnv does not exist. Premerge cannot be used to perform Seed validation.");
            exitSystem(1);
        }
        
        printUrl(jobid);
        String jobStatus = pollPatchStatus(jobid);
        System.out.println((new Date()).toString() + " " + "Final Job status is: " + jobStatus);
        if(jobStatus.contains("PASSED (WITH WARNINGS")) {
            writer.write("\n" + (new Date()).toString() + " " + "\n\nPatching completed successfuly. Some requested files did not load on thier first attempt " +
                "but were successful on subsequent tries. This may indicate an unspecified file dependency relationship in " +
                "your transaction files which should be investigated and resolved. See the detailed status in $ADE_VIEW_ROOT/fusionapps/premerge/job_status_xlf.txt");
            exitSystem(0); //exit with SUCCESS
        }
        else if(jobStatus.contains("PASSED")) {
            writer.write("\n" + (new Date()).toString() + " " + "\n\nPatching completed successfuly. See the detailed status in $ADE_VIEW_ROOT/fusionapps/premerge/job_status_xlf.txt");
            exitSystem(0); //exit with SUCCESS
        }
        else if(jobStatus.contains("FAILED")) {
            writer.write((new Date()).toString() + " " + "\n\nPatching had some failures. See the detailed status in $ADE_VIEW_ROOT/fusionapps/premerge/job_status_xlf.txt");
            exitSystem(1); //exit with FAILURE
        }
        else if(jobStatus.contains("UNKNOWN")) {
            writer.write("\n" + (new Date()).toString() + " " + "\n\nPatching status is UNKNOWN. Premerge is timing out as it could not get completed patching status in " +
                "30 mins. Seed data patching could not be tested. Developers are advised to complete the patch testing before merging any seed data changes.");
            exitSystem(3); //exit with failure
        }
        else if(jobStatus.contains("Logfile missing")) {
            writer.write((new Date()).toString() + " " + "\n\nPatching status cannot be ascertained as Logfile is missing. Please review the status of the patch manually before merging code.");
            exitSystem(3); //exit with WARNING
        }
        writer.close();
    }
    
    static void printUrl(String jobid) throws Exception{
        if(jobid !=null){
            writer.write("\n" + (new Date()).toString() + " " + "\n\nYou can go to http://fre.us.oracle.com:7777/cgi-bin/fprScheduler?scope=env&req=job&sid="+env+"&jobid="+jobid
                          + " to view the full patch details.");
        }         
    }
    
    static void exitSystem(int code) {
        
        try{
            writer.close();
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
            pe.runProcess(pb, "job_status_xlf.txt", false); //never append here
            String fileContents = FileParserHelper.getFileContentsAsString("job_status_xlf.txt");
            //System.out.println(fileContents);
            Matcher m = pPatchStatus.matcher(fileContents);
          
            if(m.find())
                status = m.group(1);
            if(status.contains("FAILED") || status.contains("PASSED")) {
                pb = new ProcessBuilder("bash","-c","fpr -r showjobfailures -j " + patchId);
                pe.runProcess(pb, "job_status_xlf.txt", true); //append job failures
                
                pb = new ProcessBuilder("bash","-c","fpr -r getpatchinglogs -j " + patchId);
                pe.runProcess(pb, "job_status_xlf.txt", true); //append patch logs
                return status;
            }
            if(status.contains("Logfile missing"))
                return status;
            
            System.out.println((new Date()).toString() + " " + "Job status is: " + status);
            if(i%3 ==0) {
                checkQueueStatus();
            }
        }   
        return status;
    }
    
    public static String parseJobId() throws Exception{
        
        String fileContents = FileParserHelper.getFileContentsAsString("pseudotranslation.txt");
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
    
    public static void checkQueueStatus() throws Exception{
        ProcessExecutor pe = new ProcessExecutor();
        String cmd_queue = "fpr -e " + env + " -r queuestatus";
        ProcessBuilder pb = new ProcessBuilder("bash","-c",cmd_queue);
        pe.runProcess(pb, "queue_status.txt", true);
        
        String fileContents = FileParserHelper.getFileContentsAsString("queue_status.txt");
        if(fileContents.contains("Environment Queue Status : InActive")) {
            writer.write((new Date()).toString() + " " + "\nQueue is currently down. Cannot patch. It is advisable that you submit a patch " +
                "request later when queue comes up before merging seed data changes.");
            exitSystem(1);
        }
    }
}
