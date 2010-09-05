/*

ircDDB

Copyright (C) 2010   Michael Dirska, DL1BFF (dl1bff@mdx.de)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/


package net.ircDDB.dv;

import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.TimeZone;
import java.text.SimpleDateFormat;


import net.ircDDB.IRCMessage;
import net.ircDDB.IRCMessageQueue;
import net.ircDDB.Dbg;


public class MemDBSimpleQuery implements MemDBQueryPlugin
{

  MemDB mdb;

  Pattern tablePattern;
  Pattern keyPattern;
  SimpleDateFormat parseDateFormat;


  public MemDBSimpleQuery()
  {
    mdb = null;
    tablePattern = Pattern.compile("[0-9]");
    keyPattern = Pattern.compile("[A-Z0-9_]{8}");

    parseDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    parseDateFormat.setTimeZone( TimeZone.getTimeZone("GMT"));
    
  }

  public void processQuery ( IRCMessage m )
  {
    if (mdb == null)
    {
      Dbg.println(Dbg.ERR, "MemDBSimpleQuery: mdb not initialized");
      return;
    }

    String msg = m.params[1];

    Scanner s = new Scanner(msg);

    String command;

    if (s.hasNext())
    {
      command = s.next();
    }
    else
    {
      return; // no command
    }

    int tableID = 0;

    if (s.hasNext(tablePattern))
    {
      tableID = s.nextInt();
      if ((tableID < 0) || (tableID >= mdb.numberOfTables))
      {
	Dbg.println(Dbg.WARN, "MemDBSimpleQuery: invalid table ID " + tableID);
	return;
      }
    }

    if (command.equals("FIND"))
    {
      if (s.hasNext(keyPattern))
      {

	String key = s.next(keyPattern);

	String reply;

	if (mdb.db.get(tableID).containsKey(key))
	{
	  MemDB.DbObject o = mdb.db.get(tableID).get(key);

	  reply = "UPDATE " + tableID + " " + 
	    parseDateFormat.format(o.dbDate) + " " +
	                            o.key + " " + o.value;;
	}
	else
	{
	  reply = "NOT_FOUND " + tableID + " " + key;
	}

	String other = m.getPrefixNick(); // nick of other user
	
	IRCMessage m2 = new IRCMessage();
	m2.command = "PRIVMSG";
	m2.numParams = 2;
	m2.params[0] = other;
	m2.params[1] = reply;

	IRCMessageQueue q = mdb.getSendQ();
	if (q != null)
	{
	  q.putMessage(m2);
	}
      }
      else
      {
	Dbg.println(Dbg.INFO, "MemDBSimpleQuery: invalid search key");
      }
    }
  }

  public void setParams ( java.util.Properties p, MemDB mdb )
  {
    this.mdb = mdb;
  }

  public void userJoin (String nick, String name, String host)
  {
  }
  public void userLeave (String nick)
  {
  }
  public void userListReset()
  {
  }

}

