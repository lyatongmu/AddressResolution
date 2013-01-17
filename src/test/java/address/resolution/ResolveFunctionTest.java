package address.resolution;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * 单个测试功能
 */
public class ResolveFunctionTest extends TestCase {
    
    String address1 = "浙江省杭州市天目上路176号（西湖数源软件园）18号楼";
    String expressDept1 = "数源营业部";
    
    public void testAddressResolution() {
        List<Address> addressList = new ArrayList<Address>();
        addressList.add(new Address(address1, expressDept1));
        
        LuceneIndexing.createIndex(addressList);
        
        LuceneIndexing.query("浙江省杭州市天目上路176号（西湖数源软件）20号楼", true);
    }

}
