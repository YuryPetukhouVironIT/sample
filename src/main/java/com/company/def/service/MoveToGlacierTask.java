package com.company.def.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.company.def.funcclass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MoveToGlacierTask implements Runnable {

    private static final Logger logger = LogManager.getLogger(MoveToGlacierTask.class);
    private final String s3Key;

    public MoveToGlacierTask (final String s3Key) {
        this.s3Key = s3Key;
    }

    @Override
    public void run() {
        logger.info("Moving dicom to glacier");
        final String bucketFrom = funcclass.getS3BucketName();
        final String bucketTo = funcclass.getGlacierBucketName();
        try {
            final ObjectListing objectListing = funcclass.s3clientNotEncrypted.listObjects(new ListObjectsRequest()
                .withBucketName(bucketFrom)
                .withPrefix(s3Key));
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                funcclass.s3clientNotEncrypted.copyObject(bucketFrom, objectSummary.getKey(),
                    bucketTo, objectSummary.getKey());
            }
            logger.info("Moved dicom to glacier");
        } catch (AmazonServiceException e)
        {
            logger.error(e);
        }
    }
}
