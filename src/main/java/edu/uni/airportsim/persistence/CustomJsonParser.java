package edu.uni.airportsim.persistence;

import edu.uni.airportsim.persistence.CustomJsonTokenizer.Token;
import edu.uni.airportsim.persistence.CustomJsonTokenizer.TokenType;

import java.math.BigDecimal;
import java.util.List;

public class CustomJsonParser {
    private final CustomJsonTokenizer tokenizer = new CustomJsonTokenizer();
    private List<Token> tokens = List.of();
    private int current;

    public JsonValue parse(String source) {
        tokens = tokenizer.tokenize(source);
        current = 0;
        JsonValue value = parseValue();
        consume(TokenType.EOF, "Expected end of JSON input");
        return value;
    }

    private JsonValue parseValue() {
        if (match(TokenType.LEFT_BRACE)) {
            return parseObject();
        }
        if (match(TokenType.LEFT_BRACKET)) {
            return parseArray();
        }
        if (match(TokenType.STRING)) {
            return JsonValue.of(previous().lexeme());
        }
        if (match(TokenType.NUMBER)) {
            return new JsonNumber(new BigDecimal(previous().lexeme()));
        }
        if (match(TokenType.TRUE)) {
            return JsonValue.of(true);
        }
        if (match(TokenType.FALSE)) {
            return JsonValue.of(false);
        }
        if (match(TokenType.NULL)) {
            return JsonValue.nullValue();
        }
        throw new IllegalArgumentException("Expected JSON value near token " + peek().type());
    }

    private JsonObject parseObject() {
        JsonObject object = new JsonObject();
        if (match(TokenType.RIGHT_BRACE)) {
            return object;
        }
        do {
            Token key = consume(TokenType.STRING, "Expected object key string");
            consume(TokenType.COLON, "Expected ':' after object key");
            object.put(key.lexeme(), parseValue());
        } while (match(TokenType.COMMA));
        consume(TokenType.RIGHT_BRACE, "Expected '}' after object");
        return object;
    }

    private JsonArray parseArray() {
        JsonArray array = new JsonArray();
        if (match(TokenType.RIGHT_BRACKET)) {
            return array;
        }
        do {
            array.add(parseValue());
        } while (match(TokenType.COMMA));
        consume(TokenType.RIGHT_BRACKET, "Expected ']' after array");
        return array;
    }

    private boolean match(TokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw new IllegalArgumentException(message + " near token " + peek().type());
    }

    private boolean check(TokenType type) {
        return peek().type() == type;
    }

    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
