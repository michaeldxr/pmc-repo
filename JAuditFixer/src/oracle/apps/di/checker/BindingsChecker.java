package oracle.apps.di.checker;

//import com.sun.org.apache.xpath.internal.axes.IteratorPool;

import java.io.File;

import java.util.ArrayList;

import java.util.HashSet;
import java.util.Iterator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.di.ADFDiWorkbook;
import oracle.apps.di.CheckerConstants;
import oracle.apps.di.SharedStringsParser;
import oracle.apps.di.metadata.ADFDiComponentMetadata;
import oracle.apps.di.metadata.ADFDiMetadata;
import oracle.apps.di.metadata.WorkbookMetadata;
import oracle.apps.di.metadata.Worksheet;
import oracle.apps.helpers.parsers.UIPageDef;
import oracle.apps.votuning.PageDef;

public class BindingsChecker extends DIGenericStandardsChecker {
    String sharedStringsContent = "";

    public BindingsChecker() {
        super();
    }
//
    public static void main(String[] args) throws Exception {
        String str = "#{row.bindings.invoiceId}";
        String rowPrefixRegEx="(row\\.)?";
        String regEx = String.format("#\\{"+rowPrefixRegEx+"bindings\\.%s(\\.|})","invoiceId");
        System.out.println("RegEx:"+regEx);
        Pattern regExP = Pattern.compile(regEx);
        Matcher m = regExP.matcher(str);
        if (m.find()) {
            System.out.println("Match: " + m.group(0));
        }
    }

    @Override
    protected ArrayList<CheckerResult> doCheck(ADFDiWorkbook workbook, boolean getOnlyFailedResults) throws Exception {
       
        HashSet<String> pageDefNames = getListOfPageDefsInWorkbook(workbook);

        if (pageDefNames == null || pageDefNames.size() < 1)
            return null;
        int numOfNames = pageDefNames.size();
        HashSet<String> absoluteageDefPaths = getAbsolutePageDefPaths(pageDefNames, workbook.getSpreadsheetPath());

        if (absoluteageDefPaths == null || absoluteageDefPaths.size() < 1) {
            System.out.println("[ERROR] There was an error resolving page defs for workbook: " + workbook.getSpreadsheetPath());
            return null;
        }
//        if (numOfNames != absoluteageDefPaths.size()) {
//            System.out.println("There was an error resolving page defs for workbook: " + workbook.getSpreadsheetPath());
//        }
//        if (absoluteageDefPaths.size() > 1) {
//            System.out.println("[ALERT] !!!!!!!!!!!!!!!!!!!!!! ==> More than one page def in: " + workbook.getSpreadsheetPath());
//        }
        ArrayList<CheckerResult> results = checkPageDefsAgainstWorkbook(workbook, absoluteageDefPaths);

        return results;
    }

    private ArrayList<CheckerResult> checkPageDefsAgainstWorkbook(ADFDiWorkbook workbook, HashSet<String> absoluteageDefPaths) {
        try {
            sharedStringsContent = SharedStringsParser.getSharedStringsContent(workbook.getSpreadsheetPath());
        } catch (Exception ex) {
            System.out.println("Exception getting shared strings input: " + workbook.getSpreadsheetPath());
        }

        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        Iterator<String> pageDefIter = absoluteageDefPaths.iterator();
        while (pageDefIter.hasNext()) {
            String pageDefPath = pageDefIter.next();
            ArrayList<CheckerResult> pdefresults = checkPageDefsAgainstWorkbook(workbook, pageDefPath);
            if (pdefresults != null && pdefresults.size() > 0)
                results.addAll(pdefresults);
        }
        return results;
    }

