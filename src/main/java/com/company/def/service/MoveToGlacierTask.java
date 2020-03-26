package com.cephx.def.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.cephx.def.funcclass;
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
        logger.info("move dicom to glacier");
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
//            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
//                funcclass.s3clientNotEncrypted.deleteObject(bucketFrom, objectSummary.getKey());
//            }
//            funcclass.s3clientNotEncrypted.copyObject(bucketFrom, s3Key, bucketTo, s3Key);
//            funcclass.s3clientNotEncrypted.deleteObject(bucketFrom, s3Key);
            logger.info("moved dicom to glacier");
        } catch (AmazonServiceException e)
        {
            logger.error(e);
        }
    }
}
