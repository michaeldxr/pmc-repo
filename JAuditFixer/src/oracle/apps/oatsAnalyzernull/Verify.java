package oracle.apps.oatsAnalyzernull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.FileUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

class OutputData implements Comparator<OutputData> {
	String oatsFile="";
	String oatsFileLoc="";
	String sourceFile="";
	String failId="";
	String labelString="";
	String headertext="";
	@Override
	public boolean equals(Object other) {
	    if (!(other instanceof OutputData)) {
	        return false;
	    }
	    OutputData that = (OutputData) other;
	    return this.sourceFile.equals(that.sourceFile)
		        && this.oatsFile.equals(that.oatsFile) && this.oatsFileLoc.equals(that.oatsFileLoc) ;
	}
	@Override
	public int compare(OutputData obj1, OutputData obj2) {
		return obj1.labelString.compareTo(obj2.labelString);
	}
}
public class Verify {
	
	public static String LABELVERSOURCEFILELOC="D:\\OATS\\LabelVerSource\\";
	public static String VIEWVERSOURCEFILELOC="D:\\OATS\\ViewVerSource\\";
	public static String LABELVERSOURCEFILEOUTPUTLOC="D:\\OATS\\SourceOutput\\LabelVerOutFiles\\";
	public static String VIEWVERSOURCEFILEOUTPUTLOC= "D:\\OATS\\SourceOutput\\ViewVerOutFiles\\";
	public static String IGNORETAGSFILELOC="D:\\EclipseWorkspace\\VerifyOATS\\src\\IgnoreElements.txt";
	public static String OATSSOURCEDIRLIST="D:\\OATS\\FILELIST.txt";
	public static String OATSSOURCEOUTPUTDIR="D:\\OATS\\FileOATSOUT\\";
        public static String OATSSOURCEDIR="";
	public static String VOMATCHOUTDIR="D:\\OATS\\VOMatchOut\\";
	public static String VOMATCHINDIR="D:\\OATS\\VOMatchIn\\";
	public static String PAGEDEFLISTFILELOC="D:\\EclipseWorkspace\\VerifyOATS\\src\\PageDefList.txt";
	public static String LABELVERBUNDLEDIR="D:\\OATS\\BundleCase\\labelVerBundle\\";
	public static String VIEWVERBUNDLEDIR="D:\\OATS\\BundleCase\\viewVerBundle\\";
	
	public static String BUNDLELIST="D:\\OATS\\BundleList.txt";
	public static String BUNDLEVIEWLIST="";
	public static String TMPUIFILELIST="D:\\OATS\\UIFiles.txt";
	
	public static String LABELVERBUNDLEUIFILELOC="D:\\OATS\\LabelVerBundleUiSource\\";
	public static String LABELVERBUNDLEUIFILEOUTPUTLOC="D:\\OATS\\LabelVerBundleUiSourceOutput\\";
	
