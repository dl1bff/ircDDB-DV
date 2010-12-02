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

import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

import net.ircDDB.Dbg;


public class RptrUDPReceiver implements Runnable
{

  RptrApp app;
  int udpPort;

  public RptrUDPReceiver ( RptrApp n, int port )
  {
    app = n;
    udpPort = port;
  }

  public void run()
  {
    byte localhostAddr[] = { 127, 0, 0, 1 };
    byte data[] = new byte[80];

    while(true)
    {


      try
      {
	DatagramSocket s = new DatagramSocket(udpPort, InetAddress.getByAddress(localhostAddr));
	s.setReuseAddress(true);

	for (int count = 0; count < 10000; count++)
	{
	  DatagramPacket p = new DatagramPacket(data, data.length);

	  s.receive(p);

	  // System.out.println("len + offset: " + p.getLength() + " " + p.getOffset());

	  switch (p.getLength())
	  {
	  case 11:

	    if ((p.getOffset() == 0) &&
	      (data[8] == 0) && (data[10] == 0) && ("ABCD".indexOf(data[9]) >= 0))
	    {
	      String cs = new String(data, 0, 8);
	      // System.out.println("(" + cs + ")");

	      if (app != null)
	      {
		app.mheardCall(cs.replaceAll("[^A-Z0-9_ ]", ""), (char) data[9], null);
	      }
	    }
	    break;

	  case 39:
	  case 40:
	  case 59:
	  case 60:

	    if ((p.getOffset() == 0) &&
	      ("ABCD".indexOf(data[18]) >= 0))
	    {
	      String myCall = new String(data, 27, 8);
	      String myExt = new String(data, 35, 4);
	      String yourCall = new String(data, 19, 8);
	      String rpt2 = new String(data, 3, 8);

	     //  System.out.println("rpt1 (" + rpt1 + ")");

	      String headerInfo = String.format("%1$s %2$s %3$02X %4$02X %5$02X %6$s",
		  rpt2.replace(' ', '_'),
		  yourCall.replace(' ', '_'),
		  data[0], data[1], data[2],
		  myExt.replace(' ', '_'));

	      String info =  headerInfo.replaceAll("[^A-Z0-9/_ ]", "_");

	      int len = p.getLength();

	      if ((len == 59) || (len == 60))
	      {
		StringBuffer msg = new StringBuffer();

		for (int i = (len - 20); i < len; i++)
		{
		  int d = data[i] & 0x7F;

		  if ((d > 32) && (d < 127))
		  {
		    msg.append( (char) d );
		  }
		  else
		  {
		    msg.append( "_" );
		  }
		}

		if ((len == 60) && (data[39] == 'S'))
		{
		  info = info + " # " + msg.toString();
		}
		else
		{
		  info = info + " " + msg.toString();
		}
	      }

	      if (app != null)
	      {

		if (
		    ((data[0] == (byte) 0xFF) && (data[1] == (byte) 0xFF) && (data[2] == (byte) 0xFF))
		    )
		{
		  info = null;
		}

		app.mheardCall(myCall.replaceAll("[^A-Z0-9_ ]", ""), (char) data[18], info);
	      }
	      
	    }
	    break;
	    
	  }

	}


	s.close();

      }
      catch ( Exception e )
      {
	Dbg.println(Dbg.WARN, "RptrUDPReceiver: " +e);
      }
      
      
      try
      {
	Thread.sleep(1000);
      }
      catch ( InterruptedException e )
      {
	Dbg.println(Dbg.WARN, "RptrUDPReceiver/sleep: " +e);
      }

    }
  }

}

