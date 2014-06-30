#!/usr/bin/env python
# ----------------------------------------------------------------------
# Numenta Platform for Intelligent Computing (NuPIC)
# Copyright (C) 2014, Numenta, Inc.  Unless you have purchased from
# Numenta, Inc. a separate commercial license for this software code, the
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

import csv
import sys



def run(dataPath):
  with open(dataPath) as csvfile:
    reader = csv.reader(csvfile)

    print "DATA = ["
    reader.next()  # header

    for row in reader:
      timestamp = row[0]
      longitude = float(row[1])
      latitude = float(row[2])
      speed = float(row[3])
      anomalyScore = float(row[4])
      newSequence = "true" if int(row[5]) else "false"
      print "[new Date(\"{0}\"), {1}, {2}, {3}, {4}, {5}],".format(
        timestamp, longitude, latitude, speed, anomalyScore, newSequence)

    print "]"



if __name__ == "__main__":
  if len(sys.argv) < 2:
    print "Usage: {0} /path/to/data.csv".format(sys.argv[0])
    sys.exit(0)

  dataPath = sys.argv[1]
  run(dataPath)
