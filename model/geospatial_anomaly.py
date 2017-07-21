#!/usr/bin/env python
# ----------------------------------------------------------------------
# Numenta Platform for Intelligent Computing (NuPIC)
# Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
# with Numenta, Inc., for a separate license for this software code, the
# following terms and conditions apply:
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero Public License version 3 as
# published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU Affero Public License for more details.
#
# You should have received a copy of the GNU Affero Public License
# along with this program.  If not, see http://www.gnu.org/licenses.
#
# http://numenta.org/licenses/
# ----------------------------------------------------------------------

"""
A simple client to create a CLA anomaly detection model for geospatial data.
"""

import csv
import datetime
import sys

from nupic.frameworks.opf.model_factory import ModelFactory

import model_params



DEFAULT_DATA_PATH = "data/commute.csv"
DEFAULT_OUTPUT_PATH = "anomaly_scores.csv"

ACCURACY_THRESHOLD = 80  # meters
INTERVAL_THRESHOLD = 60 * 30  # seconds



def addTimeEncoders(params):
  params["modelParams"]["sensorParams"]["encoders"]["timestamp_timeOfDay"] = {
    "fieldname": u"timestamp",
    "name": u"timestamp_timeOfDay",
    "timeOfDay": (51, 9.5),
    "type": "DateEncoder"
  }
  return params



def setEncoderScale(params, scale):
  params["modelParams"]["sensorParams"]["encoders"]["vector"]["scale"] = \
    int(scale)
  return params



def createModel(useTimeEncoders, scale, verbose):
  params = model_params.MODEL_PARAMS
  if useTimeEncoders:
    params = addTimeEncoders(params)
  if scale:
    params = setEncoderScale(params, scale)
  if verbose:
    print "Model parameters:"
    print params
  model = ModelFactory.create(params)
  model.enableInference({"predictedField": "vector"})
  return model



def runGeospatialAnomaly(dataPath, outputPath,
                         scale=False,
                         autoSequence=True,
                         useTimeEncoders=False,
                         verbose=False):

  model = createModel(useTimeEncoders, scale, verbose)

  tracksProcessed = 0

  with open (dataPath) as fin:
    reader = csv.reader(fin)
    csvWriter = csv.writer(open(outputPath,"wb"))
    csvWriter.writerow(["timestamp",
                       "longitude",
                       "latitude",
                       "speed",
                       "anomaly_score",
                       "new_sequence"])

    reader.next()
    reader.next()
    reader.next()

    lastTimestamp = None
    lastTrackName = None
    outputFormat = "%Y-%m-%dT%H:%M:%S"

    for _, record in enumerate(reader, start=1):
      trackName = record[0]
      timestamp = datetime.datetime.fromtimestamp(int(record[1]) / 1e3)
      longitude = float(record[2])
      latitude = float(record[3])
      speed = float(record[5])
      accuracy = float(record[7])

      altitude = float(record[4]) if record[4] != "" else None

      if accuracy > ACCURACY_THRESHOLD:
        continue

      newSequence = False
      # Handle the automatic sequence creation
      if autoSequence:
        if lastTimestamp and (
          (timestamp - lastTimestamp).total_seconds() > INTERVAL_THRESHOLD):
          newSequence = True
      # Manual sequence resets depend on the track name
      else:
        if trackName != lastTrackName:
          newSequence = True

      lastTimestamp = timestamp
      lastTrackName = trackName

      if newSequence:
        tracksProcessed += 1
        if verbose:
          print "Starting new sequence (#{})...".format(tracksProcessed)
        model.resetSequenceStates()

      modelInput = {
        "vector": (speed, longitude, latitude, altitude)
      }

      if useTimeEncoders:
        modelInput["timestamp"] = timestamp

      result = model.run(modelInput)
      anomalyScore = result.inferences["anomalyScore"]

      csvWriter.writerow([timestamp.strftime(outputFormat),
                          longitude,
                          latitude,
                          speed,
                          anomalyScore,
                          1 if newSequence else 0])
      if verbose:
        print "[{0}] - Anomaly score: {1}.".format(timestamp, anomalyScore)

      if tracksProcessed > 20: break

  print "Anomaly scores have been written to {0}".format(outputPath)



if __name__ == "__main__":
  dataPath = DEFAULT_DATA_PATH
  outputPath = DEFAULT_OUTPUT_PATH

  if len(sys.argv) > 1:
    dataPath = sys.argv[1]

  if len(sys.argv) > 2:
    outputPath = sys.argv[2]

  runGeospatialAnomaly(dataPath, outputPath)
