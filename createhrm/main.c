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

/*
 * global libraries
 */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <pwd.h>
#include <err.h>

#define DEBUG 0

/*
 * local libraries
 */
#include "config.h"
#include "convert.h"
#include "createhrm.h"
#include "writehrm.h"
#include "rawdataread.h"

/*
 * Print version screen
 */
static void print_version(void)
{
  printf("%s %s\n", PACKAGE_NAME, PACKAGE_VERSION);
  printf("Copyright (c) 2007\n");
  printf("Written by Remco den Breeje\n");
  printf("Please report bugs to <%s>.\n", PACKAGE_BUGREPORT);
  printf("\n");
}

/*
 * Print helpscreen
 */
static void print_usage(void)
{
  print_version();
  printf("usage: %s [-vh] -i inputfile [-o outputfile]\n", PACKAGE_NAME);
  printf(" -i [file]  input file with raw SonicLink data\n");
  printf(" -o [file]  output file in Polar hrm format\n");
  printf(" -v         print version information and exit\n");
  printf(" -h         print usage information and exit\n");
  printf("\n");
}

/* 
 * Process arguments 
 */
static int process_arg(main_s * s, int argc, char **argv)
{
  int i, j;
  char flag;

  /* loop arguments */
  for (i=1; i < argc; i++)
  {
    /* get flag */
    flag = argv[i][1];

    /* read options with arguments */
    switch(flag)
    {
      case 'i':
	i++; 
	s->inputfile = strdup(argv[i]);
	break;
      case 'o':
	i++; 
	s->outputfile = strdup(argv[i]);
	break;
      default:
	/* read argument-less '-abc' options */
	for (j=1; j <strlen(argv[i]); j++)
	{
	  flag = argv[i][j];
	  switch(flag)
	  {
	    case 'v':
	      print_version();
	      return EXIT_SUCCESS;
	      break;
	    case 'h':
	      print_usage();
	      return EXIT_SUCCESS;
	      break;
	  }
	}
    }
  } /* end of reading options */
  
  /* check mandatory, the not so optional, options */
  if(s->inputfile == NULL) /* -i option */
  {
    print_usage();
    return EXIT_FAILURE;
  }

  return EXIT_SUCCESS;
}

/* 
 * Main Program
 */
int main(int argc, char **argv)
{
  int rc;
  main_s * s;
  FILE * ifp, * ofp, *cfp; /* input-, output-, config-filepointer */

  /* init main struct */
  if((s = malloc(sizeof(main_s))) == NULL)
  {
    fprintf(stderr, "Memory allocation error\n");
    return EXIT_FAILURE;
  }

  /* set initial section count - will be allocated in convert.c */
  s->section = NULL;
  s->sectioncount = 0;

  /* read arguments */
  if((rc = process_arg(s, argc, argv)))
    return rc;

  /* read user config */
  config_init(s);
  s->configfile = strdup("~/.config/createhrm.rc");
  s->configfile = strdup("/home/remco/.config/createhrm.rc");
  if((rc = config_open(&cfp, s)))  /* open config file */
    err(1, "Configfile %s", s->configfile);

  config_read(cfp, s);
  config_close(cfp);

  /* Read raw input file */
  if((rc = rawdata_open(&ifp, s)))  /* open input file */
  {
    switch(rc)
    {
      case E_NOFILE:
	fprintf(stderr, "Error! No such input file: '%s'\n", s->inputfile);
	break;
      case E_NOACCESS:
	fprintf(stderr, "Error! Input file not readable: '%s'\n", s->inputfile);
	break;
      default:
	fprintf(stderr, "Error! Unkown error with file '%s'\n", 
	    s->inputfile);
    }
    return EXIT_FAILURE;
  }
  rawdata_read(ifp, s);	/* read data */
  rawdata_close(ifp);	/* close input file */

  /* Convert raw data to hrm structs */
  convert(s);

  /* Write hrm file */
  if((rc = writehrm_create(&ofp, s)))
  {
    switch(rc)
    {
      case E_NOFILE:
      case E_NOACCESS:
	fprintf(stderr, "Error! Could not create output file: '%s'\n", s->inputfile);
	break;
      default:
	fprintf(stderr, "Error! Unkown error with file '%s'\n", s->outputfile);
    }
    return EXIT_FAILURE;
  }
  writehrm_data(ofp, s);
  writehrm_close(ofp);

  /* free */
  writehrm_free(s);
  convert_free(s);
  config_free(s);
  rawdata_free(s);
  free(s->inputfile);
  free(s->outputfile);
  free(s);
  return EXIT_SUCCESS;
}
