package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.FileReader;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.LRGUtil;


public class VCPerformanceDBWrapper {
  
  String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
  static String statsUname = "codescan";
  static String statsPwd = "codescan";
  static String pwd;
  static String series = "";  
  static int releaseNo = 0;
  
  public static void main(String[] args) {             
   
      if (args.length < 2) {
          System.out.println("Need to specify both 1.directory and 2.series");
          System.exit(1);
      }

      pwd = args[0];
      series = args[1];
//      pwd = "/scratch/sudgupta/view_storage/sudgupta_fatools_aug5_view/fatools/opensource/jauditFixScripts/JAuditFixer/";
//      series = "FSM_MAIN_GENERIC";
      
      String release = FamilyModuleHelper.getRelease(series);
      if(!ViewCriteriaHelper.isEmpty(release)) {
          try{
               releaseNo= Integer.parseInt(release);
              System.out.println("Release #" + releaseNo);
          } catch (Exception e) {
              releaseNo = 0;
          }
      }

      VCPerformanceDBWrapper dbWrapper = new VCPerformanceDBWrapper();

    try {         
        System.out.println("Updating DB....");
        dbWrapper.updateDatabase1();        
        dbWrapper.updateDatabase2();       
    } catch (Exception e) {
        e.printStackTrace();        
    }
  }  
  
  
  public void updateDatabase1() throws Exception {
    
    Connection con = null;
    try{
        String fileName = pwd + "/VC_scan_UI_Performance.csv";
      
        BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
        String line = fileReader.readLine();
        con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
        con.setAutoCommit(false);
        
        CallableStatement cstmt1 = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
        CallableStatement cstmt2 = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT_P(?,?,?,?,?,?,?,?,?) }");
        CallableStatement cstmt = null;

          System.out.println("First delete records from DB...");
          Statement stmt = con.createStatement();
          String sql ="DELETE FROM CODESCAN_RESULTS WHERE (ISSUETYPE = 'BadViewCriteriaItems' or ISSUETYPE " +
                  "= 'SavedSearchBadViewCriteriaItems') and SERIES = '" + series + "'";
           
          String sql1 = "DELETE FROM CODESCAN_RESULTS_P WHERE (ISSUETYPE = 'BadViewCriteriaItems' or ISSUETYPE " +
                  "= 'SavedSearchBadViewCriteriaItems') and SERIES = '" + series + "'";

          if(series.contains("FSM"))
            stmt.executeUpdate(sql1);
          else
            stmt.executeUpdate(sql);
//          sql ="DELETE FROM CODESCAN_RESULTS_P WHERE ISSUETYPE = 'LOVSmartFilterBadViewCriteriaItem' and SERIES = '" + series + "'";
//          stmt.executeUpdate(sql);
          stmt.close();
        
        if(line == null)  {
            con.commit();  
            return;
        }
        
        cstmt1.registerOutParameter(1, Types.INTEGER);
        cstmt2.registerOutParameter(1, Types.INTEGER);
        
        PreparedStatement pstmt1 = con.prepareStatement("INSERT INTO VC_PERFORMANCE1(SEQ_ID, VIEW_NAME, VIEWATTRIBUTE," +
            "VCROW_VCITEMNAME, COLUMN_NAME, TABLE_NAME, IS_VIEW, RESOLVED_TABLE_COLUMN, INDEXED_TABLE,COL_TYPE, COLUMN_INDEX," +
            "COLUMN_POSITION_IN_INDEX, VC_REQUIRED,UI_FILE,MODEL_VALUE,COMPONENT,NUM_ROWS,NUM_BLOCKS) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        PreparedStatement pstmt2 = con.prepareStatement("INSERT INTO VC_PERFORMANCE1_P(SEQ_ID, VIEW_NAME, VIEWATTRIBUTE," +
            "VCROW_VCITEMNAME, COLUMN_NAME, TABLE_NAME, IS_VIEW, RESOLVED_TABLE_COLUMN, INDEXED_TABLE,COL_TYPE, COLUMN_INDEX," +
            "COLUMN_POSITION_IN_INDEX, VC_REQUIRED,UI_FILE,MODEL_VALUE,COMPONENT,NUM_ROWS,NUM_BLOCKS) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        PreparedStatement pstmt = null;
        
        System.out.println("Now inserting values...");
        
        String[] labelSeries = null;
        String prevLabel = "";
        while((line = fileReader.readLine()) != null) {
            
          String[] parts = line.split(",");
          if(parts.length < 25) continue;   
            
            if (LRGUtil.isDeferred(con, series, parts[3].trim()))
                continue;
            
            if(prevLabel != parts[4].trim()) {
                labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
                prevLabel = parts[4].trim();
            }
            String issueType="";
            if(line.contains("savedSearch")){
                 issueType="SavedSearchBadViewCriteriaItems";
                 cstmt= cstmt1;
                 pstmt= pstmt1;
            }
            else if(line.contains("LOVSmartFilterBadViewCriteriaItem")){
                issueType= "LOVSmartFilterBadViewCriteriaItem";  
                cstmt= cstmt2;
                pstmt= pstmt2;
            }
            else {
               issueType= "BadViewCriteriaItems";    
               cstmt= cstmt1;
               pstmt= pstmt1;
            }
          if(series.contains("FSM")){
            cstmt= cstmt2;
            pstmt= pstmt2;
          }
            
          cstmt.setString(2, parts[0].trim()); //family
          cstmt.setString(3, parts[1].trim()); //module
          cstmt.setString(4, parts[2].trim()); //product
          cstmt.setString(5, parts[3].trim()); //filename
          cstmt.setString(6, issueType); //issue
          cstmt.setString(9, labelSeries[0]); //series
          cstmt.setString(10, labelSeries[1]); //label
               
          cstmt.setString(7, "ViewCriteria Search may not use index"); //subissue        
          cstmt.setString(8,parts[21]);//VCR:VCI
          cstmt.execute();
    
          int seq = cstmt.getInt(1);
          
          pstmt.setInt(1,seq); 
          pstmt.setString(2,parts[5]); //viewname
          pstmt.setString(3,parts[6]); //viewattr
          pstmt.setString(4,parts[21]);  //vcrow_vcitem
          pstmt.setString(5,parts[7]); //column name
          pstmt.setString(6,parts[8]); //table name
          pstmt.setString(7,parts[9]); //isView
          pstmt.setString(8,parts[10]); //resolvedTableColumn
          pstmt.setString(9,parts[11]); //indexed_table
          pstmt.setString(10,parts[14]); //col_type
          pstmt.setString(11,parts[15]); //col_index
          pstmt.setString(12,parts[16]); //col_position_in_index
          pstmt.setString(13,parts[17]); //vc_required
          pstmt.setString(14,parts[22]); //ui_file
          pstmt.setString(15,parts[23]); //model value
          pstmt.setString(16,parts[24]); //component
          pstmt.setString(17,parts[12]); //num_rows
          pstmt.setString(18,parts[13]); //num_blocks
    
          pstmt.execute();    
        }
        con.commit();
        cstmt1.close();
        cstmt2.close();
        pstmt1.close();
        pstmt2.close();
    }catch(Exception e) {
        e.printStackTrace();
        if(con != null)
            con.rollback();
    } finally{
        if(con != null)
            con.close();
    }
  }
  
      public void updateDatabase2() throws Exception {
        
        Connection con = null;
        try{
            String fileName = pwd + "/VC_scan_UI_Performance_noVC_rows.csv";            
            BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
            String line = fileReader.readLine();
      
            con = DriverManager.getConnection(statsDB, statsUname, statsPwd);
            con.setAutoCommit(false);
            
            CallableStatement cstmt1 = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT(?,?,?,?,?,?,?,?,?) }");
            CallableStatement cstmt2 = con.prepareCall("{ ? = call INSERT_CODESCAN_RESULT_P(?,?,?,?,?,?,?,?,?) }");
            CallableStatement cstmt = null;
            
              System.out.println("First delete records from DB...");
              Statement stmt = con.createStatement();
              String sql = "DELETE FROM CODESCAN_RESULTS WHERE (ISSUETYPE = 'NoViewCriteria' or " +
                      "ISSUETYPE = 'SavedSearchNoViewCriteria' or ISSUETYPE='NoViewCriteriaLOV') and SERIES = '" + series + "'";
              String sql1 = "DELETE FROM CODESCAN_RESULTS_P WHERE (ISSUETYPE = 'NoViewCriteria' or " +
                  "ISSUETYPE = 'SavedSearchNoViewCriteria' or ISSUETYPE='NoViewCriteriaLOV') and SERIES = '" + series + "'";

              if(series.contains("FSM"))
                stmt.executeUpdate(sql1);   
              else
                stmt.executeUpdate(sql);     
//              sql = "DELETE FROM CODESCAN_RESULTS_P WHERE (ISSUETYPE = 'LOVSmartFilterNoViewCriteria' " +
//                  "or ISSUETYPE = 'LOVSmartFilterNoMRU' or ISSUETYPE='NoViewCriteriaLOV') and SERIES = '" + series + "'";
//              stmt.executeUpdate(sql);   
            
              stmt.close();
      
            
            if(line == null)  {
                con.commit();  
                return;
            }
        
            cstmt1.registerOutParameter(1, Types.INTEGER);
            cstmt2.registerOutParameter(1, Types.INTEGER);
            
            PreparedStatement pstmt1 = con.prepareStatement("INSERT INTO VC_PERFORMANCE2(SEQ_ID, VIEWNAME," +
                "UI_FILE,MODEL_VALUE,COMPONENT,LIST_OF_TABLES,MAX_ROWS,BLOCKS,TABLE_WITH_MAX_ROWS, WHERE_CONTAINS_BINDS,MRU_COUNT) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?)");
            PreparedStatement pstmt2 = con.prepareStatement("INSERT INTO VC_PERFORMANCE2_P(SEQ_ID, VIEWNAME," +
                "UI_FILE,MODEL_VALUE,COMPONENT,LIST_OF_TABLES,MAX_ROWS,BLOCKS,TABLE_WITH_MAX_ROWS, WHERE_CONTAINS_BINDS,MRU_COUNT) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?)");
            PreparedStatement pstmt = null;
            System.out.println("Now inserting values...");
            String[] labelSeries = null;
            String prevLabel = "";
            int count=0;
           
            while((line = fileReader.readLine()) != null) {
                
              String[] parts = line.split(",");
              if(parts.length < 15) continue;   
                
                if (releaseNo >= 11 && parts.length < 16) {
                    System.out.println("MRU not printed...continuing..this should not happen...");
                    continue;
                }
              if (LRGUtil.isDeferred(con, series, parts[3].trim()))
                    continue;
                
                if(prevLabel != parts[4].trim()) {
                    labelSeries = FamilyModuleHelper.getLabelSeries(parts[4].trim());
                    prevLabel = parts[4].trim();
                }
                
                String issueType = "";
                if(line.contains("savedSearch")){
                   issueType = "SavedSearchNoViewCriteria"; 
                    cstmt= cstmt1;
                    pstmt= pstmt1;
                }
                else if (line.contains("No DisplayViewCriteria") || line.toLowerCase().contains("implicitviewcriteria")){
                    issueType =  "NoViewCriteriaLOV";   
                    if(releaseNo >= 11){
                        cstmt= cstmt1;
                        pstmt= pstmt1;
                    }else {
                        cstmt= cstmt2;
                        pstmt= pstmt2;
                    }
                }
                else if(line.contains("No ViewCriteriaUsage") || line.contains("LOVSmartFilterNoViewCriteria")){
                    issueType =  "LOVSmartFilterNoViewCriteria"; 
                    if(releaseNo >= 11 && line.contains("No ViewCriteriaUsage") && parts[10].trim() != null && parts[10].trim().equals("0"))
                        issueType =  "LOVSmartFilterNoMRU"; 
                    cstmt= cstmt2;
                    pstmt= pstmt2;
                }
                else {
                    issueType =  "NoViewCriteria"; 
                    cstmt= cstmt1;
                    pstmt= pstmt1;
                }
              if(series.contains("FSM")){
                cstmt= cstmt2;
                pstmt= pstmt2;
              }
                
              cstmt.setString(2, parts[0].trim()); //family
              cstmt.setString(3, parts[1].trim()); //module
              cstmt.setString(4, parts[2].trim()); //product
              cstmt.setString(5, parts[3].trim()); //filename
              cstmt.setString(6,issueType);
              cstmt.setString(9, labelSeries[0]); //series
              cstmt.setString(10, labelSeries[1]); //label
                      
              cstmt.setString(7, parts[6].trim()); //subissue        
              cstmt.setString(8,parts[8]); //model value for uniqueness
              cstmt.execute();
                
              int seq = cstmt.getInt(1);
              pstmt.setInt(1,seq); 
              pstmt.setString(2,parts[5].trim()); //viewname
              pstmt.setString(3,parts[7].trim());  //jsff
              pstmt.setString(4,parts[8].trim()); //model
              pstmt.setString(5,parts[9].trim()); //component
                
                if(releaseNo < 11) {
              pstmt.setString(6,parts[10].trim()); //list of tables
              pstmt.setString(7,parts[11].trim()); //maxrows
              pstmt.setString(8,parts[12].trim()); //blocks
              pstmt.setString(9,parts[13].trim()); //table with max rows
              pstmt.setString(10,parts[14].trim()); //where contains binds  
              pstmt.setString(11,""); //MRU Count  
                } else {
                        pstmt.setString(6,parts[11].trim()); //list of tables
                        pstmt.setString(7,parts[12].trim()); //maxrows
                        pstmt.setString(8,parts[13].trim()); //blocks
                        pstmt.setString(9,parts[14].trim()); //table with max rows
                        pstmt.setString(10,parts[15].trim()); //where contains binds 
                        pstmt.setString(11,parts[10].trim()); //MRU Count          
                }
                try{
              pstmt.execute();    
                } catch(Exception e){
                    System.out.println(getString(parts));
                    e.printStackTrace();
                }
                count++;
            }
            con.commit();
            cstmt1.close();
            cstmt2.close();
            pstmt1.close();
            pstmt2.close();
            System.out.println("There are "+count+" violations for View Criteria Enhanced Scans on "+series+"\n\n");
            
        }catch(Exception e) {
            e.printStackTrace();
            if(con != null)
                con.rollback();
        } finally{
            if(con != null)
                con.close();
        }
      }
      
      private String getString(String[] parts) {
        String partsString = "";
        for(int i =0; i < parts.length; i++)
          partsString += parts[i] + ",";
        return partsString;
      }
  }
