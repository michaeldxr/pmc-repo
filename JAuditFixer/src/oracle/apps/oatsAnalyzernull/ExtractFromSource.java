package oracle.apps.oatsAnalyzernull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ExtractFromSource {
	private  Stack<String> idStack = new Stack<String>();
	private  StringBuffer buf = new StringBuffer();
	private  static ArrayList<String> exceptionList = new ArrayList<String>();
	private static ArrayList<String> attribList = new ArrayList<String>();
	public static HashMap<String, String> bundleMap = new HashMap<String, String>();

//Generate UIElements ExceptionList
	public void generateExceptionList(String exceptionFile){
	exceptionList.add("af:panelGroupLayout".toLowerCase());
	exceptionList.add("af:panelFormLayout".toLowerCase());
	exceptionList.add("af:panellabelmessage".toLowerCase());
	exceptionList.add("af:panelheader".toLowerCase());
	exceptionList.add("af:group".toLowerCase());
	//exceptionList.add("af:panelLabelAndMessage".toLowerCase());
	exceptionList.add("af:selectItem".toLowerCase());
	exceptionList.add("af:spacer".toLowerCase());
	exceptionList.add("af:popup".toLowerCase());
	//exceptionList.add("af:dialog".toLowerCase());
	exceptionList.add("af:contextInfo".toLowerCase());
	//exceptionList.add("af:panelTabbed".toLowerCase());
	exceptionList.add("af:showDetailItem".toLowerCase());
	
	}
	
	public void generateExceptionList(){
		//text, title, value, emptyText, label, popupTitle, headerText
		attribList.add("text");
		attribList.add("title");
		attribList.add("value");
		attribList.add("emptyText");
		attribList.add("label");
		attribList.add("popupTitle");
		attribList.add("headerText");
		attribList.add("query");
		
	}
//List all files in directory or sub directories based on parameters passed
	public Collection<File> getFileList(String dirPath,
			String[] extensions, Boolean recursive) {
		return FileUtils.listFiles(new File(dirPath), extensions, recursive);
	}
//Generates stack ids for the Tag passed as parameter	
	private void genStackTrace(String attribute,String type, String typeVal, String headerString) {
		
		if(type.equals("required")){
			buf.append("<component>");
			buf.append("<type>");
			buf.append(attribute);
			buf.append("</type>");
			buf.append("<id required='"+typeVal+"' header=\""+headerString+"\">");
		}
		else if (attribList.contains(type)){
			buf.append("<component>");
			buf.append("<type>");
			buf.append(attribute);
			buf.append("</type>");
			buf.append("<id "+type+"=\""+typeVal+"\" header=\""+headerString+"\">");
		}
		else{
			buf.append("<component>");
			buf.append("<type>");
			buf.append(attribute);
			buf.append("</type>");
			buf.append("<id required='"+false+"' header=\""+headerString+"\">");
		}
		@SuppressWarnings("unchecked")
		Stack<String> idStack2 = (Stack<String>) idStack.clone();
		Stack<String> idStack3 = new Stack<String>();
		while (!idStack2.isEmpty()) {
			if (idStack2.peek() != null) {
				idStack3.push(idStack2.pop());
			} else {
				idStack2.pop();
			}
		}
		while (!idStack3.isEmpty()) {
			if (idStack3.size() != 1)
				buf.append(idStack3.pop() + ":");
			else {
				buf.append(idStack3.pop());
			}
		}
		buf.append("</id>");
		
		buf.append("</component>");
		
	}

	public void extract(String inFile, String outFile, String tag) {
		File infile = new File(inFile);
		File outfile = new File(outFile);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
			
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
			SAXParser saxParser = factory.newSAXParser();
			buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			buf.append("<" + infile.getName() + ">");
			DefaultHandler handler = new DefaultHandler() {
				String componentHeader="";
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if(qName.equalsIgnoreCase("c:set")){
						bundleMap.put(attributes.getValue("var"), attributes.getValue("value"));
					}
					if(qName.equalsIgnoreCase("fnd:applicationsPanel")){
						if(attributes.getValue("title")!=null){
							componentHeader=attributes.getValue("title");
						}
						else{
							componentHeader="";
						}
					}
					if(qName.equalsIgnoreCase("fnd:dialog")){
						if(attributes.getValue("title")!=null){
							componentHeader=attributes.getValue("title");
						}
						else{
							componentHeader="";
						}
					}
					if(!exceptionList.contains(qName.toLowerCase())){
						idStack.push(attributes.getValue("id"));
						if (attributes.getValue("id") != null && ( attributes.getValue("rendered")==null || (attributes.getValue("rendered")!=null && !attributes.getValue("rendered").equalsIgnoreCase("false"))) && ( attributes.getValue("visible")==null || (attributes.getValue("visible")!=null && !attributes.getValue("visible").equalsIgnoreCase("false")))) {
							if(attributes.getValue("required")!=null){
								if(attributes.getValue("required").equalsIgnoreCase("true")){
									
									genStackTrace(qName,"required","true",componentHeader);
								}
								else{
									genStackTrace(qName,"required","false",componentHeader);
								}
							}
							else{
								genStackTrace(qName,"required","false",componentHeader);
							}
							if(qName.equalsIgnoreCase("af:query")){
								genStackTrace(qName,"query",attributes.getValue("value"),componentHeader);
							}
							
							if (attributes.getValue("text")!=null){
								genStackTrace(qName,"text",attributes.getValue("text"),componentHeader);
							}
							if (attributes.getValue("title")!=null){
								genStackTrace(qName,"title",attributes.getValue("title"),componentHeader);
							}
							if (attributes.getValue("value")!=null){
								genStackTrace(qName,"value",attributes.getValue("value"),componentHeader);
							}
							if (attributes.getValue("label")!=null){
								genStackTrace(qName,"label",attributes.getValue("label"),componentHeader);
							}
							if (attributes.getValue("popupTitle")!=null){
								genStackTrace(qName,"popupTitle",attributes.getValue("popupTitle"),componentHeader);
							}
							if (attributes.getValue("headerText")!=null){
								genStackTrace(qName,"headerText",attributes.getValue("headerText"),componentHeader);
							}
							
						}
					}
					
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					if(!exceptionList.contains(qName.toLowerCase()))
						idStack.pop();
					
					if(qName.equalsIgnoreCase("fnd:applicationsPanel")){
						componentHeader="";
					}
				}

				public void characters(char ch[], int start, int length)
						throws SAXException {

				}
			};
			saxParser.parse(infile, handler);
			buf.append("</" + infile.getName() + ">");
			bw.write(buf.toString());
			buf.delete(0,  buf.length());
			bw.close();
		} catch (IOException e) {
			//e.printStackTrace();
		} catch (ParserConfigurationException e) {
			//e.printStackTrace();
		} catch (SAXException e) {
			//e.printStackTrace();
		}

	}
//Extract method which takes DIR as argument	
	public void extractFromScriptSourceBatch(String scriptsDirInp,
			String scriptsDirOut, String[] extensions, Boolean recursive,
			String tag) {
		Collection<File> filesLists = getFileList(scriptsDirInp, extensions,
				recursive);
		ExtractFromSource obj = new ExtractFromSource();
		Iterator<File> itr = filesLists.iterator();
		String str = "";
		while (itr.hasNext()) {
			File tmp = itr.next();
			str = tmp.getName().replace('.', '_');
			obj.extract(tmp.getAbsolutePath(), scriptsDirOut + str + ".xml",
					tag);
		}
	}
}

