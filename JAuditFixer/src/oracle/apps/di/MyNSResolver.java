package oracle.apps.di;

import oracle.xml.parser.v2.NSResolver;

public class MyNSResolver implements NSResolver {
    public MyNSResolver() {
        super();
    }


    public String resolveNamespacePrefix(String prefix) {
        if (prefix.equals("wbk") || prefix.equals("w"))
            return "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
        if (prefix.equals("am"))
            return "http://xmlns.oracle.com/bc4j";
        else if (prefix.equals("rel") || prefix.equals("r"))
            return "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
        else if (prefix.equals("jpr"))
            return "http://xmlns.oracle.com/ide/project";
        return "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    }


}
