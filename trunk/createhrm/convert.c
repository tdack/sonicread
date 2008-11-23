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

#include "convert.h"

/* MACRO - hexadecimal to decimal converter (1byte only!!!) */
#define hex2dec(d) ( ((d) & 0x0F) + 10*((d) >> 4) )

/* Interval values used by Polar */
static int interval[8] = { 5, 15, 30, 60, 120, 240, 300, 480 };

static unsigned char array_mean(unsigned char * data, int length)
{
  if(!length)
    return 0;

  int i, sum = 0;
  for(i=0; i < length; i++)
    sum += data[i];
  return (char)( (double)(sum / length) + 0.5);
}

static unsigned char array_min(unsigned char * data, int length)
{
  int i;
  unsigned char rval = 255;
  for(i=0; i < length; i++)
    rval = min(rval, data[i]);
  return rval;
}

static unsigned char array_max(unsigned char * data, int length)
{
  int i;
  unsigned char rval = 0;
  for(i=0; i < length; i++)
    rval = max(rval, data[i]);
  return rval;
}

static double intarray_mean(unsigned int * data, int length)
{
  if(!length)
    return 0;

  int i, sum = 0;
  for(i=0; i < length; i++)
    sum += data[i];
  return (double)( (double)sum / length);
}

static int array_compare(unsigned char * data, int length, 
    unsigned char compvalue, char compchar)
{
  int i, rval = 0;
  for(i=0; i < length; i++)
  {
    switch(compchar)
    {
      case '>':
	if(data[i] > compvalue)
	  rval++;
	break;
      case '=':
	if(data[i] == compvalue)
	  rval++;
	break;
      case '<':
	if(data[i] < compvalue)
	  rval++;
	break;
    }
  }
  return rval;
}

static unsigned char sdata(main_s * s, int section, int index)
{
  /* limit index to 60 - increment section if needed */
  section += index/60;
  index %= 60;

  if(section >= s->sectioncount)
  {
    fprintf(stderr, "Error! Section %d does not exist!\n", section);
    exit(1);
  } 
  if(index >= s->section[section]->size)
  {
    fprintf(stderr, "Error! Section %d data index %d exeeds size!\n", section, index);
    exit(1);
  }

  return s->section[section]->data[index];
}

/*
 * Main config - polar type
 * needed by (all) other convert_xxx functions 
 */
static void convert_main(main_s * s)
{
  int i;
  static char *charset = "0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-%/()*+.:?";

  /* Set watch type - TODO */
  s->polartype = POLAR_S510;

  /* Set number of samples */
  s->hrm->samplecount = sdata(s, 1, 0);
  /* Set Interval Rate */
  s->sec_per_sample = interval[sdata(s, 1, 1) - 95];
  
  /* Set number of laps/measurements */
  s->hrm->measurecnt = hex2dec(sdata(s, 1, 21));
  s->hrm->lapcnt = hex2dec(sdata(s, 1, 22));

  /* debug info */
  DBG("samplecount: %d", s->hrm->samplecount);
  DBG("measurecnt: %d", s->hrm->measurecnt);
  DBG("lapcount: %d", s->hrm->lapcnt);

  /* Read exercise name */
  if(sdata(s, 1, 2))
  {
    for(i=0; i <= 6; i++)
      s->hrm->trainingname[i] = charset[sdata(s, 1, 3+i)];
  }
  else
  {
    sprintf(s->hrm->trainingname, "BasicUse");
  }

  /* check if we need to process the speed and intervals */
  switch(s->polartype)
  {
    case POLAR_S510:
      s->has_speed = ((sdata(s, 1, 26) && 0x01) == 0x01) |  /* bike 1 */
		     ((sdata(s, 1, 26) && 0x02) == 0x02);   /* or bike 2 */
      s->has_interval = s->hrm->measurecnt != s->hrm->lapcnt;
      break;
    // case OTHER_MODEL
  }
}

