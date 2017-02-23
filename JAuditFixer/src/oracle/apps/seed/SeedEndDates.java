package oracle.apps.seed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.sqlanalyzer.AutoExecuteVCScanLRG;
import oracle.apps.sqlanalyzer.BindVariableKinds;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.utility.LRGUtil;

public class SeedEndDates extends JoesBaseClass{
    
    static BufferedWriter writer;
    Pattern p = Pattern.compile("<\\w+?>(.*?)</\\w+?>");
    HashSet<String> exemptions = new HashSet<String>();
    private static String mode="view";// can be LRG/view/premerge
    private static final String issuetype="SeedEndDateIncorrect";
    
  public SeedEndDates() 
  {
      super(JoesBaseClass.CRAWL_TYPE.SEED);
  }
       
    public static void main(String[] args) {
        
        try{
            SeedEndDates scanner = new SeedEndDates();
            scanner.generateExemptionList();
            writer = new BufferedWriter(new FileWriter("SeedEndDateIncorrect.csv") );        
            writer.write("Family,Module,Product,FileName,Series_Label,Subissue, Description\n"); 
            
            mode = System.getProperty("mode");
            if(mode == null)
                mode = "view";
          //mode = "LRG";
            
            Date start = new Date();
            System.out.println("Starting SeedEndDatesScan at: " + start);
            
            if(mode.equals("view")) {
            
                String sCrawlDir = args[0];
               // String sCrawlDir="/ade_autofs/ud21_fa/FUSIONAPPS_PT.R8INT_LINUX.X64.rdd/LATEST/fusionapps/fin/";
                System.out.println("Analyzing all files in directory (Seed end dates check):" + sCrawlDir);      
                scanner.crawlDirectory(sCrawlDir, false); 
            } else if(mode.equals("LRG")) {
                
                String series = args[0];
                //String series = "FSM_MAIN_GENERIC";
                String label = LRGUtil.getLatestLabel(series);
                System.out.println("Running " + issuetype + " Scan on series: " + series + ".......");
                System.out.println("Latest label is: " + label);
                
                ArrayList<String> families = LRGUtil.getFamiliesFromSeries(series);
              String serverTop = FamilyModuleHelper.getLabelServerTop1(series);
              String viewRoot = serverTop + "/"+series+".rdd/"+label + "/";
                
                
                for (int j = 0; j < families.size(); j++) {
                    
                    String family = families.get(j);
                    String sCrawlDir = LRGUtil.getCrawlDirNew(series, family, issuetype);     
                    System.out.println("Crawling dir '" + viewRoot + sCrawlDir + "' for family: " + family);
                    scanner.crawlDirectoryNew(viewRoot,sCrawlDir,false);           
                    System.out.println("Done crawling for family "+family+".");
                }                
                LRGUtil.addLabelInfoToAB(issuetype, series, label);
                FamilyModuleHelper.printScanDuration(start,issuetype);
                
            } else if(mode.equals("premerge")) {
            
                //String[] files = args[0].trim().split("\n");
                String ade_view_root = args[1].trim();
                String filelistpath=args[0].trim();
                String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root); 
                String label = args[2].trim();
                
                String release= FamilyModuleHelper.getRelease(label);
                System.out.println("Release: " + release);
                
                try{              
                    int releaseNum = Integer.parseInt(release);
                    if (releaseNum < 9) //This check was mandated post release 9 only, hence this check is necessary to avoid the current rel7 and rel8 branches from failing
                        return;              
                }catch(NumberFormatException e){
                    System.out.println("Could not determine release. Skipping SeedEndDates check...");
                    return;
                }
                for(int i = 0; i < files.length; i++) {
                    
                    String filePath = ade_view_root + "/" + files[i].trim();
                    File f = new File(filePath);
                    if(!f.exists())
                        continue;
                    if(!scanner.fileOfInterest(filePath))
                        continue;
                    scanner.processFile(f,false);
                }
                writer.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/SeedDataEndDates " +
                    "for description of the issue and resolution.\n\n");
                writer.close();   
                
            } else {
                System.out.println("Unrecognized mode of operation: " + mode);
                System.out.println("Mode must be view/LRG/premerge");
                System.exit(1);
            }          
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    
   
    
    protected void processFile(File f, boolean doADE) {
        try{
            BufferedReader reader = new BufferedReader(new FileReader(f));
            String line = "";
            int lineNo = 0;
            String violation = "";
            String violation1="";
            int count_4712 =0;
            String subissue="end date may be incorrect";
            boolean isEnabled=true;
            
            while((line = reader.readLine()) != null) {
                lineNo++;
                line = line.trim().toLowerCase();
                if(line.contains("rowkey")){
                    if(isEnabled && !violation1.equals(""))
                        violation += violation1;
                    
                    isEnabled=true; //reset
                    violation1="";
                }
                
                if(line.contains("enabledflag")){
                    Matcher m = p.matcher(line);
                    if(m.find()){
                        String flag = m.group(1).trim();
                        if(flag.equals("n"))
                            isEnabled=false;
                    }
                }
                if(line.contains("enddate") && !line.contains("isnull")){
//                    if(line.contains("effectiveenddate"))
//                        subissue="effective end date may be incorrect";
                    Matcher m = p.matcher(line);
                    if(m.find()) {
                        String date = m.group(1).trim();
                        if(date.length() < 4)
                            continue;
                        String year = date.substring(0, 4);
                        try{
                            int y = Integer.parseInt(year);
                            if(y == 4712) {
                                count_4712++;
                                continue;
                            }
//                            if(y > 2049 || y < 1950)
//                                subissue = "date is outside 1950-2049 range";
                            
                            violation1 += "#" + lineNo + ":" + date + "; ";
//                            if(y != 2048)
//                                violation += "#" + lineNo + ":" + date + "; ";
//                            else
//                                subissue = "date is 2048-12-31";
                        } catch(NumberFormatException ex) {
                            //System.out.println("Weird date format: " + fName + "," + lineNo);
                            continue;
                        }
                    }                        
                }        
            }//while
            
            //process the last record
            if(isEnabled && !violation1.equals(""))
                violation += violation1;
            
            if(exemptions.contains(FamilyModuleHelper.getPathAfterViewRoot(f.getAbsolutePath())))
                return;
            if(!violation.equals(""))
                writer.write(FamilyModuleHelper.getSeedFileNameInfo(f.getAbsolutePath()) +subissue + "," + violation + "\n");
//            if(count_4712>0)
//                writer.write(FamilyModuleHelper.getSeedFileNameInfo(fName) +"Seed file used 4712 as end dates," + count_4712 + "\n");
            
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public void generateExemptionList() {
        
    try{
      BufferedReader fileReader = new BufferedReader(new FileReader(FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
          "/fatools/opensource/jauditFixScripts/txt/angrybirdCodescanExemptions_new.txt"));
      String line = null;
      
      while((line = fileReader.readLine()) != null) {
        String[] parts = line.split(",");
        if(parts.length < 2)
            continue;
        String issue = parts[0].trim();
        if(!issue.equals("SeedDataEndDates"))
            continue;
        String fileName = parts[1].trim();
          
        exemptions.add(fileName);
      }
    }catch(Exception e) {
        System.out.println("Skipping exemptions as file could not be found: " + e.getMessage());
    }
 }

  protected String getSummaryReport() {
    return null;
  }

  protected String getSummaryReportSubject() {
    return null;
  }
}
