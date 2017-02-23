package oracle.apps.di.checker;

import java.util.ArrayList;

import java.util.HashSet;
import java.util.Iterator;

import oracle.apps.di.ADFDiWorkbook;
import oracle.apps.di.CheckerConstants;
import oracle.apps.di.metadata.ADFDiComponentMetadata;
import oracle.apps.di.metadata.ADFDiMetadata;
import oracle.apps.di.metadata.ActionSet;
import oracle.apps.di.metadata.ActionSet.*;
import oracle.apps.di.metadata.TableMetadata;

import oracle.apps.di.metadata.Worksheet;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class TableChecker extends DIGenericStandardsChecker {
    boolean runT1 = false;
    boolean runT2 = false;
    boolean runT3 = false;
    boolean runT4 = false;
    boolean runT5 = false;
    boolean runT6= false;
    boolean runT7= false;
    boolean runT8 = false;
    public TableChecker() {
        super();
    }

    public void setRunT1(boolean runT1) {
        this.runT1 = runT1;
    }

    public boolean isRunT1() {
        return runT1;
    }

    @Override
    protected ArrayList<CheckerResult> doCheck(ADFDiWorkbook workbook, boolean getOnlyFailedResults) throws Exception {
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        if (runT1)
            results.addAll(runT1_tableKeyColumn(workbook));
        if (runT2)
            results.addAll(runT2_statusCol(workbook));
        if (runT3)
            results.addAll(runT3_cachedAttr(workbook));
        if (runT4)
            results.addAll(runT4_commitAction(workbook));
        if (runT5)
            results.addAll(runT5_LimitBatchSize(workbook));
        if (runT6)
            results.addAll(runT6_InsertBeforeRowAction(workbook));
        if (runT7)
            results.addAll(runT7_BatchSize100(workbook));
        if (runT8)
            results.addAll(runT8_DependentDropdown(workbook));
        
        return results;
    }

    public ArrayList<CheckerResult> runT1_tableKeyColumn(ADFDiWorkbook workbook) {
        String issue = CheckerConstants.ISSUE_TABLEKEY;
        String subIssue = "Table:KeyColumn required";
        String code = "T_1";

        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        ArrayList<ADFDiMetadata> tableMetadata = workbook.getComponentsOfType(ADFDiMetadata.META_TABLE);
        if (tableMetadata == null || tableMetadata.size() < 1)
            return results;
        Iterator<ADFDiMetadata> tableMetaIter = tableMetadata.iterator();
        while (tableMetaIter.hasNext()) {
            ADFDiMetadata meta = tableMetaIter.next();
            if (meta instanceof TableMetadata) {
                boolean tableHasRowKeyColumn = false;
                TableMetadata tableMeta = (TableMetadata)meta;
                ArrayList<TableMetadata.Column> tableColumns = tableMeta.getColumns();
                if (tableColumns == null)
                    continue;
                Iterator<TableMetadata.Column> columnIter = tableColumns.iterator();
                while (columnIter.hasNext()) {
                    TableMetadata.Column col = columnIter.next();
                    if (col == null)
                        continue;
                    String id = col.getId();
                    if (id == null)
                        continue;
                    if ("_ADF_RowKeyColumn".equals(id)) {
                        tableHasRowKeyColumn = true;
                        break;
                    }
                }
                // Violation - Table does not have row key column
                if (!tableHasRowKeyColumn) {
                    results.add(new CheckerResult(code, issue, subIssue,
                                                  String.format("Table with ID: %s does not have _ADF_RowKeyColumn", tableMeta.getComponentId()),
                                                  false));
                }
            }
        }
        return results;
    }

    public boolean doesTableDoMDL(TableMetadata tableMeta) {
        if (tableMeta == null)
            return false;

        if (tableMeta.getInsertRowEnabled().equalsIgnoreCase("true") || tableMeta.getUpdateRowEnabled().equalsIgnoreCase("true") ||
            tableMeta.getDeleteRowEnabled().equalsIgnoreCase("true"))
            return true;
        return false;
    }

    public ArrayList<CheckerResult> runT2_statusCol(ADFDiWorkbook workbook) {
        String issue = CheckerConstants.ISSUE_TABLE_STATUSCOL;
        String subIssue = "Table:DMLColumns required";
        String code = "T_2";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        ArrayList<ADFDiMetadata> tableMetadata = workbook.getComponentsOfType(ADFDiMetadata.META_TABLE);
        if (tableMetadata == null || tableMetadata.size() < 1)
            return results;
        Iterator<ADFDiMetadata> tableMetaIter = tableMetadata.iterator();
        while (tableMetaIter.hasNext()) {
            ADFDiMetadata meta = tableMetaIter.next();
            if (meta instanceof TableMetadata) {

                boolean tableHasStatusColumn = false;
                boolean tableHasChangeColumn = false;
                boolean tableHasFlaggedColumn = false;
                ActionSet as = null;
                ArrayList<Action> asacts = new ArrayList<Action>();
                
                boolean tableHasStatusColumnAndDisplayRowErrors = false;
                TableMetadata tableMeta = (TableMetadata)meta;
                boolean tableDoesDML = doesTableDoMDL(tableMeta);
                boolean deleteEnabled = false;
                boolean insertEnabled = false;
                boolean updateEnabled=false;
                if(tableMeta.getDeleteRowEnabled().equalsIgnoreCase("true"))
                    deleteEnabled=true;
                else if(tableMeta.getDeleteRowEnabled().equalsIgnoreCase("false"))
                    deleteEnabled=false;
                
                if(tableMeta.getInsertRowEnabled().equalsIgnoreCase("true"))
                    insertEnabled=true;
                else if(tableMeta.getInsertRowEnabled().equalsIgnoreCase("false"))
                    insertEnabled=false;
                
                if(tableMeta.getUpdateRowEnabled().equalsIgnoreCase("true"))
                    updateEnabled=true;
                else if(tableMeta.getUpdateRowEnabled().equalsIgnoreCase("false"))
                    updateEnabled=false;
                
                ArrayList<TableMetadata.Column> tableColumns = tableMeta.getColumns();
                if (tableColumns == null)
                    continue;
                Iterator<TableMetadata.Column> columnIter = tableColumns.iterator();
                while (columnIter.hasNext()) {
                    TableMetadata.Column col = columnIter.next();
                   
                    if (col == null)
                        continue;
                    String id = col.getId();
                    if (id == null)
                        continue;
                    if ("_ADF_StatusColumn".equals(id)){
                        tableHasStatusColumn = true;
                        Node unode = col.getUnode();
                        Node inode = col.getInode();    
                        if ("false".equals(col.getInsertUsesUpdate())) {
                            //Loop through insertComponent
                            try {
                                as = ActionSet.getMetadata((Element)inode);
                            } catch (Exception e) {
                                System.out.println(e);
                            }
                            ArrayList<ActionSet.Action> ac = as.getActions();
                            ActionSet.Action a = null;
                            Iterator<ActionSet.Action> actionIter =
                                ac.iterator();
                            while (actionIter.hasNext()) {
                                a = actionIter.next();
                                if (null != a) {
                                    if ("DisplayRowErrors".equals(a.getMethod()))
                                        tableHasStatusColumnAndDisplayRowErrors =
                                                true;
                                }

                            }

                        } else{
                            //Loop through updateComponent
                            try {
                                as = ActionSet.getMetadata((Element)unode);
                            } catch (Exception e) {
                                System.out.println(e);
                            }
                            ArrayList<ActionSet.Action> ac = as.getActions();
                            ActionSet.Action a = null;
                            Iterator<ActionSet.Action> actionIter =
                                ac.iterator();
                            while (actionIter.hasNext()) {
                                a = actionIter.next();
                                if (null != a) {
                                    if ("DisplayRowErrors".equals(a.getMethod()))
                                        tableHasStatusColumnAndDisplayRowErrors =
                                                true;
                                }

                            }
                        }
                            
                    }
                    if ("_ADF_ChangedColumn".equals(id))
                        tableHasChangeColumn = true;
                    if ("_ADF_FlagColumn".equals(id))
                        tableHasFlaggedColumn = true;

                }
                // Violation - Table does not have row key column
                if (tableDoesDML && !tableHasStatusColumn) {
                    results.add(new CheckerResult(code, issue, subIssue,
                                                  String.format("Table with ID: %s does DML and does not have _ADF_StatusColumn",
                                                                tableMeta.getComponentId()), false));
                }
                // Violation - Table does not have row key column
                if ((insertEnabled || updateEnabled) && !tableHasChangeColumn) {
                    results.add(new CheckerResult(code, issue, subIssue,
                                                  String.format("Table with ID: %s does DML and does not have _ADF_ChangedColumn",
                                                                tableMeta.getComponentId()), false));
                }
                if (tableMeta.getDeleteRowEnabled().equalsIgnoreCase("true") && !tableHasFlaggedColumn) {
                    results.add(new CheckerResult(code, issue, subIssue,
                                                  String.format("Table with ID: %s does Delete and does not have _ADF_FlagColumn",
                                                                tableMeta.getComponentId()), false));
                }
                if (tableDoesDML && tableHasStatusColumn && !tableHasStatusColumnAndDisplayRowErrors){
                    results.add(new CheckerResult(code, issue, subIssue,
                                                  String.format("Table with ID: %s does DML and  have _ADF_StatusColumn, but not DisplayRowErrors in DoubleClickActionSet",
                                                                tableMeta.getComponentId()), false)); 
                }
            }
        }
        return results;
    }
    public ArrayList<CheckerResult> runT4_commitAction(ADFDiWorkbook workbook) {
        String issue = CheckerConstants.ISSUE_TABLE_COMMITACTION;
        String subIssue = "Table:CommitBatchActionId should call AM Method to do commit";
        String code = "T_4";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        ArrayList<ADFDiMetadata> tableMetadata = workbook.getComponentsOfType(ADFDiMetadata.META_TABLE);
        if (tableMetadata == null || tableMetadata.size() < 1)
            return results;
        Iterator<ADFDiMetadata> tableMetaIter = tableMetadata.iterator();
        while (tableMetaIter.hasNext()) {
            ADFDiMetadata meta = tableMetaIter.next();
            if (meta instanceof TableMetadata) {

                ActionSet as = null;
                 
                TableMetadata tableMeta = (TableMetadata)meta;
                boolean tableDoesDML = doesTableDoMDL(tableMeta);
                boolean isCommitActionEnabledInBatchOptions=false;
                
                
                if(tableMeta.getcommitBatchEnabled().equalsIgnoreCase("true"))
                    isCommitActionEnabledInBatchOptions=true;
                else  if(tableMeta.getcommitBatchEnabled().equalsIgnoreCase("false"))
                    isCommitActionEnabledInBatchOptions=false;
                
                // Violation - Table does not CommitBatchActionId
                if (tableDoesDML && !isCommitActionEnabledInBatchOptions) {
                    results.add(new CheckerResult(code, issue, subIssue,
                                                  String.format("Table with ID: %s does DML and does not have AM Method calling commit in CommitBatchActionID",
                                                                tableMeta.getComponentId()), false));
                }
                
            }
        }
        return results;
    }

    public ArrayList<CheckerResult> runT5_LimitBatchSize(ADFDiWorkbook workbook) {
        String issue = CheckerConstants.ISSUE_TABLE_LIMITBATCHSIZE;
        String subIssue = "Table:LimitBatchSize should be true";
        String code = "T_5";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        ArrayList<ADFDiMetadata> tableMetadata =
            workbook.getComponentsOfType(ADFDiMetadata.META_TABLE);
        if (tableMetadata == null || tableMetadata.size() < 1)
            return results;
        Iterator<ADFDiMetadata> tableMetaIter = tableMetadata.iterator();
        while (tableMetaIter.hasNext()) {
            ADFDiMetadata meta = tableMetaIter.next();
            if (meta instanceof TableMetadata) {

                TableMetadata tableMeta = (TableMetadata)meta;

                boolean insertEnabled = false;
                boolean isLimitBatchSizeEnabled = false;


                boolean tableDoesDML = doesTableDoMDL(tableMeta);

                if (tableMeta.getlimitBatchSize().equalsIgnoreCase("true"))
                    isLimitBatchSizeEnabled = true;
                else if (tableMeta.getlimitBatchSize().equalsIgnoreCase("false"))
                    isLimitBatchSizeEnabled = false;

                // Violation - Table does not CommitBatchActionId
                if (tableDoesDML && !isLimitBatchSizeEnabled) {
                    results.add(new CheckerResult(code, issue, subIssue,
                                                  String.format("Table with ID: %s does DML and LimitBatchSize is not true",
                                                                tableMeta.getComponentId()),
                                                  false));
                }

            }
        }
        return results;
    }
    
    public ArrayList<CheckerResult> runT6_InsertBeforeRowAction(ADFDiWorkbook workbook) {
        String issue = CheckerConstants.ISSUE_TABLE_INSERTROWACTION;
        String subIssue = "Table:InsertBeforeRowActionID should call a method to create a new row";
        String code = "T_6";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        ArrayList<ADFDiMetadata> tableMetadata = workbook.getComponentsOfType(ADFDiMetadata.META_TABLE);
        if (tableMetadata == null || tableMetadata.size() < 1)
            return results;
        Iterator<ADFDiMetadata> tableMetaIter = tableMetadata.iterator();
        while (tableMetaIter.hasNext()) {
            ADFDiMetadata meta = tableMetaIter.next();
            if (meta instanceof TableMetadata) {

              
                TableMetadata tableMeta = (TableMetadata)meta;
              
                boolean insertEnabled = false;
               
                boolean containInsertBeforeRowActionID=false;
               // boolean tableDoesDML = doesTableDoMDL(tableMeta);
                if(tableMeta.getInsertRowEnabled().equalsIgnoreCase("true"))
                    insertEnabled=true;
                else if(tableMeta.getInsertRowEnabled().equalsIgnoreCase("false"))
                    insertEnabled=false;
                
              
               
                if(tableMeta.getInsertBeforeRowAction().equalsIgnoreCase("true"))
                    containInsertBeforeRowActionID=true;
                else  if(tableMeta.getInsertBeforeRowAction().equalsIgnoreCase("false"))
                    containInsertBeforeRowActionID=false;
                // Violation - Table does not CommitBatchActionId
                if (insertEnabled && !containInsertBeforeRowActionID) {
                    results.add(new CheckerResult(code, issue, subIssue,
                                                  String.format("Table with ID: %s does insert and InsertBeforeRowActionID doesn't contain a method call",
                                                                tableMeta.getComponentId()), false));
                }
                
            }
        }
        return results;
    }

    public ArrayList<CheckerResult> runT8_DependentDropdown(ADFDiWorkbook workbook) {
        String issue = CheckerConstants.ISSUE_DEPDENDENTDROPDOWN;
        String subIssue = "List of Dependent Dropdowns";
        String code = "T_8";
         ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        ArrayList<ADFDiMetadata> tableMetadata =
            workbook.getComponentsOfType(ADFDiMetadata.META_TABLE);
        if (tableMetadata == null || tableMetadata.size() < 1)
            return results;
        Iterator<ADFDiMetadata> tableMetaIter = tableMetadata.iterator();
        while (tableMetaIter.hasNext()) {
            ADFDiMetadata meta = tableMetaIter.next();
            if (meta instanceof TableMetadata) {
                 TableMetadata tableMeta = (TableMetadata)meta;

                ArrayList<TableMetadata.Column> tableColumns =
                    tableMeta.getColumns();
                if (tableColumns == null)
                    continue;
                Iterator<TableMetadata.Column> columnIter =
                    tableColumns.iterator();
                while (columnIter.hasNext()) {
                     TableMetadata.Column col = columnIter.next();
                    if (col == null)
                        continue;

                    //get List ID
                    String ListId = null;
                    ListId = retrieveListId(col);
                     if (ListId != null) {
                        ArrayList<ADFDiMetadata> sheetMetas =
                            workbook.getComponentsOfType(ADFDiMetadata.META_WORKSHEET);

                        HashSet<String> pageDefs = new HashSet<String>();
                        Iterator<ADFDiMetadata> sheetIter =
                            sheetMetas.iterator();
                        String pdefName = null;
                        while (sheetIter.hasNext()) {
                            ADFDiMetadata sheetmeta = sheetIter.next();
                            if (sheetmeta instanceof Worksheet) {
                                try {
                                Worksheet sheetMeta = (Worksheet)sheetmeta;
                                pdefName = sheetMeta.getPageDefName();
                                } catch (Exception e){
                                    System.out.println(e.toString());
                                }

                            }


                            if (null != pdefName && !("".equals(pdefName))) {
                                if (!pdefName.contains("no page definition selected")){
                                try {
                                    results.add(new CheckerResult(code, issue, subIssue,
                                                                  String.format("PageDef: %s contains Dependent Drop down Binding %s",
                                                                                pdefName,ListId), false));
                                } catch (Exception e) {
                                    System.out.println("exception " + e);
                                }
                            }
                            }
                        }
                    }

                }


            }
        }
        return results;
    }
    public ArrayList<CheckerResult> runT7_BatchSize100(ADFDiWorkbook workbook) {
         String issue = CheckerConstants.ISSUE_TABLE_BATCHSIZE100;
        String subIssue = "Table:BatchSize should be 100";
        String code = "T_7";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        ArrayList<ADFDiMetadata> tableMetadata = workbook.getComponentsOfType(ADFDiMetadata.META_TABLE);
        if (tableMetadata == null || tableMetadata.size() < 1)
            return results;
        Iterator<ADFDiMetadata> tableMetaIter = tableMetadata.iterator();
        while (tableMetaIter.hasNext()) {
            ADFDiMetadata meta = tableMetaIter.next();
            if (meta instanceof TableMetadata) {

              
                TableMetadata tableMeta = (TableMetadata)meta;
              
               
                boolean batchsize100=false;
                
               
                boolean tableDoesDML = doesTableDoMDL(tableMeta);
              
               
                if(tableMeta.getBatchsize().equalsIgnoreCase("100")){
                     batchsize100=true;
                }
                else { 
                     batchsize100=false;
                }
                // Violation - Table does not CommitBatchActionId
                if (tableDoesDML && !batchsize100) {
                    results.add(new CheckerResult(code, issue, subIssue,
                                                  String.format("Table with ID: %s does DML and BatchSize value is not 100",
                                                                tableMeta.getComponentId()), false));
                }
                
            }
        }
        return results;
    }
    public ArrayList<CheckerResult> runT3_cachedAttr(ADFDiWorkbook workbook) {
        String issue = CheckerConstants.ISSUE_TABLE_CACHEDATTR;
        String subIssue = "Table:cachedAttrs must not be duplicated";
        String code = "T_3";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        ArrayList<ADFDiMetadata> tableMetadata = workbook.getComponentsOfType(ADFDiMetadata.META_TABLE);
        if (tableMetadata == null || tableMetadata.size() < 1)
            return results;
        Iterator<ADFDiMetadata> tableMetaIter = tableMetadata.iterator();
        while (tableMetaIter.hasNext()) {
            ADFDiMetadata meta = tableMetaIter.next();
            if (meta instanceof TableMetadata) {

                boolean hasCachedAttrs = false;
                TableMetadata tableMeta = (TableMetadata)meta;
                ArrayList<String> cachedAttrs = tableMeta.getCachedAttrs();
                if (cachedAttrs != null && cachedAttrs.size() > 0)
                    hasCachedAttrs = true;

                if (hasCachedAttrs) {
                    ArrayList<TableMetadata.Column> tableColumns = tableMeta.getColumns();
                    if (tableColumns == null)
                        continue;
                    Iterator<TableMetadata.Column> columnIter = tableColumns.iterator();
                    while (columnIter.hasNext()) {
                        TableMetadata.Column col = columnIter.next();
                        if (col == null)
                            continue;

                        Iterator<String> cachedAttrsIter = cachedAttrs.iterator();
                        while (cachedAttrsIter.hasNext()) {
                            String cachedA = cachedAttrsIter.next();
                            if (cachedA == null)
                                continue;
                            // Violation: Column contains same cached attr
                            if (doesColumnValueHave(col, cachedA)) {
                                results.add(new CheckerResult(code, issue, subIssue,
                                                              String.format("Table ID: %s ColumnID:%s has value same as cached attribute: %s",
                                                                            tableMeta.getComponentId(), col.getId(), cachedA), false));
                            }
                        }
                    }
                }

            }
        }
        return results;
    }

    public boolean doesColumnValueHave(TableMetadata.Column col, String valueToCheck) {
        if (col == null)
            return false;
        ADFDiComponentMetadata updateComp = col.getUpdateComponent();
        if (updateComp != null) {
            String val = updateComp.getValue();
            if (val != null && val.trim().equals(valueToCheck))
                return true;
        }

        ADFDiComponentMetadata insertComp = col.getInsertComponent();
        if (insertComp != null) {
            String val = insertComp.getValue();
            if (val != null && val.trim().equals(valueToCheck))
                return true;
        }

        return false;
    }
    private String retrieveListId(TableMetadata.Column col) {
        ADFDiComponentMetadata updateComp = col.getUpdateComponent();
        String dependOnListId = null;
        String UpdateListId = null;
        String InsertListId = null;
        try {
            if (updateComp != null) {
                dependOnListId = updateComp.getDependsOnListId();
                if (null != dependOnListId)
                    if (null != dependOnListId &&
                        !("".equals(dependOnListId))) {
                        UpdateListId = updateComp.getListId();
                    }

            }
            ADFDiComponentMetadata insertComp = col.getInsertComponent();
            if (insertComp != null) {
                dependOnListId = insertComp.getDependsOnListId();
                if (null != dependOnListId)
                    if (null != dependOnListId &&
                        !("".equals(dependOnListId))) {
                        InsertListId = updateComp.getListId();
                    }
            }
            //       if (null != InsertListId)
            //        System.out.println("InsertListId "+InsertListId);
            //        if (null != UpdateListId)
            //        System.out.println("UpdateListId "+UpdateListId);
            if (null != InsertListId && null != UpdateListId)
                if (!InsertListId.equals(UpdateListId))
                    return InsertListId + ',' + UpdateListId;
            if (null != InsertListId)
                return InsertListId;
            if (null != UpdateListId)
                return UpdateListId;
            if (null == UpdateListId && null == InsertListId)
                return null;
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public void setRunT2(boolean runT2) {
        this.runT2 = runT2;
    }

    public boolean isRunT2() {
        return runT2;
    }

    public void setRunT3(boolean runT3) {
        this.runT3 = runT3;
    }

    public boolean isRunT3() {
        return runT3;
    }
    public boolean isRunT4() {
        return runT4;
    }
    public void setRunT4(boolean runT4) {
        this.runT4 = runT4;
    }
    public boolean isRunT5() {
        return runT5;
    }
    public void setRunT5(boolean runT5) {
        this.runT5 = runT5;
    }
    public boolean isRunT6() {
        return runT6;
    }
    public void setRunT6(boolean runT6) {
        this.runT6 = runT6;
    }
    public boolean isRunT7() {
        return runT7;
    }
    public void setRunT7(boolean runT7) {
        this.runT7 = runT7;
    }

    public void setRunT8(boolean runT8) {
        this.runT8 = runT8;
    }

    public boolean isRunT8() {
        return runT8;
    }
}
