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

#include "writehrm.h"

/* locally used global libraries */
#include <errno.h>
#include <stdarg.h>

static void hrm_header(FILE * fp, char * s)
{
  fprintf(fp, "[%s]\n", s);
}

static void hrm_data_int(FILE * fp, int value)
{
  fprintf(fp, "%d\n", value);
}
  
static void hrm_data_strbinchar(FILE * fp, char * name, char value)
{
  int i;
  fprintf(fp, "%s=", name);
  for(i=0; i < 8; i++)
    fprintf(fp, "%d", ((value << i) & 0x80) ? 1 : 0);
  fprintf(fp, "\n");
}

static void hrm_data_strint(FILE * fp, char * name, int value)
{
  fprintf(fp, "%s=%d\n", name, value);
}

static void hrm_data_strstr(FILE * fp, char * name, char * value)
{
  fprintf(fp, "%s=%s\n", name, value);
}

static void hrm_string(FILE * fp, char * str)
{
  fprintf(fp, "%s\n", str);
}

static void hrm_printf(FILE * fp, char * format, ...)
{
  va_list arg;
  va_start(arg, format);
  vfprintf(fp, format, arg);
  va_end(arg);
  fprintf(fp, "\n");
}

static void hrm_spacer(FILE * fp)
{
  hrm_string(fp, "");
}

/*
 * Create an file, and return the filepointer
 */
unsigned int writehrm_create(FILE * * fp, main_s * s)
{
  /* print to stdout if no output file is entered */
  if(s->outputfile == NULL)
  {
    *fp = stdout;
    goto return_success;
  }
    
  /* try to open file */
  *fp = fopen(s->outputfile, "w");

  /* check for errors */
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

return_success:
  return E_SUCCESS;
}

static void hrm_put_params(FILE * fp, main_s * s)
{
  hrm_header(fp, "Params");
  hrm_data_strint(fp,	  "Version",	s->hrm->params->version);
  hrm_data_strint(fp,	  "Monitor",	s->hrm->params->monitor);
  hrm_data_strbinchar(fp, "SMode",	s->hrm->params->smode.speed	<< 7 |
				s->hrm->params->smode.cadence		<< 6 |
				s->hrm->params->smode.altitude		<< 5 |
				s->hrm->params->smode.power		<< 4 |
				s->hrm->params->smode.power_balance	<< 3 |
				s->hrm->params->smode.power_index	<< 2 |
				s->hrm->params->smode.power_hrcc	<< 1 |
				s->hrm->params->smode.unit );
  hrm_data_strstr(fp,	  "Date",	s->hrm->params->date);
  hrm_data_strstr(fp,	  "StartTime",	s->hrm->params->starttime);
  hrm_printf(fp, "Length=%d:%02d:%02d.%d",  s->hrm->params->length.hours,
					      s->hrm->params->length.minutes,
					      s->hrm->params->length.seconds,
					      s->hrm->params->length.tenths);
  hrm_data_strint(fp,	  "Interval",	s->hrm->params->interval);
  hrm_data_strint(fp,	  "Upper1",	s->hrm->params->upper[0]);
  hrm_data_strint(fp,	  "Lower1",	s->hrm->params->lower[0]);
  hrm_data_strint(fp,	  "Upper2",	s->hrm->params->upper[1]);
  hrm_data_strint(fp,	  "Lower2",	s->hrm->params->lower[1]);
  hrm_data_strint(fp,	  "Upper3",	s->hrm->params->upper[2]);
  hrm_data_strint(fp,	  "Lower3",	s->hrm->params->lower[2]);
  hrm_data_strstr(fp,	  "Timer1",	s->hrm->params->timer1);
  hrm_data_strstr(fp,	  "Timer2",	s->hrm->params->timer2);
  hrm_data_strstr(fp,	  "Timer3",	s->hrm->params->timer3);
  hrm_data_strint(fp,	  "ActiveLimit",s->hrm->params->activelimit);
  hrm_data_strint(fp,	  "MaxHR",	s->hrm->params->maxhr);
  hrm_data_strint(fp,	  "RestHR",	s->hrm->params->resthr);
  hrm_data_strint(fp,	  "StartDelay",	s->hrm->params->startdelay);
  hrm_data_strint(fp,	  "VO2max",	s->hrm->params->vo2max);
  hrm_data_strint(fp,	  "Weight",	s->hrm->params->weight);
  hrm_spacer(fp);
}

