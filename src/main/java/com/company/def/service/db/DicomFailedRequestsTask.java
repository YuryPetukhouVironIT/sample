package com.company.def.service.db;

import com.company.def.funcclass;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DicomFailedRequestsTask {

    private static final Logger logger = LogManager.getLogger(DicomFailedRequestsTask.class);

//    @Scheduled(fixedDelay = 1000 * 60 * 60 * 24)
    public void handleRequests() {
        final List<String> failedDicomRequests = PatientService.getFailedRequestsJsons();
        for (String json : failedDicomRequests) {
            try (final CloseableHttpClient client = HttpClients.createDefault()) {
                final URI url = setTaskDoneUrl();
                final HttpPost post = new HttpPost();
                post.setURI(url);
                post.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
                post.setHeader("Content-Type", "application/json");
                final HttpResponse dicomTaskResponse = client.execute(post);
                final Boolean result = Boolean.valueOf(EntityUtils.toString(dicomTaskResponse.getEntity()));
                if (result) {
                }

            } catch (IOException e) {
                logger.error(e);
            } catch (URISyntaxException e) {
                logger.error(e);
            }
        }
    }

    private URI setTaskDoneUrl() throws URISyntaxException {
        return new URI(funcclass.baseUrl + "dicomTasks/set3DTaskDone");
    }
}
