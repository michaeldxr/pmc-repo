package oracle.apps.lovWidth;

public class MappingHelper {
    public MappingHelper() {
        super();
    }

  public static String getFamilyForPackage(String packageName) {
      if (isEmpty(packageName))
          return "";

      if (packageName.startsWith("oracle.apps.financials"))
          return "fin";
      if (packageName.startsWith("oracle.apps.setup") || packageName.startsWith("oracle.apps.functionalCore"))
          return "fsm";
      if (packageName.startsWith("oracle.apps.hcm"))
          return "hcm";
      if (packageName.startsWith("oracle.apps.crmCommon"))
          return "crmCommon";
      if (packageName.startsWith("oracle.apps.incentiveCompensation"))
          return "ic";
      if (packageName.startsWith("oracle.apps.projects"))
          return "prj";
      else if (packageName.contains("oracle.apps.")) {
          int appsInd = packageName.indexOf("oracle.apps.");
          appsInd = appsInd + "oracle.apps.".length();
          int nextDotIndex = packageName.indexOf(".", appsInd);
          if (nextDotIndex != -1) {
              return packageName.substring(appsInd, nextDotIndex);
          }
      }
      return "unknown";
  }

  public static String getProductForPackage(String packageName) {
      String family = getFamilyForPackage(packageName);
      if (isEmpty(family))
          return "unknown";
      int indexOfProduct = 3;
      if (family.equals("fin")) {
          indexOfProduct = 3;

      }

      String[] packageParts = packageName.split("\\.");
      if (packageParts == null || packageParts.length - 1 < indexOfProduct)
          return "";
      String productStr = packageParts[indexOfProduct];
      return getProductShortCode(productStr);

  }

  public static String getProductShortCode(String fullProductName) {
      String productName = fullProductName;
      if (fullProductName.equals("payables"))
          productName = "AP";
      else if (fullProductName.equals("receivables"))
          productName = "AR";
      else if (fullProductName.equals("generalLedger"))
          productName = "GL";
      else if (fullProductName.equals("financialCommon"))
          productName = "Fun";
      else if (fullProductName.equals("commonModules"))
          productName = "Fun";
      else if (fullProductName.equals("subledgerAccounting"))
          productName = "XLA";
      else if (fullProductName.equals("payments"))
          productName = "IBY";
      else if (fullProductName.equals("tax"))
          productName = "ZX";
      else if (fullProductName.equals("cashManagement"))
          productName = "CE";
      else if (fullProductName.equals("assets"))
          productName = "FA";
      else if (fullProductName.equals("expenses"))
          productName = "EXM";
      else if (fullProductName.equals("collections"))
          productName = "IEX";
      else if (fullProductName.equals("legalEntity"))
          productName = "XLE";
      else if (fullProductName.equals("europeanLocalizations"))
          productName = "JE";
      else if (fullProductName.equals("commitmentControl"))
          productName = "CC";

      return productName;
  }

  public static boolean doesPackageBelongToFamily(String packageName, String family) {
      if (isEmpty(packageName) || isEmpty(family))
          return false;
      if (family.trim().equals("all"))
          return true;
      String familyForPkg = getFamilyForPackage(packageName);
      if (!isEmpty(familyForPkg) && familyForPkg.equals(family))
          return true;
      return false;
  }

  public static boolean isEmpty(String str) {

      if (str == null || str.trim().equals(""))
          return true;
      return false;
  }
}
