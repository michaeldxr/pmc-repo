package oracle.apps.votuning_v2;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class UiFileHelper {
  
  public static boolean bDebug = false;

  private static String getPreviousDirectory(String path) {
      if (!isEmpty(path)) {
          int indexLastSlash = path.lastIndexOf("/");
          if (indexLastSlash != -1) {
              return path.substring(0, indexLastSlash);
          }
      }
      return null;
  }
  /**
   * gets the DataBindings.cpx file for the ui file path specified.
   * returns null if
   * 1. cannot find the databindings.cpx file in the adfmsrc folder
   * 2. more than one databindings.cpx file is found
   * 3. file does not exist
   * @param uiFileAbsPath
   * @return
   */
  public static DataBindings getDataBindingsFileForJpr(String jprPath) {
    String jprBaseDir = getPreviousDirectory(jprPath);
    String adfmBaseDir = jprBaseDir+"/adfmsrc/";
      File baseDir = new File(adfmBaseDir);

      if (baseDir == null || !baseDir.exists()) {
          if(bDebug)
             System.out.println("Could not obtain a valid base directory for cpx in: " + jprPath);
          return null;
      }
      ArrayList<String> matchingFiles = getAllValidCpxFilesInDir(baseDir);
      if (matchingFiles == null || matchingFiles.size() <= 0) {
          if(bDebug)
            System.out.println("[ERROR] cannot find databinds.cpx for ui file " + jprPath);
          return null;
      }
      if (matchingFiles.size() > 1) {
          if(bDebug){
            System.out.println("[ERROR] more than one databindings.cpx file found for ui file: " + jprPath);
            System.out.println("The patsh are:");
            Iterator<String> filesfounditer = matchingFiles.iterator();
            while (filesfounditer.hasNext()) {
                String fileFound = filesfounditer.next();
                System.out.println("File: " + fileFound);
            }
          }
          return null;
      }
      String cpxFilePath = matchingFiles.get(0);
      File cpxFile = new File(cpxFilePath);
      if (cpxFile.exists())
          return DataBindings.createBindingsFile(cpxFile);
      else {
        if(bDebug)
          System.out.println("data bindings file does not exist: " + cpxFilePath);
      }
      return null;

  }


    /**
     * gets the DataBindings.cpx file for the ui file path specified.
     * returns null if
     * 1. cannot find the databindings.cpx file in the adfmsrc folder
     * 2. more than one databindings.cpx file is found
     * 3. file does not exist
     * @param uiFileAbsPath
     * @return
     */
    public static DataBindings getDataBindingsFileForUiFile(String uiFileAbsPath) {
        String strBaseDir = getAdfmsrcPath(uiFileAbsPath);
        File baseDir = new File(strBaseDir);

        if (baseDir == null || !baseDir.exists()) {
          if(bDebug)
            System.out.println("Could not obtain a valid base directory for cpx in: " + uiFileAbsPath);
            return null;
        }
        ArrayList<String> matchingFiles = getAllValidCpxFilesInDir(baseDir);
        if (matchingFiles == null || matchingFiles.size() <= 0) {
          if(bDebug)
            System.out.println("[ERROR] cannot find databinds.cpx for ui file " + uiFileAbsPath);
            return null;
        }
        if (matchingFiles.size() > 1) {
            if(bDebug){
              System.out.println("[ERROR] more than one databindings.cpx file found for ui file: " + uiFileAbsPath);
              System.out.println("The patsh are:");
              Iterator<String> filesfounditer = matchingFiles.iterator();
              while (filesfounditer.hasNext()) {
                  String fileFound = filesfounditer.next();
                  System.out.println("File: " + fileFound);
              }
            }
            return null;
        }
        String cpxFilePath = matchingFiles.get(0);
        File cpxFile = new File(cpxFilePath);
        if (cpxFile.exists())
            return DataBindings.createBindingsFile(cpxFile);
        else {
          if(bDebug)
            System.out.println("data bindings file does not exist: " + cpxFilePath);
        }
        return null;

    }

    public static ArrayList<String> addToArray(ArrayList<String> list, ArrayList<String> arrayToAdd) {
        if (arrayToAdd == null) {
            return list;
        }
        if (list == null) {
            list = new ArrayList<String>();
        }
        for (int i = 0; i < arrayToAdd.size(); i++) {
            list.add(arrayToAdd.get(i));
        }
        return list;
    }

    public static ArrayList<String> getAllValidCpxFilesInDir(File baseDirectory) {

        ArrayList<String> filesFound = new ArrayList<String>();
        if (baseDirectory == null) {
            return null;
        }
        String absPath = baseDirectory.getAbsolutePath();
        if (absPath.contains(".ade_path")) {
            return null;
        }
        if (baseDirectory.isDirectory()) {
            ValidCpxFileFilter filter = new ValidCpxFileFilter();
            String[] matches = baseDirectory.list(filter);

            for (int k = 0; k < matches.length; k++) {
                filesFound.add(baseDirectory + "/" + matches[k]);
            }
            //filesFound = addToArray(filesFound, absPath, matches);

            File[] files = baseDirectory.listFiles();
            for (int i = 0; i < files.length; i++) {
                ArrayList<String> filesgotback = getAllValidCpxFilesInDir(files[i]);
                if (filesgotback != null) {
                    filesFound = addToArray(filesFound, filesgotback);
                }
            }
        }
        return filesFound;
    }

    public static String getUIFileAbstractPath(String absPath) {
        if (isEmpty(absPath)) {
            return null;
        }
        final String SEARCH = "/public_html";
        int indexpublichtml = absPath.indexOf(SEARCH);
        if (indexpublichtml != -1) {
            return absPath.substring(indexpublichtml + SEARCH.length());
        }
        return null;
    }

    public String getPageDefNameForUIFile(String uiFileAbsPath) {
        String pageDefName = "";

        return pageDefName;
    }

    /**
     * Get the base directory to look for the databindings.cpx file
     * base directory will be within /adfmsrc/
     * If ui File path is $ADE_VIEW_ROT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/public_html/oracle/apps/TestPage.jspx
     * then base directory for cpx will be: $ADE_VIEW_ROT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/adfmsrc
     * @param uiFileAbsPath
     * @return
     */
    public static String getAdfmsrcPath(String uiFileAbsPath) {
        if (!isValidUIFileToFindCpx(uiFileAbsPath)) {
            System.out.println("[UiFileHelper:ERROR] file is not in a valid path (should be in public_html): " +
                               uiFileAbsPath);
            return null;
        }

        int indexPublicHtml = uiFileAbsPath.indexOf("/public_html/");
        if(indexPublicHtml == -1)
            return null;
        String strBeforePublicHtml = uiFileAbsPath.substring(0, indexPublicHtml);
        if (isEmpty(strBeforePublicHtml)) {
          if(bDebug)
            System.out.println("Could not find cpx file for file: " + uiFileAbsPath);
            return null;
        }
        String strCpxBaseDir = strBeforePublicHtml + "/adfmsrc/";

        return strCpxBaseDir;
    }

    /**
     * A Valid UI file in order to find the databindings cpx has to be in directory /public_html/
     * @param uiFileAbsPath
     * Absolute file path
     * @return
     * true/false depending if it is valid
     */
    public static boolean isValidUIFileToFindCpx(String uiFileAbsPath) {
        if (isEmpty(uiFileAbsPath)) {
            return false;
        }
        if (uiFileAbsPath.contains(".ade_path")) {
            return false;
        }
        if (uiFileAbsPath.contains("/public_html/")) {
            return true;
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

    /**
     * gets the base lba path from the absolute ui file path
     * e.g. if the ui file path is: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/public_html/oracle/apps/financials/Test.jspx
     * then the absolute base path is derived from this ui path as:
     * $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/
     * This is useful for functions like when looking for the am in all related
     * model files.. then we start from the base lba path
     * @param absFilePath
     * @return
     */
    public static String getBaseLBAPath(String absFilePath) {
        String baseLocation = "";
        if (isEmpty(absFilePath)) {
            return null;
        }
        if (absFilePath.contains("/public_html/")) {
            int indexPublicHtml = absFilePath.indexOf("/public_html/");
            if (indexPublicHtml != -1) {
                String beforeUIFilePath = absFilePath.substring(0, indexPublicHtml);


                int indexLastSlash = beforeUIFilePath.lastIndexOf("/");
                String previousDir = beforeUIFilePath.substring(0, indexLastSlash);
                return previousDir;

            }
        } else if (absFilePath.contains("/src/")) {
            int indexSrc = absFilePath.indexOf("/src/");
            if (indexSrc != -1) {
                String beforeSrcFilePath = absFilePath.substring(0, indexSrc);
                if (!isEmpty(beforeSrcFilePath)) {
                    int indexLastSlash = beforeSrcFilePath.lastIndexOf("/");
                    if (indexLastSlash != -1) {
                        String previousDir = beforeSrcFilePath.substring(0, indexLastSlash);
                        return previousDir;
                    }
                }

            }
        } else {
          if(bDebug)
            System.out.println("Cannot recognize ui file path.. should contain public_html" + absFilePath);
        }
        return baseLocation;
    }

    /**
     * Gets the jpr for this ui file.
     * e.g. UI File: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/public_html/oracle/apps/financials/Test.jspx
     * 1. gets the path before public_html e.g. : $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di
     * 2. looks within that location for the existence of .jpr files
     *  2a. If more than one is found - returns null (ERROR) there should not be more than one jpr files in that locationn
     *  2b. If not found goes one directory up and reruns the search for .jpr
     *  2c. if found returns the absolute path of the jpr file found
     * @param uiFilePath
     * UI file path for which the jpr location is to be found
     * @return
     */
    public static String getJprLocationForUiFile(String uiFilePath) {
        String jprLocation = "";
        if (isEmpty(uiFilePath)) {
            return null;
        }
        if (uiFilePath.contains("/public_html/")) {
            int indexPublicHtml = uiFilePath.indexOf("/public_html/");
            if (indexPublicHtml != -1) {
                String beforeUIFilePath = uiFilePath.substring(0, indexPublicHtml);
                boolean bFound = false;
                while (!bFound) {
                    File f = new File(beforeUIFilePath);
                    if (f.exists()) {
                        String[] filenames = f.list(new JprFileFilter());
                        if (filenames == null || filenames.length == 0) {
                            int lastSlash = beforeUIFilePath.lastIndexOf("/");
                            if (lastSlash != -1) {
                                beforeUIFilePath = beforeUIFilePath.substring(0, lastSlash);
                            } else {
                              if(bDebug)
                                System.out.println("cannot find jpr.. returning");
                                return null;
                            }
                        } else if (filenames.length == 1) {
                            jprLocation = beforeUIFilePath + "/" + filenames[0];
                            bFound = true;
                        } else if (filenames.length > 1) {
                          if(bDebug)
                            System.out.println("Found more than one jprs in location: " + beforeUIFilePath);
                            return null;
                        }
                    } 
                }
            }
        } else {
            if(bDebug)
                System.out.println("Cannot recognize ui file path.. should contain public_html" + uiFilePath);
        }
        return jprLocation;
    }

    /**
     * gets the jpr location for the model file. e.g. AM's, VO's.
     * Typically will be used when you have an AM file path and you need to
     * look at the jpr of the model project to find all dependent BCs.
     * e.g. model file path: $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/model/src/oracle/apss/financials/TestAM.xml
     * 1. Find the part before /src/ (model artifacts will always be in this folder)
     * e.g. $ADE_VIEW_ROOT/fusionapps/fin/components/ledger/gl/journals/desktopEntry/model
     * 2. Look for jpr within this folder.
     *  2a. If more than one found - return null
     *  2b. If not found - get previous directory and reexecute searrch
     *  2c. If found - return absolute path of jpr
     * @param modelFilePath
     * Model file path whose jpr is to be found
     * @return
     */
    public static String getJprLocationForModelFile(String modelFilePath) {
        String jprLocation = "";
        if (isEmpty(modelFilePath)) {
            return null;
        }
        if (modelFilePath.contains("/src/")) {
            int indexSrc = modelFilePath.indexOf("/src/");
            if (indexSrc != -1) {
                String beforeSrcFilePath = modelFilePath.substring(0, indexSrc);
                boolean bFound = false;
                while (!bFound) {
                    File f = new File(beforeSrcFilePath);
                    if (f.exists()) {
                        String[] filenames = f.list(new JprFileFilter());
                        if (filenames == null || filenames.length == 0) {
                            int lastSlash = beforeSrcFilePath.lastIndexOf("/");
                            if (lastSlash != -1) {
                                beforeSrcFilePath = beforeSrcFilePath.substring(0, lastSlash);
                            } else {
                              if(bDebug)
                                System.out.println("cannot find jpr.. returning");
                                return null;
                            }
                        } else if (filenames.length == 1) {
                            jprLocation = beforeSrcFilePath + "/" + filenames[0];
                            bFound = true;
                        } else if (filenames.length > 1) {
                          if(bDebug)
                            System.out.println("Found more than one jprs in location: " + beforeSrcFilePath);
                            return null;
                        }
                    } else{
                        System.out.println("Cannot find jpr for file: "+modelFilePath);
                        return null;
                        }
                }
            }
        } else {
            if(bDebug)
                System.out.println("Cannot recognize model file path.. should always reside in /src/" + modelFilePath);
        }
        return jprLocation;
    }

    /**
     * Gets the product name from the file path
     *
     * @param uiFilePath
     * @return
     */
    public static String getProductName(String uiFilePath) {
        int productIndex = 5;
        // file path: /ade/<view_name>/fusionapps/fin/components/ledger/gl
        if (uiFilePath.contains("/fusionapps/")) {
            int fusionappsIndex = uiFilePath.indexOf("/fusionapps/");
            String path_after_fusionapps = uiFilePath.substring(fusionappsIndex);
            if (!isEmpty(path_after_fusionapps)) {
                String[] parts = path_after_fusionapps.split("/");
                if (parts != null & parts.length > productIndex) 
                    return parts[productIndex];
            }
        } else if (uiFilePath.contains("/fsm/")) {
            int fusionappsIndex = uiFilePath.indexOf("/fsm/");
            productIndex = 4;
            String path_after_fusionapps = uiFilePath.substring(fusionappsIndex);
            if (!isEmpty(path_after_fusionapps)) {
                String[] parts = path_after_fusionapps.split("/");
                if (parts != null & parts.length > productIndex)
                    return parts[productIndex];
            }            
        }else if (uiFilePath.contains("/atgpf/")) {
            int fusionappsIndex = uiFilePath.indexOf("/atgpf/");
            productIndex = 4;
            String path_after_fusionapps = uiFilePath.substring(fusionappsIndex);
            if (!isEmpty(path_after_fusionapps)) {
                String[] parts = path_after_fusionapps.split("/");
                if (parts != null & parts.length > productIndex)
                    return parts[productIndex];
            }            
        } 
        else if(uiFilePath.contains("/emcore/")) {
            int fusionappsIndex = uiFilePath.indexOf("/emcore/");
            productIndex = 2;
            String path_after_fusionapps = uiFilePath.substring(fusionappsIndex);
            if (!isEmpty(path_after_fusionapps)) {
                String[] parts = path_after_fusionapps.split("/");
                if (parts != null & parts.length > productIndex)
                    return parts[productIndex];
            }           
        }
        return "";
    }


    public static HashMap<String, ArrayList<String>> addToMap(HashMap<String, ArrayList<String>> mapToAddTo,
                                                              String key, String value) {
        if (mapToAddTo == null) {
            mapToAddTo = new HashMap<String, ArrayList<String>>();
        }
        if (mapToAddTo.containsKey(key)) {
            ArrayList<String> listValues = mapToAddTo.get(key);
            listValues.add(value);
            mapToAddTo.put(key, listValues);
        } else {
            ArrayList<String> listValues = new ArrayList<String>();
            listValues.add(value);
            mapToAddTo.put(key, listValues);

        }
        return mapToAddTo;
    }


    public static HashMap<String, Integer> addToMap(HashMap<String, Integer> mapToAddTo,
                                                    HashMap<String, Integer> mapToAdd) {
        Set<String> keySet = mapToAdd.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String key = keyIter.next();
            Integer cntToAdd = mapToAdd.get(key);
            if (mapToAddTo.containsKey(key)) {
                Integer cnt = mapToAddTo.get(key);
                cnt = cnt + cntToAdd;
                mapToAddTo.put(key, cnt);

            } else {
                mapToAddTo.put(key, cntToAdd);
            }
        }

        return mapToAddTo;
    }

    public static HashMap<String, ArrayList<TuningAnalysisRecord>> addToMap(HashMap<String, ArrayList<TuningAnalysisRecord>> mapToAddTo,
                                                                            String key, TuningAnalysisRecord record) {
        if (mapToAddTo == null) {
            mapToAddTo = new HashMap<String, ArrayList<TuningAnalysisRecord>>();
        }
        if (mapToAddTo.containsKey(key)) {
            ArrayList<TuningAnalysisRecord> recordList = mapToAddTo.get(key);
            recordList.add(record);
            mapToAddTo.put(key, recordList);
        } else {
            ArrayList<TuningAnalysisRecord> recordList = new ArrayList<TuningAnalysisRecord>();
            recordList.add(record);
            mapToAddTo.put(key, recordList);

        }
        return mapToAddTo;
    }

    public static HashMap<String, ArrayList<TuningAnalysisRecord>> getOnlyMultipleUsages(HashMap<String, ArrayList<TuningAnalysisRecord>> allUsages) {
        if (allUsages == null) {
            return null;
        }
        HashMap<String, ArrayList<TuningAnalysisRecord>> onlyviolations =
            new HashMap<String, ArrayList<TuningAnalysisRecord>>();
        Set<String> bcset = allUsages.keySet();
        Iterator<String> bcIter = bcset.iterator();
        while (bcIter.hasNext()) {
            String bc = bcIter.next();
            ArrayList<TuningAnalysisRecord> records = allUsages.get(bc);
            if (records != null && records.size() > 1) {
                onlyviolations.put(bc, records);
            }
        }
        return onlyviolations;
    }

}
