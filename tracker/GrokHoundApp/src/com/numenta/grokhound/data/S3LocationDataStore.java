/**
 *
 * Copyright (C) 2014 Numenta Inc. All rights reserved.
 *
 * The information and source code contained herein is the
 * exclusive property of Numenta Inc.  No part of this software
 * may be used, reproduced, stored or distributed in any form,
 * without explicit written authorization from Numenta Inc.
 *
 */

package com.numenta.grokhound.data;

import android.location.Location;
import android.os.Build;
import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.numenta.grokhound.GrokApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

public class S3LocationDataStore implements LocationDataStore {
    private static final String TAG = S3LocationDataStore.class.getSimpleName();
    private String _awsDataBucket;
    private String _awsDataFolder;
    private String _awsAccessKey;
    private String _awsSecretKey;
    private AmazonS3Client _s3Client;
    private String _s3Folder;

    public S3LocationDataStore(String awsDataBucket, String awsDataFolder,
            String awsAccessKey, String awsSecretKey) {
        super();
        this._awsDataBucket = awsDataBucket;
        this._awsDataFolder = awsDataFolder;
        this._awsAccessKey = awsAccessKey;
        this._awsSecretKey = awsSecretKey;
        _s3Client = new AmazonS3Client(new BasicAWSCredentials(_awsAccessKey,
                _awsSecretKey));
        _s3Folder = _awsDataFolder + "/" + Build.SERIAL + "/";
    }

    @Override
    public void save(Collection<Location> locations) throws IOException {
        if (locations == null || locations.isEmpty()) {
            Log.w(TAG, "Nothing to save");
            // Nothing to save
            return;
        }
        Log.d(TAG, "Saving " + locations.size() + " locations");

        // Convert location Array to CSV
        StringBuilder data = new StringBuilder();

        // CSV Header
//        data.append("device").append(",").append("time").append(",")
//                .append("longitude").append(",")
//                .append("latitude").append(",").append("altitude").append(",")
//                .append("speed").append(",").append("bearing").append(",")
//                .append("accuracy").append("\n");

        // CSV Data
        for (Location loc : locations) {
            data.append(Build.SERIAL).append(",").append(loc.getTime()).append(",")
                    .append(loc.getLongitude())
                    .append(",").append(loc.getLatitude()).append(",")
                    .append(loc.getAltitude()).append(",")
                    .append(loc.getSpeed()).append(",")
                    .append(loc.getBearing()).append(",")
                    .append(loc.getAccuracy()).append("\n");
        }
        try {
            // Create S3 file name : S3_BUCKET/S3_DATA_FOLDER/SERIAL/TIME.CSV
            String fileName = System.currentTimeMillis() + ".csv";
            byte bytes[] = data.toString().getBytes("UTF-8");

            // Cache new data
            File cacheDir = GrokApplication.getInstance().getCacheDir();
            FileOutputStream fos = new FileOutputStream(new File(cacheDir,
                    fileName));
            fos.write(bytes);
            fos.close();

            // Try to upload all cached files
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("text/csv");
            for (File file : cacheDir.listFiles()) {
                fileName = file.getName();
                if (fileName.endsWith(".csv")) {
                    // Create put object request
                    PutObjectRequest por = new PutObjectRequest(_awsDataBucket, _s3Folder
                            + fileName, file);
                    por.setMetadata(metadata);
                    _s3Client.putObject(por);

                    // Remove from cache
                    file.delete();
                }
            }
        } catch (AmazonClientException e) {
            throw new IOException(e);
        }
    }
}
