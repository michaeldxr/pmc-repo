package oracle.apps.lovWidth;

import java.io.File;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import oracle.apps.helpers.JprHelper;

/**
 * ModelFileMapperDump -> This class when run, checks for all AM's, VO's, EO's and PropertySets.
 * When it finds any of the above, it records the BC Package and the ade relative path in the database.
 */
public class ModelFileMapperDump {
    private static String m_jdbc = "jdbc:oracle:thin:@//rws65979fwks:1522/rws65979";
    private static String m_userName = "fusion";
    private static String m_password = "fusion";
    /*
  create table ZEESHA_MODELMAPPER(
  bc_package varchar2(2000) primary key,
  bc_filepath varchar2(2000)
  );

  create table COMBOWIDTH_PROD(
  family varchar2(100),
  product varchar2(100),
  bcname varchar2(400),
  bcpackage varchar2(2000),
  bcattribute varchar2(200),
  bcattributeWidth varchar2(50),
  lovtype varchar2(20) --expert, eobased,vooverride
  );

  create table combowidth_consumer(
  family varchar2(100),
  product varchar2(100),
  uiPageName varchar2(100),
  uiPagePath varchar2(100),
  lovUses varchar2(400)
  );

   */
    private String tableName = "ZEESHA_MODELMAPPER";
    private Connection m_connection;

    public ModelFileMapperDump() {
        super();
    }

    public static boolean canDumpToDb() {
        // dont want to close conneciton because you want to use the same connection everwhere..
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(m_jdbc, m_userName, m_password);
            PreparedStatement stmt1 = connection.prepareStatement("select 1 from " + "ZEESHA_MODELMAPPER");
            stmt1.execute();
            stmt1.close();

            PreparedStatement stmt2 = connection.prepareStatement("select 1 from " + "COMBOWIDTH_PROD");
            stmt2.execute();
            stmt2.close();

            PreparedStatement stmt3 = connection.prepareStatement("select 1 from " + "combowidth_consumer");
            stmt3.execute();
            stmt3.close();

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        return true;
    }

    public static void main(String[] args) {
        ModelFileMapperDump modelFileMapperDump = new ModelFileMapperDump();
        modelFileMapperDump.isDbInfoValid();

        if (args == null || args.length != 1 || isEmpty(args[0])) {
            System.out.println("missing args");
            System.exit(1);
        }
        String crawlDir = args[0];
        System.out.println("Analyzing dir " + crawlDir);
        modelFileMapperDump.modelCrawler(crawlDir);

    }

    private void modelCrawler(String path) {
        if (path.contains(".ade_path"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (absFilePath.endsWith(".jpr") && !absFilePath.contains("/classes/") && !absFilePath.contains("/noship/") &&
                    !absFilePath.contains("dbSchema.jpr") && !absFilePath.contains("Test.jpr")) {
                    //if (JprHelper.isModelProject(absFilePath)) {
                    analyseJprFile(listOfFiles[i]);
                    //}

                }
            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                if (isValidModelDirectory(dir)) {
                    modelCrawler(dir);
                }
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }

    public boolean isValidModelDirectory(String absDirPath) {
        if (!absDirPath.contains("/dbSchema/") && !absDirPath.contains("/classes/") && !absDirPath.contains("/test/")) {
            return true;
        }
        return false;
    }

    /**
     * Process each valid ui file. If the file has any found violations, fix them and increment the violation count
     * @param file
     * @param bDoADE
     */
    public void analyseJprFile(File file) {
        String jprPath = file.getAbsolutePath();

        System.out.println("Processing file: " + jprPath);

        // get all AM
        processAllApplicationModules(jprPath);
        // get All VO
        processAllVOs(jprPath);
        // get all EO
        processAllEOs((jprPath));
        // all property sets
        processAllPropertySets(jprPath);

    }

    public void processAllApplicationModules(String jprPath) {
        JprHelper jprHelper = new JprHelper();
        HashMap<String, String> aMsInJpr = jprHelper.getListOfAllAMsInJpr(jprPath);
        Set<String> keySet = aMsInJpr.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String amPackage = keyIter.next();
            String amFilePath = aMsInJpr.get(amPackage);
            dumpModelInDb(amPackage, getAdeRelativePath(amFilePath));
        }
    }

    /**
     * Format the absolute file path by the occurrence of /fusionapps/
     * e.g. /scratch/zcurrimb/view_storage/zcurrimb_m6b_latest/fusionapps/fin/components/abc.txt
     * will become
     * /fusionapps/fin/components/abc.txt
     * @param absFilePath
     * Absolute file path
     * @return
     * formatted file path
     */
    private String getAdeRelativePath(String absFilePath) {
        if (isEmpty(absFilePath)) {
            return absFilePath;
        }
        if (absFilePath.contains("/fusionapps/")) {
            int fusionappsInd = absFilePath.indexOf("/fusionapps/");
            return absFilePath.substring(fusionappsInd + 1);
        }
        return absFilePath;
    }

    public void processAllVOs(String jprPath) {
        JprHelper jprHelper = new JprHelper();
        HashMap<String, String> vosInJpr = jprHelper.getListOfAllViewObjectsInJpr(jprPath);
        Set<String> keySet = vosInJpr.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String voPackage = keyIter.next();
            String voPath = vosInJpr.get(voPackage);
            dumpModelInDb(voPackage, getAdeRelativePath(voPath));
        }
    }

    public void processAllEOs(String jprPath) {
        JprHelper jprHelper = new JprHelper();
        HashMap<String, String> eosinJpr = jprHelper.getListOfAllEntityObjectsInJpr(jprPath);
        Set<String> keySet = eosinJpr.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String eoPackage = keyIter.next();
            String eoPath = eosinJpr.get(eoPackage);
            dumpModelInDb(eoPackage, getAdeRelativePath(eoPath));
        }

    }


    public void processAllPropertySets(String jprPath) {
        JprHelper jprHelper = new JprHelper();
        HashMap<String, String> psInJpr = jprHelper.getListOfAllPropertySetsInJpr(jprPath);
        Set<String> keySet = psInJpr.keySet();
        Iterator<String> keyIter = keySet.iterator();
        while (keyIter.hasNext()) {
            String eoPackage = keyIter.next();
            String eoPath = psInJpr.get(eoPackage);
            dumpModelInDb(eoPackage, getAdeRelativePath(eoPath));
        }

    }


    public Connection getConnection() {
        return m_connection;
    }

    public void dumpModelInDb(String packageName, String path) {
        if (isEmpty(packageName) || isEmpty(path))
            return;
        try {
            Connection connection = getConnection();
            String existence = String.format("select 1 from %s where bc_package=?", tableName);
            PreparedStatement existenceStmt = connection.prepareStatement(existence);
            existenceStmt.setString(1, packageName.trim());
            ResultSet result = existenceStmt.executeQuery();
            boolean exists = false;
            if (result != null && result.next()) {
                exists = true;
            }
            result.close();
            existenceStmt.close();
            if (!exists) {
                String insertStmt = String.format("insert into %s values(?,?)", tableName);
                PreparedStatement insStmt = connection.prepareStatement(insertStmt);
                insStmt.setString(1, packageName);
                insStmt.setString(2, path);
                insStmt.executeUpdate();
                insStmt.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    public boolean isDbInfoValid() {

        // dont want to close conneciton because you want to use the same connection everwhere..
        try {
            m_connection = DriverManager.getConnection(m_jdbc, m_userName, m_password);
            PreparedStatement prepareStatement = m_connection.prepareStatement("select 1 from " + tableName);
            prepareStatement.execute();
            prepareStatement.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        return true;
    }
}
