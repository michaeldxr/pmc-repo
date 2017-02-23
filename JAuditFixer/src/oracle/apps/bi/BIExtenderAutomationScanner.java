package oracle.apps.bi; 

/* Scanner for BI Extender Automation Compliance
 * 4-13-2012  Seth Stafford (sstaffor) 
 * 4-26-2012  Completed first running, testable version.
 * 4-27-2012  Clean up for first merge to FATOOLS_MAIN_GENERIC
 * 4-30-2012  Correctly incrementing FF counter.
 * 4-30-2012  Added code for finding isNull=true attribute on BIEnabled flag
 * 4-30-2012  Fixed error in detecting BIFlattenedFactName
 * 4-30-2012  Added isRelevantDirectory() for dramatic speed-up
 * 5-01-2012  Merging in Sudipti Gupta's changes for AngryBirds integration.
 * 5-08-2012  Added loop over BIFlattenedFactNames on a DFF -- each Usage has 
 *            a BIFlattenedFactName, but only one should be non null.
 * 6-12-2012  Avoid Stack Overflow in XMLTreeWalker.next_Node when last 
 *            BIFlattenedFactName in the file comes before LOTs more XML.  
 *            Only able to avoid it if there is a non-null BIFlattenedFactName
 *            defined for the last DFF in the file.
 *            Unclear what causes next_Node to run away in those cases.
 * 6-13-2012  Maybe the Stack Overflow problems happen because the BINodeFilter
 *            used with XMLTreeWalker has been rejecting null nodes.  
 *            Accepting null nodes in the filter fixed the one clear failure case.
 * 6-13-2012  Also adjusting the System output to make it friendly for spreadsheets
 * 6-14-2012  Adding support for exemptions on per-DFFCode basis.
 * 6-20-2012  Merging in 17 exemptions required for SCM. [per Srikanth.Karimisetty]
 * 6-22-2012  Removing 1 exemption which SCM no longer needs. [CST_ELEMENT_ANALYSIS_GROUPS]
 * 6-29-2012  Adding 5 exemptions for PRJ.  They added Fact Names w/o consulting OTBI.
 * 
 * There are two rules to enforce:
 * 
 *    1. WARNING: Seeded FlexField is not BI Enabled. (Not implemented yet)
 *    2. ERROR: BI Enabled FlexField has null BIFlattenedFactName for all Usages.
 *    
 *
 * The XML structure is like this:
 * <DescriptiveFlexfield>
 *   <FlexfieldUsage
 *    <TableUsage
 *      <EntityUsage
 *        <BIFlattenedFactName>
 * 
 *  Each Usage will have a BIFlattenedFactName tag, but at most one of them should
 *  be non-null.
 * 
 * */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import java.util.Date;
import java.util.HashSet;
import java.util.StringTokenizer;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.Mail;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import oracle.xml.parser.v2.XMLNode;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

public class BIExtenderAutomationScanner {
    
  public static Writer outputFileWriter;
      
  static int flexField_definitions = 0;
  static int FFseed_files = 0;
  static int java_files = 0;
  static int stCrawlDepth = 0;
  static String exceptions ="";
  static String sCurrentFileName = "";
  static String sCurrentFilePath = "";
  
  static boolean bDebug = false;
  HashSet<String> exemptions = new HashSet<String>(); 
  HashSet<String> actionIds = new HashSet<String>();
  static String label = " ";
  
  public static void main(String[] args) throws Exception{             
   
      if(args == null || args.length < 1){
        System.out.println("Crawl directory has to be specified");
        System.exit(1);
      }
      String sCrawlDir = args[0];
      if (sCrawlDir == null || sCrawlDir.trim().equals("")) {
          System.out.println("Crawl directory has to be specified");
          System.exit(1);
      }
      
      boolean append = false;
      if(args.length > 1)
         append = new Boolean(args[1].trim());
            
      System.out.println("Analyzing all files in directory:" + sCrawlDir);

    BIExtenderAutomationScanner scanner = 
        new BIExtenderAutomationScanner();

    scanner.initializeExemptions(); // does nothing for now

    try {         
        outputFileWriter = new BufferedWriter(new FileWriter("BIExtenderAutomation_scan.csv", append) );
        if(!append)
        outputFileWriter.write("Family, Module, Product, Filename, Label, LineNum, NodeName, Id, Description, ScanType\n");           

        label = FamilyModuleHelper.getLabel(sCrawlDir);
        scanner.crawlDirectory(sCrawlDir);       
        
        System.out.println("Number of Seed Data files (*SD.xml) processed: "+ FFseed_files);
        System.out.println("Number of FlexField definitions processed: "+ flexField_definitions);  
        if(!exceptions.equals("")) {
          Date runDate = new Date();
          String summary = "Crawl Directory: " + sCrawlDir + "\n";
          summary += "\n Following exceptions were recorded during the scan: \n" + exceptions; 
          //Mail.sendMail("sudipti.gupta@oracle.com", summary, "SCRIPT: BIExtenderAutomation Scan run on " + runDate.toString());
        }
      
    } catch (Exception e) {
        e.printStackTrace();     
        Mail.sendMail("sudipti.gupta@oracle.com", e.getMessage(), "SCRIPT: BIExtenderAutomation Scan on " + sCrawlDir + " failed");
    }
    finally{
      outputFileWriter.close();
    }
  }  
  
