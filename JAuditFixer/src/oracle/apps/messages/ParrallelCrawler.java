package oracle.apps.messages;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.NullEntityResolver;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;
import oracle.xml.parser.v2.XMLNode;

import org.w3c.dom.NodeList;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;

public class ParrallelCrawler extends JoesBaseClass implements Runnable
{
    static final LinkedList<String> m_workQueue = new LinkedList<String>();

    static final LinkedList<String> m_pkbList = new LinkedList<String>();
    static final LinkedList<String> m_javaList = new LinkedList<String>();
    static final LinkedList<String> m_xmlList = new LinkedList<String>();
    static final LinkedList<String> m_lpcList = new LinkedList<String>();
    static final LinkedList<String> m_csharpList = new LinkedList<String>();
    static final LinkedList<String> m_stringBundleFileList = new LinkedList<String>();
    static final LinkedList<String> m_messageFileList = new LinkedList<String>();
    static int default_release = 13;
    static final ArrayList<Thread> m_workers = new ArrayList<Thread>();
    static String m_phase;
    static final HashSet<String> large_files = new HashSet<String>();
    public static final HashSet<String> m_diagMessageSet = new HashSet<String>();
    public static final HashSet<String> m_messageSet = new HashSet<String>();
    public static final HashSet<String> mrtPreviouslyApprovedMessageSet = new HashSet<String>();
    public static final HashSet<String> m_stringRepMessageSet = new HashSet<String>();
    public static final HashMap<String, ArrayList<String>> m_stringRepMessageMap = new HashMap<String, ArrayList<String>>();
    public static final HashMap<String, String> m_messageToSeedFileMap = new HashMap<String, String>();
    public static final HashMap<String, ArrayList<String>> m_messageToMessageDetailMap = new HashMap<String, ArrayList<String>>();
    public static HashSet<String> m_unreferencedMessageSet;
    public static HashSet<MessageUsage> m_undefinedMessageSet = new HashSet<MessageUsage>();
    public static HashMap<String,ArrayList<String>> m_mrtMessageToMessageDetailMap = new HashMap<String,ArrayList<String>>();
    public static HashSet<String> m_undefined_exemptionlist = new HashSet<String>();
    public static HashSet<String> m_unused_exemptionlist= new HashSet<String>();
    public static HashSet<String> m_msg_mrt_review_exemptionlist = new HashSet<String>();
    public static HashSet<String> m_msg_not_in_mrt_exemptionlist = new HashSet<String>();
    public static final ArrayList<MessageUsage> m_messageUsagesList = new ArrayList<MessageUsage>();
    public static String sSeries;
    public static String sLabel;
    public static String mode;
    public static String crawlPath;
    private class MessageUsage
    {
        public String m_messageName;
        public String m_usageType = "TBD";
        public String m_usageLocation;
        public String m_isDiagnosticMessage;

        public MessageUsage(String sMessageName, String sUsageType, String sUsageLocation)
        {
            m_messageName = sMessageName;
            m_usageType = sUsageType;
            m_usageLocation = sUsageLocation;
            m_isDiagnosticMessage = String.valueOf(m_diagMessageSet.contains(sMessageName));
        }

        public String toString()
        {
            int index = m_usageLocation.lastIndexOf('.');
            String type = "NONE";
            if(index > 0)
                type = m_usageLocation.substring(index);
         //   m_usageLocation = m_usageLocation.substring(m_usageLocation.indexOf("fusionapps"));
            String sFamily = FamilyModuleHelper.getFamily(m_usageLocation);
            String sModule = getProduct(m_usageLocation);
         // return sFamily +"," +sModule +"," +m_messageName + "," + m_usageLocation +"," + m_usageType + "," +m_isDiagnosticMessage +","  +type;
            return sFamily +"," +sModule +"," +m_messageName + "," + m_usageLocation;
        }
        
    }
    
    public ParrallelCrawler(JoesBaseClass.CRAWL_TYPE crawL_TYPE)
    {
        super(crawL_TYPE);
    }

    protected void processFile(File fName, boolean bDoADE)
    {
        String s;
        try
        {
            s = fName.getAbsolutePath();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return;
        }

        if (s.endsWith(".pkb") || s.endsWith(".pkh") || s.endsWith(".sql"))
        {
            //System.out.println("found pkb --> " + s);
            synchronized (m_pkbList)
            {
                m_pkbList.add(s);
            }
            return;
        }

        if (s.endsWith(".java"))
        {
            //System.out.println("found java --> " + s);
            synchronized (m_javaList)
            {
                m_javaList.add(s);
            }
            return;
        }
        
        if(s.endsWith(".cs")) {
          synchronized (m_csharpList)
          {
              m_csharpList.add(s);
          }
          return;
        }
        
      if (s.endsWith("Bundle.xlf"))
      {
          //System.out.println("found java --> " + s);
          synchronized (m_stringBundleFileList)
          {
              m_stringBundleFileList.add(s);
          }
          return;
      }

        if (s.endsWith("MessageSD.xml"))
        {
            //System.out.println("found message file --> " + s);
            synchronized (m_messageFileList)
            {
                m_messageFileList.add(s);
            }

            return;
        }

        if (s.endsWith(".xml") || s.endsWith(".jspx") || s.endsWith(".jsff") ||
            s.endsWith(".bpel") || s.endsWith(".xdm") || s.endsWith(".xsl"))
        {
            synchronized (m_xmlList)
            {
                m_xmlList.add(s);
            }

            return;
        }

        if (s.endsWith(".oc") || s.endsWith(".lc") || s.endsWith(".lpc") ||
            s.endsWith(".opc") || s.endsWith(".h"))
        {
            synchronized (m_lpcList)
            {
                m_lpcList.add(s);
            }

            return;
        }

    }


    public void run()
    {
        String work = null;

        try
        {
            while (true)
            {

                synchronized (m_workQueue)
                {
                    while (m_workQueue.size() <= 0)
                        m_workQueue.wait();

                    work = m_workQueue.remove();
                }

                if (m_phase.equals("SCANNING"))
                {
                    long start = System.currentTimeMillis();

                    crawlDirectory(work, false);

                    long end = System.currentTimeMillis();
                    double elasped = (end - start) / 60000.00;
                } 
                else if (m_phase.equals("PROCESS_MESSAGE_FILES"))
                {
                    processMessageFile(work);
                } 
                else if (m_phase.equals("PROCESS_PKB_FILES"))
                {
                    processPkbFile(work);
                } 
                else if (m_phase.equals("PROCESS_JAVA_FILES"))
                {
                    processJavaFile(work);
                } 
                else if (m_phase.equals("PROCESS_LARGE_FILES"))
                {
                    processLargeFile(work);
                } 
                else if (m_phase.equals("PROCESS_EXEMPTION_FILES"))
                {
                    processExemptionFile(work);
                } 
                else if (m_phase.equals("PROCESS_XML_FILES"))
                {
                    processXmlFile(work);
                } else if (m_phase.equals("PROCESS_CSHARP_FILES"))
                {
                    processCSharpFile(work);
                } else if (m_phase.equals("PROCESS_LPC_FILES"))
                {
                    processLpcFile(work);
                } else if (m_phase.equals("PROCESS_XLF_FILES"))
                {
                    processBundleFile(work);
                }else if (m_phase.equals("DONE"))
                {
                    return;
                }

            }

            //            System.out.println("*************************************************************************************");
            //            System.out.println("Crawled this directory --> " + path);
            //            System.out.printf("Total Time is %.2f minutes.\n", elasped);

        } catch (Exception e)
        {
            e.printStackTrace();
            if (work != null)
                System.out.println("work is:  " + work);
            return;
        }
    }

    protected String getSummaryReport()
    {
        return "TODO getSummaryReport()";
    }

    protected String getSummaryReportSubject()
    {
        return "TODO getSummaryReportSubject()";
    }

    static final int NUM_THREADS = 200;

    private static void initThreadPool()
    {
        for (int i = 0; i < NUM_THREADS; i++)
        {
            ParrallelCrawler pc =
                new ParrallelCrawler(CRAWL_TYPE.MESSAGE_SCANNER);
            Thread t = new Thread(pc);
            m_workers.add(t);

            t.start();
        }
    }

    private static void cleanupThreadPool()
    {
        for (int i = 0; i < NUM_THREADS; i++)
        {
            synchronized (m_workQueue)
            {
                m_workQueue.add("PLEASE FINISH");
                m_workQueue.notify();
            }
        }

    }

    public static ArrayList<String> parseMRTLine(String l)
    {
        ArrayList<String> retval = new ArrayList<String>();
        
        int start = 1;
       // System.out.println(l.length());
        for(int i = 1; i < l.length()-1; i++)
        {
            char b = l.charAt(i+1);
            char c = l.charAt(i);
            if(c == '"' && b == ',')
            {
                String s = l.substring(start, i);
                retval.add(s);
                
                i++;
                
                while(i < l.length() && (c = l.charAt(i)) != '"')
                {
                    i++;
                }
                
                i++;
                
                start = i;
            }
            
          if(b == '"' && i == l.length()-2){
            String s = l.substring(start, i+1);
            
            retval.add(s);
            
            i++;
          }
        //  System.out.println("i: "+i+"  "+"l.length()-1: "+ (l.length()-1));
        //  System.out.println(i<l.length()-1);
        }
        
        return retval;
    }
    
    private static int versionToRelease(String version){
      if(version.contains("Release 1 (Fusion 1.0)"))
        return 1;
      else if(version.contains("Release 2"))
        return 2;
      else if(version.contains("Release 3"))
        return 3;
      else if(version.contains("Release 4"))
        return 4;
      else if(version.contains("Release 5"))
        return 5;
      else if(version.contains("Release 6"))
        return 6;
      else if(version.contains("Release 7"))
        return 7;
      else if(version.contains("Release 8"))
        return 8;
      else if(version.contains("Release 9"))
        return 9;
      else if(version.contains("Release 10"))
        return 10;
      else if(version.contains("Release 11"))
        return 11;
      else if(version.contains("Release 12"))
        return 12;
      else if(version.contains("Release 13"))
        return 13;
      else if(version.contains("Release 14"))
        return 14;
      else if(version.contains("Release 15"))
        return 15;
      else if(version.contains("Release 16"))
        return 16;
      else if(version.equals("Fusion 2.0"))
        return 16;
      else if(version.equals(""))
        return 16;
      else
        return default_release;
    }
    
