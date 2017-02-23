package oracle.apps.lovWidth;

import java.io.File;

import java.io.FileWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import oracle.apps.helpers.JprHelper;
import oracle.apps.votuning.DataBindings;
import oracle.apps.votuning.UiFileHelper;

public class ComboboxWidthScanner {
    /*
   * Number of jsff files processed
   */
    static int m_jsffCount = 0;

    /**
     * number of jspx files processed
     */
    static int m_jspxCount = 0;

    static boolean startFromModel = false;

    static int comboboxNotRenderedCount = 0;
    static boolean m_bDebug = false;
    static int m_numberOfJprsScanned = 0;

    HashMap<String, ProducerLOV> m_listProderVOs = new HashMap<String, ProducerLOV>();
    ArrayList<ConsumerVO> m_consumers = new ArrayList<ConsumerVO>();
    String m_family = "fin";
    // Scan type can be expert/eo/vooverride/all
    String m_scanType = "all";
    static String m_baseOutFileName = "producer.csv";
    HashMap<String, Integer> producersByProduct = new HashMap<String, Integer>();
    private static boolean m_dumpDb = false;


    static boolean writeErrors = true;

    public void printStatsOfProducersPerProduct(HashMap<String, Integer> counts) {
        //Print stats
        Set<String> productSet = counts.keySet();
        Iterator<String> psetiter = productSet.iterator();
        while (psetiter.hasNext()) {
            String p = psetiter.next();
            System.out.println(String.format("\n Product: %s , Number of producers: %d", p, counts.get(p)));
        }
        System.out.println("Number of combobox's not rendered: " + comboboxNotRenderedCount);
    }


    /**
     * Map of all the databindings indexed by the adfmsrc directory.
     * This is used to speed up lookup time. Instead of trying to find, parse the cpx file for each ui file, this  map holds the cpx object indexed in the map by the adfmsrc directory.
     * if there is an adfmsrc directory as the key in this map, the bindings object is retrieved from here. Else for the ui file, attempt to find the cpx file in thea dfmsrc directory. once found it is put in the map.
     */
    private static HashMap<String, DataBindings> bindingsMap = new HashMap<String, DataBindings>();


