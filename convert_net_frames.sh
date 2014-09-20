#!/bin/bash
for i in $(ls $1_main_*.eps); do
    c=$(basename ${i} .eps);
    $(pstopnm -stdout -portrait $i > ${c}.ppm);
done;
for i in $(ls $1_audio_*.eps); do
    c=$(basename ${i} .eps);
    $(pstopnm -stdout -portrait $i > ${c}.ppm);
done;