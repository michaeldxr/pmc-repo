package oracle.apps.psr;

import java.io.BufferedWriter;

import java.io.File;
import java.io.FileWriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.NullEntityResolver;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLNode;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PassivationPreMergeViolations {
   
    private static PassivationPatternsFinder passivationFinder;
    private static BufferedWriter writer;
    private static int violations = 0;
    
    public static void main(String[] args) throws Exception{
      String ade_view_root = args[1].trim();
      String filelistpath=args[0].trim();
      String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root); 
      passivationFinder = new PassivationPatternsFinder(JoesBaseClass.CRAWL_TYPE.VO_XML);
      passivationFinder.generateExemptionList();
      
      PassivationPreMergeViolations preMergeChecker = new PassivationPreMergeViolations();
      
      writer = new BufferedWriter(new FileWriter("passivation_violations.txt"));
      
      for(int i = 0; i < files.length; i++) {
          
          String filePath = ade_view_root + "/" + files[i];
          File f = new File(filePath);
          if(!f.exists())
              continue;
        if(preMergeChecker.isFileOfInterest(files[i]))
          preMergeChecker.processFile(ade_view_root + "/" + files[i]);
      }
      writer.close();
    }
    
    private boolean isFileOfInterest(String f){
      if(f.contains("/flex/") || f.contains("/publicFlex/") || f.contains("/viewLink/") 
         || f.contains("/noship") || f.contains("/link/") || f.contains("/association/"))
          return false;
      
      String absPathLowerCase = f.toLowerCase();
      if (absPathLowerCase.contains("test.") || absPathLowerCase.contains("/test/") || 
          absPathLowerCase.contains("/modeltest") || absPathLowerCase.contains("/uitest/")
          || absPathLowerCase.contains("/testui") || absPathLowerCase.contains("/servicetest") 
          || absPathLowerCase.contains("/publicservicetest") || absPathLowerCase.contains("/publicuitest")
          || absPathLowerCase.contains("/publicmodeltest") || absPathLowerCase.contains("/structuretest"))
          return false;
      
        //has false positive pattter super.getAttribute()..
        if(absPathLowerCase.contains("productmodel/common/publicationservice/view/itempublicationvo.xml"))
          return false;
        
        if(absPathLowerCase.contains("oracle/apps/prc/po/processdemand/uimodel/demandworkbench/view/docbuilderlinesvo.xml"))
          return false;
      
      if (f.endsWith("VO.xml"))
          return true;
      
      return false;      
    }
    
  public void processFile(String f) 
  {     
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
        hasCustomRowImpl = true;
      }

      NodeList viewAttributes = doc.getElementsByTagName("ViewAttribute");

      String rowImplSource = null;
      String rowImplSourceNoComments = null;

      if(hasCustomRowImpl)
      {
        if (!rowClassValue.contains("not set"))
        {
            rowImplSource = passivationFinder.getRowImplSourceCode(f, rowClassValue);
            rowImplSourceNoComments = removeJavaComments(rowImplSource);
        }
      }

      for(int i = 0; i < viewAttributes.getLength(); i++)
      {
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
            if(passivationFinder.hasDefaultGetter(rowImplSource, name, type) == true) 
            {
                getterAssessment = "Default Getter";
            }
            else 
            {
              customGetter = passivationFinder.getGetter(rowImplSourceNoComments, type, name);
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
        boolean isViolation = false; //write to SpreadSheet or not
          
        String issueType = "";
     
          if(getterAssessment.equals("Custom Getter") && wouldPassivate && !hasGetAttribute && !isEntityBased) {
              isViolation = true; //if a violation has been found
              violations++;
              issueType = "Attribute has a custom getter which will always rederive its value and hence should not be marked to passivate.";
          }
          else if(transExpr.equals("present") && recalcCondn.equals("true") && wouldPassivate && !isEntityBased) {
              isViolation = false; //if a violation has been found
              //violations++;
              issueType = "Attribute has a Transient Expression which will always recalculate and hence should not be marked to passivate.";
          }
          else if (wouldPassivate && !updateable && !isEntityBased) {
          isViolation = false; //if a violation has been found
          //violations++;
          issueType = "This is a read-only non-entity based attribute and hence should not be passivated.";
        }
          if(passivationFinder.isExempt(passivationFinder.getFileName(f), name))
              isViolation=false;
        
        if(isViolation) {                    
                             
                 writer.write("File Name: " + f + "\nLine Number: " + lineNo + "\nAttribute Name: " + 
                         name+ "\nGetter Assessment: " + getterAssessment + "\nUpdatebale: " + 
                              String.valueOf(updateable) + "\nWouldPassivate: " + String.valueOf(wouldPassivate) +
                              "\nVOPassivateAll: " + String.valueOf(passivateAll) + "\nAttribute Marked to passivate:" + String.valueOf(specifiedAttrPassivation) +
                              "\nIsEntityBased: " + String.valueOf(isEntityBased) + 
                              "\nHasGetAttribute: " + String.valueOf(hasGetAttribute) + 
                              "\nTransientExpression: " + transExpr + "\nRecalcCondition: " + recalcCondn + "\nIssue: " + issueType + "\n\n");
        }
      }
        
        if(violations > 0)
            writer.write("\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/AMPassivation.\n");
    } catch (Exception e) {
        System.err.println("ERROR:  Error while processing file.  Please review manually: " + f);
        return;
    }
  }
  
  
  private String removeJavaComments(String str) {
      // Remove all line comments
      Matcher m1 = Pattern.compile("^\\s*?//.*$[\r\n]*", Pattern.MULTILINE).matcher(str);
      str = m1.replaceAll("");
      // Remove all other comments
      Matcher m2 = Pattern.compile("/\\*.*?\\*/\\s*?[\r\n]*", Pattern.DOTALL).matcher(str);
      str = m2.replaceAll("");
      
      return str;
  }
}
