package oracle.apps;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;

import java.io.InputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;


public class stuckThreadReport {
    private BufferedWriter htmlfilewriter;
   
   
    public stuckThreadReport() {
        super();
    }
    public static void main(String[] args) {
        String htmlFileName = args[0];
        String numberofProducts=args[1];
        String family = args[2];
        String env1=args[3];
        String env2=args[4];
                                
//        String htmlFileName = "CRMSTSTUCKREPORT.html";
//        String numberofProducts="22";
//        String family = "CRM";
//        String env1="ST3B12U";
//        String env2="ST3B12C";
        //CRMSTSTUCKREPORT.html 22 CRM ST3B12U ST3B12C
        
        
        stuckThreadReport stuckThreadReport = new stuckThreadReport();
       
        Connection conn=null; 
        Statement stmt=null;
        
        try {
        DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
        conn = DriverManager.getConnection("jdbc:oracle:thin:@den65024fwks.us.oracle.com:1522:den65024","fusion","fusion");
        stmt = conn.createStatement();
        } catch (Exception e){System.out.println(e);}
        try {
        stuckThreadReport.startHtml(htmlFileName,numberofProducts,family,conn,stmt,env1,env2);
        } catch(Exception e){
            System.out.println(e);
                
        }
        try {
            conn.close();
            stmt.close();
        } catch(Exception e){System.out.println(e);}
    }
    private void startHtml(String filename,String numberofproducts,String family,Connection conn,Statement stmt,String env1,String env2) throws Exception
    {
        StringBuffer sb = new StringBuffer();
        HashMap stringMap = new HashMap();
        HashMap serverMap=new HashMap();
        HashMap envMap=new HashMap();
        ArrayList<String> finArray = new ArrayList<String>();
        ArrayList<String> finServerArray = new ArrayList<String>();
        ArrayList<String> finEnvArray = new ArrayList<String>();
        if (family.equals("FIN")) {
          
            finEnvArray.add("ST3B15");
          finEnvArray.add("ST3B16");
          envMap.put("FIN", finEnvArray);
            
            finArray.add("AP-1");
            finArray.add("AP-2");
            finArray.add("AR");
            finArray.add("GL-1");
            finArray.add("GL-2");
            finArray.add("Common");
            finArray.add("ESS-1");
            finArray.add("ESS-2");
          finArray.add("SOA1");
          finArray.add("SearchServer");
            stringMap.put("FIN", finArray);

            finServerArray.add("apserver1.xlsx");
            finServerArray.add("apserver2.xlsx");
            finServerArray.add("arserver1.xlsx");
            finServerArray.add("glserver1.xlsx");
            finServerArray.add("glserver2.xlsx");
            finServerArray.add("cmnserver1.xlsx");
            finServerArray.add("essserver1.xlsx");
            finServerArray.add("essserver2.xlsx");
          finServerArray.add("soaserver1.xlsx");
          finServerArray.add("searchserver1.xlsx");
            serverMap.put("FIN", finServerArray);
        }
      /*  if (family.equals("CRM")) {
            
            finEnvArray.add("ST3B12U");
            finEnvArray.add("ST3B12C");
            envMap.put("CRM", finEnvArray);
            finArray.add("Admin");
            finArray.add("Analytics(1)");
            finArray.add("Common(1)");
            finArray.add("Common(2)");
            finArray.add("Performance(1)");
            finArray.add("Performance(2)");
            finArray.add("ContractManagement(1)");
            finArray.add("Customer(1)");
            finArray.add("Customer(2)");
            finArray.add("EmailMarketing(1)");
            finArray.add("Marketing(1)");
            finArray.add("Marketing(2)");
            finArray.add("OrderCapture(1)");
            finArray.add("Outlook(1)");
            finArray.add("Sales(1)");
            finArray.add("Sales(2)");
            finArray.add("Search(1)");
            finArray.add("ess(1)");
            finArray.add("ess(2)");
            finArray.add("soa(1)");
            finArray.add("soa(2)");
           
            stringMap.put("CRM", finArray);

            finServerArray.add("crmadminserver.xlsx");
            finServerArray.add("crmanalyticsserver1.xlsx");
            finServerArray.add("crmcommonserver1.xlsx");
            finServerArray.add("crmcommonserver2.xlsx");
            finServerArray.add("crmperformanceserver1.xlsx");
            finServerArray.add("crmperformanceserver2.xlsx");
            finServerArray.add("crmcontractmanagmentserver1.xlsx");
            finServerArray.add("crmcustomerserver1.xlsx");
            
            finServerArray.add("crmcustomerserver2.xlsx");
            finServerArray.add("crmemailmarketingserver1.xlsx");
            finServerArray.add("crmmarketingserver1.xlsx");
            finServerArray.add("crmmarketingserver2.xlsx");
            finServerArray.add("crmordercaptureserver1.xlsx");
            finServerArray.add("crmoutlookserver1.xlsx");
            finServerArray.add("crmsalesserver1.xlsx");
            finServerArray.add("crmsalesserver2.xlsx");
            finServerArray.add("crmsearchserver1.xlsx");
            finServerArray.add("crmessserver1.xlsx");
            finServerArray.add("crmessserver2.xlsx");
            finServerArray.add("crmsoaserver1.xlsx");
            finServerArray.add("crmsoaserver2.xlsx");
            serverMap.put("CRM", finServerArray);
        }
        
        if (family.equals("HCM")) {
            finEnvArray.add("ST3B12U");
            finEnvArray.add("ST3B12C");
            envMap.put("HCM", finEnvArray);
            finArray.add("Admin");
            finArray.add("Benefits");
            finArray.add("Compensation");
            finArray.add("Core");
            finArray.add("Core Setup");
            finArray.add("ESS(1)");
            finArray.add("Ford");
            finArray.add("Payroll");
            finArray.add("Search");
            finArray.add("SOA(1)");
            finArray.add("SOA(2)");
            finArray.add("Talent");
           
            stringMap.put("HCM", finArray);

            finServerArray.add("hcmadminserver.xlsx");
            finServerArray.add("hcmbenserver.xlsx");
            finServerArray.add("hcmcompserver.xlsx");
            finServerArray.add("hcmcoreserver.xlsx");
            finServerArray.add("hcmcoresetupserver.xlsx");
            finServerArray.add("hcmess1server.xlsx");
            finServerArray.add("hcmfordserver.xlsx");
            finServerArray.add("hcmpayrollserver.xlsx");
            
            finServerArray.add("hcmsearchserver.xlsx");
            finServerArray.add("hcmsoa1server.xlsx");
            finServerArray.add("hcmsoa2server.xlsx");
            finServerArray.add("hcmtalentserver.xlsx");
           
            serverMap.put("HCM", finServerArray);
        }
        
        if (family.equals("FS")) {
            
            finEnvArray.add("ST3B12U");
            finEnvArray.add("ST3B12C");
            envMap.put("FS", finEnvArray);
            finArray.add("ADF");
            finArray.add("ESS");
            
           
            stringMap.put("FS", finArray);

            finServerArray.add("fscadfserver.xlsx");
            finServerArray.add("fsuessserver.xlsx");
          
           
            serverMap.put("FS", finServerArray);
        }*/
        if (family.equals("IC")) {
            finArray.add("ADF");
            finArray.add("ADMIN");
            finArray.add("ESS");
            finArray.add("SOA1");
           
            stringMap.put("IC", finArray);
            
            finEnvArray.add("ST3B15");
            finEnvArray.add("ST3B16");
            envMap.put("IC", finEnvArray);

            finServerArray.add("icadfserver.xlsx");
            finServerArray.add("icadminserver.xlsx");
            finServerArray.add("icessserver.xlsx");
            finServerArray.add("icsoa1server.xlsx");
          
            serverMap.put("IC", finServerArray);
        }
        /*if (family.equals("PRC")) {
            finArray.add("ESS");
            finArray.add("PROC(1)");
            finArray.add("PROC(2)");
            finArray.add("SOA1");
            finArray.add("SP");
            stringMap.put("PRC", finArray);
            finEnvArray.add("ST3B12U");
            finEnvArray.add("ST3B12C");
            envMap.put("PRC", finEnvArray);
            finServerArray.add("prcessserver1.xlsx");
            finServerArray.add("prcprocserver1.xlsx");
            finServerArray.add("prcprocserver2.xlsx");
            finServerArray.add("prccsoaserver1.xlsx");
            finServerArray.add("prcspserver1.xlsx");
            serverMap.put("PRC", finServerArray);
        }
        if (family.equals("SCM")) {
            finArray.add("Admin");
            finArray.add("APS");
            finArray.add("COSTING");
            finArray.add("DOO");
            finArray.add("GLOBAL");
            finArray.add("LOGISTICS");
            finArray.add("PIM");
            finArray.add("ESS1");
            finArray.add("ESS2");
            finArray.add("SOA1");
            finArray.add("SOA2");
            stringMap.put("SCM", finArray);
            finEnvArray.add("ST3B12U");
            finEnvArray.add("ST3B12C");
            envMap.put("SCM", finEnvArray);
            finServerArray.add("scmadminserver.xlsx");
            finServerArray.add("scmapsserver.xlsx");
            finServerArray.add("scmcstserver.xlsx");
            finServerArray.add("scmdooserver.xlsx");
            finServerArray.add("scmgblserver.xlsx");
            
            finServerArray.add("scmlogserver.xlsx");
            finServerArray.add("scmpimserver.xlsx");
            finServerArray.add("scmessserver1.xlsx");
            finServerArray.add("scmessserver2.xlsx");
            finServerArray.add("scmsoaserver1.xlsx");
            finServerArray.add("scmsoaserver2.xlsx");
            serverMap.put("SCM", finServerArray);
        }*/
        PreparedStatement pst=null;
        PreparedStatement datepst=null;
        ResultSet rs=null;
        ResultSet daters=null;
        Process p = Runtime.getRuntime().exec("rm -f "+filename);
        checkProcess(p, null, "remove html file", false);
        ArrayList<String> envs = (ArrayList<String>)envMap.get(family);
        int envSize= envs.size();
       // htmlfilewriter = new BufferedWriter(new FileWriter(filename));
        FileWriter fw = new FileWriter(filename);
        BufferedWriter bw = new BufferedWriter(fw);
        sb.append("<H1 style=\"font-family:arial;color:red;\">Stuck Thread Counts by Day - "+family+"</H1>");
        sb.append("<tr><tr>");
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy hh:mm:ss");
        sb.append("<i><H7 style=\"font-family:arial;color:33CCFF;\">Last Updated - "+sdf.format(cal.getTime())+"</H7><i>");
        sb.append("<tr><tr>");
        sb.append("<table border=1><tr>");
         sb.append("<td bgcolor=CCFFFF></td>");
                for (int i = 0; i < envSize; i++) {
                    sb.append("<td colspan=" +
                              numberofproducts +
                              " align=center bgcolor=CCFFFF style=\"font-family:arial;color:red;font-size:20px;\">" +
                              envs.get(i) + "</td>");
                }
                sb.append("</tr>");
                sb.append("<tr>");
        sb.append("<td bgcolor=FFCC99>Date</td>");
      
        ArrayList<String> headings = (ArrayList<String>)stringMap.get(family);
        int headsize=headings.size();
        for (int i = 0; i < envSize; i++) {
            for (int j = 0; j < headsize; j++) {
                sb.append("<td bgcolor=FFCC99>" + headings.get(j) + "</td>");
            }
            sb.append("<td bgcolor=FFCC99>Total</td>");
        }
//        for (int i = 0; i < headsize; i++) {
//            sb.append("<td bgcolor=FFCC99>" + headings.get(i) + "</td>");
//        }
//        sb.append("<td bgcolor=FFCC99>Total</td>");
        sb.append("</tr>");   
       
        
        try {
             pst =
                conn.prepareStatement("select count(*) as rowcount,datetimestring from stuckthreads where product=? and env=? and family=? and datetimestring=? group by datetimestring");
         
               datepst = conn.prepareStatement("select distinct datetimestring,datetime from stuckthreads where family=? order by datetime desc");
         
         }

        catch (Exception e) {
            System.out.println(e);
        }
        
        datepst.setString(1,family);
        daters= datepst.executeQuery();
        while (daters.next()) {
            sb.append("</tr>");
            sb.append("<td align=center>" + daters.getString(1) + "</td>");
            ArrayList<String> servers =
                (ArrayList<String>)serverMap.get(family);
            int serversize = servers.size();
            int runningtotal = 0;
            for (int j = 0; j < envSize; j++) {
            for (int i = 0; i < serversize; i++) {
                pst.setString(1, servers.get(i));
                pst.setString(2, envs.get(j));
                pst.setString(3, family);
                pst.setString(4, daters.getString(1));
                rs = pst.executeQuery();

                rs.next();
                if (rs.getRow() > 0) {
                    sb.append("<td align=center><b>" + rs.getInt(1) + "<b></td>");
                    runningtotal = runningtotal + rs.getInt(1);
                } else {
                    sb.append("<td align=center>0</td>");
                }


            }
            sb.append(" <td align=center style=\"font-family:arial;color:red;font-size:20px;\"><b>" +
                      runningtotal + "<b></td> ");
            runningtotal = 0;
            }
//            for (int i = 0; i < serversize; i++) {
//                pst.setString(1, servers.get(i));
//                pst.setString(2, env2);
//                pst.setString(3, family);
//                pst.setString(4, daters.getString(1));
//                rs = pst.executeQuery();
//                rs.next();
//                if (rs.getRow() > 0) {
//                    sb.append("<td align=center><b>" + rs.getInt(1) + "<b></td>");
//                    runningtotal = runningtotal + rs.getInt(1);
//                } else {
//                    sb.append("<td align=center>0</td>");
//                }
//
//
//            }
//            sb.append(" <td align=center  style=\"font-family:arial;color:red;font-size:20px;\"><b>" +
//                      runningtotal + "<b></td> ");
            sb.append("</tr>");
        }
        pst.close();
        rs.close();
        datepst.close();
        daters.close();
       
          
       
        sb.append("</table>");

        bw.write(sb.toString());
        bw.close();
        fw.close();
    }
   
    private void checkProcess(Process p, File f, String description, boolean isDiff) throws Exception
    {
        int exitCode = p.waitFor();
        if(!isDiff && exitCode != 0)
        {
            if(f != null)
                System.out.println("There was an error when trying to " +description+ " for file : " +f.getAbsolutePath());
            else
                System.out.println("There was an error when trying to " +description);
        }

        
    }
    
  
    
    
    
    
    
    
}
