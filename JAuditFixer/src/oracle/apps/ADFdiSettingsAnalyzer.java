package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import oracle.apps.helpers.ProcessExecutor;
import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ADFdiSettingsAnalyzer {
    
    static BufferedWriter writer;
    static String viewRoot;
    
    public static void main(String[] args) throws Exception{
        
        if(args.length> 0 && args[0] != null && args[0].trim() != "")
            viewRoot = args[0].trim();
        
        viewRoot = "/ade_autofs/ud21_fa/FUSIONAPPS_11.1.1.5.1_LINUX.X64.rdd/LATEST/";
        
        ProcessExecutor pe = new ProcessExecutor();   
        ProcessBuilder pb = new ProcessBuilder("bash","-c","zcat " + viewRoot + 
                                                ".labellog.emd.gz | grep 'FILE fusionapps' | grep '/web.xml' " +
            "| grep -vi '/noship/' | cut -f2 -d ' '");
        pe.runProcess(pb, "/tmp/webXmlList.txt", false); 
        
        BufferedReader reader = new BufferedReader(new FileReader("/tmp/webXmlList.txt"));
        writer = new BufferedWriter(new FileWriter("adfdiSettings.csv"));
        writer.write("FAMILY,PRODUCT,FILENAME, SERVLET, INIT-PARAMS, SERVLET-MAPPING, MIME1, " +
            "MIME2, SECURITY-CONSTRAINT,FILTER1,FILTER2,FILTER3,ExcelFilterDefinedBeforeLibrary, MAPPING1," +
            "MAPPING2,MAPPING3\n");
        
        ADFdiSettingsAnalyzer scanner = new ADFdiSettingsAnalyzer();
        
        String line = "";
        while((line = reader.readLine()) != null) {
            String webXml = line.trim();
            scanner.processFile(webXml);
        }
        
        reader.close();
        writer.close();
        
    }
    
    private void processFile(String fileName) {
        try{
            XMLDocument doc = XMLParserHelper.getXMLDocument(viewRoot+fileName);
            if(doc == null)  return;
            
            String servClass = "";
            String servName = "";
            TreeMap<String,String> initParams = new TreeMap<String,String>();
            String servMapping = "";
            String mime1="";
            String mime2 = "";
            String securityContraint = "";
            String filter1 = "";
            String filter2 = "";
            String filter3 = "";
            boolean excelFilterDefinedBeforeLibrary = true;
            String filterMapping1 = "";
            String filterMapping2 = "";
            String filterMapping3 = "";
            String filterMapping4 = "";
    
            
            NodeList servlets = doc.getElementsByTagName("servlet");
            
            for(int i =0; i < servlets.getLength(); i++) {
                
                Node servlet = servlets.item(i);
                Node servletClass = XMLParserHelper.getChildNodeWithName(servlet, "servlet-class");
                if(servletClass == null) continue;
                
                servClass = servletClass.getTextContent().trim();
                if(!servClass.equals("oracle.adf.desktopintegration.servlet.DIRemoteServlet")) continue;
                
                servName = XMLParserHelper.getChildNodeWithName(servlet, "servlet-name").getTextContent().trim();     
                
                ArrayList<Node> initParamNodes = XMLParserHelper.getChildNodesWithName(servlet, "init-param");
                
                for(int j= 0; j < initParamNodes.size(); j++) {
                    Node initParam = initParamNodes.get(j);
                    String paramName = XMLParserHelper.getChildNodeWithName(initParam, "param-name").getTextContent().trim();
                    String paramValue = XMLParserHelper.getChildNodeWithName(initParam, "param-value").getTextContent().trim();
                    initParams.put(paramName,paramValue);
                }
                
                break;                
            }
            if(!servClass.equals("oracle.adf.desktopintegration.servlet.DIRemoteServlet")) //adfdi servlet not found
                return;
            
            NodeList servLetMappings = doc.getElementsByTagName("servlet-mapping");
            for(int i =0; i < servLetMappings.getLength(); i++) {
                
                Node servletMapping = servLetMappings.item(i);
                String mappingName = XMLParserHelper.getChildNodeWithName(servletMapping, "servlet-name").getTextContent().trim(); 
                if(!mappingName.equals(servName)) continue;
                
                String url = XMLParserHelper.getChildNodeWithName(servletMapping, "url-pattern").getTextContent().trim();
                servMapping = mappingName + ":" + url;
                break;
            }
            
            NodeList mimeMappings = doc.getElementsByTagName("mime-mapping");
            for(int i =0; i < mimeMappings.getLength(); i++) {
                
                Node mimeMapping = mimeMappings.item(i);
                String extension = XMLParserHelper.getChildNodeWithName(mimeMapping, "extension").getTextContent().trim();
                String mimeType = XMLParserHelper.getChildNodeWithName(mimeMapping, "mime-type").getTextContent().trim();
                
                if(extension.equals("xlsx"))
                    mime1 = extension + ":" + mimeType;
                
                if(extension.equals("xlsm"))
                    mime2 = extension + ":" + mimeType;
            }
            
            NodeList securityConstraints = doc.getElementsByTagName("security-constraint");
            for(int i =0; i < securityConstraints.getLength(); i++) {
                
                Node constraint = securityConstraints.item(i);
                String secConstraint = XMLParserHelper.getStringFromNode(constraint);
                if(secConstraint.contains(servName)){
                    securityContraint = secConstraint;
                    break;
                }
            }
            
            NodeList filters = doc.getElementsByTagName("filter");
            for(int i =0; i < filters.getLength(); i++) {
                
                Node filter = filters.item(i);
                String filterName = XMLParserHelper.getChildNodeWithName(filter, "filter-name").getTextContent().trim();
                String filterClass = XMLParserHelper.getChildNodeWithName(filter, "filter-class").getTextContent().trim();
                String filterParams = "";
                
                ArrayList<Node> initParamNodes = XMLParserHelper.getChildNodesWithName(filter, "init-param");
                
                for(int j= 0; j < initParamNodes.size(); j++) {
                    Node initParam = initParamNodes.get(j);
                    String paramName = XMLParserHelper.getChildNodeWithName(initParam, "param-name").getTextContent().trim();
                    String paramValue = XMLParserHelper.getChildNodeWithName(initParam, "param-value").getTextContent().trim().replace(","," ");
                    filterParams += paramName + ":" + paramValue + ";";
                }
                
                if(filterName.equals("adfBindings"))
                    filter1 = filterName + " " + filterClass + " " + filterParams;
                if(filterName.equals("adfdiExcelDownload")){
                    filter2 = filterName + " " + filterClass + " " + filterParams;
                    if(!filter3.equals(""))
                        excelFilterDefinedBeforeLibrary = false;
                }
                if(filterName.equals("ADFLibraryFilter"))
                    filter3 = filterName + " " + filterClass + " " + filterParams;
            }            
            
            
            NodeList filterMappings = doc.getElementsByTagName("filter-mapping");
            for(int i =0; i < filterMappings.getLength(); i++) {
                
                Node filterMapping = filterMappings.item(i);
                String filterName = XMLParserHelper.getChildNodeWithName(filterMapping, "filter-name").getTextContent().trim();
                String mapping = "";
                String dispatchers = "";
                
                ArrayList<Node> dispatcherNodes = XMLParserHelper.getChildNodesWithName(filterMapping, "dispatcher");
                for(int j = 0; j < dispatcherNodes.size(); j++) {
                    dispatchers += dispatcherNodes.get(j).getTextContent().trim() + " ";
                }
                
                
                if(filterName.equals("adfBindings")){
                    Node servletName = XMLParserHelper.getChildNodeWithName(filterMapping, "servlet-name");
                    if(servletName == null) continue;
                    mapping = servletName.getTextContent().trim();
                    if(mapping.equals(servName))
                        filterMapping1 = filterName + ":" + mapping + ":" + dispatchers;
                }
                else if(filterName.equals("adfdiExcelDownload")){
                    Node urlPattern = XMLParserHelper.getChildNodeWithName(filterMapping, "url-pattern");
                    if(urlPattern == null) continue;
                    mapping = urlPattern.getTextContent().trim();
                    if(mapping.contains("xlsx"))
                        filterMapping2 = filterName + ":" + mapping + ":" + dispatchers;
                    else if(mapping.contains("xlsm"))
                        filterMapping3 = filterName + ":" + mapping + ":" + dispatchers;
                }
                else if(filterName.equals("ADFLibraryFilter")){
                    Node urlPattern = XMLParserHelper.getChildNodeWithName(filterMapping, "url-pattern");
                    if(urlPattern == null) continue;
                    mapping = urlPattern.getTextContent().trim();
                    filterMapping4 = filterName + ":" + mapping + ":" + dispatchers;
                }
            } //filterMappings
            
            
            writer.write(getFamilyProductFromFileName(fileName) + fileName + ", " + servName + ":" + servClass +
                         ", " +  getInitParams(initParams) + ", " + servMapping + "," + mime1 + ", " + mime2 + ", "
                         + securityContraint.replaceAll("\\s*", "") + ", " + filter1 + ", " + filter2 + ", " + filter3 + ", " + excelFilterDefinedBeforeLibrary
                         + ", " + filterMapping1 + ", " + filterMapping2 + ", " + filterMapping3 + ", " + filterMapping4 + "\n");
            
            
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getFamilyProductFromFileName(String fileName) {
        String family = "";
        String product = "";
        
        String[] parts = fileName.split("/");
        if(parts.length > 1) family = parts[1].trim().toUpperCase();
        if(parts.length > 4) product = parts[4].trim().toUpperCase();
        
        return family + ", " + product + ", ";
    }
    
    private String getInitParams(TreeMap<String,String> initParams) {
        
        StringBuffer sbInitParams = new StringBuffer();
        for(Iterator<String> it = initParams.keySet().iterator(); it.hasNext();) {
            String paramName = it.next();
            sbInitParams.append(paramName + ":" + initParams.get(paramName).replace(","," ") + ";");
        }
        return sbInitParams.toString();
    }
} //class
