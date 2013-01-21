package address.resolution;

public class Address {
	
	String addressCN;    // 中文地址
	String expressDept;  // 营业部
	String addressEN;    // 英文地址
	
	public Address(String addressCN, String expressDept) {
	    this.addressCN = addressCN;
	    this.expressDept = expressDept;
	}
	
	public Address(String addressCN, String expressDept, String addressEN) {
        this.addressCN = addressCN;
        this.expressDept = expressDept;
        this.addressEN = addressEN;
    }

}
