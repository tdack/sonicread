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

#ifndef _WRITEHRM_H
#define _WRITEHRM_H

/* global libraries */
#include <stdio.h>

/* local libraries */
#include "createhrm.h"

unsigned int writehrm_create(FILE * * fp, main_s * s);
void writehrm_data(FILE * fp, main_s * s);
void writehrm_close(FILE * fp);
void writehrm_free(main_s * s);

#endif
