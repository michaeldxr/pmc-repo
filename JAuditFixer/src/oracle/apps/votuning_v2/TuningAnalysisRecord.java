package oracle.apps.votuning_v2;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class TuningAnalysisRecord implements Serializable {
    public final static String STATUS_WILLFIX_ITERATOR = "WILL_FIX_ITERATOR";
    public final static String STATUS_DEFAULTVO_FIX_SUCCESS =
        "VOFETCH_DEFAULT_SUCCESS";
    public final static String STATUS_DEFAULTVO_FIX_FAILED =
        "VOFETCH_DEFAULT_FAILED";
    public final static String STATUS_WILLFIX_AM = "WILL_FIX_AM";
    public final static String STATUS_AMFETCH_UNCHANGED = "VOFETCH_NO_CHANGE";
    public final static String STATUS_WILL_NOT_FIX = "WILL_NOT_FIX";
    public final static String STATUS_FIX_ITERATOR_SUCCESS =
        "FIX_ITERATOR_SUCCESS";
    public final static String STATUS_FIX_MULTITERATOR_SUCCESS =
        "FIX_MULTIPLEITERATOR_SUCCESS";
    public final static String STATUS_FIX_MULTITERATOR_FAILED =
        "FIX_MULTIPLEITERATOR_FAILED";
    public final static String STATUS_FIX_ITERATOR_FAILED =
        "FIX_ITERATOR_FAILED";
    public final static String STATUS_FIX_ITERATOR_NOCHANGE =
        "FIX_ITERATOR_NOCHANGE";
    public final static String STATUS_FIX_AM_SUCCESS = "FIX_AM_SUCCESS";
    public final static String STATUS_FIX_MULTIPLEBC_SUCCESS =
        "FIX_MULTIPLE_BC_SUCCESS";
    public final static String STATUS_FIX_MULTIPLEBC_FAILURE =
        "FIX_MULTIPLE_BC_FAILED";
    public final static String STATUS_FIX_AM_FAILED = "FIX_AM_FAILED";
    public final static String STATUS_MULTIPLE_BC = "STATUS_MULTIPLE_BC";

    public void setUifilePath(String uifilePath) {
        this.uifilePath = uifilePath;
    }

    public String getUifilePath() {
        return uifilePath;
    }

    public void setUifileName(String uifileName) {
        this.uifileName = uifileName;
    }

    public String getUifileName() {
        return uifileName;
    }

    public void setCpxFilePath(String cpxFilePath) {
        this.cpxFilePath = cpxFilePath;
    }

    public String getCpxFilePath() {
        return cpxFilePath;
    }

    public void setPageDefFilePath(String pageDefFilePath) {
        this.pageDefFilePath = pageDefFilePath;
    }

    public String getPageDefFilePath() {
        return pageDefFilePath;
    }


    public void setPageDefIteratorName(String pageDefIteratorName) {
        this.pageDefIteratorName = pageDefIteratorName;
    }

    public String getPageDefIteratorName() {
        return pageDefIteratorName;
    }

    public void setIteratorRangeSize(String iteratorRangeSize) {
        this.iteratorRangeSize = iteratorRangeSize;
    }

    public String getIteratorRangeSize() {
        return iteratorRangeSize;
    }

    public void setAutoHeightRows(String autoHeightRows) {
        this.autoHeightRows = autoHeightRows;
    }

    public String getAutoHeightRows() {
        return autoHeightRows;
    }


    public void setUiTagFetchSize(String uiTagFetchSize) {
        this.uiTagFetchSize = uiTagFetchSize;
    }

    public String getUiTagFetchSize() {
        return uiTagFetchSize;
    }

    public void setSerializedNode(String serializedNode) {
        this.serializedNode = serializedNode;
    }

    public String getSerializedNode() {
        return serializedNode;
    }

    public void setUiTagRows(String uiTagRows) {
        this.uiTagRows = uiTagRows;
    }

    public String getUiTagRows() {
        return uiTagRows;
    }

    public void setVoUsageFetchSizeInAm(String voUsageFetchSizeInAm) {
        this.voUsageFetchSizeInAm = voUsageFetchSizeInAm;
    }

    public String getVoUsageFetchSizeInAm() {
        return voUsageFetchSizeInAm;
    }

    public void setVoDefaultFetchSize(String voDefaultFetchSize) {
        this.voDefaultFetchSize = voDefaultFetchSize;
    }

    public String getVoDefaultFetchSize() {
        return voDefaultFetchSize;
    }

    public void setVoFullPackage(String voFullPackage) {
        this.voFullPackage = voFullPackage;
    }

    public String getVoFullPackage() {
        return voFullPackage;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getProduct() {
        return product;
    }

    public void setAmPath(String amName) {
        this.amPath = amName;
    }

    public String getAmPath() {
        return amPath;
    }

    public void setDiagnostic(String diagnostic) {
        this.diagnostic = diagnostic;
    }

    public String getDiagnostic() {
        return diagnostic;
    }

    public void setContentDelivery(String contentDelivery) {
        this.contentDelivery = contentDelivery;
    }

    public String getContentDelivery() {
        return contentDelivery;
    }

    public void setAmFullPackage(String amFullPackage) {
        this.amFullPackage = amFullPackage;
    }

    public String getAmFullPackage() {
        return amFullPackage;
    }

    public void setVoInstanceName(String voInstanceName) {
        this.voInstanceName = voInstanceName;
    }

    public String getVoInstanceName() {
        return voInstanceName;
    }

    public void setAutoHeightNewValue(String autoHeightNewValue) {
        this.autoHeightNewValue = autoHeightNewValue;
    }

    public String getAutoHeightNewValue() {
        return autoHeightNewValue;
    }

    public void setIteratorRangeSizeNewValue(String iteratorRangeSizeNewValue) {
        this.iteratorRangeSizeNewValue = iteratorRangeSizeNewValue;
    }

    public String getIteratorRangeSizeNewValue() {
        return iteratorRangeSizeNewValue;
    }

    public void setVoUsageFetchSizeNewValue(String voUsageFetchSizeNewValue) {
        this.voUsageFetchSizeNewValue = voUsageFetchSizeNewValue;
    }

    public String getVoUsageFetchSizeNewValue() {
        return voUsageFetchSizeNewValue;
    }

    public void setVoDefaultFetchSizeNewValue(String voDefaultFetchSizeNewValue) {
        this.voDefaultFetchSizeNewValue = voDefaultFetchSizeNewValue;
    }

    public String getVoDefaultFetchSizeNewValue() {
        return voDefaultFetchSizeNewValue;
    }

    public void setFixStatus(String fixStatus) {
        this.fixStatus = fixStatus;
    }

    public String getFixStatus() {
        return fixStatus;
    }

    public void setFixStatusComment(String fixStatusComment) {
        this.fixStatusComment = fixStatusComment;
    }

    public String getFixStatusComment() {
        return fixStatusComment;
    }

    public void setBCanUpdateAmUsage(boolean bCanUpdateAmUsage) {
        this.bCanUpdateAmUsage = bCanUpdateAmUsage;
    }

    public boolean isBCanUpdateAmUsage() {
        return bCanUpdateAmUsage;
    }

    public void setIsGoodRecord(boolean isGoodRecord) {
        this.isGoodRecord = isGoodRecord;
    }

    public boolean isIsGoodRecord() {
        return isGoodRecord;
    }
    public void addRuleApplied(String ruleApplied)
    {
        rulesApplied.add(ruleApplied);
        }
    public void addFixStatus(String fixStatusToAdd)
    {
        m_setfixStatus.add(fixStatusToAdd);
        }
    public void addFixStatus(HashSet<String> fixStatusToAdd)
    {
        m_setfixStatus.addAll(fixStatusToAdd);
        
        
        }
    public boolean hasFixStatus(String fixStatusToCheck)
    {
        if(m_setfixStatus==null ||m_setfixStatus.size()<=0)
            return false;
        
        if(m_setfixStatus.contains(fixStatusToCheck))
            return true;
        
        return false;
        }
    
HashSet<String> rulesApplied = new HashSet<String>();
HashSet<String> m_setfixStatus = new HashSet<String>();
String componentName="";
    String uifilePath = "";
    String uifileName = "";
    String product = "";

String componentId = "";
    String cpxFilePath = "";
    String pageDefFilePath = "";

    String amPath = "";
    String voPath = "";
    String baseVoPath="";
    String amFullPackage = "";
    String voInstanceName = "";
    String voFullPackage = "";
    String componentType = "";
    boolean bCanUpdateAmUsage = false;
    boolean isGoodRecord = false;
    String pageDefIteratorName = "";

    String iteratorRangeSize = "";
    String autoHeightRows = "";
    String voUsageFetchSizeInAm = "";
    String voDefaultFetchSize = "";

    String autoHeightNewValue = "";
    String iteratorRangeSizeNewValue = "";
    String voUsageFetchSizeNewValue = "";
    String voDefaultFetchSizeNewValue = "";


    boolean tableWrappedByPanelStretchLayout = false;
    String panelStretchAncestry = "";

    String uiTagFetchSize = "";
    String uiTagRows = "";
    String serializedNode = "";
    String diagnostic = "";
    String contentDelivery = "";

    String fixStatus = "";
    String fixStatusComment = "";

    String lovUsesName = "";
    HashMap<String, String> customAttrs = new HashMap<String, String>();
    

    public void addCustomAttr(String key, String val) {
        customAttrs.put(key, val);
    }

    public String getCustomAttrWithKey(String key) {
        return customAttrs.get(key);
    }

    public TuningAnalysisRecord() {
        super();
    }

    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }

    public String getComponentType() {
        return componentType;
    }

    public void setVoPath(String voPath) {
        this.voPath = voPath;
    }

    public String getVoPath() {
        return voPath;
    }

    public void setLovUsesName(String lovUsesName) {
        this.lovUsesName = lovUsesName;
    }

    public String getLovUsesName() {
        return lovUsesName;
    }

    public void setCustomAttrs(HashMap<String, String> lovCustomAttrs) {
        this.customAttrs = lovCustomAttrs;
    }

    public HashMap<String, String> getLovCustomAttrs() {
        return customAttrs;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }

    public HashSet<String> getRulesApplied() {
        return rulesApplied;
    }

    public HashSet<String> getSetfixStatus() {
        return m_setfixStatus;
    }

    public void setBaseVoPath(String baseVoPath) {
        this.baseVoPath = baseVoPath;
    }

    public String getBaseVoPath() {
        return baseVoPath;
    }

    public void setSetfixStatus(HashSet<String> m_setfixStatus) {
        this.m_setfixStatus = m_setfixStatus;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setTableWrappedByPanelStretchLayout(boolean tableWrappedByPanelStretchLayout)
    {
        this.tableWrappedByPanelStretchLayout = tableWrappedByPanelStretchLayout;
    }

    public boolean isTableWrappedByPanelStretchLayout()
    {
        return tableWrappedByPanelStretchLayout;
    }

    public void setPanelStretchAncestry(String panelStretchAncestry) {
        this.panelStretchAncestry = panelStretchAncestry;
    }

    public String getPanelStretchAncestry() {
        return panelStretchAncestry;
    }
}
