package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileOutputStream;
import java.io.FileReader;

import java.io.FileWriter;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.HashSet;
import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.NullEntityResolver;
import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLNode;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;

public class PassivationPatternsFinder extends JoesBaseClass 
{
    Workbook m_myBook;
    Worksheet m_attributesSheet;
    
    int m_numberOfVOsReviewed = 0;
    int m_numberOfVOsWithCustomRowImpl = 0;
    HashMap<String, Integer> m_passivateFlagDistribution = new HashMap<String,Integer>();
    
    int m_numberOfVOAttrs = 1;
    int m_numberOfVOAttrViolations = 1;
    int m_numberOfVOAttrsToPassivate = 0;
    
    static BufferedWriter writer;
    static BufferedWriter writer1;
    static BufferedWriter writer2;
    int num_fixed = 0;
    int num_audit_exemptions = 0;
    
    static String mode = "violations";
    
    static boolean bDebug = false;
    static String sCrawlDir;
    
    HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>(); 
    
    public PassivationPatternsFinder(JoesBaseClass.CRAWL_TYPE crawL_TYPE) {
        super(crawL_TYPE);

      m_myBook = new Workbook("VO Attributes");

      m_attributesSheet = new Worksheet("Attributes");
      m_myBook.addWorksheet(m_attributesSheet);

      m_attributesSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("File Name")));
      m_attributesSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Path")));
      m_attributesSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Line Number")));
      m_attributesSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("Attribute Name")));
      m_attributesSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Attribute Type")));
      m_attributesSheet.addCell(new Cell(new CellLocation("F1"), new CellValue("Getter Assessment")));
      m_attributesSheet.addCell(new Cell(new CellLocation("G1"), new CellValue("Updateable")));
      m_attributesSheet.addCell(new Cell(new CellLocation("H1"), new CellValue("Passivation")));
      m_attributesSheet.addCell(new Cell(new CellLocation("I1"), new CellValue("VO Passivate All")));
      m_attributesSheet.addCell(new Cell(new CellLocation("J1"), new CellValue("Attribute Passivate Setting")));
      m_attributesSheet.addCell(new Cell(new CellLocation("K1"), new CellValue("Count")));
      m_attributesSheet.addCell(new Cell(new CellLocation("L1"), new CellValue("Entity Based")));
      m_attributesSheet.addCell(new Cell(new CellLocation("M1"), new CellValue("Has Execute")));
      m_attributesSheet.addCell(new Cell(new CellLocation("N1"), new CellValue("Has getAttribute")));      
      m_attributesSheet.addCell(new Cell(new CellLocation("O1"), new CellValue("Transient Expr")));     
      m_attributesSheet.addCell(new Cell(new CellLocation("P1"), new CellValue("Recalc Condition")));         
    }

    static final Integer one = new Integer(1);

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
//      code = replaceComments(code);

      return code;
    }


  private static final String sGeneralGetterRegex1 = whiteSpace + "public" + whiteSpace;
  private static final String sGeneralGetterRegex2 = whiteSpace + "(get";
  private static final String sGeneralGetterRegex3 =
      ")" + whiteSpace + "\\(" + whiteSpace + "\\)" + whiteSpace;

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
    //System.out.println(regEx);

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
  private static final String sGetterRegex3 =
      ")" + whiteSpace + "\\(" + whiteSpace + "\\)" + whiteSpace + "\\{" + whiteSpace + "return" + whiteSpace +"\\(";
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
      if (absPathLowerCase.contains("test.") || absPathLowerCase.contains("/test/") || 
          absPathLowerCase.contains("/modeltest") || absPathLowerCase.contains("/uitest/")
          || absPathLowerCase.contains("/testui") || absPathLowerCase.contains("servicetest") 
          || absPathLowerCase.contains("/publicservicetest") || absPathLowerCase.contains("/publicuitest")
          || absPathLowerCase.contains("/publicmodeltest") || absPathLowerCase.contains("/structuretest"))
          return;
      
      //has false positive pattter super.getAttribute()..
      if(absPathLowerCase.contains("productmodel/common/publicationservice/view/itempublicationvo.xml"))
	return;
        if(f.contains("oracle/apps/prc/po/processDemand/uiModel/demandWorkbench/view/DocBuilderLinesVO.xml"))
          return;

      m_numberOfVOsReviewed++;
      //System.out.println("INFO: Reviewing file: " + fName.getAbsolutePath());


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
          {
            System.out.println("WARNING:  Unexpected number of view objects found for file:  " +f);
            System.out.println("Skipping...");
            return;
          }
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
          if(recalcCondn.contains("true") && !recalcCondn.equals("true"))
              writer2.write(f + ", " + name +  ", " + recalcCondn +  " - If RecalcCondition always evaluates to true, " +
                  "then this attribute should be manually fixed to not passivate. \n");
            
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
//            m_attributesSheet.addCell(new Cell(new CellLocation(c6), new CellValue("OAViewRowImpl")));
//          String getter = getGetter(rowImplSource, rowImplSourceNoComments, type, attrName);
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
                  //Cell c = new Cell(new CellLocation(c6), new CellValue("Custom Getter"));
                 // m_attributesSheet.addCell(c);
                 // m_attributesSheet.createComment(getter, "F" + m_numberOfVOAttrs, 12, 30);
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
              String c1 = getCellLocation(0, row);
              String c2 = getCellLocation(1, row);
              String c3 = getCellLocation(2, row);
              String c4 = getCellLocation(3, row);
              String c5 = getCellLocation(4, row);
              String c6 = getCellLocation(5, row);
              String c7 = getCellLocation(6, row);
              String c8 = getCellLocation(7, row);
              String c9 = getCellLocation(8, row);
              String c10 = getCellLocation(9, row);
              String c11 = getCellLocation(10, row);
              String c12 = getCellLocation(11, row);
              String c13 = getCellLocation(12, row);
              String c14 = getCellLocation(13, row);
              String c15 = getCellLocation(14, row);
              String c16 = getCellLocation(15, row);
    
              m_attributesSheet.addCell(new Cell(new CellLocation(c1), new CellValue(fName.getName())));
              m_attributesSheet.addCell(new Cell(new CellLocation(c2), new CellValue(fName.getAbsolutePath())));
              m_attributesSheet.addCell(new Cell(new CellLocation(c3), new CellValue(lineNo)));
              m_attributesSheet.addCell(new Cell(new CellLocation(c4), new CellValue(name)));
              m_attributesSheet.addCell(new Cell(new CellLocation(c5), new CellValue(specifiedAttrType)));
              m_attributesSheet.addCell(new Cell(new CellLocation(c7), new CellValue(String.valueOf(updateable))));
              m_attributesSheet.addCell(new Cell(new CellLocation(c8), new CellValue(String.valueOf(wouldPassivate))));
              m_attributesSheet.addCell(new Cell(new CellLocation(c9), new CellValue(String.valueOf(passivateAll))));
              m_attributesSheet.addCell(new Cell(new CellLocation(c10), new CellValue(String.valueOf(specifiedAttrPassivation))));
              m_attributesSheet.addCell(new Cell(new CellLocation(c11), new CellValue(1)));
              m_attributesSheet.addCell(new Cell(new CellLocation(c12), new CellValue(String.valueOf(isEntityBased))));
    
              m_attributesSheet.addCell(new Cell(new CellLocation(c13), new CellValue(String.valueOf(hasExecute))));
              m_attributesSheet.addCell(new Cell(new CellLocation(c14), new CellValue(String.valueOf(hasGetAttribute))));
              
              Cell cell1 = new Cell(new CellLocation(c15), new CellValue(transExpr));
              m_attributesSheet.addCell(cell1);
              m_attributesSheet.addCell(new Cell(new CellLocation(c16), new CellValue(recalcCondn)));
              
              if(transExpr.equals("present"))
                m_attributesSheet.createComment(transExprValue, "O" + row, 8, 20);
                
              Cell cell2 = new Cell(new CellLocation(c6), new CellValue(getterAssessment));
              m_attributesSheet.addCell(cell2);
              
              if(getterAssessment.equals("Custom Getter"))
                  m_attributesSheet.createComment(customGetter, "F" + row, 10, 20);
              
              if(mode.equals("violations"))
                   writer.write(getFileNameInfo(fName.getCanonicalPath()) + "," + lineNo + "," + 
                           name+ "," + specifiedAttrType + "," + getterAssessment + "," + 
                                String.valueOf(updateable) + "," + String.valueOf(wouldPassivate) +
                                "," + String.valueOf(passivateAll) + "," + String.valueOf(specifiedAttrPassivation) +
                                "," + String.valueOf(isEntityBased) + "," + String.valueOf(hasExecute) + 
                                "," + String.valueOf(hasGetAttribute) + "," + transExpr + "," + recalcCondn + "\n");
          }
        }
        