static void hrm_put_note(FILE * fp, main_s * s)
{
  hrm_header(fp, "Note");
  hrm_string(fp, "Hrm file created with " PACKAGE_NAME " v" PACKAGE_VERSION); 
  hrm_printf(fp, "Training name: %8s\n", s->hrm->trainingname);
  hrm_spacer(fp);
}

static void hrm_put_inttimes(FILE * fp, main_s * s)
{
  int lap;
  hrm_header(fp, "IntTimes");
  
  /* only process if this exercise contains interval data or more than 1 lap */
  if(s->has_interval || s->hrm->inttimes->laps > 1)
  {

    /* print lapcount laps */
    for(lap = 0; lap < s->hrm->inttimes->laps; lap++)
    {
      hrm_printf(fp, "%s\t%d\t%d\t%d\t%d",	  
	  s->hrm->inttimes->lap[lap].time,
	  s->hrm->inttimes->lap[lap].HR,
	  s->hrm->inttimes->lap[lap].HRmin,
	  s->hrm->inttimes->lap[lap].HRavg,
	  s->hrm->inttimes->lap[lap].HRmax
	  );
      hrm_printf(fp, "%d\t%d\t%d\t%d\t%d\t%d",
	  s->hrm->inttimes->lap[lap].flags,
	  s->hrm->inttimes->lap[lap].rec_time,
	  s->hrm->inttimes->lap[lap].rec_hr,
	  s->hrm->inttimes->lap[lap].speed,
	  s->hrm->inttimes->lap[lap].cadence,
	  s->hrm->inttimes->lap[lap].altitude
	  );
      hrm_printf(fp, "%d\t%d\t%d\t%d\t%d",
	  s->hrm->inttimes->lap[lap].extra1,
	  s->hrm->inttimes->lap[lap].extra2,
	  s->hrm->inttimes->lap[lap].extra3,
	  s->hrm->inttimes->lap[lap].ascent,
	  s->hrm->inttimes->lap[lap].distance
	  );
      hrm_printf(fp, "%d\t%d\t%d\t%d\t%d\t%d",
	  s->hrm->inttimes->lap[lap].laptype,
	  s->hrm->inttimes->lap[lap].lapdistance,
	  s->hrm->inttimes->lap[lap].power,
	  s->hrm->inttimes->lap[lap].temperature,
	  s->hrm->inttimes->lap[lap].phaselap,
	  0			/* reserved */
	  );
      hrm_printf(fp, "%d\t%d\t%d\t%d\t%d\t%d",
	  0, 0, 0, 0, 0, 0	/* reserved */
	  );
    }
    hrm_spacer(fp);
  }
}

static void hrm_put_intnotes(FILE * fp, main_s * s)
{
  /* only process if this exercise contains interval data or more than 1 lap */
  if(s->has_interval || s->hrm->inttimes->laps > 1)
  {
    hrm_header(fp, "IntNotes");
    hrm_spacer(fp);
  }
}

static void hrm_put_extradata(FILE * fp, main_s * s)
{
  hrm_header(fp, "ExtraData");
  hrm_spacer(fp);
}
  
static void hrm_put_hrlimits_sum(FILE * fp, main_s * s)
{
  int i;
  hrm_header(fp, "Summary-123");

  /* loop 3 limits */
  for(i=0; i < 3; i++)
  {
    hrm_printf(fp, "%d\t%d\t%d\t%d\t%d\t%d",
	s->hrm->hrsum->limit[i].totalsec,
	s->hrm->hrsum->limit[i].t_max_hr,
	s->hrm->hrsum->limit[i].t_ul_hr_max,
	s->hrm->hrsum->limit[i].t_ll_hr_ul,
	s->hrm->hrsum->limit[i].t_rest_hr_ll,
	s->hrm->hrsum->limit[i].t_hr_rest
	);
    hrm_printf(fp, "%d\t%d\t%d\t%d",
	s->hrm->hrsum->limit[i].max,
	s->hrm->hrsum->limit[i].upper,
	s->hrm->hrsum->limit[i].lower,
	s->hrm->hrsum->limit[i].rest
	);
  }
  hrm_printf(fp, "%d\t%d",
      s->hrm->hrsum->startsample,
      s->hrm->hrsum->stopsample
      );
  hrm_spacer(fp);
}

