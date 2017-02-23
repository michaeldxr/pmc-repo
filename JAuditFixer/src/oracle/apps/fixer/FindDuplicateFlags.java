package oracle.apps.fixer;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;

import oracle.apps.helpers.FileParserHelper;


public class FindDuplicateFlags {
    static BufferedWriter writer;
    public static void main(String[] args) throws Exception{
        String sCrawlDir = "/ade_autofs/ade_fusion_linux/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/fusionapps/";
        writer = new BufferedWriter(new FileWriter("duplicateFlags.txt"));
        crawlDirectory(sCrawlDir);
        writer.close();
    }
    
    public static void crawlDirectory(String path) {
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
                // Zeesha: Added, because for files modified recently there is an .ade_path file created in that directory, that is neither node, nor dir. Skip this without raising error
                if (sName.contains(".ade_path"))
                    continue;

                if (sName.endsWith("jpr"))
                    processFile(listOfFiles[i]);
                
            } else if (listOfFiles[i].isDirectory())
            {
                crawlDirectory(listOfFiles[i].getAbsolutePath());
            } else
            {
                System.out.println("ERROR:  node is neither file or directory: " +
                                   listOfFiles[i]);
            }
        }

    }
    
    public static void processFile(File f) {
        
        try{
            String absPath = f.getAbsolutePath();
            String fileContents = FileParserHelper.getFileContentsAsString(absPath);
            int n =0;
            int index = fileContents.indexOf("jbo.AttrSwitchGeneration");
            while(index != -1) {
                n++;
                index = fileContents.indexOf("jbo.AttrSwitchGeneration",index+20);
            }
            if(n >1)
                writer.write(absPath + "," + n + "\n");
        }catch(Exception e){
            e.printStackTrace();
        }
        
    }
        
}
