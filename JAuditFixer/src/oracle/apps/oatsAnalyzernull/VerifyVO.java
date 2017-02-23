package oracle.apps.oatsAnalyzernull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

class ViewCriteria {
    HashMap<String, String> viewCriteriaAttributes =
        new HashMap<String, String>();
    ViewCriteriaRow ViewCriteriaRows = new ViewCriteriaRow();

    void printViewCriteria() {
        HashMap<String, String> viewCriteriaAttributesTmp =
            this.viewCriteriaAttributes;
        for (Entry<String, String> entry1 :
             viewCriteriaAttributesTmp.entrySet()) {
            System.out.println(entry1.getKey() + " -#- " + entry1.getValue());
        }
        ViewCriteriaRow ViewCriteriaRowsTmp = this.ViewCriteriaRows;
        ViewCriteriaRowsTmp.printViewCriteriaRow();
    }


    boolean matchViewCriteria(ViewCriteria matchObj) {
        if (this.viewCriteriaAttributes.equals(matchObj.viewCriteriaAttributes)) {
            if (this.ViewCriteriaRows.matchViewCriteriaRow(matchObj.ViewCriteriaRows)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

}

class ViewCriteriaRow {
    HashMap<String, String> attributes = new HashMap<String, String>();
    ArrayList<ViewCriteriaItems> ViewCriteriaItem =
        new ArrayList<ViewCriteriaItems>();

    void printViewCriteriaRow() {
        HashMap<String, String> attributesTmp = this.attributes;
        for (Entry<String, String> entry1 : attributesTmp.entrySet()) {
            System.out.println(entry1.getKey() + " --- " + entry1.getValue());
        }
        ArrayList<ViewCriteriaItems> ViewCriteriaItemTmp =
            this.ViewCriteriaItem;
        for (int i1 = 0; i1 < ViewCriteriaItemTmp.size(); i1++) {
            ViewCriteriaItems ViewCriteriaItemsTmp =
                ViewCriteriaItemTmp.get(i1);
            ViewCriteriaItemsTmp.printViewCriteriaItems();
        }
    }

    public boolean matchViewCriteriaRow(ViewCriteriaRow viewCriteriaRows) {

        if (this.attributes.equals(viewCriteriaRows.attributes)) {
            if (this.ViewCriteriaItem.size() ==
                viewCriteriaRows.ViewCriteriaItem.size()) {
                for (int i = 0; i < this.ViewCriteriaItem.size(); i++) {
                    ViewCriteriaItems obj1 = this.ViewCriteriaItem.get(i);
                    ViewCriteriaItems obj2 =
                        viewCriteriaRows.ViewCriteriaItem.get(i);
                    if (!obj1.matchViewCriteriaItems(obj2)) {
                        return false;
                    }
                }
            } else {
                return false;
            }
            return true;
        } else {
            return false;
        }
    }
}

class ViewCriteriaItems {
    HashMap<String, String> items = new HashMap<String, String>();

    void printViewCriteriaItems() {
        HashMap<String, String> itemsTmp = this.items;
        for (Entry<String, String> entry1 : itemsTmp.entrySet()) {
            System.out.println(entry1.getKey() + " --- " + entry1.getValue());
        }
    }

    boolean matchViewCriteriaItems(ViewCriteriaItems obj) {
        if (this.items.equals(obj.items)) {
            return true;
        } else {
            return false;
        }
    }
}

public class VerifyVO {

    private static HashMap<String, String> labelVOMap =
        new HashMap<String, String>();
    private static HashMap<String, String> viewVOMap =
        new HashMap<String, String>();
    private static HashMap<String, ArrayList<String>> partialMap =
        new HashMap<String, ArrayList<String>>();
    private static String tmpbuf;

    public static Collection<File> getFileList(String dirPath,
                                               String[] extensions,
                                               Boolean recursive) {
        return FileUtils.listFiles(new File(dirPath), extensions, recursive);
    }

    static boolean stackMatch(String one,
                              HashMap<String, String> mismatchTags) {
        int matchCount = 0;
        int size = 0;
        int start = 0;
        int end = 0;
        String buf;
        for (Entry<String, String> entry : mismatchTags.entrySet()) {
            String two = entry.getKey();

            try {
                if (one.substring(one.lastIndexOf(':')).equalsIgnoreCase(two.substring(two.lastIndexOf(':')))) {
                    ArrayList<String> partialList =
                        partialMap.get(entry.getKey());
                    size = partialList.size();
                    start = 0;
                    end = one.indexOf(':');
                    buf = null;
                    while (end != -1) {
                        buf = one.substring(start, end);
                        start = end + 1;
                        end = one.indexOf(':', start);
                        if (partialList.contains(buf)) {
                            matchCount++;
                        }
                    }
                    buf = one.substring(start);
                    if (partialList.contains(buf)) {
                        matchCount++;
                    }
                    if (matchCount == size) {
                        tmpbuf = entry.getKey();
                        return true;
                    }
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
        return false;
    }

    static void createpartialMap(HashMap<String, String> mismatchTags) {
        for (Entry<String, String> entry : mismatchTags.entrySet()) {
            String two = entry.getKey();
            int start = 0;
            int end = two.indexOf(':');
            String buf;
            while (end != -1) {
                buf = two.substring(start, end);
                start = end + 1;
                end = two.indexOf(':', start);
                if (!partialMap.containsKey(entry.getKey())) {
                    ArrayList<String> tmp = new ArrayList<String>();
                    tmp.add(buf);
                    partialMap.put(entry.getKey(), tmp);
                } else {
                    ArrayList<String> tmp = partialMap.get(entry.getKey());
                    tmp.add(buf);
                    partialMap.put(entry.getKey(), tmp);
                }
            }
            buf = two.substring(start);
            if (!partialMap.containsKey(entry.getKey())) {
                ArrayList<String> tmp = new ArrayList<String>();
                tmp.add(buf);
                partialMap.put(entry.getKey(), tmp);
            } else {
                ArrayList<String> tmp = partialMap.get(entry.getKey());
                tmp.add(buf);
                partialMap.put(entry.getKey(), tmp);
            }
        }
    }

    private static HashMap<String, ArrayList<ViewCriteria>> extractViewCriteria(HashMap<String, String> vOMap) {
        HashMap<String, ArrayList<ViewCriteria>> criteriaItemsList =
            new HashMap<String, ArrayList<ViewCriteria>>();
        for (Entry<String, String> entry : vOMap.entrySet()) {
            final ArrayList<ViewCriteria> criteriaItems =
                new ArrayList<ViewCriteria>();
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                                   false);
                SAXParser saxParser = factory.newSAXParser();
                DefaultHandler handler = new DefaultHandler() {
                    boolean inCriteria = false;
                    boolean inCriteriaRow = false;
                    boolean inCriteriaItem = false;
                    ViewCriteria mainObj;

                    public void startElement(String uri, String localName,
                                             String qName,
                                             Attributes attributes) throws SAXException {
                        if (qName.equalsIgnoreCase("ViewCriteria") &&
                            !inCriteria) {
                            inCriteria = true;
                            attributes.getValue("Name");
                            mainObj = new ViewCriteria();
                            int attrCount = attributes.getLength();
                            for (int i = 0; i < attrCount; i++) {
                                mainObj.viewCriteriaAttributes.put(attributes.getQName(i),
                                                                   attributes.getValue(i));
                            }
                        }
                        if (qName.equalsIgnoreCase("ViewCriteriaRow")) {
                            inCriteriaRow = true;
                            int attrCount = attributes.getLength();
                            for (int i = 0; i < attrCount; i++) {
                                mainObj.ViewCriteriaRows.attributes.put(attributes.getQName(i),
                                                                        attributes.getValue(i));
                            }
                        }
                        if (qName.equalsIgnoreCase("ViewCriteriaItem")) {
                            inCriteriaItem = true;
                        }
                        if (inCriteria && inCriteriaRow && inCriteriaItem) {
                            ViewCriteriaItems vObj = new ViewCriteriaItems();
                            int attrCount = attributes.getLength();
                            for (int i = 0; i < attrCount; i++) {
                                vObj.items.put(attributes.getQName(i),
                                               attributes.getValue(i));
                            }
                            mainObj.ViewCriteriaRows.ViewCriteriaItem.add(vObj);
                        }
                    }

                    public void endElement(String uri, String localName,
                                           String qName) throws SAXException {
                        if (qName.equalsIgnoreCase("ViewCriteria")) {
                            criteriaItems.add(mainObj);
                        }
                    }

                    public void characters(char[] ch, int start,
                                           int length) throws SAXException {

                    }
                };
                saxParser.parse(entry.getValue(), handler);
            } catch (ParserConfigurationException e) {
                //e.printStackTrace();
            } catch (SAXException e) {
                //e.printStackTrace();
            } catch (IOException e) {
                //e.printStackTrace();
            }
            for (int i = 0; i < criteriaItems.size(); i++) {
                criteriaItems.get(i);
            }
            criteriaItemsList.put(entry.getValue().substring(entry.getValue().lastIndexOf(File.separator) +
                                                             1),
                                  criteriaItems);
        }
        return criteriaItemsList;
    }

    public static ArrayList<String> FetchConflictingVO(String labelVerSource,
                                                       String viewVerSource) {
        Collection<File> labelVerFiles =
            getFileList(labelVerSource, new String[] { "xml" }, true);
        Collection<File> viewVerFiles =
            getFileList(viewVerSource, new String[] { "xml" }, true);
        ArrayList<String> criteriaErrorList = new ArrayList<String>();
        Iterator<File> labelVerItr = labelVerFiles.iterator();
        Iterator<File> viewVerItr = viewVerFiles.iterator();
        while (labelVerItr.hasNext()) {
            String fname = labelVerItr.next().getName();
            if (fname.endsWith("VO.xml")) {
            } else {
                labelVerItr.remove();
            }
        }

        while (viewVerItr.hasNext()) {
            String fname = viewVerItr.next().getName();
            if (fname.endsWith("VO.xml")) {
            } else {
                viewVerItr.remove();
            }
        }

        labelVerItr = labelVerFiles.iterator();
        viewVerItr = viewVerFiles.iterator();
        while (labelVerItr.hasNext()) {
            File tmpFile = labelVerItr.next();
            String strTmp = tmpFile.getName();
            String voName =
                strTmp.substring(strTmp.lastIndexOf(File.separator) + 1);
            labelVOMap.put(voName, tmpFile.getAbsolutePath());
        }
        while (viewVerItr.hasNext()) {
            File tmpFile = viewVerItr.next();
            String strTmp = tmpFile.getName();
            String voName =
                strTmp.substring(strTmp.lastIndexOf(File.separator) + 1);
            viewVOMap.put(voName, tmpFile.getAbsolutePath());
        }
        HashMap<String, ArrayList<ViewCriteria>> labelVerCriteriaMap =
            extractViewCriteria(labelVOMap);
        HashMap<String, ArrayList<ViewCriteria>> viewVerCriteriaMap =
            extractViewCriteria(viewVOMap);
        for (Entry<String, ArrayList<ViewCriteria>> entry1 :
             labelVerCriteriaMap.entrySet()) {
            if (viewVerCriteriaMap.containsKey(entry1.getKey())) {
                ArrayList<ViewCriteria> labelVerCriteriaArrayList =
                    entry1.getValue();
                ArrayList<ViewCriteria> viewVerCriteriaArrayList =
                    viewVerCriteriaMap.get(entry1.getKey());

                if (labelVerCriteriaArrayList.size() !=
                    viewVerCriteriaArrayList.size()) {
                    System.out.println("RAISE ERROR FROM SIZE");
                } else {
                    int flag = 0;
                    for (int j = 0; j < labelVerCriteriaArrayList.size();
                         j++) {
                        ViewCriteria matchObj1 =
                            labelVerCriteriaArrayList.get(j);
                        ViewCriteria matchObj2 =
                            viewVerCriteriaArrayList.get(j);
                        if (!matchObj1.matchViewCriteria(matchObj2)) {
                            flag++;
                            if (!criteriaErrorList.contains(matchObj1.viewCriteriaAttributes.get("Name")))
                                criteriaErrorList.add(matchObj1.viewCriteriaAttributes.get("Name"));
                            if (flag == 1) {
                                System.out.println("Source VO File: " +
                                                   entry1.getKey() +
                                                   "\nView Criteria Changed:" +
                                                   matchObj1.viewCriteriaAttributes.get("Name"));
                            }
                        }
                    }
                }
            } else {
                System.out.println("RAISE ERROR");
            }
        }
        labelVOMap.clear();
        viewVOMap.clear();
        return criteriaErrorList;
    }

    public static String generateUIFilePath(String pageDefFilePath,
                                            String uiFileName) {
        pageDefFilePath = pageDefFilePath.replace("adfmsrc", "public_html");
        pageDefFilePath =
                (String)pageDefFilePath.subSequence(0, pageDefFilePath.lastIndexOf(File.separator) +
                                                    1);
        return (pageDefFilePath + uiFileName);
    }

    private static ArrayList<String> parseForQueryStack(File outFile,
                                                        final String Criteria) {
        final ArrayList<String> criteriaMatchId = new ArrayList<String>();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {
                Boolean ID = false;

                public void startElement(String uri, String localName,
                                         String qName,
                                         Attributes attributes) throws SAXException {
                    if (qName.equalsIgnoreCase("id")) {
                        if (attributes.getValue("query") != null &&
                            attributes.getValue("query").contains(Criteria)) {
                            ID = true;
                        }
                    }
                }

                public void endElement(String uri, String localName,
                                       String qName) throws SAXException {
                    if (qName.equalsIgnoreCase("id")) {
                        ID = false;
                    }
                }

                public void characters(char[] ch, int start,
                                       int length) throws SAXException {
                    if (ID)
                        criteriaMatchId.add(new String(ch, start, length));
                }
            };
            saxParser.parse(outFile, handler);
        } catch (ParserConfigurationException e) {
            //e.printStackTrace();
        } catch (SAXException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }
        return criteriaMatchId;
    }

    private static boolean verifyInvalidPageDefOAT(final File pageDef,
                                                   final ArrayList<String> ViewCriterialName,
                                                   final String processedOATSDir,
                                                   final Collection<File> labelVerBundleFiles) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {
                public void startElement(String uri, String localName,
                                         String qName,
                                         Attributes attributes) throws SAXException {
                    if (qName.equalsIgnoreCase("searchRegion") &&
                        ViewCriterialName.contains(attributes.getValue("Criteria"))) {
                        String Criteria = attributes.getValue("Criteria");
                        String uiFile =
                            pageDef.getName().replaceFirst("PageDef.xml",
                                                           ".jsff");
                        String uiFilePath =
                            generateUIFilePath(pageDef.getAbsolutePath(),
                                               uiFile);
                        String outFilePath =
                            Verify.VOMATCHOUTDIR + uiFile.replace("jsff",
                                                                  "xml");
                        ExtractFromSource obj = new ExtractFromSource();
                        obj.generateExceptionList(Verify.IGNORETAGSFILELOC);
                        obj.extract(uiFilePath, outFilePath, "id");

                        final File outFile = new File(outFilePath);
                        if (outFile.exists()) {
                            ArrayList<String> stackIds =
                                parseForQueryStack(outFile, Criteria);
                            Collection<File> oatsFiles =
                                getFileList(processedOATSDir,
                                            new String[] { "xml" }, true);
                            Iterator<File> oatsItr = oatsFiles.iterator();
                            final HashMap<String, String> mismatchTags =
                                new HashMap<String, String>();
                            for (int k = 0; k < stackIds.size(); k++) {
                                mismatchTags.put(stackIds.get(k), uiFilePath);
                            }
                            createpartialMap(mismatchTags);
                            final ArrayList<OutputData> faultyOATS =
                                new ArrayList<OutputData>();
                            while (oatsItr.hasNext()) {
                                final File itrFile = oatsItr.next();
                                try {
                                    SAXParserFactory factory =
                                        SAXParserFactory.newInstance();
                                    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                                                       false);
                                    SAXParser saxParser =
                                        factory.newSAXParser();
                                    DefaultHandler handler =
                                        new DefaultHandler() {
                                        Boolean ID = false;

                                        public void startElement(String uri,
                                                                 String localName,
                                                                 String qName,
                                                                 Attributes attributes) throws SAXException {

                                            if (qName.equalsIgnoreCase("id")) {
                                                ID = true;
                                            }
                                        }

                                        public void endElement(String uri,
                                                               String localName,
                                                               String qName) throws SAXException {
                                            if (qName.equalsIgnoreCase("id")) {
                                                ID = false;
                                            }
                                        }

                                        public void characters(char[] ch,
                                                               int start,
                                                               int length) throws SAXException {

                                            if (ID) {
                                                String tmpStr =
                                                    new String(ch, start,
                                                               length);
                                                if (stackMatch(tmpStr,
                                                               mismatchTags)) {
                                                    OutputData obj =
                                                        new OutputData();
                                                    obj.oatsFile =
                                                            itrFile.getParent().substring(itrFile.getParent().lastIndexOf(File.separator) +
                                                                                          1);
                                                    obj.oatsFileLoc =
                                                            itrFile.getAbsolutePath();
                                                    obj.sourceFile =
                                                            mismatchTags.get(tmpbuf);
                                                    obj.failId = tmpStr;
                                                    obj.headertext =
                                                            getQueryPanelHeaderText(tmpbuf,
                                                                                    outFile);
                                                    if (!faultyOATS.contains(obj))
                                                        faultyOATS.add(obj);
                                                }

                                            }
                                        }

                                        private String getQueryPanelHeaderText(final String failId,
                                                                               File outFile) {

                                            final ArrayList<String> headerTextList =
                                                new ArrayList<String>();
                                            try {
                                                SAXParserFactory factory =
                                                    SAXParserFactory.newInstance();
                                                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                                                                   false);
                                                SAXParser saxParser =
                                                    factory.newSAXParser();
                                                DefaultHandler handler =
                                                    new DefaultHandler() {
                                                    Boolean ID = false;
                                                    String headerText = "";

                                                    public void startElement(String uri,
                                                                             String localName,
                                                                             String qName,
                                                                             Attributes attributes) throws SAXException {
                                                        if (qName.equalsIgnoreCase("id")) {
                                                            ID = true;
                                                            if (attributes.getValue("header") !=
                                                                null) {
                                                                headerText =
                                                                        attributes.getValue("header");
                                                            }
                                                        }
                                                    }

                                                    public void endElement(String uri,
                                                                           String localName,
                                                                           String qName) throws SAXException {
                                                        ID = false;
                                                    }

                                                    public void characters(char[] ch,
                                                                           int start,
                                                                           int length) throws SAXException {
                                                        if (ID) {
                                                            String tmpStr =
                                                                new String(ch,
                                                                           start,
                                                                           length);
                                                            if (failId.equalsIgnoreCase(tmpStr)) {
                                                                if (!headerTextList.contains(headerText))
                                                                    headerTextList.add(headerText);
                                                                headerText =
                                                                        "";
                                                            }
                                                        }
                                                    }
                                                };
                                                saxParser.parse(outFile,
                                                                handler);
                                            } catch (IOException e) {
                                                //e.printStackTrace();
                                            } catch (ParserConfigurationException e) {
                                                //e.printStackTrace();
                                            } catch (SAXException e) {
                                                //e.printStackTrace();
                                            }
                                            final String headerValue =
                                                headerTextList.get(0);
                                            headerTextList.clear();
                                            //------------------obtain String value for the bundle header id----------------------
                                            int beginIndex =
                                                headerValue.indexOf("#{");
                                            int endIndex =
                                                headerValue.indexOf("['");
                                            int stringBeginIndex =
                                                headerValue.indexOf("['",
                                                                    beginIndex) +
                                                2;
                                            int stringEndIndex =
                                                headerValue.indexOf("']",
                                                                    stringBeginIndex);
                                            HashMap<String, String> headerToStringMap =
                                                new HashMap<String, String>();
                                            labelVerBundleFiles.iterator();
                                            while (beginIndex != -1 &&
                                                   endIndex != -1) {
                                                headerToStringMap.put(headerValue.substring(beginIndex +
                                                                                            2,
                                                                                            endIndex) +
                                                                      ".xlf",
                                                                      headerValue.substring(stringBeginIndex,
                                                                                            stringEndIndex));
                                                beginIndex =
                                                        headerValue.indexOf("#{",
                                                                            endIndex);
                                                endIndex =
                                                        headerValue.indexOf("['",
                                                                            beginIndex);
                                                stringBeginIndex =
                                                        headerValue.indexOf("['",
                                                                            beginIndex) +
                                                        2;
                                                stringEndIndex =
                                                        headerValue.indexOf("']",
                                                                            stringBeginIndex);
                                            }
                                            String bundleFile = "";
                                            for (final Entry<String, String> entry :
                                                 headerToStringMap.entrySet()) {
                                                bundleFile =
                                                        Verify.LABELVERBUNDLEDIR +
                                                        File.separator +
                                                        entry.getKey();
                                                //		System.out.println("Initial Value : "+bundleFile);
                                                //	entry.getValue();
                                                try {
                                                    BufferedReader br =
                                                        new BufferedReader(new FileReader(new File(Verify.BUNDLELIST)));
                                                    //		System.out.println("Locating : "+entry.getKey());
                                                    String tmp = "";
                                                    while ((tmp =
                                                            br.readLine()) !=
                                                           null) {
                                                        if (tmp.contains(entry.getKey()))
                                                            bundleFile = tmp;
                                                    }
                                                    //	System.out.println("Location : "+bundleFile);
                                                    br.close();
                                                    SAXParserFactory factory =
                                                        SAXParserFactory.newInstance();
                                                    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                                                                       false);
                                                    SAXParser saxParser =
                                                        factory.newSAXParser();
                                                    DefaultHandler handler =
                                                        new DefaultHandler() {
                                                        Boolean ID = false;
                                                        Boolean SOURCE = false;

                                                        public void startElement(String uri,
                                                                                 String localName,
                                                                                 String qName,
                                                                                 Attributes attributes) throws SAXException {
                                                            if (qName.equals("trans-unit")) {
                                                                if (attributes.getValue("id") !=
                                                                    null &&
                                                                    attributes.getValue("id").equalsIgnoreCase(entry.getValue())) {
                                                                    ID = true;
                                                                }
                                                            }
                                                            if (qName.equals("source")) {
                                                                SOURCE = true;
                                                            }
                                                        }

                                                        public void endElement(String uri,
                                                                               String localName,
                                                                               String qName) throws SAXException {
                                                            if (qName.equals("trans-unit")) {
                                                                ID = false;
                                                            }
                                                            if (qName.equals("source")) {
                                                                SOURCE = false;
                                                            }
                                                        }

                                                        public void characters(char[] ch,
                                                                               int start,
                                                                               int length) throws SAXException {
                                                            if (ID && SOURCE) {
                                                                String tmpBuf =
                                                                    new String(ch,
                                                                               start,
                                                                               length);
                                                                headerTextList.add(tmpBuf);
                                                                SOURCE = false;
                                                            }
                                                        }
                                                    };
                                                    saxParser.parse(new File(bundleFile),
                                                                    handler);
                                                } catch (IOException e) {
                                                    System.out.println("Bundle file :" +
                                                                       bundleFile +
                                                                       " not found during VerifyVO String extraction");
                                                    //e.printStackTrace();
                                                } catch (ParserConfigurationException e) {
                                                    //e.printStackTrace();
                                                } catch (SAXException e) {
                                                    //e.printStackTrace();
                                                }
                                            }
                                            //------------------obtain String value for the bundle header id----------------------
                                            StringBuffer buf =
                                                new StringBuffer();
                                            buf.append(headerTextList.get(0));
                                            if (headerTextList.size() > 0) {

                                                for (int i = 1;
                                                     i < headerTextList.size();
                                                     i++) {
                                                    buf.append(":" +
                                                               headerTextList.get(i));
                                                }
                                                return buf.toString();
                                            } else {
                                                System.out.println("Trans-HeaderId not found");
                                                return "";
                                            }
                                        }
                                    };
                                    saxParser.parse(itrFile, handler);
                                } catch (ParserConfigurationException e) {
                                    //e.printStackTrace();
                                } catch (SAXException e) {
                                    //e.printStackTrace();
                                } catch (IOException e) {
                                    //e.printStackTrace();
                                }
                            }

                            //--------------------------------------Second Pass for Eliminating False Positive with Header String Matches : Start---------
                            ArrayList<OutputData> faultyOatsRefinded =
                                new ArrayList<OutputData>();

                            for (int i = 0; i < faultyOATS.size(); i++) {
                                OutputData tmpObj = faultyOATS.get(i);
                                if (verifyHeaderInOATS(faultyOATS.get(i).oatsFileLoc,
                                                       faultyOATS.get(i).headertext.replaceAll("\\s",
                                                                                               ""))) {
                                    faultyOatsRefinded.add(tmpObj);
                                }
                            }

                            HashMap<String, ArrayList<OutputData>> printFormatMaptmp =
                                new HashMap<String, ArrayList<OutputData>>();
                            for (int i = 0; i < faultyOatsRefinded.size();
                                 i++) {
                                if (printFormatMaptmp.containsKey(faultyOatsRefinded.get(i).sourceFile)) {
                                    ArrayList<OutputData> tmp =
                                        printFormatMaptmp.get(faultyOatsRefinded.get(i).sourceFile);
                                    OutputData tmpObj =
                                        faultyOatsRefinded.get(i);
                                    tmp.add(tmpObj);
                                } else {
                                    ArrayList<OutputData> tmp =
                                        new ArrayList<OutputData>();
                                    OutputData tmpObj =
                                        faultyOatsRefinded.get(i);
                                    tmp.add(tmpObj);
                                    printFormatMaptmp.put(faultyOatsRefinded.get(i).sourceFile,
                                                          tmp);
                                }
                            }

                            String tmpBuftmp;
                            int lastIndextmp;
                            String subTmpBuftmp;
                            for (Entry<String, ArrayList<OutputData>> entry :
                                 printFormatMaptmp.entrySet()) {
                                System.out.println("Source File : " +
                                                   entry.getKey());
                                tmpBuftmp = entry.getValue().get(0).failId;
                                lastIndextmp = tmpBuftmp.lastIndexOf(":");
                                subTmpBuftmp =
                                        tmpBuftmp.substring(lastIndextmp + 1);
                                System.out.println("Query Panel Component Id:" +
                                                   subTmpBuftmp);
                                RunOatsVerification.ERRORSRAISED++;
                                System.out.println("List of OATS scripts impacted by code change :\n");
                                ArrayList<OutputData> tmp = entry.getValue();
                                for (int i = 0; i < tmp.size(); i++) {
                                    System.out.println((i + 1) + "\t" +
                                                       tmp.get(i).oatsFileLoc.replace(".xml",
                                                                                      ""));
                                }
                            }

                            //--------------------------------------Second Pass for Eliminating False Positive with Header String Matches : End---------
                        } else {
                            System.out.println("Unable to locate out file");
                        }
                    }
                }

                public void endElement(String uri, String localName,
                                       String qName) throws SAXException {

                }

                public void characters(char[] ch, int start,
                                       int length) throws SAXException {

                }
            };
            saxParser.parse(pageDef, handler);
        } catch (ParserConfigurationException e) {
            //e.printStackTrace();
        } catch (SAXException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }

        return false;
    }

    private static boolean verifyHeaderInOATS(String oatsFileLoc,
                                              final String headertext) {
        File oats = new File(oatsFileLoc);
        final ArrayList<Boolean> flag = new ArrayList<Boolean>();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                               false);
            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {
                public void startElement(String uri, String localName,
                                         String qName,
                                         Attributes attributes) throws SAXException {

                }

                public void endElement(String uri, String localName,
                                       String qName) throws SAXException {
                }


                public void characters(char[] ch, int start,
                                       int length) throws SAXException {
                    String tmpBuf = new String(ch, start, length);
                    tmpBuf = tmpBuf.replaceAll("\\s", "");
                    if (tmpBuf.equalsIgnoreCase(headertext)) {
                        flag.add(true);
                    }
                }
            };
            saxParser.parse(oats, handler);
        } catch (IOException io) {

        } catch (SAXNotRecognizedException e) {
            //			e.printStackTrace();
        } catch (SAXNotSupportedException e) {
            //			e.printStackTrace();
        } catch (ParserConfigurationException e) {
            //			e.printStackTrace();
        } catch (SAXException e) {
            //			e.printStackTrace();
        }
        if (flag.size() > 0)
            return true;
        else
            return false;
    }

    public static void LocatePageDef(String pageDefListFileName,
                                     ArrayList<String> ViewCriterialName,
                                     String processedOATSDir) {
        try {
            BufferedReader br =
                new BufferedReader(new FileReader(pageDefListFileName));
            Collection<File> labelVerBundleFiles =
                getFileList(Verify.LABELVERBUNDLEDIR, new String[] { "xlf" },
                            true);
            new HashMap<String, String>();
            String tmpStr;
            while ((tmpStr = br.readLine()) != null) {
                File tmpFile = new File(tmpStr);
                if (tmpFile.exists()) {
                    verifyInvalidPageDefOAT(tmpFile, ViewCriterialName,
                                            processedOATSDir,
                                            labelVerBundleFiles);
                }
            }
            br.close();
        } catch (FileNotFoundException e) {
            //e.printStackTrace();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

}

