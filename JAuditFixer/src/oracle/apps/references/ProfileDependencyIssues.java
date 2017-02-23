package oracle.apps.references;

import java.io.File;

import java.util.ArrayList;
import java.util.HashSet;

import java.util.Iterator;

import oracle.apps.helpers.AngryBirdsDumper;
import oracle.apps.helpers.FamilyModuleHelper;
import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.helpers.ZOverallIntBase;
import oracle.apps.scanresults.ABScanResult;
import oracle.apps.seed.SeedReExtract;
import oracle.apps.utility.JoesBaseClass;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ProfileDependencyIssues extends ZOverallIntBase {
    
    //static final String issue="ProfileDepdencyErrors-FinV2Main";
    static final String profileIssue="ProfileDepdencyErrors";
    public ProfileDependencyIssues() {
        super(profileIssue,JoesBaseClass.CRAWL_TYPE.JPR);
        setBDebug(true);
    }
    
    public static void main(String[] args) throws Exception {
        ProfileDependencyIssues profileDependencyIssues = new ProfileDependencyIssues();
        
     
        if (args == null || args.length < 1 || isEmpty(args[0])) {
            System.out.println("Missing scan directory. Usage:  " + overallUsage);
            System.exit(1);
        }
        
        // Unique type of scan, want to hsow the results in protytpe section, with the 
        String branchType = System.getProperty("branchType");
        if(isEmpty(branchType))
           branchType="Rup4"; 
        profileDependencyIssues.setScanIssue(profileIssue+"-"+branchType);
        String crawlDir = args[0];
        profileDependencyIssues.startScan(crawlDir);
        
  
    }

  
    public boolean isAbsolutePathInList(HashSet<String> listOfPaths, String pathToCheck) {
        if (listOfPaths == null || listOfPaths.size() < 1)
            return false;
        String absPath = "";
        try {
            File f1 = new File(pathToCheck);
            absPath = f1.getCanonicalPath();
        } catch (Exception ex) {
        }
        if (isEmpty(absPath))
            return true;

        Iterator<String> pathIter = listOfPaths.iterator();
        while (pathIter.hasNext()) {
            String p = pathIter.next();
            try {
                File f = new File(p);
                String path2 = f.getCanonicalPath();
                if (!isEmpty(path2) && absPath.trim().equals(path2))
                    return true;
            } catch (Exception ex) {
            }

        }
        return false;
    }

   

    @Override
    protected void doProcessFile(File fName) {
        
        String absPath = fName.getAbsolutePath();
        if(absPath.endsWith("Test.jpr"))
            return;
        if(bDebug)
            System.out.println("Processing Jpr: " + absPath);
        
        NodeList profileDependencyNodes = JprHelper.getAllProfileDependencyNodes(absPath);
        if (profileDependencyNodes == null || profileDependencyNodes.getLength() < 1) {
            return;
        }
        
        String baseJprLocation = JprHelper.getBaseJprLocation(absPath);
        if (isEmpty(baseJprLocation))
            return;

        int numberProfileDependencies = profileDependencyNodes.getLength();
        HashSet<String> adfLibraries = JprHelper.getAllAdfLibrarysInJpr(absPath, true);
        for (int i = 0; i < numberProfileDependencies; i++) {
            Node depNode = profileDependencyNodes.item(i);
            if (depNode == null)
                return;
            Node ownerUrlNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(depNode, "url", "n", "ownerURL");
            Node profileNameNode = XMLParserHelper.getChildNodeWithMatchingAttributeAndName(depNode, "value", "n", "profileName");
            if (ownerUrlNode == null || profileNameNode == null) {
                System.out.println("[ERROR] Missing one of ownerUrl or profileName node");
                System.exit(1);
            }
            String profileJprRelative = XMLParserHelper.getAttributeValue(ownerUrlNode, "path");
            String profileName = XMLParserHelper.getAttributeValue(profileNameNode, "v");
            if (isEmpty(profileJprRelative) || isEmpty(profileName)) {
                System.out.println("[ERROR] Missing one of ownerUrl or profileName.. Can't process further");
                System.exit(1);
            }

            String dependentJpr = baseJprLocation + "/" + profileJprRelative;
            if (!doesFileExist(dependentJpr))
                return;
            
            // If it also has a BO Dependency on this jpr you can skip it
            HashSet<String> boDepdendentJprs = JprHelper.getProjectDependencysInJar(absPath);
            if(isAbsolutePathInList(boDepdendentJprs, dependentJpr))
            {
                System.out.println("BO Depdency exists.. skpping .. " + absPath);
                return;
                }
            String dependentJarRequired = JprHelper.getDeploymentProfileUrlWithName(dependentJpr, profileName, true);
            if (isEmpty(dependentJarRequired)) {
                System.out.println("Could not fid the dependent jar path for profile: " + profileName + " in jpr: " + dependentJpr);
                return;
            }
           // System.out.println("Have to check for dependent jar: " + dependentJarRequired);

            if (isAbsolutePathInList(adfLibraries, dependentJarRequired)) {
                return;
            }
            

            // Violation : profile dependency but no corresponding library dependency
            System.out.println("Violation in jpr: " + absPath+" Profile: " + profileName);
            ABScanResult r = new ABScanResult(absPath);
            r.setIssue(getScanIssue());
            r.setSubIssue("ProfileDependency present but no corresponding jar reference");
            r.setDescription(String.format("Jpr Contains dependent profile %s but no jar reference: %s",profileName,FamilyModuleHelper.getFileNameFromPath(dependentJarRequired)));
            results.add(r);
        }
        
    }
}
