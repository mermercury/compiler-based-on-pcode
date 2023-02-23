import Components.Token;
import Components.TokenMap;
import Components.TokenType;

import java.util.ArrayList;

public class Lexer {
    private ArrayList<String> strings;
    private int lineNo; // 行号
    private int idx; // 一行中解析位置

    public Lexer(ArrayList<String> strings) {
        this.strings = strings;
        this.lineNo = 1;
        this.idx = 0;
    }

    public ArrayList<Token> lexing() throws Exception {
        ArrayList<Token> tokens = new ArrayList<>();
        for (int i = 0; i < strings.size(); i++) {
            tokens.addAll(lexLine(strings.get(i)));
            lineNo++;
        }
        return tokens;
    }

    private boolean comment = false;
    private ArrayList<Token> lexLine(String line) throws Exception {
        ArrayList<Token> list = new ArrayList<>();
        idx = 0;
        String leastIdent = null;
        while (idx < line.length()) {
            char c = line.charAt(idx++);/*
            *
            */
            if (comment) { // 处于多行注释状态，直接忽略中间的字符
                if (c != '*' || (idx >= line.length())) {
                    continue;
                } else if (line.charAt(idx) == '/') {
                    idx++;
                    comment = false;
                    continue;
                }
            } else {
                // 字母开头，可能是标识符或者保留字
                if (Character.isLetter(c) || c == '_') {
                    StringBuilder sb = new StringBuilder();
                    while (idx < line.length() && (Character.isLetterOrDigit(c) || c == '_')) {
                        sb.append(c);
                        c = line.charAt(idx++);
                    }
                    if (Character.isLetterOrDigit(c) || c == '_') {
                        sb.append(c);
                    } else {
                        idx--;
                    }
                    list.add(lexToken(sb.toString()));
                    leastIdent = sb.toString();
                } else if (Character.isDigit(c)) {
                    StringBuilder sb = new StringBuilder();
                    while (idx < line.length() && Character.isDigit(c)) {
                        sb.append(c);
                        c = line.charAt(idx++);
                    }
                    if (Character.isDigit(c)) {
                        sb.append(c);
                    } else {
                        idx--;
                    }
                    list.add(new Token(TokenType.INTCON, sb.toString(), lineNo));
                } else if (c == '!') {
                    c = line.charAt(idx++);
                    if (c == '=') {
                        list.add(new Token(TokenType.NEQ, "!=", lineNo));
                    } else {
                        list.add(new Token(TokenType.NOT, "!", lineNo));
                        idx--;
                    }
                } else if (c == '&') {
                    idx++;
                    list.add(new Token(TokenType.AND, "&&", lineNo));
                } else if (c == '|') {
                    idx++;
                    list.add(new Token(TokenType.OR, "||", lineNo));
                } else if (c == '+') {
                    c = line.charAt(idx++);
                    if (c == '+') { // i++ -> i=i+1
                        list.add(new Token(TokenType.ASSIGN, "=", lineNo));
                        list.add(lexToken(leastIdent));
                        list.add(new Token(TokenType.PLUS, "+", lineNo));
                        list.add(new Token(TokenType.INTCON, "1", lineNo));
                    } else {
                        list.add(new Token(TokenType.PLUS, "+", lineNo));
                        idx--;
                    }
                } else if (c == '-') {
                    list.add(new Token(TokenType.MINU, "-", lineNo));
                } else if (c == '*') {
                    list.add(new Token(TokenType.MULT, "*", lineNo));
                } else if (c == '/') {
                    c = line.charAt(idx++);
                    if (c == '/') {
                        break; //单行注释,本行可忽略
                    } else if (c == '*') {
                        comment = true; //自由注释
                    } else {
                        list.add(new Token(TokenType.DIV, "/", lineNo));
                        idx--;
                    }
                } else if (c == '%') {
                    list.add(new Token(TokenType.MOD, "%", lineNo));
                } else if (c == '<') {
                    c = line.charAt(idx++);
                    if (c == '=') {
                        list.add(new Token(TokenType.LEQ, "<=", lineNo));
                    } else {
                        list.add(new Token(TokenType.LSS, "<", lineNo));
                        idx--;
                    }
                } else if (c == '>') {
                    c = line.charAt(idx++);
                    if (c == '=') {
                        list.add(new Token(TokenType.GEQ, ">=", lineNo));
                    } else {
                        list.add(new Token(TokenType.GRE, ">", lineNo));
                        idx--;
                    }
                } else if (c == '=') {
                    c = line.charAt(idx++);
                    if (c == '=') {
                        list.add(new Token(TokenType.EQL, "==", lineNo));
                    } else {
                        list.add(new Token(TokenType.ASSIGN, "=", lineNo));
                        idx--;
                    }
                } else if (c == ';') {
                    list.add(new Token(TokenType.SEMICN, ";", lineNo));
                } else if (c == ',') {
                    list.add(new Token(TokenType.COMMA, ",", lineNo));
                } else if (c == '(') {
                    list.add(new Token(TokenType.LPARENT, "(", lineNo));
                } else if (c == ')') {
                    list.add(new Token(TokenType.RPARENT, ")", lineNo));
                } else if (c == '[') {
                    list.add(new Token(TokenType.LBRACK, "[", lineNo));
                } else if (c == ']') {
                    list.add(new Token(TokenType.RBRACK, "]", lineNo));
                } else if (c == '{') {
                    list.add(new Token(TokenType.LBRACE, "{", lineNo));
                } else if (c == '}') {
                    list.add(new Token(TokenType.RBRACE, "}", lineNo));
                } else if (c == '"') {
                    // FormatString 格式化输出
                    StringBuilder builder = new StringBuilder();
                    builder.append('"');
                    c = line.charAt(idx++);
                    while (c != '"') {
                        if (c == '\\' && line.charAt(idx) != '"' && line.charAt(idx) == 'n') {
                            builder.append("\n");
                            idx++;
                            c = line.charAt(idx++);
                        } else {
                            builder.append(c);
                            c = line.charAt(idx++);
                        }
                    }
                    builder.append(c);
                    Token printfString = new Token(TokenType.STRCON, builder.toString(), lineNo);
                    list.add(printfString);
                } else {
                    // illegal?
                    continue;
                }
            }
        }
        return list;
    }

    public Token lexToken(String tokenVal) {
        TokenType tokenType = TokenMap.getTokenType(tokenVal);
        return new Token(tokenType, tokenVal, lineNo);
    }

}
