package oracle.apps.references;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileWriter;

import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import java.util.zip.ZipEntry;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DuplicateEAREntires {
    
    private static BufferedWriter writer;
    
    public static void main(String[] args) throws Exception{
        
        String crawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_PT.R9INT_LINUX.X64.rdd/LATEST/fusionapps/fin";
        writer = new BufferedWriter(new FileWriter("duplicateEAREntries.csv"));
        writer.write("FileName, Description, Duplicates \n");
        
        DuplicateEAREntires scanner = new DuplicateEAREntires();
        scanner.crawlDirectory(crawlDir);
        writer.close();
    }
    
    private void crawlDirectory(String crawlDir) {
        
        if (crawlDir.contains(".ade_path") || crawlDir.contains("/classes/") || crawlDir.contains("/noship"))
            return;
        
        String pathAfterFusionapps = FamilyModuleHelper.getPathAfterViewRoot(crawlDir);
        String[] parts = pathAfterFusionapps.split("/");
        if(parts.length > 2 && !pathAfterFusionapps.contains("deploy"))
            return;

        File folder = new File(crawlDir);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            try{
                if (listOfFiles[i].isFile()) {
                    String absFilePath = listOfFiles[i].getAbsolutePath();
                    if(absFilePath.endsWith(".ear"))
                        processEar(absFilePath);                
    
                } else if (listOfFiles[i].isDirectory()) {
                    String dir = listOfFiles[i].getAbsolutePath();
                    crawlDirectory(dir);
                } else {
                    System.out.println("ERROR:  node is neither file or directory: " + listOfFiles[i].getAbsolutePath());
                }
            }catch(Exception e){
                    e.printStackTrace();
            }
        }
    }
    
    private void processEar(String absPath) {
        try{
            HashMap<String,ArrayList<String>> earEntries = new HashMap<String,ArrayList<String>>();
            JarInputStream jarStream = new JarInputStream(new FileInputStream(absPath));
            JarFile jarFile = new JarFile(absPath);

            if (jarStream == null) {
                System.err.println("[ERROR] Could not open jar stream");
                return;
            }
            
            while (true) {

                JarEntry jarEntry = jarStream.getNextJarEntry();
                if (jarEntry == null) {
                    break;
                }
                String strEntryName = jarEntry.getName().trim();
                if(!jarEntry.isDirectory()){
                    
                    String lastPart = "";
                    int index = strEntryName.indexOf("oracle/apps");
                    if(index != -1)
                        lastPart = strEntryName.substring(index);
                    else{
                        String[] parts = strEntryName.split("/");
                        lastPart = parts[parts.length-1];
                    }                   
                    
                    ArrayList<String> commonEntries = new ArrayList<String>();
                    if(earEntries.containsKey(lastPart))
                       commonEntries = earEntries.get(lastPart); 
                    
                    commonEntries.add(strEntryName);
                    earEntries.put(lastPart,commonEntries);
                    
                    if(strEntryName.endsWith(".jar") || strEntryName.endsWith(".war") || strEntryName.endsWith(".mar")){
                        
                        JarInputStream nestedJarStream = new JarInputStream(jarFile.getInputStream(jarEntry));
                        while(true){
                            JarEntry nestedJarEntry = nestedJarStream.getNextJarEntry();
                            if(nestedJarEntry == null)
                                break;
                            String nestedEntryName = nestedJarEntry.getName().trim();
                            if(!nestedJarEntry.isDirectory()){
                                
                                index = nestedEntryName.indexOf("oracle/apps");
                                if(index != -1)
                                    lastPart = nestedEntryName.substring(index);
                                else{
                                    String[] parts = nestedEntryName.split("/");
                                    lastPart = parts[parts.length-1];
                                }
                                commonEntries = new ArrayList<String>();
                                if(earEntries.containsKey(lastPart))
                                   commonEntries = earEntries.get(lastPart); 
                                
                                commonEntries.add(strEntryName+"/"+nestedEntryName);
                                earEntries.put(lastPart,commonEntries);
                                
                                if(nestedEntryName.endsWith(".jar") || nestedEntryName.endsWith(".mar")){
                                    ZipEntry je = jarFile.getEntry(strEntryName + "!" + nestedEntryName);
                                    System.out.println(je);
                                }
                                
                            }
                        }
                    }
                        
                }                
            }
            
            for(Iterator<String> it = earEntries.keySet().iterator();it.hasNext();){
               
                String jarEntry = it.next();
                ArrayList<String> duplicates = earEntries.get(jarEntry);
                if(duplicates.size() < 2)
                    continue;
                String duplicateFiles = "";
                
                if(jarEntry.endsWith(".jar") || jarEntry.endsWith(".war") || jarEntry.endsWith(".mar")){
                    for(int c = 0; c < duplicates.size();c++)
                        duplicateFiles += duplicates.get(c) + "; ";
                }else{
                    
                    HashSet<String> duplicateZips = new HashSet<String>();
                    for(int c = 0; c < duplicates.size();c++){
                        
                        String dupFile = duplicates.get(c);
                        String[] parts = dupFile.split("/");
                        for(int j = 0; j < parts.length; j++){
                            if(parts[j].endsWith(".jar") || parts[j].endsWith(".mar") || parts[j].endsWith(".war")){
                                duplicateZips.add(parts[j]);
                                break;
                            }
                        }
                    }
                    if(duplicateZips.size() != 1){
                        for(int c = 0; c < duplicates.size();c++)
                            duplicateFiles += duplicates.get(c) + "; ";
                    }
                }
                
                if(!duplicateFiles.equals(""))
                    writer.write(FamilyModuleHelper.getPathAfterViewRoot(absPath) + "," + jarEntry + "," + duplicateFiles + "\n");
            }
            
        }catch(Exception e){
            System.out.println("Exception while processing file: " + absPath);
            e.printStackTrace();
        }
    }
}
