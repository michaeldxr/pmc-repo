package oracle.apps.di;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import oracle.apps.di.checker.CheckerResult;
import oracle.apps.di.metadata.ADFDiMetadata;


public class ADFDiWorkbook {
    static boolean bDebug = false;
    String _spreadsheetPath = "";
    // Should capture the strings as is in the sharedStrings.xml file
    ArrayList<String> sharedStrings = new ArrayList<String>();
    // Should capture all the strings as is in the ADFDi_Metadata sheet.
    ArrayList<String> adfdiMetadataStrings = new ArrayList<String>();
    
    ArrayList<ADFDiMetadata> diMetadata = new ArrayList<ADFDiMetadata>();
    
    
    // List of all the exceptions that occurred while parsing this workbook
    ArrayList<String> parseExceptions = new ArrayList<String>();
    // List of metadata component names that failed parsing for this workbook
    ArrayList<String> listOfFailedParsedComponents=new ArrayList<String>();
    boolean captureExceptionStacks=false;
    private ADFDiWorkbook() {
        super();
    }

    public static void main(String[] args) {
        ADFDiWorkbook aDFDiWorkbook = new ADFDiWorkbook();
    }

    public static ADFDiWorkbook getDIWorkbook_Obsolete(String spreadsheetPath, boolean captureExceptionStacks) {
        ADFDiWorkbook workbook = new ADFDiWorkbook();
        workbook.captureExceptionStacks=captureExceptionStacks;
        workbook._spreadsheetPath = spreadsheetPath;
        // TODO: There should be a way to differentiate a plain xlsx from a DI enabled spreadsheet??
        try {
            workbook.sharedStrings = SharedStringsParser.getListOfAllStrings(spreadsheetPath,true);
            ArrayList<String> metadataStrings = SharedStringsParser.getListOfAllADFDiMetaStrings(spreadsheetPath,true);
            if (metadataStrings == null || metadataStrings.size() < 1)
                return workbook;


            Iterator<String> metadataIter = metadataStrings.iterator();
            String metadataNme = "";
            while (metadataIter.hasNext()) {
                try {
                    String metadata = metadataIter.next();
                    metadataNme = SharedStringsParser.getStringTagName(metadata);
                    if (metadataNme == null)
                        metadataNme = "";
                    ADFDiMetadata diMeta = ADFDiMetadata.getMetadata(metadata);
                    if (diMeta != null)
                        workbook.diMetadata.add(diMeta);
                } catch (Exception ex2) {
                    if(metadataNme!=null && !metadataNme.trim().equals(""))
                        workbook.listOfFailedParsedComponents.add(metadataNme);
                    
                    if (bDebug) {
                        System.out.println("[INFO:SKIPPING METADATA: "+metadataNme+"] There was an exception parsing ADFDi metadata for file: " + spreadsheetPath);
                        // ex2.printStackTrace();
                        if(workbook.captureExceptionStacks)
                            workbook.addException("[ParsingException]", ex2);
                    }
                }
            }

        } catch (Exception ex) {
            System.out.println("There was an exception parsing the DI Workbook: " + spreadsheetPath);
            ex.printStackTrace();
            if(workbook.captureExceptionStacks)
                workbook.addException("[ParsingException]", ex);
        }
        return workbook;
    }
    public static ADFDiWorkbook getDIWorkbook(String spreadsheetPath, boolean captureExceptionStacks) {
        ADFDiWorkbook workbook = new ADFDiWorkbook();
        workbook.captureExceptionStacks=captureExceptionStacks;
        workbook._spreadsheetPath = spreadsheetPath;
        try {
            // Get the index preserved list of all strings. Do not do any concatenation. keep the original indexes preserved
            workbook.sharedStrings = SharedStringsParser.getListOfAllStrings(spreadsheetPath,false);
            // adfdi Metadata strings as is in the ADFDI_Metadata file
            workbook.adfdiMetadataStrings = SharedStringsParser.getListOfAllADFDiMetaStrings_New(workbook.sharedStrings,spreadsheetPath);
            // If for some reason it could not parse the ADFDI_Metadata file, then resort to the older method of trying to concatenate the large strings 
            // in the sharedstrings file itself
            if (workbook.adfdiMetadataStrings == null || workbook.adfdiMetadataStrings.size() < 1)
                workbook.adfdiMetadataStrings=SharedStringsParser.getListOfAllStrings(spreadsheetPath,true);

            ArrayList<String> metadataStrings = workbook.adfdiMetadataStrings;
            Iterator<String> metadataIter = metadataStrings.iterator();
            String metadataNme = "";
            String metadata="";
            while (metadataIter.hasNext()) {
                try {
                     metadata = metadataIter.next();
                    if(ADFDiMetadata.isStringDIMetadata(metadata))
                        continue;
                    if(!SharedStringsParser.isStringADIComponent(metadata))
                        continue;
                    metadataNme = SharedStringsParser.getStringTagName(metadata);
                    if (metadataNme == null)
                        metadataNme = "";
                    ADFDiMetadata diMeta = ADFDiMetadata.getMetadata(metadata);
                    if (diMeta != null)
                        workbook.diMetadata.add(diMeta);
                } catch (Exception ex2) {
                    if(metadataNme!=null && !metadataNme.trim().equals(""))
                        workbook.listOfFailedParsedComponents.add(metadataNme);
                    
                    if (bDebug) {
                        System.out.println("[INFO:SKIPPING METADATA: "+metadataNme+"] There was an exception parsing ADFDi metadata for file: " + spreadsheetPath);
                        // ex2.printStackTrace();
                        if(workbook.captureExceptionStacks)
                            workbook.addException("[ParsingException]", ex2);
                    }
                }
            }

        } catch (Exception ex) {
            System.out.println("There was an exception parsing the DI Workbook: " + spreadsheetPath);
            ex.printStackTrace();
            if(workbook.captureExceptionStacks)
                workbook.addException("[ParsingException]", ex);
        }
        return workbook;
    }
   