static void convert_params(main_s * s)
{
  /* allocate memory */
  if((s->hrm->params = malloc(sizeof(hrm_params_s))) == NULL)
  {
    fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
    exit(EXIT_FAILURE);
  }

  /* make shortcut */
  hrm_params_s * par = s->hrm->params;

  /* version - static */
  par->version = 106;

  /* monitor - static ?? */
  par->monitor = 10;

  /* smode - TODO 
   * No support for cadence, altitude, power and units
   */
  par->smode.speed	  = s->has_speed;
  par->smode.cadence	  = 0;
  par->smode.altitude	  = 0;
  par->smode.power	  = 0;
  par->smode.power_balance= 0;
  par->smode.power_index  = 0;
  par->smode.power_hrcc	  = s->has_speed; /* if has speed I guess.. */
  par->smode.unit	  = 0;

  /* date - assume year > 2000 */
  if((par->date = malloc(9*sizeof(char))) == NULL)
  {
    fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
    exit(EXIT_FAILURE);
  }
  sprintf(par->date, "%4d%02d%02x", 
      2000 + sdata(s, 1, 14), 
      sdata(s, 1, 15) & 0x0F, 
      sdata(s, 1, 13));
  
  /* starttime - row 26 tot 29 - TODO second tenths */
  if((par->starttime = malloc(11*sizeof(char))) == NULL)
  {
    fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
    exit(EXIT_FAILURE);
  }

  /* starttime */
  sprintf(par->starttime, "%02x:%02x:%02x.0",
      sdata(s, 1, 12),
      sdata(s, 1, 11),
      sdata(s, 1, 10));

  /* length - row 32 tot 35 */
  par->length.hours = hex2dec(sdata(s, 1, 18));
  par->length.minutes = hex2dec(sdata(s, 1, 17));
  par->length.seconds = hex2dec(sdata(s, 1, 16));
  par->length.tenths = hex2dec(sdata(s, 1, 15) >> 4);

  /* interval - the number of seconds between 2 samples */
  par->interval = s->sec_per_sample;

  /* upper and lower limits */
  par->lower[0] = sdata(s, 1, 28);
  par->upper[0] = sdata(s, 1, 29);
  par->lower[1] = sdata(s, 1, 30);
  par->upper[1] = sdata(s, 1, 31);
  par->lower[2] = 0;
  par->upper[2] = 0;

  /* times - UNSUPORTED - set to zero */
  par->timer1 = strdup("0:00:00.0");
  par->timer2 = strdup("0:00:00.0");
  par->timer3 = strdup("0:00:00.0");

  /* activelimit - UNSUPORTED - set to zero */
  par->activelimit = 0;

  /* startdelay - TODO */
  par->startdelay = 0;

  /* Energy wasted */
  par->kcal = (hex2dec(sdata(s, 2, 11))*1000) + 
	      (hex2dec(sdata(s, 2, 10))*10) + 
	      (hex2dec(sdata(s, 2, 9))/10);
  
  /* copy user config */
  par->maxhr  = s->config->maxhr;
  par->resthr = s->config->resthr;
  par->vo2max = s->config->vo2max;
  par->weight = s->config->weight;

  /* Debug info */
  DBG("date: %s ", par->date);
  DBG("starttime: %s ", par->starttime);
  DBG("kcal: %d ", par->kcal);
}

/*
 * Free memory allocated for convert_params
 */
static void convert_params_free(main_s * s)
{
  free(s->hrm->params->date);
  free(s->hrm->params->starttime);
  free(s->hrm->params->timer1);
  free(s->hrm->params->timer2);
  free(s->hrm->params->timer3);
  free(s->hrm->params);
}


/* 
 * Convert lap times 
 */
