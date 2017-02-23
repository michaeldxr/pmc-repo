package oracle.apps.oatsAnalyzernull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class CustomComparator implements Comparator<OutputData> {
    @Override
    public int compare(OutputData o1, OutputData o2) {
        return o1.failId.compareTo(o2.failId);
    }
}
class CustomBundleComparator implements Comparator<OutputData> {
    @Override
    public int compare(OutputData o1, OutputData o2) {
        return o1.labelString.compareTo(o2.labelString);
    }
}
public class BundleExtract {
	static HashMap<String,String> bundleMap= new HashMap<String, String>();
	private static void printAttributes(String filePath){
		final Set<String> atrib = new HashSet<String>();
		File infile = new File(filePath);
		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {

				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					int attCount= attributes.getLength();
					for(int i =0 ; i < attCount ; i++ ){
						if(attributes.getValue(i).contains("Bundle") || attributes.getValue(i).contains("bundle")){
							atrib.add(attributes.getQName(i));
						}
					}
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					
				}

				public void characters(char ch[], int start, int length)
						throws SAXException {

				}
			};
			saxParser.parse(infile, handler);
			//System.out.println(atrib.toString());
		} catch (IOException e) {
			//e.printStackTrace();
		} catch (ParserConfigurationException e) {
			//e.printStackTrace();
		} catch (SAXException e) {
			//e.printStackTrace();
		}

	}
	public static Collection<File> getFileList(String dirPath,
			String[] extensions, Boolean recursive) {
		return FileUtils.listFiles(new File(dirPath), extensions, recursive);
	}
	public static Collection<File> getFileList(String fileList) {
		Collection<File> fileCollection= new ArrayList<File>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(fileList)));
			String tmp = "";
			while((tmp=br.readLine())!=null){
				fileCollection.add(new File(tmp));
			}
			br.close();
		} catch (IOException e) {
			//e.printStackTrace();
		}
		return fileCollection;
	}
	
	private static HashMap<String,String> generateBundleMap(String sourceOutputFile ){
		final HashMap<String,String> sourceBundleMap = new HashMap<String, String>();
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
			Boolean ID =false;
			String attName;
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if(qName.equalsIgnoreCase("id")){
						if(attributes.getValue("text")!= null){
							ID=true;
							attName= attributes.getValue("text");
						}
						else if(attributes.getValue("title")!= null){
							ID=true;
							attName= attributes.getValue("title");
						}
						else if(attributes.getValue("value")!= null){
							ID=true;
							attName= attributes.getValue("value");
						}
						else if(attributes.getValue("emptyText")!= null){
							ID=true;
							attName= attributes.getValue("emptyText");
						}
						else if(attributes.getValue("label")!= null){
							ID=true;
							attName= attributes.getValue("label");
						}
						else if(attributes.getValue("popupTitle")!= null){
							ID=true;
							attName= attributes.getValue("popupTitle");
						}
						else if(attributes.getValue("headerText")!= null){
							ID=true;
							attName= attributes.getValue("headerText");
						}
					}
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					ID=false;
				}

				public void characters(char ch[], int start, int length)
						throws SAXException {
					if(ID){
						if(attName!=null)
						{
							//System.out.println(attName);
							String tmp =new String(ch, start, length);
							sourceBundleMap.put(attName+"$"+tmp,new String(ch, start, length));
						}
					}
				}
			};
			saxParser.parse(new File(sourceOutputFile), handler);
		} catch (IOException e) {
			//e.printStackTrace();
		} catch (ParserConfigurationException e) {
			//e.printStackTrace();
		} catch (SAXException e) {
			//e.printStackTrace();
		}
		return sourceBundleMap;
	}
		
	private static HashMap<String,String> generateBundleMapAdv(final String sourceOutputFile ){
		final HashMap<String,String> sourceBundleMap = new HashMap<String, String>();
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
			Boolean ID =false;
			String attName;
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if(qName.equalsIgnoreCase("id")){
						if(attributes.getValue("text")!= null){
							ID=true;
							attName= attributes.getValue("text");
						}
						else if(attributes.getValue("title")!= null){
							ID=true;
							attName= attributes.getValue("title");
						}
						else if(attributes.getValue("value")!= null){
							ID=true;
							attName= attributes.getValue("value");
						}
						else if(attributes.getValue("emptyText")!= null){
							ID=true;
							attName= attributes.getValue("emptyText");
						}
						else if(attributes.getValue("label")!= null){
							ID=true;
							attName= attributes.getValue("label");
						}
						else if(attributes.getValue("popupTitle")!= null){
							ID=true;
							attName= attributes.getValue("popupTitle");
						}
						else if(attributes.getValue("headerText")!= null){
							ID=true;
							attName= attributes.getValue("headerText");
						}
					}
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					ID=false;
				}

				public void characters(char ch[], int start, int length)
						throws SAXException {
					if(ID){
						if(attName!=null)
						{
							//System.out.println(attName);
							String tmp =new String(ch, start, length);
							sourceBundleMap.put(attName+"$"+tmp,sourceOutputFile);
						}
					}
				}
			};
			saxParser.parse(new File(sourceOutputFile), handler);
		} catch (IOException e) {
			//e.printStackTrace();
		} catch (ParserConfigurationException e) {
			//e.printStackTrace();
		} catch (SAXException e) {
			//e.printStackTrace();
		}
		return sourceBundleMap;
	}
	static void getBundleMismatchAdv(String labelVerDir, String viewVerDir,String oatsOutputDir,String viewBundleDir,String lableBundleDir){
		Collection<File> labelVerOutputFiles = getFileList(labelVerDir, new String[]{"xml"}, true);
		Collection<File> viewVerOutputFiles = getFileList(viewVerDir, new String[]{"xml"}, true);
		
		HashMap<String,String> labelFileLocMap = new HashMap<String, String>();
		HashMap<String,String> viewFileLocMap = new HashMap<String, String>();
		
		HashMap<String,String> viewVerBundleMapRaw = new HashMap<String, String>();
		HashMap<String,String> labelVerBundleMapRaw = new HashMap<String, String>();
		
		HashMap<String,String> viewVerBundleMap = new HashMap<String, String>();
		HashMap<String,String> labelVerBundleMap = new HashMap<String, String>();
		
		HashMap<String,String> diffBundleMap = new HashMap<String, String>();
		
		Iterator<File> labelItr = labelVerOutputFiles.iterator();
		Iterator<File> viewItr = viewVerOutputFiles.iterator();
		while(labelItr.hasNext()){
			File tmp = labelItr.next();
			labelFileLocMap.put(tmp.getName(),tmp.getAbsolutePath());
		}
		while(viewItr.hasNext()){
			File tmp = viewItr.next();
			viewFileLocMap.put(tmp.getName(),tmp.getAbsolutePath());
		}
		
		for (Entry<String, String> entry : viewFileLocMap.entrySet()) {
			if(labelFileLocMap.containsKey(entry.getKey())){
				
				viewVerBundleMapRaw=generateBundleMapAdv(entry.getValue());
				labelVerBundleMapRaw=generateBundleMapAdv(labelFileLocMap.get(entry.getKey()));
				for(Entry<String,String> entry1 :viewVerBundleMapRaw.entrySet()){
					int endIndex=entry1.getKey().lastIndexOf("$");
					String key = (String) entry1.getKey().subSequence(0, endIndex);
					int leftBrace= key.indexOf("['");
					int rightBrace=key.indexOf("']");
					while(leftBrace!=-1 && rightBrace!=-1 && rightBrace > leftBrace){
						viewVerBundleMap.put(key.substring(leftBrace+2, rightBrace)+"#"+entry1.getKey().substring(endIndex+1), (String) entry1.getValue());
						leftBrace=key.indexOf("['", leftBrace+1);
						rightBrace=key.indexOf("']",rightBrace+1);
					}
				}
				
				for(Entry<String,String> entry1 :labelVerBundleMapRaw.entrySet()){
					int endIndex=entry1.getKey().lastIndexOf("$");
					String key = (String) entry1.getKey().subSequence(0, endIndex);
					int leftCurly=key.indexOf("{");
					int leftBrace= key.indexOf("['");
					int rightBrace=key.indexOf("']");
					while(leftBrace!=-1 && rightBrace!=-1 && leftCurly!=-1 && rightBrace > leftBrace){
					//VERIFY EXCEPTIONS
						try{
							labelVerBundleMap.put(key.substring(leftBrace+2, rightBrace)+"#"+entry1.getKey().substring(endIndex+1), (String) entry1.getValue());
							bundleMap.put(key.substring(leftBrace+2, rightBrace), key.substring(leftCurly+1,leftBrace));	
						}catch(Exception e){
							//bundleMap.put(key.substring(leftBrace+2, rightBrace),"");
							//e.printStackTrace();
							break;
						}
						leftBrace=key.indexOf("['", leftBrace+1);
						rightBrace=key.indexOf("']",rightBrace+1);
						int rightCurly=key.indexOf("}",leftCurly);
						if(rightCurly!=-1)
							leftCurly=key.indexOf("{",rightCurly);
					}
				}
				
				for (Entry<String, String> entry1 : labelVerBundleMap.entrySet()) {
					
					if(viewVerBundleMap.containsKey(entry1.getKey())){
						String viewMapValueTmp=viewVerBundleMap.get(entry1.getKey());
						String labelMapValueTmp=entry1.getValue();
						int lastIndexView=viewMapValueTmp.lastIndexOf(File.separator);
						int lastIndexlabel=labelMapValueTmp.lastIndexOf(File.separator);
						String viewMapValue=viewMapValueTmp.substring(lastIndexView);
						String labelMapValue=labelMapValueTmp.substring(lastIndexlabel);
						if(!viewMapValue.equalsIgnoreCase(labelMapValue)){
							diffBundleMap.put(entry1.getKey(), entry1.getValue());
						}
					}
					else{
						diffBundleMap.put(entry1.getKey(), entry1.getValue());
					}
				}
				labelVerBundleMap.clear();
				viewVerBundleMap.clear();
			}
			else{
				System.out.println("File :"+entry.getKey()+" not found in Label Server if new file added please ignore");
			}
			labelVerBundleMapRaw.clear();
			viewVerBundleMapRaw.clear();
		}
		labelVerBundleMap=null;
		viewVerBundleMap=null;
		labelVerOutputFiles=null;
		viewVerOutputFiles=null;
		labelFileLocMap=null;
		viewFileLocMap=null;
		labelVerOutputFiles=null;
		viewVerOutputFiles=null;
		matchBundleStringadv(oatsOutputDir, viewBundleDir, diffBundleMap);
}
	
	
	private static void matchBundleStringadv(String oatsOutputDir,String labelBundleDir, HashMap<String, String> diffBundleMap) {

		//Collection<File> labelVerBundleFiles = getFileList(labelBundleDir, new String[]{"xlf"}, true);
		Collection<File> labelVerBundleFiles = getFileList(Verify.BUNDLELIST);
		HashMap<String,String> labelVerBundleFileMap = new HashMap<String, String>();
		Iterator<File> lblItr = labelVerBundleFiles.iterator();
		while(lblItr.hasNext()){
			File tmpFile = lblItr.next();
			labelVerBundleFileMap.put(tmpFile.getName(), tmpFile.getAbsolutePath());
		}
		//------------------------------------------------------------------------------------------------------
		
		//Preprocess to make map of bundleheaders and arraylist of files affected from diffBundleMap
		HashMap<String,ArrayList<String>> diffBundleMapRefined = new HashMap<String, ArrayList<String>>();
		for(Entry<String,String> entry : diffBundleMap.entrySet()){
			String tmpStr= entry.getKey().substring(0,entry.getKey().indexOf("#"));
			if(diffBundleMapRefined.containsKey(tmpStr)){
				ArrayList<String> fileList = diffBundleMapRefined.get(tmpStr);
				fileList.add(entry.getValue());
				diffBundleMapRefined.put(tmpStr, fileList);
			}
			else{
				ArrayList<String> fileList = new ArrayList<String>();
				fileList.add(entry.getValue());
				diffBundleMapRefined.put(tmpStr, fileList);
			}
		}
		
		for (Entry<String,ArrayList<String>> entry : diffBundleMapRefined.entrySet()) {
			String tmpStr=entry.getKey();
			String bundleFileName = bundleMap.get(tmpStr);
			int lastIndex=bundleFileName.lastIndexOf(".");
			bundleFileName=bundleFileName.substring(lastIndex+1);
			String transUnitId=tmpStr;
			if(labelVerBundleFileMap.get(bundleFileName+".xlf")!=null){
				final ArrayList<String> labelStr= getTransUnitID(transUnitId,labelVerBundleFileMap.get(bundleFileName+".xlf"));
				//System.out.println("labelStr :"+labelStr);
				ArrayList<OutputData> faultyOATS1 = matchStringInOats(oatsOutputDir,labelStr);
				//System.out.println("labelStr :"+labelStr.toString());
				HashMap<String, String> requiredIdMap = new HashMap<String, String>();
				for(int k=0;k<entry.getValue().size();k++){
					requiredIdMap.putAll(Verify.getRequiredIds(entry.getValue().get(k)));
				}
				//HashMap<String, String> requiredIdMap = Verify.getRequiredIds(entry.getValue());
				ArrayList<OutputData> faultyOATS2 = getFaultyOatsByRequiredID(requiredIdMap,oatsOutputDir);
				HashMap<String,OutputData> faultyOatsMap1 = new HashMap<String, OutputData>();
				for(int k=0;k<faultyOATS1.size();k++){
					faultyOatsMap1.put(faultyOATS1.get(k).oatsFileLoc, faultyOATS1.get(k));
				}
				HashMap<String,OutputData> faultyOatsMap2 = new HashMap<String, OutputData>();
				for(int k=0;k<faultyOATS2.size();k++){
					faultyOatsMap2.put(faultyOATS2.get(k).oatsFileLoc, faultyOATS2.get(k));
				}
				
				ArrayList<OutputData> faultyOATS = new ArrayList<OutputData>();
				int size1= faultyOatsMap1.size();
				int size2= faultyOatsMap2.size();
				
				if(size1>=size2){
					for(Entry<String,OutputData> entry2 : faultyOatsMap2.entrySet()){
						if(faultyOatsMap1.containsKey(entry2.getKey())){
							faultyOATS.add(faultyOatsMap1.get(entry2.getKey()));
						}
					}
				}
				else{
					for(Entry<String,OutputData> entry2 : faultyOatsMap1.entrySet()){
						if(faultyOatsMap2.containsKey(entry2.getKey())){
							faultyOATS.add(entry2.getValue());
						}
					}
				}
				
				//Convert paths to UI filenames
				ArrayList<String> fileList = new ArrayList<String>();
				for(int k=0;k<entry.getValue().size();k++){
					int lastIndex1 = entry.getValue().get(k).lastIndexOf(File.separator);
					String tmp =entry.getValue().get(k).substring(lastIndex1+1);
					tmp = tmp.replace(".xml", "");
					tmp = tmp.replace("_", ".");
					fileList.add(tmp);	
				}
				
				if(faultyOATS.size()>0){
					RunOatsVerification.ERRORSRAISED++;
					int flag=0;
						for(int k=0;k<faultyOATS.size();k++){
							String labelString=faultyOATS.get(k).labelString;
							if(!faultyOATS.get(k).labelString.equalsIgnoreCase(labelString)){
								flag=0;
							}
							if(flag==0){
								System.out.println("Source File : "+bundleFileName);
								System.out.println("Label Version String ID : "+transUnitId);
								System.out.println("Label Version String  Value: "+labelString);
								System.out.println("List of UI Files Affected : ");	
								for(int m=0;m<fileList.size();m++){
									System.out.println((m+1)+"\t"+fileList.get(m));
								}
								System.out.println();
								System.out.println("List of OATS scripts impacted by code change:\n");
								flag=1;
							}
							System.out.println((k+1)+"\t"+faultyOATS.get(k).oatsFileLoc.replace(".xml", ""));
						}
						System.out.println("\n");
					
				}
			}
		
			
			}
		
	}
	private static ArrayList<OutputData> getFaultyOatsByRequiredID(final HashMap<String, String> requiredIdMap, String oatsOutputDir) {
		Verify.partialMap.clear();
		Verify.createpartialMap(requiredIdMap);
		Collection<File> oatsFiles = getFileList(oatsOutputDir, new String[]{"xml"}, true);
		Iterator<File> oatsItr = oatsFiles.iterator();
		final ArrayList<OutputData> faultyOATS = new ArrayList<OutputData>();
		while(oatsItr.hasNext()){
			final File itrFile = oatsItr.next();
			try {
				SAXParserFactory factory = SAXParserFactory.newInstance();
				factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
				SAXParser saxParser = factory.newSAXParser();
				DefaultHandler handler = new DefaultHandler() {
					Boolean ID = false;

					public void startElement(String uri, String localName,
							String qName, Attributes attributes)
							throws SAXException {
						if (qName.equalsIgnoreCase("id")) {
							ID = true;
						}
					}

					public void endElement(String uri, String localName,
							String qName) throws SAXException {
						if (qName.equalsIgnoreCase("id")) {
							ID = false;
						}
					}

					public void characters(char ch[], int start, int length)
							throws SAXException {
						if(ID){
							String tmpStr= new String(ch, start, length);
							//mismatchTags.containsKey(tmpStr)
							if(Verify.stackMatch(tmpStr,requiredIdMap)){
								//System.out.println(mismatchTags.toString());
							OutputData obj = new OutputData();
							obj.oatsFile=itrFile.getParent().substring(itrFile.getParent().lastIndexOf(File.separator)+1);
							obj.oatsFileLoc=itrFile.getAbsolutePath();
							obj.sourceFile=requiredIdMap.get(Verify.tmpbuf);
							obj.failId=tmpStr;
							/*System.out.println("obj.oatsFile :"+obj.oatsFile);
							System.out.println("obj.oatsFileLoc :"+obj.oatsFileLoc);
							System.out.println("obj.sourceFile :"+obj.sourceFile);
							System.out.println("obj.failId :"+obj.failId);*/
							if(!faultyOATS.contains(obj))
								faultyOATS.add(obj);
						}
						
					}
				}
				};
				saxParser.parse(itrFile, handler);
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}catch (SAXException e) {
				// TODO: handle exception
				////e.printStackTrace();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
			
	}
		return faultyOATS;
	}
	private static String getUIFileLocation(String value) {
		try{
			int lastIndex=value.lastIndexOf(File.separator);
			String fileName= value.substring(lastIndex);
			fileName=fileName.replace(".xml", "");
			fileName=fileName.replace("_", ".");
			System.out.println("Verify.labelverFiles :"+Verify.labelverFiles);
			BufferedReader br = new BufferedReader(new FileReader(new File(Verify.labelverFiles)));
			String tmpLine="";
			while((tmpLine=br.readLine())!=null){
				if(tmpLine.contains(fileName)){
					break;
				}
			}
			System.out.println("returning file name :"+tmpLine);
			br.close();
			return tmpLine;
		}catch(IOException e){
			System.out.println("IOException in reading labelverFiles");
		}
		return "";
	}
	static void getBundleMismatch(String labelVerDir, String viewVerDir,String oatsOutputDir,String viewBundleDir,String lableBundleDir){
		Collection<File> labelVerOutputFiles = getFileList(labelVerDir, new String[]{"xml"}, true);
		Collection<File> viewVerOutputFiles = getFileList(viewVerDir, new String[]{"xml"}, true);
		
		HashMap<String,String> labelFileLocMap = new HashMap<String, String>();
		HashMap<String,String> viewFileLocMap = new HashMap<String, String>();
		
		HashMap<String,String> viewVerBundleMapRaw = new HashMap<String, String>();
		HashMap<String,String> labelVerBundleMapRaw = new HashMap<String, String>();
		
		HashMap<String,String> viewVerBundleMap = new HashMap<String, String>();
		HashMap<String,String> labelVerBundleMap = new HashMap<String, String>();
		
		HashMap<String,String> diffBundleMap = new HashMap<String, String>();
		
		Iterator<File> labelItr = labelVerOutputFiles.iterator();
		Iterator<File> viewItr = viewVerOutputFiles.iterator();
		while(labelItr.hasNext()){
			File tmp = labelItr.next();
			labelFileLocMap.put(tmp.getName(),tmp.getAbsolutePath());
		}
		while(viewItr.hasNext()){
			File tmp = viewItr.next();
			viewFileLocMap.put(tmp.getName(),tmp.getAbsolutePath());
		}
		
		for (Entry<String, String> entry : viewFileLocMap.entrySet()) {
			if(labelFileLocMap.containsKey(entry.getKey())){
				
				viewVerBundleMapRaw=generateBundleMap(entry.getValue());
				labelVerBundleMapRaw=generateBundleMap(labelFileLocMap.get(entry.getKey()));
				
				for(Entry entry1 :viewVerBundleMapRaw.entrySet()){
					String key = (String) entry1.getKey();
					int leftBrace= key.indexOf("['");
					int rightBrace=key.indexOf("']");
					while(leftBrace!=-1 && rightBrace!=-1 && rightBrace > leftBrace){
						viewVerBundleMap.put(key.substring(leftBrace+2, rightBrace)+"#"+((String)entry1.getValue()), (String) entry1.getValue());
						leftBrace=key.indexOf("['", leftBrace+1);
						rightBrace=key.indexOf("']",rightBrace+1);
					}
				}

				for(Entry entry1 :labelVerBundleMapRaw.entrySet()){
					String key = (String) entry1.getKey();
					int leftCurly=key.indexOf("{");
					int leftBrace= key.indexOf("['");
					int rightBrace=key.indexOf("']");
					while(leftBrace!=-1 && rightBrace!=-1 && leftCurly!=-1 && rightBrace > leftBrace){
						labelVerBundleMap.put(key.substring(leftBrace+2, rightBrace)+"#"+((String)entry1.getValue()), (String) entry1.getValue());
						//VERIFY EXCEPTIONS
						bundleMap.put(key.substring(leftBrace+2, rightBrace), key.substring(leftCurly+1,leftBrace));
						leftBrace=key.indexOf("['", leftBrace+1);
						rightBrace=key.indexOf("']",rightBrace+1);
						int rightCurly=key.indexOf("}",leftCurly);
						if(rightCurly!=-1)
							leftCurly=key.indexOf("{",rightCurly);
					}
					//viewVerBundleMap.put(arg0, arg1)
				}

				if(!viewVerBundleMap.equals(labelVerBundleMap)){
					for (Entry<String, String> entry1 : labelVerBundleMap.entrySet()) {
						if(!viewVerBundleMap.containsKey(entry1.getKey()) || (viewVerBundleMap.containsKey(entry1.getKey()) && !viewVerBundleMap.get(entry1.getKey()).equalsIgnoreCase(entry1.getValue()))){
							diffBundleMap.put(entry1.getKey(), entry1.getValue());
						}
					}
				}
				labelVerBundleMap.clear();
				viewVerBundleMap.clear();
			}
			else{
				System.out.println("File :"+entry.getKey()+" not found in Label Server if new file added please ignore");
			}
			labelVerBundleMapRaw.clear();
			viewVerBundleMapRaw.clear();
		}
		labelVerBundleMap=null;
		viewVerBundleMap=null;
		labelVerOutputFiles=null;
		viewVerOutputFiles=null;
		labelFileLocMap=null;
		viewFileLocMap=null;
		labelVerOutputFiles=null;
		viewVerOutputFiles=null;
//		System.out.println("DIFF BUNDLE :"+diffBundleMap.toString());
		
		try{
		File dest= new File(Verify.LABELVERBUNDLEUIFILELOC);
		BufferedReader oatsListReader;
		for(Entry entry : diffBundleMap.entrySet()){
			String tmpStr= (String) entry.getKey();
			int hashIndex= tmpStr.indexOf("#");
			String tmpStr1= bundleMap.get(tmpStr.subSequence(0,hashIndex))+".xlf";
			System.out.println("Executing1 : "+"grep "+tmpStr1+" "+Verify.BUNDLELIST);
			oatsListReader = new BufferedReader(new FileReader(new File(Verify.BUNDLELIST)));
			String oatsLocTmpBuf="";
			while((oatsLocTmpBuf=oatsListReader.readLine())!=null){
				if(oatsLocTmpBuf.contains(tmpStr1)){
					break;
				}
			}
			oatsListReader.close();			
			
			String line = "";
			line=oatsLocTmpBuf;
			if(line !=null){
				System.out.println("LINE : "+line);
				int index = line.indexOf(File.separator+"fusionapps"+File.separator+"fin"+File.separator+"components"+File.separator);
				if(index!=-1){
					int index3=line.indexOf(File.separator, index+28);
					index3=line.indexOf(File.separator, index3+1);
					String subStr= line.substring(0, index3+1);
					System.out.println("SUBSTR :"+subStr);
						Collection<File> labelUIFiles=getFileList(subStr, new String[]{"jspx","jsff"}, true);
						Iterator<File> uiItr = labelUIFiles.iterator();
						
						while(uiItr.hasNext()){
							File tmp = uiItr.next();
							FileUtils.copyFileToDirectory(tmp, dest,true);
						}
						ArrayList<String> tmp = new ArrayList<String>();
						tmp.add(((String) entry.getKey()).substring(0,((String) entry.getKey()).indexOf("#")));
						System.out.println("TMP : "+tmp.toString());
						System.out.println("Reached Here");
				}
				else{
					System.out.println("Bundle :"+tmpStr1 +" not found in bundle list");
				}
			
			}
			else{
				System.out.println("Bundle grep failed for "+tmpStr1);
			}
			FileUtils.cleanDirectory(dest); 
			 
			//bw.write(bundleMap.get(tmpStr.subSequence(0,hashIndex))+".xlf");	
		}
		
		
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
		
		
		//labelVerOutputFiles = getFileList(labelVerDir, new String[]{"xml"}, true);
		System.out.println("diffBundleMap :"+diffBundleMap.toString());
		matchBundleString(oatsOutputDir, viewBundleDir, diffBundleMap);
	}
	
	private static String getKeyFromValue(HashMap<String, String> map ,String value){
		for(Entry<String,String> entry : map.entrySet()){
			if(entry.getValue().equalsIgnoreCase(value)){
				return entry.getKey();
			}
		}
		return "";
	}
	
	private static ArrayList<OutputData> matchStringInOats(String oatsOutputDir,final HashMap<String,String> labelStr){
		Collection<File> oatsFiles = getFileList(oatsOutputDir, new String[]{"xml"}, true);
		Iterator<File> oatsItr = oatsFiles.iterator();
		//final ArrayList<String> faultyOATS = new ArrayList<String>();
		final ArrayList<OutputData> faultyOATS = new ArrayList<OutputData>();
		while(oatsItr.hasNext()){
			final File itrFile = oatsItr.next();
			try {
				SAXParserFactory factory = SAXParserFactory.newInstance();
				SAXParser saxParser = factory.newSAXParser();
				DefaultHandler handler = new DefaultHandler() {
					Boolean ID = false;

					public void startElement(String uri, String localName,
							String qName, Attributes attributes)
							throws SAXException {
						if (qName.equalsIgnoreCase("id")) {
							ID = true;
						}
					}

					public void endElement(String uri, String localName,
							String qName) throws SAXException {
						if (qName.equalsIgnoreCase("id")) {
							ID = false;
						}
					}

					public void characters(char ch[], int start, int length)
							throws SAXException {
						if (ID) {
							String tmpStr= new String(ch, start, length);
							//System.out.println(tmpStr);
							if(labelStr.containsValue(tmpStr) && !faultyOATS.contains(itrFile.getParent().substring(itrFile.getParent().lastIndexOf(File.separator)+1))){
								OutputData obj = new OutputData();
								obj.oatsFileLoc=itrFile.getAbsolutePath();
								obj.failId=getKeyFromValue(labelStr, tmpStr);
								obj.oatsFile=itrFile.getParent().substring(itrFile.getParent().lastIndexOf(File.separator)+1);
								
								//faultyOATS.add(itrFile.getParent().substring(itrFile.getParent().lastIndexOf(File.separator)+1));	
								if(!faultyOATS.contains(obj))
									faultyOATS.add(obj);
							}
							
						}
					}
				};
				saxParser.parse(itrFile, handler);
			//	System.out.println(idMap.toString());
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}catch (SAXException e) {
				// TODO: handle exception
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		return faultyOATS;
	}

	
	private static ArrayList<OutputData> matchStringInOats(String oatsOutputDir,final ArrayList<String> labelStr){
		Collection<File> oatsFiles = getFileList(oatsOutputDir, new String[]{"xml"}, true);
		Iterator<File> oatsItr = oatsFiles.iterator();
		//final ArrayList<String> faultyOATS = new ArrayList<String>();
		final ArrayList<OutputData> faultyOATS = new ArrayList<OutputData>();
		while(oatsItr.hasNext()){
			final File itrFile = oatsItr.next();
			try {
				SAXParserFactory factory = SAXParserFactory.newInstance();
				SAXParser saxParser = factory.newSAXParser();
				DefaultHandler handler = new DefaultHandler() {
					Boolean ID = false;

					public void startElement(String uri, String localName,
							String qName, Attributes attributes)
							throws SAXException {
						if (qName.equalsIgnoreCase("id")) {
							ID = true;
						}
					}

					public void endElement(String uri, String localName,
							String qName) throws SAXException {
						if (qName.equalsIgnoreCase("id")) {
							ID = false;
						}
					}

					public void characters(char ch[], int start, int length)
							throws SAXException {
						if (ID) {
							String tmpStr= new String(ch, start, length);
							//System.out.println("TMPSTR :"+tmpStr);
							if(labelStr.contains(tmpStr) && !faultyOATS.contains(itrFile.getParent().substring(itrFile.getParent().lastIndexOf(File.separator)+1))){
								OutputData obj = new OutputData();
								obj.oatsFileLoc=itrFile.getAbsolutePath();
								obj.oatsFile=itrFile.getParent().substring(itrFile.getParent().lastIndexOf(File.separator)+1);
								obj.labelString=tmpStr;
								//faultyOATS.add(itrFile.getParent().substring(itrFile.getParent().lastIndexOf(File.separator)+1));	
								if(!faultyOATS.contains(obj))
									faultyOATS.add(obj);
							}
							
						}
					}
				};
				saxParser.parse(itrFile, handler);
			//	System.out.println(idMap.toString());
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}catch (SAXException e) {
				// TODO: handle exception
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		return faultyOATS;
	}
	
	private static void matchBundleString(String oatsOutputDir,String labelBundleDir, HashMap<String, String>diffBundleMap){
		Collection<File> labelVerBundleFiles = getFileList(labelBundleDir, new String[]{"xlf"}, true);
		HashMap<String,String> labelVerBundleFileMap = new HashMap<String, String>();
		Iterator<File> lblItr = labelVerBundleFiles.iterator();
		while(lblItr.hasNext()){
			File tmpFile = lblItr.next();
			labelVerBundleFileMap.put(tmpFile.getName(), tmpFile.getAbsolutePath());
		}
		for (Entry<String, String> entry : diffBundleMap.entrySet()) {
			String tmpStr=entry.getKey().substring(0,entry.getKey().indexOf("#"));
			String bundleFileName = bundleMap.get(tmpStr);
			int lastIndex=bundleFileName.lastIndexOf(".");
			bundleFileName=bundleFileName.substring(lastIndex+1);

			String transUnitId=tmpStr;
			if(labelVerBundleFileMap.get(bundleFileName+".xlf")!=null){
				final ArrayList<String> labelStr= getTransUnitID(transUnitId,labelVerBundleFileMap.get(bundleFileName+".xlf"));
				//System.out.println("labelStr :"+labelStr);
				ArrayList<OutputData> faultyOATS = matchStringInOats(oatsOutputDir,labelStr);
				//System.out.println("labelStr :"+labelStr.toString());
				
				if(faultyOATS.size()>0){
					RunOatsVerification.ERRORSRAISED++;
					int flag=0;
						for(int k=0;k<faultyOATS.size();k++){
							String labelString=faultyOATS.get(k).labelString;
							if(!faultyOATS.get(k).labelString.equalsIgnoreCase(labelString)){
								flag=0;
							}
							if(flag==0){
								System.out.println("Source File : "+bundleFileName);
								System.out.println("Label Version String ID : "+transUnitId);
								System.out.println("Label Version String  Value: "+labelString);
								System.out.println("List of OATS scripts impacted by code change:\n");
								flag=1;
							}
							//System.out.println((k+1)+".\t"+faultyOATS.get(k).oatsFile.replace(".xml", ""));
							System.out.println((k+1)+"\t"+faultyOATS.get(k).oatsFileLoc.replace(".xml", ""));
						}
					
				}
			}
		
			
			}
	}
	
	private static ArrayList<String> getTransUnitID(final String transUnitId, String bundleFile) {
		final ArrayList<String> stringVals= new ArrayList<String>();
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
			Boolean collectSource=false;
			Boolean inSource=false;
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if(qName.equalsIgnoreCase("trans-unit")){
						if(attributes.getValue("id")!=null && attributes.getValue("id").contains(transUnitId)){
							collectSource=true;
						}
					}
					if(qName.equalsIgnoreCase("source")){
						inSource=true;
					}
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
						if(qName.equalsIgnoreCase("trans-unit")){
							collectSource=false;
						}
						if(qName.equalsIgnoreCase("Source")){
							inSource=false;
						}
				}
				public void characters(char ch[], int start, int length)
						throws SAXException {
					if(inSource && collectSource){
						String str =new String(ch,start,length);
						//System.out.println("STR :"+str);
						if(str!=null)
							stringVals.add(str);
					}
}
			};
			saxParser.parse(new File(bundleFile), handler);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}catch (SAXException e) {
			// TODO: handle exception
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		

		return stringVals;
	}
	
	private static HashMap<String,String> generateBundleMapFromFile(String bundleFileName){
		final HashMap<String,String> idSourceMap = new HashMap<String, String>();
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
				Boolean inSource=false;
				Boolean inTransUnit=false;
				Boolean collectID=false;
				String tmpId="";
				
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if(qName.equalsIgnoreCase("trans-unit")){
						inTransUnit=true;
						if(attributes.getValue("id")!=null){
							tmpId=attributes.getValue("id");
							collectID=true;
						}
					}
					if(qName.equalsIgnoreCase("source")){
						inSource=true;
					}					
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					if(qName.equalsIgnoreCase("trans-unit")){
						inTransUnit=false;
					}
					if(qName.equalsIgnoreCase("source")){
						inSource=false;
					}
				}
				public void characters(char ch[], int start, int length)
						throws SAXException {
					if(inTransUnit && inSource && collectID){
						idSourceMap.put(tmpId,new String(ch,start,length));
						collectID=false;
					}
				}
			};
			saxParser.parse(new File(bundleFileName), handler);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}catch (SAXException e) {
			// TODO: handle exception
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return idSourceMap;
	}
	
	static void BundleToBundleMismatch(String labelSourceInputDir,String viewSourceInputDir, String oatsOutputDir) {
		Collection<File> labelVerBundleFiles = getFileList(labelSourceInputDir, new String[]{"xlf"}, true);
		Collection<File> viewVerBundleFiles = getFileList(viewSourceInputDir, new String[]{"xlf"}, true);
		
		HashMap<String,String> labelVerBundleFileMap = new HashMap<String, String>();
		HashMap<String,String> viewVerBundleFileMap = new HashMap<String, String>();
		
		Iterator<File> lblItr = labelVerBundleFiles.iterator();
		Iterator<File> viewItr = viewVerBundleFiles.iterator();
		
		while(lblItr.hasNext()){
			File tmpFile = lblItr.next();
			labelVerBundleFileMap.put(tmpFile.getName(), tmpFile.getAbsolutePath());
		}
		while(viewItr.hasNext()){
			File tmpFile = viewItr.next();
			viewVerBundleFileMap.put(tmpFile.getName(), tmpFile.getAbsolutePath());
		}
		
		for(Entry<String,String> entry : labelVerBundleFileMap.entrySet()){
			HashMap<String,String> bundleMismatch = new HashMap<String,String>();
			if(viewVerBundleFileMap.containsKey(entry.getKey())){
				//System.out.println(entry.getValue());
			//	System.out.println(generateBundleMapFromFile(entry.getValue()));
				HashMap<String,String> labelBundleMap = generateBundleMapFromFile(entry.getValue());
				HashMap<String,String> viewBundleMap = generateBundleMapFromFile(viewVerBundleFileMap.get(entry.getKey()));
				
				//System.out.println(viewBundleMap.toString());
				if(!labelBundleMap.equals(viewBundleMap)){
					for(Entry<String, String> entity1 : labelBundleMap.entrySet()){
						//System.out.println(viewBundleMap.get(entity1.getKey()));
						//System.out.println(entity1.getValue());
						if(!viewBundleMap.containsKey(entity1.getKey())  || (viewBundleMap.get(entity1.getKey()) !=null && !viewBundleMap.get(entity1.getKey()).equalsIgnoreCase(entity1.getValue()))  ){
							//System.out.println(entity1.getKey()+"-----"+entity1.getValue());
							bundleMismatch.put(entity1.getKey(),entity1.getValue());
						}
						else{
							viewBundleMap.remove(entity1.getKey());
						}
					}
					//System.out.println("ViewBundleMap "+viewBundleMap.toString());
					//System.out.println(bundleMismatch.toString());
					ArrayList<OutputData> faultyOATS = matchStringInOats(oatsOutputDir,bundleMismatch);
					Collections.sort(faultyOATS,new CustomComparator());
					if(faultyOATS.size()>0){
						RunOatsVerification.ERRORSRAISED++;
						int flag=0;
						String tmpStrBuf="";
						int counter=1;
						for(int k=0;k<faultyOATS.size();k++){
							if(!tmpStrBuf.equalsIgnoreCase(faultyOATS.get(k).failId)){
								flag=0;
								counter=1;
							}
							if(flag==0){
								System.out.println("\nBundle File Name : "+entry.getKey());
								System.out.println("Label Version String  : \""+bundleMismatch.get(faultyOATS.get(k).failId)+"\" \nLabel Version ID : "+faultyOATS.get(k).failId);
								System.out.println("Below OATS are affected :");
								flag=1;
								tmpStrBuf=faultyOATS.get(k).failId;
							}
							
							System.out.println(counter+"\t"+faultyOATS.get(k).oatsFileLoc.replace(".xml", ""));
							counter++;
						}
					}
					faultyOATS=null;
					bundleMismatch=null;
				}
			}
			else{
				System.out.println("View and Label File Mismatch");
			}
			
		}
		
	}
	
	static void BundleToBundleMismatchAdv(String labelSourceInputDir,String viewSourceInputDir, String oatsOutputDir) {
		Collection<File> labelVerBundleFiles = getFileList(labelSourceInputDir, new String[]{"xlf"}, true);
		Collection<File> viewVerBundleFiles = getFileList(viewSourceInputDir, new String[]{"xlf"}, true);
	
		HashMap<String,String> labelVerBundleFileMap = new HashMap<String, String>();
		HashMap<String,String> viewVerBundleFileMap = new HashMap<String, String>();
		
		Iterator<File> lblItr = labelVerBundleFiles.iterator();
		Iterator<File> viewItr = viewVerBundleFiles.iterator();
		
		while(lblItr.hasNext()){
			File tmpFile = lblItr.next();
			labelVerBundleFileMap.put(tmpFile.getName(), tmpFile.getAbsolutePath());
		}
		while(viewItr.hasNext()){
			File tmpFile = viewItr.next();
			viewVerBundleFileMap.put(tmpFile.getName(), tmpFile.getAbsolutePath());
		}
		for(Entry<String,String> entry : labelVerBundleFileMap.entrySet()){
			HashMap<String,String> bundleMismatch = new HashMap<String,String>();
			if(viewVerBundleFileMap.containsKey(entry.getKey())){
				HashMap<String,String> labelBundleMap = generateBundleMapFromFile(entry.getValue());
				HashMap<String,String> viewBundleMap = generateBundleMapFromFile(viewVerBundleFileMap.get(entry.getKey()));
				
				if(!labelBundleMap.equals(viewBundleMap)){
					for(Entry<String, String> entity1 : labelBundleMap.entrySet()){
						if(!viewBundleMap.containsKey(entity1.getKey())  || (viewBundleMap.get(entity1.getKey()) !=null && !viewBundleMap.get(entity1.getKey()).equalsIgnoreCase(entity1.getValue()))  ){
							bundleMismatch.put(entity1.getKey(),entity1.getValue());
						}
						else{
							viewBundleMap.remove(entity1.getKey());
						}
					}
					ArrayList<OutputData> faultyOATS = matchStringInOats(oatsOutputDir,bundleMismatch);

					try{
						File dest= new File(Verify.LABELVERBUNDLEUIFILELOC);
						BufferedReader oatsListReader = new BufferedReader(new FileReader(new File(Verify.BUNDLELIST)));
						int lastIndex= entry.getValue().lastIndexOf(File.separator);
						String tmpStr1= entry.getValue().substring(lastIndex+1);
						String oatsLocTmpBuf="";
						while((oatsLocTmpBuf=oatsListReader.readLine())!=null){
							if(oatsLocTmpBuf.contains(tmpStr1)){
								break;
							}
						}
						oatsListReader.close();
						String line=oatsLocTmpBuf;
						if(line !=null){
							int index = line.indexOf(File.separator+"fusionapps"+File.separator+"fin"+File.separator+"components"+File.separator);
							if(index!=-1){
      						int index3=line.indexOf(File.separator, index+28);
    							index3=line.indexOf(File.separator, index3+1);
  								String subStr= line.substring(0, index3+1);
									Collection<File> labelUIFiles=getFileList(subStr, new String[]{"jspx","jsff"}, true);
									Iterator<File> uiItr = labelUIFiles.iterator();
									
									while(uiItr.hasNext()){
										File tmp = uiItr.next();
										FileUtils.copyFileToDirectory(tmp, dest,true);
									}
									BundleToOATS.listImpactedOATS(bundleMismatch,entry.getValue());
								
							}
							else{
								System.out.println("Directory structure mismatch");
							}
						}else{
							System.out.println("Bundle "+entry.getValue()+" could not be located");
						}
					}catch(IOException e){
						//TODO: Raise relevant exception
					}
					faultyOATS=null;
					bundleMismatch=null;
				}
			}
			else{
				System.out.println("View and Label File Mismatch");
			}
			
		}
		
	}
	
	private static HashMap<String,String> extractBundleFromVO(String inputVOFile){
		final HashMap<String,String> BundleVOMap = new HashMap<String, String>();
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
			Boolean inViewAttribute= false;
			String Name="";
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if(qName.equalsIgnoreCase("ViewAttribute")){
						inViewAttribute=true;
						if(attributes.getValue("Name")!=null){
							Name=attributes.getValue("Name");
						}
					}
					for(int i=0;i< attributes.getLength();i++){
						String tmpAttr = attributes.getValue(i);
						if(tmpAttr.contains("${adfBundle")){
							BundleVOMap.put(tmpAttr, Name);
						}
					}
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					if(qName.equalsIgnoreCase("ViewAttribute")){
						inViewAttribute=false;
						Name="";
					}
				}

				public void characters(char ch[], int start, int length)
						throws SAXException {
					
				}
			};
			saxParser.parse(new File(inputVOFile), handler);
		//	System.out.println(idMap.toString());
		}catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}catch (SAXException e) {
			// TODO: handle exception
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		//System.out.println(BundleVOMap.toString());
		return BundleVOMap;
	}
	
	static void extractBundleMisMatchVO(String viewVerSourceDir, String labelVerSourceDir, String labelBundleDir, String viewBundleDir, String oatsOutputDir){
		Collection<File> labelVerFiles = getFileList(labelVerSourceDir, new String[]{"xml"}, true);
		Collection<File> viewVerFiles = getFileList(viewVerSourceDir, new String[]{"xml"}, true);	
		Iterator<File> labelVerItr = labelVerFiles.iterator();
		Iterator<File> viewVerItr = viewVerFiles.iterator();
		while(labelVerItr.hasNext()){
			String fname = labelVerItr.next().getName();
			if(fname.endsWith("VO.xml")){
			}
			else{
				labelVerItr.remove();
			}
		}
		
		while(viewVerItr.hasNext()){
			String fname = viewVerItr.next().getName();
			if(fname.endsWith("VO.xml")){
			}
			else{
				viewVerItr.remove();
			}
		}
		labelVerItr = labelVerFiles.iterator();
		viewVerItr = viewVerFiles.iterator();
		HashMap<String,String> labelVerBundleFileMap = new HashMap<String, String>();
		HashMap<String,String> viewVerBundleFileMap = new HashMap<String, String>();
		
		while(labelVerItr.hasNext()){
			File tmpFile = labelVerItr.next();
			labelVerBundleFileMap.put(tmpFile.getName(), tmpFile.getAbsolutePath());
		}
		while(viewVerItr.hasNext()){
			File tmpFile = viewVerItr.next();
			viewVerBundleFileMap.put(tmpFile.getName(), tmpFile.getAbsolutePath());
		}
		
		Collection<File> labelVerBundleFiles = getFileList(Verify.BUNDLELIST);
		Collection<File> viewVerBundleFiles = getFileList(Verify.BUNDLEVIEWLIST);
		HashMap<String,String> labelVerBundleMap = new HashMap<String, String>();
		HashMap<String,String> viewVerBundleMap = new HashMap<String, String>();
		
		Iterator<File> lblItr = labelVerBundleFiles.iterator();
		Iterator<File> viewItr = viewVerBundleFiles.iterator();
		
		while(lblItr.hasNext()){
			File tmpFile = lblItr.next();
			labelVerBundleMap.put(tmpFile.getName(), tmpFile.getAbsolutePath());
		}
		while(viewItr.hasNext()){
			File tmpFile = viewItr.next();
			viewVerBundleMap.put(tmpFile.getName(), tmpFile.getAbsolutePath());
		}
		
		
		//----------------------------------------------------------------------
		
		
		for(Entry<String,String> entry : labelVerBundleFileMap.entrySet()){
			if(viewVerBundleFileMap.containsKey(entry.getKey())){
				File labelVerFile= new File(entry.getValue());
				File viewVerFile= new File(viewVerBundleFileMap.get(entry.getKey()));
				
				//Extract Map for each file
				HashMap<String,String> labelVOMap=extractBundleFromVO(labelVerFile.getAbsolutePath());
				HashMap<String,String> viewVOMap=extractBundleFromVO(viewVerFile.getAbsolutePath());
				HashMap<String,String> diffVOMap = new HashMap<String, String>();
				//Match Map and if unequal locate mismatch Map entitites and locate label ver String value and use it to locate OATS using matchStringInOats
				if(labelVOMap.equals(viewVOMap)){
				//	System.out.println("Matched");
				}
				else{
					for(Entry<String,String> innerEntry : labelVOMap.entrySet()){
						if(!viewVOMap.containsKey(innerEntry.getKey())){
							diffVOMap.put(innerEntry.getKey(), innerEntry.getValue());
						}
						else if(!viewVOMap.get(innerEntry.getKey()).equalsIgnoreCase(innerEntry.getValue())){
							diffVOMap.put(innerEntry.getKey(), innerEntry.getValue());
						}
					}
					for(Entry<String,String> innEntry : diffVOMap.entrySet()){
						String bundleFileName= innEntry.getKey().substring(innEntry.getKey().indexOf("${adfBundle['")+13, innEntry.getKey().indexOf("']"));
						int lastIndex=bundleFileName.lastIndexOf(".");
						bundleFileName=bundleFileName.substring(lastIndex+1);
						String labelValue = innEntry.getKey().substring(innEntry.getKey().indexOf("']['")+4,innEntry.getKey().lastIndexOf("']"));
						//--------------------------------------------------------------------------------
						if(labelVerBundleMap.containsKey(bundleFileName+".xlf") && viewVerBundleMap.containsKey(bundleFileName+".xlf")){
							String transUnitId=labelValue;
							final ArrayList<String> labelStr= getTransUnitID(transUnitId,labelVerBundleMap.get(bundleFileName+".xlf"));
	//---------------------------------------------------------------------------------------------
								ArrayList<OutputData> faultyOATS = matchStringInOats(oatsOutputDir,labelStr);
								
								Collections.sort(faultyOATS,new CustomBundleComparator());
								//-------------------
								int flag=0;
								for(int k=0;k<faultyOATS.size();k++){
									String labelString=faultyOATS.get(k).labelString;
									if(!faultyOATS.get(k).labelString.equalsIgnoreCase(labelString)){
										flag=0;
									}
									if(flag==0){
										System.out.println("Source VO File: :"+entry.getKey());
										System.out.println("Bundle File Name : "+bundleFileName);
										System.out.println("Label Version String ID : "+labelValue);
										System.out.println("Source UI File : "+faultyOATS.get(k).labelString);
										System.out.println("List of OATS scripts impacted by code change :");
										flag=1;
									}
									System.out.println((k+1)+"\t"+faultyOATS.get(k).oatsFileLoc.replace(".xml", ""));
								}	
								System.out.println("---------------------------------------------------------------------------");
	//---------------------------------------------------------------------------------------------
							
							
						}
						else{
							System.out.println("Bundle file missing in label or view version****");
						}
						//--------------------------------------------------------------------------------
					}
				}
			}
			else{
				System.out.println("File mismatch in View or Label Version during Bundle Verification of VO");
			}
		}
		
	}
}

