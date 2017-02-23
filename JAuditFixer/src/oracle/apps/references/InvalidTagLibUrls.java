package oracle.apps.references;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;

import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JprHelper;
import oracle.apps.utility.LRGUtil;

public class InvalidTagLibUrls {
    
    static BufferedWriter writer;
    private static String mode="view";// can be LRG/view/premerge
    private static final String issuetype="InvalidTagLibURL";
    private static HashSet<String> uniqueTagLibs = new HashSet<String>();
    
    public static void main(String[] args) {
        
        try{
        InvalidTagLibUrls scanner = new InvalidTagLibUrls();
        writer = new BufferedWriter(new FileWriter("InvalidTagLibURL.csv") );        
        writer.write("Family,Module,Product,FileName,Series,Label,TagLibs\n"); 
        
        BufferedWriter writer1 = new BufferedWriter(new FileWriter("taglibs.txt"));
            
        mode = System.getProperty("mode");
        if(mode == null)
            mode = "view";
            
        if(mode.equals("view")) {
        
            //String sCrawlDir = args[0];
            String sCrawlDir="/ade_autofs/ade_fusion_linux/FUSIONAPPS_PT.R8INT_LINUX.X64.rdd/LATEST/fusionapps/fin/components";
            System.out.println("Analyzing all files in directory (Invalid Tag Lib URL check):" + sCrawlDir);      
            scanner.crawlDirectory(sCrawlDir); 
        } else if(mode.equals("LRG")) {
            String series = args[0];
            //String series = "FUSIONAPPS_PT.R8INT_LINUX.X64";
            String label = LRGUtil.getLatestLabel(series);
            System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);
            
            ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
            
            for (int j = 0; j < families.size(); j++) {
                
                String family = families.get(j);
                String sCrawlDir = LRGUtil.getCrawlDir(series, family, label, issuetype);
                System.out.println("Crawling dir '" + sCrawlDir + "' for family: " + family);                    
                scanner.crawlDirectory(sCrawlDir);                    
                System.out.println("Done crawling for family "+family+".");
                writer.flush();
            }                
            LRGUtil.addLabelInfoToAB(issuetype, series, label);
        } else if(mode.equals("premerge")) {
            
        }
            
        writer.close();
           
            for(Iterator<String> it = uniqueTagLibs.iterator();it.hasNext();) 
                writer1.write(it.next() + "\n");
            
            writer1.close();
            
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    
    private void crawlDirectory(String path)
    {
        if (path.contains(".ade_path") || path.contains("/classes/"))
            return;
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++)
        {
            if (listOfFiles[i].isFile())
            {
                String sName = listOfFiles[i].getAbsolutePath();
                if (sName.contains(".ade_path"))
                    continue;

                if (fileOfInterest(sName))
                    processFile(listOfFiles[i].getAbsolutePath());
                
            } else if (listOfFiles[i].isDirectory())
            {
                if (FamilyModuleHelper.isPathOfInterest(listOfFiles[i].getAbsolutePath()))
                    crawlDirectory(listOfFiles[i].getAbsolutePath());
            } else
                System.out.println("ERROR:  node is neither file or directory: " +
                                   listOfFiles[i]);
        }
    }

    
    public boolean fileOfInterest(String sName) 
    {
        if(!FamilyModuleHelper.isPathOfInterest(sName))
            return false;  
        
        if (sName.endsWith(".jpr"))
            return true;
        
        return false;
    }
    
    public void processFile(String fName) {
        try{
            
            HashSet<String> taglibs = JprHelper.getAllTagLibUrlsinJpr(fName);
            String tagLibUrls="";
            for(Iterator<String> it = taglibs.iterator(); it.hasNext();){
                String taglib= it.next();
                tagLibUrls += taglib+"; ";
                uniqueTagLibs.add(taglib);
            }
            
            if(!FamilyModuleHelper.isEmpty(tagLibUrls))
            writer.write(FamilyModuleHelper.getFileNameInfo1(fName) + tagLibUrls + "\n");
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
