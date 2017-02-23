package oracle.apps.di;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;

import java.io.InputStream;

import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import java.util.zip.ZipInputStream;

import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.XMLDocument;
//TODO: Su: This version doesnt work..use AdfDiWebPickers
//This is just for internal testing purpose..(to see contents of the spreadsheet)

public class AdfDiWebPicker_v2 extends JoesBaseClass {
    
    private static String sCrawlDir = "";
   
    public AdfDiWebPicker_v2() {
        super(JoesBaseClass.CRAWL_TYPE.XLS);
    }
    
    public static void main(String[] args) throws Exception {
        
        
        AdfDiWebPicker_v2 scanner = new AdfDiWebPicker_v2();
//        sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_PT.R9INT_LINUX.X64.rdd/LATEST/" +
//            "fusionapps/fin/components/ledger/gl/journals/desktopEntry";
//        scanner.crawlDirectory(sCrawlDir,false);
          //scanner.processFile(new File("/scratch/sudgupta/Desktop/CreateQuickInvoices.xlsx"),false);
          scanner.processFile(new File("/scratch/sudgupta/Desktop/NewCreateQuickInvoices.xlsx"),false);
        
    }

    public void processFile(File fName, boolean bDoADE) {
        
        String filepath = fName.getAbsolutePath(); 
        System.out.println(filepath);
        try {
            
//            ZipInputStream zipStream = new ZipInputStream(new FileInputStream(filepath));
//            
//            if (zipStream == null) {
//                System.err.println("[ERROR] Could not open zip stream");
//                return;
//            }
            ZipFile spreadsheetZip = new ZipFile(filepath);
            ZipEntry sharedStringsXmlEntry = spreadsheetZip.getEntry("xl/sharedStrings.xml");
            
            if (sharedStringsXmlEntry == null)
                throw new Exception("Could not find any shared strings in file: " + filepath +
                                    ". Either the file is not a DI spreadshseet, or the file does not contain any shared strings");
            
            BufferedWriter writer = new BufferedWriter(new FileWriter("excel2.txt"));
            
            /*InputStream sharedStringsIn = spreadsheetZip.getInputStream(sharedStringsXmlEntry);
            BufferedReader reader = new BufferedReader(new InputStreamReader(sharedStringsIn));

            String line="";
            while((line = reader.readLine()) != null){
                writer.write(line+"\n");
            }
            reader.close();
            writer.close();*/
            
            ArrayList<String> excelStrings = SharedStringsParser.getListOfAllStrings(filepath,false);
            for(int i = 0; i < excelStrings.size();i++)
                writer.write(excelStrings.get(i) + "\n");
            
            writer.close();
            
            ArrayList<String> excelMetaStrings = SharedStringsParser.getListOfAllADFDiMetaStrings_New(excelStrings,filepath);
            writer = new BufferedWriter(new FileWriter("excelMeta2.txt"));
            for(int i = 0; i < excelMetaStrings.size();i++)
                writer.write(excelMetaStrings.get(i) + "\n");
            
            writer.close();
            
           // XMLDocument doc = XMLParserHelper.getXMLDocument(sharedStringsIn);
            /*while(true){
                
                ZipEntry ze = zipStream.getNextEntry();
                if (ze == null) 
                    break;
                
                String strEntryName = ze.getName().trim();
                System.out.println(strEntryName);
            }*/
            
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return null;
    }
}
