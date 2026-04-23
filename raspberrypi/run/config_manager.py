import yaml
import os
import logging

logger = logging.getLogger(__name__)

class ConfigManager:
    DEFAULT_PATH = os.environ.get("CONFIG_PATH", "config.yaml")

    @staticmethod
    def load(filepath=None):
        if filepath is None:
            filepath = ConfigManager.DEFAULT_PATH

        config = {
            'system': {},
            'paths': {},
            'ble': {},
            'webapp': {}
        }

        if os.path.exists(filepath):
            try:
                with open(filepath, "r") as file:
                    loaded_config = yaml.safe_load(file)
                    if loaded_config:
                        # Deep merge or update top-level keys
                        for key in config:
                            if key in loaded_config:
                                config[key].update(loaded_config[key])
            except Exception as e:
                logger.error(f"Error loading config file {filepath}: {e}")

        # Environment variable overrides (Bootstrapping)
        # These take precedence for initial setup
        if os.environ.get("ROOM_ID"):
            config['system']['room_id'] = os.environ.get("ROOM_ID")
        if os.environ.get("API_URL"):
            config['webapp']['api_url'] = os.environ.get("API_URL")
        
        # Default paths if not set
        config['paths']['database'] = config['paths'].get('database', "data/production.sqlite")
        config['paths']['log_file'] = config['paths'].get('log_file', "logs/raspberrypi.log")

        # Create directories
        db_dir = os.path.dirname(config['paths']['database'])
        log_dir = os.path.dirname(config['paths']['log_file'])
        
        if db_dir:
            os.makedirs(db_dir, exist_ok=True)
        if log_dir:
            os.makedirs(log_dir, exist_ok=True)
            
        return config

    @staticmethod
    def save(config, filepath=None):
        if filepath is None:
            filepath = ConfigManager.DEFAULT_PATH
        
        try:
            with open(filepath, "w") as file:
                yaml.safe_dump(config, file, default_flow_style=False)
            logger.info(f"Configuration successfully saved to {filepath}")
        except Exception as e:
            logger.error(f"Failed to save configuration to {filepath}: {e}")
            raise
