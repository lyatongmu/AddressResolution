package address.resolution;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

public class LuceneIndexing {
	
	private static Logger log = Logger.getLogger(LuceneIndexing.class);
	
	static Analyzer analyzer = new StandardAnalyzer();
	
	static String addressEN_FILED_NAME = "addressEN";
	static String expressDept_FILED_NAME = "expressDept";

    public static void createIndex(List<Address> addressList) {
        File tempIndexDir = new File("D:/temp/index"); 
        IndexWriter indexWriter = null;
        int count = 0;
        try {
            indexWriter = new IndexWriter(tempIndexDir, analyzer, false);
            indexWriter.setMaxBufferedDocs(10); // 设置强制索引document对象后flush
            
            indexWriter.optimize();
            
            for ( Address address : addressList ) {
            	if(address.addressEN == null) {
            		address.addressEN = GoogleTranslateor.translate(address.addressGBK);
            	}
            	
            	Document indexDoc = new Document();
            	Field addressENFiled = new Field(addressEN_FILED_NAME, address.addressEN, Field.Store.NO, Field.Index.TOKENIZED);
				indexDoc.add(addressENFiled);
				
				Field expressDeptField = new Field(expressDept_FILED_NAME, address.expressDept, Field.Store.YES, Field.Index.NO);
				indexDoc.add(expressDeptField);
            	
                indexWriter.addDocument(indexDoc);
                
                count ++;
                // 进度条的信息更新，每一百个更新一次
                if ((count > 0 && count % 100 == 0)) {
                    count = 0;
                    indexWriter.optimize();
                }
            }
        } catch (Exception e) {
            log.error("创建索引异常", e);
        } finally {
            try {
                if(indexWriter != null) {
                    indexWriter.close();
                }
            } catch (IOException e) {
            	log.error("关闭索引文件错误！", e);
            }
        }
    }
    
    public static String query(String addressGBK) throws ParseException, CorruptIndexException, IOException {
    	String addressEN = GoogleTranslateor.translate(addressGBK);
    	
        Query query = new QueryParser(addressEN_FILED_NAME, analyzer).parse(addressEN);

        Hits hits = null;
        IndexSearcher searcher = new IndexSearcher("E:/cms/demo/jh/index/1363");
        
        String result = null;
        long startTime = new Date().getTime();
        if (searcher != null) {
            hits = searcher.search(query);
            if (hits.length() > 0) {
                System.out.println("找到:" + hits.length() + " 个结果!");
                for (int i = 0; i < hits.length(); i++) {
                    Document hitDoc = hits.doc(i);
                    System.out.println("内容：" + hitDoc.get(expressDept_FILED_NAME));
                }
                result = hits.doc(0).get(expressDept_FILED_NAME);
            }
        }
        long endTime = new Date().getTime();
        log.debug("这花费了" + (endTime - startTime) + " 毫秒搜索!");
        
        return result;
    }

}