    /**
     * Inputs:
     * 1. Family : <familyName or all>
     * 2. ScanType: expert/eobased/vooverride/all
     * 3. -DdumpDB=true
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ComboboxWidthScanner comboboxWidthScanner = new ComboboxWidthScanner();

        // Make sure it is run with correct number of arguments
        // Usage:  comboboxWidthScanner.sh <path to your code in an open ade view> <family> <scantype = expert/eobased>
        if (args.length != 3 || isEmpty(args[0]) || isEmpty(args[1]) || isEmpty(args[2])) {
            System.out.println("Usage:  combobxScanner.sh <path to your code in an open ade view> ");
            System.exit(1);
        }


        // Command liine processing
        String sysprops = setSystemProperties();
        System.out.println(" ==> Running with system properties : " + sysprops);
        String sCrawlDir = args[0];
        comboboxWidthScanner.m_scanType = args[2];
        comboboxWidthScanner.m_family = args[1];
        Date startDate = new Date();

        // get arguments
        System.out.println(String.format("\n\n ============================================================"));
        System.out.println(String.format("  ScanDirectory=%s \n Family=%s \n ScanType=%s", sCrawlDir, args[1], args[2]));
        System.out.println(String.format(" ============================================================\n\n"));

        // startFromModel  - only look at lovs defined on vo attributes
        try {
            if (startFromModel) {
                comboboxWidthScanner.modelCrawler(sCrawlDir);
            } else {
                comboboxWidthScanner.crawlForUIFiles(sCrawlDir);
            }
        } catch (Exception ex) {
            System.out.println(String.format("Exception occurred while scanning directory:%s , Exception: %s ", sCrawlDir,
                                             ex.getMessage()));
        } finally {
            if (startFromModel) {
                comboboxWidthScanner.writeListConsumersToSpreadsheet(startFromModel);
            } else if (comboboxWidthScanner.getScanType() == ModifiedComboboxParser.ScanType.RECORD_ALL_CONSUMERS) {
                comboboxWidthScanner.writeListConsumersToSpreadsheet(startFromModel);
            } else {
                comboboxWidthScanner.writeProducerMapToSpreadsheet();
            }
        }

        System.out.println("Number of jprs looked at:" + m_numberOfJprsScanned);
        comboboxWidthScanner.printStatsOfProducersPerProduct(comboboxWidthScanner.producersByProduct);
        Date endDate = new Date();
        long milsecRun = endDate.getTime() - startDate.getTime();
        System.out.println("total elapsed time in sec " + milsecRun / 1000);


    }

    private void modelCrawler(String path) {
        if (path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (absFilePath.endsWith(".jpr") && !absFilePath.contains("/classes/") &&
                    !absFilePath.contains("dbSchema.jpr") && !absFilePath.contains("Test.jpr")) {
                    if (JprHelper.isModelProject(absFilePath)) {
                        m_numberOfJprsScanned++;
                        analyseJprFile(listOfFiles[i]);
                    }

                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidModelDirectory(dir)) {
                    modelCrawler(dir);
                }
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    /**
     * recurse over the crawl directory and for each valid ui file - process it.
     * Valid directories -
     * - should not contain, /dbSchema/ or /model/ or /publicModel/ or /test/ or /classes/
     * Valid UI files
     * - jspx or jsff extensions
     * - and not in folder with /classes/ or /test/ or /model/ or /publicModel/
     * @param path
     * @param bDoADE
     */
    private void crawlForUIFiles(String path) {
        if (path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (absFilePath.endsWith(".jpr") && !absFilePath.contains("/classes/") && !absFilePath.contains("/noship/") &&
                    !absFilePath.contains("dbSchema.jpr") && !absFilePath.contains("Test.jpr")) {
                    m_numberOfJprsScanned++;
                    analyseUiFileJpr(listOfFiles[i]);
                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidDirectory(dir)) {
                    crawlForUIFiles(dir);
                }
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }


    /**
     * Process each valid ui file. If the file has any found violations, fix them and increment the violation count
     * @param file
     * @param bDoADE
     */
    public void analyseJprFile(File file) {
        String uiFilePath = file.getAbsolutePath();
        if (m_bDebug) {
            System.out.println("Processing file: " + uiFilePath);
        }

        ModifiedComboboxParser comboProc = null;
        comboProc = new ModifiedComboboxParser(m_family, getScanType());
        comboProc.setScanType(getScanType());
        comboProc.analyzeJpr(file);
        m_consumers.addAll(comboProc.getList_consumers());
        //HashMap<String, ProducerLOV> jprProducers = comboProc.getListProderVOs();
        //addToProducerMap(jprProducers);

    }

    public ModifiedComboboxParser.ScanType getScanType() {
        if (m_scanType.equals("expert")) {
            return ModifiedComboboxParser.ScanType.EXPERT_VO;
        } else if (m_scanType.equals("eobased")) {
            return ModifiedComboboxParser.ScanType.EOBASED;
        } else if (m_scanType.equals("vooverride")) {
            return ModifiedComboboxParser.ScanType.VOOVERRIDE;
        } else if (m_scanType.equals("eoexpert")) {
            return ModifiedComboboxParser.ScanType.EXPERT_AND_EOBASED;
        } else if (m_scanType.equals("cummulative")) {
            return ModifiedComboboxParser.ScanType.RECORD_ALL_CONSUMERS;
        } else {
            return ModifiedComboboxParser.ScanType.ALL;
        }

    }


    public void addToProducerMap(HashMap<String, ProducerLOV> jprProducers) {
        Set<String> keySet = jprProducers.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();

            if (m_listProderVOs.containsKey(key)) {
                ProducerLOV obtProducer = m_listProderVOs.get(key);
                obtProducer.getConsumers().addAll(jprProducers.get(key).getConsumers());
                m_listProderVOs.put(key, obtProducer);
            } else {
                m_listProderVOs.put(key, jprProducers.get(key));
            }
        }

    }


    /**
     * Process each valid ui file.
     * @param file
     * @param bDoADE
     */
    public void analyseUiFileJpr(File file) {
        String jprPath = file.getAbsolutePath();
        if (m_bDebug) {
            System.out.println("\n\n\n ==== Processing ui Jpr file: " + jprPath);
        }
        HashSet<String> uiFiles = JprHelper.getListOfAllUIFilesInJpr(jprPath);
        if (uiFiles == null || uiFiles.size() <= 0)
            return;
        Iterator<String> uiFileIter = uiFiles.iterator();
        DataBindings databindings = UiFileHelper.getDataBindingsFileForJpr(jprPath);
        if (databindings == null) {
            System.out.println(" ----- Cannot obtain databindings for jpr: " + jprPath);
            return;
        }
        while (uiFileIter.hasNext()) {
            String uiFilePath = uiFileIter.next();
            File uiFile = new File(uiFilePath);
            if (!uiFile.exists())
                return;
            UIComboProcessor comboProc = new UIComboProcessor(m_family, getScanType());


            comboProc.analyzeUIFile(uiFile, databindings, jprPath);
            if (getScanType() == ModifiedComboboxParser.ScanType.RECORD_ALL_CONSUMERS) {
                m_consumers.addAll(comboProc.getList_consumers());
                int notRenderedCnt = comboProc.getComboboxNotRenderedCnt();
                comboboxNotRenderedCount = comboboxNotRenderedCount + notRenderedCnt;
            } else {
                addToProducerMap(comboProc.getListProderVOs());
            }
        }


    }


