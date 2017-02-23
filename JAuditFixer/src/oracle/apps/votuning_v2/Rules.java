package oracle.apps.votuning_v2;

import java.io.File;

import java.util.ArrayList;

import java.util.HashMap;

import java.util.Iterator;
import java.util.Set;


public class Rules {
    private static Integer ITERRANGESIZE_VOFETCH_OFFSET = 1;
    private static Integer AUTOHGT_ITERATOR_OFFSET = 1;
    private static Integer AUTOHGT_NULL = 15;
    private static Integer SELECTONE_LISTRANGESIZE = 250;
    private static Integer COMBOBOX_LISTRANGESIZE = 250;
    // no multiple usages check doen for these since they are both setting it to 16 it should not matter
    private static Integer SELECTONE_USAGE_FETCHSIZE = 16;
    private static Integer COMBOBOX_USAGE_FETCHSIZE = 16;

    public Rules() {
        super();
    }

    /**
     * gets the iterator fetch size (newValue, if new value is null takes the old value)
     * converts to integer
     * if range size <0 - return since we do not want to modify the fetch size in this case. -1 is a valid value which means fetch all rows from the underlying collection
     *
     * if rangesize>0
     *
     * @param record
     * @param multipleBcusages
     * @return
     * @throws Exception
     */
    public static TuningAnalysisRecord calculateVOFetchSize(TuningAnalysisRecord record,
                                                            HashMap<String, ArrayList<TuningAnalysisRecord>> multipleBcusagesFetchSize) throws Exception {


        if (record == null) {
            return null;
        }

        String bcKey = record.getAmFullPackage() + "#" + record.getVoInstanceName();
        if (multipleBcusagesFetchSize != null && multipleBcusagesFetchSize.containsKey(bcKey)) {
            record.setFixStatus(record.getFixStatus() + "," + TuningAnalysisRecord.STATUS_MULTIPLE_BC);
            record.addFixStatus(TuningConstants.FixStatus.STATUS_MULTIPLE_BC);
            record.setFixStatusComment("this BC Object is referenced by multiple pages.. cannot change at a page level");
            return record;
        }

        String amPath = record.getAmPath();
        if (isEmpty(amPath)) {
            record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
            record.addFixStatus(TuningConstants.FixStatus.WILL_NOT_FIX);
            record.setFixStatusComment(record.getFixStatusComment() + " \n Am path is null");
            return record;
        }

        File f = new File(amPath);
        if (!f.exists()) {
            record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
            record.addFixStatus(TuningConstants.FixStatus.WILL_NOT_FIX);
            record.setFixStatusComment(record.getFixStatusComment() + " \n am file does not exist");
            return record;
        }

        Integer voFetchSize = calculateVOFetchSize(record);
        if (voFetchSize == null) {
            return record;
        }
        String oldVoUsageFetchSize = record.getVoUsageFetchSizeInAm();
        if (!isEmpty(oldVoUsageFetchSize) && !oldVoUsageFetchSize.trim().equals("-37")) {
            try {
                Integer intoldfetchSize = Integer.parseInt(oldVoUsageFetchSize);
                if (intoldfetchSize == voFetchSize) {
                    record.setVoUsageFetchSizeNewValue(voFetchSize.toString());
                    record.setFixStatus(TuningAnalysisRecord.STATUS_AMFETCH_UNCHANGED);
                    return record;
                }
            } catch (Exception ex) {

            }
        }
        record.setVoUsageFetchSizeNewValue(voFetchSize.toString());
        record.setFixStatus(TuningAnalysisRecord.STATUS_WILLFIX_AM);

        return record;
    }

