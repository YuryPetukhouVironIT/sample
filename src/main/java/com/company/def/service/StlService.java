package com.company.def.service;

import java.io.IOException;

public interface StlService {
    String jsonLocation(final Long patientId) throws Exception;

    boolean updateViewerSettings(final long patientId, final String updatedSettingsString) throws IOException;

    String getSavedViewerSettings(long patientId) throws IOException;
}
