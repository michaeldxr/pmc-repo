package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.lovWidth.ModifiedComboboxParser;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CaseInsensitiveViewCriteriaLRG extends JoesBaseClass {
    
    ModifiedComboboxParser parser;
    private Node baseViewObject;
  
    //Store the viwattribute mappings for the current VO being processed
    private HashMap<String,Node> vaAliasAttributeMap = new HashMap<String,Node>();
    private HashMap<String,Node> vaNameAttributeMap = new HashMap<String,Node>();
    private HashMap<String,Node> extendedVaAliasAttributeMap = new HashMap<String,Node>();
    private HashMap<String,Node> extendedVaNameAttributeMap = new HashMap<String,Node>();
  
    //Store the mappings for userdefined SQLQuery
    private HashMap<String,HashSet> colNameTableNames = new HashMap<String,HashSet>();
    private HashMap<String,String> colAliasName = new HashMap<String,String>();
  
    private HashMap<String,HashSet> extendedColNameTableNames = new HashMap<String,HashSet>();
    private HashMap<String,String> extendedColAliasName = new HashMap<String,String>();
  
    private static BufferedWriter writer;
    private static String m_family;
    
    private static HashMap<String,HashSet> caseInsensitive_indexColumns; //UPPER
    private static HashMap<String,HashSet> caseSensitive_indexColumns;  //Default
    private static HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();
    private static HashSet<String> exemptedTables = new HashSet<String>();
    
    private static HashSet<String> tableColumns = new HashSet<String>();
    private static final String issuetype ="ViewCriteriaCaseMismatch";
    private static int releaseNum=0;
    private static String series="";
    
  public CaseInsensitiveViewCriteriaLRG() 
  {
      super(JoesBaseClass.CRAWL_TYPE.JPR);
  }
  
  public static void main(String[] args) throws Exception {
    
    
        if (args.length != 1 || args[0] == null || args[0].equals("")) {
            System.out.println("Series has to be specified for " + issuetype);
            System.exit(1);
        }
        series = args[0];
       //series = "FSM_MAIN_GENERIC";
        
        String release= FamilyModuleHelper.getRelease(series);
        try{              
            releaseNum = Integer.parseInt(release);
        }catch(NumberFormatException e){
           e.printStackTrace();
           releaseNum=0;
        }

        String label = LRGUtil.getLatestLabel(series);
        System.out.println("Running "+issuetype+" Scan on series: "+series+".......");
        System.out.println("Latest label is: "+label);
      
        writer = new BufferedWriter(new FileWriter("ViewCriteriaCaseMismatch_LRG_temp.csv"));
        writer.write("Family,Module,Product,Filename,Label,ViewAttribute, EntityAttribute," +
                   "EntityUsage, Column Name, Table Name, Column Type, Index, VC Required, Is VC Case-insensitive?, VC Item Name, VC Row Name, Column Data Case\n");
      
        generateExemptionList();
        caseInsensitive_indexColumns = SqlIndexFinder.readIndexesFromFile("caseInsensitive");
        caseSensitive_indexColumns = SqlIndexFinder.readIndexesFromFile("caseSensitive");
      
        ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
        CaseInsensitiveViewCriteriaLRG civc = new CaseInsensitiveViewCriteriaLRG();
        
      String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
      String viewRoot = serverTop + "/" + series + ".rdd/" + label + "/";
        
        for(int j=0; j<families.size();j++){
          
            String family = families.get(j);
            m_family = family.toUpperCase();            
            civc.parser = new ModifiedComboboxParser(family.toLowerCase(), ModifiedComboboxParser.ScanType.ALL);
            String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, issuetype);
            System.out.println("Crawling dir '" + viewRoot + sCrawlDir +
                             "' for family: " + family);
            civc.crawlDirectoryNew(viewRoot, sCrawlDir, false);
        }
        writer.close();

        //Step 2: Read from view_criteria_scan and filter to view_criteria_scan1
        HashMap<String, String> tableColumnsData = SqlIndexFinder.findDataAllCaps(tableColumns);
        writer = new BufferedWriter(new FileWriter("ViewCriteriaCaseMismatch_LRG.csv"));
        writer.write("Family,Module,Product,Filename,Label,ViewAttribute, EntityAttribute, EntityUsage, Column Name, Table Name, Column Type, Index, VC Required, Is VC Case-insensitive?, VC Item Name, VC Row Name, Column Data Case\n");
        BufferedReader reader = new BufferedReader(new FileReader("ViewCriteriaCaseMismatch_LRG_temp.csv"));

        String line = reader.readLine(); //header line
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length < 16)
                continue;

            String tableCol = parts[9].trim() + "," + parts[8].trim();
            String data = tableColumnsData.get(tableCol);

            String filePath = parts[3].trim();
            if (filePath.contains("/flex/") ||
                filePath.contains("/publicFlex/"))
                continue;

            if (data != null && !data.equals("Not varchar")) {
                line += "," + data;
                if (parts[12].trim().equals("null") ||
                    parts[12].trim().equals("Optional"))
                    continue;
                writer.write(line + "\n");
            }
        }
        writer.close();
        reader.close();
        
        LRGUtil.addLabelInfoToAB(issuetype, series, label);
    }
  
  
  protected void processFile(File fName, boolean doAde){
      
    try{
    String m_jprAbsFilePath = fName.getAbsolutePath(); //name of the jpr
    JprHelper jprHelper = new JprHelper();
    
    parser.setJprAbsFilePath(m_jprAbsFilePath);
    
    HashMap<String,String> listOfAllVOs = jprHelper.getListOfAllViewObjectsInJpr(m_jprAbsFilePath);
    
    Iterator<String> voFilePathIter = listOfAllVOs.values().iterator();
    
    while (voFilePathIter.hasNext()) 
        processVO(voFilePathIter.next());    
    } catch(Exception e){
      e.printStackTrace();
    }
    
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

        if(isUpper.equals("true") && index.equalsIgnoreCase("upper"))
            continue;
        if(index.equalsIgnoreCase("both") || index.equalsIgnoreCase("none"))
            continue;
        if(isUpper.equals("false") && index.equalsIgnoreCase("default"))
            continue;
          
        if(isExempt(viewAttrName, ViewCriteriaHelper.getFileName(fileName)) || isExemptedTable(table_name.trim()))
            continue;
        if(releaseNum <=8 && table_name.equals("HZ_LOCATIONS") && col_name.equals("POSTAL_CODE"))
            continue;
        if(series.contains("R9ERP") && fileName.contains("LegalLocationVO.xml") && viewAttrName.equals("PostalCode"))
            continue;
        
        if(releaseNum <= 12 && fileName.endsWith("ConversionTypeCodeVVO.xml") && viewAttrName.equals("ConversionType"))
          continue;
          
        writer.write(ViewCriteriaHelper.getFileNameInfo(fileName,"") + viewAttrName + "," +
               XMLParserHelper.getAttributeValue(va, "EntityAttrName") + "," + XMLParserHelper.getAttributeValue(va, "EntityUsage") +
               "," + col_name + "," + table_name + "," + colType + "," + index + "," + vcRequired + "," + isUpper + "," 
                     + vc_name + "," + vcRowName + "\n"); 
          
        tableColumns.add(table_name.trim() + "," + col_name.trim());
          
      } //for vc_row_children
    } //for vc_rows
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

  protected String getSummaryReport() {
    return null;
  }

  protected String getSummaryReportSubject() {
    return null;
  }
}
