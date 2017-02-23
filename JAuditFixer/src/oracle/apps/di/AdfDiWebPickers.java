package oracle.apps.di;

import java.io.BufferedWriter;
import java.io.File;

import java.io.FileWriter;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.di.metadata.ADFDiComponentMetadata;
import oracle.apps.di.metadata.ADFDiMetadata;
import oracle.apps.di.metadata.ActionSet;
import oracle.apps.di.metadata.TableMetadata;
import oracle.apps.di.metadata.Worksheet;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.JoesBaseClass;

public class AdfDiWebPickers  extends JoesBaseClass{
    
    private static BufferedWriter writer;
    private static String mode="view"; //can be view/LRG/premerge
    private static final String issuetype = "ADFdiWebPickers";
    private static String sCrawlDir = "";
    private static Pattern pBinding = Pattern.compile("#\\{.*?\\}");
    
    public AdfDiWebPickers() {
        super(JoesBaseClass.CRAWL_TYPE.XLS);
    }
    
    public static void main(String[] args) throws Exception{
        
        writer = new BufferedWriter(new FileWriter("ADFdi_webPickers.csv"));
        writer.write("Family,module,product,filename,Series,Label,Component,Label Header, DialogPage\n");
        AdfDiWebPickers scanner = new AdfDiWebPickers();
        sCrawlDir = "/ade_autofs/ud21_fa/FUSIONAPPS_PT.R9INT_LINUX.X64.rdd/LATEST/" +
            "fusionapps/fin/components";
        scanner.crawlDirectory(sCrawlDir,false);
        writer.close();
    }

    protected void processFile(File fName, boolean bDoADE){
        
        String filepath = fName.getAbsolutePath();       
        try{            
            ADFDiWorkbook workbook = ADFDiWorkbook.getDIWorkbook(filepath, false);
//            for(int i = 0; i < workbook.adfdiMetadataStrings.size(); i++)
//                System.out.println(workbook.adfdiMetadataStrings.get(i));
            
            for(Iterator<ADFDiMetadata> it = workbook.diMetadata.iterator(); it.hasNext();) {
                ADFDiMetadata mt = it.next();
                //System.out.println(mt.getMetadataType());
                
                if(mt instanceof TableMetadata){
                    
                    ArrayList<TableMetadata.Column> columns = ((TableMetadata)mt).getColumns();
                    for(int i = 0; i < columns.size(); i++) {
                        
                        TableMetadata.Column col = columns.get(i);
                        ADFDiComponentMetadata m = col.getInsertComponent(); 
                        
                        if(m != null) {
                            ActionSet dblClickActionSet = m.getDblclickActionSet();
                            
                            if(dblClickActionSet != null){
                                ArrayList<ActionSet.Action> actions = dblClickActionSet.getActions();
                                for(int j = 0; j < actions.size(); j++) {
                                    ActionSet.Action act = actions.get(j);
                                    if(act.getMethodType().equals("Dialog")){
                                        
                                        String binding = col.getHeaderLabel();
                                        Matcher mat = pBinding.matcher(binding);
                                        if(mat.find())
                                            binding=mat.group();
                                        
                                        writer.write(FamilyModuleHelper.getFileNameInfo1(filepath) 
                                        + mt.getMetadataType() + "(" + ((TableMetadata)mt).getComponentId() + ";" 
                                        + ((TableMetadata)mt).getPositionCol() + ((TableMetadata)mt).getPositionRow() + ") --> " 
                                        + m.getComponentType()  + "(" + col.getId() + ";insertComponent)," + binding + ","
                                        + act.getDialogPage() + "\n");
                                    }
                                }
                            }
                        }
                        
                        m= col.getUpdateComponent();
                        if(m != null) {
                            ActionSet dblClickActionSet = m.getDblclickActionSet();
                                if(dblClickActionSet != null){
                                    ArrayList<ActionSet.Action> actions = dblClickActionSet.getActions();
                                    for(int j = 0; j < actions.size(); j++) {
                                        ActionSet.Action act = actions.get(j);
                                        if(act.getMethodType().equals("Dialog")) {
                                            
                                            String binding = col.getHeaderLabel();
                                            Matcher mat = pBinding.matcher(binding);
                                            if(mat.find())
                                                binding=mat.group();
                                            
                                            writer.write(FamilyModuleHelper.getFileNameInfo1(filepath) 
                                            + mt.getMetadataType() + "(" + ((TableMetadata)mt).getComponentId() + ";" 
                                            + ((TableMetadata)mt).getPositionCol() + ((TableMetadata)mt).getPositionRow() + ") --> " 
                                            + m.getComponentType()  + "(" + col.getId() + ";updateComponent)," + binding  + "," 
                                            + act.getDialogPage() + "\n");
                                        }
                                    }
                                }
                        }                        
                    }     // iterating over columns                                   
                    
                } else if(mt instanceof ADFDiComponentMetadata){
                    
                    ActionSet dblClickActionSet = ((ADFDiComponentMetadata)mt).getDblclickActionSet();
                    if(dblClickActionSet == null)
                        continue;
                    ArrayList<ActionSet.Action> actions = dblClickActionSet.getActions();
                    for(int j = 0; j < actions.size(); j++) {
                        ActionSet.Action act = actions.get(j);
                        if(act.getMethodType().equals("Dialog")){
                            
                            String binding = ((ADFDiComponentMetadata)mt).getValue();
                            Matcher mat = pBinding.matcher(binding);
                            if(mat.find())
                                binding=mat.group();
                                
                            writer.write(FamilyModuleHelper.getFileNameInfo1(filepath) 
                            + ((ADFDiComponentMetadata)mt).getComponentType() + "("
                            + ((ADFDiComponentMetadata)mt).getComponentId() + ";" 
                            + ((ADFDiComponentMetadata)mt).getPositionCol() + ((ADFDiComponentMetadata)mt).getPositionRow() + "),"
                            + binding + "," + act.getDialogPage() + "\n");
                        }
                    }
                    
                } else if(mt instanceof Worksheet){
                    
                    ArrayList<Worksheet.WorksheetEvent> worksheetEvents = ((Worksheet)mt).getWorksheetEvents();
                    for(int i =0; i < worksheetEvents.size(); i++) {
                        
                        Worksheet.WorksheetEvent event = worksheetEvents.get(i);
                        ActionSet as = event.getEventActions();
                        if(as == null) continue;
                        ArrayList<ActionSet.Action> actions = as.getActions();
                        
                        for(int j =0; j < actions.size(); j++){
                            
                            ActionSet.Action act = actions.get(j);
                            if(act.getMethodType().equals("Dialog"))
                                writer.write(FamilyModuleHelper.getFileNameInfo1(filepath) 
                                + mt.getMetadataType() + "(" + ((Worksheet)mt).getWorksheetTitle() + ") --> " 
                                + event.getEventName() + "," 
                                + act.getDialogPage() + "\n");
                        }                        
                    }                    
                }
            }
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
