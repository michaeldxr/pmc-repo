package oracle.apps.votuning_v4;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.votuning_v4.file_type.AppModuleUsage;
import oracle.apps.votuning_v4.file_type.BC4J_XCFG;
import oracle.apps.votuning_v4.file_type.DataControl;
import oracle.apps.votuning_v4.file_type.EntityObject;
import oracle.apps.votuning_v4.file_type.PageDef;
import oracle.apps.votuning_v4.file_type.PageDefUsages;
import oracle.apps.votuning_v4.file_type.PageMap;
import oracle.apps.votuning_v4.file_type.UIPageTunableElements;
import oracle.apps.votuning_v4.file_type.ViewObject;
import oracle.apps.votuning_v4.file_type.ViewUsage;


public class ScanResultsAnalyzer {

  private static ScanResult result;
  private static boolean isGoodRecord = true;
  private static boolean isSubAM = false;
  public static HashMap<String, Integer> m_maxAutoHeight =
    new HashMap<String, Integer>();
  public static HashMap<String, Integer> m_maxIterRangeSize =
    new HashMap<String, Integer>();
  public ArrayList<ScanResult> al_results = new ArrayList<ScanResult>();
  public static HashMap<String,String> mapping;
  private int count = 1;

  public ScanResultsAnalyzer(HashMap<String, String> jarSrcMapping) {
    super();
    mapping = jarSrcMapping;
  }

  public void AnalyzeResult() {

    UIPageTunableElements uit;

    Set<String> keySet = UIPageTunableElements.m_tunablePageElements.keySet();
    Iterator<String> iter = keySet.iterator();

    while (iter.hasNext()) {
      String key = iter.next();
      ArrayList<UIPageTunableElements> list =
        UIPageTunableElements.m_tunablePageElements.get(key);

      for (int i = 0; i < list.size(); i++) {
        uit = list.get(i);
        initReuslt(uit);
      }
    }  
  }

  public static void saveMaxNum(Map<String, Integer> m, String key, String value) {   
    if (value == null)
      return;

    Integer v = null;
     
    try {
      v = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      System.out.println("Unresolvable AutoHeight: " + value);
      return;
    }

    if (m.containsKey(key)) {
      if (m.get(key) < v)
        m.put(key, v);
    } else {
      m.put(key, v);
    }
  }

  private String getPageDefUsage(String pagePath) {

    PageMap pm = null;
    Iterator<String> iter = PageMap.getPageMap().keySet().iterator();

    while (iter.hasNext()) {
      String key = iter.next();
      ArrayList<PageMap> list = PageMap.getPageMap().get(key);

      for (int i = 0; i < list.size(); i++) {
        pm = list.get(i);

        if (pm.getPath().equals("/" + pagePath)) {
          result.setCpx(pm.getCpx());
          return pm.getUsageId();
        }
      }
    }

    return null;
  }

  private String getPageDef(String pageDefUsage) {
    Iterator<String> iter =
      PageDefUsages.m_pageDefUsageMap.keySet().iterator();

    while (iter.hasNext()) {
      String key = iter.next();
      PageDefUsages usage = PageDefUsages.m_pageDefUsageMap.get(key);

      if (usage.getId().equals(pageDefUsage))
        return usage.getPath();
    }

    return null;
  }

  private String getDataControl(String pageDef, String bindings) {
    PageDef.PageDefBinding pdi = null;

    if (pageDef != null)
      pageDef = pageDef.replaceAll("\\.", "/") + ".xml";

    ArrayList<PageDef.PageDefBinding> list =
      PageDef.m_pageDefBindings.get(pageDef);

    if (list != null) {
      for (int i = 0; i < list.size(); i++) {
        pdi = list.get(i);

        if (pdi.getSBindingID().equals(bindings)) {
          if (pdi.getSBindingType().equals("list") ||
              pdi.getSBindingType().equals("listOfValues"))
            result.setListBindingName(pdi.getSListBinding());
          if (pdi.getSBinds().contains("Root.")) 
            isSubAM = true;
          
          result.setBinds(pdi.getSBinds());
          
          result.setIterator(pdi.getSIterId());
          result.setIterRangeSize(pdi.getSRangeSize());
          
          return pdi.getSDataControl();
        }
      }
    }
    return null;
  }

