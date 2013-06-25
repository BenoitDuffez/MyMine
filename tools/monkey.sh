#! /bin/bash
cmd="adb shell monkey -v -v -p net.bicou.redmine 50000 -s `date +%s` --pct-syskeys 0"
echo "trying: $cmd"
$cmd > .monkey.log &
adb logcat -v time | grep BicouRedmine

