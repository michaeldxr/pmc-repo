package oracle.apps.logs;

import java.io.BufferedReader;
import java.io.FileReader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadExceptions {
  
    
  public static void main(String[] args) {
        
    try{
       
        HashMap<String,Integer> exceptions = new HashMap<String,Integer>();
        BufferedReader fileReader = new BufferedReader(new FileReader("/scratch/sudgupta/Desktop/exceptions.txt"));
        
        Pattern exceptionUsage = Pattern.compile(".*\\.(.*Exception.*?): ");
        Matcher m;
      
        String line = fileReader.readLine();
        while(line != null) {
            
            m = exceptionUsage.matcher(line);
            if(m.find()) {
               String ex = m.group(1);
               if(exceptions.containsKey(ex)) {
                 int count = exceptions.get(ex);
                 exceptions.put(ex,++count);
               }
               else
                 exceptions.put(ex,1);
            }            
            line = fileReader.readLine();            
        }
        
      Iterator i = exceptions.keySet().iterator(); 
        while(i.hasNext()){
          String ex = (String)i.next();
          System.out.println(ex + " -> " +  exceptions.get(ex));
        }
    } catch(Exception e) {
            e.printStackTrace();
      }
    
  }
}