  private String getAMUsage(String dataControl, String cpx) {
    DataControl dc = null;
    if (dataControl != null) {

      Iterator<String> iter = DataControl.m_dataControls.keySet().iterator();

      while (iter.hasNext()) {
        String key = iter.next();
        dc = DataControl.m_dataControls.get(key);

        if (dc.getSId().equals(dataControl) && dc.getSCpxPath().equals(cpx)) {
          result.setAMPackage(dc.getSPackage());
          return dc.getSConfiguration();
        }
      }
    }
    return null;
  }

  private String getAM(String AMUsage, String AMPackage) {
    BC4J_XCFG am = null;
    if (AMUsage != null) {
      
      Iterator<String> iter = BC4J_XCFG.m_appModules.keySet().iterator();

      while (iter.hasNext()) {
        String key = iter.next();
        am = BC4J_XCFG.m_appModules.get(key);

        if (am.getSName().equals(AMUsage) &&
            am.getSAppName().contains(AMPackage)) {
          
          if (result.getType().equals("table") || result.getType().equals("treeTable"))
            saveMaxNum(m_maxIterRangeSize, result.getJar_ui() + am.getSAppName() + result.getBinds(),
                      result.getIterRangeSize());
          
          result.setJar_am(am.getSJarName());
          return am.getSAppName();
        }
      }
    }
    return null;
  }

  private String getVO(String AM, String ViewUsage) {
    ViewUsage vu = null;
    AppModuleUsage amUsage = null;
    if (AM != null) {
      
      AM = AM.replaceAll("\\.", "/") + ".xml";
      
      if(isSubAM ){      
        ArrayList<AppModuleUsage> al =AppModuleUsage.m_subAM.get(result.getJar_am()+AM);
        if(al != null){
          Iterator<AppModuleUsage> iter_subAM = al.iterator();
          String[] temp = result.getBinds().split("\\.");
          ViewUsage = temp[temp.length - 1];
          result.setBinds(ViewUsage);
          while(iter_subAM.hasNext()){
            amUsage = iter_subAM.next();
            if(amUsage.getAmUsage().equals(temp[temp.length-2])){
              result.setAM(amUsage.getAmUsagePath());
              AM = amUsage.getAmUsagePath().replaceAll("\\.", "/") + ".xml";
              
              break;
            }
          }
        }
      }     
      
      Iterator<ViewUsage> iter = JarAnalyzer.viewUsage_list.iterator();

      while (iter.hasNext()) {
        vu = iter.next();

        if (vu.getApplicationModuleName().equals(AM) &&
            vu.getUsageName().equals(ViewUsage)) {
          if(isSubAM )
            result.setJar_am(vu.getJar());
          result.setVOUsageFetchSize(vu.getFetchSize());
          return vu.getViewObjectName();
        }
      }
    }
    return null;
  }

  private void populateVOFetchSize(String VO, boolean isFirstRun) {
    ViewObject viewObject = null;

    if (VO != null) {
      VO = VO.replaceAll("\\.", "/") + ".xml";
      
      Iterator<ViewObject> iter = JarAnalyzer.viewObject_list.iterator();

      while (iter.hasNext()) {
        viewObject = iter.next();

        if (viewObject.getFname().equals(VO)) {

          //track down ListBinding and VA Info
          if (isFirstRun &&
              (result.getType().equals("selectOneChoice") || result.getType().equals("inputComboboxListOfValues")))
            populateListBindingInfo(viewObject);

          //Set VA's VO when this method called by pupulateVAInfo or pupulateEOInfo
          if (!isFirstRun) {
            result.setVa_VO(viewObject.getFname());
            result.setVODefaultFetchSize(viewObject.getDefaultFetchSize());
            result.setNumOfAttrs(viewObject.getNumAttr());
          } else if (result.getVa_VO() == null)
            //For tables and treeTables
            result.setVODefaultFetchSize(viewObject.getDefaultFetchSize());
          
          result.setJar_vo(viewObject.getJar());
          result.setIsTransientVO(viewObject.isIsTransientVO());
          break;
        }
      }
    } else {
      //Set Default VO fetch to null if VA's VO is not found
      result.setVODefaultFetchSize(null);
    }
  }

  private void populateListBindingInfo(ViewObject vo) {
    if (result.getListBindingName() != null) {
      for (ViewObject.ListBinding lb : vo.getAl_listBinding()) {
        if (lb.getName().equals(result.getListBindingName())) {
          if(lb.getListVOName() == null)
            isGoodRecord = false;
          
          result.setViewAccessor(lb.getListVOName());
          result.setListRangeSize(lb.getListRangeSize());
          pupulateVAInfo(vo);
          break;
        }
      }
    }
  }

