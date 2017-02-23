package oracle.apps.ppr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.ppr.file_type.AppModuleUsage;
import oracle.apps.ppr.file_type.BC4J_XCFG;
import oracle.apps.ppr.file_type.DataControl;
import oracle.apps.ppr.file_type.EntityObject;
import oracle.apps.ppr.file_type.PageDef;
import oracle.apps.ppr.file_type.PageDefUsages;
import oracle.apps.ppr.file_type.PageMap;
import oracle.apps.ppr.file_type.UIPageTunableElements;
import oracle.apps.ppr.file_type.ViewObject;
import oracle.apps.ppr.file_type.ViewUsage;


public class ScanResultsAnalyzer {

  private static ScanResult result;
  private static boolean isGoodRecord = true;
  private static boolean isSubAM = false;
  public ArrayList<ScanResult> al_results = new ArrayList<ScanResult>();
  public static HashMap<String,String> mapping;
  int count = 0;
  int count1 = 0;

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
          count++;
      }
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

        if (pdi.getSBindingID() != null && pdi.getSBindingID().equals(bindings)) {
          if (pdi.getSBindingType().equals("list") ||
              pdi.getSBindingType().equals("button"))
            result.setListBindingName(pdi.getSListBinding());
          if (pdi.getSBinds().contains("Root.")) 
            isSubAM = true;
          
          result.setBinds(pdi.getSBinds());
          result.setIterator(pdi.getSIterId());
          result.setAttr(pdi.getAttrName());
          
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
      
      Iterator<ViewUsage> iter = ExemptionListGenerator.viewUsage_list.iterator();

      while (iter.hasNext()) {
        vu = iter.next();

        if (vu.getApplicationModuleName().equals(AM) &&
            vu.getUsageName().equals(ViewUsage)) {
          if(isSubAM )
            result.setJar_am(vu.getJar());
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
      
      Iterator<ViewObject> iter = ExemptionListGenerator.viewObject_list.iterator();

      while (iter.hasNext()) {
        viewObject = iter.next();

        if (viewObject.getFname().equals(VO)) {

          //track down ListBinding and VA Info
          if (isFirstRun &&
              (result.getType().equals("selectOneChoice") || result.getType().equals("selectBooleanCheckbox")))
            populateListBindingInfo(viewObject);

          //Set VA's VO when this method called by pupulateVAInfo or pupulateEOInfo
          if (!isFirstRun) {
            result.setVa_VO(viewObject.getFname());
            if(result.getAttr() != null){
                boolean hasValidation = viewObject.getAttrsWithValidation().contains(result.getAttr());
                result.setHasValidation(hasValidation);
            }else{
                result.setHasValidation(null);
            }
          }
          
          result.setJar_vo(viewObject.getJar());
          break;
        }
      }
    }
  }

  private void populateListBindingInfo(ViewObject vo) {
    if (result.getListBindingName() != null) {
      for (ViewObject.ListBinding lb : vo.getAl_listBinding()) {
        if (lb.getName().equals(result.getListBindingName())) {
          if(lb.getListVOName() == null)
            isGoodRecord = false;
          
          result.setViewAccessor(lb.getListVOName());
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
      Iterator<EntityObject> iter = ExemptionListGenerator.entityObject_list.iterator();

      while (iter.hasNext()) {
        entityObject = iter.next();

        if (entityObject.getFname().equals(EOPath)) {
          for (EntityObject.ViewAccessor va : entityObject.getAl_va()) {
            if (va.getName().equals(vaName)) {
              result.setJar_eo(entityObject.getJar());
              result.setEntityObject(EOPath);
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
    
    
    try{
      result.setJar_ui(uit.getSJar());
      result.setPage(uit.getSPage());
      result.setTableId(uit.getSTableId());
      result.setType(uit.getSType());
  
      if (uit.getSValue() != null) {
      
        Pattern p1 = Pattern.compile("\\#\\{bindings\\.(.+?)\\.hints\\.(.+?)\\.(?:.+)}");
        Pattern p2 = Pattern.compile("\\#\\{bindings\\.(.+?)\\.(?:.+)}");
        Matcher m1 = p1.matcher(uit.getSValue());
        Matcher m2 = p2.matcher(uit.getSValue());
  
        if(m1.matches()){
            pageDefUsage = this.getPageDefUsage(result.getPage());
            result.setPageDef(getPageDef(pageDefUsage));
            result.setDataControl(getDataControl(result.getPageDef(), m1.group(1)));
            result.setAttr(m1.group(2));
        }else if (m2.find()) {
          pageDefUsage = this.getPageDefUsage(result.getPage());
          result.setPageDef(getPageDef(pageDefUsage));
          result.setDataControl(getDataControl(result.getPageDef(), m2.group(1)));
        }
          //Stop if iterator is not found
          if(result.getIterator() != null){
            AMUsage = this.getAMUsage(result.getDataControl(), result.getCpx());
            AM = this.getAM(AMUsage, result.getAMPackage());
            result.setAM(AM);
            VO = this.getVO(AM, result.getBinds());
            result.setVO(VO);
    
            if(VO == null)
              isGoodRecord = false;
          
            if(result.getType().equals("inputDate") || result.getType().equals("inputText") ){
              findAttrsWithValidations(VO,false);
            }
            
            if(result.getType().equals("selectOneChoice") || result.getType().equals("selectBooleanCheckbox") ){
              findAttrsWithValidations(VO, true);
            }
            
              
            if(isGoodRecord){
                  al_results.add(result);
              }
          }
  
  
      }else{
          System.out.println("No Value Found.");
      }
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  private void findAttrsWithValidations(String VO, boolean isLOV) {
    try {
      ViewObject viewObject = null;

      if (result.getAttr() == null && result.getListBindingName() == null)
        return;

      if (VO != null) {
        VO = VO.replaceAll("\\.", "/") + ".xml";

        Iterator<ViewObject> iter =
          ExemptionListGenerator.viewObject_list.iterator();

        while (iter.hasNext()) {
          viewObject = iter.next();

          if (viewObject.getFname().equals(VO)) {
            //Check if there are validations in VO first
            HashSet<String> attrs = new HashSet<String>();
            if(result.getAttr() != null)
              attrs.add(result.getAttr());
            
            if(isLOV){
              if(result.getListBindingName() != null && !result.getListBindingName().isEmpty()){
                //list binding
                for (ViewObject.ListBinding lb : viewObject.getAl_listBinding()) {
                  if (lb.getName().equals(result.getListBindingName())) {
                    Iterator<String> i = lb.getAttrs().iterator();
                    while(i.hasNext())
                      attrs.add(i.next());
                    break;
                  }
                }
              }else{
                //table binding
                for (ViewObject.ListBinding lb : viewObject.getAl_listBinding()) {
                  if (lb.getAttrs().contains(result.getAttr())) {
                    Iterator<String> i = lb.getAttrs().iterator();
                    while(i.hasNext())
                      attrs.add(i.next());
                  }
                }
              }
            }

            for (String attr : attrs) {
              if (!viewObject.getAttrsWithValidation().isEmpty() &&
                  viewObject.getAttrsWithValidation().contains(attr.toLowerCase())) {
                result.setHasValidation(true);
                return;
              }
            }

            //Check if there are validations in EO then.
            for (String attr : attrs) {
              String value =
                viewObject.getM_attrsWithEO().get(attr.toLowerCase());
              if (value == null)
                return;

              String[] temp = value.split("#");
              String EOUsage = temp[0];
              String EOAttr = temp[1];

              for (ViewObject.EntityUsage eo : viewObject.getAl_eo()) {
                if (eo.getName().equals(EOUsage)) {
                  String EOPath = eo.getEntity();

                  if (EOPath != null) {
                    EOPath = EOPath.replaceAll("\\.", "/") + ".xml";
                    Iterator<EntityObject> eo_iter =
                      ExemptionListGenerator.entityObject_list.iterator();
                    EntityObject entityObject = null;

                    while (eo_iter.hasNext()) {
                      entityObject = eo_iter.next();

                      if (entityObject.getFname().equals(EOPath)) {
                        if (!entityObject.getAttrsWithValidation().isEmpty() &&
                            entityObject.getAttrsWithValidation().contains(EOAttr.toLowerCase())) {
                          result.setHasValidation(true);
                          return;
                        }
                        break;
                      }
                    }
                  }
                }

              }
            }
            break;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
