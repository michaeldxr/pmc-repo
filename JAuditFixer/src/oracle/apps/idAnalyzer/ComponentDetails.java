package oracle.apps.idAnalyzer;

/**
 * Component Details.
 * Details of an adf component found in a UI file
 * contains the following
 * 1. Name of component. e.g. af:outputText
 * 2. fileName: Name of the file it is present in. e.g. ProtectedWebPage.jspx
 * 3. FilePath: Absolute path of the file
 * 4. product: The product name it is found in. e.g. gl, ap, ar
 * 5. source line number: the line number in the file that it is found in
 */
public class ComponentDetails {

    /**
     * name of component
     * e.g. af:outputText
     */
    private String componentName = "";

    /**
     * file name e.g.ProtectedPage.jspx
     */
    private String fileName = "";

    /**
     * File absolute pate
     */
    private String filePath = "";

    /**
     * Product Short Name
     */
    private String product = "";

    /**
     * Line number in the source file where this component tag occurs
     */
    private Integer sourceLineNumber = 0;

    /**
     * Length of the Id
     */
    private Integer idLength = 0;

    /**
     * Component Id
     */
    private String componentId = "";

    /**
     * Source tag of the component
     */
    private String componentSourceTag = "";

    /**
     * Suggested id if there is a violation
     */
    private String suggestedId="";
    
    /**
     * Soruce of the tag serialized as a string
     */
    private String serializedTag="";
    
    public ComponentDetails() {
        super();
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getProduct() {
        return product;
    }

    public void setSourceLineNumber(Integer sourceFileNumber) {
        this.sourceLineNumber = sourceFileNumber;
    }

    public Integer getSourceLineNumber() {
        return sourceLineNumber;
    }

    public void setIdLength(Integer idLength) {
        this.idLength = idLength;
    }

    public Integer getIdLength() {
        return idLength;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentSourceTag(String componentSourceTag) {
        this.componentSourceTag = componentSourceTag;
    }

    public String getComponentSourceTag() {
        return componentSourceTag;
    }

    public void setSuggestedId(String suggestedId) {
        this.suggestedId = suggestedId;
    }

    public String getSuggestedId() {
        return suggestedId;
    }

    public void setSerializedTag(String serializedTag) {
        this.serializedTag = serializedTag;
    }

    public String getSerializedTag() {
        return serializedTag;
    }
}