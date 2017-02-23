package oracle.apps.serialization;

import java.io.BufferedWriter;

import java.io.File;
import java.io.FileWriter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.JavaFileParserHelper;

public class SerializationJavaScannerV2 {
    
   HashSet<String> knownSerializableTypes;
      
   //varName -> Type
   HashMap<String,String> nonTransientMemberContainers = new HashMap<String,String>(); //member variables that are collections and not transient
   
   HashMap<String,String> nonSerializableMembers = new HashMap<String,String>();
   
   HashMap<String,String> memberVariables = new HashMap<String,String>(); //all member variables and thier types 
   HashMap<String,String> localVariables = new HashMap<String,String>(); //all local variables and thier types --> refresh on each method
   
   //process Imports to get the fully qualified type
   HashMap<String,String> fullyQualifiedObjTypes = new HashMap<String,String>();
   
   Pattern memberVariableDeclarationRE = Pattern.compile("(?:(?:public|private|protected|transient|static|final)\\s+)*(\\w+)(?:<.+?>)?(?:\\[\\])*\\s+(\\w+)"); 
   Pattern localVariableDeclarationRE = Pattern.compile("(\\w+)(?:<.+>)?(?:\\[\\])?\\s+(\\w+)\\s*="); //local variables have to be initialized
   Pattern localVariableDeclarationRE1 = Pattern.compile("(\\w+)(?:<.+>)?(?:\\[\\])?\\s+(\\w+)\\s*;"); //local variables not initialized initialized
   Pattern variableInitializationRE = Pattern.compile("new\\s+\\w+(?:<.+>)?\\s*\\((.*)\\)");
   
   Pattern numericLiteral = Pattern.compile("\\d+(\\.\\d*)?");
   Pattern importPattern = Pattern.compile("import\\s+(.*?)\\s*;");
   
   static BufferedWriter writer;
   static BufferedWriter fileWriter;
   static int java_files = 0;
   static boolean append = false;
    
   private void initizializeContainers(){
       String elements[] = {"String", "long", "int", "double", "float", "Long", "Integer", "Double", "BigInteger", 
                            "short", "boolean", "byte", "char", "BigDecimal", "Character", "Short", "Number", "Float",
                            "Double", "Boolean", "Byte", "Date", "Timestamp", "oracle.jbo.Key", "javax.faces.model.SelectItem",
                            "oracle.apps.financials.generalLedger.inquiry.balances.publicModel.util.InquirySlice", "ComponentReference"};
       
       knownSerializableTypes = new HashSet(Arrays.asList(elements));
    }
   
   public static void main(String[] args) throws Exception{             
     
        String sCrawlDir = args[0];
       
        if (sCrawlDir == null || sCrawlDir.trim().equals("")) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }
        
        
        if(args.length > 1)
          append = new Boolean(args[1].trim());
        
        writer = new BufferedWriter(new FileWriter("SerializationJavaScan.csv", append));
        if(!append)
            writer.write("Family,Module,Product,Filename,Label,container Variable, Container Type, Inserted Object Name, Inserted Object Type, Line, Comment\n");
        
        fileWriter = new BufferedWriter(new FileWriter("NonSerializableMembers.csv",append));
         if(!append)
           fileWriter.write("Family,Module,Product,Filename,Label,Member Variable, Var Type\n");
     
        
        SerializationJavaScannerV2 serScanner = new SerializationJavaScannerV2();
        serScanner.initizializeContainers();
        serScanner.crawlDirectory(sCrawlDir);
        