    /**
     * Formula: voFetchSize = iteratorRangeSize +1
     * Status will be set to "WILL_NOT_FIX if: "
     * 1. Iterator range size is empty
     * 2. Iterator range size is <=0
     *
     * Return null if any one of the above "WILL_NOF_FIX conditions is true"
     * @param record
     * @return
     * @throws Exception
     */
    private static Integer calculateVOFetchSize(TuningAnalysisRecord record) throws Exception {

        if (record == null) {
            return null;
        }
        Integer iterRangeSizeNew = null;

        String iterRangeSizeNewVal = record.getIteratorRangeSizeNewValue();


        if (isEmpty(iterRangeSizeNewVal) || iterRangeSizeNewVal.trim().equals("-37")) {
            record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
            record.setFixStatusComment("New iterator range size was null.. ");
            return null;
        }

        try {
            iterRangeSizeNew = Integer.parseInt(iterRangeSizeNewVal);
        } catch (NumberFormatException nex) {
            System.out.println("Couldn ot convert iterator range size to number");
            record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
            record.setFixStatusComment("Could not convert iterator range size to number");
            return null;
        }
        if (iterRangeSizeNew == null || iterRangeSizeNew <= 0) {
            System.out.println("Iterator range size was less than 0.. not changing vo fetch size");
            record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
            record.setFixStatusComment("Iterator range size was less than 0 ");
            return null;
        }

        // formula vofetchSize = iteratorRangeSize+1
        return iterRangeSizeNew + ITERRANGESIZE_VOFETCH_OFFSET;
    }

    public static Integer getMaxIteratorRangeSize(ArrayList<TuningAnalysisRecord> list_records) throws Exception {
        if (list_records == null || list_records.size() <= 0) {
            return null;
        }
        Integer maxIteratorSize = -37;
        Iterator<TuningAnalysisRecord> recordIter = list_records.iterator();
        while (recordIter.hasNext()) {
            TuningAnalysisRecord record = recordIter.next();
            String iteratorFixStatus = record.getFixStatus();
            Integer intSize = -37;
            if (!isEmpty(iteratorFixStatus) && iteratorFixStatus.contains(TuningAnalysisRecord.STATUS_MULTIPLE_BC)) {
                String iteratorSize = record.getIteratorRangeSizeNewValue();
                if (isEmpty(iteratorSize)) {
                    iteratorSize = record.getIteratorRangeSize();
                }
                if (iteratorSize.trim().equals(TuningConstants.NO_CHANGE)) {
                    iteratorSize = record.getIteratorRangeSize();
                }
                if (isEmpty(iteratorSize)) {
                    continue;
                }
                intSize = Integer.parseInt(iteratorSize);


            } else {
                String iteratorSize = record.getIteratorRangeSize();
                if (isEmpty(iteratorSize)) {
                    continue;
                }
                intSize = Integer.parseInt(iteratorSize);
            }

            if (intSize > maxIteratorSize) {
                maxIteratorSize = intSize;
            }
        }
        return maxIteratorSize;
    }


    public static HashMap<String, String> calculateVOFetchSizeForMultipleBCUages(HashMap<String, ArrayList<TuningAnalysisRecord>> multipleBcusages) {
        if (multipleBcusages == null) {
            return null;
        }

        HashMap<String, String> multipleBcUsageFetchSize = new HashMap<String, String>();
        Set<String> bcKeys = multipleBcusages.keySet();
        Iterator<String> bcKeyIter = bcKeys.iterator();
        while (bcKeyIter.hasNext()) {
            Integer maxIteratorRangeSize = -37;

            String bcKey = bcKeyIter.next();
            ArrayList<TuningAnalysisRecord> listRecords = multipleBcusages.get(bcKey);
            if (listRecords == null) {
                continue;
            }
            try {
                maxIteratorRangeSize = getMaxIteratorRangeSize(listRecords);
            } catch (Exception ex) {
                System.out.println("Exception occurred while trying to obtain max iterator range size for bckey: " + bcKey);

            }
            if (maxIteratorRangeSize != null && maxIteratorRangeSize > 0) {
                // because this is the old value.. once it gets updated the new value will be the old value +1.
                Integer fetchSize = maxIteratorRangeSize + ITERRANGESIZE_VOFETCH_OFFSET;
                multipleBcUsageFetchSize.put(bcKey, fetchSize.toString());
            } else {
                //ERROR
                Integer fetchSize = -37;
                multipleBcUsageFetchSize.put(bcKey, fetchSize.toString());
            }

        }
        return multipleBcUsageFetchSize;
    }

