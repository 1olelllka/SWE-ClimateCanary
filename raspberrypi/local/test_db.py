# /// script
# requires-python = ">=3.9"
# dependencies = [
#     "aiosqlite>=0.20.0",
# ]
# ///
import asyncio
import os
from db_manager import DatabaseManager

async def test_database_flow():
    # Use a specific test database so we don't clutter the real one
    test_db_path = "/home/pi/test_data/test_data.sqlite"
    
    # Remove old test DB if it exists for a fresh start
    if os.path.exists(test_db_path):
        os.remove(test_db_path)

    print("=== 1. Initializing Database ===")
    db = DatabaseManager(test_db_path)
    await db.init_db()
    print("Database initialized successfully.\n")

    print("=== 2. Testing Limits (Webapp Simulation) ===")
    await db.set_limit("max_temp", 28.5)
    await db.set_limit("max_co2", 1000.0)
    limits = await db.get_all_limits()
    print(f"Current Limits stored: {limits}\n")

    print("=== 3. Testing Normal Measurements (Arduino Simulation) ===")
    await db.insert_measurement(temp=22.5, moisture=45.0, co2=450.0)
    await db.insert_measurement(temp=23.0, moisture=44.5, co2=460.0)
    print("Inserted 2 normal measurements.\n")

    print("=== 4. Testing Limit Violations ===")
    # Simulate high CO2
    current_co2 = 1200.0
    if current_co2 > limits.get("max_co2", 9999):
        print(f"-> Detected CO2 violation: {current_co2} > {limits['max_co2']}")
        await db.register_violation("co2", limits["max_co2"], current_co2)
    
    # Try to register the exact same violation again (should be ignored by DB logic)
    await db.register_violation("co2", limits["max_co2"], 1250.0)
    
    active_violations = await db.get_active_violations()
    print(f"Active Violations: {active_violations}\n")

    print("=== 5. Resolving Violations ===")
    # Simulate CO2 dropping back to normal
    normal_co2 = 800.0
    if normal_co2 < limits.get("max_co2", 9999):
         print("-> CO2 returned to normal. Resolving...")
         await db.resolve_violation("co2")
         
    active_violations_after = await db.get_active_violations()
    print(f"Active Violations after resolve: {active_violations_after}\n")

    print("=== 6. Testing Error Logging (Ausfallsicherheit) ===")
    # Simulate a network drop
    await db.log_event("NETWORK", "Connection to Webapp lost. Server returned 502.", "ERROR")
    await db.log_event("BLE", "SensorStation connection unstable.", "WARN")
    await db.log_event("SYSTEM", "Daily cleanup routine finished.", "INFO")
    print("Logged 3 events (Check /home/pi/ble-raspberry/gateway.log for output).\n")
    
    print("=== TEST COMPLETE ===")

if __name__ == "__main__":
    # Ensure the directory exists
    os.makedirs("/home/pi/test_data", exist_ok=True)
    asyncio.run(test_database_flow())
