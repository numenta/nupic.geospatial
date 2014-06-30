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

from flask import Flask



app = Flask(__name__)



@app.route('/')
def visualize():
    return app.send_static_file('visualize.html')


@app.route('/simulate')
def simulate():
    return app.send_static_file('simulate.html')


@app.route('/js/<path:path>')
def js(path):
    return app.send_static_file(os.path.join('js', path))


@app.route('/css/<path:path>')
def css(path):
    return app.send_static_file(os.path.join('css', path))



if __name__ == "__main__":
  app.run()
