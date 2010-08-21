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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;

import java.util.Properties;

import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

import java.util.Date;
import java.util.TimeZone;
import java.util.NoSuchElementException;
import java.text.SimpleDateFormat;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.FileOutputStream;

import net.ircDDB.Dbg;
import net.ircDDB.IRCMessage;
import net.ircDDB.IRCMessageQueue;
import net.ircDDB.IRCDDBExtApp;


public class MemDB implements IRCDDBExtApp
{

	int numberOfTables;
	List<Map<String,DbObject>> db;
	String[] bootFile;

        Pattern datePattern;
        Pattern timePattern;
	SimpleDateFormat parseDateFormat;


	IRCMessageQueue sendQ;

	CallSignValidator validator;

	MemDBQueryPlugin queryPlugin;

	String updateChannel;


	
	public MemDB()
	{
	  db = new ArrayList<Map<String,DbObject>>();

	  datePattern = Pattern.compile("20[0-9][0-9]-((1[0-2])|(0[1-9]))-((3[01])|([12][0-9])|(0[1-9]))");
	  timePattern = Pattern.compile("((2[0-3])|([01][0-9])):[0-5][0-9]:[0-5][0-9]");

	  parseDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	  parseDateFormat.setTimeZone( TimeZone.getTimeZone("GMT"));

	  sendQ = null;
	  validator = new CallSignValidator();
	  queryPlugin = null;

	  updateChannel = null;
	}
		
	public boolean setParams( Properties p, int numTables, Pattern[] keyPattern, Pattern[] valuePattern)
	{
	  numberOfTables = numTables;

	  updateChannel = p.getProperty("irc_channel", "none");

	  if (updateChannel.equals("none"))
	  {
	    updateChannel = null;
	  }

	  bootFile = new String[numberOfTables];

	  int i;

	  for (i=0; i < numberOfTables; i++)
	  {
	    db.add( Collections.synchronizedMap( new HashMap<String,DbObject>() ));
	    bootFile[i] = p.getProperty("memdb_bootfile" + i, "db" + i + ".txt");
	  }

	  for (i=(numberOfTables - 1); i >= 0; i--)
	  {
	    try
	    {
	      Scanner s = new Scanner (new File(bootFile[i]));
      
	      while (s.hasNext(datePattern))
	      {
		processUpdate(i, s, keyPattern[i], valuePattern[i]);
	      }
		    
	    }
	    catch (FileNotFoundException e)
	    {
	      Dbg.println(Dbg.WARN, "file " + bootFile[i] + " not found.");
	    }
	  }
	  
	  String queryPluginName = p.getProperty("memdb_query_plugin", "none");

	  if (!queryPluginName.equals("none"))
	  {
	    try
	    {
	      Class queryPluginClass = Class.forName(queryPluginName);

	      queryPlugin = (MemDBQueryPlugin) queryPluginClass.newInstance();

	      queryPlugin.setParams( p, this );

	    }
	    catch (Exception e)
	    {
	      Dbg.println(Dbg.ERR, "query plugin not loaded! " + e);
	      queryPlugin = null;
	    }
	  }
	
	  return true;
	}

	public boolean needsDatabaseUpdate(int tableID)
	{
		return true;
	}
	
	public void setCurrentNick (String nick)
	{
	}

	public void setCurrentServerNick (String nick)
	{
	}

	public void setTopic (String topic)
	{
	}

	
	public void userJoin (String nick, String name, String host)
	{
	  if (queryPlugin != null)
	  {
	    queryPlugin.userJoin(nick, name, host);
	  }
	}
	
	public void userLeave (String nick)
	{
	  if (queryPlugin != null)
	  {
	    queryPlugin.userLeave(nick);
	  }
	}

	public void userListReset()
	{
	  if (queryPlugin != null)
	  {
	    queryPlugin.userListReset();
	  }
	}

	public void userChanOp (String nick, boolean op)
	{
	}
	
	class DbObject
	{
		Date dbDate;
		String key;
		String value;
		
		DbObject( Date d, String k, String v )
		{
			dbDate = d;
			key = k;
			value = v;
		}
	}

