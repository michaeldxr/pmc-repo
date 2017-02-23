package oracle.apps.helpers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Node;

public class XmlFixer {
    private String[] fileContents = null;
    private File m_file = null;
    // if you turng this out will print sop statements to console.. useful for debugging
    private boolean bDebug = false;
    private String FILE_NEW_LINE = "";
   

    public String getFilePath() {
        if (m_file == null) {
            return "";
        }
        return m_file.getAbsolutePath();
    }

    public XmlFixer(String absFilePath) throws Exception {
        super();
        File f = new File(absFilePath);
        if (!f.exists()) {
            throw new Exception("File does not exist:" + absFilePath);
        }
        m_file = f;
    }

    public void removeAttribute(Node node, String attributeName,
                                String attrOldValue) throws Exception {
        if (fileContents == null) {
            fileContents = convertFileToStringArray(m_file);
        }
        if (node == null) {
            return;
        }
        String newLine = "";
        String entireAttributeString =
            attributeName + "=\"" + attrOldValue + "\"";
        Integer nodeEndOfStartTagLineNumber = getLineNumberInSourceFile(node);
        // note: we start i from startTagLineNumber -1 because filecontents is an array and its index will be one less than actual line number
        // e.g. if line number = 1, filecontents[0] needs to be checked
        for (int i = nodeEndOfStartTagLineNumber - 1; i > 0; i--) {
            String line = fileContents[i];
            if (line.contains(entireAttributeString)) {
                int startIndex = line.indexOf(entireAttributeString);
                newLine = line.substring(0, startIndex);
                newLine =
                        newLine + " " + line.substring(startIndex + entireAttributeString.length());
                if (bDebug) {
                    System.out.println("Old line: " + line);
                    System.out.println("new line: " + newLine);
                }
                fileContents[i] = newLine;
                break;
            }
        }
     }
        
      public void removeAttributeWithSpaces(Node node, String attributeName,
                                  String attrOldValue) throws Exception {
          if (fileContents == null) {
              fileContents = convertFileToStringArray(m_file);
          }
          if (node == null) {
              return;
          }
          String newLine = "";
          String entireAttributeString = attributeName + "=\"" + attrOldValue + "\"";
          Pattern p = Pattern.compile("\\s*" + entireAttributeString + "\\s*");
          
          
          Integer nodeEndOfStartTagLineNumber = getLineNumberInSourceFile(node);
          // note: we start i from startTagLineNumber -1 because filecontents is an array and its index will be one less than actual line number
          // e.g. if line number = 1, filecontents[0] needs to be checked
          for (int i = nodeEndOfStartTagLineNumber - 1; i > 0; i--) {
              String line = fileContents[i];
              Matcher m = p.matcher(line);
              String suffix = "";
              if (m.find()) {
                  newLine = line.replaceAll("\\s*" + entireAttributeString + "\\s*"," ");
                  if(newLine.matches("\\s*/>\\s*")){
                     newLine = "";
                     suffix= "/>";
                  } else if(newLine.matches("\\s*>\\s*")){
                     newLine = "";
                     suffix = ">";
                  } else if (newLine.matches("\\s*"))
                      newLine = "";
                      
                  if (bDebug) {
                      System.out.println("Old line: " + line);
                      System.out.println("new line: " + newLine);
                  }
                  fileContents[i] = newLine;
                  fileContents[i-1] = fileContents[i-1].replaceAll(FILE_NEW_LINE, "") + suffix + FILE_NEW_LINE;
                  break;
              }
          }
    }


    public boolean lineContainsAttributeName(String line,
                                             String attributeName) {
        // Modify this method so that it looks for the firs occurrence of the valid attribute in the line.
        // e.g. MaxFetchSize=0 FetchSize=16 should return true

        int startSearchIndex = 0;
        boolean validStart = false;
        boolean validEnd = false;
        if (isEmpty(line) || isEmpty(attributeName)) {
            return false;
        }

        if (!line.contains(attributeName)) {
            return false;
        }

        while (true) {
            int startIndexOfAttr =
                line.indexOf(attributeName, startSearchIndex);

            if (startIndexOfAttr < 0) {
                return false;
            }

            if (startIndexOfAttr == 0) {
                validStart = true;
            } else {
                Character charbefore = line.charAt(startIndexOfAttr - 1);
                if (Character.isWhitespace(charbefore)) {
                    validStart = true;
                } else {
                    validStart = false;
                }
            }

            if (validStart == false) {
                startSearchIndex = startSearchIndex + attributeName.length();
                continue;
            }

            int endIndexOfAttr = startIndexOfAttr + attributeName.length();
            int strLength = line.length();
            // this is quite silly - because it should not end with the attributeName, but its still valid
            if (endIndexOfAttr == strLength - 1) {
                validEnd = true;
            } else {
                Character charAfter = line.charAt(endIndexOfAttr);
                if (Character.isWhitespace(charAfter)) {
                    validEnd = true;
                }
                if (charAfter == '=') {
                    validEnd = true;
                }
            }
            if (validStart && validEnd) {
                return true;
            }

            startSearchIndex = startSearchIndex + attributeName.length();

        }


    }