    public static Integer calculateVOFetchSizeForMultipleBCUages(ArrayList<TuningAnalysisRecord> listRecords) throws Exception {
        if (listRecords == null) {
            return null;
        }

        Integer maxIteratorRangeSize = getMaxIteratorRangeSize(listRecords);

        if (maxIteratorRangeSize != null && maxIteratorRangeSize > 0) {
            // because this is the old value.. once it gets updated the new value will be the old value +1.
            return maxIteratorRangeSize + ITERRANGESIZE_VOFETCH_OFFSET;

        }


        return null;
    }

    /**
     * Calculates the iterator range size for multiple iterator usages.
     * e.g. two af:tables refer to the same page def iterator. In that case the iterator range size needs to be set to the MaxAutoHeight+1
     *
     * 1. for every multiple iterator Usage
     *  1a. get the analysis record
     *  1b. calculate the new iterator range size for the record
     *  1c. If record.Status==WILL_NOT_FIX
     *          break; // do not want to be changing the iterator range size if any one of the records does not want to fix it.
     *      else if record.Status==WILL_FIX or NOT_CHANGED
     *          if (newFetchSize > max)
     *              max = newFetchSize
     * return mapOfFixedMultipleIteratorRangeSizes
     * @param mapMultipleUsages
     * @return
     * @throws Exception
     */  // JOE_TODO:  need to fix for new logic
    public static HashMap<String, TuningAnalysisRecord> calculateRangeSizeForMultipleIteratorUsages(HashMap<String, ArrayList<TuningAnalysisRecord>> mapMultipleUsages) throws Exception {
        if (mapMultipleUsages == null || mapMultipleUsages.size() <= 0) {
            return null;
        }

        HashMap<String, TuningAnalysisRecord> newIteratorRangeSizes = new HashMap<String, TuningAnalysisRecord>();
        Set<String> iterKeys = mapMultipleUsages.keySet();
        Iterator<String> iterKeyIter = iterKeys.iterator();
        while (iterKeyIter.hasNext()) {

            String pageDefKey = iterKeyIter.next();
            ArrayList<TuningAnalysisRecord> list_records = mapMultipleUsages.get(pageDefKey);
            Iterator<TuningAnalysisRecord> recordsIter = list_records.iterator();
            Integer maxIteratorSize = -37;
            TuningAnalysisRecord finalRecord = new TuningAnalysisRecord();
            finalRecord.setIteratorRangeSizeNewValue("-37");
            while (recordsIter.hasNext()) {
                TuningAnalysisRecord record = recordsIter.next();
                TuningAnalysisRecord newRecord = calculateAutoHeightNIteratorRange(record);
                String iteratorFixStatus = newRecord.getFixStatus();
                if (!isEmpty(iteratorFixStatus) &&
                    (iteratorFixStatus.contains(TuningAnalysisRecord.STATUS_WILLFIX_ITERATOR) || iteratorFixStatus.contains(TuningAnalysisRecord.STATUS_FIX_ITERATOR_NOCHANGE))) {
                    try {
                        Integer newIteratorSize = Integer.parseInt(newRecord.getIteratorRangeSizeNewValue());
                        if (newIteratorSize > maxIteratorSize) {
                            maxIteratorSize = newIteratorSize;
                            finalRecord = newRecord;
                        }
                    } catch (Exception ex) {
                        break;
                    }
                } else if (iteratorFixStatus.contains(TuningAnalysisRecord.STATUS_WILL_NOT_FIX)) {
                    break;
                }

            }
            if (maxIteratorSize == null || maxIteratorSize < 0) {
                continue;
            }
            if (finalRecord != null) {
                TuningAnalysisRecord multipleIterRecord = new TuningAnalysisRecord();
                multipleIterRecord.setAutoHeightRows(finalRecord.getAutoHeightRows());
                multipleIterRecord.setIteratorRangeSize(finalRecord.getIteratorRangeSize());
                multipleIterRecord.setIteratorRangeSizeNewValue(finalRecord.getIteratorRangeSizeNewValue());
                multipleIterRecord.setFixStatus(finalRecord.getFixStatus());
                multipleIterRecord.setFixStatusComment(finalRecord.getFixStatusComment());
                multipleIterRecord.addFixStatus(finalRecord.getSetfixStatus());
                multipleIterRecord.setPageDefFilePath(finalRecord.getPageDefFilePath());
                multipleIterRecord.setPageDefIteratorName(finalRecord.getPageDefIteratorName());
                //newIteratorRangeSizes.put(pageDefKey, finalRecord);
                newIteratorRangeSizes.put(pageDefKey, multipleIterRecord);
            }

        }
        return newIteratorRangeSizes;
    }

