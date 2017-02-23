package oracle.apps.messages;

import oracle.apps.utility.JoesBaseClass;

//import static org.junit.Assert.*;
//import org.junit.Test;

public class ParrallelCrawlerTest
{
    public ParrallelCrawlerTest()
    {
    }

    /**
     * @see ParrallelCrawler2#getPossibleJavaMessageReferences(String)
     */
   // @Test
    public void testGetPossibleJavaMessageReferences()
    {
        ParrallelCrawler pc2 = new ParrallelCrawler(ParrallelCrawler.CRAWL_TYPE.MESSAGE_SCANNER);
        try
        {
           // ParrallelCrawler.reviewMRTData();
            //pc2.processPkbFile("/scratch/albowicz/view_storage/albowicz_joe/fusionapps/prj/pjc/db/plsql/pjc_pjb_adjustments.pkh");
            
//ZOC_AVAILABLE_RULE_MESSAGE, /scratch/albowicz/view_storage/albowicz_joe/fusionapps/crm/components/sales/zoc/star/publicFlow/src/oracle/apps/orderCapture/core/star/publicFlow/productGroupEligibilityCompatibility/ProductGroupEligibilityCompatibility.xml

//            pc2.processJavaFile("/scratch/albowicz/view_storage/albowicz_joe/fusionapps/hcm/components/hcmCompensation/compensation/expressions/protectedModel/src/oracle/apps/hcm/compensation/expressions/protectedModel/common/DxprErrCodes.java");

//            pc2.processMessageFile("/scratch/albowicz/view_storage/albowicz_joe/fusionapps/crm/zoc/db/data/EligibilityCompatibility/MessageSD.xml");
//            pc2.processXmlFile("/scratch/albowicz/view_storage/albowicz_joe/fusionapps/crm/components/sales/zoc/star/publicFlow/src/oracle/apps/orderCapture/core/star/publicFlow/productGroupEligibilityCompatibility/ProductGroupEligibilityCompatibility.xml");

//            pc2.processMessageFile("/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/150126.1708.S/fusionapps/fin/ce/db/data/FinCeBankStmtSetup/MessageSD.xml");
//            pc2.processXmlFile("/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/150126.1708.S/fusionapps/fin/components/payables/ce/cashTrxSetup/model/src/oracle/apps/financials/cashManagement/cashTrxSetup/model/entity/BankStatementTransactionCreationRuleEO.xml");
//            
//            pc2.processXmlFile("/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/150126.1708.S/fusionapps/atf/components/commonComp/atk/devPortal/model/src/oracle/apps/atk/devPortal/model/entity/TechSamplePayloadEO.xml");
//
//            pc2.processMessageFile("/scratch/albowicz/view_storage/albowicz_joe/fusionapps/fin/ce/db/data/FinCeBankRelationships/MessageSD.xml");
//            pc2.processPkbFile("/scratch/albowicz/view_storage/albowicz_joe/fusionapps/fin/ce/db/plsql/ce_bank_and_account_validation.pkb");
//        pc2.processJavaFile("/scratch/albowicz/view_storage/albowicz_joe/fusionapps/fin/components/payables/exm/entry/workArea/ui/src/oracle/apps/financials/expenses/entry/workArea/ui/bean/ManageBankAccountBean.java");
//          pc2.processJavaFile("/scratch/albowicz/view_storage/albowicz_joe/fusionapps/fin/components/payables/exm/entry/workArea/ui/src/oracle/apps/financials/expenses/entry/workArea/ui/bean/ManageBankAccountBean.java");
          //pc2.processPkbFile("/scratch/albowicz/view_storage/albowicz_joe/fusionapps/fin/vrm/db/plsql/vrm_revenue_documents_internal.pkb");
          //pc2.processPkbFile("/scratch/albowicz/view_storage/albowicz_joe/fusionapps/scm/wsh/db/plsql/wsh_bols_util_pkg.pkb");
           // pc2.processXmlFile("/scratch/albowicz/view_storage/albowicz_joe/fusionapps/fin/components/ledger/gl/journals/journalEntries/ui/public_html/WEB-INF/oracle/apps/financials/generalLedger/journals/journalEntries/ui/flow/DrillDownToSlaFlow.xml");
            
//            /scratch/albowicz/view_storage/albowicz_joe/fusionapps/fin/components/payables/exm/entry/workArea/ui/src/oracle/apps/financials/expenses/entry/workArea/ui/bean/ManageBankAccountBean.java
              pc2.processPkbFile("/scratch/sadevula/view_storage/sadevula_r11fin3/fusionapps/fin/ce/db/ce_party_merge_pkg.pkb");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
       // assertFalse("just passes", false);
    }
}
