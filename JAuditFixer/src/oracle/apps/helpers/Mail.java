package oracle.apps.helpers;

import java.io.PrintWriter;
import java.io.StringWriter;


import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Properties;

import javax.mail.*;
import javax.mail.internet.*;
import javax.net.ssl.*;


public final class Mail {
    /**
     * This class should not be instantiated anywhere.
     */  
    private Mail() { }
    
    /**
     * Sends a piece of mail
     *
     * @param replyToAddress The sender's email address
     * @param message The email message
     * @param subject The email subject
     */
    public static void sendMail(String replyToAddress, String message, String subject) {
        if (replyToAddress == null || replyToAddress.length() == 0) return;
        
        try {
            // Format example: [Tue, May. 20, 08] [16:55:29.804] -0700
            SimpleDateFormat dateFormat = new SimpleDateFormat("[EEE, MMM. dd, yy] [HH:mm:ss.SSS] Z");
            String timeSent = dateFormat.format(Calendar.getInstance().getTime());          
    
            // Create a header with some possibly-useful information
            StringBuffer messageHeader = new StringBuffer();
            messageHeader.append("\n------------------------------------------------------------------------");
            messageHeader.append("\nSender email address: " + replyToAddress);
            messageHeader.append("\nTime sent: " + timeSent);
            messageHeader.append("\n");
            messageHeader.append("\njava.runtime.name: [" + System.getProperty("java.runtime.name") + "]");
            messageHeader.append("\njava.version: [" + System.getProperty("java.version") + "]");
            messageHeader.append("\njava.vm.version: [" + System.getProperty("java.vm.version") + "]");
            messageHeader.append("\njava.vm.name: [" + System.getProperty("java.vm.name") + "]");
            messageHeader.append("\njava.vm.vendor: [" + System.getProperty("java.vm.vendor") + "]");
            messageHeader.append("\nos.arch: [" + System.getProperty("os.arch") + "]");
            messageHeader.append("\nos.name: [" + System.getProperty("os.name") + "]");
            messageHeader.append("\nos.version: [" + System.getProperty("os.version") + "]");
            messageHeader.append("\nuser.name: [" + System.getProperty("user.name") + "]");
            messageHeader.append("\n------------------------------------------------------------------------");
            messageHeader.append("\n");
            messageHeader.append("\n___Message___");
            messageHeader.append("\n");
            
            // Set the SMTP server properties
            Properties properties = new Properties();
           // properties.put("mail.smtp.host", "mail.oracle.com");                  // This may not work after a while.  The new SMTP host is stbeehive.oracle.com, port 465, with SSL authentication
            properties.put("mail.smtp.host", "localhost"); //Use the local SMTP server to send mail...
            //properties.put("mail.smtp.auth","true");
            //properties.put("mail.smtp.port", "465"); //By default, JDeveloper does not allow you to edit the jpr and jws files. This can be achieved using the "External Tools" feature in the IDE. You can register your favorite c", "465");
            //properties.put("mail.smtp.socketFactory.port","465");
            //properties.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
            //properties.put("mail.smtp.ssl.trust", "*");
            //properties.put("mail.smtp.starttls.enable", "true");
// properties.put("", "");            
            // Set up authentication property
            
            Session session = Session.getDefaultInstance(properties);
            //session.setDebug(true);
    
            Message email = new MimeMessage(session);
          
            String[] recipientsList = replyToAddress.split(",");

            for(int i=0; i<recipientsList.length; i++){
              String tmpRecipient = recipientsList[i].trim();
              recipientsList[i] = tmpRecipient;
              if(tmpRecipient != null && tmpRecipient.length() > 0) {
                  try {
                      InternetAddress replyTo = new InternetAddress(tmpRecipient);
//                      email.setReplyTo(new InternetAddress[] { replyTo });
                      email.addRecipient(Message.RecipientType.TO, replyTo);
                  } catch(AddressException ae) {
                      // Invalid reply-to email address
                      System.out.println("Invalid email address :"+replyToAddress);
                  }
              }
            }
          
            String sender = recipientsList[0];
            email.setFrom(new InternetAddress(sender, sender));
            //email.setRecipient(Message.RecipientType.TO, new InternetAddress("joseph.albowicz@oracle.com", "Joe Albowicz"));
//            email.setRecipient(Message.RecipientType.TO, new InternetAddress(replyToAddress, replyToAddress));
            //email.addRecipient(Message.RecipientType.TO, new InternetAddress("zeesha.currimbhoy@oracle.com", "Zeesha Currimbhoy"));
            //email.addRecipient(Message.RecipientType.TO, new InternetAddress("angel.irizarry@oracle.com", "Angel Irizarry"));
            //email.addRecipient(Message.RecipientType.TO, new InternetAddress("sudipti.gupta@oracle.com", "Sudipti Gupta"));
            email.setSubject(subject);
            email.setText(messageHeader.toString() + message);
            
            Transport.send(email);
        } catch (Exception e) {
            e.printStackTrace();
             //if(Repository.getConfig().check(GlobalConfig.DBG_ON)) { Repository.dbg().err("Mail.sendMail()", "Error while trying to send a piece of mail: " + ModelUtilities.convertThrowableToString(throwable)); }
        }
    }

    /**
     * Sends an error message via email
     *
     * @param errorMessage A description of the error
     * @param throwable An throwable object that will be inspected and displayed in the email
     */
    public static void sendError(String location, String errorMessage, Throwable throwable) {
        StringBuffer message = new StringBuffer(errorMessage != null ? errorMessage + "\n\n" : ""); // Put the error message first if there is one
        message.append(convertThrowableToString(throwable));
        
        sendMail("ApplyComponentScript Error - " + location, message.toString(), "ApplyComponentScript Error");          
    }
    

    public static String convertThrowableToString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }
}

