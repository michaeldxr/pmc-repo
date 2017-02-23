/* $Header: fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/premerge/PremergeConsolidationCheck.java /main/1 2016/09/23 01:56:08 mengxu Exp $ */

/* Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.*/

/*
   DESCRIPTION
    <short description of component this file declares/defines>

   PRIVATE CLASSES
    <list of private classes defined - with one-line descriptions>

   NOTES
    <other useful comments, qualifications, etc.>

   MODIFIED    (MM/DD/YY)
    mengxu      07/19/16 - Creation
 */

/**
 *  @version $Header: fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/premerge/PremergeConsolidationCheck.java /main/1 2016/09/23 01:56:08 mengxu Exp $
 *  @author  mengxu
 *  @since   release specific (what release of product did this appear in)
 */

package oracle.apps.premerge;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.io.InputStream;

import java.io.PrintWriter;

import java.io.UnsupportedEncodingException;

import java.security.MessageDigest;

import java.security.NoSuchAlgorithmException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import oracle.apps.helpers.FamilyModuleHelper;

public class PremergeConsolidationCheck {

  static String avr;
  static String m_pmcDb =
    "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
  static String m_pmcUName = "fintech";
  static String m_pmcPwd = "fintech";

  public static void main(String[] args) throws SQLException {
    String sql = "";

    if ((args != null) && (args.length >= 5)) {

      Connection con = null;
      Statement stmt = null;
      ResultSet rs = null;

      PrintWriter writerExe;
      PrintWriter writer;
      BufferedReader br;

      // Save parameters
      String consolidateFilelist = args[0];
      avr = args[1];
      String exemConsolidateFilelist = args[2];
      String vioConsolidateFilelist = args[3];
      
      String label = args[4];
      String series = FamilyModuleHelper.getLabelSeries(label)[0];
      System.out.println("series = " + series);
      try {
        writerExe = new PrintWriter(exemConsolidateFilelist, "UTF-8");
        writer = new PrintWriter(vioConsolidateFilelist, "UTF-8");

        br = new BufferedReader(new FileReader(consolidateFilelist));

        // Generate hashcode for files
        Map<String, String> consolidMap = new HashMap<String, String>();
        String line;
        while ((line = br.readLine()) != null) {
          consolidMap.put(line, getFileHashCode((avr + "/" + line), series));
        }

        System.out.println("Check exemptions.....");

        con = DriverManager.getConnection(m_pmcDb, m_pmcUName, m_pmcPwd);
        stmt = con.createStatement();

        Iterator it = consolidMap.entrySet().iterator();
        String fileList = "";
        while (it.hasNext()) {
          Map.Entry pair = (Map.Entry)it.next();
          fileList = fileList + "'" + pair.getKey() + "', ";         
        }
        
        if(!fileList.isEmpty()){
          fileList = fileList.substring(0, (fileList.length() -2));
            
          sql =
              "SELECT FILEPATH, HASHCODE from SMC_EXEMPTIONS where FILEPATH in (" + fileList + ")";
          rs = stmt.executeQuery(sql);
          
          while(rs.next()){
            String filepath = rs.getString("FILEPATH");
            String hashcode = rs.getString("HASHCODE");

            String curHashcode = consolidMap.get(filepath);
            if(hashcode.equals(curHashcode)){ //Exempted
              writerExe.println(filepath);
              consolidMap.remove(filepath);
            }
          }
          
          it = consolidMap.entrySet().iterator();
          while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            writer.println(pair.getKey() + ", hashcode= " + pair.getValue() + ".");
          }
          
        }
        
        br.close();
        writer.close();
        writerExe.close();

      } catch (FileNotFoundException e) {
        e.printStackTrace();
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        try {
          if (rs != null) {
            rs.close();
          }
          if (stmt != null) {
            stmt.close();
          }
          if (con != null) {
            con.close();
          }
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }


    } else {
      System.out.println("No valid parameter.");
    }
  }


  /**
   * Generate hash code based on the file content and the branch name.
   * @param fileDir
   * @param series
   * @return
   */
  private static String getFileHashCode(String fileDir, String series) {
    String result = "";
    InputStream fis = null;
    try {
      fis = new FileInputStream(fileDir);
      byte[] buffer = new byte[1024];
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      for (int numRead = 0; (numRead = fis.read(buffer)) > 0; ) {
        md5.update(buffer, 0, numRead);
      }

      byte[] byteHashCode = md5.digest();
      StringBuilder sb = new StringBuilder();
      for (byte b : byteHashCode) {
        sb.append(String.format("%02X", b));
      }
      result = sb.toString();

      int branchHc = series.hashCode();
      result = result + String.format("%02X", branchHc);

      return result;

    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    } finally {
      try {
        if (fis != null) {
          fis.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return result;
  }


}
