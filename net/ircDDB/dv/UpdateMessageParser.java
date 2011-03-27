/*

ircDDB

Copyright (C) 2011   Michael Dirska, DL1BFF (dl1bff@mdx.de)

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


import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.ircDDB.Dbg;


public class UpdateMessageParser
{

  class UpdatePattern
  {
    Pattern pattern;
    String commands;

    UpdatePattern(String p, String c)
    {
      pattern = Pattern.compile("^(UPDATE) (20[0-2][0-9]-[01][0-9]-[0-3][0-9])" +
        " ([0-2][0-9]:[0-5][0-9]:[0-5][0-9]) " + p + "$");
      commands = "..." + c;
    }
  }


  CallSignVisibilityChecker cvc;

  List<UpdatePattern> list;

  Pattern callsignPattern;

  public UpdateMessageParser( CallSignVisibilityChecker checker )
  {
    cvc = checker;

    callsignPattern = Pattern.compile("^(([1-9][A-Z])|([A-Z][0-9])|([A-Z][A-Z][0-9]))[0-9A-Z]*[A-Z][_]*[_A-RT-Z]$");

    list = new ArrayList<UpdatePattern>();

    list.add(new UpdatePattern(
      "([A-Z0-9/_]{8}) ([A-Z0-9/_]{8})",
      "M."));

    list.add(new UpdatePattern(
      "([A-Z0-9/_]{8}) ([A-Z0-9/_]{8}) ([0-9A-F]{1})",
      "M.."));

    list.add(new UpdatePattern(
      "([A-Z0-9/_]{8}) ([A-Z0-9/_]{8}) ([A-Z0-9/_]{8}) ([A-Z0-9/_]{8})" +
       " ([0-9A-F]{2}) ([0-9A-F]{2}) ([0-9A-F]{2}) ([A-Z0-9/_]{4})",
      "M..Y...m"));

    list.add(new UpdatePattern(
      "([A-Z0-9/_]{8}) ([A-Z0-9/_]{8}) ([0-9A-F]{1}) ([A-Z0-9/_]{8})" +
       " ([A-Z0-9/_]{8}) ([0-9A-F]{2}) ([0-9A-F]{2}) ([0-9A-F]{2}) ([A-Z0-9/_]{4})",
      "M...Y...m"));

    list.add(new UpdatePattern(
      "([A-Z0-9/_]{8}) ([A-Z0-9/_]{8}) ([A-Z0-9/_]{8}) ([A-Z0-9/_]{8})" +
       " ([0-9A-F]{2}) ([0-9A-F]{2}) ([0-9A-F]{2}) ([A-Z0-9/_]{4}) ([\\p{Graph}]{20})",
      "M..Y...mm"));

    list.add(new UpdatePattern(
      "([A-Z0-9/_]{8}) ([A-Z0-9/_]{8}) ([0-9A-F]{1}) ([A-Z0-9/_]{8})" +
       " ([A-Z0-9/_]{8}) ([0-9A-F]{2}) ([0-9A-F]{2}) ([0-9A-F]{2}) ([A-Z0-9/_]{4})" +
       " ([\\p{Graph}]{20})",
      "M...Y...mm"));

    list.add(new UpdatePattern(
      "([A-Z0-9/_]{8}) ([A-Z0-9/_]{8}) ([0-9A-F]{1}) ([A-Z0-9/_]{8})" +
       " ([A-Z0-9/_]{8}) ([0-9A-F]{2}) ([0-9A-F]{2}) ([0-9A-F]{2}) ([A-Z0-9/_]{4})" +
       " ([0-9A-F]{2}) ([A-Z0-9/_]{8})",
      "M...Y...m.."));

    list.add(new UpdatePattern(
      "([A-Z0-9/_]{8}) ([A-Z0-9/_]{8}) ([0-9A-F]{1}) ([A-Z0-9/_]{8})" +
       " ([A-Z0-9/_]{8}) ([0-9A-F]{2}) ([0-9A-F]{2}) ([0-9A-F]{2}) ([A-Z0-9/_]{4})" +
       " ([0-9A-F]{2}) ([A-Z0-9/_]{8}) ([\\p{Graph}]{20})",
      "M...Y...m..m"));

    list.add(new UpdatePattern(
      "([A-Z0-9/_]{8}) ([A-Z0-9/_]{8}) ([A-Z0-9/_]{8}) ([A-Z0-9/_]{8})" +
       " ([0-9A-F]{2}) ([0-9A-F]{2}) ([0-9A-F]{2}) ([A-Z0-9/_]{4})" +
       " (#) ([\\p{Graph}]{20})",
      "M..Y...m.."));
  }

  public String parseAndModify(String msg)
  {

    Iterator<UpdatePattern> i = list.iterator();

    while (i.hasNext())
    {
      UpdatePattern u = i.next();

      Matcher m = u.pattern.matcher(msg);

      if (m.matches())
      {
	if (m.groupCount() == u.commands.length())
	{
	  int j;
	  boolean mycallVisible = false;
	  StringBuffer sb = new StringBuffer(100);

	  for (j = 1; j <= m.groupCount(); j++)
	  {
	    if (j > 1)
	    {
	      sb.append(' ');
	    }

	    switch(u.commands.charAt(j-1))
	    {
	    case 'M':
	      mycallVisible = cvc.isCallSignVisible(m.group(j));
	      if (mycallVisible)
	      {
		sb.append(m.group(j));
	      }
	      else
	      {
		sb.append("********");
	      }
	      break;

	    case 'm':
	      if (mycallVisible)
	      {
		sb.append(m.group(j));
	      }
	      else
	      {
		sb.append("____________________".substring(0, m.group(j).length()) );
	      }
	      break;
	      
	    case 'Y':
	      Matcher m2 = callsignPattern.matcher(m.group(j));
	      if ((cvc.isCallSignVisible(m.group(j)) || (! m2.matches()) )
		&& (!m.group(j).startsWith("VIS")))
	      {
		sb.append(m.group(j));
	      }
	      else
	      {
		sb.append("********");
	      }
	      break;

	    default:
	      sb.append(m.group(j));
	      break;

	    }
	  }

	  return sb.toString();
	}
	else
	{
	  Dbg.println( Dbg.ERR, "groupCount != command.length");
	  return null;
	}
      }
    }

    return null;
  }
}

