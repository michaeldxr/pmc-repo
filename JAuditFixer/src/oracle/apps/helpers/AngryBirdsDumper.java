package oracle.apps.helpers;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import oracle.apps.utility.LRGUtil;

public class AngryBirdsDumper {
    public static String exemptionsBaseFolder = FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") + "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/exemptions/";
    File _csvFile = null;
    private String angryBirdsDbConnectString = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
    private String angryBirdsDbUName = "codescan";
    private String angryBirdsDbPwd = "codescan";
    boolean bDebug = true;
    boolean csvHasHeader = false;
    public AngryBirdsDumper(String csvFilePath, boolean csvHasHeader) throws Exception {
        super();

        if (isEmpty(csvFilePath))
            throw new Exception("Missing Mandatory Argument: csvFilePath");

        _csvFile = new File(csvFilePath);
        if (_csvFile == null || !_csvFile.exists())
            throw new Exception("File does not exist: " + csvFilePath);
        this.csvHasHeader=csvHasHeader;
    }
    
    public boolean dumpCsvToAngryBirdsDB(String series, String issueType, boolean prototype) throws Exception {
       
        Connection connection = getNewConnection();
        if (connection == null)
            throw new Exception("Could not obtain db connection for host: " + angryBirdsDbConnectString);

        int numberOfRowsDeleted = deleteScanResultsForFamily(connection, series, issueType,prototype);
        System.out.println("Number of rows deleted: " + numberOfRowsDeleted);

        PreparedStatement insertStmt = getInsertStatement(connection,prototype);

        BufferedReader fileReader = new BufferedReader(new FileReader(_csvFile));
        boolean exitSuccess = true;
        int numberOfInserts = 0;
        try {
            String line = "";
            int cnt = 0;
            String[] labelSeries = null;
            String prevLabel ="";
            while ((line = fileReader.readLine()) != null) {
                cnt++;
                if(csvHasHeader && cnt==1)
                    continue;
                String[] parts = line.split(",");
                if (parts.length < 8) {
                    System.out.println("Invalid Format.. Skipping .. : " + line);
                    continue;
                }
                
             //filter out QOC from rel9 and prior...
              if (LRGUtil.isDeferred(connection, series, parts[3].trim()))
                 continue;
                
                if(prevLabel != parts[4].trim()) {
                    labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
                    prevLabel = parts[4].trim();
                }      
                
                String issue = parts[5].trim();
                
                    insertStmt.setString(1, parts[0].trim()); //family
                    insertStmt.setString(2, parts[1].trim()); //module
                    insertStmt.setString(3, parts[2].trim()); //product
                    insertStmt.setString(4, parts[3].trim()); //filename
                    insertStmt.setString(5, labelSeries[0]); //series
                    insertStmt.setString(6, labelSeries[1]); //label
                    insertStmt.setString(7, issue); //issue
                    insertStmt.setString(8, parts[6].trim()); //subissue
                    insertStmt.setString(9, parts[7].trim()); // description

                    insertStmt.addBatch();
                    //System.out.println("Insert");
               // }                
                
                numberOfInserts++;
            }
            insertStmt.executeBatch();
            fileReader.close();
            connection.commit();
            System.out.println("Number of inserts:" + numberOfInserts);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (connection != null) 
                connection.rollback();
            exitSuccess = false;
        } finally {
            if (insertStmt != null) insertStmt.close();
            if(connection != null) connection.close();
        }
        return exitSuccess;
    }


    
    public PreparedStatement getInsertStatement(Connection connection, boolean prototype) throws Exception {
        String tableName = "CODESCAN_RESULTS";
        if(prototype)
            tableName = "CODESCAN_RESULTS_P";
        String sql =
            "INSERT INTO " + tableName + "(FAMILY,MODULE,PRODUCT,FILENAME,SERIES,LABEL,ISSUETYPE,SUB_ISSUE,DESCRIPTION) VALUES (?,?,?,?,?,?,?,?,?)";
        return connection.prepareStatement(sql);
    }
    
    public int deleteScanResultsForFamily(Connection dbConnection, String series,
                                          String issueTypeToDelete, boolean prototype) throws Exception {

        String tableName = "CODESCAN_RESULTS";
        if(prototype)
            tableName = "CODESCAN_RESULTS_P";
        PreparedStatement stmt = null;
        String sql = "DELETE FROM " + tableName + " WHERE ISSUETYPE=? and SERIES=?"; 
      
        stmt = dbConnection.prepareStatement(sql);
        stmt.setString(1, issueTypeToDelete);
        stmt.setString(2, series);
      
        int numberOfRowsDeleted = stmt.executeUpdate();
        stmt.close();
        if (bDebug)
            System.out.println("[DONE] deleteScanResultsForFamily: Number of rows deleted: " + numberOfRowsDeleted);
        return numberOfRowsDeleted;

    }

    public Connection getNewConnection() throws Exception {
        Connection connection = DriverManager.getConnection(angryBirdsDbConnectString, angryBirdsDbUName, angryBirdsDbPwd);
        connection.setAutoCommit(false);
        return connection;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }
    
    public static String getExemptionFlie(String exemptionFileName)
    {
        if(isEmpty(exemptionFileName))
            return "";
       return exemptionsBaseFolder+"/"+exemptionFileName;
    }
}
