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

import java.util.Properties;
import java.util.regex.Pattern;
import java.util.Date;
import net.ircDDB.IRCApplication;
import net.ircDDB.IRCMessage;
import net.ircDDB.IRCMessageQueue;
import net.ircDDB.IRCDDBExtApp;

import net.ircDDB.Dbg;



public class RptrStandAloneApp extends RptrApp
{

  public boolean setParams (Properties p, int numTables, Pattern[] k, Pattern[] v)
  {
    boolean success = super.setParams(p, numTables, k, v);

    if (!success)
    {
      return false;
    }

    insertUsers = p.getProperty("rptr_insert_users", "no").trim().equals("yes");
    fixUnsyncMNG = p.getProperty("rptr_fix_unsync_mng", "no").trim().equals("yes");

    Dbg.println(Dbg.DBG1, "property 'rptr_insert_users': " + insertUsers);

    if ( !p.getProperty("version", "multihomed").trim().equals("standalone") )
    {
      Dbg.println(Dbg.ERR, "'version' property not correct - will not insert users");
      insertUsers = false;
    }

    udpPort = Integer.parseInt(p.getProperty("mheard_udp_port", "12346"));

    return true;
  }	  

}

