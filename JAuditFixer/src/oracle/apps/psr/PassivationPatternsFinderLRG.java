package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.NullEntityResolver;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.sqlanalyzer.ViewCriteriaHelper;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLNode;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PassivationPatternsFinderLRG extends JoesBaseClass 
{

    private HashMap<String, Integer> m_passivateFlagDistribution = new HashMap<String,Integer>();
    private static HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();
    
    private int m_numberOfVOsReviewed = 0;
    private int m_numberOfVOsWithCustomRowImpl = 0;
                                               
    private int m_numberOfVOAttrs = 1;
    private int m_numberOfVOAttrViolations = 1;
    private int m_numberOfVOAttrsToPassivate = 0;
    
    private int num_fixed = 0;
    private int num_audit_exemptions = 0;
    private static int releaseNo = 11;
    
    private static BufferedWriter writer;
    private static String mode = "violations";
    private static final Integer one = new Integer(1);
    private static final String issuetype = "Passivation";
    
    public PassivationPatternsFinderLRG(JoesBaseClass.CRAWL_TYPE crawL_TYPE) {
        super(crawL_TYPE);
    }

    private void updatePassivateFlagStats(Node value) 
    {
      String key;
      
      if(value == null || value.getNodeValue() == null)
        key = "null";
      else
          key = value.getNodeValue();

      Integer integer = m_passivateFlagDistribution.get(key);
      if(integer == null)
          integer = one;
      else
          integer = integer.intValue()+1;
      
      m_passivateFlagDistribution.put(key, integer);
      
      return;
    }


    public String getRowImplSourceCode(String voFile, String rowClassName) throws Exception 
    {
      int index = rowClassName.lastIndexOf('.') +1;
      String className = rowClassName.substring(index);
      
      index = voFile.lastIndexOf('/') +1;
      String path = voFile.substring(0, index);
      
      path = path + className +".java";
      
      File fFile = new File(path);
      if(!fFile.exists()) return "";
      StringBuffer fileData = new StringBuffer(1000);
      BufferedReader reader = new BufferedReader(new FileReader(fFile));

      char[] buf = new char[1024];
      int numRead = 0;
      while ((numRead = reader.read(buf)) != -1) {
          String readData = String.valueOf(buf, 0, numRead);
          fileData.append(readData);
          buf = new char[1024];
      }

      reader.close();
      String code = fileData.toString();

      return code;
    }


  private static final String sGeneralGetterRegex1 = whiteSpace + "public" + whiteSpace;
  private static final String sGeneralGetterRegex2 = whiteSpace + "(get";
  private static final String sGeneralGetterRegex3 = ")" + whiteSpace + "\\(" + whiteSpace + "\\)" + whiteSpace;
  private String getGeneralGetRegex(String type, String varName) {
    String varNameInitCaps = varName.substring(0, 1).toUpperCase() + varName.substring(1);
    String retVal = sGeneralGetterRegex1;
    retVal += type;
    retVal += sGeneralGetterRegex2;
    retVal += varNameInitCaps;
    retVal += sGeneralGetterRegex3;
    
    return retVal;
  }


  public String getGetter(String sourceNoComments, String type, String attrName) 
  {
    String regEx = getGeneralGetRegex(type, attrName);
 
    Pattern p = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
    Matcher m = p.matcher(sourceNoComments);
    
    if(m.find())
    {
        int start = m.start();
        int end = m.end();
        
        int index = sourceNoComments.indexOf("{", end) +1;
        int openCount = 1;

        while(openCount > 0) 
        {
            char c = sourceNoComments.charAt(index);
            
           if(c == '{')
               openCount++;
           else if(c == '}')
               openCount--;
            index++;
        }
        
        String retVal = sourceNoComments.substring(start, index);
        return retVal;
    }
      
    return "getter not found";
  }


  private static final String sGetterRegex1 = whiteSpace + "public" + whiteSpace;
  private static final String sGetterRegex2 = whiteSpace + "(get";
  private static final String sGetterRegex3 = ")" + whiteSpace + "\\(" + whiteSpace + "\\)" + whiteSpace + "\\{" + whiteSpace + "return" + whiteSpace +"\\(";
  private static final String sGetterRegex4 = "\\)" +whiteSpace + "getAttributeInternal\\(";
  private static final String sGetterRegex5 = "\\);" +whiteSpace + "\\}";
  private String getGetRegex(String type, String varName) {
      String varNameInitCaps = varName.substring(0, 1).toUpperCase() + varName.substring(1);

      String retVal = sGetterRegex1;
      retVal += type;
      retVal += sGetterRegex2;
      retVal += varNameInitCaps;
      retVal += sGetterRegex3;
      retVal += type;
      retVal += sGetterRegex4;
      retVal += varNameInitCaps.toUpperCase();
      retVal += sGetterRegex5;
    
      return retVal;
  }


    public boolean hasDefaultGetter(String source, String attrName, String type) 
    {
       String regEx = getGetRegex(type, attrName) ;
       //System.out.println(regEx);

      Pattern p = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
      Matcher m = p.matcher(source);
      
      if(m.find()) 
        return true;
      
      return false;
    }

    public void processFile(File fName, boolean bDoADE) 
    {
      String f = fName.getAbsolutePath();
      if(f.contains("/flex/") || f.contains("/publicFlex/") || f.contains("/viewLink/") || f.contains("/noship"))
          return;
      
      String absPathLowerCase = f.toLowerCase();

      //has false positive pattter super.getAttribute()..
      if(absPathLowerCase.contains("productmodel/common/publicationservice/view/itempublicationvo.xml"))
	return;
      
        if(absPathLowerCase.contains("oracle/apps/prc/po/processdemand/uimodel/demandworkbench/view/docbuilderlinesvo.xml"))
          return;

      m_numberOfVOsReviewed++;
      try {
          DOMParser parser = new DOMParser();
          parser.setDebugMode(true);
          parser.setErrorStream(System.out);
          parser.setEntityResolver(new NullEntityResolver());
          parser.showWarnings(true);
          parser.setValidationMode(DOMParser.NONVALIDATING);

          parser.parse("file:" + f);

          XMLDocument doc = parser.getDocument();
          
          NodeList viewObjects = doc.getElementsByTagName("ViewObject");
          if(viewObjects == null || viewObjects.getLength() != 1) 
           return;
        
        Node viewObject = viewObjects.item(0);
        NamedNodeMap voAttrs = viewObject.getAttributes();


        Node passivateFlag = voAttrs.getNamedItem("Passivate");
        updatePassivateFlagStats(passivateFlag);
        boolean passivateAll = false;
        if(passivateFlag != null && "All".equals(passivateFlag.getNodeValue())) 
            passivateAll = true;

        Node rowClass = voAttrs.getNamedItem("RowClass");
        String rowClassValue = "not set";
        if(rowClass != null)
            rowClassValue = rowClass.getNodeValue();
        
        boolean hasCustomRowImpl = false;
        
        String baseRowClass = "";
        if(rowClassValue.equals("oracle.apps.fnd.applcore.oaext.model.OAViewRowImpl"))
        {
          baseRowClass = "OAViewRowImpl";
        }
        else if (rowClassValue.equals("oracle.apps.fnd.applcore.oaext.model.DFFViewRowImpl"))
        {
          baseRowClass = "DFFViewRowImpl";
        }
        else if (rowClassValue.equals("oracle.apps.projects.foundation.base.view.PrjViewRowImpl"))
        {
          baseRowClass = "PrjViewRowImpl";
        }
        else if (rowClassValue.equals("oracle.jbo.server.ViewRowImpl"))
        {
          baseRowClass = "ViewRowImpl";
        }
        else if (rowClassValue.equals("oracle.apps.crmCommon.core.publicModel.CRMViewRowImpl"))
        {
          baseRowClass = "CRMViewRowImpl";
        }
        else if (rowClassValue.equals("oracle.apps.fnd.applcore.oaext.model.EFFViewRowImpl"))
        {
          baseRowClass = "EFFViewRowImpl";
        }
        else if (rowClassValue.equals("oracle.apps.hcm.common.pay.PayViewRowImpl"))
        {
          baseRowClass = "PayViewRowImpl";
        }
        else if (rowClassValue.equals("oracle.apps.hcm.common.hrCore.publicModel.PerViewRowImpl"))
        {
          baseRowClass = "PerViewRowImpl";
        }
        else if (rowClassValue.equals("oracle.apps.hcm.common.core.HcmViewRowImpl"))
        {
          baseRowClass = "HcmViewRowImpl";
        }
        else if (rowClassValue.equals("oracle.apps.hcm.talent.common.publicModel.TalentViewRowImpl"))
        {
          baseRowClass = "TalentViewRowImpl";
        }
        else
        {
          m_numberOfVOsWithCustomRowImpl++;
          hasCustomRowImpl = true;
        }

        ArrayList<String> m_attrNamesOfInterest = new ArrayList<String>(20);
        ArrayList<String> m_attrTypesOfInterest = new ArrayList<String>(20);
          
        NodeList viewAttributes = doc.getElementsByTagName("ViewAttribute");


        String rowImplSource = null;
        String rowImplSourceNoComments = null;

        if(hasCustomRowImpl)
        {
          if (!rowClassValue.contains("not set"))
          {
              rowImplSource = getRowImplSourceCode(f, rowClassValue);
              rowImplSourceNoComments = removeJavaComments(rowImplSource);
          }
        }

        for(int i = 0; i < viewAttributes.getLength(); i++)
        {
            m_numberOfVOAttrs++;
            Node node = viewAttributes.item(i);
            int lineNo = ((XMLNode)node).getLineNumber();
            
            NamedNodeMap attrAttrs = node.getAttributes();

            Node nameAttr = attrAttrs.getNamedItem("Name");
            String name = nameAttr.getNodeValue();

            String type = "[a-zA-Z_]+";
            String specifiedAttrType = "not set";
            
            Node typeAttr = attrAttrs.getNamedItem("Type");
            if(typeAttr != null)
            {
                specifiedAttrType = typeAttr.getNodeValue();
                type = specifiedAttrType;
                int index = type.lastIndexOf('.');
                if(index > 0)
                    type = type.substring(index+1);
            }
  
  
            String specifiedAttrPassivation = "not set";
            Node passivateAttr = attrAttrs.getNamedItem("Passivate");
            if(passivateAttr != null)
                specifiedAttrPassivation = passivateAttr.getNodeValue();
  
            boolean wouldPassivate = passivateAll;
            if(passivateAll == false) 
            {
              if(specifiedAttrPassivation != null && specifiedAttrPassivation.equals("true"))
                  wouldPassivate = true;
            }
            
            
            if(wouldPassivate)
            {
                m_numberOfVOAttrsToPassivate++;
    
                m_attrNamesOfInterest.add(name);
                m_attrTypesOfInterest.add(type);
            }


          boolean updateable = true;
          Node updateAttr = attrAttrs.getNamedItem("IsUpdateable");
          if(updateAttr != null && updateAttr.getNodeValue().equals("false"))
              updateable = false;
            
          boolean isEntityBased = false;
          Node entityAttr = attrAttrs.getNamedItem("EntityAttrName");
          if(entityAttr != null)
              isEntityBased = true;
            
            
          Node transientExpression = XMLParserHelper.getChildNodeWithName(node, "TransientExpression");
          Node recalcCondition = XMLParserHelper.getChildNodeWithName(node, "RecalcCondition");
            
          String transExpr = " ";
          String transExprValue = " ";
          String recalcCondn = " ";
            
          if(transientExpression != null) {
              transExpr = "present";
              transExprValue = transientExpression.getTextContent();
              if(recalcCondition == null || recalcCondition.getTextContent().trim().toLowerCase().equals("true"))
                recalcCondn = "true";
              else
                recalcCondn = recalcCondition.getTextContent().trim().toLowerCase();
          }
          if(recalcCondn.contains("true") && !recalcCondn.equals("true"));
            
          String getterAssessment = "";
          String customGetter = "";
        
          boolean hasExecute = false;
          boolean hasGetAttribute = false;

          if(hasCustomRowImpl == false) 
          {
            getterAssessment = baseRowClass;
          }
          else if (!rowClassValue.equals("not set"))
          {
              if(hasDefaultGetter(rowImplSource, name, type) == true) 
              {
                  getterAssessment = "Default Getter";
              }
              else 
              {
                customGetter = getGetter(rowImplSourceNoComments, type, name);
                if("getter not found".equals(customGetter)) {
                    
                    getterAssessment = "Getter Not Found in RowImpl";
                }
                  else
                {
                    getterAssessment = "Custom Getter";
                }


                hasExecute = customGetter.toLowerCase().contains("execute");
                hasGetAttribute = customGetter.toLowerCase().contains("getattribute");
                  
              }                 
          }
          boolean writeRow = false; //write to SpreadSheet or not
          if(!isExempt(getFileName(fName.getAbsolutePath()),name)) { 
            if((getterAssessment.equals("Custom Getter") && wouldPassivate && !hasGetAttribute && !isEntityBased) 
               || (transExpr.equals("present") && recalcCondn.equals("true") && wouldPassivate && !isEntityBased)
               || (wouldPassivate && !updateable && !isEntityBased)) {
              m_numberOfVOAttrViolations++;
              writeRow = true; //if a violation has been found
            }
          }
            
          int row = m_numberOfVOAttrViolations;
            if(mode.equals("all")) {
              row = m_numberOfVOAttrs;
              writeRow = true; //always write row if mode is all
            }

          if(writeRow) {
              if(mode.equals("violations"))
                   writer.write(getFileNameInfo(fName.getCanonicalPath()) + "," + lineNo + "," + 
                           name+ "," + specifiedAttrType + "," + getterAssessment + "," + 
                                String.valueOf(updateable) + "," + String.valueOf(wouldPassivate) +
                                "," + String.valueOf(passivateAll) + "," + String.valueOf(specifiedAttrPassivation) +
                                "," + String.valueOf(isEntityBased) + "," + String.valueOf(hasExecute) + 
                                "," + String.valueOf(hasGetAttribute) + "," + transExpr + "," + recalcCondn + "\n");
          }
        }
        

      } catch (Exception e) {
          e.printStackTrace();
          logException(f, e);
          System.err.println("ERROR:  Error while processing file.  Please review manually: " +fName.getAbsolutePath());
          return;
      }
    }
    
  private String getFileNameInfo(String absPath) {
    int i = 0;
    String family = "";
    String blankString =  "NA,NA,NA,NA,NA,";
    if(isEmpty(absPath))
        return blankString;
    if(absPath.startsWith("/ade/"))
        return blankString;
    
    if(absPath.contains("fusionapps/")) {
     i = absPath.indexOf("fusionapps/");
    }
    if(absPath.contains("fsm/")){
     i = absPath.indexOf("fsm/");
     family = "FSM";
    }
      if (absPath.contains("atgpf/")) {
          i = absPath.indexOf("atgpf/");
          family = "ATGPF";
      }
    String pathAfterFusionApps = absPath.substring(i);
    String[] parts = pathAfterFusionApps.split("/");
    String module = "";
    String product = "";
    
    if(parts == null) return blankString;
    
    if(absPath.contains("fusionapps/")) {
        int partsLength = parts.length;
        if(partsLength>1)
          family = parts[1].trim().toUpperCase();  
        if(partsLength>3)
          module = parts[3].trim();
        if(partsLength>4)
          product = parts[4].trim().toUpperCase();
        
    }
    if(absPath.contains("fsm/")) {
      int partsLength = parts.length;
      if(partsLength>2)
        module = parts[2].trim();
      if(partsLength>3)
        product = parts[3].trim().toUpperCase();
    }
      if (absPath.contains("atgpf/")) {
          int partsLength = parts.length;
          if (partsLength > 1)
              module = parts[1].trim();
          if (partsLength > 3)
              product = parts[3].trim().toUpperCase();
      }
       
    if(i<1)
       return family + "," + module + "," + product + "," + pathAfterFusionApps + "," ;
    
    String path_before_fusionapps = absPath.substring(0, i-1);
    parts = path_before_fusionapps.split("/");
    if(parts==null || parts.length<2)
      return blankString;
     
    String label = parts[parts.length -2] + "_" + parts[parts.length -1];      
    label = label.replace(".rdd","");   
    
    return family + "," + module + "," + product + "," + pathAfterFusionApps + "," + label;
  }

    protected String getSummaryReport() 
    {
        String report = "Passivation Report\n";
        report += "Number of VOs reviewed:  " + m_numberOfVOsReviewed +"\n";
        report += "Number of VOs with custom VO Row Impl:  " +m_numberOfVOsWithCustomRowImpl +"\n";
        
        report += "Distribution of VO passivate option:\n";

      Iterator<String> iter = m_passivateFlagDistribution.keySet().iterator();
      while(iter.hasNext()) 
      {
          String key = iter.next();
          Integer value = m_passivateFlagDistribution.get(key);
          
          
        report += "Passivate=" +key +", was set a total of " +value.intValue() +" times.\n";
      }

      report += "************************************************************************\n";
      report += "Stats on Attributes for which a non default row class exists.\n";
      report += "Number of VOs with Row Impl:  " +m_numberOfVOsWithCustomRowImpl +"\n";
      report += "Number of VO Attributes:  " +m_numberOfVOAttrs +"\n";
      report += "Number of VO Attributes marked to passivate:  " +m_numberOfVOAttrsToPassivate +"\n";
      report += "Average Number of Attributes:  " +((double)m_numberOfVOAttrs/m_numberOfVOsWithCustomRowImpl) +"\n";
      report += "Average Number of Passivated Attributes:  " +((double)m_numberOfVOAttrsToPassivate/m_numberOfVOsWithCustomRowImpl) +"\n";
      report += "************************************************************************\n\n";
      
      report += "Number of patterns fixed: " + num_fixed + "\n";
      report += "Number of audit exemptions: " + num_audit_exemptions + "\n";      

        return report;
    }

    protected String getSummaryReportSubject() 
    {
        return "SCRIPT: Passivation Summary Report";
    }


  public static void main(String[] args){
      try {
          if (args.length != 1 || args[0] == null || args[0].equals("")) {
              System.out.println("Series has to be specified for " + issuetype);
              System.exit(1);
          }

          String series = args[0];
          String release = FamilyModuleHelper.getRelease(series);
          if(!isEmpty(release)) {
              try{
                   releaseNo= Integer.parseInt(release);
              }catch (NumberFormatException e) {
                  e.printStackTrace();
                  releaseNo =11;
              }
          }

          String label = LRGUtil.getLatestLabel(series);
          System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
          System.out.println("Latest label is: " + label);

          writer = new BufferedWriter(new FileWriter("Passivation_LRG.csv"));
          writer.write("Family,Module,Product,Filename,Label,LineNum,AttrName,AttrType,Getter Assessment," +
                "Updateable, Passivation,VO Passivate All, Attr Passivate Setting,Entity Based,HasExecute,Has getAttribute,Trans Expr, Recalc Condition\n");
       
          generateExemptionList();
        
        String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
        String viewRoot = serverTop + "/"+series+".rdd/"+label+"/";
       
        ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
        
        PassivationPatternsFinderLRG x = new PassivationPatternsFinderLRG(JoesBaseClass.CRAWL_TYPE.VO_XML);
        
          for (int j = 0; j < families.size(); j++) {
            
              String family = families.get(j);
              String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, "RowCountThresholdNew");
            
              System.out.println("Crawling dir '" + viewRoot + sCrawlDir + "' for family: " + family);                
              x.crawlDirectoryNew(viewRoot,sCrawlDir,false);
              
              System.out.println("Crawling done for family" + family +".");
          }
          writer.close();

          LRGUtil.addLabelInfoToAB(issuetype, series, label);
          System.out.println("Finished running PassivationPatternsFinderLRG for "+(series+"_"+label));
      } catch (Exception e) {
          e.printStackTrace();
      }
  }
  
  public static boolean isEmpty(String str) {
      if (str == null || str.trim().equals(""))
          return true;
      return false;
  }
  
  public boolean isExempt(String fileName, String attr){
        if(!exemptions.containsKey(fileName)) 
            return false;
        
        HashSet<String> lines = (HashSet<String>)exemptions.get(fileName);
        if(lines.contains(attr))
            return true;
        
        return false;
    }
  
  public String getFileName(String filePath) {
    if (filePath.contains("fusionapps/")) {
            Integer fusionappsIndex = filePath.indexOf("fusionapps/");
            String path_after_fusionapps =
                filePath.substring(fusionappsIndex);
            return path_after_fusionapps;
    }
    if (filePath.contains("fsm/")) {
            Integer fusionappsIndex = filePath.indexOf("fsm/");
            String path_after_fusionapps =
                filePath.substring(fusionappsIndex);
            return path_after_fusionapps;
    }
    return filePath;
  }
  
    private static void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
              "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 3)
            continue;
        String issue = parts[2].trim();
        if(!issue.equals("Passivation"))
            continue;
        String fileName = parts[0].trim();
        String attrName = parts[1].trim();
          
          int exemptionRelease = -1;
         
          try{ 
          if(parts.length > 4)
              exemptionRelease = Integer.parseInt(parts[4].trim());
          }catch(Exception e){
              exemptionRelease = -1;
          }
          
          if(exemptionRelease == -1 || exemptionRelease >= releaseNo){
            HashSet<String> exemptLines = null;
            if(!exemptions.containsKey(fileName)) 
              exemptLines = new HashSet<String>();
            else 
              exemptLines = exemptions.get(fileName);
            
            exemptLines.add(attrName);
            exemptions.put(fileName, exemptLines);
          }
          
      }
        
        fileReader.close();
    }catch(Exception e) {
        e.printStackTrace();
    }
    } 
}
