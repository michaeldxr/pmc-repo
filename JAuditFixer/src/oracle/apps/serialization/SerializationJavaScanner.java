package oracle.apps.serialization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Writer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.financials.commonModules.spreadsheetHelper.Cell;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellLocation;
import oracle.apps.financials.commonModules.spreadsheetHelper.CellValue;
import oracle.apps.financials.commonModules.spreadsheetHelper.Workbook;
import oracle.apps.financials.commonModules.spreadsheetHelper.Worksheet;
import oracle.apps.financials.commonModules.spreadsheetHelper.XLSXCreator;

import oracle.apps.helpers.JavaFileParserHelper;

public class SerializationJavaScanner {
    
    static Workbook m_myBook;
    static  Worksheet m_attributesSheet;
    
    static Writer outputFileWriter;   
    static int java_files = 0;
    static int row = 2;
    
    Pattern pageFlow = Pattern.compile("set\\w*?PageFlowScope\\w*\\(", Pattern.CASE_INSENSITIVE);
    Pattern pageFlow_put = Pattern.compile("set\\w*?PageFlowScope\\w*\\(\\s*\"\\w+\"\\s*,\\s*(\"\\w*\"|null|true|false)", Pattern.CASE_INSENSITIVE);
    
//    Pattern componentRefViolation = Pattern.compile("private\\s+Rich\\w+\\s+\\w+");
//    Pattern map = Pattern.compile("(HashMap|Map).*\\s+(\\w+)", Pattern.DOTALL);
//    Pattern bindingContainer = Pattern.compile("private\\s+BindingContainer");    
    Pattern singleLineCommentsRE = Pattern.compile("^\\s*//");
    Pattern multiLineCommentsRE = Pattern.compile("^\\s*/\\*");
    
    Pattern scopeMapObject1 = Pattern.compile("(\\w+)\\s*=\\s*.*(getPageFlowScope)",Pattern.DOTALL);
    Pattern scopeMapObject2 = Pattern.compile("(\\w+)\\s*=\\s*.*(getViewScope)",Pattern.DOTALL);
    Pattern scopeMapObject3 = Pattern.compile("(\\w+)\\s*=\\s*.*(getRequestScope)",Pattern.DOTALL);
    Pattern scopeMapObject4 = Pattern.compile("(\\w+)\\s*=\\s*.*(getSessionScope)",Pattern.DOTALL);
    
    Pattern pageScope = Pattern.compile("getPageFlowScope.*?\\.put\\(", Pattern.CASE_INSENSITIVE);
    Pattern pageScope_put = Pattern.compile("getPageFlowScope.*?\\.put\\(\\s*\"\\w+\"\\s*,\\s*\"\\w+\"", Pattern.CASE_INSENSITIVE);
  
    Pattern viewScope = Pattern.compile("getViewScope.*?\\.put\\(", Pattern.CASE_INSENSITIVE);
    Pattern requestScope = Pattern.compile("getRequestScope.*?\\.put\\(", Pattern.CASE_INSENSITIVE);
    Pattern sessionScope = Pattern.compile("getSessionScope.*?\\.put\\(", Pattern.CASE_INSENSITIVE);
    
    public static void main(String[] args){             
     
        String sCrawlDir = args[0];
        
        if (sCrawlDir == null || sCrawlDir.trim().equals("")) {
            System.out.println("Crawl directory has to be specified");
            System.exit(1);
        }
        
        m_myBook = new Workbook("VO Attributes");

        m_attributesSheet = new Worksheet("Attributes");
        m_myBook.addWorksheet(m_attributesSheet);

        m_attributesSheet.addCell(new Cell(new CellLocation("A1"), new CellValue("Family")));
        m_attributesSheet.addCell(new Cell(new CellLocation("B1"), new CellValue("Module")));
        m_attributesSheet.addCell(new Cell(new CellLocation("C1"), new CellValue("Product")));
        m_attributesSheet.addCell(new Cell(new CellLocation("D1"), new CellValue("FileName")));
        m_attributesSheet.addCell(new Cell(new CellLocation("E1"), new CellValue("Label")));
        m_attributesSheet.addCell(new Cell(new CellLocation("F1"), new CellValue("PageFlow Scope")));
        m_attributesSheet.addCell(new Cell(new CellLocation("G1"), new CellValue("View Scope")));
        m_attributesSheet.addCell(new Cell(new CellLocation("H1"), new CellValue("Request Scope")));
        m_attributesSheet.addCell(new Cell(new CellLocation("I1"), new CellValue("Session Scope")));
        m_attributesSheet.addCell(new Cell(new CellLocation("J1"), new CellValue("implements Serializable?")));
        
        System.out.println("Analyzing all files in directory:" + sCrawlDir);

        SerializationJavaScanner scanner = new SerializationJavaScanner();

      try {         
          outputFileWriter = new BufferedWriter(new FileWriter("serialization_java_scan.csv") );
//          outputFileWriter.write("Family, Module, SubModule, Filename, Label, PageFlowScope, ViewScope, Component Ref Pattern followed?," +
//              "Component Ref Pattern violated?, implements Serializable?, contains HashMap?, contains Binding Container?\n");    
          outputFileWriter.write("Family, Module, SubModule, Filename, Label, PageFlowScope, ViewScope, RequestScope, SessionScope, implements Serializable?\n");        
          scanner.crawlDirectory(sCrawlDir);       
          
          System.out.println("Number of Java files processed: "+ java_files); 
          outputFileWriter.close();
          
          FileOutputStream out = null;

          out = new FileOutputStream("serialization_java_scan.xlsx");
          XLSXCreator.create(m_myBook, out);
        
      } catch (Exception e) {
          e.printStackTrace();               
      }
    }  
    
