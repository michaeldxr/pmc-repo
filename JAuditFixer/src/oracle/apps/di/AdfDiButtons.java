package oracle.apps.di;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.di.metadata.ADFDiComponentMetadata;
import oracle.apps.di.metadata.ADFDiMetadata;
import oracle.apps.di.metadata.ActionSet;
import oracle.apps.di.metadata.ButtonMetadata;
import oracle.apps.di.metadata.TableMetadata;
import oracle.apps.di.metadata.WorkbookMetadata;
import oracle.apps.di.metadata.Worksheet;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.JoesBaseClass;

public class AdfDiButtons  extends JoesBaseClass{
    
    private static BufferedWriter writer;
    private static String mode="view"; //can be view/LRG/premerge
    private static final String issuetype = "ADFdiButtons";
    private static String sCrawlDir = "";
    private static Pattern pBundleReference = Pattern.compile("#\\{(\\w+)\\['(.+?)'\\]\\}");
    private static PreparedStatement ps;
    private static ResultSet rs;
    
    public AdfDiButtons() {
        super(JoesBaseClass.CRAWL_TYPE.XLS);
    }
    
    public static void main(String[] args) throws Exception{
        
        String series = "FUSIONAPPS_PT.R9ERP_LINUX.X64";
        Connection con = DriverManager.getConnection("jdbc:oracle:thin:@angrybirds.us.oracle.com:1521:codescan","STRSCANNER","strscanner");
        ps = con.prepareStatement("SELECT string_value from strings,bundles where strings.bundle_id = bundles.id" +
            " and series = '" + series + "' and string_key = ? and fqn = ?");
            
        writer = new BufferedWriter(new FileWriter("ADFdi_buttons.csv"));
        writer.write("Family,module,product,filename,Series,Label,Component,Label Header, ButtonType\n");
        AdfDiButtons scanner = new AdfDiButtons();
        sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_PT.R9ERP_LINUX.X64.rdd/LATEST/" +
            "fusionapps/fin/components";
        scanner.crawlDirectory(sCrawlDir,false);
        writer.close();
        ps.close();
        rs.close();
        con.close();
    }

    protected void processFile(File fName, boolean bDoADE){
        
        String filepath = fName.getAbsolutePath();       
        try{            
            ADFDiWorkbook workbook = ADFDiWorkbook.getDIWorkbook(filepath, false);
            HashMap<String,String> resources = new HashMap<String,String>();
//            for(int i = 0; i < workbook.adfdiMetadataStrings.size(); i++)
//                System.out.println(workbook.adfdiMetadataStrings.get(i));
            
            for(Iterator<ADFDiMetadata> it = workbook.diMetadata.iterator(); it.hasNext();) {
                ADFDiMetadata mt = it.next();
                //System.out.println(mt.getMetadataType());
                
                 if(mt instanceof ButtonMetadata){
                     
                     String label = ((ADFDiComponentMetadata)mt).getValue();
                     String resolvedLabel = getResolvedLabel(label,resources);
                                
                    writer.write(FamilyModuleHelper.getFileNameInfo1(filepath) 
                    + ((ADFDiComponentMetadata)mt).getComponentType() + ","
                    + label + "," + resolvedLabel + ",Regular Button" + "\n");                       
                                      
                } 
                else if(mt instanceof Worksheet){
                    
                    ArrayList<String> menuItems = ((Worksheet)mt).getMenuItems();
                    for(int i =0; i < menuItems.size(); i++) {
                        
                        String label = menuItems.get(i);
                        String resolvedLabel = getResolvedLabel(label,resources);
                        writer.write(FamilyModuleHelper.getFileNameInfo1(filepath) 
                        + mt.getMetadataType() + ","
                        + label + "," + resolvedLabel + ",RibbonButton- Worksheet MenuItem\n");
                                           
                    }                    
                }
                else if(mt instanceof WorkbookMetadata){
                                    
                    resources = ((WorkbookMetadata)mt).getResources();
                    ArrayList<String> menuItems = ((WorkbookMetadata)mt).getMenuItems();
                    for(int i =0; i < menuItems.size(); i++) {
                        
                        String label = menuItems.get(i);
                        String resolvedLabel = getResolvedLabel(label,resources);
                        writer.write(FamilyModuleHelper.getFileNameInfo1(filepath) 
                        + mt.getMetadataType() + ","
                        + label + "," + resolvedLabel + ",RibbonButton- Workbook MenuItem\n");
                                           
                    }                    
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getResolvedLabel(String label,HashMap<String,String> resources) {
        
        String resolvedLabel=" ";
        try{
            Matcher m = pBundleReference.matcher(label);
            if(m.find()){
                String alias = m.group(1);
                String key = m.group(2);
                String bundleClass = resources.get(alias);
                if(bundleClass != null && !bundleClass.trim().equals("")){
                    ps.setString(1,key);
                    ps.setString(2,bundleClass);
                    rs = ps.executeQuery();
                    if(rs.next())
                        resolvedLabel = rs.getString("STRING_VALUE");
                }
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
                            
        return resolvedLabel;
    }

    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return null;
    }
}
