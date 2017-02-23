package oracle.apps.ppr;

import java.io.IOException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import java.util.ArrayList;
import java.util.Iterator;

import oracle.apps.helpers.FamilyModuleHelper;


public class DBWrapper {

  public DBWrapper() {
    super();
  }

  public static void uploadToDB (ArrayList<ScanResult> al_results, String series) throws SQLException {
    int count = 0;
    ScanResult result;
    Connection con = null;
    PreparedStatement insert = null;
    PreparedStatement delete_master = null;
    String fileNameInfo;
    
    if(al_results == null || al_results.isEmpty())
      return;
    
    try {
      con =
          DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
      con.setAutoCommit(false);
      
      insert =
          con.prepareStatement("INSERT INTO Exemptions (series, page,id,description) VALUES (?,?,?,?)");
      delete_master =
          con.prepareStatement("DELETE FROM Exemptions WHERE series = ?" );
      delete_master.setString(1, series);

      delete_master.execute();
      delete_master.close();

      Iterator<ScanResult> iter = al_results.iterator();

      while (iter.hasNext()) {
        result = iter.next();
        
        if(!result.isHasValidation())
          continue;

        insert.setString(1, series);
        insert.setString(2, result.getPage());
        insert.setString(3, result.getTableId());
        insert.setString(4, "Attribute Level Validation");

        insert.addBatch();
      }
      
      insert.executeBatch();
      con.commit();
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("Failed to insert result into Angrybirds Database.");
      con.rollback();
    }finally{
      insert.close();
      con.close();
    }
  }
}
