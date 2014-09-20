#!/bin/bash
for i in $(ls $1-1*.ppm); do
	c=$(basename ${i} .ppm);
	$(pnmquant 256 $i > ${c}_temp.ppm);
	$(ppmtogif ${c}_temp.ppm > ${c}.gif);
	$(rm ${c}_temp.ppm);
	$(rm $i);
done
