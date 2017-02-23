package oracle.apps.di.checker;

import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import java.io.FileWriter;
import java.io.IOException;

import java.io.InputStreamReader;

import java.net.URL;

import java.net.URLConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
//import java.util.Set;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.di.ADFDiWorkbook;
import oracle.apps.di.CheckerConstants;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.utility.JoesBaseClass;
import oracle.apps.utility.LRGUtil;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * 
 * Purpose: Check the jspx page related to the spreadsheet if set "ClientComponent" property as true
 * 
 * Step:
 *    1. get JSPX page from spreadsheet shareString
 *    2. According to a rule to get the jspx absolute path
 *    3. Read the jspx adf component, check if it includes clientcomponent=true
 *    
 * Related Setting file:
 *    1. config/Rule-R13.xml. Set the check ID and regist the check into framework
 *    2. Resources/adfFacesComponent.config include all adf component need be searched. 
 *    3. Resources/specialJspxPath.properties. If jspx path is not the path we often use, can set them in it. 
 *    
 */

public class ClientCompoentChecker  extends DIGenericStandardsChecker {
    static boolean writeExceptions = false;
    private Set<String> ADF_FACES_COMPONENT;
//    private final String CLIENT_COMPONENT_HTML = " clientComponent=\"true\"";
    //private final String NO_CLIENT_COMPONENT_HTML = " clientComponent=\"false\"";
//    private final String ADF_FACES_COMPONNET_PATH = FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
//    "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/di/config/adfFacesComponent.properties";; //"/adfFacesComponent.properties";
//    private final String SPECIAL_JSPX_PATH = FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
//    "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/di/config/specialJspxPath.properties"; //"/specialJspxPath.properties";
    private String ADF_FACES_COMPONNET_PATH = FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
    "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/di/config/adfFacesComponent.properties";; //"/adfFacesComponent.properties";
   private String SPECIAL_JSPX_PATH = FamilyModuleHelper.getLabelServer("FATOOLS_MAIN_LINUX") +
    "/fatools/opensource/jauditFixScripts/JAuditFixer/src/oracle/apps/di/config/specialJspxPath.properties"; //"/specialJspxPath.properties";
    private static final String BLANK = "";
    private String xlsxPath = ""; 
    private static final int INT_ZERO = 0;  
    private final String FUSION_COMPONENT_PATH = "/fusionapps/";
    private String code = "C_2", issue = CheckerConstants.ISSUE_CLIENT_COMPONENT;  
         
    public ClientCompoentChecker() {
        super();
    }
    
    public void setxlsxPath( String path ){
        xlsxPath = path;
    }
    
    public String getxlsxPath(  ){
        return xlsxPath;
    }
    
    
    public static void main(String[] args) throws Exception {
          
          List<CheckerResult> results = new ArrayList<CheckerResult>();  
          ADFDiWorkbook workbook = null;
          ClientCompoentChecker check = new ClientCompoentChecker();
          workbook = ADFDiWorkbook.getDIWorkbook(check.getxlsxPath(), writeExceptions);
          ArrayList<String> list =  workbook.getSharedStrings();
          try{
              
              results = check.doCheck( workbook ,false);
             
          }catch( Exception e ){
              System.out.println(e.getMessage());
          }          

      }
    
