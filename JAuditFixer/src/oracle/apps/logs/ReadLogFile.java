package oracle.apps.logs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadLogFile {
    public ReadLogFile() {
        super();
    }
    
  public static void main(String[] args) {
        
    try{       
        BufferedReader fileReader = new BufferedReader(new FileReader("/scratch/sudgupta/Desktop/AdminServer_18_01_2011_1295389547.log"));
        BufferedWriter writer = new BufferedWriter(new FileWriter("log_query_time1.txt") ); 
        
        Pattern query = Pattern.compile("PSR SQL TRACE.*Execute.*took: ([0-9]+) ms.*sql text  is:");
        Matcher m;
        
        int num_queries=0;
        int num_queries_20 = 0;
     
        String line = fileReader.readLine();
      
        while(line != null) {       
            
            m = query.matcher(line);    
            
            if(m.find()) {
              num_queries++;
              int t = Integer.parseInt(m.group(1));            
             
              if(t > 20) { //log queries that take longer than 20 ms to execute
               writer.write("Query took: " + m.group(1) + " ms.\n ");
                num_queries_20++;
              }
            }            
            line = fileReader.readLine();            
        }
        System.out.println("Total # of Queries: " + num_queries);
        System.out.println("# of Queries that took over 20ms to execute: " + num_queries_20);
        writer.close();
        
    } catch(Exception e) {
            e.printStackTrace();
      }    
  }
}
