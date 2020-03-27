package com.company.def.service.webapi;

import com.company.def.exceptions.WebApiException;
import com.company.def.service.AnalysisService;
import com.company.def.service.db.PatientService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Service
public class PartnerDicomServiceImpl implements PartnerDicomService {

    @Autowired
    private PartnerPatientService partnerPatientService;
    @Autowired
    private AnalysisService analysisService;

    @Override
    public void getStlFile(final HttpServletRequest request, final HttpServletResponse response, final String patientId) {
        analysisService.getStlFile(request, response, patientId);
    }

    @Override
    public void getStlZipFile(final HttpServletRequest request, final HttpServletResponse response, final String patientId) {
        analysisService.getStlZipFile(request, response, patientId);
    }

    @Override
    public void getVideosZipFile(final HttpServletRequest request, final HttpServletResponse response, final String patientId) {
        analysisService.getVideosZipFile(request, response, patientId);
    }

    @Override
    public long checkDicom(final HttpServletRequest request) {
        final long patientId = partnerPatientService.checkPatient(request);
        if (StringUtils.isBlank(PatientService.getStlPath(patientId))) {
            throw new WebApiException("");
        }
        return patientId;
    }

}
