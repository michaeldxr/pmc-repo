package oracle.apps.psr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;
import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ELHelpProviderScannerLRG {
    private static HashMap<String, HashSet<String>> _jarMap = new HashMap<String, HashSet<String>>();
    private static int m_numberJarFiles = 0;
    private static int m_numberJprFiles = 0;
    private static boolean bDebug = true;
    private static Workbook m_myBook;
    
    private static Worksheet m_elPrefixesSheet;
    private static BufferedWriter writer;
    private int numberOfExceptions = 0;
    private static String fullLabel;
    private StringBuffer exceptions = new StringBuffer();
    
    private static final String issuetype = "ELHelpProvider";

   
    public static void main(String[] args) throws Exception{
        try {
            if (args.length != 1 || args[0] == null || args[0].equals("")) {
                System.out.println("Series has to be specified for " + issuetype);
                System.exit(1);
            }

            String series = args[0];
        
            String label = LRGUtil.getLatestLabel(series);
            fullLabel = series+"_"+label;
            System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
            System.out.println("Latest label is: " + label);

            writer = new BufferedWriter(new FileWriter("ELHelpProvider_LRG.csv"));
            writer.write("Label,Prefix,Jar1,Jar2,\n");

            String sCrawlDir = LRGUtil.getCrawlDir(series, null, label,issuetype);
            System.out.println("Crawling dir: " + sCrawlDir);
            
            ELHelpProviderScannerLRG eLHelpProviderFixer = new ELHelpProviderScannerLRG();
            eLHelpProviderFixer.crawlDirectory(sCrawlDir);
            eLHelpProviderFixer.writeResultsToSheet();
            
            System.out.println("Done crawling directory.");
    
            writer.close();

            LRGUtil.addLabelInfoToAB(issuetype, series, label);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
   

    public void writeResultsToSheet() throws Exception{
        if (_jarMap == null || _jarMap.size() <= 0)
            return;

        Set<String> keySet = _jarMap.keySet();
        Iterator<String> keyIter = keySet.iterator();
      
        while (keyIter.hasNext()) {
            String prefix = keyIter.next();
            HashSet<String> jars = _jarMap.get(prefix);
            String product = "";
            String formattedJars = "";
            String jarsForcsv = "";
            if (jars != null && jars.size() > 1) {

                Iterator<String> jarsIter = jars.iterator();
                while (jarsIter.hasNext()) {
                    String nextjar = jarsIter.next();
                    formattedJars = formattedJars + " \n " + nextjar;
                    jarsForcsv =  getFileName(nextjar) + "," + jarsForcsv;
                }

                if(jarsForcsv.contains("AdfAppCmmnCompActivitiesPublicResource.jar") 
                   && prefix.contains("AppCmmnCompActivities_"))
                    continue;
                writer.write(fullLabel + "," + prefix + "," + jarsForcsv + "\n");
            }
        }
    }


    private void crawlDirectory(String crawlDir) {
        if (crawlDir.contains(".ade_path") || crawlDir.contains("/classes/") || crawlDir.contains("/noship"))
            return;

        File folder = new File(crawlDir);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                String absFilePath = listOfFiles[i].getAbsolutePath();
                if (isValidJarFile(absFilePath)) {
                    m_numberJarFiles++;
                    processJarForClassInMultipleJars(listOfFiles[i]);
                }

            } else if (listOfFiles[i].isDirectory()) {
                String dir = listOfFiles[i].getAbsolutePath();
                crawlDirectory(dir);
            } else {
                System.out.println("ERROR:  node is neither file or directory");
            }
        }
    }


    /**
     * For jar file -> look for adf-settings.xml
     * find prefix tag in adf-settings.xml and log it in the jar map
     * jarMap: Key-Prefix, Value -> HashSet<Jar path>
     * @param f Jar file that is to be processed
     */
    private void processJarForClassInMultipleJars(File f) {
        String jarPath = f.getAbsolutePath();

        try {
            JarInputStream jarStream =
                new JarInputStream(new FileInputStream(f));
            JarFile jar = new JarFile(f);
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

                if (!jarEntry.isDirectory()) {
                    if (strEntryName.contains("adf-settings.xml")) {

                        // check if adfsettings has a prefix
                        InputStream contents = jar.getInputStream(jarEntry);
                        XMLDocument adfsettingsXml =
                            XMLParserHelper.getXMLDocument(contents);
                        if (adfsettingsXml == null)
                            continue;
                        NodeList helpProviderNodes =
                            adfsettingsXml.getElementsByTagName("help-provider");
                        if (helpProviderNodes == null)
                            continue;
                        int helpProvLen = helpProviderNodes.getLength();
                        for (int i = 0; i < helpProvLen; i++) {
                            Node helpProvNode = helpProviderNodes.item(i);
                            if (helpProvNode == null)
                                continue;
                            if (XMLParserHelper.isNodeName(helpProvNode,
                                                           "help-provider")) {
                                String prefix =
                                    XMLParserHelper.getAttributeValue(helpProvNode,
                                                                      "prefix");
                                if (isEmpty(prefix))
                                    continue;
                                System.out.println("Found adf-settings with prefix " +
                                                   prefix + " in file:" +
                                                   f.getName());
                                addToJarMap(prefix, jarPath);
                            }
                        }
                        if (contents != null)
                            contents.close();
                    }
                }
            }
            if (jar != null)
                jar.close();
            if (jarStream != null)
                jarStream.close();
        } catch (IOException ex) {
            System.err.println("Error occurred while opening jar file: " +
                               f.getName());
            logException(jarPath, ex);
            return;
        }
    }

    public boolean isValidJarFile(String path) {
        if (isEmpty(path))
            return false;

        if (path.contains("/classes/") || path.contains("/.ade_path/") || path.contains(".ear/"))
            return false;
        if (path.endsWith("Test.jar"))
            return false;
        if (path.endsWith(".jar") && path.contains("/jlib/"))
            return true;

        return false;
    }

  
    public boolean isValidJpr(String path) {
        if (isEmpty(path))
            return false;

        if (path.contains("/classes/") || path.contains("/.ade_path/") || path.contains("/dbSchema/"))
            return false;
        if (path.endsWith("Test.jpr"))
            return false;
        if (path.endsWith(".jpr"))
            return true;

        return false;
    }

    public static boolean isEmpty(String str) {
        if (str == null || str.trim().equals(""))
            return true;
        return false;
    }
    
  private String getFileName(String filePath) {
      
    int index = 0;
    if (filePath.contains("fusionapps/")) {
            index = filePath.indexOf("fusionapps/");            
    }
    if (filePath.contains("fsm/")) {
            index = filePath.indexOf("fsm/");
    }
      if (filePath.contains("atgpf/")) {
              index = filePath.indexOf("atgpf/");
      }
      if(index > 0) {
        return filePath.substring(index);
      }
      return filePath;
  }

    public void addToJarMap(String prefix, String jarPath) {
        if (_jarMap.containsKey(prefix)) {
            HashSet<String> setJars = _jarMap.get(prefix);
            if (setJars.contains(jarPath))
                return;
            setJars.add(jarPath);
            _jarMap.put(prefix, setJars);
        } else {
            HashSet<String> setJars = new HashSet<String>();
            setJars.add(jarPath);
            _jarMap.put(prefix, setJars);
        }
    }
    
  public void logException(String info, Exception e) 
  {
      numberOfExceptions++;
      
      exceptions.append("\n@@ Exception # " +numberOfExceptions +"@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@\n");
      exceptions.append(info.toString() +"\n");
      exceptions.append(e.toString() +"\n");
      StackTraceElement[] element = e.getStackTrace();
      for(int i = 0; i < element.length; i++) {
          exceptions.append(element[i].toString() +"\n");
      }
  }
}