    private ArrayList<CheckerResult> checkPageDefsAgainstWorkbook(ADFDiWorkbook workbook, String absoluteageDefPath) {
        if (isEmpty(absoluteageDefPath))
            return null;
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        if (isEmpty(sharedStringsContent))
            return null;
        String msg = "";
        try {


            msg = "Parsing pagedef";
            UIPageDef parsedPdef = UIPageDef.getPageDef(absoluteageDefPath);
            if (parsedPdef == null)
                throw new Exception("Exception parsing pagedef");

            // get the workbook shared strings file as a entire file
            // for each attribute binding: check #{bindings.<attributeBinding.id>.
            filterOutUsedMethodActions(parsedPdef);
            addResults(results, checkAttributeUsages(parsedPdef.getAttributeBindings()));
            addResults(results, checkActionUsages(parsedPdef.getActionBindings()));
            addResults(results, checkMethodActionUsages(parsedPdef.getMethodActionBindings()));
            addResults(results, checkTree(parsedPdef.getPageDefTreeBindings()));
            addResults(results, checkLists(parsedPdef.getListBindings()));
            addResults(results, checkUnusedIterators(parsedPdef));


        } catch (Exception ex) {
            System.out.println("Exception in: " + msg + " : " + absoluteageDefPath);

        }
        return results;
    }

    /**
     *Remove all the method actions or actions that are used in invoke actions. because they will not be unused
     * @param pageDef
     */
    private void filterOutUsedMethodActions(UIPageDef pageDef) {

        ArrayList<UIPageDef.InvokeAction> invokeActions = pageDef.getInvokeActions();
        if (invokeActions == null || invokeActions.size() < 1)
            return;

        ArrayList<UIPageDef.MethodActionBinding> methodActions = pageDef.getMethodActionBindings();
        if (methodActions != null && methodActions.size() > 0) {
            Iterator<UIPageDef.MethodActionBinding> methodActIter = methodActions.iterator();
            while (methodActIter.hasNext()) {
                UIPageDef.MethodActionBinding methodAct = methodActIter.next();
                if (isInvokeAction(invokeActions, methodAct.getId()))
                    methodActIter.remove();

            }
        }

        ArrayList<UIPageDef.ActionBinding> actBindings = pageDef.getActionBindings();
        if (actBindings != null && actBindings.size() > 0) {
            Iterator<UIPageDef.ActionBinding> methodActIter = actBindings.iterator();
            while (methodActIter.hasNext()) {
                UIPageDef.ActionBinding methodAct = methodActIter.next();
                if (isInvokeAction(invokeActions, methodAct.getId()))
                    methodActIter.remove();
            }
        }

    }

    private boolean isInvokeAction(ArrayList<UIPageDef.InvokeAction> invokeActions, String checkId) {
        if (isEmpty(checkId))
            return false;
        Iterator<UIPageDef.InvokeAction> invokeActIter = invokeActions.iterator();
        while (invokeActIter.hasNext()) {
            String binds = invokeActIter.next().getBinds();
            if (isEmpty(binds))
                continue;
            if (binds.equals(checkId))
                return true;
        }
        return false;
    }

    private ArrayList<CheckerResult> checkUnusedIterators(UIPageDef pgDef) {
        String code = "B_1";

        String issue = CheckerConstants.ISSUE_BINDINGS;
        String subIssue = "Iterator not used";

        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();
        ArrayList<UIPageDef.PageDefIteratorBinding> iterbindings = pgDef.getIteratorBindings();
        if (iterbindings == null || iterbindings.size() < 1)
            return results;
        Iterator<UIPageDef.PageDefIteratorBinding> iterBindingIter = iterbindings.iterator();
        while (iterBindingIter.hasNext()) {
            UIPageDef.PageDefIteratorBinding iter = iterBindingIter.next();
            if (!isIterBindingReferenced(iter, pgDef)) {
                results.add(new CheckerResult(code, issue, subIssue, String.format("Iterator %s not used", iter.getId()), false));
            }

        }
        return results;
    }

