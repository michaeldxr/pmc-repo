package oracle.apps.stringscan;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import oracle.apps.helpers.FamilyModuleHelper;

public class FindInfoletConfigFiles {
    
    final static String ATK_INFOLET_CONFIG_FILENAME = "AtkInfoletConfig.xml";
    
    public static void main(String[] args) {
        
        // check if the command line arguments have been provided correctly
        if (args.length != 2) {
            System.out.println("Error, please provide label and output file path.\n" +
                " e.g. java FindInfoletConfigFiles <LABEL> <LOGDIR>/infoletConfigFiles.txt");
            return;
        }
        
        if (args[0] == null || args[0].length() == 0) {
            System.out.println("Error, label must be specified.");
            return;
        }
        
        if (args[1] == null || args[1].length() == 0) {
            System.out.println("Error, output file path must be specified.");
            return;
        }
        
        // retrieve the command line arguments
        String label = args[0];
        String outputFile = args[1];
        
        // determine the release number from the label path
        int releaseNum = 0;
        int seriesStartIndex = label.indexOf("FUSIONAPPS");
        if (seriesStartIndex != -1) {
            int seriesEndIndex = label.indexOf(".rdd", seriesStartIndex);
            String series = label.substring(seriesStartIndex, seriesEndIndex);

            String release = FamilyModuleHelper.getRelease(series);

            try {
                releaseNum = Integer.parseInt(release);
            } catch (NumberFormatException e) {
                releaseNum = 0;
            }
        }
        
        ArrayList<String> infoletConfigFiles = null;
        
        // based on the release number, determine if infolet config files will be checked
        if (releaseNum >= 13) {
            // retrieve the list of infolet config xmls from the AtkInfoletConfig.xml file and store in a list
            infoletConfigFiles = getInfoletConfigFiles(label);
        } else {
            // just initialize an empty list, so none of the infolet config files will be checked
            infoletConfigFiles = new ArrayList<String>();
        }
        
        // write each infolet config file to the output file
        FileWriter fileWriter = null;
        try {
            // open the output file
            fileWriter = new FileWriter(outputFile);
            for (String stringRecord : infoletConfigFiles) {
                fileWriter.write(stringRecord + "\n");
            }
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static ArrayList<String> getInfoletConfigFiles(String label) {
        
        // initialize an arraylist to store the list of infolet config files
        ArrayList<String> infoletConfigFiles = new ArrayList<String>();
        ZipFile jarFile = null;
        String jarFilePath = label + "/fusionapps/jlib/AdfAtkHomePageFuseProtectedModel.jar";
                
        try {
            // first check if the jar file exists at the specified path
            File file = new File(jarFilePath);
            if (!file.exists()) {
                throw new Exception("Error, " + jarFilePath + " does not exist.");
            }
            
            // get the jar file we want to extract from
            jarFile = new ZipFile(jarFilePath);
            
            // extract the contents of the jar file
            Enumeration<? extends ZipEntry> entries = jarFile.entries();
            
            // go through each file in the jar             
            while (entries.hasMoreElements()) {
                final ZipEntry zipEntry = entries.nextElement();
                final String fileName = zipEntry.getName();
                
                // check if it is the infolet config file that we're interested in 
                if (fileName.endsWith(ATK_INFOLET_CONFIG_FILENAME)) {
                    
                    BufferedReader br = null;
                    
                    // open the AtkInfoletConfig.xml file for reading
                    try {
                        InputStream input = jarFile.getInputStream(zipEntry);
                        br = new BufferedReader(new InputStreamReader(input));
                        
                        String line = br.readLine();
                        
                        // loop through each line in the file until the end of the file
                        while (line != null) {
                            // create the pattern and matcher objects
                            Pattern p = Pattern.compile("taskMenuSource=\"(.+?)\"");
                            Matcher m = p.matcher(line);
                            
                            if (m.find()) {
                                String filePath = m.group(1);                                
                                infoletConfigFiles.add(filePath);
                            }
                            
                            // read the next line
                            line = br.readLine();
                        }
                        
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        // close the xml file
                        if (br != null) {
                            try {
                                br.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // close the jar file
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return infoletConfigFiles;
    }
    
}
