#!/bin/sh
#Double density for input bucket
dd=6

d[6]=xxh
d[4]=xh
d[3]=h
d[2]=m

for in in "$@"
do
	p=`dirname $in`
	img=`basename $in`
	path=`dirname "$p"`
	
	id=`identify -verbose $in | grep width | tr -d ","`
	w=`echo "$id" | awk '{print $2}'`
	h=`echo "$id" | awk '{print $3}'`

	#Copy for input bucket
	cp $in $path/drawable-xxhdpi/$img

	for s in 4 3 2
	do
		nw=`echo "$w * $s / $dd" | bc`
		nh=`echo "$h * $s / $dd" | bc`
		dest="$path/drawable-${d[$s]}dpi/$img"
		convert $in -resize ${nw}x${nh}^ $dest 
	done
done


