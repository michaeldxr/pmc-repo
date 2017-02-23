package oracle.apps.UICodeScanner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.PreparedStatement;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JavaFileParserHelper;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.helpers.ProcessExecutor;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;
import oracle.apps.sqlanalyzer.ViewCriteriaHelper;

import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AltaIconIssues extends JoesBaseClass {
    
    static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
    static Writer outputFileWriter; 
    HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();
    String m_sSummaryReport = "";
    int uiViolationCount = 0;
    int javaViolationCount = 0;
    static HashMap<String,String> classicToAltaIcons = new HashMap<String,String>();
    static HashMap<String,String> iconsBiDiStatus = new HashMap<String,String>(); //classic: alta img --> bidi status
    static HashMap<String,String> specialClassicIcons = new HashMap<String,String>(); //classic icon --> status message to display to users
    
    static HashSet<String> r11Files = new HashSet<String>();
    //static HashSet<String> javaFilesWithPng = new HashSet<String>();
    static HashMap<String,String> origToRTLIcons = new HashMap<String,String>();
    Pattern pImage = Pattern.compile("/images/(\\w+\\.png)");
    Pattern pBiDiImage = Pattern.compile("#\\{fnd:bidiImage\\('/images/(\\w+\\.png)'\\)\\}");
    private static String mode="view"; //LRG/view/premerge mode
                                                          

    public AltaIconIssues() {
        super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF_AND_JAVA);
    }
    
    protected void processFile(File fName, boolean bDoADE) {
        
        String filePath = fName.getAbsolutePath();
        if(filePath.endsWith("jsff") || filePath.endsWith("jspx"))
            processUIFile(filePath);
        else if(filePath.endsWith("java") /*&& javaFilesWithPng.contains(filePath)*/)
            processJavaFile(filePath);
    }
    
    private void processTrxFile(String filePath) {
        if(filePath.endsWith("jsff") || filePath.endsWith("jspx"))
            processUIFile(filePath);
        else if(filePath.endsWith("java"))
            processJavaFile(filePath);
        
    }

    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return null;
    }
    
    public static void main(String[] args) throws Exception {
        
        Date start = new Date();
        
        mode = System.getProperty("mode");
        if(mode == null)
            mode = "view";
        //mode = "LRG";
        
        AltaIconIssues scanner = new AltaIconIssues();
        outputFileWriter = new BufferedWriter(new FileWriter("AltaUIViolations.csv") );
        outputFileWriter.write("Family, Module, Product, filePath, series, label, NodeName, Id, IssueType, Description, Attribute Name, Image\n");    
        
        scanner.generateExemptionList();    
        
        if(mode.equals("view")){
            
        String sCrawlDir = args[0];       
        
        boolean makeFix = false;
        if(args.length > 1)
            makeFix = new Boolean(args[1].trim());
           
         if (sCrawlDir == null || sCrawlDir.trim().equals("")) {
             System.out.println("Crawl directory has to be specified");
             System.exit(1);
         }
         
         File f = new File(sCrawlDir.trim());
         if(!f.exists()){
             System.out.println("Crawl directory does not exist - " + sCrawlDir);
             System.exit(1);
         }
                 
        System.out.println("Building script metadata: " + new java.util.Date());
        scanner.buildScriptMetadata(sCrawlDir);
        
       /* ProcessExecutor pe = new ProcessExecutor();   
        ProcessBuilder pb = new ProcessBuilder("bash","-c","find " + sCrawlDir + " | grep java | grep -v noship | xargs grep -l png");
        pe.runProcess(pb, "/tmp/ListOfJavaFiles.txt", false); 
        
        BufferedReader fileReader = new BufferedReader(new FileReader("/tmp/ListOfJavaFiles.txt"));
        String line = null;
        while((line = fileReader.readLine()) != null)
            javaFilesWithPng.add(line.trim());*/
        
            
        System.out.println("Analyzing all files in directory:" + sCrawlDir + " starting at : " + new java.util.Date());        //List of files to be prcessed will be built by shell command
        scanner.crawlDirectory(sCrawlDir,false);
        
        outputFileWriter.close();
        System.out.println("Finished analyzing files: " + new java.util.Date());
        
        if(makeFix){
            diffHelper.startDiffLog(); 
            scanner.fixUXViolations(sCrawlDir);
            scanner.fixJavaViolations(sCrawlDir);
            diffHelper.closeDiffLog(); 
            System.out.println("Finished making fixes: " + new java.util.Date());
        }
        }
        else if(mode.equals("LRG")){
            String series = args[0]; //only needs series name as the argument
            //String series = "FSM_MAIN_GENERIC";
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running AltaIconIssues Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);
            
            String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
            String root = serverTop + "/"+series+".rdd/"+label+"/fusionapps/";
            String viewRoot = serverTop + "/"+series+".rdd/"+label+"/";
            if(series.contains("FSM"))
              root = serverTop + "/"+series+".rdd/"+label+"/fsm/";
            
            System.out.println("Building script metadata: " + new java.util.Date());
            scanner.buildScriptMetadata(root);
            
           // ProcessExecutor pe = new ProcessExecutor();   
            
            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
            for (int j = 0; j < families.size(); j++) {
                
                //javaFilesWithPng.clear();
                String family = families.get(j);
                String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, "AltaIconIssues");
                
               /* ProcessBuilder pb = new ProcessBuilder("bash","-c","find " + sCrawlDir + " | grep java | grep -v noship | xargs grep -l png");
                pe.runProcess(pb, "/tmp/ListOfJavaFiles.txt", false); 
                
                BufferedReader fileReader = new BufferedReader(new FileReader("/tmp/ListOfJavaFiles.txt"));
                String line = null;
                while((line = fileReader.readLine()) != null)
                    javaFilesWithPng.add(line.trim());*/
                
                System.out.println("Crawling dir '" + viewRoot + sCrawlDir + "' for family: " + family);                
                scanner.crawlDirectoryNew(viewRoot,sCrawlDir,false);      
                System.out.println("Done crawling for family "+family+".");
            }        
                        
            outputFileWriter.close();
            System.out.println("Finished analyzing files: " + new java.util.Date());
            scanner.writeResultsToAB(series);
        }
        else if(mode.equals("premerge")){            
            //String[] files = args[0].trim().split("\n");
            String ade_view_root = args[1].trim();
            String filelistpath=args[0].trim();
            String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root); 
            
            scanner.buildScriptMetadata(ade_view_root+"/fusionapps");
            
            for(int i = 0; i < files.length; i++) {
                
                String filePath = ade_view_root + "/" + files[i].trim();
                File f = new File(filePath);
                if(!f.exists())
                    continue;
                if(!scanner.fileOfInterest(filePath))
                    continue;
                scanner.processTrxFile(filePath);
            }
            outputFileWriter.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/AltaIconIssues " +
                "for description of the issue and resolution.\n\n");
            outputFileWriter.close();   
            
        }
        
        FamilyModuleHelper.printScanDuration(start,"AltaIconIssues");
    
    }
    
    private boolean isExempt(String f, String id) {
        
        if(!exemptions.containsKey(f))
            return false;
        
        HashSet<String> exemptIds = exemptions.get(f);
        if(exemptIds.contains("*") || exemptIds.contains(id))
            return true;
        
        return false;
    }
    
    private void generateExemptionList()  {
        
        try{
          BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
              "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_r13.txt"));
          String line = null;
          
          while((line = fileReader.readLine()) != null) {
            String[] parts = line.split(",");
            if(parts.length < 3)
                continue;
            String issue = parts[2].trim();
            if(!issue.equals("AltaIconIssue"))
                continue;
            String fileName = parts[0].trim();
            String id = parts[1].trim();
              
            HashSet<String> exemptLines = null;
            if(!exemptions.containsKey(fileName)) 
              exemptLines = new HashSet<String>();
            else 
              exemptLines = exemptions.get(fileName);
            
            exemptLines.add(id);
            exemptions.put(fileName, exemptLines);
          }
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        
    }
    
    private void fixJavaViolations(String crawlDir) throws Exception{
        
        BufferedReader reader = new BufferedReader(new FileReader("AltaUIViolations.csv"));
        String line = reader.readLine();
        String prevFilePath = null;
        JavaFileParserHelper javaHelper = null;
        String[] fileContents = null;
        String fileContentsAsString = "";
        String viewRoot = ViewCriteriaHelper.getViewRoot(crawlDir);
        BufferedWriter writer = new BufferedWriter(new FileWriter("fixer_notes.txt",true));
        String filePath = "";
        boolean contentsChanged = false;
        
        while((line = reader.readLine()) != null) {
            
          String[] parts = line.split(",");
            if(parts.length < 12)
                continue;
          filePath = viewRoot + "/" + parts[3].trim();
          if(!filePath.endsWith("java"))
              continue;
            
            if(!filePath.equals(prevFilePath)) { 
              
                if(prevFilePath != null && contentsChanged) {     
                                    
                  fileContentsAsString = javaHelper.getFileContentsAsString(fileContents,false);   
                  applyFix(prevFilePath,fileContentsAsString);
                }
                      
              javaHelper = new JavaFileParserHelper(filePath);
              fileContents = javaHelper.getFileContents();
              contentsChanged = false;
            }
            
            String issueType = parts[8].trim();
            String oldImage = parts[11].trim();
            String newImage = classicToAltaIcons.get(oldImage);            
            prevFilePath = filePath;
            
            if(oldImage.equals("addfile_qualifier.png")){
                writer.write(filePath + ", Java File Line# " + parts[6].trim() + "," + oldImage 
                             + " : Please fix manually since this is not an informational/qualifier icon. " +
                    "Use the appropriate functional alta icon func_documentadd_16*.png." +
                    "Product team needs to connect the missing states to properly use this as a functional icon.\n\n");
            continue;
            }
            
            if(issueType.equals("NewAltaUIWithClassicIcon"))
                continue;       
            
            if(issueType.equals("ClassicIconInAltaUI")) {
                
                String[] imageParts = newImage.split(",");     
                if(imageParts.length > 1) {
                    writer.write(filePath + ", Java File Line# " + parts[6].trim() + "," + oldImage 
                         + " : Please fix manually using the correct replacement alta icon for a button or link.\n\n");
                    continue;
                }
                
                String bidiStatus = iconsBiDiStatus.get(oldImage+":"+newImage);
                if(isEmpty(bidiStatus)){
                    writer.write(filePath + ", Java File Line# " + parts[6].trim() + "," + oldImage 
                         + " : Please fix manually as Bidirectional statu of the image cannot be determined.\n\n");
                    continue;
                }
                
                if(bidiStatus.equals("Neither icons are Bidi")){                
                    try{
                        int lineNo = Integer.parseInt(parts[6].trim());
                        fileContents[lineNo-1] = fileContents[lineNo-1].replace(oldImage,newImage);
                        contentsChanged = true;
                    }
                    catch(Exception e){
                        e.printStackTrace();
                        writer.write(filePath + ", Java File Line# " + parts[6].trim() + "," + oldImage 
                             + " : Please fix manually.\n\n");
                        continue;
                    }
                }                    
                else {
                   
                    writer.write(filePath + ", Java File Line# " + parts[6].trim() + "," + oldImage
                         + " : Please fix manually.");
                    if(bidiStatus.equals("Only classic icon is Bidi"))
                        writer.write("Only the classic Icon is Bidi. Please remove the BiDi EL if present when replacing with the Alta image: " + newImage + "\n\n");
                    else if(bidiStatus.equals("Only alta icon is Bidi"))
                        writer.write("Only the Alta Icon is Bidi. Please add the BiDi EL when replacing with the Alta image: " + newImage + "\n\n");
                    else if(bidiStatus.equals("Both classic and alta icons are Bidi"))
                        writer.write("Both Classic and Alta Icons are Bidi. Please make sure EL expression to support BiDi icons is present: " + newImage + "\n\n");
                    continue;
                        
                }
            } 
            else if(issueType.equals("MissingELForBiDiSupport")) {
            
                    writer.write(filePath + ", Java File Line# " + parts[6].trim() + "," + oldImage 
                         + " : Please make sure EL expression to support BiDi icons is present. \n\n"); 
                    continue;
            }
        }
        
        //apply fix to the last file 
        if(javaHelper != null && contentsChanged) {
          fileContentsAsString = javaHelper.getFileContentsAsString(fileContents, false);        
          applyFix(prevFilePath,fileContentsAsString);
        }
        reader.close();
        writer.close();
    }
    
    private void fixUXViolations(String crawlDir) throws Exception{
        
        BufferedReader reader = new BufferedReader(new FileReader("AltaUIViolations.csv"));
        String line = reader.readLine();
        String prevFilePath = null;
        XMLDocument doc = null;
        XmlFixer xmlFixer = null;
        String[] fileContents = null;
        String fileContentsAsString = null;
        String viewRoot = ViewCriteriaHelper.getViewRoot(crawlDir);
        BufferedWriter writer = new BufferedWriter(new FileWriter("fixer_notes.txt"));
        boolean checkFndTagInNS = false; 
        String prevNodeName = "";
        String prevNodeId = "";
        String prevNodeAttr ="";
        String attrNewValue = "";
        String newImage = "";
        String filePath = "";
        
        while((line = reader.readLine()) != null) {
            
          String[] parts = line.split(",");
            if(parts.length < 12)
                continue;
          filePath = viewRoot + "/" + parts[3].trim();
          if(filePath.endsWith("java")) 
              continue;
            
            if(!filePath.equals(prevFilePath)) { 
              
                if(prevFilePath != null) {     
                  
                  if(checkFndTagInNS){
                      NodeList roots = doc.getElementsByTagName("root");
                      
                      if(roots.getLength() ==1) {
                        Node root = roots.item(0);
                        String fndTag = XMLParserHelper.getAttributeValue(root, "xmlns:fnd");
                        if(isEmpty(fndTag))
                            xmlFixer.addAttribute(root, "xmlns:fnd", "http://xmlns.oracle.com/apps/fnd/applcore");
                      }
                      else 
                        System.out.println(filePath + ": Jsff should have exactly 1 root node!!! " + filePath); 
                  }
                  
                  fileContentsAsString = xmlFixer.getFileContentsAsString(false);   
                  applyFix(prevFilePath,fileContentsAsString);
                }
              
              doc = XMLParserHelper.getXMLDocument(filePath);        
              xmlFixer = new XmlFixer(filePath);
              fileContents = xmlFixer.convertFileToStringArray();
              checkFndTagInNS = false;
            }
            
        
            String nodeName = parts[7].trim();
            String nodeId = parts[6].trim();
            
            Node n = findElement(doc,nodeName,nodeId); //Find the node with the given name and id  
            
            String issueType = parts[8].trim();
            String oldImage = parts[11].trim();
            String newImages = classicToAltaIcons.get(oldImage);
            String attributeName = parts[10].trim();
            String attrOldValue = XMLParserHelper.getAttributeValue(n, attributeName);
                        
            if(filePath.equals(prevFilePath) && nodeName.equals(prevNodeName) 
               && nodeId.equals(prevNodeId) && attributeName.equals(prevNodeAttr)
                && !isEmpty(attrNewValue))
                attrOldValue = attrNewValue;
            
            prevFilePath = filePath;
            prevNodeName = nodeName;
            prevNodeId = nodeId;
            prevNodeAttr = attributeName;
            attrNewValue = "";
            newImage = "";
            
            if(oldImage.equals("addfile_qualifier.png")){
                writer.write(filePath + "," + nodeName + "(" + nodeId + "), " + attributeName + "," + oldImage 
                             + " : Please fix manually since this is not an informational/qualifier icon. " +
                    "Use the appropriate functional alta icon func_documentadd_16*.png." +
                    "Product team needs to connect the missing states to properly use this as a functional icon.\n\n");
            continue;
            }
            
            if(issueType.equals("NewAltaUIWithClassicIcon"))
                continue;       
            
            String bidiStatus = "";
            if(issueType.equals("ClassicIconInAltaUI")) {
                
                String[] imageParts = newImages.split(",");     
                if(imageParts.length > 1) {
                    if(nodeName.contains("Button"))
                        newImage = imageParts[1].trim();
                    else
                        newImage =imageParts[0].trim();                    
                }else
                    newImage = newImages;
                
                if(imageParts.length > 2)
                    System.out.println("More than 2 alta images for the same classic image: " + line);
                
                bidiStatus = iconsBiDiStatus.get(oldImage+":"+newImage);
                
                if(bidiStatus.equals("Neither icons are Bidi")){
                    attrNewValue = attrOldValue.replace(oldImage,newImage);
                }                    
                else if(bidiStatus.equals("Both classic and alta icons are Bidi")){
                    
                    Matcher m = pBiDiImage.matcher(attrOldValue);
                    if(m.matches())
                        attrNewValue = attrOldValue.replace(oldImage,newImage);
                    else{
                        m = pImage.matcher(attrOldValue);
                        if(m.matches()){
                            attrNewValue = "#{fnd:bidiImage('/images/" + newImage + "')}";
                            checkFndTagInNS = true;
                        }
                        else{
                            writer.write(filePath + "," + nodeName + "(" + nodeId + "), " + attributeName + "," + oldImage 
                                     + " : Please fix manually by adding prper EL for BiDi language support. " +
                                "Script cannot make the change since it is a non-trivial fix.\n\n");       
                            continue;
                        }
                    }                    
                }else if(bidiStatus.equals("Only classic icon is Bidi")){
                    
                    Matcher m = pBiDiImage.matcher(attrOldValue);
                    if(m.matches())
                        attrNewValue = "/images/" + newImage;
                    else{
                        if(!attrOldValue.contains("fnd:bidiImage"))
                            attrNewValue = attrOldValue.replace(oldImage,newImage);
                        else{
                            writer.write(filePath + "," + nodeName + "(" + nodeId + "), " + attributeName + "," + oldImage 
                                 + " : Please fix manually - Classic Icon is Bidi, but the new Alta Icon is not, so remove the BiDi EL if present." +
                            "Script cannot make the change since it is a non-trivial fix.\n\n"); 
                            continue;
                        }
                    }                    
                }else if(bidiStatus.equals("Only alta icon is Bidi")) {
                    checkFndTagInNS = true;
                    Matcher m = pImage.matcher(attrOldValue);
                    if(m.matches())
                        attrNewValue = "#{fnd:bidiImage('/images/" + newImage + "')}";
                    else{
                            writer.write(filePath + "," + nodeName + "(" + nodeId + "), " + attributeName + "," + oldImage 
                                 + " : Please fix manually - Classic Icon is not Bidi, but the new Alta Icon is, so add the BiDi EL." +
                            "Script cannot make the change since it is a non-trivial fix.\n\n"); 
                            continue;
                        }
                }
                else
                    System.out.println("Unknown Bidi Status!! Shouldn't happen ! " + line); 
       
            } 
            else if(issueType.equals("MissingELForBiDiSupport")) {
                checkFndTagInNS = true;
                Matcher m = pImage.matcher(attrOldValue);
                if(m.matches())
                    attrNewValue = "#{fnd:bidiImage('/images/" + oldImage + "')}";
                else{
                        writer.write(filePath + "," + nodeName + "(" + nodeId + "), " + attributeName + "," + oldImage 
                             + " : Please add the BiDi EL manually." +
                        "Script cannot make the change since it is a non-trivial fix.\n\n"); 
                        continue;
                    }
                
            }               
            if(!attrNewValue.equals("")){
                try{
                 boolean fixMade = xmlFixer.modifyAttributeIrrespectiveOldValue(n, attributeName, attrNewValue);
                    if(!fixMade)
                        writer.write(filePath + "," + nodeName + "(" + nodeId + "), " + attributeName + "," + oldImage 
                         + " : Script could not fix the violation. Please review and fix manually.\n\n");
                }catch(Exception e) {
                    if(bidiStatus.equals("Neither icons are Bidi"))
                        xmlFixer.modifyAttributeImage(n, attributeName, oldImage,newImage);
                    else
                        writer.write(filePath + "," + nodeName + "(" + nodeId + "), " + attributeName + "," + oldImage 
                         + " : Script could not fix the violation. Please review and fix manually.\n\n");
                }
            }
            else
                writer.write(filePath + "," + nodeName + "(" + nodeId + "), " + attributeName + "," + oldImage 
             + " : Script could not fix the violation. Please review and fix manually.\n\n");
        }
        
        //apply fix to the last file 
        if(xmlFixer != null) {
            
            if(checkFndTagInNS){
                NodeList roots = doc.getElementsByTagName("jsp:root");
                if(roots.getLength() ==1) {
                  Node root = roots.item(0);
                  String fndTag = XMLParserHelper.getAttributeValue(root, "xmlns:fnd");
                  if(isEmpty(fndTag))
                      xmlFixer.addAttribute(root, "xmlns:fnd", "http://xmlns.oracle.com/apps/fnd/applcore");
                }
                else 
                  System.out.println("Jsff should have exactly 1 root node!!! " + filePath); 
            }
            
          fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
          applyFix(prevFilePath,fileContentsAsString);
        }
        reader.close();
        writer.close();
    }
    
    private Node findElement(XMLDocument doc, String nodename, String id){
      NodeList list = doc.getElementsByTagName(nodename);
      for(int i = 0; i< list.getLength(); i++) {
          Node n = list.item(i);
          if(XMLParserHelper.getAttributeValue(n, "id").equals(id)) return n;
      }
      return null;
    }
    
    private void applyFix(String absPath,
                        String newFileContents) throws Exception {
       
       File file = new File(absPath);
       if (!file.exists()) {
           throw new Exception("while making fix, file not found: " +
                               absPath);
       }
       diffHelper.checkoutAndDelete(file);

       FileWriter fw = new FileWriter(file.getAbsolutePath());
       BufferedWriter bw = new BufferedWriter(fw);
       bw.write(newFileContents);
       if (bw != null)
           bw.close();
       if (fw != null)
           fw.close();
       diffHelper.checkinAndDiff(file);
    }
    
    private void writeResultsToAB(String series) {
        
        try{
            
           Connection con = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan", "codescan", "codescan");
           con.setAutoCommit(false);
            
            PreparedStatement cstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS(FAMILY,MODULE,PRODUCT,FILENAME,SERIES,LABEL,ISSUETYPE,SUB_ISSUE,DESCRIPTION) " +
                    "VALUES(?,?,?,?,?,?,?,?,?)");
            PreparedStatement cstmt1 = con.prepareStatement("INSERT INTO CODESCAN_RESULTS_P(FAMILY,MODULE,PRODUCT,FILENAME,SERIES,LABEL,ISSUETYPE,SUB_ISSUE,DESCRIPTION) " +
                  "VALUES(?,?,?,?,?,?,?,?,?)");
               
              System.out.println("First delete records from DB...");
              Statement stmt = con.createStatement();
              String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE in ('ClassicIconInAltaUI'," +
                  "'MissingELForBiDiSupport','NewAltaUIWithClassicIcon') and series = '" + series + "'" ;
              String sql1 = "DELETE FROM CODESCAN_RESULTS_P WHERE ISSUETYPE in ('ClassicIconInAltaUI'," +
              "'MissingELForBiDiSupport','NewAltaUIWithClassicIcon') and series = '" + series + "'" ;
              
              if(series.contains("FSM"))
                stmt.executeUpdate(sql1); 
              else
                stmt.executeUpdate(sql);           
              stmt.close();    
            
            System.out.println("Now inserting values...");
            int count = 0;
            
            BufferedReader fileReader = new BufferedReader(new FileReader("AltaUIViolations.csv"));
            String line = fileReader.readLine();
            
            while((line = fileReader.readLine()) != null) {
                
              String[] parts = line.split(",");
                if(parts.length < 12) {
                    System.out.println("Skipping: " + line);
                    continue;
                }
                String subissue = parts[6] + "("+ parts[7] + ")";
                String description = parts[10] + ": " + parts[11] + " - " + parts[9];
                
              if(series.contains("FSM")){
              
                cstmt1.setString(1, parts[0].trim()); //family
                cstmt1.setString(2, parts[1].trim()); //module
                cstmt1.setString(3, parts[2].trim()); //product
                cstmt1.setString(4, parts[3].trim()); //filename 
                cstmt1.setString(5, parts[4].trim()); //series
                cstmt1.setString(6, parts[5].trim()); //label
                cstmt1.setString(7, parts[8].trim()); //issue     
                cstmt1.setString(8, subissue); //subissue              
                cstmt1.setString(9,description); //Description
                cstmt1.addBatch();     
              }else { 
                
                cstmt.setString(1, parts[0].trim()); //family
                cstmt.setString(2, parts[1].trim()); //module
                cstmt.setString(3, parts[2].trim()); //product
                cstmt.setString(4, parts[3].trim()); //filename 
                cstmt.setString(5, parts[4].trim()); //series
                cstmt.setString(6, parts[5].trim()); //label
                cstmt.setString(7, parts[8].trim()); //issue     
                cstmt.setString(8, subissue); //subissue              
                cstmt.setString(9,description); //Description
                cstmt.addBatch();     
              }
                count++;
                
              if(count%1000 ==0){
                if(series.contains("FSM"))
                    cstmt1.executeBatch();
                else
                    cstmt.executeBatch();
              }
                
            }
            
          if(series.contains("FSM"))
              cstmt1.executeBatch();
          else
              cstmt.executeBatch();
          
            con.commit();
            cstmt.close();
            cstmt1.close();
            con.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        
    }
    
    public void buildScriptMetadata(String crawlDir) throws Exception {
        
        String viewRoot = ViewCriteriaHelper.getViewRoot(crawlDir);
        String imageFile = viewRoot + "/fusionapps/jlib/AdfAcrImagesPublicUi.jar";
        if(crawlDir.contains("FSM"))
          imageFile = viewRoot + "/fsm/fajars/AdfAcrImagesPublicUi.jar";
        
        System.out.println("Reading images jar: " + imageFile);
        
        ZipFile jar = new ZipFile(imageFile);
        Enumeration entries = jar.entries();

        while (entries.hasMoreElements()) {
          ZipEntry ze = (ZipEntry)entries.nextElement();
          String name = ze.getName();
            name = name.replace("images/","");
          if(name.endsWith(".png") && name.contains("_rtl"))
              origToRTLIcons.put(name.replace("_rtl", ""),name);
        }
        
        
        BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
                    "/fatools/opensource/jauditFixScripts/txt/R12UIFiles.txt")); 
        String line = null;
        
        while((line = fileReader.readLine()) != null)
            r11Files.add(line.trim());
        
   
        fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
            "/fatools/opensource/jauditFixScripts/txt/ClassicIconConversionList.csv")); 
        line = null;
        
        while((line = fileReader.readLine()) != null) {
          String[] parts = line.split(",");
          if(parts.length < 2)
              continue;
            
            String classicIcon = parts[0].trim();
            String altaIcon = parts[1].trim();
          
            if(!classicToAltaIcons.containsKey(classicIcon))
                classicToAltaIcons.put(classicIcon,altaIcon);
            else
                classicToAltaIcons.put(classicIcon,classicToAltaIcons.get(classicIcon) + "," + altaIcon);
            
            if(parts.length > 2)
                iconsBiDiStatus.put(classicIcon +":" +altaIcon, parts[2].trim());
            if(parts.length > 3)
                specialClassicIcons.put(classicIcon,parts[3].trim());
        }
        
        fileReader.close();
        
    }
    
    private boolean isEmpty(String s){
        if (s == null || s.trim().equals(""))
            return true;
        return false;
    }
    
    public void processUIFile(String filePath){
        
        try{
            
            XMLDocument doc = XMLParserHelper.getXMLDocument(filePath);
            if(doc == null){
                System.out.println("XML DOM could not be constructed for file: " + filePath);
                return;
            }
            
            boolean newFile=false;
            String fileNameAfterFusionapps = "fusionapps/" + FamilyModuleHelper.getPathAfterFusionapps(filePath);
            if(!r11Files.contains(fileNameAfterFusionapps))
                newFile=true;
            
            String fileDetails = FamilyModuleHelper.getFileNameInfo1(filePath);
            
            checkNodeAttributes(fileNameAfterFusionapps,newFile,fileDetails,doc,"commandButton",new ArrayList<String>(Arrays.asList("icon")));
            checkNodeAttributes(fileNameAfterFusionapps,newFile,fileDetails,doc,"menu",new ArrayList<String>(Arrays.asList("icon")));
            checkNodeAttributes(fileNameAfterFusionapps,newFile,fileDetails,doc,"goButton",new ArrayList<String>(Arrays.asList("icon")));
            checkNodeAttributes(fileNameAfterFusionapps,newFile,fileDetails,doc,"commandMenuItem",new ArrayList<String>(Arrays.asList("icon")));
            checkNodeAttributes(fileNameAfterFusionapps,newFile,fileDetails,doc,"commandNavigationItem",new ArrayList<String>(Arrays.asList("icon")));
            checkNodeAttributes(fileNameAfterFusionapps,newFile,fileDetails,doc,"commandToolbarButton",new ArrayList<String>(Arrays.asList("icon",
                                                                        "disabledIcon","hoverIcon","depressedIcon")));
            checkNodeAttributes(fileNameAfterFusionapps,newFile,fileDetails,doc,"activeCommandToolbarButton",new ArrayList<String>(Arrays.asList("icon",
                                                                        "disabledIcon","hoverIcon","depressedIcon")));
            checkNodeAttributes(fileNameAfterFusionapps,newFile,fileDetails,doc,"goImageLink",new ArrayList<String>(Arrays.asList("icon",
                                                                        "disabledIcon","hoverIcon","depressedIcon")));
            checkNodeAttributes(fileNameAfterFusionapps,newFile,fileDetails,doc,"commandImageLink",new ArrayList<String>(Arrays.asList("icon",
                                                                        "disabledIcon","hoverIcon","depressedIcon")));
            checkNodeAttributes(fileNameAfterFusionapps,newFile,fileDetails,doc,"image",new ArrayList<String>(Arrays.asList("source")));
            checkNodeAttributes(fileNameAfterFusionapps,newFile,fileDetails,doc,"activeImage",new ArrayList<String>(Arrays.asList("source")));
            checkNodeAttributes(fileNameAfterFusionapps,newFile,fileDetails,doc,"dialog",new ArrayList<String>(Arrays.asList("titleIconSource")));
        
            
        }catch(Exception e){
            System.out.println("Could not process UI file: "+ filePath);
            e.printStackTrace();
        }
    }
    
    public void checkNodeAttributes(String fileName, boolean newFile, String fileDetails, XMLDocument doc, String nodeName, ArrayList<String> attributes) throws Exception{
        
        NodeList nodes = doc.getElementsByTagName(nodeName);
        Matcher m = null;
        String img = "";
        for(int i =0; i< nodes.getLength(); i++) {
            
            Node n = nodes.item(i);
            String id = XMLParserHelper.getAttributeValue(n,"id");
            
            for( int j =0; j< attributes.size();j++) {  
                
            String attr = XMLParserHelper.getAttributeValue(n,attributes.get(j));
            if(!isEmpty(attr)){
                m = pImage.matcher(attr);
                while(m.find()){
                    
                    img = m.group(1);
                    if(isExempt(fileName,id))
                        continue;
                    if(fileName.contains("hcmTime/time/webEntry/transaction/") && img.equals("cal_prev_blk.png"))
                        continue;
                        
                    if(classicToAltaIcons.containsKey(img))
                        outputFileWriter.write(fileDetails+id+","+nodeName+",ClassicIconInAltaUI," +
                            "Ensure no listed classic icon (from top 1000 list) in Alta UI,"+ attributes.get(j) + "," + img + "\n");
                    else if(newFile && !img.startsWith("func_") && !img.startsWith("grid_") && !img.startsWith("qual_") && !img.equals("seperator_img.png"))
                        outputFileWriter.write(fileDetails+id+"," + nodeName + ",NewAltaUIWithClassicIcon," +
                            "Any new UI created R13 onwrads should not use any classic icon,"+ attributes.get(j) + "," + img + "\n");
                    else if(origToRTLIcons.containsKey(img) && !attr.contains("fnd:bidiImage"))
                        outputFileWriter.write(fileDetails+id + "," + nodeName + ",MissingELForBiDiSupport," +
                            "Ensure proper EL is used with RTL icons to support BiDi,"+ attributes.get(j) + "," + img + "\n");
                   
                }
             }
            }//end for loop attribute 
        }// end for loop nodes
    }
    
    public void processJavaFile(String filePath){
        
        try{
            JavaFileParserHelper javaHelper = new JavaFileParserHelper(filePath);
            javaHelper.removeAllComments();
            String fileContents = javaHelper.getFileWithoutCommentsReadOnly();
            if(!fileContents.contains("png"))
                return;
            String[] lines = javaHelper.getFileContentsArrayWithoutComments(fileContents);
            String line = "";
            Matcher m = null;
            String img = "";
            String fileDetails = FamilyModuleHelper.getFileNameInfo1(filePath);
            String fileNameAfterFusionapps = "fusionapps/" + FamilyModuleHelper.getPathAfterFusionapps(filePath);
            
            for(int i=0;i<lines.length;i++){
                
                line = lines[i].trim();
                if(line.contains("png")){
                    
                    m= pImage.matcher(line);                
                    while(m.find()){
                        img = m.group(1);
                        if(isExempt(fileNameAfterFusionapps,img))
                            continue;
                        if(classicToAltaIcons.containsKey(img))
                            outputFileWriter.write(fileDetails+ (i+1) +",Java File Line#,ClassicIconInAltaUI," +
                                "Ensure no listed classic icon (from top 1000 list) in Alta UI,-," + img + "\n");
                        else if(origToRTLIcons.containsKey(img) && !line.contains("bidiImage"))
                            outputFileWriter.write(fileDetails+ (i+1) + ",Java File Line# ,MissingELForBiDiSupport," +
                                "Ensure proper EL is used with RTL icons to support BiDi,-,"+ img + "\n");
                        
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        
    }
}
