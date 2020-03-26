package com.cephx.def.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

@Service
public class IPService {
    public String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");
        if (StringUtils.isEmpty(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        } else {
            int commaIndex = ipAddress.indexOf(",");
            if (commaIndex > 0) {
                ipAddress = ipAddress.substring(0, commaIndex);
            }
        }
        return ipAddress;
    }
}
