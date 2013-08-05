#! /bin/bash

package=net.bicou.redmine
db=redmine.db

adb $1 shell "cat /data/data/$package/databases/$db > /sdcard/$db"
adb $1 shell su -c "cat /data/data/$package/databases/$db > /sdcard/$db"
adb $1 pull /sdcard/$db .$db
/opt/local/bin/sqlite3 -column -header .$db

