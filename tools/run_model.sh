#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Usage: ./run_model.sh /path/to/processed_data.csv"
    exit
fi

HOUND=..
OUTPUT=output
ANOMALY_SCORES=$OUTPUT/anomaly_scores.csv
VISUALIZATION_DATA=$HOUND/static/js/data.js

mkdir -p $OUTPUT
$HOUND/model/geospatial_anomaly.py $1 $ANOMALY_SCORES
rm $VISUALIZATION_DATA
$HOUND/tools/anomaly_to_js_data.py $ANOMALY_SCORES > $VISUALIZATION_DATA
