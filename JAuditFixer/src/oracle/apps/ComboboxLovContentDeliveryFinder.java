package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileOutputStream;

import java.io.FileReader;

import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.JoesBaseClass;

import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ComboboxLovContentDeliveryFinder extends JoesBaseClass {
//  private static Pattern p =
//    Pattern.compile("/(?:.+?)/(?:.+?)/(.+\\.rdd)/(.+?)/(fusionapps/(.+?)/components/(.+?)/(.+?)/(?:.+)\\.(?:jspx|jsff))");
  private static Workbook myBook ;
  private static Worksheet analysisSheet;
  private static Connection con = null;
  private static PreparedStatement insert;
  private int row = 1;
  private static BufferedWriter writer;
  private static int releaseNum=0;
  
  HashMap<String,HashSet> exemptions = new HashMap<String,HashSet>();  
  
  private static String mode="view"; //LRG/view/premerge mode
  //list of UI components which have ContentDelivery attribute.
/*
  public static final String[] l_type =
  { "bubbleGraph", "attachment", "lineGraph", "hierarchyViewer",
    "horizontalBarGraph", "graph", "gauge", "tree", "comboGraph", "menu",
    "table", "panelCard", "pieGraph", "pivotTable", "treeTable",
    "projectGantt", "barGraph" };
*/
  public static final String[] l_type =
  { "inputComboboxListOfValues" };

  public ComboboxLovContentDeliveryFinder() {
    super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF);
    try {
      con =
        DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
      insert =
        con.prepareStatement("INSERT INTO codescan_Results_p (SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)");     
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("Failed to create the JDBC connection");
    }
  }

  private static void initWorkBook() {
    // Create a worksheet
    myBook = new Workbook("output");
    analysisSheet = new Worksheet("ContentDelivery Info");
    
    myBook.addWorksheet(analysisSheet);
    
    Worksheet validationSheet = new Worksheet("Validation");
    myBook.addWorksheet(validationSheet);
      
    validationSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("default")));
    validationSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("immediate")));
    validationSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("lazy")));
    
    myBook.defineName("ContentDelivery", "Validation", "$A$1", "$C$1");

    analysisSheet.setColumnWidth(1, 15);
    analysisSheet.addCell(new Cell(new CellLocation("A1"),
                                   new CellValue("SERIES")));

    analysisSheet.setColumnWidth(2, 15);
    analysisSheet.addCell(new Cell(new CellLocation("B1"),
                                   new CellValue("LABEL")));

    analysisSheet.setColumnWidth(3, 5);
    analysisSheet.addCell(new Cell(new CellLocation("C1"),
                                   new CellValue("FAMILY")));

    analysisSheet.setColumnWidth(4, 10);
    analysisSheet.addCell(new Cell(new CellLocation("D1"),
                                   new CellValue("MODULE")));

    analysisSheet.setColumnWidth(5, 4);
    analysisSheet.addCell(new Cell(new CellLocation("E1"),
                                   new CellValue("PRODUCT")));

    analysisSheet.setColumnWidth(6, 50);
    analysisSheet.addCell(new Cell(new CellLocation("F1"),
                                   new CellValue("FILENAME")));

    analysisSheet.setColumnWidth(7, 10);
    analysisSheet.addCell(new Cell(new CellLocation("G1"),
                                   new CellValue("ID")));

    analysisSheet.setColumnWidth(8, 10);
    analysisSheet.addCell(new Cell(new CellLocation("H1"),
                                   new CellValue("COMP_TYPE")));

    analysisSheet.setColumnWidth(9, 10);
    analysisSheet.addCell(new Cell(new CellLocation("I1"),
                                   new CellValue("CONTENTDELIVERY")));

    analysisSheet.setColumnWidth(10, 40);
    analysisSheet.addCell(new Cell(new CellLocation("J1"),
                                   new CellValue("Description")));
    analysisSheet.setColumnWidth(11, 12);
    analysisSheet.addCell(new Cell(new CellLocation("K1"),
                                     new CellValue("New ContentDelivery Value")));
    
  }

  public static void main(String[] args) throws Exception{
      
    ComboboxLovContentDeliveryFinder finder = new ComboboxLovContentDeliveryFinder();
    finder.initWorkBook();
    finder.generateExemptionList();
    finder.markStartTime();
    FamilyModuleHelper.generateDeferedPaths();
    
      mode = System.getProperty("mode");
      if(mode == null)
          mode = "view";
    
      if(mode.equals("view")) {
            if (args.length < 1 || args[0] == null || args[0].equals("")) {
              //System.out.println("USAGE: contentDeliveryFinder.sh <crawlDirectory>");
              System.out.println("USAGE: comboboxContentDeliveryFinder.sh <crawlDirectory>");
              System.exit(1);
            }
        
            String dir = args[0];
            //String dir = "/scratch/lkwan/view_storage/lkwan_r11_oct13x2/fusionapps/fin/components/payables/ce/bankRelationships";
            //String dir = "/scratch/lkwan/view_storage/lkwan_r11_oct13x2/fusionapps/fin";
            
            if(args.length > 1){
              String series = args[1].trim();
              String release= FamilyModuleHelper.getRelease(series);
              try{              
                  releaseNum = Integer.parseInt(release);
              }catch(NumberFormatException e){
                 e.printStackTrace();
                 releaseNum=0;
              }
            }
            System.out.println("Analyzing files in directory for contentDelivery: " + dir);
            
            finder.crawlDirectory(dir, false);
           
      } else if(mode.equals("LRG")) {
    
          String series = args[0]; //only needs series name as the argument      
          //String series = "FUSIONAPPS_PT.INTERPSCM_LINUX.X64"; 
          String release= FamilyModuleHelper.getRelease(series);
          try{              
              releaseNum = Integer.parseInt(release);
          }catch(NumberFormatException e){
             e.printStackTrace();
             releaseNum=0;
          }
          //String series ="FUSIONAPPS_PT.DEVINT_LINUX.X64";
          String label = LRGUtil.getLatestLabel(series);
          System.out.println("Running ComboboxLovContentDelivery Scan on series: " + series + ".......");
          System.out.println("Latest label is: " + label);
          
          ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
          for (int j = 0; j < families.size(); j++) {
              String family = families.get(j);
              String sCrawlDir = LRGUtil.getCrawlDir(series, family, label, "ComboboxLovContentDelivery");
              System.out.println("Crawling dir '" + sCrawlDir + "' for family: " + family);                
              finder.crawlDirectory(sCrawlDir,false);      
              System.out.println("Done crawling for family "+family+".");
          }        
          dumpToAB(series); //TODO: Might have to move this inside the for loop if batch grows too large
          LRGUtil.addLabelInfoToAB("ComboboxLovContentDelivery", series, label);          
      } else if(mode.equals("premerge")) {
          
          String[] files = args[0].trim().split("\n");
          String ade_view_root = args[1].trim();
          String label = args[2].trim();
          
          String release= FamilyModuleHelper.getRelease(label);
          System.out.println("Release: " + release);
          
          try{              
              releaseNum = Integer.parseInt(release);
              //if (releaseNum < 9) //This check was mandated post release 9 only, hence this check is necessary to avoid the current rel7 and rel8 branches from failing
              if (releaseNum < 11) //LKW?????This check was mandated post release 11 only, hence this check is necessary to avoid the current rel7 and rel8 branches from failing
                  return;              
          }catch(NumberFormatException e){
              System.out.println("Could not determine release. Skipping ContentDelivery check...");
              return;
          }
          
          writer = new BufferedWriter(new FileWriter("comboboxLovContentDelivery.txt"));
          
          for(int i = 0; i < files.length; i++) {
              
              String filePath = ade_view_root + "/" + files[i].trim();
              File f = new File(filePath);
              if(!f.exists())
                  continue;
              if(!finder.fileOfInterest(filePath))
                  continue;
              finder.processFile(f,false);
          }
          writer.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/ComboboxLovContentDelivery " +
              "for description of the issue and resolution.\n\n");
          writer.close();   
          
      }
      
    finder.generateSpreadSheet();
    finder.markEndTime();
  }
  
  private void generateSpreadSheet(){
      
    analysisSheet.createDataValidation("ContentDelivery", "K1", "K"+row, true);   
    FileOutputStream out;
    try {
      out = new FileOutputStream("ComboboxLovContentDeliveryInfo.xlsx");
      XLSXCreator.create(myBook, out);
      out.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  


  protected void processFile(File fName, boolean bDoADE) {
  //  System.out.println("Parsing " + fName.getAbsolutePath());
      try {
        XMLDocument doc = XMLParserHelper.getXMLDocument(fName.getAbsolutePath());
        if(doc == null)
            return;
        for (String type : l_type) {
          
          NodeList nl1 = doc.getElementsByTagName(type);            
          if(nl1 == null || nl1.getLength() < 1) 
            continue;
          
          for (int i = 0; i < nl1.getLength(); i++) {

            Node n = nl1.item(i);
           /* 
            //ignore mdvt graph and dyanmic tables
            if (n.getNodeName().startsWith("mdvt") ||
                n.getNodeName().startsWith("dynamic"))
              return;

            */
            String description = "Set ContentDelivery in New ContentDelivery Value column.";
            String newContentValue = "immediate";

            NamedNodeMap attrs = n.getAttributes();
            if (attrs == null)
              return;
            Node item = attrs.getNamedItem("contentDelivery");
            String sContentDelivery = null;
            if (item != null) 
              sContentDelivery = item.getNodeValue();
            else 
              sContentDelivery = "default";
            
        /*
            //lazy is fine for panelCard
            if (type.equals("panelCard") && 
                (sContentDelivery.equals("default") ||
                 sContentDelivery.equals("lazy")))
              continue;
            
            //attachment and menu's default behavior is immediate, which is good. 
            if ((type.equals("attachment") || type.equals("menu")) && sContentDelivery.equals("default") )
              continue;

            if (!type.equals("attachment") && !type.equals("menu") &&
                sContentDelivery.equals("immediate"))
              continue;
        

            if (type.equals("attachment") || type.equals("menu")) {
                description = "Should make use of the default value (immediate)";
                newContentValue = "default";
            } else if (type.equals("panelCard")) {
                description = "Should make use of the default value (lazy)";
                newContentValue = "default";
            }
        */
            item = attrs.getNamedItem("id");
            String sId = item.getNodeValue();
            
            if(!isExempt(sId,FamilyModuleHelper.getPathAfterViewRoot(fName.getAbsolutePath()))) {
                System.out.println("Violation found in file: " + fName.getName());
                insert(fName, sId, type, sContentDelivery, description);
                writeToExcel(fName, sId,type, sContentDelivery, description, newContentValue);
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        System.out.println("Skipping...");
      }
  }


  protected String getSummaryReport() {
    return null;
  }

  protected String getSummaryReportSubject() {
    return null;
  }

  private void writeToExcel(File f,String sId, String type, String sContentDelivery,
                            String description, String newContentValue) throws Exception{
    String fileNameInfo = FamilyModuleHelper.getFileNameInfo1(f.getCanonicalPath());
    String[] parts = fileNameInfo.split(",");
    row++;
    String cellNumber = "A" + row;
    analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(parts[4].trim())));

    cellNumber = "B" + row;
    analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(parts[5].trim())));

    cellNumber = "C" + row;
    analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(parts[0].trim())));

    cellNumber = "D" + row;
    analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(parts[1].trim())));

    cellNumber = "E" + row;
    analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(parts[2].trim())));

    cellNumber = "F" + row;
    analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(parts[3].trim())));

    cellNumber = "G" + row;
    analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(sId)));

    cellNumber = "H" + row;
    analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(type)));

    cellNumber = "I" + row;
    analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(sContentDelivery)));

    cellNumber = "J" + row;
    analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(description)));
    
    cellNumber = "K" + row;
    analysisSheet.addCell(new Cell(new CellLocation(cellNumber),
                                     new CellValue(newContentValue)));
    
    if(writer != null){
        writer.write("\n\nFileName: " + FamilyModuleHelper.getPathAfterViewRoot(f.getCanonicalPath()));
        writer.write("\nComponentId: " + sId);
        writer.write("\nComponentType: " + type);
        writer.write("\nContentDelivery: " + sContentDelivery);
        writer.write("\nDescription: " + description);
    }
  }


  private static void dumpToAB(String series) throws SQLException{
    PreparedStatement delete =
      con.prepareStatement("DELETE FROM codescan_Results_p where issuetype = 'ContentDeliveryComboboxLov' and series='" + series + "'");
    delete.execute();   
    insert.executeBatch();
    insert.close();
    con.commit();
    con.close();  
  }
  
  private void insert(File f,String sId, String type, String sContentDelivery,
                            String subissues) throws Exception {
      
    String fileNameInfo = FamilyModuleHelper.getFileNameInfo1(f.getCanonicalPath());
    String[] parts = fileNameInfo.split(",");
    
    if (LRGUtil.isDeferred(releaseNum, parts[3].trim()))
        return;
    insert.setString(1, parts[4].trim());
    insert.setString(2, parts[5].trim());
    insert.setString(3, parts[0].trim());
    insert.setString(4, parts[1].trim());
    insert.setString(5, parts[2].trim());
    insert.setString(6, parts[3].trim());
    insert.setString(7, "ContentDeliveryComboboxLov");
    insert.setString(8, subissues);
    insert.setString(9,
                     type + " " + sId + " has ContentDelivery set to " +
                     sContentDelivery + ".");
    insert.addBatch();
  }
  
    public void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_comboboxLov.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 3)
            continue;
        String issue = parts[0].trim();
        if(!issue.equals("ContentDeliveryComboboxLov"))
            continue;
        String fileName = parts[1].trim();
        String id = parts[2].trim();
          
        HashSet<String> exemptLines = null;
        if(!exemptions.containsKey(fileName)) 
          exemptLines = new HashSet<String>();
        else 
          exemptLines = exemptions.get(fileName);
        
        exemptLines.add(id);
        exemptions.put(fileName, exemptLines);
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }
    
    private boolean isExempt(String id, String fileName){
        
        if(!exemptions.containsKey(fileName)) 
            return false;
        
        HashSet<String> componentIds = (HashSet<String>)exemptions.get(fileName);
        if(componentIds.contains(id))
            return true;
        
        return false;
    }
    //Su: Overriding Joe's base class method to test for defered path
    public void crawlDirectory(String path, boolean bDoADE)
    {
        if (path.contains(".ade_path") || path.contains("/classes/"))
            return;
        
        if(FamilyModuleHelper.isDeferredPath(releaseNum, FamilyModuleHelper.getPathAfterViewRoot(path)))
            return;        

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++)
        {
            if (listOfFiles[i].isFile())
            {
                String sName = listOfFiles[i].getAbsolutePath();
                // Zeesha: Added, because for files modified recently there is an .ade_path file created in that directory, that is neither node, nor dir. Skip this without raising error
                if (sName.contains(".ade_path"))
                    continue;

                if (fileOfInterest(sName))
                {
                    processFile(listOfFiles[i], bDoADE);
                }

            } else if (listOfFiles[i].isDirectory())
            {
                // Added by zeesha - no point going into the directory if we are not interested in it. Better performance
                if (isDirOfInterest(listOfFiles[i].getAbsolutePath()))
                    crawlDirectory(listOfFiles[i].getAbsolutePath(), bDoADE);
            } else
            {
                System.out.println("ERROR:  node is neither file or directory: " +
                                   listOfFiles[i]);
            }
        }

    }
}

