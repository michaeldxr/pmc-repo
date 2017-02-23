package oracle.apps.fixer;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.HashMap;
import java.util.HashSet;

import java.util.Iterator;

import oracle.apps.helpers.Mail;

public class PrintMap {
    
    static HashMap<String,HashSet<String>> externalReferences = new HashMap<String,HashSet<String>>();
    static BufferedWriter writer;
    
    public static void main(String[] args) throws Exception {
        
        String codeline = "PREARU";
//        if(args.length >0)
//            codeline = args[0].trim();
        
        writer = new BufferedWriter(new FileWriter("externalReferences1.txt"));
        
        writer.write("\n\nFIN References:\n\n");        
        readExternalReferencesFromDB(codeline,"FIN");
        writeMaps();
        writer.close();
        
        writer = new BufferedWriter(new FileWriter("externalReferences2.txt"));
        writer.write("\n\nHCM References:\n\n");        
        readExternalReferencesFromDB(codeline,"HCM");
        writeMaps();
        writer.close();
        
        writer = new BufferedWriter(new FileWriter("externalReferences3.txt"));
        writer.write("\n\nCRM References:\n\n");        
        readExternalReferencesFromDB(codeline,"CRM");
        writeMaps();
        writer.close();
        
        writer = new BufferedWriter(new FileWriter("externalReferences4.txt"));
        writer.write("\n\nSCM References:\n\n");        
        readExternalReferencesFromDB(codeline,"SCM");
        writeMaps();
        writer.close();
        
        writer = new BufferedWriter(new FileWriter("externalReferences5.txt"));
        writer.write("\n\nALL References:\n\n");        
        readExternalReferencesFromDB(codeline,"ALL");
        writeMaps();        
        writer.close();
        
        writer = new BufferedWriter(new FileWriter("externalReferences6.txt"));
        writer.write("\n\nATF References:\n\n");        
        readExternalReferencesFromDB(codeline,"ATF");
        writeMaps();        
        writer.close();
        
        writer = new BufferedWriter(new FileWriter("externalReferences7.txt"));
        writer.write("\n\nPRC References:\n\n");        
        readExternalReferencesFromDB(codeline,"PRC");
        writeMaps();        
        writer.close();
        
        writer = new BufferedWriter(new FileWriter("externalReferences8.txt"));
        writer.write("\n\nIC References:\n\n");        
        readExternalReferencesFromDB(codeline,"IC");
        writeMaps();        
        writer.close();
        
        writer = new BufferedWriter(new FileWriter("externalReferences9.txt"));
        writer.write("\n\nPRJ References:\n\n");        
        readExternalReferencesFromDB(codeline,"PRJ");
        writeMaps();        
        writer.close();
        
        writer = new BufferedWriter(new FileWriter("externalReferences10.txt"));
        writer.write("\n\nFSCM References:\n\n");        
        readExternalReferencesFromDB(codeline,"FSCM");
        writeMaps();        
        writer.close();
    }
   
   
    public static void readExternalReferencesFromDB(String codeline, String family) {
        try{
            System.out.println("Reading Serialized HashMap from database...");
            Connection con = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","fintech","fintech");
            Statement st = con.createStatement();
            String stmt = "SELECT REFERENCESMAP FROM SERIALIZEDEXTERNALREFERENCES where codeline ='" + codeline + "' and family = '" + family + "'";
            ResultSet rs = st.executeQuery(stmt);
            while(rs.next()) {
                byte[] buf = rs.getBytes(1);
                if(buf != null) {
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buf));
                    externalReferences = (HashMap<String,HashSet<String>>)ois.readObject();
                    ois.close();
                }
            }
            rs.close();
            st.close();
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            Mail.sendMail("sudipti.gupta@oracle.com", e.getMessage(),
                          "Could not read external Enum References Serialized Map. Please review and try again.");
            System.exit(1);
        }
    }
    
    
    public static void writeMaps() {
        
        try{             
            for(Iterator<String> it = externalReferences.keySet().iterator(); it.hasNext();){
                String s = it.next();
                writer.write(s + " --> " + externalReferences.get(s).toString() + "\n");
            }            
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
}
