package oracle.apps.oatsAnalyzernull;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;

import java.io.IOException;

import java.util.Collection;

import java.util.Iterator;

import org.apache.commons.io.FileUtils;

public class OatsFileListFetcher implements Runnable {
    String dirLocation;
    String outDir;
    public String threadName="";
    public OatsFileListFetcher(String dirToFetch, String outDirLoc,int threadid) {
        dirLocation=dirToFetch;
        outDir=outDirLoc;
        if(threadName.equals("")){
            threadName="Thread-"+threadid;
            }
    }

    public void run() {
        Collection<File> fileList = FileUtils.listFiles(new File(dirLocation),  new String[] { "java","properties" }, true);
        Iterator itr = fileList.iterator();
        String fName="";
        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new FileWriter( new File(outDir+""+threadName)));
          //  System.out.println("Thread : "+threadName+" --> Appending to file "+outDir+""+threadName);
            while(itr.hasNext()){
                fName= ((File)itr.next()).getAbsolutePath();
                bw.write(fName);
                bw.write("\n");
            }
            bw.close();
        } catch (IOException e) {
            //Unable to create the oats list file
            System.out.println("Unable to create the oats list file");
        }
    }
}
