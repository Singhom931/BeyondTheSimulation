import asyncio, websockets, json

async def main():
    uri = "ws://127.0.0.1:8765"
    async with websockets.connect(uri) as ws:
        print("[CLIENT] Connected!")

        # send data (simulate redstone signal)
        await ws.send(json.dumps({
            "id": "client1",
            "signals": {"north": 15, "south": 0, "east": 0, "west": 0}
        }))
        print("[CLIENT] Sent signal packet")

        # wait for response
        reply = await ws.recv()
        print("[CLIENT] Received:", reply)

asyncio.run(main())
