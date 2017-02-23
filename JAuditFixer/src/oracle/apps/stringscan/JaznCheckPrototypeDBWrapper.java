package oracle.apps.stringscan;

import au.com.bytecode.opencsv.CSVReader;

import java.io.File;
import java.io.FileNotFoundException;

import java.io.FileReader;

import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.PreparedStatement;

import java.sql.SQLException;

import java.util.Scanner;

import oracle.apps.helpers.FamilyModuleHelper;

public class JaznCheckPrototypeDBWrapper {
    public JaznCheckPrototypeDBWrapper() {
        super();
    }
    
    public static Connection con = null;
    public static PreparedStatement insert = null;
    public static PreparedStatement delete = null;
    public static String prefix = null;
    
    public static void main(String[] args) {
        
        if(args.length != 2){
          System.out.println("USAGE: runJaznChk.sh <series> <abs_output_dir>");
          System.exit(1);
        }
        
        if(args[0] == null || args[0].isEmpty()){
          System.out.println("USAGE: runJaznChk.sh <series> <abs_output_dir>");
          System.out.println("Please specify the series you want to scan on.");
          System.exit(1);
        }
        String series = args[0]; 
        
        if(args[1] == null || args[1].isEmpty()){
          System.out.println("USAGE: runJaznChk.sh <series> <abs_output_dir>");
          System.out.println("Please specify the output directory.");
          System.exit(1);
        }
        String outputDir = args[1];
        
//        String series = "FUSIONAPPS_11.1.1.5.1_LINUX.X64";
//        String outputDir = "/scratch/yaoyao/logs/";
        
        prefix = FamilyModuleHelper.getLabelServerTop1(series) + "/"+series+".rdd/LATEST/";
        try {
          con =
              DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
          con.setAutoCommit(false);

          insert = con.prepareStatement("INSERT INTO CODESCAN_RESULTS_P (FAMILY, MODULE, PRODUCT, FILENAME, ISSUETYPE, SUB_ISSUE, DESCRIPTION, SERIES,LABEL) VALUES (?,?,?,?,?,?,?,?,?)");
          delete = con.prepareStatement("DELETE FROM CODESCAN_RESULTS_P WHERE ISSUETYPE = ? AND SERIES = ?");
          
          delete.setString(1, "jaznCheck");
          delete.setString(2, series);
          
          delete.executeUpdate();
        }catch(Exception e){
          e.printStackTrace();
          System.out.println("Failed to create JDBC c");
          System.exit(1);
        }
        
        try {
          System.out.println("Parsing report csv: " + outputDir +"jaznRpt_"+series+".csv...");
          scanCSV(outputDir+"jaznRpt_"+series+".csv");

          System.out.println("inserting the data...");
          insert.executeBatch();
          con.commit();
        } catch (SQLException e) {
          e.printStackTrace();
          System.out.println("Failed to commit");
          try {
            con.rollback();
          } catch (SQLException f) {
            System.out.println("Failed to rollback");
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
    }
    
    public static void scanCSV(String csvPath) throws Exception {
      Scanner s;
      String[] cols;
      try {
          CSVReader reader = new CSVReader(new FileReader(csvPath));
        
          String [] nextLine;
          
          if((nextLine = reader.readNext()) == null)
              return;
          
          while ((nextLine = reader.readNext()) != null) {
              insert(nextLine);
          }
          
      } catch (FileNotFoundException e) {
        System.out.println("Report jaznRpt.csv not found");
      }
    }
    
    private static void insert(String[] cols) throws Exception {
      String fileNameInfo = FamilyModuleHelper.getFileNameInfo1(prefix + cols[0]);
      String[] parts = fileNameInfo.split(",");
      
      if("".equals(parts[4].trim()))
        return;

      insert.setString(1, parts[0].trim());//family
      insert.setString(2, parts[1].trim());//module
      insert.setString(3, parts[2].trim());//product
      insert.setString(4, parts[3].trim());//filename
      insert.setString(5, "jaznCheck");//issue
      insert.setString(6, cols[1]);//sub-issue
      insert.setString(7, cols[2]);//description
      insert.setString(8, parts[4].trim()); //series
      insert.setString(9, parts[5].trim());//label
      insert.addBatch();
    }
    
}