	public static String AFFECTEDBUNDLELISTFILENAMEUICASEDIR="D:\\OATS\\AffectedBundleFileUiCase\\";
	public static String AFFECTEDBUNDLELISTFILENAMEBUNDLECASEDIR="D:\\OATS\\AffectedBundleFileBundleCase\\";
	public static String AFFECTEDBUNDLELISTFILENAMEVOCASEDIR="D:\\OATS\\AffectedBundleFileVoCase\\";
	public static String labelverFiles;
	public static HashMap<String,ArrayList<String>> partialMap = new HashMap<String, ArrayList<String>>();
	static String tmpbuf;
	static void createpartialMap(HashMap<String,String> mismatchTags){
		String buf;
		for (Entry<String, String> entry : mismatchTags.entrySet()) {
			String two= entry.getKey();
			int start=0;
			int end= two.indexOf(':');
			while(end!=-1){
				buf= two.substring(start, end);
				start=end+1;
				end=two.indexOf(':', start);
				if(!partialMap.containsKey(entry.getKey())){
					ArrayList<String> tmp = new ArrayList<String>();
					tmp.add(buf);
					partialMap.put(entry.getKey(),tmp);
					tmp=null;
				}
				else{
					ArrayList<String> tmp = partialMap.get(entry.getKey());
					tmp.add(buf);
					partialMap.put(entry.getKey(),tmp);
					tmp=null;
				}
			}
			buf= two.substring(start);
			if(!partialMap.containsKey(entry.getKey())){
				ArrayList<String> tmp = new ArrayList<String>();
				tmp.add(buf);
				partialMap.put(entry.getKey(),tmp);
				tmp=null;
			}
			else{
				ArrayList<String> tmp = partialMap.get(entry.getKey());
				tmp.add(buf);
				partialMap.put(entry.getKey(),tmp);
				tmp=null;
			}
		}
		buf=null;
	}
	static boolean stackMatch(String one,HashMap<String,String> mismatchTags){
		int matchCount=0;
		int size=0;
		int start=0;
		int end=0;
		String buf;
		for (Entry<String, String> entry : mismatchTags.entrySet()) {
			String two= entry.getKey();
			try{
				if(one.substring(one.lastIndexOf(':')).equalsIgnoreCase(two.substring(two.lastIndexOf(':')))){
					ArrayList<String> partialList= partialMap.get(entry.getKey());
					size=partialList.size();
					start=0;
					end= one.indexOf(':');
					buf=null;
					while(end!=-1){
						buf= one.substring(start, end);
						start=end+1;
						end=one.indexOf(':', start);
						if(partialList.contains(buf)){
							matchCount++;
						}
					}
					buf= one.substring(start);
					if(partialList.contains(buf)){
						matchCount++;
					}
          //only add the size constraint is not enough, we should add the order constranint
					if(matchCount==size&&size>=2){
            int contain=one.indexOf(two);
            if(contain!=-1){
              
              tmpbuf=entry.getKey();
              return true;
            }
						//tmpbuf=entry.getKey();
						//return true;
					}
			}
			}
			catch (StringIndexOutOfBoundsException e) {
			}
		}
		buf=null;
		return false;
	}
	public static Collection<File> getFileList(String dirPath,
			String[] extensions, Boolean recursive) {
		return FileUtils.listFiles(new File(dirPath), extensions, recursive);
	}
	public static HashMap<String, String> generateIdMap(final String inpFile, final String tag) {
		final HashMap<String, String> idMap = new HashMap<String, String>();
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
				Boolean ID = false;

				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if (qName.equalsIgnoreCase(tag)) {
						ID = true;
					}
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					if (qName.equalsIgnoreCase(tag)) {
						ID = false;
					}
				}