    public int getStartIndexOfAttributeNameInStr(String line,
                                                 String attributeName) {
        // Modify this method so that it looks for the firs occurrence of the valid attribute in the line.
        // e.g. MaxFetchSize=0 FetchSize=16 should return true

        int startSearchIndex = 0;
        boolean validStart = false;
        boolean validEnd = false;
        if (isEmpty(line) || isEmpty(attributeName)) {
            return -1;
        }

        if (!line.contains(attributeName)) {
            return -1;
        }

        while (true) {
            int startIndexOfAttr =
                line.indexOf(attributeName, startSearchIndex);

            if (startIndexOfAttr < 0) {
                return -1;
            }

            if (startIndexOfAttr == 0) {
                validStart = true;
            } else {
                Character charbefore = line.charAt(startIndexOfAttr - 1);
                if (Character.isWhitespace(charbefore)) {
                    validStart = true;
                } else {
                    validStart = false;
                }
            }

            if (validStart == false) {
                startSearchIndex = startSearchIndex + attributeName.length();
                continue;
            }

            int endIndexOfAttr = startIndexOfAttr + attributeName.length();
            int strLength = line.length();
            // this is quite silly - because it should not end with the attributeName, but its still valid
            if (endIndexOfAttr == strLength - 1) {
                validEnd = true;
            } else {
                Character charAfter = line.charAt(endIndexOfAttr);
                if (Character.isWhitespace(charAfter)) {
                    validEnd = true;
                }
                if (charAfter == '=') {
                    validEnd = true;
                }
            }
            if (validStart && validEnd) {
                return startIndexOfAttr;
            }

            startSearchIndex = startSearchIndex + attributeName.length();

        }

    }

public boolean modifyTextContent(Node node, String attributeStringToMatchOn, String oldTextContent,String newTextContent) throws Exception
{
        boolean fixMade = false;
        if (fileContents == null) {
            fileContents = convertFileToStringArray(m_file);
        }
        if (node == null) {
            return fixMade;
        }
        
        String nodeName = node.getNodeName();
        String nodeStartTag1 = "<" + nodeName;
        String nodeEndTag1 = "</" + nodeName;
        Integer nodeEndOfStartTagLineNumber = getLineNumberInSourceFile(node);
        Integer colNumberOfEndOfStartTag = getColumnNumberInSourceFile(node);
        
        int lineNumber = nodeEndOfStartTagLineNumber-1;
        String line = fileContents[lineNumber];
        int indexEndTag = line.indexOf(nodeEndTag1, colNumberOfEndOfStartTag);
        if(indexEndTag==-1)
            return false;
        
        String obtOldVal = line.substring(colNumberOfEndOfStartTag-1, indexEndTag);
        String newLine="";
        if(obtOldVal!=null && obtOldVal.trim().equals(oldTextContent.trim()))
        {
         newLine = line.substring(0,colNumberOfEndOfStartTag-1);
         newLine = newLine+newTextContent;
         newLine = newLine+line.substring(indexEndTag);
        }
        if(!isEmpty(newLine))
        {
            if(bDebug)
            {
                System.out.println("Old line is: \n"+line);
                System.out.println("New Line is: \n"+newLine);
                }
                fileContents[lineNumber]=newLine;
                return true;
            }
        
        return false;
        
        
    }

    public void modifyAttributeImage(Node node, String attributeName, 
                                        String oldImage, String newImage) throws Exception
    {
       
        if (fileContents == null) {
            fileContents = convertFileToStringArray(m_file);
        }
        
        Integer nodeEndOfStartTagLineNumber = getLineNumberInSourceFile(node);
        String nodeName = node.getNodeName();
        
        for (int i = nodeEndOfStartTagLineNumber - 1; i > 0; i--) {
           
              String line = fileContents[i];        
              fileContents[i]=line.replace(oldImage,newImage);;
                            
               if (lineContainsAttributeName(line, attributeName) || line.contains("<"+nodeName)) 
                break;           
        }
    }
    