//        if(m_attrNamesOfInterest.size() > 0)
//        {
//            String rowImplSource = getRowImplSourceCode(f, rowClassValue);
//            String rowImplSourceNoComments = replaceComments(rowImplSource);
//            
//           // System.out.println(rowImplSource);
//            
//            System.out.println("Names of interest are:");
//            Iterator<String> namesIter = m_attrNamesOfInterest.iterator();
//            Iterator<String> typesIter = m_attrTypesOfInterest.iterator();
//            while(namesIter.hasNext()) 
//            {
//                String attrName = namesIter.next();
//                String type = typesIter.next();
//                System.out.println(attrName);
//              System.out.println(type);
//                
//                if(hasDefaultGetter(rowImplSource, attrName, type) == false) 
//                {
//                  System.out.println("Attr is interesting --> " + attrName);
//                  String getter = getGetter(rowImplSource, rowImplSourceNoComments, type, attrName);
//                  //System.out.println(getter);
//                  //System.out.println(getter);
//                }
//            }
//        }

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


  public static void main(String[] args) throws Exception
  {
     if (args.length < 2 || args[0] == null || args[1] == null 
         || args[0].equals("") || args[1].equals("")) {
          System.out.println("Usage:  reviewPassivationPatterns.sh <path of excel output>");
          System.exit(1);
      }

      sCrawlDir = args[0];
      String sOutpFilename = args[1];
      Boolean bDoADE = false;
      if(args.length > 2)
         bDoADE = new Boolean(args[2]); 

      
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
     
    System.out.println("Analyzing all files in directory: " + sCrawlDir);
      
      if(args.length > 3) {
          mode = args[3];
          if(!mode.equals("all") && !mode.equals("violations")) {
              System.out.println("Unrecognized mode - Mode must be 'all' or 'violations'. Using the default mode 'violations'...");
              mode = "violations";
          }
      }
      
    boolean append = false;
    if(args.length > 4)
       append = new Boolean(args[4].trim());
    
    
      PassivationPatternsFinder x = new PassivationPatternsFinder(JoesBaseClass.CRAWL_TYPE.VO_XML);

      try {         
        
          writer = new BufferedWriter(new FileWriter("passivation_scan.csv",append));
          if(!append)
          writer.write("Family,Module,Product,Filename,Label,LineNum,AttrName,AttrType,Getter Assessment," +
                "Updateable, Passivation,VO Passivate All, Attr Passivate Setting,Entity Based,HasExecute,Has getAttribute,Trans Expr, Recalc Condition\n");
          
          writer1 = new BufferedWriter(new FileWriter("passivation_audit_exemption.csv"));
          writer1.write("Rule,File,Attribute,Bug\n");
          writer2 = new BufferedWriter(new FileWriter("errors.txt"));
        
          if (bDoADE.booleanValue())
              x.startDiffLog();

          x.generateExemptionList();
          x.crawlDirectory(sCrawlDir, bDoADE);
          
          writer.close();
          
          if(bDoADE.booleanValue())
              x.fixPassivationPatterns();
          
          if (bDoADE.booleanValue())
              x.closeDiffLog();

        FileOutputStream out = null;

        out = new FileOutputStream(sOutpFilename);
        XLSXCreator.create(x.m_myBook, out);
        writer1.close();
        writer2.close();

      } catch (Exception e) {
          e.printStackTrace();
          x.logException(sCrawlDir, e);
        } 
      finally{
        if(writer !=null)
            writer.close();
      }

      x.logUsageReport(sCrawlDir);
  }
  
  public static boolean isEmpty(String str) {
      if (str == null || str.trim().equals(""))
          return true;
      return false;
  }
  
  private void fixPassivationPatterns() throws Exception {
      
    BufferedReader reader = new BufferedReader(new FileReader("passivation_scan.csv"));
    String line = reader.readLine();
    String pathPrefix = getPathBeforeFusionapps();
    XmlFixer xmlFixer = null;
    XMLDocument doc = null;
    String prevFilePath = null;
    String fileContentsAsString = null;
    String[] fileContents = null;
    HashMap<String,HashSet> voAttributesPassivateAll = new HashMap<String,HashSet>();
    HashSet attributeNames = null;
    
    while((line = reader.readLine()) != null) {
        
      String[] parts = line.split(",");
      if(parts.length < 18)
        continue;
      
      String filePath = pathPrefix + parts[3].trim();
      String attrName = parts[6].trim();
        
      String vo_passivate_all = parts[11].trim();
        
      String updateable = parts[9].trim();
      if(updateable.equalsIgnoreCase("true")) {
        String transientExpr = parts[16].trim();
        String recalcCondn = parts[17].trim();
        if(!transientExpr.equalsIgnoreCase("present") || !recalcCondn.equalsIgnoreCase("true")) {
          writer1.write("File.AdfModel.55," + getFileNameAfterViewRoot(filePath) + "," + attrName + ",Bug\n");
          num_audit_exemptions++;
        }
      }
        
      if(vo_passivate_all.equalsIgnoreCase("true")) {      
          
          if(voAttributesPassivateAll.containsKey(filePath))
            attributeNames = voAttributesPassivateAll.get(filePath);        
          else
            attributeNames = new HashSet<String>();
          
          attributeNames.add(attrName);
          voAttributesPassivateAll.put(filePath, attributeNames);
          continue;
      }
        
      try {
        if(!filePath.equals(prevFilePath)) {
          
            if(prevFilePath != null) {
              fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
              applyFix(prevFilePath,fileContentsAsString);
            }
          
          doc = XMLParserHelper.getXMLDocument(filePath);   
          if(doc == null) {
            writer2.write("Could not parse file: " + filePath + "\n\n");
            continue;
          }
          xmlFixer = new XmlFixer(filePath);
          if(xmlFixer == null) {
            writer2.write("Could not obtain XMLFixer for: " + filePath + "\n\n");
            continue;
          }
          fileContents = xmlFixer.convertFileToStringArray();
        }
        prevFilePath = filePath;        
          
       Node n = findElement(doc,parts[6].trim());
       xmlFixer.removeAttributeWithSpaces(n, "Passivate", "true");
       num_fixed++;
            
      } catch (Exception e) {
            System.out.println("Some exception occured while processing file: " + filePath);
            e.printStackTrace();
            System.out.println("Skipping to next file...");
            logException(filePath, e);
            writer2.write("Some exception occured while processing file: " + filePath + ": " + e.getMessage() + "\n\n");
      }
    }
    reader.close();
    //apply fix to the last file
    if(xmlFixer != null) {
      fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
      applyFix(prevFilePath,fileContentsAsString);
    }
    
    //Now fix VO_PAssivate_All attributes
    fixVoPassivateAllAttributes(voAttributesPassivateAll);
  }
  
  private void fixVoPassivateAllAttributes(HashMap<String,HashSet> voAttributesPassivateAll) throws Exception{
      
    printMap(voAttributesPassivateAll);
    
    Iterator it = voAttributesPassivateAll.keySet().iterator();
    XmlFixer xmlFixer = null;
    String[] fileContents = null;
    String fileContentsAsString = null;
    XMLDocument doc = null;
    
    while(it.hasNext()){
     
      String filePath = (String)it.next();
      HashSet<String> attributeNames = voAttributesPassivateAll.get(filePath);
      try {
          xmlFixer = new XmlFixer(filePath);
          doc = XMLParserHelper.getXMLDocument(filePath); 
          fileContents = xmlFixer.convertFileToStringArray();
            
          NodeList list = doc.getElementsByTagName("ViewObject");
            
          if(list.getLength() != 1) {
            writer2.write("File does not have exactly one ViewObject...Skipping\n");
            continue;
          }        
          Node viewObj = list.item(0);
          xmlFixer.removeAttributeWithSpaces(viewObj, "Passivate", "All");
          
          list = doc.getElementsByTagName("ViewAttribute");
          for(int i = 0; i< list.getLength(); i++) {
              Node n = list.item(i);
              String attrName = XMLParserHelper.getAttributeValue(n, "Name");
              if(attributeNames.contains(attrName)){
                  String attrPassivateSetting = XMLParserHelper.getAttributeValue(n, "Passivate");
                  if(attrPassivateSetting != null && attrPassivateSetting.equals("true"))
                      xmlFixer.removeAttributeWithSpaces(n, "Passivate", "true");
                      num_fixed++;
              }else {
                  boolean isEntityBased = false;
                  String entityAttr = XMLParserHelper.getAttributeValue(n, "EntityAttrName");
                  if(entityAttr != null)
                    isEntityBased = true;
                  if(isEntityBased)
                      continue;
                  String attrPassivateSetting = XMLParserHelper.getAttributeValue(n, "Passivate");
                  if(attrPassivateSetting != null && !attrPassivateSetting.equals("true") ) {
                    System.out.println("attrPassivate Setting is not true for a non-transient attr!!!");
                    writer2.write("attrPassivate Setting is not true for a non-transient attr: " 
                                  + attrName + " -> " + filePath + "\n");
                      continue;
                  }
                  if(attrPassivateSetting == null)
                    xmlFixer.addAttribute(n, "Passivate", "true");
                    num_fixed++;
              }
          }
          fileContentsAsString = xmlFixer.getFileContentsAsString(false);
          applyFix(filePath,fileContentsAsString);
        }
        catch(Exception e) {
          writer2.write(e.getMessage() + ":" + filePath + "\n");
        }
    } //while
  } //method
  
  private String getPathBeforeFusionapps(){
  int i = sCrawlDir.indexOf("fusionapps/");
  if(i == -1)
      i = sCrawlDir.indexOf("fsm/");
  return sCrawlDir.substring(0,i);
  }
  
  private void applyFix(String absPath,
                      String newFileContents) throws Exception {
    if(bDebug)
         System.out.println("applying Fix to file: "+ absPath);
     File file = new File(absPath);
     if (!file.exists()) {
         throw new Exception("while making fix, file not found: " +
                             absPath);
     }
    checkoutAndDelete(file);
      
     System.out.println("Writing new file...");
     FileWriter fw = new FileWriter(file.getAbsolutePath());
     BufferedWriter bw = new BufferedWriter(fw);
     bw.write(newFileContents);
     if (bw != null)
         bw.close();
     if (fw != null)
         fw.close();
    checkinAndDiff(file);
  }
  
  private Node findElement(XMLDocument doc, String attrName){
    NodeList list = doc.getElementsByTagName("ViewAttribute");
    for(int i = 0; i< list.getLength(); i++) {
        Node n = list.item(i);
        String attrValue = XMLParserHelper.getAttributeValue(n, "Name");
        if(attrValue != null && attrValue.equals(attrName)) return n;
    }
    return null;
  }
  
  private void printMap(HashMap<String,HashSet> voAttributesPassivateAll) {
    Iterator it = voAttributesPassivateAll.keySet().iterator();
    while(it.hasNext()){
      String filePath = (String)it.next();
      HashSet attributeNames = voAttributesPassivateAll.get(filePath);
      System.out.println(filePath + " --> " + attributeNames );
    }
  }
  
  
  private String getFileNameAfterViewRoot(String filePath) {
      
    if (filePath.contains("fusionapps/")) {
            Integer fusionappsIndex = filePath.indexOf("fusionapps/");
            String path_after_fusionapps =
                filePath.substring(fusionappsIndex);
            return path_after_fusionapps;
    }
    if (filePath.contains("fsm/")) {
            Integer fsmIndex = filePath.indexOf("fsm/");
            String path_after_fsm =
                filePath.substring(fsmIndex);
            return path_after_fsm;
    }
    return filePath;
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
        if(!issue.equals("Passivation"))
            continue;
        String fileName = parts[0].trim();
        String attrName = parts[1].trim();
          
        HashSet<String> exemptLines = null;
        if(!exemptions.containsKey(fileName)) 
          exemptLines = new HashSet<String>();
        else 
          exemptLines = exemptions.get(fileName);
        
        exemptLines.add(attrName);
        exemptions.put(fileName, exemptLines);
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }
  
}
