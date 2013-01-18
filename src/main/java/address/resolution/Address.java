package address.resolution;

public class Address {
	
	String addressCN;   // 中文地址
	String expressDept;  // 营业部
	String addressEN;    // 英文地址
	
	public Address(String addressGBK, String expressDept) {
	    this.addressCN = addressGBK;
	    this.expressDept = expressDept;
	}

}
