package com.company.def.service.db;

import com.company.def.funcclass;
import com.company.def.model.DoctorLogin;
import com.company.def.repository.DoctorLoginRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DoctorLoginService {
    @Autowired
    private DoctorLoginRepository doctorLoginRepository;

    public int getLoginCountForLastVersion(long doctorId) {
        return doctorLoginRepository.getLoginCountWithVersion(doctorId, funcclass.versionNumber);
    }

    public int getTotalCountLogins(long doctorId) {
        return doctorLoginRepository.getTotalCountLogins(doctorId);
    }

    public void insertDoctorLogin(DoctorLogin doctorLogin) {
        doctorLoginRepository.insert(doctorLogin.getDoctorId(), doctorLogin.getCephxVersion(), doctorLogin.getIp());
    }

}
