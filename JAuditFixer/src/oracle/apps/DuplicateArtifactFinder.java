package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;

import java.io.InputStream;

import java.security.DigestInputStream;
import java.security.MessageDigest;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.sql.Types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import java.util.Iterator;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.ProcessExecutor;


public class DuplicateArtifactFinder {
    
    static BufferedWriter writer;
    static String viewRoot;
    HashMap<String,ArrayList<String>> allArtifacts = new HashMap<String,ArrayList<String>>(); 
    static HashMap<String,HashSet<String>> exemptions = new HashMap<String,HashSet<String>>();
    static String label;
    
    HashMap<String,String> ears = new HashMap<String,String>(); //ear name -> HCM/CRM/FSCM
    HashMap<String,HashSet<String>> jars = new HashMap<String,HashSet<String>>(); //jar Name -> ear(s)
    HashMap<String,String> jarTypes = new HashMap<String,String>();
    
    public static void main(String[] args) throws Exception{
        
        DuplicateArtifactFinder scanner = new DuplicateArtifactFinder();
        java.util.Date start = new java.util.Date();
        
        String series = "";
        System.out.println("Starting ArtifactDuplicates scan: " + start);
        
        if(args.length> 0 && args[0] != null && args[0].trim() != "")
            series = args[0].trim();
        
        //series = "FUSIONAPPS_PT.V2MIBERP_LINUX.X64";
        
        viewRoot = FamilyModuleHelper.getLabelServerTop1(series) + "/"+series+".rdd/LATEST/";
        
        //viewRoot = "/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/";
        
        String absPath = new File(viewRoot + ".labellog.emd.gz").getCanonicalPath();
        String[] parts = absPath.split("/");
        if(parts.length > 4)
            label = parts[4];
        
        ProcessExecutor pe = new ProcessExecutor();   
        ProcessBuilder pb = new ProcessBuilder("bash","-c","zcat " + viewRoot + 
                                                ".labellog.emd.gz | grep 'FILE fusionapps' | egrep '/src/|/adfmsrc/|/public_html/' " +
            "| grep -vi '/noship/' | cut -f2 -d ' '");
        pe.runProcess(pb, "/tmp/fileList.txt", false); 
        
        BufferedReader reader = new BufferedReader(new FileReader("/tmp/fileList.txt"));
        writer = new BufferedWriter(new FileWriter("artifactDuplicates.csv"));
        writer.write("FAMILY,MODULE, PRODUCT,FILENAME, ARTIFACT, MD5 CHECKSUM\n");        
        
        scanner.buildEarMetadata();
        
        String line = "";
        while((line = reader.readLine()) != null) {
            String javaFile = line.trim();
            scanner.processFile(javaFile);
        }
        
        reader.close();
        
        scanner.printDuplicates();
        writer.close();
        scanner.identifyFileDifferences();
        scanner.dumpToAngrybirds(series);
        
        FamilyModuleHelper.printScanDuration(start,"ArtifactDuplicates");
        //WRITE OUT EXEMPTIONS...one time excercise
      /*  writer =  new BufferedWriter(new FileWriter("exemptions.csv"));
        for(Iterator<String> it = exemptions.keySet().iterator(); it.hasNext();) {
            String artifact = it.next();
            String filename = exemptions.get(artifact);
            writer.write(getFileNameDetails(filename) + artifact + "\n");
        }
        writer.close();*/
        
    }
    
