#!/bin/bash

if [ "$#" -ne 1 ]; then
    echo "Usage: ./run_complete.sh /path/to/data.csv"
    exit
fi

HOUND=..
OUTPUT=output
PROCESSED_DATA=$OUTPUT/processed_data.csv

mkdir -p $OUTPUT
$HOUND/tools/preprocess_data.py $1 $PROCESSED_DATA
./run_model.sh $PROCESSED_DATA