    /*
     * Forumula:
     * 1. [autoHeight!=null && autoHeight!=-1] iteratorRangeSize = autoHeightRows + 5
     * 2. [autoHeight==null || autoHeight==-1] iteratorRangeSize = 20
     *
     * Record Status = WILL_NOT_FIX if:
     * 1. Iterator.Range Size is not set. We do not want to be adding a range size to the iterator if it is not already present.
     * 2. If autoHeight is an ELExpression
     * 3. Cannot determine integer value of the autoHeight value
     * @param record
     * @return
     *
     * @throws Exception
     */
// JOE_TODO:  need to fix so that new logic is honored
    public static TuningAnalysisRecord calculateAutoHeightNIteratorRange(TuningAnalysisRecord record) throws Exception {
        if (record == null)
            return null;
        // final String AHNULL_ITERATOR_RANGESIZE = "20";
        // final Integer AH_ITERATOR_OFFSET = 5;
        String autoHeight = record.getAutoHeightRows();
        String iteratorRangeSize = record.getIteratorRangeSize();

        if (isEmpty(iteratorRangeSize) || iteratorRangeSize.trim().contains("-37")) {
            record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
            record.setFixStatusComment("iterator range size is null or empty");
            return record;
        }

        Integer iteratorRangeSizeInteger = Integer.parseInt(iteratorRangeSize);
        if (iteratorRangeSizeInteger == null || iteratorRangeSizeInteger < 0) {
            record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
            record.setFixStatusComment("Iterator range size empty or less than 0");
            return record;
        }

        if(record.isTableWrappedByPanelStretchLayout())
        {
            //System.out.println("ok, here we go");
            // since we are direct ancestory of panelStretchLayout, auto-height is ignored
            // note:  cases with dimensionsFrom set to children will be (isTableWrappedByPanelStretchLayout() == false).
            autoHeight = null;
        }

        if (!isEmpty(autoHeight) && !autoHeight.trim().equals("-1")) {
            if (isELExpression(autoHeight)) {
                record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
                record.setFixStatusComment("autoHeight is EL EXpression");
            } else {
                try {
                    Integer autoHeightInteger = Integer.parseInt(autoHeight);

                    if (autoHeightInteger != (iteratorRangeSizeInteger - AUTOHGT_ITERATOR_OFFSET)) {
                        record.setAutoHeightNewValue(autoHeightInteger.toString());
                        Integer newIteratorSize = autoHeightInteger + AUTOHGT_ITERATOR_OFFSET;
                        record.setIteratorRangeSizeNewValue(newIteratorSize.toString());
                        record.setFixStatus(TuningAnalysisRecord.STATUS_WILLFIX_ITERATOR);
                    } else {
                        //TODO: Iterator size not required to be fixed... but set iterator range size new value..
                        //record.setIteratorRangeSizeNewValue(record.getIteratorRangeSize());
                        record.setIteratorRangeSizeNewValue(record.getIteratorRangeSize());
                        record.setFixStatus(TuningAnalysisRecord.STATUS_FIX_ITERATOR_NOCHANGE);
                    }
                } catch (NumberFormatException nex) {
                    record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
                    record.setFixStatusComment("Cannot determine integer value of autoheight: " + autoHeight);
                    throw new Exception(nex);
                }
            }
        } 
        else 
        {
            if(record.isTableWrappedByPanelStretchLayout())
            {
                if(iteratorRangeSizeInteger.intValue() < 15)
                {
                    record.setFixStatus(TuningAnalysisRecord.STATUS_WILLFIX_ITERATOR);
                    Integer autoHghtNullRangeSize = AUTOHGT_NULL + AUTOHGT_ITERATOR_OFFSET;
                    record.setIteratorRangeSizeNewValue(autoHghtNullRangeSize.toString());
                }
                else
                {
                    record.setFixStatus(TuningAnalysisRecord.STATUS_WILL_NOT_FIX);
                    record.setFixStatusComment("current IRS is " + iteratorRangeSizeInteger + " which is >= 15");
                }
            }
            else
            {
                record.setFixStatus(TuningAnalysisRecord.STATUS_WILLFIX_ITERATOR);
                Integer autoHghtNullRangeSize = AUTOHGT_NULL + AUTOHGT_ITERATOR_OFFSET;
                record.setIteratorRangeSizeNewValue(autoHghtNullRangeSize.toString());
            }
            //record.setFixStatusComment("auto Height is null or -1.. TODO: Set iterator range size to a constant value.. Right now skipping.. ");
        }
        return record;
    }

