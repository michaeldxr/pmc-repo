package oracle.apps.UICodeScanner;


import au.com.bytecode.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.AdeDiffHelper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.XmlFixer;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class SelfPPRScanner extends JoesBaseClass {

    class ScanResult {
        private String family;
        private String module;
        private String product;
        private String filename;
        private String series;
        private String label;
        private String componentId;
        private String outcome1;
        private String outcome2;
        private Boolean autoSubmit;
        private Boolean valueChangeListener;
        private Boolean selfPPR;
        private String type;
        private String pattern;
        private String description = "";
        private Boolean uptakeFndConvertNumber;
        private Boolean removePPR;

        private String currencyCode;
        private String currencySymbol;
        private Boolean integerOnly = false;
        private String maxFractionDigits;
        private String maxIntegerDigits;
        private String minFractionDigits;
        private String minIntegerDigits;
        private Boolean groupingUsed = true;

        ScanResult() {
            super();
        }

        ScanResult(String fullPath, String componentId, Boolean autoSubmit,
                   Boolean valueChangeListener, Boolean selfPPR, String type,
                   String pattern) {

            try {
                String s = FamilyModuleHelper.getFileNameInfo1(fullPath);

                String[] ss = s.split(",");

                family = ss[0];
                module = ss[1];
                product = ss[2];
                filename = ss[3];
                series = ss[4];
                label = ss[5];
                this.componentId = componentId;
                this.autoSubmit = autoSubmit;
                this.valueChangeListener = valueChangeListener;
                this.selfPPR = selfPPR;
                this.type = type == null ? "" : type;
                this.pattern = pattern == null ? "" : pattern;

                uptakeFndConvertNumber = false;
                removePPR = false;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void analyze() {
            analyzeOutcome1();
            analyzeOutcome2();
        }

        private void analyzeOutcome1() {
            if (!pattern.isEmpty() &&
                (pattern.contains("#") && !pattern.startsWith("#{"))) {
                outcome1 = "Teams to review. Hard coded pattern.";
                return;
            }

            if (!type.isEmpty() && type.contains("#{")) {
                outcome1 = "Teams to review. Complicated EL.";
                return;
            }

            if (!pattern.isEmpty() &&
                pattern.contains("fnd:currencyPatternWithPrecisionAndSymbol")) {
                outcome1 = "Teams to review. Complicated EL.";
                return;
            }

            if ((!type.isEmpty() && type.equals("percent") &&
                 (!pattern.isEmpty() && pattern.startsWith("#{")))) {
                outcome1 = "Teams to review. Invalid pattern.";
                return;
            }

            if (type.isEmpty() &&
                (!pattern.isEmpty() && pattern.startsWith("#{fnd:currencyPattern"))) {
                outcome1 =
                        "Teams to review. Type and Pattern are inconsistent.";
                return;
            }

            if ((!pattern.isEmpty() &&
                 (!pattern.startsWith("#{applCorePrefs") &&
                  !pattern.startsWith("#{fnd:currencyPattern")))) {
                outcome1 = "Teams to review. Custom pattern.";
                return;
            }

            outcome1 = "Run automation script to uptake fnd:convertNumber.";
            uptakeFndConvertNumber = true;
        }

        private void analyzeOutcome2() {
            if (selfPPR) {
                outcome2 = "Remove PPR.";
                removePPR = true;
                return;
            }

            if (autoSubmit) {
                outcome2 = "Teams to review high traffic pages.";
            } else {
                outcome2 = "No action needed.";
            }

        }

        public void setFamily(String family) {
            this.family = family;
        }

        public String getFamily() {
            return family;
        }

        public void setModule(String module) {
            this.module = module;
        }

        public String getModule() {
            return module;
        }

        public void setProduct(String product) {
            this.product = product;
        }

        public String getProduct() {
            return product;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getFilename() {
            return filename;
        }

        public void setSeries(String series) {
            this.series = series;
        }

        public String getSeries() {
            return series;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public void setComponentId(String componentId) {
            this.componentId = componentId;
        }

        public String getComponentId() {
            return componentId;
        }

        public void setOutcome1(String outcome1) {
            this.outcome1 = outcome1;
        }

        public String getOutcome1() {
            return outcome1;
        }

        public void setOutcome2(String outcome2) {
            this.outcome2 = outcome2;
        }

        public String getOutcome2() {
            return outcome2;
        }

        public void setAutoSubmit(Boolean autoSubmit) {
            this.autoSubmit = autoSubmit;
        }

        public Boolean getAutoSubmit() {
            return autoSubmit;
        }

        public void setValueChangeListener(Boolean valueChangeListener) {
            this.valueChangeListener = valueChangeListener;
        }

        public Boolean getValueChangeListener() {
            return valueChangeListener;
        }

        public void setSelfPPR(Boolean selfPPR) {
            this.selfPPR = selfPPR;
        }

        public Boolean getSelfPPR() {
            return selfPPR;
        }

        public void setType(String Type) {
            this.type = Type;
        }

        public String getType() {
            return type;
        }

        public void setPattern(String Pattern) {
            this.pattern = Pattern;
        }

        public String getPattern() {
            return pattern;
        }

        public void setDescription(String Description) {
            this.description = Description;
        }

        public String getDescription() {
            return description;
        }

        public void setUptakeFndConvertNumber(Boolean uptakeFndConvertNumber) {
            this.uptakeFndConvertNumber = uptakeFndConvertNumber;
        }

        public String getUptakeFndConvertNumber() {
            return uptakeFndConvertNumber ? "Yes" : "No";
        }

        public void setRemovePPR(Boolean removePPR) {
            this.removePPR = removePPR;
        }

        public String getRemovePPR() {
            return removePPR ? "Yes" : "No";
        }

        public Boolean getUptakeFndConvertNumber1() {
            return uptakeFndConvertNumber;
        }

        public Boolean getRemovePPR1() {
            return removePPR;
        }

        public void setCurrencyCode(String currencyCode) {
            this.currencyCode = currencyCode == null ? "" : currencyCode;
        }

        public String getCurrencyCode() {
            return currencyCode;
        }

        public void setCurrencySymbol(String currencySymbol) {
            this.currencySymbol = currencySymbol == null ? "" : currencySymbol;
        }

        public String getCurrencySymbol() {
            return currencySymbol;
        }
        
        public void setIntegerOnly(String integerOnly) {
            String s = integerOnly == null ? "false" : integerOnly;
            setIntegerOnly(new Boolean(s));
        }

        public void setIntegerOnly(Boolean integerOnly) {
            this.integerOnly = integerOnly == null ? false : integerOnly;
        }

        public Boolean getIntegerOnly() {
            return integerOnly;
        }

        public void setMaxFractionDigits(String maxFractionDigits) {
            this.maxFractionDigits =
                    maxFractionDigits == null ? "" : maxFractionDigits;
        }

        public String getMaxFractionDigits() {
            return maxFractionDigits;
        }

        public void setMaxIntegerDigits(String maxIntegerDigits) {
            this.maxIntegerDigits =
                    maxIntegerDigits == null ? "" : maxIntegerDigits;
        }

        public String getMaxIntegerDigits() {
            return maxIntegerDigits;
        }

        public void setMinFractionDigits(String minFractionDigits) {
            this.minFractionDigits =
                    minFractionDigits == null ? "" : minFractionDigits;
        }

        public String getMinFractionDigits() {
            return minFractionDigits;
        }

        public void setMinIntegerDigits(String minIntegerDigits) {
            this.minIntegerDigits =
                    minIntegerDigits == null ? "" : minIntegerDigits;
        }

        public String getMinIntegerDigits() {
            return minIntegerDigits;
        }
        
        public void setGroupingUsed(String groupingUsed) {
            String s = groupingUsed == null ? "true" : groupingUsed;
            setGroupingUsed(new Boolean(s));
        }

        public void setGroupingUsed(Boolean groupingUsed) {
            this.groupingUsed = groupingUsed == null ? true : groupingUsed;
        }

        public Boolean getGroupingUsed() {
            return groupingUsed;
        }
    }

    private static String sCrawlDir;
    private static String mode = "view"; //can be view/LRG/premerge
    private static BufferedWriter writer1;
    private static BufferedWriter writer2;
    public static HashMap<String, HashSet<String>> exemption_fndConvertNumber =
        new HashMap<String, HashSet<String>>();
    public static ArrayList<ScanResult> al_results =
        new ArrayList<ScanResult>();
    static int count = 0;
    static int count1 = 0;
    static int releaseNum = 0;

    public SelfPPRScanner() {
        super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF);
    }

    public static void main(String[] args) {
        SelfPPRScanner scanner = new SelfPPRScanner();

        mode = System.getProperty("mode");
        if (mode == null)
            mode = "view";
        System.out.println("Operation mode is: " + mode);

        //    writer1 = new BufferedWriter(new FileWriter("UnusedAutoSubmit.csv"));
        //    writer1.write("Family,Module,Product,Filename,Series,Label," +
        //                  "ComponentName, ComponentId, Description\n");


        if (mode.equals("view")) {

            if (args.length < 1 || args[0] == null ||
                args[0].trim().equals("")) {
                System.out.println("Expected 1 parameter, but got " +
                                   args.length);
                System.exit(1);
            }
            sCrawlDir = args[0].trim();
//                        sCrawlDir =
//                                "/scratch/yaoyao/view_storage/yaoyao_intfin/fusionapps/fin/components";


            File f = new File(sCrawlDir);
            if (!f.exists()) {
                System.out.println("Crawl directory does not exist - " +
                                   sCrawlDir);
                System.exit(1);
            }

            generateExemptionList();


            System.out.println("Analyzing all files in directory: " +
                               sCrawlDir);
            scanner.crawlDirectory(sCrawlDir, false);
            System.out.println("Done crawlying dir for " + sCrawlDir);
            System.out.println(count + " violations found in total.");
            System.out.println("Dumping scan results to xls...");
            writeExcel(al_results, null);
            System.out.println("Please review fndConvertNumber.xlsx for details. ");
        }
        if (mode.equals("LRG")) {
            if (args.length < 1 || args[0] == null ||
                args[0].trim().equals("")) {
                System.out.println("USAGE: runFndConvertNumber.sh <series>");
                System.exit(1);
            }
            
            Date start = new Date();

            String series = args[0];
            //String series = "FUSIONAPPS_PT.INTFIN_LINUX.X64";

            generateExemptionList();

            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running fndConvertNumber Scan on series: " +
                               series + ".......");

            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
            for (int j = 0; j < families.size(); j++) {
                String family = families.get(j);
                String sCrawlDir =
                    LRGUtil.getCrawlDir(series, family, label, "UptakeFndConvertNumber");
                System.out.println("Crawling dir '" + sCrawlDir +
                                   "' for family: " + family);
                scanner.crawlDirectory(sCrawlDir, false);
                System.out.println("Done crawling for family " + family + ".");
            }


            try {
                System.out.println("Uploading scan results to DB....");
                scanner.uploadToDB(al_results, series);
                System.out.println("Done uploading scan results to DB.");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ERROR: Failed to upload scan results to DB.");
            }
            FamilyModuleHelper. printScanDuration(start,"UptakeFndConvertNumber");

        } else if (mode.equals("premerge")) {

            //String[] files = args[0].trim().split("\n");
            String ade_view_root = args[1].trim();
            String filelistpath=args[0].trim();
            String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root); 
            String label = args[2].trim();

            String release = FamilyModuleHelper.getRelease(label);
            System.out.println("Release: " + release);

            try {
                releaseNum = Integer.parseInt(release);
                if (releaseNum <
                    13) //This check was mandated post release 10 only, hence this check is necessary to avoid the current rel9 branches from failing
                    return;
            } catch (NumberFormatException e) {
                System.out.println("Could not determine release. Skipping FndConvertNumber check...");
                return;
            }

            generateExemptionList();

            for (int i = 0; i < files.length; i++) {
                String filePath = ade_view_root + "/" + files[i].trim();
                File f = new File(filePath);
                if (!f.exists())
                    continue;
                if (!(filePath.endsWith(".jsff") || filePath.endsWith(".jspx")))
                    continue;
                System.out.println("Processing " + f.getAbsoluteFile());
                scanner.processFile(f, false);
            }
            scanner.writeResultsToTextFile();

        } else if (mode.equals("fix")) {

            if (args.length != 2) {
                System.out.println("Expected 2 parameters, but got " +
                                   (args.length) + ".");
                System.out.println("USAGE: fndConvertNumber_fix.sh <INPUT_PATH>");
                System.exit(1);
            }

            adeRoot = args[0];
            if (adeRoot == null || adeRoot.isEmpty()) {
                System.out.println("$ADE_VIEW_ROOT is not set. Please make sure you are in an ADE view.");
                System.exit(1);
            }

            String input = args[1];
//                        adeRoot = "/ade/yaoyao_intfin";
//            
//                        String input =
//                            "/home/yaoyao/fndConvertNumber.csv";
            if (input == null || input.isEmpty()) {
                System.out.println("Please provide abosolute file path of the csv.");
                System.exit(1);
            } else if (!input.endsWith(".csv")) {
                System.out.println("Please save the spreadsheet in csv format.");
                System.exit(1);
            } else {
                File f = new File(input);
                if (!f.isFile()) {
                    System.out.println(input + " is not a file.");
                    System.exit(1);
                }
            }

            SelfPPRScanner fixer = new SelfPPRScanner();


            try {
                System.out.println("Reading the input csv file...");
                fixer.readCSV(input);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("ERROR: Failed to read and parse the csv file.");
                System.exit(1);
            }

            try {
                fixer.fixAll(al_results, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //    writer1.close();
        //    writer2.close();
    }

    protected void processFile(File fName, boolean bDoADE) {

        String fileName = fName.getAbsolutePath();

        if (!fileName.endsWith(".jsff") && !fileName.endsWith(".jspx"))
            return;
        
        if (mode == null || mode.equals("view")){
            System.out.println("Processing: " + fName);
        }

        try {

            XMLDocument doc = XMLParserHelper.getXMLDocument(fileName, false);
            if (doc == null)
                return;
            NodeList adfc = doc.getElementsByTagName("jsp:root");
            if (adfc == null) {
                System.out.println("REVIEW:  Not a page file ---> " +
                                   fileName);
                return;
            }


            NodeList list = doc.getElementsByTagName("inputText");

            for (int i = 0; i < list.getLength(); i++) {

                Node child = list.item(i);
                String type = null;
                String pattern = null;
                String currencyCode = null;
                String currencySymbol = null;
                String integerOnly = null;
                String maxFractionDigits = null;
                String maxIntegerDigits = null;
                String minFractionDigits = null;
                String minIntegerDigits = null;
                String groupingUsed = null;


                //do ppr?
                Boolean ppr = false;
                String autoSubmit =
                    XMLParserHelper.getAttributeValue(child, "autoSubmit");
                if (autoSubmit != null && autoSubmit.equals("true")) {
                    ppr = true;
                }

                //is af:convertNumber present?
                ArrayList<Node> al =
                    XMLParserHelper.getChildNodesWithName(child,
                                                          "af:convertNumber");
                if (al == null || al.size() == 0) {
                    continue;
                } else {
                    type =
XMLParserHelper.getAttributeValue(al.get(0), "type");
                    pattern =
                            XMLParserHelper.getAttributeValue(al.get(0), "pattern");
                    currencyCode =
                            XMLParserHelper.getAttributeValue(al.get(0), "currencyCode");
                    currencySymbol =
                            XMLParserHelper.getAttributeValue(al.get(0),
                                                              "currencySymbol");
                    integerOnly =
                            XMLParserHelper.getAttributeValue(al.get(0), "integerOnly");
                    maxFractionDigits =
                            XMLParserHelper.getAttributeValue(al.get(0),
                                                              "maxFractionDigits");
                    maxIntegerDigits =
                            XMLParserHelper.getAttributeValue(al.get(0),
                                                              "maxIntegerDigits");
                    minFractionDigits =
                            XMLParserHelper.getAttributeValue(al.get(0),
                                                              "minFractionDigits");
                    minIntegerDigits =
                            XMLParserHelper.getAttributeValue(al.get(0),
                                                              "minIntegerDigits");
                    groupingUsed =
                            XMLParserHelper.getAttributeValue(al.get(0), "groupingUsed");
                }

                //valueChangeListener
                Boolean lisenterExists = false;
                String valueChangeListener =
                    XMLParserHelper.getAttributeValue(child,
                                                      "valueChangeListener");
                if (valueChangeListener != null) {
                    lisenterExists = true;
                }


                //self ppr?
                boolean selfPpr = false;
                String id = XMLParserHelper.getAttributeValue(child, "id");

                if (ppr && !lisenterExists) {
                    String partialTriggers =
                        XMLParserHelper.getAttributeValue(child,
                                                          "partialTriggers");
                    if (partialTriggers != null) {

                        if (id != null) {
                            String[] parts = partialTriggers.split(" ");
                            for (int j = 0; j < parts.length; j++) {
                                String partialTrigId = parts[j].trim();
                                if (partialTrigId.equals(id)) {
                                    selfPpr = true;
                                }
                            }
                        }
                    }
                }

                if (isExempted(fileName, id)) {
                    System.out.println("Exempton:" + fileName + ":" + id);
                } else {
                    ScanResult r =
                        new ScanResult(fileName, id, ppr, lisenterExists,
                                       selfPpr, type, pattern);
                    r.setCurrencyCode(currencyCode);
                    r.setCurrencySymbol(currencySymbol);
                    r.setIntegerOnly(integerOnly);
                    r.setMaxFractionDigits(maxFractionDigits);
                    r.setMaxIntegerDigits(maxIntegerDigits);
                    r.setMinFractionDigits(minFractionDigits);
                    r.setMinIntegerDigits(minIntegerDigits);
                    r.setGroupingUsed(groupingUsed);
                    r.analyze();
                    al_results.add(r);
                    count++;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return null;
    }


    private static void generateExemptionList() {

        try {
            //regular exemptions
            BufferedReader fileReader =
                new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
                                                  "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_Yao.txt"));
            String line = null;

            while ((line = fileReader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 3)
                    continue;
                String issue = parts[0].trim();
                if (issue.equals("UptakeFndConvertNumber")) {
                    String fileName = parts[1].trim();
                    String id = parts[2].trim();

                    int index = fileName.indexOf("oracle/");

                    if (index != -1)
                        fileName = fileName.substring(index);

                    HashSet<String> exemptLines = null;
                    if (!exemption_fndConvertNumber.containsKey(fileName))
                        exemptLines = new HashSet<String>();
                    else
                        exemptLines = exemption_fndConvertNumber.get(fileName);

                    exemptLines.add(id);
                    exemption_fndConvertNumber.put(fileName, exemptLines);
                }
            }
        } catch (Exception e) {
            System.out.println("Skipping exemptions as file could not be found: " +
                               e.getMessage());
        }
    }


    private static boolean isExempted(String page, String id) {

        int index = page.indexOf("oracle/");

        if (index != -1)
            page = page.substring(index);

        HashSet<String> s = exemption_fndConvertNumber.get(page);
        if (s == null)
            return false;
        else if (s.contains("ALL")) {
            return true;
        } else {
            return s.contains(id);
        }
    }
    
    private void writeResultsToTextFile() {
        BufferedWriter br = null;
        try{
             br = new BufferedWriter(new FileWriter("UptakeFndConvertNumber.txt"));
            for (ScanResult r : al_results) {
                br.write("\nFileName: " + r.getFilename());
                br.write("\nSub-Issue: " + r.getOutcome1());
                br.write("\nDescription: Found af:convertNumber for InputText " + r.getComponentId() + "\n\n");
            }
            br.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/ClientSideNumberFormatting " +
                                            "for description of the issue and resolution.\n\n");
            
        }catch(Exception e) {
            e.printStackTrace();
        }
        finally{
            try{
                if(br != null) br.close();
            } catch(IOException ex){
                ex.printStackTrace();
            }
        }
    }
    

    public static void writeExcel(ArrayList<ScanResult> al_results,
                                  String outputDir) {
        Workbook myBook = new Workbook("output");

        // Create a worksheet for LOVs
        Worksheet sheet = new Worksheet("Scan Results");
        myBook.addWorksheet(sheet);

        sheet.setColumnWidth(1, 25);
        sheet.addCell(new Cell(new CellLocation("A1"),
                               new CellValue("Family")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("B1"),
                               new CellValue("Module")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("C1"),
                               new CellValue("Product")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("D1"),
                               new CellValue("Filename")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("E1"),
                               new CellValue("Series")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("F1"),
                               new CellValue("Label")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("G1"),
                               new CellValue("ComponentId")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("H1"),
                               new CellValue("Outcome1")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("I1"),
                               new CellValue("Outcome2")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("J1"),
                               new CellValue("AutoSubmit")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("K1"),
                               new CellValue("ValueChangeListener")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("L1"),
                               new CellValue("Self-PPR")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("M1"), new CellValue("Type")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("N1"),
                               new CellValue("Pattern")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("O1"),
                               new CellValue("CurrencyCode")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("P1"),
                               new CellValue("CurrencySymbol")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("Q1"),
                               new CellValue("IntegerOnly")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("R1"),
                               new CellValue("MaxFractionDigits")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("S1"),
                               new CellValue("MaxIntegerDigits")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("T1"),
                               new CellValue("MinFractionDigits")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("U1"),
                               new CellValue("MinIntegerDigits")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("V1"),
                               new CellValue("GroupingUsed")));


        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("W1"),
                               new CellValue("Uptake fnd:convertNumber")));

        sheet.setColumnWidth(2, 25);
        sheet.addCell(new Cell(new CellLocation("X1"),
                               new CellValue("Remove PPR")));


        int row_lov = 1;
        for (ScanResult r : al_results) {
            row_lov++;
            String cellNumber = "A" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getFamily())));

            cellNumber = "B" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getModule())));

            cellNumber = "C" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getProduct())));

            cellNumber = "D" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getFilename())));

            cellNumber = "E" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getSeries())));

            cellNumber = "F" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getLabel())));

            cellNumber = "G" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getComponentId())));

            cellNumber = "H" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getOutcome1())));

            cellNumber = "I" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getOutcome2())));

            cellNumber = "J" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getAutoSubmit().toString())));

            cellNumber = "K" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getValueChangeListener().toString())));

            cellNumber = "L" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getSelfPPR().toString())));

            cellNumber = "M" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getType())));

            cellNumber = "N" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getPattern())));

            cellNumber = "O" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getCurrencyCode())));

            cellNumber = "P" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getCurrencySymbol())));

            cellNumber = "Q" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getIntegerOnly().toString())));

            cellNumber = "R" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getMaxFractionDigits())));

            cellNumber = "S" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getMaxIntegerDigits())));

            cellNumber = "T" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getMinFractionDigits())));

            cellNumber = "U" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getMinIntegerDigits())));

            cellNumber = "V" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getGroupingUsed().toString())));

            cellNumber = "W" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getUptakeFndConvertNumber())));

            cellNumber = "X" + row_lov;
            sheet.addCell(new Cell(new CellLocation(cellNumber),
                                   new CellValue(r.getRemovePPR())));

        }

        FileOutputStream out;
        String ourputName;
        if (outputDir == null || outputDir.isEmpty()) {
            ourputName = "fndConvertNumber.xlsx";
        } else if (outputDir.endsWith("/")) {
            ourputName = outputDir + "fndConvertNumber.xlsx";
        } else {
            ourputName = outputDir + "/fndConvertNumber.xlsx";
        }
        try {
            out = new FileOutputStream(ourputName);
            XLSXCreator.create(myBook, out);
            out.close();
            System.out.println("The scan result is dumped to: " + ourputName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static AdeDiffHelper diffHelper;
    static BufferedWriter fixWriter;
    static BufferedWriter errorWriter;
    static int count_success = 0;
    static int count_failure = 0;
    static String adeRoot = null;

    private void fixAll(ArrayList<ScanResult> al_violations,
                        String outputDir) throws Exception {

        System.out.println("Fixing all violations....");

        if (outputDir == null || outputDir.isEmpty()) {
            outputDir = "";
        } else if (!outputDir.endsWith("/")) {
            outputDir = outputDir + "/";
        }

        diffHelper = new AdeDiffHelper(outputDir + "diff.txt");
        errorWriter =
                new BufferedWriter(new FileWriter(outputDir + "errors.txt"));
        diffHelper.startDiffLog();
        fixWriter =
                new BufferedWriter(new FileWriter(outputDir + "CouldNotFix.txt"));
        for (ScanResult r : al_violations) {
            try {
                if (makeFixes(r))
                    count_success++;
                else
                    count_failure++;
            } catch (Exception e) {
                count_failure++;
                errorWriter.write(e.getMessage() + "\n");
                continue;
            }
        }
        diffHelper.closeDiffLog();
        errorWriter.close();
        fixWriter.close();

        System.out.println();
        System.out.println("********************Summary********************");
        System.out.println(count_success +
                           " violations have been fixed. Please review diff.txt to confirm the changes. ");
        System.out.println(count_failure +
                           " violations have not been fixed. Please review CouldNotFix.txt to review all the failed cases and fix them manually");
        System.out.println("NOTE: YOU HAVE TO DO ALL JUNIT TESTING AND RUNTIME TESTING BEFORE CHECKING IN YOUR CHANGES.");
        System.out.println("***********************************************");
    }

    private boolean makeFixes(ScanResult r) throws Exception {

        if (!r.uptakeFndConvertNumber) {
            fixWriter.write("SKIPPED: UptakeFndConvertNumber flag set to No. " +
                            r.getFilename() + ":" + r.getComponentId() + "\n");
            return false;
        } else {
            String fullPath = adeRoot + "/" + r.getFilename();
            File f = new File(fullPath);
            if (!f.isFile()) {
                fixWriter.write("ERROR: " + fullPath + " is not a file.\n");
                return false;
            }

            if (r.getOutcome1().equals("Teams to review. Complicated EL.")) {
                fixWriter.write("SKIPPED: Complicated EL. " + r.getFilename() +
                                ":" + r.getComponentId() + "\n");
                return false;
            } else if (r.getOutcome1().equals("Teams to review. Custom pattern.")) {
                fixWriter.write("SKIPPED: Custom pattern. " + r.getFilename() +
                                ":" + r.getComponentId() + "\n");
                return false;
            } else if (r.getOutcome1().equals("Teams to review. Hard coded pattern.")) {
                fixWriter.write("SKIPPED: Hard coded pattern. " +
                                r.getFilename() + ":" + r.getComponentId() +
                                "\n");
                return false;
            } else if (r.getOutcome1().equals("Teams to review. Invalid pattern.")) {
                fixWriter.write("SKIPPED: Invalid pattern. " +
                                r.getFilename() + ":" + r.getComponentId() +
                                "\n");
                return false;
            } else {
                boolean fixed = fixGeneric(fullPath, r);
                return fixed;
            }
        }
    }


    private boolean fixGeneric(String fullPath,
                               ScanResult r) throws Exception {
        XmlFixer xmlFixer = new XmlFixer(fullPath);

        XMLDocument doc = createDomParser(fullPath);

        Node n = findTargetInputText(fullPath, r, doc);

        if (n == null) {
            fixWriter.write("ERROR: InputText with id " + r.getComponentId() +
                            " doesn't exist.\n");
            return false;
        }

        ArrayList<Node> al =
            XMLParserHelper.getChildNodesWithName(n, "af:convertNumber");
        if (al == null || al.size() == 0) {
            fixWriter.write("SKIPPED: Already fixed." + r.getFilename() + ":" +
                            r.getComponentId() + "\n");
            return false;
        } else {


            Node converter = al.get(0);

            String newTag = constructNewTag(r);

            xmlFixer.replaceNode(converter, newTag);

            if (r.removePPR)
                xmlFixer.removeAttribute(n, "autoSubmit", "true");


            addNameSpace(doc, xmlFixer);

            String fileContentsAsString =
                xmlFixer.getFileContentsAsString(false); //this will get the changed file contents
            diffHelper.applyFix(fullPath, fileContentsAsString);
        }
        return true;
    }

    private void addNameSpace(XMLDocument doc,
                              XmlFixer xmlFixer) throws Exception {
        NodeList components = doc.getElementsByTagName("root");
        if (components == null || components.getLength() == 0) {
            throw new Exception("ERROR: Could not find jsp:root tag in DOM.");
        } else {
            Node root = components.item(0);
            String attrVal =
                XMLParserHelper.getAttributeValue(root, "xmlns:fnd");
            if (attrVal == null) {
                xmlFixer.addAttribute(root, "xmlns:fnd",
                                      "http://xmlns.oracle.com/apps/fnd/applcore");
            }
        }
    }

    private String constructNewTag(ScanResult r) {
        String ret = null;
        StringBuilder temp = new StringBuilder();
        String startTag = "<fnd:convertNumber ";
        String endTag = "/>";

        String pattern = r.getPattern();
        String newType = null;
        String newCurrencyCode = null;

        if (pattern == null || pattern.isEmpty()) {
            //keep using the old type;
            newType = r.getType();
        } else if (pattern.contains("applCorePrefs.numberFormatPattern")) {
            newType = "number";
        } else if (pattern.contains("applCorePrefs.integerFormatPattern")) {
            newType = "integer";
        } else if (pattern.contains("applCorePrefs.numericCodeFormatPattern")) {
            newType = "numericCode";
        } else {
            Pattern p =
                Pattern.compile("#\\{fnd:currencyPattern\\((.+)\\)\\}");
            Matcher m = p.matcher(pattern);

            if (m.matches()) {
                newType = "currency";
                newCurrencyCode = m.group(1);
            }
        }
        
        if(newType == null || newType.isEmpty() || newType.equals("number")){
            if(r.getIntegerOnly())
                newType = "integer";
        }
        
        if(newType != null && newType.equals("integer")){
            if(!r.getGroupingUsed())
                newType="numericNumber";
        }

        temp.append(startTag);

        temp.append(createAttributeStr("type", newType));
        temp.append(createAttributeStr("maxFractionDigits",
                                       r.getMaxFractionDigits()));
        temp.append(createAttributeStr("maxIntegerDigits",
                                       r.getMaxIntegerDigits()));
        temp.append(createAttributeStr("minFractionDigits",
                                       r.getMinFractionDigits()));
        temp.append(createAttributeStr("minIntegerDigits",
                                       r.getMinIntegerDigits()));

        if (r.getCurrencyCode() != null && !r.getCurrencyCode().isEmpty()) {
            temp.append(createAttributeStr("currencySignStyle", "code"));
        }

        if (newCurrencyCode != null)
            temp.append(createAttributeStr("currencyCode",
                                           "#{" + newCurrencyCode + "}"));

        temp.append(endTag);

        ret = temp.toString();

        return ret;
    }

    private String createAttributeStr(String attrName, String attrVal) {
        if (attrVal == null || attrVal.isEmpty() || attrName == null ||
            attrName.isEmpty())
            return "";

        return attrName + "=\"" + attrVal + "\" ";
    }

    private Node findTargetInputText(String fullPath, ScanResult r,
                                     XMLDocument doc) throws Exception {

        Node n = null;

        NodeList components = doc.getElementsByTagName("inputText");
        if (components == null || components.getLength() < 1) {
            fixWriter.write("ERROR: Could not fix: can't find any node with name <inputText> in file " +
                            fullPath + ". \n");
            return null;
        }

        for (int i = 0; i < components.getLength(); i++) {
            n = components.item(i);
            String id = XMLParserHelper.getAttributeValue(n, "id");
            if (id != null && id.equals(r.getComponentId()))
                break;
            else
                n = null;
        }

        return n;
    }

    private XMLDocument createDomParser(String fullPath) throws Exception {
        XMLDocument doc = null;

        doc = XMLParserHelper.getXMLDocument(fullPath);
        if (doc == null) {
            fixWriter.write("ERROR: Could not parse " +
                            fullPath.substring(fullPath.indexOf("oracle/")) +
                            " using DOM" + ". \n");
        }

        return doc;
    }

    private void readCSV(String input) throws Exception {
        CSVReader reader = null;

        try {
            reader = new CSVReader(new FileReader(input));
            String[] nextLine;

            //skip the header;
            reader.readNext();

            while ((nextLine = reader.readNext()) != null) {
                if (nextLine.length == 1 && nextLine[0].isEmpty()) {

                } else if (nextLine.length != 24) {
                    throw new Exception("Expected 24 columns in csv, but got " +
                                        nextLine.length);
                }

                ScanResult r = new ScanResult();
                r.setFilename(nextLine[3]);
                r.setComponentId(nextLine[6]);
                r.setOutcome1(nextLine[7]);
                r.setOutcome2(nextLine[8]);
                r.setAutoSubmit(new Boolean(nextLine[9]));
                r.setValueChangeListener(new Boolean(nextLine[10]));
                r.setSelfPPR(new Boolean(nextLine[11]));
                r.setType(nextLine[12]);
                r.setPattern(nextLine[13]);
                r.setCurrencyCode(nextLine[14]);
                r.setCurrencySymbol(nextLine[15]);
                r.setIntegerOnly(new Boolean(nextLine[16]));
                r.setMaxFractionDigits(nextLine[17]);
                r.setMaxIntegerDigits(nextLine[18]);
                r.setMinFractionDigits(nextLine[19]);
                r.setMinIntegerDigits(nextLine[20]);
                r.setGroupingUsed(new Boolean(nextLine[21]));

                String flag1 = nextLine[22];
                if (flag1 != null && !flag1.isEmpty() &&
                    flag1.equalsIgnoreCase("yes"))
                    r.setUptakeFndConvertNumber(true);
                else
                    r.setUptakeFndConvertNumber(false);

                String flag2 = nextLine[23];
                if (flag2 != null && !flag2.isEmpty() &&
                    flag2.equalsIgnoreCase("yes"))
                    r.setRemovePPR(true);
                else
                    r.setRemovePPR(false);

                al_results.add(r);
            }
        } catch (Exception e) {
            if (reader != null) {
                reader.close();
            }

            throw e;
        }
    }


    public void uploadToDB(ArrayList<ScanResult> al_results,
                           String series) throws Exception {

        Connection con = null;
        Statement stmt = null;
        PreparedStatement cstmt = null;

        try {
            String statsDB =
                "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
            String statsUname = "codescan";
            String statsPwd = "codescan";

            //Class.forName("oracle.jdbc.driver.OracleDriver");
            con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
            con.setAutoCommit(false);

            cstmt =
con.prepareStatement("INSERT INTO CODESCAN_RESULTS(FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION,LABEL,SERIES) " +
                     "VALUES(?,?,?,?,?,?,?,?,?)");

            System.out.println("First delete records from DB...");
            stmt = con.createStatement();
            String sql =
                "DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'UptakeFndConvertNumber' and SERIES = '" +
                series + "'";

            stmt.executeUpdate(sql);

            System.out.println("Now inserting values...");
            int count = 0;

            for (ScanResult r : al_results) {

                cstmt.setString(1, r.getFamily()); //family
                cstmt.setString(2, r.getModule()); //module
                cstmt.setString(3, r.getProduct()); //product
                cstmt.setString(4, r.getFilename()); //filename
                cstmt.setString(8, r.getLabel()); //label
                cstmt.setString(9, r.getSeries()); //series
                cstmt.setString(5, "UptakeFndConvertNumber"); //issue

                cstmt.setString(6, r.getOutcome1()); //subissue
                cstmt.setString(7,
                                "Found af:convertNumber for InputText " + r.getComponentId() +
                                "."); //Description
                cstmt.addBatch();
                count++;

                if (count % 50 == 0) {
                    cstmt.executeBatch();
                }
            }
            
            if(count % 50 != 0){
                cstmt.executeBatch();
            }

            con.commit();
            cstmt.close();
            System.out.println("Total count for UptakeFndConvertNumber is " +
                               count + " on series " + series);
        } catch (Exception e) {
            e.printStackTrace();
            if (con != null)
                con.rollback();
            throw e;
        } finally {
            if(stmt != null)
                stmt.close();
            if(cstmt != null)
                cstmt.close();
            if (con != null)
                con.close();
        }
    }


}
