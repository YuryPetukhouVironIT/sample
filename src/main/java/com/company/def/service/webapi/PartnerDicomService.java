package com.company.def.service.webapi;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface PartnerDicomService {
    void getStlFile(final HttpServletRequest request, HttpServletResponse response, final String patientId);

    void getStlZipFile(final HttpServletRequest request, HttpServletResponse response, final String patientId);

    void getVideosZipFile(final HttpServletRequest request, HttpServletResponse response, final String patientId);

    long checkDicom(final HttpServletRequest request);
}
