import yaml
import os

class ConfigManager:
    @staticmethod
    def load(filepath="/home/pi/run/config.yaml"):
        if not os.path.exists(filepath):
            raise FileNotFoundError(f"Configuration file not found at {filepath}")
            
        with open(filepath, "r") as file:
            config = yaml.safe_load(file)
            
        # This will automatically create /home/pi/data and /home/pi/logs if missing
        db_dir = os.path.dirname(config['paths']['database'])
        log_dir = os.path.dirname(config['paths']['log_file'])
        
        if db_dir:
            os.makedirs(db_dir, exist_ok=True)
        if log_dir:
            os.makedirs(log_dir, exist_ok=True)
            
        return config
