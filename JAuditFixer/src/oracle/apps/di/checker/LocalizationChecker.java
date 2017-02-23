package oracle.apps.di.checker;

import java.util.ArrayList;

import java.util.Iterator;

import oracle.apps.di.ADFDiWorkbook;
import oracle.apps.di.CheckerConstants;
import oracle.apps.di.SharedStringsParser;
import oracle.apps.di.metadata.ADFDiMetadata;
import oracle.apps.helpers.FamilyModuleHelper;

public class LocalizationChecker extends DIGenericStandardsChecker {

    public LocalizationChecker()  {
        super();
    }

    public static void main(String[] args) throws Exception {
        String p = FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.V1RUP3DEV_LINUX.X64") + 
            "/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/public_html/oracle/apps/financials/generalLedger/journals/desktopEntry/di/excel/CreateJournalDT.xlsx";
        LocalizationChecker checker = new LocalizationChecker();
        checker.check(p,null, true);
    }

    /**
     * doCheck is the entry point. This method should be overriden by all checkers
     * @param getOnlyFailedResults
     * @return
     */
    protected ArrayList<CheckerResult> doCheck(ADFDiWorkbook workbook,boolean getOnlyFailedResults) throws Exception {
       return workbook.runLocalicationChecks();
    }

//    public CheckerResult doMetadataStringsCheck() {
//        try {
//            //ArrayList<String> metadataStrings = SharedStringsParser.getListOfAllADFDiMetaStrings(_spreadsheetPath, "Workbook");
//            ArrayList<String> metadataStrings = SharedStringsParser.getListOfAllADFDiMetaStrings(_spreadsheetPath);
//            Iterator<String> metadataIter = metadataStrings.iterator();
//            while (metadataIter.hasNext()) {
//                String metadata = metadataIter.next();
//                ADFDiMetadata meta = ADFDiMetadata.getMetadata(metadata);
//                System.out.println("got metadata");
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//        return null;
//    }
//
//    public CheckerResult doWorkbookStringsCheck(ArrayList<String> workbookStrings) throws Exception {
//        Iterator<String> workbookStringIter = workbookStrings.iterator();
//        while (workbookStringIter.hasNext()) {
//            String workbookString = workbookStringIter.next();
//            ADFDiMetadata meta = ADFDiMetadata.getMetadata(workbookString);
//            System.out.println("got metadata");
//        }
//        return null;
//    }

   

}