				public void characters(char ch[], int start, int length)
						throws SAXException {
					if (ID) {
						idMap.put(new String(ch, start, length), inpFile);
					}
				}
			};
			saxParser.parse(inpFile, handler);
		} catch (ParserConfigurationException e) {
			System.out.println("Parser Error in File :"+inpFile);
			return new HashMap<String,String>();
			
		}catch (SAXException e) {
			System.out.println("Parser Error in File :"+inpFile);
			return new HashMap<String,String>();
		}
		catch (IOException e) {
			System.out.println("IOException in File :"+inpFile);
			return new HashMap<String,String>();
		}
		return idMap;
	}
	private static HashMap<String,String> compareXML(String labelVer, String viewVer, final String tag){
		final HashMap<String,String> diff = new HashMap<String,String>();
		final File lblVer = new File(labelVer);
		File vieVer = new File(viewVer);
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
				Boolean ID = false;

				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if (qName.equalsIgnoreCase(tag)) {
						ID = true;
					}
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					if (qName.equalsIgnoreCase(tag)) {
						ID = false;
					}
				}

				public void characters(char ch[], int start, int length)
						throws SAXException {
					if (ID) {
						
						diff.put(new String(ch, start, length),lblVer.getName());
					}
				}
			};
			saxParser.parse(lblVer, handler);
		} catch (ParserConfigurationException e) {
			System.out.println("Parser Exception in Label Version File : "+lblVer);
			diff.put("", lblVer.getName());
		}
		catch (SAXException e) {
		System.out.println("Parser Exception in Label Version File : "+lblVer);
		diff.put("", lblVer.getName());
		}
		catch (IOException e) {
			System.out.println("IOException in Label Version File : "+lblVer);
			diff.put("", lblVer.getName());
		}
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
				Boolean ID = false;

				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if (qName.equalsIgnoreCase(tag)) {
						ID = true;
					}
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					if (qName.equalsIgnoreCase(tag)) {
						ID = false;
					}
				}

				public void characters(char ch[], int start, int length)
						throws SAXException {
					if (ID) {
						if(diff.containsKey(new String(ch, start, length))){
							diff.remove(new String(ch, start, length));
						}
					}
				}
			};
			saxParser.parse(vieVer, handler);
		} catch (ParserConfigurationException e) {
			System.out.println("Parser Exception in View Version File : "+vieVer);
			diff.put("", vieVer.getName());
		}catch (SAXException e) {
			System.out.println("Parser Exception in View Version File : "+vieVer);
			diff.put("", vieVer.getName());
		}
		catch (IOException e) {
			System.out.println("IOException in View Version File : "+vieVer);
			diff.put("", vieVer.getName());
		}
		return diff;
	}

	public static ArrayList<OutputData> getFlagOATSModified(String processedOATSDir, final HashMap<String,String> mismatchTags, final String tag){
		Collection<File> oatsFiles = getFileList(processedOATSDir, new String[]{"xml"}, true);
		Iterator<File> oatsItr = oatsFiles.iterator();
		createpartialMap(mismatchTags);
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
						if (qName.equalsIgnoreCase(tag)) {
							ID = true;
						}
					}
					public void endElement(String uri, String localName,
							String qName) throws SAXException {
						if (qName.equalsIgnoreCase(tag)) {
							ID = false;
						}
					}
					public void characters(char ch[], int start, int length)
							throws SAXException {
						
						String tmpStr= new String(ch, start, length);
						if (ID) {
							//mismatchTags.containsKey(tmpStr)
							if(stackMatch(tmpStr,mismatchTags)){
								//System.out.println(mismatchTags.toString());
							OutputData obj = new OutputData();
							obj.oatsFile=itrFile.getParent().substring(itrFile.getParent().lastIndexOf(File.separator)+1);
							obj.oatsFileLoc=itrFile.getAbsolutePath();
							obj.sourceFile=mismatchTags.get(tmpbuf);
							obj.failId=tmpStr;
							if(!faultyOATS.contains(obj))
								faultyOATS.add(obj);
						}
					}
						tmpStr= null;
					}
				};
				saxParser.parse(itrFile, handler);
			} catch (ParserConfigurationException e) {
	//			e.printStackTrace();
	//			System.out.println("ParserConfigurationException FILE : "+itrFile.getAbsolutePath());
			}
			catch (SAXException e) {
				e.printStackTrace();
	//			System.out.println("SAXException FILE : "+itrFile.getAbsolutePath());
			}
			catch (IOException e) {
	//			e.printStackTrace();
	//			System.out.println("IOException FILE : "+itrFile.getAbsolutePath());
			}
		}
		oatsFiles=null;
		oatsItr=null;
		return faultyOATS;
	}
	
	private static HashMap<String,String> matchIds(String labelVerOutFile, String viewVerOutFile, final String tag){
		final HashMap<String,String> diffs = compareXML(labelVerOutFile,viewVerOutFile,tag);
		final HashMap<String,String> diffCruxId = new HashMap<String,String>();
		final HashMap<String, String> matchingIds = new HashMap<String, String>();
		Set<String> keys= diffs.keySet();
		Iterator<String> keyItr = keys.iterator();
		String tmpStr;
		while(keyItr.hasNext()){
			tmpStr = keyItr.next();
			if((tmpStr.lastIndexOf(":")+1)<tmpStr.length()){
				diffCruxId.put(tmpStr.substring(tmpStr.lastIndexOf(":")+1),tmpStr);
			}
		}
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
				Boolean ID = false;

				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if (qName.equalsIgnoreCase(tag)) {
						ID = true;
					}
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					if (qName.equalsIgnoreCase(tag)) {
						ID = false;
					}
				}

				public void characters(char ch[], int start, int length)
						throws SAXException {
					if (ID) {
						String tmpStr= new String(ch, start, length);
						String tmpStrbk= new String (tmpStr);
						if((tmpStr.lastIndexOf(":")+1)<tmpStr.length()){
							tmpStr= tmpStr.substring(tmpStr.lastIndexOf(":")+1);
							if(diffCruxId.containsKey(tmpStr)){
								matchingIds.put(diffCruxId.get(tmpStr),tmpStrbk);
								diffCruxId.remove(tmpStr);
							}	
						}
						tmpStr=null;
						tmpStrbk=null;
					}
				}
			};
			saxParser.parse(viewVerOutFile, handler);
		} catch (ParserConfigurationException e) {
			//e.printStackTrace();
		}
		catch (SAXException e) {
			//e.printStackTrace();
		}
		catch (IOException e) {
			//e.printStackTrace();
		}
		for (Entry<String, String> entry : diffCruxId.entrySet()) {
			matchingIds.put(entry.getKey(), "");
		}		
		return matchingIds;		
	}
	
	private static void generateOutputs(String scriptsOutDir, String labelVerSourceDir, String viewVerSourceDir,String sourceTag){
		Collection<File> labelVerSourceFiles = getFileList(labelVerSourceDir, new String[]{"xml"}, true);
		Collection<File> viewVerSourceFiles = getFileList(viewVerSourceDir, new String[]{"xml"}, true);
		ArrayList<String> viewVerSourceFileNames = new ArrayList<String>();
		HashMap<String,String> viewVerSourceFilePaths = new HashMap<String,String>();
		Iterator<File> viewVerSourceItrNames = viewVerSourceFiles.iterator();
		while(viewVerSourceItrNames.hasNext()){
			File tmpFile = viewVerSourceItrNames.next();
			viewVerSourceFileNames.add(tmpFile.getName());
			viewVerSourceFilePaths.put(tmpFile.getName(),tmpFile.getAbsolutePath());
		}
		Iterator<File> labelVerSourceItr = labelVerSourceFiles.iterator();
		while(labelVerSourceItr.hasNext()){
			File labelSource = labelVerSourceItr.next();
			if(viewVerSourceFileNames.contains(labelSource.getName())){
				ArrayList<OutputData> oatsMap = getFlagOATSModified(scriptsOutDir,compareXML(labelSource.getAbsolutePath(),viewVerSourceFilePaths.get(labelSource.getName()),sourceTag),sourceTag);
				
				if(oatsMap.size()!=0){
					String tmpStr1 = labelSource.getName();
					int index1= tmpStr1.lastIndexOf('_');
					int index2= tmpStr1.lastIndexOf('.');
					String origType = tmpStr1.substring(index1+1, index2);
					tmpStr1= tmpStr1.substring(0,index1)+"."+origType;
					RunOatsVerification.ERRORSRAISED++;
					HashMap<String,String> diffMap = matchIds(labelSource.getAbsolutePath(),viewVerSourceDir+labelSource.getName(),"id");
					if(diffMap.size()>0){
						System.out.println("Source File: " + tmpStr1);
						for (Entry<String, String> entry : diffMap.entrySet()) {
							if(entry.getValue().length()>0)
								System.out.println("Label Version Component ID  = " + entry.getKey() + "\nCurrent Transaction Component ID : " + entry.getValue());
							else
								System.out.println("Label Version Component ID  = " + entry.getKey() + "\nCurrent Transaction Component ID : NA, Component Missing in View");
							System.out.println("----------------------------------------------------------------------------");
						}
					}
					printFaultyOats(oatsMap);
					
					tmpStr1= null;
					origType=null;
				}
			}
			else{
				String tmpStr1 = labelSource.getName();
				int index1= tmpStr1.lastIndexOf('_');
				int index2= tmpStr1.lastIndexOf('.');
				String origType = tmpStr1.substring(index1+1, index2);
				tmpStr1= tmpStr1.substring(0,index1)+"."+origType;
				System.out.println("File mismatch between label and view");
				System.out.println("OutputFile not found :"+tmpStr1);
				tmpStr1= null;
				origType=null;
			}
		}
		labelVerSourceFiles=null;
		viewVerSourceFiles=null;
		viewVerSourceFileNames=null;
		viewVerSourceFilePaths=null;
		viewVerSourceItrNames=null;
		labelVerSourceItr=null;
	}
	
	static void printFaultyOats(ArrayList<OutputData> oatsMap){

		HashMap<String,ArrayList<OutputData>> printFormatMap = new HashMap<String, ArrayList<OutputData>>();
		for(int i =0 ;i< oatsMap.size();i++){
			String tmpStr1 = oatsMap.get(i).sourceFile;
			int index1= tmpStr1.lastIndexOf('_');
			int index2= tmpStr1.lastIndexOf('.');
			String origType = tmpStr1.substring(index1+1, index2);
			tmpStr1= tmpStr1.substring(0,index1)+"."+origType;
			if(printFormatMap.containsKey(tmpStr1)){
				ArrayList<OutputData> tmpArList = printFormatMap.get(tmpStr1);
				OutputData tmpObj=oatsMap.get(i);
				tmpArList.add(tmpObj);
				printFormatMap.put(tmpStr1, tmpArList);
			}
			else{
				ArrayList<OutputData> tmpArList = new ArrayList<OutputData>();
				OutputData tmpObj=oatsMap.get(i);
				tmpArList.add(tmpObj);
				printFormatMap.put(tmpStr1, tmpArList);
			}
			tmpStr1= null;
			origType=null;
			}
		System.out.println("List of OATS scripts impacted by code change : ");
		for(Entry<String,ArrayList<OutputData>> entry : printFormatMap.entrySet()){
			System.out.println();
			RunOatsVerification.ERRORSRAISED++;
			ArrayList<OutputData> tmpArList = entry.getValue();
			for(int i=0;i<tmpArList.size();i++){
				System.out.println((i+1)+"\t"+tmpArList.get(i).oatsFileLoc.replace(".xml", ""));
			}
		}
		printFormatMap=null;
	}
	
	private static  HashMap<String,String>  generateMismatchMaps(HashMap<String,String> viewMap){
		final HashMap<String,String> requiredMismatchMap = new HashMap<String, String>();
		for (final Entry<String, String> entry : viewMap.entrySet()) {
		    try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
				Boolean ComponentID = false;
				Boolean REQ = false;
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if(qName.equalsIgnoreCase("component")){
						ComponentID=true;
					}
					if(qName.equalsIgnoreCase("id") && ComponentID){
						if(attributes.getValue("required")!=null && attributes.getValue("required").equalsIgnoreCase("true") ){
							REQ=true;
						}
					}
					
				}

				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					if(qName.equalsIgnoreCase("component")){
						ComponentID=false;
					}
					if(qName.equalsIgnoreCase("id")){
						REQ=false;
					}
				}

				public void characters(char ch[], int start, int length)
						throws SAXException {
					if(ComponentID && REQ){
						requiredMismatchMap.put( new String(ch,start,length),entry.getValue());
					}
					
				}
			};
			saxParser.parse(entry.getValue(), handler);
		} catch (ParserConfigurationException e) {
		//	//e.printStackTrace();
		}catch (SAXException e) {
		//	//e.printStackTrace();
		}
		    catch (IOException e) {
		//	//e.printStackTrace();
		}
		
	}
		return requiredMismatchMap;
	}
	
	private static void validateNewRequiredFields(String labelVerSourceDir, String viewVerSourceDir, String processedOATSDir){
		Collection<File> labelVerSourceFiles = getFileList(labelVerSourceDir, new String[]{"xml"}, true);
		Collection<File> viewVerSourceFiles = getFileList(viewVerSourceDir, new String[]{"xml"}, true);
		HashMap<String,String> labelMap = new HashMap<String, String>();
		HashMap<String,String> viewMap = new HashMap<String, String>();
		final HashMap<String,String> partialMatchMap = new HashMap<String, String>();
		
		Iterator<File> labelItr = labelVerSourceFiles.iterator();
		while(labelItr.hasNext()){
			File tmpFile=labelItr.next();
			labelMap.put(tmpFile.getName(), tmpFile.getAbsolutePath());
		}
		Iterator<File> viewItr = viewVerSourceFiles.iterator();
		while(viewItr.hasNext()){
			File tmpFile=viewItr.next();
			viewMap.put(tmpFile.getName(), tmpFile.getAbsolutePath());
		}
		HashMap<String,String> misMatchViewMap = generateMismatchMaps(viewMap);
		HashMap<String,String> misMatchLabelMap = generateMismatchMaps(labelMap);
		for (Entry<String, String> entry : misMatchViewMap.entrySet()) {
		    if(!misMatchLabelMap.containsKey(entry.getKey())){
		    	String tmpStr1 = entry.getValue();
				partialMatchMap.put(entry.getKey(), tmpStr1);
		    	tmpStr1= null;
		    }
		}
		labelVerSourceFiles=null;
		viewVerSourceFiles=null;
		labelMap=null;
		viewMap=null;
		misMatchViewMap=null;
		misMatchLabelMap=null;
		Collection<File> oatsFiles = getFileList(processedOATSDir, new String[]{"xml"}, true);
		Iterator<File> oatsItr = oatsFiles.iterator();
		for(Entry<String,String> entry : partialMatchMap.entrySet()){
			oatsItr = oatsFiles.iterator();
			final HashMap<String, String> requiredIdMap = getRequiredIds(entry.getValue());
			partialMap.clear();
			createpartialMap(requiredIdMap);
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
								if(stackMatch(tmpStr,requiredIdMap)){
								OutputData obj = new OutputData();
								obj.oatsFile=itrFile.getParent().substring(itrFile.getParent().lastIndexOf(File.separator)+1);
								obj.oatsFileLoc=itrFile.getAbsolutePath();
								obj.sourceFile=requiredIdMap.get(tmpbuf);
								obj.failId=tmpStr;
								if(!faultyOATS.contains(obj))
									faultyOATS.add(obj);
							}
							
						}
					}
					};
					saxParser.parse(itrFile, handler);
				} catch (ParserConfigurationException e) {
					//e.printStackTrace();
				}catch (SAXException e) {
					////e.printStackTrace();
				}
				catch (IOException e) {
					//e.printStackTrace();
				}
				
		}
		if(faultyOATS.size()>0){
			String tmpStrBuf= faultyOATS.get(0).sourceFile;
			int index1= tmpStrBuf.lastIndexOf('_');
			int index2= tmpStrBuf.lastIndexOf('.');
			int index3= tmpStrBuf.lastIndexOf(File.separator);
			String origType = tmpStrBuf.substring(index1+1, index2);
			tmpStrBuf= tmpStrBuf.substring(0,index1)+"."+origType;
			tmpStrBuf=tmpStrBuf.substring(index3+1);
			System.out.println("Required ID Added : "+entry.getKey());
			System.out.println("UI File : "+tmpStrBuf);
			System.out.println("probable Oats Affected Due to Addition of Required Field :");
			for(int i=0;i<faultyOATS.size();i++){
				System.out.println((i+1)+"\t"+faultyOATS.get(i).oatsFileLoc);
			}	
			origType=null;
		}
		else{
			System.out.println("Required ID Added : "+entry.getKey());
			System.out.println("Unable to locate affected OATS. Please refer to Oats cases for corresponding UI File.");
		}
			System.out.println();
	}
			System.out.println("----------------------------------------------------------------------------");
}
	