static void convert_inttimes(main_s * s)
{
  /* alloc mem */
  if((s->hrm->inttimes = malloc(sizeof(hrm_inttimes_s))) == NULL)
  {
    fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
    exit(EXIT_FAILURE);
  }

  /* init vars */
  hrm_inttimes_s * ii = s->hrm->inttimes;
  hrm_hrdata_s   * hrd = s->hrm->hrdata;
  int l;		/* lap index */
  int startsec, seccnt;	/* section start index, section count */
  int size;		/* number of bytes per lap */

  /* determine how large each inttime subsection is */
  if(s->has_interval)
    size = 16;
  else
    size = 11;

  /* determine in which section the lap data starts */
  startsec = 4 + (int)((s->hrm->samplecount-1)/60);
  if(s->has_speed) /* add offset for speed data */
    startsec += 1 + (int)((s->hrm->samplecount-1)/60);
  
  /* determine in how _many_ sections the lap data resides */
  seccnt = (int)(((double)(s->hrm->measurecnt * size)/60) + .9999); 

  /* check section size */
  if(s->section[startsec+seccnt-1]->size != (s->hrm->measurecnt * size) % 60 )
  {
    fprintf(stderr, "ERROR! Lap-data section (%d) has wrong size "
	"(%d instead of %d!)\n", 
	startsec+seccnt-1, s->section[startsec+seccnt-1]->size, (s->hrm->measurecnt * size)%60);
    //exit(EXIT_FAILURE);
  }

  /* set lap count in inttimes struct */
  ii->laps = s->hrm->measurecnt;
  if((ii->lap = malloc(ii->laps * sizeof(*(ii->lap)))) == NULL)
  {
    fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
    exit(EXIT_FAILURE);
  }

  /* loop laps */
  for(l = 0; l < ii->laps; l++)
  {
    int os = l * size;  /* data offset */
    int sec = startsec; /* current section */
    int startsample; 	/* lap data starts at sample: */
    int stopsample;	/* lap data stop at sample: */
    int samplecount;	/* number of samples */
      
    DBG("processing lap %d(%d)", l+1, ii->laps);

    /* row 1 */	
    { /* read time */
      int hh  = sdata(s, sec, os+2);
      int mm = sdata(s, sec, os+1) & 0x3F;
      int ss = sdata(s, sec, os+0) & 0x3F;
      int tenth = 4 * (sdata(s, sec, os+1) >> 6) + (sdata(s, sec, os+0) >> 6);
      sprintf(ii->lap[l].time, "%d:%02d:%02d.%1d", hh, mm, ss, tenth);
      ii->lap[l].totalsec = 3600*hh + 60*mm + ss; /* +round(tenths) needed? */

      /* overwrite last lap time with the last sample time */
      if(l == ii->laps - 1)
      {
	int totaltime = s->hrm->samplecount * s->sec_per_sample;
	hh = (int)totaltime / 3600;
	mm = (int)((totaltime % 3600) / 60);
	ss = (int)totaltime % 60;
	tenth = 0;
	sprintf(ii->lap[l].time, "%d:%02d:%02d.%1d", hh, mm, ss, tenth);
	ii->lap[l].totalsec = 3600*hh + 60*mm + ss; /* +round(tenths) needed? */
      }
    }
  
    /* set set sample numbers */
    startsample = ii->lap[max(l-1, 0)].totalsec / s->sec_per_sample;
    stopsample  = ii->lap[l].totalsec / s->sec_per_sample - 1;
    
    /* overwrite start/stop at first/last lap */
    if(l == 0)
      startsample = 0;
   // else if(l == ii->laps - 1) /* huh?! niet nodig! */
    //  stopsample  = ii->lap[l].totalsec / s->sec_per_sample;
    
    /* calculate number of samples */
    samplecount = stopsample - startsample + 1;

    /* set hr data */
    ii->lap[l].HR	= sdata(s, sec, os+3);
    ii->lap[l].HRmin	= array_min(hrd->hr + startsample, samplecount);
    ii->lap[l].HRavg	= array_mean(hrd->hr + startsample, samplecount);
    ii->lap[l].HRmax	= array_max(hrd->hr + startsample, samplecount);

    /* overwrite momentary hearth-rate at last lap with last hrsample */
    if(l == ii->laps - 1)
      ii->lap[l].HR	= hrd->hr[stopsample];

    /* row 2 */ 
    if(s->has_interval)
    {
      ii->lap[l].flags	  = (sdata(s, sec, os+15) >> 4) > 0;
      ii->lap[l].rec_time = sdata(s, sec, os+13) * 60 + sdata(s, sec, os+12);
      ii->lap[l].rec_hr	  = ( (sdata(s, sec, os+14) < 254 ) ? sdata(s, sec, os+14) : 0);
    }
    else
    {
      ii->lap[l].flags	  = 0;
      ii->lap[l].rec_time = 0;
      ii->lap[l].rec_hr	  = 0;
    }
    if(s->has_speed)
    {
      /* speed = round( (MSByte + LSByte) * 0.625 ) */
      int speed;
      speed  = sdata(s, sec, os+9);
      speed += sdata(s, sec, os+10) << 8;
      ii->lap[l].speed	= (int)(((double)speed * 5/8) + 0.5);
    }
    else
      ii->lap[l].speed= 0;
    ii->lap[l].cadence	= 0;
    ii->lap[l].altitude	= 0;
    /* row 3 */
    ii->lap[l].extra1	= 0;
    ii->lap[l].extra2	= 0;
    ii->lap[l].extra3	= 0;
    ii->lap[l].ascent	= 0;
    ii->lap[l].distance	= 0;
    /* row 4 */
    ii->lap[l].laptype	  = (sdata(s, sec, os+10) >> 4) > 0;

    if(s->has_speed)
    { /* calculate lap-distance (in meters) with speed samples */
      double avgspeed = intarray_mean(hrd->speed + startsample, samplecount);
      avgspeed /= 10;  /* convert to km/u */
      avgspeed /= 3.6; /* convert to m/s */
      ii->lap[l].lapdistance = (int)(avgspeed * samplecount * s->sec_per_sample 
	  + .5 );      /* calculated distance (+.5 = round helper) */
    }
    else
      ii->lap[l].lapdistance = 0;

    ii->lap[l].power	  = 0;
    ii->lap[l].temperature= 0;
    ii->lap[l].phaselap	  = 0;
  }
}

