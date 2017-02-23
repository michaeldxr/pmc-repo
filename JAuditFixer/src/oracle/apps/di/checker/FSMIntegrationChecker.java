package oracle.apps.di.checker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.di.CheckerConstants;
import oracle.apps.helpers.RegExScanner;

public class FSMIntegrationChecker {
    String portletRegEx = "javax\\.portlet\\.faces\\.ViewLink=false";
    Pattern portletRegExPattern = Pattern.compile(portletRegEx);
    ArrayList<String> exceptions = new ArrayList<String>();


    public FSMIntegrationChecker() {
        super();
    }

    public static void main(String[] args) throws Exception {
        //        FSMIntegrationChecker checker = new FSMIntegrationChecker();
        //        String str = "destination=\"/oracle/apps/hcm/profiles/contentLibrary/publicDi/excel/CompetencyContentItem.xlsx?javax.portlet.faces.ViewLink=false\"/>\n";
        //        Matcher m = checker.portletRegExPattern.matcher(str);
        //        if(m.find())
        //        {
        //            System.out.println("Found match: " + m.group(0));
        //            }
        //        String path = "/ade../FUSIONAPPS_PT.V1RUP3DEV_LINUX.X64.rdd/LATEST/fusionapps/hcm/components/hcmCoreSetup/profiles/contentLibrary/ui/public_html/oracle/apps/hcm/profiles/contentLibrary/ui/page/ManageContentItem.jsff";
        //        ArrayList<CheckerResult> results = checker.checkJsffForFSMIntegrationIssue(path);
        //        if(results==null || results.size()<1)
        //            System.out.println("No results");

        //String digitsRegEx="\\d+[\\.\\d+]{0,1}";
        String digitsRegEx = "\\d+(\\.\\d+){0,1}";
        Pattern pattrn = Pattern.compile(digitsRegEx);
        String toTest = "Branding Item Value 123.45";
        Matcher m1 = pattrn.matcher(toTest);
        while (m1.find()) {
            System.out.println("Found: " + m1.group());
        }


    }


    public ArrayList<CheckerResult> checkJsffForFSMIntegrationIssue(String jsffPagePath) {
        String code = "I_1";
        
        String subIssue = "FSMIntegration:PortletParams";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        try {

            RegExScanner scanner = new RegExScanner(jsffPagePath, "xml");
            ArrayList<RegExScanner.RegExScanResult> scanResults = scanner.scanFor(portletRegExPattern);
            if (scanResults != null && scanResults.size() > 0) {
                Iterator<RegExScanner.RegExScanResult> scanResultIter = scanResults.iterator();
                while (scanResultIter.hasNext()) {

                    RegExScanner.RegExScanResult scanResult = scanResultIter.next();
                    String line = scanResult.getMatchingLine();
                    if (line == null)
                        continue;
                    if (line.indexOf("xlsx") < 0) {
                        System.out.println("This line does not seem to reference a spreadsheet call: " + line);
                        continue;
                    }
                    String lineformatted = scanResult.getMatchingLine();
                    if(lineformatted!=null)
                        lineformatted=lineformatted.replaceAll(",", "#");
                    else
                        lineformatted="";
                    results.add(new CheckerResult(code,CheckerConstants.ISSUE_FSM,subIssue,
                                                  String.format("Line: %s Page uses portlet parameters: %s", scanResult.getLineNumber(), lineformatted.trim()),
                                                  false));

                }
            }
        } catch (Exception ex) {
            exceptions.add(String.format("Error while checking jsff for fsm integration issue. Exception : %s", ex.getMessage()));
        }
        return results;
    }
}