    /**
     * Changed method to accomodate requirement to fix list range size only if it is over a max value
     * e.g. if the list range size was set to 10 you do not want to change it to 250
     * this works only for combobox lovs
     * @param record
     * @param fix_only_if_overMax
     * @param maxThreshold
     */
    public static void calculateListRangeSize(TuningAnalysisRecord record, Boolean fix_only_if_overMax, Integer maxThreshold) {
        String componentName = record.getComponentName();
        if (!isEmpty(componentName) && componentName.equals("af:selectOneChoice")) {
            String listRangeSize = record.getCustomAttrWithKey(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE);
            if (!isEmpty(listRangeSize) && listRangeSize.trim().equals(SELECTONE_LISTRANGESIZE.toString())) {
                record.addCustomAttr(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE_NEW, TuningConstants.NO_CHANGE);
                return;
            }
            record.addCustomAttr(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE_NEW, SELECTONE_LISTRANGESIZE.toString());
        } else if (!isEmpty(componentName) && componentName.equals("af:inputComboboxListOfValues")) {
            String listRangeSize = record.getCustomAttrWithKey(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE);
            if (!isEmpty(listRangeSize) && listRangeSize.trim().equals(COMBOBOX_LISTRANGESIZE.toString())) {
                record.addCustomAttr(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE_NEW, TuningConstants.NO_CHANGE);
                return;
            }
            if (fix_only_if_overMax) {
                if (isEmpty(listRangeSize)) {
                    record.addCustomAttr(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE_NEW, COMBOBOX_LISTRANGESIZE.toString());
                    return;
                }
                try {
                    Integer intLRS = Integer.parseInt(listRangeSize);
                    if (intLRS == null) {
                        record.addFixStatus(TuningConstants.FixStatus.LISTRANGESIZE_WILL_NOT_FIX);
                        record.setFixStatusComment(record.getFixStatusComment() + "\n listRangeSize is not an integer");
                        return;
                    }
                    if (intLRS == -1 || intLRS > maxThreshold) {
                        record.addCustomAttr(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE_NEW, COMBOBOX_LISTRANGESIZE.toString());
                    } else {
                        record.addCustomAttr(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE_NEW, TuningConstants.NO_CHANGE);
                        record.addFixStatus(TuningConstants.FixStatus.LISTRANGESIZE_WILL_NOT_FIX);
                        record.setFixStatusComment(record.getFixStatusComment() + "\n listRangeSize is not over max.. ");
                    }

                } catch (Exception ex) {
                    record.addFixStatus(TuningConstants.FixStatus.LISTRANGESIZE_WILL_NOT_FIX);
                    record.setFixStatusComment(record.getFixStatusComment() + "/n listRangeSize is not an integer");
                }

            } else {
                record.addCustomAttr(TuningConstants.CUSTOMATTR_LIST_LISTRANGESIZE_NEW, COMBOBOX_LISTRANGESIZE.toString());
            }
        }

    }

