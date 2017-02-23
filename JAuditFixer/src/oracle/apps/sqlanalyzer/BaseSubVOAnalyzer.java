package oracle.apps.sqlanalyzer;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;

import java.io.ObjectOutputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import java.util.HashSet;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.Mail;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class BaseSubVOAnalyzer {
    
    static String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
    static String statsUname = "codescan";
    static String statsPwd = "codescan";
    public static HashMap<String,HashSet<String>> base_subVos = new HashMap<String,HashSet<String>>();

    public static void main(String[] args) throws Exception {
            
        Connection con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
        Statement stmt = con.createStatement();
        BaseSubVOAnalyzer scanner = new BaseSubVOAnalyzer();             
        
        ResultSet rs = stmt.executeQuery("SELECT SERIES,LABELSERVER from BRANCH_FAMILY_CODELINE");
        while(rs.next()) {
            String series = rs.getString(1);
            String labelserver = rs.getString(2);
            String sCrawlDir = labelserver + "/"+series + ".rdd/LATEST/";
            if(series.contains("FSM"))
                sCrawlDir += "fsm/components";
            else
                sCrawlDir += "fusionapps";
            
            System.out.println("Analyzing baseVO-subVO relationships in directory:" + sCrawlDir);
            scanner.crawlDirectory(sCrawlDir);  
        }

        scanner.writeSerializedHashMapToDB();        
        BufferedWriter writer = new BufferedWriter(new FileWriter("base_sub.txt"));
        writer.write(base_subVos.toString());
        writer.close();
    
    }
    
    public void crawlDirectory(String path) throws Exception
    {
        if(path.contains(".ade_path") || path.contains("/classes/"))
            return;
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
            
        if(listOfFiles == null)
            return;      
        for(int i = 0; i <listOfFiles.length; i++)
        {
            String sName = listOfFiles[i].getAbsolutePath(); 
            if(listOfFiles[i].isFile() && fileOfInterest(sName))            
                    processFile(listOfFiles[i]);              
            else if(FamilyModuleHelper.isPathOfInterest(sName) && listOfFiles[i].isDirectory()) 
                crawlDirectory(listOfFiles[i].getAbsolutePath());            
        }      
    }
    
    public boolean fileOfInterest(String sName) 
    {
      if(!FamilyModuleHelper.isPathOfInterest(sName))
          return false;  
     
      if(sName.contains("/link/")  || sName.contains("/association/"))
          return false;
      if (sName.endsWith("VO.xml"))
          return true;
      return false;
    }
    
    public void processFile(File f) {
        try{
            
          XMLDocument doc = XMLParserHelper.getXMLDocument(f.getAbsolutePath());
            NodeList viewObjects = doc.getElementsByTagName("ViewObject");
            if(viewObjects == null || viewObjects.getLength() != 1) 
            {
//              System.out.println("WARNING:  Unexpected number of view objects found for file:  " +f);
//              System.out.println("Skipping...");
              return;
            }
            Node viewObject = viewObjects.item(0);
            String baseVo = XMLParserHelper.getAttributeValue(viewObject, "Extends");
            HashSet<String> subVos = new HashSet<String>();
            if(!FamilyModuleHelper.isEmpty(baseVo)) {
                
                if(base_subVos.containsKey(baseVo))
                    subVos = base_subVos.get(baseVo);
                subVos.add(FamilyModuleHelper.getRelativeFileName(f.getAbsolutePath()));
                base_subVos.put(baseVo,subVos);
                
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void writeSerializedHashMapToDB() throws Exception{
        try{
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(base_subVos);
            oos.close();
            
            System.out.println("Writing Serialized HashMap to database...");
            Connection con = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","codescan","codescan");
            con.setAutoCommit(false);
            Statement st = con.createStatement();
            st.executeUpdate("DELETE FROM SERIALIZEDREFERENCES WHERE NAME = 'BaseSubVos'");
            st.close();
            PreparedStatement ps = con.prepareStatement("INSERT INTO SERIALIZEDREFERENCES(NAME, COUNT_REFERENCES, LAST_UPDATE_DATE, SERIALIZED_OBJECT) values (?,?,?,?)");
            
            ps.setString(1,"BaseSubVos");
            ps.setInt(2,base_subVos.size());
            ps.setString(3,(new Date()).toString());
            ps.setBytes(4,baos.toByteArray());    
            ps.execute();
            ps.close();
            con.commit();
            con.close();
            
        } catch(Exception e) {
            e.printStackTrace();
            Mail.sendMail("sudipti.gupta@oracle.com", e.getMessage(),
                          "Could not write serialized basevo_subvo References Map. Please review and try again.");
        }
    }
    
}

