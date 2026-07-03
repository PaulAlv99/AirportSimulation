package edu.uni.airportsim.persistence;

import java.util.ArrayList;
import java.util.List;

public class CustomJsonTokenizer {
    public enum TokenType {
        LEFT_BRACE,
        RIGHT_BRACE,
        LEFT_BRACKET,
        RIGHT_BRACKET,
        COLON,
        COMMA,
        STRING,
        NUMBER,
        TRUE,
        FALSE,
        NULL,
        EOF
    }

    public record Token(TokenType type, String lexeme) {
    }

    public List<Token> tokenize(String source) {
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        while (index < source.length()) {
            char character = source.charAt(index);
            switch (character) {
                case '{' -> {
                    tokens.add(new Token(TokenType.LEFT_BRACE, "{"));
                    index++;
                }
                case '}' -> {
                    tokens.add(new Token(TokenType.RIGHT_BRACE, "}"));
                    index++;
                }
                case '[' -> {
                    tokens.add(new Token(TokenType.LEFT_BRACKET, "["));
                    index++;
                }
                case ']' -> {
                    tokens.add(new Token(TokenType.RIGHT_BRACKET, "]"));
                    index++;
                }
                case ':' -> {
                    tokens.add(new Token(TokenType.COLON, ":"));
                    index++;
                }
                case ',' -> {
                    tokens.add(new Token(TokenType.COMMA, ","));
                    index++;
                }
                case '"', '\'' -> index = readString(source, index, tokens);
                case ' ', '\r', '\n', '\t' -> index++;
                default -> {
                    if (character == '-' || Character.isDigit(character)) {
                        index = readNumber(source, index, tokens);
                    } else if (source.startsWith("true", index)) {
                        tokens.add(new Token(TokenType.TRUE, "true"));
                        index += 4;
                    } else if (source.startsWith("false", index)) {
                        tokens.add(new Token(TokenType.FALSE, "false"));
                        index += 5;
                    } else if (source.startsWith("null", index)) {
                        tokens.add(new Token(TokenType.NULL, "null"));
                        index += 4;
                    } else {
                        throw new IllegalArgumentException("Unexpected JSON character: " + character);
                    }
                }
            }
        }
        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    private int readString(String source, int start, List<Token> tokens) {
        char quote = source.charAt(start);
        StringBuilder value = new StringBuilder();
        int index = start + 1;
        while (index < source.length()) {
            char character = source.charAt(index);
            if (character == quote) {
                tokens.add(new Token(TokenType.STRING, value.toString()));
                return index + 1;
            }
            if (character == '\\') {
                index = readEscape(source, index + 1, value);
            } else {
                value.append(character);
                index++;
            }
        }
        throw new IllegalArgumentException("Unterminated JSON string");
    }

    private int readEscape(String source, int index, StringBuilder value) {
        if (index >= source.length()) {
            throw new IllegalArgumentException("Invalid JSON string escape");
        }
        char escaped = source.charAt(index);
        switch (escaped) {
            case '"', '\'', '\\', '/' -> value.append(escaped);
            case 'b' -> value.append('\b');
            case 'f' -> value.append('\f');
            case 'n' -> value.append('\n');
            case 'r' -> value.append('\r');
            case 't' -> value.append('\t');
            case 'u' -> {
                String hex = source.substring(index + 1, index + 5);
                value.append((char) Integer.parseInt(hex, 16));
                return index + 5;
            }
            default -> throw new IllegalArgumentException("Unsupported JSON escape: \\" + escaped);
        }
        return index + 1;
    }

    private int readNumber(String source, int start, List<Token> tokens) {
        int index = start;
        if (source.charAt(index) == '-') {
            index++;
        }
        while (index < source.length() && Character.isDigit(source.charAt(index))) {
            index++;
        }
        if (index < source.length() && source.charAt(index) == '.') {
            index++;
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
        }
        if (index < source.length() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
            index++;
            if (index < source.length() && (source.charAt(index) == '+' || source.charAt(index) == '-')) {
                index++;
            }
            while (index < source.length() && Character.isDigit(source.charAt(index))) {
                index++;
            }
        }
        tokens.add(new Token(TokenType.NUMBER, source.substring(start, index)));
        return index;
    }
}
