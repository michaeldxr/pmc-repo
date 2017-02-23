package oracle.apps.di.checker;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.HashSet;
import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.di.ADFDiWorkbook;
import oracle.apps.di.CheckerConstants;
import oracle.apps.di.SharedStringsParser;
import oracle.apps.di.metadata.ADFDiMetadata;
import oracle.apps.di.metadata.WorkbookMetadata;
import oracle.apps.di.metadata.Worksheet;
import oracle.apps.helpers.FamilyModuleHelper;

public class WorkbookChecker extends DIGenericStandardsChecker {
    HashMap<String, ArrayList<String>> mapWorkbookIds =
        new HashMap<String, ArrayList<String>>();
    boolean workbookIdCheck = false;
    boolean workbookErrorsCheck = false;
    boolean workbookCustomizationCheck = false;

    public WorkbookChecker() {
        super();
    }

    @Override
    protected ArrayList<CheckerResult> doCheck(ADFDiWorkbook workbook,
                                               boolean getOnlyFailedResults) throws Exception {
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        ArrayList<ADFDiMetadata> sheetMetas =
            workbook.getComponentsOfType(ADFDiMetadata.META_WORKSHEET);

        HashSet<String> pageDefs = new HashSet<String>();
        Iterator<ADFDiMetadata> sheetIter = sheetMetas.iterator();
        String pdefName = null;
        while (sheetIter.hasNext()) {
            ADFDiMetadata meta = sheetIter.next();
            if (meta instanceof Worksheet) {
                Worksheet sheetMeta = (Worksheet)meta;
                pdefName = sheetMeta.getPageDefName();

            }
        }

        if (null == pdefName || "".equals(pdefName))
            this.setWorkbookErrorsCheck(false);
        if (workbookIdCheck)
            logWorkbookIdUsage(workbook);
        if (workbookErrorsCheck)
            results.addAll(checkForWorksheetErrors(workbook));
        if (workbookCustomizationCheck)
            results.addAll(checkForWorkbookCustomization(workbook));
        return results;
    }

    private ArrayList<CheckerResult> checkForWorksheetErrors(ADFDiWorkbook workbook) {
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        try {
            String sharedStringsContent =
                SharedStringsParser.getSharedStringsContent(workbook.getSpreadsheetPath());
            return checkForWorksheetErrors(sharedStringsContent);
        } catch (Exception ex) {

        }
        return results;
    }

    private ArrayList<CheckerResult> checkForWorksheetErrors(String content) {

        String code = "W_2";
        String issue = CheckerConstants.ISSUE_WorkbookErrors;
        String subIssue = "WorksheetErrors must be displayed";
        String subIssue1 =
            "No DoubleClickActionSet calling DisplayWorksheetErrors";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        String regEx = "#\\{worksheet\\.errors\\}";
        String regExDoubleClk = "DisplayWorksheetErrors";
        try {
            Pattern worksheetErrPattern = Pattern.compile(regEx);
            Matcher m = worksheetErrPattern.matcher(content);
            if (!m.find()) {

                results.add(new CheckerResult(code, issue, subIssue,
                                              "WorksheetErrors must be displayed",
                                              false));
            } else {
                //WorksheetErrors is found is spreadsheet. Now Checking whether DoubleClickActionSet is configured to call DisplayWorksheetErrors
                Pattern DisplayWorksheetErrors =
                    Pattern.compile(regExDoubleClk);
                Matcher m1 = DisplayWorksheetErrors.matcher(content);
                if (!m1.find()) {
                    results.add(new CheckerResult(code, issue, subIssue1,
                                                  "If WorksheetErrors is displayed in the worksheet, you should have a DoubleClickActionSet calling DisplayWorksheetErrors",
                                                  false));
                }
            }

        } catch (Exception ex) {
        }

        return results;
    }

