#!/bin/sh
#
#  ircDDB automatic startup
#
#  Copyright (C) 2010   Michael Dirska, DL1BFF (dl1bff@mdx.de)
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 2 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
#
#

START_RUNSH=no
OLD_PID=""

if [ -s run.pid ]
then
   OLD_PID=` cat run.pid | tr -c -d '[0-9]' `
fi

if [ -n "$OLD_PID" ]
then

   ps --pid $OLD_PID >/dev/null

   if [ $? != 0 ]
   then
     # run.sh not running, kill old java process
     JAVA_PID=`ps auxwww | grep "PPID${OLD_PID}\$" | awk 'NR==1 { print $2 }' | tr -c -d '[0-9]' `
     if [ -n "JAVA_PID" ]
     then
        kill "$JAVA_PID"
     fi
     START_RUNSH=yes
   fi

else
   START_RUNSH=yes
fi

if [ "$START_RUNSH" = "yes" ]
then
   nohup ./run.sh "(cwd: `pwd`)" >stdout.txt 2>stderr.txt &
fi

