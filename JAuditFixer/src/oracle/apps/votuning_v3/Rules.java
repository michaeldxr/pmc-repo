package oracle.apps.votuning_v3;

public class Rules {

  private static Integer ITERRANGESIZE_VOFETCH_OFFSET = 1;
  private static Integer AUTOHGT_ITERATOR_OFFSET = 1;
  public static Integer AUTOHGT_NULL = 15;
  private static Integer SELECTONE_LISTRANGESIZE = 250;
  private static Integer COMBOBOX_LISTRANGESIZE = 250;
  // no multiple usages check doen for these since they are both setting it to 16 it should not matter
  private static Integer SELECTONE_USAGE_FETCHSIZE = 16;
  private static Integer COMBOBOX_USAGE_FETCHSIZE = 16;

  public Rules() {
    super();
  }

  public static boolean validateLOVs(ScanResult result) {
    boolean isViolation = false;
    String description = result.getViewAccessor() + ": ";
    
    if(result.getViewAccessor() == null){
      return !isViolation;
    }
    
    //Rule 1: ListRangeSize = 250
//    if (result.getListRangeSize() != null) {
//      if (result.getType().equals("selectOneChoice") &&
//          !result.getListRangeSize().equals(SELECTONE_LISTRANGESIZE.toString())) {
//        description += "Fix List Range Size. ";
//        isViolation = true;
//        result.setNew_ListRangeSize(SELECTONE_LISTRANGESIZE);
//      } else if (result.getType().equals("inputComboboxListOfValues") &&
//                 !result.getListRangeSize().equals(COMBOBOX_LISTRANGESIZE.toString())) {
//        description += "Fix List Range Size. ";
//        isViolation = true;
//        result.setNew_ListRangeSize(COMBOBOX_LISTRANGESIZE);
//      }
//    } else {
//      description += "Fix List Range Size. ";
//      isViolation = true;
//      result.setNew_ListRangeSize(COMBOBOX_LISTRANGESIZE);
//    }
    
    //Rule 2: VA:FetchSize = 16 (at least)
    if (result.getType().equals("selectOneChoice")) {
      if (!result.getEffVOUsageFetchSize().equals("#NOT FOUND#") &&
          Integer.parseInt(result.getEffVOUsageFetchSize()) <
          SELECTONE_USAGE_FETCHSIZE) {
        description += "Fix View Accessor Fetch Size. ";
        isViolation = true;
        result.setNew_VUFetchSize(SELECTONE_USAGE_FETCHSIZE);
      }
    } else if (result.getType().equals("inputComboboxListOfValues")) {
      if (!result.getEffVOUsageFetchSize().equals("#NOT FOUND#") &&
          Integer.parseInt(result.getEffVOUsageFetchSize()) <
          COMBOBOX_USAGE_FETCHSIZE) {
        description += "Fix View Accessor Fetch Size. ";
        isViolation = true;
        result.setNew_VUFetchSize(COMBOBOX_USAGE_FETCHSIZE);
      }
    }
    
    //Check if the VO usage fetch size is less than the VO default fetch size.
    if(result.getVa_VO() != null){
      Integer VUFetchSize = null;
      
      if(result.getNew_VUFetchSize() == null)
        VUFetchSize = Integer.parseInt(result.getEffVOUsageFetchSize());
      else
        VUFetchSize = result.getNew_VUFetchSize();
      
      if(result.getVODefaultFetchSize() != null && VUFetchSize < result.getVODefaultFetchSize()){
        description += "Fix VO Default Fetch Size. ";
        isViolation = true;
        result.setNew_VODefaultFetchSize(VUFetchSize);
      }
    }
    
    
    if (isViolation){
      result.setDescription(description);
      isViolation = updateSrcPath(result);
    }

    return !isViolation;
  }

  public static void validateTables1stRun(ScanResult result) {
    Integer temp = null;
    Integer new_IterRangeSize = null;

    
    //Rule 1: Iterator.RangeSize = autoHeightRows + 1    
      try{
        if(result.getEffAutoHeightRows() == null)
            temp = AUTOHGT_NULL;
        else
            temp = Integer.parseInt(result.getEffAutoHeightRows());
        
        Integer max_autoHgt = ScanResultsAnalyzer.m_maxAutoHeight.get(result.getJar_ui() + result.getPageDef()+result.getIterator());
        
        //in case null value is found in the map.
        if(max_autoHgt == null){
          max_autoHgt = temp;
        }

        new_IterRangeSize = max_autoHgt + AUTOHGT_ITERATOR_OFFSET;
                                             
        //keep tracking the post-fix maxium iteratro range size        
        ScanResultsAnalyzer.saveMaxNum(ScanResultsAnalyzer.m_maxIterRangeSize, result.getJar_am() + result.getAM() + result.getBinds(),
                    new_IterRangeSize.toString());
        result.setNew_iterRangeSize(new_IterRangeSize);
       
      }catch(Exception e){
        e.printStackTrace();
      }



  }
  
