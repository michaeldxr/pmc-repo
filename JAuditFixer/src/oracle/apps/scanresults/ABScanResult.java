package oracle.apps.scanresults;

import java.io.BufferedWriter;
import java.io.FileWriter;

import java.util.ArrayList;

import java.util.Iterator;

import oracle.apps.helpers.FamilyModuleHelper;

public class ABScanResult {
    String _fileFullPath = "";
    String _fileRelativePath = "";
    String _fileName = "";
    String _label = "";

    String _family = "";
    String _product = "";
    String _module = "";

    String issue = "";
    String subIssue = "";
    String description = "";


    public ABScanResult(String path) {
        super();
        _fileFullPath = path;
        try {
            _fileName = FamilyModuleHelper.getFileNameFromPath(path);
            _family = FamilyModuleHelper.getFamily(path);
            _product = FamilyModuleHelper.getProduct(path);
            _fileRelativePath = FamilyModuleHelper.getRelativeFileName(path);
            _label = FamilyModuleHelper.getLabelInfo(path);
            _module = FamilyModuleHelper.getModule(path);
        } catch (Exception ex) {
        }

    }

    public void setFileFullPath(String _fileFullPath) {
        this._fileFullPath = _fileFullPath;
    }

    public String getFileFullPath() {
        return _fileFullPath;
    }

    public void setFileRelativePath(String _fileRelativePath) {
        this._fileRelativePath = _fileRelativePath;
    }

    public String getFileRelativePath() {
        return _fileRelativePath;
    }

    public void setFileName(String _fileName) {
        this._fileName = _fileName;
    }

    public String getFileName() {
        return _fileName;
    }

    public void setLabel(String _label) {
        this._label = _label;
    }

    public String getLabel() {
        return _label;
    }

    public void setFamily(String _family) {
        this._family = _family;
    }

    public String getFamily() {
        return _family;
    }

    public void setProduct(String _product) {
        this._product = _product;
    }

    public String getProduct() {
        return _product;
    }

    public void setModule(String _module) {
        this._module = _module;
    }

    public String getModule() {
        return _module;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getIssue() {
        return issue;
    }

    public void setSubIssue(String subIssue) {
        this.subIssue = subIssue;
    }

    public String getSubIssue() {
        return subIssue;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static void writeResults(ArrayList<ABScanResult> results, String fileName) throws Exception {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(fileName));
            bw.write("FAMILY,MODULE,PRODUCT,FILENAME,Label,ISSUETYPE,SUB_ISSUE,DESCRIPTION\n");
            if (results != null && results.size() > 0) {
                Iterator<ABScanResult> resultIter = results.iterator();
                while (resultIter.hasNext()) {
                    ABScanResult r = resultIter.next();
                    StringBuffer line = new StringBuffer();
                    line.append(((r.getFamily() == null) ? "" : r.getFamily()) + ",");
                    line.append(((r.getModule() == null) ? "" : r.getModule()) + ",");
                    line.append(((r.getProduct() == null) ? "" : r.getProduct()) + ",");
                    line.append(((r.getFileRelativePath() == null) ? "" : r.getFileRelativePath()) + ",");
                    line.append(((r.getLabel() == null) ? "" : r.getLabel()) + ",");
                    line.append(((r.getIssue() == null) ? "" : r.getIssue()) + ",");
                    line.append(((r.getSubIssue() == null) ? "" : r.getSubIssue()) + ",");
                    line.append(((r.getDescription() == null) ? "" : r.getDescription()) + "\n");

                    bw.write(line.toString());
                }
            }
        } catch (Exception ex) {
        } finally {
            if (bw != null)
                bw.close();
        }
    }
}
