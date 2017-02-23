package oracle.apps.di;

public class CheckerConstants {
    public static String ISSUE_NAMINGSTANDARDS="ADFdi-NamingStandards";
    public static String ISSUE_RESOURCES="ADFdi-Resources";
    
    public static String ISSUE_TABLEKEY="ADFdi-TableKey";
    public static String ISSUE_TABLE_STATUSCOL="ADFdi-TableStatusCol";
    public static String ISSUE_TABLE_CACHEDATTR="ADFdi-TableCachedAttr";
    public static String ISSUE_TABLE_COMMITACTION="ADFdi-TableCommit";
    public static String ISSUE_TABLE_LIMITBATCHSIZE="ADFdi-LimitBatchSize";
    public static String ISSUE_TABLE_INSERTROWACTION="ADFdi-InsertBeforeRowActionID";
    public static String ISSUE_TABLE_BATCHSIZE100="ADFdi-BatchSizeValue100";
    public static String ISSUE_FSM="ADFdi-FSM";
    public static String ISSUE_WBKID="ADFdi-WbkId";
    public static String ISSUE_BINDINGS="ADFdi-Bindings";
    public static String ISSUE_WorkbookErrors="ADFdi-WrksheetErrors";
    public static String ISSUE_DEPDENDENTDROPDOWN = "ADFdi-DependentDropdown";
    
    public static String ISSUE_COMPOENTS_BUTTON="ADFdi-Buttons";
    
    public static String ISSUE_CLIENT_COMPONENT="ADFdi-ClientComponent";

    public static String ISSUE_COMPONENTS_STATUSVIWER="ADFdi-StatusViwer";
    
    public static String ISSUE_CUSTOMIZATIONENABLED = "ADFdi-customization";
    
    public static String ISSUE_NAMING_PATH = "ADFdi-Naming-Path";
    public static String N_DESC="Naming Standards";
    public static String N1_MSG="Design Time Spreadsheet must end with DT.xlsx";
    public static String N2_MSG="Spreadsheet must be present in a directory containing /di/";
    public static String N3_MSG="Design time spreadsheets must be present in the /src/ directory";
    public static String N4_MSG="Published spreadsheets must be present in the /public_html/ directory";
    
    public static String N6_MSG="Design time spreadsheets and their defintion files must be under the /src/ directory with the same package path as runtime spreadsheets";
    
    public CheckerConstants() {
        super();
    }
}
