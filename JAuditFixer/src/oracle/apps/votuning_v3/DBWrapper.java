package oracle.apps.votuning_v3;

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

  private final int batchSize = 100;

  public DBWrapper() {
    super();
  }

  public static void uploadToDB (ArrayList<ScanResult> al_results, String series) throws SQLException {
    int count = 0;
    ScanResult result;
    Connection con = null;
    CallableStatement insert_master = null;
    PreparedStatement insert_detail = null;
    PreparedStatement delete_master = null;
    String fileNameInfo;
    int seq1;
    
    
    try {
      con =
          DriverManager.getConnection("jdbc:oracle:thin:codescan/codescan@angrybirds.oracle.com:1521:codescan");
      con.setAutoCommit(false);

      insert_master = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
      insert_master.registerOutParameter(1, Types.INTEGER);
      
      insert_detail =
          con.prepareStatement("INSERT INTO VOTUNING (SEQ_ID,COMPONENT,COMPONENT_ID,AUTOHEIGHT_ROWS,AUTOHGT_NEW,ITERATOR_RANGE_SIZE,IRS_NEW,ITERATOR_NAME,LIST_RANGE_SIZE,LRS_NEW,VO_USAGE_FETCH_SIZE,VO_USAGE_FETCH_SIZE_NEW,VO_DEFAULT_FETCH_SIZE,VO_DEFAULT_FETCH_SIZE_NEW,PageDef,AMPACKAGE,VOPACKAGE,VOINSTANCE_NAME) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
      delete_master =
          con.prepareStatement("DELETE FROM CODESCAN_RESULTS WHERE ISSUETYPE = 'TableAndLOVTuning_v2' and series = ?" );
      delete_master.setString(1, series);

      delete_master.execute();
      delete_master.close();

      Iterator<ScanResult> iter = al_results.iterator();

      while (iter.hasNext()) {
        result = iter.next();

        fileNameInfo = FamilyModuleHelper.getFileNameInfo1(result.getPage());
        String[] parts = fileNameInfo.split(",");
        insert_master.setString(2, parts[0].trim());//family
        insert_master.setString(3, parts[1].trim());//module
        insert_master.setString(4, parts[2].trim());//product
        insert_master.setString(5, parts[3].trim());//filename
        insert_master.setString(6, "TableAndLOVTuning_v2");//issue
        insert_master.setString(7, result.getTableId());//sub-issue, for uniqueness
        insert_master.setString(8, result.getDescription());//description
        insert_master.setString(9, series); //series
        insert_master.setString(10, parts[5].trim());//label
        insert_master.execute();
        
        seq1 = insert_master.getInt(1);
        insert_detail.setInt(1, seq1);//seq num
        insert_detail.setString(2,result.getType()); //component name
        insert_detail.setString(3,result.getTableId()); //component id
        insert_detail.setString(4,result.getAutoHeightRows()); //autoheight rows
        insert_detail.setString(5,result.getNew_autoHeight()); //autoHeight new
        insert_detail.setString(6,result.getIterRangeSize() == null ? "Not Set" : result.getIterRangeSize() ); //IRS     
        insert_detail.setString(7,result.getNew_iterRangeSize() == null ? "" : result.getNew_iterRangeSize().toString()); //IRS new
        insert_detail.setString(8,result.getIterator()); //Iterator Name
        insert_detail.setString(9,result.getListBindingName() == null ? "Not Set" : result.getListRangeSize()); //LRS
        insert_detail.setString(10,result.getNew_ListRangeSize() == null ? "" :result.getNew_ListRangeSize().toString()); //LRS New
        if (result.getType().equals("table") || result.getType().equals("treeTable"))
          insert_detail.setString(11,result.getVOUsageFetchSize() == null ? "Not Set" : result.getVOUsageFetchSize().toString()); //VO Usage Fetch Size
        else
          insert_detail.setString(11,result.getVa_fetchSize() == null ? "Not Set" : result.getVa_fetchSize()); //VO Usage Fetch Size
        insert_detail.setString(12,result.getNew_VUFetchSize() == null ? "" : result.getNew_VUFetchSize().toString()); //VO Usage Fetch Size new
        insert_detail.setString(13,result.getVODefaultFetchSize() == null ? "Not Set" :result.getVODefaultFetchSize().toString()); //Vo Default Fetch Size
        insert_detail.setString(14,result.getNew_VODefaultFetchSize() == null ? "" : result.getNew_VODefaultFetchSize().toString()); //Vo Default Fetch Size New
        if(result.getPageDef().indexOf("fusionapps/") != -1)
          insert_detail.setString(15,result.getPageDef().substring(result.getPageDef().indexOf("fusionapps/")));  //pagedef
        else if(result.getPageDef().indexOf("fsm/") != -1)
          insert_detail.setString(15,result.getPageDef().substring(result.getPageDef().indexOf("fsm/")));
        else
          insert_detail.setString(15, result.getPageDef());
        if(result.getAM().indexOf("fusionapps/") != -1)
          insert_detail.setString(16,result.getAM().substring(result.getAM().indexOf("fusionapps/")));  //am package
        else if(result.getAM().indexOf("fsm/") != -1)
          insert_detail.setString(16,result.getAM().substring(result.getAM().indexOf("fsm/")));
        else
          insert_detail.setString(16, result.getAM());
        if(result.getVO().indexOf("fusionapps/") != -1)
          insert_detail.setString(17,result.getVO().substring(result.getVO().indexOf("fusionapps/"))); //vo package
        else if(result.getVO().indexOf("fsm/") != -1)
          insert_detail.setString(17,result.getVO().substring(result.getVO().indexOf("fsm/")));
        else
          insert_detail.setString(17,result.getVO());
        insert_detail.setString(18,result.getBinds()); //vo instance
        insert_detail.execute();
      }
      
      con.commit();
    } catch (IOException e) {
      System.out.println("Failed to extract series and label info from jsff path");
      con.rollback();
    } catch (SQLException e) {
      e.printStackTrace();
      System.out.println("Failed to insert result into Angrybirds Database.");
      con.rollback();
    }finally{
      insert_master.close();
      insert_detail.close();
      con.close();
    }
  }
}
