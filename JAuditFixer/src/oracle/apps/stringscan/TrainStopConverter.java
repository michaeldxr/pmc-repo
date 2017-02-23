package oracle.apps.stringscan;

import java.io.FileNotFoundException;

import java.io.IOException;

import java.sql.SQLException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.stringscan.ConverterBase;

public class TrainStopConverter extends ConverterBase {
    public static final int FILENAME = 0;
    public static final int  DISPLAYNAME= 1;

    public TrainStopConverter(String series) {
        super(series);
        ISSUETYPE = "i18n Train Stop Check";
    }

    public static void main(String[] args) {
        if (args.length != 2 || !args[0].endsWith(".csv")) {
            System.out.println("Usage:  TrainStopConverter <path of csv file> <series>");
            System.exit(1);
        }

        ConverterBase converter = new TrainStopConverter(args[1]);
                
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
        System.out.println("Processing line: " + line);
        String module = "";
        String product = "";
        String family = "";
        String file = null;
        String lineNum = null;
        String displayName = null;
          
        //ignore header lines: File Name,Line #,Source Text
        if(line != null && line.trim().equals("File Name,Line #,Source Text")){
          System.out.println("Ignoring header line..");
          return;
        }
        
        int firstComma = line.indexOf(",");
        
        if(firstComma != -1){
          file = line.substring(0, firstComma).trim();
        }else{
          System.out.println("WARNING: Couldn't find the first delimiter from the csv.");
          return;
        }
        
        int secondComma = line.indexOf(",", firstComma+1);
        if(secondComma != -1){
          lineNum = line.substring(firstComma+1,secondComma).trim();
        }else{
          System.out.println("WARNING: Couldn't find the second delimiter from the csv.");
          return;
        }
        
        displayName = line.substring(secondComma+1, line.length()).trim();
        
        String issueType = ISSUETYPE;
        String subIssue = "Hardcoded Display Name";
        
        String description = "Non-SR Bundle reference found in line #" + lineNum + ": "+ displayName;
        
        try {
            String[] info = FamilyModuleHelper.getFileNameInfo(labelServerTop + "/" + series + ".rdd/" + label + "/" + file).split(",");
            family = info[0];
            module = info[1];
            product = info[2];
        } catch (IOException e) {
            System.out.println("File Not Found in the label");
            return;
        }
        
        ViolationDetails violation = new ViolationDetails(file,series,label,family,module,product,issueType,subIssue,description);
        list.add(violation);
        
    }
}