/*
 * Free memory allocated for convert_inttimes
 */
static void convert_inttimes_free(main_s * s) 
{ 
  free(s->hrm->inttimes->lap);
  free(s->hrm->inttimes); 
}


/* 
 * Convert HR limit summary 
 */
static void convert_hrsum(main_s * s)
{
  /* alloc mem */
  if((s->hrm->hrsum = malloc(sizeof(hrm_hrsum_s))) == NULL)
  {
    fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
    exit(EXIT_FAILURE);
  }

  /* init vars */
  int i;
  hrm_hrsum_s * l = s->hrm->hrsum;

  /* loop 3+1 limits */
  for(i=0; i < 3+1; i++)
  {
    /* duplicate params */
    l->limit[i].max   = s->hrm->params->maxhr;
    l->limit[i].rest  = s->hrm->params->resthr;
    if(i < 3) /* only for the thee limits */
    {
      l->limit[i].upper = s->hrm->params->upper[i];
      l->limit[i].lower = s->hrm->params->lower[i];
    }
    else
    {
      l->limit[i].upper = 0; // TODO - thresholds are config?!
      l->limit[i].lower = 0;
    }

    /* init values */
    l->limit[i].totalsec = 0;
    l->limit[i].t_max_hr = 0;
    l->limit[i].t_ul_hr_max = 0;
    l->limit[i].t_ll_hr_ul = 0;
    l->limit[i].t_rest_hr_ll = 0;
    l->limit[i].t_hr_rest = 0;

    /* only execute when the following calculations make sense ;) */
    if(l->limit[i].upper < l->limit[i].max)
    {
      /* hrdata > max */
      l->limit[i].t_max_hr = array_compare(s->hrm->hrdata->hr, s->hrm->samplecount,
	  l->limit[i].max, '>');
      l->limit[i].totalsec += l->limit[i].t_max_hr;

      /* hrdata > upper AND hrdata > max */
      l->limit[i].t_ul_hr_max = array_compare(s->hrm->hrdata->hr, s->hrm->samplecount,
	  l->limit[i].upper, '>');
      l->limit[i].t_ul_hr_max -= l->limit[i].t_max_hr;
      l->limit[i].totalsec += l->limit[i].t_ul_hr_max;

      /* hrdata >= lower AND hrdata > upper */
      l->limit[i].t_ll_hr_ul = array_compare(s->hrm->hrdata->hr, 
	  s->hrm->samplecount, l->limit[i].lower, '>');
      l->limit[i].t_ll_hr_ul += array_compare(s->hrm->hrdata->hr, 
	  s->hrm->samplecount, l->limit[i].lower, '=');
      l->limit[i].t_ll_hr_ul -= l->limit[i].t_ul_hr_max;
      l->limit[i].t_ll_hr_ul -= l->limit[i].t_max_hr;
      l->limit[i].totalsec += l->limit[i].t_ll_hr_ul;

      /* hrdata < rest */
      l->limit[i].t_hr_rest = array_compare(s->hrm->hrdata->hr, s->hrm->samplecount, 
	  l->limit[i].rest, '<');
      l->limit[i].totalsec += l->limit[i].t_hr_rest;

      /* hrdata > rest AND rest < lower limit */
      l->limit[i].t_rest_hr_ll = array_compare(s->hrm->hrdata->hr, 
	  s->hrm->samplecount, l->limit[i].lower, '<');
      l->limit[i].t_rest_hr_ll -= l->limit[i].t_hr_rest;
      l->limit[i].totalsec += l->limit[i].t_rest_hr_ll;

      /* convert from number of samples to seconds */
      l->limit[i].t_max_hr *= s->sec_per_sample;
      l->limit[i].t_ul_hr_max *= s->sec_per_sample;
      l->limit[i].t_ll_hr_ul *= s->sec_per_sample;
      l->limit[i].t_rest_hr_ll *= s->sec_per_sample;
      l->limit[i].t_hr_rest *= s->sec_per_sample;
      l->limit[i].totalsec *= s->sec_per_sample;

    }
  } /* endof for loop */

  /* set start/stop sample */
  l->startsample = 0;
  l->stopsample = s->hrm->samplecount;
}

