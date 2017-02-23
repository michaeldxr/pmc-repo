package oracle.apps.references;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import java.util.ArrayList;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class InvalidDeploymentProfiles extends JoesBaseClass{
    
    static BufferedWriter writer;
    
    public InvalidDeploymentProfiles() 
    {
        super(JoesBaseClass.CRAWL_TYPE.JPR);
    }

    protected void processFile(File fName, boolean bDoADE) {
        
        try {
            
            XMLDocument jprXml = XMLParserHelper.getXMLDocument(fName.getAbsolutePath());
            jprXml.setProperty("SelectionLanguage", "XPath");
            
            NodeList profileList = jprXml.selectNodes("//hash[@n='profileDefinitions']/hash");
            
            for(int i = 0; i < profileList.getLength(); i++) {
                
                String profileName = "";
                String profileLocation="";
                String profileType="";
                
                Node profile = profileList.item(i);
                profileName = XMLParserHelper.getAttributeValue(profile, "n");
                
                if(profileName.startsWith("Svc")){
                
                    ArrayList<Node> childHashes = XMLParserHelper.getChildNodesWithName(profile, "hash");
                    for(int j =0; j < childHashes.size(); j++) {
                        
                        Node childHash = childHashes.get(j);                        
                        Node url = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(childHash, "url", "n", "jarURL");                       
                        profileLocation = XMLParserHelper.getAttributeValue(url, "path");                        
                       
                        if(profileLocation==null || profileLocation.contains("jlib"))                          
                            continue;
                        
                        Node profileClass = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(childHash, "value", "n", "profileClass");
                        if(profileClass != null)
                            profileType = XMLParserHelper.getAttributeValue(profileClass, "v");
                        
                        Node profileNameNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(childHash, "value", "n", "profileName");
                        if(profileNameNode != null)
                            profileName=XMLParserHelper.getAttributeValue(profileNameNode, "v");
                        
                        writer.write(FamilyModuleHelper.getFileNameInfo1(fName.getAbsolutePath()) + 
                                     profileName + "," + profileLocation + "," + profileType + "\n");
                        
                    }
                    
                } else {
                    Node url = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(profile, "url", "n", "jarURL");                    
                    profileLocation = XMLParserHelper.getAttributeValue(url, "path");
                
                    if(profileLocation==null || profileLocation.contains("jlib"))
                        continue;
                    
                    Node profileClass = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(profile, "value", "n", "profileClass");
                    if(profileClass != null)
                        profileType = XMLParserHelper.getAttributeValue(profileClass, "v");
                    
                    writer.write(FamilyModuleHelper.getFileNameInfo1(fName.getAbsolutePath()) + 
                                 profileName + "," + profileLocation + "," + profileType + "\n");
                }
                
            }            
            
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return "SCRIPT: Detect invalid deployment profiles";
    }
    
    public static void main(String[] args) throws Exception{
        
//        if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
//            System.out.println("Usage:  detectInvalidProfiles.sh <path to your code in ade>");
//            System.exit(1);
//        }
//        
//        String sCrawlDir = args[0].trim();
        String sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_PT.R9ERP_LINUX.X64.rdd/LATEST/fusionapps/";
        
        File f = new File(sCrawlDir);
        if(!f.exists()){
            System.out.println("Crawl directory does not exist - " + sCrawlDir);
            System.exit(1);
        }
        writer = new BufferedWriter(new FileWriter("invalidProfiles.csv"));
        writer.write("Family,Module,Product,Filename,Series,Label,Profile Name, Jar URL, Profile Class\n"); 
        InvalidDeploymentProfiles x = new InvalidDeploymentProfiles();
        x.crawlDirectory(sCrawlDir, false);
        writer.close();
        
    }
}
