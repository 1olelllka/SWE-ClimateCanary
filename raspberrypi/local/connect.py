import asyncio
import os
import aiohttp
from aiohttp import web
from bleak import BleakScanner, BleakClient

TARGET_NAME = os.getenv("BLE_NAME", "SensorStation")
CHAR_UUID = os.getenv("BLE_UUID", "19B10001-E8F2-537E-4F6C-D104768A1214")
WRITE_UUID = os.getenv("BLE_WRITE_UUID", "19B10002-E8F2-537E-4F6C-D104768A1214")

API_URL = os.getenv("API_URL", "http://172.20.10.4:8080/test-info")

LOCAL_PORT = int(os.getenv("LOCAL_PORT", 8080))

command_queue = asyncio.Queue()

async def send_to_webapp(data_string):
    headers = {"Content-Type": "application/json"}
    payload = {"device": TARGET_NAME, "message": data_string}
    try:
        timeout = aiohttp.ClientTimeout(total=10)
        async with aiohttp.ClientSession(timeout=timeout) as session:
            async with session.post(API_URL, json=payload, headers=headers) as response:
                if response.status not in (200, 201):
                    print(f"Failed to push to Webapp (Status: {response.status})")

                body = await response.text()

                print(f"Status: {response.status}")

    except Exception as e:
        print(f"Network error reaching REST server: {e}")

def notification_handler(sender, data):
    message = data.decode('utf-8').strip()
    print(f"\n[Arduino -> Pi] Received: {message}")
    asyncio.create_task(send_to_webapp(message))

async def handle_webapp_post(request):
    try:
        data = await request.json()
        command = data.get("message")

        if command:
            print(f"\n[Webapp -> Pi] Received command: {command}")
            await command_queue.put(command)
            return web.json_response({"status": "success", "message": "Command queued for Arduino"})
        else:
            return web.json_response({"status": "error", "message": "Missing 'command' key"}, status=400)
    except Exception as e:
        return web.json_response({"status": "error", "message": str(e)}, status=400)

async def start_local_server():
    app = web.Application()
    app.router.add_post('/api/send-command', handle_webapp_post)
    app.router.add_post('/test-info', handle_webapp_post)
    app.router.add_post('/send-to-raspberry', handle_webapp_post)
    runner = web.AppRunner(app)
    await runner.setup()
    site = web.TCPSite(runner, '0.0.0.0', LOCAL_PORT)
    await site.start()
    print(f"Pi Listening for Webapp commands on port {LOCAL_PORT}...")

async def main():
    await start_local_server()

    print("Perfoming webapp connection test.\n")
    await send_to_webapp("TEST: Initial connection test.")

    print(f"Scanning for Arduino named: '{TARGET_NAME}'...")
    device = await BleakScanner.find_device_by_filter(
        lambda d, _: d.name and TARGET_NAME in d.name,
        timeout=15.0
    )

    if not device:
        print(f"Error: Could not find '{TARGET_NAME}'.")
        await asyncio.Event().wait()
        return

    print("Connecting to Arduino...")
    try:
        async with BleakClient(device, timeout=20.0) as client:
            print("Connected!")

            await client.start_notify(CHAR_UUID, notification_handler)
            while True:
                new_command = await command_queue.get()

                print(f"[Pi -> Arduino] Sending: {new_command}")
                command_bytes = new_command.encode('utf-8')
                await client.write_gatt_char(WRITE_UUID, command_bytes, response=False)

    except Exception as e:
        print(f"BLE Connection dropped: {e}")

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nGateway stopped.")
