package oracle.apps.di;

import java.io.File;

import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;

public class ADFdiAnalyzerLRG {

    private long start;

    private long end;

    static String standardRules =
        FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
        "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/di/config/Rules_R8_R9_R10.xml";

    public ADFdiAnalyzerLRG() {
        super();
    }

    public static int getRelease(String series) {
        int release = 8; //default release version
        String ret = FamilyModuleHelper.getRelease(series);
        try {
            release = Integer.parseInt(ret);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if(release ==11){
        standardRules =
                FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
                "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/di/config/Rules_R11.xml";
        }
        if(release ==12){
        standardRules =
                FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
                "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/di/config/Rules_R12.xml";
        }
        if(release == 13){
        standardRules =
                FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
                "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/di/config/Rules_R13.xml";
        }

        return release;
    }

    public static void main(String[] args) throws Exception {
        //Test Code
        // args = new String[] { "FUSIONAPPS_PT.DEVINT_LINUX.X64" };
        int release;
        if (args.length != 2 || args[0] == null || args[0].isEmpty()) {
            System.out.println("USAGE: ADFdiAnalyzerLRG <series>");
            System.exit(1);
        }
        String series = args[0];
        String resultPath = args[1];

        release = getRelease(series);
        System.out.println("release:" + release);
        System.out.println("standardrules:" + standardRules);

        ADFdiAnalyzerLRG analyzer = new ADFdiAnalyzerLRG();


        analyzer.markStartTime();
        String label = LRGUtil.getLatestLabel(series);
        System.out.println("Running ADFdi Scan on series: " + series +
                           ".......");

        String scanPath =
            FamilyModuleHelper.getLabelServerTop1(series) + "/" + series +
            ".rdd/" + label + "/fusionapps/";
        //Trigger Scaners
        ADFDiStandardsChecker.scan(new String[] { scanPath, series, args[1],
                                                        Integer.toString(release),
                                                        standardRules });
        ADFdiCheckDBWrapper.update(new String[] { resultPath, series });
        
        if (release >= 11) {
            RowLevelBindingCheck.scan(new String[] { scanPath, "false",
                                                     args[1] });
            RowLevelBindingsDBWrapper.update(new String[] { resultPath,
                                                            series });
        }
        
        if(release >=9){
            AdfDiVersionCheck.scan(new String[] { scanPath, series });
            ADFdiVersionDBWrapper.update(new String[] {resultPath, series});
        }
        //ADFdiSecurePageDef.scan(new String[] { series });

        //Trigger DBWrapper
        //    File directory = new File(".");
        //    String resultPath = directory.getCanonicalPath();

        //ADFdiCheckPrototypeDBWrapper.update(new String[]{resultPath, series});


        //ADFdiVersionPrototypeDBWrapper.update(new String[] {resultPath, series});

        //    RowLevelBindingsPrototypeDBWrapper.update(new String[] {resultPath, series});


        //ADFdiSecurePageDefDBWrapper.update(new String[] {resultPath, series});


        System.out.println("ADFdi Scan on series: " + series + " finished!");
    }


    public void markStartTime() {
        start = System.currentTimeMillis();
    }

    public void markEndTime() {
        end = System.currentTimeMillis();
        long e = (end - start) / 1000;

        System.out.println("Elasped Time is " + e + " seconds.");
    }
}
