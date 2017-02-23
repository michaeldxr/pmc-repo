package oracle.apps.di.checker;

import java.util.ArrayList;

import oracle.apps.di.ADFDiWorkbook;
import oracle.apps.di.CheckerConstants;
import oracle.apps.di.metadata.ADFDiComponentMetadata;
import oracle.apps.di.metadata.ADFDiMetadata;
import oracle.apps.di.metadata.ButtonMetadata;

public class ButtonChecker extends DIGenericStandardsChecker{
  
    public ButtonChecker() {
        super();
    }

    protected ArrayList<CheckerResult> doCheck(ADFDiWorkbook workbook,
                                               boolean getOnlyFailedResults) {
        String issue = CheckerConstants.ISSUE_COMPOENTS_BUTTON;
        String subIssue = "Components: There must be no ADF buttons on the worksheet";
        String code = "C_1";
        
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        
        ArrayList<ADFDiMetadata> buttons=workbook.getComponentsOfType(ADFDiMetadata.META_BUTTON);
        if(buttons !=null && buttons.size()>0){
            for(int i=0;i<buttons.size();i++){
                if(buttons.get(i) instanceof ButtonMetadata){
                    String label = ((ADFDiComponentMetadata)buttons.get(i)).getValue();
                    String ret=String.format("The Button of %s exist on the worksheet and please remove them to the Ribbon Button", label);
                    System.out.println("ret:"+ret);      
                    
                    results.add(new CheckerResult(code,issue,subIssue,ret,false)); 
                }
            }
        }
        
            
        return results;
    }
}
