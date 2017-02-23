package oracle.apps.votuning_v3.file_type;

import java.util.ArrayList;
import java.util.HashMap;

public class AppModuleUsage {
  
  public static HashMap<String, ArrayList<AppModuleUsage>> m_subAM = new HashMap<String, ArrayList<AppModuleUsage>>();
  private String jar;
  private String parent_am;
  private String amUsage;
  private String amUsagePath;
  
  public AppModuleUsage(String jar, String am, String amUsage, String amUsagePath) {
    super();
    this.jar = jar;
    this.parent_am = am;
    this.amUsage = amUsage;
    this.amUsagePath = amUsagePath;
  }

  public static void setSubAM(HashMap<String, ArrayList<AppModuleUsage>> m_subAM) {
    AppModuleUsage.m_subAM = m_subAM;
  }

  public static HashMap<String, ArrayList<AppModuleUsage>> getSubAM() {
    return m_subAM;
  }

  public void setJar(String jar) {
    this.jar = jar;
  }

  public String getJar() {
    return jar;
  }

  public void setParent_am(String parent_am) {
    this.parent_am = parent_am;
  }

  public String getParent_am() {
    return parent_am;
  }

  public void setAmUsage(String amUsage) {
    this.amUsage = amUsage;
  }

  public String getAmUsage() {
    return amUsage;
  }

  public void setAmUsagePath(String amUsagePath) {
    this.amUsagePath = amUsagePath;
  }

  public String getAmUsagePath() {
    return amUsagePath;
  }
}