    public boolean modifyAttributeIrrespectiveOldValue(Node node,
                                                    String attributeName,
                                                    String attrNewValue
                                                    ) throws Exception {
        boolean fixMade = false;
        if (fileContents == null) {
            fileContents = convertFileToStringArray(m_file);
        }
        if (node == null) {
            return fixMade;
        }
        boolean foundAttribute = false;

        String newLine = "";

        String newAttributeString =
            attributeName + "=\"" + attrNewValue + "\"";
        Integer nodeEndOfStartTagLineNumber = getLineNumberInSourceFile(node);
        String nodeName = node.getNodeName();
        String nodeStartTag1 = "<" + nodeName;

        // note: we start i from startTagLineNumber -1 because filecontents is an array and its index will be one less than actual line number
        // e.g. if line number = 1, filecontents[0] needs to be checked
        for (int i = nodeEndOfStartTagLineNumber - 1; i > 0; i--) {
            String line = fileContents[i];

            if (lineContainsAttributeName(line, attributeName)) {
                boolean bFound = false;
                boolean bErrorInReplacing = false;
                int attrStartIndex =
                    getStartIndexOfAttributeNameInStr(line, attributeName);
                if (attrStartIndex == -1) {
                    continue;
                }
                int endIndexOfAttributeName =
                    attrStartIndex + attributeName.length();
                int indexEquals = line.indexOf("=", endIndexOfAttributeName);
                if (indexEquals == -1) {
                    throw new Exception("modifyAttributeIrrespectiveOldValue: could not find index of equals");
                }

                int indexOfStartQuote = line.indexOf("\"", indexEquals);
                int indexOfEndQuote = line.indexOf("\"", indexOfStartQuote+1);
                if (indexOfStartQuote == -1 || indexOfEndQuote == -1) {
                    throw new Exception("modifyAttributeIrrespectiveOldValue: could not find index of quotes");
                }

                String afterQuotes = line.substring(indexOfEndQuote + 1);
                newLine = line.substring(0, indexOfStartQuote);
                newLine = newLine + "\"" + attrNewValue + "\"";
                newLine = newLine + " " + afterQuotes;
                if(bDebug)
                {
                System.out.println("[modifyAttributeIrrespectiveOldValue] Old line: " + line);
                System.out.println("[modifyAttributeIrrespectiveOldValue] new line: " + newLine);
                }
                fileContents[i]=newLine;
                return true;

            }

        }
        return fixMade;
    }
   

    public void modifyAttribute(Node node, String attributeName,
                                String attrOldValue, String attrNewValue,
                                boolean addAttributeIfNotFound) throws Exception {
        if (fileContents == null) {
            fileContents = convertFileToStringArray(m_file);
        }
        if (node == null) {
            return;
        }
        boolean foundAttribute = false;
        if (attrOldValue == null) {
            attrOldValue = "";
        }
        
        if(attrOldValue.equals(attrNewValue))
          return;
        String newLine = "";
        String entireAttributeString =
            attributeName + "=\"" + attrOldValue + "\"";
        String newAttributeString =
            attributeName + "=\"" + attrNewValue + "\"";
        Integer nodeEndOfStartTagLineNumber = getLineNumberInSourceFile(node);
        String nodeName = node.getNodeName();
        String nodeStartTag1 = "<" + nodeName;

        // note: we start i from startTagLineNumber -1 because filecontents is an array and its index will be one less than actual line number
        // e.g. if line number = 1, filecontents[0] needs to be checked
        for (int i = nodeEndOfStartTagLineNumber - 1; i > 0; i--) {
            String line = fileContents[i];
            if (lineContainsAttributeName(line, attributeName)) {

                if (line.contains(entireAttributeString)) {
                    int startIndex = line.indexOf(entireAttributeString);
                    newLine = line.substring(0, startIndex);
                    newLine = newLine + newAttributeString;
                    newLine =
                            newLine + line.substring(startIndex + entireAttributeString.length());
                    foundAttribute = true;
                    if (bDebug) {
                        System.out.println("Old line: " + line);
                        System.out.println("new line: " + newLine);
                    }
                    fileContents[i] = newLine;
                    break;
                } else if (lineContainsAttributeName(line, attributeName)) {
                    boolean bFound = false;
                    String modifiedAttrString = attributeName;
                    int attrStartIndex =
                        getStartIndexOfAttributeNameInStr(line, attributeName);
                    if (attrStartIndex == -1) {
                        continue;
                    }
                    //line.indexOf(attributeName);
                    int startIndex = attrStartIndex + attributeName.length();
                    int indexEquals = line.indexOf("=", startIndex);
                    int endAttrIndex = -1;
                    if (indexEquals == -1) {
                        bFound = false;
                    }
                    if (indexEquals != -1) {

                        String beforeEquals =
                            line.substring(startIndex, indexEquals);
                        if (beforeEquals.trim().equals("")) {
                            modifiedAttrString =
                                    modifiedAttrString + beforeEquals + "=";
                            String attrStrToFind = "\"" + attrOldValue + "\"";
                            int indexQuoteAttr =
                                line.indexOf(attrStrToFind, indexEquals);
                            if (indexQuoteAttr != -1) {
                                String beforeQuote =
                                    line.substring(indexEquals + 1,
                                                   indexQuoteAttr);
                                if (beforeQuote.trim().equals("")) {
                                    bFound = true;
                                    modifiedAttrString =
                                            modifiedAttrString + beforeQuote +
                                            "\"" + attrNewValue + "\"";
                                    endAttrIndex =
                                            indexQuoteAttr + attrStrToFind.length();
                                }
                            }
                        } else {
                            bFound = false;
                        }
                    }
                    if (bFound) {
                        newLine = line.substring(0, attrStartIndex);
                        newLine = newLine + modifiedAttrString;
                        newLine = newLine + line.substring(endAttrIndex);
                        foundAttribute = true;
                        if (bDebug) {
                            System.out.println("Old line: " + line);
                            System.out.println("new line: " + newLine);
                        }
                        fileContents[i] = newLine;
                        break;
                    } else if (line.contains(nodeStartTag1)) {
                        break;
                    }
                } else if (line.contains(nodeStartTag1)) {
                    // attribute not found, and it reached the start tag of the node..
                    break;
                }
            }
        }
        if (!foundAttribute) {

            if (addAttributeIfNotFound) {
                String endLineOfNodeTag =
                    fileContents[nodeEndOfStartTagLineNumber - 1];
                Integer columnNumOfEndTag = getColumnNumberInSourceFile(node);
                int indexEndTagOnLine = endLineOfNodeTag.lastIndexOf(">");
                int endIndexForSub = indexEndTagOnLine;
                if (indexEndTagOnLine != -1) {
                    if (indexEndTagOnLine >= 1) {
                        if (endLineOfNodeTag.charAt(indexEndTagOnLine - 1) ==
                            '/') {
                            endIndexForSub = indexEndTagOnLine - 1;
                        }
                    }
                } else {
                    endIndexForSub = columnNumOfEndTag - 2;
                }
                String lineToAdd = " " + newAttributeString + " ";
                if (endLineOfNodeTag.length() >= columnNumOfEndTag) {
                    String beforeEndTag =
                        endLineOfNodeTag.substring(0, endIndexForSub);
                    String after = endLineOfNodeTag.substring(endIndexForSub);
                    newLine = beforeEndTag + lineToAdd + after;
                    fileContents[nodeEndOfStartTagLineNumber - 1] = newLine;
                    if (bDebug) {
                        System.out.println("Old line: " + endLineOfNodeTag);
                        System.out.println("new line: " + newLine);
                    }
                } else {
                    throw new Exception("Could not fix node");
                }
            } else {
                System.out.println("Could not find String: " +
                                   entireAttributeString);
                throw new Exception("could not fix node");
            }
        }
    }    
    