	void processUpdate ( int tableID, Scanner s, Pattern keyPattern, Pattern valuePattern)
	{
		if (s.hasNext(datePattern))
		{
			String d = s.next(datePattern);
			
			if (s.hasNext(timePattern))
			{
				String t = s.next(timePattern);
				
				
				Date dbDate = null;

				try
				{
					dbDate = parseDateFormat.parse(d + " " + t);
				}
				catch (java.text.ParseException e)
				{
					dbDate = null;
				}
					
				if ((dbDate != null) && s.hasNext(keyPattern))
				{
					String key = s.next(keyPattern);
					
					
					if (s.hasNext(valuePattern))
					{
						String value = s.next(valuePattern);
						
						// DbObject o = new DbObject( dbDate, key, value );
						// db.put(key, o);

						dbUpdate( tableID, dbDate, key, value, null );
					}
				}
			}
		}
		
	}
	
	public void msgChannel (IRCMessage m)
	{
	}
	
	public void msgQuery (IRCMessage m)
	{
	  if (queryPlugin != null)
	  {
	    queryPlugin.processQuery(m);
	  }
	}
	
	
	public synchronized void setSendQ( IRCMessageQueue s )
	{
		sendQ = s;
	}
	
	public synchronized IRCMessageQueue getSendQ ()
	{
		return sendQ;
	}
	
	
	class DbObjectComparator implements Comparator<DbObject>
	{
		public int compare(DbObject o1, DbObject o2)
		{
			return o1.dbDate.compareTo(o2.dbDate);
		}
	}
	
	
	LinkedList<DbObject> getSortedDbEntries(int tableID)
	{
		Collection<DbObject> c = db.get(tableID).values();
				
		LinkedList<DbObject> l = new LinkedList<DbObject>();
				
		l.addAll(c);
				
		Collections.sort(l, new DbObjectComparator() );
		
		return l;
	}
	
	public Date getLastEntryDate(int tableID)
	{
		LinkedList<DbObject> l = getSortedDbEntries(tableID);

		DbObject o = null;
		
		try
		{
			o = l.getLast();
		}
		catch (NoSuchElementException e)
		{
			o = null;
		}
			
		if (o != null)
		{
			return o.dbDate;
		}
		
		return new Date(950000000000L); // February 2000
	}

	public LinkedList<IRCDDBExtApp.DatabaseObject> getDatabaseObjects(
		int tableID, Date beginDate, int numberOfObjects)
	{
		LinkedList<IRCDDBExtApp.DatabaseObject> result;

		result = new LinkedList<IRCDDBExtApp.DatabaseObject>();
		
        	LinkedList<DbObject> l = getSortedDbEntries(tableID);
		int count = 0;

		for (DbObject o : l)
		{
			if (beginDate.compareTo(o.dbDate) <= 0)
			{
				if (count > numberOfObjects)
				{
					break;
				}

				IRCDDBExtApp.DatabaseObject obj;

				obj = new IRCDDBExtApp.DatabaseObject();
				obj.modTime = o.dbDate;
				obj.key = o.key;
				obj.value = o.value;
				
				result.add(obj);

				count ++;
			}
		}

		return result;
	}

	String getZoneRPCS( String arearp_cs, String ircUser )
	{
	  if (ircUser == null)
	  {
	    return null; // don't accept updates from channel or bootfile
	  }

	  String areaCS = arearp_cs.replace('_', ' ');

	  String zonerp_cs = "";

	  int p = ircUser.indexOf('-');

	  if (p == 1)
	  {
	     if ( ircUser.substring(0,p).equals("d") )
	     {
	       zonerp_cs = areaCS.substring(0,7).trim();
	     }
	  }
	  else if ((p >= 4) && (p <= 6))
	  {
	    zonerp_cs = ircUser.substring(0, p).toUpperCase();
	  }

	  while (zonerp_cs.length() < 8)
	  {
	    zonerp_cs = zonerp_cs + " ";
	  }

	  Dbg.println(Dbg.DBG1, "zonerp_cs (" + zonerp_cs + ")");

	  Pattern zonePattern = Pattern.compile(
	    "^(([1-9][A-Z])|([A-Z][0-9])|([A-Z][A-Z][0-9]))[0-9A-Z]*[A-Z][ ]*  $");

	  Matcher m = zonePattern.matcher(zonerp_cs);

	  if (m.matches())
	  {
	    if (areaCS.substring(0,7).equals(zonerp_cs.substring(0,7)))
	    {
	      return zonerp_cs.replace(' ', '_');
	    }
	  }

	  return null;
	}


