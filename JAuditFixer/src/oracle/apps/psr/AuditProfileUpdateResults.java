package oracle.apps.psr;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import java.io.IOException;

import oracle.apps.helpers.Mail;

public class AuditProfileUpdateResults {

  static String emailAddress =
    "mengjun.xu@oracle.com,fusion_premerge_support_ww_grp@oracle.com";


  public static void main(String[] args) throws FileNotFoundException,
                                                IOException {
    String txnDesFile = "";
    String txnDiffFile = "";
    String mailContent = "";

    if (args != null && args.length == 2) {
      txnDesFile = args[0];
      txnDiffFile = args[1];

      BufferedReader br = new BufferedReader(new FileReader(txnDesFile));
      String line;
      while ((line = br.readLine()) != null) {
        mailContent += line + "\n";
      }
      
      mailContent += "\n";
      
      BufferedReader br2 = new BufferedReader(new FileReader(txnDiffFile));
      String line2;
      while ((line2 = br2.readLine()) != null) {
        mailContent += line2 + "\n";
      }

      Mail.sendMail(emailAddress, mailContent,
                    "Automatic JAudit Daily Update");
    } else {
      Mail.sendMail(emailAddress, "No updates today.",
                    "Automatic JAudit Daily Update (No updates)");
    }
  }
}
