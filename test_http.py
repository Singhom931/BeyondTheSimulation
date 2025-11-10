from flask import Flask, request, jsonify

app = Flask(__name__)

# store signals for all IDs
state = {}
master_input = None  # store the actual input from the master
mirrored_uuid = None  # the "master" UUID

@app.route('/', methods=['GET', 'POST'])
def index():
    global master_input, mirrored_uuid, state
    if request.method == 'POST':
        data = request.get_json(force=True)
        print("\n[POST RECEIVED]")
        print("Data:", data)

        id_ = data.get("id")
        signals = data.get("signals", {})

        if id_ == mirrored_uuid:
            # store master input
            master_input = signals.copy()
            # propagate to all other UUIDs
            for other_id in state.keys():
                if other_id != mirrored_uuid:
                    state[other_id] = master_input.copy()
            # master always outputs 0
            out = {"north": 0, "east": 0, "south": 0, "west": 0}
            print(f"[INFO] Master UUID posted; mirrored to other IDs: {list(state.keys())}")
        else:
            # if master input exists, override this UUID's signals
            if master_input:
                out = master_input.copy()
            else:
                out = signals.copy()

        # store/update this UUID's own data
        state[id_] = out

        return jsonify({"id": id_, "signals": state[id_]})

    elif request.method == 'GET':
        print("\n[GET RECEIVED]")
        uuid = request.args.get("uuid")
        if not uuid:
            print(state)
            return jsonify(state)
        elif uuid not in state.keys():
            if not state: mirrored_uuid = uuid; print("Adding Master")
            state[uuid] = None
        return jsonify(state)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
