package com.company.def.service.webapi;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface PartnerPatientService {

    String downloadPage (final HttpServletRequest request);

    void passInputParameters(final HttpServletRequest request, final HttpServletResponse response) throws Exception;

    long checkPatient(final HttpServletRequest request);

    void checkIfPatientExists(final HttpServletRequest request);
}
