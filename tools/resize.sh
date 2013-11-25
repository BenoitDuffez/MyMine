#!/bin/sh
#input bucket = xxhdpi
#target buckets = xhdpi, hdpi, mdpi

# Usage:
# place image(s) in drawable-xxhdpi
# ./resize.sh path/to/res/drawable-xxhdpi/the_images*.png
# Also:
# density="hdpi" ./resize.sh path/to/....
# this will override the input density (default: xxhdpi)

# Densities:
# mdpi   = 1
# hdpi   = 1.5
# xhdpi  = 2
# xxhdpi = 3

# We use double density for input bucket because we want integers only
case $density in
mdpi)
  dd=2
  ;;
hdpi)
  dd=3
  ;;
xhdpi)
  dd=4
  ;;
*)
 dd=6
  ;;
esac

# Density definitions
d[6]=xxh
d[4]=xh
d[3]=h
d[2]=m

# Convert images
for in in "$@"
do
	p=`dirname $in`
	img=`basename $in`
	path=`dirname "$p"`
	
	id=`identify -verbose $in | grep width | tr -d ","`
	w=`echo "$id" | awk '{print $2}'`
	h=`echo "$id" | awk '{print $3}'`

	# Copy for input bucket
	cp $in $path/drawable-xxhdpi/$img

	for s in 4 3 2
	do
		nw=`echo "$w * $s / $dd" | bc`
		nh=`echo "$h * $s / $dd" | bc`
		dest="$path/drawable-${d[$s]}dpi/$img"
		convert $in -resize ${nw}x${nh}^ $dest 
	done
done


