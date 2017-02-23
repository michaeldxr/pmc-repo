package oracle.apps.ppr;

  public class ScanResult {
    private String jar_ui;
    private String jar_vo;
    private String jar_am;
    private String jar_eo;
    private String page;
    private String type;
    private String tableId;
    private String Iterator;
    private String cpx;
    private String AMPackage;
    private String binds;
    private String AM;
    private String VO;
    private String listBindingName;
    private String ViewAccessor;
    private String va_VO;
    private String description;
    private String pageDef;
    private String dataControl;  
    private String EntityObject;
    private String attr;
    private Boolean hasValidation = false;

    public void setJar_ui(String jar) {
      this.jar_ui = jar;
    }

    public String getJar_ui() {
      return jar_ui;
    }

    public void setPage(String page) {
      this.page = page;
    }

    public String getPage() {
      return page;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getType() {
      return type;
    }

    public void setTableId(String tableId) {
      this.tableId = tableId;
    }

    public String getTableId() {
      return tableId;
    }

    public void setIterator(String binds) {
      this.Iterator = binds;
    }

    public String getIterator() {
      return Iterator;
    }

    public void setCpx(String cpx) {
      this.cpx = cpx;
    }

    public String getCpx() {
      return cpx;
    }

    public void setAMPackage(String AMPackage) {
      this.AMPackage = AMPackage;
    }

    public String getAMPackage() {
      return AMPackage;
    }

    public void setBinds(String binds) {
      this.binds = binds;
    }

    public String getBinds() {
      return binds;
    }

    public void setAM(String AM) {
      this.AM = AM;
    }

    public String getAM() {
      return AM;
    }

    public void setVO(String VO) {
      this.VO = VO;
    }

    public String getVO() {
      return VO;
    }

    public void setListBindingName(String listBindingName) {
      this.listBindingName = listBindingName;
    }

    public String getListBindingName() {
      return listBindingName;
    }

    public void setViewAccessor(String ViewAccessor) {
      this.ViewAccessor = ViewAccessor;
    }

    public String getViewAccessor() {
      return ViewAccessor;
    }

    public void setVa_VO(String va_VO) {
      this.va_VO = va_VO;
    }

    public String getVa_VO() {
      return va_VO;
    }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public void setPageDef(String pageDef) {
    this.pageDef = pageDef;
  }

  public String getPageDef() {
    return pageDef;
  }

  public void setDataControl(String dataControl) {
    this.dataControl = dataControl;
  }

  public String getDataControl() {
    return dataControl;
  }

  public void setJar_vo(String jar_model) {
    this.jar_vo = jar_model;
  }

  public String getJar_vo() {
    return jar_vo;
  }

  public void setJar_am(String jar_am) {
    this.jar_am = jar_am;
  }

  public String getJar_am() {
    return jar_am;
  }

  public void setEntityObject(String EntityObject) {
    this.EntityObject = EntityObject;
  }

  public String getEntityObject() {
    return EntityObject;
  }

  public void setJar_eo(String jar_eo) {
    this.jar_eo = jar_eo;
  }

  public String getJar_eo() {
    return jar_eo;
  }

    public void setAttr(String attr) {
        this.attr = attr;
    }

    public String getAttr() {
        return attr;
    }

    public void setHasValidation(Boolean hasValidation) {
        this.hasValidation = hasValidation;
    }

    public Boolean isHasValidation() {
        return hasValidation;
    }
}
