#!/bin/sh
adb push .redmine.db /sdcard/redmine.db && adb shell su -c "cat /sdcard/redmine.db > /data/data/net.bicou.redmine/databases/redmine.db"

