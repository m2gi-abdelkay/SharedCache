# Makefile for JAVANAISE Project
# Compiles the Java RMI-based distributed shared memory system

# Directories
SRC_DIR = src
BIN_DIR = bin
JAVA_FILES = $(shell find $(SRC_DIR) -name "*.java")
CLASS_FILES = $(JAVA_FILES:$(SRC_DIR)/%.java=$(BIN_DIR)/%.class)

# Java compilation settings
JAVAC = javac
JAVA = java
JAVAC_FLAGS = -d $(BIN_DIR) -cp $(SRC_DIR) -Xlint:unchecked
RMIREGISTRY_PORT = 1099

# Default target
all: compile

# Create bin directory if it doesn't exist
$(BIN_DIR):
	mkdir -p $(BIN_DIR)

# Compile all Java files
compile: $(BIN_DIR) $(CLASS_FILES)
	@echo "Compilation completed successfully!"

# Pattern rule for compiling Java files
$(BIN_DIR)/%.class: $(SRC_DIR)/%.java
	@mkdir -p $(dir $@)
	$(JAVAC) $(JAVAC_FLAGS) $<

# Clean compiled files
clean:
	rm -rf $(BIN_DIR)
	@echo "Cleaned compiled files"

# Start RMI Registry
start-registry:
	@echo "Starting RMI Registry on port $(RMIREGISTRY_PORT)..."
	@cd bin && rmiregistry $(RMIREGISTRY_PORT) &
	@echo "RMI Registry started"

# Stop RMI Registry
stop-registry:
	@echo "Stopping RMI Registry..."
	@pkill -f rmiregistry || true
	@echo "RMI Registry stopped"

# Run the Coordinator
run-coordinator: compile
	@echo "Starting JVN Coordinator..."
	cd $(BIN_DIR) && $(JAVA) jvn.Coordinator.Coordinator

# Run the Server
run-server: compile
	@echo "Starting JVN Server..."
	cd $(BIN_DIR) && $(JAVA) jvn.Server.Server

# Run the Client
run-client: compile
	@echo "Starting JVN Client..."
	cd $(BIN_DIR) && $(JAVA) jvn.Client.Client

# Run the IRC application
run-irc: compile
	@echo "Starting IRC Chat Application..."
	cd $(BIN_DIR) && $(JAVA) irc.Irc

# Generate RMI stubs (if needed for older Java versions)
generate-stubs: compile
	@echo "Generating RMI stubs..."
	cd $(BIN_DIR) && rmic jvn.Coordinator.JvnCoordImpl
	cd $(BIN_DIR) && rmic jvn.Server.JvnServerImpl
	@echo "RMI stubs generated"

# Full setup: clean, compile, and start registry
setup: clean compile start-registry
	@echo "Full setup completed!"

# Test compilation only
test-compile: compile
	@echo "Test compilation successful - all files compiled without errors"

# Display help
help:
	@echo "Available targets:"
	@echo "  all              - Compile all Java files (default)"
	@echo "  compile          - Compile all Java files"
	@echo "  clean            - Remove compiled files"
	@echo "  start-registry   - Start RMI Registry"
	@echo "  stop-registry    - Stop RMI Registry"
	@echo "  run-coordinator  - Run the JVN Coordinator"
	@echo "  run-server       - Run the JVN Server"
	@echo "  run-client       - Run the JVN Client"
	@echo "  run-irc          - Run the IRC Chat Application"
	@echo "  generate-stubs   - Generate RMI stubs (for older Java versions)"
	@echo "  setup            - Full setup (clean + compile + start registry)"
	@echo "  test-compile     - Test compilation only"
	@echo "  help             - Display this help message"

# Rebuild everything
rebuild: clean compile

# Check if Java files have been modified
check-modified:
	@echo "Checking for modified Java files..."
	@find $(SRC_DIR) -name "*.java" -newer $(BIN_DIR) 2>/dev/null || echo "No modifications detected"

.PHONY: all compile clean start-registry stop-registry run-coordinator run-server run-client run-irc generate-stubs setup help rebuild test-compile check-modified