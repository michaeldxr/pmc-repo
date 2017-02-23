package oracle.apps.fixer;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.FileParserHelper;
import oracle.apps.helpers.Mail;
import oracle.apps.helpers.ProcessExecutor;

public class BuildExternalReferences {
    
    static HashMap<String,HashSet<String>> externalReferences = new HashMap<String,HashSet<String>>(); //classname,methodName -> potentially referencing class
    
    Pattern classPattern = Pattern.compile("(?:(?:public|private|protected|static|final|native|synchronized|abstract|threadsafe|transient)\\s+)*class\\s+([^ ]+)\\s+extends[^{]+\\{");
    Pattern accessorPattern = Pattern.compile("([^ ]+EOImpl|[^ ]+RowImpl)\\.(get\\w+|set\\w+)");
    static String codeLine = "PREARU";
    static BufferedWriter writer;

    public static void main(String[] args) throws Exception{
        
        if(args.length>0)
            codeLine=args[0].trim();
        
        Date startDate = new Date();
        
        String sCrawlDir1 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/fin";
        String sCrawlDir2 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/hcm";
        String sCrawlDir3 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/scm";
        String sCrawlDir4 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/crm";
        String sCrawlDir5 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/prc";
        String sCrawlDir6 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/prj";
        String sCrawlDir7 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/ic";
        String sCrawlDir8 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/atf";
        String sCrawlDir9 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps";
        if(codeLine.equals("V2")) {
            sCrawlDir1 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/fin";
            sCrawlDir2 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/hcm";
            sCrawlDir3 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/scm";
            sCrawlDir4 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/crm";
             sCrawlDir5 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/prc";
             sCrawlDir6 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/prj";
             sCrawlDir7 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/ic";
             sCrawlDir8 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps/atf";
             sCrawlDir9 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP4DEV_LINUX.X64") + "/fusionapps";
        }
        else if(codeLine.equals("PREARU")) {
            sCrawlDir1 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.RUP3PREARU_LINUX.X64") + "/fusionapps/fin";
            sCrawlDir2 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.RUP3PREARU_LINUX.X64") + "/fusionapps/hcm";
            sCrawlDir3 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.RUP3PREARU_LINUX.X64") + "/fusionapps/scm";
            sCrawlDir4 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.RUP3PREARU_LINUX.X64") + "/fusionapps/crm";
            sCrawlDir5 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.RUP3PREARU_LINUX.X64") + "/fusionapps/prc";
            sCrawlDir6 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.RUP3PREARU_LINUX.X64") + "/fusionapps/prj";
            sCrawlDir7 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.RUP3PREARU_LINUX.X64") + "/fusionapps/ic";
            sCrawlDir8 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.RUP3PREARU_LINUX.X64") + "/fusionapps/atf";
            sCrawlDir9 = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.RUP3PREARU_LINUX.X64") +  "/fusionapps";
        }
        BuildExternalReferences buildRef = new BuildExternalReferences();
        
        writer = new BufferedWriter(new FileWriter("errors.txt"));
        
        System.out.println("Building external references for: " + sCrawlDir1);
        buildRef.checkForExternalReferences(sCrawlDir1,"FIN");
        buildRef.writeExternalReferencesToDB("FIN");
        externalReferences = new HashMap<String,HashSet<String>>();
        
        System.out.println("Building external references for: " + sCrawlDir2);
        buildRef.checkForExternalReferences(sCrawlDir2,"HCM");
        buildRef.writeExternalReferencesToDB("HCM");
        externalReferences = new HashMap<String,HashSet<String>>();
        
        System.out.println("Building external references for: " + sCrawlDir3);
        buildRef.checkForExternalReferences(sCrawlDir3,"SCM");
        buildRef.writeExternalReferencesToDB("SCM");
        externalReferences = new HashMap<String,HashSet<String>>(); 
        
        System.out.println("Building external references for: " + sCrawlDir4);
        buildRef.checkForExternalReferences(sCrawlDir4,"CRM");
        buildRef.writeExternalReferencesToDB("CRM");
        externalReferences = new HashMap<String,HashSet<String>>();
        
        System.out.println("Building external references for: " + sCrawlDir5);
        buildRef.checkForExternalReferences(sCrawlDir5,"PRC");
        buildRef.writeExternalReferencesToDB("PRC");
        externalReferences = new HashMap<String,HashSet<String>>();
        
        System.out.println("Building external references for: " + sCrawlDir6);
        buildRef.checkForExternalReferences(sCrawlDir6,"PRJ");
        buildRef.writeExternalReferencesToDB("PRJ");
        externalReferences = new HashMap<String,HashSet<String>>();
        
        System.out.println("Building external references for: " + sCrawlDir7);
        buildRef.checkForExternalReferences(sCrawlDir7,"IC");
        buildRef.writeExternalReferencesToDB("IC");
        externalReferences = new HashMap<String,HashSet<String>>();
        
        System.out.println("Building external references for: " + sCrawlDir8);
        buildRef.checkForExternalReferences(sCrawlDir8,"ATF");
        buildRef.writeExternalReferencesToDB("ATF");
        externalReferences = new HashMap<String,HashSet<String>>();
        
        System.out.println("Building external references for: " + sCrawlDir9);
        buildRef.checkForExternalReferences(sCrawlDir9,"ALL");
        buildRef.writeExternalReferencesToDB("ALL");
        
        Date endDate = new Date();
        
        writer.write("\n\nStarted at: " + startDate);
        writer.write("\nFinished at: " + endDate);
        
        writer.close();
    }
   