    private boolean isIterBindingReferenced(UIPageDef.PageDefIteratorBinding iterbinding, UIPageDef pgdef) {
        if (iterbinding == null)
            return false;
        String id = iterbinding.getId();
        if (isEmpty(id))
            return false;
        ArrayList<UIPageDef.AttributeBindings> attrbindings = pgdef.getAttributeBindings();
        if (attrbindings != null && attrbindings.size() > 0) {
            Iterator<UIPageDef.AttributeBindings> attrBindingsIter = attrbindings.iterator();
            while (attrBindingsIter.hasNext()) {
                UIPageDef.AttributeBindings attr = attrBindingsIter.next();
                if (attr != null && id.equals(attr.getIterBindingId())) {
                    return true;
                }
            }
        }
        ArrayList<UIPageDef.PageDefTreeBindings> treebindings = pgdef.getPageDefTreeBindings();
        if (treebindings != null && treebindings.size() > 0) {
            Iterator<UIPageDef.PageDefTreeBindings> attrBindingsIter = treebindings.iterator();
            while (attrBindingsIter.hasNext()) {
                UIPageDef.PageDefTreeBindings attr = attrBindingsIter.next();
                if (attr != null && id.equals(attr.getIteratorName())) {
                    return true;
                }
            }
        }


        ArrayList<UIPageDef.PageDefListBindings> listbindings = pgdef.getListBindings();
        if (listbindings != null && listbindings.size() > 0) {
            Iterator<UIPageDef.PageDefListBindings> attrBindingsIter = listbindings.iterator();
            while (attrBindingsIter.hasNext()) {
                UIPageDef.PageDefListBindings attr = attrBindingsIter.next();
                if (attr != null && id.equals(attr.getIteratorBindingName())) {
                    return true;
                }
                if (attr != null && id.equals(attr.getListIter())) {
                    return true;
                }
            }
        }

        ArrayList<UIPageDef.ActionBinding> actionBindings = pgdef.getActionBindings();
        if (actionBindings != null && actionBindings.size() > 0) {
            Iterator<UIPageDef.ActionBinding> attrBindingsIter = actionBindings.iterator();
            while (attrBindingsIter.hasNext()) {
                UIPageDef.ActionBinding attr = attrBindingsIter.next();
                if (attr == null)
                    continue;
                String actionIter = attr.getIteratorBinding();
                if (isEmpty(actionIter))
                    continue;

                if (id.equals(actionIter)) {
                    return true;
                }

            }
        }

        return false;
    }

    private ArrayList<CheckerResult> checkAttributeUsages(ArrayList<UIPageDef.AttributeBindings> attrbindings) {
        String code = "B_1";
        if (attrbindings == null || attrbindings.size() < 1)
            return null;
        String issue = CheckerConstants.ISSUE_BINDINGS;
        String subIssue = "AttributeBindings not used";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();

        Iterator<UIPageDef.AttributeBindings> attrBindingsIter = attrbindings.iterator();
        while (attrBindingsIter.hasNext()) {
            try {
                UIPageDef.AttributeBindings attr = attrBindingsIter.next();
                String rowPrefixRegEx="(row\\.)?";
                String regEx = String.format("#\\{"+rowPrefixRegEx+"bindings\\.%s(\\.|})",attr.getId());
                
               // String regEx = String.format("#\\{"+rowPrefixRegEx+"bindings\\.%s\\.", attr.getId());
                //String regEx2 = String.format("#\\{"+rowPrefixRegEx+"bindings\\.%s}", attr.getId());
                if (!isMatch(regEx, sharedStringsContent) ) {
                    {
                        results.add(new CheckerResult(code, issue, subIssue, String.format("AttributeBinding: %s not used", attr.getId()),
                                                      false));
                    }
                }
            } catch (Exception ex) {
              
            }

        }
        return results;
    }

