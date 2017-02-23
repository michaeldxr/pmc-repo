package oracle.apps.stringscan;
import java.io.FileNotFoundException;

import java.io.IOException;

import java.sql.SQLException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;


public class EssConverter extends ConverterBase {
    
    public static final int FILENAME = 0;
    public static final int FAMILY = 2;


    public EssConverter(String series) {
        super(series);
        ISSUETYPE = "i18n ESS String Check";
    }

    public static void main(String[] args) {
        if (args.length != 2 || !args[0].endsWith(".csv")) {
            System.out.println("Usage:  EssConverter <path of csv file> <series>");
            System.exit(1);
        }

        ConverterBase converter = new EssConverter(args[1]);
        try {
            converter.readFile(args[0]);
            converter.pushToAB(ISSUETYPE);
        } catch (FileNotFoundException e) {
            System.out.println("Cannot find csv file, please make sure the right path");
        } catch (SQLException e) {
            System.out.println("Fail to push data to the DB");
        }
    }

    public void processLine(String line) {
        System.out.println("Processing line: " + line);
        String module = "";
        String product = "";
        String family = "";
        
        String[] attr = line.split(",");
        String fileName = "fusionapps/"+attr[FAMILY].trim()+"/components/"+ attr[FILENAME];
        
        String issueType = ISSUETYPE;
        String subIssue = "NoNameDesc";

        String description = "Either resource-key or resource-desc is missing.";

        try {
            String[] info = FamilyModuleHelper.getFileNameInfo(labelServerTop + "/" + series + ".rdd/" + label + "/" + fileName).split(",");
            family = info[0];
            module = info[1];
            product = info[2];
        } catch (IOException e) {
            System.out.println("File Not Found in the label");
            return;
        }
        ViolationDetails violation = new ViolationDetails(fileName,series,label,family,module,product,issueType,subIssue,description);
        list.add(violation);
    }
}
