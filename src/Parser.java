import CodeGen.MidType;
import CodeGen.PCode;
import Components.*;
import Errors.ErrorLog;
import Errors.MyException;
import symbolTableEl.Symbol;
import symbolTableEl.SymbolTable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Parser {
    private ArrayList<Token> tokens;
    private int index;
    private Token sym; // 当前正在处理的token
    private int cycleDepth;
    private Boolean retInt; // 当前解析函数的类型
    private int scopeId; // 记录作用域编号
    private int scope; // 当前嵌套了几层作用域
    //    private HashMap<Integer, SymbolTable> symbols = new HashMap<>(); // <scopeId, 当前作用域内的一个符号表>
    private ArrayList<SymbolTable> symbols = new ArrayList<>(); // <scopeId, 当前作用域内的一个符号表>
    private ArrayList<PCode> codes = new ArrayList<>(); //在语法分析的过程中生成pcode
    private HashMap<String, Function> functions = new HashMap<>(); // 全部函数表
    private boolean needReturn;
    private ArrayList<HashMap<String, String>> ifLabels = new ArrayList<>();
    private ArrayList<HashMap<String, String>> whileLabels = new ArrayList<>();
    private int cnt = 0;
    private int rightBraceLineNo = 0;

    private String getLabel(String type) {
        cnt++;
        return "label_" + type + "_"+ cnt;
    }

    public Parser(ArrayList<Token> list) {
        this.tokens = list;
        this.index = 0;
        this.cycleDepth = 0;
        this.retInt = false;
        scopeId = -1;
        scope = -1;
        needReturn = false;
    }

    public void parse() {
        //从头开始进行语法分析
        if (getNextSym()) {
            parseCompUnit();
        }
        //printPCode();
    }

    private boolean finish() {
        return index >= tokens.size();
    }

    private int getCurIndex() {
        return index - 1; //index指向下一个要解析的token位置
    }

    private void addSymbol(Token token, String type, int dimensions, int scopeId) {
        symbols.get(scope).addSymbol(type, dimensions, token, scopeId);
    }

    private void errorCheckA() {
        String str = sym.getVal();
        for (int i = 1; i < str.length()-1; i++) {
            char c = str.charAt(i);
            if (c == '%') {
                if ((i + 1) >= str.length()) {
                    ErrorLog.addError(new MyException(sym.getLineNo(), 'a'));
                    return;
                } else if (str.charAt(i + 1) != 'd') {
                    ErrorLog.addError(new MyException(sym.getLineNo(), 'a'));
                    return;
                }
            } else if (c == '\\') {
                if ((i + 1) >= str.length()) {
                    ErrorLog.addError(new MyException(sym.getLineNo(), 'a'));
                    return;
                } else if (str.charAt(i + 1) != 'n') {
                    ErrorLog.addError(new MyException(sym.getLineNo(), 'a'));
                    return;
                }
            } else if ((c < 32 && c != '\n') || ((c > 33) && (c < 40)) || (c > 126)) {
                ErrorLog.addError(new MyException(sym.getLineNo(), 'a'));
                return;
            }
        }
    }

    // CompUnit → {Decl} {FuncDef} MainFuncDef
    // 1.是否存在Decl 2.是否存在FuncDef
    public void parseCompUnit() {
        enterSub();

        int preIndex; //记录之前扫描位置，便于偷看
        // 1.Decl
        while (!finish()) {
            // Decl → ConstDecl | VarDecl 覆盖两种声明
            preIndex = (index - 1);
            // ConstDecl
            if (sym.getType().equals(TokenType.CONSTTK)) {
                parseConstDecl();
                continue;
            } // VarDecl
            else if (sym.getType().equals(TokenType.INTTK)) {
                getNextSym(); // 可能是int main
                if (sym.getType().equals(TokenType.IDENFR)) {
                    // VarDecl → BType VarDef { ',' VarDef } ';'
                    getNextSym();
                    if (sym.getType().equals(TokenType.LBRACK) || sym.getType().equals(TokenType.ASSIGN) || sym.getType().equals(TokenType.COMMA) || sym.getType().equals(TokenType.SEMICN)) {
                        rollBack(preIndex);
                        parseVarDecl();
                        continue;
                    }
                }
            }
            rollBack(preIndex);
            break; //不属于decl范围直接跳到FuncDef范围
        }

        //FuncDef → int/void Ident '(' [FuncFParams] ')' Block
        while (!finish()) {
            preIndex = getCurIndex();
            if (sym.getType().equals(TokenType.INTTK) || sym.getType().equals(TokenType.VOIDTK)) {
                getNextSym();
                if (sym.getType().equals(TokenType.IDENFR)) {
                    getNextSym();
                    if (sym.getType().equals(TokenType.LPARENT)) {
                        rollBack(preIndex);
                        parseFuncDef();
                        continue;
                    }
                } else if (sym.getType().equals(TokenType.MAINTK)) {
                    rollBack(preIndex);
                    parseMainFuncDef();
                }
            }
        }
        outSub();
    }

    // 'int' 'main' '(' ')' Block
    private void parseMainFuncDef() {
        needReturn = true;
        if (sym.getType().equals(TokenType.INTTK)) {
            getNextSym();
            if (sym.getType().equals(TokenType.MAINTK)) {
                Function function = new Function(sym, "int");
                function.setParas(new ArrayList<>());
                functions.put("main", function);
                codes.add(new PCode(MidType.MAIN, sym.getVal()));
                getNextSym();
                if (sym.getType().equals(TokenType.LPARENT)) {
                    getNextSym();
                    if (sym.getType().equals(TokenType.RPARENT)) {
                        getNextSym();
                    } else {
                        // miss ')'
                    }
                    boolean isReturn = parseBlock(false);
                    if (needReturn && !isReturn) {
                        ErrorLog.addError(new MyException(rightBraceLineNo, 'g'));
                    }
                    codes.add(new PCode(MidType.EXIT));
                }
            }
        }
    }

    // FuncType Ident '(' [FuncFParams] ')' Block
    private void parseFuncDef() {
        // FuncType
        String retType = parseFuncType();
        if (sym.getType().equals(TokenType.IDENFR)) {
//            if (functions.containsKey(sym.getVal()) || (symbols.containsKey(0) && symbols.get(0).hasSymbol(sym.getVal()))) { // 全局变量和函数不能重名
            if (functions.containsKey(sym.getVal()) || (symbols.size() > 0 && symbols.get(0).hasSymbol(sym.getVal()))) { // 全局变量和函数不能重名
                ErrorLog.addError(new MyException(sym.getLineNo(), 'b'));
            }
            ArrayList<Integer> paras = new ArrayList<>();
            PCode code = new PCode(MidType.FUNC, sym.getVal());
            codes.add(code);
            Function function = new Function(sym, retType);
            // 解析完函数名了，进入新的作用域
            enterSub();
            getNextSym();
            if (sym.getType().equals(TokenType.LPARENT)) {
                getNextSym();
                if (sym.getType().equals(TokenType.RPARENT)) {
                    getNextSym();
                } else {
                    paras = parseFParams();
                    if (sym.getType().equals(TokenType.RPARENT)) {
                        getNextSym();
                    } else {
                        ErrorLog.addError(new MyException(tokens.get(index - 2).getLineNo(), 'j'));
                    }
                }
                function.setParas(paras);
                functions.put(function.getValue(), function);
                if (function.getReturnType().equals("int")) {
                    needReturn = true;
                } else {
                    needReturn = false;
                }
                boolean isReturn = parseBlock(true);
                if (needReturn && !isReturn) {
                    ErrorLog.addError(new MyException(rightBraceLineNo, 'g'));
                }
                outSub();
                code.setValue2(function);
                codes.add(new PCode(MidType.RET, 0));
                // codes.add(new PCode(MidType.RET, hasRet ? 1 : 0)); //TODO:为什么Value1表达的是是否有返回值，这里却直接置了0
                codes.add(new PCode(MidType.ENDFUNC));
            }
        }
    }

    // '{' { BlockItem } '}'
    // Decl | Stmt
    private boolean parseBlock(boolean fromFunc) {
        if (!fromFunc) {
            enterSub();
        }
        boolean isReturn = false;
        if (sym.getType().equals(TokenType.LBRACE)) {
            getNextSym();
            while (!sym.getType().equals(TokenType.RBRACE)) {
                if (sym.getType().equals(TokenType.INTTK)) {
                    parseVarDecl();
                } else if (sym.getType().equals(TokenType.CONSTTK)) {
                    parseConstDecl();
                } else {
                    isReturn = parseStmt();
                }
            }
            if (sym.getType().equals(TokenType.RBRACE)) {
                rightBraceLineNo = sym.getLineNo();
                getNextSym();
            }
        }
        if (!fromFunc) {
            outSub();
        }
        return isReturn;
    }

    private boolean parseStmt() {
        boolean isReturn = false;

        //'if' '(' Cond ')' Stmt [ 'else' Stmt ]
        if (sym.getType().equals(TokenType.IFTK)) {
            ifLabels.add(new HashMap<>());
            ifLabels.get(ifLabels.size() - 1).put("if", getLabel("if"));
            ifLabels.get(ifLabels.size() - 1).put("else", getLabel("else"));
            ifLabels.get(ifLabels.size() - 1).put("if_end", getLabel("if_end"));
            ifLabels.get(ifLabels.size() - 1).put("if_block", getLabel("if_block"));
            // 准备进入body啦，加入一个if开始的标签~
            codes.add(new PCode(MidType.LABEL, ifLabels.get(ifLabels.size() - 1).get("if")));
            getNextSym();
            if (sym.getType().equals(TokenType.LPARENT)) {
                getNextSym();
                parseCond(String.valueOf(TokenType.IFTK)); // from where?
                if (sym.getType().equals(TokenType.RPARENT)) {
                    getNextSym();
                } else {
                    ErrorLog.addError(new MyException(tokens.get(index - 2).getLineNo(), 'j'));
                }
                // if (condition)解析完了 看是跳转到else部分还是if-body部分
                codes.add(new PCode(MidType.JZ, ifLabels.get(ifLabels.size() - 1).get("else")));
                // 准备进入body啦，加入一个if-body开始的标签~
                codes.add(new PCode(MidType.LABEL, ifLabels.get(ifLabels.size() - 1).get("if_block")));
                parseStmt();
                // if结束直接就跳转到if-end了，加入无条件跳转
                codes.add(new PCode(MidType.JUMP, ifLabels.get(ifLabels.size() - 1).get("if_end")));
                // 准备进入else部分，插入标签
                codes.add(new PCode(MidType.LABEL, ifLabels.get(ifLabels.size() - 1).get("else")));
                if (sym.getType().equals(TokenType.ELSETK)) {
                    getNextSym();
                    parseStmt();
                }
                codes.add(new PCode(MidType.LABEL, ifLabels.get(ifLabels.size() - 1).get("if_end")));
                ifLabels.remove(ifLabels.size() - 1);
            }
        }
        // 'while' '(' Cond ')' Stmt
        else if (sym.getType().equals(TokenType.WHILETK)) {
            //TODO:或许可以把标签名改成while_begin...
            whileLabels.add(new HashMap<>());
            whileLabels.get(whileLabels.size() - 1).put("while", getLabel("while"));
            whileLabels.get(whileLabels.size() - 1).put("while_end", getLabel("while_end"));
            whileLabels.get(whileLabels.size() - 1).put("while_block", getLabel("while_block"));
            codes.add(new PCode(MidType.LABEL, whileLabels.get(whileLabels.size() - 1).get("while")));
            cycleDepth++;
            getNextSym();
            if (sym.getType().equals(TokenType.LPARENT)) {
                getNextSym();
                parseCond(String.valueOf(TokenType.WHILETK));
                if (sym.getType().equals(TokenType.RPARENT)) {
                    getNextSym();
                } else {
                    ErrorLog.addError(new MyException(tokens.get(index - 2).getLineNo(), 'j'));
                }
                codes.add(new PCode(MidType.JZ, whileLabels.get(whileLabels.size() - 1).get("while_end")));
                codes.add(new PCode(MidType.LABEL, whileLabels.get(whileLabels.size() - 1).get("while_block")));
                parseStmt();
                cycleDepth--;
                // 无条件地跳转到while去做判断
                codes.add(new PCode(MidType.JUMP, whileLabels.get(whileLabels.size() - 1).get("while")));
                codes.add(new PCode(MidType.LABEL, whileLabels.get(whileLabels.size() - 1).get("while_end")));
                whileLabels.remove(whileLabels.size() - 1);
            }
        }
        //'break' ';' | 'continue' ';'
        else if (sym.getType().equals(TokenType.BREAKTK) || sym.getType().equals(TokenType.CONTINUETK)) {
            TokenType type = sym.getType();
            if (cycleDepth == 0) {
                ErrorLog.addError(new MyException(sym.getLineNo(), 'm'));
                getNextSym();
            } else {
                int lineNo = sym.getLineNo();
                getNextSym();
                if (sym.getType().equals(TokenType.SEMICN)) {
                    getNextSym();
                } else {
                    ErrorLog.addError(new MyException(lineNo, 'i'));
                }
                if (type.equals(TokenType.BREAKTK)) {
                    codes.add(new PCode(MidType.JUMP, whileLabels.get(whileLabels.size() - 1).get("while_end")));
                } else {
                    codes.add(new PCode(MidType.JUMP, whileLabels.get(whileLabels.size() - 1).get("while")));
                }
            }
        }
        //'return' [Exp] ';'
        else if (sym.getType().equals(TokenType.RETURNTK)) {
            isReturn = true; // 这是一条return语句
            boolean flag = false; // return a real number
            getNextSym();
            if (sym.getType().equals(TokenType.SEMICN)) {
                getNextSym(); //TODO:如果不是“;”就一定是Exp的一部分吗
            } else {
                if (!needReturn) {
                    ErrorLog.addError(new MyException(sym.getLineNo(), 'f'));
                }
                parseExp();
                flag = true;
                if (sym.getType().equals(TokenType.SEMICN)) {
                    getNextSym();
                } else {
                    ErrorLog.addError(new MyException((tokens.get(index-2)).getLineNo(), 'i'));
                }
            }
            codes.add(new PCode(MidType.RET, flag ? 1 : 0));
        }
        // 'printf''('FormatString{','Exp}')'';'
        else if (sym.getType().equals(TokenType.PRINTFTK)) {
            int paraNum = 0; // 待打印的%d有几个
            int lineNo = sym.getLineNo();
            getNextSym();
            if (sym.getType().equals(TokenType.LPARENT)) {
                getNextSym();
                if (sym.getType().equals(TokenType.STRCON)) {
                    Token strCon = sym;
                    errorCheckA();  //TODO: errorCheckA
                    getNextSym();
                    while (sym.getType().equals(TokenType.COMMA)) {
                        getNextSym();
                        parseExp();
                        paraNum++;
                    }
                    int formatNum = strCon.getFormatNum();
                    if (formatNum != paraNum) {
                        ErrorLog.addError(new MyException(lineNo, 'l'));
                    }
                    if (sym.getType().equals(TokenType.RPARENT)) {
                        getNextSym();
                    } else {
                        ErrorLog.addError(new MyException(tokens.get(index - 2).getLineNo(), 'j'));
                    }
                    if (sym.getType().equals(TokenType.SEMICN)) {
                        getNextSym();
                    } else {
                        ErrorLog.addError(new MyException((tokens.get(index-2)).getLineNo(), 'i'));
                    }
                    codes.add(new PCode(MidType.PRINT, strCon.getVal(), paraNum));
                }
            }
        }
        // { : Block
        else if (sym.getType().equals(TokenType.LBRACE)) {
            parseBlock(false);
        }
        else if (sym.getType().equals(TokenType.LPARENT) || sym.getType().equals(TokenType.INTCON) || sym.getType().equals(TokenType.PLUS) || sym.getType().equals(TokenType.MINU) || sym.getType().equals(TokenType.NOT)) {
            parseExp();
        } // LVal 或者 Exp
        else if (sym.getType().equals(TokenType.IDENFR)) {
            int prevIndex = getCurIndex();
            Token ident = sym;
            Token next = tokens.get(index);
            // TODO:如果是多维数组怎么预读?

            // 先当LVal识别了，如果不是 = 再回退
            if (tryToGetLVal()) {
//            if (next.getType().equals(TokenType.ASSIGN)) {
                // LVal：在此处LVal要么被getint()赋值，要么被exp赋值，都是被赋值的，所以要获得ADDRESS
                int dimensions = parseLVal();
                if (hasSymbol(ident)) {
                    codes.add(new PCode(MidType.ADDRESS, getSymbol(ident).getScopeId() + "_" + ident.getVal(), dimensions));
                }
                if (isConst(ident)) {
                    ErrorLog.addError(new MyException(ident.getLineNo(), 'h'));
                }
                getNextSym(); // '='

                if (sym.getType().equals(TokenType.GETINTTK)) {
                    getNextSym();
                    codes.add(new PCode(MidType.GETINT));
                    if (sym.getType().equals(TokenType.LPARENT)) {
                        getNextSym();
                        if (sym.getType().equals(TokenType.RPARENT)) {
                            getNextSym();
                        } else {
                            ErrorLog.addError(new MyException(tokens.get(index - 2).getLineNo(), 'j'));
                        }
                        if (sym.getType().equals(TokenType.SEMICN)) {
                            getNextSym();
                        } else {
                            ErrorLog.addError(new MyException((tokens.get(index-2)).getLineNo(), 'i'));
                        }
                    }
                } else {
                    parseExp();
                    if (sym.getType().equals(TokenType.SEMICN)) {
                        getNextSym();
                    } else {
                        ErrorLog.addError(new MyException((tokens.get(index-2)).getLineNo(), 'i'));
                    }
                }
                if (hasSymbol(ident))
                    codes.add(new PCode(MidType.POP, getSymbol(ident).getScopeId() + "_" + ident.getVal()));
            }
            else {
                rollBack(prevIndex);
                parseExp();
                if (sym.getType().equals(TokenType.SEMICN)) {
                    getNextSym();
                } else {
                    ErrorLog.addError(new MyException((tokens.get(index-2)).getLineNo(), 'i'));
                }
            }
        } else if (sym.getType().equals(TokenType.SEMICN)) {
            // [Exp] ';' 无Exp的情况, 这种情况似乎不需要在意...
            getNextSym();
        } else {
            ErrorLog.addError(new MyException((tokens.get(index-2)).getLineNo(), 'i'));
        }
        return isReturn;
    }

    private void enterSub() {
        scope++;
        scopeId++;
        symbols.add(scope, new SymbolTable());
//        symbols.put(scope, new SymbolTable()); // 用过出去之后就要销毁，所以用scope而非scopeId存
    }

    private void outSub() {
        symbols.remove(scope);
        scope--;
    }

    // Cond → LOrExp
    private void parseCond(String from) {
        parseLOrExp(from);
    }

    // LAndExp | LOrExp '||' LAndExp
    private void parseLOrExp(String from) {
        String label = getLabel("cond_" + 0);
        parseLAndExp(from, label);
        codes.add(new PCode(MidType.LABEL, label));
        if (sym.getType().equals(TokenType.OR)) {
            if (from.equals(String.valueOf(TokenType.IFTK))) {
                codes.add(new PCode(MidType.JNZ, ifLabels.get(ifLabels.size() - 1).get("if_block")));
            } else if (from.equals(String.valueOf(TokenType.WHILETK))){
                codes.add(new PCode(MidType.JNZ, whileLabels.get(whileLabels.size() - 1).get("while_block")));
            }
        }
        int i = 1;
        while (sym.getType().equals(TokenType.OR)) {
            // 短路求值，如果||中有一项为真就直接跳到if-body去(进入if语句内部)
            getNextSym();
            label = getLabel("cond_" + i);
            parseLAndExp(from, label);
            codes.add(new PCode(MidType.LABEL, label));
            codes.add(new PCode(MidType.OR));
            if (sym.getType().equals(TokenType.OR)) {
                if (from.equals(String.valueOf(TokenType.IFTK))) {
                    codes.add(new PCode(MidType.JNZ, ifLabels.get(ifLabels.size() - 1).get("if_block")));
                } else if (from.equals(String.valueOf(TokenType.WHILETK))){
                    codes.add(new PCode(MidType.JNZ, whileLabels.get(whileLabels.size() - 1).get("while_block")));
                }
            }
            i++;
        }
    }

    // EqExp | LAndExp '&&' EqExp
    private void parseLAndExp(String from, String label) {
        parseEqExp();
        if (sym.getType().equals(TokenType.AND)) {
            codes.add(new PCode(MidType.JZ, label));
        }

        while (sym.getType().equals(TokenType.AND)) {
            TokenType type = sym.getType();
            getNextSym();
            parseEqExp();
            codes.add(new PCode(MidType.AND));
            if (sym.getType().equals(TokenType.AND)) {
                codes.add(new PCode(MidType.JZ, label));
            }
        }
    }

    //  RelExp | EqExp ('==' | '!=') RelExp
    private void parseEqExp() {
        parseRelExp();

        while (sym.getType().equals(TokenType.EQL) || sym.getType().equals(TokenType.NEQ)) {
            TokenType type = sym.getType();
            getNextSym();
            parseRelExp();
            if (type.equals(TokenType.EQL)) {
                codes.add(new PCode(MidType.CMPEQ));
            } else {
                codes.add(new PCode(MidType.CMPNE));
            }
        }
    }

    // AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private void parseRelExp() {
        parseAddExp();

        while (sym.getType().equals(TokenType.LSS) || sym.getType().equals(TokenType.GRE) || sym.getType().equals(TokenType.LEQ) || sym.getType().equals(TokenType.GEQ)) {
            TokenType type = sym.getType();
            getNextSym();
            parseAddExp();
            if (type.equals(TokenType.LSS)) {
                codes.add(new PCode(MidType.CMPLT));
            } else if (type.equals(TokenType.LEQ)) {
                codes.add(new PCode(MidType.CMPLE));
            } else if (type.equals(TokenType.GRE)) {
                codes.add(new PCode(MidType.CMPGT));
            } else {
                codes.add(new PCode(MidType.CMPGE));
            }
        }
    }

    // AddExp → MulExp | AddExp ('+' | '−') MulExp
    // 对文法进行改写：AddExp -> MulExp {('+' | '-') MulExp}
    private int parseAddExp() {
        int dimensions = 0;
        dimensions = parseMulExp();
        while (sym.getType().equals(TokenType.PLUS) || sym.getType().equals(TokenType.MINU)) {
            TokenType type = sym.getType();
            getNextSym();
            dimensions = parseMulExp();
            if (type.equals(TokenType.PLUS)) {
                codes.add(new PCode(MidType.ADD));
            } else {
                codes.add(new PCode(MidType.SUB));
            }
        }
        return dimensions;
    }

    // MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    private int parseMulExp() {
        int dimensions = 0;
        dimensions = parseUnaryExp();

        while (sym.getType().equals(TokenType.MULT) || sym.getType().equals(TokenType.DIV) || sym.getType().equals(TokenType.MOD) || sym.getType().equals(TokenType.BITAND)) {
            TokenType type = sym.getType();
            getNextSym();
            dimensions = parseUnaryExp();
            if (type.equals(TokenType.MULT)) {
                codes.add(new PCode(MidType.MUL));
            } else if (type.equals(TokenType.DIV)) {
                codes.add(new PCode(MidType.DIV));
            } else if (type.equals(TokenType.MOD)) {
                codes.add(new PCode(MidType.MOD));
            } else {
                codes.add(new PCode(MidType.BITAND));
            }
        }
        return dimensions;
    }

    // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    private int parseUnaryExp() {
        // 根据第一个符号的不同进行区分
        // PrimaryExp → '(' Exp ')' | LVal | Number
        // LVal → Ident {'[' Exp ']'}
        // Number → IntConst
        // (和intConst是可以与UnaryExp的其他两种解析路线区分开的
        int dimensions = 0;

        if (sym.getType().equals(TokenType.INTCON) || sym.getType().equals(TokenType.LPARENT)) {
            dimensions = parsePrimaryExp();
        } else if (sym.getType().equals(TokenType.PLUS) || sym.getType().equals(TokenType.MINU) || sym.getType().equals(TokenType.NOT)) {
            TokenType opType = sym.getType();
            TokenType type = parseUnaryOp();
            dimensions = parseUnaryExp();
            if (opType.equals(TokenType.PLUS)) {
                codes.add(new PCode(MidType.POS));
            } else if (opType.equals(TokenType.MINU)) {
                codes.add(new PCode(MidType.NEG));
            } else {
                codes.add(new PCode(MidType.NOT));
            }
        } else if (sym.getType().equals(TokenType.IDENFR)) {
            Token next = tokens.get(index);
            String name = sym.getVal();
            if (next.getType().equals(TokenType.LPARENT)) {
                // Ident '(' [FuncRParams] ')'
                int lineNo = sym.getLineNo();
                Token ident = sym;
                ArrayList<Integer> paras = null;
                if (!hasFunction(ident)) {
                    ErrorLog.addError(new MyException(ident.getLineNo(), 'c'));
                }
                else {
                    paras = functions.get(ident.getVal()).getParas(); // 这个函数声明的 需要的参数
                }
                getNextSym(); // (
                getNextSym();
                if (sym.getType().equals(TokenType.RPARENT)) {
                    getNextSym();
                } else {
                    ArrayList<Integer> rparas = (parseFuncRParams());
                    if (sym.getType().equals(TokenType.RPARENT)) {
                        getNextSym();
                        checkParas(lineNo, paras, rparas);
                    } else {
                        ErrorLog.addError(new MyException(tokens.get(index - 2).getLineNo(), 'j'));
                    }
                }
                codes.add(new PCode(MidType.CALL, ident.getVal()));
                if (hasFunction(ident)) {
                    if (functions.get(ident.getVal()).getReturnType().equals("void")) {
                        dimensions = -1; //没有返回值
                    }
                }
            } else {
                dimensions = parsePrimaryExp();
            }
        }
        return dimensions;
    }

    // FuncFParam { ',' FuncFParam }
    private ArrayList<Integer> parseFParams() {
        ArrayList<Integer> list = new ArrayList<>();
        int dimensions = parseFParam();
        list.add(dimensions);
        while (sym.getType().equals(TokenType.COMMA)) {
            getNextSym();
            dimensions = parseFParam();
            list.add(dimensions);
        }
        return list;
    }

    private String parseFuncType() {
        TokenType type = sym.getType();
        String string = (type.equals(TokenType.INTTK)) ? "int" : "void";
        getNextSym();
        return string;
    }

    // int VarDef { ',' VarDef } ';'
    private void parseVarDecl() {
        if (sym.getType().equals(TokenType.INTTK)) {
            getNextSym();
            parseVarDef();
            while (sym.getType().equals(TokenType.COMMA)) {
                getNextSym();
                parseVarDef();
            }
            if (sym.getType().equals(TokenType.SEMICN)) {
                getNextSym();
            } else {
                ErrorLog.addError(new MyException((tokens.get(index-2)).getLineNo(), 'i'));
            }
        }
    }

    // Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    private void parseVarDef() {
        String name = null;
        int dimensions = 0;
        Token ident = sym;

        if (sym.getType().equals(TokenType.IDENFR)) {
            hasSymbolInThisArea(sym);
            codes.add(new PCode(MidType.VAR, scopeId+"_"+sym.getVal()));
            name = sym.getVal();
            getNextSym();
            while (sym.getType().equals(TokenType.LBRACK)) {
                dimensions++;
                getNextSym();
                parseConstExp();
                if (sym.getType().equals(TokenType.RBRACK)) {
                    getNextSym();
                } else {
                    ErrorLog.addError(new MyException(tokens.get(index - 2).getLineNo(), 'k'));
                }
            }
            // 有点像是给上一个VAR做了补充说明
            if (dimensions > 0) {
                codes.add(new PCode(MidType.DIMVAR, scopeId+"_"+ident.getVal(), dimensions));
            }
            // 因为是defination, 所以向符号表中加入符号
            addSymbol(ident, "var", dimensions, scopeId);
            if (sym.getType().equals(TokenType.ASSIGN)) {
                getNextSym();
                parseInitVal();
            } else {
                // 暂时没有赋初值，增加占位符
                codes.add(new PCode(MidType.PLACEHOLDER, scopeId+"_"+ident.getVal(), dimensions));
            }
        }
    }

    // 变量初值;1.表达式初值 2.一维数组初值 3.二维数组初值
    // 处理方法同常量初值
    // Exp | '{' [ InitVal { ',' InitVal } ] '}'
    private void parseInitVal() {
        if (sym.getType().equals(TokenType.LBRACE)) {
            getNextSym();
            if (sym.getType().equals(TokenType.RBRACE)) {
                getNextSym();
            } else {
                parseInitVal();
                while (sym.getType().equals(TokenType.COMMA)) {
                    getNextSym();
                    parseInitVal();
                }
                if (sym.getType().equals(TokenType.RBRACE)) {
                    getNextSym();
                }
            }
        } else if (sym.getType().equals(TokenType.GETINTTK)) {
            getNextSym();
            codes.add(new PCode(MidType.GETINT));
            if (sym.getType().equals(TokenType.LPARENT)) {
                getNextSym();
                if (sym.getType().equals(TokenType.RPARENT)) {
                    getNextSym();
                } else {
                    ErrorLog.addError(new MyException(tokens.get(index - 2).getLineNo(), 'j'));
                }
                if (sym.getType().equals(TokenType.SEMICN)) {
                    getNextSym();
                } else {
                    ErrorLog.addError(new MyException((tokens.get(index-2)).getLineNo(), 'i'));
                }
            }
        } else {
            parseExp();
        }
    }

    // ConstDecl → 'const' int ConstDef { ',' ConstDef } ';'
    private void parseConstDecl() {
        if (sym.getType().equals(TokenType.CONSTTK)) {
            getNextSym();
            if (sym.getType().equals(TokenType.INTTK)) {
                getNextSym();
                parseConstDef();
                while (sym.getType().equals(TokenType.COMMA)) {
                    getNextSym();
                    parseConstDef();
                }
                if (sym.getType().equals(TokenType.SEMICN)) {
                    getNextSym();
                } else {
                    ErrorLog.addError(new MyException((tokens.get(index-2)).getLineNo(), 'i'));
                }
            }
        }
    }

    // ConstDef → Ident { '[' ConstExp ']' } '=' ConstInitVal
    // 常量定义：包含普通变量、一维数组、二维数组共三种情况
    private void parseConstDef() {
        String name = null;
        int dimensions = 0;
        Token ident = sym;
        if ((sym.getType().equals(TokenType.IDENFR))) {
            hasSymbolInThisArea(sym); // TODO: errorCheckB
            codes.add(new PCode(MidType.VAR, scopeId + "_" + ident.getVal()));
            name = sym.getVal();
            getNextSym();
            // ConstExp → AddExp
            while (sym.getType().equals(TokenType.LBRACK)) {
                dimensions++;
                getNextSym();
                parseConstExp();
                if (sym.getType().equals(TokenType.RBRACK)) {
                    getNextSym();
                } else {
                    ErrorLog.addError(new MyException(tokens.get(index - 2).getLineNo(), 'k'));
                }
            }
            if (dimensions > 0) {
                codes.add(new PCode(MidType.DIMVAR, scopeId+"_"+ident.getVal(), dimensions));
            }
            addSymbol(ident, "const", dimensions, scopeId);
            if (sym.getType().equals(TokenType.ASSIGN)) {
                getNextSym();
                parseConstInitVal();
            }
        }
    }

    // 常量初值 // 1.常表达式初值 2.一维数组初值 3.二维数组初值
    // ConstExp
    //  | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    private void parseConstInitVal() {
        if (!sym.getType().equals(TokenType.LBRACE)) {
            parseConstExp();
        } else if (sym.getType().equals(TokenType.LBRACE)){
            getNextSym();
            if (sym.getType().equals(TokenType.RBRACE)) {
                getNextSym();
            } else {
                parseConstInitVal();
                while (sym.getType().equals(TokenType.COMMA)) {
                    getNextSym();
                    parseConstInitVal();
                }
                if (sym.getType().equals(TokenType.RBRACE)) {
                    getNextSym();
                }
            }
        }
    }

    // ConstExp → AddExp
    private void parseConstExp() {
        parseAddExp();
    }

    // FuncRParams → Exp { ',' Exp }
    private ArrayList<Integer> parseFuncRParams() {
        ArrayList<Integer> rparas = new ArrayList<>();
        int dimensions = parseExp();
        rparas.add(dimensions);
        codes.add(new PCode(MidType.RPARA, dimensions));
        while (sym.getType().equals(TokenType.COMMA)) {
            getNextSym();
            dimensions = parseExp();
            rparas.add(dimensions);
            codes.add(new PCode(MidType.RPARA, dimensions));
        }
        return rparas;
    }

    // 函数形参
    // FuncFParam → int Ident ['[' ']' { '[' ConstExp ']' }]
    private int parseFParam() {
        int dimensions = 0;

        if (sym.getType().equals(TokenType.INTTK)) {
            getNextSym();
            if (sym.getType().equals(TokenType.IDENFR)) {
                hasSymbolInThisArea(sym);
                Token ident = sym;
                getNextSym();
                // [][2][3][4]...
                if (sym.getType().equals(TokenType.LBRACK)) {
                    dimensions++;
                    getNextSym();
                    if (sym.getType().equals(TokenType.RBRACK)) { //[]
                        getNextSym();
                    } else {
                        ErrorLog.addError(new MyException(tokens.get(index - 2).getLineNo(), 'k'));
                    }
                    while (sym.getType().equals(TokenType.LBRACK)) { // 如果不止二维数组就要用while
                        dimensions++;
                        getNextSym();
                        parseConstExp();
                        if (sym.getType().equals(TokenType.RBRACK)) {
                            getNextSym();
                        } else {
                            ErrorLog.addError(new MyException(tokens.get(index - 2).getLineNo(), 'k'));
                        }
                    }
                }
                // 一个形式参数解析完了
                codes.add(new PCode(MidType.PARA, scopeId + "_" + ident.getVal(), dimensions));
                addSymbol(ident, "para", dimensions, scopeId);
            }
        }
        return dimensions;
    }

    private void rollBack(int preIndex) {
        this.index = preIndex;
        sym = tokens.get(index);
        index++;
    }

    private TokenType parseUnaryOp() {
        TokenType type = sym.getType();
        getNextSym();
        return type;
    }

    // PrimaryExp → '(' Exp ')' | LVal | Number
    private int parsePrimaryExp() {
        int dimensions = 0;
        if (sym.getType().equals(TokenType.LPARENT)) {
            getNextSym();
            parseExp();
            if (sym.getType().equals(TokenType.RPARENT)) {
                getNextSym(); // 只有在处理到vt时才用获取下一个
            }
        } else if (sym.getType().equals(TokenType.IDENFR)) { // LVal → Ident {'[' Exp ']'} 1.普通变量 2.一维数组 3.二维数组
            Token ident = sym;
            dimensions = parseLVal();
            //TODO:传值不是传地址？？？
            if (hasSymbol(ident)) {
                if (dimensions == 0) {
                    codes.add(new PCode(MidType.VALUE, getSymbol(ident).getScopeId() + "_" + ident.getVal(), dimensions));
                } else {
                    codes.add(new PCode(MidType.ADDRESS, getSymbol(ident).getScopeId() + "_" + ident.getVal(), dimensions));
                }
            }
        } else if (sym.getType().equals(TokenType.INTCON)) {
            parseNumber();
        }
        return dimensions;
    }

    private void parseNumber() {
        codes.add(new PCode(MidType.PUSH, Integer.parseInt(sym.getVal())));
        getNextSym();
    }

    // LVal → Ident {'[' Exp ']'} 1.普通变量 2.一维数组 3.二维数组
    private int parseLVal() {
        int dimensions = 0;
        Token ident = sym;
        if (sym.getType().equals(TokenType.IDENFR)) {
            // codes.add(new PCode(MidType.PUSH, getSymbol(ident).getScopeId() + "_" + ident.getVal()));
            if (!hasSymbol(ident)) {
                ErrorLog.addError(new MyException(sym.getLineNo(), 'c'));
            }
            getNextSym();
            while (sym.getType().equals(TokenType.LBRACK)) {
                dimensions++;
                getNextSym();
                parseExp();
                if (sym.getType().equals(TokenType.RBRACK)) {
                    getNextSym();
                } else {
                    ErrorLog.addError(new MyException(tokens.get(index - 2).getLineNo(), 'k'));
                }
            }
        }
        //TODO:LVal是否一定是被赋值（取地址）
        int realDim = (hasSymbol(ident)) ? (getSymbol(ident).getDimensions() - dimensions) : 0;
        //codes.add(new PCode(MidType.ADDRESS, getSymbol(ident).getScopeId() + "_" + ident.getVal(), realDim));
        return realDim;
    }

    // Exp → AddExp
    private int parseExp() {
        int dimensions = parseAddExp();
        return dimensions;
    }

    private boolean getNextSym() {
        if (index >= tokens.size()) return false;
        else {
            sym = tokens.get(index++);
            return true;
        }
    }

    private boolean hasFunction(Token word) {
        return functions.containsKey(word.getVal());
    }

    // TODO:外层的同名symbol
    private Symbol getSymbol(Token word) {
        Symbol symbol = null;
//        for (SymbolTable s : symbols.values()) {
        for (SymbolTable s : symbols) {
            if (s.hasSymbol(word)) {
                symbol = s.getSymbol(word);
            }
        }
        return symbol;
    }

    public ArrayList<PCode> getCodes() {
        return codes;
    }

    // 通过预读在判断是LVal还是Exp的过程中。。。
    private boolean tryToGetLVal() {
        int index = getCurIndex();
        Token word = tokens.get(index);
        while (true) {
            if (word.getType().equals(TokenType.ASSIGN)) {
                return true;
            } else if (word.getType().equals(TokenType.SEMICN)) {
                return false;
            } else {
                word = tokens.get(++index);
            }
        }
    }

    public void printPCode(String outputpath) throws IOException {
//        for (PCode code : codes) {
//            System.out.println(code);
//        }
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputpath));
        for (PCode code: codes) {
            writer.write(code.toString()+"\n");
        }
        writer.close();
    }

    private void hasSymbolInThisArea(Token token) { // 函数名或者变量名在当前作用域下重复定义-errorB
        if (symbols.get(scope).hasSymbol(token)) {
            ErrorLog.addError(new MyException(sym.getLineNo(), 'b'));
        }
        return;
    }

    private boolean hasSymbol(Token word) {
        for (SymbolTable s : symbols) {
            if (s.hasSymbol(word)) {
                return true;
            }
        }
        return false;
    }

    private void checkParas(int lineNo, ArrayList<Integer> paras, ArrayList<Integer> rparas) {
        if (paras.size() != rparas.size()) {
            ErrorLog.addError(new MyException(lineNo, 'd'));
        } else {
            for (int i = 0; i < paras.size(); i++) {
                if (!paras.get(i).equals(rparas.get(i))) { // 类型就是数组维数
                    ErrorLog.addError(new MyException(lineNo, 'e'));
                }
            }
        }
    }

    private boolean isConst(Token word) {
        for (SymbolTable s : symbols) {
            if (s.hasSymbol(word)) {
                if (s.isConst(word)) {
                    return true;
                }
            }
        }
        return false;
    }
}