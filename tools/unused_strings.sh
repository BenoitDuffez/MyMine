#! /bin/bash

ok=1
for string in $(cat ../res/values/strings*.xml | grep "<string name=" | tr \" " " | awk '{print $3}')
do

	nb=`find ../src | xargs grep "R.string.$string" | wc -l | bc`
	if [ $nb == 0 ]
	then
		nb=`find .. | grep xml | xargs grep "@string/$string" | wc -l | bc`
		if [ $nb == 0 ]
		then
			echo "$string is not used"
			ok=0
		fi
	fi
done

if [ $ok == 1 ]
then
	echo "All good!"
fi


