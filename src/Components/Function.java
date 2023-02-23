package Components;

import Components.Token;

import java.util.ArrayList;

public class Function {
    private String value; // name...
    private String returnType;
    private ArrayList<Integer> paras;
    private int index; // 最终在生成代码执行栈中的位置
    private int parasSize = 0;

    public Function(Token token, String returnType) {
        this.value = token.getVal();
        this.returnType = returnType;
    }

    public void setParas(ArrayList<Integer> paras) {
        this.paras = paras;
    }

    public String getValue() {
        return value;
    }

    public String getReturnType() {
        return returnType;
    }

    public int getParasSize() {
        if (parasSize == 0) {
            return paras.size();
        } else {
            return parasSize;
        }
    }

    public void setParasSize(int parasSize) {
        this.parasSize = parasSize;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public ArrayList<Integer> getParas() {
        return paras;
    }
}
