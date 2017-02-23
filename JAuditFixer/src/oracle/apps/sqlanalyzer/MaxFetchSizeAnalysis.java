package oracle.apps.sqlanalyzer;

import java.io.File;

import oracle.apps.utility.JoesBaseClass;

public class MaxFetchSizeAnalysis extends JoesBaseClass{
    
    public MaxFetchSizeAnalysis() 
    {
        super(JoesBaseClass.CRAWL_TYPE.JSPX_AND_JSFF_AND_PAGE_DEF_AND_CPX);
    }
    
    public static void main(String[] args) {
        
    }

    protected void processFile(File fName, boolean bDoADE) {
    }

    protected String getSummaryReport() {
        return null;
    }

    protected String getSummaryReportSubject() {
        return null;
    }
}
