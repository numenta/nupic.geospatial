#!/usr/bin/env python
# ----------------------------------------------------------------------
# Numenta Platform for Intelligent Computing (NuPIC)
# Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
# with Numenta, Inc., for a separate license for this software code, the
# following terms and conditions apply:
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License version 3 as
# published by the Free Software Foundation.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
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

from nupic.data.datasethelpers import findDataset
from nupic.frameworks.opf.modelfactory import ModelFactory

import model_params



DEFAULT_DATA_PATH = "data/commute.csv"
DEFAULT_OUTPUT_PATH = "anomaly_scores.csv"

ACCURACY_THRESHOLD = 80  # meters
INTERVAL_THRESHOLD = 30  # seconds



def createModel():
  return ModelFactory.create(model_params.MODEL_PARAMS)


def runGeospatialAnomaly(dataPath, outputPath):
  model = createModel()

  with open (findDataset(dataPath)) as fin:
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

    for _, record in enumerate(reader, start=1):
      timestamp = datetime.datetime.fromtimestamp(int(record[1]) / 1e3)
      longitude = float(record[2])
      latitude = float(record[3])
      speed = float(record[5])
      accuracy = float(record[7])

      if accuracy > ACCURACY_THRESHOLD:
        continue

      newSequence = False
      if lastTimestamp and (
        (timestamp - lastTimestamp).total_seconds() > INTERVAL_THRESHOLD):
        newSequence = True
      lastTimestamp = timestamp

      if newSequence:
        print "Starting new sequence..."
        model.resetSequenceStates()

      modelInput = {
        "vector": (longitude, latitude, speed)
      }
      result = model.run(modelInput)
      anomalyScore = result.inferences['anomalyScore']

      csvWriter.writerow([timestamp,
                          longitude,
                          latitude,
                          speed,
                          anomalyScore,
                          1 if newSequence else 0])

      print "[{0}] - Anomaly score: {1}.".format(timestamp, anomalyScore)

  print "Anomaly scores have been written to {0}".format(outputPath)



if __name__ == "__main__":
  dataPath = DEFAULT_DATA_PATH
  outputPath = DEFAULT_OUTPUT_PATH

  if len(sys.argv) > 1:
    dataPath = sys.argv[1]

  if len(sys.argv) > 2:
    outputPath = sys.argv[2]

  runGeospatialAnomaly(dataPath, outputPath)