    private void processFile(File fName) {
      try {           
          
        JavaFileParserHelper javaHelper = new JavaFileParserHelper(fName.getAbsolutePath());
        javaHelper.removeAllComments();
        String fileContents = javaHelper.getFileWithoutCommentsReadOnly();
          
        String instanceVariableSection = getInstanceVariablesSection(javaHelper.getFileContents());
        String viewScopeComment = "";
        String pageScopeComment = "";
        String requestScopeComment = "";
        String sessionScopeComment = "";
          
        boolean containsPageFlowScope = fileContents.contains("getPageFlowScope()");
        if(!containsPageFlowScope){
              Matcher m = pageFlow.matcher(fileContents);
              containsPageFlowScope = m.find();
        }
        
        if(containsPageFlowScope)
            pageScopeComment = getPageScopeLines(javaHelper.getFileContents());         
      
        boolean containsViewScope = fileContents.contains("getViewScope()");
          
        if(containsViewScope)
          viewScopeComment = getViewScopeLines(javaHelper.getFileContents());
          
        boolean containsRequestScope = fileContents.contains("getRequestScope()");
          
        if(containsRequestScope)
          requestScopeComment = getRequestScopeLines(javaHelper.getFileContents());
          
        boolean containsSessionScope = fileContents.contains("getSessionScope()");
          
        if(containsSessionScope)
          sessionScopeComment = getSessionScopeLines(javaHelper.getFileContents());
          
        //boolean ComponentRefPatternFollowed = fileContents.contains("ComponentReference<");
        
//        Matcher m1 = componentRefViolation.matcher(instanceVariableSection);
//        boolean ComponentRefPatternViolated = m1.find();
          
        boolean implementsSerializable = instanceVariableSection.contains("implements Serializable");
        
//        Matcher m2  = map.matcher(instanceVariableSection);
//        boolean containsMap = m2.find();
//    
//        Matcher m3 = bindingContainer.matcher(instanceVariableSection);
//        boolean containsBindingContainer = m3.find();
          
        if(!containsPageFlowScope && !containsViewScope && !containsRequestScope && !containsSessionScope /*&& !ComponentRefPatternFollowed && !ComponentRefPatternViolated
            && !implementsSerializable && !containsMap && !containsBindingContainer*/)
            return;
          
//        outputFileWriter.write(getFileNameInfo(fName.getCanonicalPath()) + containsPageFlowScope
//            + "," + containsViewScope + "," + ComponentRefPatternFollowed + "," + 
//            ComponentRefPatternViolated + "," +  implementsSerializable + "," +  containsMap + "," +  containsBindingContainer + "\n");
//          
          outputFileWriter.write(getFileNameInfo(fName.getCanonicalPath()) + containsPageFlowScope
           + "," + containsViewScope + "," + containsRequestScope + "," + containsSessionScope + "," + implementsSerializable + "\n");
          
          String c6 = getCellLocation(5, row);
          String c7 = getCellLocation(6, row);
          String c8 = getCellLocation(7, row);
          String c9 = getCellLocation(8, row);
          String c10 = getCellLocation(9, row);

          
          row++;
          
          m_attributesSheet.addCell(new Cell(new CellLocation(c6), new CellValue(String.valueOf(containsPageFlowScope))));
          m_attributesSheet.addCell(new Cell(new CellLocation(c7), new CellValue(String.valueOf(containsViewScope))));
          m_attributesSheet.addCell(new Cell(new CellLocation(c8), new CellValue(String.valueOf(containsRequestScope))));
          m_attributesSheet.addCell(new Cell(new CellLocation(c9), new CellValue(String.valueOf(containsSessionScope))));
          m_attributesSheet.addCell(new Cell(new CellLocation(c10), new CellValue(String.valueOf(implementsSerializable))));

          
          if(!pageScopeComment.equals(""))
            m_attributesSheet.createComment(pageScopeComment, c6, 10, 40);
          
          if(!viewScopeComment.equals(""))
            m_attributesSheet.createComment(viewScopeComment, c7, 10, 40);
          
         if(!requestScopeComment.equals(""))
          m_attributesSheet.createComment(requestScopeComment, c8, 10, 40);
          
         if(!sessionScopeComment.equals(""))
          m_attributesSheet.createComment(sessionScopeComment, c9, 10, 40);
      } 
      catch (Exception e) {
          e.printStackTrace();        
          System.out.println("ERROR:  Error while processing file.  " +
              "review carefully if the script updated it. " +fName);
          return;
      }
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
        if (sName.endsWith(".java")) {
          java_files++;
          return true;
        }
      return false;
    }
  
