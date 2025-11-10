import asyncio
import json
import websockets

clients = {}  # {uuid: websocket}
master_uuid = None
state = {}  # {uuid: {"north": int, ...}}
master_signals = None

async def handle_client(websocket):
    global master_uuid, state, master_signals
    try:
        async for msg in websocket:
            data = json.loads(msg)
            uuid = data.get("id")
            signals = data.get("signals", {})

            if uuid not in clients:
                clients[uuid] = websocket
                print(f"[+] New client connected: {uuid}")

            # first UUID that connects becomes master if none exists
            if master_uuid is None:
                master_uuid = uuid
                print(f"[MASTER SET] {master_uuid}")

            # Master logic: update global master_signals and mirror to others
            if uuid == master_uuid:
                master_signals = signals.copy()
                print(f"[MASTER UPDATE] {master_signals}")
                # broadcast to all other clients (wrap in their UUID for compatibility)
                for other_uuid, ws in clients.items():
                    if other_uuid != master_uuid:
                        try:
                            await ws.send(json.dumps({other_uuid: master_signals}))
                        except Exception as e:
                            print(f"[!] Error sending to {other_uuid}: {e}")

            else:
                # Non-master: override their output with master signals if available
                if master_signals:
                    out = master_signals.copy()
                else:
                    out = signals.copy()

                state[uuid] = out
                # send back the current state for this UUID
                await websocket.send(json.dumps({uuid: out}))

            # keep a full state for debugging
            print(f"[STATE] {state}")

    except Exception as e:
        print(f"[!] Connection error: {e}")
    finally:
        # cleanup
        for uid, ws in list(clients.items()):
            if ws == websocket:
                del clients[uid]
                print(f"[-] Disconnected: {uid}")
        if master_uuid == uuid:
            master_uuid = None
            master_signals = None
            print("[MASTER RESET]")

async def main():
    print("[SERVER READY] Listening on ws://127.0.0.1:8765")
    async with websockets.serve(handle_client, "127.0.0.1", 8765):
        await asyncio.Future()  # run forever

if __name__ == "__main__":
    asyncio.run(main())