    private ArrayList<CheckerResult> checkTree(ArrayList<UIPageDef.PageDefTreeBindings> treebindings) {
        String code = "B_1";
        if (treebindings == null || treebindings.size() < 1)
            return null;
        String issue = CheckerConstants.ISSUE_BINDINGS;
        String subIssue = "TreeBinding not used";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();

        Iterator<UIPageDef.PageDefTreeBindings> attrBindingsIter = treebindings.iterator();
        while (attrBindingsIter.hasNext()) {
            try {
                UIPageDef.PageDefTreeBindings attr = attrBindingsIter.next();
                String regEx = String.format("ID=\"%s\"", attr.getId());
//                String regEx1 = String.format("#\\{bindings\\.%s\\.", attr.getId());
//                String regEx2 = String.format("#\\{bindings\\.%s}", attr.getId());
                String rowPrefixRegEx="(row\\.)?";
                String regEx2 = String.format("#\\{"+rowPrefixRegEx+"bindings\\.%s(\\.|})",attr.getId());
                
                // For trees they appear in the excel as Tree ID="treebinding.id"
                // But i am being conservative here and checking for bindings as well.
                if (!isMatch(regEx, sharedStringsContent)) {
                    if (!isMatch(regEx2, sharedStringsContent)) {
                        {
                            results.add(new CheckerResult(code, issue, subIssue, String.format("TreeBinding: %s not used", attr.getId()),
                                                          false));
                        }
                    }
                }
            } catch (Exception ex) {
            }

        }
        return results;
    }

    private ArrayList<CheckerResult> checkLists(ArrayList<UIPageDef.PageDefListBindings> listBindings) {
        String code = "B_1";
        if (listBindings == null || listBindings.size() < 1)
            return null;
        String issue = CheckerConstants.ISSUE_BINDINGS;
        String subIssue = "TreeBinding not used";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();

        Iterator<UIPageDef.PageDefListBindings> attrBindingsIter = listBindings.iterator();
        while (attrBindingsIter.hasNext()) {
            try {
                UIPageDef.PageDefListBindings attr = attrBindingsIter.next();
                String regEx = String.format("ID=\"%s\"", attr.getId());
                String rowPrefixRegEx="(row\\.)?";
                String regEx2 = String.format("#\\{"+rowPrefixRegEx+"bindings\\.%s(\\.|})",attr.getId());
                // For trees they appear in the excel as Tree ID="treebinding.id"
                // But i am being conservative here and checking for bindings as well.
                if (!isMatch(regEx, sharedStringsContent)) {
                    if (!isMatch(regEx2, sharedStringsContent)) {
                        {
                            results.add(new CheckerResult(code, issue, subIssue, String.format("ListBinding: %s not used", attr.getId()),
                                                          false));
                        }
                    }
                }
            } catch (Exception ex) {
            }

        }
        return results;
    }

    private boolean isMatch(String regEx, String contents) {
        Pattern regExP = Pattern.compile(regEx);
        Matcher m = regExP.matcher(contents);
        return m.find();
    }

    private ArrayList<CheckerResult> checkMethodActionUsages(ArrayList<UIPageDef.MethodActionBinding> actionbindings) {
        String code = "B_1";
        if (actionbindings == null || actionbindings.size() < 1)
            return null;
        String issue = CheckerConstants.ISSUE_BINDINGS;
        String subIssue = "ActionBindings not used";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();

        Iterator<UIPageDef.MethodActionBinding> attrBindingsIter = actionbindings.iterator();
        while (attrBindingsIter.hasNext()) {
            try {
                UIPageDef.MethodActionBinding attr = attrBindingsIter.next();
                String regEx = String.format("ID=\"%s\"", attr.getId());
                if (!isMatch(regEx, sharedStringsContent)) {
                    results.add(new CheckerResult(code, issue, subIssue, String.format("MethodAction: %s not used", attr.getId()), false));
                }
            } catch (Exception ex) {
            }

        }
        return results;
    }

