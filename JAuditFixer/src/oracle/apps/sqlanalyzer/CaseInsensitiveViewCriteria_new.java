package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.io.IOException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.PreparedStatement;
import java.sql.Statement;

import java.sql.Types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.apps.helpers.XmlFixer;
import oracle.apps.lovWidth.ModifiedComboboxParser;

import oracle.apps.utility.JoesBaseClass;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class CaseInsensitiveViewCriteria_new extends JoesBaseClass {

  static BufferedWriter writer;
  static String sCrawlDir;
  static String m_family;

  static boolean isView = false;
  static String indexedTableName = " ";
  static String resolvedTableColumn = " ";
  static Pattern pTablesColumn = Pattern.compile("(.*?)\\((.*?)\\)");

  //Store the viwattribute mappings for the current VO being processed
  private HashMap<String, Node> vaAliasAttributeMap =
    new HashMap<String, Node>();
  private HashMap<String, Node> vaNameAttributeMap =
    new HashMap<String, Node>();
  private HashMap<String, Node> extendedVaAliasAttributeMap =
    new HashMap<String, Node>();
  private HashMap<String, Node> extendedVaNameAttributeMap =
    new HashMap<String, Node>();

  //Store the mappings for userdefined SQLQuery
  HashMap<String, HashSet> colNameTableNames = new HashMap<String, HashSet>();
  HashMap<String, String> colAliasName = new HashMap<String, String>();

  HashMap<String, HashSet> extendedColNameTableNames =
    new HashMap<String, HashSet>();
  HashMap<String, String> extendedColAliasName = new HashMap<String, String>();

  static HashMap<String, HashSet> caseInsensitive_indexColumns; //UPPER
  static HashMap<String, HashSet> caseSensitive_indexColumns; //Default

  HashMap<String, HashSet> exemptions = new HashMap<String, HashSet>();
  HashSet<String> exemptedTables = new HashSet<String>();

  Pattern pUpper = Pattern.compile("upper", Pattern.CASE_INSENSITIVE);
  static AdeDiffHelper diffHelper = new AdeDiffHelper("diff.txt");

  ModifiedComboboxParser parser;
  private Node baseViewObject;
  static boolean makeFix =
    false; //DO not support fixer mode anymore as it might conflict with UI SEarch case-sensitivity..
  static String viewRoot = "";
  static HashSet<String> tableColumns = new HashSet<String>();
  private static String mode = "view"; //can be view/LRG/premerge
  private static final String issuetype = "ViewCriteriaCaseMismatch_new";
  private static String series = "";
  private static int releaseNum = 11;
  
  public CaseInsensitiveViewCriteria_new() 
  {
      super(JoesBaseClass.CRAWL_TYPE.JPR);
  }

  public static void main(String[] args) throws Exception {

    mode = System.getProperty("mode");
    if (mode == null)
      mode = "view";
    //mode= "LRG";

    CaseInsensitiveViewCriteria_new civc =
      new CaseInsensitiveViewCriteria_new();

    writer = new BufferedWriter(new FileWriter("view_criteria_scan.csv"));
    writer.write("Family,Module,Product,Filename,Label,ViewAttribute, EntityAttribute," +
                 "EntityUsage, Column Name, Table Name, Column Type, Index, VC Required, Is VC Case-insensitive?, " +
                 "VC Item Name, VC Row Name, Original TableCol\n");
    civc.generateExemptionList();

    caseInsensitive_indexColumns =
        SqlIndexFinder.readIndexesFromFile("function");
    caseSensitive_indexColumns = SqlIndexFinder.readIndexesFromFile("default");
    SqlIndexFinder.readViewDataFromFile();
    SqlIndexFinder.readTableDataFromFile();

    if (mode.equals("view")) {

      if (args.length < 1 || args[0] == null || args[0].trim().equals("")) {
        System.out.println("Usage:  reviewViewCriteria.sh");
        System.exit(1);
      }

      sCrawlDir = args[0].trim();

      File f = new File(sCrawlDir);
      if (!f.exists()) {
        System.out.println("Crawl directory does not exist - " + sCrawlDir);
        System.exit(1);
      }

      viewRoot = ViewCriteriaHelper.getViewRoot(sCrawlDir);
      System.out.println("Analyzing all files in directory: " + sCrawlDir);
      m_family = ViewCriteriaHelper.getFamily(sCrawlDir);

      civc.crawlDirectory(sCrawlDir, false);

    } else if (mode.equals("LRG")) {

      series = args[0];
      //series = "FSM_MAIN_GENERIC";

      String release = FamilyModuleHelper.getRelease(series);
      try {
        releaseNum = Integer.parseInt(release);
      } catch (NumberFormatException e) {
        e.printStackTrace();
        releaseNum = 13;
      }

      String label = LRGUtil.getLatestLabel(series);
      System.out.println("Running " + issuetype + " Scan on series: " +
                         series + ".......");
      System.out.println("Latest label is: " + label);

      ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
      String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
      String viewRoot = serverTop + "/" + series + ".rdd/" + label + "/";

      for (int j = 0; j < families.size(); j++) {

        String family = families.get(j);
        m_family = family.toUpperCase();
        String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, issuetype);
        System.out.println("Crawling dir '" + viewRoot + sCrawlDir +
                           "' for family: " + family);
        civc.crawlDirectoryNew(viewRoot, sCrawlDir, false);
      }
    }

    writer.close();

    HashMap<String, String> tableColumnsData =
      SqlIndexFinder.findDataAllCaps(tableColumns);
    writer = new BufferedWriter(new FileWriter("view_criteria_scan1.csv"));
    writer.write("Family,Module,Product,Filename,Label,ViewAttribute, EntityAttribute," +
                 "EntityUsage, Column Name, Table Name, Column Type, Index, VC Required, Is VC Case-insensitive?, " +
                 "VC Item Name, VC Row Name, Original TableCol,Column Data Case, #Rows, #Blocks\n");

    BufferedReader reader =
      new BufferedReader(new FileReader("view_criteria_scan.csv"));

    String line = reader.readLine(); //header line

    while ((line = reader.readLine()) != null) {

      String[] parts = line.split(",");
      if (parts.length < 16)
        continue;

      String tableCol = parts[9].trim() + "," + parts[8].trim();
      String data = tableColumnsData.get(tableCol);
      String rows = SqlIndexFinder.tableRows.get(parts[9].trim());
      String blocks = SqlIndexFinder.tableBlocks.get(parts[9].trim());

      int numBlocks = 1000;
      try {
        if (blocks != null)
          numBlocks = Integer.parseInt(blocks);
      } catch (NumberFormatException e) {
        numBlocks = 1000;
      }


      String filePath = parts[3].trim();
      if (filePath.contains("/flex/") || filePath.contains("/publicFlex/"))
        continue;

      if (numBlocks < 200)
        continue;

      if (data != null && !data.equals("Not varchar")) {
        line += "," + data + "," + rows + ";" + blocks;

        if (parts[12].trim().equals("null") ||
            parts[12].trim().equals("Optional"))
          continue;

        writer.write(line + "\n");
      }
    }
    writer.close();
    reader.close();

    if(mode.equals("LRG"))
      civc.dumpResultsToAngrybirds();
  }
  
  private void dumpResultsToAngrybirds() {
    try{
      Connection con = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan", "codescan", "codescan");
      con.setAutoCommit(false);
      
      CallableStatement cstmt = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT_P(?,?,?,?,?,?,?,?,?) }");
      cstmt.registerOutParameter(1, Types.INTEGER);
      
      String fileName = "view_criteria_scan1.csv";
      
      BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
      String line = fileReader.readLine();
         
        System.out.println("First delete records from DB...");
        Statement stmt = con.createStatement();
      
        String sql = "DELETE FROM CODESCAN_RESULTS_P WHERE ISSUETYPE = 'ViewCriteriaCaseMismatch_new' and series = '" + series + "'" ;
        stmt.executeUpdate(sql);
        stmt.close();
      
      PreparedStatement pstmt = con.prepareStatement("INSERT INTO VIEW_CRITERIA_SCAN_P(SEQ_ID, VIEW_ATTR, ENTITY_ATTR," +
          "ENTITY_USAGE, COL_NAME, TABLE_NAME, COL_TYPE, COL_INDEX, VC_REQUIRED,VC_ISUPPER, COLUMN_DATA_CASE) " +
          "VALUES(?,?,?,?,?,?,?,?,?,?,?)");
      System.out.println("Now inserting values...");
      String[] labelSeries = null;
      String prevLabel = "";
      int count=0;
      
      while((line = fileReader.readLine()) != null) {
          
        String[] parts = line.split(",");
        if(parts.length < 16) continue;
  
          
          if(prevLabel != parts[4].trim()) {
              labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
              prevLabel = parts[4].trim();
          }
          
        cstmt.setString(2, parts[0].trim()); //family
        cstmt.setString(3, parts[1].trim()); //module
        cstmt.setString(4, parts[2].trim()); //product
        cstmt.setString(5, parts[3].trim()); //filename
        cstmt.setString(9, labelSeries[0]); //series
        cstmt.setString(10, labelSeries[1]); //label
        cstmt.setString(6, "ViewCriteriaCaseMismatch"); //issue        
        cstmt.setString(7, ""); //subissue        
        cstmt.setString(8,parts[15].trim() + ":" + parts[14].trim() + ": VC missing key index");
        cstmt.execute();
          
        int seq = cstmt.getInt(1);
        pstmt.setInt(1,seq); 
        pstmt.setString(2,parts[5]); //view_attr
        pstmt.setString(3,parts[6]); //entity_attr
        pstmt.setString(4,parts[7]);  //entity_usage
        pstmt.setString(5,parts[8]); //column name
        pstmt.setString(6,parts[9]); //table name
        pstmt.setString(7,parts[10]); //column type
        pstmt.setString(8,parts[11]); //column index
        pstmt.setString(9,parts[12]); //vc_required
        pstmt.setString(10,parts[13]); //vc_isupper
        
        String col_data = "";
        if(parts.length > 17)
            col_data = parts[17].trim();
        pstmt.setString(11,col_data); //column_data_case
      
        pstmt.execute();    
        count++;
      }
      con.commit();
      cstmt.close();
      pstmt.close();
      System.out.println("Total count for ViewCriteriaCaseMismatch is "+count +" on series "+series);
      
    }catch(Exception e){
      e.printStackTrace();
    }
  }
 

  protected void processFile(File fName, boolean doAde){
   
    try{
    String m_jprAbsFilePath = fName.getAbsolutePath(); //name of the jpr
    JprHelper jprHelper = new JprHelper();

    parser =
        new ModifiedComboboxParser(m_family.toLowerCase(), ModifiedComboboxParser.ScanType.ALL);
    parser.setJprAbsFilePath(m_jprAbsFilePath);

    HashMap<String, String> listOfAllVOs =
      jprHelper.getListOfAllViewObjectsInJpr(m_jprAbsFilePath);

    Iterator<String> voFilePathIter = listOfAllVOs.values().iterator();

    while (voFilePathIter.hasNext())
      processVO(voFilePathIter.next());
    } catch(Exception e){
      e.printStackTrace();
    }

  }


  public void processVO(String f) throws Exception {

    XMLDocument doc = XMLParserHelper.getXMLDocument(f);

    if (doc == null) {
      System.out.println("Could not parse XMLDocument..Skipping");
      return;
    }

    NodeList viewObjects = doc.getElementsByTagName("ViewObject");
    if (viewObjects == null || viewObjects.getLength() != 1) {
      System.out.println("WARNING:  Unexpected number of view objects found");
      System.out.println("Skipping...");
      return;
    }
    NodeList vc_rows = doc.getElementsByTagName("ViewCriteriaRow");
    Node viewObject = viewObjects.item(0);

    String where = XMLParserHelper.getAttributeValue(viewObject, "Where");
    Node sqlQuery =
      XMLParserHelper.getChildNodeWithName(viewObject, "SQLQuery");

    if (vc_rows == null && where == null && sqlQuery == null)
      return; //VO has no view criteria

    vaAliasAttributeMap = new HashMap<String, Node>();
    vaNameAttributeMap = new HashMap<String, Node>();

    extendedVaAliasAttributeMap = new HashMap<String, Node>();
    extendedVaNameAttributeMap = new HashMap<String, Node>();

    colNameTableNames = new HashMap<String, HashSet>();
    colAliasName = new HashMap<String, String>();

    extendedColNameTableNames = new HashMap<String, HashSet>();
    extendedColAliasName = new HashMap<String, String>();


    /*  if(where != null)
          processWhereClause(viewObject,where ,f, m_jprAbsFilePath);*/

    if (vc_rows != null) {
      createVOAttributeMappings(doc);

      SQLParserHelper.parseSql(viewObject, colNameTableNames, colAliasName);

      String baseVo =
        XMLParserHelper.getAttributeValue(viewObjects.item(0), "Extends");
      if (baseVo != null) {

        XMLDocument baseVoDoc = parser.getVOXml(baseVo);
        if (baseVoDoc == null)
          return;
        NodeList baseViewObjects =
          baseVoDoc.getElementsByTagName("ViewObject");
        if (baseViewObjects == null || baseViewObjects.getLength() != 1)
          return;
        baseViewObject = baseViewObjects.item(0);

        SQLParserHelper.parseSql(baseViewObject, extendedColNameTableNames,
                                 extendedColAliasName);
      }

      processViewCriteria(viewObject, vc_rows, f);
    }
    /* if(sqlQuery != null)
          processSQLQuery(sqlQuery,f);     */
  }

  private void createVOAttributeMappings(XMLDocument doc) throws Exception {

    NodeList view_attributes = doc.getElementsByTagName("ViewAttribute");

    for (int i = 0; i < view_attributes.getLength(); i++) {

      Node va = view_attributes.item(i);
      String alias = XMLParserHelper.getAttributeValue(va, "AliasName");
      if (alias == null) {
        //errors.write(fileName + "->" +  XMLParserHelper.getAttributeValue(va, "Name") + " -> No Alias\n");
        continue;
      }
      String entityUsage =
        XMLParserHelper.getAttributeValue(va, "EntityUsage");

      if (entityUsage != null)
        alias += "_" + entityUsage;
      vaAliasAttributeMap.put(alias.trim().toUpperCase(), va);

      String name = XMLParserHelper.getAttributeValue(va, "Name");
      vaNameAttributeMap.put(name, va);
    } //for

    NodeList viewObjects = doc.getElementsByTagName("ViewObject");
    if (viewObjects == null || viewObjects.getLength() != 1)
      return;
    String baseVo =
      XMLParserHelper.getAttributeValue(viewObjects.item(0), "Extends");

    if (baseVo != null) {

      XMLDocument baseVoDoc = parser.getVOXml(baseVo);
      if (baseVoDoc == null)
        return;
      NodeList baseViewObjects = baseVoDoc.getElementsByTagName("ViewObject");
      if (baseViewObjects == null || baseViewObjects.getLength() != 1)
        return;
      baseViewObject = baseViewObjects.item(0);

      view_attributes = baseVoDoc.getElementsByTagName("ViewAttribute");

      for (int i = 0; i < view_attributes.getLength(); i++) {
        Node va = view_attributes.item(i);
        String alias = XMLParserHelper.getAttributeValue(va, "AliasName");
        if (alias == null) {
          //errors.write(fileName + "->" +  XMLParserHelper.getAttributeValue(va, "Name") + " -> No Alias\n");
          continue;
        }
        String entityUsage =
          XMLParserHelper.getAttributeValue(va, "EntityUsage");

        if (entityUsage != null)
          alias += "_" + entityUsage;
        extendedVaAliasAttributeMap.put(alias.trim().toUpperCase(), va);

        String name = XMLParserHelper.getAttributeValue(va, "Name");
        extendedVaNameAttributeMap.put(name, va);
      } //for
    } //if baseVo != null

  }


  public void processViewCriteria(Node viewObject, NodeList vc_rows,
                                  String fileName) throws Exception {

    for (int i = 0; i < vc_rows.getLength(); i++) {

      Node vc_row = vc_rows.item(i);
      String isUpper_row =
        XMLParserHelper.getAttributeValue(vc_row, "UpperColumns");
      String vcRowName = XMLParserHelper.getAttributeValue(vc_row, "Name");
      String vcRequired = "";
      String isUpper_item = "";

      // if(isUpper != null && isUpper.equals("0")) continue;

      NodeList vc_row_children = vc_row.getChildNodes();

      for (int j = 0; j < vc_row_children.getLength(); j++) {

        vcRequired = "";
        isUpper_item = "";

        boolean useBaseViewObject = false;

        Node child = vc_row_children.item(j);
        String nodeName = child.getNodeName();

        if (!nodeName.equals("ViewCriteriaItem"))
          continue;

        vcRequired = XMLParserHelper.getAttributeValue(child, "Required");

        Node parentVC =
          XMLParserHelper.getParentNodeWithName(child, "ViewCriteria");
        String vcViewObjectName = null;
        if (parentVC != null)
          vcViewObjectName =
              XMLParserHelper.getAttributeValue(parentVC, "ViewObjectName");

        if (vcViewObjectName != null) {
          String[] parts = vcViewObjectName.split("\\.");
          vcViewObjectName = parts[parts.length - 1];
        }
        if (vcViewObjectName != null &&
            !vcViewObjectName.equals(XMLParserHelper.getAttributeValue(viewObject,
                                                                       "Name"))) {
          if (baseViewObject != null &&
              vcViewObjectName.equals(XMLParserHelper.getAttributeValue(baseViewObject,
                                                                        "Name"))) {
            //its ok
          } else {
            System.out.println("VCViewObjectName different from current VO name: " +
                               vcViewObjectName + " -> " + fileName);
            continue;
          }
        }

        isUpper_item =
            XMLParserHelper.getAttributeValue(child, "UpperColumns");
        //if(isUpper != null && isUpper.equals("0")) continue;

        String viewAttrName =
          XMLParserHelper.getAttributeValue(child, "ViewAttribute");
        String vc_name = XMLParserHelper.getAttributeValue(child, "Name");

        Node va = vaNameAttributeMap.get(viewAttrName);

        if (va == null) {
          va = extendedVaNameAttributeMap.get(viewAttrName);
          if (va != null)
            useBaseViewObject = true;
        }

        if (va == null) {
          System.out.println(fileName + "," + viewAttrName +
                             ", Could not find the ViewAttribute for ViewCriteriaItem");
          continue;
        }

        String col_name = "";
        String table_name = "";
        String colType = "";

        boolean entityFound = false;

        if (XMLParserHelper.getAttributeValue(va, "EntityUsage") !=
            null) { //if it is an entity based attribute

          Node ea = null;
          if (useBaseViewObject)
            ea =
                parser.getEntityAttributeNode(baseViewObject, va, viewAttrName);
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
        if (!entityFound) { //it is not an entity based attribute or the corresponding entity object could not be determined

          String aliasName =
            XMLParserHelper.getAttributeValue(va, "AliasName");
          colType = XMLParserHelper.getAttributeValue(va, "SQLType");

          if (useBaseViewObject)
            col_name = extendedColAliasName.get(aliasName);
          else
            col_name = colAliasName.get(aliasName);

          if (col_name == null) {
            //  errors.write("Could not get column name for attribute alias:" + aliasName + "\n");
            table_name = "Could not determine";
          } else {
            HashSet possibleTables = null;
            if (useBaseViewObject)
              possibleTables = extendedColNameTableNames.get(aliasName);
            else
              possibleTables = colNameTableNames.get(aliasName);

            for (Iterator it = possibleTables.iterator(); it.hasNext(); ) {
              String tableName = (String)it.next();
              table_name += tableName + " ";
            }
          }
        }

        String index = getIndex(table_name, col_name);
        if (index.equals("Could Not Determine"))
          continue;

        String tableName = table_name;
        String colName = col_name;
        if (tableName != null)
          tableName = tableName.trim();
        if (colName != null)
          colName = colName.trim();

        if (!ViewCriteriaHelper.isEmpty(indexedTableName))
          tableName = indexedTableName;

        String resolvedTableName = " ";
        if (!ViewCriteriaHelper.isEmpty(resolvedTableColumn)) {
          Matcher m = pTablesColumn.matcher(resolvedTableColumn);
          if (m.find()) {
            colName = m.group(2).trim();
            resolvedTableName = m.group(1).trim();
          }
        }

        String isUpper = "true";
        if (isUpper_item != null && isUpper_item.equals("0"))
          isUpper = "false";
        if (isUpper_item == null && isUpper_row != null &&
            isUpper_row.equals("0"))
          isUpper = "false";

        if (colType != null && !colType.contains("VARCHAR"))
          continue;
        //            if(vcRequired == null || vcRequired.equals("Optional"))
        //                continue;
        if (isUpper.equals("true") && index.equalsIgnoreCase("upper"))
          continue;
        if (index.equalsIgnoreCase("both") || index.equalsIgnoreCase("none"))
          continue;
        if (isUpper.equals("false") && index.equalsIgnoreCase("default"))
          continue;

        if (isExempt(viewAttrName, ViewCriteriaHelper.getFileName(fileName)) ||
            isExemptedTable(tableName))
          continue;

        writer.write(ViewCriteriaHelper.getFileNameInfo(fileName, "") +
                     viewAttrName + "," +
                     XMLParserHelper.getAttributeValue(va, "EntityAttrName") +
                     "," +
                     XMLParserHelper.getAttributeValue(va, "EntityUsage") +
                     "," + colName + "," + tableName + "," + colType + "," +
                     index + "," + vcRequired + "," + isUpper + "," + vc_name +
                     "," + vcRowName + "," + table_name + ";" + col_name +
                     "\n");

        tableColumns.add(tableName.trim() + "," + colName.trim());

      } //for vc_row_children
    } //for vc_rows
  }

  public void generateExemptionList() {

    try {
      BufferedReader fileReader =
        new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
                                          "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions.txt"));
      String line = null;

      while ((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if (parts.length < 3)
          continue;
        String issue = parts[2].trim();
        if (issue.equals("ViewCriteria")) {

          String fileName = parts[0].trim();
          String attribute = parts[1].trim();

          HashSet<String> exemptLines = null;
          if (!exemptions.containsKey(fileName))
            exemptLines = new HashSet<String>();
          else
            exemptLines = exemptions.get(fileName);

          exemptLines.add(attribute);
          exemptions.put(fileName, exemptLines);
        }
        if (issue.equals("ViewCriteriaCaseMismatch")) {
          String tableName = parts[0].trim();
          exemptedTables.add(tableName);
        }
      }
    } catch (Exception e) {
      System.out.println("Skipping exemptions as file could not be found: " +
                         e.getMessage());
    }
  }

  private boolean isExempt(String attribute, String fileName) {

    if (!exemptions.containsKey(fileName))
      return false;

    HashSet<String> lines = (HashSet<String>)exemptions.get(fileName);
    if (lines.contains(attribute))
      return true;

    return false;
  }

  private boolean isExemptedTable(String tableName) {
    if (exemptedTables.contains(tableName))
      return true;
    return false;
  }


  private static String getIndex(String tableName,
                                 String colName) throws Exception {

    isView = false;
    resolvedTableColumn = " ";
    indexedTableName = " ";
    String[] tableNames = null;

    tableName = convertViewNamesIntoTableNames(tableName, colName);

    String index = "None";
    if (tableName == null)
      return index;

    if (isView) {

      Matcher m = pTablesColumn.matcher(resolvedTableColumn);
      if (m.find()) {
        tableName = m.group(1);
        colName = m.group(2);
      } else
        return "Could Not Determine";
    }


    tableNames = tableName.trim().split(" ");

    for (int i = 0; i < tableNames.length; i++) {

      String tablNameToUse = tableNames[i].trim();
      if (SqlIndexFinder.synonyms.containsKey(tablNameToUse))
        tablNameToUse = SqlIndexFinder.synonyms.get(tablNameToUse);

      HashSet caseInsensitiveIndexes =
        caseInsensitive_indexColumns.get(tablNameToUse);
      if (caseInsensitiveIndexes != null) {
        for (Iterator it = caseInsensitiveIndexes.iterator(); it.hasNext(); ) {
          String indexColumn = (String)it.next();
          if (indexColumn.equals(colName)) {
            index = "Upper";
            indexedTableName = tablNameToUse;
            break;
          }
        }
      }

      HashSet caseSensitiveIndexes =
        caseSensitive_indexColumns.get(tablNameToUse);
      if (caseSensitiveIndexes != null) {
        for (Iterator it = caseSensitiveIndexes.iterator(); it.hasNext(); ) {
          String indexColumn = (String)it.next();
          if (indexColumn.equals(colName)) {
            if (index.equals("None"))
              index = "Default";
            else
              index = "Both";

            indexedTableName = tablNameToUse;
            break;
          }
        }
      }

      if (!index.equals("None"))
        return index;
    } //for
    return index;
  }

  public static String convertViewNamesIntoTableNames(String table_name,
                                                      String aliasName) throws Exception {


    if (table_name == null || table_name.trim().equals(""))
      return "";
    String[] parts = table_name.split(" ");

    for (int i = 0; i < parts.length; i++) {

      String table = parts[i].trim();

      if (SqlIndexFinder.synonyms.containsKey(table))
        table = SqlIndexFinder.synonyms.get(table);

      if (SqlIndexFinder.viewNameText.containsKey(table)) {
        isView = true;

        if (ViewCriteriaHelper.isEmpty(resolvedTableColumn))
          resolvedTableColumn =
              SqlIndexFinder.getResolvedTablesColumn(table, aliasName);

        Matcher m = pTablesColumn.matcher(resolvedTableColumn);
        if (m.find()) {

          table = m.group(1).trim();
          if (SqlIndexFinder.viewNameText.containsKey(table)) {
            resolvedTableColumn =
                SqlIndexFinder.getResolvedTablesColumn(table, m.group(2));
            Matcher m1 = pTablesColumn.matcher(resolvedTableColumn);
            if (m1.find()) {
              table = m1.group(1).trim();
              if (SqlIndexFinder.viewNameText.containsKey(table)) {
                resolvedTableColumn =
                    SqlIndexFinder.getResolvedTablesColumn(table, m1.group(2));
                Matcher m2 = pTablesColumn.matcher(resolvedTableColumn);
                if (m2.find())
                  table = m2.group(1).trim();
              }
            }
          }

        }
      }
      table_name = table_name.replace(parts[i], table);
    } //for
    return table_name;
  }


  protected String getSummaryReport() {
    return null;
  }

  protected String getSummaryReportSubject() {
    return null;
  }
}
