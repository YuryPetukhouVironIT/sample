package com.cephx.def.service;

public interface TokenService {

    String createWpToken();

    boolean checkWpToken(final String token);

    void removeWpToken(String authToken);

    String registerDoctor(String redirectUrl) throws Exception;

    String createViewerToken();

    boolean checkViewerToken(final String token);
}