        writer.close();
        fileWriter.close();
   }
   
  private void processFile(File fName) {
      
    memberVariables = new HashMap<String,String>(); 
    localVariables = new HashMap<String,String>();
    nonTransientMemberContainers = new HashMap<String,String>();        
    
    try {                           
        
      JavaFileParserHelper javaHelper = new JavaFileParserHelper(fName.getAbsolutePath());
      javaHelper.removeAllComments();
      String fileContents = javaHelper.getFileWithoutCommentsReadOnly();        
     
      String instanceVariableSection = getInstanceVariablesSection(fileContents);        
               
      boolean implementsSerializable = instanceVariableSection.contains("implements Serializable");
      
      if(!implementsSerializable) return;
        
      processImports(fileContents);
        
      populateMemberVariables(instanceVariableSection, fName);
        
      processMethods(fileContents,fName);
  
    } catch(Exception e) {
      e.printStackTrace();
    }
  }        

  
  private void populateMemberVariables(String instanceVariableSection, File f) throws Exception{
      
      String[] lines = instanceVariableSection.split("\\n");
      
      for(int i = 1; i< lines.length; i++) {
          
        Matcher m = memberVariableDeclarationRE.matcher(lines[i]);
        if(m.find()) {
          String varType = m.group(1);
          String varName = m.group(2);
          memberVariables.put(varName,varType);
          
          boolean isCollection = isCollectionClass(varType);
          boolean isTransient = lines[i].contains("transient ");
          boolean isStatic = lines[i].contains("static ");
          
          if(!isTransient && !isStatic && !isCollection && !knownSerializableTypes.contains(varType)) {
            fileWriter.write(getFileNameInfo(f.getCanonicalPath()) + varName + "," + varType + "\n");
          }
          
          if(!isTransient && !isStatic && isCollection) {
              
              nonTransientMemberContainers.put(varName, varType);
              String[] parts = lines[i].split("=");
              
              if(parts.length == 2) {
                String varInitializationPart = parts[1];
                Matcher m1 = variableInitializationRE.matcher(varInitializationPart);
                if(m1.find()) {
                    
                  String initObjectsString = m1.group(1);
                  if(initObjectsString == null || initObjectsString.equals("")) continue;
                  String[] initObjects = initObjectsString.split(",");
                  
                  for(int j=0; j < initObjects.length ;j++) {                   
                    if(isLiteral(initObjects[j]))
                        continue;
                      
                    if(isValidInsertedObjectPattern(initObjects[j]))
                        continue;
                    
                    String objType = "Unknown";
                      
                    if(memberVariables.containsKey(initObjects[j])) {
                        objType = memberVariables.get(initObjects[j]);
                        if(knownSerializableTypes.contains(objType))
                           continue;
                    }
                      
                    writer.write(getFileNameInfo(f.getCanonicalPath()) + varName + "," + varType + 
                                                 "," + initObjects[j] + "," + objType + "," + lines[i].replace(",","COMMA").trim() + ",During Initialization\n");
                  }                  
                } //if m1.find
              } //if parts.length
          } //if isNonTransientCollection
        } // if m.find          
      }    //for each line
  }
  
  private void processMethods(String fileContents, File fName) throws Exception{
    
    String[] lines = fileContents.split("\\n");
    String method = "";
    int classStart = 0;
    int methodStart = 0;
    int num_braces =0;
    int methodEnd = 0;
      
    boolean publicClassEncountered = false;
       
    for(int i = 0; i < lines.length; i++) {
        if(lines[i].contains("public class"))
          publicClassEncountered = true;
        if(lines[i].contains("{")) 
            num_braces++;
        if(lines[i].contains("}"))
            num_braces--;
        if(num_braces == 1 && classStart == 0 && publicClassEncountered)               
               classStart = i;              
        
        if(num_braces == 2 && methodStart == 0 && classStart != 0) {             
           methodStart = i;        
        }
        if(methodStart != 0 && num_braces == 1){
            methodEnd= i;
            for(int j = methodStart; j <= methodEnd; j++) {         
                method += lines[j] + "\n";
            }           
            parseMethod(method, fName, methodStart);  
            methodStart = 0;
            method = "";
        }
    }       
  }
  
  private void parseMethod(String method, File f, int methodStart) throws Exception{
      
    localVariables = new HashMap<String,String>(); //reset localVariables section at the begining of method
    String[] lines = method.split("\\n");    
    
    for(int i =0; i < lines.length;i++) {
        
      String varName = "";
      String varType = "";           
      String objType = "";
      String objName = "";
        
      Matcher m = localVariableDeclarationRE.matcher(lines[i]);
     
      if(m.find()) {
        varType = m.group(1);
        varName = m.group(2);
        localVariables.put(varName,varType);
      } 
      Matcher mm = localVariableDeclarationRE1.matcher(lines[i]);
      if(mm.find()) {
        varType = mm.group(1);
        varName = mm.group(2);
        localVariables.put(varName,varType);
      }
             
      String memContainer = lineContainsMember(lines[i]);
        
      if(memContainer != null) { //lines contains non-transient member variable of collection type
          
        String regex1 = memContainer + "\\.(?:add|put)\\((.*)\\)";
        String regex2 = memContainer + "\\.(?:addAll|putAll)\\((.*)\\)";
        Pattern p1 = Pattern.compile(regex1);
        Pattern p2 = Pattern.compile(regex2);
        
        Matcher m1 = p1.matcher(lines[i]);
        Matcher m2 = p2.matcher(lines[i]);
        
        if(m2.find()) {            
            objName = m2.group(1);
            
          if(lines[i].contains(memContainer + ".put(")) {
              
            if(!lines[i].trim().endsWith(";")) {
               String fullLine = lines[i].trim() + lines[i+1].trim();
               m2 = p2.matcher(fullLine);
               if(m2.find())
                   objName = m2.group(1);
            }
                
            String[] parts = objName.split(",");
            if(parts.length == 2)
               objName = parts[1].trim();
            else
               objName= parts[0].trim();
          }
            
            objType = "Collection";
            writer.write(getFileNameInfo(f.getCanonicalPath()) + memContainer + "," + nonTransientMemberContainers.get(memContainer) + 
                                       "," + objName + "," + objType + "," + (i+methodStart+1) + ",\n");
          
        } else if(m1.find()){
            
         objName = m1.group(1);        
         
         if(lines[i].contains(memContainer + ".put(")) {
             
           if(!lines[i].trim().endsWith(";")) {
              String fullLine = lines[i].trim() + lines[i+1].trim();
              m1 = p1.matcher(fullLine);
              if(m1.find())
                  objName = m1.group(1);
           }
               
           String[] parts = objName.split(",");
           if(parts.length == 2)
              objName = parts[1].trim();
           else
              objName= parts[0].trim();
         }
         
         if(isLiteral(objName))
             continue;
         
         if(isValidInsertedObjectPattern(objName))
             continue;
         
          if(localVariables.containsKey(objName)) {
              objType = localVariables.get(objName);
          } else if(memberVariables.containsKey(objName)) {
              objType = memberVariables.get(objName);              
          }
          
          if(objType != null && !objType.equals("")) {
          String objTypeClass = fullyQualifiedObjTypes.get(objType);
          if(objTypeClass != null && !objTypeClass.equals(""))
              objType = objTypeClass;
          }
          if(knownSerializableTypes.contains(objType))
             continue;
         
          writer.write(getFileNameInfo(f.getCanonicalPath()) + memContainer + "," + nonTransientMemberContainers.get(memContainer) + 
                                     "," + objName + "," + objType + "," + (i+methodStart+1) + ",\n");  
        }
      }
        
    }
    
  }
  
  private boolean isLiteral(String s){
   
    Matcher m = numericLiteral.matcher(s);
    
    if( m.matches())
        return true;
    
    if(s.equals("null"))
        return true;
    if(s.equals("true") || s.equals("false"))
        return true;
    
    if(s.matches("\".*\""))
        return true;
    if(s.matches("'\\w'"))
        return true;
    
    return false;
  }
  
  private boolean isValidInsertedObjectPattern(String s){
      
    if(s.contains("getPageFlowScope().get"))
        return true;
    
    if(s.contains("toString()"))
        return true;
    
    return false;
  }
  
  private String lineContainsMember(String line){
      for(Iterator<String> it = nonTransientMemberContainers.keySet().iterator(); it.hasNext();){
        String varName = it.next();
        if(line.contains(varName + "."))
            return varName;
      }
      return null;
  }
   
   
  private void crawlDirectory(String path) 
    {
        if(path.contains(".ade_path") || path.contains("/classes/"))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
            
        if(listOfFiles == null)
            return;
        
        for(int i = 0; i <listOfFiles.length; i++)
        {
            if(listOfFiles[i].isFile()) 
            {
                String sName = listOfFiles[i].getAbsolutePath();              
                if(fileOfInterest(sName))               
                    processFile(listOfFiles[i]);                           
            }
            else if(listOfFiles[i].isDirectory()) 
                crawlDirectory(listOfFiles[i].getAbsolutePath());          
            else 
                System.out.println("ERROR:  node is neither file or directory: "+listOfFiles[i]);          
        }      
    }

  private boolean fileOfInterest(String sName) 
  {
      if (sName.endsWith(".java") && sName.contains("/bean/")) {
        java_files++;
        return true;
      }
    return false;
  }
   
   
   ///////////////////////////////////////////////////////////////////////////////////////////////////////
   private boolean isCollectionClass(String varType) {
       
     if(varType.endsWith("Set") ||
         varType.endsWith("List") ||
         varType.endsWith("Map") ||
         varType.endsWith("Queue") ||
         varType.equals("Hashtable") ||
         varType.equals("Dictionary"))
         return true;
     
     return false;
   }
   
   
  private String getInstanceVariablesSection(String fileContents) {
      
      String result = "";
      int classStart = 0;
      int methodStart = 0;
      int num_braces =0;
      boolean publicClassEncountered = false;
      
      String[] lines = fileContents.split("\\n");
      
      for(int i = 0; i < lines.length; i++) {
          if(lines[i].contains("public class"))
            publicClassEncountered = true;
          if(lines[i].contains("{")) 
              num_braces++;
          if(lines[i].contains("}"))
              num_braces--;
          if(num_braces == 1 && classStart == 0 && publicClassEncountered)               
                 classStart = i;              
          
          if(num_braces == 2 && methodStart == 0 && classStart != 0) {             
             methodStart = i;
             break;             
          }
      }
      
      for(int j = classStart; j < methodStart; j++) {         
          result += lines[j] + "\n";
      }
      return result;
  }
  
  private void processImports(String fileContents) {
      
    int classStart = 0;
    
    String[] lines = fileContents.split("\\n");
    
    for(int i = 0; i < lines.length; i++) {  
        if(lines[i].contains("{")) {                     
           classStart = i;
           break;             
        }
    }
    
    
    for(int j = 0; j < classStart; j++) {         
        
       if(!lines[j].contains("import ")) continue;

       Matcher m = importPattern.matcher(lines[j]);
        if(m.find()){
          String importedFullClass = m.group(1).trim();
          if(importedFullClass.startsWith("java.")) continue;
          String[] importedClassParts = importedFullClass.split("\\.");
          String importedClass = importedClassParts[importedClassParts.length-1];
          fullyQualifiedObjTypes.put(importedClass, importedFullClass);
        }
    }
   
    
  }
  
  private String getFileNameInfo(String absPath) {
    int i = 0;
    String family = "";
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
    String pathAfterFusionApps = absPath.substring(i);
    String[] parts = pathAfterFusionApps.split("/");
    String module = "";
    String product = "";
    
    if(parts == null) return blankString;
    
    if(absPath.contains("fusionapps/")) {
        int partsLength = parts.length;
        if(partsLength>1)
          family = parts[1].trim().toUpperCase();  
        if(partsLength>3)
          module = parts[3].trim();
        if(partsLength>4)
          product = parts[4].trim().toUpperCase();
        
    }
    if(absPath.contains("fsm/")) {
      int partsLength = parts.length;
      if(partsLength>2)
        module = parts[2].trim();
      if(partsLength>3)
        product = parts[3].trim().toUpperCase();
    }
       
    if(i<1)
       return family + "," + module + "," + product + "," + pathAfterFusionApps + ",," ;
    
    String path_before_fusionapps = absPath.substring(0, i-1);
    parts = path_before_fusionapps.split("/");
    if(parts==null || parts.length<2)
      return blankString;
    String label = parts[parts.length -1];
    String[] series = parts[parts.length -2].split("\\.");
    if(series==null || series.length<2)
       label = " ";
    else if(absPath.contains("fusionapps/"))
       label = series[1] + "_" + label;
    else if(absPath.contains("fsm/"))
       label = series[0] + "_" + label;
    
   return family + "," + module + "," + product + "," + pathAfterFusionApps + "," + label + ",";
  }
  
  private boolean isEmpty(String str) {
      if (str == null || str.trim().equals(""))
          return true;
      return false;
  }
}







