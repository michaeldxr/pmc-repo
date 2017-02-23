package oracle.apps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;

import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.utility.JoesBaseClass;


public class OverlappingJarContentReporter extends JoesBaseClass {

    private class JarContent {
        public String path;
        public List<String> owners;

        public JarContent(String path, String owner) {
            this.path = path;
            owners = new ArrayList<String>();
            owners.add(owner);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof JarContent) {
                JarContent o = (JarContent)obj;
                return path.equals(o.path);
            }
            else {
                return false;
            }
        }

        @Override
        public String toString() {
            return "[Jar Content:'" + path + "']";
        }
    }

    private static final Pattern DFLT_PCKG_REGEX = Pattern.compile("<value\\s+n=\"defaultPackage\"\\s+v=\"([\\w\\.]+)?\"\\s*/>");
    private static final Pattern JAR_REL_PATH_REGEX = Pattern.compile("<url\\s+n=\"jarURL\"\\s+path=\"([\\w/\\{\\}\\.\\$]+?)\"\\s*/>");

    private int numJarsFound = 0;
    private int numJarsNotFound = 0;

    private List<JarContent> filesSeen;
    private List<JarContent> exceptions = new ArrayList<JarContent>();

    public OverlappingJarContentReporter() {
        super(CRAWL_TYPE.JPR);
        filesSeen = new ArrayList<JarContent>();
        buildExceptionsList();
    }

    private boolean isException(JarContent jc) {
        if (exceptions.contains(jc))
            return true;
        if (jc.path.startsWith("META-INF/"))
            return true;
        if (jc.path.contains(".ade_path"))
            return true;
        if (jc.path.endsWith("/"))
            return true;

        return false;
    }

    public void start(String crawlDirectory) {
        System.out.println("Analyzing jars ==========================================");
        crawlDirectory(crawlDirectory, false);


        try {
            Process p = Runtime.getRuntime().exec("rm -f overlapping_content.csv");
            p.waitFor();

            BufferedWriter bundleTypesBW = new BufferedWriter(new FileWriter("overlapping_content.csv"));
            StringBuilder sb = new StringBuilder();
            sb.append("Content, Content Owners\n");


            for (JarContent jc : filesSeen) {
                if (jc.owners.size() > 1) {
                    sb.append(jc.path + ", ");
                    for (String owner : jc.owners) {
                        sb.append(owner + ", ");
                    }
                    sb.append("\n");
                }
            }

            bundleTypesBW.write(sb.toString());
            bundleTypesBW.close();
        }
        catch (Exception e) {
            System.out.println("ERROR: Could not create CSV. Perhaps you are in a read-only file system.");
        }
    }

    private void buildExceptionsList() {
        exceptions.add(new JarContent("WEB-INF/orion-web.xml", null));
        exceptions.add(new JarContent("WEB-INF/weblogic.xml", null));
        exceptions.add(new JarContent("WEB-INF/portlet.xml", null));
        exceptions.add(new JarContent("WEB-INF/adfdi-client-registry.xml",
                                      null));
        exceptions.add(new JarContent("adflibREADME.txt", null));
        exceptions.add(new JarContent("login.html", null));
        exceptions.add(new JarContent("error.html", null));
    }

    protected void processFile(File jpr, boolean bDoADE) {
        if (jpr.getName().endsWith("Test.jpr") || jpr.getName().endsWith("DbSchema.jpr"))
            return;
        File jar = null;

        System.out.println("INFO: Reviewing jpr: " + jpr.getName());

        try {
            String jprString = convertFileToString(jpr);

            String jarRelPath = null;
            Matcher m1 = JAR_REL_PATH_REGEX.matcher(jprString);
            boolean jarFound = false;
            boolean deploymentFound = false;
            while (m1.find()) {
                deploymentFound = true;
                jarRelPath = m1.group(1);
                if (jarRelPath.endsWith(".jar")) {
                    jarFound = true;
                    break;
                }
            }

            if (jarFound) {
                jarRelPath = Pattern.compile("\\$\\{.*?\\}").matcher(jarRelPath).replaceAll("");
                jar = new File(jpr.getParentFile().getAbsolutePath() + "/" + jarRelPath);
                if (jar.exists())
                    System.out.println("INFO: ...JAR path: " + jar.getCanonicalPath());
                else {
                    System.out.println("WARNING: ...JAR not found. Skipping.");
                    numJarsNotFound++;
                    return;
                }
            }
            else if (deploymentFound) {
                System.out.println("INFO: ...JPR does not deploy to a JAR. Skipping.");
                return;
            }
            else {
                System.out.println("WARNING: ...JAR not found. Skipping.");
                numJarsNotFound++;
                return;
            }

            String defaultPackage = null;
            Matcher m2 = DFLT_PCKG_REGEX.matcher(jprString);
            if (m2.find()) {
                defaultPackage = m2.group(1);
                System.out.println("INFO: ...Default package: " + defaultPackage);
            }

            JarFile jarFile = new JarFile(jar.getAbsolutePath());
            Enumeration<JarEntry> contents = jarFile.entries();
            List<JarContent> badContent = new ArrayList<JarContent>();
            while (contents.hasMoreElements()) {
                JarEntry f = contents.nextElement();
                JarContent jc = new JarContent(f.getName(), jar.getName());

                if (!isException(jc)) {
                    if (f.getName().endsWith(".class") && defaultPackage != null && !getFQN(f).startsWith(defaultPackage)) {
                        // Files that do not belong in jar because they do not match the default package.
                        badContent.add(jc);
                    }

                    int i = filesSeen.indexOf(jc);
                    if (i > -1) {
                        filesSeen.get(i).owners.add(jar.getName());
                    }
                    else {
                        filesSeen.add(jc);
                    }
                }
            }

            if (badContent.size() > 0) {
                System.out.println("INFO: ...Bad Content (not in default package):");
                for (JarContent jc : badContent) {
                    System.out.println("INFO: ......" + jc);
                }
            }
        }
        catch (Exception e) {

            e.printStackTrace();
        }

        numJarsFound++;
    }

    private String getFQN(JarEntry file) {
        String path = file.getName();
        int i = path.indexOf("oracle");
        if (i > -1) {
            return path.substring(i).replace('/', '.');
        }
        else
            return "";
    }

    protected String getSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("------------------ Summary Report ------------------");
        sb.append("\nJars analyzed:         " + numJarsFound);
        sb.append("\nJars not found:        " + numJarsNotFound);
        sb.append("\n");
        return sb.toString();
    }

    protected String getSummaryReportSubject() {
        return "SCRIPT: Overlapping Jar Content";
    }


    protected String convertFileToString(File f) throws Exception {
        File fp = new File(f.getAbsolutePath());
        FileReader reader = new FileReader(fp);
        BufferedReader input = new BufferedReader(reader);

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = input.readLine()) != null) {
            sb.append(line);
            sb.append("\n");
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("USAGE: findOverlappingJarContent.sh");
            System.exit(1);
        }

        String crawlDirectory = args[0];

        OverlappingJarContentReporter x = new OverlappingJarContentReporter();
        x.start(crawlDirectory);

        System.out.println(x.getSummaryReport());
    }
}
