package com.cephx.def.service;

import com.cephx.def.funcclass;
import com.cephx.def.mail.mail.sender.templates.DefaultHTMLMessage;
import com.cephx.def.mail.mail.smtp.DelayedSMTPMailMessage;
import com.cephx.def.mail.mail.smtp.MailAddress;
import com.cephx.def.mail.mail.smtp.SMTPMailSender;
import com.cephx.def.mail.mail.smtp.type.HTMLTypeData;
import com.cephx.def.system;
import com.cephx.def.util.string.StringUtility;

import java.util.Vector;

public class MailService {

    public static synchronized void sendMail(String message, String address, String subject, boolean isReplaceAllContent) throws Exception {
        DelayedSMTPMailMessage msg = new DelayedSMTPMailMessage(
                funcclass.smtpHost, new MailAddress("support@cephx.com",
                "support@cephx.com"), new Vector(), subject, null);
        msg.addRecipient(new MailAddress(address, address));
        sendMail(msg, message,isReplaceAllContent);
    }

    public static synchronized void sendMailToCephxSupport(String message, String subject) throws Exception {
        DelayedSMTPMailMessage msg = new DelayedSMTPMailMessage(
                funcclass.smtpHost, new MailAddress("support@cephx.com",
                "support@cephx.com"), new Vector(), subject, null);

        sendMail(msg, message, false);
    }

    public static synchronized void sendMail(String vTo, String vFrom,
                                             String vSubject, String vMessage, boolean replaceContent) throws Exception {
        DelayedSMTPMailMessage msg = new DelayedSMTPMailMessage(
                funcclass.smtpHost, new MailAddress(vFrom, vFrom),
                new Vector(), vSubject, null);
        msg.addRecipient(new MailAddress(vTo, vTo));
        sendMail(msg, vMessage, replaceContent);
    }

    public static synchronized void sendMail(DelayedSMTPMailMessage msg,
                                             String message, boolean isReplaceAllContent) throws Exception {
        msg.setExtraData(new HTMLTypeData());

        String htmlMess = message;
        if (htmlMess.indexOf("<") == -1) {// text format -->convert toHTML
            htmlMess = StringUtility.replace(" " + message, "\n", " <BR> ");
            htmlMess = StringUtility.httpToAHref(htmlMess);
        }

        msg.setMessage(new DefaultHTMLMessage(htmlMess, msg.getSubject(), isReplaceAllContent)
                .toString());
        Vector res = msg.getRecipients();
        StringBuffer coll = new StringBuffer();
        for (int i = 0; i < res.size(); ++i)
            coll.append(((MailAddress) res.elementAt(i)).toString() + "+");
        system.log("\nsent mail to: " + coll.toString() + " titled: "
                + msg.getSubject() + "\n");
        sendDelayedMessage(msg);
    }

    private static synchronized void sendDelayedMessage(DelayedSMTPMailMessage vMessg) {
        try {
            SMTPMailSender.sendMail(vMessg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
