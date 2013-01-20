package address.resolution;

import java.io.File;
import java.io.IOException;
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
	
	private static Analyzer analyzer = new StandardAnalyzer();
	
	private static String ADDRESS_EN_FILED = "addressEN";
	private static String EXPRESS_DEPT_FILED = "expressDept";
	
	static String INDEX_FILE_PATH = "D:/temp/address/index";
	
	public static void createIndex(List<Address> addressList) {
	    createIndex(addressList, INDEX_FILE_PATH);
	}
	
    public static void createIndex(List<Address> addressList, String path) {
        File indexDir = new File(path); 
        boolean create = !indexDir.exists(); // 如果索引文件不存在，则新建；否则Append到存在的索引文件后面
        if( create ) {
            indexDir.mkdirs();
        }
        
        IndexWriter indexWriter = null;
        try {
        	// create : 新建索引还是增量添加
            indexWriter = new IndexWriter(indexDir, analyzer, create); 
            indexWriter.setMaxBufferedDocs(10); // 设置强制索引document对象后flush
            
            for ( Address address : addressList ) {
            	String addressEN = address.addressEN;
				if(addressEN == null) {
            		addressEN = GoogleTranslator.translate(address.addressCN, true);
            		if(addressEN == null || addressEN.equals(GoogleTranslator.REPEAT_ADDR_TAG)) {
            			ThreadPool.addCount();
            			continue;
                	}
            	}
            	
            	Document indexDoc = new Document();
            	
            	Field addressENFiled = new Field(ADDRESS_EN_FILED, addressEN, Field.Store.NO, Field.Index.TOKENIZED);
				indexDoc.add(addressENFiled);
				
				Field expressDeptField = new Field(EXPRESS_DEPT_FILED, address.expressDept, Field.Store.YES, Field.Index.NO);
				indexDoc.add(expressDeptField);
            	
                indexWriter.addDocument(indexDoc);
                
                ThreadPool.addCount();
                if ((ThreadPool.COUNT > 0 && ThreadPool.COUNT % 10 == 0)) {
                    log.info("已解析完【" + ThreadPool.COUNT + "】个地址。");
                    indexWriter.optimize();
                }
                
                indexWriter.optimize();
                
            }
        } catch (Exception e) {
            log.error("创建或更新索引时异常", e);
        } finally {
            try {
                if(indexWriter != null) {
                    indexWriter.close();
                }
            } catch (IOException e) {
            	log.error("关闭索引indexWriter时错误！", e);
            }
        }
    }
    
    public static String query(Address address) {
        return query(address, INDEX_FILE_PATH);
    }
    
    public static String query(Address address, String indexPath) {
    	String addressCN = address.addressCN;
    	String addressEN = address.addressEN;
    	if(addressEN == null) { 
            return null;
        }
    	if(addressEN.equals(GoogleTranslator.REPEAT_ADDR_TAG)) {
            return GoogleTranslator.REPEAT_ADDR_TAG;
        }
    	
    	// 消除会导致lucene报错的特殊字符
    	addressEN = QueryParser.escape(addressEN);
    	
    	String result = null;
        long startTime = System.currentTimeMillis();
        
        IndexSearcher searcher = null;
        try {
            searcher = new IndexSearcher(indexPath);
            Query query = new QueryParser(ADDRESS_EN_FILED, analyzer).parse(addressEN);
            
            if (searcher != null) {
                Hits hits = searcher.search(query);
                if (hits.length() > 0) {
                    log.debug("地址【" + addressCN + "】匹配到【" + hits.length() + "】个结果:");
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
        } finally {
            try {
                if(searcher != null) {
                	searcher.close();
                }
            } catch (Exception e) {
            	log.error("关闭索引searcher错误！", e);
            }
        }
        
        log.debug("耗时【" + (System.currentTimeMillis() - startTime) + "】ms!");
        
        return result;
    }
}
