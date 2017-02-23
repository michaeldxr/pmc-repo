package oracle.apps.idAnalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileReader;

import java.io.FileWriter;

import java.io.InputStream;

import java.io.StringWriter;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class NamingContainerIdFixer {
    /**
     * Number of jsff files
     */
    static int m_jsffCount = 0;

    /**
     * number of jspx files
     */
    static int m_jspxCount = 0;

    /**
     * Total number of violations detected while running
     */
    static int number_of_violations_byrun = 0;

    /**
     * Total number of violations fixed
     */
    static int m_numberOfViolationsFixed = 0;

    /**
     * Total number of violations not fixed
     */
    static int m_numberOfViolationsNotFixed = 0;

    /**
     * Total number of violations not fixed
     */
    static int m_numberOfSecondaryRefs = 0;

    /**
     * Total number of violations not fixed
     */
    static int m_numberOfRefsFixed = 0;

    /**
     * Total number of violations not fixed
     */
    static int m_numberOfRefsNotFixed = 0;

    /**
     * HashMap of potential java files that may need to be modified due to id changes
     */
    static HashSet<String> potentailJavaFiles = new HashSet<String>();

    /**
     * Map of all changedIds indexed by the old id.
     * This will be used while analyzing the java files since we need to scan each java file
     * by the old ids that were changed
     */
    HashMap<String, ArrayList<String>> map_changedId_byoldId =
        new HashMap<String, ArrayList<String>>();


    /**
     * Writer for diff.txt
     */
    private BufferedWriter diff;

    /**
     * Writer for evaluated JavaFiles
     */
    private BufferedWriter javaWriter;

    final static String diffFileName = "diff.txt";

    final static String javaFileName = "javaFileAnalysis.txt";

    /**
     * Constructor
     */
    public NamingContainerIdFixer() {
        super();
    }


    public static void main(String[] args) {

        // Print Statistics
        Date startDate = new Date();

        if (args.length != 3 || isEmpty(args[0]) || isEmpty(args[1]) ||
            isEmpty(args[2])) {
            System.out.println("Usage:  namingContainerIdFixer.sh <path to your code in ade> <csv file name, including path> <doADE>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        String sCsvFile = args[1];
        Boolean bDoADE = new Boolean(args[2]);
        System.out.println("Analyzing Directory " + sCrawlDir);
        System.out.println("Csv file location " + sCsvFile);

        NamingContainerIdFixer namingCntFixer = new NamingContainerIdFixer();
        HashMap<String, ArrayList<ComponentDetails>> map_parsed_components =
            new HashMap<String, ArrayList<ComponentDetails>>();

        try {

            map_parsed_components = namingCntFixer.parseCsvFile(sCsvFile);
            if (map_parsed_components == null) {
                System.out.println("Error in parsing csv file");
                System.exit(1);
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            System.exit(1);
        }
        if (map_parsed_components == null) {
            System.out.println("An error occurred");
            System.exit(1);
        }

        System.out.println("[INFO] Based on CSV File total number of files to be processed: " +
                           map_parsed_components.size());

        try {
            if (bDoADE.booleanValue()) {
                // Remove any existing diff.txt. Create a new bufferred writer for diff.txt
                namingCntFixer.startDiffLog();
            }

            /**
             * Recursevely scan the directory. For any valid UI file, process all the faces components
             * For each naming container, determine if the length of the component Id is within standards (7char)
             * populate the list of resulting naming containers not within standards, indexed by their file name
             */
            System.out.println("====================Analyzing and fixing UI files ===================== ");
            namingCntFixer.crawlDirectory(sCrawlDir, bDoADE,
                                          map_parsed_components);
            System.out.println("======================================================================= ");
            if (bDoADE.booleanValue()) {
                namingCntFixer.closeDiffLog();
            }

            System.out.println("==================== Analyzing any references in java files ===================== ");
            namingCntFixer.evaluatePotentialJavaFiles(sCrawlDir, bDoADE,
                                                      namingCntFixer.map_changedId_byoldId);
            int NumberOfJavaFiles = potentailJavaFiles.size();
            if (NumberOfJavaFiles > 0) {
                System.out.println("Number of potential Java  Files: " +
                                   NumberOfJavaFiles);
                Iterator<String> javafiles_iter =
                    potentailJavaFiles.iterator();
                while (javafiles_iter.hasNext()) {
                    System.out.println("Please evaluate Java File:" +
                                       javafiles_iter.next());
                }
                System.out.println("\n Note: All above Java files have refereces to the old id. Please manually evaluate these files and make changes if necessary. ");
                System.out.println("Please review" + javaFileName +
                                   " (It contains an analysis of all references in the java files");

            }

            System.out.println("======================================================================= ");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        printStatistics(startDate);


    }

    public void evaluatePotentialJavaFiles(String sCrawlDir, boolean bDoADE,
                                           HashMap<String, ArrayList<String>> mapoldIds) {
        try {
            javaWriter = new BufferedWriter(new FileWriter(javaFileName));

            crawlDirectoryForJavaFiles(sCrawlDir, bDoADE, mapoldIds);

            javaWriter.close();

        } catch (Exception ex) {
            System.out.println("Exception occurred while evaluating java files");
        }


    }

    public static void printStatistics(Date startDate) {
        // Print Statistics
        Date endDate = new Date();
        System.out.println("=================================================");
        System.out.println("Run Statistics");
        System.out.println("Start Time: " + startDate.toString());
        System.out.println("End Time: " + endDate.toString());
        long elapsedTime = endDate.getTime() - startDate.getTime();
        System.out.println("Elapsed Time (sec): " + elapsedTime / 1000);
        System.out.println("=================================================");

        System.out.println("=================================================");
        System.out.println("Total Number of jspx files reviewed: " +
                           m_jspxCount);
        System.out.println("Total Number of jsff files reviewed: " +
                           m_jsffCount);
        System.out.println("Total Number of violations found in run: " +
                           number_of_violations_byrun);
        System.out.println("Total Number of violations fixed: " +
                           m_numberOfViolationsFixed);
        System.out.println("Total Number of Secondary refs: " +
                           m_numberOfSecondaryRefs);
        System.out.println("Total Number of Secondary refs fixed: " +
                           m_numberOfRefsFixed);
        System.out.println("Total Number of secondary refs not fixed: " +
                           m_numberOfRefsNotFixed);
        System.out.println("Total Number of potentially affected java files: " +
                           potentailJavaFiles.size());

        System.out.println("=================================================");

    }


    public HashMap<String, ArrayList<ComponentDetails>> parseCsvFile(String csvFilePath) throws Exception {
        boolean validationError = false;
        HashMap<String, ArrayList<ComponentDetails>> map_components =
            new HashMap<String, ArrayList<ComponentDetails>>(100);
        File file = new File(csvFilePath);
        FileReader reader = new FileReader(file);
        BufferedReader input = new BufferedReader(reader);
        String firstLine = input.readLine();
        if (firstLine == null) {
            System.out.println("File is empty!!");
            return null;
        }
        String line;
        while ((line = input.readLine()) != null) {
            if (isEmpty(line)) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length < 6) {
                continue;
            }
            String filePath = parts[2].trim();
            ComponentDetails component = new ComponentDetails();
            component.setProduct(parts[0].trim());
            component.setFileName(parts[1].trim());
            component.setFilePath(filePath);
            component.setComponentName(parts[3].trim());
            String oldId = parts[4].trim();
            String newId = parts[5].trim();
            component.setComponentId(oldId);
            component.setSuggestedId(newId);
            if (doesIdExist(map_components, filePath, oldId, true)) {
                validationError = true;
                System.out.println("[Validation Error] Old Id already exists: " +
                                   oldId);
            }
            if (doesIdExist(map_components, filePath, newId.trim(), false)) {
                validationError = true;
                System.out.println("[Validation Error] New Id already exists: " +
                                   newId);
            }
            addToCSVParsedMap(map_components, filePath, component);
        }
        if (validationError) {
            return null;
        }

        return map_components;
    }

    private boolean doesIdExist(HashMap<String, ArrayList<ComponentDetails>> all_components_per_file,
                                String filePath, String id, boolean isOld) {
        boolean exists = false;
        String formattedPath = getFusionappsIndexedPath(filePath);
        if (all_components_per_file == null || isEmpty(formattedPath)) {
            return exists;
        }
        if (all_components_per_file.containsKey(formattedPath)) {
            ArrayList<ComponentDetails> list_components =
                all_components_per_file.get(formattedPath);
            Iterator<ComponentDetails> comp_iter = list_components.iterator();
            while (comp_iter.hasNext()) {
                ComponentDetails component = comp_iter.next();
                String oldId = component.getComponentId();
                String newId = component.getSuggestedId();
                if (isOld) {
                    if (id.trim().equals(oldId)) {
                        return true;
                    }
                } else {
                    if (id.trim().equals(newId)) {
                        return true;
                    }
                }
            }
        }

        return exists;
    }

    /**
     * Format the absolute file path by the occurrence of /fusionapps/
     * e.g. /scratch/zcurrimb/view_storage/zcurrimb_m6b_latest/fusionapps/fin/components/abc.txt
     * will become
     * /fusionapps/fin/components/abc.txt
     * @param absFilePath
     * Absolute file path
     * @return
     * formatted file path
     */
    private String getFusionappsIndexedPath(String absFilePath) {
        if (isEmpty(absFilePath)) {
            return absFilePath;
        }
        if (absFilePath.contains("/fusionapps/")) {
            int fusionappsIndex = absFilePath.indexOf("/fusionapps/");
            return absFilePath.substring(fusionappsIndex);
        }
        return absFilePath;
    }

    /**
     * Add to the csv parsed map, the filename and component
     * For the filename, if a listofcomponents already exists, add to the list
     * if it does not exist, create a new list and add to the list
     * @param componentMapByFile
     * ComponentMap to add to
     * @param filePath
     * FilePath formatted to start with /fusionapps/
     * @param component
     * Component
     */
    private void addToCSVParsedMap(HashMap<String, ArrayList<ComponentDetails>> componentMapByFile,
                                   String filePath,
                                   ComponentDetails component) {
        String formattedPath = getFusionappsIndexedPath(filePath);
        if (componentMapByFile == null || isEmpty(formattedPath) ||
            component == null) {
            return;
        }
        if (componentMapByFile.containsKey(formattedPath)) {
            ArrayList<ComponentDetails> list_components =
                componentMapByFile.get(formattedPath);
            list_components.add(component);
            componentMapByFile.put(formattedPath, list_components);
        } else {
            ArrayList<ComponentDetails> list_components =
                new ArrayList<ComponentDetails>();
            list_components.add(component);
            componentMapByFile.put(formattedPath, list_components);
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
    private void crawlDirectory(String path, Boolean bDoADE,
                                HashMap<String, ArrayList<ComponentDetails>> csvParsedComponents) {
        if (path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (isValidUIFile(absFilePath)) {
                    String formattedPath =
                        getFusionappsIndexedPath(absFilePath);
                    if (csvParsedComponents.containsKey(formattedPath)) {
                        System.out.println("Processing file " +
                                           listOfFiles[i].getName());
                        ArrayList<ComponentDetails> list_components =
                            csvParsedComponents.get(formattedPath);
                        if (absFilePath.trim().endsWith(".jsff")) {
                            m_jsffCount++;
                        } else if (absFilePath.trim().endsWith(".jspx")) {
                            m_jspxCount++;
                        }
                        processUiFile(listOfFiles[i], bDoADE, list_components);
                    }

                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidDirectory(dir)) {
                    crawlDirectory(dir, bDoADE, csvParsedComponents);
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
    private void crawlDirectoryForJavaFiles(String path, Boolean bDoADE,
                                            HashMap<String, ArrayList<String>> changedComponents) throws Exception {
        if (path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (absFilePath.endsWith(".java")) {

                    processJavaFile(listOfFiles[i], bDoADE,
                                    map_changedId_byoldId);

                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidDirectory(dir)) {
                    crawlDirectoryForJavaFiles(dir, bDoADE, changedComponents);
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
    public void processJavaFile(File file, boolean bDoADE,
                                HashMap<String, ArrayList<String>> chnagedIds) throws Exception {


        FileReader reader = new FileReader(file);
        BufferedReader input = new BufferedReader(reader);
        String filePath = file.getAbsolutePath();
        String formattedPath = getFusionappsIndexedPath(filePath);
        boolean foundViolationInFile = false;
        String line;
        int lineNum = 1;
        while ((line = input.readLine()) != null) {
            Set<String> old_id_set = chnagedIds.keySet();
            Iterator<String> old_id_iter = old_id_set.iterator();
            while (old_id_iter.hasNext()) {
                String id = old_id_iter.next();
                String searchStr = "\"" + id + "\"";

                if (line.contains(searchStr)) {
                    foundViolationInFile = true;
                    StringBuilder sb = new StringBuilder();
                    sb.append("--JavaFile: " + file.getName() + " \n");
                    sb.append("Line Number:" + lineNum + ": ");
                    sb.append(line + " \n");
                    sb.append("oldId: " + id + " \n");
                    String newChanges =
                        getformattedNewChangeString(chnagedIds, id);
                    sb.append(newChanges + " \n\n");
                    javaWriter.write(sb.toString());
                    System.out.println("JavaFile: " + file.getName() +
                                       " line Num: " + lineNum);
                    System.out.println("Line: " + line);
                    System.out.println("oldId : " + id);

                    System.out.println("Id changes that were made : ");
                    System.out.println(newChanges);
                }

            }
            lineNum++;
        }
        if (foundViolationInFile) {
            potentailJavaFiles.add(formattedPath);
        }


    }

    private String getformattedNewChangeString(HashMap<String, ArrayList<String>> map,
                                               String oldId) {
        String changedformatStr = "";
        if (map.containsKey(oldId)) {
            ArrayList<String> list_changes = map.get(oldId);
            Iterator<String> iter_changes = list_changes.iterator();
            while (iter_changes.hasNext()) {
                String changedStr = iter_changes.next();
                String[] changedParts = changedStr.split("#");
                if (changedParts.length > 1) {
                    changedformatStr =
                            changedformatStr + "New Id: " + changedParts[0] +
                            " (in file: " + changedParts[1] + " ) \n";
                }
            }
        }
        return changedformatStr;
    }

    /**
     * Process each valid ui file. If the file has any found violations, fix them and increment the violation count
     * @param file
     * @param bDoADE
     */
    public void processUiFile(File file, boolean bDoADE,
                              ArrayList<ComponentDetails> list_components) {

        NamingCntIdFileProcessor namingContainerProc =
            new NamingCntIdFileProcessor();

        String fixedFileContents =
            namingContainerProc.fixNamingContainerIdViolations(file, bDoADE,
                                                               list_components);
        m_numberOfViolationsFixed =
                m_numberOfViolationsFixed + namingContainerProc.getNcViolationsFixed();
        m_numberOfViolationsNotFixed =
                m_numberOfViolationsNotFixed + namingContainerProc.getNcViolationsNotFixed();
        m_numberOfSecondaryRefs =
                m_numberOfSecondaryRefs + namingContainerProc.getNumberOfSecondaryRefs();
        m_numberOfRefsFixed =
                m_numberOfRefsFixed + namingContainerProc.getSecondaryRefsFixed();
        m_numberOfRefsNotFixed =
                m_numberOfRefsNotFixed + namingContainerProc.getSecondaryRefsNotFixed();
        
 
        if (isEmpty(fixedFileContents)) {
            System.out.println("Error occurred while fixing file.. Skipping : " +
                               file.getName());
        }

        // get all violating components
        ArrayList<ComponentDetails> list_violations =
            namingContainerProc.getAllViolatingComponents();
        if (list_violations.size() > 0) {

            try {
                applyFixedFile(file, fixedFileContents, bDoADE);
            } catch (Exception ex) {
                System.out.println("Exception while trying to fix file: " +
                                   file.getPath());
                ex.printStackTrace();
            }
            HashMap<String, String> changedIds =
                namingContainerProc.getChangedIds();
            addChangedIdsToGlobalMap(changedIds, file.getAbsolutePath());
            // keep track of the total number of violations
            number_of_violations_byrun =
                    number_of_violations_byrun + list_violations.size();


        }

    }

    private void addChangedIdsToGlobalMap(HashMap<String, String> changedIds,
                                          String filePath) {
        Set<String> set_old_ids = changedIds.keySet();
        Iterator<String> old_ids_iter = set_old_ids.iterator();
        while (old_ids_iter.hasNext()) {
            String oldId = old_ids_iter.next();
            String newId = changedIds.get(oldId);
            String formattedPath = getFusionappsIndexedPath(filePath);
            addToChangedIdMap(oldId, newId, formattedPath);
        }
    }

    public void applyFixedFile(File file, String newFileContents,
                               boolean bDoAde) throws Exception {
        if (bDoAde) {
            checkoutAndDelete(file);

            FileWriter fw = new FileWriter(file.getAbsolutePath());
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(newFileContents);
            bw.close();

            checkinAndDiff(file);
        }

    }

    private void addToChangedIdMap(String oldId, String newid,
                                   String filePath) {
        String Delim = "#";
        String valueToAdd = newid + Delim + filePath;
        if (map_changedId_byoldId.containsKey(oldId)) {
            ArrayList<String> changedIdList = map_changedId_byoldId.get(oldId);
            changedIdList.add(valueToAdd);
            map_changedId_byoldId.put(oldId, changedIdList);

        } else {
            ArrayList<String> changedidList = new ArrayList<String>();
            changedidList.add(valueToAdd);
            map_changedId_byoldId.put(oldId, changedidList);
        }
    }


    private void checkinAndDiff(File f) throws Exception {

        Process ciProcess =
            Runtime.getRuntime().exec("ade ci " + f.getAbsolutePath());
        checkProcess(ciProcess, f, "checkin", false);

        String diffCmd = "ade diff -label " + f.getAbsolutePath();
        Process diffProcess = Runtime.getRuntime().exec(diffCmd);
        checkProcess(diffProcess, f, "diff file with previous in label", true);
    }

    private void checkoutAndDelete(File f) throws Exception {

        Process coProcess =
            Runtime.getRuntime().exec("ade co -nc " + f.getAbsolutePath());
        checkProcess(coProcess, f, "checkout", false);

        Process rmProcess =
            Runtime.getRuntime().exec("rm -f " + f.getAbsolutePath());
        checkProcess(rmProcess, f, "remove old file", false);


    }

    /**
     * Start the diff log. Remove any previously existing diff.txt file
     * @throws Exception
     */
    private void startDiffLog() throws Exception {
        // Remove any existing diff.txt
        Process p = Runtime.getRuntime().exec("rm -f " + diffFileName);
        checkProcess(p, null, "remove any existing diff log", false);

        diff = new BufferedWriter(new FileWriter(diffFileName));
    }

    private void checkProcess(Process p, File f, String description,
                              boolean isDiff) throws Exception {
        int exitCode = p.waitFor();
        if (!isDiff && exitCode != 0) {
            if (f != null)
                System.out.println("There was an error when trying to " +
                                   description + " for file : " +
                                   f.getAbsolutePath());
            else
                System.out.println("There was an error when trying to " +
                                   description);
        }

        dumpInputStream(p.getInputStream(), isDiff);
        dumpInputStream(p.getErrorStream(), isDiff);
    }

    private void dumpInputStream(InputStream in,
                                 boolean isDiff) throws Exception {
        int n = in.available();
        byte b[] = new byte[n];
        in.read(b, 0, n);
        String str = new String(b);
        if (str.contains("ade ERROR")) {
            System.out.println(str);
            System.out.println("There was an error interacting with ADE, exiting script.");
            System.exit(-1);
        }

        if (isDiff == false)
            System.out.println(str.trim());
        else
            diff.write(str);
    }


    private void closeDiffLog() throws Exception {
        diff.close();
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public boolean isValidDirectory(String absDirPath) {
        if (!absDirPath.contains("/model/") &&
            !absDirPath.contains("/dbSchema/") &&
            !absDirPath.contains("/publicModel/") &&
            !absDirPath.contains("/classes/") &&
            !absDirPath.contains("/test/")) {
            return true;
        }
        return false;
    }

    public boolean isValidUIFile(String absFilePath) {
        if (!isEmpty(absFilePath)) {
            if (absFilePath.contains("/test/") ||
                absFilePath.contains("/modelTest/") ||
                absFilePath.contains("/classes/") ||
                absFilePath.contains("/model/") ||
                absFilePath.contains("/publicModel/")) {
                return false;
            }
            if (absFilePath.endsWith(".jspx") ||
                absFilePath.endsWith(".jsff")) {
                return true;
            }
        }
        return false;
    }

}