    public DataBindings getDataBindingsCpx(String uiFilePath) {
        // databindings.cpx is in adfm
        String baseDir = UiFileHelper.getAdfmsrcPath(uiFilePath);
        if (isEmpty(baseDir)) {
            if (m_bDebug) {
                String msg =
                    String.format("Could not obtain base directory for UI file: %s. This is required to find a databindings.cpx. ",
                                  uiFilePath);
                System.out.println(msg);
            }
            return null;
        }

        DataBindings bindings = null;
        if (bindingsMap.containsKey(baseDir)) {
            bindings = bindingsMap.get(baseDir);
        } else {
            bindings = UiFileHelper.getDataBindingsFileForUiFile(uiFilePath);
            if (bindings != null) {
                bindingsMap.put(baseDir, bindings);
            } else {
                if (m_bDebug) {
                    System.out.println("[ERROR] Bindings was null.. skipping file");
                }
                return null;
            }
        }
        return bindings;
    }


    public void writeProducerMap() {
        //FileWriter prdocuerMapWriter = null;
        String fileName = m_baseOutFileName;

        HashMap<String, Writer> productWriters = new HashMap<String, Writer>();

        try {
            if (m_scanType.trim().equals("expert")) {
                fileName = "expert_" + fileName;
            } else if (m_scanType.trim().equals("eobased")) {
                fileName = "eobased_" + fileName;
            }
            Writer consolidatedWriter = new FileWriter("all_" + fileName);
            consolidatedWriter.write("Family,Product,ProducerVOName,AttributeName,AttributeWidth,ProducerVOPackage\n");
            Collection<ProducerLOV> collProducers = m_listProderVOs.values();
            Iterator<ProducerLOV> producerIter = collProducers.iterator();
            while (producerIter.hasNext()) {
                ProducerLOV prod = producerIter.next();
                String product = prod.getProduct();
                Writer writer = null;
                if (!productWriters.containsKey(product)) {
                    String productfileName = product + "_" + fileName;
                    FileWriter fwriter = new FileWriter(productfileName);
                    fwriter.write("Family,Product,ProducerVOName,AttributeName,AttributeWidth,ProducerVOPackage\n");
                    productWriters.put(product, fwriter);

                }
                writer = productWriters.get(product);
                if (producersByProduct.containsKey(product)) {
                    Integer cnt = producersByProduct.get(product);
                    cnt++;
                    producersByProduct.put(product, cnt);
                } else {
                    producersByProduct.put(product, 1);
                }
                String writeStr =
                    prod.getFamily() + "," + prod.getProduct() + "," + prod.getBcName() + "," + prod.getDisplayAttr() + "," +
                    prod.getDisplayWidth() + "," + prod.getBcPackage() + "\n";
                consolidatedWriter.write(writeStr);
                writer.write(writeStr);
            }

            // close all writers
            Collection<Writer> writers = productWriters.values();
            Iterator<Writer> writerIter = writers.iterator();
            while (writerIter.hasNext()) {
                writerIter.next().close();
            }
            consolidatedWriter.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void writeListConsumersToSpreadsheet(boolean startModel) {
        ConsumerWriter writer = null;
        if (startModel) {
            writer = new ConsumerWriter("consumeroutput_startModel.xlsx", startModel);
        } else {
            writer = new ConsumerWriter("consumeroutput.xlsx", startModel);
        }
        Iterator<ConsumerVO> consIter = m_consumers.iterator();
        while (consIter.hasNext()) {
            ConsumerVO consumer = consIter.next();
            writer.writeConsumerAggregation(consumer, startModel);
            writer.writeConsumerDetails(consumer, startModel);
        }
        try {
            writer.writeSpreadsheetToDisk();
        } catch (Exception ex) {
            System.out.println("Exception while writing to disk");
            ex.printStackTrace();

        }
    }

    public void writeProducerMapToSpreadsheet() {
        //FileWriter prdocuerMapWriter = null;
        String fileName = "output.xlsx";

        HashMap<String, ComboboxOutputWriter> productWriters = new HashMap<String, ComboboxOutputWriter>();

        try {
            if (m_scanType.trim().equals("expert")) {
                fileName = "expert_" + fileName;
            } else if (m_scanType.trim().equals("eobased")) {
                fileName = "eobased_" + fileName;
            }
            //Writer consolidatedWriter = new FileWriter("all_" + fileName);
            //consolidatedWriter.write("Family,Product,ProducerVOName,AttributeName,AttributeWidth,ProducerVOPackage\n");
            Collection<ProducerLOV> collProducers = m_listProderVOs.values();
            Iterator<ProducerLOV> producerIter = collProducers.iterator();
            ComboboxOutputWriter allOutputWriter = new ComboboxOutputWriter("all_" + fileName);
            while (producerIter.hasNext()) {
                ProducerLOV prod = producerIter.next();
                String product = prod.getProduct();
                //Writer writer = null;
                if (!productWriters.containsKey(product)) {
                    String productfileName = product + "_" + fileName;
                    ComboboxOutputWriter writer = new ComboboxOutputWriter(productfileName);
                    // FileWriter fwriter = new FileWriter(productfileName);
                    // fwriter.write("Family,Product,ProducerVOName,AttributeName,AttributeWidth,ProducerVOPackage\n");
                    productWriters.put(product, writer);

                }
                ComboboxOutputWriter outputWriter = productWriters.get(product);
                if (producersByProduct.containsKey(product)) {
                    Integer cnt = producersByProduct.get(product);
                    cnt++;
                    producersByProduct.put(product, cnt);
                } else {
                    producersByProduct.put(product, 1);
                }
                outputWriter.writeProducer(prod);
                allOutputWriter.writeProducer(prod);
                ArrayList<ConsumerVO> consumerList = prod.getConsumers();

                if (consumerList != null && consumerList.size() > 0) {
                    int listSize = consumerList.size();
                    for (int i = 0; i < listSize; i++) {
                        ConsumerVO cons = consumerList.get(i);
                        outputWriter.writeConsumer(cons, prod.bcPackage, prod.displayAttr);
                        allOutputWriter.writeConsumer(cons, prod.bcPackage, prod.displayAttr);
                    }

                }
            }

            // close all writers
            Collection<ComboboxOutputWriter> writers = productWriters.values();
            Iterator<ComboboxOutputWriter> writerIter = writers.iterator();
            while (writerIter.hasNext()) {
                writerIter.next().writeSpreadsheetToDisk();
            }
            allOutputWriter.writeSpreadsheetToDisk();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    // ------------------------ [START] UTILITY FUNCTIONS -------------------------

    /**
     * Valid directories are those that do not contain
     * - model and dbschema and publicModel and test and classes
     * @param absDirPath
     * @return
     */
    public boolean isValidDirectory(String absDirPath) {
        if (!absDirPath.contains("/model/") && !absDirPath.contains("/dbSchema/") && !absDirPath.contains("/publicModel/") &&
            !absDirPath.contains("/classes/") && !absDirPath.contains("/test/")) {
            return true;
        }
        return false;
    }

    public boolean isValidModelDirectory(String absDirPath) {
        if (!absDirPath.contains("/dbSchema/") && !absDirPath.contains("/classes/") && !absDirPath.contains("/test/")) {
            return true;
        }
        return false;
    }

    /**
     * Valid ui files are those that
     * - end with jspx or jsff
     * - and are not in a directory containing test or modeltest or classes or model or publicmodel
     * @param absFilePath
     * @return
     */
    public boolean isValidUIFile(String absFilePath) {
        if (!isEmpty(absFilePath)) {
            if (absFilePath.contains("/test/") || absFilePath.contains("/modelTest/") || absFilePath.contains("/classes/") ||
                absFilePath.contains("/model/") || absFilePath.contains("/publicModel/")) {
                return false;
            }
            if (absFilePath.endsWith(".jspx") || absFilePath.endsWith(".jsff")) {
                return true;
            }
        }
        return false;
    }


    /**
     *checks whether the given string is empty
     * @param str
     * @return
     */
    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public static String setSystemProperties() {
        StringBuffer sysprops = new StringBuffer();
        String dbMode = System.getProperty("dumpDB");
        if (!isEmpty(dbMode) && "false".equals(dbMode.trim())) {
            m_dumpDb = false;
        } else if (!isEmpty(dbMode) && "true".equals(dbMode.trim())) {
            m_dumpDb = true;
        }
        sysprops.append(" -DdumpDB=" + m_dumpDb);
        String ot = System.getProperty("out");
        if (!isEmpty(ot)) {
            m_baseOutFileName = ot;
            sysprops.append(" -Dout=" + m_baseOutFileName);
        }

        String startModel = System.getProperty("startFromModel");
        if (!isEmpty(startModel) && "true".equals(startModel.trim())) {
            startFromModel = true;
        } else if (!isEmpty(startModel) && "false".equals(startModel.trim())) {
            startFromModel = false;
        }
        sysprops.append(" -DstartFromModel=" + startFromModel);

        String debg = System.getProperty("debug");
        if (!isEmpty(debg) && "true".equals(debg.trim())) {
            m_bDebug = true;
        } else if (!isEmpty(debg) && "false".equals(debg.trim())) {
            m_bDebug = false;
        }

        sysprops.append(" -Ddebug=" + m_bDebug);

        return sysprops.toString();
    }


    // ------------------------ [END] UTILITY FUNCTIONS -------------------------


}
