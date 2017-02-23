package oracle.apps.stringscan;
public class ViolationDetails {
    private String fileName;
    private String series;
    private String label;
    private String family;
    private String module;
    private String product;
    private String issueType;
    private String subIssue;
    private String description;


    public ViolationDetails(String fileName, String series, String label, String family,
                            String module, String product,
                            String issueType, String subIssue,
                            String description) {
        this.fileName = fileName;
        this.series = series;
        this.label = label;
        this.family = family;
        this.module = module;
        this.product = product;
        this.issueType = issueType;
        this.subIssue = subIssue;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getFamily() {
        return family;
    }

    public String getModule() {
        return module;
    }

    public String getProduct() {
        return product;
    }

    public String getIssueType() {
        return issueType;
    }

    public String getSubIssue() {
        return subIssue;
    }

    public String getDescription() {
        return description;
    }

    public void setSubIssue(String sub_issue) {
        this.subIssue = sub_issue;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFileName() {
        return fileName;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getSeries() {
        return series;
    }
}
