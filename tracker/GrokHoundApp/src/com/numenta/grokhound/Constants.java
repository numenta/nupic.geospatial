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
package com.numenta.grokhound;

public interface Constants {
/*
// AWS S3 bucket Policy for "grok.apps.data"
{
	"Version": "2008-10-17",
	"Id": "Policy1400284919423",
	"Statement": [
		{
			"Sid": "Stmt1400284913623",
			"Effect": "Allow",
			"Principal": {
				"AWS": "arn:aws:iam::783782770022:user/kidtracker_s3uploadonly"
			},
			"Action": "s3:PutObject",
			"Resource": "arn:aws:s3:::grok.apps.data/*"
		}
	]
}
 */
	// S3 Bucket for 
	public static final String S3_DATA_BUCKET = "grok.apps.data";
	public static final String S3_DATA_FOLDER = "com.numenta.grokhound";
	public static final String AWS_ACCESS_KEY = "AKIAJBLE2HWSRZG5ZTZQ";
	public static final String AWS_SECRET_KEY = "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";
}