public static HashMap<String, String> getRequiredIds(final String uiFile) {
		final HashMap<String,String> requiredIds = new HashMap<String, String>();
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
			SAXParser saxParser = factory.newSAXParser();
			DefaultHandler handler = new DefaultHandler() {
				Boolean ComponentID = false;
				Boolean REQ = false;
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if(qName.equalsIgnoreCase("component")){
						ComponentID=true;
					}
					if(qName.equalsIgnoreCase("id") && ComponentID){
						if(attributes.getValue("required")!=null && attributes.getValue("required").equalsIgnoreCase("true") ){
							REQ=true;
						}
					}
					
				}
				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					if(qName.equalsIgnoreCase("component")){
						ComponentID=false;
					}
					if(qName.equalsIgnoreCase("id")){
						REQ=false;
					}
				}
				public void characters(char ch[], int start, int length)
						throws SAXException {
					if(ComponentID && REQ){
						String tmpBuf = new String(ch,start,length);
						requiredIds.put(tmpBuf, uiFile);
						tmpBuf=null;
					}
					
				}
			};
			saxParser.parse(uiFile, handler);
		} catch (ParserConfigurationException e) {
		//	//e.printStackTrace();
		}catch (SAXException e) {
		//	//e.printStackTrace();
		}
		    catch (IOException e) {
		//	//e.printStackTrace();
		}
		return requiredIds;
	}
	public static void StartVerification(){
			
		ExtractFromSource obj = new ExtractFromSource();
		obj.generateExceptionList();
		long Start = System.nanoTime();
		//Extracts ids from changed source file from Latest Label
		ExtractFromSource sourceObjLabelVer = new ExtractFromSource();
		sourceObjLabelVer.generateExceptionList(IGNORETAGSFILELOC);
		sourceObjLabelVer.extractFromScriptSourceBatch(LABELVERSOURCEFILELOC, LABELVERSOURCEFILEOUTPUTLOC, new String[]{"jsff","jspx"}, false, "id");
		long Stop1 = System.nanoTime() / 1000000;
		//Extracts ids from changed source file transaction view
		ExtractFromSource sourceObjViewVer = new ExtractFromSource();
		sourceObjViewVer.extractFromScriptSourceBatch(VIEWVERSOURCEFILELOC, VIEWVERSOURCEFILEOUTPUTLOC, new String[]{"jsff","jspx"}, false, "id");
		long Stop2 = System.nanoTime() / 1000000;
		//Extracts ids from all OATs files
		OatsExtractThreaded.extractFromScriptBatchList(OATSSOURCEDIRLIST,OATSSOURCEOUTPUTDIR,new String[]{"absoluteLocator='(.+?)'","text='(.+?)'","title='(.+?)'","value='(.+?)'","popupTitle='(.+?)'","headerText='(.+?)'"});
		long Stop3 = System.nanoTime() / 1000000;
		System.out.println("======================UI Attribute Change in create/edit/view pages=======================");
		//Generate List of OATS files effected
		generateOutputs(OATSSOURCEOUTPUTDIR, LABELVERSOURCEFILEOUTPUTLOC, VIEWVERSOURCEFILEOUTPUTLOC, "id");
		long Stop4 = System.nanoTime() / 1000000;
		System.out.println("\n\n======================UI Attribute changes in Query Panel=======================");
		//Verify ViewCriteria UseCase
		//VerifyVO.FetchConflictingVO("D:\\OATS\\testlabel","D:\\OATS\\testView");
		ArrayList<String> errorCriteriaList=VerifyVO.FetchConflictingVO(LABELVERSOURCEFILELOC,VIEWVERSOURCEFILELOC);
		if(errorCriteriaList.size()>0){
			VerifyVO.LocatePageDef(PAGEDEFLISTFILELOC, errorCriteriaList,OATSSOURCEOUTPUTDIR);	
		}
		else{
			//System.out.println("No OATS affected due to Mismatch");
		}
		long Stop5 = System.nanoTime() / 1000000;
		
		System.out.println("\n\n======================Verifying Required Attribute Test Case=======================");
		validateNewRequiredFields(LABELVERSOURCEFILEOUTPUTLOC,VIEWVERSOURCEFILEOUTPUTLOC,OATSSOURCEOUTPUTDIR);
		long Stop6 = System.nanoTime() / 1000000;
			
		
		System.out.println("\n\n======================String Reference ID changes in .jsff / .jspx files======================");
		System.out.println("---------------------------------------------------------");
		//BundleExtract.getBundleMismatch(LABELVERSOURCEFILEOUTPUTLOC,VIEWVERSOURCEFILEOUTPUTLOC,OATSSOURCEOUTPUTDIR,VIEWVERBUNDLEDIR,LABELVERBUNDLEDIR);
		BundleExtract.getBundleMismatchAdv(LABELVERSOURCEFILEOUTPUTLOC,VIEWVERSOURCEFILEOUTPUTLOC,OATSSOURCEOUTPUTDIR,VIEWVERBUNDLEDIR,LABELVERBUNDLEDIR);
		long Stop7 = System.nanoTime() / 1000000;
		System.out.println("---------------------------------------------------------");
		System.out.println("\n\n======================String Value Changes======================");
		System.out.println("---------------------------------------------------------");
		//BundleExtract.BundleToBundleMismatchAdv(LABELVERBUNDLEDIR,VIEWVERBUNDLEDIR,OATSSOURCEOUTPUTDIR);
		BundleExtract.BundleToBundleMismatchAdv(LABELVERSOURCEFILELOC,VIEWVERSOURCEFILELOC,OATSSOURCEOUTPUTDIR);
		long Stop8 = System.nanoTime() / 1000000;
		System.out.println("---------------------------------------------------------");
		System.out.println("\n\n======================String Reference ID changes in EO.xml/VO.xml======================");
		System.out.println("---------------------------------------------------------");
		BundleExtract.extractBundleMisMatchVO(VIEWVERSOURCEFILELOC, LABELVERSOURCEFILELOC,  LABELVERBUNDLEDIR,VIEWVERBUNDLEDIR,OATSSOURCEOUTPUTDIR);
		long Stop9 = System.nanoTime() / 1000000;
		System.out.println("--------------------------End-----------------------------");
		
		/*System.out.println("-------------------Execution Timings----------------------");
		System.out.println("Label Source Extraction : "+(Stop1-Start)+" ms");
		System.out.println("View Source Extraction : "+(Stop2-Stop1)+" ms");
		System.out.println("Oats Scripts Extraction : "+(Stop3-Stop2)+" ms");
		System.out.println("UI Attribute Change in create /edit/view pages : "+(Stop4-Stop3)+" ms");
		System.out.println("Verifying ViewCriteria Test Case : "+(Stop5-Stop4)+" ms");
		System.out.println("Verifying Required Attribute Test Case :"+(Stop6-Stop5)+" ms");
		System.out.println("JSFF and JSPX Bundle mismatch UseCase : "+(Stop7-Stop6)+" ms");
		System.out.println("Bundle to Bundle Mismatch UseCase : "+(Stop8-Stop7)+" ms");
		System.out.println("VO.xml to Bundle Mismatch UseCase : "+(Stop9-Stop8)+" ms");*/
		
		System.out.println("ERROR:"+RunOatsVerification.ERRORSRAISED);
	}
}
