package Components;

import java.util.HashMap;

public class TokenMap {
    private static HashMap<String, TokenType> enumMap = new HashMap<>();
    private static boolean isInit = false;

    public static HashMap<String, TokenType> getEnumMap() {
        if (!isInit) {
            initialize();
        }
        return enumMap;
    }

    public static TokenType getTokenType(String val) {
        if (!isInit) {
            initialize();
        }
        if (enumMap.containsKey(val)) {
            return enumMap.get(val);
        } else {
            return TokenType.IDENFR;
        }
    }

    private TokenMap() {}

    private static void initialize() {
        enumMap.put("main", TokenType.MAINTK);
        enumMap.put("int", TokenType.INTTK);
        enumMap.put("const", TokenType.CONSTTK);
        enumMap.put("break", TokenType.BREAKTK);
        enumMap.put("continue", TokenType.CONTINUETK);
        enumMap.put("if", TokenType.IFTK);
        enumMap.put("else", TokenType.ELSETK);
        enumMap.put("while", TokenType.WHILETK);
        enumMap.put("getint", TokenType.GETINTTK);
        enumMap.put("printf", TokenType.PRINTFTK);
        enumMap.put("return", TokenType.RETURNTK);
        enumMap.put("void", TokenType.VOIDTK);
        enumMap.put("bitand", TokenType.BITAND);


        isInit = true;
    }


}
