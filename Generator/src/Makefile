# Makefile for Java Sales Dashboard (JavaFX + XChart)

# Variables
JAVAC = javac
JAVA = java
JFLAGS = -d bin
SRC_DIR = .
BIN_DIR = bin
MAIN_CLASS = SalesDashboard
JAR_LIB = lib/xchart-3.8.1.jar
# JavaFX libs installed by openjfx on Ubuntu WSL
JAVAFX_LIBS = /usr/share/openjfx/lib/javafx.base.jar:/usr/share/openjfx/lib/javafx.controls.jar:/usr/share/openjfx/lib/javafx.fxml.jar:/usr/share/openjfx/lib/javafx.graphics.jar:/usr/share/openjfx/lib/javafx.media.jar:/usr/share/openjfx/lib/javafx.swing.jar:/usr/share/openjfx/lib/javafx.web.jar

# Directories
SRC = $(wildcard $(SRC_DIR)/*.java)
CLASS_FILES = $(patsubst $(SRC_DIR)/%.java,$(BIN_DIR)/%.class,$(SRC))

# Default target: compile and run
all: compile run

# Compile Java files
compile: $(CLASS_FILES)

$(BIN_DIR)/%.class: $(SRC_DIR)/%.java
	@mkdir -p $(BIN_DIR)
	$(JAVAC) $(JFLAGS) -cp "$(JAVAFX_LIBS):$(JAR_LIB)" $<

# Run the application
run: $(CLASS_FILES)
	$(JAVA) --module-path /usr/share/openjfx/lib --add-modules=javafx.controls,javafx.fxml -cp "$(BIN_DIR):$(JAR_LIB):/usr/share/openjfx/lib/*" $(MAIN_CLASS)

# Clean up compiled files
clean:
	rm -rf $(BIN_DIR)/*.class

.PHONY: all compile run clean
