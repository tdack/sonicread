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

#define DEBUG 1

#include "config.h"

/* locally used global libraries */
#include <errno.h>

/* local defines */
#define BUFLEN 1024

int config_init(main_s * s)
{
  /* initialize memory */
  if((s->config = malloc(sizeof(*(s->config)))) == NULL)
  {
    fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
    return EXIT_FAILURE;
  }
  
  return EXIT_SUCCESS;
}

/* open config file */
unsigned int config_open(FILE * * fp, main_s * s)
{
  /* try to open file */
  *fp = fopen(s->configfile, "r+");
  
  if(*fp == NULL)
  {
    /* failed to open file, try to create it */
    switch(errno)
    {
      case ENOENT:
	/* create */
	*fp = fopen(s->configfile, "w");
	if(*fp == NULL)
	{
	  switch(errno)
	  {
	    case EACCES:
	      return E_NOACCESS;
	    default:	/* 'unkown' error */
	      return E_ERROR;
	  }
	}
	break;
      case EACCES:
	return E_NOACCESS;
      default:	/* 'unkown' error */
	return E_ERROR;
    }
  }
  
  return E_SUCCESS;
}

/* read user config - TODO */
void config_read(FILE * fp, main_s * s)
{
  char *str;

  /* allocate memory */
  str = malloc(sizeof(char)*BUFLEN);

  /* load default config */
  s->config->maxhr = 180;
  s->config->resthr = 70;
  s->config->vo2max = 30;
  s->config->weight = 0;
  
  /* process config file */
  while(fgets(str, BUFLEN, fp) != NULL)
  {
    int itmp;
    if(sscanf(str, "maxhr=%d", &itmp))
      s->config->maxhr = itmp;
    else if(sscanf(str, "resthr=%d", &itmp))
      s->config->resthr = itmp;
    else if(sscanf(str, "vo2max=%d", &itmp))
      s->config->vo2max = itmp;
    else if(sscanf(str, "weight=%d", &itmp))
      s->config->weight = itmp;
  }
}

/* close config file */
void config_close(FILE * fp)
{
  fclose(fp);
}

/* free allocated memory */
void config_free(main_s * s)
{
  free(s->config);
}
