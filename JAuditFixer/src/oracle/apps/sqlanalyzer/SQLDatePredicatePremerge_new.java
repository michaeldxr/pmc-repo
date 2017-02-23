package oracle.apps.sqlanalyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.sql.DriverManager;

import oracle.apps.helpers.FamilyModuleHelper;

public class SQLDatePredicatePremerge_new {
       
    public static void main(String[] args) throws Exception{
        SQLDatePredicateIssues_new scanner = new SQLDatePredicateIssues_new();
        
        //String[] files = args[0].trim().split("\n");
        String ade_view_root = args[1].trim();
        String filelistpath=args[0].trim();
        String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);
        scanner.writer = new BufferedWriter(new FileWriter("where_clause_scan.csv"));
        scanner.generateExemptionList();
        scanner.writer.write("Family,Module,Product,Filename,Label,Sysdate Pattern," +
            "Table Name1, Column Name1, ResolvedTableCol1, TimeStamped Rows1, NonTimestamped Rows 1, " +
            "Percent TimeStamped1, Table Name2, Column Name2, ResolvedTableCol2, TimeStamped Rows2, " +
            "NonTimeStamped Rows 2, Percent Timestamped2\n");
        
        try{
            scanner.con = DriverManager.getConnection("jdbc:oracle:thin:@//slc03zob.us.oracle.com:1522/slc03zob", "fusion", "fusion");
        } catch(Exception e){
            e.printStackTrace();
        }         
        
        SqlIndexFinder.readViewDataFromFile();
        //    SqlIndexFinder.populateDateTypes();
        scanner.readTableColTimeStampsFromFile();   
        
        for(int i = 0; i < files.length; i++) {
            String filePath = ade_view_root + "/" + files[i].trim();
            File f = new File(filePath);
            if(!f.exists())
                continue;
            
            scanner.processFile(f,false);
        }          
        
        scanner.writer.close();
        
        BufferedWriter outputFileWriter = new BufferedWriter(new FileWriter("sql_date_scan.txt"));
        
        BufferedReader reader = new BufferedReader(new FileReader("where_clause_scan.csv"));
        String line = reader.readLine();
        boolean hasViolation = false;
        while((line = reader.readLine()) != null){
            
            String[] parts = line.split(",");
            if(parts.length < 12) continue;         
            
                hasViolation = true;
                outputFileWriter.write("Issue: Sysdate Not Truncated\n");
                outputFileWriter.write("FileName: " + parts[3] + "\n");
                outputFileWriter.write("Sysdate Pattern: " + parts[5] + "\n");
                outputFileWriter.write("Table Name1: " + parts[6] + "\n");
                outputFileWriter.write("Column Name1: " + parts[7] + "\n");
                outputFileWriter.write("Resolved TableCol1: " + parts[8] + "\n");  
                outputFileWriter.write("TimestampedRows1: " + parts[9] + "\n");  
                outputFileWriter.write("NonTimeStampedRows1: " + parts[10] + "\n"); 
                outputFileWriter.write("PercentTimeStamped1: " + parts[11] + "\n");
            
                if( parts.length== 18) {
                    outputFileWriter.write("Table Name2: " + parts[12] + "\n");
                    outputFileWriter.write("Column Name2: " + parts[13] + "\n");
                    outputFileWriter.write("Resolved TableCol2: " + parts[14] + "\n");    
                    outputFileWriter.write("TimestampedRows2: " + parts[15] + "\n");  
                    outputFileWriter.write("NonTimeStampedRows2: " + parts[16] + "\n");  
                    outputFileWriter.write("PercentTimeStamped2: " + parts[17] + "\n\n");
                }
        }
        
        reader.close();
        
        if(hasViolation)
            outputFileWriter.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/SysdateNotTruncated " +
                "for description of the issue and resolution.\n\n\n");
               
        outputFileWriter.close();
    
    }
}
