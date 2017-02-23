package oracle.apps.messages;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileReader;
import java.io.PrintWriter;

import java.util.ArrayList;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.NullEntityResolver;
import oracle.apps.utility.JoesBaseClass;

import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLDocument;
import oracle.xml.parser.v2.XMLElement;
import oracle.xml.parser.v2.XMLNode;

import org.w3c.dom.NodeList;

public class ExhaustiveSearch extends JoesBaseClass implements Runnable 
{

    static PrintWriter m_output;

    static final LinkedList<String> m_workQueue = new LinkedList<String>();
    static final ArrayList<Thread> m_workers = new ArrayList<Thread>();

    public static final HashSet<String> m_messageSet = new HashSet<String>();
    public static final HashSet<Pattern> m_messagePatternSet = new HashSet<Pattern>();

    static String m_phase;
    
    
    public ExhaustiveSearch(JoesBaseClass.CRAWL_TYPE crawL_TYPE)
    {
        super(crawL_TYPE);
    }
    
    protected void processFile(File fName, boolean bDoADE) 
    {
        String s = fName.getAbsolutePath();
        
        
        if(s.endsWith("MessageSD.xlf") || s.endsWith("MessageSD.xml"))
            return;
        
        if(s.contains("/fusionapps/translation/") || s.contains("/."))
            return;
        
        if(s.endsWith(".tar") || s.endsWith(".lck") || s.endsWith(".log") || s.endsWith(".xlsx") || s.endsWith(".xls") || s.endsWith(".jpg") || s.endsWith(".gif") || s.endsWith(".xdm") || s.endsWith(".a") || s.endsWith(".xlsm") || s.endsWith(".sso") || s.endsWith(".o") || s.endsWith(".adfbc_diagram") || s.endsWith(".ico") || s.endsWith(".tmpl") || s.endsWith(".def") || s.endsWith(".war") || s.endsWith(".so") || s.endsWith(".ear") || s.endsWith(".pdf") || s.endsWith(".png") || s.endsWith(".jar") || s.endsWith(".cs") || s.endsWith(".resx"))
            return;
        
        if(s.contains("/bin/linuxx64/") || s.contains("/deploy/") || s.contains("/build_metadata/"))
            return;
        
        // TODO:  NEED TO POSSIBLY ANALYZE THESE FILES
        if(s.endsWith(".xdmz") || s.endsWith(".xdoz") || s.endsWith(".mar") || s.endsWith(".zip"))
            return;
        

        long size = fName.length();
        if(size > 10000000)
        {
            System.out.println("Big file alert! (" + size + ") -->" + s + " -----   SKIPPING");
            return;
        }
        


        // Check if any of the messages are contained in this file.

        String content = readFileToString(s);
        
        if(content == null)
        {
            System.out.println("***********************************");
            System.out.println("Error Reading " + s);
            return;
        }
        

        if(s.endsWith(".java"))
        {
            content = removeJavaComments(content);
        }
        else if(s.endsWith(".xml") || s.endsWith(".jsff"))
        {
            content = removeHTMLComments(content);
        }
        else if(s.endsWith(".lpc") || s.endsWith(".opc") || s.endsWith(".lc"))
        {
            content = removeLpcComments(content);
        }
        else if(s.endsWith(".pkb") || s.endsWith(".pkh"))
        {
            content = removePlSQLComments(content);
        }


//        Iterator<Pattern> iter = m_messagePatternSet.iterator();
//        while(iter.hasNext()) 
//        {
//            Pattern p = iter.next();
//            Matcher m = p.matcher(content);
//
//            if(m.find())
//            {
//                synchronized(m_output)
//                {
//                    m_output.println(p.toString() + ", " + s);
//                    m_output.flush();
//                }
//                
//                System.out.println("***************************************************");
//                System.out.println("Found msg in file:");
//                System.out.println(p.toString());
//                System.out.println(s);
//            }
//        }

        

        Iterator<String> iter = m_messageSet.iterator();
        while(iter.hasNext()) 
        {
            String msg = iter.next();

            if(hasExactString(content, msg))
            {
                synchronized(m_output)
                {
                    m_output.println(msg + ", " + s);
                    m_output.flush();
                }
                
                int index = content.indexOf(msg);
                int start = index - 100;
                int end = index +100;
                if(start < 0)
                    start = 0;
                if(end > content.length())
                    end = content.length();
                
                System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
                System.out.println("Found msg in file:");
                System.out.println(msg);
                System.out.println(s);
                System.out.println(content.substring(start, end));
                System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
                
            }
        }
    }

    public boolean isMessageNameDelimiter(char c)
    {
        if(c >= 'a' && c <= 'z')
            return false;
        if(c >= 'A' && c <= 'Z')
            return false;
        if(c >= '0' && c <= '9')
            return false;
        if(c == '_' || c == '-')
            return false;
        
        return true;
    }

    public boolean hasExactString(String content, String s)
    {
        int index = 0;
        int length = s.length();
        int contentLength = content.length();
        
        while(index < contentLength && (index = content.indexOf(s, index)) > 0)
        {
            char a = content.charAt(index-1);
            char b = content.charAt(index+length);

            // advance index!
            index = index + length;
            
            if(isMessageNameDelimiter(a) && isMessageNameDelimiter(b))
                return true;
        }
        
        return false;
    }

