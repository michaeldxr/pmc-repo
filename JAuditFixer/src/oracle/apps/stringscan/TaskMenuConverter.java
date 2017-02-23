package oracle.apps.stringscan;

import java.io.FileNotFoundException;

import java.io.IOException;

import java.sql.SQLException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.stringscan.ConverterBase;

public class TaskMenuConverter extends ConverterBase {
    public static final int FILENAME = 0;
    public static final int FAMILY = 2;
    private boolean foundFamily;
    String family;
    String module;
    String product;
    String fileName;
    String issueType;
    String subIssue;

    public TaskMenuConverter(String series) {
        super(series);
        ISSUETYPE="i18n Task Menu Check";
        issueType = ISSUETYPE;
        subIssue = "Should use SR bundle header refernece";
    }

    public static void main(String[] args) {
        if (args.length != 2 || !args[0].endsWith(".log")) {
            System.out.println("Usage:  TaskMenuConverter <path of log file> <series>");
            System.exit(1);
        }

        ConverterBase converter = new TaskMenuConverter(args[1]);
        try {
            converter.readFile(args[0]);
            converter.pushToAB(ISSUETYPE);
        } catch (FileNotFoundException e) {
            System.out.println("Cannot find log file, please make sure the right path");
        } catch (SQLException e) {
            System.out.println("Fail to push data to the DB");
        }
    }

    public void processLine(String line) {
        Matcher m;

        System.out.println("Processing line: " + line);
        if (!foundFamily) {
            Pattern TopDirRegex =
                Pattern.compile("Top Directory\\=/ade_autofs/(?:.+)/([^/]+?)/([^/]+?)/fusionapps/([^/]+?)/components");
            m = TopDirRegex.matcher(line);
            if (m.find()) {
                foundFamily = true;
                family = m.group(3).toLowerCase();
                System.out.println("STARTING: " + family + "==============================");
                return;
            }
        } else {
            Pattern filePathRegex =
                Pattern.compile("\\=\\=(.+) contains");
            m = filePathRegex.matcher(line);
            if (m.matches()) {
                fileName =
                        "fusionapps/" + family + "/components/" +
                        m.group(1);
                return;
            } else {
                Pattern lineRegex = Pattern.compile("Line ([0-9]{1,4}?):(.+)");
                m = lineRegex.matcher(line);
                if (m.matches()) {
                    String description =
                        "A violation found in line " + m.group(1) + ": " +
                        m.group(2).trim();
                    
                    try {
                        String[] info = FamilyModuleHelper.getFileNameInfo(labelServerTop + "/" + series + ".rdd/" + label + "/" + fileName).split(",");
                        family = info[0];
                        module = info[1];
                        product = info[2];
                    } catch (IOException e) {
                        System.out.println("File Not Found in the label");
                        return;
                    }
                    
                    ViolationDetails violation =
                        new ViolationDetails(fileName, series, label, family, module,
                                             product, issueType, subIssue,
                                             description);
                    list.add(violation);
                } else {
                    foundFamily = false;
                    System.out.println("ENDING: " + family + "==============================");
                }
            }
        }
    }
}
