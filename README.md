# Hound

Geospatial anomaly detection app using NuPIC.

## Usage

First, install requirements:

    pip install -r requirements.txt

Then, run:

    python server.py

Finally, open `http://localhost:5000` in your browser.

### Loading your own data

Use `tools/parse_csv.py` to convert your CSV data file into the contents for the `static/js/data.js` file.
