package oracle.apps.idAnalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import oracle.apps.NullEntityResolver;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLAttr;
import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NamingCntIdFileProcessor {

    /**
     * Number of violations fixed
     */
    int ncViolationsFixed = 0;

    /**
     * Number of violations not fixed due to formatting
     */
    int ncViolationsNotFixed = 0;

    /**
     * Number of secondary references fixed
     */
    int secondaryRefsFixed = 0;

    /**
     * Number of secondary references not fixed
     */
    int secondaryRefsNotFixed = 0;

    /**
     * Number of secondary references
     */
    int numberOfSecondaryRefs = 0;

    /**
     * HashSet of all the seen component ids in the file.
     * This is required to ensure that there are no duplicate ids in a UI file
     */
    HashSet<String> seenComponentIds = new HashSet<String>(500);


    /**
     * This is a list of all such adf components that do not support componentIds.
     * Typically this kind of information is obtained from the component defintiions. But we do
     * not have the ability over here to get the component definitions and find out if the component supports an id.
     * Hence we have a predefined list of components that need to be excluded while checking for components with Ids
     * e.g. af:setPropertyListener does not support setting an id
     */
    HashSet<String> namingContainers = new HashSet<String>(100);

    /**
     * Hashmap of components that appear as partial triggers
     * Key: line number in the file that a secondaryReferenceContainer is found
     * Value : line number in the source file
     */
    HashMap<Integer, ArrayList<PartialTriggers>> secondaryRefs_byLineNumber =
        new HashMap<Integer, ArrayList<PartialTriggers>>(100);

    /**
     * Secondary references by component id
     */
    HashMap<String, ArrayList<PartialTriggers>> secondaryRefs_byCompId =
        new HashMap<String, ArrayList<PartialTriggers>>(100);

    /**
     * Hashmap of triggers that reference atleast one component id that is a violation
     * All these referenced triggers need to be updated
     * ArrayList is required since we get the line number of the entire tag. There maybe more than one secondary reference attributes present at that line,
     * hence we need a list to keep track of all the reference attributes on that line
     */
    HashMap<Integer, ArrayList<PartialTriggers>> violatingTriggers_byLineNumber =
        new HashMap<Integer, ArrayList<PartialTriggers>>(100);

    /**
     * key - line number in the source file where the component is found
     * Value - component details of the violating component
     * We need to store the line number as the key because it will help when we are trying to fix all the files. We can only read the file serially and hence we will have
     * to iterate over all the lines in the files to get to the violating line number. At that point
     * we can just do a contains chceck to find if the line number in the file is to be fixed.
     * Assumption - a particular line will contain only one violating component. Keeping it simple by not using array list within the hash map
     */
    HashMap<Integer, ComponentDetails> violatingComponents =
        new HashMap<Integer, ComponentDetails>(100);


    /**
     * Map of all the changed ids
     * Key: oldId, Value: newId
     */
    HashMap<String, String> changedIds = new HashMap<String, String>(100);

    /**
     * length of the id beyond which the component is flagged as a violation
     */
    final static Integer CUT_OFF_ID_LENGTH = 7;

    /**
     * these are the list of component attributes or containres that might reference the violating component
     * e.g. partialTriggers
     */
    HashSet<String> secondaryComponentContainers = new HashSet<String>(10);


    public static class ComponentConstants {
        static String ID_ATTR = "id";
        static String FACES_COMP_PREFIX = "af:";
        static String PARTIAL_TRIGGER_TAG = "partialTriggers";
        static String TRIGGER_COMP_SEPERATOR = " ";
        static String TRIGGER_COMP_ID_SEPEARATOR = ":";
    }


    /**
     * Constructor
     * Initializes a list of all naming containers
     * Initializes a list of all secondary reference containers that will have to be scanned for. If a component id is to be changed
     * then all the secondary references of it will also have to be changed
     */
    public NamingCntIdFileProcessor() {
        super();
        intializeListOfNamingContainers();
        initializeSecondaryCompIdRef();
    }

    /**
     * Main Method for this file
     * Entry point and parses the file to look for all naming container violations
     * @param file
     * @param bDoADE
     */
    public void analyzeNamingCntsForSpreadsheet(File file, boolean bDoADE) {

        DOMParser parser = null;
        String absFilePath = file.getAbsolutePath();
  //      System.out.print("..");


        try {
            parser = initializeDomParser();
            parser.parse("file:" + absFilePath);
        } catch (Exception ex) {
            System.out.println("Error while initializing dom parser for file.. Skipping " +
                               absFilePath);
            return;
        }

        XMLDocument doc = parser.getDocument();
        Element rootNode = doc.getDocumentElement();

        if (rootNode.hasChildNodes()) {

            traverseNodes(rootNode.getChildNodes(), file);
        }
        if (violatingComponents.size() > 0) {
            updateComponentsWithSuggestedIds(violatingComponents);
        }
    }

    /**
     *Fix all naming container id violations
     * 1. Parse the file with an xml parser
     *  - generate a list of all seen ids
     *  - generate the violating component map indexed by the line number in the file
     *      - isANamingContainer && idLength>cutOff && belongsToCsvComponentList
     *  - generate a list of all secondary component references
     *
     *  2. If violating components detected
     *
     * @param file
     * @param bDoADE
     * @param csvComponentsForFile
     * @return
     */
    public String fixNamingContainerIdViolations(File file, boolean bDoADE,
                                                 ArrayList<ComponentDetails> csvComponentsForFile) {

        DOMParser parser = null;
        String absFilePath = file.getAbsolutePath();

        if (!bDoADE) {
            //*           System.out.println("Processing file: " + file.getName());
        }


        try {
            parser = initializeDomParser();
            parser.parse("file:" + absFilePath);
        } catch (Exception ex) {
            System.out.println("Error while initializing dom parser for file: " +
                               absFilePath);
            return null;
        }

        XMLDocument doc = parser.getDocument();
        Element rootNode = doc.getDocumentElement();

        if (rootNode.hasChildNodes()) {
            // only build a list of seen ids and secondary references.. dont generate the violating component map yet
            traverseNodes(rootNode.getChildNodes(), file,
                          csvComponentsForFile);
        }

        if (violatingComponents.size() > 0) {

            try {
                return makeFixes(file, violatingComponents, seenComponentIds,
                                 bDoADE);
            } catch (Exception ex) {
                System.out.println("Error occurred while making fixes.. skipping");
            }


        }
        return null;
    }

    private String convertStringArrayToString(String[] fileContents) {
        StringBuffer sb = new StringBuffer();
        int len = fileContents.length;
        for (int i = 0; i < len; i++) {
            sb.append(fileContents[i]);
        }
        return sb.toString();
    }

    /**
     * Reads the file and fixes all the violating components.
     * Returns the new file contents as a string
     * @param f
     * @param lineNumberWithComponents
     * @param seenIds
     * @param bDoAde
     * @return
     * @throws Exception
     */
    private String makeFixes(File f,
                             HashMap<Integer, ComponentDetails> lineNumberWithComponents,
                             HashSet<String> seenIds,
                             boolean bDoAde) throws Exception {

        String[] fileContents = convertFileToStringArray(f);
        if (fileContents == null) {
            System.out.println("[convertFileToStringArray] Error while fixing file: " +
                               f.getName());
            return null;
        }

        String[] changedFileContents = fileContents.clone();


        Set<Integer> set_lineNums = lineNumberWithComponents.keySet();
        Iterator<Integer> linenum_iter = set_lineNums.iterator();
        while (linenum_iter.hasNext()) {
            Integer lineNum = linenum_iter.next();
            ComponentDetails component = lineNumberWithComponents.get(lineNum);
            String componentName = component.getComponentName().trim();
            String componentid = component.getComponentId().trim();
            String strSearchString =
                " " + ComponentConstants.ID_ATTR + "=\"" + componentid + "\"";

            boolean bFound = false;
            String changedLine = "";
            // offset = 1 since index = length -1
            int offset = 1;
            while (!bFound) {
                int lineOffset = lineNum - offset;
                if (lineOffset < 0) {
                    System.out.println("****[Attention]Could not find search string: " +
                                       strSearchString);
                    ncViolationsNotFixed++;
                    break;
                }
                String str = fileContents[lineOffset];
                if (str.contains(strSearchString)) {
                    bFound = true;
                    String generatedId = component.getSuggestedId();
                    if (seenIds.contains(generatedId)) {
                        System.out.println("File: " + f.getName() +
                                           ". For componentId: " +
                                           componentid +
                                           "Cannot use the suggested Id:" +
                                           generatedId +
                                           ". Reason: Duplicate");

                        ncViolationsNotFixed++;
                        break;
                    }

                    String replacement =
                        " " + ComponentConstants.ID_ATTR + "=\"" +
                        generatedId + "\"";
                    changedLine =
                            str.replaceFirst(strSearchString, replacement);
                    changedFileContents[lineOffset] = changedLine;
                    changedIds.put(componentid, generatedId);
                    ncViolationsFixed++;
                    seenIds.add(generatedId);
                    if (!bDoAde) {
                        //*                                 System.out.println("****old line: " + str);
                        //*                                 System.out.println("****new line: " + changedLine);
                    }

                }
                offset++;
                if (offset > 50) {
                    System.out.println("[ERROR] Could not find search string: " +
                                       strSearchString);
                    ncViolationsNotFixed++;
                    break;
                }

            }
        }

        // Processing secondary references

        createMapOfViolatingTriggers();

        if (!violatingTriggers_byLineNumber.isEmpty()) {

            Set<Integer> set_trigger_lineNums =
                violatingTriggers_byLineNumber.keySet();
            Iterator<Integer> trigger_line_iter =
                set_trigger_lineNums.iterator();
            while (trigger_line_iter.hasNext()) {


                Integer triggerLineNum = trigger_line_iter.next();
                ArrayList<PartialTriggers> list_triggers =
                    violatingTriggers_byLineNumber.get(triggerLineNum);
                Iterator<PartialTriggers> triggers_iter =
                    list_triggers.iterator();
                while (triggers_iter.hasNext()) {
                    PartialTriggers trigger = triggers_iter.next();
                    String triggerRaw = trigger.getRawTriggerOn();
                    String triggerName = trigger.getTriggerName();

                    String triggerSearchString =
                        triggerName + "=\"" + triggerRaw + "\"";
                    // offset = 1 since index = length -1
                    int offset = 1;
                    boolean bFound = false;
                    while (!bFound) {
                        int lineOffset = triggerLineNum - offset;
                        if (lineOffset < 0) {
                            System.out.println("****[Attention]Could not find search string: " +
                                               triggerSearchString);
                            secondaryRefsNotFixed++;
                            break;
                        }

                        String str = changedFileContents[lineOffset];
                        if (str.contains(triggerSearchString)) {
                            bFound = true;

                            int indexOfTriggerStart =
                                str.indexOf(triggerSearchString);
                            if (indexOfTriggerStart != -1) {

                                String beforeTriggerStarts =
                                    str.substring(0, indexOfTriggerStart);
                                String afterTriggerEnds =
                                    str.substring(indexOfTriggerStart +
                                                  triggerSearchString.length());
                                String changedTriggerValue =
                                    triggerName + "=\"" +
                                    trigger.getTriggerReplacement() + "\"";
                                if (!isEmpty(changedTriggerValue)) {
                                    String changedLine =
                                        beforeTriggerStarts + changedTriggerValue +
                                        afterTriggerEnds;
                                    changedFileContents[lineOffset] =
                                            changedLine;
                                    secondaryRefsFixed++;
                                    if (!bDoAde) {
                                        //*                                          System.out.println("****old line: " +
                                        //*                                                             str);
                                        //*                                         System.out.println("****new line: " +
                                        //*                                                            changedLine);
                                    }
                                } else {
                                    secondaryRefsNotFixed++;
                                }


                            } else {
                                secondaryRefsNotFixed++;
                            }

                        }

                        offset++;
                        if (offset > 50) {
                            System.out.println("Could not find search string: " +
                                               triggerSearchString);
                            secondaryRefsNotFixed++;
                            break;
                        }


                    }
                }

            }
        }
        // printAnyOtherReferences(changedFileContents, changedIds);
        return convertStringArrayToString(changedFileContents);
    }


    private void printAnyOtherReferences(String[] changedFileContents,
                                         HashMap<String, String> changedIds) {

        Set<String> oldIds_set = changedIds.keySet();
        Iterator<String> oldId_iter = oldIds_set.iterator();
        while (oldId_iter.hasNext()) {
            String oldId = oldId_iter.next();

            int len = changedFileContents.length;
            for (int i = 0; i < len; i++) {
                String line = changedFileContents[i];
                String searchStr1 = "\"" + oldId + "\"";
                String searchStr2 = "'" + oldId + "'";
                if (line.contains(searchStr1) || line.contains(searchStr2)) {
                    System.out.println("**** [ATTTENTTTTION] line: " +
                                       (i + 1) + "oldId: " + oldId +
                                       " maybe referenced in line: " + line);
                }
            }
        }
    }

    private void createMapOfViolatingTriggers() {
        Set<Integer> set_lineNumbers = secondaryRefs_byLineNumber.keySet();
        if (set_lineNumbers == null) {
            return;
        }
        Iterator<Integer> lineNumIter = set_lineNumbers.iterator();
        if (lineNumIter == null) {
            return;
        }
        while (lineNumIter.hasNext()) {

            Integer lineNum = lineNumIter.next();
            ArrayList<PartialTriggers> list_triggers =
                secondaryRefs_byLineNumber.get(lineNum);
            Iterator<PartialTriggers> trigger_iter = list_triggers.iterator();
            while (trigger_iter.hasNext()) {
                PartialTriggers trigger = trigger_iter.next();
                HashSet<String> compIdsReferenced =
                    trigger.getTriggerOnSplitIds();
                Iterator<String> compIdsRef_iter =
                    compIdsReferenced.iterator();
                HashSet<String> onlyViolatingCompIds = new HashSet<String>(30);
                boolean containsViolatingId = false;
                while (compIdsRef_iter.hasNext()) {
                    String compId = compIdsRef_iter.next();
                    if (changedIds.containsKey(compId)) {
                        onlyViolatingCompIds.add(compId);
                        containsViolatingId = true;
                    }
                }

                if (containsViolatingId) {
                    trigger.setViolatingCompId(onlyViolatingCompIds);
                    String rawString = trigger.getRawTriggerOn();
                    String replacedStr =
                        getRawTriggerReplacedString(rawString, onlyViolatingCompIds,
                                                    changedIds);

                    //System.out.println("oldString: " + rawString);
                    //System.out.println("replacedString: " + replacedStr);
                    trigger.setTriggerReplacement(replacedStr);
                    addToSecondaryRefByLineNum(violatingTriggers_byLineNumber,
                                               lineNum, trigger);
                    numberOfSecondaryRefs++;
                }

            }
            //PartialTriggers trigger = secondaryRefs_byLineNumber.get(lineNum);
            //HashSet<String> compIdsReferenced = trigger.getTriggerOnSplitIds();
            //Iterator<String> compIdsRef_iter = compIdsReferenced.iterator();
            //HashSet<String> onlyViolatingCompIds = new HashSet<String>(30);
            /*boolean containsViolatingId = false;
            while (compIdsRef_iter.hasNext()) {
                String compId = compIdsRef_iter.next();
                if (changedIds.containsKey(compId)) {
                    onlyViolatingCompIds.add(compId);
                    containsViolatingId = true;
                }
            }*/

            /* if (containsViolatingId) {
                trigger.setViolatingCompId(onlyViolatingCompIds);
                String rawString = trigger.getRawTriggerOn();
                String replacedStr =
                    getRawTriggerReplacedString(rawString, onlyViolatingCompIds,
                                                changedIds);
                System.out.println("oldString: " + rawString);
                System.out.println("replacedString: " + replacedStr);
                trigger.setTriggerReplacement(replacedStr);
                addToSecondaryRefByLineNum(violatingTriggers_byLineNumber,
                                           lineNum, trigger);
            }*/
        }
    }


    private String getRawTriggerReplacedString(String rawTriggerString,
                                               HashSet<String> violatingIds,
                                               HashMap<String, String> changedIds) {
        // raw trigger string - is the trigger string to be searched against
        String replacedString = rawTriggerString;
        Iterator<String> violatingId_Iter = violatingIds.iterator();
        while (violatingId_Iter.hasNext()) {
            String violatingId = violatingId_Iter.next();
            String changedId = changedIds.get(violatingId);
            if (isEmpty(violatingId) || isEmpty(changedId)) {
                continue;
            }
            int foundIndex = 0;

            int startIndex = 0;
            // consider using a regex to replace all
            while ((foundIndex =
                    replacedString.indexOf(violatingId, startIndex)) != -1) {
                if (isValidMatchIndex(replacedString, violatingId,
                                      foundIndex)) {
                    String start = replacedString.substring(0, foundIndex);
                    String end =
                        replacedString.substring(foundIndex + violatingId.length());
                    replacedString = start + changedId + end;

                    startIndex = foundIndex + changedId.length();
                } else {
                    startIndex = startIndex + 1;
                }
            }

        }


        return replacedString;
    }

    private boolean isValidMatchIndex(String str, String violatingId,
                                      int startIndex) {
        char prevChar;
        char nextChar;
        int prevCharIndex = startIndex - 1;
        int nextCharIndex = startIndex + violatingId.length();
        if (prevCharIndex < 0) {
            prevChar = ' ';
        } else {
            prevChar = str.charAt(prevCharIndex);
        }
        if (nextCharIndex > str.length() - 1) {
            nextChar = ' ';
        } else {
            nextChar = str.charAt(nextCharIndex);
        }


        if (isPossibleDelimiterCharacter(prevChar) &&
            isPossibleDelimiterCharacter(nextChar)) {
            return true;
        }
        return false;
    }

    private boolean isPossibleDelimiterCharacter(char c) {
        String s = Character.toString(c);
        if (s.equals(":") || s.equals("\"") || s.equals("[") ||
            s.equals(" ") || s.equals("]")) {
            return true;
        }
        return false;
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
            fileContents.add(line + newline);
        }
        reader.close();
        return fileContents.toArray(new String[fileContents.size()]);

    }

    private ComponentDetails getComponentFromList(ArrayList<ComponentDetails> list_components,
                                                  String componentName,
                                                  String componentId) {
        Iterator<ComponentDetails> comp_iter = list_components.iterator();
        while (comp_iter.hasNext()) {
            ComponentDetails comp = comp_iter.next();
            String compName = comp.getComponentName();
            String compId = comp.getComponentId();
            if (compName.equals(componentName) && compId.equals(componentId)) {
                return comp;
            }
        }
        return null;
    }

    private void updateComponentsWithSuggestedIds(HashMap<Integer, ComponentDetails> analyzedComponents) {
        Set<Integer> set_lines = analyzedComponents.keySet();
        Iterator<Integer> lines_iter = set_lines.iterator();
        while (lines_iter.hasNext()) {
            Integer lineNum = lines_iter.next();
            ComponentDetails comp = analyzedComponents.get(lineNum);
            String componentName = comp.getComponentName();
            String generatedId =
                generateComponentId(seenComponentIds, componentName);
            comp.setSuggestedId(generatedId);
            seenComponentIds.add(generatedId);
            analyzedComponents.put(lineNum, comp);
        }
    }

    /**
     * Main method that traverses all the nodes in the xml equivalent of the input file
     * Recursevely traverses all the nodes and their children, and builds the map of all seen component ids.
     * for each component seen, determines if it is a valid faces component that supports ids.
     * If it is a valid component, and the component id is empty, it adds it to the list of
     * violating components along with the line number in the source file.
     *
     * output:
     * 1. HashSet of all seen componentIds
     * 2. Map of all the violating components, indexed by the source line number
     * @param childNodes
     */
    public void traverseNodes(NodeList childNodes, File uiFile,
                              ArrayList<ComponentDetails> csvListOfViolations) {

        if (childNodes == null) {
            return;
        }
        String uiFilePath = uiFile.getPath();

        int childNodeLength = childNodes.getLength();
        for (int i = 0; i < childNodeLength; i++) {
            Node child = childNodes.item(i);

            String componentName = child.getNodeName();
            Node componentIdNode = getIdAttrNode(child);
            String componentId = "";
            if (componentIdNode != null) {
                componentId = componentIdNode.getNodeValue();
            }
            if (!isEmpty(componentId)) {
                // Only if you need to process naming containers..

                if (namingContainers.contains(componentName)) {
                    Integer idLength = componentId.length();
                    if (idLength > CUT_OFF_ID_LENGTH) {
                        ComponentDetails comp =
                            getComponentFromList(csvListOfViolations,
                                                 componentName, componentId);
                        if (comp == null) {
                            System.out.println("\n FYI.. There was a violation that was not found in the csv file: " +
                                               componentName + " id:" +
                                               componentId);
                        } else {
                            Integer lineNumber =
                                getLineNumberOfAttrNode(componentIdNode);
                            comp.setSourceLineNumber(lineNumber);
                            addToViolatingComponentMap(lineNumber, comp);
                        }

                    }

                }


                // this is just to check that there are no duplicates
                if (seenComponentIds.contains(componentId)) {
                    System.out.println("\n FYI .. Found a duplicate id  : " +
                                       componentId + " while scanning file: " +
                                       uiFile.getAbsolutePath());
                }
                seenComponentIds.add(componentId);
            }
            /**
                 * For each node, see if it has any secondary reference containers and maintain a list of all the referenced
                 * ids in that container. In the second pass of the file, if a component id is required to be changed, then all
                 * associated references will also have to be changed
                 */

            if (isSecondaryRefContainer(child)) {
                processSecondaryRefContainers(child);
            }


            // If it is only for analysis purposes - spreadsheet generation, do not need to look for triggers..
            // OR Maybe I can .. will decide..

            if (child.hasChildNodes()) {
                traverseNodes(child.getChildNodes(), uiFile,
                              csvListOfViolations);
            }

        }
    }

    public void traverseNodes(NodeList childNodes, File uiFile) {

        if (childNodes == null) {
            return;
        }
        String uiFilePath = uiFile.getPath();

        int childNodeLength = childNodes.getLength();
        for (int i = 0; i < childNodeLength; i++) {
            Node child = childNodes.item(i);

            String componentName = child.getNodeName();
            Node componentIdNode = getIdAttrNode(child);
            String componentId = "";
            if (componentIdNode != null) {
                componentId = componentIdNode.getNodeValue();
            }
            if (!isEmpty(componentId)) {
                // Only if you need to process naming containers..

                if (namingContainers.contains(componentName)) {
                    Integer idLength = componentId.length();
                    if (idLength > CUT_OFF_ID_LENGTH) {
                        // violating component
                        String productName = getProductName(uiFilePath);
                        Integer lineNumber =
                            getLineNumberOfAttrNode(componentIdNode);
                        String serializedTag = getNodeAsString(child, "");
                        ComponentDetails component = new ComponentDetails();
                        component.setComponentName(componentName);
                        component.setFileName(uiFile.getName());
                        component.setFilePath(uiFilePath);
                        component.setProduct(productName);
                        component.setSourceLineNumber(lineNumber);
                        component.setIdLength(idLength);
                        component.setComponentId(componentId);
                        component.setSerializedTag(serializedTag);
                        addToViolatingComponentMap(lineNumber, component);
                        // Analysis purposes - using a map of the idLength vs component
                        //addToViolationsPerLength(idLength, component);
                    }

                }


                // this is just to check that there are no duplicates
                if (seenComponentIds.contains(componentId)) {
                    System.out.println("\n FYI .. Found a duplicate id  : " +
                                       componentId + " while scanning file: " +
                                       uiFile.getAbsolutePath());
                }
                seenComponentIds.add(componentId);
            }
            /**
                 * For each node, see if it has any secondary reference containers and maintain a list of all the referenced
                 * ids in that container. In the second pass of the file, if a component id is required to be changed, then all
                 * associated references will also have to be changed
                 */

            if (isSecondaryRefContainer(child)) {
                processSecondaryRefContainers(child);
            }


            // If it is only for analysis purposes - spreadsheet generation, do not need to look for triggers..
            // OR Maybe I can .. will decide..

            if (child.hasChildNodes()) {
                traverseNodes(child.getChildNodes(), uiFile);
            }

        }
    }

    /**
     * generates a non duplicate component id based on the component name
     * 1. Makes sure that it is not a duplicate
     * 2. once generated does NOT add it to the list of seen ids. It is the responsibility of the
     *    caller to add it to the list of seen ids once this id is consumed
     * @param seenIds
     * @param componentName
     * @return
     */
    private String generateComponentId(HashSet<String> seenIds,
                                       String componentName) {

        String generatedId = "";
        String shortName = generateShortName(componentName);
        if (isEmpty(shortName)) {
            return "";
        }
        Integer idSuffix = 0;

        do {
            idSuffix++;
            generatedId = shortName + idSuffix.toString();
        } while (seenIds.contains(generatedId));
        return generatedId;
    }

    /**
     * Generate the short name of the component
     * e.g. outputText will be ot
     * panelGroupLayout will be pgl
     * @param componentName
     * @return
     */
    private String generateShortName(String componentName) {
        String shortName = "";

        if (isEmpty(componentName)) {
            return shortName;
        }
        if (componentName.startsWith("af:")) {
            Integer prefixIndex = componentName.indexOf("af:");
            String pureComponentName =
                componentName.substring(prefixIndex + "af:".length());

            for (int i = 0; i < pureComponentName.length(); i++) {
                if (i == 0) {
                    char firstChar = pureComponentName.charAt(i);
                    shortName = String.valueOf(firstChar);

                } else {
                    Character c = pureComponentName.charAt(i);
                    if (Character.isUpperCase(c)) {
                        shortName =
                                shortName + (String.valueOf(c).toLowerCase());
                    }
                }

            }

        }

        return shortName;
    }

    private String getNodeAsString(Node node, String indent) {
        StringWriter sw = new StringWriter();
        switch (node.getNodeType()) {

        case Node.DOCUMENT_NODE:
            String str = "<xml version=\"1.0\">\n";
            sw.append(str);


            break;

        case Node.ELEMENT_NODE:
            String name = node.getNodeName();
            sw.append(indent + "<" + name);
            NamedNodeMap attributes = node.getAttributes();

            for (int i = 0; i < attributes.getLength(); i++) {
                Node current = attributes.item(i);
                sw.append(" " + current.getNodeName() + "=\"" +
                          current.getNodeValue() + "\"");
            }
            sw.append(">");


            break;

        case Node.TEXT_NODE:
            sw.append(node.getNodeValue());
            break;
        }

        return sw.toString();

    }

    /**
     * Gets the component Id from the XML Node
     * @param childNode
     * Child Node
     * @return
     * ID of the Node
     */
    private Node getIdAttrNode(Node childNode) {

        NamedNodeMap attributes = childNode.getAttributes();
        if (attributes != null && attributes.getLength() > 0) {
            Node idNode = attributes.getNamedItem(ComponentConstants.ID_ATTR);


            if (idNode != null) {
                return idNode;
            }
        }
        return null;
    }

    /**
     * Checks if it is a container of a possible component id references.
     * e.g. the component might contain partialTriggers.
     * Even if it has one of the secondary component id references it returns true
     * @param componentNode
     * ComponentNode whose attributelist is to be checked
     * @return
     * true - contains one of the possible secondary reference attributes
     */
    private boolean isSecondaryRefContainer(Node componentNode) {
        NamedNodeMap attributes = componentNode.getAttributes();
        if (attributes != null && attributes.getLength() > 0) {

            Iterator<String> secondaryComponents =
                secondaryComponentContainers.iterator();
            while (secondaryComponents.hasNext()) {
                String secondaryTag = secondaryComponents.next().trim();
                Node secondaryAttr = attributes.getNamedItem(secondaryTag);
                if (secondaryAttr != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * for the given node, checks if it contains any attribute that is a possible reference container (e.g. partial trigger)
     * for each reference container found -
     *  1. the the raw string value
     *  2. get the split component ids
     *  3. create a partial trigger component, with the information
     *  4. Index it by the line number of the attribute and store it in the 'secondaryRefs_byLineNumber map
     * @param componentNode
     */
    private void processSecondaryRefContainers(Node componentNode) {
        NamedNodeMap attributes = componentNode.getAttributes();
        if (attributes == null || attributes.getLength() <= 0) {
            return;
        }

        Iterator<String> secondaryComponents =
            secondaryComponentContainers.iterator();
        while (secondaryComponents.hasNext()) {
            String secondaryTag = secondaryComponents.next().trim();
            Node secondaryAttr = attributes.getNamedItem(secondaryTag);
            if (secondaryAttr == null) {
                continue;
            }

            // finds a possible references
            String valueOfSecondarRef = secondaryAttr.getNodeValue();

            if (isEmpty(valueOfSecondarRef)) {
                continue;
            }

            HashSet<String> list_componentIds =
                getAllComponentIds(valueOfSecondarRef, secondaryTag);
            PartialTriggers trigger = new PartialTriggers();
            trigger.setRawTriggerOn(valueOfSecondarRef);
            trigger.setTriggerName(secondaryTag);
            trigger.setTriggerOnSplitIds(list_componentIds);
            Integer lineNum = getLineNumberOfAttrNode(secondaryAttr);
            trigger.setLineNumber(lineNum);
            String serializedTag = getNodeAsString(componentNode, "");
            trigger.setSerializedTag(serializedTag);
            addToSecondaryRefByLineNum(secondaryRefs_byLineNumber, lineNum,
                                       trigger);

            Iterator<String> id_iter = list_componentIds.iterator();
            while (id_iter.hasNext()) {
                String obtId = id_iter.next();
                addToSecondaryRefByCompId(secondaryRefs_byCompId, obtId,
                                          trigger);
            }
        }
    }

    private void addToSecondaryRefByLineNum(HashMap<Integer, ArrayList<PartialTriggers>> secondaryRefMap,
                                            Integer lineNum,
                                            PartialTriggers trigger) {
        if (secondaryRefMap.containsKey(lineNum)) {
            ArrayList<PartialTriggers> list_triggers =
                secondaryRefMap.get(lineNum);
            list_triggers.add(trigger);
            secondaryRefMap.put(lineNum, list_triggers);
        } else {
            ArrayList<PartialTriggers> list_triggers =
                new ArrayList<PartialTriggers>(30);
            list_triggers.add(trigger);
            secondaryRefMap.put(lineNum, list_triggers);
        }
    }

    private void addToSecondaryRefByCompId(HashMap<String, ArrayList<PartialTriggers>> secondaryRefMap,
                                           String compId,
                                           PartialTriggers trigger) {
        if (secondaryRefMap.containsKey(compId)) {
            ArrayList<PartialTriggers> list_triggers =
                secondaryRefMap.get(compId);
            list_triggers.add(trigger);
            secondaryRefMap.put(compId, list_triggers);
        } else {
            ArrayList<PartialTriggers> list_triggers =
                new ArrayList<PartialTriggers>(30);
            list_triggers.add(trigger);
            secondaryRefMap.put(compId, list_triggers);
        }
    }

    /**
     *Possible formats for the secondary references.
     * Till now, i only know of partial triggers that can contain commponentIds.
     *
     *Case 1
     * - component delimiter space
     * partialTriggers = "ot1 ot2 ot3"
     *
     * - delimiter space, and comonentid seperator ':'
     * partialTriggers = "t1:ot1 ot2 ot3"
     *
     * - delimtier space
     * - seperator ::
     * - This probably means the parents child
     * partialTriggers = "::t1 ::ot1"
     *
     * - probably means roots child
     * partialTriggers="::::ot1"
     *
     * - application tables can also have editPartialTriggers, deletePartialTriggers
     * - the delimiters in this case are different
     * - in these cases the commponent delimiter is ',', and the entire trigger can be within '[]'
     * e.g editPartialTriggers = "[ot1, ::ot2]"
     * @param secondaryRefValue
     * Using a return value of HashSet<String> since I only want unique componentIds on the line
     */
    private HashSet<String> getAllComponentIds(String secondaryRefValue,
                                               String refContainer) {

        HashSet<String> list_of_component_ids = new HashSet<String>(30);

        if (isEmpty(secondaryRefValue)) {
            return list_of_component_ids;
        }
        refContainer = refContainer.trim();
        secondaryRefValue = secondaryRefValue.trim();
        if (refContainer.equals("partialTriggers")) {

            String[] list_seperated_components =
                secondaryRefValue.split(ComponentConstants.TRIGGER_COMP_SEPERATOR);
            for (int i = 0; i < list_seperated_components.length; i++) {
                String component = list_seperated_components[i];
                if (component.contains(ComponentConstants.TRIGGER_COMP_ID_SEPEARATOR)) {
                    String[] component_parts =
                        component.split(ComponentConstants.TRIGGER_COMP_ID_SEPEARATOR);
                    for (int j = 0; j < component_parts.length; j++) {
                        String componentId = component_parts[j];
                        if (!isEmpty(componentId)) {

                            list_of_component_ids.add(componentId.trim());
                        }
                    }
                } else {
                    // Case 1 - partialTriggers = "ot1 ot2 ot3"
                    if (!isEmpty(component)) {
                        list_of_component_ids.add(component.trim());
                    }
                }
            }

        } else {
            secondaryRefValue =
                    removeAnyStartAndEndBrackets(secondaryRefValue);
            String[] list_seperated_components = secondaryRefValue.split(",");
            for (int i = 0; i < list_seperated_components.length; i++) {
                String component = list_seperated_components[i];
                if (component.contains(ComponentConstants.TRIGGER_COMP_ID_SEPEARATOR)) {
                    String[] component_parts =
                        component.split(ComponentConstants.TRIGGER_COMP_ID_SEPEARATOR);
                    for (int j = 0; j < component_parts.length; j++) {
                        String componentId = component_parts[j];
                        if (!isEmpty(componentId)) {
                            list_of_component_ids.add(componentId.trim());
                        }
                    }
                } else {
                    // Case 1 - partialTriggers = "ot1 ot2 ot3"
                    if (!isEmpty(component)) {
                        list_of_component_ids.add(component.trim());
                    }
                }
            }

        }

        // System.out.println("Split Ids: " + list_of_component_ids);
        return list_of_component_ids;
    }

    private String removeAnyStartAndEndBrackets(String str) {
        while (str.trim().startsWith("[")) {
            str = str.substring(1);
        }
        while (str.trim().endsWith("]")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }


    /**
     * Gets the line number of the attribute node in the corresponding source file
     * @param attrNode
     * Attribute node whose line number is to be found
     * @return
     * Line number in the source file
     */
    public Integer getLineNumberOfAttrNode(Node attrNode) {
        XMLAttr attr = null;
        if (attrNode instanceof XMLAttr) {
            attr = (XMLAttr)attrNode;
        }


        if (attr != null) {
            return attr.getLineNumber();
        }
        return 0;
    }

    public void addToViolatingComponentMap(Integer lineNumber,
                                           ComponentDetails component) {
        if (violatingComponents.containsKey(lineNumber)) {
            System.out.println("Error.. there is already a component at this line number.. Invalid file formatting.. line can contain only one component");
        } else {
            violatingComponents.put(lineNumber, component);
        }
    }


    /**
     * Gets the product name from the file path
     *
     * @param uiFilePath
     * @return
     */
    public String getProductName(String uiFilePath) {
        String productName = "";
        Integer productIndex = 5;
        // file path: /ade/<view_name>/fusionapps/fin/components/ledger/gl
        if (uiFilePath.contains("/fusionapps/")) {
            Integer fusionappsIndex = uiFilePath.indexOf("/fusionapps/");
            String path_after_fusionapps =
                uiFilePath.substring(fusionappsIndex);
            if (!isEmpty(path_after_fusionapps)) {
                String[] parts = path_after_fusionapps.split("/");
                if (parts != null & parts.length > productIndex) {
                    return parts[productIndex];
                }
            }
        }
        return "";
    }

    public ArrayList<ComponentDetails> getAllViolatingComponents() {
        ArrayList<ComponentDetails> list_all_components =
            new ArrayList<ComponentDetails>();

        Set<Integer> keys = violatingComponents.keySet();
        Iterator<Integer> keyIter = keys.iterator();
        while (keyIter.hasNext()) {
            Integer fileNum = keyIter.next();

            ComponentDetails component = violatingComponents.get(fileNum);
            list_all_components.add(component);

        }
        return list_all_components;
    }

    /**
     * Initializes the Dom parser to parse jspx and jsff files
     * @return
     * Dom parser
     * @throws Exception
     */
    private DOMParser initializeDomParser() throws Exception {

        DOMParser parser = new DOMParser();
        parser.setDebugMode(true);
        parser.setErrorStream(System.out);
        parser.setEntityResolver(new NullEntityResolver());
        parser.showWarnings(true);
        parser.setValidationMode(DOMParser.NONVALIDATING);

        return parser;
    }

    /**
     * Initialize the list of naming containers. Only these components are to be looked for as violations
     */
    public void intializeListOfNamingContainers() {
        namingContainers.add("af:region");
        namingContainers.add("af:declarativeComponent");
        namingContainers.add("af:navigationPane");
        namingContainers.add("af:pageTemplate");
        namingContainers.add("af:panelCollection");
        namingContainers.add("af:pageTemplate");
        namingContainers.add("af:query");
        namingContainers.add("af:quickQuery");
        namingContainers.add("af:subform");
        namingContainers.add("af:table");
        namingContainers.add("af:train");
        namingContainers.add("af:tree");
        namingContainers.add("af:treeTable");
        namingContainers.add("af:trainbuttonBar");
        namingContainers.add("af:carousel");
        namingContainers.add("af:calendar");
        namingContainers.add("af:breadCrumbs");
        namingContainers.add("f:form");
        namingContainers.add("h:dataTable");
        namingContainers.add("af:resetActionListener");
        namingContainers.add("tr:breadCrumbs");
        namingContainers.add("tr:navigationPane");
        namingContainers.add("tr:navigationTree");
        namingContainers.add("tr:page");
        namingContainers.add("tr:subform");
        namingContainers.add("tr:table");
        namingContainers.add("tr:train");
        namingContainers.add("tr:tree");
        namingContainers.add("tr:treeTable");
    }


    public void initializeSecondaryCompIdRef() {
        secondaryComponentContainers.add("partialTriggers");
        secondaryComponentContainers.add("deletePartialTriggers");
        secondaryComponentContainers.add("createPartialTriggers");
        secondaryComponentContainers.add("duplicatePartialTriggers");
        secondaryComponentContainers.add("editPartialTriggers");
        secondaryComponentContainers.add("tableId");
        secondaryComponentContainers.add("resultComponentId");
        secondaryComponentContainers.add("exportedId");
        secondaryComponentContainers.add("treeTableId");
    }

    public boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public int getNcViolationsFixed() {
        return ncViolationsFixed;
    }

    public int getNcViolationsNotFixed() {
        return ncViolationsNotFixed;
    }

    public HashMap<String, ArrayList<PartialTriggers>> getSecondaryRefs_byCompId() {
        return secondaryRefs_byCompId;
    }

    public int getSecondaryRefsFixed() {
        return secondaryRefsFixed;
    }

    public int getSecondaryRefsNotFixed() {
        return secondaryRefsNotFixed;
    }

    public int getNumberOfSecondaryRefs() {
        return numberOfSecondaryRefs;
    }

    public HashMap<String, String> getChangedIds() {
        return changedIds;
    }
}
