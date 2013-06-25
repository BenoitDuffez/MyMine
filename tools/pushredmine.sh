#!/bin/sh
adb push redmine.db /sdcard/ && adb shell su -c "cat /sdcard/redmine.db > /data/data/net.bicou.redmine/databases/redmine.db"