    public void addAttribute(Node node, String attributeName,
                                String attrValue) throws Exception{
      addAttribute(node, attributeName, attrValue, "\"");
    }

    public void addAttribute(Node node, String attributeName,
                                String attrValue, String quoting) throws Exception{
      if (fileContents == null) {
          fileContents = convertFileToStringArray(m_file);
      }
      if (node == null) {
          return;
      }
      if (attrValue == null) {
          attrValue = "";
      }
     
      String newLine = "";
      String newAttributeString =
          attributeName + "=" + quoting + attrValue + quoting;
      Integer nodeEndOfStartTagLineNumber = getLineNumberInSourceFile(node);
     
      String endLineOfNodeTag =
          fileContents[nodeEndOfStartTagLineNumber - 1];
      Integer columnNumOfEndTag = getColumnNumberInSourceFile(node)-2;
      
      
      //int indexEndTagOnLine = endLineOfNodeTag.lastIndexOf(">"); 
      int endIndexForSub = columnNumOfEndTag;
      if (columnNumOfEndTag >= 0) {          
          if (columnNumOfEndTag >=1 && endLineOfNodeTag.charAt(columnNumOfEndTag - 1) == '/') 
              endIndexForSub = columnNumOfEndTag - 1;                  
      } else {
          throw new Exception("Could not fix node"); 
      }
      String lineToAdd = " " + newAttributeString;
      if (endLineOfNodeTag.length() >= columnNumOfEndTag) {
          String beforeEndTag =
              endLineOfNodeTag.substring(0, endIndexForSub);
          String after = endLineOfNodeTag.substring(endIndexForSub);
          newLine = beforeEndTag + lineToAdd + after;
          fileContents[nodeEndOfStartTagLineNumber - 1] = newLine;
          if (bDebug) {
              System.out.println("Old line: " + endLineOfNodeTag);
              System.out.println("new line: " + newLine);
          }
      } else {
          throw new Exception("Could not fix node");
      }

    }

