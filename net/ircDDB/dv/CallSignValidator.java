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

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.ircDDB.Dbg;


public class CallSignValidator
{



   boolean isValidCallSign(String s, Pattern p, int min_len, int max_len)
   {

	   if (s.length() != 8)
	   {
		   Dbg.println(Dbg.DBG1, "(" + s + ") not 8 chars");
		   return false;
	   }

	   int len = s.substring(0,7).trim().length();

	   if (len < min_len)
	   {
		   Dbg.println(Dbg.DBG1, "(" + s + ") less than " + min_len + " chars");
		   return false;
	   }

	   if (len > max_len)
	   {
		   Dbg.println(Dbg.DBG1, "(" + s + ") more than " + max_len + " chars");
		   return false;
	   }

	   Matcher m = p.matcher(s);

	   if (m.matches())
	   {
		   return true;
	   }

	   Dbg.println(Dbg.DBG1, "(" + s + ") does not match pattern");
	   return false;
   }


  Pattern areaPattern;
  Pattern targetPattern;
  Pattern starnetPattern;


  public CallSignValidator()
  {
    areaPattern = Pattern.compile(
     "^(([1-9][A-Z])|([A-Z][0-9])|([A-Z][A-Z][0-9]))[0-9A-Z]*[A-Z][ ]*[A-RT-Z]$");
    targetPattern = Pattern.compile(
     "^(([1-9][A-Z])|([A-Z][0-9])|([A-Z][A-Z][0-9]))[0-9A-Z]*[A-Z][ ]*[ A-RT-Z]$");
    starnetPattern = Pattern.compile(
     "^STN[0-9][0-9][0-9] [ A-RT-Z]$");
  }



  public boolean isValid (String k, String v, String user)
  {
    String targetCS = k.replace('_', ' ');
    String areaCS = v.replace('_', ' ');

    boolean validSourceCall = isValidCallSign(targetCS, targetPattern, 3, 7)
	||  isValidCallSign(targetCS, starnetPattern, 6, 6) ;

    if (! validSourceCall )
    {
      Dbg.println(Dbg.DBG1, "targetCS does not comply with call sign rules");
      return false;
    }

    if (!isValidCallSign(areaCS, areaPattern, 4, 6))
    {
      Dbg.println(Dbg.DBG1, "areaCS does not comply with call sign rules");
      return false;
    }

    if (targetCS.substring(0,7).equals(areaCS.substring(0,7)))
    {
      Dbg.println(Dbg.DBG1, "first seven chars are the same: " + targetCS.substring(0,7));
      return false;
    }
    
    if (user == null)
    {
      return true; // no irc user verification possible
    }

    int p = user.indexOf('-');

    if (p == 1)
    {
      if ( user.substring(0,p).equals("d") ||
	  user.substring(0,p).equals("s") )
      {
	Dbg.println(Dbg.DBG2, "allow 'd-' or 's-' user: " + user);
	return true;
      }
      else
      {
	Dbg.println(Dbg.DBG1, "invalid sender: " + user);
	return false;
      }
    }

    if ((p < 4) && (p > 6))
    {
      Dbg.println(Dbg.DBG1, "not a repeater: " + user);
      return false;
    }

    if (! areaCS.substring(0,7).trim().toLowerCase().equals(user.substring(0,p)))
    {
      Dbg.println(Dbg.DBG1, "wrong repeater: " + user);
      return false;
    }

    return true;
  }
}

