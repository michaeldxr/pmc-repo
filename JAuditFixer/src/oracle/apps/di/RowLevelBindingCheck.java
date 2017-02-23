package oracle.apps.di;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import java.util.List;
import java.util.Map;

import oracle.apps.di.checker.BindingsChecker;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;
import oracle.apps.sqlanalyzer.ViewCriteriaHelper;
import oracle.apps.utility.JoesBaseClass;

import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RowLevelBindingCheck extends JoesBaseClass {
  private static BufferedWriter writer;
  private static HashMap<String, String> voNamesToScan =
    new HashMap<String, String>();
  private static boolean makeFix = false;
  private static String viewRoot = "";
  private static String mode = "view"; //can be view/LRG/premerge
  private static final String issuetype = "ADFdiRowLevelBindings";
  private static String sCrawlDir = "";

  static String EXEMPTIONS =
    FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
    "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/exemptions/ADFdi_RowLevelBinding_Exemptions.txt";

  static List<String> EXEMPTIONLIST;

  public RowLevelBindingCheck(JoesBaseClass.CRAWL_TYPE type) {
    super(type);
  }
    

  public static void main(String[] args) throws Exception {
    scan(args);
  }

  public static void scan(String[] args) throws Exception {

    String file = "ADFdiRowLevelBindings.csv";

    EXEMPTIONLIST = getExemptionList();

    mode = System.getProperty("mode");
    if (mode == null)
      mode = "view";

    if (mode.equals("view")) {
      
      if (args.length >= 3 && args[2] != null && args[2].trim().length() > 0) {
        file = args[2] + "/" + file;
      }
      writer = new BufferedWriter(new FileWriter(file));
      writer.write("FAMILY,MODULE,PRODUCT,FILENAME,LABEL,SERIES,VIEWACCESSOR,ROWLEVELBINDING,EXCEL,PAGEDEF\n");
      
      if (args.length > 1)
        makeFix = new Boolean(args[1].trim());

      sCrawlDir = args[0];
      //sCrawlDir = "/ade_autofs/ade_fusion_linux/FUSIONAPPS_PT.R8INT_LINUX.X64.rdd/LATEST/fusionapps/";
      viewRoot = ViewCriteriaHelper.getViewRoot(sCrawlDir);

      RowLevelBindingCheck x =
        new RowLevelBindingCheck(JoesBaseClass.CRAWL_TYPE.XLS);

      //Just construct a global list of VOs to scan in the first pass by crawling the current directory
      System.out.println("Scanning the current directory (" + sCrawlDir +
                         ") to look for Excels...");
      x.crawlDirectory(sCrawlDir, false);

      //This should run at top family level fusionapps/fsm/emgc/.. to scan the VOs
      int index = sCrawlDir.indexOf("/components/");
      if (index != -1)
        sCrawlDir = sCrawlDir.substring(0, index + 12);
      x = new RowLevelBindingCheck(JoesBaseClass.CRAWL_TYPE.VO_XML);
      System.out.println("Now scanning at the family level (" + sCrawlDir +
                         ") to look for VOs corresponding to those Excels...");
      x.crawlDirectory(sCrawlDir, false);

      writer.close();
      if (makeFix) {
        System.out.println("Fixing bind variable mismatches");
        x.startDiffLog();
        x.makeFixes();
        x.closeDiffLog();
        System.out.println("Done!");
      }
    } else if (mode.equals("LRG")) {
      
      if (args.length >= 3 && args[2] != null && args[2].trim().length() > 0) {
        file = args[2] + "/" + file;
      }
      writer = new BufferedWriter(new FileWriter(file));
      writer.write("FAMILY,MODULE,PRODUCT,FILENAME,LABEL,SERIES,VIEWACCESSOR,ROWLEVELBINDING,EXCEL,PAGEDEF\n");

      String series = args[0];
      String label = LRGUtil.getLatestLabel(series);
      System.out.println("Running " + issuetype + " Scan on series: " +
                         series + ".......");
      System.out.println("Latest label is: " + label);

      RowLevelBindingCheck x =
        new RowLevelBindingCheck(JoesBaseClass.CRAWL_TYPE.XLS);

      String sCrawlDir = LRGUtil.getCrawlDir(series, null, label, issuetype);
      System.out.println("Crawling dir: " + sCrawlDir);
      x.crawlDirectory(sCrawlDir, false);
      x = new RowLevelBindingCheck(JoesBaseClass.CRAWL_TYPE.VO_XML);
      x.crawlDirectory(sCrawlDir, false);
      System.out.println("Done crawling directory.");
      writer.close();

      LRGUtil.addLabelInfoToAB(issuetype, series, label);
    } else if (mode.equals("premerge")) {
      
      writer = new BufferedWriter(new FileWriter(file));
      writer.write("FAMILY,MODULE,PRODUCT,FILENAME,LABEL,SERIES,VIEWACCESSOR,ROWLEVELBINDING,EXCEL,PAGEDEF\n");

      //String[] files = args[0].trim().split("\n");
      String ade_view_root = args[1].trim();
      String filelistpath=args[0].trim();
      String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);
      String label = args[2].trim();

      String ret = FamilyModuleHelper.getRelease(label);
      System.out.println("Release: " + ret);
      
      if(ret=="" || ret==null){
          ret="8";
      }
      int release=8;
      try {
            release = Integer.parseInt(ret);
      } catch (NumberFormatException e) {
            e.printStackTrace();
      }
      
      if(release <11){
          return;
      }
      RowLevelBindingCheck x =
        new RowLevelBindingCheck(JoesBaseClass.CRAWL_TYPE.XLS);


      String path="";
      for (int i = 0; i < files.length; i++) {

        String filePath = ade_view_root + "/" + files[i].trim();
        File f = new File(filePath);
        if (!f.exists())
          continue;
        if (!x.fileOfInterest(filePath))
          continue;
        
        path=filePath;
        x.processFile(f, false);

      }
     int index = path.indexOf("/components/");
     if (index != -1)
          sCrawlDir = path.substring(0, index + 12);
     x = new RowLevelBindingCheck(JoesBaseClass.CRAWL_TYPE.VO_XML);
     System.out.println("Now scanning at the family level (" + sCrawlDir +
                           ") to look for VOs corresponding to those Excels...");
     x.crawlDirectory(sCrawlDir, false);
     System.out.println("Done crawling directory.");
     writer.close();
      
    }
  }

  public void makeFixes() throws Exception {

    BufferedReader fileReader =
      new BufferedReader(new FileReader("ADFdiRowLevelBindings.csv"));
    String line =
      fileReader.readLine(); //first line is the header - read & ignore

    String prevFilePath = null;
    XMLDocument doc = null;
    XmlFixer xmlFixer = null;
    String fileContentsAsString = null;
    BufferedWriter writer1 =
      new BufferedWriter(new FileWriter("CouldNotFix.txt"));

    while ((line = fileReader.readLine()) != null) {

      try {

        String[] parts = line.split(",");
        if (parts.length < 10)
          continue;

        String filePath = viewRoot + parts[3].trim();
        String viewAccessor = parts[6].trim();
        String rowLevelBindExisting = parts[7].trim().toLowerCase();

        if (!filePath.equals(prevFilePath)) {

          if (prevFilePath != null) {
            fileContentsAsString = xmlFixer.getFileContentsAsString(false);
            doADE(true, new File(prevFilePath), fileContentsAsString);
          }

          doc = XMLParserHelper.getXMLDocument(filePath);
          if (doc == null)
            continue;
          xmlFixer = new XmlFixer(filePath);
        }
        prevFilePath = filePath;

        if (doc == null)
          continue;

        NodeList viewaccessors = doc.getElementsByTagName("ViewAccessor");

        for (int i = 0; i < viewaccessors.getLength(); i++) {
          Node va = viewaccessors.item(i);
          String name = XMLParserHelper.getAttributeValue(va, "Name");
          if (name.equals(viewAccessor)) {
            xmlFixer.modifyAttribute(va, "RowLevelBinds", rowLevelBindExisting,
                                     "false", true);
            break;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (xmlFixer != null) {
      fileContentsAsString = xmlFixer.getFileContentsAsString(false);
      doADE(true, new File(prevFilePath), fileContentsAsString);
    }
    writer1.close();
  }

  protected void processFile(File fName, boolean bDoADE) {

    try {
      String filepath = fName.getAbsolutePath();

      if (filepath.endsWith("xlsx") || filepath.endsWith("xlsm")) {

        ADFDiWorkbook workbook = ADFDiWorkbook.getDIWorkbook(filepath, false);
        HashSet<String> pageDefNames =
          BindingsChecker.getListOfPageDefsInWorkbook(workbook);
        HashSet<String> absoluteageDefPaths =
          BindingsChecker.getAbsolutePageDefPaths(pageDefNames,
                                                  workbook.getSpreadsheetPath());
        if (absoluteageDefPaths == null || absoluteageDefPaths.size() < 1) {
          System.out.println("[ERROR] There was an error resolving page defs for workbook: " +
                             workbook.getSpreadsheetPath());
          return;
        }

        for (Iterator<String> it = absoluteageDefPaths.iterator();
             it.hasNext(); ) {
          String pagedef = it.next();
          System.out.println("Processing PageDef: " + pagedef +
                             " corresponding to workbook: " + filepath);
          try {
            XMLDocument doc = XMLParserHelper.getXMLDocument(pagedef);
            if (doc == null)
              continue;
            parsePageDef(doc, pagedef, filepath);
          } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Skipping pagedef: " + pagedef);
          }
        }
      } else if (filepath.endsWith("VO.xml")) {

        int index = filepath.indexOf("/oracle/apps/");
        if (index != -1) {
          String pathkey =
            filepath.substring(index + 1).replace("/", ".").replace(".xml",
                                                                    "");

          if (voNamesToScan.containsKey(pathkey)) {

            String pagedef_workbook = voNamesToScan.get(pathkey);

            XMLDocument doc = XMLParserHelper.getXMLDocument(filepath);
            NodeList viewAccesors = doc.getElementsByTagName("ViewAccessor");
            NodeList viewAttributes =
              doc.getElementsByTagName("ViewAttribute");
            HashSet<String> vaNames = new HashSet<String>();
            for (int i = 0; i < viewAttributes.getLength(); i++) {
              Node viewattr = viewAttributes.item(i);
              vaNames.add(XMLParserHelper.getAttributeValue(viewattr, "Name"));
            }

            for (int i = 0; i < viewAccesors.getLength(); i++) {

              Node va = viewAccesors.item(i);
              boolean isDependentDropdown = false;
              String rowbind =
                XMLParserHelper.getAttributeValue(va, "RowLevelBinds");
              if (rowbind == null)
                rowbind = "null";

              Node parameterMap =
                XMLParserHelper.getChildNodeWithName(va, "ParameterMap");
              if (parameterMap != null) {
                ArrayList<Node> pimaps =
                  XMLParserHelper.getChildNodesWithName(parameterMap, "PIMap");
                if (pimaps != null) {
                  for (int j = 0; j < pimaps.size(); j++) {
                    Node pimap = pimaps.get(j);
                    Node transientExpression =
                      XMLParserHelper.getChildNodeWithName(pimap,
                                                           "TransientExpression");
                    if (transientExpression != null) {
                      String value = transientExpression.getTextContent();
                      if (value != null && vaNames.contains(value)) {
                        isDependentDropdown = true;
                        break;
                      }
                    }
                  }
                }
              }
              if (!isDependentDropdown &&
                  !rowbind.toLowerCase().equals("false")) {
                String fileNameInfo =
                  FamilyModuleHelper.getFileNameInfo1(filepath);
                String[] parts = fileNameInfo.split(",");
                String fileName = parts[3];
                String viewAccessor =
                  XMLParserHelper.getAttributeValue(va, "Name");
                String excel = (pagedef_workbook.split(","))[1];
                String filter = fileName + "," + viewAccessor + "," + excel;

                if (checkExemption(filter)) {
                  writer.write(fileNameInfo + viewAccessor + "," + rowbind +
                               "," + pagedef_workbook + "\n");
                }
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Skipping File: " + fName.getAbsolutePath());
    }
  }

  private boolean checkExemption(String filter) {
    for (String exemptionStr : EXEMPTIONLIST) {
      if (exemptionStr.equals(filter)) {
        return false;
      } else {
        String[] filters = exemptionStr.split(",");
        if (filter.indexOf(filters[0]) >= 0 &&
            filter.indexOf(filters[1]) > 0 && filter.indexOf(filters[2]) > 0) {
          return false;
        }
      }
    }

    return true;
  }

  //Just construct a global list of VOs to scan in the first pass

  private void parsePageDef(XMLDocument doc, String pagedefName,
                            String workbookName) throws Exception {

    //HashMap<String,String> treeIterBindings = new HashMap<String,String>();

    NodeList bindingsNode = doc.getElementsByTagName("bindings");
    if (XMLParserHelper.hasOneNOnlyOneNode(bindingsNode)) {
      NodeList bindingChildList = bindingsNode.item(0).getChildNodes();
      if (bindingChildList != null && bindingChildList.getLength() > 0) {
        for (int i = 0; i < bindingChildList.getLength(); i++) {
          Node bindingChild = bindingChildList.item(i);
          if (XMLParserHelper.isNodeName(bindingChild, "tree")) {
            Node nodeDefinition =
              XMLParserHelper.getChildNodeWithName(bindingChild,
                                                   "nodeDefinition");
            String voName = "";
            if (nodeDefinition != null)
              voName =
                  XMLParserHelper.getAttributeValue(nodeDefinition, "DefName");
            //treeIterBindings.put(XMLParserHelper.getAttributeValue(bindingChild, "id"), voName);
            voNamesToScan.put(voName,
                              FamilyModuleHelper.getPathAfterFusionapps(pagedefName) +
                              "," +
                              FamilyModuleHelper.getPathAfterFusionapps(workbookName));
          }
        }
      }
    }
    //return treeIterBindings;
  }

  protected String getSummaryReport() {
    return null;
  }

  protected String getSummaryReportSubject() {
    return null;
  }

  public static List<String> getExemptionList() {
    List<String> exemptions = new ArrayList<String>();
    try {
      File f = new File(EXEMPTIONS);
      if (f == null || !f.exists())
        return exemptions;
      BufferedReader br = new BufferedReader(new FileReader(f));
      String line = "";
      while ((line = br.readLine()) != null) {
        if (isEmpty(line))
          continue;
        if (line.startsWith("#"))
          continue;
        String[] parts = line.split("#");
        if (parts == null || parts.length < 4)
          continue;
        String fileName = parts[0];
        String viewAccessor = parts[1];
        String excel = parts[2];
        exemptions.add(fileName + "," + viewAccessor + "," + excel);
      }
      if (br != null)
        br.close();
    } catch (Exception ex) {
    }
    return exemptions;
  }

  public static boolean isEmpty(String str) {
    if (str == null || str.trim().equals(""))
      return true;
    return false;
  }
}