    private static String getFamily(String sFamily){
      if(sFamily.equals("Financials"))
        return "FIN";
      else if(sFamily.equals("Setup"))
        return "SETUP";
      else if(sFamily.equals("Common"))
        return "COM";
      else if(sFamily.equals("Procurement"))
        return "PRC";
      else if(sFamily.equals("Projects"))
        return "PRJ";
      else if(sFamily.equals("Higher Education"))
        return "HED";
      else
        return sFamily;
    } 
    
    private static ArrayList getAllFamilies(){
      ArrayList<String> families = new ArrayList<String>();
      families.add("atf");
      families.add("com");
      families.add("crm");
      families.add("fin");
      families.add("hcm");
      families.add("hed");
      families.add("scm");
      families.add("ic");
      families.add("prc");
      families.add("prj");
      families.add("grc");
      families.add("fscm");
      
      return families;
    }
    public static void reviewMRTData()
    {
      ArrayList family = null;
      if(sSeries.contains("V2MIB")){
        family = getAllFamilies(); 
      }
      else{
         family = LRGUtil.getFamiliesFromSeries(sSeries);
      }
      String module = null;
        try
        {
            PrintWriter pw = new PrintWriter("mrt_message_review.csv");
            pw.println("Series,Label,Family,Product,Message,File Name,Issue,Sub-Issue,Description,Exists in SD.xml/GenBundle.xlf,Message Text Same,Message User Details Same,Message Cause Same,Message Action Same,Message Admin Details Same,Message Admin Action Same,Message Context Same,V.From,V.To,Obsolete,Dev Manager,Product Manager,InfoDev Manager,Creation Date,Last Update Date, Last Exported Date");
            HashSet<String> mrtApprovedMessageSet = new HashSet<String>();
            String file = FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
            "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/exemptions/mrt_dump_file.csv";
            System.out.println("MRT dump file location: "+file);
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            
            String s = br.readLine();
            
            while((s = br.readLine()) != null)
            {
                s = s.trim();
                
                while(s.endsWith("\"") == false || s.endsWith("\"\"") == true)
                {
                    s += br.readLine();
                    s = s.trim();
                }
                
              //  ArrayList<String> list = parseMRTLine(s);
              String s1 = s.substring(1,s.length()-1);
              String es = "\",\"";
              String[] list1 = s1.split(es);
              List<String> list = Arrays.asList(list1);
                
                if(list.size() < 28)
                {
                    System.out.println("Whoa, about to crash.");
                    System.out.println(s);
                    continue;
                }
                
                String sFamily = list.get(0);
                String sProduct = list.get(1);
                String sMessageName = list.get(2); 
                String sMessageStatus = list.get(6); //In order to enable comparison between the seed file and the MRT dump, storing "" instead of "-". MRT dump has "-" while seed file has "" for null values.
                String sMessageText = list.get(14).equals("-") ? "" : list.get(14);
                String sMessageUserDetails = list.get(15).equals("-") ? "" : list.get(15);
                String sMessageCause = list.get(16).equals("-") ? "" : list.get(16);
                String sMessageAction = list.get(17).equals("-") ? "" : list.get(17);
                String sMessageAdminDetails = list.get(18).equals("-") ? "" : list.get(18);
                String sMessageAdminAction = list.get(19).equals("-") ? "" : list.get(19);
                String sMessageContext = list.get(20).equals("-") ? "" : list.get(20);
              
                String sVFrom = list.get(7).equals("-") ? "" : list.get(7);
                String sVTo = list.get(8).equals("-") ? "" : list.get(8);
                String sIsObsolete = list.get(10).equals("-") ? "" : list.get(10);
                String sDevManager = list.get(21).equals("-") ? "" : list.get(21);
                String sProdManager = list.get(22).equals("-") ? "" : list.get(22);
                String sInfoDevManager = list.get(23).equals("-") ? "" : list.get(23);
                String sCreationDate = list.get(24).equals("-") ? "" : list.get(24);
                String sLastUpdateDate = list.get(25).equals("-") ? "" : list.get(25);
                String sLastExportDate = list.get(26).equals("-") ? "" : list.get(26);
                String sPreviouslyApproved = list.get(27).equals("-") ? "" : list.get(27);
              
                String description = "";
                String issue = null;
                String temp = null;
                String subIssue = null;
                sFamily = getFamily(sFamily);
                
                boolean seedFileExists;
              
                 int versionFrom = versionToRelease(sVFrom);
                 int versionTo = versionToRelease(sVTo);
              if(family.contains(sFamily.toLowerCase()) || family.contains(sFamily.toUpperCase())){
                  if(sMessageStatus.equalsIgnoreCase("APPROVED") && (versionTo >= default_release) && (versionFrom <= default_release)){
                    String review = sSeries + "," + sLabel+"," +sFamily +"," +sProduct +"," +sMessageName+",";
                    
                      if(m_messageSet.contains(sMessageName)){
                            String sSeedFile = m_messageToSeedFileMap.get(sMessageName);
                          
                            review = review + sSeedFile +",";
                            
                            temp = "Yes,";
                            
                            seedFileExists = true;
                            
                            module = FamilyModuleHelper.getModule(sSeedFile);
                            
                            ArrayList<String> sMessageDetailSD = m_messageToMessageDetailMap.get(sMessageName);
                            
                            boolean flag = sMessageText.equals(sMessageDetailSD.get(0));
                            
                            temp = temp + booleanToYesNo(flag) + ",";
                            
                            if(!flag)
                                description = "Message Text is not the same.";
                            
                            flag = sMessageUserDetails.equals(sMessageDetailSD.get(1));
                            
                            temp = temp + booleanToYesNo(flag) + ",";
                            
                            if(!flag)
                                description += " Message User Details is not the same.";
                            
                            flag = sMessageCause.equals(sMessageDetailSD.get(2));
                            
                            temp = temp + booleanToYesNo(flag) + ",";
                            
                            if(!flag)
                                description += " Message Cause is not the same.";
                            
                            flag = sMessageAction.equals(sMessageDetailSD.get(3));
                            
                            temp = temp + booleanToYesNo(flag) + ",";
                            
                            if(!flag)
                                description += " Message Action is not the same.";
                            
                            flag = sMessageAdminDetails.equals(sMessageDetailSD.get(4));
                            
                            temp = temp + booleanToYesNo(flag) + ",";
                            
                            if(!flag)
                                description += " Message Admin Details is not the same.";
                            
                            flag = sMessageAdminAction.equals(sMessageDetailSD.get(5));
                            
                            temp = temp + booleanToYesNo(flag) + ",";
                            
                            if(!flag)
                                description += " Message Admin Action is not the same.";
                            
                            flag = sMessageContext.equals(sMessageDetailSD.get(6));
                            
                            temp = temp + booleanToYesNo(flag) + ",";
                            
                            if(!flag)
                                description += " Message Context is not the same.";
                            
                            issue = "MRT Message Review";
                            subIssue = "Seed file is not in sync with MRT for message : "+sMessageName;
                            
                            review += issue + ","+ subIssue+","+ description + ","+temp;
                            
                           
                      }   
                      else if(m_stringRepMessageSet.contains(sMessageName)){
                      
                            ArrayList<String> sMessageDetailSD = m_stringRepMessageMap.get(sMessageName);
                            
                            String sSeedFile = sMessageDetailSD.get(2);
                        
                            review = review + sSeedFile +",";
                            
                            temp = "Yes,";
    
                            seedFileExists = true;
                             
                            boolean flag = sMessageText.equals(sMessageDetailSD.get(0));
                            
                            if(!flag)
                                description += " Message Text is not the same.";
                            
                            temp = temp + booleanToYesNo(flag) + ",";
                            
                            flag = sMessageDetailSD.get(1).contains(sMessageContext);
                            
                            if(!flag)
                                description += " Message Context is not the same.";
                            
                            issue = "MRT Message Review";
                            subIssue = "Gen Bundle file is not in sync with MRT for message : "+sMessageName;
                            
                            review += issue +","+ subIssue+ "," +description + ","+temp;
                            
                            review = review +"N/A,N/A,N/A,N/A,N/A,"+booleanToYesNo(flag) + ",";
                      }
                      else{
                            review += "-,MRT Message Review,Message " + sMessageName +" is not defined,Message does not exist in any MessageSD.xml or GenBundle.xml file,No,-,-,-,-,-,-,-,";
                            seedFileExists = false;
                      }
                            String tempExportDate = (sLastExportDate == "" ? "-" :sLastExportDate);
                            review += sVFrom+ "," +sVTo+ "," +sIsObsolete+ "," +sDevManager+ "," +sProdManager+ "," +sInfoDevManager+ "," +sCreationDate+ "," +sLastUpdateDate+ "," + tempExportDate;
                      
                      if(!review.contains("Yes,Yes,Yes,Yes,Yes,Yes,Yes,Yes") && !review.contains("Yes,Yes,N/A,N/A,N/A,N/A,N/A,Yes") && !review.contains("Yes,Yes,Yes,Yes,Yes,Yes,Yes,No") && !review.contains("Yes,Yes,Yes,Yes,Yes,Yes,Yes,No")){ /*Donot add row to the report if message is present in SD file and MRT and all texts are same */
                        if(!(seedFileExists == false && sIsObsolete.equals("Y")) && !m_msg_mrt_review_exemptionlist.contains(sMessageName)){
                            pw.println(review);
                        }
                               
                      }
                    mrtApprovedMessageSet.add(sMessageName);
                  }
                  
                  if(sPreviouslyApproved.equals("Y")){
                    mrtPreviouslyApprovedMessageSet.add(sMessageName);
                  }
                  ArrayList<String> m_mrtMessageDetail = new ArrayList<String>();
                  addMessageDetail(m_mrtMessageDetail,sMessageStatus,sIsObsolete,sVFrom,sVTo,sDevManager,sProdManager,sInfoDevManager,sCreationDate,sLastUpdateDate,sLastExportDate,sPreviouslyApproved);
                  m_mrtMessageToMessageDetailMap.put(sMessageName,m_mrtMessageDetail);
              }
        }
            pw.flush();
            pw.close();
            
            br.close();
            

            pw = new PrintWriter("message_not_in_mrt.csv");
            pw.println("Series, Label, Family, Product, Message, File Name, Issue, Description, MessageStatus, Obsolete, V.From, V.To, Dev Manager, Product Manager, InfoDev Manager");
            
            Iterator<String> iter = m_messageSet.iterator();
            while(iter.hasNext())
            {
                String sMessageName = iter.next();
                String issue = null;
                String description = null;
                if(mrtApprovedMessageSet.contains(sMessageName) == false && sMessageName.startsWith("FND_CMN") == false && !m_msg_not_in_mrt_exemptionlist.contains(sMessageName))
                {
                  if(mrtPreviouslyApprovedMessageSet.contains(sMessageName) == false){
                    String sSeedFile = m_messageToSeedFileMap.get(sMessageName);
                    String sFamily = FamilyModuleHelper.getFamily(sSeedFile);
                    String sModule = getProduct(sSeedFile);
                    String review = sSeries +"," +sLabel +"," +sFamily +"," +sModule +"," +sMessageName +"," + sSeedFile + ",";
                        if(m_mrtMessageToMessageDetailMap.containsKey(sMessageName)){
                          ArrayList<String> mrtMessageDetail = m_mrtMessageToMessageDetailMap.get(sMessageName);
                          String mVFrom = mrtMessageDetail.get(2);
                          String mVTo = mrtMessageDetail.get(3);
                          String mIsObsolete = mrtMessageDetail.get(1);
                          int versionFrom = versionToRelease(mVFrom);
                            if(versionFrom <= default_release || mIsObsolete.equals("Y")){
                              String mStatus = mrtMessageDetail.get(0);
                              String mDevManager = mrtMessageDetail.get(4);
                              String mProdManager = mrtMessageDetail.get(5);
                              String sInfoDevManager = mrtMessageDetail.get(6)==""?"-":mrtMessageDetail.get(6);
                              issue = "Message not in MRT";
                              description = sMessageName + " : This message is not in approved status and has never been approved previously or Message is Obsolete or not versioned for this release.";
                              review += issue + "," + description +"," + mStatus + "," +mIsObsolete + "," + mVFrom + "," + mVTo + "," + mDevManager + "," + mProdManager + "," + sInfoDevManager;
                              pw.println(review);
                            }
                        }
                        else{
                          issue = "Message not in MRT";
                          description = sMessageName + " : The message is not present in MRT in any status";
                          review += issue + "," + description +"," +"-,-,-,-,-,-,-";                    
                          pw.println(review);
                        }
                  }
                }
            }
           iter = m_stringRepMessageSet.iterator();
          while(iter.hasNext())
          {
              String sMessageName = iter.next();
              String issue = null;
              String description = null;
              if(mrtApprovedMessageSet.contains(sMessageName) == false && sMessageName.startsWith("FND_CMN") == false && !m_msg_not_in_mrt_exemptionlist.contains(sMessageName))
              {
                if(mrtPreviouslyApprovedMessageSet.contains(sMessageName) == false){
                  String sBundleFile = m_stringRepMessageMap.get(sMessageName).get(2);
                  String sFamily = FamilyModuleHelper.getFamily(sBundleFile);
                  String sModule = getProduct(sBundleFile);
                  String review = sSeries +"," +sLabel +"," +sFamily +"," +sModule +"," +sMessageName +"," + sBundleFile + ",";
                  if(m_mrtMessageToMessageDetailMap.containsKey(sMessageName)){
                    ArrayList<String> mrtMessageDetail = m_mrtMessageToMessageDetailMap.get(sMessageName);
                    String mVFrom = mrtMessageDetail.get(2);
                    int versionFrom = versionToRelease(mVFrom);
                      String mIsObsolete = mrtMessageDetail.get(1);
                      if(versionFrom <= default_release ){
                        String mStatus = mrtMessageDetail.get(0);
                        if(!mStatus.equals("APPROVED") || mIsObsolete.equals("Y")){
                          String mVTo = mrtMessageDetail.get(3);
                          String mDevManager = mrtMessageDetail.get(4);
                          String mProdManager = mrtMessageDetail.get(5);
                          String sInfoDevManager = mrtMessageDetail.get(6)==""?"-":mrtMessageDetail.get(6);
                          issue = "Message not in MRT";
                          description = sMessageName + " : This message is not in approved status and has never been approved previously or Message is Obsolete or not versioned for this release.";
                          review += issue + "," + description +"," + mStatus + "," +mIsObsolete + "," + mVFrom + "," + mVTo + "," + mDevManager + "," + mProdManager + "," + sInfoDevManager;
                          pw.println(review);
                        }
                        
                      }
                  }
                  else{
                    issue = "Message not in MRT";
                    description = sMessageName + " : The message is not present in MRT in any status";
                    review += issue + "," + description +"," +"-,-,-,-,-,-,-";
                    pw.println(review);
                  }
              }
          }
          }
            pw.flush();
            pw.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void addMessageDetail(ArrayList<String> m_mrtMessageDetail, String sMessageStatus,String sIsObsolete,String sVFrom,String sVTo,String sDevManager,String sProdManager,String sInfoDevManager,String sCreationDate,String sLastUpdateDate,String sLastExportDate, String sPreviouslyApproved ){
      m_mrtMessageDetail.add(sMessageStatus);
      m_mrtMessageDetail.add(sIsObsolete);
      m_mrtMessageDetail.add(sVFrom);
      m_mrtMessageDetail.add(sVTo);
      m_mrtMessageDetail.add(sDevManager);
      m_mrtMessageDetail.add(sProdManager);
      m_mrtMessageDetail.add(sInfoDevManager);
      m_mrtMessageDetail.add(sCreationDate);
      m_mrtMessageDetail.add(sLastUpdateDate);
      m_mrtMessageDetail.add(sLastExportDate);
      m_mrtMessageDetail.add(sPreviouslyApproved == "" ? "-" : sPreviouslyApproved);
    }
    
    private static String booleanToYesNo(boolean flag){
        if(flag)
          return "Yes";
          
        else
          return "No";
    }
    
   public static String getCrawlDir(String series, String label){
       String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
       String crawlDir = "";
       
       if(series.contains("FSM"))
             crawlDir =serverTop + "/"+series+".rdd/"+label+"/fsm/";
       else
             crawlDir =serverTop + "/"+series+".rdd/"+label+"/fusionapps/";
       return crawlDir;
   } 
   
    public static void main(String[] args)
    {
      System.out.println("Beginning message scans...");
        try
        {
          mode = System.getProperty("mode");
          if(mode == null)
             mode="view";
          System.out.println("Operation mode is: " + mode);
                  
          if(mode.equals("view")){ //should receive two arguments: pwd and series
              if (args.length < 2 || args[0] == null || args[0].trim().equals("") || args[1] == null || args[1].trim().equals("")) {
                  System.out.println("USAGE: reviewMessageScanViolations.sh <SERIES>");
                  System.exit(1);
                  }
              crawlPath = args[0].trim();
              sSeries = args[1];
              sLabel = LRGUtil.getLatestLabel(sSeries);
            }
          else if(mode.equals("LRG")){
              if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
                  System.out.println("USAGE: runMessageScan.sh <series>");
                  System.exit(1);
                }
               sSeries = args[0];
               sLabel = LRGUtil.getLatestLabel(sSeries);
               crawlPath = getCrawlDir(sSeries,sLabel);
            }
          
        //  sSeries = "FUSIONAPPS_11.1.1.5.1_LINUX.X64";
        String release = FamilyModuleHelper.getRelease(sSeries);
        try {
          default_release = Integer.parseInt(release);
        } catch (NumberFormatException e) {
          default_release = 13;
        }
          System.out.println("Series: "+sSeries+" Label: "+sLabel+" Release: "+release);
          System.out.println("Crawl Path "+crawlPath);
        //  ParrallelCrawler pc2 = new ParrallelCrawler(ParrallelCrawler.CRAWL_TYPE.MESSAGE_SCANNER);
        //  pc2.processPkbFile("/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/fin/ce/db/plsql/ce_party_merge_pkg.pkb");
           long start = System.currentTimeMillis();

            m_phase = "INIT";

            initThreadPool();

            doScanningPhase();
            
            doParseBundleFilesPhase();

            doParseMessageFilesPhase();

          //  writeAnalysisPhase1();

            doProcessPKBFilesPhase();

            doProcessJavaFilesPhase();

            doProcessLpcFilesPhase();

            doProcessXmlFilesPhase();
          
            doProcessLargeFilesPhase();
          
            doProcessCSharpFilesPhase(); 

            doProcessExemptionFilesPhase();

            reviewMRTData();

            logUnreferencedMessages();
          
          //  logReferencedMessages();
          
            logUseages();
          
          //  logLargeFiles();
            
            m_phase = "DONE";
            
            cleanupThreadPool();

            long end = System.currentTimeMillis();
            double elasped = (end - start) / 60000.00;
            System.out.printf("Total Time is %.2f minutes.\n", elasped);  

        } catch (Exception e)
        {
            e.printStackTrace();
        }

    }


    private static void logUseages() throws Exception
    {
      /*  PrintWriter pw = new PrintWriter("message_usages.csv");

       pw.println("Family, Module, Message,Usage Type,Usage Location,Diagnostic Message,File Type");

        Iterator<MessageUsage> iter = m_messageUsagesList.iterator();
        while (iter.hasNext())
        {
            MessageUsage mu = iter.next();
            pw.println(mu.toString());
        }


        pw.flush();
        pw.close();*/
  
        PrintWriter pw =  new PrintWriter("undefined_message_usages.csv");

        pw.println("Series, Label, Family, Product, Message, File Name, Issue, Description, Message present in Bundle files, Message present in MRT, Message Status, Obsolete, VFrom, VTo, DevManager, ProdManager,InfoDevManager, CreationDate,LastUpdateDate,LastExportDate,PreviouslyApproved");
      
        Iterator<MessageUsage> iter = m_undefinedMessageSet.iterator();
        HashSet<String> uniqueUndefinedMessageSet = new HashSet<String>();
        String mrtDetail = "";
        String isObsolete = "";
        while (iter.hasNext())
        {
            MessageUsage mu = iter.next();
            if(!uniqueUndefinedMessageSet.contains(mu.toString())){
                String description = "Message "+ mu.m_messageName+ " referenced in code files but not defined in seed file";
                String issue = "Undefined Message Usage";
                String muRow = sSeries+","+sLabel+","+mu.toString();
                boolean flag1 = m_mrtMessageToMessageDetailMap.containsKey(mu.m_messageName);
                boolean flag2 = m_stringRepMessageMap.containsKey(mu.m_messageName);
                boolean flag = flag1 || flag2;
                muRow += "," + issue + "," + description + "," + flag2 + "," + flag1 + ",";
                if(!m_undefined_exemptionlist.contains(mu.m_messageName)){
                    if(flag){
                        if(flag1){
                          isObsolete = m_mrtMessageToMessageDetailMap.get(mu.m_messageName).get(1);
                          mrtDetail = m_mrtMessageToMessageDetailMap.get(mu.m_messageName).toString();
                          muRow += mrtDetail.substring(1, mrtDetail.length()-1);
                        }
                    }
                    else{
                      muRow += "Message not present in MRT or String Repository in any status,-,-,-,-,-,-,-,-,-,-";
                    }
                    pw.println(muRow);
                    uniqueUndefinedMessageSet.add(mu.toString());
                }
          }
           
        }
        pw.flush();
        pw.close();
    }

    private static void logUnreferencedMessages() throws Exception
    {
        String sSourceSeedFile;
        
        String issue = "Unused Message";
        
        PrintWriter pw = new PrintWriter("unused_messages.csv");

        pw.println("Series, Label, Family, Product, Message Name, File Name, Issue, Description, Message present in MRT, Message Status, Obsolete, VFrom, VTo, DevManager, ProdManager,InfoDevManager, CreationDate,LastUpdateDate,LastExportDate,PreviouslyApproved");
        Iterator<String> iter = m_unreferencedMessageSet.iterator();
        while (iter.hasNext())
        {
            String s = iter.next();
            if(!m_unused_exemptionlist.contains(s)){
                boolean isInMrt = m_mrtMessageToMessageDetailMap.containsKey(s);
                sSourceSeedFile = m_messageToSeedFileMap.get(s);
                String sFamily = FamilyModuleHelper.getFamily(sSourceSeedFile);
                String sModule = getProduct(sSourceSeedFile);
                if(isInMrt){
                    ArrayList m_messagedetail = m_mrtMessageToMessageDetailMap.get(s);
                    String msgStatus = m_messagedetail.get(0).toString();
                    String isObsolete = m_messagedetail.get(1).toString();
                    String sVFrom = m_messagedetail.get(2).toString();
                    String sVTo = m_messagedetail.get(3).toString();
                    String sDevMgr = m_messagedetail.get(4).toString();    
                    String sProdMgr = m_messagedetail.get(5).toString();
                    String sInfoDevMgr = m_messagedetail.get(6).toString();
                    String sCreationDate = m_messagedetail.get(7).toString();
                    String sLastUpdateDate = m_messagedetail.get(8).toString() == "" ? "-" : m_messagedetail.get(8).toString();
                    String sLastExportDate = m_messagedetail.get(9).toString();
                    String sPreviouslyApproved = m_messagedetail.get(10).toString();
                    String description = "Message "+s+" present in seed file and in MRT but not referenced in code files";
                    pw.println(sSeries+"," + sLabel+"," + sFamily +"," +sModule +"," +s +"," +sSourceSeedFile+","+issue+","+description+","+isInMrt+","+msgStatus+","+isObsolete+","+sVFrom+","+sVTo+","+sDevMgr+","+sProdMgr+","+sInfoDevMgr+","+sCreationDate+","+sLastUpdateDate+","+sLastExportDate+","+sPreviouslyApproved);
               }
                else{
                  String description = "Message "+s+" present in seed file but not referenced in code files and not present in MRT";
                  pw.println(sSeries+"," + sLabel+"," + sFamily +"," +sModule +"," +s +"," +sSourceSeedFile+","+issue+","+description+","+isInMrt+","+"-"+","+"-"+","+"-"+","+"-"+","+"-"+","+"-"+","+"-"+","+"-"+","+"-"+","+"-"+","+"-");
                }
            }
        }
        pw.flush();
        pw.close();
    }

    private static void logReferencedMessages() throws Exception
    {
        PrintWriter pw =
            new PrintWriter("used_messages.csv");

        Iterator<String> iter = m_messageSet.iterator();
        while (iter.hasNext())
        {
            String s = iter.next();

            if (m_unreferencedMessageSet.contains(s))
                continue;
          
           String file = m_messageToSeedFileMap.get(s);

            pw.println(s +","+file);
        }


        pw.flush();
        pw.close();
    }


    private static void doProcessLpcFilesPhase() throws Exception
    {
        long start = System.currentTimeMillis();

        m_phase = "PROCESS_LPC_FILES";

        Iterator<String> iter = m_lpcList.iterator();

        while (iter.hasNext())
        {
            String s = iter.next();

            synchronized (m_workQueue)
            {
                m_workQueue.add(s);
                m_workQueue.notify();
            }
        }

        while (isWorkDone() == false)
        {
            Thread.sleep(10000);

            System.out.println("*********************************************************************************");
            System.out.println("Size of Queue is:  " + m_workQueue.size());
        }

        long end = System.currentTimeMillis();
        double elasped = (end - start) / 60000.00;
        System.out.printf("Processing LPC files phase took %.2f minutes.\n",
                          elasped);
    }


  private static void doProcessCSharpFilesPhase() throws Exception
  {
      long start = System.currentTimeMillis();

      m_phase = "PROCESS_CSHARP_FILES";

      Iterator<String> iter = m_csharpList.iterator();

      while (iter.hasNext())
      {
          String s = iter.next();

          synchronized (m_workQueue)
          {
              m_workQueue.add(s);
              m_workQueue.notify();
          }
      }

      while (isWorkDone() == false)
      {
          Thread.sleep(10000);

          System.out.println("*********************************************************************************");
          System.out.println("Size of Queue is:  " + m_workQueue.size());
      }

      long end = System.currentTimeMillis();
      double elasped = (end - start) / 60000.00;
      System.out.printf("Processing CSharp files phase took %.2f minutes.\n",
                        elasped);
  }

    private static void doProcessXmlFilesPhase() throws Exception
    {
        long start = System.currentTimeMillis();

        m_phase = "PROCESS_XML_FILES";

        Iterator<String> iter = m_xmlList.iterator();

        while (iter.hasNext())
        {
            String s = iter.next();

            synchronized (m_workQueue)
            {
                m_workQueue.add(s);
                m_workQueue.notify();
            }
        }

        while (isWorkDone() == false)
        {
            Thread.sleep(10000);

            System.out.println("*********************************************************************************");
            System.out.println("Size of Queue is:  " + m_workQueue.size());
        }

        long end = System.currentTimeMillis();
        double elasped = (end - start) / 60000.00;
        System.out.printf("Processing XML files phase took %.2f minutes.\n",
                          elasped);
    }


    private static void doProcessJavaFilesPhase() throws Exception
    {
        long start = System.currentTimeMillis();

        m_phase = "PROCESS_JAVA_FILES";

        Iterator<String> iter = m_javaList.iterator();

        while (iter.hasNext())
        {
            String s = iter.next();

            synchronized (m_workQueue)
            {
                m_workQueue.add(s);
                m_workQueue.notify();
            }
        }

        while (isWorkDone() == false)
        {
            Thread.sleep(10000);

            System.out.println("*********************************************************************************");
            System.out.println("Size of Queue is:  " + m_workQueue.size());
        }

        long end = System.currentTimeMillis();
        double elasped = (end - start) / 60000.00;
        System.out.printf("Processing Java files phase took %.2f minutes.\n",
                          elasped);
    }

  private static void doProcessLargeFilesPhase() throws Exception
  {
      long start = System.currentTimeMillis();

      m_phase = "PROCESS_LARGE_FILES";
      
      String s = FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
      "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/exemptions/large_file_mu_fp.csv";
    synchronized (m_workQueue)
    {
        m_workQueue.add(s);
        m_workQueue.notify();
    }
    while (isWorkDone() == false)
    {
        Thread.sleep(10000);

        System.out.println("*********************************************************************************");
        System.out.println("Size of Queue is:  " + m_workQueue.size());
    }

    long end = System.currentTimeMillis();
    double elasped = (end - start) / 60000.00;
    System.out.printf("Processing Large files phase took %.2f minutes.\n",
                      elasped);

  }
  
  private static void doProcessExemptionFilesPhase() throws Exception
  {
      long start = System.currentTimeMillis();

      m_phase = "PROCESS_EXEMPTION_FILES";
      
      String relative_path = FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
    "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/exemptions/";
      
      String[] s = {relative_path+"undefined_messages_exemption.txt",
                    relative_path+"unused_messages_exemption.txt",
                    relative_path+"msg_mrt_review_exemption.txt",
                    relative_path+"msg_not_in_mrt_exemption.txt" };
    synchronized (m_workQueue)
    {
      for(int i=0; i<s.length; i++){
        m_workQueue.add(s[i]);
        m_workQueue.notify();
      }
    }
    while (isWorkDone() == false)
    {
        Thread.sleep(10000);

        System.out.println("*********************************************************************************");
        System.out.println("Size of Queue is:  " + m_workQueue.size());
    }

    long end = System.currentTimeMillis();
    double elasped = (end - start) / 60000.00;
    System.out.printf("Processing Exemption files phase took %.2f minutes.\n",
                      elasped);

  }

    private static void doProcessPKBFilesPhase() throws Exception
    {
        long start = System.currentTimeMillis();

        m_phase = "PROCESS_PKB_FILES";

        m_unreferencedMessageSet = (HashSet<String>)m_messageSet.clone();

        Iterator<String> iter = m_pkbList.iterator();

        while (iter.hasNext())
        {
            String s = iter.next();

            synchronized (m_workQueue)
            {
                m_workQueue.add(s);
                m_workQueue.notify();
            }
        }

        while (isWorkDone() == false)
        {
            Thread.sleep(10000);

            System.out.println("*********************************************************************************");
            System.out.println("Size of Queue is:  " + m_workQueue.size());
        }

        long end = System.currentTimeMillis();
        double elasped = (end - start) / 60000.00;
        System.out.printf("Processing PKB files phase took %.2f minutes.\n",
                          elasped);
    }

  private static void doParseMessageFilesPhase() throws Exception
  {
      long start = System.currentTimeMillis();

      m_phase = "PROCESS_MESSAGE_FILES";
      Iterator<String> iter = m_messageFileList.iterator();
      while (iter.hasNext())
      {
          String s = iter.next();

          synchronized (m_workQueue)
          {
              m_workQueue.add(s);
              m_workQueue.notify();
          }
      }

      while (isWorkDone() == false)
      {
          Thread.sleep(10000);

          System.out.println("*********************************************************************************");
          System.out.println("Size of Queue is:  " + m_workQueue.size());
      }

      long end = System.currentTimeMillis();
      double elasped = (end - start) / 60000.00;
      System.out.printf("Parsing messages phase took %.2f minutes.\n",
                        elasped);

  }

    private static void doParseBundleFilesPhase() throws Exception
    {
        long start = System.currentTimeMillis();

        m_phase = "PROCESS_XLF_FILES";
        Iterator<String> iter = m_stringBundleFileList.iterator();
        while (iter.hasNext())
        {
            String s = iter.next();

            synchronized (m_workQueue)
            {
                m_workQueue.add(s);
                m_workQueue.notify();
            }
        }

        while (isWorkDone() == false)
        {
            Thread.sleep(10000);

            System.out.println("*********************************************************************************");
            System.out.println("Size of Queue is:  " + m_workQueue.size());
        }

        long end = System.currentTimeMillis();
        double elasped = (end - start) / 60000.00;
        System.out.printf("Parsing string bundle files phase took %.2f minutes.\n",
                          elasped);

    }

    private static void doScanningPhase() throws Exception
    {
        long start = System.currentTimeMillis();
        
      /*  String[] paths = {
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/hz/odi/model",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/zca/odi/model",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/zpm/odi/model",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/moo/odi/project/CRM_MOO_Import",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/zmm/odi/model",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/hcm/ben/odi/project/HcmBenPlanCopyExport",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/mkt/odi/project/CRM_MKT_Import",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/hcm/hrc/odi/model/MOD_Oracle_HcmViewObjects.xml",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/mkt/odi/model",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/moo/odi/model",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/zmm/odi/project/CRM_ZMM_BulkImport",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/mot/odi/model",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/zpm/odi/project",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/hcm/hra/odi/project/HCM_HRA_DirectSQLLoader",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/okc/odi/project/CRM_OKC_BulkImport",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/hcm/ben/odi/model",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/zca/odi/project",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/scm/msc/odi/project/SCM_MSC_BulkImport",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/hcm/hrc/odi/project/HcmBatchLoader_EHR2HR",
"/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/crm/hz/odi/project" };

    */

        m_phase = "SCANNING";

        synchronized (m_workQueue)
        {
            m_workQueue.add(crawlPath);
            m_workQueue.notify();
        }

        while (isWorkDone() == false)
        {
            Thread.sleep(10000);

            System.out.println("*********************************************************************************");
            System.out.println("Size of Queue is:  " + m_workQueue.size());
            System.out.println("Num discovered Java files = " +
                               m_javaList.size());
            System.out.println("Num discovered PKB files = " +
                               m_pkbList.size());
            System.out.println("Num discovered Message files = " +
                               m_messageFileList.size());
            System.out.println("Num discovered Bundle files = " +
                             m_stringBundleFileList.size());
            System.out.println("Num discovered Xml files = " +
                               m_xmlList.size());
            System.out.println("Num discovered Lpc files = " +
                               m_lpcList.size());
        }

        long end = System.currentTimeMillis();
        double elasped = (end - start) / 60000.00;
        System.out.printf("Scanning phase took %.2f minutes.\n", elasped);

    }

    protected void crawlDirectory(String path, boolean bDoADE)
    {
        if (path.contains(".ade_path") || path.contains("/classes/"))
            return;

        if (optionallySkipDirectory(path))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++)
        {
            if (listOfFiles[i].isFile())
            {
                String sName = listOfFiles[i].getAbsolutePath();
                // Zeesha: Added, because for files modified recently there is an .ade_path file created in that directory, that is neither node, nor dir. Skip this without raising error
                if (sName.contains(".ade_path"))
                    continue;

                if (fileOfInterest(sName))
                {
                    processFile(listOfFiles[i], bDoADE);
                }

                crawlPeek(listOfFiles[i]);
            } else if (listOfFiles[i].isDirectory())
            {
                // Added by zeesha - no point going into the directory if we are not interested in it. Better performance
                if (isDirOfInterest(listOfFiles[i].getAbsolutePath()))
                {
                    //                    crawlDirectory(listOfFiles[i].getAbsolutePath(), bDoADE);
                    synchronized (m_workQueue)
                    {
                        m_workQueue.add(listOfFiles[i].getAbsolutePath());
                        m_workQueue.notify();
                    }
                }
            } else
            {
                System.out.println("ERROR:  node is neither file or directory: " +
                                   listOfFiles[i]);
            }
        }

    }