    @Override
    protected ArrayList<CheckerResult> doCheck(ADFDiWorkbook workbook, boolean getOnlyFailedResults) throws Exception {
        ArrayList<CheckerResult> list = new ArrayList<CheckerResult>();
         
        setxlsxPath( workbook.getSpreadsheetPath() );
        ADF_FACES_COMPONENT = loadConfigurations(ADF_FACES_COMPONNET_PATH);
        List<String> listJspx =  getJspxFromXlsm(workbook); 
        List<String> listJspxPath =  outputWebPickerPagePathInfo( listJspx );
        for ( int i = 0 ; i < listJspxPath.size(); i ++  ){
            searchClientComponentInJspx( listJspxPath.get(i), list ); 
        }
        
        return list;
        
    }
    
    
    
    
    /**
     * check "clientComponent=true" to all the ADF faces component in the jspx
     * 
     * @param fileName,
     *            jspx file absolute path
     * @param flist
     *            Collection of search result           
     * @return boolean If any one dont find component equal false
     */
    private boolean searchClientComponentInJspx(String fileName, List<CheckerResult> list ) {
        String subIssue = "\"ClientComponent\" property must be set to True for any ADF Faces component targeted to receive headless clicks or headless text entries.";
     
        if(!fileName.endsWith(".jsff") && !fileName.endsWith(".jspx")) 
            return true;
        
        try{
            XMLDocument doc = XMLParserHelper.getXMLDocument(fileName, false);
            if(doc == null){
                return true;
            }
            
                
            NodeList adfc = doc.getElementsByTagName("jsp:root");
            if (adfc == null) 
            {
                System.out.println("REVIEW:  Not a page file ---> " + fileName);
                return true;
            }

            NodeList listNode = doc.getElementsByTagNameNS("*","*");

                        
            for(int i = 0; i < listNode.getLength(); i++) {
                
                Node child = listNode.item(i);
                String nodeName = child.getNodeName();
                if ( ADF_FACES_COMPONENT.contains(nodeName) ){
                    String strAttrValue = XMLParserHelper.getAttributeValue(child, "clientComponent");
                    if ( strAttrValue == null || !strAttrValue.equalsIgnoreCase("true") ){
                        int fileStartPos = fileName.lastIndexOf("/");
                        String jspxFileName = fileName.substring( fileStartPos + 1 );
                        list.add(new CheckerResult(code, issue, subIssue, String.format(" Page: %s There are still some ADF Faces components whose \"ClientComponent\" property was not set to True", jspxFileName  ),
                                                      false));
                        return false;
                    }
                }

            }
            
        } catch(Exception e) {
            e.printStackTrace();
        }
        return true;
    }


             

    /**
     * output web picker page path infomation to file(webpickerPagePath.log)
     * 
     * @param contents
     *            list of jspx page relative path
     * @return set of jspx page absolute path
     */
    private List<String> outputWebPickerPagePathInfo(List<String> contents) {
            // split public html dir path
        
            List<String> jspxPathList = new ArrayList<String>();
            
            String _xlsxPath = xlsxPath.replace("/./", "/");
            String publicHtmlPath = BLANK;
            String publicHtml = "/public_html/";
            int publicHtmlIndex = _xlsxPath.indexOf(publicHtml);
            String src = "/src/";
            int srcIndex = _xlsxPath.indexOf(src);
            if (publicHtmlIndex > 0) {
                    publicHtmlPath = _xlsxPath.substring(0, publicHtmlIndex + publicHtml.length());
            } else if (srcIndex > 0) {
                    publicHtmlPath = _xlsxPath.substring(0, srcIndex + src.length()).replace(src, publicHtml);
            }

            BufferedWriter writer = null;
            Set<String> pagePathSet = new HashSet<String>();
            try {

                    String pagePath = null;
                    String faces = "/faces/";
                    Map<String, String> specialPagePath = loadProperties( SPECIAL_JSPX_PATH );
                        
                    //for (String c : contents) {
                    for ( int i = 0; i < contents.size(); i ++ ){
                        String c = contents.get( i );
                        if (c != null &&  !c.substring(0,1).equalsIgnoreCase("/")  ) c = "/" + c;
                        
                            if (c != null && c.indexOf(faces) >= INT_ZERO) {
                                if (specialPagePath.containsKey(c)) {
                                     pagePath = publicHtmlPath.substring(0, publicHtmlPath.indexOf(FUSION_COMPONENT_PATH))
                                                            + specialPagePath.get(c);
                                } else {
                                     pagePath = publicHtmlPath + c.substring(faces.length(), c.length());
                                }
                                jspxPathList.add( pagePath  );

                            }
                        
                    }
                    return jspxPathList;
            } catch (Exception e) {
                    throw new RuntimeException(e);
            } 
    }

    /**
     * load properties form properties file
     * 
     * @param path
     *            the properties file path 
     * @return key/value map
     */
    private  Map<String, String> loadProperties(String filePath) {
            
       
            Map<String, String> definitions = new HashMap<String, String>();
            BufferedReader reader = null;
            try {
                File f = new File( filePath );
                if (f == null || !f.exists())
                  return definitions ;

                   reader = new BufferedReader( new FileReader(f) );
                
                        String definition = null;
                        while ((definition = reader.readLine()) != null) {
                                definition = definition.trim();
                                if (!definition.startsWith("#")) {
                                        String[] prop = definition.split("=");
                                        String value = null;
                                        if (prop.length == 1) {
                                                value = "";
                                        } else if (prop.length == 2) {
                                                value = prop[1];
                                        } else {
                                                throw new RuntimeException("Unexpected definition:" + definition);
                                        }
                                        definitions.put(prop[0], value);
                                }
                        }
//                    } else {
//                            throw new RuntimeException( filePath + "file is not in the same directory with FDIJspxConverter.sh");
//                    }
            } catch (IOException e) {
                    throw new RuntimeException(e);
            } finally {
                    try {
                            if (reader != null) {
                                    reader.close();
                            }
                    } catch (IOException e) {
                            reader = null;
                            throw new RuntimeException(e);
                    }
            }
            return definitions;
    }   
    
