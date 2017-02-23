package oracle.apps.di.checker;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import oracle.apps.di.ADFDiWorkbook;


public abstract class DIGenericStandardsChecker {
    String _spreadsheetPath = "";
    HashMap<String, ArrayList<String>> exceptions = new HashMap<String, ArrayList<String>>();

    public DIGenericStandardsChecker() {
    }
    //    public DIGenericStandardsChecker(String spreadsheetPath) throws Exception {
    //        super();
    //
    //    }

    public void init(String spreadsheetPath) throws Exception {
        if (spreadsheetPath == null || spreadsheetPath.trim().equals(""))
            throw new Exception("DIGenericStandardsChecker: Spreadhseet Path not specified");
        if (!doesFileExist(spreadsheetPath))
            throw new Exception("DIGenericStandardsChecker: File does not exist: " + spreadsheetPath);
        _spreadsheetPath = spreadsheetPath;
    }

    public boolean doesFileExist(String path) {
        File f = new File(path);
        if (f == null || !f.exists())
            return false;
        return true;
    }

    public String getExtension() {
        int extensionIndex = _spreadsheetPath.lastIndexOf(".");
        if (extensionIndex == -1)
            return "";
        return _spreadsheetPath.substring(extensionIndex + 1);
    }

    public String getFileNameWithoutExt() {
        int extensionIndex = _spreadsheetPath.lastIndexOf(".");
        if (extensionIndex == -1)
            return "";
        int fileNameBeginIndex = _spreadsheetPath.lastIndexOf("/");
        if (fileNameBeginIndex == -1)
            fileNameBeginIndex = 0;
        else
            fileNameBeginIndex = fileNameBeginIndex + 1;
        return _spreadsheetPath.substring(fileNameBeginIndex, extensionIndex);

    }

    public boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    public ArrayList<CheckerResult> check(String path, ADFDiWorkbook workbook, boolean getOnlyFailedResults) {

        try {
            init(path);
        } catch (Exception ex) {
            System.out.println("Exception initializing Spreadsheet: " + path);
            ex.printStackTrace();
        }
        if (isEmpty(_spreadsheetPath)) {
            System.out.println("==> Error .. Spreadsheet path is empty");
        }
       // System.out.println("Checking Spreadsheet : " + getSpreadsheetPath() + " for standard : " + getStandardName());

        try {
            return doCheck(workbook, getOnlyFailedResults);
        } catch (Exception ex) {
            addException(_spreadsheetPath, getExceptionString(String.format("[%s]- %s", getClass().getName(), ex.getMessage()), ex));
        }
        return null;
    }

    public void addException(String path, String exceptionString) {
        if (exceptions.containsKey(path)) {
            ArrayList<String> listExceptios = exceptions.get(path);
            listExceptios.add(exceptionString);
            exceptions.put(path, listExceptios);
        } else {
            ArrayList<String> listExceptios = new ArrayList<String>();
            listExceptios.add(exceptionString);
            exceptions.put(path, listExceptios);
        }
    }

    private String getExceptionString(String msg, Exception ex) {
        if (ex == null)
            return "";
        StringBuffer sb = new StringBuffer();
        sb.append(msg + "\n");
        sb.append(ex.getMessage() + "\n");
        StackTraceElement[] stacks = ex.getStackTrace();
        if (stacks == null)
            return sb.toString();
        for (int i = 0; i < stacks.length; i++) {
            sb.append(stacks[i].toString() + "\n");
        }
        return sb.toString();
    }

    protected abstract ArrayList<CheckerResult> doCheck(ADFDiWorkbook workbook, boolean getOnlyFailedResults) throws Exception;

    public String getStandardName() {
        return "";
    }

    public String getStandardCode() {
        return "";
    }

    public String getSpreadsheetPath() {
        return _spreadsheetPath;
    }

    public void printResults(ArrayList<CheckerResult> results) {
        if (results == null || results.size() < 1)
            System.out.println("There are no results to print");
        Iterator<CheckerResult> result = results.iterator();
        while (result.hasNext())
            result.next().print();
    }

    public void addResult(ArrayList<CheckerResult> results, CheckerResult result, boolean addOnlyFailedResults) {
        if (!addOnlyFailedResults) {
            results.add(result);
            return;
        }
        if (!result.checkerPassed)
            results.add(result);

    }
    public void addResults(ArrayList<CheckerResult> results, ArrayList<CheckerResult> resultsToAdd) {
        if(resultsToAdd==null || resultsToAdd.size()<1)
            return;
        results.addAll(resultsToAdd);
    }
    public HashMap<String, ArrayList<String>> getExceptions() {
        return exceptions;
    }
}
