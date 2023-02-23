package symbolTableEl;

import Components.Token;

public class Symbol {
    private String type; // 类型
    private int dimensions; // 0?1?2
    private String value; // 类似Token
    private int scopeId; // 处于哪个scope
    private Token token;

    public Symbol(String type, int dimensions, int scopeId, Token token) {
        this.type = type;
        this.dimensions = dimensions;
        this.value = token.getVal();
        this.scopeId = scopeId;
        this.token = token;
    }

    public String getType() {
        return type;
    }

    public int getDimensions() {
        return dimensions;
    }

    public String getValue() {
        return value;
    }

    public int getScopeId() {
        return scopeId;
    }

    public Token getToken() {
        return token;
    }

    @Override
    public String toString() {
        return value;
    }
}
