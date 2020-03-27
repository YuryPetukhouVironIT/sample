package com.company.def.service;

import com.company.def.funcclass;
import com.company.def.service.db.PatientService;
import net.swisstech.bitly.BitlyClient;
import net.swisstech.bitly.model.Response;
import net.swisstech.bitly.model.v3.ShortenResponse;
import org.springframework.stereotype.Service;

@Service
public class BitlyServiceImpl implements BitlyService {

    private static final BitlyClient client = new BitlyClient(funcclass.BITLY_API_KEY);


    @Override
    public void createShortenedLinks(final Long patientId) {
        final Response<ShortenResponse> readOnlyResponse = client.shorten()
            .setLongUrl(PatientService.shareLink(PatientService.stlJsonPath(patientId))+"&guid="+PatientService.stlReadOnlyGuid(patientId))
            .call();
        PatientService.createStlReadOnlyLink(patientId,readOnlyResponse.data.url);
        final Response<ShortenResponse> readWriteResponse = client.shorten()
            .setLongUrl(PatientService.shareLink(PatientService.stlJsonPath(patientId))+"&guid="+PatientService.stlReadWriteGuid(patientId))
            .call();
        PatientService.createStlReadWriteLink(patientId,readWriteResponse.data.url);

    }
}
