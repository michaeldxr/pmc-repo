package oracle.apps.sqlanalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.XMLParserHelper;

import org.w3c.dom.Node;

public class SQLParserHelper {
    
  static Pattern selectFromPattern = Pattern.compile("SELECT\\s+(.*?)\\s+FROM\\s+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL); 
  static Pattern fromPattern = Pattern.compile("\\s+FROM\\s+(.*?)(WHERE|UNION|ORDER BY|HAVING|GROUP BY|SELECT|FROM)\\s+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL); 
  static Pattern fromOnlyPattern = Pattern.compile("\\s+FROM\\s+(.*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL); 
  static Pattern fromJoinPattern = Pattern.compile("\\s+FROM\\s+(.*?)\\s+(?:(?:(?:INNER|FULL|OUTER|LEFT|RIGHT)\\s+)*JOIN\\s+(.*?)\\s+ON.*?)+", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
 // static Pattern fromSpacePattern = Pattern.compile("\\s+FROM\\s+",Pattern.CASE_INSENSITIVE);
 // static Pattern nestedSelectPattern = Pattern.compile("\\(\\s*SELECT\\s+",Pattern.CASE_INSENSITIVE);
  static HashMap<String,String> nestedQueries = new HashMap<String,String>();
  static Pattern selectToken1 = Pattern.compile("\\(\\s*\\w+\\.\\w+\\s*\\)");
  static Pattern selectToken2 = Pattern.compile("\\(\\s*\\w+\\s*\\)");
  static Pattern nestedSelectQueryPattern = Pattern.compile("\\(\\s*SELECT\\s+(.*?)FROM\\s+(.*?)WHERE.*?\\)");
  static  int num_nestedQueriesFound = 0;
  
  public static void main(String[] args){
      
//      System.out.println(removeSingleLineSQLComments("abjdjsf -- kjdfkdfskk"));
//      System.out.println(removeSingleLineSQLComments("--kjdfkdfskk43sdhdjsh----"));
//      System.out.println(removeSingleLineSQLComments("abcd '----' dshds "));
//      System.out.println(removeSingleLineSQLComments("abcd '----' dshds \r\n --this should be removed\r\n sdgds '----' this shouldnt be "));
  }
  
  public static void parseFromSql(Node viewObject,  HashMap<String,String> tableAliasNames) {
      
      Node sql = XMLParserHelper.getChildNodeWithName(viewObject, "SQLQuery");
      
      if(sql == null)
          sql = XMLParserHelper.getChildNodeWithName(viewObject, "SUBQUERY");
      String sqlQuery = null;
      nestedQueries = new HashMap<String,String>();
      num_nestedQueriesFound = 0;
      
      if(sql != null)
          sqlQuery = sql.getTextContent();
      
      if(sqlQuery != null) {    
          
          sqlQuery = sqlQuery.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
          sqlQuery = sqlQuery.replaceAll("--.*$",""); //remove single line comments
          
          
          sqlQuery = removeNestedQueries(sqlQuery); 
          parseFromQuery(sqlQuery,tableAliasNames);
          
          for(Iterator<String> it = nestedQueries.values().iterator(); it.hasNext();) {
              String nestedQuery = it.next();
              parseFromQuery(nestedQuery,tableAliasNames);
          }
      }
      
      String fromList = XMLParserHelper.getAttributeValue(viewObject,"FromList");
      if(fromList != null) {
        processFromPart(fromList,tableAliasNames);
      }
  }
  
  /*public static String removeSingleLineSQLComments(String sql) {
      
      int newCommentLiteral,lastCommentLiteral=0;
      
      while ((newCommentLiteral = sql.indexOf("--", lastCommentLiteral)) != -1)
              {
                  System.out.println(sql.substring(lastCommentLiteral, newCommentLiteral - lastCommentLiteral));
                  int countQuotes = sql.substring(lastCommentLiteral, newCommentLiteral - lastCommentLiteral+1).split("'").length -1;
                  if (countQuotes % 2 == 0) //this is a comment, since there's an even number of quotes preceding
                  {
                      int eol = sql.indexOf("\n");
                      if (eol == -1)
                          eol = sql.length()-1; //no more newline, meaning end of the string
                      sql = sql.substring(0,newCommentLiteral) + sql.substring(eol+1);
                      lastCommentLiteral = newCommentLiteral;
                  }
                  else //this is within a string, find string ending and moving to it
                  {
                      int singleQuote = sql.indexOf("'", newCommentLiteral);
                      if (singleQuote == -1)
                          singleQuote = sql.length();
                      int doubleQuote = sql.indexOf('"', newCommentLiteral);
                      if (doubleQuote == -1)
                          doubleQuote = sql.length();

                      lastCommentLiteral = Math.min(singleQuote, doubleQuote) + 1;

                      //instead of finding the end of the string you could simply do += 2 but the program will become slightly slower
                  }
              }
      return sql;
  }*/
  
    public static void parseFromSql(String sqlQuery,  HashMap<String,String> tableAliasNames) {
        
        nestedQueries = new HashMap<String,String>();
        num_nestedQueriesFound = 0;
        
          
        if(sqlQuery != null) {    
            
            sqlQuery = sqlQuery.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
            sqlQuery = sqlQuery.replaceAll("--.*$",""); //remove single line comments
            
            
            sqlQuery = removeNestedQueries(sqlQuery); 
            parseFromQuery(sqlQuery,tableAliasNames);
            
            for(Iterator<String> it = nestedQueries.values().iterator(); it.hasNext();) {
                String nestedQuery = it.next();
                parseFromQuery(nestedQuery,tableAliasNames);
            }
        }
    }
  
  public static void parseSql(Node viewObject, HashMap<String,HashSet> colNameTableNames, HashMap<String,String> colAliasName) {
      
    Node sql = XMLParserHelper.getChildNodeWithName(viewObject, "SQLQuery");
    String sqlQuery = null;
    HashMap<String,String> tableAliasNames = new HashMap<String,String>();
    nestedQueries = new HashMap<String,String>();
    num_nestedQueriesFound = 0;
   
    if(sql != null)
        sqlQuery = sql.getTextContent();
    
    if(sqlQuery != null) {    
        
        sqlQuery = sqlQuery.replaceAll("/\\*.*?\\*/", ""); //remove hints and comments
        sqlQuery = sqlQuery.replaceAll("--.*$",""); //remove single line comments
        
        
        sqlQuery = removeNestedQueries(sqlQuery); 
        parseQuery(sqlQuery,tableAliasNames,colAliasName,colNameTableNames);
        
        for(Iterator<String> it = nestedQueries.values().iterator(); it.hasNext();) {
            String nestedQuery = it.next();
            parseQuery(nestedQuery,tableAliasNames,colAliasName,colNameTableNames);
        }
    }
    
    String fromList = XMLParserHelper.getAttributeValue(viewObject,"FromList");
    if(fromList != null) {
      processFromPart(fromList,tableAliasNames);
    }
    
    String selectList = XMLParserHelper.getAttributeValue(viewObject,"SelectList");
    if(selectList != null) {
      selectList = removeNestedQueries(selectList);
      processSelectPart(selectList,tableAliasNames,colNameTableNames,colAliasName);
    }
    
  }
  
  public static void parseFromQuery(String sqlQuery, HashMap<String,String> tableAliasNames) {
      
      Matcher m = fromJoinPattern.matcher(sqlQuery);
      if(m.find())
          processFromJoinPart(m,tableAliasNames);
      else{        
        m = fromPattern.matcher(sqlQuery);
        if(!m.find())
            m = fromOnlyPattern.matcher(sqlQuery);
        else
            m.reset();       
        
        while(m.find()){
          processFromPart(m.group(1),tableAliasNames);
        }   
      }            
  }
  
    public static void parseQuery(String sqlQuery, HashMap<String,String> tableAliasNames,
                                  HashMap<String,String> colAliasName,HashMap<String,HashSet> colNameTableNames){
        
        Matcher m = fromJoinPattern.matcher(sqlQuery);
        if(m.find())
            processFromJoinPart(m,tableAliasNames);
        else{        
          m = fromPattern.matcher(sqlQuery);
          if(!m.find())
              m = fromOnlyPattern.matcher(sqlQuery);
          else
              m.reset();       
          
          while(m.find()){
            processFromPart(m.group(1),tableAliasNames);
          }   
        }            
        
        m = selectFromPattern.matcher(sqlQuery);
        
        while(m.find())
            processSelectPart(m.group(1),tableAliasNames,colNameTableNames,colAliasName);          
    }
      
  
  //recursively remove the nested queries 
  public static String removeNestedQueries(String sqlQuery){
    
   // System.out.println(sqlQuery);
   
      
    sqlQuery = sqlQuery.toUpperCase().replaceAll("\\(\\s*SELECT","(SELECT");
    int index = sqlQuery.indexOf("(SELECT");
    if(index == -1)
      return sqlQuery;
    int braces = 1;
    int startIndex = index;
    int endIndex = -1;
    index++;
    while(index < sqlQuery.length()) {
        
      char c = sqlQuery.charAt(index);
      if(c == '(') braces++;
      if(c == ')') braces --;
      if(braces == 0) {
          endIndex = index;
          String nestedQuery = sqlQuery.substring(startIndex, endIndex+1);
          String nestedQueryKey = "NESTED_QUERY" + (++num_nestedQueriesFound);
          //System.out.println(nestedQuery);
          nestedQueries.put(nestedQueryKey, nestedQuery);
          sqlQuery = sqlQuery.replace(nestedQuery, nestedQueryKey);
          //System.out.println(sqlQuery);
          return removeNestedQueries(sqlQuery);
      }
      index++;
    }
    
    return sqlQuery;
  }
  
  
  public static  void processFromPart(String fromPart, HashMap<String,String> tableAliasNames) {
  
   // In case of nested queries extract the innermost from....
//      Matcher m = fromSpacePattern.matcher(fromPart);
//      int index = 0;
//      while(m.find())
//          index = m.start() + m.group().length();
//      fromPart = fromPart.substring(index); 
      
      StringTokenizer tokenizer = new StringTokenizer(fromPart,","); //SELECT ... FROM table t1, table2 .....WHERE ...
        
      while(tokenizer.hasMoreElements()){
        
        String token = tokenizer.nextToken().trim().toUpperCase();
        token = token.replaceAll("\\s+", " ");
        String[] parts = token.split("\\s"); //split by whitespace
        String tableAlias = "";
        String tableName = "";
          
        if(parts.length == 2) {
          tableAlias = parts[1];
          tableName = parts[0];
        } else {
          if(parts.length != 1){
           // System.out.println("Weird case(FROM): " + token + " in File: " + fileName);
            continue;
          }
          tableAlias = parts[0];
          tableName = parts[0];
        }
          if(!tableAliasNames.containsKey(tableAlias))
            tableAliasNames.put(tableAlias,tableName);
      }
  }
  
  public static  void processFromJoinPart(Matcher m, HashMap<String,String> tableAliasNames) {
      
      m.reset();
      String fromPart = "";
      while(m.find()) {
        
        String fromPart1 = m.group(1);
        String fromPart2 = m.group(2);
        fromPart+= fromPart1 + "," + fromPart2 + ",";
      }
      
        StringTokenizer tokenizer = new StringTokenizer(fromPart,","); 
          
        while(tokenizer.hasMoreElements()){
          
          String token = tokenizer.nextToken().trim().toUpperCase();
          token = token.replaceAll("\\s+", " ");
          String[] parts = token.split("\\s"); //split by whitespace
          String tableAlias = "";
          String tableName = "";
            
          if(parts.length == 2) {
            tableAlias = parts[1];
            tableName = parts[0];
          } else {
            if(parts.length != 1){
             // System.out.println("Weird case(FROM): " + token + " in File: " + fileName);
              continue;
            }
            tableAlias = parts[0];
            tableName = parts[0];
          }
            tableAliasNames.put(tableAlias,tableName);
        }      
  }
  
  public static void processSelectPart(String selectPart, HashMap<String,String> tableAliasNames, HashMap<String,HashSet> colNameTableNames, HashMap<String,String> colAliasName){
    // System.out.println(selectPart);
     
     StringTokenizer tokenizer = new StringTokenizer(selectPart,","); //SELECT t1.abc abc, t1.xyz xyz, jkl... FROM
       
     while(tokenizer.hasMoreElements()){
       
       String token = tokenizer.nextToken().trim().toUpperCase();
         
       token = token.replaceAll("DISTINCT\\s+", "");
         
        if(token.contains("(") || token.contains(")")) {
            Matcher m1 = selectToken1.matcher(token);
            Matcher m2 = selectToken2.matcher(token);
            if(m1.find() || m2.find()) {
                token = token.replace("(","");
                token = token.replace(")"," ");
            }
            else
               continue;
        }
         
       token = token.replaceAll("\\s+", " ");
       String[] parts = token.split("\\s"); //split by whitespace
         
       String colName = "";
       String aliasName = "";
       String tableAlias = "";
       String tableName = "";
         
       if(parts.length == 2) {
         aliasName = parts[1]; //alias name for the view attribute
         String[] colParts = parts[0].split("\\.");
         if(colParts.length == 2){
           tableAlias = colParts[0];
           colName = colParts[1];
         } else
             colName = colParts[0];
       } 
       else if(parts.length == 3){
           if(!parts[1].equalsIgnoreCase("AS")){
             //System.out.println("Weird case(SELECT): " + token + " in File: " + fileName);
             continue;
           }
           aliasName = parts[2];
          String[] colParts = parts[0].split("\\.");
          if(colParts.length == 2){
            tableAlias = colParts[0];
            colName = colParts[1];
          } else
             colName = colParts[0];
       }
       else {
         if(parts.length != 1){
          // System.out.println("Weird case(SELECT): " + token + " in File: " + fileName);
           continue;
         }
         String[] colParts = parts[0].split("\\.");
         if(colParts.length == 2){
           tableAlias = colParts[0];
           colName = colParts[1];            
         } else
             colName = colParts[0];
           
         aliasName = colName;
       }
         
         if(colName.contains("NESTED_QUERY")) {
             String nestedQuery = nestedQueries.get(colName.trim());
             nestedQueries.remove(colName.trim());
             if(nestedQuery != null){
                 Matcher m = nestedSelectQueryPattern.matcher(nestedQuery);
                 if(m.find()) {
                     colName = m.group(1);
                     tableName = m.group(2);
                     tableAlias = "";
                     HashSet tables = new HashSet();
                     if(!tableName.contains(",")){
                         
                        String[] parts1 = tableName.trim().split("\\s+");
                        tableName = parts1[0];
                        
                        parts1 = colName.trim().split("\\s+");
                        parts1 = parts1[0].split("\\.");
                        if(parts1.length == 2)
                            colName = parts1[1];
                        else
                            colName = parts1[0];
                        
                        tables.add(tableName);
                        colNameTableNames.put(aliasName,tables);
                     }
                 }
             }
         }
       if(!colAliasName.containsKey(aliasName))
          colAliasName.put(aliasName,colName);
     
         
       HashSet tables = new HashSet();
         
       if(!tableAlias.equals("")){
         tableName = tableAliasNames.get(tableAlias);
         if(tableName == null) {
            tableName = tableAlias;
         }          
         tables.add(tableName);          
       } else {
          tables = new HashSet(tableAliasNames.values());
       }
      if(!colNameTableNames.containsKey(aliasName))
          colNameTableNames.put(aliasName,tables);
    }//while tokenizer
  }
  
}
