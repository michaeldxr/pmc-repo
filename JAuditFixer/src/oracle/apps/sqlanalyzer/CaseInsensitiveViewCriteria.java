package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Pattern;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.helpers.XmlFixer;
import oracle.apps.lovWidth.ModifiedComboboxParser;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CaseInsensitiveViewCriteria {
    
  static BufferedWriter writer;
  static String sCrawlDir;
  static String m_family;
  
  //Store the viwattribute mappings for the current VO being processed
  private HashMap<String,Node> vaAliasAttributeMap = new HashMap<String,Node>();
  private HashMap<String,Node> vaNameAttributeMap = new HashMap<String,Node>();
  private HashMap<String,Node> extendedVaAliasAttributeMap = new HashMap<String,Node>();
  private HashMap<String,Node> extendedVaNameAttributeMap = new HashMap<String,Node>();
  
  //Store the mappings for userdefined SQLQuery
  HashMap<String,HashSet> colNameTableNames = new HashMap<String,HashSet>();
  HashMap<String,String> colAliasName = new HashMap<String,String>();
  
  HashMap<String,HashSet> extendedColNameTableNames = new HashMap<String,HashSet>();
  HashMap<String,String> extendedColAliasName = new HashMap<String,String>();
  
  static HashMap<String,HashSet> caseInsensitive_indexColumns; //UPPER
  static HashMap<String,HashSet> caseSensitive_indexColumns;  //Default
  
  HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();
  HashSet<String> exemptedTables = new HashSet<String>();
  
  Pattern pUpper = Pattern.compile("upper", Pattern.CASE_INSENSITIVE);
  static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");
  
  ModifiedComboboxParser parser;
  private Node baseViewObject;
  static boolean makeFix = false; //DO not support fix mode anymore as it might conflict with UI search case insensitivity scan
  static boolean refreshFromDB = false;
  static String viewRoot ="";
  static HashSet<String> tableColumns = new HashSet<String>();
  
  
  public static void main(String[] args) throws Exception
  {
     if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
          System.out.println("Usage:  reviewViewCriteria.sh");
          System.exit(1);
      }
    sCrawlDir = args[0].trim();
    
      File f = new File(sCrawlDir);
      if(!f.exists()) {
          System.out.println("Crawl directory does not exist - " + sCrawlDir);
          System.exit(1);
      }
      
      viewRoot = ViewCriteriaHelper.getViewRoot(sCrawlDir);
    System.out.println("Analyzing all files in directory: " + sCrawlDir);
     
   /* if(args.length > 1)
       makeFix = new Boolean(args[1].trim());
    
    boolean append = false;
    if(args.length > 2)
       append = new Boolean(args[2].trim());
    
    if(args.length > 3)
        refreshFromDB = new Boolean(args[3].trim()); */
    
    CaseInsensitiveViewCriteria civc = new CaseInsensitiveViewCriteria();
    
    try {         
        m_family = ViewCriteriaHelper.getFamily(sCrawlDir);
        writer = new BufferedWriter(new FileWriter("view_criteria_scan.csv"));
        
        writer.write("Family,Module,Product,Filename,Label,ViewAttribute, EntityAttribute," +
          "EntityUsage, Column Name, Table Name, Column Type, Index, VC Required, Is VC Case-insensitive?, VC Item Name, VC Row Name, Column Data Case\n");
        civc.generateExemptionList();
        
        if(refreshFromDB) {     
           System.out.println("Refreshing column indexes from the database...");
            Pattern pUpperFunctionIndex = Pattern.compile("UPPER\\(\"(\\w+)\"\\)");
           caseInsensitive_indexColumns = SqlIndexFinder.findFunctionIndexes(pUpperFunctionIndex);
           caseSensitive_indexColumns = SqlIndexFinder.findDefaultIndexes();
        }
        else{
          caseInsensitive_indexColumns = SqlIndexFinder.readIndexesFromFile("caseInsensitive");
          caseSensitive_indexColumns = SqlIndexFinder.readIndexesFromFile("caseSensitive");
        }
        
        civc.crawlDirectory(sCrawlDir);
        writer.close();
        
        HashMap<String,String> tableColumnsData = SqlIndexFinder.findDataAllCaps(tableColumns);
        writer = new BufferedWriter(new FileWriter("view_criteria_scan1.csv"));
        writer.write("Family,Module,Product,Filename,Label,ViewAttribute, EntityAttribute," +
                "EntityUsage, Column Name, Table Name, Column Type, Index, VC Required, Is VC Case-insensitive?, VC Item Name, VC Row Name, Column Data Case\n");
        
        BufferedReader reader = new BufferedReader(new FileReader("view_criteria_scan.csv"));
        
        String line = reader.readLine(); //header line
        
        while((line = reader.readLine()) != null) {
            
          String[] parts = line.split(",");    
          if(parts.length < 16)
              continue;
               
          String tableCol = parts[9].trim() + "," + parts[8].trim();
          String data = tableColumnsData.get(tableCol);
            
          String filePath = parts[3].trim();
          if(filePath.contains("/flex/") || filePath.contains("/publicFlex/"))
              continue;
          
          if(data != null && !data.equals("Not varchar")) {
              line += "," + data;        
                          
         if(parts[12].trim().equals("null") || parts[12].trim().equals("Optional"))
            continue;
              
         writer.write(line + "\n");
          }
        }
        writer.close();
        reader.close();
        
      /*  if(makeFix){
            System.out.println("Fixing cases where VC are case-insentitive, " +
                "column index is default (case-sensitive) and column data is All caps...");
            diffHelper.startDiffLog();     
            civc.makeFixes();
            diffHelper.closeDiffLog();    
            System.out.println("Done!");
        }*/
        
    } catch(Exception e){
      e.printStackTrace();
    }
  }
  
  private void crawlDirectory(String path) throws Exception
  {
      if(path.contains(".ade_path") || path.contains("/classes/"))
          return;
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
          else if(listOfFiles[i].isDirectory()) 
              crawlDirectory(listOfFiles[i].getAbsolutePath());          
          else 
              System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);          
      }      
  }
  
  public boolean fileOfInterest(String sName) 
  {
    if(!FamilyModuleHelper.isPathOfInterest(sName))
       return false;  
      
    if (sName.endsWith(".jpr"))
        return true;
    return false;
  }
  
  public void processFile(File fName) throws Exception{
      
    String m_jprAbsFilePath = fName.getAbsolutePath(); //name of the jpr
    JprHelper jprHelper = new JprHelper();
    
    parser = new ModifiedComboboxParser(m_family.toLowerCase(), ModifiedComboboxParser.ScanType.ALL);
    parser.setJprAbsFilePath(m_jprAbsFilePath);
    
    HashMap<String,String> listOfAllVOs = jprHelper.getListOfAllViewObjectsInJpr(m_jprAbsFilePath);
    
    Iterator<String> voFilePathIter = listOfAllVOs.values().iterator();
    
    while (voFilePathIter.hasNext()) 
        processVO(voFilePathIter.next());    
    
  }
  
  
  public void processVO(String f) throws Exception{    
  
    XMLDocument doc = XMLParserHelper.getXMLDocument(f);

    if(doc == null) {
      System.out.println("Could not parse XMLDocument..Skipping");
      return;
    }
    
      NodeList viewObjects = doc.getElementsByTagName("ViewObject");
      if(viewObjects == null || viewObjects.getLength() != 1) 
      {
        System.out.println("WARNING:  Unexpected number of view objects found");
        System.out.println("Skipping...");
        return;
      }
      NodeList vc_rows = doc.getElementsByTagName("ViewCriteriaRow");
      Node viewObject = viewObjects.item(0);
      
      String where = XMLParserHelper.getAttributeValue(viewObject, "Where");
      Node sqlQuery = XMLParserHelper.getChildNodeWithName(viewObject, "SQLQuery");
      
      if(vc_rows == null && where == null && sqlQuery == null)
          return; //VO has no view criteria
      
      vaAliasAttributeMap = new HashMap<String,Node>();
      vaNameAttributeMap  = new HashMap<String,Node>();
      
      extendedVaAliasAttributeMap = new HashMap<String,Node>();
      extendedVaNameAttributeMap = new HashMap<String,Node>();
      
      colNameTableNames = new HashMap<String,HashSet>();
      colAliasName = new HashMap<String,String>();
      
      extendedColNameTableNames = new HashMap<String,HashSet>();
      extendedColAliasName = new HashMap<String,String>();
         
      
      /*  if(where != null)
          processWhereClause(viewObject,where ,f, m_jprAbsFilePath);*/
      
      if(vc_rows != null) {             
          createVOAttributeMappings(doc);
          
          SQLParserHelper.parseSql(viewObject, colNameTableNames, colAliasName);    
          
          String baseVo = XMLParserHelper.getAttributeValue(viewObjects.item(0), "Extends");      
          if(baseVo != null) {
            
            XMLDocument baseVoDoc = parser.getVOXml(baseVo);      
            if(baseVoDoc == null) return;
            NodeList baseViewObjects = baseVoDoc.getElementsByTagName("ViewObject");
            if(baseViewObjects == null || baseViewObjects.getLength() != 1)
                return;
            baseViewObject = baseViewObjects.item(0);
            
            SQLParserHelper.parseSql(baseViewObject, extendedColNameTableNames, extendedColAliasName);   
         }
          
         processViewCriteria(viewObject, vc_rows, f);
      }      
      /* if(sqlQuery != null)
          processSQLQuery(sqlQuery,f);     */   
  }
  
  private void createVOAttributeMappings(XMLDocument doc) throws Exception{
    
    NodeList view_attributes = doc.getElementsByTagName("ViewAttribute");
    
    for(int i = 0; i< view_attributes.getLength(); i++) {
        
      Node va = view_attributes.item(i);
      String alias = XMLParserHelper.getAttributeValue(va, "AliasName");    
      if(alias == null) {
       //errors.write(fileName + "->" +  XMLParserHelper.getAttributeValue(va, "Name") + " -> No Alias\n");
       continue;
      }
      String entityUsage = XMLParserHelper.getAttributeValue(va,"EntityUsage");
        
      if(entityUsage != null)
          alias += "_" + entityUsage;
      vaAliasAttributeMap.put(alias.trim().toUpperCase(), va);        
        
      String name = XMLParserHelper.getAttributeValue(va, "Name");
      vaNameAttributeMap.put(name,va);
    } //for
    
    NodeList viewObjects = doc.getElementsByTagName("ViewObject");
    if(viewObjects == null || viewObjects.getLength() != 1)
        return;
    String baseVo = XMLParserHelper.getAttributeValue(viewObjects.item(0), "Extends");
    
    if(baseVo != null) {
        
      XMLDocument baseVoDoc = parser.getVOXml(baseVo);      
      if(baseVoDoc == null) return;
      NodeList baseViewObjects = baseVoDoc.getElementsByTagName("ViewObject");
      if(baseViewObjects == null || baseViewObjects.getLength() != 1)
          return;
      baseViewObject = baseViewObjects.item(0);
      
      view_attributes = baseVoDoc.getElementsByTagName("ViewAttribute");
      
      for(int i = 0; i< view_attributes.getLength(); i++){
        Node va = view_attributes.item(i);
        String alias = XMLParserHelper.getAttributeValue(va, "AliasName");    
        if(alias == null) {
         //errors.write(fileName + "->" +  XMLParserHelper.getAttributeValue(va, "Name") + " -> No Alias\n");
         continue;
        }
        String entityUsage = XMLParserHelper.getAttributeValue(va,"EntityUsage");
          
        if(entityUsage != null)
            alias += "_" + entityUsage;
        extendedVaAliasAttributeMap.put(alias.trim().toUpperCase(), va);        
          
        String name = XMLParserHelper.getAttributeValue(va, "Name");
        extendedVaNameAttributeMap.put(name,va);
      }      //for
    } //if baseVo != null
    
  }
 
  
  public void processViewCriteria(Node viewObject, NodeList vc_rows, String fileName) throws Exception {
    
    for(int i = 0; i< vc_rows.getLength(); i++){
        
      Node vc_row = vc_rows.item(i);
      String isUpper_row = XMLParserHelper.getAttributeValue(vc_row, "UpperColumns");
      String vcRowName = XMLParserHelper.getAttributeValue(vc_row, "Name");
      String vcRequired = "";
        String isUpper_item = "";
        
     // if(isUpper != null && isUpper.equals("0")) continue;
        
      NodeList vc_row_children = vc_row.getChildNodes();
        
      for(int j =0; j < vc_row_children.getLength(); j++) {
          
          vcRequired = "";
          isUpper_item = "";
          
          boolean useBaseViewObject = false;
          
          Node child = vc_row_children.item(j);
          String nodeName = child.getNodeName();
          
          if(!nodeName.equals("ViewCriteriaItem")) continue;
          
          vcRequired = XMLParserHelper.getAttributeValue(child, "Required");
          
          Node parentVC = XMLParserHelper.getParentNodeWithName(child, "ViewCriteria");
          String vcViewObjectName = null;
          if(parentVC != null)
             vcViewObjectName = XMLParserHelper.getAttributeValue(parentVC, "ViewObjectName");
          
          if(vcViewObjectName != null) {
             String[] parts= vcViewObjectName.split("\\.");
             vcViewObjectName = parts[parts.length - 1];
          }    
          if(vcViewObjectName != null && !vcViewObjectName.equals(XMLParserHelper.getAttributeValue(viewObject, "Name"))) {
              if(baseViewObject != null && vcViewObjectName.equals(XMLParserHelper.getAttributeValue(baseViewObject, "Name"))){
                 //its ok
              } else {
                System.out.println("VCViewObjectName different from current VO name: " + vcViewObjectName + " -> " + fileName);
                continue;
              }
          }
          
          isUpper_item = XMLParserHelper.getAttributeValue(child, "UpperColumns");
          //if(isUpper != null && isUpper.equals("0")) continue;
          
          String viewAttrName = XMLParserHelper.getAttributeValue(child, "ViewAttribute");
          String vc_name = XMLParserHelper.getAttributeValue(child, "Name");
       
          Node va = vaNameAttributeMap.get(viewAttrName);
          
          if(va == null) {
            va = extendedVaNameAttributeMap.get(viewAttrName);
            if(va != null)
              useBaseViewObject = true;
          }
          
          if(va == null) {            
            System.out.println(fileName + "," + viewAttrName + ", Could not find the ViewAttribute for ViewCriteriaItem");
            continue;
          }
          
          String col_name = "";
          String table_name = "";
          String colType = "";
          
          boolean entityFound = false;
          
          if(XMLParserHelper.getAttributeValue(va, "EntityUsage") != null) { //if it is an entity based attribute
          
            Node ea = null;
            if(useBaseViewObject)
              ea = parser.getEntityAttributeNode(baseViewObject, va, viewAttrName);
            else
              ea = parser.getEntityAttributeNode(viewObject, va, viewAttrName);
            
           /* if(ea == null) {              
              errors.write(fileName + ",, Could not find the corresponding EntityAttribute to ViewAttribute: " + viewAttrName + "\n");
              continue;            
            }*/
            if (ea != null) {
                
              entityFound = true;
              col_name = XMLParserHelper.getAttributeValue(ea, "ColumnName");
              table_name = XMLParserHelper.getAttributeValue(ea, "TableName");
              colType = XMLParserHelper.getAttributeValue(ea, "ColumnType");
            }          
          } 
          if(!entityFound) { //it is not an entity based attribute or the corresponding entity object could not be determined
              
             String aliasName = XMLParserHelper.getAttributeValue(va, "AliasName");
             colType = XMLParserHelper.getAttributeValue(va, "SQLType");
             
             if(useBaseViewObject)
                col_name = extendedColAliasName.get(aliasName);
             else
                col_name = colAliasName.get(aliasName);
             
             if(col_name == null) {
             //  errors.write("Could not get column name for attribute alias:" + aliasName + "\n");
               table_name = "Could not determine";
             }
             else{
                HashSet possibleTables = null;
                if(useBaseViewObject)
                   possibleTables = extendedColNameTableNames.get(aliasName);
                else
                   possibleTables = colNameTableNames.get(aliasName);
                 
                for(Iterator it = possibleTables.iterator(); it.hasNext();){
                  String tableName = (String)it.next();
                    table_name+=tableName + " ";
                }
             }
          }
          
          String index = getIndex(table_name,col_name);
          
          String isUpper = "true";
          if(isUpper_item != null && isUpper_item.equals("0"))
              isUpper = "false";
          if(isUpper_item == null && isUpper_row != null && isUpper_row.equals("0"))
              isUpper = "false";    
        
        if(colType != null && !colType.contains("VARCHAR"))
            continue;
//            if(vcRequired == null || vcRequired.equals("Optional"))
//                continue;
        if(isUpper.equals("true") && index.equalsIgnoreCase("upper"))
            continue;
        if(index.equalsIgnoreCase("both") || index.equalsIgnoreCase("none"))
            continue;
        if(isUpper.equals("false") && index.equalsIgnoreCase("default"))
            continue;
          
        if(isExempt(viewAttrName, ViewCriteriaHelper.getFileName(fileName)) || isExemptedTable(table_name))
            continue;
          
        writer.write(ViewCriteriaHelper.getFileNameInfo(fileName,"") + viewAttrName + "," +
               XMLParserHelper.getAttributeValue(va, "EntityAttrName") + "," + XMLParserHelper.getAttributeValue(va, "EntityUsage") +
               "," + col_name + "," + table_name + "," + colType + "," + index + "," + vcRequired + "," + isUpper + "," 
                     + vc_name + "," + vcRowName + "\n"); 
          
        tableColumns.add(table_name.trim() + "," + col_name.trim());
          
      } //for vc_row_children
    } //for vc_rows
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
        if(issue.equals("ViewCriteria")){
            
            String fileName = parts[0].trim();
            String attribute = parts[1].trim();
              
            HashSet<String> exemptLines = null;
            if(!exemptions.containsKey(fileName)) 
              exemptLines = new HashSet<String>();
            else 
              exemptLines = exemptions.get(fileName);
            
            exemptLines.add(attribute);
            exemptions.put(fileName, exemptLines);
        }
          if(issue.equals("ViewCriteriaCaseMismatch")) {
              String tableName = parts[0].trim();
              exemptedTables.add(tableName);
          }
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }
    
    private boolean isExempt(String attribute, String fileName){
        
        if(!exemptions.containsKey(fileName)) 
            return false;
        
        HashSet<String> lines = (HashSet<String>)exemptions.get(fileName);
        if(lines.contains(attribute))
            return true;
        
        return false;
    }
    
    private boolean isExemptedTable(String tableName) {
        if(exemptedTables.contains(tableName))
            return true;
        return false;
    }

    
    public void makeFixes() throws Exception {
        
        BufferedReader fileReader = new BufferedReader(new FileReader("view_criteria_scan1.csv"));
        String line = fileReader.readLine(); //first line is the header - read & ignore
        
        String prevFilePath = null;
        XMLDocument doc = null;
        XmlFixer xmlFixer = null;
        String fileContentsAsString = null;
        
        while((line = fileReader.readLine()) != null) {
            
            String[] parts = line.split(",");
            if(parts.length < 16)
                continue;
            String filePath = viewRoot + parts[3].trim();
            
            String vcIsUpper = parts[13].trim().toLowerCase();
            String colIndex = parts[11].trim().toLowerCase();
            String colData = "";
            if(parts.length > 16)
                colData = parts[16].trim().toLowerCase();
            
            if(!vcIsUpper.equals("true") || !colIndex.equals("default") || !colData.equals("all caps"))
                continue;
            
            String vcName = parts[14].trim();
            String vcRowName = parts[15].trim();
            
            if(!filePath.equals(prevFilePath)) {
              
                if(prevFilePath != null) {
                  fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
                  applyFix(prevFilePath,fileContentsAsString);
                }
              
              doc = XMLParserHelper.getXMLDocument(filePath);        
              xmlFixer = new XmlFixer(filePath);
            }
            prevFilePath = filePath;
            
            NodeList list = doc.getElementsByTagName("ViewCriteriaItem");
            Node vcItemToFix = null;
            
            for(int i =0; i < list.getLength(); i++){
                Node n = list.item(i);
                String name = XMLParserHelper.getAttributeValue(n, "Name");
                Node parent = XMLParserHelper.getParentNodeWithName(n, "ViewCriteriaRow");
                String vcrName = XMLParserHelper.getAttributeValue(parent, "Name");
                if(name.equals(vcName) && vcrName.equals(vcRowName)){
                    vcItemToFix = n;
                    break;
                }                
            }
            if(vcItemToFix != null) {
                String upperCol = XMLParserHelper.getAttributeValue(vcItemToFix, "UpperColumns");
                if(upperCol == null)
                    xmlFixer.addAttribute(vcItemToFix, "UpperColumns", "0");
                else if(upperCol.equals("1"))
                    xmlFixer.modifyAttribute(vcItemToFix, "UpperColumns", "1", "0", true);  
                else
                    System.out.println("Nothing to fix");
            }
        }
        
        if(xmlFixer != null) {
          fileContentsAsString = xmlFixer.getFileContentsAsString(false);        
          applyFix(prevFilePath,fileContentsAsString);
        }
    }
    
    private void applyFix(String absPath,
                        String newFileContents) throws Exception {
       File file = new File(absPath);
       if (!file.exists()) {
           throw new Exception("while making fix, file not found: " +
                               absPath);
       }
       diffHelper.checkoutAndDelete(file);

       FileWriter fw = new FileWriter(file.getAbsolutePath());
       BufferedWriter bw = new BufferedWriter(fw);
       bw.write(newFileContents);
       if (bw != null)
           bw.close();
       if (fw != null)
           fw.close();
       diffHelper.checkinAndDiff(file);
    }
    
    
    public static String getIndex(String tableName, String colName) {
       String index = "None";
       if(tableName == null) return index;
       String[] tableNames = tableName.trim().split(" ");
       
       for(int i = 0; i < tableNames.length; i++) {
       
           HashSet caseInsensitiveIndexes = caseInsensitive_indexColumns.get(tableNames[i]);
           if(caseInsensitiveIndexes != null) {
               for(Iterator it = caseInsensitiveIndexes.iterator(); it.hasNext();) {
                 String indexColumn = (String)it.next();
                   if(indexColumn.equals(colName)) {
                       index = "Upper";
                       break;
                   }
               }
           }
           
          HashSet caseSensitiveIndexes = caseSensitive_indexColumns.get(tableNames[i]);
          if(caseSensitiveIndexes != null) {
              for(Iterator it = caseSensitiveIndexes.iterator(); it.hasNext();) {
                String indexColumn = (String)it.next();
                  if(indexColumn.equals(colName)) {
                      if(index.equals("None"))
                       index = "Default";
                      else
                       index = "Both";
                      break;
                  }
              }
          }
        
         if(!index.equals("None"))
               return index;
      } //for
       return index;
    }
}
