package oracle.apps.rowCountAnalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileOutputStream;

import java.io.FileReader;

import java.io.FileWriter;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.NullEntityResolver;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SetRowCountThreshold extends JoesBaseClass
{
  int m_page_reviewCount = 0;
  int m_pageDef_reviewCount = 0;

  int m_fixCount = 0;
  private String m_sSummaryReport = "";
  
  HashMap<String, Integer> m_rctSettings = new HashMap<String, Integer>();

  HashSet<String> m_pageList = new HashSet<String>();
  HashMap<String,String> m_pageDefList = new HashMap<String,String>();
  HashSet<String> m_CPXList = new HashSet<String>();
  
  HashMap<String, ArrayList<String>> m_pageToPageDefUsageId = new HashMap<String, ArrayList<String>>();
  HashMap<String, String> m_usageIdToPageDef = new HashMap<String, String>();
  HashSet<String> violations = new HashSet<String>();
  
  static BufferedWriter writer;
  
  public static boolean premergeMode = false;
  public static Boolean bDoADE = true;
  
  boolean bDebug = false;  
  HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();    

  public SetRowCountThreshold() 
  {
      super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF_AND_PAGE_DEF_AND_CPX);
  }

  protected String getSummaryReportSubject() 
  {
      return "SCRIPT: Set RowCountThreshold to -1";
  }

  protected String getSummaryReport() 
  {
      m_sSummaryReport = "For this run I reviewed a total of " +m_page_reviewCount +" pages.\n" + m_sSummaryReport;
      m_sSummaryReport = "For this run I reviewed a total of " +m_pageDef_reviewCount +" pageDefs.\n" + m_sSummaryReport;
      m_sSummaryReport = "For this run I fixed a total of    " +m_fixCount +" pageDefs.\n" + m_sSummaryReport;
      
      String rctBreakdown = "During this run, I found the following RCT settings.\n";
      Iterator<String> iter = m_rctSettings.keySet().iterator();

      while(iter.hasNext()) 
      {
        String rctValue = iter.next();
        Integer count = m_rctSettings.get(rctValue);
        String line = rctValue + " was found " +count+ " times.\n";
        rctBreakdown += line;
      }
      
      m_sSummaryReport += rctBreakdown;

      return m_sSummaryReport;
  }

  static Integer one = new Integer(1);

  private void updateRCTCounts(String rctValue) 
  {
    Integer count = m_rctSettings.get(rctValue);
    
    if(count == null) 
    {
      m_rctSettings.put(rctValue, one);
      return;
    }
    
    m_rctSettings.put(rctValue, count.intValue()+1);
  }

    private void fixPageDef(String pageDef, String id, String binding, boolean bDoADE) throws Exception
    {
        boolean fixed = false;
        
        String x = "id=\"" +id +"\"";
        String y = "Binds=\"" +binding +"\"";
        
        File fp = new File(pageDef);
        FileReader reader = new FileReader( fp );
        BufferedReader input = new BufferedReader( reader );
        FileReader reader2 = new FileReader( fp );
        BufferedReader input2 = new BufferedReader( reader2 );

        String line;

        StringBuffer sb = new StringBuffer();

        int lineNo = 1;

        while( (line = input.readLine()) != null )
        {
            input2.skip(line.length());
            input2.mark(1024);
            int c1 = input2.read();
            int c2 = input2.read();
            input2.reset();

            String newline = "\n";
            if(c1 == -1)
            {
                newline = "";
            }
            else if(c1 == '\n')
            {
                input2.read();
            }
            else if(c1 == '\r' && c2 != '\n')
            {
                newline = "\r";
                input2.read();
            }
            else if(c1 == '\r' && c2 == '\n')
            {
                newline = "\r\n";
                input2.read();
                input2.read();
            }

            while(line.contains(">") == false && input.ready())
            {
                String extraLine = input.readLine();
                line += newline+extraLine;
                lineNo++;

                input2.skip(extraLine.length());
                input2.mark(1024);
                c1 = input2.read();
                c2 = input2.read();
                input2.reset();

                newline = "\n";
                if(c1 == -1)
                {
                    newline = "";
                }
                else if(c1 == '\n')
                {
                    input2.read();
                }
                else if(c1 == '\r' && c2 != '\n')
                {
                    newline = "\r";
                    input2.read();
                }
                else if(c1 == '\r' && c2 == '\n')
                {
                    newline = "\r\n";
                    input2.read();
                    input2.read();
                }
            }


            if(line.contains("<iterator") && line.contains(x) && line.contains(y)) 
            {
                if(line.contains("RowCountThreshold")) 
                {
                    int index = line.indexOf("RowCountThreshold") +19;
                    String leftOver = line.substring(index);
                    int loc = leftOver.indexOf("\"");
                    leftOver = leftOver.substring(loc);

                    String str = line.substring(0, index) + "-1" +leftOver;
                    line = str;
                    fixed = true;
                }
                else 
                {
                    int index = line.indexOf("/>");
                    if(index == -1)
                        index = line.indexOf(">");
                    
                    String str = line.substring(0, index) + " RowCountThreshold=\"-1\"" +line.substring(index);
                    line = str;
                    fixed = true;
                }
            }

            sb.append(line);
            
            sb.append(newline);

            lineNo++;
        }

        reader.close();

        if(fixed == false)
            return;

        if(bDoADE == false)
        {
            return;
        }

        m_sSummaryReport += "Applying fix to file: " +pageDef +"\n";

        checkoutAndDelete(fp);

        FileWriter fw = new FileWriter(fp.getAbsolutePath());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(sb.toString());
        bw.close();

        m_fixCount++;
        checkinAndDiff(fp);        
    }

    protected void checkPageDef(String pageDef, String binding, boolean bDoADE) 
    {
      if(bDebug)
        System.out.println("Checking PageDef:  " + pageDef);

      try {
          File f = new File(pageDef);
          DOMParser parser = new DOMParser();
          parser.setDebugMode(true);
          parser.setErrorStream(System.out);
          parser.setEntityResolver(new NullEntityResolver());
          parser.showWarnings(true);
          parser.setValidationMode(DOMParser.NONVALIDATING);

          parser.parse("file:" + pageDef);

          XMLDocument doc = parser.getDocument();
          NodeList adfc = doc.getElementsByTagName("pageDefinition");
          if (adfc == null) 
          {
            if(bDebug)
              System.out.println("REVIEW:  Not a pageDefinition file ---> " + pageDef);
             return;
          }

        NodeList iteratorList = doc.getElementsByTagName("iterator");

        for (int i = 0; i < iteratorList.getLength(); i++) 
        {
          Node iter = iteratorList.item(i);
          NamedNodeMap attributes = iter.getAttributes();
          Node text = attributes.getNamedItem("id");
          Node bindsNode = attributes.getNamedItem("Binds");
            
          if(bindsNode == null)
            continue;            

          if(binding.equals(bindsNode.getNodeValue())) {
              String id = text.getNodeValue();
              
              Node rct = attributes.getNamedItem("RowCountThreshold");
              if(rct != null)
              {
                  String rctText = rct.getNodeValue();
                  updateRCTCounts(rctText);
                  if(rctText.equals("-1"))
                      return;
              
                  if(isExempt(id,FamilyModuleHelper.getPathAfterViewRoot(f.getAbsolutePath())))
                      continue;
                  violations.add(FamilyModuleHelper.getFileNameInfo(f.getCanonicalPath()) + "RowCountThreshold," + id + "," + rctText + "\n");
                  fixPageDef(pageDef, id, binding, bDoADE);
                  return;
              }
              else 
              {
                  if(isExempt(id,FamilyModuleHelper.getPathAfterViewRoot(f.getAbsolutePath())))
                      continue;
                  violations.add(FamilyModuleHelper.getFileNameInfo(f.getCanonicalPath()) + "RowCountThreshold," + id + "," + "RCT Not Set\n");
                  fixPageDef(pageDef, id, binding, bDoADE);
                  updateRCTCounts("RCT Not Set");
              }
          }
        }
      } catch (Exception e) {
          e.printStackTrace();
          logException(pageDef, e);
          System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +pageDef);
          return;
      }
      return;
    }
    
    private void reviewPageDef(String page, String binding, boolean bDoADE) throws IOException
    {
        if(bDebug)
          System.out.println("Reviewing pageDef for page:  " +page);
        
        int index = page.indexOf("public_html") +11;
        String key = page.substring(index);
        if(bDebug)
          System.out.println(key);
        
        ArrayList<String> usageIdList = m_pageToPageDefUsageId.get(key);
        if(usageIdList == null) return;

        for(int i = 0; i < usageIdList.size(); i++)
        {
          String usageId = usageIdList.get(i);
  
          String path = m_usageIdToPageDef.get(usageId);
          if(bDebug)
            System.out.println(path);
          
          index = binding.indexOf("bindings.")+9;
          String str = binding.substring(index);
          index = str.indexOf(".");
          str = str.substring(0, index);
          if(bDebug)
            System.out.println(str);
          
          if(path == null || path.equals("not found"))
          {
              System.out.println("***************************************************************************");
              System.out.println("REVIEW:  Could not resolve pagedef for this page.");
              System.out.println("You will need to process this one manually.");
              System.out.println("page:  " +page);
              System.out.println("binding:  " + binding);
              System.out.println("***************************************************************************");
              continue;
          }
          
          if(premergeMode){
            File f = new File(path);
            if(RCTPremergeChecker.processPageDef(f.getCanonicalPath()))
              checkPageDef(f.getAbsolutePath(), str, bDoADE);
          }
          else
            checkPageDef(path, str, bDoADE);
        }
        
    }

  protected void processPage(String f, boolean bDoADE) 
  {
    m_page_reviewCount++;
    if(bDebug)
       System.out.println("Checking Page:  " +f.toString());

      try {
          DOMParser parser = new DOMParser();
          parser.setDebugMode(true);
          parser.setErrorStream(System.out);
          parser.setEntityResolver(new NullEntityResolver());
          parser.showWarnings(true);
          parser.setValidationMode(DOMParser.NONVALIDATING);

          parser.parse("file:" + f);

          XMLDocument doc = parser.getDocument();
          NodeList adfc = doc.getElementsByTagName("jsp:root");
          if (adfc == null) 
          {
              System.out.println("REVIEW:  Not a page file ---> " + f);
              return;
          }

        NodeList tableList = doc.getElementsByTagName("table");

        for (int i = 0; i < tableList.getLength(); i++) 
        {
          Node iter = tableList.item(i);
          NamedNodeMap attributes = iter.getAttributes();
          Node text = attributes.getNamedItem("id");
          if(text == null)
              continue;

          if(bDebug)
             System.out.println("Found table --->  " +text.getNodeValue());

          Node value = attributes.getNamedItem("value");
          if(value != null)
          {
              String valueText = value.getNodeValue();
              reviewPageDef(f, valueText, bDoADE);
              
          }
        }


      } catch (Exception e) {
          e.printStackTrace();
          logException(f, e);
          System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +f);
          return;
      }
  }

  protected void processPageDef(File f, boolean bDoADE) 
  {
    m_pageDef_reviewCount++;
    if(bDebug)
      System.out.println("Checking PageDef:  " +f.toString());

    try {
        DOMParser parser = new DOMParser();
        parser.setDebugMode(true);
        parser.setErrorStream(System.out);
        parser.setEntityResolver(new NullEntityResolver());
        parser.showWarnings(true);
        parser.setValidationMode(DOMParser.NONVALIDATING);

        parser.parse("file:" + f.getCanonicalPath());

        XMLDocument doc = parser.getDocument();
        NodeList adfc = doc.getElementsByTagName("pageDefinition");
        if (adfc == null) 
        {
            System.out.println("REVIEW:  Not a pageDefinition file ---> " + f.getCanonicalPath());
            return;
        }

      NodeList iteratorList = doc.getElementsByTagName("iterator");

      for (int i = 0; i < iteratorList.getLength(); i++) 
      {
        Node iter = iteratorList.item(i);
        NamedNodeMap attributes = iter.getAttributes();
        Node text = attributes.getNamedItem("id");
        if(text == null)
            continue;
        if(bDebug)
          System.out.println("Found iterator --->  " +text.getNodeValue());

        Node rct = attributes.getNamedItem("RowCountThreshold");
        if(rct != null)
        {
            String rctText = rct.getNodeValue();
            updateRCTCounts(rctText);
        }
        else 
        {
            updateRCTCounts("RCT Not Set");
        }
      }


    } catch (Exception e) {
        e.printStackTrace();
        logException(f.getAbsolutePath(), e);
        System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +f.getAbsolutePath());
        return;
    }

  }

  public void processFile(File fName, boolean bDoADE) 
  {
    String fileName = fName.getPath();

    if(fileName.endsWith(".jsff") || fileName.endsWith(".jspx"))
        m_pageList.add(fileName);
    
//        processPage(fName, bDoADE);
    
        if(fileName.contains("PageDef"))
            if (fileName.matches(".*PageDef[0-9]*\\.xml")){
                String relPath = fileName;
                int index = fileName.indexOf("oracle/apps");
                if(index != -1)
                    relPath = fileName.substring(index);
              m_pageDefList.put(relPath,fileName);
            }

    if(fileName.endsWith(".cpx"))
        m_CPXList.add(fileName);
//        processPageDef(fName, bDoADE);
  }

    private String getRealPageDefPath(String path) 
    {
        String actual = path.replaceAll("\\.", "/") +".xml";
        
        if(m_pageDefList.containsKey(actual))
            return m_pageDefList.get(actual);
        
        return "not found";
    }

    private void digestCPX(String f) 
    {   
        if(bDebug)
          System.out.println("Reviewing CPX:  " + f);
        try {
            DOMParser parser = new DOMParser();
            parser.setDebugMode(true);
            parser.setErrorStream(System.out);
            parser.setEntityResolver(new NullEntityResolver());
            parser.showWarnings(true);
            parser.setValidationMode(DOMParser.NONVALIDATING);

            parser.parse("file:" + f);

            XMLDocument doc = parser.getDocument();

        NodeList pageMaps = doc.getElementsByTagName("pageMap");
        NodeList pageDefUsages = doc.getElementsByTagName("pageDefinitionUsages");
        if(pageMaps.getLength() != 1 || pageDefUsages.getLength() != 1)
        {
            System.out.println("ERROR:  Unexpected number of pageMap/pageDefinitionUsages sections in file ---> " +f);
            return;
        }

        Node pageMap = pageMaps.item(0);
        NodeList pages = pageMap.getChildNodes();
        for(int i = 0; i < pages.getLength(); i++) 
        {
            Node page = pages.item(i);
            if(page.getNodeType() == Node.TEXT_NODE || page.getNodeType() == Node.COMMENT_NODE)
                continue;

            NamedNodeMap attributes = page.getAttributes();

            Node path = attributes.getNamedItem("path");
            Node usageId = attributes.getNamedItem("usageId");
            String sPage = "";
            String sUsageId = "";
            
            
            if(bDebug)
              System.out.println("found path and usageId --> " + path.getNodeValue() + ",    " +usageId.getNodeValue());
            //check if it is there already
            if(path != null)
                sPage = path.getNodeValue();
            if(usageId !=null)
                sUsageId = usageId.getNodeValue();
            
            ArrayList<String> usageIdList = m_pageToPageDefUsageId.get(sPage);
            if(usageIdList == null)
            {
                usageIdList = new ArrayList<String>();
                m_pageToPageDefUsageId.put(sPage, usageIdList);
            }
            
            usageIdList.add(sUsageId);
            
//            m_pageToPageDefUsageId.put(sPage, sUsageId);
        }

        Node pageDefUsage = pageDefUsages.item(0);
        NodeList usages = pageDefUsage.getChildNodes();

            for(int i = 0; i < usages.getLength(); i++) 
            {
                Node page = usages.item(i);
                if(page.getNodeType() == Node.TEXT_NODE || page.getNodeType() == Node.COMMENT_NODE)
                    continue;

                NamedNodeMap attributes = page.getAttributes();
                Node usageId = attributes.getNamedItem("id");
                Node path = attributes.getNamedItem("path");
                
                if(bDebug)
                   System.out.println("found path and usageId --> " + path.getNodeValue() + ",    " +usageId.getNodeValue());
                String nv = path.getNodeValue();

                String sUsageId= "";
                if(usageId != null)
                    sUsageId = usageId.getNodeValue();
             
                String realPath = getRealPageDefPath(nv);
              
                m_usageIdToPageDef.put(sUsageId, realPath);
            }



        } catch (Exception e) {
            e.printStackTrace();
            logException(f, e);
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it. " +f);
            return;
        }
    }

  public void processFiles(boolean bDoADE) 
  {
    // first load the cpx so we have a mapping between pages and pageDefs
      for(Iterator i = m_CPXList.iterator() ; i.hasNext();)
        digestCPX((String)i.next());
      
      for(Iterator i = m_pageList.iterator(); i.hasNext();)
          processPage((String)i.next(), bDoADE);
  }

  public static void main(String[] args) 
  {
      if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
          System.out.println("Usage:  setTableRCT.sh <path to your code in ade> <do ade>");
          System.exit(1);
      }
      
      boolean append = false;
      String sCrawlDir = args[0].trim();
      if(args.length > 1)
         bDoADE = new Boolean(args[1].trim());
      if(args.length > 2)
          append = new Boolean(args[2].trim());
      
      File f = new File(sCrawlDir);
      if(!f.exists()){
          System.out.println("Crawl directory does not exist - " + sCrawlDir);
          System.exit(1);
      }
      
      /*if(sCrawlDir.contains("/LATEST")) {
          int ind = sCrawlDir.indexOf("/LATEST");
          String pathBeforeLatest = sCrawlDir.substring(0,ind);
          File series = new File(pathBeforeLatest);
          File[] labels = series.listFiles();
          
          long lastModified = 0;
          String latestLabel = "";
          for(int i = 0; i < labels.length; i++) {
              if(labels[i].lastModified() > lastModified) {
                  lastModified = labels[i].lastModified();
                  latestLabel = labels[i].getName();
              }
          }
          if(!latestLabel.equals(""))
              sCrawlDir = sCrawlDir.replace("LATEST", latestLabel);
      }*/
      
      
