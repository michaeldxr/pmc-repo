package oracle.apps.di;

import java.io.CharArrayWriter;

import java.util.ArrayList;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SharedStringsHandler extends DefaultHandler {
    String previousString = "";
    ArrayList<String> strings = new ArrayList<String>();
    int numberOfStringsConcatenated = 0;
    boolean doConcatenation = true;

    /**
     * Contents writer to get the content between tags
     */
    private CharArrayWriter contents = new CharArrayWriter();


    public SharedStringsHandler(boolean doConcatenation) {
        super();
        this.doConcatenation = doConcatenation;
    }

    @Override
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        contents.reset();
    }

    @Override
    public void endElement(String uri, String localName,
                           String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if (localName.equals("t")) {
            String nodeContent = contents.toString();
            // cant skip this coz indexes will get messed up
            if (nodeContent == null)
                nodeContent="";
           // nodeContent = nodeContent.trim();
            if (!doConcatenation) {
                strings.add(nodeContent);
            } else {
                previousString = previousString + nodeContent;
                if (nodeContent.length() < 32766) {
                    strings.add(previousString);
                    // Adding empty strings into the array to preserve the indexes of the strings in the final list
                    if (numberOfStringsConcatenated > 0) {
                        while (numberOfStringsConcatenated > 0) {
                            strings.add("");
                            numberOfStringsConcatenated--;
                        }
                    }
                    previousString = "";
                    numberOfStringsConcatenated = 0;
                } else {
                    numberOfStringsConcatenated++;
                }
            }

        }
    }

    @Override
    public void characters(char[] ch, int start,
                           int length) throws SAXException {
        contents.write(ch, start, length);
    }

    public ArrayList<String> getStrings() {
        return strings;
    }
}
