package rpt.widget.grid;

import java.util.ArrayList;

import org.json.JSONArray;

import com.agilecontrol.nea.util.StringBuilderWriter;

/**
 * ��json array ���У�ת��Ϊtree �ṹ

��״�ṹת��
		��ʽ: col1, col2
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
		 * ��ת�е�ʱ�򣬵�ǰ�ڵ���к�columnIndex��������ײ��child��Ч, ���ڵ�Ϊ�²���ײ�ڵ����Сֵ
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
		 * �Ƿ����ӽڵ�
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
	 * @param d ��ά���飬���ݸ�ʽ[ [...]] ��Ƕ����jsonarray, �����1ά����Ҫת����
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
	 * ��þ����������ȡ��һ�е�����
	 * @return
	 */
	private int getColumns() throws Exception{
		return data.getJSONArray(0).length();
	}
	/**
	 * �����µĸ��ڵ㣬tree���ж�����ڵ�
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
	 * �����µĽڵ�
	 * @param value
	 * @return
	 */
	private TreeNode createChildNode(Object value,TreeNode tn){
		TreeNode node=new TreeNode(value, tn, tn.root);
		tn.children.add(node);
		return node;
	}
	/**
	 * ��ȡָ���ڵ���ӽڵ��е����һ��
	 * @param node
	 * @return null ���û���ӽڵ�
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
	 * ��״�ṹת��
		��ʽ: col1, col2
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
		int cols=getColumns();// ������
		for(int i=0;i<data.length();i++){
			JSONArray row=data.getJSONArray(i);
			//��ǰ���ڵ�ָ��tree������root�ڵ�
			currNode=getLastRootNode();
			if(currNode==null) currNode=createRootNode(row.get(0));
			
			for(int j=0;j<cols;j++){
				Object col=row.get(j);
				Object preCol=null;
				//�ƶ�ָ�룬��ǰ���ڵ�ָ��ǰ�ڵ�Ķ��ӽڵ�����һ��
				if(j!=0){
					TreeNode lastChild=getLastChild(currNode);//���в��趯currentNode,�Ѿ�ָ����
					//��ǰ�ڵ�Ϊ�գ���ʾ��δ���������½��ӽڵ��ڵ�ǰ�ڵ�
					if(lastChild==null)currNode=createChildNode(col, currNode);
					else currNode=lastChild;
				}
				preCol=currNode.value;
//				TreeNode sibling=getLastSibling(currNode);//��ȡ��ǰ�ڵ���һ���ֵܽڵ㣬����Ϊ��
//				=getValue(sibling);//��ǰ�ڵ���һ���ֵܽڵ��ֵ ,�ֵܽڵ�Ҳ������
				
				//���Ԫ����ͬ��curr�ڵ㱣�֣�������Ҫ�����½ڵ�
				if(!col.equals(preCol)){
					//�����У��ڸ��ڵ㴴���µ�child, ������ڵ�Ϊ�գ�����root�ڵ�
					if(j==0) {
						currNode=createRootNode(col);
					}else{
//						System.out.println("handle("+ i+","+j+"), col="+ col+", prevcol="+preCol);
						currNode=createChildNode(col, currNode.parent);//������ǰ�ڵ���ֵܽڵ㣬��ָ����ӽڵ�
					}
				}
				if(j==cols-1){
					//���Ľڵ㹹������ڵ���
					currNode.col=colCount++;
				}else if(currNode.col<0)currNode.col=colCount;//���ֵ�һ���ӽڵ��col
			}
		}
		return tree;
	}
}
