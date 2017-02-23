package oracle.apps.lovWidth;

public class VOFixDetails {
    String voPackage = "";;
    String voFullPath ="" ;
    String attributeName="";
    String origAttributeWidth;
    String newAttributeWidth;
    String status="";
    public VOFixDetails() {
        super();
    }

    public void setVoPackage(String voPackage) {
        this.voPackage = voPackage;
    }

    public String getVoPackage() {
        return voPackage;
    }

    public void setVoFullPath(String voFullPath) {
        this.voFullPath = voFullPath;
    }

    public String getVoFullPath() {
        return voFullPath;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setOrigAttributeWidth(String origAttributeWidth) {
        this.origAttributeWidth = origAttributeWidth;
    }

    public String getOrigAttributeWidth() {
        return origAttributeWidth;
    }

    public void setNewAttributeWidth(String newAttributeWidth) {
        this.newAttributeWidth = newAttributeWidth;
    }

    public String getNewAttributeWidth() {
        return newAttributeWidth;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
