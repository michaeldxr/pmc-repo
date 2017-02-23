package oracle.apps.fixer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.utility.JoesBaseClass;

public class EnumPremergeChecker {
    
    public static void main(String[] args) throws Exception {
        
        EnumTransformer4 scanner = new EnumTransformer4(JoesBaseClass.CRAWL_TYPE.JAVA_JPR);
        scanner.bDoADE = false;
        scanner.writer =  new BufferedWriter(new FileWriter("enum_violations.csv"));
        scanner.writer1 = new BufferedWriter(new FileWriter("enum_errors.txt"));
        scanner.writer.write("Family,Module,Product,FileName,Series,Label,Issue\n");
        
        //String[] files = args[0].trim().split("\n");
        String ade_view_root = args[1].trim();
        String filelistpath=args[0].trim();
        String[] files=FamilyModuleHelper.getFileList(filelistpath, ade_view_root);
        
        for(int i = 0; i < files.length; i++) {
            
            String filePath = ade_view_root + "/" + files[i].trim();
            File f = new File(filePath);
            if(!f.exists())
                continue;
            if(!scanner.isDirOfInterest(filePath))
                continue;
            scanner.processFile(f,false);
        }
        scanner.writer.write("\n\nPlease see http://globaldc.oracle.com/perl/twiki/view/FusionSharedTools/AttributesEnumclasses " +
            "for description of the issue and resolution.\n\n Note: When fixing VORowImpl/EOImpl classes, make sure the corresponding jpr is also fixed with the right settings (Use Enumberations to invoke accessors). \n\n");
        scanner.writer.close();
        scanner.writer1.close();    
        
    }
}
