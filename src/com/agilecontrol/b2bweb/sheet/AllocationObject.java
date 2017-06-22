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
 * ��̯���� �������
 * @author yfzhu
 *
 */
public class AllocationObject{
	private static Logger logger= LoggerFactory.getLogger(AllocationObject.class);
	
	/**
	 * ȡֵ�㷨��Ŀǰ��֧��2��
	 */
	public final static int CONVERTION_ROUND=1;
	public final static int CONVERTION_FLOOR=2;
	//34��39-41 ��round up[0,5,6,7], ������round
	public final static int CONVERTION_CKSHOE=3;
	/**
	 * �����������
	 */
	public int balanceValue;
	/**
	 * �Ƿ�������Ϊ0
	 */
	public boolean allowBalance;
	/**
	 * ��̯ʱ����㷨, CONVERTION_ROUND,CONVERTION_FLOOR
	 */
	public int convMethod; 
	/**
	 * ��allowBalance=false��ʱ�����ã������Ϊ����ʱ��������̯������Ԫ�ص�˳����һ��������ŵ���Ԫ�ص�λ��������ע����allowIndex��Ӧλ��Ϊ0����ʾ�������̯����λ�ã�һ����Ƕ�Ӧû����Ӧ���룩
	 * ���磬ĳ��Ʒ�����飺[34,35,36,37], ����˳��[36,37,35,34](��Ӧ��[2,3,1,0], ������Ʒ�������[34,36,37]ʱ��allowIndex Ϊ[1,0,1,1],�� 35λ���ϲ��ܼ�1
	 */
	public int[] incrementSequence;
	/**
	 * ��allowBalance=false��ʱ�����ã������Ϊ����ʱ���Ӹ���Ԫ�ؼ���1��˳������ŵ���Ԫ�ص�λ������
	 */
	public int[] decrementSequence;

	
	/**
	 * 
	 * @param allowBalance 
	 * @param convMethod ��̯ʱ����㷨, CONVERTION_ROUND,CONVERTION_FLOOR
	 */
	public AllocationObject(boolean allowBalance, int convMethod){
		this.allowBalance= allowBalance;
		this.convMethod= convMethod;
		clear();
		
	}
	/**
	 * 
	 * @param allowBalance 
	 * @param convMethod ��̯ʱ����㷨, CONVERTION_ROUND,CONVERTION_FLOOR
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
	 * ��Ҫ���м�����index����ǰ�棬���ߵ�index���ں��档 ע�ⷵ�ص������е�Ԫ�ش������λ��Index
	 * sample: [0 >0 0 >0 >0 0] return [3 1 4 2 0 5]
	 * 		[0, >0, 0, >0, >0, >0, 0]	return [3,4,1,5,2,0,6]
	 * @param ratios Ԫ��Ϊ>0��ʾ��������룬Ϊ0�Ķ����ں���
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
	 * @param ratios , ratio Ԫ��Ϊ0 ��ʾ��ֹ��ֵ
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
		
		//ratio ֮�ͱ���Ϊ100
		float sumRatio=0;
		for(int i=0;i< ratios.length;i++) sumRatio+=ratios[i];
		if( Math.abs(sumRatio-100f)>1){
			throw new NDSException("Ratio setting error, shoule sum to 100:"+ JSONUtils.toJSONArrayPrimitive(ratios).toString() );
		}
		
		
		int[] qtys=new int[ratios.length];
		int sum=0;  
		
		for(int i=0;i< qtys.length; i++){
			//Ŀǰ��������2���㷨
			int q= (convMethod==AllocationObject.CONVERTION_ROUND? (int)Math.round( qty* ratios[i]/100.0):(int)Math.floor( qty* ratios[i]/100.0));
			qtys[i]=q;
			sum+=q;
		}
		logger.debug("allocateSKU( "+ JSONUtils.toJSONArrayPrimitive(ratios).toString()+", "+ 
				qty+",  sum="+ sum+", qtys="+ JSONUtils.toJSONArrayPrimitive(qtys).toString()+", allowbalance="+allowBalance+" )");
		if(!allowBalance){
			
			if(sum>qty){
				logger.debug("found more, will descrease");
				//���Ϊ������decrementSequence��ָ����Ԫ�����μ���
				if(decrementSequence!=null){
					logger.debug("dec sequnce:"+ JSONUtils.toJSONArrayPrimitive(decrementSequence).toString());
					for(int i=0; i< decrementSequence.length;i++){
						int idx=decrementSequence[i];
						if(qtys[idx]>0){
							// >1 �Ѿ�������ʾ�����Ǵ���SKU�ģ����һ���������������
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
				//���Ϊ����
				if(incrementSequence!=null){
					for(int i=0; i< incrementSequence.length;i++){
				
						int idx=incrementSequence[i];
						if(ratios[idx]>0){
							//ratio Ϊ0�ı�ʾ�����ڴ�SKU������ֵ
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