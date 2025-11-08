package com.diablo931.network;

import com.diablo931.beyondthesimulation;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;

public class ServerStringReceiver implements ServerPlayNetworking.PlayPayloadHandler<C2SStringPayload> {
    @Override
    public void receive(C2SStringPayload payload, ServerPlayNetworking.Context context) {
        String receivedString = payload.myString();
        context.server().execute(() -> {
            // Log the received string to the server console
            beyondthesimulation.LOGGER.info("Received from client: {}", receivedString);

            // Example action: Broadcast the message to all players
            context.server().getPlayerManager().broadcast(
                    Text.literal("Server received: " + receivedString),
                    false
            );
        });
    }
}
