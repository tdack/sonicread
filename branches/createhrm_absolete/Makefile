CFLAGS = -Wall -g -O0
CC=gcc

OBJS=main.o writehrm.o rawdataread.o convert.o config.o

all: 	createhrm

createhrm:	${OBJS}
	@echo compiling $@
	@${CC} -o $@ $^ 

main.o:	main.c writehrm.h createhrm.h
	@echo compling $@
	@${CC} -c ${CFLAGS} $<

%.o:	%.c %.h  createhrm.h
	@echo compling $@
	@${CC} -c ${CFLAGS} $<

install:	createhrm
	cp createhrm /usr/local/bin/

clean:
	rm -f *.o *~ createhrm

edit:	
	vi -p main.c createhrm.h writehrm.c convert.c
