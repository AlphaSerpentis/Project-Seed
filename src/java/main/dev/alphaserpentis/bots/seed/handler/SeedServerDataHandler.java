package dev.alphaserpentis.bots.seed.handler;

import com.google.gson.reflect.TypeToken;
import dev.alphaserpentis.bots.seed.data.server.SeedServerData;
import dev.alphaserpentis.bots.seed.serialization.SeedServerDataDeserializer;
import dev.alphaserpentis.coffeecore.handler.api.discord.servers.ServerDataHandler;
import io.reactivex.rxjava3.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Path;

public class SeedServerDataHandler extends ServerDataHandler<SeedServerData> {

    public SeedServerDataHandler(
            @NonNull Path path
    ) throws IOException {
        super(path, new TypeToken<>() {}, new SeedServerDataDeserializer());
    }

    @Override
    protected SeedServerData createNewServerData() {
        return new SeedServerData();
    }
}
