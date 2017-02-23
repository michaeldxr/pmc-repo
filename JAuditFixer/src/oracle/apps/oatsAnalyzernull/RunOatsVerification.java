package oracle.apps.oatsAnalyzernull;

//import com.sun.xml.internal.bind.annotation.OverrideAnnotationOf;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileReader;

import java.io.FilenameFilter;
import java.io.IOException;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;

import java.util.concurrent.Executors;

import java.util.concurrent.TimeUnit;

import oracle.apps.helpers.FamilyModuleHelper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;


public class RunOatsVerification {

    public static int ERRORSRAISED = 0;
    public static String oatsTempDir="";//="/home/rackumar/Desktop/OutFiles/";
    
    public static String determineRelease(String label){
        String release = FamilyModuleHelper.getRelease(label);
        if(release != null){
           //System.out.println("Transaction belongs to release : "+ release);
            return release;
        }
        else{
       //     System.out.println("Transaction release cannot be determined");
            return null;
        }   
    }
    
    public static void generateOatsFileList(String oatsDir){
        File dirList = new File(oatsDir);
        String[] directories= dirList.list(new FilenameFilter(){
                @Override
                public boolean accept(File dir, String name) {
                    return new File(dir,name).isDirectory();
                }
            });
        int threadCount = Runtime.getRuntime().availableProcessors();
    //    System.out.println("Thread Count : "+threadCount);
        Queue<String> taskQueue = new LinkedList<String>();
        for(int i=0;i<directories.length;i++){
                taskQueue.add(oatsDir+""+directories[i]);
            }
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        int threadInitCount=1;
        while(!taskQueue.isEmpty()){
            executorService.execute(new OatsFileListFetcher(taskQueue.remove(),oatsTempDir,threadInitCount));
            threadInitCount++;
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.valueOf("4"), TimeUnit.MINUTES);
        } catch (InterruptedException e) {
        //Unable to complete tasks
        }
        Collection<File> tempFiles = FileUtils.listFiles(new File(oatsTempDir),  TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        Iterator<File> itr = tempFiles.iterator();
        File oatsFinalOutList = new File(Verify.OATSSOURCEDIRLIST);
       
            try {
                while(itr.hasNext()){
                  File processFile = itr.next();
                  FileUtils.write(oatsFinalOutList, FileUtils.readFileToString(processFile), true);
                }
            } catch (IOException e) {
            //Unable to append file
            }       
    }
    
    public static void printFile(){
            BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(new File("Verify.OATSSOURCEDIRLIST")));
            String tmpBuffer="";
            while((tmpBuffer=br.readLine())!=null){
                System.out.println(tmpBuffer);
                }
            br.close();;
        } catch (IOException e) {
       //     System.out.println("Cannot read OATS FILE");
        }
    }

    public static void main(String[] args) {
    
        Verify.LABELVERSOURCEFILELOC = args[0];
        Verify.VIEWVERSOURCEFILELOC = args[1];
        Verify.LABELVERSOURCEFILEOUTPUTLOC = args[2];
        Verify.VIEWVERSOURCEFILEOUTPUTLOC = args[3];
        Verify.IGNORETAGSFILELOC = args[4];
        Verify.OATSSOURCEDIRLIST = args[5];
        Verify.OATSSOURCEOUTPUTDIR = args[6];
        Verify.VOMATCHOUTDIR = args[7];
        Verify.VOMATCHINDIR = args[8];
        Verify.PAGEDEFLISTFILELOC = args[9];
        Verify.VIEWVERBUNDLEDIR = args[10];
        Verify.LABELVERBUNDLEDIR = args[11];
        Verify.BUNDLELIST = args[12];
        Verify.TMPUIFILELIST = args[13];
        Verify.LABELVERBUNDLEUIFILELOC = args[14];
        Verify.LABELVERBUNDLEUIFILEOUTPUTLOC = args[15];
        Verify.labelverFiles = args[16];
        Verify.BUNDLEVIEWLIST = args[17];
        String oatsArPath=args[18];
        String label=args[19];
        String mapFile=args[20];
        oatsTempDir=args[21];
//        System.out.println("Map File :"+mapFile);
        HashMap<String,String> releaseMap = new HashMap<String,String>();
        releaseMap = generateReleaseMap(mapFile);
    //    System.out.println(releaseMap.toString());
        String release = determineRelease(label);
  //      System.out.println("Release : "+release);
        if(releaseMap.get(release)==null)
            release="12";
        Verify.OATSSOURCEDIR=releaseMap.get(release)+"/"+oatsArPath;
      //  System.out.println("NEW OATS PATH : "+Verify.OATSSOURCEDIR);
        generateOatsFileList(Verify.OATSSOURCEDIR);
        Verify.StartVerification();
    }

    private static HashMap<String, String> generateReleaseMap(String mapFile) {
        HashMap<String,String> relMap = new HashMap<String,String>();
        File releaseFile = new File(mapFile);
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(releaseFile));
            String tmpString="";
            while((tmpString=br.readLine())!=null){
                    String[] splits = tmpString.split(":");
                    relMap.put(splits[0], splits[1]);
                }
            br.close();
            
        } catch (IOException e) {
          System.out.println("Unable to generate OATS label mapping");
        }
        return relMap;
    }
}