    public boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public String getIndentation(Node node) {
        
      if (fileContents == null) {
          try{
          fileContents = convertFileToStringArray(m_file);
          }catch(Exception ex)
          {
              ex.printStackTrace();
              return "";
              }
      }
        String indentation = "";
        if (node == null)
            return indentation;
        String nodeName = node.getNodeName();
        String startTagNodeName = "<" + nodeName;
        int endOfstartTag = getLineNumberInSourceFile(node);
        if (endOfstartTag <= 0)
            return indentation;
        int indexOfStartTag = -1;
        String startTagLine = "";
        for (int i = endOfstartTag; i >= 0; i--) {
            String line = fileContents[i - 1];
            if (line.contains(startTagNodeName)) {
                indexOfStartTag = line.indexOf(startTagNodeName);
                startTagLine = line;
                break;
            }
        }
        if (indexOfStartTag == -1) {
            return "";
        }

        for (int indexBeforeIndentation = indexOfStartTag - 1;
             indexBeforeIndentation >= 0; indexBeforeIndentation--) {
            char charAt = startTagLine.charAt(indexBeforeIndentation);
            if (charAt == ' ') {
                indentation = indentation + " ";
            } else {
                break;
            }
        }
        return indentation;
    }

    public String removeTrailingSpaces(String str) {
        if (str == null)
            return null;

        String strRemovedTrails = "";
        int len = str.length();
        int indexEndSub = -1;
        for (int i = len - 1; i >= 0; i--) {
            if (str.charAt(i) == ' ') {
                indexEndSub = i;
            } else {
                break;
            }
        }
        if (indexEndSub == 0) {
            return "";
        }
        if (indexEndSub > 0) {

            return str.substring(0, indexEndSub);
        }
        return str;
    }
    public void addAsLastChildToNode(Node parentNode, String childToAdd,
                                     String indentFromParent) throws Exception {
         if (parentNode == null || childToAdd == null)
             return;
         if (fileContents == null) {
             fileContents = convertFileToStringArray(m_file);
         }

         boolean bError = false;
         Integer endTagLineNumber = getEndTagLineNumber(parentNode);
         String parentNodeName = parentNode.getNodeName();
         String parentNodeIndentation = getIndentation(parentNode);
         String childNodeIndentation = parentNodeIndentation + indentFromParent;

         String endTagStr1 = "</" + parentNodeName + ">";
         String endTagStr2 = "/>";
         

         // System.out.println("child node string: " + childNodeStr);
         String endLine = fileContents[endTagLineNumber - 1];
         String newLine = "";
         // If endline has eg. </list>
         if (endLine.contains(endTagStr1)) {
             int endIndex = endLine.indexOf(endTagStr1);
             // If it is only a space or indentation before the end tag -  preserve the indentation
             newLine = endLine.substring(0, endIndex);
             if (newLine.trim().equals("")) {
                 newLine = "";
             } else {
                 newLine = removeTrailingSpaces(newLine);
                 newLine = newLine+FILE_NEW_LINE;
             }
             newLine =
                     newLine + childNodeIndentation + childToAdd + FILE_NEW_LINE;
             newLine =
                     newLine + parentNodeIndentation + endLine.substring(endIndex);
         } else if (endLine.contains(endTagStr2)) {
             // if the end tag is something like <list n="classpath" />, then we need to add the entry like
             // <list n="classpath"> <url path="abc.jar" /> </list>

             // get indentation of start tag
             //TODO: get indenation of parent node

             int endIndex = endLine.indexOf(endTagStr2);
             // If it is only a space or indentation before the end tag -  preserve the indentation

             newLine = endLine.substring(0, endIndex);
             newLine = newLine + ">";
             newLine =
                     newLine + FILE_NEW_LINE + childNodeIndentation + childToAdd +
                     FILE_NEW_LINE;
             newLine =
                     newLine + parentNodeIndentation + endTagStr1 + FILE_NEW_LINE;
         } else {
             bError = true;
         }
         if (bError) {
             throw new Exception("Did not add last child to node");
         }
         if (bDebug) {
             System.out.println("Old line: " + endLine);
             System.out.println("New line: " + newLine);
         }
         fileContents[endTagLineNumber - 1] = newLine;

     }
    
