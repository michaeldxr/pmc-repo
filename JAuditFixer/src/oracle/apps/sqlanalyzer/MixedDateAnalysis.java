package oracle.apps.sqlanalyzer;

import java.io.BufferedWriter;

import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;

import java.sql.ResultSet;
import java.sql.Statement;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

public class MixedDateAnalysis {

    static Connection con = null;
    HashMap<String,String> tabColData = new HashMap<String, String>();
    static int counter = 0;
    
    public static void main(String[] args) throws Exception{
        
        Date startDate = new Date();
        MixedDateAnalysis analyzer = new MixedDateAnalysis();
        try{
        con = DriverManager.getConnection("jdbc:oracle:thin:@//slcac752-vip:1546/rup2st3c", "fusion_read_only", "hTweru4568");       
      
        analyzer.getAllDateColumns();          
        } catch(Exception e){
            e.printStackTrace();
        }
        finally {
            if(con != null)
                con.close();
            
            analyzer.writeTableColumnData(); 
            System.out.println("Number of rows processed: " + counter);
        }
        Date endDate = new Date();
        System.out.println("Started:" + startDate);
        System.out.println("Finished:" + endDate);
    }
    
    public void getAllDateColumns() throws Exception {
        Statement stmt1 = con.createStatement();        
        Statement stmt2= con.createStatement();
        ResultSet rs1 = null;
        ResultSet rs2 = null;
        String tableName = "";
        String colName = "";
        String sql = "";    
      

        rs1 = stmt1.executeQuery("select TABLE_NAME,COLUMN_NAME from ALL_TAB_COLUMNS where OWNER='FUSION'" +
            "and DATA_TYPE = 'DATE' and table_name like 'AP_%' and table_name not like '%_V' and table_name not like '%_GT'");
        
       while(rs1.next()) {
            counter++;
//           if(counter >= 1000)
//               break;
            String timestamp = "";
            int timeStampedRows = 0;
            int nonTimeStampedRows = 0;
            int percentTimeStamped = 0;
           
           try {
            tableName = rs1.getString("TABLE_NAME");
            colName = rs1.getString("COLUMN_NAME");
           
           
            sql = "SELECT timestamped, count(timestamped) AS C FROM (SELECT (CASE WHEN " + colName + " = TRUNC(" + colName + ") " +
                "then 'N' else 'Y' END) AS timestamped FROM " + tableName + " WHERE ROWNUM < 5000) GROUP BY timestamped" ;
            rs2 = stmt2.executeQuery(sql);
           
            while(rs2.next()) {
                String timestamptedCol = rs2.getString("TIMESTAMPED");
                int count = rs2.getInt("C");     
                
                if(timestamptedCol.equals("Y"))
                    timeStampedRows = count;
                else if(timestamptedCol.equals("N"))
                    nonTimeStampedRows = count;             
            }   
            timestamp = "Y = " + timeStampedRows + ",N = " + nonTimeStampedRows;
            if(timeStampedRows > 0 && nonTimeStampedRows > 0)
                percentTimeStamped = (timeStampedRows*100)/(nonTimeStampedRows+timeStampedRows);
            else if(timeStampedRows == 0)
                percentTimeStamped = 0;
            else if(nonTimeStampedRows == 0)
                percentTimeStamped = 100;
            
            timestamp += "," + percentTimeStamped;
           
            tabColData.put(tableName + "," + colName,timestamp);
           } catch(Exception e){
             //  e.printStackTrace();
               System.out.println(tableName + "," + colName + "," + e.getMessage());
           }
        }
       
        rs1.close();
        rs2.close();
        stmt1.close();
        stmt2.close();
        
    }
    
    public void writeTableColumnData() throws Exception {
        
        BufferedWriter writer = new BufferedWriter(new FileWriter("tableColumnDates.txt"));
        writer.write("TableName,ColumnName,TimeStampedRows,NontimeStampedRows,Percentile\n");
        
        for(Iterator<String> it = tabColData.keySet().iterator(); it.hasNext();) {
            String tableColumn = it.next();
            String timestamp = tabColData.get(tableColumn);
            writer.write(tableColumn + "," + timestamp + "\n");
        }
        writer.close();
    }
}
