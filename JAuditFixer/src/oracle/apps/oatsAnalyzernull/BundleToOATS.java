package oracle.apps.oatsAnalyzernull;

import java.io.File;
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
import org.xml.sax.helpers.DefaultHandler;


public class BundleToOATS {

	public static Collection<File> getFileList(String dirPath,
			String[] extensions, Boolean recursive) {
		return FileUtils.listFiles(new File(dirPath), extensions, recursive);
	}
	
	private static boolean arrayListContains(String keyword, ArrayList<String> list){
		//System.out.println("keyword : "+keyword);
		for(int i=0;i<list.size();i++){
			if(keyword.contains(list.get(i))){
				return true;
			}
		}
		return false;
	}
	public static void listImpactedOATS(final HashMap<String, String> bundleMismatch, String bundleName){
		final ArrayList<String> bundleMismatchKeyList = new ArrayList<String>();
		bundleMismatchKeyList.addAll(bundleMismatch.keySet());
		ExtractFromSource sourceObjLabelVer = new ExtractFromSource();
		sourceObjLabelVer.generateExceptionList();
		sourceObjLabelVer.generateExceptionList(Verify.IGNORETAGSFILELOC);
		sourceObjLabelVer.extractFromScriptSourceBatch(Verify.LABELVERBUNDLEUIFILELOC, Verify.LABELVERBUNDLEUIFILEOUTPUTLOC, new String[]{"jsff","jspx"}, false, "id");
		Collection<File> labelVerSourceFiles = getFileList(Verify.LABELVERBUNDLEUIFILEOUTPUTLOC, new String[]{"xml"}, true);
		final ArrayList<String> affectedFiles = new ArrayList<String>();
		Iterator<File> labelItr= labelVerSourceFiles.iterator();
		final HashMap<String,String> uiFileComponentIdMap = new HashMap<String, String>();
		HashMap<String,ArrayList<OutputData>> uiFileOATSMap = new HashMap<String, ArrayList<OutputData>>();
		while(labelItr.hasNext()){
			final ArrayList<String> uiComponentId = new ArrayList<String>();
			final File uiFile = labelItr.next();
			try {
				SAXParserFactory factory = SAXParserFactory.newInstance();
				factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
				SAXParser saxParser = factory.newSAXParser();
				DefaultHandler handler = new DefaultHandler() {
				Boolean ID =false;
				String attName="";
					public void startElement(String uri, String localName,
							String qName, Attributes attributes)
							throws SAXException {
						if(qName.equalsIgnoreCase("id")){
							if(attributes.getValue("text")!= null){
								ID=true;
								if(arrayListContains(attributes.getValue("text"),bundleMismatchKeyList)){
									//System.out.println("text :"+attributes.getValue("text"));
									attName= attributes.getValue("text");
								}
							}
							else if(attributes.getValue("title")!= null){
								ID=true;
								if(arrayListContains(attributes.getValue("title"),bundleMismatchKeyList)){
									//System.out.println("title :"+attributes.getValue("title"));
									attName= attributes.getValue("title");
								}
							}
							else if(attributes.getValue("value")!= null){
								ID=true;
								if(arrayListContains(attributes.getValue("value"),bundleMismatchKeyList)){
									//System.out.println("value :"+attributes.getValue("value"));
									attName= attributes.getValue("value");
								}
							}
							else if(attributes.getValue("emptyText")!= null){
								ID=true;
								if(arrayListContains(attributes.getValue("emptyText"),bundleMismatchKeyList)){
									//System.out.println("emptyText :"+attributes.getValue("emptyText"));
									attName= attributes.getValue("emptyText");
								}
							}
							else if(attributes.getValue("label")!= null){
								ID=true;
								if(arrayListContains(attributes.getValue("label"),bundleMismatchKeyList)){
									//System.out.println("label :"+attributes.getValue("label"));
									attName= attributes.getValue("label");
								}
							}
							else if(attributes.getValue("popupTitle")!= null){
								ID=true;
								if(arrayListContains(attributes.getValue("popupTitle"),bundleMismatchKeyList)){
								//	System.out.println("popupTitle :"+attributes.getValue("popupTitle"));
									attName= attributes.getValue("popupTitle");
								}
							}
							else if(attributes.getValue("headerText")!= null){
								ID=true;
								if(arrayListContains(attributes.getValue("headerText"),bundleMismatchKeyList)){
								//	System.out.println("headerText :"+attributes.getValue("headerText"));
									attName= attributes.getValue("headerText");
								}
							}
						}
					}

					public void endElement(String uri, String localName,
							String qName) throws SAXException {
						if(qName.equalsIgnoreCase("id"))
							ID=false;
						//attName=null;
					}

					public void characters(char ch[], int start, int length)
							throws SAXException {
						if(ID){
						//	System.out.println(attName);
							if(!attName.equals(""))
							{
								//System.out.println(attName);
								String tmp =new String(ch, start, length);
								uiFileComponentIdMap.put(new String(ch, start, length),uiFile.getName());
								affectedFiles.add(uiFile.getAbsolutePath());
								attName="";
								ID=false;
							}
						}
					}
				};
				saxParser.parse(uiFile, handler);
			} catch (IOException e) {
        //File not found
			} catch (ParserConfigurationException e) {
				//e.printStackTrace();
			} catch (SAXException e) {
				//e.printStackTrace();
			}
			Verify.partialMap.clear();
			if(uiFileComponentIdMap.size()>0){
				ArrayList<OutputData> faultyOATS = Verify.getFlagOATSModified(Verify.OATSSOURCEOUTPUTDIR, uiFileComponentIdMap, "id");
				if(faultyOATS.size()>0){
					for(int i=0;i<faultyOATS.size();i++){
					}	
					if(uiFileOATSMap.containsKey(uiFile.getName())){
						ArrayList<OutputData> tmpList = uiFileOATSMap.get((uiFile.getName()));
						tmpList.addAll(faultyOATS);
						uiFileOATSMap.put(uiFile.getName(),tmpList);
					}
					else{
						uiFileOATSMap.put(uiFile.getName(),faultyOATS);	
					}
				}
			}
			
			uiComponentId.clear();
			uiFileComponentIdMap.clear();
		}
		ArrayList<String> compiledList = new ArrayList<String>();
		for(Entry<String,ArrayList<OutputData>> entry : uiFileOATSMap.entrySet()){
			ArrayList<OutputData> tmpObj = entry.getValue();
			for(int i=0;i<tmpObj.size();i++){
				if(!compiledList.contains(tmpObj.get(i).oatsFileLoc)){
					compiledList.add(tmpObj.get(i).oatsFileLoc);
				}
			}
		}
		
		//Remove duplicates in UI Files
		
		ArrayList<String> uiFileList = new ArrayList<String>();
		for(int i=0;i<affectedFiles.size();i++){
			int lastIndex=affectedFiles.get(i).lastIndexOf(File.separator);
			
			if(!uiFileList.contains(affectedFiles.get(i).substring(lastIndex+1))){
				uiFileList.add(affectedFiles.get(i).substring(lastIndex+1));
			}
		}
		
		if(compiledList.size()>0){
			int LastBundleIndex= bundleName.lastIndexOf(File.separator);
			String bundleTmp = bundleName.substring(LastBundleIndex+1);
			System.out.println("Source File : "+bundleTmp);
			System.out.print("Label Version String ID : ");
			for(int k=0;k<bundleMismatchKeyList.size();k++){
				if(k!=bundleMismatchKeyList.size()-1)
					System.out.print(bundleMismatchKeyList.get(k)+", ");
				else{
					System.out.println(bundleMismatchKeyList.get(k));
				}
			}
			System.out.print("Label Version String  Value : ");
			int mapSize= bundleMismatch.size();
			for(Entry<String,String> entry : bundleMismatch.entrySet()){
				if(mapSize!=1){
					System.out.print(entry.getValue()+", ");
				}
				else{
					System.out.println(entry.getValue());
				}
			}
			System.out.println("List of UI Files Affected :");
			
			
			for(int i=0;i<uiFileList.size();i++){
				//int lastIndex=uiFileList.get(i).lastIndexOf(File.separator);
				String tmp =uiFileList.get(i).replace(".xml", "");
				tmp=tmp.replace("_", ".");
				System.out.println((i+1)+"\t"+tmp);
			}
			System.out.println();
			System.out.println("List of OATS scripts impacted by code change:");
			for(int i=0;i<compiledList.size();i++){
				System.out.println((i+1)+"\t"+compiledList.get(i));
			}
		}
	}
}

