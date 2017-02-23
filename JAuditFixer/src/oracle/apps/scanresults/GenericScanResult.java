package oracle.apps.scanresults;

import oracle.apps.helpers.FamilyModuleHelper;

public class GenericScanResult {
    String _fileFullPath = "";
    String _relativePath = "";
    String _fileName = "";
    String _product = "";
    String _module = "";
    String _family = "";
    String _label = "";
    String _lineNumber = "";
    String _issue = "";
    String _message = "";

    public GenericScanResult(String path) {
        super();
        _fileFullPath = path;
        try {
            _fileName = getFileNameFromPath(path);
            _family = FamilyModuleHelper.getFamily(path);
            _product = FamilyModuleHelper.getProduct(path);
            _relativePath = FamilyModuleHelper.getRelativeFileName(path);
            _label = FamilyModuleHelper.getLabelInfo(path);
            _module=FamilyModuleHelper.getModule(path);
        } catch (Exception ex) {
        }

    }

    public String getFileNameFromPath(String path) {
        if (isEmpty(path))
            return "";
        int indexExtension = path.lastIndexOf(".");
        if (indexExtension == -1)
            indexExtension = path.length() - 1;
        int fileNameStart = path.lastIndexOf("/");

        return path.substring(fileNameStart + 1, indexExtension);
    }

    public String getExtension(String path) {
        if (isEmpty(path))
            return "";
        int indexExtension = path.lastIndexOf(".");
        if (indexExtension == -1)
            return "";
        return path.substring(indexExtension + 1);
    }

    public boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    public void setFileFullPath(String _fileFullPath) {
        this._fileFullPath = _fileFullPath;
    }

    public String getFileFullPath() {
        return _fileFullPath;
    }

    public void setRelativePath(String _relativePath) {
        this._relativePath = _relativePath;
    }

    public String getRelativePath() {
        return _relativePath;
    }

    public void setFileName(String _fileName) {
        this._fileName = _fileName;
    }

    public String getFileName() {
        return _fileName;
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

    public void setFamily(String _family) {
        this._family = _family;
    }

    public String getFamily() {
        return _family;
    }

    public void setLabel(String _label) {
        this._label = _label;
    }

    public String getLabel() {
        return _label;
    }

    public void setLineNumber(String _lineNumber) {
        this._lineNumber = _lineNumber;
    }

    public String getLineNumber() {
        return _lineNumber;
    }

    public void setIssue(String _issue) {
        this._issue = _issue;
    }

    public String getIssue() {
        return _issue;
    }

    public void setMessage(String _message) {
        this._message = _message;
    }

    public String getMessage() {
        return _message;
    }
}
