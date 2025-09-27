# Makefile for Java Sales Dashboard

# Variables
JAVAC = javac
JAVA = java
JFLAGS = -d bin
SRC_DIR = src
BIN_DIR = bin
MAIN_CLASS = SalesDashboard.java
JAR_LIB = lib/xchart-3.8.1.jar # Add your JAR dependencies here

# Directories
SRC = $(wildcard $(SRC_DIR)/*.java)
CLASS_FILES = $(SRC:$(SRC_DIR)/%.java=$(BIN_DIR)/%.class)

# Default target: compile and run
all: compile run

# Compile Java files
compile: $(CLASS_FILES)

$(BIN_DIR)/%.class: $(SRC_DIR)/%.java
	$(JAVAC) $(JFLAGS) $<

# Run the application
run: $(CLASS_FILES)
	$(JAVA) -cp "$(BIN_DIR):$(JAR_LIB)" $(MAIN_CLASS)

# Clean up compiled files
clean:
	rm -rf $(BIN_DIR)/*.class

.PHONY: all compile run clean
