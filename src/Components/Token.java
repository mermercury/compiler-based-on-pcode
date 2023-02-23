package Components;

public class Token implements Item{
    private TokenType type;
    private String val;
    private int LineNo;

    public int getLineNo() {
        return LineNo;
    }

    public Token(TokenType type, String val, int lineNo) {
        this.type = type;
        this.val = val;
        LineNo = lineNo;
    }

    public TokenType getType() {
        return type;
    }

    public String getVal() {
        return val;
    }

    @Override
    public String toString() {
        return type + " " + val;
    }

    public int getFormatNum() {
        int n = 0;
        for (int i = 0; i < val.length(); i++) {
            if (i + 1 < val.length()) {
                if (val.charAt(i) == '%' && val.charAt(i + 1) == 'd') {
                    n++;
                }
            }
        }
        return n;
    }
}