    private ArrayList<CheckerResult> checkForWorkbookCustomization(ADFDiWorkbook workbook) {
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        try {
            ArrayList<ADFDiMetadata> workbookMeta =
                workbook.getComponentsOfType(ADFDiMetadata.META_WORKBOOK);
            if (workbookMeta != null && workbookMeta.size() > 0) {
                Iterator<ADFDiMetadata> workbookMetaIter =
                    workbookMeta.iterator();
                while (workbookMetaIter.hasNext()) {
                    ADFDiMetadata meta = workbookMetaIter.next();
                    if (meta == null)
                        continue;
                    if (meta instanceof WorkbookMetadata) {
                        boolean custEnabled =
                            ((WorkbookMetadata)meta).getCustomizationEnabled();
                        if (!custEnabled) {
                            results.add(new CheckerResult("W_3",
                                                          CheckerConstants.ISSUE_CUSTOMIZATIONENABLED,
                                                          "Worbook customization must be enabled",
                                                          "CustomizationEnabled in workbook properties must be True",
                                                          false));
                        }
                    }
                }
            }
        } catch (Exception ex) {
        }
        return results;
    }

    private void logWorkbookIdUsage(ADFDiWorkbook workbook) {
        ArrayList<ADFDiMetadata> workbookMeta =
            workbook.getComponentsOfType(ADFDiMetadata.META_WORKBOOK);
        if (workbookMeta == null || workbookMeta.size() < 1)
            return;

        Iterator<ADFDiMetadata> workbookMetaIter = workbookMeta.iterator();
        while (workbookMetaIter.hasNext()) {
            ADFDiMetadata meta = workbookMetaIter.next();
            if (meta == null)
                continue;
            if (meta instanceof WorkbookMetadata) {
                String wbId = ((WorkbookMetadata)meta).getWorkbookId();
                if (wbId != null && !wbId.trim().equals("")) {
                    logUsage(wbId, workbook.getSpreadsheetPath());
                }
            }
        }
    }

    private void logUsage(String workbookId, String absPath) {
        try {
            // Only do this check for published spreadsheets that are in public_html
            if (!absPath.contains("/public_html/"))
                return;
            if (mapWorkbookIds.containsKey(workbookId)) {
                ArrayList<String> paths = mapWorkbookIds.get(workbookId);
                if (!isAlreadyPresentInList(paths, absPath)) {
                    paths.add(absPath);
                    mapWorkbookIds.put(workbookId, paths);
                }
            } else {
                ArrayList<String> paths = new ArrayList<String>();
                paths.add(absPath);
                mapWorkbookIds.put(workbookId, paths);
            }

        } catch (Exception ex) {
        }

    }

    private boolean isAlreadyPresentInList(ArrayList<String> absPathList,
                                           String absPath) {
        try {
            String relativePath =
                FamilyModuleHelper.getRelativeFileName(absPath);
            String fileName = getFileName(absPath);
            if (fileName == null)
                return false;
            fileName = fileName.trim();
            if (fileName.endsWith("DT"))
                fileName = fileName.substring(0, fileName.length() - 2);
            if (relativePath == null)
                return false;
            relativePath = relativePath.trim();
            if (absPathList == null || absPathList.size() < 1)
                return false;
            Iterator<String> pathIter = absPathList.iterator();
            while (pathIter.hasNext()) {
                String pathToCheck = pathIter.next();
                String relPathToCheck =
                    FamilyModuleHelper.getRelativeFileName(pathToCheck);
                if (relPathToCheck != null &&
                    relPathToCheck.trim().equals(relativePath))
                    return true;
                String fileNametoCheck = getFileName(pathToCheck);
                if (fileNametoCheck == null)
                    continue;
                if (fileNametoCheck.endsWith("DT"))
                    fileNametoCheck =
                            fileNametoCheck.substring(0, fileNametoCheck.length() -
                                                      2);
                if (fileNametoCheck.equalsIgnoreCase(fileName))
                    return true;
            }
        } catch (Exception ex) {
        }
        return false;
    }

    private String getFileName(String path) {
        if (path == null)
            return "";
        //Dont really need a check for -1, because when we substring we will do a +1, so it will make it 0
        int fileNameStart = path.lastIndexOf("/");

        int extensionIndex = path.lastIndexOf(".");
        if (extensionIndex < 0)
            extensionIndex = path.length() - 1;
        return path.substring(fileNameStart + 1, extensionIndex);


    }

