package oracle.apps.bi; 

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.HashSet;

import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLNode;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

public class BIExtenderAutomationScannerLRG {
    
  private static Writer writer;
      
  static int flexField_definitions = 0;
  static int FFseed_files = 0;
  static int java_files = 0;
  static int stCrawlDepth = 0;
  static String exceptions ="";
  static String sCurrentFileName = "";
  static String sCurrentFilePath = "";
  static String fullLabel="";
  static boolean bDebug = false;
  HashSet<String> actionIds = new HashSet<String>();
  
  private static HashSet<String> exemptions = new HashSet<String>(); 
  private static final String issuetype = "BIExtenderAutomation";
  
  public static void main(String[] args) throws Exception{   
      try {
          if (args.length != 1 || args[0] == null || args[0].equals("")) {
              System.out.println("Series has to be specified for " + issuetype);
              System.exit(1);
          }

          String series = args[0];

          String label = LRGUtil.getLatestLabel(series);
          fullLabel = series+"_"+label;
          System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
          System.out.println("Latest label is: " + label);

          writer = new BufferedWriter(new FileWriter("BIExtenderAutomation_LRG.csv") );
          writer.write("Family, Module, Product, Filename, Label, LineNum, NodeName, Id, Description, ScanType\n");    

          initializeExemptions(); 

          ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
          for (int j = 0; j < families.size(); j++) {
              String family = families.get(j);
              String sCrawlDir = LRGUtil.getCrawlDir(series, family, label, issuetype);
              System.out.println("Crawling dir '" + sCrawlDir + "' for family: " + family);
              
              BIExtenderAutomationScannerLRG scanner = new BIExtenderAutomationScannerLRG();
              scanner.crawlDirectory(sCrawlDir);
              
              System.out.println("Done crawling for family "+family+".");
          }
          writer.close();
          
          LRGUtil.addLabelInfoToAB(issuetype, series, label);
      } catch (Exception e) {
          e.printStackTrace();
      }
  }  
  
  private  static void initializeExemptions() 
  {
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
    
     exemptions.add("PJB_BILLING_EVENTS_FLEX");          /* 14256809, /prj/pjb/db/data/PjbTransactions/ */      
     exemptions.add("PJF_CLASS_CATEGORIES_DESC_FLEX");   /* 14256792, /prj/pjf/db/data/PjfAllSetup/ */      
     exemptions.add("PJF_CLASS_CODES_DESC_FLEX");        /* 14256792, /prj/pjf/db/data/PjfAllSetup/ */      
     exemptions.add("PJF_EVENT_TYPES_DESC_FLEX");        /* 14256801, /prj/pjf/db/data/PjfAllSetup/ */      
     exemptions.add("PJF_PROJECT_CLASS_CODE_DESC_FLEX"); /* 14256792, /prj/pjf/db/data/PjfProjectDefinition/ */
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

    NodeList  nodes = doc.getElementsByTagName("DescriptiveFlexfield");
    
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
      System.out.print(getFileNameInfo(f.getCanonicalPath())+","+sDFFCode+",");
         
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
          writer.write(getFileNameInfo(f.getCanonicalPath()) + 
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

    private String getFileNameInfo(String absPath) {
        
      int i = 0;
      String family = "";
        String product = "";
      
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
        
        if (absPath.contains("emcore/")) {
            i = absPath.indexOf("emcore/");
            family = "EMGC";
            product="EMCORE";
        }
        
      String pathAfterFusionApps = absPath.substring(i);
      String[] parts = pathAfterFusionApps.split("/");
      
      
      if(parts == null) return blankString;
      
      if(absPath.contains("fusionapps/")){
          if(parts.length > 2)
            product = parts[2].trim().toUpperCase();
          if(parts.length > 1)
            family = parts[1].trim().toUpperCase();
      }
      if(absPath.contains("fsm/") && parts.length > 1)
          product = parts[1].trim().toUpperCase();
      
        if(absPath.contains("atgpf/") && parts.length > 1)
            product = parts[1].trim().toUpperCase();
      
      if(i<1)
         return "," + family + ",DB," + product + "," + pathAfterFusionApps + "," + fullLabel + ",";
      
      String path_before_fusionapps = absPath.substring(0, i-1);
      parts = path_before_fusionapps.split("/");
      
      if(parts==null || parts.length<2)
        return blankString;
      
      return family + ",DB," + product + "," + pathAfterFusionApps + "," + fullLabel + ",";
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