    private void checkForExternalReferences(String sCrawlDir, String family) throws Exception{        
        
        if(sCrawlDir.contains(".ade_path") || sCrawlDir.contains("/classes/") 
           || sCrawlDir.contains("/src/") || sCrawlDir.contains("/adfmsrc/") 
           || sCrawlDir.contains("/public_html/") || sCrawlDir.contains("/WEB-INF/")
           || sCrawlDir.contains("/dbSchema/"))
            return;        
        if(family.equals("ALL") && (sCrawlDir.contains("fusionapps/fin") || sCrawlDir.contains("fusionapps/hcm") || 
                                    sCrawlDir.contains("fusionapps/scm") || sCrawlDir.contains("fusionapps/crm")
                                    || sCrawlDir.contains("fusionapps/ic")  || sCrawlDir.contains("fusionapps/prc")
                                    || sCrawlDir.contains("fusionapps/prj") || sCrawlDir.contains("fusionapps/atf")))
            return;
        

        File folder = new File(sCrawlDir);
        File[] listOfFiles = folder.listFiles();
            
        if(listOfFiles == null)
            return;
        
        for(int i = 0; i <listOfFiles.length; i++)
        {
            if(listOfFiles[i].isFile()) 
            {
                String sName = listOfFiles[i].getAbsolutePath();
                if(sName.contains(".ade_path"))
                    continue;
                
                if(sName.endsWith(".jar"))                 
                    findExternalReferences(listOfFiles[i]);               
            }
            else if(listOfFiles[i].isDirectory()) {               
               checkForExternalReferences(listOfFiles[i].getAbsolutePath(), family);
            }
            else {
                System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);
            }
        }
    }
    
    private void findExternalReferences(File f)  throws Exception{
        
        System.out.println("Processing: " + f.getName());
        
        String bytecode = getByteCode(f.getAbsolutePath());
        Matcher m = classPattern.matcher(bytecode);
       
        int prevIndex = 0;
        int nextIndex = 0;
        String prevRefClass = "";
       
        while(m.find()) {
            
            String refClass = m.group(1);
            
            nextIndex = m.end();
        
            String classCode = bytecode.substring(prevIndex,nextIndex);
            prevIndex = nextIndex;
            
            if(prevRefClass.contains("AttributesEnum")) {
                prevRefClass = refClass;              
                continue;
            }
            
            Matcher m1 = accessorPattern.matcher(classCode);
            
            
            while(m1.find()) {
                
                String implClass = m1.group(1).replace("/", ".");
                String method = m1.group(2);            
                
                if(prevRefClass.equals(implClass)) //avoid putting internal references
                    continue; 
                HashSet<String> refClasses = new HashSet<String>();
                    
                if(externalReferences.containsKey(implClass+","+method))
                    refClasses = externalReferences.get(implClass+","+method);
                refClasses.add(prevRefClass);
                
                externalReferences.put(implClass+","+method,refClasses);
            }
                       
            prevRefClass = refClass;

        }
        //Process the last remaining class
        if(!prevRefClass.contains("AttributeEnum")) {
            String classCode = bytecode.substring(prevIndex);
            Matcher m1 = accessorPattern.matcher(classCode);        
            
            while(m1.find()) {
                
                String implClass = m1.group(1).replace("/", ".");
                String method = m1.group(2); 
                
                if(prevRefClass.equals(implClass)) //avoid putting internal references
                    continue; 
                
                HashSet<String> refClasses = new HashSet<String>();
                    
                if(externalReferences.containsKey(implClass+","+method))
                    refClasses = externalReferences.get(implClass+","+method);
                refClasses.add(prevRefClass);
                
                externalReferences.put(implClass+","+method,refClasses);
            }
        }
    }    
    
    private String getByteCode(String filePath) throws Exception{
       
      try{

           String javapCommand = "javap -classpath " + filePath + " -c -private -s $(jar -tf " +  
                                 filePath + " | grep class | sed 's/\\.class//g')";
           ProcessBuilder pb = new ProcessBuilder("bash", "-c", javapCommand);
           ProcessExecutor pe = new ProcessExecutor();
           pe.runProcess(pb, "bytecode.txt", false);
          
           return FileParserHelper.getFileContentsAsString("bytecode.txt");

      } catch( Exception e) {
                e.printStackTrace(); 
                writer.write(e.getMessage() + "\n\n");
                return "";
      }
    }

    private void writeExternalReferencesToDB(String family) throws Exception{
        try{
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(externalReferences);
            oos.close();
            
            FileOutputStream fos = new FileOutputStream("references.txt."+family);
            ObjectOutputStream oos1 = new ObjectOutputStream(fos);
            oos1.writeObject(externalReferences);
            oos1.close();
            
            System.out.println("Writing Serialized HashMap to database...");
            Connection con = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","fintech","fintech");
            con.setAutoCommit(false);
            Statement st = con.createStatement();
            st.executeUpdate("DELETE FROM SERIALIZEDEXTERNALREFERENCES WHERE CODELINE ='"+codeLine+"' AND FAMILY ='" + family + "'");
            st.close();
            PreparedStatement ps = con.prepareStatement("INSERT INTO SERIALIZEDEXTERNALREFERENCES(REFERENCESMAP,CODELINE,FAMILY) values (?,?,?)");
            
            
            ps.setBytes(1,baos.toByteArray());
            ps.setString(2,codeLine);
            ps.setString(3,family);
            ps.execute();
            ps.close();
            con.commit();
            con.close();
            
        } catch(Exception e) {
            e.printStackTrace();
            Mail.sendMail("sudipti.gupta@oracle.com", e.getMessage(),
                          "Could not write external Enum References Serialized Map. Please review and try again.");
            writer.write(e.getMessage() + "\n\n");
        }
    }
}
