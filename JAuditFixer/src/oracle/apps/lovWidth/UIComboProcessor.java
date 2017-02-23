package oracle.apps.lovWidth;

import java.io.File;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.HashSet;

import java.util.Iterator;
import java.util.Set;

import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.votuning.DataBindings;
import oracle.apps.votuning.PageDef;

import oracle.apps.votuning.TuningConstants;
import oracle.apps.votuning.UiFileHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UIComboProcessor extends ModifiedComboboxParser{
  public UIComboProcessor(String family, ModifiedComboboxParser.ScanType scanType) {
      super(family, scanType);
  }

  
  private static boolean bDebug = true;


  /**
   * Absolute ui path. Since this is used by all the methods, decided to
   * make it a private class variable.
   */
  private String m_uiAbsPath = "";
  private String m_adeViewRoot = "";
  int comboboxNotRenderedCnt = 0;
  /**
   * Databindings.cpx file object for this ui project (jpr).
   * This is passed in from the caller
   */
  public DataBindings m_dataBindingsCpx = null;

  /**
   * Page def for this ui file
   */
  public PageDef m_pagedef = null;
  private String m_uiJprPath;

  
  ArrayList<String> errors = new ArrayList<String>();

  /**
   * Base lba path.
   * e.g. if the ui file path is: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/public_html/oracle/apps/financials/Test.jspx
   * then the absolute base path is derived from this ui path as:
   * $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/
   */
  private String m_baseLbaPath = "";

  //private HashMap<String, String> map_component_iterators = new HashMap<String, String>();
  public int nestedAmCounter = 0;

  /**
   * List of the output records used for analysis purposes
   */
  HashMap<String, String> commonVOPaths = new HashMap<String, String>();

  /**
   *
   * @param uiFile
   * @param bindings
   * @param errorLogger
   * @return
   * null if:
   * 1. [Prerequisite] Databindings is nulll
   * 2. page def is null
   * 3. Cannot obtain baseLba path from ui file - this means there is something seriously wrong with the ui path.
   * 4. [XML Parsing Error] Cannot parse the input ui file with the xml parser
   */
  public void analyzeUIFile(File uiFile, DataBindings bindings, String jprPath) {

      // validate input parameters
      boolean isValid = initializeAndValidateMandatoryParams(uiFile, bindings);
      if (!isValid) {
          System.out.println("[ERROR]Initialization failed (bindings, or pagedef or baselba path was null) ");
          return ;
      }
      String fileName = uiFile.getName();
      if (bDebug) {
          System.out.println("Processing file: " + fileName);
      }

      m_uiJprPath = jprPath;
      // traverse ui file
      XMLDocument uiXml = XMLParserHelper.getXMLDocument(m_uiAbsPath);
      if (uiXml == null) {
          return ;
      }

      Element rootNode = uiXml.getDocumentElement();
      if (rootNode != null && rootNode.hasChildNodes()) {
          traverseNodes(rootNode.getChildNodes(), uiFile);
      }

    
  }

  /**
   * Recursively traverses all entire ui file nodes.
   * 1. af:inputComboboxListOfValues
   * 2. get the model -> has to be EL
   * Gets the tree iterator and obtains the iterator binding from the page def.
   * Also gets the am package name from page def and cpx.
   * For the vo usage it tries to find the am and gets the vo fetch size.
   * @param childNodes
   * @param uiFile
   */
  public void traverseNodes(NodeList childNodes, File uiFile) {

      if (childNodes == null) {
          return;
      }

      int childNodeLength = childNodes.getLength();
      for (int i = 0; i < childNodeLength; i++) {
          Node child = childNodes.item(i);
          String nodeName = child.getNodeName();
          if (!isEmpty(nodeName) && "af:inputComboboxListOfValues".equals(nodeName.trim())) {
              String renderedVal = XMLParserHelper.getAttributeValue(child,"rendered");
              String visibleVal = XMLParserHelper.getAttributeValue(child,"visible");
              if(!isEmpty(renderedVal) && "false".equals(renderedVal.trim()))
              {
                  System.out.println("Combobox with rendered = false");
                    comboboxNotRenderedCnt++;
                  continue;
                  }
            if(!isEmpty(visibleVal) && "false".equals(visibleVal.trim()))
            {
                System.out.println("Combobox with visible = false");
                  comboboxNotRenderedCnt++;
                continue;
                }
              String modelValue = getModelAttributeValue(child);
              ConsumerVO consumer = new ConsumerVO();
              consumer.setProduct(UiFileHelper.getProductName(m_uiAbsPath));
              consumer.setUiFileFullPath(getAdeRelativePath(m_uiAbsPath));
              consumer.setUiFileName(uiFile.getName());
              consumer.setComboboxId(XMLParserHelper.getAttributeValue(child, "id"));

              if (isEmpty(modelValue) || !isPossibleELExpression(modelValue)) {
                  if (bDebug) {
                      String msg = String.format("[ERROR] : modelValue is either empty or not an EL %s . Skipping file: %s ",modelValue,m_uiAbsPath);
                      System.out.println(msg);
                  }
                  continue;
              }
  
              try {
                  HashMap<String, String> modelChannel = getModelLayerChannel(child);
                  if (modelChannel == null)
                      continue;
                  processModel(modelChannel, consumer);
              } catch (Exception ex) {
                  String msg = "****[LOV]Exception: " + ex.getMessage();
                  System.out.println(msg);
                  errors.add(msg);
                  }finally{
                  list_consumers.add(consumer);
                  }
          }

          if (child.hasChildNodes()) {
              traverseNodes(child.getChildNodes(), uiFile);
          }
      }
  }




  private String getAMNameFromDataControlName(String datacontrolName) {
      if (isEmpty(datacontrolName)) {
          return null;
      }
      int lastindexDc = datacontrolName.lastIndexOf("DataControl");
      if (lastindexDc != -1) {
          return datacontrolName.substring(0, lastindexDc);
      }
      return null;
  }

  public String getApplicationModuleFileLocation(String amPackage) {
      // check db first

      String amPath = getAmFile(amPackage);
      if (!isEmpty(amPath))
          return amPath;
      amPath = getBCPathFromDB(amPackage);
      if (!isEmpty(amPath)) {

          String viewRoot = getAdeViewRoot();
          amPath = viewRoot + "/" + amPath;

      }
      return amPath;
      // Location 1: project dependencies (e.g. ui project -> dependencies-> model project (buidoutput).
      // can get the model project from here, and check for the am package


  }

  



  private boolean updateDisplayAttrNames(Node listBindingNode) {
      return true;
  }

  private boolean isEOBasedVA(String dataSourceName) {
      if (!isEmpty(dataSourceName) && dataSourceName.contains(".")) {
          return true;
      }
      return false;
  }

  private String getEOUsageFromVOXml(XMLDocument voXml, String EOName) throws Exception {
      if (voXml == null) {
          throw new Exception("Could not obtain vo xml");
      }
      NodeList listViewObj = voXml.getElementsByTagName("ViewObject");
      // should never happen
      if (!XMLParserHelper.hasOneNOnlyOneNode(listViewObj)) {
          // System.out.println("Expecting one and only one view object node");
          throw new Exception("Expecting one and only one view object node in vo file: ");
      }
      Node voNode = listViewObj.item(0);
      Node eoUsageNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(voNode, "EntityUsage", "Name", EOName);
      if (eoUsageNode == null) {
          return null;
      }
      return XMLParserHelper.getAttributeValue(eoUsageNode, "Entity");

  }


  public boolean isApplcoreVO(String voPackage) {
      if (voPackage.startsWith("oracle.apps.fnd.applcore"))
          return true;
      return false;

  }

  public boolean isComonVOPath(String bcPackage) {
      Set<String> keySet = commonVOPaths.keySet();
      Iterator<String> keyIter = keySet.iterator();
      while (keyIter.hasNext()) {
          String commonPackage = keyIter.next();
          if (bcPackage.contains(commonPackage)) {
              return true;
          }
      }
      return false;
  }

  public String getADEViewRoot() {
      int fusionappsIndex = m_uiAbsPath.indexOf("/fusionapps/");
      return m_uiAbsPath.substring(0, fusionappsIndex);
  }

  public String getCommonVOLocation(String bcPackage) {
      Set<String> keySet = commonVOPaths.keySet();
      Iterator<String> keyIter = keySet.iterator();
      while (keyIter.hasNext()) {
          String commonPackage = keyIter.next();
          if (bcPackage.contains(commonPackage)) {
              String packagePath = commonVOPaths.get(commonPackage);
              String adeViewRoot = getADEViewRoot();
              String voBaseLocation = adeViewRoot + packagePath;
              return voBaseLocation;
          }
      }
      return "";
  }



  public String getVOFullPackageFromAM(XMLDocument amXml, String viewUsageName) throws Exception {

      boolean bFoundViewUsage = false;
      NodeList appmoduleNodeList = amXml.getElementsByTagName("AppModule");
      if (!XMLParserHelper.hasOneNOnlyOneNode(appmoduleNodeList)) {
          String msg = "Expecting one and only one AppModule node in the amxml";
          throw new Exception(msg);
      }

      Node appModuleNode = appmoduleNodeList.item(0);
      NodeList appModuleChildren = appModuleNode.getChildNodes();
      if (appModuleChildren == null) {
          String msg = String.format("View usage tag not found in am");
          throw new Exception(msg);
      }
      int appModuleChildLen = appModuleChildren.getLength();
      for (int i = 0; i < appModuleChildLen; i++) {
          Node appModuleChild = appModuleChildren.item(i);
          String tagName = appModuleChild.getNodeName();
          if (!isEmpty(tagName) && tagName.trim().equals("ViewUsage")) {
              String obtainedVoUsageName = XMLParserHelper.getAttributeValue(appModuleChild, "Name");

              if (!isEmpty(obtainedVoUsageName) && obtainedVoUsageName.trim().equals(viewUsageName)) {

                  bFoundViewUsage = true;
                  String voUsagePackage = XMLParserHelper.getAttributeValue(appModuleChild, "ViewObjectName");
                  return voUsagePackage;


              }
          }
      }

      return "";
  }



  /**
   * Tries to find the am file : amPathToFind
   * 1.  From the ui file path - gets the uiJpr full path
   *      e.g. if the ui file name is : $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/public_html/oracle/apps/financials/Test.jspx
   *      UI Jpr will be: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/<something.jpr>
   * 2. if the UI Jpr is not null: get the dependent model project dependencies e.g. model.jpr
   *      2a. For each dependent model project jpr - get the base model directory
   *          e.g. if the model jpr is: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/model/Model.jpr
   *          base model directory is: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/model
   *      2b - try to find the am in this base model directory
   * [NOTE; It is most likely that the am will be in the dependent model projects and hence this is the first search location]
   * 3. if the am is not found in any of the dependent model jprs or if the ui jpr was not recoverable then :
   *      3a. Search for all probable model directories in the base lba location
   *          e.g. for ui path:$ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/public_html/oracle/apps/financials/Test.jspx
   *          base lba location is: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry
   *      3b. Find all probably model directories in this path
   *      3c. for each model directory found - try to locate the am.
   *
   * Return - amFile if found
   * Retrun - null if not found
   * @param amPathToFind
   * path of the am artifact to find
   * @return
   * Return - amFile if found
   * Retrun - null if not found
   */
  private String getAmFile(String amFullPackage) {
      File amFile = null;

      String amPathToFind = amFullPackage.replaceAll("\\.", "/");
      amPathToFind = amPathToFind + ".xml";

      String uiJprFullPath = UiFileHelper.getJprLocationForUiFile(m_uiAbsPath);
      if (uiJprFullPath != null) {
          HashSet<String> modelDependencyJars = JprHelper.getProjectDependencysInJar(uiJprFullPath);

          if (modelDependencyJars != null) {
              Iterator<String> modelJprIterator = modelDependencyJars.iterator();
              while (modelJprIterator.hasNext()) {
                  String modelJprLocation = modelJprIterator.next();
                  File modelFile = new File(modelJprLocation);
                  if (modelFile.exists()) {
                      String modelDir = getPreviousDirectory(modelJprLocation);
                      amFile = findBCInDirectory(modelDir, amPathToFind);
                      if (amFile != null) {
                          return amFile.getAbsolutePath();
                      }
                  }
              }

          }
      } else {
          return null;
      }

      if (isEmpty(m_baseLbaPath)) {
          System.out.println("ERROR: Base UI Path is empty.. skipping file:");
          return null;
      }
      ArrayList<String> modelDirs = getListOfModelDirectoriesInPath(m_baseLbaPath);

      for (int i = 0; i < modelDirs.size(); i++) {
          String modelDir = modelDirs.get(i);
          amFile = findBCInDirectory(modelDir, amPathToFind);
          if (amFile != null && amFile.exists()) {
              return amFile.getAbsolutePath();

          }
      }

      ArrayList<String> modelDirsInPrevDir = getListOfAllModelDirsInPrevDir(m_baseLbaPath);
      if (modelDirsInPrevDir != null && modelDirsInPrevDir.size() > 0) {
          int modeldirsinDirSize = modelDirsInPrevDir.size();
          for (int i = 0; i < modeldirsinDirSize; i++) {
              String modelDirInDir = modelDirsInPrevDir.get(i);
              amFile = findBCInDirectory(modelDirInDir, amPathToFind);
              if (amFile != null && amFile.exists()) {
                  return amFile.getAbsolutePath();

              }
          }
      }


      String msg =
          String.format("[ERROR] Could not find am in the probable directories.. AM: %s, UIFile: %s.. will look at dependencies in ui jpr now.. however during modifications will not be possible to modify this am...  ",
                        amPathToFind, m_uiAbsPath);
      System.out.println(msg);
      if (isEmpty(uiJprFullPath)) {
          System.out.println("Could not find jpr for ui file: " + m_uiAbsPath);
          return null;
      }

      return null;

  }


  private ArrayList<String> getListOfModelDirectoriesInPath(String baseDir) {
      if (isEmpty(baseDir)) {
          return null;
      }
      File f = new File(baseDir);
      ArrayList<String> list_modeDir = new ArrayList<String>();
      if (f.exists()) {
          File[] files = f.listFiles();
          int fileLen = files.length;
          for (int i = 0; i < fileLen; i++) {
              File file = files[i];
              String absPath = file.getAbsolutePath();

              if (file.isDirectory() && isValidModelDir(absPath))
                  list_modeDir.add(absPath);
          }

      }
      return list_modeDir;
  }

  private ArrayList<String> getListOFAllValidDirectoriesInPath(String baseDir) {
      if (isEmpty(baseDir)) {
          return null;
      }
      File f = new File(baseDir);
      ArrayList<String> list_allDirs = new ArrayList<String>();
      if (f.exists()) {
          File[] files = f.listFiles();
          int fileLen = files.length;
          for (int i = 0; i < fileLen; i++) {
              File file = files[i];
              String absPath = file.getAbsolutePath();

              if (file.isDirectory() && !absPath.contains(".ade_path"))
                  list_allDirs.add(absPath);
          }

      }
      return list_allDirs;
  }

  private boolean isValidModelDir(String dirPath) {
      if (isEmpty(dirPath)) {
          return false;
      }

      int lastSlash = dirPath.lastIndexOf("/");
      String lastDir = "nothing";
      if (lastSlash != -1) {
          lastDir = dirPath.substring(lastSlash);
          lastDir = lastDir.toLowerCase();
      }

      if (dirPath.contains(".ade_path") || lastDir.contains("dbschema") || lastDir.endsWith("test")) {
          return false;
      }

      if ((lastDir.contains("ui") || lastDir.contains("publicui")) && !lastDir.contains("model")) {
          return false;
      }
      if ((lastDir.contains("di") || lastDir.contains("publicdi")) && !lastDir.contains("model")) {
          return false;
      }


      return true;
  }

  private ArrayList<String> getListOfAllModelDirsInPrevDir(String baseDir) {
      ArrayList<String> modelDirs = new ArrayList<String>();
      if (isEmpty(baseDir)) {
          return null;
      }

      // am still not found.. then go one directory higher
      String prevDir = getPreviousDirectory(baseDir);
      if (isEmpty(prevDir)) {
          return null;
      }

      ArrayList<String> alldirs = getListOFAllValidDirectoriesInPath(prevDir);
      if (alldirs != null) {
          Iterator<String> allDirsIter = alldirs.iterator();
          while (allDirsIter.hasNext()) {
              String dir = allDirsIter.next();
              ArrayList<String> modeDirsInDir = getListOfModelDirectoriesInPath(dir);
              modelDirs.addAll(modeDirsInDir);
          }
      } else {
          return null;
      }
      return modelDirs;
  }

  private File findBCInDirectory(String modelDir, String amPath) {
      String amFullPAth = modelDir + "/src/" + amPath;
      File f = new File(amFullPAth);
      if (f.exists()) {
          return f;
      }
      return null;
  }

  private String getPreviousDirectory(String path) {
      if (!isEmpty(path)) {
          int indexLastSlash = path.lastIndexOf("/");
          if (indexLastSlash != -1) {
              return path.substring(0, indexLastSlash);
          }
      }
      return null;
  }

  private String extractIteratorNameFromEL(String elExp) {
      String extractedValue = "";
      if (isEmpty(elExp)) {
          return extractedValue;
      }
      if (elExp.startsWith("#{bindings.")) {
          int indexbindings = elExp.indexOf("#{bindings.");
          int startPos = indexbindings + "#{bindings.".length();
          int indexNextDot = elExp.indexOf(".", startPos);
          if (startPos != -1 && indexNextDot != -1) {
              String iteratorId = elExp.substring(startPos, indexNextDot);
              //System.out.println("IterName is : " + iteratorId);
              return iteratorId;
          } else {
              System.out.println("[ERROR] Could not find iter name from bindings");
          }
      } else if (elExp.contains(".bindings.")) {
          int indexbindings = elExp.indexOf(".bindings.");
          int startPos = indexbindings + ".bindings.".length();
          int indexNextDot = elExp.indexOf(".", startPos);
          if (startPos != -1 && indexNextDot != -1) {
              String iteratorId = elExp.substring(startPos, indexNextDot);
              //System.out.println("IterName is : " + iteratorId);
              return iteratorId;
          } else {
              System.out.println("[ERROR] Could not find iter name from bindings");
          }
      } else if (elExp.startsWith("#{")) {
          int indexbindings = elExp.indexOf("#{");
          int startPos = indexbindings + "#{".length();
          int indexfirstDot = elExp.indexOf(".", startPos);
          startPos = indexfirstDot + ".".length();
          int endPos = elExp.indexOf(".", startPos);
          if (startPos != -1 && endPos != -1) {
              String iteratorId = elExp.substring(startPos, endPos);
              return iteratorId;
          }
      } else {
          System.out.println("Not a valid EL Exp.. should start with #{");

      }
      return extractedValue;
  }

  /**
   *Is a possible EL Expression if it starts with '#{'
   * @param possibleEL
   * @return
   */
  private boolean isRowLevelBinding(String possibleEL) {
      if (!isEmpty(possibleEL) && possibleEL.trim().startsWith("#{row.")) {
          return true;
      }
      return false;
  }

  public String getTableIteratorIdForRow(Node componentNode) {
      String iteratorId = "";
      Node traversalNode = componentNode;
      while (true) {
          Node parentNode = traversalNode.getParentNode();
          String parentNodeName = parentNode.getNodeName();
          if (isEmpty(parentNodeName)) {
              break;
          }
          if (parentNodeName.equals("af:table") || parentNodeName.equals("af:treeTable")) {
              String modelValue = XMLParserHelper.getAttributeValue(parentNode, "value");
              iteratorId = extractIteratorNameFromEL(modelValue);
              break;
          }
          if (parentNodeName.equals("jsp:root")) {
              break;
          }
          traversalNode = parentNode;
      }

      return iteratorId;
  }


  // model layer needs the lovUses, amname

  public HashMap<String, String> getModelLayerChannel(Node comboboxNode) throws Exception {

      String lovUses = "";
      String pageDefIteratorname = "";
      String amFullPackage = "";
      String voInstance = "";
      boolean isRowLevelBinding = false;
      String rowLevelAttr = "";
      String modelValue = getModelAttributeValue(comboboxNode);
      if (isEmpty(modelValue) || !isPossibleELExpression(modelValue)) {

          String msg = String.format("[ERROR] Cannot recognize format of value node: %s ", modelValue);
          throw new Exception(msg);
      }

      String modelIteratorId = extractIteratorNameFromEL(modelValue);

      if (isRowLevelBinding(modelValue)) {
          //TODO:
          // all row level are model driven
          String tableIteratorId = getTableIteratorIdForRow(comboboxNode);
          if (isEmpty(tableIteratorId)) {
              throw new Exception(String.format("RowLevel - Cannot obtain tableIteratorId.. modelValue: %s", modelValue));
          }
          PageDef.PageDefIteratorBinding iterBinding = m_pagedef.getIterBindingForTreeId(tableIteratorId);
          if (iterBinding == null) {
              String msg =
                  String.format("[RowBinding] Could not obtain the iterator binding for component Looking for iteratorId: %s",
                                tableIteratorId);

              throw new Exception(msg);
          }
          pageDefIteratorname = iterBinding.getId();
          // for these cases, have to get the attribute, first.
          String rowLevelVOAttr = modelIteratorId;
          rowLevelAttr = rowLevelVOAttr;
          isRowLevelBinding = true;
         
          // throw new Exception("TODO: Row Level Binding case");
      } else {
          // gets the page def iterator binding
          PageDef.pageDefListBindings listBinding = m_pagedef.getListBindingForId(modelIteratorId);
          if (listBinding == null) {
              // Beans are valid cases, that should go untouched. so not an exception
              if (modelIteratorId.contains("Bean")) {
                  return null;
              }
              String msg =
                  String.format("[ListBindingNotObtained] Possible binding to bean:  list binding for iteratorId %s, ui file:",
                                m_uiAbsPath);
              throw new Exception(msg);
          }
          // 1. -LOV USES
          lovUses = listBinding.getLovUses();


          PageDef.PageDefIteratorBinding iterBinding = null;
          String pageDefListBindingType = listBinding.getLovType();

          if (pageDefListBindingType.equals(TuningConstants.LOV_TYPE_DYNAMICLIST)) {
              iterBinding = m_pagedef.getIteratorBindingWithId(listBinding.getListIter());
          } else if (pageDefListBindingType.equals(TuningConstants.LOV_TYPE_MODEL_DRIVEN)) {
              iterBinding = m_pagedef.getIterBindingForListId(modelIteratorId);
          } else if (pageDefListBindingType.equals(TuningConstants.LOV_TYPE_NAVIGATION)) {
              System.out.println("TODO: Navigation list");
              iterBinding = m_pagedef.getIteratorBindingWithId(listBinding.getListIter());
          }


          if (iterBinding == null) {
              String msg =
                  String.format("Could not obtain the iterator binding for combobx, in ui file: %s.. modelIteratorId %s",
                                m_uiAbsPath, modelIteratorId);

              throw new Exception(msg);
          }
          pageDefIteratorname = iterBinding.getId();

      }

      PageDef.PageDefIteratorBinding iterBinding = m_pagedef.getMapIterators().get(pageDefIteratorname);
      if (iterBinding == null) {
          String msg =
              String.format("[ERROR] :updateVOPropertiesInRecord: Could not find iterator Id: %s in page def %s Skipping this record in the UI File %s",
                            pageDefIteratorname, m_pagedef.getAbsPath(), m_uiAbsPath);

          throw new Exception(msg);
      }

      String voInstanceName = iterBinding.getBindName();
      String datacontrolName = iterBinding.getDataControlName();
      if (datacontrolName == null) {


          String msg =
              String.format("[ERROR] :updateVOPropertiesInRecord: page def entry for iterator Binding %s does not have an entry for 'DataControlName'. uiFile: %s.. pagedef path: %s cpx path: %s.. skipping",
                            pageDefIteratorname, m_uiAbsPath, m_dataBindingsCpx.getCpxFullPath(), m_pagedef.getAbsPath());

          throw new Exception(msg);
      }

      String amName = getAMNameFromDataControlName(datacontrolName);
      if (amName == null) {

          String msg =
              String.format("[ERROR] :updateVOPropertiesInRecord:  could not obtain am name from data control: %s uiFile: %s..",
                            datacontrolName, m_uiAbsPath);

          throw new Exception(msg);
      }
      String dcPackage = m_dataBindingsCpx.getDataControlPackage(datacontrolName);
      if (isEmpty(dcPackage)) {


          String msg =
              String.format("[ERROR] :updateVOPropertiesInRecord:  could not obtain data control package name for datacontrol name: %s,  databindings.cpx file path: %s uiFile: %s..",
                            datacontrolName, m_dataBindingsCpx.getCpxFullPath(), m_uiAbsPath);

          throw new Exception(msg);
      }
      amFullPackage = dcPackage + "." + amName;
      voInstance = voInstanceName;

      HashMap<String, String> channelpropeties = new HashMap<String, String>();
      channelpropeties.put("amPackage", amFullPackage);
      channelpropeties.put("voInstance", voInstance);
      channelpropeties.put("lovUses", lovUses);
      if (isRowLevelBinding && isEmpty(rowLevelAttr)) {
          throw new Exception("for row level binding row level attribute cannot be empty.. " + modelValue);
      }
      if (isRowLevelBinding) {
          channelpropeties.put("rowLevelAttribute", rowLevelAttr);
      }
      return channelpropeties;
  }


  /**
   * input
   * lovUses (from ui page def)
   * voInstanceName (from ui pagedef - Iterator)
   * amPackage  - from ui (pagedef + databindings)
   * rowLevelAttr - if it is a row level binding on the ui (e.g. bindings.row.CurrencyCode) then lovUses = null
   * and have to get lov uses from attribute (currencycode) on the lov instance
   * 
   * 
   * @param modelChannel
   * @param consumer
   * @throws Exception
   */
  public void processModel(HashMap<String, String> modelChannel, ConsumerVO consumer) throws Exception {
      String lovUses = modelChannel.get("lovUses");
      String voIntanceName = modelChannel.get("voInstance");
      String amPackage = modelChannel.get("amPackage");
      String rowLevelAttr = modelChannel.get("rowLevelAttribute");

      if (isEmpty(voIntanceName) || isEmpty(amPackage)) {
          throw new Exception("Missing vo instance name, or am package");
      }

      String amAbsPath = getApplicationModuleFileLocation(amPackage);
      if (isEmpty(amAbsPath)) {
          throw new Exception("Could not obtain am: " + amPackage);
      }
      String jprFilePath = UiFileHelper.getJprLocationForModelFile(amAbsPath);
      if (!isEmpty(jprFilePath)) {
          setJprAbsFilePath(jprFilePath);
      }
      String voPackage = getVOFullPackageFromAM(XMLParserHelper.getXMLDocument(amAbsPath), voIntanceName);
      if (isEmpty(voPackage)) {
          throw new Exception("Could not obtain vo instance from am: " + voIntanceName + ": " + amPackage);
      }

      // get VO , get listbinding with name lov uses
      XMLDocument voXml = getViewObjectAsXml(voPackage, amAbsPath);
      if (voXml == null) {
          throw new Exception("Could not get vo xml for: " + voPackage);
      }

      if (!isEmpty(rowLevelAttr)) {
          // Need to first do row level processing to get the attribute and the lov uses from it.
          // get the listBindingNode
          Node attributeNode =
              ViewObjectHelper.getAttributeByName(ViewObjectHelper.getViewObjectNodeFromxml(voXml), rowLevelAttr);
          if (attributeNode == null) {
              throw new Exception("[ERROR] Could not get attribute Node for a row level binding..Looking for attr: " + rowLevelAttr);
          }
          String rowLevelLovUses = XMLParserHelper.getAttributeValue(attributeNode, "LOVName");
          if (isEmpty(rowLevelLovUses)) {
              throw new Exception("could not obtain row level lov uses for attribute: " + rowLevelAttr + " in pcakge: " +
                                  voPackage);
          }
          lovUses = rowLevelLovUses;
      }
      consumer.setLovUses(lovUses);
      if (isEmpty(lovUses)) {
          throw new Exception("*** Lov uses was not obtained.. ");
      }
      Node listBindingNode = ViewObjectHelper.getListBindingWithName(voXml, lovUses);
      if (listBindingNode == null) {
          System.out.println("---- [ERROR] Could not get list binding node: " + lovUses);
          return;
      }

      ArrayList<String> displatAttrs = ViewObjectHelper.getListBindingDisplayAttrNames(listBindingNode);

      if (displatAttrs == null || displatAttrs.size() == 0) {
          System.out.println(" **** How come no display attrs on combobox.. pls check vo..  " + voPackage);
          return;
      }

      Iterator<String> displayAttrsIter = displatAttrs.iterator();
      String listBindingVoPackage;

      if (isEntityBasedListBinding(listBindingNode)) {


          listBindingVoPackage =
                  getViewAccessorVOPackage(listBindingNode, ViewObjectHelper.getViewObjectNodeFromxml(voXml), amAbsPath);



      } else {

          listBindingVoPackage = getListBindingVOPackage(listBindingNode, ViewObjectHelper.getViewObjectNodeFromxml(voXml));

      }
      if(isEmpty(listBindingVoPackage))
      {
          throw new Exception("Could not get list Binding VO Package!!");
          }
 
      XMLDocument listVO = getViewObjectAsXml(listBindingVoPackage, amAbsPath);
      if (listVO == null) {
          throw new Exception("Could not obtain list VO");
      }


      Node listBindingVONode = ViewObjectHelper.getViewObjectNodeFromxml(listVO);
      if (listBindingVONode == null)
          return;
      while (displayAttrsIter.hasNext()) {
          String displayAttr = displayAttrsIter.next();

         processModel(listBindingVONode,listBindingVoPackage,displayAttr,consumer);

      }
  }


  public String getViewAccessorVOPackage(Node listBindingNode, Node voNode, String amPath) {
      String viewAccessorName = XMLParserHelper.getAttributeValue(listBindingNode, "ListVOName");
      if (isEmpty(viewAccessorName))
          return "";
      String[] parts = viewAccessorName.split("\\.");
      if (parts == null || parts.length > 2) {
          System.out.println("The view acessor has more than 2 parts.. dont know how to handle");
          System.out.println(viewAccessorName);
          return "";
      }
      String eoName = parts[0];
      String vaName = parts[1];
      Node entityUsageNode = ViewObjectHelper.getEntityUsageNodeWithName(voNode, eoName);
      if (entityUsageNode == null) {
          System.out.println("[WEIRD!!! ] Cannot find entity usage node for : " + eoName);
          return "";
      }
      String entityPackage = XMLParserHelper.getAttributeValue(entityUsageNode, "Entity");
      if (isEmpty(entityPackage)) {
          System.out.println("[WEIRD] could not obtain eneity package from usage node.. ");
          return "";
      }
      if (!isValidProducerPackage(entityPackage)) {
          System.out.println("Not a valid producer package:" + entityPackage);
          return "";
      }
      XMLDocument eoXml = getEntityObjectAsXml(entityPackage, amPath);
      if (eoXml == null) {
          System.out.println("Could not find eo xml .. ");
          return "";
      }
      Node eoNode = EntityObjectHelper.getEntityObjectNode(eoXml);
      if (eoNode == null) {
          System.out.println("[WEIRD!!] could not obtain eo node from eoxml");
          return "";
      }
      Node viewAccessorNode = EntityObjectHelper.getViewAccessorWithName(eoNode, vaName);
      String viewAccessorVOPackage = XMLParserHelper.getAttributeValue(viewAccessorNode, "ViewObjectName");
      return viewAccessorVOPackage;


  }


  public XMLDocument findBCFromProjectDependencies(String jprFilePath, String bcPackageToFind) {
      XMLDocument bcXml = null;
      if (isEmpty(jprFilePath))
          return bcXml;
      if (!jprFilePath.endsWith(".jpr"))
          return bcXml;


      HashSet<String> adfLibrarys = JprHelper.getAllAdfLibrarysInJpr(jprFilePath, true);
      bcXml = ViewObjectHelper.findBCFromSetOfJarFiles(adfLibrarys, bcPackageToFind);
      if (bcXml != null)
          return bcXml;

      HashSet<String> adfbcImports = JprHelper.getAllBcImportsInJpr(jprFilePath, true);
      bcXml = ViewObjectHelper.findBCFromSetOfJarFiles(adfbcImports, bcPackageToFind);
      if (bcXml != null)
          return bcXml;
      // Also increase search paths, by looking in ADFLibrarydependencies file
      HashSet<String> secondaryDeps = JprHelper.getAllSecondaryDependencies(jprFilePath);
      bcXml = ViewObjectHelper.findBCFromSetOfJarFiles(secondaryDeps, bcPackageToFind);
      return bcXml;
  }


  public XMLDocument getViewObjectAsXml(String voPackage, String amFilePath) {
      String voPath = "";
      String modelJpr = UiFileHelper.getJprLocationForModelFile(amFilePath);

      // TODO First find it in the current project
      JprHelper jprHelper = new JprHelper();
      listOfAllVOs = jprHelper.getListOfAllViewObjectsInJpr(modelJpr);
      if (listOfAllVOs != null && listOfAllVOs.containsKey(voPackage)) {
          String pthObt = listOfAllVOs.get(voPackage);
          if (!isEmpty(pthObt))
              return XMLParserHelper.getXMLDocument(pthObt);
      }
      if (!isEmpty(modelJpr)) {
          XMLDocument voXml = findBCFromProjectDependencies(modelJpr, voPackage);
          if (voXml != null) {
              return voXml;
          }
      }
      voPath = getBCPathFromDB(voPackage);
      if (!isEmpty(voPath)) {

          String viewRoot = getAdeViewRoot();
          voPath = viewRoot + "/" + voPath;

      }
      if (isEmpty(voPath))
          return null;
      return XMLParserHelper.getXMLDocument(voPath);
  }

  public XMLDocument getEntityObjectAsXml(String voPackage, String amFilePath) {
      String voPath = "";
      String modelJpr = UiFileHelper.getJprLocationForModelFile(amFilePath);

      // TODO First find it in the current project
      JprHelper jprHelper = new JprHelper();
      HashMap<String, String> eomaps = jprHelper.getListOfAllEntityObjectsInJpr(modelJpr);
      if (eomaps != null && eomaps.containsKey(voPackage)) {
          String pthObt = eomaps.get(voPackage);
          if (!isEmpty(pthObt))
              return XMLParserHelper.getXMLDocument(pthObt);
      }
      if (!isEmpty(modelJpr)) {
          XMLDocument voXml = findBCFromProjectDependencies(modelJpr, voPackage);
          if (voXml != null) {
              return voXml;
          }
      }
      voPath = getBCPathFromDB(voPackage);
      if (!isEmpty(voPath)) {

          String viewRoot = getAdeViewRoot();
          voPath = viewRoot + "/" + voPath;

      }
      if (isEmpty(voPath))
          return null;
      return XMLParserHelper.getXMLDocument(voPath);
  }




  private String getModelAttributeValue(Node componentNode) {
      String nodeName = componentNode.getNodeName();
      if (isEmpty(nodeName)) {
          return null;
      }


      if (nodeName.equals("af:inputComboboxListOfValues")) {
          String modelValue = XMLParserHelper.getAttributeValue(componentNode, "model");
          return modelValue;
      }


      return null;
  }


 

  /**
   *Is a possible EL Expression if it starts with '#{'
   * @param possibleEL
   * @return
   */
  private boolean isPossibleELExpression(String possibleEL) {
      if (!isEmpty(possibleEL) && possibleEL.trim().startsWith("#{")) {
          return true;
      }
      return false;
  }

  /**
   * checks:
   * 1. uiFile != null
   * 2. bindings != null
   * 3. pageDef !=null
   * 4. m_baseLbaPath != null
   * 3. initializePageDef
   * 4. initialize uifilePath
   * 5. initialize m_dataBindingsCpx
   * @param uiFile
   * @param bindings
   * @return
   */
  public boolean initializeAndValidateMandatoryParams(File uiFile, DataBindings bindings) {

      if (uiFile == null)
          return false;
      m_uiAbsPath = uiFile.getAbsolutePath();
      if (bindings == null)
          return false;

      m_dataBindingsCpx = bindings;

      //---------------- get page definition for this file--------------
      File pageDef = getPageDef(uiFile, bindings);
      if (pageDef == null) {
          if (bDebug) {
              String msg = String.format("[No pagedef] Cannot get pageDef for file.. Skipping: %s", m_uiAbsPath);
              System.out.println(msg);
          }
          return false;
      }

      String pageDefAbsPath = pageDef.getAbsolutePath();
      m_pagedef = PageDef.parsePageDef(pageDefAbsPath);
      if (m_pagedef == null) {
          if (bDebug) {
              String msg =
                  String.format("[No pagedef] Cannot parse page def: %s for file.. Skipping: %s", pageDefAbsPath, m_uiAbsPath);
              System.out.println(msg);
          }
          return false;
      }

      m_baseLbaPath = UiFileHelper.getBaseLBAPath(m_uiAbsPath);
      if (isEmpty(m_baseLbaPath)) {
          if (bDebug) {
              String msg = "[ERROR] Could not get the base lba path for file: " + m_uiAbsPath;
              System.out.println(msg);
          }
          return false;
      }

      return true;

  }

  public File getPageDef(File uiFile, DataBindings bindings) {
      if (uiFile == null) {
          System.out.println("[ERROR] Input file was null");
          return null;
      }
      String absPath = uiFile.getAbsolutePath();
      HashMap<String, String> mapPagesPageDefs = bindings.getPagesPageDef();
      if (mapPagesPageDefs == null) {
          System.out.println("databindings do not have any page refs");
          return null;
      }
      String abstractPath = UiFileHelper.getUIFileAbstractPath(absPath);
      if (UiFileHelper.isEmpty(abstractPath)) {
          System.out.println("ui file abstract path is null");
          return null;
      }
      String pageDefDotPath = mapPagesPageDefs.get(abstractPath);
      if (UiFileHelper.isEmpty(pageDefDotPath)) {
          System.out.println("Could not obtain page def path for file: " + absPath);
          return null;
      }
      String pageDefAbsPath = pageDefDotPath.replaceAll("\\.", "/");
      String adfmsrcPath = UiFileHelper.getAdfmsrcPath(absPath);
      pageDefAbsPath = adfmsrcPath + pageDefAbsPath + ".xml";
      File pageDef = new File(pageDefAbsPath);
      if (pageDef.exists()) {
          return pageDef;
      }
      return null;
  }

  
  public String getAdeViewRoot() {
      if (isEmpty(m_adeViewRoot)) {
          int fappsIndex = m_uiAbsPath.indexOf("/fusionapps");
          if (fappsIndex != -1) {
              return m_uiAbsPath.substring(0, fappsIndex);
          }
      }

      return m_adeViewRoot;
  }


  public void setErrors(ArrayList<String> errors) {
      this.errors = errors;
  }

  public ArrayList<String> getErrors() {
      return errors;
  }


  public void setComboboxNotRenderedCnt(int comboboxNotRenderedCnt) {
      this.comboboxNotRenderedCnt = comboboxNotRenderedCnt;
  }

  public int getComboboxNotRenderedCnt() {
      return comboboxNotRenderedCnt;
  }
}