    public HashMap<String, ArrayList<CheckerResult>> getWorkbookIdViolationsByFile() {
        String issue = CheckerConstants.ISSUE_WBKID;
        String code = "W_1";
        String subIssue = "WorkbookID Must be globally unique";
        if (mapWorkbookIds == null || mapWorkbookIds.size() < 1)
            return null;
        HashMap<String, ArrayList<CheckerResult>> results =
            new HashMap<String, ArrayList<CheckerResult>>();

        Iterator<String> workbookIdIter = mapWorkbookIds.keySet().iterator();
        while (workbookIdIter.hasNext()) {
            String workbookId = workbookIdIter.next();
            ArrayList<String> filePaths = mapWorkbookIds.get(workbookId);
            // cut of for angry birds table is 1000. so we should make sure we stay within this limit
            String allFiles = getAllFilesString(filePaths);

            if (filePaths != null && filePaths.size() > 1) {
                // violation: more than one file references the same id.
                // Challenge. The two files might actually be the same workbook design vs published
                // Mark a violation against each of the files
                Iterator<String> fileIter = filePaths.iterator();
                while (fileIter.hasNext()) {
                    String fpath = fileIter.next();
                    CheckerResult viol =
                        new CheckerResult(code, issue, subIssue,
                                          String.format("Same workbookId %s present in files: %s",
                                                        workbookId, allFiles),
                                          false);
                    addToMap(results, fpath, viol);

                }

            }
        }
        return results;
    }

    private String getAllFilesString(ArrayList<String> listStrings) {
        if (listStrings == null || listStrings.size() < 1)
            return "";
        String delimiter = "#";
        int maxCutOff = 900;
        StringBuffer entirePathString = new StringBuffer();
        try {
            Iterator<String> listStringsIter = listStrings.iterator();
            StringBuffer sbFileNames = new StringBuffer();
            int cnt = 0;
            while (listStringsIter.hasNext()) {
                cnt++;
                String filePath = listStringsIter.next();
                if (cnt > 1) {
                    sbFileNames.append(delimiter);
                    entirePathString.append(delimiter);
                }
                sbFileNames.append(getFileName(filePath));
                entirePathString.append(FamilyModuleHelper.getRelativeFileName(filePath));
            }

            String strEntirePathString = entirePathString.toString();
            String fileNameString = sbFileNames.toString();
            if (strEntirePathString.length() <= maxCutOff)
                return strEntirePathString;

            return fileNameString;
        } catch (Exception ex) {
        }
        return listStrings.toString().replaceAll(",", "#");
    }

    public void addToMap(HashMap<String, ArrayList<CheckerResult>> mapResults,
                         String filePath, CheckerResult checkerResult) {
        if (mapResults.containsKey(filePath)) {
            ArrayList<CheckerResult> existingResults =
                mapResults.get(filePath);
            existingResults.add(checkerResult);
            mapResults.put(filePath, existingResults);
        } else {
            ArrayList<CheckerResult> existingResults =
                new ArrayList<CheckerResult>();
            existingResults.add(checkerResult);
            mapResults.put(filePath, existingResults);
        }
    }

    public void setWorkbookIdCheck(boolean workbookIdCheck) {
        this.workbookIdCheck = workbookIdCheck;
    }

    public boolean isWorkbookIdCheck() {
        return workbookIdCheck;
    }

    public void setWorkbookErrorsCheck(boolean workbookErrorsCheck) {
        this.workbookErrorsCheck = workbookErrorsCheck;
    }

    public boolean isWorkbookErrorsCheck() {
        return workbookErrorsCheck;
    }
    
    public void setWorkbookCustomizationCheck(boolean workbookCustomizationCheck){
        this.workbookCustomizationCheck = workbookCustomizationCheck;
    }
    
    public boolean isWorkbookCustomizationCheck(){
        return this.workbookCustomizationCheck;
    }
}
