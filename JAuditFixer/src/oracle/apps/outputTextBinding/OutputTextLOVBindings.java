package oracle.apps.outputTextBinding;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;
import java.io.Writer;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.helpers.JprHelper;
import oracle.apps.votuning.DataBindings;
import oracle.apps.votuning.UiFileHelper;

public class OutputTextLOVBindings {
    
   /**
   * Number of jsff files processed
   */
    static int m_jsffCount = 0;

    /**
     * number of jspx files processed
     */
    static int m_jspxCount = 0;

    static boolean m_bDebug = false;
    static int m_numberOfJprsScanned = 0;
    public static Writer outputFileWriter;

    /**
     * Map of all the databindings indexed by the adfmsrc directory.
     * This is used to speed up lookup time. Instead of trying to find, parse the cpx file for each ui file, this  map holds the cpx object indexed in the map by the adfmsrc directory.
     * if there is an adfmsrc directory as the key in this map, the bindings object is retrieved from here. Else for the ui file, attempt to find the cpx file in thea dfmsrc directory. once found it is put in the map.
     */
    private static HashMap<String, DataBindings> bindingsMap = new HashMap<String, DataBindings>();


    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        OutputTextLOVBindings lovBindingsScanner = new OutputTextLOVBindings();

        // Usage:  comboboxWidthScanner.sh <path to your code>
        if (args.length < 1 || isEmpty(args[0])) {
            System.out.println("Usage:  OutputTextBindings.sh <path to code for scan>");
            System.exit(1);
        }

        // Command liine processing
        String sCrawlDir = args[0];
        Date startDate = new Date();
        
        try {            
          outputFileWriter = new BufferedWriter(new FileWriter("outTextBindings.csv") );
          outputFileWriter.write("Family,Module,SubModule,FilePath,Filename,NodeId,ParentTableId,VOName," +
              "VOFetchSize,VORangeSize,Binding,BindingType,ControlType,LOVName,Exception\n");   
          System.out.println("Finding outputText bindings in crawl directory: " + sCrawlDir);
            lovBindingsScanner.crawlDirectory(sCrawlDir);        
        
        } catch (Exception ex) {
            System.out.println(String.format("Exception occurred while scanning directory:%s , Exception: %s ", sCrawlDir,
                                             ex.getMessage()));
            } finally {
              outputFileWriter.close();
            }

        System.out.println("Number of jprs looked at:" + m_numberOfJprsScanned);
       // lovBindingsScanner.printStatsOfProducersPerProduct(lovBindingsScanner.producersByProduct);
        Date endDate = new Date();
        long milsecRun = endDate.getTime() - startDate.getTime();
        System.out.println("total elapsed time in sec " + milsecRun / 1000);
    }

    /**
     * recurse over the crawl directory and for each valid ui file - process it.
     * Valid directories -
     * - should not contain, /dbSchema/ or /model/ or /publicModel/ or /test/ or /classes/
     * Valid UI files
     * - jspx or jsff extensions
     * - and not in folder with /classes/ or /test/ or /model/ or /publicModel/
     * @param path
     */
    private void crawlDirectory(String path) throws Exception {
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
                    crawlDirectory(dir);
                }
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }


    /**
     * Process each valid ui file.
     * @param file
     * @param bDoADE
     */
    public void analyseUiFileJpr(File file) throws Exception{
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
            UIComboProcessor comboProc = new UIComboProcessor();
            comboProc.analyzeUIFile(uiFile, databindings, jprPath);           
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

}
