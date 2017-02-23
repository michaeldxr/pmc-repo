package oracle.apps.lovWidth;

import java.util.ArrayList;

public class ConsumerVO {
  private String lovUses;
  private String lovAttribute;
  private String voName;
  private String voPackage;
  private String uiFileName ;
  private String uiFileFullPath;
  private String product ;
  private String comboboxId;
  String family = "";
  ArrayList<ProducerLOV> _producers = new ArrayList<ProducerLOV>();
  public ConsumerVO() {
      super();
  }

  public void setLovUses(String lovUses) {
      this.lovUses = lovUses;
  }

  public String getLovUses() {
      return lovUses;
  }

  public void setLovAttribute(String lovAttribute) {
      this.lovAttribute = lovAttribute;
  }

  public String getLovAttribute() {
      return lovAttribute;
  }

  public void setVoName(String voName) {
      this.voName = voName;
  }

  public String getVoName() {
      return voName;
  }

  public void setVoPackage(String voPackage) {
      this.voPackage = voPackage;
  }

  public String getVoPackage() {
      return voPackage;
  }

    public void setUiFileName(String uiFileName) {
        this.uiFileName = uiFileName;
    }

    public String getUiFileName() {
        return uiFileName;
    }

    public void setUiFileFullPath(String uiFileFullPath) {
        this.uiFileFullPath = uiFileFullPath;
    }

    public String getUiFileFullPath() {
        return uiFileFullPath;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getProduct() {
        return product;
    }

    public void setComboboxId(String comboboxId) {
        this.comboboxId = comboboxId;
    }

    public String getComboboxId() {
        return comboboxId;
    }

    public void setFamily(String family) {
        this.family = family;
    }

    public String getFamily() {
        return family;
    }

    public void setProducers(ArrayList<ProducerLOV> _producers) {
        this._producers = _producers;
    }

    public ArrayList<ProducerLOV> getProducers() {
        return _producers;
    }
}
