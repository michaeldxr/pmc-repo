package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

import oracle.apps.helpers.FamilyModuleHelper;

public class ELHelpProviderDBWrapperLRG {
    
  String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
  static String statsUname = "codescan";
  static String statsPwd = "codescan";
  static String pwd;
  static String series = "";
  
  public static void main(String[] args) {             
   
    if (args.length < 2) {
        System.out.println("Need to specify both 1.directory and 2.series");
        System.exit(1);
    }

    pwd = args[0];
    series = args[1];

    ELHelpProviderDBWrapperLRG dbWrapper = new ELHelpProviderDBWrapperLRG();

    try {         
        System.out.println("Updating DB....");
        dbWrapper.updateDatabase();          
    } catch (Exception e) {
        e.printStackTrace();        
    }
  }  
  
  
  public void updateDatabase() throws Exception {
      
    Connection con = null;
    try{
        String fileName = pwd+ "/ELHelpProvider_LRG.csv";
        
        BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
        String line = fileReader.readLine();
       
        con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
        con.setAutoCommit(false);

        CallableStatement cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
           
          System.out.println("First delete records from DB...");
          Statement stmt = con.createStatement();
          
          String sql  = "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'ELHelpProvider' and SERIES ='" + series+"'";
        
          stmt.executeUpdate(sql);           
          stmt.close();
        
        if(line == null) {
            con.commit();   
            return;
        }
        
        System.out.println("Now inserting values...");
        
        String[] labelSeries = null;
        String prevLabel = "";
        
        cstmt.registerOutParameter(1, Types.INTEGER);
        int count = 0;
        while((line = fileReader.readLine()) != null) {
            
          String[] parts = line.split(",");
          if(parts.length < 4) continue;
            
          String family1 = getFamilyFromJar(parts[2].trim() + " " + parts[3].trim());
            
            if(prevLabel != parts[0].trim()) {
                labelSeries = FamilyModuleHelper.getLabelSeries(parts[0].trim());
                prevLabel = parts[0].trim();
            }
            
          cstmt.setString(2, family1); //family
          cstmt.setString(3, ""); //module
          cstmt.setString(4, ""); //product
          cstmt.setString(5, parts[2].trim()); //filename
          cstmt.setString(6, "ELHelpProvider"); //issue
          cstmt.setString(7, parts[1].trim()); //subissue = prefix
          cstmt.setString(8,parts[3].trim()); //description = jar2
          cstmt.setString(9,labelSeries[0]); //series
          cstmt.setString(10,labelSeries[1]); //label
         
          cstmt.execute();          
          count++;
        }
        con.commit();
        cstmt.close();
        System.out.println("Total count for ELHelpProvider is "+count +" on series "+series);
    }catch(Exception e){
        e.printStackTrace();
        if(con != null)
            con.rollback();
    }finally {
         if(con != null)
             con.close();
    }
  }
  
  private String getFamilyFromJar(String jars){
    String family = "";
    if(jars.contains("AdfFin") || jars.contains("fusionapps/fin/"))
        family = "FIN";
    else if(jars.contains("AdfHcm") || jars.contains("SvcHcm") || jars.contains("fusionapps/hcm/"))
        family = "HCM";
    else if(jars.contains("AdfIc") || jars.contains("fusionapps/ic/"))
        family = "IC";
    else if(jars.contains("AdfContracts") || jars.contains("AdfCrm") || jars.contains("AdfCdm") 
            || jars.contains("AdfSales") || jars.contains("AdfMkt") ||
            jars.contains("AdfMkl") || jars.contains("AdfZpm") ||
            jars.contains("AdfBase") || jars.contains("AdfOppty") ||
            jars.contains("AdfPartner") || jars.contains("AdfApp") ||
            jars.contains("AdfFoundation") || jars.contains("AdfCustomer") ||
            jars.contains("AdfOrder") || jars.contains("AdfOc") 
            || jars.contains("AdfExtn") || jars.contains("AdfCc") || jars.contains("fusionapps/crm/") )
        family = "CRM";
    else if(jars.contains("AdfScm") || jars.contains("AdfEgp") ||
            jars.contains("AdfEgo") || jars.contains("AdfCmr") ||
            jars.contains("AdfInv") || jars.contains("AdfRcv") ||
            jars.contains("AdfWsh") || jars.contains("AdfCst") ||
            jars.contains("AdfMsc") || jars.contains("AdfDoo") ||
            jars.contains("AdfJmf") || jars.contains("fusionapps/scm/")
            || jars.contains("AdfQp"))
        family = "SCM";
    else if(jars.contains("AdfPjo") || jars.contains("AdfPjs") ||
            jars.contains("AdfPjc") || jars.contains("AdfPjf") || 
            jars.contains("AdfPjt") || jars.contains("SvcPjt") || 
            jars.contains("AdfPjr") || jars.contains("AdfPje") || jars.contains("fusionapps/prj/"))
        family = "PRJ";
    else if(jars.contains("AdfPrc") || jars.contains("fusionapps/prc/"))
        family = "PRC";
    else if(jars.contains("AdfFunctional") || jars.contains("AdfSetup") || 
            jars.contains("AdfFeature") || jars.contains("AdfResources") || 
            jars.contains("AdfTopology") || jars.contains("AdfAsk") || jars.contains("AdfAsm") || jars.contains("fsm/"))
        family = "FSM";
    else if(jars.contains("/atf/") || jars.contains("AdfAtk") || jars.contains("SvcAtk"))
        family="ATF";
    else
        family="XYZ"; //unknown - if this is seen in angrybirds, then this jar name lists need to be updated
    
    return family;
  }
  
}