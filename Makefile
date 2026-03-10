SOURCES = Article.java Utils.java SharedData.java WorkerThread.java Tema1.java
CLASSPATH = .:libs/*

build:
	javac -cp "$(CLASSPATH)" $(SOURCES)

run:
	java -cp "$(CLASSPATH)" Tema1 $(ARGS)

clean:
	rm -f *.class *.txt


