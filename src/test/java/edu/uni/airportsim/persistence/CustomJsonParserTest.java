package edu.uni.airportsim.persistence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomJsonParserTest {
    @Test
    void parsesObjectArrayAndPrimitiveValues() {
        CustomJsonParser parser = new CustomJsonParser();

        JsonObject object = parser.parse("{\"name\":\"Lisbon\",\"count\":2,\"active\":true,\"items\":[1,null]}").asObject();

        assertEquals("Lisbon", object.getString("name", ""));
        assertEquals(2, object.getInt("count", 0));
        assertTrue(object.get("active").asBoolean());
        assertEquals(2, object.getArray("items").size());
    }

    @Test
    void writesAndParsesRoundTrip() {
        JsonObject object = new JsonObject()
                .put("airport", JsonValue.of("LIS"))
                .put("flights", new JsonArray().add(JsonValue.of("TP100")));

        String json = new CustomJsonWriter().write(object);
        JsonObject parsed = new CustomJsonParser().parse(json).asObject();

        assertEquals("LIS", parsed.getString("airport", ""));
        assertEquals("TP100", parsed.getArray("flights").get(0).asString());
    }
}
