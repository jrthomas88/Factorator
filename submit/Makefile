SERVER=./FactorServer.java
SUBSERV=./FactorSubServer.java
CLIENT=./FactorClient.java
TYPE=./FactorType.java
DATA=./FactorData.java
MATH=./FactorMath.java
SEQ=./FactorSequential.java

all: server client

server: mkdir ${SERVER} ${TYPE} ${DATA} ${SUBSERV} ${MATH}
	javac ${SERVER} ${TYPE} ${DATA} ${SUBSERV} ${MATH}
	cp ./*.class ./model/

client: mkdir ${CLIENT} ${SERVER} ${SUBSERV} ${TYPE} ${DATA} ${MATH}
	javac ${CLIENT} ${SERVER} ${SUBSERV} ${TYPE} ${DATA} ${MATH}
	cp ./*.class ./model

sequential: mkdir  ${SEQ} ${TYPE} ${SERVER} ${SUBSERV} ${DATA} ${MATH}
	javac ${SEQ} ${TYPE} ${SERVER} ${SUBSERV} ${DATA} ${MATH}
	cp ./*.class ./model

clean:
	rm -f -r model
	rm -f *.class

mkdir:
	rm -f -r model
	mkdir ./model
