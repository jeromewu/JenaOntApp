name=swrlapp
main=com.delta.SWRLApp
jarfile=./target/${name}-1.0-SNAPSHOT-jar-with-dependencies.jar
ontfile=./data/ont/demoData.rdf
rulefile=./data/rule/demo.rule
log4jproperties=./src/main/resources/log4j.properties

all: compile

compile:
	mvn clean compile assembly:single

run:
	java -cp ${jarfile} -Dlog4j.configuration=file:${log4jproperties} ${main} ${ontfile} ${rulefile}
