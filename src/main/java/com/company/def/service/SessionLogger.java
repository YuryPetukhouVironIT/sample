package com.cephx.def.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.Enumeration;
import java.util.Iterator;

@Component
public class SessionLogger {

    private static final Logger logger = LogManager.getLogger(SessionLogger.class);

    public void logSession (final String headMessage, final HttpSession session) {
//        final Enumeration<String> attributeNames= session.getAttributeNames();
//        logger.info(headMessage);
//        while (attributeNames.hasMoreElements()) {
//            final String attributeName  = attributeNames.nextElement();
//            logger.info("Session attribute: "+ attributeName + " ; Value: "+session.getAttribute(attributeName));
//        }
    }

}
