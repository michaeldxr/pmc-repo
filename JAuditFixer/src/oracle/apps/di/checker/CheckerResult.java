package oracle.apps.di.checker;

public class CheckerResult {
    String path = "";
    String checkerCode = "";
    boolean checkerPassed = false;
    String description = "";
    String subIssue = "";
    String issue = "";

    public CheckerResult() {
        super();
    }

    public CheckerResult(String code, String issue, String subIssue, boolean status) {
        super();
        this.checkerCode = code;
        this.issue = issue;
        this.checkerPassed = status;
        this.subIssue = subIssue;

    }

    public CheckerResult(String code, String issue, String subIssue, String description, boolean status) {
        super();
        this.checkerCode = code;
        this.issue = issue;
        this.checkerPassed = status;
        this.subIssue = subIssue;
        this.description = description;

    }

    public void setCheckerCode(String checkerCode) {
        this.checkerCode = checkerCode;
    }

    public String getCheckerCode() {
        return checkerCode;
    }

    public void setCheckerPassed(boolean checkerPassed) {
        this.checkerPassed = checkerPassed;
    }

    public boolean isCheckerPassed() {
        return checkerPassed;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }


    public void print() {
        String statStr = "false";
        if (isCheckerPassed())
            statStr = "true";
        if (!isCheckerPassed()) {
            System.out.println("[FAILED] " + getIssue() + " - " + getSubIssue());
            String desc = getDescription();
            if (desc != null && !desc.trim().equals(""))
                System.out.println("  ->[REASON] " + desc);
        } else {
            System.out.println("[PASSED] " + getCheckerCode() + " - " + getIssue());
        }
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setSubIssue(String subIssue) {
        this.subIssue = subIssue;
    }

    public String getSubIssue() {
        return subIssue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getIssue() {
        return issue;
    }
}