    private String getFileNameInfo(String absPath) {
      int i = absPath.indexOf("fusionapps/");
      String pathAfterFusionApps = absPath.substring(i);
      String[] parts = pathAfterFusionApps.split("/");
      String family = parts[1].toUpperCase();
      String module = parts[3];
      String product = parts[4].toUpperCase();
      
      String path_before_fusionapps = absPath.substring(0, i-1);
      parts = path_before_fusionapps.split("/");
      String label = parts[parts.length -1];
      String[] series = parts[parts.length -2].split("\\.");
      label = series[1] + "_" + label;
      
      String c1 = getCellLocation(0, row);
      String c2 = getCellLocation(1, row);
      String c3 = getCellLocation(2, row);
      String c4 = getCellLocation(3, row);
      String c5 = getCellLocation(4, row);
      
      m_attributesSheet.addCell(new Cell(new CellLocation(c1), new CellValue(family)));
      m_attributesSheet.addCell(new Cell(new CellLocation(c2), new CellValue(module)));
      m_attributesSheet.addCell(new Cell(new CellLocation(c3), new CellValue(product)));
      m_attributesSheet.addCell(new Cell(new CellLocation(c4), new CellValue(pathAfterFusionApps)));
      m_attributesSheet.addCell(new Cell(new CellLocation(c5), new CellValue(label)));
      
      return family + "," + module + "," + product + "," + pathAfterFusionApps + "," + label + ",";
    }
    
    private String getInstanceVariablesSection(String[] lines) {
        String result = "";
        int classStart = 0;
        int methodStart = 0;
        int num_braces =0;
        
        for(int i = 0; i < lines.length; i++) {
            if(lines[i].contains("{")) 
                num_braces++;
            if(num_braces == 1 && classStart == 0) 
                classStart = i;
            if(num_braces == 2 && methodStart == 0) {
                methodStart = i;
                break;
            }
        }
        for(int j = classStart; j < methodStart; j++) {
            Matcher m1 = singleLineCommentsRE.matcher(lines[j]);
            Matcher m2 = multiLineCommentsRE.matcher(lines[j]);
            if(m1.find() ) continue;
            if(m2.find()) {
                while(!lines[j].contains("*/")) j++;
                continue;
            }
            result += lines[j];
        }
        return result;
    }
    
    private String getCellLocation(int col, int row) 
    {
        return getColumn(col) + row;
    }
    
    private String getColumn(int col) 
    {
        int r = col % 26;
        int x = col / 26;
        
        String retVal = "";
        
        if(col >= 26)
        {
            retVal = getColumn(x-1);
        }
        
        return retVal +getSingleColumnDigit(r);
    }
    
    private String getSingleColumnDigit(int col) 
    {
        assert(col >= 0 && col < 26);
        
        int valueOfA  = 65;
        char c = (char)(valueOfA+col);
        return String.valueOf(c);
    }
    
