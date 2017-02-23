package oracle.apps.references;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;

import java.io.InputStreamReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DuplicateDeploymentDescEntries {
    
    private static BufferedWriter writer;
    private static String mode="view"; //can be view/LRG/premerge
    
    public static void main(String[] args) throws Exception{
        
        mode = System.getProperty("mode");
        if(mode == null)
            mode="view";
        System.out.println("Operation mode is: " + mode);
        
        writer = new BufferedWriter(new FileWriter("duplicateDeploymentDescriptorEntries.csv"));
        writer.write("FileName, Description\n");
        
        DuplicateDeploymentDescEntries scanner = new DuplicateDeploymentDescEntries();
        if(mode.equals("view")) {
            
            if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
                 System.out.println("Crawl directory must be specified");
                 System.exit(1);
             }
            String crawlDir = args[0].trim();
            
            File f = new File(crawlDir);
            if(!f.exists()) {
                System.out.println("Crawl directory does not exist - " + crawlDir);
                System.exit(1);
            }
            
            System.out.println("Analyzing all files in directory: " + crawlDir);
            scanner.crawlDirectory(crawlDir);
            writer.close();
            
        }else if(mode.equals("LRG")){
            
            if (args.length != 1 || args[0] == null || args[0].equals("")) {
                System.out.println("Series has to be specified for " + "DuplicateDeploymentDescriptorEntries");
                System.exit(1);
            }

            String series = args[0];
            
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running DuplicateDeploymentDescriptorEntries Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);
            
            String crawlDir = LRGUtil.getCrawlDir(series, null, label,"DuplicateDeploymentDescriptorEntries");
            
            scanner.crawlDirectory(crawlDir);
            writer.close();
            
            //scanner.updateDatabase();
            
        } else if (mode.equals("premerge")){
            //TODO
        }
    
    }
    
    private void crawlDirectory(String crawlDir) {
        
        if (crawlDir.contains(".ade_path") || crawlDir.contains("/classes/") || crawlDir.contains("/noship"))
            return;
        
        String pathAfterFusionapps = FamilyModuleHelper.getPathAfterViewRoot(crawlDir);
        String[] parts = pathAfterFusionapps.split("/");
        if(parts.length > 2 && !pathAfterFusionapps.contains("deploy"))
            return;

        File folder = new File(crawlDir);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if(absFilePath.endsWith(".ear"))
                    processEar(absFilePath);                

            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                crawlDirectory(dir);
            } else {
                System.out.println("ERROR:  node is neither file or directory: " + listOfFiles[i].getAbsolutePath());
            }
        }
    }
    
    private void processEar(String absPath) {
        try{
            HashSet<String> webmodules = new HashSet<String>();
            JarInputStream jarStream = new JarInputStream(new FileInputStream(absPath));
            JarFile jarFile = new JarFile(absPath);
            

            if (jarStream == null) {
                System.err.println("[ERROR] Could not open jar stream");
                return;
            }
            
            while (true) {

                JarEntry jarEntry = jarStream.getNextJarEntry();
                if (jarEntry == null) {
                    break;
                }
                String strEntryName = jarEntry.getName().trim();
            

                if(strEntryName.equals("META-INF/application.xml")){
                    
                    XMLDocument doc = XMLParserHelper.getXMLDocument(jarFile.getInputStream(jarEntry));
                    NodeList modules = doc.getElementsByTagName("module");
                    for(int i = 0; i < modules.getLength(); i++){
                        Node m = modules.item(i);
                        Node ejb = XMLParserHelper.getChildNodeWithName(m, "ejb");
                        if(ejb != null){
                            String ejbName = ejb.getTextContent();
                            if(webmodules.contains(ejbName))
                                writer.write(absPath + "/" + strEntryName + "," + ejbName + "\n");
                            else
                                webmodules.add(ejbName);
                        }
                        
                        Node web = XMLParserHelper.getChildNodeWithName(m, "web");
                        if(web != null){
                            Node contextRoot = XMLParserHelper.getChildNodeWithName(web, "context-root");
                            String contextRootStr = contextRoot.getTextContent();
                            if(webmodules.contains(contextRootStr))
                                writer.write(absPath + "/" + strEntryName + "," + contextRootStr + " - " 
                                +  XMLParserHelper.getChildNodeWithName(web, "web-uri").getTextContent() + "\n");
                            else
                                webmodules.add(contextRootStr);                            
                        }
                    }
                    //BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(jarEntry)));
                }
            }
            
        }catch(Exception e){
            System.out.println("Exception while processing file: " + absPath);
            e.printStackTrace();
        }
    }
    
  /*  public void updateDatabase() throws Exception {
      
      Connection con = null;
      try{
          String fileName = pwd + "/BindVarKindMismatch.csv";
         
          BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
          String line = fileReader.readLine();
               
          //Class.forName("oracle.jdbc.driver.OracleDriver");
          con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
          con.setAutoCommit(false);
          
          PreparedStatement cstmt = con.prepareStatement("INSERT INTO CODESCAN_RESULTS(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,LABEL,SERIES) " +
                  "VALUES(?,?,?,?,?,?,?,?,?)");
             
            System.out.println("First delete records from DB...");
            Statement stmt = con.createStatement();
            String sql = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'BindVarKindMismatch' and SERIES = '" + series + "'" ;
            
            stmt.executeUpdate(sql);           
            stmt.close();    
         
          System.out.println("Now inserting values...");
          int count = 0;
          while((line = fileReader.readLine()) != null) {
              
            String[] parts = line.split(",");
              if(parts.length < 9) {
                  System.out.println("Skipping: " + line);
                  continue;
              }
              
              String description = "";
              String reference = parts[8].trim();
              String kind = parts[7].trim();
              
              if(reference.equals("none") && kind.equals("where"))
                  description = "BindVariable is not referenced in the VO. Safest to change its kind to viewcriteria. ";
              if(reference.equals("vci") && !kind.equals("viewcriteria"))
                  description = "BindVariable is only referenced in VC. Change the kind to viewcriteria. ";
              if((reference.equals("where") || reference.equals("both")) && !kind.equals("where"))
                  description = "Make the \"kind\" of BindVariable \"where\" as it is referenced " +
                      "in the SQL Query. ";
              
              if(parts.length > 10)
                  description += parts[10].trim();      
              
              if (LRGUtil.isDeferred(con, series, parts[3].trim()))
                continue;
              
            cstmt.setString(1, parts[0].trim()); //family
            cstmt.setString(2, parts[1].trim()); //module
            cstmt.setString(3, parts[2].trim()); //product
            cstmt.setString(4, parts[3].trim()); //filename 
            cstmt.setString(8, parts[5].trim()); //label
            cstmt.setString(9, parts[4].trim()); //series
            cstmt.setString(5, "BindVarKindMismatch"); //issue     
                
            cstmt.setString(6, parts[6].trim() + ": " + kind); //subissue              
            cstmt.setString(7,description); //Description
            cstmt.addBatch();     
            count++;
          }
          cstmt.executeBatch();
          con.commit();
          cstmt.close();
          System.out.println("Total count for BindVarKindMismatch is "+count +" on series "+series);
      }catch(Exception e){
          e.printStackTrace();
          if(con != null)
             con.rollback();
      }finally {
         if(con != null)
             con.close();
      }
    }  */
}
