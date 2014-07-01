#! /usr/bin/env python
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
import os

from flask import Flask, request

from tools.preprocess_data import preprocess
from model.geospatial_anomaly import runGeospatialAnomaly
from tools.anomaly_to_js_data import postprocess



app = Flask(__name__)

DIR_OUTPUT           = "output"
FILE_DATA            = "data.csv"
FILE_PROCESSED_DATA  = "processed_data.csv"
FILE_MODEL_OUTPUT    = "model_output.csv"

DIR_STATIC_JS        = os.path.join("static", "js")
FILE_JS_DATA         = "data.js"



@app.route('/')
def visualize():
  return app.send_static_file('visualize.html')


@app.route('/simulate')
def simulate():
  return app.send_static_file('simulate.html')


@app.route('/process', methods=['POST'])
def process():
  dataFile = os.path.join(DIR_OUTPUT, FILE_DATA)
  processedDataFile = os.path.join(DIR_OUTPUT, FILE_PROCESSED_DATA)
  modelOutputFile = os.path.join(DIR_OUTPUT, FILE_MODEL_OUTPUT)
  jsDataFile = os.path.join(DIR_STATIC_JS, FILE_JS_DATA)

  with open(dataFile, 'w') as f:
    f.write(request.data)

  preprocess(dataFile, processedDataFile)
  runGeospatialAnomaly(processedDataFile, modelOutputFile)
  postprocess(modelOutputFile, jsDataFile)

  return "Done."


@app.route('/js/<path:path>')
def js(path):
  return app.send_static_file(os.path.join('js', path))


@app.route('/css/<path:path>')
def css(path):
  return app.send_static_file(os.path.join('css', path))


@app.route('/img/<path:path>')
def img(path):
  return app.send_static_file(os.path.join('img', path))


if __name__ == "__main__":
  if not os.path.exists(DIR_OUTPUT):
    os.makedirs(DIR_OUTPUT)

  app.run(debug=True)
