package oracle.apps.outputTextBinding;

import java.util.HashMap;
import java.util.HashSet;
import oracle.apps.helpers.JprHelper;
import oracle.apps.helpers.XMLParserHelper;
import oracle.apps.lovWidth.ViewObjectHelper;

import oracle.xml.parser.v2.XMLDocument;

public class ModifiedComboboxParser {

  String m_jprAbsFilePath = "";

  String m_jprName = "";

  HashMap<String, String> listOfAllVOs;    

  public void setJprName(String m_jprName) {
      this.m_jprName = m_jprName;
  }

  public String getJprName() {
      return m_jprName;
  }

  public void setJprAbsFilePath(String m_jprAbsFilePath) {
      this.m_jprAbsFilePath = m_jprAbsFilePath;
  }

  public String getJprAbsFilePath() {
      return m_jprAbsFilePath;
  }

  public String getAdeRelativePath(String absFilePath) {
      if (isEmpty(absFilePath)) {
          return absFilePath;
      }
      if (absFilePath.contains("/fusionapps/")) {
          int fusionappsInd = absFilePath.indexOf("/fusionapps/");
          return absFilePath.substring(fusionappsInd + 1);
      }
      return absFilePath;
  }
  

  public XMLDocument getPropertySetXml(String propertySetPackage) {

      JprHelper jprHelper = new JprHelper();
      HashMap<String, String> listOfPsInJpr = jprHelper.getListOfAllPropertySetsInJpr(m_jprAbsFilePath);


      XMLDocument psXml = null;
      if (listOfPsInJpr != null && listOfPsInJpr.containsKey(propertySetPackage)) {
          // va is in the same project
          String psabsPath = listOfPsInJpr.get(propertySetPackage);
          psXml = XMLParserHelper.getXMLDocument(psabsPath);
      } else {
          // not in the same project.. look at the library dependencies of this project..
          psXml = findBCFromProjectDependencies(m_jprAbsFilePath, propertySetPackage);
      }
      return psXml;
  }

  public String getAdeViewRoot(String path) {

      int fappsIndex = path.indexOf("/fusionapps");
      if (fappsIndex != -1) {
          return path.substring(0, fappsIndex);
      }


      return "";
  }

  public XMLDocument getVOXml(String voPackage) {
      if (listOfAllVOs.containsKey(voPackage)) {
          // va is in the same project
          String listVOAbsPath = listOfAllVOs.get(voPackage);
          return XMLParserHelper.getXMLDocument(listVOAbsPath);
      } else {
          // not in the same project.. look at the library dependencies of this project..
          return findBCFromProjectDependencies(m_jprAbsFilePath, voPackage);
      }
  }

  public XMLDocument findBCFromProjectDependencies(String jprFilePath, String bcPackageToFind) {
      XMLDocument bcXml = null;
      if (isEmpty(jprFilePath))
          return bcXml;
      if (!jprFilePath.endsWith(".jpr"))
          return bcXml;


      HashSet<String> adfLibrarys = JprHelper.getAllAdfLibrarysInJpr(jprFilePath, true);
      bcXml = ViewObjectHelper.findBCFromSetOfJarFiles(adfLibrarys, bcPackageToFind);
      if (bcXml != null)
          return bcXml;

      HashSet<String> adfbcImports = JprHelper.getAllBcImportsInJpr(jprFilePath, true);
      bcXml = ViewObjectHelper.findBCFromSetOfJarFiles(adfbcImports, bcPackageToFind);
      if (bcXml != null)
          return bcXml;
      // Also increase search paths, by looking in ADFLibrarydependencies file
      HashSet<String> secondaryDeps = JprHelper.getAllSecondaryDependencies(jprFilePath);
      bcXml = ViewObjectHelper.findBCFromSetOfJarFiles(secondaryDeps, bcPackageToFind);
      return bcXml;
  }

  private boolean isEmpty(String str) {
      if (str == null || str.trim().equals(""))
          return true;
      return false;
  }
}
