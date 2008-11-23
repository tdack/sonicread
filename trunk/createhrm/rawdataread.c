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

#define DEBUG 0

#include "rawdataread.h"

/* locally used global libraries */
#include <errno.h>
#include <string.h>

/* local defines */
#define BUFLEN 1024

static unsigned char fetch_decimal(char * str)
{
  int i = 1;		/* character index in string - skip '#' */
  int atnumber = 0;	/* index scanning a number */
  char * number;	/* decimal in string */
  unsigned char rval;	/* return value */

  /* set default characters */
  number = strdup("   ");

  /* loop till end of line or when the number length exeeds 3 chars */
  while(str[i] != '\0' && atnumber < sizeof(number))
  {
    /* read number character */
    number[atnumber] = str[i];

    /* increment number index */
    if(str[i] != ' ')
      atnumber++;

    /* number read */
    if(atnumber > 0 && (str[i] == ' ' || str[i] == '\0')) 
      break;

    i++;
  }

  /* return decimal value */
  rval = (unsigned char)strtol(number, NULL, 10);

  /* free alloc'd mem */
  free(number);

  return rval;
}



/*
 * Open the raw data file, and return the filepointer
 */
unsigned int rawdata_open(FILE * * fp, main_s * s)
{
  /* try to open file */
  *fp = fopen(s->inputfile, "r");
  
  if(*fp == NULL)
  {
    switch(errno)
    {
      case ENOENT:
	return E_NOFILE;
      case EACCES:
	return E_NOACCESS;
      default:	/* 'unkown' error */
	return E_ERROR;
    }
  }
  
  return E_SUCCESS;
}

void rawdata_read(FILE * fp, main_s * s)
{
  int si;
  unsigned char d;
  char *str;
  int linecount = 1;

  /* allocate memory */
  str = malloc(sizeof(char)*BUFLEN);

  /* no sections found yet */
  si = -1;

  while(fgets(str, BUFLEN, fp) != NULL)
  {
    /* correct start of line? */
    if(str[0] == '#')
    {
      /* fetch decimal */
      d = fetch_decimal(str);

      /* look for section start */
      if(d == 85 && strstr(str, "section start") != NULL)
      {

	/* get section index */
	si = s->sectioncount;

	DBG("New section #%d found at %d", si, linecount);

	/* allocate mem for pointer to array */
	if((s->section = (section_s **)realloc(s->section, (
						si+1)*sizeof(section_s *))) 
	    == NULL)
	{
	  fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
	  exit(EXIT_FAILURE);
	}

	/* allocate mem for section [si] array */
	if((s->section[si] = malloc(sizeof(section_s))) == NULL)
	{
	  fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
	  exit(EXIT_FAILURE);
	}
	s->section[si]->number = si + 1;
	s->section[si]->size = 0; /* default size */

	/* update section count */
	s->sectioncount = si + 1;
      } /* look for section size */
      else if(s->sectioncount > 0 && 
	      strstr(str, "section size") != NULL)
      {
	/* get section index */
	si = s->sectioncount - 1;

	/* get section size, size has to equal 0 to set new size! */
	if(s->section[si]->size > 0)
	{
	  DBG("ERROR! section %d already has an size set!", si + 1);
	  // quit
	}
	DBG("  Section size: %d", d);
	s->section[si]->size = d;
	
	/* alloc memory and set index */
	s->section[si]->data = malloc(d*sizeof(unsigned char));
	s->section[si]->index = 0;

      }	/* look for section end */
      else if(strstr(str, "CRC-16 data") != NULL)
      {
	DBG("  Section end");
      }
      else if(s->sectioncount > 0) /* is there a section open? */
      {
	/* get section index */
	si = s->sectioncount - 1;

	/* get section size, size has to be > 0 to fill it with data!
	 * and protect from overflow - no error since it prop just is 
	 * an empty line
	 */
	if(s->section[si]->size > 0 && 
	    s->section[si]->index < s->section[si]->size)
	{
	  /* set decimal value */
	  s->section[si]->data[s->section[si]->index] = d;

	  /* increment index */
	  s->section[si]->index++;
	}
      }
    }
    linecount++;
  }

  /* free allocated memory */
  free(str);

  /* have we read a thing? */
  if(linecount == 1) 
    printf("Error reading file\n");
}

/* 
 * Close the file
 */
void rawdata_close(FILE * fp)
{
  // TODO error checking
  fclose(fp);
}

/* 
 * Free allocated memory 
 */
void rawdata_free(main_s * s)
{
  int i;

  /* free all section data */
  for(i=0; i < s->sectioncount; i++)
    if(s->section[i]->size)
      free(s->section[i]->data);

  /* free all sections */
  for(i=0; i < s->sectioncount; i++)
    free(s->section[i]);

  /* free sections pointer */
  free(s->section);

  /*
	*/
}

