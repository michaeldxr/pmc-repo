package oracle.apps.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.io.InputStreamReader;

import java.net.URL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.utility.LRGUtil;

public class FamilyModuleHelper {
  
    private  final static String statsDB = "jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan";
    private final static String statsUname = "codescan";
    private final static String statsPwd = "codescan"; 
    private static Pattern pRelease = Pattern.compile("<release>(.*?)</release>",Pattern.DOTALL);
    private static Pattern pReleaseNumber = Pattern.compile("\\d+\\.\\d+\\.(\\d+?)\\.");
    private static Pattern pReleaseNumberNew = Pattern.compile("\\d+\\.(\\d+)\\.");
    private static Pattern pReleaseNew = Pattern.compile("\"versionPolicy\":\"(.*?)\"",Pattern.DOTALL);
    private static HashMap<String,Integer> pathRelease = new HashMap<String,Integer>();
    private static HashMap<String,String> productAliases = null;
    
    public static void main(String[] args){
        System.out.println(getRelease("FUSIONAPPS_11.1.1.5.1_LINUX.X64"));
    }
    
    public static void buildProductAliases() {
        try{
            productAliases = new HashMap<String,String>();
            Connection con= DriverManager.getConnection(statsDB,"violation_reports","violation_report");
            Statement stmt = con.createStatement();            
            ResultSet rs = stmt.executeQuery("select family,product,productalias from productfamily");            
            while(rs.next())
                productAliases.put(rs.getString("FAMILY").toUpperCase()+"_"+rs.getString("PRODUCT"),rs.getString("PRODUCTALIAS"));
            
            if(rs != null)   rs.close();
            if(stmt != null) stmt.close();
            if(con != null) con.close();
            
        }catch(Exception e) {
            e.printStackTrace();
        }        
    }
    