    /**
     * load configurations from file,one line one value in the configuration
     * file
     * 
     * @param filePath
     *            configuration file absolute path
     * @return set of configuration
     */
    private Set<String> loadConfigurations(String filePath) {
        
        Set<String> configs = new HashSet<String>();
        BufferedReader reader = null;
//        URL pathURL = this.getClass().getResource(  filePath ) ; 
        

        File f = new File( filePath );
        if (f == null || !f.exists())
          return configs ;
        
            try {
//                reader = new BufferedReader( new InputStreamReader(  pathURL.openStream()));
                reader = new BufferedReader( new FileReader(f) );
                String line = "";
                while (line != null)
                {
                    line = reader.readLine();
                    if ( line != null && !BLANK.equals(line.trim()) && !line.trim().startsWith("#")) {
                        configs.add(line);
                    }
                }
                
            } catch (IOException e) {
                    e.printStackTrace();
            } finally {
                    if (reader != null) {
                            try {
                                   reader.close();
                            } catch (IOException e1) {
                                    reader = null;
                                    e1.printStackTrace();
                            }
                    }
            }
            return configs;
    }
    


    /**
     * get JSP file name from spreadsheet
     * 
     * @param workbook
     *            Spread sheet DT file instance
     * @return List of jspx file name
     */
  
    private List<String> getJspxFromXlsm( ADFDiWorkbook workbook ) throws Exception {
        List<String>  webPickerButtonList = new ArrayList<String>();
        List<String> resultStrList = new ArrayList<String>();
        
        webPickerButtonList = siStringsFilter( workbook );
        resultStrList = buttonWebPickerPageFilter( webPickerButtonList );
        return resultStrList;
    }
 
    /**
     * filter si string which contains web picker in the button
     * 
     * @param workbook
     *            spread sheet absolute path
     * @return list of label 'si' string
     */
    private List<String> siStringsFilter( ADFDiWorkbook workbook )  {
        
        ArrayList<String>  webPickerButtonList = new ArrayList<String>();
       ArrayList<String>  sharedStringList = workbook.getSharedStrings();
        if (sharedStringList != null) {
                for (int i = 0; i < sharedStringList.size(); i++) {
                    String str = sharedStringList.get(i);
                    if (str != null && str.startsWith("<Worksheet>") && str.contains("<ActionOptions")
                                    && str.contains("type=\"Dialog\"")) {
                            webPickerButtonList.add(str);
                    }
                }
        }
        
        return webPickerButtonList;
       
    }
    
   
    /**
     * filtering web picker page path string
     * 
     * @param buttonStringList
     *            list of label 'si' string
     * @return list of jspx page relative path
     */
    private List<String> buttonWebPickerPageFilter(List<String> buttonStringList) {
            String pageStart = "<Page><Page>";
            String pageEnd = "</Page></Page>";
            List<String> resultList = new ArrayList<String>();
            for (int i = 0; i < buttonStringList.size(); i++) {
                    String lineStr = buttonStringList.get(i).toString();
                    String pageStr = null;
                    while (lineStr.indexOf(pageStart) >= 0) {
                            if (lineStr.indexOf(pageStart) + 12 < lineStr.indexOf(pageEnd)) {
                                    pageStr = lineStr.substring(lineStr.indexOf(pageStart) + 12, lineStr.indexOf(pageEnd));
                                    resultList.add(pageStr);
                            } else {
                                    throw new RuntimeException(lineStr);
                            }

                            lineStr = lineStr.substring(lineStr.indexOf(pageEnd) + 14, lineStr.length());
                    }
            }

            if (resultList.size() == 0) {
                    resultList.add(" ");
            }
            return resultList;
    }
    
  
}
