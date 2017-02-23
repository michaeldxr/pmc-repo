package oracle.apps.lovWidth;

import java.util.ArrayList;

/**
 * Producer LOV. This class represents a ProducerLOV:
 * ProducerLOV -> the VO that is responsible for the LOV
 */
public class ProducerLOV {
    String family;
    String displayAttr;
    String displayWidth;
    String columnWidth;
    String product;
    String bcPackage;
    String bcName;
    String bcProjectName;
    ArrayList<ConsumerVO> consumers = new ArrayList<ConsumerVO>();
    String lovType;
    String propertySetName = "";
    String propertySetDWidth = "";

    String secondaryBcName = "";
    String secondaryBcPackage = "";
    String secondaryBcAttr = "";
    String secondaryBcAttrWidth = "";
    String secondaryColWidth = "";
    String secondaryBcAttrPSName = "";
    String secondaryBcAttrPSDWidth = "";
    String secondaryBcProduct = "";

    public ProducerLOV() {
        super();
    }

    public void setDisplayAttr(String displayAttr) {
        this.displayAttr = displayAttr;
    }

    public String getDisplayAttr() {
        return displayAttr;
    }

    public void setDisplayWidth(String displayWidth) {
        this.displayWidth = displayWidth;
    }

    public String getDisplayWidth() {
        return displayWidth;
    }


    public void setBcPackage(String bcPackage) {
        this.bcPackage = bcPackage;
    }

    public String getBcPackage() {
        return bcPackage;
    }

    public void setBcName(String bcName) {
        this.bcName = bcName;
    }

    public String getBcName() {
        return bcName;
    }

    public void setBcProjectName(String bcProjectName) {
        this.bcProjectName = bcProjectName;
    }

    public String getBcProjectName() {
        return bcProjectName;
    }

    public void setConsumers(ArrayList<ConsumerVO> consumers) {
        this.consumers = consumers;
    }

    public ArrayList<ConsumerVO> getConsumers() {
        return consumers;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getFamily() {
        return family;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getProduct() {
        return product;
    }

    public void setLovType(String lovType) {
        this.lovType = lovType;
    }

    public String getLovType() {
        return lovType;
    }

    public void setSecondaryBcName(String secondaryBcName) {
        this.secondaryBcName = secondaryBcName;
    }

    public String getSecondaryBcName() {
        return secondaryBcName;
    }

    public void setSecondaryBcPackage(String secondaryBcPackage) {
        this.secondaryBcPackage = secondaryBcPackage;
    }

    public String getSecondaryBcPackage() {
        return secondaryBcPackage;
    }

    public void setSecondaryBcAttr(String secondaryBcAttr) {
        this.secondaryBcAttr = secondaryBcAttr;
    }

    public String getSecondaryBcAttr() {
        return secondaryBcAttr;
    }

    public void setSecondaryBcAttrWidth(String secondaryBcAttrWidth) {
        this.secondaryBcAttrWidth = secondaryBcAttrWidth;
    }

    public String getSecondaryBcAttrWidth() {
        return secondaryBcAttrWidth;
    }

    public void setPropertySetName(String propertySetName) {
        this.propertySetName = propertySetName;
    }

    public String getPropertySetName() {
        return propertySetName;
    }

    public void setPropertySetDWidth(String propertySetDWidth) {
        this.propertySetDWidth = propertySetDWidth;
    }

    public String getPropertySetDWidth() {
        return propertySetDWidth;
    }

    public void setSecondaryBcAttrPSName(String secondaryBcAttrPSName) {
        this.secondaryBcAttrPSName = secondaryBcAttrPSName;
    }

    public String getSecondaryBcAttrPSName() {
        return secondaryBcAttrPSName;
    }

    public void setSecondaryBcAttrPSDWidth(String secondaryBcAttrPSDWidth) {
        this.secondaryBcAttrPSDWidth = secondaryBcAttrPSDWidth;
    }

    public String getSecondaryBcAttrPSDWidth() {
        return secondaryBcAttrPSDWidth;
    }

    public void setColumnWidth(String columnWidth) {
        this.columnWidth = columnWidth;
    }

    public String getColumnWidth() {
        return columnWidth;
    }

    public void setSecondaryColWidth(String secondaryColWidth) {
        this.secondaryColWidth = secondaryColWidth;
    }

    public String getSecondaryColWidth() {
        return secondaryColWidth;
    }

    public boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    public Long convertStrToLong(String str) {
        if (isEmpty(str))
            return 0L;
        str = str.trim();
        try {
            return Long.parseLong(str);
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0L;
        }
    }

    public Long getEffectiveAttributeWidth() {

        String voWidth = this.getDisplayWidth();
        if (!isEmpty(voWidth)) {
            return convertStrToLong(voWidth);
        }

        voWidth = this.getPropertySetDWidth();
        if (!isEmpty(voWidth)) {
            return convertStrToLong(voWidth);
        }

        String eoWidth = this.getSecondaryBcAttrWidth();
        if (!isEmpty(eoWidth)) {
            return convertStrToLong(eoWidth);
        }

        eoWidth = this.getSecondaryBcAttrPSDWidth();
        if (!isEmpty(eoWidth)) {
            return convertStrToLong(eoWidth);
        }

        eoWidth = this.getSecondaryColWidth();
        if (!isEmpty(eoWidth)) {
            return convertStrToLong(eoWidth);
        }


        String vocolWidth = this.getColumnWidth();
        if (!isEmpty(vocolWidth)) {
            return convertStrToLong(vocolWidth);
        }

        return -1L;
    }

    public void setSecondaryBcProduct(String secondaryBcProduct) {
        this.secondaryBcProduct = secondaryBcProduct;
    }

    public String getSecondaryBcProduct() {
        return secondaryBcProduct;
    }
}
