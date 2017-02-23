package oracle.apps.idAnalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import oracle.apps.NullEntityResolver;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NoIdFileProcessor {
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
    HashSet<String> compNotSupportingIds = new HashSet<String>(100);

    /**
     * Number of violations fixed
     */
    int violationsFixed = 0;

    /**
     * Number of violations not fixed due to formatting
     */
    int violationsNotFixed_formatting = 0;

    /**
     * key - line number iin the source file where the component is found
     * Value - component details of the violating component
     * We need to store the line number as the key because it will help when we are trying to fix all the files. We can only read the file serially and hence we will have
     * to iterate over all the lines in the files to get to the violating line number. At that point
     * we can just do a contains chceck to find if the line number in the file is to be fixed.
     * Assumption - a particular line will contain only one violating component. Keeping it simple by not using array list within the hash map
     */
    HashMap<Integer, ComponentDetails> componentsWithoutIds =
        new HashMap<Integer, ComponentDetails>(100);

/**
 * List of lines not fixed due to formatting
 */
ArrayList<String> linesNotFixedDueToFormatting = new ArrayList<String>();

    public NoIdFileProcessor() {
        super();
        initializeComponentsNotSupportingIds();
    }




    public static class ComponentConstants {
        public static String ID_ATTR = "id";
        public static String FACES_COMP_PREFIX = "af:";
    }

    /**
     * Initialize the list of exclusion components. These are the af components that do not support ids.
     */
    public void initializeComponentsNotSupportingIds() {
        compNotSupportingIds.add("af:attributeDragSource");
        compNotSupportingIds.add("af:attributeDropTarget");
        compNotSupportingIds.add("af:autoSuggestBehavior");
        compNotSupportingIds.add("af:calendarDropTarget");
        compNotSupportingIds.add("af:clientAttribute");
        compNotSupportingIds.add("af:clientListener");
        compNotSupportingIds.add("af:collectionDropTarget");
        compNotSupportingIds.add("af:componentDragSource");
        compNotSupportingIds.add("af:convertColor");
        compNotSupportingIds.add("af:convertDateTime");
        compNotSupportingIds.add("af:convertNumber");
        compNotSupportingIds.add("af:dataFlavor");
        compNotSupportingIds.add("af:dropTarget");
        compNotSupportingIds.add("af:dragSource");
        compNotSupportingIds.add("af:exportCollectionActionListener");
        compNotSupportingIds.add("af:fileDownloadActionListener");
        compNotSupportingIds.add("af:forEach");
        compNotSupportingIds.add("af:insertTextBehavior");
        compNotSupportingIds.add("af:panelDashboardBehavior");
        compNotSupportingIds.add("af:resetActionListener");
        compNotSupportingIds.add("af:resource");
        compNotSupportingIds.add("af:returnActionListener");
        compNotSupportingIds.add("af:richTextEditorInsertBehavior");
        compNotSupportingIds.add("af:scrollComponentIntoViewBehavior");
        compNotSupportingIds.add("af:serverListener");
        compNotSupportingIds.add("af:setPropertyListener");
        compNotSupportingIds.add("af:setActionListener");
        compNotSupportingIds.add("af:showPopupBehavior");
        compNotSupportingIds.add("af:showPrintablePageBehavior");
        compNotSupportingIds.add("af:skipLinkTarget");
        compNotSupportingIds.add("af:switcher");
        compNotSupportingIds.add("af:validatebyteLength");
        compNotSupportingIds.add("af:validateDateRestriction");
        compNotSupportingIds.add("af:validateDateTimeRange");
        compNotSupportingIds.add("af:validateDoubleRange");
        compNotSupportingIds.add("af:validateLength");
        compNotSupportingIds.add("af:validateLongRange");
        compNotSupportingIds.add("af:validateRegExp");
        compNotSupportingIds.add("af:componentDef");
        compNotSupportingIds.add("af:xmlContent");
        compNotSupportingIds.add("af:collectionDragSource");
    }


    /**
     * Processes the ui file for noComponentIds case.
     * @param file
     * @param bDoADE
     * @return
     */
    public String processUiFileForNoIds(File file, boolean bDoADE) {

        DOMParser parser = null;
        String absFilePath = file.getAbsolutePath();
        if (!bDoADE) {
            System.out.println("Processing file: " + absFilePath);
        }

        try {
            parser = initializeDomParser();
            parser.parse("file:" + absFilePath);
        } catch (Exception ex) {
            System.out.println("Error while initializing dom parser for file: " +
                               absFilePath);
            ex.printStackTrace();
            System.out.println("Skipping this file.. ");
            return null;
        }

        XMLDocument doc = parser.getDocument();
        Element rootNode = doc.getDocumentElement();
        if (rootNode.hasChildNodes()) {
            traverseNodes(rootNode.getChildNodes(), file);
        }

        if (componentsWithoutIds.size() > 0) {
            // this file needs fixing
            try {
                return getFixedFileContents(file, componentsWithoutIds,
                                            seenComponentIds, bDoADE);
            } catch (Exception ex) {
                System.out.println("Error while fixing file: " +
                                   file.getPath());
                ex.printStackTrace();
                return null;
            }

        }
        return null;

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
    public void traverseNodes(NodeList childNodes, File uiFile) {

        if (childNodes == null) {
            return;
        }
        String uiFilePath = uiFile.getPath();

        int childNodeLength = childNodes.getLength();
        for (int i = 0; i < childNodeLength; i++) {
            Node child = childNodes.item(i);
            String componentId = getNodeId(child);
            if (isEmpty(componentId)) {
                String componentName = child.getNodeName();
                if (isADFFacesCompSupportingId(componentName)) {
                    ComponentDetails component = new ComponentDetails();
                    component.setComponentName(componentName);
                    component.setFileName(uiFile.getName());
                    component.setFilePath(uiFilePath);
                    String productName = getProductName(uiFilePath);
                    component.setProduct(productName);
                    Integer lineNumber = getLineNumberInSourceFile(child);
                    component.setSourceLineNumber(lineNumber);
                    addToViolatingComponentMap(lineNumber, component);
                }

            } else {
                // this is just to check that there are no duplicates
                if (seenComponentIds.contains(componentId)) {
                    System.out.println("\n [FYI].. found a duplicate id : " +
                                       componentId + " while scanning file: " +
                                       uiFile.getAbsolutePath());
                }
                seenComponentIds.add(componentId);
            }
            if (child.hasChildNodes()) {
                traverseNodes(child.getChildNodes(), uiFile);
            }
        }
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
    private String getFixedFileContents(File f,
                                        HashMap<Integer, ComponentDetails> lineNumberWithComponents,
                                        HashSet<String> seenIds,
                                        boolean bDoAde) throws Exception {

        File fp = new File(f.getAbsolutePath());
        FileReader reader = new FileReader(fp);
        BufferedReader input = new BufferedReader(reader);

        FileReader reader2 = new FileReader(fp);
        BufferedReader input2 = new BufferedReader(reader2);


        String line;
        StringBuffer sb = new StringBuffer();

        int lineNo = 1;
        input.mark(1024);
        try {
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


                if (lineNumberWithComponents.containsKey(lineNo)) {
                    ComponentDetails component =
                        lineNumberWithComponents.get(lineNo);
                    line = fixLine(line, newline, component, seenIds, bDoAde);
                    sb.append(line + newline);
                    lineNumberWithComponents.put(lineNo, component);
                } else {
                    sb.append(line);
                    sb.append(newline);
                }

                lineNo++;
            }
        } catch (Exception ex) {
            System.out.println("Exception while fixing file.. " +
                               f.getAbsolutePath());
            throw new Exception(ex);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return sb.toString();

    }

    /**
     * fixes the violating line by adding in the component id for a component which has no ids
     * @param line
     * line to be fixed
     * @param newline
     * newline character
     * @param component
     * component details of the violating component
     * @param seenIds
     * map of all the seen ids to make sure a duplicate component id is not generated
     * @param doAde
     * whether system out statement required
     * @return
     * Returns the fixed line
     */
    private String fixLine(String line, String newline,
                           ComponentDetails component, HashSet<String> seenIds,
                           boolean doAde) {
        /* First do some sanity tests to make sure we are dealing with good input.
         * Good input Definition: < should not appear more than once, also > should not appear more than once
         * [Assumtpion] - one line contains only one component.
         * e.g. <af:outputText /> <af:outputText /> not allowed
         * also /> <af:outputText /> not allowed
         * we get the index of the '>'. Hence we should not have more than one occurrence of > on a line
         * more than one occurrence of < should be allowed.. but for now , not allowing even this
         * TODO: Remove the check for two opening tags. Should not matter e.g.
         * <af:component > <af:component - should be allowed. does not matter for us
         */


        if (appearsMoreThanOnce(line, "<") || appearsMoreThanOnce(line, ">")) {
            System.out.println("ERROR:  skipping this line whose formatting I cannot understand");
            System.out.println(line);
            linesNotFixedDueToFormatting.add(line);
            violationsNotFixed_formatting++;
            return line;
        }

        String componentName = component.getComponentName();
        String generatedId = generateComponentId(seenIds, componentName);
        seenIds.add(generatedId);
        if (isEmpty(generatedId)) {
            return line;
        }

        String retVal = null;

        /*component can either be <af:component > or <af:component /> */
        int indexOfEndTag = line.indexOf("/>");
        if (indexOfEndTag == -1) {
            indexOfEndTag = line.indexOf(">");
        }
        // not found - return as is
        if (indexOfEndTag == -1) {
            return line;
        }
        retVal = line.substring(0, indexOfEndTag);
        String endingPortionOfLine = line.substring(indexOfEndTag);
        if (!retVal.endsWith(" ")) {
            retVal = retVal + " ";
        }
        retVal =
                retVal + ComponentConstants.ID_ATTR + "=\"" + generatedId + "\" ";
        retVal = retVal + endingPortionOfLine;
        if (!doAde) {
            System.out.println("old line: " + line);
            System.out.println("new line: " + retVal + "\n");
        }
        violationsFixed++;
        return retVal;
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

    private boolean isADFFacesCompSupportingId(String componentName) {
        if (isEmpty(componentName)) {
            return false;
        }
        componentName = componentName.trim();
        if (componentName.startsWith(ComponentConstants.FACES_COMP_PREFIX)) {

            if (!compNotSupportingIds.contains(componentName)) {
                return true;
            }
        }
        return false;
    }

    /**
     *getLineNumber on a tag returns the linenumber of the end of the start tag of the component
     * e.g. <af:outputText />, in this case will return the line number of this tag
     * e.g. <af:panelGroupLayout
     *          name="this person"
     *          partialTrigger="something" >
     *          in this case it will return the thrid line as the line number
     *Not sure, but ideally we may want to put the id just after the componentName .
     * TODO:To achieve this we can check if the comonent has attributes. If it has attributes - return line of the first attribute
     * else jus return line number
     * @param componentNode
     * @return
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
     * Gets the component Id from the XML Node
     * @param childNode
     * Child Node
     * @return
     * ID of the Node
     */
    private String getNodeId(Node childNode) {
        childNode.getNodeName();
        NamedNodeMap attributes = childNode.getAttributes();
        if (attributes != null && attributes.getLength() > 0) {
            Node idNode = attributes.getNamedItem(ComponentConstants.ID_ATTR);
            if (idNode != null) {
                return idNode.getNodeValue();
            }
        }
        return "";
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

    /**
     * Required to check for ui file formatting
     * @param str
     * @param pattern
     * @return
     */
    private boolean appearsMoreThanOnce(String str, String pattern) {
        int index = str.indexOf(pattern);
        if (index == -1)
            return false;

        String str2 = str.substring(index + pattern.length());
        index = str2.indexOf(pattern);
        if (index == -1)
            return false;

        return true;
    }

    private DOMParser initializeDomParser() throws Exception {

        DOMParser parser = new DOMParser();
        parser.setDebugMode(true);
        parser.setErrorStream(System.out);
        parser.setEntityResolver(new NullEntityResolver());
        parser.showWarnings(true);
        parser.setValidationMode(DOMParser.NONVALIDATING);

        return parser;
    }

    private boolean isEmpty(String str) {
        if (str == null || str.trim().equals("")) {
            return true;
        }
        return false;
    }

    public void addToViolatingComponentMap(Integer lineNumber,
                                           ComponentDetails component) {
        if (componentsWithoutIds.containsKey(lineNumber)) {
            System.out.println("Error.. there is already a component at this line number.. Invalid file formatting.. line can contain only one component");
        } else {
            componentsWithoutIds.put(lineNumber, component);
        }
    }

    public ArrayList<ComponentDetails> getAllViolatingComponents(HashMap<Integer, ComponentDetails> resultMap) {
        ArrayList<ComponentDetails> list_all_components =
            new ArrayList<ComponentDetails>();
        Set<Integer> keys = resultMap.keySet();
        Iterator<Integer> keyIter = keys.iterator();
        while (keyIter.hasNext()) {
            Integer fileNum = keyIter.next();

            ComponentDetails component = resultMap.get(fileNum);
            list_all_components.add(component);

        }
        return list_all_components;
    }

    public HashMap<Integer, ComponentDetails> getComponentsWithoutIds() {
        return componentsWithoutIds;
    }

    public int getViolationsFixed() {
        return violationsFixed;
    }

    public int getViolationsNotFixed_formatting() {
        return violationsNotFixed_formatting;
    }
    
    public ArrayList<String> getLinesNotFixedDueToFormatting() {
        return linesNotFixedDueToFormatting;
    }
}
