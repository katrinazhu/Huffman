import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Class SimpleHuffProcessor that contains methods to aid with and perform the 
 * compression and uncompression of a specified text or picture file of bits.
 * by: Katrina Zhu
 * Net Id: kz37
 * 12.4.2014
 *
 */
public class SimpleHuffProcessor implements IHuffProcessor {
	int[]freqs = new int[256];
	TreeNode huffTree;
    private HuffViewer myViewer;
    String[]myCodes = new String[257];
	ArrayList<Integer>keys = new ArrayList<Integer>();
	/**
	 * method compress that creates a file with header information and compressed
	 * bits corresponding to the original file
	 */
    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {   	
      	BitOutputStream out2 = new BitOutputStream(out);
    	out2.writeBits(BITS_PER_INT, MAGIC_NUMBER);
    	for(int x = 0; x<ALPH_SIZE; x++){
    		out2.writeBits(BITS_PER_INT, freqs[x]);
    	}
    	int num = in.read();
    	//loop to print compressed bits based on bits read
    	while(num!=-1){
    		for(int x = 0; x<myCodes[num].length(); x++){
    			if(myCodes[num].charAt(x)==('0')){
    				out2.writeBits(1, 0);
    			}
    			else{
    				out2.writeBits(1, 1);  
    			}
    		}
    		num = in.read();
    	}
    	//to print PSEUDO-EOF at end of file
    	for(int x = 0; x<myCodes[PSEUDO_EOF].length(); x++){
			if(myCodes[PSEUDO_EOF].charAt(x)==('0')){
				out2.writeBits(1, 0);
			}
			else{
				out2.writeBits(1, 1);
			}
		}
    	out2.close();
    	return totalCodedBits();
    }
    /**
     * calculates the total bits that were coded in the compressed file
     */
    private int totalCodedBits(){
    	int count = 0;
    	for(int i = 0; i<keys.size(); i++){
    		count += myCodes[keys.get(i)].length() * freqs[keys.get(i)];
    	}
    	return count;
    	}
    /**
     * given an ArrayList of single treenodes, builds a tree using Huffman Compression
     * and returns the root of that tree
     */    
    private TreeNode buildTree (ArrayList<TreeNode> forest){   	
    	//base case: when there are no two nodes left to combine,
    	//return the root node
    	if(forest.size()==1)
    		return forest.get(0);
    	else{
		int min = forest.get(0).myWeight;
    	for(int x = 1; x<forest.size(); x++){
			if(forest.get(x).myWeight<min)
				min = forest.get(x).myWeight;
		}
    	TreeNode minNode1 = null;
    	TreeNode minNode2 = null;
    	for(int x = 0; x<forest.size(); x++){
    		if(forest.get(x).myWeight==min){
    			minNode1 = forest.get(x);
    			forest.remove(minNode1);
    			break;
    		}
    	}
    	min = forest.get(0).myWeight;
    	for(int x = 1; x<forest.size(); x++){
			if(forest.get(x).myWeight<min)
				min = forest.get(x).myWeight;
		}
    	for(int x = 0; x<forest.size(); x++){
    		if(forest.get(x).myWeight==min){
    			minNode2 = forest.get(x);
    			forest.remove(minNode2);
    			break;
    		}
    	}
    	//adding a new treenode with leaves minNode 1 and minNode 2 and value -1
    	TreeNode result = new TreeNode(-1, minNode1.myWeight + minNode2.myWeight, minNode1, minNode2);
    	forest.add(result);
    	return buildTree(forest);
    	}
    }
    /**
     * Given a root to a tree, this method finds paths to all the leaves in the tree
     * and stores the leaf values and their paths in an array
     */
    private void createCodings(TreeNode root, String path){
    	if(root.myValue!=-1){
    		myCodes[root.myValue] = path;
    	}  	
    	else{
    		createCodings(root.myLeft, path+"0");    		
    		createCodings(root.myRight, path+"1");
    	}
    }
    /**
     * Method to create a tree and codings of the file 
     */
    public int preprocessCompress(InputStream in) throws IOException{
    	int num = in.read();
    	//stores all values to appear and keys and the frequencies of these
    	//values in freqs
    	while(num != -1){
    		if(keys.contains(num)){
    			int appearances = freqs[num];
    			int add = appearances + 1;
    			freqs[num] = add;
    		}
    		else{
    			freqs[num] = 1;
    			keys.add(num);
    		}
    		
    		num = in.read();
    	}
    	Collections.sort(keys);
    	ArrayList<TreeNode> forest = new ArrayList<TreeNode>();
    	for(int x = 0; x<keys.size(); x++){
    		forest.add(new TreeNode(keys.get(x), freqs[keys.get(x)]));
    	}
    	forest.add(new TreeNode(PSEUDO_EOF, 1));
    	huffTree = buildTree(forest);    	
    	createCodings(huffTree, "");
    	//finds and returns number of bits saved by compression
    	int max = 0;
    	for(int x = 0; x<keys.size(); x++){
    		if(myCodes[keys.get(x)].length()>max)
    			max = myCodes[keys.get(x)].length();
    	}
    	return 8-max;
    }

    public void setViewer(HuffViewer viewer) {
        myViewer = viewer;
    }
    /**
     * Method that uncompresses a given file using the information given
     * in the header of the file and the body of the file
     */
    public int uncompress(InputStream in, OutputStream out) throws IOException {
    	BitInputStream in2 = new BitInputStream(in);
    	int magic = in2.readBits(BITS_PER_INT);
        if (magic != MAGIC_NUMBER){
        	in2.close();
            throw new IOException("magic number not right");
        }
        //recreates the forest from the header information
    	ArrayList<TreeNode> forest = new ArrayList<TreeNode>();
    	for(int x = 0; x<ALPH_SIZE; x++){
    		int y = in2.readBits(BITS_PER_INT);
    		if(y!=0){
    			forest.add(new TreeNode(x, y));
    		}
    	}
    	forest.add(new TreeNode(PSEUDO_EOF, 1));
    	//recreates tree and codings from header information
    	TreeNode root = buildTree(forest);
    	createCodings(root, "");
    	TreeNode tnode = root;
    	BitOutputStream out2 = new BitOutputStream(out);
    	int codedBits = 0;
    	while (true) {
            int bits = in2.readBits(1);      
            if (bits == -1) {
            	in2.close();
            	out2.close();
            	throw new IOException("error reading bits, no PSEUDO-EOF");
            }
            if ( (bits & 1) == 0) 
            	tnode = tnode.myLeft; 
            else                  
            	tnode = tnode.myRight;
            if (tnode.myValue!=-1) {
                if (tnode.myValue==PSEUDO_EOF) {
                    out2.close();
                    break;   // out of while-loop
                }
                else{
                    //write-out character stored in leaf-node
                	out2.writeBits(BITS_PER_WORD, tnode.myValue);
                	codedBits+=BITS_PER_WORD;
                    tnode = root;  // start back at top       
                }
            }
        }
    	in2.close();    	
        return codedBits;
    }
    
    private void showString(String s){
        myViewer.update(s);
    }

}