static void hrm_put_hrTH_sum(FILE * fp, main_s * s)
{
  int i;
  hrm_header(fp, "Summary-TH");

  /* read 4th array */
  for(i=3; i < 4; i++)
  {
    hrm_printf(fp, "%d\t%d\t%d\t%d\t%d\t%d",
	s->hrm->hrsum->limit[i].totalsec,
	s->hrm->hrsum->limit[i].t_max_hr,
	s->hrm->hrsum->limit[i].t_ul_hr_max,
	s->hrm->hrsum->limit[i].t_ll_hr_ul,
	s->hrm->hrsum->limit[i].t_rest_hr_ll,
	s->hrm->hrsum->limit[i].t_hr_rest
	);
    hrm_printf(fp, "%d\t%d\t%d\t%d",
	s->hrm->hrsum->limit[i].max,
	s->hrm->hrsum->limit[i].upper,
	s->hrm->hrsum->limit[i].lower,
	s->hrm->hrsum->limit[i].rest
	);
  }
  hrm_printf(fp, "%d\t%d",
      s->hrm->hrsum->startsample,
      s->hrm->hrsum->stopsample
      );
  hrm_spacer(fp);
}


static void hrm_put_trip(FILE * fp, main_s * s)
{
  /* only process if exercise has speed data */
  if(s->has_speed)
  {
    hrm_header(fp, "Trip");
    hrm_data_int(fp, s->hrm->trip->distance);
    hrm_data_int(fp, s->hrm->trip->ascent);
    hrm_data_int(fp, s->hrm->trip->totalsec);
    hrm_data_int(fp, s->hrm->trip->avg_altitute);
    hrm_data_int(fp, s->hrm->trip->max_altitute);
    hrm_data_int(fp, s->hrm->trip->avg_speed);
    hrm_data_int(fp, s->hrm->trip->max_speed);
    hrm_data_int(fp, s->hrm->trip->odometer);
    hrm_spacer(fp);
  }
}

static void hrm_put_hrzones(FILE * fp, main_s * s)
{
  int i;
  hrm_header(fp, "HRZones");
  i = 0;
  for(i=0; i < 11; i++)
  {
    hrm_data_int(fp, s->hrm->hrzones->hr[i]);
  }
  hrm_spacer(fp);
}

static void hrm_put_swaptimes(FILE * fp, main_s * s)
{
  hrm_header(fp, "SwapTimes");
  hrm_spacer(fp);
}

static void hrm_put_hrdata(FILE * fp, main_s * s)
{
  int i;
  hrm_header(fp, "HRData");

  // HACK: double print first row -> start at -1
  for(i=-1; i < s->hrm->samplecount; i++)
  {
    if(s->has_speed)
    {
      hrm_printf(fp, "%d\t%d",
	s->hrm->hrdata->hr[max(i, 0)],
	s->hrm->hrdata->speed[max(i, 0)]);
    }
    else
    {
      hrm_printf(fp, "%d",
	s->hrm->hrdata->hr[max(i, 0)]);
    }
  }
  hrm_spacer(fp);
}


/* 
 * Write all hrm sections to file fp
 */
void writehrm_data(FILE * fp, main_s * s)
{
  hrm_put_params(fp, s);	
  hrm_put_note(fp, s);
  hrm_put_inttimes(fp, s);
  hrm_put_intnotes(fp, s);
  hrm_put_extradata(fp, s);
  hrm_put_hrlimits_sum(fp, s);
  hrm_put_hrTH_sum(fp, s);
  hrm_put_hrzones(fp, s);
  hrm_put_swaptimes(fp, s);
  hrm_put_trip(fp, s);
  hrm_put_hrdata(fp, s);
}

/* 
 * Close the file
 */
void writehrm_close(FILE * fp)
{
  // TODO error checking
  fclose(fp);
}

/* 
 * Free alloc'd mem
 */
void writehrm_free(main_s * s)
{
  /* not a thing to do (yet) */
}

