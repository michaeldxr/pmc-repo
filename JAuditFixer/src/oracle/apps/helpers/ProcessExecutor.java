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

public class ProcessExecutor {
    public ProcessExecutor() {
        super();
    }

    public int runProcess(String command, String writeToOutputFile, boolean appendToFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        return runProcess(pb, writeToOutputFile, appendToFile);

    }

    public int runProcess(ProcessBuilder pbuilder, String writeToOutputFile, boolean appendToFile) throws Exception {
        if (pbuilder == null)
            return 0;

        Process process1 = pbuilder.start();
       // Thread.sleep(1000);
        return monitorProcess(process1, writeToOutputFile, appendToFile);
    }

    public int monitorProcess(Process process, String writeToOutputFile, boolean appendToFile) throws Exception {
        ProcessStreamEater inputEater = new ProcessStreamEater(process.getInputStream(), writeToOutputFile, appendToFile);
        ProcessStreamEater errorEater = new ProcessStreamEater(process.getErrorStream(), writeToOutputFile+".err", true);
        //Thread.sleep(1000);
        inputEater.start();
        errorEater.start();
        process.waitFor();
        inputEater.join();
        Thread.sleep(2000);
        return process.exitValue();
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }

    private class ProcessStreamEater extends Thread {

        InputStream is;
        String outputFile = "";
        boolean writeOutputToFile = false;
        boolean appendToFile = false;

        public ProcessStreamEater(InputStream is) {
            this.is = is;
            writeOutputToFile = false;

        }

        public ProcessStreamEater(InputStream is, String writeOutputToFile, boolean appendToExistingFile) {
            this.is = is;
            if (!isEmpty(writeOutputToFile)) {
                this.writeOutputToFile = true;
                outputFile = writeOutputToFile;
                appendToFile = appendToExistingFile;
            } else {
                this.writeOutputToFile = false;
            }
        }

        @Override
        public void run() {
            try {
                BufferedWriter pw = null;
                if(is==null)
                    return;
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = null;
                if (writeOutputToFile) {
                    pw = new BufferedWriter(new FileWriter(new File(outputFile), appendToFile));
                }
                while ((line = br.readLine()) != null) {
                    if (writeOutputToFile) {
                        pw.write(line+"\n");
                    } else
                        System.out.println(line);

                }
                if (pw != null) {
                    pw.flush();
                    pw.close();
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
                }
        }
    }
}
