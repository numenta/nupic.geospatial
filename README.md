# Hound

Geospatial anomaly detection app using NuPIC.

## Usage

First, install requirements:

    pip install -r requirements.txt

Then, run:

    python server.py

Finally, open `http://localhost:5000` in your browser.

### Using the route simulator

You can use the route simulator to generate geospatial data and run it through the model. Simply open `http://localhost:5000/simulate` in your browser.

### Loading your own data

Use `tools/run_complete.sh` to run your own data through the system.