	public IRCDDBExtApp.UpdateResult dbUpdate( int tableID, Date d, String k, String v, String ircUser )
	{

		if (tableID == 0)
		{
		  if (!validator.isValid(k, v, ircUser))
		  {
		    Dbg.println(Dbg.DBG1, "invalid " + k + " " + v);
		    return null;
		  }

		  if (!db.get(1).containsKey(v))
		  {
		    Dbg.println(Dbg.DBG1, v + " not found");

		    String zonerp_cs = getZoneRPCS( v, ircUser );

		    if (zonerp_cs != null)
		    {
		      DbObject n = new DbObject( new Date(), v, zonerp_cs );

		      db.get(1).put(v, n);

		      if (updateChannel != null)
		      {
			IRCMessage m2 = new IRCMessage();
			m2.command = "PRIVMSG";
			m2.numParams = 2;
			m2.params[0] = updateChannel;
			m2.params[1] = "1 " +
			  parseDateFormat.format(n.dbDate) + " " +
			  n.key + " " + n.value;

			IRCMessageQueue q = getSendQ();
			if (q != null)
			{
			  q.putMessage(m2);
			}

		      }
		    }
		  }
		}
		else if (tableID == 1)
		{
		  if ((ircUser != null) && (!ircUser.startsWith("s-")))
		      // only accept updates via channel, bootfile or "s-" user
		  {
		    Dbg.println(Dbg.DBG1, "not accepting UPDATE for tableID=1");
		    return null;
		  }
		}
		else // only tableID 0 and 1 are valid
		{
		  Dbg.println(Dbg.DBG1, "invalid tableID " + tableID);
		  return null;
		}

		IRCDDBExtApp.UpdateResult result;
		IRCDDBExtApp.DatabaseObject newObj;

		result = new IRCDDBExtApp.UpdateResult();

		newObj = new IRCDDBExtApp.DatabaseObject();
		newObj.modTime = d;
		newObj.key = k;
		newObj.value = v;

		DbObject n = new DbObject( d, k, v );

		if (db.get(tableID).containsKey(k))
		{
			DbObject o = db.get(tableID).get(k);

			IRCDDBExtApp.DatabaseObject oldObj;
			oldObj = new IRCDDBExtApp.DatabaseObject();
			oldObj.modTime = o.dbDate;
			oldObj.key = o.key;
			oldObj.value = o.value;

			result.oldObj = oldObj;
			result.keyWasNew = false;

			if (o.dbDate.getTime() > d.getTime())
			{
				// System.out.println("old entry was newer");
				return null;
			}
		}
		else
		{
			result.oldObj = null;
			result.keyWasNew = true;
		}

		Date nowDate = new Date();

		if (d.getTime() > (nowDate.getTime() + 300000))
		{
			Dbg.println(Dbg.WARN, "new entry more than 5 min in future - ignoring");
			return null;
		}

		db.get(tableID).put (k, n);

		result.newObj = newObj;
		
		return result;
	}


	
	public void run()
	{
		
		while (true)
		{
			
			try
			{
				Thread.sleep(300000);
			}
			catch ( InterruptedException e )
			{
				Dbg.println(Dbg.WARN, "sleep was interrupted: " + e);
			}

			

			try
			{
			  int i;
			  for (i=0; i < numberOfTables; i++)
			  {
			    PrintWriter p = new PrintWriter(new FileOutputStream(bootFile[i]));

			    LinkedList<DbObject> l = getSortedDbEntries(i);

			    for (DbObject o : l)
			    {
				    p.println(parseDateFormat.format(o.dbDate) + " " + o.key + " " + o.value);
			    }

			    p.close();
			  }
				
			}
			catch (IOException e)
			{
				Dbg.println(Dbg.ERR, "dumpDb failed " + e);
			}

		}
	}
	




}