  public void initializeExemptions() 
  {
     // let's try out one exemption
     //exemptions.add("PO_ATTRIBUTE_VALUES"); // PO doesn't want this exemption
     
     /*
      *  SCM Exemptions:
      *
      */     
     exemptions.add("INV_UNITS_OF_MEASURE");        /*  /scm/inv/db/data/InvUom/ */      
     exemptions.add("INV_CYCLE_COUNT_ENTRIES");     /*  /scm/inv/db/data/InvCounting/ */      
     exemptions.add("INV_CYCLE_COUNT_HEADERS");     /*  /scm/inv/db/data/InvCounting/ */      
     exemptions.add("INV_RESERVATIONS");            /*  /scm/inv/db/data/InvMaterialAvailability/ */      
     exemptions.add("CST_COST_ELEMENT_GRPS_B");     /*  /scm/cst/db/data/CstOverheads/ */      
     /*  5 */
     exemptions.add("CST_ELEMENT_GROUP_DETAILS");   /*  /scm/cst/db/data/CstOverheads/ */      
     exemptions.add("CST_EXPENSE_POOLS_B");         /*  /scm/cst/db/data/CstOverheads/ */      
     exemptions.add("CST_TRANSACTION_GROUPS");      /*  /scm/cst/db/data/CstOverheads/ */      
     exemptions.add("CST_COMPONENT_GROUPS_B");      /*  /scm/cst/db/data/CstCoreSetup/ */      
     exemptions.add("CST_COST_INV_ORGS");           /*  /scm/cst/db/data/CstCoreSetup/ */      
     /* 10 */
     exemptions.add("CST_COST_PROFILES_B");         /*  /scm/cst/db/data/CstCoreSetup/ */      
     exemptions.add("CST_DEFAULT_COST_PROFILES");   /*  /scm/cst/db/data/CstCoreSetup/ */      
     exemptions.add("CST_ITEM_COST_PROFILES");      /*  /scm/cst/db/data/CstCoreSetup/ */      
     exemptions.add("CST_PERIOD_VALIDATIONS");      /*  /scm/cst/db/data/CstCoreSetup/ */      
     exemptions.add("CST_TRANSACTION_OVERHEADS");   /*  /scm/cst/db/data/CstCoreSetup/ */      
     /* 15 */
     exemptions.add("CST_VAL_UNIT_DETAILS");        /*  /scm/cst/db/data/CstCoreSetup/ */      
     // Removed 06-22-2012
     //exemptions.add("CST_ELEMENT_ANALYSIS_GROUPS"); /*  /scm/cst/db/data/CstCoreSetup/ */      


     /*
      *  PRJ Exemptions:
      *
      */
     exemptions.add("PJB_BILLING_EVENTS_FLEX");          /* 14256809, /prj/pjb/db/data/PjbTransactions/ */      
     exemptions.add("PJF_CLASS_CATEGORIES_DESC_FLEX");   /* 14256792, /prj/pjf/db/data/PjfAllSetup/ */      
     exemptions.add("PJF_CLASS_CODES_DESC_FLEX");        /* 14256792, /prj/pjf/db/data/PjfAllSetup/ */      
     exemptions.add("PJF_EVENT_TYPES_DESC_FLEX");        /* 14256801, /prj/pjf/db/data/PjfAllSetup/ */      
     exemptions.add("PJF_PROJECT_CLASS_CODE_DESC_FLEX"); /* 14256792, /prj/pjf/db/data/PjfProjectDefinition/ */      
     /*  5 */

  	

  }
  
  public void processFile(File fName)
  {
    try {           
      if(isFlexFieldSeedData(fName.getName()))
          processFFSeedFile(fName);
    } 
    catch (Exception e) {
        e.printStackTrace();        
        System.out.println("ERROR:  Error while processing file: " +fName);
        exceptions += "\n Exception in file: " + fName + ": " + e.getMessage();
        return;
    }
  }

