package oracle.apps.logs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;

import java.io.FileWriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FileParserHelper;

public class ExtractIncidentInfo {
    
    static BufferedWriter writer;
    Pattern pIncidentId = Pattern.compile("Incident Id\\s*:\\s*(\\d+)");  
    Pattern pServerName = Pattern.compile("DFW_SERVER_NAME\\s*:\\s*(\\w+)");
    Pattern pDomainName = Pattern.compile("DFW_DOMAIN_NAME\\s*:\\s*(\\w+)");
    
    Pattern pStartTime = Pattern.compile("^StartTime:\\s+(.*)");
    Pattern pEndTime = Pattern.compile("^EndTime:\\s+(.*)");
    Pattern pUser = Pattern.compile("^User:\\s+(.*)");
    Pattern pEcid = Pattern.compile("^ECID:\\s+(.*)");
    Pattern pUri = Pattern.compile("^URI:\\s+(.*)");
    
    public static void main(String[] args) throws Exception{
        writer = new BufferedWriter(new FileWriter("incidents.csv"));
        writer.write("IncidentId,Server,Domain,StartTime,EndTime,User,ECID, , URI\n");
        
        ExtractIncidentInfo extractor = new ExtractIncidentInfo();
        extractor.crawlDirectory(args[0].trim());
//        extractor.crawlDirectory("/net/ple65147fwks/scratch/jenchen/slow_click_test/" +
//            "slc03vkc/scratch/aime/work/APPTOP/instance/domains/slc03vkc.us.oracle.com");
//        
        writer.close();
    }
 
 
    private void crawlDirectory(String path)
    {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++)
        {
            if (listOfFiles[i].isFile())
            {
                String sName = listOfFiles[i].getAbsolutePath();
                if (sName.contains(".ade_path"))
                    continue;

                if (fileOfInterest(sName))
                    processFile(listOfFiles[i].getAbsolutePath());
                
            } else if (listOfFiles[i].isDirectory())
            {
                if (isDirOfInterest(listOfFiles[i].getAbsolutePath()))
                    crawlDirectory(listOfFiles[i].getAbsolutePath());
            } else
                System.out.println("ERROR:  node is neither file or directory: " +
                                   listOfFiles[i]);
        }
    }

    private boolean isDirOfInterest(String sName)
    {
        if (sName == null || sName.equals(""))
            return false;
        
        if(sName.contains("/fusionapps") || sName.contains("/logs"))
            return false; 

        return true;
    }
    
    public boolean fileOfInterest(String sName) 
    {        
        if (sName.contains("/incident/") && sName.endsWith("readme.txt"))
            return true;
        
        return false;
    }
    
    public void processFile(String fName) {
        try{
            BufferedReader reader = new BufferedReader(new FileReader(fName));
            String fileContents = FileParserHelper.getFileContentsAsString(fName);
            
            if(!fileContents.contains("DFW-99995"))
                return;
            
            Matcher m1 = pIncidentId.matcher(fileContents);
            Matcher m2 = pServerName.matcher(fileContents);
            Matcher m3 = pDomainName.matcher(fileContents);
            
            String incidentId = "";
            String server = "";
            String domain = "";
            String startTime = "";
            String endTime = "";
            String user = "";
            String ecid = "";
            String uri = "";
            
            if(m1.find())
                incidentId = m1.group(1);
            if(m2.find())
                server = m2.group(1);
            if(m3.find())
                domain = m3.group(1);             
            
            String line ="";
            while((line = reader.readLine()) != null) {
                
                Matcher m4 = pStartTime.matcher(line);
                Matcher m5 = pEndTime.matcher(line);
                Matcher m6 = pUser.matcher(line);
                Matcher m7 = pEcid.matcher(line);
                Matcher m8 = pUri.matcher(line);
                
                if(m4.find())
                    startTime = m4.group(1);
                if(m5.find())
                    endTime = m5.group(1);
                if(m6.find())
                    user = m6.group(1);
                if(m7.find())
                    ecid = m7.group(1);
                if(m8.find()) {
                    uri = m8.group(1);
                    writer.write(incidentId + "," + server + ","+ domain + "," + startTime 
                        + "," + endTime + "," + user + "," + ecid + "," + uri + "\n");
                }
            }
            reader.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