  private void pupulateVAInfo(ViewObject vo) {
    Pattern VA = Pattern.compile("(.+)\\.(.+)");
    Matcher m = null;

    m = VA.matcher(result.getViewAccessor());
    if (m.find()) {
      for (ViewObject.EntityUsage eo : vo.getAl_eo()) {
        if (eo.getName().equals(m.group(1))) {
          result.setViewAccessor(m.group(2));
          populateEOInfo(eo.getEntity(), m.group(2));
          break;
        }
      }
    } else {
      for (ViewObject.ViewAccessor va : vo.getAl_va()) {
        if (va.getName().equals(result.getViewAccessor())) {
          result.setVa_fetchSize(va.getFetchSize());
          populateVOFetchSize(va.getVOName(), false);
          break;
        }
      }
    }
  }

  private void populateEOInfo(String EOPath, String vaName) {
    EntityObject entityObject = null;

    if (EOPath != null) {
      EOPath = EOPath.replaceAll("\\.", "/") + ".xml";
      Iterator<EntityObject> iter = JarAnalyzer.entityObject_list.iterator();

      while (iter.hasNext()) {
        entityObject = iter.next();

        if (entityObject.getFname().equals(EOPath)) {
          for (EntityObject.ViewAccessor va : entityObject.getAl_va()) {
            if (va.getName().equals(vaName)) {
              result.setJar_eo(entityObject.getJar());
              result.setEntityObject(EOPath);
              result.setVa_fetchSize(va.getFetchSize());
              populateVOFetchSize(va.getVOName(), false);
              break;
            }
          }
        }
      }
    }
  }

  private void initReuslt(UIPageTunableElements uit) {
    String pageDefUsage;
    String AMUsage;
    String AM;
    String VO;
    
    result = new ScanResult();
    isGoodRecord = true;
    isSubAM = false;
    
    result.setJar_ui(uit.getSJar());
    result.setPage(uit.getSPage());
    result.setTableId(uit.getSTableId());
    result.setType(uit.getSType());
    result.setAutoHeightRows(uit.getSAutoHeightRows());
    result.setIsWrappedByPanelStretchLayout(uit.getIsWrappedByPanelStretchLayout());

    if (uit.getSValue() != null) {
      Pattern p = Pattern.compile("\\#\\{bindings\\.(.+?)\\.(?:.+)}");
      Matcher m = p.matcher(uit.getSValue());

      if (m.find()) {
        pageDefUsage = this.getPageDefUsage(result.getPage());
        result.setPageDef(getPageDef(pageDefUsage));
        result.setDataControl(getDataControl(result.getPageDef(), m.group(1)));
        
        //Stop if iterator is not found
        if(result.getIterator() != null){
          AMUsage = this.getAMUsage(result.getDataControl(), result.getCpx());
          AM = this.getAM(AMUsage, result.getAMPackage());
          result.setAM(AM);
          VO = this.getVO(AM, result.getBinds());
          result.setVO(VO);
  
          if(VO == null)
            isGoodRecord = false;
  
          populateVOFetchSize(VO, true);

          result.setEffAutoHeightRows(calculateEffAutoHeightRows());
          result.setEffVOUsageFetchSize(this.calculateEffVOUsageFetchSize());
          
          //cleanup
          if(result.getType().equals("selectOneChoice") || result.getType().equals("inputComboboxListOfValues"))
          if(result.getVa_VO() == null)
            result.setVODefaultFetchSize(null);
          
          if(result.getVa_fetchSize() == null && result.getViewAccessor() != null)
            result.setVa_fetchSize("Not Set");
          
          if(isGoodRecord){
                        if (result.getType().equals("table") ||
                            result.getType().equals("treeTable")) {

                            if (result.getIsWrappedByPanelStretchLayout().equals("Yes") &&
                                result.getEffAutoHeightRows() != null &&
                                Integer.parseInt(result.getEffAutoHeightRows()) < 15) 
                            {
                                saveMaxNum(m_maxAutoHeight,
                                           result.getJar_ui() +
                                           result.getPageDef() +
                                           result.getIterator(),
                                           Rules.AUTOHGT_NULL.toString());
                                al_results.add(result);
                                return;
                            }

                            saveMaxNum(m_maxAutoHeight,
                                       result.getJar_ui() + result.getPageDef() +
                                       result.getIterator(),
                                       result.getEffAutoHeightRows() == null ?
                                       Rules.AUTOHGT_NULL.toString() :
                                       result.getEffAutoHeightRows());

                        }
            al_results.add(result);
            }
        }
      }
    }
  }

