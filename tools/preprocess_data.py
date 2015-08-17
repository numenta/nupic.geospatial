#!/usr/bin/env python
# ----------------------------------------------------------------------
# Numenta Platform for Intelligent Computing (NuPIC)
# Copyright (C) 2014, Numenta, Inc.  Unless you have purchased from
# Numenta, Inc. a separate commercial license for this software code, the
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
Takes in a datafile generated by Grok Hound app.

- Strips out non-commutes
- Sub-samples readings to a minimum resolution of 10-seconds
"""

import csv
import datetime
import sys



def preprocess(dataPath, outPath, verbose=False):
  with open(dataPath) as csvfile:
    reader = csv.reader(csvfile)
    writer = csv.writer(open(outPath, "wb"))

    lastTimestamp = None
    lastTimestampKept = None
    numStationary = 0

    for row in reader:
      timestamp = datetime.datetime.fromtimestamp(int(row[1]) / 1e3)
      speed = float(row[5])

      keep = True

      if (lastTimestamp and
          (timestamp - lastTimestamp).total_seconds() > 5):
        keep = False

      if (lastTimestampKept and
          (timestamp - lastTimestampKept).total_seconds() < 10):
        keep = False

      if speed <= 0.5:
        numStationary += 1
      else:
        numStationary = 0

      if numStationary > 30:
        keep = False

      lastTimestamp = timestamp

      if keep:
        if verbose:
          print "Keeping row:\t{0}".format(row)
        lastTimestampKept = timestamp
        writer.writerow(row)
      else:
        if verbose:
          print "Discarding row:\t{0}".format(row)



if __name__ == "__main__":
  if len(sys.argv) < 3:
    print ("Usage: {0} "
           "/path/to/data.csv /path/to/outfile.csv").format(sys.argv[0])
    sys.exit(0)

  dataPath = sys.argv[1]
  outPath = sys.argv[2]
  preprocess(dataPath, outPath)
