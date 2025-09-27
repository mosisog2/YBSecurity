# Makefile for AutoVisualizer with auto-download of XChart

JAVAC = javac
JAVA = java

SRC = AutoVisualizer.java
BIN = bin
LIB = lib
JAR_NAME = xchart-3.8.1.jar
JAR_URL = https://repo1.maven.org/maven2/org/knowm/xchart/xchart/3.8.1/xchart-3.8.1.jar
JAR_PATH = $(LIB)/$(JAR_NAME)

# Classpath: use ";" on Windows instead of ":"
CP = .:$(JAR_PATH):$(BIN)

# Default target
all: compile

# Download the jar if it's not already there
$(JAR_PATH):
	@echo "Downloading XChart..."
	@mkdir -p $(LIB)
	@curl -L -o $(JAR_PATH) $(JAR_URL)

# Compile Java code
compile: $(JAR_PATH)
	@mkdir -p $(BIN)
	$(JAVAC) -cp $(CP) -d $(BIN) $(SRC)
	@echo "Compilation complete."

# Run the program
run: compile
	$(JAVA) -cp $(CP) AutoVisualizer

# Remove compiled class files
clean:
	rm -rf $(BIN)/*.class
	@echo "Cleaned compiled classes."

# Remove everything (including downloaded JAR)
distclean: clean
	rm -f $(JAR_PATH)
	@echo "Removed downloaded libraries."