    public static void calculateLOVModelDrivenFetchSize(TuningAnalysisRecord record) {
        String componentName = record.getComponentName();
        if (!isEmpty(componentName) && componentName.equals("af:selectOneChoice")) {
            /*
             * Rules for SOC:
             * 1. If UsageFetchSize == 16 : return
             * 2. If UsageFetchSize not set, and defaultFetchSize ==16 : return
             * 3. If effectiveFetchSize ( Usage or default in case usage is not set) < 16 ==> only then change it. Else no change
             * */
            
            String usageFetchSize = record.getVoUsageFetchSizeInAm();
            String defaultFetchSize = record.getVoDefaultFetchSize();

            if (!isEmpty(usageFetchSize) && usageFetchSize.trim().equals(SELECTONE_USAGE_FETCHSIZE.toString())) {
                record.setVoUsageFetchSizeNewValue(TuningConstants.NO_CHANGE);
                return;
            }
            // The usageFetch size is not set and the defaultFetchSize==16, then also we are ok because default fetchsize will take precedence
            if(isEmpty(usageFetchSize)  && defaultFetchSize.trim().equals(SELECTONE_USAGE_FETCHSIZE.toString()))
            {
                record.setVoUsageFetchSizeNewValue(TuningConstants.NO_CHANGE);
                return;
            }
            String effectiveFetchSize=getEffectiveFetchSize(usageFetchSize,defaultFetchSize);
            // If it comes out that the effective fetch size is not set (default:1) then we need to change this value
            if(isEmpty(effectiveFetchSize) || effectiveFetchSize.trim().equals(TuningConstants.VO_FETCHSIZE_NOTSET))
            {
                record.setVoUsageFetchSizeNewValue(SELECTONE_USAGE_FETCHSIZE.toString());
                return;
            }
            Integer effectFSInt = getIntegerValueOfString(effectiveFetchSize, 1);
            if(effectFSInt<SELECTONE_USAGE_FETCHSIZE)
            {
                    record.setVoUsageFetchSizeNewValue(SELECTONE_USAGE_FETCHSIZE.toString());
            }else
            {
                record.setVoUsageFetchSizeNewValue(TuningConstants.NO_CHANGE);
            }

        } else if (!isEmpty(componentName) && componentName.equals("af:inputComboboxListOfValues")) {
            String usageFetchSize = record.getVoUsageFetchSizeInAm();
            String defaultFetchSize = record.getVoDefaultFetchSize();
            String effectiveFetchSize=getEffectiveFetchSize(usageFetchSize,defaultFetchSize);
            
            if (!isEmpty(usageFetchSize) && usageFetchSize.trim().equals(COMBOBOX_USAGE_FETCHSIZE.toString())) {
                record.setVoUsageFetchSizeNewValue(TuningConstants.NO_CHANGE);
                return;
            }
            
            // The usageFetch size is not set and the defaultFetchSize==16, then also we are ok because default fetchsize will take precedence
            if(isEmpty(usageFetchSize)  && defaultFetchSize.trim().equals(COMBOBOX_USAGE_FETCHSIZE.toString()))
            {
                record.setVoUsageFetchSizeNewValue(TuningConstants.NO_CHANGE);
                return;
            }
            
            // If it comes out that the effective fetch size is not set (default:1) then we need to change this value
            if(isEmpty(effectiveFetchSize) || effectiveFetchSize.trim().equals(TuningConstants.VO_FETCHSIZE_NOTSET))
            {
                record.setVoUsageFetchSizeNewValue(COMBOBOX_USAGE_FETCHSIZE.toString());
                return;
            }
            
            Integer effectFSInt = getIntegerValueOfString(effectiveFetchSize, 1);
            if(effectFSInt<COMBOBOX_USAGE_FETCHSIZE)
            {
                    record.setVoUsageFetchSizeNewValue(COMBOBOX_USAGE_FETCHSIZE.toString());
            }else
            {
                record.setVoUsageFetchSizeNewValue(TuningConstants.NO_CHANGE);
            }
            
        }

    }

    //================MISCELLANEOUS FUNCTIONS==================

public static String getEffectiveFetchSize(String usageFetchSize, String defaultFetchSize)
{
    String effectiveFetchSize = usageFetchSize;
    if(isEmpty(effectiveFetchSize) || effectiveFetchSize.trim().equals(TuningConstants.VO_FETCHSIZE_NOTSET))
    {
        effectiveFetchSize=defaultFetchSize;
    }
    return effectiveFetchSize;
    }
    public static boolean isELExpression(String str) {
        if (!isEmpty(str) && str.trim().startsWith("#{")) {
            return true;
        }
        return false;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }
    public static Integer getIntegerValueOfString(String str,Integer returnValueIfError)
    {
        if(isEmpty(str))    
            return returnValueIfError;
        try{
            return Integer.parseInt(str);
        }catch(Exception ex)
        {
            return returnValueIfError;
            }
    }
}
