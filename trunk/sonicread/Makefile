TARGET  = SonicRead

SRC = CreateHsr.java \
      SonicLink.java \
      CaptureAudio.java \
      ${TARGET}.java
OBJ = ${addsuffix .class, ${basename ${SRC}}}

all:	${OBJ}

run:	${OBJ}
	java ${TARGET}

%.class: %.java
	javac $<

clean:
	rm -rf ${OBJ}
