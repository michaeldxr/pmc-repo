package oracle.apps.helpers.parsers;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.FamilyModuleHelper;

/*
 * PLSQL Syntax
 *
 * PROCECURE name ()
 *   some declarations
 * IS
 * BEGIN
 *  logic
 * EXCEPTION
 *  error handling
 * END name
 *
 * same as above for FUNCTION
 *
 * PRAGMA AUTONOMOUS TRANSACTION goes between procedure and begin
 */

public class PlsqlFileParser extends GenericFileParserHelper {
    public PlsqlFileParser(String path) throws Exception {
        super(path);
    }

public static void main(String[] args) throws Exception
{
PlsqlFileParser parser = new PlsqlFileParser(FamilyModuleHelper.getLabelServer("FUSIONAPPS_PT.11.1.1_LINUX.X64") + "/fusionapps/fin/ap/db/plsql/ap_retainage_release_pkg.pkb");
    String line1="commit to;";
    System.out.println("Testing line:"+line1+" Status: " + parser.hasCommitOrRollback(line1));
    
       
        
    }

public static void printTable(Hashtable h)
{
    Set s = h.keySet();
    Iterator  siter = s.iterator();
    while(siter.hasNext())
    {
        Object o = siter.next();
        System.out.println("Key: " + o +" Value: "+h.get(o));
    }
    }
    /**
     *PLSQL Comments are of type
     * /star star/ (Multiline same as java)
     * REM (Single line)
     * -- (single line)
     * @throws Exception
     */
    public void removeAllComments() throws Exception {


        String origFileContents = getFileContentsAsString(getFileContents(), false);
        try {

            // --
            fileWithoutCommentsReadOnly = removeSingleLineComments(origFileContents, "--.*$");
            // Line starts with REM
            fileWithoutCommentsReadOnly = removeSingleLineComments(fileWithoutCommentsReadOnly, "^\\s*REM.*$");
            // /star start/
            String multiLineCommentsRE = "/\\*.*?\\*/";
            fileWithoutCommentsReadOnly = removeMultilineComments(fileWithoutCommentsReadOnly, multiLineCommentsRE);


        } catch (Exception ex) {
            throw new Exception("Exception while trying to remove comments from file: " + m_file.getAbsolutePath(), ex);
        }

    }

