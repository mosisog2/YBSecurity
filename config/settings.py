# LIDA Multi-Dataset Analytics Platform Configuration

# Server Configuration
DEFAULT_PORT = 8080
MULTI_DATASET_PORT = 8084
RETAIL_ANALYTICS_PORT = 8082
ORIGINAL_LIDA_PORT = 8081

# Dataset Configuration
DATASETS_DIR = "datasets"
MAX_DATASET_SIZE = 1000000  # Maximum rows per dataset
SAMPLE_SIZE = 10000  # Default sample size for large datasets

# LIDA Configuration
LIDA_MODEL = "gpt-3.5-turbo"
LIDA_TEMPERATURE = 0.2
LIDA_MAX_TOKENS = 2000
LIDA_USE_CACHE = True

# Visualization Configuration
DEFAULT_CHART_WIDTH = 800
DEFAULT_CHART_HEIGHT = 400
MAX_CHART_CATEGORIES = 20
COLOR_PALETTE = ["#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"]

# Security Configuration
CORS_ORIGINS = ["http://localhost:*", "http://127.0.0.1:*"]
DEBUG_MODE = True

# Logging Configuration
LOG_LEVEL = "INFO"
LOG_FORMAT = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"