    static private boolean isWorkDone()
    {
        if (m_workQueue.size() > 0)
            return false;

        for (int i = 0; i < m_workers.size(); i++)
        {
            Thread t = m_workers.get(i);

            Thread.State ts = t.getState();
            switch (ts)
            {
            case BLOCKED:
                return false;
            case NEW:
                return false;
            case RUNNABLE:
                return false;
            }
        }

        return true;
    }

    private static void writeAnalysisPhase1()
    {
        m_phase = "ANALYSIS1";

        try
        {
            PrintWriter messageFileListPW =
                new PrintWriter("message_files.csv");
            PrintWriter pkbFileListPW =
                new PrintWriter("pkb_files.csv");
            PrintWriter javaFileListPW =
                new PrintWriter("java_files.csv");
            PrintWriter xmlFileListPW =
                new PrintWriter("xml_files.csv");
            PrintWriter diagMessagesPW =
                new PrintWriter("diagnostic_messages.csv");
            PrintWriter lpcFilePW =
                new PrintWriter("lpc_files.csv");

            String x = "File Name, Message Name";
            diagMessagesPW.println(x);

            Iterator<String> iter = m_diagMessageSet.iterator();
            while (iter.hasNext())
            {
                String s = iter.next();
                diagMessagesPW.println(s);
            }

            diagMessagesPW.flush();
            diagMessagesPW.close();
            

            iter = m_messageFileList.iterator();
            while (iter.hasNext())
            {
                String s = iter.next();
                messageFileListPW.println(s);
            }

            iter = m_pkbList.iterator();
            while (iter.hasNext())
            {
                String s = iter.next();
                pkbFileListPW.println(s);
            }

            iter = m_javaList.iterator();
            while (iter.hasNext())
            {
                String s = iter.next();
                javaFileListPW.println(s);
            }

            iter = m_xmlList.iterator();
            while (iter.hasNext())
            {
                String s = iter.next();
                xmlFileListPW.println(s);
            }

            iter = m_lpcList.iterator();
            while (iter.hasNext())
            {
                String s = iter.next();
                lpcFilePW.println(s);
            }

            messageFileListPW.flush();
            messageFileListPW.close();

            pkbFileListPW.flush();
            pkbFileListPW.close();

            javaFileListPW.flush();
            javaFileListPW.close();

            xmlFileListPW.flush();
            xmlFileListPW.close();

            lpcFilePW.flush();
            lpcFilePW.close();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static final String m_messageNameRegex = "[\\w%.-]+";

    static final Pattern m_messagePatternForPkb =  Pattern.compile("(?:'|:)("     +m_messageNameRegex +")'");
    static final Pattern m_messagePatternForJava = Pattern.compile("(?:[\":'])("  +m_messageNameRegex +")[\"']");
    static final Pattern m_messagePatternForLpc =  Pattern.compile("(?:[\":'])("  +m_messageNameRegex +")[\"']");
   static final Pattern m_messagePatternForCSharp =  Pattern.compile("(?:[\":'])("  +m_messageNameRegex +")[\"']");
    static final Pattern m_messagePatternForXML =  Pattern.compile("(?:[\"':>])(" +m_messageNameRegex +")(?:['\"<])");

    static final Pattern m_messagePatternForJavaEnum = Pattern.compile("enum" +whiteSpace + identifier + whiteSpace +"\\{([^}]*)\\}", Pattern.DOTALL);
    static final Pattern m_messagePatternForJavaEnum2 = Pattern.compile(m_messageNameRegex);
    
//    public enum APP_MSGS {
//        HWM_TL_CONCURRENT_UPDATE,
//        HWM_TL_TER_ERROR,
//        HWM_TL_LOCK_NOT_ACQUIRED,
//        HWM_TL_TCR_ERROR,
//        HWM_TL_VALIDATION_FAIL;        
//    }



    protected ArrayList<String> getPossibleMessageReferences(String l, Pattern p)
    {
        ArrayList<String> retVal = null;

        l = l.trim();

        retVal = new ArrayList<String>();

        Matcher m = p.matcher(l);

        while (m.find())
        {
            String s = m.group(1);
            retVal.add(s);
        }

        return retVal;
    }

    protected ArrayList<String> getPossibleJavaEnumMessageReferences(String l)
    {
        ArrayList<String> retVal = null;

        l = l.trim();

        retVal = new ArrayList<String>();

        Matcher m = m_messagePatternForJavaEnum.matcher(l);

        while (m.find())
        {
            String s = m.group(1);

            Matcher m2 = m_messagePatternForJavaEnum2.matcher(s);
            while(m2.find())
            {
                String sMessage = m2.group(0);
                retVal.add(sMessage);
            }
        }

        return retVal;
    }


  static final String m_xmlMsgReference = identifier +":::(" +m_messageNameRegex +")";
  static final Pattern m_xmlMsgReferencePattern = Pattern.compile(m_xmlMsgReference, Pattern.CASE_INSENSITIVE); 
    
  static final String m_odiXmlMsgReference = "rowid," + whiteSpace  + "'S',"  +  whiteSpace + "'("  +m_messageNameRegex +")'";
  static final Pattern m_odiXmlMsgReferencePattern = Pattern.compile(m_odiXmlMsgReference, Pattern.CASE_INSENSITIVE); 
    
    
    public String identifyMessageUsagesInXML(String content, String sLocation)
    {
        String retval = content;

        Matcher m = m_xmlMsgReferencePattern.matcher(content);

        while (m.find())
        {
            String s0 = m.group(0);
            String s1 = m.group(1);
            
            MessageUsage mu = new MessageUsage(s1, "XML Message", sLocation);
            synchronized (m_messageUsagesList)
            {
                m_messageUsagesList.add(mu);
            }

            if (m_stringRepMessageSet.contains(s1) == false && m_messageSet.contains(s1) == false && s1.startsWith("FND_") == false)
            {
                synchronized (m_undefinedMessageSet)
                {
                    m_undefinedMessageSet.add(mu);
                }
            }

            noteMessageUse(s1);

            retval = retval.replace(s0, "");
        }


    /*  Matcher m = m_odiXmlMsgReferencePattern.matcher(content);

      while (m.find())
      {
          String s0 = m.group(0);
          String s1 = m.group(1);
          
          MessageUsage mu = new MessageUsage(s1, "XML Message", sLocation);
          synchronized (m_messageUsagesList)
          {
              m_messageUsagesList.add(mu);
          }

          if (m_stringRepMessageSet.contains(s1) == false && m_messageSet.contains(s1) == false && s1.startsWith("FND_") == false)
          {
              synchronized (m_undefinedMessageSet)
              {
                  m_undefinedMessageSet.add(mu);
              }
          }

          noteMessageUse(s1);
          System.out.println("Odi "+s1);
          retval = retval.replace(s0, "");
      } */
        return retval;
    }

    private static void logLargeFiles() throws Exception{
      PrintWriter pw = new PrintWriter("large_files.csv");
      for(String f : large_files){
        pw.println(f);
      }
      pw.close();
    }


    protected void processXmlFile(String s) throws Exception
    {
        String content = null;

        File f = new File(s);
        if (f.length() > 10000000)
        {
           // System.out.println("Skipping Large file.   (" + f.length() +")  " + s);
            synchronized (large_files){
              large_files.add(f.getAbsolutePath());
            }
           
            return;
        }

        if (f.length() <= 0)
        {
            System.out.println("Skipping EMPTY file.   (" + f.length() +")  " + s);
            return;
        }

        content = readFileToStringAlternate(s);

        if(content == null)
            return;

        content = removeHTMLComments(content);
        
        content = identifyMessageUsagesInXML(content, s);

        ArrayList<String> line_tokens =
            getPossibleMessageReferences(content, m_messagePatternForXML);

        for (int i = 0; i < line_tokens.size(); i++)
        {
            String ref = line_tokens.get(i);


            if (m_messageSet.contains(ref))
            {
                dumpUsageContext(content, ref, "XML@");
                addMessageUsage(ref, "TBD", s);
                noteMessageUse(ref);
            }

        }
    }

  protected void processCSharpFile(String s) throws Exception
  {
      String content = readFileToString(s);

      if(content == null)
          return;

      content = removeJavaComments(content);

      content = identifyMessageUsagesInCSharp(content, s);
  }
  
  static final String m_CSharpMsgReference2 = "MessageCodes\\.Code\\." + "("  +m_messageNameRegex +")";
  static final Pattern m_CSharpMsgReferencePattern2 = Pattern.compile(m_CSharpMsgReference2, Pattern.CASE_INSENSITIVE); 
  
  static final String m_CSharpMsgReference1 = "MessageCodes\\.Code\\." + "("  +m_messageNameRegex +")" + ".ToString";
  static final Pattern m_CSharpMsgReferencePattern1 = Pattern.compile(m_CSharpMsgReference1, Pattern.CASE_INSENSITIVE); 
  
  public String identifyMessageUsagesInCSharp(String content, String sLocation)
  {
      String retval = content;
      Matcher m = m_CSharpMsgReferencePattern1.matcher(content);

      while (m.find())
      {
          String s0 = m.group(0);
          String s1 = m.group(1);
          MessageUsage mu = new MessageUsage(s1, "CSharp Message", sLocation);
          synchronized (m_messageUsagesList)
          {
              m_messageUsagesList.add(mu);
          }

          if (m_stringRepMessageSet.contains(s1) == false && m_messageSet.contains(s1) == false && s1.startsWith("FND_") == false)
          {
              synchronized (m_undefinedMessageSet)
              {
                  m_undefinedMessageSet.add(mu);
              }
          }

          noteMessageUse(s1);

          retval = retval.replace(s0, "");
      }
      
     content = retval;
     m = m_CSharpMsgReferencePattern2.matcher(content);

    while (m.find())
    {
        String s0 = m.group(0);
        String s1 = m.group(1);
        MessageUsage mu = new MessageUsage(s1, "CSharp Message", sLocation);
        synchronized (m_messageUsagesList)
        {
            m_messageUsagesList.add(mu);
        }

        if (m_stringRepMessageSet.contains(s1) == false && m_messageSet.contains(s1) == false && s1.startsWith("FND_") == false)
        {
            synchronized (m_undefinedMessageSet)
            {
                m_undefinedMessageSet.add(mu);
            }
        }

        noteMessageUse(s1);

        retval = retval.replace(s0, "");
    }
  
  return retval;
  }
  
    protected void processJavaFile(String s) throws Exception
    {
        String content = readFileToString(s);
        
        if(content == null)
            return;


        content = removeJavaComments(content);
        
        content = identifyMessageUsagesInJava(content, s);

        ArrayList<String> line_tokens = getPossibleMessageReferences(content, m_messagePatternForJava);


        for (int i = 0; i < line_tokens.size(); i++)
        {
            String ref = line_tokens.get(i);

            if (m_messageSet.contains(ref))
            {
                dumpUsageContext(content, ref, "JAVA@");
                addMessageUsage(ref, "TBD", s);
                noteMessageUse(ref);
            }
        }
        
        line_tokens = getPossibleJavaEnumMessageReferences(content);
        
        for (int i = 0; i < line_tokens.size(); i++)
        {
            String ref = line_tokens.get(i);

            if (m_messageSet.contains(ref))
            {
                dumpUsageContext(content, ref, "JAVA@");
                addMessageUsage(ref, "JAVA ENUM", s);
                noteMessageUse(ref);
            }

        }
    }


    protected void processLpcFile(String s) throws Exception
    {
        String content = readFileToString(s);

        if(content == null)
            return;

        content = removeLpcComments(content);

        ArrayList<String> line_tokens =
            getPossibleMessageReferences(content, m_messagePatternForLpc);


        for (int i = 0; i < line_tokens.size(); i++)
        {
            String ref = line_tokens.get(i);

            if (m_messageSet.contains(ref))
            {
                dumpUsageContext(content, ref, "CFILE@");
                addMessageUsage(ref, "TBD", s);
                noteMessageUse(ref);
            }


        }
    }

    // PL/SQL example
    //FND_MESSAGE.SET_NAME('SQLAP', 'AP_DEBUG');
    
    private class Varible
    {
        public String m_name;
        public String m_value;
        
        public Varible(String name, String value)
        {
            m_name = name;
            m_value = value;
        }
    }

    static final String m_variableDecl = whiteSpace +"(" +identifier +")" +whiteSpace +":=" + whiteSpace +"'" +"(" +m_messageNameRegex +")" +"';";
    static final Pattern m_variableDeclPattern = Pattern.compile(m_variableDecl, Pattern.CASE_INSENSITIVE);

    static final String m_paramRegex = "\\s*([^,)(]*)\\s*,\\s*('?)([^')(]*)('?)\\s*";
    static final Pattern m_messageReferenceForPkb1 = Pattern.compile("(?:FND_MESSAGE|HR_UTILITY|pay_proc_logging).(?:(?:SET_NAME)|(?:SET_MESSAGE)|(?:GET_STRING)|(?:GET_NUMBER)|(?:SET_MESSAGE))\\s*\\(" 
                                                                     +m_paramRegex + "\\)", Pattern.CASE_INSENSITIVE);
   /* //ZMM-ZMM_BULK_IMPORT_TASKS_7
  static final String m_insertValue = identifier +"-(" +m_messageNameRegex +")";
  static final Pattern m_insertValuePattern = Pattern.compile(m_insertValue, Pattern.CASE_INSENSITIVE); */
    
    ////Get_Message('GL_BUDGET_PERIOD_INVALID');

    static final String m_plsqlGetMessage = "gl_message."+"get_message\\('(" +m_messageNameRegex +")'";
    static final Pattern m_plsqlGetMessagePattern = Pattern.compile(m_plsqlGetMessage, Pattern.CASE_INSENSITIVE);
        
        

    //p_message_name     => 'ZX_GENERIC_MESSAGE'
    //msg_name  =>'GL_SHRD0180'
    //NAME            => 'JA_CN_SEC_LEDGER_NOT_ELIGIBLE'
    static final String m_parameterDecl = "(?:p_error_message_name|p_messagename|p_error_message|p_message|p_message_name|msg_name|p_msg_name)" +whiteSpace +"=>" +whiteSpace +"'(" +m_messageNameRegex +")'";
    static final Pattern m_parameterDeclPattern = Pattern.compile(m_parameterDecl, Pattern.CASE_INSENSITIVE);

    //l_msg_name := 'FA_WHATIF_ASSET_DIST_INFO'
    //l_msg := 'IBY_INVALID_PMTINSTRASSINGMENT'
    //l_message_name := 'PO_PDOI_NO_DOC_FOUND';
    
    static final String m_plsqlMessageVariable = "(?:l_msg_name|l_msg|l_message_name)" +whiteSpace +":=" +whiteSpace +"'(" +m_messageNameRegex +")'";
    static final Pattern m_plsqlMessageVariablePattern = Pattern.compile(m_plsqlMessageVariable, Pattern.CASE_INSENSITIVE);


    public String identifyMessageUsagesInPLSQL(String content,
                                               String sLocation)
    {
        HashMap<String, Varible> varibles = new HashMap<String, Varible>();
        Matcher m = m_variableDeclPattern.matcher(content);

        while (m.find())
        {
            String s1 = m.group(1);
            String s2 = m.group(2);

            Varible v = new Varible(s1, s2);
            varibles.put(s1, v);
        }




        String retval = content;

        m = m_messageReferenceForPkb1.matcher(content);

        while (m.find())
        {
            String s0 = m.group(0);
            String s1 = m.group(1);
            String s2 = m.group(2);
            String s3 = m.group(3);
            String s4 = m.group(4);
            
            boolean flag = true;
            if("'".equals(s2) == false)
            {
                // need to make use of the message name referenced by varibles, if possible.
                Varible v = varibles.get(s3);
                if(v != null)
                {
                    String msg = v.m_value;
                    s3 = msg;
                }
                else{
                  flag = false; //if we can't find message referenced by variables, skip note message usage
                }
            }

          if(flag){
            MessageUsage mu = new MessageUsage(s3, "FND_MESSAGE.SET_NAME", sLocation);
            synchronized (m_messageUsagesList)
            {
                m_messageUsagesList.add(mu);
            }

            if (m_stringRepMessageSet.contains(s1) == false && m_messageSet.contains(s3) == false && s3.startsWith("FND_") == false)
            {
                synchronized (m_undefinedMessageSet)
                {
                    m_undefinedMessageSet.add(mu);
                }
            }
            
            noteMessageUse(s3);

            retval = retval.replace(m.group(0), "");
          }
            
        }


        content = retval;

        m = m_parameterDeclPattern.matcher(content);

        while (m.find())
        {
            String s0 = m.group(0);
            String s1 = m.group(1);
            
            MessageUsage mu = new MessageUsage(s1, "Message is parameter", sLocation);
            synchronized (m_messageUsagesList)
            {
                m_messageUsagesList.add(mu);
            }

            if (m_stringRepMessageSet.contains(s1) == false && m_messageSet.contains(s1) == false && s1.startsWith("FND_") == false)
            {
                synchronized (m_undefinedMessageSet)
                {
                    m_undefinedMessageSet.add(mu);
                }
            }

            noteMessageUse(s1);

            retval = retval.replace(s0, "");
        }
        
     /*     content = retval;
        
      m = m_insertValuePattern.matcher(content);
        while (m.find())
               {
                   String s0 = m.group(0);
                   String s1 = m.group(1);
                   
                   MessageUsage mu = new MessageUsage(s1, "Message is parameter 3", sLocation);
                   synchronized (m_messageUsagesList)
                   {
                       m_messageUsagesList.add(mu);
                   }
  
                   if (m_stringRepMessageSet.contains(s1) == false && m_messageSet.contains(s1) == false && s1.startsWith("FND_") == false)
                   {
                       synchronized (m_undefinedMessageSet)
                       {
                           m_undefinedMessageSet.add(mu);
                       }
                   }
  
                   noteMessageUse(s1);
  
                   retval = retval.replace(s0, "");
               }
        
*/
//Get_Message('GL_BUDGET_PERIOD_INVALID');
        content = retval;

        m = m_plsqlGetMessagePattern.matcher(content);

        while (m.find())
        {
            String s0 = m.group(0);
            String s1 = m.group(1);
            
            MessageUsage mu = new MessageUsage(s1, "Message is parameter 2", sLocation);
            synchronized (m_messageUsagesList)
            {
                m_messageUsagesList.add(mu);
            }

            if (m_stringRepMessageSet.contains(s1) == false && m_messageSet.contains(s1) == false && s1.startsWith("FND_") == false)
            {
                synchronized (m_undefinedMessageSet)
                {
                    m_undefinedMessageSet.add(mu);
                }
            }

            noteMessageUse(s1);

            retval = retval.replace(s0, "");
        }

        content = retval;
        
       // content = identifyMessageUsagesHelper(content, sLocation, m_plsqlMessageVariablePattern, 1, "PLSQL Message Variable Reference");

        return content;
    }


    public String identifyMessageUsagesHelper(String content, String sLocation, Pattern p, int messagePosition, String sDescription)
    {
        String retval = content;
        
        Matcher m = p.matcher(content);

        while (m.find())
        {
            String s0 = m.group(0);
            String sMessage = m.group(messagePosition);
            
            MessageUsage mu = new MessageUsage(sMessage, sDescription, sLocation);
            synchronized (m_messageUsagesList)
            {
                m_messageUsagesList.add(mu);
            }

            if (m_stringRepMessageSet.contains(sMessage) == false && m_messageSet.contains(sMessage) == false && sMessage.startsWith("FND_") == false)
            {
                synchronized (m_undefinedMessageSet)
                {
                    m_undefinedMessageSet.add(mu);
                }
            }

            noteMessageUse(sMessage);

            retval = retval.replace(s0, "");
        }
        
        return retval;
    }
    
  public void processLargeFile(String file)
  {
          FileReader fr;
    try {
      fr = new FileReader(file);
      BufferedReader br = new BufferedReader(fr); 
      String s = br.readLine();
      while((s = br.readLine()) != null){
          String[] mus = s.split(",");
          String sMessage = mus[2];
          String sLocation = mus[3];
          MessageUsage mu = new MessageUsage(sMessage, "Large xml file", sLocation);
          synchronized (m_messageUsagesList)
          {
              m_messageUsagesList.add(mu);
          }

          if (m_stringRepMessageSet.contains(sMessage) == false && m_messageSet.contains(sMessage) == false && sMessage.startsWith("FND_") == false)
          {
              synchronized (m_undefinedMessageSet)
              {
                  m_undefinedMessageSet.add(mu);
              }
          }

          noteMessageUse(sMessage);    
      }
    } catch (FileNotFoundException e) {
    } catch (IOException e) {
    }
  }
  
  public void processExemptionFile(String file)
  {
          FileReader fr;
    try {
      fr = new FileReader(file);
      BufferedReader br = new BufferedReader(fr); 
      String s = br.readLine();
      while((s = br.readLine()) != null){
          String[] sTokens = s.split("#");
          String sMessage = sTokens[0];
        if(file.contains("undefined_messages_exemption")){
          m_undefined_exemptionlist.add(sMessage);
        }
        else if(file.contains("unused_messages_exemption")){
          m_unused_exemptionlist.add(sMessage);
        }
        else if(file.contains("msg_mrt_review_exemption")){
          m_msg_mrt_review_exemptionlist.add(sMessage);
        }
        else if(file.contains("msg_not_in_mrt_exemption")){
          m_msg_not_in_mrt_exemptionlist.add(sMessage);
        }
      }
    } catch (FileNotFoundException e) {
    } catch (IOException e) {
    }
  }

    //throw new ApplcoreException("POZ:::POZ_MISSING_SITE_GRANT"); 

    static final String m_javaMsgReference = "\"" +identifier +":::(" +m_messageNameRegex +")\"";
    static final Pattern m_javaMsgReferencePattern = Pattern.compile(m_javaMsgReference, Pattern.CASE_INSENSITIVE);

    //Message.getUserText("ZSP", "ZSP_CMN_PROC_INTERNAL_ERROR", messageParameters);
    static final String m_javaMessageAPIReference = "Message.(?:getUserText|getShortText)" +whiteSpace +"\\(" + whiteSpace +"\"" +identifier +"\"" + whiteSpace +"," + whiteSpace +"\"(" + m_messageNameRegex +")\"" + whiteSpace + "," + whiteSpace +"[^)]+\\)";
    static final Pattern m_javaMessageAPIReferencePattern = Pattern.compile(m_javaMessageAPIReference, Pattern.CASE_INSENSITIVE);

    // A fex expenses specific cases
    // ADFUtil.getExmMsg("WorkAreaAMDataControl", "EXM_EREPT_DELETE")
    // getExmMsg("EXM_POL_MLG_CUM_ERR", "ERR", "Upload", "ERR_TEXT", e.toString()), AppsLogger.WARNING)
    static final String m_javaExmMessageAPIReference = "((?:ADFUtil.)?getExmMsg)" + whiteSpace +"\\(" +"([^,)(]+)" +whiteSpace +"," +whiteSpace +"([^,)(]+)";
    static final Pattern m_javaExmMessageAPIReferencePattern = Pattern.compile(m_javaExmMessageAPIReference, Pattern.CASE_INSENSITIVE);

    //getFNDMessageText("ZCQ", "ZCQ_CMN_ENG_CONNECT_ERR", tokens);
    //getFndMessageText("ANC","ANC_END_DT_EQ_STRT_DT","Y"); 
    //getFNDMessageText("EGI", "EGI_FILEFORMAT_ERROR",

    static final String m_javaGetMessage1Reference = "getFNDMessageText\\(\"" +identifier +"\"" +whiteSpace +"," + whiteSpace +"\"(" + m_messageNameRegex +")\"" + whiteSpace + ",";
    static final Pattern m_javaGetMessage1ReferencePattern = Pattern.compile(m_javaGetMessage1Reference, Pattern.CASE_INSENSITIVE);

    //getFndMessageInformation("DOO_CMN_SERVICE_ERROR", tokens);
    //getFNDMessage("MKT_CONT_REP_READ_ERR", null);
    //getFNDMessage("AP_INV_ACTIONS_GENERIC", null);

    static final String m_javaGetMessage2Reference = "(?:getFNDMessage|getFndMessageInformation)\\(\"(" +m_messageNameRegex +")\"" +whiteSpace +"," + whiteSpace + identifier + whiteSpace + "\\)";
    static final Pattern m_javaGetMessage2ReferencePattern = Pattern.compile(m_javaGetMessage2Reference, Pattern.CASE_INSENSITIVE);
    
    public String identifyMessageUsagesInJava(String content, String sLocation)
    {
        String retval = content;

        Matcher m = m_javaMsgReferencePattern.matcher(content);

        while (m.find())
        {
            String s0 = m.group(0);
            String s1 = m.group(1);
            
            MessageUsage mu = new MessageUsage(s1, "Java Message", sLocation);
            synchronized (m_messageUsagesList)
            {
                m_messageUsagesList.add(mu);
            }

            if (m_stringRepMessageSet.contains(s1) == false && m_messageSet.contains(s1) == false && s1.startsWith("FND_") == false)
            {
                synchronized (m_undefinedMessageSet)
                {
                    m_undefinedMessageSet.add(mu);
                }
            }

            noteMessageUse(s1);

            retval = retval.replace(s0, "");
        }


        //Message.getUserText("ZSP", "ZSP_CMN_PROC_INTERNAL_ERROR", messageParameters);

        content = retval;
        
        m = m_javaMessageAPIReferencePattern.matcher(content);

        while (m.find())
        {
            String s0 = m.group(0);
            String s1 = m.group(1);
            
            MessageUsage mu = new MessageUsage(s1, "Java Message 2", sLocation);
            synchronized (m_messageUsagesList)
            {
                m_messageUsagesList.add(mu);
            }

            if (m_stringRepMessageSet.contains(s1) == false && m_messageSet.contains(s1) == false && s1.startsWith("FND_") == false)
            {
                synchronized (m_undefinedMessageSet)
                {
                    m_undefinedMessageSet.add(mu);
                }
            }

            noteMessageUse(s1);

            retval = retval.replace(s0, "");
        }

        // A fex expenses specific cases
        // ADFUtil.getExmMsg("WorkAreaAMDataControl", "EXM_EREPT_DELETE")
        // getExmMsg("EXM_POL_MLG_CUM_ERR", "ERR", "Upload", "ERR_TEXT", e.toString()), AppsLogger.WARNING)

        content = retval;
        
        m = m_javaExmMessageAPIReferencePattern.matcher(content);

        while (m.find())
        {
            String s0 = m.group(0);
            String s1 = m.group(1);
            String s2 = m.group(2);
            String s3 = m.group(3);
            
            String sMessageName;
            
            if(s1.contains("ADFUtil"))
                sMessageName = s3;
            else
                sMessageName = s2;
            
            sMessageName = sMessageName.trim();
            sMessageName = sMessageName.replace("\"", "");
            
            MessageUsage mu = new MessageUsage(sMessageName, "Java Message EXM", sLocation);
            synchronized (m_messageUsagesList)
            {
                m_messageUsagesList.add(mu);
            }

            if (m_stringRepMessageSet.contains(s1) == false && m_messageSet.contains(sMessageName) == false && sMessageName.startsWith("FND_") == false)
            {
                synchronized (m_undefinedMessageSet)
                {
                    m_undefinedMessageSet.add(mu);
                }
            }

            noteMessageUse(sMessageName);

            retval = retval.replace(s0, "");
        }

        content = retval;

        content = identifyMessageUsagesHelper(content, sLocation, m_javaGetMessage1ReferencePattern, 1, "Java Get Message 1");

        content = identifyMessageUsagesHelper(content, sLocation, m_javaGetMessage2ReferencePattern, 1, "Java Get Message 2");

        return content;
    }


    public void processPkbFile(String s) throws Exception
    {
        String content = readFileToString(s);
        
        if(content == null)
            return;

        content = removePlSQLComments(content);
        content = identifyMessageUsagesInPLSQL(content, s);
        ArrayList<String> line_tokens =
            getPossibleMessageReferences(content, m_messagePatternForPkb);

        for (int i = 0; i < line_tokens.size(); i++)
        {
            String ref = line_tokens.get(i);

            if (m_messageSet.contains(ref))
            {
                dumpUsageContext(content, ref, "PLSQL@");
                addMessageUsage(ref, "TBD", s);
                noteMessageUse(ref);
            }


        }

    }

    private void dumpUsageContext(String content, String ref, String sPrefix)
    {
        int i = content.indexOf(ref);
        
        while(i != -1)
        {
            int start = i -150;
            if(start < 0)
                start = 0;
            int end = i+150;
            if(end > content.length())
                end = content.length();
            
            String context = content.substring(start, end);
            context = context.replaceAll("\n", "\n" +sPrefix);
            
     //       System.out.println(sPrefix +"%%%%%%%%%%%%%%%%% Usage of -->  " + ref +"     %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
      //      System.out.println(sPrefix +context);
     //       System.out.println(sPrefix +"%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");

            i = content.indexOf(ref, i+1);
        }
    }

    private void noteMessageUse(String ref)
    {
        if(m_unreferencedMessageSet == null)
            return;
        
        synchronized (m_unreferencedMessageSet)
        {
            ref = ref.toUpperCase();
            if (m_unreferencedMessageSet.contains(ref))
                m_unreferencedMessageSet.remove(ref);
        }
    }

    private void addMessageUsage(String sMessageName, String sUsageType,
                                 String sUsageLocation)
    {
        MessageUsage mu =
            new MessageUsage(sMessageName, sUsageType, sUsageLocation);
        synchronized (m_messageUsagesList)
        {
            m_messageUsagesList.add(mu);
        }
    }
    
  public void processBundleFile(String s)
  {
      try
      {
          DOMParser parser = new DOMParser();
          parser.setErrorStream(System.out);
          parser.setEntityResolver(new NullEntityResolver());
          parser.showWarnings(true);
          parser.setValidationMode(DOMParser.NONVALIDATING);
          parser.setDebugMode(true);

          parser.parse("file:" + s);

          XMLDocument doc = parser.getDocument();



          NodeList nodeList = doc.getChildrenByTagName("xliff");
          XMLElement xliff = (XMLElement)nodeList.item(0);
          nodeList = xliff.getChildrenByTagName("file");
          XMLElement file = (XMLElement)nodeList.item(0);
          nodeList = file.getChildrenByTagName("body");
          XMLElement body = (XMLElement)nodeList.item(0);
  

          NodeList nl = body.getChildrenByTagName("trans-unit");

          for (int i = 0; i < nl.getLength(); i++)
          {
              XMLElement transUnit = (XMLElement)nl.item(i);
              XMLNode currentChild = (XMLNode)nl.item(i);
              currentChild.getNodeType();

              XMLNode msg = (XMLNode)nl.item(i);

              NodeList msg_children = msg.getChildNodes();
            
              String message = transUnit.getAttribute("id");
              String source = "";
              String notes = "";
            
            for (int j = 0; j < msg_children.getLength(); j++)
            {
                XMLNode msg_part = (XMLNode)msg_children.item(j);

                String name = msg_part.getNodeName();
                String value = msg_part.getText();

                if ("source".equals(name))
                    source = value;
                else if ("note".equals(name))
                    notes = notes + value;
             }
            
        
            if(notes.contains("::TYPE:Msg")){
              recordADFMessage(message,source,notes,s);
            //  System.out.println(message);
            //  System.out.println(source);
            //  System.out.println(notes);
            }
      }
      }
    catch (Exception e)
            {
                e.printStackTrace();
            }
  }

  private void recordADFMessage(String sMessageName, String sSource, String sNotes, String sPath){
    synchronized (m_stringRepMessageSet)
    {
      ArrayList<String> m_messageDetail = new ArrayList<String>();
      m_messageDetail.add(sSource);
      m_messageDetail.add(sNotes);
      m_messageDetail.add(sPath);
      m_stringRepMessageSet.add(sMessageName);
      m_stringRepMessageMap.put(sMessageName, m_messageDetail);
    //  System.out.println( sMessageName);
    //  System.out.println(sSource);
    //  System.out.println(sNotes);
    //  System.out.println(sPath);
    }
      
  }

    public void processMessageFile(String s)
    {
        try
        {
            DOMParser parser = new DOMParser();
            parser.setErrorStream(System.out);
            parser.setEntityResolver(new NullEntityResolver());
            parser.showWarnings(true);
            parser.setValidationMode(DOMParser.NONVALIDATING);
            parser.setDebugMode(true);

            parser.parse("file:" + s);

            XMLDocument doc = parser.getDocument();

            NodeList seedDataList = doc.getChildrenByTagName("SEEDDATA");
            XMLElement seedNode = (XMLElement)seedDataList.item(0);

            NodeList nl = seedNode.getChildrenByTagName("Message");

            for (int i = 0; i < nl.getLength(); i++)
            {
                XMLNode currentChild = (XMLNode)nl.item(i);
                currentChild.getNodeType();

                XMLNode msg = (XMLNode)nl.item(i);

                NodeList msg_children = msg.getChildNodes();

                String sMessageName = "";
                String sLoggableAlertable = "";
                String sMessageType = "";
                String sMessageCategory = "";
                String sMessageSeverity = "";
                String sMessageText = "";
                String sMessageUserDetails = "";
                String sMessageCause = "";
                String sMessageAction = "";
                String sMessageAdminDetails = "";
                String sMessageAdminAction = "";
                String sMessageContext = "";
                String sCreationDate = "";
                String sLastUpdateDate = "";

                for (int j = 0; j < msg_children.getLength(); j++)
                {
                    XMLNode msg_part = (XMLNode)msg_children.item(j);

                    String name = msg_part.getNodeName();
                    String value = msg_part.getText();

                    if ("MessageName".equals(name))
                        sMessageName = value;
                    else if ("LoggableAlertable".equals(name))
                        sLoggableAlertable = value;
                    else if ("MessageType".equals(name))
                        sMessageType = value;
                    else if ("MessageCategory".equals(name))
                        sMessageCategory = value;
                    else if ("MessageSeverity".equals(name))
                        sMessageSeverity = value;
                    else if("MessageText".equals(name))
                        sMessageText = value;
                    else if("MessageUserDetails".equals(name))
                        sMessageUserDetails = value;
                    else if("MessageCause".equals(name))
                        sMessageCause = value;
                    else if("MessageUserAction".equals(name))
                        sMessageAction = value;
                    else if("MessageAdminDetails".equals(name))
                        sMessageAdminDetails = value;
                    else if("MessageAdminAction".equals(name))
                        sMessageAdminAction = value;
                    else if("MessageContext".equals(name))
                      sMessageContext = value;
                    else if("CreationDate".equals(name))
                      sCreationDate = value;
                    else if("LastUpdateDate".equals(name))
                      sLastUpdateDate = value;
                  
                }

                if ("Y".equals(sLoggableAlertable) &&
                    "ERROR".equals(sMessageType) &&
                    "HIGH".equals(sMessageSeverity) &&
                    sMessageCategory != null &&
                    !"".equals(sMessageCategory.trim()))
                {
                    recordMessage(sMessageName, true, s, sMessageText, sMessageUserDetails, sMessageCause, sMessageAction, sMessageAdminDetails, sMessageAdminAction, sMessageContext, sCreationDate, sLastUpdateDate);
                } else
                {
                    recordMessage(sMessageName, false, s, sMessageText, sMessageUserDetails, sMessageCause, sMessageAction, sMessageAdminDetails, sMessageAdminAction, sMessageContext, sCreationDate, sLastUpdateDate);
                }

                
                if (sMessageName == null || "".equals(sMessageName))
                {
                    System.out.println("This is weird...");
                }
            }

        } catch (Exception e)
        {
            e.printStackTrace();
        }

    }


    private void recordMessage(String sMessageName, boolean isDiagMessage, String sPath, String sMessageText, String sMessageUserDetails, String sMessageCause, String sMessageAction, String sMessageAdminDetails, String sMessageAdminAction, String sMessageContext, String sCreationDate, String sLastUpdateDate)
    {
      
        synchronized (m_messageSet)
        {
            ArrayList<String> m_messageDetail = new ArrayList<String>();
            m_messageDetail.add(sMessageText);
            m_messageDetail.add(sMessageUserDetails);
            m_messageDetail.add(sMessageCause);
            m_messageDetail.add(sMessageAction);
            m_messageDetail.add(sMessageAdminDetails);
            m_messageDetail.add(sMessageAdminAction);
            m_messageDetail.add(sMessageContext);
            m_messageDetail.add(sCreationDate);
            m_messageDetail.add(sLastUpdateDate);
          //  sPath = sPath.substring(sPath.indexOf("fusionapps"));
            m_messageSet.add(sMessageName);
            m_messageToSeedFileMap.put(sMessageName, sPath);
            m_messageToMessageDetailMap.put(sMessageName, m_messageDetail);
        }

        if (isDiagMessage)
        {
            synchronized (m_diagMessageSet)
            {
                m_diagMessageSet.add(sMessageName);
            }
        }

    }


    public static String getProduct(String sPath)
    {
        String tokens[] = sPath.split("/");
        
        if(sPath.contains("components"))
        {
            return tokens[9].toUpperCase();
        }
        
        if(sPath.contains("/db/"))
        {
            return tokens[7].toUpperCase();
        }
        
        return "";
    }

}
