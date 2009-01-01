/*
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Remco den Breeje, <stacium@gmail.com>
 */

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Scanner;
import java.util.Locale;

class CreateHsr {
  public int ix;	/* byte index */
  public int is;	/* Section index */ 
  public int iss;	/* Section size index */
  public int section_start;
  public int section_number;
  public int total_byte_cnt;
  public int[] data;
  private int crc;

  public CreateHsr()
  {
    data = new int[8*1024];
    crc = ix = is = iss = 0;
    section_start = 8;
    section_number = 1;
    total_byte_cnt = 0;
  }

  public boolean IsDone()
  {
    return(total_byte_cnt > 0 && (ix == total_byte_cnt));
  }

  public boolean AddData(int b)
  {
    if(b >= 0)
    {
      data[ix++] = b;
      crc16(data[ix - 1]);
      if(ix == 1 && data[0] != 85)
      {
	System.out.println("Bad first section header (byte 1)");
	return false;
      }
      if(ix == 2 && data[1] == 81) 
	System.out.println("Found S510");
      if(ix == 3 && data[2] != 1)
      {
	System.out.println("Bad first section header (byte 3)");
	return false;
      }
      /* looking for end of section header */
      if(ix == 8)
      {
	if(crc > 0)
	{
	  System.out.println("First section CRC error");
	  return false;
	}

	/* calculate total number of bytes to be received (8=section header) */
	total_byte_cnt = 8 + 5 * data[3] + data[4] + 256 * data[5] + 1;
      }
      if(ix <= 8) /* got all info we need for the first 8 bytes */
	return true;
      if(ix == section_start + 3) /* new section (+3=header size) */
      {
	if(data[section_start] != 85 ||
	    data[section_start + 1 ] != section_number ||
	    data[section_start + 2 ] < 1 || 
	    data[section_start + 2 ] > 60)
	{
	  System.out.format("Bad section header, section_number: %d\n", section_number);
	}
	if(section_number == 1) { /* tell user, we started listening */
	  System.out.format("Section %d started at %d\n", section_number, ix);
	}
      }
      /* looking for end of section */
      if(ix >= section_start + 3 && ix == section_start + data[section_start + 2] + 3 + 2)
      {
	System.out.format("Section %d ended at %d\n", section_number, ix);
	if(crc > 0)
	{
	  System.out.println("Section CRC error");
	  return false;
	}
	/* update section start with new index */
	section_start = ix;
	section_number++;
      }
      /* looking for final byte */
      if(ix == total_byte_cnt)
      {
	if(data[ix - 1] != 7)
	{
	  System.out.println("Bad trailer byte");
	  return false;
	}
	if(section_start != ix - 1 || section_number != data[3] + 1)
	{
	  System.out.println("Bad section structure");
	  return false;
	}
      }
      
      // progress
      // System.out.format("%d of %d\n", ix, total_byte_cnt);
    }
    return true;
  }

  public void WriteHsr()
  {
    int i,j,k;
    if(total_byte_cnt == 0)
      return;

    System.out.format("# Raw data:\n#\n");
    j = 8;
    k = 11;
    System.out.format("# dec hex comment\n");
    System.out.format("# --- --- --------------------------\n");
    for(i = 0;i < total_byte_cnt;i++)
    {
      System.out.format("# %3d  %02X",data[i],data[i]);
      if(i == 0 || k == 3)
	System.out.format((i < total_byte_cnt - 1) ? " section start" : " no more sections");
      if(i == 1)
	System.out.format(" data format type");
      if(i == 3)
	System.out.format(" number of sections");
      if(i == 4)
	System.out.format(" number of data bytes (lsb)");
      if(i == 5)
	System.out.format(" number of data bytes (msb)");
      if(k == 2)
	System.out.format(" section number");
      if(k == 1)
	System.out.format(" section size");
      if(j == 1 || j == 2)
	System.out.format(" CRC-16 data");
      System.out.format("\n");
      if(--j == 0)
	System.out.format("#\n");
      if(--k == 0)
      {
	j = data[i] + 2;
	k = j + 3;
      }
    }
    System.out.format("\n");
  }

  public int crc16(int data)
  {
    int i;
    for(i=7; i >= 0; i--)
    {
      crc <<= 1;
      if((data & (1 << i)) > 0)
	crc ^= 1;
      if((crc & 0x10000) > 0)
	crc ^= 0x18005;
    }
    return crc;
  }
}


