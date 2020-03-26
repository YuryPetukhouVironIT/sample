package com.cephx.def.controller.webapi;

import com.cephx.def.service.StlService;
import com.cephx.def.service.db.PatientService;
import com.cephx.def.service.webapi.PartnerDicomService;
import com.cephx.def.service.webapi.PartnerPatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/webapi/dicom")
public class PartnerDicomController {

    @Autowired
    private PartnerPatientService partnerPatientService;
    @Autowired
    private PartnerDicomService dicomService;
    @Autowired
    private StlService stlService;

    @RequestMapping("/downloadSTL")
    public void downloadSTLAnalysis(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        partnerPatientService.passInputParameters(request, response);
        long patientId = dicomService.checkDicom(request);
        dicomService.getStlFile(request, response, String.valueOf(patientId));
    }

    @RequestMapping("/downloadSTLZIP")
    public void downloadSTLZIPAnalysis(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        partnerPatientService.passInputParameters(request, response);
        long patientId = dicomService.checkDicom(request);
        dicomService.getStlZipFile(request, response, String.valueOf(patientId));
    }

    @RequestMapping("/downloadVideo")
    public void downloadVideoAnalysis(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        partnerPatientService.passInputParameters(request, response);
        long patientId = dicomService.checkDicom(request);
        dicomService.getVideosZipFile(request, response, String.valueOf(patientId));
    }

    @RequestMapping("/stlViewer")
    public void stlViewer(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        partnerPatientService.passInputParameters(request, response);
        long patientId = dicomService.checkDicom(request);
        final String replyPage = PatientService.shareLink(stlService.jsonLocation(patientId));
        response.sendRedirect(replyPage);
    }

}