//        String sCrawlDir = "/ade/sudgupta_fusionapps_erp_apr20_view/fusionapps/fin/components/receivables/ar/receipts/shared";
//        Boolean bDoADE = false;
//        boolean append = false;

      SetRowCountThreshold x = new SetRowCountThreshold();

      try {
          writer = new BufferedWriter(new FileWriter("rowCountThreshold.csv", append));
          if(!append)
              writer.write("Family,Module,Product,Filename,Label,Issue,IteratorId,RCT\n");  //"RowCountThreshold," + id + "," + rctText + "\n");
          if (bDoADE.booleanValue())
              x.startDiffLog();

          x.generateExemptionList();
          x.crawlDirectory(sCrawlDir, bDoADE);
          
          x.processFiles(bDoADE);         
          x.writeViolations();

          if (bDoADE.booleanValue())
              x.closeDiffLog();
          writer.close();
      } catch (Exception e) {
          e.printStackTrace();
          x.logException(sCrawlDir, e);
      }

      x.logUsageReport(sCrawlDir, !bDoADE);
  }
    
    public void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 3)
            continue;
        String issue = parts[2].trim();
        if(!issue.equals("RowCountThreshold"))
            continue;
        String fileName = parts[0].trim();
        String iteratorId = parts[1].trim();
          
        HashSet<String> exemptLines = null;
        if(!exemptions.containsKey(fileName)) 
          exemptLines = new HashSet<String>();
        else 
          exemptLines = exemptions.get(fileName);
        
        exemptLines.add(iteratorId);
        exemptions.put(fileName, exemptLines);
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }
    
    private boolean isExempt(String id, String pageDef){
        
        if(!exemptions.containsKey(pageDef)) 
            return false;
        
        HashSet<String> lines = (HashSet<String>)exemptions.get(pageDef);
        if(lines.contains(id))
            return true;
        
        return false;
    }
    
    public void writeViolations() throws Exception{
        for(Iterator<String> it = violations.iterator(); it.hasNext();){
            writer.write(it.next());
        }
    }
}
