
/**
 * @author Katrina Zhu
 *
 */
public class Huff {

    public static void main(String[] args){
        HuffViewer sv = new HuffViewer("Duke Compsci Huffing");
        IHuffProcessor proc = new SimpleHuffProcessor();
        sv.setModel(proc);    
    }
}
