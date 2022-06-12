.PHONY: run

all: compile

run: compile
	java -cp '.:postgresql-42.4.0.jar' Main

compile: clear
	javac -cp '.:postgresql-42.4.0.jar' Main.java

clear:
	rm -rf *.class
