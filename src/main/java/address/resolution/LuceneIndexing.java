package address.resolution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

public class LuceneIndexing {
	
	static Logger log = Logger.getLogger(LuceneIndexing.class);
	
	static Analyzer analyzer = new StandardAnalyzer();
	
	static String ADDRESS_EN_FILED = "addressEN";
	static String EXPRESS_DEPT_FILED = "expressDept";
	
	static String INDEX_FILE_PATH = "D:/temp/address/index";
	
	public static void createIndex(List<Address> addressList) {
	    createIndex(addressList, true);
	}
	
    public static void createIndex(List<Address> addressList, boolean create) {
        File indexDir = new File(INDEX_FILE_PATH); 
        if( !indexDir.exists() ) {
            indexDir.mkdirs();
        }
        
        IndexWriter indexWriter = null;
        try {
            indexWriter = new IndexWriter(indexDir, analyzer, create);
            indexWriter.setMaxBufferedDocs(10); // 设置强制索引document对象后flush
            
            for ( Address address : addressList ) {
            	if(address.addressEN == null) {
            		address.addressEN = GoogleTranslateor.translate(address.addressGBK);
            	}
            	
            	if(address.addressEN == null) {
            	    continue;
            	}
            	
            	Document indexDoc = new Document();
            	
            	Field addressENFiled = new Field(ADDRESS_EN_FILED, address.addressEN, Field.Store.NO, Field.Index.TOKENIZED);
				indexDoc.add(addressENFiled);
				
				Field expressDeptField = new Field(EXPRESS_DEPT_FILED, address.expressDept, Field.Store.YES, Field.Index.NO);
				indexDoc.add(expressDeptField);
            	
                indexWriter.addDocument(indexDoc);
                
                ThreadPool.count ++;
                if ((ThreadPool.count > 0 && ThreadPool.count % 10 == 0)) {
                    log.info("已解析完【" + ThreadPool.count + "】个地址。");
                    indexWriter.optimize();
                }
                
                indexWriter.optimize();
                
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
    
    public static String query(String addressGBK, boolean createIndex) {
    	String addressEN = GoogleTranslateor.translate(addressGBK);
    	if(addressEN == null) {
            return null;
        }
    	
    	String result = null;
        long startTime = System.currentTimeMillis();
    	
        try {
            IndexSearcher searcher = new IndexSearcher(INDEX_FILE_PATH);
            Query query = new QueryParser(ADDRESS_EN_FILED, analyzer).parse(addressEN);
            
            
            if (searcher != null) {
                Hits hits = searcher.search(query);
                if (hits.length() > 0) {
                    log.debug("地址【" + addressGBK + "】匹配到【" + hits.length() + "】个结果:");
                    for (int i = 0; i < hits.length(); i++) {
                        Document hitDoc = hits.doc(i);
                        log.debug("              " + hitDoc.get(EXPRESS_DEPT_FILED));
                    }
                    result = hits.doc(0).get(EXPRESS_DEPT_FILED);
                }
                else {
                    log.debug("没有找到结果。");
                }
            }
        } catch (Exception e) {
            log.error("检索异常", e);
        }
        
        log.debug("耗时【" + (System.currentTimeMillis() - startTime) + "】ms!");
        
        // 为新地址建立索引
        if(result != null && createIndex) {
            List<Address> addressList = new ArrayList<Address>();
            Address address = new Address(addressGBK, result);
            address.addressEN = addressEN;
            addressList.add(address);
            
            createIndex(addressList, false);
        }
        
        return result;
    }
}