    private ArrayList<CheckerResult> checkActionUsages(ArrayList<UIPageDef.ActionBinding> actionbindings) {
        String code = "B_1";
        if (actionbindings == null || actionbindings.size() < 1)
            return null;
        String issue = CheckerConstants.ISSUE_BINDINGS;
        String subIssue = "ActionBindings not used";
        ArrayList<CheckerResult> results = new ArrayList<CheckerResult>();

        Iterator<UIPageDef.ActionBinding> attrBindingsIter = actionbindings.iterator();
        while (attrBindingsIter.hasNext()) {
            try {
                UIPageDef.ActionBinding attr = attrBindingsIter.next();
                String regEx = String.format("ID=\"%s\"", attr.getId());
                Pattern regExP = Pattern.compile(regEx);
                Matcher m = regExP.matcher(sharedStringsContent);
                if (!m.find()) {
                    results.add(new CheckerResult(code, issue, subIssue, String.format("Action: %s not used", attr.getId()), false));
                }
            } catch (Exception ex) {
            }

        }
        return results;
    }

    public static HashSet<String> getListOfPageDefsInWorkbook(ADFDiWorkbook workbook) {
        if (workbook == null)
            return null;
        ArrayList<ADFDiMetadata> sheetMetas = workbook.getComponentsOfType(ADFDiMetadata.META_WORKSHEET);
        if (sheetMetas == null || sheetMetas.size() < 1)
            return null;

        HashSet<String> pageDefs = new HashSet<String>();
        Iterator<ADFDiMetadata> sheetIter = sheetMetas.iterator();
        while (sheetIter.hasNext()) {
            ADFDiMetadata meta = sheetIter.next();
            if (meta instanceof Worksheet) {
                Worksheet sheetMeta = (Worksheet)meta;
                String pdefName = sheetMeta.getPageDefName();
                if (pdefName != null && !pdefName.trim().equals("")) {
                    pdefName = pdefName.trim();
                    if (pdefName.startsWith("["))
                        continue;
                    if (pdefName.contains("_")) {
                        int lastUnderscore = pdefName.lastIndexOf("_");
                        pdefName = pdefName.substring(lastUnderscore + 1);
                    }
                    pageDefs.add(pdefName + ".xml");
                }
            }
        }
        return pageDefs;

    }

    public static HashSet<String> getAbsolutePageDefPaths(HashSet<String> pageDefNames, String excelPath) {
        HashSet<String> absolutePaths = new HashSet<String>();

        String pageDefBaseDir = getPageDefBaseDir(excelPath);
        if (pageDefBaseDir == null || pageDefBaseDir.trim().equals(""))
            return null;
        try {
            findFileAbsolutePage(pageDefBaseDir, pageDefNames, absolutePaths);
        } catch (Exception ex) {
        }
        return absolutePaths;

    }

    public static void findFileAbsolutePage(String searchPath, HashSet<String> pageDefNames, HashSet<String> pageDefAbsPaths) throws Exception {
        
        File searchPathFile = new File(searchPath);
        if (searchPathFile == null || !searchPathFile.exists())
            return;
        if(searchPath.contains(".ade_path"))
            return;
        File[] listfiles = searchPathFile.listFiles();
        if (listfiles == null || listfiles.length < 1)
            return;
        int fileLen = listfiles.length;
        for (int i = 0; i < fileLen; i++) {
            File f = listfiles[i];
            if (f.isFile()) {
                String obtName = f.getName();
                if (obtName != null && pageDefNames.contains(obtName.trim())) {
                    pageDefAbsPaths.add(f.getAbsolutePath());
                }
            } else if (f.isDirectory()) {
                findFileAbsolutePage(f.getAbsolutePath(), pageDefNames, pageDefAbsPaths);
            }
        }
    }

    public static String getPageDefBaseDir(String excelFilePath) {
        // TODO: Can actually just get it from the workbook properties application home folder etc. but dont know how reliable that will be

        int baseIndex = 0;
        baseIndex = excelFilePath.indexOf("/public_html/");
        if (baseIndex < 0)
            baseIndex = excelFilePath.indexOf("/src/");
        if (baseIndex < 0)
            baseIndex = excelFilePath.indexOf("/adfmsrc/");
        // Error
        if (baseIndex < 0) {
            System.out.println("[ERROR] Cannot find project base directory for excel : " + excelFilePath);
            return "";
        }
        String baseDir = excelFilePath.substring(0, baseIndex);
        return baseDir + "/adfmsrc/";
    }
    
}
