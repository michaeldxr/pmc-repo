package oracle.apps.stringscan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;


public abstract class ConverterBase {
    ArrayList<ViolationDetails> list;
    String series;
    public static  String ISSUETYPE;
    private static final String dbUser = "codescan";
    private static final String dbPassword = "codescan";
    private static final String dbSid = "codescan";
    private static final String dbHost = "angrybirds.oracle.com";
    private static final String dbPort = "1521";
    private static final String JDBCSTRING =
        "jdbc:oracle:thin:" + dbUser + "/" + dbPassword + "@" + dbHost + ":" +
        dbPort + ":" + dbSid;
    public static String labelServerTop;
    public static String label;

    ConverterBase(String series) {
        list = new ArrayList<ViolationDetails>();
        this.series = series;
        label = LRGUtil.getLatestLabel(series);
        labelServerTop = FamilyModuleHelper.getLabelServerTop1(series);        
    }

    public void readFile(String path) throws FileNotFoundException {

        Scanner scanner = new Scanner(new File(path));

        //read the file line by line
        try {
            while (scanner.hasNextLine()) {
                processLine(scanner.nextLine());
            }
        } finally {
            scanner.close();
        }
    }

    public void pushToAB(String issueType) throws SQLException{
        Connection con = null;
        
        generateExemptionList();
        
        try {
            con = DriverManager.getConnection(JDBCSTRING);

            //clean old data
            PreparedStatement delete =
                con.prepareStatement("DELETE FROM codescan_results where issuetype = ? and series = ?");
            delete.setString(1, issueType);
            delete.setString(2, series);
            delete.execute();
            System.out.println("Old data removed");

            //insert new data
            PreparedStatement insert =
                con.prepareStatement("INSERT INTO codescan_results (SERIES,LABEL,FAMILY,MODULE,PRODUCT,FILENAME,ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)");
            for (int i = 0; i < list.size(); i++) {
                if(isExempt(list.get(i).getFileName()) || !isFileOfInterest(list.get(i).getFileName()))
                    continue;
                
                insert.setString(1,list.get(i).getSeries());
                insert.setString(2, list.get(i).getLabel());
                insert.setString(3, list.get(i).getFamily());
                insert.setString(4, list.get(i).getModule());
                insert.setString(5, list.get(i).getProduct());
                insert.setString(6, list.get(i).getFileName());
                insert.setString(7, list.get(i).getIssueType());
                insert.setString(8, list.get(i).getSubIssue());
                insert.setString(9, list.get(i).getDescription());
                System.out.println("Inserting:  " + list.get(i).getFileName());
                insert.execute();
            }
            con.commit();
        } catch(Exception e){
            e.printStackTrace();
        }
        finally {
                con.rollback();
                con.close();
        }
    }
    
    public String getProdShortCode(){
        return null;
    }

    //add violation detail to the arraylist

    public abstract void processLine(String line);
    
    
    public static HashSet<String> exemptions = new HashSet<String>();
    public void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_Yao.txt"));
        
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 3)
            continue;
        String issue = parts[0].trim();
        if(!issue.equals(ISSUETYPE))
            continue;
        String fileName = parts[1].trim();
        String id = parts[2].trim();
          
        exemptions.add(fileName.toLowerCase());
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
    }
    
    private boolean isExempt(String fileName){       
        return exemptions.contains(fileName.toLowerCase());        
    }

    private boolean isFileOfInterest(String filename){
        String lowerCaseName = filename.toLowerCase();
        if (lowerCaseName.contains("/noship/") ||
            lowerCaseName.contains("/test/") ||
            lowerCaseName.contains("servicetest") ||
            lowerCaseName.contains("datasecuritytest") ||
            lowerCaseName.contains("securitypublictest") ||
            lowerCaseName.contains("publicuitest") ||
            lowerCaseName.contains("structuretest") ||
            lowerCaseName.contains("modeltest") ||
            lowerCaseName.contains("uitest") ||
            lowerCaseName.contains("testui") ||
            lowerCaseName.contains("setest") ||
            lowerCaseName.contains("testviewcontroller") ||
            lowerCaseName.contains("flextest") ||
            lowerCaseName.contains("uimodeler-launch-test") ||
            lowerCaseName.contains("publicuibasetest") ||
            lowerCaseName.contains("uipickertest") ||
            lowerCaseName.contains("/dist/") ||
            lowerCaseName.contains("/fsm/dev") ||
            lowerCaseName.contains("fusionapps/crm/components/ordercapture/qoc/") ||
            lowerCaseName.contains("fusionapps/crm/qoc/components/"))
            return false;
        
        return true;
    }
}
