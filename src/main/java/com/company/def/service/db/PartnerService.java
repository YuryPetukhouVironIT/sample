package com.company.def.service.db;

import com.company.def.DBconnection;
import com.company.def.repository.PartnerRepository;
import com.company.def.servlets.admin.Partner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class PartnerService {

    @Autowired
    private PartnerRepository partnerRepository;

    public static Partner getPartnerByAPiKey(String apiKey) {
        return DBconnection.GetDBconnection().getPartnerByAPiKey(apiKey);
        //return partnerRepository.findByApiKey(apiKey);
    }

    public Partner getPartnerByName(String name) {
        return partnerRepository.findByName(name);
    }
}