/*
 * Free memory allocated for convert_hrsum
 */
static void convert_hrsum_free(main_s * s) 
{
  free(s->hrm->hrsum);
}

/*
 * Convert Cycling trip data
 */
static void convert_trip(main_s * s)
{
  /* alloc mem */
  if((s->hrm->trip = malloc(sizeof(hrm_trip_s))) == NULL)
  {
    fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
    exit(EXIT_FAILURE);
  }

  /* init vars */
  int i, l; /* index */
  hrm_trip_s * trip = s->hrm->trip;
  hrm_inttimes_s * ii = s->hrm->inttimes;

  /* init values */
  trip->distance      = 0;
  trip->ascent	      = 0;
  trip->totalsec      = 0;
  trip->avg_altitute  = 0;
  trip->max_altitute  = 0;
  trip->avg_speed     = 0;
  trip->max_speed     = 0;
  trip->odometer      = 0;

  /* calculate speed-related values */
  if(s->has_speed)
  {
    /* calculate total distance to hectometers */
    trip->distance = (((int) sdata(s, 2, 24)) + ((int) sdata(s, 2, 25) << 8 ));
  
    for(i=0; i < s->hrm->samplecount; i++)
    {
      trip->avg_speed += s->hrm->hrdata->speed[max(i, 0)];
      trip->max_speed = max(s->hrm->hrdata->speed[max(i, 0)], trip->max_speed);
    }
  
    trip->avg_speed += 5;
    trip->avg_speed /= 10; /* to km/u */
    trip->avg_speed /= s->hrm->samplecount; /* average */
    trip->avg_speed *= 128; /* to hrm format */
      
    trip->avg_speed = intarray_mean(s->hrm->hrdata->speed, s->hrm->samplecount);
  
    trip->max_speed  = sdata(s, 2, 28) * 128;
    trip->max_speed += sdata(s, 2, 27) / 2;
  }


  /* total trip time */
  trip->totalsec += s->hrm->params->length.hours * 3600;
  trip->totalsec += s->hrm->params->length.minutes * 60;
  trip->totalsec += s->hrm->params->length.seconds;
  trip->totalsec += (s->hrm->params->length.tenths + 5) / 10;

  // TODO - add support for altitude 

  /* odometer - byte 21, 22 and 23 - convert from hexadecimal to decimal */
  trip->odometer  =	    hex2dec(sdata(s, 2, 21));
  trip->odometer += 100	  * hex2dec(sdata(s, 2, 22));
  trip->odometer += 10000 * hex2dec(sdata(s, 2, 23));
  
  /* debug info */
  DBG("convert_trip data:");
  DBG("ODO: %d", trip->odometer);
}