    public ArrayList<String> getAllAutonomousProceduresAndFunctions() throws Exception {
        ArrayList<String> allprocFunctions = new ArrayList<String>();

        allprocFunctions.addAll(getAllProcedures());
        allprocFunctions.addAll(getAllFunctions());

        //String procedureDecl = "procedure" + requiredWhiteSpace + "(" + identifier + ")";
        String pragmaDecl = "pragma" + requiredWhiteSpace + "AUTONOMOUS_TRANSACTION" + whiteSpace + ";";
        //String endprocedureRegEx = "end" + requiredWhiteSpace + "\\1" + ";";
        String pragMaRegEx = pragmaDecl + ".*?begin";
        Pattern pragMaPattern = Pattern.compile(pragMaRegEx, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

        ArrayList<String> pragMaProcedures = new ArrayList<String>();
        Iterator<String> allProcIter = allprocFunctions.iterator();
        while (allProcIter.hasNext()) {
            String procedure = allProcIter.next();
            Matcher m = pragMaPattern.matcher(procedure);
            if (m.find())
                pragMaProcedures.add(procedure);
        }


        return pragMaProcedures;
    }

    public boolean isNextCharacterInString(String str, Integer startIndex, char charToCheckFr)
    {
        int s = startIndex;
        int end = str.length();
        for (int i = s; i<end;i++)
        {
            char c = str.charAt(i);
            if(!Character.isWhitespace(c))
            {
                if(str.charAt(s)==charToCheckFr)
                    return true;
                else
                    return false;
            }
            
        }
        return false;
    }
    private boolean isWhiteSpaceChar(char c)
    {
        if(c==' ' || c=='\n')    
            return true;
        return false;
    }
    public ArrayList<String> getAllProcedures() throws Exception {
        ArrayList<String> procedures = new ArrayList<String>();
        String procedureDecl = "procedure" + requiredWhiteSpace + "(" + identifier + ")";
        String procedureParams="\\(.*?\\)";
        procedureParams="("+procedureParams+")"+"{0,1}";
        procedureDecl=procedureDecl+whiteSpace+procedureParams;
        // the arguments for the procedure
        //procedureDecl = procedureDecl + whiteSpace + "\\(.*?\\)";
        //procedureDecl = procedureDecl + whiteSpace + "[^;]";
        Pattern procedurePattern = Pattern.compile(procedureDecl, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        String readOnlyFileNoComments = getFileWithoutCommentsReadOnly();
        Matcher m = procedurePattern.matcher(readOnlyFileNoComments);
        while (m.find()) {
            String procedureName = m.group(1);
           // System.out.println("ProcedureName is " + procedureName);
           // System.out.println("Group 0: " + m.group());
            if(isNextCharacterInString(readOnlyFileNoComments, m.end(), ';'))
                continue;

            int startOfProcedure = m.start();

            int endOfProcedure = getEndOfBlockIndex(procedureName, startOfProcedure);
            if (endOfProcedure != -1) {
                String procedureBlock = readOnlyFileNoComments.substring(startOfProcedure, endOfProcedure);
                procedures.add(procedureBlock);
//                System.out.println("\n=============================================\n");
//                System.out.println("\nProcedure Block is:\n " + procedureBlock);
//                System.out.println("\n=============================================\n");
            }
        }

        return procedures;
    }

    public int getEndOfBlockIndex(String procedureName, int startIndex) {
        String endProcedure2 = "begin.*?end" + requiredWhiteSpace + procedureName + whiteSpace + ";";
        int endOfProcedure = -1;
        Pattern endProcedurePattern = Pattern.compile(endProcedure2, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m22 = endProcedurePattern.matcher(fileWithoutCommentsReadOnly);

        if (m22.find(startIndex)) {
            endOfProcedure = m22.end();
        }
        return endOfProcedure;
    }


    public ArrayList<String> getAllFunctions() throws Exception {
        ArrayList<String> procedures = new ArrayList<String>();
        
        if(!m_file.getName().contains("xla_cmp_tad"))
            return procedures;
        
        
        String procedureDecl = "function" + requiredWhiteSpace + "(" + identifier + ")";
        String procedureParams="\\(.*?\\)";
        procedureParams="("+procedureParams+")"+"{0,1}";
        procedureDecl=procedureDecl+whiteSpace+procedureParams;
        // the arguments for the procedure
        //procedureDecl = procedureDecl + whiteSpace + "\\(.*?\\)";
        //procedureDecl = procedureDecl + whiteSpace + "[^;]";
        Pattern procedurePattern = Pattern.compile(procedureDecl, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        String readOnlyFileNoComments = getFileWithoutCommentsReadOnly();
        Matcher m = procedurePattern.matcher(readOnlyFileNoComments);
        while (m.find()) {
            String procedureName = m.group(1);
           // System.out.println("ProcedureName is " + procedureName);
          //  System.out.println("Group 0: " + m.group());
            if(isNextCharacterInString(readOnlyFileNoComments, m.end(), ';'))
                continue;

            // check if the next line after the declaration block is a RETURN ;
            
            int startOfProcedure = m.start();
        
            

            int endOfProcedure = getEndOfBlockIndex(procedureName, startOfProcedure);
            if (endOfProcedure != -1) {
                String procedureBlock = readOnlyFileNoComments.substring(startOfProcedure, endOfProcedure);
                if(isValidProcedureBlock(procedureBlock))
                {
                procedures.add(procedureBlock);
                }else{
                   // System.out.println("Invalid procedure block : \n"+procedureBlock+"\n\n");
                    }
//                System.out.println("\n=============================================\n");
//                System.out.println("\nProcedure Block is:\n " + procedureBlock);
//                System.out.println("\n=============================================\n");
            }
            
           
            
        }

        return procedures;
    }

public boolean isValidProcedureBlock(String block)
{
    
    String procedureRegEx="\\b(procedure|function)\\b"+requiredWhiteSpace;
    Pattern p = Pattern.compile(procedureRegEx, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    Matcher m = p.matcher(block);
    int numberOfoccurences = 0;
    while(m.find())
    {
            numberOfoccurences++;
        }
    if(numberOfoccurences!=1)
        return false;
    return true;
    }

    public String getExceptionblockFromProcedure(String procedureText) throws Exception {
        if (procedureText == null || procedureText.length() < 1)
            return "";
        String exceptionRegEx = "exception" + requiredWhiteSpace;
        Pattern p = Pattern.compile(exceptionRegEx, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(procedureText);
        if (m.find())
            return procedureText.substring(m.start());
        return "";
    }

    public String getNameFromBlock(String block) {
        String procedureDecl = "procedure" + requiredWhiteSpace + "(" + identifier + ")";
        String funcitonDecl = "function" + requiredWhiteSpace + "(" + identifier + ")";
        Pattern procPat = Pattern.compile(procedureDecl, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = procPat.matcher(block);
        if (m.find())
            return m.group(1);

        Pattern funcPat = Pattern.compile(funcitonDecl);
        Matcher m2 = funcPat.matcher(block);
        if (m2.find())
            return m2.group(1);
        return "";

    }

    public boolean willStatementReturn(String str) {
        if (isEmpty(str))
            return false;
        String terminatorRegEx = "(RETURN|RAISE)" + whiteSpace + ";";
        Pattern p = Pattern.compile(terminatorRegEx, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(str);
        if (m.find())
            return true;

        return false;
    }

    public boolean hasCommitOrRollback(String str) {
        if (isEmpty(str))
            return false;
        String endOfStmt="[\\s;]+";
        String terminatorRegEx = "(COMMIT|ROLLBACK)" + endOfStmt;
        Pattern p = Pattern.compile(terminatorRegEx, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher m = p.matcher(str);
        if (m.find())
            return true;

        return false;
    }
}
