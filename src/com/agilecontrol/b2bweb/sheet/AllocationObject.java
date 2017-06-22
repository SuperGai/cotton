package com.agilecontrol.b2bweb.sheet;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agilecontrol.nea.util.JSONUtils;
import com.agilecontrol.nea.util.NDSException;

/**
 * 分摊对象， 保存余额
 * @author yfzhu
 *
 */
public class AllocationObject{
	private static Logger logger= LoggerFactory.getLogger(AllocationObject.class);
	
	/**
	 * 取值算法，目前仅支持2种
	 */
	public final static int CONVERTION_ROUND=1;
	public final static int CONVERTION_FLOOR=2;
	//34，39-41 做round up[0,5,6,7], 其他的round
	public final static int CONVERTION_CKSHOE=3;
	/**
	 * 分配完后的余额
	 */
	public int balanceValue;
	/**
	 * 是否允许余额不为0
	 */
	public boolean allowBalance;
	/**
	 * 分摊时候的算法, CONVERTION_ROUND,CONVERTION_FLOOR
	 */
	public int convMethod; 
	/**
	 * 在allowBalance=false的时候设置，当余额为正数时，将余额分摊给各个元素的顺序（逐一），这里放的是元素的位置索引，注意若allowIndex相应位置为0，表示不允许分摊到此位置（一般就是对应没有相应条码）
	 * 比如，某商品尺码组：[34,35,36,37], 增加顺序：[36,37,35,34](对应：[2,3,1,0], 而该商品尺码仅有[34,36,37]时，allowIndex 为[1,0,1,1],则 35位置上不能加1
	 */
	public int[] incrementSequence;
	/**
	 * 在allowBalance=false的时候设置，当余额为负数时，从各个元素减少1的顺序，这里放的是元素的位置索引
	 */
	public int[] decrementSequence;

	
	/**
	 * 
	 * @param allowBalance 
	 * @param convMethod 分摊时候的算法, CONVERTION_ROUND,CONVERTION_FLOOR
	 */
	public AllocationObject(boolean allowBalance, int convMethod){
		this.allowBalance= allowBalance;
		this.convMethod= convMethod;
		clear();
		
	}
	/**
	 * 
	 * @param allowBalance 
	 * @param convMethod 分摊时候的算法, CONVERTION_ROUND,CONVERTION_FLOOR
	 * @param incrementSequence
	 * @param decrementSequence
	 */
	public AllocationObject(boolean allowBalance, int convMethod,int[] incrementSequence,int[] decrementSequence/*, int[] allowIndex*/){
		this.allowBalance= allowBalance;
		this.convMethod= convMethod;
		this.incrementSequence=incrementSequence;
		this.decrementSequence= decrementSequence;
		//this.allowIndex= allowIndex;
		clear();
		
	}
	
