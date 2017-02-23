package oracle.apps.votuning_v2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import java.util.Iterator;

import oracle.apps.helpers.JprHelper;
import oracle.apps.sqlanalyzer.CaseInsensitiveViewCriteria;
import oracle.apps.sqlanalyzer.SqlIndexFinder;

public class LOVAnalysisPreMergeChecker {
    static RunLOVAnalysis lovAnalyzer = new RunLOVAnalysis();
    static ArrayList<String> jsffFilesInTransaction = new ArrayList<String>();

    public static void main(String[] args) throws Exception {

        String[] files = args[0].trim().split("\n");
        String ade_view_root = args[1].trim();

        RunLOVAnalysis.csvWriter = new BufferedWriter(new FileWriter("vo_scan_temp.csv"));

        for (int i = 0; i < files.length; i++) {
            String filePath = ade_view_root + "/" + files[i].trim();
            File f = new File(filePath);
            if (!f.exists())
                continue;
            // Want to include the voxml files as well. Incase people modify the vo.xml in txn, and there is a violation on that vo file,
            // then it should be flagged as a violation
            if (filePath.endsWith(".jsff"))
                jsffFilesInTransaction.add(files[i].trim());
            else if (filePath.endsWith("VO.xml") || filePath.endsWith("EO.xml"))
                jsffFilesInTransaction.add(files[i].trim());
        }

        if (jsffFilesInTransaction.size() < 1)
            return;

        RunLOVAnalysis.errorLogger = RunLOVAnalysis.initLogger();
        System.out.println("Running Table LOV Scan Pass1...");
        String sCrawlDir = RunLOVAnalysis.getPass1AnalysisPath(ade_view_root + "/"+jsffFilesInTransaction.get(0));
        if(sCrawlDir==null || sCrawlDir.equals(""))
            return;
        
        lovAnalyzer.runPass1(sCrawlDir);

        lovAnalyzer.scanAndMakeFixes(sCrawlDir, false);

        //lovAnalyzer.writeOutputXml(RunLOVAnalysis.onlyreqiured_analysisRecords, RunLOVAnalysis.onlyrequired_errorRecords, "lovresults.xml");

        RunLOVAnalysis.writeToSpreadsheet(lovAnalyzer.onlyreqiured_analysisRecords, "Results");

        RunLOVAnalysis.csvWriter.close();

        BufferedWriter outputFileWriter = new BufferedWriter(new FileWriter("vo_scan.csv"));

        BufferedReader reader = new BufferedReader(new FileReader("vo_scan_temp.csv"));
        String line = "";
        boolean hasViolation = false;

        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(",");
            if (parts.length < 21)
                continue;

            String fileName = parts[3].trim();
            String voPackage = (parts[18] != null) ? parts[18] : "";
            voPackage = voPackage.trim();


            if (jsffFilesInTransaction.contains(fileName) || isVoPackageInListOfTxnFiles(voPackage)) {
                hasViolation = true;
                outputFileWriter.write("FileName: " + fileName + "\n");
                outputFileWriter.write("Issue: " + VoTuningDBWrapperLRG.getDescription(parts[6], parts[20]) + "\n");
                outputFileWriter.write("Component: " + parts[5] + "(" + parts[6] + ")" + "\n");
                outputFileWriter.write("AutoHeightRows: " + parts[7] + "\n");
                outputFileWriter.write("Iterator Range Size: " + parts[8] + "\n");
                outputFileWriter.write("Iterator Range Size New: " + parts[9] + "\n");
                outputFileWriter.write("Iterator Id: " + parts[10] + "\n");
                outputFileWriter.write("List Range Size: " + parts[11] + "\n");
                outputFileWriter.write("List Range Size New: " + parts[12] + "\n");
                outputFileWriter.write("VO Usage Fetch Size: " + parts[13] + "\n");
                outputFileWriter.write("VO Usage Fetch Size New: " + parts[14] + "\n");
                outputFileWriter.write("VO Usage Default Size: " + parts[15] + "\n");
                outputFileWriter.write("VO Usage Default Size New: " + parts[16] + "\n");
                outputFileWriter.write("AM PAckage: " + parts[17] + "\n");
                outputFileWriter.write("VO PAckage: " + parts[18] + "\n");
                outputFileWriter.write("VO Instance: " + parts[19] + "\n\n");
            }
        }
        if (hasViolation)
            outputFileWriter.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/Tuningtablesandlovsforbetterperformance\n");

        outputFileWriter.close();
        reader.close();

    }

    public static boolean isVoPackageInListOfTxnFiles(String voPackage) {
        if(jsffFilesInTransaction==null || jsffFilesInTransaction.size()<1)
            return false;
        if(voPackage==null || "".equals(voPackage))
            return false;
        String voFilePackagePart = voPackage.replaceAll("\\.", "/") + ".xml";
        Iterator<String> fileIter = jsffFilesInTransaction.iterator();
        while (fileIter.hasNext()) {
            String txnFile = fileIter.next();
            if (txnFile.contains(voFilePackagePart))
                return true;
        }
        return false;
    }
}
