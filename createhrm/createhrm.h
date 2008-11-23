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

#ifndef _CREATEHRM_H
#define _CREATEHRM_H

/*
 * createhrm macro's
 */
#include <stdarg.h>
#if defined(DEBUG) && (DEBUG > 0)
#define DBG(...) {							      \
      fprintf(stderr, "DBG[%s:%d] %s(): ", __FILE__, __LINE__, __FUNCTION__); \
      fprintf(stderr, __VA_ARGS__);                                           \
      fprintf(stderr, "\n");                                                  \
}
#else
#define DBG(...) {}
#endif
#define min(a,b) ( ( a ) < ( b ) ? ( a ) : ( b ) )
#define max(a,b) ( ( a ) > ( b ) ? ( a ) : ( b ) )

/*
 * local defines 
 */
#define PACKAGE_NAME		"polar_createhrm"
#define PACKAGE_VERSION		"0.0.1"
#define PACKAGE_BUGREPORT	"remco@vioco.nl"
#define EXIT_SUCCESS  0
#define EXIT_FAILURE  1
#define TRUE	1
#define FALSE	0

/*
 * supported polar types
 */
enum {
  POLAR_S510 = 0
}; 

/*
 * error codes
 */
enum {
  E_SUCCESS = 0,
  E_NOACCESS,
  E_NOFILE,
  E_ERROR		/* global error */
};

/* 
 * section struct
 */
typedef struct section_s_tag {
  int number;
  int size;

  int index;
  unsigned char * data;
} section_s;

/*
 * hrm data - Params 
 */
typedef struct hrm_params_s_tag {
  int	version;
  int	monitor;
  int	mode;
  struct {
    int speed;
    int cadence;
    int altitude;
    int power;
    int power_balance;
    int power_index;
    int power_hrcc;
    int unit;
  } smode;
  char *date;		/* length: 8 chars */
  char *starttime;	/* length: 10 chars */
  struct {
    int hours;
    int minutes;
    int seconds;
    int tenths;
  } length;
  //char *length;		/* length: 10 chars */
  int	interval;
  int	upper[3];
  int	lower[3];
  char *timer1;		/* length: 5 chars */
  char *timer2;		/* length: 5 chars */
  char *timer3;		/* length: 5 chars */
  int	activelimit;
  int	maxhr;
  int	resthr;
  int	startdelay;
  int	vo2max;
  int	weight;
  int	kcal;
} hrm_params_s;

/* 
 * hrm data - Lap TImes 
 */
typedef struct hrm_inttimes_tag { 
  int laps;

  struct { 
    int totalsec;	/* global */
    char time[9];	/* row 1 */
    int HR;
    int HRmin;
    int HRavg;
    int HRmax;
    int flags;		/* row 2 */
    int rec_time;
    int rec_hr;
    int speed;
    int cadence;
    int altitude;
    int extra1;		/* row 3 */
    int extra2;
    int extra3;
    int ascent;
    int distance;
    int laptype;	/* row 4 */
    int lapdistance;
    int power;
    int temperature;
    int phaselap;
  } * lap;
} hrm_inttimes_s;

/*
 * hrm data - HR Limit and Threshold Summary 
 */
typedef struct hrm_hrsum_tag {
  int startsample;
  int stopsample;
  struct {
    int max;
    int upper;
    int lower;
    int rest;
    int totalsec;
    int t_max_hr;	/* max < x	 */
    int t_ul_hr_max;	/*  ul < x < max */
    int t_ll_hr_ul;	/*  ll < x < ul  */
    int t_rest_hr_ll;	/* rst < x < ll  */
    int t_hr_rest;	/*       x < rst */
  } limit[4];		/* 3 limits and threshold */
} hrm_hrsum_s;

/*
 * hrm data - Cycling parameters 
 */
typedef struct hrm_trip_tag {
  int distance;
  int ascent;
  int totalsec;
  int avg_altitute;
  int max_altitute;
  int avg_speed;
  int max_speed;
  int odometer;
} hrm_trip_s;

/*
 * hrm data- Heart Rate Zones
 */
typedef struct hrm_hrzones_tag {
  unsigned char hr[11];
} hrm_hrzones_s;

/* 
 * hrm data - (Extended) Heart Rate Data
 */
typedef struct hrm_hrdata_tag { 
  unsigned char * hr;
  unsigned int * speed;
  unsigned int * cadence;
} hrm_hrdata_s;

/*
 * global struct 
 **/
typedef struct main_s_tag {
  char * inputfile;
  char * outputfile;
  char * configfile;

  /* main config */
  int polartype;
  int sec_per_sample;
  int has_speed;
  int has_interval;

  /* user configurations */
  struct {
    int maxhr;
    int resthr;
    int vo2max;
    int weight;
  } * config;
  
  /* sections */
  int sectioncount;
  section_s * * section;

  /* hrm data */
  struct {
    int samplecount;
    int measurecnt;
    int lapcnt;
    char trainingname[9];

    /* hrm blocks */
    hrm_params_s    * params;
    hrm_inttimes_s  * inttimes;
    hrm_hrsum_s	    * hrsum;
    hrm_trip_s	    * trip;
    //hrm_hrzones_s   * tmp;
    hrm_hrzones_s   * hrzones;
    hrm_hrdata_s    * hrdata;
  } * hrm;

} main_s;

#endif
