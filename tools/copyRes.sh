#!/bin/sh

for img in _disabled_holo_light.png _focused_holo_light.png _normal_holo_light.png _pressed_holo_light.png
do
	for d in l m h xh xxh
	do
		for o in on off
		do
			cp /Applications/Android\ Studio.app/sdk/platforms/android-19/data/res/drawable-${d}dpi/btn_rating_star_${o}${img} MyMine/src/main/res/drawable-${d}dpi/
		done
	done
done