    /**
     * Will add nodeToadd after siblingNode
     */
  public void addSiblingAfterNode(Node siblingNode, String nodeToAdd) throws Exception {
       if (siblingNode == null || nodeToAdd == null)
           return;
       if (fileContents == null) {
           fileContents = convertFileToStringArray(m_file);
       }

       Integer endTagLineNumber = getEndTagLineNumber(siblingNode);

       String endLine = fileContents[endTagLineNumber - 1];
       String newLine = endLine + getIndentation(siblingNode) + nodeToAdd + FILE_NEW_LINE;
       if (bDebug) {
           System.out.println("Old line: " + endLine);
           System.out.println("New line: " + newLine);
       }
       fileContents[endTagLineNumber - 1] = newLine;

   }
    public void addAsLastChildToNode(Node parentNode, Node childToAdd,
                                     String indentFromParent) throws Exception {
        
        String childNodeStr =
            XMLParserHelper.getNodeAsStringWithCloseTag(childToAdd, "");
        addAsLastChildToNode(parentNode, childNodeStr, indentFromParent);
        
    }
public void replaceNode(Node nodeToReplace, String newNodeString) throws Exception
{
        if (fileContents == null) {
            fileContents = convertFileToStringArray(m_file);
        }
        if (nodeToReplace == null) {
            return;
        }
        String nodeName = nodeToReplace.getNodeName();
        if (isEmpty(nodeName)) {
            return;
        }
        String startNodeTag = "<" + nodeName + " ";
        //handle cases when <nodeName is the only thing in nodeName line, and no " " at the end
        String startNodeTagSpecial = "<" + nodeName;
        String startNodeTag2 = "<" + nodeName + ">";
        Integer nodeEndOfStartTagLineNumber = getLineNumberInSourceFile(nodeToReplace);
        Integer nodeEndOfEndTagLineNumber = getEndTagLineNumber(nodeToReplace);
        Integer nodeStartTagLineNumber = -1;
        // note: we start i from startTagLineNumber -1 because filecontents is an array and its index will be one less than actual line number
        // e.g. if line number = 1, filecontents[0] needs to be checked
        for (int i = nodeEndOfStartTagLineNumber - 1; i > 0; i--) {
            String line = fileContents[i];
            if (line.contains(startNodeTag) || line.contains(startNodeTag2) || line.trim().equals(startNodeTagSpecial)) {
                nodeStartTagLineNumber = i + 1;
                break;
            }
        }
        if (nodeStartTagLineNumber == -1) {
            throw new Exception("Could not find start tag line number");
        }
        String endTagStr1 = "</" + nodeName + ">";
        String endTagStr2 = "/>";
        if (nodeStartTagLineNumber.compareTo(nodeEndOfEndTagLineNumber) != 0) {
            String startLine = fileContents[nodeStartTagLineNumber - 1];
            String endLine = fileContents[nodeEndOfEndTagLineNumber - 1];
            String newStartLine = "";
            String newEndLine = "";

            boolean bError = false;
            if (startLine.contains(startNodeTag)) {
                int indexStart = startLine.indexOf(startNodeTag);
                newStartLine = startLine.substring(0, indexStart);
            } else if (startLine.contains(startNodeTag2)) {
                int indexStart = startLine.indexOf(startNodeTag2);
                newStartLine = startLine.substring(0, indexStart);
            } else if (startLine.trim().equals(startNodeTagSpecial)) {
                int indexStart = startLine.indexOf(startNodeTagSpecial);
                newStartLine = startLine.substring(0, indexStart);
            } else {
                bError = true;
            }
            if (endLine.contains(endTagStr1)) {
                int endIndex = endLine.indexOf(endTagStr1);
                newEndLine = endLine.substring(endIndex + endTagStr1.length());
            } else if (endLine.contains(endTagStr2)) {                
                int endIndex = endLine.indexOf(endTagStr2);
                newEndLine = endLine.substring(endIndex + endTagStr2.length());
            } else {
                bError = true;
            }
            if (bError) {
                throw new Exception("Could not remove node");
            }
            String oldcontents = "";
            String newContents = "";
            
            fileContents[nodeStartTagLineNumber - 1] = newStartLine;
            fileContents[nodeEndOfEndTagLineNumber - 1] = newEndLine;
            
            // added for replace functionality
            newStartLine=newStartLine + newNodeString;
            fileContents[nodeStartTagLineNumber - 1] = newStartLine;
            
            oldcontents = startLine;
            newContents = newStartLine;



            for (int j = nodeStartTagLineNumber;
                 j < nodeEndOfEndTagLineNumber - 1; j++) {
                oldcontents = oldcontents + fileContents[j];
                newContents = newContents + "";
                fileContents[j] = "";
            }
            oldcontents = oldcontents + endLine;
            newContents = newContents + newEndLine;
            if (bDebug) {
                System.out.println("oldContents" + oldcontents);
                System.out.println("newContents: " + newContents);
            }

        } else {
            String line = fileContents[nodeStartTagLineNumber - 1];
            String newLine = "";
            // start and end node on the same line
            if (line.contains(startNodeTag)) {
                int indexStart = line.indexOf(startNodeTag);
                newLine = line.substring(0, indexStart);
                // added for replace functionality
                newLine=newLine + newNodeString;
            }else {
                throw new Exception("Could not remove node because could not find start tag");
            }
            if (line.contains(endTagStr1)) {
                int endIndex = line.indexOf(endTagStr1);
                newLine =
                        newLine + line.substring(endIndex + endTagStr1.length());
            } else if (line.contains(endTagStr2)) {
                int endIndex = line.indexOf(endTagStr2);
                newLine =
                        newLine + line.substring(endIndex + endTagStr2.length());
            } else {
                throw new Exception("could not remove node");
            }
           
            if (bDebug) {
                System.out.println("start and end tag on same line: ");
                System.out.println("Old line: " + line);
                System.out.println("New line: " + newLine);
            }
          
            fileContents[nodeStartTagLineNumber - 1] = newLine;
            
            
        }
    }