  public static boolean validateTables2ndRun(ScanResult result) {
    

    
    boolean isViolation = false;
    String description = result.getTableId() + ": ";
    
    if(result.getIterator() == null){
      return !isViolation;
    }
    try{
      if(result.getIterRangeSize() == null || Integer.parseInt(result.getIterRangeSize()) != result.getNew_iterRangeSize()){   
          description += "Fix Iterator RangeSize. ";
          isViolation = true;
          result.setNew_iterRangeSize(result.getNew_iterRangeSize());
          if(result.getAutoHeightRows() != null && result.getAutoHeightRows().contains("#"))
            result.setNew_autoHeight(result.getEffAutoHeightRows());
      }else{
        result.setNew_iterRangeSize(null);
      }
      
      //Rule 2: VO Usage Fetch Size = Iterator.RangeSize + 1

      Integer max_IterRangeSize = ScanResultsAnalyzer.m_maxIterRangeSize.get(result.getJar_am() + result.getAM() +result.getBinds());
      
  //    if(max_IterRangeSize == null || max_IterRangeSize.equals(result.getNew_iterRangeSize())){
  //      max_IterRangeSize = result.getNew_iterRangeSize();
  //      ScanResultsAnalyzer.m_maxIterRangeSize.put(result.getJar_ui() + result.getPageDef()+result.getDataControl()+result.getBinds(), max_IterRangeSize);
  //    }
      
      if (!result.getEffVOUsageFetchSize().equals("#NOT FOUND#") &&
          Integer.parseInt(result.getEffVOUsageFetchSize()) != max_IterRangeSize + ITERRANGESIZE_VOFETCH_OFFSET)
      {
        description += "Fix VO Usage Fetch Size. ";
        isViolation = true;
        result.setNew_VUFetchSize(max_IterRangeSize + ITERRANGESIZE_VOFETCH_OFFSET);
      }
      
      //Check if the VO usage fetch size is less than the VO default fetch size.
      if(result.getVO() != null){
        Integer VUFetchSize = null;
        
        if(result.getNew_VUFetchSize() == null)
          VUFetchSize = Integer.parseInt(result.getEffVOUsageFetchSize());
        else
          VUFetchSize = result.getNew_VUFetchSize();
        
        if(result.getVODefaultFetchSize() != null && VUFetchSize < result.getVODefaultFetchSize()){
          description += "Fix VO Default Fetch Size. ";
          isViolation = true;
          result.setNew_VODefaultFetchSize(VUFetchSize);
        }
      }
    }catch(Exception e){
      e.printStackTrace();
    }
    
    if (isViolation){
      result.setDescription(description);
      isViolation = updateSrcPath(result);
    }

    return !isViolation;
  }
  
  private static boolean updateSrcPath(ScanResult result){
    String prefix;
    
    prefix = ScanResultsAnalyzer.mapping.get(result.getJar_ui());
    
    //skip it if src path is not found.
    if(prefix == null){
        return false;
    }
    else{
      result.setPage( prefix + "/public_html/" + result.getPage());
      result.setPageDef(prefix + "/adfmsrc/" + result.getPageDef().replaceAll("\\.", "/") + ".xml");
    }
    
    prefix = ScanResultsAnalyzer.mapping.get(result.getJar_vo());
    
    if(prefix == null){
        return false;
    }
    else{
      result.setVO(prefix + "/src/" + result.getVO().replaceAll("\\.", "/") + ".xml");
    }
    
    prefix = ScanResultsAnalyzer.mapping.get(result.getJar_am());
    
    if(prefix == null){
        return false;
    }
    else{
      result.setAM(prefix + "/src/" + result.getAM().replaceAll("\\.", "/") + ".xml");
    }
    
    if(result.getEntityObject() != null && result.getJar_eo() != null){
      prefix = ScanResultsAnalyzer.mapping.get(result.getJar_eo());
      
      if(prefix == null){
          return false;
      }
      else{
        result.setEntityObject(prefix + "/src/" + result.getEntityObject());
      }
    }
    
    return true;
  }
}
