/*

ircDDB DV Plugins

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

import java.sql.Connection;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;


import java.util.SimpleTimeZone;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;

import java.util.LinkedList;
import java.util.HashSet;
import java.util.Set;
import java.util.Properties;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.ircDDB.IRCApplication;
import net.ircDDB.IRCMessage;
import net.ircDDB.IRCMessageQueue;
import net.ircDDB.IRCDDBExtApp;

import net.ircDDB.Dbg;



public class DsmApp implements IRCDDBExtApp
{
  Connection db;
  Statement  sql;
  IRCMessageQueue sendQ;
  String currentServerNick;

  String jdbcClass;
  String jdbcURL;
  String jdbcUsername;
  String jdbcPassword;

  CallSignValidator callSignCheck;
	
  public void userJoin (String nick, String name, String host)
  {
  }

  public boolean needsDatabaseUpdate()
  {
	  return false;
  }

  public Date getLastEntryDate()
  {
	  return null;
  }
	
  public IRCDDBExtApp.UpdateResult dbUpdate(Date d, String k, String v, String ircUser)
  {
	  return null;
  }
	
  public LinkedList<IRCDDBExtApp.DatabaseObject> getDatabaseObjects(Date d, int num)
  {
	  return null;
  }
	
  public void setParams (Properties p, Pattern k, Pattern v,
     net.ircDDB.IRCDDBEntryValidator validator)
  {
    jdbcClass = p.getProperty("dsm_jdbc_class", "none");
    jdbcURL = p.getProperty("dsm_jdbc_url", "none");
    jdbcUsername = p.getProperty("dsm_jdbc_username", "none");
    jdbcPassword = p.getProperty("dsm_jdbc_password", "none");
  }

  public void setTopic (String topic)
  {
  }
	
  public void setCurrentNick (String nick)
  {
  }
	
  public void setCurrentServerNick (String nick)
  {
    currentServerNick = nick;
  }
	
  public void userListReset ()
  {
  }
	

  public void userLeave (String nick)
  {
  }
	
  public void userChanOp (String nick, boolean op)
  {
  }

  public void msgChannel (IRCMessage m)
  {
  }
	
  public void msgQuery (IRCMessage m)
  {
  }

  public synchronized IRCMessageQueue getSendQ ()
  {
    return sendQ;
  }


  public synchronized void setSendQ (IRCMessageQueue q)
  {
    sendQ = q;
  }

  public DsmApp()
  {
    db = null;
    sql = null;

    sendQ = null;
    currentServerNick = null;

    callSignCheck = new CallSignValidator();
  }	

  boolean init()
  {
    DatabaseMetaData dbmd; 

    try
    {
      Class.forName(jdbcClass);
    }
    catch (ClassNotFoundException e)
    {
      Dbg.println(Dbg.ERR, "DBClient/ClassLoader: " + e);
      return false;
    }

    try
    {
      db = DriverManager.getConnection(jdbcURL,
	      jdbcUsername, jdbcPassword);
    }
    catch (SQLException e)
    {
      Dbg.println(Dbg.ERR, "DBClient/getConnection: " + e);
      return false;
    }

    try
    {
      dbmd = db.getMetaData();
    }
    catch (SQLException e)
    {
      Dbg.println(Dbg.ERR, "DBClient/getMetaData: " + e);
      return false;
    }

    try
    {
      sql = db.createStatement();
    }
    catch (SQLException e)
    {
      Dbg.println(Dbg.ERR, "DBClient/createStatement: " + e);
      return false;
    }

    return true;
  }

  boolean doSelect()
  {
    Set<Integer> deleteList = new HashSet<Integer>();

    ResultSet rs;

    try
    {
      rs = sql.executeQuery(
	"select timestamp with time zone 'now' at time zone 'UTC', " +
	"id, reporttime, stationcall, repeatercall, sqluser " +
	"from ircddb_queue  limit 30" );

    }
    catch (SQLException e)
    {
	    Dbg.println(Dbg.ERR, "DBClient/executeQuery: " + e);
	    return false;
    }

		

    try
    {
      if (rs != null)
      {
	while (rs.next())
	{
			    
	  java.sql.Timestamp now = rs.getTimestamp(1);
	  java.sql.Timestamp ts = rs.getTimestamp(3);
	  long n = ts.getTime();
	  long n2 = now.getTime();

	  int id = rs.getInt(2);

	  java.text.DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	  if (n > n2)
	  {
	    n = n2 | 0x3FFFFL;
	    // a time in the future, max. 4 minutes
	  }
					
					
	  String targetCS = rs.getString(4);
	  String areaCS = rs.getString(5);

	  while (targetCS.length() < 8)
	  {
	    targetCS = targetCS + ' ';
	  }

	  while (areaCS.length() < 8)
	  {
	    areaCS = areaCS + ' ';
	  }

	  String repeater = areaCS.substring(0,7).trim().toLowerCase();
	  String sqluser = rs.getString(6);

	  // System.out.println(sqluser + " rp:" + repeater);

	  if ( callSignCheck.isValid( targetCS, areaCS, null ) &&
		repeater.equals(sqluser) &&
		!sqluser.equals("none") &&
		 (currentServerNick != null))
	  {
	    IRCMessage m = new IRCMessage();
	    m.command = "PRIVMSG";
	    m.numParams = 2;
	    m.params[0] = currentServerNick;
	    m.params[1] = "UPDATE " + df.format(new Date(n)) +
	      " " + targetCS.replace(' ', '_') + " " +  areaCS.replace(' ', '_');
			    
	    IRCMessageQueue q = getSendQ();
	    
	    if (q != null)
	    {
		    q.putMessage(m);
	    }
	  }

	  deleteList.add(new Integer(id));
	}
      }
      else
      {
	      Dbg.println(Dbg.WARN, "DBClient: ResultSet null");
      }

      rs.close();

      for (Integer i : deleteList)
      {
	sql.executeUpdate ("delete from ircddb_queue where " +
	 " id = " + i );
      }
    }
    catch (SQLException e)
    {
      Dbg.println(Dbg.ERR, "DBClient/select/delete " + e);
      return false;
    }

    return true;
  }

  void closeConnection()
  {
    if (db != null)
    {
      try
      {
	db.close();
      }
      catch (SQLException e)
      {
	Dbg.println(Dbg.ERR, "DBClient/close: " + e);
      }
    }

    db = null;
    sql = null;
  }

  public void run()
  {
    int timer = 0;
    int state = -1;


    while(true)
    {
      if (timer == 0)
      {
	switch(state)
	{
	case -1:
	  if (sendQ != null)
	  {
	    state = 0;
	  }
	  break;

	case 0:
	  Dbg.println(Dbg.INFO, "DBClient: connect request");

	  if (init())
	  {
	    Dbg.println(Dbg.INFO, "DBClient: connected");
	    state = 1;
	    timer = 1;
	  }
	  else
	  {
		  timer = 1;
		  state = 2;
	  }
	  break;
				
	case 1:
	  if (sendQ == null)
	  {
	    state = 2;
	  }
	  else
	  {
	    if (doSelect())
	    {
	      timer = 2;
	    }
	    else
	    {
	      state = 2;
	    }
	  }
	  break;

	case 2:
	  closeConnection();
	  timer = 10;
	  state = -1;
	  break;
	}
      }
      else
      {
	timer--;
      }

      try
      {
	Thread.sleep(1000);
      }
      catch ( InterruptedException e )
      {
	Dbg.println(Dbg.WARN, "sleep was interrupted " + e);
      }
    }
  }	
}