    private void addException(String msg, Exception ex) {
        if (ex == null)
            return;
        StringBuffer sb = new StringBuffer();
        sb.append(msg + "\n");
        sb.append(ex.getMessage() + "\n");
        StackTraceElement[] stacks = ex.getStackTrace();
        if (stacks == null)
            return;
        for (int i = 0; i < stacks.length; i++) {
            sb.append(stacks[i].toString() + "\n");
        }
        parseExceptions.add(sb.toString());
    }

    public ArrayList<CheckerResult> runLocalicationChecks() throws Exception {
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();

        //        Iterator<String> allStringsIter = sharedStrings.iterator();
        //        String sharedstring = "";
        //        while (allStringsIter.hasNext()) {
        //            try {
        //                sharedstring = allStringsIter.next();
        //                if (sharedstring == null || sharedstring.trim().equals(""))
        //                    continue;
        //                if (ADFDiMetadata.isStringDIMetadata(sharedstring))
        //                    continue;
        //                if (SharedStringsParser.isStringADIComponent(sharedstring)) {
        //                    ADFDiMetadata meta = ADFDiMetadata.getMetadata(sharedstring);
        //                    if (meta != null)
        //                        results.addAll(meta.runLocalizationCheck());
        //                } else {
        //                    //TODO: Check the shared string for hardcoded text
        //                    // for some reason adfdi parsing of read only tree table is erroring out coz the strings are not occuring in order in the xml.
        //                    // Hardcoded here till i find a fix
        //                    if (sharedstring.equals("ReadOnlyTreeTable"))
        //                        continue;
        //                    if (hasSomeTagContent(sharedstring))
        //                        continue;
        //                    System.out.println("Have to check string: " + sharedstring);
        //                    if (!ADFDiMetadata.isLocalizedString(sharedstring, false)) {
        //                        CheckerResult r =
        //                            new CheckerResult("R", "Strings:Generic", String.format("String: (%s) must be localized", sharedstring), false);
        //                        results.add(r);
        //                    }
        //                }
        //            } catch (Exception ex) {
        //                System.out.println("Exception while running localization check on spreadsheet:" + _spreadsheetPath);
        //                System.out.println(" ==> String resulting in error: " + sharedstring);
        //                addException("[RunLocalization]",ex);
        //
        //            }
        //        }
        ADFDiMetadata meta = null;
        try {
            Iterator<ADFDiMetadata> metaIter = diMetadata.iterator();
            while (metaIter.hasNext()) {
                meta = metaIter.next();
                ArrayList<CheckerResult> localResult = meta.runLocalizationCheck();
                if (localResult != null && localResult.size() > 0)
                    results.addAll(localResult);
            }
        } catch (Exception ex) {
            System.out.println("[UNEXPECTED ERROR] Exception while running localization check for spreadsheet: " + _spreadsheetPath);
        }
        return results;
    }

    
    public boolean hasSomeTagContent(String str) {
        int maxCap = 5;
        Pattern p = Pattern.compile("[<|>]");
        Matcher m = p.matcher(str);
        int numberOfTagBrackets = 0;
        //int numberOfCloseTags=0;
        while (m.find()) {
            numberOfTagBrackets++;
            if (numberOfTagBrackets > maxCap)
                break;
        }
        if (numberOfTagBrackets > 5)
            return true;
        return false;

    }

    public ArrayList<String> getParseExceptions() {
        return parseExceptions;
    }

    public ArrayList<String> getListOfFailedParsedComponents() {
        return listOfFailedParsedComponents;
    }

    public ArrayList<ADFDiMetadata> getDiMetadata() {
        return diMetadata;
    }
    
    public ArrayList<ADFDiMetadata> getComponentsOfType(String componentType)
    {
        ArrayList<ADFDiMetadata> filteredMeta = new ArrayList<ADFDiMetadata>();
        Iterator<ADFDiMetadata> diMetaIter =  diMetadata.iterator();    
        while(diMetaIter.hasNext())
        {
            ADFDiMetadata meta = diMetaIter.next();
            if(meta==null)
                continue;
            String type = meta.getMetadataType();
            if(type==null || type.equals(""))
                continue;
            if(type.equals(componentType))
                filteredMeta.add(meta);
            }
        return filteredMeta;
    }
    public String getSpreadsheetPath()
    {
        return _spreadsheetPath;
        }

    public static void setBDebug(boolean bDebug) {
        ADFDiWorkbook.bDebug = bDebug;
    }

    public static boolean isBDebug() {
        return bDebug;
    }

    public ArrayList<String> getSharedStrings() {
        return sharedStrings;
    }
}