    /*
     * Given a DescriptiveFlexField Node, pull out the named Node
     * (this assumes the node is going to be unique)
     */
    private Node getNamedNode(Node pN, String pName) throws Exception 
    {
        Node rN = null;
        if (pN.equals(null)) return rN;
        
        NodeList sNodes = pN.getChildNodes();
        int i = 0; Boolean keepGoing = true; String sNodeName =  null;
                                           
        rN = sNodes.item(0);
        while (keepGoing && i < sNodes.getLength()) {
            rN = sNodes.item(i);
            sNodeName = rN.getLocalName();
            if (sNodeName != null) { 
               if (sNodeName.equals(pName)) {
                   keepGoing = false; 
               }
            }
            i=i+1; 
        }      
        return rN;
    }

  private void processFFSeedFile(File f) throws Exception 
  {  
    FFseed_files++;
    
    XMLDocument doc = XMLParserHelper.getXMLDocument(f.getAbsolutePath());  
    BINodeFilter kFilter = new BINodeFilter();
    
    if(doc == null) {
      System.out.println("Could not parse FF seed file: " + f.getName());
      return;
    }

      //System.out.print("\n");
      //System.out.println("----"+stCrawlDepth+ "---------------------------------");
      //System.out.println("Starting file: "+f.getName()+",  "+f.getAbsolutePath());
      // sCurrentFileName = f.getName();
      // sCurrentFilePath = f.getAbsolutePath();
    
    NodeList   nodes = doc.getElementsByTagName("DescriptiveFlexfield");
    TreeWalker walker = doc.createTreeWalker(doc,NodeFilter.SHOW_ELEMENT,kFilter,false);
    
    for(int i = 0; i < nodes.getLength(); i++) 
    {    
      Boolean isEnabled = false;
      Boolean hasFactNameDefined = false;
      Node    n = nodes.item(i);

      TreeWalker iWalker = doc.createTreeWalker(n,NodeFilter.SHOW_ELEMENT,kFilter,false);
        
      // find the DescriptiveFlexFieldCode element
      Node dffcNode = getNamedNode(n,"DescriptiveFlexfieldCode");
      Node bienNode = getNamedNode(n,"BIEnabledFlag");
      String sDFFCode = dffcNode==null ? null : dffcNode.getFirstChild().getNodeValue(); 
      String sDFFFactName = null;
      
      //System.out.print("    DFFCode:"+sDFFCode);
      System.out.print(FamilyModuleHelper.getSeedFileNameInfo(f.getCanonicalPath())+","+sDFFCode+",");
         
      // work out if this DFF is BIEnabled or not
      if (bienNode != null) {
          if (bienNode.hasChildNodes()) {
            // has an explicit value defined  
            NodeList bienChildren = bienNode.getChildNodes();
            if (bienChildren.item(0).getNodeValue().equals("Y"))
            { 
              isEnabled = true;
            }
          }
          /*
          else {  //probably has an isNull='true' attribute
              NamedNodeMap nnMap = bienNode.getAttributes();
              Node nIsNullAttr   = nnMap.getNamedItem("isNull");
              String sIsNullAttrValue = nIsNullAttr.getNodeValue(); 
          }
          */
      }
        
      if (isEnabled) 
      {
          // now check to see if BIFlattenedFactName is defined
          Node icNode = iWalker.getCurrentNode(); 
          if (icNode==null) {System.out.println("icNode:"+icNode);}
          else {
            Node iNode = iWalker.nextNode();
	    while (iNode!=null) /* There can be several fact name tags*/
            {
              String lName      =      iNode==null ? null : iNode.getLocalName();
              Node   firstChild =      iNode==null ? null : iNode.getFirstChild();
              String sValue     = firstChild==null ? null : firstChild.getNodeValue();

              if (lName!=null && lName.equals("BIFlattenedFactName"))
              { 
		  // found (another) one, update the flag carefully
                  if (sValue!=null) 
		  {     
		    if (!hasFactNameDefined) {
                      // this DFF checks out ok
                  	hasFactNameDefined = true; 
                  	sDFFFactName = sValue; 			  
		    } 
		    else {
			// oops!  found too many FactNames defined
                        // leave sValue as it was and tip-toe away until 
  			// we know how to handle this
                        System.out.println("Too many fact names on this DFF!");
		    }
                  } // if (sValue==null) we just keep looking
              }
	      /*
	      ** Workaround: XMLTreeWalker.next_Node hits a stack overflow on large
	      ** files if you wind up looking for another BIFlattenedFactName node
	      ** when a) there aren't any more, and b) there's a LOT more XML to parse.
	      ** So let's not stick around to fall off the edge of this cliff if we 
	      ** already know we have a good BIFlattenedFactName for the last DFF in 
  	      ** the FlexfieldSD.xml file.  We still are vulnerable to the case where
 	      ** the last DFF has only null BIFlattenedFactNames.  	
	      */
 	      if (!hasFactNameDefined) 
  	      { iNode = iWalker.nextNode(); } 
	      else iNode = null; //signal to exit while loop
	    }
          }
       }
      else {
          //System.out.println("DFF: "+sDFFCode + " is not BIEnabled.");
      }  
                
      if (isEnabled && !hasFactNameDefined && !isExempt(sDFFCode)) 
      {
          outputFileWriter.write(FamilyModuleHelper.getSeedFileNameInfo(f.getCanonicalPath()) + 
                                 ((XMLNode)n).getLineNumber() + "," 
                                + n.getNodeName() + "," + sDFFCode + 
        ",DFF is BIEnabled but BIFlattenedFactName is not defined,BIExtenderAutomation\n");
      }  
        
        flexField_definitions++;
        //System.out.println("\t\t BIFlattenedFactName:"+sDFFFactName);
        System.out.println((isEnabled?"Y":"N")+","+sDFFFactName+","
				+(isExempt(sDFFCode)?"Y":"N"));
    }   
  }