    public void run()
    {
        String work = null;

        try
        {
            while(true)
            {
                
                synchronized(m_workQueue)
                {
                    while(m_workQueue.size() <= 0)
                        m_workQueue.wait();
                    
                    work = m_workQueue.remove();
                }

                if(m_phase.equals("SCANNING"))
                {
                    crawlDirectory(work, false);
                }
                else if(m_phase.equals("DONE"))
                {
                    return;
                }
            }

        }
        catch(Exception e)
        {
            e.printStackTrace();
            if(work != null)
                System.out.println("work is:  " + work);
            return;
        }
    }

    protected String getSummaryReport() 
    {
        return "TODO getSummaryReport()";
    }

    protected String getSummaryReportSubject() 
    {
        return "TODO getSummaryReportSubject()";
    }

    static final int NUM_THREADS = 50;

    private static void initThreadPool()
    {
        m_phase = "INIT";

        for(int i = 0; i < NUM_THREADS; i++)
        {
            ExhaustiveSearch pc = new ExhaustiveSearch(CRAWL_TYPE.ALL_FILES);
            Thread t = new Thread(pc);
            m_workers.add(t);
            
            t.start();
        }
    }
    
    private static void cleanupThreadPool()
    {
        m_phase = "DONE";

        for(int i = 0; i < NUM_THREADS; i++)
        {
            synchronized(m_workQueue)
            {
                m_workQueue.add("PLEASE FINISH");
                m_workQueue.notify();
            }
        }
        
    }


    private static void readSearchStrings() throws Exception
    {
        FileReader fr = new FileReader("/scratch/albowicz/analysis/unused_messages.csv");
        BufferedReader br = new BufferedReader(fr);
        
        String s;
        
        while((s = br.readLine()) != null)
        {
            s = s.trim();
            m_messageSet.add(s);
            
            String str = "[^\\w]" + s + "[^\\w]";
            Pattern p = Pattern.compile(str);

            m_messagePatternSet.add(p);
        }
    }

    public static void main(String[] args) 
    {
        try
        {
            m_output = new PrintWriter("/scratch/albowicz/analysis/exhaustive_search.txt");

            long start = System.currentTimeMillis();
            
            readSearchStrings();
            
            initThreadPool();
    
            doScanningPhase();
    
            cleanupThreadPool();
    
    
            long end = System.currentTimeMillis();
            double elasped = (end - start) / 60000.00;
            System.out.printf("Total Time is %.2f minutes.\n", elasped);

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
    }



    private static void doScanningPhase() throws Exception
    {
        long start = System.currentTimeMillis();

        String path = "/scratch/albowicz/view_storage/albowicz_joe/fusionapps/";

        m_phase = "SCANNING";

        synchronized(m_workQueue)
        {
            m_workQueue.add(path);
            m_workQueue.notify();
        }

        while(isWorkDone() == false)
        {
            Thread.sleep(10000);

            System.out.println("*********************************************************************************");
            System.out.println("Size of Queue is:  " + m_workQueue.size());
        }

        long end = System.currentTimeMillis();
        double elasped = (end - start) / 60000.00;
        System.out.printf("Scanning phase took %.2f minutes.\n", elasped);

    }

    protected void crawlDirectory(String path, boolean bDoADE)
    {
        if (path.contains(".ade_path") || path.contains("/classes/"))
            return;

        if(optionallySkipDirectory(path))
            return;

        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null)
            return;

        for (int i = 0; i < listOfFiles.length; i++)
        {
            if (listOfFiles[i].isFile())
            {
                String sName = listOfFiles[i].getAbsolutePath();
                // Zeesha: Added, because for files modified recently there is an .ade_path file created in that directory, that is neither node, nor dir. Skip this without raising error
                if (sName.contains(".ade_path"))
                    continue;

                if (fileOfInterest(sName))
                {
                    processFile(listOfFiles[i], bDoADE);
                }

                crawlPeek(listOfFiles[i]);
            } else if (listOfFiles[i].isDirectory())
            {
                // Added by zeesha - no point going into the directory if we are not interested in it. Better performance
                if (isDirOfInterest(listOfFiles[i].getAbsolutePath()))
                {
//                    crawlDirectory(listOfFiles[i].getAbsolutePath(), bDoADE);
                    synchronized(m_workQueue)
                    {
                        m_workQueue.add(listOfFiles[i].getAbsolutePath());
                        m_workQueue.notify();
                    }
                }
            } else
            {
                System.out.println("ERROR:  node is neither file or directory: " +
                                   listOfFiles[i]);
            }
        }

    }
    
    
    static private boolean isWorkDone()
    {
        for(int i = 0; i < m_workers.size(); i++)
        {
            Thread t = m_workers.get(i);
            
            Thread.State ts = t.getState();
            switch(ts)
            {
                case BLOCKED:
                    return false;
                case NEW:
                    return false;
                case RUNNABLE:
                    return false;
            }
        }
        
        
        System.out.println("All threads report work is done");

        if(m_workQueue.size() > 0)
            return false;

        return true;
    }


}
