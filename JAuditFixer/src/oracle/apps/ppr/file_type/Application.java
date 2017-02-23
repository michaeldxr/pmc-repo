package oracle.apps.ppr.file_type;

import java.io.File;

import java.io.FileNotFoundException;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Application {
  public Application() {
    super();
  }
  
  public static void processFile(String absPath){
    Scanner s = null;
    String line, jprPath;
    Pattern p = Pattern.compile(" *<hash><url n=\"URL\" path=\"(.+)\"/></hash>");
    Matcher m;
    
    
    boolean foundJpr = false;
    try {
      s = new Scanner(new File(absPath));
      while(s.hasNextLine()){
        line = s.nextLine();
        if(!foundJpr &&line.trim().equals("<list n=\"listOfChildren\">")){
          foundJpr = true;
        }else if(line.trim().equals("</list>")){
          s.close();
          return;
        }else{
          m = p.matcher(line);
          if(m.matches() && !m.group(1).contains("dbSchema")){
            jprPath = getAbsoluteADEPath(m.group(1),absPath);
            Project.processFile(jprPath);
          }
        }
      }
    } catch (FileNotFoundException e) {
      System.out.println("jws file not found");
    } finally{
      s.close();
    }
  }
  
  private static String getAbsoluteADEPath(String relativePath,
                                           String jwsPath) {
    String absPath = "";
    int dirLevel = 1;
    String prefix = "";
    String[] s1 = null;
    
    if(relativePath.contains("..")){
      s1 = relativePath.split("/");
      for (String s : s1) {
        if (s.equals(".."))
          dirLevel++;
        else {
          if (s.endsWith("jpr"))
            absPath += s;
          else
            absPath += s + "/";
        }
      }
    }else{
         absPath =  relativePath;
    }

    s1 = jwsPath.split("/");

    int offset = s1.length - dirLevel;

    for (int i = 0; i < offset; i++) {
      prefix += s1[i] + "/";
    }

    absPath = prefix + absPath;

    return absPath;
  }
}
