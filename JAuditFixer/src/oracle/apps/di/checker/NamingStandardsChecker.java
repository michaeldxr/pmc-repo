package oracle.apps.di.checker;

import java.io.File;

import java.util.ArrayList;

import java.util.List;

import oracle.apps.di.ADFDiWorkbook;
import oracle.apps.di.CheckerConstants;

public class NamingStandardsChecker extends DIGenericStandardsChecker {
    boolean runN1 = false;
    boolean runN2 = false;
    boolean runN3 = false;
    boolean runN4 = false;

    //For ADFdi customization
    boolean runN6 = false;

    public NamingStandardsChecker() {
        super();
    }

    public String getStandardName() {
        return getStandardCode() + " - " + CheckerConstants.N_DESC;
    }

    public String getStandardCode() {
        return "N";
    }

    public static void main(String[] args) throws Exception {
        String p =
            "/scratch/zcurrimb/view_storage/zcurrimb_rup2st2cdrm/fusionapps/fin/components/ledger/gl/journals/desktopEntry/di/public_html/oracle/apps/financials/generalLedger/journals/desktopEntry/di/excel/CreateJournalDT.xlsx";
        NamingStandardsChecker checker = new NamingStandardsChecker();
        ArrayList<CheckerResult> checkerResults = checker.check(p, null, true);
        checker.printResults(checkerResults);

    }

    /**
     * doCheck is the entry point. This method should be overriden by all checkers
     * @param getOnlyFailedResults
     * @return
     */
    protected ArrayList<CheckerResult> doCheck(ADFDiWorkbook workbook,
                                               boolean getOnlyFailedResults) throws Exception {
        ArrayList<CheckerResult> checkerResults =
            new ArrayList<CheckerResult>();
        if (runN1)
            addResult(checkerResults, runN1(), getOnlyFailedResults);
        if (runN2)
            addResult(checkerResults, runN2_SrcControlStd(),
                      getOnlyFailedResults);
        if (runN3)
            addResult(checkerResults, runN3(), getOnlyFailedResults);
        if (runN4)
            addResult(checkerResults, runN4(), getOnlyFailedResults);

        if (runN6)
            addResults(checkerResults, runN6());
        return checkerResults;
    }


    /**
     * Rule: design Time spreadhseets must end with DT.xlsx
     * @return
     */
    public CheckerResult runN1() {
        String code = "N_1";
        String subIssue = CheckerConstants.N1_MSG;
        CheckerResult result = null;

        String fileName = getFileNameWithoutExt();
        if (isEmpty(fileName))
            return null;

        if (fileName.endsWith("DT"))
            result =
                    new CheckerResult(code, CheckerConstants.ISSUE_NAMINGSTANDARDS,
                                      subIssue, true);
        else {
            if (isSpreadsheetInSrcDirectory() && !fileName.endsWith("DT")) {
                result =
                        new CheckerResult(code, CheckerConstants.ISSUE_NAMINGSTANDARDS,
                                          subIssue, true);
                result.setDescription("Design time spreadsheet (present in src directory) does not contain suffix DT");
            }

        }
        return result;
    }

    public CheckerResult runN2_SrcControlStd() {
        String code = "N_2";
        String subIssue = CheckerConstants.N2_MSG;
        CheckerResult result = null;


        if (getSpreadsheetPath().contains("/di/"))
            result =
                    new CheckerResult(code, CheckerConstants.ISSUE_NAMINGSTANDARDS,
                                      subIssue, true);
        else
            result =
                    new CheckerResult(code, CheckerConstants.ISSUE_NAMINGSTANDARDS,
                                      subIssue, true);

        return result;
    }

    public CheckerResult runN3() {
        String code = "N_3";
        String subIssue = CheckerConstants.N3_MSG;
        CheckerResult result = null;

        String fileName = getFileNameWithoutExt();
        if (isEmpty(fileName))
            return null;

        if (fileName.endsWith("DT") && !isSpreadsheetInSrcDirectory())
            result =
                    new CheckerResult(code, CheckerConstants.ISSUE_NAMINGSTANDARDS,
                                      subIssue, true);
        else
            result =
                    new CheckerResult(code, CheckerConstants.ISSUE_NAMINGSTANDARDS,
                                      subIssue, true);

        return result;
    }

    public CheckerResult runN4() {
        String code = "N_4";
        String subIssue = CheckerConstants.N4_MSG;
        CheckerResult result = null;

        String fileName = getFileNameWithoutExt();
        if (isEmpty(fileName))
            return null;

        // NotDIT = publishedspreadsheet
        if (!fileName.endsWith("DT") && !isSpreadsheetInPubHtmlDirectory())
            result =
                    new CheckerResult(code, CheckerConstants.ISSUE_NAMINGSTANDARDS,
                                      subIssue, true);
        else
            result =
                    new CheckerResult(code, CheckerConstants.ISSUE_NAMINGSTANDARDS,
                                      subIssue, true);

        return result;
    }

    public ArrayList<CheckerResult> runN6() {

        String code = "N_6";
        String subIssue = CheckerConstants.N6_MSG;
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();

        String fileName = getFileNameWithoutExt();
        if (isEmpty(fileName)) {
            return results;
        }

        if (!fileName.endsWith("DT") && isSpreadsheetInPubHtmlDirectory()) {
            String designTimeFileName =
                getSpreadsheetPath().replace("/public_html/", "/src/");
            designTimeFileName =
                    designTimeFileName.replace("." + getExtension(), "DT." + getExtension());

            StringBuilder defFileName = new StringBuilder(256);
            defFileName.append(designTimeFileName);
            defFileName.append("-workbook-definition.xml");

            File designTimeFile = new File(designTimeFileName);
            File defFile = new File(defFileName.toString());

            if (!designTimeFile.exists()) {
                CheckerResult result =
                    new CheckerResult(code, CheckerConstants.ISSUE_NAMING_PATH,
                                      subIssue,
                                      "Design time spreedsheet can't be found under /src/ directory with the same package path as runtime spreadsheet",
                                      false);
                results.add(result);
            }
            if (!defFile.exists()) {
                CheckerResult result =
                    new CheckerResult(code, CheckerConstants.ISSUE_NAMING_PATH,
                                      subIssue,
                                      "Design time spreedsheet definition file can't be found under /src/ directory with the same package path as runtime spreadsheet",
                                      false);
                results.add(result);
            }
        }
        return results;
    }

    public boolean isSpreadsheetInSrcDirectory() {
        if (getSpreadsheetPath().contains("/src/"))
            return true;
        return false;
    }

    public boolean isSpreadsheetInPubHtmlDirectory() {
        if (getSpreadsheetPath().contains("/public_html/"))
            return true;
        return false;
    }

    public void setRunN1(boolean runN1) {
        this.runN1 = runN1;
    }

    public boolean isRunN1() {
        return runN1;
    }

    public void setRunN2(boolean runN2) {
        this.runN2 = runN2;
    }

    public boolean isRunN2() {
        return runN2;
    }

    public void setRunN3(boolean runN3) {
        this.runN3 = runN3;
    }

    public boolean isRunN3() {
        return runN3;
    }

    public void setRunN4(boolean runN4) {
        this.runN4 = runN4;
    }

    public boolean isRunN4() {
        return runN4;
    }

    public void setRunN6(boolean runN6) {
        this.runN6 = runN6;
    }

    public boolean isRunN6() {
        return runN6;
    }
}
