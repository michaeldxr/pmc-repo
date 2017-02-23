package oracle.apps.logs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadGcLogs {
  
    
  public static void main(String[] args) {
      
    if (args.length < 1) {
        System.out.println("Usage:  ReadGcLogs.sh <path to log file>");
        System.exit(1);
    }
    String filename = args[0];
    Pattern memoryUsage = Pattern.compile("\\[INFO \\]\\[memory \\] \\[(YC|OC)#(\\d+)\\].*?(\\d+)KB->(\\d+)KB");
    Matcher m;
        
    try{        
        BufferedWriter outputFileWriter = new BufferedWriter(new FileWriter("Memory_Usage.csv") );
       
        BufferedReader fileReader = new BufferedReader(new FileReader(filename));    
        
        outputFileWriter.write("GC,Seq#,Before,After\n");
      
        String line = fileReader.readLine();
        while(line != null) {
            m = memoryUsage.matcher(line);
            if(m.find())
                outputFileWriter.write(m.group(1) + "," + m.group(2) + "," + m.group(3) + "," + m.group(4) + "\n");
            
            line = fileReader.readLine();            
        }        
        outputFileWriter.close();
    } catch(Exception e) {
            e.printStackTrace();
      }    
  }
}
