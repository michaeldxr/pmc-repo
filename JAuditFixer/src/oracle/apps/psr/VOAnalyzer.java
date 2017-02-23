package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.PreparedStatement;

import java.sql.Statement;

import java.util.ArrayList;

import java.util.HashSet;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.JoesBaseClass;

import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class VOAnalyzer extends JoesBaseClass{
    
    static BufferedWriter writer;
    private static String mode="view"; //LRG/view/premerge mode
    static String sCrawlDir = "";
    HashSet<String> exemptions = new HashSet<String>();
    public VOAnalyzer() {
        super(JoesBaseClass.CRAWL_TYPE.VO_XML);
    }
        
    public static void main(String[] args) throws Exception{
        
       java.util.Date start = new java.util.Date();
        VOAnalyzer x = new VOAnalyzer();
        x.generateExemptions();
        
        mode = System.getProperty("mode");
        if(mode == null)
            mode = "view";
        //mode = "LRG";
        
        writer = new BufferedWriter(new FileWriter("vo_passivation_scan.csv"));
        writer.write("Family,Module,Product,Filename,Series, Label,Passivate, # Attributes, MaxFetchSize, SdoName \n");
        
        if(mode.equals("view")) {
              
           if (args.length < 1 || args[0] == null || args[0].equals("")) {
             System.out.println("USAGE: voNotPassivated.sh <crawlDirectory>");
             System.exit(1);
           }
           sCrawlDir = args[0];
              
           System.out.println("Analyzing files in directory for VO Passivation: " + sCrawlDir);
        
           x.crawlDirectory(sCrawlDir, false);
           writer.close();
           System.out.println("Done. Please review vo_passivation_scan.csv file for results of the scan.");
        }
        else if(mode.equals("LRG")){
            //String series = "FSM_MAIN_GENERIC";
            String series = args[0]; //only needs series name as the argument    
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running VONotPassivated Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);
            
            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
            String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
            String viewRoot = serverTop + "/"+series+".rdd/"+label + "/";
          
            for (int j = 0; j < families.size(); j++) {
                String family = families.get(j);
                String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, "VONotPassivated");               
                
                System.out.println("Crawling dir '" + viewRoot + sCrawlDir + "' for family: " + family);
                
                x.crawlDirectoryNew(viewRoot,sCrawlDir,false);      
                System.out.println("Done crawling for family "+family+".");
            }        
            writer.close();
            x.dumpResultsToAB(series);
            
        } else if(mode.equals("premerge")) {
            //premerge integration
            //String[] files = args[0].trim().split("\n");
            String ade_view_root = args[1].trim();
            String filelistpath=args[0].trim();
            String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root); 

            String label = args[2].trim();
           
            
            String release= FamilyModuleHelper.getRelease(label);
            System.out.println("Release: " + release);
            
            try{              
                int releaseNum = Integer.parseInt(release);
                if (releaseNum < 12) //This check was mandated post release 12 only
                    return;              
            }catch(NumberFormatException e){
                System.out.println("Could not determine release. Performing the check irrespective" +
                    "(but if you are on release older than 12, you may ignore the violations for this scan)...");
            }
            
            for(int i = 0; i < files.length; i++) {
                
                String filePath = ade_view_root + "/" + files[i].trim();
                File f = new File(filePath);
                if(!f.exists())
                    continue;
                if(!x.fileOfInterest(filePath))
                    continue;
                x.processFile(f,false);
            }
            writer.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/VONotPassivated " +
                "for description of the issue and resolution.\n\n");
            writer.close();   
        }
        FamilyModuleHelper.printScanDuration(start,"VONotPassivated");
    }
    
    private void generateExemptions() {
        try{
            BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
                "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_Su.txt"));
            String line = null;
            
            while((line = fileReader.readLine()) != null) {
              String[] parts = line.split(",");
              if(parts.length < 2)
                  continue;
              String issue = parts[0].trim();
              if(!issue.equals("VONotPassivated"))
                  continue;
              String fileName = parts[1].trim();
              
              exemptions.add(fileName);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private void dumpResultsToAB(String series) throws Exception {
        
        System.out.println("Dumping results to Angrybirds DB...");
        //BufferedWriter w = new BufferedWriter(new FileWriter("filtered.csv"));
        //w.write("Family,Module,Product,Filename,Series, Label,IssueType, Description\n");
        Connection con =
          DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
        PreparedStatement insert =
          con.prepareStatement("INSERT INTO codescan_results (SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME," +
            "ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)");     
      PreparedStatement insert1 =
        con.prepareStatement("INSERT INTO codescan_results_p (SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME," +
          "ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)");     
        
        Statement delete = con.createStatement();
        if(series.contains("FSM"))
          delete.executeUpdate("DELETE from codescan_results_p where issuetype='VONotPassivated' and series = '" + series + "'");
        else
          delete.executeUpdate("DELETE from codescan_results where issuetype='VONotPassivated' and series = '" + series + "'");
        delete.close();
        

        BufferedReader reader = new BufferedReader(new FileReader("vo_passivation_scan.csv"));
        String line = reader.readLine();
        
        while((line = reader.readLine()) != null) {
            
            String[] parts = line.split(",");
            if(parts.length < 10)
                continue;
            
            String subissue = "#Attributes = " + parts[7].trim() + ", MaxFetchSize = " + parts[8].trim() + ", SdoName = " + parts[9].trim();

          if(series.contains("FSM")){
            insert1.setString(1, parts[4].trim());
            insert1.setString(2, parts[5].trim());
            insert1.setString(3, parts[0].trim());
            insert1.setString(4, parts[1].trim());
            insert1.setString(5, parts[2].trim());
            insert1.setString(6, parts[3].trim());
            insert1.setString(7, "VONotPassivated");
            insert1.setString(8, subissue);
            insert1.setString(9,"Stateful VOs should be passivated.");
            insert1.addBatch();
            
          } else {
              insert.setString(1, parts[4].trim());
              insert.setString(2, parts[5].trim());
              insert.setString(3, parts[0].trim());
              insert.setString(4, parts[1].trim());
              insert.setString(5, parts[2].trim());
              insert.setString(6, parts[3].trim());
              insert.setString(7, "VONotPassivated");
              insert.setString(8, subissue);
              insert.setString(9,"Stateful VOs should be passivated.");
              insert.addBatch();
          }
            
        }
        reader.close();
      if(series.contains("FSM"))
        insert1.executeBatch();
      else
        insert.executeBatch();
        insert.close();
        insert1.close();
        con.commit();
        con.close();  
        //w.close();
        System.out.println("Done");
    }

    protected void processFile(File fName, boolean bDoADE) {
        
        String f = fName.getAbsolutePath();
        //System.out.println(f);
        if(f.contains("/flex/") || f.contains("/publicFlex/") || f.contains("/viewLink/") || f.contains("/noship"))
            return;
        
        String absPathLowerCase = f.toLowerCase();
        if (absPathLowerCase.contains("test.") || absPathLowerCase.contains("/test/") || 
            absPathLowerCase.contains("/modeltest") || absPathLowerCase.contains("/uitest/")
            || absPathLowerCase.contains("/testui") || absPathLowerCase.contains("servicetest") 
            || absPathLowerCase.contains("/publicservicetest") || absPathLowerCase.contains("/publicuitest")
            || absPathLowerCase.contains("/publicmodeltest") || absPathLowerCase.contains("/structuretest"))
            return;
        
        try{
            
            XMLDocument doc = XMLParserHelper.getXMLDocument(f);

            if(doc == null) {
              System.out.println("Could not parse XMLDocument..Skipping");
              return;
            }
            
              NodeList viewObjects = doc.getElementsByTagName("ViewObject");
              if(viewObjects == null || viewObjects.getLength() != 1) 
              {
                System.out.println("WARNING:  Unexpected number of view objects found");
                System.out.println("Skipping...");
                return;
              }
            Node viewObject = viewObjects.item(0);

            String passivateFlag = XMLParserHelper.getAttributeValue(viewObject, "Passivate");
            ArrayList<Node> attributes = XMLParserHelper.getChildNodesWithName(viewObject, "ViewAttribute");
            int numAttributes = 0;
            if(attributes != null)
                numAttributes = attributes.size();
            
            String maxFetchSize = XMLParserHelper.getAttributeValue(viewObject, "MaxFetchSize");
            String sdoName = XMLParserHelper.getAttributeValue(viewObject, "SdoName");
            
            if(passivateFlag == null || !passivateFlag.equals("None"))
                return;
            
            String fileName = FamilyModuleHelper.getPathAfterViewRoot(f);
            
            if(fileName.endsWith("VVO.xml") || fileName.contains("analytics") || fileName.contains("Analytics")
                || fileName.contains("service") || fileName.contains("Service") || fileName.contains("restModel"))
                return;
            if((fileName.contains("Ess/") || fileName.contains("/ess")) 
               && !fileName.endsWith("ParamVO.xml") && !fileName.endsWith("ParamsVO.xml"))
                return;
                         
            if(exemptions.contains(fileName))  
                return;
            
            writer.write(FamilyModuleHelper.getFileNameInfo1(f)+passivateFlag + ","+ numAttributes + "," + maxFetchSize + "," + sdoName + "\n");
            
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return null;
    }
}
