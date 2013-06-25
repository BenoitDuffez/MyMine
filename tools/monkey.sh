#! /bin/bash
cmd="adb shell monkey -v -v -p net.bicou.redmine -s `date +%s` --pct-syskeys 0 --pct-nav 0 50000"
echo "trying: $cmd"
$cmd > .monkey.log &
adb logcat -v time | grep BicouRedmine

