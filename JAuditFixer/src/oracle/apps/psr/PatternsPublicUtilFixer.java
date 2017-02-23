package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.utility.JoesBaseClass;


public class PatternsPublicUtilFixer extends JoesBaseClass {
    private int m_reviewCount = 0;
    private int m_fixCount = 0;
    private int m_invokeCount = 0;
    private int m_manualFixNeededCount = 0;
    private List<String> badFilePaths = new ArrayList<String>();

    public PatternsPublicUtilFixer(JoesBaseClass.CRAWL_TYPE type) {
        super(type);
    }

    protected String getSummaryReportSubject() {
        return "SCRIPT: Migrate PatternsPublicUtil.invokePopup(String)";
    }

    protected void processFile(File fName, boolean bDoADE) {
        m_reviewCount++;
        processJavaFile(fName, bDoADE);
    }

    protected String getSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("***************************************************\n");
        sb.append("Number of java files seen:     " + m_reviewCount + "\n");
        sb.append("Number of java files fixed:    " + m_fixCount + "\n");
        sb.append("---- List of files that require further review ----\n");
        for (String badFilePath : badFilePaths) {
            sb.append("--  " + badFilePath + "\n");
        }
        sb.append("***************************************************\n");

        return sb.toString();
    }


    private void processJavaFile(File f, boolean bDoADE) {
        String fileName = f.getName();
        String path = f.getAbsolutePath();

        // skip some of them.
        if (path.contains("/model/") || path.contains("/test/") || path.contains("/uiTest/") || path.contains("/modelTest/"))
            return;

        System.out.println("Checking Java File:  " + f.getAbsolutePath());
        boolean fixed = false;

        try {
            StringBuffer fileData = new StringBuffer(1000);
            BufferedReader reader = new BufferedReader(new FileReader(f));

            char[] buf = new char[1024];
            int numRead = 0;
            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(readData);
                buf = new char[1024];
            }
            reader.close();
            String str = fileData.toString();
            String origData = fileData.toString();

            str = replaceComments(str);


            String invokeRegEx = "(PatternsPublicUtil.invokePopup" + whiteSpace + "\\()(" + whiteSpace + "[^,;]*" + whiteSpace + ")(\\)" + whiteSpace + ";)";
            Pattern invokePattern = Pattern.compile(invokeRegEx);
            Matcher m = invokePattern.matcher(str);

            while (m.find()) {
                int start = m.start(0);
                int offset = start - str.length() + origData.length();
                if (offset < 0)
                    offset = 0;

                String invokeString = m.group(0);
                String prefix = m.group(1);
                String innerStuff = m.group(2);
                String suffix = m.group(3);

                if (innerStuff.contains(",")) {
                    System.out.println("ERROR:  Either already fixed, or type 2 issue.");
                    System.out.println("Forcing exit since this is TBD");
                    System.out.println("File name-->" + fileName);
                    System.exit(-1);
                }
                else {
                    String newInvokeString = "";

                    // Need to check if the parameter is a string, or is a call to get the string.
                    if (innerStuff.contains("getClientId")) {
                        String paramRegEx = "(" + identifier + whiteSpace + ".)?" + whiteSpace + "(" + identifier + ")" + "(\\(" + whiteSpace + "\\))?" + whiteSpace + "\\.getClientId";
                        Pattern paramPattern = Pattern.compile(paramRegEx,
                                                               Pattern.MULTILINE);
                        Matcher m2 = paramPattern.matcher(innerStuff);
                        if (m2.find()) {
                            String method = m2.group(2);
                            String parens = m2.group(3);

                            String obj = m2.group(1);
                            if (obj != null && "".equals(obj) == false && obj.contains("this") == false) {
                                method = obj + method;
                            }
                            if (parens == null)
                                newInvokeString = prefix + method + ", null" + suffix;
                            else
                                newInvokeString = prefix + method + "(), null" + suffix;
                        }
                        else {
                            System.out.println("*****************************************************************************************************");
                            System.out.println("ERROR:  Could not migrate this case.  Please review this file and handle this manually.");
                            System.out.println(f.getAbsolutePath());
                            System.out.println("*****************************************************************************************************");
                            badFilePaths.add(f.getAbsolutePath());
                            m_manualFixNeededCount++;
                            continue;
                        }

                    }
                    else {
                        // Need to handle these cases.
                        //PatternsPublicUtil.invokePopup(clientId);
                        int invokeLocation = origData.indexOf(invokeString,
                                                              offset);

                        String paramDeclRegEx = "\\s*?(String)?\\s+?" + innerStuff + "\\s*?=\\s*?\\(?(\\w+\\.)?\\s*?((?:\\w+)?\\w+(\\(\\s*?\\))?)\\)?\\s*?\\.getClient[^;]*;";
                        Pattern paramPattern = Pattern.compile(paramDeclRegEx);
                        Matcher m2 = paramPattern.matcher(origData);

                        //There may be multiple decls that match.  need to find the one closest to the fix.
                        int bestMatch = -1;

                        while (m2.find()) {
                            int s = m2.start();
                            if (s > invokeLocation)
                                break;
                            bestMatch = m2.start();
                        }

                        if (bestMatch == -1) {
                            System.out.println("*****************************************************************************************************");
                            System.out.println("ERROR:  Could not migrate this case.  Please review this file and handle this manually.");
                            System.out.println(f.getAbsolutePath());
                            System.out.println("*****************************************************************************************************");
                            badFilePaths.add(f.getAbsolutePath());
                            m_manualFixNeededCount++;
                            continue;
                        }

                        if (m2.find(bestMatch)) {
                            String varDecl = m2.group(0);

                            int index1 = origData.lastIndexOf(varDecl,
                                                              invokeLocation);
                            int index2 = index1 + varDecl.length();
                            if (index1 == -1) {
                                System.out.println("woops");
                            }
                            origData = origData.substring(0,
                                                          index1) + origData.substring(index2);

                            String method = m2.group(3);
                            if (m2.group(2) != null && !m2.group(2).equals("this.") && !m2.group(2).isEmpty()) {
                                method = m2.group(2) + method;
                            }
                            newInvokeString = prefix + method + ", null" + suffix;
                        }
                    }
                    
                    int index1 = origData.lastIndexOf(invokeString, offset);
                    int index2 = index1 + invokeString.length();

                    origData = origData.substring(0,
                                                  index1) + newInvokeString + origData.substring(index2);
                    fixed = true;
                }
                m_invokeCount++;
            }

            if (fixed == false)
                return;

            if (bDoADE == false) {
                System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$");
                System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$");
                System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$");
                return;
            }

            checkoutAndDelete(f);

            FileWriter fw = new FileWriter(f.getAbsolutePath());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(origData);
            bw.close();

            m_fixCount++;
            checkinAndDiff(f);
        }
        catch (Exception e) {
            e.printStackTrace();
            logException(fileName, e);
            System.out.println("*****************************************************************************************************");
            System.out.println("ERROR:  Error while processing file.  review carefully if the script updated it.");
            System.out.println(f.getAbsolutePath());
            System.out.println("*****************************************************************************************************");
            badFilePaths.add(f.getAbsolutePath());
            return;
        }

    }

    public static void main(String[] args) {
        if (args.length != 2 || args[0] == null || args[1] == null || args[0].equals("") || args[1].equals("")) {
            System.out.println("Usage:  patternsPublicUtilFixer.sh <path to your code in ade> <do ade>");
            System.exit(1);
        }

        String sCrawlDir = args[0];
        Boolean bDoADE = new Boolean(args[1]);

        PatternsPublicUtilFixer x = new PatternsPublicUtilFixer(JoesBaseClass.CRAWL_TYPE.JAVA);

        try {

            if (bDoADE.booleanValue())
                x.startDiffLog();

            x.crawlDirectory(sCrawlDir, bDoADE);
        }
        catch (Exception e) {
            e.printStackTrace();
            x.logException(sCrawlDir, e);
        }

        x.logUsageReport(sCrawlDir);
    }
}