  private String calculateEffAutoHeightRows() {

    //It's fine if AutoHgt is null
    if (result.getAutoHeightRows() == null) {
      return null;
    }
  
    Matcher m = null;

    try{
    //In case of numbers
    Pattern intRegex = Pattern.compile("-?\\d{1,5}");
    m = intRegex.matcher(result.getAutoHeightRows());
    if (m.matches()) {
      if (result.getAutoHeightRows().trim().equals("0")){
        if(result.getIterRangeSize() == null){
          isGoodRecord = false;
          return null;
        }else{
          return result.getIterRangeSize().trim();
        }
      }
      else if(result.getAutoHeightRows().trim().equals("-1"))
        return null;
      else
        return result.getAutoHeightRows();
    }

    String op;
    int num;
    int rangeSize;

    if (result.getIterRangeSize() != null)
      rangeSize = Integer.parseInt(result.getIterRangeSize().trim());
    else
      return null;

    //In case of conditional operation EL. i.e. #{ (bindings.ItemSupplierSiteIterator.rangeSize &gt; 4) ? 4 : bindings.ItemSupplierSiteIterator.rangeSize}
    Pattern conditionalOpsEL =
      Pattern.compile("\\#\\{ ?\\(? ?bindings\\.(?:.+)\\.rangeSize ?(>=?|<=?)(.+?)\\)? ?\\?(.+?)\\:(.+?)\\}");
    m = conditionalOpsEL.matcher(result.getAutoHeightRows());
    if (m.find()) {
      op = m.group(1).trim();
      if (!m.group(2).contains(")"))
        num = Integer.parseInt(m.group(2).trim());
      else
        num = Integer.parseInt(m.group(2).replace(")", " ").trim());
      
      return num+"";
//      if (op.equals(">") || op.equals(">=")) {
//        if (rangeSize >= num)
//          return result.getIterRangeSize();
//        else
//          return num + "";
//      } else if (op.equals("<") || op.equals("<=")) {
//        if (rangeSize <= num)
//          return num + "";
//        else
//          return result.getIterRangeSize();
//      }
    }
    
    //In case of regular bindings i.e. #{bindings.xxx.rangeSize + 1}
    Pattern regularEL =
      Pattern.compile("\\#\\{ ?bindings\\.(?:.+)\\.rangeSize ?(-|\\+)? ?1? {0,2}\\}");

    m = regularEL.matcher(result.getAutoHeightRows());
    if (m.matches()) {
      if (m.group(1) == null)
        return result.getIterRangeSize();
      else if (m.group(1).equals("-"))
        return String.valueOf(rangeSize - 1);
      else if (m.group(1).equals("+"))
        return String.valueOf(rangeSize + 1);
    }
    

    //In case of other EL
    Pattern otherEL = Pattern.compile("\\#\\{(?:.+) \\? ?'?(.+?)'? ?: ?'?(.+?)'?\\}");
    m = otherEL.matcher(result.getAutoHeightRows());
    if (m.find()) {
      Integer num1 = null;
      Integer num2 = null;
      try {
        num1 = Integer.parseInt(m.group(1).trim());
        num2 = Integer.parseInt(m.group(2).trim());
      } catch (Exception e) {
        //Unresolvable EL
        isGoodRecord = false;
        return null;
      }

      //always return the larger number
      return num1 > num2 ? num1.toString() : num2.toString();
    }
    }catch(Exception e){
      e.printStackTrace();
      System.out.println("Skipping this file...");
    }

    isGoodRecord = false;
    return null;
  }

  private String calculateEffVOUsageFetchSize() {

    if (result.getType().equals("table") || result.getType().equals("treeTable")) {
      if (result.getVOUsageFetchSize() == null) {
        if (result.getVO() == null)
          return "#NOT FOUND#";

        if (result.getVODefaultFetchSize() == null)
          return "1";
        else
          return result.getVODefaultFetchSize().toString();
      } else
        return result.getVOUsageFetchSize().toString();
    } else {
      if (result.getVa_fetchSize() == null) {
        if (result.getVa_VO() == null)
          return "#NOT FOUND#";

        if (result.getVODefaultFetchSize() == null)
          return "1";
        else
          return result.getVODefaultFetchSize().toString();
      } else
        return result.getVa_fetchSize().toString();
    }

  }

}
