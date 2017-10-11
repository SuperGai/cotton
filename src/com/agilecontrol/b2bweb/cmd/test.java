package com.agilecontrol.b2bweb.cmd;

import java.math.BigDecimal;

public class test {
public static void main(String[] args) {
	
	
	BigDecimal a=new BigDecimal("0.05");
	BigDecimal big=new BigDecimal("0.3");
	BigDecimal tot_advise_amt1=new BigDecimal("210000");
	BigDecimal tot_advise_amt_up=tot_advise_amt1.add(tot_advise_amt1.multiply(a));
	BigDecimal tot_advise_amt_down=tot_advise_amt1.subtract(tot_advise_amt1.multiply(big));
	
	System.out.println(tot_advise_amt_up+" "+tot_advise_amt_down);
	
}
	
	
}
