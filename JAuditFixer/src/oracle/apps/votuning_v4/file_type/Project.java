package oracle.apps.votuning_v4.file_type;

import java.io.Serializable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oracle.apps.helpers.XMLParserHelper;

import oracle.xml.parser.v2.XMLDocument;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class Project{
 
  public static HashMap<String, String> adePathMapping =
    new HashMap<String, String>();
  public static HashSet<String> s_jlib = new HashSet<String>();

  public Project() {
    super();
  }

  public static void processFile(String path) {
    String jar_path;

    try {
      XMLDocument doc = XMLParserHelper.getXMLDocument(path);

      NodeList nl = doc.getElementsByTagName("url");
      int length = nl.getLength();

      for (int i = 0; i < length; i++) {
        Node n = nl.item(i);
        Node jarURL = n.getAttributes().getNamedItem("n");

        if (jarURL != null && jarURL.getNodeValue().equals("jarURL")) {
          jar_path = n.getAttributes().getNamedItem("path").getNodeValue();
          if (jar_path.endsWith(".jar") && jar_path.contains("jlib")) {
            jar_path = getAbsoluteADEPath(jar_path, path);
            adePathMapping.put(jar_path, getSrcPath(path));
            s_jlib.add(getJlibDir(jar_path));
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Skipping this file....");
    }
  }

  private static String getAbsoluteADEPath(String relativePath,
                                           String jprPath) {
    String absPath = "";
    int dirLevel = 1;
    String prefix = "";

    String[] s1 = relativePath.split("/");
    for (String s : s1) {
      if (s.equals(".."))
        dirLevel++;
      else {
        if (s.endsWith("jar"))
          absPath += s;
        else
          absPath += s + "/";
      }
    }

    s1 = jprPath.split("/");

    int offset = s1.length - dirLevel;

    for (int i = 0; i < offset; i++) {
      prefix += s1[i] + "/";
    }

    absPath = prefix + absPath;

    return absPath;
  }

  private static String getSrcPath(String jprPath) {
    Pattern p = Pattern.compile("(.+)/(?:.+\\.jpr)");
    Matcher m = p.matcher(jprPath);
    if (m.matches()) {
      return m.group(1);
    }

    return null;
  }

  private static String getJlibDir(String path) {
    Pattern p = Pattern.compile("(.+/jlib)/(?:.+\\.jar)");
    Matcher m = p.matcher(path);
    if (m.matches()) {
      return m.group(1);
    }

    return null;
  }

}
