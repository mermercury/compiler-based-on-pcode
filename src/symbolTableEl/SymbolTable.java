package symbolTableEl;

import Components.Token;

import java.util.HashMap;

public class SymbolTable {
    private HashMap<String, Symbol> symbolHashMap;

    public SymbolTable() {
        symbolHashMap = new HashMap<>();
    }

    public void addSymbol(String type, int dimensions, Token token, int scopeID) {
        symbolHashMap.put(token.getVal(), new Symbol(type, dimensions, scopeID, token));
    }

    public boolean hasSymbol(Token token) {
        return symbolHashMap.containsKey(token.getVal());
    }

    public boolean hasSymbol(String name) {
        return symbolHashMap.containsKey(name);
    }

    public Symbol getSymbol(Token token) {
        return symbolHashMap.get(token.getVal());
    }

    @Override
    public String toString() {
        return symbolHashMap.toString();
    }

    public boolean isConst(Token word) {
        return symbolHashMap.get(word.getVal()).getType().equals("const");
    }
}