	/**
	 * 按要求，中间的码的index排在前面，两边的index排在后面。 注意返回的数组中的元素代表的是位置Index
	 * sample: [0 >0 0 >0 >0 0] return [3 1 4 2 0 5]
	 * 		[0, >0, 0, >0, >0, >0, 0]	return [3,4,1,5,2,0,6]
	 * @param ratios 元素为>0表示有这个尺码，为0的都排在后面
	 * @param centerFirst true if from center, 
	 * ([0 1 0 1 1 0],true)=[3 1 4 2 0 5],  ([0 1 0 1 1 0],false)=[5 0 2 4 1 3]
	 * @return
	 */
	protected int[] getSizeSequence(float[] ratios, boolean centerFirst){
		ArrayList<Integer> seq=new ArrayList(); //has such size
		ArrayList<Integer> seqNull=new ArrayList(); // no such size
		if( ratios.length%2==1){
			if( ratios[ ratios.length/2] >0) seq.add(ratios.length/2);
			else seqNull.add(ratios.length/2);
		}
		for(int i=0;i<ratios.length/2;i++){
			//small index center one
			int pos=ratios.length/2 -1 -i;
			if (ratios[pos ] >0) seq.add( pos);
			else seqNull.add(pos);
			
			//big index center one
			pos=(ratios.length+1)/2 +i ;
			if (ratios[ pos] >0) seq.add( pos);
			else seqNull.add(pos);
		}
		
		int[] ret=new int[ratios.length];
		for(int i=0;i< seq.size();i++){
			ret[i]= seq.get(i);
		}
		for(int i=0;i< seqNull.size();i++){
			ret[i+seq.size()]= seqNull.get(i);
		}
		if(centerFirst) return ret;
		else{
			int[] nn= new int[ret.length];
			for(int i=0;i<nn.length;i++)
				nn[i]= ret[nn.length-1-i];
			return nn;
		}
	}		
	public String toString(){
		return "{allowBalance:"+ allowBalance+", conv:"+ convMethod+", balance:"+ balanceValue+"}";
	}
	public void clear(){
		balanceValue=0;
	}
	/**
	
	 * @param qty
	 * @param ratios , ratio 元素为0 表示禁止赋值
	 * @param asis [Integer] null means no value
	 * @return [Integer] null means no value(not allowed)
	 * @throws JSONException 
	 */
	public JSONArray allocateSKU(int qty,float[] ratios, JSONArray asis) throws NDSException, JSONException{
		if(decrementSequence==null){
			decrementSequence=getSizeSequence(ratios,false);
		}
		if(incrementSequence==null){
			incrementSequence=getSizeSequence(ratios,true);
		}
		
		//ratio 之和必须为100
		float sumRatio=0;
		for(int i=0;i< ratios.length;i++) sumRatio+=ratios[i];
		if( Math.abs(sumRatio-100f)>1){
			throw new NDSException("Ratio setting error, shoule sum to 100:"+ JSONUtils.toJSONArrayPrimitive(ratios).toString() );
		}
		
		
		int[] qtys=new int[ratios.length];
		int sum=0;  
		
		for(int i=0;i< qtys.length; i++){
			//目前仅仅考虑2种算法
			int q= (convMethod==AllocationObject.CONVERTION_ROUND? (int)Math.round( qty* ratios[i]/100.0):(int)Math.floor( qty* ratios[i]/100.0));
			qtys[i]=q;
			sum+=q;
		}
		logger.debug("allocateSKU( "+ JSONUtils.toJSONArrayPrimitive(ratios).toString()+", "+ 
				qty+",  sum="+ sum+", qtys="+ JSONUtils.toJSONArrayPrimitive(qtys).toString()+", allowbalance="+allowBalance+" )");
		if(!allowBalance){
			
			if(sum>qty){
				logger.debug("found more, will descrease");
				//余额为负数从decrementSequence中指定的元素依次减少
				if(decrementSequence!=null){
					logger.debug("dec sequnce:"+ JSONUtils.toJSONArrayPrimitive(decrementSequence).toString());
					for(int i=0; i< decrementSequence.length;i++){
						int idx=decrementSequence[i];
						if(qtys[idx]>0){
							// >1 已经隐含表示此列是存在SKU的，并且还有数量可以下来
							qtys[idx]= qtys[idx]-1;
							sum--;
							if(sum==qty) break;
						}
					}
					if(sum!=qty)throw new  NDSException("(001)@cannot-allocate-the-number-to-the-size@");//Internal Error: find  decrementSequence, but not balanced");
					logger.debug("allocateSKU( "+ JSONUtils.toJSONArrayPrimitive(ratios).toString()+", "+ 
							qty+", sum="+ sum+", qtys="+ JSONUtils.toJSONArrayPrimitive(qtys).toString()+")");
					
				}else{
					logger.debug("not found dec sequnce");
					for(int i=0;i< qtys.length;i++){
						if(qtys[i]>0) {
							qtys[i]= qtys[i]-1;
							sum--;
							if(sum==qty) break;
						}
					}
					if(sum!=qty)throw new  NDSException("(002)@cannot-allocate-the-number-to-the-size@");//throw new  NDSException("Internal Error: not balanced after decrease");
				}
			}else if(sum<qty){
				//余额为正数
				if(incrementSequence!=null){
					for(int i=0; i< incrementSequence.length;i++){
				
						int idx=incrementSequence[i];
						if(ratios[idx]>0){
							//ratio 为0的表示不存在此SKU，不赋值
							qtys[idx]= qtys[idx]+1;
							sum++;
							if(sum==qty) break;
						}
					}
					if(sum!=qty)throw new  NDSException("(003)@cannot-allocate-the-number-to-the-size@");//throw new  NDSException("Internal Error: find  incrementSequence, but not balanced");
				}else{
					for(int i=0;i< qtys.length;i++){
						if(ratios[i]>0){
							qtys[i]= qtys[i]+1;
							sum++;
							if(sum==qty) break;
						}
					}
					if(sum!=qty)throw new  NDSException("(004)@cannot-allocate-the-number-to-the-size@");//throw new  NDSException("Internal Error: not balanced after increase");
				}
			}
			
			
		}
		balanceValue= qty-sum;
		
		JSONArray ja=new JSONArray();
		for(int i=0;i<qtys.length;i++){
			int q=qtys[i];
			if(asis.isNull(i)) ja.put(JSONObject.NULL);
			else ja.put(q);
		}
		
		logger.debug("allocateSKU( "+ JSONUtils.toJSONArrayPrimitive(ratios).toString()+", "+ qty+")="+ ja.toString());
		return ja;
		
		
	}			
}	