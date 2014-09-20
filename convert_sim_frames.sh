#!/bin/bash
for i in $(ls $1-*.gif); do
	c=$(basename ${i} .gif);
	$(giftopnm $i > ${c}.ppm);
done
