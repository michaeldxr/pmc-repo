package oracle.apps.votuning;

public class TuningConstants {
    public static class FixStatus {
        public static final String LISTRANGESIZE_FIX_SUCESS = "LRS_FIX_SUCCESS";
        public static final String LISTRANGESIZE_FIX_FAILED = "LRS_FIX_FAILED";
        public static final String LISTRANGESIZE_NOCHANGE = "LRS_NOCHANGE";
        public static final String LISTRANGESIZE_WILL_NOT_FIX = "LRS_NOCHANGE";

        public static final String ITEARTOR_RANGESIZE_FIX_SUCCESS = "ITERATOR_FIX_SUCCESS";
        public static final String ITEARTOR_RANGESIZE_FIX_FAILED = "ITERATOR_FIX_FAILED";
        public static final String ITEARTOR_RANGESIZE_FIX_NOCHANGE = "ITERATOR_NOCHANGE";
        public static final String ITEARTOR_RANGESIZE_FIX_CANNOT = "ITERATOR_WILL_NOT_FIX";
        
        public static final String VA_FETCHSIZE_FIX_SUCCESS = "VA_FETCHSIZE_FIX_SUCCESS";
        public static final String VA_FETCHSIZE_FIX_FAILED = "VA_FETCHSIZE_FIX_FIX_FAILED";
        public static final String VA_FETCHSIZE_FIX_NOCHANGE = "VA_FETCHSIZE_FIX_FIX_NOCHANGE";

        public final static String DEFAULTVO_FIX_SUCCESS = "VOFETCH_DEFAULT_SUCCESS";
        public final static String DEFAULTVO_FIX_FAILED = "VOFETCH_DEFAULT_FAILED";
        
        public final static String AM_VO_WILLFIX_AM = "WILL_FIX_AM";
        public final static String AM_VO_FETCH_UNCHANGED = "AM_VO_FETCH_NOCHANGE";
        public final static String AM_VO_WILL_NOT_FIX = "AM_VO_FETCH_WILL_NOT_FIX";
        public final static String AM_VO_FETCH_SUCESS = "AM_VO_FETCH_SUCCESS";
        public final static String AM_VO_FETCH_FAILED = "AM_VO_FETCH_FAILED";
        public final static String WILL_NOT_FIX = "WILL_NOT_FIX";

        public final static String FIX_MULTITERATOR_SUCCESS = "FIX_MULTIPLEITERATOR_SUCCESS";
        public final static String FIX_MULTITERATOR_FAILED = "FIX_MULTIPLEITERATOR_FAILED";
        public final static String STATUS_FIX_AM_SUCCESS = "FIX_AM_SUCCESS";
        public final static String STATUS_FIX_MULTIPLEBC_SUCCESS = "FIX_MULTIPLE_BC_SUCCESS";
        public final static String STATUS_FIX_MULTIPLEBC_FAILURE = "FIX_MULTIPLE_BC_FAILED";
        public final static String STATUS_FIX_MULTIPLEBC_NOCHANGE = "FIX_MULTIPLE_BC_NOCHANGE";
        
        public final static String STATUS_FIX_AM_FAILED = "FIX_AM_FAILED";
        public final static String STATUS_MULTIPLE_BC = "STATUS_MULTIPLE_BC";
        
        
    }

    public static class Rules {
        public static final String FIX_SELECTONECHOICE_LRS = "FIX_SOC_LRS";
        public static final String FIX_COMBOBOX_LRS = "FIX_COMBOBOX_LRS";
        public static final String FIX_SELECTONECHOICE_VA_FETCHSIZE = "FIX_SOC_VA_FETCH";
        public static final String FIX_COMBOBOX_VA_FETCHSIZE = "FIX_COMBOBOX_VA_FETCH";
        public static final String FIX_TUNE_TABLES_PDEF_ITERATOR = "FIX_TUNETABLES_PDEF_ITER";
        public static final String FIX_TUNE_TABLES_VOFETCHSIZE = "FIX_TUNETABLES_VO_FETCH";
    }
    public static final String LOV_TYPE = "LovType";

    // If lov uses specified on list binding
    public static final String LOV_TYPE_MODEL_DRIVEN = "ModelDriven";

    // if no lov uses, and list iter binding specified
    public static final String LOV_TYPE_DYNAMICLIST = "DynamicList";

    // Navigation list uses base vo for traversal. fetch size configured on am:vo usage
    public static final String LOV_TYPE_NAVIGATION = "NavigationList";

    public static final String LOV_ROW_LEVEL = "RowLevelLOV";
    public static final String LOV_ROW_LEVEL_ATTR = "LOVAttribute";
    public static final String LOV_DYNAMICLIST_LISTITER = "DynamicList_ListIter";

    public static final String CUSTOMATTR_TREE_CONTENTDEL = "contentDelivery";
    public static final String CUSTOMATTR_TREE_ROWS = "rows";
    public static final String CUSTOMATTR_TREE_TABLE_FETCHSIZE = "fetchSize";
    public static final String CUSTOMATTR_MODEL_VALUE = "modelValue";
    public static final String CUSTOMATTR_BASELISTVOPATH = "ListVOPath";
    public static final String CUSTOMATTR_LISTVONAME = "ListViewObjectName";
    public static final String CUSTOMATTR_LIST_LISTRANGESIZE = "ListRangeSize";
    public static final String CUSTOMATTR_LIST_LISTRANGESIZE_NEW = "ListRangeSizeNewVal";
    public static final String NO_CHANGE = "NoChange";
    // key that will help lookup what bc:va fetch size to change
    public static final String CUSTOMATTR_LOV_VA_BCPATH_VANAME = "LOV_VA_BCPATH_VANAME";
    public final static String VO_FETCHSIZE_NOTSET="Not Set";
    public final static String VO_BASEVO_NOT_FOUND="BaseVO Not Found";
    
    public TuningConstants() {
        super();
    }
}
