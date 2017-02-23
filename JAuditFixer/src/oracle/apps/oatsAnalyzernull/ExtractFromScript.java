package oracle.apps.oatsAnalyzernull;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

public class ExtractFromScript {
	private Pattern TAG_REGEX;
	private StringBuffer buf = new StringBuffer();

	private ArrayList<String> getTagValues(String str) {
		ArrayList<String> tagValues = new ArrayList<String>();
		Matcher matcher = TAG_REGEX.matcher(str);
		while (matcher.find()) {
			if(matcher.groupCount()>0){
				tagValues.add(matcher.group(1));
			}
				
		}
		return tagValues;
	}

	public Collection<File> getFileList(String dirPath,
			String[] extensions, Boolean recursive) {
		return FileUtils.listFiles(new File(dirPath), extensions, recursive);
	}
	
	private ArrayList<File> getFileFromOATSList(String OATSFile){
		ArrayList<File> oatsList= new ArrayList<File>();
		File oatsFile = new File(OATSFile);
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(oatsFile));
			String tmp;
			while((tmp=br.readLine())!=null){
				oatsList.add(new File(tmp));
			}
			br.close();
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			//e.printStackTrace();
		}
		
		return oatsList;
	}
	
	public void extractFromScriptListBatch(String OATSFile,String scriptsDirOut,String[] tag){
		ArrayList<File> fileAr = getFileFromOATSList(OATSFile);
		ExtractFromScript obj = new ExtractFromScript();
		String str;
		for(int i =0;i<fileAr.size();i++){
			File tmp = fileAr.get(i);
			str=tmp.getName();
			obj.extract(tmp.getAbsolutePath(), scriptsDirOut +tmp.getParent().replace("D:", "")+File.separator+ str + ".xml",
					tag);
		}
	}
	
	public void extractFromScriptBatch(String scriptsDirInp,
			String scriptsDirOut, String[] extensions, Boolean recursive,
			String[] tag) {
		Collection<File> filesLists = getFileList(scriptsDirInp, extensions,
				recursive);
		ExtractFromScript obj = new ExtractFromScript();
		Iterator<File> itr = filesLists.iterator();
		String str = "";
		String[] ar;
		while (itr.hasNext()) {
			File tmp = itr.next();
			str = (tmp.getParent().substring(tmp.getParent().lastIndexOf(File.separator)+1)).replace(' ', '-');
			obj.extract(tmp.getAbsolutePath(), scriptsDirOut+str + ".xml",
					tag);
		}
	}
	
	public void extract(String inFile, String outFile, String[] tagArray) {
		try {
		File infile = new File(inFile);
		File outfile = new File(outFile);
		outfile.getParentFile().mkdirs();
		
		BufferedReader br;
		
		for(int i1=0;i1<tagArray.length;i1++){
			br = new BufferedReader(new FileReader(infile));
			TAG_REGEX = Pattern.compile(tagArray[i1]);
			ArrayList<String> matches = new ArrayList<String>();			
				String strBuf;
				while ((strBuf = br.readLine()) != null) {
					matches = getTagValues(strBuf);
					if (matches.size() > 0) {
						for (int i = 0; i < matches.size(); i++) {
								buf.append("<id>");
								buf.append(matches.get(i).replaceAll("&", "&amp;"));
								buf.append("</id>");	
						}
					}
				}
				
				br.close();
		}
		//FileWriter fw = new FileWriter(outfile);
		BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
		bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		bw.write("<" + infile.getName().replaceAll(" ", "") + ">");
	
		bw.write(buf.toString());
	
		bw.write("</" + infile.getName() + ">");
		
		bw.close();
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			//e.printStackTrace();
		}
		
		buf.delete(0, buf.length());
	}
}