    public void removeNode(Node node) throws Exception {
        if (fileContents == null) {
            fileContents = convertFileToStringArray(m_file);
        }
        if (node == null) {
            return;
        }
        String nodeName = node.getNodeName();
        if (isEmpty(nodeName)) {
            return;
        }
        String startNodeTag = "<" + nodeName + " ";
        String startNodeTag2 = "<" + nodeName + ">";
        Integer nodeEndOfStartTagLineNumber = getLineNumberInSourceFile(node);
        Integer nodeEndOfEndTagLineNumber = getEndTagLineNumber(node);
        Integer nodeStartTagLineNumber = -1;
        // note: we start i from startTagLineNumber -1 because filecontents is an array and its index will be one less than actual line number
        // e.g. if line number = 1, filecontents[0] needs to be checked
        for (int i = nodeEndOfStartTagLineNumber - 1; i > 0; i--) {
            String line = fileContents[i];
            if (line.contains(startNodeTag) || line.contains(startNodeTag2)) {
                nodeStartTagLineNumber = i + 1;
                break;
            }
        }
        if (nodeStartTagLineNumber == -1) {
            throw new Exception("Could not find start tag line number");
        }
        String endTagStr1 = "</" + nodeName + ">";
        String endTagStr2 = "/>";
        if (nodeStartTagLineNumber.compareTo(nodeEndOfEndTagLineNumber) != 0) {
            String startLine = fileContents[nodeStartTagLineNumber - 1];
            String endLine = fileContents[nodeEndOfEndTagLineNumber - 1];
            String newStartLine = "";
            String newEndLine = "";

            boolean bError = false;
            if (startLine.contains(startNodeTag)) {
                int indexStart = startLine.indexOf(startNodeTag);
                newStartLine = startLine.substring(0, indexStart);
            } else if (startLine.contains(startNodeTag2)) {
                int indexStart = startLine.indexOf(startNodeTag2);
                newStartLine = startLine.substring(0, indexStart);
            } else {
                bError = true;
            }
            if (endLine.contains(endTagStr1)) {
                int endIndex = endLine.indexOf(endTagStr1);
                newEndLine = endLine.substring(endIndex + endTagStr1.length());
            } else if (endLine.contains(endTagStr2)) {
                int endIndex = endLine.indexOf(endTagStr2);
                newEndLine = endLine.substring(endIndex + endTagStr2.length());
            } else {
                bError = true;
            }
            if (bError) {
                throw new Exception("Could not remove node");
            }
            String oldcontents = "";
            String newContents = "";
            fileContents[nodeStartTagLineNumber - 1] = newStartLine;
            fileContents[nodeEndOfEndTagLineNumber - 1] = newEndLine;
            oldcontents = startLine;
            newContents = newStartLine;


            for (int j = nodeStartTagLineNumber;
                 j < nodeEndOfEndTagLineNumber - 1; j++) {
                oldcontents = oldcontents + fileContents[j];
                newContents = newContents + "";
                fileContents[j] = "";
            }
            oldcontents = oldcontents + endLine;
            newContents = newContents + newEndLine;
            if (bDebug) {
                System.out.println("oldContents" + oldcontents);
                System.out.println("newContents: " + newContents);
            }

        } else {
            String line = fileContents[nodeStartTagLineNumber - 1];
            String newLine = "";
            // start and end node on the same line
            if (line.contains(startNodeTag)) {
                int indexStart = line.indexOf(startNodeTag);
                newLine = line.substring(0, indexStart);
            } else {
                throw new Exception("Could not remove node because could not find start tag");
            }
            if (line.contains(endTagStr1)) {
                int endIndex = line.indexOf(endTagStr1);
                newLine =
                        newLine + line.substring(endIndex + endTagStr1.length());
            } else if (line.contains(endTagStr2)) {
                int endIndex = line.indexOf(endTagStr2);
                newLine =
                        newLine + line.substring(endIndex + endTagStr2.length());
            } else {
                throw new Exception("could not remove node");
            }
            if (bDebug) {
                System.out.println("start and end tag on same line: ");
                System.out.println("Old line: " + line);
                System.out.println("New line: " + newLine);
            }
            fileContents[nodeStartTagLineNumber - 1] = newLine;
        }
    }


    /**
     * Gets the line number of the end of the start tag in the source file
     * @param componentNode
     * componentNode whose line number is to be found
     * @return
     * returns the line number in the source file
     */
    public Integer getLineNumberInSourceFile(Node componentNode) {
        if (componentNode instanceof XMLElement) {
            XMLElement xmlComponentNode = (XMLElement)componentNode;
            Integer lineNum = xmlComponentNode.getLineNumber();
            return lineNum;
        }

        return 0;
    }

