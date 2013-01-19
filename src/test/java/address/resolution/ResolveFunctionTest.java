package address.resolution;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

/**
 * 单个测试功能
 */
public class ResolveFunctionTest extends TestCase {
    
    public void setUp() {
    	File resultKeyFile = new File("D:/temp/address/en/result.key");
    	if( resultKeyFile.exists() ) {
    		resultKeyFile.delete();
    	} 
    	
    	GoogleTranslateor.RESULT_FILE_PATH = "D:/temp/address/en/test.data";
    }
    
    public void testAddressResolution() {
    	String address1 = "浙江省杭州市天目山路176号（西湖数源软件园）18号楼";
        String expressDept1 = "数源营业部";
        
        List<Address> addressList = new ArrayList<Address>();
        addressList.add(new Address(address1, expressDept1));
        
        LuceneIndexing.createIndex(addressList);
        
        LuceneIndexing.query("浙江省杭州市天目山路176号（西湖数源软件）20号楼", true);
    }

}
