#!/bin/sh
#  
#  ircDDB autoupdate wrapper script
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

URL=http://server-group-2-update.example.com/ircDDB

LKG=0
D2=200
D1=0

while :
do

 D=` expr "$D2" - "$D1" `

 echo " --------"
 date
 echo "$D"

 if [ "$D" -lt 180 ] && [ "$LKG" = 0 ]
 then
        echo "respawning too quickly, going back to old version"
        cp ircDDB.jar.lastKnownGood ircDDB.jar
        cp app2.jar.lastKnownGood app2.jar
        sleep 100
        LKG=1
 else
        if [ "$D2" != 200  ]
        then
                # if the software previously ran longer than 180 seconds
                # keep a copy of the old version
                cp ircDDB.jar ircDDB.jar.lastKnownGood
                cp app.jar app.jar.lastKnownGood
        fi
        curl -O $URL/ircDDB.jar
        curl -O $URL/app2.jar
        LKG=0
 fi

 SECURITY="-Djava.security.manager -Djava.security.policy=ircDDB.policy"
 CP="app2.jar:ircDDB.jar:/opt/dstarmon/postgresql-8.4-701.jdbc3.jar"
 JAVA=/opt/products/dstar/j2se/jre/bin/java

 D1=` date '+%s' `

 $JAVA $SECURITY -cp $CP de.mdx.ircDDB.IRCDDBApp

 D2=` date '+%s' `

 sleep 5

done

