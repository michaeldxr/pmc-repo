package oracle.apps.helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import oracle.apps.utility.JoesBaseClass;

public class AdeDiffHelper {
    /**
     * Writer for diff.txt
     */
    private BufferedWriter diff;

    private String m_diffFileName = "";

    public AdeDiffHelper(String diffFileName) {
        super();
        m_diffFileName = diffFileName;
    }
    
  private class StreamEater extends Thread {

      InputStream is;
      boolean isDiff;

      public StreamEater(InputStream is) {
          this(is, false);
      }

      public StreamEater(InputStream is, boolean isDiff) {
          this.is = is;
          this.isDiff = isDiff;
      }

      @Override
      public void run() {
          try {
              PrintWriter pw = null;

              InputStreamReader isr = new InputStreamReader(is);
              BufferedReader br = new BufferedReader(isr);
              String line = null;
              if (isDiff) {
                  pw = new PrintWriter(new FileOutputStream(new File(m_diffFileName), true));
              }
              while ((line = br.readLine()) != null) {
                  if (isDiff) {
                      pw.println(line);
                  }
                  else
                      System.out.println(line);
                  if (line.contains("ade ERROR")) {
                      System.exit(-1);
                  }
              }
              if (pw != null) {
                  pw.flush();
                  pw.close();
              }
          }
          catch (IOException ioe) {
              ioe.printStackTrace();
          }
      }
  }
    
    public void checkoutAndDelete(File f) throws Exception {
        ProcessBuilder pb1 = new ProcessBuilder("ade", "co", "-nc",
                                                f.getAbsolutePath());
        Process process1 = pb1.start();
    //    System.out.println("ade co -nc " + f.getName());
        StreamEater input1Eater = new StreamEater(process1.getInputStream());
        StreamEater error1Eater = new StreamEater(process1.getErrorStream());
        input1Eater.start();
        error1Eater.start();
        process1.waitFor();

        ProcessBuilder pb2 = new ProcessBuilder("rm", "-f",
                                                f.getAbsolutePath());
        Process process2 = pb2.start();
     //   System.out.println("rm -f " + f.getName());
        StreamEater input2Eater = new StreamEater(process2.getInputStream());
        StreamEater error2Eater = new StreamEater(process2.getErrorStream());
        input2Eater.start();
        error2Eater.start();
        process2.waitFor();
    }
    
  public void checkinAndDiff(File f) throws Exception {
      checkin(f);

      ProcessBuilder pb2 = new ProcessBuilder("ade", "diff", "-pred",
                                              f.getAbsolutePath());
      Process process2 = pb2.start();
   //   System.out.println("ade diff -pred " + f.getName() + " >> diff.txt");
      StreamEater input2Eater = new StreamEater(process2.getInputStream(), true);
      StreamEater error2Eater = new StreamEater(process2.getErrorStream(), true);
      input2Eater.start();
      error2Eater.start();
      process2.waitFor();
  }

  public void checkin(File f) throws Exception {
      ProcessBuilder pb1 = new ProcessBuilder("ade", "ci",
                                              f.getAbsolutePath());
      Process process1 = pb1.start();
   //   System.out.println("ade ci " + f.getName());
      StreamEater input1Eater = new StreamEater(process1.getInputStream());
      StreamEater error1Eater = new StreamEater(process1.getErrorStream());
      input1Eater.start();
      error1Eater.start();
      process1.waitFor();
  }
    

    /**
     * Start the diff log. Remove any previously existing diff.txt file
     * @throws Exception
     */
    public void startDiffLog() throws Exception {

        // Remove any existing diff.txt
        Process p = Runtime.getRuntime().exec("rm -f " + m_diffFileName);
        checkProcess(p, null, "remove any existing diff log", false);

        diff = new BufferedWriter(new FileWriter(m_diffFileName));

    }

    public void checkProcess(Process p, File f, String description,
                             boolean isDiff) throws Exception {
        //System.out.println("In checkProcess, before calling waitFor...");
        int exitCode = p.waitFor();
       
      //System.out.println("In checkProcess, after calling waitFor...");
        if (!isDiff && exitCode != 0) {
           if (f != null)
                System.out.println("There was an error when trying to " +
                                   description + " for file : " +
                                   f.getAbsolutePath());
            else
                System.out.println("There was an error when trying to " +
                                   description);
        }

        dumpInputStream(p.getInputStream(), isDiff);
        dumpInputStream(p.getErrorStream(), isDiff);
    }

    public void dumpInputStream(InputStream in,
                                boolean isDiff) throws Exception {

        int n = in.available();
        byte b[] = new byte[n];
        in.read(b, 0, n);
        String str = new String(b);
        if (str.contains("ade ERROR")) {
            System.out.println(str);
            System.out.println("There was an error interacting with ADE, exiting script.");
            System.exit(-1);
        }

        if (isDiff == false)
            System.out.println(str.trim());
        else {

            if (diff == null) {
                System.out.println("hey diff was null");
            }
            diff.write(str);
        }
    }


    public void closeDiffLog() throws Exception {
        diff.close();
    }
    
    public void applyFix(String absPath,
                        String newFileContents) throws Exception {
       
       File file = new File(absPath);
       if (!file.exists()) {
           throw new Exception("while making fix, file not found: " +
                               absPath);
       }
       checkoutAndDelete(file);
        
       System.out.println("Writing new file...");
       FileWriter fw = new FileWriter(file.getAbsolutePath());
       BufferedWriter bw = new BufferedWriter(fw);
       bw.write(newFileContents);
       if (bw != null)
           bw.close();
       if (fw != null)
           fw.close();
       checkinAndDiff(file);
    }
}
