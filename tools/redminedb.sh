#! /bin/bash

adb $1 shell su -c "cat /data/data/net.bicou.redmine/databases/redmine.db > /sdcard/redmine.db" && adb $1 pull /sdcard/redmine.db .redmine.db && /opt/local/bin/sqlite3 -column -header .redmine.db