    /**
     * Gets the column number of the end of the start tag in the soruce file
     * @param componentNode
     * ComponentNode whose column number is to be found
     * @return
     * Returns the column number in the source file
     */
    public Integer getColumnNumberInSourceFile(Node componentNode) {
        if (componentNode instanceof XMLElement) {
            XMLElement xmlComponentNode = (XMLElement)componentNode;
            Integer columnNumber = xmlComponentNode.getColumnNumber();
            return columnNumber;
        }

        return 0;
    }

    /**
     * Gets the line number of the end of the end tag in the source file
     * @param componentNode
     * componentNode whose line number is to be found
     * @return
     * returns the line number in the source file
     */
    public Integer getEndTagLineNumber(Node componentNode) {
        if (componentNode instanceof XMLElement) {
            XMLElement xmlComponentNode = (XMLElement)componentNode;
            Integer lineNum = xmlComponentNode.getEndTagLineNum();
            return lineNum;
        }

        return 0;
    }

    private String modifyAttribute(String originalLine,
                                   String attributeToModify, String newValue) {
        String newLine = "";
        if (originalLine.contains(attributeToModify)) {
            int startIndex = originalLine.indexOf(attributeToModify);
            int endIndex =
                originalLine.indexOf("\"", startIndex + attributeToModify.length());
            newLine = originalLine.substring(0, startIndex);
            newLine = newLine + attributeToModify;
            newLine = newLine + newValue;
            newLine = newLine + originalLine.substring(endIndex);
        }
        return newLine;
    }

    private String removeAttribute(String originalLine,
                                   String attributeToRemove) {
        String newLine = "";
        if (originalLine.contains(attributeToRemove)) {
            int startIndex = originalLine.indexOf(attributeToRemove);
            int endIndex = originalLine.indexOf("\"");
            newLine = originalLine.substring(0, startIndex);
            newLine = newLine + " ";
            newLine = newLine + originalLine.substring(endIndex);
        }
        return newLine;
    }


    public String getFileContentsAsString(String[] fileContentsArray,
                                          boolean removeBlankLines) {
        StringBuffer sb = new StringBuffer();
        int len = fileContentsArray.length;
        for (int i = 0; i < len; i++) {
            String line = fileContentsArray[i];
            if (removeBlankLines) {
                if (line.trim().equals("")) {
                    continue;
                }
            }
            sb.append(line);

        }
        return sb.toString();
    }

    public String getFileContentsAsString(boolean removeBlankLines) throws Exception{
        
        if (fileContents == null) {
            fileContents = convertFileToStringArray(m_file);
        }
        StringBuffer sb = new StringBuffer();
        int len = fileContents.length;
        for (int i = 0; i < len; i++) {
            String line = fileContents[i];
            if (removeBlankLines) {
                if (line.trim().equals("")) {
                    continue;
                }
            }
            sb.append(line);

        }
        return sb.toString();
    }

    public String[] convertFileToStringArray() throws Exception {
        if (m_file != null) {
            return convertFileToStringArray(m_file);
        } else {
            throw new Exception("File does not exist");
        }
    }

    public String[] convertFileToStringArray(File file) throws Exception {

        ArrayList<String> fileContents = new ArrayList<String>(500);

        File fp = new File(file.getAbsolutePath());
        FileReader reader = new FileReader(fp);
        BufferedReader input = new BufferedReader(reader);

        FileReader reader2 = new FileReader(fp);
        BufferedReader input2 = new BufferedReader(reader2);

        String line;
        input.mark(1024);

        while ((line = input.readLine()) != null) {
            input2.skip(line.length());
            input2.mark(1024);
            int c1 = input2.read();
            int c2 = input2.read();
            input2.reset();

            /* Determine the new line character being used in the file.. there were some errors in
             * processing due to carriage return characters.. this is required to handle new line characters
             * in the file formatting*/
            String newline = "\n";
            if (c1 == -1) {
                newline = "";
            } else if (c1 == '\n') {
                input2.read();
            } else if (c1 == '\r' && c2 != '\n') {
                newline = "\r";
                input2.read();
            } else if (c1 == '\r' && c2 == '\n') {
                newline = "\r\n";
                input2.read();
                input2.read();
            }
            FILE_NEW_LINE = newline;
            fileContents.add(line + newline);
        }
        if (input2 != null) {
            input2.close();
        }
        if (reader2 != null) {
            reader2.close();
        }
        if (input != null) {
            input.close();
        }
        if (reader != null) {
            reader.close();
        }
        return fileContents.toArray(new String[fileContents.size()]);

    }

    public void setBDebug(boolean bDebug) {
        this.bDebug = bDebug;
    }

    public boolean isBDebug() {
        return bDebug;
    }

    public void setFileContents(String[] fileContents) {
        this.fileContents = fileContents;
    }

    public String[] getFileContents() {
        return fileContents;
    }
}
