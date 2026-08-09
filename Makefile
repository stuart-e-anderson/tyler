#
# gmake makefile for Tyler
#
# This should work under Linux (redhat 7.3) and cygwin.
#

# prefer older...
POSSIBLE_JAVAROOTS = \
        c:/jdk1.1.8 \
        c:/jdk1.3.1_01 \
        c:/jdk1.3.1_02 \
        c:/j2sdk1.4.0_01 \
        c:/j2sdk1.4.1_01 \
        /usr/local/java \
        /usr/local/jdk1.2.2 \
        /usr/java/jdk1.3.1 \
        /usr/java/jdk1.3.1_03 \
        /usr/java/j2sdk1.4.0 \
        $(NULL)

# Set JAVAROOT to the first existing one of the above...
JAVAROOT := $(shell \
                for d in $(POSSIBLE_JAVAROOTS); do \
                   if [ -f $$d/bin/javac ]; then \
                       echo $$d; \
                       break; \
                   fi \
                done \
            )

ifeq ($(JAVAROOT), )
        # (no tab in the following)
        $(error ERROR: no suitable JAVAROOT found)
endif


# XXX on redhat 7.3,, when jikes doesn't exist, this works
# but shows garbage?
JIKES := $(shell ((jikes -version) 2>&1) | grep -v -q " not found" && echo jikes)

# SEP is ":" on linux, ";" on Windows
SEP := $(shell \
            if [ -d c:/ ]; then \
                echo ";"; \
            else \
                echo ":"; \
            fi \
        )

# Use jikes if it exists, since it's hella fast
ifneq ($(JIKES), )    # if jikes exists...
    CLASSPATH = ${JAVAROOT}/jre/lib/rt.jar
    JAVAC = ${JIKES} -g +P -source 1.1 -target 1.1 -classpath $(CLASSPATH)
else
    JAVAC = $(JAVAROOT)/bin/javac -g -deprecation -target 1.1
endif




Tyler.jar: Tyler.class Makefile
	${JAVAROOT}/bin/jar -cfm Tyler.jar Tyler.mf *.class
Tyler.src.zip: Tyler.jar Tyler.htm Tyler.mf TylerSave.php todo.txt Makefile
	(cd ..; zip tyler/Tyler.src.zip tyler/Tyler.htm tyler/*.java tyler/Tyler.mf tyler/TylerSave.php tyler/todo.txt tyler/Makefile tyler/MakeAll.bat)

Tyler.class: Complex.java MyMath.java Isometry2.java HyperbolicUtils.java Rational.java MatrixMath.java DoubleBufferedCanvas.java SphericalUtils.java AbstractIsometry2.java EuclideanIsometry2.java SphericalIsometry2.java LogScrollbar.java ServerSaveOrLoadDialog.java Tyler.java ClosestArrowFinder.java MyGraphics.java Makefile
	${JAVAC} Tyler.java

.SUFFIXES: .java .class
.java.class:
	${JAVAC} $*.java

.PHONY: clean
clean:
	rm -f *.class *.jar *.zip


#
# A couple more targets, only useful to Don...
#

SENDFILES=Tyler.htm Tyler.jar TylerSave.php TylerSave.php.txt Tyler.src.zip *.class

.PHONY: send
send: Tyler.src.zip
	# (need to send all class files, for browsers using java 1.0)
	ln -s -f TylerSave.php TylerSave.php.txt
	rrr 'sh -c "scp $(SENDFILES) hatch@www.hadron.org:public_html/tylertest"'

.PHONY: supersend
supersend: Tyler.src.zip
	ncftpput -u supermgr van.dreamhost.com superliminal.com/geometry/tyler $(SENDFILES)
.PHONY: supersendtest
supersendtest: Tyler.src.zip
	ncftpput -u supermgr van.dreamhost.com superliminal.com/geometry/tylertest $(SENDFILES)
.PHONY: supersendtest2
supersendtest2: Tyler.src.zip
	ncftpput -u supermgr van.dreamhost.com superliminal.com/geometry/tylertest2 $(SENDFILES)


.PHONY: get
get:
	(mkdir tmp >& /dev/null 2>&1) || true
	curl http://www.superliminal.com/geometry/tyler/Tyler.htm > tmp/Tyler.htm
	curl http://www.superliminal.com/geometry/tyler/Tyler.jar > tmp/Tyler.jar
	curl http://www.superliminal.com/geometry/tyler/Tyler.src.zip > tmp/Tyler.src.zip
	curl http://www.superliminal.com/geometry/tyler/TylerSave.php > tmp/TylerSave.php
	curl http://www.superliminal.com/geometry/tyler/TylerSave.php.txt > tmp/TylerSave.php.txt
	(cd tmp; ${JAVAROOT}/bin/jar -xvf Tyler.jar)
	(cd tmp; ${JAVAROOT}/bin/jar -xvf Tyler.src.zip)
	(mkdir tmptest >& /dev/null 2>&1) || true
	curl http://www.superliminal.com/geometry/tylertest/Tyler.htm > tmptest/Tyler.htm
	curl http://www.superliminal.com/geometry/tylertest/Tyler.jar > tmptest/Tyler.jar
	curl http://www.superliminal.com/geometry/tylertest/Tyler.src.zip > tmptest/Tyler.src.zip
	curl http://www.superliminal.com/geometry/tylertest/TylerSave.php > tmptest/TylerSave.php
	(cd tmptest; ${JAVAROOT}/bin/jar -xvf Tyler.jar)
	(cd tmptest; ${JAVAROOT}/bin/jar -xvf Tyler.src.zip)
