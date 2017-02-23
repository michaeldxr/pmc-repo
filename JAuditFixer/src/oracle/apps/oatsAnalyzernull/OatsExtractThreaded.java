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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

class Extract implements Runnable{
	private Pattern TAG_REGEX;
	private StringBuffer buf = new StringBuffer();
	String inFile;
	String outFile; 
	String[] tagArray;
	
	public Extract(String inFile, String outFile, String[] tagArray) {
		this.inFile=inFile;
		this.outFile=outFile;
		this.tagArray=tagArray;
	}
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
	public void extractThread() {
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
		BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
		bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		bw.write("<_" + infile.getName().replaceAll(" ", "") + ">");
		bw.write(buf.toString());
		bw.write("</_" + infile.getName().replaceAll(" ", "") + ">");
		bw.close();
		} catch (FileNotFoundException e) {
			//e.printStackTrace();
		} catch (IOException e) {
			//e.printStackTrace();
		}
		buf.delete(0, buf.length());
	}
	@Override
	public void run() {
		//System.out.println("Executing thread : START");
		extractThread();
		//System.out.println("Executing thread : END");
	}
	
}

public class OatsExtractThreaded {
	public static String OATSSOURCEDIRLIST="D:\\OATS\\FILELIST.txt";
	public static String OATSSOURCEOUTPUTDIR="D:\\OATS\\THREADTEST\\";
	public static  Collection<File> getFileList(String dirPath,
			String[] extensions, Boolean recursive) {
		return FileUtils.listFiles(new File(dirPath), extensions, recursive);
	}
	private static ArrayList<File> getFileFromOATSList(String OATSFile){
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
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
		return oatsList;
	}
	public static  void extractFromScriptBatchList(String OATSFile,String scriptsDirOut,String[] tag){
		
		ArrayList<File> fileAr = getFileFromOATSList(OATSFile);
		//System.out.println("Input Count : "+fileAr.size());
		int threadCount=Runtime.getRuntime().availableProcessors();
		//threadCount=10;
		//System.out.println("Thread Pool Size : "+threadCount);
		ConcurrentLinkedQueue<File> fileQueue = new ConcurrentLinkedQueue<File>();
		fileQueue.addAll(fileAr);
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		String str="";
		while(!fileQueue.isEmpty()){
			File tmpFile= fileQueue.poll();
			str = tmpFile.getName();;
			Extract obj = new Extract(tmpFile.getAbsolutePath(), scriptsDirOut +tmpFile.getParent().replace("D:", "")+File.separator+ str + ".xml",tag);
			executor.submit(obj);
		}
		//System.out.println("Here");
		executor.shutdown();
		try {
		  executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
		  System.out.println("Exception while bringing down executor service");
		}catch (Exception e) {
			// TODO: handle exception
		}
	}
	public static void main(String[] args) {
		long startTime= System.currentTimeMillis();
			extractFromScriptBatchList(OATSSOURCEDIRLIST,OATSSOURCEOUTPUTDIR,new String[]{"absoluteLocator='(.+?)'","text='(.+?)'","title='(.+?)'","value='(.+?)'","label='(.+?)'","popupTitle='(.+?)'","headerText='(.+?)'"});
		long stopTime= System.currentTimeMillis();
		System.out.println("Execution Time : "+(stopTime-startTime));
	}
}