    public void buildEarMetadata() throws Exception{
        
        BufferedReader reader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
                            "/fatools/opensource/jauditFixScripts/txt/ears.txt"));
        
        String line = reader.readLine();
        String stripe = "HCM";
        while((line = reader.readLine()) != null) {
            
            if(line.contains("#CRM")) {
                stripe = "CRM";
                continue;                
            }
            if(line.contains("#FSCM")) {
                stripe = "FSCM";
                continue;                
            }
            if(line.trim().equals("") || !line.trim().endsWith("ear"))
                continue;
            ears.put(line.trim(),stripe);
        }
        reader.close();
        
        
        reader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
                                    "/fatools/opensource/jauditFixScripts/txt/jars.txt"));
        String earName = "";
        String jarName = "";
        String jarType = "";
        HashSet<String> earsToWhichJarBelongs = new HashSet<String>();
        while((line = reader.readLine()) != null) {
            
            String[] parts = line.split("/");
            if(parts.length < 3)
                continue;
            earName = parts[1];
            jarName = parts[parts.length-1];
            if(line.contains("WEB-INF"))
                jarType = "UI";
            else
                jarType = "Model";
            
           
            jarTypes.put(jarName,jarType);
                        
            if(jars.containsKey(jarName))
                earsToWhichJarBelongs = jars.get(jarName);
            else
                earsToWhichJarBelongs = new HashSet<String>();
            
            earsToWhichJarBelongs.add(earName);
            jars.put(jarName, earsToWhichJarBelongs);
        }
        reader.close();
        
        reader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
            "/fatools/opensource/jauditFixScripts/JAuditFixer/exemptions.csv"));
        while((line = reader.readLine()) != null) {
            
            String[] parts = line.split(",");
            if(parts.length < 5)
                continue;
            HashSet<String> exemptedFiles = new HashSet<String>();
            if(exemptions.containsKey(parts[4].trim()))
                exemptedFiles = exemptions.get(parts[4].trim());
            exemptedFiles.add(parts[3].trim());
            exemptions.put(parts[4].trim(),exemptedFiles);
        }
        reader.close(); 
    }
    
    private void identifyFileDifferences() throws Exception {
        
        BufferedReader reader = new BufferedReader(new FileReader("artifactDuplicates.csv"));
        String line = reader.readLine();
        
        BufferedWriter writer = new BufferedWriter(new FileWriter("artifactDuplicates1.csv"));
        writer.write("FAMILY,MODULE, PRODUCT,FILENAME, ARTIFACT, MD5 CHECKSUM, JPR, JAR, JAR TYPE, EAR(old), EAR(new)\n");
        
        HashMap<String,HashSet<String>> artifactChecksums = new HashMap<String,HashSet<String>>();
        
        while((line = reader.readLine()) != null){
            
            String[] parts = line.split(",");
            if(parts.length < 5)
                continue;
            String artifactName = parts[4].trim();
            String checksum = "";
            if(parts.length >5)
                checksum = parts[5].trim();
            
            HashSet<String> checksums = new HashSet<String>();
            if(artifactChecksums.containsKey(artifactName))
                checksums = artifactChecksums.get(artifactName);
            
            checksums.add(checksum);
            artifactChecksums.put(artifactName,checksums);
        }
        
        reader.close();
        
        reader = new BufferedReader(new FileReader("artifactDuplicates.csv"));
        line = reader.readLine();
        while((line = reader.readLine()) != null){
            
            String[] parts = line.split(",");
            if(parts.length < 5)
                continue;
            String artifactName = parts[4].trim();
            String fileName = parts[3].trim();
            String identical = "identical";
            HashSet<String> checksums = artifactChecksums.get(artifactName);
            if(checksums != null && checksums.size() > 1)
                identical = "different";                
            
            writer.write(line.trim() +" : " + identical + "," + getProjectDetails(fileName) + "\n");            
        }    
        
        writer.close();
        reader.close();
        
        writer = new BufferedWriter(new FileWriter("artifactDuplicates2.csv"));
        writer.write("FAMILY,MODULE, PRODUCT,FILENAME, ARTIFACT, MD5 CHECKSUM, JPR, JAR, JAR TYPE, EAR(old), EAR(new), Phase\n");
        
        reader = new BufferedReader(new FileReader("artifactDuplicates1.csv"));
        line = reader.readLine();
        HashMap<String,ArrayList<String>> artifactEars = new HashMap<String,ArrayList<String>>();
        
        while((line = reader.readLine()) != null){
            
            String[] parts = line.split(",");
            if(parts.length < 11)
                continue;
            String artifactName = parts[4].trim();
            String ear = parts[10].trim();
            ArrayList<String> newEars = new ArrayList<String>();
            
            if(artifactEars.containsKey(artifactName))
                newEars = artifactEars.get(artifactName);
            
            if(!ear.equals(""))
                newEars.add(ear);
            artifactEars.put(artifactName,newEars);
            
        }
        reader.close();
        
        reader = new BufferedReader(new FileReader("artifactDuplicates1.csv"));
        line = reader.readLine();
        
        while((line = reader.readLine()) != null){
            String[] parts = line.split(",");
            String artifactName = parts[4].trim();
            
            if(exemptions.containsKey(artifactName) && exemptions.get(artifactName).contains(parts[3].trim()))
               continue;     
            
            ArrayList<String> ears = artifactEars.get(artifactName);
            String jarType = parts[8].trim();
            
            int hcm = 0;
            int crm = 0;
            int fscm = 0;
            String phase = " ";
            if(ears.size() <=1) //If there are 2 conflicts and one of the artifacts (or both) doesnt even get packaged in an EAR, exlcude these as there will be no conflict
                continue;
            for(int i =0; i < ears.size(); i++){
                if(ears.get(i).contains("HCM"))
                    hcm++;
                if(ears.get(i).contains("CRM"))
                    crm++;
               if(ears.get(i).contains("FSCM"))
                    fscm++;
            }
            if(hcm ==0 && fscm ==0 && crm ==0)
                phase = " ";
            else if( hcm >1 || fscm > 1 || crm > 1)
                phase = "Phase 1";
            else
                phase = "Phase 2";
            
            if(jarType.contains("Model") && phase.equals("Phase 2"))
                phase = "Phase 1.5";
            
            writer.write(line + "," + phase + "\n");
            
        }
        writer.close();
        reader.close();
        
    }
    
    private String getProjectDetails(String fileName) {
        
        String jprName = "";
        ArrayList<String> jarNames = new ArrayList<String>();
        ArrayList<String> earNames = new ArrayList<String>();
        HashSet<String> newEars = new HashSet<String>();
        int index = -1;
        
        if(fileName.contains("/src/"))
            index = fileName.indexOf("/src/");
        else if(fileName.contains("/adfmsrc/"))
            index = fileName.indexOf("/adfmsrc/");
        else if(fileName.contains("/public_html/"))
            index = fileName.indexOf("/public_html/");
        
        if(index == -1)
            return " , , , , ";
        
        File f = new File(viewRoot + fileName.substring(0, index));
        if(!f.isDirectory())
            return " , , , , ";
        
        File[] listOfFiles = f.listFiles();
        for (int i = 0; i < listOfFiles.length; i++){
            if(listOfFiles[i].getName().endsWith(".jpr")){
                jprName = listOfFiles[i].getAbsolutePath();
                break;
            }
        }
        
        if(jprName.equals(""))
            return " , , , , ";
        
        jarNames = JprHelper.getAllDeploymentProfileUrls(jprName);
        for(int i=0; i< jarNames.size(); i++) {
            String jar = jarNames.get(i);
            HashSet<String> earsToWhichJarBelongs = jars.get(jar);
            if(earsToWhichJarBelongs !=null)
            earNames.addAll(earsToWhichJarBelongs);
        }
        
        for(int i=0; i< earNames.size(); i++) {
            String ear = earNames.get(i);
            String newEar = ears.get(ear);
            if(newEar != null)
                newEars.add(newEar);
        }
        
        String details = FamilyModuleHelper.getPathAfterViewRoot(jprName) + " ,";
        
        for(int i = 0; i < jarNames.size(); i++)
            details += jarNames.get(i) + " ";        
        details += " , ";
        
        for(int i = 0; i < jarNames.size(); i++){
            String jarType = jarTypes.get(jarNames.get(i));
            if(jarType != null)
                details += jarTypes.get(jarNames.get(i)) + " ";   
        }
        details += " , ";
        
        for(int i = 0; i < earNames.size(); i++)
            details += earNames.get(i) + " ";        
        details += " , ";
        
        for(Iterator<String> it = newEars.iterator(); it.hasNext();)
            details += it.next() + " ";
        
        return details;
    }
    
    
    private void dumpToAngrybirds(String series) throws Exception {
        
         Connection con =
          DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
        con.setAutoCommit(false);
        
        CallableStatement cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
        cstmt.registerOutParameter(1, Types.INTEGER);   
        
        PreparedStatement delete =
          con.prepareStatement("DELETE FROM codescan_results where issuetype = 'ArtifactDuplicates' and series='" + series + "'");
        delete.execute();   
        
        PreparedStatement pstmt = con.prepareStatement("INSERT INTO DUPLICATE_ARTIFACTS(PARENT_ID,JPR,JARS,JAR_TYPE," +
              "OLD_EARS,NEW_EARS,PHASE) VALUES(?,?,?,?,?,?,?)");
        
        BufferedReader reader = new BufferedReader(new FileReader("artifactDuplicates2.csv"));
        String line = reader.readLine();
        
        while((line = reader.readLine()) != null) {
            
            String[] parts = line.split(",");
            if(parts.length< 12){
                System.out.println(line);
                continue;
            }
            
            String artifact = parts[4].trim();
            if(artifact.equals("hwtaskflow.xml"))
                continue;
 
            cstmt.setString(2,parts[0].trim());
            cstmt.setString(3,parts[1].trim());
            cstmt.setString(4,parts[2].trim());
            cstmt.setString(5,parts[3].trim());
            cstmt.setString(6,"ArtifactDuplicates");
            cstmt.setString(7,artifact); //sub issue - duplicate artifact
            cstmt.setString(8,parts[5].trim());
            cstmt.setString(9,series);
            cstmt.setString(10,label);
            
            cstmt.execute();              
            int seq = cstmt.getInt(1);
            
            pstmt.setInt(1,seq); 
            pstmt.setString(2,parts[6].trim());
            pstmt.setString(3,parts[7].trim());
            pstmt.setString(4,parts[8].trim());
            pstmt.setString(5,parts[9].trim());
            pstmt.setString(6,parts[10].trim());
            pstmt.setString(7,parts[11].trim());
            pstmt.execute();
        }
        cstmt.close();
        pstmt.close();
        con.commit();
        con.close();  
    }
    
    private void printDuplicates() throws Exception{
        
       // Random randomGenerator = new Random(); 
        
        for(Iterator<String> it = allArtifacts.keySet().iterator(); it.hasNext();) {
            
            String artifact = it.next();
            ArrayList<String> srcFiles = allArtifacts.get(artifact);
            int numConflicts = srcFiles.size();
            //int exempted = -1;
            
            if(numConflicts <= 1) 
                continue;
            if (numConflicts >=10)
                System.out.println(artifact + " : " + numConflicts);
            /*else{
                exempted = getExemptedNumber(srcFiles,randomGenerator);
                
                if(srcFiles.get(exempted).endsWith(".png") 
                   && !srcFiles.get(exempted).contains("fusionapps/com/components/Images/publicUi"))
                    System.out.println("do not exempt: " + srcFiles.get(exempted));
                else
                    exemptions.put(artifact,srcFiles.get(exempted));
            } */
            
            for(int i =0; i < srcFiles.size(); i++){
                String fileName = srcFiles.get(i);
                writer.write(getFileNameDetails(fileName) + artifact+ "," + getMD5CheckSum(fileName) + "\n");
            }
        }        
    }
    
    /*private int getExemptedNumber(ArrayList<String> srcFiles, Random randomGenerator) {
        
        int numConflicts = srcFiles.size();
        int exempted = randomGenerator.nextInt(numConflicts);
        
        for(int i =0; i< numConflicts; i++) {
            
            String f = srcFiles.get(i);
            if(f.endsWith(".png") && f.contains("fusionapps/com/components/Images/publicUi"))
                return i;
            
            if(!f.endsWith(".png") && (f.contains("fusionapps/com/components") 
               || f.contains("fusionapps/atf/components") 
               || f.contains("fusionapps/com/components")))
                return i;
            
            if(f.endsWith(".java") && f.contains("grcSemSearch"))
                return i;
        }
        
        return exempted;
    }*/
    
    private String getMD5CheckSum(String fileName) {
        try{
            
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            InputStream fis = new FileInputStream(viewRoot + fileName);
            byte[] buffer = new byte[1024];
            DigestInputStream dis = new DigestInputStream(fis, md5);
            
            while(dis.available() > 0)
                dis.read(buffer);
                       
            byte[] digest = dis.getMessageDigest().digest();
            fis.close();
            
            StringBuffer hexString = new StringBuffer();
            
            for (int i=0;i<digest.length;i++) 
                hexString.append(Integer.toHexString(0xFF & digest[i]));
                
            return hexString.toString();
        } 
        catch(FileNotFoundException ex) {
            return "";
        }
        catch(Exception e){
            e.printStackTrace();
            return "";
        }
    }
    
    private static String getFileNameDetails(String fileName) {
        String family = "";
        String product = "";
        String module = "";
        
        String[] parts = fileName.split("/");
        if(parts.length > 1) family = parts[1].trim().toUpperCase();
        if(parts.length > 3) module = parts[3].trim().toUpperCase();
        if(parts.length > 4) product = parts[4].trim().toUpperCase();
        
        return family + ", " + module + ", " + product + ", " + fileName + ", ";
    }
    
    private void processFile(String fileName) {
        
        try{
                    
            if(fileName.contains("/META-INF/"))
                return;
            if(fileName.contains("/persdef/"))
                return;
            if(fileName.endsWith("/UpdateableCollection.xml") || fileName.endsWith("/UpdateableSingleValue.xml")
                || fileName.endsWith("/ReadOnlySingleValue.xml"))
                return;
            if(fileName.contains("WEB-INF")){
                if(fileName.endsWith("adfc-config.xml") || fileName.endsWith("trinidad-config.xml")
                    || fileName.endsWith("web.xml") || fileName.endsWith("weblogic.xml")
                    || fileName.endsWith("faces-config.xml") || fileName.endsWith("wsrp_v2_interfaces.wsdl")
                    || fileName.endsWith("portlet.xml") || fileName.endsWith("wsrp_v2_bindings.wsdl")
                    || fileName.endsWith("adfdi-client-registry.xml") || fileName.endsWith("xmlp-client-config.xml")
                    || fileName.endsWith(".adfc_diagram")) 
                    return;
            }
            
            String lowerCaseName = fileName.toLowerCase();
            if (lowerCaseName.contains("/noship/") ||
                lowerCaseName.contains("/test/") ||
                fileName.contains("Test/") ||
                lowerCaseName.contains("servicetest") ||
                lowerCaseName.contains("datasecuritytest") ||
                lowerCaseName.contains("securitypublictest") ||
                lowerCaseName.contains("publicuitest") ||
                lowerCaseName.contains("structuretest") ||
                lowerCaseName.contains("modeltest") ||
                lowerCaseName.contains("uitest") ||
                lowerCaseName.contains("testui") ||
                lowerCaseName.contains("setest") ||
                lowerCaseName.contains("testviewcontroller") ||
                lowerCaseName.contains("flextest") ||
                lowerCaseName.contains("uimodeler-launch-test") ||
                lowerCaseName.contains("publicuibasetest") ||
                lowerCaseName.contains("uipickertest"))
                return;
            
            String artifactName = getRelativeNamefromFile(fileName);
                  
            if(isEmpty(artifactName))
                return;
            
            if(artifactName.equals("login.html") || artifactName.equals("error.html") || artifactName.equals("index.jspx"))
                return;
            
            if(artifactName.equals("oracle/adf/view/rich/model/AttributeDescriptor/ComponentType.xml")||
                artifactName.equals("oracle/adf/view/rich/model/AttributeDescriptor/Operator.xml")
                || artifactName.equals("oracle/adf/view/rich/model/QueryDescriptor.xml")
                 || artifactName.equals("oracle/adf/view/rich/model/QueryDescriptor/QueryMode.xml")
                 || artifactName.equals("oracle/adf/view/rich/model/QueryModel.xml")
                || artifactName.equals("oracle/adf/view/rich/component/rich/data/RichTable.xml")
                || artifactName.equals("oracle/adf/view/rich/event/QueryEvent.xml")
                || artifactName.equals("oracle/adf/view/rich/event/QueryListener.xml")
                || artifactName.equals("oracle/adf/view/rich/model/AttributeCriterion.xml")
                ||artifactName.equals("oracle/adf/view/rich/model/AttributeDescriptor.xml")
                || artifactName.equals("oracle/adf/view/rich/model/ConjunctionCriterion.xml") 
                || artifactName.equals("oracle/adf/view/rich/model/ConjunctionCriterion/Conjunction.xml")
                || artifactName.equals("oracle/adf/view/rich/model/Criterion.xml") 
                || artifactName.equals("org/apache/myfaces/trinidad/event/SelectionEvent.xml") 
                || artifactName.equals("org/apache/myfaces/trinidad/event/SortEvent.xml")
                || artifactName.equals("org/apache/myfaces/trinidad/event/SortListener.xml")
                || artifactName.equals("org/apache/myfaces/trinidad/model/SortCriterion.xml") )
                return;
            
            if(artifactName.startsWith("com/oracle/xmlns/adf/svc/") || artifactName.startsWith("sdo/commonj/"))
                return; //Bug 24422713 - generated files
            
            ArrayList<String> srcFiles = new ArrayList<String>();
            if(allArtifacts.containsKey(artifactName)){
                srcFiles = allArtifacts.get(artifactName);
            } 
            
            srcFiles.add(fileName);
            allArtifacts.put(artifactName,srcFiles);
            
            
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    private boolean isEmpty(String name) {
        if(name == null || name.trim().equals(""))
            return true;
        return false;
    }
    private String getRelativeNamefromFile(String fileName) {
        
      
        String className = "";
        int index = fileName.indexOf("/src/");
        int offset = 5;
        if(index == -1){
            index = fileName.indexOf("/adfmsrc/");
            if(index != -1)
                offset = 9;
            else {
                index = fileName.indexOf("/public_html/");
                if(index != -1)
                    offset = 13;
            }
        }
        
        if(index != -1)
            className = fileName.substring(index+offset);
        else
            System.out.println(fileName);
        
        return className;
        
    }
    
}
