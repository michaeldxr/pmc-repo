package oracle.apps.di.checker;

import java.util.ArrayList;

import java.util.Iterator;

import oracle.apps.di.ADFDiWorkbook;
import oracle.apps.di.CheckerConstants;
import oracle.apps.di.metadata.ADFDiMetadata;
import oracle.apps.di.metadata.WorkbookMetadata;

public class StatusViewerChecker extends DIGenericStandardsChecker{
    boolean hasautoDisplayStatusViewermethod =false;
    boolean hasautoDisplayStatusViewerEnabled =false;
    public StatusViewerChecker() {
        super();
    }

    protected ArrayList<CheckerResult> doCheck(ADFDiWorkbook workbook,
                                               boolean getOnlyFailedResults) {
        String issue = CheckerConstants.ISSUE_COMPONENTS_STATUSVIWER;
        String subIssue = "Components: AutoDisplayStatusViewerEnabled should be set true and ToggleStatusViewer should be set on the workbook";
        String code = "V_1";
        
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        
        ArrayList<ADFDiMetadata> workbookMeta=workbook.getComponentsOfType(ADFDiMetadata.META_WORKBOOK);
        
        if (workbookMeta != null && workbookMeta.size() > 0) {
            Iterator<ADFDiMetadata> workbookMetaIter =
                workbookMeta.iterator();
            while (workbookMetaIter.hasNext()) {
                ADFDiMetadata meta = workbookMetaIter.next();
                if (meta == null)
                    continue;
                if (meta instanceof WorkbookMetadata) {
                    if( ((WorkbookMetadata)meta).getAutoDisplayStatusViewerEnabled() && ((WorkbookMetadata)meta).getAutoDisplayStatusViewerMehtod()){
                        System.out.println("StatusView is set correct");
                    }
                    else{                    
                        String ret=String.format("The method of ToggleStatusViewer should be set and the AutoDisplayStatusViewerEnabled should be set true on the workbook %s",((WorkbookMetadata)meta).getWorkbookId());
                        System.out.println("ret:"+ret);
                        results.add(new CheckerResult(code,issue,subIssue,ret,false));
                    }
                }
            }
        }
        return results;
    }
}