  private boolean isExempt(String pDFFCode) 
  {
    return (pDFFCode!=null)?exemptions.contains(pDFFCode):false;
  }
   
  private Boolean isRelevantDirectory(String pAbsolutePath)
  {
      // let's try to prune the search tree a little ...
      Boolean bRelevant = true; 
      
      if (pAbsolutePath.contains("/.ade_path/"))
          bRelevant = false;      
      else if (pAbsolutePath.contains("fusionapps/dist")) {
          // dump /dist/ directories quickly
          bRelevant = false;
      } else  if (stCrawlDepth>2) { 
          // tolerate shallow searching
          if ( stCrawlDepth>2 && pAbsolutePath.contains("/db/data")
            || stCrawlDepth>1 && pAbsolutePath.contains("/db")) {
            //System.out.println("stCrawlDepth:"+stCrawlDepth);                                        
            //System.out.println("pAbsolutePath:"+pAbsolutePath);
          }
          else bRelevant = false;
      }
      
      return bRelevant;
  }
    
  public void crawlDirectory(String path) 
  {
      stCrawlDepth++; //System.out.print("+");
      File folder = new File(path);
      File[] listOfFiles = folder.listFiles();
          
      if(listOfFiles == null)
          return;
      
      for(int i = 0; i <listOfFiles.length; i++)
      {
          if(listOfFiles[i].isFile()) 
          {
              String sName = listOfFiles[i].getAbsolutePath();              
              if(fileOfInterest(sName))               
                  processFile(listOfFiles[i]);                           
          }
          else if(listOfFiles[i].isDirectory()) {
              //System.out.println("stCrawlDepth:"+stCrawlDepth);
              if (isRelevantDirectory(listOfFiles[i].getAbsolutePath())) {
                crawlDirectory(listOfFiles[i].getAbsolutePath()); 
              }
          }
          else 
              System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);          
      }      
      stCrawlDepth--; //System.out.print("-");
      //if (stCrawlDepth==0) { System.out.print("\n"); }
  }


  public boolean fileOfInterest(String sName) 
  {
      
    String absPathLowerCase = sName.toLowerCase();
    if (absPathLowerCase.contains("/dbschema/") || absPathLowerCase.contains("/.ade_path/")
        || absPathLowerCase.contains("test.") || absPathLowerCase.contains("/test/") || absPathLowerCase.contains("/classes/") 
        || absPathLowerCase.contains("modeltest") || absPathLowerCase.contains("/uitest") || absPathLowerCase.contains("/noship/")
        || absPathLowerCase.contains("/publicuitest")  
        || absPathLowerCase.contains("servicetest") || absPathLowerCase.contains("/testui")
        || absPathLowerCase.contains("/structuretest"))
        return false;
        
    if (isFlexFieldSeedData(sName))
      return true; 
    
    return false;
  }
  
  
  private boolean isFFSecondaryUsage(String sName) {
    // at present, secondary usages are not relevant to the BI Extender Automation project
    if (sName.endsWith("SecondaryUsageSD.xml"))
      return true;
    return false;
  }
  
  private boolean isFlexFieldSeedData(String sName) {
    if (sName.endsWith("FlexfieldSD.xml"))
        return true;
    return false;
  }   
  
  private boolean isEmpty(String str) {
      if (str == null || str.trim().equals("")) 
          return true;      
      return false;
  }  

  
  


  private class BINodeFilter implements NodeFilter 
  {

     public short acceptNode(Node pN) 
     {    
        // maybe we get stack overflow in XMLTreeWalker.next_Node because
	// it starts returning null nodes to this method and they are rejected?
	//if (pN == null) { return NodeFilter.FILTER_REJECT; }
      	if (pN == null) { return NodeFilter.FILTER_ACCEPT; }

        String sLocalName = pN.getLocalName();
        if (sLocalName!=null) 
        { 
            if (sLocalName.equals("BIFlattenedFactName")) {
                return NodeFilter.FILTER_ACCEPT;
            }             
        }
        return NodeFilter.FILTER_REJECT;
     }
  } // class BINodeFilter


}
