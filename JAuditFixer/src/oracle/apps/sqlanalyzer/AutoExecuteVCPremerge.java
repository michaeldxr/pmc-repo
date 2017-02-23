package oracle.apps.sqlanalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import oracle.apps.fixer.EnumTransformer4;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.JoesBaseClass;

public class AutoExecuteVCPremerge {
    
    public static void main(String[] args) throws Exception {
        
        AutoExecuteVCScan scanner = new AutoExecuteVCScan();
        scanner.generateExemptionList();
        scanner.writer =  new BufferedWriter(new FileWriter("autoexecute_vcscan.csv"));
        scanner.writer.write("Family,Module,Product,Filename,Series,Label,ViewCriteria, Description\n");
        
        //String[] files = args[0].trim().split("\n");
        String ade_view_root = args[1].trim();
        String filelistpath=args[0].trim();
        String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);
        for(int i = 0; i < files.length; i++) {
            
            String filePath = ade_view_root + "/" + files[i].trim();
            File f = new File(filePath);
            if(!f.exists())
                continue;
            if(!scanner.fileOfInterest(filePath))
                continue;
            scanner.processFile(f);
        }
        scanner.writer.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/AutoExecuteViewCriteria " +
            "for description of the issue and resolution.\n\n");
        scanner.writer.close();   
        
    }
}
