package Components;

import java.util.ArrayList;

public class Syntax implements Item{
    // 每一个语法成分元素，只需要在最后打印结果时输出
    private SyntaxType type;
    private ArrayList<Item> components; //保存组成本层的所有子item

    public String toString() {
        // 深度优先遍历所有子节点，最后输出自己的<语法成分>
        String str = new String();
        for (Item item: this.components) {
            str = str + item.toString() + "\n";
        }
        str = str + "<" + this.type.toString() + ">";
        return str;
    }
    public Syntax(SyntaxType type, ArrayList<Item> list) {
        this.type = type;
        this.components = list;
    }
}
