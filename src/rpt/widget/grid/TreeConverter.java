package rpt.widget.grid;

import java.util.ArrayList;

import org.json.JSONArray;

import com.agilecontrol.nea.util.StringBuilderWriter;

/**
 * 将json array 多列，转换为tree 结构

树状结构转换
		格式: col1, col2
		     a      x
		     a      y
		     b      z
		     total1  null
		     total2  null
		 what we want: 
		 [{headerName:a, childern:[{headerName: x}, {headerName: y}]},
		  {headerName:b, childern:[{headerName: z}]}    
		  {headerName:total1}, {headerName:total2}
		]
		
 * @author yfzhu
 *
 */
public class TreeConverter {

	
	private JSONArray data;
	
	public class TreeNode{
		Object value;
		ArrayList<TreeNode> children=new ArrayList();
		/**
		 * 行转列的时候，当前节点的列号columnIndex，仅对最底层的child有效, 父节点为下层最底层节点的最小值
		 */
		int col=-1;
		TreeNode parent;
		TreeNode root;
		public TreeNode(Object value){
			this.value=value;
			
		}
		public TreeNode(Object value, TreeNode parent, TreeNode root){
			this.value=value;
			this.parent=parent;
			this.root=root;
		}
		/**
		 * 是否有子节点
		 * @return
		 */
		public boolean hasChildren(){
			return children.size()>0;
		}
		
		public void write(StringBuilderWriter sbw){
			sbw.println(value.toString() +", col="+ col );
			sbw.pushIndent();
			for(TreeNode child:children){
				child.write(sbw);
			}
			sbw.popIndent();
		}
	}
	
	ArrayList<TreeNode> tree;

	/**
	 * 
	 * @param d 二维数组，数据格式[ [...]] 内嵌的是jsonarray, 如果是1维，需要转换下
	 */
	public TreeConverter(JSONArray d){
		if(d.opt(0) instanceof JSONArray)
			this.data =d;
		else{
			data=new JSONArray();
			for(int i=0;i< d.length();i++){
				JSONArray j=new JSONArray();
				j.put(d.opt(i));
				data.put(j);
			}
			
		}
	}
	/**
	 * 
	 * @return null if tree has no node
	 */
	private TreeNode getLastRootNode(){
		if(tree.size()==0) return null;
		return tree.get(tree.size()-1);
	}
	/**
	 * 获得矩阵的列数，取第一行的列数
	 * @return
	 */
	private int getColumns() throws Exception{
		return data.getJSONArray(0).length();
	}
	/**
	 * 创建新的根节点，tree中有多个根节点
	 * @param value
	 * @return
	 */
	private TreeNode createRootNode(Object value){
		TreeNode node=new TreeNode(value);
		node.parent=null;
		node.root=node;
		node.value=value;
		tree.add(node);
		return node;
	}
	/**
	 * 创建新的节点
	 * @param value
	 * @return
	 */
	private TreeNode createChildNode(Object value,TreeNode tn){
		TreeNode node=new TreeNode(value, tn, tn.root);
		tn.children.add(node);
		return node;
	}
	/**
	 * 获取指定节点的子节点中的最后一个
	 * @param node
	 * @return null 如果没有子节点
	 */
	private TreeNode getLastChild(TreeNode node){
		if(node.children.size()==0) return null;
		return node.children.get(node.children.size()-1);
	}
	
	
	
	public String toString(){
		StringBuilder sb=new StringBuilder();
		StringBuilderWriter sbw=new StringBuilderWriter(sb);
		for(TreeNode node: tree){
			node.write(sbw);
		}
		return sb.toString();
	}
	
	public static void main(String[] args) throws Exception{
		//JSONArray ja=new JSONArray("[['a','b','c','x'],['a','b','c','y'],['a','b','d','x'],['a','b','d','y'],['a','e','f','x'],['a','e','f','y'],['b','e','f','y'],['b','e','f',null] ,['b',null,null,null]]");
		JSONArray ja=new JSONArray("['a','b','c','x']");
		TreeConverter tc=new TreeConverter(ja);
		tc.toTree();
		/**
a
    b
        c
            x
            y
        d
            x
            y
    e
        f
            x
            y
b
    e
        f
            y
            null
    null
        null
            null		 
		 */
		System.out.println(tc.toString());
	}
	/**
	 * 树状结构转换
		格式: col1, col2
		     a      x
		     a      y
		     b      z
		     total1  null
		     total2  null
		 what we want: 
		 [{headerName:a, childern:[{headerName: x}, {headerName: y}]},
		  {headerName:b, childern:[{headerName: z}]}    
		  {headerName:total1}, {headerName:total2}
		]
	 * 
	 * @param data  [[]]
	 * @return [{}]
	 * @throws Exception
	 */
	public ArrayList<TreeNode> toTree() throws Exception{
		tree=new ArrayList();
		int colCount=0;
		TreeNode currNode=null;
		int cols=getColumns();// 多少列
		for(int i=0;i<data.length();i++){
			JSONArray row=data.getJSONArray(i);
			//当前树节点指向tree的最后的root节点
			currNode=getLastRootNode();
			if(currNode==null) currNode=createRootNode(row.get(0));
			
			for(int j=0;j<cols;j++){
				Object col=row.get(j);
				Object preCol=null;
				//移动指针，当前树节点指向当前节点的儿子节点的最后一个
				if(j!=0){
					TreeNode lastChild=getLastChild(currNode);//首列不需动currentNode,已经指向了
					//当前节点为空，表示尚未建立，则新建子节点在当前节点
					if(lastChild==null)currNode=createChildNode(col, currNode);
					else currNode=lastChild;
				}
				preCol=currNode.value;
//				TreeNode sibling=getLastSibling(currNode);//获取当前节点上一个兄弟节点，可能为空
//				=getValue(sibling);//当前节点上一个兄弟节点的值 ,兄弟节点也许不存在
				
				//如果元素相同，curr节点保持，否则需要创建新节点
				if(!col.equals(preCol)){
					//非首列，在父节点创建新的child, 如果父节点为空，创建root节点
					if(j==0) {
						currNode=createRootNode(col);
					}else{
//						System.out.println("handle("+ i+","+j+"), col="+ col+", prevcol="+preCol);
						currNode=createChildNode(col, currNode.parent);//创建当前节点的兄弟节点，并指向此子节点
					}
				}
				if(j==cols-1){
					//最后的节点构造成现在的列
					currNode.col=colCount++;
				}else if(currNode.col<0)currNode.col=colCount;//保持第一个子节点的col
			}
		}
		return tree;
	}
}