/*
 * Free memory allocated for convert_trip
 */
static void convert_trip_free(main_s * s)
{
  free(s->hrm->trip);
} 

/* 
 * Convert HR zones used for this exercise - UNSUPPORTED!
 */
static void convert_hrzones(main_s * s)
{
  s->hrm->params->version = 106;

  /* alloc mem */
  if((s->hrm->hrzones = malloc(sizeof(hrm_hrzones_s))) == NULL)
  {
    fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
    exit(EXIT_FAILURE);
  }

  /* init vars */
  int i;

  /* enter hrzones */
  for(i=0; i < 11; i++)
    s->hrm->hrzones->hr[i] = 0;
}

/*
 * Free memory allocated for convert_hrzones
 */
static void convert_hrzones_free(main_s * s)
{
  free(s->hrm->hrzones);
}


/*
 * Convert Heart Rate Data
 *
 * Read from section 3 and, if speed enabled, section 4
 * All decimal values. Speeds has to be multiplied by 5
 */
void convert_hrdata(main_s * s)
{
  /* allocate mem */
  if((s->hrm->hrdata = malloc(sizeof(hrm_hrdata_s))) == NULL)
  {
    fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
    exit(EXIT_FAILURE);
  }

  /* init vars */
  int i, hrsec, spdsec;
  hrm_hrdata_s * hrd = s->hrm->hrdata;

  /* alloc mem */
  if((hrd->hr = malloc(s->hrm->samplecount * sizeof(*(hrd->hr)))) == NULL)
  {
    fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
    exit(EXIT_FAILURE);
  }

  if(s->has_speed)
    if((hrd->speed = malloc(s->hrm->samplecount * sizeof(*(hrd->speed)))) == NULL)
  {
    fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
    exit(EXIT_FAILURE);
  }


  /* set sections - updated if samplecount rises above 60 */
  hrsec = 3;
  spdsec = 4 + (int)(s->hrm->samplecount/60);

  /* read */
  for(i=0; i < s->hrm->samplecount; i++)
  {
    hrd->hr[i] = sdata(s, hrsec, i);
    if(s->has_speed)
      hrd->speed[i] = sdata(s, spdsec, i) * 5;
  }
}

/*
 * Free memory allocated for convert_data
 */
static void convert_hrdata_free(main_s * s)
{
  if(s->has_speed)
    free(s->hrm->hrdata->speed);
  free(s->hrm->hrdata->hr);
  free(s->hrm->hrdata);
}



/*
 * Convert raw data to hrm structs 
 */
void convert(main_s * s)
{
  /* initialize memory */
  if((s->hrm = malloc(sizeof(*(s->hrm)))) == NULL)
  {
    fprintf(stderr, "ERROR! Memory allocation error at %s:%d\n", __FILE__, __LINE__);
    exit(EXIT_FAILURE);
  }

  /* Convert data */
  convert_main(s);
  convert_params(s);
  convert_hrdata(s);
  convert_inttimes(s);
  convert_hrsum(s);
  convert_trip(s);
  convert_hrzones(s);

  /*
  hrm_header(fp, "Params");
  hrm_data_int(fp, "Version", s->hrm->params->version);
  hrm_data_int(fp, "Monitor", s->hrm->params->monitor);
  hrm_data_int(fp, "Mode", s->hrm->params->mode);
  hrm_data_int(fp, "SMode", s->hrm->params->smode);
  hrm_data_str(fp, "Date", s->hrm->params->date, sizeof(s->hrm->params->date));
  */
}

/* 
 * Free allocated memory
 */
void convert_free(main_s * s)
{
  convert_params_free(s);
  convert_hrdata_free(s);
  convert_inttimes_free(s);
  convert_hrzones_free(s);
  convert_hrsum_free(s);
  convert_trip_free(s);
  free(s->hrm);
}
