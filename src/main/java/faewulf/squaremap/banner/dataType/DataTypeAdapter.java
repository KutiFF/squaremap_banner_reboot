package faewulf.squaremap.banner.dataType;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import faewulf.squaremap.banner.Squaremapbanner;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.marker.Icon;
import xyz.jpenilla.squaremap.api.marker.Marker;

import java.io.IOException;
import java.util.UUID;

// Custom TypeAdapter for Data record
public class DataTypeAdapter extends TypeAdapter<Data> {

    @Override
    public void write(JsonWriter out, Data data) throws IOException {
        if (data == null) {
            out.nullValue();
            return;
        }

        out.beginObject();
        out.name("key").value(data.key().getKey());
        out.name("name").value(data.name());
        out.name("type").value(data.keyIconType().getKey());
        out.name("x").value(data.marker().point().x());
        out.name("z").value(data.marker().point().z());
        if (data.owner() != null)
            out.name("owner").value(data.owner().toString());
        if (data.ownerName() != null)
            out.name("owner_name").value(data.ownerName());
        out.endObject();
    }

    /**
     * Returns null (and logs a warning) for an incomplete entry instead of throwing, so one damaged banner
     * does not make the whole save file unreadable. Callers must skip null values.
     */
    @Override
    public Data read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        String keyString = null;
        String name = null;
        String keyIconType = null;
        String ownerString = null;
        String ownerName = null;
        int x = 0, z = 0;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
                case "key":
                    keyString = in.nextString();
                    break;
                case "name":
                    name = in.nextString();
                    break;
                case "type":
                    keyIconType = in.nextString();
                    break;
                case "x":
                    x = in.nextInt();
                    break;
                case "z":
                    z = in.nextInt();
                    break;
                case "owner":
                    ownerString = in.nextString();
                    break;
                case "owner_name":
                    ownerName = in.nextString();
                    break;
                default:
                    // unknown field (e.g. written by a newer version) - ignore it
                    in.skipValue();
            }
        }
        in.endObject();

        if (keyString == null || name == null || keyIconType == null) {
            Squaremapbanner.LOGGER.warn("Skipping a banner entry with missing fields in the save file (name: {})", name);
            return null;
        }

        UUID owner = null;
        if (ownerString != null) {
            try {
                owner = UUID.fromString(ownerString);
            } catch (IllegalArgumentException ignored) {
                // keep owner = null
            }
        }

        Key key = Key.of(keyString);
        Key keyIcon = Key.of(keyIconType);
        Icon icon = Marker.icon(Point.point(x, z), key, 16);
        return new Data(icon, keyIcon, name, key, owner, ownerName);
    }
}