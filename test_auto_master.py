import asyncio
import json
import websockets

clients = set()
master_id = None
state = {}

# ✅ For websockets 14.x → handler takes only ONE argument
async def handle_client(websocket):
    global master_id, state
    print(f"[+] New connection: {websocket.remote_address}")
    clients.add(websocket)

    try:
        async for msg in websocket:
            print(f"[RECV] {msg}")
            data = json.loads(msg)
            id_ = data.get("id")
            signals = data.get("signals", {})

            if master_id is None:
                master_id = id_
                print(f"[MASTER SET] {id_}")

            if id_ == master_id:
                state = signals.copy()
                print(f"[MASTER UPDATE] {state}")
                # broadcast update to all clients except master
                for client in clients:
                    if client != websocket:
                        await client.send(json.dumps(state))
            else:
                print(f"[CLIENT {id_}] Received {signals}")
                await websocket.send(json.dumps({master_id: state}))

    except Exception as e:
        print("[!] Error in handler:", e)
    finally:
        clients.remove(websocket)
        print(f"[-] Disconnected: {websocket.remote_address}")

async def main():
    print("[SERVER READY] Listening on ws://0.0.0.0:8765")
    # ✅ websockets.serve(handler, host, port) works the same
    async with websockets.serve(handle_client, "0.0.0.0", 8765):
        await asyncio.Future()  # keep alive

if __name__ == "__main__":
    asyncio.run(main())