    public static void printScanDuration(Date start, String scanName) {
        
        Date end = new Date();
        long elapsedTime = end.getTime() - start.getTime();
        System.out.println("Time taken to complete " + scanName + " scan: " + elapsedTime / 60000 + " minutes");
        
    }
    
    
    public static void generateDeferedPaths() {
        
        try{
            BufferedReader reader = new BufferedReader(new FileReader(getLabelServer("FATOOLS_MAIN_LINUX") + 
                                         "/fatools/opensource/jauditFixScripts/txt/Deferred_paths.txt"));
//            BufferedReader reader = new BufferedReader(new FileReader("/scratch/sudgupta/view_storage/sudgupta_fatools_may16_view/" + 
//                                         "/fatools/opensource/jauditFixScripts/txt/Deferred_paths.txt"));
            String line = "";
            while((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if(parts.length < 2)
                    continue;
                pathRelease.put(parts[1].trim(), Integer.parseInt(parts[0].trim()));
            }
            reader.close();
        }catch(Exception e) {
                e.printStackTrace();
        }
    }
    
    public static boolean isDeferredPath(int release, String path){
        if(!pathRelease.containsKey(path))
            return false;
        int deferTillRelease = pathRelease.get(path);
        if(release < deferTillRelease)
            return true;
        return false;
    }
    
    public static String getLabelServerTop1(String series){
        return getLabelServerTop(series);
    }
    
    public static String[] getFileList(String filename,String aderoot){
        String filelistpath;
        HashSet<String> transactionFiles = new HashSet<String>();
        if(!isaEmpty(aderoot)&&!isaEmpty(filename)){
                    filelistpath=aderoot+"/fusionapps/premerge/"+filename;
                    System.out.println("The path of the filelist file is "+filelistpath);
                    File fl=new File(filelistpath);
                    if(fl.exists()){
                            BufferedReader br;
                            try {
                                br = new BufferedReader(new FileReader(fl));
                                           String line = "";
                
                                while ((line = br.readLine()) != null) {
                                      line=line.trim();
                                      if (!isEmpty(line) && line.startsWith("fusionapps/"))
                                          transactionFiles.add(line.trim());
                                }
                            } catch (Exception e) {
                            }
                    }
                    
        }
        
        String[] files=transactionFiles.toArray(new String[transactionFiles.size()]);
        return files;
        
    }
    
    public static boolean isaEmpty(String str) {
        if (str == null || str.trim().equals("") || str.trim().equals("null")) {
            return true;
        }
        return false;
    }
    
    public static String findReleaseNumber(String series) {
        
        String url = "http://fabs.oraclecorp.com/fabs/rest/ro/version/"+series + "/";
        String urlContent = getUrlContent(url);
        if(urlContent == null)
            return null;
        String number = "";
        
        Matcher m = pReleaseNumberNew.matcher(urlContent.trim());
        if(m.find()){
            number = m.group(1);
            if(!number.equals("1"))
                return number;
            else{
                Matcher m2 = pReleaseNumber.matcher(urlContent.trim());
                if(m2.find())
                    return m2.group(1);
            }
        }
        return "";
    }
    
    public static String getRelease(String label) {
        try{
        
        if(label.contains("FSM"))
          return "13";
        String[] parts= label.split("X64");
        String series = label;
        if(parts.length > 1) series = parts[0]+"X64";
        
        String relnum = findReleaseNumber(series);
        if(!isEmpty(relnum))
            return relnum;
        String url = "http://fabs.oraclecorp.com/fabs/rest/ro/version/"+series+"/";
        String urlContent = getUrlContent(url);
        
        if(urlContent == null)
            return getReleaseOld(label);
        else{
            String rel[]=urlContent.split("\\.");
            if(rel.length>=2){
                return rel[1];
            }else{
                return getReleaseOld(label);
            }
        }
//        System.out.println(urlContent);
//        Matcher m = pReleaseNew.matcher(urlContent);
//        if(m.find()){
//            String release= m.group(1);
//            if(release == null || release.equals("null"))
//                return getReleaseOld(label);
//            Matcher m1= pReleaseNumberNew.matcher(release);
//            if(m1.find()){
//                String number = m1.group(1);
//                if(!number.equals("1"))
//                    return number;
//                else{
//                    Matcher m2 = pReleaseNumber.matcher(release);
//                    if(m2.find())
//                        return m2.group(1);
//                }
//            }
//        }
//        else 
//             return getReleaseOld(label);
//                
//        return "";
        
        }catch(Exception e) {
            e.printStackTrace();
            return getReleaseOld(label);
        }
    }
    
    public static String getReleaseOld(String label) {
        try{
          String series = label;
          String[] parts= label.split("X64");
          if(parts.length >= 1) 
            series = parts[0]+"X64";
        
        String url = "http://oracruise.oraclecorp.com/fusionapps/rest/projects/"+series+"/withRelease";
        String urlContent = getUrlContent(url);
        if(urlContent == null)
            return "";
        //System.out.println(urlContent);
        Matcher m = pRelease.matcher(urlContent);
        if(m.find()){
            String release= m.group(1);
            Matcher m1= pReleaseNumberNew.matcher(release);
            if(m1.find()){
                String number = m1.group(1);
                if(!number.equals("1"))
                    return number;
                else{
                    Matcher m2 = pReleaseNumber.matcher(release);
                    if(m2.find())
                        return m2.group(1);
                }
            }
        }
        return "";
        }catch(Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    public static String getUrlContent(String url) {
        try{
            URL u = new URL(url);
            BufferedReader reader = new BufferedReader(new InputStreamReader(u.openStream()));
            
            String line = "";
            StringBuffer result = new StringBuffer();
            while((line= reader.readLine()) != null)
                result.append(line);
            reader.close();
            return result.toString();
            
        }catch(Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    public static String getLabelServerTop(String series){
            
            String labelserver = getLabelServer(series);
            String[] parts = labelserver.split("/");
            if(parts.length>3)
                return "/" + parts[1]+"/"+parts[2];
            else 
                return "";
        }
    
    public static String getLabelServer(String series){
        
        String lServer = "FATOOLS_MAIN_LINUX".equals(series)? System.getenv("FAToolsMainLatest") : System.getenv(series);
        if (lServer != null && lServer.isEmpty() == false) return lServer;
        
        String command = "ade desc -l $(ade showlabels -series " + series + " -latest | tail -n 1) -labelserver";
        
        try{
            ProcessExecutor pe = new ProcessExecutor();
            ProcessBuilder pb = new ProcessBuilder("bash","-c",command);
            pe.runProcess(pb, "label.txt", false);
            String fileContents = FileParserHelper.getFileContentsAsString("label.txt").trim();
            System.out.println("GetLAbelServer(): " + fileContents);
            if (isEmpty(fileContents) || fileContents.contains("Could not describe label"))
                return getDefaultLabelServer(series);         
            else
                return fileContents;
        } catch (Exception e) {
            System.out.println("GetLabelServer(): "+e.getMessage());
            return getDefaultLabelServer(series);  
        }

    }
    
    private static boolean fileExists(String path) {
        File f = new File(path + "/fatools/opensource/jauditFixScripts/txt/tableCol_timestamps.txt");
        return f.exists();
    }
    
    private static String getDefaultLabelServer(String series){
        
        String path = LRGUtil.getLabelServerFromDB(series)+"/"+series+".rdd"+ "/LATEST";               
        if (fileExists(path))
             return path;
         else {
             path = "/ade_autofs/ud62_fa/" + series + ".rdd/LATEST/";
             if (fileExists(path))
                 return path;
             else {
                 path = "/ade_autofs/ud21_fa/" + series + ".rdd/LATEST/";
                 if (fileExists(path))
                    return path;
                 else {
                    System.out.println("GetDefaultLabelServer() failed, switch to $ADE_VIEW_ROOT");
                    return System.getenv("ADE_VIEW_ROOT");
                 }
             }
         }           
    }
    
    public static boolean isPathOfInterest(String sName)  {
        
        if(sName == null || sName.equals(""))
            return false;
        
        if (sName.contains(".ade_path") || sName.contains("/classes/"))
            return false;
        
         sName = sName.toLowerCase();
        
        if(sName.contains("/noship/") 
            || sName.contains("/test/")
            || sName.contains("servicetest")
            || sName.contains("datasecuritytest") 
            || sName.contains("securitypublictest")
            || sName.contains("uimodeltest")
            || sName.contains("structuretest")
            || sName.contains("modeltest")
            || sName.contains("uitest")
            || sName.contains("testui")
            || sName.contains("setest") 
            || sName.contains("testviewcontroller")
            || sName.contains("flextest")
            || sName.contains("uimodeler-launch-test")
            || sName.contains("publicuibasetest")
            || sName.contains("uipickertest")
            || sName.contains("launch-test")
            || sName.contains("basetest"))
            return false;
        
        return true;
    }


    public static String getFamily(String path) {
        if (isEmpty(path))
            return "";

        if (path.contains("/fsm")) {
            return "FSM";
        } else if (path.contains("/atgpf"))
            return "ATGPF";
        else if (path.contains("/fusionapps")) {
            String pathAfterFusionapps = getPathAfterFusionapps(path);
            if (isEmpty(pathAfterFusionapps))
                return "NA";
            String[] parts = pathAfterFusionapps.split("/");
            if (parts != null && parts.length > 0)
                return parts[0].toUpperCase().trim();
        } else if (path.contains("EMGC") || path.contains("/emgc") || path.contains("/emcore"))
            return "EMGC";

        return "";
    }

    public static String getPathAfterFusionapps(String path) {
        if (path.contains("/fusionapps")) {
            int indexFusionapps = path.indexOf("/fusionapps");
            if (indexFusionapps == -1)
                return "";
            try{
              return path.substring(indexFusionapps + "/fusionapps/".length());
            } catch(Exception e) {
                return "";
            }
        }
        return "";

    }

    public static String getRelativeFileName(String path) throws Exception {
        String fileNameInfo = getFileNameInfo(path);
        if (isEmpty(fileNameInfo))
            return "";
        String[] parts = fileNameInfo.split(",");
        if (parts.length < 4)
            return "";
        return parts[3].trim();
    }

    public static String getModule(String path)  
    {
        try
        {
        String fileNameInfo = getFileNameInfo(path);
        if (isEmpty(fileNameInfo))
            return "";
        String[] parts = fileNameInfo.split(",");
        if (parts.length < 2)
            return "";
        return parts[1].trim();
        }
        catch(Exception e)
        {
            return "";
        }
    }

    public static String getLabelInfo(String path) throws Exception {
        String fileNameInfo = getFileNameInfo(path);
        if (isEmpty(fileNameInfo))
            return "";
        String[] parts = fileNameInfo.split(",");
        if (parts.length < 5)
            return "";
        return parts[4].trim();
    }
    
    public static String getSeriesFromPath(String path) throws Exception{
        
        int i = -1;
        
        if(path.contains("/fusionapps")) 
         i = path.indexOf("/fusionapps");
        
        if(path.contains("/fsm"))
         i = path.indexOf("/fsm");
        
          if (path.contains("/atgpf")) 
              i = path.indexOf("/atgpf");
          
          if (path.contains("/emcore")) 
              i = path.indexOf("/emcore");
          
        if(i == -1)
            return "";
        
        String pathBeforeFusionapps = path.substring(0, i);
        
        String[] parts = pathBeforeFusionapps.split("/");
        if(parts==null || parts.length<2)
          return "";
        
        String series = parts[parts.length -2];
        series = series.replace(".rdd","");   
        
        return series;
        
    }

    public static String getProduct(String path) {
        if (isEmpty(path))
            return "";

        if (path.contains("/fsm")) {
            return "FSM";
        } else if (path.contains("/atgpf"))
            return "ATGPF";
        else if (path.contains("/fusionapps")) {
            String pathAfterFusionapps = getPathAfterFusionapps(path);
            if (isEmpty(pathAfterFusionapps))
                return "NA";
            String[] parts = pathAfterFusionapps.split("/");
            if (parts != null && parts.length > 3) {                
                // fusionapps/fin/components/ledger or fusionapps/fin/xla/db
                String shouldBeComponent = parts[2];
                String module = parts[3];
                if (shouldBeComponent != null && !shouldBeComponent.contains("component")) {
                    module = parts[2];
                }
                return module.toUpperCase().trim();
            }
        } else if(path.contains("/emcore"))
            return "EMCORE";
        else if(path.contains("/emgc"))
            return "EMGC";

        return "";
    }
    

    public static String getPlSqlProduct(String path) {
        if (isEmpty(path))
            return "";

        if (path.contains("/fsm")) {
            return "FSM";
        } else if (path.contains("/atgpf"))
            return "ATGPF";
        else if (path.contains("/fusionapps")) {
            String pathAfterFusionapps = getPathAfterFusionapps(path);
            if (isEmpty(pathAfterFusionapps))
                return "NA";
            String[] parts = pathAfterFusionapps.split("/");
            if (parts != null && parts.length > 2) {
                String plsqlProd = parts[1];

                if (!isEmpty(plsqlProd))
                    return plsqlProd.trim().toUpperCase();
                
                return getProduct(path);
            }
        }else if(path.contains("/emcore"))
            return "EMCORE";
        else if(path.contains("/emgc"))
            return "EMGC";

        return "";
    }
    public static boolean isEmpty(String path) {
        if (path == null || path.trim().equals(""))
            return true;
        return false;
    }

    public static Integer getIndexOfFusionapps(String absPath) {
        int index = -1;
        try {
            File f = new File(absPath);
            absPath = f.getCanonicalPath();

            if (absPath.contains("/fusionapps")) {
                index = absPath.indexOf("/fusionapps");
            }else if (absPath.contains("/fsm")) {
                index = absPath.indexOf("/fsm");
            } else if (absPath.contains("/atgpf")) {
                index = absPath.indexOf("/atgpf");

            } else if (absPath.contains("/emgc")) {
                index = absPath.indexOf("/emgc");

            } else if (absPath.contains("/emcore")) {
                index = absPath.indexOf("/emcore");

            }
        } catch (Exception ex) {
        }
        return index+1;
    }
    
    public static String getPathAfterViewRoot(String absPath) {
        int index = -1;
        try {

            if (absPath.contains("/fusionapps")) 
                index = absPath.indexOf("/fusionapps");
             else if (absPath.contains("/fsm")) 
                index = absPath.indexOf("/fsm");
            else  if (absPath.contains("/atgpf")) 
                index = absPath.indexOf("/atgpf"); 
            else  if (absPath.contains("/emgc")) 
                index = absPath.indexOf("/emgc"); 
            else  if (absPath.contains("/emcore")) 
                index = absPath.indexOf("/emcore"); 
        } catch (Exception ex) {
        }
        return absPath.substring(index+1);
    }
    
    public static String getSeedFileNameInfo(String absPath) {
        int i = 0;
        String family = "";
          String product = "";
          
        File f = new File(absPath);
        try{
          absPath = f.getCanonicalPath();
        }catch (Exception e) { 
            e.printStackTrace();
        }
        
        String blankString =  "NA,NA,NA,NA,NA,";
        if(isEmpty(absPath))
            return blankString;
        if(absPath.startsWith("/ade/"))
            return blankString;
        
        if(absPath.contains("fusionapps/")) {
         i = absPath.indexOf("fusionapps/");
        }
        if(absPath.contains("fsm/")){
         i = absPath.indexOf("fsm/");
         family = "FSM";
        }
          if (absPath.contains("atgpf/")) {
              i = absPath.indexOf("atgpf/");
              family = "ATGPF";
          }
          
          if (absPath.contains("emcore/")) {
              i = absPath.indexOf("emcore/");
              family = "EMGC";
              product="EMCORE";
          }
          
        String pathAfterFusionApps = absPath.substring(i);
        String[] parts = pathAfterFusionApps.split("/");
        
        
        if(parts == null) return blankString;
        
        if(absPath.contains("fusionapps/")){
            if(parts.length > 2)
              product = parts[2].trim().toUpperCase();
            if(parts.length > 1)
              family = parts[1].trim().toUpperCase();
        }
        if(absPath.contains("fsm/") && parts.length > 1)
            product = parts[1].trim().toUpperCase();
        
          if(absPath.contains("atgpf/") && parts.length > 1)
              product = parts[1].trim().toUpperCase();
          
        String path_before_fusionapps = absPath.substring(0, i-1);
        parts = path_before_fusionapps.split("/");
        
        String label = getLabel(absPath);
        
        if(i<1)
           return "," + family + ",DB," + product + "," + pathAfterFusionApps + "," + label + ",";
        
        if(parts==null || parts.length<2)
          return blankString;
        
        return family + ",DB," + product + "," + pathAfterFusionApps + "," + label + ",";
    }

    public static String getFileNameInfo(String absPath) throws IOException 
    {
        //System.out.println("filennameInfo: " + absPath);
        File f = new File(absPath);
        try{
          absPath = f.getCanonicalPath();
        }catch (Exception e) { 
            e.printStackTrace();
        }
        int i = -1;
        String family = "";
        String module = " ";
        String product = " ";
        String blankString = "NA,NA,NA,NA,NA,";
        if (isEmpty(absPath))
            return blankString;

        if (absPath.contains("/fusionapps")) {
            i = absPath.indexOf("/fusionapps");
        }
        if (absPath.contains("/fsm")) {
            i = absPath.indexOf("/fsm");
            family = "FSM";
        }
        if (absPath.contains("/atgpf")) {
            i = absPath.indexOf("/atgpf");
            family = "ATGPF";
        }
        if (absPath.contains("/emcore")) {
            i = absPath.indexOf("/emcore");
            family = "EMGC";
        }
        if (i < 0)
            return blankString;
        
        String pathAfterFusionApps = absPath.substring(i+1);
        String[] parts = pathAfterFusionApps.split("/");       

        if (parts == null)
            return blankString;

        if (absPath.contains("/fusionapps")) {
            int partsLength = parts.length;
            if (partsLength > 1)
                family = parts[1].trim().toUpperCase();
            if (partsLength > 3) {
                module = parts[3].trim();
            }
            if (partsLength > 4)
                product = parts[4].trim().toUpperCase();
            if (partsLength > 3) {
                // fusionapps/fin/components/ledger or fusionapps/fin/xla/db
                String shouldBeComponent = parts[2];

                if (shouldBeComponent != null && !shouldBeComponent.contains("component")) {

                    product = parts[2];
                    module = parts[3];
                }
            }

        }
        if (absPath.contains("/fsm")) {
            int partsLength = parts.length;
            if (partsLength > 2)
                module = parts[2].trim();
            if (partsLength > 3)
                product = parts[3].trim().toUpperCase();
        }
        if (absPath.contains("/atgpf")) {
            int partsLength = parts.length;
            if (partsLength > 3)
                module = parts[3].trim();
            if (partsLength > 2)
                product = parts[2].trim().toUpperCase();
        }
        
        if (absPath.contains("/emcore")) {
            int partsLength = parts.length;
            if (partsLength > 0)
                product = parts[0].trim();
        }
        
        if(productAliases == null)
            buildProductAliases();
        
        if(productAliases != null && productAliases.containsKey(family+"_"+product))
            product=productAliases.get(family+"_"+product);


        String path_before_fusionapps = absPath.substring(0, i);
        parts = path_before_fusionapps.split("/");
        if (parts == null || parts.length < 2)
            return family + "," + module + "," + product + "," + pathAfterFusionApps + ", ,";

        String label = parts[parts.length - 2] + "_" + parts[parts.length - 1];
        label = label.replace(".rdd", "");

        return family + "," + module + "," + product + "," + pathAfterFusionApps + "," + label + ",";
    }

    public static String getFileNameFromPath(String path) {
        if (isEmpty(path))
            return "";
        int indexExtension = path.lastIndexOf(".");
        if (indexExtension == -1)
            indexExtension = path.length() - 1;
        int fileNameStart = path.lastIndexOf("/");

        return path.substring(fileNameStart + 1, indexExtension);
    }

    /**
     * View Root for path like:
     * /ade/zcurrimb_rup3csindex/fusionapps/fin = /ade/zcurrimb_rup3csindex
     * @param path
     * @return
     */
    public static String getViewRootFromPath(String path) {
        String viewRoot = "";
        try {
            File f = new File(path);
            String absPath = f.getCanonicalPath();
            int fusionappsIndex = getIndexOfFusionapps(absPath);
            if (fusionappsIndex < 0)
                return viewRoot;

            return absPath.substring(0, fusionappsIndex);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return viewRoot;
    }
    
    public static String getRowClassImpl(String voFile, String rowClassName) {
        int index = rowClassName.lastIndexOf('.') +1;
        String className = rowClassName.substring(index);
        
        index = voFile.lastIndexOf('/') +1;
        String path = voFile.substring(0, index);
        
        path = path + className +".java";
        
        File fFile = new File(path);
        if(!fFile.exists()) return null;
        else return fFile.getAbsolutePath();
    }
    
    public static String getSeries(String codeline) {
        if(codeline.equals("V2"))
            return "('FUSIONAPPS_MAIN_LINUX.X64','FSM_MAIN_GENERIC')";
        if(codeline.equals("REL8"))
            return "('FUSIONAPPS_PT.R8INT_LINUX.X64')";
        if(codeline.equals("REL7"))
            return "('FUSIONAPPS_11.1.1.5.1_LINUX.X64','FSM_11.1.1.7.0_GENERIC')";
        if(codeline.equals("R8XFAM"))
            return "('FUSIONAPPS_PT.R8XFAM_LINUX.X64')";
        if(codeline.equals("DEVMAIN"))
            return "('FUSIONAPPS_PT.DEVMAIN_LINUX.X64')";
        if(codeline.equals("INTERP"))
            return "('FUSIONAPPS_PT.INTERP_LINUX.X64')";
        if(codeline.equals("INTERPDEV"))
            return "('FUSIONAPPS_PT.INTERPFIN_LINUX.X64','FUSIONAPPS_PT.INTERPPRC_LINUX.X64','FUSIONAPPS_PT.INTERPPRJ_LINUX.X64',"+
            "'FUSIONAPPS_PT.INTERPSCM_LINUX.X64')";   
        if(codeline.equals("LRBDEV"))
            return "('FUSIONAPPS_PT.DEVATF_LINUX.X64','FUSIONAPPS_PT.DEVCOM_LINUX.X64','FUSIONAPPS_PT.DEVCRMCUR_LINUX.X64'," +
                "'FUSIONAPPS_PT.DEVFIN_LINUX.X64', 'FUSIONAPPS_PT.DEVFSCM_LINUX.X64','FUSIONAPPS_PT.DEVHCM_LINUX.X64',"+
                "'FUSIONAPPS_PT.DEVIC_LINUX.X64', 'FUSIONAPPS_PT.DEVSCM_LINUX.X64','FUSIONAPPS_PT.DEVPRJ_LINUX.X64')";
        if(codeline.equals("R8DEV"))
            return "('FUSIONAPPS_PT.R8ACL_LINUX.X64','FUSIONAPPS_PT.R8ATF_LINUX.X64','FUSIONAPPS_PT.R8FIN_LINUX.X64','FUSIONAPPS_PT.R8HCM_LINUX.X64'," +
                "'FUSIONAPPS_PT.R8IC_LINUX.X64', 'FUSIONAPPS_PT.R8PRC_LINUX.X64','FUSIONAPPS_PT.R8PRJ_LINUX.X64',"+
                "'FUSIONAPPS_PT.R8SCM_LINUX.X64')";
        if(codeline.equals("LRBDEVNEXT"))
            return "('FUSIONAPPS_PT.DEVSCMNXT_LINUX.X64','FUSIONAPPS_PT.DEVHCMNXT_LINUX.X64')";          
        return null;
    }
    
    public static String getLabel(String filePath) {

      try{
           File f = new File(filePath);
           filePath = f.getCanonicalPath();
      } catch(Exception e) {
           e.printStackTrace();
      }
      int index = -1;
      if (filePath.contains("fusionapps/")) {
              index = filePath.indexOf("fusionapps/");            
      }
      if (filePath.contains("fsm/")) {
              index = filePath.indexOf("fsm/");
      }
        if (filePath.contains("atgpf/")) {
                index = filePath.indexOf("atgpf/");
        }
        if (filePath.contains("emcore/")) {
                index = filePath.indexOf("emcore/");
        }
      
        if(index < 1) return "";
        String path_before_fusionapps = filePath.substring(0, index-1);
        String[] parts = path_before_fusionapps.split("/");
        if(parts == null || parts.length < 2)
            return "";
        String label = parts[parts.length -2] + "_" + parts[parts.length -1];      
        label = label.replace(".rdd","");   
        return label;
    }
    
    public static String[] getLabelSeries(String seriesLabel) {
        int index = seriesLabel.indexOf("X64");
        if(index != -1) {          
        
            String series = seriesLabel.substring(0, index+3);
            String label = seriesLabel.substring(index+4);
            
            String[] result = new String[2];
            result[0] = series;
            result[1] = label;
            
            return result;
        } else if(seriesLabel.contains("FSM")) {
            index = seriesLabel.indexOf("GENERIC");
            
            if(index != -1) {          
            
                String series = seriesLabel.substring(0, index+7);
                String label = seriesLabel.substring(index+8);
                
                String[] result = new String[2];
                result[0] = series;
                result[1] = label;
                
                return result;
            }             
        }
        return new String[]{"",""};
    }
    
    public static String getFileNameInfo1(String absPath) throws IOException {
        
      File f = new File(absPath);           
      absPath = f.getCanonicalPath();
      int i = 0;
      String family = "";
      String module = "";
      String product = "";
      String blankString =  "NA,NA,NA,NA,NA,NA,";
      if(isEmpty(absPath))
          return blankString;
      
      if(absPath.contains("fusionapps/")) {
       i = absPath.indexOf("fusionapps/");
      }
      else if(absPath.contains("fsm/")){
       i = absPath.indexOf("fsm/");
       family = "FSM";
      }
      else  if (absPath.contains("atgpf/")) {
            i = absPath.indexOf("atgpf/");
            family = "ATGPF";
        }
       else if (absPath.contains("emcore/")) {
            i = absPath.indexOf("emcore/");
            family = "EMGC";
            module = "EMCORE";
        }
        
      String pathAfterFusionApps = absPath.substring(i);
      String[] parts = pathAfterFusionApps.split("/");
      
      if(parts == null) return blankString;
      
      if(absPath.contains("fusionapps/")) {
          int partsLength = parts.length;
          if(partsLength>1)
            family = parts[1].trim().toUpperCase();  
          if(partsLength>3)
            module = parts[3].trim();
          if(partsLength>4)
            product = parts[4].trim().toUpperCase();
          
          if(product.equals(".ADF")){
              if(module.equals("payables"))
                  product="AP";
              else if(module.equals("ledger"))
                  product="GL";
          }          
      }
      else if(absPath.contains("fsm/")) {
        int partsLength = parts.length;
        if(partsLength>2)
          module = parts[2].trim();
        if(partsLength>3)
          product = parts[3].trim().toUpperCase();
      }
      
      else  if (absPath.contains("atgpf/")) {
            int partsLength = parts.length;
            if (partsLength > 1)
                module = parts[1].trim();
            if (partsLength > 3)
                product = parts[3].trim().toUpperCase();
        }
       else  if (absPath.contains("emgc/")) {
            int partsLength = parts.length;
            if (partsLength > 1)
                product = parts[1].trim();
        }
        if(productAliases == null)
            buildProductAliases();
        
        if(productAliases !=null && productAliases.containsKey(family+"_"+product))
            product=productAliases.get(family+"_"+product);
         
      if(i<1)
         return family + "," + module + "," + product + "," + pathAfterFusionApps + ", , ," ;
      
      String path_before_fusionapps = absPath.substring(0, i-1);
      parts = path_before_fusionapps.split("/");
      if(parts==null || parts.length<2)
        return blankString;
     
      String series = parts[parts.length -2];
      series = series.replace(".rdd","");   
      String label = parts[parts.length -1]; 
      
      return family + "," + module + "," + product + "," + pathAfterFusionApps + "," + series + "," + label + ",";
    }
}