    private String getPageScopeLines(String[] lines) {
        String comment = "";
        String pageFlowObj = null;
        
        for(int i = 0; i < lines.length; i++) {
            
            Matcher m1 = singleLineCommentsRE.matcher(lines[i]);
            Matcher m2 = multiLineCommentsRE.matcher(lines[i]);
            if(m1.find()) continue;
            if(m2.find()) {
                while(!lines[i].contains("*/") && i < lines.length-1) i++;
                continue;
            }
            String line = "";
            while(!lines[i].contains(";") && i < lines.length-1){
                line += lines[i];
                i++;
            }
            line+=lines[i];
            
            m1 = pageFlow.matcher(line);
            m2 = scopeMapObject1.matcher(line);
            
            Matcher m3 = pageScope.matcher(line);
            
            if(m1.find()) {
                Matcher m11  = pageFlow_put.matcher(line);
                if(!m11.find())
                  comment += line.trim() + "\n";
            }
            if(m3.find()) {
                Matcher m33  = pageScope_put.matcher(line);
                if(!m33.find())
                  comment += line.trim() + "\n";
            }
            if(m2.find())
                pageFlowObj = m2.group(1);
            if(pageFlowObj != null && line.contains(pageFlowObj +".put")) {
                Pattern p = Pattern.compile(pageFlowObj +".put\\(\\s*\"\\w+\"\\s*,\\s*\"\\w+\"");
                Matcher m = p.matcher(line);
                if(!m.find())
                  comment += line.trim() + "\n";
            }
        }
        return comment;
    }
    
    private String getViewScopeLines(String[] lines) {
        String comment = "";
        String viewScopeObj = null;
        
        for(int i = 0; i < lines.length; i++) {
            
            Matcher m1 = singleLineCommentsRE.matcher(lines[i]);
            Matcher m2 = multiLineCommentsRE.matcher(lines[i]);
            if(m1.find()) continue;
            if(m2.find()) {
                while(!lines[i].contains("*/") && i < lines.length-1) i++;
                continue;
            }
            String line = "";
            while(!lines[i].contains(";") && i < lines.length-1){
                line += lines[i];
                i++;
            }
            line+=lines[i];
            
            m1 = viewScope.matcher(line);
            m2 = scopeMapObject2.matcher(line);
            
            if(m1.find())
                comment += line.trim() + "\n";            
            if(m2.find() && viewScopeObj == null)
                viewScopeObj = m2.group(1);
            
            if(viewScopeObj != null && line.contains(viewScopeObj +".put"))
                comment += line;
        }
        return comment;
    }
    
  private String getRequestScopeLines(String[] lines) {
      String comment = "";
      String requestScopeObj = null;
      
      for(int i = 0; i < lines.length; i++) {
          
          Matcher m1 = singleLineCommentsRE.matcher(lines[i]);
          Matcher m2 = multiLineCommentsRE.matcher(lines[i]);
          if(m1.find()) continue;
          if(m2.find()) {
              while(!lines[i].contains("*/") && i < lines.length-1) i++;
              continue;
          }
          String line = "";
          while(!lines[i].contains(";") && i < lines.length-1){
              line += lines[i];
              i++;
          }
          line+=lines[i];
          
          m1 = requestScope.matcher(line);
          m2 = scopeMapObject3.matcher(line);
          
          if(m1.find())
              comment += line.trim() + "\n";            
          if(m2.find() && requestScopeObj == null)
              requestScopeObj = m2.group(1);
          
          if(requestScopeObj != null && line.contains(requestScopeObj +".put"))
              comment += line;
      }
      return comment;
  }
  
  private String getSessionScopeLines(String[] lines) {
      String comment = "";
      String sessionScopeObj = null;
      
      for(int i = 0; i < lines.length; i++) {
          
          Matcher m1 = singleLineCommentsRE.matcher(lines[i]);
          Matcher m2 = multiLineCommentsRE.matcher(lines[i]);
          if(m1.find()) continue;
          if(m2.find()) {
              while(!lines[i].contains("*/") && i < lines.length-1) i++;
              continue;
          }
          String line = "";
          while(!lines[i].contains(";") && i < lines.length-1){
              line += lines[i];
              i++;
          }
          line+=lines[i];
          
          m1 = sessionScope.matcher(line);
          m2 = scopeMapObject4.matcher(line);
          
          if(m1.find())
              comment += line.trim() + "\n";            
          if(m2.find() && sessionScopeObj == null)
              sessionScopeObj = m2.group(1);
          
          if(sessionScopeObj != null && line.contains(sessionScopeObj +".put"))
              comment += line;
      }
      return comment;
  }
}
