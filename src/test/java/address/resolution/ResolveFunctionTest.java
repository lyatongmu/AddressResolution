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
    	GoogleTranslator.RESULT_FILE_DIR = "D:/temp/address/en/test";
    	deleteDir(new File("D:/temp/address/index"));
        deleteDir(new File(GoogleTranslator.RESULT_FILE_DIR));
    }
    
    public void tearDown() {
		deleteDir(new File("D:/temp/address/index"));
		deleteDir(new File(GoogleTranslator.RESULT_FILE_DIR));
    }
    
    public static void deleteDir(File dir) {
		if (dir.exists()) {
			for ( String fileName : dir.list() ) {
			    File file =  new File(dir.getPath() + "/" + fileName);
			    if( file.isDirectory() ) {
			        deleteDir(file);
			    } 
			    else {
			        file.delete();
			    }
			}
			if (dir.list().length == 0) {
				dir.delete();
			}
		}
	}

    public void testAddressResolution() {
    	String address1 = "浙江省杭州市天目山路176号（西湖数源软件园）18号楼";
        String expressDept1 = "数源营业部";
        
        List<Address> addressList = new ArrayList<Address>();
        addressList.add(new Address(address1, expressDept1));
        
        LuceneIndexing.createIndex(addressList);
        
        String testAddress = "浙江省杭州市天目山路176号（西湖数源软件）20号楼";
        Address temp = new Address(testAddress, null);
        String addressEN = GoogleTranslator.translate(testAddress, true);
        
        temp.addressEN = addressEN;
        LuceneIndexing.query(temp);
    }

}
